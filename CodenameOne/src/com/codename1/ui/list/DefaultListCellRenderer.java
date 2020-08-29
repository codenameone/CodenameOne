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
package com.codename1.ui.list;

import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.List;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import java.util.Map;

/**
 * Default implementation of the renderer based on a label see the {@link ListCellRenderer}
 * for more information about the use and purpose of this class
 *
 * @author Chen Fishbein
 */
public class DefaultListCellRenderer<T> extends Label implements ListCellRenderer<T>, CellRenderer<T> {
    private boolean showNumbers;
    private boolean showNumbersForce;
    private static boolean showNumbersDefault = true;
    private Label focusComponent = new Label();
    private boolean rightAlignNumbers;
    private boolean alwaysRenderSelection;
    
    /** 
     * Creates a new instance of DefaultCellRenderer 
     */
    public DefaultListCellRenderer() {
        super("");
        setCellRenderer(true);
        setEndsWith3Points(false);
        focusComponent.setUIID("ListRendererFocus");
        focusComponent.setFocus(true);
        setUIID("ListRenderer");
    }

    /**
     * {@inheritDoc}
     */
    protected void initLaf(UIManager uim) {
        super.initLaf(uim);
        if(!showNumbersForce) {
            showNumbers = uim.isThemeConstant("rendererShowsNumbersBool", showNumbersDefault);
        }
    }
    
    
    

    /**
     * {@inheritDoc}
     */
    public void refreshTheme(boolean merge) {
        super.refreshTheme(merge);
        focusComponent.refreshTheme(merge);
    }

    /** 
     * Creates a new instance of DefaultCellRenderer 
     * 
     * @param showNumbers indicates numbers should be shown
     */
    public DefaultListCellRenderer(boolean showNumbers) {
        this();
        showNumbersForce = true;
        this.showNumbers = showNumbers;
    }


    /**
     * {@inheritDoc}
     */
    public Component getCellRendererComponent(Component list, Object model, T value, int index, boolean isSelected) {
        if(!alwaysRenderSelection && !Display.getInstance().shouldRenderSelection(list)) {
            isSelected = false;
        }
        setFocus(isSelected);
        if(showNumbers) {
            String text = "" + value;
            Map<String, String> t =  UIManager.getInstance().getBundle();
            if(t != null && value != null) {
                Object o = t.get(value.toString());
                if(o != null) {
                    text = (String)o;
                }
            }
            if(isRTL()){
                setText(text+ " ." + (index + 1));
            }else{
                setText("" + (index + 1) + ". " + text);
            }
        } else {
            if(value != null) {
                String v = value.toString();
                setText(v);
                if(isRightAlignNumbers()) {
                    char c = v.charAt(0);
                    Style s = getStyle();
                    if(c >= '0' && c <= '9') {
                        s.setAlignment(RIGHT);
                    } else {
                        s.setAlignment(LEFT);
                    }
                }
            } else {
                setText("null");
            }
        }
        if(value instanceof Command) {
            Image i = ((Command)value).getIcon();
            if(i == null) {
                if(((Command)value).getMaterialIcon() != 0) {
                    if(((Command)value).getIconFont()!=null) {
                        setFontIcon(((Command)value).getIconFont(),((Command)value).getMaterialIcon());
                    } else {
                        setMaterialIcon(((Command)value).getMaterialIcon());
                    }
                } else {
                    setIcon(null);
                }
            } else {
                setIcon(i);
            }
            setEnabled(((Command)value).isEnabled());
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public Style getStyle() {
        if (alwaysRenderSelection && hasFocus()) {
            return getSelectedStyle();
        }

        return super.getStyle();
    }

    /**
     * {@inheritDoc}
     */
    public Component getListCellRendererComponent(List list, T value, int index, boolean isSelected) {
        return getCellRendererComponent(list, list.getModel(), value, index, isSelected);
    }

    /**
     * {@inheritDoc}
     */
    public Component getListFocusComponent(List list) {
        return focusComponent;
    }
    /**
     * Overriden to do nothing and remove a performance issue where renderer changes
     * perform needless repaint calls
     */
    public void repaint() {
    }

    /**
     * Indicate whether numbering should exist for the default cell renderer
     * 
     * @return true if numers are shown by the numbers
     */
    public boolean isShowNumbers() {
        return showNumbers;
    }

    /**
     * Indicate whether numbering should exist for the default cell renderer
     * 
     * @param showNumbers indicate whether numbering should exist for the default cell renderer
     */
    public void setShowNumbers(boolean showNumbers) {
        this.showNumbers = showNumbers;
        showNumbersForce = true;
    }

    /**
     * The background transparency factor to apply to the selection focus
     * 
     * @return selection transperancy value
     */
    public int getSelectionTransparency() {
        return focusComponent.getUnselectedStyle().getBgTransparency() & 0xff;
    }

    /**
     * The background transparency factor to apply to the selection focus
     * 
     * @param selectionTransparency the selection transperancy value
     */
    public void setSelectionTransparency(int selectionTransparency) {
        focusComponent.getUnselectedStyle().setBgTransparency(selectionTransparency);
    }

    /**
     * Indicates whether the default list cell renderer will show numbers by default
     * when constructed
     *
     * @param def true to show numbers for all renderers created in the future
     */
    public static void setShowNumbersDefault(boolean def) {
        showNumbersDefault = def;
    }

    /**
     * Indicates whether the default list cell renderer will show numbers by default
     * when constructed
     *
     * @return true when showing numbers, false otherwise
     */
    public static boolean isShowNumbersDefault() {
        return showNumbersDefault;
    }

    /**
     * {@inheritDoc}
     */
    public Component getFocusComponent(Component list) {
        return focusComponent;
    }

    /**
     * @return the rightAlignNumbers
     */
    public boolean isRightAlignNumbers() {
        return rightAlignNumbers;
    }

    /**
     * @param rightAlignNumbers the rightAlignNumbers to set
     */
    public void setRightAlignNumbers(boolean rightAlignNumbers) {
        this.rightAlignNumbers = rightAlignNumbers;
    }

    /**
     * Indicates that selection should always be rendered regardless of the status of the shouldRenderSelection flag
     * 
     * @return the alwaysRenderSelection
     */
    public boolean isAlwaysRenderSelection() {
        return alwaysRenderSelection;
    }

    /**
     * Indicates that selection should always be rendered regardless of the status of the shouldRenderSelection flag
     * 
     * @param alwaysRenderSelection the alwaysRenderSelection to set
     */
    public void setAlwaysRenderSelection(boolean alwaysRenderSelection) {
        this.alwaysRenderSelection = alwaysRenderSelection;
    }
}
