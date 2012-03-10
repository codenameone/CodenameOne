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

import com.codename1.ui.CheckBox;
import com.codename1.ui.Button;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.List;
import com.codename1.ui.RadioButton;
import com.codename1.ui.TextArea;
import com.codename1.ui.animations.Animation;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.plaf.UIManager;
import java.util.Hashtable;
import java.util.Vector;

/**
 * The generic list cell renderer can display containers or arbitrary Codename One components
 * as items in a list. It generally relies on the source data being either a hashtable or
 * a list of Strings. It extracts values from the hashtable using the component name as
 * an indication to the hashtable key lookup.
 * This renderer supports label tickering, check boxes/radio buttons etc. seamlessly.
 * Please notice that you must use at least two distinguished instances of the component
 * to render, reusing the same instance WILL NOT WORK.
 * Also the renderer instance cannot be reused for multiple lists, each list will need
 * a new instance of this renderer!
 *
 * @author Shai Almog
 */
public class GenericListCellRenderer implements ListCellRenderer, CellRenderer {
    private Button lastClickedComponent;
    private Vector pendingAnimations;

    /**
     * If this flag exists in a hashtable of data the renderer will enable/disable
     * the entries, the flag assumes either Boolean.TRUE or Boolean.FALSE. 
     * Notice that just setting it to false when necessary will not work, when its
     * used it must be applied to all entries otherwise the reuse of the renderer
     * component will break this feature.
     */
    public static final String ENABLED = "$$ENABLED$$";

    /**
     * Put this flag as a hashtable key to indicate that a checkbox entry rendered by
     * this renderer should act as a "select all" entry and toggle all other entries.
     * The value for this entry is ignored
     */
    public static final String SELECT_ALL_FLAG = "$$SELECTALL$$";

    private Label focusComponent = new Label();
    private Component selected;
    private Component unselected;

    private Component[] selectedEntries;
    private Component[] unselectedEntries;
    private Component selectedEven;
    private Component unselectedEven;
    private Component[] selectedEntriesEven;
    private Component[] unselectedEntriesEven;

    private Monitor mon = new Monitor();
    private Component parentList;
    private boolean selectionListener = true;
    private boolean firstCharacterRTL;
    private boolean fisheye;


    /**
     * Constructs a generic renderer with the given selected/unselected components
     *
     * @param selected indicates the selected value for the renderer
     * @param unselected indicates the unselected value for the renderer
     */
    public GenericListCellRenderer(Component selected, Component unselected) {
        if(selected == unselected) {
            throw new IllegalArgumentException("Must use distinct instances for renderer!");
        }
        this.selected = selected;
        this.unselected = unselected;
        focusComponent.setUIID(selected.getUIID() + "Focus");
        focusComponent.setFocus(true);

        selectedEntries = initRenderer(selected);
        unselectedEntries = initRenderer(unselected);
        firstCharacterRTL = selected.getUIManager().isThemeConstant("firstCharRTLBool", false);
        addSelectedEntriesListener(selectedEntries);
    }

    
    private void addSelectedEntriesListener(Component[] e) {
        for(int iter = 0 ; iter < e.length ; iter++) {
            if(e[iter] instanceof Button) {
                ((Button)e[iter]).addActionListener(mon);
            }
        }
    }

    private Component[] initRenderer(Component r) {
        r.setCellRenderer(true);
        if(r instanceof Container) {
            Vector selectedVector = new Vector();
            findComponentsOfInterest(r, selectedVector);
            return vectorToComponentArray(selectedVector);
        } else {
            return new Component[] {r};
        }
    }

    /**
     * Allows partitioning the renderer into "areas" that can be clicked. When 
     * receiving an action event in the list this method allows a developer to
     * query the renderer to "see" whether a button within the component was "touched"
     * by the user on a touch screen device. 
     * This method will reset the value to null after returning a none-null value!
     * 
     * @return a button or null
     */
    public Button extractLastClickedComponent() {
        Button c = lastClickedComponent;
        lastClickedComponent = null;
        return c;
    }

    /**
     * Constructs a generic renderer with the given selected/unselected components for
     * odd/even values allowing a "pinstripe" effect
     *
     * @param odd indicates the selected value for the renderer
     * @param oddUnselected indicates the unselected value for the renderer
     * @param even indicates the selected value for the renderer
     * @param evenUnselected  indicates the unselected value for the renderer
     */
    public GenericListCellRenderer(Component odd, Component oddUnselected, Component even, Component evenUnselected) {
        this(odd, oddUnselected);
        selectedEven = even;
        unselectedEven = evenUnselected;
        selectedEntriesEven = initRenderer(even);
        unselectedEntriesEven = initRenderer(evenUnselected);
        addSelectedEntriesListener(selectedEntriesEven);
    }

    private Component[] vectorToComponentArray(Vector v) {
        Component[] result = new Component[v.size()];
        for(int iter = 0 ; iter < result.length ; iter++) {
            result[iter] = (Component)v.elementAt(iter);
        }
        return result;
    }

    private void findComponentsOfInterest(Component cmp, Vector dest) {
        if(cmp instanceof Container) {
            Container c = (Container)cmp;
            int count = c.getComponentCount();
            for(int iter = 0 ; iter < count ; iter++) {
                findComponentsOfInterest(c.getComponentAt(iter), dest);
            }
            return;
        }
        if((cmp instanceof Label || cmp instanceof TextArea) && cmp.getName() != null) {
            dest.addElement(cmp);
            return;
        }
    }

    /**
     * @inheritDoc
     */
    public Component getCellRendererComponent(Component list, Object model, Object value, int index, boolean isSelected) {
        Component cmp;
        Component[] entries;
        if(!fisheye && !Display.getInstance().shouldRenderSelection(list)) {
            isSelected = false;
        }
        if(isSelected && (fisheye || list.hasFocus())) {
            cmp = selected;
            entries = selectedEntries;
            if(selectedEven != null && index % 2 == 0) {
                cmp = selectedEven;
                entries = selectedEntriesEven;

                // prevent the list from over-optimizing the background painting
                if(list instanceof List) {
                    ((List)list).setMutableRendererBackgrounds(true);
                }
            }
            if(value instanceof Hashtable) {
                Hashtable h = (Hashtable)value;
                Boolean enabled = (Boolean)h.get(ENABLED);
                if(enabled != null) {
                    cmp.setEnabled(enabled.booleanValue());
                }
                for(int iter = 0 ; iter < entries.length ; iter++) {
                    String currentName = entries[iter].getName();

                    Object val;
                    if(currentName.equals("$number")) {
                        val = "" + (index + 1);
                    } else {
                        // a selected entry might differ in its value to allow for
                        // behavior such as rollover images
                        val = h.get("#" + currentName);
                        if(val == null) {
                            val = h.get(currentName);
                        }
                    }
                    setComponentValueWithTickering(entries[iter], val, list);
                    entries[iter].setFocus(entries[iter].isFocusable());
                }
            } else {
                setComponentValueWithTickering(entries[0], value, list);
                entries[0].setFocus(entries[0].isFocusable());
            }
            return cmp;
        } else {
            cmp = unselected;
            entries = unselectedEntries;
            if(unselectedEven != null && index % 2 == 0) {
                cmp = unselectedEven;
                entries = unselectedEntriesEven;

                // prevent the list from over-optimizing the background painting
                if(list instanceof List) {
                    ((List)list).setMutableRendererBackgrounds(true);
                }
            }
            if(value instanceof Hashtable) {
                Hashtable h = (Hashtable)value;
                Boolean enabled = (Boolean)h.get(ENABLED);
                if(enabled != null) {
                    cmp.setEnabled(enabled.booleanValue());
                }
                for(int iter = 0 ; iter < entries.length ; iter++) {
                    String currentName = entries[iter].getName();
                    if(currentName.equals("$number")) {
                        setComponentValue(entries[iter], "" + (index + 1));
                        continue;
                    }
                    setComponentValue(entries[iter], h.get(currentName));
                }
            } else {
                setComponentValue(entries[0], value);
            }
            return cmp;
        }
    }


    /**
     * @inheritDoc
     */
    public Component getListCellRendererComponent(List list, Object value, int index, boolean isSelected) {
        return getCellRendererComponent(list, list.getModel(), value, index, isSelected);
    }


    private boolean isSelectedValue(Object v) {
        return v != null && "true".equalsIgnoreCase(v.toString());
    }

    private void setComponentValueWithTickering(Component cmp, Object value, Component l) {
        setComponentValue(cmp, value);
        if(cmp instanceof Label) {
            if(selectionListener) {
                if(l instanceof List) {
                    ((List)l).addActionListener(mon);
                }
                parentList = l;
            }
            Label label = (Label)cmp;
            if(label.shouldTickerStart() && Display.getInstance().shouldRenderSelection()) {
                if(!label.isTickerRunning()) {
                    parentList = l;
                    if(parentList != null) {
                        Form f = parentList.getComponentForm();
                        if(f != null) {
                            f.registerAnimated(mon);
                            label.startTicker(cmp.getUIManager().getLookAndFeel().getTickerSpeed(), true);
                        }
                    }
                }
            } else {
                if(label.isTickerRunning()) {
                    label.stopTicker();
                }
                label.setTextPosition(0);
            }
        }
    }

    /**
     * Initializes the given component with the given value 
     * 
     * @param cmp one of the components that is or is a part of the renderer
     * @param value the value to install into the component
     */
    protected void setComponentValue(Component cmp, Object value) {
        // fixed components shouldn't be modified by the renderer, this allows for
        // hardcoded properties in the renderer. We still want them to go through the
        // process so renderer selected/unselected styles are applied
        if(cmp.getName().toLowerCase().endsWith("fixed")) {
            return;
        }
        if(cmp instanceof Label) {
            if(value instanceof Image) {
                Image i = (Image)value;
                if(i.isAnimation()) {
                    if(pendingAnimations == null) {
                        pendingAnimations = new Vector();
                    }
                    if(!pendingAnimations.contains(i)) {
                        pendingAnimations.addElement(i);
                        if(parentList != null) {
                            Form f = parentList.getComponentForm();
                            if(f != null) {
                                f.registerAnimated(mon);
                            }
                        }
                    }
                }
                ((Label)cmp).setIcon(i);
                ((Label)cmp).setText("");
                return;
            } else {
                ((Label)cmp).setIcon(null);
            }
            if(cmp instanceof CheckBox) {
                ((CheckBox)cmp).setSelected(isSelectedValue(value));
                return;
            }
            if(cmp instanceof RadioButton) {
                ((RadioButton)cmp).setSelected(isSelectedValue(value));
                return;
            }

            Label l = (Label)cmp;
            if(value == null) {
                l.setText("");
            } else {
                if(value instanceof Label){
                    l.setText(((Label)value).getText());
                    l.setIcon(((Label)value).getIcon());
                }else{
                    l.setText(value.toString());
                }
            }
            if(firstCharacterRTL) {
                String t = l.getText();
                if(t.length() > 0) {
                    l.setRTL(Display.getInstance().isRTL(t.charAt(0)));
                }
            }
            return;
        }
        if(cmp instanceof TextArea) {
            if(value == null) {
                ((TextArea)cmp).setText("");
            } else {
                ((TextArea)cmp).setText(value.toString());
            }
        }
    }

    /**
     * @inheritDoc
     */
    public Component getListFocusComponent(List list) {
        return focusComponent;
    }

    /**
     * @inheritDoc
     */
    public Component getFocusComponent(Component list) {
        return focusComponent;
    }

    /**
     * @return the selectionListener
     */
    public boolean isSelectionListener() {
        return selectionListener;
    }

    /**
     * @param selectionListener the selectionListener to set
     */
    public void setSelectionListener(boolean selectionListener) {
        if(parentList != null) {
            if(parentList instanceof List) {
                ((List)parentList).addActionListener(mon);
            }
        } 
        this.selectionListener = selectionListener;
    }

    /**
     * @return the selected
     */
    public Component getSelected() {
        return selected;
    }

    /**
     * @return the unselected
     */
    public Component getUnselected() {
        return unselected;
    }

    /**
     * @return the selectedEven
     */
    public Component getSelectedEven() {
        return selectedEven;
    }

    /**
     * @return the unselectedEven
     */
    public Component getUnselectedEven() {
        return unselectedEven;
    }

    /**
     * In fisheye rendering mode the renderer maintains selected component drawing
     * @return the fisheye
     */
    public boolean isFisheye() {
        return fisheye;
    }

    /**
     * In fisheye rendering mode the renderer maintains selected component drawing
     * @param fisheye the fisheye to set
     */
    public void setFisheye(boolean fisheye) {
        this.fisheye = fisheye;
    }


    class Monitor implements ActionListener, Animation {
        private boolean selectAllChecked;
        private int selectAllOffset;
        
        /**
         * @inheritDoc
         */
        public boolean animate() {
            if(parentList != null) {
                boolean repaint = false;
                if(pendingAnimations != null) {
                    int s = pendingAnimations.size();
                    for(int iter = 0 ; iter < s ; iter++) {
                        Image i = (Image)pendingAnimations.elementAt(iter);
                        repaint = repaint || i.animate();
                    }
                    pendingAnimations.removeAllElements();
                }
                Form f = parentList.getComponentForm();
                if(f != null) {
                    if(parentList.hasFocus() && Display.getInstance().shouldRenderSelection(parentList)) {
                        for(int iter = 0 ; iter < selectedEntries.length ; iter++) {
                            if(selectedEntries[iter] instanceof Label) {
                                Label l = (Label)selectedEntries[iter];
                                if(l.isTickerRunning()) {
                                    repaint = true;
                                    l.animate();
                                }
                            }
                        }
                    } else {
                        for(int iter = 0 ; iter < selectedEntries.length ; iter++) {
                            if(selectedEntries[iter] instanceof Label) {
                                Label l = (Label)selectedEntries[iter];
                                if(l.isTickerRunning()) {
                                    l.stopTicker();
                                    repaint = true;
                                }
                            }
                        }
                    }
                    if(repaint) {
                        parentList.repaint();
                    } else {
                        f.deregisterAnimated(this);
                    }
                    return false;
                }
                if(repaint) {
                    parentList.repaint();
                } 
            }
            return false;
        }

        /**
         * @inheritDoc
         */
        public void paint(Graphics g) {
        }

        /**
         * @inheritDoc
         */
        public void actionPerformed(ActionEvent evt) {
            if(evt.getComponent() instanceof Button) {
                lastClickedComponent = (Button)evt.getComponent();
                return;
            }
            if(parentList instanceof List) {
                // prevent list from losing focus on action
                parentList.setHandlesInput(true);
                Object selection = ((List)parentList).getSelectedItem();
                if(selection instanceof Hashtable) {
                    Hashtable h = (Hashtable)selection;
                    Command cmd = (Command)h.get("$navigation");
                    if(cmd != null) {
                        parentList.getComponentForm().dispatchCommand(cmd, new ActionEvent(cmd));
                        return;
                    }
                    for(int iter = 0 ; iter < selectedEntries.length ; iter++) {
                        if(selectedEntries[iter] instanceof CheckBox ||
                                selectedEntries[iter] instanceof RadioButton) {
                            boolean sel = !isSelectedValue(h.get(selectedEntries[iter].getName()));
                            if(h.get(SELECT_ALL_FLAG) != null) {
                                selectAllChecked = sel;
                                selectAllOffset = ((List)parentList).getSelectedIndex();

                                // we need to toggle all entries
                                int count = ((List)parentList).getModel().getSize();
                                String selectionVal = "" + sel;
                                for(int x = 0 ; x < count ; x++) {
                                    Object o = ((List)parentList).getModel().getItemAt(x);
                                    if(o instanceof Hashtable) {
                                        ((Hashtable)o).put(selectedEntries[iter].getName(), selectionVal);
                                    }
                                }
                            } else {
                                if(selectAllChecked) {
                                    selectAllChecked = false;
                                    Hashtable selAll = (Hashtable)((List)parentList).getModel().getItemAt(selectAllOffset);
                                    selAll.put(selectedEntries[iter].getName(), "false");
                                }
                                h.put(selectedEntries[iter].getName(), "" + sel);
                            }
                            return;
                        }
                    }
                }
            }
        }
    }
}
