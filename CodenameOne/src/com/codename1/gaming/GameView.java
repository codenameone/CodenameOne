/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.gaming;

import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.Light;
import com.codename1.gpu.RenderView;
import com.codename1.ui.Form;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Rectangle;

/// A GPU accelerated game surface: a `com.codename1.gpu.RenderView` that hosts a
/// `SpriteRenderer` over a `Scene` and calls your `#update(double)` once per frame.
///
/// Subclass it, build your world by adding `Sprite`s to `#getScene()`, advance the
/// game in `#update(double)`, add the view to a `com.codename1.ui.Form` and call
/// `#start()`:
///
/// ```java
/// class MyGame extends GameView {
///     final Sprite player = new Sprite(playerImage);
///     MyGame() { getScene().add(player); player.setPosition(160, 240); }
///
///     protected void update(double dt) {
///         if (getInput().isGameKeyDown(Display.GAME_RIGHT)) {
///             player.setX(player.getX() + 200 * dt);   // 200 px/second
///         }
///     }
/// }
///
/// Form f = new Form("Game", new BorderLayout());
/// MyGame game = new MyGame();
/// f.add(BorderLayout.CENTER, game);
/// f.show();
/// game.start();
/// ```
///
/// Rendering is GPU driven: the underlying `RenderView` runs the frame loop (a
/// display link on device, the software rasterizer in the simulator), so there is
/// no EDT busy loop. Drawing is handled for you by the `SpriteRenderer` -- you only
/// position sprites. The `deltaSeconds` passed to `#update(double)` is the wall
/// clock time since the previous frame; multiply movement by it to stay framerate
/// independent. With `#setFixedTimestep(double)` the update is stepped at a fixed
/// interval for deterministic physics, and `#getInterpolationAlpha()` gives a 0..1
/// blend factor.
///
/// `#update(double)` runs on the render thread together with drawing. Keep it non
/// blocking -- offload asset loading to a background thread and hand the result
/// back with `com.codename1.ui.CN#callSerially(java.lang.Runnable)`.
public abstract class GameView extends RenderView implements SpriteRenderer.Updatable {
    private final Scene scene;
    private boolean running;
    private boolean paused;
    private double fixedTimestep;
    private double accumulator;
    private double interpolationAlpha = 1;
    private static final int MAX_FIXED_STEPS = 8;
    private final GameInput input = new GameInput();
    private final TouchControls controls;
    private ActionListener pressListener;
    private ActionListener dragListener;
    private ActionListener releaseListener;
    private boolean formListenersAdded;

    public GameView() {
        super(new SpriteRenderer());
        SpriteRenderer r = (SpriteRenderer) getRenderer();
        r.setUpdatable(this);
        this.scene = r.getScene();
        this.controls = new TouchControls(input);
        r.setControls(controls);
        setFocusable(true);
    }

    /// Advance the game by the given amount of time. Called once per frame (or
    /// repeatedly at a fixed interval when `#setFixedTimestep(double)` is used).
    protected abstract void update(double deltaSeconds);

    /// The scene drawn by this view; add and remove `Sprite`s here.
    public Scene getScene() {
        return scene;
    }

    /// The pollable input state for this view.
    public GameInput getInput() {
        return input;
    }

    /// The on-screen touch controls for this view (a virtual joystick and buttons).
    /// Add controls to it to make the game playable on touch devices; whatever you
    /// add feeds the same `GameInput` your keyboard handling already reads.
    public TouchControls getControls() {
        return controls;
    }

    /// The camera this view renders through. It starts in 2D mode; call
    /// `GameCamera#setPerspective(float, float, float)` on it to switch the view into
    /// a 3D perspective with billboarded sprites.
    public GameCamera getCamera() {
        return ((SpriteRenderer) getRenderer()).getCamera();
    }

    /// The directional light shading lit 3D `Model`s in perspective mode.
    public Light getLight() {
        return ((SpriteRenderer) getRenderer()).getLight();
    }

    /// Adds a 3D `Model` to be drawn in the perspective camera alongside the sprites.
    /// Build models from `#onSetup(com.codename1.gpu.GraphicsDevice)`, where the GPU
    /// device is available.
    public void addModel(Model model) {
        ((SpriteRenderer) getRenderer()).addModel(model);
    }

    /// Removes a previously added 3D `Model`.
    public void removeModel(Model model) {
        ((SpriteRenderer) getRenderer()).removeModel(model);
    }

    /// Override to allocate GPU resources (meshes, textures, `Model`s) once the GPU
    /// device is ready. Invoked once on the render thread before the first frame --
    /// the only place you can call `com.codename1.gpu.Primitives` /
    /// `com.codename1.gpu.GltfLoader`, which need the device. The default does
    /// nothing.
    ///
    /// #### Parameters
    ///
    /// - `device`: the GPU device for creating meshes, textures and buffers
    protected void onSetup(GraphicsDevice device) {
    }

    /// {@inheritDoc} Forwards GPU setup to `#onSetup(com.codename1.gpu.GraphicsDevice)`.
    @Override
    public void setup(GraphicsDevice device) {
        onSetup(device);
    }

    /// Sets the ARGB color the view is cleared to each frame.
    public void setClearColor(int argb) {
        ((SpriteRenderer) getRenderer()).setClearColor(argb);
    }

    /// Starts the game loop (continuous rendering). Safe to call before or after the
    /// view is shown.
    public void start() {
        if (running) {
            return;
        }
        running = true;
        paused = false;
        accumulator = 0;
        setContinuous(true);
        requestFocus();
        requestRender();
    }

    /// Stops the game loop (no further frames until `#start()`).
    public void stop() {
        if (!running) {
            return;
        }
        running = false;
        setContinuous(false);
    }

    /// Pauses updates; frames still render but `#update(double)` is not called.
    public void pause() {
        paused = true;
    }

    /// Resumes after `#pause()`.
    public void resume() {
        paused = false;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isPaused() {
        return paused;
    }

    /// Sets a fixed update interval in seconds (0 disables, the default). With a
    /// fixed timestep `#update(double)` may be called several times per frame to
    /// catch up, and `#getInterpolationAlpha()` returns the leftover fraction.
    public void setFixedTimestep(double seconds) {
        fixedTimestep = seconds < 0 ? 0 : seconds;
    }

    public double getFixedTimestep() {
        return fixedTimestep;
    }

    /// The 0..1 fraction of a fixed step left after the last update, for
    /// interpolating rendered positions. Always 1 with a variable timestep.
    public double getInterpolationAlpha() {
        return interpolationAlpha;
    }

    /// {@inheritDoc} Re-applies the running state once the GPU peer exists and starts
    /// listening for pointer events at the form level.
    @Override
    protected void initComponent() {
        super.initComponent();
        addFormPointerListeners();
        if (running) {
            setContinuous(true);
            requestFocus();
        }
    }

    /// {@inheritDoc} Stops listening for pointer events when detached.
    @Override
    protected void deinitialize() {
        removeFormPointerListeners();
        super.deinitialize();
    }

    /// The GPU surface is hosted in a native peer that swallows the platform's
    /// pointer events before they reach this component, so the usual
    /// `#pointerPressed(int[], int[])` callbacks never fire over the surface. Instead
    /// we listen at the form level (those listeners fire for every pointer event,
    /// regardless of which component is hit) and route the touches to the on-screen
    /// controls ourselves.
    private void addFormPointerListeners() {
        if (formListenersAdded) {
            return;
        }
        Form f = getComponentForm();
        if (f == null) {
            return;
        }
        if (pressListener == null) {
            pressListener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    routeFormTouch(e.getX(), e.getY(), true, true, false);
                }
            };
            dragListener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    routeFormTouch(e.getX(), e.getY(), true, false, false);
                }
            };
            releaseListener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    routeFormTouch(e.getX(), e.getY(), false, false, true);
                }
            };
        }
        f.addPointerPressedListener(pressListener);
        f.addPointerDraggedListener(dragListener);
        f.addPointerReleasedListener(releaseListener);
        formListenersAdded = true;
    }

    private void removeFormPointerListeners() {
        if (!formListenersAdded) {
            return;
        }
        Form f = getComponentForm();
        if (f != null && pressListener != null) {
            f.removePointerPressedListener(pressListener);
            f.removePointerDraggedListener(dragListener);
            f.removePointerReleasedListener(releaseListener);
        }
        formListenersAdded = false;
    }

    /// Routes a single form-level pointer event (in absolute coordinates) to the
    /// on-screen controls and the raw pointer state, in view-local pixels.
    private void routeFormTouch(int x, int y, boolean down, boolean pressed, boolean released) {
        int lx = x - getAbsoluteX();
        int ly = y - getAbsoluteY();
        controls.onTouches(new int[]{lx}, new int[]{ly}, down);
        input.pointer(lx, ly, down, pressed, released);
    }

    /// {@inheritDoc} Drives game logic each frame -- invoked by the `SpriteRenderer`
    /// before the scene is drawn.
    @Override
    public void frame(double deltaSeconds) {
        // Keep the renderer's coordinate space in sync with this component's logical
        // size so sprites, the joystick and touch (all in component pixels) line up
        // with what is drawn, regardless of the GPU framebuffer's device-pixel size.
        ((SpriteRenderer) getRenderer()).setLogicalSize(getWidth(), getHeight());
        if (running && !paused) {
            if (fixedTimestep <= 0) {
                update(deltaSeconds);
                interpolationAlpha = 1;
            } else {
                accumulator += deltaSeconds;
                int steps = 0;
                while (accumulator >= fixedTimestep && steps < MAX_FIXED_STEPS) {
                    update(fixedTimestep);
                    accumulator -= fixedTimestep;
                    steps++;
                }
                if (accumulator > fixedTimestep) {
                    accumulator = fixedTimestep;
                }
                interpolationAlpha = accumulator / fixedTimestep;
            }
        }
        relayoutControls();
        input.clearFrameEdges();
    }

    /// Repositions any anchored on-screen controls inside the current safe area, so a
    /// joystick or button pinned to a corner clears notches/home indicators and tracks
    /// rotation without the game recomputing coordinates.
    private void relayoutControls() {
        int sx = 0;
        int sy = 0;
        int sw = getWidth();
        int sh = getHeight();
        Form f = getComponentForm();
        if (f != null) {
            Rectangle safe = f.getSafeArea();
            if (safe != null && safe.getWidth() > 0 && safe.getHeight() > 0) {
                int ax = getAbsoluteX();
                int ay = getAbsoluteY();
                int left = Math.max(0, safe.getX() - ax);
                int top = Math.max(0, safe.getY() - ay);
                int right = Math.min(getWidth(), safe.getX() + safe.getWidth() - ax);
                int bottom = Math.min(getHeight(), safe.getY() + safe.getHeight() - ay);
                if (right > left && bottom > top) {
                    sx = left;
                    sy = top;
                    sw = right - left;
                    sh = bottom - top;
                }
            }
        }
        controls.relayout(sx, sy, sw, sh);
    }

    // ---- input capture ---------------------------------------------------

    /// While running the view consumes all key events (including the directional
    /// pad and fire button) so they are not stolen for focus traversal.
    @Override
    public boolean handlesInput() {
        return running && !paused;
    }

    @Override
    public void keyPressed(int keyCode) {
        input.keyDown(keyCode);
    }

    @Override
    public void keyReleased(int keyCode) {
        input.keyUp(keyCode);
    }

    @Override
    public void pointerPressed(int[] x, int[] y) {
        routeTouches(x, y, true, true, false);
    }

    @Override
    public void pointerDragged(int[] x, int[] y) {
        routeTouches(x, y, true, false, false);
    }

    @Override
    public void pointerReleased(int[] x, int[] y) {
        routeTouches(x, y, false, false, true);
    }

    /// Converts the absolute multi-touch points to view-local pixels, feeds them to
    /// the on-screen `TouchControls` and updates the raw pointer state from the first
    /// touch.
    private void routeTouches(int[] x, int[] y, boolean down, boolean pressed, boolean released) {
        int ox = getAbsoluteX();
        int oy = getAbsoluteY();
        int[] lx = new int[x.length];
        int[] ly = new int[y.length];
        for (int i = 0; i < x.length; i++) {
            lx[i] = x[i] - ox;
            ly[i] = y[i] - oy;
        }
        controls.onTouches(lx, ly, down);
        if (x.length > 0) {
            input.pointer(lx[0], ly[0], down, pressed, released);
        }
    }
}
