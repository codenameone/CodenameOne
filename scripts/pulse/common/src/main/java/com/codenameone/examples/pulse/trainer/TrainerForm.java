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
import com.codenameone.examples.pulse.api.SessionDto;
import com.codenameone.examples.pulse.live.LiveChannel;
import com.codenameone.examples.pulse.member.Classes;
import com.codenameone.examples.pulse.net.Api;
import com.codenameone.examples.pulse.net.Net;
import com.codenameone.examples.pulse.ui.Lang;
import com.codenameone.examples.pulse.ui.Menu;
import com.codenameone.examples.pulse.ui.Nav;
import com.codenameone.examples.pulse.ui.Ui;
import java.util.List;

/// The trainer's home: the classes they teach this week, each leading to its
/// roster, and from there to the live view of the class.
public class TrainerForm extends LiveChannel.Adapter implements Nav.Home {
    private final Form form = Ui.home("Trainer");
    private final Menu menu = new Menu(form, Nav.TRAINER);
    private final Container list = Ui.list("trainer");
    private List<SessionDto> classes;

    @Override
    public Form form() {
        return form;
    }

    @Override
    public void start() {
        menu.item(FontImage.MATERIAL_EVENT, "My classes", "classes", () -> form.showBack());
        menu.item(FontImage.MATERIAL_FAVORITE, "Live class", "live", this::live);
        menu.item(FontImage.MATERIAL_SCHEDULE, "Shifts", "shifts", () -> ShiftsForm.show(form));
        Container bar = new Container(new BorderLayout());
        bar.setUIID("SvHomeBar");
        bar.add(BorderLayout.WEST, menu.flatButton());
        bar.add(BorderLayout.CENTER, Ui.label("Coaching", "SvTitle"));
        Ui.pull(list, this::load);
        Container shell = new Container(new BorderLayout());
        shell.add(BorderLayout.NORTH, bar);
        shell.add(BorderLayout.CENTER, list);
        form.add(shell);
        menu.install();
        form.addShowListener(e -> {
            LiveChannel.listen(this);
            load();
        });
    }

    private void load() {
        Api.trainer().classes(Net.to(found -> {
            classes = found;
            fill();
        }, Ui.failed(list, this::load)));
    }

    private void fill() {
        list.removeAll();
        Ui.section(list, "This week");
        for (int iter = 0; classes != null && iter < classes.size(); iter++) {
            final SessionDto session = classes.get(iter);
            boolean cancelled = Classes.CANCELLED.equals(session.state());
            Container row = Ui.row(FontImage.MATERIAL_FITNESS_CENTER, session.name(),
                    Ui.when(session.startsAt()) + Ui.DOT + session.roomName(),
                    Ui.badge(cancelled ? Lang.tr("Cancelled")
                            : session.booked() + "/" + session.capacity(),
                            cancelled ? "SvBadgeBad" : "SvBadge"));
            list.add(Ui.tap(row, "class-" + session.id(), e -> RosterForm.show(form, session)));
        }
        if (classes == null || classes.isEmpty()) {
            list.add(Ui.empty(FontImage.MATERIAL_SPORTS, Lang.tr("No classes this week"),
                    Lang.tr("Classes the studio gives you appear here.")));
        }
        Ui.refresh(form);
    }

    /// Opens the class that is on now, or the next one.
    private void live() {
        SessionDto now = current(classes, System.currentTimeMillis());
        if (now == null) {
            Ui.say(Lang.tr("No class today"));
            return;
        }
        LiveClassForm.show(form, now);
    }

    /// The class running at `at`, else the next to start; cancelled ones never.
    static SessionDto current(List<SessionDto> classes, long at) {
        SessionDto next = null;
        for (int iter = 0; classes != null && iter < classes.size(); iter++) {
            SessionDto session = classes.get(iter);
            if (Classes.CANCELLED.equals(session.state()) || session.endsAt() < at) {
                continue;
            }
            if (session.startsAt() <= at) {
                return session;
            }
            if (next == null || session.startsAt() < next.startsAt()) {
                next = session;
            }
        }
        return next;
    }

    @Override
    public void sessionChanged(String id, int booked, int waitlisted, String state) {
        load();
    }
}
