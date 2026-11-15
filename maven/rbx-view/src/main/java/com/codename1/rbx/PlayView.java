/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.rbx;

import com.codename1.gpu.RenderView;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.util.UITimer;

/// A playable place: the 3D view, the place's own GUI over it, and touch,
/// keyboard and pointer controls. It owns the frame loop: every tick steps
/// the engine, moves the camera and hands the renderer a snapshot.
///
/// Touch: the left half of the screen is a floating joystick, a drag on the
/// right half orbits the camera, the round button jumps. Keys: WASD or the
/// arrows walk, Space jumps.
public class PlayView extends Container {
    private static final int TICK_MS = 16;

    private final Engine engine;
    private final PartRenderer renderer = new PartRenderer();
    private final RenderView view;
    private final CameraRig camera = new CameraRig();
    private final GuiPainter gui = new GuiPainter();
    private final Overlay overlay = new Overlay();
    private UITimer timer;
    private long last;
    private boolean paused;
    private Runnable afterStep;

    public PlayView(Engine engine) {
        super(new LayeredLayout());
        this.engine = engine;
        engine.setEffectSink(renderer);
        view = new RenderView(renderer);
        view.setContinuous(true);
        add(view).add(overlay);
    }

    public Engine getEngine() {
        return engine;
    }

    public CameraRig getCamera() {
        return camera;
    }

    /// True when this device can draw the 3D view.
    public boolean isSupported() {
        return view.isSupported();
    }

    /// Runs `r` on the EDT right after every engine step, before the frame
    /// is drawn: a multiplayer client sends its movement here, so what it
    /// sends is exactly the frame the player sees.
    public void setAfterStep(Runnable r) {
        afterStep = r;
    }

    /// Pauses or resumes simulation (the in-experience menu).
    public void setPaused(boolean p) {
        paused = p;
        last = 0;
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        final Form f = getComponentForm();
        timer = UITimer.timer(TICK_MS, true, f, new Runnable() {
            @Override
            public void run() {
                tick();
            }
        });
        overlay.requestFocus();
    }

    @Override
    protected void deinitialize() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        super.deinitialize();
    }

    /// The `KeyCode` name of a Codename One key code, or null.
    static String keyName(int code) {
        if (code >= 'a' && code <= 'z') {
            return String.valueOf((char) (code - 32));
        }
        if (code >= 'A' && code <= 'Z' || code >= '0' && code <= '9') {
            return code <= '9' ? numberName(code) : String.valueOf((char) code);
        }
        if (code == ' ') {
            return "Space";
        }
        int ga = Display.getInstance().getGameAction(code);
        switch (ga) {
            case Display.GAME_UP:
                return "Up";
            case Display.GAME_DOWN:
                return "Down";
            case Display.GAME_LEFT:
                return "Left";
            case Display.GAME_RIGHT:
                return "Right";
            case Display.GAME_FIRE:
                return "Space";
            default:
                return null;
        }
    }

    private static String numberName(int code) {
        String[] n = {"Zero", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine"};
        return n[code - '0'];
    }

    private float scale() {
        return CN.convertToPixels(1) * 25.4f / 160f;
    }

    void tick() {
        if (paused) {
            return;
        }
        long now = System.currentTimeMillis();
        double dt = last == 0 ? TICK_MS / 1000.0 : Math.min(0.1, (now - last) / 1000.0);
        last = now;
        float s = scale();
        Object cam = engine.getWorkspace().prop("CurrentCamera");
        if (cam instanceof Instance) {
            ((Instance) cam).put("ViewportSize", new Vector2(getWidth() / s, getHeight() / s));
        }
        Input.move(engine, overlay.stickX, overlay.stickY);
        engine.step(dt);
        if (afterStep != null) {
            afterStep.run();
        }
        camera.update(engine);
        renderer.submit(SceneSnapshot.of(engine));
        overlay.repaint();
    }

    /// GUI, joystick and jump button; routes pointers to the place first.
    final class Overlay extends Component {
        double stickX;
        double stickY;
        private int stickId = -1;
        private int cx;
        private int cy;
        private int lastX;
        private int lastY;
        private boolean orbiting;

        Overlay() {
            setFocusable(true);
            getAllStyles().setBgTransparency(0);
        }

        private int jumpX() {
            return getWidth() - CN.convertToPixels(14);
        }

        private int jumpY() {
            return getHeight() - CN.convertToPixels(14);
        }

        private int jumpR() {
            return CN.convertToPixels(7);
        }

        @Override
        public void paint(Graphics g) {
            g.translate(getX(), getY());
            gui.paint(g, engine, scale());
            g.translate(-getX(), -getY());
            g.setAlpha(90);
            g.setColor(0xffffff);
            int r = jumpR();
            g.fillArc(getX() + jumpX() - r, getY() + jumpY() - r, r * 2, r * 2, 0, 360);
            if (stickId >= 0) {
                int sr = CN.convertToPixels(10);
                g.drawArc(getX() + cx - sr, getY() + cy - sr, sr * 2, sr * 2, 0, 360);
                int kx = cx + (int) (stickX * sr);
                int ky = cy - (int) (stickY * sr);
                g.fillArc(getX() + kx - sr / 3, getY() + ky - sr / 3, sr * 2 / 3, sr * 2 / 3, 0, 360);
            }
            g.setAlpha(255);
        }

        private boolean onJump(int x, int y) {
            int dx = x - jumpX();
            int dy = y - jumpY();
            return dx * dx + dy * dy <= jumpR() * jumpR();
        }

        @Override
        public void pointerPressed(int x, int y) {
            int lx = x - getAbsoluteX();
            int ly = y - getAbsoluteY();
            float s = scale();
            Input.pointer(engine, lx / s, ly / s, 0, true);
            if (onJump(lx, ly)) {
                Input.key(engine, "Space", true);
                return;
            }
            if (lx < getWidth() / 2) {
                stickId = 0;
                cx = lx;
                cy = ly;
            } else {
                orbiting = true;
                lastX = lx;
                lastY = ly;
            }
        }

        @Override
        public void pointerDragged(int x, int y) {
            int lx = x - getAbsoluteX();
            int ly = y - getAbsoluteY();
            float s = scale();
            Input.pointer(engine, lx / s, ly / s, 1, true);
            if (stickId >= 0) {
                double sr = CN.convertToPixels(10);
                double dx = (lx - cx) / sr;
                double dy = -(ly - cy) / sr;
                double m = Math.sqrt(dx * dx + dy * dy);
                if (m > 1) {
                    dx /= m;
                    dy /= m;
                }
                stickX = dx;
                stickY = dy;
            } else if (orbiting) {
                camera.orbit(-(lx - lastX) * 0.01, (ly - lastY) * 0.01);
                lastX = lx;
                lastY = ly;
            }
        }

        @Override
        public void pointerReleased(int x, int y) {
            int lx = x - getAbsoluteX();
            int ly = y - getAbsoluteY();
            float s = scale();
            Input.pointer(engine, lx / s, ly / s, 2, true);
            Input.key(engine, "Space", false);
            stickId = -1;
            stickX = 0;
            stickY = 0;
            orbiting = false;
        }

        @Override
        public void keyPressed(int code) {
            String k = keyName(code);
            if (k != null) {
                Input.key(engine, k, true);
            }
        }

        @Override
        public void keyReleased(int code) {
            String k = keyName(code);
            if (k != null) {
                Input.key(engine, k, false);
            }
        }
    }
}
