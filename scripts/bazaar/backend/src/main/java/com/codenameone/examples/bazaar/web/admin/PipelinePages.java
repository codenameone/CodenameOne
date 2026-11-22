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

package com.codenameone.examples.bazaar.web.admin;

import com.codename1.backend.HttpServer;
import com.codename1.backend.annotations.Controller;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.appkit.server.Caller;
import com.codename1.backend.security.SecurityExchange;
import com.codename1.backend.mvc.Model;
import com.codenameone.examples.bazaar.api.PipelineCardDto;
import com.codenameone.examples.bazaar.api.PipelineColumnDto;
import com.codenameone.examples.bazaar.ops.SalesPipeline;
import com.codenameone.examples.bazaar.pipeline.Pipeline;
import com.codenameone.examples.bazaar.web.Money;
import com.codenameone.examples.bazaar.web.Shell;
import com.codenameone.examples.bazaar.web.WebPages;
import com.codenameone.examples.bazaar.web.table.DataTable;
import com.codenameone.examples.bazaar.web.security.PageEtags;
import java.util.List;

/// `/admin/{storeId}/pipeline`: the sales pipeline in the back office, read
/// only. The same columns as the app's board -- carts by how far they got,
/// orders by where fulfilment has them, open returns -- with each cart's
/// purchase-intent score and its reasons in words. Moving a card is the
/// app's, or the fulfilment board's here; this page only shows where things
/// stand, so it is plain HTML with no script of its own.
@Controller
public class PipelinePages {
    private final WebPages pages;
    private final Pipeline pipeline;

    public PipelinePages(WebPages pages, Pipeline pipeline) {
        this.pages = pages;
        this.pipeline = pipeline;
    }

    @GetMapping("/admin/{storeId}/pipeline")
    public String board(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        Shell shell = pages.admin(request, storeId, "Sales pipeline");
        SecurityExchange.current().setAttribute(PageEtags.NO_STORE, "1");
        List<PipelineColumnDto> cols = pipeline.board(Caller.name(), storeId, null, 0);
        long now = System.currentTimeMillis();
        MarketingView v = new MarketingView();
        v.page = "pipeline";
        v.base = "/admin/" + storeId;
        v.heading = "Sales pipeline";
        v.intro = "Every open cart and order, from browsing to returns. Carts are sorted by how "
                + "likely they are to buy; the reasons say why. Send reminders and move orders "
                + "from the merchant app or the fulfilment board.";
        for (int c = 0; c < cols.size(); c++) {
            PipelineColumnDto col = cols.get(c);
            BoardColumn out = new BoardColumn();
            out.id = col.id();
            out.title = col.title() == null ? SalesPipeline.title(col.id()) : col.title();
            out.count = String.valueOf(col.total());
            for (int iter = 0; col.cards() != null && iter < col.cards().size(); iter++) {
                out.cards.add(card(storeId, col.cards().get(iter), now));
            }
            v.columns.add(out);
        }
        model.addAttribute("view", v);
        model.addAttribute("table", new DataTable());
        return WebPages.view(model, shell, "marketing");
    }

    static AdminRow card(String storeId, PipelineCardDto p, long now) {
        AdminRow r = new AdminRow();
        String who = p.customer() == null || p.customer().length() == 0 ? "Guest" : p.customer();
        boolean order = SalesPipeline.ORDER.equals(p.kind());
        r.id = p.id();
        r.title = order ? "#" + p.number() + " " + who : who;
        r.amount = Money.format(p.valueCents(), p.currency());
        r.detail = age(order ? p.createdAt() : p.updatedAt(), now);
        if (order) {
            r.url = "/admin/" + storeId + "/orders/" + p.id();
        }
        if (p.intent() >= 0) {
            r.status = "Intent " + p.intent() + " (" + SalesPipeline.band(p.intent()) + ")";
            StringBuilder why = new StringBuilder();
            for (int iter = 0; p.reasons() != null && iter < p.reasons().size(); iter++) {
                String text = reason(p.reasons().get(iter), p.currency());
                if (text.length() > 0) {
                    why.append(why.length() == 0 ? "" : "; ").append(text);
                }
            }
            r.meta = why.toString();
        }
        return r;
    }

    /// A reason code in words; unknown codes are left out.
    static String reason(String code, String currency) {
        String kind = SalesPipeline.reasonCode(code);
        long n = SalesPipeline.reasonValue(code);
        if ("views".equals(kind)) {
            return n == 1 ? "viewed a product once" : "viewed products " + n + " times";
        }
        if ("visits".equals(kind)) {
            return "came back: " + n + " visits";
        }
        if ("minutes".equals(kind)) {
            return n + " min on product pages";
        }
        if ("value".equals(kind)) {
            return "cart worth " + Money.format(n, currency);
        }
        if ("returning".equals(kind)) {
            return n == 1 ? "returning customer, 1 order before"
                    : "returning customer, " + n + " orders before";
        }
        return "";
    }

    static String age(long at, long now) {
        if (at <= 0) {
            return "";
        }
        long minutes = Math.max(0, now - at) / 60000L;
        if (minutes < 60) {
            return minutes + "m ago";
        }
        long hours = minutes / 60;
        return hours < 48 ? hours + "h ago" : (hours / 24) + "d ago";
    }
}
