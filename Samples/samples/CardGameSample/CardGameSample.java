package com.codename1.samples;

import com.codename1.gaming.GameView;
import com.codename1.gaming.Sprite;
import com.codename1.io.Log;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Toolbar;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import java.util.Random;

/// A 2D card game: Memory (Concentration). Tap a face-down card to flip it; match two
/// and they stay up, otherwise they flip back. It shows the flat-2D side of
/// `com.codename1.gaming` -- a `Sprite` per card laid out in a grid, a horizontal
/// flip animation (scale x through zero, swapping the image at the midpoint), tap
/// hit-testing via `GameInput`, and a little game state machine. All the card art is
/// generated at runtime, so the sample needs no assets.
public class CardGameSample {
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
        Form f = new Form("Memory", new BorderLayout());
        MemoryView game = new MemoryView();
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
    static class MemoryView extends GameView {
        private static final int COLS = 4;
        private static final int ROWS = 4;          // 16 cards -> 8 pairs
        private static final int PAIRS = COLS * ROWS / 2;
        private static final int[] SUIT_COLORS = {
            0xffe53935, 0xff1e88e5, 0xff43a047, 0xfffb8c00,
            0xff8e24aa, 0xff00acc1, 0xfff4511e, 0xff3949ab
        };

        private final Random rnd = new Random(11);
        private final Card[] cards = new Card[COLS * ROWS];
        private Image backImage;
        private int cardW, cardH;
        private Card firstUp;
        private Card secondUp;
        private double mismatchTimer;     // counts down before flipping a mismatch back
        private int moves;
        private int matched;
        private boolean ready;

        MemoryView() {
            setClearColor(0xff0b3d2e);     // felt green
            for (int i = 0; i < cards.length; i++) {
                cards[i] = new Card();
                getScene().add(cards[i].sprite);
            }
        }

        private void layout() {
            int w = getWidth();
            int h = getHeight();
            cardW = Math.min(w / (COLS + 1), 220);
            cardH = cardW * 7 / 5;
            backImage = makeBack(cardW, cardH);

            // a shuffled deck of pair ids
            int[] deck = new int[cards.length];
            for (int i = 0; i < cards.length; i++) {
                deck[i] = i / 2;
            }
            for (int i = deck.length - 1; i > 0; i--) {
                int j = rnd.nextInt(i + 1);
                int t = deck[i];
                deck[i] = deck[j];
                deck[j] = t;
            }

            float gapX = (w - COLS * cardW) / (float) (COLS + 1);
            float gapY = (h - ROWS * cardH) / (float) (ROWS + 1);
            for (int i = 0; i < cards.length; i++) {
                Card c = cards[i];
                c.pairId = deck[i];
                c.face = makeFace(cardW, cardH, c.pairId);
                c.state = Card.DOWN;
                c.flip = 0;
                int col = i % COLS;
                int row = i / COLS;
                c.sprite.setImage(backImage);
                c.sprite.setAnchor(0.5, 0.5);
                c.sprite.setPosition(gapX + col * (cardW + gapX) + cardW / 2f,
                        gapY + row * (cardH + gapY) + cardH / 2f);
            }
            ready = true;
        }

        protected void update(double dt) {
            if (!ready) {
                if (getWidth() > 0 && getHeight() > 0) {
                    layout();
                }
                return;
            }
            // advance flip animations
            for (int i = 0; i < cards.length; i++) {
                cards[i].animate(dt);
            }
            // a mismatched pair is shown briefly, then flipped back
            if (mismatchTimer > 0) {
                mismatchTimer -= dt;
                if (mismatchTimer <= 0) {
                    firstUp.flipDown();
                    secondUp.flipDown();
                    firstUp = null;
                    secondUp = null;
                }
                return;   // ignore taps while resolving a mismatch
            }
            handleTap();
        }

        private void handleTap() {
            if (!getInput().wasPointerPressed()) {
                return;
            }
            int px = getInput().getPointerX();
            int py = getInput().getPointerY();
            Card hit = cardAt(px, py);
            if (hit == null || hit.state != Card.DOWN || hit.isBusy()) {
                return;
            }
            hit.flipUp();
            if (firstUp == null) {
                firstUp = hit;
                return;
            }
            secondUp = hit;
            moves++;
            if (firstUp.pairId == secondUp.pairId) {
                firstUp.state = Card.MATCHED;
                secondUp.state = Card.MATCHED;
                firstUp = null;
                secondUp = null;
                matched++;
                title(matched >= PAIRS ? "You win in " + moves + " moves!" : "Memory - moves " + moves);
            } else {
                mismatchTimer = 0.8;   // show both, then flip back
                title("Memory - moves " + moves);
            }
        }

        private Card cardAt(int px, int py) {
            for (int i = 0; i < cards.length; i++) {
                Rectangle b = cards[i].sprite.getBounds();
                if (px >= b.getX() && px <= b.getX() + b.getWidth()
                        && py >= b.getY() && py <= b.getY() + b.getHeight()) {
                    return cards[i];
                }
            }
            return null;
        }

        private void title(final String s) {
            CN.callSerially(new Runnable() {
                public void run() {
                    Form f = Display.getInstance().getCurrent();
                    if (f != null) {
                        f.setTitle(s);
                    }
                }
            });
        }

        /// A single card: its sprite, which face it belongs to, and the flip state.
        class Card {
            static final int DOWN = 0;
            static final int UP = 1;
            static final int MATCHED = 2;
            final Sprite sprite = new Sprite();
            int pairId;
            Image face;
            int state = DOWN;
            // flip: 0 = fully showing current side, animates 0->1 then snaps and back
            double flip;
            int flipDir;          // +1 flipping up, -1 flipping down, 0 idle
            boolean showingFace;

            boolean isBusy() {
                return flipDir != 0;
            }

            void flipUp() {
                flipDir = 1;
                flip = 0;
            }

            void flipDown() {
                state = DOWN;
                flipDir = -1;
                flip = 0;
            }

            void animate(double dt) {
                if (state == MATCHED) {
                    // gentle pulse so matched pairs read as "done"
                    sprite.setImage(face);
                    return;
                }
                if (flipDir == 0) {
                    sprite.setScale(1f, 1f);
                    return;
                }
                flip += dt * 5;            // ~0.2s per half flip
                if (flip >= 1) {
                    // swap the visible side at the thin point and finish
                    showingFace = flipDir > 0;
                    sprite.setImage(showingFace ? face : backImage);
                    sprite.setScale(1f, 1f);
                    if (flipDir > 0) {
                        state = UP;
                    }
                    flipDir = 0;
                    return;
                }
                // squeeze horizontally; swap image as it passes the midpoint
                float sx = Math.abs(1f - 2f * (float) flip);
                if (flip >= 0.5 && !showingFace && flipDir > 0) {
                    showingFace = true;
                    sprite.setImage(face);
                } else if (flip >= 0.5 && showingFace && flipDir < 0) {
                    showingFace = false;
                    sprite.setImage(backImage);
                }
                sprite.setScale(Math.max(0.05f, sx), 1f);
            }
        }

        // ---- runtime art -----------------------------------------------------

        private static Image makeBack(int w, int h) {
            Image img = Image.createImage(w, h, 0);
            Graphics g = img.getGraphics();
            g.setAntiAliased(true);
            g.setColor(0xff37474f);
            g.fillRoundRect(0, 0, w - 1, h - 1, 18, 18);
            g.setColor(0xff546e7a);
            g.drawRoundRect(3, 3, w - 7, h - 7, 14, 14);
            // a simple diamond lattice
            g.setColor(0xff78909c);
            for (int y = h / 6; y < h; y += h / 6) {
                for (int x = w / 6; x < w; x += w / 6) {
                    g.fillArc(x - 3, y - 3, 6, 6, 0, 360);
                }
            }
            return img;
        }

        private Image makeFace(int w, int h, int pairId) {
            Image img = Image.createImage(w, h, 0);
            Graphics g = img.getGraphics();
            g.setAntiAliased(true);
            g.setAntiAliasedText(true);
            g.setColor(0xfffafafa);
            g.fillRoundRect(0, 0, w - 1, h - 1, 18, 18);
            int color = SUIT_COLORS[pairId % SUIT_COLORS.length];
            g.setColor(color);
            g.drawRoundRect(3, 3, w - 7, h - 7, 14, 14);
            // a big pip in the center + the pair's letter in the corners
            g.fillArc(w / 2 - w / 5, h / 2 - w / 5, w * 2 / 5, w * 2 / 5, 0, 360);
            Font f = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE);
            g.setFont(f);
            String label = String.valueOf((char) ('A' + pairId));
            g.drawString(label, 8, 4);
            g.drawString(label, w - 8 - f.stringWidth(label), h - 4 - f.getHeight());
            return img;
        }
    }
}
