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
package com.codenameone.examples.pulse.member;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.pulse.api.MemberDto;
import com.codenameone.examples.pulse.api.NewMemberDto;
import com.codenameone.examples.pulse.net.Api;
import com.codenameone.examples.pulse.net.Net;
import com.codenameone.examples.pulse.ui.Lang;
import com.codenameone.examples.pulse.ui.Ui;
import java.util.List;

/// The account holder and the children they book for.
///
/// A child is booked by their guardian, into the classes of their age band,
/// and needs a signed waiver before their first class -- the guardian signs it
/// here. Each child has a pass of their own, for a child who comes in without
/// the guardian's phone.
public final class HouseholdForm {
    private HouseholdForm() {
    }

    public static void show(final Form previous) {
        final Form form = Ui.form("Household", "Household");
        Ui.back(form, previous);
        final Container list = Ui.list("household");
        final Runnable[] load = new Runnable[1];
        load[0] = () -> Api.household().members(Net.to(members -> fill(form, list, members,
                load[0]), Ui.failed(list, load[0])));
        Ui.pull(list, load[0]);
        form.add(BorderLayout.CENTER, list);
        form.add(BorderLayout.SOUTH, Ui.dock(Ui.primary("Add a child", "addChild",
                e -> add(form, load[0]))));
        form.show();
        load[0].run();
    }

    private static void fill(final Form form, Container list, List<MemberDto> members,
            final Runnable load) {
        list.removeAll();
        for (int iter = 0; members != null && iter < members.size(); iter++) {
            final MemberDto member = members.get(iter);
            boolean child = "CHILD".equals(member.relation());
            Component end;
            if (member.waiverSigned()) {
                end = Ui.flat(FontImage.MATERIAL_QR_CODE,
                        Lang.tr("Pass for {0}", member.name()), "pass-" + iter,
                        e -> PassForm.show(form, member.id()));
            } else {
                Button sign = new Button(Lang.tr("Sign waiver"), "SvPillQueue");
                sign.setName("waiver-" + iter);
                sign.addActionListener(e -> Api.household().signWaiver(member.id(),
                        Net.to(signed -> load.run(), Ui::fail)));
                end = sign;
            }
            Container row = Ui.row(child ? FontImage.MATERIAL_CHILD_CARE
                    : FontImage.MATERIAL_PERSON, member.name(),
                    Lang.tr("Age {0}", Integer.valueOf(member.age()))
                    + (member.planName() == null ? "" : "  \u00b7  " + member.planName())
                    + (member.waiverSigned() ? "" : "\n" + Lang.tr("Waiver needed before class")),
                    end);
            row.setName("member-" + iter);
            list.add(member.waiverSigned() ? Ui.tap(row, "open-" + iter,
                    e -> PassForm.show(form, member.id())) : row);
        }
        Ui.refresh(form);
    }

    private static void add(final Form previous, final Runnable added) {
        final Form form = Ui.form("Add a child", "AddChild");
        Ui.back(form, previous);
        Container page = Ui.page();
        final TextField name = Ui.field(page, "Name", "name", "Their first and last name");
        final TextField birth = Ui.field(page, "Date of birth", "birthDate", "YYYY-MM-DD");
        final TextField medical = Ui.field(page, "Anything the coach should know",
                "medical", "Allergies, asthma, an injury - optional");
        page.add(Ui.text("Kids' classes are by age, so the timetable shows them the classes "
                + "they can join. You sign their waiver next.", "SvMuted"));
        final SpanLabel error = Ui.text("", "SvError");
        error.setName("error");
        final Button save = Ui.primary("Add", "submit", null);
        save.addActionListener(e -> {
            save.setEnabled(false);
            Api.household().addChild(new NewMemberDto(name.getText().trim(),
                    birth.getText().trim(), medical.getText().trim()), Net.to(child -> {
                        added.run();
                        previous.showBack();
                    }, (status, message) -> {
                        save.setEnabled(true);
                        error.setText(message);
                        Ui.refresh(form);
                    }));
        });
        page.add(error).add(save);
        form.add(BorderLayout.CENTER, page);
        form.show();
    }
}
