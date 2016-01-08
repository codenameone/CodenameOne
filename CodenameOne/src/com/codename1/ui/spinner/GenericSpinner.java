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

import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.list.DefaultListModel;
import com.codename1.ui.list.ListCellRenderer;
import com.codename1.ui.list.ListModel;

/**
 * A spinner class that allows arbitrary values, this is effectively a combo box replacement for platforms
 * where a combo box is not available
 *
 * @author Shai Almog
 */
public class GenericSpinner extends BaseSpinner {
    private Spinner[] spin;
    private ListModel[] model = new ListModel[] { new DefaultListModel(new Object[] {"Value 1", "Value 2", "Value 3"}) };
    private ListCellRenderer[] renderer = new ListCellRenderer[] { new SpinnerRenderer<Object>() };
    private Object[] value;
    private String[] renderingPrototype;
    
    /**
     * Default constructor
     */
    public GenericSpinner() {
        SpinnerRenderer<Object> render = (SpinnerRenderer<Object>) renderer[0];
        render.setShowNumbers(false);
        render.setUIID("SpinnerRenderer");
    }
    
    void initSpinner() {
        if(spin == null) {
            if(model.length == 1) {
                spin = new Spinner[] {createSpinner(0)};
                if(renderingPrototype != null) {
                    spin[0].setRenderingPrototype(renderingPrototype[0]);
                }
                setLayout(new BorderLayout());
                addComponent(BorderLayout.CENTER, spin[0]);
            } else {
                setLayout(new BoxLayout(BoxLayout.X_AXIS));
                spin = new Spinner[model.length];
                int slen = spin.length;
                for(int iter = 0 ; iter < slen ; iter++) {
                    spin[iter] = createSpinner(iter);
                    addComponent(spin[iter]);
                    if(iter < slen - 1) {
                        addComponent(createSeparator());
                    }
                    if(renderingPrototype != null) {
                        spin[iter].setRenderingPrototype(renderingPrototype[iter]);
                    }
                }
            }
        } 
    }

    /**
     * Sets the column count
     * 
     * @return the number of columns in the spinner
     */
    public void setColumns(int columns) {
        if(model.length != columns) {
            ListModel[] lm = new ListModel[columns];
            ListCellRenderer[] lr = new ListCellRenderer[columns];
            Object[] values = new Object[columns];
            String[] rp = new String[columns];
            if(spin != null) {
                spin = null;
                removeAll();
                initSpinner();
            }
            for(int iter = 0 ; iter < columns ; iter++) {
                lm[iter] = model[Math.min(iter, model.length - 1)];
                lr[iter] = renderer[Math.min(iter, model.length - 1)];
                if(value != null) {
                    values[iter] = value[Math.min(iter, model.length - 1)];
                }
                if(renderingPrototype != null) {
                    rp[iter] = renderingPrototype[Math.min(iter, model.length - 1)];
                }
            }
            model = lm;
            renderer = lr;
            value = values;
            renderingPrototype = rp;
        }
    }
    
    /**
     * Returns the rendering prototype
     * @return the prototype
     */
    public String getRenderingPrototype() {
        return getRenderingPrototype(0);
    }
    
    /**
     * Returns the rendering prototype
     * @return the prototype
     */
    public String getRenderingPrototype(int column) {
        if(renderingPrototype == null) {
            return null;
        }
        return renderingPrototype[column];
    }

    /**
     * The rendering prototype
     * @param pr the prototype
     */
    public void setRenderingPrototype(String pr) {
        setRenderingPrototype(0, pr);
    }
    
    /**
     * The rendering prototype
     * @param column the column
     * @param pr the prototype
     */
    public void setRenderingPrototype(int column, String pr) {
        if(renderingPrototype == null) {
            renderingPrototype = new String[model.length];
        }
        renderingPrototype[column] = pr;
    }

    /**
     * Return the column count
     * 
     * @return the number of columns in the spinner
     */
    public int getColumns() {
        return model.length;
    }
    
    /**
     * The value for the spinner
     * @return the value
     */
    public Object getValue() {
        return getValue(0);
    }
    
    /**
     * The value for the spinner
     * @return the value
     */
    public Object getValue(int offset) {
        if(spin != null && spin[offset] != null) {
            return spin[offset].getModel().getItemAt(spin[offset].getModel().getSelectedIndex());
        }
        if(model[offset] != null) {
            return model[offset].getItemAt(model[offset].getSelectedIndex());
        }
        return value;
    }

    /**
     * The value for the spinner
     * @param value the value to set
     */
    public void setValue(Object value) {
        setValue(0, value);
    }
    
    /**
     * The value for the spinner
     * @param value the value to set
     */
    public void setValue(int offset, Object value) {
        if(this.value == null) {
            this.value = new Object[model.length];
        }
        this.value[offset] = value;
        if(spin != null && spin[offset] != null) {
            spin[offset].setValue(value);
        } 
    }

    Spinner createSpinner(int column) {
        Spinner spin = new Spinner(model[column], renderer[column]);
        spin.setRenderingPrototype(null);
        spin.setShouldCalcPreferredSize(true);
        spin.setListSizeCalculationSampleCount(30);
        spin.initSpinnerRenderer();
        spin.updateToDefaultRTL();
        if(value != null && value[column] != null) {
            spin.setValue(value[column]);
        }
        return spin;
    }

    
    /**
     * @inheritDoc
     */
    public String[] getPropertyNames() {
        return new String[] {"model", "renderer", "items", "columns"};
    }

    /**
     * @inheritDoc
     */
    public Class[] getPropertyTypes() {
       return new Class[] {ListModel.class, ListCellRenderer.class, com.codename1.impl.CodenameOneImplementation.getStringArrayClass(), Integer.class};
    }

    /**
     * @inheritDoc
     */
    public String[] getPropertyTypeNames() {
        return new String[] {"ListModel", "ListCellRenderer", "String[]", "int"};
    }

    /**
     * @inheritDoc
     */
    public Object getPropertyValue(String name) {
        if(name.equals("items")) {
            ListModel m = getModel();
            String[] s = new String[m.getSize()];
            int slen = s.length;
            for(int iter = 0 ; iter < slen ; iter++) {
                Object o = m.getItemAt(iter);
                if(o != null) {
                    s[iter] = o.toString();
                }
            }
            return s;
        }
        if(name.equals("model")) {
            return getModel();
        }
        if(name.equals("renderer")) {
            return getRenderer();
        }
        if(name.equals("columns")) {
            return new Integer(getColumns());
        }
        return null;
    }

    /**
     * @inheritDoc
     */
    public String setPropertyValue(String name, Object value) {
        if(name.equals("items")) {
            setModel(new DefaultListModel((Object[])value));
            return null;
        }
        if(name.equals("model")) {
            setModel((ListModel)value);
            return null;
        }
        if(name.equals("renderer")) {
            setRenderer((ListCellRenderer)value);
            return null;
        }
        if(name.equals("columns")) {
            setColumns(((Integer)value).intValue());
            return null;
        }
        return super.setPropertyValue(name, value);
    }

    /**
     * @return the model
     */
    public ListModel getModel() {
        return model[0];
    }

    /**
     * @return the model
     */
    public ListModel getModel(int offset) {
        return model[offset];
    }

    /**
     * @param model the model to set
     */
    public void setModel(ListModel model) {
        setModel(0, model);
    }

    /**
     * @param model the model to set
     */
    public void setModel(int offset, ListModel model) {
        this.model[offset] = model;
        if(spin != null && spin[offset] != null) {
            spin[offset].setModel(model);
        }
    }

    /**
     * @return the renderer
     */
    public ListCellRenderer getRenderer(int offset) {
        return renderer[offset];
    }

    /**
     * @param renderer the renderer to set
     */
    public void setRenderer(int offset, ListCellRenderer renderer) {
        this.renderer[offset] = renderer;
        if(spin != null && spin[offset] != null) {
            spin[offset].setRenderer(renderer);
        }
    }

    /**
     * @return the renderer
     */
    public ListCellRenderer getRenderer() {
        return getRenderer(0);
    }

    /**
     * @param renderer the renderer to set
     */
    public void setRenderer(ListCellRenderer renderer) {
        setRenderer(0, renderer);
    }

    /**
     * Some components may optionally generate a state which can then be restored
     * using setCompnentState(). This method is used by the UIBuilder.
     * @return the component state or null for undefined state.
     */
    public Object getComponentState() {
        if(getColumns() == 1) {
            return getValue();
        } 
        Object[] o = new Object[getColumns()];
        int olen = o.length;
        for(int iter = 0 ; iter < olen ; iter++) {
            o[iter] = getValue(iter);
        }
        return o;
    }
    
    /**
     * If getComponentState returned a value the setter can update the value and restore
     * the prior state.
     * @param state the non-null state
     */
    public void setComponentState(Object state) {
        if(getColumns() == 1) {
            setValue(state);
            return;
        }
        Object[] o = (Object[])state;
        int olen = o.length;
        for(int iter = 0 ; iter < olen ; iter++) {
            setValue(iter, o[iter]);
        }
    }
}
