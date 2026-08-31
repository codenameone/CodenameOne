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
import com.codename1.ui.events.WindowEvent;
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

import java.util.ArrayList;
import java.util.HashMap;
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
    /// The off-surface half of the timeout, so it still arrives while the surface
    /// holding the dialog is not being painted. Null when no timeout is armed.
    private java.util.Timer timeoutClock;
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
    /// Guards the one frame in which a hosted dialog pumps its own animation list.
    /// The list contains the dialog itself, so the pump reaches animate() again.
    private boolean pumpingHostedAnimations;

    /// What this dialog was told about native window mode, or null when it was not
    /// told. A `Boolean` rather than a boolean so "unset" is distinguishable, which is
    /// what makes instance beats static beats theme expressible.
    private Boolean nativeWindowMode;

    /// The window backing this dialog, non-null for exactly the lifetime of one
    /// native mode showing.
    private Window nativeWindow;

    /// True while the dialog's own title is hidden because the window draws one.
    private boolean nativeTitleHidden;

    /// Whether the title area was visible before the native window hid it.
    private boolean titleAreaWasVisible;

    /// Whether the title component was visible before the native window hid it.
    private boolean titleComponentWasVisible;

    /// True while an anchored popup is being shown, which never opens a window.
    private boolean inPopupShow;

    /// The layer inside `#layerHost` this dialog was added to. Held rather than
    /// re-fetched: `Window#getFormLayeredPane(java.lang.Class, boolean)` re-applies
    /// its layout flag on every call, so asking again after the add can clear it.
    private Container activeLayer;

    /// The dimming, input blocking backdrop behind a hosted dialog, or null when the
    /// dialog neither blocks nor dismisses on an outside press.
    private Container scrim;

    /// The dialog's own background painter, put back when the showing ends.
    private Painter savedBgPainter;

    /// Whether `#savedBgPainter` holds a saved value. Separate from the value itself
    /// because null is a painter a style can legitimately have.
    private boolean savedBgPainterValid;

    /// Listens for the host window resizing while this dialog is on it.
    private ActionListener hostSizeListener;

    /// Listens for the back key while this dialog is on a window.
    private ActionListener hostBackListener;

    /// Listens for the host window being disposed or hidden out from under this dialog.
    private ActionListener hostWindowListener;

    /// Whether the hosted showing in progress has a caller parked on it.
    private boolean hostedModal;

    /// The host's focus owner before this dialog took the keyboard.

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

        DialogScrim(Dialog dlg, int tint, Image backdrop) {
            this.dlg = dlg;
            this.tint = tint;
            this.backdrop = backdrop;
            // Always, not only when modal. Grabbing is what puts this in front of the
            // window's own content for hit testing, and that is as necessary for
            // delivering an outside press to a modeless popup as it is for swallowing
            // one aimed through a modal dialog. A scrim only exists when the dialog
            // wants one of those two things.
            setGrabsPointerEvents(true);
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
    private static final class HostBackListener implements Window.ScopedKeyListener {
        private final Dialog dlg;

        HostBackListener(Dialog dlg) {
            this.dlg = dlg;
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            dlg.hostBackPressed(evt);
        }
    }

    /// Ends a hosted dialog when the window it is on goes away.
    private static final class HostWindowListener implements ActionListener {
        private final Dialog dlg;

        HostWindowListener(Dialog dlg) {
            this.dlg = dlg;
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            dlg.hostWindowGone(evt);
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

    /// Whether newly created dialogs open in a native operating system window on the
    /// desktop.
    private static boolean defaultNativeWindowMode;

    /// Whether `#defaultNativeWindowMode` has been resolved from the theme yet.
    private static boolean defaultNativeWindowModeInitialized;

    private static void initDefaultNativeWindowMode() {
        if (!defaultNativeWindowModeInitialized) {
            defaultNativeWindowModeInitialized = true;
            defaultNativeWindowMode = UIManager.getInstance()
                    .isThemeConstant("defaultNativeWindowModeBool", defaultNativeWindowMode);
        }
    }

    /// Whether newly created dialogs are backed by a real operating system window on
    /// desktop platforms.
    ///
    /// This default can be configured globally with the theme constant
    /// `defaultNativeWindowModeBool`.
    ///
    /// #### Returns
    ///
    /// true when new dialogs open in their own window
    public static boolean isDefaultNativeWindowMode() {
        initDefaultNativeWindowMode();
        return defaultNativeWindowMode;
    }

    /// Sets whether newly created dialogs are backed by a real operating system window
    /// on desktop platforms.
    ///
    /// This overrides the theme constant `defaultNativeWindowModeBool` for the rest of
    /// the application's life.
    ///
    /// #### Parameters
    ///
    /// - `nativeWindowMode`: true to open new dialogs in their own window
    public static void setDefaultNativeWindowMode(boolean nativeWindowMode) {
        defaultNativeWindowModeInitialized = true;
        Dialog.defaultNativeWindowMode = nativeWindowMode;
    }

    /// Whether this dialog is backed by a real operating system window.
    ///
    /// Resolved when the dialog is shown, in this order: what
    /// `#setNativeWindowMode(boolean)` was told, else
    /// `#isDefaultNativeWindowMode()`, else the theme constant behind it. A platform
    /// with no windowing system ignores all three and shows the dialog the ordinary
    /// way -- the setting is a preference about how to render, not a contract, so
    /// shared code does not have to guard it.
    ///
    /// #### Returns
    ///
    /// true when this dialog asks for its own window
    public boolean isNativeWindowMode() {
        if (nativeWindowMode != null) {
            return nativeWindowMode.booleanValue();
        }
        return isDefaultNativeWindowMode();
    }

    /// Sets whether this dialog is backed by a real operating system window.
    ///
    /// Takes effect the next time the dialog is shown. A dialog already on screen is
    /// never moved between the two: there is no safe point to reparent a live, focused,
    /// possibly editing hierarchy while a caller is parked waiting for it.
    ///
    /// #### Parameters
    ///
    /// - `nativeWindowMode`: true to open this dialog in its own window
    public void setNativeWindowMode(boolean nativeWindowMode) {
        this.nativeWindowMode = Boolean.valueOf(nativeWindowMode);
    }

    /// The window backing this dialog while it is showing.
    ///
    /// #### Returns
    ///
    /// the window, or null when the dialog is not in one
    public Window getNativeWindow() {
        return nativeWindow;
    }

    /// Called once the window backing this dialog has been configured and before it is
    /// shown, so an application can adjust it -- make it resizable, give it an icon,
    /// take its decoration away.
    ///
    /// #### Parameters
    ///
    /// - `w`: the window about to be shown
    protected void initNativeWindow(Window w) {
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
        // Onto the operating system's title bar too while this dialog is backed by a
        // window of its own. The window carries a copy taken when it was created and
        // the dialog's own title label is hidden behind the native title bar, so
        // without this a setTitle after showing -- from onShow, or from any later
        // update -- would change nothing the user can see.
        if (nativeWindow != null && !nativeWindow.isWindowDisposed()) {
            nativeWindow.setTitle(title == null ? "" : title);
        }
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
        // Armed off the surface as well as on it. The registration above is polled from
        // animate(), which rides the animation loop of whatever is being painted -- and
        // a dialog hosted in a window, or living in a native window of its own, is not
        // painted at all while that window is minimized. Minimizing deliberately does
        // not dispose the dialog, so a modal caller waiting for the timeout to release
        // it waited for as long as the window stayed down. isTimedOut() clears the
        // deadline as it fires, so whichever of the two reaches it first wins and the
        // other finds nothing to do.
        cancelTimeoutClock();
        timeoutClock = Display.getInstance().setTimeout((int) time, new TimeoutClock(this));
    }

    /// Polls the deadline from a timer thread rather than from the animation loop.
    ///
    /// Named rather than anonymous: an anonymous Runnable here is
    /// SIC_INNER_SHOULD_BE_STATIC_ANON under the zero-findings SpotBugs gate.
    private static final class TimeoutClock implements Runnable {
        private final Dialog dlg;

        TimeoutClock(Dialog dlg) {
            this.dlg = dlg;
        }

        @Override
        public void run() {
            dlg.isTimedOut();
        }
    }

    /// Stops the off-surface clock, if one is armed.
    ///
    /// Called as a showing ends and as the deadline is reached, so a timer does not sit
    /// on a thread of its own for the remainder of a long timeout that nothing is
    /// waiting for any more.
    private void cancelTimeoutClock() {
        if (timeoutClock != null) {
            timeoutClock.cancel();
            timeoutClock = null;
        }
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
    /// A hosted dialog owns the commands activated inside it even though it is
    /// parented. Without this the walk goes past it to the window, whose command
    /// listeners are not the dialog's -- so pressing the dialog's own OK button set no
    /// lastCommandPressed and never disposed it, and a modal wait on it never ended.
    @Override
    boolean isCommandHost() {
        return getParent() == null || layerHost != null;
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

    /// Whether this showing opens a real operating system window.
    ///
    /// #### Returns
    ///
    /// true to open a window of its own
    private boolean usesNativeWindow() {
        if (!Desktop.isSupported()) {
            return false;
        }
        // A menu is framework furniture, and an anchored popup points at a rectangle in
        // somebody else's coordinate space -- neither is a thing to give a title bar to.
        if (isMenu() || inPopupShow) {
            return false;
        }
        return isNativeWindowMode();
    }

    /// Shows this dialog as a real operating system window and, when modal, waits there
    /// until it is disposed.
    ///
    /// #### Parameters
    ///
    /// - `modal`: whether to wait here until the dialog is disposed
    private void showInNativeWindow(boolean modal) {
        Display.getInstance().flushEdt();
        // A modeless dialog can be shown again before it is disposed, exactly as
        // InteractionDialog can. Overwriting the field left the first window on screen
        // and empty, with its own close and dispose bridges still pointing at this
        // dialog -- whose window was now the second one, so closing the abandoned
        // window tore down the showing the user was actually looking at.
        Window previous = nativeWindow;
        if (previous != null) {
            finishNativeShowing();
            if (!previous.isWindowDisposed()) {
                previous.dispose();
            }
            setDisposed(false);
        }
        TopLevelContainer host = resolveHost();
        Window w = new Window(getTitle() == null ? "" : getTitle(), new BorderLayout());
        nativeWindow = w;
        // Before show(). Window.setOwnerWindow throws once the peer exists, because
        // every port fixes the ownership relation when it creates the native window.
        if (host != null) {
            w.setOwnerWindow(host);
        }
        w.setCloseOperation(Window.DO_NOTHING_ON_CLOSE);
        w.setResizable(false);
        w.setDecorated(true);
        w.getContentPane().setScrollableY(false);
        attachNativePayload(w);
        publishNativeMenuCommands(w);
        w.addCommandListener(new NativeCommandBridge(this));
        w.addCloseListener(new NativeCloseBridge(this));
        w.addWindowListener(new NativeDisposeBridge(this));
        initNativeWindow(w);
        hideOwnTitleIfDecorated(w);
        // The keyboard, on the same terms as a hosted dialog. A window never reads a
        // nested form's key listeners, and its default-command dispatch only runs for
        // whatever holds the key scope -- so without these two a dialog in a window of
        // its own had neither its shortcuts nor its default action.
        w.pushKeyInputScope(this);
        publishKeyListeners(w);
        if (modal) {
            // MODALITY_WINDOW, not APPLICATION. Desktop.blocks() already blocks the main
            // surface when the owner is a Form, and exempts a dialog opened from another
            // dialog's window -- which is exactly what a modal Dialog has always meant.
            // APPLICATION would newly freeze unrelated windows.
            w.setModalityType(Window.MODALITY_WINDOW);
        }
        // Shown before it is sized, because the size that matters is the drawable and a
        // window cannot report its chrome until the platform has made one. Sizing first
        // asked for a frame of the content's size and left the box clipped along the
        // bottom by exactly the height of the title bar.
        w.show();
        sizeAndPlaceNativeWindow(w, host);
        // The same callbacks the historical path runs, and for the same reasons: the
        // dialog sound, a subclass's onShowCompleted, and every show listener. A modal
        // dialog whose show listener is what disposes it would otherwise never be
        // released. Before the wait below, so a listener that disposes is honoured.
        onShow();
        onShowCompletedImpl();
        // Sized once more, because those callbacks are where content is commonly added
        // or revealed and the window does not resize itself. Sizing only beforehand
        // left anything they added compressed or clipped in a window that cannot be
        // resized. Sizing only afterwards would show the window at its pending size
        // first, so it is done twice: once so it appears right, once so it stays right.
        if (!isDisposed() && !w.isWindowDisposed()) {
            sizeAndPlaceNativeWindow(w, host);
        }
        if (modal) {
            // Idempotent for a window already on screen: it takes the modal blocker and
            // parks the caller without showing anything a second time.
            w.showModal();
            // Disposed, not merely detached. Hiding a window ends the modal wait exactly
            // as disposing it does -- isModalFinished() reads both -- so a caller that
            // hides the window this handed it arrives here with the window still alive.
            // finishNativeShowing() only takes the payload back out, so the peer stayed
            // registered with its owner for a dialog nobody can reach any more, one per
            // showing. The dispose fires Disposed, and the bridge on it calls
            // finishNativeShowing() first; the call below is then the idempotent no-op.
            if (!w.isWindowDisposed()) {
                w.dispose();
            }
            finishNativeShowing();
        }
    }

    /// Puts the dialog into the window.
    ///
    /// The whole dialog goes in, not just its content, for the same reason the layered
    /// path does it that way: a dialog is positioned by margins written into its own
    /// title and content styles, and moving only the content leaves that arithmetic
    /// describing something that is no longer there. Taking the content pane out is
    /// also not a thing a `Form` supports -- `Form#removeComponent(Component)` forwards
    /// to the content pane, so asking a form to remove its own content pane asks that
    /// pane to remove itself from itself.
    ///
    /// Nothing inside the dialog resolves to the dialog as a result:
    /// `Form#getTopLevelContainer()` hands the walk upwards once it is parented, so
    /// buttons, repaints and focus all reach the window.
    ///
    /// #### Parameters
    ///
    /// - `w`: the window to move into
    private void attachNativePayload(Window w) {
        // Margins written by an earlier showing would inset the box inside the window.
        applyDialogMargins(0, 0, 0, 0, false);
        savedBgPainter = getStyle().getBgPainter();
        savedBgPainterValid = true;
        getStyle().setBgPainter(NO_OP_PAINTER);
        w.getContentPane().addComponent(BorderLayout.CENTER, this);
    }

    /// Takes the dialog back out of the window. Idempotent.
    private void detachNativePayload() {
        if (getParent() != null) {
            remove();
        }
        if (savedBgPainterValid) {
            // Whether one was saved, not whether it was non-null: a dialog whose style
            // had no painter would otherwise keep the no-op one for good, and every
            // later showing of it would paint no background at all.
            getStyle().setBgPainter(savedBgPainter);
            savedBgPainter = null;
            savedBgPainterValid = false;
        }
    }

    /// Hands menu style commands to the window so the platform can put them wherever it
    /// shows a window's commands.
    ///
    /// Button bar commands are deliberately not published: `placeButtonCommands` never
    /// calls `addCommand`, so a dialog with an OK and a Cancel button has a command
    /// count of zero and no native menu is built for them. They are buttons inside the
    /// dialog and they stay that way.
    ///
    /// #### Parameters
    ///
    /// - `w`: the window to publish to
    /// {@inheritDoc}
    ///
    /// Mirrored onto the operating system window for as long as one is up. The commands
    /// are copied across when that window is built, and a dialog is free to change them
    /// afterwards -- from `onShow`, or at any time after a modeless show. Without this
    /// the platform menu kept whatever it was built with.
    ///
    /// Button-bar commands never arrive here: `placeButtonCommands` does not call
    /// `addCommand`, which is what keeps an OK/Cancel dialog from growing a native menu.
    @Override
    public void addCommand(Command cmd, int offset) {
        super.addCommand(cmd, offset);
        syncNativeMenuCommands();
    }

    /// {@inheritDoc}
    ///
    /// Taken off the operating system window too. A command removed while the dialog was
    /// showing stayed on the platform menu and could still be picked from it, which then
    /// reached the dialog through the command bridge as though it were still there.
    @Override
    public void removeCommand(Command cmd) {
        super.removeCommand(cmd);
        syncNativeMenuCommands();
    }

    /// {@inheritDoc}
    ///
    /// Cleared from the operating system window too, for the same reason.
    @Override
    public void removeAllCommands() {
        super.removeAllCommands();
        syncNativeMenuCommands();
    }

    /// Puts the operating system window's menu back in step with this dialog's own
    /// ordered commands.
    ///
    /// Rebuilt rather than patched, because a window appends and a dialog inserts: a
    /// command added at an offset -- a high-priority action at the front -- went to the
    /// end of the native menu, so the two showed a different order for every insertion
    /// that was not at the tail.
    private void syncNativeMenuCommands() {
        Window w = nativeWindow;
        if (w == null) {
            return;
        }
        w.removeAllCommands();
        publishNativeMenuCommands(w);
    }

    private void publishNativeMenuCommands(Window w) {
        int count = getCommandCount();
        for (int i = 0; i < count; i++) {
            w.addCommand(getCommand(i));
        }
    }

    /// Hides the dialog's own title while the operating system draws one.
    ///
    /// #### Parameters
    ///
    /// - `w`: the window
    private void hideOwnTitleIfDecorated(Window w) {
        if (!w.isDecorated() || nativeTitleHidden) {
            return;
        }
        nativeTitleHidden = true;
        // What they were, not what they usually are. An application is free to hide
        // the title area or the title itself before showing, and a dialog is reusable
        // -- putting both back visible on teardown would resurrect title UI the
        // application had deliberately suppressed the next time it is shown hosted or
        // the legacy way.
        titleAreaWasVisible = getTitleArea().isVisible();
        titleComponentWasVisible = getTitleComponent().isVisible();
        getTitleArea().setVisible(false);
        getTitleComponent().setVisible(false);
    }

    /// The window this dialog is currently inside, whichever way it got there.
    ///
    /// A dialog reaches a window two ways -- into its layered pane, or as the content
    /// of a window of its own -- and the keyboard has to work the same in both. Keeping
    /// the two apart is what left a native-window dialog with no key listeners and no
    /// default command.
    ///
    /// #### Returns
    ///
    /// the window, or null when this dialog is not in one
    private Window hostWindow() {
        return layerHost != null ? layerHost : nativeWindow;
    }

    /// Registers this dialog's key listeners on its host window.
    ///
    /// #### Parameters
    ///
    /// - `host`: the window to publish onto
    private void publishKeyListeners(Window host) {
        publishKeyMap(host, keyListenerMap(), false);
        // Both maps. A form dispatches the raw code and the game action; a window that
        // was given only the first left every game key shortcut on a hosted dialog
        // silently dead.
        publishKeyMap(host, gameKeyListenerMap(), true);
    }

    /// Registers one of this dialog's key maps on its host window.
    ///
    /// #### Parameters
    ///
    /// - `host`: the window to publish onto
    ///
    /// - `own`: the map, may be null
    ///
    /// - `game`: true when these are game key listeners
    private void publishKeyMap(Window host,
            HashMap<Integer, ArrayList<ActionListener>> own, boolean game) {
        if (own == null) {
            return;
        }
        for (Map.Entry<Integer, ArrayList<ActionListener>> e : own.entrySet()) {
            int code = e.getKey().intValue();
            ArrayList<ActionListener> listeners = e.getValue();
            for (ActionListener l : listeners) {
                publishKeyListener(host, code, l, game);
            }
        }
    }

    /// Registers one of this dialog's key listeners on its host, wrapped so it only
    /// fires while this dialog owns the keyboard.
    ///
    /// #### Parameters
    ///
    /// - `host`: the window to publish onto
    ///
    /// - `keyCode`: the code
    ///
    /// - `listener`: the listener
    private void publishKeyListener(Window host, int keyCode, ActionListener listener,
            boolean game) {
        if (hostedKeyListeners == null) {
            hostedKeyListeners = new ArrayList<HostedKeyListener>();
        }
        // Once each. Adding the dialog to the layer initializes it, and a subclass that
        // registers a listener from initComponent() is doing so with the host already
        // set -- so it is published there and then, and the bulk publication that
        // follows put a second wrapper on the same listener. Both fired, so every one of
        // those shortcuts ran the application's callback twice for a single key.
        int published = hostedKeyListeners.size();
        for (int iter = 0; iter < published; iter++) {
            HostedKeyListener existing = hostedKeyListeners.get(iter);
            if (existing.keyCode == keyCode && existing.game == game
                    && existing.delegate == listener) { //NOPMD CompareObjectsWithEquals
                return;
            }
        }
        HostedKeyListener wrapper = new HostedKeyListener(this, keyCode, listener, game);
        hostedKeyListeners.add(wrapper);
        if (game) {
            host.addGameKeyListener(keyCode, wrapper);
        } else {
            host.addKeyListener(keyCode, wrapper);
        }
    }

    /// Takes this dialog's key listeners back off its host.
    ///
    /// #### Parameters
    ///
    /// - `host`: the window they were published onto
    private void unpublishKeyListeners(Window host) {
        if (hostedKeyListeners == null) {
            return;
        }
        for (HostedKeyListener wrapper : hostedKeyListeners) {
            if (wrapper.game) {
                host.removeGameKeyListener(wrapper.keyCode, wrapper);
            } else {
                host.removeKeyListener(wrapper.keyCode, wrapper);
            }
        }
        hostedKeyListeners = null;
    }

    /// The wrappers this dialog put on its host, so they can be taken off again.
    private ArrayList<HostedKeyListener> hostedKeyListeners;

    /// {@inheritDoc}
    @Override
    void keyListenerAdded(int keyCode, ActionListener listener) {
        // Added after the dialog was shown -- from onShow, say -- so it has to reach
        // the host the same way the ones present at show time did.
        Window host = hostWindow();
        if (host != null) {
            publishKeyListener(host, keyCode, listener, false);
        }
    }

    /// {@inheritDoc}
    @Override
    void gameKeyListenerAdded(int keyCode, ActionListener listener) {
        Window host = hostWindow();
        if (host != null) {
            publishKeyListener(host, keyCode, listener, true);
        }
    }

    /// {@inheritDoc}
    @Override
    void gameKeyListenerRemoved(int keyCode, ActionListener listener) {
        unpublishKeyListener(keyCode, listener, true);
    }

    /// {@inheritDoc}
    @Override
    void keyListenerRemoved(int keyCode, ActionListener listener) {
        unpublishKeyListener(keyCode, listener, false);
    }

    /// Takes one published wrapper back off the host.
    ///
    /// #### Parameters
    ///
    /// - `keyCode`: the code it was registered for
    ///
    /// - `listener`: the listener it wraps
    ///
    /// - `game`: true when it is a game key listener
    private void unpublishKeyListener(int keyCode, ActionListener listener, boolean game) {
        Window host = hostWindow();
        if (host == null || hostedKeyListeners == null) {
            return;
        }
        for (int iter = 0; iter < hostedKeyListeners.size(); iter++) {
            HostedKeyListener wrapper = hostedKeyListeners.get(iter);
            if (wrapper.keyCode == keyCode && wrapper.game == game
                    && wrapper.delegate == listener) { //NOPMD CompareObjectsWithEquals
                if (game) {
                    host.removeGameKeyListener(keyCode, wrapper);
                } else {
                    host.removeKeyListener(keyCode, wrapper);
                }
                hostedKeyListeners.remove(iter);
                return;
            }
        }
    }

    /// One of a hosted dialog's key listeners, as the host window sees it.
    ///
    /// The wrapper exists for the scoping: every hosted dialog publishes onto the same
    /// window, so without it a dialog covered by another one would still be running its
    /// shortcuts. It fires only while the dialog it belongs to is the one holding the
    /// keyboard.
    private static final class HostedKeyListener implements Window.ScopedKeyListener {
        private final Dialog dlg;
        private final int keyCode;
        private final ActionListener delegate;
        private final boolean game;

        HostedKeyListener(Dialog dlg, int keyCode, ActionListener delegate, boolean game) {
            this.dlg = dlg;
            this.keyCode = keyCode;
            this.delegate = delegate;
            this.game = game;
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            Window host = dlg.hostWindow();
            if (host == null) {
                return;
            }
            if (!host.isKeyDispatchOwner(dlg)) {
                return;
            }
            // Sourced from the dialog, and carrying the code this listener was
            // registered for. A listener added to a Dialog is handed an event whose
            // source is that dialog when it is shown the historical way, and a game
            // listener is handed the game action rather than the key that produced it.
            // Forwarding the host's own event changed both: the source became the
            // window, so a listener that compares or casts it saw something else -- and
            // on the iOS VM a bad cast is not even an exception, it reads the wrong
            // object's fields -- and a game listener saw the physical key code.
            ActionEvent forwarded = new ActionEvent(dlg, keyCode);
            delegate.actionPerformed(forwarded);
            // Back to the host, which is what stops the next listener for this key and
            // the window's own default handling.
            if (forwarded.isConsumed()) {
                evt.consume();
            }
        }
    }

    /// Puts the dialog's own title back.
    private void restoreOwnTitle() {
        if (!nativeTitleHidden) {
            return;
        }
        nativeTitleHidden = false;
        getTitleArea().setVisible(titleAreaWasVisible);
        getTitleComponent().setVisible(titleComponentWasVisible);
    }

    /// Sizes the window to the dialog's content and centres it on its host.
    ///
    /// #### Parameters
    ///
    /// - `w`: the window
    ///
    /// - `host`: the top level it belongs to, may be null
    private void sizeAndPlaceNativeWindow(Window w, TopLevelContainer host) {
        revalidate();
        // Onto the owner's display before anything is measured against it. Both the
        // conversion below and the one inside setWindowContentSize read the window's own
        // monitor, so sizing first and moving afterwards measured against whichever
        // display the platform happened to open the window on -- a different scale there
        // left a window that cannot be resized substantially the wrong size.
        placeNativeWindow(w, host);
        Dimension pref = getDialogPreferredSize();
        int cw = Math.max(1, pref.getWidth());
        int ch = Math.max(1, pref.getHeight());
        // The window's own display, which the placement above has just put on the
        // owner's, and in the pixels the preferred size is measured in.
        Rectangle work = w.getWorkAreaInPixels();
        if (work.getWidth() > 0) {
            cw = Math.min(cw, work.getWidth() * 9 / 10);
        }
        if (work.getHeight() > 0) {
            ch = Math.min(ch, work.getHeight() * 9 / 10);
        }
        // The drawable size, not the frame's: a decorated window's chrome sits outside
        // the surface, so asking for a frame this size clips the box by the title bar.
        w.setWindowContentSize(cw, ch);
        // Centred again at the size it ended up rather than the one it opened with.
        placeNativeWindow(w, host);
    }

    /// Puts a native-window dialog on its owner's display, centred.
    private static void placeNativeWindow(Window w, TopLevelContainer host) {
        if (host != null) {
            w.centerOn(host);
        } else {
            w.centerOnDesktop();
        }
    }

    /// A command reached the window this dialog is in.
    ///
    /// #### Parameters
    ///
    /// - `cmd`: the command
    ///
    /// - `ev`: the event that carried it
    void nativeCommandActivated(Command cmd, ActionEvent ev) {
        if (cmd != null) {
            actionCommandImplNoRecurseComponent(cmd, ev);
        }
    }

    /// The user activated the window's own close control.
    void nativeCloseRequested(ActionEvent evt) {
        // Vetoed so the dialog owns the teardown. Letting the window dispose itself
        // first would leave the dialog believing it was still showing.
        evt.consume();
        Command back = getBackCommand();
        if (back != null) {
            dispatchCommand(back, new ActionEvent(back, ActionEvent.Type.Command));
            return;
        }
        dispose();
    }

    /// Takes this dialog back out of its window. Idempotent, and marshalled onto the
    /// event dispatch thread.
    ///
    /// `Window#dispose()` publishes its disposed flag and wakes the parked caller before
    /// it fires the event this hangs off, so the caller can get here first.
    void finishNativeShowing() {
        if (!Display.getInstance().isEdt()) {
            Display.getInstance().callSeriallyAndWait(new Runnable() {
                @Override
                public void run() {
                    finishNativeShowing();
                }
            });
            return;
        }
        if (nativeWindow == null) {
            return;
        }
        Window closing = nativeWindow;
        unpublishKeyListeners(closing);
        closing.removeKeyInputScope(this);
        nativeWindow = null;
        // The window can die without the dialog having asked -- an owner disposing its
        // children, the desktop shutting down, someone calling dispose() on what
        // getNativeWindow() handed them. The dialog is over either way, and leaving the
        // flag clear would send a later dispose() down the ordinary Form teardown for a
        // showing that never went through it.
        setDisposed(true);
        detachNativePayload();
        restoreOwnTitle();
    }

    /// Routes a window's command activations back into the dialog.
    private static final class NativeCommandBridge implements ActionListener {
        private final Dialog dlg;

        NativeCommandBridge(Dialog dlg) {
            this.dlg = dlg;
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            dlg.nativeCommandActivated(evt.getCommand(), evt);
        }
    }

    /// Routes the window's close control back into the dialog.
    private static final class NativeCloseBridge implements ActionListener {
        private final Dialog dlg;

        NativeCloseBridge(Dialog dlg) {
            this.dlg = dlg;
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            dlg.nativeCloseRequested(evt);
        }
    }

    /// Tears the dialog down however its window died -- disposed by the dialog, by an
    /// owner cascade, or by the desktop shutting down.
    private static final class NativeDisposeBridge implements ActionListener {
        private final Dialog dlg;

        NativeDisposeBridge(Dialog dlg) {
            this.dlg = dlg;
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            if (evt instanceof WindowEvent
                    && ((WindowEvent) evt).getType() == WindowEvent.Type.Disposed) {
                dlg.finishNativeShowing();
            }
        }
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
        // The hosted path has the same re-show case the native one does, and a worse
        // failure: this dialog is still parented to the old layer, so adding it again
        // throws -- but only after a second scrim has been built and the field pointing
        // at the first overwritten, leaving that one covering the window and swallowing
        // every press with nothing left holding a reference to remove it.
        if (layerHost != null) {
            disposeFromHostLayer();
            setDisposed(false);
        }
        Window host = (Window) resolveHost();
        layerHost = host;
        hostedModal = modal;
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
        savedBgPainterValid = true;
        getStyle().setBgPainter(NO_OP_PAINTER);

        // Always, for any Dialog. Form.showModal tints the previous surface whether or
        // not the dialog blocks -- a modeless Dialog still replaces what was there --
        // so gating this on modality dropped the dimming a modeless one has always had,
        // and with it the tint InfiniteProgress.showInfiniteBlocking configures before
        // showing itself modelessly. InteractionDialog is the thing that overlays
        // without a backdrop, and it does not come through here.
        //
        // The host's tint, not the dialog's: the historical path dims the previous form
        // and paints that form's tintColor, which is why ComboBox and the floating
        // action button submenu set the host's tint to zero to opt out of dimming.
        scrim = new DialogScrim(this, host.getTintColor(), backdrop);
        layer.addComponent(scrim);

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
        // And this dialog's own key listeners with it. A window dispatches keys through
        // its own map and never looks at a nested form's, so an application's shortcuts
        // stopped working the moment the dialog was hosted rather than shown the
        // historical way.
        publishKeyListeners(host);
        // A window closed through its own title bar disposes by default, and takes the
        // layered pane and everything in it with it. Without this a modal caller waits
        // on a dialog whose surface is gone, and a modeless one keeps the dead window
        // alive through layerHost.
        hostWindowListener = new HostWindowListener(this);
        host.addWindowListener(hostWindowListener);
        // Keys follow the dialog, not the window's previous focus owner. Saved rather
        // than cleared so a dialog over a dialog gives the keyboard back to the one
        // underneath rather than to the window.
        host.pushKeyInputScope(this);
        // And the pointer with it. The scrim already stops presses reaching what is
        // underneath, but the window's own pointer listeners run before anything is hit
        // tested, so without this a listener on the window still saw every press meant
        // for the dialog -- and one that consumed it took the press away from the
        // dialog altogether. The historical path got this for free by replacing the
        // surface, which left the old one's listeners unreachable.
        host.pushPointerInputScope(this);
        focusFirstFocusable(host);

        onShow();
        onShowCompletedImpl();

        if (modal) {
            while (!isDisposed()) {
                CN.invokeAndBlock(BLOCKING_SLEEP);
            }
            // Only when the editing this would end belongs to the surface this dialog
            // was on. Hosted modality claims its host's pointer and keyboard and leaves
            // every other window interactive, but this call is process wide -- on iOS it
            // reaches the global stopTextEditing -- so closing a dialog in one window
            // dismissed the keyboard of a field being typed into in another.
            // Against the surface this dialog was actually shown on, held from above,
            // not one resolved now: the teardown clears layerHost before this runs, so
            // resolveHost() answers with whatever has the focus by then. For a dialog
            // that timed out while the user had moved to another window, that is the
            // window they moved to -- which made the test pass and stopped the editing
            // this exists to leave alone.
            Component editing = Display.impl.getEditingText();
            TopLevelContainer editingHost =
                    editing == null ? null : editing.getTopLevelContainer();
            if (editing == null || editingHost == null || editingHost == host) { //NOPMD CompareObjectsWithEquals
                Display.getInstance().setShowVirtualKeyboard(false);
            }
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
            // Not into a branch that is not on screen. Visibility is asked of each
            // component on its own, so a child of a hidden or disabled container still
            // answers that it is visible and focusable -- and the host accepts it, after
            // which the scope check finds it inside the dialog and calls it valid. The
            // keyboard then went to a control the user cannot see.
            if (!c.isVisible() || !c.isEnabled()) {
                return null;
            }
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
    void hostBackPressed(ActionEvent evt) {
        // Every hosted dialog on this window listens for the key, and they are called
        // in the order they were shown -- so without this the oldest dialog answers a
        // back aimed at the one on top of it. Consumed so the ones underneath do not
        // also act on it.
        if (!isTopmostHostedDialog()) {
            return;
        }
        evt.consume();
        Command back = getBackCommand();
        if (back != null) {
            dispatchCommand(back, new ActionEvent(back, ActionEvent.Type.Command));
            return;
        }
        dispose();
    }

    /// Whether this is the last dialog added to its host's shared layer, which is the
    /// one the user sees on top.
    ///
    /// #### Returns
    ///
    /// true when nothing is stacked above this dialog
    private boolean isTopmostHostedDialog() {
        Container layer = activeLayer;
        if (layer == null) {
            return false;
        }
        Dialog last = null;
        for (Component cmp : layer.getChildrenAsList(true)) {
            if (cmp instanceof Dialog) {
                last = (Dialog) cmp;
            }
        }
        return last == this; //NOPMD CompareObjectsWithEquals
    }

    /// The host window this dialog is on was disposed or hidden.
    void hostWindowGone(ActionEvent evt) {
        if (!(evt instanceof WindowEvent) || isDisposed()) {
            return;
        }
        WindowEvent.Type type = ((WindowEvent) evt).getType();
        if (type == WindowEvent.Type.Disposed) {
            dispose();
            return;
        }
        // Hidden is terminal only for a modal dialog, and deliberately not for a
        // modeless one.
        //
        // A hidden window cannot be reached, so a modal dialog on it can never be
        // dismissed and its caller would wait for good -- Window.showModal() ends its
        // own wait on exactly this event for the same reason. A modeless dialog has
        // nobody waiting, and a window hidden through HIDE_ON_CLOSE is kept alive
        // precisely so it can be shown again: disposing what is in its layered pane
        // would quietly throw that content away between a hide and the next show.
        //
        // Minimizing does not arrive here at all. It clears nativeVisible too, but the
        // port reports it as Minimized and Window records it separately, so a window the
        // user shrank keeps its dialogs and gets them back on restore.
        if (type == WindowEvent.Type.Hidden && hostedModal) {
            dispose();
        }
    }

    /// Takes this dialog back out of its host window's layered pane.
    ///
    /// The counterpart to `#showInHostLayer(int, int, int, int, boolean, boolean)`, and
    /// the reason `#disposeImpl()` is overridden rather than `#dispose()`: `MenuBar`
    /// calls disposeImpl directly, and a Dialog skips super.dispose() when it is a menu.
    private void disposeFromHostLayer() {
        Window host = layerHost;
        layerHost = null;
        hostedModal = false;
        if (host != null) {
            if (hostSizeListener != null) {
                host.removeSizeChangedListener(hostSizeListener);
                hostSizeListener = null;
            }
            if (hostBackListener != null) {
                host.removeKeyListener(MenuBar.backSK, hostBackListener);
                hostBackListener = null;
            }
            if (hostWindowListener != null) {
                host.removeWindowListener(hostWindowListener);
                hostWindowListener = null;
            }
            // Releasing the claim is all that is needed: the window restores focus
            // itself once the last claimant has gone, which is the only point at which
            // handing focus back cannot hand it to something still covered.
            unpublishKeyListeners(host);
            host.removeKeyInputScope(this);
            host.removePointerInputScope(this);
        }
        if (scrim != null) {
            scrim.remove();
            scrim = null;
        }
        remove();
        if (savedBgPainterValid) {
            // Whether one was saved, not whether it was non-null: a dialog whose style
            // had no painter would otherwise keep the no-op one for good, and every
            // later showing of it would paint no background at all.
            getStyle().setBgPainter(savedBgPainter);
            savedBgPainter = null;
            savedBgPainterValid = false;
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
        if (usesNativeWindow()) {
            showInNativeWindow(modal);
            return;
        }
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
        inPopupShow = true;
        try {
            return showPopupDialogImpl(rect);
        } finally {
            inPopupShow = false;
        }
    }

    /// The body of `#showPopupDialog(Rectangle)`.
    ///
    /// Split out so the popup flag is cleared however the showing ends. An anchored
    /// popup never opens an operating system window: the rectangle it points at is in
    /// its host's coordinate space, and nothing exposes where a window's drawable
    /// actually starts on the desktop, so the popup would land off by the height of
    /// the host's title bar with no way to correct for it. A separate window would also
    /// never see the click that is supposed to dismiss it, and would steal focus from
    /// the window that opened it every time it appeared.
    ///
    /// #### Parameters
    ///
    /// - `rect`: the rectangle to point at
    ///
    /// #### Returns
    ///
    /// the command the user triggered, if any
    private Command showPopupDialogImpl(Rectangle rect) {
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
        // The showing this clock belonged to is over. The deadline itself is left alone:
        // it survives a dispose today, so a dialog shown again inside its original
        // timeout still times out through the animation poll, exactly as before.
        cancelTimeoutClock();

        if (nativeWindow != null) {
            // The window fires Disposed, which is what actually tears the dialog down.
            // None of the base teardown applies: there is no previous form, because
            // Display.setCurrent was never called for this showing.
            nativeWindow.dispose();
        } else if (!menu) {
            // the dispose parent method might send us back to the form while the command
            // within the dialog might be directing us to another form causing a "blip"
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
        if (isTimedOut()) {
            return false;
        }
        if (layerHost == null || pumpingHostedAnimations) {
            // Shown the historical way the dialog is the surface being painted, so
            // Display already loops its animatableComponents -- and this dialog is one
            // of the entries whenever a timeout is set: setTimeout registers the
            // dialog with registerAnimatedInternal so that isTimedOut is polled at all.
            // Calling super here would re-enter that very loop through
            // Form.animate() -> repaintAnimations(), which is unbounded recursion.
            // The flag catches the same re-entry from the hosted branch below, where
            // the loop reaches this entry one frame later.
            return false;
        }
        // Hosted, the dialog is not the painted surface: nothing loops its own
        // animatableComponents, so anything the application registered on the dialog
        // through registerAnimated() sat in that list and never advanced. Form.animate()
        // drains it for a form nested in another surface, which is exactly what a hosted
        // dialog is.
        pumpingHostedAnimations = true;
        try {
            return super.animate();
        } finally {
            pumpingHostedAnimations = false;
        }
    }

    private boolean isTimedOut() {
        if (time != 0 && System.currentTimeMillis() >= time) {
            time = 0;
            cancelTimeoutClock();
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
