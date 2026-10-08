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

import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.javax.swing.JComponent;
import com.codename1.desktopcompat.javax.swing.SwingConstants;

/// How a scrollable component sizes itself along one axis inside a
/// viewport.
public enum ScrollableSizeHint {

    /// The component keeps its preferred size and the viewport scrolls it.
    NONE {
        @Override
        public boolean getTracksParentSize(JComponent component, int orientation) {
            return false;
        }
    },

    /// The component always has the size of the viewport.
    FIT,

    /// The component takes the viewport's size while that is larger than
    /// its minimum size, and scrolls below it.
    MINIMUM_STRETCH {
        @Override
        public boolean getTracksParentSize(JComponent component, int orientation) {
            return parentLength(component, orientation) > length(component.getMinimumSize(), orientation);
        }
    },

    /// The component takes the viewport's size while that is larger than
    /// its preferred size, and scrolls below it.
    PREFERRED_STRETCH {
        @Override
        public boolean getTracksParentSize(JComponent component, int orientation) {
            return parentLength(component, orientation) > length(component.getPreferredSize(), orientation);
        }
    };

    /// Whether `component` is to be given the size of its parent along
    /// `orientation`, one of the [SwingConstants] `HORIZONTAL` and
    /// `VERTICAL`.
    public boolean getTracksParentSize(JComponent component, int orientation) {
        return true;
    }

    private static int length(Dimension d, int orientation) {
        if (d == null) {
            return 0;
        }
        return orientation == SwingConstants.HORIZONTAL ? d.width : d.height;
    }

    private static int parentLength(JComponent component, int orientation) {
        Container p = component.getParent();
        if (p == null) {
            return 0;
        }
        return orientation == SwingConstants.HORIZONTAL ? p.getWidth() : p.getHeight();
    }
}
