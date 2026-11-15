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

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.plaf.Border;
import com.codenameone.examples.brickyard.api.AccountDto;
import com.codenameone.examples.brickyard.api.BadgeAwardDto;
import com.codenameone.examples.brickyard.api.ProfileDto;
import com.codenameone.examples.brickyard.net.Api;
import com.codenameone.examples.brickyard.net.Net;
import com.codenameone.examples.brickyard.net.Session;

import java.util.List;

/// The player's profile: the block avatar editor, the display name, and the
/// badges earned so far.
public final class AvatarForm {
    private AvatarForm() {
    }

    public static void show() {
        final Form f = Ui.section("Me", "profile", 4);
        f.getToolbar().addMaterialCommandToRightBar("", com.codename1.ui.FontImage.MATERIAL_SETTINGS,
                e -> SettingsForm.show());
        Container content = Ui.content(f);
        final Avatar[] current = {Avatar.load()};
        final AvatarView preview = new AvatarView(current[0], false);
        content.add(BorderLayout.centerCenter(preview));
        final int[] part = {Avatar.HEAD};
        Container parts = new Container(new GridLayout(2, 3));
        final Button[] partButtons = new Button[Avatar.PARTS.length];
        for (int i = 0; i < Avatar.PARTS.length; i++) {
            final int index = i;
            partButtons[i] = Ui.button(Avatar.PARTS[i], i == 0 ? "Chip" : "LinkButton", "part" + i, e -> {
                part[0] = index;
                for (int j = 0; j < partButtons.length; j++) {
                    partButtons[j].setUIID(j == index ? "Chip" : "LinkButton");
                }
                f.revalidate();
            });
            parts.add(partButtons[i]);
        }
        content.add(parts);
        Container swatches = new Container(new GridLayout(2, Avatar.PALETTE.length / 2));
        for (int i = 0; i < Avatar.PALETTE.length; i++) {
            final int color = Avatar.PALETTE[i];
            Button b = new Button(" ", "Chip");
            b.setName("swatch" + i);
            b.getAllStyles().setBgColor(color);
            b.getAllStyles().setBgTransparency(255);
            b.getAllStyles().setBorder(Border.createLineBorder(1, 0x10131f));
            b.addActionListener(e -> {
                current[0] = current[0].with(part[0], color);
                current[0].save();
                preview.setAvatar(current[0]);
                Api.account().saveProfile(new ProfileDto(null, current[0].encode()),
                        Net.to(saved -> { }, Ui::failed));
            });
            swatches.add(b);
        }
        content.add(swatches);

        AccountDto me = Session.user();
        final TextField name = Ui.field("Display name", TextField.ANY, "displayName");
        name.setText(me == null || me.displayName() == null ? "" : me.displayName());
        content.add(Ui.panel(name, Ui.secondary("Save name", "saveNameButton",
                e -> Api.account().saveProfile(new ProfileDto(name.getText().trim(), null), Net.to(saved -> {
                    Session.setUser(saved);
                    Ui.toast("Saved.");
                }, Ui::failed)))));

        content.add(Ui.label("Badges", "RowTitle"));
        final Container badges = new Container(BoxLayout.y());
        badges.setName("myBadges");
        content.add(badges);
        f.show();
        // The server's colours win: they follow the account to every device.
        Api.account().profile(Net.to(profile -> {
            if (profile != null && profile.avatar() != null && profile.avatar().length() > 0) {
                current[0] = Avatar.decode(profile.avatar());
                current[0].save();
                preview.setAvatar(current[0]);
            }
        }, (status, message) -> { }));
        Api.badges().mine(Net.to(list -> {
            fill(badges, list);
            f.revalidate();
        }, (status, message) -> { }));
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
