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
package com.codenameone.examples.reel.moderation;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.reel.Telemetry;
import com.codenameone.examples.reel.api.BlocklistEntryDto;
import com.codenameone.examples.reel.api.ModerationDecisionDto;
import com.codenameone.examples.reel.api.ModerationEventDto;
import com.codenameone.examples.reel.api.ModerationItemDto;
import com.codenameone.examples.reel.net.Api;
import com.codenameone.examples.reel.net.Net;
import com.codenameone.examples.reel.ui.Art;
import com.codenameone.examples.reel.ui.Lang;
import com.codenameone.examples.reel.ui.Nav;
import com.codenameone.examples.reel.ui.SettingsForm;
import com.codenameone.examples.reel.ui.Shell;
import com.codenameone.examples.reel.ui.Ui;

/// The moderator's home: the open queue by kind, each item with the signals
/// that raised it, and a decision that needs a note when it goes against the
/// creator. Also the blocklist the automatic checks match against, and any
/// account's moderation record.
public final class ModerationForm implements Nav.Home {
    static final String[] KINDS = {"", "TITLE", "VIDEO", "REPORT", "APPEAL", "KEYWORD", "FRAME"};
    private final Shell shell = new Shell("Moderation", Nav.MODERATION);
    private final Container page = Ui.list("queue");
    private String kind = "";

    @Override
    public void start() {
        final Form form = shell.form();
        shell.menu().item(FontImage.MATERIAL_INBOX, "Queue", "queueMenu", this::load)
                .item(FontImage.MATERIAL_BLOCK, "Blocklist", "blocklist", this::blocklist)
                .item(FontImage.MATERIAL_HISTORY, "Account record", "record", this::record)
                .item(FontImage.MATERIAL_SETTINGS, "Settings", "settings", () -> SettingsForm.show(form));
        Ui.pull(page, this::load);
        shell.done();
        load();
    }

    @Override
    public Form form() {
        return shell.form();
    }

    void load() {
        final Form form = shell.form();
        Container whole = new Container(new BorderLayout());
        String[] labels = new String[KINDS.length];
        for (int iter = 0; iter < KINDS.length; iter++) {
            labels[iter] = KINDS[iter].length() == 0 ? Lang.tr("All") : Lang.tr(KINDS[iter]);
        }
        whole.add(BorderLayout.NORTH, Ui.choice("kind", labels, KINDS, kind, v -> {
            kind = v;
            load();
        }, 4));
        whole.add(BorderLayout.CENTER, page);
        shell.show("Moderation", whole);
        Api.moderation().queue(kind, Net.to(items -> {
            if (!Nav.isHome(form)) {
                return;
            }
            page.removeAll();
            if (items == null || items.isEmpty()) {
                page.add(Ui.empty(FontImage.MATERIAL_DONE_ALL, "The queue is empty", null));
            }
            for (int iter = 0; items != null && iter < items.size(); iter++) {
                final ModerationItemDto it = items.get(iter);
                page.add(Ui.tap(Ui.row(icon(it.kind()), it.subjectName(), Lang.tr(it.kind()) + Ui.DOT + it.reason()
                        + Ui.DOT + Ui.when(it.createdAt()), Ui.badge(Ui.tenths(it.score()), it.score() >= 0.8
                        ? "SvBadgeBad" : "SvBadge")), "item-" + iter, e -> item(form, it)));
            }
            page.revalidate();
        }, Ui.failed(page, this::load)));
    }

    static char icon(String kind) {
        return "REPORT".equals(kind) ? FontImage.MATERIAL_FLAG : "APPEAL".equals(kind) ? FontImage.MATERIAL_GAVEL
                : "FRAME".equals(kind) ? FontImage.MATERIAL_IMAGE : "KEYWORD".equals(kind)
                ? FontImage.MATERIAL_TEXT_FIELDS : FontImage.MATERIAL_MOVIE;
    }

    private void item(final Form previous, final ModerationItemDto it) {
        final Form form = Ui.form(it.subjectName(), "ModerationItem");
        Ui.back(form, previous);
        Container p = Ui.page();
        if (it.thumbnailUrl() != null && it.thumbnailUrl().length() > 0) {
            com.codename1.ui.Label frame = new com.codename1.ui.Label("", "RlFrame");
            Art.load(frame, it.thumbnailUrl(), 60, 34);
            p.add(frame);
        }
        p.add(Ui.fact("Kind", Lang.tr(it.kind())))
                .add(Ui.fact("Owner", it.owner()))
                .add(Ui.fact("Reason", it.reason()))
                .add(Ui.fact("Score", Ui.tenths(it.score())))
                .add(Ui.fact("Raised", Ui.when(it.createdAt())));
        if (it.detail() != null && it.detail().length() > 0) {
            p.add(Ui.text(it.detail(), "SvText"));
        }
        for (int iter = 0; it.signals() != null && iter < it.signals().size(); iter++) {
            p.add(Ui.chip(it.signals().get(iter), FontImage.MATERIAL_WARNING));
        }
        final TextField note = Ui.field(p, "Note to the creator", "note", "Required to reject or remove");
        final SpanLabel error = Ui.text("", "SvError");
        error.setName("error");
        p.add(error);
        String[] actions = {"APPROVE", "REJECT", "REMOVE", "DISMISS"};
        for (int iter = 0; iter < actions.length; iter++) {
            final String action = actions[iter];
            ActionListener<ActionEvent> go = e -> {
                if (("REJECT".equals(action) || "REMOVE".equals(action)) && note.getText().trim().length() == 0) {
                    error.setText(Lang.tr("Say why, for the creator"));
                    return;
                }
                Api.moderation().decide(it.id(), new ModerationDecisionDto(action, note.getText().trim()),
                        Net.to(x -> {
                            Telemetry.moderated(action);
                            previous.showBack();
                            load();
                        }, (code, message) -> error.setText(message)));
            };
            String name = "decide-" + action;
            p.add("APPROVE".equals(action) ? Ui.primary(Lang.tr(action), name, go)
                    : "DISMISS".equals(action) ? Ui.secondary(Lang.tr(action), name, go)
                    : Ui.danger(Lang.tr(action), name, go));
        }
        p.add(Ui.link("Account record", "ownerRecord", e -> events(form, it.owner())));
        form.add(BorderLayout.CENTER, p);
        form.show();
    }

    private void blocklist() {
        final Container list = Ui.list("blocklistItems");
        Container whole = new Container(new BorderLayout());
        Container add = Ui.page();
        final String[] kind = {"KEYWORD"};
        add.add(Ui.choice("blockKind", new String[] {Lang.tr("Keyword"), Lang.tr("Frame hash")},
                new String[] {"KEYWORD", "FRAME"}, kind[0], v -> kind[0] = v));
        final TextField value = Ui.field(add, "Value", "blockValue", "A word, or a frame's hash");
        final TextField note = Ui.field(add, "Note", "blockNote", "Why");
        add.add(Ui.secondary("Block", "block", e -> {
            if (value.getText().trim().length() == 0) {
                return;
            }
            Api.moderation().block(new BlocklistEntryDto(null, kind[0], value.getText().trim(),
                    note.getText().trim(), 0), Net.to(x -> blocklist(), Ui::fail));
        }));
        whole.add(BorderLayout.NORTH, add);
        whole.add(BorderLayout.CENTER, list);
        shell.show("Blocklist", whole);
        Api.moderation().blocklist(Net.to(entries -> {
            list.removeAll();
            for (int iter = 0; entries != null && iter < entries.size(); iter++) {
                final BlocklistEntryDto b = entries.get(iter);
                list.add(Ui.row(FontImage.MATERIAL_BLOCK, b.value(), Lang.tr(b.kind()) + Ui.DOT + b.note(),
                        Ui.flat(FontImage.MATERIAL_DELETE, "unblock-" + iter, e -> Api.moderation().unblock(b.id(),
                                Net.to(x -> blocklist(), Ui::fail)))));
            }
            list.revalidate();
        }, Ui.failed(list, this::blocklist)));
    }

    private void record() {
        Container p = Ui.page();
        final TextField who = Ui.field(p, "Username", "recordUser", "someone@example.com");
        p.add(Ui.primary("Show", "showRecord", e -> {
            if (who.getText().trim().length() > 0) {
                events(shell.form(), who.getText().trim());
            }
        }));
        shell.show("Account record", p);
    }

    public static void events(Form previous, final String username) {
        final Form form = Ui.form(username, "ModerationEvents");
        Ui.back(form, previous);
        final Container list = Ui.list("events");
        form.add(BorderLayout.CENTER, list);
        form.show();
        Api.moderation().events(username, Net.to(events -> {
            if (events == null || events.isEmpty()) {
                list.add(Ui.empty(FontImage.MATERIAL_VERIFIED_USER, "A clean record", null));
            }
            for (int iter = 0; events != null && iter < events.size(); iter++) {
                ModerationEventDto ev = events.get(iter);
                list.add(Ui.row(FontImage.MATERIAL_HISTORY, Lang.tr(ev.action()), Ui.when(ev.at()) + Ui.DOT + ev.by()
                        + (ev.reason() == null || ev.reason().length() == 0 ? "" : Ui.DOT + ev.reason()), null));
            }
            list.revalidate();
        }, Ui.failed(list, () -> events(previous, username))));
    }
}
