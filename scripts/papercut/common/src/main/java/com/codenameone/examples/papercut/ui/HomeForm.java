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
package com.codenameone.examples.papercut.ui;

import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codenameone.examples.papercut.api.PlayerDto;
import com.codenameone.examples.papercut.engage.Ads;
import com.codenameone.examples.papercut.net.Session;
import com.codenameone.examples.papercut.play.Arena;
import com.codenameone.examples.papercut.play.ArenaView;
import com.codenameone.examples.papercut.play.Bot;
import com.codenameone.examples.papercut.play.Hud;

/// The first screen: a run the [Bot] plays on the page behind the menu, the
/// name, the three ways to play, and -- for a player who signed in -- their
/// bests, their ink and the way to everything else.
public final class HomeForm extends Form {
    private final ArenaView attract;

    public HomeForm() {
        super(new BorderLayout());
        setName("home");
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

        PlayerDto me = Session.user();
        Container menu = new Container(BoxLayout.y(), "Card");
        menu.setName("menu");
        menu.add(new Label("Papercut", "CardTitle"));
        menu.add(new Label(me == null ? "Cut, fold, survive." : "Hello, " + me.displayName(), "CardText"));
        menu.add(play("Classic", Arena.CLASSIC, "Button", "playClassic"));
        menu.add(play("Today's sheet", Arena.DAILY, "Secondary", "playDaily"));
        menu.add(play("Fold", Arena.FOLD, "Secondary", "playFold"));
        if (me != null) {
            menu.add(new Label("Best " + Hud.format(me.bestClassic()) + "   ink " + me.inkCoins(), "Muted"));
        }

        Container tools = new Container(new GridLayout(1, 5), "Container");
        tools.add(tool(FontImage.MATERIAL_LEADERBOARD, "boards", e -> LeaderboardForm.show()));
        tools.add(tool(FontImage.MATERIAL_STOREFRONT, "shop", e -> ShopForm.show()));
        tools.add(tool(FontImage.MATERIAL_EMOJI_EVENTS, "achievements", e -> AchievementsForm.show()));
        tools.add(tool(FontImage.MATERIAL_GROUP, "friends", e -> FriendsForm.show()));
        tools.add(tool(FontImage.MATERIAL_SETTINGS, "settings", e -> SettingsForm.show()));
        menu.add(tools);
        if (me == null) {
            menu.add(Ui.button("Sign in for the boards", "Plain", "signIn", e -> SignInForm.show()));
        } else if (me.admin()) {
            menu.add(Ui.button("Console", "Plain", "console", e -> Nav.adminConsole()));
        }

        Container front = new Container(new BorderLayout(BorderLayout.CENTER_BEHAVIOR_CENTER));
        front.add(BorderLayout.CENTER, menu);
        Component banner = Ads.banner(Ui.adFree());
        if (banner != null) {
            front.add(BorderLayout.SOUTH, FlowLayout.encloseCenter(banner));
        }
        Container layers = new Container(new LayeredLayout());
        layers.add(attract).add(front);
        add(BorderLayout.CENTER, layers);
        addShowListener(e -> attract.start());
    }

    private Button play(String name, int mode, String uiid, String id) {
        Button b = new Button(name, uiid);
        b.setName(id);
        b.addActionListener(e -> {
            attract.stop();
            Runs.play(mode);
        });
        return b;
    }

    private Button tool(char icon, String name, com.codename1.ui.events.ActionListener<com.codename1.ui.events.ActionEvent> go) {
        Button b = new Button("", "HudButton");
        b.setName(name);
        FontImage.setMaterialIcon(b, icon, 4.5f);
        b.addActionListener(e -> {
            attract.stop();
            if (Session.user() == null && !"settings".equals(name)) {
                SignInForm.show();
            } else {
                go.actionPerformed(e);
            }
        });
        return b;
    }

    /// The day's seed for a guest, the same for everyone on the same UTC day.
    /// A signed-in player gets the server's, which an admin can override.
    static long dailySeed() {
        return System.currentTimeMillis() / 86400000L * 2654435761L + 17;
    }
}
