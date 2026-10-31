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
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.pulse.api.NudgeDto;
import com.codenameone.examples.pulse.net.Api;
import com.codenameone.examples.pulse.net.Net;
import com.codenameone.examples.pulse.ui.Lang;
import com.codenameone.examples.pulse.ui.Ui;
import java.util.List;

/// Everything the studio sent: a place freed up in a queue, a streak about to
/// lapse, a challenge from a friend. The same messages arrive as push
/// notifications; this is where they stay. Opening one tells the server it was
/// read, which is how the owner's campaigns are measured.
public final class NudgesForm {
    private NudgesForm() {
    }

    public static void show(final Form previous) {
        final Form form = Ui.form("Inbox", "Nudges");
        Ui.back(form, previous);
        final Container list = Ui.list("nudges");
        final Runnable[] load = new Runnable[1];
        load[0] = () -> Api.engagement().nudges(Net.to(found -> fill(form, list, found, load[0]),
                Ui.failed(list, load[0])));
        Ui.pull(list, load[0]);
        form.add(BorderLayout.CENTER, list);
        form.show();
        load[0].run();
    }

    private static void fill(final Form form, Container list, List<NudgeDto> nudges,
            final Runnable load) {
        list.removeAll();
        if (nudges == null || nudges.isEmpty()) {
            list.add(Ui.empty(FontImage.MATERIAL_INBOX, Lang.tr("Nothing yet"),
                    Lang.tr("News from the studio shows up here.")));
        }
        for (int iter = 0; nudges != null && iter < nudges.size(); iter++) {
            final NudgeDto nudge = nudges.get(iter);
            boolean unread = nudge.openedAt() <= 0;
            Container row = Ui.row(unread ? FontImage.MATERIAL_ADD_ALERT
                    : FontImage.MATERIAL_INBOX, nudge.title(),
                    nudge.body() + "\n" + Ui.when(nudge.sentAt()),
                    unread ? Ui.badge(Lang.tr("New"), "SvBadgeGood") : null);
            list.add(Ui.tap(row, "nudge-" + iter, e -> {
                if (nudge.openedAt() <= 0) {
                    Api.engagement().opened(nudge.id(), Net.to(opened -> load.run(),
                            (status, message) -> { }));
                }
                if (nudge.sessionId() != null && nudge.sessionId().length() > 0) {
                    ScheduleForm.show(form);
                }
            }));
        }
        Ui.refresh(form);
    }
}
