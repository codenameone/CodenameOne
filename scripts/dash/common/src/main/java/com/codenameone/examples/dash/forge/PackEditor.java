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
import com.codename1.ui.Command;
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codenameone.examples.dash.api.PackSaveDto;
import com.codenameone.examples.dash.net.Api;
import com.codenameone.examples.dash.net.Net;
import com.codenameone.examples.dash.net.Session;
import com.codenameone.examples.dash.rules.LevelDef;
import com.codenameone.examples.dash.rules.ObstacleDef;
import com.codenameone.examples.dash.rules.PickupDef;
import com.codenameone.examples.dash.rules.forge.ContentPack;
import com.codenameone.examples.dash.rules.forge.PackCodec;
import com.codenameone.examples.dash.rules.forge.Samples;
import com.codenameone.examples.dash.rules.forge.Validator;
import com.codenameone.examples.dash.ui.LevelsForm;
import com.codenameone.examples.dash.ui.Ui;
import java.util.List;

/// A pack being edited: its name and description, and the levels and the
/// custom obstacles and pickups in it, each opening its own editor.
///
/// The pack lives in memory while it is edited and goes to the server as a
/// draft on "Save". A draft can be half built; "Publish" is where the
/// [Validator] -- the one the server runs too -- has to be satisfied.
public final class PackEditor {
    /// The pack on the bench. Every sub-editor edits it in place.
    static ContentPack pack;

    private PackEditor() {
    }

    /// Starts a pack from a name, with one level to run straight away.
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
            show();
        }), Ui::failed));
    }

    static void show() {
        final ContentPack p = pack;
        Form f = Ui.form("Edit pack", "packEditor");
        Ui.back(f, () -> save(() -> ForgeHome.show("mine")));
        f.add(Fields.text("Name", "packName", p.name, 60, v -> p.name = v));
        f.add(Fields.area("Description", "packDescription", p.description, Validator.MAX_LONG_TEXT,
                v -> p.description = v));

        f.add(Ui.label("Levels", "SectionTitle"));
        f.add(Fields.help("Each level is a track: a length, a speed, the obstacles and pickups along it "
                + "and where the camera swings between the side view and the chase view."));
        for (int i = 0; i < p.levels.size(); i++) {
            final int at = i;
            LevelDef l = p.levels.get(i);
            f.add(item(l.name, LevelsForm.describe(l), "level-" + l.id, () -> LevelEditor.edit(at)));
        }
        f.add(Ui.secondary("Add a level", "addLevel", e -> LevelEditor.create()));

        f.add(Ui.label("Obstacles and pickups", "SectionTitle"));
        f.add(Fields.help("Your own pieces, on top of the game's hurdles, barriers, haulers, gaps, coins "
                + "and power-ups. Every level in the pack can place them."));
        for (int i = 0; i < p.defs.size(); i++) {
            final int at = i;
            Object d = p.defs.get(i);
            if (d instanceof ObstacleDef) {
                ObstacleDef o = (ObstacleDef) d;
                f.add(item(o.id, "Obstacle, " + DefEditor.clearanceName(o.clearance), "def-" + o.id,
                        () -> DefEditor.edit(at)));
            } else if (d instanceof PickupDef) {
                PickupDef k = (PickupDef) d;
                f.add(item(k.id, "Pickup, " + DefEditor.kindName(k.kind), "def-" + k.id, () -> DefEditor.edit(at)));
            }
        }
        f.add(Ui.row(Ui.secondary("Add an obstacle", "addObstacle", e -> DefEditor.create(true)),
                Ui.secondary("Add a pickup", "addPickup", e -> DefEditor.create(false))));

        List<String> problems = Validator.check(p);
        if (!problems.isEmpty()) {
            f.add(Ui.label("Before it can be published", "SectionTitle"));
            for (int i = 0; i < problems.size() && i < 6; i++) {
                f.add(Ui.text(problems.get(i), "ForgeProblem"));
            }
        }
        f.add(Ui.row(Ui.secondary("Save", "savePack", e -> save(() -> Ui.toast("Saved"))),
                Ui.primary("Publish", "publishPack", e -> publish())));
        f.add(Ui.secondary("Delete pack", "deletePack", e -> {
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

    /// Sends the draft, then runs `then`.
    static void save(final Runnable then) {
        Api.forge().saveDraft(new PackSaveDto(PackCodec.write(pack)), Net.to(s -> Ui.later(then), Ui::failed));
    }

    private static void publish() {
        List<String> problems = Validator.check(pack);
        if (!Validator.isValid(pack)) {
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
