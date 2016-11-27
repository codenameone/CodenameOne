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

import com.codename1.ui.Component;
import com.codename1.ui.TextArea;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

/**
 * The binding framework can implicitly bind UI elements to properties and provide common UI 
 * capabilities. 
 *
 * @author Shai Almog
 * @deprecated this API is experimental
 */
public class UiBinding {
    /**
     * Changes to the text area are automatically reflected to the given property and visa versa
     * @param prop the property value
     * @param ta the text area
     */
    public void bindString(Property<String, ? extends Object> prop, TextArea ta) {
        new Binder(prop, ta, String.class);
    }

    /**
     * Changes to the text area are automatically reflected to the given property and visa versa
     * @param prop the property value
     * @param ta the text area
     */
    public void bindInteger(Property<Integer, ? extends Object> prop, TextArea ta) {
        new Binder(prop, ta, Integer.class);
    }
    
    class Binder implements ActionListener<ActionEvent>, PropertyChangeListener {
        private boolean lock;
        private Property prop;
        private TextArea tcmp;
        private Class type;
        
        public Binder(Property prop, TextArea tcmp, Class type) {
            this.prop = prop;
            this.tcmp = tcmp;
            this.type = type;
            prop.addChangeListener(this);
            tcmp.addActionListener(this);
        }
        
        public void actionPerformed(ActionEvent evt) {
            if(!lock) {
                lock = true;
                if(tcmp != null) {
                    if(type == String.class) {
                        String text = tcmp.getText();
                        prop.set(text);
                    } else {
                        if(type == Integer.class) {
                            Integer inv = (Integer)prop.get();
                            int val = 0;
                            if(inv != null) {
                                val = inv;
                            }
                            prop.set(tcmp.getAsInt(val));
                        }
                    }
                }
                lock = false;
            }
        }

        public void propertyChanged(PropertyBase p) {
            if(!lock) {
                lock = true;
                if(tcmp != null) {
                    Object val = prop.get();
                    if(val != null) {
                        tcmp.setText(val.toString());
                    } else {
                        tcmp.setText("");
                    }
                }
                lock = false;
            }
        }
        
    }
}
