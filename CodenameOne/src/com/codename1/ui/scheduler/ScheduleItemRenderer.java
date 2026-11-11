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
import com.codename1.ui.Graphics;

/// Paints one item of a `Scheduler`.
///
/// The views draw items themselves and keep no component per item, so this
/// is the place to change how an item looks. `DefaultScheduleItemRenderer`
/// is the one a scheduler starts with, and is made to be extended.
public interface ScheduleItemRenderer {
    /// The item is the selected one.
    int SELECTED = 1;
    /// The item is being dragged, and this is the copy under the pointer.
    int DRAGGED = 2;
    /// The item began before the part of it that is painted here.
    int STARTS_BEFORE = 4;
    /// The item goes on after the part of it that is painted here.
    int ENDS_AFTER = 8;
    /// The rectangle is a single line: a month cell or the all day lane.
    int COMPACT = 16;
    /// Time runs across, as in the resource timeline, not down.
    int HORIZONTAL = 32;
    /// The pointer of a desktop rests on the item. The view lights the
    /// item itself; a renderer that wants more can tell by this.
    int HOVERED = 64;

    /// Paints `item` into a rectangle. The clip is already inside the view;
    /// an implementation that sets one of its own restores it.
    ///
    /// #### Parameters
    ///
    /// - `state`: a combination of the constants of this interface
    void paintItem(Graphics g, Scheduler scheduler, ScheduleItem item, int x, int y, int width, int height, int state);
}
