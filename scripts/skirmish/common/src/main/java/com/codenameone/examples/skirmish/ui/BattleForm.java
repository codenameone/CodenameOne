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
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.util.UITimer;
import com.codenameone.examples.skirmish.battle.BattleView;
import com.codenameone.examples.skirmish.battle.Driver;
import com.codenameone.examples.skirmish.battle.MissionDriver;
import com.codenameone.examples.skirmish.battle.OnlineDriver;
import com.codenameone.examples.skirmish.battle.PracticeDriver;
import com.codenameone.examples.skirmish.battle.TutorialDriver;
import com.codenameone.examples.skirmish.engage.Telemetry;
import com.codenameone.examples.skirmish.net.Session;
import com.codenameone.examples.skirmish.rules.CardDef;
import com.codenameone.examples.skirmish.rules.Deck;
import com.codenameone.examples.skirmish.rules.Match;
import com.codenameone.examples.skirmish.rules.PlayerState;
import com.codenameone.examples.skirmish.rules.forge.DialogStep;
import java.util.List;

/// A battle: the arena ([BattleView], `com.codename1.gaming`) with ordinary
/// Codename One widgets around it -- the hand, the elixir meter, the clock and
/// the emotes. The arena is drawn by the GPU; the widgets are styled by
/// `theme.css`. Both are the same app, laid out by the same form.
///
/// The same screen plays practice, online and spectated battles; only the
/// [Driver] differs.
// tag::skirmish-battle-form[]
public final class BattleForm implements BattleView.Hud, OnlineDriver.Listener {
    private static final String[] EMOTES = {"GG", "Wow!", "Oops", "Thanks"};

    private final Form form;
    private final BattleView view;
    private final Driver driver;
    private final String mode;
    private final Button[] hand = new Button[Deck.HAND];
    private final String[] shown = new String[Deck.HAND];
    private final Label next = new Label("", "NextCard");
    private final ElixirBar elixir = new ElixirBar();
    private final Label elixirCount = new Label("5", "ElixirCount");
    private final Label clock = new Label("3:00", "BattleClock");
    private final Label myCrowns = new Label("0", "CrownsBlue");
    private final Label theirCrowns = new Label("0", "CrownsRed");
    private final Container overlay = new Container(new BorderLayout());
    private int cardsPlayed;
    private boolean over;
    private Tutorial coach;
    private MissionEnd missionEnd;

    /// What happens when a Forge mission is over or left: the caller scores it
    /// and decides where the player goes next.
    public interface MissionEnd {
        void finished(MissionDriver driver);

        void left(MissionDriver driver);
    }

    private BattleForm(Driver driver, String mode) {
        this.driver = driver;
        this.mode = mode;
        view = new BattleView(driver);
        view.setName("arena");
        view.setHud(this);
        form = new Form(new BorderLayout());
        form.setName("battle");
        form.setUIID("BattleForm");
        form.getToolbar().hideToolbar();
        form.add(BorderLayout.NORTH, top());
        overlay.setUIID("Container");
        Container center = new Container(new LayeredLayout());
        center.add(view).add(overlay);
        form.add(BorderLayout.CENTER, center);
        if (driver.interactive()) {
            form.add(BorderLayout.SOUTH, bottom());
        }
        Telemetry.screen("battle");
        Telemetry.battleStarted(mode);
    }

    /// Offline against the bot, with the player's saved deck when signed in.
    public static void practice() {
        String[] deck = Deck.starter();
        if (Session.user() != null && Session.user().deck() != null
                && Session.user().deck().size() == Deck.SIZE) {
            List<String> d = Session.user().deck();
            deck = d.toArray(new String[d.size()]);
        }
        new BattleForm(new PracticeDriver(System.currentTimeMillis(), deck), "practice").show();
    }

    /// The training battle. Not shown yet: the lesson attaches itself first.
    static BattleForm tutorial(TutorialDriver driver) {
        return new BattleForm(driver, "tutorial");
    }

    void setCoach(Tutorial coach) {
        this.coach = coach;
    }

    void begin() {
        show();
    }

    Form form() {
        return form;
    }

    BattleView view() {
        return view;
    }

    Component elixirBar() {
        return elixir;
    }

    /// The hand slot showing `card`, or null when it is not in the hand.
    Component slotOf(String card) {
        for (int i = 0; i < hand.length; i++) {
            if (hand[i] != null && card.equals(shown[i])) {
                return hand[i];
            }
        }
        return null;
    }

    /// Leaves the battle without a result.
    void abandon() {
        if (over) {
            return;
        }
        over = true;
        view.stop();
        driver.dispose();
        leaveTo();
    }

    /// A Forge mission, its story told over the battle.
    public static BattleForm mission(final MissionDriver driver, MissionEnd end) {
        final BattleForm f = new BattleForm(driver, "mission");
        f.missionEnd = end;
        driver.setListener(f::say);
        f.show();
        return f;
    }

    /// One line of a mission's story, in the mentor's bubble at the top of
    /// the field. A line that stops the clock waits for "Next".
    private void say(final DialogStep step) {
        overlay.removeAll();
        Container bubble = new Container(new BorderLayout());
        bubble.setUIID("CoachBubble");
        String speaker = step.speaker == null || "elda".equals(step.speaker) ? CardArt.MENTOR : step.speaker;
        bubble.add(BorderLayout.WEST, new Label(CardArt.face(speaker, CN.convertToPixels(11)), "Container"));
        SpanLabel text = new SpanLabel(step.text);
        text.setTextUIID("CoachText");
        bubble.add(BorderLayout.CENTER, text);
        if (step.freeze) {
            Button next = Ui.button("Next", "StoneButton", "mission-next", e -> {
                overlay.removeAll();
                overlay.revalidate();
                ((MissionDriver) driver).resume();
            });
            bubble.add(BorderLayout.SOUTH, FlowLayout.encloseRight(next));
        } else {
            UITimer.timer(4000, false, form, () -> {
                if (overlay.contains(bubble)) {
                    overlay.removeAll();
                    overlay.revalidate();
                }
            });
        }
        overlay.add(BorderLayout.NORTH, bubble);
        overlay.revalidate();
    }

    public static BattleForm online(OnlineDriver driver, boolean spectator) {
        BattleForm f = new BattleForm(driver, spectator ? "spectate" : "online");
        driver.setListener(f);
        f.show();
        return f;
    }

    private void show() {
        form.show();
        view.start();
    }

    private Container top() {
        Container names = new Container(new BorderLayout());
        names.setUIID("BattleTop");
        // Clear of the notch and the rounded corners; the arena below it
        // can run to the edges, but nothing a player reads or presses may.
        names.setSafeArea(true);
        Label them = new Label(driver.name(1 - driver.me()), "EnemyName");
        Label us = new Label(driver.name(driver.me()), "PlayerName");
        names.add(BorderLayout.WEST, BoxLayout.encloseY(them, us));
        clock.setName("clock");
        Button leave = Ui.button("", "BattleMenu", "leave", e -> confirmLeave());
        leave.setMaterialIcon(com.codename1.ui.FontImage.MATERIAL_CLOSE);
        names.add(BorderLayout.CENTER, FlowLayout.encloseCenterMiddle(clock));
        names.add(BorderLayout.EAST, BoxLayout.encloseX(BoxLayout.encloseY(theirCrowns, myCrowns), leave));
        return names;
    }

    private Container bottom() {
        Container cards = new Container(new GridLayout(1, Deck.HAND + 1));
        cards.setUIID("Hand");
        for (int i = 0; i < Deck.HAND; i++) {
            final int slot = i;
            Button b = new Button("", "CardSlot");
            b.setName("card" + i);
            b.setTextPosition(Component.BOTTOM);
            // Choosing on press, not on release, is what lets a drag start on
            // the card and end on the field.
            b.addPointerPressedListener(e -> choose(slot));
            hand[i] = b;
            cards.add(b);
        }
        cards.add(BoxLayout.encloseY(new Label("Next", "NextLabel"), next));
        Container meter = new Container(new BorderLayout());
        meter.setUIID("ElixirRow");
        meter.add(BorderLayout.WEST, elixirCount).add(BorderLayout.CENTER, elixir);
        Container south = BoxLayout.encloseY(meter, cards);
        if (driver instanceof OnlineDriver) {
            Container emotes = new Container(new GridLayout(1, EMOTES.length));
            for (final String id : EMOTES) {
                emotes.add(Ui.button(id, "EmoteButton", "emote-" + id,
                        e -> ((OnlineDriver) driver).emote(id)));
            }
            south.add(emotes);
        }
        south.setUIID("BattleBottom");
        // Above the home indicator, where a press would be taken for a swipe.
        south.setSafeArea(true);
        return south;
    }

    private void choose(int slot) {
        String card = shown[slot];
        if (card == null || over || (coach != null && !coach.mayChoose(card))) {
            return;
        }
        view.select(card);
        for (int i = 0; i < hand.length; i++) {
            hand[i].setUIID(i == slot ? "CardSlotSelected" : "CardSlot");
        }
        form.revalidate();
        if (coach != null) {
            coach.chose(card);
        }
    }
    // end::skirmish-battle-form[]

    @Override
    public void ticked(Match m) {
        PlayerState me = m.player(driver.me());
        int size = Math.min(CN.convertToPixels(13), CN.getDisplayWidth() / (Deck.HAND + 2));
        boolean changed = false;
        for (int i = 0; i < Deck.HAND && hand[0] != null; i++) {
            String card = me.hand(i);
            if (card != null && !card.equals(shown[i])) {
                shown[i] = card;
                CardDef def = driver.match().cards().get(card);
                Image icon = CardArt.card(card, size);
                hand[i].setIcon(icon);
                hand[i].setText("");
                hand[i].setUIID(card.equals(view.selected()) ? "CardSlotSelected" : "CardSlot");
                changed = true;
            }
            if (card != null) {
                boolean affordable = me.wholeElixir() >= driver.match().cards().get(card).cost;
                if (hand[i].isEnabled() != affordable) {
                    hand[i].setEnabled(affordable);
                    changed = true;
                }
            }
        }
        if (hand[0] != null && me.next() != null) {
            next.setIcon(CardArt.card(me.next(), CN.convertToPixels(7), false));
        }
        elixir.setElixir(me.elixir());
        elixirCount.setText(String.valueOf(me.wholeElixir()));
        int s = m.secondsLeft();
        String time = (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
        clock.setText(m.isOvertime() ? "OT " + time : time);
        clock.setUIID(m.isDoubleElixir() ? "BattleClockHot" : "BattleClock");
        myCrowns.setText(String.valueOf(m.player(driver.me()).crowns()));
        theirCrowns.setText(String.valueOf(m.player(1 - driver.me()).crowns()));
        if (changed) {
            form.revalidate();
        }
    }

    @Override
    public void placed(String card) {
        cardsPlayed++;
        Telemetry.cardPlayed(card);
        view.select(null);
        for (Button b : hand) {
            if (b != null) {
                b.setUIID("CardSlot");
            }
        }
        if (coach != null) {
            coach.placed(card);
        }
    }

    /// The simulation is over. Practice ends here; online the server's word
    /// on the result is awaited in [#ended(int, int, int, int, int, String)].
    @Override
    public void ended(Match m) {
        if (coach != null) {
            over = true;
            view.stop();
            Telemetry.battleEnded(mode, m.result() == driver.me() ? 1 : m.result() == Match.DRAW ? 0 : -1,
                    m.tick() / Match.TICKS_PER_SECOND, cardsPlayed);
            driver.dispose();
            coach.finished(m.result() == driver.me() ? 0 : 1);
            return;
        }
        if (driver instanceof MissionDriver) {
            over = true;
            Telemetry.battleEnded(mode, m.result() == driver.me() ? 1 : m.result() == Match.DRAW ? 0 : -1,
                    m.tick() / Match.TICKS_PER_SECOND, cardsPlayed);
            if (view.finaleRunning()) {
                UITimer.timer(1800, false, form, () -> {
                    view.stop();
                    missionEnd.finished((MissionDriver) driver);
                });
            } else {
                view.stop();
                missionEnd.finished((MissionDriver) driver);
            }
            return;
        }
        if (driver instanceof PracticeDriver) {
            over = true;
            PracticeDriver p = (PracticeDriver) driver;
            finish(m.result(), m.player(0).crowns(), m.player(1).crowns(), 0, 0, null, p.replay(), m.tick());
        }
    }

    @Override
    public void ended(final int result, final int crowns0, final int crowns1, final int trophies, final int coins,
            final String matchId) {
        over = true;
        if (view.finaleRunning()) {
            // Let the finishing camera sweep land before the result screen.
            com.codename1.ui.util.UITimer.timer(200, false, form,
                    () -> ended(result, crowns0, crowns1, trophies, coins, matchId));
            return;
        }
        finish(result, crowns0, crowns1, trophies, coins, matchId, null, driver.match().tick());
    }

    private void finish(int result, int c0, int c1, int trophies, int coins, String matchId, String replay,
            int ticks) {
        view.stop();
        int me = driver.me();
        int outcome = result == Match.DRAW ? 0 : result == me ? 1 : -1;
        Telemetry.battleEnded(mode, outcome, ticks / Match.TICKS_PER_SECOND, cardsPlayed);
        ResultForm.show(new ResultForm.Outcome(mode, outcome, me == 0 ? c0 : c1, me == 0 ? c1 : c0,
                trophies, coins, matchId, replay, driver.name(0), driver.name(1), me));
        driver.dispose();
    }

    @Override
    public void emote(int seat, String id) {
        Label bubble = new Label(id, seat == driver.me() ? "EmoteMine" : "EmoteTheirs");
        overlay.add(seat == driver.me() ? BorderLayout.SOUTH : BorderLayout.NORTH,
                FlowLayout.encloseCenter(bubble));
        overlay.revalidate();
        final Component holder = bubble.getParent();
        UITimer.timer(2000, false, form, () -> {
            overlay.removeComponent(holder);
            overlay.revalidate();
        });
    }

    @Override
    public void lost() {
        if (over) {
            return;
        }
        over = true;
        view.stop();
        driver.dispose();
        Dialog.show("Connection lost", "The connection to the battle was lost.", "OK", null);
        leaveTo();
    }

    private void confirmLeave() {
        if (over) {
            return;
        }
        if (coach != null) {
            if (Dialog.show("Leave training?", "You can replay it any time from \"How to play\".", "Leave",
                    "Stay")) {
                coach.stop();
                abandon();
            }
            return;
        }
        if (missionEnd != null) {
            if (Dialog.show("Leave the mission?", "You can try it again any time.", "Leave", "Stay")) {
                over = true;
                view.stop();
                driver.dispose();
                missionEnd.left((MissionDriver) driver);
            }
            return;
        }
        boolean sure = !(driver instanceof OnlineDriver) || !driver.interactive()
                || Dialog.show("Leave the battle?", "Leaving counts as a loss.", "Leave", "Stay");
        if (!sure) {
            return;
        }
        over = true;
        view.stop();
        if (driver instanceof OnlineDriver) {
            ((OnlineDriver) driver).leave();
        }
        driver.dispose();
        leaveTo();
    }

    private void leaveTo() {
        if (Session.user() == null) {
            SignInForm.show();
        } else if (!driver.interactive() && Session.user().admin()) {
            Nav.adminConsole();
        } else {
            Nav.home();
        }
    }
}
