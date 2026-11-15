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
    private static final double STEP = 1 / 60.0;
    private static final int MAX_STEPS = 6;

    private final Engine engine;
    private final PartRenderer renderer = new PartRenderer();
    private final RenderView view;
    private final CameraRig camera = new CameraRig();
    private final GuiPainter gui = new GuiPainter();
    private final Overlay overlay = new Overlay();
    private UITimer timer;
    private long last;
    private boolean paused;
    private boolean editMode;
    private Runnable afterStep;
    private PickListener pickListener;
    private TransformListener transformListener;
    private final Gizmo gizmo = new Gizmo();
    private boolean savedMultiKey;

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

    /// The studio's viewport: nothing simulates, there are no touch
    /// controls, and every drag orbits the camera around its pivot (see
    /// [CameraRig#setPivot]).
    public void setEditMode(boolean edit) {
        editMode = edit;
    }

    /// Told which part a click in the edit-mode viewport landed on.
    public interface PickListener {
        /// `part` is the nearest part under the pointer, or null for a
        /// click on the sky.
        void picked(Instance part);
    }

    /// Told when a drag on the gizmo starts and ends, so an editor can make
    /// the whole drag one undo step.
    public interface TransformListener {
        /// Before the first change of a drag.
        void transformStarted();

        /// After the last change of a drag.
        void transformFinished();
    }

    public void setTransformListener(TransformListener l) {
        transformListener = l;
    }

    /// The edit-mode selection outline and transform handles.
    public Gizmo getGizmo() {
        return gizmo;
    }

    /// Clicks (a press and release without a drag) in edit mode select:
    /// `l` hears the part under the pointer.
    public void setPickListener(PickListener l) {
        pickListener = l;
    }

    /// The nearest part along the camera ray through the view pixel
    /// (`px`, `py`) of a `w` by `h` view, or null. Parts are tested as the
    /// oriented boxes they are; a sphere or cylinder is picked by its box,
    /// which is what an editor's click needs.
    public static Instance pick(Engine e, int px, int py, int w, int h) {
        Object c = e.getWorkspace().prop("CurrentCamera");
        if (!(c instanceof Instance) || w <= 0 || h <= 0) {
            return null;
        }
        Instance cam = (Instance) c;
        CFrame f = Behaviors.cf(cam);
        double fov = cam.num("FieldOfView");
        double ty = Math.tan(Math.toRadians(fov <= 0 ? 70 : fov) / 2);
        double tx = ty * w / h;
        double nx = (2.0 * px / w - 1) * tx;
        double ny = (1 - 2.0 * py / h) * ty;
        // columns: right, up, back
        double dx = f.r00 * nx + f.r01 * ny - f.r02;
        double dy = f.r10 * nx + f.r11 * ny - f.r12;
        double dz = f.r20 * nx + f.r21 * ny - f.r22;
        double[] best = {Double.MAX_VALUE};
        Instance[] hit = new Instance[1];
        pickIn(e.getWorkspace(), f.x, f.y, f.z, dx, dy, dz, best, hit);
        return hit[0];
    }

    private static void pickIn(Instance parent, double ox, double oy, double oz, double dx, double dy, double dz,
            double[] best, Instance[] hit) {
        java.util.List<Instance> kids = parent.getChildren();
        for (int i = 0; i < kids.size(); i++) { // NOPMD ForLoopCanBeForeach - the tree may be large
            Instance k = kids.get(i);
            if (k.isA("BasePart") && k.num("Transparency") < 0.95) {
                double t = rayBox(k, ox, oy, oz, dx, dy, dz);
                if (t >= 0 && t < best[0]) {
                    best[0] = t;
                    hit[0] = k;
                }
            }
            pickIn(k, ox, oy, oz, dx, dy, dz, best, hit);
        }
    }

    /// The ray parameter where the ray enters the part's box, or -1.
    static double rayBox(Instance part, double ox, double oy, double oz, double dx, double dy, double dz) {
        Object sz = part.prop("Size");
        if (!(sz instanceof Vector3)) {
            return -1;
        }
        Vector3 size = (Vector3) sz;
        CFrame f = Behaviors.cf(part);
        double rx = ox - f.x;
        double ry = oy - f.y;
        double rz = oz - f.z;
        // into the part's frame: the transpose of its rotation
        double[] o = {f.r00 * rx + f.r10 * ry + f.r20 * rz, f.r01 * rx + f.r11 * ry + f.r21 * rz,
            f.r02 * rx + f.r12 * ry + f.r22 * rz};
        double[] d = {f.r00 * dx + f.r10 * dy + f.r20 * dz, f.r01 * dx + f.r11 * dy + f.r21 * dz,
            f.r02 * dx + f.r12 * dy + f.r22 * dz};
        double[] half = {size.x / 2, size.y / 2, size.z / 2};
        double near = 0;
        double far = Double.MAX_VALUE;
        for (int a = 0; a < 3; a++) {
            if (Math.abs(d[a]) < 1e-12) {
                if (o[a] < -half[a] || o[a] > half[a]) {
                    return -1;
                }
                continue;
            }
            double t1 = (-half[a] - o[a]) / d[a];
            double t2 = (half[a] - o[a]) / d[a];
            near = Math.max(near, Math.min(t1, t2));
            far = Math.min(far, Math.max(t1, t2));
            if (near > far) {
                return -1;
            }
        }
        return near;
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
        // A player holds W while pressing A and Space. Outside multi-key mode
        // Display drops the release of any key but the last two pressed, so W
        // stayed down and the character ran on into the lava.
        savedMultiKey = Display.getInstance().isMultiKeyMode();
        Display.getInstance().setMultiKeyMode(true);
    }

    @Override
    protected void deinitialize() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        Display.getInstance().setMultiKeyMode(savedMultiKey);
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
        if (editMode) {
            camera.update(engine);
            renderer.submit(SceneSnapshot.of(engine));
            Object c = engine.getWorkspace().prop("CurrentCamera");
            if (c instanceof Instance) {
                CFrame eye = Behaviors.cf((Instance) c);
                renderer.submitGuides(gizmo.guides(eye.x, eye.y, eye.z));
            }
            return;
        }
        Input.move(engine, overlay.stickX, overlay.stickY);
        // Fixed 60 Hz steps, as Roblox simulates: one long step on a slow
        // frame let a falling character pass clean through a thin kill brick.
        // A frame catches up at most MAX_STEPS steps, so a stall cannot snowball.
        int steps = 0;
        double left = dt;
        while (left > 1e-6 && steps < MAX_STEPS) {
            double h = Math.min(STEP, left);
            engine.step(h);
            left -= h;
            steps++;
        }
        if (afterStep != null) {
            afterStep.run();
        }
        camera.update(engine);
        renderer.submit(SceneSnapshot.of(engine));
        hudTick++;
        if (hudTick % HUD_EVERY == 0) {
            submitHud();
        }
    }

    /// Ticks between HUD repaints: the HUD changes far slower than the scene.
    private static final int HUD_EVERY = 3;
    private int hudTick;

    /// Paints the GUI and the touch controls at half resolution and hands
    /// them to the renderer, which draws them over the 3D frame.
    private void submitHud() {
        int w = overlay.getWidth();
        int h = overlay.getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        boolean half = com.codename1.ui.Image.createImage(1, 1, 0).getGraphics().isAffineSupported();
        int hw = half ? Math.max(1, w / 2) : w;
        int hh = half ? Math.max(1, h / 2) : h;
        com.codename1.ui.Image img = com.codename1.ui.Image.createImage(hw, hh, 0);
        Graphics g = img.getGraphics();
        g.setAntiAliased(true);
        if (half) {
            g.scale(0.5f, 0.5f);
        }
        overlay.paintHud(g);
        renderer.submitHud(img.getRGB(), hw, hh);
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
        private int downX;
        private int downY;
        private boolean transforming;

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
            // Nothing: the HUD reaches the screen through the renderer (see
            // submitHud), since a GPU view is drawn over lightweight paint.
        }

        /// The GUI, the jump button and the joystick, at the origin.
        void paintHud(Graphics g) {
            gui.paint(g, engine, scale());
            int r = jumpR();
            g.setAlpha(70);
            g.setColor(0x000000);
            g.fillArc(jumpX() - r, jumpY() - r, r * 2, r * 2, 0, 360);
            g.setAlpha(230);
            g.setColor(0xffffff);
            g.drawArc(jumpX() - r, jumpY() - r, r * 2, r * 2, 0, 360);
            g.drawArc(jumpX() - r + 1, jumpY() - r + 1, r * 2 - 2, r * 2 - 2, 0, 360);
            // an up chevron: the jump glyph players know
            int a = r / 2;
            g.fillTriangle(jumpX(), jumpY() - a, jumpX() - a, jumpY() + a / 2, jumpX() + a, jumpY() + a / 2);
            if (stickId >= 0) {
                int sr = CN.convertToPixels(10);
                g.setAlpha(60);
                g.setColor(0x000000);
                g.fillArc(cx - sr, cy - sr, sr * 2, sr * 2, 0, 360);
                g.setAlpha(200);
                g.setColor(0xffffff);
                g.drawArc(cx - sr, cy - sr, sr * 2, sr * 2, 0, 360);
                int kx = cx + (int) (stickX * sr);
                int ky = cy - (int) (stickY * sr);
                g.fillArc(kx - sr / 3, ky - sr / 3, sr * 2 / 3, sr * 2 / 3, 0, 360);
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
            if (!editMode && onJump(lx, ly)) {
                Input.key(engine, "Space", true);
                return;
            }
            if (editMode && gizmo.press(engine, lx, ly, getWidth(), getHeight(),
                    pick(engine, lx, ly, getWidth(), getHeight()))) {
                transforming = true;
                if (transformListener != null) {
                    transformListener.transformStarted();
                }
                return;
            }
            if (!editMode && lx < getWidth() / 2) {
                stickId = 0;
                cx = lx;
                cy = ly;
            } else {
                orbiting = true;
                lastX = lx;
                lastY = ly;
                downX = lx;
                downY = ly;
            }
        }

        @Override
        public void pointerDragged(int x, int y) {
            int lx = x - getAbsoluteX();
            int ly = y - getAbsoluteY();
            float s = scale();
            Input.pointer(engine, lx / s, ly / s, 1, true);
            if (transforming) {
                gizmo.drag(engine, lx, ly, getWidth(), getHeight());
                return;
            }
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
            if (transforming) {
                transforming = false;
                gizmo.release();
                if (transformListener != null) {
                    transformListener.transformFinished();
                }
                return;
            }
            int slop = CN.convertToPixels(1);
            if (editMode && orbiting && pickListener != null
                    && Math.abs(lx - downX) <= slop && Math.abs(ly - downY) <= slop) {
                pickListener.picked(pick(engine, lx, ly, getWidth(), getHeight()));
            }
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
