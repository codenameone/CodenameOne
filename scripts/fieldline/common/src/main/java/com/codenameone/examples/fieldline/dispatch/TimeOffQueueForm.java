package com.codenameone.examples.fieldline.dispatch;

import com.codename1.appkit.Net;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.fieldline.Telemetry;
import com.codenameone.examples.fieldline.api.DecisionDto;
import com.codenameone.examples.fieldline.api.TimeOffDto;
import com.codenameone.examples.fieldline.net.Api;
import com.codenameone.examples.fieldline.ui.Layouts;
import com.codenameone.examples.fieldline.ui.Ui;
import java.util.List;

/// Time off waiting for an answer. Approving one is what substitution is: the
/// planner takes the technician off those days and re-places their jobs on
/// the rest of the crew, and the card that comes back says who got what.
public final class TimeOffQueueForm {
    private TimeOffQueueForm() {
    }

    public static void show(final Form previous, final Runnable changed) {
        final Form form = Ui.form("Time-off requests", "TimeOffQueue");
        Ui.back(form, () -> {
            changed.run();
            previous.showBack();
        });
        final Container page = Ui.page();
        form.add(BorderLayout.CENTER, page);
        form.show();
        load(form, page);
    }

    /// The queue as a page of the console, on `form`, filled in as it is made.
    static Container content(Form form) {
        Container page = Ui.page();
        page.setName("TimeOffQueue");
        load(form, page);
        return page;
    }

    private static void load(final Form form, final Container page) {
        load(form, page, null);
    }

    /// Fills the queue, with `header` -- what the last decision changed --
    /// above it.
    private static void load(final Form form, final Container page, final Component header) {
        Api.dispatch().timeOff(Net.to(list -> fill(form, page, list, header), Ui::fail));
    }

    private static void fill(final Form form, final Container page, List<TimeOffDto> list,
            Component header) {
        page.removeAll();
        if (header != null) {
            page.add(header);
        }
        if (list.isEmpty()) {
            page.add(Ui.empty(FontImage.MATERIAL_INBOX, "Nothing waiting",
                    "New requests from the crew land here."));
        }
        for (final TimeOffDto t : list) {
            Container card = new Container(BoxLayout.y());
            card.setUIID("FlCard");
            card.add(Ui.person(t.techName(), com.codenameone.examples.fieldline.field
                    .TimeOffForm.range(t.start(), t.end()), Ui.badge(
                            com.codenameone.examples.fieldline.field.TimeOffForm.reasonName(
                                    t.reason()), "FlBadgeKind")));
            Container buttons = new Container(new GridLayout(1, 2));
            buttons.add(Ui.secondary("Decline", "decline-" + t.id(),
                    e -> decide(form, page, t, false)));
            buttons.add(Ui.primary("Approve and cover", "approve-" + t.id(),
                    e -> decide(form, page, t, true)));
            card.add(buttons);
            page.add(card);
        }
        Layouts.dress(page);
        form.revalidate();
    }

    private static void decide(final Form form, final Container page, TimeOffDto t,
            final boolean approve) {
        Api.dispatch().decide(t.id(), new DecisionDto(approve), Net.to(run -> {
            if (approve) {
                Telemetry.absenceCovered();
            }
            load(form, page, run != null && approve ? RunView.card(form, run) : null);
        }, Ui::fail));
    }
}
