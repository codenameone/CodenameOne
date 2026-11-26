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
package com.codenameone.examples.bazaar.merchant;

import com.codename1.appkit.Net;
import com.codename1.components.Switch;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.bazaar.api.AuditDto;
import com.codenameone.examples.bazaar.api.AutomationDto;
import com.codenameone.examples.bazaar.api.NodeTypeDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.DesktopActions;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.List;

/// The store's automations: trigger, condition and action flows the server
/// runs as things happen. The list shows each with how often it ran and a
/// switch to turn it on; a flow opens in the [AutomationEditor].
final class AutomationsPane extends MerchantPane {
    private List<NodeTypeDto> catalog;
    private List<AutomationDto> automations;

    AutomationsPane() {
        super("Automations", "merchant-automations");
    }

    @Override
    void load() {
        clearTools();
        tool(FontImage.MATERIAL_ADD, "New automation", "automation-new", this::create);
        tool(FontImage.MATERIAL_REFRESH, "Refresh", "automation-refresh", this::load);
        final String store = MerchantState.storeId();
        Api.automations().catalog(store, Net.to(found -> {
            catalog = found;
            paint();
        }, Ui::fail));
        Api.automations().list(store, Net.to(found -> {
            automations = found;
            paint();
        }, Ui::fail));
    }

    @Override
    void actions() {
        DesktopActions.set(DesktopActions.NEW_PRODUCT, this::create);
    }

    private void paint() {
        if (catalog == null || automations == null) {
            return;
        }
        Container page = MUi.page("automation-list");
        page.add(Ui.text(Lang.tr("When something happens in the store, a flow checks its "
                + "conditions and acts. Drag steps in, join them, then switch the flow on."),
                "SvMuted"));
        Container list = new Container(BoxLayout.y());
        list.setUIID("MeCard");
        if (automations.isEmpty()) {
            list.add(Ui.empty(FontImage.MATERIAL_ACCOUNT_TREE, "No automations yet",
                    "Start one with New automation."));
        }
        for (int iter = 0; iter < automations.size(); iter++) {
            final AutomationDto a = automations.get(iter);
            NodeTypeDto trigger = AutomationCodec.find(catalog, a.triggerKind());
            String detail = (trigger == null ? Lang.tr("No trigger") : Lang.tr(trigger.label()))
                    + Ui.DOT + Lang.tr("Ran {0} times", String.valueOf(a.runs()))
                    + (a.lastRunAt() > 0 ? Ui.DOT + Lang.tr("Last run {0}", MUi.when(a.lastRunAt()))
                            : "");
            Container end = new Container(BoxLayout.x());
            final Switch on = Ui.toggle("automation-active-" + iter, a.active(), wanted -> {
                AutomationDto changed = new AutomationDto(a.id(), a.name(), a.triggerKind(),
                        a.graphJson(), wanted, a.runs(), a.lastRunAt(), a.updatedAt());
                Api.automations().save(MerchantState.storeId(), a.id(), changed, Net.to(saved -> {
                    Ui.say(wanted ? "Automation on" : "Automation off");
                    load();
                }, (status, message) -> {
                    Ui.fail(status, message);
                    load();
                }));
            });
            end.add(on);
            Button edit = Ui.flat(FontImage.MATERIAL_EDIT, "automation-edit-" + iter, e -> open(a));
            end.add(edit);
            Container row = Ui.row(FontImage.MATERIAL_ACCOUNT_TREE, a.name(), detail, end);
            row.setName("automation-" + iter);
            list.add(row);
            com.codenameone.examples.bazaar.ui.RowMenus.attach(row,
                    com.codename1.ui.Command.create(Lang.tr("Edit"), null, e -> open(a)),
                    com.codename1.ui.Command.create(Lang.tr("Activity"), null, e -> activity(a)),
                    com.codename1.ui.Command.create(Lang.tr("Delete"), null, e -> remove(a)));
        }
        page.add(list);
        root(page);
    }

    private void create() {
        if (catalog == null) {
            return;
        }
        open(new AutomationDto(null, Lang.tr("New automation"), null, "", false, 0, 0, 0));
    }

    private void open(AutomationDto a) {
        AutomationEditor editor = new AutomationEditor(this, a, catalog);
        push(a.name() == null ? "Automation" : a.name(), editor);
    }

    void saved() {
        pop();
        load();
    }

    void remove(final AutomationDto a) {
        if (a.id() == null || !MUi.confirm("Delete automation", "This flow stops running and is "
                + "gone for good.", "Delete")) {
            return;
        }
        Api.automations().remove(MerchantState.storeId(), a.id(), Net.to(left -> {
            automations = left;
            popAll();
            paint();
        }, Ui::fail));
    }

    void activity(AutomationDto a) {
        if (a.id() == null) {
            Ui.say("Save the automation first");
            return;
        }
        final Container page = MUi.page("automation-activity");
        push("Activity", page);
        Api.automations().activity(MerchantState.storeId(), a.id(), Net.to(found -> {
            fillActivity(page, found);
        }, Ui::fail));
    }

    private static void fillActivity(Container page, List<AuditDto> found) {
        page.removeAll();
        Container card = new Container(BoxLayout.y());
        card.setUIID("MeCard");
        if (found == null || found.isEmpty()) {
            card.add(Ui.empty(FontImage.MATERIAL_HISTORY, "Nothing yet",
                    "Each run of the flow is written down here."));
        }
        for (int iter = 0; found != null && iter < found.size(); iter++) {
            AuditDto a = found.get(iter);
            String detail = (a.detail() == null ? "" : a.detail()) + Ui.DOT + MUi.when(a.createdAt());
            card.add(Ui.row(FontImage.MATERIAL_BOLT, a.subject() == null ? a.action() : a.subject(),
                    detail, null));
        }
        page.add(card);
        Ui.relayout(page);
    }
}
