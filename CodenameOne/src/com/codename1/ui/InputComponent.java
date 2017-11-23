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

import com.codename1.ui.layouts.BorderLayout;
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
 * <li>{@code textComponentOnTopBool} toggles the on top mode see {@link #onTopMode(boolean)}
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
public abstract class InputComponent extends Container {
    private Boolean onTopMode;
    private final Button lbl = new Button("", "Label") {
            @Override
            protected boolean shouldRenderComponentSelection() {
                return true;
            }
        };
    private final Label errorMessage = new Label("", "ErrorLabel");

    static Boolean guiBuilderMode;

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
        setUIID("TextComponent");
        getEditor().setLabelForComponent(lbl);
        lbl.setFocusable(false);
        String tuid = getUIManager().getThemeConstant("textComponentFieldUIID", null);
        if(tuid != null) {
            getEditor().setUIID(tuid);
        }
        refreshForGuiBuilder();
    }
    
    /**
     * Returns the internal label implementation
     * @return the label
     */
    Label getLabel() {
        return lbl;
    }
    
    /**
     * Returns the internal error message implementation
     * @return the label
     */
    Label getErrorMessage() {
        return errorMessage;
    }
    
    void constructUI() {
        if(getComponentCount() == 0) {
            if(isOnTopMode()) {
                lbl.setUIID("FloatingHint");
                setLayout(new BorderLayout());
                add(BorderLayout.CENTER, getEditor());
                add(BorderLayout.NORTH, lbl);
                add(BorderLayout.SOUTH, errorMessage);
            } else {
                setLayout(new BorderLayout());
                add(BorderLayout.CENTER, getEditor());
                add(BorderLayout.WEST, lbl);
                add(BorderLayout.SOUTH, errorMessage);
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
            lbl.remove();
            errorMessage.remove();
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
        if(errorMessage == null || errorMessage.length() == 0) {
            // no need for double showing of error
            if(this.errorMessage.getText().length() == 0) {
                return this;
            }
            // clear the error mode
            this.errorMessage.setText("");
            if(col != null) {
                lbl.setUIID(lbl.getUIID());
                getEditor().setUIID(getEditor().getUIID());
            }
        } else {
            this.errorMessage.setText(errorMessage);
            if(col != null) {
                int val = Integer.parseInt(col, 16);
                lbl.getAllStyles().setFgColor(val);
                Border b = Border.createUnderlineBorder(2, val);
                getEditor().getAllStyles().setBorder(b);
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
    public InputComponent label(String text) {
        lbl.setText(text);
        refreshForGuiBuilder();
        return this;
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
}
