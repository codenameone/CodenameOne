/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

import com.codename1.ui.animations.ComponentAnimation;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.plaf.Border;
import java.util.ArrayList;

/**
 * <p>Encapsulates a text field and label into a single component. This allows the UI to adapt for iOS/Android 
 * behavior differences and support features like floating hint when necessary. It also includes platform specific 
 * error handling logic.</p>
 * <p>
 * It is highly recommended to use text components in the context of a {@link com.codename1.ui.layouts.TextModeLayout}
 * This allows the layout to implicitly adapt to the on-top mode and use a box layout Y mode for iOS and other
 * platforms.
 * </p>
 * <p>
 * This class supports several theme constants:
 * </p>
 * <ol>
 * <li>{@code textComponentErrorColor} a hex RGB color which defaults to null in which case this has no effect. 
 *      When defined this will change the color of the border and label to the given color to match the material design
 *      styling.
 * <li>{@code textComponentOnTopBool} toggles the on top mode see {@link #onTopMode(boolean)}
 * <li>{@code textComponentAnimBool} toggles the animation mode see {@link #focusAnimation(boolean)}
 * <li>{@code textComponentFieldUIID} sets the UIID of the text field to something other than {@code TextField} 
 *      which is useful for platforms such as iOS where the look of the text field is different within the text component
 * </ol>
 *
 * @author Shai Almog
 */
public class TextComponent extends Container {
    private final TextField field = new TextField() {
        @Override
        void paintHint(Graphics g) {
            if(isFocusAnimation()) {
                if(!hasFocus()) {
                    super.paintHint(g);
                }
            } else {
                super.paintHint(g);
            }
        }

        @Override
        void focusGainedInternal() {
            super.focusGainedInternal();
            if(isInitialized() && isFocusAnimation()) {
                lbl.setFocus(true);
                if(!lbl.isVisible()) {
                    final Label text = new Label(getHint(), "TextHint");
                    setHint("");
                    final Label placeholder = new Label();
                    Component.setSameSize(placeholder, field);
                    animationLayer.add(BorderLayout.NORTH, text);
                    animationLayer.add(BorderLayout.CENTER, placeholder);
                    text.setX(getX());
                    text.setY(getY());
                    text.setWidth(getWidth());
                    text.setHeight(getHeight());
                    ComponentAnimation anim = ComponentAnimation.compoundAnimation(animationLayer.createAnimateLayout(animationSpeed), text.createStyleAnimation("FloatingHint", animationSpeed));
                    getAnimationManager().addAnimation(anim, new Runnable() {
                        public void run() {
                            Component.setSameSize(field);
                            text.remove();
                            placeholder.remove();
                            lbl.setVisible(true);
                        }
                    });
                }
            }
        }

        @Override
        void focusLostInternal() {
            super.focusLostInternal();
            if(isInitialized() && isFocusAnimation()) {
                lbl.setFocus(false);
                if(getText().length() == 0 && lbl.isVisible()) {
                    final Label text = new Label(lbl.getText(), lbl.getUIID());
                    final Label placeholder = new Label();
                    Component.setSameSize(placeholder, lbl);
                    animationLayer.add(BorderLayout.NORTH, placeholder);
                    animationLayer.add(BorderLayout.CENTER, text);
                    text.setX(lbl.getX());
                    text.setY(lbl.getY());
                    text.setWidth(lbl.getWidth());
                    text.setHeight(lbl.getHeight());
                    ComponentAnimation anim = ComponentAnimation.compoundAnimation(animationLayer.createAnimateLayout(animationSpeed), text.createStyleAnimation(getHintLabel().getUIID(), animationSpeed));
                    getAnimationManager().addAnimation(anim, new Runnable() {
                        public void run() {
                            setHint(lbl.getText());
                            lbl.setVisible(false);
                            Component.setSameSize(lbl);
                            text.remove();
                            placeholder.remove();
                        }
                    });                    
                }
            }
        }
    };
    private final Button lbl = new Button("", "Label") {
            @Override
            protected boolean shouldRenderComponentSelection() {
                return true;
            }
        };
    private Container animationLayer;
    private Boolean onTopMode;
    private Boolean focusAnimation;
    private final Label errorMessage = new Label("", "ErrorLabel");
    private static int animationSpeed = 100;
    private static Boolean guiBuilderMode;
    
    /**
     * Default constructor allows us to create an arbitrary text component
     */
    public TextComponent() {
        if(guiBuilderMode == null) {
            guiBuilderMode = Display.getInstance().getProperty("GUIBuilderDesignMode", null) != null;
        }
        
        setUIID("TextComponent");
        field.setLabelForComponent(lbl);
        lbl.setFocusable(false);
        String tuid = getUIManager().getThemeConstant("textComponentFieldUIID", null);
        if(tuid != null) {
            field.setUIID(tuid);
        }
        refreshForGuiBuilder();
    }

    private void constructUI() {
        if(getComponentCount() == 0) {
            if(isOnTopMode()) {
                lbl.setUIID("FloatingHint");
                if(isFocusAnimation()) {                    
                    setLayout(new LayeredLayout());
                    Container tfContainer = BorderLayout.center(field).
                            add(BorderLayout.NORTH, lbl).
                            add(BorderLayout.SOUTH, errorMessage);
                    add(tfContainer);
                    
                    Label errorMessageFiller = new Label();
                    Component.setSameSize(errorMessageFiller, errorMessage);
                    animationLayer = BorderLayout.south(errorMessageFiller);
                    add(animationLayer);
                    if(field.getText() == null || field.getText().length() == 0) {
                        field.setHint(lbl.getText());
                        lbl.setVisible(false);
                    }  
                } else {
                    setLayout(new BorderLayout());
                    add(BorderLayout.CENTER, field);
                    add(BorderLayout.NORTH, lbl);
                    add(BorderLayout.SOUTH, errorMessage);
                }
            } else {
                setLayout(new BorderLayout());
                add(BorderLayout.CENTER, field);
                add(BorderLayout.WEST, lbl);
                add(BorderLayout.SOUTH, errorMessage);
            }
        }
    }
    
    private void refreshForGuiBuilder() {
        if(guiBuilderMode) {
            removeAll();
            field.remove();
            lbl.remove();
            errorMessage.remove();
            if(animationLayer != null) {
                animationLayer.remove();
            }
            constructUI();
        }
    }
    
    @Override
    void initComponentImpl() {
        constructUI();
        super.initComponentImpl();
    }
    
    
    /**
     * Groups together multiple text components and labels so they align properly, this is implicitly invoked 
     * by {@link com.codename1.ui.layouts.TextModeLayout} so this method is unnecessary when using that 
     * layout
     * @param cmps a list of components if it's a text component that is not in the on top mode the width of the labels 
     * will be aligned
     */
    public static void group(Component... cmps) {
        ArrayList<Component> al = new ArrayList<Component>();
        for(Component c : cmps) {
            if(c instanceof TextComponent) {
                TextComponent t = (TextComponent)c;
                if(!t.isOnTopMode()) {
                    al.add(t.lbl);
                }
            } else {
                al.add(c);
            }
        }
        Component[] cc = new Component[al.size()];
        al.toArray(cc);
        Component.setSameWidth(cc);
    }
    
    /**
     * Sets the on top mode which places the label above the text when true. It's to the left of the text otherwise 
     * (right in bidi languages). This is determined by the platform theme using the {@code textComponentOnTopBool}
     * theme constant which defaults to false
     * @param onTopMode true for the label to be above the text
     * @return this for chaining calls E.g. {@code TextComponent tc = new TextComponent().text("Text").label("Label"); }
     */
    public TextComponent onTopMode(boolean onTopMode) {
        this.onTopMode = Boolean.valueOf(onTopMode);
        refreshForGuiBuilder();
        return this;
    }
    
    /**
     * Indicates the on top mode which places the label above the text when true. It's to the left of the text otherwise 
     * (right in bidi languages). This is determined by the platform theme using the {@code textComponentOnTopBool}
     * theme constant which defaults to false
     * 
     * @return true if the text should be on top
     */
    public boolean isOnTopMode() {
        if(onTopMode != null) {
            return onTopMode.booleanValue();
        }
        return getUIManager().isThemeConstant("textComponentOnTopBool", false);
    }
    
    /**
     * The focus animation mode forces the hint and text to be identical and animates the hint to the label when
     * focus is in the text field as is common on Android. This can be customized using the theme constant
     * {@code textComponentAnimBool} which is true by default on Android. Notice that this is designed for the 
     * {@code onTopMode} and might not work if that is set to false...
     * 
     * @return true if the text should be on top
     */
    public boolean isFocusAnimation() {
        if(focusAnimation != null) {
            return focusAnimation.booleanValue();
        }
        return getUIManager().isThemeConstant("textComponentAnimBool", false);
    }

    /**
     * The focus animation mode forces the hint and text to be identical and animates the hint to the label when
     * focus is in the text field as is common on Android. This can be customized using the theme constant
     * {@code textComponentAnimBool} which is true by default on Android. Notice that this is designed for the 
     * {@code onTopMode} and might not work if that is set to false...
     * @param focusAnimation true for the label to animate into place on focus, false otherwise
     * @return this for chaining calls E.g. {@code TextComponent tc = new TextComponent().text("Text").label("Label"); }
     */
    public TextComponent focusAnimation(boolean focusAnimation) {
        this.focusAnimation = Boolean.valueOf(focusAnimation);
        refreshForGuiBuilder();
        return this;
    }
    
    /**
     * Sets the text of the field
     * @param text the text
     * @return this for chaining calls E.g. {@code TextComponent tc = new TextComponent().text("Text").label("Label"); }
     */
    public TextComponent text(String text) {
        field.setText(text);
        refreshForGuiBuilder();
        return this;
    }
    
    /**
     * Sets the text of the error label
     * @param errorMessage the text
     * @return this for chaining calls E.g. {@code TextComponent tc = new TextComponent().text("Text").label("Label"); }
     */
    public TextComponent errorMessage(String errorMessage) {
        String col = getUIManager().getThemeConstant("textComponentErrorColor", null);
        if(errorMessage == null || errorMessage.length() == 0) {
            // no need for double showing of error
            if(this.errorMessage.getText().length() == 0) {
                return this;
            }
            // clear the error mode
            this.errorMessage.setText("");
            if(col != null) {
                lbl.setUIID(lbl.getUIID());
                field.setUIID(field.getUIID());
            }
        } else {
            this.errorMessage.setText(errorMessage);
            if(col != null) {
                int val = Integer.parseInt(col, 16);
                lbl.getAllStyles().setFgColor(val);
                Border b = Border.createUnderlineBorder(2, val);
                field.getAllStyles().setBorder(b);
            }
        }
        refreshForGuiBuilder();
        return this;
    }
    
    /**
     * Sets the text of the label
     * @param text the text
     * @return this for chaining calls E.g. {@code TextComponent tc = new TextComponent().text("Text").label("Label"); }
     */
    public TextComponent label(String text) {
        lbl.setText(text);
        refreshForGuiBuilder();
        return this;
    }

    /**
     * Sets the hint of the field
     * @param hint the text of the hint
     * @return this for chaining calls E.g. {@code TextComponent tc = new TextComponent().text("Text").label("Label"); }
     */
    public TextComponent hint(String hint) {
        field.setHint(hint);
        refreshForGuiBuilder();
        return this;
    }

    /**
     * Sets the hint of the field
     * @param hint the icon for the hint
     * @return this for chaining calls E.g. {@code TextComponent tc = new TextComponent().text("Text").label("Label"); }
     */
    public TextComponent hint(Image hint) {
        field.setHintIcon(hint);
        refreshForGuiBuilder();
        return this;
    }
    
    /**
     * Sets the text field to multiline or single line
     * @param multiline true for multiline, false otherwise
     * @return this for chaining calls E.g. {@code TextComponent tc = new TextComponent().text("Text").label("Label"); }
     */
    public TextComponent multiline(boolean multiline) {
        field.setSingleLineTextArea(!multiline);
        refreshForGuiBuilder();
        return this;
    }
    
    /**
     * Sets the columns in the text field
     * @param columns the number of columns which is used for preferred size calculations
     * @return this for chaining calls E.g. {@code TextComponent tc = new TextComponent().text("Text").label("Label"); }
     */
    public TextComponent columns(int columns) {
        field.setColumns(columns);
        refreshForGuiBuilder();
        return this;
    }

    /**
     * Sets the rows in the text field
     * @param rows the number of rows which is used for preferred size calculations
     * @return this for chaining calls E.g. {@code TextComponent tc = new TextComponent().text("Text").label("Label"); }
     */
    public TextComponent rows(int rows) {
        field.setRows(rows);
        refreshForGuiBuilder();
        return this;
    }

    /**
     * Sets the constraint for text input matching the constraints from the text area class
     * @param constraint one of the constants from the {@link com.codename1.ui.TextArea} class see 
     *             {@link com.codename1.ui.TextArea#setConstraint(int)}
     * @return this for chaining calls E.g. {@code TextComponent tc = new TextComponent().text("Text").label("Label"); }
     */
    public TextComponent constraint(int constraint) {
        field.setConstraint(constraint);
        return this;
    }

    
    /**
     * Allows us to invoke setters/getters and bind listeners to the text field
     * @return the text field instance
     */
    public TextField getField() {
        return field;
    }

    /**
     * {@inheritDoc}
     */
    public String[] getPropertyNames() {
        return new String[] {"text", "label", "hint", "multiline", "columns", "rows", "constraint"};
    }

    /**
     * {@inheritDoc}
     */
    public Class[] getPropertyTypes() {
       return new Class[] {String.class, String.class, String.class, Boolean.class, Integer.class, Integer.class, Integer.class};
    }
    
    /**
     * {@inheritDoc}
     */
    public String[] getPropertyTypeNames() {
        return new String[] {"String", "String", "String", "Boolean", "Integer", "Integer", "Integer"};
    }

    /**
     * {@inheritDoc}
     */
    public Object getPropertyValue(String name) {
        if(name.equals("text")) {
            return field.getText();
        }
        if(name.equals("label")) {
            return lbl.getText();
        }
        if(name.equals("hint")) {
            return field.getHint();
        }
        if(name.equals("multiline")) {
            return Boolean.valueOf(!field.isSingleLineTextArea());
        }
        if(name.equals("columns")) {
            return field.getColumns();
        }
        if(name.equals("rows")) {
            return field.getRows();
        }
        if(name.equals("constraint")) {
            return field.getConstraint();
        }
        
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String setPropertyValue(String name, Object value) {
        if(name.equals("text")) {
            text((String)value);
            return null;
        }
        if(name.equals("label")) {
            label((String)value);
            return null;
        }
        if(name.equals("hint")) {
            hint((String)value);
            return null;
        }
        if(name.equals("multiline")) {
            field.setSingleLineTextArea(!((Boolean)value).booleanValue());
            return null;
        }
        if(name.equals("columns")) {
            field.setColumns((Integer)value);
            return null;
        }
        if(name.equals("rows")) {
            field.setRows((Integer)value);
            return null;
        }
        if(name.equals("constraint")) {
            field.setConstraint((Integer)value);
            return null;
        }
        return super.setPropertyValue(name, value);
    }
}
