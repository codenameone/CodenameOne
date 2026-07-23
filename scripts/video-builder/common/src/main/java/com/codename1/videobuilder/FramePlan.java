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

import java.util.ArrayList;
import java.util.List;

/** Selects the frames whose pixels can actually change on the scripted timeline. */
final class FramePlan {
    private static final List<String> TIMED_ACTIONS = List.of(
            "transition", "layer.animate", "code.type", "pointer.move", "pointer.click",
            "intro.show", "outro.show");

    private FramePlan() { }

    static Plan create(VideoScript script, long durationMs) {
        int fps = script.getOutput().frameRate();
        long frameCount = (durationMs * fps + 999) / 1000;
        List<Frame> frames = new ArrayList<>();
        Position previous = null;
        for (long frameIndex = 0; frameIndex < frameCount; frameIndex++) {
            long positionMs = frameIndex * 1000L / fps;
            Position position = locate(script, positionMs);
            boolean render = previous == null || position.sceneIndex != previous.sceneIndex
                    || (previous.sceneIndex == position.sceneIndex
                        && dynamicSampleChanged(position.scene, previous.relativeMs,
                                position.relativeMs, fps))
                    || (previous.sceneIndex == position.sceneIndex
                        && discreteStateChanged(position.scene, previous.relativeMs, position.relativeMs))
                    || crossedBoundary(position.scene,
                            previous == null || previous.sceneIndex != position.sceneIndex
                                    ? -1 : previous.relativeMs,
                            position.relativeMs);
            if (render) frames.add(new Frame(frameIndex, positionMs, position.sceneIndex));
            previous = position;
        }
        if (frameCount > 0 && (frames.isEmpty() || frames.get(frames.size() - 1).frameIndex != frameCount - 1)) {
            long index = frameCount - 1;
            Position position = locate(script, index * 1000L / fps);
            frames.add(new Frame(index, index * 1000L / fps, position.sceneIndex));
        }
        return new Plan(List.copyOf(frames), frameCount);
    }

    private static boolean dynamicSampleChanged(VideoScript.Scene scene, long previousMs,
                                                long currentMs, int outputFps) {
        for (VideoScript.Action action : scene.actions()) {
            if (TIMED_ACTIONS.contains(action.type())
                    && (inRange(currentMs, action.atMs(), action.atMs() + action.durationMs())
                    || inRange(previousMs, action.atMs(), action.atMs() + action.durationMs()))) {
                return true;
            }
            if ("demo.mount".equals(action.type()) && currentMs >= action.atMs()
                    && VideoScript.bool(action.values().get("animated"), true)) return true;
            if ("svg.show".equals(action.type()) && currentMs >= action.atMs()
                    && VideoScript.bool(action.values().get("animated"), false)) return true;
            if ("replay".equals(action.type())) {
                long previousSample = replaySample(action, previousMs, outputFps);
                long currentSample = replaySample(action, currentMs, outputFps);
                if (previousSample != currentSample && (previousSample >= 0 || currentSample >= 0)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns a stable sample number for a replay phase. Replays are visual aids rather than
     * source footage, so rendering at their declared cadence and repeating the staged images is
     * both deterministic and dramatically cheaper than re-painting identical UI at the output
     * frame rate.
     */
    private static long replaySample(VideoScript.Action action, long relativeMs, int outputFps) {
        long start = action.atMs();
        long rewind = VideoScript.number(action.values().get("rewindDurationMs"), 700);
        long from = VideoScript.number(action.values().get("fromMs"), 0);
        long to = VideoScript.number(action.values().get("toMs"), 0);
        float playbackRate = Math.max(0.01f,
                VideoScript.decimal(action.values().get("playbackRate"), 0.65f));
        long replay = (long) Math.ceil(Math.max(0, to - from) / playbackRate);
        if (relativeMs < start || relativeMs > start + rewind + replay) return -1;
        if (relativeMs <= start + rewind) {
            int fps = Math.max(1, Math.min(outputFps,
                    VideoScript.integer(action.values().get("rewindFps"), 10)));
            return (relativeMs - start) * fps / 1000L;
        }
        int fps = Math.max(1, Math.min(outputFps,
                VideoScript.integer(action.values().get("replayFps"), outputFps)));
        return 1_000_000L + (relativeMs - start - rewind) * fps / 1000L;
    }

    private static boolean crossedBoundary(VideoScript.Scene scene, long previousMs, long currentMs) {
        for (VideoScript.Action action : scene.actions()) {
            if (crossed(previousMs, currentMs, action.atMs())) return true;
            if (action.durationMs() > 0
                    && crossed(previousMs, currentMs, action.atMs() + action.durationMs())) return true;
        }
        return false;
    }

    private static boolean discreteStateChanged(VideoScript.Scene scene, long previousMs, long currentMs) {
        for (VideoScript.Action action : scene.actions()) {
            if ("bullets.show".equals(action.type())) {
                int count = Math.max(1, VideoScript.strings(action.values().get("items")).size());
                if (bulletState(action, previousMs, count) != bulletState(action, currentMs, count)) return true;
            } else if ("diagram.show".equals(action.type())
                    && progressState(action, previousMs, 16) != progressState(action, currentMs, 16)) {
                return true;
            }
        }
        return false;
    }

    private static int bulletState(VideoScript.Action action, long relativeMs, int count) {
        float progress = progress(action, relativeMs);
        int visible = progress >= 1f ? count : Math.min(count, (int) Math.ceil(count * progress));
        int emphasis = Math.round(count * progress);
        return visible * 1000 + emphasis;
    }

    private static int progressState(VideoScript.Action action, long relativeMs, int steps) {
        return Math.round(progress(action, relativeMs) * steps);
    }

    private static float progress(VideoScript.Action action, long relativeMs) {
        if (relativeMs < action.atMs()) return -1f;
        if (action.durationMs() <= 0) return 1f;
        return Math.min(1f, Math.max(0f,
                (relativeMs - action.atMs()) / (float) action.durationMs()));
    }

    private static boolean crossed(long previousMs, long currentMs, long boundaryMs) {
        return previousMs < boundaryMs && currentMs >= boundaryMs;
    }

    private static boolean inRange(long value, long start, long end) {
        return value >= start && value <= end;
    }

    private static Position locate(VideoScript script, long positionMs) {
        long cursor = 0;
        List<VideoScript.Scene> scenes = script.getScenes();
        for (int i = 0; i < scenes.size(); i++) {
            VideoScript.Scene scene = scenes.get(i);
            long end = cursor + scene.durationMs();
            if (positionMs < end || i + 1 == scenes.size()) {
                return new Position(i, Math.max(0, positionMs - cursor), scene);
            }
            cursor = end;
        }
        VideoScript.Scene last = scenes.get(scenes.size() - 1);
        return new Position(scenes.size() - 1, last.durationMs(), last);
    }

    record Frame(long frameIndex, long positionMs, int sceneIndex) { }
    record Plan(List<Frame> frames, long frameCount) { }
    private record Position(int sceneIndex, long relativeMs, VideoScript.Scene scene) { }
}
