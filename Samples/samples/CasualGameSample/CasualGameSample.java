package com.codename1.samples;

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
import java.util.Random;

/// Casual arena collector: pilot a little ship around a starfield with the on-screen
/// joystick, scoop up spinning gems and dodge the drifting asteroids. It shows off the
/// 2D side of `com.codename1.gaming` working together -- many `Sprite`s in a `Scene`,
/// per-sprite animation (twinkling stars, spinning gems, a pulsing thruster flame),
/// particle bursts, collision, and a `TouchControls` joystick that drives the same
/// `GameInput` a keyboard would. Every pixel of art is generated at runtime, so the
/// sample needs no assets.
public class CasualGameSample {
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
        Form f = new Form("Casual Game", new BorderLayout());
        CollectorView game = new CollectorView();
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
    static class CollectorView extends GameView {
        private static final float SPEED = 300f;     // pixels / second
        private static final int GEMS = 5;
        private static final int ASTEROIDS = 6;
        private static final int STARS = 70;

        private final Random rnd = new Random(7);
        private final Sprite player = new Sprite(makeShip(46));
        private final Sprite flame = new Sprite(makeFlame(34));
        private final List<Gem> gems = new ArrayList<Gem>();
        private final List<Asteroid> rocks = new ArrayList<Asteroid>();
        private final List<Particle> particles = new ArrayList<Particle>();
        private final Image gemImg = makeGem(30);
        private final Image[] sparks = {
            makeSpark(12, 0xffffe070), makeSpark(12, 0xff9ad0ff), makeSpark(12, 0xffff9ad0)
        };
        private float heading = -90f;   // pointing up
        private int score;
        private float hitFlash;
        private boolean ready;

        CollectorView() {
            setClearColor(0xff070b18);
            // thruster flame rides behind the ship
            flame.setAnchor(0.5, 0);
            flame.setZOrder(5);
            getScene().add(flame);
            player.setZOrder(6);
            getScene().add(player);
        }

        private void layoutWorld() {
            int w = getWidth();
            int h = getHeight();
            for (int i = 0; i < STARS; i++) {
                Star s = new Star(rnd.nextInt(w), rnd.nextInt(h), rnd);
                s.setZOrder(-20);
                getScene().add(s);
            }
            player.setPosition(w / 2f, h / 2f);
            for (int i = 0; i < GEMS; i++) {
                Gem g = new Gem(gemImg);
                placeAway(g, w, h, 60);
                gems.add(g);
                getScene().add(g);
            }
            for (int i = 0; i < ASTEROIDS; i++) {
                Asteroid a = new Asteroid(makeAsteroid(40 + rnd.nextInt(34), rnd), rnd, w, h);
                rocks.add(a);
                getScene().add(a);
            }
            // Anchor the stick to the bottom-left of the safe area; the framework
            // keeps it clear of notches/home indicators and follows rotation.
            getControls().addJoystick(88, TouchControls.LEFT, TouchControls.BOTTOM, 30);
            ready = true;
        }

        private void placeAway(Sprite s, int w, int h, int margin) {
            for (int tries = 0; tries < 20; tries++) {
                s.setPosition(margin + rnd.nextInt(Math.max(1, w - margin * 2)),
                        margin + rnd.nextInt(Math.max(1, h - margin * 2)));
                if (Math.hypot(s.getX() - player.getX(), s.getY() - player.getY()) > 120) {
                    return;
                }
            }
        }

        protected void update(double dt) {
            if (!ready) {
                if (getWidth() > 0 && getHeight() > 0) {
                    layoutWorld();
                }
                return;
            }
            int w = getWidth();
            int h = getHeight();

            float ax = getInput().getAxisX();
            float ay = getInput().getAxisY();
            if (getInput().isGameKeyDown(Display.GAME_LEFT)) {
                ax = -1;
            } else if (getInput().isGameKeyDown(Display.GAME_RIGHT)) {
                ax = 1;
            }
            if (getInput().isGameKeyDown(Display.GAME_UP)) {
                ay = -1;
            } else if (getInput().isGameKeyDown(Display.GAME_DOWN)) {
                ay = 1;
            }
            boolean moving = ax != 0 || ay != 0;
            player.setX(clamp(player.getX() + ax * SPEED * dt, 24, w - 24));
            player.setY(clamp(player.getY() + ay * SPEED * dt, 24, h - 24));
            if (moving) {
                heading = (float) Math.toDegrees(Math.atan2(ay, ax)) + 90;
            }
            player.setRotation(heading);

            // thruster flame: sit behind the ship, pulse while moving
            double rad = Math.toRadians(heading - 90);
            flame.setPosition(player.getX() - Math.cos(rad) * 22, player.getY() - Math.sin(rad) * 22);
            flame.setRotation(heading);
            float pulse = moving ? 0.85f + (float) Math.abs(Math.sin(score + flame.getX() * 0.05)) * 0.5f : 0f;
            flame.setScale(0.9f, pulse);
            flame.setAlpha(moving ? 255 : 0);

            // gems
            for (int i = 0; i < gems.size(); i++) {
                Gem g = gems.get(i);
                if (g.isVisible() && player.intersects(g)) {
                    g.setVisible(false);
                    score++;
                    burst(g.getX(), g.getY());
                    CN.callSerially(new Runnable() {
                        public void run() {
                            Form f = Display.getInstance().getCurrent();
                            if (f != null) {
                                f.setTitle("Casual Game - score " + score);
                            }
                        }
                    });
                    // respawn the gem somewhere fresh
                    placeAway(g, w, h, 50);
                    g.setVisible(true);
                }
            }

            // asteroids drift, wrap, and bump the ship
            for (int i = 0; i < rocks.size(); i++) {
                Asteroid a = rocks.get(i);
                if (player.intersects(a) && hitFlash <= 0) {
                    hitFlash = 0.4f;
                    burst(player.getX(), player.getY());
                    // knock the ship back toward centre
                    player.setPosition((player.getX() + w / 2f) / 2, (player.getY() + h / 2f) / 2);
                }
            }
            if (hitFlash > 0) {
                hitFlash -= (float) dt;
                setClearColor(hitFlash > 0 ? 0xff3a0d18 : 0xff070b18);
            }

            // particles
            for (int i = particles.size() - 1; i >= 0; i--) {
                Particle p = particles.get(i);
                if (p.dead) {
                    getScene().remove(p);
                    particles.remove(i);
                }
            }
        }

        private void burst(double x, double y) {
            for (int i = 0; i < 12; i++) {
                Particle p = new Particle(sparks[rnd.nextInt(sparks.length)], rnd);
                p.setPosition(x, y);
                p.setZOrder(7);
                particles.add(p);
                getScene().add(p);
            }
        }

        private static double clamp(double v, double lo, double hi) {
            return v < lo ? lo : (v > hi ? hi : v);
        }
    }

    /// A twinkling background star.
    static class Star extends Sprite {
        private final double speed;
        private double phase;
        Star(int x, int y, Random rnd) {
            super(dot(2 + rnd.nextInt(3), 0xffffffff));
            setPosition(x, y);
            speed = 1.5 + rnd.nextDouble() * 3;
            phase = rnd.nextDouble() * Math.PI * 2;
        }
        protected void onUpdate(double dt) {
            phase += speed * dt;
            setAlpha(110 + (int) (Math.abs(Math.sin(phase)) * 145));
        }
    }

    /// A slowly spinning gem.
    static class Gem extends Sprite {
        Gem(Image img) {
            super(img);
        }
        protected void onUpdate(double dt) {
            setRotation(getRotation() + (float) (90 * dt));
        }
    }

    /// A drifting, tumbling asteroid that wraps around the screen.
    static class Asteroid extends Sprite {
        private final double vx, vy, spin;
        private final int w, h;
        Asteroid(Image img, Random rnd, int w, int h) {
            super(img);
            this.w = w;
            this.h = h;
            setPosition(rnd.nextInt(w), rnd.nextInt(h));
            double a = rnd.nextDouble() * Math.PI * 2;
            double sp = 30 + rnd.nextInt(50);
            vx = Math.cos(a) * sp;
            vy = Math.sin(a) * sp;
            spin = (rnd.nextBoolean() ? 1 : -1) * (20 + rnd.nextInt(40));
            setZOrder(-5);
        }
        protected void onUpdate(double dt) {
            double nx = getX() + vx * dt;
            double ny = getY() + vy * dt;
            if (nx < -40) nx = w + 40; else if (nx > w + 40) nx = -40;
            if (ny < -40) ny = h + 40; else if (ny > h + 40) ny = -40;
            setPosition(nx, ny);
            setRotation(getRotation() + (float) (spin * dt));
        }
    }

    /// A short-lived spark that flies out, expands and fades.
    static class Particle extends Sprite {
        private final double vx, vy;
        private double life = 0.6;
        boolean dead;
        Particle(Image img, Random rnd) {
            super(img);
            double a = rnd.nextDouble() * Math.PI * 2;
            double sp = 80 + rnd.nextInt(160);
            vx = Math.cos(a) * sp;
            vy = Math.sin(a) * sp;
        }
        protected void onUpdate(double dt) {
            life -= dt;
            if (life <= 0) {
                dead = true;
                setVisible(false);
                return;
            }
            setPosition(getX() + vx * dt, getY() + vy * dt);
            float t = (float) (life / 0.6);
            setAlpha((int) (255 * t));
            setScale(1.6f - t);
        }
    }

    // ---- runtime art ---------------------------------------------------------

    static Image makeShip(int size) {
        Image img = Image.createImage(size, size, 0);
        Graphics g = img.getGraphics();
        g.setAntiAliased(true);
        // hull
        g.setColor(0xff4f9dff);
        int[] xs = {size / 2, size - 6, size / 2, 6};
        int[] ys = {2, size - 6, size - 14, size - 6};
        g.fillPolygon(xs, ys, 4);
        // cockpit
        g.setColor(0xffd6ecff);
        g.fillArc(size / 2 - 5, 10, 10, 12, 0, 360);
        // wing edge highlight
        g.setColor(0xff8ec5ff);
        int[] hx = {size / 2, size / 2, 10};
        int[] hy = {4, size - 16, size - 8};
        g.fillPolygon(hx, hy, 3);
        return img;
    }

    static Image makeFlame(int size) {
        Image img = Image.createImage(size, size, 0);
        Graphics g = img.getGraphics();
        g.setAntiAliased(true);
        g.setColor(0xffff7a18);
        int[] xs = {size / 2, size - 7, size / 2, 7};
        int[] ys = {0, size / 2, size - 1, size / 2};
        g.fillPolygon(xs, ys, 4);
        g.setColor(0xffffd23f);
        int[] ix = {size / 2, size - 12, size / 2, 12};
        int[] iy = {6, size / 2, size - 6, size / 2};
        g.fillPolygon(ix, iy, 4);
        return img;
    }

    static Image makeGem(int size) {
        Image img = Image.createImage(size, size, 0);
        Graphics g = img.getGraphics();
        g.setAntiAliased(true);
        g.setColor(0xff22d3a8);
        int[] xs = {size / 2, size - 2, size / 2, 2};
        int[] ys = {1, size / 2, size - 1, size / 2};
        g.fillPolygon(xs, ys, 4);
        g.setColor(0xff8af7df);   // top facet highlight
        int[] tx = {size / 2, size - 2, size / 2};
        int[] ty = {1, size / 2, size / 2};
        g.fillPolygon(tx, ty, 3);
        g.setColor(0xffffffff);
        g.fillArc(size / 2 - 2, size / 4, 4, 4, 0, 360);
        return img;
    }

    static Image makeAsteroid(int size, Random rnd) {
        Image img = Image.createImage(size, size, 0);
        Graphics g = img.getGraphics();
        g.setAntiAliased(true);
        int n = 9;
        int[] xs = new int[n];
        int[] ys = new int[n];
        double c = size / 2.0;
        for (int i = 0; i < n; i++) {
            double a = i * 2 * Math.PI / n;
            double r = c * (0.72 + rnd.nextDouble() * 0.28);
            xs[i] = (int) (c + Math.cos(a) * r);
            ys[i] = (int) (c + Math.sin(a) * r);
        }
        g.setColor(0xff6b7280);
        g.fillPolygon(xs, ys, n);
        // a couple of craters
        g.setColor(0xff4b5563);
        g.fillArc((int) (c - size * 0.18), (int) (c - size * 0.1), size / 5, size / 5, 0, 360);
        g.fillArc((int) (c + size * 0.05), (int) (c + size * 0.08), size / 6, size / 6, 0, 360);
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

    static Image dot(int size, int color) {
        Image img = Image.createImage(size, size, 0);
        Graphics g = img.getGraphics();
        g.setAntiAliased(true);
        g.setColor(color);
        g.fillArc(0, 0, size, size, 0, 360);
        return img;
    }
}
