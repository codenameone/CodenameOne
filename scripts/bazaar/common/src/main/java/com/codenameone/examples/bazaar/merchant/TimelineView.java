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
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.bazaar.api.TimelineEventDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.List;

/// A vertical timeline of a transfer's or purchase order's events: a line
/// down the left with a stop per event, oldest at the top. Painted, so it
/// stays one component however many events there are.
final class TimelineView extends Component {
    private final String[] titles;
    private final String[] details;

    TimelineView(List<TimelineEventDto> events) {
        int n = events == null ? 0 : events.size();
        titles = new String[n];
        details = new String[n];
        for (int iter = 0; iter < n; iter++) {
            TimelineEventDto e = events.get(iter);
            titles[iter] = Lang.tr(Words.of(e.kind()));
            details[iter] = Period.iso(e.at()) + (e.username() == null || e.username().length() == 0
                    ? "" : "  " + e.username());
        }
        setUIID("MeCard");
        setName("timeline");
    }

    /// A card that loads and shows the timeline of transfer or purchase order `id`.
    static Container load(boolean transfer, String id) {
        final Container holder = new Container(BoxLayout.y());
        holder.setName(transfer ? "transfer-timeline" : "purchase-timeline");
        OnComplete<Response<List<TimelineEventDto>>> done = Net.to(events -> {
            holder.removeAll();
            holder.add(new TimelineView(events));
            Ui.relayout(holder);
        }, Ui::fail);
        if (transfer) {
            Api.ops().transferTimeline(MerchantState.storeId(), id, done);
        } else {
            Api.ops().purchaseOrderTimeline(MerchantState.storeId(), id, done);
        }
        return holder;
    }

    private int step() {
        return Math.max(1, getStyle().getFont().getHeight()) * 3;
    }

    @Override
    protected Dimension calcPreferredSize() {
        return new Dimension(Math.max(1, getWidth()), Math.max(1, titles.length) * step()
                + getStyle().getVerticalPadding());
    }

    @Override
    public void paint(Graphics g) {
        Font f = getStyle().getFont();
        int step = step();
        int x = getX() + getStyle().getPaddingLeftNoRTL() + f.getHeight() / 2;
        int y0 = getY() + getStyle().getPaddingTop() + f.getHeight() / 2;
        int r = Math.max(3, f.getHeight() / 4);
        g.setColor(getStyle().getFgColor());
        g.setFont(f);
        if (titles.length > 1) {
            g.drawLine(x, y0, x, y0 + (titles.length - 1) * step);
        }
        for (int iter = 0; iter < titles.length; iter++) {
            int y = y0 + iter * step;
            g.fillArc(x - r, y - r, r * 2, r * 2, 0, 360);
            g.drawString(titles[iter], x + f.getHeight(), y - f.getHeight() / 2);
            g.drawString(details[iter], x + f.getHeight(), y + f.getHeight() / 2);
        }
    }
}
