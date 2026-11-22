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

import com.codename1.kanban.DefaultKanbanModel;
import com.codename1.kanban.KanbanCard;
import com.codename1.kanban.KanbanColumn;
import com.codenameone.examples.bazaar.api.PipelineCardDto;
import com.codenameone.examples.bazaar.api.PipelineColumnDto;
import com.codenameone.examples.bazaar.ops.SalesPipeline;
import java.util.ArrayList;
import java.util.List;

/// The sales pipeline board's logic, apart from any component so it runs in
/// a unit test: the Kanban model, the words on a card, and the purchase-intent
/// explanation.
///
/// Text goes through a [Words] so the pane can translate it (`Lang.tr`) and a
/// test can read the English keys as they are.
final class Pipelines {
    /// Translates a message key with `{0}` arguments, and formats money.
    interface Words {
        String tr(String key, String... args);

        String money(long cents, String currency);
    }

    /// The English keys as they are, money as `12.34 USD`.
    static final Words PLAIN = new Words() {
        @Override
        public String tr(String key, String... args) {
            String out = key;
            for (int iter = 0; args != null && iter < args.length; iter++) {
                out = replace(out, "{" + iter + "}", args[iter]);
            }
            return out;
        }

        @Override
        public String money(long cents, String currency) {
            long part = Math.abs(cents % 100);
            return cents / 100 + "." + (part < 10 ? "0" : "") + part + " " + currency;
        }
    };

    private Pipelines() {
    }

    static String replace(String s, String what, String with) {
        StringBuilder out = new StringBuilder();
        int from = 0;
        int at = s.indexOf(what);
        while (at >= 0) {
            out.append(s.substring(from, at)).append(with);
            from = at + what.length();
            at = s.indexOf(what, from);
        }
        return out.append(s.substring(from)).toString();
    }

    /// The server's columns in board order, the unknown ones last.
    static List<PipelineColumnDto> ordered(List<PipelineColumnDto> columns) {
        List<PipelineColumnDto> out = new ArrayList<PipelineColumnDto>();
        if (columns == null) {
            return out;
        }
        String[] ids = SalesPipeline.columns();
        for (int iter = 0; iter < ids.length; iter++) {
            for (int at = 0; at < columns.size(); at++) {
                if (ids[iter].equals(columns.get(at).id())) {
                    out.add(columns.get(at));
                }
            }
        }
        for (int at = 0; at < columns.size(); at++) {
            if (SalesPipeline.indexOf(columns.get(at).id()) < 0) {
                out.add(columns.get(at));
            }
        }
        return out;
    }

    /// The board as a Kanban model; each card's user data is its DTO.
    static DefaultKanbanModel model(List<PipelineColumnDto> columns, long now, Words w) {
        DefaultKanbanModel model = new DefaultKanbanModel();
        List<PipelineColumnDto> ordered = ordered(columns);
        for (int iter = 0; iter < ordered.size(); iter++) {
            PipelineColumnDto c = ordered.get(iter);
            model.addColumn(new KanbanColumn(c.id(), columnTitle(c, w)));
            for (int at = 0; c.cards() != null && at < c.cards().size(); at++) {
                model.addCard(c.id(), card(c.cards().get(at), now, w));
            }
        }
        return model;
    }

    /// A column's title with how many cards it has in all.
    static String columnTitle(PipelineColumnDto c, Words w) {
        String title = w.tr(c.title() == null ? SalesPipeline.title(c.id()) : c.title());
        return title + " " + c.total();
    }

    static KanbanCard card(PipelineCardDto p, long now, Words w) {
        KanbanCard card = new KanbanCard(p.id(), title(p, w));
        card.setDescription(summary(p, now, w));
        if (p.intent() >= 0) {
            card.setBadge(w.tr(bandKey(p.intent()), String.valueOf(p.intent())));
            card.setStyleKey("intent-" + SalesPipeline.band(p.intent()));
            List<String> why = explain(p, w);
            for (int iter = 0; iter < why.size() && iter < 2; iter++) {
                card.addTag(why.get(iter));
            }
        } else {
            card.setBadge(Fulfil.age(p.createdAt(), now));
        }
        card.setUserData(p);
        return card;
    }

    /// `#1042 name` for an order, the shopper (or Guest) for a cart.
    static String title(PipelineCardDto p, Words w) {
        String who = p.customer() == null || p.customer().length() == 0 ? w.tr("Guest")
                : p.customer();
        return SalesPipeline.ORDER.equals(p.kind()) ? "#" + p.number() + "  " + who : who;
    }

    /// The value and how long the card has been around: since the cart was
    /// last touched, or since the order came in.
    static String summary(PipelineCardDto p, long now, Words w) {
        long since = SalesPipeline.CART.equals(p.kind()) ? p.updatedAt() : p.createdAt();
        return w.money(p.valueCents(), p.currency()) + "  " + Fulfil.age(since, now);
    }

    /// The key of the intent badge for a score.
    static String bandKey(int score) {
        String band = SalesPipeline.band(score);
        return "high".equals(band) ? "High intent {0}" : "medium".equals(band)
                ? "Medium intent {0}" : "Low intent {0}";
    }

    /// The intent score in plain words, a sentence per reason, the reason
    /// that counted most first. Unknown reason codes are left out.
    static List<String> explain(PipelineCardDto p, Words w) {
        List<String> out = new ArrayList<String>();
        for (int iter = 0; p.reasons() != null && iter < p.reasons().size(); iter++) {
            String reason = p.reasons().get(iter);
            String code = SalesPipeline.reasonCode(reason);
            long n = SalesPipeline.reasonValue(reason);
            String text = null;
            if ("views".equals(code)) {
                text = n == 1 ? w.tr("Viewed a product once")
                        : w.tr("Viewed products {0} times", String.valueOf(n));
            } else if ("visits".equals(code)) {
                text = w.tr("Came back: {0} visits", String.valueOf(n));
            } else if ("minutes".equals(code)) {
                text = w.tr("{0} min on product pages", String.valueOf(n));
            } else if ("value".equals(code)) {
                text = w.tr("Cart worth {0}", w.money(n, p.currency()));
            } else if ("returning".equals(code)) {
                text = n == 1 ? w.tr("Returning customer, 1 order before")
                        : w.tr("Returning customer, {0} orders before", String.valueOf(n));
            }
            if (text != null) {
                out.add(text);
            }
        }
        return out;
    }

    /// The label of the button that moves a card on, or null when it stays.
    static String moveLabel(String from) {
        String to = SalesPipeline.next(from);
        if (to == null) {
            return null;
        }
        if (SalesPipeline.REMINDED.equals(to)) {
            return "Send reminder";
        }
        if (SalesPipeline.FULFILLING.equals(to)) {
            return "Pack";
        }
        if (SalesPipeline.SHIPPED.equals(to)) {
            return "Ship";
        }
        return "Mark delivered";
    }
}
