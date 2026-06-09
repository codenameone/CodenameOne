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

/// Hello-world side-scroller: run left and right with the joystick and tap the JUMP
/// button to hop over the field, collecting coins. It shows a scrolling world -- the
/// `Scene` camera follows the player so the level slides past -- plus a `TouchControls`
/// joystick and an action button, and a tiny hand-rolled gravity/jump (no physics
/// engine needed). Everything is generated at runtime.
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
        private static final float RUN_SPEED = 280f;   // px/s
        private static final float GRAVITY = 2200f;     // px/s^2
        private static final float JUMP_SPEED = 900f;   // px/s
        private static final int LEVEL_WIDTH = 4000;
        private static final int COINS = 14;

        private final Sprite player = new Sprite(makeHero(48, 0xffff7043));
        private final Sprite[] coins = new Sprite[COINS];
        private final Image coinImg = makeCoin(28, 0xffffd54a);
        private float vy;
        private boolean onGround;
        private int groundY;
        private int jumpKey;
        private int score;
        private boolean ready;

        RunnerView() {
            setClearColor(0xff0d1b2a);
            for (int i = 0; i < COINS; i++) {
                coins[i] = new Sprite(coinImg);
                getScene().add(coins[i]);
            }
            getScene().add(player);
        }

        private void layoutWorld() {
            groundY = getHeight() - 80;
            // a long ground strip built from tiles
            Image tile = makeTile(120, 80, 0xff2e4a3a);
            for (int x = 0; x < LEVEL_WIDTH; x += 120) {
                Sprite t = new Sprite(tile);
                t.setAnchor(0, 0);
                t.setPosition(x, groundY);
                t.setZOrder(-10);
                getScene().add(t);
            }
            player.setPosition(120, groundY - 24);
            for (int i = 0; i < COINS; i++) {
                coins[i].setPosition(300 + i * 250, groundY - 60 - (i % 3) * 70);
            }
            jumpKey = Display.getInstance().getKeyCode(Display.GAME_FIRE);
            TouchControls c = getControls();
            float r = 80;
            c.addJoystick(r + 24, getHeight() - r - 24, r);
            c.addButton(jumpKey, getWidth() - 100, getHeight() - 100, 55).setLabel("Jump").setColor(0xc0ff7043);
            ready = true;
        }

        protected void update(double dt) {
            if (!ready) {
                if (getWidth() > 0 && getHeight() > 0) {
                    layoutWorld();
                }
                return;
            }
            float ax = getInput().getAxisX();
            if (getInput().isGameKeyDown(Display.GAME_LEFT)) {
                ax = -1;
            } else if (getInput().isGameKeyDown(Display.GAME_RIGHT)) {
                ax = 1;
            }
            double nx = player.getX() + ax * RUN_SPEED * dt;
            player.setX(nx < 24 ? 24 : (nx > LEVEL_WIDTH - 24 ? LEVEL_WIDTH - 24 : nx));
            if (ax != 0) {
                player.setScale(ax < 0 ? -1 : 1, 1);   // face the run direction
            }

            // jump + gravity
            boolean jump = getInput().isKeyDown(jumpKey) || getInput().wasKeyPressed(jumpKey);
            if (jump && onGround) {
                vy = -JUMP_SPEED;
                onGround = false;
            }
            vy += GRAVITY * (float) dt;
            double ny = player.getY() + vy * dt;
            if (ny >= groundY - 24) {
                ny = groundY - 24;
                vy = 0;
                onGround = true;
            }
            player.setY(ny);

            // camera follows the player so the level scrolls past
            getScene().setCamera((int) (player.getX() - getWidth() * 0.3f), 0);

            // collect coins
            for (int i = 0; i < COINS; i++) {
                if (coins[i].isVisible() && player.intersects(coins[i])) {
                    coins[i].setVisible(false);
                    score++;
                    showScore(score);
                }
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

    static Image makeHero(int size, int color) {
        Image img = Image.createImage(size, size, 0);
        Graphics g = img.getGraphics();
        g.setAntiAliased(true);
        g.setColor(color);
        g.fillRoundRect(4, 2, size - 8, size - 4, 14, 14);
        g.setColor(0xffffffff);
        g.fillArc(size - 22, 8, 10, 10, 0, 360);   // eye
        return img;
    }

    static Image makeTile(int w, int h, int color) {
        Image img = Image.createImage(w, h, 0);
        Graphics g = img.getGraphics();
        g.setColor(color);
        g.fillRect(0, 0, w, h);
        g.setColor(0xff3c6b50);
        g.fillRect(0, 0, w, 8);
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
