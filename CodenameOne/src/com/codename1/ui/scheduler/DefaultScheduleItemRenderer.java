/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.ui.scheduler;

import com.codename1.scheduling.ScheduleItem;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.plaf.Style;

/// The renderer a `Scheduler` starts with: a rounded block in the item's
/// colour with its title, and its time and detail where they fit.
///
/// The colours and the font come from a UIID: the item's own when it has
/// one, otherwise `SchedulerItem`, `SchedulerBackgroundItem` or
/// `SchedulerBlocked` by its kind. An item or a resource with a colour of
/// its own overrides the background, and the text turns black or white to
/// stay readable on it.
public class DefaultScheduleItemRenderer implements ScheduleItemRenderer {
    @Override
    public void paintItem(Graphics g, Scheduler scheduler, ScheduleItem item, int x, int y, int width, int height,
            int state) {
        if (width <= 0 || height <= 0) {
            return;
        }
        String uiid = uiidOf(item);
        Style st = (state & SELECTED) != 0 ? Look.selected(uiid) : Look.style(uiid);
        int own = item.getColor() >= 0 ? item.getColor() : scheduler.resourceColor(item.getResourceId());
        int bg = own >= 0 ? own : st.getBgColor();
        int fg = own >= 0 ? Look.textOn(bg) : st.getFgColor();
        int opacity = st.getBgTransparency() & 0xff;
        if (own >= 0 && opacity == 0) {
            opacity = 255;
        }
        if ((state & SELECTED) != 0 && own >= 0) {
            bg = Look.mix(bg, 0, 50);
        }
        int alpha = g.getAlpha();
        boolean aa = g.isAntiAliased();
        if ((state & DRAGGED) != 0) {
            opacity = opacity * 190 / 255;
        }
        boolean compact = (state & COMPACT) != 0;
        int arc = Math.min(Math.min(width, height), Look.mm(compact ? 1f : 1.6f));
        g.setAntiAliased(true);
        g.setColor(bg);
        g.setAlpha(alpha * opacity / 255);
        g.fillRoundRect(x, y, width, height, arc, arc);
        if (item.getKind() == ScheduleItem.Kind.BLOCKED) {
            hatch(g, fg, alpha, x, y, width, height);
        }
        g.setAlpha(alpha);
        if ((state & SELECTED) != 0) {
            g.setColor(fg);
            g.drawRoundRect(x, y, width - 1, height - 1, arc, arc);
        }
        g.setAntiAliased(aa);
        paintText(g, scheduler, item, st.getFont(), fg, x, y, width, height, state);
    }

    /// The UIID an item is painted with.
    protected String uiidOf(ScheduleItem item) {
        if (item.getUIID() != null) {
            return item.getUIID();
        }
        if (item.getKind() == ScheduleItem.Kind.BACKGROUND) {
            return "SchedulerBackgroundItem";
        }
        if (item.getKind() == ScheduleItem.Kind.BLOCKED) {
            return "SchedulerBlocked";
        }
        return "SchedulerItem";
    }

    /// Draws the title, and under it the time and the detail when the
    /// rectangle is tall enough.
    protected void paintText(Graphics g, Scheduler scheduler, ScheduleItem item, Font font, int color, int x, int y,
            int width, int height, int state) {
        boolean compact = (state & COMPACT) != 0;
        int pad = Look.mm(compact ? 0.7f : 1f);
        int fh = font.getHeight();
        int tw = width - 2 * pad;
        if (tw <= 0 || height < fh * 2 / 3) {
            return;
        }
        int cx = g.getClipX();
        int cy = g.getClipY();
        int cw = g.getClipWidth();
        int ch = g.getClipHeight();
        g.clipRect(x, y, width, height);
        g.setFont(font);
        g.setColor(color);
        int ty = height < fh + 2 * pad ? y + (height - fh) / 2 : y + pad;
        String title = item.getTitle();
        if (compact && !item.isAllDay() && item.getKind() == ScheduleItem.Kind.EVENT
                && (state & HORIZONTAL) == 0) {
            title = scheduler.formatTime(item.getStart()) + " " + title;
        }
        g.drawString(Look.fit(title, font, tw), x + pad, ty);
        if (!compact && item.getKind() == ScheduleItem.Kind.EVENT) {
            int ly = ty + fh;
            int alpha = g.getAlpha();
            g.setAlpha(alpha * 200 / 255);
            if (ly + fh <= y + height - pad / 2 && !item.isAllDay()) {
                g.drawString(Look.fit(scheduler.formatRange(item.getStart(), item.getEnd()), font, tw), x + pad, ly);
                ly += fh;
            }
            String detail = item.getDetail();
            if (detail != null && detail.length() > 0 && ly + fh <= y + height - pad / 2) {
                g.drawString(Look.fit(detail, font, tw), x + pad, ly);
            }
            g.setAlpha(alpha);
        }
        g.setClip(cx, cy, cw, ch);
    }

    private static void hatch(Graphics g, int color, int alpha, int x, int y, int w, int h) {
        int cx = g.getClipX();
        int cy = g.getClipY();
        int cw = g.getClipWidth();
        int ch = g.getClipHeight();
        g.clipRect(x, y, w, h);
        g.setColor(color);
        g.setAlpha(alpha * 40 / 255);
        int step = Look.mm(1.6f);
        for (int i = -h; i < w; i += step) {
            g.drawLine(x + i, y + h, x + i + h, y);
        }
        g.setClip(cx, cy, cw, ch);
    }
}
