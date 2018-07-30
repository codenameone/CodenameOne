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

package com.codename1.ui.spinner;

import com.codename1.components.InteractionDialog;
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.l10n.L10NManager;
import com.codename1.l10n.SimpleDateFormat;
import com.codename1.ui.Button;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.ComponentSelector;
import static com.codename1.ui.ComponentSelector.$;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.VirtualInputDevice;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.list.DefaultListModel;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.RoundRectBorder;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.ListIterator;

/**
 * <p>{@code Picker} is a component and API that allows either popping up a spinner or
 * using the native picker API when applicable. This is quite important for some
 * platforms where the native spinner behavior is very hard to replicate.</p>
 * 
 * <script src="https://gist.github.com/codenameone/5e437d82812dfcbdf092.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/components-picker.png" alt="Picker UI" />
 * <img src="https://www.codenameone.com/img/developer-guide/components-picker-date-time-on-simulator.png" alt="Date And Time Picker On the simulator" />
 * <img src="https://www.codenameone.com/img/developer-guide/components-picker-date-android.png" alt="Android native date picker" />
 * <img src="https://www.codenameone.com/img/developer-guide/components-picker-strings-android.png" alt="Android native String picker" />
 * <img src="https://www.codenameone.com/img/developer-guide/components-picker-time-android.png" alt="Android native time picker" />
 * <img src="https://www.codenameone.com/img/developer-guide/components-picker-duration-android.png" alt="Android duration picker" />
 * <img src="https://www.codenameone.com/img/developer-guide/components-picker-duration-hours-android.png" alt="Android duration hours picker" />
 * <img src="https://www.codenameone.com/img/developer-guide/components-picker-time-android.png" alt="Android duration minutes picker" />
 * 
 *
 * @author Shai Almog
 */
public class Picker extends Button {

    /**
     * Whether useLightweightPopup should default to true, this can be set via
     * the theme constant {@code lightweightPickerBool}
     * @return the defaultUseLightweightPopup
     */
    public static boolean isDefaultUseLightweightPopup() {
        return defaultUseLightweightPopup;
    }

    /**
     * Whether useLightweightPopup should default to true, this can be set via
     * the theme constant {@code lightweightPickerBool}
     * @param aDefaultUseLightweightPopup the defaultUseLightweightPopup to set
     */
    public static void setDefaultUseLightweightPopup(
        boolean aDefaultUseLightweightPopup) {
        defaultUseLightweightPopup = aDefaultUseLightweightPopup;
    }
    private int type = Display.PICKER_TYPE_DATE;
    private Object value = new Date();
    private boolean showMeridiem;
    private Object metaData;
    private Object renderingPrototype = "XXXXXXXXXXXXXX";
    private SimpleDateFormat formatter;
    private int preferredPopupWidth;
    private int preferredPopupHeight;
    private int minuteStep = 5;
    private VirtualInputDevice currentInput;
    
    // Variables to store the form's previous margins before showing
    // the popup dialog so that we can restore them when the popup is disposed.
    private byte[] tmpContentPaneMarginUnit;
    private float tmpContentPaneBottomMargin;

    /**
     * Whether useLightweightPopup should default to true, this can be set via
     * the theme constant {@code lightweightPickerBool}
     */
    private static boolean defaultUseLightweightPopup;
    
    /**
     * Flag to indicate that the picker should prefer lightweight components 
     * rather than native components.
     */
    private boolean useLightweightPopup;
    
    /**
     * Checks if the given type is supported in LightWeight mode.  
     * @param type The type.  Expects one of the Display.PICKER_XXX constants.
     * @return True if the given type is supported in lightweight mode.
     */
    private static boolean isLightweightModeSupportedForType(int type) {
        switch (type) {
            case Display.PICKER_TYPE_STRINGS:
            case Display.PICKER_TYPE_DATE:
            case Display.PICKER_TYPE_TIME:
            case Display.PICKER_TYPE_DATE_AND_TIME:
            case Display.PICKER_TYPE_DURATION:
            case Display.PICKER_TYPE_DURATION_HOURS:
            case Display.PICKER_TYPE_DURATION_MINUTES:
            case Display.PICKER_TYPE_CALENDAR:
                return true;
        }
        return false;
    }
    
    /**
     * Sets the picker to use lightweight mode for its widgets.  With this mode enabled
     * the picker will use cross-platform lightweight widgets instead of native widgets.
     * @param useLightweightPopup 
     */
    public void setUseLightweightPopup(boolean useLightweightPopup) {
        this.useLightweightPopup = useLightweightPopup;
    }
    
    /**
     * Checks if this picker is in lightweight mode.  If this returns true, then the 
     * picker will use cross-platform lightweight widgets instead of native widgets.
     */
    public boolean isUseLightweightPopup() {
        return useLightweightPopup;
    }
    
    /**
     * Check to see if the built-in action listener should ignore a given 
     * action event.  This allows us to propagate action events
     * out of the Picker as opposed to detecting clicks on the picker button.
     * @param evt
     * @return 
     */
    private boolean ignoreActionEvent(ActionEvent evt) {
        return evt.getX() == -99 && evt.getY() == -99;
    }
    
    /**
     * Default constructor
     */
    public Picker() {
        setUIID("Picker");
        setPreferredTabIndex(0);
        if (!Display.getInstance().isNativePickerTypeSupported(Display.PICKER_TYPE_STRINGS)) {
            // For platforms that don't support native pickers, we'll make lightweight mode
            // the default.  This will result in these platforms using the new Spinner3D classes
            // instead of the old Spinner classes
            useLightweightPopup = true;
        } else {
            defaultUseLightweightPopup = getUIManager().isThemeConstant("lightweightPickerBool", defaultUseLightweightPopup);
            useLightweightPopup = defaultUseLightweightPopup;
        }
        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if (ignoreActionEvent(evt)) {
                    // This was fired from the interaction dialog in lightweight mode
                    // we don't want to re-handle it here.
                    return;
                }
                if (isEditing()) {
                    evt.consume();
                    return;
                }
                if (useLightweightPopup && isLightweightModeSupportedForType(type)) {
                    showInteractionDialog();
                    evt.consume();
                    return;
                }
                
                if(Display.getInstance().isNativePickerTypeSupported(type)) {
                    
                    switch (type) {
                        case Display.PICKER_TYPE_DURATION:
                        case Display.PICKER_TYPE_DURATION_HOURS:
                        case Display.PICKER_TYPE_DURATION_MINUTES: {
                            metaData = "minuteStep="+minuteStep;
                            break;
                        }
                    }
                    
                    setEnabled(false);
                    Object val = Display.getInstance().showNativePicker(type, Picker.this, value, metaData);
                    if(val != null) {
                        value = val;
                        updateValue();
                    } else {
                        // cancel pressed.   Don't send the rest of the events.
                        evt.consume();
                    }
                    setEnabled(true);
                } else {
                    Dialog pickerDlg = new Dialog();
                    pickerDlg.setDisposeWhenPointerOutOfBounds(true);
                    pickerDlg.setLayout(new BorderLayout());
                    Calendar cld = Calendar.getInstance();
                    switch(type) {
                        case Display.PICKER_TYPE_STRINGS: {
                            GenericSpinner gs = new GenericSpinner();
                            if(renderingPrototype != null) {
                                gs.setRenderingPrototype((String)renderingPrototype);
                            }
                            String[] strArr = (String[])metaData;
                            gs.setModel(new DefaultListModel((Object[])strArr));
                            if(value != null) {
                                int slen = strArr.length;
                                for(int iter = 0 ; iter < slen ; iter++) {
                                    if(strArr[iter].equals(value)) {
                                        gs.getModel().setSelectedIndex(iter);
                                        break;
                                    }
                                }
                            }
                            if (showDialog(pickerDlg, gs)) {
                                value = gs.getValue();
                            } else {
                                evt.consume();
                            }
                            break;
                        }
                        case Display.PICKER_TYPE_CALENDAR:
                            showInteractionDialog();
                            evt.consume();
                            break;
                                
                        case Display.PICKER_TYPE_DATE: {
                            DateSpinner ds = new DateSpinner();
                            if(value == null) {
                                cld.setTime(new Date());
                            } else {
                                cld.setTime((Date)value);
                            }
                            ds.setStartYear(1900);
                            ds.setCurrentDay(cld.get(Calendar.DAY_OF_MONTH));
                            ds.setCurrentMonth(cld.get(Calendar.MONTH) + 1);
                            ds.setCurrentYear(cld.get(Calendar.YEAR));
                            if (showDialog(pickerDlg, ds)) {
                            
                                cld.set(Calendar.DAY_OF_MONTH, ds.getCurrentDay());
                                cld.set(Calendar.MONTH, ds.getCurrentMonth() - 1);
                                cld.set(Calendar.YEAR, ds.getCurrentYear());
                                value = cld.getTime();
                            } else {
                                evt.consume();
                            }
                            break;
                        }
                        case Display.PICKER_TYPE_TIME: {
                            int v = ((Integer)value).intValue();
                            int hour = v / 60;
                            int minute = v % 60;
                            TimeSpinner ts = new TimeSpinner();
                            ts.setShowMeridiem(isShowMeridiem());
                            if(showMeridiem && hour > 12) {
                                ts.setCurrentMeridiem(true);
                                ts.setCurrentHour(hour - 12);
                            } else {
                                ts.setCurrentHour(hour);
                            }
                            ts.setCurrentMinute(minute);
                            if (showDialog(pickerDlg, ts)) {

                                if(isShowMeridiem()) {
                                    int offset = 0;
                                    if(ts.getCurrentHour() == 12) {
                                        if(!ts.isCurrentMeridiem()) {
                                            offset = 12;
                                        }
                                    } else {
                                        if(ts.isCurrentMeridiem()) {
                                            offset = 12;
                                        }
                                    }
                                    hour = ts.getCurrentHour() + offset;
                                } else {
                                    hour = ts.getCurrentHour();
                                }
                                value = new Integer(hour * 60 + ts.getCurrentMinute());
                            } else {
                                evt.consume();
                            }
                            break;
                        }
                        case Display.PICKER_TYPE_DATE_AND_TIME: {
                            DateTimeSpinner dts = new DateTimeSpinner();
                            cld.setTime((Date)value);
                            dts.setCurrentDate((Date)value);
                            dts.setShowMeridiem(isShowMeridiem());
                            if(isShowMeridiem() && dts.isCurrentMeridiem()) {
                                dts.setCurrentHour(cld.get(Calendar.HOUR));
                            } else {
                                dts.setCurrentHour(cld.get(Calendar.HOUR_OF_DAY));
                            }
                            dts.setCurrentMinute(cld.get(Calendar.MINUTE));
                            if (showDialog(pickerDlg, dts)) {
                                cld.setTime(dts.getCurrentDate());
                                if(isShowMeridiem() && dts.isCurrentMeridiem()) {
                                    cld.set(Calendar.AM_PM, Calendar.PM);
                                    cld.set(Calendar.HOUR, dts.getCurrentHour());
                                } else {
                                    cld.set(Calendar.HOUR_OF_DAY, dts.getCurrentHour());
                                }
                                cld.set(Calendar.MINUTE, dts.getCurrentMinute());
                                value = cld.getTime();
                            } else {
                                evt.consume();
                            }
                            break;
                        }
                        case Display.PICKER_TYPE_DURATION_HOURS:
                        case Display.PICKER_TYPE_DURATION_MINUTES:
                        case Display.PICKER_TYPE_DURATION: {
                            long v = ((Long)value).longValue();
                            int hour = (int)(v / 1000 / 60 / 60);
                            int minute = (int) ((v / 1000 / 60) % 60);
                            TimeSpinner ts = new TimeSpinner();
                            ts.setDurationMode(true);
                            if (type == Display.PICKER_TYPE_DURATION_HOURS) {
                                ts.setMinutesVisible(false);
                            } else if (type == Display.PICKER_TYPE_DURATION_MINUTES) {
                                ts.setHoursVisible(false);
                            }
                            ts.setCurrentHour(hour);
                            ts.setCurrentMinute(minute);
                            ts.setMinuteStep(minuteStep);
                            if (showDialog(pickerDlg, ts)) {
                                
                                value = new Long(ts.getCurrentHour() * 60 * 60 * 1000l + 
                                        ts.getCurrentMinute() * 60 * 1000l);
                            } else {
                                evt.consume();
                            }
                            break;
                        }
                            
                    }
                    updateValue();
                }
            }
            
            private Spinner3D createStringPicker3D() {
                Spinner3D out = new Spinner3D(new DefaultListModel((String[])metaData));
                if (value != null) {
                    out.setValue(value);
                }
                //out.refreshStyles();
                return out;
            }
            
            private DateSpinner3D createDatePicker3D() {
                DateSpinner3D out = new DateSpinner3D();
                if (value != null) {
                    out.setValue(value);
                } else {
                    out.setValue(new Date());
                }
                return out;
            }
            
            private CalendarPicker createCalendarPicker() {
                
                CalendarPicker out = new CalendarPicker();
                if (value != null) {
                    out.setValue(value);
                } else {
                    out.setValue(new Date());
                }
            
                return out;
            }
            
            private TimeSpinner3D createTimePicker3D() {
                TimeSpinner3D out = new TimeSpinner3D();
                out.setMinuteStep(minuteStep);
                out.setShowMeridiem(showMeridiem);
                if (value != null) {
                    out.setValue(value);
                } else {
                    out.setValue(0);
                }
                return out;
            }
            
            private DateTimeSpinner3D createDateTimePicker3D() {
                DateTimeSpinner3D out = new DateTimeSpinner3D();
                if (value != null) {
                    out.setValue(value);
                } else {
                    out.setValue(new Date());
                }
                return out;
            }
            
            private DurationSpinner3D createDurationPicker3D() {
                DurationSpinner3D out = new DurationSpinner3D(
                        type == Display.PICKER_TYPE_DURATION_MINUTES ? DurationSpinner3D.FIELD_MINUTE :
                        type == Display.PICKER_TYPE_DURATION_HOURS ? DurationSpinner3D.FIELD_HOUR :
                                DurationSpinner3D.FIELD_HOUR | DurationSpinner3D.FIELD_MINUTE
                );
                if (value != null) {
                    out.setValue(value);
                } else {
                    out.setValue(0);
                }
                return out;
            }
            
            private static final int COMMAND_DONE=1;
            private static final int COMMAND_NEXT=2;
            private static final int COMMAND_PREV=3;
            private static final int COMMAND_CANCEL=4;
            
            private void endEditing(int command, InteractionDialog dlg, InternalPickerWidget spinner) {
                currentInput = null;
                restoreContentPane();
                dlg.disposeToTheBottom();
                if (command != COMMAND_CANCEL) {
                    value = spinner.getValue();
                    updateValue();
                    // (x, y) = (-99, -99) signals the built-in action listner
                    // to ignore this event and just propagage it to external
                    // listeners.  See ignoreActionEvent(ActionEvent)
                    fireActionEvent(-99, -99);
                    
                    Component next = null;
                    Form f = getComponentForm();
                    if (f != null) {
                        if (command == COMMAND_NEXT) {
                            next = f.getNextComponent(Picker.this);
                        } else if (command == COMMAND_PREV) {
                            next = f.getPreviousComponent(Picker.this);
                        }
                    }
                    if (next != null) {
                        next.requestFocus();
                        next.startEditingAsync();
                    }
                }
            }
            
            private void showInteractionDialog() {
                boolean isTablet = Display.getInstance().isTablet();
                final InternalPickerWidget spinner;
                switch (type) {
                    case Display.PICKER_TYPE_STRINGS:
                        spinner = createStringPicker3D();
                        break;
                    case Display.PICKER_TYPE_CALENDAR:
                        spinner = createCalendarPicker();
                        break;
                    case Display.PICKER_TYPE_DATE:
                        spinner = createDatePicker3D();
                        break;
                    case Display.PICKER_TYPE_TIME:
                        spinner = createTimePicker3D();
                        break;
                    case Display.PICKER_TYPE_DATE_AND_TIME:
                        spinner = createDateTimePicker3D();
                        break;
                    case Display.PICKER_TYPE_DURATION:
                    case Display.PICKER_TYPE_DURATION_HOURS:
                    case Display.PICKER_TYPE_DURATION_MINUTES:
                        spinner = createDurationPicker3D();
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported picker type "+type);
                }
                final InteractionDialog dlg = new InteractionDialog() {

                    ActionListener keyListener;
                    @Override
                    protected void initComponent() {
                        final InteractionDialog self = this;
                        super.initComponent();
                        if (keyListener == null) {
                            keyListener = new ActionListener() {

                                @Override
                                public void actionPerformed(ActionEvent evt) {
                                    if (Display.getInstance().isShiftKeyDown()) {
                                        endEditing(COMMAND_PREV, self, spinner);
                                    } else {
                                        endEditing(COMMAND_NEXT, self, spinner);
                                    }
                                        
                                }
                                
                            };
                        }
                        getComponentForm().addKeyListener(9, keyListener);
                    }

                    @Override
                    protected void deinitialize() {
                        Form f = getComponentForm();
                        if (f == null) {
                            f = Display.getInstance().getCurrent();
                        }
                        if (f != null && keyListener != null) {
                            f.removeKeyListener(9, keyListener);
                        }
                        super.deinitialize();
                    }
                    
                    
                    
                };
                //dlg.setFormMode(!isTablet);
                ComponentSelector.select("DialogTitle", dlg).getParent().setPadding(0).setMargin(0).setBorder(Border.createEmpty());
                dlg.getTitleComponent().setVisible(false);
                ComponentSelector.select(dlg.getTitleComponent()).setPadding(0).setMargin(0);
                dlg.setUIID(isTablet ? "PickerDialogTablet" : "PickerDialog");
                dlg.getUnselectedStyle().setBgColor(new Label("", "Spinner3DOverlay").getUnselectedStyle().getBgColor());
                dlg.getUnselectedStyle().setBgTransparency(255);
                if (isTablet) {
                    
                    dlg.getUnselectedStyle().setBorder(RoundRectBorder.create().cornerRadius(2f));
                    
                }
                
                dlg.getContentPane().setLayout(new BorderLayout());
                
                String dlgUiid = isTablet ? "PickerDialogContentTablet" : "PickerDialogContent";
                dlg.getContentPane().setUIID(dlgUiid);
                dlg.getContentPane().getUnselectedStyle().setBgColor(new Label("", "Spinner3DOverlay").getUnselectedStyle().getBgColor());
                
                
                final Component spinnerC;
                
                
                spinnerC = (Component)spinner;
                Container wrapper = BorderLayout.center(spinnerC);
                ComponentSelector.select(wrapper).addTags("SpinnerWrapper");
                ComponentSelector.select(wrapper).selectAllStyles()
                        .setBorder(Border.createEmpty())
                        .setBgTransparency(0)
                        .setMargin(0)
                        .setPaddingMillimeters(3f, 0);
                //wrapper.add(BorderLayout.CENTER, spinnerC);
                dlg.getContentPane().add(BorderLayout.CENTER, wrapper);
                
                
                Button doneButton = new Button("Done", isTablet? "PickerButtonTablet" : "PickerButton");
                doneButton.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        endEditing(COMMAND_DONE, dlg, spinner);
                        
                        
                    }
                    
                });
                Button cancelButton = new Button("Cancel", isTablet ? "PickerButtonTablet" : "PickerButton");
                cancelButton.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        endEditing(COMMAND_CANCEL, dlg, spinner);
                        
                                
                    }
                    
                });
                
                Button nextButton = null;
                //final Component nextComponent = getNextFocusRight() != null ? getNextFocusRight() :
                //        getNextFocusDown() != null ? getNextFocusDown() :
                //        null;
                ListIterator<Component> traversalIt = getComponentForm().getTabIterator(Picker.this);
                if (traversalIt.hasNext()) {
                    nextButton = new Button("", isTablet ? "PickerButtonTablet" : "PickerButton");
                    // Javascript port needs to know that this button is going to try to 
                    // focus a text field (possibly) so that it can prepare the text field
                    // in the native event handler.  We use this client property to let it know... it
                    // will handle the rest.
                    nextButton.putClientProperty("$$focus", ((Form.TabIterator)traversalIt).getNext());
                    FontImage.setMaterialIcon(nextButton, FontImage.MATERIAL_KEYBOARD_ARROW_DOWN);
                    nextButton.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent evt) {
                            endEditing(COMMAND_NEXT, dlg, spinner);
                            
                        }
                        
                    });
                }
                
                Button prevButton = null;
                
                if (traversalIt.hasPrevious()) {
                    prevButton = new Button("", isTablet ? "PickerButtonTablet" : "PickerButton");
                    
                    // Javascript port needs to know that this button is going to try to 
                    // focus a text field (possibly) so that it can prepare the text field
                    // in the native event handler.  We use this client property to let it know... it
                    // will handle the rest.
                    prevButton.putClientProperty("$$focus", ((Form.TabIterator)traversalIt).getPrevious());
                    FontImage.setMaterialIcon(prevButton, FontImage.MATERIAL_KEYBOARD_ARROW_UP);
                    prevButton.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent evt) {
                            endEditing(COMMAND_PREV, dlg, spinner);
                            
                        }
                        
                    });
                }
                        
                
                Container west = new Container(BoxLayout.x());
                $(west).selectAllStyles().setMargin(0).setPadding(0).setBorder(Border.createEmpty()).setBgTransparency(0);
                west.add(cancelButton);
                if (prevButton != null) {
                    west.add(prevButton);
                }
                if (nextButton != null) {
                    west.add(nextButton);
                }
                
                Container buttonBar = BorderLayout.centerEastWest(null, doneButton, west);
                buttonBar.setUIID(isTablet ? "PickerButtonBarTablet" : "PickerButtonBar");
                dlg.getContentPane().add(BorderLayout.NORTH, buttonBar);
                
                Form form = getComponentForm();
                if (form == null) {
                    throw new RuntimeException("Attempt to show interaction dialog while button is not on form.  Illegal state");
                }
                
                final int top = Math.max(0, form.getContentPane().getHeight() - dlg.getPreferredH());
                if (top == 0) {
                    wrapper.getUnselectedStyle().setPaddingTop(0);
                    wrapper.getUnselectedStyle().setPaddingBottom(0);
                }
                final int left = 0;
                final int right = 0;
                final int bottom = 0;
                dlg.setWidth(Display.getInstance().getDisplayWidth());
                dlg.setHeight(dlg.getPreferredH());
                dlg.setY(Display.getInstance().getDisplayHeight());
                dlg.setX(0);
                dlg.setRepositionAnimation(false);
                registerAsInputDevice(dlg);
                if (Display.getInstance().isTablet()) {
                    getComponentForm().getAnimationManager().flushAnimation(new Runnable() {

                        @Override
                        public void run() {
                            dlg.showPopupDialog(Picker.this);
                        }
                        
                    });
                    
                } else {
                    getComponentForm().getAnimationManager().flushAnimation(new Runnable() {

                        @Override
                        public void run() {
                            dlg.show(top, bottom, left, right);
                            padContentPane(top, bottom, left, right);
                        }
                        
                    });
                    
                }
                
            }
            
            
            private boolean showDialog(Dialog pickerDlg, Component c) {
                pickerDlg.addComponent(BorderLayout.CENTER, c);
                Button ok = new Button(new Command("OK"));
                final boolean[] userCanceled = new boolean[1];
                Button cancel = new Button(new Command("Cancel") {
                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        userCanceled[0] = true;
                        super.actionPerformed(evt);
                    }
                });
                Container buttons = GridLayout.encloseIn(2, cancel, ok);
                pickerDlg.addComponent(BorderLayout.SOUTH, buttons);
                if(Display.getInstance().isTablet()) {
                    pickerDlg.showPopupDialog(Picker.this);
                } else {
                    pickerDlg.show();
                }
                return !userCanceled[0];
            }
        });
        updateValue();
    }

    @Override
    public void startEditingAsync() {
        fireActionEvent(-1, -1);
    }

    @Override
    public void stopEditing(Runnable onFinish) {
        stopEditingCallback = onFinish;
        Form f = this.getComponentForm();
        if (f != null) {
            if (f.getCurrentInputDevice() == currentInput) {
                try {
                    f.setCurrentInputDevice(null);
                } catch (Throwable t) {
                    Log.e(t);
                }
            }
        }
    }

    @Override
    public boolean isEditing() {
        Form f = this.getComponentForm();
        boolean out = currentInput != null &&  f != null && f.getCurrentInputDevice() == currentInput;
        
        return out;
    }

    @Override
    public boolean isEditable() {
        return isUseLightweightPopup();
    }
    
    
    
    /**
     * Sets the type of the picker to one of Display.PICKER_TYPE_DATE, Display.PICKER_TYPE_DATE_AND_TIME, Display.PICKER_TYPE_STRINGS, 
     * Display.PICKER_TYPE_DURATION, Display.PICKER_TYPE_DURATION_HOURS, Display.PICKER_TYPE_DURATION_MINUTES or
     * Display.PICKER_TYPE_TIME
     * @param type the type
     */
    public void setType(int type) {
        this.type = type;
        switch(type) {
            case Display.PICKER_TYPE_DATE:
            case Display.PICKER_TYPE_DATE_AND_TIME:
                if(!(value instanceof Date)) {
                    value = new Date();
                }
                break;
            case Display.PICKER_TYPE_STRINGS:
                if(!Util.instanceofObjArray(value) && !(value instanceof String[])) {
                    setStrings(new String[] {" "});
                }
                break;
            case Display.PICKER_TYPE_TIME:
                if(!(value instanceof Integer)) {
                    setTime(0);
                }
                break;
            case Display.PICKER_TYPE_DURATION:
            case Display.PICKER_TYPE_DURATION_HOURS:
            case Display.PICKER_TYPE_DURATION_MINUTES:
                    
                if (!(value instanceof Long)) {
                    setDuration(0l);
                }
                break;
        }
    }

    /**
     * Returns the type of the picker
     * @return one of Display.PICKER_TYPE_DATE, Display.PICKER_TYPE_DATE_AND_TIME, Display.PICKER_TYPE_STRINGS,
     * Display.PICKER_TYPE_DURATION, Display.PICKER_TYPE_DURATION_HOURS, Display.PICKER_TYPE_DURATION_MINUTES, or
     * Display.PICKER_TYPE_TIME
     */
    public int getType() {
        return type;
    }
    
    /**
     * Returns the date, this value is used both for type date/date and time. Notice that this 
     * value isn't used for time
     * @return the date object
     */
    public Date getDate() {
        return (Date)value;
    }
    
    /**
     * Sets the date, this value is used both for type date/date and time. Notice that this 
     * value isn't used for time. Notice that this value will have no effect if the picker
     * is currently showing.
     * 
     * @param d the new date
     */
    public void setDate(Date d) {
        value = d;
        updateValue();
    }
    
    private String twoDigits(int i) {
        if(i < 10) {
            return "0" + i;
        }
        return "" + i;
    }
    
    /**
     * <p>Sets the string entries for the string picker. <br>
     * sample usage for this method below:</p>
     * 
     * <script src="https://gist.github.com/codenameone/47602e679f61712693bd.js"></script>
     * @param strs string array
     */
    public void setStrings(String... strs) {
        this.type = Display.PICKER_TYPE_STRINGS;
        int slen = strs.length;
        for (int i = 0; i < slen; i++) {
            String str = strs[i];
            strs[i] = getUIManager().localize(str, str);
        }
        metaData = strs;
        
        if(!(value instanceof String)) {
            value = null;
        }
        updateValue();
    }
    
    private void restoreContentPane() {
        Form f = getComponentForm();
        
        if (tmpContentPaneMarginUnit != null && f != null) {
            Container contentPane = f.getContentPane();
            Style style = contentPane.getStyle();
            style.setMarginUnit(tmpContentPaneMarginUnit);
            style.setMarginBottom(tmpContentPaneBottomMargin);
            tmpContentPaneMarginUnit=null;
            f.revalidate();
            // If we remove the margin, it sometimes leaves the content pane
            // in a negative scroll position - which leaves a gap at the top.
            // Simulating a drag will trigger tensile drag to push the content
            // back up to the top.
            // See https://github.com/codenameone/CodenameOne/issues/2476
            if (f != null && f.getContentPane() != null && f.getContentPane().getScrollY() < 0) {
                f.getContentPane().pointerPressed(100, 100);
                f.getContentPane().pointerDragged(100, 100);
                f.getContentPane().pointerReleased(100, 100);
            }
        }
    }



    private void padContentPane(final int top, final int bottom, final int left, final int right) {
        final Form f = getComponentForm();
        if (f != null) {
            f.getAnimationManager().flushAnimation(new Runnable() {
                public void run() {
                    Container contentPane = f.getContentPane();
                    Style style = contentPane.getStyle();
                    byte[] marginUnits = style.getMarginUnit();
                    if (marginUnits == null) {
                        marginUnits = new byte[]{
                                Style.UNIT_TYPE_PIXELS,
                                Style.UNIT_TYPE_PIXELS,
                                Style.UNIT_TYPE_PIXELS,
                                Style.UNIT_TYPE_PIXELS
                        };
                    }
                    if (tmpContentPaneMarginUnit == null) {
                        tmpContentPaneMarginUnit = new byte[4];
                        System.arraycopy(marginUnits, 0, tmpContentPaneMarginUnit, 0, 4);
                        tmpContentPaneBottomMargin = style.getMarginBottom();
                    }


                    marginUnits[Component.BOTTOM] = Style.UNIT_TYPE_PIXELS;
                    style.setMarginUnit(marginUnits);
                    style.setMarginBottom(Math.max(0, contentPane.getHeight() - top));
                    f.revalidate();

                    f.scrollComponentToVisible(Picker.this);
                }

            });


        }
    }

    private void registerAsInputDevice(final InteractionDialog dlg) {
        
        final Form f = this.getComponentForm();
        if (f != null) {
            final ActionListener sizeChanged;
            if (!Display.getInstance().isTablet()) {
                sizeChanged = new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        final int top = f.getContentPane().getHeight() - dlg.getPreferredH();
                        if (top <= 0) {
                            ComponentSelector.select(".SpinnerWrapper", dlg).setPadding(0);
                        }
                        final int left = 0;
                        final int right = 0;
                        final int bottom = 0;
                        dlg.setWidth(Display.getInstance().getDisplayWidth());
                        dlg.setHeight(dlg.getPreferredH());
                        dlg.setY(Display.getInstance().getDisplayHeight());
                        dlg.setX(0);
                        f.getAnimationManager().flushAnimation(new Runnable() {

                            @Override
                            public void run() {
                                dlg.resize(top, bottom, left, right);
                                padContentPane(top, bottom, left, right);




                            }

                        });
                    }

                };
                f.addSizeChangedListener(sizeChanged);
            } else {
                sizeChanged = null;
            }
            
            try {
                VirtualInputDevice nextInput = new VirtualInputDevice() {

                    @Override
                    public void close() throws Exception {
                        currentInput = null;
                        if (sizeChanged != null) {
                            f.removeSizeChangedListener(sizeChanged);
                        }
                        if (dlg.isShowing()) {
                            restoreContentPane();
                            dlg.disposeToTheBottom(new Runnable() {
                                public void run() {
                                    if (stopEditingCallback != null) {
                                        Runnable r = stopEditingCallback;
                                        stopEditingCallback = null;
                                        r.run();
                                    }
                                }
                            });
                        } else {
                            stopEditingCallback = null;
                        } 
                    }
                };
                f.setCurrentInputDevice(nextInput);
                currentInput = nextInput;
            } catch (Exception ex) {
                Log.e(ex);
                // Failed to edit string because the previous input device would not
                // give up control
                return;
            }
        }
    }
    
    private Runnable stopEditingCallback;
    
    /**
     * Returns the String array matching the metadata
     * @return a string array
     */
    public String[] getStrings() {
        return (String[])metaData;
    }
    
    /**
     * Sets the current value in a string array picker
     * @param str the current value
     */
    public void setSelectedString(String str) {
        value = str;
        updateValue();
    }
    
    /**
     * Returns the current string
     * @return the selected string
     */
    public String getSelectedString() {
        return (String) value;
    }
    
    /**
     * Returns the index of the selected string
     * @return the selected string offset or -1
     */
    public int getSelectedStringIndex() {
        int offset = 0;
        for(String s : (String[])metaData) {
            if(s == value) {
                return offset;
            }
            offset++;
        }
        return -1;
    }

    /**
     * Returns the index of the selected string
     * @param index sets the index of the selected string
     */
    public void setSelectedStringIndex(int index) {
        value = ((String[])metaData)[index];
        updateValue();
    }

    /**
     * Updates the display value of the picker, subclasses can override this to invoke 
     * set text with the right value
     */
    protected void updateValue() {
        if(value == null) {
            setText("...");
            return;
        }
        
        if(getFormatter() != null) {
            setText(formatter.format(value));
            return;
        }
        
        switch(type) {
            case Display.PICKER_TYPE_STRINGS: {
                value = getUIManager().localize(value.toString(), value.toString());
                setText(value.toString());
                break;
            }
            case Display.PICKER_TYPE_CALENDAR:
            case Display.PICKER_TYPE_DATE: {
                setText(L10NManager.getInstance().formatDateShortStyle((Date)value));
                break;
            }
            case Display.PICKER_TYPE_TIME: {
                int v = ((Integer)value).intValue();
                int hour = v / 60;
                int minute = v % 60;
                if(showMeridiem) {
                    String text;
                    if(hour >= 12) {
                        text = "pm";
                    } else {
                        text = "am";
                    }
                    int cookedHour = hour <= 12 ? hour : hour - 12;
                    if (cookedHour == 0) {
                        cookedHour = 12;
                    }
                    setText(twoDigits(cookedHour) + ":" + twoDigits(minute) + text);
                } else {
                    setText(twoDigits(hour) + ":" + twoDigits(minute));
                }
                break;
            }
            case Display.PICKER_TYPE_DATE_AND_TIME: {
                setText(L10NManager.getInstance().formatDateTimeShort((Date)value));
                break;
            }
            case Display.PICKER_TYPE_DURATION_HOURS:
            case Display.PICKER_TYPE_DURATION_MINUTES:
            case Display.PICKER_TYPE_DURATION: {
                long v = ((Long)value).longValue();
                int hour = (int)(v / 60 / 60 / 1000);
                int minute = (int)(v / 1000 / 60) % 60;
                StringBuilder sb = new StringBuilder();
                UIManager uim = getUIManager();
                if (hour > 0) {
                    sb.append(hour).append(" ")
                            .append(hour > 1 ? uim.localize("hours", "hours") : uim.localize("hour", "hour"))
                            .append(" ");
                }
                if (minute > 0) {
                    sb.append(minute).append(" ")
                            .append(minute > 1 ? uim.localize("minutes", "minutes") : uim.localize("minute", "minute"));
                            
                }
                setText(sb.toString().trim());
                if ("".equals(getText())) {
                    setText("...");
                }
                break;
            }
                
        }
    }
    
    /**
     * This value is only used for time type and is ignored in the case of date and time where
     * both are embedded within the date.
     * @param time the time value as minutes since midnight e.g. 630 is 10:30am
     */
    public void setTime(int time) {
        value = new Integer(time);
        updateValue();
    }

    /**
     * Convenience method equivalent to invoking setTime(hour * 60 + minute);
     * @param hour the hour in 24hr format
     * @param minute the minute within the hour
     */
    public void setTime(int hour, int minute) {
        setTime(hour * 60 + minute);
    }
    
    /**
     * This value is only used for time type and is ignored in the case of date and time where
     * both are embedded within the date.
     * 
     * @return the time value as minutes since midnight e.g. 630 is 10:30am
     */
    public int getTime() {
        return ((Integer)value).intValue();
    }
    
    /**
     * This value is only used for duration type.
     * @param duration The duration value in milliseconds.
     * @see #setDuration(int, int) 
     * @see #getDuration() 
     * @see #getDurationHours() 
     * @see #getDurationMinutes() 
     */
    public void setDuration(long duration) {
        value = new Long(duration);
        updateValue();
    }
    
    /**
     * Sets the minute step size for PICKER_TYPE_DURATION, and PICKER_TYPE_DURATION_TIME types.
     * @param step The step size in minutes.
     */
    public void setMinuteStep(int step) {
        this.minuteStep = step;
    }
    
    /**
     * Convenience method for setting duration in hours and minutes.
     * @param hour The hours for duration.
     * @param minute The minutes for duration.
     * @see #setDuration(long) 
     * @see #getDuration() 
     * @see #getDurationHours() 
     * @see #getDurationMinutes() 
     */
    public void setDuration(int hour, int minute) {
        setDuration(hour * 60 * 60 * 1000l + minute * 60 *1000l);
    }
    
    /**
     * This value is used for the duration type.
     * @return The duration in milliseconds.
     * @see #getDurationHours() 
     * @see #getDurationMinutes() 
     */
    public long getDuration() {
        return (Long)value;
    }
    
    /**
     * Gets the duration hours.  Used only for duration type.
     * @return The duration hours.
     * @see #getDurationMinutes() 
     * @see #getDuration() 
     */
    public int getDurationHours() {
        return (int)(getDuration() / 60 / 60 / 1000l);
    }
    
    /**
     * Gets the duration minutes.  Used only for duration type.
     * @return The duration minutes.
     * @see #getDurationHours() 
     * @see #getDuration()
     */
    public int getDurationMinutes() {
        return (int)(getDuration() / 1000 / 60) % 60;
    }

    /**
     * Indicates whether hours should be rendered as AM/PM or 24hr format
     * @return the showMeridiem
     */
    public boolean isShowMeridiem() {
        return showMeridiem;
    }

    /**
     * Indicates whether hours should be rendered as AM/PM or 24hr format
     * @param showMeridiem the showMeridiem to set
     */
    public void setShowMeridiem(boolean showMeridiem) {
        this.showMeridiem = showMeridiem;
        updateValue();
    }

    /**
     * When using a lightweight spinner this will be used as the rendering prototype
     * @return the renderingPrototype
     */
    public Object getRenderingPrototype() {
        return renderingPrototype;
    }

    /**
     * When using a lightweight spinner this will be used as the rendering prototype
     * @param renderingPrototype the renderingPrototype to set
     */
    public void setRenderingPrototype(Object renderingPrototype) {
        this.renderingPrototype = renderingPrototype;
    }

    /**
     * Allows us to define a date format for the display of dates/times
     * @return the defined formatter
     */
    public SimpleDateFormat getFormatter() {
        return formatter;
    }

    /**
     * Allows us to define a date format for the display of dates/times
     * 
     * @param formatter the new formatter
     */
    public void setFormatter(SimpleDateFormat formatter) {
        this.formatter = formatter;
        updateValue();
    }
    
    /**
     * The preferred width of the popup dialog for the picker.  This will only 
     * be used on devices where the popup width and height are configurable, such 
     * as the iPad or tablets.  On iPhone, the picker always spans the width of the 
     * screen along the bottom.
     * @param width The preferred width of the popup.
     */
    public void setPreferredPopupWidth(int width) {
        this.preferredPopupWidth = width;
    }
    
    /**
     * The preferred height of the popup dialog for the picker.  This will only 
     * be used on devices where the popup width and height are configurable, such 
     * as the iPad or tablets.  On iPhone, the picker always spans the width of the 
     * screen along the bottom.
     * @param height The preferred height of the popup.
     */
    public void setPreferredPopupHeight(int height) {
        this.preferredPopupHeight = height;
    }
    
    /**
     * The preferred width of the popup dialog. This will only 
     * be used on devices where the popup width and height are configurable, such 
     * as the iPad or tablets.  On iPhone, the picker always spans the width of the 
     * screen along the bottom. 
     * @return 
     */
    public int getPreferredPopupWidth() {
        return preferredPopupWidth;
    }
    
    /**
     * The preferred height of the popup dialog.  This will only 
     * be used on devices where the popup width and height are configurable, such 
     * as the iPad or tablets.  On iPhone, the picker always spans the width of the 
     * screen along the bottom.
     * @return 
     */
    public int getPreferredPopupHeight() {
        return preferredPopupHeight;
    }
    
    /**
     * {@inheritDoc}
     */
    public String[] getPropertyNames() {
        return new String[] {"Strings"};
    }

    /**
     * {@inheritDoc}
     */
    public Class[] getPropertyTypes() {
       return new Class[] { String[].class };
    }

    /**
     * {@inheritDoc}
     */
    public String[] getPropertyTypeNames() {
        return new String[] {"String []"};
    }

    /**
     * {@inheritDoc}
     */
    public Object getPropertyValue(String name) {
        if(name.equals("Strings")) {
            return getStrings();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String setPropertyValue(String name, Object value) {
        if(name.equals("Strings")) {
            setStrings((String[])value);
            return null;
        }
        return super.setPropertyValue(name, value);
    }

    /**
     * Returns the value which works for all picker types
     * @return the value object
     */
    public Object getValue() {
        return value;
    }

    private Label focusedOverlay;
    
    @Override
    public void paint(Graphics g) {
        super.paint(g); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public Style getStyle() {
        if(isEditing()) {
            return getSelectedStyle();
        }
        return super.getStyle(); 
    }
 
}
