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
package com.codename1.wordprocessing;

import java.util.Collections;
import java.util.List;

/// One page of a `DocumentLayout`: its size and everything drawn on it, in painting order.
public final class LayoutPage {
    private final int index;
    private final float width;
    private final float height;
    private final List<LayoutItem> items;

    LayoutPage(int index, float width, float height, List<LayoutItem> items) {
        this.index = index;
        this.width = width;
        this.height = height;
        this.items = Collections.unmodifiableList(items);
    }

    /// The page's position in the document, from zero.
    ///
    /// #### Returns
    ///
    /// the index
    public int getIndex() {
        return index;
    }

    /// The paper width in points.
    ///
    /// #### Returns
    ///
    /// the width
    public float getWidth() {
        return width;
    }

    /// The paper height in points.
    ///
    /// #### Returns
    ///
    /// the height
    public float getHeight() {
        return height;
    }

    /// Everything drawn on the page, in painting order.
    ///
    /// #### Returns
    ///
    /// an unmodifiable list
    public List<LayoutItem> getItems() {
        return items;
    }
}
