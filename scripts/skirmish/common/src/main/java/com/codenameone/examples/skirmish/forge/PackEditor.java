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
import com.codename1.ui.Command;
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codenameone.examples.skirmish.api.PackSaveDto;
import com.codenameone.examples.skirmish.battle.ForgeFiles;
import com.codenameone.examples.skirmish.net.Api;
import com.codenameone.examples.skirmish.net.Net;
import com.codenameone.examples.skirmish.net.Session;
import com.codenameone.examples.skirmish.rules.Arena;
import com.codenameone.examples.skirmish.rules.CardDef;
import com.codenameone.examples.skirmish.rules.forge.AssetRef;
import com.codenameone.examples.skirmish.rules.forge.CampaignDef;
import com.codenameone.examples.skirmish.rules.forge.ContentPack;
import com.codenameone.examples.skirmish.rules.forge.MissionDef;
import com.codenameone.examples.skirmish.rules.forge.PackCodec;
import com.codenameone.examples.skirmish.rules.forge.Samples;
import com.codenameone.examples.skirmish.rules.forge.Validator;
import com.codenameone.examples.skirmish.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// A pack being edited: its name and description, and the cards, arenas,
/// missions and campaigns in it, each opening its own editor.
///
/// The pack lives in memory while it is edited and goes to the server as a
/// draft on "Save". A draft can be half built; "Publish" is where the
/// [Validator] -- the one the server runs too -- has to be satisfied.
public final class PackEditor {
    /// The pack on the bench. Every sub-editor edits it in place.
    static ContentPack pack;

    private PackEditor() {
    }

    /// Starts a pack from a name, with one mission against the bot to play
    /// straight away.
    public static void create() {
        final TextField name = new TextField("", "Name your pack");
        name.setName("packName");
        Command go = new Command("Create");
        if (Dialog.show("New pack", name, go, new Command("Cancel")) != go || name.getText().trim().length() == 0) {
            return;
        }
        create(name.getText().trim());
    }

    /// A new pack under this name, saved as a draft and opened in the editor.
    public static void create(String name) {
        String id = Fields.slug(name, "pack") + "-" + Long.toString(System.currentTimeMillis() % 46656L, 36);
        pack = Samples.blank(id, Session.user() == null ? "" : Session.user().displayName());
        pack.name = name;
        save(PackEditor::show);
    }

    public static void open(String id) {
        Api.forge().draft(id, Net.to(dto -> Ui.later(() -> {
            try {
                pack = PackCodec.read(dto.content());
            } catch (IllegalArgumentException e) {
                Ui.failed(0, "That draft could not be read: " + e.getMessage());
                return;
            }
            List<String> files = new ArrayList<String>();
            for (AssetRef a : pack.assets) {
                files.add(a.sha256);
            }
            ForgeFiles.fetch(files, () -> Ui.later(PackEditor::show), Ui::failed);
        }), Ui::failed));
    }

    static void show() {
        final ContentPack p = pack;
        Form f = Ui.form("Edit pack", "packEditor");
        Ui.back(f, () -> save(() -> ForgeHome.show("mine")));
        f.add(Fields.text("Name", "packName", p.name, 60, v -> p.name = v));
        f.add(Fields.area("Description", "packDescription", p.description, Validator.MAX_LONG_TEXT,
                v -> p.description = v));

        f.add(Ui.label("Characters and cards", "SectionTitle"));
        f.add(Fields.help("Your own troops, buildings, spells and towers. A pack's missions can use these "
                + "and every card of the game."));
        for (int i = 0; i < p.cards.size(); i++) {
            final int at = i;
            CardDef c = p.cards.get(i);
            f.add(item(c.name, kind(c.kind) + ", " + c.cost + " elixir", "card-" + c.id,
                    () -> CardEditor.edit(at)));
        }
        f.add(Ui.stone("Add a card", "addCard", e -> CardEditor.create()));

        f.add(Ui.label("Arenas", "SectionTitle"));
        for (int i = 0; i < p.arenas.size(); i++) {
            final int at = i;
            Arena a = p.arenas.get(i);
            f.add(item(a.name(), a.width() / Arena.TILE + " x " + a.height() / Arena.TILE + " tiles, "
                    + a.laneCount() + (a.laneCount() == 1 ? " lane" : " lanes"), "arena-" + a.id(),
                    () -> ArenaEditor.edit(at)));
        }
        f.add(Ui.stone("Add an arena", "addArena", e -> ArenaEditor.create()));

        f.add(Ui.label("Missions", "SectionTitle"));
        for (int i = 0; i < p.missions.size(); i++) {
            final int at = i;
            MissionDef m = p.missions.get(i);
            f.add(item(m.name, m.briefing, "mission-" + m.id, () -> MissionEditor.edit(at)));
        }
        f.add(Ui.stone("Add a mission", "addMission", e -> MissionEditor.create()));

        f.add(Ui.label("Campaigns", "SectionTitle"));
        f.add(Fields.help("A campaign puts missions in order, with a story before and after. Each mission "
                + "opens once the one before it is won."));
        for (int i = 0; i < p.campaigns.size(); i++) {
            final int at = i;
            CampaignDef c = p.campaigns.get(i);
            f.add(item(c.name, c.missions.size() + " missions", "campaign-" + c.id,
                    () -> CampaignEditor.edit(at)));
        }
        f.add(Ui.stone("Add a campaign", "addCampaign", e -> CampaignEditor.create()));

        List<String> problems = Validator.check(p);
        if (!problems.isEmpty()) {
            f.add(Ui.label("Before it can be published", "SectionTitle"));
            for (int i = 0; i < problems.size() && i < 6; i++) {
                f.add(Ui.text(problems.get(i), "ForgeProblem"));
            }
        }
        f.add(Ui.row(Ui.stone("Save", "savePack", e -> save(() -> Ui.toast("Saved"))),
                Ui.wood("Publish", "publishPack", e -> publish())));
        f.add(Ui.stone("Delete pack", "deletePack", e -> {
            if (Dialog.show("Delete?", "Players who have the link can no longer open it.", "Delete", "Keep")) {
                Api.forge().delete(p.id, Net.to(s -> Ui.later(() -> ForgeHome.show("mine")), Ui::failed));
            }
        }));
        f.show();
    }

    private static MultiButton item(String title, String detail, String name, final Runnable open) {
        MultiButton b = new MultiButton(title);
        b.setUIID("HistoryRow");
        b.setUIIDLine1("HistoryDraw");
        b.setTextLine2(detail);
        b.setName(name);
        b.addActionListener(e -> open.run());
        return b;
    }

    static String kind(int kind) {
        return kind == CardDef.BUILDING ? "Building" : kind == CardDef.SPELL ? "Spell"
                : kind == CardDef.TOWER ? "Tower" : "Troop";
    }

    /// Sends the draft, then runs `then`.
    static void save(final Runnable then) {
        Api.forge().saveDraft(new PackSaveDto(PackCodec.write(pack)), Net.to(s -> Ui.later(then), Ui::failed));
    }

    private static void publish() {
        List<String> problems = Validator.check(pack);
        if (!problems.isEmpty()) {
            Dialog.show("Not yet", problems.get(0), "OK", null);
            return;
        }
        final String id = pack.id;
        save(() -> Api.forge().publish(id, Net.to(s -> Ui.later(() -> {
            Ui.toast("Published version " + s.version());
            PackScreen.show(id);
        }), Ui::failed)));
    }
}
