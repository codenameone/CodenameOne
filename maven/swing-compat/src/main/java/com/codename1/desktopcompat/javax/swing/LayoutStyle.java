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
package com.codename1.desktopcompat.javax.swing;

import com.codename1.desktopcompat.java.awt.Container;

/// Answers how much space belongs between two components, and between a
/// component and the edge of its container. [GroupLayout] asks it for
/// every preferred gap.
///
/// The shared instance is the one given to `setInstance`, or a default
/// that answers 6 pixels between related components and for the container
/// edge, and 12 between unrelated ones -- the JDK's values for a look and
/// feel that brings no style of its own.
///
/// Not supported: the look and feel is never asked for a style, and the
/// default does not widen an `INDENT` gap by the width of a check box or
/// radio button indicator; it answers the related gap for it.
public abstract class LayoutStyle {

    /// The kinds of gap a component placement can ask for.
    public enum ComponentPlacement {
        /// The two components are logically related.
        RELATED,
        /// The two components are not logically related.
        UNRELATED,
        /// One component is indented under the other.
        INDENT
    }

    private static LayoutStyle instance;

    /// Creates a new `LayoutStyle`.
    public LayoutStyle() {
    }

    /// Sets the shared instance; `null` restores the default.
    public static void setInstance(LayoutStyle style) {
        instance = style;
    }

    /// Returns the shared instance.
    public static LayoutStyle getInstance() {
        LayoutStyle style = instance;
        if (style == null) {
            style = DefaultStyle.INSTANCE;
        }
        return style;
    }

    /// Returns the amount of space to use between two components.
    /// `position` is where `component2` sits relative to `component1`,
    /// one of the `SwingConstants` compass points `NORTH`, `SOUTH`, `EAST`
    /// and `WEST`.
    public abstract int getPreferredGap(JComponent component1, JComponent component2, ComponentPlacement type,
            int position, Container parent);

    /// Returns the amount of space to place between a component and the
    /// edge of its parent that `position` names.
    public abstract int getContainerGap(JComponent component, int position, Container parent);

    private static final class DefaultStyle extends LayoutStyle {
        static final LayoutStyle INSTANCE = new DefaultStyle();

        private static void checkPosition(int position) {
            if (position != SwingConstants.NORTH && position != SwingConstants.SOUTH
                    && position != SwingConstants.WEST && position != SwingConstants.EAST) {
                throw new IllegalArgumentException();
            }
        }

        @Override
        public int getPreferredGap(JComponent component1, JComponent component2, ComponentPlacement type,
                int position, Container parent) {
            if (component1 == null || component2 == null || type == null) {
                throw new NullPointerException();
            }
            checkPosition(position);
            return type == ComponentPlacement.UNRELATED ? 12 : 6;
        }

        @Override
        public int getContainerGap(JComponent component, int position, Container parent) {
            if (component == null) {
                throw new NullPointerException();
            }
            checkPosition(position);
            return 6;
        }
    }
}
