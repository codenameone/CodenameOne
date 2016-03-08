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

import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.list.ListCellRenderer;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.LookAndFeel;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import java.util.Vector;

/**
 * This class represents the Form MenuBar.
 * This class is responsible to show the Form Commands and to handle device soft
 * keys, back key, clear key, etc...
 * This class can be overridden and replaced in the LookAndFeel
 * @see LookAndFeel#setMenuBarClass(java.lang.Class) 

 * @author Chen Fishbein
 */
public class MenuBar extends Container implements ActionListener {
    private boolean minimizeOnBack = true;
    private Command selectCommand;
    private Command defaultCommand;
    /**
     * Indicates the command that is defined as the back command out of this form.
     * A back command can be used both to map to a hardware button (e.g. on the Sony Ericsson devices)
     * and by elements such as transitions etc. to change the behavior based on 
     * direction (e.g. slide to the left to enter screen and slide to the right to exit with back).
     */
    private Command backCommand;
    /**
     * Indicates the command that is defined as the clear command out of this form similar
     * in spirit to the back command
     */
    private Command clearCommand;
    /**
     * This member holds the left soft key value
     */
    static int leftSK;
    /**
     * This member holds the right soft key value
     */
    static int rightSK;
    /**
     * This member holds the 2nd right soft key value
     * this is used for different BB devices
     */
    static int rightSK2;
    /**
     * This member holds the back command key value
     */
    static int backSK;
    /**
     * This member holds the clear command key value
     */
    static int clearSK;
    static int backspaceSK;

    static {
        // RIM and potentially other devices reinitialize the static initializer thus overriding
        // the new static values set by the initialized display 
        if (Display.getInstance() == null || Display.getInstance().getImplementation() == null) {
            leftSK = -6;
            rightSK = -7;
            rightSK2 = -7;
            backSK = -11;
            clearSK = -8;
            backspaceSK = -8;
        }
    }
    private Command menuCommand;
    private Vector commands = new Vector();
    private Button[] soft;
    private Command[] softCommand;
    private Button left;
    private Button right;
    private Button main;
    private ListCellRenderer menuCellRenderer;
    private Transition transitionIn;
    private Transition transitionOut;
    private Component commandList;
    private Style menuStyle;
    private Command selectMenuItem;
    private Command cancelMenuItem;
    private Form parent;
    private boolean thirdSoftButton;
    private boolean hideEmptyCommands;
    private boolean menuDisplaying;
    
    /**
     * Empty Constructor
     */
    public MenuBar() {
    }

    private int componentCountOffset(Container c) {
        if(getUIManager().isThemeConstant("paintsTitleBarBool", false)) {
            Container t = getTitleAreaContainer();
            if(t == c && ((BorderLayout)t.getLayout()).getNorth() != null) {
                return 1;
            }
        }
        return 0;
    }
    
    /**
     * Initialize the MenuBar
     * 
     * @param parent the associated Form
     */
    protected void initMenuBar(Form parent) {
        this.parent = parent;
        selectMenuItem = createMenuSelectCommand();
        cancelMenuItem = createMenuCancelCommand();
        UIManager manager = parent.getUIManager();
        LookAndFeel lf = manager.getLookAndFeel();
        //don't minimize the app if it's a Dialog
        minimizeOnBack = manager.isThemeConstant("minimizeOnBackBool", true) && !(parent instanceof Dialog);
        hideEmptyCommands = manager.isThemeConstant("hideEmptyCommandsBool", false);
        menuStyle = manager.getComponentStyle("Menu");
        setUIID("SoftButton");
        menuCommand = new Command(manager.localize("menu", "Menu"), lf.getMenuIcons()[2]);
        // use the slide transition by default
        if (lf.getDefaultMenuTransitionIn() != null || lf.getDefaultMenuTransitionOut() != null) {
            transitionIn = lf.getDefaultMenuTransitionIn();
            transitionOut = lf.getDefaultMenuTransitionOut();
        } else {
            transitionIn = CommonTransitions.createSlide(CommonTransitions.SLIDE_VERTICAL, true, 300, true);
            transitionOut = CommonTransitions.createSlide(CommonTransitions.SLIDE_VERTICAL, false, 300, true);
        }
        menuCellRenderer = lf.getMenuRenderer();
        int softkeyCount = Display.getInstance().getImplementation().getSoftkeyCount();
        thirdSoftButton = Display.getInstance().isThirdSoftButton();

        int commandBehavior = getCommandBehavior();
        if (softkeyCount > 1 && commandBehavior < Display.COMMAND_BEHAVIOR_BUTTON_BAR) {
            if (thirdSoftButton) {
                setLayout(new GridLayout(1, 3));
                soft = new Button[]{createSoftButton("SoftButtonCenter"), createSoftButton("SoftButtonLeft"), createSoftButton("SoftButtonRight")};
                main = soft[0];
                left = soft[1];
                right = soft[2];
                if (parent.isRTL()) {
                    right.setUIID("SoftButtonLeft");
                    left.setUIID("SoftButtonRight");
                    addComponent(right);
                    addComponent(main);
                    addComponent(left);
                } else {
                    addComponent(left);
                    addComponent(main);
                    addComponent(right);
                }
                if (isReverseSoftButtons()) {
                    Button b = soft[1];
                    soft[1] = soft[2];
                    soft[2] = b;
                }
            } else {
                setLayout(new GridLayout(1, 2));
                soft = new Button[]{createSoftButton("SoftButtonLeft"), createSoftButton("SoftButtonRight")};
                main = soft[0];
                left = soft[0];
                right = soft[1];
                if (parent.isRTL()) {
                    right.setUIID("SoftButtonLeft");
                    left.setUIID("SoftButtonRight");
                    addComponent(right);
                    addComponent(left);
                } else {
                    addComponent(left);
                    addComponent(right);
                }
                if (isReverseSoftButtons()) {
                    Button b = soft[0];
                    soft[0] = soft[1];
                    soft[1] = b;
                }
            }
            // It doesn't make sense for softbuttons to have ... at the end
            for (int iter = 0; iter < soft.length; iter++) {
                soft[iter].setEndsWith3Points(false);
            }
        } else {
            // special case for touch screens we still want the 3 softbutton areas...
            if (thirdSoftButton) {
                setLayout(new GridLayout(1, 3));
                soft = new Button[]{createSoftButton("SoftButtonCenter"), createSoftButton("SoftButtonLeft"), createSoftButton("SoftButtonRight")};
                main = soft[0];
                left = soft[1];
                right = soft[2];
                addComponent(left);
                addComponent(main);
                addComponent(right);
                if (isReverseSoftButtons()) {
                    Button b = soft[1];
                    soft[1] = soft[2];
                    soft[2] = b;
                }
            } else {
                soft = new Button[]{createSoftButton("SoftButtonCenter")};
            }
        }

        softCommand = new Command[soft.length];
    }

    /**
     * This method removes empty J2ME softbuttons that don't have a command
     */
    public void removeEmptySoftbuttons() {
        if(left != null && left.getParent() != null && "".equals(left.getText())) {
            left.getParent().removeComponent(left);
            revalidate();
        }
        if(right != null && right.getParent() != null && "".equals(right.getText())) {
            right.getParent().removeComponent(right);
            revalidate();
        }
    }
    
    public int getCommandBehavior() {
        int i = Display.getInstance().getCommandBehavior();
        if (Display.getInstance().getImplementation().getSoftkeyCount() == 0) {
            if (i != Display.COMMAND_BEHAVIOR_BUTTON_BAR && i != Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_BACK
                    && i != Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_RIGHT && i != Display.COMMAND_BEHAVIOR_ICS) {
                return Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_BACK;
            }
            return i;
        }
        if (i == Display.COMMAND_BEHAVIOR_DEFAULT) {
            if (Display.getInstance().isTouchScreenDevice()) {
                return Display.COMMAND_BEHAVIOR_TOUCH_MENU;
            }
            return Display.COMMAND_BEHAVIOR_SOFTKEY;
        }
        return i;
    }

    /**
     * Default command is invoked when a user presses fire, this functionality works
     * well in some situations but might collide with elements such as navigation
     * and combo boxes. Use with caution.
     * 
     * @param defaultCommand the command to treat as default
     */
    public void setDefaultCommand(Command defaultCommand) {
        this.defaultCommand = defaultCommand;
    }

    /**
     * Default command is invoked when a user presses fire, this functionality works
     * well in some situations but might collide with elements such as navigation
     * and combo boxes. Use with caution.
     * 
     * @return the command to treat as default
     */
    public Command getDefaultCommand() {
        if (selectCommand != null) {
            return selectCommand;
        }
        return defaultCommand;
    }

    /**
     * Indicates the command that is defined as the clear command in this form.
     * A clear command can be used both to map to a "clear" hardware button 
     * if such a button exists.
     * 
     * @param clearCommand the command to treat as the clear Command
     */
    public void setClearCommand(Command clearCommand) {
        this.clearCommand = clearCommand;
    }

    /**
     * Indicates the command that is defined as the clear command in this form.
     * A clear command can be used both to map to a "clear" hardware button 
     * if such a button exists.
     * 
     * @return the command to treat as the clear Command
     */
    public Command getClearCommand() {
        return clearCommand;
    }

    /**
     * Find the command component instance if such an instance exists
     * @param c the command instance
     * @return the button instance
     */
    public Button findCommandComponent(Command c) {
        Button b = findCommandComponent(c, this);
        if (b == null) {
            return findCommandComponent(c, getTitleAreaContainer());
        }
        return b;
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

    void adaptTitleLayoutBackCommandStructure() {
        Container t = getTitleAreaContainer();
        if (t.getComponentCount() - componentCountOffset(t) == 3) {
            return;
        }
        BorderLayout titleLayout = (BorderLayout) t.getLayout();
        if (Display.COMMAND_BEHAVIOR_ICS == getCommandBehavior()) {
            titleLayout.setCenterBehavior(BorderLayout.CENTER_BEHAVIOR_SCALE);
            getTitleComponent().getUnselectedStyle().setAlignment(Component.LEFT, true);
        } else {
            titleLayout.setCenterBehavior(BorderLayout.CENTER_BEHAVIOR_CENTER_ABSOLUTE);
        }
        t.removeAll();
        t.addComponent(BorderLayout.CENTER, getTitleComponent());
        Container leftContainer = new Container(new BoxLayout(BoxLayout.X_AXIS));
        Container rightContainer = new Container(new BoxLayout(BoxLayout.X_AXIS));
        t.addComponent(BorderLayout.EAST, rightContainer);
        t.addComponent(BorderLayout.WEST, leftContainer);
        initTitleBarStatus();
    }

    private Container findLeftTitleContainer() {
        Component cmp = ((BorderLayout) getTitleAreaContainer().getLayout()).getWest();
        if(cmp instanceof Container) {
            return (Container)cmp;
        }
        return null;
    }

    private Container findRightTitleContainer() {
        return (Container) ((BorderLayout) getTitleAreaContainer().getLayout()).getEast();
    }

    private void updateTitleCommandPlacement() {
        int commandBehavior = getCommandBehavior();
        Container t = getTitleAreaContainer();
        BorderLayout titleLayout = (BorderLayout) t.getLayout();
        if (getParent() == null) {
            installMenuBar();
        } else {
            if (getParent() == getTitleAreaContainer() && commandBehavior != Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_RIGHT
                    && commandBehavior != Display.COMMAND_BEHAVIOR_ICS) {
                getParent().removeComponent(this);
                installMenuBar();
            }
        }
        if (!(parent instanceof Dialog)) {
            if ((commandBehavior == Display.COMMAND_BEHAVIOR_ICS || 
                    commandBehavior == Display.COMMAND_BEHAVIOR_SIDE_NAVIGATION ||
                    commandBehavior == Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_BACK)
                    && parent.getTitle() != null && parent.getTitle().length() > 0) {
                synchronizeCommandsWithButtonsInBackbutton();

                return;
            } else {
                if (commandBehavior == Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_RIGHT
                        || commandBehavior == Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_BACK) {
                    if (getParent() != null) {
                        if (getParent() == getTitleAreaContainer()) {
                            return;
                        }
                        getParent().removeComponent(this);
                    }
                    //getTitleAreaContainer().addComponent(BorderLayout.EAST, findRightTitleContainer());
                    return;
                }
            }
        }
        if (t.getComponentCount() - componentCountOffset(t) > 1) {
            if (Display.COMMAND_BEHAVIOR_ICS == getCommandBehavior()) {
                titleLayout.setCenterBehavior(BorderLayout.CENTER_BEHAVIOR_SCALE);
            } else {
                titleLayout.setCenterBehavior(BorderLayout.CENTER_BEHAVIOR_CENTER_ABSOLUTE);
            }
            Component l = getTitleComponent();
            if (l.getParent() != null) {
                l.getParent().removeComponent(l);
            }
            t.removeAll();
            t.addComponent(BorderLayout.CENTER, l);
            initTitleBarStatus();
        }
    }

    /**
     * Indicates the command that is defined as the back command out of this form.
     * A back command can be used both to map to a hardware button (e.g. on the Sony Ericsson devices)
     * and by elements such as transitions etc. to change the behavior based on 
     * direction (e.g. slide to the left to enter screen and slide to the right to exit with back).
     * 
     * @param backCommand the command to treat as the back Command
     */
    public void setBackCommand(Command backCommand) {
        this.backCommand = backCommand;
        if(backCommand != null && UIManager.getInstance().isThemeConstant("hideBackCommandBool", false)) {
            removeCommand(backCommand);
        }
        
        int b = getCommandBehavior();
        if (b == Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_BACK || b == Display.COMMAND_BEHAVIOR_ICS
                || Display.getInstance().isNativeTitle()) {
            int i = commands.indexOf(backCommand);
            if (i > -1) {
                commands.removeElementAt(i);
            }
        }
        updateTitleCommandPlacement();
    }

    /**
     * Indicates the command that is defined as the back command out of this form.
     * A back command can be used both to map to a hardware button (e.g. on the Sony Ericsson devices)
     * and by elements such as transitions etc. to change the behavior based on 
     * direction (e.g. slide to the left to enter screen and slide to the right to exit with back).
     * 
     * @return the command to treat as the back Command
     */
    public Command getBackCommand() {
        return backCommand;
    }

    /**
     * The selectCommand is the command to invoke when a Component has foucs in
     * Third Soft Button state.
     * 
     * @return the select command
     */
    public Command getSelectCommand() {
        return selectCommand;
    }

    /**
     * Sets the select command
     * 
     * @param selectCommand
     */
    public void setSelectCommand(Command selectCommand) {
        this.selectCommand = selectCommand;
    }

    /**
     * Updates the command mapping to the softbuttons
     */
    private void updateCommands() {
        int commandBehavior = getCommandBehavior();
        if (commandBehavior == Display.COMMAND_BEHAVIOR_NATIVE) {
            Display.getInstance().getImplementation().setNativeCommands(commands);
            return;
        }
        if (commandBehavior >= Display.COMMAND_BEHAVIOR_BUTTON_BAR) {
            return;
        }
        if (soft.length > 1) {
            soft[0].setText("");
            soft[1].setText("");
            soft[0].setIcon(null);
            soft[1].setIcon(null);
            int commandSize = getCommandCount();
            if (soft.length > 2) {
                soft[2].setText("");
                if (commandSize > 2) {
                    if (commandSize > 3) {
                        softCommand[2] = menuCommand;
                    } else {
                        softCommand[2] = getCommand(getCommandCount() - 3);
                    }
                    soft[2].setText(softCommand[2].getCommandName());
                    soft[2].setIcon(softCommand[2].getIcon());
                } else {
                    softCommand[2] = null;
                }
            }
            if (commandSize > 0) {
                softCommand[0] = getCommand(getCommandCount() - 1);
                soft[0].setText(softCommand[0].getCommandName());
                soft[0].setIcon(softCommand[0].getIcon());
                if (commandSize > 1) {
                    if (soft.length == 2 && commandSize > 2) {
                        softCommand[1] = menuCommand;
                    } else {
                        softCommand[1] = getCommand(getCommandCount() - 2);
                    }
                    soft[1].setText(softCommand[1].getCommandName());
                    soft[1].setIcon(softCommand[1].getIcon());
                } else {
                    softCommand[1] = null;
                }
            } else {
                softCommand[0] = null;
                softCommand[1] = null;
            }

            // we need to add the menu bar to an already visible form
            if (commandSize == 1) {
                if (parent.isVisible()) {
                    parent.revalidate();
                }
            }
            repaint();
        }
    }

    /**
     * Invoked when a softbutton is pressed
     */
    public void actionPerformed(ActionEvent evt) {
        if (evt.isConsumed()) {
            return;
        }
        Object src = evt.getSource();
        if (commandList == null) {
            Button source = (Button) src;
            for (int iter = 0; iter < soft.length; iter++) {
                if (source == soft[iter]) {
                    if (softCommand[iter] == menuCommand) {
                        showMenu();
                        return;
                    }
                    if (softCommand[iter] != null) {
                        ActionEvent e = new ActionEvent(softCommand[iter],ActionEvent.Type.Command);
                        softCommand[iter].actionPerformed(e);
                        if (!e.isConsumed()) {
                            parent.actionCommandImpl(softCommand[iter]);
                        }
                    }
                    return;
                }
            }
        } else {
            // the list for the menu sent the event
            if (src instanceof Button) {
                for (int iter = 0; iter < soft.length; iter++) {
                    if (src == soft[iter]) {
                        Container parent = commandList.getParent();
                        while (parent != null) {
                            if (parent instanceof Dialog) {
                                ((Dialog) parent).actionCommand(softCommand[iter]);
                                return;
                            }
                            parent = parent.getParent();
                        }
                    }
                }
            }
            Command c = getComponentSelectedCommand(commandList);
            if (!c.isEnabled()) {
                return;
            }
            Container p = commandList.getParent();
            while (p != null) {
                if (p instanceof Dialog) {
                    ((Dialog) p).actionCommand(c);
                    return;
                }
                p = p.getParent();
            }
        }

    }

    /**
     * Creates a soft button Component
     * @return the softbutton component
     */
    protected Button createSoftButton(String uiid) {
        Button b = new Button();
        b.setUIID(uiid);
        b.addActionListener(this);
        b.setFocusable(false);
        b.setTactileTouch(true);
        updateSoftButtonStyle(b);
        return b;
    }

    private void updateSoftButtonStyle(Button b) {
        int softkeyCount = Display.getInstance().getImplementation().getSoftkeyCount();
        if (softkeyCount < 2) {
            b.getStyle().setMargin(0, 0, 0, 0);
            b.getStyle().setPadding(0, 0, 0, 0);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setUnselectedStyle(Style style) {
        style.setMargin(Component.TOP, 0, true);
        style.setMargin(Component.BOTTOM, 0, true);
        super.setUnselectedStyle(style);
        if (soft != null) {
            for (int iter = 0; iter < soft.length; iter++) {
                updateSoftButtonStyle(soft[iter]);
            }
        }
    }

    /**
     * Prevents scaling down of the menu when there is no text on the menu bar 
     */
    protected Dimension calcPreferredSize() {
        if (soft.length > 1) {
            Dimension d = super.calcPreferredSize();
            if ((soft[0].getText() == null || soft[0].getText().equals(""))
                    && (soft[1].getText() == null || soft[1].getText().equals(""))
                    && soft[0].getIcon() == null && soft[1].getIcon() == null
                    && (soft.length < 3
                    || ((soft[2].getText() == null || soft[2].getText().equals("")) && soft[2].getIcon() == null))) {
                d.setHeight(0);
            }
            return d;
        }
        return super.calcPreferredSize();
    }

    /**
     * Sets the menu transitions for showing/hiding the menu, can be null...
     */
    public void setTransitions(Transition transitionIn, Transition transitionOut) {
        this.transitionIn = transitionIn;
        this.transitionOut = transitionOut;
    }

    /**
     * This method will return true if the menu dialog is currently displaying
     * @return true of the menu dialog is displaying
     */
    public boolean isMenuShowing(){
        return menuDisplaying;
    }
    
    /**
     * This method shows the menu on the Form.
     * The method creates a Dialog with the commands and calls showMenuDialog.
     * The method blocks until the user dispose the dialog.
     */
    public void showMenu() {
        final Dialog d = new Dialog("Menu", "");
        d.setDisposeWhenPointerOutOfBounds(true);
        d.setMenu(true);

        d.setTransitionInAnimator(transitionIn);
        d.setTransitionOutAnimator(transitionOut);
        d.setLayout(new BorderLayout());
        d.setScrollable(false);
        //calling parent.createCommandComponent is done only for backward 
        //compatability reasons, in the next version this call be replaced with 
        //calling directly to createCommandComponent
        ((Form) d).getMenuBar().commandList = createCommandComponent(commands);
        if (menuCellRenderer != null && ((Form) d).getMenuBar().commandList instanceof List) {
            ((List) ((Form) d).getMenuBar().commandList).setListCellRenderer(menuCellRenderer);
        }
        d.getContentPane().getStyle().setMargin(0, 0, 0, 0);
        d.addComponent(BorderLayout.CENTER, ((Form) d).getMenuBar().commandList);
        if (thirdSoftButton) {
            d.addCommand(selectMenuItem);
            d.addCommand(cancelMenuItem);
        } else {
            d.addCommand(cancelMenuItem);
            if (soft.length > 1) {
                d.addCommand(selectMenuItem);
            }
        }
        d.setClearCommand(cancelMenuItem);
        d.setBackCommand(cancelMenuItem);

        if (((Form) d).getMenuBar().commandList instanceof List) {
            ((List) ((Form) d).getMenuBar().commandList).addActionListener(((Form) d).getMenuBar());
        }
        menuDisplaying = true;
        Command result = showMenuDialog(d);
        menuDisplaying = false;
        if (result != cancelMenuItem) {
            Command c = null;
            if (result == selectMenuItem) {
                c = getComponentSelectedCommand(((Form) d).getMenuBar().commandList);
                if (c != null) {
                    ActionEvent e = new ActionEvent(c,ActionEvent.Type.Command);
                    c.actionPerformed(e);
                }
            } else {
                c = result;
                // a touch menu will always send its commands on its own...
                if (!isTouchMenus()) {
                    c = result;
                    if (c != null) {
                        ActionEvent e = new ActionEvent(c,ActionEvent.Type.Command);
                        c.actionPerformed(e);
                    }
                }
            }
            // menu item was handled internally in a touch interface that is not a touch menu
            if (c != null) {
                parent.actionCommandImpl(c);
            }
        }
        if (((Form) d).getMenuBar().commandList instanceof List) {
            ((List) ((Form) d).getMenuBar().commandList).removeActionListener(((Form) d).getMenuBar());
        }

        Form upcoming = Display.getInstance().getCurrentUpcoming();
        if (upcoming == parent) {
            d.disposeImpl();
        } else {
            parent.tint = (upcoming instanceof Dialog);
        }
    }

    Button[] getSoftButtons() {
        return soft;
    }

    private void updateBackBorderToRTL(Style s) {
        Border b = s.getBorder();
        if(b != null) {
            b = b.mirrorBorder();
            s.setBorder(b);
        }
    }
    
    void verifyBackCommandRTL(Button bg) {
        if(getCommandBehavior() == Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_BACK && isRTL()) {
            if(bg.getClientProperty("$cn1BackRTL") == null) {
                bg.putClientProperty("$cn1BackRTL", Boolean.TRUE);
                updateBackBorderToRTL(bg.getUnselectedStyle());
                updateBackBorderToRTL(bg.getSelectedStyle());
                updateBackBorderToRTL(bg.getPressedStyle());
            }
        }
    }
    
    private void addTwoTitleButtons(Container leftContainer, Container rightContainer) {
        ensureCommandsInContainer(getCommand(0), null, rightContainer, "TitleCommand", null);
        if (parent.getBackCommand() != null) {
            ensureCommandsInContainer(parent.getBackCommand(), null, leftContainer, "BackCommand", null);
            updateGridCommands(1);
        } else {
            if (getCommandBehavior() == Display.COMMAND_BEHAVIOR_ICS) {
                ensureCommandsInContainer(getCommand(1), null, rightContainer, "TitleCommand", null);
            } else {
                ensureCommandsInContainer(getCommand(1), null, leftContainer, "TitleCommand", null);
            }
            updateGridCommands(2);
        }
    }

    private void updateGridCommands(int startOffset) {
        int cmdCount = getCommandCount() - startOffset;
        if (cmdCount <= 0 || getCommandBehavior() == Display.COMMAND_BEHAVIOR_ICS) {
            return;
        }
        setLayout(new GridLayout(1, cmdCount));
        while (cmdCount < getComponentCount()) {
            removeComponent(getComponentAt(getComponentCount() - 1));
        }
        int off = startOffset;
        while (getComponentCount() < cmdCount) {
            Button btn = new Button(getCommand(off));
            btn.setUIID("TouchCommand");
            off++;
            addComponent(btn);
        }
        for (int iter = 0; iter < cmdCount; iter++) {
            Button btn = (Button) getComponentAt(iter);
            if (btn.getCommand() != getCommand(iter + startOffset)) {
                btn.setCommand(getCommand(iter + startOffset));
            }
        }
    }

    protected Button createBackCommandButton() {
        Button back = new Button(parent.getBackCommand());
        if (getCommandBehavior() == Display.COMMAND_BEHAVIOR_ICS) {
            back.setText("<");
            back.setIcon(null);
        }
        return back;
    }

    void synchronizeCommandsWithButtonsInBackbutton() {
        adaptTitleLayoutBackCommandStructure();
        Container leftContainer = findLeftTitleContainer();
        
        if(leftContainer == null && getCommandBehavior() == Display.COMMAND_BEHAVIOR_SIDE_NAVIGATION) {
            return;
        }
        
        Container rightContainer = findRightTitleContainer();

        int componentCount = getCommandCount();
        if (parent.getBackCommand() != null && 
                (!UIManager.getInstance().isThemeConstant("hideBackCommandBool", false) || 
                UIManager.getInstance().isThemeConstant("showBackCommandOnTitleBool", false))) {
            if (leftContainer.getComponentCount() - componentCountOffset(leftContainer) <= 0) {
                Button back = createBackCommandButton();
                leftContainer.addComponent(back);
                if(!back.getUIID().equals("BackCommand")) {
                    back.setUIID("BackCommand");
                }
                hideEmptyCommand(back);
                verifyBackCommandRTL(back);
            } else {
                Button b = (Button) leftContainer.getComponentAt(0);
                if (b.getCommand() != parent.getBackCommand()) {
                    b.setCommand(parent.getBackCommand());
                    if(!b.getUIID().equals("BackCommand")) {
                        b.setUIID("BackCommand");
                    }
                    verifyBackCommandRTL(b);
                    hideEmptyCommand(b);
                }
            }
            componentCount++;
        }

        switch (componentCount) {
            case 0:
                leftContainer.removeAll();
                rightContainer.removeAll();
                removeAll();
                initTitleBarStatus();
                break;
            case 1:
                if (parent.getBackCommand() != null) {
                    rightContainer.removeAll();
                    ensureCommandsInContainer(parent.getBackCommand(), null, leftContainer, "BackCommand", null);
                } else {
                    leftContainer.removeAll();
                    ensureCommandsInContainer(getCommand(0), null, rightContainer, "TitleCommand", null);
                }
                if (getCommandBehavior() != Display.COMMAND_BEHAVIOR_ICS) {
                    removeAll();
                    initTitleBarStatus();
                }
                break;
            case 2:
                addTwoTitleButtons(leftContainer, rightContainer);
                break;
            case 3:
                if (Display.getInstance().isTablet()) {
                    if (parent.getBackCommand() != null) {
                        //ensureCommandsInContainer(getCommand(1), getCommand(0), rightContainer, "TitleCommand", "TitleCommand");
                        ensureCommandsInContainer(parent.getBackCommand(), null, leftContainer, "BackCommand", null);
                    } else {
                        //ensureCommandsInContainer(getCommand(2), getCommand(1), rightContainer, "TitleCommand", "TitleCommand");
                        //ensureCommandsInContainer(getCommand(0), null, leftContainer, "TitleCommand", null);
                    }
                    if (getCommandBehavior() != Display.COMMAND_BEHAVIOR_ICS) {
                        removeAll();
                        initTitleBarStatus();
                    }
                } else {
                    addTwoTitleButtons(leftContainer, rightContainer);
                    break;
                }
            default:
                if (getCommandBehavior() == Display.COMMAND_BEHAVIOR_ICS) {
                    rightContainer.removeAll();
                    Image i = (Image) UIManager.getInstance().getThemeImageConstant("menuImage");
                    if (i == null) {
                        //i = Resources.getSystemResource().getImage("of_menu.png");
                        i = FontImage.createMaterial(FontImage.MATERIAL_MORE_VERT, getUIManager().getComponentStyle("TouchCommand"));
                    }                    
                    Button menu = createTouchCommandButton(new Command("", i) {

                        public void actionPerformed(ActionEvent ev) {
                            showMenu();
                        }
                    });
                    //rightContainer.addComponent(createTouchCommandButton(getCommand(0)));
                    //rightContainer.addComponent(createTouchCommandButton(getCommand(1)));
                    rightContainer.addComponent(menu);
                    rightContainer.revalidate();
                } else {
                    if (Display.getInstance().isTablet()) {
                        ensureCommandsInContainer(getCommand(0), getCommand(2), rightContainer, "TitleCommand", "TitleCommand");
                        if (parent.getBackCommand() != null) {
                            ensureCommandsInContainer(parent.getBackCommand(), getCommand(1), leftContainer, "BackCommand", "TitleCommand");
                            updateGridCommands(3);
                        } else {
                            ensureCommandsInContainer(getCommand(1), getCommand(3), leftContainer, "TitleCommand", "TitleCommand");
                            updateGridCommands(4);
                        }
                    } else {
                        addTwoTitleButtons(leftContainer, rightContainer);
                    }
                }
                break;
        }
    }

    void hideEmptyCommand(Button b) {
        if(hideEmptyCommands) {
            if(b.getText() == null || b.getText().length() == 0) {
                b.setUIID("Container");
            }
        }
    }
    
    private void ensureCommandsInContainer(Command a, Command b, Container c, String styleA, String styleB) {
        if (c.getComponentCount() - componentCountOffset(c) == 0) {
            Button btn = new Button(a);
            if(!btn.getUIID().equals(styleA)) {
                btn.setUIID(styleA);
            }
            c.addComponent(btn);
            if (b != null) {
                btn = new Button(b);
                if(!btn.getUIID().equals(styleB)) {
                    btn.setUIID(styleB);
                }
                c.addComponent(btn);
            }
            hideEmptyCommand(btn);
            return;
        }
        if (c.getComponentCount() - componentCountOffset(c) == 1) {
            Button btn = (Button) c.getComponentAt(0);
            if(!btn.getUIID().equals(styleA)) {
                btn.setUIID(styleA);
            }
            if (btn.getCommand() != a) {
                btn.setCommand(a);
            }
            if (b != null) {
                btn = new Button(b);
                if(!btn.getUIID().equals(styleB)) {
                    btn.setUIID(styleB);
                }
                c.addComponent(btn);
            }
            hideEmptyCommand(btn);
            return;
        }
        if (c.getComponentCount() - componentCountOffset(c) == 2) {
            Button btn = (Button) c.getComponentAt(0);
            if(!btn.getUIID().equals(styleA)) {
                btn.setUIID(styleA);
            }
            if (btn.getCommand() != a) {
                btn.setCommand(a);
            }
            hideEmptyCommand(btn);
            if (b != null) {
                btn = (Button) c.getComponentAt(1);
                if(!btn.getUIID().equals(styleB)) {
                    btn.setUIID(styleB);
                }
                if (btn.getCommand() != b) {
                    btn.setCommand(b);
                }
                hideEmptyCommand(btn);
            } else {
                c.removeComponent(c.getComponentAt(1));
            }
            return;
        }
    }

    /**
     * Adds a Command to the MenuBar
     * 
     * @param cmd Command to add
     */
    public void addCommand(Command cmd) {
        // prevent duplicate commands which might happen in some edge cases
        // with the select command
        if (commands.contains(cmd)) {
            return;
        }

        if(getBackCommand() == cmd && UIManager.getInstance().isThemeConstant("hideBackCommandBool", false)) {
            return;
        }
        
        // special case for default commands which are placed at the end and aren't overriden later
        if (soft.length > 2 && cmd == parent.getDefaultCommand()) {
            commands.addElement(cmd);
        } else {
            commands.insertElementAt(cmd, 0);
        }

        if (!(parent instanceof Dialog)) {
            int behavior = getCommandBehavior();
            if (behavior == Display.COMMAND_BEHAVIOR_BUTTON_BAR || behavior == Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_BACK
                    || behavior == Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_RIGHT || behavior == Display.COMMAND_BEHAVIOR_ICS) {
                if (behavior == Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_BACK && (cmd == parent.getBackCommand()
                        || findCommandComponent(cmd) != null)) {
                    return;
                }
                if (parent.getBackCommand() != cmd) {
                    if ((behavior == Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_BACK || 
                            behavior == Display.COMMAND_BEHAVIOR_ICS ||
                            behavior == Display.COMMAND_BEHAVIOR_SIDE_NAVIGATION)
                            && parent.getTitle() != null && parent.getTitle().length() > 0) {
                        synchronizeCommandsWithButtonsInBackbutton();
                        return;
                    }

                    setLayout(new GridLayout(1, getCommandCount()));
                    addComponent(createTouchCommandButton(cmd));
                } else {
                    commands.removeElement(cmd);
                }
                return;
            }
        }

        updateCommands();
    }

    /**
     * Returns the command occupying the given index
     * 
     * @param index offset of the command
     * @return the command at the given index
     */
    public Command getCommand(int index) {
        if(index < 0 || index >= commands.size()){
            return null;
        }
        return (Command) commands.elementAt(index);
    }

    /**
     * Returns number of commands
     * 
     * @return number of commands
     */
    public int getCommandCount() {
        return commands.size();
    }

    /**
     * Add a Command to the MenuBar
     * 
     * @param cmd Command to Add
     * @param index determines the order of the added commands
     */
    protected void addCommand(Command cmd, int index) {
        if (getCommandCount() == 0 && parent != null) {
            installMenuBar();
        }
        // prevent duplicate commands which might happen in some edge cases
        // with the select command
        if (commands.contains(cmd)) {
            return;
        }
        commands.insertElementAt(cmd, index);
        if (!(parent instanceof Dialog)) {
            int behavior = getCommandBehavior();
            if (behavior == Display.COMMAND_BEHAVIOR_BUTTON_BAR
                    || behavior == Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_BACK
                    || behavior == Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_RIGHT
                    || behavior == Display.COMMAND_BEHAVIOR_ICS
                    || behavior == Display.COMMAND_BEHAVIOR_SIDE_NAVIGATION) {
                if (behavior == Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_BACK && cmd == parent.getBackCommand()) {
                    return;
                }
                if(behavior == Display.COMMAND_BEHAVIOR_SIDE_NAVIGATION) {
                    return;
                }
                if ((behavior == Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_BACK || behavior == Display.COMMAND_BEHAVIOR_ICS)
                        && parent.getTitle() != null && parent.getTitle().length() > 0) {
                    synchronizeCommandsWithButtonsInBackbutton();
                    return;
                }
                if (parent.getBackCommand() != cmd) {
                    if (behavior != Display.COMMAND_BEHAVIOR_ICS) {
                        setLayout(new GridLayout(1, getComponentCount() + 1));
                        addComponent(Math.min(getComponentCount(), index), createTouchCommandButton(cmd));
                        revalidate();
                    }
                } else {
                    commands.removeElement(cmd);
                }
                return;
            }
        }
        updateCommands();
    }

    /**
     * Adds the MenuBar on the parent Form
     */
    protected void installMenuBar() {
        if (getParent() == null) {
            int type = getCommandBehavior();
            if (type == Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_RIGHT 
                    || type == Display.COMMAND_BEHAVIOR_ICS
                    || type == Display.COMMAND_BEHAVIOR_SIDE_NAVIGATION) {
                //getTitleAreaContainer().addComponent(BorderLayout.EAST, this);
                return;
            }
            int softkeyCount = Display.getInstance().getImplementation().getSoftkeyCount();
            if (softkeyCount > 1 || type == Display.COMMAND_BEHAVIOR_BUTTON_BAR
                    || type == Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_BACK) {
                if(Display.getInstance().getProperty("adPaddingBottom", null) == null) {
                    parent.addComponentToForm(BorderLayout.SOUTH, this);
                }
            }
        }
    }

    /**
     * Removes the MenuBar from the parent Form
     */
    protected void unInstallMenuBar() {
        parent.removeComponentFromForm(this);
        Container t = getTitleAreaContainer();
        BorderLayout titleLayout = (BorderLayout) t.getLayout();
        titleLayout.setCenterBehavior(BorderLayout.CENTER_BEHAVIOR_SCALE);
        Component l = getTitleComponent();
        t.removeAll();
        if (l.getParent() != null) {
            l.getParent().removeComponent(l);
        }
        t.addComponent(BorderLayout.CENTER, l);
        initTitleBarStatus();
    }

    /**
     * Remove all commands from the menuBar
     */
    protected void removeAllCommands() {
        commands.removeAllElements();
        int behavior = getCommandBehavior();
        if (behavior == Display.COMMAND_BEHAVIOR_BUTTON_BAR
                || behavior == Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_BACK
                || behavior == Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_RIGHT
                || behavior == Display.COMMAND_BEHAVIOR_ICS) {
            
            if(getTitleComponent() != null){
                getTitleComponent().getParent().removeAll();
            }
            getTitleAreaContainer().removeAll();
            getTitleAreaContainer().addComponent(BorderLayout.CENTER, getTitleComponent());            
            removeAll();
            initTitleBarStatus();
            return;
        }
        updateCommands();
    }

    /**
     * Removes a Command from the MenuBar
     * 
     * @param cmd Command to remove
     */
    protected void removeCommand(Command cmd) {
        int behavior = getCommandBehavior();
        if (behavior == Display.COMMAND_BEHAVIOR_BUTTON_BAR
                || behavior == Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_BACK
                || behavior == Display.COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_RIGHT
                || behavior == Display.COMMAND_BEHAVIOR_ICS) {
            int i = commands.indexOf(cmd);
            if (i > -1) {
                commands.removeElementAt(i);
                Button b = findCommandComponent(cmd);
                if (b != null && b.getParent() != null) {
                    b.getParent().removeComponent(b);
                }
                if (getCommandCount() > 0) {
                    setLayout(new GridLayout(1, getCommandCount()));
                }
            }
            return;
        }
        commands.removeElement(cmd);
        updateCommands();
    }

    void addSelectCommand(String selectText) {
        if (thirdSoftButton) {
            if (selectCommand == null) {
                selectCommand = createSelectCommand();
            }
            selectCommand.setCommandName(selectText);
            addCommand(selectCommand);
        }
    }

    void removeSelectCommand() {
        if (thirdSoftButton) {
            removeCommand(selectCommand);
        }
    }

    /**
     * Factory method that returns the Form select Command.
     * This Command is used when Display.getInstance().isThirdSoftButton() 
     * returns true.
     * This method can be overridden to customize the Command on the Form.
     * 
     * @return Command
     */
    protected Command createSelectCommand() {
        return new Command(parent.getUIManager().localize("select", "Select"));
    }

    /**
     * Factory method that returns the Form Menu select Command.
     * This method can be overridden to customize the Command on the Form.
     * 
     * @return Command
     */
    protected Command createMenuSelectCommand() {
        UIManager manager = parent.getUIManager();
        LookAndFeel lf = manager.getLookAndFeel();
        return new Command(manager.localize("select", "Select"), lf.getMenuIcons()[0]);
    }

    /**
     * Factory method that returns the Form Menu cancel Command.
     * This method can be overridden to customize the Command on the Form.
     * 
     * @return Command
     */
    protected Command createMenuCancelCommand() {
        UIManager manager = parent.getUIManager();
        LookAndFeel lf = manager.getLookAndFeel();
        return new Command(manager.localize("cancel", "Cancel"), lf.getMenuIcons()[1]);
    }

    /**
     * The MenuBar default implementation shows the menu commands in a List 
     * contained in a Dialog.
     * This method replaces the menu ListCellRenderer of the Menu List.
     * 
     * @param menuCellRenderer
     */
    public void setMenuCellRenderer(ListCellRenderer menuCellRenderer) {
        this.menuCellRenderer = menuCellRenderer;
    }

    /**
     * Returns the Menu Dialog Style
     * 
     * @return Menu Dialog Style
     */
    public Style getMenuStyle() {
        return menuStyle;
    }

    static boolean isLSK(int keyCode) {
        return keyCode == leftSK;
    }

    static boolean isRSK(int keyCode) {
        return keyCode == rightSK || keyCode == rightSK2;
    }

    /**
     * This method returns true if the MenuBar should handle the given keycode.
     * 
     * @param keyCode to determine if the MenuBar is responsible for.
     * @return true if the keycode is a MenuBar related keycode such as softkey,
     * back button, clear button, ...
     */
    public boolean handlesKeycode(int keyCode) {
        int game = Display.getInstance().getGameAction(keyCode);
        if (keyCode == leftSK || (keyCode == rightSK || keyCode == rightSK2) || keyCode == backSK
                || (keyCode == clearSK && clearCommand != null)
                || (keyCode == backspaceSK && clearCommand != null)
                || (thirdSoftButton && game == Display.GAME_FIRE)) {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void keyPressed(int keyCode) {
        int commandBehavior = getCommandBehavior();
        if (commandBehavior >= Display.COMMAND_BEHAVIOR_BUTTON_BAR) {
            return;
        }
        if (getCommandCount() > 0) {
            if (keyCode == leftSK) {
                if (left != null) {
                    left.pressed();
                }
            } else {
                // it might be a back command or the fire...
                if ((keyCode == rightSK || keyCode == rightSK2)) {
                    if (right != null) {
                        right.pressed();
                    }
                } else {
                    if (Display.getInstance().getGameAction(keyCode) == Display.GAME_FIRE) {
                        main.pressed();
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void keyReleased(int keyCode) {
        int commandBehavior = getCommandBehavior();
        if (commandBehavior >= Display.COMMAND_BEHAVIOR_BUTTON_BAR && keyCode != backSK && keyCode != clearSK && keyCode != backspaceSK) {
            return;
        }
        if (getCommandCount() > 0) {
            int softkeyCount = Display.getInstance().getImplementation().getSoftkeyCount();
            if (softkeyCount < 2 && keyCode == leftSK) {
                if (commandList != null) {
                    Container parent = commandList.getParent();
                    while (parent != null) {
                        if (parent instanceof Dialog && ((Dialog) parent).isMenu()) {
                            return;
                        }
                        parent = parent.getParent();
                    }
                }
                showMenu();
                return;
            } else {
                if (keyCode == leftSK) {
                    if (left != null) {
                        left.released();
                    }
                    return;
                } else {
                    // it might be a back command...
                    if ((keyCode == rightSK || keyCode == rightSK2)) {
                        if (right != null) {
                            right.released();
                        }
                        return;
                    } else {
                        if (Display.getInstance().getGameAction(keyCode) == Display.GAME_FIRE) {
                            main.released();
                            return;
                        }
                    }
                }
            }
        }

        // allows a back/clear command to occur regardless of whether the
        // command was added to the form
        Command c = null;
        if (keyCode == backSK) {
            // the back command should be invoked
            c = parent.getBackCommand();
            if(c == null && minimizeOnBack) {
                Display.getInstance().minimizeApplication();
                return;
            }
        } else {
            if (keyCode == clearSK || keyCode == backspaceSK) {
                c = getClearCommand();
            }
        }
        if (c != null) {
            ActionEvent ev = new ActionEvent(c, keyCode);
            c.actionPerformed(ev);
            if (!ev.isConsumed()) {
                parent.actionCommandImpl(c);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void refreshTheme(boolean merge) {
        super.refreshTheme(merge);
        UIManager manager = parent.getUIManager();
        if (menuStyle.isModified() && merge) {
            menuStyle.merge(manager.getComponentStyle("Menu"));
        } else {
            menuStyle = manager.getComponentStyle("Menu");
        }
        if (menuCellRenderer != null) {
            List tmp = new List();
            tmp.setListCellRenderer(menuCellRenderer);
            tmp.refreshTheme(merge);
        }
        for (int iter = 0; iter < soft.length; iter++) {
            updateSoftButtonStyle(soft[iter]);
        }

        revalidate();
    }

    /*private void fixCommandAlignment() {
    if (left != null) {
    if (parent.isRTL()) {
    left.setAlignment(Label.RIGHT);
    right.setAlignment(Label.LEFT);
    } else {
    left.setAlignment(Label.LEFT);
    right.setAlignment(Label.RIGHT);
    }
    if(main != null && main != left && main != right) {
    main.setAlignment(CENTER);
    }
    }
    }*/
    /**
     * A menu is implemented as a dialog, this method allows you to override dialog
     * display in order to customize the dialog menu in various ways
     * 
     * @param menu a dialog containing menu options that can be customized
     * @return the command selected by the user in the dialog (not menu) Select 
     * or Cancel
     */
    protected Command showMenuDialog(Dialog menu) {
        UIManager manager = parent.getUIManager();
        boolean pref = manager.isThemeConstant("menuPrefSizeBool", false);
        int height;
        int marginLeft;
        int marginRight = 0;
        if (pref) {
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
        } else {
            float menuWidthPercent = 1 - Float.parseFloat(manager.getThemeConstant("menuWidthPercent", "75")) / 100;
            float menuHeightPercent = 1 - Float.parseFloat(manager.getThemeConstant("menuHeightPercent", "50")) / 100;
            height = (int) (parent.getHeight() * menuHeightPercent);
            marginLeft = (int) (parent.getWidth() * menuWidthPercent);
        }

        if (isReverseSoftButtons()) {
            marginRight = marginLeft;
            marginLeft = 0;
        }
        if (getCommandBehavior() == Display.COMMAND_BEHAVIOR_ICS) {
            menu.setTransitionOutAnimator(transitionIn);
            menu.setTransitionInAnimator(transitionOut);
            int th = getTitleAreaContainer().getHeight();
            return menu.show(th, height - th, marginLeft, marginRight, true);
        } else {
            if (manager.getLookAndFeel().isTouchMenus() && manager.isThemeConstant("PackTouchMenuBool", true)) {
                return menu.showPacked(BorderLayout.SOUTH, true);
            } else {
                return menu.show(height, 0, marginLeft, marginRight, true);
            }
        }
    }

    /**
     * Allows an individual form to reverse the layout direction of the softbuttons, this method is RTL
     * sensitive and might reverse the result based on RTL state
     * 
     * @return The value of UIManager.getInstance().getLookAndFeel().isReverseSoftButtons()
     */
    protected boolean isReverseSoftButtons() {
        LookAndFeel lf = parent.getUIManager().getLookAndFeel();
        if (isRTL()) {
            return !lf.isReverseSoftButtons();
        }
        return lf.isReverseSoftButtons();
    }

    /**
     * Calculates the amount of columns to give to the touch commands within the 
     * grid
     * 
     * @param grid container that will be arranged in the grid containing the 
     * components
     * @return an integer representing the touch command grid size
     */
    protected int calculateTouchCommandGridColumns(Container grid) {
        int count = grid.getComponentCount();
        int maxWidth = 10;
        for (int iter = 0; iter < count; iter++) {
            Component c = grid.getComponentAt(iter);
            Style s = c.getUnselectedStyle();
            // bidi doesn't matter since this is just a summary of width
            maxWidth = Math.max(maxWidth,
                    c.getPreferredW()
                    + s.getMargin(false, LEFT) + s.getMargin(false, RIGHT));
        }
        return Math.max(2, Display.getInstance().getDisplayWidth() / maxWidth);
    }

    /**
     * Sets the command UIID to the given UIID, notice that this won't work for all menu types since some menu
     * types might be implemented natively or as a list in which case the UIID won't apply!
     * @param cmd the command
     * @param uiid the uiid for the given command
     */
    public void setCommandUIID(Command cmd, String uiid) {
        Button b = findCommandComponent(cmd);
        if(b != null) {
            b.setUIID(uiid);
            revalidate();
        }
        cmd.putClientProperty("cn1$CommandUIID", uiid);
    }
    
    /**
     * Creates a touch command for use as a touch menu item
     * 
     * @param c command to map into the returned button
     * @return a button that would fire the touch command appropriately
     */
    protected Button createTouchCommandButton(Command c) {
        Button b = new Button(c);
        if (b.getIcon() == null) {
            // some themes look awful without any icon
            b.setIcon((Image) parent.getUIManager().getThemeImageConstant("defaultCommandImage"));
        } else {
            if (UIManager.getInstance().isThemeConstant("commandAsIconBool", false)) {
                b.setText("");
            }
        }
        b.setTactileTouch(true);
        b.setTextPosition(Label.BOTTOM);
        b.setEndsWith3Points(false);
        String uiid = (String)c.getClientProperty("cn1$CommandUIID");
        if(uiid != null) {
            b.setUIID(uiid);
        } else {
            b.setUIID("TouchCommand");
        }
        Integer gap = (Integer)c.getClientProperty("iconGap");
        if(gap != null) {
            b.setGap(gap.intValue());
        }
        return b;
    }

    /**
     * Creates the component containing the commands within the given vector
     * used for showing the menu dialog, this method calls the createCommandList
     * method by default however it allows more elaborate menu creation.
     *
     * @param commands list of command objects
     * @return Component that will result in the parent menu dialog recieving a command event
     */
    protected Component createCommandComponent(Vector commands) {
        UIManager manager = parent.getUIManager();
        // Create a touch based menu interface
        if (manager.getLookAndFeel().isTouchMenus()) {
            Container menu = new Container();
            menu.setScrollableY(true);
            for (int iter = 0; iter < commands.size(); iter++) {
                Command c = (Command) commands.elementAt(iter);
                menu.addComponent(createTouchCommandButton(c));
            }
            if (!manager.isThemeConstant("touchCommandFlowBool", false)) {
                int cols = calculateTouchCommandGridColumns(menu);
                if (cols > getCommandCount()) {
                    cols = getCommandCount();
                }
                int rows = Math.max(1, getCommandCount() / cols + (getCommandCount() % cols != 0 ? 1 : 0));
                if (rows > 1) {
                    // try to prevent too many columns concentraiting within a single row
                    int remainingColumns = (rows * cols) % getCommandCount();
                    int newCols = cols;
                    int newRows = rows;
                    while (remainingColumns != 0 && remainingColumns > 1 && newCols >= 2) {
                        newCols--;
                        newRows = Math.max(1, getCommandCount() / newCols + (getCommandCount() % newCols != 0 ? 1 : 0));
                        if (newRows != rows) {
                            break;
                        }
                        remainingColumns = (newRows * newCols) % getCommandCount();
                    }
                    if (newRows == rows) {
                        cols = newCols;
                        rows = newRows;
                    }
                }
                GridLayout g = new GridLayout(rows, cols);
                g.setFillLastRow(manager.isThemeConstant("touchCommandFillBool", true));
                menu.setLayout(g);
            } else {
                ((FlowLayout) menu.getLayout()).setFillRows(true);
            }
            menu.setPreferredW(Display.getInstance().getDisplayWidth());
            return menu;
        }
        return createCommandList(commands);
    }

    /**
     * This method returns a Vector of Command objects
     * 
     * @return Vector of Command objects
     */
    protected Vector getCommands() {
        return commands;
    }

    /**
     * Creates the list component containing the commands within the given vector
     * used for showing the menu dialog
     * 
     * @param commands list of command objects
     * @return List object
     */
    protected List createCommandList(Vector commands) {
        List l = new List(commands);
        l.setUIID("CommandList");
        Component c = (Component) l.getRenderer();
        c.setUIID("Command");
        c = l.getRenderer().getListFocusComponent(l);
        c.setUIID("CommandFocus");

        l.setFixedSelection(List.FIXED_NONE_CYCLIC);
        if (parent.getUIManager().isThemeConstant("menuPrefSizeBool", false)) {
            // an entry way down in the list might be noticeably wider
            l.setListSizeCalculationSampleCount(50);
        }
        return l;
    }

    Command getComponentSelectedCommand(Component cmp) {
        if (cmp instanceof List) {
            List l = (List) cmp;
            return (Command) l.getSelectedItem();
        } else {
            cmp = cmp.getComponentForm().getFocused();
            if (cmp instanceof Button) {
                return ((Button) cmp).getCommand();
            }
        }
        // nothing to do for this case...
        return null;
    }

    /**
     * This method returns the select menu item, when a menu is opened
     * @return select Command
     */
    protected Command getSelectMenuItem() {
        return selectMenuItem;
    }

    /**
     * This method returns the cancel menu item, when a menu is opened
     * @return cancel Command
     */
    protected Command getCancelMenuItem() {
        return cancelMenuItem;
    }

    /**
     * When set to true the physical back button will minimize the application
     * @return the minimizeOnBack
     */
    public boolean isMinimizeOnBack() {
        return minimizeOnBack;
    }

    /**
     * When set to true the physical back button will minimize the application
     * @param minimizeOnBack the minimizeOnBack to set
     */
    public void setMinimizeOnBack(boolean minimizeOnBack) {
        this.minimizeOnBack = minimizeOnBack;
    }

    /**
     * {@inheritDoc}
     */
    protected int getDragRegionStatus(int x, int y) {
        return DRAG_REGION_NOT_DRAGGABLE;
    }
    
    /**
     * Returns the parent Form title area
     * @return the title area Container
     */
    protected Container getTitleAreaContainer(){
        return parent.getTitleArea();
    }
    
    /**
     * Gets the Form titleComponent
     * @return titleComponent
     */
    protected Component getTitleComponent(){
        return parent.getTitleComponent();
    }
    
    Form getParentForm(){
        return parent;
    }
    
    void initTitleBarStatus() {
        parent.initTitleBarStatus();
    }
    
    private boolean isTouchMenus() {
        int t = getCommandBehavior();
        return t == Display.COMMAND_BEHAVIOR_TOUCH_MENU ||
                (t == Display.COMMAND_BEHAVIOR_DEFAULT && Display.getInstance().isTouchScreenDevice());
    }
}