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

package com.codename1.components;

import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.Style;

/**
 * Wraps a text field so it will have an X to clear its content on the right hand side
 *
 * @author Shai Almog
 */
public class ClearableTextField extends Container {
    private ClearableTextField() {
        super(new BorderLayout());
    }
    
    /**
     * Wraps the given text field with a UI that will allow us to clear it
     * @param tf the text field
     * @return a Container that should be added to the UI instead of the actual text field
     */
    public static ClearableTextField wrap(final TextField tf) {
        ClearableTextField cf = new ClearableTextField();
        Button b = new Button("", tf.getUIID());
        FontImage.setMaterialIcon(b, FontImage.MATERIAL_CLEAR);
        removeCmpBackground(tf);
        removeCmpBackground(b);
        cf.setUIID(tf.getUIID());
        cf.add(BorderLayout.CENTER, tf);
        cf.add(BorderLayout.EAST, b);
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                tf.stopEditing();
                tf.setText("");
                tf.startEditingAsync();
            }
        });
        return cf;
    }
    
    private static void removeCmpBackground(Component cmp) {
        Style s = cmp.getAllStyles();
        s.setBorder(Border.createEmpty());
        s.setBackgroundType(Style.BACKGROUND_NONE);
        s.setBgTransparency(0);
    }
}
