/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.components;

import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;

/// A rule that divides one group of controls from the next.
///
/// Every desktop toolkit draws one -- `NSBox` in separator mode, WinUI's
/// `MenuFlyoutSeparator` and navigation separators, `GtkSeparator` -- and Codename One had
/// no component for it, so applications drew their own out of a `Label` with a bottom
/// border, or a `Container` with a fixed height and a background colour. Those look
/// approximately right on one platform and wrong everywhere else, because the thing that
/// differs between platforms is exactly the part they hard-code: the thickness, the
/// colour, and how much air sits either side of it.
///
/// All three come from the `Separator` UIID. The rule itself is the style's **border**
/// where there is one and its background colour otherwise, the air either side is the
/// style's margin, and the thickness is the `separatorThicknessMM` theme constant. A
/// theme that says nothing gets a one-pixel line in the foreground colour.
///
/// ```java
/// Container settings = new Container(BoxLayout.y());
/// settings.add(new Label("Appearance"))
///         .add(new Separator())
///         .add(new Label("Privacy"));
/// ```
///
/// A separator is not focusable and is skipped by keyboard traversal: it is decoration,
/// and stopping on it with Tab would be a bug on every platform.
public class Separator extends Component {

    /// A rule that runs left to right, dividing the rows of a vertical stack.
    public static final int HORIZONTAL = 0;

    /// A rule that runs top to bottom, dividing the columns of a horizontal row.
    public static final int VERTICAL = 1;

    private int orientation = HORIZONTAL;

    /// A horizontal separator.
    public Separator() {
        this(HORIZONTAL);
    }

    /// A separator running in the given direction.
    ///
    /// #### Parameters
    ///
    /// - `orientation`: `#HORIZONTAL` or `#VERTICAL`
    public Separator(int orientation) {
        this.orientation = orientation;
        setUIID("Separator");
        setFocusable(false);
    }

    /// The direction this rule runs in.
    ///
    /// #### Returns
    ///
    /// `#HORIZONTAL` or `#VERTICAL`
    public int getOrientation() {
        return orientation;
    }

    /// Sets the direction this rule runs in.
    ///
    /// #### Parameters
    ///
    /// - `orientation`: `#HORIZONTAL` or `#VERTICAL`
    public void setOrientation(int orientation) {
        if (this.orientation != orientation) {
            this.orientation = orientation;
            setShouldCalcPreferredSize(true);
        }
    }

    /// The rule's thickness in pixels, from the `separatorThicknessMM` theme constant.
    ///
    /// Never zero. A theme is free to ask for a hairline, and a hairline on a high density
    /// screen rounds to zero millimetres worth of pixels -- which would make the separator
    /// invisible rather than thin, and invisible is the one thing it must not be.
    ///
    /// #### Returns
    ///
    /// the thickness in pixels, at least 1
    protected int getThickness() {
        // A string, like every other *MM constant here (see Slider's track and thumb): the
        // theme format has no float accessor, and parsing a malformed one has to fall back
        // rather than throw out of a paint.
        String mm = getUIManager().getThemeConstant("separatorThicknessMM", null);
        if (mm == null || mm.trim().length() == 0) {
            return 1;
        }
        try {
            return Math.max(1, Display.getInstance().convertToPixels(Float.parseFloat(mm.trim())));
        } catch (NumberFormatException notANumber) {
            return 1;
        }
    }

    @Override
    protected Dimension calcPreferredSize() {
        Style s = getStyle();
        int thickness = getThickness();
        if (orientation == VERTICAL) {
            return new Dimension(thickness + s.getHorizontalPadding(), s.getVerticalPadding());
        }
        return new Dimension(s.getHorizontalPadding(), thickness + s.getVerticalPadding());
    }

    @Override
    public void paint(Graphics g) {
        Style s = getStyle();
        if (s.getBorder() != null) {
            // A themed border already draws the rule, including the per-side colours a
            // platform that wants a two-tone bevel needs. Painting over it would double it.
            return;
        }
        if ((s.getBgTransparency() & 0xff) == 0xff) {
            // An opaque background IS the rule, and Component.paintBackground has already
            // drawn it. The component is exactly as thick as the rule, so filling over it with
            // the foreground would repaint the whole visible area in the text colour -- which
            // is what the Android Material and iOS Modern themes would have got, since both
            // define Separator through background-color and never set a foreground.
            //
            // Same reasoning as the border case above: whoever already drew the rule owns it.
            return;
        }
        int thickness = getThickness();
        g.setColor(s.getFgColor());
        int alpha = g.concatenateAlpha(s.getFgAlpha());
        if (orientation == VERTICAL) {
            g.fillRect(getX() + s.getPaddingLeft(isRTL()) + (getWidth() - s.getHorizontalPadding()
                            - thickness) / 2,
                    getY() + s.getPaddingTop(),
                    thickness,
                    getHeight() - s.getVerticalPadding());
        } else {
            g.fillRect(getX() + s.getPaddingLeft(isRTL()),
                    getY() + s.getPaddingTop() + (getHeight() - s.getVerticalPadding()
                            - thickness) / 2,
                    getWidth() - s.getHorizontalPadding(),
                    thickness);
        }
        g.setAlpha(alpha);
    }
}
