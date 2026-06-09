package com.codename1.samples;

import com.codename1.gaming.GameCamera;
import com.codename1.gaming.GameView;
import com.codename1.gaming.Model;
import com.codename1.gaming.Sprite;
import com.codename1.gaming.TouchControls;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.Material;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.Primitives;
import com.codename1.gpu.Texture;
import com.codename1.io.Log;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;

/// Demonstrates the 3D side of {@code com.codename1.gaming}: a {@link GameView} with
/// its {@link GameCamera} in perspective mode, showing a lit spinning 3D cube
/// ({@link Model}) on a ground plane surrounded by billboarded {@link Sprite} coins
/// that always face the orbiting camera. Everything is generated at runtime, so the
/// sample needs no assets.
public class Gaming3DDemoSample {
    private Form current;
    private Resources theme;

    public void init(Object context) {
        theme = UIManager.initFirstTheme("/theme");
        Toolbar.setGlobalToolbar(true);
        Log.bindCrashProtection(true);
    }

    public void start() {
        if (current != null) {
            current.show();
            return;
        }
        Form f = new Form("3D Gaming Demo", new BorderLayout());
        f.add(BorderLayout.NORTH, new Label("Perspective billboards + a 3D model"));
        World3DView game = new World3DView();
        f.add(BorderLayout.CENTER, game);
        f.show();
        game.start();
    }

    public void stop() {
        current = Display.getInstance().getCurrent();
    }

    public void destroy() {
    }

    /// The 3D game surface.
    static class World3DView extends GameView {
        private static final int COIN_COUNT = 10;
        private static final float COIN_RING = 5f;
        private static final int BUILDINGS = 8;

        private final Model[] monument = new Model[3];
        private final Sprite[] coins = new Sprite[COIN_COUNT];
        private double time;
        private double orbit;
        private float camHeight = 7f;
        private boolean controlsReady;

        World3DView() {
            setClearColor(0xff1a2a4a);   // dusk sky
            getCamera()
                    .setPerspective(60f, 0.1f, 300f)
                    .setPosition(0f, 7f, 15f)
                    .setTarget(0f, 2.5f, 0f);

            // a ring of billboarded coins floating in 3D world space
            Image coinImage = makeCoin(48, 0xffffd54a);
            for (int i = 0; i < COIN_COUNT; i++) {
                double a = i * 2 * Math.PI / COIN_COUNT;
                coins[i] = new Sprite(coinImage);
                coins[i].setPosition(Math.cos(a) * COIN_RING, 1.4, Math.sin(a) * COIN_RING);
                coins[i].setSize(1.1f, 1.1f);   // world units, not pixels
                getScene().add(coins[i]);
            }
            // a few billboarded trees for scenery (always face the camera)
            Image treeImage = makeTree(72);
            double[] tx = {-11, 10, -9, 12, 2};
            double[] tz = {-10, -8, 9, 7, -13};
            for (int i = 0; i < tx.length; i++) {
                Sprite tree = new Sprite(treeImage);
                tree.setPosition(tx[i], 1.8, tz[i]);
                tree.setSize(3.6f, 3.6f);
                getScene().add(tree);
            }
        }

        protected void onSetup(GraphicsDevice device) {
            // textured grid ground: a quad rotated flat, lit and tinted green
            Mesh groundMesh = Primitives.quad(device, 64f);
            Texture grid = device.createTexture(makeGrid(256, 0xffe8efe8, 0xff9fc0a0, 8));
            Material groundMat = new Material(Material.Type.LAMBERT)
                    .setColor(0xff3f7d4f).setTexture(grid);
            Model ground = new Model(groundMesh, groundMat);
            ground.setRotation(-90f, 0f, 0f);   // XY quad -> horizontal XZ ground
            addModel(ground);

            // central spinning monument: three stacked, shiny cubes
            Mesh cubeMesh = Primitives.cube(device, 1f);
            int[] mcol = {0xffffcc33, 0xff33c7c7, 0xffff7043};
            float[] msize = {2.6f, 1.7f, 1.0f};
            float y = 0f;
            for (int i = 0; i < 3; i++) {
                y += msize[i] / 2f;
                Material m = new Material(Material.Type.PHONG).setColor(mcol[i]).setShininess(24f);
                Model c = new Model(cubeMesh, m);
                c.setScale(msize[i]);
                c.setPosition(0f, y, 0f);
                monument[i] = c;
                addModel(c);
                y += msize[i] / 2f;
            }

            // a ring of colored "buildings" of varying height (one cube mesh, scaled)
            int[] bcol = {0xff5b8def, 0xffe06ca0, 0xff8bd450, 0xfff2b134,
                0xff9b6cf0, 0xff4fd1c5, 0xffef6f6f, 0xffb0bec5};
            for (int i = 0; i < BUILDINGS; i++) {
                double a = i * 2 * Math.PI / BUILDINGS;
                float h = 1.6f + (i % 4) * 0.9f;
                Material m = new Material(Material.Type.LAMBERT).setColor(bcol[i % bcol.length]);
                Model b = new Model(cubeMesh, m);
                b.setScale(1.5f, h, 1.5f);
                b.setPosition((float) (Math.cos(a) * 9.5), h / 2f, (float) (Math.sin(a) * 9.5));
                addModel(b);
            }

            getLight().setDirection(-0.4f, -1f, -0.3f).setColor(0xfffff4e0)
                    .setAmbientColor(0xff3a4257);
        }

        protected void update(double dt) {
            time += dt;
            // each monument tier spins at its own speed and direction
            for (int i = 0; i < monument.length; i++) {
                if (monument[i] != null) {
                    float dir = (i % 2 == 0) ? 1f : -1f;
                    monument[i].setRotation(0f, (float) (time * (30 + i * 22)) * dir, 0f);
                }
            }
            // bob the coins up and down
            for (int i = 0; i < COIN_COUNT; i++) {
                coins[i].setY(1.4 + Math.sin(time * 2 + i) * 0.3);
            }
            if (!controlsReady && getWidth() > 0) {
                getControls().addJoystick(80, TouchControls.LEFT, TouchControls.BOTTOM, 30);
                controlsReady = true;
            }
            // the joystick orbits the camera (x) and raises/lowers it (y); when
            // untouched the camera drifts slowly on its own.
            float ax = getInput().getAxisX();
            float ay = getInput().getAxisY();
            orbit += (ax != 0 ? ax * 1.5 : 0.25) * dt;
            camHeight = clamp(camHeight + ay * 7f * (float) dt, 2.5f, 18f);
            float r = 15f;
            getCamera().setPosition((float) (Math.sin(orbit) * r), camHeight,
                    (float) (Math.cos(orbit) * r)).setTarget(0f, 2.5f, 0f);
        }

        private static float clamp(float v, float lo, float hi) {
            return v < lo ? lo : (v > hi ? hi : v);
        }
    }

    /// Builds a round "coin" sprite texture with a transparent background.
    static Image makeCoin(int size, int color) {
        Image img = Image.createImage(size, size, 0); // 0 == transparent
        Graphics g = img.getGraphics();
        g.setAntiAliased(true);
        g.setColor(color);
        g.fillArc(0, 0, size - 1, size - 1, 0, 360);
        g.setColor(0xfffff2b0);
        g.fillArc(size / 4, size / 5, size / 3, size / 3, 0, 360); // highlight
        return img;
    }

    /// Builds a grid texture (a base fill crossed by `cells` evenly spaced lines each
    /// way) used to tint and detail the ground so the 3D space reads with depth.
    static Image makeGrid(int size, int baseColor, int lineColor, int cells) {
        Image img = Image.createImage(size, size, baseColor);
        Graphics g = img.getGraphics();
        g.setColor(lineColor);
        int step = size / cells;
        for (int i = 0; i <= cells; i++) {
            int p = Math.min(size - 2, i * step);
            g.fillRect(p, 0, 2, size);   // vertical line
            g.fillRect(0, p, size, 2);   // horizontal line
        }
        return img;
    }

    /// Builds a simple billboard tree (brown trunk, layered green canopy) on a
    /// transparent background.
    static Image makeTree(int size) {
        Image img = Image.createImage(size, size, 0);
        Graphics g = img.getGraphics();
        g.setAntiAliased(true);
        g.setColor(0xff7a4a26);
        g.fillRect(size / 2 - size / 14, size * 3 / 5, size / 7, size * 2 / 5); // trunk
        g.setColor(0xff2f8f3f);
        g.fillArc(size / 6, size / 5, size * 2 / 3, size * 2 / 3, 0, 360);       // canopy
        g.setColor(0xff3fa84f);
        g.fillArc(size / 4, size / 8, size / 2, size / 2, 0, 360);               // highlight
        return img;
    }
}
