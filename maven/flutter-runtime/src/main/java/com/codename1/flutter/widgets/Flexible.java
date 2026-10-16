/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.widgets;

import com.codename1.flutter.FlexFit;

/**
 * Marks a child of Row/Column as flexible — Flutter's {@code Flexible}. It
 * receives a share of the free main-axis space proportional to its flex factor
 * (default 1). {@code Expanded} is {@code Flexible} with {@code fit: tight};
 * this class reuses that flex machinery ({@link ExpandedRenderElement} reads
 * the flex factor), with {@code fit} retained but not yet distinguished from
 * tight in the layout pass.
 */
public class Flexible extends Expanded {

    private FlexFit fit = FlexFit.loose;

    public void fit(FlexFit v) {
        this.fit = v == null ? FlexFit.loose : v;
    }

    public FlexFit getFit() {
        return fit;
    }
}
