/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui.layouts;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;


/// LayoutStyle is used to determine how much space to place between components
/// during layout.  LayoutStyle can be obtained for two components, or for
/// a component relative to an edge of a parent container.  The amount of
/// space can vary depending upon whether or not the components are
/// logically grouped together (`RELATED`).
///
/// This class is primarily useful for JREs prior to 1.6.  In 1.6 API for this
/// was added to Swing.  When run on a JRE of 1.6 or greater this will call into
/// the appropriate methods in Swing.
///
/// @version $Revision: 1.10 $
public class LayoutStyle {
    /// Possible argument to getPreferredGap.  Used to indicate the two components
    /// are grouped together.
    public static final int RELATED = 0;
    /// Possible argument to getPreferredGap.  Used to indicate the two components
    /// are not grouped together.
    public static final int UNRELATED = 1;

    /// Possible argument to getPreferredGap.  Used to indicate the distance
    /// to indent a component is being requested.  To visually indicate
    /// a set of related components they will often times be horizontally
    /// indented, the `INDENT` constant for this.
    /// For example, to indent a check box relative to a label use this
    /// constant to `getPreferredGap`.
    public static final int INDENT = 3;

    private static LayoutStyle layoutStyle = new LayoutStyle();
    //private static LookAndFeel laf;

    /// Factory methods for obtaining the current `LayoutStyle`
    /// object appropriate for the current look and feel.
    ///
    /// #### Returns
    ///
    /// the current LayoutStyle instance
    public static LayoutStyle getSharedInstance() {
        return layoutStyle;
    }

    /// Sets the LayoutStyle instance to use for this look and feel.
    /// You generally don't need to invoke this, instead use the getter which
    /// will return the LayoutStyle appropriate for the current look and feel.
    ///
    /// #### Parameters
    ///
    /// - `layout`: @param layout the LayoutStyle to use; a value of null indicates
    /// the default should be used
    public static void setSharedInstance(LayoutStyle layout) {
        layoutStyle = layout;
    }

    /// Returns the amount of space to use between two components.
    /// The return value indicates the distance to place
    /// `component2` relative to `component1`.
    /// For example, the following returns the amount of space to place
    /// between `component2` and `component1`
    /// when `component2` is placed vertically above
    /// `component1`:
    ///
    /// ```java
    ///   int gap = getPreferredGap(component1, component2,
    ///                             LayoutStyle.RELATED,
    ///                             SwingConstants.NORTH, parent);
    /// ```
    ///
    /// The `type` parameter indicates the type
    /// of gap being requested.  It can be one of the following values:
    ///
    /// `RELATED`
    /// If the two components will be contained in
    /// the same parent and are showing similar logically related
    /// items, use `RELATED`.
    /// `UNRELATED`
    /// If the two components will be
    /// contained in the same parent but show logically unrelated items
    /// use `UNRELATED`.
    /// `INDENT`
    /// Used to obtain the preferred distance to indent a component
    /// relative to another.  For example, if you want to horizontally
    /// indent a JCheckBox relative to a JLabel use `INDENT`.
    /// This is only useful for the horizontal axis.
    ///
    /// It's important to note that some look and feels may not distinguish
    /// between `RELATED` and `UNRELATED`.
    ///
    /// The return value is not intended to take into account the
    /// current size and position of `component2` or
    /// `component1`.  The return value may take into
    /// consideration various properties of the components.  For
    /// example, the space may vary based on font size, or the preferred
    /// size of the component.
    ///
    /// #### Parameters
    ///
    /// - `component1`: @param component1 the `JComponent`
    /// `component2` is being placed relative to
    ///
    /// - `component2`: the `JComponent` being placed
    ///
    /// - `type`: how the two components are being placed
    ///
    /// - `position`: @param position   the position `component2` is being placed
    /// relative to `component1`; one of
    /// `SwingConstants.NORTH`,
    /// `SwingConstants.SOUTH`,
    /// `SwingConstants.EAST` or
    /// `SwingConstants.WEST`
    ///
    /// - `parent`: @param parent     the parent of `component2`; this may differ
    /// from the actual parent and may be null
    ///
    /// #### Returns
    ///
    /// the amount of space to place between the two components
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: @throws IllegalArgumentException if `position` is not
    /// one of `SwingConstants.NORTH`,
    /// `SwingConstants.SOUTH`,
    /// `SwingConstants.EAST` or
    /// `SwingConstants.WEST`; `type` not one
    /// of `INDENT`, `RELATED`
    /// or `UNRELATED`; or `component1` or
    /// `component2` is null
    public int getPreferredGap(Component component1, Component component2,
                               int type, int position, Container parent) {
        if (position != GroupLayout.NORTH &&
                position != GroupLayout.SOUTH &&
                position != GroupLayout.WEST &&
                position != GroupLayout.EAST) {
            throw new IllegalArgumentException("Invalid position");
        }
        if (component1 == null || component2 == null) {
            throw new IllegalArgumentException("Components must be non-null");
        }
        if (type == RELATED) {
            return Display.getInstance().convertToPixels(1, true);
        } else if (type == UNRELATED) {
            return Display.getInstance().convertToPixels(2, true);
        } else if (type == INDENT) {
            if (position == GroupLayout.EAST || position == GroupLayout.WEST) {
                int gap = getButtonChildIndent(component1, position);
                if (gap != 0) {
                    return gap;
                }
                return Display.getInstance().convertToPixels(1, true);
            }
            return Display.getInstance().convertToPixels(1, true);
        }
        throw new IllegalArgumentException("Invalid type");
    }

    /// Returns the amount of space to position a component inside its
    /// parent.
    ///
    /// #### Parameters
    ///
    /// - `component`: the `Component` being positioned
    ///
    /// - `position`: @param position  the position `component` is being placed
    /// relative to its parent; one of
    /// `SwingConstants.NORTH`,
    /// `SwingConstants.SOUTH`,
    /// `SwingConstants.EAST` or
    /// `SwingConstants.WEST`
    ///
    /// - `parent`: @param parent    the parent of `component`; this may differ
    /// from the actual parent and may be null
    ///
    /// #### Returns
    ///
    /// @return the amount of space to place between the component and specified
    /// edge
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: @throws IllegalArgumentException if `position` is not
    /// one of `SwingConstants.NORTH`,
    /// `SwingConstants.SOUTH`,
    /// `SwingConstants.EAST` or
    /// `SwingConstants.WEST`;
    /// or `component` is null
    public int getContainerGap(Component component, int position,
                               Container parent) {
        if (position != GroupLayout.NORTH &&
                position != GroupLayout.SOUTH &&
                position != GroupLayout.WEST &&
                position != GroupLayout.EAST) {
            throw new IllegalArgumentException("Invalid position");
        }
        if (component == null) {
            throw new IllegalArgumentException("Component must be non-null");
        }
        return Display.getInstance().convertToPixels(2, true);
    }

    /// For some look and feels check boxs and radio buttons have an empty
    /// border around them.  Look and feel guidelines generally don't include
    /// this space.  Use this method to subtract this space from the specified
    /// components.
    ///
    /// #### Parameters
    ///
    /// - `source`: First component
    ///
    /// - `target`: Second component
    ///
    /// - `position`: Position doing layout along.
    ///
    /// - `offset`: Ideal offset, not including border/margin
    ///
    /// #### Returns
    ///
    /// offset - border/margin around the component.
    int getCBRBPadding(Component source, Component target, int position,
                       int offset) {
        if (offset < 0) {
            return 0;
        }
        return offset;
    }

    /// For some look and feels check boxs and radio buttons have an empty
    /// border around them.  Look and feel guidelines generally don't include
    /// this space.  Use this method to subtract this space from the specified
    /// components.
    ///
    /// #### Parameters
    ///
    /// - `source`: Component
    ///
    /// - `position`: Position doing layout along.
    ///
    /// - `offset`: Ideal offset, not including border/margin
    ///
    /// #### Returns
    ///
    /// offset - border/margin around the component.
    int getCBRBPadding(Component source, int position, int offset) {
        return Math.max(offset, 0);
    }

    int flipDirection(int position) {
        switch (position) {
            case GroupLayout.NORTH:
                return GroupLayout.SOUTH;
            case GroupLayout.SOUTH:
                return GroupLayout.NORTH;
            case GroupLayout.EAST:
                return GroupLayout.WEST;
            case GroupLayout.WEST:
                return GroupLayout.EAST;
            default:
                throw new IllegalArgumentException("Illegal position: " + position);
        }
    }

    // Fix component alignment to work with labels etc when doing bidi
    /*private boolean isLeftAligned(Label button, int position) {
        if (position == GroupLayout.WEST) {
            boolean ltr = button.getComponentOrientation().isLeftToRight();
            int hAlign = button.getHorizontalAlignment();
            return ((ltr && (hAlign == SwingConstants.LEFT ||
                             hAlign == SwingConstants.LEADING)) ||
                    (!ltr && (hAlign == SwingConstants.TRAILING)));
        }
        return false;
    }

    private boolean isRightAligned(AbstractButton button, int position) {
        if (position == SwingConstants.EAST) {
            boolean ltr = button.getComponentOrientation().isLeftToRight();
            int hAlign = button.getHorizontalAlignment();
            return ((ltr && (hAlign == SwingConstants.RIGHT ||
                             hAlign == SwingConstants.TRAILING)) ||
                    (!ltr && (hAlign == SwingConstants.LEADING)));
        }
        return false;
    }*/

    /*private Image getIcon(Label button) {
        Icon icon = button.getIcon();
        if (icon != null) {
            return icon;
        }
        String key = null;
        if (button instanceof JCheckBox) {
            key = "CheckBox.icon";
        } else if (button instanceof JRadioButton) {
            key = "RadioButton.icon";
        }
        if (key != null) {
            Object oIcon = UIManager.get(key);
            if (oIcon instanceof Icon) {
                return (Icon)oIcon;
            }
        }
        return null;
    }*/

    /// Returns the amount to indent the specified component if it's
    /// a JCheckBox or JRadioButton.  If the component is not a JCheckBox or
    /// JRadioButton, 0 will be returned.
    int getButtonChildIndent(Component c, int position) {
        /*if ((c instanceof JRadioButton) || (c instanceof JCheckBox)) {
            AbstractButton button = (AbstractButton)c;
            Insets insets = c.getInsets();
            Icon icon = getIcon(button);
            int gap = button.getIconTextGap();
            if (isLeftAligned(button, position)) {
                return insets.left + icon.getIconWidth() + gap;
            } else if (isRightAligned(button, position)) {
                return insets.right + icon.getIconWidth() + gap;
            }
        }*/
        return 0;
    }
}
