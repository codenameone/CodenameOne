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
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.InputComponent;
import com.codename1.ui.Label;
import com.codename1.ui.List;
import com.codename1.ui.Painter;
import com.codename1.ui.PickerComponent;
import com.codename1.ui.RadioButton;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextComponent;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.events.FocusListener;
import com.codename1.ui.events.ScrollListener;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.spinner.Picker;
import com.codename1.ui.util.UITimer;

import java.util.ArrayList;
import java.util.HashMap;

/// Binds validation constraints to form elements, when validation fails it can be highlighted directly on
/// the component via an emblem or change of the UIID (to original UIID name + "Invalid" e.g. "TextFieldInvalid").
/// Validators just run thru a set of Constraint objects to decide if validation succeeded or failed.
///
/// It's possible to create any custom logic of validation. Example (see
/// [this discussion](https://stackoverflow.com/questions/48481888/codename-one-regexconstraint-to-check-a-valid-phone-number/48483465#48483465) on StackOverflow):
///
/// ```java
/// val.addConstraint(phone, new Constraint() {
///   public  boolean isValid(Object value) {
///     String v = (String)value;
///     for(int i = 0 ; i = '0' && c <= '9' || c == '+' || c == '-') {
///         continue;
///       }
///       return false;
///     }
///     return true;
///   }
///   public String getDefaultFailMessage() {
///     return "Must be valid phone number";
///   }
/// });
/// ```
///
/// @author Shai Almog
public class Validator {
    private static final String VALID_MARKER = "cn1$$VALID_MARKER";
    /// Indicates the default mode in which validation failures are expressed
    private static HighlightMode defaultValidationFailureHighlightMode = HighlightMode.EMBLEM;
    /// The emblem that will be drawn on top of the component to indicate the validation failure
    private static Image defaultValidationFailedEmblem = null;
    /// The position of the validation emblem on the component as X/Y values between 0 and 1 where
    /// 0 indicates the start of the component and 1 indicates its end on the given axis.
    private static float defaultValidationEmblemPositionX = 1;
    /// The position of the validation emblem on the component as X/Y values between 0 and 1 where
    /// 0 indicates the start of the component and 1 indicates its end on the given axis.
    private static float defaultValidationEmblemPositionY = 0.5f;
    /// Indicates whether validation should occur on every key press (data change listener) or
    /// action performed (editing completion)
    private static boolean validateOnEveryKey = false;
    private final HashMap<Component, Constraint> constraintList = new HashMap<Component, Constraint>();
    private final ArrayList<Component> submitButtons = new ArrayList<Component>();
    private InteractionDialog message = new InteractionDialog();
    /// Error message UIID defaults to DialogBody. Allows customizing the look of the message
    private String errorMessageUIID = "DialogBody";
    /// Indicates the mode in which validation failures are expressed
    private HighlightMode validationFailureHighlightMode = defaultValidationFailureHighlightMode;
    /// The emblem that will be drawn on top of the component to indicate the validation failure
    private Image validationFailedEmblem = defaultValidationFailedEmblem;
    /// The position of the validation emblem on the component as X/Y values between 0 and 1 where
    /// 0 indicates the start of the component and 1 indicates its end on the given axis.
    private float validationEmblemPositionX = defaultValidationEmblemPositionX;
    /// The position of the validation emblem on the component as X/Y values between 0 and 1 where
    /// 0 indicates the start of the component and 1 indicates its end on the given axis.
    private float validationEmblemPositionY = defaultValidationEmblemPositionY;
    /// Indicates whether an error message should be shown for the focused component
    private boolean showErrorMessageForFocusedComponent;

    /// Default constructor
    public Validator() {
        validationFailedEmblem = initDefaultValidationFailedEmblem();
    }

    private static Image initDefaultValidationFailedEmblem() {
        synchronized (Validator.class) {
            if (defaultValidationFailedEmblem == null) {
                // initialize the default emblem
                defaultValidationFailedEmblem = FontImage.createMaterial(FontImage.MATERIAL_CANCEL, "InvalidEmblem", 3);
            }
            return defaultValidationFailedEmblem;
        }
    }

    /// Indicates the default mode in which validation failures are expressed
    ///
    /// #### Returns
    ///
    /// the defaultValidationFailureHighlightMode
    public static HighlightMode getDefaultValidationFailureHighlightMode() {
        return defaultValidationFailureHighlightMode;
    }

    /// Indicates the default mode in which validation failures are expressed
    ///
    /// #### Parameters
    ///
    /// - `aDefaultValidationFailureHighlightMode`: the defaultValidationFailureHighlightMode to set
    public static void setDefaultValidationFailureHighlightMode(HighlightMode aDefaultValidationFailureHighlightMode) {
        defaultValidationFailureHighlightMode = aDefaultValidationFailureHighlightMode;
    }

    /// The emblem that will be drawn on top of the component to indicate the validation failure
    ///
    /// #### Returns
    ///
    /// the defaultValidationFailedEmblem
    public static Image getDefaultValidationFailedEmblem() {
        return defaultValidationFailedEmblem;
    }

    /// The emblem that will be drawn on top of the component to indicate the validation failure
    ///
    /// #### Parameters
    ///
    /// - `aDefaultValidationFailedEmblem`: the defaultValidationFailedEmblem to set
    public static void setDefaultValidationFailedEmblem(Image aDefaultValidationFailedEmblem) {
        defaultValidationFailedEmblem = aDefaultValidationFailedEmblem;
    }

    /// The position of the validation emblem on the component as X/Y values between 0 and 1 where
    /// 0 indicates the start of the component and 1 indicates its end on the given axis.
    ///
    /// #### Returns
    ///
    /// the defaultValidationEmblemPositionX
    public static float getDefaultValidationEmblemPositionX() {
        return defaultValidationEmblemPositionX;
    }

    /// The position of the validation emblem on the component as X/Y values between 0 and 1 where
    /// 0 indicates the start of the component and 1 indicates its end on the given axis.
    ///
    /// #### Parameters
    ///
    /// - `aDefaultValidationEmblemPositionX`: the defaultValidationEmblemPositionX to set
    public static void setDefaultValidationEmblemPositionX(float aDefaultValidationEmblemPositionX) {
        defaultValidationEmblemPositionX = aDefaultValidationEmblemPositionX;
    }

    /// The position of the validation emblem on the component as X/Y values between 0 and 1 where
    /// 0 indicates the start of the component and 1 indicates its end on the given axis.
    ///
    /// #### Returns
    ///
    /// the defaultValidationEmblemPositionY
    public static float getDefaultValidationEmblemPositionY() {
        return defaultValidationEmblemPositionY;
    }

    /// The position of the validation emblem on the component as X/Y values between 0 and 1 where
    /// 0 indicates the start of the component and 1 indicates its end on the given axis.
    ///
    /// #### Parameters
    ///
    /// - `aDefaultValidationEmblemPositionY`: the defaultValidationEmblemPositionY to set
    public static void setDefaultValidationEmblemPositionY(float aDefaultValidationEmblemPositionY) {
        defaultValidationEmblemPositionY = aDefaultValidationEmblemPositionY;
    }

    /// Indicates whether validation should occur on every key press (data change listener) or
    /// action performed (editing completion)
    ///
    /// #### Returns
    ///
    /// the validateOnEveryKey
    public static boolean isValidateOnEveryKey() {
        return validateOnEveryKey;
    }

    /// Indicates whether validation should occur on every key press (data change listener) or
    /// action performed (editing completion)
    ///
    /// #### Parameters
    ///
    /// - `aValidateOnEveryKey`: the validateOnEveryKey to set
    public static void setValidateOnEveryKey(boolean aValidateOnEveryKey) {
        validateOnEveryKey = aValidateOnEveryKey;
    }

    /// Indicates the default mode in which validation failures are expressed
    ///
    /// #### Returns
    ///
    /// the validationFailureHighlightMode
    public HighlightMode getValidationFailureHighlightMode() {
        return validationFailureHighlightMode;
    }

    /// Indicates the default mode in which validation failures are expressed
    ///
    /// #### Parameters
    ///
    /// - `validationFailureHighlightMode`: the validationFailureHighlightMode to set
    public void setValidationFailureHighlightMode(HighlightMode validationFailureHighlightMode) {
        this.validationFailureHighlightMode = validationFailureHighlightMode;
    }

    /// The emblem that will be drawn on top of the component to indicate the validation failure
    ///
    /// #### Returns
    ///
    /// the validationFailedEmblem
    public Image getValidationFailedEmblem() {
        return validationFailedEmblem;
    }

    /// The emblem that will be drawn on top of the component to indicate the validation failure
    ///
    /// #### Parameters
    ///
    /// - `validationFailedEmblem`: the validationFailedEmblem to set
    public void setValidationFailedEmblem(Image validationFailedEmblem) {
        this.validationFailedEmblem = validationFailedEmblem;
    }

    /// The position of the validation emblem on the component as X/Y values between 0 and 1 where
    /// 0 indicates the start of the component and 1 indicates its end on the given axis.
    ///
    /// #### Returns
    ///
    /// the validationEmblemPositionX
    public float getValidationEmblemPositionX() {
        return validationEmblemPositionX;
    }

    /// The position of the validation emblem on the component as X/Y values between 0 and 1 where
    /// 0 indicates the start of the component and 1 indicates its end on the given axis.
    ///
    /// #### Parameters
    ///
    /// - `validationEmblemPositionX`: the validationEmblemPositionX to set
    public void setValidationEmblemPositionX(float validationEmblemPositionX) {
        this.validationEmblemPositionX = validationEmblemPositionX;
    }

    /// The position of the validation emblem on the component as X/Y values between 0 and 1 where
    /// 0 indicates the start of the component and 1 indicates its end on the given axis.
    ///
    /// #### Returns
    ///
    /// the validationEmblemPositionY
    public float getValidationEmblemPositionY() {
        return validationEmblemPositionY;
    }

    /// The position of the validation emblem on the component as X/Y values between 0 and 1 where
    /// 0 indicates the start of the component and 1 indicates its end on the given axis.
    ///
    /// #### Parameters
    ///
    /// - `validationEmblemPositionY`: the validationEmblemPositionY to set
    public void setValidationEmblemPositionY(float validationEmblemPositionY) {
        this.validationEmblemPositionY = validationEmblemPositionY;
    }

    /// Indicates whether an error message should be shown for the focused component
    ///
    /// #### Returns
    ///
    /// true if the error message should be displayed
    public boolean isShowErrorMessageForFocusedComponent() {
        return showErrorMessageForFocusedComponent;
    }

    /// Indicates whether an error message should be shown for the focused component
    ///
    /// #### Parameters
    ///
    /// - `showErrorMessageForFocusedComponent`: true to show the error message
    public void setShowErrorMessageForFocusedComponent(boolean showErrorMessageForFocusedComponent) {
        this.showErrorMessageForFocusedComponent = showErrorMessageForFocusedComponent;
    }

    /// Error message UIID defaults to DialogBody. Allows customizing the look of the message
    ///
    /// #### Returns
    ///
    /// the errorMessageUIID
    public String getErrorMessageUIID() {
        return errorMessageUIID;
    }

    /// Error message UIID defaults to DialogBody. Allows customizing the look of the message
    ///
    /// #### Parameters
    ///
    /// - `errorMessageUIID`: the errorMessageUIID to set
    public void setErrorMessageUIID(String errorMessageUIID) {
        this.errorMessageUIID = errorMessageUIID;
    }

    /// Places a constraint on the validator, returns this object so constraint
    /// additions can be chained. Shows validation errors messages even when the
    /// TextModeLayout is not `onTopMode` (it's possible to disable this
    /// functionality setting to false the theme constant
    /// `showValidationErrorsIfNotOnTopMode`: basically, the error
    /// message is shown for two second in place of the label on the left of the
    /// InputComponent (or on right of the InputComponent for RTL languages);
    /// this solution never breaks the layout, because the error message is
    /// trimmed to fit the available space. The error message UIID is
    /// "ErrorLabel" when it's not onTopMode.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: the component to validate
    ///
    /// - `c`: the constraint or constraints
    ///
    /// #### Returns
    ///
    /// @return this object so we can write code like v.addConstraint(cmp1,
    /// cons).addConstraint(cmp2, otherConstraint);
    public Validator addConstraint(Component cmp, Constraint... c) {
        Constraint constraint = null;
        if (c.length == 1) {
            constraint = c[0];
            constraintList.put(cmp, constraint);
        } else if (c.length > 1) {
            constraint = new GroupConstraint(c);
            constraintList.put(cmp, constraint);
        }
        if (constraint == null) {
            throw new IllegalArgumentException("addConstraint needs at least a Constraint, but the Constraint array in empty");
        }
        bindDataListener(cmp);
        boolean isV = isValid();
        for (Component btn : submitButtons) {
            btn.setEnabled(isV);
        }

        // Show validation error on iPhone
        if (UIManager.getInstance().isThemeConstant("showValidationErrorsIfNotOnTopMode", true) && cmp instanceof InputComponent) {
            final InputComponent inputComponent = (InputComponent) cmp;
            if (!inputComponent.isOnTopMode()) {
                Label labelForComponent = null;
                if (inputComponent instanceof TextComponent) {
                    labelForComponent = ((TextComponent) inputComponent).getField().getLabelForComponent();
                } else if (inputComponent instanceof PickerComponent) {
                    labelForComponent = ((PickerComponent) inputComponent).getPicker().getLabelForComponent();
                }

                if (labelForComponent != null) {
                    final Label myLabel = labelForComponent;
                    final String originalText = myLabel.getText();
                    final String originalUIID = myLabel.getUIID();
                    final Constraint myConstraint = constraint;

                    final Runnable showError = new Runnable() {
                        @Override
                        public void run() {
                            boolean isValid = false;
                            if (inputComponent instanceof TextComponent) {
                                isValid = myConstraint.isValid(((TextComponent) inputComponent).getField().getText());
                            } else if (inputComponent instanceof PickerComponent) {
                                isValid = myConstraint.isValid(((PickerComponent) inputComponent).getPicker().getValue());
                            }

                            String errorMessage = trimLongString(UIManager.getInstance().localize(myConstraint.getDefaultFailMessage(), myConstraint.getDefaultFailMessage()), "ErrorLabel", myLabel.getWidth());

                            if (errorMessage != null && errorMessage.length() > 0 && !isValid) {
                                // show the error in place of the label for component
                                myLabel.setUIID("ErrorLabel");
                                myLabel.setText(errorMessage);
                                UITimer.timer(2000, false, Display.getInstance().getCurrent(), new Runnable() {
                                    @Override
                                    public void run() {
                                        myLabel.setUIID(originalUIID);
                                        myLabel.setText(originalText);
                                    }
                                });
                            } else {
                                // show the label for component without the error
                                myLabel.setUIID(originalUIID);
                                myLabel.setText(originalText);
                            }
                        }
                    };

                    FocusListener myFocusListener = new ConstraintFocusListener(showError);

                    if (inputComponent instanceof TextComponent) {
                        ((TextComponent) inputComponent).getField().addFocusListener(myFocusListener);
                    } else if (inputComponent instanceof PickerComponent) {
                        ((PickerComponent) inputComponent).getPicker().addFocusListener(myFocusListener);
                    }

                }
            }
        }
        return this;
    }

    /// Long error messages are trimmed to fit the available space in the layout
    ///
    /// #### Parameters
    ///
    /// - `errorMessage`: the string to be trimmed
    ///
    /// - `uiid`: the uiid of the errorMessage
    ///
    /// - `width`: the maximum width
    ///
    /// #### Returns
    ///
    /// the new String trimmed to fit the available width
    private String trimLongString(String errorMessage, String uiid, int width) {
        Label errorLabel = new Label(errorMessage, uiid);
        while (errorLabel.getPreferredW() > width && errorMessage.length() > 1) {
            errorMessage = errorMessage.substring(0, errorMessage.length() - 1);
            errorLabel.setText(errorMessage);
        }
        return errorMessage;
    }

    /// Submit buttons (or any other component type) can be disabled until all components contain a valid value.
    /// Notice that this method should be invoked after all the constraints are added so the initial state of the buttons
    /// will be correct.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: set of buttons or components to disable until everything is valid
    ///
    /// #### Returns
    ///
    /// the validator instance so this method can be chained
    public Validator addSubmitButtons(Component... cmp) {
        boolean isV = isValid();
        for (Component c : cmp) {
            submitButtons.add(c);
            c.setEnabled(isV);
        }
        return this;
    }

    /// Returns the value of the given component, this can be overriden to add support for custom built components
    ///
    /// #### Parameters
    ///
    /// - `cmp`: the component
    ///
    /// #### Returns
    ///
    /// the object value
    protected Object getComponentValue(Component cmp) {
        if (cmp instanceof InputComponent) {
            cmp = ((InputComponent) cmp).getEditor();
        }
        if (cmp instanceof TextArea) {
            return ((TextArea) cmp).getText();
        }
        if (cmp instanceof Picker) {
            return ((Picker) cmp).getValue();
        }
        if (cmp instanceof RadioButton || cmp instanceof CheckBox) {
            if (((Button) cmp).isSelected()) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        if (cmp instanceof Label) {
            return ((Label) cmp).getText();
        }
        if (cmp instanceof List) {
            return ((List) cmp).getSelectedItem();
        }
        return null;
    }

    /// Binds an event listener to the given component
    ///
    /// #### Parameters
    ///
    /// - `cmp`: the component to bind the data listener to
    ///
    /// #### Deprecated
    ///
    /// @deprecated this method was exposed by accident, constraint implicitly calls it and you don't need to
    /// call it directly. It will be made protected in a future update to Codename One!
    public void bindDataListener(Component cmp) {
        if (showErrorMessageForFocusedComponent) {
            if (!(cmp instanceof InputComponent && ((InputComponent) cmp).isOnTopMode())) {
                cmp.addFocusListener(new FocusListener() {
                    @Override
                    public void focusGained(Component cmp) {
                        // special case. Before the form is showing don't show error dialogs
                        Form p = cmp.getComponentForm();
                        if (p != Display.getInstance().getCurrent()) { //NOPMD CompareObjectsWithEquals
                            return;
                        }
                        if (message != null) {
                            message.dispose();
                        }
                        if (!isValid(cmp)) {
                            String err = getErrorMessage(cmp);
                            if (err != null && err.length() > 0) {
                                message = new InteractionDialog(err);
                                message.getTitleComponent().setUIID(errorMessageUIID);
                                message.setAnimateShow(false);
                                if (validationFailureHighlightMode == HighlightMode.EMBLEM || validationFailureHighlightMode == HighlightMode.UIID_AND_EMBLEM) {
                                    int xpos = cmp.getAbsoluteX();
                                    int ypos = cmp.getAbsoluteY();
                                    Component scr = cmp.getScrollable();
                                    if (scr != null) {
                                        xpos -= scr.getScrollX();
                                        ypos -= scr.getScrollY();
                                        scr.addScrollListener(new ScrollListener() {
                                            @Override
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
                                    InteractionDialog dialog = message;
                                    dialog.showPopupDialog(new Rectangle(xpos, ypos, validationFailedEmblem.getWidth(),
                                            validationFailedEmblem.getHeight()));
                                } else {
                                    message.showPopupDialog(cmp);
                                }
                            }
                        }
                    }

                    @Override
                    public void focusLost(Component cmp) {
                    }
                });
            }
        }
        if (validateOnEveryKey) {
            if (cmp instanceof TextComponent) {
                ((TextComponent) cmp).getField().addDataChangedListener(new ComponentListener(cmp));
                return;
            }
            if (cmp instanceof TextField) {
                ((TextField) cmp).addDataChangedListener(new ComponentListener(cmp));
                return;
            }
        }
        if (cmp instanceof TextComponent) {
            ((TextComponent) cmp).getField().addActionListener(new ComponentListener(cmp));
            return;
        }
        if (cmp instanceof TextArea) {
            ((TextArea) cmp).addActionListener(new ComponentListener(cmp));
            return;
        }
        if (cmp instanceof List) {
            ((List) cmp).addActionListener(new ComponentListener(cmp));
            return;
        }
        if (cmp instanceof CheckBox || cmp instanceof RadioButton) {
            ((Button) cmp).addActionListener(new ComponentListener(cmp));
            return;
        }
        if (cmp instanceof Picker) {
            ((Picker) cmp).addActionListener(new ComponentListener(cmp));
            return;
        }
        if (cmp instanceof PickerComponent) {
            ((PickerComponent) cmp).getPicker().addActionListener(new ComponentListener(cmp));
        }
    }

    /// Returns true if all the constraints are currently valid
    ///
    /// #### Returns
    ///
    /// true if the entire validator is valid
    public boolean isValid() {
        for (Component c : constraintList.keySet()) {
            if (!isValid(c)) {
                return false;
            }
        }
        return true;
    }

    /// Validates and highlights an individual component
    ///
    /// #### Parameters
    ///
    /// - `cmp`: the component to validate
    protected void validate(Component cmp) {
        Object val = getComponentValue(cmp);
        Constraint c = constraintList.get(cmp);
        if (c != null) {
            setValid(cmp, c.isValid(val));
        }
    }

    boolean isValid(Component cmp) {
        Boolean b = (Boolean) cmp.getClientProperty(VALID_MARKER);
        if (b != null) {
            return b.booleanValue();
        }
        Object val = getComponentValue(cmp);
        Constraint c = constraintList.get(cmp);
        if (c != null) {
            return c.isValid(val);
        }
        return true;
    }

    /// Returns the validation error message for the given component or null if no such message exists
    ///
    /// #### Parameters
    ///
    /// - `cmp`: the invalid component
    ///
    /// #### Returns
    ///
    /// a string representing the error message
    public String getErrorMessage(Component cmp) {
        return constraintList.get(cmp).getDefaultFailMessage();
    }

    void setValid(Component cmp, boolean v) {
        Boolean b = (Boolean) cmp.getClientProperty(VALID_MARKER);
        if (b != null && b.booleanValue() == v) {
            return;
        }
        cmp.putClientProperty(VALID_MARKER, v);
        if (!v) {
            // if one component is invalid... just disable the submit buttons
            for (Component c : submitButtons) {
                c.setEnabled(false);
            }
        } else {
            boolean isV = isValid();
            for (Component c : submitButtons) {
                c.setEnabled(isV);
            }
            if (message != null && cmp.hasFocus()) {
                message.dispose();
            }
        }

        if (cmp instanceof InputComponent && ((InputComponent) cmp).isOnTopMode()) {
            InputComponent tc = (InputComponent) cmp;
            if (v) {
                tc.errorMessage(null);
            } else {
                tc.errorMessage(getErrorMessage(cmp));
            }
        }

        if (cmp.getComponentForm() != null) {
            if (validationFailureHighlightMode == HighlightMode.EMBLEM || validationFailureHighlightMode == HighlightMode.UIID_AND_EMBLEM) {
                if (!(cmp.getComponentForm().getGlassPane() instanceof ComponentListener)) {
                    cmp.getComponentForm().setGlassPane(new ComponentListener(null));
                }
            }
        }
        if (v) {
            if (validationFailureHighlightMode == HighlightMode.UIID || validationFailureHighlightMode == HighlightMode.UIID_AND_EMBLEM) {
                String uiid = cmp.getUIID();
                if (uiid.endsWith("Invalid")) {
                    uiid = uiid.substring(0, uiid.length() - 7);
                    cmp.setUIID(uiid);
                }
                return;
            }
        } else {
            if (validationFailureHighlightMode == HighlightMode.UIID || validationFailureHighlightMode == HighlightMode.UIID_AND_EMBLEM) {
                String uiid = cmp.getUIID();
                if (!uiid.endsWith("Invalid")) {
                    cmp.setUIID(uiid + "Invalid");
                }
            }
        }
    }

    /// Indicates the validation failure modes
    public enum HighlightMode {
        UIID,
        EMBLEM,
        UIID_AND_EMBLEM,
        NONE
    }

    private static class ConstraintFocusListener implements FocusListener {
        private final Runnable showError;

        public ConstraintFocusListener(Runnable showError) {
            this.showError = showError;
        }

        @Override
        public void focusLost(Component cmp) {
            showError.run();
        }

        @Override
        public void focusGained(Component cmp) {
            // no code here
        }
    }

    class ComponentListener implements ActionListener, DataChangedListener, Painter {
        private final Component cmp;
        private Rectangle visibleRect = new Rectangle();

        public ComponentListener(Component cmp) {
            this.cmp = cmp;
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            validate(cmp);
        }

        @Override
        public void dataChanged(int type, int index) {
            validate(cmp);
        }

        /// Handles the glasspane work just to save a new class object (smaller code)
        @Override
        public void paint(Graphics g, Rectangle rect) {
            for (Component c : constraintList.keySet()) {
                if (!isValid(c)) {
                    if (c instanceof InputComponent && ((InputComponent) c).isOnTopMode()) {
                        continue;
                    }
                    int xpos = c.getAbsoluteX();
                    int ypos = c.getAbsoluteY();
                    float width = c.getWidth();
                    float height = c.getHeight();
                    xpos += Math.round(width * validationEmblemPositionX);
                    ypos += Math.round(height * validationEmblemPositionY);

                    Container parent = c.getParent();
                    visibleRect = parent.getVisibleBounds(visibleRect);
                    Container grandParent = parent.getParent();
                    if (grandParent != null) {
                        visibleRect.setX(visibleRect.getX() + grandParent.getAbsoluteX());
                        visibleRect.setY(visibleRect.getY() + grandParent.getAbsoluteY());
                    }

                    int[] originalClip = g.getClip();
                    g.setClip(visibleRect);
                    if (xpos + validationFailedEmblem.getWidth() > Display.getInstance().getDisplayWidth()) {
                        g.drawImage(validationFailedEmblem, xpos - validationFailedEmblem.getWidth(), ypos - validationFailedEmblem.getHeight() / 2);
                    } else {
                        g.drawImage(validationFailedEmblem, xpos - validationFailedEmblem.getWidth() / 2, ypos - validationFailedEmblem.getHeight() / 2);
                    }
                    g.setClip(originalClip);
                }
            }
        }
    }
}
