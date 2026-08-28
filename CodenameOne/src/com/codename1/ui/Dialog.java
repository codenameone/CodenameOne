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

import com.codename1.components.InteractionDialog;
import com.codename1.io.Log;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

import java.util.Map;

/// A dialog is a form that occupies a part of the screen and appears as a modal
/// entity to the developer. Dialogs allow us to prompt users for information and
/// rely on the information being available on the next line after the show method.
///
/// Modality indicates that a dialog will block the calling thread even if the
/// calling thread is the EDT. Notice that a dialog will not release the block
/// until dispose is called even if show() from another form is called! Events are still performed thanks
/// to the `com.codename1.ui.Display#invokeAndBlock(java.lang.Runnable)` capability of the
/// `Display` class.
///
/// To determine the size of the dialog use the show method that accepts 4 integer
/// values, notice that these values accept margin from the four sides rather than x, y, width
/// and height values!
///
/// It's important to style a `Dialog` using `Dialog#getDialogStyle()` or
/// `Dialog#setDialogUIID(java.lang.String)` methods rather than styling the dialog object directly.
///
/// The `Dialog` class also includes support for popup dialog which is a dialog type that is positioned
/// next to a component or screen area and points an arrow at that location.
///
/// Static `Dialog.show(...)` APIs can optionally use `InteractionDialog` under the hood by setting
/// `Dialog#setDefaultInteractionDialogMode(boolean)` or the theme constant `defaultInteractionDialogModeBool`.
///
/// Typical dialog usage looks like this:
///
/// ```java
/// final Button show = new Button("Show Dialog");
/// final Button showPopup = new Button("Show Popup");
/// cnt.add(show).add(showPopup);
/// show.addActionListener(new ActionListener() {
///     public void actionPerformed(ActionEvent evt) {
///         Dialog.show("Dialog Title", "This is the dialog body, it can contain anything...", "OK", "Cancel");
///     }
/// });
/// showPopup.addActionListener(new ActionListener() {
///     public void actionPerformed(ActionEvent evt) {
///         Dialog d = new Dialog("Popup Title");
///         TextArea popupBody = new TextArea("This is the body of the popup", 3, 10);
///         popupBody.setUIID("PopupBody");
///         popupBody.setEditable(false);
///         d.setLayout(new BorderLayout());
///         d.add(BorderLayout.CENTER, popupBody);
///         d.showPopupDialog(showPopup);
///     }
/// });
/// ```
///
/// See this sample for showing a dialog at the bottom of the screen:
///
/// ```java
/// Dialog dlg = new Dialog("At Bottom");
/// dlg.setLayout(new BorderLayout());
/// // span label accepts the text and the UIID for the dialog body
/// dlg.add(new SpanLabel("Dialog Body text", "DialogBody"));
/// int h = Display.getInstance().getDisplayHeight();
/// dlg.setDisposeWhenPointerOutOfBounds(true);
/// dlg.show(h /8 * 7, 0, 0, 0);
/// ```
///
/// @author Shai Almog
///
/// #### See also
///
/// - Display#invokeAndBlock(java.lang.Runnable)
public class Dialog extends Form implements AbstractDialog {

    /// Constant indicating the type of alert to indicate the sound to play or
    /// icon if none are explicitly set
    public static final int TYPE_NONE = 0;
    /// Constant indicating the type of alert to indicate the sound to play or
    /// icon if none are explicitly set
    public static final int TYPE_ALARM = 1;
    /// Constant indicating the type of alert to indicate the sound to play or
    /// icon if none are explicitly set
    public static final int TYPE_CONFIRMATION = 2;
    /// Constant indicating the type of alert to indicate the sound to play or
    /// icon if none are explicitly set
    public static final int TYPE_ERROR = 3;
    /// Constant indicating the type of alert to indicate the sound to play or
    /// icon if none are explicitly set
    public static final int TYPE_INFO = 4;
    /// Constant indicating the type of alert to indicate the sound to play or
    /// icon if none are explicitly set
    public static final int TYPE_WARNING = 5;
    /// Indicates whether Codename One should try to automatically adjust a showing dialog size
    /// when a screen size change event occurs
    private static boolean autoAdjustDialogSize = true;
    /// Default screen orientation position for the upcoming dialog. By default
    /// the dialog will be shown at hardcoded coordinates, this method allows us
    /// to pack the dialog appropriately in one of the border layout based locations
    /// see BorderLayout for futher details.
    private static String defaultDialogPosition;
    /// Allows a developer to indicate his interest that the dialog should no longer
    /// scroll on its own but rather rely on the scrolling properties of internal
    /// scrollable containers. This flag only affects the static show methods within
    /// this class.
    private static boolean disableStaticDialogScrolling;
    /// The default type for dialogs
    private static int defaultDialogType = TYPE_INFO;
    /// Places commands as buttons at the bottom of the standard static dialogs rather than
    /// as softbuttons. This is especially appropriate for devices such as touch devices and
    /// devices without the common softbuttons (e.g. blackberries).
    /// The default value is false
    private static boolean commandsAsButtons = true;
    /// The default pointer out of bounds dispose behavior, notice that
    /// this only applies to dialogs and not popup dialogs where this is
    /// always true by default
    private static boolean defaultDisposeWhenPointerOutOfBounds = false;
    /// Dialog background can be blurred using a Gaussian blur effect, this sets the radius of the Gaussian
    /// blur. -1 is a special case value that indicates that no blurring should take effect and the default tint mode
    /// only should be used
    private static float defaultBlurBackgroundRadius = -1;
    private static boolean defaultInteractionDialogMode;
    private static boolean defaultInteractionDialogModeInitialized;
    private static boolean defaultTitleCentered;
    /// Indicates whether the dialog has been disposed
    private boolean disposed;
    /// Indicates the time in which the alert should be disposed
    private long time;
    /// Indicates the last command selected by the user in this form
    private Command lastCommandPressed;
    /// Indicates that this is a menu preventing getCurrent() from ever returning this class
    private boolean menu;
    private int dialogType;
    private int top = -1;
    private int bottom;
    private int left;
    private int right;
    private boolean includeTitle;
    private String position;
    /// Default screen orientation position for the upcoming dialog. By default
    /// the dialog will be shown at hardcoded coordinates, this method allows us
    /// to pack the dialog appropriately in one of the border layout based locations
    /// see BorderLayout for futher details.
    private String dialogPosition = defaultDialogPosition;
    /// Determines whether the execution of a command on this dialog implicitly
    /// disposes the dialog. This defaults to true which is a sensible default for
    /// simple dialogs.
    private boolean autoDispose = true;
    private boolean modal = true;
    private Command[] buttonCommands;
    private boolean disposeOnRotation;
    private boolean disposeWhenPointerOutOfBounds = defaultDisposeWhenPointerOutOfBounds;
    private boolean pressedOutOfBounds;
    /// Returns true if the dialog was disposed automatically due to device rotation
    private boolean disposedDueToRotation;
    private Label dialogTitle;
    private Container dialogContentPane;
    private Container centeredTitleBody;
    private Container centeredTitleArea;
    /// Indicates if we want to enforce directional bias for the popup dialog. If null this field is ignored but if
    /// its set to a value it biases the system towards a fixed direction for the popup dialog.
    private Boolean popupDirectionBiasPortrait;
    /// Dialog background can be blurred using a Gaussian blur effect, this sets the radius of the Gaussian
    /// blur. -1 is a special case value that indicates that no blurring should take effect and the default tint mode
    /// only should be used
    private float blurBackgroundRadius = defaultBlurBackgroundRadius;
    private boolean isUIIDByPopupPosition;
    private boolean interactionDialogMode = defaultInteractionDialogMode;
    private boolean titleCentered = defaultTitleCentered;

    /// The top level this dialog was told to appear on, or null to work it out.
    private TopLevelContainer hostTopLevel;

    /// True while `#hostTopLevel` holds a host worked out from a popup's anchor rather
    /// than one the application asked for. Such a host belongs to that one showing.
    private boolean hostTopLevelInferred;

    /// The host in force before a popup inferred one, put back when the popup goes.
    private TopLevelContainer hostTopLevelBeforeInference;

    /// The window this dialog is currently hosted in, or null when it is shown the
    /// historical way by taking over the main surface. Every behaviour that belongs to
    /// that historical path is gated on this being null.
    private Window layerHost;

    /// The layer inside `#layerHost` this dialog was added to. Held rather than
    /// re-fetched: `Window#getFormLayeredPane(java.lang.Class, boolean)` re-applies
    /// its layout flag on every call, so asking again after the add can clear it.
    private Container activeLayer;

    /// The dimming, input blocking backdrop behind a hosted dialog, or null when the
    /// dialog neither blocks nor dismisses on an outside press.
    private Container scrim;

    /// The dialog's own background painter, put back when the hosted showing ends.
    private Painter savedBgPainter;

    /// Listens for the host window resizing while this dialog is on it.
    private ActionListener hostSizeListener;

    /// Listens for the back key while this dialog is on a window.
    private ActionListener hostBackListener;

    /// The host's shape when the dialog was shown, so a resize can tell an orientation
    /// flip from an ordinary resize. True when it was taller than it was wide.
    private boolean hostWasPortrait;

    /// Shown a dialog that neither blocks input nor closes on an outside press needs no
    /// backdrop at all; this is the marker that one was skipped.
    private static final Painter NO_OP_PAINTER = new NoOpPainter();

    /// A painter that draws nothing.
    ///
    /// A hosted dialog spans its whole layer, so its `Form` background would paint an
    /// opaque rectangle over the window behind it. The historical path has the same
    /// problem and solves it the same way, by swapping the painter for the duration of
    /// the showing and putting the original back afterwards.
    private static final class NoOpPainter implements Painter {
        @Override
        public void paint(Graphics g, Rectangle rect) {
        }
    }

    /// Yields for a moment, so the modal wait does not spin.
    private static final Runnable BLOCKING_SLEEP = new BlockingSleepRunnable();

    /// The body of `#BLOCKING_SLEEP`, named rather than anonymous so it is a static
    /// class and does not hold the dialog that created it.
    private static final class BlockingSleepRunnable implements Runnable {
        @Override
        public void run() {
            com.codename1.io.Util.sleep(10);
        }
    }

    /// The backdrop behind a dialog hosted on a window.
    ///
    /// It does three jobs at once, and they are the same job: it dims the window, it
    /// swallows the presses that must not reach what is behind a modal dialog, and it
    /// is what delivers an outside press to the dialog. Without something in the layer
    /// that responds to pointer events, `Window` hands the press to its content pane
    /// instead and the dialog never hears about it.
    private static final class DialogScrim extends Container {
        private final Dialog dlg;
        private final int tint;
        private final Image backdrop;

        DialogScrim(Dialog dlg, boolean blocking, int tint, Image backdrop) {
            this.dlg = dlg;
            this.tint = tint;
            this.backdrop = backdrop;
            setGrabsPointerEvents(blocking);
        }

        @Override
        protected void paintBackground(Graphics g) {
            int w = getWidth();
            int h = getHeight();
            if (backdrop != null) {
                g.drawImage(backdrop, getX(), getY(), w, h);
            }
            int alpha = (tint >> 24) & 0xff;
            if (alpha != 0) {
                g.setColor(tint);
                g.fillRect(getX(), getY(), w, h, (byte) alpha);
            }
        }

        @Override
        public void pointerPressed(int x, int y) {
            dlg.scrimPressed(x, y);
        }

        @Override
        public void pointerReleased(int x, int y) {
            dlg.scrimReleased(x, y);
        }
    }

    /// Watches the host window's size while a dialog is on it, standing in for the
    /// `sizeChangedInternal` the dialog would have been sent on the main surface.
    private static final class HostSizeListener implements ActionListener {
        private final Dialog dlg;

        HostSizeListener(Dialog dlg) {
            this.dlg = dlg;
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            dlg.hostResized(evt.getX(), evt.getY());
        }
    }

    /// Delivers the back key to a dialog hosted on a window. A window has no menu bar
    /// to route it, so the dialog listens for it directly.
    private static final class HostBackListener implements ActionListener {
        private final Dialog dlg;

        HostBackListener(Dialog dlg) {
            this.dlg = dlg;
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            dlg.hostBackPressed();
        }
    }

    /// Constructs a Dialog with a title
    ///
    /// #### Parameters
    ///
    /// - `title`: the title of the dialog
    public Dialog(String title) {
        this();
        setTitle(title);
    }

    /// Constructs a Dialog with a title
    ///
    /// #### Parameters
    ///
    /// - `title`: the title of the dialog
    ///
    /// - `lm`: the layout for the dialog
    public Dialog(String title, Layout lm) {
        this(lm);
        setTitle(title);
    }

    /// Constructs a Dialog
    public Dialog() {
        this("Dialog", "DialogTitle");
    }

    /// Constructs a Dialog with a layout
    ///
    /// #### Parameters
    ///
    /// - `lm`: the layout manager
    public Dialog(Layout lm) {
        this("Dialog", "DialogTitle", lm);
    }


    Dialog(String dialogUIID, String dialogTitleUIID) {
        super();
        initImpl(dialogUIID, dialogTitleUIID, null);
    }

    Dialog(String dialogUIID, String dialogTitleUIID, Layout lm) {
        super();
        initImpl(dialogUIID, dialogTitleUIID, lm);
    }

    /// The default pointer out of bounds dispose behavior, notice that
    /// this only applies to dialogs and not popup dialogs where this is
    /// always true by default
    ///
    /// #### Returns
    ///
    /// the defaultDisposeWhenPointerOutOfBounds
    public static boolean isDefaultDisposeWhenPointerOutOfBounds() {
        return defaultDisposeWhenPointerOutOfBounds;
    }

    /// The default pointer out of bounds dispose behavior, notice that
    /// this only applies to dialogs and not popup dialogs where this is
    /// always true by default
    ///
    /// #### Parameters
    ///
    /// - `aDefaultDisposeWhenPointerOutOfBounds`: the defaultDisposeWhenPointerOutOfBounds to set
    public static void setDefaultDisposeWhenPointerOutOfBounds(
            boolean aDefaultDisposeWhenPointerOutOfBounds) {
        defaultDisposeWhenPointerOutOfBounds =
                aDefaultDisposeWhenPointerOutOfBounds;
    }

    /// Shows a modal prompt dialog with the given title and text.
    ///
    /// #### Parameters
    ///
    /// - `title`: The title for the dialog optionally null;
    ///
    /// - `text`: the text displayed in the dialog
    ///
    /// - `type`: @param type       the type of the alert one of TYPE_WARNING, TYPE_INFO,
    /// TYPE_ERROR, TYPE_CONFIRMATION or TYPE_ALARM
    ///
    /// - `icon`: the icon for the dialog, can be null
    ///
    /// - `okText`: the text to appear in the command dismissing the dialog
    ///
    /// - `cancelText`: @param cancelText optionally null for a text to appear in the cancel command
    /// for canceling the dialog
    ///
    /// #### Returns
    ///
    /// true if the ok command was pressed or if cancelText is null. False otherwise.
    public static boolean show(String title, String text, int type, Image icon, String okText, String cancelText) {
        return show(title, text, type, icon, okText, cancelText, 0);
    }

    /// Shows a modal prompt dialog with the given title and text.
    ///
    /// #### Parameters
    ///
    /// - `title`: The title for the dialog optionally null;
    ///
    /// - `text`: the text displayed in the dialog
    ///
    /// - `type`: @param type       the type of the alert one of TYPE_WARNING, TYPE_INFO,
    /// TYPE_ERROR, TYPE_CONFIRMATION or TYPE_ALARM
    ///
    /// - `icon`: the icon for the dialog, can be null
    ///
    /// - `okText`: the text to appear in the command dismissing the dialog
    ///
    /// - `cancelText`: @param cancelText optionally null for a text to appear in the cancel command
    /// for canceling the dialog
    ///
    /// - `timeout`: a timeout after which null would be returned if timeout is 0 inifinite time is used
    ///
    /// #### Returns
    ///
    /// true if the ok command was pressed or if cancelText is null. False otherwise.
    public static boolean show(String title, String text, int type, Image icon, String okText, String cancelText, long timeout) {
        Command[] cmds;
        Command okCommand = new Command(okText);
        if (cancelText != null) {
            cmds = new Command[]{new Command(cancelText), okCommand};
        } else {
            cmds = new Command[]{okCommand};
        }
        return show(title, text, okCommand, cmds, type, icon, timeout) == okCommand; //NOPMD CompareObjectsWithEquals
    }

    /// Shows a modal prompt dialog with the given title and text.
    ///
    /// #### Parameters
    ///
    /// - `title`: The title for the dialog optionally null;
    ///
    /// - `text`: the text displayed in the dialog
    ///
    /// - `cmds`: @param cmds    commands that are added to the form any click on any command
    /// will dispose the form
    ///
    /// - `type`: @param type    the type of the alert one of TYPE_WARNING, TYPE_INFO,
    /// TYPE_ERROR, TYPE_CONFIRMATION or TYPE_ALARM
    ///
    /// - `icon`: the icon for the dialog, can be null
    ///
    /// - `timeout`: a timeout after which null would be returned if timeout is 0 inifinite time is used
    ///
    /// #### Returns
    ///
    /// the command pressed by the user
    public static Command show(String title, String text, Command[] cmds, int type, Image icon, long timeout) {
        return show(title, text, null, cmds, type, icon, timeout);
    }

    /// Shows a modal prompt dialog with the given title and text.
    ///
    /// #### Parameters
    ///
    /// - `title`: The title for the dialog optionally null;
    ///
    /// - `text`: the text displayed in the dialog
    ///
    /// - `defaultCommand`: command to be assigned as the default command or null
    ///
    /// - `cmds`: @param cmds           commands that are added to the form any click on any command
    /// will dispose the form
    ///
    /// - `type`: @param type           the type of the alert one of TYPE_WARNING, TYPE_INFO,
    /// TYPE_ERROR, TYPE_CONFIRMATION or TYPE_ALARM
    ///
    /// - `icon`: the icon for the dialog, can be null
    ///
    /// - `timeout`: a timeout after which null would be returned if timeout is 0 inifinite time is used
    ///
    /// #### Returns
    ///
    /// the command pressed by the user
    public static Command show(String title, String text, Command defaultCommand, Command[] cmds, int type, Image icon, long timeout) {
        return show(title, text, defaultCommand, cmds, type, icon, timeout, null);
    }

    /// Shows a modal prompt dialog with the given title and text.
    ///
    /// #### Parameters
    ///
    /// - `title`: The title for the dialog optionally null;
    ///
    /// - `text`: the text displayed in the dialog
    ///
    /// - `cmds`: @param cmds       commands that are added to the form any click on any command
    /// will dispose the form
    ///
    /// - `type`: @param type       the type of the alert one of TYPE_WARNING, TYPE_INFO,
    /// TYPE_ERROR, TYPE_CONFIRMATION or TYPE_ALARM
    ///
    /// - `icon`: the icon for the dialog, can be null
    ///
    /// - `timeout`: a timeout after which null would be returned if timeout is 0 inifinite time is used
    ///
    /// - `transition`: the transition installed when the dialog enters/leaves
    ///
    /// #### Returns
    ///
    /// the command pressed by the user
    public static Command show(String title, String text, Command[] cmds, int type, Image icon, long timeout, Transition transition) {
        return show(title, text, null, cmds, type, icon, timeout, transition);
    }

    /// Shows a modal prompt dialog with the given title and text.
    ///
    /// #### Parameters
    ///
    /// - `title`: The title for the dialog optionally null;
    ///
    /// - `text`: the text displayed in the dialog
    ///
    /// - `defaultCommand`: command to be assigned as the default command or null
    ///
    /// - `cmds`: @param cmds           commands that are added to the form any click on any command
    /// will dispose the form
    ///
    /// - `type`: @param type           the type of the alert one of TYPE_WARNING, TYPE_INFO,
    /// TYPE_ERROR, TYPE_CONFIRMATION or TYPE_ALARM
    ///
    /// - `icon`: the icon for the dialog, can be null
    ///
    /// - `timeout`: a timeout after which null would be returned if timeout is 0 inifinite time is used
    ///
    /// - `transition`: the transition installed when the dialog enters/leaves
    ///
    /// #### Returns
    ///
    /// the command pressed by the user
    public static Command show(String title, String text, Command defaultCommand, Command[] cmds, int type, Image icon, long timeout, Transition transition) {
        Map<String, String> h = UIManager.getInstance().getBundle();
        if (h != null && text != null) {
            Object o = h.get(text);
            if (o != null) {
                text = (String) o;
            }
        }
        TextArea t = new TextArea(text, 3, 30);
        t.setUIID("DialogBody");
        t.setEditable(false);
        return show(title, t, defaultCommand, cmds, type, icon, timeout, transition);
    }

    /// Shows a modal prompt dialog with the given title and text.
    ///
    /// #### Parameters
    ///
    /// - `title`: The title for the dialog optionally null;
    ///
    /// - `text`: the text displayed in the dialog
    ///
    /// - `okText`: the text to appear in the command dismissing the dialog
    ///
    /// - `cancelText`: @param cancelText optionally null for a text to appear in the cancel command
    /// for canceling the dialog
    ///
    /// #### Returns
    ///
    /// true if the ok command was pressed or if cancelText is null. False otherwise.
    public static boolean show(String title, String text, String okText, String cancelText) {
        return show(title, text, defaultDialogType, null, okText, cancelText);
    }

    /// Shows a modal dialog with the given component as its "body" placed in the
    /// center.
    ///
    /// #### Parameters
    ///
    /// - `title`: title for the dialog
    ///
    /// - `body`: component placed in the center of the dialog
    ///
    /// - `cmds`: @param cmds  commands that are added to the form any click on any command
    /// will dispose the form
    ///
    /// #### Returns
    ///
    /// the command pressed by the user
    public static Command show(String title, Component body, Command... cmds) {
        return show(title, body, cmds, defaultDialogType, null);
    }

    /// Shows a modal dialog with the given component as its "body" placed in the
    /// center.
    ///
    /// #### Parameters
    ///
    /// - `title`: title for the dialog
    ///
    /// - `body`: text placed in the center of the dialog
    ///
    /// - `cmds`: @param cmds  commands that are added to the form any click on any command
    /// will dispose the form
    ///
    /// #### Returns
    ///
    /// the command pressed by the user
    public static Command show(String title, String body, Command... cmds) {
        TextArea t = new TextArea(body, 3, 30);
        t.setUIID("DialogBody");
        t.setEditable(false);
        return show(title, t, cmds);
    }

    /// Shows a modal dialog with the given component as its "body" placed in the
    /// center.
    ///
    /// #### Parameters
    ///
    /// - `title`: title for the dialog
    ///
    /// - `body`: component placed in the center of the dialog
    ///
    /// - `cmds`: @param cmds  commands that are added to the form any click on any command
    /// will dispose the form
    ///
    /// - `type`: @param type  the type of the alert one of TYPE_WARNING, TYPE_INFO,
    /// TYPE_ERROR, TYPE_CONFIRMATION or TYPE_ALARM
    ///
    /// - `icon`: the icon for the dialog, can be null
    ///
    /// #### Returns
    ///
    /// the command pressed by the user
    public static Command show(String title, Component body, Command[] cmds, int type, Image icon) {
        return show(title, body, cmds, type, icon, 0);
    }

    /// Shows a modal dialog with the given component as its "body" placed in the
    /// center.
    ///
    /// #### Parameters
    ///
    /// - `title`: title for the dialog
    ///
    /// - `body`: component placed in the center of the dialog
    ///
    /// - `cmds`: @param cmds    commands that are added to the form any click on any command
    /// will dispose the form
    ///
    /// - `type`: @param type    the type of the alert one of TYPE_WARNING, TYPE_INFO,
    /// TYPE_ERROR, TYPE_CONFIRMATION or TYPE_ALARM
    ///
    /// - `icon`: the icon for the dialog, can be null
    ///
    /// - `timeout`: a timeout after which null would be returned if timeout is 0 inifinite time is used
    ///
    /// #### Returns
    ///
    /// the command pressed by the user
    public static Command show(String title, Component body, Command[] cmds, final int type, Image icon, long timeout) {
        return show(title, body, cmds, type, icon, timeout, null);
    }

    /// Shows a modal dialog with the given component as its "body" placed in the
    /// center.
    ///
    /// #### Parameters
    ///
    /// - `title`: title for the dialog
    ///
    /// - `body`: component placed in the center of the dialog
    ///
    /// - `cmds`: @param cmds       commands that are added to the form any click on any command
    /// will dispose the form
    ///
    /// - `type`: @param type       the type of the alert one of TYPE_WARNING, TYPE_INFO,
    /// TYPE_ERROR, TYPE_CONFIRMATION or TYPE_ALARM
    ///
    /// - `icon`: the icon for the dialog, can be null
    ///
    /// - `timeout`: a timeout after which null would be returned if timeout is 0 infinite time is used
    ///
    /// - `transition`: the transition installed when the dialog enters/leaves
    ///
    /// #### Returns
    ///
    /// the command pressed by the user
    public static Command show(String title, Component body, Command[] cmds, int type, Image icon, long timeout, Transition transition) {
        return show(title, body, null, cmds, type, icon, timeout, transition);
    }

    /// Shows a modal dialog with the given component as its "body" placed in the
    /// center.
    ///
    /// #### Parameters
    ///
    /// - `title`: title for the dialog
    ///
    /// - `body`: component placed in the center of the dialog
    ///
    /// - `defaultCommand`: command to be assigned as the default command or null
    ///
    /// - `cmds`: @param cmds           commands that are added to the form any click on any command
    /// will dispose the form
    ///
    /// - `type`: @param type           the type of the alert one of TYPE_WARNING, TYPE_INFO,
    /// TYPE_ERROR, TYPE_CONFIRMATION or TYPE_ALARM
    ///
    /// - `icon`: the icon for the dialog, can be null
    ///
    /// - `timeout`: a timeout after which null would be returned if timeout is 0 inifinite time is used
    ///
    /// - `transition`: the transition installed when the dialog enters/leaves
    ///
    /// #### Returns
    ///
    /// the command pressed by the user
    public static Command show(String title, Component body, Command defaultCommand, Command[] cmds, int type, Image icon, long timeout, Transition transition) {
        if (!isDefaultInteractionDialogMode()) {
            Dialog dialog = new Dialog(title);
            dialog.dialogType = type;
            dialog.setTransitionInAnimator(transition);
            dialog.setTransitionOutAnimator(transition);
            dialog.lastCommandPressed = null;
            dialog.setLayout(new BorderLayout());
            if (cmds != null) {
                if (commandsAsButtons) {
                    dialog.placeButtonCommands(cmds);
                } else {
                    for (Command cmd : cmds) {
                        dialog.addCommand(cmd);
                    }
                }

                // maps the first command to back
                if (cmds.length == 1 || cmds.length == 2) {
                    dialog.setBackCommand(cmds[0]);
                }
            }
            if (defaultCommand != null) {
                dialog.setDefaultCommand(defaultCommand);
            }
            dialog.addComponent(BorderLayout.CENTER, body);
            if (icon != null) {
                dialog.addComponent(BorderLayout.EAST, new Label(icon));
            }
            if (timeout != 0) {
                dialog.setTimeout(timeout);
            }
            if (body.isScrollable() || disableStaticDialogScrolling) {
                dialog.setScrollable(false);
            }
            dialog.show();
            return dialog.lastCommandPressed;
        }

        AbstractDialog dialog = new InteractionDialog(title, new BorderLayout());
        dialog.setDialogType(type);
        dialog.setTransitions(transition);
        dialog.configureCommands(cmds, commandsAsButtons);
        dialog.setDefaultCommand(defaultCommand);
        dialog.addComponent(BorderLayout.CENTER, body);
        if (icon != null) {
            dialog.addComponent(BorderLayout.EAST, new Label(icon));
        }
        if (timeout != 0) {
            dialog.setTimeout(timeout);
        }
        if (body.isScrollable() || disableStaticDialogScrolling) {
            dialog.setScrollable(false);
        }
        return dialog.showDialog();
    }

    /// Default screen orientation position for the upcoming dialog. By default
    /// the dialog will be shown at hardcoded coordinates, this method allows us
    /// to pack the dialog appropriately in one of the border layout based locations
    /// see BorderLayout for futher details.
    ///
    /// #### Returns
    ///
    /// position for dialogs on the sceen using BorderLayout orientation tags
    public static String getDefaultDialogPosition() {
        return defaultDialogPosition;
    }

    /// Default screen orientation position for the upcoming dialog. By default
    /// the dialog will be shown at hardcoded coordinates, this method allows us
    /// to pack the dialog appropriately in one of the border layout based locations
    /// see BorderLayout for futher details.
    ///
    /// #### Parameters
    ///
    /// - `p`: for dialogs on the sceen using BorderLayout orientation tags
    public static void setDefaultDialogPosition(String p) {
        defaultDialogPosition = p;
    }

    /// The default type for dialogs
    ///
    /// #### Returns
    ///
    /// the default type for the dialog
    public static int getDefaultDialogType() {
        return defaultDialogType;
    }

    /// The default type for dialogs
    ///
    /// #### Parameters
    ///
    /// - `d`: the default type for the dialog
    public static void setDefaultDialogType(int d) {
        defaultDialogType = d;
    }

    /// Indicates whether Codename One should try to automatically adjust a showing dialog size
    /// when a screen size change event occurs
    ///
    /// #### Returns
    ///
    /// true to indicate that Codename One should make a "best effort" to resize the dialog
    public static boolean isAutoAdjustDialogSize() {
        return autoAdjustDialogSize;
    }

    /// Indicates whether Codename One should try to automatically adjust a showing dialog size
    /// when a screen size change event occurs
    ///
    /// #### Parameters
    ///
    /// - `a`: true to indicate that Codename One should make a "best effort" to resize the dialog
    public static void setAutoAdjustDialogSize(boolean a) {
        autoAdjustDialogSize = a;
    }

    /// Allows a developer to indicate his interest that the dialog should no longer
    /// scroll on its own but rather rely on the scrolling properties of internal
    /// scrollable containers. This flag only affects the static show methods within
    /// this class.
    ///
    /// #### Returns
    ///
    /// true if scrolling should be activated, false otherwise
    public static boolean isDisableStaticDialogScrolling() {
        return disableStaticDialogScrolling;
    }

    /// Allows a developer to indicate his interest that the dialog should no longer
    /// scroll on its own but rather rely on the scrolling properties of internal
    /// scrollable containers. This flag only affects the static show methods within
    /// this class.
    ///
    /// #### Parameters
    ///
    /// - `d`: indicates whether scrolling should be active or not
    public static void setDisableStaticDialogScrolling(boolean d) {
        disableStaticDialogScrolling = d;
    }

    /// Places commands as buttons at the bottom of the standard static dialogs rather than
    /// as softbuttons. This is especially appropriate for devices such as touch devices and
    /// devices without the common softbuttons (e.g. blackberries).
    /// The default value is false
    ///
    /// #### Returns
    ///
    /// true if commands are placed as buttons and not as softbutton keys
    public static boolean isCommandsAsButtons() {
        return commandsAsButtons;
    }

    /// Places commands as buttons at the bottom of the standard static dialogs rather than
    /// as softbuttons. This is especially appropriate for devices such as touch devices and
    /// devices without the common softbuttons (e.g. blackberries).
    /// The default value is false
    ///
    /// #### Parameters
    ///
    /// - `c`: true to place commands as buttons and not as softbutton keys
    public static void setCommandsAsButtons(boolean c) {
        commandsAsButtons = c;
    }

    /// Dialog background can be blurred using a Gaussian blur effect, this sets the radius of the Gaussian
    /// blur. -1 is a special case value that indicates that no blurring should take effect and the default tint mode
    /// only should be used
    ///
    /// #### Returns
    ///
    /// the defaultBlurBackgroundRadius
    public static float getDefaultBlurBackgroundRadius() {
        return defaultBlurBackgroundRadius;
    }

    /// Dialog background can be blurred using a Gaussian blur effect, this sets the radius of the Gaussian
    /// blur. -1 is a special case value that indicates that no blurring should take effect and the default tint mode
    /// only should be used. Notice that this value can be set using the theme constant: `dialogBlurRadiusInt`
    ///
    /// #### Parameters
    ///
    /// - `aDefaultBlurBackgroundRadius`: the defaultBlurBackgroundRadius to set
    public static void setDefaultBlurBackgroundRadius(float aDefaultBlurBackgroundRadius) {
        defaultBlurBackgroundRadius = aDefaultBlurBackgroundRadius;
    }

    /// Indicates whether newly-created dialogs place their title in the absolute
    /// center, with the body below it. This default can be configured with the
    /// `dialogTitleCenterBool` theme constant.
    ///
    /// #### Returns
    ///
    /// true when newly-created dialogs use the centered title layout
    public static boolean isDefaultTitleCentered() {
        return defaultTitleCentered;
    }

    /// Sets whether newly-created `Dialog` and `InteractionDialog` instances place
    /// their title in the absolute center, with the body below it.
    ///
    /// #### Parameters
    ///
    /// - `defaultTitleCentered`: true to use the centered title layout by default
    public static void setDefaultTitleCentered(boolean defaultTitleCentered) {
        Dialog.defaultTitleCentered = defaultTitleCentered;
    }

    private static void initDefaultInteractionDialogMode() {
        if (!defaultInteractionDialogModeInitialized) {
            defaultInteractionDialogModeInitialized = true;
            defaultInteractionDialogMode = UIManager.getInstance().isThemeConstant("defaultInteractionDialogModeBool", defaultInteractionDialogMode);
        }
    }

    /// Indicates whether newly-created dialogs should use `InteractionDialog` under the hood.
    ///
    /// This default can be configured globally using the theme constant
    /// `defaultInteractionDialogModeBool`.
    public static boolean isDefaultInteractionDialogMode() {
        initDefaultInteractionDialogMode();
        return defaultInteractionDialogMode;
    }

    /// Indicates whether newly-created dialogs should use `InteractionDialog` under the hood.
    ///
    /// This value overrides the theme constant `defaultInteractionDialogModeBool`
    /// for the remainder of the app lifecycle.
    public static void setDefaultInteractionDialogMode(boolean defaultInteractionDialogMode) {
        defaultInteractionDialogModeInitialized = true;
        Dialog.defaultInteractionDialogMode = defaultInteractionDialogMode;
    }

    /// Indicates whether this dialog should use `InteractionDialog` under the hood.
    public boolean isInteractionDialogMode() {
        return interactionDialogMode;
    }

    /// Indicates whether this dialog should use `InteractionDialog` under the hood.
    public void setInteractionDialogMode(boolean interactionDialogMode) {
        this.interactionDialogMode = interactionDialogMode;
    }

    /// Disabling ad padding for dialogs
    @Override
    void initAdPadding(Display d) {
    }

    private void initImpl(String dialogUIID, String dialogTitleUIID, Layout lm) {
        super.getContentPane().setUIID(dialogUIID);
        super.getTitleComponent().setText("");
        super.getTitleComponent().setVisible(false);
        super.getTitleArea().setVisible(false);
        super.getTitleArea().setUIID("Container");
        lockStyleImages(getUnselectedStyle());
        titleArea.setVisible(false);

        if (lm != null) {
            dialogContentPane = new Container(lm);
        } else {
            dialogContentPane = new Container();
        }
        dialogContentPane.setUIID("DialogContentPane");
        dialogTitle = new Label("", dialogTitleUIID);
        super.getContentPane().setLayout(new BorderLayout());
        updateTitleLayout();
        super.getContentPane().setScrollable(false);
        super.getContentPane().setAlwaysTensile(false);

        super.getStyle().setBgTransparency(0);
        super.getStyle().setBgImage(null);
        super.getStyle().setBorder(null);
        setSmoothScrolling(false);
        deregisterAnimated(this);
    }

    /// When the dialog is disposed this form will show. Notice that this can only be set after show was invoked!
    ///
    /// #### Parameters
    ///
    /// - `previousForm`: the previous form
    @Override
    public void setPreviousForm(Form previousForm) {
        super.setPreviousForm(previousForm);
    }

    /// Overriden to disable the toolbar in dialogs
    ///
    /// {@inheritDoc}
    @Override
    protected final void initGlobalToolbar() {
    }

    @Override
    public Container getContentPane() {
        return dialogContentPane;
    }

    /// {@inheritDoc}
    @Override
    public Layout getLayout() {
        return dialogContentPane.getLayout();
    }

    /// {@inheritDoc}
    @Override
    public final void setLayout(Layout layout) {
        dialogContentPane.setLayout(layout);
    }

    /// {@inheritDoc}
    @Override
    public String getTitle() {
        return dialogTitle.getText();
    }

    /// {@inheritDoc}
    @Override
    public final void setTitle(String title) {
        dialogTitle.setText(title);
    }

    /// {@inheritDoc}
    @Override
    public final void addComponent(Component cmp) {
        dialogContentPane.addComponent(cmp);
    }

    /// {@inheritDoc}
    @Override
    public void addComponent(Object constraints, Component cmp) {
        dialogContentPane.addComponent(constraints, cmp);
    }

    /// {@inheritDoc}
    @Override
    public void addComponent(int index, Object constraints, Component cmp) {
        dialogContentPane.addComponent(index, constraints, cmp);
    }

    /// {@inheritDoc}
    @Override
    public void addComponent(int index, Component cmp) {
        dialogContentPane.addComponent(index, cmp);
    }

    /// {@inheritDoc}
    @Override
    public void removeAll() {
        dialogContentPane.removeAll();
    }

    /// {@inheritDoc}
    @Override
    public void removeComponent(Component cmp) {
        dialogContentPane.removeComponent(cmp);
    }

    /// Refreshing the theme reinstalls the menu bar, and installing it moves the form's title
    /// component into the title area. A dialog's title component is its own title label, and a
    /// dialog keeps the form title area hidden -- so without putting the label back where it
    /// belongs, refreshing the theme loses the dialog's title altogether, along with the space
    /// it occupied in the centered-title layout.
    ///
    /// {@inheritDoc}
    @Override
    public void refreshTheme(boolean merge) {
        super.refreshTheme(merge);
        restoreDisplacedTitle();
    }

    /// Puts the title label back where this dialog's layout wants it, and touches nothing else.
    ///
    /// Rebuilding the whole layout would remove and re-add the content pane, which deinitializes
    /// that subtree -- taking the form's focus with it when the focused component is in there,
    /// and closing an editor the user is typing in. Only the label moved, so only the label is
    /// moved back.
    private void restoreDisplacedTitle() {
        Container root = super.getContentPane();
        Container target = titleCentered ? centeredTitleArea : root;
        if (target == null) {
            updateTitleLayout();
            revalidate();
            return;
        }
        if (titleCentered && centeredTitleArea != null) {
            // The area's UIID comes from a theme constant, and a refresh is exactly when that
            // constant can have changed. Restoring the label without it would leave a centered
            // dialog wearing the previous theme's title styling.
            centeredTitleArea.setUIID(getUIManager().getThemeConstant(
                    "dlgCenteredTitleUIID", "Container"));
        }
        if (dialogTitle.getParent() == target) { //NOPMD CompareObjectsWithEquals
            return;
        }
        if (dialogTitle.getParent() != null) {
            dialogTitle.remove();
        }
        if (titleCentered) {
            target.addComponent(BorderLayout.CENTER, dialogTitle);
        } else {
            target.addComponent(BorderLayout.NORTH, dialogTitle);
        }
        revalidate();
    }

    /// {@inheritDoc}
    @Override
    public Label getTitleComponent() {
        return dialogTitle;
    }

    /// {@inheritDoc}
    @Override
    public void setTitleComponent(Label title) {
        Container parent = dialogTitle.getParent();
        if (parent != null) {
            parent.removeComponent(dialogTitle);
        }
        dialogTitle = title;
        updateTitleLayout();
    }

    /// Returns whether this dialog places its title in the absolute center with
    /// the body below it.
    ///
    /// #### Returns
    ///
    /// true when the centered title layout is active
    public boolean isTitleCentered() {
        return titleCentered;
    }

    /// Places the title in the absolute center with the body below it. Passing
    /// false restores the traditional title-at-top layout.
    ///
    /// #### Parameters
    ///
    /// - `titleCentered`: true to use the centered title layout
    public void setTitleCentered(boolean titleCentered) {
        if (this.titleCentered == titleCentered) {
            return;
        }
        this.titleCentered = titleCentered;
        updateTitleLayout();
        revalidate();
    }

    private void updateTitleLayout() {
        Container root = super.getContentPane();
        if (dialogTitle.getParent() != null) {
            dialogTitle.remove();
        }
        if (dialogContentPane.getParent() != null) {
            dialogContentPane.remove();
        }
        if (centeredTitleBody != null && centeredTitleBody.getParent() != null) {
            centeredTitleBody.remove();
        }
        if (titleCentered) {
            if (centeredTitleBody == null) {
                centeredTitleBody = new Container(new BorderLayout());
                centeredTitleBody.setUIID("Container");
                centeredTitleArea = new Container(new BorderLayout(BorderLayout.CENTER_BEHAVIOR_CENTER_ABSOLUTE));
            }
            centeredTitleArea.setUIID(getUIManager().getThemeConstant(
                    "dlgCenteredTitleUIID", "Container"));
            centeredTitleArea.removeAll();
            centeredTitleBody.removeAll();
            centeredTitleArea.addComponent(BorderLayout.CENTER, dialogTitle);
            centeredTitleBody.addComponent(BorderLayout.CENTER, centeredTitleArea);
            centeredTitleBody.addComponent(BorderLayout.SOUTH, dialogContentPane);
            root.addComponent(BorderLayout.CENTER, centeredTitleBody);
        } else {
            root.addComponent(BorderLayout.NORTH, dialogTitle);
            root.addComponent(BorderLayout.CENTER, dialogContentPane);
        }
    }

    /// {@inheritDoc}
    @Override
    public Style getTitleStyle() {
        return dialogTitle.getStyle();
    }

    @Override
    void updateIcsIconCommandBehavior() {
        // don't set the app icon to the dialog title
    }

    /// Returns the container that actually implements the dialog positioning.
    /// This container is normally not accessible via the Codename One API.
    ///
    /// #### Returns
    ///
    /// internal dialog container useful for various calculations.
    public Container getDialogComponent() {
        return super.getContentPane();
    }

    /// {@inheritDoc}
    @Override
    public void setTitleComponent(Label title, Transition t) {
        Container parent = dialogTitle.getParent();
        if (parent != null) {
            parent.replace(dialogTitle, title, t);
        }
        dialogTitle = title;
    }

    /// Returns the uiid of the dialog
    ///
    /// #### Returns
    ///
    /// the uiid of the dialog
    public String getDialogUIID() {
        return super.getContentPane().getUIID();
    }

    /// Simple setter to set the Dialog uiid
    ///
    /// #### Parameters
    ///
    /// - `uiid`: the id for the dialog
    public void setDialogUIID(String uiid) {
        super.getContentPane().setUIID(uiid);
    }

    /// Simple getter to get the Dialog Style
    ///
    /// #### Returns
    ///
    /// the style of the dialog
    public Style getDialogStyle() {
        return super.getContentPane().getUnselectedStyle();
    }

    /// Simple setter to set the Dialog Style
    ///
    /// #### Parameters
    ///
    /// - `style`
    public void setDialogStyle(Style style) {
        super.getContentPane().setUnselectedStyle(style);
    }

    /// Initialize the default transition for the dialogs overriding the forms
    /// transition
    ///
    /// #### Parameters
    ///
    /// - `uim`: the UIManager instance
    @Override
    protected void initLaf(UIManager uim) {
        super.initLaf(uim);
        setTransitionOutAnimator(uim.getLookAndFeel().getDefaultDialogTransitionOut());
        setTransitionInAnimator(uim.getLookAndFeel().getDefaultDialogTransitionIn());
    }

    /// This method shows the form as a modal alert allowing us to produce a behavior
    /// of an alert/dialog box. This method will block the calling thread even if the
    /// calling thread is the EDT. Notice that this method will not release the block
    /// until dispose is called even if show() from another form is called!
    ///
    /// Modal dialogs Allow the forms "content" to "hang in mid air" this is especially useful for
    /// dialogs where you would want the underlying form to "peek" from behind the
    /// form.
    ///
    /// #### Parameters
    ///
    /// - `top`: space in pixels between the top of the screen and the form
    ///
    /// - `bottom`: space in pixels between the bottom of the screen and the form
    ///
    /// - `left`: space in pixels between the left of the screen and the form
    ///
    /// - `right`: space in pixels between the right of the screen and the form
    ///
    /// - `includeTitle`: @param includeTitle whether the title should hang in the top of the screen or
    /// be glued onto the content pane
    ///
    /// #### Returns
    ///
    /// the last command pressed by the user if such a command exists
    ///
    /// #### Deprecated
    ///
    /// @deprecated use the version that doesn't accept the include title, the includeTitle
    /// feature is no longer supported
    public Command show(int top, int bottom, int left, int right, boolean includeTitle) {
        return show(top, bottom, left, right, includeTitle, true);
    }

    /// This method shows the form as a modal alert allowing us to produce a behavior
    /// of an alert/dialog box. This method will block the calling thread even if the
    /// calling thread is the EDT. Notice that this method will not release the block
    /// until dispose is called even if show() from another form is called!
    ///
    /// Modal dialogs Allow the forms "content" to "hang in mid air" this is especially useful for
    /// dialogs where you would want the underlying form to "peek" from behind the
    /// form.
    ///
    /// See this sample for showing a dialog at the bottom of the screen:
    /// ```java
    /// Dialog dlg = new Dialog("At Bottom");
    /// dlg.setLayout(new BorderLayout());
    /// // span label accepts the text and the UIID for the dialog body
    /// dlg.add(new SpanLabel("Dialog Body text", "DialogBody"));
    /// int h = Display.getInstance().getDisplayHeight();
    /// dlg.setDisposeWhenPointerOutOfBounds(true);
    /// dlg.show(h /8 * 7, 0, 0, 0);
    /// ```
    ///
    /// #### Parameters
    ///
    /// - `top`: space in pixels between the top of the screen and the form
    ///
    /// - `bottom`: space in pixels between the bottom of the screen and the form
    ///
    /// - `left`: space in pixels between the left of the screen and the form
    ///
    /// - `right`: space in pixels between the right of the screen and the form
    ///
    /// #### Returns
    ///
    /// the last command pressed by the user if such a command exists
    public Command show(int top, int bottom, int left, int right) {
        return show(top, bottom, left, right, false, true);
    }

    /// This method shows the form as a modal alert allowing us to produce a behavior
    /// of an alert/dialog box. This method will block the calling thread even if the
    /// calling thread is the EDT. Notice that this method will not release the block
    /// until dispose is called even if show() from another form is called!
    ///
    /// Modal dialogs Allow the forms "content" to "hang in mid air" this is especially useful for
    /// dialogs where you would want the underlying form to "peek" from behind the
    /// form.
    ///
    /// #### Parameters
    ///
    /// - `top`: space in pixels between the top of the screen and the form
    ///
    /// - `bottom`: space in pixels between the bottom of the screen and the form
    ///
    /// - `left`: space in pixels between the left of the screen and the form
    ///
    /// - `right`: space in pixels between the right of the screen and the form
    ///
    /// - `includeTitle`: @param includeTitle whether the title should hang in the top of the screen or
    /// be glued onto the content pane
    ///
    /// - `modal`: @param modal        indicates the dialog should be modal set to false for modeless dialog
    /// which is useful for some use cases
    ///
    /// #### Returns
    ///
    /// the last command pressed by the user if such a command exists
    ///
    /// #### Deprecated
    ///
    /// use showAtPosition, the includeTitle flag is no longer supported
    public Command show(int top, int bottom, int left, int right, boolean includeTitle, boolean modal) {
        this.top = top;
        this.bottom = bottom;
        if (isRTL()) {
            this.left = right;
            this.right = left;
        } else {
            this.left = left;
            this.right = right;
        }
        //this.includeTitle = includeTitle;
        setDisposed(false);
        this.modal = modal;
        lastCommandPressed = null;
        showModal(this.top, this.bottom, this.left, this.right, includeTitle, modal, false);
        return lastCommandPressed;
    }

    /// This method shows the form as a modal alert allowing us to produce a behavior
    /// of an alert/dialog box. This method will block the calling thread even if the
    /// calling thread is the EDT. Notice that this method will not release the block
    /// until dispose is called even if show() from another form is called!
    ///
    /// Modal dialogs Allow the forms "content" to "hang in mid air" this is especially useful for
    /// dialogs where you would want the underlying form to "peek" from behind the
    /// form.
    ///
    /// #### Parameters
    ///
    /// - `top`: space in pixels between the top of the screen and the form
    ///
    /// - `bottom`: space in pixels between the bottom of the screen and the form
    ///
    /// - `left`: space in pixels between the left of the screen and the form
    ///
    /// - `right`: space in pixels between the right of the screen and the form
    ///
    /// - `modal`: @param modal  indicates the dialog should be modal set to false for modeless dialog
    /// which is useful for some use cases
    ///
    /// #### Returns
    ///
    /// the last command pressed by the user if such a command exists
    public Command showAtPosition(int top, int bottom, int left, int right, boolean modal) {
        this.top = top;
        this.bottom = bottom;
        if (isRTL()) {
            this.left = right;
            this.right = left;
        } else {
            this.left = left;
            this.right = right;
        }
        //this.includeTitle = includeTitle;
        setDisposed(false);
        this.modal = modal;
        lastCommandPressed = null;
        showModal(this.top, this.bottom, this.left, this.right, false, modal, false);
        return lastCommandPressed;
    }

    /// Disable title bar status for iOS 7 which breaks dialogs
    @Override
    void initTitleBarStatus() {
    }

    /// Indicates the time (in milliseconds) afterwhich the dialog will be disposed
    /// implicitly
    ///
    /// #### Parameters
    ///
    /// - `time`: a milliseconds time used to dispose the dialog
    @Override
    public void setTimeout(long time) {
        this.time = System.currentTimeMillis() + time;
        super.registerAnimatedInternal(this);
    }

    /// {@inheritDoc}
    @Override
    public void setTransitions(Transition transition) {
        setTransitionInAnimator(transition);
        setTransitionOutAnimator(transition);
    }

    /// {@inheritDoc}
    @Override
    public void configureCommands(Command[] cmds, boolean commandsAsButtons) {
        if (cmds == null) {
            return;
        }
        if (commandsAsButtons) {
            placeButtonCommands(cmds);
        } else {
            for (Command cmd : cmds) {
                addCommand(cmd);
            }
        }
        if (cmds.length == 1 || cmds.length == 2) {
            setBackCommand(cmds[0]);
        }
    }

    /// {@inheritDoc}
    @Override
    void sizeChangedInternal(int w, int h) {
        if (disposeOnRotation) {
            disposedDueToRotation = true;
            dispose();
            Form frm = getPreviousForm();
            if (frm != null) {
                frm.sizeChangedInternal(w, h);
            }
            return;
        }
        autoAdjust(w, h);
        super.sizeChangedInternal(w, h);
        Form frm = getPreviousForm();
        if (frm != null) {
            frm.sizeChangedInternal(w, h);
        }
    }

    /// Auto adjust size of the dialog.
    /// This method is triggered from a sizeChanged event.
    ///
    /// #### Parameters
    ///
    /// - `w`: width of the screen
    ///
    /// - `h`: height of the screen
    protected void autoAdjust(int w, int h) {
        if (autoAdjustDialogSize) {
            growOrShrinkImpl(w, h);
        }
    }

    private void addButtonBar(Container c) {
        super.getContentPane().addComponent(BorderLayout.SOUTH, c);
    }

    /// Places the given commands in the dialog command area, this is very useful for touch devices.
    ///
    /// #### Parameters
    ///
    /// - `cmds`: the commands to place
    ///
    /// #### Deprecated
    ///
    /// this method shouldn't be invoked externally, it should have been private
    public void placeButtonCommands(Command[] cmds) {
        buttonCommands = cmds;
        Container buttonArea;
        boolean commandGrid = getUIManager().isThemeConstant("dlgCommandGridBool", false);
        if (commandGrid) {
            buttonArea = new Container(new GridLayout(1, cmds.length));
        } else {
            buttonArea = new Container(new FlowLayout(CENTER));
        }
        buttonArea.setUIID("DialogCommandArea");
        if (commandGrid) {
            // Native dialog actions are card chrome rather than inset body
            // content. Keep the theme's top spacing, but let the grid and its
            // separators meet the left, right, and bottom card edges.
            super.getContentPane().getAllStyles().setPadding(0, 0, 0, 0);
            Style commandAreaStyle = buttonArea.getAllStyles();
            commandAreaStyle.setPadding(LEFT, 0);
            commandAreaStyle.setPadding(RIGHT, 0);
            commandAreaStyle.setPadding(BOTTOM, 0);
        }
        String uiid = getUIManager().getThemeConstant("dlgButtonCommandUIID", null);
        addButtonBar(buttonArea);
        if (cmds.length > 0) {
            String lineColor = getUIManager().getThemeConstant(
                    Boolean.TRUE.equals(Display.getInstance().isDarkMode())
                            ? "dlgInvisibleButtonsDark" : "dlgInvisibleButtons",
                    getUIManager().getThemeConstant("dlgInvisibleButtons", null));
            if (cmds.length > 3) {
                lineColor = null;
            }
            int largest = Integer.parseInt(getUIManager().getThemeConstant("dlgCommandButtonSizeInt", "0"));
            for (int iter = 0; iter < cmds.length; iter++) {
                Button b = new Button(cmds[iter]);
                if (uiid != null) {
                    b.setUIID(uiid);
                }

                // special case for dialog butons uppercase on Android
                if (Button.isCapsTextDefault()) {
                    b.setCapsText(true);
                }

                largest = Math.max(b.getPreferredW(), largest);
                if (lineColor != null && lineColor.length() > 0) {
                    int color = Integer.parseInt(lineColor, 16);
                    Border brd = null;
                    if (iter < cmds.length - 1) {
                        brd = Border.createCompoundBorder(Border.createLineBorder(1, color), null, null, Border.createLineBorder(1, color));
                    } else {
                        brd = Border.createCompoundBorder(Border.createLineBorder(1, color), null, null, null);
                    }
                    b.getUnselectedStyle().setBorder(brd);
                    b.getSelectedStyle().setBorder(brd);
                    b.getPressedStyle().setBorder(brd);
                }
                buttonArea.addComponent(b);

            }
            for (int iter = 0; iter < cmds.length; iter++) {
                buttonArea.getComponentAt(iter).setPreferredW(largest);
            }
            buttonArea.getComponentAt(0).requestFocus();
        }
    }

    /// {@inheritDoc}
    @Override
    public void keyReleased(int keyCode) {
        if (commandsAsButtons) {
            if (MenuBar.isLSK(keyCode)) {
                if (buttonCommands != null && buttonCommands.length > 0) {
                    dispatchCommand(buttonCommands[0], new ActionEvent(buttonCommands[0], ActionEvent.Type.KeyRelease));
                    return;
                }
            }
            if (MenuBar.isRSK(keyCode)) {
                if (buttonCommands != null && buttonCommands.length > 1) {
                    dispatchCommand(buttonCommands[1], new ActionEvent(buttonCommands[1], ActionEvent.Type.KeyRelease));
                    return;
                }
            }
        }
        super.keyReleased(keyCode);
    }

    /// {@inheritDoc}
    @Override
    protected void onShow() {
        if (dialogType > 0) {
            Display.getInstance().playDialogSound(dialogType);
        }
    }

    @Override
    void onShowCompletedImpl() {
        pressedOutOfBounds = false;
        disposedDueToRotation = false;
        setLightweightMode(false);
        onShowCompleted();
        if (isDisposed()) {
            disposeImpl();
        }
        if (showListener != null) {
            showListener.fireActionEvent(new ActionEvent(this, ActionEvent.Type.Show));
        }
    }

    /// {@inheritDoc}
    @Override
    public void showBack() {
        showImpl(true);
    }

    /// {@inheritDoc}
    @Override
    public void setScrollable(boolean scrollable) {
        getContentPane().setScrollable(scrollable);
    }

    /// The default version of show modal shows the dialog occupying the center portion
    /// of the screen.
    @Override
    public void show() {
        showImpl(false);
    }

    /// The default version of show modal shows the dialog occupying the center portion
    /// of the screen.
    private void showImpl(boolean reverse) {
        if (modal && Display.isInitialized() && Display.getInstance().isMinimized()) {
            Log.p("Modal dialogs cannot be displayed on a minimized app");
            return;
        }
        // this behavior allows a use case where dialogs of various sizes are layered
        // one on top of the other
        setDisposed(false);
        if (top > -1) {
            show(top, bottom, left, right, includeTitle, modal);
        } else {
            if (modal) {
                if (getDialogPosition() == null) {
                    super.showModal(reverse);
                } else {
                    showPacked(getDialogPosition(), true);
                }
            } else {
                showModeless();
            }
        }
    }

    /// Shows a modeless dialog which is useful for some simpler use cases such as
    /// progress indication etc...
    public void showModeless() {
        // this behavior allows a use case where dialogs of various sizes are layered
        // one on top of the other
        modal = false;
        setDisposed(false);
        if (top > -1) {
            show(top, bottom, left, right, includeTitle, false);
        } else {
            if (getDialogPosition() == null) {
                showDialog(false, false);
            } else {
                showPacked(getDialogPosition(), false);
            }
        }
    }

    /// The top level this dialog appears on when it is shown.
    ///
    /// A dialog is not attached to anything at the moment it is shown, so it cannot
    /// resolve its own host the way an attached component can. Left unset it uses the
    /// focused window, else the current `Form` -- which is the historical behaviour and
    /// the right answer for an application with one window. Set it to put the dialog on
    /// a particular `com.codename1.ui.Window` instead.
    ///
    /// #### Parameters
    ///
    /// - `host`: the top level to show on, or null to work it out
    public void setTopLevelHost(TopLevelContainer host) {
        this.hostTopLevel = host;
        // An explicit choice replaces an inferred one outright, and there is no longer
        // an earlier host worth restoring.
        this.hostTopLevelInferred = false;
        this.hostTopLevelBeforeInference = null;
    }

    /// Returns the top level set with `#setTopLevelHost(TopLevelContainer)`.
    ///
    /// #### Returns
    ///
    /// the explicit host, or null when none was set
    public TopLevelContainer getTopLevelHost() {
        return hostTopLevel;
    }

    /// The top level to show on: the explicit host, else the one this dialog is already
    /// attached to, else the focused window, else the current form.
    ///
    /// The middle rung is guarded on actually being parented. A `Dialog` is a `Form`,
    /// so `getTopLevelContainer()` answers `this` for an unattached one -- asking
    /// unguarded would make every dialog its own host.
    ///
    /// #### Returns
    ///
    /// the host top level, or null when there is none
    TopLevelContainer resolveHost() {
        if (hostTopLevel != null) {
            return hostTopLevel;
        }
        if (getParent() != null) {
            TopLevelContainer attached = getTopLevelContainer();
            if (attached != null && attached != this) { //NOPMD CompareObjectsWithEquals
                return attached;
            }
        }
        return TopLevelSupport.current();
    }

    /// Records a host worked out from a popup's anchor, remembering whatever was set
    /// before so the inference does not outlive the popup.
    ///
    /// #### Parameters
    ///
    /// - `inferred`: the anchor's top level
    private void inferHost(TopLevelContainer inferred) {
        if (inferred == null || inferred == this) { //NOPMD CompareObjectsWithEquals
            return;
        }
        if (!hostTopLevelInferred) {
            hostTopLevelBeforeInference = hostTopLevel;
        }
        hostTopLevel = inferred;
        hostTopLevelInferred = true;
    }

    /// Drops a host inferred from a popup's anchor and restores whatever was set before.
    private void releaseInferredHost() {
        if (hostTopLevelInferred) {
            hostTopLevel = hostTopLevelBeforeInference;
            hostTopLevelBeforeInference = null;
            hostTopLevelInferred = false;
        }
    }

    /// The container this dialog measures itself against, or null when the main surface
    /// is the measure.
    ///
    /// Returning null rather than the `Form` is deliberate: the historical path then
    /// evaluates the exact expression it always did, so nothing about a single window
    /// application can shift by a pixel.
    ///
    /// #### Returns
    ///
    /// the window to measure against, or null for the display
    private Container hostBounds() {
        TopLevelContainer h = resolveHost();
        return h != null && h.asContainer().isNativeWindow() ? h.asContainer() : null;
    }

    /// The width to size against: the host window's, or the display's.
    ///
    /// #### Returns
    ///
    /// the width in pixels
    private int hostWidth() {
        Container c = hostBounds();
        return c == null ? Display.getInstance().getDisplayWidth() : c.getWidth();
    }

    /// The height to size against: the host window's, or the display's.
    ///
    /// #### Returns
    ///
    /// the height in pixels
    private int hostHeight() {
        Container c = hostBounds();
        return c == null ? Display.getInstance().getDisplayHeight() : c.getHeight();
    }

    /// Whether a popup should be laid out as though the surface were portrait.
    ///
    /// A window has no orientation, so its own shape is all there is to go on.
    ///
    /// #### Returns
    ///
    /// true to use the portrait placement
    private boolean hostPrefersPortrait() {
        boolean bias = Display.getInstance().isPortrait();
        TopLevelContainer h = resolveHost();
        return h == null ? bias : h.asContainer().prefersPortraitLayout(bias);
    }

    /// {@inheritDoc}
    ///
    /// A dialog in a window's layered pane has no previous form to return to and was
    /// never handed to `Display#setCurrent(Form)`, so none of the base teardown applies
    /// to it. It comes back out of the layer instead.
    @Override
    void disposeImpl() {
        if (layerHost != null) {
            disposeFromHostLayer();
            return;
        }
        super.disposeImpl();
    }

    /// {@inheritDoc}
    ///
    /// Overridden rather than editing `Form`, because the base version measures the
    /// display before it works out the margins and would centre a window hosted dialog
    /// in the wrong coordinate space -- on a window smaller than the display the
    /// margins could exceed the host outright and leave the dialog clipped or off
    /// screen. With no window host this delegates, so the historical path evaluates
    /// exactly the expression it always did.
    @Override
    void showDialog(boolean modal, boolean reverse) {
        if (hostBounds() == null) {
            super.showDialog(modal, reverse);
            return;
        }
        int h = hostHeight() - getMenuBar().getPreferredH() - super.getTitleComponent().getPreferredH();
        int w = hostWidth();
        showModal(h / 100 * 20, h / 100 * 10, w / 100 * 20, w / 100 * 20, true, modal, reverse);
    }

    /// Whether this showing goes into a window's layered pane rather than taking over
    /// the main surface.
    ///
    /// False for everything that has no window to go on, which is every mobile port and
    /// every desktop application that never opened one -- so the historical path is
    /// reached by the same code it always was.
    ///
    /// #### Returns
    ///
    /// true to show in the host's layered pane
    private boolean usesHostLayer() {
        if (!Desktop.isSupported()) {
            return false;
        }
        // A menu is framework furniture that Display.getCurrent() deliberately looks
        // through, and Dialog.dispose() skips super.dispose() for it. A menu can only
        // come from a Form's MenuBar in the first place, so this never costs anything.
        if (isMenu()) {
            return false;
        }
        return hostBounds() != null;
    }

    /// Shows this dialog inside its host window's layered pane and, when modal, waits
    /// there until it is disposed.
    ///
    /// This is the whole of the window path. It replaces `Form#showModal` rather than
    /// extending it: there is no previous form to swap out, nothing to hand to
    /// `Display#setCurrent(Form)`, and no `RunnableWrapper` -- the dialog is simply a
    /// component in a layer that spans the window.
    ///
    /// #### Parameters
    ///
    /// - `top`: space in pixels above the dialog
    ///
    /// - `bottom`: space in pixels below the dialog
    ///
    /// - `left`: space in pixels left of the dialog
    ///
    /// - `right`: space in pixels right of the dialog
    ///
    /// - `includeTitle`: whether the title hangs at the top or is glued to the content
    ///
    /// - `modal`: whether to wait here until the dialog is disposed
    private void showInHostLayer(int top, int bottom, int left, int right,
            boolean includeTitle, boolean modal) {
        Display.getInstance().flushEdt();
        Window host = (Window) resolveHost();
        layerHost = host;
        hostWasPortrait = host.getHeight() >= host.getWidth();
        Container layer = host.getFormLayeredPane(Dialog.class, true);
        if (!(layer.getLayout() instanceof LayeredLayout)) {
            layer.setLayout(new LayeredLayout());
        }
        activeLayer = layer;

        // Before anything is added to the layer. The layer repaints the whole window
        // beneath its contents, so capturing afterwards would recurse through that
        // backdrop and photograph the dialog being set up.
        Image backdrop = captureBlurBackdrop(host);

        savedBgPainter = getStyle().getBgPainter();
        getStyle().setBgPainter(NO_OP_PAINTER);

        if (modal || disposeWhenPointerOutOfBounds) {
            scrim = new DialogScrim(this, modal, getTintColor(), backdrop);
            layer.addComponent(scrim);
        }

        applyDialogMargins(top, bottom, left, right, includeTitle);
        if (getTransitionOutAnimator() == null && getTransitionInAnimator() == null) {
            initLaf(getUIManager());
        }
        layer.addComponent(this);
        host.revalidateWithAnimationSafety();

        hostSizeListener = new HostSizeListener(this);
        host.addSizeChangedListener(hostSizeListener);
        hostBackListener = new HostBackListener(this);
        host.addKeyListener(MenuBar.backSK, hostBackListener);
        focusFirstFocusable(host);

        onShow();
        onShowCompletedImpl();

        if (modal) {
            while (!isDisposed()) {
                CN.invokeAndBlock(BLOCKING_SLEEP);
            }
            Display.getInstance().setShowVirtualKeyboard(false);
        }
    }

    /// The blurred snapshot of the host to sit behind the dialog, or null when this
    /// dialog does not blur its background.
    ///
    /// #### Parameters
    ///
    /// - `host`: the window being covered
    ///
    /// #### Returns
    ///
    /// the blurred image, or null
    private Image captureBlurBackdrop(Window host) {
        float radius = getBlurBackgroundRadius();
        if (radius <= 0 || host.getWidth() <= 0 || host.getHeight() <= 0) {
            return null;
        }
        Image shot = host.capture();
        if (shot == null) {
            shot = Image.createImage(host.getWidth(), host.getHeight());
            host.paintComponent(shot.getGraphics(), true);
        }
        return Display.getInstance().gaussianBlurImage(shot, radius);
    }

    /// Gives focus to the first thing in the dialog that can take it.
    ///
    /// `Form#initFocused()` cannot be used here: focus belongs to the window, not to
    /// the dialog, so the dialog has to hand its own first focusable to the host.
    ///
    /// #### Parameters
    ///
    /// - `host`: the window that owns focus
    private void focusFirstFocusable(Window host) {
        Component first = findFirstFocusable(getDialogComponent());
        if (first != null) {
            host.setFocused(first);
        }
    }

    /// Depth first search for something focusable.
    ///
    /// #### Parameters
    ///
    /// - `c`: the component to search from
    ///
    /// #### Returns
    ///
    /// the first focusable component, or null
    private Component findFirstFocusable(Component c) {
        if (c == null) {
            return null;
        }
        if (c.isFocusable() && c.isVisible() && c.isEnabled()) {
            return c;
        }
        if (c instanceof Container) {
            Container cnt = (Container) c;
            int count = cnt.getComponentCount();
            for (int i = 0; i < count; i++) {
                Component found = findFirstFocusable(cnt.getComponentAt(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /// A press landed on the backdrop rather than on the dialog.
    ///
    /// #### Parameters
    ///
    /// - `x`: the press x
    ///
    /// - `y`: the press y
    void scrimPressed(int x, int y) {
        pressedOutOfBounds = !getTitleComponent().containsOrOwns(x, y)
                && !getContentPane().containsOrOwns(x, y)
                && !getMenuBar().containsOrOwns(x, y);
    }

    /// A release landed on the backdrop rather than on the dialog.
    ///
    /// #### Parameters
    ///
    /// - `x`: the release x
    ///
    /// - `y`: the release y
    void scrimReleased(int x, int y) {
        if (disposeWhenPointerOutOfBounds && pressedOutOfBounds
                && !getTitleComponent().containsOrOwns(x, y)
                && !getContentPane().containsOrOwns(x, y)
                && !getMenuBar().containsOrOwns(x, y)) {
            dispose();
        }
    }

    /// The host window changed size while this dialog was on it.
    ///
    /// Stands in for `#sizeChangedInternal(int, int)`, which the window never sends to
    /// a dialog in its layered pane. A window has no orientation, so `disposeOnRotation`
    /// fires on the host's shape actually flipping rather than on any resize at all --
    /// otherwise dragging a window wider would close a popup.
    ///
    /// #### Parameters
    ///
    /// - `w`: the host's new width
    ///
    /// - `h`: the host's new height
    void hostResized(int w, int h) {
        boolean portrait = h >= w;
        if (disposeOnRotation && portrait != hostWasPortrait) {
            hostWasPortrait = portrait;
            disposedDueToRotation = true;
            dispose();
            return;
        }
        hostWasPortrait = portrait;
        autoAdjust(w, h);
        revalidate();
    }

    /// The back key was pressed while this dialog was on a window.
    ///
    /// A window has no menu bar to map the key to a command, so the dialog does it.
    void hostBackPressed() {
        Command back = getBackCommand();
        if (back != null) {
            dispatchCommand(back, new ActionEvent(back, ActionEvent.Type.Command));
            return;
        }
        dispose();
    }

    /// Takes this dialog back out of its host window's layered pane.
    ///
    /// The counterpart to `#showInHostLayer(int, int, int, int, boolean, boolean)`, and
    /// the reason `#disposeImpl()` is overridden rather than `#dispose()`: `MenuBar`
    /// calls disposeImpl directly, and a Dialog skips super.dispose() when it is a menu.
    private void disposeFromHostLayer() {
        Window host = layerHost;
        layerHost = null;
        if (host != null) {
            if (hostSizeListener != null) {
                host.removeSizeChangedListener(hostSizeListener);
                hostSizeListener = null;
            }
            if (hostBackListener != null) {
                host.removeKeyListener(MenuBar.backSK, hostBackListener);
                hostBackListener = null;
            }
        }
        if (scrim != null) {
            scrim.remove();
            scrim = null;
        }
        remove();
        if (savedBgPainter != null) {
            getStyle().setBgPainter(savedBgPainter);
            savedBgPainter = null;
        }
        Container layer = activeLayer;
        activeLayer = null;
        if (layer != null && layer.getComponentCount() == 0) {
            layer.remove();
        }
        if (host != null) {
            // Without this the pixels the dialog occupied are left on screen: removing
            // a component from a layer does not by itself wake the window's paint.
            host.revalidateWithAnimationSafety();
        }
    }

    @Override
    void showModal(int top, int bottom, int left, int right, boolean includeTitle, boolean modal, boolean reverse) {
        if (Display.isInitialized() && Display.getInstance().isMinimized()) {
            Log.p("Modal dialogs cannot be displayed on a minimized app");
            return;
        }
        this.top = top;
        this.bottom = bottom;
        this.left = left;
        this.right = right;
        if (usesHostLayer()) {
            showInHostLayer(top, bottom, left, right, includeTitle, modal);
            return;
        }

        // hide the title if no text is there to allow the styles of the dialog title to disappear
        if (dialogTitle != null && getUIManager().isThemeConstant("hideEmptyTitleBool", false)) {
            boolean b = dialogTitle.getText().length() > 0;
            getTitleArea().setVisible(b);
            getTitleComponent().setVisible(b);
        }
        super.showModal(top, bottom, left, right, includeTitle, modal, reverse);
    }

    /// A popup dialog is shown with the context of a component and  its selection, it is disposed seamlessly if the back button is pressed
    /// or if the user touches outside its bounds. It can optionally provide an arrow in the theme to point at the context component. The popup
    /// dialog has the PopupDialog style by default.
    ///
    /// #### Parameters
    ///
    /// - `c`: the context component which is used to position the dialog and can also be pointed at
    ///
    /// #### Returns
    ///
    /// the command that might have been triggered by the user within the dialog if commands are placed in the dialog
    public Command showPopupDialog(Component c) {
        Rectangle componentPos = c.getSelectedRect();
        componentPos.setX(componentPos.getX() - c.getScrollX());
        componentPos.setY(componentPos.getY() - c.getScrollY());

        // The rectangle above is in the anchor's coordinate space, so the popup has to
        // appear on the anchor's own top level -- there is nowhere else that rectangle
        // means anything. This overrides a host set earlier rather than deferring to
        // it, and is put back when the popup goes.
        inferHost(c.getTopLevelContainer());
        return showPopupDialog(componentPos);
    }

    /// A popup dialog is shown with the context of a component and  its selection, it is disposed seamlessly if the back button is pressed
    /// or if the user touches outside its bounds. It can optionally provide an arrow in the theme to point at the context component. The popup
    /// dialog has the PopupDialog style by default.
    ///
    /// #### Parameters
    ///
    /// - `rect`: the screen rectangle to which the popup should point
    ///
    /// #### Returns
    ///
    /// the command that might have been triggered by the user within the dialog if commands are placed in the dialog
    public Command showPopupDialog(Rectangle rect) {
        if ("Dialog".equals(getDialogUIID())) {
            setDialogUIID("PopupDialog");
            if ("DialogTitle".equals(getTitleComponent().getUIID())) {
                getTitleComponent().setUIID("PopupDialogTitle");
            }
            getContentPane().setUIID("PopupContentPane");
        }

        disposeOnRotation = true;
        disposeWhenPointerOutOfBounds = true;
        Command backCommand = null;
        if (getBackCommand() == null) {
            backCommand = new Command("Back");
            setBackCommand(backCommand);
        }

        Component contentPane = super.getContentPane();
        Label title = super.getTitleComponent();

        int menuHeight = calcMenuHeight();
        UIManager manager = getUIManager();

        // hide the title if no text is there to allow the styles of the dialog title to disappear, we need this code here since otherwise the
        // preferred size logic of the dialog won't work with large title borders
        if (dialogTitle != null && manager.isThemeConstant("hideEmptyTitleBool", false)) {
            boolean b = getTitle().length() > 0;
            getTitleArea().setVisible(b);
            getTitleComponent().setVisible(b);
            if (!b && manager.isThemeConstant("shrinkPopupTitleBool", true)) {
                getTitleComponent().setPreferredSize(new Dimension(0, 0));
                getTitleComponent().getStyle().setBorder(null);
                getTitleArea().setPreferredSize(new Dimension(0, 0));
                if (getContentPane().getClientProperty("$ENLARGED_POP") == null) {
                    getContentPane().putClientProperty("$ENLARGED_POP", Boolean.TRUE);
                    int cpPaddingTop = getContentPane().getStyle().getPaddingTop();
                    int titlePT = getTitleComponent().getStyle().getPaddingTop();
                    byte[] pu = getContentPane().getStyle().getPaddingUnit();
                    if (pu == null) {
                        pu = new byte[4];
                    }
                    pu[0] = Style.UNIT_TYPE_PIXELS;
                    getContentPane().getStyle().setPaddingUnit(pu);
                    int pop = Display.getInstance().convertToPixels(manager.getThemeConstant("popupNoTitleAddPaddingInt", 1), false);
                    getContentPane().getStyle().setPadding(TOP, pop + cpPaddingTop + titlePT);
                }
            }
        }

        // allows a text area to recalculate its preferred size if embedded within a dialog
        revalidate();

        Style contentPaneStyle = getDialogStyle();

        boolean restoreArrow = false;
        if (manager.isThemeConstant(getDialogUIID() + "ArrowBool", false)) {
            Image t = manager.getThemeImageConstant(getDialogUIID() + "ArrowTopImage");
            Image b = manager.getThemeImageConstant(getDialogUIID() + "ArrowBottomImage");
            Image l = manager.getThemeImageConstant(getDialogUIID() + "ArrowLeftImage");
            Image r = manager.getThemeImageConstant(getDialogUIID() + "ArrowRightImage");
            Border border = contentPaneStyle.getBorder();
            if (border != null) {
                border.setImageBorderSpecialTile(t, b, l, r, rect);
                restoreArrow = true;
            }
        } else {
            Border border = contentPaneStyle.getBorder();
            if (border != null) {
                border.setTrackComponent(rect);
            }
        }
        int prefHeight = contentPane.getPreferredH();
        int prefWidth = contentPane.getPreferredW();
        if (contentPaneStyle.getBorder() != null) {
            prefWidth = Math.max(contentPaneStyle.getBorder().getMinimumWidth(), prefWidth);
            prefHeight = Math.max(contentPaneStyle.getBorder().getMinimumHeight(), prefHeight);
        }

        prefWidth += getUIManager().getLookAndFeel().getVerticalScrollWidth();

        int availableHeight = hostHeight() - menuHeight - title.getPreferredH();
        int availableWidth = hostWidth();
        int width = Math.min(availableWidth, prefWidth);
        int x = 0;
        int y = 0;
        Command result;

        boolean showPortrait;
        if (popupDirectionBiasPortrait != null) {
            showPortrait = popupDirectionBiasPortrait.booleanValue();
        } else {
            showPortrait = hostPrefersPortrait();
        }

        // if we don't have enough space then disregard device orientation
        if (showPortrait) {
            if (availableHeight < (availableWidth - rect.getWidth()) / 2) {
                showPortrait = false;
            }
        } else {
            if (availableHeight / 2 > availableWidth - rect.getWidth()) {
                showPortrait = true;
            }
        }
        if (showPortrait) {
            if (width < availableWidth) {
                int idealX = rect.getX() - width / 2 + rect.getSize().getWidth() / 2;

                // if the ideal position is less than 0 just use 0
                if (idealX > 0) {
                    // if the idealX is too far to the right just align to the right
                    if (idealX + width > availableWidth) {
                        x = availableWidth - width;
                    } else {
                        x = idealX;
                    }
                }
            }
            if (rect.getY() < availableHeight / 2) {
                // popup downwards
                if ("PopupDialog".equals(getDialogUIID()) && isUIIDByPopupPosition) {
                    getContentPane().setUIID("PopupContentPaneDownwards");
                }
                y = rect.getY() + rect.getSize().getHeight();
                int height = Math.min(prefHeight, availableHeight - y);
                result = show(y, availableHeight - height - y, x, availableWidth - width - x, true, true);
            } else {
                // popup upwards
                if ("PopupDialog".equals(getDialogUIID()) && isUIIDByPopupPosition) {
                    getContentPane().setUIID("PopupContentPaneUpwards");
                }
                int height = Math.min(prefHeight, availableHeight - (availableHeight - rect.getY()));
                y = rect.getY() - height;
                result = show(y, availableHeight - height - y, x, availableWidth - width - x, true, true);
            }
        } else {
            int height = Math.min(prefHeight, availableHeight);
            if (height < availableHeight) {
                int idealY = rect.getY() - height / 2 + rect.getSize().getHeight() / 2;

                // if the ideal position is less than 0 just use 0
                if (idealY > 0) {
                    // if the idealY is too far up just align to the top
                    if (idealY + height > availableHeight) {
                        y = availableHeight - height;
                    } else {
                        y = idealY;
                    }
                }
            }


            if (prefWidth > rect.getX()) {
                // popup right
                if ("PopupDialog".equals(getDialogUIID()) && isUIIDByPopupPosition) {
                    getContentPane().setUIID("PopupContentPaneRight");
                }
                x = rect.getX() + rect.getSize().getWidth();
                if (x + prefWidth > availableWidth) {
                    x = availableWidth - prefWidth;
                }

                width = Math.min(prefWidth, availableWidth - x);
                result = show(y, availableHeight - height - y, Math.max(0, x), Math.max(0, availableWidth - width - x), true, true);
            } else {
                // popup left
                if ("PopupDialog".equals(getDialogUIID()) && isUIIDByPopupPosition) {
                    getContentPane().setUIID("PopupContentPaneLeft");
                }
                width = Math.min(prefWidth, availableWidth - (availableWidth - rect.getX()));
                x = rect.getX() - width;
                result = show(y, availableHeight - height - y, Math.max(0, x), Math.max(0, availableWidth - width - x), true, true);
            }
        }

        if (restoreArrow) {
            contentPaneStyle.getBorder().clearImageBorderSpecialTile();
        }

        if (result == backCommand) { //NOPMD CompareObjectsWithEquals
            return null;
        }
        return result;
    }

    private int calcMenuHeight() {
        if (getSoftButtonCount() > 1) {
            Component menuBar = getSoftButton(0).getParent();
            Style menuStyle = menuBar.getStyle();
            return menuBar.getPreferredH() + menuStyle.getVerticalMargins();
        }
        return 0;
    }

    /// Convenience method to show a dialog sized to match its content.
    ///
    /// #### Parameters
    ///
    /// - `position`: one of the values from the BorderLayout class e.g. BorderLayout.CENTER, BorderLayout.NORTH etc.
    ///
    /// - `modal`: whether the dialog should be modal or modaless
    ///
    /// #### Returns
    ///
    /// the command selected if the dialog is modal and disposed via a command
    public Command showPacked(String position, boolean modal) {
        return showPackedImpl(position, modal, false);
    }

    /// Convenience method to show a dialog stretched to one of the sides
    ///
    /// #### Parameters
    ///
    /// - `position`: one of the values from the BorderLayout class except for center e.g. BorderLayout.NORTH, BorderLayout.EAST etc.
    ///
    /// - `modal`: whether the dialog should be modal or modaless
    ///
    /// #### Returns
    ///
    /// the command selected if the dialog is modal and disposed via a command
    public Command showStretched(String position, boolean modal) {
        return showPackedImpl(position, modal, true);
    }

    /// Convenience method to show a dialog stretched to one of the sides
    ///
    /// #### Parameters
    ///
    /// - `position`: one of the values from the BorderLayout class except for center e.g. BorderLayout.NORTH, BorderLayout.EAST etc.
    ///
    /// - `modal`: whether the dialog should be modal or modaless
    ///
    /// #### Returns
    ///
    /// the command selected if the dialog is modal and disposed via a command
    ///
    /// #### Deprecated
    ///
    /// due to typo use showStretched instead
    public Command showStetched(String position, boolean modal) {
        return showPackedImpl(position, modal, true);
    }

    /// Returns the preferred size of the dialog, this allows developers to position a dialog
    /// manually in arbitrary positions.
    ///
    /// #### Returns
    ///
    /// the preferred size of this dialog
    public Dimension getDialogPreferredSize() {
        Component contentPane = super.getContentPane();
        Style contentPaneStyle = getDialogStyle();
        int width = hostWidth();
        int prefHeight = contentPane.getPreferredH();
        int prefWidth = contentPane.getPreferredW();
        prefWidth = Math.min(prefWidth, width);
        if (contentPaneStyle.getBorder() != null) {
            prefWidth = Math.max(contentPaneStyle.getBorder().getMinimumWidth(), prefWidth);
            prefHeight = Math.max(contentPaneStyle.getBorder().getMinimumHeight(), prefHeight);
        }
        return new Dimension(prefWidth, prefHeight);
    }

    /// Convenience method to show a dialog sized to match its content.
    ///
    /// #### Parameters
    ///
    /// - `position`: one of the values from the BorderLayout class e.g. BorderLayout.CENTER, BorderLayout.NORTH etc.
    ///
    /// - `modal`: whether the dialog should be modal or modaless
    ///
    /// #### Returns
    ///
    /// the command selected if the dialog is modal and disposed via a command
    private Command showPackedImpl(String position, boolean modal, boolean stretch) {
        if (getTitle() == null) {
            setTitle("");
        }
        this.position = position;
        int height = hostHeight();
        int width = hostWidth();
        if (top > -1) {
            refreshTheme();
        }
        Component contentPane = super.getContentPane();
        if (dialogTitle != null && getUIManager().isThemeConstant("hideEmptyTitleBool", false)) {
            boolean b = getTitle().length() > 0;
            getTitleArea().setVisible(b);
            getTitleComponent().setVisible(b);
        }
        Style contentPaneStyle = contentPane.getStyle();

        revalidate();

        int prefWidth = contentPane.getPreferredW();
        prefWidth = Math.min(prefWidth, width);
        // Cap the packed dialog width so a long body wraps into a centered card
        // instead of stretching to a full-width strip on wide screens (tablet /
        // desktop / landscape). Two optional, theme-driven caps, both density-robust:
        //  - dialogMaxWidthPercentInt: a percentage of the screen width (the primary
        //    guard; behaves the same on any device, so an iOS-style ~72% alert reads
        //    correctly on a phone AND stays a card on a wide desktop).
        //  - dialogMaxWidthMMInt: an absolute millimetre cap that tightens it further
        //    on very wide low-density screens (NOTE: CN1's convertToPixels treats its
        //    unit as millimetres, so this is physical, not iOS-point, width).
        // Unset (0) keeps the legacy full-width behaviour.
        int origPrefWidth = prefWidth;
        int maxPct = getUIManager().getThemeConstant("dialogMaxWidthPercentInt", 0);
        if (maxPct > 0 && maxPct < 100) {
            prefWidth = Math.min(prefWidth, width * maxPct / 100);
        }
        int maxWidthMM = getUIManager().getThemeConstant("dialogMaxWidthMMInt", 0);
        if (maxWidthMM > 0) {
            int maxWidthPx = Display.getInstance().convertToPixels(maxWidthMM, true);
            if (maxWidthPx > 0) {
                prefWidth = Math.min(prefWidth, maxWidthPx);
            }
        }
        if (prefWidth < origPrefWidth) {
            // Re-measure at the capped width so the wrapped body reports its true
            // (taller) height. Merely invalidating preferred sizes is NOT enough:
            // a TextArea derives its preferred height from rows wrapped at its
            // CURRENT width, and the children still hold their stale (uncapped)
            // widths -- reporting the unwrapped, shorter height and clipping the
            // body behind the commands wherever the cap binds. Lay the content
            // out at the capped width first so nested text actually wraps.
            contentPane.setWidth(prefWidth);
            contentPane.setHeight(height);
            ((Container) contentPane).layoutContainer();
            contentPane.setShouldCalcPreferredSize(true);
        }
        int prefHeight = contentPane.getPreferredH();
        if (contentPaneStyle.getBorder() != null) {
            prefWidth = Math.max(contentPaneStyle.getBorder().getMinimumWidth(), prefWidth);
            prefHeight = Math.max(contentPaneStyle.getBorder().getMinimumHeight(), prefHeight);
        }
        int topBottom = Math.max(0, (height - prefHeight) / 2);
        int leftRight = Math.max(0, (width - prefWidth) / 2);

        if (BorderLayout.CENTER.equals(position)) {
            show(topBottom, topBottom, leftRight, leftRight, true, modal);
            return lastCommandPressed;
        }
        if (BorderLayout.EAST.equals(position)) {
            if (stretch) {
                show(0, 0, Math.max(0, width - prefWidth), 0, true, modal);
            } else {
                show(topBottom, topBottom, Math.max(0, width - prefWidth), 0, true, modal);
            }
            return lastCommandPressed;
        }
        if (BorderLayout.WEST.equals(position)) {
            if (stretch) {
                show(0, 0, 0, Math.max(0, width - prefWidth), true, modal);
            } else {
                show(topBottom, topBottom, 0, Math.max(0, width - prefWidth), true, modal);
            }
            return lastCommandPressed;
        }
        if (BorderLayout.NORTH.equals(position)) {
            if (stretch) {
                show(0, Math.max(0, height - prefHeight), 0, 0, true, modal);
            } else {
                show(0, Math.max(0, height - prefHeight), leftRight, leftRight, true, modal);
            }
            return lastCommandPressed;
        }
        if (BorderLayout.SOUTH.equals(position)) {
            if (stretch) {
                show(Math.max(0, height - prefHeight), 0, 0, 0, true, modal);
            } else {
                show(Math.max(0, height - prefHeight), 0, leftRight, leftRight, true, modal);
            }
            return lastCommandPressed;
        }
        throw new IllegalArgumentException("Unknown position: " + position);
    }

    /// Closes the current form and returns to the previous form, releasing the
    /// EDT in the process
    @Override
    public void dispose() {
        if (isDisposed()) {
            return;
        }
        setDisposed(true);

        // the dispose parent method might send us back to the form while the command
        // within the dialog might be directing us to another form causing a "blip"
        if (!menu) {
            super.dispose();
        }
        // A host worked out from a popup's anchor belongs to that one showing. Left in
        // place it would silently redirect the next plain show() to whatever surface
        // the last popup happened to be anchored on.
        releaseInferredHost();
    }

    /// Shows a modal dialog and returns the command pressed within the modal dialog
    ///
    /// #### Returns
    ///
    /// last command pressed in the modal dialog
    @Override
    public Command showDialog() {
        lastCommandPressed = null;
        show();
        return lastCommandPressed;
    }

    /// Invoked to allow subclasses of form to handle a command from one point
    /// rather than implementing many command instances
    ///
    /// #### Parameters
    ///
    /// - `cmd`: the action command
    @Override
    protected void actionCommand(Command cmd) {
        // this is important... In a case of nested dialogs based on commands/events a command might be
        // blocked by a different dialog, so when that dialog is disposed (as a result of a command) going
        // back to this dialog will block that command from proceeding and it can be fired again later
        // E.g.:
        // Dialog A has a command which triggers dialog B on top.
        // User presses Cancel in dialog B
        // Dialog A is shown as a result of dialog B dispose method
        // Cancel command event firing is blocked since the dialog B dispose method is now blocking on dialog A show()...
        // When dialog A is disposed using the OK command the OK command is sent correctly and causes dispose
        // EDT is released which also releases the Cancel for dialog B to keep processing...
        // Cancel for dialog B proceeds in the event chain reaching this method....
        // lastCommandPressed can be overrwritten if this check isn't made!!!
        if (!autoDispose || lastCommandPressed == null) {
            lastCommandPressed = cmd;
        }
        if (menu || (autoDispose && cmd.isDisposesDialog())) {
            dispose();
        }
    }

    /// {@inheritDoc}
    @Override
    public boolean animate() {
        isTimedOut();
        return false;
    }

    private boolean isTimedOut() {
        if (time != 0 && System.currentTimeMillis() >= time) {
            time = 0;
            dispose();
            deregisterAnimatedInternal(this);
            return true;
        }
        return false;
    }

    /// Indicates that this is a menu preventing getCurrent() from ever returning this class
    @Override
    boolean isMenu() {
        return menu;
    }

    /// Indicates that this is a menu preventing getCurrent() from ever returning this class
    void setMenu(boolean menu) {
        this.menu = menu;
    }

    /// Prevent a menu from adding the select button
    void addSelectCommand() {
        if (!menu) {
            getMenuBar().addSelectCommand(getSelectCommandText());
        }
    }

    /// Allows us to indicate disposed state for dialogs
    @Override
    boolean isDisposed() {
        return disposed || isTimedOut();
    }

    /// Allows us to indicate disposed state for dialogs
    void setDisposed(boolean disposed) {
        this.disposed = disposed;
    }

    /// Determines whether the execution of a command on this dialog implicitly
    /// disposes the dialog. This defaults to true which is a sensible default for
    /// simple dialogs.
    ///
    /// #### Returns
    ///
    /// true if this dialog disposes on any command
    public boolean isAutoDispose() {
        return autoDispose;
    }

    /// Determines whether the execution of a command on this dialog implicitly
    /// disposes the dialog. This defaults to true which is a sensible default for
    /// simple dialogs.
    ///
    /// #### Parameters
    ///
    /// - `autoDispose`: true if this dialog disposes on any command
    public final void setAutoDispose(boolean autoDispose) {
        this.autoDispose = autoDispose;
    }

    /// The type of the dialog can be one of TYPE_WARNING, TYPE_INFO,
    /// TYPE_ERROR, TYPE_CONFIRMATION or TYPE_ALARM
    ///
    /// #### Returns
    ///
    /// @return can be one of TYPE_WARNING, TYPE_INFO,
    /// TYPE_ERROR, TYPE_CONFIRMATION or TYPE_ALARM
    public int getDialogType() {
        return dialogType;
    }

    /// The type of the dialog can be one of TYPE_WARNING, TYPE_INFO,
    /// TYPE_ERROR, TYPE_CONFIRMATION or TYPE_ALARM
    ///
    /// #### Parameters
    ///
    /// - `dialogType`: @param dialogType can be one of TYPE_WARNING, TYPE_INFO,
    /// TYPE_ERROR, TYPE_CONFIRMATION or TYPE_ALARM
    @Override
    public void setDialogType(int dialogType) {
        this.dialogType = dialogType;
    }

    /// This flag indicates if the dialog should be disposed if a pointer
    /// released event occurred out of the dialog content.
    ///
    /// #### Returns
    ///
    /// true if the dialog should dispose
    public boolean isDisposeWhenPointerOutOfBounds() {
        return disposeWhenPointerOutOfBounds;
    }

    /// This flag indicates if the dialog should be disposed if a pointer
    /// released event occurred out of the dialog content.
    ///
    /// #### Parameters
    ///
    /// - `disposeWhenPointerOutOfBounds`
    public final void setDisposeWhenPointerOutOfBounds(boolean disposeWhenPointerOutOfBounds) {
        this.disposeWhenPointerOutOfBounds = disposeWhenPointerOutOfBounds;
    }

    /// {@inheritDoc}
    @Override
    public void pointerReleased(int x, int y) {
        super.pointerReleased(x, y);
        if (disposeWhenPointerOutOfBounds &&
                pressedOutOfBounds &&
                !getTitleComponent().containsOrOwns(x, y) &&
                !getContentPane().containsOrOwns(x, y) &&
                !getMenuBar().containsOrOwns(x, y)) {
            dispose();
        }
    }

    /// {@inheritDoc}
    @Override
    public void pointerPressed(int x, int y) {
        super.pointerPressed(x, y);
        pressedOutOfBounds = !getTitleComponent().containsOrOwns(x, y) &&
                !getContentPane().containsOrOwns(x, y) &&
                !getMenuBar().containsOrOwns(x, y);
    }

    /// Returns true if a dialog that was disposed did it because of a pointer out of bounds
    ///
    /// #### Returns
    ///
    /// true when a dialog was disposed due to pointer out of bounds.
    public boolean wasDisposedDueToOutOfBoundsTouch() {
        return pressedOutOfBounds;
    }

    /// Screen orientation position for the upcoming dialog. By default
    /// the dialog will be shown at hardcoded coordinates, this method allows us
    /// to pack the dialog appropriately in one of the border layout based locations
    /// see BorderLayout for futher details.
    ///
    /// #### Returns
    ///
    /// the dialogPosition
    public String getDialogPosition() {
        return dialogPosition;
    }

    /// Screen orientation position for the upcoming dialog. By default
    /// the dialog will be shown at hardcoded coordinates, this method allows us
    /// to pack the dialog appropriately in one of the border layout based locations
    /// see BorderLayout for futher details.
    ///
    /// #### Parameters
    ///
    /// - `dialogPosition`: the dialogPosition to set
    public void setDialogPosition(String dialogPosition) {
        this.dialogPosition = dialogPosition;
    }

    /// {@inheritDoc}
    @Override
    void repaint(Component cmp) {
        if (getParent() != null) {
            super.repaint(cmp);
            return;
        }
        if (isVisible() && !disposed && (isMenu() || CN.getCurrentForm() == this)) { //NOPMD CompareObjectsWithEquals
            Display.getInstance().repaint(cmp);
        }
    }

    /// Allows a dialog component to grow or shrink to its new preferred size
    public void growOrShrink() {
        getDialogComponent().setShouldCalcPreferredSize(true);
        growOrShrinkImpl(hostWidth(), hostHeight());
        forceRevalidate();
    }

    private void growOrShrinkImpl(int w, int h) {
        Component contentPane = super.getContentPane();
        Component title = super.getTitleComponent();
        int prefHeight = contentPane.getPreferredH();
        int prefWidth = contentPane.getPreferredW();
        Style contentPaneStyle = contentPane.getStyle();
        Style titleStyle = title.getStyle();

        // if the dialog is packed we can scale it far more accurately based on intention
        if (position != null) {
            int menuHeight = 0;
            if (getSoftButtonCount() > 1) {
                Component menuBar = getSoftButton(0).getParent();
                Style menuStyle = menuBar.getStyle();
                menuHeight = menuBar.getPreferredH() + menuStyle.getVerticalMargins();
            }
            prefWidth = Math.min(prefWidth, w);
            h = h - menuHeight - title.getPreferredH(); // - titleStyle.getMargin(false, TOP) - titleStyle.getMargin(false, BOTTOM);
            int topBottom = Math.max(0, (h - prefHeight) / 2);
            int leftRight = Math.max(0, (w - prefWidth) / 2);
            int top = topBottom;
            int bottom = topBottom;
            int left = leftRight;
            int right = leftRight;

            if (BorderLayout.EAST.equals(position)) {
                left = Math.max(0, w - prefWidth);
                right = 0;
            } else {
                if (BorderLayout.WEST.equals(position)) {
                    right = 0;
                    left = Math.max(0, w - prefWidth);
                } else {
                    if (BorderLayout.NORTH.equals(position)) {
                        top = 0;
                        bottom = Math.max(0, h - prefHeight);
                    } else {
                        if (BorderLayout.SOUTH.equals(position)) {
                            top = Math.max(0, h - prefHeight);
                            bottom = 0;
                        }
                    }
                }
            }

            titleStyle.setMargin(Component.TOP, 0, true);
            titleStyle.setMargin(Component.BOTTOM, 0, true);
            titleStyle.setMargin(Component.LEFT, 0, true);
            titleStyle.setMargin(Component.RIGHT, 0, true);

            contentPaneStyle.setMargin(Component.TOP, top, true);
            contentPaneStyle.setMargin(Component.BOTTOM, bottom, true);
            contentPaneStyle.setMargin(Component.LEFT, left, true);
            contentPaneStyle.setMargin(Component.RIGHT, right, true);
        } else {
            int oldW = getWidth();
            int oldH = getHeight();
            if (oldW != w || oldH != h) {
                // try to preserve the old size of the dialog if we still have room for it...
                if (prefWidth <= w && prefHeight <= h) {
                    float oldLeftRightDistRatio = 1;
                    if (left + right != 0) {
                        oldLeftRightDistRatio = ((float) left) / ((float) left + right);
                    }
                    float oldTopBottomDistRatio = 1;
                    if (left + right != 0) {
                        oldTopBottomDistRatio = ((float) top) / ((float) top + bottom);
                    }
                    top = Math.max(0, (int) ((h - prefHeight) * oldTopBottomDistRatio));
                    left = Math.max(0, (int) ((w - prefWidth) * oldLeftRightDistRatio));
                    bottom = Math.max(0, (h - prefHeight) - top);
                    right = Math.max(0, (w - prefWidth) - left);

                    titleStyle.setMargin(Component.TOP, 0, true);
                    titleStyle.setMargin(Component.BOTTOM, 0, true);
                    titleStyle.setMargin(Component.LEFT, 0, true);
                    titleStyle.setMargin(Component.RIGHT, 0, true);

                    contentPaneStyle.setMargin(Component.TOP, top, true);
                    contentPaneStyle.setMargin(Component.BOTTOM, bottom, true);
                    contentPaneStyle.setMargin(Component.LEFT, left, true);
                    contentPaneStyle.setMargin(Component.RIGHT, right, true);
                } else {
                    float ratioW = ((float) w) / ((float) oldW);
                    float ratioH = ((float) h) / ((float) oldH);

                    Style s = getDialogStyle();

                    s.setMargin(TOP, (int) (s.getMarginTop() * ratioH));
                    s.setMargin(BOTTOM, (int) (s.getMarginBottom() * ratioH));
                    s.setMargin(LEFT, (int) (s.getMarginLeft(isRTL()) * ratioW));
                    s.setMargin(RIGHT, (int) (s.getMarginRight(isRTL()) * ratioW));

                    titleStyle.setMargin(TOP, (int) (titleStyle.getMarginTop() * ratioH));
                    titleStyle.setMargin(LEFT, (int) (titleStyle.getMarginLeft(isRTL()) * ratioW));
                    titleStyle.setMargin(RIGHT, (int) (titleStyle.getMarginRight(isRTL()) * ratioW));
                }
            }
        }
    }

    /// Indicates if we want to enforce directional bias for the popup dialog. If null this field is ignored but if
    /// its set to a value it biases the system towards a fixed direction for the popup dialog.
    ///
    /// #### Returns
    ///
    /// the popupDirectionBiasPortrait
    public Boolean getPopupDirectionBiasPortrait() {
        return popupDirectionBiasPortrait;
    }

    /// Indicates if we want to enforce directional bias for the popup dialog. If null this field is ignored but if
    /// its set to a value it biases the system towards a fixed direction for the popup dialog.
    ///
    /// #### Parameters
    ///
    /// - `popupDirectionBiasPortrait`: the popupDirectionBiasPortrait to set
    public void setPopupDirectionBiasPortrait(Boolean popupDirectionBiasPortrait) {
        this.popupDirectionBiasPortrait = popupDirectionBiasPortrait;
    }

    /// Returns true if the dialog was disposed automatically due to device rotation
    ///
    /// #### Returns
    ///
    /// the disposedDueToRotation value
    public boolean wasDisposedDueToRotation() {
        return disposedDueToRotation;
    }

    /// Dialog background can be blurred using a Gaussian blur effect, this sets the radius of the Gaussian
    /// blur. -1 is a special case value that indicates that no blurring should take effect and the default tint mode
    /// only should be used
    ///
    /// #### Returns
    ///
    /// the blurBackgroundRadius
    public float getBlurBackgroundRadius() {
        return blurBackgroundRadius;
    }

    /// Dialog background can be blurred using a Gaussian blur effect, this sets the radius of the Gaussian
    /// blur. -1 is a special case value that indicates that no blurring should take effect and the default tint mode
    /// only should be used. Notice that this value can be set using the theme constant: `dialogBlurRadiusInt`
    ///
    /// #### Parameters
    ///
    /// - `blurBackgroundRadius`: the blurBackgroundRadius to set
    public void setBlurBackgroundRadius(float blurBackgroundRadius) {
        this.blurBackgroundRadius = blurBackgroundRadius;
    }

    /// In case of a blur effect we need to do something different...
    /// {@inheritDoc}
    @Override
    void initDialogBgPainter(Painter p, Form previousForm) {
        if (getBlurBackgroundRadius() > 0 && Display.impl.isGaussianBlurSupported()) {
            Image img = Image.createImage(previousForm.getWidth(), previousForm.getHeight());
            Graphics g = img.getGraphics();
            previousForm.paintComponent(g, true);
            img = Display.getInstance().gaussianBlurImage(img, blurBackgroundRadius);
            getUnselectedStyle().setBgImage(img);
            getUnselectedStyle().setBackgroundType(Style.BACKGROUND_IMAGE_SCALED_FILL);
        } else {
            super.initDialogBgPainter(p, previousForm);
        }
    }

    /// Allows to use the UIIDs "PopupContentPaneDownwards",
    /// "PopupContentPaneUpwards", "PopupContentPaneRight",
    /// "PopupContentPaneLeft" (instead of the default UIID "PopupContentPane")
    /// to style the PopupDialog more accurately based on the position of the
    /// dialog popup compared to the context component.
    ///
    /// #### Parameters
    ///
    /// - `b`: to enable
    public void setUIIDByPopupPosition(boolean b) {
        this.isUIIDByPopupPosition = b;
    }
}
