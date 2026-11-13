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

import com.codename1.scheduling.GanttTask;
import com.codename1.ui.Graphics;
import com.codename1.ui.plaf.Style;

/// The renderer a `Gantt` starts with: a rounded bar with its progress
/// filled in, a bracket for a summary and a diamond for a milestone.
///
/// The colours come from `GanttBar`, `GanttBarProgress`, `GanttBarCritical`,
/// `GanttSummaryBar` and `GanttMilestone`, or from the task's own UIID or
/// colour when it has one.
public class DefaultGanttBarRenderer implements GanttBarRenderer {
    @Override
    public void paintBar(Graphics g, Gantt gantt, GanttTask task, int x, int y, int width, int height, int state) {
        boolean critical = (state & CRITICAL) != 0;
        boolean aa = g.isAntiAliased();
        int alpha = g.getAlpha();
        g.setAntiAliased(true);
        if ((state & DRAGGED) != 0) {
            g.setAlpha(alpha * 170 / 255);
        }
        if (task.isSummary()) {
            paintSummary(g, colorOf(task, "GanttSummaryBar", false), x, y, width, height);
        } else if (task.isMilestone()) {
            g.setColor(colorOf(task, critical ? "GanttBarCritical" : "GanttMilestone", false));
            int cx = x + width / 2;
            int cy = y + height / 2;
            int r = Math.min(width, height) / 2;
            g.fillPolygon(new int[]{cx, cx + r, cx, cx - r}, new int[]{cy - r, cy, cy + r, cy}, 4);
        } else {
            int bg = colorOf(task, critical ? "GanttBarCritical" : "GanttBar", false);
            int arc = Math.min(Math.min(width, height), Look.mm(1.2f));
            g.setColor(bg);
            g.fillRoundRect(x, y, width, height, arc, arc);
            int done = width * task.getPercentComplete() / 100;
            if (done > 0) {
                int cx = g.getClipX();
                int cy = g.getClipY();
                int cw = g.getClipWidth();
                int ch = g.getClipHeight();
                g.clipRect(x, y, done, height);
                g.setColor(task.getColor() >= 0 || critical ? Look.mix(bg, 0, 70)
                        : Look.style("GanttBarProgress").getBgColor());
                g.fillRoundRect(x, y, width, height, arc, arc);
                g.setClip(cx, cy, cw, ch);
            }
            if ((state & SELECTED) != 0) {
                g.setColor(Look.style("GanttBar").getFgColor());
                g.drawRoundRect(x, y, width - 1, height - 1, arc, arc);
            }
        }
        g.setAlpha(alpha);
        g.setAntiAliased(aa);
    }

    /// The colour of a task: its own, its UIID's, or the theme's for its kind.
    protected int colorOf(GanttTask task, String uiid, boolean foreground) {
        if (task.getColor() >= 0 && !foreground) {
            return task.getColor();
        }
        Style st = Look.style(task.getUIID() != null ? task.getUIID() : uiid);
        return foreground ? st.getFgColor() : st.getBgColor();
    }

    private static void paintSummary(Graphics g, int color, int x, int y, int width, int height) {
        int bar = Math.max(2, height * 2 / 5);
        int tip = Math.min(height, Math.max(2, width / 2));
        g.setColor(color);
        g.fillRect(x, y, width, bar);
        g.fillPolygon(new int[]{x, x + tip, x}, new int[]{y, y, y + height}, 3);
        g.fillPolygon(new int[]{x + width, x + width - tip, x + width}, new int[]{y, y, y + height}, 3);
    }
}
