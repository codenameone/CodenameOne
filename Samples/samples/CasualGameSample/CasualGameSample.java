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
import java.util.Random;

/// Hello-world casual game: drive a little ship around the field with the on-screen
/// joystick and collect the coins. It shows the smallest interesting `GameView`: a
/// `Sprite` you move, more sprites you collide with, and a `TouchControls` joystick
/// that feeds the same `GameInput` a keyboard would. Everything is generated at
/// runtime, so the sample needs no assets.
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
        private static final float SPEED = 260f;   // pixels / second
        private final Random rnd = new Random(7);
        private final Sprite player = new Sprite(makeShip(40, 0xff4f9dff));
        private final Sprite coin = new Sprite(makeCoin(28, 0xffffd54a));
        private int score;
        private boolean ready;

        CollectorView() {
            setClearColor(0xff101826);
            getScene().add(coin);
            getScene().add(player);
        }

        protected void initComponent() {
            super.initComponent();
            // place things and add the joystick once the view has a size
        }

        private void layoutWorld() {
            player.setPosition(getWidth() / 2f, getHeight() / 2f);
            placeCoin();
            TouchControls c = getControls();
            float r = 90;
            c.addJoystick(r + 30, getHeight() - r - 30, r);
            ready = true;
        }

        private void placeCoin() {
            coin.setPosition(40 + rnd.nextInt(Math.max(1, getWidth() - 80)),
                    40 + rnd.nextInt(Math.max(1, getHeight() - 80)));
        }

        protected void update(double dt) {
            if (!ready) {
                if (getWidth() > 0 && getHeight() > 0) {
                    layoutWorld();
                }
                return;
            }
            // move the player by the joystick axes (works with arrow keys too)
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
            player.setX(clamp(player.getX() + ax * SPEED * dt, 20, getWidth() - 20));
            player.setY(clamp(player.getY() + ay * SPEED * dt, 20, getHeight() - 20));
            if (ax != 0 || ay != 0) {
                player.setRotation((float) Math.toDegrees(Math.atan2(ay, ax)) + 90);
            }

            if (player.intersects(coin)) {
                score++;
                placeCoin();
                final int s = score;
                CN.callSerially(new Runnable() {
                    public void run() {
                        Form f = Display.getInstance().getCurrent();
                        if (f != null) {
                            f.setTitle("Casual Game - score " + s);
                        }
                    }
                });
            }
        }

        private static double clamp(double v, double lo, double hi) {
            return v < lo ? lo : (v > hi ? hi : v);
        }
    }

    static Image makeShip(int size, int color) {
        Image img = Image.createImage(size, size, 0);
        Graphics g = img.getGraphics();
        g.setAntiAliased(true);
        g.setColor(color);
        int[] xs = {size / 2, size - 4, 4};
        int[] ys = {2, size - 4, size - 4};
        g.fillPolygon(xs, ys, 3);
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
}
