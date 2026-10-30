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

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.pulse.api.AwayDto;
import com.codenameone.examples.pulse.net.Api;
import com.codenameone.examples.pulse.net.Net;
import com.codenameone.examples.pulse.ui.Lang;
import com.codenameone.examples.pulse.ui.Ui;
import java.util.List;

/// Saying you will not be in: travelling, injured, or anything else.
///
/// While an away period runs the server sends no "we miss you" nudges, keeps
/// the streak from breaking, and -- when the member asks -- freezes billing
/// for the plan, as a freeze would.
public final class AwayForm {
    private AwayForm() {
    }

    public static void show(final Form previous) {
        final Form form = Ui.form("Away", "Away");
        Ui.back(form, previous);
        final Container list = Ui.list("away");
        final Runnable[] load = new Runnable[1];
        load[0] = () -> Api.engagement().away(Net.to(found -> fill(form, list, found, load[0]),
                Ui.failed(list, load[0])));
        form.add(BorderLayout.CENTER, list);
        form.add(BorderLayout.SOUTH, Ui.dock(Ui.primary("I'll be away", "addAway",
                e -> add(form, load[0]))));
        form.show();
        load[0].run();
    }

    private static String reason(String reason) {
        return "TRAVEL".equals(reason) ? Lang.tr("Travelling")
                : "INJURY".equals(reason) ? Lang.tr("Injured") : Lang.tr("Away");
    }

    private static void fill(Form form, Container list, List<AwayDto> periods,
            final Runnable load) {
        list.removeAll();
        if (periods == null || periods.isEmpty()) {
            list.add(Ui.empty(FontImage.MATERIAL_BEACH_ACCESS, Lang.tr("No time away"),
                    Lang.tr("Going somewhere? Tell us and we will not nag you.")));
        }
        for (int iter = 0; periods != null && iter < periods.size(); iter++) {
            final AwayDto period = periods.get(iter);
            list.add(Ui.row(FontImage.MATERIAL_BEACH_ACCESS, reason(period.reason()),
                    period.startsOn() + " - " + period.endsOn()
                    + (period.freezeBilling() ? "\n" + Lang.tr("Billing paused") : ""),
                    Ui.flat(FontImage.MATERIAL_DELETE_OUTLINE, "Remove", "removeAway-" + iter,
                            e -> Api.engagement().removeAway(period.id(),
                                    Net.to(gone -> load.run(), Ui::fail)))));
        }
        Ui.refresh(form);
    }

    private static void add(final Form previous, final Runnable added) {
        final Form form = Ui.form("Time away", "AddAway");
        Ui.back(form, previous);
        Container page = Ui.page();
        final TextField from = Ui.field(page, "From", "from", "YYYY-MM-DD");
        final TextField until = Ui.field(page, "Until", "until", "YYYY-MM-DD");
        final String[] reason = {"TRAVEL"};
        page.add(Ui.label("Why", "SvLabel"));
        page.add(Ui.choice("reason", new String[] {Lang.tr("Travel"), Lang.tr("Injury"),
            Lang.tr("Other")}, new String[] {"TRAVEL", "INJURY", "OTHER"}, reason[0],
                value -> reason[0] = value));
        final boolean[] freeze = {false};
        page.add(Ui.row(FontImage.MATERIAL_PAUSE_CIRCLE, Lang.tr("Pause billing"),
                Lang.tr("Freeze the plan for these dates"),
                Ui.toggle("freezeBilling", false, on -> freeze[0] = on)));
        page.add(Ui.primary("Save", "submit", e -> Api.engagement().addAway(new AwayDto(null,
                from.getText().trim(), until.getText().trim(), reason[0], freeze[0]),
                Net.to(saved -> {
                    added.run();
                    previous.showBack();
                }, Ui::fail))));
        form.add(BorderLayout.CENTER, page);
        form.show();
    }
}
