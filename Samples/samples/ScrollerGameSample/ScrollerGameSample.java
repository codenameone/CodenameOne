package com.codename1.samples;

import com.codename1.gaming.AnimatedSprite;
import com.codename1.gaming.GameView;
import com.codename1.gaming.Sprite;
import com.codename1.gaming.TouchControls;
import com.codename1.io.Log;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import java.util.ArrayList;
import java.util.List;

/// Side-scroller hello-world: run left and right with the joystick (legs animate as
/// you move) and jump with the joystick up or the JUMP button to collect coins. It
/// shows a scrolling world -- the `Scene` camera follows the player so the level
/// slides past -- an `AnimatedSprite` walk cycle that flips to face the run
/// direction, a cheap parallax backdrop (a fixed sun, drifting clouds and
/// half-speed hills), a `TouchControls` joystick + action button, and a tiny
/// hand-rolled gravity/jump (no physics engine needed). Every pixel of art is
/// generated at runtime, so the sample needs no assets.
public class ScrollerGameSample {
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
        Form f = new Form("Scroller", new BorderLayout());
        RunnerView game = new RunnerView();
        f.add(BorderLayout.CENTER, game);
        f.show();
        game.start();
    }

    public void stop() {
        current = Display.getInstance().getCurrent();
    }

    public void destroy() {
    }

    /// The game surface.
    static class RunnerView extends GameView {
        private static final float RUN_SPEED = 300f;   // px/s
        private static final float GRAVITY = 2600f;     // px/s^2
        private static final float JUMP_SPEED = 1000f;  // px/s
        private static final int LEVEL_WIDTH = 5000;
        private static final int COINS = 16;
        private static final int CLOUDS = 5;
        private static final int HILLS = 6;

        private final Image[] runFrames = makeRunFrames(56);
        private final AnimatedSprite player = new AnimatedSprite(runFrames, 0.09);
        private final Image coinImg = makeCoin(30, 0xffffd54a);
        private final Image sunImg = makeSun(150);
        private final Image cloudImg = makeCloud(160, 70);
        private final Image hillImg = makeHill(520, 240);
        private final Sprite sun = new Sprite(sunImg);
        private final Sprite[] clouds = new Sprite[CLOUDS];
        private final Sprite[] hills = new Sprite[HILLS];
        private final Sprite[] coins = new Sprite[COINS];
        private final List<Sprite> tiles = new ArrayList<Sprite>();
        private final List<Particle> sparks = new ArrayList<Particle>();
        private final double[] cloudX = new double[CLOUDS];
        private final double[] cloudY = new double[CLOUDS];

        private float vy;
        private boolean onGround;
        private int groundY;
        private int facing = 1;
        private int jumpKey;
        private int upKey;
        private int score;
        private int laidOutW;
        private int laidOutH;
        private boolean ready;

        RunnerView() {
            setClearColor(0xff8ecae6);   // daytime sky
            sun.setZOrder(-100);
            getScene().add(sun);
            for (int i = 0; i < CLOUDS; i++) {
                clouds[i] = new Sprite(cloudImg);
                clouds[i].setZOrder(-90);
                clouds[i].setAlpha(235);
                getScene().add(clouds[i]);
            }
            for (int i = 0; i < HILLS; i++) {
                hills[i] = new Sprite(hillImg);
                hills[i].setAnchor(0, 1);   // anchor at bottom-left
                hills[i].setZOrder(-80);
                getScene().add(hills[i]);
            }
            for (int i = 0; i < COINS; i++) {
                coins[i] = new Sprite(coinImg);
                getScene().add(coins[i]);
            }
            player.setZOrder(10);
            player.play();
            getScene().add(player);
        }

        /// Builds the ground strip and lays the world out for the current view size.
        private void buildLevel() {
            jumpKey = Display.getInstance().getKeyCode(Display.GAME_FIRE);
            upKey = Display.getInstance().getKeyCode(Display.GAME_UP);
            Image tile = makeTile(128, 110, 0xff5a8f4e);
            for (int x = 0; x < LEVEL_WIDTH; x += 128) {
                Sprite t = new Sprite(tile);
                t.setAnchor(0, 0);
                t.setZOrder(-10);
                tiles.add(t);
                getScene().add(t);
            }
            for (int i = 0; i < CLOUDS; i++) {
                cloudX[i] = (i * 320) % LEVEL_WIDTH;
            }
            // anchored controls: stick bottom-left, JUMP bottom-right, inside safe area
            TouchControls c = getControls();
            c.addJoystick(80, TouchControls.LEFT, TouchControls.BOTTOM, 24);
            c.addButton(jumpKey, 55, TouchControls.RIGHT, TouchControls.BOTTOM, 30)
                    .setLabel("Jump").setColor(0xc0ff7043);
            relayoutLevel();
            ready = true;
        }

        /// Repositions everything that depends on the view size; called on first build
        /// and whenever the view resizes (e.g. rotation), so the ground never vanishes.
        private void relayoutLevel() {
            int w = getWidth();
            int h = getHeight();
            groundY = h - 90;
            for (int i = 0; i < tiles.size(); i++) {
                tiles.get(i).setPosition(i * 128, groundY);
            }
            for (int i = 0; i < HILLS; i++) {
                hills[i].setY(groundY + 10);
            }
            for (int i = 0; i < CLOUDS; i++) {
                cloudY[i] = 40 + (i * 53) % Math.max(1, groundY - 160);
            }
            for (int i = 0; i < COINS; i++) {
                coins[i].setPosition(360 + i * 280, groundY - 70 - (i % 3) * 70);
            }
            if (!ready) {
                player.setPosition(160, groundY - 30);
            } else if (onGround) {
                player.setY(groundY - 30);
            }
            sun.setPosition(0, h * 0.22);   // x set each frame (screen-fixed)
            laidOutW = w;
            laidOutH = h;
        }

        protected void update(double dt) {
            if (!ready) {
                if (getWidth() > 0 && getHeight() > 0) {
                    buildLevel();
                }
                return;
            }
            int w = getWidth();
            int h = getHeight();
            if (w != laidOutW || h != laidOutH) {
                relayoutLevel();   // rotation / resize
            }

            // run
            float ax = getInput().getAxisX();
            if (getInput().isGameKeyDown(Display.GAME_LEFT)) {
                ax = -1;
            } else if (getInput().isGameKeyDown(Display.GAME_RIGHT)) {
                ax = 1;
            }
            boolean moving = ax != 0;
            double nx = player.getX() + ax * RUN_SPEED * dt;
            player.setX(nx < 30 ? 30 : (nx > LEVEL_WIDTH - 30 ? LEVEL_WIDTH - 30 : nx));
            if (moving) {
                facing = ax < 0 ? -1 : 1;
                if (!player.isPlaying()) {
                    player.play();
                }
            } else {
                player.pause();
                player.setCurrentFrame(0);   // standing pose
            }
            player.setScale(facing, 1);

            // jump: the JUMP button OR pushing the stick / pressing up
            boolean jump = getInput().wasKeyPressed(jumpKey) || getInput().wasKeyPressed(upKey);
            if (jump && onGround) {
                vy = -JUMP_SPEED;
                onGround = false;
                player.play();
            }
            vy += GRAVITY * (float) dt;
            double ny = player.getY() + vy * dt;
            if (ny >= groundY - 30) {
                if (!onGround) {
                    puff(player.getX(), groundY - 6);   // landing dust
                }
                ny = groundY - 30;
                vy = 0;
                onGround = true;
            }
            player.setY(ny);
            // squash a touch while airborne for a livelier jump
            float air = onGround ? 1f : 0.88f;
            player.setScale(facing, air);

            // camera follows the player so the level scrolls past
            int cam = (int) (player.getX() - w * 0.32f);
            getScene().setCamera(cam, 0);

            updateBackdrop(cam, w, h, dt);

            // collect coins
            for (int i = 0; i < COINS; i++) {
                if (coins[i].isVisible() && player.intersects(coins[i])) {
                    coins[i].setVisible(false);
                    burst(coins[i].getX(), coins[i].getY());
                    score++;
                    showScore(score);
                }
            }

            // fade out spent sparks
            for (int i = sparks.size() - 1; i >= 0; i--) {
                if (sparks.get(i).dead) {
                    getScene().remove(sparks.get(i));
                    sparks.remove(i);
                }
            }
        }

        /// Parallax: the sun is pinned to the screen, clouds drift slowly, hills scroll
        /// at half the camera speed. Each layer counters the scene camera by a depth
        /// factor (0 = fixed to screen, 1 = moves with the world).
        private void updateBackdrop(int cam, int w, int h, double dt) {
            sun.setPosition(cam + w * 0.8, h * 0.2);
            for (int i = 0; i < CLOUDS; i++) {
                cloudX[i] -= (14 + i * 4) * dt;          // gentle drift
                double screenX = cloudX[i] - cam * 0.15; // light parallax
                double span = w + 220;
                screenX = ((screenX % span) + span) % span - 110;
                clouds[i].setPosition(cam + screenX, cloudY[i]);
            }
            double hillSpan = 480;
            for (int i = 0; i < HILLS; i++) {
                double screenX = i * hillSpan - cam * 0.5;
                double total = HILLS * hillSpan;
                screenX = ((screenX % total) + total) % total - hillSpan;
                hills[i].setX(cam + screenX);
            }
        }

        private void burst(double x, double y) {
            for (int i = 0; i < 9; i++) {
                Particle p = new Particle(makeSpark(10, 0xffffe070), i);
                p.setPosition(x, y);
                p.setZOrder(12);
                sparks.add(p);
                getScene().add(p);
            }
        }

        private void puff(double x, double y) {
            for (int i = 0; i < 6; i++) {
                Particle p = new Particle(makeSpark(12, 0x99ffffff), i + 3);
                p.setPosition(x - 16 + i * 6, y);
                p.setZOrder(9);
                sparks.add(p);
                getScene().add(p);
            }
        }

        private void showScore(final int s) {
            CN.callSerially(new Runnable() {
                public void run() {
                    Form f = Display.getInstance().getCurrent();
                    if (f != null) {
                        f.setTitle("Scroller - coins " + s + "/" + COINS);
                    }
                }
            });
        }
    }

    /// A short-lived spark that flies out, expands and fades.
    static class Particle extends Sprite {
        private final double vx, vy;
        private double life = 0.5;
        boolean dead;
        Particle(Image img, int seed) {
            super(img);
            double a = seed * 0.7;
            double sp = 90 + (seed % 5) * 40;
            vx = Math.cos(a) * sp;
            vy = Math.sin(a) * sp - 60;
        }
        protected void onUpdate(double dt) {
            life -= dt;
            if (life <= 0) {
                dead = true;
                setVisible(false);
                return;
            }
            setPosition(getX() + vx * dt, getY() + vy * dt);
            setAlpha((int) (255 * (life / 0.5)));
        }
    }

    // ---- runtime art ---------------------------------------------------------

    /// A little runner: head, body, swinging arms and legs. The leg/arm swing is
    /// driven by the frame index so the cycle reads as a run.
    static Image[] makeRunFrames(int size) {
        int n = 6;
        Image[] frames = new Image[n];
        for (int f = 0; f < n; f++) {
            Image img = Image.createImage(size, size, 0);
            Graphics g = img.getGraphics();
            g.setAntiAliased(true);
            double phase = f * 2 * Math.PI / n;
            float cx = size / 2f;
            float hipY = size * 0.62f;
            float shoulderY = size * 0.34f;
            float swing = (float) Math.sin(phase) * (size * 0.16f);
            // back limbs (darker) first
            g.setColor(0xffc1502f);
            limb(g, cx, hipY, cx - swing, size - 3, 7);          // back leg
            limb(g, cx, shoulderY, cx + swing * 0.8f, shoulderY + size * 0.22f, 6); // back arm
            // body
            g.setColor(0xffff7043);
            g.fillRoundRect((int) (cx - size * 0.16f), (int) (size * 0.30f),
                    (int) (size * 0.32f), (int) (size * 0.34f), 12, 12);
            // front limbs (brighter)
            g.setColor(0xffff8a5c);
            limb(g, cx, hipY, cx + swing, size - 3, 7);          // front leg
            limb(g, cx, shoulderY, cx - swing * 0.8f, shoulderY + size * 0.22f, 6); // front arm
            // head
            g.setColor(0xffffc4a0);
            g.fillArc((int) (cx - size * 0.13f), (int) (size * 0.10f),
                    (int) (size * 0.26f), (int) (size * 0.26f), 0, 360);
            g.setColor(0xff3a2a22);
            g.fillArc((int) (cx + size * 0.02f), (int) (size * 0.16f), 5, 5, 0, 360); // eye
            frames[f] = img;
        }
        return frames;
    }

    /// Draws a thick limb segment as a filled quad from (x1,y1) to (x2,y2).
    static void limb(Graphics g, float x1, float y1, float x2, float y2, float width) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.001f) {
            return;
        }
        float px = -dy / len * width / 2;
        float py = dx / len * width / 2;
        int[] xs = {(int) (x1 + px), (int) (x2 + px), (int) (x2 - px), (int) (x1 - px)};
        int[] ys = {(int) (y1 + py), (int) (y2 + py), (int) (y2 - py), (int) (y1 - py)};
        g.fillPolygon(xs, ys, 4);
    }

    static Image makeTile(int w, int h, int color) {
        Image img = Image.createImage(w, h, 0);
        Graphics g = img.getGraphics();
        g.setColor(0xff6b4a2f);   // soil
        g.fillRect(0, 0, w, h);
        g.setColor(color);        // grass top
        g.fillRect(0, 0, w, 16);
        g.setColor(0xff4a7a40);
        g.fillRect(0, 16, w, 4);
        return img;
    }

    static Image makeHill(int w, int h) {
        Image img = Image.createImage(w, h, 0);
        Graphics g = img.getGraphics();
        g.setAntiAliased(true);
        g.setColor(0xff77b06a);
        g.fillArc(0, h / 3, w, h * 2, 0, 180);
        g.setColor(0xff88c07a);
        g.fillArc(w / 6, h / 2, w * 2 / 3, h * 2, 0, 180);
        return img;
    }

    static Image makeSun(int size) {
        Image img = Image.createImage(size, size, 0);
        Graphics g = img.getGraphics();
        g.setAntiAliased(true);
        g.setColor(0x40fff2b0);   // soft glow
        g.fillArc(0, 0, size - 1, size - 1, 0, 360);
        g.setColor(0xffffe9a8);
        g.fillArc(size / 5, size / 5, size * 3 / 5, size * 3 / 5, 0, 360);
        return img;
    }

    static Image makeCloud(int w, int h) {
        Image img = Image.createImage(w, h, 0);
        Graphics g = img.getGraphics();
        g.setAntiAliased(true);
        g.setColor(0xffffffff);
        g.fillArc(0, h / 3, h, h, 0, 360);
        g.fillArc(w / 3, 0, h * 6 / 5, h * 6 / 5, 0, 360);
        g.fillArc(w - h, h / 4, h, h, 0, 360);
        g.fillRoundRect(h / 2, h / 2, w - h, h / 2, 20, 20);
        return img;
    }

    static Image makeCoin(int size, int color) {
        Image img = Image.createImage(size, size, 0);
        Graphics g = img.getGraphics();
        g.setAntiAliased(true);
        g.setColor(color);
        g.fillArc(0, 0, size - 1, size - 1, 0, 360);
        g.setColor(0xfffff2b0);
        g.fillArc(size / 4, size / 5, size / 3, size / 3, 0, 360);
        return img;
    }

    static Image makeSpark(int size, int color) {
        Image img = Image.createImage(size, size, 0);
        Graphics g = img.getGraphics();
        g.setAntiAliased(true);
        g.setColor(color);
        g.fillArc(0, 0, size - 1, size - 1, 0, 360);
        return img;
    }
}
