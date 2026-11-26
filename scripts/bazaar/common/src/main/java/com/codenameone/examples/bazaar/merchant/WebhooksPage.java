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
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.spinner.Picker;
import com.codenameone.examples.bazaar.api.WebhookDeliveryDto;
import com.codenameone.examples.bazaar.api.WebhookDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.RowMenus;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// Webhooks, from the store's settings: the URLs the server posts a signed
/// event to, which event each hears, and the log of every delivery with a
/// button to send one again or send a test.
final class WebhooksPage {
    private final MerchantPane host;
    private final Container page = MUi.page("webhooks-page");
    private List<String> topics = new ArrayList<String>();

    WebhooksPage(MerchantPane host) {
        this.host = host;
    }

    void show() {
        host.push("Webhooks", page);
        Api.webhooks().topics(MerchantState.storeId(), Net.to(found -> {
            topics = found == null ? new ArrayList<String>() : found;
        }, (status, message) -> { }));
        load();
    }

    private void load() {
        Api.webhooks().list(MerchantState.storeId(), Net.to(this::paint, Ui::fail));
    }

    private void paint(List<WebhookDto> hooks) {
        page.removeAll();
        page.add(Ui.text(Lang.tr("Bazaar posts each event as JSON, signed with the webhook's "
                + "secret in the X-Bazaar-Signature header."), "SvMuted"));
        page.add(MUi.buttons(Ui.primary(Lang.tr("Add webhook"), "webhook-add",
                e -> edit(null))));
        Container list = new Container(BoxLayout.y());
        list.setUIID("MeCard");
        if (hooks == null || hooks.isEmpty()) {
            list.add(Ui.empty(FontImage.MATERIAL_WEBHOOK, "No webhooks",
                    "Add one to tell another system when something happens."));
        }
        for (int iter = 0; hooks != null && iter < hooks.size(); iter++) {
            final WebhookDto w = hooks.get(iter);
            Container end = new Container(BoxLayout.x());
            Switch on = Ui.toggle("webhook-active-" + iter, w.active(), wanted -> save(
                    new WebhookDto(w.id(), w.topic(), w.url(), w.secret(), wanted, w.createdAt())));
            end.add(on);
            end.add(Ui.flat(FontImage.MATERIAL_HISTORY, "webhook-open-" + iter,
                    e -> deliveries(w)));
            end.add(Ui.flat(FontImage.MATERIAL_EDIT, "webhook-edit-" + iter, e -> edit(w)));
            Container row = Ui.row(FontImage.MATERIAL_WEBHOOK, w.topic(), w.url(), end);
            row.setName("webhook-" + iter);
            list.add(row);
            RowMenus.attach(row,
                    Command.create(Lang.tr("Deliveries"), null, e -> deliveries(w)),
                    Command.create(Lang.tr("Send test"), null, e -> test(w)),
                    Command.create(Lang.tr("Edit"), null, e -> edit(w)),
                    Command.create(Lang.tr("Delete"), null, e -> remove(w)));
        }
        page.add(list);
        Ui.relayout(page);
    }

    private void edit(final WebhookDto w) {
        Container body = new Container(BoxLayout.y());
        String[] codes = topics.isEmpty() ? new String[] {w == null ? "order.created" : w.topic()}
                : topics.toArray(new String[topics.size()]);
        Picker topic = new Picker();
        topic.setType(com.codename1.ui.Display.PICKER_TYPE_STRINGS);
        topic.setUIID("SvField");
        topic.setName("webhook-topic");
        topic.setStrings(codes);
        int at = 0;
        for (int iter = 0; w != null && iter < codes.length; iter++) {
            if (codes[iter].equals(w.topic())) {
                at = iter;
            }
        }
        topic.setSelectedStringIndex(at);
        body.add(Ui.label("Event", "SvLabel")).add(topic);
        TextField url = MUi.text(body, "URL", "webhook-url", w == null ? "https://" : w.url());
        TextField secret = MUi.text(body, "Signing secret", "webhook-secret",
                w == null ? "" : w.secret());
        body.add(Ui.text(Lang.tr("Leave the secret empty and the server makes one."), "SvMuted"));
        Command ok = new com.codename1.ui.Command(Lang.tr("Save"));
        Command cancel = new com.codename1.ui.Command(Lang.tr("Cancel"));
        if (Dialog.show(Lang.tr(w == null ? "Add webhook" : "Edit webhook"), body, cancel, ok)
                != ok) {
            return;
        }
        String u = url.getText().trim();
        if (!u.regionMatches(true, 0, "https://", 0, 8) && !u.regionMatches(true, 0, "http://", 0,
                7)) {
            Ui.say("The URL must start with https://");
            return;
        }
        String s = secret.getText().trim();
        save(new WebhookDto(w == null ? null : w.id(), codes[Math.max(0,
                topic.getSelectedStringIndex())], u, s.length() == 0 ? null : s,
                w == null || w.active(), w == null ? 0 : w.createdAt()));
    }

    private void save(WebhookDto w) {
        if (w.id() == null) {
            Api.webhooks().create(MerchantState.storeId(), w, Net.to(saved -> {
                Ui.say("Saved");
                load();
            }, Ui::fail));
        } else {
            Api.webhooks().save(MerchantState.storeId(), w.id(), w, Net.to(saved -> load(),
                    Ui::fail));
        }
    }

    private void remove(WebhookDto w) {
        if (!MUi.confirm("Delete webhook", "Events stop going to this URL.", "Delete")) {
            return;
        }
        Api.webhooks().remove(MerchantState.storeId(), w.id(), Net.to(this::paint, Ui::fail));
    }

    private void test(WebhookDto w) {
        Api.webhooks().test(MerchantState.storeId(), w.id(), Net.to(d -> Ui.say(
                Lang.tr("Test sent: {0}", Lang.tr(Words.of(d.status())))), Ui::fail));
    }

    private void deliveries(final WebhookDto w) {
        final Container log = MUi.page("webhook-deliveries");
        host.push("Deliveries", log);
        reload(w, log);
    }

    private void reload(final WebhookDto w, final Container log) {
        Api.webhooks().deliveries(MerchantState.storeId(), w.id(), Net.to(found -> {
            log.removeAll();
            log.add(MUi.buttons(
                    Ui.secondary(Lang.tr("Send test"), "webhook-test", e -> {
                        test(w);
                        reload(w, log);
                    }),
                    Ui.secondary(Lang.tr("Refresh"), "webhook-refresh", e -> reload(w, log))));
            Container card = new Container(BoxLayout.y());
            card.setUIID("MeCard");
            if (found == null || found.isEmpty()) {
                card.add(Ui.text(Lang.tr("Nothing has been sent yet."), "SvMuted"));
            }
            for (int iter = 0; found != null && iter < found.size(); iter++) {
                final WebhookDeliveryDto d = found.get(iter);
                String detail = d.topic() + Ui.DOT + Lang.tr("{0} attempts",
                        String.valueOf(d.attempts())) + (d.responseCode() > 0 ? Ui.DOT + "HTTP "
                        + d.responseCode() : "") + Ui.DOT + MUi.when(d.createdAt());
                Container end = new Container(BoxLayout.y());
                end.add(MUi.badge(d.status()));
                end.add(Ui.link(Lang.tr("Send again"), "redeliver-" + iter,
                        e -> Api.webhooks().redeliver(MerchantState.storeId(), d.id(),
                                Net.to(again -> reload(w, log), Ui::fail))));
                end.add(Ui.link(Lang.tr("Payload"), "payload-" + iter, e -> payload(d)));
                card.add(Ui.row(FontImage.MATERIAL_SEND, d.id(), detail, end));
            }
            log.add(card);
            Ui.relayout(log);
        }, Ui::fail));
    }

    private static void payload(WebhookDeliveryDto d) {
        com.codename1.ui.TextArea text = new com.codename1.ui.TextArea(
                d.payload() == null ? "" : d.payload(), 12, 40);
        text.setEditable(false);
        text.setUIID("MeCode");
        Dialog.show(Lang.tr("Payload"), text, new com.codename1.ui.Command(Lang.tr("Close")));
    }
}
