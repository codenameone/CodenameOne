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
package com.codenameone.examples.dash.admin;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.Component;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.dash.api.ConditionDto;
import java.util.ArrayList;
import java.util.List;

/// The list of conditions a query or a pipeline is made of: "days away is
/// at least 7", "purchases equals 0". Each line is a field, a comparison and
/// a number; a player must meet every line. The fields are the server's
/// (`social.Audience`), which refuses anything else.
// tag::dash-conditions[]
final class Conditions {
    static final String[] FIELDS = {"daysAway", "daysSinceJoin", "trophies", "races", "wins", "gems", "coins",
        "purchases", "friends"};
    static final String[] FIELD_NAMES = {"Days away", "Days since joining", "Trophies", "Races", "Wins", "Gems",
        "Coins", "Purchases", "Friends invited"};
    /// The same names as they read inside a sentence.
    static final String[] FIELD_WORDS = {"days away", "days since joining", "trophies", "races", "wins", "gems",
        "coins", "purchases", "friends invited"};
    static final String[] OPS = {">=", "<=", "=", ">", "<", "!="};
    static final String[] OP_NAMES = {"at least", "at most", "equals", "more than", "less than", "is not"};
    static final int MAX = 6;

    private final List<ConditionDto> lines = new ArrayList<>();
    private final Container box = new Container(BoxLayout.y());
    private final String name;

    Conditions(String name, List<ConditionDto> start) {
        this.name = name;
        if (start != null) {
            lines.addAll(start);
        }
        box.setName(name);
        build();
    }

    Container component() {
        return box;
    }

    List<ConditionDto> value() {
        return new ArrayList<>(lines);
    }

    /// "days away at least 7 and purchases equals 0", for a list.
    static String describe(List<ConditionDto> conditions) {
        StringBuilder sb = new StringBuilder();
        for (ConditionDto c : conditions) {
            if (sb.length() > 0) {
                sb.append(" and ");
            }
            sb.append(FIELD_WORDS[index(FIELDS, c.field())]).append(' ')
                    .append(OP_NAMES[index(OPS, c.op())]).append(' ').append(c.value());
        }
        return sb.length() == 0 ? "every player" : sb.toString();
    }

    private void build() {
        box.removeAll();
        for (int i = 0; i < lines.size(); i++) {
            box.add(line(i));
        }
        if (lines.size() < MAX) {
            Button add = Desk.button("Add condition", name + "-add", e -> {
                lines.add(new ConditionDto("daysAway", ">=", 7));
                build();
            });
            FontImage.setMaterialIcon(add, FontImage.MATERIAL_ADD);
            box.add(Desk.actions(add));
        }
        if (box.getComponentForm() != null) {
            box.getComponentForm().revalidate();
        }
    }

    private Container line(final int i) {
        ConditionDto c = lines.get(i);
        Component field = Desk.choice(name + "-field-" + i, FIELD_NAMES, index(FIELDS, c.field()),
                v -> set(i, FIELDS[v], null, null));
        Component op = Desk.choice(name + "-op-" + i, OP_NAMES, index(OPS, c.op()), v -> set(i, null, OPS[v], null));
        Component value = Desk.number(name + "-value-" + i, c.value(), v -> set(i, null, null, v));
        Container fields;
        if (AdminConsole.wide()) {
            fields = GridLayout.encloseIn(3, field, op, value);
        } else {
            // A phone has no room for three pickers side by side: the field
            // on its own line, the comparison under it.
            fields = BoxLayout.encloseY(field, GridLayout.encloseIn(2, op, value));
        }
        Button remove = Desk.button("", name + "-remove-" + i, e -> {
            lines.remove(i);
            build();
        });
        FontImage.setMaterialIcon(remove, FontImage.MATERIAL_CLOSE);
        Container row = new Container(new BorderLayout());
        row.setUIID("DeskConditionRow");
        // One width for "Where" and "and", so the rows' fields line up.
        com.codename1.ui.Label joint = new com.codename1.ui.Label(i == 0 ? "Where" : "and", "DeskLabel");
        joint.setPreferredW(new com.codename1.ui.Label("Where", "DeskLabel").getPreferredW());
        row.add(BorderLayout.WEST, joint);
        row.add(BorderLayout.CENTER, fields).add(BorderLayout.EAST, remove);
        return row;
    }

    private void set(int i, String field, String op, Integer value) {
        ConditionDto c = lines.get(i);
        lines.set(i, new ConditionDto(field == null ? c.field() : field, op == null ? c.op() : op,
                value == null ? c.value() : value));
    }

    static int index(String[] all, String s) {
        for (int i = 0; i < all.length; i++) {
            if (all[i].equals(s)) {
                return i;
            }
        }
        return 0;
    }
}
// end::dash-conditions[]
