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

import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.animations.Motion;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.ScrollListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.LookAndFeel;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import java.util.Vector;

/**
 * Toolbar can replace the default TitleArea component. Toolbar allows
 * customizing the Form title with different commands on the title area, within
 * the side menu or the overflow menu.
 *
 * @author Chen
 */
public class Toolbar extends Container {

    private Component titleComponent;

    private ToolbarSideMenu sideMenu;

    private Vector overflowCommands;

    private Button menuButton;

    private ScrollListener scrollListener;

    private ActionListener releasedListener;

    private boolean scrollOff = false;

    private int initialY;

    private int actualPaneInitialY;

    private int actualPaneInitialH;

    private Motion hideShowMotion;

    private boolean showing;

    private boolean initialized = false;

    /**
     * Empty Constructor
     */
    public Toolbar() {
        setLayout(new BorderLayout());
        setUIID("Toolbar");
        sideMenu = new ToolbarSideMenu();
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
     * Sets the Toolbar title component. This method allow placing any component
     * in the Toolbar ceneter instead of the regular Label. Can be used to place
     * a TextField to preform search operations
     *
     * @param titleCmp Comoponent to place in the Toolbar center.
     */
    public void setTitleComponent(Component titleCmp) {
        checkIfInitialized();
        titleComponent = titleCmp;
        addComponent(BorderLayout.CENTER, titleComponent);
    }

    /**
     * Adds a Command to the overflow menu
     *
     * @param cmd a Command
     */
    public void addCommandToOverflowMenu(Command cmd) {
        checkIfInitialized();
        if (overflowCommands == null) {
            overflowCommands = new Vector();
        }
        overflowCommands.add(cmd);
        sideMenu.installRightCommands();
    }

    /**
     * Adds a Command to the side navigation menu
     *
     * @param cmd a Command
     */
    public void addCommandToSideMenu(Command cmd) {
        checkIfInitialized();
        sideMenu.addCommand(cmd);
        sideMenu.installMenuBar();
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
        cmd.putClientProperty(SideMenuBar.COMMAND_SIDE_COMPONENT, cmp);
        cmd.putClientProperty(SideMenuBar.COMMAND_ACTIONABLE, Boolean.TRUE);
        sideMenu.addCommand(cmd);
        sideMenu.installMenuBar();
    }

    /**
     * Adds a Component to the side navigation menu.
     *
     * @param cmp c Component to be added to the menu
     */
    public void addComponentToSideMenu(Component cmp) {
        checkIfInitialized();
        Command cmd = new Command("");
        cmd.putClientProperty(SideMenuBar.COMMAND_SIDE_COMPONENT, cmp);
        cmd.putClientProperty(SideMenuBar.COMMAND_ACTIONABLE, Boolean.FALSE);
        sideMenu.addCommand(cmd);
        sideMenu.installMenuBar();
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
     * @param cmd a Command
     */
    public void addCommandToLeftBar(Command cmd) {
        checkIfInitialized();
        cmd.putClientProperty("TitleCommand", Boolean.TRUE);
        cmd.putClientProperty("Left", Boolean.TRUE);
        sideMenu.addCommand(cmd, 0);
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
                + menu.getStyle().getPadding(LEFT)
                + menu.getStyle().getPadding(RIGHT));
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
            transitionOut = lf.getDefaultMenuTransitionOut();
        } else {
            transitionIn = CommonTransitions.createFade(300);
            transitionOut = CommonTransitions.createFade(300);
        }
        menu.setTransitionOutAnimator(transitionIn);
        menu.setTransitionInAnimator(transitionOut);
        return menu.show(th, height - th, marginLeft, marginRight, true);
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
            if (showing) {
                setY(val);
                actualPane.setY(actualPaneInitialY + val);
                actualPane.setHeight(actualPaneInitialH + getHeight() - val);
                actualPane.doLayout();
            } else {
                setY(val);
                actualPane.setY(actualPaneInitialY + val);
                actualPane.setHeight(actualPaneInitialH - val);
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
                        if (Math.abs(toolbarNewY) < 2) {
                            return;
                        }
                        toolbarNewY = Math.max(toolbarNewY, -getHeight());
                        toolbarNewY = Math.min(toolbarNewY, initialY);
                        int paneNewY = getHeight() + toolbarNewY;
                        if (toolbarNewY != getY()) {
                            setY(toolbarNewY);
                            int currentY = actualPane.getY();
                            actualPane.setY(paneNewY);
                            int paneHeight = actualPane.getHeight() + (currentY - paneNewY);
                            actualPane.setHeight(paneHeight);
                            actualPane.doLayout();
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

    class ToolbarSideMenu extends SideMenuBar {

        @Override
        protected Container getTitleAreaContainer() {
            return Toolbar.this;
        }

        @Override
        protected Component getTitleComponent() {
            return titleComponent;
        }

        @Override
        protected void initMenuBar(Form parent) {
            super.initMenuBar(parent);
            Component ta = parent.getTitleArea();
            parent.removeComponentFromForm(ta);
            parent.addComponentToForm(BorderLayout.NORTH, Toolbar.this);
            initialized = true;
            if (scrollOff) {
                bindScrollListener(true);
            }
            setTitle(parent.getTitle());
            parent.revalidate();
            initTitleBarStatus();
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
                Image i = (Image) UIManager.getInstance().getThemeImageConstant("menuImage");
                if (i == null) {
                    i = Resources.getSystemResource().getImage("of_menu.png");
                }
                menuButton = sideMenu.createTouchCommandButton(new Command("", i) {

                    public void actionPerformed(ActionEvent ev) {
                        sideMenu.showMenu();
                    }
                });
                menuButton.putClientProperty("overflow", Boolean.TRUE);
                menuButton.setUIID("TitleCommand");
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
                                    if (c.getClientProperty("overflow") != null) {
                                        return;
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
