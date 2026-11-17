package com.codenameone.examples.fieldline.dispatch;

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.fieldline.api.ChangeDto;
import com.codenameone.examples.fieldline.api.ScheduleRunDto;
import com.codenameone.examples.fieldline.ui.Ui;

/// Every change a run of the planner made, with its reason.
public final class RunForm {
    private RunForm() {
    }

    public static void show(Form previous, ScheduleRunDto run) {
        Form form = Ui.form(RunView.kind(run.kind()), "Run");
        Ui.back(form, previous);
        form.add(BorderLayout.CENTER, page(run));
        form.show();
    }

    /// What a run changed, as a page: the screen's content on a phone, the
    /// detail beside the list of runs in the console.
    static Container page(ScheduleRunDto run) {
        Container page = Ui.page();
        page.add(Ui.text(run.summary(), "FlRunSummary"));
        if (run.before() != null && run.after() != null) {
            page.add(RunView.deltas(run.before(), run.after()));
        }
        Ui.section(page, "Changes");
        Container list = Ui.list("changes");
        // A list fills a screen of its own; here it is part of the page,
        // which scrolls, and it is filled at once rather than fetched.
        list.removeAll();
        list.setScrollableY(false);
        if (run.changes() != null) {
            for (ChangeDto c : run.changes()) {
                list.add(RunView.change(c));
            }
        }
        page.add(list);
        page.add(Ui.label(Ui.when(run.at()), "FlRunFoot"));
        return page;
    }
}
