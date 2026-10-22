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

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.skirmish.rules.forge.CampaignDef;
import com.codenameone.examples.skirmish.rules.forge.ContentPack;
import com.codenameone.examples.skirmish.rules.forge.MissionDef;
import com.codenameone.examples.skirmish.rules.forge.Validator;
import com.codenameone.examples.skirmish.ui.Ui;

/// A campaign: missions in the order they open, the story before the first
/// and after the last, and how many stars the finale asks for.
public final class CampaignEditor {
    private final int index;
    private final CampaignDef c;

    private CampaignEditor(int index) {
        this.index = index;
        this.c = PackEditor.pack.campaigns.get(index);
    }

    static void create() {
        CampaignDef c = new CampaignDef();
        String id = "campaign";
        for (int n = 2; PackEditor.pack.campaign(id) != null; n++) {
            id = "campaign-" + n;
        }
        c.id = id;
        c.name = PackEditor.pack.name;
        for (MissionDef m : PackEditor.pack.missions) {
            c.missions.add(m.id);
        }
        PackEditor.pack.campaigns.add(c);
        new CampaignEditor(PackEditor.pack.campaigns.size() - 1).show();
    }

    static void edit(int index) {
        new CampaignEditor(index).show();
    }

    private void show() {
        final ContentPack p = PackEditor.pack;
        Form f = Ui.form("Campaign", "campaignEditor");
        Ui.back(f, PackEditor::show);
        f.add(Fields.text("Name", "campaignName", c.name, 40, v -> c.name = v));
        f.add(Fields.area("Before the first mission", "campaignIntro", c.intro, Validator.MAX_LONG_TEXT,
                v -> c.intro = v));
        f.add(Fields.area("After the last", "campaignOutro", c.outro, Validator.MAX_LONG_TEXT, v -> c.outro = v));
        f.add(Ui.label("Missions, in order", "SectionTitle"));
        for (int i = 0; i < c.missions.size(); i++) {
            final int at = i;
            MissionDef m = p.mission(c.missions.get(i));
            Container row = new Container(new BorderLayout());
            row.setUIID("ForgeRow");
            row.add(BorderLayout.CENTER, Ui.label((i + 1) + ". " + (m == null ? c.missions.get(i) : m.name),
                    "ForgeLabel"));
            row.add(BorderLayout.EAST, BoxLayout.encloseX(
                    Ui.button("Up", "ForgeStep", "campaign-up-" + i, e -> move(at, -1)),
                    Ui.button("Down", "ForgeStep", "campaign-down-" + i, e -> move(at, 1)),
                    Ui.button("x", "ForgeStep", "campaign-remove-" + i, e -> {
                        c.missions.remove(at);
                        show();
                    })));
            f.add(row);
        }
        for (final MissionDef m : p.missions) {
            if (!c.missions.contains(m.id)) {
                f.add(Ui.stone("Add " + m.name, "campaign-add-" + m.id, e -> {
                    c.missions.add(m.id);
                    show();
                }));
            }
        }
        f.add(Fields.stepper("Stars to open the finale", "campaignFinale", c.finaleStars, 0,
                3 * Math.max(1, c.missions.size() - 1), 1, "", v -> c.finaleStars = v));
        f.add(Ui.row(Ui.stone("Done", "campaignDone", e -> PackEditor.show()),
                Ui.stone("Delete campaign", "campaignDelete", e -> {
                    PackEditor.pack.campaigns.remove(index);
                    PackEditor.show();
                })));
        f.show();
    }

    private void move(int at, int by) {
        int to = at + by;
        if (to < 0 || to >= c.missions.size()) {
            return;
        }
        String s = c.missions.remove(at);
        c.missions.add(to, s);
        show();
    }
}
