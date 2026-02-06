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
import com.codename1.ui.list.ListModel;

/// A `TextComponent` version of `com.codename1.ui.AutoCompleteTextField`
///
/// This component was contributed here https://github.com/codenameone/CodenameOne/issues/2705
///
/// @author Francesco Galgani
public class AutoCompleteTextComponent extends TextComponent {
    private static final int animationSpeed = 100;
    private final AutoCompleteTextField field;
    private Container animationLayer;
    private Boolean focusAnimation;

    /// This constructor allows us to create an AutoCompleteTextComponent with the
    /// given listModel and customFilter
    ///
    /// #### Parameters
    ///
    /// - `listModel`
    ///
    /// - `customFilter`
    public AutoCompleteTextComponent(ListModel<String> listModel, final AutoCompleteFilter customFilter) {
        field = new AutoCompleteTextField(listModel) {
            @Override
            void paintHint(Graphics g) {
                if (isFocusAnimation()) {
                    if (!hasFocus()) {
                        super.paintHint(g);
                    }
                } else {
                    super.paintHint(g);
                }
            }

            @Override
            void focusGainedInternal() {
                super.focusGainedInternal();
                if (isInitialized() && isFocusAnimation()) {
                    getLabel().setFocus(true);
                    if (!getLabel().isVisible()) {
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
                            @Override
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
                if (isInitialized() && isFocusAnimation()) {
                    getLabel().setFocus(false);
                    if (AutoCompleteTextComponent.this.getText().length() == 0 && AutoCompleteTextComponent.this.getLabel().isVisible()) {
                        final Label text = new Label(getLabel().getText(), getLabel().getUIID());
                        final Label placeholder = new Label();
                        Component.setSameSize(placeholder, getLabel());
                        animationLayer.add(BorderLayout.NORTH, placeholder);
                        animationLayer.add(BorderLayout.CENTER, text);
                        text.setX(getLabel().getX());
                        text.setY(getLabel().getY());
                        text.setWidth(getLabel().getWidth());
                        text.setHeight(getLabel().getHeight());
                        String hintLabelUIID = "TextHint";
                        if (getHintLabel() != null) {
                            hintLabelUIID = getHintLabel().getUIID();
                        }
                        ComponentAnimation anim = ComponentAnimation.compoundAnimation(animationLayer.createAnimateLayout(animationSpeed), text.createStyleAnimation(hintLabelUIID, animationSpeed));
                        getAnimationManager().addAnimation(anim, new Runnable() {
                            @Override
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

            @Override
            protected boolean filter(String text) {
                return customFilter.filter(text);
            }
        };
        initInput();
    }

    /// Allows us to invoke setters/getters and bind listeners to the text field
    ///
    /// #### Returns
    ///
    /// the text field instance
    @Override
    public TextField getField() {
        return field;
    }

    @Override
    void constructUI() {
        if (getComponentCount() == 0) {
            if (isOnTopMode() && isFocusAnimation()) {
                getLabel().setUIID("FloatingHint");
                setLayout(new LayeredLayout());
                Container tfContainer = BorderLayout.center(field).
                        add(BorderLayout.NORTH, getLabel()).
                        add(BorderLayout.SOUTH,
                                LayeredLayout.encloseIn(
                                        getErrorMessage(),
                                        getDescriptionMessage()));
                add(tfContainer);

                Label errorMessageFiller = new Label();
                Component.setSameSize(errorMessageFiller, getErrorMessage());
                animationLayer = BorderLayout.south(errorMessageFiller);
                add(animationLayer);
                if (field.getText() == null || field.getText().length() == 0) {
                    field.setHint(getLabel().getText());
                    getLabel().setVisible(false);
                }
            } else {
                super.constructUI();
            }
        }
    }

    /// Returns the editor component e.g. text field picker etc.
    ///
    /// #### Returns
    ///
    /// the editor component
    @Override
    public final Component getEditor() {
        return field;
    }

    @Override
    void refreshForGuiBuilder() {
        if (guiBuilderMode) {
            if (animationLayer != null) {
                animationLayer.remove();
            }
            super.refreshForGuiBuilder();
        }
    }

    /// The focus animation mode forces the hint and text to be identical and
    /// animates the hint to the label when focus is in the text field as is
    /// common on Android. This can be customized using the theme constant
    /// `textComponentAnimBool` which is true by default on Android. Notice
    /// that this is designed for the `onTopMode` and might not work if
    /// that is set to false...
    ///
    /// #### Returns
    ///
    /// true if the text should be on top
    @Override
    public boolean isFocusAnimation() {
        if (focusAnimation != null) {
            return focusAnimation.booleanValue();
        }
        return getUIManager().isThemeConstant("textComponentAnimBool", false);
    }

    /// The focus animation mode forces the hint and text to be identical and
    /// animates the hint to the label when focus is in the text field as is
    /// common on Android. This can be customized using the theme constant
    /// `textComponentAnimBool` which is true by default on Android. Notice
    /// that this is designed for the `onTopMode` and might not work if
    /// that is set to false...
    ///
    /// #### Parameters
    ///
    /// - `focusAnimation`: @param focusAnimation true for the label to animate into place on focus,
    /// false otherwise
    ///
    /// #### Returns
    ///
    /// this for chaining calls E.g. `AutoCompleteTextComponent tc = new AutoCompleteTextComponent().text("Text").label("Label");`
    @Override
    public AutoCompleteTextComponent focusAnimation(boolean focusAnimation) {
        this.focusAnimation = Boolean.valueOf(focusAnimation);
        refreshForGuiBuilder();
        return this;
    }

    /// Sets the text of the field
    ///
    /// #### Parameters
    ///
    /// - `text`: the text
    ///
    /// #### Returns
    ///
    /// this for chaining calls E.g. `AutoCompleteTextComponent tc = new AutoCompleteTextComponent().text("Text").label("Label");`
    @Override
    public AutoCompleteTextComponent text(String text) {
        field.setText(text);
        refreshForGuiBuilder();
        return this;
    }

    /// Overridden for covariant return type {@inheritDoc}
    @Override
    public AutoCompleteTextComponent onTopMode(boolean onTopMode) {
        return (AutoCompleteTextComponent) super.onTopMode(onTopMode);
    }

    /// Overridden for covariant return type
    /// {@inheritDoc}
    @Override
    public AutoCompleteTextComponent descriptionMessage(String descriptionMessage) {
        super.descriptionMessage(descriptionMessage);
        return this;
    }

    /// Overridden for covariant return type {@inheritDoc}
    @Override
    public AutoCompleteTextComponent errorMessage(String errorMessage) {
        super.errorMessage(errorMessage);
        return this;
    }

    /// Overridden for covariant return type      * {@inheritDoc}
    /// }
    @Override
    public AutoCompleteTextComponent label(String text) {
        super.label(text);
        return this;
    }

    /// Convenience method for setting the label and hint together
    ///
    /// #### Parameters
    ///
    /// - `text`: the text and hint
    ///
    /// #### Returns
    ///
    /// this for chaining calls E.g. `AutoCompleteTextComponent tc = new AutoCompleteTextComponent().text("Text").label("Label");`
    @Override
    public AutoCompleteTextComponent labelAndHint(String text) {
        super.label(text);
        hint(text);
        return this;
    }

    /// Sets the hint of the field
    ///
    /// #### Parameters
    ///
    /// - `hint`: the text of the hint
    ///
    /// #### Returns
    ///
    /// this for chaining calls E.g. `AutoCompleteTextComponent tc = new AutoCompleteTextComponent().text("Text").label("Label");`
    @Override
    public AutoCompleteTextComponent hint(String hint) {
        field.setHint(hint);
        refreshForGuiBuilder();
        return this;
    }

    /// Sets the hint of the field
    ///
    /// #### Parameters
    ///
    /// - `hint`: the icon for the hint
    ///
    /// #### Returns
    ///
    /// this for chaining calls E.g. `AutoCompleteTextComponent tc = new AutoCompleteTextComponent().text("Text").label("Label");`
    @Override
    public AutoCompleteTextComponent hint(Image hint) {
        field.setHintIcon(hint);
        refreshForGuiBuilder();
        return this;
    }

    /// Sets the text field to multiline or single line
    ///
    /// #### Parameters
    ///
    /// - `multiline`: true for multiline, false otherwise
    ///
    /// #### Returns
    ///
    /// this for chaining calls E.g. `AutoCompleteTextComponent tc = new AutoCompleteTextComponent().text("Text").label("Label");`
    @Override
    public AutoCompleteTextComponent multiline(boolean multiline) {
        field.setSingleLineTextArea(!multiline);
        refreshForGuiBuilder();
        return this;
    }

    /// Sets the columns in the text field
    ///
    /// #### Parameters
    ///
    /// - `columns`: @param columns the number of columns which is used for preferred size
    /// calculations
    ///
    /// #### Returns
    ///
    /// this for chaining calls E.g. `AutoCompleteTextComponent tc = new AutoCompleteTextComponent().text("Text").label("Label");`
    @Override
    public AutoCompleteTextComponent columns(int columns) {
        field.setColumns(columns);
        refreshForGuiBuilder();
        return this;
    }

    /// Sets the rows in the text field
    ///
    /// #### Parameters
    ///
    /// - `rows`: @param rows the number of rows which is used for preferred size
    /// calculations
    ///
    /// #### Returns
    ///
    /// this for chaining calls E.g. `AutoCompleteTextComponent tc = new AutoCompleteTextComponent().text("Text").label("Label");`
    @Override
    public AutoCompleteTextComponent rows(int rows) {
        field.setRows(rows);
        refreshForGuiBuilder();
        return this;
    }

    /// Sets the constraint for text input matching the constraints from the text
    /// area class
    ///
    /// #### Parameters
    ///
    /// - `constraint`: @param constraint one of the constants from the
    /// `com.codename1.ui.TextArea` class see
    /// `com.codename1.ui.TextArea#setConstraint(int)`
    ///
    /// #### Returns
    ///
    /// this for chaining calls E.g. `AutoCompleteTextComponent tc = new AutoCompleteTextComponent().text("Text").label("Label");`
    @Override
    public AutoCompleteTextComponent constraint(int constraint) {
        field.setConstraint(constraint);
        return this;
    }

    /// Allows us to invoke setters/getters and bind listeners to the text field
    ///
    /// #### Returns
    ///
    /// the text field instance
    public AutoCompleteTextField getAutoCompleteField() {
        return field;
    }

    /// {@inheritDoc}
    @Override
    public String[] getPropertyNames() {
        return new String[]{"text", "label", "hint", "multiline", "columns", "rows", "constraint"};
    }

    /// {@inheritDoc}
    @Override
    public Class[] getPropertyTypes() {
        return new Class[]{String.class, String.class, String.class, Boolean.class, Integer.class, Integer.class, Integer.class};
    }

    /// {@inheritDoc}
    @Override
    public String[] getPropertyTypeNames() {
        return new String[]{"String", "String", "String", "Boolean", "Integer", "Integer", "Integer"};
    }

    /// {@inheritDoc}
    @Override
    public Object getPropertyValue(String name) {
        if ("text".equals(name)) {
            return field.getText();
        }
        if ("hint".equals(name)) {
            return field.getHint();
        }
        if ("multiline".equals(name)) {
            return Boolean.valueOf(!field.isSingleLineTextArea());
        }
        if ("columns".equals(name)) {
            return field.getColumns();
        }
        if ("rows".equals(name)) {
            return field.getRows();
        }
        if ("constraint".equals(name)) {
            return field.getConstraint();
        }

        return super.getPropertyValue(name);
    }

    /// {@inheritDoc}
    @Override
    public String setPropertyValue(String name, Object value) {
        if ("text".equals(name)) {
            text((String) value);
            return null;
        }
        if ("hint".equals(name)) {
            hint((String) value);
            return null;
        }
        if ("multiline".equals(name)) {
            field.setSingleLineTextArea(!((Boolean) value).booleanValue());
            return null;
        }
        if ("columns".equals(name)) {
            field.setColumns((Integer) value);
            return null;
        }
        if ("rows".equals(name)) {
            field.setRows((Integer) value);
            return null;
        }
        if ("constraint".equals(name)) {
            field.setConstraint((Integer) value);
            return null;
        }
        return super.setPropertyValue(name, value);
    }

    /// Returns the text in the field `com.codename1.ui.TextArea#getText()`
    ///
    /// #### Returns
    ///
    /// the text
    @Override
    public String getText() {
        return field.getText();
    }

    /// Allows filtering the entries in the auto complete
    public interface AutoCompleteFilter {
        /// Callback to filter based on this given text
        ///
        /// #### Parameters
        ///
        /// - `text`: the text for filtering
        ///
        /// #### Returns
        ///
        /// true if the entry should be filtered in
        boolean filter(String text);
    }
}
