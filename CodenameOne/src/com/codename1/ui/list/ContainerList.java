/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui.list;

import com.codename1.ui.Container;
import com.codename1.ui.Graphics;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.events.SelectionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.util.EventDispatcher;
import java.util.Vector;

/**
 * This is a "list component" implemented as a container with a layout manager
 * which provides <b>some</b> of the ui advantages of a Container and some of a
 * list while pulling out some of the drawbacks of both.
 * This container uses the model/renderer approach for populating itself, adding/removing
 * entries will probably break it. It still provides most of the large size advantages
 * a list offers since the components within it are very simple and don't contain any
 * actual state other than layout information. The big advantage with this class is
 * the ability to leverage elaborate CodenameOne layouts such as Grid, Table &amp; flow layout
 * to provide other ways of rendering the content of a list model.
 *
 * @author Shai Almog
 */
public class ContainerList extends Container {
    private CellRenderer renderer = new DefaultListCellRenderer();
    private ListModel model;
    private Listeners listener;
    private EventDispatcher dispatcher = new EventDispatcher();

    /**
     * Default constructor
     */
    public ContainerList() {
        this(new DefaultListModel());
    }

    /**
     * Constructs a container list with the given model
     *
     * @param m the model
     */
    public ContainerList(ListModel m) {
        init(m);
    }

    private void init(ListModel m) {
        setModel(m);
        setUIID("ContainerList");
        setScrollableY(true);
    }

    /**
     * Constructs a container list with the given model and layout
     *
     * @param l layout manager
     * @param m the model
     */
    public ContainerList(Layout l, ListModel m) {
        super(l);
        init(m);
    }

    /**
     * The renderer used to draw the container list elements
     * 
     * @param r renderer instance
     */
    public void setRenderer(CellRenderer r) {
        renderer = r;
        repaint();
    }

    /**
     * The renderer used to draw the container list elements
     */
    public CellRenderer getRenderer() {
        return renderer;
    }

    private void updateComponentCount() {
        int cc = getComponentCount();
        int modelCount = model.getSize();
        if(cc != modelCount) {
            if(cc < modelCount) {
                for(int iter = cc ; iter < modelCount ; iter++) {
                    addComponent(new Entry(iter));
                }
            } else {
                while(getComponentCount() > modelCount) {
                    removeComponent(getComponentAt(getComponentCount() - 1));
                }
            }
            Form f = getComponentForm();
            if(f != null) {
                f.revalidate();
            }
        }
    }

    /**
     * Returns the list model
     * 
     * @return the list model
     */
    public ListModel getModel() {
        return model;
    }

    /**
     * Allows binding a listener to user selection actions
     *
     * @param l the action listener to be added
     */
    public void addActionListener(ActionListener l) {
        dispatcher.addListener(l);
    }

    /**
     * This method allows extracting the action listeners from the current list
     *
     * @return vector containing the action listeners on the list
     */
    public Vector getActionListeners() {
        return dispatcher.getListenerVector();
    }

    /**
     * Allows binding a listener to user selection actions
     *
     * @param l the action listener to be removed
     */
    public void removeActionListener(ActionListener l) {
        dispatcher.removeListener(l);
    }

    /**
     * @inheritDoc
     */
    protected void initComponent() {
        if(model != null) {
            int i = model.getSelectedIndex();
            if(i > 0) {
                getComponentAt(i).requestFocus();
            }
            bindListeners();
        }
    }
    
    /**
     * @inheritDoc
     */
    protected void deinitialize() {
        if (this.model != null && listener != null) {
            this.model.removeDataChangedListener(listener);
            this.model.removeSelectionListener(listener);
            listener = null;
        }
    }
    
    /**
     * Set the model for the container list
     *
     * @param model a model class that is mapped into the internal components
     */
    public void setModel(ListModel model) {
        if (this.model != null && listener != null) {
            this.model.removeDataChangedListener(listener);
            this.model.removeSelectionListener(listener);
            listener = null;
        }
        this.model = model;
        updateComponentCount();
        if(model.getSelectedIndex() > 0) {
            getComponentAt(model.getSelectedIndex()).requestFocus();
        }
        if (isInitialized()) {
            bindListeners();
        }
        repaint();
    }

    private void bindListeners() {
        if (listener == null) {
            listener = new Listeners();
            model.addDataChangedListener(listener);
            model.addSelectionListener(listener);
        }
    }

    /**
     * Returns the current/last selected item
     * @return selected item or null
     */
    public Object getSelectedItem() {
        int i = model.getSelectedIndex();
        if(i > -1) {
            return model.getItemAt(i);
        }
        return null;
    }


    /**
     * Returns the current selected offset in the list
     *
     * @return the current selected offset in the list
     */
    public int getSelectedIndex() {
        return model.getSelectedIndex();
    }

    /**
     * Sets the current selected offset in the list, by default this implementation
     * will scroll the list to the selection if the selection is outside of the screen
     *
     * @param index the current selected offset in the list
     */
    public void setSelectedIndex(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("Selection index is negative:" + index);
        }        
        getComponentAt(index).requestFocus();
        model.setSelectedIndex(index);
    }
    
    
    /**
     * @inheritDoc
     */
    public String[] getPropertyNames() {
        return new String[] {"ListItems", "Renderer"};
    }

    /**
     * @inheritDoc
     */
    public Class[] getPropertyTypes() {
       return new Class[] {com.codename1.impl.CodenameOneImplementation.getObjectArrayClass(), CellRenderer.class};
    }

    /**
     * @inheritDoc
     */
    public Object getPropertyValue(String name) {
        if(name.equals("ListItems")) {
            Object[] obj = new Object[model.getSize()];
            for(int iter = 0 ; iter < obj.length ; iter++) {
                obj[iter] = model.getItemAt(iter);
            }
            return obj;
        }
        if(name.equals("Renderer")) {
            return getRenderer();
        }
        return null;
    }

    /**
     * @inheritDoc
     */
    public String setPropertyValue(String name, Object value) {
        if(name.equals("ListItems")) {
            setModel(new DefaultListModel((Object[])value));
            return null;
        }
        if(name.equals("Renderer")) {
            setRenderer((CellRenderer)value);
            return null;
        }
        return super.setPropertyValue(name, value);
    }


    /**
     * This class is an internal implementation detail
     */
    class Entry extends Component {
        private int offset;

        Entry(int off) {
            setFocusable(true);
            offset = off;
        }

        public void initComponent() {
            offset = getParent().getComponentIndex(this);
        }

        protected void focusGained() {
            model.setSelectedIndex(offset);
        }

        public void paintBackground(Graphics g) {
        }
        
        public void paintBorder(Graphics g) {
        }

        public void paint(Graphics g) {
            Component cmp = renderer.getCellRendererComponent(ContainerList.this, model, model.getItemAt(offset), offset, hasFocus());
            cmp.setX(getX());
            cmp.setY(getY());
            cmp.setWidth(getWidth());
            cmp.setHeight(getHeight());
            if(cmp instanceof Container) {
                ((Container)cmp).revalidate();
            }
            cmp.paintComponent(g);
        }

        public void longPointerPress(int x, int y) {
            super.longPointerPress(x, y);
            pointerReleasedImpl(x, y, true);
        }

        public void pointerReleased(int x, int y) {
            super.pointerReleased(x, y);
            pointerReleasedImpl(x, y, false);
        }

        
        /**
         * @inheritDoc
         */
        public void pointerReleasedImpl(int x, int y, boolean longPress) {
            if (!isDragActivated()) {
                // fire the action event into the selected component
                Component cmp = renderer.getCellRendererComponent(ContainerList.this, model, model.getItemAt(offset), offset, hasFocus());
                if(cmp instanceof Container) {
                    int absX = getAbsoluteX();
                    int absY = getAbsoluteY();
                    int newX = x - absX + cmp.getX();
                    int newY = y - absY + cmp.getY();
                    Component selectionCmp = ((Container) cmp).getComponentAt(newX, newY);
                    if(selectionCmp != null) {
                        selectionCmp.setX(0);
                        selectionCmp.setY(0);
                        if(longPress){
                            selectionCmp.longPointerPress(newX, newY);
                        }else{
                            selectionCmp.pointerPressed(newX, newY);
                            selectionCmp.pointerReleased(newX, newY);
                        }
                    }
                }

                dispatcher.fireActionEvent(new ActionEvent(ContainerList.this, x, y));
            }
        }
        
        /**
         * @inheritDoc
         */
        public void keyReleased(int keyCode) {
            super.keyReleased(keyCode);
            if(Display.getInstance().getGameAction(keyCode) == Display.GAME_FIRE) {
                dispatcher.fireActionEvent(new ActionEvent(ContainerList.this, keyCode));
            }
        }
        
        public Dimension calcPreferredSize() {
            Component c = renderer.getCellRendererComponent(ContainerList.this, model, model.getItemAt(offset), offset, hasFocus());
            if(getWidth() <= 0) {
                c.setWidth(Display.getInstance().getDisplayWidth());
                c.setHeight(Display.getInstance().getDisplayHeight());
            } else {
                c.setWidth(getWidth());
                c.setHeight(getHeight());
            }
            if(c instanceof Container) {
                ((Container)c).revalidate();
            }
            Dimension d = c.getPreferredSize();
            return d;
        }
    }

    private class Listeners implements DataChangedListener, SelectionListener {

        public void dataChanged(int status, int index) {
            updateComponentCount();
        }

        public void selectionChanged(int oldSelected, int newSelected) {
            getComponentAt(newSelected).requestFocus();
        }
    }
}
