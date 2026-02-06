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
package com.codename1.ui.plaf;

import com.codename1.components.InfiniteProgress;
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.InputComponent;
import com.codename1.ui.Label;
import com.codename1.ui.List;
import com.codename1.ui.MenuBar;
import com.codename1.ui.SideMenuBar;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextSelection;
import com.codename1.ui.TextSelection.Span;
import com.codename1.ui.TextSelection.Spans;
import com.codename1.ui.Toolbar;
import com.codename1.ui.animations.BubbleTransition;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.list.ListCellRenderer;

/// Allows a UI developer to completely customize the look of the application by
/// overriding drawing/sizing methods appropriately.
///
/// @author Chen Fishbein
///
/// #### Deprecated
///
/// @deprecated this class is still crucial for some features in Codename One. The deprecation is here to indicate
/// our desire to reduce usage/reliance on this class.
public abstract class LookAndFeel {
    private static final int fadeScrollEdgeStartAlpha = 0x999999;
    private static final int fadeScrollEdgeEndAlpha = 0;
    private final Image[] menuIcons = new Image[3];
    private final UIManager manager;
    private Component verticalScroll;
    private Component horizontalScroll;
    private Component verticalScrollThumb;
    private Component horizontalScrollThumb;
    /// Right-To-Left. Default false.
    private boolean rtl;
    private long tickerSpeed = 50;
    /// Tint color is set when a form is partially covered be it by a menu or by a
    /// dialog. A look and feel can override this default value.
    private int defaultFormTintColor = 0x77000000;
    /// This color is used to paint disable mode.
    private int disableColor = 0xcccccc;
    /// This member allows us to define a default animation that will draw the transition for
    /// entering a form
    private Transition defaultFormTransitionIn;
    /// This member allows us to define a default animation that will draw the transition for
    /// exiting a form
    private Transition defaultFormTransitionOut;
    /// This member allows us to define a default animation that will draw the transition for
    /// entering a menu
    private Transition defaultMenuTransitionIn;
    /// This member allows us to define a default animation that will draw the transition for
    /// exiting a menu
    private Transition defaultMenuTransitionOut;
    /// This member allows us to define a default animation that will draw the transition for
    /// entering a dialog
    private Transition defaultDialogTransitionIn;
    /// This member allows us to define a default animation that will draw the transition for
    /// exiting a form
    private Transition defaultDialogTransitionOut;
    /// Indicates whether lists and containers should have smooth scrolling by default
    private boolean defaultSmoothScrolling = true;
    /// Indicates whether lists and containers should scroll only via focus and thus "jump" when
    /// moving to a larger component as was the case in older versions of Codename One.
    private boolean focusScrolling;
    /// Indicates the default speed for smooth scrolling
    private int defaultSmoothScrollingSpeed = 150;
    private boolean scrollVisible;
    private boolean fadeScrollEdge;
    private boolean fadeScrollBar;
    private int fadeScrollBarSpeed = 5;
    private int fadeScrollEdgeLength = 15;
    private Image fadeScrollTop;
    private Image fadeScrollBottom;
    private Image fadeScrollRight;
    private Image fadeScrollLeft;
    private int textFieldCursorColor;
    private boolean backgroundImageDetermineSize;
    /// Indicates whether softbuttons should be reversed from their default orientation
    private boolean reverseSoftButtons;
    /// This renderer is assigned to all Forms Menu's by default.
    private ListCellRenderer menuRenderer;
    /// Allows defining a tactile touch device that vibrates when the user presses a component
    /// that should respond with tactile feedback on a touch device (e.g. vibrate).
    /// Setting this to 0 disables tactile feedback completely
    private int tactileTouchDuration = 0;
    /// Indicates whether labels should end with 3 points by default
    private boolean defaultEndsWith3Points = true;
    /// Indicates whether tensile drag should be active by default
    private boolean defaultTensileDrag = true;
    /// Indicates whether tensile highlight should be active by default
    private boolean defaultTensileHighlight = false;
    /// The MenuBar class
    private Class menuBar = MenuBar.class;
    private boolean defaultSnapToGrid;
    private boolean defaultAlwaysTensile;
    private Image tensileHighlightTopImage;
    private Image tensileHighlightBottomImage;
    private Image tensileGlowTopImage;
    private Image tensileGlowBottomImage;

    public LookAndFeel(UIManager manager) {
        this.manager = manager;
    }

    /// Every component binds itself to the look and feel thus allowing the look
    /// and feel to customize the component.  Binding occurs at the end of the
    /// constructor when the component is in a valid state and ready to be used.
    /// Notice that a component might be bound twice or more and it is the
    /// responsibility of the LookAndFeel to protect against that.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: component instance that may be customized by the look and feel
    public void bind(Component cmp) {
    }

    /// Invoked when a look and feel is removed, allows a look and feel to release
    /// resources related to binding components.
    ///
    /// #### See also
    ///
    /// - #bind(Component)
    public void uninstall() {
    }

    /// Invoked for drawing a button widget
    ///
    /// #### Parameters
    ///
    /// - `g`: graphics context
    ///
    /// - `b`: component to draw
    ///
    /// #### Deprecated
    ///
    /// this method is no longer used by the implementation, we shifted code away to improve performance
    public abstract void drawButton(Graphics g, Button b);

    /// Invoked for drawing a checkbox widget
    ///
    /// #### Parameters
    ///
    /// - `g`: graphics context
    ///
    /// - `cb`: component to draw
    public abstract void drawCheckBox(Graphics g, Button cb);

    /// Invoked for drawing a combo box widget
    ///
    /// #### Parameters
    ///
    /// - `g`: graphics context
    ///
    /// - `cb`: component to draw
    public abstract void drawComboBox(Graphics g, List cb);

    /// Invoked for drawing a label widget
    ///
    /// #### Parameters
    ///
    /// - `g`: graphics context
    ///
    /// - `l`: component to draw
    ///
    /// #### Deprecated
    ///
    /// this method is no longer used by the implementation, we shifted code away to improve performance
    public abstract void drawLabel(Graphics g, Label l);

    /// Calculates the text selection spans for a given label
    ///
    /// #### Parameters
    ///
    /// - `sel`: TextSelection instance
    ///
    /// - `l`: Label
    ///
    /// #### Returns
    ///
    /// A span representing the positions of characters in the label
    ///
    /// #### Since
    ///
    /// 7.0
    ///
    /// #### See also
    ///
    /// - TextSelection
    public abstract Span calculateLabelSpan(TextSelection sel, Label l);

    /// Invoked for drawing a list widget
    ///
    /// #### Parameters
    ///
    /// - `g`: graphics context
    ///
    /// - `l`: component to draw
    public abstract void drawList(Graphics g, List l);

    /// Invoked for drawing the radio button widget
    ///
    /// #### Parameters
    ///
    /// - `g`: graphics context
    ///
    /// - `rb`: component to draw
    public abstract void drawRadioButton(Graphics g, Button rb);

    /// Draw the given text area
    ///
    /// #### Parameters
    ///
    /// - `g`: graphics context
    ///
    /// - `ta`: component to draw
    public abstract void drawTextArea(Graphics g, TextArea ta);

    /// Calculates the Spans used in text selection for a given text area.
    ///
    /// #### Parameters
    ///
    /// - `sel`: The current TextSelection instance.
    ///
    /// - `ta`: The TextArea to calculate spans for.
    ///
    /// #### Returns
    ///
    /// The spans for the given text field.
    ///
    /// #### Since
    ///
    /// 7.0
    public abstract Spans calculateTextAreaSpan(TextSelection sel, TextArea ta);

    /// Draws the text field without its cursor which is drawn in a separate method
    /// input mode indication can also be drawn using this method.
    ///
    /// #### Parameters
    ///
    /// - `g`: graphics context
    ///
    /// - `ta`: component to draw
    public abstract void drawTextField(Graphics g, TextArea ta);

    /// Calculates the Spans used in text selection for a given text field.
    ///
    /// #### Parameters
    ///
    /// - `sel`: The current TextSelection instance.
    ///
    /// - `ta`: The textfield to calculate spans for.
    ///
    /// #### Returns
    ///
    /// The spans for the given text field.
    ///
    /// #### Since
    ///
    /// 7.0
    public abstract Spans calculateTextFieldSpan(TextSelection sel, TextArea ta);

    /// Draws the cursor of the text field, blinking is handled simply by avoiding
    /// a call to this method.
    ///
    /// #### Parameters
    ///
    /// - `g`: graphics context
    ///
    /// - `ta`: component to draw
    public abstract void drawTextFieldCursor(Graphics g, TextArea ta);

    /// Calculate the preferred size of the component
    ///
    /// #### Parameters
    ///
    /// - `b`: component whose size should be calculated
    ///
    /// #### Returns
    ///
    /// the preferred size for the button
    public abstract Dimension getButtonPreferredSize(Button b);

    /// Calculate the preferred size of the component
    ///
    /// #### Parameters
    ///
    /// - `cb`: component whose size should be calculated
    ///
    /// #### Returns
    ///
    /// the preferred size for the component
    public abstract Dimension getCheckBoxPreferredSize(Button cb);

    /// Calculate the preferred size of the component
    ///
    /// #### Parameters
    ///
    /// - `l`: component whose size should be calculated
    ///
    /// #### Returns
    ///
    /// the preferred size for the component
    public abstract Dimension getLabelPreferredSize(Label l);

    /// Calculate the preferred size of the component
    ///
    /// #### Parameters
    ///
    /// - `l`: component whose size should be calculated
    ///
    /// #### Returns
    ///
    /// the preferred size for the component
    public abstract Dimension getListPreferredSize(List l);

    /// Calculate the preferred size of the component
    ///
    /// #### Parameters
    ///
    /// - `rb`: component whose size should be calculated
    ///
    /// #### Returns
    ///
    /// the preferred size for the component
    public abstract Dimension getRadioButtonPreferredSize(Button rb);

    //public abstract Dimension getSpinnerPreferredSize(Spinner sp);

    /// Calculate the preferred size of the component
    ///
    /// #### Parameters
    ///
    /// - `ta`: component whose size should be calculated
    ///
    /// - `pref`: indicates whether preferred or scroll size should be returned
    ///
    /// #### Returns
    ///
    /// the preferred size for the component
    public abstract Dimension getTextAreaSize(TextArea ta, boolean pref);

    /// Calculate the preferred size of the component
    ///
    /// #### Parameters
    ///
    /// - `ta`: component whose size should be calculated
    ///
    /// #### Returns
    ///
    /// the preferred size for the component
    public abstract Dimension getTextFieldPreferredSize(TextArea ta);

    /// Calculate the preferred size of the component
    ///
    /// #### Parameters
    ///
    /// - `box`: component whose size should be calculated
    ///
    /// #### Returns
    ///
    /// the preferred size for the component
    public abstract Dimension getComboBoxPreferredSize(List box);

    /// Draws a vertical scroll bar in the given component
    ///
    /// #### Parameters
    ///
    /// - `g`: graphics context
    ///
    /// - `c`: component to draw on
    ///
    /// - `offsetRatio`: ratio of the scroll bar from 0 to 1
    ///
    /// - `blockSizeRatio`: block size for the scroll from 0 to 1
    public void drawVerticalScroll(Graphics g, Component c, float offsetRatio, float blockSizeRatio) {
        if (verticalScroll == null) {
            initScroll();
        }
        int borderW = 0;
        if (c.getStyle().getBorder() != null) {
            borderW = c.getStyle().getBorder().getThickness();
        }
        int x = c.getX();
        if (!c.isRTL()) {
            x += c.getWidth() - getVerticalScrollWidth() - borderW;
        } else {
            x += borderW;
        }
        int y = c.getY();
        int height = c.getHeight();
        int width = getVerticalScrollWidth();
        drawScroll(g, c, offsetRatio, blockSizeRatio, true, x, y, width, height, verticalScroll, verticalScrollThumb);
        if (fadeScrollEdge) {
            if (offsetRatio > 0) {
                if (fadeScrollTop == null || Display.getInstance().getDisplayWidth() != fadeScrollTop.getWidth()) {
                    fadeScrollTop = generateFadeImage(fadeScrollEdgeStartAlpha, fadeScrollEdgeEndAlpha, c.getStyle().getBgColor(), false,
                            Display.getInstance().getDisplayWidth(), fadeScrollEdgeLength);
                }
                g.drawImage(fadeScrollTop, c.getX(), c.getY());
            }
            if (offsetRatio < 1 - blockSizeRatio) {
                // special case since when scrolling with the physical keys we leave
                // the margin out of the equasion
                int l = c.getScrollY() + c.getHeight() +
                        c.getStyle().getVerticalMargins() +
                        c.getStyle().getVerticalPadding();

                int totalScroll = c.getScrollDimension().getHeight();
                if (l >= totalScroll) {
                    return;
                }
                if (fadeScrollBottom == null || Display.getInstance().getDisplayWidth() != fadeScrollBottom.getWidth()) {
                    fadeScrollBottom = generateFadeImage(fadeScrollEdgeEndAlpha, fadeScrollEdgeStartAlpha, c.getStyle().getBgColor(), false,
                            Display.getInstance().getDisplayWidth(), fadeScrollEdgeLength);
                }
                g.drawImage(fadeScrollBottom, c.getX(), c.getY() + c.getHeight() - fadeScrollBottom.getHeight());
            }
        }
    }

    private Image generateFadeImage(int startColor, int endColor, int bgColor, boolean horizontal, int width, int height) {
        Image mute = Image.createImage(width, height);
        mute.getGraphics().fillLinearGradient(startColor, endColor, 0, 0, width, height, horizontal);
        Object mask = mute.createMask();
        mute = Image.createImage(width, height);
        Graphics gr = mute.getGraphics();
        gr.setColor(bgColor);
        gr.fillRect(0, 0, width, height);
        return mute.applyMask(mask);
    }

    /// Draws a horizontal scroll bar in the given component
    ///
    /// #### Parameters
    ///
    /// - `g`: graphics context
    ///
    /// - `c`: component to draw on
    ///
    /// - `offsetRatio`: ratio of the scroll bar from 0 to 1
    ///
    /// - `blockSizeRatio`: block size for the scroll from 0 to 1
    public void drawHorizontalScroll(Graphics g, Component c, float offsetRatio, float blockSizeRatio) {
        if (horizontalScroll == null) {
            initScroll();
        }
        int borderH = 0;
        if (c.getStyle().getBorder() != null) {
            borderH = c.getStyle().getBorder().getThickness();
        }
        int x = c.getX();
        int y = c.getY() + c.getHeight() - getHorizontalScrollHeight() - borderH;

        int width = c.getWidth();
        int height = getHorizontalScrollHeight();
        drawScroll(g, c, offsetRatio, blockSizeRatio, false, x, y, width, height, horizontalScroll, horizontalScrollThumb);
        if (fadeScrollEdge) {
            if (offsetRatio > 0) {
                if (fadeScrollLeft == null || Display.getInstance().getDisplayHeight() != fadeScrollLeft.getHeight()) {
                    fadeScrollLeft = generateFadeImage(fadeScrollEdgeStartAlpha, fadeScrollEdgeEndAlpha, c.getStyle().getBgColor(), true, fadeScrollEdgeLength,
                            Display.getInstance().getDisplayHeight());
                }
                g.drawImage(fadeScrollLeft, c.getX(), c.getY());
            }
            if (offsetRatio < 1 - blockSizeRatio) {
                // special case since when scrolling with the physical keys we leave
                // the margin out of the equasion
                int l = c.getScrollX() + c.getWidth() +
                        c.getStyle().getHorizontalMargins() +
                        c.getStyle().getHorizontalPadding();
                int totalScroll = c.getScrollDimension().getWidth();
                if (l >= totalScroll) {
                    return;
                }
                if (fadeScrollRight == null || Display.getInstance().getDisplayHeight() != fadeScrollRight.getHeight()) {
                    fadeScrollRight = generateFadeImage(fadeScrollEdgeEndAlpha, fadeScrollEdgeStartAlpha, c.getStyle().getBgColor(), true, fadeScrollEdgeLength,
                            Display.getInstance().getDisplayHeight());
                }
                g.drawImage(fadeScrollRight, c.getX() + c.getWidth() - fadeScrollRight.getWidth(), c.getY());
            }
        }
    }

    private void drawScroll(Graphics g, Component c, float offsetRatio,
                            float blockSizeRatio, boolean isVertical, int x, int y, int width, int height,
                            Component scroll, Component scrollThumb) {
        Style scrollStyle = scroll.getUnselectedStyle();
        Style scrollThumbStyle = scrollThumb.getUnselectedStyle();

        int alpha = scrollStyle.getBgTransparency() & 0xff;
        int thumbAlpha = scrollThumbStyle.getBgTransparency() & 0xff;
        int originalAlpha = g.getAlpha();
        if (fadeScrollBar) {
            if (scrollStyle.getBgTransparency() != 0) {
                scrollStyle.setBgTransparency(c.getScrollOpacity(), true);
            }
            scrollThumbStyle.setBgTransparency(c.getScrollOpacity(), true);
            g.setAlpha(c.getScrollOpacity());
        }
        // take margin into consideration when positioning the scroll
        int marginLeft = scrollStyle.getMarginLeft(c.isRTL());
        int marginTop = scrollStyle.getMarginTop();
        x += marginLeft;
        width -= (marginLeft + scrollStyle.getMarginRight(c.isRTL()));
        y += marginTop;
        height -= (marginTop + scrollStyle.getMarginBottom());

        scroll.setX(x);
        scroll.setY(y);
        scroll.setWidth(width);
        scroll.setHeight(height);

        int cx = g.getClipX();
        int cy = g.getClipY();
        int cw = g.getClipWidth();
        int ch = g.getClipHeight();

        scroll.paintComponent(g);

        marginLeft = scrollThumbStyle.getMarginLeft(c.isRTL());
        marginTop = scrollThumbStyle.getMarginTop();
        x += marginLeft;
        width -= (marginLeft + scrollThumbStyle.getMarginRight(c.isRTL()));
        y += marginTop;
        height -= (marginTop + scrollThumbStyle.getMarginBottom());

        int offset;
        int blockSize;

        if (isVertical) {
            blockSize = (int) (c.getHeight() * blockSizeRatio) + 2;
            offset = (int) ((c.getHeight()) * offsetRatio);
        } else {
            blockSize = (int) (c.getWidth() * blockSizeRatio) + 2;
            offset = (int) ((c.getWidth()) * offsetRatio);
        }

        if (isVertical) {
            scrollThumb.setX(x);
            scrollThumb.setY(y + offset);
            scrollThumb.setWidth(width);
            scrollThumb.setHeight(blockSize);
        } else {
            scrollThumb.setX(x + offset);
            scrollThumb.setY(y);
            scrollThumb.setWidth(blockSize);
            scrollThumb.setHeight(height);
        }

        g.setClip(cx, cy, cw, ch);
        scrollThumb.paintComponent(g);
        g.setClip(cx, cy, cw, ch);
        if (fadeScrollBar) {
            scrollStyle.setBgTransparency(alpha, true);
            scrollThumbStyle.setBgTransparency(thumbAlpha, true);
            g.setAlpha(originalAlpha);
        }
    }


    /// Sets the foreground color and font for a generic component, reuse-able by most component
    /// drawing code
    ///
    /// #### Parameters
    ///
    /// - `g`: graphics context
    ///
    /// - `c`: component from which fg styles should be set
    public void setFG(Graphics g, Component c) {
        Style s = c.getStyle();
        g.setFont(s.getFont());
        g.setColor(s.getFgColor());
    }

    /// Returns the default width of a vertical scroll bar
    ///
    /// #### Returns
    ///
    /// default width of a vertical scroll bar
    public int getVerticalScrollWidth() {
        if (verticalScroll == null) {
            initScroll();
        }
        Style scrollStyle = verticalScroll.getStyle();

        // bidi doesn't matter for width calculations
        return scrollStyle.getMarginLeftNoRTL() + scrollStyle.getMarginRightNoRTL() +
                scrollStyle.getPaddingLeftNoRTL() + scrollStyle.getPaddingRightNoRTL();
    }

    /// Returns the default height of a horizontal scroll bar
    ///
    /// #### Returns
    ///
    /// default height of a horizontal scroll bar
    public int getHorizontalScrollHeight() {
        if (horizontalScroll == null) {
            initScroll();
        }
        Style scrollStyle = horizontalScroll.getStyle();

        // bidi doesn't matter for height calculations
        return scrollStyle.getMarginTop() + scrollStyle.getMarginBottom() +
                scrollStyle.getPaddingTop() + scrollStyle.getPaddingBottom();
    }

    /// Draws generic component border
    void drawBorder(Graphics g, Component c, int color, int borderWidth) {
        drawBorder(g, c, color, color, borderWidth);
    }

    /// Draws generic component border
    void drawBorder(Graphics g, Component c, int topAndRightColor, int bottomAndLeftColor, int borderWidth) {
        g.setColor(topAndRightColor);     //Text Component upper border color

        g.fillRect(c.getX(), c.getY(), c.getWidth(), borderWidth);
        g.fillRect(c.getX(), c.getY(), borderWidth, c.getHeight());
        g.setColor(bottomAndLeftColor);     //Text Component lower border color

        g.fillRect(c.getX(), c.getY() + c.getHeight() - borderWidth, c.getWidth(), borderWidth);
        g.fillRect(c.getX() + c.getWidth() - borderWidth, c.getY(), borderWidth, c.getHeight());
    }

    /// Allows us to define a default animation that will draw the transition for
    /// entering a form
    ///
    /// #### Returns
    ///
    /// default transition
    public Transition getDefaultFormTransitionIn() {
        return defaultFormTransitionIn;
    }

    /// Allows us to define a default animation that will draw the transition for
    /// entering a form
    ///
    /// #### Parameters
    ///
    /// - `defaultFormTransitionIn`: the default transition
    public void setDefaultFormTransitionIn(Transition defaultFormTransitionIn) {
        this.defaultFormTransitionIn = defaultFormTransitionIn;
    }

    /// Allows us to define a default animation that will draw the transition for
    /// exiting a form
    ///
    /// #### Returns
    ///
    /// default transition
    public Transition getDefaultFormTransitionOut() {
        return defaultFormTransitionOut;
    }

    /// Allows us to define a default animation that will draw the transition for
    /// exiting a form
    ///
    /// #### Parameters
    ///
    /// - `defaultFormTransitionOut`: the default transition
    public void setDefaultFormTransitionOut(Transition defaultFormTransitionOut) {
        this.defaultFormTransitionOut = defaultFormTransitionOut;
    }

    /// Allows us to define a default animation that will draw the transition for
    /// entering a Menu
    ///
    /// #### Returns
    ///
    /// default transition
    public Transition getDefaultMenuTransitionIn() {
        return defaultMenuTransitionIn;
    }

    /// Allows us to define a default animation that will draw the transition for
    /// entering a Menu
    ///
    /// #### Parameters
    ///
    /// - `defaultMenuTransitionIn`: the default transition
    public void setDefaultMenuTransitionIn(Transition defaultMenuTransitionIn) {
        this.defaultMenuTransitionIn = defaultMenuTransitionIn;
    }

    /// Allows us to define a default animation that will draw the transition for
    /// exiting a Menu
    ///
    /// #### Returns
    ///
    /// default transition
    public Transition getDefaultMenuTransitionOut() {
        return defaultMenuTransitionOut;
    }

    /// Allows us to define a default animation that will draw the transition for
    /// exiting a Menu
    ///
    /// #### Parameters
    ///
    /// - `defaultMenuTransitionOut`: the default transition
    public void setDefaultMenuTransitionOut(Transition defaultMenuTransitionOut) {
        this.defaultMenuTransitionOut = defaultMenuTransitionOut;
    }

    /// Allows us to define a default animation that will draw the transition for
    /// entering a dialog
    ///
    /// #### Returns
    ///
    /// default transition
    public Transition getDefaultDialogTransitionIn() {
        return defaultDialogTransitionIn;
    }

    /// Allows us to define a default animation that will draw the transition for
    /// entering a dialog
    ///
    /// #### Parameters
    ///
    /// - `defaultDialogTransitionIn`: the default transition
    public void setDefaultDialogTransitionIn(Transition defaultDialogTransitionIn) {
        this.defaultDialogTransitionIn = defaultDialogTransitionIn;
    }

    /// Allows us to define a default animation that will draw the transition for
    /// exiting a dialog
    ///
    /// #### Returns
    ///
    /// default transition
    public Transition getDefaultDialogTransitionOut() {
        return defaultDialogTransitionOut;
    }

    /// Allows us to define a default animation that will draw the transition for
    /// exiting a dialog
    ///
    /// #### Parameters
    ///
    /// - `defaultDialogTransitionOut`: the default transition
    public void setDefaultDialogTransitionOut(Transition defaultDialogTransitionOut) {
        this.defaultDialogTransitionOut = defaultDialogTransitionOut;
    }

    /// Tint color is set when a form is partially covered be it by a menu or by a
    /// dialog. A look and feel can override this default value.
    ///
    /// #### Returns
    ///
    /// default tint color
    public int getDefaultFormTintColor() {
        return defaultFormTintColor;
    }

    /// Tint color is set when a form is partially covered be it by a menu or by a
    /// dialog. A look and feel can override this default value.
    ///
    /// #### Parameters
    ///
    /// - `defaultFormTintColor`: the default tint color
    public void setDefaultFormTintColor(int defaultFormTintColor) {
        this.defaultFormTintColor = defaultFormTintColor;
    }

    /// This color is used to paint disable mode text color.
    ///
    /// #### Returns
    ///
    /// the color value
    public int getDisableColor() {
        return disableColor;
    }

    /// Simple setter to disable color
    ///
    /// #### Parameters
    ///
    /// - `disableColor`: the disable color value
    public void setDisableColor(int disableColor) {
        this.disableColor = disableColor;
    }

    /// Indicates whether lists and containers should have smooth scrolling by default
    ///
    /// #### Returns
    ///
    /// true if smooth scrolling should be on by default
    public boolean isDefaultSmoothScrolling() {
        return defaultSmoothScrolling;
    }

    /// Indicates whether lists and containers should have smooth scrolling by default
    ///
    /// #### Parameters
    ///
    /// - `defaultSmoothScrolling`: true if smooth scrolling should be on by default
    public void setDefaultSmoothScrolling(boolean defaultSmoothScrolling) {
        this.defaultSmoothScrolling = defaultSmoothScrolling;
    }

    /// Indicates the default speed for smooth scrolling
    ///
    /// #### Returns
    ///
    /// speed for smooth scrollin
    public int getDefaultSmoothScrollingSpeed() {
        return defaultSmoothScrollingSpeed;
    }

    /// Indicates the default speed for smooth scrolling
    ///
    /// #### Parameters
    ///
    /// - `defaultSmoothScrollingSpeed`: speed for smooth scrollin
    public void setDefaultSmoothScrollingSpeed(int defaultSmoothScrollingSpeed) {
        this.defaultSmoothScrollingSpeed = defaultSmoothScrollingSpeed;
    }

    /// Indicates whether softbuttons should be reversed from their default orientation
    ///
    /// #### Returns
    ///
    /// true if softbuttons should be reversed
    public boolean isReverseSoftButtons() {
        return reverseSoftButtons;
    }

    /// Indicates whether softbuttons should be reversed from their default orientation
    ///
    /// #### Parameters
    ///
    /// - `reverseSoftButtons`: true if softbuttons should be reversed
    public void setReverseSoftButtons(boolean reverseSoftButtons) {
        this.reverseSoftButtons = reverseSoftButtons;
    }

    /// This method returns the MenuBar class.
    ///
    /// #### Returns
    ///
    /// the MenuBar class.
    ///
    /// #### Deprecated
    ///
    /// @deprecated this is no longer supported, Toolbar should be used as
    /// the extension point
    public Class getMenuBarClass() {
        return menuBar;
    }

    /// Simple setter for the MenuBar Class
    ///
    /// #### Parameters
    ///
    /// - `menuBar`
    ///
    /// #### Deprecated
    ///
    /// @deprecated this is no longer supported, Toolbar should be used as
    /// the extension point
    public void setMenuBarClass(Class menuBar) {
        this.menuBar = menuBar;
    }


    /// Returns the Menu default renderer
    ///
    /// #### Returns
    ///
    /// default renderer for the menu
    public ListCellRenderer getMenuRenderer() {
        return menuRenderer;
    }

    /// Sets the Menu default renderer
    ///
    /// #### Parameters
    ///
    /// - `menuRenderer`: default renderer for the menu
    public void setMenuRenderer(ListCellRenderer menuRenderer) {
        this.menuRenderer = menuRenderer;
    }

    /// Sets globally the Menu icons
    ///
    /// #### Parameters
    ///
    /// - `select`: select icon
    ///
    /// - `cancel`: cancel icon
    ///
    /// - `menu`: menu icon
    public void setMenuIcons(Image select, Image cancel, Image menu) {
        menuIcons[0] = select;
        menuIcons[1] = cancel;
        menuIcons[2] = menu;

    }

    /// Simple getter for the menu icons
    ///
    /// #### Returns
    ///
    /// @return an Image array at size of 3, where the first is the select image
    /// the second is the cancel image and the last is the menu image.
    public Image[] getMenuIcons() {
        return menuIcons;
    }

    /// Gets the ticker speed
    ///
    /// #### Returns
    ///
    /// ticker speed in milliseconds
    public long getTickerSpeed() {
        return tickerSpeed;
    }

    /// Sets the ticker speed
    ///
    /// #### Parameters
    ///
    /// - `tickerSpeed`: the speed in milliseconds
    public void setTickerSpeed(long tickerSpeed) {
        this.tickerSpeed = tickerSpeed;
    }

    private void initScroll() {
        verticalScroll = new Label();
        verticalScroll.setUIID("Scroll");
        horizontalScroll = new Label();
        horizontalScroll.setUIID("HorizontalScroll");
        verticalScrollThumb = new Label();
        verticalScrollThumb.setUIID("ScrollThumb");
        horizontalScrollThumb = new Label();
        horizontalScrollThumb.setUIID("HorizontalScrollThumb");
    }

    /// This method is a callback to the LookAndFeel when a theme is being
    /// changed in the UIManager
    ///
    /// #### Parameters
    ///
    /// - `completeClear`: indicates that the theme is set and not added
    public void refreshTheme(boolean completeClear) {
        fadeScrollTop = null;
        fadeScrollBottom = null;
        fadeScrollRight = null;
        fadeScrollLeft = null;
        initScroll();
        if (menuRenderer != null) {
            if (menuRenderer instanceof Component) {
                ((Component) menuRenderer).refreshTheme();
            }
        }

        Toolbar.setGlobalToolbar(manager.isThemeConstant("globalToobarBool", Toolbar.isGlobalToolbar()));

        boolean isTouch = Display.getInstance().isTouchScreenDevice();
        scrollVisible = manager.isThemeConstant("scrollVisibleBool", true);
        fadeScrollEdge = manager.isThemeConstant("fadeScrollEdgeBool", false);
        fadeScrollEdgeLength = manager.getThemeConstant("fadeScrollEdgeInt", fadeScrollEdgeLength);
        fadeScrollBar = manager.isThemeConstant("fadeScrollBarBool", false);

        InputComponent.setMultiLineErrorMessage(manager.isThemeConstant("inputComponentErrorMultilineBool", false));

        try {
            tickerSpeed = Long.parseLong(manager.getThemeConstant("tickerSpeedInt", "" + tickerSpeed));
            Label.setDefaultTickerEnabled(tickerSpeed >= 1);
        } catch (NumberFormatException err) {
            Log.e(err);
        }

        Button.setCapsTextDefault(manager.isThemeConstant("capsButtonTextBool", false));
        Button.setButtonRippleEffectDefault(manager.isThemeConstant("buttonRippleBool", false));

        defaultFormTintColor = (int) Long.parseLong(manager.getThemeConstant("tintColor", Integer.toHexString(defaultFormTintColor)), 16);
        disableColor = Integer.parseInt(manager.getThemeConstant("disabledColor", Integer.toHexString(disableColor)), 16);
        Dialog.setDefaultDialogPosition(manager.getThemeConstant("dialogPosition", Dialog.getDefaultDialogPosition()));
        Dialog.setCommandsAsButtons(manager.isThemeConstant("dialogButtonCommandsBool", Dialog.isCommandsAsButtons()));
        Dialog.setDefaultBlurBackgroundRadius(manager.getThemeConstant("dialogBlurRadiusInt", (int) Dialog.getDefaultBlurBackgroundRadius()));

        List.setDefaultIgnoreFocusComponentWhenUnfocused(manager.isThemeConstant("ignorListFocusBool", List.isDefaultIgnoreFocusComponentWhenUnfocused()));

        if (isTouch) {
            Display.getInstance().setPureTouch(manager.isThemeConstant("pureTouchBool", Display.getInstance().isPureTouch()));
        }

        int defaultTransitionSpeed = Integer.parseInt(manager.getThemeConstant("transitionSpeedInt", "220"));
        String slideDir = manager.getThemeConstant("slideDirection", "horizontal");
        String dialogSlideDir = manager.getThemeConstant("dlgSlideDirection", "vertical");
        String menuSlideDir = manager.getThemeConstant("menuSlideDirection", dialogSlideDir);
        boolean outdir = manager.isThemeConstant("slideOutDirBool", false);
        boolean indir = manager.isThemeConstant("slideInDirBool", true);
        boolean dialogOutdir = manager.isThemeConstant("dlgSlideOutDirBool", false);
        boolean dialogIndir = manager.isThemeConstant("dlgSlideInDirBool", true);
        boolean menuOutdir = manager.isThemeConstant("menuSlideOutDirBool", false);
        boolean menuIndir = manager.isThemeConstant("menuSlideInDirBool", true);
        defaultFormTransitionIn = getTransitionConstant(defaultFormTransitionIn, "formTransitionIn", slideDir, defaultTransitionSpeed, indir);
        defaultFormTransitionOut = getTransitionConstant(defaultFormTransitionOut, "formTransitionOut", slideDir, defaultTransitionSpeed, outdir);
        defaultMenuTransitionIn = getTransitionConstant(defaultMenuTransitionIn, "menuTransitionIn", menuSlideDir, defaultTransitionSpeed, menuIndir);
        defaultMenuTransitionOut = getTransitionConstant(defaultMenuTransitionOut, "menuTransitionOut", menuSlideDir, defaultTransitionSpeed, menuOutdir);
        defaultDialogTransitionIn = getTransitionConstant(defaultDialogTransitionIn, "dialogTransitionIn", dialogSlideDir, defaultTransitionSpeed, dialogIndir);
        defaultDialogTransitionOut = getTransitionConstant(defaultDialogTransitionOut, "dialogTransitionOut", dialogSlideDir, defaultTransitionSpeed, dialogOutdir);
        initCommandBehaviorConstant(manager.getThemeConstant("commandBehavior", null), completeClear);
        reverseSoftButtons = manager.isThemeConstant("reverseSoftButtonsBool", reverseSoftButtons);
        textFieldCursorColor = manager.getThemeConstant("textFieldCursorColorInt", 0);

        String gap = manager.getThemeConstant("labelGap", null);
        if (gap != null) {
            Label.setDefaultGap(Display.getInstance().convertToPixels(Util.toFloatValue(gap)));
        }

        InfiniteProgress.setDefaultMaterialDesignMode(
                manager.isThemeConstant("infiniteProgressMaterialModeBool", false));
        InfiniteProgress.setDefaultMaterialDesignColor(
                manager.getThemeConstant("infiniteProgressMaterialColorInt", 0x6200ee));

        TextArea.setDefaultValign(manager.getThemeConstant("textCmpVAlignInt", TextArea.getDefaultValign()));
        defaultSnapToGrid = manager.isThemeConstant("snapGridBool", false);
        defaultAlwaysTensile = manager.isThemeConstant("alwaysTensileBool", false);
        defaultTensileDrag = manager.isThemeConstant("tensileDragBool", true);
        defaultEndsWith3Points = manager.isThemeConstant("endsWith3PointsBool", false);
        defaultTensileHighlight = manager.isThemeConstant("tensileHighlightBool", false);
        tensileHighlightBottomImage = null;
        tensileHighlightTopImage = null;
        if (defaultTensileHighlight) {
            tensileHighlightBottomImage = manager.getThemeImageConstant("tensileHighlightBottomImage");
            tensileHighlightTopImage = manager.getThemeImageConstant("tensileHighlightTopImage");
            tensileGlowBottomImage = manager.getThemeImageConstant("tensileGlowBottomImage");
            tensileGlowTopImage = manager.getThemeImageConstant("tensileGlowTopImage");
            if (tensileHighlightBottomImage != null && tensileHighlightTopImage != null) {
                defaultTensileDrag = true;
                defaultAlwaysTensile = false;
            } else {
                defaultTensileHighlight = false;
            }
        }
        backgroundImageDetermineSize = manager.isThemeConstant("bgImageSizeBool", false);
    }

    private void initCommandBehaviorConstant(String c, boolean complete) {
        if (c != null) {
            if ("SoftKey".equalsIgnoreCase(c)) {
                Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_SOFTKEY);
                return;
            }
            if ("Touch".equalsIgnoreCase(c)) {
                Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_TOUCH_MENU);
                return;
            }
            if ("Bar".equalsIgnoreCase(c)) {
                Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_BUTTON_BAR);
                return;
            }
            if ("Title".equalsIgnoreCase(c)) {
                Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_BACK);
                return;
            }
            if ("Right".equalsIgnoreCase(c)) {
                Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_RIGHT);
                return;
            }
            if ("Native".equalsIgnoreCase(c)) {
                Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_NATIVE);
                return;
            }
            if ("ICS".equalsIgnoreCase(c)) {
                Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_ICS);
                return;
            }
            if ("SIDE".equalsIgnoreCase(c)) {
                Log.p("WARNING: Theme sets the commandBehavior constant which is deprecated.  Please update the theme to NOT include this theme constant.  Using commandBehavior may cause your app to perform in unexpected ways.  In particular, using SIDE command behavior in conjunction with Toolbar.setOnTopSideMenu(true) may result in runtime exceptions.", Log.WARNING);
                Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_SIDE_NAVIGATION);
                setMenuBarClass(SideMenuBar.class);
            }
        } else {
            if (complete) {
                Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_DEFAULT);
            }
        }
    }

    private Transition getTransitionConstant(Transition t, String constant, String slideDir, int speed, boolean forward) {
        Image img = manager.getThemeImageConstant(constant + "Image");
        if (img != null) {
            return CommonTransitions.createTimeline(img);
        }
        String val = manager.getThemeConstant(constant, null);
        if (val == null) {
            return t;
        }
        if ("empty".equalsIgnoreCase(val)) {
            return CommonTransitions.createEmpty();
        }
        if ("slide".equalsIgnoreCase(val)) {
            if ("horizontal".equalsIgnoreCase(slideDir)) {
                return CommonTransitions.createSlide(CommonTransitions.SLIDE_HORIZONTAL, forward, speed);
            } else {
                return CommonTransitions.createSlide(CommonTransitions.SLIDE_VERTICAL, forward, speed);
            }
        }
        if ("cover".equalsIgnoreCase(val)) {
            if ("horizontal".equalsIgnoreCase(slideDir)) {
                return CommonTransitions.createCover(CommonTransitions.SLIDE_HORIZONTAL, forward, speed);
            } else {
                return CommonTransitions.createCover(CommonTransitions.SLIDE_VERTICAL, forward, speed);
            }
        }
        if ("uncover".equalsIgnoreCase(val)) {
            if ("horizontal".equalsIgnoreCase(slideDir)) {
                return CommonTransitions.createUncover(CommonTransitions.SLIDE_HORIZONTAL, forward, speed);
            } else {
                return CommonTransitions.createUncover(CommonTransitions.SLIDE_VERTICAL, forward, speed);
            }
        }
        if ("fslide".equalsIgnoreCase(val)) {
            if ("horizontal".equalsIgnoreCase(slideDir)) {
                return CommonTransitions.createFastSlide(CommonTransitions.SLIDE_HORIZONTAL, forward, speed);
            } else {
                return CommonTransitions.createFastSlide(CommonTransitions.SLIDE_VERTICAL, forward, speed);
            }
        }
        if ("fade".equalsIgnoreCase(val)) {
            return CommonTransitions.createFade(speed);
        }
        if ("slidefade".equalsIgnoreCase(val)) {
            return CommonTransitions.createSlideFadeTitle(forward, speed);
        }
        if ("pulse".equalsIgnoreCase(val)) {
            return CommonTransitions.createDialogPulsate();
        }
        if ("bubble".equalsIgnoreCase(val)) {
            BubbleTransition transition = new BubbleTransition(speed);
            transition.setRoundBubble(false);
            return transition;
        }
        return t;
    }

    /// Indicates whether the menu UI should target a touch based device or a
    /// standard cell phone
    ///
    /// #### Returns
    ///
    /// true for touch menus
    ///
    /// #### Deprecated
    ///
    /// use Display.getCommandBehavior() == Display.COMMAND_BEHAVIOR_TOUCH_MENU
    public boolean isTouchMenus() {
        int t = Display.getInstance().getCommandBehavior();
        return t == Display.COMMAND_BEHAVIOR_TOUCH_MENU ||
                (t == Display.COMMAND_BEHAVIOR_DEFAULT && Display.getInstance().isTouchScreenDevice());
    }

    /// Indicates whether the menu UI should target a touch based device or a
    /// standard cell phone
    ///
    /// #### Parameters
    ///
    /// - `touchMenus`: true to enable touch menus false to disable
    ///
    /// #### Deprecated
    ///
    /// use Display.setCommandBehavior(Display.COMMAND_BEHAVIOR_TOUCH_MENU)
    public void setTouchMenus(boolean touchMenus) {
        if (touchMenus) {
            Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_TOUCH_MENU);
        } else {
            Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_SOFTKEY);
        }
    }

    /// Use this to check if the LookAndFeel is in RTL mode
    ///
    /// #### Returns
    ///
    /// true if the LookAndFeel is in right-to-left mode, false otherwise
    public boolean isRTL() {
        return rtl;
    }

    /// Sets this LookAndFeel to operate in right-to-left mode.
    ///
    /// #### Parameters
    ///
    /// - `rtl`: - true if right-to-left, false if left-to-right
    public void setRTL(boolean rtl) {
        if (UIManager.localeAccessible) {
            this.rtl = rtl;
            if (rtl) {
                Display.getInstance().setBidiAlgorithm(true);
            }
        }
    }

    /// Allows defining a tactile touch device that vibrates when the user presses a component
    /// that should respond with tactile feedback on a touch device (e.g. vibrate).
    /// Setting this to 0 disables tactile feedback completely
    ///
    /// #### Returns
    ///
    /// the tactileTouchDuration
    public int getTactileTouchDuration() {
        return tactileTouchDuration;
    }

    /// Allows defining a tactile touch device that vibrates when the user presses a component
    /// that should respond with tactile feedback on a touch device (e.g. vibrate).
    /// Setting this to 0 disables tactile feedback completely
    ///
    /// #### Parameters
    ///
    /// - `tactileTouchDuration`: the duration of vibration
    public void setTactileTouchDuration(int tactileTouchDuration) {
        this.tactileTouchDuration = tactileTouchDuration;
    }

    /// Indicates whether labels should end with 3 points by default
    ///
    /// #### Returns
    ///
    /// whether labels should end with 3 points by default
    public boolean isDefaultEndsWith3Points() {
        return defaultEndsWith3Points;
    }

    /// Indicates whether labels should end with 3 points by default
    ///
    /// #### Parameters
    ///
    /// - `defaultEndsWith3Points`: True to indicates that labels should end with 3 points by default
    public void setDefaultEndsWith3Points(boolean defaultEndsWith3Points) {
        this.defaultEndsWith3Points = defaultEndsWith3Points;
    }

    /// Indicates whether tensile drag should be active by default
    ///
    /// #### Returns
    ///
    /// whether tensile drag should be active by default
    public boolean isDefaultTensileDrag() {
        return defaultTensileDrag;
    }

    /// Indicates whether tensile drag should be active by default
    ///
    /// #### Parameters
    ///
    /// - `defaultTensileDrag`: true if tensile drag should be active by default
    public void setDefaultTensileDrag(boolean defaultTensileDrag) {
        this.defaultTensileDrag = defaultTensileDrag;
    }

    /// Indicates whether lists and containers should scroll only via focus and thus "jump" when
    /// moving to a larger component as was the case in older versions of Codename One.
    ///
    /// #### Returns
    ///
    /// true if focus scrolling is enabled
    public boolean isFocusScrolling() {
        return focusScrolling;
    }

    /// Indicates whether lists and containers should scroll only via focus and thus "jump" when
    /// moving to a larger component as was the case in older versions of Codename One.
    ///
    /// #### Parameters
    ///
    /// - `focusScrolling`: true to enable focus scrolling
    public void setFocusScrolling(boolean focusScrolling) {
        this.focusScrolling = focusScrolling;
    }

    /// Indicates whether the edge of a scrollable area should fade out
    ///
    /// #### Returns
    ///
    /// the fadeScrollEdge
    public boolean isFadeScrollEdge() {
        return fadeScrollEdge;
    }

    /// Indicates whether the edge of a scrollable area should fade out
    ///
    /// #### Parameters
    ///
    /// - `fadeScrollEdge`: the fadeScrollEdge to set
    public void setFadeScrollEdge(boolean fadeScrollEdge) {
        this.fadeScrollEdge = fadeScrollEdge;
    }

    /// Indicates whether the scrollbar should fade when unused
    ///
    /// #### Returns
    ///
    /// the fadeScrollBar
    public boolean isFadeScrollBar() {
        return fadeScrollBar;
    }

    /// Indicates whether the scrollbar should fade when unused
    ///
    /// #### Parameters
    ///
    /// - `fadeScrollBar`: the fadeScrollBar to set
    public void setFadeScrollBar(boolean fadeScrollBar) {
        this.fadeScrollBar = fadeScrollBar;
    }

    /// Indicates the width/height of the fading edge to indicate scrolling
    ///
    /// #### Returns
    ///
    /// the fadeScrollEdgeLength
    public int getFadeScrollEdgeLength() {
        return fadeScrollEdgeLength;
    }

    /// Indicates the width/height of the fading edge to indicate scrolling
    ///
    /// #### Parameters
    ///
    /// - `fadeScrollEdgeLength`: the fadeScrollEdgeLength to set
    public void setFadeScrollEdgeLength(int fadeScrollEdgeLength) {
        this.fadeScrollEdgeLength = fadeScrollEdgeLength;
    }

    /// The color of the text field cursor
    ///
    /// #### Returns
    ///
    /// the textFieldCursorColor
    public int getTextFieldCursorColor() {
        return textFieldCursorColor;
    }

    /// The color of the text field cursor
    ///
    /// #### Parameters
    ///
    /// - `textFieldCursorColor`: the textFieldCursorColor to set
    public void setTextFieldCursorColor(int textFieldCursorColor) {
        this.textFieldCursorColor = textFieldCursorColor;
    }

    /// Indicates whether scrolling this component should jump to a specific location
    /// in a grid
    ///
    /// #### Returns
    ///
    /// the defaultSnapToGrid
    public boolean isDefaultSnapToGrid() {
        return defaultSnapToGrid;
    }

    /// Indicates whether scrolling this component should jump to a specific location
    /// in a grid
    ///
    /// #### Parameters
    ///
    /// - `defaultSnapToGrid`: the defaultSnapToGrid to set
    public void setDefaultSnapToGrid(boolean defaultSnapToGrid) {
        this.defaultSnapToGrid = defaultSnapToGrid;
    }

    /// Enable the tensile drag to work even when a component doesn't have a scroll showable (scrollable flag still needs to be set to true)
    ///
    /// #### Returns
    ///
    /// the defaultAlwaysTensile
    public boolean isDefaultAlwaysTensile() {
        return defaultAlwaysTensile;
    }

    /// Enable the tensile drag to work even when a component doesn't have a scroll showable (scrollable flag still needs to be set to true)
    ///
    /// #### Parameters
    ///
    /// - `defaultAlwaysTensile`: the defaultAlwaysTensile to set
    public void setDefaultAlwaysTensile(boolean defaultAlwaysTensile) {
        this.defaultAlwaysTensile = defaultAlwaysTensile;
    }

    /// Indicates whether tensile highlight should be active by default
    ///
    /// #### Returns
    ///
    /// the defaultTensileHighlight
    public boolean isDefaultTensileHighlight() {
        return defaultTensileHighlight;
    }

    /// Paints the tensile hightlight image
    ///
    /// #### Parameters
    ///
    /// - `g`: graphics destination for the tensile highlight image
    ///
    /// - `top`: destination of the tensile highlight image
    ///
    /// - `opacity`: the opacity of the image
    public void paintTensileHighlight(Component t, Graphics g, boolean top, int opacity) {
        if (opacity > 0 && tensileHighlightTopImage != null && tensileHighlightBottomImage != null) {
            int absX = t.getAbsoluteX();
            int absY = t.getAbsoluteY();
            if (tensileGlowTopImage != null) {
                int a = g.getAlpha();
                float aspect = ((float) tensileGlowTopImage.getWidth()) / ((float) Display.getInstance().getDisplayWidth());
                int newHeight = (int) (((float) tensileGlowTopImage.getHeight()) * aspect);
                if (top) {
                    // this is a pull to refresh operation
                    if (t.isAlwaysTensile()) {
                        return;
                    }
                    g.drawImage(tensileHighlightTopImage, absX, absY, t.getWidth(), tensileHighlightTopImage.getHeight());
                    g.setAlpha(opacity / 3);
                    g.drawImage(tensileGlowTopImage, absX, absY, t.getWidth(), newHeight);
                    g.setAlpha(a);
                } else {
                    g.drawImage(tensileHighlightBottomImage, absX, absY + t.getScrollY() + (t.getHeight() - tensileHighlightBottomImage.getHeight()), t.getWidth(), tensileHighlightBottomImage.getHeight());
                    g.setAlpha(opacity / 3);
                    g.drawImage(tensileGlowBottomImage, absX, absY + t.getScrollY() + (t.getHeight() - newHeight), t.getWidth(), newHeight);
                    g.setAlpha(a);
                }
            } else {
                int a = g.getAlpha();
                g.setAlpha(opacity);
                if (top) {
                    // this is a pull to refresh operation
                    if (t.isAlwaysTensile()) {
                        g.setAlpha(a);
                        return;
                    }
                    g.drawImage(tensileHighlightTopImage, absX, absY, Display.getInstance().getDisplayWidth(), tensileHighlightTopImage.getHeight());
                } else {
                    g.drawImage(tensileHighlightBottomImage, absX, absY + t.getScrollY() + t.getHeight() - tensileHighlightBottomImage.getHeight(), Display.getInstance().getDisplayWidth(), tensileHighlightBottomImage.getHeight());
                }
                g.setAlpha(a);
            }
        }
    }


    UIManager getUIManager() {
        return manager;
    }

    /// #### Returns
    ///
    /// the fadeScrollBarSpeed
    public int getFadeScrollBarSpeed() {
        return fadeScrollBarSpeed;
    }

    /// #### Parameters
    ///
    /// - `fadeScrollBarSpeed`: the fadeScrollBarSpeed to set
    public void setFadeScrollBarSpeed(int fadeScrollBarSpeed) {
        this.fadeScrollBarSpeed = fadeScrollBarSpeed;
    }

    /// #### Returns
    ///
    /// scrollVisible
    public boolean isScrollVisible() {
        return scrollVisible;
    }

    /// Indicates if the bg image of a style should determine the minimum preferred size according to the theme
    ///
    /// #### Returns
    ///
    /// the backgroundImageDetermineSize
    public boolean isBackgroundImageDetermineSize() {
        return backgroundImageDetermineSize;
    }

    /// Indicates if the bg image of a style should determine the minimum preferred size according to the theme
    ///
    /// #### Parameters
    ///
    /// - `backgroundImageDetermineSize`: the backgroundImageDetermineSize to set
    public void setBackgroundImageDetermineSize(boolean backgroundImageDetermineSize) {
        this.backgroundImageDetermineSize = backgroundImageDetermineSize;
    }

    /// Paints the pull to refresh
    ///
    /// #### Parameters
    ///
    /// - `g`: graphics context
    ///
    /// - `cmp`: the Component which we draw the pull to refresh beneath it
    ///
    /// - `taskExecuted`: an indication if the refresh task is currently running
    public abstract void drawPullToRefresh(Graphics g, Component cmp, boolean taskExecuted);

    /// Returns the required height of the pull to refresh feature
    public abstract int getPullToRefreshHeight();

}
