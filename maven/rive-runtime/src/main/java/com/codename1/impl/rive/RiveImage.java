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
package com.codename1.impl.rive;

import com.codename1.gpu.GpuImage;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.Renderer;
import com.codename1.ui.CN;
import com.codename1.ui.Graphics;
import com.codename1.ui.events.AnimationEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/// The `com.codename1.ui.Image` a `.riv` file becomes. The generated image
/// registry creates one per file, so an application reaches it the way it
/// reaches any other image (`Resources.getImage("coin.riv")`, a CSS `url()`)
/// and drives it through the interactive-animation methods of `Image`.
///
/// It shows the first artboard of the file, scaled to fit the area it is
/// drawn into, keeping its aspect ratio and centered. Its pixel size is the
/// artboard's size, taking one artboard unit as one density-independent
/// pixel; [#scaled(int, int)] makes an independent image of another size.
///
/// A new image plays its artboard's first state machine when it has one,
/// otherwise its first animation; `animate()` drives its clock. Paused, it
/// still reacts to inputs, view model properties and pointer events,
/// settling them at the current time without moving it.
///
/// An artboard with image meshes (deformed, possibly skinned images) draws
/// through `com.codename1.gpu` where the platform has a GPU backend: as a
/// `GpuImage`, it places a GPU surface over the rectangle it is drawn into
/// and records each frame for `GpuRiveRenderer`. Everything else, and every
/// platform without a GPU backend, draws through `Graphics`.
///
/// One instance is one animation. Shown in two places at once (two labels,
/// or twice in one `paint`), both show the
/// same state and react to the same inputs and pointer events, and only the
/// first placement drawn in a pass gets the GPU surface; a second
/// simultaneous placement draws through `Graphics`. Use
/// [#scaled(int, int)] for an independent copy.
public class RiveImage extends GpuImage {
    private final RiveFile file;
    private final Artboard artboard;
    private final GraphicsRiveRenderer renderer = new GraphicsRiveRenderer();
    private final Mat2D base = new Mat2D();
    private final boolean meshes;
    private GpuRiveRenderer gpu;
    private final int width;
    private final int height;
    private LinearAnimationInstance animation;
    private StateMachineInstance machine;
    private boolean playing;
    private boolean dirty;
    private long lastTime;
    private ArrayList<AnimationEvent> heldEvents;

    /// An image of the first artboard of `file`, at the artboard's size.
    ///
    /// #### Parameters
    ///
    /// - `file`: the generated file
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if the file has no artboard
    public RiveImage(RiveFile file) {
        this(file, -1, -1);
    }

    private RiveImage(RiveFile file, int w, int h) {
        this.file = file;
        artboard = file.instance((String) null);
        if (artboard == null) {
            throw new IllegalArgumentException("The Rive file has no artboard");
        }
        if (w < 0) {
            // one dip is 1/160 inch; measured over 100 of them to keep the fraction
            float dip = CN.convertToPixels(100f * 25.4f / 160f) / 100f;
            w = (int) (artboard.getWidth() * dip);
            h = (int) (artboard.getHeight() * dip);
        }
        width = Math.max(1, w);
        height = Math.max(1, h);
        meshes = containsMesh(artboard, 0);
        ArrayList<LinearAnimation> anims = artboard.getAnimations();
        ArrayList<StateMachine> machines = artboard.getStateMachines();
        if (!machines.isEmpty()) {
            machine = new StateMachineInstance(machines.get(0), artboard);
            machine.advance(0);
            // no listener can be registered yet: keep what the entry state
            // reported for the first delivery
            holdReportedEvents();
        } else if (!anims.isEmpty()) {
            animation = new LinearAnimationInstance(anims.get(0), artboard);
            animation.apply(1f);
        }
        artboard.advance(0);
        playing = true;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    /// A new image of the same file at `width` by `height` pixels, in its
    /// initial state: it shares nothing with this one.
    @Override
    public com.codename1.ui.Image scaled(int width, int height) {
        return new RiveImage(file, width, height);
    }

    @Override
    public boolean isAnimation() {
        return true;
    }

    // ------------------------------------------------------------ inputs

    @Override
    public boolean isInteractive() {
        return machine != null && machine.hasPointerListeners();
    }

    @Override
    public void setAnimationInput(String name, boolean value) {
        if (machine != null) {
            machine.setBoolean(name, value);
            inputChanged();
        }
    }

    @Override
    public void setAnimationInput(String name, float value) {
        if (machine != null) {
            machine.setNumber(name, value);
            inputChanged();
        }
    }

    @Override
    public void fireAnimationTrigger(String name) {
        if (machine != null) {
            machine.fireTrigger(name);
            inputChanged();
        }
    }

    /// A paused image settles a change at once, without moving time; a
    /// playing one picks it up on its next frame.
    private void inputChanged() {
        dirty = true;
        if (!playing) {
            advanceAnimation(0);
        }
    }

    // ------------------------------------------------------------ view model

    @Override
    public void setAnimationProperty(String path, Object value) {
        ViewModelInstanceValue v = viewModelValue(path);
        if (v == null) {
            return;
        }
        if (value == null) {
            if (v instanceof ViewModelInstanceTrigger) {
                ((ViewModelInstanceTrigger) v).fire();
                inputChanged();
            }
            return;
        }
        if (v instanceof ViewModelInstanceNumber && value instanceof Number) {
            ((ViewModelInstanceNumber) v).setPropertyValue(((Number) value).floatValue());
        } else if (v instanceof ViewModelInstanceColor && value instanceof Number) {
            ((ViewModelInstanceColor) v).setPropertyValue(((Number) value).intValue());
        } else if (v instanceof ViewModelInstanceString && value instanceof String) {
            ((ViewModelInstanceString) v).setPropertyValue((String) value);
        } else if (v instanceof ViewModelInstanceEnum && value instanceof String) {
            ((ViewModelInstanceEnum) v).setValueByName((String) value);
        } else if (v instanceof ViewModelInstanceBoolean && value instanceof Boolean) {
            ((ViewModelInstanceBoolean) v).setPropertyValue(((Boolean) value).booleanValue());
        } else if (v instanceof ViewModelInstanceTrigger && value instanceof Boolean
                && ((Boolean) value).booleanValue()) {
            ((ViewModelInstanceTrigger) v).fire();
        } else {
            return;
        }
        inputChanged();
    }

    @Override
    public Object getAnimationProperty(String path) {
        ViewModelInstanceValue v = viewModelValue(path);
        if (v instanceof ViewModelInstanceNumber) {
            return Float.valueOf(((ViewModelInstanceNumber) v).getPropertyValue());
        }
        if (v instanceof ViewModelInstanceString) {
            return ((ViewModelInstanceString) v).getPropertyValue();
        }
        if (v instanceof ViewModelInstanceBoolean) {
            return Boolean.valueOf(((ViewModelInstanceBoolean) v).isPropertyValue());
        }
        if (v instanceof ViewModelInstanceColor) {
            return Integer.valueOf(((ViewModelInstanceColor) v).getPropertyValue());
        }
        return null;
    }

    private ViewModelInstanceValue viewModelValue(String path) {
        ViewModelInstance current = artboard.getViewModelInstance();
        if (current == null || path == null) {
            return null;
        }
        int start = 0;
        while (true) {
            int slash = path.indexOf('/', start);
            String name = slash < 0 ? path.substring(start) : path.substring(start, slash);
            ViewModelInstanceValue v = current.propertyValue(name);
            if (slash < 0) {
                return v;
            }
            if (!(v instanceof ViewModelInstanceViewModel)) {
                return null;
            }
            current = ((ViewModelInstanceViewModel) v).getReference();
            if (current == null) {
                return null;
            }
            start = slash + 1;
        }
    }

    // ------------------------------------------------------------ events

    @Override
    public void addAnimationListener(com.codename1.ui.events.ActionListener l) {
        super.addAnimationListener(l);
        fireEvents();
    }

    private void holdReportedEvents() {
        ArrayList<StateMachineInstance.ReportedEvent> events = machine.getReportedEvents();
        if (events.isEmpty()) {
            return;
        }
        if (heldEvents == null) {
            heldEvents = new ArrayList<AnimationEvent>();
        }
        for (int i = 0; i < events.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed
            heldEvents.add(toPublic(events.get(i).event));
        }
        events.clear();
    }

    private void fireEvents() {
        boolean listening = hasAnimationListeners();
        if (heldEvents != null && listening) {
            ArrayList<AnimationEvent> held = heldEvents;
            heldEvents = null;
            for (int i = 0; i < held.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed
                fireAnimationEvent(held.get(i));
            }
        }
        if (machine == null) {
            return;
        }
        ArrayList<StateMachineInstance.ReportedEvent> events = machine.getReportedEvents();
        if (events.isEmpty() || !listening) {
            events.clear();
            return;
        }
        AnimationEvent[] out = new AnimationEvent[events.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = toPublic(events.get(i).event);
        }
        events.clear();
        for (AnimationEvent e : out) {
            fireAnimationEvent(e);
        }
    }

    private AnimationEvent toPublic(Event e) {
        Map<String, Object> props = new HashMap<String, Object>();
        ArrayList<Component> children = e.getChildren();
        for (int i = 0; i < children.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed
            Component c = children.get(i);
            if (c instanceof CustomPropertyNumber) {
                props.put(c.getName(), Float.valueOf(((CustomPropertyNumber) c).getPropertyValue()));
            } else if (c instanceof CustomPropertyBoolean) {
                props.put(c.getName(), Boolean.valueOf(((CustomPropertyBoolean) c).isPropertyValue()));
            } else if (c instanceof CustomPropertyString) {
                props.put(c.getName(), ((CustomPropertyString) c).getPropertyValue());
            }
        }
        String name = e.getName() == null ? "" : e.getName();
        if (e instanceof OpenUrlEvent) {
            OpenUrlEvent u = (OpenUrlEvent) e;
            String[] targets = {"_blank", "_parent", "_self", "_top"};
            int t = u.getTargetValue();
            return new AnimationEvent(this, name, AnimationEvent.TYPE_OPEN_URL, u.getUrl(),
                    t >= 0 && t < targets.length ? targets[t] : "_blank", props);
        }
        return new AnimationEvent(this, name, AnimationEvent.TYPE_GENERAL, null, null, props);
    }

    // ------------------------------------------------------------ playback

    @Override
    public void setAnimationPlaying(boolean play) {
        if (play == playing) {
            return;
        }
        if (play && machine == null && animation != null && animation.isFinished()) {
            // a one-shot animation that finished starts over
            animation.reset();
            animation.apply(1f);
            artboard.advance(0);
            dirty = true;
        }
        playing = play;
        lastTime = 0;
    }

    @Override
    public boolean isAnimationPlaying() {
        return playing;
    }

    @Override
    public void advanceAnimation(float seconds) {
        if (machine != null) {
            machine.advance(seconds);
        } else if (animation != null) {
            animation.advance(seconds);
            animation.apply(1f);
        }
        artboard.advance(seconds);
        dirty = true;
        fireEvents();
    }

    /// The playing state machine instance, null when an animation plays.
    StateMachineInstance machineInstance() {
        return machine;
    }

    /// The artboard this image shows.
    Artboard artboard() {
        return artboard;
    }

    @Override
    public boolean animate() {
        boolean repaint = dirty;
        dirty = false;
        if (!playing) {
            return repaint;
        }
        long now = System.currentTimeMillis();
        float dt = lastTime == 0 ? 0f : (now - lastTime) / 1000f;
        lastTime = now;
        if (dt > 0.1f) {
            dt = 0.1f;
        }
        if (machine != null) {
            // A state machine keeps being asked: an input or pointer event can
            // wake it after it settled. It repaints only while it moves.
            boolean wake = machine.needsAdvance();
            boolean keepGoing = machine.advance(dt);
            artboard.advance(dt);
            fireEvents();
            return keepGoing || wake || repaint;
        }
        if (animation == null) {
            return repaint;
        }
        advanceAnimation(dt);
        dirty = false;
        if (animation.isFinished()) {
            playing = false;
        }
        return true;
    }

    // ------------------------------------------------------------ drawing

    /// Draws through the GPU when the artboard has a mesh; vectors alone
    /// draw as well through `Graphics`.
    @Override
    protected boolean usesGpu() {
        return meshes;
    }

    @Override
    protected Renderer createRenderer() {
        if (gpu == null) {
            gpu = new GpuRiveRenderer();
        }
        return new GpuDraw(gpu);
    }

    /// Records the artboard, fitted into the `width` by `height` rectangle at
    /// `offsetX`, `offsetY` of the surface, for the render thread.
    private int prepareTrace;

    @Override
    protected void prepareFrame(int surfaceWidth, int surfaceHeight, int offsetX, int offsetY,
                                int width, int height) {
        float aw = artboard.getWidth();
        float ah = artboard.getHeight();
        if (gpu == null || aw <= 0 || ah <= 0) {
            return;
        }
        // TEMPORARY trace for the RiveGpu emulator crash; removed once found.
        if (prepareTrace++ < 10) {
            System.out.println("CN1GPU: prepareFrame surface=" + surfaceWidth + "x" + surfaceHeight
                    + " at " + offsetX + "," + offsetY + " image=" + width + "x" + height);
        }
        float scale = Math.min(width / aw, height / ah);
        float ox = (width - aw * scale) / 2f;
        float oy = (height - ah * scale) / 2f;
        base.set(scale, 0, 0, scale, offsetX + ox, offsetY + oy);
        gpu.beginFrame(surfaceWidth, surfaceHeight, base);
        artboard.draw(gpu);
        gpu.endFrame(0);
    }

    /// Draws the artboard fitted into `w` by `h` at `x`, `y`: scaled to fit,
    /// keeping its aspect ratio, and centered.
    @Override
    protected void drawFallback(Graphics g, int x, int y, int w, int h) {
        float aw = artboard.getWidth();
        float ah = artboard.getHeight();
        if (aw <= 0 || ah <= 0 || w <= 0 || h <= 0) {
            return;
        }
        float scale = Math.min(w / aw, h / ah);
        float ox = (w - aw * scale) / 2f;
        float oy = (h - ah * scale) / 2f;
        base.set(scale, 0, 0, scale, x + ox, y + oy);
        renderer.begin(g, base);
        artboard.draw(renderer);
        renderer.end();
    }

    /// Whether `artboard`, or an artboard nested in it, has an image mesh.
    static boolean containsMesh(Artboard artboard, int depth) {
        if (artboard == null || depth > 8) {
            return false;
        }
        ArrayList<Core> objects = artboard.getObjects();
        for (int i = 0; i < objects.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed
            Core c = objects.get(i);
            if (c instanceof Mesh) {
                return true;
            }
            if (c instanceof NestedArtboard
                    && containsMesh(((NestedArtboard) c).getArtboardInstance(), depth + 1)) {
                return true;
            }
        }
        return false;
    }

    /// Draws what the EDT recorded; advancing and recording happen on the
    /// EDT, never on the render thread.
    private static final class GpuDraw implements Renderer {
        private final GpuRiveRenderer r;

        GpuDraw(GpuRiveRenderer r) {
            this.r = r;
        }

        @Override
        public void onInit(GraphicsDevice device) {
            r.contextLost();
        }

        @Override
        public void onResize(GraphicsDevice device, int width, int height) {
            device.setViewport(0, 0, width, height);
        }

        @Override
        public void onFrame(GraphicsDevice device) {
            r.render(device);
        }

        @Override
        public void onDispose(GraphicsDevice device) {
            r.dispose(device);
        }
    }

    // ------------------------------------------------------------ pointer

    @Override
    public void pointerPressed(int x, int y) {
        if (machine != null) {
            machine.pointerDown(artboardX(x), artboardY(y));
            afterPointer();
        }
    }

    @Override
    public void pointerDragged(int x, int y) {
        if (machine != null) {
            machine.pointerMove(artboardX(x), artboardY(y));
            afterPointer();
        }
    }

    @Override
    public void pointerReleased(int x, int y) {
        if (machine != null) {
            machine.pointerUp(artboardX(x), artboardY(y));
            afterPointer();
        }
    }

    private float fitScale() {
        float aw = artboard.getWidth();
        float ah = artboard.getHeight();
        if (aw <= 0 || ah <= 0) {
            return 0;
        }
        return Math.min(width / aw, height / ah);
    }

    private float artboardX(int x) {
        float s = fitScale();
        return s == 0 ? 0 : (x - (width - artboard.getWidth() * s) / 2f) / s;
    }

    private float artboardY(int y) {
        float s = fitScale();
        return s == 0 ? 0 : (y - (height - artboard.getHeight() * s) / 2f) / s;
    }

    private void afterPointer() {
        fireEvents();
        dirty = true;
        if (!playing && machine.needsAdvance()) {
            advanceAnimation(0);
        }
    }
}
