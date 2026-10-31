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

import com.codename1.components.SpanLabel;
import com.codename1.ui.ButtonGroup;
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.RadioButton;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.dash.rules.Catalog;
import com.codenameone.examples.dash.rules.Content;
import com.codenameone.examples.dash.rules.DialogLine;
import com.codenameone.examples.dash.rules.Goal;
import com.codenameone.examples.dash.rules.LevelDef;
import com.codenameone.examples.dash.rules.ObstacleDef;
import com.codenameone.examples.dash.rules.PickupDef;
import com.codenameone.examples.dash.rules.Placement;
import com.codenameone.examples.dash.rules.Track;
import com.codenameone.examples.dash.rules.Validator;
import com.codenameone.examples.dash.rules.ViewCue;
import com.codenameone.examples.dash.rules.ViewMode;
import com.codenameone.examples.dash.run.Levels;
import com.codenameone.examples.dash.run.Theme;
import com.codenameone.examples.dash.ui.RunForm;
import com.codenameone.examples.dash.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// One level of the pack on the bench: its settings, the track itself as a
/// [TrackTimeline] to paint pieces and camera cues on, the coach's lines, and
/// a playtest that runs the draft exactly as a player would -- the same
/// [com.codenameone.examples.dash.rules.Run], so what passes here passes on
/// the server.
public final class LevelEditor {
    private static final String[] GOALS = {"Reach the finish", "Collect coins", "Survive", "Endless"};
    private static final String ERASE = "";

    private LevelEditor() {
    }

    static void create() {
        int n = PackEditor.pack.levels.size() + 1;
        while (PackEditor.pack.level("level-" + n) != null) {
            n++;
        }
        LevelDef l = new LevelDef("level-" + n, "Level " + n);
        l.length = 400 * Track.UNIT;
        l.stars = new int[] {300, 400, 500};
        l.cue(0, ViewMode.SIDE_2D, 0);
        l.cue(200, ViewMode.CHASE_3D, 30);
        PackEditor.pack.levels.add(l);
        edit(PackEditor.pack.levels.size() - 1);
    }

    static void edit(final int index) {
        final LevelDef l = PackEditor.pack.levels.get(index);
        final Catalog catalog = PackEditor.pack.catalog();
        Form f = Ui.form(l.name, "levelEditor");
        Ui.back(f, PackEditor::show);
        f.add(Fields.text("Name", "levelName", l.name, 40, v -> l.name = v));
        f.add(Fields.choice("Theme", "levelTheme", Theme.IDS, Math.max(0, Fields.indexOf(Theme.IDS, l.theme)),
                i -> l.theme = Theme.IDS[i]));
        f.add(Fields.stepper("Lanes", "levelLanes", l.lanes, 1, 5, 1, "", v -> l.lanes = v));
        f.add(Fields.tenths("Start speed", "levelStart", l.startSpeed / 100, 40, 200, 5, "m/s",
                v -> l.startSpeed = v * 100));
        f.add(Fields.tenths("Top speed", "levelTop", l.maxSpeed / 100, 60, 300, 5, "m/s",
                v -> l.maxSpeed = v * 100));
        f.add(Fields.stepper("Length", "levelLength", l.length / Track.UNIT, 0, 5000, 50, "m",
                v -> l.length = v * Track.UNIT));
        f.add(Fields.choice("Goal", "levelGoal", GOALS, l.goal.ordinal(), i -> l.goal = Goal.values()[i]));
        f.add(Fields.stepper("Goal value", "levelGoalValue", l.goalValue, 0, 10000, 10, "", v -> l.goalValue = v));
        f.add(Fields.stepper("Endless from", "levelProcedural", l.proceduralFrom / Track.UNIT, 0, 5000, 50, "m",
                v -> l.proceduralFrom = v * Track.UNIT));
        f.add(Fields.help("Past \"endless from\" the track builds itself out of the game's chunks, harder "
                + "every stretch. At 0 m the whole level is hand built."));
        for (int s = 0; s < 3; s++) {
            final int star = s;
            f.add(Fields.stepper("Star " + (s + 1), "levelStar" + s, l.stars[s], 0, 100000, 25, "points",
                    v -> l.stars[star] = v));
        }

        f.add(Ui.label("Track", "SectionTitle"));
        final TrackTimeline timeline = new TrackTimeline(l, catalog);
        final SpanLabel problems = new SpanLabel("", "ForgeProblem");
        timeline.setChanged(() -> problems.setText(problemsOf(l, catalog)));
        f.add(palette(l, catalog, timeline));
        Container strip = new Container(BoxLayout.x());
        strip.setScrollableX(true);
        strip.add(timeline);
        f.add(strip);
        f.add(Fields.help("Tap a lane to place the piece you picked; tap the top row for one across every "
                + "lane. Tap a piece with \"Erase\" to take it away. A camera cue goes where you tap."));
        problems.setText(problemsOf(l, catalog));
        f.add(problems);

        f.add(Ui.label("Coach", "SectionTitle"));
        for (int i = 0; i < l.dialog.size(); i++) {
            final DialogLine d = l.dialog.get(i);
            f.add(Ui.row(Ui.text(d.at / Track.UNIT + " m: " + d.text, "QuestReward"),
                    Ui.secondary("Remove", "removeLine" + i, e -> {
                        l.dialog.remove(d);
                        edit(index);
                    })));
        }
        f.add(Ui.secondary("Add a line", "addLine", e -> addLine(l, index)));

        f.add(Ui.row(Ui.primary("Playtest", "playtest", e -> RunForm.playtest(
                Levels.draft(PackEditor.pack, l), () -> edit(index))),
                Ui.secondary("Delete level", "deleteLevel", e -> {
                    if (Dialog.show("Delete?", "Delete " + l.name + "?", "Delete", "Keep")) {
                        PackEditor.pack.levels.remove(index);
                        PackEditor.show();
                    }
                })));
        f.show();
    }

    /// The pieces to paint with: every obstacle and pickup the pack can use,
    /// the camera modes, and an eraser.
    // tag::dash-level-palette[]
    private static Container palette(final LevelDef l, final Catalog catalog, final TrackTimeline timeline) {
        Container c = new Container(new FlowLayout());
        ButtonGroup group = new ButtonGroup();
        List<String> ids = new ArrayList<String>();
        ids.add(Content.HURDLE);
        ids.add(Content.BARRIER);
        ids.add(Content.HAULER);
        ids.add(Content.GAP);
        ids.add(Content.COIN);
        ids.add(Content.GEM);
        ids.add(Content.MAGNET);
        ids.add(Content.SHIELD);
        ids.add(Content.BOOST);
        for (Object d : PackEditor.pack.defs) {
            if (d instanceof ObstacleDef) {
                ids.add(((ObstacleDef) d).id);
            } else if (d instanceof PickupDef) {
                ids.add(((PickupDef) d).id);
            }
        }
        boolean first = true;
        for (final String id : ids) {
            RadioButton b = tool(group, id, "tool-" + id);
            b.addActionListener(e -> timeline.setTool((level, metres, lane) -> place(level, catalog, id, metres, lane)));
            if (first) {
                b.setSelected(true);
                timeline.setTool((level, metres, lane) -> place(level, catalog, id, metres, lane));
                first = false;
            }
            c.add(b);
        }
        ViewMode[] modes = ViewMode.values();
        for (int i = 0; i < modes.length; i++) {
            final ViewMode m = modes[i];
            RadioButton b = tool(group, "Cue " + TrackTimeline.modeName(m), "tool-cue-" + m.name());
            b.addActionListener(e -> timeline.setTool((level, metres, lane) -> cue(level, m, metres)));
            c.add(b);
        }
        RadioButton erase = tool(group, "Erase", "tool-erase");
        erase.addActionListener(e -> timeline.setTool((level, metres, lane) -> place(level, catalog, ERASE, metres,
                lane)));
        c.add(erase);
        return c;
    }
    // end::dash-level-palette[]

    private static RadioButton tool(ButtonGroup group, String text, String name) {
        RadioButton b = new RadioButton(text);
        b.setToggle(true);
        b.setUIID("BoardTab");
        b.setName(name);
        group.add(b);
        return b;
    }

    /// Puts `id` at the spot, or erases what is there. A piece already in the
    /// cell is replaced rather than stacked.
    private static boolean place(LevelDef l, Catalog catalog, String id, int metres, int lane) {
        int z = metres * Track.UNIT;
        boolean removed = false;
        for (int i = l.placements.size() - 1; i >= 0; i--) {
            Placement p = l.placements.get(i);
            ObstacleDef o = catalog.obstacle(p.defId);
            int depth = o == null ? 2 * Track.UNIT : Math.max(Track.UNIT, o.depth);
            if (p.lane == lane && z >= p.z && z < p.z + Math.max(5 * Track.UNIT, depth)) {
                l.placements.remove(i);
                removed = true;
            }
        }
        if (ERASE.equals(id)) {
            return removed;
        }
        if (l.placements.size() >= com.codenameone.examples.dash.rules.forge.Validator.MAX_PLACEMENTS) {
            Ui.toast("That is as many pieces as a level can hold");
            return removed;
        }
        l.placements.add(new Placement(z, id, lane));
        return true;
    }

    /// A camera cue at `metres`, replacing one already within ten metres.
    private static boolean cue(LevelDef l, ViewMode mode, int metres) {
        for (int i = l.cues.size() - 1; i >= 0; i--) {
            if (Math.abs(l.cues.get(i).at - metres * Track.UNIT) < 10 * Track.UNIT) {
                l.cues.remove(i);
            }
        }
        int blend = metres == 0 ? 0 : 30 * Track.UNIT;
        ViewCue c = new ViewCue(metres * Track.UNIT, mode, blend);
        int at = 0;
        while (at < l.cues.size() && l.cues.get(at).at < c.at) {
            at++;
        }
        l.cues.add(at, c);
        return true;
    }

    private static void addLine(final LevelDef l, final int index) {
        final TextField text = new TextField("", "What the coach says", 40, TextField.ANY);
        final TextField at = new TextField("0", "At metres", 6, TextField.NUMERIC);
        Container body = BoxLayout.encloseY(text, at);
        Command ok = new Command("Add");
        if (Dialog.show("Coach line", body, ok, new Command("Cancel")) != ok || text.getText().trim().length() == 0) {
            return;
        }
        int metres = parse(at.getText());
        DialogLine d = new DialogLine(metres * Track.UNIT, "Coach", text.getText().trim(), 0);
        int i = 0;
        while (i < l.dialog.size() && l.dialog.get(i).at <= d.at) {
            i++;
        }
        l.dialog.add(i, d);
        edit(index);
    }

    private static int parse(String s) {
        int v = 0;
        for (int i = 0; i < s.length() && i < 6; i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                v = v * 10 + (c - '0');
            }
        }
        return v;
    }

    private static String problemsOf(LevelDef l, Catalog catalog) {
        List<Validator.Problem> ps = Validator.check(l, catalog);
        if (ps.isEmpty()) {
            return "Ready to publish.";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ps.size() && i < 5; i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(ps.get(i));
        }
        return sb.toString();
    }
}
