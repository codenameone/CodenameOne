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

import com.codenameone.examples.bazaar.api.SeriesPointDto;
import com.codenameone.examples.bazaar.api.TopItemDto;
import com.codenameone.examples.bazaar.web.Money;

import java.util.List;

/// The dashboard's charts, drawn on the server as SVG geometry: no chart
/// script, nothing to download, and the page reads the same with scripts off.
/// The template supplies the elements and the stylesheet's `.chart` classes;
/// this works out the numbers, in whole units of a fixed `viewBox` so the
/// markup stays short and deterministic.
final class AdminCharts {
    /// The sales line's drawing area inside its 600 x 200 box.
    static final int W = 600;
    static final int H = 200;
    static final int LEFT = 8;
    static final int TOP = 12;
    static final int BOTTOM = 176;
    static final int BAR_ROW = 28;

    private AdminCharts() {
    }

    /// Fills the sales line of `view` from one point per day.
    static void sales(AdminView view, List<SeriesPointDto> days, String currency) {
        int n = days == null ? 0 : days.size();
        if (n == 0) {
            view.salesSummary = "No sales in this period.";
            return;
        }
        long max = 0;
        long total = 0;
        for (int iter = 0; iter < n; iter++) {
            max = Math.max(max, days.get(iter).salesCents());
            total += days.get(iter).salesCents();
        }
        int right = W - LEFT;
        StringBuilder line = new StringBuilder();
        StringBuilder area = new StringBuilder();
        for (int iter = 0; iter < n; iter++) {
            SeriesPointDto p = days.get(iter);
            int x = n == 1 ? (LEFT + right) / 2 : LEFT + (int) ((long) (right - LEFT) * iter / (n - 1));
            int y = max == 0 ? BOTTOM
                    : BOTTOM - (int) ((BOTTOM - TOP) * p.salesCents() / max);
            if (iter > 0) {
                line.append(' ');
            }
            line.append(x).append(',').append(y);
            area.append(" L").append(x).append(',').append(y);
            // A tick under the first, the middle and the last day.
            if (iter == 0 || iter == n - 1 || iter == n / 2) {
                ChartMark t = new ChartMark();
                t.textX = x;
                t.textY = H - 6;
                t.label = p.label();
                view.salesTicks.add(t);
            }
            AdminRow r = new AdminRow();
            r.title = p.label();
            r.quantity = Integer.toString(p.orders());
            r.amount = Money.format(p.salesCents(), currency);
            view.salesDays.add(r);
        }
        view.salesLine = line.toString();
        int firstX = n == 1 ? (LEFT + right) / 2 : LEFT;
        int lastX = n == 1 ? (LEFT + right) / 2 : right;
        view.salesArea = "M" + firstX + "," + BOTTOM + area + " L" + lastX + "," + BOTTOM + " Z";
        view.salesMax = Money.format(max, currency);
        view.salesSummary = "Sales of " + Money.format(total, currency) + " over " + n
                + " days; the best day took " + Money.format(max, currency) + ".";
    }

    /// Fills the top-products bars: one row per product, longest first as given.
    static void top(AdminView view, List<TopItemDto> items, String currency) {
        int n = items == null ? 0 : Math.min(8, items.size());
        long max = 0;
        for (int iter = 0; iter < n; iter++) {
            max = Math.max(max, items.get(iter).salesCents());
        }
        int barLeft = 200;
        int barMax = W - barLeft - 90;
        for (int iter = 0; iter < n; iter++) {
            TopItemDto t = items.get(iter);
            ChartMark m = new ChartMark();
            m.x = barLeft;
            m.y = iter * BAR_ROW + 4;
            m.height = BAR_ROW - 10;
            m.width = max == 0 ? 0 : Math.max(2, (int) ((long) barMax * t.salesCents() / max));
            m.textX = 0;
            m.textY = m.y + m.height - 4;
            m.valueX = barLeft + m.width + 6;
            String title = t.title() == null ? "" : t.title();
            m.label = title.length() > 28 ? title.substring(0, 27) + "..." : title;
            m.value = Money.format(t.salesCents(), currency) + " (" + t.quantity() + ")";
            view.topBars.add(m);
        }
        view.topHeight = Math.max(BAR_ROW, n * BAR_ROW);
        view.topBox = "0 0 " + W + " " + view.topHeight;
    }
}
