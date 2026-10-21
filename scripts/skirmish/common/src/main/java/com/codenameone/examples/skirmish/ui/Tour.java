/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codenameone.examples.skirmish.ui;

import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Painter;
import com.codename1.ui.TextArea;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.skirmish.battle.BattleView;
import com.codenameone.examples.skirmish.net.Session;
import com.codenameone.examples.skirmish.rules.Arena;
import com.codenameone.examples.skirmish.rules.CardDef;
import com.codenameone.examples.skirmish.rules.Cards;
import com.codenameone.examples.skirmish.rules.Deck;
import com.codenameone.examples.skirmish.rules.Match;

// tag::skirmish-tour[]
/// The attract-mode walkthrough: the app driving itself through sign-in, the
/// home screen's rewards, the deck, the trophy room, the training battle and a
/// practice battle with its finishing camera sweep. It is what the sample's
/// promotional video is recorded from, so a recording needs nobody's hands --
/// open `skirmish://tour?server=http://host:port` and start the screen
/// recorder.
///
/// It drives the real screens the way a finger would: presses and releases
/// at a component's position, so every listener, transition and animation is
/// the one a player sees, and a ripple marks each touch for the viewer.
public final class Tour implements Runnable {
    static final String EMAIL = "player@skirmish.example";
    static final String PASSWORD = "skirmish-demo";

    private Tour() {
    }

    /// Starts the walkthrough on its own thread; it waits on the EDT the way a
    /// person would, so the EDT stays free to animate.
    public static void start() {
        Thread t = new Thread(new Tour(), "tour");
        t.start();
    }

    @Override
    public void run() {
        try {
            script();
        } catch (RuntimeException err) {
            com.codename1.io.Log.e(err);
        }
    }

    private void script() {
        // Sign in, typed out.
        waitForForm("signin");
        pause(1500);
        typeInto("email", EMAIL);
        typeInto("password", PASSWORD);
        pause(400);
        tap("signInButton");
        until(() -> Session.user() != null, 30000);
        waitForForm("home");
        pause(2500);

        // The rewards: a quest to claim and a chest to open.
        Component claim = firstNamed("claim-");
        if (claim != null) {
            scrollTo(claim);
            pause(900);
            tap(claim);
            collect();
        }
        if (has("chest0")) {
            scrollTo(find("chest0"));
            pause(900);
            tap("chest0");
            collect();
        }
        scrollTo(find("deckStrip"));
        pause(1800);

        // The collection and the trophy room.
        visit("deckButton", "deck", 3500);
        visit("achievementsButton", "achievements", 3500);

        // The training battle, followed the way a new player follows it.
        scrollTo(find("tutorialButton"));
        pause(600);
        tap("tutorialButton");
        waitForForm("battle");
        tutorial(arena());
        waitForForm("home");
        pause(1500);

        // A practice battle against the bot, ending on the camera sweep.
        scrollTo(find("practiceButton"));
        pause(500);
        tap("practiceButton");
        waitForForm("battle");
        battle(arena());
    }

    // ------------------------------------------------------------- scenes

    /// Follows the lesson step by step, doing whatever the mentor asks for.
    private void tutorial(final BattleView view) {
        String last = "";
        long lastAt = 0;
        long deadline = System.currentTimeMillis() + 240000;
        while (System.currentTimeMillis() < deadline) {
            String state = Tutorial.tourState();
            if (state.equals("free")) {
                break;
            }
            if (state.equals("next") && has("coachNext")) {
                // Long enough to read the bubble.
                pause(2800);
                tap("coachNext");
                pause(800);
            } else if (state.startsWith("choose:") && fresh(state, last, lastAt)) {
                lastAt = System.currentTimeMillis();
                pause(1500);
                String card = state.substring(7);
                int slot = slot(view, card);
                if (slot >= 0) {
                    tap("card" + slot);
                }
                pause(900);
            } else if (state.startsWith("place:") && fresh(state, last, lastAt)) {
                lastAt = System.currentTimeMillis();
                pause(1500);
                String[] parts = com.codename1.util.StringUtil.tokenize(state, ':').toArray(new String[0]);
                int x = Integer.parseInt(parts[2]);
                int y = Integer.parseInt(parts[3]);
                place(view, x, y);
                pause(900);
            } else {
                pause(200);
            }
            last = state;
        }
        // A short free fight, then the lesson's reward.
        play(view, 14000);
        onEdt(() -> view.previewFinale(0));
        pause(3800);
        onEdt(() -> {
            view.stop();
            Tutorial.finishForTour();
        });
        collect();
    }

    /// A step is acted on once, and again only if it has not moved on after
    /// a few seconds -- a touch that landed during a transition.
    private static boolean fresh(String state, String last, long lastAt) {
        return !state.equals(last) || System.currentTimeMillis() - lastAt > 5000;
    }

    private void battle(final BattleView view) {
        play(view, 30000);
        onEdt(() -> view.previewFinale(0));
        pause(4200);
        final Match m = view.driver().match();
        onEdt(() -> {
            view.stop();
            ResultForm.show(new ResultForm.Outcome("practice", 1, Math.max(1, m.player(0).crowns()), m.player(1).crowns(),
                    0, 0, null, null, "You", "Training Bot", 0));
        });
        waitForForm("result");
        pause(3500);
        com.codename1.io.Log.p("[tour] done");
    }

    /// Plays the hand for `millis`: whenever a card is affordable, it goes
    /// down by the bridge of the lane the enemy is weaker in.
    private void play(final BattleView view, long millis) {
        long end = System.currentTimeMillis() + millis;
        int lane = 0;
        while (System.currentTimeMillis() < end) {
            Match m = view.driver().match();
            if (m.isOver()) {
                return;
            }
            int slot = affordable(view);
            if (slot >= 0 && has("card" + slot)) {
                CardDef card = Cards.get(m.player(view.driver().me()).hand(slot));
                tap("card" + slot);
                pause(700);
                int x = lane % 2 == 0 ? Arena.LEFT_LANE_X : Arena.RIGHT_LANE_X;
                int y = card.kind == CardDef.SPELL ? Arena.mirrorY(Arena.PRINCESS_Y_BOTTOM) : 19 * Arena.TILE;
                place(view, x, y);
                lane++;
                pause(2200);
            } else {
                pause(300);
            }
        }
    }

    private int affordable(BattleView view) {
        Match m = view.driver().match();
        int me = view.driver().me();
        for (int i = 0; i < Deck.HAND; i++) {
            String id = m.player(me).hand(i);
            if (id != null && Cards.get(id).cost <= m.player(me).wholeElixir()) {
                return i;
            }
        }
        return -1;
    }

    private static int slot(BattleView view, String card) {
        for (int i = 0; i < Deck.HAND; i++) {
            if (card.equals(view.driver().match().player(0).hand(i))) {
                return i;
            }
        }
        return -1;
    }

    private void place(final BattleView view, int ax, int ay) {
        until(() -> view.screenPoint(ax, ay) != null, 5000);
        int[] p = view.screenPoint(ax, ay);
        touch(p[0], p[1]);
    }

    private BattleView arena() {
        until(() -> find("arena") instanceof BattleView, 20000);
        return (BattleView) find("arena");
    }

    private void visit(String button, String form, int stay) {
        scrollTo(find(button));
        pause(600);
        tap(button);
        waitForForm(form);
        pause(stay);
        onEdt(() -> {
            Form f = Display.getInstance().getCurrent();
            Command back = f.getBackCommand();
            Button b = f.getToolbar().findCommandComponent(back);
            if (b != null) {
                ripple(f, b.getAbsoluteX() + b.getWidth() / 2, b.getAbsoluteY() + b.getHeight() / 2);
            }
        });
        pause(250);
        onEdt(() -> {
            Form f = Display.getInstance().getCurrent();
            f.getBackCommand().actionPerformed(null);
        });
        waitForForm("home");
        pause(1200);
    }

    private void collect() {
        until(() -> has("collectReward"), 15000);
        pause(3200);
        tap("collectReward");
        pause(1500);
    }

    // ------------------------------------------------------------- fingers

    private void typeInto(String name, String text) {
        until(() -> find(name) instanceof TextArea, 20000);
        final TextArea field = (TextArea) find(name);
        Component c = find(name);
        onEdt(() -> ripple(Display.getInstance().getCurrent(),
                c.getAbsoluteX() + c.getWidth() / 2, c.getAbsoluteY() + c.getHeight() / 2));
        pause(300);
        for (int i = 1; i <= text.length(); i++) {
            final String part = text.substring(0, i);
            onEdt(() -> field.setText(part));
            pause(70);
        }
    }

    private void tap(String name) {
        until(() -> find(name) != null, 20000);
        tap(find(name));
    }

    private void tap(Component c) {
        touch(c.getAbsoluteX() + c.getWidth() / 2, c.getAbsoluteY() + c.getHeight() / 2);
    }

    /// A press and a release at a point, with the ripple drawn where it lands.
    private void touch(final int x, final int y) {
        onEdt(() -> {
            Form f = Display.getInstance().getCurrent();
            ripple(f, x, y);
            f.pointerPressed(x, y);
        });
        pause(120);
        onEdt(() -> Display.getInstance().getCurrent().pointerReleased(x, y));
    }

    private void scrollTo(final Component c) {
        if (c == null) {
            return;
        }
        onEdt(() -> {
            Container scroller = c.getParent();
            while (scroller != null && !scroller.isScrollableY()) {
                scroller = scroller.getParent();
            }
            if (scroller != null) {
                scroller.scrollComponentToVisible(c);
            }
        });
        pause(700);
    }

    /// A ring that grows and fades where a touch landed, painted over the
    /// form so it shows on any screen.
    private static void ripple(final Form f, final int x, final int y) {
        if (f == null) {
            return;
        }
        final long start = System.currentTimeMillis();
        final int max = CN.convertToPixels(9);
        f.setGlassPane(new Painter() {
            @Override
            public void paint(Graphics g, Rectangle rect) {
                long age = System.currentTimeMillis() - start;
                if (age > 600) {
                    return;
                }
                int r = (int) (max * (0.35f + 0.65f * age / 600f));
                int alpha = g.getAlpha();
                g.setAntiAliased(true);
                g.setColor(0xffffff);
                g.setAlpha((int) (170 * (1f - age / 600f)));
                g.fillArc(x - r, y - r, r * 2, r * 2, 0, 360);
                g.setAlpha((int) (255 * (1f - age / 600f)));
                g.setColor(0xffd34d);
                g.drawArc(x - r, y - r, r * 2, r * 2, 0, 360);
                g.setAlpha(alpha);
            }
        });
        for (int i = 1; i <= 8; i++) {
            com.codename1.ui.util.UITimer.timer(i * 80, false, f, f::repaint);
        }
    }

    // ------------------------------------------------------------- waiting

    private interface Check {
        boolean ok();
    }

    private static void until(Check check, long timeout) {
        long deadline = System.currentTimeMillis() + timeout;
        while (!check.ok()) {
            if (System.currentTimeMillis() > deadline) {
                throw new IllegalStateException("The tour gave up waiting");
            }
            pause(100);
        }
    }

    private static void waitForForm(final String name) {
        until(() -> {
            Form current = Display.getInstance().getCurrent();
            return current != null && name.equals(current.getName());
        }, 40000);
        pause(300);
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException err) {
            Thread.currentThread().interrupt();
        }
    }

    private static void onEdt(Runnable r) {
        CN.callSeriallyAndWait(r);
    }

    private static boolean has(String name) {
        return find(name) != null;
    }

    private static Component find(String name) {
        return search(name, false);
    }

    private static Component firstNamed(String prefix) {
        return search(prefix, true);
    }

    /// The form's own children are its content pane's, so the title area and
    /// the layered pane (where the mentor's bubble lives) are searched too.
    private static Component search(String name, boolean prefix) {
        Form f = Display.getInstance().getCurrent();
        if (f == null) {
            return null;
        }
        Container[] roots = {f.getContentPane(), f.getTitleArea(), f.getLayeredPane()};
        for (int i = 0; i < roots.length; i++) {
            Component found = roots[i] == null ? null : find(roots[i], name, prefix);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static Component find(Container c, String name, boolean prefix) {
        for (int i = 0; i < c.getComponentCount(); i++) {
            Component k = c.getComponentAt(i);
            String n = k.getName();
            if (n != null && (prefix ? n.startsWith(name) : n.equals(name))) {
                return k;
            }
            if (k instanceof Container) {
                Component found = find((Container) k, name, prefix);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
// end::skirmish-tour[]
