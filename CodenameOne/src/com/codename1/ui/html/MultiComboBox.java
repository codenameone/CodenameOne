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
package com.codename1.ui.html;

import com.codename1.ui.ComboBox;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;
import com.codename1.ui.List;
import com.codename1.ui.list.DefaultListCellRenderer;
import com.codename1.ui.list.DefaultListModel;
import com.codename1.ui.list.ListCellRenderer;
import com.codename1.ui.list.ListModel;
import java.util.Vector;

/**
 * A ComboBox control that allows multiple selection and supports OPTGROUP labels.
 * Note that this does not inherit from ComboBox but rather from List, since basically like an HTML multiple ComboBox we show an open List 
 * The List/Combo size is controlled from the outside in HTMLComponent using a wrapping Container.
 *
 * This class is also used by HTMLComboBox as the list popup when opening the HTMLComboBox (To allow OPTGROUP support)
 *
 * @author Ofir Leitner
 */
class MultiComboBox extends List {

    private boolean multiple; // true if this is a multiple choice combo
    private MultiListModel model; 

    MultiComboBox(boolean multiple) {
        this(null,multiple);
    }


    MultiComboBox(ListModel underlyingModel, boolean multiple) {
        super();
        setUIID("ComboBox");
        this.multiple=multiple;
        setScrollToSelected(!multiple); // In multiple comboboxes we don't scroll to selected, as there can be several
        model=new MultiListModel(underlyingModel,multiple);
        setModel(model);
        MultiCellRenderer multiRenderer=new MultiCellRenderer(model);
        setRenderer(multiRenderer);
        if (underlyingModel!=null) { // Need to scan the existing data since there will be no "addItem" with an optgroup item
            for(int i=0;i<underlyingModel.getSize();i++) {
                if (underlyingModel.getItemAt(i) instanceof String) {
                    multiRenderer.setOptgroup(true);
                    break;
                }
            }
        }

        ListCellRenderer r = getRenderer();
        if(r instanceof Component) {
            Component c = (Component) getRenderer();
            c.setUIID("ComboBoxItem");
            c.getSelectedStyle().setPadding(1, 1, 1, 1);
            c.getUnselectedStyle().setPadding(1, 1, 1, 1);
        }
        Component c = getRenderer().getListFocusComponent(this);
        if(c != null){
            c.setUIID("ComboBoxFocus");
        }
    }

    Vector getSelected() {
        if (multiple) {
            if (model!=null) {
                return model.selected;
            }
        } else if (getSelectedItem()!=null) {
            Vector v=new Vector();
            v.addElement(getSelectedItem());
            return v;
        }
        return null;
    }


    // Overiding List's methods to provide for multiple selection and OPTGROUP support:

    /**
     * {{@inheritDoc}}
     */
    public void addItem(Object item) {
        super.addItem(item);
        if (item instanceof String) {
            ((MultiCellRenderer)getRenderer()).setOptgroup(true);
        }
    }

    /**
     * {{@inheritDoc}}
     */
    public void setSelectedItem(Object item) {
        super.setSelectedItem(item);
        model.toggleSelected(item);
    }

    /**
     * {{@inheritDoc}}
     */
    protected void fireActionEvent() {
        if (multiple) {
            Object obj = getSelectedItem();
            model.toggleSelected(obj);
        } else {
            super.fireActionEvent();
        }

    }

    /**
     * {{@inheritDoc}}
     */
    public void keyReleased(int keyCode) {
        // other events are in keyReleased to prevent the next event from reaching the next form

        int gameAction = Display.getInstance().getGameAction(keyCode);
        if ((multiple) && (gameAction == Display.GAME_FIRE)) {
            boolean h = handlesInput();
            //setHandlesInput(!h);
            if (h) {
                fireActionEvent();
            }
            repaint();
            return;
        } else {
            super.keyReleased(keyCode);
        }
    }

    /**
     * {{@inheritDoc}}
     */
    protected void fireClicked() {
        boolean h = handlesInput();
        if (!multiple) {
            setHandlesInput(!h);
        }
        if (h) {
            fireActionEvent();
        }
        repaint();
    }

    /**
     * {@inheritDoc}
     */
    public void keyPressed(int keyCode) {
        // scrolling events are in keyPressed to provide immediate feedback
        if (!handlesInput()) {
            return;
        }

        int gameAction = Display.getInstance().getGameAction(keyCode);
        int keyFwd;
        int keyBck;
        if (getOrientation() != HORIZONTAL) {
            keyFwd = Display.GAME_DOWN;
            keyBck = Display.GAME_UP;
            if (gameAction == Display.GAME_LEFT || gameAction == Display.GAME_RIGHT) {
                setHandlesInput(false);
            }
        } else {
            if (isRTL()) {
                keyFwd = Display.GAME_LEFT;
                keyBck = Display.GAME_RIGHT;
            } else {
                keyFwd = Display.GAME_RIGHT;
                keyBck = Display.GAME_LEFT;
            }
            if (gameAction == Display.GAME_DOWN || gameAction == Display.GAME_UP) {
                setHandlesInput(false);
            }
        }

        int selectedIndex = model.getSelectedIndex();
        if (gameAction == keyBck) {
            if (selectedIndex>0) {
                if (model.getItemAt(selectedIndex-1) instanceof String) {
                    if (selectedIndex==1) { // First item is an optgroup
                        return;
                    }
                    model.setDirection(-1);
                    selectedIndex--;
                    model.setSelectedIndex(selectedIndex);
                }
            }
        } else if (gameAction == keyFwd) {
            if (selectedIndex<size()-1) {
                if (model.getItemAt(selectedIndex+1) instanceof String) {
                    if (selectedIndex==size()-2) { //Last item is an optgroup
                        return;
                    }
                    model.setDirection(1);
                    selectedIndex++;
                    model.setSelectedIndex(selectedIndex);
                }
            }
        }
        super.keyPressed(keyCode);
        model.setDirection(0);
    }

    // Inner classes:

    /**
     * A model that knows to handle both multiple selection and OPTGORUP labels
     *
     * @author Ofir Leitner
     */
    class MultiListModel extends DefaultListModel {

        Vector selected = new Vector();
        int direction;
        boolean multiple;
        ListModel underlyingModel;

        public MultiListModel(ListModel underlyingModel,boolean multiple) {
            this.multiple=multiple;
            this.underlyingModel=underlyingModel;
        }

        public Object getItemAt(int index) {
            if (underlyingModel!=null) {
                return underlyingModel.getItemAt(index);
            } else {
                return super.getItemAt(index);
            }
        }

        public int getSize() {
            if (underlyingModel!=null) {
                return underlyingModel.getSize();
            } else {
                return super.getSize();
            }
        }

        public int getSelectedIndex() {
            if (underlyingModel!=null) {
                return underlyingModel.getSelectedIndex();
            } else {
                return super.getSelectedIndex();
            }
        }

        void setDirection(int direction) {
            this.direction=direction;
        }

        public void setSelectedIndex(int index) {
            if (getItemAt(index) instanceof String) { //don't select optgroup
                if (direction==0) {
                    toggleSelected(getItemAt(getSelectedIndex())); //Since the incorrect item is toggled later, we toggle it here
                    return;
                }
            }
            if (underlyingModel!=null) {
                underlyingModel.setSelectedIndex(index);
            } else {
                super.setSelectedIndex(index);
            }
        }

        void toggleSelected(Object item) {
            if (multiple) {
                if (selected.contains(item)) {
                    selected.removeElement(item);
                } else {
                    selected.addElement(item);
                }
            }
        }

        boolean isSelected(Object item) {
            return ((multiple) && (selected.contains(item)));
        }

    }
    /**
     * A renderer that knows to handle both multiple selection and OPTGORUP labels
     *
     * @author Ofir Leitner
     */
    class MultiCellRenderer extends DefaultListCellRenderer {

        private MultiListModel model;
        private boolean optgroup;
        private int bgColor=-1;
        private int fgColor=-1;

        MultiCellRenderer(MultiListModel model) {
            super(false);
            this.model=model;
        }

        void setOptgroup(boolean optgroup) {
            this.optgroup=optgroup;
        }

        public Component getListCellRendererComponent(List list, Object value, int index, boolean isSelected) {
           Component cmp=super.getListCellRendererComponent(list, value, index, isSelected);
           if (model.isSelected(value)) {
               setUIID("HTMLMultiComboBoxItem");
               fgColor=getUnselectedStyle().getFgColor();
               bgColor=getUnselectedStyle().getBgColor();
           } else {
               setUIID("ComboBoxItem");
               bgColor=-1;
               fgColor=-1;
           }

           if (optgroup) {
               if (value instanceof String) {
                   setUIID("HTMLOptgroup");
               } else {
                   setUIID("HTMLOptgroupItem");
               }
           }

           if (fgColor!=-1) {
                getSelectedStyle().setFgColor(fgColor);
                getUnselectedStyle().setFgColor(fgColor);
           }

           return cmp;

        }

        public void paint(Graphics g) {
            if (hasFocus()) {
                g.setColor(getListFocusComponent(null).getSelectedStyle().getBgColor());
                g.fillRect(getX(), getY(), getWidth(), getHeight());

            }
            if (bgColor!=-1) {
                g.setColor(bgColor);
                g.fillRect(getX()+getStyle().getPadding(Component.LEFT), getY()+getStyle().getPadding(Component.TOP),
                        getWidth()-+getStyle().getPadding(Component.LEFT)-+getStyle().getPadding(Component.RIGHT),
                        getHeight()-+getStyle().getPadding(Component.TOP)-+getStyle().getPadding(Component.BOTTOM));
            }
            super.paint(g);
        }

    }

}
