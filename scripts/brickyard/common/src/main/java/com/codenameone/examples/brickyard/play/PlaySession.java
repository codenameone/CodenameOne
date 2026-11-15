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
import com.codename1.rbx.RigidBodies;
import com.codename1.rbx.Scheduler;
import com.codename1.rbx.place.CompatReport;
import com.codename1.rbx.place.PlaceFile;
import com.codename1.rbx.place.PlaceLoader;
import com.codename1.ui.CN;
import com.codename1.ui.Dialog;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.brickyard.api.AccountDto;
import com.codenameone.examples.brickyard.engage.Friends;
import com.codenameone.examples.brickyard.net.Session;

/// Runs a published place on this device: loads the file into a solo engine
/// (server and client in one process), adds the signed-in player and shows
/// the 3D view. The menu leaves, invites, lists the players with their
/// leaderstats and shows what of the place this runtime does not support.
public final class PlaySession {
    private PlaySession() {
    }

    /// Starts playing `place`, the published place file of `experienceId`.
    public static void start(final String experienceId, final String title, byte[] place) {
        final Form previous = CN.getCurrentForm();
        final Engine engine = new Engine(Engine.SOLO);
        engine.setPhysics(new RigidBodies());
        final StringBuilder errors = new StringBuilder();
        engine.getScheduler().setOutput(new Scheduler.Output() {
            @Override
            public void message(String type, String text, Object owner) {
                if (type.startsWith("error") && errors.length() < 4000) {
                    errors.append(text).append('\n');
                }
            }
        });
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

        final PlayView view = new PlayView(engine);
        final Form f = new Form(title, new BorderLayout());
        f.setName("play");
        f.setScrollable(false);
        f.add(BorderLayout.CENTER, view);
        Friends.presence(title);
        final Runnable leave = new Runnable() {
            @Override
            public void run() {
                view.setPaused(true);
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
                Dialog.show("Compatibility", report.toString() + "\n\n" + errs, "OK", null);
            }
        });
        f.show();
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
