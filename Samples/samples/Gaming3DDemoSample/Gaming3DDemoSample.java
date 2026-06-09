package com.codename1.samples;

import com.codename1.gaming.GameCamera;
import com.codename1.gaming.GameView;
import com.codename1.gaming.Model;
import com.codename1.gaming.Sprite;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.Material;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.Primitives;
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
        private static final int COIN_COUNT = 8;
        private static final float COIN_RING = 4f;

        private Model cube;
        private double time;
        private double orbit;
        private float camHeight = 6f;
        private boolean controlsReady;

        World3DView() {
            setClearColor(0xff101824);
            // perspective camera looking at the cube from above and behind
            getCamera()
                    .setPerspective(60f, 0.1f, 200f)
                    .setPosition(0f, 6f, 12f)
                    .setTarget(0f, 1f, 0f);

            // a ring of billboarded coins in 3D world space
            Image coinImage = makeCoin(48, 0xffffd54a);
            for (int i = 0; i < COIN_COUNT; i++) {
                double a = i * 2 * Math.PI / COIN_COUNT;
                Sprite coin = new Sprite(coinImage);
                coin.setPosition(Math.cos(a) * COIN_RING, 1.2, Math.sin(a) * COIN_RING);
                coin.setSize(1.2f, 1.2f);   // world units, not pixels
                getScene().add(coin);
            }
        }

        protected void onSetup(GraphicsDevice device) {
            // ground plane: a quad rotated flat, lit and green
            Mesh quad = Primitives.quad(device, 16f);
            Material grass = new Material(Material.Type.LAMBERT).setColor(0xff2f7d32);
            Model ground = new Model(quad, grass);
            ground.setRotation(-90f, 0f, 0f);   // XY quad -> horizontal XZ ground
            addModel(ground);

            // a gold spinning cube sitting on the ground
            Mesh cubeMesh = Primitives.cube(device, 2f);
            Material gold = new Material(Material.Type.PHONG).setColor(0xffffcc33).setShininess(32f);
            cube = new Model(cubeMesh, gold);
            cube.setPosition(0f, 1f, 0f);
            addModel(cube);

            getLight().setDirection(-0.5f, -1f, -0.4f).setColor(0xffffffff).setAmbientColor(0xff404048);
        }

        protected void update(double dt) {
            time += dt;
            if (cube != null) {
                cube.setRotation(0f, (float) (time * 60), 0f);   // spin around Y
            }
            if (!controlsReady && getWidth() > 0) {
                getControls().addJoystick(110, getHeight() - 110, 80);
                controlsReady = true;
            }
            // the joystick orbits the camera (x) and raises/lowers it (y); when
            // untouched the camera drifts on its own.
            float ax = getInput().getAxisX();
            float ay = getInput().getAxisY();
            orbit += (ax != 0 ? ax * 1.5 : 0.3) * dt;
            camHeight = clamp(camHeight + ay * 6f * (float) dt, 1.5f, 14f);
            float r = 12f;
            getCamera().setPosition((float) (Math.sin(orbit) * r), camHeight,
                    (float) (Math.cos(orbit) * r));
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
}
