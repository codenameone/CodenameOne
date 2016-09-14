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
package com.codename1.ui.validation;

import com.codename1.components.InteractionDialog;
import com.codename1.ui.Button;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.List;
import com.codename1.ui.Painter;
import com.codename1.ui.RadioButton;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.events.FocusListener;
import com.codename1.ui.events.ScrollListener;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.Style;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Binds validation constraints to form elements, when validation fails it can be highlighted directly on
 * the component via an emblem or change of the UIID (to original UIID name + "Invalid" e.g. "TextFieldInvalid").
 * Validators just run thru a set of Constraint objects to decide if validation succeeded or failed.
 *
 * @author Shai Almog
 */
public class Validator {    
    private static final String VALID_MARKER = "cn1$$VALID_MARKER";
    
    private InteractionDialog message = new InteractionDialog();
    
    /**
     * Error message UIID defaults to DialogBody. Allows customizing the look of the message
     */
    private String errorMessageUIID = "DialogBody";
    
    /**
     * Indicates the default mode in which validation failures are expressed
     * @return the defaultValidationFailureHighlightMode
     */
    public static HighlightMode getDefaultValidationFailureHighlightMode() {
        return defaultValidationFailureHighlightMode;
    }

    /**
     * Indicates the default mode in which validation failures are expressed
     * @param aDefaultValidationFailureHighlightMode the defaultValidationFailureHighlightMode to set
     */
    public static void setDefaultValidationFailureHighlightMode(HighlightMode aDefaultValidationFailureHighlightMode) {
        defaultValidationFailureHighlightMode = aDefaultValidationFailureHighlightMode;
    }

    /**
     * The emblem that will be drawn on top of the component to indicate the validation failure
     * @return the defaultValidationFailedEmblem
     */
    public static Image getDefaultValidationFailedEmblem() {
        return defaultValidationFailedEmblem;
    }

    /**
     * The emblem that will be drawn on top of the component to indicate the validation failure
     * @param aDefaultValidationFailedEmblem the defaultValidationFailedEmblem to set
     */
    public static void setDefaultValidationFailedEmblem(Image aDefaultValidationFailedEmblem) {
        defaultValidationFailedEmblem = aDefaultValidationFailedEmblem;
    }

    /**
     * The position of the validation emblem on the component as X/Y values between 0 and 1 where 
     * 0 indicates the start of the component and 1 indicates its end on the given axis.
     * @return the defaultValidationEmblemPositionX
     */
    public static float getDefaultValidationEmblemPositionX() {
        return defaultValidationEmblemPositionX;
    }

    /**
     * The position of the validation emblem on the component as X/Y values between 0 and 1 where 
     * 0 indicates the start of the component and 1 indicates its end on the given axis.
     * @param aDefaultValidationEmblemPositionX the defaultValidationEmblemPositionX to set
     */
    public static void setDefaultValidationEmblemPositionX(float aDefaultValidationEmblemPositionX) {
        defaultValidationEmblemPositionX = aDefaultValidationEmblemPositionX;
    }

    /**
     * The position of the validation emblem on the component as X/Y values between 0 and 1 where 
     * 0 indicates the start of the component and 1 indicates its end on the given axis.
     * @return the defaultValidationEmblemPositionY
     */
    public static float getDefaultValidationEmblemPositionY() {
        return defaultValidationEmblemPositionY;
    }

    /**
     * The position of the validation emblem on the component as X/Y values between 0 and 1 where 
     * 0 indicates the start of the component and 1 indicates its end on the given axis.
     * @param aDefaultValidationEmblemPositionY the defaultValidationEmblemPositionY to set
     */
    public static void setDefaultValidationEmblemPositionY(float aDefaultValidationEmblemPositionY) {
        defaultValidationEmblemPositionY = aDefaultValidationEmblemPositionY;
    }

    /**
     * Indicates whether validation should occur on every key press (data change listener) or
     * action performed (editing completion)
     * @return the validateOnEveryKey
     */
    public static boolean isValidateOnEveryKey() {
        return validateOnEveryKey;
    }

    /**
     * Indicates whether validation should occur on every key press (data change listener) or
     * action performed (editing completion)
     * @param aValidateOnEveryKey the validateOnEveryKey to set
     */
    public static void setValidateOnEveryKey(boolean aValidateOnEveryKey) {
        validateOnEveryKey = aValidateOnEveryKey;
    }

    /**
     * Indicates the default mode in which validation failures are expressed
     * @return the validationFailureHighlightMode
     */
    public HighlightMode getValidationFailureHighlightMode() {
        return validationFailureHighlightMode;
    }

    /**
     * Indicates the default mode in which validation failures are expressed
     * @param validationFailureHighlightMode the validationFailureHighlightMode to set
     */
    public void setValidationFailureHighlightMode(HighlightMode validationFailureHighlightMode) {
        this.validationFailureHighlightMode = validationFailureHighlightMode;
    }

    /**
     * The emblem that will be drawn on top of the component to indicate the validation failure
     * @return the validationFailedEmblem
     */
    public Image getValidationFailedEmblem() {
        return validationFailedEmblem;
    }

    /**
     * The emblem that will be drawn on top of the component to indicate the validation failure
     * @param validationFailedEmblem the validationFailedEmblem to set
     */
    public void setValidationFailedEmblem(Image validationFailedEmblem) {
        this.validationFailedEmblem = validationFailedEmblem;
    }

    /**
     * The position of the validation emblem on the component as X/Y values between 0 and 1 where 
     * 0 indicates the start of the component and 1 indicates its end on the given axis.
     * @return the validationEmblemPositionX
     */
    public float getValidationEmblemPositionX() {
        return validationEmblemPositionX;
    }

    /**
     * The position of the validation emblem on the component as X/Y values between 0 and 1 where 
     * 0 indicates the start of the component and 1 indicates its end on the given axis.
     * @param validationEmblemPositionX the validationEmblemPositionX to set
     */
    public void setValidationEmblemPositionX(float validationEmblemPositionX) {
        this.validationEmblemPositionX = validationEmblemPositionX;
    }

    /**
     * The position of the validation emblem on the component as X/Y values between 0 and 1 where 
     * 0 indicates the start of the component and 1 indicates its end on the given axis.
     * @return the validationEmblemPositionY
     */
    public float getValidationEmblemPositionY() {
        return validationEmblemPositionY;
    }

    /**
     * The position of the validation emblem on the component as X/Y values between 0 and 1 where 
     * 0 indicates the start of the component and 1 indicates its end on the given axis.
     * @param validationEmblemPositionY the validationEmblemPositionY to set
     */
    public void setValidationEmblemPositionY(float validationEmblemPositionY) {
        this.validationEmblemPositionY = validationEmblemPositionY;
    }

    /**
     * Indicates whether an error message should be shown for the focused component
     * @return true if the error message should be displayed
     */
    public boolean isShowErrorMessageForFocusedComponent() {
        return showErrorMessageForFocusedComponent;
    }

    /**
     * Indicates whether an error message should be shown for the focused component
     * 
     * @param showErrorMessageForFocusedComponent true to show the error message
     */
    public void setShowErrorMessageForFocusedComponent(boolean showErrorMessageForFocusedComponent) {
        this.showErrorMessageForFocusedComponent = showErrorMessageForFocusedComponent;
    }

    /**
     * Error message UIID defaults to DialogBody. Allows customizing the look of the message
     * @return the errorMessageUIID
     */
    public String getErrorMessageUIID() {
        return errorMessageUIID;
    }

    /**
     * Error message UIID defaults to DialogBody. Allows customizing the look of the message
     * @param errorMessageUIID the errorMessageUIID to set
     */
    public void setErrorMessageUIID(String errorMessageUIID) {
        this.errorMessageUIID = errorMessageUIID;
    }
    /**
     * Indicates the validation failure modes
     */
    public static enum HighlightMode {
        UIID,
        EMBLEM,
        UIID_AND_EMBLEM,
        NONE
    }

    /**
     * Indicates the default mode in which validation failures are expressed
     */
    private static HighlightMode defaultValidationFailureHighlightMode = HighlightMode.UIID;

    /**
     * Indicates the mode in which validation failures are expressed
     */
    private HighlightMode validationFailureHighlightMode = defaultValidationFailureHighlightMode;
    
    /**
     * The emblem that will be drawn on top of the component to indicate the validation failure
     */
    private static Image defaultValidationFailedEmblem = null;

    /**
     * The emblem that will be drawn on top of the component to indicate the validation failure
     */
    private Image validationFailedEmblem = defaultValidationFailedEmblem;

    
    /**
     * The position of the validation emblem on the component as X/Y values between 0 and 1 where 
     * 0 indicates the start of the component and 1 indicates its end on the given axis.
     */
    private static float defaultValidationEmblemPositionX = 1;

    /**
     * The position of the validation emblem on the component as X/Y values between 0 and 1 where 
     * 0 indicates the start of the component and 1 indicates its end on the given axis.
     */
    private static float defaultValidationEmblemPositionY = 0.5f;
    
    
    /**
     * The position of the validation emblem on the component as X/Y values between 0 and 1 where 
     * 0 indicates the start of the component and 1 indicates its end on the given axis.
     */
    private float validationEmblemPositionX = defaultValidationEmblemPositionX;

    /**
     * The position of the validation emblem on the component as X/Y values between 0 and 1 where 
     * 0 indicates the start of the component and 1 indicates its end on the given axis.
     */
    private float validationEmblemPositionY = defaultValidationEmblemPositionY;
    
    private HashMap<Component, Constraint> constraintList = new HashMap<Component, Constraint>();

    private ArrayList<Component> submitButtons = new ArrayList<Component>();
    
    /**
     * Indicates whether validation should occur on every key press (data change listener) or
     * action performed (editing completion)
     */
    private static boolean validateOnEveryKey = false;
    
    /**
     * Indicates whether an error message should be shown for the focused component
     */
    private boolean showErrorMessageForFocusedComponent;
    
    /**
     * Places a constraint on the validator, returns this object so constraint additions can be chained. 
     * Notice that only one constraint 
     * @param cmp the component to validate
     * @param c the constraint or constraints
     * @return this object so we can write code like v.addConstraint(cmp1, cons).addConstraint(cmp2, otherConstraint);
     */
    public Validator addConstraint(Component cmp, Constraint... c) {
        if(c.length == 1) {
            constraintList.put(cmp, c[0]);
        } else {
            constraintList.put(cmp, new GroupConstraint(c));
        }
        bindDataListener(cmp);
        boolean isV = isValid();
        for(Component btn : submitButtons) {
            btn.setEnabled(isV);
        }
        return this;
    }
    
    /**
     * Submit buttons (or any other component type) can be disabled until all components contain a valid value.
     * Notice that this method should be invoked after all the constraints are added so the initial state of the buttons
     * will be correct.
     * 
     * @param cmp set of buttons or components to disable until everything is valid
     * @return the validator instance so this method can be chained
     */
    public Validator addSubmitButtons(Component... cmp) {
        boolean isV = isValid();
        for(Component c : cmp) {
            submitButtons.add(c);
            c.setEnabled(isV);
        }
        return this;
    }
    
    /**
     * Returns the value of the given component, this can be overriden to add support for custom built components
     * 
     * @param cmp the component
     * @return  the object value
     */
    protected Object getComponentValue(Component cmp) {
        if(cmp instanceof TextArea) {
            return ((TextArea)cmp).getText();
        }
        if(cmp instanceof RadioButton || cmp instanceof CheckBox) {
            if(((Button)cmp).isSelected()) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        if(cmp instanceof Label) {
            return ((Label)cmp).getText();
        }
        if(cmp instanceof List) {
            return ((List)cmp).getSelectedItem();
        }
        return null;
    }
    
    /**
     * Binds an event listener to the given component
     * @param cmp the component to bind the data listener to
     * @deprecated this method was exposed by accident, constraint implicitly calls it and you don't need to 
     * call it directly. It will be made protected in a future update to Codename One!
     */
    public void bindDataListener(Component cmp) {
        if(showErrorMessageForFocusedComponent) {
            cmp.addFocusListener(new FocusListener() {
                public void focusGained(Component cmp) {
                    // special case. Before the form is showing don't show error dialogs
                    Form p = cmp.getComponentForm();
                    if(p != Display.getInstance().getCurrent()) {
                        return;
                    }
                    if(message != null) {
                        message.dispose();
                    }
                    if(!isValid(cmp)) {
                        String err = getErrorMessage(cmp);
                        if(err != null && err.length() > 0) {
                            message = new InteractionDialog(err);
                            message.getTitleComponent().setUIID(errorMessageUIID);
                            message.setAnimateShow(false);
                            if(validationFailureHighlightMode == HighlightMode.EMBLEM || validationFailureHighlightMode == HighlightMode.UIID_AND_EMBLEM) {
                                int xpos = cmp.getAbsoluteX();
                                int ypos = cmp.getAbsoluteY();
                                Component scr = cmp.getScrollable();
                                if(scr != null) {
                                    xpos -= scr.getScrollX();
                                    ypos -= scr.getScrollY();
                                    scr.addScrollListener(new ScrollListener() {
                                        public void scrollChanged(int scrollX, int scrollY, int oldscrollX, int oldscrollY) {
                                            if (message != null) {
                                                message.dispose();
                                            }
                                            message = null;
                                        }
                                    });
                                }
                                float width = cmp.getWidth();
                                float height = cmp.getHeight();
                                xpos += Math.round(width * validationEmblemPositionX);
                                ypos += Math.round(height * validationEmblemPositionY);
                                message.showPopupDialog(new Rectangle(xpos, ypos, validationFailedEmblem.getWidth(), 
                                        validationFailedEmblem.getHeight()));
                            } else {
                                message.showPopupDialog(cmp);
                            }
                        }
                    }
                }

                public void focusLost(Component cmp) {
                }
            });
        }
        if(validateOnEveryKey) {
            if(cmp instanceof TextField) {
                ((TextField)cmp).addDataChangedListener(new ComponentListener(cmp));
                return;
            }
        }
        if(cmp instanceof TextArea) {
            ((TextArea)cmp).addActionListener(new ComponentListener(cmp));
            return;
        }
        if(cmp instanceof List) {
            ((List)cmp).addActionListener(new ComponentListener(cmp));
            return;
        }
        if(cmp instanceof CheckBox || cmp instanceof RadioButton) {
            ((Button)cmp).addActionListener(new ComponentListener(cmp));
            return;
        } 
    }

    /**
     * Returns true if all the constraints are currently valid
     * @return true if the entire validator is valid
     */
    public boolean isValid() {
        for(Component c : constraintList.keySet()) {
            if(!isValid(c)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Validates and highlights an individual component
     * @param cmp the component to validate
     */
    protected void validate(Component cmp) {
        Object val = getComponentValue(cmp);
        setValid(cmp, constraintList.get(cmp).isValid(val));
    }
    
    boolean isValid(Component cmp) {
        Boolean b = (Boolean)cmp.getClientProperty(VALID_MARKER);
        if(b != null) {
            return b.booleanValue();
        }
        Object val = getComponentValue(cmp);
        return constraintList.get(cmp).isValid(val);
    }
    
    /**
     * Returns the validation error message for the given component or null if no such message exists
     * @param cmp the invalid component
     * @return a string representing the error message
     */
    public String getErrorMessage(Component cmp) {
        return constraintList.get(cmp).getDefaultFailMessage();
    }

    void setValid(Component cmp, boolean v) {
        Boolean b = (Boolean)cmp.getClientProperty(VALID_MARKER);
        if(b != null && b.booleanValue() == v) {
            /*
            if (!v) {
                for(Component c : submitButtons) {
                    c.setEnabled(false);
                }
            }
            */
            return;
        }
        cmp.putClientProperty(VALID_MARKER, v);
        if(!v) {
            // if one component is invalid... just disable the submit buttons
            for(Component c : submitButtons) {
                c.setEnabled(false);
            }
        } else {
            boolean isV = isValid();
            for(Component c : submitButtons) {
                c.setEnabled(isV);
            }
            if(message != null && cmp.hasFocus()) {
                message.dispose();
            }
        }
        
        if(validationFailureHighlightMode == HighlightMode.EMBLEM || validationFailureHighlightMode == HighlightMode.UIID_AND_EMBLEM) {
            if(!(cmp.getComponentForm().getGlassPane() instanceof ComponentListener)) {
                cmp.getComponentForm().setGlassPane(new ComponentListener(null));
            }
        }
        if(v) {
            if(validationFailureHighlightMode == HighlightMode.UIID || validationFailureHighlightMode == HighlightMode.UIID_AND_EMBLEM) {
                String uiid = cmp.getUIID();
                if(uiid.endsWith("Invalid")) {
                    uiid = uiid.substring(0, uiid.length() - 7);
                    cmp.setUIID(uiid);
                }
                return;
            }
            if(validationFailureHighlightMode == HighlightMode.EMBLEM && validationFailedEmblem != null) {
                
            }
        } else {
            if(validationFailureHighlightMode == HighlightMode.UIID || validationFailureHighlightMode == HighlightMode.UIID_AND_EMBLEM) {
                String uiid = cmp.getUIID();
                if(!uiid.endsWith("Invalid")) {
                    cmp.setUIID(uiid + "Invalid");
                }
                return;
            }            
        }
    }
    
    class ComponentListener implements ActionListener, DataChangedListener, Painter {
        private Component cmp;
        public ComponentListener(Component cmp) {
            this.cmp = cmp;
        }
        
        public void actionPerformed(ActionEvent evt) {
            validate(cmp);
        }

        public void dataChanged(int type, int index) {
            validate(cmp);
        }

        /**
         * Handles the glasspane work just to save a new class object (smaller code)
         */
        @Override
        public void paint(Graphics g, Rectangle rect) {
            for(Component c : constraintList.keySet()) {
                if(!isValid(c)) {
                    int xpos = c.getAbsoluteX();
                    int ypos = c.getAbsoluteY();
                    float width = c.getWidth();
                    float height = c.getHeight();
                    xpos += Math.round(width * validationEmblemPositionX);
                    ypos += Math.round(height * validationEmblemPositionY);
                    g.drawImage(validationFailedEmblem, xpos - validationFailedEmblem.getWidth() / 2, ypos - validationFailedEmblem.getHeight() / 2);
                }
            }
        }
    }
}
