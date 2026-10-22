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

import com.codename1.components.InfiniteProgress;
import com.codename1.components.ShareButton;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.skirmish.api.ChallengeDto;
import com.codenameone.examples.skirmish.api.GameProtocol;
import com.codenameone.examples.skirmish.engage.Telemetry;
import com.codenameone.examples.skirmish.net.Api;
import com.codenameone.examples.skirmish.net.GameLink;
import com.codenameone.examples.skirmish.net.Net;
import com.codenameone.examples.skirmish.battle.OnlineDriver;
import java.util.Map;

/// Waiting for a battle: the ranked queue, a challenge link of one's own, a
/// challenge link someone sent, or -- for an administrator -- a match to
/// watch. All four open the same game socket and differ only in the first
/// thing they say on it.
// tag::skirmish-matchmaking[]
public final class MatchmakingForm implements GameLink.Listener {
    private final String hello;
    private final boolean spectator;
    private GameLink link;
    private OnlineDriver driver;
    private BattleForm battle;
    private boolean cancelled;

    private MatchmakingForm(String hello, boolean spectator) {
        this.hello = hello;
        this.spectator = spectator;
    }

    /// The ranked queue, matched by trophies; a bot fills in after a while.
    public static void queue() {
        new MatchmakingForm("{\"type\":\"" + GameProtocol.QUEUE + "\"}", false)
                .open("Finding an opponent", null);
    }

    /// A challenge of one's own: a code and a link to send to a friend.
    public static void host() {
        Api.game().challenge(Net.to(c -> Ui.later(() -> new MatchmakingForm(challenge(c.code()), false)
                .open("Waiting for your friend", c)), Ui::failed));
    }

    /// Someone else's challenge, opened from its link.
    public static void join(String code) {
        new MatchmakingForm(challenge(code), false).open("Joining the challenge", null);
    }

    public static void spectate(String matchId) {
        new MatchmakingForm("{\"type\":\"" + GameProtocol.SPECTATE + "\",\"match\":\"" + matchId + "\"}", true)
                .open("Connecting", null);
    }

    private static String challenge(String code) {
        return "{\"type\":\"" + GameProtocol.CHALLENGE + "\",\"code\":\"" + code + "\"}";
    }

    private void open(String title, ChallengeDto challenge) {
        Form f = Ui.form(title, "matchmaking");
        Ui.back(f, this::cancel);
        f.setLayout(BoxLayout.yCenter());
        f.add(Ui.centered(new InfiniteProgress()));
        if (challenge != null) {
            f.add(Ui.label("Challenge code", "SectionTitle"));
            f.add(Ui.centered(Ui.label(challenge.code(), "ChallengeCode")));
            f.add(Ui.text(challenge.url(), "ChallengeUrl"));
            ShareButton share = new ShareButton();
            share.setText("Send the link");
            share.setUIID("WoodButton");
            share.setName("shareChallenge");
            share.setTextToShare("Battle me in Skirmish: " + challenge.url());
            share.addActionListener(e -> Telemetry.challengeShared());
            f.add(share);
            f.add(Ui.stone("Copy the link", "copyChallenge", e -> {
                Display.getInstance().copyToClipboard(challenge.url());
                Ui.toast("Copied");
            }));
        }
        f.add(Ui.stone("Cancel", "cancel", e -> cancel()));
        f.show();
        link = GameLink.open(this);
    }

    private void cancel() {
        cancelled = true;
        if (link != null) {
            link.send("{\"type\":\"" + GameProtocol.CANCEL + "\"}");
            link.close();
        }
        Nav.home();
    }

    @Override
    public void message(Map<String, Object> m) {
        if (cancelled) {
            return;
        }
        String type = GameLink.text(m, "type");
        if (GameProtocol.READY.equals(type)) {
            link.send(hello);
        } else if (GameProtocol.START.equals(type) && driver == null) {
            driver = new OnlineDriver(link, m, spectator);
            battle = BattleForm.online(driver, spectator);
        } else if (GameProtocol.ERROR.equals(type) || GameProtocol.KICKED.equals(type)) {
            String why = GameProtocol.KICKED.equals(type) ? "You were removed from the game."
                    : GameLink.text(m, "message");
            link.close();
            if (driver != null) {
                driver.dispose();
            }
            Ui.toast(why);
            Nav.home();
        } else if (driver != null) {
            driver.message(m);
        }
    }
    // end::skirmish-matchmaking[]

    @Override
    public void closed(int code) {
        if (cancelled) {
            return;
        }
        if (battle != null) {
            battle.lost();
        } else {
            Ui.toast("Could not reach the game server.");
            Nav.home();
        }
    }
}
