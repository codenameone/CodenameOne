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

package com.codename1.ui.layouts;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.InputComponent;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.table.TableLayout;
import java.util.ArrayList;

/**
 * <p>This is a special case layout specifically designed for {@link com.codename1.ui.InputComponent}. 
 * When the  on top mode of text layout is used this layout acts exactly like a table layout and uses the 
 * given constraints. When this mode is false it uses a regular box Y layout mode and orders the elements one 
 * on top of the other.</p>
 * <p>One important difference between this layout and the default table layout is that the vertical alignment 
 * here is set to {@code TOP} so the error label below doesn't break component alignment if two components
 * are on the same row and only one has an error message.
 * </p> 
  * <p>
 * The following code demonstrates a simple set of inputs and validation as it appears in iOS, Android and with 
 * validation errors
 * </p>
 * <script src="https://gist.github.com/codenameone/5a28c7944aeab7d8ae6b26dc81690238.js"></script>
 * <img src="https://www.codenameone.com/img/blog/pixel-perfect-text-field-picker-ios.png" alt="Running on iOS" />
 * <img src="https://www.codenameone.com/img/blog/pixel-perfect-text-field-picker-android.png" alt="Running on Android" />
 * <img src="https://www.codenameone.com/img/blog/pixel-perfect-text-field-error-handling-blank.png" alt="Android validation errors" />
*
 *
 * @author Shai Almog
 */
public class TextModeLayout extends Layout {
    /**
     * The underlying table layout can be used freely to create constraints on the fly
     */
    public final TableLayout table;
    private Layout actual;
    
    /**
     * Automatically invokes the {@link com.codename1.ui.InputComponent#group(com.codename1.ui.Component...)} 
     * method on the text components in a BoxY layout scenario
     */
    private boolean autoGrouping = true;
    
    private int lastComponentCount = 0;
    
    /**
     * The constructor works like the standard table layout constructor and will behave as such with the on 
     * top mode
     * @param rows the number of rows
     * @param columns the number of columns;
     */
    public TextModeLayout(int rows, int columns) {
        table = new TableLayout(rows, columns);    
        table.setGrowHorizontally(true);
        if(!UIManager.getInstance().isThemeConstant("textComponentOnTopBool", false)) {
            actual = BoxLayout.y();
        } else {
            actual = table;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addLayoutComponent(Object value, Component comp, Container c) {
        if(actual == table) {
            // forcing default constraint to still be aligned top
            if(!(value instanceof TableLayout.Constraint)) {
                value = createConstraint();
            }
            table.addLayoutComponent(value, comp, c);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object cloneConstraint(Object constraint) {
        return actual.cloneConstraint(constraint);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getComponentConstraint(Component comp) {
        return actual.getComponentConstraint(comp);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isConstraintTracking() {
        return actual.isConstraintTracking();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOverlapSupported() {
        return actual.isOverlapSupported();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean obscuresPotential(Container parent) {
        return actual.obscuresPotential(parent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeLayoutComponent(Component comp) {
        actual.removeLayoutComponent(comp);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void layoutContainer(Container parent) {
        if(autoGrouping && actual != table && lastComponentCount != parent.getComponentCount()) {
            lastComponentCount = parent.getComponentCount();
            ArrayList<Component> tc = new ArrayList<Component>();
            for(Component c : parent) {
                if(c instanceof InputComponent) {
                    tc.add(c);
                }
            }
            if(tc.size() > 0) {
                Component[] tcArr = new Component[tc.size()];
                tc.toArray(tcArr);
                InputComponent.group(tcArr);
            }
        } 
        actual.layoutContainer(parent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Dimension getPreferredSize(Container parent) {
        return actual.getPreferredSize(parent);
    }

    /**
     * Creates a new Constraint instance to add to the layout
     *
     * @return the default constraint
     */
    public TableLayout.Constraint createConstraint() {
        return table.createConstraint().verticalAlign(Component.TOP);
    }

    /**
     * Creates a new Constraint instance to add to the layout
     *
     * @param row the row for the table starting with 0
     * @param column the column for the table starting with 0
     * @return the new constraint
     */
    public TableLayout.Constraint createConstraint(int row, int column) {
        return table.createConstraint(row, column).verticalAlign(Component.TOP);
    }

    /**
     * Automatically invokes the {@link com.codename1.ui.InputComponent#group(com.codename1.ui.Component...)}
     * method on the text components in a BoxY layout scenario
     * @return the autoGrouping
     */
    public boolean isAutoGrouping() {
        return autoGrouping;
    }

    /**
     * Automatically invokes the {@link com.codename1.ui.InputComponent#group(com.codename1.ui.Component...)}
     * method on the text components in a BoxY layout scenario
     * @param autoGrouping the autoGrouping to set
     */
    public void setAutoGrouping(boolean autoGrouping) {
        this.autoGrouping = autoGrouping;
    }
}
