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
package com.codename1.rive;

import com.codename1.impl.rive.Artboard;
import com.codename1.impl.rive.Core;
import com.codename1.impl.rive.GpuRiveRenderer;
import com.codename1.impl.rive.GraphicsRiveRenderer;
import com.codename1.impl.rive.LinearAnimation;
import com.codename1.impl.rive.LinearAnimationInstance;
import com.codename1.impl.rive.CustomPropertyBoolean;
import com.codename1.impl.rive.CustomPropertyNumber;
import com.codename1.impl.rive.CustomPropertyString;
import com.codename1.impl.rive.Event;
import com.codename1.impl.rive.Mat2D;
import com.codename1.impl.rive.Mesh;
import com.codename1.impl.rive.NestedArtboard;
import com.codename1.impl.rive.OpenUrlEvent;
import com.codename1.impl.rive.StateMachine;
import com.codename1.impl.rive.StateMachineInstance;
import com.codename1.impl.rive.ViewModelInstance;
import com.codename1.impl.rive.ViewModelInstanceBoolean;
import com.codename1.impl.rive.ViewModelInstanceColor;
import com.codename1.impl.rive.ViewModelInstanceEnum;
import com.codename1.impl.rive.ViewModelInstanceNumber;
import com.codename1.impl.rive.ViewModelInstanceString;
import com.codename1.impl.rive.ViewModelInstanceTrigger;
import com.codename1.impl.rive.ViewModelInstanceValue;
import com.codename1.impl.rive.ViewModelInstanceViewModel;
import com.codename1.ui.CN;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.RenderView;
import com.codename1.gpu.Renderer;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/// Plays an artboard of a [RiveFile].
///
/// The artboard is scaled to fit the component, keeping its aspect ratio and
/// centered. Its preferred size is the artboard's size, taking one artboard
/// unit as one density-independent pixel.
///
/// An artboard with image meshes (deformed, possibly skinned images) is drawn
/// through `com.codename1.gpu` when the platform has a GPU backend: the view
/// then hosts a `com.codename1.gpu.RenderView` that fills it. Everything else,
/// and every platform without a GPU backend, draws through `Graphics`. The
/// view is a `Container` for that reason only; it has no children otherwise.
///
/// A new view plays: its state machine (or animation) runs while the view is
/// showing, driven by the form's animation clock. [#pause()] stops the clock;
/// a paused view still reacts to its inputs, view model properties and
/// pointer events, settling them at the current time without moving it.
/// When the state machine has pointer listeners the view keeps drags to
/// itself, so dragging inside it does not scroll the form.
public class RiveView extends Container {
    private final Artboard artboard;
    private final GraphicsRiveRenderer renderer = new GraphicsRiveRenderer();
    private final Mat2D base = new Mat2D();
    private LinearAnimationInstance animation;
    private StateMachineInstance machine;
    private final ArrayList<RiveEventListener> eventListeners = new ArrayList<RiveEventListener>();
    private float viewScale = 1f;
    private float viewX;
    private float viewY;
    private boolean playing;
    private long lastTime;
    private final boolean meshes;
    private GpuRiveRenderer gpu;
    private RenderView gpuView;
    private int pointersPassed = -1;
    private ArrayList<RiveEvent> heldEvents;

    /// A view of the first artboard of `file`. It plays the artboard's first
    /// state machine when it has one, otherwise its first animation, as soon
    /// as it is shown; call [#pause()] to show a still frame.
    public RiveView(RiveFile file) {
        this(file, null);
    }

    /// A view of the artboard named `artboardName` (null for the first).
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if the file has no artboard by that name
    public RiveView(RiveFile file, String artboardName) {
        artboard = file.instance(artboardName);
        if (artboard == null) {
            throw new IllegalArgumentException("No artboard named " + artboardName);
        }
        setUIIDFinal("RiveView");
        setLayout(new BorderLayout());
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

    /// Selects the animation named `name` and rewinds it.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if the artboard has no animation by that name
    public void setAnimation(String name) {
        if (name == null) {
            throw new IllegalArgumentException("No animation named null");
        }
        ArrayList<LinearAnimation> anims = artboard.getAnimations();
        for (int i = 0; i < anims.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            if (name.equals(anims.get(i).getName())) {
                machine = null;
                animation = new LinearAnimationInstance(anims.get(i), artboard);
                animation.apply(1f);
                artboard.advance(0);
                fireEvents();
                repaint();
                return;
            }
        }
        throw new IllegalArgumentException("No animation named " + name);
    }

    /// Plays the state machine named `name` in place of the current animation
    /// or state machine, from its entry state.
    ///
    /// #### Parameters
    ///
    /// - `name`: the state machine's name, see [RiveFile#getStateMachineNames(String)]
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if the artboard has no state machine by that name
    public void setStateMachine(String name) {
        if (name == null) {
            throw new IllegalArgumentException("No state machine named null");
        }
        ArrayList<StateMachine> machines = artboard.getStateMachines();
        for (int i = 0; i < machines.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            if (name.equals(machines.get(i).getName())) {
                animation = null;
                machine = new StateMachineInstance(machines.get(i), artboard);
                machine.advance(0);
                artboard.advance(0);
                if (eventListeners.isEmpty()) {
                    // nobody listens yet: hold the entry state's events for the
                    // first listener, as the constructor does
                    holdReportedEvents();
                }
                fireEvents();
                repaint();
                return;
            }
        }
        throw new IllegalArgumentException("No state machine named " + name);
    }

    /// Sets a boolean input of the playing state machine. Does nothing when
    /// no state machine plays or it has no boolean input by that name.
    ///
    /// #### Parameters
    ///
    /// - `input`: the input's name
    /// - `value`: the new value
    public void setBoolean(String input, boolean value) {
        if (machine != null) {
            machine.setBoolean(input, value);
            inputChanged();
        }
    }

    /// Sets a number input of the playing state machine. Does nothing when
    /// no state machine plays or it has no number input by that name.
    ///
    /// #### Parameters
    ///
    /// - `input`: the input's name
    /// - `value`: the new value
    public void setNumber(String input, float value) {
        if (machine != null) {
            machine.setNumber(input, value);
            inputChanged();
        }
    }

    /// Fires a trigger input of the playing state machine; it is consumed by
    /// the next advance. Does nothing when no state machine plays or it has
    /// no trigger by that name.
    ///
    /// #### Parameters
    ///
    /// - `input`: the trigger's name
    public void fireTrigger(String input) {
        if (machine != null) {
            machine.fireTrigger(input);
            inputChanged();
        }
    }

    /// A paused view settles an input change at once, without moving time;
    /// a playing one picks it up on its next frame.
    private void inputChanged() {
        if (!playing) {
            advance(0);
            repaint();
        }
    }

    // ------------------------------------------------------------ view model

    /// Sets the number property at `path` of the artboard's view model
    /// instance. The path names nested view models with "/", as in
    /// `"settings/volume"`. Does nothing when the artboard has no view model
    /// or no number property at that path.
    ///
    /// #### Parameters
    ///
    /// - `path`: the property's path
    /// - `value`: the new value
    public void setViewModelNumber(String path, float value) {
        ViewModelInstanceValue v = viewModelValue(path);
        if (v instanceof ViewModelInstanceNumber) {
            ((ViewModelInstanceNumber) v).setPropertyValue(value);
            inputChanged();
        }
    }

    /// Sets the string property at `path` (nested view models separated by
    /// "/"). Does nothing when there is no string property at that path.
    ///
    /// #### Parameters
    ///
    /// - `path`: the property's path
    /// - `value`: the new value
    public void setViewModelString(String path, String value) {
        ViewModelInstanceValue v = viewModelValue(path);
        if (v instanceof ViewModelInstanceString) {
            ((ViewModelInstanceString) v).setPropertyValue(value);
            inputChanged();
        }
    }

    /// Sets the boolean property at `path` (nested view models separated by
    /// "/"). Does nothing when there is no boolean property at that path.
    ///
    /// #### Parameters
    ///
    /// - `path`: the property's path
    /// - `value`: the new value
    public void setViewModelBoolean(String path, boolean value) {
        ViewModelInstanceValue v = viewModelValue(path);
        if (v instanceof ViewModelInstanceBoolean) {
            ((ViewModelInstanceBoolean) v).setPropertyValue(value);
            inputChanged();
        }
    }

    /// Sets the color property at `path` (nested view models separated by
    /// "/"). Does nothing when there is no color property at that path.
    ///
    /// #### Parameters
    ///
    /// - `path`: the property's path
    /// - `argb`: the new color, as 0xAARRGGBB
    public void setViewModelColor(String path, int argb) {
        ViewModelInstanceValue v = viewModelValue(path);
        if (v instanceof ViewModelInstanceColor) {
            ((ViewModelInstanceColor) v).setPropertyValue(argb);
            inputChanged();
        }
    }

    /// Selects the value named `value` of the enum property at `path`
    /// (nested view models separated by "/"). Does nothing when there is no
    /// enum property at that path or the enum has no such value.
    ///
    /// #### Parameters
    ///
    /// - `path`: the property's path
    /// - `value`: the name of the enum value
    public void setViewModelEnum(String path, String value) {
        ViewModelInstanceValue v = viewModelValue(path);
        if (v instanceof ViewModelInstanceEnum) {
            ((ViewModelInstanceEnum) v).setValueByName(value);
            inputChanged();
        }
    }

    /// Fires the trigger property at `path` (nested view models separated by
    /// "/"). Does nothing when there is no trigger property at that path.
    ///
    /// #### Parameters
    ///
    /// - `path`: the property's path
    public void fireViewModelTrigger(String path) {
        ViewModelInstanceValue v = viewModelValue(path);
        if (v instanceof ViewModelInstanceTrigger) {
            ((ViewModelInstanceTrigger) v).fire();
            inputChanged();
        }
    }

    /// The number property at `path` (nested view models separated by "/").
    ///
    /// #### Parameters
    ///
    /// - `path`: the property's path
    ///
    /// #### Returns
    ///
    /// the value, or 0 when there is no number property at that path
    public float getViewModelNumber(String path) {
        ViewModelInstanceValue v = viewModelValue(path);
        return v instanceof ViewModelInstanceNumber ? ((ViewModelInstanceNumber) v).getPropertyValue() : 0;
    }

    /// The string property at `path` (nested view models separated by "/").
    ///
    /// #### Parameters
    ///
    /// - `path`: the property's path
    ///
    /// #### Returns
    ///
    /// the value, or null when there is no string property at that path
    public String getViewModelString(String path) {
        ViewModelInstanceValue v = viewModelValue(path);
        return v instanceof ViewModelInstanceString ? ((ViewModelInstanceString) v).getPropertyValue() : null;
    }

    /// The boolean property at `path` (nested view models separated by "/").
    ///
    /// #### Parameters
    ///
    /// - `path`: the property's path
    ///
    /// #### Returns
    ///
    /// the value, or false when there is no boolean property at that path
    public boolean getViewModelBoolean(String path) {
        ViewModelInstanceValue v = viewModelValue(path);
        return v instanceof ViewModelInstanceBoolean && ((ViewModelInstanceBoolean) v).isPropertyValue();
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

    /// Adds a listener for the events the playing state machine reports. It
    /// is called on the EDT after the advance or pointer event that reported
    /// them.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void addEventListener(RiveEventListener l) {
        eventListeners.add(l);
    }

    /// Removes a listener added with [#addEventListener(RiveEventListener)].
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void removeEventListener(RiveEventListener l) {
        eventListeners.remove(l);
    }

    private void holdReportedEvents() {
        ArrayList<StateMachineInstance.ReportedEvent> events = machine.getReportedEvents();
        if (events.isEmpty()) {
            return;
        }
        if (heldEvents == null) {
            heldEvents = new ArrayList<RiveEvent>();
        }
        for (int i = 0; i < events.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed
            heldEvents.add(toPublic(events.get(i).event));
        }
        events.clear();
    }

    private void fireEvents() {
        if (heldEvents != null && !eventListeners.isEmpty()) {
            ArrayList<RiveEvent> held = heldEvents;
            heldEvents = null;
            RiveEventListener[] ls = eventListeners.toArray(new RiveEventListener[eventListeners.size()]);
            for (int i = 0; i < held.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed
                for (RiveEventListener l : ls) {
                    l.onRiveEvent(held.get(i));
                }
            }
        }
        if (machine == null) {
            return;
        }
        ArrayList<StateMachineInstance.ReportedEvent> events = machine.getReportedEvents();
        if (events.isEmpty() || eventListeners.isEmpty()) {
            events.clear();
            return;
        }
        RiveEvent[] out = new RiveEvent[events.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = toPublic(events.get(i).event);
        }
        events.clear();
        RiveEventListener[] ls = eventListeners.toArray(new RiveEventListener[eventListeners.size()]);
        for (RiveEvent e : out) {
            for (RiveEventListener l : ls) {
                l.onRiveEvent(e);
            }
        }
    }

    private static RiveEvent toPublic(Event e) {
        Map<String, Object> props = new HashMap<String, Object>();
        ArrayList<com.codename1.impl.rive.Component> children = e.getChildren();
        for (int i = 0; i < children.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed
            com.codename1.impl.rive.Component c = children.get(i);
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
            return new RiveEvent(name, RiveEvent.TYPE_OPEN_URL, u.getUrl(),
                    t >= 0 && t < targets.length ? targets[t] : "_blank", props);
        }
        return new RiveEvent(name, RiveEvent.TYPE_GENERAL, null, null, props);
    }

    /// Starts or resumes playback. A one-shot animation that finished starts
    /// over from its beginning.
    public void play() {
        if (playing) {
            return;
        }
        if (machine == null && animation != null && animation.isFinished()) {
            animation.reset();
            animation.apply(1f);
            artboard.advance(0);
            repaint();
        }
        playing = true;
        lastTime = 0;
        Form f = getComponentForm();
        if (f != null) {
            f.registerAnimated(this);
        }
    }

    /// Pauses playback on the current frame. Inputs, view model properties
    /// and pointer events still take effect while paused, at the paused time.
    public void pause() {
        playing = false;
        Form f = getComponentForm();
        if (f != null) {
            f.deregisterAnimated(this);
        }
    }

    /// Whether the view is playing.
    public boolean isPlaying() {
        return playing;
    }

    /// The playing state machine instance, null when an animation plays.
    StateMachineInstance machineInstance() {
        return machine;
    }

    /// True when `artboard`, or an artboard nested in it, has an image mesh.
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

    /// Whether this view draws its meshes through `com.codename1.gpu` when
    /// shown: the artboard has a mesh and the platform has a GPU backend.
    boolean wantsGpu() {
        return meshes && Display.getInstance().isGpuSupported();
    }

    /// True while a live GPU surface draws this view.
    boolean isGpuActive() {
        return gpuView != null && gpuView.getPeer() != null;
    }

    private void attachGpu() {
        if (gpuView != null || !wantsGpu()) {
            return;
        }
        if (gpu == null) {
            gpu = new GpuRiveRenderer();
        }
        gpuView = new RenderView(new GpuDraw(gpu));
        addComponent(BorderLayout.CENTER, gpuView);
        passPointersIfNeeded();
    }

    /// Marks the GPU surface and whatever it holds (the native peer appears
    /// once the surface initializes) so pointer events reach this view.
    private void passPointersIfNeeded() {
        if (gpuView == null) {
            return;
        }
        int n = gpuView.getComponentCount();
        if (n != pointersPassed) {
            pointersPassed = n;
            passPointerEvents(gpuView);
        }
    }

    /// Draws what the EDT recorded; advancing and recording happen on the
    /// EDT (`animate`/`paint`), never on the render thread.
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

    /// A form hands a pointer event to the deepest component under it, which
    /// would be the GPU surface; marked this way it goes on to this view, as
    /// `com.codename1.gaming.GameView` does with its surface.
    private void passPointerEvents(Component c) {
        c.setIgnorePointerEvents(true);
        if (c instanceof Container) {
            Container ct = (Container) c;
            for (int i = 0; i < ct.getComponentCount(); i++) {
                passPointerEvents(ct.getComponentAt(i));
            }
        }
    }

    @Override
    protected void initComponent() {
        // The surface is kept once created: RenderView keeps its peer when it
        // leaves the form, so showing the view again reuses it.
        attachGpu();
        super.initComponent();
        passPointersIfNeeded();
        if (playing) {
            lastTime = 0;
            getComponentForm().registerAnimated(this);
        }
    }

    @Override
    protected void deinitialize() {
        Form f = getComponentForm();
        if (f != null) {
            f.deregisterAnimated(this);
        }
        super.deinitialize();
    }

    /// Advances the artboard by `seconds`, as one frame of playback does.
    public void advance(float seconds) {
        if (machine != null) {
            machine.advance(seconds);
        } else if (animation != null) {
            animation.advance(seconds);
            animation.apply(1f);
        }
        artboard.advance(seconds);
        fireEvents();
    }

    @Override
    public boolean animate() {
        if (!playing) {
            return false;
        }
        long now = System.currentTimeMillis();
        float dt = lastTime == 0 ? 0f : (now - lastTime) / 1000f;
        lastTime = now;
        if (dt > 0.1f) {
            dt = 0.1f;
        }
        if (machine != null) {
            // A state machine stays registered: an input or pointer event can
            // wake it after it settled. It repaints only while it moves.
            boolean wake = machine.needsAdvance();
            boolean keepGoing = machine.advance(dt);
            artboard.advance(dt);
            fireEvents();
            return keepGoing || wake;
        }
        advance(dt);
        if (animation != null && animation.isFinished()) {
            playing = false;
        }
        return true;
    }

    /// Computes the fit of the artboard in the current inner area (letterboxed,
    /// centered), relative to this view. False when there is nothing to fit.
    private boolean updateFit() {
        float aw = artboard.getWidth();
        float ah = artboard.getHeight();
        if (aw <= 0 || ah <= 0) {
            viewScale = 0;
            return false;
        }
        int w = Math.max(0, getInnerWidth());
        int h = Math.max(0, getInnerHeight());
        float scale = Math.min(w / aw, h / ah);
        int padLeft = getStyle().getPaddingLeft(isRTL());
        int padTop = getStyle().getPaddingTop();
        viewScale = scale;
        viewX = padLeft + (w - aw * scale) / 2f;
        viewY = padTop + (h - ah * scale) / 2f;
        return scale > 0;
    }

    @Override
    public void layoutContainer() {
        super.layoutContainer();
        updateFit();
    }

    @Override
    public void paint(Graphics g) {
        if (!updateFit()) {
            return;
        }
        float scale = viewScale;
        float ox = viewX;
        float oy = viewY;
        if (isGpuActive() && gpuView.getWidth() > 0 && gpuView.getHeight() > 0) {
            passPointersIfNeeded();
            // The surface fills the inner area; draw in its own pixels.
            int sw = gpuView.getWidth();
            int sh = gpuView.getHeight();
            base.set(scale, 0, 0, scale, ox - gpuView.getX(), oy - gpuView.getY());
            gpu.beginFrame(sw, sh, base);
            artboard.draw(gpu);
            gpu.endFrame(clearColor());
            gpuView.requestRender();
            super.paint(g);
            return;
        }
        base.set(scale, 0, 0, scale, getX() + ox, getY() + oy);
        renderer.begin(g, base);
        artboard.draw(renderer);
        renderer.end();
    }

    private int clearColor() {
        com.codename1.ui.plaf.Style s = getStyle();
        return (s.getBgTransparency() & 0xff) == 0xff ? 0xff000000 | s.getBgColor() : 0;
    }

    @Override
    public void pointerPressed(int x, int y) {
        super.pointerPressed(x, y);
        if (machine != null) {
            machine.pointerDown(artboardX(x), artboardY(y));
            afterPointer();
        }
    }

    @Override
    public void pointerReleased(int x, int y) {
        super.pointerReleased(x, y);
        if (machine != null) {
            machine.pointerUp(artboardX(x), artboardY(y));
            afterPointer();
        }
    }

    @Override
    public void pointerDragged(int x, int y) {
        // A state machine with pointer listeners owns the drag; handing it to
        // super would scroll the scrollable parent under the finger.
        if (!hasPointerListeners()) {
            super.pointerDragged(x, y);
        }
        if (machine != null) {
            machine.pointerMove(artboardX(x), artboardY(y));
            afterPointer();
        }
    }

    @Override
    public void pointerHover(int[] x, int[] y) {
        super.pointerHover(x, y);
        if (machine != null && x.length > 0) {
            if (contains(x[0], y[0])) {
                machine.pointerMove(artboardX(x[0]), artboardY(y[0]));
            } else {
                // left the view: far outside every hit area, so the
                // listeners that were hovered see an exit
                machine.pointerMove(-1e9f, -1e9f);
            }
            afterPointer();
        }
    }

    private boolean hasPointerListeners() {
        return machine != null && machine.hasPointerListeners();
    }

    private float artboardX(int x) {
        updateFit();
        return viewScale == 0 ? 0 : (x - getAbsoluteX() - viewX) / viewScale;
    }

    private float artboardY(int y) {
        updateFit();
        return viewScale == 0 ? 0 : (y - getAbsoluteY() - viewY) / viewScale;
    }

    private void afterPointer() {
        fireEvents();
        if (!playing && machine.needsAdvance()) {
            advance(0);
            repaint();
        }
    }

    @Override
    protected Dimension calcPreferredSize() {
        // one dip is 1/160 inch; measured over 100 of them to keep the fraction
        float dip = CN.convertToPixels(100f * 25.4f / 160f) / 100f;
        return new Dimension((int) (artboard.getWidth() * dip) + getStyle().getHorizontalPadding(),
                (int) (artboard.getHeight() * dip) + getStyle().getVerticalPadding());
    }
}
