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
package com.codename1.ui;

import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.list.*;
import java.util.ArrayList;

/**
 * This class is an editable TextField with predefined completion suggestion 
 * that shows up in a drop down menu while the user types in text
 *
 * @author Chen
 */
public class AutoCompleteTextField extends TextField {

    private Container popup;
    private FilterProxyListModel<String> filter;
    private ActionListener listener = new FormPointerListener();
    private ListCellRenderer completionRenderer;
    private ArrayList<ActionListener> listeners = new ArrayList<ActionListener>();
    
    /**
     * Constructor with completion suggestions
     * @param completion a String array of suggestion for completion
     */ 
    public AutoCompleteTextField(String[] completion) {
        this(new DefaultListModel<String>(completion));
    }

    /**
     * Constructor with completion suggestions, filtering is automatic in this case
     * @param listModel a list model containing potential string suggestions
     */ 
    public AutoCompleteTextField(ListModel<String> listModel) {
        popup = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        filter = new FilterProxyListModel<String>(listModel);
        popup.setScrollable(false);
    }

    /**
     * The default constructor is useful for cases of filter subclasses overriding the
     * getSuggestionModel value as well as for the GUI builder
     */
    public AutoCompleteTextField() {
        filter = new FilterProxyListModel<String>(new DefaultListModel(new String[]{""}));
        popup = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        popup.setScrollable(false);
    }
    
    /**
     * @inheritDoc
     */
    @Override
    protected void initComponent() {
        super.initComponent();
        getComponentForm().addPointerPressedListener(listener);
    }

    /**
     * @inheritDoc
     */
    @Override
    protected void deinitialize() {
        super.deinitialize();
        getComponentForm().removePointerPressedListener(listener);
    }

    private void setParentText(String text) {
        super.setText(text);
    }

    /**
     * @inheritDoc
     */
    @Override
    public void setText(String text) {
        super.setText(text);
        if (text == null) {
            return;
        }
        if(filter(text)) {
            updateFilterList();
        } 
    }
    
    /**
     * In a case of an asynchronous filter this method can be invoked to refresh the completion list
     */
    protected void updateFilterList() {
        Form f = getComponentForm();
        if (f != null && popup.getParent() == null) {
            addPopup();
        }
        popup.revalidate();
    }
    
    /**
     * Subclasses can override this method to perform more elaborate filter operations
     * @param text the text to filter
     * @return true if the filter has changed the list, false if it hasn't or is working asynchronously
     */
    protected boolean filter(String text) {
        if(filter != null) {
            filter.filter(text);        
            return true;
        }
        return false;
    }
    
    /**
     * Returns the list model to show within the completion list
     * @return the list model can be anything
     */
    protected ListModel<String> getSuggestionModel() {
        return filter;
    }

    /**
     * Sets a custom renderer to the completion suggestions list.
     * @param completionRenderer a ListCellRenderer for the suggestions List
     */ 
    public void setCompletionRenderer(ListCellRenderer completionRenderer) {
        this.completionRenderer = completionRenderer;
    }
    
    /**
     * @inheritDoc
     */
    public void keyPressed(int k) {
        if(popup != null && popup.getParent() != null && popup.getComponentCount() > 0) {
            int game = Display.getInstance().getGameAction(k);
            if(game == Display.GAME_DOWN || game == Display.GAME_UP || game == Display.GAME_FIRE) {
                popup.getComponentAt(0).keyPressed(k);
                return;
            }
        }
        super.keyPressed(k);
    }

    /**
     * @inheritDoc
     */
    public void keyReleased(int k) {
        if(popup != null && popup.getParent() != null && popup.getComponentCount() > 0) {
            int game = Display.getInstance().getGameAction(k);
            if(game == Display.GAME_DOWN || game == Display.GAME_UP || game == Display.GAME_FIRE) {
                popup.getComponentAt(0).keyReleased(k);
                return;
            }
        }
        super.keyReleased(k);
    }

    private void removePopup() {
        Form f = getComponentForm();
        if (f != null) {
            f.getLayeredPane().removeComponent(popup);
            popup.setParent(null);
            f.revalidate();
        }
    }

    /**
     * Adds an action listener that fires an event when an entry in the auto-complete list is selected.
     * Notice that this method will only take effect when the popup is reshown, if it is invoked when
     * a popup is already showing it will have no effect.
     * @param a the listener
     */
    public void addListListener(ActionListener a) {
        listeners.add(a);
    }

    /**
     * Removes an action listener that fires an event when an entry in the auto-complete list is selected.
     * Notice that this method will only take effect when the popup is reshown, if it is invoked when
     * a popup is already showing it will have no effect.
     * @param a the listener
     */
    public void removeListListener(ActionListener a) {
        listeners.remove(a);
    }
    
    private void addPopup() {
        Form f = getComponentForm();
        popup.removeAll();
        filter(getText());
        final com.codename1.ui.List l = new com.codename1.ui.List(getSuggestionModel());
        for(ActionListener al : listeners) {
            l.addActionListener(al);
        }
        if(completionRenderer == null){
            ((DefaultListCellRenderer<String>)l.getRenderer()).setShowNumbers(false);
        }else{
            l.setRenderer(completionRenderer);
        }
        l.setUIID("AutoCompletePopup");
        l.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                setParentText((String) l.getSelectedItem());
                removePopup();
            }
        });
        popup.addComponent(l);
        popup.getStyle().setMargin(LEFT, getAbsoluteX());
        int top = getAbsoluteY() - f.getTitleArea().getHeight() + getHeight();
        popup.getStyle().setMargin(TOP, top);
        popup.setPreferredW(getWidth());
        popup.setPreferredH(Display.getInstance().getDisplayHeight() - top);
        if (f != null) {
            if (popup.getParent() == null) {
                f.getLayeredPane().addComponent(popup);
            }
            f.revalidate();
        }
    }

    class FormPointerListener implements ActionListener {

        public void actionPerformed(ActionEvent evt) {
            Form f = getComponentForm();
            if (f.getLayeredPane().getComponentCount() > 0 && popup.getComponentCount() > 0) {
                if (!popup.getComponentAt(0).
                        contains(evt.getX(), evt.getY())) {
                    removePopup();
                }
            } else {
                if (contains(evt.getX(), evt.getY())) {
                    addPopup();
                }

            }

        }
    }

    /**
     * @inheritDoc
     */
    public String[] getPropertyNames() {
        return new String[] {"completion"};
    }

    /**
     * @inheritDoc
     */
    public Class[] getPropertyTypes() {
       return new Class[] {com.codename1.impl.CodenameOneImplementation.getStringArrayClass()};
    }
    
    /**
     * @inheritDoc
     */
    public String[] getPropertyTypeNames() {
        return new String[] {"String[]"};
    }

    /**
     * @inheritDoc
     */
    public Object getPropertyValue(String name) {
        if(name.equals("completion")) {
            String[] r = new String[filter.getUnderlying().getSize()];
            for(int iter = 0 ; iter < r.length ; iter++) {
                r[iter] = (String)filter.getUnderlying().getItemAt(iter);
            }
            return r;
        }
        return null;
    }

    /**
     * @inheritDoc
     */
    public String setPropertyValue(String name, Object value) {
        if(name.equals("completion")) {
            filter = new FilterProxyListModel<String>(new DefaultListModel<String>((String[])value));
            return null;
        }
        return super.setPropertyValue(name, value);
    }
}
