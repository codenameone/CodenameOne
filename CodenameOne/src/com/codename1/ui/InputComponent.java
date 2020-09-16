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

import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.plaf.Border;
import java.util.ArrayList;

/**
 * <p>A base class for {@link com.codename1.ui.TextComponent}, {@link com.codename1.ui.PickerComponent} 
 * and potentially other components that wish to accept input in a dynamic way that matches iOS and 
 * Android native input guidelines.</p>
 * 
 * <p>
 * It is highly recommended to use input components in the context of a 
 * {@link com.codename1.ui.layouts.TextModeLayout}. This allows the layout to implicitly adapt to the on-top 
 * mode and use a box layout Y mode for iOS and other platforms.
 * </p>
 * <p>
 * This class supports several theme constants:
 * </p>
 * <ol>
 * <li>{@code textComponentErrorColor} a hex RGB color which defaults to null in which case this has no effect. 
 *      When defined this will change the color of the border and label to the given color to match the material design
 *      styling.
 * <li>{@code textComponentErrorLineBorderBool} when set to {@code false}, this will prevent the text component from
 * applying an underline border when there is a validation error. Defaults to {@code true}.
 * <li>{@code textComponentOnTopBool} toggles the on top mode see {@link #onTopMode(boolean)}
 * <li>{@code textComponentFieldUIID} sets the UIID of the text field to something other than {@code TextField} 
 *      which is useful for platforms such as iOS where the look of the text field is different within the text component
 * </ol>
 * <li>{@code inputComponentErrorMultilineBool} sets the error label to multiline when activated
 * </li>
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
public abstract class InputComponent extends Container {
    private static boolean multiLineErrorMessage;
    private Boolean onTopMode;
    private final Button lbl = new Button("", "Label") {
            @Override
            protected boolean shouldRenderComponentSelection() {
                return true;
            }
        };
    private TextHolder errorMessageImpl = createErrorLabel();
    private final Label descriptionMessage = new Label("", "DescriptionLabel");

    static Boolean guiBuilderMode;
    
    Button action;
    private boolean actionAsButton;
    
    /**
     * Protected constructor for subclasses to override
     */
    protected InputComponent() {
        if(guiBuilderMode == null) {
            guiBuilderMode = Display.getInstance().getProperty("GUIBuilderDesignMode", null) != null;
        }
    }

    /**
     * This method must be invoked by the constructor of the subclasses to initialize the UI
     */
    protected void initInput() {
        // this can happen for base class constructors
        if(getEditor() != null) {
            setUIID("TextComponent");
            getEditor().setLabelForComponent(lbl);
            lbl.setFocusable(false);
            String tuid = getUIManager().getThemeConstant("textComponentFieldUIID", null);
            if(tuid != null) {
                getEditor().setUIID(tuid);
            }
            refreshForGuiBuilder();
        }
    }
    
    /**
     * Returns the internal label implementation
     * @return the label
     */
    Label getLabel() {
        return lbl;
    }

    /**
     * Can be overriden by subclasses to support custom error label components
     * @return Component instance such as JLabel, TextArea etc. usually with the 
     * {@code ErrorLabel} UIID
     */
    protected TextHolder createErrorLabel() {
        if(multiLineErrorMessage && isOnTopMode()) {
            TextArea errorLabel = new TextArea() {
                @Override
                protected Dimension calcPreferredSize() {
                    if(getText() == null || getText().length() == 0) {
                        return new Dimension();
                    }
                    return super.calcPreferredSize(); 
                }
                
            };
            errorLabel.setRows(1);
            errorLabel.setActAsLabel(true);
            errorLabel.setGrowByContent(true);
            errorLabel.setFocusable(false);
            errorLabel.setEditable(false);
            errorLabel.setUIID("ErrorLabel");
            return errorLabel;
        }
        return new Label("", "ErrorLabel");
    }
    
    /**
     * Returns the internal error message implementation
     * @return the label
     */
    Component getErrorMessage() {
        return (Component)errorMessageImpl;
    }

    /**
     * Returns the internal description message implementation
     * @return the label
     */
    Label getDescriptionMessage() {
        return descriptionMessage;
    }

    // varags calls are significantly slower in java
    private static int max(int a, int b, int c) {
        return Math.max(Math.max(a, b), c);
    }
    
    private static int max(int a, int b, int c, int d) {
        return Math.max(Math.max(Math.max(a, b), c), d);
    }

    @Override
    protected Dimension calcPreferredSize() {
        
        if(getComponentCount() == 0) {
            if(isOnTopMode()) {
                lbl.setUIID("FloatingHint");
                int w = max(getEditor().getOuterPreferredW(), lbl.getOuterPreferredW(), getErrorMessage().getOuterPreferredW(), descriptionMessage.getOuterPreferredW());
                int h = getEditor().getOuterPreferredH() + lbl.getOuterPreferredH() + 
                    Math.max(getErrorMessage().getOuterPreferredH(), descriptionMessage.getOuterPreferredH());
                return new Dimension(w + getStyle().getHorizontalPadding(), 
                         h + getStyle().getVerticalPadding()
                );
            } else {
                return new Dimension(
                        Math.max(getEditor().getOuterPreferredW() + lbl.getOuterPreferredW(), getErrorMessage().getOuterPreferredW()) + getStyle().getHorizontalPadding(),
                        getErrorMessage().getOuterPreferredH() + Math.max(getEditor().getOuterPreferredH(), lbl.getOuterPreferredH()) + getStyle().getVerticalPadding()
                );
            }
        }
        return super.calcPreferredSize();
    }
    
    private void addEditorAction() {
        if(action != null) {
            if(actionAsButton) {
                add(BorderLayout.CENTER, BorderLayout.centerEastWest(
                        getEditor(),
                        action, null));
            } else {
                add(BorderLayout.CENTER, LayeredLayout.encloseIn(
                    getEditor(),
                    FlowLayout.encloseRightMiddle(action)
                ));
            }
        } else {
            add(BorderLayout.CENTER, getEditor());
        }
    }
    
    void constructUI() {
        if(getComponentCount() == 0) {
            if(isOnTopMode()) {
                lbl.setUIID("FloatingHint");
                setLayout(new BorderLayout());
                add(BorderLayout.NORTH, lbl);
                addEditorAction();
                add(BorderLayout.SOUTH, 
                    LayeredLayout.encloseIn(getErrorMessage(), descriptionMessage));
            } else {
                setLayout(new BorderLayout());
                addEditorAction();
                add(BorderLayout.WEST, lbl);
                add(BorderLayout.SOUTH, getErrorMessage());
            }
        }
    }
    
    /**
     * Returns the editor component e.g. text field picker etc.
     * @return the editor component
     */
    public abstract Component getEditor();
        
    void refreshForGuiBuilder() {
        if(guiBuilderMode) {
            removeAll();
            getEditor().remove();
            if(action != null) {
                action.remove();
            }
            lbl.remove();
            descriptionMessage.remove();
            getErrorMessage().remove();
            constructUI();
        }
    }
    
    
    /**
     * Sets the on top mode which places the label above the text when true. It's to the left of the text otherwise 
     * (right in bidi languages). This is determined by the platform theme using the {@code textComponentOnTopBool}
     * theme constant which defaults to false
     * @param onTopMode true for the label to be above the text
     * @return this for chaining calls E.g. {@code TextComponent tc = new TextComponent().text("Text").label("Label"); }
     */
    public InputComponent onTopMode(boolean onTopMode) {
        this.onTopMode = Boolean.valueOf(onTopMode);
        refreshForGuiBuilder();
        return this;
    }
    
    @Override
    void initComponentImpl() {
        constructUI();
        super.initComponentImpl();
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
     * Groups together multiple text components and labels so they align properly, this is implicitly invoked 
     * by {@link com.codename1.ui.layouts.TextModeLayout} so this method is unnecessary when using that 
     * layout
     * @param cmps a list of components if it's a text component that is not in the on top mode the width of the labels 
     * will be aligned
     */
    public static void group(Component... cmps) {
        ArrayList<Component> al = new ArrayList<Component>();
        for(Component c : cmps) {
            if(c instanceof InputComponent) {
                InputComponent t = (InputComponent)c;
                if(!t.isOnTopMode()) {
                    al.add(t.lbl);
                    t.lbl.setPreferredSize(null);
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
     * Sets the text of the error label
     * @param errorMessage the text
     * @return this for chaining calls E.g. {@code TextComponent tc = new TextComponent().text("Text").label("Label"); }
     */
    public InputComponent errorMessage(String errorMessage) {
        String col = getUIManager().getThemeConstant("textComponentErrorColor", null);
        boolean line = getUIManager().isThemeConstant("textComponentErrorLineBorderBool", true);
        if(errorMessage == null || errorMessage.length() == 0) {
            // no need for double showing of error
            if(this.errorMessageImpl.getText().length() == 0) {
                return this;
            }
            // clear the error mode
            this.errorMessageImpl.setText("");
            if(col != null) {
                lbl.setUIID(lbl.getUIID());
                getEditor().setUIID(getEditor().getUIID());
            }
            descriptionMessage.setVisible(true);
        } else {
            descriptionMessage.setVisible(false);
            this.errorMessageImpl.setText(errorMessage);
            if(col != null) {
                int val = Integer.parseInt(col, 16);
                lbl.getAllStyles().setFgColor(val);

                // only show the line border error if the component is designed to allow it
                if (line) {
                    Border b = Border.createUnderlineBorder(2, val);
                    getEditor().getAllStyles().setBorder(b);
                }
            }
        }
        refreshForGuiBuilder();
        return this;
    }

    /**
     * Sets the text of the description label which currently only applies in the onTop mode. 
     * This text occupies the same space as the error message and thus hides 
     * when there's an error
     * @param descriptionMessage the text
     * @return this for chaining calls E.g. {@code TextComponent tc = new TextComponent().text("Text").label("Label"); }
     */
    public InputComponent descriptionMessage(String descriptionMessage) {
        if(descriptionMessage == null || descriptionMessage.length() == 0) {
            if(this.descriptionMessage.getText().length() == 0) {
                return this;
            }
            // clear the error mode
            this.descriptionMessage.setText("");
        } else {
            this.descriptionMessage.setText(descriptionMessage);
        }
        refreshForGuiBuilder();
        return this;
    }

    /**
     * Sets the text of the label
     * @param text the text
     * @return this for chaining calls E.g. {@code TextComponent tc = new TextComponent().text("Text").label("Label"); }
     */
    public InputComponent label(String text) {
        lbl.setText(text);
        refreshForGuiBuilder();
        return this;
    }

    private void initAction() {
        if(action == null) {
            action = new Button("", "InputComponentAction");
        }
    }
    
    /**
     * Sets the UIID for the action button
     * @param uiid a custom UIID for the action
     * @return this for chaining calls E.g. {@code TextComponent tc = new TextComponent().text("Text").label("Label"); }
     */
    public InputComponent actionUIID(String uiid) {
        initAction();
        action.setUIID(uiid);
        return this;
    }
    
    /**
     * UIID for the action button
     * @return the UIID
     */
    public String getActionUIID() {
        initAction();
        return action.getUIID();
    }
    
    /**
     * Indicates the action should behave as a button next to the component
     * and not layered on top of the text component. This is useful for UI
     * in the style of a browse button next to a text field.
     * @param asButton true so the action will act like a button
     * @return this for chaining calls E.g. {@code TextComponent tc = new TextComponent().text("Text").label("Label"); }
     */
    public InputComponent actionAsButton(boolean asButton) {
        initAction();
        this.actionAsButton = asButton;
        return this;
    }
    
    /**
     * Indicates the action should behave as a button next to the component
     * and not layered on top of the text component. This is useful for UI
     * in the style of a browse button next to a text field.
     * @return true if the action acts as a button
     */
    public boolean isActionAsButton() {
        return actionAsButton;
    }

    /**
     * Provides the text of the action button
     * @param text the text that should appear on the action button
     * @return this for chaining calls E.g. {@code TextComponent tc = new TextComponent().text("Text").label("Label"); }
     */
    public InputComponent actionText(String text) {
        initAction();
        action.setText(text);
        return this;
    }
    
    /**
     * Provides the text of the action button
     * 
     * @return the text of the action
     */
    public String getActionText() {
        initAction();
        return action.getText();
    }
    
    /**
     * Sets the icon for the action button
     * @param icon the icon constant from {@link com.codename1.ui.FontImage}
     * @return this for chaining calls E.g. {@code TextComponent tc = new TextComponent().text("Text").label("Label"); }
     */
    public InputComponent action(char icon) {
        initAction();
        action.setMaterialIcon(icon);
        refreshForGuiBuilder();
        return this;
    }

    /**
     * Binds an event for the action button
     * @param c action listener callback 
     * @return this for chaining calls E.g. {@code TextComponent tc = new TextComponent().text("Text").label("Label"); }
     */
    public InputComponent actionClick(ActionListener c) {
        initAction();
        action.addActionListener(c);
        refreshForGuiBuilder();
        return this;
    }

    /**
     * Returns the button underlying the action button that is placed on 
     * the right of the field on top of it
     * @return a button for manual customization
     */
    public Button getAction() {
        initAction();
        return action;
    }
    
    /**
     * {@inheritDoc}
     */
    public Object getPropertyValue(String name) {
        if(name.equals("label")) {
            return lbl.getText();
        }
        return null;
    }
    
    /**
     * {@inheritDoc}
     */
    public String setPropertyValue(String name, Object value) {
        if(name.equals("label")) {
            label((String)value);
            return null;
        }
        return super.setPropertyValue(name, value);
    }

    /**
     * True if error messages should be multiline by default. This can be
     * set via the theme constant {@code inputComponentErrorMultilineBool}
     * 
     * @return the multiLineErrorMessage
     */
    public static boolean isMultiLineErrorMessage() {
        return multiLineErrorMessage;
    }

    /**
     * True if error messages should be multiline by default. This can be
     * set via the theme constant {@code inputComponentErrorMultilineBool}
     * 
     * @param aMultiLineErrorMessage the multiLineErrorMessage to set
     */
    public static void setMultiLineErrorMessage(
            boolean aMultiLineErrorMessage) {
        multiLineErrorMessage = aMultiLineErrorMessage;
    }
}
