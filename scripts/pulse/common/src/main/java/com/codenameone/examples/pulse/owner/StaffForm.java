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

import com.codename1.ui.Container;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.Component;
import com.codename1.ui.Sheet;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.spinner.Picker;
import com.codenameone.examples.pulse.api.ShiftDto;
import com.codenameone.examples.pulse.api.StaffDto;
import com.codenameone.examples.pulse.net.Api;
import com.codenameone.examples.pulse.net.Net;
import com.codenameone.examples.pulse.trainer.ShiftsForm;
import com.codenameone.examples.pulse.ui.Lang;
import com.codenameone.examples.pulse.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// The studio's staff and what each may do, and the rota of shifts. Roles
/// are switches: the server keeps a trainer with classes still to teach from
/// losing the trainer role, and says so.
public final class StaffForm {
    static final String[] ROLES = {"TRAINER", "DESK", "OWNER"};
    private static final String[] ROLE_NAMES = {"Trainer", "Front desk", "Owner"};
    private static final long HOUR = 3600000L;

    private StaffForm() {
    }

    public static void show(final Form previous) {
        final Form form = Ui.form("Staff", "Staff");
        Ui.back(form, previous);
        final Container list = Ui.list("staff");
        final Runnable[] load = new Runnable[1];
        load[0] = () -> Api.owner().staff(Net.to(staff -> fill(form, list, staff, load[0]),
                Ui.failed(list, load[0])));
        Ui.pull(list, load[0]);
        form.add(BorderLayout.CENTER, list);
        form.show();
        load[0].run();
    }

    private static void fill(Form form, Container list, List<StaffDto> staff,
            final Runnable load) {
        list.removeAll();
        for (int iter = 0; staff != null && iter < staff.size(); iter++) {
            final StaffDto person = staff.get(iter);
            // One row a person, their roles as chips; the switches that change
            // them are a tap away, in a sheet, rather than three rows each.
            Container chips = new Container(new FlowLayout(Component.RIGHT));
            for (int role = 0; role < ROLES.length; role++) {
                if (person.roles() != null && person.roles().contains(ROLES[role])) {
                    chips.add(Ui.badge(Lang.tr(ROLE_NAMES[role]), "SvBadge"));
                }
            }
            Container row = Ui.row((char) 0, person.name(), person.title() + Ui.DOT
                    + Lang.tr("{0} classes this week", Integer.valueOf(person.classesThisWeek())),
                    chips);
            row.add(BorderLayout.WEST, Ui.avatar(person.name()));
            final int index = iter;
            list.add(Ui.tap(row, "staff-" + iter, e -> roles(person, index, load)));
        }
        Ui.refresh(form);
    }

    private static void roles(final StaffDto person, int iter, final Runnable load) {
        final Sheet sheet = new Sheet(null, person.name());
        sheet.getContentPane().setLayout(BoxLayout.y());
        for (int role = 0; role < ROLES.length; role++) {
            final String name = ROLES[role];
            boolean has = person.roles() != null && person.roles().contains(name);
            sheet.getContentPane().add(Ui.row((char) 0, Lang.tr(ROLE_NAMES[role]), null,
                    Ui.toggle("role-" + iter + "-" + name, has, on -> {
                        sheet.back();
                        setRole(person, name, on, load);
                    })));
        }
        sheet.show();
    }

    private static void setRole(StaffDto person, String role, boolean on, final Runnable load) {
        List<String> roles = new ArrayList<String>();
        if (person.roles() != null) {
            roles.addAll(person.roles());
        }
        roles.remove(role);
        if (on) {
            roles.add(role);
        }
        Api.owner().saveStaff(new StaffDto(person.username(), person.name(), person.title(),
                person.bio(), person.locationId(), person.certifiedUntil(), roles,
                person.classesThisWeek()), Net.to(saved -> Ui.say(Lang.tr("Roles saved")),
                (status, message) -> {
                    Ui.fail(status, message);
                    load.run();
                }));
    }

    /// Every location's shifts to come, and a way to add one.
    public static void shifts(final Form previous) {
        final Form form = Ui.form("Shifts", "OwnerShifts");
        Ui.back(form, previous);
        final Container list = Ui.list("ownerShifts");
        final Runnable[] load = new Runnable[1];
        load[0] = () -> Api.owner().shifts("", Net.to(shifts -> {
            list.removeAll();
            for (int iter = 0; shifts != null && iter < shifts.size(); iter++) {
                ShiftDto shift = shifts.get(iter);
                list.add(ShiftsForm.row(shift, ShiftsForm.SWAP_REQUESTED.equals(shift.state())
                        ? Ui.badge(Lang.tr("Offered"), "SvBadgeWarn") : null));
            }
            if (shifts == null || shifts.isEmpty()) {
                list.add(Ui.text("No shifts on the rota.", "SvMuted"));
            }
            Ui.refresh(form);
        }, Ui.failed(list, load[0])));
        Ui.pull(list, load[0]);
        form.add(BorderLayout.CENTER, list);
        form.add(BorderLayout.SOUTH, Ui.dock(Ui.primary("Add a shift", "addShift",
                e -> addShift(form, load[0]))));
        form.show();
        load[0].run();
    }

    private static void addShift(final Form previous, final Runnable added) {
        final Form form = Ui.form("Add a shift", "AddShift");
        Ui.back(form, previous);
        final Container page = Ui.page();
        form.add(BorderLayout.CENTER, page);
        form.show();
        Api.owner().staff(Net.to(staff -> {
            if (staff == null || staff.isEmpty()) {
                page.add(Ui.text("Nobody on the staff yet.", "SvMuted"));
                Ui.refresh(form);
                return;
            }
            String[] names = new String[staff.size()];
            String[] ids = new String[staff.size()];
            for (int iter = 0; iter < names.length; iter++) {
                names[iter] = staff.get(iter).name();
                ids[iter] = staff.get(iter).username();
            }
            final String[] who = {ids[0]};
            page.add(Ui.label("Who", "SvLabel"));
            page.add(Ui.choice("who", names, ids, who[0], value -> who[0] = value, 2));
            final TextField duty = Ui.field(page, "Duty", "duty", "Front desk");
            page.add(Ui.label("Starts", "SvLabel"));
            final Picker starts = new Picker();
            starts.setType(Display.PICKER_TYPE_DATE_AND_TIME);
            starts.setName("starts");
            starts.setUIID("SvField");
            page.add(starts);
            final TextField hours = Ui.field(page, "Hours", "hours", "4", TextArea.NUMERIC);
            page.add(Ui.primary("Add", "save", e -> {
                String location = "";
                for (int iter = 0; iter < staff.size(); iter++) {
                    if (staff.get(iter).username().equals(who[0])) {
                        location = staff.get(iter).locationId();
                    }
                }
                long from = starts.getDate().getTime();
                long to = from + Math.max(1, PlansForm.number(hours.getText(), 4)) * HOUR;
                Api.owner().addShift(new ShiftDto(null, location, who[0], null,
                        duty.getText().trim(), from, to, ShiftsForm.ASSIGNED), Net.to(done -> {
                            Ui.say(Lang.tr("Shift added"));
                            previous.showBack();
                            added.run();
                        }, Ui::fail));
            }));
            Ui.refresh(form);
        }, Ui::fail));
    }
}
