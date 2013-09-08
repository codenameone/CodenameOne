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
    private Form parent;
    private Form menu;
    private Container rightPanel;
    private boolean dragActivated;
    private Transition in;
    private Transition out;
    private Container sidePanel;
    private int draggedX;
    private Command rightCommand;
    boolean sideSwipePotential;
    int initialDragX;
    int initialDragY;
    boolean transitionRunning;
    
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
     * @return true if a side menu is currently controlling the screen
     */
    public static boolean isShowing() {
        Form f = Display.getInstance().getCurrent();
        return f.getClientProperty("cn1$sideMenuParent") != null;
    }

    /**
     * Folds the current side menu if it is open, notice that the menu will close asynchronously 
     */
    public static void closeCurrentMenu() {
        Form f = Display.getInstance().getCurrent();
        SideMenuBar b = (SideMenuBar)f.getClientProperty("cn1$sideMenuParent");
        if(b != null && !b.transitionRunning) {
            b.closeMenu();
        }
    }
    
    /**
     * Folds the current side menu if it is open, when the menu is closed it will invoke the runnable
     * callback method
     * 
     * @param callback will be invoked when the menu is actually closed
     */
    public static void closeCurrentMenu(final Runnable callback) {
        Form f = Display.getInstance().getCurrent();
        SideMenuBar b = (SideMenuBar)f.getClientProperty("cn1$sideMenuParent");
        if(b != null && !b.transitionRunning) {
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
    protected void installMenuBar() {
        if (parent.getClientProperty("Menu") != null) {
            return;
        }
        super.installMenuBar();
        if(parent instanceof Dialog){
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
        openButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                openMenu();
            }
        });
        addOpenButton();
        
        if(uim.isThemeConstant("sideMenuFoldedSwipe", true)) {
            parent.addPointerDraggedListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    if(sideSwipePotential) {
                        int x = evt.getX();
                        int y = evt.getY();
                        if(Math.abs(y - initialDragY) > x - initialDragX) {
                            sideSwipePotential = false;
                            return;
                        }
                        evt.consume();
                        if(x - initialDragX > Display.getInstance().getDisplayWidth() / 15) {
                            draggedX = x;
                            dragActivated = true;
                            openMenu(0, draggedX);
                        }
                    }
                }
            });
            parent.addPointerPressedListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    sideSwipePotential = !transitionRunning && evt.getX() < Display.getInstance().getDisplayWidth() / 11;
                    initialDragX = evt.getX();
                    initialDragY = evt.getY();
                    if(sideSwipePotential) {
                        if(getCommandCount() == 0) {
                            sideSwipePotential = false;
                            return;
                        }
                        if(parent.getCommandCount() == 1) {
                            if(parent.getCommand(0) == parent.getBackCommand()) {
                                sideSwipePotential = false;
                                return;
                            }
                        }
                        Component c = Display.getInstance().getCurrent().getComponentAt(initialDragX, initialDragY);
                        if(c != null && c.shouldBlockSideSwipe()) {
                            sideSwipePotential = false;
                        }
                    }
                }
            });
        }
    }

    private void installRightCommand() {
        if(rightCommand != null) {
            Layout l = parent.getTitleArea().getLayout();
            if(l instanceof BorderLayout) {
                BorderLayout bl = (BorderLayout)l;
                Component east = bl.getEast();
                if(east == null) {
                    Button b = new Button(rightCommand);
                    b.setUIID("TitleCommand");
                    parent.getTitleArea().addComponent(BorderLayout.EAST, b);
                } else {
                    if(east instanceof Container) {
                        Container cnt = (Container)east;
                        if(cnt.getComponentCount() == 0) {
                            Button b = new Button(rightCommand);
                            b.setUIID("TitleCommand");
                            cnt.addComponent(b);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * @inheritDoc
     */
    public void addCommand(Command cmd) {
        if(cmd.getClientProperty("TitleCommand") != null) {
            rightCommand = cmd;
            addOpenButton();            
            installRightCommand();
            return;
        }
        super.addCommand(cmd);
        if(parent instanceof Dialog){
            return;
        }
        addOpenButton();
    }

    /**
     * @inheritDoc
     */
    public void setBackCommand(Command backCommand) {
        super.setBackCommand(backCommand);
        if(parent instanceof Dialog){
            return;
        }
        addOpenButton();
        installRightCommand();
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
        if(cmd.getClientProperty("TitleCommand") != null) {
            rightCommand = cmd;
            addOpenButton();            
            installRightCommand();
            return;
        }
        super.addCommand(cmd, index);
        if(parent instanceof Dialog){
            return;
        }
        addOpenButton();
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
        if(parent instanceof Dialog){
            return;
        }
        if (getCommandCount() == 0) {
            if (parent.getTitleComponent() != null) {
                parent.getTitleComponent().getParent().removeAll();
            }
            parent.getTitleArea().removeAll();
            parent.getTitleArea().addComponent(BorderLayout.CENTER, parent.getTitleComponent());
        }
        installRightCommand();
    }

    public void keyReleased(int keyCode) {
        if (keyCode == leftSK) {
            if(getCommandCount() == 0) {
                return;
            }
            if(parent.getCommandCount() == 1) {
                if(parent.getCommand(0) == parent.getBackCommand()) {
                    return;
                }
            }
            openMenu();
        }
        super.keyReleased(keyCode);
    }

    /**
     * Closes the menu if it is currently open
     */
    public void closeMenu() {
        if(transitionRunning) {
            return;
        }
        if (Display.getInstance().getCurrent()  == menu) {
            parent.showBack();
        }
    }

    /**
     * Opens the menu if it is currently closed
     */
    public void openMenu() {
        openMenu(-1, 300);
    }
    
    /**
     * Opens the menu if it is currently closed
     */
    void openMenu(int dest, int time) {
        if (Display.getInstance().getCurrent() == parent) {
            menu = createMenu();
            //replace transtions to perform the Form shift
            out = parent.getTransitionOutAnimator();
            in = parent.getTransitionInAnimator();
            parent.setTransitionOutAnimator(new SideMenuBar.MenuTransition(time, true, dest));
            parent.setTransitionInAnimator(new SideMenuBar.MenuTransition(300, false, -1));
            menu.show();
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

    private void addOpenButton() {
        if (parent != null && getCommandCount() > 0 && openButton.getParent() == null) {
            Container titleArea = parent.getTitleArea();
            titleArea.removeAll();
            titleArea.addComponent(BorderLayout.WEST, openButton);
            Label l = parent.getTitleComponent();
            if (l.getParent() != null) {
                l.getParent().removeComponent(l);
            }
            titleArea.addComponent(BorderLayout.CENTER, l);
            installRightCommand();
        }
    }

    private void clean() {
        if(out != null){
            parent.setTransitionOutAnimator(out);
        }
        if(in != null){
            parent.setTransitionInAnimator(in);
        }
        out = null;
        in = null;
    }

    private void setMenuGlassPane(Form m) {
        final boolean isRTL = m.isRTL();
        final Image i = rightPanel.getStyle().getBgImage();

        if (m.getGlassPane() == null) {
            m.setGlassPane(new Painter() {

                public void paint(Graphics g, Rectangle rect) {
                    if(i == null) {
                        // will happen for areMutableImagesFast returning false on iOS and Windows Phone
                        Component c = (Component)rightPanel.getClientProperty("$parent");
                        boolean b = c.isVisible();
                        c.setVisible(true);
                        if (isRTL) {
                            int x = Math.max(draggedX, rightPanel.getWidth()) - c.getWidth();
                            g.translate(x, 0);
                            c.paintComponent(g, true);
                            g.translate(-x, 0);
                        } else {
                            int x = Math.min(draggedX, rightPanel.getX());
                            g.translate(x, 0);
                            c.paintComponent(g, true);
                            g.translate(-x, 0);
                        }
                        c.setVisible(b);
                    } else {
                        if (isRTL) {
                            g.drawImage(i, Math.max(draggedX, rightPanel.getWidth()) - i.getWidth(), 0);
                        } else {
                            g.drawImage(i, Math.min(draggedX, rightPanel.getX()), 0);
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
        Container menu = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        menu.setUIID("SideNavigationPanel");
        menu.setScrollableY(true);
        for (int iter = commands.size() - 1; iter > -1; iter--) {
            Command c = (Command) commands.elementAt(iter);
            Component cmp = (Component) c.getClientProperty("SideComponent");
            if (cmp != null) {
                if (cmp.getParent() != null) {
                    cmp.getParent().removeAll();
                }
                if (c.getClientProperty("Actionable") != null) {
                    Container cnt = new Container(new BorderLayout());
                    cnt.addComponent(BorderLayout.CENTER, cmp);
                    cnt.setLeadComponent(createTouchCommandButton(c));
                    menu.addComponent(cnt);
                } else {
                    menu.addComponent(cmp);
                }
            } else {
                menu.addComponent(createTouchCommandButton(c));
            }
        }
        return menu;
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
        b.setUIID("SideCommand");
        return b;
    }

    private Form createMenu() {
        final Form m = new Form() {
            private boolean pressedInRightPanel;
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
                closeMenu();
                super.sizeChanged(w, h);
            }

            public void pointerPressed(int x, int y) {
                super.pointerPressed(x, y);
                if (rightPanel.contains(x, y)) {
                    pressedInRightPanel = true;
                }
            }

            public void pointerDragged(int[] x, int[] y) {
                if(!transitionRunning && pressedInRightPanel) {
                    dragActivated = true;
                    pressedInRightPanel = false;
                }
                if (dragActivated) {
                    setMenuGlassPane(menu);
                    draggedX = x[0];
                    repaint();
                    return;
                }
                super.pointerDragged(x, y);
            }

            public void pointerReleased(int x, int y) {
                super.pointerReleased(x, y);
                if(!transitionRunning && dragActivated && x > (Display.getInstance().getDisplayWidth() - rightPanel.getWidth()) / 2) {
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
                            /*if (draggedX == motion.getDestinationValue() && motion.isFinished()) {
                                parent.setTransitionInAnimator(CommonTransitions.createEmpty());
                                parent.show();
                                deregisterAnimated(this);
                                Display.getInstance().callSerially(new Runnable() {

                                    public void run() {
                                        clean();
                                    }
                                });
                            }*/

                        }
                    });
                    return;
                }
                if (dragActivated || rightPanel.contains(x, y)) {
                    setMenuGlassPane(menu);
                    draggedX = x;
                    int start = x;
                    int end = 0;
                    if (isRTL()) {
                        end = getWidth();
                    }
                    final Motion motion = Motion.createEaseInOutMotion(start, end, 300);
                    motion.start();
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
                    if(transitionRunning) {
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
                if(transitionRunning) {
                    return;
                }
                closeMenu();
            }
        });
        m.setLayout(new BorderLayout());
        if(Display.getInstance().areMutableImagesFast()) {
            rightPanel = new Container(new BorderLayout());
        } else {
            rightPanel = new Container(new BorderLayout()) {
                public void paintBackground(Graphics g) {}
                public void paintBackgrounds(Graphics g) {}
                public void paint(Graphics g) {
                    Component c = (Component)rightPanel.getClientProperty("$parent");
                    boolean b = c.isVisible();
                    c.setVisible(true);
                    int x = getAbsoluteX();
                    g.translate(x, 0);
                    c.paintComponent(g, true);
                    g.translate(-x, 0);
                    c.setVisible(b);
                }
            };
        }


        if (Display.getInstance().isPortrait()) {
            if (Display.getInstance().isTablet()) {
                rightPanel.setPreferredW(m.getWidth() * 2 / 3);
            } else {
                rightPanel.setPreferredW(openButton.getWidth());
            }
        } else {
            if (Display.getInstance().isTablet()) {
                rightPanel.setPreferredW(m.getWidth() * 3 / 4);
            } else {
                rightPanel.setPreferredW(m.getWidth() * 4 / 10);
            }
        }
        if (sidePanel != null) {
            sidePanel.removeAll();
            sidePanel = null;
        }
        sidePanel = createSideNavigationComponent(getCommands());
        m.addComponent(BorderLayout.CENTER, sidePanel);
        m.addComponent(BorderLayout.EAST, rightPanel);
        m.putClientProperty("cn1$sideMenuParent", this);
        return m;
    }

    class MenuTransition extends Transition {

        private int speed;
        private boolean fwd;
        private Motion motion;
        private int position;
        private Image buffer;
        private int dest;

        public MenuTransition(int speed, boolean fwd, int dest) {
            this.speed = speed;
            this.fwd = fwd;
            this.dest = dest;
        }

        public void initTransition() {
            super.initTransition();
            if(Display.getInstance().areMutableImagesFast()) {
                buffer = Image.createImage(Display.getInstance().getDisplayWidth(), Display.getInstance().getDisplayHeight());
                boolean isRTL = (getSource().getUIManager().getLookAndFeel().isRTL());
                if (fwd) {
                    Graphics g = buffer.getGraphics();
                    getSource().paintComponent(g);
                    if(dest > -1) {
                        motion = Motion.createEaseInOutMotion(0, dest, speed);
                    } else {
                        motion = Motion.createEaseInOutMotion(0, buffer.getWidth() - rightPanel.getWidth(), speed);
                    }
                } else {
                    Graphics g = buffer.getGraphics();
                    getDestination().paintComponent(g);
                    if(dest > -1) {
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
            Component src = getSource();
            Component dest = getDestination();
            if (src.isRTL()) {
                position = position * -1;
            }
            if(Display.getInstance().areMutableImagesFast()) {
                if (fwd) {
                    dest.paintComponent(g, true);
                    g.drawImage(buffer, position, 0);
                } else {
                    src.paintComponent(g, true);
                    g.drawImage(buffer, position, 0);
                }
            } else {
                if (fwd) {
                    dest.paintComponent(g, true);
                    g.translate(position, 0);
                    src.paintComponent(g, true);
                    g.translate(-position, 0);
                } else {
                    src.paintComponent(g, true);
                    g.translate(position, 0);
                    dest.paintComponent(g, true);
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

        public void actionPerformed(final ActionEvent evt) {
            if(transitionRunning) {
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
}
