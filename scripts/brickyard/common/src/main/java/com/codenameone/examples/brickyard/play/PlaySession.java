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
package com.codenameone.examples.brickyard.play;

import com.codename1.ui.Command;
import com.codename1.ui.Form;
import com.codename1.ui.Label;

import com.codename1.rbx.Engine;
import com.codename1.rbx.Instance;
import com.codename1.rbx.PlayView;
import com.codename1.rbx.ReplicaClient;
import com.codename1.rbx.ReplicaServer;
import com.codename1.rbx.RigidBodies;
import com.codename1.rbx.Scheduler;
import com.codename1.rbx.place.CompatReport;
import com.codename1.rbx.place.PlaceFile;
import com.codename1.rbx.place.PlaceLoader;
import com.codename1.io.WebSocket;
import com.codename1.ui.CN;
import com.codename1.ui.Dialog;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.brickyard.api.AccountDto;
import com.codenameone.examples.brickyard.api.JoinDto;
import com.codenameone.examples.brickyard.api.JoinTicketDto;
import com.codenameone.examples.brickyard.engage.Friends;
import com.codenameone.examples.brickyard.net.Api;
import com.codenameone.examples.brickyard.net.Net;
import com.codenameone.examples.brickyard.net.Session;

/// Runs a published place on this device: loads the file into a solo engine
/// (server and client in one process), adds the signed-in player and shows
/// the 3D view. The menu leaves, invites, lists the players with their
/// leaderstats and shows what of the place this runtime does not support.
public final class PlaySession {
    private PlaySession() {
    }

    /// Starts playing `place`, the published place file of `experienceId`, solo.
    public static void start(final String experienceId, final String title, byte[] place) {
        start(experienceId, title, place, 1, "");
    }

    /// Starts playing `experienceId`. With room for more than one player it
    /// joins a server online (the one `serverId` names, or any with a free
    /// seat when it is empty); when the server cannot be reached it plays
    /// `place` solo on this device instead.
    public static void start(final String experienceId, final String title, final byte[] place, int maxPlayers,
            String serverId) {
        if (maxPlayers <= 1 || !WebSocket.isSupported()) {
            solo(experienceId, title, place);
            return;
        }
        Api.experiences().join(experienceId, new JoinDto(serverId == null ? "" : serverId),
                Net.to(t -> online(experienceId, title, place, t),
                        (status, message) -> solo(experienceId, title, place)));
    }

    /// The game socket to open for `ticket`: the URL of the backend instance
    /// running the room when the server names one, else this API host's.
    static String socketUrl(JoinTicketDto ticket, String base) {
        String at = ticket.url();
        if (at == null || at.length() == 0) {
            at = "ws" + base.substring(4) + "/ws/play";
        }
        return at + (at.indexOf('?') < 0 ? "?" : "&") + "ticket=" + ticket.ticket();
    }

    /// The online path: a CLIENT engine kept in step with the server by a
    /// [ReplicaClient] over the game socket. Socket messages are delivered
    /// on the EDT, where PlayView steps the engine, and a UI timer ticks the
    /// replica (sending input, acknowledging state) after the frames.
    private static void online(final String experienceId, final String title, final byte[] place,
            JoinTicketDto ticket) {
        final Engine engine = new Engine(Engine.CLIENT);
        // The client simulates its own character (Characters.simulatedHere),
        // so movement answers at once; the server checks and corrects it.
        engine.setPhysics(new RigidBodies());
        final StringBuilder errors = errorSink(engine);
        final WebSocket[] socket = new WebSocket[1];
        final boolean[] state = new boolean[2]; // [0] shown, [1] over
        final ReplicaClient client = new ReplicaClient(engine, new ReplicaServer.Link() {
            @Override
            public void send(byte[] message) {
                if (socket[0] != null && !state[1]) {
                    socket[0].send(message);
                }
            }
        });
        final Runnable lost = new Runnable() {
            @Override
            public void run() {
                if (state[1]) {
                    return;
                }
                state[1] = true;
                if (!state[0]) {
                    solo(experienceId, title, place);
                } else {
                    Dialog.show(title, "The connection to the server was lost.", "OK", null);
                }
            }
        };
        String url = socketUrl(ticket, Session.serverUrl());
        socket[0] = WebSocket.build(url)
                .onBinaryMessage((ws, m) -> CN.callSerially(() -> {
                    if (state[1]) {
                        return;
                    }
                    client.receive(m);
                    if (!state[0] && client.isJoined()) {
                        state[0] = true;
                        // Ticking right after each engine step sends the
                        // move the step just produced, once per frame.
                        show(engine, experienceId, title, null, errors, () -> {
                            state[1] = true;
                            socket[0].close();
                        }, () -> {
                            if (!state[1]) {
                                client.tick();
                            }
                        });
                    }
                }))
                .onClose((ws, code, reason) -> CN.callSerially(lost))
                .onError((ws, err) -> CN.callSerially(lost))
                .connect(10000);
    }

    private static StringBuilder errorSink(Engine engine) {
        final StringBuilder errors = new StringBuilder();
        engine.getScheduler().setOutput(new Scheduler.Output() {
            @Override
            public void message(String type, String text, Object owner) {
                if (type.startsWith("error") && errors.length() < 4000) {
                    errors.append(text).append('\n');
                }
            }
        });
        return errors;
    }

    /// The solo path: server and client in one engine on this device.
    private static void solo(final String experienceId, final String title, byte[] place) {
        final Engine engine = new Engine(Engine.SOLO);
        engine.setPhysics(new RigidBodies());
        final StringBuilder errors = errorSink(engine);
        final CompatReport report;
        try {
            report = PlaceLoader.loadPlace(engine, PlaceFile.read(place));
        } catch (RuntimeException ex) {
            Dialog.show("Can't open " + title, String.valueOf(ex.getMessage()), "OK", null);
            return;
        }
        AccountDto me = Session.user();
        String name = me == null ? "Guest" : me.username();
        String display = me == null ? "Guest" : me.displayName();
        engine.addPlayer(Math.abs((long) name.hashCode()) + 1, name, display);
        show(engine, experienceId, title, report, errors, null, null);
    }

    /// Shows `engine` in the 3D view with the play menu; `report` is null
    /// online, where the place was loaded on the server. `onLeave` runs when
    /// the player leaves; `afterStep` (may be null) runs after every engine step.
    private static Form show(final Engine engine, final String experienceId, final String title,
            final CompatReport report, final StringBuilder errors, final Runnable onLeave,
            Runnable afterStep) {
        final Form previous = CN.getCurrentForm();
        final PlayView view = new PlayView(engine);
        view.setAfterStep(afterStep);
        final Form f = new Form(title, new BorderLayout());
        f.setName("play");
        f.setScrollable(false);
        f.add(BorderLayout.CENTER, view);
        Friends.presence(title);
        final Runnable leave = new Runnable() {
            @Override
            public void run() {
                view.setPaused(true);
                if (onLeave != null) {
                    onLeave.run();
                }
                Friends.presence(null);
                if (previous != null) {
                    previous.showBack();
                }
            }
        };
        f.getToolbar().setBackCommand(new Command("Leave") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                leave.run();
            }
        });
        f.getToolbar().addCommandToOverflowMenu(new Command("Players") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                showPlayers(engine);
            }
        });
        f.getToolbar().addCommandToOverflowMenu(new Command("Invite a friend") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                Friends.invite(experienceId, title);
            }
        });
        f.getToolbar().addCommandToOverflowMenu(new Command("Compatibility") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                String errs = errors.length() == 0 ? "No script errors." : errors.toString();
                String rep = report == null ? "Running on the server." : report.toString();
                Dialog.show("Compatibility", rep + "\n\n" + errs, "OK", null);
            }
        });
        f.show();
        return f;
    }

    private static void showPlayers(Engine engine) {
        Form d = new Form("Players", BoxLayout.y());
        final Form back = CN.getCurrentForm();
        for (Instance p : engine.service("Players").getChildren()) {
            StringBuilder line = new StringBuilder(p.getName());
            Instance ls = p.findFirstChild("leaderstats");
            if (ls != null) {
                for (Instance v : ls.getChildren()) {
                    line.append("   ").append(v.getName()).append(' ').append(String.valueOf(v.prop("Value")));
                }
            }
            d.add(new Label(line.toString()));
        }
        d.getToolbar().setBackCommand(new Command("Back") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                back.showBack();
            }
        });
        d.show();
    }
}
