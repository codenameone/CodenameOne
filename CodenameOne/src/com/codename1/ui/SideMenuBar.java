/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
     * @inheritDoc
     */
    protected void installMenuBar() {
        if (parent.getClientProperty("Menu") != null) {
            return;
        }
        super.installMenuBar();
        openButton = new Button();
        openButton.setUIID("MenuButton");
        Image i = (Image) parent.getUIManager().getThemeImageConstant("sideMenuImage");
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
    }

    /**
     * @inheritDoc
     */
    public void addCommand(Command cmd) {
        super.addCommand(cmd);
        addOpenButton();
    }

    /**
     * @inheritDoc
     */
    public void setBackCommand(Command backCommand) {
        super.setBackCommand(backCommand);
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
    protected void addCommand(Command cmd, int index) {
        super.addCommand(cmd, index);
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
        if (getCommandCount() == 0) {
            if (parent.getTitleComponent() != null) {
                parent.getTitleComponent().getParent().removeAll();
            }
            parent.getTitleArea().removeAll();
            parent.getTitleArea().addComponent(BorderLayout.CENTER, parent.getTitleComponent());

        }
    }

    public void keyReleased(int keyCode) {
         if (keyCode == leftSK) {
             openMenu();
         }
    }

    /**
     * Closes the menu if it is currently open
     */
    public void closeMenu() {
        if (Display.getInstance().getCurrent() == menu) {
            parent.showBack();
        }
    }

    /**
     * Opens the menu if it is currently close
     */
    public void openMenu() {
        if (Display.getInstance().getCurrent() == parent) {
            menu = createMenu();
            //replace transtions to perform the Form shift
            out = parent.getTransitionOutAnimator();
            in = parent.getTransitionInAnimator();
            parent.setTransitionOutAnimator(new SideMenuBar.MenuTransition(400, true));
            parent.setTransitionInAnimator(new SideMenuBar.MenuTransition(400, false));
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
        }
    }

    private void clean() {
        parent.setTransitionOutAnimator(out);
        parent.setTransitionInAnimator(in);
        out = null;
        in = null;
    }

    private void setMenuGlassPane(Form m) {
        final boolean isRTL = m.isRTL();
        final Image i = rightPanel.getStyle().getBgImage();

        if (m.getGlassPane() == null) {
            m.setGlassPane(new Painter() {

                public void paint(Graphics g, Rectangle rect) {
                    if (isRTL) {
                        g.drawImage(i, Math.max(draggedX, rightPanel.getWidth()) - i.getWidth(), 0);
                    } else {
                        g.drawImage(i, Math.min(draggedX, rightPanel.getX()), 0);
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
                                while (Display.getInstance().getCurrent() != parent){
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
                    dragActivated = true;
                }
            }

            public void pointerDragged(int[] x, int[] y) {
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
                            }

                        }
                    });
                }
            }

            public void keyReleased(int keyCode) {
                if (keyCode == backSK) {
                    closeMenu();
                }
            }
        };

        m.setScrollable(false);
        m.removeComponentFromForm(m.getTitleArea());
        m.putClientProperty("Menu", "true");
        m.setTransitionInAnimator(CommonTransitions.createEmpty());
        m.setTransitionOutAnimator(CommonTransitions.createEmpty());
        m.setBackCommand(new Command("") {

            public void actionPerformed(ActionEvent evt) {
                closeMenu();
            }
        });
        m.setLayout(new BorderLayout());
        rightPanel = new Container(new BorderLayout());


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

        return m;
    }

    class MenuTransition extends Transition {

        private int speed;
        private boolean fwd;
        private Motion motion;
        private int position;
        private Image buffer;

        public MenuTransition(int speed, boolean fwd) {
            this.speed = speed;
            this.fwd = fwd;

        }

        public void initTransition() {
            super.initTransition();
            buffer = Image.createImage(Display.getInstance().getDisplayWidth(), Display.getInstance().getDisplayHeight());
            boolean isRTL = (getSource().getUIManager().getLookAndFeel().isRTL());
            if (fwd) {
                Graphics g = buffer.getGraphics();
                getSource().paintComponent(g);
                motion = Motion.createEaseInOutMotion(0, buffer.getWidth() - rightPanel.getWidth(), speed);
            } else {
                Graphics g = buffer.getGraphics();
                getDestination().paintComponent(g);
                motion = Motion.createEaseInOutMotion(buffer.getWidth() - rightPanel.getWidth(), 0, speed);
            }
            if (isRTL) {
                rightPanel.getStyle().setBackgroundType(Style.BACKGROUND_IMAGE_ALIGNED_TOP_RIGHT);
            } else {
                rightPanel.getStyle().setBackgroundType(Style.BACKGROUND_IMAGE_ALIGNED_TOP_LEFT);
            }
            rightPanel.getStyle().setBgImage(buffer);

            motion.start();
        }

        public boolean animate() {
            if(motion != null){
                position = motion.getValue();
                return !motion.isFinished();
            }
            return false;
        }

        public void cleanup() {
            clean();
        }

        public void paint(Graphics g) {
            Component src = getSource();
            Component dest = getDestination();
            if (src.isRTL()) {
                position = position * -1;
            }
            if (fwd) {
                dest.paintComponent(g, true);
                g.drawImage(buffer, position, 0);
            } else {
                src.paintComponent(g, true);
                g.drawImage(buffer, position, 0);
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
