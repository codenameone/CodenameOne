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
package com.codename1.ui;

import com.codename1.ui.animations.Motion;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.FocusListener;
import com.codename1.ui.events.SelectionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.EventDispatcher;

/**
 * <p>A component that lets the user switch between a group of components by
 * clicking on a tab with a given title and/or icon.</p>
 *
 * <p>
 * Tabs/components are added to a <code>Tabs</code> object by using the
 * <code>addTab</code> and <code>insertTab</code> methods.
 * A tab is represented by an index corresponding
 * to the position it was added in, where the first tab has an index equal to 0
 * and the last tab has an index equal to the tab count minus 1.
 * </p>
 * <p>
 * The <code>Tabs</code> uses a <code>SingleSelectionModel</code>
 * to represent the set of tab indices and the currently selected index.
 * If the tab count is greater than 0, then there will always be a selected
 * index, which by default will be initialized to the first tab.
 * If the tab count is 0, then the selected index will be -1.
 * </p>
 * <p>A simple {@code Tabs} sample looks a bit like this:</p>
 * <script src="https://gist.github.com/codenameone/ba27124a0a25e685b123.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/components-tabs.png" alt="Simple usage of Tabs" />
 * 
 * <p>A common use case for {@code Tabs} is the iOS carousel UI where dots are drawn at the bottom of the 
 * form and swiping is used to move between pages:</p>
 * <script src="https://gist.github.com/codenameone/e981c3f91f98f1515987.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/components-tabs-swipe1.png" alt="Tabs carousel page 1" />
 * <img src="https://www.codenameone.com/img/developer-guide/components-tabs-swipe2.png" alt="Tabs carousel page 2" />
 * 
 *
 * @author Chen Fishbein
 *
 */
public class Tabs extends Container {
    private Container contentPane = new Container(new TabsLayout());
    private boolean eagerSwipeMode;
    
    /**
     * Where the tabs are placed.
     */
    private int tabPlacement;
    private Container tabsContainer;
    private ButtonGroup radioGroup = new ButtonGroup();
    private Component selectedTab;
    private boolean swipeActivated = true;
    
    private ActionListener press, drag, release;
    private Motion slideToDestMotion;
    private int initialX = -1;
    private int initialY = -1;
    private int lastX = -1;
    private boolean dragStarted = false;
    private int activeComponent = 0;
    private int active = 0;
    
    private EventDispatcher focusListeners;
    private EventDispatcher selectionListener;
    private TabFocusListener focusListener;
    private boolean tabsFillRows;
    private boolean tabsGridLayout;
    private int textPosition = -1;
    private boolean changeTabOnFocus;
    private boolean changeTabContainerStyleOnFocus;
    private int tabsGap = 0;
    private Style originalTabsContainerUnselected, originalTabsContainerSelected;
    private String tabUIID = "Tab";
    private boolean animateTabSelection = true;
    
    /**
     * Creates an empty <code>TabbedPane</code> with a default
     * tab placement of <code>Component.TOP</code>.
     */
    public Tabs() {
        this(-1);
    }

    /**
     * Creates an empty <code>TabbedPane</code> with the specified tab placement
     * of either: <code>Component.TOP</code>, <code>Component.BOTTOM</code>,
     * <code>Component.LEFT</code>, or <code>Component.RIGHT</code>.
     *
     * @param tabP the placement for the tabs relative to the content
     */
    public Tabs(int tabP) {
        super(new BorderLayout());
        focusListener = new TabFocusListener();
        contentPane.setUIID("TabbedPane");
        super.addComponent(BorderLayout.CENTER, contentPane);
        tabsContainer = new Container();
        tabsContainer.setUIID("TabsContainer");
        tabsContainer.setScrollVisible(false);
        tabsContainer.getStyle().setMargin(0, 0, 0, 0);
        if(tabP == -1){
            setTabPlacement(tabPlacement);
        }else{
            setTabPlacement(tabP);
        }
        press = new SwipeListener(SwipeListener.PRESS);
        drag = new SwipeListener(SwipeListener.DRAG);
        release = new SwipeListener(SwipeListener.RELEASE);
        setUIID("Tabs");
        BorderLayout bd = (BorderLayout)super.getLayout();
        if(bd != null) {
            if(UIManager.getInstance().isThemeConstant("tabsOnTopBool", false)) {
                bd.setCenterBehavior(BorderLayout.CENTER_BEHAVIOR_TOTAL_BELOW);
            } else {
                bd.setCenterBehavior(BorderLayout.CENTER_BEHAVIOR_SCALE);
            }
        }
    }

    private void checkTabsCanBeSeen() {
        if(UIManager.getInstance().isThemeConstant("tabsOnTopBool", false)) {
            for(int iter = 0 ; iter < getTabCount() ; iter++) {
                Component c = getTabComponentAt(iter);
                if(c.isScrollableY()) {
                    if(c.getStyle().getPaddingBottom() < tabsContainer.getPreferredH()) {
                        c.getStyle().setPadding(BOTTOM, tabsContainer.getPreferredH());
                    }
                }
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    protected void initLaf(UIManager manager) {
        super.initLaf(manager);
        int tabPlace = manager.getThemeConstant("tabPlacementInt", -1);
        tabsFillRows = manager.isThemeConstant("tabsFillRowsBool", false);
        tabsGridLayout = manager.isThemeConstant("tabsGridBool", false);
        changeTabOnFocus = manager.isThemeConstant("changeTabOnFocusBool", false);
        BorderLayout bd = (BorderLayout)super.getLayout();
        if(bd != null) {
            if(manager.isThemeConstant("tabsOnTopBool", false)) {
                if(bd.getCenterBehavior() != BorderLayout.CENTER_BEHAVIOR_TOTAL_BELOW) {
                    bd.setCenterBehavior(BorderLayout.CENTER_BEHAVIOR_TOTAL_BELOW);
                    checkTabsCanBeSeen();
                }
            } else {
                bd.setCenterBehavior(BorderLayout.CENTER_BEHAVIOR_SCALE);
            }
        }
        changeTabContainerStyleOnFocus =  manager.isThemeConstant("changeTabContainerStyleOnFocusBool", false);
        if(tabPlace != -1){
            tabPlacement = tabPlace;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    void initComponentImpl() {
        super.initComponentImpl();
        getComponentForm().registerAnimatedInternal(this);
        if(changeTabContainerStyleOnFocus && Display.getInstance().shouldRenderSelection()) {
            Component f = getComponentForm().getFocused();
            if(f != null && f.getParent() == tabsContainer) {
                initTabsContainerStyle();
                tabsContainer.setUnselectedStyle(originalTabsContainerSelected);
                tabsContainer.repaint();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void refreshTheme(boolean merge) {
        super.refreshTheme(merge);
        originalTabsContainerSelected = null;
        originalTabsContainerUnselected = null;
    }

    /**
     * {@inheritDoc}
     */
    protected void deinitialize() {
        Form form = this.getComponentForm();
        if (form != null) {
            form.removePointerPressedListener(press);
            form.removePointerReleasedListener(release);
            form.removePointerDraggedListener(drag);
        }
        super.deinitialize();
    }

    /**
     * {@inheritDoc}
     */
    protected void initComponent() {
        super.initComponent();
        Form form = this.getComponentForm();
        if (form != null && swipeActivated) {
            form.addPointerPressedListener(press);
            form.addPointerReleasedListener(release);
            form.addPointerDraggedListener(drag);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean animate() {
        boolean b = super.animate();
        if (slideToDestMotion != null && swipeActivated || slideToDestMotion != null) {
            int motionX = slideToDestMotion.getValue();
            final int size = contentPane.getComponentCount();
            int tabWidth = contentPane.getWidth() - tabsGap*2;
            for (int i = 0; i < size; i++) {
                int xOffset;
                if(isRTL()) {
                    xOffset = (size - i) * tabWidth;
                    xOffset -= ((size - active) * tabWidth);
                } else {
                    xOffset = i * tabWidth;
                    xOffset -= (active * tabWidth);
                }
                xOffset += motionX;
                Component component = contentPane.getComponentAt(i);
                component.setX(xOffset);
            }
            if (slideToDestMotion.isFinished()) {
                for (int i = 0; i < contentPane.getComponentCount() ; i++) {
                    Component component = contentPane.getComponentAt(i);
                    component.paintLockRelease();
                }
                slideToDestMotion = null;
                enableLayoutOnPaint = true;
                deregisterAnimatedInternal();
                setSelectedIndex(active);
            }
            return true;
        }
        return b;
    }

    void deregisterAnimatedInternal() {
        if (slideToDestMotion == null || (slideToDestMotion.isFinished())) {
            Form f = getComponentForm();
            if (f != null) {
                f.deregisterAnimatedInternal(this);
            }
        }
    }

    /**
     * Sets the position of the text relative to the icon if exists
     *
     * @param textPosition alignment value (LEFT, RIGHT, BOTTOM or TOP)
     * @see #LEFT
     * @see #RIGHT
     * @see #BOTTOM
     * @see #TOP
     */
    public void setTabTextPosition(int textPosition) {
        if (textPosition != LEFT && textPosition != RIGHT && textPosition != BOTTOM && textPosition != TOP) {
            throw new IllegalArgumentException("Text position can't be set to " + textPosition);
        }
        this.textPosition = textPosition;
        for(int iter = 0 ; iter < getTabCount() ; iter++) {
            setTextPosition(tabsContainer.getComponentAt(iter), textPosition);
        }
    }

    /**
     * Invokes set text position on the given tab, the tab should be a toggle button radio by default but
     * can be anything
     * @param tabComponent the component representing the tab
     * @param textPosition  the text position
     */
    protected void setTextPosition(Component tabComponent, int textPosition) {
        ((Button)tabComponent).setTextPosition(textPosition);
    }
    

    /**
     * Returns The position of the text relative to the icon
     *
     * @return The position of the text relative to the icon, one of: LEFT, RIGHT, BOTTOM, TOP
     * @see #LEFT
     * @see #RIGHT
     * @see #BOTTOM
     * @see #TOP
     */
    public int getTabTextPosition(){
        return textPosition;
    }

    /**
     * Sets the tab placement for this tabbedpane.
     * Possible values are:<ul>
     * <li><code>Component.TOP</code>
     * <li><code>Component.BOTTOM</code>
     * <li><code>Component.LEFT</code>
     * <li><code>Component.RIGHT</code>
     * </ul>
     * The default value, if not set, is <code>Component.TOP</code>.
     *
     * @param tabPlacement the placement for the tabs relative to the content
     */
    public void setTabPlacement(int tabPlacement) {
        if (tabPlacement != TOP && tabPlacement != LEFT &&
                tabPlacement != BOTTOM && tabPlacement != RIGHT) {
            throw new IllegalArgumentException("illegal tab placement: must be TOP, BOTTOM, LEFT, or RIGHT");
        }
        if (this.tabPlacement == tabPlacement && tabsContainer.getParent() == null && isInitialized()) {
            return;
        }
        this.tabPlacement = tabPlacement;
        removeComponent(tabsContainer);

        setTabsLayout(tabPlacement);

        if (tabPlacement == TOP) {
            super.addComponent(BorderLayout.NORTH, tabsContainer);
        } else if (tabPlacement == BOTTOM) {
            super.addComponent(BorderLayout.SOUTH, tabsContainer);
        } else if (tabPlacement == LEFT) {
            super.addComponent(BorderLayout.WEST, tabsContainer);
        } else {// RIGHT
            super.addComponent(BorderLayout.EAST, tabsContainer);
        }

        initTabsFocus();

        tabsContainer.setShouldCalcPreferredSize(true);
        contentPane.setShouldCalcPreferredSize(true);

        revalidate();
    }

    /**
     * Adds a <code>component</code>
     * represented by a <code>title</code> and/or <code>icon</code>,
     * either of which can be <code>null</code>.
     * Cover method for <code>insertTab</code>.
     *
     * @param title the title to be displayed in this tab
     * @param icon the icon to be displayed in this tab
     * @param component the component to be displayed when this tab is clicked
     *
     * @see #insertTab
     * @see #removeTabAt
     */
    public void addTab(String title, Image icon, Component component) {
        insertTab(title, icon, component, tabsContainer.getComponentCount());
    }

    /**
     * Adds a <code>component</code>
     * represented by a <code>title</code> and/or <code>icon</code>,
     * either of which can be <code>null</code>.
     * Cover method for <code>insertTab</code>.
     *
     * @param title the title to be displayed in this tab
     * @param icon the icon to be displayed in this tab
     * @param pressedIcon the icon shown when the tab is selected
     * @param component the component to be displayed when this tab is clicked
     * @return this so these calls can be chained
     *
     * @see #insertTab
     * @see #removeTabAt
     */
    public Tabs addTab(String title, Image icon, Image pressedIcon, Component component) {
        int index = tabsContainer.getComponentCount();
        insertTab(title, icon, component, index);
        setTabSelectedIcon(index, pressedIcon);
        return this;
    }
    
    /**
     * Adds a <code>component</code>
     * represented by a <code>title</code> and/or <code>icon</code>,
     * either of which can be <code>null</code>.
     * Cover method for <code>insertTab</code>.
     *
     * @param title the title to be displayed in this tab
     * @param materialIcon one of the material design icon constants from {@link com.codename1.ui.FontImage}
     * @param iconSize icon size in millimeters 
     * @param component the component to be displayed when this tab is clicked
     * @return this so these calls can be chained
     *
     * @see #insertTab
     * @see #removeTabAt
     */
    public Tabs addTab(String title, char materialIcon, float iconSize, Component component) {
        int index = tabsContainer.getComponentCount();
        FontImage i = FontImage.createMaterial(materialIcon, "Tab", iconSize);
        insertTab(title, i, component, index);
        Style sel = getUIManager().getComponentSelectedStyle("Tab");
        i = FontImage.createMaterial(materialIcon, sel, iconSize);
        setTabSelectedIcon(index, i);
        return this;
    }
    

    /**
     * Adds a <code>component</code>
     * represented by a <code>title</code> and no <code>icon</code>.
     * Cover method for <code>insertTab</code>.
     *
     * @param title the title to be displayed in this tab
     * @param component the component to be displayed when this tab is clicked
     *
     * @see #insertTab
     * @see #removeTabAt
     */
    public void addTab(String title, Component component) {
        insertTab(title, null, component, tabsContainer.getComponentCount());
    }
    
    /**
     * Adds a <code>component</code>
     * represented by a <code>button</code>.
     * Cover method for <code>insertTab</code>.
     * The Button styling will be associated with "Tab" UIID.
     *
     * @param tab represents the tab on top
     * @param component the component to be displayed when this tab is clicked
     *
     * @see #insertTab
     * @see #removeTabAt
     * @deprecated should use radio button as an argument
     */
    public void addTab(Button tab, Component component) {
        insertTab(tab, component, tabsContainer.getComponentCount());
    }

    /**
     * Creates a tab component by default this is a RadioButton but subclasses can use this to return anything
     * @param title the title of the tab
     * @param icon the icon of the tab
     * @return component instance
     */
    protected Component createTab(String title, Image icon) {
        RadioButton b = new RadioButton(title != null ? title : "", icon);
        radioGroup.add(b);
        b.setToggle(true);
        b.setTextPosition(BOTTOM);
        if(radioGroup.getButtonCount() == 1) {
            b.setSelected(true);
        }
        if(textPosition != -1) {
            b.setTextPosition(textPosition);
        }

        if(b.getIcon() == null && !getUIManager().isThemeConstant("TabEnableAutoImageBool", true)) {
            Image d = getUIManager().getThemeImageConstant("TabUnselectedImage");
            if(d != null) {
                b.setIcon(d);
                d = getUIManager().getThemeImageConstant("TabSelectedImage");
                if(d != null) {
                    b.setRolloverIcon(d);
                    b.setPressedIcon(d);
                }
            }
        }
        return b;
    }
    
    /**
     * Inserts a <code>component</code>, at <code>index</code>,
     * represented by a <code>title</code> and/or <code>icon</code>,
     * either of which may be <code>null</code>.
     * Uses java.util.Vector internally, see <code>insertElementAt</code>
     * for details of insertion conventions.
     *
     * @param title the title to be displayed in this tab
     * @param icon the icon to be displayed in this tab
     * @param component The component to be displayed when this tab is clicked.
     * @param index the position to insert this new tab
     *
     * @see #addTab
     * @see #removeTabAt
     */
    public void insertTab(String title, Image icon, Component component,
            int index) {
        Component b = createTab(title != null ? title : "", icon);
        insertTab(b, component, index);
    }

    /**
     * Inserts a <code>component</code>, at <code>index</code>,
     * represented by a <code>button</code>
     * Uses java.util.Vector internally, see <code>insertElementAt</code>
     * for details of insertion conventions.
     * The Button styling will be associated with "Tab" UIID.
     *
     * @param tab represents the tab on top
     * @param component The component to be displayed when this tab is clicked.
     * @param index the position to insert this new tab
     *
     * @see #addTab
     * @see #removeTabAt
     * @deprecated should use radio button as an argument
     */
    public void insertTab(Component tab, Component component,
            int index) {
        checkIndex(index);
        if (component == null) {
            return;
        }
        final Component b = tab;
        if(tabUIID != null) {
            b.setUIID(tabUIID);
        }

        b.addFocusListener(focusListener);
        
        bindTabActionListener(b, new ActionListener() {
            public void actionPerformed(ActionEvent evt) {

                if(selectedTab != null){
                    if(tabUIID != null) {
                        selectedTab.setUIID(tabUIID);
                    }
                    if(!animateTabSelection) {
                        selectedTab.setShouldCalcPreferredSize(true);
                        selectedTab.repaint();
                    }
                    int previousSelectedIndex = tabsContainer.getComponentIndex(selectedTab);
                    
                    // this might happen if a tab was removed
                    if(previousSelectedIndex != -1) {
                        Component previousContent = contentPane.getComponentAt(previousSelectedIndex);
                        if (previousContent instanceof Container) {
                            ((Container) previousContent).setBlockFocus(true);
                        }
                    }
                }
                active = tabsContainer.getComponentIndex(b);                
                Component content = contentPane.getComponentAt(active);
                if (content instanceof Container) {
                    ((Container) content).setBlockFocus(false);
                }
                setSelectedIndex(active, animateTabSelection);
                initTabsFocus();
                selectedTab = b;
                if(!animateTabSelection) {
                    selectedTab.setShouldCalcPreferredSize(true);
                    tabsContainer.revalidate();
                }
                tabsContainer.scrollComponentToVisible(selectedTab);
            }
        });

        if (component instanceof Container) {
            ((Container) component).setBlockFocus(true);
        }

        tabsContainer.addComponent(index, b);
        contentPane.addComponent(index, component);
        setTabsLayout(tabPlacement);
        if (tabsContainer.getComponentCount() == 1) {
            selectedTab = tabsContainer.getComponentAt(0);
            if (component instanceof Container) {
                ((Container) component).setBlockFocus(false);
            }
            initTabsFocus();
        }
        checkTabsCanBeSeen();
    }

    /**
     * Binds an action listener to the tab component. this method should be used when overriding
     * createTab
     * @param tab the tab component
     * @param l the listener
     */
    protected void bindTabActionListener(Component tab, ActionListener l) {
        ((Button)tab).addActionListener(l);
    }
    
    /**
     * Updates the information about the tab details
     *
     * @param title the title to be displayed in this tab
     * @param icon the icon to be displayed in this tab
     * @param index the position to insert this new tab
     */
    public void setTabTitle(String title, Image icon, int index) {
        checkIndex(index);
        setTabTitle(tabsContainer.getComponentAt(index), title, icon);
    }

    /**
     * Updates the tabs title . This method should be used when overriding
     * createTab
     * @param tab the tab component
     * @param title the title
     * @param icon  the new icon
     */
    protected void setTabTitle(Component tab, String title, Image icon) {
        Button b = (Button)tab;
        b.setText(title);
        b.setIcon(icon);
    }
    
    /**
     * Returns the title of the tab at the given index
     * 
     * @param index index for the tab
     * @return label of the tab at the given index
     */
    public String getTabTitle(int index) {
        checkIndex(index);
        return getTabTitle(tabsContainer.getComponentAt(index));
    }

    /**
     * Returns the title of the tab component. This method should be used when overriding
     * createTab
     * 
     * @param tab the tab component
     * @return label of the tab 
     */
    protected String getTabTitle(Component tab) {
        return ((Button)tab).getText();
    }    

    /**
     * Returns the icon of the tab component. This method should be used when overriding
     * createTab
     * 
     * @param tab the tab component
     * @return icon of the tab 
     */
    protected Image getTabIcon(Component tab) {
        return ((Button)tab).getIcon();
    }    
    
    /**
     * Returns the icon of the tab at the given index
     *
     * @param index index for the tab
     * @return icon of the tab at the given index
     */
    public Image getTabIcon(int index) {
        checkIndex(index);
        return getTabIcon(tabsContainer.getComponentAt(index));
    }

    /**
     * Returns the selected icon of the tab component. This method should be used when overriding
     * createTab
     * 
     * @param tab the tab component
     * @return icon of the tab 
     */
    protected Image getTabSelectedIcon(Component tab) {
        return ((Button)tab).getPressedIcon();
    }    
    

    /**
     * Returns the icon of the tab at the given index
     *
     * @param index index for the tab
     * @return icon of the tab at the given index
     */
    public Image getTabSelectedIcon(int index) {
        checkIndex(index);
        return getTabSelectedIcon(tabsContainer.getComponentAt(index));
    }

    /**
     * Sets the selected icon of the tab at the given index
     *
     * @param index index for the tab
     * @param icon of the tab at the given index
     */
    public void setTabSelectedIcon(int index, Image icon) {
        checkIndex(index);
        setTabSelectedIcon(tabsContainer.getComponentAt(index), icon);
    }

    /**
     * Sets the selected icon of the tab. This method should be used when overriding
     * createTab
     *
     * @param tab the tab component
     * @param icon of the tab 
     */
    protected void setTabSelectedIcon(Component tab, Image icon) {
        ((Button)tab).setPressedIcon(icon);
    }

    /**
     * Removes the tab at <code>index</code>.
     * After the component associated with <code>index</code> is removed,
     * its visibility is reset to true to ensure it will be visible
     * if added to other containers.
     * @param index the index of the tab to be removed
     * @exception IndexOutOfBoundsException if index is out of range
     *            (index < 0 || index >= tab count)
     *
     * @see #addTab
     * @see #insertTab
     */
    public void removeTabAt(int index) {
        checkIndex(index);
        int act = activeComponent - 1;
        act = Math.max(act, 0);
        setSelectedIndex(act);
        Component key = tabsContainer.getComponentAt(index);
        tabsContainer.removeComponent(key);
        Component content = contentPane.getComponentAt(index);
        contentPane.removeComponent(content);
        setTabsLayout(tabPlacement);
    }

    /**
     * Returns the tab at <code>index</code>.
     *
     * @param index the index of the tab to be removed
     * @exception IndexOutOfBoundsException if index is out of range
     *            (index < 0 || index >= tab count)
     * @return the component at the given tab location
     * @see #addTab
     * @see #insertTab
     */
    public Component getTabComponentAt(int index) {
        checkIndex(index);
        return contentPane.getComponentAt(index);
    }

    private void checkIndex(int index) {
        if (index < 0 || index > tabsContainer.getComponentCount()) {
            throw new IndexOutOfBoundsException("Index: " + index);
        }
    }

    /**
     * Returns the index of the tab for the specified component.
     * Returns -1 if there is no tab for this component.
     *
     * @param component the component for the tab
     * @return the first tab which matches this component, or -1
     *		if there is no tab for this component
     */
    public int indexOfComponent(Component component) {
        return contentPane.getComponentIndex(component);
    }

    /**
     * Returns the number of tabs in this <code>tabbedpane</code>.
     *
     * @return an integer specifying the number of tabbed pages
     */
    public int getTabCount() {
        return tabsContainer.getComponentCount();
    }

    /**
     * Returns the currently selected index for this tabbedpane.
     * Returns -1 if there is no currently selected tab.
     *
     * @return the index of the selected tab
     */
    public int getSelectedIndex() {
        if(tabsContainer != null){
            return activeComponent;
        }
        return -1;
    }

    /**
     * Returns the component associated with the tab at the given index
     *
     * @return the component is now showing in the tabbed pane
     */
    public Component getSelectedComponent() {
        int i = getSelectedIndex();
        if(i == -1) {
            return null;
        }
        return getTabComponentAt(i);
    }

    /**
     * Adds a focus listener to the tabs buttons
     * 
     * @deprecated use addSelectionListener instead
     * @param listener FocusListener
     */
    public void addTabsFocusListener(FocusListener listener){
        if(focusListeners == null){
            focusListeners = new EventDispatcher();
        }
        focusListeners.addListener(listener);
    }
    
    
     
    /**
     * Removes a foucs Listener from the tabs buttons
     * 
     * @deprecated use addSelectionListener instead
     * @param listener FocusListener
     */
    public void removeTabsFocusListener(FocusListener listener){
        if(focusListeners != null){
            focusListeners.removeListener(listener);
        }
    }
    
    /**
     * Adds a selection listener to the tabs.
     * 
     * @param listener SelectionListener
     */
    public void addSelectionListener(SelectionListener listener){
        if(selectionListener == null){
            selectionListener = new EventDispatcher();
        }
        selectionListener.addListener(listener);
    }
    
    
     
    /**
     * Removes a selection Listener from the tabs
     * 
     * @param listener SelectionListener
     */
    public void removeSelectionListener(SelectionListener listener){
        if(selectionListener != null){
            selectionListener.removeListener(listener);
        }
    }
    

    /**
     * {@inheritDoc}
     */
    public String toString() {
        String className = getClass().getName();
        className = className.substring(className.lastIndexOf('.') + 1);
        return className + "[x=" + getX() + " y=" + getY() + " width=" +
                getWidth() + " height=" + getHeight() + ", tab placement = " +
                tabPlacement + ", tab count = " + getTabCount() +
                ", selected index = " + getSelectedIndex() + "]";
    }

    /**
     * Returns the placement of the tabs for this tabbedpane.
     *
     * @return the tab placement value
     * @see #setTabPlacement
     */
    public int getTabPlacement() {
        return tabPlacement;
    }

    /**
     * This method retrieves the Tabs content pane
     *
     * @return the content pane Container
     */
    public Container getContentPane(){
        return contentPane;
    }

    /**
     * This method retrieves the Tabs buttons Container
     *
     * @return the Tabs Container
     */
    public Container getTabsContainer(){
        return tabsContainer;
    }

    /**
     * Sets the currently selected index in the tabs component 
     * @param index the index for the tab starting with tab 0. 
     * @param slideToSelected true to animate the transition to the new selection
     * false to just move immediately
     */
    public void setSelectedIndex(int index, boolean slideToSelected) {        
        if (index < 0 || index >= tabsContainer.getComponentCount()) {
            throw new IndexOutOfBoundsException("Index: "+index+", Tab count: "+tabsContainer.getComponentCount());
        }
        if(index == activeComponent){
            return;
        }
        
        Form form = getComponentForm();
        if(slideToSelected && form != null){
            int end = contentPane.getComponentAt(activeComponent).getX();
            int start = contentPane.getComponentAt(index).getX();
            slideToDestMotion = createTabSlideMotion(start, end);
            slideToDestMotion.start();
            form.registerAnimatedInternal(Tabs.this);
            active = index;
        }else{
            if(selectionListener != null){
                selectionListener.fireSelectionEvent(activeComponent, index);
            }
            activeComponent = index;
            selectTab(tabsContainer.getComponentAt(index));
            int offset = 0;
            for(Component c : contentPane) {
                c.setLightweightMode(offset == index);
                offset++;
            }
            revalidate();
        }
    }
    
    /**
     * Invoked to select a specific tab, this method should be overriden for subclasses overriding createTab
     * @param tab the tab
     */
    protected void selectTab(Component tab) {
            Button b = (Button)tab;
            b.fireClicked();
            b.requestFocus();        
    }
    
    /**
     * Sets the selected index for this tabbedpane. The index must be a valid
     * tab index.
     * @param index the index to be selected
     * @throws IndexOutOfBoundsException if index is out of range
     * (index < 0 || index >= tab count)
     */
    public void setSelectedIndex(int index) {        
        setSelectedIndex(index, false);
    }

    /**
     * Hide the tabs bar
     */
    public void hideTabs(){
        removeComponent(tabsContainer);
        revalidate();
    }
    
    /**
     * Show the tabs bar if it was hidden
     */
    public void showTabs(){
        int tp = tabPlacement;
        tabPlacement = -1;
        setTabPlacement(tp);
        revalidate();
    }

    /**
     * Returns true if the swipe between tabs is activated, this is relevant for
     * touch devices only
     *
     * @return swipe activated flag
     */
    public boolean isSwipeActivated() {
        return swipeActivated;
    }

    /**
     * Setter method for swipe mode
     *
     * @param swipeActivated
     */
    public void setSwipeActivated(boolean swipeActivated) {
        this.swipeActivated = swipeActivated;
    }

    private void initTabsFocus(){
        for (int i = 0; i < tabsContainer.getComponentCount(); i++) {
            initTabFocus(tabsContainer.getComponentAt(i), contentPane.getComponentAt(activeComponent));
        }

    }

    private void initTabFocus(Component tab, Component content) {
        Component focus = null;
        if (content.isFocusable()) {
            focus = content;
        }

        if (content instanceof Container) {
            focus = ((Container) content).findFirstFocusable();
        }

    }

    /**
     * Indicates that a tab should change when the focus changes without the user physically pressing a button
     * @return the changeTabOnFocus
     */
    public boolean isChangeTabOnFocus() {
        return changeTabOnFocus;
    }

    /**
     * Indicates that a tab should change when the focus changes without the user physically pressing a button
     * @param changeTabOnFocus the changeTabOnFocus to set
     */
    public void setChangeTabOnFocus(boolean changeTabOnFocus) {
        this.changeTabOnFocus = changeTabOnFocus;
    }

    /**
     * Indicates that the tabs container should have its style changed to the selected style when one of the tabs has focus
     * this allows incorporating it into the theme of the application
     * @return the changeTabContainerStyleOnFocus
     */
    public boolean isChangeTabContainerStyleOnFocus() {
        return changeTabContainerStyleOnFocus;
    }

    /**
     * Indicates that the tabs container should have its style changed to the selected style when one of the tabs has focus
     * this allows incorporating it into the theme of the application
     * @param changeTabContainerStyleOnFocus the changeTabContainerStyleOnFocus to set
     */
    public void setChangeTabContainerStyleOnFocus(boolean changeTabContainerStyleOnFocus) {
        this.changeTabContainerStyleOnFocus = changeTabContainerStyleOnFocus;
    }
    
    /**
     * This method allows setting the Tabs content pane spacing (right and left), 
     * This can be used to create an effect where the selected tab is smaller 
     * and the right and left tabs are visible on the sides
     * @param tabsGap the gap on the sides of the content in pixels, the value must
     * be positive.
     */
    public void setTabsContentGap(int tabsGap){
        if(tabsGap < 0){
            throw new IllegalArgumentException("gap must be positive");
        }
        this.tabsGap = tabsGap;
    }

    private void setTabsLayout(int tabPlacement) {
        if (tabPlacement == TOP || tabPlacement == BOTTOM) {
            if(tabsFillRows) {
                FlowLayout f = new FlowLayout();
                f.setFillRows(true);
                tabsContainer.setLayout(f);
            } else {
                if(tabsGridLayout) {
                    tabsContainer.setLayout(new GridLayout(1, Math.max(1 ,getTabCount())));
                } else {
                    tabsContainer.setLayout(new BoxLayout(BoxLayout.X_AXIS));
                }
            }
            tabsContainer.setScrollableX(true);
            tabsContainer.setScrollableY(false);
        } else {// LEFT Or RIGHT
                if(tabsGridLayout) {
                    tabsContainer.setLayout(new GridLayout(Math.max(1 ,getTabCount()), 1));
                } else {
                    tabsContainer.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
                }
            tabsContainer.setScrollableX(false);
            tabsContainer.setScrollableY(true);
        }
    }

    /**
     * The UIID for a tab component which defaults to Tab
     * @return the tabUIID
     */
    public String getTabUIID() {
        return tabUIID;
    }

    /**
     * <p>The UIID for a tab button which defaults to Tab.
     * Tab buttons used to have two separate styles for selected and unselected. This was later consolidated so 
     * the tabs behave as a single toggle button (radio button) however one thing that remained is a call to 
     * <code>setUIID</code> that is implicitly made to restore the original "Tab" style. </p>
     * 
     * <p>Effectively Tabs invokes the <code>setUIID</code> call on the Tab switch so if you want to manipulate
     * the tab UIID manually (have one red and one green tab) this is a problem.</>.
     * <p>To enable such code add all the tabs then just just invoke <code>setTabUIID(null)</code> to disable 
     * this behavior.</p>
     * 
     * @param tabUIID the tabUIID to set
     */
    public void setTabUIID(String tabUIID) {
        this.tabUIID = tabUIID;
    }

    /**
     * Allows marking tabs as swipe "eager" which instantly triggers swipe on movement
     * rather than threshold the swipe.
     * @return the eagerSwipeMode
     */
    public boolean isEagerSwipeMode() {
        return eagerSwipeMode;
    }

    /**
     * Allows marking tabs as swipe "eager" which instantly triggers swipe on movement
     * rather than threshold the swipe.
     * @param eagerSwipeMode the eagerSwipeMode to set
     */
    public void setEagerSwipeMode(boolean eagerSwipeMode) {
        this.eagerSwipeMode = eagerSwipeMode;
    }

    /**
     * Indicates whether clicking on a tab button should result in an animation to the selected tab or an immediate switch
     * @return the animateTabSelection
     */
    public boolean isAnimateTabSelection() {
        return animateTabSelection;
    }

    /**
     * Indicates whether clicking on a tab button should result in an animation to the selected tab or an immediate switch
     * @param animateTabSelection the animateTabSelection to set
     */
    public void setAnimateTabSelection(boolean animateTabSelection) {
        this.animateTabSelection = animateTabSelection;
    }


    class TabsLayout extends Layout{

        public void layoutContainer(Container parent) {
            final int size = parent.getComponentCount();
           
            int tabWidth = parent.getWidth() - tabsGap*2;
            for (int i = 0; i < size; i++) {
                int xOffset;
                if(parent.isRTL()) {
                    xOffset = (size - i) * tabWidth + tabsGap;
                    xOffset -= ((size - activeComponent) * tabWidth);
                } else {
                    xOffset = i * tabWidth + tabsGap;
                    xOffset -= (activeComponent * tabWidth);
                }
                Component component = parent.getComponentAt(i);
                component.setX(component.getStyle().getMarginLeftNoRTL() + xOffset);
                component.setY(component.getStyle().getMarginTop());
                component.setWidth(tabWidth - component.getStyle().getHorizontalMargins());
                component.setHeight(parent.getHeight() - component.getStyle().getVerticalMargins());
            }

        }

        public Dimension getPreferredSize(Container parent) {
            // fill
            Dimension dim = new Dimension(0, 0);
            dim.setWidth(parent.getWidth() + parent.getStyle().getPaddingLeftNoRTL()
                    + parent.getStyle().getPaddingRightNoRTL());
            dim.setHeight(parent.getHeight() + parent.getStyle().getPaddingTop()
                    + parent.getStyle().getPaddingBottom());
            int compCount = contentPane.getComponentCount();
            for(int iter = 0 ; iter < compCount ; iter++) {
                Dimension d = contentPane.getComponentAt(iter).getPreferredSizeWithMargin();
                dim.setWidth(Math.max(d.getWidth(), dim.getWidth()));
                dim.setHeight(Math.max(d.getHeight(), dim.getHeight()));
            }
            return dim;
        }
    }

    void initTabsContainerStyle() {
        if(originalTabsContainerSelected == null) {
            originalTabsContainerSelected = tabsContainer.getSelectedStyle();
            originalTabsContainerUnselected = tabsContainer.getUnselectedStyle();
        }
    }
    
    class TabFocusListener implements FocusListener{

        public void focusGained(Component cmp) {
            if(focusListeners != null){
                focusListeners.fireFocus(cmp);
            }
            if(Display.getInstance().shouldRenderSelection()) {
                if(isChangeTabOnFocus()) {
                    if(!((Button)cmp).isSelected()) {
                        ((Button)cmp).fireClicked();
                    }
                }
                if(changeTabContainerStyleOnFocus) {
                    initTabsContainerStyle();
                    tabsContainer.setUnselectedStyle(originalTabsContainerSelected);
                    tabsContainer.repaint();
                }
            }
        }


        public void focusLost(Component cmp) {
            if(focusListeners != null){
                focusListeners.fireFocus(cmp);
            }
            if(changeTabContainerStyleOnFocus) {
                initTabsContainerStyle();
                tabsContainer.setUnselectedStyle(originalTabsContainerUnselected);
                tabsContainer.repaint();
            }
        }
    
    }
    
    /**
     * Allows developers to customize the motion object for the slide effect
     * to provide a linear slide effect. You can use the {@code tabsSlideSpeedInt} 
     * theme constant to define the time in milliseconds between releasing the swiped
     * tab and reaching the next tab. This currently defaults to 200.
     * @param start start position
     * @param end end position for the motion
     * @return the motion object
     */
    protected Motion createTabSlideMotion(int start, int end) {
        return Motion.createSplineMotion(start, end, getUIManager().getThemeConstant("tabsSlideSpeedInt", 200));
    }
    
    class SwipeListener implements ActionListener{

        private final static int PRESS = 0;
        private final static int DRAG = 1;
        private final static int RELEASE = 2;
        private final int type;
        private boolean blockSwipe;
        private boolean riskySwipe;

        public SwipeListener(int type) {
            this.type = type;
        }

        public void actionPerformed(ActionEvent evt) {
            
            if (getComponentCount() == 0 || !swipeActivated ||animate()) {
                return;
            }
            final int x = evt.getX();
            final int y = evt.getY();
            switch (type) {
                case PRESS: {
                    blockSwipe = false;
                    riskySwipe = false;
                    if (contentPane.contains(x, y)) {
                        Component testCmp = contentPane.getComponentAt(x, y);
                        if(testCmp != null && testCmp != contentPane) {
                            while(testCmp != null && testCmp != contentPane) {
                                if(testCmp.shouldBlockSideSwipe()) {
                                    lastX = -1;
                                    initialX = -1;
                                    blockSwipe = true;
                                    return;
                                }
                                if(testCmp.isScrollable()) {
                                    if(testCmp.isScrollableX()) {
                                        // we need to block swipe since the user is trying to scroll a component
                                        lastX = -1;
                                        initialX = -1;
                                        blockSwipe = true;
                                        return;
                                    }

                                    // scrollable Y component, we want to make side scrolling
                                    // slightly harder so it doesn't bother the vertical swipe
                                    riskySwipe = true;
                                    break;
                                }
                                testCmp = testCmp.getParent();
                            }
                        }
                        lastX = x;
                        initialX = x;
                        initialY = y;
                    } else {
                        lastX = -1;
                        initialX = -1;
                        initialY = -1;
                    }
                    dragStarted = false;
                    break;
                }
                case DRAG: {
                    if(blockSwipe) {
                        return;
                    }
                    if (!dragStarted) {
                        if(isEagerSwipeMode()) {
                            dragStarted = true;
                        } else {
                            if(riskySwipe) {
                                if(Math.abs(x - initialX) < Math.abs(y - initialY)) {
                                    return;
                                }
                                // give heavier weight when we have two axis swipe
                                dragStarted = Math.abs(x - initialX) > (contentPane.getWidth() / 5);
                            } else {
                                // start drag not imediately, giving components some sort
                                // of weight.
                                dragStarted = Math.abs(x - initialX) > (contentPane.getWidth() / 8);
                            }
                        }
                    } 
                    if (initialX != -1 && contentPane.contains(x, y)) {
                        int diffX = x - lastX;
                        if (diffX != 0 && dragStarted) {
                            lastX += diffX;
                            final int size = contentPane.getComponentCount();
                            for (int i = 0; i < size; i++) {
                                Component component = contentPane.getComponentAt(i);
                                component.setX(component.getX() + diffX);
                                component.paintLock(false);
                            }
                            enableLayoutOnPaint = false;
                            repaint();
                        }
                    }
                    break;
                }
                case RELEASE: {
                    if(changeTabContainerStyleOnFocus) {
                        initTabsContainerStyle();
                        tabsContainer.setUnselectedStyle(originalTabsContainerUnselected);
                        tabsContainer.repaint();
                    }
                    if(blockSwipe) {
                        return;
                    }
                    if (initialX != -1) {
                        int diff = x - initialX;
                        if (diff != 0 && dragStarted) {
                            if (Math.abs(diff) > contentPane.getWidth() / 6) {
                                if(isRTL()) {
                                    diff *= -1;
                                }
                                if (diff > 0) {
                                    active = activeComponent - 1;
                                    if (active < 0) {
                                        active = 0;
                                    }
                                } else {
                                    active = activeComponent + 1;
                                    if (active >= contentPane.getComponentCount()) {
                                        active = contentPane.getComponentCount() - 1;
                                    }
                                }
                            }
                            int start = contentPane.getComponentAt(active).getX();
                            int end = tabsGap;
                            slideToDestMotion = createTabSlideMotion(start, end);
                            slideToDestMotion.start();
                            Form form = getComponentForm();
                            if (form != null) {
                                form.registerAnimatedInternal(Tabs.this);
                            }
                            evt.consume();
                        }
                    }
                    lastX = -1;
                    initialX = -1;
                    dragStarted = false;
                    break;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public String[] getPropertyNames() {
        return new String[] {"titles", "icons", "selectedIcons"};
    }

    /**
     * {@inheritDoc}
     */
    public Class[] getPropertyTypes() {
       return new Class[] {com.codename1.impl.CodenameOneImplementation.getStringArrayClass(), 
           com.codename1.impl.CodenameOneImplementation.getImageArrayClass(), 
           com.codename1.impl.CodenameOneImplementation.getImageArrayClass()};
    }
    
    /**
     * {@inheritDoc}
     */
    public String[] getPropertyTypeNames() {
        return new String[] {"String[]", "Image[]", "Image[]"};
    }

    /**
     * {@inheritDoc}
     */
    public Object getPropertyValue(String name) {
        if(name.equals("titles")) {
            String[] t = new String[getTabCount()];
            for(int iter = 0 ; iter < t.length ; iter++) {
                t[iter] = getTabTitle(iter);
            }
            return t;
        }
        if(name.equals("icons")) {
            Image[] t = new Image[getTabCount()];
            for(int iter = 0 ; iter < t.length ; iter++) {
                t[iter] = getTabIcon(iter);
            }
            return t;
        }
        if(name.equals("selectedIcons")) {
            Image[] t = new Image[getTabCount()];
            for(int iter = 0 ; iter < t.length ; iter++) {
                t[iter] = getTabSelectedIcon(iter);
            }
            return t;
        }
        return null;
    }


    /**
     * {@inheritDoc}
     */
    public String setPropertyValue(String name, Object value) {
        if(name.equals("titles")) {
            String[] t = (String[])value;
            for(int iter = 0 ; iter < Math.min(getTabCount(), t.length) ; iter++) {
                setTabTitle(t[iter], getTabIcon(iter), iter);
            }
            return null;
        }
        if(name.equals("icons")) {
            Image[] t = (Image[])value;
            if(t == null) {
                for(int iter = 0 ; iter < getTabCount() ; iter++) {
                    setTabTitle(getTabTitle(iter), null, iter);
                }
            } else {
                for(int iter = 0 ; iter < Math.min(getTabCount(), t.length) ; iter++) {
                    setTabTitle(getTabTitle(iter), t[iter], iter);
                }
            }
            return null;
        }
        if(name.equals("selectedIcons")) {
            Image[] t = (Image[])value;
            for(int iter = 0 ; iter < Math.min(getTabCount(), t.length) ; iter++) {
                setTabSelectedIcon(iter, t[iter]);
            }
            return null;
        }
        return super.setPropertyValue(name, value);
    }
}
