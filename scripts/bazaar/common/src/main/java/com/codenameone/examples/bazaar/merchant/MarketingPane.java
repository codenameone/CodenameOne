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
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.spinner.Picker;
import com.codename1.ui.util.UITimer;
import com.codenameone.examples.bazaar.api.AudienceDto;
import com.codenameone.examples.bazaar.api.CampaignDto;
import com.codenameone.examples.bazaar.api.ScheduleDto;
import com.codenameone.examples.bazaar.api.TestSendDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.List;

/// E-mail marketing: the store's segments and campaigns.
///
/// A segment is built from rule rows -- field, operator, value -- joined by
/// AND or OR; every change asks the server for the live count, debounced so
/// typing does not send a request per key. A campaign picks a segment, may
/// carry a second subject for an A/B split, sends a test copy, and is
/// scheduled for a day and time.
final class MarketingPane extends MerchantPane {
    private List<AudienceDto> segments;
    private List<CampaignDto> campaigns;

    MarketingPane() {
        super("Campaigns", "merchant-marketing");
    }

    @Override
    void load() {
        clearTools();
        tool(FontImage.MATERIAL_FILTER_ALT, "New segment", "segment-new", () -> segment(null));
        tool(FontImage.MATERIAL_EMAIL, "New campaign", "campaign-new", () -> campaign(null));
        segments = null;
        campaigns = null;
        Api.marketing().segments(MerchantState.storeId(), Net.to(found -> {
            segments = found;
            paint();
        }, Ui::fail));
        Api.marketing().campaigns(MerchantState.storeId(), Net.to(found -> {
            campaigns = found;
            paint();
        }, Ui::fail));
    }

    private void paint() {
        if (segments == null || campaigns == null) {
            return;
        }
        Container page = MUi.page("marketing-page");
        Container segs = new Container(BoxLayout.y());
        segs.setUIID("MeCard");
        segs.add(Ui.plain(Lang.tr("Segments"), "SvRowTitle"));
        for (int iter = 0; iter < segments.size(); iter++) {
            final AudienceDto a = segments.get(iter);
            Container row = Ui.row(FontImage.MATERIAL_FILTER_ALT, a.name(),
                    Lang.tr("{0} customers", a.count()),
                    Ui.flat(FontImage.MATERIAL_EDIT, "segment-edit-" + iter, e -> segment(a)));
            segs.add(row);
        }
        page.add(segs);
        Container camps = new Container(BoxLayout.y());
        camps.setUIID("MeCard");
        camps.add(Ui.plain(Lang.tr("Campaigns"), "SvRowTitle"));
        for (int iter = 0; iter < campaigns.size(); iter++) {
            final CampaignDto c = campaigns.get(iter);
            String detail = Lang.tr(Words.of(c.status()))
                    + (c.scheduledAt() > 0 ? Ui.DOT + MUi.when(c.scheduledAt()) : "");
            camps.add(Ui.row(FontImage.MATERIAL_EMAIL, c.name(), detail,
                    Ui.flat(FontImage.MATERIAL_EDIT, "campaign-edit-" + iter, e -> campaign(c))));
        }
        page.add(camps);
        root(page);
    }

    // --- Segment builder ---

    private void segment(final AudienceDto a) {
        final SegmentRules rules = a == null ? new SegmentRules() : SegmentRules.parse(a.rules());
        if (rules.rules.isEmpty()) {
            rules.rules.add(SegmentRules.row(SegmentRules.FIELDS[0]));
        }
        final Container page = MUi.page("segment-builder");
        final Container card = new Container(BoxLayout.y());
        card.setUIID("MeCard");
        final TextField name = MUi.text(card, "Name", "segment-name", a == null ? "" : a.name());
        final Picker join = MUi.picker(card, "Customers matching", "segment-join",
                new String[] {SegmentRules.AND, SegmentRules.OR}, rules.op);
        final Label count = Ui.plain("", "SvRowTitle");
        count.setName("segment-count");
        final Container rows = new Container(BoxLayout.y());
        final UITimer[] debounce = new UITimer[1];
        final Runnable[] recount = new Runnable[1];
        recount[0] = () -> {
            rules.op = MUi.code(join, new String[] {SegmentRules.AND, SegmentRules.OR});
            if (debounce[0] != null) {
                debounce[0].cancel();
            }
            String why = rules.problem();
            if (why != null) {
                count.setText(Lang.tr(why));
                return;
            }
            final Runnable ask = () -> {
                AudienceDto probe = new AudienceDto(null, "", rules.toJson(), 0);
                Api.marketing().preview(MerchantState.storeId(), probe, Net.to(p -> {
                    count.setText(Lang.tr("{0} customers match", p.count()));
                    if (count.getParent() != null) {
                        Ui.relayout(count.getParent());
                    }
                }, (status, message) -> count.setText(message)));
            };
            if (page.getComponentForm() == null) {
                ask.run();
            } else {
                debounce[0] = UITimer.timer(400, false, page.getComponentForm(), ask);
            }
        };
        join.addActionListener(e -> recount[0].run());
        paintRules(rules, rows, recount[0]);
        card.add(rows);
        card.add(Ui.secondary(Lang.tr("Add a rule"), "segment-add-rule", e -> {
            rules.rules.add(SegmentRules.row(SegmentRules.FIELDS[0]));
            paintRules(rules, rows, recount[0]);
            recount[0].run();
        }));
        card.add(count);
        page.add(card);
        page.add(MUi.buttons(Ui.primary(Lang.tr("Save"), "segment-save", e -> {
            String why = rules.problem();
            if (why != null) {
                Ui.say(why);
                return;
            }
            AudienceDto wanted = new AudienceDto(a == null ? null : a.id(), name.getText().trim(),
                    rules.toJson(), 0);
            Net.Ok<AudienceDto> done = saved -> {
                Ui.say("Saved");
                pop();
                load();
            };
            if (a == null) {
                Api.marketing().createSegment(MerchantState.storeId(), wanted, Net.to(done, Ui::fail));
            } else {
                Api.marketing().saveSegment(MerchantState.storeId(), a.id(), wanted,
                        Net.to(done, Ui::fail));
            }
        })));
        push(a == null ? "New segment" : a.name(), page);
        recount[0].run();
    }

    private void paintRules(final SegmentRules rules, final Container rows, final Runnable changed) {
        rows.removeAll();
        for (int iter = 0; iter < rules.rules.size(); iter++) {
            final SegmentRules.Rule r = rules.rules.get(iter);
            final int at = iter;
            Container line = new Container(BoxLayout.x());
            line.setName("segment-rule-" + iter);
            if (r.isGroup()) {
                line.add(Ui.plain(Lang.tr("A nested group (kept as it is)"), "SvMuted"));
            } else {
                final Picker field = MUi.picker(null, null, "segment-field-" + iter,
                        SegmentRules.FIELDS, r.field);
                final Picker op = MUi.picker(null, null, "segment-op-" + iter,
                        SegmentRules.opsFor(r.field), r.op);
                final TextField value = new TextField(r.value == null ? "" : r.value);
                value.setUIID("SvField");
                value.setName("segment-value-" + iter);
                field.addActionListener(e -> {
                    SegmentRules.Rule fresh = SegmentRules.row(MUi.code(field, SegmentRules.FIELDS));
                    rules.rules.set(at, fresh);
                    paintRules(rules, rows, changed);
                    changed.run();
                });
                op.addActionListener(e -> {
                    r.op = MUi.code(op, SegmentRules.opsFor(r.field));
                    changed.run();
                });
                value.addDataChangedListener((type, index) -> {
                    r.value = value.getText().trim();
                    changed.run();
                });
                line.add(field);
                line.add(op);
                line.add(value);
            }
            Button remove = Ui.flat(FontImage.MATERIAL_CLOSE, "segment-remove-" + iter, e -> {
                rules.rules.remove(at);
                paintRules(rules, rows, changed);
                changed.run();
            });
            line.add(remove);
            rows.add(line);
        }
        Ui.relayout(rows);
    }

    // --- Campaign editor ---

    private void campaign(final CampaignDto c) {
        if (segments == null || segments.isEmpty()) {
            Ui.say("Make a segment first: a campaign goes to one");
            return;
        }
        Container page = MUi.page("campaign-editor");
        Container card = new Container(BoxLayout.y());
        card.setUIID("MeCard");
        final TextField name = MUi.text(card, "Name", "campaign-name", c == null ? "" : c.name());
        final String[] ids = new String[segments.size()];
        final String[] labels = new String[segments.size()];
        for (int iter = 0; iter < ids.length; iter++) {
            ids[iter] = segments.get(iter).id();
            labels[iter] = segments.get(iter).name();
        }
        final Picker audience = MUi.picker(card, "Send to", "campaign-segment", ids,
                c == null ? ids[0] : c.audienceId());
        audience.setStrings(labels);
        audience.setSelectedStringIndex(Math.max(0, indexOf(ids, c == null ? null : c.audienceId())));
        final TextField subjectA = MUi.text(card, "Subject", "campaign-subject-a",
                c == null ? "" : c.subjectA());
        final TextField subjectB = MUi.text(card, "Second subject for an A/B test (optional)",
                "campaign-subject-b", c == null || c.subjectB() == null ? "" : c.subjectB());
        final TextField split = MUi.number(card, "Percent who get the test before a winner",
                "campaign-ab", c == null ? 20 : c.abPercent());
        final TextField winner = MUi.number(card, "Pick the winner after (minutes)",
                "campaign-winner", c == null ? 60 : c.winnerAfterMinutes());
        final TextField body = MUi.text(card, "Message", "campaign-body", c == null ? "" : c.body());
        page.add(card);

        final Runnable[] save = new Runnable[1];
        final Net.Ok<CampaignDto> done = saved -> {
            Ui.say("Saved");
            pop();
            load();
        };
        save[0] = () -> {
            int at = Math.max(0, audience.getSelectedStringIndex());
            CampaignDto wanted = new CampaignDto(c == null ? null : c.id(), name.getText().trim(),
                    c == null ? "EMAIL" : c.channel(), ids[at], subjectA.getText().trim(),
                    subjectB.getText().trim(), body.getText(), MUi.integer(split),
                    MUi.integer(winner), c == null ? "DRAFT" : c.status(),
                    c == null ? 0 : c.scheduledAt(), c == null ? null : c.winner(),
                    c == null ? "#0ea5e9" : c.colour(), c == null ? null : c.links());
            if (c == null) {
                Api.marketing().createCampaign(MerchantState.storeId(), wanted,
                        Net.to(done, Ui::fail));
            } else {
                Api.marketing().saveCampaign(MerchantState.storeId(), c.id(), wanted,
                        Net.to(done, Ui::fail));
            }
        };
        Container actions = new Container(BoxLayout.y());
        actions.setUIID("MeCard");
        if (c != null && c.id() != null) {
            final TextField to = MUi.text(actions, "Send a test to", "campaign-test-to", "");
            actions.add(Ui.secondary(Lang.tr("Send test"), "campaign-send-test", e ->
                    Api.marketing().sendTest(MerchantState.storeId(), c.id(),
                            new TestSendDto(to.getText().trim()), Net.to(x ->
                                    Ui.say("Test sent"), Ui::fail))));
            final TextField day = MUi.text(actions, "Send on (yyyy-mm-dd hh:mm)", "campaign-when",
                    "");
            actions.add(Ui.secondary(Lang.tr("Schedule"), "campaign-schedule", e -> {
                long at = parseWhen(day.getText().trim(), java.util.TimeZone.getDefault());
                if (at <= System.currentTimeMillis()) {
                    Ui.say("Schedule it in the future");
                    return;
                }
                Api.marketing().schedule(MerchantState.storeId(), c.id(), new ScheduleDto(at),
                        Net.to(done, Ui::fail));
            }));
            page.add(actions);
        }
        page.add(MUi.buttons(Ui.primary(Lang.tr("Save"), "campaign-save", e -> save[0].run())));
        push(c == null ? "New campaign" : c.name(), page);
    }

    private static int indexOf(String[] all, String v) {
        for (int iter = 0; iter < all.length; iter++) {
            if (all[iter].equals(v)) {
                return iter;
            }
        }
        return -1;
    }

    /// `yyyy-mm-dd hh:mm` (the time optional, 09:00 by default) in `zone`,
    /// or 0 when it does not read as one.
    static long parseWhen(String text, java.util.TimeZone zone) {
        if (text == null || text.length() < 10 || text.charAt(4) != '-' || text.charAt(7) != '-') {
            return 0;
        }
        String y = text.substring(0, 4);
        String m = text.substring(5, 7);
        String d = text.substring(8, 10);
        String hh = "09";
        String mm = "00";
        if (text.length() >= 16 && text.charAt(13) == ':') {
            hh = text.substring(11, 13);
            mm = text.substring(14, 16);
        } else if (text.length() != 10) {
            return 0;
        }
        if (!SegmentRules.whole(y) || !SegmentRules.whole(m) || !SegmentRules.whole(d)
                || !SegmentRules.whole(hh) || !SegmentRules.whole(mm)) {
            return 0;
        }
        java.util.Calendar c = java.util.Calendar.getInstance(zone);
        c.set(java.util.Calendar.YEAR, Integer.parseInt(y));
        c.set(java.util.Calendar.MONTH, Integer.parseInt(m) - 1);
        c.set(java.util.Calendar.DAY_OF_MONTH, Integer.parseInt(d));
        c.set(java.util.Calendar.HOUR_OF_DAY, Integer.parseInt(hh));
        c.set(java.util.Calendar.MINUTE, Integer.parseInt(mm));
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        return c.getTime().getTime();
    }
}
