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

import com.codename1.ui.Command;
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codenameone.examples.dash.rules.Clearance;
import com.codenameone.examples.dash.rules.ObstacleDef;
import com.codenameone.examples.dash.rules.PickupDef;
import com.codenameone.examples.dash.rules.PickupKind;
import com.codenameone.examples.dash.rules.Track;
import com.codenameone.examples.dash.ui.Ui;

/// A custom obstacle or pickup of the pack. Definitions are immutable values
/// in the rules, so every change builds a new one and puts it back in the
/// pack's list at the same place; levels refer to it by id, which never
/// changes once it is made.
public final class DefEditor {
    private static final String[] CLEARANCES = {"Jump it", "Slide under it", "Change lanes"};
    private static final String[] KINDS = {"Coin", "Gem", "Magnet", "Shield", "Boost"};

    private DefEditor() {
    }

    static String clearanceName(Clearance c) {
        return CLEARANCES[c.ordinal()];
    }

    static String kindName(PickupKind k) {
        return KINDS[k.ordinal()];
    }

    static void create(boolean obstacle) {
        final TextField name = new TextField("", obstacle ? "Name, e.g. log" : "Name, e.g. star");
        name.setName("defName");
        Command go = new Command("Create");
        if (Dialog.show(obstacle ? "New obstacle" : "New pickup", name, go, new Command("Cancel")) != go
                || name.getText().trim().length() == 0) {
            return;
        }
        String id = unique("u-" + Fields.slug(name.getText().trim(), "piece"));
        if (obstacle) {
            PackEditor.pack.defs.add(new ObstacleDef(id, Clearance.JUMP, Track.UNIT, 500, false));
        } else {
            PackEditor.pack.defs.add(new PickupDef(id, PickupKind.COIN, 5, 0));
        }
        edit(PackEditor.pack.defs.size() - 1);
    }

    private static String unique(String id) {
        String out = id;
        int n = 2;
        while (PackEditor.pack.catalog().has(out)) {
            out = id + "-" + n++;
        }
        return out;
    }

    static void edit(final int index) {
        Object d = PackEditor.pack.defs.get(index);
        Form f = Ui.form("Edit piece", "defEditor");
        Ui.back(f, PackEditor::show);
        if (d instanceof ObstacleDef) {
            final ObstacleDef[] o = {(ObstacleDef) d};
            f.add(Ui.label(o[0].id, "SectionTitle"));
            f.add(Fields.choice("Way past", "defClearance", CLEARANCES, o[0].clearance.ordinal(), i -> {
                o[0] = new ObstacleDef(o[0].id, Clearance.values()[i], o[0].depth, o[0].height, o[0].lethal);
                PackEditor.pack.defs.set(index, o[0]);
            }));
            f.add(Fields.tenths("Depth", "defDepth", o[0].depth / 100, 5, 80, 5, "m", v -> {
                o[0] = new ObstacleDef(o[0].id, o[0].clearance, v * 100, o[0].height, o[0].lethal);
                PackEditor.pack.defs.set(index, o[0]);
            }));
            f.add(Fields.tenths("Height", "defHeight", o[0].height / 100, 0, 30, 1, "m", v -> {
                o[0] = new ObstacleDef(o[0].id, o[0].clearance, o[0].depth, v * 100, o[0].lethal);
                PackEditor.pack.defs.set(index, o[0]);
            }));
            f.add(Fields.help("A height of 0 is a gap in the floor. A lethal piece ends the run; the others "
                    + "only stumble the runner."));
            f.add(Fields.check("Lethal", "defLethal", o[0].lethal, v -> {
                o[0] = new ObstacleDef(o[0].id, o[0].clearance, o[0].depth, o[0].height, v);
                PackEditor.pack.defs.set(index, o[0]);
            }));
        } else if (d instanceof PickupDef) {
            final PickupDef[] p = {(PickupDef) d};
            f.add(Ui.label(p[0].id, "SectionTitle"));
            f.add(Fields.choice("Kind", "defKind", KINDS, p[0].kind.ordinal(), i -> {
                p[0] = new PickupDef(p[0].id, PickupKind.values()[i], p[0].points, p[0].ticks);
                PackEditor.pack.defs.set(index, p[0]);
            }));
            f.add(Fields.stepper("Points", "defPoints", p[0].points, 0, 500, 5, "", v -> {
                p[0] = new PickupDef(p[0].id, p[0].kind, v, p[0].ticks);
                PackEditor.pack.defs.set(index, p[0]);
            }));
            f.add(Fields.stepper("Lasts", "defTicks", p[0].ticks / 60, 0, 30, 1, "s", v -> {
                p[0] = new PickupDef(p[0].id, p[0].kind, p[0].points, v * 60);
                PackEditor.pack.defs.set(index, p[0]);
            }));
        }
        f.add(Ui.secondary("Delete piece", "deleteDef", e -> {
            if (Dialog.show("Delete?", "Levels that place it will stop validating.", "Delete", "Keep")) {
                PackEditor.pack.defs.remove(index);
                PackEditor.show();
            }
        }));
        f.show();
    }
}
