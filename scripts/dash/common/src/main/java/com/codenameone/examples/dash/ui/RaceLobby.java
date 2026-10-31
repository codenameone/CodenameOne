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
package com.codenameone.examples.dash.ui;

import com.codename1.components.InfiniteProgress;
import com.codename1.components.ShareButton;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.components.SpanLabel;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.dash.api.GameProtocol;
import com.codenameone.examples.dash.engage.Telemetry;
import com.codenameone.examples.dash.net.Api;
import com.codenameone.examples.dash.net.GameLink;
import com.codenameone.examples.dash.net.Net;
import com.codenameone.examples.dash.net.Session;
import com.codenameone.examples.dash.rules.Content;
import com.codenameone.examples.dash.rules.Json;
import com.codenameone.examples.dash.run.Levels;
import com.codenameone.examples.dash.run.RaceDriver;
import com.codenameone.examples.dash.run.Voice;
import java.util.Map;

/// Getting into a live race.
///
/// - **Quick race**: whoever is waiting for the same level; bots fill the
///   empty places after a few seconds, so nobody waits long.
/// - **Party**: a code and a link for friends. Everyone who opens it lands
///   here, can talk (voice, if the device has it, and text), and the host
///   starts the race when the crew is in.
///
/// Every runner in a race runs the same level on the same seed: the track is
/// identical on every screen, and the race is decided by who runs it best.
// tag::dash-lobby[]
public final class RaceLobby implements GameLink.Listener, Voice.Listener {
    private final String hello;
    private final boolean spectator;
    private GameLink link;
    private RaceDriver driver;
    private boolean cancelled;
    private Form form;
    private ShareButton share;
    private final Container members = new Container(BoxLayout.y());
    private final Container chat = new Container(BoxLayout.y());
    private Container page;
    private final Label code = new Label("", "ChallengeCode");
    private final Label url = new Label("", "ChallengeUrl");
    private boolean host;
    private String level = Content.CAUSEWAY;
    private String[] names = new String[0];
    private String[] players = new String[0];
    private String[] talking = new String[0];
    private final java.util.Set<String> mutedPeers = new java.util.HashSet<String>();

    private RaceLobby(String hello, boolean spectator) {
        this.hello = hello;
        this.spectator = spectator;
    }

    /// A quick race on `level` (an official id or `pack/level`), or on the
    /// game's featured race level when null.
    public static void quick(String level) {
        String l = level == null ? Content.CAUSEWAY : level;
        RaceLobby r = new RaceLobby("{\"type\":\"" + GameProtocol.QUICK + "\",\"level\":" + Json.write(l) + "}",
                false);
        r.level = l;
        r.waiting("Finding runners");
    }

    /// A party of one's own, on `level`.
    public static void host(String level) {
        host(level, null);
    }

    /// A party for a crew: the server pushes the invite to the other members.
    public static void host(String level, String crew) {
        String l = level == null ? Content.CAUSEWAY : level;
        RaceLobby r = new RaceLobby("{\"type\":\"" + GameProtocol.HOST + "\",\"level\":" + Json.write(l)
                + (crew == null ? "" : ",\"crew\":" + Json.write(crew)) + "}", false);
        r.level = l;
        r.host = true;
        r.party("Your party");
    }

    /// Someone else's party, from its link or code.
    public static void join(String code) {
        new RaceLobby("{\"type\":\"" + GameProtocol.JOIN + "\",\"code\":" + Json.write(code) + "}", false)
                .party("Joining the party");
    }

    /// The admin console's spectator view of a live race.
    public static void spectate(String raceId) {
        new RaceLobby("{\"type\":\"" + GameProtocol.SPECTATE + "\",\"race\":" + Json.write(raceId) + "}", true)
                .waiting("Connecting");
    }

    /// A "join with a code" prompt, for a friend who was told the code.
    public static void enterCode() {
        final Form f = Ui.form("Join a party", "joinParty");
        Ui.back(f, Nav::home);
        final TextField field = new TextField("", "Party code", 12, TextField.ANY);
        field.setName("partyCode");
        f.add(field);
        f.add(Ui.primary("Join", "joinButton", e -> {
            if (field.getText().trim().length() > 0) {
                join(field.getText().trim().toUpperCase());
            }
        }));
        f.show();
    }

    private void waiting(String title) {
        form = Ui.form(title, "lobby");
        Ui.back(form, this::cancel);
        form.setLayout(BoxLayout.yCenter());
        form.add(Ui.centered(new InfiniteProgress()));
        form.add(Ui.secondary("Cancel", "cancel", e -> cancel()));
        form.show();
        link = GameLink.open(this);
    }

    private void party(String title) {
        form = Ui.form(title, "party");
        form.setLayout(new BorderLayout());
        Ui.back(form, this::cancel);
        Container top = Ui.column();
        top.add(Ui.label("Party code", "SectionTitle"));
        code.setName("partyCode");
        top.add(Ui.centered(code));
        top.add(url);
        share = new ShareButton();
        share.setText("Invite friends");
        share.setUIID("PrimaryButton");
        share.setName("shareParty");
        share.addActionListener(e -> Telemetry.challengeShared());
        // A material icon takes the button's own colour; the stock share
        // image is a picture and sits on a light square on a dark button.
        share.setMaterialIcon(FontImage.MATERIAL_SHARE);
        Button copy = Ui.secondary("Copy the link", "copyParty", e -> {
            Display.getInstance().copyToClipboard(url.getText());
            Ui.toast("Copied");
        });
        top.add(share);
        top.add(copy);
        // The code and the invite stay put; the runners and the chat scroll
        // under them, so the conversation is never squeezed into what the
        // code card leaves and scrolling to a new line never hides the code.
        Container below = Ui.column();
        below.add(Ui.label("Runners", "SectionTitle"));
        members.setName("members");
        below.add(members);
        chat.setName("chat");
        below.add(chat);
        page = BoxLayout.encloseY(Ui.centeredColumn(below));
        page.setScrollableY(true);
        form.add(BorderLayout.NORTH, Ui.centeredColumn(top));
        form.add(BorderLayout.CENTER, page);

        final TextField say = new TextField("", "Say something", 40, TextField.ANY);
        say.setName("chatField");
        say.setDoneListener(e -> {
            String t = say.getText().trim();
            if (t.length() > 0 && link != null) {
                link.send("{\"type\":\"" + GameProtocol.CHAT + "\",\"text\":" + Json.write(t) + "}");
                say.setText("");
            }
        });
        final Button mic = Ui.button("", "HudButton", "mic", null);
        mic.setMaterialIcon(FontImage.MATERIAL_MIC);
        mic.setEnabled(Voice.available());
        mic.addActionListener(e -> {
            Voice v = Voice.current();
            if (v != null) {
                v.setMuted(!v.isMuted());
                mic.setMaterialIcon(v.isMuted() ? FontImage.MATERIAL_MIC_OFF : FontImage.MATERIAL_MIC);
            }
        });
        Container bottom = new Container(new BorderLayout());
        bottom.setSafeArea(true);
        bottom.add(BorderLayout.CENTER, say);
        bottom.add(BorderLayout.EAST, mic);
        Container actions = Ui.row(Ui.secondary("Leave", "cancel", e -> cancel()));
        if (host) {
            actions.add(Ui.primary("Start the race", "goButton", e -> link.send("{\"type\":\"" + GameProtocol.GO
                    + "\",\"level\":" + Json.write(level) + "}")));
        }
        bottom.add(BorderLayout.SOUTH, actions);
        form.add(BorderLayout.SOUTH, bottom);
        form.show();
        link = GameLink.open(this);
    }
    // end::dash-lobby[]

    private void cancel() {
        cancelled = true;
        Voice.stop();
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
        } else if (GameProtocol.PARTY.equals(type)) {
            party(m);
        } else if (GameProtocol.START.equals(type) && driver == null) {
            start(m);
        } else if (GameProtocol.VOICE.equals(type)) {
            Voice v = Voice.current();
            if (v != null) {
                v.signal(GameLink.text(m, "from"), GameLink.text(m, "data"));
            }
        } else if (GameProtocol.MUTE.equals(type)) {
            Voice v = Voice.current();
            if (v != null) {
                v.mutedBy(GameLink.text(m, "from"), Boolean.TRUE.equals(m.get("on")));
            }
        } else if (GameProtocol.CHAT.equals(type) && driver == null) {
            // A bubble per message, the newest scrolled into view.
            Container bubble = BoxLayout.encloseY(new Label(GameLink.text(m, "from"), "ChatFrom"),
                    new SpanLabel(GameLink.text(m, "text"), "ChatLine"));
            bubble.setUIID("ChatBubble");
            chat.add(bubble);
            form.revalidate();
            page.scrollComponentToVisible(bubble);
        } else if (GameProtocol.ERROR.equals(type) || GameProtocol.KICKED.equals(type)) {
            String why = GameProtocol.KICKED.equals(type) ? "You were removed from the race."
                    : GameLink.text(m, "message");
            Voice.stop();
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

    private void party(Map<String, Object> m) {
        code.setText(GameLink.text(m, "code"));
        url.setText(GameLink.text(m, "url"));
        level = GameLink.text(m, "level");
        if (share != null) {
            share.setTextToShare("Race me in Dash: " + url.getText());
        }
        names = toStrings(m.get("names"));
        players = toStrings(m.get("players"));
        if (Voice.available() && Voice.current() == null && Session.user() != null) {
            Voice.start(link, Session.user().username()).setListener(this);
        }
        if (Voice.current() != null) {
            Voice.current().members(players);
        }
        drawMembers();
    }

    private void drawMembers() {
        members.removeAll();
        for (int i = 0; i < names.length; i++) {
            String u = i < players.length ? players[i] : "";
            boolean me = Session.user() != null && Session.user().username().equals(u);
            String state = me ? "" : contains(talking, u) ? (mutedPeers.contains(u) ? "  (muted)" : "  (voice)") : "";
            Label l = new Label(names[i] + (i == 0 ? "  - host" : "") + state, "LeaderName");
            l.setMaterialIcon(FontImage.MATERIAL_DIRECTIONS_RUN);
            members.add(l);
        }
        form.revalidateLater();
    }

    /// The race is on: its level must be on this device first, so a
    /// community level's pack is fetched before the run begins.
    private void start(final Map<String, Object> m) {
        final String ref = GameLink.text(m, "level");
        Levels.Playable p = Levels.resolve(ref);
        if (p != null) {
            begin(p, m);
            return;
        }
        String pack = Levels.packOf(ref);
        if (pack == null) {
            Ui.toast("This race's level is not in this version of the game.");
            cancel();
            return;
        }
        Api.forge().pack(pack, Net.to(dto -> Ui.later(() -> {
            Levels.remember(dto.content());
            Levels.Playable found = Levels.resolve(ref);
            if (found == null) {
                Ui.toast("The race's level changed while it started.");
                cancel();
            } else {
                begin(found, m);
            }
        }), (s, msg) -> Ui.later(this::cancel)));
    }

    private void begin(Levels.Playable p, Map<String, Object> m) {
        driver = new RaceDriver(link, p, m, spectator);
        if (Voice.current() != null) {
            Voice.current().members(toStrings(m.get("players")));
        }
        RunForm.race(driver, toStrings(m.get("runners")), () -> {
            Voice.stop();
            Nav.home();
        });
    }

    @Override
    public void closed(int closeCode) {
        if (cancelled) {
            return;
        }
        Voice.stop();
        if (driver == null) {
            Ui.toast("Could not reach the game server.");
            Nav.home();
        }
    }

    @Override
    public void peers(String[] connected) {
        talking = connected;
        if (driver == null && form != null) {
            drawMembers();
        }
    }

    @Override
    public void muted(String username, boolean on) {
        if (on) {
            mutedPeers.add(username);
        } else {
            mutedPeers.remove(username);
        }
        if (driver == null && form != null) {
            drawMembers();
        }
    }

    private static boolean contains(String[] a, String v) {
        for (String s : a) {
            if (s.equals(v)) {
                return true;
            }
        }
        return false;
    }

    private static String[] toStrings(Object list) {
        if (!(list instanceof java.util.List)) {
            return new String[0];
        }
        java.util.List<?> l = (java.util.List<?>) list;
        String[] out = new String[l.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = l.get(i) == null ? "" : l.get(i).toString();
        }
        return out;
    }
}
