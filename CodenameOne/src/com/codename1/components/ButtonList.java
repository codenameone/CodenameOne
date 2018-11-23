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

import com.codename1.ui.ButtonGroup;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.RadioButton;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.events.SelectionListener;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.list.DefaultListModel;
import com.codename1.ui.list.ListModel;
import com.codename1.ui.list.MultipleSelectionListModel;
import com.codename1.ui.util.EventDispatcher;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * An abstract base class for a list of buttons.  Most useful for grids of toggle widgets such as Radio Buttons,
 * CheckBoxes, and Switches.  There are concrete implementations for {@link Switch} ({@link SwitchList}), {@link RadioButton} ({@link RadioButtonList},
 * and {@link CheckBox} ({@link CheckBoxList}).
 * 
 * <p>This abstraction allows you to work with a set of toggle buttons as a single unit.  It uses
 * a {@link ListModel} to store the toggle options, and will automatically stay in sync with its model
 * when options are added or removed, or the selection is changed.</p>
 * 
 * <script src="https://gist.github.com/shannah/838e3b7c1f558991378dc8ba3f4a9c43.js"></script>
 * 
 * <h3>Examples</h3>
 * 
 * <p><strong>Switch List in a FlowLayout:</strong><br/>
 * <pre>{@code 
SwitchList switchList = new SwitchList(new DefaultListModel("Red", "Green", "Blue", "Indigo"));
switchList.setLayout(new FlowLayout());
 * }</pre>
 * <img src="https://www.codenameone.com/img/developer-guide/switch-list-flowlayout.png" alt="SwitchList in a flow layout" />
 * </p>
 * 
 * <p><strong>Switch List in a BoxLayout.Y:</strong><br/>
 *  * <pre>{@code 
SwitchList switchList = new SwitchList(new DefaultListModel("Red", "Green", "Blue", "Indigo"));
switchList.setLayout(BoxLayout.y());
 * }</pre>
 * <img src="https://www.codenameone.com/img/developer-guide/switchlist-boxlayout-y.png" alt="SwitchList in a box layout Y" />
 * </p>
 * 
 * <p><strong>Switch List in a Grid Layout:</strong><br/>
 * <img src="https://www.codenameone.com/img/developer-guide/switchlist-gridlayout-2col.png" alt="SwitchList in a grid layout" />
 * </p>
 * 
 * <p><strong>Switch List in a Table Layout:</strong><br/>
 * <strong>2 Columns:</strong><br/>
 <pre>{@code 
SwitchList switchList = new SwitchList(new DefaultListModel("Red", "Green", "Blue", "Indigo"));
switchList.setLayout(new TableLayout(switchList.getComponentCount()/2+1, 2));
 * }</pre>
 * <img src="https://www.codenameone.com/img/developer-guide/switchlist-tablelayout-2col.png" alt="SwitchList in a grid layout" />
 * <strong>3 Columns:</strong><br/>
 <pre>{@code 
SwitchList switchList = new SwitchList(new DefaultListModel("Red", "Green", "Blue", "Indigo"));
switchList.setLayout(new TableLayout(switchList.getComponentCount()/3+1, 3));
 * }</pre>
 * <img src="https://www.codenameone.com/img/developer-guide/switchlist-tablelayout-3col.png" alt="SwitchList in a grid layout" />
 * </p>
 * @author Steve Hannah
 * @since 6.0
 */
public abstract class ButtonList extends Container implements DataChangedListener, SelectionListener, ActionListener {
    private ButtonGroup group;
    private ListModel model;
    private EventDispatcher actionListeners = new EventDispatcher();
    protected boolean ready;
    private java.util.List<Runnable> onReady = new ArrayList<Runnable>();
    private String cellUIID;
    private java.util.List<Decorator> decorators;
    
    /**
     * An interface that can be implemented to provide custom decoration/undecoration of the buttons as they are 
     * created/removed.  This will allow you to do things like add custom icons or styles to the buttons.
     * @param <M> The type used for the model item.
     * @param <V> The type used for the view.  For RadioList T would be RadioButton.  For CheckBoxList, T would be CheckBox.  For SwitchList it's different because
     * the Switch uses a wrapper component.
     */
    public static interface Decorator<M,V extends Component> {
        public void decorate(M modelItem, V viewItem);
        public void undecorate(V viewItem);
    }
    
    
    /**
     * Wrap any calls that requires that the infrastructure is ready inside this.
     * @param r Will be run when buttons are ready to be generated.
     */
    protected void onReady(Runnable r) {
        if (ready) {
            r.run();
        } else {
            onReady.add(r);
        }
    }
    
    /**
     * This should be called by the concrete implementation once it is ready to generate the
     * buttons.
     */
    protected void fireReady() {
        for (Runnable r : onReady) {
            r.run();
        }
        onReady.clear();
    }
    
    /**
     * Creates a new ButtonList.
     * @param model The options.  Each will be represented by a button.
     */
    public ButtonList(ListModel model) {
        if (model instanceof DefaultListModel && isAllowMultipleSelection()) {
            ((DefaultListModel)model).setMultiSelectionMode(true);
        }
        setModel(model);
            
    }
    
    /**
     * For multi-selection models (e.g. for checkbox or switch lists), this will return the 
     * model as a {@link MultiSelectionListModel}.  Otherwise it will return null.
     * @return The model.
     */
    public MultipleSelectionListModel getMultiListModel() {
        if (model instanceof MultipleSelectionListModel) {
            return (MultipleSelectionListModel)model;
        }
        return null;
    }
    
    /**
     * Returns the model.
     * @return The model
     */
    public ListModel getModel() {
        return model;
    }
    
    /**
     * Returns true for lists that allow multiple selection.  {@link CheckBoxList}, and {@link SwitchList} support multiple selection.
     * {@link RadioButtonList} does not.
     * @return
     */
    public abstract boolean isAllowMultipleSelection();
    
    /**
     * Creates a new button for this list. Should be implemented by subclasses to create the correct kind of button.
     * @param model
     * @return 
     */
    protected abstract Component createButton(Object model);
    
    /**
     * Sets the given button's selected state.
     * @param button The button (in the form produced by {@link #createButton}.
     * @param selected Whether the button is selected or not.
     */
    protected abstract void setSelected(Component button, boolean selected);
    
    public void setModel(ListModel model) {
        if (model != this.model) {
            if (this.model != null) {
                this.model.removeDataChangedListener(this);
                this.model.removeSelectionListener(this);
            }
            this.model = model;
            if (this.model != null) {
                this.model.addDataChangedListener(this);
                this.model.addSelectionListener(this);
            }
            if (ready) {
                refresh();
            } else {
                onReady(new Runnable() {
                    public void run() {
                        refresh();
                    }
                });
            }
        }
    }

    /**
     * Sets the layout for the list.  This refresh the list to match the new layout.
     * @param layout The layout to use.  Only layouts that don't require constraints in {@link Container#add(java.lang.Object, com.codename1.ui.Component) }
     * may be used.  E.g. {@link FlowLayout}, {@link BoxLyout}, {@link TableLayout}, {@link GridLayout} are all fine.
     */
    @Override
    public void setLayout(Layout layout) {
        if (layout != this.getLayout()) {
            super.setLayout(layout);
            refresh();
        }
    }
    
    
    
    /**
     * Refreshes the container - regenerating all of the buttons in the list from
     * the model.  This usually doesn't ever need to be called explicitly as it will be called
     * automatically when the model changes, or the layout changes.
     */
    public void refresh() {
        group = new ButtonGroup();
        
        removeAll();
        int selectedIndex = getModel().getSelectedIndex();
        int[] selectedIndices = new int[0];
        if (getModel() instanceof MultipleSelectionListModel) {
            selectedIndices = getMultiListModel().getSelectedIndices();
        }
        int len = model.getSize();
        for (int i=0; i<len; i++) {
            Component b = createComponent(model.getItemAt(i));
            if (isAllowMultipleSelection()) {
                if (Arrays.binarySearch(selectedIndices, i) >= 0) {
                    setSelected(b, true);
                }
            } else {
                if (i == selectedIndex) {
                    setSelected(b, true);
                }
            }
            add(b);
        }
        
    }
    
    
    private Component createComponent(Object model) {
        Component b = createButton(model);
        b = decorateComponent(model, b);
        return b;
    }
    
    /**
     * Decorates buttons.  This allows subclasses to add event listeners
     * to buttons.
     * @param b The button in the form returned by {@link #createButton(java.lang.Object) }
     * @return Should pass back the same component it receives.
     */
    protected Component decorateComponent(Object modelItem, Component b) {
        if (cellUIID != null) {
            setUIID(cellUIID);
        }
        if (b instanceof RadioButton) {
            group.add((RadioButton)b);
        }
        if (decorators != null) {
            for (Decorator d : decorators) {
                d.decorate(modelItem, b);
            }
        }
        return b;
    }
    
    /**
     * Undecorates buttons.  This allows subclasses to remove event listeners from
     * buttons.
     * @param b The button in the form returned by {@link #createButton(java.lang.Object) }
     * @return Should pass back the same component it receives.
     */
    protected Component undecorateComponent(Component b) {
        if (decorators != null) {
            for (Decorator d : decorators) {
                d.undecorate(b);
            }
        }
        
        if (b instanceof RadioButton) {
            group.remove((RadioButton)b);
        }
        
        return b;
    }
    
    // When the list model is changed, this fires
    // This should allow us to keep the buttons in sync
    // with the model
    @Override
    public void dataChanged(int status, int index) {
        switch (status) {
            case DataChangedListener.ADDED:
                this.addComponent(index, createComponent(model.getItemAt(index)));
                break;
            case DataChangedListener.CHANGED:
                {
                    Component cmp = undecorateComponent(this.getComponentAt(index));
                    this.removeComponent(cmp);
                    this.addComponent(index, createComponent(model.getItemAt(index)));
                    break;
                }
            case DataChangedListener.REMOVED:
                {
                    Component cmp = undecorateComponent(this.getComponentAt(index));
                    this.removeComponent(cmp);
                    break;
                }
            default:
                break;
        }
    }

    // Called when the selection is changed in the model
    @Override
    public void selectionChanged(int oldSelected, int newSelected) {
        if (isAllowMultipleSelection()) {
            if (oldSelected < 0 && newSelected >= 0) {
                Component cmp = newSelected < getComponentCount() ? getComponentAt(newSelected) : null;
                if (cmp != null) {
                    setSelected(cmp, true);
                }
            } else if (oldSelected >= 0 && newSelected < 0) {
                Component cmp = oldSelected < getComponentCount() ? getComponentAt(oldSelected) : null;
                if (cmp != null) {
                    setSelected(cmp, false);
                }
            } else {
                Component old = getComponentAt(oldSelected);
                if (old != null) {
                    setSelected(old, false);
                }
                Component cmp = newSelected < getComponentCount() ? getComponentAt(newSelected) : null;
                if (cmp != null) {
                    setSelected(cmp, true);
                }
            }
        } else {
            if (newSelected >= 0) {
                Component cmp = newSelected < getComponentCount() ? getComponentAt(newSelected) : null;
                if (cmp != null) {
                    setSelected(cmp, true);
                }
            }
            if (oldSelected >= 0) {
                Component cmp = oldSelected < getComponentCount() ? getComponentAt(oldSelected) : null;
                setSelected(cmp, false);
            }
        }
    }

    // Called when one of the buttons within the list fires an action event.
    // This should just propagate the event.
    @Override
    public void actionPerformed(ActionEvent evt) {
        if (evt.getSource() instanceof Component && contains((Component)evt.getSource())) {
            ActionEvent nevt = new ActionEvent(this, evt.getEventType(), evt.getActualComponent(), evt.getX(), evt.getY());
            actionListeners.fireActionEvent(nevt);
        }
    }
    
    /**
     * Add a listener to be notified when any of the buttons in the list are pressed.
     * @param l 
     */
    public void addActionListener(ActionListener l) {
        actionListeners.addListener(l);
    }
    
    /**
     * Remove a listener so that it no longer is notified when buttons in the list are pressed.
     * @param l 
     */
    public void removeActionListener(ActionListener l) {
        actionListeners.removeListener(l);
    }
    
    /**
     * Sets the UIID for cells of the list.  Each cell will be a component as returned by the
     * concrete implementation's {@link #createButton(java.lang.Object) } method.
     * @param uiid 
     */
    public void setCellUIID(String uiid) {
        cellUIID = uiid;
        for (Component c : this) {
            c.setUIID(cellUIID);
        }
    }
    
    /**
     * Adds a decorator that can be used to customize a button when it is created
     * 
     * @param decorator A decorator.
     */
    public void addDecorator(Decorator decorator) {
        if (decorators == null) {
            decorators = new ArrayList<Decorator>();
        }
        decorators.add(decorator);
    }
    
    /**
     * Removes a decorator.
     * @param decorator 
     */
    public void removeDecorator(Decorator decorator) {
        if (decorators != null) {
            decorators.remove(decorator);
        }
    }
    
}
