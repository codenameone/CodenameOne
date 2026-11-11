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
package com.codename1.scheduling;

import java.util.List;

/// What a `Scheduler` shows and edits.
///
/// A view asks for the items of the range on screen every time it paints, so
/// `getItems` has to be cheap: a model that loads from a server answers from
/// what it has and fires a change when more arrives. `setVisibleRange` is how
/// it learns what to load.
///
/// `DefaultScheduleModel` holds items in memory, and `CalendarScheduleModel`
/// reads a calendar source. Extend `AbstractScheduleModel` for one of your own.
public interface ScheduleModel {
    /// The resources, in the order they are shown. Empty for a schedule that
    /// is one person's.
    List<ScheduleResource> getResources();

    /// Adds every item with any part inside `[from, to)` to `out`.
    void getItems(long from, long to, List<ScheduleItem> out);

    /// Told by a view whenever the range it shows changes, with a margin
    /// around it. A model that keeps everything in memory ignores it.
    void setVisibleRange(long from, long to);

    /// Moves or resizes an item, and gives it to another resource when
    /// `resourceId` differs from the one it has.
    ///
    /// #### Returns
    ///
    /// false when the model refuses the change; the view puts the item back
    boolean updateItem(ScheduleItem item, long start, long end, String resourceId);

    /// Creates an item for a range the user dragged out on empty space.
    ///
    /// #### Returns
    ///
    /// the new item, or null when the model does not create items this way
    ScheduleItem createItem(long start, long end, String resourceId);

    /// Removes an item.
    ///
    /// #### Returns
    ///
    /// false when the model refuses
    boolean removeItem(ScheduleItem item);

    void addChangeListener(ScheduleChangeListener l);

    void removeChangeListener(ScheduleChangeListener l);
}
