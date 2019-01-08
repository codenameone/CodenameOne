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

import com.codename1.ui.Component;
import com.codename1.ui.RadioButton;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.list.ListModel;

/**
 * A list of Radio buttons that can be managed as a single component.
 * @author Steve Hannah
 * @since 6.0
 * @see ButtonList for code samples
 */
public class RadioButtonList extends ButtonList {
    
    /**
     * Change listener added to individual radio buttons to keep them in sync with the model.
     */
    private final ActionListener changeListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent evt) {
            if (evt.getSource() instanceof RadioButton && contains((Component)evt.getSource())) {
                RadioButton src = (RadioButton)evt.getSource();
                int index = RadioButtonList.this.getComponentIndex(src);
                if (src.isSelected()) {
                    getModel().setSelectedIndex(index);
                }
            }
        }
        
    };
    
    /**
     * Creates a new RadioButton list with the given model of options.  It will result in
     * one RadioButton per item in the model.
     * @param model The model that defines the options that the user can choose between.
     */
    public RadioButtonList(ListModel model) {
        super(model);
        fireReady();
    }

    /**
     * Returns false for RadioButtonList since only one radio button can be selected at a time.
     * @return 
     */
    @Override
    public boolean isAllowMultipleSelection() {
        return false;
    }

    
    @Override
    protected Component createButton(Object model) {
        return new RadioButton(String.valueOf(model));
    }

    @Override
    protected void setSelected(Component button, boolean selected) {
        ((RadioButton)button).setSelected(selected);
    }

    @Override
    protected Component decorateComponent(Object modelItem, Component b) {
        b = super.decorateComponent(modelItem, b);
        ((RadioButton)b).addActionListener(this);
        ((RadioButton)b).addChangeListener(changeListener);
        return b;
    }

    @Override
    protected Component undecorateComponent(Component b) {
        ((RadioButton)b).removeActionListener(this);
        ((RadioButton)b).removeChangeListeners(changeListener);
        return super.undecorateComponent(b);
    }
    
    
    
}
