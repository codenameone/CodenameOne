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
 * <p>
 * The following code demonstrates a simple set of inputs and validation as it appears in iOS, Android and with 
 * validation errors
 * </p>
 * <script src="https://gist.github.com/codenameone/5a28c7944aeab7d8ae6b26dc81690238.js"></script>
 * <img src="https://www.codenameone.com/img/blog/pixel-perfect-text-field-picker-ios.png" alt="Running on iOS" />
 * <img src="https://www.codenameone.com/img/blog/pixel-perfect-text-field-picker-android.png" alt="Running on Android" />
 * <img src="https://www.codenameone.com/img/blog/pixel-perfect-text-field-error-handling-blank.png" alt="Android validation errors" />
 *
 * @author Shai Almog
 */
public class TextComponent extends InputComponent {
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
                getLabel().setFocus(true);
                if(!getLabel().isVisible()) {
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
                            getLabel().setVisible(true);
                        }
                    });
                }
            }
        }

        @Override
        void focusLostInternal() {
            super.focusLostInternal();
            if(isInitialized() && isFocusAnimation()) {
                getLabel().setFocus(false);
                if(getText().length() == 0 && getLabel().isVisible()) {
                    final Label text = new Label(getLabel().getText(), getLabel().getUIID());
                    final Label placeholder = new Label();
                    Component.setSameSize(placeholder, getLabel());
                    animationLayer.add(BorderLayout.NORTH, placeholder);
                    animationLayer.add(BorderLayout.CENTER, text);
                    text.setX(getLabel().getX());
                    text.setY(getLabel().getY());
                    text.setWidth(getLabel().getWidth());
                    text.setHeight(getLabel().getHeight());
                    ComponentAnimation anim = ComponentAnimation.compoundAnimation(animationLayer.createAnimateLayout(animationSpeed), text.createStyleAnimation(getHintLabel().getUIID(), animationSpeed));
                    getAnimationManager().addAnimation(anim, new Runnable() {
                        public void run() {
                            setHint(getLabel().getText());
                            getLabel().setVisible(false);
                            Component.setSameSize(getLabel());
                            text.remove();
                            placeholder.remove();
                        }
                    });                    
                }
            }
        }
    };
    private Container animationLayer;
    private Boolean focusAnimation;
    private static int animationSpeed = 100;
    
    /**
     * Default constructor allows us to create an arbitrary text component
     */
    public TextComponent() {
        initInput();
    }

    void constructUI() {
        if(getComponentCount() == 0) {
            if(isOnTopMode() && isFocusAnimation()) {
                getLabel().setUIID("FloatingHint");
                setLayout(new LayeredLayout());
                Container tfContainer = BorderLayout.center(field).
                        add(BorderLayout.NORTH, getLabel()).
                        add(BorderLayout.SOUTH, getErrorMessage());
                add(tfContainer);

                Label errorMessageFiller = new Label();
                Component.setSameSize(errorMessageFiller, getErrorMessage());
                animationLayer = BorderLayout.south(errorMessageFiller);
                add(animationLayer);
                if(field.getText() == null || field.getText().length() == 0) {
                    field.setHint(getLabel().getText());
                    getLabel().setVisible(false);
                }  
            } else {
                super.constructUI();
            }
        }
    }
    
    /**
     * Returns the editor component e.g. text field picker etc.
     * @return the editor component
     */
    public Component getEditor() {
        return field;
    }
    
    void refreshForGuiBuilder() {
        if(guiBuilderMode) {
            if(animationLayer != null) {
                animationLayer.remove();
            }
            super.refreshForGuiBuilder();
        }
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
     * Overridden for covariant return type
     * {@inheritDoc}
     */
    public TextComponent onTopMode(boolean onTopMode) {
        return (TextComponent)super.onTopMode(onTopMode);
    }

    /**
     * Overridden for covariant return type
     * {@inheritDoc}
     */
    public TextComponent errorMessage(String errorMessage) {
        super.errorMessage(errorMessage);
        return this;
    }
    
    /**
     * Overridden for covariant return type
     * {@inheritDoc}
 }
     */
    public TextComponent label(String text) {
        super.label(text);
        return this;
    }

    /**
     * Convenience method for setting the label and hint together
     * 
     * @param text the text and hint
     * @return this for chaining calls E.g. {@code TextComponent tc = new TextComponent().text("Text").label("Label");
 }
     */
    public TextComponent labelAndHint(String text) {
        super.label(text);
        hint(text);
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
        
        return super.getPropertyValue(name);
    }

    /**
     * {@inheritDoc}
     */
    public String setPropertyValue(String name, Object value) {
        if(name.equals("text")) {
            text((String)value);
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
