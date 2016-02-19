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
package com.codename1.components;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.Slider;
import com.codename1.ui.TextArea;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * An API to present status messages to the user in an unubtrusive manner.  This is useful if
 * there are background tasks that need to display information to the user.  E.g. If a network request fails,
 * of let the user know that "Jobs are being synchronized".
 * 
 * <h2>Example Usage</h2>
 * 
 * <script src="https://gist.github.com/shannah/76ac6e3e136fb19124f2.js"></script>
 * 
 * <h3>Advanced Usage</h3>
 * <p>See the <a href="https://github.com/codenameone/codenameone-demos/blob/master/StatusBarDemo/src/com/codename1/demos/status/StatusBarDemo.java">StatusBarDemo</p>
 * 
 * <h2>Screenshots</h2>
 * 
 * <h3>Status With Progress Bar</h3>
 * <p><img src="https://cloud.githubusercontent.com/assets/2677562/13191694/5db26e12-d71a-11e5-8b21-3058e240910d.png"/></p>
 * 
 * <h3>Status With Multi-Line Message</h3>
 * <p><img src="https://cloud.githubusercontent.com/assets/2677562/13191729/9108cf22-d71a-11e5-86d6-d5c752826596.png"/></p>
 * 
 * <h2>Video Demos</h2>
 * <p><a href="https://youtu.be/SMhqQ1xvfz0">30 Second Screencast Demo of Status Bar Component</a></p>
 * 
 * @author shannah
 */
public class StatusBar {
   
    /**
     * The default UIID that to be used for the status bar component.  This is the 
     * style of the box that appears at the bottom of the screen.
     */
    private String defaultUIID="StatusBarComponent";
    
    /**
     * The default UIID that is to be used for the text in the status bar.
     */
    private String defaultMessageUIID="StatusBarMessage";
    
    
    
    //FIXME SH Need to style the status bar so that it looks nicer
    
    private static StatusBar instance;
    
    /**
     * Gets reference to the singleton StatusBar instance
     * @return 
     */
    public static StatusBar getInstance() {
        if (instance == null) {
            instance = new StatusBar();
            
        }
        return instance;
    }
    
    private StatusBar(){
        
    }
    
    
    
    /**
     * Keeps track of the currently active status messages.
     */
    private final ArrayList<Status> statuses = new ArrayList<Status>();

    /**
     * Gets the default UIID to be used for the style of the Status Bar component.
     * By default this is "StatusBarComponent".
     * @return the defaultUIID
     */
    public String getDefaultUIID() {
        return defaultUIID;
    }

    /**
     * Sets the defaults UIID to be used for the style of the Status Bar component.  By default
     * this is "StatusBarComponent"
     * @param defaultUIID the defaultUIID to set
     */
    public void setDefaultUIID(String defaultUIID) {
        this.defaultUIID = defaultUIID;
    }

    /**
     * Gets the default UIID to be used for the style of the Status Bar text.  By default
     * this is "StatusBarMessage"
     * @return the defaultMessageUIID
     */
    public String getDefaultMessageUIID() {
        return defaultMessageUIID;
    }

    /**
     * Sets the default UIID to be used for the style of the Status Bar text.  By default this is
     * "StatusBarMessage"
     * @param defaultMessageUIID the defaultMessageUIID to set
     */
    public void setDefaultMessageUIID(String defaultMessageUIID) {
        this.defaultMessageUIID = defaultMessageUIID;
    }

    
    
    /**
     * Represents a single status message.
     */
    public class Status {
        
        /**
         * This UIID that should be used to style the StatusBar text while this
         * message is being displayed.
         */
        private String messageUIID=defaultMessageUIID;
        
        /**
         * The UIID that should be used to style the StatusBar component while 
         * this message is being displayed.
         */
        private String uiid=defaultUIID;
        
        
        
        
        /**
         * The start time of the process this status is tracking.
         */
        private final long startTime;
        
        /**
         * Timer used to "expire" the message after a certain time.
         * @see #setExpires(int) 
         */
        private Timer timer;
        /**
         * Timer used to delay the showing of the message.  Useful if you only want
         * to show the message if the task ends up taking a long time.
         * @see #showDelayed(int) 
         */
        private Timer showTimer;
        
        /**
         * The message to be displayed in the status bar.
         */
        private String message;
        
        /**
         * Optional progress for the task.  (Not tested or implemented yet).
         */
        private int progress=-1;
        
        /**
         * Optional icon to show in the status bar.  (Not tested or implemented yet).
         */
        private Image icon;
        
        /**
         * Whether this status message should show an infinite progress indicator. (e.g. spinning beachball).
         */
        private boolean showProgressIndicator;
        
        private Status() {
            startTime = System.currentTimeMillis();
        }
        
        /**
         * Directs the status to be cleared (if it isn't already cleared() after a given number of milliseconds.
         * @param millis The maximum number of milliseconds that the status message should be displayed for.
         * Helpful for error messages that only need to be displayed for a few seconds.
         */
        public void setExpires(int millis) {
            if (millis < 0 && timer != null) {
                timer.cancel();
                timer = null;
            } else if (millis > 0) {
                if (timer != null) {
                    timer.cancel();
                    timer = null;
                }
                timer = new Timer();
                timer.schedule(new TimerTask() {

                    @Override
                    public void run() {
                        Display.getInstance().callSerially(new Runnable() {
                            public void run() {
                                timer = null;
                                Status.this.clear();
                            }
                        });
                    }
                    
                }, millis);
            }
        }
        
        /**
         * Sets the message that should be displayed in the status bar.
         * @param message 
         */
        public void setMessage(String message) {
            this.message = message;
            
        }
        
        /**
         * Sets the progress (1..100) that should be displayed in the progress bar
         * for this status.  (Not tested or used yet).
         * @param progress 
         */
        public void setProgress(int progress) {
            this.progress = progress;
        }
        
        /**
         * Shows this status message.  Call this method after making any changes
         * to the status that you want to have displayed.  This will always cause
         * any currently-displayed status to be replaced by this status.
         * <p>If you don't want to show the status immediately, but rather to wait some delay, you can use
         * the {@link #showDelayed(int) } method instead.</p>
         * 
         * @see #showDelayed(int) 
         */
        public void show() {
            if (showTimer != null) {
                showTimer.cancel();
                showTimer = null;
            }
            StatusBarComponent c = getStatusBarComponent();
            if (c != null) {
                c.currentlyShowing = this;
                updateStatus();
                setVisible(true);
                
            }
        }
        
        /**
         * Schedules this status message to be shown after a specified number of milliseconds,
         * if it hasn't been cleared or shown first.  
         * <p>This is handy if you want to show an status for an operation that usually completes very quickly, but could 
         * potentially hang.  In such a case you might decide not to display a status message at all unless the operation
         * takes more than 500ms to complete.</p>
         * 
         * <p>If you want to show the status immediately, use the {@link #show() } method instead.</p>
         * @param millis Number of milliseconds to wait before showing the status.
         */
        public void showDelayed(int millis) {
            showTimer = new Timer();
            showTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            if (showTimer != null) {
                                showTimer = null;
                                show();
                            }
                        }
                    });
                }
                
            }, millis);       
        }
        
        /**
         * Clears this status message. This any pending "showDelayed" requests for this status.
         */
        public void clear() {
            if (showTimer != null) {
                showTimer.cancel();
                showTimer = null;
            }
            if (timer != null) {
                timer.cancel();
                timer = null;
            }
            removeStatus(this);
        }

        /**
         * Returns the text that will be displayed for this status.
         * @return the message
         */
        public String getMessage() {
            return message;
        }

        /**
         * Returns the progress of this status.  A value of -1 indicates that the progress
         * bar should not be shown.  Values between 0 and 100 inclusive will be rendered
         * on a progress bar (slider) in the status component.
         * @return the progress
         */
        public int getProgress() {
            return progress;
        }

        /**
         * Gets the icon (may be null) that is displayed with this status.
         * @return the icon
         */
        public Image getIcon() {
            return icon;
        }

        /**
         * Sets the icon that is to be displayed with this status.  Set this to null to not show an icon.
         * @param icon the icon to set
         */
        public void setIcon(Image icon) {
            this.icon = icon;
        }

        /**
         * @return the showProgressIndicator
         */
        public boolean isShowProgressIndicator() {
            return showProgressIndicator;
        }

        /**
         * Sets whether this status message should include an infinite progress indicator (e.g. spinning beach ball).
         * @param showProgressIndicator the showProgressIndicator to set
         */
        public void setShowProgressIndicator(boolean showProgressIndicator) {
            this.showProgressIndicator = showProgressIndicator;
        }

        /**
         * Gets the UIID to use for styling the text of this status message.
         * @return the messageUIID
         */
        public String getMessageUIID() {
            return messageUIID;
        }

        /**
         * Sets the UIID to use for styling the text of this status message.
         * @param messageUIID the messageUIID to set
         */
        public void setMessageUIID(String messageUIID) {
            this.messageUIID = messageUIID;
        }

        /**
         * Gets the UIID that should be used for styling the status component while
         * this status is displayed.
         * @return the uiid
         */
        public String getUiid() {
            return uiid;
        }

        /**
         * Sets the UIID that should be used for styling the status component while 
         * this status is displayed.
         * @param uiid the uiid to set
         */
        public void setUiid(String uiid) {
            this.uiid = uiid;
        }
        
    }
    
    /**
     * Flag to indicate that the status is updating.  This is used to prevent 
     * two status updates from happening at the same time.  
     */
    private boolean updatingStatus;
    
    /**
     * Flag to indicate that a request to update the status was received while
     * updateStatus() was running.
     */
    private boolean pendingUpdateStatus;
    
    
    /**
     * Updates the StatusBar UI component with the settings of the current status.
     */
    private void updateStatus() {
        StatusBarComponent c = getStatusBarComponent();
        if (c != null) {
            if (updatingStatus) {
                pendingUpdateStatus = true;
                return;
            }
            try {
                updatingStatus = true;
                if (c.currentlyShowing != null && !statuses.contains(c.currentlyShowing)) {
                    c.currentlyShowing = null;
                }
                if (c.currentlyShowing == null || statuses.isEmpty()) {
                    if (!statuses.isEmpty()) {
                        c.currentlyShowing = statuses.get(statuses.size()-1);
                    } else {
                        setVisible(false);
                        return;
                    }

                }
                Status s = c.currentlyShowing;

                Label l = new Label(s.getMessage() != null ? s.getMessage() : "");

                c.progressLabel.setVisible(s.isShowProgressIndicator());
                if (c.progressLabel.isVisible()) {
                    if (!c.contains(c.progressLabel)) {
                        c.addComponent(BorderLayout.EAST, c.progressLabel);
                    }
                    Image anim = c.progressLabel.getAnimation();
                    if (anim != null && anim.getWidth() > 0) {
                        c.progressLabel.setWidth(anim.getWidth());
                    }
                    if (anim != null && anim.getHeight() > 0) {
                        c.progressLabel.setHeight(anim.getHeight());
                    }
                } else {
                    if (c.contains(c.progressLabel)) {
                        c.removeComponent(c.progressLabel);
                    }
                }
                c.progressBar.setVisible(s.getProgress() >= 0);
                if (s.getProgress() >= 0) {
                    if (!c.contains(c.progressBar)) {
                        c.addComponent(BorderLayout.SOUTH, c.progressBar);
                    }
                    c.progressBar.setProgress(s.getProgress());
                } else {
                    c.removeComponent(c.progressBar);
                }
                c.icon.setVisible(s.getIcon() != null);
                if (s.getIcon() != null && c.icon.getIcon() != s.getIcon()) {
                    c.icon.setIcon(s.getIcon());
                }
                if (s.getIcon() == null && c.contains(c.icon)) {
                    c.removeComponent(c.icon);
                } else if (s.getIcon() != null && !c.contains(c.icon)){
                    
                    c.addComponent(BorderLayout.WEST, c.icon);
                }
                String oldText = c.label.getText();
               
                if (!oldText.equals(l.getText())) {

                    
                    if (s.getUiid() != null) {
                        c.setUIID(s.getUiid());
                    } else if (defaultUIID != null) {
                        c.setUIID(defaultUIID);
                    }
                    
                    if (c.isVisible()) {
                        TextArea newLabel = new TextArea();
                        //newLabel.setColumns(l.getText().length()+1);
                        //newLabel.setRows(l.getText().length()+1);
                        newLabel.setFocusable(false);
                        newLabel.setEditable(false);
                        
                        //newLabel.getAllStyles().setFgColor(0xffffff);
                        if (s.getMessageUIID() != null) {
                            newLabel.setUIID(s.getMessageUIID());
                        } else if (defaultMessageUIID != null) {
                            newLabel.setUIID(defaultMessageUIID);
                        } else {
                            newLabel.setUIID(c.label.getUIID());
                        }
                        if (s.getUiid() != null) {
                            c.setUIID(s.getUiid());
                        } else if (defaultUIID != null) {
                            c.setUIID(defaultUIID);
                        }
                        newLabel.setWidth(c.label.getWidth());
                        
                        newLabel.setText(l.getText());
                        
                        Dimension oldTextAreaSize = UIManager.getInstance().getLookAndFeel().getTextAreaSize(c.label, true);
                        Dimension newTexAreaSize = UIManager.getInstance().getLookAndFeel().getTextAreaSize(newLabel, true);
                        
                        c.label.getParent().replaceAndWait(c.label, newLabel, CommonTransitions.createCover(CommonTransitions.SLIDE_VERTICAL, true, 300));
                        c.label = newLabel;
                        
                        if (oldTextAreaSize.getHeight() != newTexAreaSize.getHeight()) {
                            
                            c.label.setPreferredH(newTexAreaSize.getHeight());
                            c.getParent().animateHierarchyAndWait(300);
                        }
                        
                        
                    } else {
                        if (s.getMessageUIID() != null) {
                            c.label.setUIID(s.getMessageUIID());
                        } else if (defaultMessageUIID != null) {
                            c.label.setUIID(defaultMessageUIID);
                        }
                        if (s.getUiid() != null) {
                            c.setUIID(s.getUiid());
                        } else if (defaultUIID != null) {
                            c.setUIID(defaultUIID);
                        }
                        c.label.setText(l.getText());
                        //c.label.setColumns(l.getText().length()+1);
                        //c.label.setRows(l.getText().length()+1);
                        c.label.setPreferredW(c.getWidth());
                        c.revalidate();
                    }
                } else {
                    c.revalidate();
                }
            } finally {
                updatingStatus = false;
                if (pendingUpdateStatus) {
                    pendingUpdateStatus = false;
                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            updateStatus();
                        }
                    });   
                }
            }
        }
    }
    
    /**
     * The actual component for the status bar.  This is added to the layered pane of
     * the top-level form.
     */
    private class StatusBarComponent extends Container {
        private TextArea label;
        private InfiniteProgress progressLabel;
        private Slider progressBar;
        private Label icon;
        private Status currentlyShowing;
        boolean hidden = true;
        Button leadButton = new Button();
        
        public StatusBarComponent() {
            this.getAllStyles().setBgColor(0x0);
            this.getAllStyles().setBackgroundType(Style.BACKGROUND_NONE);
            this.getAllStyles().setBgTransparency(128);
            setVisible(false);
            label = new TextArea();
            label.setEditable(false);
            label.setFocusable(false);
            
            
            progressLabel = new InfiniteProgress();
            
            progressLabel.setAngleIncrease(4);
            progressLabel.setVisible(false);
            icon = new Label();
            icon.setVisible(false);
            progressBar = new Slider();
            progressBar.setVisible(false);
            
            leadButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    if (currentlyShowing != null && !currentlyShowing.showProgressIndicator ) {
                        currentlyShowing.clear();
                    }
                    StatusBar.this.setVisible(false);
                }
            });
            leadButton.setVisible(false);
            
            this.setLeadComponent(leadButton);
            
            setLayout(new BorderLayout());
            addComponent(BorderLayout.WEST, icon);
            addComponent(BorderLayout.CENTER, label);
            addComponent(BorderLayout.SOUTH, progressBar);
            addComponent(BorderLayout.EAST, progressLabel);
            
            progressBar.setVisible(false);
        }

        @Override
        protected Dimension calcPreferredSize() {
            if (hidden) {
                return new Dimension(Display.getInstance().getDisplayWidth(),
                    0
                );
            } else {
                return super.calcPreferredSize();
                /*
                return new Dimension(Display.getInstance().getDisplayWidth(),
                        Display.getInstance().convertToPixels(10, false)
                );*/
            }
        } 
    }
    
    /**
     * Creates a new Status.
     * @return 
     */
    public Status createStatus() {
        Status s = new Status();
        statuses.add(s);
        return s;
    }
    
    private void removeStatus(Status status) {
        if (status.timer != null) {
            status.timer.cancel();
            status.timer = null;
        }
        statuses.remove(status);
        updateStatus();
    }
    
    private StatusBarComponent getStatusBarComponent() {
        Form f = Display.getInstance().getCurrent();
        if (f != null && !(f instanceof Dialog)) {
            StatusBarComponent c = (StatusBarComponent)f.getClientProperty("StatusBarComponent");
            if (c == null) {
                c = new StatusBarComponent();
                c.hidden = true;
                f.putClientProperty("StatusBarComponent", c);
                Container layered = f.getLayeredPane(this.getClass(), true);
                layered.setLayout(new BorderLayout());
                layered.addComponent(BorderLayout.SOUTH, c);
                updateStatus();
            }
            return c;
        }
        return null;
    }
    
    /**
     * Shows or hides the status bar.
     * @param visible 
     */
    public void setVisible(boolean visible) {
        StatusBarComponent c = getStatusBarComponent();
        if (c == null || c.isVisible() == visible) {
            return;
        }
        if (visible) {
            c.setVisible(true);
            c.label.setPreferredH(UIManager.getInstance().getLookAndFeel().getTextAreaSize(c.label, true).getHeight());
            Container layered = c.getParent();
            c.hidden = true;
            layered.revalidate();
            c.hidden = false;
            layered.animateHierarchyAndWait(1000);
            updateStatus();
        } else {
            Form f = c.getComponentForm();
            Container layered = c.getParent();
            c.hidden = true;
            layered.animateHierarchyAndWait(1000);
            c.setVisible(false); 
        }
    }
}
