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
package com.codenameone.examples.skirmish.forge;

import com.codename1.components.MultiButton;
import com.codename1.components.ShareButton;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codenameone.examples.skirmish.AppConfig;
import com.codenameone.examples.skirmish.api.ForkDto;
import com.codenameone.examples.skirmish.api.MissionStarsDto;
import com.codenameone.examples.skirmish.api.PackDto;
import com.codenameone.examples.skirmish.api.PackReportDto;
import com.codenameone.examples.skirmish.api.PackSummaryDto;
import com.codenameone.examples.skirmish.api.RateDto;
import com.codenameone.examples.skirmish.net.Api;
import com.codenameone.examples.skirmish.net.Net;
import com.codenameone.examples.skirmish.net.Session;
import com.codenameone.examples.skirmish.rules.forge.CampaignDef;
import com.codenameone.examples.skirmish.rules.forge.ContentPack;
import com.codenameone.examples.skirmish.rules.forge.MissionDef;
import com.codenameone.examples.skirmish.rules.forge.PackCodec;
import com.codenameone.examples.skirmish.ui.Ui;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// A published pack: its campaigns, the missions in order with the stars
/// already won, and what a player can do with it -- play, rate, remix, share.
public final class PackScreen {
    private PackScreen() {
    }

    public static void show(final String id) {
        Api.forge().pack(id, Net.to(dto -> Api.forge().stars(id, Net.to(stars -> Ui.later(() -> show(dto, stars)),
                Ui::failed)), Ui::failed));
    }

    private static void show(final PackDto dto, List<MissionStarsDto> stars) {
        final PackSummaryDto s = dto.summary();
        final ContentPack pack;
        try {
            pack = PackCodec.read(dto.content());
        } catch (IllegalArgumentException e) {
            Ui.failed(0, "That pack could not be read: " + e.getMessage());
            return;
        }
        final Map<String, Integer> won = new HashMap<String, Integer>();
        for (MissionStarsDto m : stars) {
            won.put(m.missionId(), Integer.valueOf(m.stars()));
        }
        Form f = Ui.form(s.name(), "pack");
        Ui.back(f, () -> ForgeHome.show(s.official() ? "official" : "popular"));
        f.add(Ui.label("by " + s.author() + (s.official() ? "   Official" : ""), "ForgeByline"));
        if (s.description().length() > 0) {
            f.add(Ui.text(s.description(), "ForgeHelpText"));
        }
        if (s.pendingAssets()) {
            f.add(Ui.text("Some of this pack's files are waiting for a moderator. Friends with the link can play "
                    + "it now; it is listed for everyone once they are approved.", "ForgeHelpText"));
        }
        boolean[] listed = new boolean[pack.missions.size()];
        for (CampaignDef c : pack.campaigns) {
            f.add(Ui.label(c.name, "SectionTitle"));
            if (c.intro.length() > 0) {
                f.add(Ui.text(c.intro, "ForgeHelpText"));
            }
            int[] got = new int[c.missions.size()];
            for (int i = 0; i < got.length; i++) {
                Integer w = won.get(c.missions.get(i));
                got[i] = w == null ? 0 : w.intValue();
            }
            for (int i = 0; i < c.missions.size(); i++) {
                MissionDef m = pack.mission(c.missions.get(i));
                if (m == null) {
                    continue;
                }
                listed[pack.missions.indexOf(m)] = true;
                f.add(mission(pack, m, got[i], c.isOpen(i, got), dto));
            }
            if (got.length > 0 && got[got.length - 1] > 0 && c.outro.length() > 0) {
                f.add(Ui.text(c.outro, "ForgeHelpText"));
            }
        }
        boolean header = false;
        for (int i = 0; i < pack.missions.size(); i++) {
            if (!listed[i]) {
                if (!header) {
                    f.add(Ui.label(pack.campaigns.isEmpty() ? "Missions" : "More missions", "SectionTitle"));
                    header = true;
                }
                MissionDef m = pack.missions.get(i);
                Integer w = won.get(m.id);
                f.add(mission(pack, m, w == null ? 0 : w.intValue(), true, dto));
            }
        }
        f.add(Ui.label("Share", "SectionTitle"));
        ShareButton share = new ShareButton();
        share.setText("Send to a friend");
        share.setUIID("WoodButton");
        // The share icon ShareButton brings is drawn on white; a material
        // icon takes the button's own colours.
        share.setMaterialIcon(com.codename1.ui.FontImage.MATERIAL_SHARE);
        share.setName("sharePack");
        share.setTextToShare("Play \"" + s.name() + "\" in Skirmish: " + AppConfig.packLink(s.id()));
        f.add(share);
        f.add(Ui.row(Ui.stone("Copy the link", "copyPack", e -> {
            Display.getInstance().copyToClipboard(AppConfig.packLink(s.id()));
            Ui.toast("Copied");
        }), Ui.stone("Remix", "remixPack", e -> remix(s))));
        String me = Session.user() == null ? "" : Session.user().username();
        if (me.equals(s.owner())) {
            f.add(Ui.stone("Edit", "editPack", e -> PackEditor.open(s.id())));
        } else {
            f.add(Ui.row(Ui.stone("Rate", "ratePack", e -> rate(s)),
                    Ui.stone("Report", "reportPack", e -> report(s))));
        }
        f.show();
    }

    private static MultiButton mission(final ContentPack pack, final MissionDef m, int stars, boolean open,
            final PackDto dto) {
        MultiButton b = new MultiButton(m.name);
        b.setUIID("HistoryRow");
        b.setUIIDLine1(open ? "HistoryWin" : "HistoryDraw");
        b.setTextLine2(open ? starText(stars) + "   " + m.briefing : "Locked: win the mission before it first");
        b.setName("mission-" + m.id);
        b.setEnabled(open);
        b.addActionListener(e -> MissionPlay.start(pack, m.id, false, () -> show(pack.id)));
        return b;
    }

    static String starText(int stars) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            sb.append(i < stars ? '*' : '-');
        }
        return sb.toString();
    }

    private static void remix(final PackSummaryDto s) {
        final TextField name = new TextField(Fields.slug("my-" + s.id(), "my-pack"));
        name.setName("remixId");
        if (Dialog.show("Remix", name, new com.codename1.ui.Command("Remix"),
                new com.codename1.ui.Command("Cancel")).getCommandName().equals("Remix")) {
            Api.forge().fork(s.id(), new ForkDto(name.getText()), Net.to(made -> Ui.later(
                    () -> PackEditor.open(made.id())), Ui::failed));
        }
    }

    private static void rate(final PackSummaryDto s) {
        String[] choices = {"1 star", "2 stars", "3 stars", "4 stars", "5 stars"};
        Dialog d = new Dialog("Rate " + s.name());
        final int[] picked = {0};
        d.add(Fields.choice("Rating", "rating", choices, 4, v -> picked[0] = v + 1));
        picked[0] = 5;
        d.add(Ui.wood("Rate", "rateSend", e -> {
            d.dispose();
            Api.forge().rate(new RateDto(s.id(), picked[0]), Net.to(r -> Ui.toast("Thanks for rating"),
                    Ui::failed));
        }));
        d.show();
    }

    private static void report(final PackSummaryDto s) {
        final TextField why = new TextField("", "What is wrong with it?");
        why.setName("reportReason");
        if (Dialog.show("Report", why, new com.codename1.ui.Command("Report"),
                new com.codename1.ui.Command("Cancel")).getCommandName().equals("Report")) {
            Api.forge().report(new PackReportDto(s.id(), why.getText()), Net.to(r -> Ui.toast("Reported"),
                    Ui::failed));
        }
    }
}
