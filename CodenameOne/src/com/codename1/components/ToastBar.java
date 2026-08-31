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

import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkEvent;
import com.codename1.io.NetworkManager;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.CN;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.TopLevelContainer;
import com.codename1.ui.Window;
import com.codename1.ui.Slider;
import com.codename1.ui.TextArea;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.util.FailureCallback;
import com.codename1.util.SuccessCallback;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static com.codename1.ui.ComponentSelector.$;

/// An API to present status messages to the user in an unobtrusive manner.  This is useful if
/// there are background tasks that need to display information to the user.  E.g. If a network request fails,
/// of let the user know that "Jobs are being synchronized".
///
/// Example Usage
///
/// ```java
/// Form hi = new Form("ToastBarDemo", BoxLayout.y());
///
/// Button basic = new Button("Basic");
/// Button progress = new Button("Progress");
/// Button expires = new Button("Expires");
/// Button delayed = new Button("Delayed");
/// hi.add(basic).add(progress).add(expires).add(delayed);
///
/// basic.addActionListener(e -> {
///   ToastBar.Status status = ToastBar.getInstance().createStatus();
///   status.setMessage("Hello world");
///   status.show();
///   //...  Some time later you must clear the status
///   // status.clear();
/// });
///
/// progress.addActionListener(e -> {
///   ToastBar.Status status = ToastBar.getInstance().createStatus();
///   status.setMessage("Hello world");
///   status.setShowProgressIndicator(true);
///   status.show();
///   // ... Some time later you must clear it
/// });
///
/// expires.addActionListener(e -> {
///   ToastBar.Status status = ToastBar.getInstance().createStatus();
///   status.setMessage("Hello world");
///   status.setExpires(3000);  // only show the status for 3 seconds, then have it automatically clear
///   status.show();
/// });
///
/// delayed.addActionListener(e -> {
///   ToastBar.Status status = ToastBar.getInstance().createStatus();
///   status.setMessage("Hello world");
///   status.showDelayed(300); // Wait 300 ms to show the status
///   // ... Some time later, clear the status... this may be before it shows at all
/// });
///
/// hi.show();
/// ```
///
/// Advanced Usage
///
/// See the [StatusBarDemo](https://github.com/codenameone/codenameone-demos/blob/master/ToastBarDemo/src/com/codename1/demos/status/ToastBarDemo.java)
///
/// Screenshots
///
/// Status With Progress Bar
///
/// Status With Multi-Line Message
///
/// Video Demo
///
/// Note: the video above refers to the `ToastBar` based on its development name of StatusBar. This
/// was changed to avoid confusion with the iOS StatusBar.
///
/// @author shannah
public final class ToastBar {

    /// The default timeout for info/error messages
    private static int defaultMessageTimeout = 4000;
    /// Keeps track of the currently active status messages.
    private final ArrayList<Status> statuses = new ArrayList<Status>();
    private int position = Component.BOTTOM;
    /// The default UIID that to be used for the `ToastBar` component.  This is the
    /// style of the box that appears at the bottom of the screen.
    private String defaultUIID = "ToastBar";
    /// The default UIID that is to be used for the text in the `ToastBar`.
    private String defaultMessageUIID = "ToastBarMessage";
    //FIXME SH Need to style the {@code ToastBar} so that it looks nicer
    private boolean useFormLayeredPane;

    /// Which of the inheritable settings this instance has been given in its own right.
    ///
    /// A window's toast bar takes the singleton's configuration so the static helpers
    /// keep honouring settings made once at start-up, but it must not be frozen at the
    /// moment it happened to be created -- nor have a later change to the shared
    /// defaults overwrite something set on this window deliberately.
    private boolean positionExplicit;
    private boolean useFormLayeredPaneExplicit;
    private boolean defaultUIIDExplicit;
    private boolean defaultMessageUIIDExplicit;
    /// Flag to indicate that the status is updating.  This is used to prevent
    /// two status updates from happening at the same time.
    private boolean updatingStatus;
    /// Flag to indicate that a request to update the status was received while
    /// updateStatus() was running.
    private boolean pendingUpdateStatus;

    private ToastBar() {

    }

    /// The default timeout for info/error messages
    ///
    /// #### Returns
    ///
    /// the defaultMessageTimeout
    public static int getDefaultMessageTimeout() {
        return defaultMessageTimeout;
    }

    /// The default timeout for info/error messages
    ///
    /// #### Parameters
    ///
    /// - `aDefaultMessageTimeout`: the defaultMessageTimeout to set
    public static void setDefaultMessageTimeout(int aDefaultMessageTimeout) {
        defaultMessageTimeout = aDefaultMessageTimeout;
    }

    /// Gets reference to the singleton StatusBar instance
    ///
    /// The singleton shows on whichever `com.codename1.ui.Form` is current, including
    /// while a desktop window has the focus. To toast on a window use
    /// `#getInstance(com.codename1.ui.TopLevelContainer)`, which is what the static
    /// helpers on this class do.
    public static ToastBar getInstance() {
        return ToastBarHolder.INSTANCE;
    }

    /// The toast bar for a given top level.
    ///
    /// The singleton follows whichever `Form` is current, which is the behaviour every
    /// existing application relies on, so a `Form` or null still gets it. A
    /// `com.codename1.ui.Window` gets its own instance instead, cached on the window
    /// and dying with it -- a settable host on the singleton would have meant one
    /// window silently redirecting another surface's toasts.
    ///
    /// #### Parameters
    ///
    /// - `top`: the top level to show on, may be null
    ///
    /// #### Returns
    ///
    /// the toast bar for that top level, never null
    public static ToastBar getInstance(TopLevelContainer top) {
        if (!(top instanceof Window)) {
            return ToastBarHolder.INSTANCE;
        }
        Container c = top.asContainer();
        ToastBar b = (ToastBar) c.getClientProperty(WINDOW_INSTANCE_PROP);
        if (b == null) {
            b = new ToastBar();
            b.host = top;
            c.putClientProperty(WINDOW_INSTANCE_PROP, b);
        }
        // On every lookup, not only when one is created. An application sets the
        // position, the layer choice and the UIIDs on the singleton and then calls the
        // static helpers; those helpers reach this instance whenever a window has the
        // focus. Seeding once froze the window's bar at whatever the defaults happened
        // to be the first time it was asked for, so every configuration change after
        // that point was silently dropped -- which the static helpers have always
        // honoured.
        b.inheritDefaultsFrom(ToastBarHolder.INSTANCE);
        return b;
    }

    /// The client property a window's own toast bar is cached under.
    private static final String WINDOW_INSTANCE_PROP = "cn1$ToastBar";

    /// The top level this instance shows on, or null for the singleton, which follows
    /// whichever form is current.
    private TopLevelContainer host;

    /// The top level to show on.
    ///
    /// #### Returns
    ///
    /// the host, or null when there is none
    private TopLevelContainer resolveHost() {
        if (host != null) {
            return host;
        }
        // The current form, not the top level the user is in. The singleton is
        // form-only by contract, and it has to stay that way: a window already has its
        // own instance from getInstance(TopLevelContainer), and letting the singleton
        // resolve to that window as well would give two instances with two status
        // lists the same cached component to fight over -- one expiring a toast the
        // other still thinks it is showing. Callers that want the surface the user is
        // in ask for it, which is what the static helpers below do.
        return Display.getInstance().getCurrent();
    }

    /// Simplifies a common use case of showing an error message with an error icon that fades out after a few seconds
    ///
    /// #### Parameters
    ///
    /// - `msg`: the error message
    public static void showErrorMessage(String msg) {
        showErrorMessage(msg, defaultMessageTimeout);
    }

    /// Simplifies a common use case of showing a message with an icon that fades out after a few seconds
    ///
    /// #### Parameters
    ///
    /// - `msg`: the message
    ///
    /// - `icon`: the material icon to show from `com.codename1.ui.FontImage`
    ///
    /// - `timeout`: the timeout value in milliseconds
    ///
    /// - `listener`: the action to perform when the ToastBar is tapped
    ///
    /// #### Returns
    ///
    /// the status if we want to clear it before timeout elapses
    public static Status showMessage(String msg, char icon, int timeout, ActionListener listener) {
        Status s = ToastBar.getInstance(CN.getCurrentTopLevel()).createStatus();
        Style stl = UIManager.getInstance().getComponentStyle(s.getMessageUIID());
        s.setIcon(FontImage.createMaterial(icon, stl, 4));
        s.setMessage(msg);
        if (listener != null) {
            s.setListener(listener);
        }
        s.setExpires(timeout);
        s.show();
        return s;
    }

    /// Simplifies a common use case of showing a message with an icon that fades out after a few seconds
    ///
    /// #### Parameters
    ///
    /// - `msg`: the message
    ///
    /// - `icon`: the material icon to show from `com.codename1.ui.FontImage`
    ///
    /// - `timeout`: the timeout value in milliseconds
    ///
    /// #### Returns
    ///
    /// the status if we want to clear it before timeout elapses
    public static Status showMessage(String msg, char icon, int timeout) {
        return showMessage(msg, icon, timeout, null);
    }

    /// Simplifies a common use case of showing an error message with an error icon that fades out after a few seconds
    ///
    /// #### Parameters
    ///
    /// - `msg`: the message
    ///
    /// - `icon`: the material icon to show from `com.codename1.ui.FontImage`
    ///
    /// - `listener`: the action to perform when the ToastBar is tapped
    ///
    /// #### Returns
    ///
    /// the status if we want to clear it before timeout elapses
    public static Status showMessage(String msg, char icon, ActionListener listener) {
        return showMessage(msg, icon, defaultMessageTimeout, listener);
    }

    /// Simplifies a common use case of showing an error message with an error icon that fades out after a few seconds
    ///
    /// #### Parameters
    ///
    /// - `icon`: the material icon to show from `com.codename1.ui.FontImage`
    ///
    /// - `msg`: the message
    ///
    /// #### Returns
    ///
    /// the status if we want to clear it before timeout elapses
    public static Status showMessage(String msg, char icon) {
        return showMessage(msg, icon, defaultMessageTimeout);
    }

    /// Simplifies a common use case of showing an information message with an info icon that fades out after a few seconds
    ///
    /// #### Parameters
    ///
    /// - `msg`: the message
    ///
    /// #### Returns
    ///
    /// the status if we want to clear it before timeout elapses
    public static Status showInfoMessage(String msg) {
        return showMessage(msg, FontImage.MATERIAL_INFO, defaultMessageTimeout);
    }

    /// Simplifies a common use case of showing an error message with an error icon that fades out after a few seconds
    ///
    /// #### Parameters
    ///
    /// - `msg`: the error message
    ///
    /// - `timeout`: the timeout value in milliseconds
    ///
    /// #### Returns
    ///
    /// the status if we want to clear it before timeout elapses
    public static Status showErrorMessage(String msg, int timeout) {
        return showMessage(msg, FontImage.MATERIAL_ERROR, timeout);
    }

    /*
     * Shows a progress indicator based on connection request, this is incomplete but it meant to serve as
     * a replacement for the inifinte progress
     *
     * @param message a message to show on the progress indicator
     * @param cr the connection request whose progress should be shown
     * @param onSuccess invoked when the connection request completes, can be null
     * @param onError invoked on case of an error, can be null
     */
    public static void showConnectionProgress(String message, final ConnectionRequest cr,
                                              final SuccessCallback<NetworkEvent> onSuccess, final FailureCallback<NetworkEvent> onError) {
        final ToastBar.Status s = ToastBar.getInstance(CN.getCurrentTopLevel()).createStatus();
        s.setProgress(-1);
        s.setMessage(message);
        s.show();
        final ActionListener[] progListener = new ActionListener[1];
        final ActionListener<NetworkEvent> errorListener = new ActionListener<NetworkEvent>() {
            @Override
            public void actionPerformed(NetworkEvent evt) {
                s.clear();
                NetworkManager.getInstance().removeErrorListener(this);
                if (progListener[0] != null) {
                    NetworkManager.getInstance().removeProgressListener(progListener[0]);
                }
                if (onError != null) {
                    onError.onError(cr, evt.getError(), evt.getResponseCode(), evt.getMessage());
                }
            }
        };
        NetworkManager.getInstance().addErrorListener(errorListener);
        progListener[0] = new ActionListener<NetworkEvent>() {
            @Override
            public void actionPerformed(NetworkEvent evt) {
                switch (evt.getProgressType()) {
                    case NetworkEvent.PROGRESS_TYPE_INITIALIZING:
                        s.setProgress(-1);
                        break;
                    case NetworkEvent.PROGRESS_TYPE_INPUT:
                    case NetworkEvent.PROGRESS_TYPE_OUTPUT:
                        int currentLength = cr.getContentLength();
                        if (currentLength > 0) {
                            int sentReceived = evt.getSentReceived();
                            float prog = ((float) sentReceived) / ((float) currentLength) * 100f;
                            s.setProgress((int) prog);
                        } else {
                            s.setProgress(-1);
                        }
                        break;
                    default:
                        break;
                }
            }
        };
        cr.addResponseListener(new ActionListener<NetworkEvent>() {
            @Override
            public void actionPerformed(NetworkEvent evt) {
                NetworkManager.getInstance().removeErrorListener(errorListener);
                NetworkManager.getInstance().removeProgressListener(progListener[0]);
                s.clear();
                int rc = cr.getResponseCode();
                if (onSuccess != null && (rc == 200 || rc == 201 || rc == 202)) {
                    onSuccess.onSucess(evt);
                }
            }
        });
        NetworkManager.getInstance().addProgressListener(progListener[0]);
    }

    /// Gets the default UIID to be used for the style of the `ToastBar` component.
    /// By default this is "ToastBarComponent".
    ///
    /// #### Returns
    ///
    /// the defaultUIID
    public String getDefaultUIID() {
        return defaultUIID;
    }

    /// Sets the defaults UIID to be used for the style of the `ToastBar` component.  By default
    /// this is "ToastBarComponent"
    ///
    /// #### Parameters
    ///
    /// - `defaultUIID`: the defaultUIID to set
    public void setDefaultUIID(String defaultUIID) {
        defaultUIIDExplicit = true;
        this.defaultUIID = defaultUIID;
    }

    /// Gets the default UIID to be used for the style of the `ToastBar` text.  By default
    /// this is "ToastBarMessage"
    ///
    /// #### Returns
    ///
    /// the defaultMessageUIID
    public String getDefaultMessageUIID() {
        return defaultMessageUIID;
    }

    /// Sets the default UIID to be used for the style of the `ToastBar` text.  By default this is
    /// "ToastBarMessage"
    ///
    /// #### Parameters
    ///
    /// - `defaultMessageUIID`: the defaultMessageUIID to set
    public void setDefaultMessageUIID(String defaultMessageUIID) {
        defaultMessageUIIDExplicit = true;
        this.defaultMessageUIID = defaultMessageUIID;
    }

    /// By default the ToastBar uses the LayeredPane.  However, it may be better in many
    /// cases to use the FormLayerd pane.  This allows you to toggle whether to use
    /// the FormLayeredPane.
    ///
    /// Key use-case is for displaying the ToastBar over a Sheet, which is on the FormLayeredPane.
    /// If you don't set this to true, then the ToastBar will be displayed behind the Sheet.
    ///
    /// #### Parameters
    ///
    /// - `useFormLayeredPane`: True to use the form layered pane to display the toastbar.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    ///
    public ToastBar useFormLayeredPane(boolean useFormLayeredPane) {
        useFormLayeredPaneExplicit = true;
        applyUseFormLayeredPane(useFormLayeredPane);
        return this;
    }

    /// Moves the bar between the layered panes without recording the choice as this
    /// instance's own, which is what inheriting the shared default must do.
    ///
    /// #### Parameters
    ///
    /// - `layered`: true to use the form layered pane
    private void applyUseFormLayeredPane(boolean layered) {
        if (layered != this.useFormLayeredPane) {
            ToastBarComponent c = getToastBarComponent(false);
            if (c != null) {
                c.remove();
                getLayeredPane().remove();
            }

            this.useFormLayeredPane = layered;

        }
    }

    /// Gets the position of the toast bar on the screen.  Either `Component#TOP` or `Component#BOTTOM`.
    ///
    /// #### Returns
    ///
    /// the position
    public int getPosition() {
        return position;
    }

    /// Sets the position of the toast bar on the screen.
    ///
    /// #### Parameters
    ///
    /// - `position`: the position to set Should be one of `Component#TOP` and `Component#BOTTOM`
    public void setPosition(int position) {
        positionExplicit = true;
        this.position = position;
    }

    /// Puts an existing toast component in the slot the current position asks for.
    ///
    /// #### Parameters
    ///
    /// - `c`: the component already on screen
    private void applyPositionTo(ToastBarComponent c) {
        Container parent = c.getParent();
        if (parent == null || !(parent.getLayout() instanceof BorderLayout)) {
            return;
        }
        String want = position == Component.TOP ? BorderLayout.NORTH : BorderLayout.SOUTH;
        if (want.equals(parent.getLayout().getComponentConstraint(c))) {
            return;
        }
        // The inset written for the edge it is leaving. Only the edge the bar sits at is
        // ever written, so left in place it stayed on the far side while the new edge's
        // inset was added on top of it.
        Style s = c.getAllStyles();
        s.setPaddingUnit(Style.UNIT_TYPE_PIXELS);
        // Back to what the theme asked for, not to nothing -- see safeAreaPadding*Base.
        if (c.safeAreaPaddingTop > 0) {
            s.setPaddingTop(c.safeAreaPaddingTopBase);
            c.safeAreaPaddingTop = 0;
        }
        if (c.safeAreaPaddingBottom > 0) {
            s.setPaddingBottom(c.safeAreaPaddingBottomBase);
            c.safeAreaPaddingBottom = 0;
        }
        // And the gap left for the keyboard, which belongs to the bottom edge just as
        // much as the inset does.
        if (c.keyboardMarginBottom > 0) {
            s.setMarginUnit(Style.UNIT_TYPE_PIXELS);
            s.setMarginBottom(0);
            c.keyboardMarginBottom = 0;
        }
        parent.removeComponent(c);
        parent.addComponent(want, c);
        parent.revalidateLater();
    }

    /// Takes the shared defaults for every setting this instance has not been given
    /// in its own right.
    ///
    /// Explicit beats inherited in both directions and at any time: configuring a
    /// window's bar keeps that setting however the shared default moves afterwards,
    /// and everything else keeps following it.
    ///
    /// #### Parameters
    ///
    /// - `defaults`: the shared instance to inherit from
    private void inheritDefaultsFrom(ToastBar defaults) {
        if (defaults == this) { //NOPMD CompareObjectsWithEquals
            return;
        }
        if (!positionExplicit && position != defaults.position) {
            position = defaults.position;
            // A bar already on screen has to move with it. The slot is otherwise only
            // chosen when the component is built or reattached, so it stayed where it
            // was created while animating as though it had gone to the other end.
            ToastBarComponent shown = getToastBarComponent(false);
            if (shown != null) {
                applyPositionTo(shown);
            }
        }
        if (!useFormLayeredPaneExplicit) {
            // Through the mover, so a bar already on screen changes pane rather than
            // having the flag flipped underneath it.
            applyUseFormLayeredPane(defaults.useFormLayeredPane);
        }
        if (!defaultUIIDExplicit) {
            defaultUIID = defaults.defaultUIID;
        }
        if (!defaultMessageUIIDExplicit) {
            defaultMessageUIID = defaults.defaultMessageUIID;
        }
    }

    /// Updates the ToastBar UI component with the settings of the current status.
    private void updateStatus() {
        final ToastBarComponent c = getToastBarComponent();
        moveLayerToFront();
        if (c != null) {

            try {
                if (updatingStatus) {
                    pendingUpdateStatus = true;
                    return;
                }

                updatingStatus = true;
                if (c.currentlyShowing != null && !statuses.contains(c.currentlyShowing)) {
                    c.currentlyShowing = null;
                }
                if (c.currentlyShowing == null || statuses.isEmpty()) {
                    if (!statuses.isEmpty()) {
                        c.currentlyShowing = statuses.get(statuses.size() - 1);
                    } else {
                        setVisible(false);
                        return;
                    }

                }
                Status s = c.currentlyShowing;

                Label l = new Label(s.getMessage() != null ? s.getMessage() : "", defaultMessageUIID);

                c.leadButton.getListeners().clear();
                c.leadButton.addActionListener(s.getListener());
                c.leadButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        if (c.currentlyShowing != null && !c.currentlyShowing.showProgressIndicator) {
                            c.currentlyShowing.clear();
                            evt.consume();
                        }
                        ToastBar.this.setVisible(false);
                    }
                });

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
                c.progressBar.setVisible(s.getProgress() >= -1);
                if (s.getProgress() >= -1) {
                    if (!c.contains(c.progressBar)) {
                        c.addComponent(BorderLayout.SOUTH, c.progressBar);
                    }
                    if (s.getProgress() < 0) {
                        c.progressBar.setInfinite(true);
                    } else {
                        c.progressBar.setInfinite(false);
                        c.progressBar.setProgress(s.getProgress());
                    }
                } else {
                    c.removeComponent(c.progressBar);
                }
                c.icon.setVisible(s.getIcon() != null);
                if (s.getIcon() != null && c.icon.getIcon() != s.getIcon()) {
                    c.icon.setIcon(s.getIcon());
                }
                if (s.getIcon() == null && c.contains(c.icon)) {
                    c.removeComponent(c.icon);
                } else if (s.getIcon() != null && !c.contains(c.icon)) {

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
                        newLabel.setUIID(defaultMessageUIID);
                        //newLabel.setColumns(l.getText().length()+1);
                        //newLabel.setRows(l.getText().length()+1);
                        newLabel.setFocusable(false);
                        newLabel.setEditable(false);
                        newLabel.setVerticalAlignment(Component.CENTER);

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

                        // this can happen in an edge case where animateHierarchyAndWait and replaceAndWait
                        // are stuck in blocking mode between them and the label just got discarded see:
                        // https://stackoverflow.com/questions/46172993/codename-one-toastbar-nullpointerexception
                        if (c.label.getParent() != null) {
                            c.label.getParent().replaceAndWait(c.label, newLabel, CommonTransitions.createCover(CommonTransitions.SLIDE_VERTICAL, true, 300));
                            c.label = newLabel;

                            if (oldTextAreaSize.getHeight() != newTexAreaSize.getHeight()) {

                                c.label.setPreferredH(newTexAreaSize.getHeight());
                                c.getParent().animateHierarchyAndWait(300);
                            }
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
                        @Override
                        public void run() {
                            updateStatus();
                        }
                    });
                }
            }
        }
    }

    /// Creates a new Status.
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

    private Container getLayeredPane() {
        // The host, not the current form: a window has its own layered panes, and a
        // toast for one used to be added to the main form behind it.
        TopLevelContainer f = resolveHost();
        if (f == null) {
            throw new IllegalStateException("Cannot get layered pane when form is null");
        }
        if (useFormLayeredPane) {
            return f.getFormLayeredPane(this.getClass(), true);
        } else {
            return f.getLayeredPane(this.getClass(), true);
        }
    }

    private void moveLayerToFront() {
        TopLevelContainer f = resolveHost();
        if (f == null) {
            return;
        }
        final Container layered = getLayeredPane();
        final Container parent = layered.getParent();
        if (parent == null) {
            return;
        }
        if (parent.getComponentIndex(layered) != parent.getComponentCount() - 1) {
            f.getAnimationManager().flushAnimation(new FlushAnimationCallback(parent, layered));
        }
    }

    private ToastBarComponent getToastBarComponent() {
        return getToastBarComponent(true);
    }

    private ToastBarComponent getToastBarComponent(boolean create) {
        TopLevelContainer f = resolveHost();
        if (f != null && !(f instanceof Dialog)) {
            ToastBarComponent c = (ToastBarComponent) f.asContainer().getClientProperty("ToastBarComponent");
            if (c == null && !create) {
                return null;
            }
            if (c == null || c.getParent() == null) {
                c = new ToastBarComponent();
                c.hidden = true;
                f.asContainer().putClientProperty("ToastBarComponent", c);
                Container layered = getLayeredPane();
                layered.setLayout(new BorderLayout());
                layered.addComponent(position == Component.TOP ? BorderLayout.NORTH : BorderLayout.SOUTH, c);
                updateStatus();
            } else {
                // The constraint above is only chosen when the component is built or
                // reattached, and the position can change under one that is neither --
                // inheriting the shared default now does exactly that. Left alone the
                // bar stayed in the slot it was created in while animating as though it
                // had moved to the other one.
                applyPositionTo(c);
            }
            // The host's safe area and height. A window has no notch of its own, and
            // its height is what the toast has to sit inside.
            Rectangle safeArea = f.getSafeArea();
            // Written when it applies and taken off when it stops. Only the "applies"
            // half was here, so the gap the keyboard needed outlived the keyboard: it
            // stayed under a bar moved to the top, and stayed under one left at the
            // bottom once the keyboard had gone, in both cases as blank space no longer
            // standing for anything.
            int keyboardMargin = position == Component.BOTTOM ? f.getInvisibleAreaUnderVKB() : 0;
            if (keyboardMargin > 0 || c.keyboardMarginBottom > 0) {
                Style s = c.getAllStyles();
                s.setMarginUnit(Style.UNIT_TYPE_PIXELS);
                s.setMarginBottom(keyboardMargin);
                c.keyboardMarginBottom = keyboardMargin;
            }
            int safeBottomMargin = (f instanceof Window
                        ? f.asContainer().getHeight()
                        : Display.getInstance().getDisplayHeight())
                    - safeArea.getY()
                    - safeArea.getHeight();
            // Entered when there is an inset to apply OR one already applied, in the
            // shape the keyboard margin above already uses. Gated on the new value
            // alone, an inset that went away -- a rotation out of a notched edge -- left
            // the last one in the style: applyPositionTo clears these only when the
            // toast changes edge, so a cached toast staying at the bottom kept a blank
            // gap under it for as long as it lived.
            if (position == Component.BOTTOM
                    && (safeBottomMargin > 0 || c.safeAreaPaddingBottom > 0)) {
                int applied = Math.max(0, safeBottomMargin);
                Style s = c.getAllStyles();
                if (c.safeAreaPaddingBottom == 0) {
                    c.safeAreaPaddingBottomBase = c.getStyle().getPaddingBottom();
                }
                s.setPaddingUnit(Style.UNIT_TYPE_PIXELS);
                // The theme's own value back when there is no inset to write, not a
                // zero: this owns the padding only while an inset is applied.
                s.setPaddingBottom(applied > 0 ? applied : c.safeAreaPaddingBottomBase);
                c.safeAreaPaddingBottom = applied;
            } else if (position == Component.TOP
                    && (safeArea.getY() > 0 || c.safeAreaPaddingTop > 0)) {
                Container parent = c.getParent();
                if (parent != null) {
                    int needed = safeArea.getY() - parent.getAbsoluteY();
                    // Only what this owns. A top inset that resolves to nothing was
                    // always left alone -- the padding there is the style's own, and
                    // writing a zero over it takes away whatever the theme asked for.
                    // The clearing half is for an inset this actually applied.
                    if (needed > 0 || c.safeAreaPaddingTop > 0) {
                        int applied = Math.max(0, needed);
                        Style s = c.getAllStyles();
                        if (c.safeAreaPaddingTop == 0) {
                            c.safeAreaPaddingTopBase = c.getStyle().getPaddingTop();
                        }
                        s.setPaddingUnit(Style.UNIT_TYPE_PIXELS);
                        s.setPaddingTop(applied > 0 ? applied : c.safeAreaPaddingTopBase);
                        c.safeAreaPaddingTop = applied;
                    }
                }
            }

            return c;
        }
        return null;
    }

    /// Shows or hides the `ToastBar`.
    ///
    /// #### Parameters
    ///
    /// - `visible`
    public void setVisible(boolean visible) {
        final ToastBarComponent c = getToastBarComponent();
        if (c == null || c.isVisible() == visible) {
            return;
        }
        if (visible) {
            c.hidden = true;
            c.setVisible(false);
            c.setHeight(0);
            c.setShouldCalcPreferredSize(true);
            TopLevelContainer f = c.getTopLevelContainer();
            if (f != null) {
                f.asContainer().revalidate();
            } else {
                c.getParent().revalidate();
            }
            c.hidden = false;

            c.label.setPreferredH(UIManager.getInstance().getLookAndFeel().getTextAreaSize(c.label, true).getHeight());
            c.setShouldCalcPreferredSize(true);
            $(c).slideUpAndWait(2);
            $(c).slideDownAndWait(800);
            c.setVisible(true);
            updateStatus();

        } else {
            TopLevelContainer f = c.getTopLevelContainer();
            // A window has no menu bar and never will, so the menu test is a Form
            // question rather than something to widen the top level contract for.
            boolean menuShowing = f instanceof Form && ((Form) f).getMenuBar().isMenuShowing();
            if (f != null && f.isTopLevelShowing() && !menuShowing) {
                if (this.position == Component.BOTTOM) {
                    c.setY(c.getY() + c.getHeight());
                }
                $(c).slideUpAndWait(500);
            } else {
                c.getParent().revalidate();
            }
            c.hidden = true;
            c.setVisible(false);
        }
    }

    private static class ToastBarHolder {
        private static final ToastBar INSTANCE = new ToastBar();
    }

    private static class FlushAnimationCallback implements Runnable {
        private final Container parent;
        private final Container layered;

        public FlushAnimationCallback(Container parent, Container layered) {
            this.parent = parent;
            this.layered = layered;
        }

        @Override
        public void run() {
            parent.removeComponent(layered);
            parent.addComponent(layered);
            parent.revalidate();
        }
    }

    /// Represents a single status message.
    public class Status {

        /// The start time of the process this status is tracking.
        /// This UIID that should be used to style the ToastBar text while this
        /// message is being displayed.
        private String messageUIID = defaultMessageUIID;
        /// The UIID that should be used to style the ToastBar component while
        /// this message is being displayed.
        private String uiid = defaultUIID;
        /// Timer used to "expire" the message after a certain time.
        ///
        /// #### See also
        ///
        /// - #setExpires(int)
        private Timer timer;
        /// Timer used to delay the showing of the message.  Useful if you only want
        /// to show the message if the task ends up taking a long time.
        ///
        /// #### See also
        ///
        /// - #showDelayed(int)
        private Timer showTimer;

        /// The message to be displayed in the `ToastBar`.
        private String message;

        /// An action to perform when the ToastBar is tapped `ToastBar`.
        private ActionListener listener;

        /// Optional progress for the task.  (Not tested or implemented yet).
        private int progress = -2;

        /// Optional icon to show in the `ToastBar`.  (Not tested or implemented yet).
        private Image icon;

        /// Whether this status message should show an infinite progress indicator. (e.g. spinning beachball).
        private boolean showProgressIndicator;

        private Status() {
        }

        /// Directs the status to be cleared (if it isn't already cleared() after a given number of milliseconds.
        ///
        /// #### Parameters
        ///
        /// - `millis`: @param millis The maximum number of milliseconds that the status message should be displayed for.
        /// Helpful for error messages that only need to be displayed for a few seconds.
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
                            @Override
                            public void run() {
                                timer = null;
                                Status.this.clear();
                            }
                        });
                    }

                }, millis);
            }
        }

        /// Shows this status message.  Call this method after making any changes
        /// to the status that you want to have displayed.  This will always cause
        /// any currently-displayed status to be replaced by this status.
        ///
        /// If you don't want to show the status immediately, but rather to wait some delay, you can use
        /// the `#showDelayed(int)` method instead.
        ///
        /// #### See also
        ///
        /// - #showDelayed(int)
        public void show() {
            if (showTimer != null) {
                showTimer.cancel();
                showTimer = null;
            }
            ToastBarComponent c = getToastBarComponent();
            if (c != null) {
                c.currentlyShowing = this;
                updateStatus();
                setVisible(true);

            }
        }

        /// Schedules this status message to be shown after a specified number of milliseconds,
        /// if it hasn't been cleared or shown first.
        ///
        /// This is handy if you want to show a status for an operation that usually completes very quickly, but could
        /// potentially hang.  In such a case you might decide not to display a status message at all unless the operation
        /// takes more than 500ms to complete.
        ///
        /// If you want to show the status immediately, use the `#show()` method instead.
        ///
        /// #### Parameters
        ///
        /// - `millis`: Number of milliseconds to wait before showing the status.
        public void showDelayed(int millis) {
            showTimer = new Timer();
            showTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Display.getInstance().callSerially(new Runnable() {
                        @Override
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

        /// Clears this status message. This any pending "showDelayed" requests for this status.
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

        /// Returns the text that will be displayed for this status.
        ///
        /// #### Returns
        ///
        /// the message
        public String getMessage() {
            return message;
        }

        /// Sets the message that should be displayed in the `ToastBar`.
        ///
        /// #### Parameters
        ///
        /// - `message`
        public void setMessage(String message) {
            this.message = message;

        }

        /// Returns the listener added to perform a particular action.
        ///
        /// #### Returns
        ///
        /// the listener
        public ActionListener getListener() {
            return listener;
        }

        /// Sets the action listener needed to perform an action when the bar is tapped `ToastBar`.
        ///
        /// #### Parameters
        ///
        /// - `listener`
        public void setListener(ActionListener listener) {
            this.listener = listener;
        }

        /// Returns the progress of this status.  A value of -1 indicates that the progress
        /// bar should not be shown.  Values between 0 and 100 inclusive will be rendered
        /// on a progress bar (slider) in the status component.
        ///
        /// #### Returns
        ///
        /// the progress
        public int getProgress() {
            return progress;
        }

        /// Sets the progress (-1..100) that should be displayed in the progress bar
        /// for this status.  When set to -1 it will act as an infinite progress
        ///
        /// #### Parameters
        ///
        /// - `progress`
        public void setProgress(int progress) {
            this.progress = progress;
            updateStatus();
        }

        /// Gets the icon (may be null) that is displayed with this status.
        ///
        /// #### Returns
        ///
        /// the icon
        public Image getIcon() {
            return icon;
        }

        /// Sets the icon that is to be displayed with this status.  Set this to null to not show an icon.
        ///
        /// #### Parameters
        ///
        /// - `icon`: the icon to set
        public void setIcon(Image icon) {
            this.icon = icon;
        }

        /// #### Returns
        ///
        /// the showProgressIndicator
        public boolean isShowProgressIndicator() {
            return showProgressIndicator;
        }

        /// Sets whether this status message should include an infinite progress indicator (e.g. spinning beach ball).
        ///
        /// #### Parameters
        ///
        /// - `showProgressIndicator`: the showProgressIndicator to set
        public void setShowProgressIndicator(boolean showProgressIndicator) {
            this.showProgressIndicator = showProgressIndicator;
        }

        /// Gets the UIID to use for styling the text of this status message.
        ///
        /// #### Returns
        ///
        /// the messageUIID
        public String getMessageUIID() {
            return messageUIID;
        }

        /// Sets the UIID to use for styling the text of this status message.
        ///
        /// #### Parameters
        ///
        /// - `messageUIID`: the messageUIID to set
        public void setMessageUIID(String messageUIID) {
            this.messageUIID = messageUIID;
        }

        /// Gets the UIID that should be used for styling the status component while
        /// this status is displayed.
        ///
        /// #### Returns
        ///
        /// the uiid
        public String getUiid() {
            return uiid;
        }

        /// Sets the UIID that should be used for styling the status component while
        /// this status is displayed.
        ///
        /// #### Parameters
        ///
        /// - `uiid`: the uiid to set
        public void setUiid(String uiid) {
            this.uiid = uiid;
        }

    }

    /// The actual component for the `ToastBar`.  This is added to the layered pane of
    /// the top-level form.
    private class ToastBarComponent extends Container {
        private final InfiniteProgress progressLabel;
        private final Slider progressBar;
        private final Label icon;
        boolean hidden = true;
        /// The safe-area padding this class has written, so a move can take it off.
        ///
        /// The inset belongs to the edge the bar sits at, and only that edge is written
        /// when it is applied. Moving the bar to the other end therefore left the old
        /// edge's inset in place and added the new one on top, so a bar that started at
        /// the bottom of a device with a home indicator and was moved to the top carried
        /// both -- taller than it should be and inset away from the edge it now sits at.
        int safeAreaPaddingTop;
        int safeAreaPaddingBottom;
        /// What the style asked for before an inset was written over it.
        ///
        /// The inset replaces the padding rather than adding to it, so the theme's own
        /// value is gone for as long as one is applied -- and putting a zero back when
        /// it is taken off left a bar that the theme had spaced away from the edge
        /// sitting flush against it instead, for the rest of its life. Meaningful only
        /// while the matching safeAreaPadding above is non-zero, which is exactly the
        /// period this class owns the value in the style.
        int safeAreaPaddingTopBase;
        int safeAreaPaddingBottomBase;
        /// The keyboard margin this class has written, so it can be taken off again.
        int keyboardMarginBottom;
        Button leadButton = new Button();
        private TextArea label;
        private Status currentlyShowing;

        public ToastBarComponent() {
            this.getAllStyles().setBgColor(0x0);
            this.getAllStyles().setBackgroundType(Style.BACKGROUND_NONE);
            this.getAllStyles().setBgTransparency(128);
            super.setVisible(false);
            label = new TextArea();
            label.setUIID(defaultMessageUIID);
            label.setEditable(false);
            label.setFocusable(false);
            label.setVerticalAlignment(CENTER);

            progressLabel = new InfiniteProgress();

            progressLabel.setAngleIncrease(4);
            progressLabel.setVisible(false);
            icon = new Label();
            icon.setVisible(false);
            progressBar = new Slider();
            progressBar.setVisible(false);

            leadButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    if (currentlyShowing != null && !currentlyShowing.showProgressIndicator) {
                        currentlyShowing.clear();
                    }
                    ToastBar.this.setVisible(false);
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
}
