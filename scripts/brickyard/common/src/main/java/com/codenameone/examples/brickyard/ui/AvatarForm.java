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
package com.codenameone.examples.brickyard.ui;

import com.codename1.rbx.AvatarPreview;

import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.brickyard.api.AccountDto;
import com.codenameone.examples.brickyard.api.BadgeAwardDto;
import com.codenameone.examples.brickyard.api.ProfileDto;
import com.codenameone.examples.brickyard.api.ProgressDto;
import com.codenameone.examples.brickyard.net.Api;
import com.codenameone.examples.brickyard.net.Net;
import com.codenameone.examples.brickyard.net.Session;

import java.util.List;

/// The player's page: level and streak, the season pass, quests, the block
/// avatar editor, the display name and the badges earned so far.
public final class AvatarForm {
    private AvatarForm() {
    }

    public static void show() {
        final Form f = Ui.section("Me", "profile", 4);
        f.getToolbar().addMaterialCommandToRightBar("", com.codename1.ui.FontImage.MATERIAL_SETTINGS,
                e -> SettingsForm.show());
        Container content = Ui.content(f);
        final Avatar[] current = {Avatar.load()};

        // Level, streak and the season: filled when the server answers.
        final Container progress = new Container(BoxLayout.y());
        progress.setName("progression");
        content.add(progress);
        final Container quests = new Container(BoxLayout.y());
        quests.setName("quests");

        content.add(Ui.label("Your character", "RowTitle"));
        content.add(editor(current));

        content.add(Ui.label("Quests", "RowTitle"));
        content.add(quests);

        AccountDto me = Session.user();
        final TextField name = Ui.field("Display name", TextField.ANY, "displayName");
        name.setText(me == null || me.displayName() == null ? "" : me.displayName());
        Container nameRow = new Container(new BorderLayout());
        nameRow.add(BorderLayout.CENTER, name);
        nameRow.add(BorderLayout.EAST, Ui.secondary("Save", "saveNameButton",
                e -> Api.account().saveProfile(new ProfileDto(name.getText().trim(), null), Net.to(saved -> {
                    Session.setUser(saved);
                    Ui.toast("Saved.");
                }, Ui::failed))));
        content.add(Ui.label("Display name", "RowTitle"));
        content.add(nameRow);

        content.add(Ui.label("Badges", "RowTitle"));
        final Container badges = new Container(BoxLayout.y());
        badges.setName("myBadges");
        content.add(badges);
        f.show();
        loadProgress(f, progress, quests, current);
        Api.badges().mine(Net.to(list -> {
            fill(badges, list);
            f.revalidate();
        }, (status, message) -> { }));
    }

    /// The character the player plays as: the real body on a turntable, lit
    /// as the game lights it, above one chip per body. A chip saves at once,
    /// locally and to the account, and the play servers dress the player's
    /// character in it for everyone in the room.
    private static Container editor(final Avatar[] current) {
        final AvatarPreview preview = new AvatarPreview(current[0].body());
        preview.setName("avatarPreview");
        preview.setPreferredH(CN.convertToPixels(CN.isTablet() ? 70 : 52));
        Container chips = new Container(new GridLayout(1, Avatar.BODIES.length));
        final Button[] bodyButtons = new Button[Avatar.BODIES.length];
        for (int i = 0; i < Avatar.BODIES.length; i++) {
            final String body = Avatar.BODIES[i];
            bodyButtons[i] = Ui.button(Avatar.BODY_NAMES[i], body.equals(current[0].body()) ? "ChipOn" : "Chip",
                    "body_" + body, e -> {
                        current[0] = current[0].withBody(body);
                        current[0].save();
                        preview.setBody(body);
                        mark(bodyButtons, body);
                        Api.account().saveProfile(new ProfileDto(null, current[0].encode()),
                                Net.to(saved -> { }, Ui::failed));
                    });
            chips.add(bodyButtons[i]);
        }
        Container box = BoxLayout.encloseY(preview, chips);
        box.setUIID("Panel");
        // The server's choice wins: it follows the account to every device.
        Api.account().profile(Net.to(profile -> {
            if (profile != null && profile.avatar() != null && profile.avatar().length() > 0) {
                current[0] = Avatar.decode(profile.avatar());
                current[0].save();
                preview.setBody(current[0].body());
                mark(bodyButtons, current[0].body());
            }
        }, (status, message) -> { }));
        return box;
    }

    private static void mark(Button[] buttons, String body) {
        for (int j = 0; j < buttons.length; j++) {
            buttons[j].setUIID(Avatar.BODIES[j].equals(body) ? "ChipOn" : "Chip");
        }
        buttons[0].getParent().revalidate();
    }

    /// Asks for the level, quests and the season, and rebuilds those cards;
    /// a claim calls it again so the bars and the wallet move at once.
    static void loadProgress(final Form f, final Container progress, final Container quests,
            final Avatar[] current) {
        final Runnable again = () -> loadProgress(f, progress, quests, current);
        Api.progression().dailyLogin(Net.to(p -> {
            if (p == null) {
                return;
            }
            Api.progression().currentSeason(Net.to(season -> Api.progression().season(Net.to(tiers -> {
                progress.removeAll();
                progress.add(ProgressViews.hero(p, null));
                progress.add(ProgressViews.season(season, p, tiers, again));
                f.revalidate();
            }, (status, message) -> { })), (status, message) -> heroOnly(f, progress, p, current)));
        }, (status, message) -> { }));
        Api.progression().quests(Net.to(list -> {
            quests.removeAll();
            if (list == null || list.isEmpty()) {
                quests.add(Ui.label("No quests today. Check back tomorrow.", "Muted"));
            } else {
                for (com.codenameone.examples.brickyard.api.QuestDto q : list) {
                    quests.add(ProgressViews.quest(q, again));
                }
            }
            f.revalidate();
        }, (status, message) -> { }));
    }

    /// No season running: the level card alone.
    private static void heroOnly(Form f, Container progress, ProgressDto p, Avatar[] current) {
        progress.removeAll();
        progress.add(ProgressViews.hero(p, null));
        f.revalidate();
    }

    static void fill(Container badges, List<BadgeAwardDto> list) {
        badges.removeAll();
        if (list == null || list.isEmpty()) {
            badges.add(Ui.label("Play experiences to earn badges.", "Muted"));
            return;
        }
        for (BadgeAwardDto b : list) {
            String title = b.name() == null || b.name().length() == 0 ? b.badgeId() : b.name();
            String about = b.description() == null ? "" : b.description();
            badges.add(Ui.panel(Ui.label(title, "CardTitle"), Ui.label(about, "CardMeta")));
        }
    }
}
