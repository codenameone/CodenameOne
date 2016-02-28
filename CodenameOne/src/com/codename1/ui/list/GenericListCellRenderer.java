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

import com.codename1.cloud.CloudObject;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Button;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.List;
import com.codename1.ui.RadioButton;
import com.codename1.ui.Slider;
import com.codename1.ui.TextArea;
import com.codename1.ui.URLImage;
import com.codename1.ui.animations.Animation;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.plaf.UIManager;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * <p>The generic list cell renderer can display containers or arbitrary Codename One components
 * as items in a list. It relies on the source data being a {@code Map} object. It extracts values from 
 * the {@code Map} using the component name as an indication to the Map key lookup.<br>
 * This renderer supports label tickering, check boxes/radio buttons etc. seamlessly.</p>
 * <p>
 * Please notice that you must use at least two distinct instances of the component
 * when passing them to the constructor, reusing the same instance <b>WILL NOT WORK!</b><br>
 * Furthermore, the renderer instance cannot be reused for multiple lists, each list will need
 * a new instance of this renderer!</p>
 * <p>
 * Sample usage for this renderer follows:
 * </p>
 * <script src="https://gist.github.com/codenameone/15a2370c500e07a8fcf8.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/components-generic-list-cell-renderer.png" alt="Sample of using the generic list cell renderer" />
 *
 * <script src="https://gist.github.com/codenameone/15a2370c500e07a8fcf8.js"></script>
 * 
 * @author Shai Almog
 */
public class GenericListCellRenderer<T> implements ListCellRenderer<T>, CellRenderer<T> {

    /**
     * The default adapter to use for image URLs
     * @return the defaultAdapter
     */
    public static URLImage.ImageAdapter getDefaultAdapter() {
        return defaultAdapter;
    }

    /**
     * The default adapter to use for image URLs
     * @param aDefaultAdapter the defaultAdapter to set
     */
    public static void setDefaultAdapter(URLImage.ImageAdapter aDefaultAdapter) {
        defaultAdapter = aDefaultAdapter;
    }
    private Button lastClickedComponent;
    private ArrayList<Image> pendingAnimations;

    /**
     * If this flag exists in a Map of data the renderer will enable/disable
     * the entries, the flag assumes either Boolean.TRUE or Boolean.FALSE. 
     * Notice that just setting it to false when necessary will not work, when its
     * used it must be applied to all entries otherwise the reuse of the renderer
     * component will break this feature.
     */
    public static final String ENABLED = "$$ENABLED$$";

    /**
     * Put this flag as a Map key to indicate that a checkbox entry rendered by
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
    private boolean waitingForRegisterAnimation;
    private HashMap<String, EncodedImage> placeholders = new HashMap<String, EncodedImage>();
    
    private static URLImage.ImageAdapter defaultAdapter = URLImage.RESIZE_SCALE;
    private URLImage.ImageAdapter adapter = defaultAdapter;
    
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
        addSelectedEntriesListener(unselectedEntries);
    }

    void deinitialize(List l) {
        removeSelectedEntriesListener(selectedEntries);
        removeSelectedEntriesListener(unselectedEntries);
        l.removeActionListener(mon);
    }
     
    /**
     * Updates the placeholder instances, this is useful for changing the URLImage placeholder in runtime as
     * might happen in the designer
     */
    public void updateIconPlaceholders() {
        updateIconPlaceholders(selectedEntries);
        updateIconPlaceholders(unselectedEntries);
    }
    
    private void updateIconPlaceholders(Component[] e) {
        int elen = e.length;
        for(int iter = 0 ; iter < elen ; iter++) {
            String n = e[iter].getName();
            if(n != null) {
                if(n.endsWith("_URLImage") && e[iter] instanceof Label) {
                    placeholders.put(n, (EncodedImage)((Label)e[iter]).getIcon());
                }
            }
        }
    }

    private void removeSelectedEntriesListener(Component[] e) {
        int elen = e.length;
        for(int iter = 0 ; iter < elen ; iter++) {
            if(e[iter] instanceof Button) {
                ((Button)e[iter]).removeActionListener(mon);
            }
        }
    }
    
    private void addSelectedEntriesListener(Component[] e) {
        int elen = e.length;
        for(int iter = 0 ; iter < elen ; iter++) {
            if(e[iter] instanceof Button) {
                ((Button)e[iter]).addActionListener(mon);
            }
            String n = e[iter].getName();
            if(n != null) {
                if(n.endsWith("_URLImage") && e[iter] instanceof Label) {
                    placeholders.put(n, (EncodedImage)((Label)e[iter]).getIcon());
                }
            }
        }
    }

    private Component[] initRenderer(Component r) {
        r.setCellRenderer(true);
        if(r instanceof Container) {
            ArrayList selectedVector = new ArrayList();
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
        addSelectedEntriesListener(unselectedEntriesEven);
    }
    
    

    private Component[] vectorToComponentArray(ArrayList v) {
        Component[] result = new Component[v.size()];
        int rlen = result.length;
        for(int iter = 0 ; iter < rlen ; iter++) {
            result[iter] = (Component)v.get(iter);
        }
        return result;
    }

    private void findComponentsOfInterest(Component cmp, ArrayList dest) {
        if(cmp instanceof Container) {
            Container c = (Container)cmp;
            int count = c.getComponentCount();
            for(int iter = 0 ; iter < count ; iter++) {
                findComponentsOfInterest(c.getComponentAt(iter), dest);
            }
            return;
        }
        // performance optimization for fixed images in lists
        if(cmp.getName() != null) {
            if(cmp instanceof Label) {
                Label l = (Label)cmp;
                if(l.getName().toLowerCase().endsWith("fixed") && l.getIcon() != null) {
                    l.getIcon().lock();
                }
                dest.add(cmp);
                return;
            }
            if(cmp instanceof TextArea) {
                dest.add(cmp);
                return;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public Component getCellRendererComponent(Component list, Object model, T value, int index, boolean isSelected) {
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
            cmp.setFocus(true);
            boolean lead = false;
            if(cmp instanceof Container) {
                lead = ((Container)cmp).getLeadComponent() != null;
            }
            if(value instanceof Map) {
                Map h = (Map)value;
                Boolean enabled = (Boolean)h.get(ENABLED);
                if(enabled != null) {
                    cmp.setEnabled(enabled.booleanValue());
                }
                int elen = entries.length;
                for(int iter = 0 ; iter < elen ; iter++) {
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
                        val = updateModelValues(h, currentName, entries, iter, val);
                    }                    
                    setComponentValueWithTickering(entries[iter], val, list, cmp);
                    entries[iter].setFocus(lead || entries[iter].isFocusable());
                }
            } else {
                if(value instanceof CloudObject) {
                    CloudObject h = (CloudObject)value;
                    Boolean enabled = (Boolean)h.getBoolean(ENABLED);
                    if(enabled != null) {
                        cmp.setEnabled(enabled.booleanValue());
                    }
                    int elen = entries.length;
                    for(int iter = 0 ; iter < elen ; iter++) {
                        String currentName = entries[iter].getName();

                        Object val;
                        if(currentName.equals("$number")) {
                            val = "" + (index + 1);
                        } else {
                            // a selected entry might differ in its value to allow for
                            // behavior such as rollover images
                            val = h.getObject("#" + currentName);
                            if(val == null) {
                                val = h.getObject(currentName);
                            }
                        }
                        setComponentValueWithTickering(entries[iter], val, list, cmp);
                        entries[iter].setFocus(entries[iter].isFocusable());
                    }
                } else {
                    setComponentValueWithTickering(entries[0], value, list, cmp);
                    entries[0].setFocus(entries[0].isFocusable());
                }
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
            cmp.setFocus(false);
            if(value instanceof Map) {
                Map h = (Map)value;
                Boolean enabled = (Boolean)h.get(ENABLED);
                if(enabled != null) {
                    cmp.setEnabled(enabled.booleanValue());
                }
                int elen = entries.length;
                for(int iter = 0 ; iter < elen ; iter++) {
                    String currentName = entries[iter].getName();
                    if(currentName.equals("$number")) {
                        setComponentValue(entries[iter], "" + (index + 1), list, cmp);
                        continue;
                    }
                    Object val = h.get(currentName);
                    val = updateModelValues(h, currentName, entries, iter, val);
                    setComponentValue(entries[iter], val, list, cmp);
                }
            } else {
                if(value instanceof CloudObject) {
                    CloudObject h = (CloudObject)value;
                    Boolean enabled = h.getBoolean(ENABLED);
                    if(enabled != null) {
                        cmp.setEnabled(enabled.booleanValue());
                    }
                    int elen = entries.length;
                    for(int iter = 0 ; iter < elen ; iter++) {
                        String currentName = entries[iter].getName();
                        if(currentName.equals("$number")) {
                            setComponentValue(entries[iter], "" + (index + 1), list, cmp);
                            continue;
                        }
                        setComponentValue(entries[iter], h.getObject(currentName), list, cmp);
                    }
                } else {
                    setComponentValue(entries[0], value, list, cmp);
                }
            }
            return cmp;
        }
    }

    private Object updateModelValues(Map h, String currentName, Component[] entries, int iter, Object val) {
        String uiid = (String)h.get(currentName + "_uiid");
        if(uiid != null) {
            entries[iter].setUIID(uiid);
        }
        if(currentName.endsWith("_URLImage")) {
            URLImage img = (URLImage)h.get(currentName + "Actual");
            if(img != null) {
                val = img;
            } else {
                String name = (String)h.get(currentName + "Name");
                if(name == null) {
                    name = val.toString();
                    name = name.substring(name.lastIndexOf('/'));
                }
                val = URLImage.createToStorage(placeholders.get(currentName), name, val.toString(), adapter);
                h.put(currentName + "Actual", val);
            }
        }
        return val;
    }


    /**
     * {@inheritDoc}
     */
    public Component getListCellRendererComponent(List list, T value, int index, boolean isSelected) {
        return getCellRendererComponent(list, list.getModel(), value, index, isSelected);
    }


    private boolean isSelectedValue(Object v) {
        return v != null && "true".equalsIgnoreCase(v.toString());
    }

    private void setComponentValueWithTickering(Component cmp, Object value, Component l, Component rootRenderer) {
        setComponentValue(cmp, value, l, rootRenderer);
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
    private void setComponentValue(Component cmp, Object value, Component parent, Component rootRenderer) {
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
                        pendingAnimations = new ArrayList<Image>();
                    }
                    if(!pendingAnimations.contains(i)) {
                        pendingAnimations.add(i);
                        if(parentList == null) {
                            parentList = parent;
                        }
                        if(parentList != null) {
                            Form f = parentList.getComponentForm();
                            if(f != null) {
                                f.registerAnimated(mon);
                                waitingForRegisterAnimation = false;
                            } else {
                                waitingForRegisterAnimation = true;
                            }
                        }
                    } else {
                        if(waitingForRegisterAnimation) {
                            if(parentList != null) {
                                Form f = parentList.getComponentForm();
                                if(f != null) {
                                    f.registerAnimated(mon);
                                    waitingForRegisterAnimation = false;
                                }
                            }
                        }
                    }
                }
                Image oldImage = ((Label)cmp).getIcon();
                ((Label)cmp).setIcon(i);
                ((Label)cmp).setText("");
                if(oldImage == null || oldImage.getWidth() != i.getWidth() || oldImage.getHeight() != i.getHeight()) {
                    ((Container)rootRenderer).revalidate();
                }
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
            if(cmp instanceof Slider) {
                ((Slider)cmp).setProgress(((Integer)value).intValue());
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
     * {@inheritDoc}
     */
    public Component getListFocusComponent(List list) {
        return focusComponent;
    }

    /**
     * {@inheritDoc}
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

    /**
     * The adapter used when dealing with image URL's
     * @return the adapter
     */
    public URLImage.ImageAdapter getAdapter() {
        return adapter;
    }

    /**
     * The adapter used when dealing with image URL's
     * @param adapter the adapter to set
     */
    public void setAdapter(URLImage.ImageAdapter adapter) {
        this.adapter = adapter;
    }


    class Monitor implements ActionListener, Animation {
        private boolean selectAllChecked;
        private int selectAllOffset;
        
        /**
         * {@inheritDoc}
         */
        public boolean animate() {
            boolean hasAnimations = false;
            if(parentList != null) {
                boolean repaint = false;
                if(pendingAnimations != null && pendingAnimations.size() > 0) {
                    int s = pendingAnimations.size();
                    hasAnimations = true;
                    for(int iter = 0 ; iter < s ; iter++) {
                        Image i = (Image)pendingAnimations.get(iter);
                        repaint = i.animate() || repaint;
                    }
                    if(repaint) {
                        pendingAnimations.clear();
                    } else {
                        // flush the queue if we have too many animations
                        if(pendingAnimations.size() > 20) {
                            repaint = true;
                        }
                    }
                }
                Form f = parentList.getComponentForm();
                if(f != null) {
                    if(parentList.hasFocus() && Display.getInstance().shouldRenderSelection(parentList)) {
                        int slen = selectedEntries.length;
                        for(int iter = 0 ; iter < slen ; iter++) {
                            if(selectedEntries[iter] instanceof Label) {
                                Label l = (Label)selectedEntries[iter];
                                if(l.isTickerRunning()) {
                                    repaint = true;
                                    l.animate();
                                }
                            }
                        }
                    } else {
                        int slen = selectedEntries.length;
                        for(int iter = 0 ; iter < slen ; iter++) {
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
                        if(!hasAnimations) {
                            f.deregisterAnimated(this);
                        }
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
         * {@inheritDoc}
         */
        public void paint(Graphics g) {
        }

        /**
         * {@inheritDoc}
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
                if(selection instanceof Map) {
                    Map h = (Map)selection;
                    Command cmd = (Command)h.get("$navigation");
                    if(cmd != null) {
                        parentList.getComponentForm().dispatchCommand(cmd, new ActionEvent(cmd,ActionEvent.Type.Command));
                        return;
                    }
                    int slen = selectedEntries.length;
                    for(int iter = 0 ; iter < slen ; iter++) {
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
                                    if(o instanceof Map) {
                                        ((Map)o).put(selectedEntries[iter].getName(), selectionVal);
                                    }
                                }
                            } else {
                                if(selectAllChecked) {
                                    selectAllChecked = false;
                                    Map selAll = (Map)((List)parentList).getModel().getItemAt(selectAllOffset);
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
