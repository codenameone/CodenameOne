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

import com.codename1.ai.Tool;
import com.codename1.ai.ToolCall;
import com.codename1.ai.ToolHandler;
import com.codename1.io.JSONParser;
import com.codenameone.examples.bazaar.api.AnalyticsDto;
import com.codenameone.examples.bazaar.api.BreakdownDto;
import com.codenameone.examples.bazaar.api.FunnelStepDto;
import com.codenameone.examples.bazaar.api.InsightsDto;
import com.codenameone.examples.bazaar.api.KpiDto;
import com.codenameone.examples.bazaar.api.PipelineCardDto;
import com.codenameone.examples.bazaar.api.PipelineColumnDto;
import com.codenameone.examples.bazaar.api.TopItemDto;
import com.codenameone.examples.bazaar.ops.SalesPipeline;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The merchant assistant's tools: our own query functions over the store's
/// numbers, which the model calls instead of guessing them.
///
/// Each answers from what the app fetched for the period -- the dashboard,
/// the database-counted insights and the sales pipeline -- as JSON with the
/// figures and a `summary` sentence. The server's rules answer with those
/// summaries when no AI provider is configured, so the assistant says the
/// same true thing either way.
final class AssistantTools {
    static final String SALES = "sales_summary";
    static final String CHANNELS = "channel_mix";
    static final String TOP = "top_products";
    static final String FUNNEL = "conversion_funnel";
    static final String PIPELINE = "pipeline_overview";

    private static final String NO_ARGS = "{\"type\":\"object\",\"properties\":{}}";

    private final AnalyticsDto data;
    private final InsightsDto insights;
    private final List<PipelineColumnDto> board;
    private final String currency;

    AssistantTools(AnalyticsDto data, InsightsDto insights, List<PipelineColumnDto> board,
            String currency) {
        this.data = data;
        this.insights = insights;
        this.board = board;
        this.currency = currency == null ? "" : currency;
    }

    /// The tools, each with its handler.
    List<Tool> tools() {
        List<Tool> out = new ArrayList<Tool>();
        out.add(tool(SALES, "Net sales, orders, average order, customers and conversion for "
                + "the selected period, with the previous period to compare."));
        out.add(tool(CHANNELS, "Orders and sales split by channel: online store, app, point "
                + "of sale, marketplaces and draft orders."));
        out.add(tool(TOP, "The best-selling products of the period by sales."));
        out.add(tool(FUNNEL, "The conversion funnel: product views, carts, checkouts, orders."));
        out.add(tool(PIPELINE, "Open carts and orders by pipeline stage, with the carts most "
                + "likely to buy and why."));
        return out;
    }

    private Tool tool(final String name, String description) {
        return new Tool(name, description, NO_ARGS, new ToolHandler() {
            @Override
            public String invoke(String argumentsJson) {
                return run(name);
            }
        });
    }

    /// Runs a tool by name; an unknown name answers with an error object.
    String run(String name) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("currency", currency);
        if (SALES.equals(name)) {
            sales(out);
        } else if (CHANNELS.equals(name)) {
            channels(out);
        } else if (TOP.equals(name)) {
            top(out);
        } else if (FUNNEL.equals(name)) {
            funnel(out);
        } else if (PIPELINE.equals(name)) {
            pipeline(out);
        } else {
            out.put("error", "unknown tool " + name);
            out.put("summary", "I do not have that information.");
        }
        return JSONParser.mapToJson(out);
    }

    /// Runs the tool a model asked for.
    String run(ToolCall call) {
        return run(call.getName());
    }

    String money(long cents) {
        return Pipelines.PLAIN.money(cents, currency).trim();
    }

    private void sales(Map<String, Object> out) {
        if (data == null || data.current() == null) {
            out.put("summary", "The sales numbers are not loaded yet.");
            return;
        }
        KpiDto now = data.current();
        KpiDto was = data.previous();
        out.put("netSalesCents", now.netSalesCents());
        out.put("orders", now.orders());
        out.put("averageOrderCents", now.averageOrderCents());
        out.put("customers", now.customers());
        out.put("conversionBp", now.conversionBp());
        StringBuilder s = new StringBuilder();
        s.append("Net sales were ").append(money(now.netSalesCents())).append(" from ")
                .append(now.orders()).append(now.orders() == 1 ? " order" : " orders")
                .append(", an average of ").append(money(now.averageOrderCents())).append('.');
        if (was != null) {
            out.put("previousNetSalesCents", was.netSalesCents());
            out.put("previousOrders", was.orders());
            s.append(' ').append(change(now.netSalesCents(), was.netSalesCents()))
                    .append(" on the previous period (").append(money(was.netSalesCents()))
                    .append(").");
        }
        out.put("summary", s.toString());
    }

    /// "Up 25%", "Down 10%", "Level", or "New" when there was nothing before.
    static String change(long now, long was) {
        if (now == was) {
            return "Level";
        }
        if (was <= 0) {
            return "New sales, against nothing";
        }
        long pct = (now - was) * 100 / was;
        return (pct >= 0 ? "Up " : "Down ") + Math.abs(pct) + "%";
    }

    private void channels(Map<String, Object> out) {
        List<BreakdownDto> list = insights == null ? null : insights.channels();
        if (list == null) {
            out.put("summary", "The channel numbers are not loaded yet.");
            return;
        }
        List<Object> rows = new ArrayList<Object>();
        BreakdownDto best = null;
        long total = 0;
        for (int iter = 0; iter < list.size(); iter++) {
            BreakdownDto b = list.get(iter);
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("channel", b.label());
            row.put("orders", b.orders());
            row.put("salesCents", b.salesCents());
            rows.add(row);
            total += b.salesCents();
            if (b.orders() > 0 && (best == null || b.salesCents() > best.salesCents())) {
                best = b;
            }
        }
        out.put("channels", rows);
        if (best == null) {
            out.put("summary", "No channel sold anything in this period.");
            return;
        }
        StringBuilder s = new StringBuilder();
        s.append(best.label()).append(" sold the most: ").append(money(best.salesCents()))
                .append(" from ").append(best.orders()).append(best.orders() == 1 ? " order"
                        : " orders");
        if (total > 0) {
            s.append(", ").append(best.salesCents() * 100 / total).append("% of sales");
        }
        s.append('.');
        for (int iter = 0; iter < list.size(); iter++) {
            BreakdownDto b = list.get(iter);
            if (b != best && b.orders() > 0) {
                s.append(' ').append(b.label()).append(": ").append(money(b.salesCents()))
                        .append('.');
            }
        }
        out.put("summary", s.toString());
    }

    private void top(Map<String, Object> out) {
        List<TopItemDto> list = data == null ? null : data.topProducts();
        if (list == null || list.isEmpty()) {
            out.put("summary", "Nothing sold in this period.");
            return;
        }
        List<Object> rows = new ArrayList<Object>();
        StringBuilder s = new StringBuilder("Best sellers: ");
        for (int iter = 0; iter < list.size() && iter < 5; iter++) {
            TopItemDto t = list.get(iter);
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("product", t.title());
            row.put("units", t.quantity());
            row.put("salesCents", t.salesCents());
            rows.add(row);
            if (iter > 0) {
                s.append("; ");
            }
            s.append(t.title()).append(" (").append(t.quantity()).append(" sold, ")
                    .append(money(t.salesCents())).append(')');
        }
        out.put("products", rows);
        out.put("summary", s.append('.').toString());
    }

    private void funnel(Map<String, Object> out) {
        List<FunnelStepDto> steps = insights == null ? null : insights.funnel();
        if (steps == null || steps.isEmpty()) {
            out.put("summary", "The funnel is not loaded yet.");
            return;
        }
        int[] shares = Insights.shares(steps);
        List<Object> rows = new ArrayList<Object>();
        StringBuilder s = new StringBuilder();
        for (int iter = 0; iter < steps.size(); iter++) {
            FunnelStepDto f = steps.get(iter);
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("step", f.step());
            row.put("count", f.count());
            row.put("shareOfFirstBp", shares[iter]);
            rows.add(row);
            s.append(iter == 0 ? "" : ", then ").append(f.count()).append(' ')
                    .append(lowerFirst(f.label()));
        }
        out.put("funnel", rows);
        int last = steps.size() - 1;
        if (last > 0 && steps.get(0).count() > 0) {
            s.append(". ").append(shares[last] / 100).append('.')
                    .append(shares[last] % 100 < 10 ? "0" : "").append(shares[last] % 100)
                    .append("% of the first step reached the last");
            int worst = worstStep(steps);
            if (worst > 0) {
                s.append("; the biggest drop is from ").append(lowerFirst(
                        steps.get(worst - 1).label())).append(" to ").append(lowerFirst(
                        steps.get(worst).label()));
            }
        }
        out.put("summary", s.append('.').toString());
    }

    /// The step losing the largest share of the one before it.
    static int worstStep(List<FunnelStepDto> steps) {
        int worst = -1;
        long worstKept = Long.MAX_VALUE;
        for (int iter = 1; iter < steps.size(); iter++) {
            long before = steps.get(iter - 1).count();
            if (before <= 0) {
                continue;
            }
            long kept = steps.get(iter).count() * 10000L / before;
            if (kept < worstKept) {
                worstKept = kept;
                worst = iter;
            }
        }
        return worst;
    }

    static String lowerFirst(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char c = s.charAt(0);
        return c >= 'A' && c <= 'Z' ? (char) (c + 32) + s.substring(1) : s;
    }

    private void pipeline(Map<String, Object> out) {
        if (board == null) {
            out.put("summary", "The pipeline is not loaded yet.");
            return;
        }
        List<Object> rows = new ArrayList<Object>();
        StringBuilder s = new StringBuilder();
        PipelineCardDto hottest = null;
        for (int iter = 0; iter < board.size(); iter++) {
            PipelineColumnDto c = board.get(iter);
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("stage", c.title());
            row.put("count", c.total());
            rows.add(row);
            if (c.total() > 0) {
                s.append(s.length() == 0 ? "" : ", ").append(c.total()).append(' ')
                        .append(lowerFirst(c.title()));
            }
            for (int at = 0; c.cards() != null && at < c.cards().size(); at++) {
                PipelineCardDto p = c.cards().get(at);
                if (SalesPipeline.CART.equals(p.kind()) && (hottest == null
                        || p.intent() > hottest.intent())) {
                    hottest = p;
                }
            }
        }
        out.put("stages", rows);
        if (s.length() == 0) {
            out.put("summary", "There are no open carts or orders right now.");
            return;
        }
        s.insert(0, "Open now: ").append('.');
        if (hottest != null && hottest.intent() > 0) {
            Map<String, Object> hot = new LinkedHashMap<String, Object>();
            hot.put("customer", hottest.customer());
            hot.put("valueCents", hottest.valueCents());
            hot.put("intent", hottest.intent());
            hot.put("reasons", hottest.reasons());
            out.put("hottestCart", hot);
            List<String> why = Pipelines.explain(hottest, Pipelines.PLAIN);
            s.append(" The cart most likely to buy is worth ").append(money(
                    hottest.valueCents())).append(", intent ").append(hottest.intent());
            if (!why.isEmpty()) {
                s.append(": ").append(lowerFirst(why.get(0)));
            }
            s.append('.');
        }
        out.put("summary", s.toString());
    }
}
