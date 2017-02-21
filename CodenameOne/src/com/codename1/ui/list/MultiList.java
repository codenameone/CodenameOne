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
package com.codename1.ui.list;

import com.codename1.components.MultiButton;
import com.codename1.ui.Image;
import com.codename1.ui.List;
import java.util.Hashtable;

/**
 * <p>A list with a {@link com.codename1.components.MultiButton} renderer, <b>notice</b> that 
 * <a href="https://www.codenameone.com/blog/avoiding-lists.html">we strongly
 * discourage usage of lists</a>.. This simplifies the process of
 * working with the list and its model as the renderer comes pre-configured for common use cases.
 * </p>
 * 
 * <script src="https://gist.github.com/codenameone/b2ab6645db842d7b2750.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/graphics-urlimage-multilist.png" alt="MultiList and model in action" />
 *
 * @author Shai Almog
 */
public class MultiList extends List {
    private MultiButton sel;
    private MultiButton unsel;
    private Image placeholder;
    
    /**
     * Constructor that accepts a model for the list
     * @param model the model object to assign to the list
     */
    public MultiList(ListModel model) {
        super(model);
        sel = new MultiButton();
        unsel = new MultiButton();
    }
    
    /**
     * Constructor for the GUI builder
     */
    public MultiList() {
        super(new DefaultListModel(new Object[] {
            h("Entry 1", "more..."),
            h("Entry 2", "more..."),
            h("Entry 3", "more..."),
        }));
        sel = new MultiButton();
        unsel = new MultiButton();
    }

    /**
     * {@inheritDoc}
     */
    protected void initComponent() {
        super.initComponent();
        GenericListCellRenderer gn = new GenericListCellRenderer(sel, unsel);
        setRenderer(gn);
    }

    @Override
    protected void deinitialize() {
        super.deinitialize();
        ListCellRenderer gn = getRenderer();
        if(gn instanceof GenericListCellRenderer){
            ((GenericListCellRenderer)gn).deinitialize(this);
        }
    }
    
    
    
    private static Hashtable h(String fline, String sline) {
        Hashtable h = new Hashtable();
        h.put("Line1", fline);
        h.put("Line2", sline);
        return h;
    }
    
    /**
     * Allows developers to customize the properties of the selected multi-button in code
     * @return the selected multi button
     */
    public MultiButton getSelectedButton() {
        return sel;
    }

    /**
     * Allows developers to customize the properties of the unselected multi-button in code
     * @return the unselected multi button
     */
    public MultiButton getUnselectedButton() {
        return unsel;
    }

    /**
     * {@inheritDoc}
     */
    public String[] getPropertyNames() {
        return new String[] {
            "name1", "name2", "name3", "name4", 
            "uiid1", "uiid2", "uiid3", "uiid4", "iconName", "iconUiid", "iconPosition",
            "emblemName", "emblemUiid", "emblemPosition", "horizontalLayout", 
            "invertFirstTwoEntries", "checkBox", "radioButton", 
            "maskName", "Placeholder"
        };
    }

    /**
     * {@inheritDoc}
     */
    public Class[] getPropertyTypes() {
       return new Class[] {
           String.class,// name1
           String.class,// name2
           String.class,// name3
           String.class,// name4
           String.class,// uiid1
           String.class,// uiid2
           String.class,// uiid3
           String.class,// uiid4
           String.class,// iconName
           String.class,// iconUiid
           String.class,// iconPosition
           String.class,// emblemName
           String.class,// emblemUiid
           String.class,// emblemPosition
           Boolean.class,
           Boolean.class,
           Boolean.class,
           Boolean.class,
           String.class,
           Image.class // placeholder
       };
    }
    
    /**
     * {@inheritDoc}
     */
    public Object getPropertyValue(String name) {
        if(name.equals("placeholder")) {
            return placeholder;
        }
        return unsel.getPropertyValue(name);
    }
    
    /**
     * {@inheritDoc}
     */
    public String setPropertyValue(String name, Object value) {
        if(name.equals("placeholder")) {
            this.placeholder = (Image)value;
            if(unsel != null) {
                unsel.setIcon(placeholder);
                sel.setIcon(placeholder);
            }
            if(getRenderer() instanceof GenericListCellRenderer) {
                ((GenericListCellRenderer)getRenderer()).updateIconPlaceholders();
            }
            return null;
        }
        unsel.setPropertyValue(name, value);
        String v = sel.setPropertyValue(name, value);
        if(isInitialized()) {
            GenericListCellRenderer gn = new GenericListCellRenderer(sel, unsel);
            setRenderer(gn);
        }
        return v;
    }
}
