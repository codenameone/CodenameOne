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
package com.codenameone.examples.pulse.owner;

import com.codename1.components.Switch;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.pulse.api.ClassTypeDto;
import com.codenameone.examples.pulse.net.Api;
import com.codenameone.examples.pulse.net.Net;
import com.codenameone.examples.pulse.ui.Lang;
import com.codenameone.examples.pulse.ui.Ui;
import java.util.List;

/// The kinds of class the studio teaches. A template holds what every class
/// of its kind shares -- length, intensity, places, ages -- so scheduling one
/// is a time, a room and a trainer.
public final class TemplatesForm {
    private TemplatesForm() {
    }

    public static void show(final Form previous) {
        final Form form = Ui.form("Class templates", "Templates");
        Ui.back(form, previous);
        final Container list = Ui.list("templates");
        final Runnable[] load = new Runnable[1];
        load[0] = () -> Api.studio().classTypes(Net.to(types -> fill(form, list, types, load[0]),
                Ui.failed(list, load[0])));
        Ui.pull(list, load[0]);
        form.add(BorderLayout.CENTER, list);
        form.add(BorderLayout.SOUTH, Ui.dock(Ui.primary("New template", "newTemplate",
                e -> edit(form, null, load[0]))));
        form.show();
        load[0].run();
    }

    private static void fill(final Form form, Container list, List<ClassTypeDto> types,
            final Runnable load) {
        list.removeAll();
        for (int iter = 0; types != null && iter < types.size(); iter++) {
            final ClassTypeDto type = types.get(iter);
            Container row = Ui.row((char) 0, type.name(),
                    Lang.tr("{0} min", Integer.valueOf(type.minutes())) + Ui.DOT
                    + Lang.tr("{0} places", Integer.valueOf(type.capacity())) + Ui.DOT
                    + Lang.tr("intensity {0} of 5", Integer.valueOf(type.intensity())),
                    type.active() ? null : Ui.badge(Lang.tr("Retired"), "SvBadgeBad"));
            row.add(BorderLayout.WEST, com.codenameone.examples.pulse.member.Classes.tile(
                    type.category(), type.color()));
            list.add(Ui.tap(row, "template-" + iter, e -> edit(form, type, load)));
        }
        Ui.refresh(form);
    }

    private static void edit(final Form previous, final ClassTypeDto type, final Runnable saved) {
        final Form form = Ui.form(type == null ? "New template" : type.name(), "TemplateEditor");
        Ui.back(form, previous);
        Container page = Ui.page();
        final TextField name = Ui.field(page, "Name", "name", "Spin 45");
        final TextField about = Ui.field(page, "Description", "description",
                "What the class is like");
        final TextField category = Ui.field(page, "Category", "category", "Cardio");
        final TextField minutes = Ui.field(page, "Minutes", "minutes", "45", TextArea.NUMERIC);
        final TextField capacity = Ui.field(page, "Places", "capacity", "20", TextArea.NUMERIC);
        final TextField intensity = Ui.field(page, "Intensity, 1 to 5", "intensity", "3",
                TextArea.NUMERIC);
        final TextField minAge = Ui.field(page, "Youngest age (0 for any)", "minAge", "0",
                TextArea.NUMERIC);
        final TextField maxAge = Ui.field(page, "Oldest age (0 for any)", "maxAge", "0",
                TextArea.NUMERIC);
        final TextField color = Ui.field(page, "Colour", "color", "#e4572e");
        final boolean[] active = {type == null || type.active()};
        Switch activeSwitch = Ui.toggle("active", active[0], on -> active[0] = on);
        page.add(Ui.row((char) 0, Lang.tr("Bookable"), null, activeSwitch));
        if (type != null) {
            name.setText(type.name());
            about.setText(type.description());
            category.setText(type.category());
            minutes.setText(String.valueOf(type.minutes()));
            capacity.setText(String.valueOf(type.capacity()));
            intensity.setText(String.valueOf(type.intensity()));
            minAge.setText(String.valueOf(type.minAge()));
            maxAge.setText(String.valueOf(type.maxAge()));
            color.setText(type.color());
        }
        page.add(Ui.primary("Save", "save", e -> {
            if (name.getText().trim().length() == 0) {
                Ui.say(Lang.tr("A template needs a name."));
                return;
            }
            ClassTypeDto wanted = new ClassTypeDto(type == null ? null : type.id(),
                    name.getText().trim(), about.getText().trim(), category.getText().trim(),
                    PlansForm.number(intensity.getText(), 3), PlansForm.number(minAge.getText(), 0),
                    PlansForm.number(maxAge.getText(), 0), PlansForm.number(capacity.getText(), 20),
                    PlansForm.number(minutes.getText(), 45), color.getText().trim(), active[0]);
            Api.owner().saveClassType(wanted, Net.to(done -> {
                Ui.say(Lang.tr("Template saved"));
                previous.showBack();
                saved.run();
            }, Ui::fail));
        }));
        form.add(BorderLayout.CENTER, page);
        form.show();
    }
}
