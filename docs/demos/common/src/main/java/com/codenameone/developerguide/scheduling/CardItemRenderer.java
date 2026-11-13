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
package com.codenameone.developerguide.scheduling;

import com.codename1.scheduling.ScheduleItem;
import com.codename1.ui.Graphics;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.scheduler.DefaultScheduleItemRenderer;
import com.codename1.ui.scheduler.Scheduler;

// tag::scheduler-renderer[]
/// Draws an event as a card: a pale body with a stripe in the item's
/// colour, which deepens under the pointer and when it is selected.
public class CardItemRenderer extends DefaultScheduleItemRenderer {
    @Override
    public void paintItem(Graphics g, Scheduler scheduler, ScheduleItem item,
            int x, int y, int width, int height, int state) {
        if (item.getKind() != ScheduleItem.Kind.EVENT) {
            // Working time and blocked time keep the theme's look.
            super.paintItem(g, scheduler, item, x, y, width, height, state);
            return;
        }
        int color = item.getColor() >= 0 ? item.getColor() : 0x0f766e;
        boolean lit = (state & (HOVERED | SELECTED | DRAGGED)) != 0;
        int alpha = g.getAlpha();
        g.setColor(0xffffff);
        g.fillRect(x, y, width, height);
        g.setColor(color);
        g.setAlpha(alpha * (lit ? 90 : 45) / 255);
        g.fillRect(x, y, width, height);
        g.setAlpha(alpha);
        int stripe = Math.min(width, 4);
        g.fillRect(x, y, stripe, height);
        paintText(g, scheduler, item,
                UIManager.getInstance().getComponentStyle("SchedulerItem").getFont(),
                0x1d1b20, x + stripe, y, width - stripe, height, state);
    }
}
// end::scheduler-renderer[]
