/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.skirmish.ui;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.util.UITimer;
import com.codenameone.examples.skirmish.battle.BattleView;
import com.codenameone.examples.skirmish.battle.TutorialDriver;
import com.codenameone.examples.skirmish.engage.Telemetry;
import com.codenameone.examples.skirmish.net.Api;
import com.codenameone.examples.skirmish.net.Net;
import com.codenameone.examples.skirmish.net.Session;
import com.codenameone.examples.skirmish.rules.Arena;
import com.codenameone.examples.skirmish.rules.Cards;

/// The training battle's lesson: a mentor in a speech bubble, and a finger
/// that shows exactly where to tap and where to drag.
///
/// The lesson is a list of steps. A step says something, points at something
/// (a card in the hand, the elixir bar, a spot on the field) and waits for
/// one thing: a tap on "Next", a card being picked, a card being placed, or
/// time passing. Time stands still while the mentor talks, so nothing happens
/// behind the player's back.
// tag::skirmish-tutorial[]
public final class Tutorial {
    private static final int TAP = 0;
    private static final int CHOOSE = 1;
    private static final int PLACE = 2;
    private static final int WAIT = 3;
    private static final int FREE = 4;

    /// One step of the lesson.
    private static final class Step {
        final int kind;
        final String card;
        final String text;
        final String hint;
        final int targetX;
        final int targetY;
        final int ticks;

        Step(int kind, String card, String text, String hint, int targetX, int targetY, int ticks) {
            this.kind = kind;
            this.card = card;
            this.text = text;
            this.hint = hint;
            this.targetX = targetX;
            this.targetY = targetY;
            this.ticks = ticks;
        }
    }

    private static final int NONE = -1;
    private static final int ELIXIR = -2;
    private static final int OWN_LEFT_Y = 21 * Arena.TILE;
    private static final int OWN_BACK_Y = 24 * Arena.TILE;
    private static final int ENEMY_TOWER_Y = Arena.CLASSIC.mirrorY(Arena.CLASSIC.princessY());

    private static final Step[] STEPS = {
        new Step(TAP, null, "Welcome to the arena, commander! I am Elda. Let me show you how battles are won.",
                "Tap Next. Skip any time, and replay it from \"How to play\".", NONE, NONE, 0),
        new Step(TAP, null, "Those red towers across the river are your targets. Every tower you topple "
                + "wins you a crown, and the most crowns wins the battle.", "Tap Next",
                Arena.CLASSIC.laneX(0), ENEMY_TOWER_Y, 0),
        new Step(TAP, null, "Cards cost elixir, this purple bar. It refills by itself, so spend it rather "
                + "than let it overflow.", "Tap Next", ELIXIR, 0, 0),
        new Step(CHOOSE, TutorialDriver.TROOP, "Let's attack! Tap the Squire card to pick it up.",
                "Tap the glowing card", NONE, NONE, 0),
        new Step(PLACE, TutorialDriver.TROOP, "Now drag it onto the field by the left bridge. You can only "
                + "deploy on your own side of the river.", "Drag to the circle and let go",
                Arena.CLASSIC.laneX(0), OWN_LEFT_Y, 0),
        new Step(WAIT, null, "Watch: troops march over the nearest bridge and attack the first thing in "
                + "their way.", "", NONE, NONE, 90),
        new Step(CHOOSE, TutorialDriver.RANGED, "Enemies! Defend with the Twin Archers. Pick them up.",
                "Tap the glowing card", NONE, NONE, 0),
        new Step(PLACE, TutorialDriver.RANGED, "Drop them behind your tower. Archers shoot from a distance, "
                + "while your towers shoot too.", "Drag to the circle and let go",
                Arena.CLASSIC.laneX(0), OWN_BACK_Y, 0),
        new Step(WAIT, null, "Good! Let the elixir build up while your army fights.", "", NONE, NONE, 120),
        new Step(CHOOSE, TutorialDriver.SPELL, "Spells can land anywhere on the field. Pick the Fire Orb.",
                "Tap the glowing card", NONE, NONE, 0),
        new Step(PLACE, TutorialDriver.SPELL, "Throw it at the red tower!", "Drag onto the tower and let go",
                Arena.CLASSIC.laneX(0), ENEMY_TOWER_Y, 0),
        new Step(TAP, null, "That's all there is to it: deploy troops, defend your towers, break theirs. "
                + "Topple the King's castle in the middle for an instant win. Now finish this battle!",
                "Tap Fight!", NONE, NONE, 0),
        new Step(FREE, null, null, null, NONE, NONE, 0),
    };

    private final TutorialDriver driver;
    private final BattleForm battle;
    private final Pointer pointer = new Pointer();
    /// The lesson's layer over the battle. The finger is painted by the
    /// layer itself, after the bubble, so it is always on top.
    private final Container layer = new Container(new BorderLayout()) {
        /// As big as the battle, so the bubble can sit at either end of it.
        @Override
        protected com.codename1.ui.geom.Dimension calcPreferredSize() {
            Form f = getComponentForm();
            return f == null ? super.calcPreferredSize()
                    : new com.codename1.ui.geom.Dimension(f.getWidth(), f.getHeight());
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            // The finger reaches the hand below the layer's own bounds.
            int[] clip = g.getClip();
            Form f = getComponentForm();
            if (f != null) {
                g.setClip(getX() - getAbsoluteX(), getY() - getAbsoluteY(), f.getWidth(), f.getHeight());
            }
            pointer.draw(g, this);
            g.setClip(clip);
        }
    };
    private final Container bubble = new Container(new BorderLayout());
    private final Container top = new Container(BoxLayout.y());
    private int step = -1;
    private int waitUntil;
    private UITimer timer;

    private Tutorial(TutorialDriver driver, BattleForm battle) {
        this.driver = driver;
        this.battle = battle;
    }

    /// The lesson on screen, for [Tour] to end its free fight early.
    private static Tutorial active;

    /// What the lesson waits for, read by [Tour]: "next", "free", "wait",
    /// or the card to pick ("choose:id") or place ("place:id:x:y").
    static String tourState() {
        Tutorial t = active;
        Step s = t == null ? null : t.current();
        if (s == null) {
            return "wait";
        }
        switch (s.kind) {
            case TAP:
                return "next";
            case FREE:
                return "free";
            case CHOOSE:
                return t.mayChoose(s.card) ? "choose:" + s.card : "wait";
            case PLACE:
                return "place:" + s.card + ":" + s.targetX + ":" + s.targetY;
            default:
                return "wait";
        }
    }

    /// Ends the lesson as a win: the walkthrough's shortcut past the free
    /// fight, which otherwise lasts until a king tower falls.
    static void finishForTour() {
        if (active != null) {
            active.finished(0);
            active = null;
        }
    }

    /// The training battle, from the home screen's "How to play" or on a new
    /// player's first visit.
    public static void start() {
        Telemetry.screen("tutorial");
        TutorialDriver d = new TutorialDriver();
        BattleForm f = BattleForm.tutorial(d);
        Tutorial t = new Tutorial(d, f);
        active = t;
        f.setCoach(t);
        t.attach(f.form());
        f.begin();
        t.next();
    }

    private void attach(Form f) {
        bubble.setUIID("CoachBubble");
        bubble.setName("coach");
        top.setSafeArea(true);
        top.add(bubble);
        layer.add(BorderLayout.NORTH, top);
        f.getLayeredPane().add(layer);
        timer = UITimer.timer(33, true, f, () -> {
            tick();
            layer.repaint();
        });
    }

    private void tick() {
        Step s = current();
        if (s == null) {
            return;
        }
        if (s.kind == WAIT && driver.match().tick() >= waitUntil) {
            next();
        }
    }

    private Step current() {
        return step >= 0 && step < STEPS.length ? STEPS[step] : null;
    }

    private void next() {
        step++;
        Step s = current();
        if (s == null) {
            return;
        }
        if (s.kind == CHOOSE && TutorialDriver.RANGED.equals(s.card)) {
            // The defence lesson needs something to defend against.
            driver.enemyAttack();
        }
        if (s.kind == FREE) {
            driver.unleash();
            layer.remove();
            pointer.target = null;
            battle.form().revalidate();
            return;
        }
        if (s.kind == WAIT) {
            waitUntil = driver.match().tick() + s.ticks;
        }
        // A card needs the elixir to pay for it. Time runs until it is there,
        // and the mentor says why.
        boolean afford = s.card == null
                || driver.match().player(0).wholeElixir() >= Cards.get(s.card).cost;
        driver.setPaused(s.kind != WAIT && afford);
        say(s, afford);
    }

    private void say(Step s, boolean afford) {
        bubble.removeAll();
        Label face = new Label(CardArt.face(CardArt.MENTOR, CN.convertToPixels(12)), "Container");
        bubble.add(BorderLayout.WEST, face);
        Container words = new Container(BoxLayout.y());
        words.add(new Label("Elda, your mentor", "CoachName"));
        SpanLabel text = new SpanLabel(afford ? s.text : "Wait for your elixir to fill up a little...");
        text.setTextUIID("CoachText");
        // Wrapped to the room the bubble really has: left to itself a span
        // label measures its height for a width it does not get.
        text.setPreferredW(CN.getDisplayWidth() - CN.convertToPixels(34));
        text.setName("coachText");
        words.add(text);
        if (s.hint != null && s.hint.length() > 0 && afford) {
            words.add(new Label(s.hint, "CoachHint"));
        }
        bubble.add(BorderLayout.CENTER, words);
        Container buttons = new Container(BoxLayout.y());
        if (s.kind == TAP) {
            Button next = Ui.button(step == STEPS.length - 2 ? "Fight!" : "Next", "ClaimButton", "coachNext",
                    e -> next());
            buttons.add(next);
        }
        Button skip = Ui.button("Skip", "TutorialSkip", "coachSkip", e -> skip());
        skip.getAllStyles().setFgColor(0x8a6a3a);
        buttons.add(skip);
        bubble.add(BorderLayout.EAST, buttons);
        pointer.target = s;
        pointer.started = System.currentTimeMillis();
        place(s);
        battle.form().revalidate();
        // The layered pane is not part of the content the form lays out.
        layer.getParent().revalidate();
        if (!afford) {
            waitForElixir(s);
        }
    }

    /// The bubble sits under the battle's top bar, unless what the finger
    /// points at is up there: then it moves down, above the hand.
    private void place(Step s) {
        boolean high = s.targetX >= 0 && s.card == null || s.kind == PLACE && s.targetY < Arena.CLASSIC.riverTop();
        Style st = top.getAllStyles();
        st.setPaddingUnit(Style.UNIT_TYPE_PIXELS);
        int formH = battle.form().getHeight();
        Component bar = battle.elixirBar();
        int below = bar.getAbsoluteY() > 0 ? formH - bar.getAbsoluteY() + CN.convertToPixels(2)
                : CN.convertToPixels(32);
        if (high) {
            st.setPaddingTop(0);
            st.setPaddingBottom(below);
            layer.removeComponent(top);
            layer.add(BorderLayout.SOUTH, top);
        } else {
            st.setPaddingTop(CN.convertToPixels(9));
            st.setPaddingBottom(0);
            layer.removeComponent(top);
            layer.add(BorderLayout.NORTH, top);
        }
    }

    /// Lets time run until the step's card can be paid for, then holds it
    /// again and explains the step.
    private void waitForElixir(final Step s) {
        final int at = step;
        UITimer.timer(200, true, battle.form(), new Runnable() {
            private boolean done;

            @Override
            public void run() {
                if (done || at != step) {
                    return;
                }
                if (driver.match().player(0).wholeElixir() >= Cards.get(s.card).cost) {
                    done = true;
                    driver.setPaused(true);
                    say(s, true);
                }
            }
        });
    }

    /// Whether the lesson lets the player pick `card` now.
    boolean mayChoose(String card) {
        Step s = current();
        if (s == null || s.kind == FREE) {
            return true;
        }
        return (s.kind == CHOOSE || s.kind == PLACE) && card.equals(s.card)
                && driver.match().player(0).wholeElixir() >= Cards.get(card).cost;
    }

    void chose(String card) {
        Step s = current();
        if (s != null && s.kind == CHOOSE && card.equals(s.card)) {
            next();
        }
    }

    void placed(String card) {
        Step s = current();
        if (s != null && s.kind == PLACE && card.equals(s.card)) {
            next();
        }
    }

    boolean active() {
        Step s = current();
        return s != null && s.kind != FREE;
    }

    /// No "are you sure": the bubble says the lesson can be replayed from
    /// "How to play", and a player who skips knows what they want.
    private void skip() {
        stop();
        battle.abandon();
    }

    void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    /// The battle is over. A signed-in player is paid for finishing it once.
    void finished(int result) {
        stop();
        final String title = result == 0 ? "Training complete!" : "Good try!";
        if (Session.user() == null) {
            Dialog.show(title, "Create an account to keep your progress and earn rewards.", "OK", null);
            SignInForm.show();
            return;
        }
        Api.progress().tutorialDone(Net.to(r -> Ui.later(() -> Rewards.show(r, () -> Rewards.refresh(Nav::home))),
                (s, m) -> Ui.later(Nav::home)));
    }

    /// The finger and the glow around what it points at. Painted by the
    /// layer rather than being a component of its own, so it never takes a
    /// touch: the card and the field under it must still get them.
    private final class Pointer {
        Step target;
        long started;
        private Image finger;
        private Image shadow;
        private Component on;

        void draw(Graphics g, Component host) {
            on = host;
            paint(g);
        }

        private int getX() {
            return on.getX();
        }

        private int getY() {
            return on.getY();
        }

        private int getAbsoluteX() {
            return on.getAbsoluteX();
        }

        private int getAbsoluteY() {
            return on.getAbsoluteY();
        }

        private void paint(Graphics g) {
            Step s = target;
            if (s == null || s.kind == WAIT) {
                return;
            }
            if (finger == null) {
                Style st = new Style();
                st.setFgColor(0xffffff);
                st.setBgTransparency(0);
                finger = FontImage.createMaterial(FontImage.MATERIAL_TOUCH_APP, st, 9);
                Style sh = new Style(st);
                sh.setFgColor(0x000000);
                shadow = FontImage.createMaterial(FontImage.MATERIAL_TOUCH_APP, sh, 9);
            }
            g.setAntiAliased(true);
            long now = System.currentTimeMillis() - started;
            int[] card = s.card == null ? null : center(battle.slotOf(s.card));
            int[] spot = spot(s);
            if (s.kind == PLACE && card != null && spot != null) {
                ring(g, spot[0], spot[1], now, 0xffd34d);
                // Press on the card, slide to the spot, let go, and again.
                float t = (now % 1800L) / 1800f;
                float k = t < 0.2f ? 0f : t > 0.8f ? 1f : (t - 0.2f) / 0.6f;
                k = k * k * (3f - 2f * k);
                int x = card[0] + Math.round((spot[0] - card[0]) * k);
                int y = card[1] + Math.round((spot[1] - card[1]) * k);
                if (t > 0.15f && t < 0.85f) {
                    Image ghost = CardArt.card(s.card, CN.convertToPixels(8), false);
                    g.setAlpha(150);
                    g.drawImage(ghost, x - getAbsoluteX() + getX() - ghost.getWidth() / 2,
                            y - getAbsoluteY() + getY() - ghost.getHeight() / 2);
                    g.setAlpha(255);
                }
                hand(g, x, y, t < 0.2f || t > 0.8f);
                return;
            }
            int[] at = card != null ? card : spot;
            if (at == null) {
                return;
            }
            ring(g, at[0], at[1], now, s.card != null ? 0x7be07b : 0xffd34d);
            int bob = (int) Math.round(Math.sin(now / 180.0) * CN.convertToPixels(1.5f));
            hand(g, at[0], at[1] + bob, false);
        }

        /// The finger with its tip on (x, y), in absolute coordinates.
        private void hand(Graphics g, int x, int y, boolean pressing) {
            int fx = x - getAbsoluteX() + getX() - finger.getWidth() * 2 / 5;
            int fy = y - getAbsoluteY() + getY() - finger.getHeight() / 10 + (pressing ? CN.convertToPixels(0.6f) : 0);
            g.setAlpha(90);
            g.drawImage(shadow, fx + 3, fy + 4);
            g.setAlpha(255);
            g.drawImage(finger, fx, fy);
        }

        private void ring(Graphics g, int x, int y, long now, int color) {
            int base = CN.convertToPixels(6);
            float p = (now % 1200L) / 1200f;
            int r = base + Math.round(base * 0.6f * p);
            g.setColor(color);
            g.setAlpha(Math.round(220 * (1f - p)));
            int cx = x - getAbsoluteX() + getX();
            int cy = y - getAbsoluteY() + getY();
            for (int w = 0; w < 3; w++) {
                g.drawArc(cx - r + w, cy - r + w, (r - w) * 2, (r - w) * 2, 0, 360);
            }
            g.setAlpha(60);
            g.fillArc(cx - base / 2, cy - base / 2, base, base, 0, 360);
            g.setAlpha(255);
        }

        private int[] spot(Step s) {
            if (s.targetX == ELIXIR) {
                return center(battle.elixirBar());
            }
            if (s.targetX == NONE) {
                return null;
            }
            BattleView v = battle.view();
            return v.screenPoint(s.targetX, s.targetY);
        }

        private int[] center(Component c) {
            if (c == null || c.getWidth() <= 0) {
                return null;
            }
            return new int[] {c.getAbsoluteX() + c.getWidth() / 2, c.getAbsoluteY() + c.getHeight() / 2};
        }
    }
}
// end::skirmish-tutorial[]
