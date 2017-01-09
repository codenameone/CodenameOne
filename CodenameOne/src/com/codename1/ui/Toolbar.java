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

import com.codename1.ui.animations.BubbleTransition;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.animations.Motion;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.ScrollListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.list.DefaultListCellRenderer;
import com.codename1.ui.plaf.LookAndFeel;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import java.util.ArrayList;
import java.util.Vector;

/**
 * <p>Toolbar replaces the default title area with a powerful abstraction that allows functionality ranging
 * from side menus (hamburger) to title animations and any arbitrary component type. Toolbar allows
 * customizing the Form title with different commands on the title area, within the side menu or the overflow menu.</p>
 * 
 * <p>
 * The Toolbar allows placing components in one of 4 positions as illustrated by the sample below:
 * </p>
 * <script src="https://gist.github.com/codenameone/e72cfa6aedd7fcd1af72.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/components-toolbar.png" alt="Simple usage of Toolbar" />
 *  
 * <p>
 * {@code Toolbar} supports a search mode that implicitly replaces the title with a search field/magnifying glass
 * effect. The code below demonstrates searching thru the contacts using this API:
 * </p>
 * <script src="https://gist.github.com/codenameone/cd227aaca486889f7c940e2e97985426.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/toolbar-search-mode.jpg" alt="Dynamic search mode in the Toolbar" />
 * 
 * <p>
 * The following code also demonstrates search with a more custom UX where the title
 * area was replaced dynamically. This code predated the builtin search support above. 
 * Notice that the {@code TextField} and its hint are styled to look like the title.
 * </p>
 * <script src="https://gist.github.com/codenameone/dce6598a226aaf9a3157.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/components-toolbar-search.png" alt="Dynamic TextField search using the Toolbar" />
 * 
 * <p>
 * This sample code show off title animations that allow a title to change (and potentially shrink) as the user scrolls
 * down the UI.  The 3 frames below show a step by step process in the change.
 * </p>
 * <script src="https://gist.github.com/codenameone/085e3a8fa1c36829d812.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/components-toolbar-animation-1.png" alt="Toolbar animation stages" />
 * <img src="https://www.codenameone.com/img/developer-guide/components-toolbar-animation-2.png" alt="Toolbar animation stages" />
 * <img src="https://www.codenameone.com/img/developer-guide/components-toolbar-animation-3.png" alt="Toolbar animation stages" />
 * 
 * @author Chen
 */
public class Toolbar extends Container {

    /**
     * Indicates whether the toolbar should be properly centered by default
     * @return the centeredDefault
     */
    public static boolean isCenteredDefault() {
        return centeredDefault;
    }

    /**
     * Indicates whether the toolbar should be properly centered by default
     * @param aCenteredDefault the centeredDefault to set
     */
    public static void setCenteredDefault(boolean aCenteredDefault) {
        centeredDefault = aCenteredDefault;
    }

    private Component titleComponent;

    private ToolbarSideMenu sideMenu;

    private Vector<Command> overflowCommands;

    private Button menuButton;

    private ScrollListener scrollListener;

    private ActionListener releasedListener;

    private boolean scrollOff = false;

    private int initialY;

    private int actualPaneInitialY;

    private int actualPaneInitialH;

    private Motion hideShowMotion;

    private boolean showing;

    private boolean layered = false;
    
    private boolean initialized = false;
    
    private static boolean permanentSideMenu;
    
    private Container permanentSideMenuContainer;

    private static boolean globalToolbar;
    
    /**
     * Indicates whether the toolbar should be properly centered by default
     */
    private static boolean centeredDefault = true;

    private Command searchCommand;
    
    /**
     * Empty Constructor
     */
    public Toolbar() {
        setLayout(new BorderLayout());
        setUIID("Toolbar");
        sideMenu = new ToolbarSideMenu();
        if(centeredDefault && getUnselectedStyle().getAlignment() == CENTER) {
            setTitleCentered(true);
        }
    }
    
    /**
     * Enables/disables the Toolbar for all the forms in the application. This flag can be flipped via the 
     * theme constant {@code globalToobarBool}. Notice that the name of this method might imply that
     * one toolbar instance will be used for all forms which isn't the case, separate instances will be used for each form
     * 
     * @param gt true to enable the toolbar globally
     */
    public static void setGlobalToolbar(boolean gt) {
        globalToolbar = gt;
    }
    
    /**
     * Enables/disables the Toolbar for all the forms in the application. This flag can be flipped via the 
     * theme constant {@code globalToobarBool}. Notice that the name of this method might imply that
     * one toolbar instance will be used for all forms which isn't the case, separate instances will be used for each form
     * 
     * @return  true if the toolbar API is turned on by default
     */
    public static boolean isGlobalToolbar() {
        return globalToolbar;
    }
    
    /**
     * This constructor places the Toolbar on a different layer on top of the 
     * Content Pane.
     * 
     * @param layered if true places the Toolbar on top of the Content Pane
     */
    public Toolbar(boolean layered) {
        this();
        this.layered = layered;
    }
    
    /**
     * Sets the title of the Toolbar.
     *
     * @param title the Toolbar title
     */
    public void setTitle(String title) {
        checkIfInitialized();
        Component center = ((BorderLayout) getLayout()).getCenter();
        if (center instanceof Label) {
            ((Label) center).setText(title);
        } else {
            titleComponent = new Label(title);
            titleComponent.setUIID("Title");
            if (center != null) {
                replace(center, titleComponent, null);
            } else {
                addComponent(BorderLayout.CENTER, titleComponent);
            }
        }
    }

    /**
     * Makes the title align to the center accurately by doing it at the layout level which also takes into 
     * account right/left commands
     * @param cent whether the title should be centered
     */
    public void setTitleCentered(boolean cent) {
        if(cent) {
            ((BorderLayout)getLayout()).setCenterBehavior(BorderLayout.CENTER_BEHAVIOR_CENTER_ABSOLUTE);
        } else {
            ((BorderLayout)getLayout()).setCenterBehavior(BorderLayout.CENTER_BEHAVIOR_SCALE);
        }
    } 
    
    /**
     * Returns true if the title is centered via the layout
     * @return true if the title is centered
     */
    public boolean isTitleCentered() {
        return ((BorderLayout)getLayout()).getCenterBehavior() == BorderLayout.CENTER_BEHAVIOR_CENTER_ABSOLUTE;
    }

    /**
     * Creates a static side menu that doesn't fold instead of the standard sidemenu.
     * This is common for tablet UI's where folding the side menu doesn't make as much sense.
     * 
     * @param p true to have a permanent side menu
     */
    public static void setPermanentSideMenu(boolean p) {
        permanentSideMenu = p;
    }

    /**
     * Creates a static side menu that doesn't fold instead of the standard sidemenu.
     * This is common for tablet UI's where folding the side menu doesn't make as much sense.
     * 
     * @return true if we will use a permanent sidemenu
     */
    public static boolean isPermanentSideMenu() {
        return permanentSideMenu;
    }

    /**
     * This is a convenience method to open the side menu bar. It's useful for cases where we want to place the 
     * menu button in a "creative way" in which case we can bind the side menu to this
     */
    public void openSideMenu() {
        ((SideMenuBar)getMenuBar()).openMenu(null);
    }
    
    /**
     * Sets the Toolbar title component. This method allow placing any component
     * in the Toolbar center instead of the regular Label. Can be used to place
     * a TextField to preform search operations
     *
     * @param titleCmp Component to place in the Toolbar center.
     */
    public void setTitleComponent(Component titleCmp) {
        checkIfInitialized();
        if(titleComponent != null) {
            titleComponent.remove();
        }
        titleComponent = titleCmp;
        addComponent(BorderLayout.CENTER, titleComponent);
    }
    
    /**
     * Returns the Toolbar title Component.
     * 
     * @return the Toolbar title component
     */ 
    public Component getTitleComponent(){
        return titleComponent;
    }

    /**
     * Adds a Command to the overflow menu
     *
     * @param name the name/title of the command
     * @param icon the icon for the command
     * @param ev the even handler
     * @return a newly created Command instance
     */
    public Command addCommandToOverflowMenu(String name, Image icon, final ActionListener ev) {
        Command cmd = Command.create(name, icon, ev);
        addCommandToOverflowMenu(cmd);
        return cmd;
    }
    
    /**
     * The behavior of the back command  in the title
     */
    public static enum BackCommandPolicy {
        /**
         * Show the back command always within the title bar on the left hand side
         */
        ALWAYS,
        
        /**
         * Show the back command always but shows it with the UIID standard UIID
         */
        AS_REGULAR_COMMAND,

        /**
         * Show the back command always as a back arrow image from the material design style
         */
        AS_ARROW,

        /**
         * Shows the back command only if the {@code backUsesTitleBool} theme constant is defined to true which
         * is the case for iOS themes
         */
        ONLY_WHEN_USES_TITLE,
        
        /**
         * Shows the back command only if the {@code backUsesTitleBool} theme constant is defined to true 
         * on other platforms uses the left arrow material icon
         */
        WHEN_USES_TITLE_OTHERWISE_ARROW,
        
        /**
         * Never show the command in the title area and only set the back command to the toolbar
         */
        NEVER
    }
    
    /**
     * Sets the back command in the title bar to an arrow type and maps the back command hardware key
     * if applicable. This is functionally identical to {@code setBackCommand(title, Toolbar.BackCommandPolicy.AS_ARROW, listener); }
     * 
     * @param title command title
     * @param listener action event for the back command
     * @return  the created command
     */
    public Command setBackCommand(String title, ActionListener<ActionEvent> listener) {
        Command cmd  = Command.create(title, null, listener);
        setBackCommand(cmd, BackCommandPolicy.AS_ARROW);
        return cmd;
    }
    
    /**
     * Sets the back command in the title bar to an arrow type and maps the back command hardware key
     * if applicable. This is functionally identical to {@code setBackCommand(cmd, Toolbar.BackCommandPolicy.AS_ARROW); }
     * 
     * @param cmd the command 
     */
    public void setBackCommand(Command cmd) {
        setBackCommand(cmd, BackCommandPolicy.AS_ARROW);
    }
    
    /**
     * Sets the back command in the title bar and in the form, back command behaves based on the given
     * policy type
     * 
     * @param title command title
     * @param policy the behavior of the back command in the title
     * @param listener action event for the back command
     * @return  the created command
     */
    public Command setBackCommand(String title, BackCommandPolicy policy, ActionListener<ActionEvent> listener) {
        Command cmd  = Command.create(title, null, listener);
        setBackCommand(cmd, policy);
        return cmd;
    }

    /**
     * Sets the back command in the title bar and in the form, back command behaves based on the given
     * policy type
     * 
     * @param cmd the command 
     * @param policy the behavior of the back command in the title
     * @param iconSize the size of the back command icon in millimeters
     */
    public void setBackCommand(Command cmd, BackCommandPolicy policy, float iconSize) {
        if(iconSize < 0) {
            iconSize = 3;
        }
        getComponentForm().setBackCommand(cmd);
        switch(policy) {
            case ALWAYS:
                cmd.putClientProperty("uiid", "BackCommand");
                addCommandToLeftBar(cmd);
                break;
            case WHEN_USES_TITLE_OTHERWISE_ARROW:
                cmd.putClientProperty("uiid", "BackCommand");
                if(getUIManager().isThemeConstant("backUsesTitleBool", false)) {
                    addCommandToLeftBar(cmd);
                    break;
                } 
                // we now internally fallback to as arrow...
            case AS_ARROW:
                cmd.setCommandName("");
                cmd.setIcon(FontImage.createMaterial(FontImage.MATERIAL_ARROW_BACK, "TitleCommand", iconSize));
                addCommandToLeftBar(cmd);
                break;
            case AS_REGULAR_COMMAND:
                addCommandToLeftBar(cmd);
                break;
            case ONLY_WHEN_USES_TITLE:
                if(getUIManager().isThemeConstant("backUsesTitleBool", false)) {
                    cmd.putClientProperty("uiid", "BackCommand");
                    addCommandToLeftBar(cmd);
                }
                break;
            case NEVER:
                break;
        }
    }
    
    /**
     * Sets the back command in the title bar and in the form, back command behaves based on the given
     * policy type
     * 
     * @param cmd the command 
     * @param policy the behavior of the back command in the title
     */
    public void setBackCommand(Command cmd, BackCommandPolicy policy) {
        setBackCommand(cmd, policy, -1);
    }

    /**
     * <p>This method add a search Command on the right bar of the {@code Toolbar}.
     * When the search Command is invoked the current {@code Toolbar} is replaced with 
     * a search {@code Toolbar} to perform a search on the Current Form.</p>
     * <p>The callback ActionListener gets the search string and it's up to developer 
     * to do the actual filtering on the Form.</>
     * <p>It is possible to customize the default look of the search {@code Toolbar} with the following 
     * uiid's: {@code ToolbarSearch}, {@code TextFieldSearch} &amp; {@code TextHintSearch}.</>
     * <script src="https://gist.github.com/codenameone/cd227aaca486889f7c940e2e97985426.js"></script>
     * <img src="https://www.codenameone.com/img/developer-guide/toolbar-search-mode.jpg" alt="Dynamic search mode in the Toolbar" />
     * 
     * @param callback gets the search string callbacks
     * @param iconSize indicates the size of the icons used in the search/back in millimeters
     */ 
    public void addSearchCommand(final ActionListener callback, final float iconSize){
        searchCommand = new Command(""){

            @Override
            public void actionPerformed(ActionEvent evt) {
                SearchBar s = new SearchBar(Toolbar.this, iconSize){

                    @Override
                    public void onSearch(String text) {
                        callback.actionPerformed(new ActionEvent(text));
                    }
                
                };
                Form f = (Form)Toolbar.this.getParent();
                setHidden(true);
                f.removeComponentFromForm(Toolbar.this);
                f.setToolbar(s);
                s.initSearchBar();
                f.animateLayout(100);
            }
        
        };
        Image img;
        if(iconSize > 0) {
            img = FontImage.createMaterial(FontImage.MATERIAL_SEARCH, UIManager.getInstance().getComponentStyle("TitleCommand"), iconSize);
        } else {
            img = FontImage.createMaterial(FontImage.MATERIAL_SEARCH, UIManager.getInstance().getComponentStyle("TitleCommand"));
        }
        searchCommand.setIcon(img);
        addCommandToRightBar(searchCommand);
    }    
    
    /**
     * Removes a previously installed search command
     */
    public void removeSearchCommand() {
        if(searchCommand != null) {
            sideMenu.removeCommand(searchCommand);
            searchCommand = null;
        }
    }
    
    /**
     * <p>This method add a search Command on the right bar of the {@code Toolbar}.
     * When the search Command is invoked the current {@code Toolbar} is replaced with 
     * a search {@code Toolbar} to perform a search on the Current Form.</p>
     * <p>The callback ActionListener gets the search string and it's up to developer 
     * to do the actual filtering on the Form.</>
     * <p>It is possible to customize the default look of the search {@code Toolbar} with the following 
     * uiid's: {@code ToolbarSearch}, {@code TextFieldSearch} &amp; {@code TextHintSearch}.</>
     * <script src="https://gist.github.com/codenameone/cd227aaca486889f7c940e2e97985426.js"></script>
     * <img src="https://www.codenameone.com/img/developer-guide/toolbar-search-mode.jpg" alt="Dynamic search mode in the Toolbar" />
     * 
     * @param callback gets the search string callbacks
     */ 
    public void addSearchCommand(final ActionListener callback){
        addSearchCommand(callback, -1);
    }
    
    /**
     * Adds a Command to the overflow menu
     *
     * @param cmd a Command
     */
    public void addCommandToOverflowMenu(Command cmd) {
        checkIfInitialized();
        if (overflowCommands == null) {
            overflowCommands = new Vector<Command>();
        }
        overflowCommands.add(cmd);
        sideMenu.installRightCommands();
    }
    
    /**
     * Returns the commands within the overflow menu which can be useful for things like unit testing. Notice
     * that you should not mutate the commands or the iteratable set in any way!
     * @return the commands in the overflow menu
     */
    public Iterable<Command> getOverflowCommands() {
        return overflowCommands;
    }

    /**
     * Adds a Command to the side navigation menu
     *
     * @param name the name/title of the command
     * @param icon the icon for the command
     * @param ev the even handler
     * @return a newly created Command instance
     */
    public Command addCommandToSideMenu(String name, Image icon, final ActionListener ev) {
        Command cmd = Command.create(name, icon, ev);
        addCommandToSideMenu(cmd);
        return cmd;
    }
    
    /**
     * Adds a Command to the side navigation menu with a material design icon reference
     * {@link com.codename1.ui.FontImage}.
     *
     * @param name the name/title of the command
     * @param icon the icon for the command
     * @param ev the even handler
     * @return a newly created Command instance
     */
    public Command addMaterialCommandToSideMenu(String name, char icon, final ActionListener ev) {
        Command cmd = Command.create(name, null, ev);
        setCommandMaterialIcon(cmd, icon, "SideCommand");
        addCommandToSideMenu(cmd);
        return cmd;
    }

    /**
     * Adds a Command to the side navigation menu with a material design icon reference
     * {@link com.codename1.ui.FontImage}.
     *
     * @param name the name/title of the command
     * @param icon the icon for the command
     * @param size size in millimeters for the icon
     * @param ev the even handler
     * @return a newly created Command instance
     */
    public Command addMaterialCommandToSideMenu(String name, char icon, float size, final ActionListener ev) {
        Command cmd = Command.create(name, null, ev);
        setCommandMaterialIcon(cmd, icon, size, "SideCommand");
        addCommandToSideMenu(cmd);
        return cmd;
    }
    
    /**
     * Adds a Command to the TitleArea on the right side with a material design icon reference
     * {@link com.codename1.ui.FontImage}.
     *
     * @param name the name/title of the command
     * @param icon the icon for the command
     * @param ev the even handler
     * @return a newly created Command instance
     */
    public Command addMaterialCommandToRightBar(String name, char icon, final ActionListener ev) {
        Command cmd = Command.create(name, null, ev);
        setCommandMaterialIcon(cmd, icon, "TitleCommand");
        addCommandToRightBar(cmd);
        return cmd;
    }    

    /**
     * Adds a Command to the TitleArea on the right side with a material design icon reference
     * {@link com.codename1.ui.FontImage}.
     *
     * @param name the name/title of the command
     * @param icon the icon for the command
     * @param size size of the icon in millimeters
     * @param ev the even handler
     * @return a newly created Command instance
     */
    public Command addMaterialCommandToRightBar(String name, char icon, float size, final ActionListener ev) {
        Command cmd = Command.create(name, null, ev);
        setCommandMaterialIcon(cmd, icon, size, "TitleCommand");
        addCommandToRightBar(cmd);
        return cmd;
    }    
    
    private void setCommandMaterialIcon(Command cmd, char icon, String defaultUIID) {
        String uiid = (String)cmd.getClientProperty("uiid");
        if(uiid != null) {
            FontImage.setMaterialIcon(cmd, icon, uiid);
        } else {
            FontImage.setMaterialIcon(cmd, icon, defaultUIID);
        }
    }
    
    private void setCommandMaterialIcon(Command cmd, char icon, float size, String defaultUIID) {
        String uiid = (String)cmd.getClientProperty("uiid");
        if(uiid != null) {
            FontImage.setMaterialIcon(cmd, icon, uiid, size);
        } else {
            FontImage.setMaterialIcon(cmd, icon, defaultUIID, size);
        }
    }
    
    /**
     * Adds a Command to the TitleArea on the left side with a material design icon reference
     * {@link com.codename1.ui.FontImage}.
     *
     * @param name the name/title of the command
     * @param icon the icon for the command
     * @param ev the even handler
     * @return a newly created Command instance
     */
    public Command addMaterialCommandToLeftBar(String name, char icon, final ActionListener ev) {
        Command cmd = Command.create(name, null, ev);
        setCommandMaterialIcon(cmd, icon, "TitleCommand");
        addCommandToLeftBar(cmd);
        return cmd;
    }    

    /**
     * Adds a Command to the TitleArea on the left side with a material design icon reference
     * {@link com.codename1.ui.FontImage}.
     *
     * @param name the name/title of the command
     * @param icon the icon for the command
     * @param size size in millimeters for the icon
     * @param ev the even handler
     * @return a newly created Command instance
     */
    public Command addMaterialCommandToLeftBar(String name, char icon, float size, final ActionListener ev) {
        Command cmd = Command.create(name, null, ev);
        setCommandMaterialIcon(cmd, icon, size, "TitleCommand");
        addCommandToLeftBar(cmd);
        return cmd;
    }    

    /**
     * Adds a Command to the overflow menu with a material design icon reference
     * {@link com.codename1.ui.FontImage}.
     *
     * @param name the name/title of the command
     * @param icon the icon for the command
     * @param ev the even handler
     * @return a newly created Command instance
     */
    public Command addMaterialCommandToOverflowMenu(String name, char icon, final ActionListener ev) {
        Command cmd = Command.create(name, null, ev);
        setCommandMaterialIcon(cmd, icon, "Command");
        addCommandToOverflowMenu(cmd);
        return cmd;
    }

    /**
     * Adds a Command to the overflow menu with a material design icon reference
     * {@link com.codename1.ui.FontImage}.
     *
     * @param name the name/title of the command
     * @param icon the icon for the command
     * @param size size in millimeters for the icon
     * @param ev the even handler
     * @return a newly created Command instance
     */
    public Command addMaterialCommandToOverflowMenu(String name, char icon, float size, final ActionListener ev) {
        Command cmd = Command.create(name, null, ev);
        setCommandMaterialIcon(cmd, icon, size, "Command");
        addCommandToOverflowMenu(cmd);
        return cmd;
    }
    
    /**
     * Adds a Command to the side navigation menu
     *
     * @param cmd a Command
     */
    public void addCommandToSideMenu(Command cmd) {
        checkIfInitialized();
        if(permanentSideMenu) {
            constructPermanentSideMenu();

            Button b = new Button(cmd);
            b.setEndsWith3Points(false);
            Integer gap = (Integer)cmd.getClientProperty("iconGap");
            if(gap != null) {
                b.setGap(gap.intValue());
            }
            b.setTextPosition(Label.RIGHT);
            String uiid = (String)cmd.getClientProperty("uiid");
            if(uiid != null) {
                b.setUIID(uiid);
            } else {
                b.setUIID("SideCommand");
            }
            addComponentToSideMenu(permanentSideMenuContainer, b);
            
        } else {
            sideMenu.addCommand(cmd);
            sideMenu.installMenuBar();
        }
    }
    
    private void constructPermanentSideMenu() {
        if(permanentSideMenuContainer == null) {
            permanentSideMenuContainer = constructSideNavigationComponent();
            Form parent = getComponentForm();
            parent.addComponentToForm(BorderLayout.WEST, permanentSideMenuContainer);
        }
    }

    /**
     * Adds a Component to the side navigation menu. The Component is added to
     * the navigation menu and the command gets the events once the Component is
     * being pressed.
     *
     * @param cmp c Component to be added to the menu
     * @param cmd a Command to handle the events
     */
    public void addComponentToSideMenu(Component cmp, Command cmd) {
        checkIfInitialized();
        if(permanentSideMenu) {
            constructPermanentSideMenu();
            Container cnt = new Container(new BorderLayout());
            cnt.addComponent(BorderLayout.CENTER, cmp);
            Button btn = new Button(cmd);
            btn.setParent(cnt);
            cnt.setLeadComponent(btn);
            addComponentToSideMenu(permanentSideMenuContainer, cnt);
        } else {
            cmd.putClientProperty(SideMenuBar.COMMAND_SIDE_COMPONENT, cmp);
            cmd.putClientProperty(SideMenuBar.COMMAND_ACTIONABLE, Boolean.TRUE);
            sideMenu.addCommand(cmd);
            sideMenu.installMenuBar();
        }
    }

    /**
     * Adds a Component to the side navigation menu.
     *
     * @param cmp c Component to be added to the menu
     */
    public void addComponentToSideMenu(Component cmp) {
        checkIfInitialized();
        if(permanentSideMenu) {
            constructPermanentSideMenu();
            addComponentToSideMenu(permanentSideMenuContainer, cmp);
        } else {
            Command cmd = new Command("");
            cmd.putClientProperty(SideMenuBar.COMMAND_SIDE_COMPONENT, cmp);
            cmd.putClientProperty(SideMenuBar.COMMAND_ACTIONABLE, Boolean.FALSE);
            sideMenu.addCommand(cmd);
            sideMenu.installMenuBar();
        }
    }

    /**
     * Find the command component instance if such an instance exists
     * @param c the command instance
     * @return the button instance
     */
    public Button findCommandComponent(Command c) {
        if(permanentSideMenu) {
            Button b = findCommandComponent(c, permanentSideMenuContainer);
            if(b != null) {
                return b;
            }
        }
        Button b = sideMenu.findCommandComponent(c);
        if(b != null) {
            return b;
        }
        return findCommandComponent(c, this);
    }
    
    private Button findCommandComponent(Command c, Container cnt) {
        int count = cnt.getComponentCount();
        for (int iter = 0; iter < count; iter++) {
            Component current = cnt.getComponentAt(iter);
            if (current instanceof Button) {
                Button b = (Button) current;
                if (b.getCommand() == c) {
                    return b;
                }
            } else {
                if (current instanceof Container) {
                    Button b = findCommandComponent(c, (Container) current);
                    if(b != null) {
                        return b;
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Adds a Command to the TitleArea on the right side.
     *
     * @param name the name/title of the command
     * @param icon the icon for the command
     * @param ev the even handler
     * @return a newly created Command instance
     */
    public Command addCommandToRightBar(String name, Image icon, final ActionListener ev) {
        Command cmd = Command.create(name, icon, ev);
        addCommandToRightBar(cmd);
        return cmd;
    }
    
    /**
     * Adds a Command to the TitleArea on the right side.
     *
     * @param cmd a Command
     */
    public void addCommandToRightBar(Command cmd) {
        checkIfInitialized();
        cmd.putClientProperty("TitleCommand", Boolean.TRUE);
        sideMenu.addCommand(cmd, 0);        
    }
    
    /**
     * Adds a Command to the TitleArea on the left side.
     *
     * @param name the name/title of the command
     * @param icon the icon for the command
     * @param ev the even handler
     * @return a newly created Command instance
     */
    public Command addCommandToLeftBar(String name, Image icon, final ActionListener ev) {
        Command cmd = Command.create(name, icon, ev);
        addCommandToLeftBar(cmd);
        return cmd;
    }

    /**
     * Adds a Command to the TitleArea on the left side.
     *
     * @param cmd a Command
     */
    public void addCommandToLeftBar(Command cmd) {
        checkIfInitialized();
        cmd.putClientProperty("TitleCommand", Boolean.TRUE);
        cmd.putClientProperty("Left", Boolean.TRUE);
        sideMenu.addCommand(cmd, 0);
    }
    
    /**
     * Returns the commands within the right bar section which can be useful for things like unit testing. Notice
     * that you should not mutate the commands or the iteratable set in any way!
     * @return the commands in the overflow menu
     */
    public Iterable<Command> getRightBarCommands() {
        return getBarCommands(null);
    }

    /**
     * Returns the commands within the left bar section which can be useful for things like unit testing. Notice
     * that you should not mutate the commands or the iteratable set in any way!
     * @return the commands in the overflow menu
     */
    public Iterable<Command> getLeftBarCommands() {
        return getBarCommands(Boolean.TRUE);
    }

    private Iterable<Command> getBarCommands(Object leftValue) {
        ArrayList<Command> cmds = new ArrayList<Command>();
        findAllCommands(this, cmds);
        int commandCount = cmds.size() - 1;
        while(commandCount > 0) {
            Command c = cmds.get(commandCount);
            if(c.getClientProperty("Left") != leftValue) {
                cmds.remove(commandCount);
            }
            commandCount--;
        }
        return cmds;
    }

    private void findAllCommands(Container cnt, ArrayList<Command> cmds) {
        for(Component c : cnt) {
            if(c instanceof Container) {
                findAllCommands((Container)c, cmds);
                continue;
            }
            if(c instanceof Button) {
                cmds.add(((Button)c).getCommand());
            }
        }
    }
    
    /**
     * Returns the associated SideMenuBar object of this Toolbar.
     *
     * @return the associated SideMenuBar object
     */
    public MenuBar getMenuBar() {
        return sideMenu;
    }

    /*
     * A Overflow Menu is implemented as a dialog, this method allows you to 
     * override the dialog display in order to customize the dialog menu in 
     * various ways
     * 
     * @param menu a dialog containing Overflow Menu options that can be 
     * customized
     * @return the command selected by the user in the dialog
     */
    protected Command showOverflowMenu(Dialog menu) {
        Form parent = sideMenu.getParentForm();
        int height;
        int marginLeft;
        int marginRight = 0;
        Container dialogContentPane = menu.getDialogComponent();
        marginLeft = parent.getWidth() - (dialogContentPane.getPreferredW()
                + menu.getStyle().getHorizontalPadding());
        marginLeft = Math.max(0, marginLeft);
        if (parent.getSoftButtonCount() > 1) {
            height = parent.getHeight() - parent.getSoftButton(0).getParent().getPreferredH() - dialogContentPane.getPreferredH();
        } else {
            height = parent.getHeight() - dialogContentPane.getPreferredH();
        }
        height = Math.max(0, height);
        int th = getHeight();
        Transition transitionIn;
        Transition transitionOut;
        UIManager manager = parent.getUIManager();
        LookAndFeel lf = manager.getLookAndFeel();
        if (lf.getDefaultMenuTransitionIn() != null || lf.getDefaultMenuTransitionOut() != null) {
            transitionIn = lf.getDefaultMenuTransitionIn();
            if(transitionIn instanceof BubbleTransition){
                ((BubbleTransition)transitionIn).setComponentName("OverflowButton");
            }
            transitionOut = lf.getDefaultMenuTransitionOut();
        } else {
            transitionIn = CommonTransitions.createEmpty();
            transitionOut = CommonTransitions.createEmpty();
        }
        menu.setTransitionInAnimator(transitionIn);
        menu.setTransitionOutAnimator(transitionOut);
        
        if(isRTL()){
            marginRight = marginLeft;
            marginLeft = 0;
        }
        int tint = parent.getTintColor();
        parent.setTintColor(0x00FFFFFF);
        parent.tint = false;
        boolean showBelowTitle = manager.isThemeConstant("showMenuBelowTitleBool", true);
        int topPadding = 0;
        Component statusBar = ((BorderLayout) getLayout()).getNorth();
        if (statusBar != null) {
            topPadding = statusBar.getAbsoluteY() + statusBar.getHeight();
        }
        if(showBelowTitle){
            topPadding = th;
        }
        
        Command r = menu.show(topPadding, Math.max(topPadding, height - topPadding), marginLeft, marginRight, true);
        parent.setTintColor(tint);
        return r;
    }

    /**
     * Creates the list component containing the commands within the given
     * vector used for showing the menu dialog
     *
     * @param commands list of command objects
     * @return List object
     */
    protected List createOverflowCommandList(Vector commands) {
        List l = new List(commands);
        l.setUIID("CommandList");
        Component c = (Component) l.getRenderer();
        c.setUIID("Command");
        c = l.getRenderer().getListFocusComponent(l);
        c.setUIID("CommandFocus");
        l.setFixedSelection(List.FIXED_NONE_CYCLIC);
        ((DefaultListCellRenderer)l.getRenderer()).setShowNumbers(false);
        return l;
    }

    /**
     * Adds a status bar space to the north of the Component, subclasses can
     * override this default behavior.
     */
    protected void initTitleBarStatus() {
        if (getUIManager().isThemeConstant("paintsTitleBarBool", false)) {
            // check if its already added:
            if (((BorderLayout) getLayout()).getNorth() == null) {
                Container bar = new Container();
                bar.setUIID("StatusBar");
                addComponent(BorderLayout.NORTH, bar);
            }
        }
    }

    private void checkIfInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Need to call "
                    + "Form#setToolBar(Toolbar toolbar) before calling this method");
        }
    }
    
    /**
     * Sets the Toolbar to scroll off the screen upon content scroll. This
     * feature can only work if the Form contentPane is scrollableY
     *
     * @param scrollOff if true the Toolbar needs to scroll off the screen when
     * the Form ContentPane is scrolled
     */
    public void setScrollOffUponContentPane(boolean scrollOff) {
        if (initialized && !this.scrollOff && scrollOff) {
            bindScrollListener(true);
        }
        this.scrollOff = scrollOff;
    }

    /**
     * Hide the Toolbar if it is currently showing
     */
    public void hideToolbar() {
        showing = false;       
        if (actualPaneInitialH == 0) {
            Form f = getComponentForm();
            if(f != null){
                initVars(f.getActualPane());
            }
        }
        hideShowMotion = Motion.createSplineMotion(getY(), -getHeight(), 300);
        getComponentForm().registerAnimated(this);
        hideShowMotion.start();
    }

    /**
     * Show the Toolbar if it is currently not showing
     */
    public void showToolbar() {
        showing = true;
        hideShowMotion = Motion.createSplineMotion(getY(), initialY, 300);
        getComponentForm().registerAnimated(this);
        hideShowMotion.start();
    }

    public boolean animate() {
        if (hideShowMotion != null) {
            Form f = getComponentForm();
            final Container actualPane = f.getActualPane();
            int val = hideShowMotion.getValue();
            setY(val);
            if(!layered){
                actualPane.setY(actualPaneInitialY + val);
                if (showing) {
                    actualPane.setHeight(actualPaneInitialH + getHeight() - val);
                } else {
                    actualPane.setHeight(actualPaneInitialH - val);
                }
                actualPane.doLayout();
            }
            f.repaint();
            boolean finished = hideShowMotion.isFinished();
            if (finished) {
                f.deregisterAnimated(this);
                hideShowMotion = null;
            }
            return !finished;
        }
        return false;
    }

    private void initVars(Container actualPane) {
        initialY = getY();
        actualPaneInitialY = actualPane.getY();
        actualPaneInitialH = actualPane.getHeight();
    }

    private void bindScrollListener(boolean bind) {
        final Form f = getComponentForm();
        if (f != null) {
            final Container actualPane = f.getActualPane();
            final Container contentPane = f.getContentPane();
            if (bind) {
                initVars(actualPane);
                scrollListener = new ScrollListener() {

                    public void scrollChanged(int scrollX, int scrollY, int oldscrollX, int oldscrollY) {
                        int diff = scrollY - oldscrollY;
                        int toolbarNewY = getY() - diff;
                        if (scrollY < 0 || Math.abs(toolbarNewY) < 2) {
                            return;
                        }
                        toolbarNewY = Math.max(toolbarNewY, -getHeight());
                        toolbarNewY = Math.min(toolbarNewY, initialY);
                        if (toolbarNewY != getY()) {
                            setY(toolbarNewY);
                            if(!layered){
                                actualPane.setY(actualPaneInitialY + toolbarNewY);
                                actualPane.setHeight(actualPaneInitialH + getHeight() - toolbarNewY);
                                actualPane.doLayout();
                            }
                            f.repaint();
                        }
                    }
                };

                contentPane.addScrollListener(scrollListener);
                releasedListener = new ActionListener() {

                    public void actionPerformed(ActionEvent evt) {
                        if (getY() + getHeight() / 2 > 0) {
                            showToolbar();
                        } else {
                            hideToolbar();
                        }
                        f.repaint();
                    }
                };
                contentPane.addPointerReleasedListener(releasedListener);
            } else {
                if (scrollListener != null) {
                    contentPane.removeScrollListener(scrollListener);
                    contentPane.removePointerReleasedListener(releasedListener);
                }

            }
        }

    }
    
    /**
     * Creates the side navigation component with the Commands.
     *
     * @param commands the Command objects
     * @return the Component to display on the side navigation
     */
    protected Container createSideNavigationComponent(Vector commands, String placement) {
        return sideMenu.createSideNavigationPanel(commands, placement);
    }
    
    /**
     * Creates an empty side navigation panel.
     */
    protected Container constructSideNavigationComponent() {
        return sideMenu.constructSideNavigationPanel();
    }

    /**
     * This method responsible to add a Component to the side navigation panel.
     *
     * @param menu the Menu Container that was created in the
     * constructSideNavigationComponent() method
     *
     * @param cmp the Component to add to the side menu
     */
    protected void addComponentToSideMenu(Container menu, Component cmp) {
        sideMenu.addComponentToSideMenuImpl(menu, cmp);
    }

    /**
     * Returns the commands within the side menu which can be useful for things like unit testing. Notice
     * that you should not mutate the commands or the iteratable set in any way!
     * @return the commands in the overflow menu
     */
    public Iterable<Command> getSideMenuCommands() {
        ArrayList<Command> cmds = new ArrayList<Command>();
        if(permanentSideMenu) {
            findAllCommands(permanentSideMenuContainer, cmds);
            return cmds;
        }
        Form f = getComponentForm();
        int commands = f.getCommandCount();
        for(int iter = 0 ; iter < commands ; iter++) {
            cmds.add(f.getCommand(iter));
        }
        return cmds;
    }

    /**
     * Removes the given overflow menu command, notice that this has no effect on the menu that is currently
     * showing (if it is currently showing) only on the upcoming iterations.
     * @param cmd the command to remove from the overflow
     */
    public void removeOverflowCommand(Command cmd) {
        overflowCommands.remove(cmd);
    }

    

    class ToolbarSideMenu extends SideMenuBar {

        @Override
        protected Container createSideNavigationComponent(Vector commands, String placement) {
            return Toolbar.this.createSideNavigationComponent(commands, placement);
        }
        
        @Override
        protected Container constructSideNavigationComponent(){
            return Toolbar.this.constructSideNavigationComponent();
        }

        @Override
        protected void addComponentToSideMenu(Container menu, Component cmp) {
            Toolbar.this.addComponentToSideMenu(menu, cmp);
        }
        
        @Override
        protected Container getTitleAreaContainer() {
            return Toolbar.this;
        }

        @Override
        protected Component getTitleComponent() {
            return Toolbar.this.getTitleComponent();
        }

        @Override
        protected void initMenuBar(Form parent) {
            Component ta = parent.getTitleArea();
            parent.removeComponentFromForm(ta);
            super.initMenuBar(parent);
            if(layered){
                Container layeredPane = parent.getLayeredPane();
                Container p = layeredPane.getParent();
                Container top = new Container(new BorderLayout());
                top.addComponent(BorderLayout.NORTH, Toolbar.this);
                p.addComponent(top);
            
            }else{
                parent.addComponentToForm(BorderLayout.NORTH, Toolbar.this);
            }
            
            initialized = true;
            setTitle(parent.getTitle());
            parent.revalidate();
            initTitleBarStatus();
            Display.getInstance().callSerially(new Runnable() {

                public void run() {
                    if (scrollOff) {
                        bindScrollListener(true);
                    }
                }
            });
        }

        @Override
        public boolean contains(int x, int y) {
            return Toolbar.this.contains(x, y);
        }

        @Override
        public Component getComponentAt(int x, int y) {
            return Toolbar.this.getComponentAt(x, y);
        }

        @Override
        void installRightCommands() {
            super.installRightCommands();
            if (overflowCommands != null && overflowCommands.size() > 0) {
                UIManager uim = UIManager.getInstance();
                Image i = (Image) uim.getThemeImageConstant("menuImage");
                if (i == null) { 
                    float size = 4.5f;
                    try {
                        size = Float.parseFloat(uim.getThemeConstant("overflowImageSize", "4.5"));
                    } catch(Throwable t) {
                        t.printStackTrace();
                    }
                    i = FontImage.createMaterial(FontImage.MATERIAL_MORE_VERT, UIManager.getInstance().getComponentStyle("TitleCommand"), size);
                }
                menuButton = sideMenu.createTouchCommandButton(new Command("", i) {

                    public void actionPerformed(ActionEvent ev) {
                        sideMenu.showMenu();
                    }
                });
                menuButton.putClientProperty("overflow", Boolean.TRUE);
                menuButton.setUIID("TitleCommand");
                menuButton.setName("OverflowButton");
                Layout l = getTitleAreaContainer().getLayout();
                if (l instanceof BorderLayout) {
                    BorderLayout bl = (BorderLayout) l;
                    Component east = bl.getEast();
                    if (east == null) {
                        getTitleAreaContainer().addComponent(BorderLayout.EAST, menuButton);
                    } else {
                        if (east instanceof Container) {
                            Container cnt = (Container) east;
                            for (int j = 0; j < cnt.getComponentCount(); j++) {
                                Component c = cnt.getComponentAt(j);
                                if (c instanceof Button) {
                                    //remove the menu button and add it last
                                    if (c.getClientProperty("overflow") != null) {
                                        cnt.removeComponent(c);
                                    }
                                }
                            }
                            cnt.addComponent(cnt.getComponentCount(), menuButton);
                        } else {
                            if (east instanceof Button) {
                                if (east.getClientProperty("overflow") != null) {
                                    return;
                                }
                            }
                            east.getParent().removeComponent(east);
                            Container buttons = new Container(new BoxLayout(BoxLayout.X_AXIS));
                            buttons.addComponent(east);
                            buttons.addComponent(menuButton);
                            getTitleAreaContainer().addComponent(BorderLayout.EAST, buttons);
                        }
                    }
                }
            }
        }

        @Override
        protected Component createCommandComponent(Vector commands) {
            return createOverflowCommandList(overflowCommands);
        }

        @Override
        protected Button createBackCommandButton() {
            Button back = new Button(getBackCommand());
            return back;
        }

        @Override
        protected Command showMenuDialog(Dialog menu) {
            return showOverflowMenu(menu);
        }

        @Override
        public int getCommandBehavior() {
            return Display.COMMAND_BEHAVIOR_ICS;
        }
        
        @Override
        void synchronizeCommandsWithButtonsInBackbutton() {
            boolean hasSideCommands = false;
            Vector commands = getCommands();
            for (int iter = commands.size() - 1; iter > -1; iter--) {
                Command c = (Command) commands.elementAt(iter);
                if (c.getClientProperty("TitleCommand") == null) {
                    hasSideCommands = true;
                    break;
                }

            }

            boolean hideBack = UIManager.getInstance().isThemeConstant("hideBackCommandBool", false);
            boolean showBackOnTitle = UIManager.getInstance().isThemeConstant("showBackCommandOnTitleBool", false);
            //need to put the back command
            if (getBackCommand() != null) {

                if (hasSideCommands && !hideBack) {
                    getCommands().remove(getBackCommand());
                    getCommands().add(getCommands().size(), getBackCommand());

                } else {
                    if (!hideBack || showBackOnTitle) {
                        //put the back command on the title
                        Layout l = getTitleAreaContainer().getLayout();
                        if (l instanceof BorderLayout) {
                            BorderLayout bl = (BorderLayout) l;
                            Component west = bl.getWest();
                            Button back = createBackCommandButton();
                            if (!back.getUIID().equals("BackCommand")) {
                                back.setUIID("BackCommand");
                            }
                            hideEmptyCommand(back);
                            verifyBackCommandRTL(back);

                            if (west instanceof Container) {
                                ((Container) west).addComponent(0, back);
                            } else {
                                Container left = new Container(new BoxLayout(BoxLayout.X_AXIS));
                                left.addComponent(back);
                                if (west != null) {
                                    west.getParent().removeComponent(west);
                                    left.addComponent(west);
                                }
                                getTitleAreaContainer().addComponent(BorderLayout.WEST, left);
                            }
                        }
                    }

                }

            }

        }

        @Override
        void initTitleBarStatus() {
            Toolbar.this.initTitleBarStatus();
        }

    }

}
