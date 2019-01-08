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

import com.codename1.ui.CheckBox;
import com.codename1.ui.Component;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.list.MultipleSelectionListModel;

/**
 * A list of checkboxes.
 * 
 * 
 * 
 * @author Steve Hannah
 * @since 6.0
 * @see ButtonList for code samples.
 */
public class CheckBoxList extends ButtonList {
    
    /**
     * Change listener added to individual checkboxes to keep them in sync with the model.
     */
    private final ActionListener changeListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent evt) {
            if (evt.getSource() instanceof CheckBox && contains((Component)evt.getSource())) {
                CheckBox src = (CheckBox)evt.getSource();
                int index = CheckBoxList.this.getComponentIndex(src);
                if (src.isSelected()) {
                    getMultiListModel().addSelectedIndices(index);
                } else {
                    getMultiListModel().removeSelectedIndices(index);
                }
            }
        }
        
    };
    
    public CheckBoxList(MultipleSelectionListModel model) {
        super(model);
        fireReady();
    }

    @Override
    public boolean isAllowMultipleSelection() {
        return true;
    }

    @Override
    protected Component createButton(Object model) {
        return new CheckBox(String.valueOf(model));
    }

    @Override
    protected void setSelected(Component button, boolean selected) {
        ((CheckBox)button).setSelected(selected);
    }
    
     @Override
    protected Component decorateComponent(Object modelItem, Component b) {
        b = super.decorateComponent(modelItem, b);
        ((CheckBox)b).addActionListener(this);
        ((CheckBox)b).addChangeListener(changeListener);
        return b;
    }

    @Override
    protected Component undecorateComponent(Component b) {
        ((CheckBox)b).removeActionListener(this);
        ((CheckBox)b).removeChangeListeners(changeListener);
        return super.undecorateComponent(b);
    }
    
}
