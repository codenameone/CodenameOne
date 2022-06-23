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
import com.codename1.io.Util;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.ComboBox;
import com.codename1.ui.Component;
import com.codename1.ui.ComponentSelector;
import com.codename1.ui.ComponentSelector.ComponentClosure;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Label;
import com.codename1.ui.List;
import com.codename1.ui.TextArea;
import com.codename1.ui.list.ListCellRenderer;
import com.codename1.ui.list.ListModel;
import com.codename1.ui.Font;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Stroke;
import com.codename1.ui.TextSelection;
import com.codename1.ui.TextSelection.Char;
import com.codename1.ui.TextSelection.Span;
import com.codename1.ui.TextSelection.Spans;
import com.codename1.ui.animations.Animation;
import com.codename1.ui.events.FocusListener;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.util.Resources;

/**
 * Used to render the default look of Codename One
 *
 * @author Chen Fishbein
 * @deprecated this class is still crucial for some features in Codename One. The deprecation is here to indicate 
 * our desire to reduce usage/reliance on this class. 
 */
public class DefaultLookAndFeel extends LookAndFeel implements FocusListener {
    private Image[] chkBoxImages = null;
    private Image comboImage = null;
    private Image[] rButtonImages = null;
    private Image[] chkBoxImagesFocus = null;
    private Image[] rButtonImagesFocus = null;
    private boolean tickWhenFocused = true;
    private char passwordChar = '\u25CF'; 
    
    //used for the pull to refresh feature
    private Container pull;
    private Component updating;
    private Component pullDown;
    private Component releaseToRefresh;
    
    
    /** Creates a new instance of DefaultLookAndFeel */
    public DefaultLookAndFeel(UIManager manager) {
        super(manager);
    }

    /**
     * {@inheritDoc}
     */
    public void bind(Component cmp) {
        if (tickWhenFocused && cmp instanceof Label) {
            ((Label) cmp).addFocusListener(this);
        }
    }

    /**
     * This method allows to set all Labels, Buttons, CheckBoxes, RadioButtons
     * to start ticking when the text is too long. 
     * @param tickWhenFocused
     */
    public void setTickWhenFocused(boolean tickWhenFocused) {
        this.tickWhenFocused = tickWhenFocused;
    }

    /**
     * This method allows to set all Labels, Buttons, CheckBoxes, RadioButtons
     * to start ticking when the text is too long.
     * @return  tickWhenFocused
     */
    public boolean isTickWhenFocused() {
        return tickWhenFocused;
    }

    /**
     * Sets images for checkbox checked/unchecked modes
     * 
     * @param checkedX the image to draw in order to represent a checked checkbox
     * @param uncheckedX the image to draw in order to represent an uncheck checkbox
     */
    public void setCheckBoxImages(Image checkedX, Image uncheckedX) {
        setCheckBoxImages(checkedX, uncheckedX, checkedX, uncheckedX);
    }

    /**
     * Sets images for checkbox checked/unchecked modes
     *
     * @param checkedX the image to draw in order to represent a checked checkbox
     * @param uncheckedX the image to draw in order to represent an uncheck checkbox
     * @param disabledChecked same as checked for the disabled state
     * @param disabledUnchecked same as unchecked for the disabled state
     */
    public void setCheckBoxImages(Image checkedX, Image uncheckedX, Image disabledChecked, Image disabledUnchecked) {
        if (checkedX == null || uncheckedX == null) {
            chkBoxImages = null;
        } else {
            if(disabledUnchecked == null) {
                disabledUnchecked = uncheckedX;
            }
            if(disabledChecked == null) {
                disabledChecked = checkedX;
            }
            chkBoxImages = new Image[]{uncheckedX, checkedX, disabledUnchecked, disabledChecked};
        }
    }

    /**
     * Sets images for checkbox when in focused mode
     *
     * @param checkedX the image to draw in order to represent a checked checkbox
     * @param uncheckedX the image to draw in order to represent an uncheck checkbox
     * @param disabledChecked same as checked for the disabled state
     * @param disabledUnchecked same as unchecked for the disabled state
     */
    public void setCheckBoxFocusImages(Image checkedX, Image uncheckedX, Image disabledChecked, Image disabledUnchecked) {
        if (checkedX == null || uncheckedX == null) {
            chkBoxImagesFocus = null;
        } else {
            if(disabledUnchecked == null) {
                disabledUnchecked = uncheckedX;
            }
            if(disabledChecked == null) {
                disabledChecked = checkedX;
            }
            chkBoxImagesFocus = new Image[]{uncheckedX, checkedX, disabledUnchecked, disabledChecked};
        }
    }

    /**
     * Sets image for the combo box dropdown drawing
     * 
     * @param picker picker image
     */
    public void setComboBoxImage(Image picker) {
        comboImage = picker;
    }

    /**
     * Sets images for radio button selected/unselected modes
     * 
     * @param selected the image to draw in order to represent a selected radio button
     * @param unselected the image to draw in order to represent an unselected radio button
     */
    public void setRadioButtonImages(Image selected, Image unselected) {
        if (selected == null || unselected == null) {
            rButtonImages = null;
        } else {
            rButtonImages = new Image[]{unselected, selected};
        }
    }

    /**
     * Sets images for radio button selected/unselected modes
     *
     * @param selected the image to draw in order to represent a selected radio button
     * @param unselected the image to draw in order to represent an unselected radio button
     * @param disabledSelected same as selected for the disabled state
     * @param disabledUnselected same as unselected for the disabled state
     */
    public void setRadioButtonImages(Image selected, Image unselected, Image disabledSelected, Image disabledUnselected) {
        if (selected == null || unselected == null) {
            rButtonImages = null;
        } else {
            if(disabledUnselected == null) {
                disabledUnselected = unselected;
            }
            if(disabledSelected == null) {
                disabledSelected = selected;
            }
            rButtonImages = new Image[]{unselected, selected, disabledUnselected, disabledSelected};
        }
    }

    /**
     * Sets images for radio button selected/unselected and disabled modes, when the radio button has focus, these are entirely optional
     *
     * @param selected the image to draw in order to represent a selected radio button
     * @param unselected the image to draw in order to represent an unselected radio button
     * @param disabledSelected same as selected for the disabled state
     * @param disabledUnselected same as unselected for the disabled state
     */
    public void setRadioButtonFocusImages(Image selected, Image unselected, Image disabledSelected, Image disabledUnselected) {
        if (selected == null || unselected == null) {
            rButtonImagesFocus = null;
        } else {
            if(disabledUnselected == null) {
                disabledUnselected = unselected;
            }
            if(disabledSelected == null) {
                disabledSelected = selected;
            }
            rButtonImagesFocus = new Image[]{unselected, selected, disabledUnselected, disabledSelected};
        }
    }
    
    /**
     * Sets the password character to display in the TextArea and the TextField
     * @param the char to display
     */ 
    public void setPasswordChar(char c){
        passwordChar = c;
    }

    /**
     * Returns the images used to represent the radio button (selected followed by unselected).
     * 
     * @return images representing the radio button or null for using the default drawing
     */
    public Image[] getRadioButtonImages() {
        return rButtonImages;
    }

    /**
     * Returns the images used to represent the radio button when in focused mode
     *
     * @return images representing the radio button or null for using the default drawing
     */
    public Image[] getRadioButtonFocusImages() {
        return rButtonImagesFocus;
    }

    /**
     * Returns the images used to represent the checkbox (selected followed by unselected).
     *
     * @return images representing the check box or null for using the default drawing
     */
    public Image[] getCheckBoxImages() {
        return chkBoxImages;
    }

    /**
     * Returns the images used to represent the checkbox when focused
     *
     * @return images representing the check box or null for using the default drawing
     */
    public Image[] getCheckBoxFocusImages() {
        return chkBoxImagesFocus;
    }

    /**
     * {@inheritDoc}
     * @deprecated this method is no longer used by the implementation, we shifted code away to improve performance
     */
    public void drawButton(Graphics g, Button b) {
        drawComponent(g, b, b.getIconFromState(), null, 0);
    }

    /**
     * {@inheritDoc}
     */
    public void drawCheckBox(Graphics g, Button cb) {
        if (chkBoxImages != null) {
            Image x;
            if(chkBoxImagesFocus != null && chkBoxImagesFocus[0] != null && cb.hasFocus() && Display.getInstance().shouldRenderSelection(cb)) {
                if(cb.isEnabled()) {
                    x = chkBoxImagesFocus[cb.isSelected() ? 1 : 0];
                } else {
                    x = chkBoxImagesFocus[cb.isSelected() ? 3 : 2];
                }
            } else {
                if(cb.isEnabled()) {
                    x = chkBoxImages[cb.isSelected() ? 1 : 0];
                } else {
                    x = chkBoxImages[cb.isSelected() ? 3 : 2];
                }
            }
            drawComponent(g, cb, cb.getIconFromState(), x, 0);
        } else {
            Style style = cb.getStyle();

            // checkbox square needs to be the height and width of the font height even
            // when no text is in the check box this is a good indication of phone DPI
            int height = cb.getStyle().getFont().getHeight();

            drawComponent(g, cb, cb.getIconFromState(), null, height);

            int gradientColor;
            g.setColor(style.getFgColor());

            gradientColor = style.getBgColor();

            int width = height;

            int rectWidth = scaleCoordinate(12f, 16, width);
            int tX = cb.getX();
            if (cb.isRTL()) {
            	tX = tX + cb.getWidth() - style.getPaddingLeft(cb.isRTL()) - rectWidth;
            } else {
            	tX += style.getPaddingLeft(cb.isRTL());
            }

            int tY = cb.getY() + style.getPaddingTop() + (cb.getHeight() - style.getPaddingTop() - style.getPaddingBottom()) / 2 - height / 2;
            g.translate(tX, tY);
            int x = scaleCoordinate(1.04f, 16, width);
            int y = scaleCoordinate(4.0f, 16, height);
            int rectHeight = scaleCoordinate(12f, 16, height);

            // brighten or darken the color slightly
            int destColor = findDestColor(gradientColor);

            g.fillLinearGradient(gradientColor, destColor, x + 1, y + 1, rectWidth - 2, rectHeight - 1, false);
            int alpha = g.concatenateAlpha(style.getFgAlpha());
            g.drawRoundRect(x, y, rectWidth, rectHeight, 5, 5);
            g.setAlpha(alpha);
            if (cb.isSelected()) {
                int color = g.getColor();
                g.setColor(0x111111);
                g.translate(0, 1);
                fillCheckbox(g, width, height);
                g.setColor(color);
                g.translate(0, -1);
                fillCheckbox(g, width, height);
            }
            g.translate(-tX, -tY);
        }
    }

    private static void fillCheckbox(Graphics g, int width, int height) {
        int x1 = scaleCoordinate(2.0450495f, 16, width);
        int y1 = scaleCoordinate(9.4227722f, 16, height);
        int x2 = scaleCoordinate(5.8675725f, 16, width);
        int y2 = scaleCoordinate(13.921746f, 16, height);
        int x3 = scaleCoordinate(5.8675725f, 16, width);
        int y3 = scaleCoordinate(11f, 16, height);
        g.fillTriangle(x1, y1, x2, y2, x3, y3);

        x1 = scaleCoordinate(14.38995f, 16, width);
        y1 = scaleCoordinate(0, 16, height);
        g.fillTriangle(x1, y1, x2, y2, x3, y3);
    }

    private static int round(float x) {
        int rounded = (int) x;
        if (x - rounded > 0.5f) {
            return rounded + 1;
        }
        return rounded;
    }

    /**
     * Takes a floating point coordinate on a virtual axis and rusterizes it to 
     * a coordinate in the pixel surface. This is a very simple algorithm since
     * anti-aliasing isn't supported.
     * 
     * @param coordinate a position in a theoretical plain
     * @param plain the amount of space in the theoretical plain
     * @param pixelSize the amount of pixels available on the screen
     * @return the pixel which we should color
     */
    private static int scaleCoordinate(float coordinate, float plain, int pixelSize) {
        return round(coordinate / plain * pixelSize);
    }

    /**
     * {@inheritDoc}
     * @deprecated this method is no longer used by the implementation, we shifted code away to improve performance
     */
    public void drawLabel(Graphics g, Label l) {
        drawComponent(g, l, l.getMaskedIcon(), null, 0);
    }
    
    public Span calculateLabelSpan(TextSelection sel, Label l) {
        Image icon = l.getMaskedIcon();
        Image stateIcon = null;
        int preserveSpaceForState = 0;
        //setFG(g, l);

        int gap = l.getGap();
        int stateIconSize = 0;
        int stateIconYPosition = 0;
        String text = l.getText();
        Style style = l.getStyle();
        int cmpX = l.getX();
        int cmpY = l.getY();
        int cmpHeight = l.getHeight();
        int cmpWidth = l.getWidth();

        boolean rtl = l.isRTL();
        int leftPadding = style.getPaddingLeft(rtl);
        int rightPadding = style.getPaddingRight(rtl);
        int topPadding = style.getPaddingTop();
        int bottomPadding = style.getPaddingBottom();
        
        Font font = style.getFont();
        int fontHeight = 0;
        if (text == null) {
            text = "";
        }
        if(text.length() > 0){
            fontHeight = font.getHeight();
        }
        
        int x = cmpX + leftPadding;
        int y = cmpY + topPadding;
        boolean opposite = false;
        if (stateIcon != null) {
            stateIconSize = stateIcon.getWidth(); //square image width == height
            preserveSpaceForState = stateIconSize + gap;
            stateIconYPosition = cmpY + topPadding
                    + (cmpHeight - topPadding
                    - bottomPadding) / 2 - stateIconSize / 2;
            int tX = cmpX;
            if (((Button) l).isOppositeSide()) {
                if (rtl) {
                    tX += leftPadding;
                } else {
                    tX = tX + cmpWidth - leftPadding - stateIconSize;
                }
                cmpWidth -= leftPadding - stateIconSize;
                preserveSpaceForState = 0;
                opposite = true;
            } else {
                x = cmpX + leftPadding + preserveSpaceForState;
                if (rtl) {
                    tX = tX + cmpWidth - leftPadding - stateIconSize;
                } else {
                    tX += leftPadding;
                }
            }

            //g.drawImage(stateIcon, tX, stateIconYPosition);
        }

        //default for bottom left alignment
        int align = reverseAlignForBidi(l, style.getAlignment());

        int textPos= reverseAlignForBidi(l, l.getTextPosition());

        //set initial x,y position according to the alignment and textPosition
        if (align == Component.LEFT) {
            switch (textPos) {
                case Label.LEFT:
                case Label.RIGHT:
                    y = y + (cmpHeight - (topPadding + bottomPadding + Math.max(((icon != null) ? icon.getHeight() : 0), fontHeight))) / 2;
                    break;
                case Label.BOTTOM:
                case Label.TOP:
                    y = y + (cmpHeight - (topPadding + bottomPadding + ((icon != null) ? icon.getHeight() + gap : 0) + fontHeight)) / 2;
                    break;
            }
        } else if (align == Component.CENTER) {
            switch (textPos) {
                case Label.LEFT:
                case Label.RIGHT:
                    x = x + (cmpWidth - (preserveSpaceForState +
                            leftPadding +
                            rightPadding +
                            ((icon != null) ? icon.getWidth() + l.getGap() : 0) +
                            l.getStringWidth(font))) / 2;
                    if(!opposite){
                        x = Math.max(x, cmpX + leftPadding + preserveSpaceForState);
                    }else{
                        x = Math.min(x, cmpX + leftPadding + preserveSpaceForState);                    
                    }
                    y = y + (cmpHeight - (topPadding +
                            bottomPadding +
                            Math.max(((icon != null) ? icon.getHeight() : 0),
                            fontHeight))) / 2;
                    break;
                case Label.BOTTOM:
                case Label.TOP:
                    x = x + (cmpWidth - (preserveSpaceForState + leftPadding +
                            rightPadding +
                            Math.max(((icon != null) ? icon.getWidth() : 0),
                            l.getStringWidth(font)))) / 2;
                    if(!opposite){
                        x = Math.max(x, cmpX + leftPadding + preserveSpaceForState);
                    }else{
                        x = Math.min(x, cmpX + leftPadding + preserveSpaceForState);                    
                    }
                    y = y + (cmpHeight - (topPadding +
                            bottomPadding +
                            ((icon != null) ? icon.getHeight() + gap : 0) +
                            fontHeight)) / 2;
                    break;
            }
        } else if (align == Component.RIGHT) {
            switch (textPos) {
                case Label.LEFT:
                case Label.RIGHT:
                    x = cmpX + cmpWidth - rightPadding -
                            ( ((icon != null) ? (icon.getWidth() + gap) : 0) +
                            l.getStringWidth(font));
                    if(l.isRTL()) {
                        if(!opposite){
                            x = Math.max(x - preserveSpaceForState, cmpX + leftPadding);
                        }else{
                            x = Math.min(x - preserveSpaceForState, cmpX + leftPadding);                        
                        }
                    } else {
                        if(!opposite){
                            x = Math.max(x, cmpX + leftPadding + preserveSpaceForState);
                        }else{
                            x = Math.min(x, cmpX + leftPadding + preserveSpaceForState);                        
                        }
                    }
                    y = y + (cmpHeight - (topPadding +
                            bottomPadding +
                            Math.max(((icon != null) ? icon.getHeight() : 0),
                            fontHeight))) / 2;
                    break;
                case Label.BOTTOM:
                case Label.TOP:
                    x = cmpX + cmpWidth - rightPadding -
                             (Math.max(((icon != null) ? (icon.getWidth()) : 0),
                            l.getStringWidth(font)));
                    if(!opposite){
                        x = Math.max(x, cmpX + leftPadding + preserveSpaceForState);
                    }else{
                        x = Math.min(x, cmpX + leftPadding + preserveSpaceForState);                    
                    }
                    
                    y = y + (cmpHeight - (topPadding +
                            bottomPadding +
                            ((icon != null) ? icon.getHeight() + gap : 0) + fontHeight)) / 2;
                    break;
            }
        }


        int textSpaceW = cmpWidth - rightPadding - leftPadding;

        if (icon != null && (textPos == Label.RIGHT || textPos == Label.LEFT)) {
            textSpaceW = textSpaceW - icon.getWidth();
        }

        if (stateIcon != null) {
            textSpaceW = textSpaceW - stateIconSize;
        } else {
            textSpaceW = textSpaceW - preserveSpaceForState;
        }

        if (icon == null) { // no icon only string 
            return calculateSpanForLabelString(sel, l, text, x, y, textSpaceW);
        } else {
            int strWidth = l.getStringWidth(font);
            int iconWidth = icon.getWidth();
            int iconHeight = icon.getHeight();
            int iconStringWGap;
            int iconStringHGap;

            switch (textPos) {
                case Label.LEFT:
                    if (iconHeight > fontHeight) {
                        iconStringHGap = (iconHeight - fontHeight) / 2;
                        return calculateSpanForLabelStringValign(sel, l, text, x, y, iconStringHGap, iconHeight, textSpaceW, fontHeight);
                    } else {
                        iconStringHGap = (fontHeight - iconHeight) / 2;
                        //strWidth = drawLabelString(l, text, x, y, textSpaceW);
                        return calculateSpanForLabelString(sel, l, text, x, y, textSpaceW);
                        //g.drawImage(icon, x + strWidth + gap, y + iconStringHGap);
                    }

                case Label.RIGHT:
                    if (iconHeight > fontHeight) {
                        iconStringHGap = (iconHeight - fontHeight) / 2;
                        //g.drawImage(icon, x, y);
                        return calculateSpanForLabelStringValign(sel, l, text, x + iconWidth + gap, y, iconStringHGap, iconHeight, textSpaceW, fontHeight);
                    } else {
                        iconStringHGap = (fontHeight - iconHeight) / 2;
                        //g.drawImage(icon, x, y + iconStringHGap);
                        return calculateSpanForLabelString(sel, l, text, x + iconWidth + gap, y, textSpaceW);
                    }

                case Label.BOTTOM:
                    if (iconWidth > strWidth) { //center align the smaller

                        iconStringWGap = (iconWidth - strWidth) / 2;
                        //g.drawImage(icon, x, y);
                        return calculateSpanForLabelString(sel, l, text, x + iconStringWGap, y + iconHeight + gap, textSpaceW);
                    } else {
                        iconStringWGap = (Math.min(strWidth, textSpaceW) - iconWidth) / 2;
                        //g.drawImage(icon, x + iconStringWGap, y);
                        
                        return calculateSpanForLabelString(sel, l, text, x, y + iconHeight + gap, textSpaceW);
                    }

                case Label.TOP:
                    if (iconWidth > strWidth) { //center align the smaller

                        iconStringWGap = (iconWidth - strWidth) / 2;
                        return calculateSpanForLabelString(sel, l, text, x + iconStringWGap, y, textSpaceW);
                        //g.drawImage(icon, x, y + fontHeight + gap);
                    } else {
                        iconStringWGap = (Math.min(strWidth, textSpaceW) - iconWidth) / 2;
                        return calculateSpanForLabelString(sel, l, text, x, y, textSpaceW);
                        //g.drawImage(icon, x + iconStringWGap, y + fontHeight + gap);
                    }

            }
        }
        return sel.newSpan(l);
    }

    /**
     * {@inheritDoc}
     */
    public void drawRadioButton(Graphics g, Button rb) {
        if (rButtonImages != null) {
            Image x;
            if(rButtonImagesFocus != null && rButtonImagesFocus[0] != null && rb.hasFocus() && Display.getInstance().shouldRenderSelection(rb)) {
                if(rb.isEnabled()) {
                    x = rButtonImagesFocus[rb.isSelected() ? 1 : 0];
                } else {
                    x = rButtonImagesFocus[rb.isSelected() ? 3 : 2];
                }
            } else {
                if(rb.isEnabled()) {
                    x = rButtonImages[rb.isSelected() ? 1 : 0];
                } else {
                    x = rButtonImages[rb.isSelected() ? 3 : 2];
                }
            }
            drawComponent(g, rb, rb.getIconFromState(), x, 0);
        } else {
            Style style = rb.getStyle();

            // radio button radius needs to be of the size of the font height even
            // when no text is in the radio button this is a good indication of phone DPI
            int height = rb.getStyle().getFont().getHeight();

            drawComponent(g, rb, rb.getIconFromState(), null, height + rb.getGap());
            g.setColor(style.getFgColor());
            int x = rb.getX();
            if (rb.isRTL()) {
            	x = x + rb.getWidth() - style.getPaddingLeft(rb.isRTL()) - height;
            } else {
            	x += style.getPaddingLeft(rb.isRTL());
            }

            int y = rb.getY();

            // center the RadioButton
            y += Math.max(0, rb.getHeight() / 2 - height / 2);
            int alpha = g.concatenateAlpha(style.getFgAlpha());
            g.drawArc(x, y, height, height, 0, 360);
            g.setAlpha(alpha);
            if (rb.isSelected()) {
                int color = g.getColor();
                int destColor = findDestColor(color);
                g.fillRadialGradient(color, destColor, x + 3, y + 3, height - 5, height - 5);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void drawComboBox(Graphics g, List cb) {
        int border = 2;
        Style style = cb.getStyle();
        int leftPadding = style.getPaddingLeft(cb.isRTL());
        int rightPadding = style.getPaddingRight(cb.isRTL());

        setFG(g, cb);

        ListModel model = cb.getModel();
        ListCellRenderer renderer = cb.getRenderer();
        Object value = model.getItemAt(model.getSelectedIndex());
        Image comboBoxImage = ((ComboBox)cb).getComboBoxImage() != null ? ((ComboBox)cb).getComboBoxImage() : comboImage;                
        int comboImageWidth;
        if (comboBoxImage != null) {
            comboImageWidth = comboBoxImage.getWidth();
        } else {
            comboImageWidth = style.getFont().getHeight();
        }
        
        int cellX = cb.getX() + style.getPaddingTop();
        if(cb.isRTL()){
            cellX += comboImageWidth;
        }
        
        if (model.getSize() > 0) {
            Component cmp = renderer.getListCellRendererComponent(cb, value, model.getSelectedIndex(), cb.hasFocus());
            cmp.setX(cellX);
            cmp.setY(cb.getY() + style.getPaddingTop());
            cmp.setWidth(cb.getWidth() - comboImageWidth - rightPadding - leftPadding);
            cmp.setHeight(cb.getHeight() - style.getPaddingTop() - style.getPaddingBottom());
            cmp.paint(g);
        }

        g.setColor(style.getBgColor());
        int y = cb.getY();
        int height = cb.getHeight();
        int width = comboImageWidth + border;
        int x = cb.getX();
        if (cb.isRTL()) {
        	x += leftPadding;
        } else {
        	x += cb.getWidth() - comboImageWidth - rightPadding;
        }

        if (comboBoxImage != null) {
            g.drawImage(comboBoxImage, x, y + height / 2 - comboBoxImage.getHeight() / 2);
        } else {
            int color = g.getColor();

            // brighten or darken the color slightly
            int destColor = findDestColor(color);

            if (style.getBgTransparency() > 0) {
                g.fillLinearGradient(g.getColor(), destColor, x, y, width, height, false);
            }
            g.setColor(color);
            int alpha = g.concatenateAlpha(style.getFgAlpha());
            g.drawRect(x, y, width, height - 1);
            g.setAlpha(alpha);
            width--;
            height--;

            //g.drawRect(x, y, width, height);
            g.translate(x + 1, y + 1);
            g.setColor(0x111111);
            int x1 = scaleCoordinate(2.5652081f, 16, width);
            int y1 = scaleCoordinate(4.4753664f, 16, height);
            int x2 = scaleCoordinate(8.2872691f, 16, width);
            int y2 = scaleCoordinate(10f, 16, height);
            int x3 = scaleCoordinate(13.516078f, 16, width);
            int y3 = y1;
            g.fillTriangle(x1, y1, x2, y2, x3, y3);
            g.translate(-1, -1);
            g.setColor(style.getFgColor());
            alpha = g.concatenateAlpha(style.getFgAlpha());
            g.fillTriangle(x1, y1, x2, y2, x3, y3);
            g.setAlpha(alpha);
            g.translate(-x, -y);
        }

    }

    /**
     * Finds a suitable destination color for gradient values
     */
    private int findDestColor(int color) {
        // brighten or darken the color slightly
        int sourceR = color >> 16 & 0xff;
        int sourceG = color >> 8 & 0xff;
        int sourceB = color & 0xff;
        if (sourceR > 128 && sourceG > 128 && sourceB > 128) {
            // darken
            sourceR = Math.max(sourceR >> 1, 0);
            sourceG = Math.max(sourceG >> 1, 0);
            sourceB = Math.max(sourceB >> 1, 0);
        } else {
            // special case for black, since all channels are 0 it can't be brightened properly...
            if(color == 0) {
                return 0x222222;
            }
            
            // brighten
            sourceR = Math.min(sourceR << 1, 0xff);
            sourceG = Math.min(sourceG << 1, 0xff);
            sourceB = Math.min(sourceB << 1, 0xff);
        }
        return ((sourceR << 16) & 0xff0000) | ((sourceG << 8) & 0xff00) | (sourceB & 0xff);
    }

    /**
     * {@inheritDoc}
     */
    public void drawList(Graphics g, List l) {
    }
    
    private int getSelectionHeight(Font f) {
        return f.getHeight() + (int)(f.getHeight() * 0.2);
        //return f.getHeight() + f.getDescent();
    }
    
    private void append(TextSelection sel, Component l, Span span, String text, Font f, int posOffset, int x, int y, int h) {
        int len = text.length();
        int xPos = 0;
        int curPos = 1;
        //int h = f.getHeight() + (int)(f.getHeight() * 0.25);
        //y -= (int)(f.getHeight() * 0.25);
        //int tx = l.getAbsoluteX() - sel.getSelectionRoot().getAbsoluteX() - l.getX();
        //int ty = l.getAbsoluteY() - sel.getSelectionRoot().getAbsoluteY() - l.getY();
        while (curPos <= len) {
            int newXpos = f.stringWidth(text.substring(0, curPos));
            Char next = sel.newChar(posOffset + curPos-1, x + xPos, y, newXpos - xPos, h);
            span.add(next);
            xPos = newXpos;
            curPos++;
        }
    }

    @Override
    public Spans calculateTextAreaSpan(TextSelection sel, TextArea ta) {
        Spans out = sel.newSpans();
        //setFG(g, ta);
        //Span out = sel.newSpan(ta);
        int line = ta.getLines();
        //int oX = g.getClipX();
        //int oY = g.getClipY();
        //int oWidth = g.getClipWidth();
        //int oHeight = g.getClipHeight();
        Font f = ta.getStyle().getFont();
        int fontHeight = f.getHeight();

        int align = reverseAlignForBidi(ta);

        int leftPadding = ta.getStyle().getPaddingLeft(ta.isRTL());
        int rightPadding = ta.getStyle().getPaddingRight(ta.isRTL());
        int topPadding = ta.getStyle().getPaddingTop();
        switch (ta.getVerticalAlignment()) {
            case Component.CENTER :
                topPadding += Math.max(0, (ta.getInnerHeight() - (ta.getRowsGap() + fontHeight) * line)/2);
                break;
            case Component.BOTTOM :
                topPadding += Math.max(0, (ta.getInnerHeight() - (ta.getRowsGap() + fontHeight) * line));
        }
        //boolean shouldBreak = false;
        int posOffset = 0;
        int lastRowBottom = 0;
        for (int i = 0; i < line; i++) {
            Span rowSpan = sel.newSpan(ta);
            int x = ta.getX() + leftPadding;
            int y = ta.getY() +  topPadding +
                    (ta.getRowsGap() + fontHeight) * i;
            int adjustedY = Math.max(y, lastRowBottom);
            int yDiff = adjustedY - y;
            y = adjustedY;
            
            //if(Rectangle.intersects(x, y, ta.getWidth(), fontHeight, oX, oY, oWidth, oHeight)) {
                
                String rowText = (String) ta.getTextAt(i);
                //display ******** if it is a password field
                String displayText = "";
                if ((ta.getConstraint() & TextArea.PASSWORD) != 0) {
                    int rlen = rowText.length();
                    for (int j = 0; j < rlen; j++) {
                        displayText += passwordChar;
                    }
                } else {
                    displayText = rowText;
                }
                posOffset = ta.getText().indexOf(rowText, posOffset);
                switch(align) {
                    case Component.RIGHT:
                		x = ta.getX() + ta.getWidth() - rightPadding - f.stringWidth(displayText);
                        break;
                    case Component.CENTER:
                        x+= (ta.getWidth()-leftPadding-rightPadding-f.stringWidth(displayText))/2;
                        break;
                }
                //int nextY = ta.getY() +  topPadding + (ta.getRowsGap() + fontHeight) * (i + 2);
                //if this is the last line to display and there is more content and isEndsWith3Points() is true
                //add "..." at the last row
                if(ta.isEndsWith3Points() && ta.getGrowLimit() == (i + 1) && ta.getGrowLimit() != line){
                    if(displayText.length() > 3){
                        displayText = displayText.substring(0, displayText.length() - 3);
                    }
                    //g.drawString(displayText + "...", x, y ,ta.getStyle().getTextDecoration()); 
                    append(sel, ta, rowSpan, displayText + "...", f, posOffset, x, y, getSelectionHeight(f) - yDiff);
                    lastRowBottom = rowSpan.getBounds().getY() + rowSpan.getBounds().getHeight();
                    rowSpan = rowSpan.translate(ta.getAbsoluteX() - sel.getSelectionRoot().getAbsoluteX() - ta.getX(), ta.getAbsoluteY() - sel.getSelectionRoot().getAbsoluteY() - ta.getY());
                    out.add(rowSpan);
                    return out;
                }else{            
                    //g.drawString(displayText, x, y ,ta.getStyle().getTextDecoration());
                    append(sel, ta, rowSpan, displayText, f, posOffset, x, y, getSelectionHeight(f) - yDiff);
                    lastRowBottom = rowSpan.getBounds().getY() + rowSpan.getBounds().getHeight();
                    rowSpan = rowSpan.translate(ta.getAbsoluteX() - sel.getSelectionRoot().getAbsoluteX() - ta.getX(), ta.getAbsoluteY() - sel.getSelectionRoot().getAbsoluteY() - ta.getY());
                    out.add(rowSpan);
                }
                posOffset += displayText.length();
                //shouldBreak = true;
            //}else{
            //    if(shouldBreak){
            //        break;
            //    }
            //}
        }
        return out;
    }
    
    /**
     * {@inheritDoc}
     */
    public void drawTextArea(Graphics g, TextArea ta) {
        setFG(g, ta);
        int alpha = g.concatenateAlpha(ta.getStyle().getFgAlpha());

        int line = ta.getLines();
        int oX = g.getClipX();
        int oY = g.getClipY();
        int oWidth = g.getClipWidth();
        int oHeight = g.getClipHeight();
        Font f = ta.getStyle().getFont();
        int fontHeight = f.getHeight();

        int align = reverseAlignForBidi(ta);

        int leftPadding = ta.getStyle().getPaddingLeft(ta.isRTL());
        int rightPadding = ta.getStyle().getPaddingRight(ta.isRTL());
        int topPadding = ta.getStyle().getPaddingTop();
        switch (ta.getVerticalAlignment()) {
            case Component.CENTER :
                topPadding += Math.max(0, (ta.getInnerHeight() - ta.getRowsGap()*(line-1) - fontHeight* line)/2);
                break;
            case Component.BOTTOM :
                topPadding += Math.max(0, (ta.getInnerHeight() - ta.getRowsGap()*(line-1) - fontHeight* line));
        }
        boolean shouldBreak = false;
        
        for (int i = 0; i < line; i++) {
            int x = ta.getX() + leftPadding;
            int y = ta.getY() +  topPadding +
                    (ta.getRowsGap() + fontHeight) * i;
            if(Rectangle.intersects(x, y, ta.getWidth(), fontHeight, oX, oY, oWidth, oHeight)) {
                
                String rowText = (String) ta.getTextAt(i);
                //display ******** if it is a password field
                String displayText = "";
                if ((ta.getConstraint() & TextArea.PASSWORD) != 0) {
                    int rlen = rowText.length();
                    for (int j = 0; j < rlen; j++) {
                        displayText += passwordChar;
                    }
                } else {
                    displayText = rowText;
                }

                switch(align) {
                    case Component.RIGHT:
                		x = ta.getX() + ta.getWidth() - rightPadding - f.stringWidth(displayText);
                        break;
                    case Component.CENTER:
                        x+= (ta.getWidth()-leftPadding-rightPadding-f.stringWidth(displayText))/2;
                        break;
                }
                int nextY = ta.getY() +  topPadding + (ta.getRowsGap() + fontHeight) * (i + 2);
                //if this is the last line to display and there is more content and isEndsWith3Points() is true
                //add "..." at the last row
                if(ta.isEndsWith3Points() && ta.getGrowLimit() == (i + 1) && ta.getGrowLimit() != line){
                    if(displayText.length() > 3){
                        displayText = displayText.substring(0, displayText.length() - 3);
                    }
                    g.drawString(displayText + "...", x, y ,ta.getStyle().getTextDecoration());                
                    return;
                }else{            
                    g.drawString(displayText, x, y ,ta.getStyle().getTextDecoration());
                }
                shouldBreak = true;
            }else{
                if(shouldBreak){
                    break;
                }
            }
        }
        g.setAlpha(alpha);
    }

    private static final Image[] threeImageCache = new Image[3];
    /**
     * {@inheritDoc}
     */
    public Dimension getButtonPreferredSize(Button b) {
        threeImageCache[0] = b.getMaskedIcon();
        threeImageCache[1] = b.getRolloverIcon();
        threeImageCache[2] = b.getPressedIcon();
        return getPreferredSize(b, threeImageCache, null);
    }

    /**
     * {@inheritDoc}
     */
    public Dimension getCheckBoxPreferredSize(Button cb) {
        if(cb.isToggle()) {
            return getButtonPreferredSize(cb);
        }
        threeImageCache[0] = cb.getMaskedIcon();
        threeImageCache[1] = cb.getRolloverIcon();
        threeImageCache[2] = cb.getPressedIcon();
        if (chkBoxImages != null) {
            return getPreferredSize(cb, threeImageCache, chkBoxImages[0]);
        }
        Dimension d = getPreferredSize(cb, threeImageCache, null);

        // checkbox square needs to be the height and width of the font height even
        // when no text is in the check box this is a good indication of phone DPI
        int checkBoxSquareSize = cb.getStyle().getFont().getHeight();

        // allow for checkboxes without a string within them
        d.setHeight(Math.max(checkBoxSquareSize, d.getHeight()));

        d.setWidth(d.getWidth() + checkBoxSquareSize + cb.getGap());
        return d;
    }

    private static final Image[] oneImageCache = new Image[1];
    
    /**
     * {@inheritDoc}
     */
    public Dimension getLabelPreferredSize(Label l) {
        oneImageCache[0] = l.getMaskedIcon();
        return getPreferredSize(l, oneImageCache, null);
    }

    /**
     * {@inheritDoc}
     */
    private Dimension getPreferredSize(Label l, Image[] icons, Image stateImage) {
        int prefW = 0;
        int prefH = 0;

        Style style = l.getStyle();
        int gap = l.getGap();
        int ilen = icons.length;
        for (int i = 0; i < ilen; i++) {
            Image icon = icons[i];
            if (icon != null) {
                prefW = Math.max(prefW, icon.getWidth());
                prefH = Math.max(prefH, icon.getHeight());
            }
        }
        String text = l.getText();
        Font font = style.getFont();
        if (font == null) {
            System.out.println("Missing font for " + l);
            font = Font.getDefaultFont();
        }
        if (text != null && text.length() > 0) {
            //add the text size
            switch (l.getTextPosition()) {
                case Label.LEFT:
                case Label.RIGHT:
                    prefW += font.stringWidth(text);
                    prefH = Math.max(prefH, font.getHeight());
                    break;
                case Label.BOTTOM:
                case Label.TOP:
                    prefW = Math.max(prefW, font.stringWidth(text));
                    prefH += font.getHeight();
                    break;
            }
        }
        //add the state image(relevant for CheckBox\RadioButton)
        if (stateImage != null) {
            prefW += (stateImage.getWidth() + gap);
            prefH = Math.max(prefH, stateImage.getHeight());
        }


        if (icons[0] != null && text != null && text.length() > 0) {
            switch (l.getTextPosition()) {
                case Label.LEFT:
                case Label.RIGHT:
                    prefW += gap;
                    break;
                case Label.BOTTOM:
                case Label.TOP:
                    prefH += gap;
                    break;
            }
        }

        if(l.isShowEvenIfBlank()) {
            prefH += style.getVerticalPadding();
            prefW += style.getHorizontalPadding();
        } else {
            if (prefH != 0) {
                prefH += style.getVerticalPadding();
            }
            if (prefW != 0) {
                prefW += style.getHorizontalPadding();
            }
        }

        if(style.getBorder() != null) {
            prefW = Math.max(style.getBorder().getMinimumWidth(), prefW);
            prefH = Math.max(style.getBorder().getMinimumHeight(), prefH);
        } 
        if(isBackgroundImageDetermineSize() && style.getBgImage() != null) {
            prefW = Math.max(style.getBgImage().getWidth(), prefW);
            prefH = Math.max(style.getBgImage().getHeight(), prefH);
        }

        return new Dimension(prefW, prefH);
    }

    /**
     * {@inheritDoc}
     */
    public Dimension getListPreferredSize(List l) {
        Dimension d = getListPreferredSizeImpl(l);
        Style style = l.getStyle();
        if(style.getBorder() != null) {
            d.setWidth(Math.max(style.getBorder().getMinimumWidth(), d.getWidth()));
            d.setHeight(Math.max(style.getBorder().getMinimumHeight(), d.getHeight()));
        }
        return d;
    }

    private Dimension getListPreferredSizeImpl(List l) {
        int width = 0;
        int height = 0;
        int selectedHeight;
        int selectedWidth;
        ListModel model = l.getModel();
        int numOfcomponents = Math.max(model.getSize(), l.getMinElementHeight());
        numOfcomponents = Math.min(numOfcomponents, l.getMaxElementHeight());
        Object prototype = l.getRenderingPrototype();

        Style unselectedEntryStyle = null;
        Style selectedEntryStyle;
        if(prototype != null) {
            ListCellRenderer renderer = l.getRenderer();
            Component cmp = renderer.getListCellRendererComponent(l, prototype, 0, false);
            height = cmp.getPreferredH();
            width = cmp.getPreferredW();
            unselectedEntryStyle = cmp.getStyle();
            cmp = renderer.getListCellRendererComponent(l, prototype, 0, true);

            selectedEntryStyle = cmp.getStyle();
            selectedHeight = Math.max(height, cmp.getPreferredH());
            selectedWidth = Math.max(width, cmp.getPreferredW());
        } else {
            int hightCalcComponents = Math.min(l.getListSizeCalculationSampleCount(), numOfcomponents);
            Object dummyProto = l.getRenderingPrototype();
            if(model.getSize() > 0 && dummyProto == null) {
                dummyProto = model.getItemAt(0);
            }
            ListCellRenderer renderer = l.getRenderer();
            for (int i = 0; i < hightCalcComponents; i++) {
                Object value;
                if(i < model.getSize()) {
                    value = model.getItemAt(i);
                } else {
                    value = dummyProto;
                }
                Component cmp = renderer.getListCellRendererComponent(l, value, i, false);
                if(cmp instanceof Container) {
                    cmp.setShouldCalcPreferredSize(true);
                }
                unselectedEntryStyle = cmp.getStyle();

                height = Math.max(height, cmp.getPreferredH());
                width = Math.max(width, cmp.getPreferredW());
            }
            selectedEntryStyle = unselectedEntryStyle;
            selectedHeight = height;
            selectedWidth = width;
            if (model.getSize() > 0) {
                Object value = model.getItemAt(0);
                Component cmp = renderer.getListCellRendererComponent(l, value, 0, true);
                if(cmp instanceof Container) {
                    cmp.setShouldCalcPreferredSize(true);
                }

                selectedHeight = Math.max(height, cmp.getPreferredH());
                selectedWidth = Math.max(width, cmp.getPreferredW());
                selectedEntryStyle = cmp.getStyle();
            }
        }
        if(unselectedEntryStyle != null) {
            selectedWidth += selectedEntryStyle.getMarginLeftNoRTL() + selectedEntryStyle.getMarginRightNoRTL();
            selectedHeight += selectedEntryStyle.getMarginTop() + selectedEntryStyle.getMarginBottom();
            width += unselectedEntryStyle.getMarginLeftNoRTL() + unselectedEntryStyle.getMarginRightNoRTL();
            height += unselectedEntryStyle.getMarginTop() + unselectedEntryStyle.getMarginBottom();
        }

        Style lStyle = l.getStyle();
        int verticalPadding = lStyle.getPaddingTop() + lStyle.getPaddingBottom();
        int horizontalPadding = lStyle.getPaddingRightNoRTL() + lStyle.getPaddingLeftNoRTL() + l.getSideGap();

        if (numOfcomponents == 0) {
            return new Dimension(horizontalPadding, verticalPadding);
        }

        // If combobox without ever importing the ComboBox class dependency
        if(l.getOrientation() > List.HORIZONTAL) {
            int boxWidth = l.getStyle().getFont().getHeight() + 2;
            return new Dimension(boxWidth + selectedWidth + horizontalPadding, selectedHeight + verticalPadding);
        } else {
            if (l.getOrientation() == List.VERTICAL) {
                return new Dimension(selectedWidth + horizontalPadding, selectedHeight + (height + l.getItemGap()) * (numOfcomponents - 1) + verticalPadding);
            } else {
                return new Dimension(selectedWidth + (width + l.getItemGap()) * (numOfcomponents - 1) + horizontalPadding, selectedHeight + verticalPadding);
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public Dimension getRadioButtonPreferredSize(Button rb) {
        if(rb.isToggle()) {
            return getButtonPreferredSize(rb);
        }
        threeImageCache[0] = rb.getMaskedIcon();
        threeImageCache[1] = rb.getRolloverIcon();
        threeImageCache[2] = rb.getPressedIcon();
        if (rButtonImages != null) {
            return getPreferredSize(rb, threeImageCache, rButtonImages[0]);
        }
        Dimension d = getPreferredSize(rb, threeImageCache, null);

        // radio button radius needs to be of the size of the font height even
        // when no text is in the radio button this is a good indication of phone DPI
        int height = rb.getStyle().getFont().getHeight();

        // allow for radio buttons without a string within them
        d.setHeight(Math.max(height, d.getHeight()));

        if(rButtonImages != null && rButtonImages.length > 0) {
            d.setWidth(rButtonImages[0].getWidth() + d.getWidth() + rb.getGap());
        } else {
            d.setWidth(d.getWidth() + height + rb.getGap());
        }
        return d;
    }

    /**
     * {@inheritDoc}
     */
    public Dimension getTextAreaSize(TextArea ta, boolean pref) {
        int prefW = 0;
        int prefH = 0;
        Style style = ta.getStyle();
        Font f = style.getFont();

        //if this is a text field the preferred size should be the text width
        if (ta.getRows() == 1) {
            prefW = f.stringWidth(ta.getText());
        } else {
            prefW = f.charWidth(TextArea.getWidestChar()) * ta.getColumns();
        }
        int rows;
        if(pref) {
            rows = ta.getActualRows();
        } else {
            rows = ta.getLines();
        }
        prefH = (f.getHeight() + ta.getRowsGap()) * rows;
        if(!ta.isActAsLabel()) {
            int columns = ta.getColumns();
            String str = "";
            for (int iter = 0; iter < columns; iter++) {
                str += TextArea.getWidestChar();
            }
            if(columns > 0) {
                prefW = Math.max(prefW, f.stringWidth(str));
            }
        }
        prefH = Math.max(prefH, rows * f.getHeight());

        prefW += style.getPaddingRightNoRTL() + style.getPaddingLeftNoRTL();
        prefH += style.getPaddingTop() + style.getPaddingBottom();
        if(style.getBorder() != null) {
            prefW = Math.max(style.getBorder().getMinimumWidth(), prefW);
            prefH = Math.max(style.getBorder().getMinimumHeight(), prefH);
        }
        if(isBackgroundImageDetermineSize() && style.getBgImage() != null) {
            prefW = Math.max(style.getBgImage().getWidth(), prefW);
            prefH = Math.max(style.getBgImage().getHeight(), prefH);
        }

        return new Dimension(prefW, prefH);
    }

    /**
     * Reverses alignment in the case of bidi
     */
    private int reverseAlignForBidi(Component c) {
        return reverseAlignForBidi(c, c.getStyle().getAlignment());
    }

    /**
     * Reverses alignment in the case of bidi
     */
    private int reverseAlignForBidi(Component c, int align) {
        if(c.isRTL()) {
            switch(align) {
                case Component.RIGHT:
                    return Component.LEFT;
                case Component.LEFT:
                    return Component.RIGHT;
            }
        }
        return align;
    }

    private void drawComponent(Graphics g, Label l, Image icon, Image stateIcon, int preserveSpaceForState) {
        setFG(g, l);
        int alpha = g.concatenateAlpha(l.getStyle().getFgAlpha());
        int gap = l.getGap();
        int stateIconSize = 0;
        int stateIconYPosition = 0;
        String text = l.getText();
        Style style = l.getStyle();
        int cmpX = l.getX();
        int cmpY = l.getY();
        int cmpHeight = l.getHeight();
        int cmpWidth = l.getWidth();

        boolean rtl = l.isRTL();
        int leftPadding = style.getPaddingLeft(rtl);
        int rightPadding = style.getPaddingRight(rtl);
        int topPadding = style.getPaddingTop();
        int bottomPadding = style.getPaddingBottom();
        
        Font font = style.getFont();
        int fontHeight = 0;
        if (text == null) {
            text = "";
        }
        if(text.length() > 0){
            fontHeight = font.getHeight();
        }
        
        int x = cmpX + leftPadding;
        int y = cmpY + topPadding;
        boolean opposite = false;
        boolean stateButtonOnLeft = false;
        if (stateIcon != null) {
            stateIconSize = stateIcon.getWidth(); //square image width == height
            preserveSpaceForState = stateIconSize + gap;
            stateIconYPosition = cmpY + topPadding
                    + (cmpHeight - topPadding
                    - bottomPadding) / 2 - stateIconSize / 2;
            int tX = cmpX;
            if (((Button) l).isOppositeSide()) {
                if (rtl) {
                    tX += leftPadding;
                    stateButtonOnLeft = true;
                    x = cmpX + leftPadding + preserveSpaceForState;
                } else {
                    tX = tX + cmpWidth - leftPadding - stateIconSize;
                }
                //cmpWidth -= leftPadding - stateIconSize;
                //preserveSpaceForState = 0;
                opposite = true;
            } else {
                
                if (rtl) {
                    tX = tX + cmpWidth - leftPadding - stateIconSize;
                } else {
                    x = cmpX + leftPadding + preserveSpaceForState;
                    tX += leftPadding;
                    stateButtonOnLeft = true;
                }
            }

            g.drawImage(stateIcon, tX, stateIconYPosition);
        }

        //default for bottom left alignment
        int align = reverseAlignForBidi(l, style.getAlignment());

        int textPos= reverseAlignForBidi(l, l.getTextPosition());

        //set initial x,y position according to the alignment and textPosition
        if (align == Component.LEFT) {
            switch (textPos) {
                case Label.LEFT:
                case Label.RIGHT:
                    y = y + (cmpHeight - (topPadding + bottomPadding + Math.max(((icon != null) ? icon.getHeight() : 0), fontHeight))) / 2;
                    if (textPos == Label.RIGHT && stateIcon == null) {
                        x += preserveSpaceForState;
                    }
                    break;
                case Label.BOTTOM:
                case Label.TOP:
                    y = y + (cmpHeight - (topPadding + bottomPadding + ((icon != null) ? icon.getHeight() + gap : 0) + fontHeight)) / 2;
                    break;
            }
        } else if (align == Component.CENTER) {
            switch (textPos) {
                case Label.LEFT:
                case Label.RIGHT:
                    x = x + (cmpWidth - (preserveSpaceForState +
                            leftPadding +
                            rightPadding +
                            ((icon != null) ? icon.getWidth() + l.getGap() : 0) +
                            l.getStringWidth(font))) / 2;
                    y = y + (cmpHeight - (topPadding +
                            bottomPadding +
                            Math.max(((icon != null) ? icon.getHeight() : 0),
                            fontHeight))) / 2;
                    break;
                case Label.BOTTOM:
                case Label.TOP:
                    x = x + (cmpWidth - (preserveSpaceForState + leftPadding +
                            rightPadding +
                            Math.max(((icon != null) ? icon.getWidth() + l.getGap() : 0),
                            l.getStringWidth(font)))) / 2;
                    if(!opposite){
                        x = Math.max(x, cmpX + leftPadding + preserveSpaceForState);
                    }else{
                        x = Math.min(x, cmpX + leftPadding + preserveSpaceForState);                    
                    }
                    y = y + (cmpHeight - (topPadding +
                            bottomPadding +
                            ((icon != null) ? icon.getHeight() + gap : 0) +
                            fontHeight)) / 2;
                    break;
            }
        } else if (align == Component.RIGHT) {
            switch (textPos) {
                case Label.LEFT:
                case Label.RIGHT:
                    x = cmpX + cmpWidth - rightPadding -
                            ( ((icon != null) ? (icon.getWidth() + gap) : 0) +
                            l.getStringWidth(font));
                    x = stateButtonOnLeft ? x : x - preserveSpaceForState;

                    y = y + (cmpHeight - (topPadding +
                            bottomPadding +
                            Math.max(((icon != null) ? icon.getHeight() : 0),
                            fontHeight))) / 2;
                    break;
                case Label.BOTTOM:
                case Label.TOP:
                    x = cmpX + cmpWidth - rightPadding -
                             (Math.max(((icon != null) ? (icon.getWidth()) : 0),
                            l.getStringWidth(font)));
                    if(!opposite){
                        x = Math.max(x, cmpX + leftPadding + preserveSpaceForState);
                    }else{
                        x = Math.min(x, cmpX + leftPadding + preserveSpaceForState);                    
                    }
                    
                    y = y + (cmpHeight - (topPadding +
                            bottomPadding +
                            ((icon != null) ? icon.getHeight() + gap : 0) + fontHeight)) / 2;
                    break;
            }
        }


        int textSpaceW = cmpWidth - rightPadding - leftPadding;
        int textSpaceX = cmpX + leftPadding;
        

        if (icon != null && (textPos == Label.RIGHT || textPos == Label.LEFT)) {
            textSpaceW = textSpaceW - icon.getWidth();
            textSpaceX = (textPos == Label.RIGHT) ? textSpaceX + icon.getWidth() :
                    textSpaceX;
        }

        textSpaceW = textSpaceW - preserveSpaceForState;
        textSpaceX = stateButtonOnLeft ? textSpaceX + preserveSpaceForState : textSpaceX;

        if (icon == null) { // no icon only string 
            drawLabelString(g, l, text, x, y, textSpaceX, textSpaceW);
        } else {
            int strWidth = l.getStringWidth(font);
            int iconWidth = icon.getWidth();
            int iconHeight = icon.getHeight();
            int iconStringWGap;
            int iconStringHGap;

            switch (textPos) {
                case Label.LEFT:
                    if (iconHeight > fontHeight) {
                        iconStringHGap = (iconHeight - fontHeight) / 2;
                        strWidth = drawLabelStringValign(g, l, text, x, y, iconStringHGap, iconHeight, textSpaceX, textSpaceW, fontHeight);

                        g.drawImage(icon, x + strWidth + gap, y);
                    } else {
                        strWidth = drawLabelString(g, l, text, x, y, textSpaceX, textSpaceW);
                        drawLabelImageValign(g, l, icon, x + strWidth + gap, y, fontHeight, iconHeight);
                    }
                    break;
                case Label.RIGHT:
                    if (iconHeight > fontHeight) {
                        iconStringHGap = (iconHeight - fontHeight) / 2;
                        g.drawImage(icon, x, y);
                        drawLabelStringValign(g, l, text, x + iconWidth + gap, y, iconStringHGap, iconHeight, textSpaceX, textSpaceW, fontHeight);
                    } else {
                        drawLabelImageValign(g, l, icon, x, y, fontHeight, iconHeight);
                        drawLabelString(g, l, text, x + iconWidth + gap, y, textSpaceX, textSpaceW);
                    }
                    break;
                case Label.BOTTOM:
                    if (iconWidth > strWidth) { //center align the smaller

                        iconStringWGap = (iconWidth - strWidth) / 2;
                        g.drawImage(icon, x, y);
                        drawLabelString(g, l, text, x + iconStringWGap, y + iconHeight + gap, textSpaceX, textSpaceW);
                    } else {
                        iconStringWGap = (Math.min(strWidth, textSpaceW) - iconWidth) / 2;
                        g.drawImage(icon, x + iconStringWGap, y);
                        
                        drawLabelString(g, l, text, x, y + iconHeight + gap, textSpaceX, textSpaceW);
                    }
                    break;
                case Label.TOP:
                    if (iconWidth > strWidth) { //center align the smaller

                        iconStringWGap = (iconWidth - strWidth) / 2;
                        drawLabelString(g, l, text, x + iconStringWGap, y, textSpaceX, textSpaceW);
                        g.drawImage(icon, x, y + fontHeight + gap);
                    } else {
                        iconStringWGap = (Math.min(strWidth, textSpaceW) - iconWidth) / 2;
                        drawLabelString(g, l, text, x, y, textSpaceX, textSpaceW);
                        g.drawImage(icon, x + iconStringWGap, y + fontHeight + gap);
                    }
                    break;
            }
        }
        
        String badgeText = l.getBadgeText();
        if (badgeText != null && badgeText.length() > 0) {
            
            Component badgeCmp = l.getBadgeStyleComponent();
            int badgePaddingTop = CN.convertToPixels(1);
            int badgePaddingBottom = badgePaddingTop;
            int badgePaddingLeft = badgePaddingTop;
            int badgePaddingRight = badgePaddingTop;
            int fgColor = 0xffffff;
            int bgColor = 0x666666;
            int strokeColor = bgColor;
            Font badgeFont = null;
            Style badgeStyle;
            if (badgeCmp == null) {
                badgeStyle = l.getUIManager().getComponentStyle("Badge");
                
            } else {
                badgeStyle = badgeCmp.getStyle();
            }
            if (badgeStyle != null) {
                fgColor = badgeStyle.getFgColor();
                bgColor = badgeStyle.getBgColor();
                if (badgeStyle.getBorder() instanceof RoundBorder) {
                    strokeColor = ((RoundBorder)badgeStyle.getBorder()).getStrokeColor();
                } else {
                    strokeColor = bgColor;
                }
                badgeFont = badgeStyle.getFont();
                badgePaddingTop = badgeStyle.getPaddingTop();
                badgePaddingBottom = badgeStyle.getPaddingBottom();
                badgePaddingLeft = badgeStyle.getPaddingLeftNoRTL();
                badgePaddingRight = badgeStyle.getPaddingRightNoRTL();
            }
            if (badgeFont == null) {
                if (Font.isNativeFontSchemeSupported()) {
                    badgeFont = Font.createTrueTypeFont(Font.NATIVE_MAIN_LIGHT).derive(fontHeight/2, 0);
                } else {
                    badgeFont = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
                }
            } 
            
            int badgeFontHeight = badgeFont.getHeight();
            int badgeTextWidth = badgeFont.stringWidth(badgeText);
            GeneralPath path = new GeneralPath();
            
            Rectangle rect = new Rectangle(
                    cmpX + cmpWidth - badgeTextWidth - badgePaddingLeft - badgePaddingRight, 
                    cmpY, 
                    badgePaddingLeft + badgePaddingRight + badgeTextWidth, 
                    badgePaddingTop+badgePaddingBottom+badgeFontHeight
            );
            if (rect.getWidth() < rect.getHeight()) {
                rect.setX(cmpX + cmpWidth - rect.getHeight());
                rect.setWidth(rect.getHeight());
            }

            path.moveTo(rect.getX() + rect.getHeight()/2, rect.getY());
            path.lineTo(rect.getX() + rect.getWidth() - rect.getHeight()/2, rect.getY());
            path.arcTo(rect.getX() + rect.getWidth() - rect.getHeight()/2, rect.getY() + rect.getHeight() / 2, rect.getX() + rect.getWidth() - rect.getHeight()/2, rect.getY() + rect.getHeight(), true);
            path.lineTo(rect.getX() + rect.getHeight()/2, rect.getY() + rect.getHeight());
            path.arcTo(rect.getX() + rect.getHeight()/2, rect.getY() + rect.getHeight()/2, rect.getX() + rect.getHeight()/2, rect.getY(), true);
            path.closePath();
            
            int col = g.getColor();
            boolean antialias = g.isAntiAliased();
            g.setAntiAliased(true);
            g.setColor(bgColor);
            
            
            g.fillShape(path);
            if (bgColor != strokeColor) {
                g.setColor(strokeColor);
                int alpha2 = g.concatenateAlpha(badgeStyle.getFgAlpha());
                g.drawShape(path, new Stroke(1, Stroke.CAP_SQUARE, Stroke.JOIN_MITER, 1f));
                g.setAlpha(alpha2);
            }
            
            
            g.setColor(fgColor);
            g.setFont(badgeFont);
            int alpha2 = g.concatenateAlpha(badgeStyle.getFgAlpha());
            g.drawString(badgeText, rect.getX() + rect.getWidth()/2 - badgeTextWidth/2, rect.getY() + badgePaddingTop);
            g.setAlpha(alpha2);
            
            
            g.setColor(col);
            g.setAntiAliased(antialias);
            
            
            
            
            
        }
        g.setAlpha(alpha);
    }

    private void drawLabelImageValign(Graphics g, Label l, Image icon, int x, int y, int fontHeight, int iconHeight) {
        int iconStringHGap = (fontHeight - iconHeight) / 2;
        //int strWidth = drawLabelString(g, l, text, x, y, textSpaceX, textSpaceW);
        switch (l.getVerticalAlignment()) {
            case Component.TOP:
                g.drawImage(icon, x, y + iconStringHGap);
                break;
            case Component.BOTTOM:
                g.drawImage(icon, x, y + fontHeight - iconHeight);
                break;
            case Component.BASELINE:
                Font iconFont = l.getIconStyleComponent().getStyle().getFont();
                Font textFont = l.getStyle().getFont();
                if (iconFont == null) {
                    iconFont = Font.getDefaultFont();
                }
                if (textFont == null) {
                    textFont = Font.getDefaultFont();
                }
                iconStringHGap = textFont.getAscent() - iconFont.getAscent();
                g.drawImage(icon, x, y + iconStringHGap);
                break;
            default:
                g.drawImage(icon, x, y + iconStringHGap);

        }
    }
    
    /**
     * Implements the drawString for the text component and adjust the valign
     * assuming the icon is in one of the sides
     */
    private int drawLabelStringValign(Graphics g, Label l, String str, int x, int y,
            int iconStringHGap, int iconHeight, int textSpaceX, int textSpaceW, int fontHeight) {
        if (str.length() == 0) {
            return 0;
        }
        switch (l.getVerticalAlignment()) {
            case Component.TOP:
                return drawLabelString(g, l, str, x, y, textSpaceX, textSpaceW);
            case Component.CENTER:
                return drawLabelString(g, l, str, x, y + iconHeight / 2 - fontHeight / 2, textSpaceX, textSpaceW);
            case Component.BASELINE:
                Font iconFont = l.getIconStyleComponent().getStyle().getFont();
                Font textFont = l.getStyle().getFont();
                if (iconFont == null) {
                    iconFont = Font.getDefaultFont();
                }
                if (textFont == null) {
                    textFont = Font.getDefaultFont();
                }
                int ascentDiff = iconFont.getAscent() - textFont.getAscent();
                return drawLabelString(g, l, str, x, y + ascentDiff, textSpaceX, textSpaceW);
                
            default:
                return drawLabelString(g, l, str, x, y + iconStringHGap, textSpaceX, textSpaceW);
        }
    }
    
    private Span calculateSpanForLabelStringValign(TextSelection sel, Label l, String str, int x, int y,
            int iconStringHGap, int iconHeight, int textSpaceW, int fontHeight) {
        switch (l.getVerticalAlignment()) {
            case Component.TOP:
                return calculateSpanForLabelString(sel, l, str, x, y, textSpaceW);
            case Component.CENTER:
                return calculateSpanForLabelString(sel, l, str, x, y + iconHeight / 2 - fontHeight / 2, textSpaceW);
            case Component.BASELINE:
                Font iconFont = l.getIconStyleComponent().getStyle().getFont();
                Font textFont = l.getStyle().getFont();
                if (iconFont == null) {
                    iconFont = Font.getDefaultFont();
                }
                if (textFont == null) {
                    textFont = Font.getDefaultFont();
                }
                int ascentDiff = iconFont.getAscent() - textFont.getAscent();
                return calculateSpanForLabelString(sel, l, str, x, y + ascentDiff, textSpaceW);
            default:
                return calculateSpanForLabelString(sel, l, str, x, y + iconStringHGap, textSpaceW);
        }
    }

    /**
     * Implements the drawString for the text component and adjust the valign
     * assuming the icon is in one of the sides
     */
    private int drawLabelString(Graphics g, Label l, String text, int x, int y, int textSpaceX, int textSpaceW) {
        if (text.length() == 0) {
            return 0;
        }
        Style style = l.getStyle();

        int cx = g.getClipX();
        int cy = g.getClipY();
        int cw = g.getClipWidth();
        int ch = g.getClipHeight();
        //g.pushClip();
        g.clipRect(textSpaceX, cy, textSpaceW, ch);

        if (l.isTickerRunning()) {
            Font font = style.getFont();
            if (l.getShiftText() > 0) {
                if (l.getShiftText() > textSpaceW) {
                    l.setShiftText(x - l.getX() - l.getStringWidth(font));
                }
            } else if (l.getShiftText() + l.getStringWidth(font) < 0) {
                l.setShiftText(textSpaceW);
            }
        }
        int drawnW = drawLabelText(g, l, text, x, y, textSpaceW);

        g.setClip(cx, cy, cw, ch);
        //g.popClip();

        return drawnW;
    }
    
    private Span calculateSpanForLabelString(TextSelection sel, Label l, String text, int x, int y, int textSpaceW) {
        Style style = l.getStyle();

        

        if (l.isTickerRunning()) {
            Font font = style.getFont();
            if (l.getShiftText() > 0) {
                if (l.getShiftText() > textSpaceW) {
                    l.setShiftText(x - l.getX() - l.getStringWidth(font));
                }
            } else if (l.getShiftText() + l.getStringWidth(font) < 0) {
                l.setShiftText(textSpaceW);
            }
        }
        return calculateSpanForLabelText(sel, l, text, x, y, textSpaceW);

    }

    /**
     * Draws the text of a label
     * 
     * @param g graphics context
     * @param l label component
     * @param text the text for the label
     * @param x position for the label
     * @param y position for the label
     * @param textSpaceW the width available for the component
     * @return the space used by the drawing
     */
    protected int drawLabelText(Graphics g, Label l, String text, int x, int y, int textSpaceW) {
        Style style = l.getStyle();
        Font f = style.getFont();
        boolean rtl = l.isRTL();
        boolean isTickerRunning = l.isTickerRunning();
        int txtW = l.getStringWidth(f);
        if ((!isTickerRunning) || rtl) {
            //if there is no space to draw the text add ... at the end
            if (txtW > textSpaceW && textSpaceW > 0) {
            	// Handling of adding 3 points and in fact all text positioning when the text is bigger than
            	// the allowed space is handled differently in RTL, this is due to the reverse algorithm
            	// effects - i.e. when the text includes both Hebrew/Arabic and English/numbers then simply
            	// trimming characters from the end of the text (as done with LTR) won't do.
            	// Instead we simple reposition the text, and draw the 3 points, this is quite simple, but
            	// the downside is that a part of a letter may be shown here as well.

            	if (rtl) {
                	if ((!isTickerRunning) && (l.isEndsWith3Points())) {
	            		String points = "...";
	                	int pointsW = f.stringWidth(points);
	            		g.drawString(points, l.getShiftText() + x, y,l.getStyle().getTextDecoration());
	            		g.clipRect(pointsW+l.getShiftText() + x, y, textSpaceW - pointsW, f.getHeight());
                	}
            		//x = x - txtW + textSpaceW;
                } else {
                    if (l.isEndsWith3Points()) {
                        String points = "...";
                        int index = 1;
                        int widest = f.charWidth('W');
                        int pointsW = f.stringWidth(points);
                        int tlen = text.length();
                        while (fastCharWidthCheck(text, index, textSpaceW - pointsW, widest, f) && index < tlen){
                            index++;
                        }
                        text = text.substring(0, Math.min(text.length(), Math.max(1, index-1))) + points;
                        txtW =  f.stringWidth(text);
                    }
                }
            }
        }

        g.drawString(text, l.getShiftText() + x, y,style.getTextDecoration());
        return Math.min(txtW, textSpaceW);
    }

    private boolean fastCharWidthCheck(String s, int length, int width, int charWidth, Font f) {
        if(length * charWidth < width) {
            return true;
        }
        length = Math.min(s.length(), length);
        return f.substringWidth(s, 0, length) < width;
    }
    
    /**
     * {@inheritDoc}
     */
    public Dimension getComboBoxPreferredSize(List cb) {
        Dimension d = getListPreferredSize(cb);
        Image comboBoxImage = ((ComboBox)cb).getComboBoxImage() != null ? ((ComboBox)cb).getComboBoxImage() : comboImage;
        if(comboBoxImage != null) {
            d.setWidth(d.getWidth() + comboBoxImage.getWidth());
            d.setHeight(Math.max(d.getHeight(), comboBoxImage.getHeight()));
        }
        return d;
    }


    /**
     * Similar to getText() but works properly with password fields
     */
    protected String getTextFieldString(TextArea ta) {
        String txt = ta.getText();
        String text;
        if(ta.isSingleLineTextArea()){
            text = txt;
        }else{
            text = (String) ta.getTextAt(ta.getCursorY());
            if(ta.getCursorPosition() + text.length() < txt.length()){
                char c = txt.charAt(ta.getCursorPosition() + text.length());
                if(c == '\n'){
                    text += "\n";
                }else if(c == ' '){
                    text += " ";            
                }
            }
        }
        
        String displayText = "";
        if ((ta.getConstraint() & TextArea.PASSWORD) != 0) {
            // show the last character in a password field
            if (ta.isPendingCommit()) {
                if (text.length() > 0) {
                    int tlen = text.length();
                    for (int j = 0; j < tlen - 1; j++) {
                        displayText += passwordChar;
                    }
                    displayText += text.charAt(text.length() - 1);
                }
            } else {
                for (int j = 0; j < text.length(); j++) {
                    displayText += passwordChar;
                }
            }
        } else {
            displayText = text;
        }
        return displayText;
    }
    
    public Spans calculateTextFieldSpan(TextSelection sel, TextArea ta) {
        //setFG(g, ta);
        Span out = sel.newSpan(ta);
        // display ******** if it is a password field
        String displayText = getTextFieldString(ta);

        Style style = ta.getStyle();
        int x = 0;
        int cursorCharPosition = ta.hasFocus() ? ta.getCursorPosition() : 0;//ta.getCursorX();        
        Font f = style.getFont();
        int cursorX = 0;
        int xPos = 0;
        
        int align = reverseAlignForBidi(ta);
        int displayX = 0;

        String inputMode = ta.getInputMode();
        int inputModeWidth = f.stringWidth(inputMode);

        // QWERTY devices don't quite have an input mode hide it also when we have a VK
        if(!Display.getInstance().platformUsesInputMode() || ta.isQwertyInput() || Display.getInstance().isVirtualKeyboardShowing()) {
            inputMode = "";
            inputModeWidth = 0;
        }
        if (ta.isSingleLineTextArea()) {
            // there is currently no support for CENTER aligned text fields
            if (align == Component.LEFT) {
                if (cursorCharPosition > 0) {
                    cursorCharPosition = Math.min(displayText.length(), 
                        cursorCharPosition);
                    xPos = f.stringWidth(displayText.substring(0, cursorCharPosition));
                    cursorX = ta.getX() + style.getPaddingLeft(ta.isRTL()) + xPos;

                    // no point in showing the input mode when there is only one input mode...
                    if (inputModeWidth > 0 && ta.getInputModeOrder() != null && ta.getInputModeOrder().length == 1) {
                        inputModeWidth = 0;
                    }
                    if (ta.isEnableInputScroll()) {
                        if (ta.getWidth() > (f.getHeight() * 2) && cursorX >= ta.getWidth() - inputModeWidth - style.getPaddingLeft(ta.isRTL())) {
                            if (x + xPos >= ta.getWidth() - inputModeWidth - style.getPaddingLeft(ta.isRTL()) * 2) {
                                x=ta.getWidth() - inputModeWidth - style.getPaddingLeft(ta.isRTL()) * 2 - xPos - 1;
                            }
                        }
                    }
                }
                displayX = ta.getX() + x + style.getPaddingLeft(ta.isRTL());
            } else {
                x = 0;
                cursorX = getTextFieldCursorX(ta);
                int baseX = ta.getX() + style.getPaddingLeftNoRTL() + inputModeWidth;
                int endX = ta.getX() + ta.getWidth() - style.getPaddingRightNoRTL();

                if (cursorX < baseX) {
                    x = baseX - cursorX;
                } else {
                    if (cursorX > endX) {
                        x = endX - cursorX;
                    }
                }

                displayX = ta.getX() + ta.getWidth() - style.getPaddingRightNoRTL() - style.getPaddingLeftNoRTL() - f.stringWidth(displayText) + x;
            }

            //int cx = g.getClipX();
            //int cy = g.getClipY();
            //int cw = g.getClipWidth();
            //int ch = g.getClipHeight();
            //int clipx = ta.getX() + style.getPaddingLeft(ta.isRTL());
            //int clipw = ta.getWidth() - style.getPaddingLeft(ta.isRTL()) - style.getPaddingRight(ta.isRTL());
            //g.pushClip();
            //g.clipRect(clipx, cy, clipw, ch);

            //int xOffset = 0;
            //int len = displayText.length();
            //int charH = f.getHeight();
            int h = getSelectionHeight(f);
            switch(ta.getVerticalAlignment()) {
                case Component.BOTTOM:
                    //g.drawString(displayText, displayX, ta.getY() + ta.getHeight() - style.getPaddingBottom() - f.getHeight(), style.getTextDecoration());
                    append(sel, ta, out, displayText, f, 0, displayX, ta.getY() + ta.getHeight() - style.getPaddingBottom() - h, h);
                   // c = sel.newChar(i, charX, ta.getY() + ta.getHeight() - style.getPaddingBottom() - f.getHeight(), charW , charH);
                    break;
                case Component.CENTER:
                    //g.drawString(displayText, displayX, ta.getY() + ta.getHeight() / 2  - f.getHeight() / 2, style.getTextDecoration());
                    //c = sel.newChar(i, charX, ta.getY() + ta.getHeight() / 2  - f.getHeight() / 2, charW, charH);
                    append(sel, ta, out, displayText, f, 0, displayX,  ta.getY() + ta.getHeight() / 2  - h / 2, h);
                    break;
                default:
                    //g.drawString(displayText, displayX, ta.getY() + style.getPaddingTop(), style.getTextDecoration());
                    //c = sel.newChar(i, charX, ta.getY() + style.getPaddingTop(), charW, charH);
                    append(sel, ta, out, displayText, f, 0, displayX,  ta.getY() + style.getPaddingTop(), h);
                    break;
            }
            
            
            //g.setClip(cx, cy, cw, ch);
            //g.popClip();
            out = out.translate(ta.getAbsoluteX() - sel.getSelectionRoot().getAbsoluteX() - ta.getX(), ta.getAbsoluteY() - sel.getSelectionRoot().getAbsoluteY() - ta.getY());
            Spans spansOut = sel.newSpans();
            spansOut.add(out);
            return spansOut;
            
        } else {
            //drawTextArea(g, ta);
            return calculateTextAreaSpan(sel, ta);
        }
        
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void drawTextField(Graphics g, TextArea ta) {
        setFG(g, ta);
        int alpha = g.concatenateAlpha(ta.getStyle().getFgAlpha());
        // display ******** if it is a password field
        String displayText = getTextFieldString(ta);

        Style style = ta.getStyle();
        int x = 0;
        int cursorCharPosition = ta.hasFocus() ? ta.getCursorPosition() : 0;//ta.getCursorX();        
        Font f = style.getFont();
        int cursorX = 0;
        int xPos = 0;
        
        int align = reverseAlignForBidi(ta);
        int displayX = 0;

        String inputMode = ta.getInputMode();
        int inputModeWidth = f.stringWidth(inputMode);

        // QWERTY devices don't quite have an input mode hide it also when we have a VK
        if(!Display.getInstance().platformUsesInputMode() || ta.isQwertyInput() || Display.getInstance().isVirtualKeyboardShowing()) {
            inputMode = "";
            inputModeWidth = 0;
        }
        if (ta.isSingleLineTextArea()) {
            // there is currently no support for CENTER aligned text fields
            if (align == Component.LEFT) {
                if (cursorCharPosition > 0) {
                    cursorCharPosition = Math.min(displayText.length(), 
                        cursorCharPosition);
                    xPos = f.stringWidth(displayText.substring(0, cursorCharPosition));
                    cursorX = ta.getX() + style.getPaddingLeft(ta.isRTL()) + xPos;

                    // no point in showing the input mode when there is only one input mode...
                    if (inputModeWidth > 0 && ta.getInputModeOrder() != null && ta.getInputModeOrder().length == 1) {
                        inputModeWidth = 0;
                    }
                    if (ta.isEnableInputScroll()) {
                        if (ta.getWidth() > (f.getHeight() * 2) && cursorX >= ta.getWidth() - inputModeWidth - style.getPaddingLeft(ta.isRTL())) {
                            if (x + xPos >= ta.getWidth() - inputModeWidth - style.getPaddingLeft(ta.isRTL()) * 2) {
                                x=ta.getWidth() - inputModeWidth - style.getPaddingLeft(ta.isRTL()) * 2 - xPos - 1;
                            }
                        }
                    }
                }
                displayX = ta.getX() + x + style.getPaddingLeft(ta.isRTL());
            } else {
                x = 0;
                cursorX = getTextFieldCursorX(ta);
                int baseX = ta.getX() + style.getPaddingLeftNoRTL() + inputModeWidth;
                int endX = ta.getX() + ta.getWidth() - style.getPaddingRightNoRTL();

                if (cursorX < baseX) {
                    x = baseX - cursorX;
                } else {
                    if (cursorX > endX) {
                        x = endX - cursorX;
                    }
                }

                displayX = ta.getX() + ta.getWidth() - style.getPaddingRightNoRTL() - style.getPaddingLeftNoRTL() - f.stringWidth(displayText) + x;
            }

            int cx = g.getClipX();
            int cy = g.getClipY();
            int cw = g.getClipWidth();
            int ch = g.getClipHeight();
            int clipx = ta.getX() + style.getPaddingLeft(ta.isRTL());
            int clipw = ta.getWidth() - style.getPaddingLeft(ta.isRTL()) - style.getPaddingRight(ta.isRTL());
            //g.pushClip();
            g.clipRect(clipx, cy, clipw, ch);

            switch(ta.getVerticalAlignment()) {
                case Component.BOTTOM:
                    g.drawString(displayText, displayX, ta.getY() + ta.getHeight() - style.getPaddingBottom() - f.getHeight(), style.getTextDecoration());
                    break;
                case Component.CENTER:
                    g.drawString(displayText, displayX, ta.getY() + ta.getHeight() / 2  - f.getHeight() / 2, style.getTextDecoration());
                    break;
                default:
                    g.drawString(displayText, displayX, ta.getY() + style.getPaddingTop(), style.getTextDecoration());
                    break;
            }
            g.setClip(cx, cy, cw, ch);
            //g.popClip();
            
        } else {
            drawTextArea(g, ta);
        }
        // no point in showing the input mode when there is only one input mode...
        if(inputModeWidth > 0 && ta.getInputModeOrder() != null && ta.getInputModeOrder().length > 1) {
            
            if (ta.handlesInput() && ta.getWidth() / 2 > inputModeWidth) {
            	
                int drawXPos = ta.getX() + style.getPaddingLeft(ta.isRTL());
                if((!ta.isRTL() && style.getAlignment() == Component.LEFT) ||
                    (ta.isRTL() && style.getAlignment() == Component.RIGHT)) {
                    drawXPos = drawXPos + ta.getWidth() - inputModeWidth - style.getPaddingRightNoRTL() - style.getPaddingLeftNoRTL();
                } 
                g.setColor(style.getFgColor());
                int inputIndicatorY = ta.getY()+ ta.getScrollY() + ta.getHeight() -  
                        style.getPaddingBottom() - 
                        f.getHeight();
                g.fillRect(drawXPos, inputIndicatorY, inputModeWidth,
                        f.getHeight(), (byte) 140);
                g.setColor(style.getBgColor());
                g.drawString(inputMode, drawXPos, inputIndicatorY);
            }
        }
        g.setAlpha(alpha);
    }


    /**
     * Returns true if the given character is an RTL character or a space
     * character
     *
     * @param c character to test
     * @return true if bidi is active and this is a
     */
	private boolean isRTLOrWhitespace(char c) {
        return (Display.getInstance().isRTL(c)) || c == ' ';
    }

    /**
     * Calculates the position of the text field cursor within the string
     */
    private int getTextFieldCursorX(TextArea ta) {
        Style style = ta.getStyle();
        Font f = style.getFont();

        // display ******** if it is a password field
        String displayText = getTextFieldString(ta);
        String inputMode = ta.getInputMode();
        int inputModeWidth = f.stringWidth(inputMode);
        // QWERTY devices don't quite have an input mode hide it also when we have a VK
        if(ta.isQwertyInput() || Display.getInstance().isVirtualKeyboardShowing()) {
            inputMode = "";
            inputModeWidth = 0;
        }

        int xPos = 0;
        int cursorCharPosition = ta.getCursorX();
        int cursorX=0;
        int x = 0;

        if (reverseAlignForBidi(ta) == Component.RIGHT) {
        	if (Display.getInstance().isBidiAlgorithm()) {
                //char[] dest = displayText.toCharArray();
                cursorCharPosition = Display.getInstance().getCharLocation(displayText, cursorCharPosition-1);

                if (cursorCharPosition==-1) {
                    xPos = f.stringWidth(displayText);
                } else {
                    displayText = Display.getInstance().convertBidiLogicalToVisual(displayText);
                    if (!isRTLOrWhitespace((displayText.charAt(cursorCharPosition)))) {
                        cursorCharPosition++;
                    }
                    xPos = f.stringWidth(displayText.substring(0, cursorCharPosition));
                } 
        	}
        	int displayX = ta.getX() + ta.getWidth() - style.getPaddingLeft(ta.isRTL()) - f.stringWidth(displayText);
        	cursorX = displayX + xPos;
        	x=0;
        } else {
            if (cursorCharPosition > 0) {
                cursorCharPosition = Math.min(displayText.length(), 
                        cursorCharPosition);
                xPos = f.stringWidth(displayText.substring(0, cursorCharPosition));
            }
            cursorX = ta.getX() + style.getPaddingLeft(ta.isRTL()) + xPos;

            if (ta.isSingleLineTextArea() && ta.getWidth() > (f.getHeight() * 2) && cursorX >= ta.getWidth() - inputModeWidth  -style.getPaddingLeft(ta.isRTL())) {
                if (x + xPos >= ta.getWidth() - inputModeWidth - style.getPaddingLeftNoRTL() - style.getPaddingRightNoRTL()) {
                    x = ta.getWidth() - inputModeWidth - style.getPaddingLeftNoRTL() - style.getPaddingRightNoRTL() - xPos -1;
                }
            }
        }

        return cursorX+x;
    }

    /**
     * {@inheritDoc}
     */
    public Dimension getTextFieldPreferredSize(TextArea ta) {
        return getTextAreaSize(ta, true);
    }

    /**
     * {@inheritDoc}
     */
     public void drawTextFieldCursor(Graphics g, TextArea ta) {
         Style style = ta.getStyle();
         Font f = style.getFont();


        int cursorY;
        if(ta.isSingleLineTextArea()) {
            switch(ta.getVerticalAlignment()) {
                case Component.BOTTOM:
                    cursorY = ta.getY() + ta.getHeight() -  f.getHeight();
                    break;
                case Component.CENTER:
                    cursorY = ta.getY() + ta.getHeight() / 2 -  f.getHeight() / 2;
                    break;
                default:
                    cursorY = ta.getY() + style.getPaddingTop();
                    break;
            }
         } else {
            cursorY = ta.getY() + style.getPaddingTop() + ta.getCursorY() * (ta.getRowsGap() + f.getHeight());
         }
    	int cursorX = getTextFieldCursorX(ta);

        int align = reverseAlignForBidi(ta);
		int x=0;
        if (align==Component.RIGHT) {
            String inputMode = ta.getInputMode();
            int inputModeWidth = f.stringWidth(inputMode);

    		int baseX=ta.getX()+style.getPaddingLeftNoRTL()+inputModeWidth;
    		if (cursorX<baseX) {
    			x=baseX-cursorX;
    		}
        }

        int oldColor = g.getColor();
        int alpha = g.concatenateAlpha(style.getFgAlpha());
        if(getTextFieldCursorColor() == 0) {
            g.setColor(style.getFgColor());
         } else {
            g.setColor(getTextFieldCursorColor());
         }
        g.drawLine(cursorX + x, cursorY, cursorX + x, cursorY + f.getHeight());
        g.setColor(oldColor);
        g.setAlpha(alpha);
    }
     
     private FontImage getDefaultRefreshIcon() {
            Style s = new Style(UIManager.getInstance().getComponentStyle("Label"));
            s.setFont(Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE));
            return FontImage.createMaterial(FontImage.MATERIAL_ARROW_UPWARD, s);
     }

    /**
     * {@inheritDoc}
     */
    public void drawPullToRefresh(Graphics g, final Component cmp, boolean taskExecuted) {
        final Form parentForm = cmp.getComponentForm();
        final int scrollY = cmp.getScrollY();
        Component cmpToDraw;
        if (taskExecuted) {
            cmpToDraw = updating;
        } else {
            if (-scrollY > getPullToRefreshHeight()) {
                cmpToDraw = releaseToRefresh;
            } else {
                cmpToDraw = pullDown;
            }
        }

        if (pull.getComponentAt(0) != updating && cmpToDraw != pull.getComponentAt(0)) {
            
            parentForm.registerAnimated(new Animation() {

                int counter = 0;
                Image i;
                
                {
                    i = UIManager.getInstance().getThemeImageConstant("pullToRefreshImage");
                    if(i == null) {
                        i = getDefaultRefreshIcon();
                    }
                }
 
                public boolean animate() {
                    counter++;

                    if (pull.getComponentAt(0) == releaseToRefresh) {
                        ((Label) releaseToRefresh).setIcon(i.rotate(180 - (180 / 6)*counter));
                    } else {
                        ((Label) pullDown).setIcon(i.rotate(180 *counter/ 6));
                    }
                    if (counter == 6) {
                        ((Label) releaseToRefresh).setIcon(i);
                        ((Label) pullDown).setIcon(i.rotate(180));                        
                        parentForm.deregisterAnimated(this);
                    }
                    
                    // Placing the repaint inside a callSerially() because repaint directly
                    // inside animate causes painting artifacts in many instances
                    CN.callSerially(new Runnable() {
                        public void run() {
                            cmp.repaint(cmp.getAbsoluteX(), cmp.getAbsoluteY()-getPullToRefreshHeight(), cmp.getWidth(), 
                            getPullToRefreshHeight());
                        }
                    });
                    
                    
                    
                    return false;
                }

                public void paint(Graphics g) {
                }
            });
            
        }
        if(pull.getComponentAt(0) != cmpToDraw 
                && cmpToDraw instanceof Label 
                && (pull.getComponentAt(0) instanceof Label)){
            ((Label)cmpToDraw).setIcon(((Label)pull.getComponentAt(0)).getIcon());
        }
        Component current = pull.getComponentAt(0);
        if(current != cmpToDraw) {
            pull.replace(current, cmpToDraw, null);
        }

        pull.setWidth(cmp.getWidth());
        pull.setX(cmp.getAbsoluteX());
        pull.setY(cmp.getY() -scrollY - getPullToRefreshHeight());
        pull.layoutContainer();
        
        // We need to make the InfiniteProgress to animate, otherwise the progress
        // just stays static.
        ComponentSelector.select("*", pull).each(new ComponentClosure() {
            

            @Override
            public void call(Component c) {
                if (c instanceof InfiniteProgress) {
                    ((InfiniteProgress)c).animate(true);
                } else {
                    c.animate();
                }
            }
        });
        pull.paintComponent(g);

    }

    /**
     * {@inheritDoc}
     */
    public int getPullToRefreshHeight() {
        if (pull == null) {
            BorderLayout bl = new BorderLayout();
            bl.setCenterBehavior(BorderLayout.CENTER_BEHAVIOR_CENTER_ABSOLUTE);
            pull = new Container(bl);
        }
        if (pullDown == null) {
            pullDown = new Label(getUIManager().localize("pull.down", "Pull down to refresh..."));
            pullDown.setUIID("PullToRefresh");
            
            Image i = UIManager.getInstance().getThemeImageConstant("pullToRefreshImage");
            if(i == null) {
                i = getDefaultRefreshIcon();
            }
            i = i.rotate(180);
            ((Label) pullDown).setIcon(i);
        }
        if (releaseToRefresh == null) {
            releaseToRefresh = new Label(getUIManager().localize("pull.release", "Release to refresh..."));
            releaseToRefresh.setUIID("PullToRefresh");
            Image i = UIManager.getInstance().getThemeImageConstant("pullToRefreshImage");
            if(i == null) {
                i = getDefaultRefreshIcon();
            }
            ((Label) releaseToRefresh).setIcon(i);
        }
        if (updating == null) {
            updating = new Container(new BoxLayout(BoxLayout.X_AXIS));
            ((Container) updating).addComponent(new InfiniteProgress());
            Label l = new Label(getUIManager().localize("pull.refresh", "Updating..."));
            l.setUIID("PullToRefresh");
            ((Container) updating).addComponent(l);

            pull.getUnselectedStyle().setPadding(0, 0, 0 , 0);
            pull.getUnselectedStyle().setMargin(0, 0, 0 , 0);
            pull.addComponent(BorderLayout.CENTER, updating);
            pull.layoutContainer();
            pull.setHeight(Math.max(pullDown.getPreferredH(), pull.getPreferredH()));
        }
        String s = UIManager.getInstance().getThemeConstant("pullToRefreshHeight", null);
        if(s != null) {
            float f = Util.toFloatValue(s);
            if(f > 0) {
                return Display.getInstance().convertToPixels(f);
            }
        }
        return pull.getHeight();
    }
     

    /**
     * {@inheritDoc}
     */
    public void focusGained(Component cmp) {
        if(cmp instanceof Label) {
            Label l = (Label) cmp;
            if (l.isTickerEnabled() && l.shouldTickerStart() && Display.getInstance().shouldRenderSelection(cmp)) {
                ((Label) cmp).startTicker(getTickerSpeed(), true);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void focusLost(Component cmp) {
        if(cmp instanceof Label) {
            Label l = (Label) cmp;
            if (l.isTickerRunning()) {
                l.stopTicker();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void refreshTheme(boolean b) {
        chkBoxImages = null;
        comboImage = null;
        rButtonImages = null;
        chkBoxImagesFocus = null;
        rButtonImagesFocus = null;
        super.refreshTheme(b);
        UIManager m = getUIManager();
        Image combo = m.getThemeImageConstant("comboImage");
        if(combo != null) {
            setComboBoxImage(combo);
        } else {
            if(Font.isNativeFontSchemeSupported()) {
                Style c = UIManager.getInstance().createStyle("ComboBox.", "", false);
                combo = FontImage.createMaterial(FontImage.MATERIAL_ARROW_DROP_DOWN, c);
                setComboBoxImage(combo);
            }
        }
        updateCheckBoxConstants(m, false, "");
        updateCheckBoxConstants(m, true, "Focus");
        updateRadioButtonConstants(m, false, "");
        updateRadioButtonConstants(m, true, "Focus");        
    }
    
    private void updateCheckBoxConstants(UIManager m, boolean focus, String append) {
        Image checkSel = m.getThemeImageConstant("checkBoxChecked" + append + "Image");
        if(checkSel != null) {
            Image checkUnsel = m.getThemeImageConstant("checkBoxUnchecked" + append + "Image");
            if(checkUnsel != null) {
                Image disUnsel = m.getThemeImageConstant("checkBoxUncheckDis" + append + "Image");
                Image disSel = m.getThemeImageConstant("checkBoxCheckDis" + append + "Image");
                if(disSel == null) {
                    disSel = checkSel;
                }
                if(disUnsel == null) {
                    disUnsel = checkUnsel;
                }
                if(focus) {
                    setCheckBoxFocusImages(checkSel, checkUnsel, disSel, disUnsel);
                } else {
                    setCheckBoxImages(checkSel, checkUnsel, disSel, disUnsel);
                }
            }
            if(checkUnsel != null) {
                if(focus) {
                    setCheckBoxFocusImages(checkSel, checkUnsel, checkSel, checkUnsel);
                } else {
                    setCheckBoxImages(checkSel, checkUnsel);
                }
            }
        } else {
            if(!Font.isTrueTypeFileSupported()) {
                return;
            }
            UIManager uim = UIManager.getInstance();
            Style unsel = uim.createStyle("CheckBox.", "", false);
            Style sel = uim.createStyle("CheckBox.", "sel#", true);
            Style dis = uim.createStyle("CheckBox.", "dis#", false);
            FontImage checkedDis = FontImage.createMaterial(FontImage.MATERIAL_CHECK_BOX, dis);
            FontImage uncheckedDis = FontImage.createMaterial(FontImage.MATERIAL_CHECK_BOX_OUTLINE_BLANK, sel);
            if(focus) {
                FontImage checkedSelected = FontImage.createMaterial(FontImage.MATERIAL_CHECK_BOX, sel);
                FontImage uncheckedSelected = FontImage.createMaterial(FontImage.MATERIAL_CHECK_BOX_OUTLINE_BLANK, sel);
                setCheckBoxFocusImages(checkedSelected, uncheckedSelected, checkedDis, uncheckedDis);
            } else {
                FontImage checkedUnselected = FontImage.createMaterial(FontImage.MATERIAL_CHECK_BOX, unsel);
                FontImage uncheckedUnselected = FontImage.createMaterial(FontImage.MATERIAL_CHECK_BOX_OUTLINE_BLANK, unsel);
                setCheckBoxImages(checkedUnselected, uncheckedUnselected, checkedDis, uncheckedDis);
            }            
        }
    }

    private void updateRadioButtonConstants(UIManager m, boolean focus, String append) {
        Image radioSel = m.getThemeImageConstant("radioSelected" + append + "Image");
        if(radioSel != null) {
            Image radioUnsel = m.getThemeImageConstant("radioUnselected" + append + "Image");
            if(radioUnsel != null) {
                Image disUnsel = m.getThemeImageConstant("radioUnselectedDis" + append + "Image");
                Image disSel = m.getThemeImageConstant("radioSelectedDis" + append + "Image");
                if(disUnsel == null) {
                    disUnsel = radioUnsel;
                }
                if(disSel == null) {
                    disSel = radioSel;
                }
                if(focus) {
                    setRadioButtonFocusImages(radioSel, radioUnsel, disSel, disUnsel);
                } else {
                    setRadioButtonImages(radioSel, radioUnsel, disSel, disUnsel);
                }
            }
        } else {
            if(!Font.isTrueTypeFileSupported()) {
                return;
            }
            UIManager uim = UIManager.getInstance();
            Style unsel = uim.createStyle("RadioButton.", "", false);
            Style sel = uim.createStyle("RadioButton.", "sel#", true);
            Style dis = uim.createStyle("RadioButton.", "dis#", false);
            FontImage checkedDis = FontImage.createMaterial(FontImage.MATERIAL_RADIO_BUTTON_CHECKED, dis);
            FontImage uncheckedDis = FontImage.createMaterial(FontImage.MATERIAL_RADIO_BUTTON_UNCHECKED, sel);
            if(focus) {
                FontImage checkedSelected = FontImage.createMaterial(FontImage.MATERIAL_RADIO_BUTTON_CHECKED, sel);
                FontImage uncheckedSelected = FontImage.createMaterial(FontImage.MATERIAL_RADIO_BUTTON_UNCHECKED, sel);
                setRadioButtonFocusImages(checkedSelected, uncheckedSelected, checkedDis, uncheckedDis);
            } else {
                FontImage checkedUnselected = FontImage.createMaterial(FontImage.MATERIAL_RADIO_BUTTON_CHECKED, unsel);
                FontImage uncheckedUnselected = FontImage.createMaterial(FontImage.MATERIAL_RADIO_BUTTON_UNCHECKED, unsel);
                setRadioButtonImages(checkedUnselected, uncheckedUnselected, checkedDis, uncheckedDis);
            }
        }
    }

   
    public Span calculateSpanForLabelText(TextSelection sel, Label l, String text, int x, int y, int textSpaceW) {
        
        Span span = sel.newSpan(l);
        Style style = l.getStyle();
        Font f = style.getFont();
        int h = f.getHeight() + (int)(f.getHeight() * 0.25);
        y -= (int)(f.getHeight() * 0.25);
        boolean rtl = l.isRTL();
        boolean isTickerRunning = l.isTickerRunning();
        int txtW = l.getStringWidth(f);
        int curPos = text.length();
        if ((!isTickerRunning) || rtl) {
            //if there is no space to draw the text add ... at the end
            if (txtW > textSpaceW && textSpaceW > 0) {
            	// Handling of adding 3 points and in fact all text positioning when the text is bigger than
            	// the allowed space is handled differently in RTL, this is due to the reverse algorithm
            	// effects - i.e. when the text includes both Hebrew/Arabic and English/numbers then simply
            	// trimming characters from the end of the text (as done with LTR) won't do.
            	// Instead we simple reposition the text, and draw the 3 points, this is quite simple, but
            	// the downside is that a part of a letter may be shown here as well.

            	if (rtl) {
                	if ((!isTickerRunning) && (l.isEndsWith3Points())) {
	            		String points = "...";
	                	int pointsW = f.stringWidth(points);
                                //xPos = f.stringWidth(displayText.substring(0, cursorCharPosition));
	            		//g.drawString(points, l.getShiftText() + x, y,l.getStyle().getTextDecoration());
	            		//g.clipRect(pointsW+l.getShiftText() + x, y, textSpaceW - pointsW, f.getHeight());
                                Char nextChar = sel.newChar(curPos, pointsW+l.getShiftText() + x, y, textSpaceW - pointsW, h);
                                span.add(nextChar);
                	}
            		x = x - txtW + textSpaceW;
                } else {
                    if (l.isEndsWith3Points()) {
                        String points = "...";
                        int index = 1;
                        int widest = f.charWidth('W');
                        int pointsW = f.stringWidth(points);
                        int tlen = text.length();
                        while (fastCharWidthCheck(text, index, textSpaceW - pointsW, widest, f) && index < tlen){
                            index++;
                        }
                        text = text.substring(0, Math.min(text.length(), Math.max(1, index-1))) + points;
                        txtW =  f.stringWidth(text);
                    }
                }
            }
        }

        int len = text.length();
        int xPos = 0;
        curPos = 1;
        while (curPos <= len) {
            int newXpos = f.stringWidth(text.substring(0, curPos));
            Char next = sel.newChar(curPos-1, l.getShiftText() + x + xPos, y, newXpos - xPos, h);
            span.add(next);
            xPos = newXpos;
            curPos++;
        }
        //System.out.println("Span: "+span);
        return span.translate(l.getAbsoluteX() - sel.getSelectionRoot().getAbsoluteX() - l.getX(), l.getAbsoluteY() - sel.getSelectionRoot().getAbsoluteY() - l.getY());
    }
}
