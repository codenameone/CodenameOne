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

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.brickyard.api.AccountDto;
import com.codenameone.examples.brickyard.api.ExperienceDto;
import com.codenameone.examples.brickyard.net.Api;
import com.codenameone.examples.brickyard.net.Net;
import com.codenameone.examples.brickyard.net.Session;
import com.codenameone.examples.brickyard.studio.Studio;

import java.util.ArrayList;
import java.util.List;

/// Create: start a new experience, or open one of your own in the studio.
public final class CreateForm {
    // The server names roles in capitals (Roles.CREATOR); a lowercase name never
    // matched, and every creator was asked to become one.
    static final String CREATOR = "CREATOR";

    private CreateForm() {
    }

    public static void show() {
        final Form f = Ui.section("Create", "create", 1);
        final Container content = Ui.content(f);
        AccountDto me = Session.user();
        if (me != null && me.roles() != null && !me.roles().contains(CREATOR)) {
            pitch(content);
            f.show();
            return;
        }
        content.add(Ui.primary("New experience", "newExperienceButton", e -> Studio.open(null)));
        final Container totals = new Container(new GridLayout(1, 3));
        totals.setName("creatorTotals");
        content.add(totals);
        content.add(Ui.label("Your experiences", "RowTitle"));
        final Container list = new Container(BoxLayout.y());
        list.setName("myExperiences");
        content.add(list);
        f.show();
        // The server's list of the caller's own experiences, drafts included.
        Api.experiences().mine(Net.to(found -> {
            List<ExperienceDto> mine = found == null ? new ArrayList<ExperienceDto>() : found;
            long visits = 0;
            long likes = 0;
            int live = 0;
            for (ExperienceDto x : mine) {
                visits += x.visits();
                likes += x.favourites();
                live += x.publishedVersion() > 0 ? 1 : 0;
            }
            totals.add(stat(Long.toString(visits), "Visits"));
            totals.add(stat(Long.toString(likes), "Favourites"));
            totals.add(stat(live + " of " + mine.size(), "Published"));
            if (mine.isEmpty()) {
                list.add(Ui.text("Nothing yet. Start from a template: an obby, a tycoon, an arena, a "
                        + "simulator or a race, then make it yours.", "Muted"));
            }
            for (final ExperienceDto x : mine) {
                list.add(card(x));
            }
            f.revalidate();
        }, Ui::failed));
    }

    private static Component stat(String figure, String caption) {
        Container c = BoxLayout.encloseY(Ui.label(figure, "CreatorStat"), Ui.label(caption, "CreatorStatCaption"));
        c.setUIID("CreatorStatBox");
        return c;
    }

    /// One experience: its picture, where it stands, how it does, and the
    /// two places a creator goes from here.
    private static Component card(final ExperienceDto x) {
        boolean published = x.publishedVersion() > 0;
        Label status = Ui.label(published ? "Live, v" + x.publishedVersion() : "Draft",
                published ? "CreatorLive" : "CreatorDraft");
        long votes = x.upVotes() + x.downVotes();
        String rating = votes == 0 ? "New" : Math.round(100.0 * x.upVotes() / votes) + "% liked";
        // The name gets the whole line and the status sits under it, and the
        // buttons span the card below: side by side with the picture, a phone
        // cut all three down to their first letters.
        Container text = BoxLayout.encloseY(Ui.label(x.title(), "CardTitle"),
                FlowLayout.encloseIn(status),
                Ui.label(x.visits() + " visits  -  " + rating, "CardMeta"));
        text.setUIID("CreatorCardText");
        Container top = new Container(new BorderLayout());
        top.add(BorderLayout.WEST, new Thumbnail(x, CN.isTablet() ? 44 : 26));
        top.add(BorderLayout.CENTER, text);
        Container actions = new Container(new GridLayout(1, 2));
        com.codename1.ui.Button edit = Ui.secondary("Edit", "edit" + x.id(), e -> Studio.open(x.id()));
        edit.setAccessibilityText("Open in Studio");
        actions.add(edit);
        actions.add(Ui.button("Dashboard", "CreatorGhostButton", "dashboard" + x.id(), e -> DashboardForm.show(x)));
        Container card = BoxLayout.encloseY(top, actions);
        card.setUIID("Panel");
        return card;
    }

    /// Why a player turns creator tools on, ahead of the switch.
    private static void pitch(Container content) {
        Label art = new Label("", "CreatorHeroIcon");
        FontImage.setMaterialIcon(art, FontImage.MATERIAL_CONSTRUCTION, 14);
        content.add(art);
        content.add(Ui.label("Build the games you want to play", "CreatorHeroTitle"));
        content.add(Ui.text("Brickyard Studio is a full 3D editor in your pocket and on your desktop. "
                + "Publish to every player on the platform in one tap.", "CreatorHeroBody"));
        content.add(benefit(FontImage.MATERIAL_DASHBOARD_CUSTOMIZE, "Start from a template",
                "Obby, tycoon, arena, simulator and racing kits, ready to remix."));
        content.add(benefit(FontImage.MATERIAL_CODE, "Script it in Luau",
                "The scripting language and place files you already know, with live output."));
        content.add(benefit(FontImage.MATERIAL_INSIGHTS, "See who plays",
                "Visits, retention, funnels and ratings, live, for every version."));
        content.add(benefit(FontImage.MATERIAL_PAYMENTS, "Earn Bricks",
                "Sell game passes and items, and get paid for Plus members' play time."));
        content.add(Ui.primary("Become a creator", "becomeCreatorButton",
                e -> Api.account().becomeCreator(Net.to(updated -> {
                    Session.setUser(updated);
                    show();
                }, Ui::failed))));
    }

    private static Component benefit(char glyph, String title, String body) {
        Label icon = new Label("", "CreatorBenefitIcon");
        FontImage.setMaterialIcon(icon, glyph, 5);
        Container row = new Container(new BorderLayout());
        row.setUIID("CreatorBenefit");
        row.add(BorderLayout.WEST, icon);
        row.add(BorderLayout.CENTER, BoxLayout.encloseY(Ui.label(title, "CardTitle"), Ui.text(body, "CardMeta")));
        return row;
    }

    /// The experiences `owner` made, out of a list of anyone's.
    static List<ExperienceDto> mine(List<ExperienceDto> all, String owner) {
        List<ExperienceDto> out = new ArrayList<>();
        if (all == null || owner == null) {
            return out;
        }
        for (ExperienceDto e : all) {
            if (owner.equals(e.owner())) {
                out.add(e);
            }
        }
        return out;
    }
}
