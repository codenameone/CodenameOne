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

import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;

/// A `com.codename1.ui.Component` that drives a game loop on top of the Codename
/// One animation system.
///
/// Subclass it and implement `#update(double)` (advance the game) and
/// `#render(com.codename1.ui.Graphics)` (draw a frame). Add the view to a
/// `com.codename1.ui.Form` -- typically as the center of a `BorderLayout` -- and
/// call `#start()`:
///
/// ```java
/// class MyGame extends GameView {
///     Sprite player = new Sprite(playerImage);
///
///     protected void update(double dt) {
///         if (getInput().isGameKeyDown(Display.GAME_RIGHT)) {
///             player.setX(player.getX() + 200 * dt);
///         }
///     }
///     protected void render(Graphics g) {
///         g.setColor(0x101020);
///         g.fillRect(getX(), getY(), getWidth(), getHeight());
///         player.draw(g);
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
/// While running, the view registers itself with the form's animation system and
/// raises the framerate (`#setTargetFramerate(int)`, default 60), restoring the
/// previous framerate when stopped. `#update(double)` is given the elapsed time in
/// seconds since the previous frame; with `#setFixedTimestep(double)` the update is
/// instead stepped at a fixed interval (good for deterministic physics) and
/// `#getInterpolationAlpha()` gives the render side a 0..1 blend factor.
///
/// Input is available as pollable state through `#getInput()`.
///
/// **Both `#update(double)` and `#render(com.codename1.ui.Graphics)` run on the
/// Codename One EDT.** They must not block -- offload asset loading or other long
/// work to a background thread and hand the result back with
/// `com.codename1.ui.CN#callSerially(java.lang.Runnable)`.
public abstract class GameView extends Component {
    private boolean running;
    private boolean paused;
    private boolean attached;
    private long lastTime;
    private int targetFramerate = 60;
    private int savedFramerate = -1;
    private boolean noSleepWhileRunning;
    private double fixedTimestep;
    private double accumulator;
    private double interpolationAlpha = 1;
    private static final int MAX_FIXED_STEPS = 8;
    private final GameInput input = new GameInput();

    public GameView() {
        setFocusable(true);
        setGrabsPointerEvents(true);
    }

    /// Advance the game by the given amount of time. Called once per frame (or
    /// repeatedly at a fixed interval when `#setFixedTimestep(double)` is used).
    ///
    /// #### Parameters
    ///
    /// - `deltaSeconds`: elapsed time since the previous update, in seconds
    protected abstract void update(double deltaSeconds);

    /// Draw a frame. The graphics context is clipped to this component; draw
    /// relative to `#getX()`/`#getY()`.
    protected abstract void render(Graphics g);

    /// The pollable input state for this view.
    public GameInput getInput() {
        return input;
    }

    /// Starts the game loop. Safe to call after the view has been shown; if the
    /// view is not yet attached to a form the loop attaches automatically once it
    /// is.
    public void start() {
        if (running) {
            return;
        }
        running = true;
        paused = false;
        lastTime = System.currentTimeMillis();
        accumulator = 0;
        attach();
        requestFocus();
    }

    /// Stops the game loop and restores the previous framerate.
    public void stop() {
        if (!running) {
            return;
        }
        running = false;
        detach();
    }

    /// Pauses updates without tearing down the loop. The view stays registered but
    /// `#update(double)` is not called until `#resume()`.
    public void pause() {
        paused = true;
    }

    /// Resumes after `#pause()`, resetting the frame clock so the pause gap does
    /// not produce a large delta.
    public void resume() {
        if (paused) {
            paused = false;
            lastTime = System.currentTimeMillis();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isPaused() {
        return paused;
    }

    /// Sets the target framerate applied while the game runs. The framerate is a
    /// global Codename One setting; the previous value is restored on `#stop()`.
    public void setTargetFramerate(int fps) {
        targetFramerate = fps;
        if (attached) {
            Display.getInstance().setFramerate(fps);
        }
    }

    public int getTargetFramerate() {
        return targetFramerate;
    }

    /// When true the EDT does not sleep between frames while the game runs, trading
    /// battery for the highest possible framerate. Defaults to false; rely on
    /// `#setTargetFramerate(int)` for a capped but smooth rate. Always restored on
    /// `#stop()` and when the view is detached.
    public void setNoSleep(boolean noSleep) {
        noSleepWhileRunning = noSleep;
        if (attached) {
            Display.getInstance().setNoSleep(noSleep);
        }
    }

    public boolean isNoSleep() {
        return noSleepWhileRunning;
    }

    /// Sets a fixed update interval in seconds (0 disables, the default, giving a
    /// variable timestep). With a fixed timestep `#update(double)` may be called
    /// several times per frame to catch up, and `#getInterpolationAlpha()` returns
    /// the leftover fraction for render side interpolation.
    public void setFixedTimestep(double seconds) {
        fixedTimestep = seconds < 0 ? 0 : seconds;
    }

    public double getFixedTimestep() {
        return fixedTimestep;
    }

    /// The 0..1 fraction of a fixed step left in the accumulator after the last
    /// update, for interpolating rendered positions between physics states. Always
    /// 1 when a variable timestep is in use.
    public double getInterpolationAlpha() {
        return interpolationAlpha;
    }

    private void attach() {
        if (attached) {
            return;
        }
        Form f = getComponentForm();
        if (f == null) {
            return;
        }
        f.registerAnimated(this);
        savedFramerate = Display.getInstance().getFrameRate();
        Display.getInstance().setFramerate(targetFramerate);
        if (noSleepWhileRunning) {
            Display.getInstance().setNoSleep(true);
        }
        attached = true;
    }

    private void detach() {
        if (!attached) {
            return;
        }
        Form f = getComponentForm();
        if (f != null) {
            f.deregisterAnimated(this);
        }
        if (savedFramerate > 0) {
            Display.getInstance().setFramerate(savedFramerate);
        }
        if (noSleepWhileRunning) {
            Display.getInstance().setNoSleep(false);
        }
        attached = false;
    }

    protected void initComponent() {
        super.initComponent();
        if (running) {
            attach();
            requestFocus();
            lastTime = System.currentTimeMillis();
        }
    }

    protected void deinitialize() {
        // Release the framerate/no-sleep hold while detached so a backgrounded
        // game does not keep the EDT hot; running state is preserved so the loop
        // resumes if the view is shown again.
        detach();
        super.deinitialize();
    }

    /// {@inheritDoc}
    public boolean animate() {
        if (!running || paused) {
            return false;
        }
        long now = System.currentTimeMillis();
        double dt = (now - lastTime) / 1000.0;
        lastTime = now;
        if (dt < 0) {
            dt = 0;
        }
        if (dt > 0.25) {
            // clamp huge gaps (GC pause, app backgrounded) to avoid a spiral of death
            dt = 0.25;
        }
        if (fixedTimestep <= 0) {
            update(dt);
            interpolationAlpha = 1;
        } else {
            accumulator += dt;
            int steps = 0;
            while (accumulator >= fixedTimestep && steps < MAX_FIXED_STEPS) {
                update(fixedTimestep);
                accumulator -= fixedTimestep;
                steps++;
            }
            if (accumulator > fixedTimestep) {
                // drop backlog beyond the step cap
                accumulator = fixedTimestep;
            }
            interpolationAlpha = accumulator / fixedTimestep;
        }
        input.clearFrameEdges();
        return true;
    }

    /// {@inheritDoc}
    public void paint(Graphics g) {
        g.setAntiAliased(true);
        render(g);
    }

    /// While running the view consumes all key events (including the directional
    /// pad and fire button) so they are not stolen for focus traversal.
    public boolean handlesInput() {
        return running && !paused;
    }

    public void keyPressed(int keyCode) {
        input.keyDown(keyCode);
    }

    public void keyReleased(int keyCode) {
        input.keyUp(keyCode);
    }

    public void pointerPressed(int x, int y) {
        input.pointer(x - getAbsoluteX(), y - getAbsoluteY(), true, true, false);
    }

    public void pointerDragged(int x, int y) {
        input.pointer(x - getAbsoluteX(), y - getAbsoluteY(), true, false, false);
    }

    public void pointerReleased(int x, int y) {
        input.pointer(x - getAbsoluteX(), y - getAbsoluteY(), false, false, true);
    }

    protected Dimension calcPreferredSize() {
        Display d = Display.getInstance();
        return new Dimension(d.getDisplayWidth(), d.getDisplayHeight());
    }
}
