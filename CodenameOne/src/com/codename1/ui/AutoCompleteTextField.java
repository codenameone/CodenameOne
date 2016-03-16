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
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.list.*;
import com.codename1.ui.plaf.Style;
import java.util.ArrayList;

/**
 * <p>An editable {@link com.codename1.ui.TextField} with completion suggestions 
 * that show up in a drop down menu while the user types in text. <br>
 * This class uses the "{@code TextField}" UIID by default as well as "{@code AutoCompletePopup}" &amp;
 * "{@code AutoCompleteList}" for the popup list details.<br>
 * The sample below shows the more trivial use case for this widget:
 * </p>
 * 
 * <script src="https://gist.github.com/codenameone/7e4dc757971e460e5823.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/components-autocomplete.png" alt="Simple usage of auto complete" />
 * 
 *
 * @author Chen
 */
public class AutoCompleteTextField extends TextField {

    private Container popup;
    private boolean dontCalcSize = false;
    private FilterProxyListModel<String> filter;
    private ActionListener listener = new FormPointerListener();
    private ListCellRenderer completionRenderer;
    private ArrayList<ActionListener> listeners = new ArrayList<ActionListener>();
    private String pickedText;
    private int minimumLength;
    
    /**
     * The number of elements shown for the auto complete popup
     */
    private int minimumElementsShownInPopup = -1;
    
    /**
     * Constructor with completion suggestions
     * @param completion a String array of suggestion for completion
     */ 
    public AutoCompleteTextField(String... completion) {
        this(new DefaultListModel<String>(completion));
    }

    /**
     * Constructor with completion suggestions, filtering is automatic in this case
     * @param listModel a list model containing potential string suggestions
     */ 
    public AutoCompleteTextField(ListModel<String> listModel) {
        popup = new Container(new BoxLayout(BoxLayout.Y_AXIS)){

            @Override
            public void setShouldCalcPreferredSize(boolean shouldCalcPreferredSize) {
                if(dontCalcSize){
                    return;
                }
                super.setShouldCalcPreferredSize(shouldCalcPreferredSize);
            }
        
        };
        filter = new FilterProxyListModel<String>(listModel);                
        popup.setScrollable(false);
        popup.setUIID("AutoCompletePopup");
        setConstraint(TextArea.NON_PREDICTIVE);
        
    }

    /**
     * The default constructor is useful for cases of filter subclasses overriding the
     * getSuggestionModel value as well as for the GUI builder
     */
    public AutoCompleteTextField() {
        this(new DefaultListModel(new Object[]{""}));
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void initComponent() {
        super.initComponent();
        getComponentForm().addPointerReleasedListener(listener);
        Display.getInstance().callSerially(new Runnable() {

            @Override
            public void run() {
                addPopup();
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void deinitialize() {
        super.deinitialize();
        getComponentForm().removePointerReleasedListener(listener);
        Display.getInstance().callSerially(new Runnable() {

            @Override
            public void run() {
                removePopup();
            }
        });
    }

    void setParentText(String text) {
        super.setText(text);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setText(String text) {
        super.setText(text);
        if (text == null || (pickedText != null && pickedText.equals(text))) {
            return;
        }
        pickedText = null;
        Form f = getComponentForm();
        if(f != null && filterImpl(text)) {
            updateFilterList();
        } 
    }
    
    /**
     * In a case of an asynchronous filter this method can be invoked to refresh the completion list
     */
    protected void updateFilterList() {
        Form f = getComponentForm();
        boolean v = filter.getSize() > 0 && getText().length() >= minimumLength;
        if(v != popup.isVisible()) {
            popup.getComponentAt(0).setScrollY(0);
            popup.setVisible(v);
            popup.setEnabled(v);
            f.repaint();
        } 
        if(f != null) {
            dontCalcSize = false;
            f.revalidate();
            dontCalcSize = true;
        }

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
    
    private boolean filterImpl(String text) {
        boolean res = filter(text);
        if(filter != null && popup != null) {
            boolean v = filter.getSize() > 0 && text.length() >= minimumLength;
            if(v != popup.isVisible()) {
                popup.getComponentAt(0).setScrollY(0);
                popup.setVisible(v);
                popup.setEnabled(v);

                if(!v) {
                    Form f = getComponentForm();
                    if(f != null) {
                        f.repaint();
                    }
                }
            }
        }
        return res;
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
     * {@inheritDoc}
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
     * {@inheritDoc}
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
            Container lay = f.getLayeredPane(AutoCompleteTextField.this.getClass(), true);
            Container parent = popup.getParent();
            lay.removeComponent(parent);
            popup.remove();
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
        final Form f = getComponentForm();
        popup.removeAll();
        popup.setVisible(false);
        popup.setEnabled(false);
        filter(getText());        
        final com.codename1.ui.List l = new com.codename1.ui.List(getSuggestionModel());
        if(getMinimumElementsShownInPopup() > 0) {
            l.setMinElementHeight(getMinimumElementsShownInPopup());
        }
        l.setScrollToSelected(false);
        l.setItemGap(0);
        for(ActionListener al : listeners) {
            l.addActionListener(al);
        }
        if(completionRenderer == null){
            ((DefaultListCellRenderer<String>)l.getRenderer()).setShowNumbers(false);
        }else{
            l.setRenderer(completionRenderer);
        }
        l.setUIID("AutoCompleteList");
        l.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                pickedText = (String) l.getSelectedItem();
                setParentText(pickedText);
                
                // relaunch text editing if we are still editing
                if(Display.getInstance().isTextEditing(AutoCompleteTextField.this)) {
                    Display.getInstance().editString(AutoCompleteTextField.this, getMaxSize(), getConstraint(), (String) l.getSelectedItem());
                }
                popup.setVisible(false);
                popup.setEnabled(false);
                f.repaint();
            }
        });
        
        byte [] units = popup.getStyle().getMarginUnit();
        if(units != null){
            units[Component.LEFT] = Style.UNIT_TYPE_PIXELS;
            units[Component.TOP] = Style.UNIT_TYPE_PIXELS;
            popup.getAllStyles().setMarginUnit(units);
        }
        popup.getAllStyles().setMargin(LEFT, Math.max(0, getAbsoluteX()));        
        
        int y = getAbsoluteY();
        int topMargin;
        int popupHeight;
        int items = l.getModel().getSize();
        if(l.getModel() instanceof FilterProxyListModel){
            items = ((FilterProxyListModel)l.getModel()).getUnderlying().getSize();
        }
        int listHeight = items * l.getElementSize(false, true).getHeight();
        if(y < f.getContentPane().getHeight()/2){
            topMargin =  y - f.getTitleArea().getHeight() + getHeight();
            popupHeight = Math.min(listHeight, f.getContentPane().getHeight()/2);  
        }else{
            popupHeight = Math.min(listHeight, f.getContentPane().getHeight()/2);  
            popupHeight = Math.min(popupHeight, y - f.getTitleArea().getHeight());
            topMargin =  y - f.getTitleArea().getHeight() - popupHeight;
        }
        popup.getAllStyles().setMargin(TOP, Math.max(0, topMargin));                    
        popup.setPreferredH(popupHeight);
        popup.setPreferredW(getWidth());
        popup.setHeight(popupHeight);
        popup.setWidth(getWidth());
        popup.addComponent(l);
        popup.layoutContainer();
        //block the reflow of this popup, which can cause painting problems
        dontCalcSize = true;
        
        if (f != null) {
            if (popup.getParent() == null) {
                Container lay = f.getLayeredPane(AutoCompleteTextField.this.getClass(), true);
                lay.setLayout(new LayeredLayout());
                Container wrapper = new Container();
                wrapper.add(popup);
                lay.addComponent(wrapper);
            }
            f.revalidate();
        }
    }

    /**
     * Indicates the minimum length of text in the field in order for a popup to show
     * the default is 0 where a popup is shown immediately for all text length if the number
     * is 2 a popup will only appear when there are two characters or more.
     * @return the minimumLength
     */
    public int getMinimumLength() {
        return minimumLength;
    }

    /**
     * Indicates the minimum length of text in the field in order for a popup to show
     * the default is 0 where a popup is shown immediately for all text length if the number
     * is 2 a popup will only appear when there are two characters or more.
     * @param minimumLength the minimumLength to set
     */
    public void setMinimumLength(int minimumLength) {
        this.minimumLength = minimumLength;
    }

    /**
     * The number of elements shown for the auto complete popup
     * @return the minimumElementsShownInPopup
     */
    public int getMinimumElementsShownInPopup() {
        return minimumElementsShownInPopup;
    }

    /**
     * The number of elements shown for the auto complete popup
     * @param minimumElementsShownInPopup the minimumElementsShownInPopup to set
     */
    public void setMinimumElementsShownInPopup(int minimumElementsShownInPopup) {
        this.minimumElementsShownInPopup = minimumElementsShownInPopup;
    }

    class FormPointerListener implements ActionListener {

        public void actionPerformed(final ActionEvent evt) {
            final Form f = getComponentForm();
            Container layered = f.getLayeredPane(AutoCompleteTextField.this.getClass(), true);
            
            boolean canOpenPopup = true;
            
            for (int i = 0; i < layered.getComponentCount(); i++) {
                Container wrap = (Container) layered.getComponentAt(i);
                Component pop = wrap.getComponentAt(0);
                if(pop.isVisible()){
                    if(!pop.contains(evt.getX(), evt.getY())){
                        pop.setVisible(false);
                        pop.setEnabled(false);      
                        f.repaint();
                        evt.consume();
                    }else{
                        canOpenPopup = false;
                    }
                }
            }
            
            if(!canOpenPopup){
                return;
            }
            
            if (contains(evt.getX(), evt.getY())) {
                popup.getComponentAt(0).setScrollY(0);
                popup.setVisible(true);
                popup.setEnabled(true);
                popup.repaint();
                evt.consume();                
                f.revalidate();
                Display.getInstance().callSerially(new Runnable() {

                    public void run() {
                        pointerReleased(evt.getX(), evt.getY());
                    }
                });
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public String[] getPropertyNames() {
        return new String[] {"completion"};
    }

    /**
     * {@inheritDoc}
     */
    public Class[] getPropertyTypes() {
       return new Class[] {com.codename1.impl.CodenameOneImplementation.getStringArrayClass()};
    }
    
    /**
     * {@inheritDoc}
     */
    public String[] getPropertyTypeNames() {
        return new String[] {"String[]"};
    }

    /**
     * {@inheritDoc}
     */
    public Object getPropertyValue(String name) {
        if(name.equals("completion")) {
            return getCompletion();
        }
        return null;
    }

    /**
     * Sets the completion values
     * @param completion the completion values
     */
    public void setCompletion(String... completion) {
        filter = new FilterProxyListModel<String>(new DefaultListModel<String>(completion));        
    }
    
    /**
     * Returns the completion values
     * @return array of completion entries
     */
    public String[] getCompletion() {
        String[] r = new String[filter.getUnderlying().getSize()];
        int rlen = r.length;
        for(int iter = 0 ; iter < rlen ; iter++) {
            r[iter] = (String)filter.getUnderlying().getItemAt(iter);
        }
        return r;
    }
    
    /**
     * {@inheritDoc}
     */
    public String setPropertyValue(String name, Object value) {
        if(name.equals("completion")) {
            filter = new FilterProxyListModel<String>(new DefaultListModel<String>((String[])value));
            return null;
        }
        return super.setPropertyValue(name, value);
    }

    /**
     * When enabled this makes the filter check that the string starts with rather than within the index
     * @return the startsWithMode
     */
    public boolean isStartsWithMode() {
        return filter.isStartsWithMode();
    }

    /**
     * When enabled this makes the filter check that the string starts with rather than within the index
     * @param startsWithMode the startsWithMode to set
     */
    public void setStartsWithMode(boolean startsWithMode) {
        filter.setStartsWithMode(startsWithMode);
    }
}
