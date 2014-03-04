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

import com.codename1.ui.animations.Animation;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.animations.Motion;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import java.util.Vector;

/**
 * This is Menu Bar that displays it's commands on a side bar navigation similar
 * to Google+/Facbook apps navigation
 *
 * @author Chen
 */
public class SideMenuBar extends MenuBar {

    private Button openButton;
    private Button rightSideButton;
    private Form parent;
    private Form menu;
    private Container rightPanel;
    private boolean dragActivated;
    private Transition in;
    private Transition out;
    private Container sidePanel;
    private int draggedX;
    private java.util.ArrayList rightCommands;
    int initialDragX;
    int initialDragY;
    boolean transitionRunning;
    private ActionListener pointerDragged;
    private ActionListener pointerPressed;

    boolean sideSwipePotential;
    private boolean rightSideSwipePotential;
    private boolean topSwipePotential;

    /**
     * This string can be used in putClientProperty within command to hint about
     * the placement of the command
     */
    public static final String COMMAND_PLACEMENT_KEY = "placement";

    /**
     * This string can be used in putClientProperty within command to hint about
     * the placement of the command e.g.
     * putClientProperty(SideMenuBar.COMMAND_PLACEMENT_KEY, SideMenuBar.COMMAND_PLACEMENT_VALUE_RIGHT);
     */
    public static final String COMMAND_PLACEMENT_VALUE_RIGHT = "right";

    /**
     * This string can be used in putClientProperty within command to hint about
     * the placement of the command e.g.
     * putClientProperty(SideMenuBar.COMMAND_PLACEMENT_KEY, SideMenuBar.COMMAND_PLACEMENT_VALUE_TOP);
     */
    public static final String COMMAND_PLACEMENT_VALUE_TOP = "top";

    /**
     * Allows placing a component instance into the client properties of the command so 
     * it is shown instead of the command e.g.:
     * putClientProperty(SideMenuBar.COMMAND_SIDE_COMPONENT, myCustomComponentInstance);
     */
    public static final String COMMAND_SIDE_COMPONENT = "SideComponent";

    /**
     * When using a side component we might want to only have it behave as a visual tool
     * and still execute the command when it is clicked. The default behavior is to
     * delegate events to the component, however if this flag is used the command 
     * will act as normal while using the COMMAND_SIDE_COMPONENT only for visual effect e.g.:
     * putClientProperty(SideMenuBar.COMMAND_ACTIONABLE, Boolean.TRUE);
     */
    public static final String COMMAND_ACTIONABLE = "Actionable";
    
    /**
     * Empty Constructor
     */
    public SideMenuBar() {
    }

    /**
     * @inheritDoc
     */
    protected void initMenuBar(Form parent) {
        if (parent.getClientProperty("Menu") != null) {
            return;
        }
        super.initMenuBar(parent);
        this.parent = parent;
    }

    /**
     * Returns true if a side menu is currently controlling the screen
     *
     * @return true if a side menu is currently controlling the screen
     */
    public static boolean isShowing() {
        Form f = Display.getInstance().getCurrent();
        return f.getClientProperty("cn1$sideMenuParent") != null;
    }

    /**
     * Folds the current side menu if it is open, notice that the menu will
     * close asynchronously
     */
    public static void closeCurrentMenu() {
        Form f = Display.getInstance().getCurrent();
        SideMenuBar b = (SideMenuBar) f.getClientProperty("cn1$sideMenuParent");
        if (b != null && !b.transitionRunning) {
            b.closeMenu();
        }
    }

    /**
     * Folds the current side menu if it is open, when the menu is closed it
     * will invoke the runnable callback method
     *
     * @param callback will be invoked when the menu is actually closed
     */
    public static void closeCurrentMenu(final Runnable callback) {
        Form f = Display.getInstance().getCurrent();
        SideMenuBar b = (SideMenuBar) f.getClientProperty("cn1$sideMenuParent");
        if (b != null && !b.transitionRunning) {
            b.parent.addShowListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    callback.run();
                }
            });
            b.closeMenu();
        } else {
            callback.run();
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    protected void removeAllCommands() {
        Container t = parent.getTitleArea();
        int count = t.getComponentCount();
        for (int iter = 0; iter < count; iter++) {
            Component current = t.getComponentAt(iter);
            if ("TitleCommand".equals(current.getUIID())) {
                t.removeComponent(current);
                t.revalidate();
            }
        }
        super.removeAllCommands();
        parent.initTitleBarStatus();
    }

    /**
     * @inheritDoc
     */
    @Override
    protected void unInstallMenuBar() {
        super.unInstallMenuBar();
        if (pointerDragged != null) {
            parent.removePointerDraggedListener(pointerDragged);
        }
        if (pointerPressed != null) {
            parent.removePointerPressedListener(pointerPressed);
        }
    }

    /**
     * @inheritDoc
     */
    protected void installMenuBar() {
        if (parent.getClientProperty("Menu") != null) {
            return;
        }
        super.installMenuBar();
        if (parent instanceof Dialog) {
            return;
        }
        openButton = new Button();
        openButton.setUIID("MenuButton");
        UIManager uim = parent.getUIManager();
        Image i = (Image) uim.getThemeImageConstant("sideMenuImage");
        if (i != null) {
            openButton.setIcon(i);
        } else {
            openButton.setIcon(Resources.getSystemResource().getImage("mobile-menu.png"));
        }
        Image p = (Image) uim.getThemeImageConstant("sideMenuPressImage");
        if (p != null) {
            openButton.setPressedIcon(p);
        }
        openButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                openMenu(null);
            }
        });
        addOpenButton(null, true);

        if (uim.isThemeConstant("sideMenuFoldedSwipeBool", true) && parent.getClientProperty("sideMenuFoldedSwipeListeners") == null) {
            pointerDragged = new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    if (sideSwipePotential) {
                        final int x = evt.getX();
                        final int y = evt.getY();
                        if (Math.abs(y - initialDragY) > x - initialDragX) {
                            sideSwipePotential = false;
                            return;
                        }
                        evt.consume();
                        if (x - initialDragX > Display.getInstance().getDisplayWidth() / getUIManager().getThemeConstant("sideSwipeActivationInt", 15)) {
                            draggedX = x;
                            dragActivated = true;
                            parent.pointerReleased(-1, -1);
                            openMenu(null, 0, draggedX, false);
                        }
                        return;
                    }
                    if (rightSideSwipePotential) {
                        final int x = evt.getX();
                        final int y = evt.getY();
                        if (Math.abs(y - initialDragY) > initialDragX - x) {
                            rightSideSwipePotential = false;
                            return;
                        }
                        evt.consume();
                        if (initialDragX - x > Display.getInstance().getDisplayWidth() / getUIManager().getThemeConstant("sideSwipeActivationInt", 15)) {
                            draggedX = x;
                            dragActivated = true;
                            parent.pointerReleased(-1, -1);
                            openMenu(COMMAND_PLACEMENT_VALUE_RIGHT, 0, draggedX, false);
                        }
                    }
                    if (topSwipePotential) {
                        final int x = evt.getX();
                        final int y = evt.getY();
                        if (Math.abs(y - initialDragY) < x - initialDragX) {
                            topSwipePotential = false;
                            return;
                        }
                        evt.consume();
                        if (initialDragY - y > Display.getInstance().getDisplayHeight() / getUIManager().getThemeConstant("sideSwipeActivationInt", 15)) {
                            draggedX = y;
                            dragActivated = true;
                            parent.pointerReleased(-1, -1);
                            openMenu(COMMAND_PLACEMENT_VALUE_TOP, 0, draggedX, false);
                        }
                    }
                }
            };
            parent.addPointerDraggedListener(pointerDragged);
            pointerPressed = new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    rightSideSwipePotential = false;
                    topSwipePotential = false;
                    sideSwipePotential = false;
                    if (getCommandCount() == 0) {
                        return;
                    }
                    if (parent.getCommandCount() == 1) {
                        if (parent.getCommand(0) == parent.getBackCommand()) {
                            return;
                        }
                    }
                    int displayWidth = Display.getInstance().getDisplayWidth();
                    if (rightSideButton != null) {
                        rightSideSwipePotential = !transitionRunning && evt.getX() > displayWidth - displayWidth / getUIManager().getThemeConstant("sideSwipeSensitiveInt", 10);
                    }
                    if (parent.getTitleComponent() instanceof Button) {
                        topSwipePotential = !transitionRunning && evt.getY() < Display.getInstance().getDisplayHeight() / getUIManager().getThemeConstant("sideSwipeSensitiveInt", 10);
                    }
                    sideSwipePotential = !transitionRunning && evt.getX() < displayWidth / getUIManager().getThemeConstant("sideSwipeSensitiveInt", 10);
                    initialDragX = evt.getX();
                    initialDragY = evt.getY();
                    if (sideSwipePotential || rightSideSwipePotential || topSwipePotential) {
                        Component c = Display.getInstance().getCurrent().getComponentAt(initialDragX, initialDragY);
                        if (c != null && c.shouldBlockSideSwipe()) {
                            sideSwipePotential = false;
                        }
                    }
                }
            };
            parent.addPointerPressedListener(pointerPressed);
            parent.putClientProperty("sideMenuFoldedSwipeListeners", "true");
        }
    }

    /**
     * @inheritDoc
     */
    protected int getDragRegionStatus(int x, int y) {
        if (getUIManager().isThemeConstant("sideMenuFoldedSwipeBool", true)) {
            if (x - initialDragX > Display.getInstance().getDisplayWidth() / getUIManager().getThemeConstant("sideSwipeActivationInt", 15)) {
                return DRAG_REGION_LIKELY_DRAG_X;
            }
        }
        return DRAG_REGION_NOT_DRAGGABLE;
    }
    
    private void installRightCommands() {
        if (rightCommands != null) {
            for (int i = 0; i < rightCommands.size(); i++) {
                Command rightCommand = (Command) rightCommands.get(i);
                
                Layout l = parent.getTitleArea().getLayout();
                if (l instanceof BorderLayout) {
                    BorderLayout bl = (BorderLayout) l;
                    Component east = bl.getEast();
                    if (east == null) {
                        Button b = new Button(rightCommand);
                        b.setUIID("TitleCommand");
                        parent.getTitleArea().addComponent(BorderLayout.EAST, b);
                    } else {
                        if (east instanceof Container) {
                            Container cnt = (Container) east;
                            Button b = new Button(rightCommand);
                            b.setUIID("TitleCommand");
                            cnt.addComponent(b);
                        } else {
                            east.getParent().removeComponent(east);
                            Container buttons = new Container(new BoxLayout(BoxLayout.X_AXIS));
                            buttons.addComponent(east);
                            Button b = new Button(rightCommand);
                            b.setUIID("TitleCommand");
                            buttons.addComponent(b);
                            parent.getTitleArea().addComponent(BorderLayout.EAST, buttons);
                        }
                    }
                }

            }

        }
        parent.initTitleBarStatus();
    }

    /**
     * @inheritDoc
     */
    public void addCommand(Command cmd) {
        if (cmd.getClientProperty("TitleCommand") != null) {
            if(rightCommands == null){
                rightCommands = new java.util.ArrayList();
            }
            rightCommands.add(0, cmd);
            addOpenButton(cmd, false);
            installRightCommands();
            return;
        }
        super.addCommand(cmd);
        if (parent instanceof Dialog) {
            return;
        }
        addOpenButton(cmd, false);
    }

    /**
     * @inheritDoc
     */
    public void setBackCommand(Command backCommand) {
        super.setBackCommand(backCommand);
        if (parent instanceof Dialog) {
            return;
        }
        addOpenButton(null, false);
        installRightCommands();
        if (getBackCommand() != null
                && getCommandCount() > 0
                && !UIManager.getInstance().isThemeConstant("hideBackCommandBool", false)
                && !getCommands().contains(getBackCommand())) {
            getCommands().insertElementAt(getBackCommand(), 0);
        }
    }

    int getCommandBehavior() {
        return Display.COMMAND_BEHAVIOR_SIDE_NAVIGATION;
    }

    /**
     * @inheritDoc
     */
    protected void addCommand(Command cmd, int index) {
        if (cmd.getClientProperty("TitleCommand") != null) {
            if(rightCommands == null){
                rightCommands = new java.util.ArrayList();
            }
            rightCommands.add(0, cmd);
            addOpenButton(cmd, false);
            installRightCommands();
            return;
        }
        super.addCommand(cmd, index);
        if (parent instanceof Dialog) {
            return;
        }
        addOpenButton(cmd, false);
        if (getBackCommand() != null
                && getCommandCount() > 0
                && !UIManager.getInstance().isThemeConstant("hideBackCommandBool", false)
                && !getCommands().contains(getBackCommand())) {
            getCommands().insertElementAt(getBackCommand(), 0);
        }
    }

    /**
     * @inheritDoc
     */
    protected void removeCommand(Command cmd) {
        super.removeCommand(cmd);
        if (parent instanceof Dialog) {
            return;
        }
        if(rightCommands != null){
            rightCommands.remove(cmd);
        }
        if (getCommandCount() == 0) {
            if (parent.getTitleComponent() != null) {
                parent.getTitleComponent().getParent().removeAll();
            }
            parent.getTitleArea().removeAll();
            parent.getTitleArea().addComponent(BorderLayout.CENTER, parent.getTitleComponent());
        }        
        installRightCommands();
    }

    public void keyReleased(int keyCode) {
        if (keyCode == leftSK) {
            if (getCommandCount() == 0) {
                return;
            }
            if (parent.getCommandCount() == 1) {
                if (parent.getCommand(0) == parent.getBackCommand()) {
                    return;
                }
            }
            openMenu(null);
        }
        super.keyReleased(keyCode);
    }

    /**
     * Closes the menu if it is currently open
     */
    public void closeMenu() {
        if (transitionRunning) {
            return;
        }
        if (Display.getInstance().getCurrent() == menu) {
            parent.showBack();
        }
    }

    /**
     * Opens the menu if it is currently closed
     */
    public void openMenu(String direction) {
        openMenu(direction, -1, 300, true);
    }

    /**
     * Opens the menu if it is currently closed
     */
    void openMenu(String direction, int time, int dest, boolean transition) {
        if (Display.getInstance().getCurrent() == parent) {
            menu = createMenu(direction);
            //replace transtions to perform the Form shift
            out = parent.getTransitionOutAnimator();
            in = parent.getTransitionInAnimator();
            parent.setTransitionInAnimator(new SideMenuBar.MenuTransition(300, false, -1, direction));
            if(transition) {
                parent.setTransitionOutAnimator(new SideMenuBar.MenuTransition(dest, true, time, direction));
                menu.show();
            } else {
                parent.setTransitionOutAnimator(new SideMenuBar.MenuTransition(0, true, dest, direction));
                menu.show();
                parent.setTransitionOutAnimator(new SideMenuBar.MenuTransition(dest, true, time, direction));
            }
        }
    }

    /**
     * Returns true if the Menu is currently open
     *
     * @return true if menu open
     */
    public boolean isMenuOpen() {
        return Display.getInstance().getCurrent() == menu;
    }

    private void addOpenButton(Command cmd, boolean checkCommands) {
        if (parent != null && getCommandCount() > 0 && openButton.getParent() == null) {
            Container titleArea = parent.getTitleArea();
            titleArea.removeAll();
            if (!parent.getUIManager().isThemeConstant("hideLeftSideMenuBool", false)) {
                titleArea.addComponent(BorderLayout.WEST, openButton);
            }
            Label l = parent.getTitleComponent();
            if (l.getParent() != null) {
                l.getParent().removeComponent(l);
            }
            titleArea.addComponent(BorderLayout.CENTER, l);
            installRightCommands();
            if(parent.getUIManager().isThemeConstant("leftAlignSideMenuBool", false)) {
                ((BorderLayout) titleArea.getLayout()).setCenterBehavior(BorderLayout.CENTER_BEHAVIOR_SCALE);
            } else {
                ((BorderLayout) titleArea.getLayout()).setCenterBehavior(BorderLayout.CENTER_BEHAVIOR_CENTER_ABSOLUTE);                
            }
        }
        if (cmd != null) {
            String placement = (String) cmd.getClientProperty(COMMAND_PLACEMENT_KEY);
            if (placement != null) {
                validateCommandPlacement(placement);
            }
        } else {
            if (checkCommands) {
                for (int iter = 0; iter < getCommandCount(); iter++) {
                    Command c = getCommand(iter);
                    String placement = (String) c.getClientProperty(COMMAND_PLACEMENT_KEY);
                    if (placement != null) {
                        validateCommandPlacement(placement);
                    }
                }
            }
        }
        parent.initTitleBarStatus();
    }

    private void validateCommandPlacement(String placement) {
        if (placement == COMMAND_PLACEMENT_VALUE_TOP) {
            ((BorderLayout) parent.getTitleArea().getLayout()).setCenterBehavior(BorderLayout.CENTER_BEHAVIOR_SCALE);
            if (!(parent.getTitleComponent() instanceof Button)) {
                Button b = new Button(parent.getTitle());
                b.setUIID("Title");
                parent.setTitleComponent(b);
                b.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        openMenu(COMMAND_PLACEMENT_VALUE_TOP);
                    }
                });
            }
            return;
        }
        if (placement == COMMAND_PLACEMENT_VALUE_RIGHT) {
            if (rightSideButton != null && rightSideButton.getParent() != null) {
                return;
            }
            rightSideButton = new Button();
            rightSideButton.setUIID("MenuButtonRight");
            UIManager uim = parent.getUIManager();
            Image i = (Image) uim.getThemeImageConstant("rightSideMenuImage");
            if (i != null) {
                rightSideButton.setIcon(i);
            } else {
                rightSideButton.setIcon(Resources.getSystemResource().getImage("mobile-menu.png"));
            }
            Image p = (Image) uim.getThemeImageConstant("rightSideMenuPressImage");
            if (p != null) {
                rightSideButton.setPressedIcon(p);
            }
            rightSideButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    openMenu(COMMAND_PLACEMENT_VALUE_RIGHT);
                }
            });
            Container ta = parent.getTitleArea();
            ta.addComponent(BorderLayout.EAST, rightSideButton);
            ta.revalidate();
            return;
        }
    }

    private void clean() {
        if (out != null) {
            parent.setTransitionOutAnimator(out);
        }
        if (in != null) {
            parent.setTransitionInAnimator(in);
        }
        out = null;
        in = null;
    }

    private void setMenuGlassPane(Form m, final String placement) {
        boolean isRTLValue = m.isRTL();
        if (placement == COMMAND_PLACEMENT_VALUE_RIGHT) {
            isRTLValue = !isRTLValue;
        }
        final boolean isRTL = isRTLValue;
        final Image image = rightPanel.getStyle().getBgImage();
        UIManager uim = rightPanel.getUIManager();
        Image sh = (Image) uim.getThemeImageConstant("sideMenuShadowImage");
        if (sh == null) {
            sh = Resources.getSystemResource().getImage("sidemenu-shadow.png");
        }
        if (isRTL) {
            sh = sh.flipHorizontally(true);
        }
        final Image shadow = sh;

        if (m.getGlassPane() == null) {
            m.setGlassPane(new Painter() {
                Image img = image;

                public void paint(Graphics g, Rectangle rect) {
                    if (img == null) {
                        // will happen for areMutableImagesFast returning false on iOS and Windows Phone
                        Component c = (Component) rightPanel.getClientProperty("$parent");

                        // not sure what is happening here
                        if (c == null) {
                            return;
                        }
                        boolean b = c.isVisible();
                        c.setVisible(true);
                        if (isRTL) {
                            int x = Math.max(draggedX, rightPanel.getWidth()) - c.getWidth();
                            g.translate(x, 0);
                            Container.sidemenuBarTranslation = x;
                            if (shadow != null) {
                                g.tileImage(shadow, x + c.getWidth() - shadow.getWidth(), 0, shadow.getWidth(), rightPanel.getHeight());
                            }
                            c.paintComponent(g, true);
                            Container.sidemenuBarTranslation = 0;
                            g.translate(-x, 0);
                        } else {
                            int x = Math.min(draggedX, rightPanel.getX());
                            g.translate(x, 0);
                            Container.sidemenuBarTranslation = x;
                            if (shadow != null) {
                                g.tileImage(shadow, x - shadow.getWidth(), 0, shadow.getWidth(), rightPanel.getHeight());
                            }
                            c.paintComponent(g, true);
                            Container.sidemenuBarTranslation = 0;
                            g.translate(-x, 0);
                        }
                        c.setVisible(b);
                    } else {
                        if (Display.getInstance().areMutableImagesFast()) {
                            if (img.getHeight() != Display.getInstance().getDisplayHeight()) {
                                img = updateRightPanelBgImage(placement, parent);
                            }
                        }
                        if (isRTL) {
                            int x = Math.max(draggedX, rightPanel.getWidth()) - img.getWidth();
                            if (shadow != null) {
                                g.tileImage(shadow, x + img.getWidth() - shadow.getWidth(), 0, shadow.getWidth(), rightPanel.getHeight());
                            }
                            g.drawImage(img, x, 0);
                        } else {
                            int x = Math.min(draggedX, rightPanel.getX());
                            if (shadow != null) {
                                g.tileImage(shadow, x - shadow.getWidth(), 0, shadow.getWidth(), rightPanel.getHeight());
                            }
                            g.drawImage(img, x, 0);
                        }
                    }
                }
            });
        }

    }

    /**
     * Creates the side navigation component with the Commands
     *
     * @param commands the Command objects
     * @return the Component to display on the navigation
     */
    protected Container createSideNavigationComponent(Vector commands) {
        return createSideNavigationComponent(commands, null);
    }

    /**
     * Creates the side navigation component with the Commands
     *
     * @param commands the Command objects
     * @return the Component to display on the navigation
     */
    protected Container createSideNavigationComponent(Vector commands, String placement) {
        Container menu = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        if (getUIManager().isThemeConstant("paintsTitleBarBool", false)) {
            Container bar = new Container();
            bar.setUIID("StatusBarSideMenu");
            menu.addComponent(bar);
        }
        menu.setUIID("SideNavigationPanel");
        menu.setScrollableY(true);
        if (!getUIManager().isThemeConstant("sideMenuTensileDragBool", true)) {
            menu.setTensileDragEnabled(false);
        }
        for (int iter = commands.size() - 1; iter > -1; iter--) {
            Command c = (Command) commands.elementAt(iter);
            if (c.getClientProperty(COMMAND_PLACEMENT_KEY) != placement) {
                continue;
            }
            Component cmp = (Component) c.getClientProperty(COMMAND_SIDE_COMPONENT);
            if (cmp != null) {
                if (cmp.getParent() != null) {
                    cmp.getParent().removeAll();
                }
                if (c.getClientProperty(COMMAND_ACTIONABLE) != null) {
                    Container cnt = new Container(new BorderLayout());
                    cnt.addComponent(BorderLayout.CENTER, cmp);
                    Button btn = createTouchCommandButton(c);
                    btn.setParent(cnt);
                    cnt.setLeadComponent(btn);
                    menu.addComponent(cnt);
                } else {
                    menu.addComponent(cmp);
                }
                parent.initTitleBarStatus();
            } else {
                // special case: hide back button that doesn't have text, icon or a side component entry
                if(parent.getBackCommand() == c && (c.getCommandName() == null || c.getCommandName().length() == 0) &&
                        c.getIcon() == null) {
                    continue;
                }
                menu.addComponent(createTouchCommandButton(c));
            }
        }
        UIManager uim = menu.getUIManager();
        boolean shadowEnabled = uim.isThemeConstant("sideMenuShadowBool", true);
        Image sh = (Image) uim.getThemeImageConstant("sideMenuShadowImage");
        if (sh == null && shadowEnabled) {
            sh = Resources.getSystemResource().getImage("sidemenu-shadow.png");
        }
        final Image shadow = sh;

        if (shadow == null) {
            return menu;
        } else {
            Container main = new Container(new LayeredLayout());
            Label shadowLabel = new Label();
            shadowLabel.getStyle().setBackgroundType(Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_CENTER);
            shadowLabel.getStyle().setBgImage(shadow);
            shadowLabel.getStyle().setPadding(0, 0, 0, 0);
            shadowLabel.getStyle().setMargin(0, 0, 0, 0);
            shadowLabel.getStyle().setBgTransparency(0);
            Container c = new Container(new BorderLayout());
            if (placement == COMMAND_PLACEMENT_VALUE_RIGHT) {
                shadowLabel.setPreferredW(shadow.getWidth());
                c.addComponent(BorderLayout.WEST, shadowLabel);
                shadowLabel.getStyle().setBgImage(shadow.rotate180Degrees(true));
            } else {
                if (placement == COMMAND_PLACEMENT_VALUE_TOP) {
                    //shadowLabel.setPreferredH(shadow.getHeight());
                    //c.addComponent(BorderLayout.SOUTH, shadowLabel);
                    //shadowLabel.getStyle().setBgImage(shadow.rotate90Degrees(true));
                } else {
                    shadowLabel.setPreferredW(shadow.getWidth());
                    c.addComponent(BorderLayout.EAST, shadowLabel);
                }
            }

            main.addComponent(menu);
            main.addComponent(c);
            return main;
        }
    }

    /**
     * @inheritDoc
     */
    protected Button createTouchCommandButton(final Command c) {

        SideMenuBar.CommandWrapper wrapper = new SideMenuBar.CommandWrapper(c);
        Button b = super.createTouchCommandButton(wrapper);
        if (c.getIcon() == null) {
            b.setIcon(null);
        }
        b.setText(c.getCommandName());
        b.setTextPosition(Label.RIGHT);
        String uiid = (String)c.getClientProperty("uiid");
        if(uiid != null) {
            b.setUIID(uiid);
        } else {
            b.setUIID("SideCommand");
        }
        return b;
    }

    private Form createMenu(final String placement) {
        final Form m = new Form() {
            private boolean pressedInRightPanel;
            private boolean manualMotionLock;

            boolean shouldSendPointerReleaseToOtherForm() {
                return true;
            }

            void actionCommandImpl(Command cmd, ActionEvent ev) {
                if (cmd instanceof SideMenuBar.CommandWrapper) {
                    cmd = ((SideMenuBar.CommandWrapper) cmd).cmd;
                    ev = new ActionEvent(cmd);
                }
                final Command c = cmd;
                final ActionEvent e = ev;

                Display.getInstance().scheduleBackgroundTask(new Runnable() {

                    public void run() {
                        Display.getInstance().invokeAndBlock(new Runnable() {

                            public void run() {
                                while (Display.getInstance().getCurrent() != parent) {
                                    try {
                                        Thread.sleep(40);
                                    } catch (Exception ex) {
                                    }
                                }
                            }
                        });

                        Display.getInstance().callSerially(new Runnable() {

                            public void run() {
                                parent.actionCommandImpl(c, e);
                            }
                        });
                    }
                });

            }

            protected void sizeChanged(int w, int h) {
                Style formStyle = getStyle();
                int width = w - (formStyle.getMargin(isRTL(), Component.LEFT) + formStyle.getMargin(isRTL(), Component.RIGHT));
                //if the size changed event came from a keyboard open/close don't 
                //close the menu
                if (getWidth() != width) {
                    closeMenu();
                }
                super.sizeChanged(w, h);
            }

            public void pointerPressed(int x, int y) {
                if (manualMotionLock) {
                    return;
                }
                super.pointerPressed(x, y);
                if (rightPanel.contains(x, y)) {
                    pressedInRightPanel = true;
                }
            }

            public void pointerDragged(int[] x, int[] y) {
                if (manualMotionLock) {
                    return;
                }
                if (!transitionRunning && pressedInRightPanel) {
                    dragActivated = true;
                    pressedInRightPanel = false;
                }
                if (dragActivated) {
                    setMenuGlassPane(menu, placement);
                    draggedX = x[0];
                    repaint();
                    return;
                }
                super.pointerDragged(x, y);
            }

            public void pointerReleased(int x, int y) {
                if (manualMotionLock) {
                    return;
                }
                super.pointerReleased(x, y);
                boolean isRTLValue = isRTL();
                if (placement == COMMAND_PLACEMENT_VALUE_RIGHT) {
                    isRTLValue = !isRTLValue;
                }
                int displayWidth = Display.getInstance().getDisplayWidth();
                if (isRTLValue) {
                    if (!transitionRunning && dragActivated && x < (displayWidth - rightPanel.getWidth()) / 2) {
                        final Motion motion = Motion.createEaseInOutMotion(draggedX, rightPanel.getWidth(), 200);
                        motion.start();
                        registerAnimated(new Animation() {
                            public boolean animate() {
                                draggedX = motion.getValue();
                                if (motion.isFinished()) {
                                    dragActivated = false;
                                    Display.getInstance().getCurrent().setGlassPane(null);
                                    deregisterAnimated(this);
                                }
                                return true;
                            }

                            public void paint(Graphics g) {
                                repaint();
                            }
                        });
                        return;
                    }
                } else {
                    if (!transitionRunning && dragActivated && x > (displayWidth - rightPanel.getWidth()) / 2) {
                        final Motion motion = Motion.createEaseInOutMotion(draggedX, Display.getInstance().getDisplayWidth() - rightPanel.getWidth(), 200);
                        motion.start();
                        registerAnimated(new Animation() {
                            public boolean animate() {
                                draggedX = motion.getValue();
                                if (motion.isFinished()) {
                                    dragActivated = false;
                                    Display.getInstance().getCurrent().setGlassPane(null);
                                    deregisterAnimated(this);
                                }
                                return true;
                            }

                            public void paint(Graphics g) {
                                repaint();
                            }
                        });
                        return;
                    }
                }
                if (dragActivated || rightPanel.contains(x, y)) {
                    setMenuGlassPane(menu, placement);
                    draggedX = x;
                    int start = x;
                    int end = 0;
                    if (isRTLValue) {
                        end = getWidth();
                    }
                    final Motion motion = Motion.createEaseInOutMotion(start, end, 300);
                    motion.start();
                    manualMotionLock = true;
                    sideSwipePotential = false;
                    rightSideSwipePotential = false;
                    topSwipePotential = false;
                    registerAnimated(new Animation() {

                        public boolean animate() {
                            draggedX = motion.getValue();
                            if (motion.isFinished()) {
                                dragActivated = false;
                            }
                            return true;
                        }

                        public void paint(Graphics g) {
                            repaint();
                            if (draggedX == motion.getDestinationValue() && motion.isFinished()) {
                                parent.setTransitionInAnimator(CommonTransitions.createEmpty());
                                parent.show();
                                deregisterAnimated(this);
                                Display.getInstance().callSerially(new Runnable() {

                                    public void run() {
                                        clean();
                                    }
                                });
                            }

                        }
                    });
                }
            }

            public void keyReleased(int keyCode) {
                if (keyCode == leftSK) {
                    if (transitionRunning) {
                        return;
                    }
                    closeMenu();
                    return;
                }
                super.keyReleased(keyCode);
            }
        };

        m.setScrollable(false);
        m.removeComponentFromForm(m.getTitleArea());
        m.putClientProperty("Menu", "true");
        m.setTransitionInAnimator(CommonTransitions.createEmpty());
        m.setTransitionOutAnimator(CommonTransitions.createEmpty());
        m.setBackCommand(new Command("") {

            public void actionPerformed(ActionEvent evt) {
                if (transitionRunning) {
                    return;
                }
                closeMenu();
            }
        });
        m.setLayout(new BorderLayout());
        if (Display.getInstance().areMutableImagesFast()) {
            rightPanel = new Container(new BorderLayout());
        } else {
            rightPanel = new Container(new BorderLayout()) {
                public void paintBackground(Graphics g) {
                }

                public void paintBackgrounds(Graphics g) {
                }

                public void paint(Graphics g) {
                    Component c = (Component) rightPanel.getClientProperty("$parent");
                    
                    // not sure why its happening: https://code.google.com/p/codenameone/issues/detail?id=1072
                    if(c != null) {
                        boolean b = c.isVisible();
                        c.setVisible(true);
                        int x = getAbsoluteX();
                        g.translate(x, 0);
                        Container.sidemenuBarTranslation = x;
                        c.paintComponent(g, true);
                        Container.sidemenuBarTranslation = 0;
                        g.translate(-x, 0);
                        c.setVisible(b);
                    }
                }
            };
        }

        if (placement == COMMAND_PLACEMENT_VALUE_TOP) {
            if (Display.getInstance().isPortrait()) {
                if (Display.getInstance().isTablet()) {
                    rightPanel.setPreferredH(m.getHeight() * 2 / 3);
                } else {
                    rightPanel.setPreferredH(openButton.getHeight());
                }
            } else {
                if (Display.getInstance().isTablet()) {
                    rightPanel.setPreferredH(m.getHeight() * 3 / 4);
                } else {
                    rightPanel.setPreferredH(m.getHeight() * 4 / 10);
                }
            }
        } else {
            if (Display.getInstance().isPortrait()) {
                int v = 0;
                if (Display.getInstance().isTablet()) {
                    v = getUIManager().getThemeConstant("sideMenuSizeTabPortraitInt", -1);
                    if(v < 0) {
                        v = m.getWidth() * 2 / 3;
                    } else {
                        v = m.getWidth() / 100 * v;                        
                    }
                } else {
                    v = getUIManager().getThemeConstant("sideMenuSizePortraitInt", -1);
                    if(v < 0) {
                        v = openButton.getWidth();
                    } else {
                        v = m.getWidth() / 100 * v;                        
                    }
                }
                rightPanel.setPreferredW(v);
            } else {
                int v = 0;
                if (Display.getInstance().isTablet()) {
                    v = getUIManager().getThemeConstant("sideMenuSizeTabLandscapeInt", -1);
                    if(v < 0) {
                        v = m.getWidth() * 3 / 4;
                    } else {
                        v = m.getWidth() / 100 * v;                        
                    }
                } else {
                    v = getUIManager().getThemeConstant("sideMenuSizeLandscapeInt", -1);
                    if(v < 0) {
                        v = m.getWidth() * 4 / 10;
                    } else {
                        v = m.getWidth() / 100 * v;                        
                    }
                }
                rightPanel.setPreferredW(v);
            }
        }
        if (sidePanel != null) {
            sidePanel.removeAll();
            sidePanel = null;
        }
        sidePanel = createSideNavigationComponent(getCommands(), placement);
        if (placement == COMMAND_PLACEMENT_VALUE_RIGHT) {
            m.addComponent(BorderLayout.WEST, rightPanel);
            m.addComponent(BorderLayout.CENTER, sidePanel);
        } else {
            if (placement == COMMAND_PLACEMENT_VALUE_TOP) {
                m.addComponent(BorderLayout.NORTH, rightPanel);
                m.addComponent(BorderLayout.CENTER, sidePanel);
                Button button = new Button(" ");
                button.setUIID("Container");
                button.setPreferredH(Display.getInstance().getDisplayHeight() / 10);
                m.addComponent(BorderLayout.SOUTH, button);
                button.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        closeMenu();
                    }
                });
            } else {
                m.addComponent(BorderLayout.EAST, rightPanel);
                m.addComponent(BorderLayout.CENTER, sidePanel);
            }
        }
        m.putClientProperty("cn1$sideMenuParent", this);
        return m;
    }

    Image updateRightPanelBgImage(String placement, Component c) {
        Image img = rightPanel.getStyle().getBgImage();
        if (img != null && img.getHeight() == Display.getInstance().getDisplayHeight()) {
            return img;
        }
        boolean v = c.isVisible();
        c.setVisible(true);
        Image buffer = Image.createImage(Display.getInstance().getDisplayWidth(), Display.getInstance().getDisplayHeight());
        Graphics g = buffer.getGraphics();
        c.paintComponent(g);
        rightPanel.getStyle().setBgImage(buffer);
        c.setVisible(v);
        return buffer;
    }

    class MenuTransition extends Transition {

        private int speed;
        private boolean fwd;
        private Motion motion;
        private int position;
        private Image buffer;
        private int dest;
        private Image shadow;
        private String placement;
        private boolean isRTL;

        public MenuTransition(int speed, boolean fwd, int dest, String placement) {
            this.speed = speed;
            this.fwd = fwd;
            this.dest = dest;
            this.placement = placement;
        }

        public void initTransition() {
            super.initTransition();
            if (placement == COMMAND_PLACEMENT_VALUE_TOP) {
                if (Display.getInstance().areMutableImagesFast()) {
                    if (fwd) {
                        buffer = updateRightPanelBgImage(placement, getSource());
                        if (dest > -1) {
                            motion = Motion.createEaseInOutMotion(0, dest, speed);
                        } else {
                            motion = Motion.createEaseInOutMotion(0, buffer.getHeight() - rightPanel.getHeight(), speed);
                        }
                    } else {
                        buffer = updateRightPanelBgImage(placement, getDestination());
                        if (dest > -1) {
                            motion = Motion.createEaseInOutMotion(dest, 0, speed);
                        } else {
                            motion = Motion.createEaseInOutMotion(buffer.getHeight() - rightPanel.getHeight(), 0, speed);
                        }
                    }
                    rightPanel.getStyle().setBackgroundType(Style.BACKGROUND_IMAGE_ALIGNED_TOP);
                } else {
                    if (fwd) {
                        motion = Motion.createEaseInOutMotion(0, Display.getInstance().getDisplayHeight() - rightPanel.getHeight(), speed);
                        rightPanel.putClientProperty("$parent", getSource());
                    } else {
                        motion = Motion.createEaseInOutMotion(Display.getInstance().getDisplayHeight() - rightPanel.getHeight(), 0, speed);
                        rightPanel.putClientProperty("$parent", getDestination());
                    }
                }
            } else {
                isRTL = (getSource().getUIManager().getLookAndFeel().isRTL());
                if (placement == COMMAND_PLACEMENT_VALUE_RIGHT) {
                    isRTL = !isRTL;
                }
                if (Display.getInstance().areMutableImagesFast()) {
                    if (fwd) {
                        buffer = updateRightPanelBgImage(placement, getSource());
                        if (dest > -1) {
                            motion = Motion.createEaseInOutMotion(0, dest, speed);
                        } else {
                            motion = Motion.createEaseInOutMotion(0, buffer.getWidth() - rightPanel.getWidth(), speed);
                        }
                    } else {
                        buffer = updateRightPanelBgImage(placement, getDestination());
                        if (dest > -1) {
                            motion = Motion.createEaseInOutMotion(dest, 0, speed);
                        } else {
                            motion = Motion.createEaseInOutMotion(buffer.getWidth() - rightPanel.getWidth(), 0, speed);
                        }
                    }
                    if (isRTL) {
                        rightPanel.getStyle().setBackgroundType(Style.BACKGROUND_IMAGE_ALIGNED_TOP_RIGHT);
                    } else {
                        rightPanel.getStyle().setBackgroundType(Style.BACKGROUND_IMAGE_ALIGNED_TOP_LEFT);
                    }
                    rightPanel.getStyle().setBgImage(buffer);
                } else {
                    if (fwd) {
                        motion = Motion.createEaseInOutMotion(0, Display.getInstance().getDisplayWidth() - rightPanel.getWidth(), speed);
                        rightPanel.putClientProperty("$parent", getSource());
                    } else {
                        motion = Motion.createEaseInOutMotion(Display.getInstance().getDisplayWidth() - rightPanel.getWidth(), 0, speed);
                        rightPanel.putClientProperty("$parent", getDestination());
                    }
                }
            }
            boolean shadowEnabled = getUIManager().isThemeConstant("sideMenuShadowBool", true);

            shadow = (Image) getUIManager().getThemeImageConstant("sideMenuShadowImage");
            if (shadow == null && shadowEnabled) {
                shadow = Resources.getSystemResource().getImage("sidemenu-shadow.png");
            }

            motion.start();
        }

        public boolean animate() {
            if (motion != null) {
                position = motion.getValue();
                transitionRunning = !motion.isFinished();
                return transitionRunning;
            }
            transitionRunning = false;
            return false;
        }

        public void cleanup() {
            transitionRunning = false;
            clean();
        }

        public void paint(Graphics g) {
            if (Display.getInstance().areMutableImagesFast()) {
                // workaround for Android issue where the VKB breaks on screen size change
                if (buffer.getHeight() != Display.getInstance().getDisplayHeight()) {
                    if (fwd) {
                        buffer = updateRightPanelBgImage(placement, getSource());
                    } else {
                        buffer = updateRightPanelBgImage(placement, getDestination());
                    }
                }
            }
            Component src = getSource();
            Component dest = getDestination();
            if (placement == COMMAND_PLACEMENT_VALUE_TOP) {
                if (Display.getInstance().areMutableImagesFast()) {
                    if (fwd) {
                        dest.paintComponent(g, true);
                        g.drawImage(buffer, 0, position);
                    } else {
                        src.paintComponent(g, true);
                        g.drawImage(buffer, 0, position);
                    }
                } else {
                    if (fwd) {
                        dest.paintComponent(g, true);
                        g.translate(0, position);
                        src.paintComponent(g, true);
                        g.translate(0, -position);
                    } else {
                        src.paintComponent(g, true);
                        g.translate(0, position);
                        dest.paintComponent(g, true);
                        g.translate(-position, 0);
                    }
                }
                return;
            }
            if (isRTL) {
                position = position * -1;
            }
            if (Display.getInstance().areMutableImagesFast()) {
                if (fwd) {
                    dest.paintComponent(g, true);
                    if (shadow != null) {
                        g.tileImage(shadow, position - shadow.getWidth(), 0, shadow.getWidth(), src.getHeight());
                    }
                    g.drawImage(buffer, position, 0);
                } else {
                    src.paintComponent(g, true);
                    if (shadow != null) {
                        g.tileImage(shadow, position - shadow.getWidth(), 0, shadow.getWidth(), src.getHeight());
                    }
                    g.drawImage(buffer, position, 0);
                }
            } else {
                if (fwd) {
                    dest.paintComponent(g, true);
                    g.translate(position, 0);
                    Container.sidemenuBarTranslation = position;
                    if (shadow != null) {
                        g.tileImage(shadow, position - shadow.getWidth(), 0, shadow.getWidth(), src.getHeight());
                    }
                    src.paintComponent(g, true);
                    Container.sidemenuBarTranslation = 0;
                    g.translate(-position, 0);
                } else {
                    src.paintComponent(g, true);
                    g.translate(position, 0);
                    Container.sidemenuBarTranslation = position;
                    if (shadow != null) {
                        g.tileImage(shadow, position - shadow.getWidth(), 0, shadow.getWidth(), src.getHeight());
                    }
                    dest.paintComponent(g, true);
                    Container.sidemenuBarTranslation = 0;
                    g.translate(-position, 0);
                }
            }
        }
    }

    class CommandWrapper extends Command {

        Command cmd;

        public CommandWrapper(Command cmd) {
            super(cmd.getCommandName(), cmd.getIcon(), cmd.getId());
            this.cmd = cmd;
        }

        public Object getClientProperty(String key) {
            return this.cmd.getClientProperty(key);
        }

        public void putClientProperty(String key, Object value) {
            this.cmd.putClientProperty(key, value);
        }

        public boolean isEnabled() {
            return cmd.isEnabled();
        }

        public void setEnabled(boolean b) {
            cmd.setEnabled(b);
        }

        public void actionPerformed(final ActionEvent evt) {
            if (transitionRunning) {
                return;
            }
            closeMenu();
            clean();
            Display.getInstance().scheduleBackgroundTask(new Runnable() {

                public void run() {
                    Display.getInstance().invokeAndBlock(new Runnable() {

                        public void run() {
                            while (Display.getInstance().getCurrent() != parent) {
                                try {
                                    Thread.sleep(40);
                                } catch (Exception ex) {
                                }
                            }
                        }
                    });

                    Display.getInstance().callSerially(new Runnable() {

                        public void run() {
                            ActionEvent e = new ActionEvent(cmd);
                            parent.dispatchCommand(cmd, e);
                        }
                    });
                }
            });

        }
    }

    /**
     * Returns the Parent Form of this menu
     *
     * @return Form Object
     */
    public Form getParentForm() {
        return parent;
    }
}
