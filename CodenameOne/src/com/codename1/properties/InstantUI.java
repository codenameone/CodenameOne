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

package com.codename1.properties;

import com.codename1.io.Log;
import com.codename1.ui.ButtonGroup;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.RadioButton;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.spinner.Picker;
import com.codename1.ui.table.TableLayout;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Instant UI generates a user interface for editing a property business object based on common
 * conventions and settings within the properties. UI's are automatically bound and work seamlessly.
 * <strong>Important</strong>: These UI's are subject to change, e.g. a generated UI might not have 
 * validation for a specific property in one build and might introduce it in an update. We try to generate
 * great UI's seamlessly and some improvements might break functionality.
 *
 * @author Shai Almog
 */
public class InstantUI {
    
    /**
     * Excludes the property from the generated UI
     * @param exclude the property to exclude
     */
    public void excludeProperty(PropertyBase exclude) {
        exclude.putClientProperty("cn1$excludeFromUI", Boolean.TRUE);
    }
    
    /**
     * Returns true if the property was excluded from the GUI
     * @param exclude the property
     * @return true if the property was excluded from the GUI
     */
    public boolean isExcludedProperty(PropertyBase exclude) {
        return exclude.getClientProperty("cn1$excludeFromUI") == Boolean.TRUE;
    }
    
    /**
     * A property that's a multi-choice can use this API to define the options used e.g.:
     * {@code 
        iui.setMultiChoiceLabels(c.gender, "Male", "Female", "Undefined");
        iui.setMultiChoiceValues(c.gender, "M", "F", "U");
     * }
     * 
     * @param p the property
     * @param labels label for each option
     */
    public void setMultiChoiceLabels(PropertyBase p, String... labels) {
        p.putClientProperty("cn1$multiChceLbl", labels);
        if(p.getClientProperty("cn1$multiChceVal") == null) {
            p.putClientProperty("cn1$multiChceVal", labels);
        }
    }

    /**
     * A property that's a multi-choice can use this API to define the options used, notice that
     * this API won't work correctly without {@link #setMultiChoiceLabels(com.codename1.properties.PropertyBase, java.lang.String...)}
     * 
     * @param p the property
     * @param values actual values used for each label
     */
    public void setMultiChoiceValues(PropertyBase p, Object... values) {
        p.putClientProperty("cn1$multiChceVal", values);
    }
    
    /**
     * The component class used to map this property
     * 
     * @param p the property
     * @param cmpCls class of the component e.g. {@code Button.class}
     */
    public void setComponentClass(PropertyBase p, Class cmpCls) {
        p.putClientProperty("cn1$cmpCls", cmpCls);        
    }

    /**
     * Sets the text field constraint for the property explicitly, notice that some constraints 
     * are implicit unless set manually e.g. numeric for numbers or password for fields with password 
     * in the name
     * @param p the property
     * @param cons the text field constraint
     */
    public void setTextFieldConstraint(PropertyBase p, int cons) {
        p.putClientProperty("cn1$tconstraint", cons);
    }
    
    /**
     * The text field constraint for the property. notice that some constraints 
     * are implicit unless set manually e.g. numeric for numbers or password for fields with password 
     * in the name
     * @param p the property
     * @return the constraint matching this property
     */
    public int getTextFieldConstraint(PropertyBase p) {
        Integer v = (Integer)p.getClientProperty("cn1$tconstraint");
        if(v != null) {
            return v;
        }
        
        Class t = p.getGenericType();
        if(t != null) {
            if(t == Integer.class || t == Long.class || t == Short.class || t == Byte.class) {
                return TextArea.NUMERIC;
            }
            if(t == Double.class || t == Float.class) {
                return TextArea.DECIMAL;
            }
        }
        String n = p.getName().toLowerCase();
        if(n.indexOf("password") > -1) {
            return TextArea.PASSWORD;
        }
        if(n.indexOf("url") > -1 || n.indexOf("website") > -1 || n.indexOf("blog") > -1) {
            return TextArea.URL;
        }
        if(n.indexOf("email") > -1) {
            return TextArea.EMAILADDR;
        }
        if(n.indexOf("phone") > -1 || n.indexOf("mobile") > -1) {
            return TextArea.PHONENUMBER;
        }
        return TextArea.ANY;
    }
    
    /**
     * Creates editing UI for the given business object
     * @param bo the business object
     * @param autoCommit true if the bindings used should be auto-committed
     * @return a UI container that can be used to edit the business object
     */
    public Container createEditUI(PropertyBusinessObject bo, boolean autoCommit) {
        Container cnt;
        if(Display.getInstance().isTablet()) {
            TableLayout tl = new TableLayout(1, 2);
            tl.setGrowHorizontally(true);
            cnt = new Container(tl);
        } else {
            cnt = new Container(BoxLayout.y());
        }
        UiBinding uib = new UiBinding();
        ArrayList<UiBinding.Binding> allBindings = new ArrayList<UiBinding.Binding>();
        for(PropertyBase b : bo.getPropertyIndex()) {
            if(isExcludedProperty(b)) {
                continue;
            }
            Class cls = (Class)b.getClientProperty("cn1$cmpCls");
            if(cls != null) {
                try {
                    Component cmp = (Component)cls.newInstance();
                    cmp.setName(b.getName());
                    cnt.add(b.getLabel()).
                            add(cmp);
                    allBindings.add(uib.bind(b, cmp));
                } catch(Exception err) {
                    Log.e(err);
                    throw new RuntimeException("Custom property instant UI failed for " + b.getName() + " " + err);
                }
                continue;
            }
            String[] multiLabels = (String[])b.getClientProperty("cn1$multiChceLbl");
            if(multiLabels != null) {
                // multi choice component
                final Object[] multiValues = (Object[])b.getClientProperty("cn1$multiChceVal");
                if(multiLabels.length < 5) {
                    // toggle buttons
                    ButtonGroup bg = new ButtonGroup();
                    RadioButton[] rbs = new RadioButton[multiLabels.length];
                    cnt.add(b.getLabel());
                    Container radioBox = new Container(new GridLayout(multiLabels.length));
                    for(int iter = 0 ; iter < multiLabels.length ; iter++) {
                        rbs[iter] = RadioButton.createToggle(multiLabels[iter], bg);
                        radioBox.add(rbs[iter]);
                    }
                    cnt.add(radioBox);
                    allBindings.add(uib.bindGroup(b, multiValues, rbs));
                } else {
                    Picker stringPicker = new Picker();
                    stringPicker.setStrings(multiLabels);
                    Map<Object, Object> m1 = new HashMap<Object, Object>();
                    Map<Object, Object> m2 = new HashMap<Object, Object>();
                    for(int iter = 0 ; iter < multiLabels.length ; iter++) {
                        m1.put(multiLabels[iter], multiValues[iter]);
                        m2.put(multiValues[iter], multiLabels[iter]);
                    }
                    cnt.add(b.getLabel()).
                            add(stringPicker);
                    allBindings.add(uib.bind(b, stringPicker, 
                            new UiBinding.PickerAdapter<Object>(
                                    new UiBinding.MappingConverter(m1), new UiBinding.MappingConverter(m2))));
                }
                continue;
            }
            Class t = b.getGenericType();
            if(t != null) {
                if(t == Boolean.class) {
                    CheckBox cb = new CheckBox();
                    uib.bind(b, cb);
                    cnt.add(b.getLabel()).
                            add(cb);
                    continue;
                }
                if(t == Date.class) {
                    Picker dp = new Picker();
                    dp.setType(Display.PICKER_TYPE_DATE);
                    uib.bind(b, dp);
                    cnt.add(b.getLabel()).
                            add(dp);
                    continue;
                }
            } 
            TextField tf = new TextField();
            tf.setConstraint(getTextFieldConstraint(b));
            uib.bind(b, tf);
            cnt.add(b.getLabel()).
                    add(tf);
        }
        
        cnt.putClientProperty("cn1$iui-binding", uib.createGroupBinding(allBindings));
        return cnt;
    }
    
    /**
     * Returns the Binding object for the given container which allows us control over the widgets
     * and their commit status
     * @param cnt the container returned by the {@link #createUI(boolean)} method
     * @return a binding object
     */
    public UiBinding.Binding getBindings(Container cnt) {
        return (UiBinding.Binding)cnt.getClientProperty("cn1$iui-binding");
    }
}
