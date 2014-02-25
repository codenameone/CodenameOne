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

import com.codename1.ui.geom.Dimension;
import com.codename1.ui.*;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.list.*;

/**
 * Allows a UI developer to completely customize the look of the application by
 * overriding drawing/sizing methods appropriately.
 *
 * @author Chen Fishbein
 */
public abstract class LookAndFeel {
    private Component verticalScroll;
    private Component horizontalScroll;
    private Component verticalScrollThumb;
    private Component horizontalScrollThumb;

    /**
     * Right-To-Left. Default false.
     */
    private boolean rtl;

    private long tickerSpeed = 50;
    /**
     * Tint color is set when a form is partially covered be it by a menu or by a 
     * dialog. A look and feel can override this default value.
     */
    private int defaultFormTintColor = 0x77000000;
    /**
     * This color is used to paint disable mode.
     */
    private int disableColor = 0xcccccc;
    /**
     * This member allows us to define a default animation that will draw the transition for
     * entering a form
     */
    private Transition defaultFormTransitionIn;
    /**
     * This member allows us to define a default animation that will draw the transition for
     * exiting a form
     */
    private Transition defaultFormTransitionOut;
    /**
     * This member allows us to define a default animation that will draw the transition for
     * entering a menu
     */
    private Transition defaultMenuTransitionIn;
    /**
     * This member allows us to define a default animation that will draw the transition for
     * exiting a menu
     */
    private Transition defaultMenuTransitionOut;
    /**
     * This member allows us to define a default animation that will draw the transition for
     * entering a dialog
     */
    private Transition defaultDialogTransitionIn;
    /**
     * This member allows us to define a default animation that will draw the transition for
     * exiting a form
     */
    private Transition defaultDialogTransitionOut;
    /**
     * Indicates whether lists and containers should have smooth scrolling by default
     */
    private boolean defaultSmoothScrolling = true;
    /**
     * Indicates whether lists and containers should scroll only via focus and thus "jump" when
     * moving to a larger component as was the case in older versions of Codename One.
     */
    private boolean focusScrolling;
    /**
     * Indicates the default speed for smooth scrolling
     */
    private int defaultSmoothScrollingSpeed = 150;

    private boolean fadeScrollEdge;
    private boolean fadeScrollBar;
    private int fadeScrollBarSpeed = 5;
    private int fadeScrollEdgeLength = 15;
    private Image fadeScrollTop;
    private Image fadeScrollBottom;
    private Image fadeScrollRight;
    private Image fadeScrollLeft;
    private int fadeScrollEdgeStartAlpha = 0x999999;
    private int fadeScrollEdgeEndAlpha = 0;
    private int textFieldCursorColor;

    private boolean backgroundImageDetermineSize;
    
    /**
     * Indicates whether softbuttons should be reversed from their default orientation
     */
    private boolean reverseSoftButtons;
    /**
     * This renderer is assigned to all Forms Menu's by default.
     */
    private ListCellRenderer menuRenderer;
    private Image[] menuIcons = new Image[3];

    /**
     * Allows defining a tactile touch device that vibrates when the user presses a component
     * that should respond with tactile feedback on a touch device (e.g. vibrate).
     * Setting this to 0 disables tactile feedback completely
     */
    private int tactileTouchDuration = 0;

    /**
     * Indicates whether labels should end with 3 points by default
     */
    private boolean defaultEndsWith3Points = true;

    /**
     * Indicates whether tensile drag should be active by default
     */
    private boolean defaultTensileDrag = true;

    /**
     * Indicates whether tensile highlight should be active by default
     */
    private boolean defaultTensileHighlight = false;

    /**
     * The MenuBar class
     */
    private Class menuBar = MenuBar.class;;

    private boolean defaultSnapToGrid;
    private boolean defaultAlwaysTensile;

    private Image tensileHighlightTopImage;
    private Image tensileHighlightBottomImage;
    private Image tensileGlowTopImage;
    private Image tensileGlowBottomImage;
    
    private UIManager manager;
    public LookAndFeel(UIManager manager){
        this.manager = manager;
    }

    /**
     * Every component binds itself to the look and feel thus allowing the look 
     * and feel to customize the component.  Binding occurs at the end of the
     * constructor when the component is in a valid state and ready to be used.
     * Notice that a component might be bound twice or more and it is the 
     * responsibility of the LookAndFeel to protect against that.
     * 
     * @param cmp component instance that may be customized by the look and feel
     */
    public void bind(Component cmp) {
    }

    /**
     * Invoked when a look and feel is removed, allows a look and feel to release 
     * resources related to binding components.
     * 
     * @see #bind(Component)
     */
    public void uninstall() {
    }

    /**
     * Invoked for drawing a button widget
     * 
     * @param g graphics context
     * @param b component to draw
     */
    public abstract void drawButton(Graphics g, Button b);

    /**
     * Invoked for drawing a checkbox widget
     * 
     * @param g graphics context
     * @param cb component to draw
     */
    public abstract void drawCheckBox(Graphics g, Button cb);

    /**
     * Invoked for drawing a combo box widget
     * 
     * @param g graphics context
     * @param cb component to draw
     */
    public abstract void drawComboBox(Graphics g, List cb);

    /**
     * Invoked for drawing a label widget
     * 
     * @param g graphics context
     * @param l component to draw
     */
    public abstract void drawLabel(Graphics g, Label l);

    /**
     * Invoked for drawing a list widget
     * 
     * @param g graphics context
     * @param l component to draw
     */
    public abstract void drawList(Graphics g, List l);

    /**
     * Invoked for drawing the radio button widget
     * 
     * @param g graphics context
     * @param rb component to draw
     */
    public abstract void drawRadioButton(Graphics g, Button rb);

    /**
     * Draw the given text area
     * 
     * @param g graphics context
     * @param ta component to draw
     */
    public abstract void drawTextArea(Graphics g, TextArea ta);

    /**
     * Draws the text field without its cursor which is drawn in a separate method
     * input mode indication can also be drawn using this method.
     * 
     * @param g graphics context
     * @param ta component to draw
     */
    public abstract void drawTextField(Graphics g, TextArea ta);

    /**
     * Draws the cursor of the text field, blinking is handled simply by avoiding
     * a call to this method.
     * 
     * @param g graphics context
     * @param ta component to draw
     */
    public abstract void drawTextFieldCursor(Graphics g, TextArea ta);

    /**
     * Calculate the preferred size of the component
     * 
     * @param b component whose size should be calculated
     * @return the preferred size for the button
     */
    public abstract Dimension getButtonPreferredSize(Button b);

    /**
     * Calculate the preferred size of the component
     * 
     * @param cb component whose size should be calculated
     * @return the preferred size for the component
     */
    public abstract Dimension getCheckBoxPreferredSize(Button cb);

    /**
     * Calculate the preferred size of the component
     * 
     * @param l component whose size should be calculated
     * @return the preferred size for the component
     */
    public abstract Dimension getLabelPreferredSize(Label l);

    /**
     * Calculate the preferred size of the component
     * 
     * @param l component whose size should be calculated
     * @return the preferred size for the component
     */
    public abstract Dimension getListPreferredSize(List l);

    /**
     * Calculate the preferred size of the component
     * 
     * @param rb component whose size should be calculated
     * @return the preferred size for the component
     */
    public abstract Dimension getRadioButtonPreferredSize(Button rb);

    //public abstract Dimension getSpinnerPreferredSize(Spinner sp);
    /**
     * Calculate the preferred size of the component
     * 
     * @param ta component whose size should be calculated
     * @param pref indicates whether preferred or scroll size should be returned
     * @return the preferred size for the component
     */
    public abstract Dimension getTextAreaSize(TextArea ta, boolean pref);

    /**
     * Calculate the preferred size of the component
     * 
     * @param ta component whose size should be calculated
     * @return the preferred size for the component
     */
    public abstract Dimension getTextFieldPreferredSize(TextArea ta);

    /**
     * Calculate the preferred size of the component
     * 
     * @param box component whose size should be calculated
     * @return the preferred size for the component
     */
    public abstract Dimension getComboBoxPreferredSize(List box);

    /**
     * Draws a vertical scroll bar in the given component
     * 
     * @param g graphics context
     * @param c component to draw on
     * @param offsetRatio ratio of the scroll bar from 0 to 1
     * @param blockSizeRatio block size for the scroll from 0 to 1
     */
    public void drawVerticalScroll(Graphics g, Component c, float offsetRatio, float blockSizeRatio) {
        if(verticalScroll == null) {
            initScroll();
        }
        int borderW = 0;
        if(c.getStyle().getBorder() != null){
            borderW = c.getStyle().getBorder().getThickness();
        }
        int x = c.getX();
        if(!c.isRTL()) {
            x += c.getWidth() - getVerticalScrollWidth() - borderW;
        }else{
            x += borderW;
        }
        int y = c.getY();
        int height = c.getHeight();
        int width = getVerticalScrollWidth();
        drawScroll(g, c, offsetRatio, blockSizeRatio, true, x, y, width, height, verticalScroll, verticalScrollThumb);
        if(fadeScrollEdge) {
            if(offsetRatio > 0) {
                if(fadeScrollTop == null || Display.getInstance().getDisplayWidth() != fadeScrollTop.getWidth()) {
                    fadeScrollTop = generateFadeImage(fadeScrollEdgeStartAlpha, fadeScrollEdgeEndAlpha, c.getStyle().getBgColor(), false,
                            Display.getInstance().getDisplayWidth(),fadeScrollEdgeLength);
                }
                g.drawImage(fadeScrollTop, c.getX(), c.getY());
            }
            if(offsetRatio < 1 - blockSizeRatio) {
                // special case since when scrolling with the physical keys we leave
                // the margin out of the equasion
                int l = c.getScrollY() + c.getHeight() +
                        c.getStyle().getMargin(Component.TOP) +
                        c.getStyle().getMargin(Component.BOTTOM) +
                        c.getStyle().getPadding(Component.TOP) +
                        c.getStyle().getPadding(Component.BOTTOM);

                int totalScroll = c.getScrollDimension().getHeight();
                if(l >= totalScroll) {
                    return;
                }
                if(fadeScrollBottom == null || Display.getInstance().getDisplayWidth() != fadeScrollBottom.getWidth()) {
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

    /**
     * Draws a horizontal scroll bar in the given component
     * 
     * @param g graphics context
     * @param c component to draw on
     * @param offsetRatio ratio of the scroll bar from 0 to 1
     * @param blockSizeRatio block size for the scroll from 0 to 1
     */
    public void drawHorizontalScroll(Graphics g, Component c, float offsetRatio, float blockSizeRatio) {
        if(horizontalScroll == null) {
            initScroll();
        }
        int borderH = 0;        
        if(c.getStyle().getBorder() != null){
            borderH = c.getStyle().getBorder().getThickness();
        }
        int x = c.getX();
        int y = c.getY() + c.getHeight() - getHorizontalScrollHeight() - borderH;
        
        int width = c.getWidth();
        int height = getHorizontalScrollHeight();
        drawScroll(g, c, offsetRatio, blockSizeRatio, false, x, y, width, height, horizontalScroll, horizontalScrollThumb);
        if(fadeScrollEdge) {
            if(offsetRatio > 0) {
                if(fadeScrollLeft == null || Display.getInstance().getDisplayHeight() != fadeScrollLeft.getHeight()) {
                    fadeScrollLeft = generateFadeImage(fadeScrollEdgeStartAlpha, fadeScrollEdgeEndAlpha, c.getStyle().getBgColor(), true, fadeScrollEdgeLength,
                            Display.getInstance().getDisplayHeight());
                }
                g.drawImage(fadeScrollLeft, c.getX(), c.getY());
            }
            if(offsetRatio < 1 - blockSizeRatio) {
                // special case since when scrolling with the physical keys we leave
                // the margin out of the equasion
                int l = c.getScrollX() + c.getWidth() +
                        c.getStyle().getMargin(Component.LEFT) +
                        c.getStyle().getMargin(Component.RIGHT) +
                        c.getStyle().getPadding(Component.LEFT) +
                        c.getStyle().getPadding(Component.RIGHT);
                int totalScroll = c.getScrollDimension().getWidth();
                if(l >= totalScroll) {
                    return;
                }
                if(fadeScrollRight == null || Display.getInstance().getDisplayHeight() != fadeScrollRight.getHeight()) {
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
        if(fadeScrollBar) {
            if(scrollStyle.getBgTransparency() != 0) {
                scrollStyle.setBgTransparency(c.getScrollOpacity(), true);
            }
            scrollThumbStyle.setBgTransparency(c.getScrollOpacity(), true);
            g.setAlpha(c.getScrollOpacity());
        }
        // take margin into consideration when positioning the scroll
        int marginLeft = scrollStyle.getMargin(c.isRTL(), Component.LEFT);
        int marginTop = scrollStyle.getMargin(false, Component.TOP);
        x += marginLeft;
        width -= (marginLeft + scrollStyle.getMargin(c.isRTL(), Component.RIGHT));
        y += marginTop;
        height -= (marginTop + scrollStyle.getMargin(false, Component.BOTTOM));

        scroll.setX(x);
        scroll.setY(y);
        scroll.setWidth(width);
        scroll.setHeight(height);

        int cx = g.getClipX();
        int cy = g.getClipY();
        int cw = g.getClipWidth();
        int ch = g.getClipHeight();

        scroll.paintComponent(g);

        marginLeft = scrollThumbStyle.getMargin(c.isRTL(), Component.LEFT);
        marginTop = scrollThumbStyle.getMargin(false, Component.TOP);
        x += marginLeft;
        width -= (marginLeft + scrollThumbStyle.getMargin(c.isRTL(), Component.RIGHT));
        y += marginTop;
        height -= (marginTop + scrollThumbStyle.getMargin(false, Component.BOTTOM));

        int offset, blockSize;

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
        if(fadeScrollBar) {
            scrollStyle.setBgTransparency(alpha, true);
            scrollThumbStyle.setBgTransparency(thumbAlpha, true);
            g.setAlpha(originalAlpha);
        }
    }


    /**
     * Sets the foreground color and font for a generic component, reuse-able by most component
     * drawing code
     * 
     * @param g graphics context
     * @param c component from which fg styles should be set
     */
    public void setFG(Graphics g, Component c) {
        Style s = c.getStyle();
        g.setFont(s.getFont());
        g.setColor(s.getFgColor());
    }

    /**
     * Returns the default width of a vertical scroll bar
     * 
     * @return default width of a vertical scroll bar
     */
    public int getVerticalScrollWidth() {
        if(verticalScroll == null) {
            initScroll();
        }
        Style scrollStyle = verticalScroll.getStyle();

        // bidi doesn't matter for width calculations
        return scrollStyle.getMargin(false, Component.LEFT) + scrollStyle.getMargin(false, Component.RIGHT) +
                scrollStyle.getPadding(false, Component.LEFT) + scrollStyle.getPadding(false, Component.RIGHT);
    }

    /**
     * Returns the default height of a horizontal scroll bar
     * 
     * @return default height of a horizontal scroll bar
     */
    public int getHorizontalScrollHeight() {
        if(horizontalScroll == null) {
            initScroll();
        }
        Style scrollStyle = horizontalScroll.getStyle();

        // bidi doesn't matter for height calculations
        return scrollStyle.getMargin(false, Component.TOP) + scrollStyle.getMargin(false, Component.BOTTOM) +
                scrollStyle.getPadding(false, Component.TOP) + scrollStyle.getPadding(false, Component.BOTTOM);
    }

    /**
     * Draws generic component border
     */
    void drawBorder(Graphics g, Component c, int color, int borderWidth) {
        drawBorder(g, c, color, color, borderWidth);
    }

    /**
     * Draws generic component border
     */
    void drawBorder(Graphics g, Component c, int topAndRightColor, int bottomAndLeftColor, int borderWidth) {
        g.setColor(topAndRightColor);     //Text Component upper border color

        g.fillRect(c.getX(), c.getY(), c.getWidth(), borderWidth);
        g.fillRect(c.getX(), c.getY(), borderWidth, c.getHeight());
        g.setColor(bottomAndLeftColor);     //Text Component lower border color

        g.fillRect(c.getX(), c.getY() + c.getHeight() - borderWidth, c.getWidth(), borderWidth);
        g.fillRect(c.getX() + c.getWidth() - borderWidth, c.getY(), borderWidth, c.getHeight());
    }

    /**
     * Allows us to define a default animation that will draw the transition for
     * entering a form
     * 
     * @return default transition
     */
    public Transition getDefaultFormTransitionIn() {
        return defaultFormTransitionIn;
    }

    /**
     * Allows us to define a default animation that will draw the transition for
     * entering a form
     * 
     * @param defaultFormTransitionIn the default transition
     */
    public void setDefaultFormTransitionIn(Transition defaultFormTransitionIn) {
        this.defaultFormTransitionIn = defaultFormTransitionIn;
    }

    /**
     * Allows us to define a default animation that will draw the transition for
     * exiting a form
     * 
     * @return default transition
     */
    public Transition getDefaultFormTransitionOut() {
        return defaultFormTransitionOut;
    }

    /**
     * Allows us to define a default animation that will draw the transition for
     * exiting a form
     * 
     * @param defaultFormTransitionOut the default transition
     */
    public void setDefaultFormTransitionOut(Transition defaultFormTransitionOut) {
        this.defaultFormTransitionOut = defaultFormTransitionOut;
    }

    /**
     * Allows us to define a default animation that will draw the transition for
     * entering a Menu
     * 
     * @return default transition
     */
    public Transition getDefaultMenuTransitionIn() {
        return defaultMenuTransitionIn;
    }

    /**
     * Allows us to define a default animation that will draw the transition for
     * entering a Menu
     * 
     * @param defaultMenuTransitionIn the default transition
     */
    public void setDefaultMenuTransitionIn(Transition defaultMenuTransitionIn) {
        this.defaultMenuTransitionIn = defaultMenuTransitionIn;
    }

    /**
     * Allows us to define a default animation that will draw the transition for
     * exiting a Menu
     * 
     * @return default transition
     */
    public Transition getDefaultMenuTransitionOut() {
        return defaultMenuTransitionOut;
    }

    /**
     * Allows us to define a default animation that will draw the transition for
     * exiting a Menu
     * 
     * @param defaultMenuTransitionOut the default transition
     */
    public void setDefaultMenuTransitionOut(Transition defaultMenuTransitionOut) {
        this.defaultMenuTransitionOut = defaultMenuTransitionOut;
    }

    /**
     * Allows us to define a default animation that will draw the transition for
     * entering a dialog
     * 
     * @return default transition
     */
    public Transition getDefaultDialogTransitionIn() {
        return defaultDialogTransitionIn;
    }

    /**
     * Allows us to define a default animation that will draw the transition for
     * entering a dialog
     * 
     * @param defaultDialogTransitionIn the default transition
     */
    public void setDefaultDialogTransitionIn(Transition defaultDialogTransitionIn) {
        this.defaultDialogTransitionIn = defaultDialogTransitionIn;
    }

    /**
     * Allows us to define a default animation that will draw the transition for
     * exiting a dialog
     * 
     * @return default transition
     */
    public Transition getDefaultDialogTransitionOut() {
        return defaultDialogTransitionOut;
    }

    /**
     * Allows us to define a default animation that will draw the transition for
     * exiting a dialog
     * 
     * @param defaultDialogTransitionOut the default transition
     */
    public void setDefaultDialogTransitionOut(Transition defaultDialogTransitionOut) {
        this.defaultDialogTransitionOut = defaultDialogTransitionOut;
    }

    /**
     * Tint color is set when a form is partially covered be it by a menu or by a 
     * dialog. A look and feel can override this default value.
     * 
     * @return default tint color
     */
    public int getDefaultFormTintColor() {
        return defaultFormTintColor;
    }

    /**
     * Tint color is set when a form is partially covered be it by a menu or by a 
     * dialog. A look and feel can override this default value.
     * 
     * @param defaultFormTintColor the default tint color
     */
    public void setDefaultFormTintColor(int defaultFormTintColor) {
        this.defaultFormTintColor = defaultFormTintColor;
    }

    /**
     * This color is used to paint disable mode text color.
    
     * @return the color value
     */
    public int getDisableColor() {
        return disableColor;
    }

    /**
     * Simple setter to disable color
     * 
     * @param disableColor the disable color value
     */
    public void setDisableColor(int disableColor) {
        this.disableColor = disableColor;
    }

    /**
     * Indicates whether lists and containers should have smooth scrolling by default
     * 
     * @return true if smooth scrolling should be on by default
     */
    public boolean isDefaultSmoothScrolling() {
        return defaultSmoothScrolling;
    }

    /**
     * Indicates whether lists and containers should have smooth scrolling by default
     * 
     * @param defaultSmoothScrolling  true if smooth scrolling should be on by default
     */
    public void setDefaultSmoothScrolling(boolean defaultSmoothScrolling) {
        this.defaultSmoothScrolling = defaultSmoothScrolling;
    }

    /**
     * Indicates the default speed for smooth scrolling
     * 
     * @return speed for smooth scrollin
     */
    public int getDefaultSmoothScrollingSpeed() {
        return defaultSmoothScrollingSpeed;
    }

    /**
     * Indicates the default speed for smooth scrolling
     * 
     * @param defaultSmoothScrollingSpeed speed for smooth scrollin
     */
    public void setDefaultSmoothScrollingSpeed(int defaultSmoothScrollingSpeed) {
        this.defaultSmoothScrollingSpeed = defaultSmoothScrollingSpeed;
    }

    /**
     * Indicates whether softbuttons should be reversed from their default orientation
     * 
     * @return true if softbuttons should be reversed
     */
    public boolean isReverseSoftButtons() {
        return reverseSoftButtons;
    }

    /**
     * Indicates whether softbuttons should be reversed from their default orientation
     * 
     * @param reverseSoftButtons  true if softbuttons should be reversed
     */
    public void setReverseSoftButtons(boolean reverseSoftButtons) {
        this.reverseSoftButtons = reverseSoftButtons;
    }

    /**
     * This method returns the MenuBar class.
     * 
     * @return the MenuBar class.
     */
    public Class getMenuBarClass(){
        return menuBar;
    }

    /**
     * Simple setter for the MenuBar Class
     * 
     * @param menuBar
     */
    public void setMenuBarClass(Class menuBar) {
        this.menuBar = menuBar;
    }

    
    /**
     * Returns the Menu default renderer
     * 
     * @return default renderer for the menu
     */
    public ListCellRenderer getMenuRenderer() {
        return menuRenderer;
    }

    /**
     * Sets the Menu default renderer
     * 
     * @param menuRenderer default renderer for the menu
     */
    public void setMenuRenderer(ListCellRenderer menuRenderer) {
        this.menuRenderer = menuRenderer;
    }

    /**
     * Sets globally the Menu icons
     * 
     * @param select select icon
     * @param cancel cancel icon
     * @param menu menu icon
     */
    public void setMenuIcons(Image select, Image cancel, Image menu) {
        menuIcons[0] = select;
        menuIcons[1] = cancel;
        menuIcons[2] = menu;

    }

    /**
     * Simple getter for the menu icons
     * 
     * @return an Image array at size of 3, where the first is the select image
     * the second is the cancel image and the last is the menu image.
     */
    public Image[] getMenuIcons() {
        return menuIcons;
    }

    /**
     * Gets the ticker speed
     *
     * @return ticker speed in milliseconds
     */
    public long getTickerSpeed() {
        return tickerSpeed;
    }

    /**
     * Sets the ticker speed
     *
     * @param tickerSpeed the speed in milliseconds
     */
    public void setTickerSpeed(long tickerSpeed) {
        this.tickerSpeed = tickerSpeed;
    }

    private void initScroll() {
        verticalScroll = new Label();
        verticalScroll.setUIID("Scroll");
        horizontalScroll = new Label();
        horizontalScroll.setUIID("HorizontalScroll");
        verticalScrollThumb = new Label();;
        verticalScrollThumb.setUIID("ScrollThumb");
        horizontalScrollThumb = new Label();
        horizontalScrollThumb.setUIID("HorizontalScrollThumb");
    }

    /**
     * This method is a callback to the LookAndFeel when a theme is being 
     * changed in the UIManager
     * @param completeClear indicates that the theme is set and not added
     */
    public void refreshTheme(boolean completeClear) {
        fadeScrollTop = null;
        fadeScrollBottom = null;
        fadeScrollRight = null;
        fadeScrollLeft = null;
        initScroll();
        if(menuRenderer != null) {
            if(menuRenderer instanceof Component) {
                ((Component)menuRenderer).refreshTheme();
            }
        }

        boolean isTouch = Display.getInstance().isTouchScreenDevice();
        fadeScrollEdge = manager.isThemeConstant("fadeScrollEdgeBool", false);
        fadeScrollEdgeLength = manager.getThemeConstant("fadeScrollEdgeInt", fadeScrollEdgeLength);
        fadeScrollBar = manager.isThemeConstant("fadeScrollBarBool", false);

        try {
            tickerSpeed = Long.parseLong(manager.getThemeConstant("tickerSpeedInt", "" + tickerSpeed));
            if(tickerSpeed < 1) {
                Label.setDefaultTickerEnabled(false);
            } else {
                Label.setDefaultTickerEnabled(true);
            }
        } catch(NumberFormatException err) {
            err.printStackTrace();
        }
        defaultFormTintColor = (int)Long.parseLong(manager.getThemeConstant("tintColor", Integer.toHexString(defaultFormTintColor)), 16);
        disableColor = Integer.parseInt(manager.getThemeConstant("disabledColor", Integer.toHexString(disableColor)), 16);
        Dialog.setDefaultDialogPosition(manager.getThemeConstant("dialogPosition", Dialog.getDefaultDialogPosition()));
        Dialog.setCommandsAsButtons(manager.isThemeConstant("dialogButtonCommandsBool", Dialog.isCommandsAsButtons()));

        List.setDefaultIgnoreFocusComponentWhenUnfocused(manager.isThemeConstant("ignorListFocusBool", List.isDefaultIgnoreFocusComponentWhenUnfocused()));

        if(isTouch) {
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
        TextArea.setDefaultValign(manager.getThemeConstant("textCmpVAlignInt", TextArea.getDefaultValign()));
        defaultSnapToGrid = manager.isThemeConstant("snapGridBool", false);
        defaultAlwaysTensile = manager.isThemeConstant("alwaysTensileBool", false);
        defaultTensileDrag = manager.isThemeConstant("tensileDragBool", true);
        defaultEndsWith3Points = manager.isThemeConstant("endsWith3PointsBool", true);
        defaultTensileHighlight = manager.isThemeConstant("tensileHighlightBool", false);
        tensileHighlightBottomImage = null;
        tensileHighlightTopImage = null;
        if(defaultTensileHighlight) {
            tensileHighlightBottomImage = manager.getThemeImageConstant("tensileHighlightBottomImage");
            tensileHighlightTopImage = manager.getThemeImageConstant("tensileHighlightTopImage");
            tensileGlowBottomImage = manager.getThemeImageConstant("tensileGlowBottomImage");
            tensileGlowTopImage = manager.getThemeImageConstant("tensileGlowTopImage");
            if(tensileHighlightBottomImage != null && tensileHighlightTopImage != null) {
                defaultTensileDrag = true;
                defaultAlwaysTensile = false;
            } else {
                defaultTensileHighlight = false;
            }
        }
        backgroundImageDetermineSize = manager.isThemeConstant("bgImageSizeBool", false);
    }

    private void initCommandBehaviorConstant(String c, boolean complete) {
        if(c != null) {
            if(c.equalsIgnoreCase("SoftKey")) {
                Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_SOFTKEY);
                return;
            }
            if(c.equalsIgnoreCase("Touch")) {
                Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_TOUCH_MENU);
                return;
            }
            if(c.equalsIgnoreCase("Bar")) {
                Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_BUTTON_BAR);
                return;
            }
            if(c.equalsIgnoreCase("Title")) {
                Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_BACK);
                return;
            }
            if(c.equalsIgnoreCase("Right")) {
                Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_RIGHT);
                return;
            }
            if(c.equalsIgnoreCase("Native")) {
                Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_NATIVE);
                return;
            }
            if(c.equalsIgnoreCase("ICS")) {
                Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_ICS);
                return;
            }
            if(c.equalsIgnoreCase("SIDE")) {
                Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_SIDE_NAVIGATION);
                setMenuBarClass(SideMenuBar.class);
                return;
            }
        } else {
            if(complete) {
                Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_DEFAULT);
            }
        }
    }

    private Transition getTransitionConstant(Transition t, String constant, String slideDir, int speed, boolean forward) {
        Image img = manager.getThemeImageConstant(constant + "Image");
        if(img != null) {
            return CommonTransitions.createTimeline(img);
        }
        String val = manager.getThemeConstant(constant, null);
        if(val == null) {
            return t;
        }
        if(val.equalsIgnoreCase("empty")) {
            return CommonTransitions.createEmpty();
        }
        if(val.equalsIgnoreCase("slide")) {
            if(slideDir.equalsIgnoreCase("horizontal")) {
                return CommonTransitions.createSlide(CommonTransitions.SLIDE_HORIZONTAL, forward, speed);
            } else {
                return CommonTransitions.createSlide(CommonTransitions.SLIDE_VERTICAL, forward, speed);
            }
        }
        if(val.equalsIgnoreCase("cover")) {
            if(slideDir.equalsIgnoreCase("horizontal")) {
                return CommonTransitions.createCover(CommonTransitions.SLIDE_HORIZONTAL, forward, speed);
            } else {
                return CommonTransitions.createCover(CommonTransitions.SLIDE_VERTICAL, forward, speed);
            }
        }
        if(val.equalsIgnoreCase("uncover")) {
            if(slideDir.equalsIgnoreCase("horizontal")) {
                return CommonTransitions.createUncover(CommonTransitions.SLIDE_HORIZONTAL, forward, speed);
            } else {
                return CommonTransitions.createUncover(CommonTransitions.SLIDE_VERTICAL, forward, speed);
            }
        }
        if(val.equalsIgnoreCase("fslide")) {
            if(slideDir.equalsIgnoreCase("horizontal")) {
                return CommonTransitions.createFastSlide(CommonTransitions.SLIDE_HORIZONTAL, forward, speed);
            } else {
                return CommonTransitions.createFastSlide(CommonTransitions.SLIDE_VERTICAL, forward, speed);
            }
        }
        if(val.equalsIgnoreCase("fade")) {
            return CommonTransitions.createFade(speed);
        }
        if(val.equalsIgnoreCase("slidefade")) {
            return CommonTransitions.createSlideFadeTitle(forward, speed);
        }
        if(val.equalsIgnoreCase("pulse")) {
            return CommonTransitions.createDialogPulsate();
        }
        return t;
    }

    /**
     * Indicates whether the menu UI should target a touch based device or a
     * standard cell phone
     *
     * @return true for touch menus
     * @deprecated use Display.getCommandBehavior() == Display.COMMAND_BEHAVIOR_TOUCH_MENU
     */
    public boolean isTouchMenus() {
        int t = Display.getInstance().getCommandBehavior();
        return t == Display.COMMAND_BEHAVIOR_TOUCH_MENU ||
                (t == Display.COMMAND_BEHAVIOR_DEFAULT && Display.getInstance().isTouchScreenDevice());
    }

    /**
     * Indicates whether the menu UI should target a touch based device or a
     * standard cell phone
     *
     * @param touchMenus true to enable touch menus false to disable
     * @deprecated use Display.setCommandBehavior(Display.COMMAND_BEHAVIOR_TOUCH_MENU)
     */
    public void setTouchMenus(boolean touchMenus) {
        if(touchMenus) {
            Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_TOUCH_MENU);
        } else {
            Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_SOFTKEY);
        }
    }

	/**
	 * Sets this LookAndFeel to operate in right-to-left mode.
	 *
	 * @param rtl - true if right-to-left, false if left-to-right
	 */
	public void setRTL(boolean rtl) {
        if(UIManager.localeAccessible) {
            this.rtl = rtl;
            if(rtl) {
                Display.getInstance().setBidiAlgorithm(true);
            }
        }
	}

	/**
	 * Use this to check if the LookAndFeel is in RTL mode
	 *
	 * @return true if the LookAndFeel is in right-to-left mode, false otherwise
	 */
	public boolean isRTL() {
		return rtl;
	}

    /**
     * Allows defining a tactile touch device that vibrates when the user presses a component
     * that should respond with tactile feedback on a touch device (e.g. vibrate).
     * Setting this to 0 disables tactile feedback completely

     * @return the tactileTouchDuration
     */
    public int getTactileTouchDuration() {
        return tactileTouchDuration;
    }

    /**
     * Allows defining a tactile touch device that vibrates when the user presses a component
     * that should respond with tactile feedback on a touch device (e.g. vibrate).
     * Setting this to 0 disables tactile feedback completely
     *
     * @param tactileTouchDuration the duration of vibration
     */
    public void setTactileTouchDuration(int tactileTouchDuration) {
        this.tactileTouchDuration = tactileTouchDuration;
    }

    /**
     * Indicates whether labels should end with 3 points by default
     *
     * @return whether labels should end with 3 points by default
     */
    public boolean isDefaultEndsWith3Points() {
        return defaultEndsWith3Points;
    }

    /**
     * Indicates whether labels should end with 3 points by default
     *
     * @param defaultEndsWith3Points True to indicates that labels should end with 3 points by default
     */
    public void setDefaultEndsWith3Points(boolean defaultEndsWith3Points) {
        this.defaultEndsWith3Points = defaultEndsWith3Points;
    }

    /**
     * Indicates whether tensile drag should be active by default
     *
     * @return whether tensile drag should be active by default
     */
    public boolean isDefaultTensileDrag() {
        return defaultTensileDrag;
    }

    /**
     * Indicates whether tensile drag should be active by default
     *
     * @param defaultTensileDrag true if tensile drag should be active by default
     */
    public void setDefaultTensileDrag(boolean defaultTensileDrag) {
        this.defaultTensileDrag = defaultTensileDrag;
    }

    /**
     * Indicates whether lists and containers should scroll only via focus and thus "jump" when
     * moving to a larger component as was the case in older versions of Codename One.
     *
     * @return true if focus scrolling is enabled
     */
    public boolean isFocusScrolling() {
        return focusScrolling;
    }

    /**
     * Indicates whether lists and containers should scroll only via focus and thus "jump" when
     * moving to a larger component as was the case in older versions of Codename One.
     *
     * @param focusScrolling true to enable focus scrolling
     */
    public void setFocusScrolling(boolean focusScrolling) {
        this.focusScrolling = focusScrolling;
    }

    /**
     * Indicates whether the edge of a scrollable area should fade out
     *
     * @return the fadeScrollEdge
     */
    public boolean isFadeScrollEdge() {
        return fadeScrollEdge;
    }

    /**
     * Indicates whether the edge of a scrollable area should fade out
     * 
     * @param fadeScrollEdge the fadeScrollEdge to set
     */
    public void setFadeScrollEdge(boolean fadeScrollEdge) {
        this.fadeScrollEdge = fadeScrollEdge;
    }

    /**
     * Indicates whether the scrollbar should fade when unused
     *
     * @return the fadeScrollBar
     */
    public boolean isFadeScrollBar() {
        return fadeScrollBar;
    }

    /**
     * Indicates whether the scrollbar should fade when unused
     * 
     * @param fadeScrollBar the fadeScrollBar to set
     */
    public void setFadeScrollBar(boolean fadeScrollBar) {
        this.fadeScrollBar = fadeScrollBar;
    }

    /**
     * Indicates the width/height of the fading edge to indicate scrolling
     *
     * @return the fadeScrollEdgeLength
     */
    public int getFadeScrollEdgeLength() {
        return fadeScrollEdgeLength;
    }

    /**
     * Indicates the width/height of the fading edge to indicate scrolling
     * 
     * @param fadeScrollEdgeLength the fadeScrollEdgeLength to set
     */
    public void setFadeScrollEdgeLength(int fadeScrollEdgeLength) {
        this.fadeScrollEdgeLength = fadeScrollEdgeLength;
    }

    /**
     * The color of the text field cursor
     * @return the textFieldCursorColor
     */
    public int getTextFieldCursorColor() {
        return textFieldCursorColor;
    }

    /**
     * The color of the text field cursor
     * @param textFieldCursorColor the textFieldCursorColor to set
     */
    public void setTextFieldCursorColor(int textFieldCursorColor) {
        this.textFieldCursorColor = textFieldCursorColor;
    }

    /**
     * Indicates whether scrolling this component should jump to a specific location
     * in a grid
     * @return the defaultSnapToGrid
     */
    public boolean isDefaultSnapToGrid() {
        return defaultSnapToGrid;
    }

    /**
     * Indicates whether scrolling this component should jump to a specific location
     * in a grid
     * @param defaultSnapToGrid the defaultSnapToGrid to set
     */
    public void setDefaultSnapToGrid(boolean defaultSnapToGrid) {
        this.defaultSnapToGrid = defaultSnapToGrid;
    }

    /**
     * Enable the tensile drag to work even when a component doesn't have a scroll showable (scrollable flag still needs to be set to true)
     * @return the defaultAlwaysTensile
     */
    public boolean isDefaultAlwaysTensile() {
        return defaultAlwaysTensile;
    }

    /**
     * Enable the tensile drag to work even when a component doesn't have a scroll showable (scrollable flag still needs to be set to true)
     * @param defaultAlwaysTensile the defaultAlwaysTensile to set
     */
    public void setDefaultAlwaysTensile(boolean defaultAlwaysTensile) {
        this.defaultAlwaysTensile = defaultAlwaysTensile;
    }

    /**
     * Indicates whether tensile highlight should be active by default
     * @return the defaultTensileHighlight
     */
    public boolean isDefaultTensileHighlight() {
        return defaultTensileHighlight;
    }

    /**
     * Paints the tensile hightlight image
     *
     * @param g graphics destination for the tensile highlight image
     * @param top destination of the tensile highlight image
     * @param opacity the opacity of the image
     */
    public void paintTensileHighlight(Component t, Graphics  g, boolean top, int opacity) {
        if(opacity > 0 && tensileHighlightTopImage != null && tensileHighlightBottomImage != null) {
            int absX = t.getAbsoluteX();
            int absY = t.getAbsoluteY();
            if(tensileGlowTopImage != null) {
                int a = g.getAlpha();
                float aspect = ((float)tensileGlowTopImage.getWidth()) / ((float)Display.getInstance().getDisplayWidth());
                int newHeight = (int)(((float)tensileGlowTopImage.getHeight()) * aspect);
                if(top) {
                    g.drawImage(tensileHighlightTopImage, absX, absY, Display.getInstance().getDisplayWidth(), tensileHighlightTopImage.getHeight());
                    g.setAlpha(opacity / 3);
                    g.drawImage(tensileGlowTopImage, absX, absY, Display.getInstance().getDisplayWidth(), newHeight);
                    g.setAlpha(a);
                } else {
                    g.drawImage(tensileHighlightBottomImage, absX, absY + t.getScrollY() + (t.getHeight() - newHeight), Display.getInstance().getDisplayWidth(), tensileHighlightBottomImage.getHeight());
                    g.setAlpha(opacity / 3);
                    g.drawImage(tensileGlowBottomImage, absX, absY + t.getScrollY() +(t.getHeight() - newHeight), Display.getInstance().getDisplayWidth(), newHeight);
                    g.setAlpha(a);
                }
            } else {
                int a = g.getAlpha();
                g.setAlpha(opacity);
                if(top) {
                    g.drawImage(tensileHighlightTopImage, absX, absY, Display.getInstance().getDisplayWidth(), tensileHighlightTopImage.getHeight());
                } else {
                    g.drawImage(tensileHighlightBottomImage, absX, absY + t.getScrollY() + t.getHeight() - tensileHighlightBottomImage.getHeight(), Display.getInstance().getDisplayWidth(), tensileHighlightBottomImage.getHeight());
                }
                g.setAlpha(a);
            }
        }
    }
    
    
    UIManager getUIManager(){
        return manager;
    }

    /**
     * @return the fadeScrollBarSpeed
     */
    public int getFadeScrollBarSpeed() {
        return fadeScrollBarSpeed;
    }

    /**
     * @param fadeScrollBarSpeed the fadeScrollBarSpeed to set
     */
    public void setFadeScrollBarSpeed(int fadeScrollBarSpeed) {
        this.fadeScrollBarSpeed = fadeScrollBarSpeed;
    }

    /**
     * Indicates if the bg image of a style should determine the minimum preferred size according to the theme
     * @return the backgroundImageDetermineSize
     */
    public boolean isBackgroundImageDetermineSize() {
        return backgroundImageDetermineSize;
    }

    /**
     * Indicates if the bg image of a style should determine the minimum preferred size according to the theme
     * @param backgroundImageDetermineSize the backgroundImageDetermineSize to set
     */
    public void setBackgroundImageDetermineSize(boolean backgroundImageDetermineSize) {
        this.backgroundImageDetermineSize = backgroundImageDetermineSize;
    }

    /**
     * Paints the pull to refresh 
     * @param g graphics context
     * @param cmp the Component which we draw the pull to refresh beneath it
     * @param taskExecuted an indication if the refresh task is currently running
     */ 
    public abstract void drawPullToRefresh(Graphics g, Component cmp, boolean taskExecuted);
    
    /**
     * Returns the required height of the pull to refresh feature
     */ 
    public abstract int getPullToRefreshHeight();
    
}
