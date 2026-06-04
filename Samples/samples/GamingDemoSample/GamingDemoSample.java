package com.codename1.samples;

import com.codename1.gaming.GameView;
import com.codename1.gaming.Scene;
import com.codename1.gaming.Sprite;
import com.codename1.gaming.SoundEffect;
import com.codename1.gaming.SoundPool;
import com.codename1.gaming.physics.BodyType;
import com.codename1.gaming.physics.PhysicsBody;
import com.codename1.gaming.physics.PhysicsWorld;
import com.codename1.io.Log;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import java.io.ByteArrayInputStream;

/// Demonstrates the {@code com.codename1.gaming} package end to end: a {@link GameView}
/// game loop, sprite rendering, Box2D physics and a low latency {@link SoundPool}.
///
/// Tap anywhere to drop a ball; it falls under gravity, bounces off the floor and
/// walls and plays a short blip whose pitch varies per drop.
public class GamingDemoSample {
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
        Form f = new Form("Gaming Demo", new BorderLayout());
        f.add(BorderLayout.CENTER, new PhysicsDemoView());
        f.show();
    }

    public void stop() {
        current = Display.getInstance().getCurrent();
    }

    public void destroy() {
    }

    /// The actual game surface.
    static class PhysicsDemoView extends GameView {
        private PhysicsWorld world;
        private Scene scene;
        private SoundPool sfx;
        private SoundEffect blip;
        private Image ballImage;
        private int rateSeed;
        private boolean ready;
        private double fps;

        PhysicsDemoView() {
            ballImage = makeBall(28, 0xff5a5f);
            sfx = SoundPool.create(12);
            try {
                blip = sfx.load(new ByteArrayInputStream(makeBlipWav(660, 120)), "audio/wav");
            } catch (Exception e) {
                Log.e(e);
            }
            setTargetFramerate(60);
            // start once attached; init() (component lifecycle) fires after the form shows
        }

        protected void initComponent() {
            super.initComponent();
            start();
        }

        protected void deinitialize() {
            stop();
            super.deinitialize();
        }

        private void setupWorld() {
            int w = getWidth();
            int h = getHeight();
            world = new PhysicsWorld(0, 900); // gravity, pixels/s^2 downward
            scene = new Scene();
            // floor + side walls (static), positioned in view-local coordinates
            world.createBox(w / 2f, h - 10, w, 20, BodyType.STATIC);
            world.createBox(-10, h / 2f, 20, h * 2f, BodyType.STATIC);
            world.createBox(w + 10, h / 2f, 20, h * 2f, BodyType.STATIC);
            ready = true;
        }

        private void dropBall(float x, float y) {
            if (!ready) {
                return;
            }
            PhysicsBody body = world.createCircle(x, y, 14, BodyType.DYNAMIC);
            body.setRestitution(0.6f);
            Sprite s = new Sprite(ballImage);
            s.setPosition(x, y);
            body.setLinkedSprite(s);
            scene.add(s);
            if (blip != null) {
                float rate = 0.7f + ((rateSeed++ % 8) * 0.12f); // vary pitch per drop
                sfx.play(blip, 0.9f, 0f, rate, 0);
            }
        }

        protected void update(double dt) {
            if (!ready) {
                if (getWidth() > 0 && getHeight() > 0) {
                    setupWorld();
                } else {
                    return;
                }
            }
            if (dt > 0) {
                fps = fps * 0.9 + (1.0 / dt) * 0.1;
            }
            if (getInput().wasPointerPressed()) {
                dropBall(getInput().getPointerX(), getInput().getPointerY());
            }
            world.step((float) dt);
        }

        protected void render(Graphics g) {
            int ox = getX();
            int oy = getY();
            g.setColor(0x101826);
            g.fillRect(ox, oy, getWidth(), getHeight());
            g.translate(ox, oy);
            if (scene != null) {
                scene.render(g);
            }
            g.translate(-ox, -oy);
            g.setColor(0xffffff);
            g.drawString("Tap to drop a ball  |  balls: "
                    + (scene == null ? 0 : scene.size())
                    + "  |  fps: " + Math.round(fps), ox + 10, oy + 10);
        }
    }

    /// Builds a small round sprite image with a transparent background.
    static Image makeBall(int size, int color) {
        Image img = Image.createImage(size, size, 0); // 0 == fully transparent
        Graphics g = img.getGraphics();
        g.setAntiAliased(true);
        g.setColor(color);
        g.fillArc(0, 0, size - 1, size - 1, 0, 360);
        g.setColor(0xffffff);
        g.fillArc(size / 4, size / 5, size / 4, size / 4, 0, 360); // highlight
        return img;
    }

    /// Generates a tiny 16-bit mono WAV of a decaying sine tone so the demo needs no
    /// audio asset.
    static byte[] makeBlipWav(int freq, int millis) {
        int sampleRate = 44100;
        int samples = sampleRate * millis / 1000;
        int dataLen = samples * 2;
        byte[] out = new byte[44 + dataLen];
        writeStr(out, 0, "RIFF");
        writeIntLE(out, 4, 36 + dataLen);
        writeStr(out, 8, "WAVE");
        writeStr(out, 12, "fmt ");
        writeIntLE(out, 16, 16);          // fmt chunk size
        writeShortLE(out, 20, 1);         // PCM
        writeShortLE(out, 22, 1);         // mono
        writeIntLE(out, 24, sampleRate);
        writeIntLE(out, 28, sampleRate * 2);
        writeShortLE(out, 32, 2);         // block align
        writeShortLE(out, 34, 16);        // bits per sample
        writeStr(out, 36, "data");
        writeIntLE(out, 40, dataLen);
        for (int i = 0; i < samples; i++) {
            double env = 1.0 - (double) i / samples;           // linear decay
            double v = Math.sin(2 * Math.PI * freq * i / sampleRate) * env;
            int s = (int) (v * 30000);
            writeShortLE(out, 44 + i * 2, s);
        }
        return out;
    }

    private static void writeStr(byte[] b, int off, String s) {
        for (int i = 0; i < s.length(); i++) {
            b[off + i] = (byte) s.charAt(i);
        }
    }

    private static void writeIntLE(byte[] b, int off, int v) {
        b[off] = (byte) (v & 0xff);
        b[off + 1] = (byte) ((v >> 8) & 0xff);
        b[off + 2] = (byte) ((v >> 16) & 0xff);
        b[off + 3] = (byte) ((v >> 24) & 0xff);
    }

    private static void writeShortLE(byte[] b, int off, int v) {
        b[off] = (byte) (v & 0xff);
        b[off + 1] = (byte) ((v >> 8) & 0xff);
    }
}
