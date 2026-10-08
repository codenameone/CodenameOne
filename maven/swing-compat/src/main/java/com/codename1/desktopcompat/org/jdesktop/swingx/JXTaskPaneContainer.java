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
package com.codename1.desktopcompat.org.jdesktop.swingx;

import com.codename1.desktopcompat.javax.swing.BorderFactory;

/// The column task panes are stacked in: a [JXPanel] with a
/// [VerticalLayout] of gap 14 and a margin of the same width around it.
///
/// Inside a scroll pane it takes the viewport's width, and its height
/// too until the task panes need more than that, when it scrolls.
///
/// There is no look and feel delegate: the background is the panel's
/// own, and a gradient can be had with a background painter.
public class JXTaskPaneContainer extends JXPanel {

    public JXTaskPaneContainer() {
        super(new VerticalLayout(14));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        setScrollableWidthHint(ScrollableSizeHint.FIT);
        setScrollableHeightHint(ScrollableSizeHint.PREFERRED_STRETCH);
    }
}
