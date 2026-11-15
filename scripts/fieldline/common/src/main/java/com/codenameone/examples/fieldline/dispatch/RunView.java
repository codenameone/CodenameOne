package com.codenameone.examples.fieldline.dispatch;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.fieldline.api.ChangeDto;
import com.codenameone.examples.fieldline.api.KpiDto;
import com.codenameone.examples.fieldline.api.ScheduleRunDto;
import com.codenameone.examples.fieldline.ui.Lang;
import com.codenameone.examples.fieldline.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// What a run of the planner did, as a card: the four figures that matter
/// before and after, each with its change coloured by whether it got better,
/// and the jobs that moved with the reason for each.
public final class RunView {
    private RunView() {
    }

    public static Component card(final Form from, final ScheduleRunDto run) {
        Container card = new Container(BoxLayout.y());
        card.setUIID("FlRunCard");
        card.setName("runCard");
        Container head = new Container(new BorderLayout());
        Label icon = new Label("", "FlRunIcon");
        Ui.icon(icon, FontImage.MATERIAL_AUTO_FIX_HIGH, 4f);
        head.add(BorderLayout.WEST, icon);
        Container words = new Container(BoxLayout.y());
        words.add(Ui.label(kind(run.kind()), "FlRunTitle"));
        words.add(Ui.text(run.summary(), "FlRunSummary"));
        head.add(BorderLayout.CENTER, words);
        card.add(head);
        if (run.before() != null && run.after() != null) {
            card.add(deltas(run.before(), run.after()));
        }
        int moved = run.changes() == null ? 0 : run.changes().size();
        int shown = 0;
        if (run.changes() != null) {
            for (ChangeDto c : run.changes()) {
                if (shown++ >= 4) {
                    break;
                }
                card.add(change(c));
            }
        }
        if (moved > 4) {
            card.add(Ui.link(Lang.tr("All {0} changes", String.valueOf(moved)), "allChanges",
                    e -> RunForm.show(from, run)));
        }
        card.add(Ui.label(Lang.tr("{0} iterations in {1} ms", String.valueOf(run.iterations()),
                String.valueOf(run.millis())) + Ui.DOT + run.operators(), "FlRunFoot"));
        return card;
    }

    static Container deltas(KpiDto b, KpiDto a) {
        List<Component> cells = new ArrayList<Component>();
        cells.add(delta("Unassigned", b.unassigned(), a.unassigned(), false, ""));
        // A figure that is nought before and after says nothing; leaving it
        // out keeps the rest readable in a narrow inspector.
        if (b.lateJobs() != 0 || a.lateJobs() != 0) {
            cells.add(delta("Past SLA", b.lateJobs(), a.lateJobs(), false, ""));
        }
        cells.add(delta("Driving", b.driveMinutes(), a.driveMinutes(), false, " min"));
        if (b.overtimeMinutes() != 0 || a.overtimeMinutes() != 0) {
            cells.add(delta("Overtime", b.overtimeMinutes(), a.overtimeMinutes(), false, " min"));
        }
        cells.add(delta("Booked", b.utilizationPct(), a.utilizationPct(), true, "%"));
        Container grid = new Container(new GridLayout(1, cells.size()));
        grid.setUIID("FlDeltas");
        for (Component c : cells) {
            grid.add(c);
        }
        return grid;
    }

    private static Component delta(String caption, int before, int after, boolean upIsGood,
            String unit) {
        Container box = new Container(BoxLayout.y());
        box.setUIID("FlDelta");
        box.add(Ui.label(amount(after, unit), "FlDeltaValue"));
        int d = after - before;
        String style = d == 0 ? "FlDeltaSame" : (d > 0) == upIsGood ? "FlDeltaGood" : "FlDeltaBad";
        String text = d == 0 ? "=" : (d > 0 ? "+" : "-") + amount(Math.abs(d), unit);
        box.add(Ui.label(text, style));
        box.add(Ui.label(caption, "FlDeltaCaption"));
        return box;
    }

    /// A week of driving is thousands of minutes, which does not fit a
    /// quarter of the inspector: past two hours the figure is whole hours.
    private static String amount(int value, String unit) {
        if (" min".equals(unit) && value >= 120) {
            return (value + 30) / 60 + " h";
        }
        return value + unit;
    }

    static Component change(ChangeDto c) {
        String from = c.fromTech() == null || c.fromTech().length() == 0
                ? Lang.tr("unassigned") : c.fromTech();
        String to = c.toTech() == null || c.toTech().length() == 0
                ? Lang.tr("unassigned") : c.toTech();
        String detail = from.equals(to) ? Lang.tr("{0}, moved to {1}", to, Ui.clock(c.toStart()))
                : from + "  \u2192  " + to + (c.toStart() > 0 ? Ui.DOT + Ui.clock(c.toStart()) : "");
        if (c.reason() != null && c.reason().length() > 0) {
            detail = detail + "\n" + c.reason();
        }
        return Ui.row(FontImage.MATERIAL_SWAP_HORIZ, c.jobTitle(), detail, null);
    }

    static String kind(String kind) {
        if ("ABSENCE".equals(kind)) {
            return Lang.tr("Absence covered");
        }
        if ("TIME_OFF".equals(kind)) {
            return Lang.tr("Time off covered");
        }
        if ("AVAILABILITY".equals(kind)) {
            return Lang.tr("Re-planned for new hours");
        }
        if ("NEW_JOB".equals(kind)) {
            return Lang.tr("New job placed");
        }
        if ("CREW".equals(kind)) {
            return Lang.tr("Re-planned for a crew change");
        }
        if ("MOVE".equals(kind)) {
            return Lang.tr("Manual move");
        }
        return Lang.tr("Schedule optimized");
    }
}
