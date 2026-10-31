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
package com.codenameone.examples.dash.admin;

import com.codename1.l10n.SimpleDateFormat;
import com.codename1.components.Switch;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.dash.api.ConditionDto;
import com.codenameone.examples.dash.api.PipelineDto;
import com.codenameone.examples.dash.api.QueryDto;
import com.codenameone.examples.dash.net.Api;
import com.codenameone.examples.dash.net.Net;
import com.codenameone.examples.dash.ui.Ui;
import java.util.Date;
import java.util.List;

/// Engagement pipelines: "when a player matches these conditions, do this,
/// at most once every N days". The server runs every enabled pipeline each
/// hour (`social.Pipelines`); this page lists them, edits them and runs one
/// at once.
///
/// A push from a pipeline carries the campaign `pipeline-<name>`, so how
/// many players it brought back is read in Codename One analytics next to
/// every other campaign.
// tag::dash-pipelines-tab[]
final class PipelinesTab {
    static final String[] ACTIONS = {"push", "gems"};
    static final String[] ACTION_NAMES = {"Send a push", "Give gems, with a push"};

    private final Container page = Desk.page();

    PipelinesTab() {
        page.setName("pipelines");
    }

    Container page() {
        return page;
    }

    /// Set by the query tool just before it selects this tab: selecting a tab
    /// refreshes it, and the list arriving afterwards would replace the editor.
    private PipelineDto pending;

    void refresh() {
        if (pending != null) {
            PipelineDto start = pending;
            pending = null;
            edit(start);
            return;
        }
        Api.admin().pipelines(Net.to(list -> Ui.later(() -> show(list)), Ui::failed));
    }

    private void show(List<PipelineDto> list) {
        page.removeAll();
        Container head = new Container(new BorderLayout());
        head.add(BorderLayout.CENTER, Desk.section("Pipelines"));
        Button add = Desk.primary("New pipeline", "pipelineNew", e -> edit(blank(null)));
        FontImage.setMaterialIcon(add, FontImage.MATERIAL_ADD);
        head.add(BorderLayout.EAST, add);
        page.add(head);
        page.add(Desk.help("Each pipeline runs every hour. A player it acts on is left alone for its cool-down, "
                + "so nobody hears the same thing twice in a row."));
        if (list.isEmpty()) {
            page.add(Desk.empty("No pipelines yet."));
        }
        for (final PipelineDto p : list) {
            page.add(card(p));
        }
        page.revalidate();
    }

    private Container card(final PipelineDto p) {
        Container c = Desk.panel();
        c.setName("pipeline-" + p.name());
        Container title = new Container(new BorderLayout());
        title.add(BorderLayout.CENTER, new Label(p.name(), "DeskPanelTitle"));
        title.add(BorderLayout.EAST, new Label(p.enabled() ? "On" : "Off", p.enabled() ? "DeskBadgeOn" : "DeskBadge"));
        c.add(title);
        c.add(Desk.field("When", new Label(Conditions.describe(p.conditions()), "DeskValue")));
        c.add(Desk.field("Then", new Label("gems".equals(p.action())
                ? "give " + p.amount() + " gems: \"" + p.title() + "\"" : "push \"" + p.title() + "\"", "DeskValue")));
        c.add(Desk.field("At most", new Label("once every " + p.cooldownDays() + " days a player", "DeskValue")));
        c.add(Desk.field("Reached", new Label(p.reached() + " players"
                + (p.lastRun() == 0 ? ", not run yet" : ", last run " + new SimpleDateFormat("MMM d, HH:mm").format(new Date(p.lastRun()))), "DeskValue")));
        c.add(Desk.actions(
                Desk.danger("Delete", "pipelineDelete-" + p.name(), e -> {
                    if (Dialog.show("Delete pipeline", "Delete \"" + p.name() + "\"?", "Delete", "Cancel")) {
                        Api.admin().deletePipeline(p.id(), Net.to(x -> Ui.later(this::refresh), Ui::failed));
                    }
                }),
                Desk.button("Edit", "pipelineEdit-" + p.name(), e -> edit(p)),
                Desk.button("Run now", "pipelineRun-" + p.name(), e -> Api.admin().runPipeline(p.id(),
                        Net.to(done -> Ui.later(() -> {
                            Ui.toast("Reached " + (done.reached() - p.reached()) + " players");
                            refresh();
                        }), Ui::failed)))));
        return c;
    }

    static PipelineDto blank(List<ConditionDto> conditions) {
        return new PipelineDto(null, "", true, conditions == null ? java.util.Collections.<ConditionDto>emptyList()
                : conditions, "push", "", "", "", 20, 14, 0, 0L);
    }

    /// The editor, in place of the list. Every field is in one aligned
    /// column; the count tells the operator who it would reach today.
    void editNext(PipelineDto start) {
        pending = start;
    }

    void edit(PipelineDto start) {
        final PipelineDto[] d = {start};
        final Conditions when = new Conditions("pipelineWhen", start.conditions());
        page.removeAll();
        page.add(Desk.section(start.id() == null ? "New pipeline" : "Edit " + start.name()));
        Container form = Desk.panel();
        form.add(Desk.field("Name", Desk.text("pipelineName", start.name(), "Win back", 40,
                v -> d[0] = with(d[0], v, null, null, null, null, null, -1, -1))));
        final Switch on = new Switch();
        on.setValue(start.enabled());
        on.setName("pipelineEnabled");
        form.add(Desk.field("Enabled", on));
        form.add(Desk.field("When", when.component()));
        form.add(Desk.field("Then", Desk.choice("pipelineAction", ACTION_NAMES,
                Conditions.index(ACTIONS, start.action()), i -> d[0] = with(d[0], null, ACTIONS[i], null, null, null,
                        null, -1, -1))));
        form.add(Desk.field("Gems", Desk.number("pipelineGems", start.amount(),
                v -> d[0] = with(d[0], null, null, null, null, null, null, v, -1))));
        form.add(Desk.field("Title", Desk.text("pipelineTitle", start.title(), "Your troops miss you", 60,
                v -> d[0] = with(d[0], null, null, v, null, null, null, -1, -1))));
        form.add(Desk.field("Message", Desk.area("pipelineBody", start.body(), 200,
                v -> d[0] = with(d[0], null, null, null, v, null, null, -1, -1))));
        form.add(Desk.field("Opens", Desk.choice("pipelineScreen", PromoteTab.SCREEN_NAMES,
                Conditions.index(PromoteTab.SCREENS, start.screen()),
                i -> d[0] = with(d[0], null, null, null, null, PromoteTab.SCREENS[i], null, -1, -1))));
        form.add(Desk.field("Cool-down", Desk.number("pipelineCooldown", start.cooldownDays(),
                v -> d[0] = with(d[0], null, null, null, null, null, null, -1, v))));
        form.add(Desk.help("Gems is used only when the pipeline gives gems. The cool-down is in days."));
        final Label count = new Label("", "DeskHelp");
        count.setName("pipelineCount");
        form.add(count);
        form.add(Desk.actions(
                Desk.button("Cancel", "pipelineCancel", e -> refresh()),
                Desk.button("Count players", "pipelineCountButton", e -> Api.admin().query(
                        new QueryDto(when.value()), Net.to(r -> Ui.later(() -> {
                            count.setText(r.count() + " players match today");
                            page.revalidate();
                        }), Ui::failed))),
                Desk.primary("Save", "pipelineSave", e -> {
                    PipelineDto p = d[0];
                    if (p.name().trim().length() == 0
                            || "push".equals(p.action()) && p.title().trim().length() == 0) {
                        Ui.toast("Give the pipeline a name, and its push a title");
                        return;
                    }
                    if (p.cooldownDays() < 1 || "gems".equals(p.action()) && p.amount() < 1) {
                        Ui.toast("Set a cool-down of at least a day, and the gems to give");
                        return;
                    }
                    PipelineDto save = new PipelineDto(p.id(), p.name(), on.isValue(), when.value(), p.action(),
                            p.title(), p.body(), p.screen(), p.amount(), p.cooldownDays(), p.reached(), p.lastRun());
                    Api.admin().savePipeline(save, Net.to(x -> Ui.later(() -> {
                        Ui.toast("Saved");
                        refresh();
                    }), Ui::failed));
                })));
        page.add(form);
        page.revalidate();
    }

    private static PipelineDto with(PipelineDto p, String name, String action, String title, String body,
            String screen, Boolean enabled, int amount, int cooldown) {
        return new PipelineDto(p.id(), name == null ? p.name() : name, enabled == null ? p.enabled() : enabled,
                p.conditions(), action == null ? p.action() : action, title == null ? p.title() : title,
                body == null ? p.body() : body, screen == null ? p.screen() : screen, amount < 0 ? p.amount() : amount,
                cooldown < 0 ? p.cooldownDays() : cooldown, p.reached(), p.lastRun());
    }
}
// end::dash-pipelines-tab[]
