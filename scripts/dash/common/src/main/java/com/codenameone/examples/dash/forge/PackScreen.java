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
package com.codenameone.examples.dash.forge;

import com.codename1.components.MultiButton;
import com.codename1.components.ShareButton;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codenameone.examples.dash.AppConfig;
import com.codenameone.examples.dash.api.ForkDto;
import com.codenameone.examples.dash.api.LevelStarsDto;
import com.codenameone.examples.dash.api.PackDto;
import com.codenameone.examples.dash.api.PackReportDto;
import com.codenameone.examples.dash.api.PackSummaryDto;
import com.codenameone.examples.dash.api.RateDto;
import com.codenameone.examples.dash.net.Api;
import com.codenameone.examples.dash.net.Net;
import com.codenameone.examples.dash.net.Session;
import com.codenameone.examples.dash.rules.LevelDef;
import com.codenameone.examples.dash.rules.forge.ContentPack;
import com.codenameone.examples.dash.rules.forge.PackCodec;
import com.codenameone.examples.dash.run.Levels;
import com.codenameone.examples.dash.ui.LevelsForm;
import com.codenameone.examples.dash.ui.RaceLobby;
import com.codenameone.examples.dash.ui.RunForm;
import com.codenameone.examples.dash.ui.ScoresForm;
import com.codenameone.examples.dash.ui.Ui;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// A published pack: its levels in order with the stars already won, and what
/// a player can do with it -- run a level, race friends on it, see its
/// leaderboards, rate, remix, share.
public final class PackScreen {
    private PackScreen() {
    }

    public static void show(final String id) {
        Api.forge().pack(id, Net.to(dto -> Api.forge().stars(id, Net.to(stars -> Ui.later(() -> show(dto, stars)),
                Ui::failed)), Ui::failed));
    }

    private static void show(final PackDto dto, List<LevelStarsDto> stars) {
        final PackSummaryDto s = dto.summary();
        final ContentPack pack;
        try {
            pack = PackCodec.read(dto.content());
        } catch (IllegalArgumentException e) {
            Ui.failed(0, "That pack could not be read: " + e.getMessage());
            return;
        }
        Levels.remember(pack);
        final Map<String, Integer> won = new HashMap<String, Integer>();
        for (LevelStarsDto m : stars) {
            won.put(m.levelId(), Integer.valueOf(m.stars()));
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
        f.add(Ui.label("Levels", "SectionTitle"));
        for (LevelDef l : pack.levels) {
            Integer w = won.get(l.id);
            f.add(level(pack, l, w == null ? 0 : w.intValue()));
        }
        f.add(Ui.label("Share", "SectionTitle"));
        ShareButton share = new ShareButton();
        share.setText("Send to a friend");
        share.setUIID("PrimaryButton");
        // The share icon ShareButton brings is drawn on white; a material
        // icon takes the button's own colours.
        share.setMaterialIcon(com.codename1.ui.FontImage.MATERIAL_SHARE);
        share.setName("sharePack");
        share.setTextToShare("Play \"" + s.name() + "\" in Dash: " + AppConfig.packLink(s.id()));
        f.add(share);
        f.add(Ui.row(Ui.secondary("Copy the link", "copyPack", e -> {
            Display.getInstance().copyToClipboard(AppConfig.packLink(s.id()));
            Ui.toast("Copied");
        }), Ui.secondary("Remix", "remixPack", e -> remix(s))));
        String me = Session.user() == null ? "" : Session.user().username();
        if (me.equals(s.owner())) {
            f.add(Ui.secondary("Edit", "editPack", e -> PackEditor.open(s.id())));
        } else {
            f.add(Ui.row(Ui.secondary("Rate", "ratePack", e -> rate(s)),
                    Ui.secondary("Report", "reportPack", e -> report(s))));
        }
        f.show();
    }

    /// A level's row. A community run is on a seed of the level's own, the
    /// same every time, so its all-time board compares like with like.
    private static com.codename1.ui.Container level(final ContentPack pack, final LevelDef l, int stars) {
        MultiButton b = new MultiButton(l.name);
        b.setUIID("HistoryRow");
        b.setUIIDLine1("HistoryWin");
        b.setTextLine2(LevelsForm.describe(l));
        b.setEmblem(com.codenameone.examples.dash.ui.Kit.starsImage(stars,
                com.codename1.ui.CN.convertToPixels(3.4f)));
        b.setName("level-" + l.id);
        final Levels.Playable p = Levels.of(pack, l.id);
        final String ref = pack.id + "/" + l.id;
        b.addActionListener(e -> RunForm.solo(p, l.contentHash(), "solo", null, () -> show(pack.id)));
        return com.codename1.ui.layouts.BoxLayout.encloseY(b, Ui.row(
                Ui.button("Race friends", "DialogButton", "race-" + l.id, e -> RaceLobby.host(ref)),
                Ui.button("Scores", "DialogButton", "scores-" + l.id,
                        e -> ScoresForm.show(ref, () -> show(pack.id)))));
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
        d.add(Ui.primary("Rate", "rateSend", e -> {
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
