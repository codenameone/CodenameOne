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
package com.codenameone.examples.wayline.ui;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;

/// The sheet that rises from the bottom of a map.
///
/// It has two parts. [#peek()] is what is always there: the headline and the
/// one thing to do next. [#more()] is the rest -- the details, the lesser
/// actions -- and is shown when the sheet is pulled up and put away when it is
/// pushed down, so the map keeps most of the screen until the user asks for
/// the sheet. The handle at the top is what is pulled; a tap on it does the
/// same. A sheet with nothing in `more` has no handle.
///
/// A screen fills it the same way each time: [#reset()], add to the two parts,
/// [#ready()].
public final class BottomSheet extends Container {
    private final Grip grip = new Grip();
    private final Container peek = new Container(BoxLayout.y());
    private final Container more = new Container(BoxLayout.y());
    private boolean expanded;

    public BottomSheet() {
        super(BoxLayout.y());
        setUIID("WlSheet");
        setName("panel");
        setSafeArea(true);
        // A touch on the sheet is the sheet's, and does not reach through to
        // drag the map under it.
        setGrabsPointerEvents(true);
        add(grip).add(peek).add(more);
        more.setHidden(true);
    }

    /// The padding under the sheet's content as the theme has it, and as it
    /// was last made here.
    private int styledUnder = -1;
    private int madeUnder = -1;

    /// Keeps the content clear of what the device draws over the bottom of
    /// the screen, by making the sheet's own padding at least that deep.
    ///
    /// Marking the sheet a safe area is not enough. A safe area is padded
    /// while it is laid out, from where it then is; asked how tall it wants
    /// to be before that, it answers without the padding, is given that
    /// height, and then spends some of it on the padding after all -- which
    /// came out of the last thing in the sheet. Padding that is there both
    /// times cannot disagree with itself. The mark stays for the sides, which
    /// do not change how tall the sheet is.
    private void clearOfTheDevice() {
        Style style = getUnselectedStyle();
        int now = style.getPaddingBottom();
        if (now != madeUnder) {
            // Styled again since: a change of theme, or of name.
            styledUnder = now;
        }
        Rectangle safe = Display.getInstance().getDisplaySafeArea(new Rectangle());
        int covered = safe.getHeight() <= 0 ? 0
                : Math.max(0, CN.getDisplayHeight() - safe.getY() - safe.getHeight());
        int wanted = Math.max(styledUnder, covered);
        if (wanted != now) {
            getAllStyles().setPaddingUnitBottom(Style.UNIT_TYPE_PIXELS);
            getAllStyles().setPaddingBottom(wanted);
        }
        madeUnder = wanted;
    }

    @Override
    protected Dimension calcPreferredSize() {
        clearOfTheDevice();
        return super.calcPreferredSize();
    }

    @Override
    public void layoutContainer() {
        clearOfTheDevice();
        super.layoutContainer();
    }

    public Container peek() {
        return peek;
    }

    public Container more() {
        return more;
    }

    /// Empties the sheet, for the next thing it will show.
    public void reset() {
        peek.removeAll();
        more.removeAll();
    }

    /// The sheet has been filled: lays it out.
    ///
    /// @param open whether to show `more` straight away
    public void ready(boolean open) {
        boolean extra = more.getComponentCount() > 0;
        grip.handle(extra);
        expanded = extra && open;
        more.setHidden(!expanded);
        Container parent = getParent();
        if (parent != null) {
            parent.revalidate();
        }
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean wanted) {
        if (wanted == expanded || more.getComponentCount() == 0) {
            return;
        }
        expanded = wanted;
        more.setHidden(!wanted);
        Container parent = getParent();
        if (parent != null) {
            parent.animateLayout(180);
        }
    }

    /// The handle: a short bar, pulled up or down, or tapped.
    private final class Grip extends Component {
        private int pressedY;
        private boolean pulled;
        /// Whether there is anything to pull the sheet up for. With nothing,
        /// the handle is not drawn and still takes its room: without it the
        /// first line of the sheet sat against the sheet's top edge.
        private boolean handle;

        void handle(boolean wanted) {
            if (handle != wanted) {
                handle = wanted;
                setShouldCalcPreferredSize(true);
            }
        }

        Grip() {
            setUIID("WlGrip");
            setGrabsPointerEvents(true);
        }

        @Override
        protected Dimension calcPreferredSize() {
            Style style = getStyle();
            return new Dimension(CN.convertToPixels(12f), handle
                    ? CN.convertToPixels(1.2f) + style.getVerticalPadding()
                    : CN.convertToPixels(2.6f));
        }

        @Override
        public void paint(Graphics g) {
            if (!handle) {
                return;
            }
            Style style = getStyle();
            int width = CN.convertToPixels(10f);
            int height = CN.convertToPixels(1.2f);
            g.setColor(style.getFgColor());
            g.setAntiAliased(true);
            g.fillRoundRect(getX() + (getWidth() - width) / 2,
                    getY() + (getHeight() - height) / 2, width, height, height, height);
        }

        @Override
        public void pointerPressed(int x, int y) {
            pressedY = y;
            pulled = false;
        }

        @Override
        public void pointerDragged(int x, int y) {
            // Far enough to be meant, and once for each pull.
            if (!pulled && Math.abs(y - pressedY) > CN.convertToPixels(2f)) {
                pulled = true;
                setExpanded(y < pressedY);
            }
        }

        @Override
        public void pointerReleased(int x, int y) {
            if (!pulled) {
                setExpanded(!expanded);
            }
        }
    }
}
