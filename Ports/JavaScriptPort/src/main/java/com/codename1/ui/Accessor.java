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

package com.codename1.ui;

/**
 *
 * @author shannah
 */
public class Accessor {
    public static int getActivePeerCount() {
        return Form.activePeerCount;
    }

    /**
     * Returns the implementation-level graphics a {@link Graphics} is drawing through.
     *
     * <p>The port needs this to tell a paint aimed at the display from one aimed at an
     * offscreen image. Both raise the same per-component paint callbacks, but only the
     * display paint may touch the DOM text layer.</p>
     *
     * @param g the graphics to unwrap, may be null
     * @return the native graphics object, or null
     */
    public static Object nativeGraphics(Graphics g) {
        return g == null ? null : g.getGraphics();
    }

    /**
     * Returns true when the form paints something over its children that the DOM text layer
     * would end up on top of.
     *
     * <p>The text layer sits above the output canvas as a whole, so anything the canvas draws
     * after a component -- the glass pane, the image of a dragged component -- cannot cover
     * promoted text. While either is present the layer stands down and text goes back to the
     * canvas, where paint order still decides what is on top.</p>
     *
     * @param f the form to test, may be null
     * @return true when text promotion must be suspended for this form
     */
    public static boolean paintsOverChildren(Form f) {
        return f != null && (f.getGlassPane() != null || f.getDraggedComponent() != null);
    }

    /**
     * Returns true when a component and its whole ancestor chain are visible.
     *
     * <p>A hidden component stops painting without being detached, so this is what tells the
     * text layer that runs it is still holding can never be refreshed again.</p>
     *
     * @param c the component to test, may be null
     * @return true when the component would be painted by its form
     */
    public static boolean isDisplayable(Component c) {
        if (c == null || c.isHidden(true)) {
            return false;
        }
        // isVisible() reports the component's own flag and isHidden(true) walks the ancestors
        // for the separate zero-preferred-size hidden state, so neither notices a parent that
        // was simply made invisible. Walk the chain: a child of an invisible parent does not
        // paint, and its runs would otherwise be kept after the parent's repaint cleared them.
        for (Component current = c; current != null; current = current.getParent()) {
            if (!current.isVisible()) {
                return false;
            }
        }
        return true;
    }
}
