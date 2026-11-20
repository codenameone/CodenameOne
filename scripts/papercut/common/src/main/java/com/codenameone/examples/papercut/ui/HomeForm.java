/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.ui;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codenameone.examples.papercut.play.Arena;
import com.codenameone.examples.papercut.play.ArenaView;
import com.codenameone.examples.papercut.play.Bot;

/// The first screen: a run the [Bot] plays on the page behind the menu, the
/// name, and the three ways to play.
public final class HomeForm extends Form {
    private final ArenaView attract;

    public HomeForm() {
        super(new BorderLayout());
        setScrollable(false);
        getToolbar().setVisible(false);
        attract = new ArenaView(new Arena(Arena.CLASSIC, System.currentTimeMillis()));
        attract.setAutopilot(new Bot(0.8f));
        attract.setHost(new ArenaView.Host() {
            @Override
            public void runOver(Arena arena) {
                attract.setArena(new Arena(Arena.CLASSIC, System.currentTimeMillis()));
            }

            @Override
            public void hudChanged() {
                // The menu shows no score.
            }
        });

        Container menu = new Container(BoxLayout.y(), "Card");
        menu.add(new Label("Papercut", "CardTitle"));
        menu.add(new Label("Cut, fold, survive.", "CardText"));
        menu.add(play("Classic", Arena.CLASSIC, "Button"));
        menu.add(play("Today's sheet", Arena.DAILY, "Secondary"));
        menu.add(play("Fold", Arena.FOLD, "Secondary"));

        Container front = new Container(new BorderLayout(BorderLayout.CENTER_BEHAVIOR_CENTER));
        front.add(BorderLayout.CENTER, menu);
        Container layers = new Container(new LayeredLayout());
        layers.add(attract).add(front);
        add(BorderLayout.CENTER, layers);
        addShowListener(e -> attract.start());
    }

    private Button play(String name, int mode, String uiid) {
        Button b = new Button(name, uiid);
        b.addActionListener(e -> {
            attract.stop();
            long seed = mode == Arena.DAILY ? dailySeed() : System.currentTimeMillis();
            new PlayForm(mode, seed, null).show();
        });
        return b;
    }

    /// The day number, which every player shares until the server says
    /// otherwise.
    static long dailySeed() {
        return System.currentTimeMillis() / 86400000L * 2654435761L + 17;
    }
}
