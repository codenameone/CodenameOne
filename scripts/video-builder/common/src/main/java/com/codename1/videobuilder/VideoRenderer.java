/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.videobuilder;

import com.codename1.media.VideoIO;
import com.codename1.media.VideoWriter;
import com.codename1.media.VideoWriterBuilder;
import com.codename1.components.SpanLabel;
import com.codename1.ui.CodeEditor;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.VideoAnimationSupport;
import com.codename1.ui.VideoCodeEditor;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.util.ImageIO;
import com.codename1.ui.util.UITimer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Turns a VideoScript into preview frames and a staged, hardware-encoded MP4. */
public final class VideoRenderer {
    public interface ProgressListener { void onProgress(long positionMs, long durationMs, String sceneId); }

    private final VideoScript script;
    private final String orientation;
    private final int width;
    private final int height;
    private final Form form;
    private final Stage stage;
    private final Stage overlayStage;
    private final Layer rewindIndicator;
    private final Layer replayIndicator;
    private final Map<String, Layer> layers = new LinkedHashMap<>();
    private final Map<VideoScript.Action, Boolean> fired = new IdentityHashMap<>();
    private final Map<VideoScript.Action, float[]> pointerStarts = new IdentityHashMap<>();
    private final Map<VideoScript.Action, float[]> layerAnimationStarts = new IdentityHashMap<>();
    private DemoScene demo;
    private DemoContext demoContext;
    private int currentScene = -1;
    private long lastVisualRelativeMs = -1;
    private volatile boolean cancelled;
    private boolean previewPlaying = true;
    private long previewStarted;
    private long previewPausedAt;
    private boolean layoutDirty = true;

    public VideoRenderer(VideoScript script, String orientation) {
        this.script = script;
        this.orientation = orientation;
        boolean portrait = "portrait".equals(orientation);
        width = portrait ? script.getOutput().portraitWidth() : script.getOutput().landscapeWidth();
        height = portrait ? script.getOutput().portraitHeight() : script.getOutput().landscapeHeight();
        final Stage[] createdStage = new Stage[1];
        final Stage[] createdOverlayStage = new Stage[1];
        final Form[] createdForm = new Form[1];
        Runnable create = () -> {
            createdStage[0] = new Stage();
            createdOverlayStage[0] = new Stage();
            createdStage[0].setUIID("VideoStage");
            createdOverlayStage[0].setOpaque(false);
            createdStage[0].add(createdOverlayStage[0]);
            createdForm[0] = new Form(new FillLayout());
            createdForm[0].setScrollable(false);
            createdForm[0].add(createdStage[0]);
            createdForm[0].setWidth(width);
            createdForm[0].setHeight(height);
            createdStage[0].setWidth(width);
            createdStage[0].setHeight(height);
            createdStage[0].setShouldCalcPreferredSize(true);
        };
        if (Display.getInstance().isEdt()) create.run();
        else Display.getInstance().callSeriallyAndWait(create);
        stage = createdStage[0];
        overlayStage = createdOverlayStage[0];
        form = createdForm[0];
        rewindIndicator = new Layer(new BadgeComponent("<<  QUICK REWIND"),
                portrait ? new VideoScript.Bounds(0.05f, 0.025f, 0.48f, 0.042f)
                        : new VideoScript.Bounds(0.025f, 0.035f, 0.22f, 0.065f));
        replayIndicator = new Layer(new BadgeComponent(""),
                portrait ? new VideoScript.Bounds(0.08f, 0.49f, 0.84f, 0.055f)
                        : new VideoScript.Bounds(0.27f, 0.86f, 0.46f, 0.08f));
        rewindIndicator.setVisible(false);
        replayIndicator.setVisible(false);
        overlayStage.add(rewindIndicator);
        overlayStage.add(replayIndicator);
    }

    public void cancel() { cancelled = true; }
    public Form getForm() { return form; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    /** Captures one deterministic timeline position for fast storyboard/layout review. */
    public Path captureFrame(Path output, long positionMs) throws IOException {
        Throwable[] failure = new Throwable[1];
        Display.getInstance().callSeriallyAndWait(() -> {
            try {
                form.setVisible(true);
                tick(Math.max(0, Math.min(positionMs, script.getDurationMs() - 1)));
                saveFrame(paintFrame(), output);
            } catch (Throwable error) {
                failure[0] = error;
            }
        });
        disposeDemo();
        if (failure[0] != null) throw new IOException("Frame capture failed", failure[0]);
        return output;
    }

    /** Opens an interactive real-time preview using the same timeline engine as export. */
    public void showPreview() {
        form.getToolbar().addCommandToLeftBar("Restart", null, event -> {
            previewPausedAt = 0;
            previewStarted = System.currentTimeMillis();
            currentScene = -1;
        });
        form.getToolbar().addCommandToRightBar("Pause", null, event -> {
            previewPlaying = !previewPlaying;
            if (previewPlaying) previewStarted = System.currentTimeMillis() - previewPausedAt;
            else previewPausedAt = Math.max(0, System.currentTimeMillis() - previewStarted);
        });
        form.show();
        previewStarted = System.currentTimeMillis();
        UITimer.timer(1000 / script.getOutput().frameRate(), true, form, () -> {
            if (!previewPlaying) return;
            long position = (System.currentTimeMillis() - previewStarted) % script.getDurationMs();
            if (position < 100 && currentScene == script.getScenes().size() - 1) currentScene = -1;
            tick(position);
            form.repaint();
        });
    }

    public RenderResult render(Path output, List<NarrationService.PreparedNarration> narration,
                               ProgressListener listener) throws Exception {
        VideoIO io = VideoIO.getVideoIO();
        if (io == null || !VideoIO.isSupported()) throw new IOException("VideoIO is not available on JavaSE; install ffmpeg and ffprobe or configure ffmpeg.dir");
        if (!io.isEncoderSupported(VideoIO.CODEC_AAC)) throw new IOException("AAC encoder is not available in the configured ffmpeg");
        Files.createDirectories(output.toAbsolutePath().getParent());
        int fps = script.getOutput().frameRate();
        long duration = effectiveDuration(narration);
        FramePlan.Plan plan = FramePlan.create(script, duration);
        Path ffmpeg = StagedVideoEncoder.findFfmpeg();
        StagedVideoEncoder.Encoder encoder = StagedVideoEncoder.encoder(ffmpeg);
        Path staging = Files.createTempDirectory(output.toAbsolutePath().getParent(),
                "." + script.getId() + "-" + orientation + "-frames-");
        Path audioRaw = staging.resolve("mixed-audio.s16le");
        boolean complete = false;
        try {
            Display.getInstance().callSeriallyAndWait(() -> form.setVisible(true));
            stageFrames(staging, plan, duration, listener);
            AudioTimeline audio = new AudioTimeline(script, narration);
            if ("videoio".equals(script.getOutput().encodingPipeline())) {
                System.err.println("encode " + orientation + " pipeline=VideoIO"
                        + " renderedFrames=" + plan.frames().size()
                        + " logicalFrames=" + plan.frameCount());
                encodeWithVideoIO(staging, plan, audio, output, fps);
            } else {
                audio.writeRaw(audioRaw);
                StagedVideoEncoder.linkFrames(staging, plan);
                System.err.println("encode " + orientation + " pipeline=staged encoder=" + encoder.name()
                        + " hardware=" + encoder.hardware() + " renderedFrames=" + plan.frames().size()
                        + " logicalFrames=" + plan.frameCount());
                StagedVideoEncoder.encode(ffmpeg, encoder, staging, audioRaw, output, fps,
                        script.getOutput().videoBitRate(), duration);
            }
            complete = true;
        } finally {
            disposeDemo();
            if (complete && !"true".equals(System.getProperty("video.keepFrames"))) {
                StagedVideoEncoder.deleteTree(staging);
            } else {
                System.err.println("kept staged frames at " + staging);
            }
            if (!complete) Files.deleteIfExists(output);
        }
        return new RenderResult(output, orientation, width, height, fps, duration, plan.frameCount());
    }

    /**
     * Encodes a previously captured sparse frame plan through the public VideoIO API. Rendering
     * remains detached from the EDT; only the final frame delivery and audio mux use VideoWriter.
     */
    private void encodeWithVideoIO(Path staging, FramePlan.Plan plan, AudioTimeline audio,
                                   Path output, int fps) throws IOException {
        Path temporary = output.resolveSibling("." + output.getFileName() + ".videoio-"
                + UUID.randomUUID() + ".mp4");
        VideoWriter writer = null;
        boolean closed = false;
        try {
            writer = new VideoWriterBuilder()
                    .path(temporary.toString())
                    .container(VideoIO.CONTAINER_MP4)
                    .width(width)
                    .height(height)
                    .frameRate(fps)
                    .videoCodec(VideoIO.CODEC_H264)
                    .videoBitRate(script.getOutput().videoBitRate())
                    .hasAudio(true)
                    .audioCodec(VideoIO.CODEC_AAC)
                    .sampleRate(AudioTimeline.SAMPLE_RATE)
                    .audioChannels(AudioTimeline.CHANNELS)
                    .build();
            List<FramePlan.Frame> frames = plan.frames();
            for (int item = 0; item < frames.size(); item++) {
                FramePlan.Frame frame = frames.get(item);
                byte[] encoded = Files.readAllBytes(StagedVideoEncoder.asset(staging, item));
                Image image = Image.createImage(encoded, 0, encoded.length);
                long end = item + 1 < frames.size()
                        ? frames.get(item + 1).frameIndex() : plan.frameCount();
                for (long index = frame.frameIndex(); index < end; index++) {
                    writer.writeFrame(image, index * 1000L / fps);
                }
            }
            audio.writeTo(writer);
            writer.close();
            closed = true;
            try {
                Files.move(temporary, output, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException atomicMoveFailure) {
                Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            if (writer != null && !closed) {
                try { writer.close(); } catch (IOException ignored) { }
            }
            Files.deleteIfExists(temporary);
        }
    }

    private void stageFrames(Path staging, FramePlan.Plan plan, long duration,
                             ProgressListener listener) throws IOException {
        int[] cursor = {0};
        Throwable[] frameError = new Throwable[1];
        boolean[] editorPending = new boolean[1];
        while (cursor[0] < plan.frames().size()) {
            editorPending[0] = false;
            Display.getInstance().callSeriallyAndWait(() -> {
                while (cursor[0] < plan.frames().size() && frameError[0] == null) {
                    if (cancelled) {
                        frameError[0] = new IOException("Render cancelled");
                        break;
                    }
                    int assetIndex = cursor[0];
                    FramePlan.Frame planned = plan.frames().get(assetIndex);
                    try {
                        tick(planned.positionMs());
                        saveFrame(paintFrame(), StagedVideoEncoder.asset(staging, assetIndex));
                        cursor[0]++;
                        if (listener != null && (planned.frameIndex() % script.getOutput().frameRate() == 0
                                || cursor[0] == plan.frames().size())) {
                            listener.onProgress(planned.positionMs(), duration,
                                    script.getScenes().get(planned.sceneIndex()).id());
                        }
                        if (hasUnreadyEditors()) {
                            editorPending[0] = true;
                            break;
                        }
                    } catch (Throwable error) {
                        frameError[0] = error;
                    }
                }
            });
            if (frameError[0] != null) {
                throw new IOException("Frame staging failed", frameError[0]);
            }
            if (editorPending[0] && awaitEditorsReady()) {
                int assetIndex = cursor[0] - 1;
                FramePlan.Frame planned = plan.frames().get(assetIndex);
                Display.getInstance().callSeriallyAndWait(() -> {
                    try {
                        tick(planned.positionMs());
                        saveFrame(paintFrame(), StagedVideoEncoder.asset(staging, assetIndex));
                    } catch (Throwable error) {
                        frameError[0] = error;
                    }
                });
                if (frameError[0] != null) {
                    throw new IOException("Code editor frame staging failed at "
                            + planned.positionMs() + " ms", frameError[0]);
                }
            }
        }
    }

    private boolean hasUnreadyEditors() {
        for (Layer layer : layers.values()) {
            if (layer.content instanceof CodeEditor editor && !editor.isEditorReady()) return true;
        }
        return false;
    }

    private static void saveFrame(Image frame, Path path) throws IOException {
        ImageIO imageIO = ImageIO.getImageIO();
        if (imageIO == null || !imageIO.isFormatSupported(ImageIO.FORMAT_JPEG)) {
            throw new IOException("JPEG encoding is unavailable on this JavaSE port");
        }
        try (OutputStream output = Files.newOutputStream(path)) {
            imageIO.save(frame, output, ImageIO.FORMAT_JPEG, 0.92f);
        }
    }

    private long effectiveDuration(List<NarrationService.PreparedNarration> narration) {
        long duration = script.getDurationMs();
        for (NarrationService.PreparedNarration item : narration) {
            duration = Math.max(duration, item.atMs() + item.durationMs());
        }
        return duration;
    }

    private void tick(long positionMs) {
        ScenePosition position = locate(positionMs);
        if (position.index != currentScene) enterScene(position.index);
        VideoScript.Scene scene = script.getScenes().get(position.index);
        VideoAnimationSupport.update(form);
        ReplayPosition replay = replayPosition(scene, position.relativeMs);
        long visualRelative = replay == null ? position.relativeMs : replay.visualRelativeMs();
        if (visualRelative < lastVisualRelativeMs) resetSceneForReplay(position.index);
        if (demoContext != null) demoContext.setTimelinePositionMs(positionMs);
        for (VideoScript.Action action : scene.actions()) {
            long relative = visualRelative;
            if (relative < action.atMs()) continue;
            if ("code.type".equals(action.type())) updateTyping(action, relative);
            else if ("transition".equals(action.type())) updateTransition(action, relative);
            else if ("layer.animate".equals(action.type())) updateLayerAnimation(action, relative);
            else if ("pointer.move".equals(action.type()) || "pointer.click".equals(action.type())) {
                updatePointer(action, relative);
            }
            else if ("bullets.show".equals(action.type()) || "diagram.show".equals(action.type())
                    || "intro.show".equals(action.type()) || "outro.show".equals(action.type())) {
                if (!fired.containsKey(action)) {
                    fire(action, scene);
                    fired.put(action, Boolean.TRUE);
                }
                updateProgressAction(action, relative);
            }
            else if (!fired.containsKey(action)) {
                fire(action, scene);
                fired.put(action, Boolean.TRUE);
            }
        }
        updateReplayBadge(replay);
        lastVisualRelativeMs = visualRelative;
    }

    private void enterScene(int index) {
        VideoAnimationSupport.flush(form);
        disposeDemo();
        for (Layer layer : layers.values()) {
            layer.setVisible(false);
            Container parent = layer.getParent();
            if (parent != null) parent.removeComponent(layer);
        }
        layers.clear();
        fired.clear();
        pointerStarts.clear();
        layerAnimationStarts.clear();
        currentScene = index;
        lastVisualRelativeMs = -1;
        layoutDirty = true;
    }

    /**
     * Rebuilds deterministic interaction state while retaining initialized code editors. Recreating
     * Chromium-backed editors on every reverse sample would otherwise produce blank rewind frames
     * and needless startup work even when the replay window never changes the source listing.
     */
    private void resetSceneForReplay(int index) {
        VideoAnimationSupport.flush(form);
        disposeDemo();
        java.util.Iterator<Map.Entry<String, Layer>> iterator = layers.entrySet().iterator();
        Set<String> retained = new LinkedHashSet<>();
        while (iterator.hasNext()) {
            Map.Entry<String, Layer> entry = iterator.next();
            Layer layer = entry.getValue();
            if (layer.content instanceof CodeEditor) {
                retained.add(entry.getKey());
                continue;
            }
            layer.setVisible(false);
            Container parent = layer.getParent();
            if (parent != null) parent.removeComponent(layer);
            iterator.remove();
        }
        fired.clear();
        pointerStarts.clear();
        layerAnimationStarts.clear();
        VideoScript.Scene scene = script.getScenes().get(index);
        for (VideoScript.Action action : scene.actions()) {
            if ("code.show".equals(action.type()) && retained.contains(action.id())) {
                fired.put(action, Boolean.TRUE);
            }
        }
        currentScene = index;
        lastVisualRelativeMs = -1;
        layoutDirty = true;
    }

    private void fire(VideoScript.Action action, VideoScript.Scene scene) {
        Map<String, Object> values = action.values();
        switch (action.type()) {
            case "text.show" -> {
                String id = id(action, "text");
                String text = VideoScript.string(values.get("text"), "");
                String uiid = VideoScript.string(values.get("uiid"), "VideoBody");
                if (VideoScript.bool(values.get("responsive"), "VideoTitle".equals(uiid))) {
                    putLayer(id, new ResponsiveTextComponent(text,
                            VideoScript.integer(values.get("maxLines"), 3),
                            color(values.get("color"), 0xffffff)), actionBounds(values, scene));
                } else {
                    SpanLabel label = new SpanLabel(text);
                    label.setTextUIID(uiid);
                    putLayer(id, label, actionBounds(values, scene));
                }
            }
            case "text.hide", "layer.hide" -> hide(VideoScript.string(values.get("target"), action.id()));
            case "code.show" -> {
                String id = id(action, "code");
                CodeEditor editor = new VideoCodeEditor(VideoScript.string(values.get("language"), "java"), codeText(values));
                editor.setTheme(VideoScript.string(values.get("theme"), "dark"));
                editor.setReadOnly(VideoScript.bool(values.get("readOnly"), true));
                editor.setShowLineNumbers(VideoScript.bool(values.get("lineNumbers"), true));
                putLayer(id, editor, actionBounds(values, scene));
            }
            case "bullets.show" -> putLayer(progressLayerId(action),
                    new BulletSlideComponent(VideoScript.string(values.get("title"), ""),
                            VideoScript.strings(values.get("items"))), actionBounds(values, scene));
            case "diagram.show" -> putLayer(progressLayerId(action),
                    new DiagramComponent(textOrPath(values)), actionBounds(values, scene));
            case "svg.show" -> putLayer(id(action, "svg"), loadSvg(values), actionBounds(values, scene));
            case "image.show" -> putLayer(id(action, "image"), loadImage(values), actionBounds(values, scene));
            case "intro.show" -> putLayer(progressLayerId(action), new IntroComponent(
                    VideoScript.string(values.get("title"), script.getTitle()),
                    VideoScript.string(values.get("subtitle"), ""),
                    values.containsKey("path") || values.containsKey("paths")
                            ? loadRasterImage(values) : null), actionBounds(values, scene));
            case "outro.show" -> putLayer(progressLayerId(action), new OutroComponent(
                    VideoScript.string(values.get("eyebrow"), "CODENAME ONE"),
                    VideoScript.string(values.get("title"), "Keep going"),
                    VideoScript.string(values.get("subtitle"), ""),
                    VideoScript.string(values.get("prompt"), "")), actionBounds(values, scene));
            case "demo.mount" -> mountDemo(action, scene);
            case "demo.action" -> {
                if (demo == null) throw new IllegalStateException("demo.action used before demo.mount");
                demo.onAction(VideoScript.string(values.get("name"), ""), VideoScript.map(values.get("arguments")));
            }
            case "focus.show" -> {
                String target = VideoScript.string(values.get("target"), "");
                putLayer(id(action, "focus"), new FocusComponent(
                        color(values.get("color"), 0xffc857), VideoScript.string(values.get("label"), "")),
                        relativeBounds(target, values.get("relativeBounds"), actionBounds(values, scene)));
            }
            case "focus.hide", "pointer.hide" -> hide(VideoScript.string(values.get("target"), action.id()));
            case "pointer.show" -> {
                float[] point = point(values);
                PointerComponent pointer = new PointerComponent(
                        VideoScript.string(values.get("style"), "touch"), point[0], point[1]);
                putLayer(id(action, "pointer"), pointer, new VideoScript.Bounds(0, 0, 1, 1));
            }
            default -> { }
        }
    }

    private void mountDemo(VideoScript.Action action, VideoScript.Scene scene) {
        try {
            String className = VideoScript.string(action.values().get("class"), "");
            demo = (DemoScene) Class.forName(className).getDeclaredConstructor().newInstance();
            demoContext = new DemoContext(orientation, width, height, script.getProjectDirectory());
            Component component = demo.create(demoContext);
            component.setUIID("DemoSurface");
            putLayer(id(action, "demo"), component, actionBounds(action.values(), scene));
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Cannot mount compiled demo", ex);
        }
    }

    private void updateTyping(VideoScript.Action action, long relativeMs) {
        String target = VideoScript.string(action.values().get("target"), action.id());
        Layer layer = layers.get(target);
        if (layer == null || !(layer.content instanceof CodeEditor editor)) return;
        String text = codeText(action.values());
        long duration = Math.max(1, action.durationMs());
        float progress = Math.min(1f, Math.max(0f, (relativeMs - action.atMs()) / (float) duration));
        int codePoints = text.codePointCount(0, text.length());
        int visibleCodePoints = Math.min(codePoints, Math.round(codePoints * progress));
        int characters = text.offsetByCodePoints(0, visibleCodePoints);
        String visible = text.substring(0, characters);
        if (!visible.equals(layer.lastText)) {
            editor.setText(visible);
            layer.lastText = visible;
            layoutDirty = true;
        }
        fired.put(action, Boolean.TRUE);
    }

    private void updateTransition(VideoScript.Action action, long relativeMs) {
        String target = VideoScript.string(action.values().get("target"), action.id());
        Layer layer = layers.get(target);
        if (layer == null) return;
        float progress = Math.min(1f, Math.max(0f, (relativeMs - action.atMs()) / (float) Math.max(1, action.durationMs())));
        String easing = VideoScript.string(VideoScript.map(action.values().get("easings")).get(orientation),
                VideoScript.string(action.values().get("easing"), "ease-in-out"));
        progress = ease(progress, easing);
        boolean out = "out".equals(VideoScript.string(action.values().get("direction"), "in"));
        float shown = out ? 1f - progress : progress;
        String effect = VideoScript.string(VideoScript.map(action.values().get("effects")).get(orientation),
                VideoScript.string(action.values().get("effect"), "fade"));
        float hidden = 1f - shown;
        boolean horizontal = effect.contains("left") || effect.contains("right");
        int distance = Math.round(VideoScript.decimal(action.values().get("distance"), 0.12f)
                * (horizontal ? width : height));
        layer.alpha = effect.startsWith("wipe") || "none".equals(effect) ? 255 : Math.round(shown * 255);
        layer.offsetX = effect.contains("left") ? Math.round(hidden * distance)
                : effect.contains("right") ? -Math.round(hidden * distance) : 0;
        layer.offsetY = effect.contains("up") ? Math.round(hidden * distance)
                : effect.contains("down") ? -Math.round(hidden * distance) : 0;
        layer.scale = "zoom-in".equals(effect) ? 0.78f + shown * 0.22f
                : "zoom-out".equals(effect) ? 1.22f - shown * 0.22f
                : "morph".equals(effect) ? 0.68f + shown * 0.32f : 1f;
        if ("morph".equals(effect)) {
            layer.offsetY = Math.round(hidden * height * 0.035f);
            layer.alpha = Math.round(Math.min(1f, shown * 1.35f) * 255);
        }
        layer.reveal = effect.startsWith("wipe-") ? shown : 1f;
        layer.revealDirection = effect.startsWith("wipe-") ? effect.substring("wipe-".length()) : "none";
        layer.setVisible(shown > 0f);
        layoutDirty = true;
        fired.put(action, Boolean.TRUE);
    }

    private static float ease(float progress, String easing) {
        return switch (easing) {
            case "linear" -> progress;
            case "ease-in" -> progress * progress;
            case "ease-out" -> 1f - (1f - progress) * (1f - progress);
            case "spring" -> {
                double value = 1d - Math.exp(-6d * progress) * Math.cos(10d * progress);
                yield Math.min(1f, Math.max(0f, (float) value));
            }
            default -> progress * progress * (3f - 2f * progress);
        };
    }

    private void updatePointer(VideoScript.Action action, long relativeMs) {
        String target = VideoScript.string(action.values().get("target"), action.id());
        Layer layer = layers.get(target);
        if (layer == null || !(layer.content instanceof PointerComponent pointer)) return;
        float progress = Math.min(1f, Math.max(0f,
                (relativeMs - action.atMs()) / (float) Math.max(1, action.durationMs())));
        if ("pointer.move".equals(action.type())) {
            float[] start = pointerStarts.computeIfAbsent(action, ignored -> new float[]{pointer.x, pointer.y});
            float[] destination = point(action.values());
            float eased = 1f - (1f - progress) * (1f - progress);
            pointer.x = start[0] + (destination[0] - start[0]) * eased;
            pointer.y = start[1] + (destination[1] - start[1]) * eased;
        } else {
            pointer.pulse = progress;
        }
        fired.put(action, Boolean.TRUE);
    }

    private void updateLayerAnimation(VideoScript.Action action, long relativeMs) {
        String target = VideoScript.string(action.values().get("target"), action.id());
        Layer layer = layers.get(target);
        if (layer == null) return;
        float progress = Math.min(1f, Math.max(0f,
                (relativeMs - action.atMs()) / (float) Math.max(1, action.durationMs())));
        progress = ease(progress, VideoScript.string(action.values().get("easing"), "ease-in-out"));
        float[] start = layerAnimationStarts.computeIfAbsent(action, ignored -> new float[]{
                layer.offsetX / (float) width, layer.offsetY / (float) height,
                layer.scale, layer.alpha / 255f
        });
        float fromX = VideoScript.decimal(action.values().get("fromX"), start[0]);
        float fromY = VideoScript.decimal(action.values().get("fromY"), start[1]);
        float fromScale = VideoScript.decimal(action.values().get("fromScale"), start[2]);
        float fromAlpha = VideoScript.decimal(action.values().get("fromAlpha"), start[3]);
        float toX = VideoScript.decimal(action.values().get("toX"), fromX);
        float toY = VideoScript.decimal(action.values().get("toY"), fromY);
        float toScale = VideoScript.decimal(action.values().get("toScale"), fromScale);
        float toAlpha = VideoScript.decimal(action.values().get("toAlpha"), fromAlpha);
        layer.offsetX = Math.round((fromX + (toX - fromX) * progress) * width);
        layer.offsetY = Math.round((fromY + (toY - fromY) * progress) * height);
        layer.scale = fromScale + (toScale - fromScale) * progress;
        layer.alpha = Math.round(Math.max(0f, Math.min(1f,
                fromAlpha + (toAlpha - fromAlpha) * progress)) * 255f);
        layer.setVisible(layer.alpha > 0);
        layoutDirty = true;
        fired.put(action, Boolean.TRUE);
    }

    private void updateProgressAction(VideoScript.Action action, long relativeMs) {
        Layer layer = layers.get(progressLayerId(action));
        if (layer == null) return;
        float progress = action.durationMs() == 0 ? 1f : Math.min(1f, Math.max(0f,
                (relativeMs - action.atMs()) / (float) action.durationMs()));
        if (layer.content instanceof BulletSlideComponent bullets) bullets.progress = progress;
        else if (layer.content instanceof DiagramComponent diagram) diagram.progress = progress;
        else if (layer.content instanceof IntroComponent intro) intro.progress = progress;
        else if (layer.content instanceof OutroComponent outro) outro.progress = progress;
    }

    private String codeText(Map<String, Object> values) {
        String path = VideoScript.string(values.get("path"), "");
        if (path.isEmpty()) return VideoScript.string(values.get("text"), "");
        try { return Files.readString(script.getProjectDirectory().resolve(path), StandardCharsets.UTF_8); }
        catch (IOException ex) { throw new IllegalStateException("Cannot read code file " + path, ex); }
    }

    private String textOrPath(Map<String, Object> values) {
        String path = orientedString(values, "path", "paths");
        if (path.isEmpty()) return orientedString(values, "text", "texts");
        try { return Files.readString(script.getProjectDirectory().resolve(path), StandardCharsets.UTF_8); }
        catch (IOException ex) { throw new IllegalStateException("Cannot read text file " + path, ex); }
    }

    private Component loadSvg(Map<String, Object> values) {
        String path = orientedString(values, "path", "paths");
        Path source = script.getProjectDirectory().resolve(path);
        try {
            byte[] data = Files.readAllBytes(source);
            if (Image.isSVGSupported()) {
                Image image = Image.createSVG(source.getParent().toUri().toString(),
                        VideoScript.bool(values.get("animated"), false), data);
                return new SvgComponent(image);
            }
            return new SvgComponent(data);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot load SVG file " + path, ex);
        }
    }

    private Component loadImage(Map<String, Object> values) {
        return new SvgComponent(loadRasterImage(values));
    }

    private Image loadRasterImage(Map<String, Object> values) {
        String path = orientedString(values, "path", "paths");
        Path source = script.getProjectDirectory().resolve(path);
        try {
            return Image.createImage(source.toString());
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot load image file " + path, ex);
        }
    }

    private String orientedString(Map<String, Object> values, String fallbackKey, String variantsKey) {
        String selected = VideoScript.string(VideoScript.map(values.get(variantsKey)).get(orientation), "");
        return selected.isEmpty() ? VideoScript.string(values.get(fallbackKey), "") : selected;
    }

    private VideoScript.Bounds actionBounds(Map<String, Object> values, VideoScript.Scene scene) {
        VideoScript.Bounds fallback = scene.orientationBounds().getOrDefault(orientation, scene.bounds());
        Map<String, Object> overrides = VideoScript.map(values.get("orientation"));
        Object selected = overrides.get(orientation);
        if (selected == null && values.get("bounds") == null) {
            String role = VideoScript.string(values.get("role"), "");
            VideoScript.Bounds composed = compositionBounds(scene.composition().get(orientation), role);
            if (composed != null) return composed;
        }
        return VideoScript.bounds(selected != null ? selected : values.get("bounds"), fallback);
    }

    private VideoScript.Bounds compositionBounds(String preset, String role) {
        if ("code-over-demo".equals(preset)) {
            if ("title".equals(role)) return new VideoScript.Bounds(0.06f, 0.015f, 0.88f, 0.10f);
            if ("code".equals(role)) return new VideoScript.Bounds(0.05f, 0.13f, 0.90f, 0.39f);
            if ("demo".equals(role)) return new VideoScript.Bounds(0.08f, 0.55f, 0.84f, 0.40f);
        } else if ("code-left-demo-right".equals(preset)) {
            if ("title".equals(role)) return new VideoScript.Bounds(0.04f, 0.03f, 0.92f, 0.10f);
            if ("code".equals(role)) return new VideoScript.Bounds(0.04f, 0.16f, 0.52f, 0.78f);
            if ("demo".equals(role)) return new VideoScript.Bounds(0.61f, 0.18f, 0.35f, 0.72f);
        }
        return null;
    }

    private VideoScript.Bounds relativeBounds(String target, Object relativeValue,
                                              VideoScript.Bounds fallback) {
        Layer layer = layers.get(target);
        if (layer == null || VideoScript.map(relativeValue).isEmpty()) return fallback;
        VideoScript.Bounds relative = VideoScript.bounds(relativeValue, new VideoScript.Bounds(0, 0, 1, 1));
        VideoScript.Bounds parent = layer.bounds;
        return new VideoScript.Bounds(parent.x() + relative.x() * parent.width(),
                parent.y() + relative.y() * parent.height(),
                relative.width() * parent.width(), relative.height() * parent.height());
    }

    private float[] point(Map<String, Object> values) {
        float x = VideoScript.decimal(values.get("x"), 0.5f);
        float y = VideoScript.decimal(values.get("y"), 0.5f);
        Layer area = layers.get(VideoScript.string(values.get("area"), ""));
        if (area != null) {
            x = area.bounds.x() + x * area.bounds.width();
            y = area.bounds.y() + y * area.bounds.height();
        }
        return new float[]{x, y};
    }

    private ReplayPosition replayPosition(VideoScript.Scene scene, long sourceRelative) {
        for (VideoScript.Action action : scene.actions()) {
            if (!"replay".equals(action.type())) continue;
            long from = VideoScript.number(action.values().get("fromMs"), 0);
            long to = VideoScript.number(action.values().get("toMs"), 0);
            long rewindDuration = VideoScript.number(action.values().get("rewindDurationMs"), 700);
            float playbackRate = VideoScript.decimal(action.values().get("playbackRate"), 0.65f);
            long replayDuration = (long) Math.ceil((to - from) / playbackRate);
            long elapsed = sourceRelative - action.atMs();
            if (elapsed < 0 || elapsed >= rewindDuration + replayDuration) continue;
            if (elapsed < rewindDuration) {
                int rewindFps = Math.max(1, VideoScript.integer(action.values().get("rewindFps"), 10));
                long sample = Math.max(1, 1000L / rewindFps);
                long sampledElapsed = Math.min(rewindDuration, elapsed / sample * sample);
                float progress = sampledElapsed / (float) rewindDuration;
                return new ReplayPosition(action, to - Math.round((to - from) * progress), true,
                        elapsed / (float) rewindDuration, playbackRate);
            }
            long forwardElapsed = elapsed - rewindDuration;
            return new ReplayPosition(action, Math.min(to, from + Math.round(forwardElapsed * playbackRate)),
                    false, forwardElapsed / (float) replayDuration, playbackRate);
        }
        return null;
    }

    private void updateReplayBadge(ReplayPosition replay) {
        if (replay == null) {
            replayIndicator.setVisible(false);
            rewindIndicator.setVisible(false);
            return;
        }
        if (replay.rewinding()) {
            replayIndicator.setVisible(false);
            rewindIndicator.setVisible(true);
        } else {
            rewindIndicator.setVisible(false);
            String label = VideoScript.string(replay.action().values().get("label"),
                    "Show it again in case you missed it");
            ((BadgeComponent) replayIndicator.content).text = label + "  ·  "
                    + String.format(java.util.Locale.ROOT, "%.2gx", replay.playbackRate());
            replayIndicator.setVisible(true);
        }
    }

    private static int color(Object value, int fallback) {
        String text = VideoScript.string(value, "").replace("#", "");
        try { return text.isBlank() ? fallback : Integer.parseInt(text, 16); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private void putLayer(String id, Component content, VideoScript.Bounds bounds) {
        hide(id);
        Layer layer = new Layer(content, bounds);
        layer.setName(id);
        layers.put(id, layer);
        if (content instanceof FocusComponent) {
            overlayStage.addComponent(0, layer);
        } else if (content instanceof PointerComponent || content instanceof BadgeComponent) {
            overlayStage.add(layer);
        } else {
            stage.addComponent(Math.max(0, stage.getComponentCount() - 1), layer);
        }
        layoutDirty = true;
    }

    private boolean awaitEditorsReady() throws IOException {
        final List<CodeEditor> editors = new java.util.ArrayList<>();
        Display.getInstance().callSeriallyAndWait(() -> {
            for (Layer layer : layers.values()) {
                if (layer.content instanceof CodeEditor editor && !editor.isEditorReady()) editors.add(editor);
            }
        });
        if (editors.isEmpty()) return false;
        CountDownLatch ready = new CountDownLatch(editors.size());
        Display.getInstance().callSeriallyAndWait(() -> {
            for (CodeEditor editor : editors) editor.onReady(ready::countDown);
        });
        try {
            if (!ready.await(15, TimeUnit.SECONDS)) {
                throw new IOException("Timed out waiting for CodeEditor initialization");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for CodeEditor initialization", ex);
        }
        Display.getInstance().callSeriallyAndWait(() -> {
            for (Layer layer : layers.values()) {
                if (layer.content instanceof CodeEditor) layer.cachedEditor = null;
            }
            layoutDirty = true;
        });
        return true;
    }

    private void hide(String id) {
        Layer old = layers.remove(id);
        if (old != null) {
            VideoAnimationSupport.flush(form);
            old.setVisible(false);
            Container parent = old.getParent();
            if (parent != null) parent.removeComponent(old);
            layoutDirty = true;
        }
    }

    private String id(VideoScript.Action action, String fallback) {
        return action.id().isBlank() ? fallback + "-" + layers.size() : action.id();
    }

    private String progressLayerId(VideoScript.Action action) {
        return action.id().isBlank() ? "__progress_" + System.identityHashCode(action) : action.id();
    }

    private Image paintFrame() {
        if (layoutDirty) {
            form.setWidth(width); form.setHeight(height); stage.setWidth(width); stage.setHeight(height);
            form.setShouldCalcPreferredSize(true);
            stage.setShouldCalcPreferredSize(true);
            form.revalidate();
            layoutTree(form);
            layoutDirty = false;
        }
        Image image = Image.createImage(width, height, 0xff10131a);
        form.paintComponent(image.getGraphics(), true);
        return image;
    }

    /**
     * Lays out children added asynchronously by components such as CodeEditor before taking a frame.
     * A normal on-screen repaint performs these nested passes for us, but export deliberately paints
     * into an off-screen image and therefore needs to make the boundary explicit.
     */
    private static void layoutTree(Container container) {
        container.layoutContainer();
        for (Component child : container) {
            if (child instanceof Container nested) layoutTree(nested);
        }
    }

    private ScenePosition locate(long positionMs) {
        long cursor = 0;
        List<VideoScript.Scene> scenes = script.getScenes();
        for (int i = 0; i < scenes.size(); i++) {
            long end = cursor + scenes.get(i).durationMs();
            if (positionMs < end || i + 1 == scenes.size()) return new ScenePosition(i, Math.max(0, positionMs - cursor));
            cursor = end;
        }
        return new ScenePosition(scenes.size() - 1, scenes.get(scenes.size() - 1).durationMs());
    }

    private void disposeDemo() {
        if (demo != null) { demo.dispose(); demo = null; demoContext = null; }
    }

    public record RenderResult(Path path, String orientation, int width, int height,
                               int frameRate, long durationMs, long frameCount) { }
    private record ScenePosition(int index, long relativeMs) { }
    private record ReplayPosition(VideoScript.Action action, long visualRelativeMs,
                                  boolean rewinding, float progress, float playbackRate) { }

    private static final class FillLayout extends Layout {
        public void layoutContainer(Container parent) {
            for (Component child : parent) { child.setX(0); child.setY(0); child.setWidth(parent.getWidth()); child.setHeight(parent.getHeight()); }
        }
        public Dimension getPreferredSize(Container parent) { return new Dimension(parent.getWidth(), parent.getHeight()); }
    }

    private static final class Stage extends Container {
        Stage() { super(new StageLayout()); }
    }

    private static final class StageLayout extends Layout {
        public void layoutContainer(Container parent) {
            for (Component child : parent) {
                if (child instanceof Layer layer) {
                    VideoScript.Bounds b = layer.bounds;
                    int baseWidth = Math.round(b.width() * parent.getWidth());
                    int baseHeight = Math.round(b.height() * parent.getHeight());
                    int scaledWidth = Math.max(1, Math.round(baseWidth * layer.scale));
                    int scaledHeight = Math.max(1, Math.round(baseHeight * layer.scale));
                    child.setX(Math.round(b.x() * parent.getWidth()) + (baseWidth - scaledWidth) / 2 + layer.offsetX);
                    child.setY(Math.round(b.y() * parent.getHeight()) + (baseHeight - scaledHeight) / 2 + layer.offsetY);
                    child.setWidth(scaledWidth);
                    child.setHeight(scaledHeight);
                } else {
                    child.setX(0); child.setY(0); child.setWidth(parent.getWidth()); child.setHeight(parent.getHeight());
                }
            }
        }
        public Dimension getPreferredSize(Container parent) { return new Dimension(parent.getWidth(), parent.getHeight()); }
    }

    private static final class Layer extends Container {
        final Component content;
        final VideoScript.Bounds bounds;
        int alpha = 255;
        int offsetX;
        int offsetY;
        float scale = 1f;
        float reveal = 1f;
        String revealDirection = "none";
        String lastText = "";
        Image cachedEditor;
        String cachedEditorText;
        int cachedWidth;
        int cachedHeight;
        int editorPaints;

        Layer(Component content, VideoScript.Bounds bounds) {
            super(new FillLayout());
            this.content = content;
            this.bounds = bounds;
            setOpaque(false);
            add(content);
        }

        @Override public void paint(Graphics graphics) {
            int old = graphics.getAlpha();
            int clipX = graphics.getClipX();
            int clipY = graphics.getClipY();
            int clipWidth = graphics.getClipWidth();
            int clipHeight = graphics.getClipHeight();
            if (reveal < 1f) {
                int visibleWidth = Math.max(1, Math.round(getWidth() * reveal));
                int visibleHeight = Math.max(1, Math.round(getHeight() * reveal));
                int x = getAbsoluteX();
                int y = getAbsoluteY();
                if ("right".equals(revealDirection)) x += getWidth() - visibleWidth;
                if ("down".equals(revealDirection)) y += getHeight() - visibleHeight;
                if ("left".equals(revealDirection) || "right".equals(revealDirection)) {
                    graphics.clipRect(x, y, visibleWidth, getHeight());
                } else {
                    graphics.clipRect(x, y, getWidth(), visibleHeight);
                }
            }
            graphics.setAlpha(alpha);
            if (content instanceof CodeEditor) {
                if (editorPaints < 3 || cachedEditor == null || cachedWidth != getWidth() || cachedHeight != getHeight()
                        || !lastText.equals(cachedEditorText)) {
                    editorPaints++;
                    cachedWidth = getWidth();
                    cachedHeight = getHeight();
                    cachedEditorText = lastText;
                    cachedEditor = Image.createImage(Math.max(1, cachedWidth), Math.max(1, cachedHeight), 0x00000000);
                    Graphics cachedGraphics = cachedEditor.getGraphics();
                    cachedGraphics.translate(-getAbsoluteX(), -getAbsoluteY());
                    super.paint(cachedGraphics);
                    cachedGraphics.translate(getAbsoluteX(), getAbsoluteY());
                }
                graphics.drawImage(cachedEditor, getAbsoluteX(), getAbsoluteY());
            } else {
                super.paint(graphics);
            }
            graphics.setAlpha(old);
            graphics.setClip(clipX, clipY, clipWidth, clipHeight);
        }
    }

    /** Pure CN1 overlay so it is captured by off-screen JavaSE rendering. */
    private static final class FocusComponent extends Component {
        private final int color;
        private final String label;

        FocusComponent(int color, String label) {
            this.color = color;
            this.label = label;
            setOpaque(false);
        }

        @Override public void paint(Graphics graphics) {
            int x = getX();
            int y = getY();
            int width = getWidth();
            int height = getHeight();
            int oldAlpha = graphics.getAlpha();
            Font oldFont = graphics.getFont();
            graphics.setFont(Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL));
            graphics.setColor(color);
            graphics.setAlpha(45);
            graphics.fillRect(x, y, width, height);
            graphics.setAlpha(235);
            for (int i = 0; i < 4; i++) graphics.drawRect(x + i, y + i, width - i * 2 - 1, height - i * 2 - 1);
            if (!label.isBlank()) {
                graphics.setAlpha(255);
                int padding = 10;
                int badgeHeight = graphics.getFont().getHeight() + padding;
                int badgeWidth = Math.min(width, graphics.getFont().stringWidth(label) + padding * 2);
                // Focus content is clipped to its layer, so keep the caption inside the
                // lower-right edge. Source blocks tend to end on the left, which keeps
                // the caption from obscuring either the first line or closing braces.
                int badgeX = x + Math.max(0, width - badgeWidth);
                int badgeY = y + Math.max(6, height - badgeHeight - 6);
                graphics.setAlpha(225);
                graphics.fillRect(badgeX, badgeY, badgeWidth, badgeHeight);
                graphics.setColor(0xffffff);
                graphics.setAlpha(255);
                graphics.drawString(label, badgeX + padding, badgeY + padding / 2);
            }
            graphics.setFont(oldFont);
            graphics.setAlpha(oldAlpha);
        }
    }

    /** Short kinetic welcome graphic intended to lead directly into the first slide. */
    private static final class IntroComponent extends Component {
        private final String title;
        private final String subtitle;
        private final Image logo;
        private float progress;

        IntroComponent(String title, String subtitle, Image logo) {
            this.title = title;
            this.subtitle = subtitle;
            this.logo = logo;
            setOpaque(false);
        }

        @Override public void paint(Graphics graphics) {
            int x = getX();
            int y = getY();
            int width = getWidth();
            int height = getHeight();
            int oldAlpha = graphics.getAlpha();
            Font oldFont = graphics.getFont();
            graphics.setColor(0x171c26);
            graphics.setAlpha(248);
            graphics.fillRoundRect(x, y, width, height, 30, 30);

            float markProgress = ease(Math.min(1f, progress / 0.52f), "ease-out");
            int centerX = x + width / 2;
            int centerY = y + Math.round(height * (logo == null ? 0.34f : 0.31f));
            int unit = Math.max(18, Math.min(width, height) / 12);
            if (logo != null) {
                float scale = Math.min(width * 0.62f / logo.getWidth(), height * 0.17f / logo.getHeight());
                int logoWidth = Math.round(logo.getWidth() * scale * markProgress);
                int logoHeight = Math.round(logo.getHeight() * scale * markProgress);
                graphics.setAlpha(Math.round(markProgress * 255));
                if (logoWidth > 0 && logoHeight > 0) {
                    graphics.drawImage(logo, centerX - logoWidth / 2, centerY - logoHeight / 2,
                            logoWidth, logoHeight);
                }
            } else {
                int travel = Math.max(unit * 2, width / 7);
                int leftX = centerX - unit * 2 - Math.round((1f - markProgress) * travel);
                int rightX = centerX + unit + Math.round((1f - markProgress) * travel);

                graphics.setColor(0x50d8ff);
                graphics.setAlpha(Math.round(markProgress * 255));
                graphics.fillRoundRect(leftX, centerY - unit / 2, unit * 2, unit, unit / 2, unit / 2);
                graphics.fillArc(rightX, centerY - unit / 2, unit, unit, 0, 360);
                graphics.setColor(0xffc857);
                graphics.fillRoundRect(centerX - unit / 2, centerY - unit / 2,
                        unit, unit, unit / 4, unit / 4);
                graphics.setColor(0x50d8ff);
                int beam = Math.round(width * 0.22f * markProgress);
                graphics.fillRect(centerX - beam, centerY + unit, beam * 2, Math.max(3, unit / 10));
            }

            float titleProgress = ease(Math.min(1f, Math.max(0f, (progress - 0.20f) / 0.48f)), "ease-out");
            TextBlock titleBlock = fitText(title, Math.round(width * 0.88f), Math.round(height * 0.22f),
                    Math.max(24, Math.min(height * 0.105f, width * 0.095f)), 18,
                    Font.STYLE_BOLD, 2);
            graphics.setFont(titleBlock.font());
            graphics.setColor(0xf3f7fb);
            graphics.setAlpha(Math.round(titleProgress * 255));
            int titleY = centerY + (logo == null ? unit * 2 : unit + Math.round(height * 0.08f));
            for (String line : titleBlock.lines()) {
                graphics.drawString(line, centerX - titleBlock.font().stringWidth(line) / 2,
                        titleY + Math.round((1f - titleProgress) * unit));
                titleY += titleBlock.font().getHeight();
            }

            if (!subtitle.isBlank()) {
                float subtitleProgress = ease(Math.min(1f, Math.max(0f, (progress - 0.48f) / 0.40f)), "ease-out");
                TextBlock subtitleBlock = fitText(subtitle, Math.round(width * 0.82f), Math.round(height * 0.14f),
                        Math.max(16, Math.min(height * 0.048f, width * 0.045f)), 13,
                        Font.STYLE_PLAIN, 2);
                graphics.setFont(subtitleBlock.font());
                graphics.setColor(0x9eabbc);
                graphics.setAlpha(Math.round(subtitleProgress * 255));
                int subtitleY = titleY + Math.max(8, unit / 3);
                for (String line : subtitleBlock.lines()) {
                    graphics.drawString(line, centerX - subtitleBlock.font().stringWidth(line) / 2, subtitleY);
                    subtitleY += subtitleBlock.font().getHeight();
                }
            }
            graphics.setFont(oldFont);
            graphics.setAlpha(oldAlpha);
        }
    }

    /** End-screen-safe close with a topic-specific next step and no simulated platform controls. */
    private static final class OutroComponent extends Component {
        private final String eyebrow;
        private final String title;
        private final String subtitle;
        private final String prompt;
        private float progress;

        OutroComponent(String eyebrow, String title, String subtitle, String prompt) {
            this.eyebrow = eyebrow;
            this.title = title;
            this.subtitle = subtitle;
            this.prompt = prompt;
            setOpaque(false);
        }

        @Override public void paint(Graphics graphics) {
            int x = getX();
            int y = getY();
            int width = getWidth();
            int height = getHeight();
            boolean portrait = height > width;
            int oldAlpha = graphics.getAlpha();
            Font oldFont = graphics.getFont();
            float eased = ease(Math.min(1f, progress), "ease-out");

            graphics.setColor(0x10151f);
            graphics.setAlpha(255);
            graphics.fillRect(x, y, width, height);

            int unit = Math.max(14, Math.min(width, height) / 34);
            int contentX = x + (portrait ? unit * 2 : unit * 2);
            int contentY = y + (portrait ? unit * 5 : unit * 3);
            int contentWidth = portrait ? width - unit * 4 : Math.round(width * 0.48f);

            graphics.setAlpha(Math.round(eased * 255));
            graphics.setColor(0x50d8ff);
            graphics.fillRoundRect(contentX, contentY, unit * 3, Math.max(5, unit / 3),
                    Math.max(3, unit / 6), Math.max(3, unit / 6));
            graphics.setColor(0xffc857);
            graphics.fillArc(contentX + unit * 4, contentY - unit / 3, unit, unit, 0, 360);

            Font eyebrowFont = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL)
                    .derive(Math.max(15, portrait ? width * 0.034f : height * 0.027f), Font.STYLE_BOLD);
            graphics.setFont(eyebrowFont);
            graphics.setColor(0x9eabbc);
            graphics.drawString(eyebrow, contentX, contentY + unit * 2);

            TextBlock titleBlock = fitText(title, contentWidth,
                    portrait ? Math.round(height * 0.30f) : Math.round(height * 0.38f),
                    Math.max(28, portrait ? width * 0.105f : height * 0.105f), 22,
                    Font.STYLE_BOLD, portrait ? 4 : 3);
            graphics.setFont(titleBlock.font());
            graphics.setColor(0xf3f7fb);
            int cursorY = contentY + unit * 4 + Math.round((1f - eased) * unit);
            for (String line : titleBlock.lines()) {
                graphics.drawString(line, contentX, cursorY);
                cursorY += titleBlock.font().getHeight();
            }

            if (!subtitle.isBlank()) {
                TextBlock subtitleBlock = fitText(subtitle, contentWidth,
                        portrait ? Math.round(height * 0.18f) : Math.round(height * 0.22f),
                        Math.max(18, portrait ? width * 0.054f : height * 0.048f), 15,
                        Font.STYLE_PLAIN, portrait ? 4 : 3);
                graphics.setFont(subtitleBlock.font());
                graphics.setColor(0xb8c2d0);
                cursorY += unit;
                for (String line : subtitleBlock.lines()) {
                    graphics.drawString(line, contentX, cursorY);
                    cursorY += subtitleBlock.font().getHeight();
                }
            }

            if (!prompt.isBlank()) {
                int promptWidth = portrait ? width - unit * 4 : Math.round(width * 0.48f);
                TextBlock promptBlock = fitText(prompt, promptWidth,
                        portrait ? Math.round(height * 0.16f) : Math.round(height * 0.18f),
                        Math.max(16, portrait ? width * 0.046f : height * 0.038f), 13,
                        Font.STYLE_BOLD, 3);
                graphics.setFont(promptBlock.font());
                graphics.setColor(0xffc857);
                int promptY = portrait ? y + Math.round(height * 0.66f)
                        : y + height - unit * 5;
                for (String line : promptBlock.lines()) {
                    graphics.drawString(line, contentX, promptY);
                    promptY += promptBlock.font().getHeight();
                }
            }

            if (!portrait) {
                int reserveX = x + Math.round(width * 0.57f);
                int reserveY = y + Math.round(height * 0.16f);
                int reserveWidth = Math.round(width * 0.37f);
                int reserveHeight = Math.round(height * 0.68f);
                graphics.setColor(0x202938);
                graphics.setAlpha(Math.round(eased * 105));
                graphics.fillRoundRect(reserveX, reserveY, reserveWidth, reserveHeight, 34, 34);
                graphics.setColor(0x50d8ff);
                graphics.setAlpha(Math.round(eased * 35));
                for (int inset = 0; inset < 3; inset++) {
                    graphics.drawRoundRect(reserveX + inset, reserveY + inset,
                            reserveWidth - inset * 2, reserveHeight - inset * 2, 34, 34);
                }
            }

            graphics.setFont(oldFont);
            graphics.setAlpha(oldAlpha);
        }
    }

    /** Auto-fits and wraps titles without relying on a fixed-height SpanLabel. */
    private static final class ResponsiveTextComponent extends Component {
        private final String text;
        private final int maxLines;
        private final int color;

        ResponsiveTextComponent(String text, int maxLines, int color) {
            this.text = text;
            this.maxLines = Math.max(1, maxLines);
            this.color = color;
            setOpaque(false);
        }

        @Override public void paint(Graphics graphics) {
            int padding = Math.max(4, Math.min(getWidth(), getHeight()) / 30);
            TextBlock block = fitText(text, getWidth() - padding * 2, getHeight() - padding * 2,
                    Math.max(18, Math.min(getHeight() * 0.72f, getWidth() * 0.11f)), 14,
                    Font.STYLE_BOLD, maxLines);
            Font oldFont = graphics.getFont();
            int oldAlpha = graphics.getAlpha();
            graphics.setFont(block.font());
            graphics.setColor(color);
            graphics.setAlpha(255);
            int lineHeight = block.font().getHeight();
            int y = getY() + (getHeight() - block.lines().size() * lineHeight) / 2;
            for (String line : block.lines()) {
                graphics.drawString(line, getX() + (getWidth() - block.font().stringWidth(line)) / 2, y);
                y += lineHeight;
            }
            graphics.setFont(oldFont);
            graphics.setAlpha(oldAlpha);
        }
    }

    /** A presentation slide whose bullet points are revealed over the action duration. */
    private static final class BulletSlideComponent extends Component {
        private final String title;
        private final List<String> items;
        private float progress;

        BulletSlideComponent(String title, List<String> items) {
            this.title = title;
            this.items = List.copyOf(items);
            setOpaque(false);
        }

        @Override public void paint(Graphics graphics) {
            int x = getX();
            int y = getY();
            int width = getWidth();
            int height = getHeight();
            int oldAlpha = graphics.getAlpha();
            Font oldFont = graphics.getFont();
            int padding = Math.max(18, Math.min(width, height) / 18);
            graphics.setColor(0x171c26);
            graphics.setAlpha(242);
            graphics.fillRoundRect(x, y, width, height, 28, 28);
            int contentWidth = width - padding * 2;
            TextBlock titleBlock = fitText(title, contentWidth, Math.round(height * 0.28f),
                    Math.max(26, Math.min(height * 0.125f, width * 0.11f)), 20,
                    Font.STYLE_BOLD, 3);
            graphics.setColor(0x50d8ff);
            graphics.setAlpha(255);
            graphics.setFont(titleBlock.font());
            int cursorY = y + padding;
            for (String line : titleBlock.lines()) {
                graphics.drawString(line, x + padding, cursorY);
                cursorY += titleBlock.font().getHeight();
            }
            cursorY += Math.max(10, padding / 2);
            int visible = progress >= 1f ? items.size() : Math.min(items.size(),
                    (int) Math.ceil(items.size() * progress));
            int bulletReserve = Math.max(28, width / 24);
            int itemWidth = contentWidth - bulletReserve;
            int remainingHeight = Math.max(1, y + height - padding - cursorY);
            List<TextBlock> itemBlocks = fitItems(items, itemWidth, remainingHeight,
                    Math.max(20, Math.min(height * 0.065f, width * 0.072f)));
            for (int i = 0; i < visible; i++) {
                int itemProgress = Math.round(items.size() * progress) - i;
                graphics.setAlpha(itemProgress <= 0 ? 135 : 255);
                TextBlock block = itemBlocks.get(i);
                graphics.setFont(block.font());
                int radius = Math.max(5, block.font().getHeight() / 7);
                graphics.setColor(i % 2 == 0 ? 0xffc857 : 0x50d8ff);
                graphics.fillArc(x + padding, cursorY + block.font().getHeight() / 2 - radius,
                        radius * 2, radius * 2, 0, 360);
                graphics.setColor(0xf3f7fb);
                for (String line : block.lines()) {
                    graphics.drawString(line, x + padding + bulletReserve, cursorY);
                    cursorY += block.font().getHeight();
                }
                cursorY += Math.max(8, block.font().getHeight() / 3);
            }
            graphics.setFont(oldFont);
            graphics.setAlpha(oldAlpha);
        }
    }

    private static List<TextBlock> fitItems(List<String> items, int width, int height, float maximumSize) {
        for (float size = maximumSize; size >= 12; size -= 2) {
            List<TextBlock> blocks = new ArrayList<>();
            int used = 0;
            for (String item : items) {
                TextBlock block = textBlock(item, width, size, Font.STYLE_PLAIN);
                blocks.add(block);
                used += block.lines().size() * block.font().getHeight() + Math.max(8, block.font().getHeight() / 3);
            }
            if (used <= height || size <= 12) return blocks;
        }
        return List.of();
    }

    private static TextBlock fitText(String text, int width, int height, float maximumSize,
                                     float minimumSize, int style, int maxLines) {
        for (float size = maximumSize; size >= minimumSize; size -= 2) {
            TextBlock block = textBlock(text, width, size, style);
            if (block.lines().size() <= maxLines
                    && block.lines().size() * block.font().getHeight() <= height) return block;
        }
        return textBlock(text, width, minimumSize, style);
    }

    private static TextBlock textBlock(String text, int width, float size, int style) {
        Font font = Font.createSystemFont(Font.FACE_SYSTEM, style, Font.SIZE_MEDIUM).derive(size, style);
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\\R", -1)) {
            if (paragraph.isBlank()) { lines.add(""); continue; }
            String current = "";
            for (String word : paragraph.trim().split("\\s+")) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                if (!current.isEmpty() && font.stringWidth(candidate) > width) {
                    lines.add(current);
                    current = word;
                } else {
                    current = candidate;
                }
            }
            if (!current.isEmpty()) lines.add(current);
        }
        if (lines.isEmpty()) lines.add("");
        return new TextBlock(font, List.copyOf(lines));
    }

    private record TextBlock(Font font, List<String> lines) { }

    /** A small Mermaid-compatible flowchart renderer for animated explanatory diagrams. */
    private static final class DiagramComponent extends Component {
        private final List<DiagramNode> nodes = new ArrayList<>();
        private final List<DiagramEdge> edges = new ArrayList<>();
        private final boolean leftToRight;
        private float progress;

        DiagramComponent(String source) {
            setUIID("VideoStage");
            String[] lines = source.replace(";", "\n").split("\\R");
            boolean horizontal = true;
            Map<String, String> labels = new LinkedHashMap<>();
            for (String raw : lines) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("%%")) continue;
                if (line.startsWith("flowchart") || line.startsWith("graph")) {
                    horizontal = line.endsWith("LR") || line.endsWith("RL");
                    continue;
                }
                int arrow = line.indexOf("-->");
                if (arrow < 0) continue;
                DiagramNode from = parseNode(line.substring(0, arrow).trim());
                DiagramNode to = parseNode(line.substring(arrow + 3).trim());
                labels.putIfAbsent(from.id(), from.label());
                labels.putIfAbsent(to.id(), to.label());
                edges.add(new DiagramEdge(from.id(), to.id()));
            }
            leftToRight = horizontal;
            for (Map.Entry<String, String> entry : labels.entrySet()) {
                nodes.add(new DiagramNode(entry.getKey(), entry.getValue()));
            }
        }

        private static DiagramNode parseNode(String token) {
            int marker = token.length();
            for (char value : new char[]{'[', '(', '{'}) {
                int found = token.indexOf(value);
                if (found >= 0) marker = Math.min(marker, found);
            }
            String id = token.substring(0, marker).trim();
            String label = id;
            if (marker < token.length()) {
                int end = Math.max(token.lastIndexOf(']'), Math.max(token.lastIndexOf(')'), token.lastIndexOf('}')));
                if (end > marker) label = token.substring(marker + 1, end).replace("\"", "").trim();
            }
            return new DiagramNode(id, label);
        }

        @Override public void paint(Graphics graphics) {
            if (nodes.isEmpty()) return;
            int x = getX();
            int y = getY();
            int width = getWidth();
            int height = getHeight();
            int oldAlpha = graphics.getAlpha();
            Font oldFont = graphics.getFont();
            Font font = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM)
                    .derive(Math.max(17, Math.min(width, height) * 0.045f), Font.STYLE_BOLD);
            graphics.setFont(font);
            int visibleEdges = Math.min(edges.size(), Math.round(edges.size() * progress));
            Set<String> visibleNodes = new LinkedHashSet<>();
            visibleNodes.add(nodes.get(0).id());
            for (int i = 0; i < visibleEdges; i++) {
                visibleNodes.add(edges.get(i).from());
                visibleNodes.add(edges.get(i).to());
            }
            if (progress >= 1f) for (DiagramNode node : nodes) visibleNodes.add(node.id());
            Map<String, int[]> positions = new LinkedHashMap<>();
            int boxWidth = leftToRight ? Math.max(110, width / Math.max(3, nodes.size() + 1)) : Math.max(180, width / 3);
            int boxHeight = Math.max(58, height / 7);
            for (int i = 0; i < nodes.size(); i++) {
                int cx = leftToRight ? x + (i + 1) * width / (nodes.size() + 1) : x + width / 2;
                int cy = leftToRight ? y + height / 2 : y + (i + 1) * height / (nodes.size() + 1);
                positions.put(nodes.get(i).id(), new int[]{cx, cy});
            }
            graphics.setColor(0x50d8ff);
            graphics.setAlpha(230);
            for (int i = 0; i < visibleEdges; i++) {
                int[] from = positions.get(edges.get(i).from());
                int[] to = positions.get(edges.get(i).to());
                if (from == null || to == null) continue;
                int x1 = from[0] + (leftToRight ? boxWidth / 2 : 0);
                int y1 = from[1] + (leftToRight ? 0 : boxHeight / 2);
                int x2 = to[0] - (leftToRight ? boxWidth / 2 : 0);
                int y2 = to[1] - (leftToRight ? 0 : boxHeight / 2);
                for (int line = -1; line <= 1; line++) graphics.drawLine(x1 + line, y1, x2 + line, y2);
                if (leftToRight) graphics.fillTriangle(x2, y2, x2 - 14, y2 - 9, x2 - 14, y2 + 9);
                else graphics.fillTriangle(x2, y2, x2 - 9, y2 - 14, x2 + 9, y2 - 14);
            }
            for (DiagramNode node : nodes) {
                if (!visibleNodes.contains(node.id())) continue;
                int[] center = positions.get(node.id());
                int bx = center[0] - boxWidth / 2;
                int by = center[1] - boxHeight / 2;
                graphics.setColor(0x171c26);
                graphics.setAlpha(245);
                graphics.fillRoundRect(bx, by, boxWidth, boxHeight, 22, 22);
                graphics.setColor(0xffc857);
                graphics.setAlpha(255);
                for (int border = 0; border < 3; border++) {
                    graphics.drawRoundRect(bx + border, by + border, boxWidth - border * 2,
                            boxHeight - border * 2, 22, 22);
                }
                int tx = center[0] - font.stringWidth(node.label()) / 2;
                graphics.setColor(0xf3f7fb);
                graphics.drawString(node.label(), tx, center[1] - font.getHeight() / 2);
            }
            graphics.setFont(oldFont);
            graphics.setAlpha(oldAlpha);
        }
    }

    private record DiagramNode(String id, String label) { }
    private record DiagramEdge(String from, String to) { }

    /** Fits an SVG asset within its layer, with a JavaSE-safe vector fallback. */
    private static final class SvgComponent extends Component {
        private final Image image;
        private final Document document;
        private final float viewWidth;
        private final float viewHeight;
        private final Map<String, Map<String, String>> classStyles;

        SvgComponent(Image image) {
            this.image = image;
            document = null;
            classStyles = Map.of();
            viewWidth = image.getWidth();
            viewHeight = image.getHeight();
            setUIID("VideoStage");
            setOpaque(false);
        }

        SvgComponent(byte[] data) throws Exception {
            image = null;
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(data));
            classStyles = parseClassStyles(document);
            Element root = document.getDocumentElement();
            String[] viewBox = root.getAttribute("viewBox").trim().split("[ ,]+");
            viewWidth = viewBox.length == 4 ? svgNumber(viewBox[2], 1f)
                    : svgNumber(root.getAttribute("width"), 1f);
            viewHeight = viewBox.length == 4 ? svgNumber(viewBox[3], 1f)
                    : svgNumber(root.getAttribute("height"), 1f);
            setUIID("VideoStage");
            setOpaque(false);
        }

        @Override public void paint(Graphics graphics) {
            float scale = Math.min(getWidth() / viewWidth, getHeight() / viewHeight);
            int width = Math.max(1, Math.round(viewWidth * scale));
            int height = Math.max(1, Math.round(viewHeight * scale));
            int x = getX() + (getWidth() - width) / 2;
            int y = getY() + (getHeight() - height) / 2;
            if (image != null) {
                graphics.drawImage(image, x, y, width, height);
            } else {
                Font oldFont = graphics.getFont();
                int oldAlpha = graphics.getAlpha();
                paintSvgChildren(graphics, document.getDocumentElement(), SvgStyle.DEFAULT,
                        classStyles, x, y, scale);
                graphics.setFont(oldFont);
                graphics.setAlpha(oldAlpha);
            }
        }

        private static void paintSvgChildren(Graphics graphics, Element parent, SvgStyle inherited,
                                             Map<String, Map<String, String>> classStyles,
                                             int offsetX, int offsetY, float scale) {
            SvgStyle style = inherited.with(parent, classStyles);
            int[] translation = svgTranslation(parent, scale);
            int translatedX = offsetX + translation[0];
            int translatedY = offsetY + translation[1];
            String tag = parent.getTagName();
            switch (tag) {
                case "rect" -> paintRect(graphics, parent, style, translatedX, translatedY, scale);
                case "line" -> paintLine(graphics, parent, style, translatedX, translatedY, scale);
                case "polygon", "polyline" -> paintPolygon(graphics, parent, style, translatedX, translatedY,
                        scale, "polygon".equals(tag));
                case "circle" -> paintCircle(graphics, parent, style, translatedX, translatedY, scale);
                case "text" -> paintText(graphics, parent, style, translatedX, translatedY, scale);
                default -> { }
            }
            NodeList children = parent.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child instanceof Element element) {
                    paintSvgChildren(graphics, element, style, classStyles,
                            translatedX, translatedY, scale);
                }
            }
        }

        /** Supports the translate transform used by deterministic diagram assets. */
        private static int[] svgTranslation(Element element, float scale) {
            String transform = element.getAttribute("transform").trim();
            if (!transform.startsWith("translate(") || !transform.endsWith(")")) {
                return new int[]{0, 0};
            }
            String value = transform.substring(10, transform.length() - 1).trim();
            String[] coordinates = value.split("[ ,]+", 2);
            float x = coordinates.length > 0 ? svgNumber(coordinates[0], 0f) : 0f;
            float y = coordinates.length > 1 ? svgNumber(coordinates[1], 0f) : 0f;
            return new int[]{Math.round(x * scale), Math.round(y * scale)};
        }

        private static void paintRect(Graphics graphics, Element element, SvgStyle style,
                                      int ox, int oy, float scale) {
            int x = ox + Math.round(svgNumber(element.getAttribute("x"), 0) * scale);
            int y = oy + Math.round(svgNumber(element.getAttribute("y"), 0) * scale);
            int width = Math.round(svgNumber(element.getAttribute("width"), 0) * scale);
            int height = Math.round(svgNumber(element.getAttribute("height"), 0) * scale);
            int radius = Math.round(svgNumber(element.getAttribute("rx"), 0) * scale);
            if (style.fill() >= 0) {
                graphics.setColor(style.fill());
                if (radius > 0) graphics.fillRoundRect(x, y, width, height, radius * 2, radius * 2);
                else graphics.fillRect(x, y, width, height);
            }
            if (style.stroke() >= 0) {
                graphics.setColor(style.stroke());
                int stroke = Math.max(1, Math.round(style.strokeWidth() * scale));
                for (int i = 0; i < stroke; i++) {
                    if (radius > 0) graphics.drawRoundRect(x + i, y + i, width - i * 2,
                            height - i * 2, radius * 2, radius * 2);
                    else graphics.drawRect(x + i, y + i, width - i * 2, height - i * 2);
                }
            }
        }

        private static void paintLine(Graphics graphics, Element element, SvgStyle style,
                                      int ox, int oy, float scale) {
            if (style.stroke() < 0) return;
            int x1 = ox + Math.round(svgNumber(element.getAttribute("x1"), 0) * scale);
            int y1 = oy + Math.round(svgNumber(element.getAttribute("y1"), 0) * scale);
            int x2 = ox + Math.round(svgNumber(element.getAttribute("x2"), 0) * scale);
            int y2 = oy + Math.round(svgNumber(element.getAttribute("y2"), 0) * scale);
            graphics.setColor(style.stroke());
            int stroke = Math.max(1, Math.round(style.strokeWidth() * scale));
            for (int i = -stroke / 2; i <= stroke / 2; i++) graphics.drawLine(x1, y1 + i, x2, y2 + i);
        }

        private static void paintPolygon(Graphics graphics, Element element, SvgStyle style,
                                         int ox, int oy, float scale, boolean closed) {
            String[] points = element.getAttribute("points").trim().split("[ ,]+");
            int count = points.length / 2;
            if (count < 2) return;
            int[] xs = new int[count];
            int[] ys = new int[count];
            for (int i = 0; i < count; i++) {
                xs[i] = ox + Math.round(svgNumber(points[i * 2], 0) * scale);
                ys[i] = oy + Math.round(svgNumber(points[i * 2 + 1], 0) * scale);
            }
            if (closed && style.fill() >= 0) {
                graphics.setColor(style.fill());
                graphics.fillPolygon(xs, ys, count);
            }
            if (style.stroke() >= 0) {
                graphics.setColor(style.stroke());
                for (int i = 1; i < count; i++) graphics.drawLine(xs[i - 1], ys[i - 1], xs[i], ys[i]);
                if (closed) graphics.drawLine(xs[count - 1], ys[count - 1], xs[0], ys[0]);
            }
        }

        private static void paintCircle(Graphics graphics, Element element, SvgStyle style,
                                        int ox, int oy, float scale) {
            int radius = Math.round(svgNumber(element.getAttribute("r"), 0) * scale);
            int x = ox + Math.round(svgNumber(element.getAttribute("cx"), 0) * scale) - radius;
            int y = oy + Math.round(svgNumber(element.getAttribute("cy"), 0) * scale) - radius;
            if (style.fill() >= 0) { graphics.setColor(style.fill()); graphics.fillArc(x, y, radius * 2, radius * 2, 0, 360); }
            if (style.stroke() >= 0) { graphics.setColor(style.stroke()); graphics.drawArc(x, y, radius * 2, radius * 2, 0, 360); }
        }

        private static void paintText(Graphics graphics, Element element, SvgStyle style,
                                      int ox, int oy, float scale) {
            String text = element.getTextContent().trim();
            if (text.isEmpty() || style.fill() < 0) return;
            Font font = Font.createSystemFont(Font.FACE_SYSTEM,
                    style.bold() ? Font.STYLE_BOLD : Font.STYLE_PLAIN, Font.SIZE_MEDIUM)
                    .derive(Math.max(8, style.fontSize() * scale), style.bold() ? Font.STYLE_BOLD : Font.STYLE_PLAIN);
            graphics.setFont(font);
            graphics.setColor(style.fill());
            int x = ox + Math.round(svgNumber(element.getAttribute("x"), 0) * scale);
            int y = oy + Math.round(svgNumber(element.getAttribute("y"), 0) * scale) - font.getHeight();
            if ("middle".equals(style.anchor())) x -= font.stringWidth(text) / 2;
            else if ("end".equals(style.anchor())) x -= font.stringWidth(text);
            graphics.drawString(text, x, y);
        }

        private static float svgNumber(String text, float fallback) {
            if (text == null || text.isBlank()) return fallback;
            try { return Float.parseFloat(text.trim().replace("px", "")); }
            catch (NumberFormatException ignored) { return fallback; }
        }

        private record SvgStyle(int fill, int stroke, float strokeWidth, float fontSize,
                                boolean bold, String anchor) {
            static final SvgStyle DEFAULT = new SvgStyle(0x000000, -1, 1f, 16f, false, "start");

            SvgStyle with(Element element, Map<String, Map<String, String>> classStyles) {
                Map<String, String> inline = new LinkedHashMap<>();
                for (String className : element.getAttribute("class").trim().split("\\s+")) {
                    inline.putAll(classStyles.getOrDefault(className, Map.of()));
                }
                applyDeclarations(inline, element.getAttribute("style"));
                return new SvgStyle(svgColor(attribute(element, inline, "fill"), fill),
                        svgColor(attribute(element, inline, "stroke"), stroke),
                        svgNumber(attribute(element, inline, "stroke-width"), strokeWidth),
                        svgNumber(attribute(element, inline, "font-size"), fontSize),
                        "bold".equals(attribute(element, inline, "font-weight"))
                                || svgNumber(attribute(element, inline, "font-weight"), bold ? 700 : 400) >= 600,
                        value(attribute(element, inline, "text-anchor"), anchor));
            }

            private static String attribute(Element element, Map<String, String> inline, String name) {
                if (element.hasAttribute(name)) return element.getAttribute(name);
                return inline.getOrDefault(name, "");
            }

            private static String value(String candidate, String fallback) {
                return candidate == null || candidate.isBlank() ? fallback : candidate;
            }

            private static int svgColor(String value, int fallback) {
                if (value == null || value.isBlank()) return fallback;
                if ("none".equals(value)) return -1;
                String normalized = value.trim().replace("#", "");
                try { return Integer.parseInt(normalized, 16); }
                catch (NumberFormatException ignored) { return fallback; }
            }
        }

        private static Map<String, Map<String, String>> parseClassStyles(Document document) {
            Map<String, Map<String, String>> result = new LinkedHashMap<>();
            NodeList styles = document.getElementsByTagName("style");
            for (int i = 0; i < styles.getLength(); i++) {
                for (String rule : styles.item(i).getTextContent().split("}")) {
                    int brace = rule.indexOf('{');
                    if (brace < 0) continue;
                    String selector = rule.substring(0, brace).trim();
                    if (!selector.startsWith(".") || selector.length() < 2) continue;
                    Map<String, String> declarations = new LinkedHashMap<>();
                    applyDeclarations(declarations, rule.substring(brace + 1));
                    result.put(selector.substring(1), Map.copyOf(declarations));
                }
            }
            return Map.copyOf(result);
        }

        private static void applyDeclarations(Map<String, String> target, String source) {
            for (String declaration : source.split(";")) {
                int colon = declaration.indexOf(':');
                if (colon <= 0) continue;
                String name = declaration.substring(0, colon).trim();
                String value = declaration.substring(colon + 1).trim();
                if ("font".equals(name)) {
                    for (String part : value.split("\\s+")) {
                        if (part.endsWith("px")) target.put("font-size", part);
                        else if (part.matches("[1-9]00")) target.put("font-weight", part);
                    }
                } else {
                    target.put(name, value);
                }
            }
        }
    }

    /** Replay notice independent of theme state and inherited graphics fonts. */
    private static final class BadgeComponent extends Component {
        private String text;

        BadgeComponent(String text) {
            this.text = text;
            setOpaque(false);
        }

        @Override public void paint(Graphics graphics) {
            int x = getX();
            int y = getY();
            int width = getWidth();
            int height = getHeight();
            int oldAlpha = graphics.getAlpha();
            Font oldFont = graphics.getFont();
            Font font = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
            graphics.setFont(font);
            graphics.setColor(0x171c26);
            graphics.setAlpha(235);
            graphics.fillRect(x, y, width, height);
            graphics.setColor(0x50d8ff);
            graphics.setAlpha(255);
            for (int i = 0; i < 3; i++) graphics.drawRect(x + i, y + i, width - i * 2 - 1, height - i * 2 - 1);
            int textX = x + Math.max(10, (width - font.stringWidth(text)) / 2);
            int textY = y + Math.max(4, (height - font.getHeight()) / 2);
            graphics.drawString(text, textX, textY);
            graphics.setFont(oldFont);
            graphics.setAlpha(oldAlpha);
        }
    }

    /** Mouse/touch marker with an animated click ripple. */
    private static final class PointerComponent extends Component {
        private final String style;
        private float x;
        private float y;
        private float pulse = 1f;

        PointerComponent(String style, float x, float y) {
            this.style = style;
            this.x = x;
            this.y = y;
            setOpaque(false);
        }

        @Override public void paint(Graphics graphics) {
            int centerX = getX() + Math.round(x * getWidth());
            int centerY = getY() + Math.round(y * getHeight());
            int base = Math.max(14, Math.min(getWidth(), getHeight()) / 55);
            int oldAlpha = graphics.getAlpha();
            if (pulse < 1f) {
                int radius = base + Math.round(base * 2.5f * pulse);
                graphics.setColor(0x50d8ff);
                graphics.setAlpha(Math.max(0, Math.round(190 * (1f - pulse))));
                graphics.fillArc(centerX - radius, centerY - radius, radius * 2, radius * 2, 0, 360);
            }
            if ("mouse".equals(style)) {
                graphics.setColor(0x10131a);
                graphics.setAlpha(220);
                graphics.fillArc(centerX - base - 2, centerY - base - 2, base * 2 + 4, base * 2 + 4, 0, 360);
                graphics.setColor(0xffffff);
                graphics.setAlpha(255);
                graphics.fillArc(centerX - base, centerY - base, base * 2, base * 2, 0, 360);
                graphics.setColor(0x176b87);
                graphics.drawLine(centerX - base, centerY, centerX + base, centerY);
                graphics.drawLine(centerX, centerY - base, centerX, centerY + base);
            } else {
                graphics.setColor(0x50d8ff);
                graphics.setAlpha(120);
                graphics.fillArc(centerX - base, centerY - base, base * 2, base * 2, 0, 360);
                graphics.setAlpha(255);
                graphics.drawArc(centerX - base, centerY - base, base * 2, base * 2, 0, 360);
            }
            graphics.setAlpha(oldAlpha);
        }
    }
}
