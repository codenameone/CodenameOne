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
package com.codenameone.examples.splat.ui;

import com.codename1.io.Storage;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.splat.rules.Mission;
import com.codenameone.examples.splat.rules.Mode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/// The mission editor: lay out your half of a field, pick the mode, write
/// the briefing, play-test it against bots, and publish it so your friends
/// and your party can play it.
///
/// The draft is kept on the device as JSON after every change, so leaving
/// the screen loses nothing.
// tag::splat-mission-editor[]
public final class MissionEditor {
    static final String DRAFT = "splat-mission-draft.json";
    /// The arena code a play-test runs under; publishing gives the real one.
    static final String TEST_CODE = "draft";

    private MissionEditor() {
    }

    public static void show() {
        open(loadDraft());
    }

    static void open(final Mission mission) {
        final Form f = Ui.form("MISSION EDITOR", "missionEditor");
        f.setLayout(new BorderLayout());
        f.setScrollableY(false);
        Ui.back(f, MissionsForm::show);
        final Label status = new Label(" ", "EditorStatus");
        status.setName("editorStatus");
        final MissionCanvas[] canvas = new MissionCanvas[1];
        canvas[0] = new MissionCanvas(mission, () -> {
            saveDraft(mission);
            String p = mission.problem();
            status.setText(p == null ? mission.props.size() + " PROPS  //  " + mission.spawns.size()
                    + " SPAWNS  //  READY" : Ui.caps(p));
        });

        TextField name = new TextField(mission.name, "Operation name", 20, TextArea.ANY);
        name.setName("missionName");
        name.setUIID("EditorField");
        name.addDataChangedListener((t, i) -> {
            mission.name = name.getText();
            saveDraft(mission);
        });
        Container modes = new Container(new GridLayout(1, 3));
        final Button[] modeButtons = new Button[Mode.values().length];
        for (int i = 0; i < modeButtons.length; i++) {
            final Mode m = Mode.values()[i];
            String label = m == Mode.TEAM_PAINT ? "TEAM" : m == Mode.CAPTURE_THE_FLAG ? "FLAG" : "SNACK BAR";
            modeButtons[i] = Ui.button(label, mission.mode == m ? "EditorChipOn" : "EditorChip",
                    "mode-" + m.name(), e -> {
                        mission.mode = m;
                        for (int k = 0; k < modeButtons.length; k++) {
                            modeButtons[k].setUIID(Mode.values()[k] == m ? "EditorChipOn" : "EditorChip");
                        }
                        saveDraft(mission);
                        canvas[0].repaint();
                        f.revalidate();
                    });
            modes.add(modeButtons[i]);
        }

        // The palette: one chip per prop, then the spawn and flag tools.
        final String[] tools = new String[Mission.KINDS.length + 2];
        System.arraycopy(Mission.KINDS, 0, tools, 0, Mission.KINDS.length);
        tools[tools.length - 2] = MissionCanvas.SPAWN;
        tools[tools.length - 1] = MissionCanvas.FLAG;
        final Button[] chips = new Button[tools.length];
        Container strip = new Container(BoxLayout.x());
        strip.setScrollableX(true);
        for (int i = 0; i < tools.length; i++) {
            final String t = tools[i];
            chips[i] = Ui.button(Ui.caps(t.replace('-', ' ')), t.equals(canvas[0].tool()) ? "EditorChipOn"
                    : "EditorChip", "tool-" + t, e -> {
                        canvas[0].setTool(t);
                        for (int k = 0; k < chips.length; k++) {
                            chips[k].setUIID(tools[k].equals(t) ? "EditorChipOn" : "EditorChip");
                        }
                        f.revalidate();
                    });
            strip.add(chips[i]);
        }
        Container edit = new Container(new GridLayout(1, 2));
        edit.add(Ui.secondary("TURN", "turnProp", e -> canvas[0].turnSelected()));
        edit.add(Ui.secondary("REMOVE", "removeProp", e -> canvas[0].removeSelected()));

        TextArea briefing = new TextArea(mission.briefing, 3, 30);
        briefing.setName("missionBriefing");
        briefing.setUIID("EditorField");
        briefing.setHint("Briefing: what the narrator reads before the match");
        briefing.addDataChangedListener((t, i) -> {
            mission.briefing = briefing.getText();
            saveDraft(mission);
        });

        Container middle = new Container(BoxLayout.y());
        middle.setScrollableY(true);
        f.add(BorderLayout.NORTH, BoxLayout.encloseY(name, modes, strip));
        middle.add(canvas[0]).add(status).add(edit).add(briefing);
        f.add(BorderLayout.CENTER, middle);

        Container go = new Container(new GridLayout(1, 2));
        go.add(Ui.secondary("PLAY TEST", "playTest", e -> playTest(mission)));
        go.add(Ui.primary("PUBLISH", "publishMission", e -> MissionsForm.publish(mission)));
        f.add(BorderLayout.SOUTH, go);
        if (CN.isDesktop()) {
            Ui.desktopColumn(f);
        }
        f.show();
        String p = mission.problem();
        status.setText(p == null ? "DRAG TO MOVE  //  TAP GROUND TO PLACE" : Ui.caps(p));
    }

    /// Plays the draft at once against bots, as it would be played online.
    static void playTest(Mission mission) {
        String p = mission.problem();
        if (p != null) {
            Ui.toast(p);
            return;
        }
        Mission test = Mission.fromJson(mission.toJson());
        test.code = TEST_CODE;
        test.register();
        MatchForm.practice(test.arenaId() + ":" + test.mode.name(), () -> open(mission));
    }

    static Mission loadDraft() {
        if (Storage.getInstance().exists(DRAFT)) {
            try {
                InputStream in = Storage.getInstance().createInputStream(DRAFT);
                try {
                    ByteArrayOutputStream buf = new ByteArrayOutputStream();
                    byte[] b = new byte[4096];
                    int n;
                    while ((n = in.read(b)) > 0) {
                        buf.write(b, 0, n);
                    }
                    return Mission.fromJson(new String(buf.toByteArray(), "UTF-8"));
                } finally {
                    in.close();
                }
            } catch (IOException | IllegalArgumentException e) {
                // an unreadable draft starts over
            }
        }
        return Mission.blank();
    }

    static void saveDraft(Mission mission) {
        try {
            OutputStream out = Storage.getInstance().createOutputStream(DRAFT);
            try {
                byte[] b = mission.toJson().getBytes("UTF-8");
                out.write(b);
            } finally {
                out.close();
            }
        } catch (IOException e) {
            // the draft is a convenience; the editor still has the mission
        }
    }
}
// end::splat-mission-editor[]
