/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.javase.simulator;

import com.codename1.impl.javase.util.SwingUtils;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.*;

/**
 *
 * @author shannah
 */
public class AppFrame extends JPanel {
    
    private static final long PREFS_UPDATE_DELAY = 2000L;
    private static final long PREFS_UPDATE_PERIOD = 2000L;
    private boolean initialized;
    
    private JTabbedPane topPanel, leftPanel, centerPanel, rightPanel, bottomPanel;
    private JSplitPane outerSplit, innerSplit, centerSplit;

    private Map<String,AppPanel> appPanels = new HashMap<String,AppPanel>();
    private Map<AppPanel, AppPanelWindowHandle> windows = new HashMap<AppPanel, AppPanelWindowHandle>();
    


    private boolean shouldSavePreferences = false;
    
    
    private JScrollPane canvasScroller;
    private JComponent canvas;

    private class AppPanelWindowHandle {
        private AppPanel panel;
        private JFrame window;
        private AppFrame.FrameLocation previousLocation;

        private AppPanelWindowHandle(AppPanel panel, AppFrame.FrameLocation previousLocation) {
            this.panel = panel;
            this.previousLocation = previousLocation;
        }

        private void fitWindowOntoScreen() {
            if (window == null) throw new IllegalStateException("Cannot fit window onto screen until it is created");
            Point pos = new Point(window.getBounds().getLocation());
            pos.x += window.getWidth()/2;
            pos.y += window.getHeight()/2;
            Rectangle screenBounds = SwingUtils.getScreenBoundsAt(pos);
            if (screenBounds == null) {
                window.pack();
                window.setLocationByPlatform(true);
            } else if (!screenBounds.contains(window.getBounds())) {
                Rectangle newBounds = new Rectangle();
                newBounds.width = window.getWidth();
                newBounds.height = window.getHeight();
                newBounds.x = window.getX();
                if (newBounds.x < screenBounds.x) {
                    newBounds.x = screenBounds.x;
                } else if (newBounds.x > screenBounds.width + screenBounds.x) {
                    newBounds.x = screenBounds.x;
                }
                if (newBounds.y < screenBounds.y) {
                    newBounds.y = screenBounds.y;
                } else if (newBounds.y > screenBounds.y + screenBounds.height) {
                    newBounds.y = screenBounds.y;
                }

                if (newBounds.x + 100 > screenBounds.x+screenBounds.width) {
                    newBounds.x = screenBounds.x + screenBounds.width - 100;
                }
                if (newBounds.y + 100 > screenBounds.y + screenBounds.height) {
                    newBounds.y = screenBounds.y + screenBounds.height - 100;
                }
                window.setBounds(newBounds);

            }


        }

        private void show() {
            if (window == null) {
                window = new JFrame(panel.getLabel());
                window.getContentPane().setLayout(new BorderLayout());

                //window.setPreferredSize(new Dimension(panel.getPreferredSize()));
                Container parent = panel.getParent();
                if (parent != null) {
                    parent.remove(panel);
                }
                window.getContentPane().add(panel, BorderLayout.CENTER);
                //window.setSize(new Dimension(window.getPreferredSize()));
                //window.setLocationByPlatform(true);
                window.setBounds(panel.getPreferredWindowBounds());



                window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                window.addWindowListener(frameListener);
                window.addComponentListener(frameListener);
                window.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        FrameLocation target = previousLocation;
                        if (target == null && panel.getPreferredFrame() != FrameLocation.SeparateWindow) {
                            target = panel.getPreferredFrame();
                        }
                        if (target != null) {
                            if (windows.containsKey(panel)) {
                                moveTo(panel, target);
                            }
                        } else {
                            if (windows.containsKey(panel)) {
                                removePanel(panel);
                            }
                        }
                    }
                });
                fitWindowOntoScreen();

            }
            window.setVisible(true);
        }

        private void dispose() {
            if (window != null) {
                window.dispose();
                window = null;
            }
        }
    }
    
    
    public AppFrame(String name) {
        setLayout(new BorderLayout());
        setName(name);
        initAppFrameUI();
    }
    
    protected void initAppFrameUI() {
        
        Dimension preferredSize = new Dimension(SwingUtils.getScreenSize());
        setMinimumSize(new Dimension(300, 300));
        
        setPreferredSize(SwingUtils.getScreenSize());
        
        topPanel = new JTabbedPane();
        topPanel.setName("topPanel");
        leftPanel = new JTabbedPane();
        leftPanel.setName("leftPanel");
        centerPanel = new JTabbedPane();
        centerPanel.setName("centerPanel");
        rightPanel = new JTabbedPane();
        rightPanel.setName("rightPanel");
        bottomPanel = new JTabbedPane();
        bottomPanel.setName("bottomPanel");
        initPanels(topPanel, leftPanel, centerPanel, rightPanel, bottomPanel);
        
        innerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        innerSplit.setName("innerSplit");
        setDividerLocationIfChanged(innerSplit, 600);
        outerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        outerSplit.setName("outerSplit");
        setDividerLocationIfChanged(outerSplit, 0);
        centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        centerSplit.setName("centerSplit");
        setDividerLocationIfChanged(centerSplit, 600);
        outerSplit.setLeftComponent(leftPanel);
        outerSplit.setRightComponent(innerSplit);
        innerSplit.setTopComponent(centerSplit);
        innerSplit.setBottomComponent(bottomPanel);
        centerSplit.setLeftComponent(centerPanel);
        centerSplit.setRightComponent(rightPanel); 
        
        
        
        applyPreferences();




        addComponentListener(frameListener);
        outerSplit.addPropertyChangeListener(frameListener);
        innerSplit.addPropertyChangeListener(frameListener);
        centerSplit.addPropertyChangeListener(frameListener);
        add(outerSplit, BorderLayout.CENTER);
        updateAppFrameUI();
        initialized = true;
    }
    
    String getPreferencesPrefix() {
        String prefix = getName();
        if (prefix == null) return "AppFrame.";
        return prefix + ".AppFrame.";
        
    }
             
    
    private static Preferences prefs;
    private static Preferences prefs() {
        if (prefs == null) {
            prefs = Preferences.userNodeForPackage(AppFrame.class);
        }
        return prefs;
    }
    
    
    private void applyPreferences() {
        String prefix = getPreferencesPrefix();
        Preferences prefs = prefs();
        centerSplit.setDividerLocation(Math.max(0, prefs.getInt(prefix+"centerSplit.dividerLocation", centerSplit.getDividerLocation())));
        outerSplit.setDividerLocation(Math.max(0, prefs.getInt(prefix+"outerSplit.dividerLocation", outerSplit.getDividerLocation())));
        innerSplit.setDividerLocation(Math.max(0, prefs.getInt(prefix+"innerSplit.dividerLocation", innerSplit.getDividerLocation())));
        Dimension preferredSize = new Dimension(getPreferredSize());
        preferredSize.width = prefs.getInt(prefix+"width", preferredSize.width);
        preferredSize.height = prefs.getInt(prefix+"height", preferredSize.height);
        setPreferredSize(preferredSize);

        
        
    }
    
    private void savePreferences() {
        shouldSavePreferences = false;
        Preferences prefs = prefs();
        String prefix = getPreferencesPrefix();
        prefs.putInt(prefix+"innerSplit.dividerLocation", innerSplit.getDividerLocation());
        prefs.putInt(prefix+"outerSplit.dividerLocation", outerSplit.getDividerLocation());
        prefs.putInt(prefix+"centerSplit.dividerLocation", centerSplit.getDividerLocation());

        if (getWidth() > getMinimumSize().width) {
            prefs.putInt(prefix+"width", getWidth());

        }
        if (getHeight() > getMinimumSize().height) {
            prefs.putInt(prefix + "height", getHeight());
        }

        for (AppPanel panel : appPanels.values()) {
            panel.savePreferences(this, prefs);
        }
        
        try {
            prefs.flush();
        } catch (BackingStoreException ex) {
            Logger.getLogger(AppFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
   
    
    
    void initPanel(JComponent panel) {

        if (panel instanceof JTabbedPane) {
            JTabbedPane jTabbedPane = (JTabbedPane) panel;

        } else {
            panel.setLayout(new BorderLayout());
        }
    }
    
    private void initPanels(JComponent... panels) {
        for (JComponent panel : panels) {
            initPanel(panel);
        }
    }

    private boolean inAppFrameUI = false;
    protected void updateAppFrameUI() {
        if (inAppFrameUI) {
            return;
        }
        inAppFrameUI = true;
        try {

        } finally {
            inAppFrameUI = false;
        }
    }

    private boolean isDividerChange(AppEvent event) {
        return (event.getSourceEvent() instanceof PropertyChangeEvent &&
            ((PropertyChangeEvent) event.getSourceEvent()).getPropertyName().equals(JSplitPane.DIVIDER_LOCATION_PROPERTY)
        );
    }


    private boolean isResizeEvent(AppEvent event) {
        return (event.getSourceEvent() instanceof ComponentEvent &&
                ((ComponentEvent)event.getSourceEvent()).getID() == ComponentEvent.COMPONENT_RESIZED);
    }


    protected void respondAppFrameUI(AppEvent event) {

        if (isDividerChange(event)) {
            shouldSavePreferences = true;
        } else if (isResizeEvent(event)) {
            shouldSavePreferences = true;
        }

        updateAppFrameUI();

    }
    
   
    public static enum FrameLocation {
        LeftPanel,
        RightPanel,
        CenterPanel,
        BottomPanel,
        SeparateWindow
    }
    
    public static enum PanelName {
        Canvas,
        ComponentInspector,
        ComponentDetailsEditor,
    }

    
    public void add(AppPanel comp) {
        comp.applyPreferences(this, prefs());
        add(comp, comp.getPreferredFrame());
    }

    private JTabbedPane getPanel(FrameLocation location) {
        switch (location) {
            case LeftPanel:
                return leftPanel;
            case RightPanel:
                return rightPanel;
            case BottomPanel:
                return bottomPanel;
            case CenterPanel:
                return centerPanel;
            case SeparateWindow:
                return null;
        }
        throw new IllegalArgumentException("Unsupported location in getPanel() "+location);
    }



    
    public void add(AppPanel comp, FrameLocation location) {
        if (windows.containsKey(comp)) {
            throw new IllegalStateException("Panel is already added to the app frame in its own window.");
        }
        if (getPanelLocation(comp) != null) {
            throw new IllegalStateException("Panel is already added to this app frame.");
        }
        Container parent = comp.getParent();
        if (parent != null) {
            parent.remove(comp);
        }

        appPanels.put(comp.getId(), comp);
        comp.setAppFrame(this);
        if (location != FrameLocation.SeparateWindow) {
            JTabbedPane pane = getPanel(location);
            pane.addTab(comp.getLabel(), comp);
        } else {
            AppPanelWindowHandle windowHandle = new AppPanelWindowHandle(comp, null);
            windows.put(comp, windowHandle);
            windowHandle.show();
        }

        comp.savePreferences(this, prefs());
        setShouldSavePreferences();
    }

    public AppPanel getAppPanelById(String id) {
        return appPanels.get(id);
    }


    private JTabbedPane getParentTabbedPane(java.awt.Component cmp) {
        java.awt.Component parent = cmp.getParent();
        if (parent == null) return null;
        if (parent instanceof JTabbedPane) {
            return (JTabbedPane)parent;
        } else {
            return getParentTabbedPane(parent);
        }
    }

    public AppFrame.FrameLocation getPanelLocation(AppPanel panel) {
        if (windows.containsKey(panel)) {
            return FrameLocation.SeparateWindow;
        }
        java.awt.Container parent = getParentTabbedPane(panel);
        if (parent == bottomPanel) {
            return FrameLocation.BottomPanel;
        } else if (parent == leftPanel) {
            return FrameLocation.LeftPanel;
        } else if (parent == rightPanel) {
            return FrameLocation.RightPanel;
        } else if (parent == centerPanel) {
            return FrameLocation.CenterPanel;
        }
        return null;
    }


    public void removePanel(AppPanel panel) {
        FrameLocation oldLocation = getPanelLocation(panel);
        if (oldLocation == FrameLocation.SeparateWindow) {
            AppPanelWindowHandle handle = windows.get(panel);
            if (handle == null) {
                throw new IllegalStateException("Attempt to remove window, but it isn't registered.");
            }
            windows.remove(panel);
            handle.dispose();
        } else {
            JTabbedPane tabbedPane = getParentTabbedPane(panel);
            tabbedPane.remove(panel);
        }
        appPanels.remove(panel.getId());
    }
    
    public void moveTo(AppPanel panel, FrameLocation location) {
        FrameLocation oldLocation = getPanelLocation(panel);
        if (oldLocation == location) return;

        if (oldLocation == FrameLocation.SeparateWindow) {
            AppPanelWindowHandle handle = windows.get(panel);
            if (handle == null) {
                throw new IllegalStateException("Panel window not registered");
            }
            windows.remove(panel);
            Container parent = panel.getParent();
            if (parent != null) {
                parent.remove(panel);
            }
            handle.dispose();
        } else {

            JTabbedPane tabbedPane = getPanel(oldLocation);
            tabbedPane.remove(panel);
        }

        add(panel, location);
        if (location == FrameLocation.SeparateWindow) {
            AppPanelWindowHandle handle = windows.get(panel);
            if (handle == null) {
                throw new IllegalStateException("Cannot find window handle after adding to separate window");
            }
            handle.previousLocation = oldLocation;
        }
    }

    private Timer timer;
    private TimerTask timerTask;

    private class FrameListener implements PropertyChangeListener, ComponentListener, WindowListener  {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (!initialized) return;
            if (JSplitPane.DIVIDER_LOCATION_PROPERTY.equals(evt.getPropertyName())) {
                respondAppFrameUI(new AppEvent(evt));
            }
        }

        @Override
        public void componentResized(ComponentEvent e) {
            if (!initialized) return;
            respondAppFrameUI(new AppEvent(e));
        }

        @Override
        public void componentMoved(ComponentEvent e) {

        }

        @Override
        public void componentShown(ComponentEvent e) {

        }

        @Override
        public void componentHidden(ComponentEvent e) {

        }

        @Override
        public void windowOpened(WindowEvent e) {
            setShouldSavePreferences();
        }

        @Override
        public void windowClosing(WindowEvent e) {

        }

        @Override
        public void windowClosed(WindowEvent e) {
            setShouldSavePreferences();
        }

        @Override
        public void windowIconified(WindowEvent e) {

        }

        @Override
        public void windowDeiconified(WindowEvent e) {

        }

        @Override
        public void windowActivated(WindowEvent e) {

        }

        @Override
        public void windowDeactivated(WindowEvent e) {

        }
    }

    private FrameListener frameListener = new FrameListener();



    @Override
    public void addNotify() {

        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                if (shouldSavePreferences) {
                    savePreferences();
                }
            }

        };
        timer.schedule(timerTask, PREFS_UPDATE_DELAY, PREFS_UPDATE_PERIOD);
        super.addNotify();
    }

    @Override
    public void removeNotify() {
        if (timer != null) {
            timer.cancel();
            timerTask.cancel();
            timer = null;
            timerTask = null;
        }
        super.removeNotify();

    }

    void setShouldSavePreferences() {
        shouldSavePreferences = true;
    }

    private void setDividerLocationIfChanged(JSplitPane pane, int location) {
        if (pane.getDividerLocation() != location) {
            pane.setDividerLocation(location);
        }
    }

    JFrame getWindow(AppPanel panel) {
        AppPanelWindowHandle handle = windows.get(panel);
        if (handle == null) {
            return null;
        }
        return handle.window;
    }
}
