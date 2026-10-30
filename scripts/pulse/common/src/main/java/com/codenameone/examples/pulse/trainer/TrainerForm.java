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
package com.codenameone.examples.pulse.trainer;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.pulse.ui.Lang;
import com.codenameone.examples.pulse.ui.Menu;
import com.codenameone.examples.pulse.ui.Nav;
import com.codenameone.examples.pulse.ui.Ui;

/// The trainer's home: today's classes, rosters and the live class view.
///
/// STUB: the home and its menu entries are in place so navigation by role
/// works end to end; the screens behind them are built in the next phase.
/// Each menu entry below names the screen it will open and the `TrainerApi`
/// call that feeds it.
public class TrainerForm implements Nav.Home {
    private final Form form = Ui.home("Trainer");
    private final Menu menu = new Menu(form, Nav.TRAINER);

    @Override
    public Form form() {
        return form;
    }

    @Override
    public void start() {
        // STUB: TrainerClassesForm (TrainerApi.classes)
        menu.item(FontImage.MATERIAL_EVENT, "My classes", "classes", this::soon);
        // STUB: RosterForm: mark attendance (TrainerApi.roster/attendance)
        menu.item(FontImage.MATERIAL_CHECK_CIRCLE, "Roster", "roster", this::soon);
        // STUB: LiveClassForm: heart rate zones (LiveChannel.heartRate)
        menu.item(FontImage.MATERIAL_FAVORITE, "Live class", "live", this::soon);
        // STUB: ShiftsForm: availability and swaps (TrainerApi.shifts)
        menu.item(FontImage.MATERIAL_SCHEDULE, "Shifts", "shifts", this::soon);
        Container bar = new Container(new BorderLayout());
        bar.setUIID("SvHomeBar");
        bar.add(BorderLayout.WEST, menu.flatButton());
        bar.add(BorderLayout.CENTER, Ui.label("Coaching", "SvTitle"));
        Container page = Ui.page();
        page.setName("trainer");
        page.add(Ui.empty(FontImage.MATERIAL_SPORTS, Lang.tr("Coaching"),
                Lang.tr("Your classes, rosters and live heart rate zones are on their way.")));
        Container shell = new Container(new BorderLayout());
        shell.add(BorderLayout.NORTH, bar);
        shell.add(BorderLayout.CENTER, page);
        form.add(shell);
        menu.install();
    }

    // STUB: replaced by the real screen in the next phase.
    private void soon() {
        Ui.say("Coming soon");
    }
}
