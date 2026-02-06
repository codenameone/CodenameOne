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

import com.codename1.annotations.Async;
import com.codename1.capture.VideoCaptureConstraints;
import com.codename1.codescan.CodeScanner;
import com.codename1.contacts.Contact;
import com.codename1.db.Database;
import com.codename1.impl.CodenameOneImplementation;
import com.codename1.impl.CodenameOneThread;
import com.codename1.impl.ImplementationFactory;
import com.codename1.impl.VirtualKeyboardInterface;
import com.codename1.io.Log;
import com.codename1.io.Preferences;
import com.codename1.io.Util;
import com.codename1.l10n.L10NManager;
import com.codename1.location.LocationManager;
import com.codename1.media.Media;
import com.codename1.media.MediaRecorderBuilder;
import com.codename1.messaging.Message;
import com.codename1.notifications.LocalNotification;
import com.codename1.payment.Purchase;
import com.codename1.plugin.PluginSupport;
import com.codename1.plugin.event.IsGalleryTypeSupportedEvent;
import com.codename1.plugin.event.OpenGalleryEvent;
import com.codename1.system.CrashReport;
import com.codename1.ui.animations.Animation;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.MessageEvent;
import com.codename1.ui.events.WindowEvent;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.EventDispatcher;
import com.codename1.ui.util.ImageIO;
import com.codename1.util.AsyncResource;
import com.codename1.util.RunnableWithResultSync;
import com.codename1.util.SuccessCallback;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/// Central class for the API that manages rendering/events and is used to place top
/// level components (`Form`) on the "display".
///
/// This class handles the main thread for the toolkit referenced here on as the EDT
/// (Event Dispatch Thread) similar to the Swing EDT. This thread encapsulates the platform
/// specific event delivery and painting semantics and enables threading features such as
/// animations etc...
///
/// The EDT should not be blocked since paint operations and events would also be blocked
/// in much the same way as they would be in other platforms. To serialize calls back
/// into the EDT, use the methods `Display#callSerially` & `Display#callSeriallyAndWait`.
///
/// Notice that all Codename One calls occur on the EDT (events, painting, animations, etc...), Codename One
/// should normally be manipulated on the EDT as well (hence the `Display#callSerially` &
/// `Display#callSeriallyAndWait` methods). Theoretically, it should be possible to manipulate
/// some Codename One features from other threads, but this can't be guaranteed to work for all use cases.
///
/// @author Chen Fishbein, Shai Almog
public final class Display extends CN1Constants {
    /// A common sound type that can be used with playBuiltinSound
    public static final String SOUND_TYPE_ALARM = "alarm";
    /// A common sound type that can be used with playBuiltinSound
    public static final String SOUND_TYPE_CONFIRMATION = "confirmation";
    /// A common sound type that can be used with playBuiltinSound
    public static final String SOUND_TYPE_ERROR = "error";
    /// A common sound type that can be used with playBuiltinSound
    public static final String SOUND_TYPE_INFO = "info";
    /// A common sound type that can be used with playBuiltinSound
    public static final String SOUND_TYPE_WARNING = "warning";
    /// A common sound type that can be used with playBuiltinSound
    public static final String SOUND_TYPE_BUTTON_PRESS = "press";
    /// Unknown keyboard type is the default indicating the software should try
    /// to detect the keyboard type if necessary
    public static final int KEYBOARD_TYPE_UNKNOWN = 0;
    /// Numeric keypad keyboard type
    public static final int KEYBOARD_TYPE_NUMERIC = 1;
    /// Full QWERTY keypad keyboard type, even if a numeric keyboard also exists
    public static final int KEYBOARD_TYPE_QWERTY = 2;
    /// A touch based device that doesn't have a physical keyboard. Such a device pops up a virtual keyboad.
    public static final int KEYBOARD_TYPE_VIRTUAL = 3;
    /// Half-QWERTY which needs software assistance for completion
    public static final int KEYBOARD_TYPE_HALF_QWERTY = 4;
    /// Game action for fire
    public static final int GAME_FIRE = 8;
    /// Game action for the left key
    public static final int GAME_LEFT = 2;
    /// Game action for right key
    public static final int GAME_RIGHT = 5;
    /// Game action for UP key
    public static final int GAME_UP = 1;
    /// Game action for down key
    public static final int GAME_DOWN = 6;
    /// Special case game key used for media playback events
    public static final int MEDIA_KEY_SKIP_FORWARD = 20;
    /// Special case game key used for media playback events
    public static final int MEDIA_KEY_SKIP_BACK = 21;
    /// Special case game key used for media playback events
    public static final int MEDIA_KEY_PLAY = 22;
    /// Special case game key used for media playback events
    public static final int MEDIA_KEY_STOP = 23;
    /// Special case game key used for media playback events
    public static final int MEDIA_KEY_PLAY_STOP = 24;
    /// Special case game key used for media playback events
    public static final int MEDIA_KEY_PLAY_PAUSE = 25;
    /// Special case game key used for media playback events
    public static final int MEDIA_KEY_FAST_FORWARD = 26;
    /// Special case game key used for media playback events
    public static final int MEDIA_KEY_FAST_BACKWARD = 27;
    /// An attribute that encapsulates '#' int value.
    public static final int KEY_POUND = '#';
    /// Ignore all calls to show occurring during edit, they are discarded immediately
    public static final int SHOW_DURING_EDIT_IGNORE = 1;
    /// If show is called while editing text in the native text box an exception is thrown
    public static final int SHOW_DURING_EDIT_EXCEPTION = 2;
    /// Allow show to occur during edit and discard all user input at this moment
    public static final int SHOW_DURING_EDIT_ALLOW_DISCARD = 3;
    /// Allow show to occur during edit and save all user input at this moment
    public static final int SHOW_DURING_EDIT_ALLOW_SAVE = 4;
    /// Show will update the current form to which the OK button of the text box
    /// will return
    public static final int SHOW_DURING_EDIT_SET_AS_NEXT = 5;
    /// Indicates that the Codename One implementation should decide internally the command
    /// behavior most appropriate for this platform.
    public static final int COMMAND_BEHAVIOR_DEFAULT = 1;
    /// Indicates the classic Codename One command behavior where the commands are placed in
    /// a list within a dialog. This is the most customizable approach for none touch devices.
    public static final int COMMAND_BEHAVIOR_SOFTKEY = 2;
    /// Indicates the touch menu dialog rendered by Codename One where commands are placed
    /// into a scrollable dialog
    public static final int COMMAND_BEHAVIOR_TOUCH_MENU = 3;
    /// Indicates that commands should be added to an always visible bar at the
    /// bottom of the form.
    public static final int COMMAND_BEHAVIOR_BUTTON_BAR = 4;
    /// Identical to the bar behavior, places the back command within the title bar
    /// of the form/dialg
    public static final int COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_BACK = 5;
    /// Places all commands on the right side of the title bar with a uniform size
    /// grid layout
    public static final int COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_RIGHT = 6;
    /// Commands are placed in the same was as they are in the ice cream sandwich Android
    /// OS update where the back button has a theme icon the application icon appears next
    /// to the
    public static final int COMMAND_BEHAVIOR_ICS = 7;
    /// Commands are placed in a side menu similar to Facebook/Google+ apps
    public static final int COMMAND_BEHAVIOR_SIDE_NAVIGATION = 8;
    /// Indicates that commands should try to add themselves to the native menus
    public static final int COMMAND_BEHAVIOR_NATIVE = 10;
    /// Client property key used on the first shown `Form` to indicate the desired initial
    /// window size as a percentage of the available desktop. The value should be a `com.codename1.ui.geom.Dimension`
    /// whose width and height represent percentages.
    public static final String WINDOW_SIZE_HINT_PERCENT = "cn1.windowSizePercent";
    static final Display INSTANCE = new Display();
    static final Object lock = new Object();
    private static final int POINTER_PRESSED = 1;
    private static final int POINTER_RELEASED = 2;
    private static final int POINTER_DRAGGED = 3;
    private static final int POINTER_HOVER = 8;
    private static final int POINTER_HOVER_RELEASED = 11;
    private static final int POINTER_HOVER_PRESSED = 12;
    private static final int KEY_PRESSED = 4;
    private static final int KEY_RELEASED = 5;
    private static final int SIZE_CHANGED = 7;
    private static final int HIDE_NOTIFY = 9;
    private static final int SHOW_NOTIFY = 10;
    private static final int POINTER_PRESSED_MULTI = 21;
    private static final int POINTER_RELEASED_MULTI = 22;
    private static final int POINTER_DRAGGED_MULTI = 23;
    private static final int MAX_ASYNC_EXCEPTION_DEPTH = 10;
    private static final int[] xArray1 = new int[1];
    private static final int[] yArray1 = new int[1];
    private static final Map<String, VirtualKeyboardInterface> virtualKeyboards = new HashMap<String, VirtualKeyboardInterface>();
    static CodenameOneImplementation impl;
    private final LinkedList<Runnable> runningSerialCallsQueue = new LinkedList<Runnable>();
    /// Contains the call serially pending elements
    private final ArrayList<Runnable> pendingSerialCalls = new ArrayList<Runnable>();
    /// Contains the call serially idle elements
    private final ArrayList<Runnable> pendingIdleSerialCalls = new ArrayList<Runnable>();
    boolean codenameOneExited;
    long time;
    private int transitionDelay = -1;
    private String selectedVirtualKeyboard = null;
    private CrashReport crashReporter;
    private EventDispatcher errorHandler;
    private boolean inNativeUI;
    private Runnable bookmark;
    private EventDispatcher messageListeners;
    private EventDispatcher windowListeners;
    /// Tracks whether the initial window size hint has already been consumed for the first shown form.
    private boolean initialWindowSizeApplied;
    private boolean disableInvokeAndBlock;
    /// Enable Async stack traces.  This is disabled by default, but will cause
    /// stack traces of callSerially() calls to be stored, and logged if the
    /// Runnable throws an exception.
    private boolean enableAsyncStackTraces;
    /// A pure touch device has no focus showing when the user is using the touch
    /// interface. Selection only shows when the user actually touches the screen
    /// or suddenly switches to using a keypad/trackball. This sort of interface
    /// is common in Android devices
    private boolean pureTouch;
    private Graphics codenameOneGraphics;
    /// Indicates whether this is a touch device
    private boolean touchScreen;
    private HashMap<String, String> localProperties;
    /// Indicates whether the edt should sleep between each loop
    private boolean noSleep = false;
    /// Normally Codename One folds the VKB when switching forms this field allows us
    /// to block that behavior.
    private boolean autoFoldVKBOnFormSwitch = true;
    /// Indicates the maximum drawing speed of no more than 10 frames per second
    /// by default (this can be increased or decreased) the advantage of limiting
    /// framerate is to allow the CPU to perform other tasks besides drawing.
    /// Notice that when no change is occurring on the screen, no frame is drawn and
    /// so a high/low FPS will have no effect then.
    private int framerateLock = 15;
    private boolean codenameOneRunning = false;
    /// This is the instance of the EDT used internally to indicate whether
    /// we are executing on the EDT or some arbitrary thread
    private Thread edt;
    /// Contains animations that must be played in full by the EDT before anything further
    /// may be processed. This is useful for transitions/intro's etc... that animate without
    /// user interaction.
    private ArrayList<Animation> animationQueue;
    /// Indicates whether the 3rd softbutton should be supported on this device
    private boolean thirdSoftButton = false;
    private int showDuringEdit;
    /// Events to broadcast on the EDT, we are using a handcoded stack for maximum
    /// performance and minimal synchronization. We are using the switching algorithm
    /// where we only synchronize on the very minimal point of switching between the stacks
    /// and adding to the active stack.
    private int[] inputEventStack = new int[1000];
    private int inputEventStackPointer;
    private int[] inputEventStackTmp = new int[1000];
    private int inputEventStackPointerTmp;
    private boolean longPointerCharged;
    private boolean pointerPressedAndNotReleasedOrDragged;
    private boolean recursivePointerReleaseA;
    private boolean recursivePointerReleaseB;
    private int pointerX;
    private int pointerY;
    private boolean keyRepeatCharged;
    private boolean longPressCharged;
    private long longKeyPressTime;
    private int longPressInterval = 500;
    private long nextKeyRepeatEvent;
    private int keyRepeatValue;
    private boolean lastInteractionWasKeypad;
    private boolean dragOccured;
    private boolean processingSerialCalls;
    private int PATHLENGTH;
    private float[] dragPathX;
    private float[] dragPathY;
    private long[] dragPathTime;
    private int dragPathOffset = 0;
    private int dragPathLength = 0;
    private Boolean darkMode;
    private PluginSupport pluginSupport;
    /// Internally track display initialization time as a fixed point to allow tagging of pointer
    /// events with an integer timestamp (System.currentTimeMillis() - displayInitTime)
    /// and not a long value.
    private long displayInitTime = 0;
    /// Allows a Codename One application to minimize without forcing it to the front whenever
    /// a new dialog is poped up
    private boolean allowMinimizing;
    private boolean dropEvents;
    private ArrayList<Runnable> backgroundTasks;
    private Thread backgroundThread;
    private boolean multiKeyMode;
    private ActionListener virtualKeyboardListener;
    private EventDispatcher virtualKeyboardListeners;
    private int lastSizeChangeEventWH = -1;
    private DebugRunnable currentEdtContext;
    private int previousKeyPressed;
    private int lastKeyPressed;
    private int lastDragOffset;

    // huge false positive from PMD...
    @SuppressWarnings("PMD.SingularField")
    private Form eventForm;

    /// Private constructor to prevent instanciation
    private Display() {
    }

    /// This is the INTERNAL Display initialization method, it will be removed in future versions of the API.
    /// This method must be called before any Form is shown
    ///
    /// #### Parameters
    ///
    /// - `m`: platform specific object used by the implementation
    ///
    /// #### Deprecated
    ///
    /// this method is invoked internally do not invoke it!
    public static void init(Object m) {
        if (!INSTANCE.codenameOneRunning) {
            INSTANCE.codenameOneRunning = true;
            INSTANCE.initialWindowSizeApplied = false;
            INSTANCE.pluginSupport = new PluginSupport();
            INSTANCE.displayInitTime = System.currentTimeMillis();

            //restore menu state from previous run if exists
            int commandBehaviour = COMMAND_BEHAVIOR_DEFAULT;
            if (impl != null) {
                commandBehaviour = impl.getCommandBehavior();
            }
            impl = (CodenameOneImplementation) ImplementationFactory.getInstance().createImplementation();

            impl.setDisplayLock(lock);
            impl.initImpl(m);
            INSTANCE.codenameOneGraphics = new Graphics(impl.getNativeGraphics());
            INSTANCE.codenameOneGraphics.paintPeersBehind = impl.paintNativePeersBehind();
            impl.setCodenameOneGraphics(INSTANCE.codenameOneGraphics);

            // only enable but never disable the third softbutton
            if (impl.isThirdSoftButton()) {
                INSTANCE.thirdSoftButton = true;
            }
            if (impl.getSoftkeyCount() > 0) {
                MenuBar.leftSK = impl.getSoftkeyCode(0)[0];
                if (impl.getSoftkeyCount() > 1) {
                    MenuBar.rightSK = impl.getSoftkeyCode(1)[0];
                    if (impl.getSoftkeyCode(1).length > 1) {
                        MenuBar.rightSK2 = impl.getSoftkeyCode(1)[1];
                    }
                }
            }
            MenuBar.backSK = impl.getBackKeyCode();
            MenuBar.backspaceSK = impl.getBackspaceKeyCode();
            MenuBar.clearSK = impl.getClearKeyCode();

            INSTANCE.PATHLENGTH = impl.getDragPathLength();
            INSTANCE.dragPathX = new float[INSTANCE.PATHLENGTH];
            INSTANCE.dragPathY = new float[INSTANCE.PATHLENGTH];
            INSTANCE.dragPathTime = new long[INSTANCE.PATHLENGTH];
            com.codename1.util.StringUtil.setImplementation(impl);
            Util.setImplementation(impl);

            // this can happen on some cases where an application was restarted etc...
            // generally its probably a bug but we can let it slide...
            if (INSTANCE.edt == null) {
                INSTANCE.touchScreen = impl.isTouchDevice();
                // initialize the Codename One EDT which from now on will take all responsibility
                // for the event delivery.
                INSTANCE.edt = new CodenameOneThread(new RunnableWrapper(null, 3), "EDT");
                impl.setThreadPriority(INSTANCE.edt, impl.getEDTThreadPriority());
                INSTANCE.edt.start();
            }
            impl.postInit();
            INSTANCE.setCommandBehavior(commandBehaviour);
        } else {
            impl.confirmControlView();
        }
    }

    /// Closes down the EDT and Codename One, under normal conditions this method is completely unnecessary
    /// since exiting the application will shut down Codename One. However, if the application is minimized
    /// and the user wishes to free all resources without exiting the application then this method can be used.
    /// Once this method is used Codename One will no longer work and Display.init(Object) should be invoked
    /// again for any further Codename One call!
    /// Notice that minimize (being a Codename One method) MUST be invoked before invoking this method!
    public static void deinitialize() {
        synchronized (lock) {
            INSTANCE.codenameOneRunning = false;
            lock.notifyAll();
        }
    }

    /// This method returns true if the Display is initialized.
    ///
    /// #### Returns
    ///
    /// true if the EDT is running
    public static boolean isInitialized() {
        return INSTANCE.codenameOneRunning && (impl != null && impl.isInitialized());
    }

    /// Return the Display instance
    ///
    /// #### Returns
    ///
    /// the Display instance
    public static Display getInstance() {
        return INSTANCE;
    }

    /// Sets a bookmark that can restore the app to a particular state.  This takes a
    /// `Runnable` that will be run when `#restoreToBookmark()` () } is called.
    ///
    /// The primary purpose of this feature is live code refresh.
    ///
    /// #### Parameters
    ///
    /// - `bookmark`: A `Runnable` that can be run to restore the app to a particular point.
    ///
    /// #### Since
    ///
    /// 8.0
    public void setBookmark(Runnable bookmark) {
        this.bookmark = bookmark;
    }

    /// Runs the last bookmark that was set using `#setBookmark(java.lang.Runnable)`
    ///
    /// #### Since
    ///
    /// 8.0
    public void restoreToBookmark() {
        if (this.bookmark != null) {
            this.bookmark.run();
        }
    }

    /// Gets reference to plugin support object.
    ///
    /// #### Returns
    ///
    /// The plugin support object.
    ///
    /// #### Since
    ///
    /// 8.0
    public PluginSupport getPluginSupport() {
        return pluginSupport;
    }

    /// This method allows us to manipulate the drag started detection logic.
    /// If the pointer was dragged for more than this percentage of the display size it
    /// is safe to assume that a drag is in progress.
    ///
    /// #### Returns
    ///
    /// motion percentage
    public int getDragStartPercentage() {
        return getImplementation().getDragStartPercentage();
    }

    /// This method allows us to manipulate the drag started detection logic.
    /// If the pointer was dragged for more than this percentage of the display size it
    /// is safe to assume that a drag is in progress.
    ///
    /// #### Parameters
    ///
    /// - `dragStartPercentage`: percentage of the screen required to initiate drag
    public void setDragStartPercentage(int dragStartPercentage) {
        getImplementation().setDragStartPercentage(dragStartPercentage);
    }

    CodenameOneImplementation getImplementation() {
        return impl;
    }

    /// Indicates the maximum frames the API will try to draw every second
    /// by default this is set to 10. The advantage of limiting
    /// framerate is to allow the CPU to perform other tasks besides drawing.
    /// Notice that when no change is occurring on the screen no frame is drawn and
    /// so a high/low FPS will have no effect then.
    /// 10FPS would be very reasonable for a business application.
    ///
    /// #### Parameters
    ///
    /// - `rate`: the frame rate
    public void setFramerate(int rate) {
        framerateLock = 1000 / rate;
    }

    /// Vibrates the device for the given length of time, notice that this might ignore the time value completely
    /// on some OS's where this level of control isn't supported e.g. iOS see: https://github.com/codenameone/CodenameOne/issues/1904
    ///
    /// #### Parameters
    ///
    /// - `duration`: length of time to vibrate (might be ignored)
    public void vibrate(int duration) {
        impl.vibrate(duration);
    }

    /// Flash the backlight of the device for the given length of time
    ///
    /// #### Parameters
    ///
    /// - `duration`: length of time to flash the backlight
    ///
    /// #### Deprecated
    ///
    /// this refers to functionality of devices that are no longer sold, not to the devices "flash"
    public void flashBacklight(int duration) {
        impl.flashBacklight(duration);
    }

    /// Manually announces text to native accessibility services, optionally associating the
    /// announcement with a specific component. Most assistive technologies will announce a
    /// component automatically when it gains focus; this method is intended for situations
    /// where an announcement should occur independently of focus changes.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: the component related to this announcement or `null` for the root view
    ///
    /// - `text`: the message to announce
    public void announceForAccessibility(Component cmp, String text) {
        impl.announceForAccessibility(cmp, text);
    }

    /// Convenience overload to announce text without specifying a component.
    ///
    /// #### Parameters
    ///
    /// - `text`: the message to announce
    public void announceForAccessibility(String text) {
        announceForAccessibility(null, text);
    }

    /// Returns the status of the show during edit flag
    ///
    /// #### Returns
    ///
    /// @return one of the following: SHOW_DURING_EDIT_IGNORE,
    /// SHOW_DURING_EDIT_EXCEPTION, SHOW_DURING_EDIT_ALLOW_DISCARD,
    /// SHOW_DURING_EDIT_ALLOW_SAVE, SHOW_DURING_EDIT_SET_AS_NEXT
    ///
    /// #### Deprecated
    ///
    /// this method isn't applicable in modern devices
    public int getShowDuringEditBehavior() {
        return showDuringEdit;
    }

    /// Invoking the show() method of a form/dialog while the user is editing
    /// text in the native text box can have several behaviors: SHOW_DURING_EDIT_IGNORE,
    /// SHOW_DURING_EDIT_EXCEPTION, SHOW_DURING_EDIT_ALLOW_DISCARD,
    /// SHOW_DURING_EDIT_ALLOW_SAVE, SHOW_DURING_EDIT_SET_AS_NEXT
    ///
    /// #### Parameters
    ///
    /// - `showDuringEdit`: @param showDuringEdit one of the following: SHOW_DURING_EDIT_IGNORE,
    ///                       SHOW_DURING_EDIT_EXCEPTION, SHOW_DURING_EDIT_ALLOW_DISCARD,
    ///                       SHOW_DURING_EDIT_ALLOW_SAVE, SHOW_DURING_EDIT_SET_AS_NEXT
    ///
    /// #### Deprecated
    ///
    /// this method isn't applicable in modern devices
    public void setShowDuringEditBehavior(int showDuringEdit) {
        this.showDuringEdit = showDuringEdit;
    }

    /// Indicates the maximum frames the API will try to draw every second
    ///
    /// #### Returns
    ///
    /// the frame rate
    public int getFrameRate() {
        return 1000 / framerateLock;
    }

    /// Returns true if we are currently in the event dispatch thread.
    /// This is useful for generic code that can be used both with the
    /// EDT and outside of it.
    ///
    /// #### Returns
    ///
    /// @return true if we are currently in the event dispatch thread;
    /// otherwise false
    public boolean isEdt() {
        return edt == Thread.currentThread(); //NOPMD CompareObjectsWithEquals
    }

    /// Plays sound for the dialog
    void playDialogSound(final int type) {
        impl.playDialogSound(type);
    }

    /// Stops the remote control service.  This should be implemented in the platform
    /// to handle unbinding the `com.codename1.media.RemoteControlListener` with the platform's remote control.
    ///
    /// This is executed when a new listener is registered using `com.codename1.media.MediaManager#setRemoteControlListener(com.codename1.media.RemoteControlListener)`
    ///
    /// #### Since
    ///
    /// 7.0
    public void stopRemoteControl() {
        impl.stopRemoteControl();
    }

    /// Starts the remote control service.  This should be implemented
    /// in the platform to handle binding the `RemoteControlListener` with
    /// the platform's remote control.
    ///
    /// This is executed when the user registers a new listener using `MediaManager#setRemoteControlListener(com.codename1.media.RemoteControlListener)`
    ///
    /// #### Since
    ///
    /// 7.0
    public void startRemoteControl() {
        impl.startRemoteControl();
    }

    /// Returns true if the platform is in dark mode, null is returned for
    /// unknown status
    ///
    /// #### Returns
    ///
    /// true in case of dark mode
    public Boolean isDarkMode() {
        if (darkMode != null) {
            return darkMode;
        }
        return impl.isDarkMode();
    }

    /// Override the default dark mode setting
    ///
    /// #### Parameters
    ///
    /// - `darkMode`: can be set to null to reset to platform default
    public void setDarkMode(Boolean darkMode) {
        this.darkMode = darkMode;
    }

    /// Returns true if the user has selected larger type fonts in the system settings.
    ///
    /// #### Returns
    ///
    /// true when the platform indicates a larger text preference.
    ///
    /// #### Since
    ///
    /// 7.1
    public boolean isLargerTextEnabled() {
        return impl.isLargerTextEnabled();
    }

    /// Returns a scale factor representing how much larger system fonts should be.
    /// A value of `1.0` indicates the default system font size.
    ///
    /// #### Returns
    ///
    /// scale factor for larger system fonts.
    ///
    /// #### Since
    ///
    /// 7.1
    public float getLargerTextScale() {
        return impl.getLargerTextScale();
    }

    /// Checks if async stack traces are enabled.  If enabled, the stack trace
    /// at the point of `#callSerially(java.lang.Runnable)` calls will
    /// be recorded, and logged in the case that there is an uncaught exception.
    ///
    /// Currently this is only supported in the JavaSE/Simulator port.
    ///
    /// #### Returns
    ///
    /// Whether async stack traces are enabled.
    ///
    /// #### Since
    ///
    /// 7.0
    ///
    /// #### See also
    ///
    /// - #setEnableAsyncStackTraces(boolean)
    public boolean isEnableAsyncStackTraces() {
        return enableAsyncStackTraces;
    }

    /// Enables or disables async stack traces.  If enabled, the stack trace
    /// at the point of `#callSerially(java.lang.Runnable)` calls will
    /// be recorded, and logged in the case that there is an uncaught exception.
    ///
    /// Currently this is only supported in the JavaSE/Simulator port.
    ///
    /// #### Parameters
    ///
    /// - `enableAsyncStackTraces`: True to enable async stack traces.
    ///
    /// #### Since
    ///
    /// 7.0
    ///
    /// #### See also
    ///
    /// - #isEnableAsyncStackTraces()
    public void setEnableAsyncStackTraces(boolean enableAsyncStackTraces) {
        this.enableAsyncStackTraces = enableAsyncStackTraces;
    }

    /// Causes the runnable to be invoked on the event dispatch thread. This method
    /// returns immediately and will not wait for the serial call to occur
    ///
    /// #### Parameters
    ///
    /// - `r`: @param r runnable (NOT A THREAD!) that will be invoked on the EDT serial to
    ///          the paint and key handling events
    public void callSerially(Runnable r) {
        // otherwise this will fail in an odd locaiton. Better it fails here...
        if (r == null) {
            throw new NullPointerException();
        }
        if (codenameOneRunning) {
            synchronized (lock) {
                scheduleSerialCall(isEnableAsyncStackTraces() ? new DebugRunnable(r) : r);
                lock.notifyAll();
            }
        } else {
            r.run();
        }
    }

    // We factor out the scheduling of a serial call so that we can
    // use the Schedule annotation for IntelliJ async debugging https://www.jetbrains.com/help/idea/debug-asynchronous-code.html
    private void scheduleSerialCall(@Async.Schedule Runnable r) {
        pendingSerialCalls.add(r);
    }

    /// Causes the runnable to be invoked on the event dispatch thread when the event
    /// dispatch thread is idle. This method returns immediately and will not wait for the serial call
    /// to occur. Notice this method is identical to call serially but will perform the runnable only when
    /// the EDT is idle
    ///
    /// #### Parameters
    ///
    /// - `r`: @param r runnable (NOT A THREAD!) that will be invoked on the EDT serial to
    ///          the paint and key handling events
    public void callSeriallyOnIdle(Runnable r) {
        if (codenameOneRunning) {
            synchronized (lock) {
                pendingIdleSerialCalls.add(r);
                lock.notifyAll();
            }
        } else {
            r.run();
        }
    }

    public String getLineSeparator() {
        return impl.getLineSeparator();
    }

    /// Allows executing a background task in a separate low priority thread. Tasks are serialized
    /// so they don't overload the CPU.
    ///
    /// #### Parameters
    ///
    /// - `r`: the task to perform in the background
    public void scheduleBackgroundTask(@Async.Schedule Runnable r) {
        synchronized (lock) {
            if (backgroundTasks == null) {
                backgroundTasks = new ArrayList<Runnable>();
            }
            backgroundTasks.add(r);
            if (backgroundThread == null) {
                backgroundThread = new CodenameOneThread(new Runnable() {
                    @Override
                    public void run() {
                        // using while true to avoid double lock optimization with synchronized block
                        while (true) {
                            Runnable nextTask = null;
                            synchronized (lock) {
                                if (!backgroundTasks.isEmpty()) {
                                    nextTask = backgroundTasks.get(0);
                                } else {
                                    backgroundThread = null;
                                    return;
                                }
                                backgroundTasks.remove(0);
                            }
                            try {
                                executeBackgroundTaskRunnable(nextTask);
                            } catch (Throwable e) {
                                Log.e(e);
                            }
                            Util.sleep(10);
                        }
                    }
                }, "Task Thread");
                backgroundThread.setPriority(Thread.MIN_PRIORITY + 1);
                backgroundThread.start();
            }
        }
    }

    private void executeBackgroundTaskRunnable(@Async.Execute Runnable r) {
        r.run();
    }

    /// Identical to callSerially with the added benefit of waiting for the Runnable method to complete.
    ///
    /// #### Parameters
    ///
    /// - `r`: @param r runnable (NOT A THREAD!) that will be invoked on the EDT serial to
    ///          the paint and key handling events
    ///
    /// #### Throws
    ///
    /// - `IllegalStateException`: @throws IllegalStateException if this method is invoked on the event dispatch thread (e.g. during
    ///                               paint or event handling).
    public void callSeriallyAndWait(Runnable r) {
        if (isEdt()) {
            throw new RuntimeException("This method MUST NOT be invoked on the EDT");
        }
        RunnableWrapper c = new RunnableWrapper(r, 0);
        callSerially(c);
        flushEdt();
        synchronized (lock) {
            while (!c.isDone()) {
                try {
                    // poll doneness to prevent potential race conditions
                    lock.wait(50);
                } catch (InterruptedException err) {
                }
            }
        }
    }

    /// Checks if this platform uses input modes.  No current platforms return true for this.  It is a holdover from J2ME.
    ///
    /// #### Returns
    ///
    /// True if the platform supports input modes.  Only true for J2ME and RIM.
    public boolean platformUsesInputMode() {
        return impl.platformUsesInputMode();
    }

    /// Identical to callSerially with the added benefit of waiting for the Runnable method to complete.
    ///
    /// #### Parameters
    ///
    /// - `r`: @param r       runnable (NOT A THREAD!) that will be invoked on the EDT serial to
    ///                the paint and key handling events
    ///
    /// - `timeout`: timeout duration, on timeout the method just returns
    ///
    /// #### Throws
    ///
    /// - `IllegalStateException`: @throws IllegalStateException if this method is invoked on the event dispatch thread (e.g. during
    ///                               paint or event handling).
    public void callSeriallyAndWait(Runnable r, int timeout) {
        RunnableWrapper c = new RunnableWrapper(r, 0);
        callSerially(c);
        synchronized (lock) {
            long t = System.currentTimeMillis();
            while (!c.isDone()) {
                try {
                    // poll doneness to prevent potential race conditions
                    lock.wait(20);
                } catch (InterruptedException err) {
                }
                if (System.currentTimeMillis() - t >= timeout) {
                    return;
                }
            }
        }
    }

    /// Allows us to "flush" the edt to allow any pending transitions and input to go
    /// by before continuing with our other tasks.
    void flushEdt() {
        if (!isEdt()) {
            return;
        }
        while (!shouldEDTSleepNoFormAnimation()) {
            edtLoopImpl();
        }
        while (animationQueue != null && !animationQueue.isEmpty()) {
            edtLoopImpl();
        }
    }

    /// Restores the menu in the given form
    private void restoreMenu(Form f) {
        if (f != null) {
            f.restoreMenu();
        }
    }

    /// Returns true if the system is currently in the process of transitioning between
    /// forms
    ///
    /// #### Returns
    ///
    /// true if in the middle of form transition
    public boolean isInTransition() {
        if (animationQueue != null && !animationQueue.isEmpty()) {
            return animationQueue.get(0) instanceof Transition;
        }
        return false;
    }

    // Seems to be a false positive on this rule
    @SuppressWarnings({"PMD.SimplifyConditional", "PMD.AvoidBranchingStatementAsLastInLoop"})
    private void paintTransitionAnimation() {
        Animation ani = animationQueue.get(0);
        if (!ani.animate()) {
            animationQueue.remove(0);
            if (ani instanceof Transition) {
                Form source = (Form) ((Transition) ani).getSource();
                restoreMenu(source);

                if (!animationQueue.isEmpty()) {
                    ani = animationQueue.get(0);
                    if (ani instanceof Transition) {
                        ((Transition) ani).initTransition();
                    }
                } else {
                    Form f = (Form) ((Transition) ani).getDestination();
                    restoreMenu(f);
                    if (source == null || source == impl.getCurrentForm() || source == getCurrent()) { //NOPMD CompareObjectsWithEquals
                        setCurrentForm(f);
                    }
                    ((Transition) ani).cleanup();
                }
                return;
            }
        }
        ani.paint(codenameOneGraphics);

        impl.flushGraphics();

        if (transitionDelay > 0) {
            // yield for a fraction, some devices don't "properly" implement
            // flush and so require the painting thread to get CPU too.
            try {
                synchronized (lock) {
                    long end = System.currentTimeMillis() + transitionDelay;
                    while (true) {
                        long remaining = end - System.currentTimeMillis();
                        if (remaining <= 0) {
                            break;
                        }
                        lock.wait(remaining);
                        break;
                    }
                }
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    /// This method represents the event thread for the UI library on which
    /// all events are carried out. It differs from the MIDP event thread to
    /// prevent blocking of actual input and drawing operations. This also
    /// enables functionality such as "true" modal dialogs etc...
    void mainEDTLoop() {
        impl.initEDT();
        UIManager.getInstance();
        try {
            // when there is no current form the EDT is useful only
            // for features such as call serially
            while (impl.getCurrentForm() == null) { // PMD Fix: AvoidBranchingStatementAsLastInLoop
                synchronized (lock) {
                    while (shouldEDTSleep() && pendingIdleSerialCalls.isEmpty()) {
                        try {
                            lock.wait();
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                    if (shouldEDTSleep() && !pendingIdleSerialCalls.isEmpty()) {
                        Runnable r = pendingIdleSerialCalls.remove(0);
                        callSerially(r);
                    }

                    // paint transition or intro animations and don't do anything else if such
                    // animations are in progress...
                    if (animationQueue != null && !animationQueue.isEmpty()) {
                        paintTransitionAnimation();
                        continue;
                    }
                }
                processSerialCalls();
            }
        } catch (Throwable err) {
            Log.e(err);
            if (crashReporter != null) {
                crashReporter.exception(err);
            }
            if (!impl.handleEDTException(err)) {
                if (errorHandler != null) {
                    errorHandler.fireActionEvent(new ActionEvent(err, ActionEvent.Type.Exception));
                } else {
                    Dialog.show("Error", "An internal application error occurred: " + err, "OK", null);
                }
            }
        }

        while (codenameOneRunning) { // PMD Fix: AvoidBranchingStatementAsLastInLoop
            try {
                // wait indefinetly Lock surrounds the should method to prevent serial calls from
                // getting "lost"
                synchronized (lock) {
                    if (shouldEDTSleep()) {
                        if (!pendingIdleSerialCalls.isEmpty()) {
                            Runnable r = pendingIdleSerialCalls.remove(0);
                            callSerially(r);
                        } else {
                            impl.edtIdle(true);
                            while (shouldEDTSleep() && pendingIdleSerialCalls.isEmpty()) {
                                try {
                                    lock.wait();
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                    break;
                                }
                            }
                            impl.edtIdle(false);
                            if (!pendingIdleSerialCalls.isEmpty()) {
                                Runnable r = pendingIdleSerialCalls.remove(0);
                                callSerially(r);
                            }
                        }
                    }
                }


                edtLoopImpl();
            } catch (Throwable err) {
                if (!codenameOneRunning) {
                    return;
                }
                Log.e(err);
                if (crashReporter != null) {
                    CodenameOneThread.handleException(err);
                }
                if (!impl.handleEDTException(err)) {
                    if (errorHandler != null) {
                        errorHandler.fireActionEvent(new ActionEvent(err, ActionEvent.Type.Exception));
                    } else {
                        Dialog.show("Error", "An internal application error occurred: " + err, "OK", null);
                    }
                }
            }
        }
        impl.deinitialize();
        //INSTANCE.impl = null;
        //INSTANCE.codenameOneGraphics = null;
        INSTANCE.edt = null;
    }

    /// Returns the stack trace from the exception on the given
    /// thread. This API isn't supported on all platforms and may
    /// return a blank string when unavailable.
    ///
    /// #### Parameters
    ///
    /// - `parentThread`: the thread in which the exception was thrown
    ///
    /// - `t`: the exception
    ///
    /// #### Returns
    ///
    /// a stack trace string that might be blank
    public String getStackTrace(Thread parentThread, Throwable t) {
        System.out.println("CN1SS:ERR:Invoking getStackTrace in Display");
        return impl.getStackTrace(parentThread, t);
    }

    /// Implementation of the event dispatch loop content
    void edtLoopImpl() {
        try {
            // transitions shouldn't be bound by framerate
            if (animationQueue == null || animationQueue.isEmpty()) {
                // prevents us from waking up the EDT too much and
                // thus exhausting the systems resources. The + 1
                // prevents us from ever waiting 0 milliseconds which
                // is the same as waiting with no time limit
                if (!noSleep) {
                    synchronized (lock) {
                        impl.edtIdle(true);
                        long waitTime = Math.max(1, framerateLock - (time));
                        long end = System.currentTimeMillis() + waitTime;
                        while (true) {
                            long remaining = end - System.currentTimeMillis();
                            if (remaining <= 0) {
                                break;
                            }
                            try {
                                lock.wait(remaining);
                                break;
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                        impl.edtIdle(false);
                    }
                }
            } else {
                // paint transition or intro animations and don't do anything else if such
                // animations are in progress...
                paintTransitionAnimation();
                return;
            }
        } catch (RuntimeException ignor) {
            Log.e(ignor);
        }
        long currentTime = System.currentTimeMillis();

        // minimal amount of sync, just flipping the stack pointers
        synchronized (lock) {
            inputEventStackPointerTmp = inputEventStackPointer;
            inputEventStackPointer = 0;
            lastDragOffset = -1;
            int[] qt = inputEventStackTmp;
            inputEventStackTmp = inputEventStack;

            // We have a special flag here for a case where the input event stack might still be processing this can
            // happen if an event callback calls something like invokeAndBlock while processing and might reach
            // this code again
            if (qt[qt.length - 1] == Integer.MAX_VALUE) {
                inputEventStack = new int[qt.length];
            } else {
                inputEventStack = qt;
                qt[qt.length - 1] = 0;
            }
        }

        // we copy the variables to the stack since the array might be replaced while we are working if the EDT
        // is nested into an "invokeAndBlock"
        int actualTmpPointer = inputEventStackPointerTmp;
        inputEventStackPointerTmp = 0;
        int[] actualStack = inputEventStackTmp;
        int offset = 0;
        actualStack[actualStack.length - 1] = Integer.MAX_VALUE;
        while (offset < actualTmpPointer) {
            offset = handleEvent(offset, actualStack);
        }

        actualStack[actualStack.length - 1] = 0;

        if (!impl.isInitialized()) {
            return;
        }
        codenameOneGraphics.setGraphics(impl.getNativeGraphics());
        Form current = impl.getCurrentForm();
        if (current != null) {
            // Revalidate components that registered to be revalidated
            // before the next paint cycle.
            current.flushRevalidateQueue();
        }
        impl.paintDirty();

        // draw the animations

        if (current != null) {
            current.repaintAnimations();
            // check key repeat events
            long t = System.currentTimeMillis();
            if (keyRepeatCharged && nextKeyRepeatEvent <= t) {
                current.keyRepeated(keyRepeatValue);
                int keyRepeatNextIntervalTime = 10;
                nextKeyRepeatEvent = t + keyRepeatNextIntervalTime;
            }
            if (longPressCharged && longPressInterval <= t - longKeyPressTime) {
                longPressCharged = false;
                current.longKeyPress(keyRepeatValue);
            }
            if (longPointerCharged && longPressInterval <= t - longKeyPressTime) {
                longPointerCharged = false;
                current.longPointerPress(pointerX, pointerY);
            }
        }
        processSerialCalls();

        time = System.currentTimeMillis() - currentTime;
    }

    boolean hasNoSerialCallsPending() {
        return pendingSerialCalls.isEmpty();
    }

    /// Called by the underlying implementation to indicate that editing in the native
    /// system has completed and changes should propagate into Codename One
    ///
    /// #### Parameters
    ///
    /// - `c`: edited component
    ///
    /// - `text`: new text for the component
    public void onEditingComplete(final Component c, final String text) {
        if (!isEdt() && codenameOneRunning) {
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    onEditingComplete(c, text);
                }
            });
            return;
        }
        c.onEditComplete(text);
        c.fireActionEvent();
    }

    /// Used by the EDT to process all the calls submitted via call serially
    void processSerialCalls() {
        processingSerialCalls = true;
        int size = pendingSerialCalls.size();
        if (size > 0) {
            //Runnable[] array = null;
            synchronized (lock) {
                size = pendingSerialCalls.size();
                //array = new Runnable[size];

                // copy all elements to an array and remove them otherwise invokeAndBlock from
                // within a callSerially() can cause an infinite loop...
                //pendingSerialCalls.toArray(array);
                runningSerialCallsQueue.addAll(pendingSerialCalls);

                if (size == pendingSerialCalls.size()) {
                    // this is faster
                    pendingSerialCalls.clear();
                } else {
                    // this can occur if an element was added during the loop
                    for (int iter = 0; iter < size; iter++) {
                        pendingSerialCalls.remove(0);
                    }
                }
            }
            while (!runningSerialCallsQueue.isEmpty()) {
                executeSerialCall(runningSerialCallsQueue.remove(0));
            }

            // after finishing an event cycle there might be serial calls waiting
            // to return.
            synchronized (lock) {
                lock.notifyAll();
            }
        }
        processingSerialCalls = false;
    }

    // Executes a Runnable from a pending serial call. We wrap it in its
    // own function so we can use the Async.Execute annotation for debugging.
    // https://www.jetbrains.com/help/idea/debug-asynchronous-code.html
    private void executeSerialCall(@Async.Execute Runnable r) {
        r.run();
    }

    boolean isProcessingSerialCalls() {
        return processingSerialCalls;
    }

    void notifyDisplay() {
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    /// Invokes a Runnable with blocking disabled.  If any attempt is made to block
    /// (i.e. call `#invokeAndBlock(java.lang.Runnable)` from inside this Runnable,
    /// it will result in a `BlockingDisallowedException` being thrown.
    ///
    /// #### Parameters
    ///
    /// - `r`: Runnable to be run immediately.
    ///
    /// #### Throws
    ///
    /// - `BlockingDisallowedException`: @throws BlockingDisallowedException If `#invokeAndBlock(java.lang.Runnable)` is attempted
    ///                                     anywhere in the Runnable.
    ///
    /// #### Since
    ///
    /// 7.0
    public void invokeWithoutBlocking(Runnable r) {
        if (disableInvokeAndBlock || !isEdt()) {
            r.run();
        } else {
            disableInvokeAndBlock = true;
            try {
                r.run();
            } finally {
                disableInvokeAndBlock = false;
            }
        }
    }

    /// Invokes a RunnableWithResultSync with blocking disabled.  If any attempt is made to block
    /// (i.e. call `#invokeAndBlock(java.lang.Runnable)` from inside this Runnable,
    /// it will result in a `BlockingDisallowedException` being thrown.
    ///
    /// #### Parameters
    ///
    /// - `r`: RunnableWithResultSync to be run immediately.
    ///
    /// #### Throws
    ///
    /// - `BlockingDisallowedException`: @throws BlockingDisallowedException If `#invokeAndBlock(java.lang.Runnable)` is attempted
    ///                                     anywhere in the Runnable.
    ///
    /// #### Since
    ///
    /// 7.0
    public <T> T invokeWithoutBlockingWithResultSync(RunnableWithResultSync<T> r) {
        if (disableInvokeAndBlock || !isEdt()) {
            return r.run();
        } else {
            disableInvokeAndBlock = true;
            try {
                return r.run();
            } finally {
                disableInvokeAndBlock = false;
            }
        }
    }

    /// Invokes runnable and blocks the current thread, if the current thread is the
    /// EDT it will still be blocked in a way that doesn't break event dispatch .
    /// **Important:** calling this method spawns a new thread that shouldn't access the UI!
    ///
    /// See [this section](https://www.codenameone.com/manual/edt.html#_invoke_and_block) in the developer guide for further information.
    ///
    /// #### Parameters
    ///
    /// - `r`: runnable (NOT A THREAD!) that will be invoked synchronously by this method
    ///
    /// - `dropEvents`: @param dropEvents indicates if the display should drop all events
    ///                   while this runnable is running
    ///
    /// #### Throws
    ///
    /// - `BlockingDisallowedException`: @throws BlockingDisallowedException if this method is called while blocking is disabled (i.e. we are running
    ///                                     inside a call to `#invokeWithoutBlocking(java.lang.Runnable)` on the EDT).
    public void invokeAndBlock(Runnable r, boolean dropEvents) {
        this.dropEvents = dropEvents;
        try {
            if (isEdt()) {
                if (disableInvokeAndBlock) {
                    throw new BlockingDisallowedException();
                }
                // this class allows a runtime exception to propogate correctly out of the
                // internal thread
                RunnableWrapper w = new RunnableWrapper(r, 1);
                RunnableWrapper.pushToThreadPool(w);

                synchronized (lock) {
                    // prevent an invoke and block loop from breaking the ongoing event processing
                    if (inputEventStackPointerTmp > 0) {
                        inputEventStackPointerTmp = inputEventStackPointer;
                    }
                    try {
                        // yield the CPU for a very short time to let the invoke thread
                        // get started
                        lock.wait(2);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }

                    while (!runningSerialCallsQueue.isEmpty()) {
                        pendingSerialCalls.add(0, runningSerialCallsQueue.removeLast());
                    }
                }


                // loop over the EDT until the thread completes then return
                while (!w.isDone() && codenameOneRunning) {
                    edtLoopImpl();
                    synchronized (lock) {
                        if (shouldEDTSleep()) {
                            impl.edtIdle(true);
                            try {
                                lock.wait(10);
                            } catch (InterruptedException ex) {
                            }
                            impl.edtIdle(false);
                        }
                    }
                }
                // if the thread thew an exception we need to throw it onwards
                if (w.getErr() != null) {
                    throw w.getErr();
                }
            } else {
                r.run();
            }
        } catch (BlockingDisallowedException re) {
            Log.e(re);
            throw re;
        } catch (RuntimeException re) {
            throw re;
        } finally {
            this.dropEvents = false;
        }
    }

    /// Invokes runnable and blocks the current thread, if the current thread is the
    /// EDT it will still be blocked in a way that doesn't break event dispatch .
    /// **Important:** calling this method spawns a new thread that shouldn't access the UI!
    ///
    /// See [this section](https://www.codenameone.com/manual/edt.html#_invoke_and_block) in the developer guide for further information.
    ///
    /// #### Parameters
    ///
    /// - `r`: runnable (NOT A THREAD!) that will be invoked synchroniously by this method
    public void invokeAndBlock(Runnable r) {
        invokeAndBlock(r, false);
    }

    /// The name of this method is misleading due to it's legacy. It will return true on the desktop too where
    /// the mouse sends pointer events.
    ///
    /// #### Returns
    ///
    /// true if this device supports touch/pointer events
    public boolean isTouchScreenDevice() {
        return touchScreen;
    }

    /// Indicates if this is a touch screen device that will return pen events,
    /// defaults to true if the device has pen events but can be overriden by
    /// the developer.
    ///
    /// #### Parameters
    ///
    /// - `touchScreen`: false if this is not a touch screen device
    public void setTouchScreenDevice(boolean touchScreen) {
        this.touchScreen = touchScreen;
    }

    /// Calling this method with noSleep=true will cause the edt to run without sleeping.
    ///
    /// #### Parameters
    ///
    /// - `noSleep`: causes the edt to stop the sleeping periods between 2 cycles
    public void setNoSleep(boolean noSleep) {
        this.noSleep = noSleep;
    }

    /// Displays the given Form on the screen.
    ///
    /// #### Parameters
    ///
    /// - `newForm`: the Form to Display
    void setCurrent(final Form newForm, boolean reverse) {
        if (edt == null) {
            throw new IllegalStateException("Initialize must be invoked before setCurrent!");
        }
        Form current = impl.getCurrentForm();


        if (autoFoldVKBOnFormSwitch && !(newForm instanceof Dialog)) {
            setShowVirtualKeyboard(false);
        }

        if (current == newForm) { //NOPMD CompareObjectsWithEquals
            current.revalidate();
            current.repaint();
            current.onShowCompletedImpl();
            return;
        }

        if (impl.isEditingText()) {
            switch (showDuringEdit) {
                case SHOW_DURING_EDIT_ALLOW_DISCARD:
                    break;
                case SHOW_DURING_EDIT_ALLOW_SAVE:
                    impl.saveTextEditingState();
                    break;
                case SHOW_DURING_EDIT_EXCEPTION:
                    throw new IllegalStateException("Show during edit");
                case SHOW_DURING_EDIT_IGNORE:
                    return;
                case SHOW_DURING_EDIT_SET_AS_NEXT:
                    impl.setCurrentForm(newForm);
                    return;
                default:
                    break;
            }
        }

        if (!isEdt()) {
            callSerially(new RunnableWrapper(newForm, null, reverse));
            return;
        }

        if (current != null) {
            if (current.isInitialized()) {
                current.deinitializeImpl();
            } else {
                Form fg = getCurrentUpcoming();
                if (fg != current) { //NOPMD CompareObjectsWithEquals
                    if (fg.isInitialized()) {
                        fg.deinitializeImpl();
                    }
                }
            }
        }
        if (!newForm.isInitialized()) {
            newForm.initComponentImpl();
        }

        if (newForm.getWidth() != getDisplayWidth() || newForm.getHeight() != getDisplayHeight()) {
            newForm.setSize(new Dimension(getDisplayWidth(), getDisplayHeight()));
            newForm.setShouldCalcPreferredSize(true);
            newForm.layoutContainer();
            newForm.revalidate();
        } else {
            // if shouldLayout is true
            newForm.layoutContainer();
            newForm.revalidate();

        }

        boolean transitionExists = false;
        if (animationQueue != null && !animationQueue.isEmpty()) {
            Object o = animationQueue.get(animationQueue.size() - 1);
            if (o instanceof Transition) {
                current = (Form) ((Transition) o).getDestination();
                impl.setCurrentForm(current);
            }
        }

        if (current != null) {
            // make sure the fold menu occurs as expected then set the current
            // to the correct parent!
            if (current instanceof Dialog && current.isMenu()) {
                Transition t = current.getTransitionOutAnimator();
                if (t != null) {
                    // go back to the parent form first
                    if (current.getPreviousForm() != null) {
                        initTransition(t.copy(false), current, current.getPreviousForm());
                    }
                }
                current = current.getPreviousForm();
                impl.setCurrentForm(current);
            }

            // prevent the transition from occurring from a form into itself
            if (newForm != current) { //NOPMD CompareObjectsWithEquals
                if ((current != null && current.getTransitionOutAnimator() != null) || newForm.getTransitionInAnimator() != null) {
                    if (animationQueue == null) {
                        animationQueue = new ArrayList<Animation>();
                    }
                    // prevent form transitions from breaking our dialog based
                    // transitions which are a bit sensitive
                    if (current != null && (!(newForm instanceof Dialog))) {
                        Transition t = current.getTransitionOutAnimator();
                        if (t != null) {
                            transitionExists = initTransition(t.copy(reverse), current, newForm);
                        }
                    }
                    if (current != null && !(current instanceof Dialog)) {
                        Transition t = newForm.getTransitionInAnimator();
                        if (t != null) {
                            transitionExists = initTransition(t.copy(reverse), current, newForm);
                        }
                    }
                }
            }
        }
        synchronized (lock) {
            lock.notifyAll();
        }

        if (!transitionExists) {
            if (animationQueue == null || animationQueue.isEmpty()) {
                setCurrentForm(newForm);
            } else {
                // we need to add an empty transition to "serialize" this
                // screen change...
                Transition t = CommonTransitions.createEmpty();
                initTransition(t, current, newForm);
            }
        }
    }

    /// Initialize the transition and add it to the queue
    private boolean initTransition(Transition transition, Form source, Form dest) {
        try {
            dest.setVisible(true);
            transition.init(source, dest);
            if (source != null) {
                source.setLightweightMode(true);
            }
            dest.setLightweightMode(true);

            // if a native transition implementation exists then substitute it into place
            transition = impl.getNativeTransition(transition);
            animationQueue.add(transition);

            if (animationQueue.size() == 1) {
                transition.initTransition();
            }
        } catch (Throwable e) {
            Log.e(e);
            transition.cleanup();
            animationQueue.remove(transition);
            return false;
        }
        return true;
    }

    void setCurrentForm(Form newForm) {
        boolean forceShow = false;
        Form current = impl.getCurrentForm();
        if (current != null) {
            current.setVisible(false);
        } else {
            forceShow = true;
        }
        if (!initialWindowSizeApplied) {
            initialWindowSizeApplied = applyInitialWindowSize(newForm);
        }
        keyRepeatCharged = false;
        longPressCharged = false;
        longPointerCharged = false;
        current = newForm;
        impl.setCurrentForm(current);
        current.setVisible(true);
        if (forceShow || !allowMinimizing || inNativeUI) {
            impl.confirmControlView();
        }
        int w = current.getWidth();
        int h = current.getHeight();
        if (isEdt() && (w != impl.getDisplayWidth() || h != impl.getDisplayHeight())) {
            current.sizeChangedInternal(impl.getDisplayWidth(), impl.getDisplayHeight());
        } else {
            repaint(current);
        }
        lastKeyPressed = 0;
        previousKeyPressed = 0;
        newForm.onShowCompletedImpl();
    }

    private boolean applyInitialWindowSize(Form form) {
        if (form == null) {
            return false;
        }
        Object hint = form.getClientProperty(WINDOW_SIZE_HINT_PERCENT);
        if (!(hint instanceof Dimension)) {
            return false;
        }
        impl.setInitialWindowSizeHintPercent((Dimension) hint);
        return true;
    }

    /// Indicates whether a delay should exist between calls to flush graphics during
    /// transition. In some devices flushGraphics is asynchronious causing it to be
    /// very slow with our background thread. The solution is to add a short wait allowing
    /// the implementation time to paint the screen. This value is set automatically by default
    /// but can be overriden for some devices.
    ///
    /// #### Parameters
    ///
    /// - `transitionD`: -1 for no delay otherwise delay in milliseconds
    public void setTransitionYield(int transitionD) {
        transitionDelay = transitionD;
    }

    /// Fires the native in place text editing logic, normally you wouldn't invoke this API directly and instead
    /// use an API like `com.codename1.ui.TextArea#startEditingAsync()`, `com.codename1.ui.TextArea#startEditing()`
    /// or `com.codename1.ui.Form#setEditOnShow(com.codename1.ui.TextArea)`.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: the `TextArea` component
    ///
    /// - `maxSize`: the maximum size from the text area
    ///
    /// - `constraint`: the constraints of the text area
    ///
    /// - `text`: the string to edit
    public void editString(Component cmp, int maxSize, int constraint, String text) {
        editString(cmp, maxSize, constraint, text, 0);
    }

    /// Fires the native in place text editing logic, normally you wouldn't invoke this API directly and instead
    /// use an API like `com.codename1.ui.TextArea#startEditingAsync()`, `com.codename1.ui.TextArea#startEditing()`
    /// or `com.codename1.ui.Form#setEditOnShow(com.codename1.ui.TextArea)`.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: the `TextArea` component
    ///
    /// - `maxSize`: the maximum size from the text area
    ///
    /// - `constraint`: the constraints of the text area
    ///
    /// - `text`: the string to edit
    ///
    /// - `initiatingKeycode`: the keycode used to initiate the edit.
    public void editString(Component cmp, int maxSize, int constraint, String text, int initiatingKeycode) {
        if (isTextEditing(cmp)) {
            return;
        }
        cmp.requestFocus();
        if (cmp instanceof TextArea) {
            ((TextArea) cmp).setSuppressActionEvent(false);
        }
        Form f = cmp.getComponentForm();

        // this can happen in the spinner in the simulator where the key press should in theory start native
        // edit
        if (f == null) {
            return;
        }
        Component.setDisableSmoothScrolling(true);
        f.scrollComponentToVisible(cmp);
        Component.setDisableSmoothScrolling(false);
        keyRepeatCharged = false;
        longPressCharged = false;
        lastKeyPressed = 0;
        previousKeyPressed = 0;
        impl.editStringImpl(cmp, maxSize, constraint, text, initiatingKeycode);
    }

    /// Allows us to stop editString on the given text component
    ///
    /// #### Parameters
    ///
    /// - `cmp`: the text field/text area component
    public void stopEditing(Component cmp) {
        if (isTextEditing(cmp)) {
            impl.stopTextEditing();
        }
    }

    /// Allows us to stop editString on the given text component or Form.
    /// If cmp is a `Form`, it will stop editing in any active
    /// component on the form, and close the keyboard if it is opened.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: the text field/text area component
    ///
    /// - `onFinish`: invoked when editing stopped
    public void stopEditing(Component cmp, Runnable onFinish) {
        if (isTextEditing(cmp)) {
            impl.stopTextEditing(onFinish);
        } else {
            if (onFinish != null) {
                onFinish.run();
            }
        }
    }

    boolean isTextEditing(Component c) {
        if (c instanceof Form && c == getCurrent()) { //NOPMD CompareObjectsWithEquals
            return impl.isEditingText();
        }

        return impl.isEditingText(c);
    }

    boolean isNativeEditorVisible(Component c) {
        return impl.isNativeEditorVisible(c);
    }

    /// Minimizes the current application if minimization is supported by the platform (may fail).
    /// Returns false if minimization failed.
    ///
    /// #### Returns
    ///
    /// false if minimization failed true if it succeeded or seems to be successful
    public boolean minimizeApplication() {
        return getImplementation().minimizeApplication();
    }

    /// Indicates whether an application is minimized
    ///
    /// #### Returns
    ///
    /// true if the application is minimized
    public boolean isMinimized() {
        return getImplementation().isMinimized();
    }

    /// Restore the minimized application if minimization is supported by the platform
    public void restoreMinimizedApplication() {
        getImplementation().restoreMinimizedApplication();
    }

    private void addSingleArgumentEvent(int type, int code) {
        synchronized (lock) {
            if (this.dropEvents) {
                return;
            }
            inputEventStack[inputEventStackPointer] = type;
            inputEventStackPointer++;
            inputEventStack[inputEventStackPointer] = code;
            inputEventStackPointer++;
            lock.notifyAll();
        }
    }

    /// Checks if the control key is currently down.  Only relevant for desktop ports.
    public boolean isControlKeyDown() {
        return impl.isControlKeyDown();
    }

    /// Checks if the meta key is currently down.  Only relevant for desktop ports.
    public boolean isMetaKeyDown() {
        return impl.isMetaKeyDown();
    }

    /// Checks if the alt key is currently down.  Only relevant for desktop ports.
    public boolean isAltKeyDown() {
        return impl.isAltKeyDown();
    }

    /// Checks if the altgraph key is currently down.  Only relevant for desktop ports.
    public boolean isAltGraphKeyDown() {
        return impl.isAltGraphKeyDown();
    }

    /// Checks if the last mouse press was a right click.
    ///
    /// #### Returns
    ///
    /// True if the last mouse press was a right click.
    ///
    /// #### Since
    ///
    /// 7.0
    public boolean isRightMouseButtonDown() {
        return impl.isRightMouseButtonDown();
    }

    /// Checks if shift key is currently down.  Only relevant for desktop ports.
    public boolean isShiftKeyDown() {
        return impl.isShiftKeyDown();
    }

    /// Pushes a key press event with the given keycode into Codename One
    ///
    /// #### Parameters
    ///
    /// - `keyCode`: keycode of the key event
    public void keyPressed(final int keyCode) {
        if (impl.getCurrentForm() == null) {
            return;
        }
        addSingleArgumentEvent(KEY_PRESSED, keyCode);

        lastInteractionWasKeypad = lastInteractionWasKeypad || (keyCode != MenuBar.leftSK && keyCode != MenuBar.clearSK && keyCode != MenuBar.backSK);

        // this solves a Sony Ericsson bug where on slider open/close someone "brilliant" chose
        // to send a keyPress with a -43/-44 keycode... Without ever sending a key release!
        keyRepeatCharged = (keyCode >= 0 || getGameAction(keyCode) > 0) || keyCode == impl.getClearKeyCode();
        longPressCharged = keyRepeatCharged;
        longKeyPressTime = System.currentTimeMillis();
        keyRepeatValue = keyCode;
        int keyRepeatInitialIntervalTime = 800;
        nextKeyRepeatEvent = System.currentTimeMillis() + keyRepeatInitialIntervalTime;
        previousKeyPressed = lastKeyPressed;
        lastKeyPressed = keyCode;
    }

    /// Pushes a key release event with the given keycode into Codename One
    ///
    /// #### Parameters
    ///
    /// - `keyCode`: keycode of the key event
    public void keyReleased(final int keyCode) {
        keyRepeatCharged = false;
        longPressCharged = false;
        if (impl.getCurrentForm() == null) {
            return;
        }
        if (!multiKeyMode) {
            // this can happen when traversing from the native form to the current form
            // caused by a keypress
            // We need the previous key press for Codename One issue 108 which can occur when typing into
            // text field rapidly and pressing two buttons at once. Originally I had a patch
            // here specifically to the native edit but that patch doesn't work properly for
            // all native phone bugs (e.g. incoming phone call rejected and the key release is
            // sent to the java application).
            if (keyCode != lastKeyPressed) {
                if (keyCode != previousKeyPressed) {
                    return;
                } else {
                    previousKeyPressed = 0;
                }
            } else {
                lastKeyPressed = 0;
            }
        }
        addSingleArgumentEvent(KEY_RELEASED, keyCode);
    }

    void keyRepeatedInternal(final int keyCode) {
    }

    private void addPointerEvent(int type, int x, int y) {
        synchronized (lock) {
            if (this.dropEvents) {
                return;
            }
            inputEventStack[inputEventStackPointer] = type;
            inputEventStackPointer++;
            inputEventStack[inputEventStackPointer] = x;
            inputEventStackPointer++;
            inputEventStack[inputEventStackPointer] = y;
            inputEventStackPointer++;
            lock.notifyAll();
        }
    }

    private void addPointerEvent(int type, int[] x, int[] y) {
        synchronized (lock) {
            if (this.dropEvents) {
                return;
            }
            inputEventStack[inputEventStackPointer] = type;
            inputEventStackPointer++;
            inputEventStack[inputEventStackPointer] = x.length;
            inputEventStackPointer++;
            for (int value : x) {
                inputEventStack[inputEventStackPointer] = value;
                inputEventStackPointer++;
            }
            inputEventStack[inputEventStackPointer] = y.length;
            inputEventStackPointer++;
            for (int value : y) {
                inputEventStack[inputEventStackPointer] = value;
                inputEventStackPointer++;
            }
            lock.notifyAll();
        }
    }

    private void addPointerDragEventWithTimestamp(int x, int y) {
        synchronized (lock) {
            if (this.dropEvents) {
                return;
            }
            try {
                if (lastDragOffset > -1) {
                    inputEventStack[lastDragOffset] = x;
                    inputEventStack[lastDragOffset + 1] = y;
                    inputEventStack[lastDragOffset + 2] = (int) (System.currentTimeMillis() - displayInitTime);
                } else {
                    inputEventStack[inputEventStackPointer] = POINTER_DRAGGED;
                    inputEventStackPointer++;
                    lastDragOffset = inputEventStackPointer;
                    inputEventStack[inputEventStackPointer] = x;
                    inputEventStackPointer++;
                    inputEventStack[inputEventStackPointer] = y;
                    inputEventStackPointer++;
                    inputEventStack[inputEventStackPointer] = (int) (System.currentTimeMillis() - displayInitTime);
                    inputEventStackPointer++;
                }
            } catch (ArrayIndexOutOfBoundsException err) {
                Log.p("EDT performance is very slow triggering this exception!");
                Log.e(err);
            }
            lock.notifyAll();
        }
    }

    private void addPointerEventWithTimestamp(int type, int x, int y) {
        synchronized (lock) {
            if (this.dropEvents) {
                return;
            }
            try {
                inputEventStack[inputEventStackPointer] = type;
                inputEventStackPointer++;
                inputEventStack[inputEventStackPointer] = x;
                inputEventStackPointer++;
                inputEventStack[inputEventStackPointer] = y;
                inputEventStackPointer++;
                inputEventStack[inputEventStackPointer] = (int) (System.currentTimeMillis() - displayInitTime);
                inputEventStackPointer++;
            } catch (ArrayIndexOutOfBoundsException err) {
                Log.p("EDT performance is very slow triggering this exception!");
                Log.e(err);
            }
            lock.notifyAll();
        }
    }

    /// Pushes a pointer drag event with the given coordinates into Codename One
    ///
    /// #### Parameters
    ///
    /// - `x`: the x position of the pointer
    ///
    /// - `y`: the y position of the pointer
    public void pointerDragged(final int[] x, final int[] y) {
        if (impl.getCurrentForm() == null) {
            return;
        }
        longPointerCharged = false;
        if (x.length == 1) {
            addPointerDragEventWithTimestamp(x[0], y[0]);
        } else {
            addPointerEvent(POINTER_DRAGGED_MULTI, x, y);
        }
    }

    /// Pushes a pointer hover event with the given coordinates into Codename One
    ///
    /// #### Parameters
    ///
    /// - `x`: the x position of the pointer
    ///
    /// - `y`: the y position of the pointer
    public void pointerHover(final int[] x, final int[] y) {
        if (impl.getCurrentForm() == null) {
            return;
        }
        if (x.length == 1) {
            addPointerEventWithTimestamp(POINTER_HOVER, x[0], y[0]);
        } else {
            addPointerEvent(POINTER_HOVER, x, y);
        }
    }

    /// Pushes a pointer hover release event with the given coordinates into Codename One
    ///
    /// #### Parameters
    ///
    /// - `x`: the x position of the pointer
    ///
    /// - `y`: the y position of the pointer
    public void pointerHoverPressed(final int[] x, final int[] y) {
        if (impl.getCurrentForm() == null) {
            return;
        }
        addPointerEvent(POINTER_HOVER_PRESSED, x[0], y[0]);
    }

    /// Pushes a pointer hover release event with the given coordinates into Codename One
    ///
    /// #### Parameters
    ///
    /// - `x`: the x position of the pointer
    ///
    /// - `y`: the y position of the pointer
    public void pointerHoverReleased(final int[] x, final int[] y) {
        if (impl.getCurrentForm() == null) {
            return;
        }
        addPointerEvent(POINTER_HOVER_RELEASED, x[0], y[0]);
    }

    /// Pushes a pointer press event with the given coordinates into Codename One
    ///
    /// #### Parameters
    ///
    /// - `x`: the x position of the pointer
    ///
    /// - `y`: the y position of the pointer
    public void pointerPressed(final int[] x, final int[] y) {
        if (impl.getCurrentForm() == null) {
            return;
        }

        lastInteractionWasKeypad = false;
        longPointerCharged = true;
        longKeyPressTime = System.currentTimeMillis();
        pointerX = x[0];
        pointerY = y[0];
        if (x.length == 1) {
            addPointerEvent(POINTER_PRESSED, x[0], y[0]);
        } else {
            addPointerEvent(POINTER_PRESSED_MULTI, x, y);
        }
    }

    /// Pushes a pointer release event with the given coordinates into Codename One
    ///
    /// #### Parameters
    ///
    /// - `x`: the x position of the pointer
    ///
    /// - `y`: the y position of the pointer
    public void pointerReleased(final int[] x, final int[] y) {
        longPointerCharged = false;
        if (impl.getCurrentForm() == null) {
            return;
        }
        if (x.length == 1) {
            addPointerEvent(POINTER_RELEASED, x[0], y[0]);
        } else {
            addPointerEvent(POINTER_RELEASED_MULTI, x, y);
        }
    }

    private void addSizeChangeEvent(int type, int w, int h) {
        synchronized (lock) {
            inputEventStack[inputEventStackPointer] = type;
            inputEventStackPointer++;
            inputEventStack[inputEventStackPointer] = w;
            inputEventStackPointer++;
            inputEventStack[inputEventStackPointer] = h;
            inputEventStackPointer++;
            lock.notifyAll();
        }
    }

    /// Notifies Codename One of display size changes, this method is invoked by the implementation
    /// class and is for internal use
    ///
    /// #### Parameters
    ///
    /// - `w`: the width of the drawing surface
    ///
    /// - `h`: the height of the drawing surface
    public void sizeChanged(int w, int h) {
        Form current = impl.getCurrentForm();
        if (current == null) {
            return;
        }
        if (w == current.getWidth() && h == current.getHeight()) {
            // a workaround for a race condition on pixel 2 where size change events can happen really quickly
            if (lastSizeChangeEventWH == -1 || lastSizeChangeEventWH == w + h) {
                return;
            }
        }

        lastSizeChangeEventWH = w + h;
        addSizeChangeEvent(SIZE_CHANGED, w, h);
    }

    private void addNotifyEvent(int type) {
        synchronized (lock) {
            inputEventStack[inputEventStackPointer] = type;
            inputEventStackPointer++;
            lock.notifyAll();
        }
    }

    /// Broadcasts hide notify into Codename One, this method is invoked by the Codename One implementation
    /// to notify Codename One of hideNotify events
    public void hideNotify() {
        keyRepeatCharged = false;
        longPressCharged = false;
        longPointerCharged = false;
        pointerPressedAndNotReleasedOrDragged = false;
        addNotifyEvent(HIDE_NOTIFY);
    }

    /// Broadcasts show notify into Codename One, this method is invoked by the Codename One implementation
    /// to notify Codename One of showNotify events
    public void showNotify() {
        addNotifyEvent(SHOW_NOTIFY);
    }

    /// Used by the flush functionality which doesn't care much about component
    /// animations
    boolean shouldEDTSleepNoFormAnimation() {
        boolean b;
        synchronized (lock) {
            b = inputEventStackPointer == 0 &&
                    hasNoSerialCallsPending() &&
                    (!keyRepeatCharged || !longPressCharged);
        }
        return b;
    }

    private void updateDragSpeedStatus(int x, int y, int timestamp) {
        //save dragging input to calculate the dragging speed later
        dragPathX[dragPathOffset] = x;
        dragPathY[dragPathOffset] = y;
        dragPathTime[dragPathOffset] = displayInitTime + (long) timestamp;
        if (dragPathLength < PATHLENGTH) {
            dragPathLength++;
        }
        dragPathOffset++;
        if (dragPathOffset >= PATHLENGTH) {
            dragPathOffset = 0;
        }
    }

    boolean isRecursivePointerRelease() {
        return recursivePointerReleaseB;
    }

    private int[] readArrayStackArgument(int offset) {
        int[] a = new int[inputEventStackTmp[offset]];
        offset++;
        int alen = a.length;
        System.arraycopy(inputEventStackTmp, offset + 0, a, 0, alen);
        return a;
    }

    /// Invoked on the EDT to propagate the event
    private int handleEvent(int offset, int[] inputEventStackTmp) {
        Form f = getCurrentUpcomingForm(true);

        // might happen when returning from a deinitialized version of Codename One
        if (f == null) {
            return offset;
        }

        // no need to synchronize since we are reading only and modifying the stack frame offset
        int type = inputEventStackTmp[offset];
        offset++;

        switch (type) {
            case KEY_PRESSED:
                f.keyPressed(inputEventStackTmp[offset]);
                offset++;
                eventForm = f;
                break;
            case KEY_RELEASED:
                // pointer release can cycle into invoke and block which will cause this method
                // to recurse if a pointer will be released while we are in an invoke and block state
                // this is the case in http://code.google.com/p/codenameone/issues/detail?id=265
                Form xf = eventForm;
                eventForm = null;

                //make sure the released event is sent to the same Form who got a
                //pressed event
                if (xf == f || multiKeyMode) { //NOPMD CompareObjectsWithEquals
                    f.keyReleased(inputEventStackTmp[offset]);
                    offset++;
                }
                break;
            case POINTER_PRESSED:
                if (recursivePointerReleaseA) {
                    recursivePointerReleaseB = true;
                }
                dragOccured = false;
                dragPathLength = 0;
                pointerPressedAndNotReleasedOrDragged = true;
                xArray1[0] = inputEventStackTmp[offset];
                offset++;
                yArray1[0] = inputEventStackTmp[offset];
                offset++;
                f.pointerPressed(xArray1, yArray1);
                eventForm = f;
                break;
            case POINTER_PRESSED_MULTI: {
                if (recursivePointerReleaseA) {
                    recursivePointerReleaseB = true;
                }
                dragOccured = false;
                dragPathLength = 0;
                pointerPressedAndNotReleasedOrDragged = true;
                int[] array1 = readArrayStackArgument(offset);
                offset += array1.length + 1;
                int[] array2 = readArrayStackArgument(offset);
                offset += array2.length + 1;
                f.pointerPressed(array1, array2);
                eventForm = f;
                break;
            }
            case POINTER_RELEASED:
                recursivePointerReleaseA = true;
                pointerPressedAndNotReleasedOrDragged = false;

                // pointer release can cycle into invoke and block which will cause this method
                // to recurse if a pointer will be released while we are in an invoke and block state
                // this is the case in http://code.google.com/p/codenameone/issues/detail?id=265
                Form x = eventForm;
                eventForm = null;

                // make sure the released event is sent to the same Form that got a
                // pressed event
                if (x == f || f.shouldSendPointerReleaseToOtherForm()) { //NOPMD CompareObjectsWithEquals
                    xArray1[0] = inputEventStackTmp[offset];
                    offset++;
                    yArray1[0] = inputEventStackTmp[offset];
                    offset++;
                    f.pointerReleased(xArray1, yArray1);
                }
                recursivePointerReleaseA = false;
                recursivePointerReleaseB = false;
                break;
            case POINTER_RELEASED_MULTI:
                recursivePointerReleaseA = true;
                pointerPressedAndNotReleasedOrDragged = false;

                // pointer release can cycle into invoke and block which will cause this method
                // to recurse if a pointer will be released while we are in an invoke and block state
                // this is the case in http://code.google.com/p/codenameone/issues/detail?id=265
                Form xy = eventForm;
                eventForm = null;

                // make sure the released event is sent to the same Form that got a
                // pressed event
                if (xy == f || f.shouldSendPointerReleaseToOtherForm()) { //NOPMD CompareObjectsWithEquals
                    int[] array1 = readArrayStackArgument(offset);
                    offset += array1.length + 1;
                    int[] array2 = readArrayStackArgument(offset);
                    offset += array2.length + 1;
                    f.pointerReleased(array1, array1);
                }
                recursivePointerReleaseA = false;
                recursivePointerReleaseB = false;
                break;
            case POINTER_DRAGGED: {
                dragOccured = true;
                int arg1 = inputEventStackTmp[offset];
                offset++;
                int arg2 = inputEventStackTmp[offset];
                offset++;
                int timestamp = inputEventStackTmp[offset];
                offset++;
                updateDragSpeedStatus(arg1, arg2, timestamp);
                pointerPressedAndNotReleasedOrDragged = false;
                xArray1[0] = arg1;
                yArray1[0] = arg2;
                f.pointerDragged(xArray1, yArray1);
                break;
            }
            case POINTER_DRAGGED_MULTI: {
                dragOccured = true;
                pointerPressedAndNotReleasedOrDragged = false;
                int[] array1 = readArrayStackArgument(offset);
                offset += array1.length + 1;
                int[] array2 = readArrayStackArgument(offset);
                offset += array2.length + 1;
                f.pointerDragged(array1, array2);
                break;
            }
            case POINTER_HOVER: {
                int arg1 = inputEventStackTmp[offset];
                offset++;
                int arg2 = inputEventStackTmp[offset];
                offset++;
                int timestamp = inputEventStackTmp[offset];
                offset++;
                updateDragSpeedStatus(arg1, arg2, timestamp);
                xArray1[0] = arg1;
                yArray1[0] = arg2;
                f.pointerHover(xArray1, yArray1);
                break;
            }
            case POINTER_HOVER_RELEASED: {
                int arg1 = inputEventStackTmp[offset];
                offset++;
                int arg2 = inputEventStackTmp[offset];
                offset++;
                xArray1[0] = arg1;
                yArray1[0] = arg2;
                f.pointerHoverReleased(xArray1, yArray1);
                break;
            }
            case POINTER_HOVER_PRESSED: {
                int arg1 = inputEventStackTmp[offset];
                offset++;
                int arg2 = inputEventStackTmp[offset];
                offset++;
                xArray1[0] = arg1;
                yArray1[0] = arg2;
                f.pointerHoverPressed(xArray1, yArray1);
                break;
            }
            case SIZE_CHANGED:
                int w = inputEventStackTmp[offset];
                offset++;
                int h = inputEventStackTmp[offset];
                offset++;
                f.sizeChangedInternal(w, h);
                break;
            case HIDE_NOTIFY:
                f.hideNotify();
                break;
            case SHOW_NOTIFY:
                f.showNotify();
                break;
            default:
                break;
        }
        return offset;
    }

    /// This method should be invoked by components that broadcast events on the pointerReleased callback.
    /// This method will indicate if a drag occured since the pointer press event, notice that this method will not
    /// behave as expected for multi-touch events.
    ///
    /// #### Returns
    ///
    /// true if a drag has occured since the last pointer pressed
    public boolean hasDragOccured() {
        return dragOccured;
    }

    /// Returns true for a case where the EDT has nothing at all to do
    boolean shouldEDTSleep() {
        Form current = impl.getCurrentForm();
        return ((current == null || (!current.hasAnimations())) &&
                (animationQueue == null || animationQueue.isEmpty()) &&
                inputEventStackPointer == 0 &&
                (!impl.hasPendingPaints()) &&
                hasNoSerialCallsPending() && !keyRepeatCharged
                && !longPointerCharged) || (isMinimized() && hasNoSerialCallsPending());
    }

    Form getCurrentInternal() {
        return impl.getCurrentForm();
    }

    /// Same as getCurrent with the added exception of looking into the future
    /// transitions and returning the last current in the transition (the upcoming
    /// value for current)
    ///
    /// #### Returns
    ///
    /// @return the form currently displayed on the screen or null if no form is
    /// currently displayed
    Form getCurrentUpcoming() {
        return getCurrentUpcomingForm(false);
    }

    private Form getCurrentUpcomingForm(boolean includeMenus) {
        Form upcoming = null;

        // we are in the middle of a transition so we should extract the next form
        if (animationQueue != null) {
            int size = animationQueue.size();
            for (int iter = 0; iter < size; iter++) {
                Animation o = animationQueue.get(iter);
                if (o instanceof Transition) {
                    upcoming = (Form) ((Transition) o).getDestination();
                }
            }
        }
        if (upcoming == null) {
            if (includeMenus) {
                Form f = impl.getCurrentForm();
                if (f instanceof Dialog) {
                    if (f.isDisposed()) {
                        return getCurrent();
                    }
                }
                return f;
            } else {
                return getCurrent();
            }
        }
        return upcoming;
    }

    /// Return the form currently displayed on the screen or null if no form is
    /// currently displayed.
    ///
    /// #### Returns
    ///
    /// @return the form currently displayed on the screen or null if no form is
    /// currently displayed
    public Form getCurrent() {
        Form current = impl.getCurrentForm();
        if (current instanceof Dialog) {
            if (current.isMenu() || current.isDisposed()) {
                Form p = current.getPreviousForm();
                if (p != null) {
                    return p;
                }

                // we are in the middle of a transition so we should extract the next form
                if (animationQueue != null) {
                    int size = animationQueue.size();
                    for (int iter = 0; iter < size; iter++) {
                        Animation o = animationQueue.get(iter);
                        if (o instanceof Transition) {
                            return (Form) ((Transition) o).getDestination();
                        }
                    }
                }
            }
        }
        return current;
    }

    /// Return the number of alpha levels supported by the implementation.
    ///
    /// #### Returns
    ///
    /// the number of alpha levels supported by the implementation
    ///
    /// #### Deprecated
    ///
    /// this method isn't implemented in most modern devices
    public int numAlphaLevels() {
        return impl.numAlphaLevels();
    }

    /// Returns the number of colors applicable on the device, note that the API
    /// does not support gray scale devices.
    ///
    /// #### Returns
    ///
    /// the number of colors applicable on the device
    ///
    /// #### Deprecated
    ///
    /// this method isn't implemented in most modern devices
    public int numColors() {
        return impl.numColors();
    }

    /// Return the width of the display
    ///
    /// #### Returns
    ///
    /// the width of the display
    public int getDisplayWidth() {
        return impl.getDisplayWidth();
    }

    /// Return the height of the display
    ///
    /// #### Returns
    ///
    /// the height of the display
    public int getDisplayHeight() {
        return impl.getDisplayHeight();
    }

    /// Returns the size of the desktop hosting the application window when running on a desktop platform.
    ///
    /// #### Returns
    ///
    /// the desktop size or the current display size if not supported
    public Dimension getDesktopSize() {
        Dimension desktopSize = impl.getDesktopSize();
        if (desktopSize != null) {
            return desktopSize;
        }
        return new Dimension(getDisplayWidth(), getDisplayHeight());
    }

    /// Returns the current window bounds when running on a desktop platform.
    ///
    /// #### Returns
    ///
    /// the bounds of the application window
    public Rectangle getWindowBounds() {
        Rectangle bounds = impl.getWindowBounds();
        if (bounds == null) {
            return new Rectangle(0, 0, getDisplayWidth(), getDisplayHeight());
        }
        return bounds;
    }

    /// Requests a resize of the application window when supported by the platform.
    ///
    /// #### Parameters
    ///
    /// - `width`: the desired window width
    ///
    /// - `height`: the desired window height
    public void setWindowSize(int width, int height) {
        impl.setWindowSize(width, height);
    }

    /// Returns the initial desktop window size hint provided by the first shown form, when available.
    ///
    /// #### Returns
    ///
    /// the stored hint or `null`
    public Dimension getInitialWindowSizeHintPercent() {
        return impl.getInitialWindowSizeHintPercent();
    }

    /// Sets the initial desktop window size hint (percent of the desktop) that should be used when the
    /// first form is shown. This is primarily useful for desktop environments where the Codename One
    /// application is hosted in a window rather than full-screen.
    ///
    /// #### Parameters
    ///
    /// - `hint`: @param hint a `Dimension` whose width/height represent percentages of the desktop to use for
    ///             the initial window size, or `null` to clear a previously stored hint
    public void setInitialWindowSizeHintPercent(Dimension hint) {
        impl.setInitialWindowSizeHintPercent(hint);
    }

    /// Causes the given component to repaint, used internally by Form
    ///
    /// #### Parameters
    ///
    /// - `cmp`: the given component to repaint
    void repaint(final Animation cmp) {
        impl.repaint(cmp);
    }

    /// Converts the dips count to pixels, dips are roughly 1mm in length. This is a very rough estimate and not
    /// to be relied upon
    ///
    /// #### Parameters
    ///
    /// - `dipCount`: the dips that we will convert to pixels
    ///
    /// - `horizontal`: indicates pixels in the horizontal plane
    ///
    /// #### Returns
    ///
    /// value in pixels
    public int convertToPixels(int dipCount, boolean horizontal) {
        return impl.convertToPixels(dipCount, horizontal);
    }

    /// Converts from specified unit to pixels.
    ///
    /// #### Parameters
    ///
    /// - `value`: The value to convert, expressed in unitType.
    ///
    /// - `unitType`: @param unitType The unit type.  One of `Style#UNIT_TYPE_DIPS`, `Style#UNIT_TYPE_PIXELS`,
    ///                 `Style#UNIT_TYPE_REM`, `Style#UNIT_TYPE_SCREEN_PERCENTAGE`, `Style#UNIT_TYPE_VH`,
    ///                 `Style#UNIT_TYPE_VW`, `Style#UNIT_TYPE_VMIN`, `Style#UNIT_TYPE_VMAX`
    ///
    /// #### Returns
    ///
    /// The value converted to pixels.
    ///
    /// #### Since
    ///
    /// 8.0
    public int convertToPixels(float value, byte unitType) {
        return convertToPixels(value, unitType, true);
    }

    /// Converts from specified unit to pixels.
    ///
    /// #### Parameters
    ///
    /// - `value`: The value to convert, expressed in unitType.
    ///
    /// - `unitType`: @param unitType   The unit type.  One of `Style#UNIT_TYPE_DIPS`, `Style#UNIT_TYPE_PIXELS`,
    ///                   `Style#UNIT_TYPE_REM`, `Style#UNIT_TYPE_SCREEN_PERCENTAGE`, `Style#UNIT_TYPE_VH`,
    ///                   `Style#UNIT_TYPE_VW`, `Style#UNIT_TYPE_VMIN`, `Style#UNIT_TYPE_VMAX`
    ///
    /// - `horizontal`: Whether screen percentage units should be based on horitonzal or vertical percentage.
    ///
    /// #### Returns
    ///
    /// The value converted to pixels.
    ///
    /// #### Since
    ///
    /// 8.0
    public int convertToPixels(float value, byte unitType, boolean horizontal) {
        switch (unitType) {
            case Style.UNIT_TYPE_REM:
                return Math.round(value * Font.getDefaultFont().getHeight());
            case Style.UNIT_TYPE_VH:
                return Math.round(value / 100f * CN.getDisplayHeight());
            case Style.UNIT_TYPE_VW:
                return Math.round(value / 100f * CN.getDisplayWidth());
            case Style.UNIT_TYPE_VMIN:
                return Math.round(value / 100f * Math.min(CN.getDisplayWidth(), CN.getDisplayHeight()));
            case Style.UNIT_TYPE_VMAX:
                return Math.round(value / 100f * Math.max(CN.getDisplayWidth(), CN.getDisplayHeight()));
            case Style.UNIT_TYPE_DIPS:
                return Display.getInstance().convertToPixels(value);
            case Style.UNIT_TYPE_SCREEN_PERCENTAGE:
                if (!horizontal) {
                    float h = Display.getInstance().getDisplayHeight();
                    h = h / 100.0f * value;
                    return (int) h;
                } else {
                    float w = Display.getInstance().getDisplayWidth();
                    w = w / 100.0f * value;
                    return (int) w;
                }
            default:
                return (int) value;
        }

    }

    /// Converts the dips count to pixels, dips are roughly 1mm in length. This is a very rough estimate and not
    /// to be relied upon. This version of the method assumes square pixels which is pretty much the norm.
    ///
    /// #### Parameters
    ///
    /// - `dipCount`: the dips that we will convert to pixels
    ///
    /// #### Returns
    ///
    /// value in pixels
    public int convertToPixels(float dipCount) {
        return Math.round(impl.convertToPixels((int) (dipCount * 1000), true) / 1000.0f);
    }

    /// Checks to see if the platform supports a native image cache.
    ///
    /// #### Returns
    ///
    /// True on platforms that support a native image cache.  Currently only Javascript.
    boolean supportsNativeImageCache() {
        return impl.supportsNativeImageCache();
    }

    /// Returns the game action code matching the given key combination
    ///
    /// #### Parameters
    ///
    /// - `keyCode`: key code received from the event
    ///
    /// #### Returns
    ///
    /// game action matching this keycode
    public int getGameAction(int keyCode) {
        return impl.getGameAction(keyCode);
    }

    /// Returns the keycode matching the given game action constant (the opposite of getGameAction).
    /// On some devices getKeyCode returns numeric keypad values for game actions,
    /// this breaks the code since we filter these values (to prevent navigation on '2').
    /// We pick unused negative values for game keys and assign them to game keys for
    /// getKeyCode so they will work with getGameAction.
    ///
    /// #### Parameters
    ///
    /// - `gameAction`: game action constant from this class
    ///
    /// #### Returns
    ///
    /// keycode matching this constant
    ///
    /// #### Deprecated
    ///
    /// @deprecated this method doesn't work properly across device and is mocked up here
    /// mostly for the case of unit testing. Do not use it for anything other than that! Do
    /// not rely on getKeyCode(GAME_*) == keyCodeFromKeyEvent, this will never actually happen!
    public int getKeyCode(int gameAction) {
        return impl.getKeyCode(gameAction);
    }

    /// Indicates whether the 3rd softbutton should be supported on this device
    ///
    /// #### Returns
    ///
    /// true if a third softbutton should be used
    public boolean isThirdSoftButton() {
        return thirdSoftButton;
    }

    /// Indicates whether the 3rd softbutton should be supported on this device
    ///
    /// #### Parameters
    ///
    /// - `thirdSoftButton`: true if a third softbutton should be used
    public void setThirdSoftButton(boolean thirdSoftButton) {
        this.thirdSoftButton = thirdSoftButton;
    }

    /// Displays the virtual keyboard on devices that support manually poping up
    /// the vitual keyboard
    ///
    /// #### Parameters
    ///
    /// - `show`: toggles the virtual keyboards visibility
    ///
    /// #### Deprecated
    ///
    /// @deprecated this method was only relevant for feature phones.
    /// You should use `com.codename1.ui.TextArea#startEditingAsync()` or `com.codename1.ui.TextArea#stopEditing()`
    /// to control text field editing/VKB visibility
    public void setShowVirtualKeyboard(boolean show) {
        if (isTouchScreenDevice()) {
            VirtualKeyboardInterface vkb = getDefaultVirtualKeyboard();
            if (vkb != null) {
                vkb.showKeyboard(show);
            }
        }
    }

    /// Indicates if the virtual keyboard is currently showing or not
    ///
    /// #### Returns
    ///
    /// true if the virtual keyboard is showing
    ///
    /// #### Deprecated
    ///
    /// @deprecated this method was only relevant for feature phones.
    /// You should use `com.codename1.ui.TextArea#isEditing()` instead.
    public boolean isVirtualKeyboardShowing() {
        if (!isTouchScreenDevice()) {
            return false;
        }
        return getDefaultVirtualKeyboard() != null && getDefaultVirtualKeyboard().isVirtualKeyboardShowing();
    }

    /// Returns all platform supported virtual keyboards names
    ///
    /// #### Returns
    ///
    /// all platform supported virtual keyboards names
    ///
    /// #### Deprecated
    ///
    /// this method is only used in feature phones and has no modern equivalent
    public String[] getSupportedVirtualKeyboard() {
        String[] retVal = new String[virtualKeyboards.size()];
        int index = 0;
        for (String k : virtualKeyboards.keySet()) {
            retVal[index++] = k;
        }
        return retVal;
    }

    /// Register a virtual keyboard
    ///
    /// #### Parameters
    ///
    /// - `vkb`
    ///
    /// #### Deprecated
    ///
    /// this method is only used in feature phones and has no modern equivalent
    public void registerVirtualKeyboard(VirtualKeyboardInterface vkb) {
        virtualKeyboards.put(vkb.getVirtualKeyboardName(), vkb);
    }

    /// Get the default virtual keyboard or null if the VirtualKeyboard is disabled
    ///
    /// #### Returns
    ///
    /// the default vkb
    ///
    /// #### Deprecated
    ///
    /// this method is only used in feature phones and has no modern equivalent
    public VirtualKeyboardInterface getDefaultVirtualKeyboard() {
        if (selectedVirtualKeyboard == null) {
            return null;
        }
        return virtualKeyboards.get(selectedVirtualKeyboard);
    }

    /// Sets the default virtual keyboard to be used by the platform
    ///
    /// #### Parameters
    ///
    /// - `vkb`: @param vkb a VirtualKeyboard to be used or null to disable the
    ///            VirtualKeyboard
    ///
    /// #### Deprecated
    ///
    /// this method is only used in feature phones and has no modern equivalent
    public void setDefaultVirtualKeyboard(VirtualKeyboardInterface vkb) {
        if (vkb != null) {
            selectedVirtualKeyboard = vkb.getVirtualKeyboardName();
            if (!virtualKeyboards.containsKey(selectedVirtualKeyboard)) {
                registerVirtualKeyboard(vkb);
            }
        } else {
            selectedVirtualKeyboard = null;
        }
    }

    /// Gets the VirtualKeyboardListener Objects of exists.
    ///
    /// #### Returns
    ///
    /// a Listener Object or null if not exists
    ///
    /// #### Deprecated
    ///
    /// Use `#removeVirtualKeyboardListener(com.codename1.ui.events.ActionListener)`
    public ActionListener getVirtualKeyboardListener() {
        return virtualKeyboardListener;
    }

    /// Sets a listener for VirtualKeyboard hide/show events.
    /// The Listener will get an event once the keyboard is opened/closed with
    /// a Boolean value that represents the state of the keyboard true for open
    /// and false for closed getSource() on the ActionEvent will return the
    /// Boolean value.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    ///
    /// #### Deprecated
    ///
    /// Use `#addVirtualKeyboardListener(com.codename1.ui.events.ActionListener)`
    public void setVirtualKeyboardListener(ActionListener l) {
        if (virtualKeyboardListener != null) {
            removeVirtualKeyboardListener(l);
        }
        virtualKeyboardListener = l;
        addVirtualKeyboardListener(l);
    }

    /// Adds a listener for VirtualKeyboard hide/show events.  ActionEvents will return a Boolean
    /// value for `ActionEvent#getSource()`, with Boolean.TRUE on show, and Boolean.FALSE
    /// on hide.
    ///
    /// Note: Keyboard events may not be 100% reliable as they use heuristics on most platforms to guess when the keyboard
    /// is shown or hidden.
    ///
    /// #### Parameters
    ///
    /// - `l`: The listener.
    ///
    /// #### Since
    ///
    /// 6.0
    ///
    /// #### See also
    ///
    /// - #removeVirtualKeyboardListener(com.codename1.ui.events.ActionListener)
    public void addVirtualKeyboardListener(ActionListener l) {
        if (virtualKeyboardListeners == null) {
            virtualKeyboardListeners = new EventDispatcher();
        }
        virtualKeyboardListeners.addListener(l);
    }

    /// Removes a listener for VirtualKeyboard hide/show events.  ActionEvents will return a Boolean
    /// value for `ActionEvent#getSource()`, with Boolean.TRUE on show, and Boolean.FALSE
    /// on hide.
    ///
    /// Note: Keyboard events may not be 100% reliable as they use heuristics on most platforms to guess when the keyboard
    /// is shown or hidden.
    ///
    /// #### Parameters
    ///
    /// - `l`: The listener.
    ///
    /// #### Since
    ///
    /// 6.0
    ///
    /// #### See also
    ///
    /// - #addVirtualKeyboardListener(com.codename1.ui.events.ActionListener)
    public void removeVirtualKeyboardListener(ActionListener l) {
        if (virtualKeyboardListeners != null) {
            virtualKeyboardListeners.removeListener(l);
        }
    }

    /// Fires a virtual keyboard show event.
    ///
    /// #### Parameters
    ///
    /// - `show`
    ///
    /// #### Since
    ///
    /// 6.0
    public void fireVirtualKeyboardEvent(boolean show) {
        if (virtualKeyboardListeners != null) {
            virtualKeyboardListeners.fireActionEvent(new ActionEvent(show));
        }
    }

    /// Gets the invisible area under the Virtual Keyboard.
    ///
    /// #### Returns
    ///
    /// Height of the VKB that overlaps the screen.
    ///
    /// #### Since
    ///
    /// 6.0
    public int getInvisibleAreaUnderVKB() {
        return impl.getInvisibleAreaUnderVKB();
    }

    /// Returns the type of the input device one of:
    /// KEYBOARD_TYPE_UNKNOWN, KEYBOARD_TYPE_NUMERIC, KEYBOARD_TYPE_QWERTY,
    /// KEYBOARD_TYPE_VIRTUAL, KEYBOARD_TYPE_HALF_QWERTY
    ///
    /// #### Returns
    ///
    /// KEYBOARD_TYPE_UNKNOWN
    public int getKeyboardType() {
        return impl.getKeyboardType();
    }

    /// Indicates whether the device supports native in place editing in which case
    /// lightweight input logic shouldn't be used for input.
    ///
    /// #### Returns
    ///
    /// false by default
    public boolean isNativeInputSupported() {
        return impl.isNativeInputSupported();
    }

    /// Indicates whether the device supports multi-touch events, this is only
    /// relevant when touch events are supported
    ///
    /// #### Returns
    ///
    /// false by default
    public boolean isMultiTouch() {
        return impl.isMultiTouch();
    }

    /// Indicates whether the device has a double layer screen thus allowing two
    /// stages to touch events: click and hover. This is true for devices such
    /// as the storm but can also be true for a PC with a mouse pointer floating
    /// on top.
    ///
    /// A click touch screen will also send pointer hover events to the underlying
    /// software and will only send the standard pointer events on click.
    ///
    /// #### Returns
    ///
    /// false by default
    public boolean isClickTouchScreen() {
        return impl.isClickTouchScreen();
    }

    /// This method returns the dragging speed based on the latest dragged
    /// events
    ///
    /// #### Parameters
    ///
    /// - `yAxis`: indicates what axis speed is required
    ///
    /// #### Returns
    ///
    /// the dragging speed
    public float getDragSpeed(boolean yAxis) {
        float speed;
        if (yAxis) {
            speed = impl.getDragSpeed(dragPathY, dragPathTime, dragPathOffset, dragPathLength);
        } else {
            speed = impl.getDragSpeed(dragPathX, dragPathTime, dragPathOffset, dragPathLength);
        }
        return speed;
    }

    /// Indicates whether Codename One should consider the bidi RTL algorithm
    /// when drawing text or navigating with the text field cursor.
    ///
    /// #### Returns
    ///
    /// true if the bidi algorithm should be considered
    public boolean isBidiAlgorithm() {
        return impl.isBidiAlgorithm();
    }

    /// Indicates whether Codename One should consider the bidi RTL algorithm
    /// when drawing text or navigating with the text field cursor.
    ///
    /// #### Parameters
    ///
    /// - `activate`: @param activate set to true to activate the bidi algorithm, false to
    ///                 disable it
    public void setBidiAlgorithm(boolean activate) {
        impl.setBidiAlgorithm(activate);
    }

    /// Converts the given string from logical bidi layout to visual bidi layout so
    /// it can be rendered properly on the screen. This method is only necessary
    /// for devices/platforms that don't have "built in" bidi support such as
    /// Sony Ericsson devices.
    /// See [this](http://www.w3.org/International/articles/inline-bidi-markup/#visual)
    /// for more on visual vs. logical ordering.
    ///
    /// #### Parameters
    ///
    /// - `s`: a "logical" string with RTL characters
    ///
    /// #### Returns
    ///
    /// a "visual" renderable string
    public String convertBidiLogicalToVisual(String s) {
        return impl.convertBidiLogicalToVisual(s);
    }

    /// Returns the index of the given char within the source string, the actual
    /// index isn't necessarily the same when bidi is involved
    /// See [this](http://www.w3.org/International/articles/inline-bidi-markup/#visual)
    /// for more on visual vs. logical ordering.
    ///
    /// #### Parameters
    ///
    /// - `source`: the string in which we are looking for the position
    ///
    /// - `index`: the "logical" location of the cursor
    ///
    /// #### Returns
    ///
    /// the "visual" location of the cursor
    public int getCharLocation(String source, int index) {
        return impl.getCharLocation(source, index);
    }

    /// Returns true if the given character is an RTL character
    ///
    /// #### Parameters
    ///
    /// - `c`: character to test
    ///
    /// #### Returns
    ///
    /// true if the charcter is an RTL character
    public boolean isRTL(char c) {
        return impl.isRTL(c);
    }

    /// This method is essentially equivalent to cls.getResourceAsStream(String)
    /// however some platforms might define unique ways in which to load resources
    /// within the implementation.
    ///
    /// #### Parameters
    ///
    /// - `cls`: class to load the resource from
    ///
    /// - `resource`: relative/absolute URL based on the Java convention
    ///
    /// #### Returns
    ///
    /// input stream for the resource or null if not found
    public InputStream getResourceAsStream(Class cls, String resource) {
        return impl.getResourceAsStream(cls, resource);
    }

    /// An error handler will receive an action event with the source exception from the EDT
    /// once an error handler is installed the default Codename One error dialog will no longer appear
    ///
    /// #### Parameters
    ///
    /// - `e`: listener receiving the errors
    public void addEdtErrorHandler(ActionListener e) {
        if (errorHandler == null) {
            errorHandler = new EventDispatcher();
        }
        errorHandler.addListener(e);
    }

    /// An error handler will receive an action event with the source exception from the EDT
    /// once an error handler is installed the default Codename One error dialog will no longer appear
    ///
    /// #### Parameters
    ///
    /// - `e`: listener receiving the errors
    public void removeEdtErrorHandler(ActionListener e) {
        if (errorHandler != null) {
            errorHandler.removeListener(e);
            Collection v = errorHandler.getListenerCollection();
            if (v == null || v.isEmpty()) {
                errorHandler = null;
            }
        }
    }

    /// Allows a Codename One application to minimize without forcing it to the front whenever
    /// a new dialog is poped up
    ///
    /// #### Returns
    ///
    /// allowMinimizing value
    public boolean isAllowMinimizing() {
        return allowMinimizing;
    }

    /// Allows a Codename One application to minimize without forcing it to the front whenever
    /// a new dialog is poped up
    ///
    /// #### Parameters
    ///
    /// - `allowMinimizing`: value
    public void setAllowMinimizing(boolean allowMinimizing) {
        this.allowMinimizing = allowMinimizing;
    }

    /// This is an internal state flag relevant only for pureTouch mode (otherwise it
    /// will always be true). A pureTouch mode is stopped if a user switches to using
    /// the trackball/navigation pad and this flag essentially toggles between those two modes.
    ///
    /// #### Returns
    ///
    /// the shouldRenderSelection
    public boolean shouldRenderSelection() {
        return !pureTouch || pointerPressedAndNotReleasedOrDragged || lastInteractionWasKeypad;
    }

    /// This is an internal state flag relevant only for pureTouch mode (otherwise it
    /// will always be true). A pureTouch mode is stopped if a user switches to using
    /// the trackball/navigation pad and this flag essentially toggles between those two modes.
    ///
    /// #### Parameters
    ///
    /// - `c`: the component to test against, this prevents a touch outside of the component that triggers a repaint from painting the component selection
    ///
    /// #### Returns
    ///
    /// the shouldRenderSelection
    public boolean shouldRenderSelection(Component c) {
        if (c.isCellRenderer()) {
            return shouldRenderSelection();
        }
        return !pureTouch || lastInteractionWasKeypad || (pointerPressedAndNotReleasedOrDragged && c.contains(pointerX, pointerY)) || c.shouldRenderComponentSelection();
    }

    /// A pure touch device has no focus showing when the user is using the touch
    /// interface. Selection only shows when the user actually touches the screen
    /// or suddenly switches to using a keypad/trackball. This sort of interface
    /// is common in Android devices
    ///
    /// #### Returns
    ///
    /// the pureTouch flag
    public boolean isPureTouch() {
        return pureTouch;
    }

    /// A pure touch device has no focus showing when the user is using the touch
    /// interface. Selection only shows when the user actually touches the screen
    /// or suddenly switches to using a keypad/trackball. This sort of interface
    /// is common in Android devices
    ///
    /// #### Parameters
    ///
    /// - `pureTouch`: the value for pureTouch
    public void setPureTouch(boolean pureTouch) {
        this.pureTouch = pureTouch;
    }

    /// Indicates whether Codename One commands should be mapped to the native menus
    ///
    /// #### Returns
    ///
    /// the nativeCommands status
    ///
    /// #### Deprecated
    ///
    /// use getCommandBehavior() == Display.COMMAND_BEHAVIOR_NATIVE
    public boolean isNativeCommands() {
        return getCommandBehavior() == COMMAND_BEHAVIOR_NATIVE;
    }

    /// Indicates whether Codename One commands should be mapped to the native menus
    ///
    /// #### Parameters
    ///
    /// - `nativeCommands`: the flag to set
    ///
    /// #### Deprecated
    ///
    /// use setCommandBehavior(Display.COMMAND_BEHAVIOR_NATIVE)
    public void setNativeCommands(boolean nativeCommands) {
        setCommandBehavior(COMMAND_BEHAVIOR_NATIVE);
    }

    /// Exits the application...
    public void exitApplication() {
        codenameOneExited = true;
        impl.exit();
    }

    /// Checks if this platform supports full-screen mode.  If full-screen mode is supported, you can use
    /// the `#requestFullScreen()`, `#exitFullScreen()`, and `#isInFullScreenMode()` methods
    /// to enter and exit full-screen - and query the current state.
    ///
    /// Currently only desktop and Javascript builds support full-screen mode; And Javascript
    /// only supports this on certain browsers.  See the [MDN Fullscreen API docs](https://developer.mozilla.org/en-US/docs/Web/API/Fullscreen_API)
    /// for a list of browsers that support full-screen.
    ///
    /// When running in the simulator, full-screen is only supported for the desktop skin.
    ///
    /// #### Returns
    ///
    /// true if Full-screen mode is supported on this platform.
    ///
    /// #### Since
    ///
    /// 6.0
    ///
    /// #### See also
    ///
    /// - #requestFullScreen()
    ///
    /// - #exitFullScreen()
    ///
    /// - #isInFullScreenMode()
    public boolean isFullScreenSupported() {
        return impl.isFullScreenSupported();
    }

    /// Try to enter full-screen mode if the platform supports it.
    ///
    /// Currently only desktop and Javascript builds support full-screen mode; And Javascript
    /// only supports this on certain browsers.  See the [MDN Fullscreen API docs](https://developer.mozilla.org/en-US/docs/Web/API/Fullscreen_API)
    /// for a list of browsers that support full-screen.
    ///
    /// When running in the simulator, full-screen is only supported for the desktop skin.
    ///
    /// #### Returns
    ///
    /// @return true on success.  This will also return true if the app is already running in full-screen mode.  It will return false
    /// if the app fails to enter full-screen mode.
    ///
    /// #### Since
    ///
    /// 6.0
    ///
    /// #### See also
    ///
    /// - #exitFullScreen()
    ///
    /// - #isInFullScreenMode()
    ///
    /// - #isFullScreenSupported()
    public boolean requestFullScreen() {
        return impl.requestFullScreen();
    }

    /// Try to exit full-screen mode if the platform supports it.
    ///
    /// Currently only desktop and Javascript builds support full-screen mode; And Javascript
    /// only supports this on certain browsers.  See the [MDN Fullscreen API docs](https://developer.mozilla.org/en-US/docs/Web/API/Fullscreen_API)
    /// for a list of browsers that support full-screen.
    ///
    /// When running in the simulator, full-screen is only supported for the desktop skin.
    ///
    /// #### Returns
    ///
    /// @return true on success.  This will also return true if the app is already NOT in full-screen mode.  It will return false
    /// if the app fails to exit full-screen mode.
    ///
    /// #### Since
    ///
    /// 6.0
    ///
    /// #### See also
    ///
    /// - #requestFullScreen()
    ///
    /// - #isInFullScreenMode()
    ///
    /// - #isFullScreenSupported()
    public boolean exitFullScreen() {
        return impl.exitFullScreen();
    }

    /// Checks if the app is currently running in full-screen mode.
    ///
    /// #### Returns
    ///
    /// true if the app is currently in full-screen mode.
    ///
    /// #### Since
    ///
    /// 6.0
    ///
    /// #### See also
    ///
    /// - #requestFullScreen()
    ///
    /// - #exitFullScreen()
    ///
    /// - #isFullScreenSupported()
    public boolean isInFullScreenMode() {
        return impl.isInFullScreenMode();
    }

    /// Shows a native Form/Canvas or some other heavyweight native screen
    ///
    /// #### Parameters
    ///
    /// - `nativeFullScreenPeer`: the native screen peer
    public void showNativeScreen(Object nativeFullScreenPeer) {
        inNativeUI = true;
        impl.showNativeScreen(nativeFullScreenPeer);
    }

    /// Normally Codename One folds the VKB when switching forms this field allows us
    /// to block that behavior.
    ///
    /// #### Returns
    ///
    /// the autoFoldVKBOnFormSwitch
    public boolean isAutoFoldVKBOnFormSwitch() {
        return autoFoldVKBOnFormSwitch;
    }

    /// Normally Codename One folds the VKB when switching forms this field allows us
    /// to block that behavior.
    ///
    /// #### Parameters
    ///
    /// - `autoFoldVKBOnFormSwitch`: the autoFoldVKBOnFormSwitch to set
    public void setAutoFoldVKBOnFormSwitch(boolean autoFoldVKBOnFormSwitch) {
        this.autoFoldVKBOnFormSwitch = autoFoldVKBOnFormSwitch;
    }

    /// Indicates the way commands should be added to a form as one of the ocmmand constants defined
    /// in this class
    ///
    /// #### Returns
    ///
    /// the commandBehavior
    ///
    /// #### Deprecated
    ///
    /// @deprecated we recommend migrating to the `Toolbar` API. When using the toolbar the command
    /// behavior can't be manipulated
    public int getCommandBehavior() {
        return impl.getCommandBehavior();
    }

    /// Indicates the way commands should be added to a form as one of the ocmmand constants defined
    /// in this class
    ///
    /// #### Parameters
    ///
    /// - `commandBehavior`: the commandBehavior to set
    ///
    /// #### Deprecated
    ///
    /// @deprecated we recommend migrating to the `Toolbar` API. When using the toolbar the command
    /// behavior can't be manipulated
    public void setCommandBehavior(int commandBehavior) {
        if (commandBehavior == Display.COMMAND_BEHAVIOR_SIDE_NAVIGATION) {
            String message = "WARNING: Display.setCommandBehavior() is deprecated, Using it may result in unexpected behaviour. In particular, using COMMAND_BEHAVIOR_SIDE_NAVIGATION in conjunction with Toolbar.setOnTopSideMenu(true) may result in runtime errors.";
            Log.p(message, Log.WARNING);
        }
        impl.setCommandBehavior(commandBehavior);
    }

    /// Posts a message to the native platform.  Different platforms may handle messages posted this
    /// way differently.
    ///
    /// The Javascript port will dispatch the message on the window object
    /// as a custom DOM event named 'cn1outbox', with the event data containing a 'detail' key with the
    /// message, and a 'code' key with the code.
    ///
    /// #### Parameters
    ///
    /// - `message`: The message.
    ///
    /// #### Since
    ///
    /// 7.0
    public void postMessage(MessageEvent message) {
        impl.postMessage(message);
    }

    /// Adds a listener to receive messages from the native platform.  This is one mechanism for the native
    /// platform to communicate with the Codename one app.
    ///
    /// In the JavaScript port, listeners will be notified when DOM events named 'cn1inbox' are received on the
    /// window object.  The event data 'detail' key will be the source of the message, and the 'code' key will be the
    /// source of the code.
    ///
    /// #### Parameters
    ///
    /// - `l`: The listener.
    ///
    /// #### Since
    ///
    /// 7.0
    public void addMessageListener(ActionListener<MessageEvent> l) {
        if (messageListeners == null) {
            messageListeners = new EventDispatcher();
        }
        messageListeners.addListener(l);
    }

    /// Removes a listener from receiving messages from the native platform.
    ///
    /// #### Parameters
    ///
    /// - `l`: The listener.
    ///
    /// #### Since
    ///
    /// 7.0
    public void removeMessageListener(ActionListener<MessageEvent> l) {
        if (messageListeners != null) {
            messageListeners.removeListener(l);
        }
    }

    /// Dispatches a message to all of the registered listeners.
    ///
    /// #### Parameters
    ///
    /// - `evt`
    ///
    /// #### Since
    ///
    /// 7.0
    ///
    /// #### See also
    ///
    /// - #addMessageListener(com.codename1.ui.events.ActionListener)
    ///
    /// - #removeMessageListener(com.codename1.ui.events.ActionListener)
    public void dispatchMessage(MessageEvent evt) {
        if (messageListeners != null && messageListeners.hasListeners()) {
            messageListeners.fireActionEvent(evt);
        }
    }

    /// Adds a listener to receive notifications about native window changes such as resize or movement.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to add
    public void addWindowListener(ActionListener<WindowEvent> l) {
        if (windowListeners == null) {
            windowListeners = new EventDispatcher();
        }
        windowListeners.addListener(l);
    }

    /// Removes a previously registered window listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to remove
    public void removeWindowListener(ActionListener<WindowEvent> l) {
        if (windowListeners != null) {
            windowListeners.removeListener(l);
        }
    }

    /// Dispatches a window change event to registered listeners. This method is intended to be invoked by
    /// platform implementations.
    ///
    /// #### Parameters
    ///
    /// - `evt`: the window event to dispatch
    public void fireWindowEvent(WindowEvent evt) {
        if (evt == null || windowListeners == null || !windowListeners.hasListeners()) {
            return;
        }
        if (isEdt()) {
            windowListeners.fireActionEvent(evt);
        } else {
            final WindowEvent windowEvent = evt;
            callSerially(new Runnable() {
                @Override
                public void run() {
                    if (windowListeners != null && windowListeners.hasListeners()) {
                        windowListeners.fireActionEvent(windowEvent);
                    }
                }
            });
        }
    }

    /// Returns the property from the underlying platform deployment or the default
    /// value if no deployment values are supported. This is equivalent to the
    /// getAppProperty from the jad file.
    ///
    /// The implementation should be responsible for the following keys to return
    /// reasonable valid values for the application:
    ///
    /// - AppName
    ///
    /// - User-Agent
    ///
    /// - AppVersion
    ///
    /// - Platform - Similar to microedition.platform
    ///
    /// - OS - returns what is the underlying platform e.g. - iOS, Android, RIM, SE...
    ///
    /// - OSVer - OS version when available as a user readable string (not necessarily a number e.g: 3.2.1).
    ///
    /// #### Parameters
    ///
    /// - `key`: the key of the property
    ///
    /// - `defaultValue`: a default return value
    ///
    /// #### Returns
    ///
    /// the value of the property
    public String getProperty(String key, String defaultValue) {
        if ("AppArg".equals(key)) {
            String out = impl.getAppArg();
            return out == null ? defaultValue : out;
        }
        if ("Component.revalidateOnStyleChange".equals(key)) {
            return Component.isRevalidateOnStyleChange() ? "true" : "false";
        }
        if (localProperties != null) {
            String v = localProperties.get(key);
            if (v != null) {
                return v;
            }
        }
        return impl.getProperty(key, defaultValue);
    }

    /// Sets a local property to the application, this method has no effect on the
    /// implementation code and only allows the user to override the logic of getProperty
    /// for internal application purposes.
    ///
    /// #### Parameters
    ///
    /// - `key`: key the key of the property
    ///
    /// - `value`: the value of the property
    public void setProperty(String key, String value) {
        if ("AppArg".equals(key)) {
            impl.setAppArg(value);
            return;
        }
        if ("blockOverdraw".equals(key)) {
            Container.setBlockOverdraw(true);
            return;
        }
        if ("blockCopyPaste".equals(key)) {
            impl.blockCopyPaste("true".equals(value));
        }
        if ("Component.revalidateOnStyleChange".equals(key)) {
            Component.setRevalidateOnStyleChange("true".equalsIgnoreCase(value));
        }
        if (key.startsWith("platformHint.")) {
            impl.setPlatformHint(key, value);
            return;
        }
        if (localProperties == null) {
            localProperties = new HashMap<String, String>();
        }
        if (value == null) {
            localProperties.remove(key);
        } else {
            localProperties.put(key, value);
        }
    }

    /// Returns true if executing this URL should work, returns false if it will not
    /// and null if this is unknown.
    ///
    /// ```java
    /// Boolean can = Display.getInstance().canExecute("imdb:///find?q=godfather");
    /// if(can != null && can) {
    ///   Display.getInstance().execute("imdb:///find?q=godfather");
    /// } else {
    ///   Display.getInstance().execute("http://www.imdb.com");
    /// }
    /// ```
    ///
    /// #### Parameters
    ///
    /// - `url`: the url that would be executed
    ///
    /// #### Returns
    ///
    /// @return true if executing this URL should work, returns false if it will not
    /// and null if this is unknown
    public Boolean canExecute(String url) {
        return impl.canExecute(url);
    }

    /// Executes the given URL on the native platform
    ///
    /// ```java
    /// Boolean can = Display.getInstance().canExecute("imdb:///find?q=godfather");
    /// if(can != null && can) {
    ///   Display.getInstance().execute("imdb:///find?q=godfather");
    /// } else {
    ///   Display.getInstance().execute("http://www.imdb.com");
    /// }
    /// ```
    ///
    /// #### Parameters
    ///
    /// - `url`: the url to execute
    public void execute(String url) {
        impl.execute(url);
    }

    /// Executes the given URL on the native platform, this method is useful if
    /// the platform has the ability to send an event to the app when the execution
    /// has ended, currently this works only for Android platform to invoke other
    /// intents.
    ///
    /// #### Parameters
    ///
    /// - `url`: the url to execute
    ///
    /// - `response`: @param response a callback from the platform when this execution returned
    ///                 to the application
    public void execute(String url, ActionListener response) {
        impl.execute(url, response);
    }

    /// Returns one of the density variables appropriate for this device, notice that
    /// density doesn't always correspond to resolution and an implementation might
    /// decide to change the density based on DPI constraints.
    ///
    /// #### Returns
    ///
    /// one of the DENSITY constants of Display
    public int getDeviceDensity() {
        return impl.getDeviceDensity();
    }

    /// Returns the device density as a string.
    ///
    /// - DENSITY_VERY_LOW : "very-low"
    ///
    /// - DENSITY_LOW : "low"
    ///
    /// - DENSITY_MEDIUM : "medium"
    ///
    /// - DENSITY_HIGH : "high"
    ///
    /// - DENSITY_VERY_HIGH : "very-high"
    ///
    /// - DENSITY_HD : "hd"
    ///
    /// - DENSITY_560 : "560"
    ///
    /// - DENSITY_2HD : "2hd"
    ///
    /// - DENSITY_4K : "4k";
    ///
    /// #### Returns
    ///
    /// Device density as a string.
    ///
    /// #### Since
    ///
    /// 7.0
    ///
    /// #### See also
    ///
    /// - #getDeviceDensity()
    public String getDensityStr() {
        switch (getDeviceDensity()) {
            case DENSITY_VERY_LOW:
                return "very-low";
            case DENSITY_LOW:
                return "low";
            case DENSITY_MEDIUM:
                return "medium";
            case DENSITY_HIGH:
                return "high";
            case DENSITY_VERY_HIGH:
                return "very-high";
            case DENSITY_HD:
                return "hd";
            case DENSITY_560:
                return "560";
            case DENSITY_2HD:
                return "2hd";
            case DENSITY_4K:
                return "4k";
            default:
                throw new IllegalStateException("Unknown density " + getDeviceDensity());
        }
    }

    /// Plays a builtin device sound matching the given identifier, implementations
    /// and themes can offer additional identifiers to the ones that are already built
    /// in.
    ///
    /// #### Parameters
    ///
    /// - `soundIdentifier`: @param soundIdentifier the sound identifier which can match one of the
    ///                        common constants in this class or be a user/implementation defined sound
    ///
    /// #### Deprecated
    ///
    /// this isn't supported on most platforms
    public void playBuiltinSound(String soundIdentifier) {
        impl.playBuiltinSound(soundIdentifier);
    }

    /// Gets the display safe area as a rectangle.
    ///
    /// #### Parameters
    ///
    /// - `rect`: Out parameter that will store the display safe area.
    ///
    /// #### Returns
    ///
    /// The display safe area.
    ///
    /// #### Since
    ///
    /// 7.0
    ///
    /// #### See also
    ///
    /// - Form#getSafeArea()
    public Rectangle getDisplaySafeArea(Rectangle rect) {
        return impl.getDisplaySafeArea(rect);
    }

    /// Installs a replacement sound as the builtin sound responsible for the given
    /// sound identifier (this will override the system sound if such a sound exists).
    ///
    /// #### Parameters
    ///
    /// - `soundIdentifier`: the sound string passed to playBuiltinSound
    ///
    /// - `data`: @param data            an input stream containing platform specific audio file, its usually safe
    ///                        to assume that wav/mp3 would be supported.
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the stream throws an exception
    public void installBuiltinSound(String soundIdentifier, InputStream data) throws IOException {
        impl.installBuiltinSound(soundIdentifier, data);
    }

    /// Indicates whether a user installed or system sound is available
    ///
    /// #### Parameters
    ///
    /// - `soundIdentifier`: the sound string passed to playBuiltinSound
    ///
    /// #### Returns
    ///
    /// true if a sound of this given type is avilable
    ///
    /// #### Deprecated
    ///
    /// this isn't supported on most platforms
    public boolean isBuiltinSoundAvailable(String soundIdentifier) {
        return impl.isBuiltinSoundAvailable(soundIdentifier);
    }

    /// Allows muting/unmuting the builtin sounds easily
    ///
    /// #### Returns
    ///
    /// true if the sound is *not* muted
    public boolean isBuiltinSoundsEnabled() {
        return impl.isBuiltinSoundsEnabled();
    }

    /// Allows muting/unmuting the builtin sounds easily
    ///
    /// #### Parameters
    ///
    /// - `enabled`: indicates whether the sound is muted
    public void setBuiltinSoundsEnabled(boolean enabled) {
        impl.setBuiltinSoundsEnabled(enabled);
    }

    /// Creates a sound in the given URI which is partially platform specific.
    /// Notice that an audio is "auto destroyed" on completion and cannot be played
    /// twice!
    ///
    /// #### Parameters
    ///
    /// - `uri`: the platform specific location for the sound
    ///
    /// - `onCompletion`: invoked when the audio file finishes playing, may be null
    ///
    /// #### Returns
    ///
    /// a handle that can be used to control the playback of the audio
    ///
    /// #### Throws
    ///
    /// - `java.io.IOException`: if the URI access fails
    public Media createMedia(String uri, boolean isVideo, Runnable onCompletion) throws IOException {
        return impl.createMedia(uri, isVideo, onCompletion);
    }

    /// Creates media asynchronously.
    ///
    /// #### Parameters
    ///
    /// - `uri`: the platform specific location for the sound
    ///
    /// - `onCompletion`: invoked when the audio file finishes playing, may be null
    ///
    /// #### Returns
    ///
    /// a handle that can be used to control the playback of the audio
    ///
    /// #### Since
    ///
    /// 7.0
    public AsyncResource<Media> createMediaAsync(String uri, boolean video, Runnable onCompletion) {
        return impl.createMediaAsync(uri, video, onCompletion);
    }

    /// Adds a callback to a Media element that will be called when the media finishes playing.
    ///
    /// #### Parameters
    ///
    /// - `media`: The media to add the callback to.
    ///
    /// - `onCompletion`: The callback that will run on the EDT when the playback completes.
    ///
    /// #### See also
    ///
    /// - #removeCompletionHandler(com.codename1.media.Media, java.lang.Runnable)
    public void addCompletionHandler(Media media, Runnable onCompletion) {
        impl.addCompletionHandler(media, onCompletion);
    }

    /// Removes onComplete callback from Media element.
    ///
    /// #### Parameters
    ///
    /// - `media`: The media element.
    ///
    /// - `onCompletion`: The callback.
    ///
    /// #### See also
    ///
    /// - #addCompletionHandler(com.codename1.media.Media, java.lang.Runnable)
    public void removeCompletionHandler(Media media, Runnable onCompletion) {
        impl.removeCompletionHandler(media, onCompletion);
    }

    /// Create the sound in the given stream
    /// Notice that an audio is "auto destroyed" on completion and cannot be played
    /// twice!
    ///
    /// #### Parameters
    ///
    /// - `stream`: the stream containing the media data
    ///
    /// - `mimeType`: the type of the data in the stream
    ///
    /// - `onCompletion`: invoked when the audio file finishes playing, may be null
    ///
    /// #### Returns
    ///
    /// a handle that can be used to control the playback of the audio
    ///
    /// #### Throws
    ///
    /// - `java.io.IOException`: if the URI access fails
    public Media createMedia(InputStream stream, String mimeType, Runnable onCompletion) throws IOException {
        return impl.createMedia(stream, mimeType, onCompletion);
    }

    public AsyncResource<Media> createMediaAsync(InputStream stream, String mimeType, Runnable onCompletion) {
        return impl.createMediaAsync(stream, mimeType, onCompletion);

    }

    /// Creates a soft/weak reference to an object that allows it to be collected
    /// yet caches it. This method is in the porting layer since CLDC only includes
    /// weak references while some platforms include nothing at all and some include
    /// the superior soft references.
    ///
    /// #### Parameters
    ///
    /// - `o`: object to cache
    ///
    /// #### Returns
    ///
    /// a caching object or null  if caching isn't supported
    public Object createSoftWeakRef(Object o) {
        return impl.createSoftWeakRef(o);
    }

    /// Extracts the hard reference from the soft/weak reference given
    ///
    /// #### Parameters
    ///
    /// - `o`: the reference returned by createSoftWeakRef
    ///
    /// #### Returns
    ///
    /// the original object submitted or null
    public Object extractHardRef(Object o) {
        return impl.extractHardRef(o);
    }

    /// Indicates if the implemenetation has a native underlying theme
    ///
    /// #### Returns
    ///
    /// true if the implementation has a native theme available
    public boolean hasNativeTheme() {
        return impl.hasNativeTheme();
    }

    /// Installs the native theme, this is only applicable if hasNativeTheme() returned true. Notice that this method
    /// might replace the DefaultLookAndFeel instance and the default transitions.
    public void installNativeTheme() {
        impl.installNativeTheme();
    }

    /// Performs a clipboard copy operation, if the native clipboard is supported by the implementation it would be used
    ///
    /// #### Parameters
    ///
    /// - `obj`: @param obj object to copy, while this can be any arbitrary object it is recommended that only Strings or Codename One
    ///            image objects be used to copy
    public void copyToClipboard(Object obj) {
        impl.copyToClipboard(obj);
    }

    /// Returns the current content of the clipboard
    ///
    /// #### Returns
    ///
    /// can be any object or null see copyToClipboard
    public Object getPasteDataFromClipboard() {
        return impl.getPasteDataFromClipboard();
    }

    /// Returns true if the device is currently in portrait mode
    ///
    /// #### Returns
    ///
    /// true if the device is in portrait mode
    public boolean isPortrait() {
        return impl.isPortrait();
    }

    /// Returns true if the device allows forcing the orientation via code, feature phones do not allow this
    /// although some include a jad property allowing for this feature
    ///
    /// Since version 6.0, orientation lock is supported in Javascript builds in some browsers.  For a full
    /// list of browsers the support locking orientation, see the [MDN Lock Orientation docs](https://developer.mozilla.org/en-US/docs/Web/API/Screen/lockOrientation).
    ///
    /// **NOTE:** In Javascript builds, orientation lock is only supported if the app is running in full-screen mode.  If the app is not
    /// currently in full-screen mode, then `#canForceOrientation()` will return false and `#lockOrientation(boolean)` will do nothing.
    ///
    /// #### Returns
    ///
    /// true if lockOrientation  would work
    ///
    /// #### See also
    ///
    /// - #lockOrientation(boolean)
    ///
    /// - #unlockOrientation()
    public boolean canForceOrientation() {
        return impl.canForceOrientation();
    }

    /// On devices that return true for canForceOrientation() this method can lock the device orientation
    /// either to portrait or landscape mode
    ///
    /// Since version 6.0, orientation lock is supported in Javascript builds in some browsers.  For a full
    /// list of browsers the support locking orientation, see the [MDN Lock Orientation docs](https://developer.mozilla.org/en-US/docs/Web/API/Screen/lockOrientation).
    ///
    /// **NOTE:** In Javascript builds, orientation lock is only supported if the app is running in full-screen mode.  If the app is not
    /// currently in full-screen mode, then `#canForceOrientation()` will return false and `#lockOrientation(boolean)` will do nothing.
    ///
    /// #### Parameters
    ///
    /// - `portrait`: true to lock to portrait mode, false to lock to landscape mode
    ///
    /// #### See also
    ///
    /// - #unlockOrientation()
    ///
    /// - #canForceOrientation()
    public void lockOrientation(boolean portrait) {
        impl.lockOrientation(portrait);
    }

    /// This is the reverse method for lock orientation allowing orientation lock to be disabled
    ///
    /// Since version 6.0, orientation lock is supported in Javascript builds in some browsers.  For a full
    /// list of browsers the support locking orientation, see the [MDN Lock Orientation docs](https://developer.mozilla.org/en-US/docs/Web/API/Screen/lockOrientation).
    ///
    /// **NOTE:** In Javascript builds, orientation lock is only supported if the app is running in full-screen mode.  If the app is not
    /// currently in full-screen mode, then `#canForceOrientation()` will return false and `#lockOrientation(boolean)` will do nothing.
    ///
    /// #### See also
    ///
    /// - #lockOrientation(boolean)
    ///
    /// - #canForceOrientation()
    public void unlockOrientation() {
        impl.unlockOrientation();
    }

    /// Indicates whether the device is a tablet, notice that this is often a guess
    ///
    /// #### Returns
    ///
    /// true if the device is assumed to be a tablet
    public boolean isTablet() {
        return impl.isTablet();
    }

    /// Returns true if this is a desktop application
    ///
    /// #### Returns
    ///
    /// true if this is a desktop application
    public boolean isDesktop() {
        return impl.isDesktop();
    }

    /// Returns true if the device has dialing capabilities
    ///
    /// #### Returns
    ///
    /// false if it cannot dial
    public boolean canDial() {
        return impl.canDial();
    }

    /// On most platforms it is quite fast to draw on a mutable image and then render that
    /// image, however some platforms have much slower mutable images in comparison to just
    /// drawing on the screen. These platforms should return false here and Codename One will try
    /// to use less mutable image related optimizations in transitions and other operations.
    ///
    /// #### Returns
    ///
    /// true if mutable images are fast on this platform
    public boolean areMutableImagesFast() {
        return impl.areMutableImagesFast();
    }

    /// This method returns the platform Location Manager used for geofencing. This allows tracking the
    /// user location in the background. Usage:
    ///
    /// ```java
    /// // File: BGLocationTest.java
    /// public void showForm() {
    ///     Form hi = new Form("Hi World");
    ///     hi.addComponent(new Label("Hi World"));
    ///
    ///     Location loc = new Location();
    ///     loc.setLatitude(51.5033630);
    ///     loc.setLongitude(-0.1276250);
    ///
    ///     Geofence gf = new Geofence("test", loc, 100, 100000);
    ///
    ///     LocationManager.getLocationManager().addGeoFencing(GeofenceListenerImpl.class, gf);
    ///
    ///     hi.show();
    /// }
    /// ```
    ///
    /// ```java
    /// // File: GeofenceListenerImpl.java
    /// public class GeofenceListenerImpl implements GeofenceListener {
    ///     public void onExit(String id) {
    ///         System.out.println("Exited "+id);
    ///     }
    ///
    ///     public void onEntered(String id) {
    ///         System.out.println("Entered "+id);
    ///     }
    /// }
    /// ```
    ///
    /// ```java
    /// `public class GeofenceListenerImpl implements GeofenceListener {
    /// public void onExit(String id) {
    /// System.out.println("Exited "+id);`
    ///
    /// public void onEntered(String id) {
    /// System.out.println("Entered "+id);
    /// }
    /// }
    /// Form hi = new Form("Hi World");
    /// hi.addComponent(new Label("Hi World"));
    ///
    /// Location loc = new Location();
    /// loc.setLatitude(51.5033630);
    /// loc.setLongitude(-0.1276250);
    ///
    /// Geofence gf = new Geofence("test", loc, 100, 100000);
    ///
    /// LocationManager.getLocationManager().addGeoFencing(GeofenceListenerImpl.class, gf);
    ///
    /// hi.show();}
    /// ```
    ///
    /// #### Returns
    ///
    /// LocationManager Object
    public LocationManager getLocationManager() {
        return impl.getLocationManager();
    }

    /// This method tries to invoke the device native camera to capture images.
    /// The method returns immediately and the response will be sent asynchronously
    /// to the given ActionListener Object
    /// The image is saved as a jpeg to a file on the device.
    ///
    /// use this in the actionPerformed to retrieve the file path
    /// String path = (String) evt.getSource();
    ///
    /// if evt returns null the image capture was cancelled by the user.
    ///
    /// #### Parameters
    ///
    /// - `response`: a callback Object to retrieve the file path
    ///
    /// #### Throws
    ///
    /// - `RuntimeException`: if this feature failed or unsupported on the platform
    public void capturePhoto(ActionListener response) {
        impl.capturePhoto(response);
    }

    /// This method tries to invoke the device native hardware to capture audio.
    /// The method returns immediately and the response will be sent asynchronously
    /// to the given ActionListener Object
    /// The audio is saved to a file on the device.
    ///
    /// use this in the actionPerformed to retrieve the file path
    /// String path = (String) evt.getSource();
    ///
    /// #### Parameters
    ///
    /// - `response`: a callback Object to retrieve the file path
    ///
    /// #### Throws
    ///
    /// - `RuntimeException`: if this feature failed or unsupported on the platform
    public void captureAudio(ActionListener<ActionEvent> response) {
        impl.captureAudio(response);
    }

    /// This method tries to invoke the device native hardware to capture audio.
    /// The method returns immediately and the response will be sent asynchronously
    /// to the given ActionListener Object
    /// The audio is saved to a file on the device.
    ///
    /// use this in the actionPerformed to retrieve the file path
    /// String path = (String) evt.getSource();
    ///
    /// #### Parameters
    ///
    /// - `recordingOptions`: Audio recording options.
    ///
    /// - `response`: a callback Object to retrieve the file path
    ///
    /// #### Throws
    ///
    /// - `RuntimeException`: if this feature failed or unsupported on the platform
    ///
    /// #### Since
    ///
    /// 7.0
    public void captureAudio(MediaRecorderBuilder recordingOptions, ActionListener response) {
        impl.captureAudio(recordingOptions, response);
    }

    /// This method tries to invoke the device native camera to capture video.
    /// The method returns immediately and the response will be sent asynchronously
    /// to the given ActionListener Object
    /// The video is saved to a file on the device.
    ///
    /// use this in the actionPerformed to retrieve the file path
    /// String path = (String) evt.getSource();
    ///
    /// #### Parameters
    ///
    /// - `response`: a callback Object to retrieve the file path
    ///
    /// #### Throws
    ///
    /// - `RuntimeException`: if this feature failed or unsupported on the platform
    public void captureVideo(ActionListener response) {
        impl.captureVideo(response);
    }

    /// Same as `#captureVideo(com.codename1.ui.events.ActionListener)`, except that it
    /// attempts to impose constraints on the capture.  Constraints include width, height,
    /// and max length.  Not all platforms support capture constraints.  Use the `VideoCaptureConstraints#isSupported()`
    /// to see if a constraint is supported.  If constraints are not supported at all, then this method
    /// will fall back to calling `#captureVideo(com.codename1.ui.events.ActionListener)`.
    ///
    /// #### Parameters
    ///
    /// - `constraints`: Capture constraints to use.
    ///
    /// - `response`: a callback Object to retrieve the file path
    ///
    /// #### Since
    ///
    /// 7.0
    ///
    /// #### See also
    ///
    /// - com.codename1.capture.Capture#captureVideo(com.codename1.capture.VideoCaptureConstraints, com.codename1.ui.events.ActionListener)
    public void captureVideo(VideoCaptureConstraints constraints, ActionListener response) {
        impl.captureVideo(constraints, response);
    }

    /// Opens the device image gallery
    /// The method returns immediately and the response will be sent asynchronously
    /// to the given ActionListener Object
    ///
    /// use this in the actionPerformed to retrieve the file path
    /// String path = (String) evt.getSource();
    ///
    /// #### Parameters
    ///
    /// - `response`: a callback Object to retrieve the file path
    ///
    /// #### Throws
    ///
    /// - `RuntimeException`: if this feature failed or unsupported on the platform
    ///
    /// #### Deprecated
    ///
    /// see openGallery instead
    public void openImageGallery(ActionListener response) {
        if (pluginSupport.firePluginEvent(new OpenGalleryEvent(response, Display.GALLERY_IMAGE)).isConsumed()) {
            return;
        }
        impl.openImageGallery(response);
    }

    /// Opens the device gallery to pick an image or a video.
    ///
    /// The method returns immediately and the response is sent asynchronously
    /// to the given ActionListener Object as the source value of the event (as a String)
    ///
    /// E.g. within the callback action performed call you can use this code: `String path = (String) evt.getSource();`.
    ///
    /// A more detailed sample of picking a video file can be seen here:
    ///
    /// ```java
    /// final Form hi = new Form("MediaPlayer", new BorderLayout());
    /// hi.setToolbar(new Toolbar());
    /// Style s = UIManager.getInstance().getComponentStyle("Title");
    /// FontImage icon = FontImage.createMaterial(FontImage.MATERIAL_VIDEO_LIBRARY, s);
    /// hi.getToolbar().addCommandToRightBar(new Command("", icon) {
    /// @Override
    ///     public void actionPerformed(ActionEvent evt) {
    ///         Display.getInstance().openGallery((e) -> {
    ///             if(e != null && e.getSource() != null) {
    ///                 String file = (String)e.getSource();
    ///                 try {
    ///                     Media video = MediaManager.createMedia(file, true);
    ///                     hi.removeAll();
    ///                     hi.add(BorderLayout.CENTER, new MediaPlayer(video));
    ///                     hi.revalidate();
    ///                 } catch(IOException err) {
    ///                     Log.e(err);
    ///                 }
    ///             }
    ///         }, Display.GALLERY_VIDEO);
    ///     }
    /// });
    /// hi.show();
    /// ```
    ///
    /// Version 5.0 and higher support multi-selection (i.e. the types `#GALLERY_IMAGE_MULTI`, `#GALLERY_VIDEO_MULTI`, and `#GALLERY_ALL_MULTI`).  When using one of the multiselection
    /// types, the source of the ActionEvent will be a `String[]`, containing the paths of the selected elements, or null if the user cancelled the dialog.
    ///
    /// Platform support
    ///
    /// Currently (version 5.0 and higher), all platforms support the types `#GALLERY_IMAGE`, `#GALLERY_VIDEO`, `#GALLERY_ALL`, `#GALLERY_IMAGE_MULTI`, `#GALLERY_VIDEO_MULTI`, `#GALLERY_ALL_MULTI`.  On iOS,
    /// multi-selection requires a deployment target of iOS 8.0 or higher, so it is disabled by default.   You can enable multi-selection on iOS, by adding the ios.enableGalleryMultiselect=true build hint.  This
    /// build hint will be added automatically for you if you run your app in the simulator, and it calls openGallery() with one of the multiselect gallery types.
    ///
    /// #### Parameters
    ///
    /// - `response`: @param response a callback Object to retrieve the file path For multiselection types (`#GALLERY_IMAGE_MULTI`, `#GALLERY_VIDEO_MULTI`, and `#GALLERY_ALL_MULTI`), the source
    ///                 of the ActionEvent sent this callback will be a String[].  For other types, it will be a String.  If the dialog was cancelled, it will be null.
    ///
    /// - `type`: one of the following `#GALLERY_IMAGE`, `#GALLERY_VIDEO`, `#GALLERY_ALL`, `#GALLERY_IMAGE_MULTI`, `#GALLERY_VIDEO_MULTI`, `#GALLERY_ALL_MULTI`.
    ///
    /// #### Throws
    ///
    /// - `RuntimeException`: if this feature failed or unsupported on the platform.  Use `#isGalleryTypeSupported(int)` to check if the type is supported before calling this method.
    ///
    /// #### See also
    ///
    /// - #isGalleryTypeSupported(int) To see if a type is supported on the current platform.
    public void openGallery(ActionListener response, int type) {
        if (pluginSupport.firePluginEvent(new OpenGalleryEvent(response, type)).isConsumed()) {
            return;
        }

        impl.openGallery(response, type);
    }

    /// Checks to see if the given gallery type is supported on the current platform.
    ///
    /// #### Parameters
    ///
    /// - `type`: one of the following `#GALLERY_IMAGE`, `#GALLERY_VIDEO`, `#GALLERY_ALL`, `#GALLERY_IMAGE_MULTI`, `#GALLERY_VIDEO_MULTI`, `#GALLERY_ALL_MULTI`.
    ///
    /// #### Returns
    ///
    /// True if the type is supported
    ///
    /// #### See also
    ///
    /// - #openGallery(com.codename1.ui.events.ActionListener, int)
    public boolean isGalleryTypeSupported(int type) {
        IsGalleryTypeSupportedEvent evt = new IsGalleryTypeSupportedEvent(type);
        if (pluginSupport.firePluginEvent(evt).isConsumed()) {
            return evt.getPluginEventResponse();
        }
        return impl.isGalleryTypeSupported(type);
    }

    /// Returns a 2-3 letter code representing the platform name for the platform override
    ///
    /// #### Returns
    ///
    /// the name of the platform e.g. ios, rim, win, and, me, HTML5
    public String getPlatformName() {
        return impl.getPlatformName();
    }

    /// Returns the suffixes for ovr files that should be used when loading a layered resource file on this platform
    ///
    /// #### Returns
    ///
    /// a string array with the proper order of resource override layers
    public String[] getPlatformOverrides() {
        return impl.getPlatformOverrides();
    }

    /// Send an email using the platform mail client.
    ///
    /// The code below demonstrates sending a simple message with attachments using the devices
    /// native email client:
    ///
    /// ```java
    /// Message m = new Message("Body of message");
    /// m.getAttachments().put(textAttachmentUri, "text/plain");
    /// m.getAttachments().put(imageAttachmentUri, "image/png");
    /// Display.getInstance().sendMessage(new String[] {"someone@gmail.com"}, "Subject of message", m);
    /// ```
    ///
    /// #### Parameters
    ///
    /// - `recipients`: array of e-mail addresses
    ///
    /// - `subject`: e-mail subject
    ///
    /// - `msg`: the Message to send
    public void sendMessage(String[] recipients, String subject, Message msg) {
        impl.sendMessage(recipients, subject, msg);
    }

    /// Opens the device Dialer application with the given phone number
    ///
    /// #### Parameters
    ///
    /// - `phoneNumber`
    public void dial(String phoneNumber) {
        impl.dial(phoneNumber);
    }

    /// Indicates the level of SMS support in the platform as one of:
    /// `#SMS_NOT_SUPPORTED` (for desktop, tablet etc.),
    /// `#SMS_SEAMLESS` (no UI interaction), `#SMS_INTERACTIVE` (with compose UI),
    /// `#SMS_BOTH`.
    ///
    /// The sample below demonstrates the use case for this property:
    ///
    /// ```java
    /// switch(Display.getInstance().getSMSSupport()) {
    ///     case Display.SMS_NOT_SUPPORTED:
    ///         return;
    ///     case Display.SMS_SEAMLESS:
    ///         showUIDialogToEditMessageData();
    ///         Display.getInstance().sendSMS(phone, data);
    ///         return;
    ///     default:
    ///         Display.getInstance().sendSMS(phone, data);
    ///         return;
    /// }
    /// ```
    ///
    /// #### Returns
    ///
    /// one of the SMS_* values
    public int getSMSSupport() {
        return impl.getSMSSupport();
    }

    /// Sends a SMS message to the given phone number
    ///
    /// #### Parameters
    ///
    /// - `phoneNumber`: to send the sms
    ///
    /// - `message`: the content of the sms
    public void sendSMS(String phoneNumber, String message) throws IOException {
        impl.sendSMS(phoneNumber, message, false);
    }

    /// Sends a SMS message to the given phone number, the code below demonstrates the logic
    /// of detecting platform behavior for sending SMS.
    ///
    /// ```java
    /// switch(Display.getInstance().getSMSSupport()) {
    ///     case Display.SMS_NOT_SUPPORTED:
    ///         return;
    ///     case Display.SMS_SEAMLESS:
    ///         showUIDialogToEditMessageData();
    ///         Display.getInstance().sendSMS(phone, data);
    ///         return;
    ///     default:
    ///         Display.getInstance().sendSMS(phone, data);
    ///         return;
    /// }
    /// ```
    ///
    /// #### Parameters
    ///
    /// - `phoneNumber`: to send the sms
    ///
    /// - `message`: the content of the sms
    ///
    /// - `interactive`: indicates the SMS should show a UI or should not show a UI if applicable see getSMSSupport
    ///
    /// #### See also
    ///
    /// - #getSMSSupport()
    public void sendSMS(String phoneNumber, String message, boolean interactive) throws IOException {
        impl.sendSMS(phoneNumber, message, interactive);
    }

    /// Place a notification on the device status bar (if device has this
    /// functionality).
    /// Clicking the notification might re-start the Application.
    ///
    /// #### Parameters
    ///
    /// - `tickerText`: the ticker text of the Notification
    ///
    /// - `contentTitle`: the title of the Notification
    ///
    /// - `contentBody`: the content of the Notification
    ///
    /// - `vibrate`: enable/disable notification alert
    ///
    /// - `flashLights`: enable/disable notification flashing
    ///
    /// #### Deprecated
    ///
    /// @deprecated there is a new version of this method with a slightly improved
    /// signature
    public void notifyStatusBar(String tickerText, String contentTitle,
                                String contentBody, boolean vibrate, boolean flashLights) {
        notifyStatusBar(tickerText, contentTitle, contentBody, vibrate, flashLights, null);
    }

    /// Indicates whether the notify status bar method will present a notification to the user
    ///
    /// #### Returns
    ///
    /// true if the notify status bar method will present a notification to the user
    public boolean isNotificationSupported() {
        return impl.isNotificationSupported();
    }

    /// Place a notification on the device status bar (if device has this
    /// functionality).
    /// Clicking the notification might re-start the Application.
    ///
    /// #### Parameters
    ///
    /// - `tickerText`: the ticker text of the Notification
    ///
    /// - `contentTitle`: the title of the Notification
    ///
    /// - `contentBody`: the content of the Notification
    ///
    /// - `vibrate`: enable/disable notification alert
    ///
    /// - `flashLights`: enable/disable notification flashing
    ///
    /// - `args`: additional arguments to the notification
    ///
    /// #### Returns
    ///
    /// a platform native object that allows modifying notification state
    ///
    /// #### Deprecated
    ///
    /// use scheduleLocalNotification instead
    public Object notifyStatusBar(String tickerText, String contentTitle,
                                  String contentBody, boolean vibrate, boolean flashLights, Hashtable args) {
        return impl.notifyStatusBar(tickerText, contentTitle, contentBody, vibrate, flashLights, args);
    }

    /// Removes the notification previously posted with the notify status bar method
    ///
    /// #### Parameters
    ///
    /// - `o`: the object returned from the notifyStatusBar method
    public void dismissNotification(Object o) {
        impl.dismissNotification(o);
    }

    /// Returns true if the underlying OS supports numeric badges on icons. Notice this is only available on iOS
    /// and only when push notification is enabled
    ///
    /// #### Returns
    ///
    /// true if the underlying OS supports numeric badges
    public boolean isBadgingSupported() {
        return impl.isBadgingSupported();
    }

    /// Sets the number that appears on the application icon in iOS
    ///
    /// #### Parameters
    ///
    /// - `number`: number to show on the icon
    public void setBadgeNumber(int number) {
        impl.setBadgeNumber(number);
    }

    /// Returns true if the underlying OS supports opening the native navigation
    /// application
    ///
    /// #### Returns
    ///
    /// true if the underlying OS supports launch of native navigation app
    public boolean isOpenNativeNavigationAppSupported() {
        return impl.isOpenNativeNavigationAppSupported();
    }

    /// Opens the native navigation app in the given coordinate.
    ///
    /// #### Parameters
    ///
    /// - `latitude`
    ///
    /// - `longitude`
    public void openNativeNavigationApp(double latitude, double longitude) {
        impl.openNativeNavigationApp(latitude, longitude);
    }

    /// Opens the native navigation app with the given search location
    ///
    /// #### Parameters
    ///
    /// - `location`: the location to search for in the native navigation map
    public void openNativeNavigationApp(String location) {
        impl.openNativeNavigationApp(location);
    }

    /// Gets all contacts from the address book of the device
    ///
    /// #### Parameters
    ///
    /// - `withNumbers`: if true returns only contacts that has a number
    ///
    /// #### Returns
    ///
    /// array of contacts unique ids
    public String[] getAllContacts(boolean withNumbers) {
        return impl.getAllContacts(withNumbers);
    }

    /// Notice: this method might be very slow and should be invoked on a separate thread!
    /// It might have platform specific optimizations over getAllContacts followed by looping
    /// over individual contacts but that isn't guaranteed. See isGetAllContactsFast for
    /// information.
    ///
    /// The sample below demonstrates listing all the contacts within the device with their photos
    ///
    /// ```java
    /// Form hi = new Form("Contacts", new BoxLayout(BoxLayout.Y_AXIS));
    /// hi.add(new InfiniteProgress());
    /// int size = Display.getInstance().convertToPixels(5, true);
    /// FontImage fi = FontImage.createFixed("" + FontImage.MATERIAL_PERSON, FontImage.getMaterialDesignFont(), 0xff, size, size);
    ///
    /// Display.getInstance().scheduleBackgroundTask(() -> {
    ///     Contact[] contacts = Display.getInstance().getAllContacts(true, true, false, true, false, false);
    ///     Display.getInstance().callSerially(() -> {
    ///         hi.removeAll();
    ///         for(Contact c : contacts) {
    ///             MultiButton mb = new MultiButton(c.getDisplayName());
    ///             mb.setIcon(fi);
    ///             mb.setTextLine2(c.getPrimaryPhoneNumber());
    ///             hi.add(mb);
    ///             mb.putClientProperty("id", c.getId());
    ///             Display.getInstance().scheduleBackgroundTask(() -> {
    ///                 Contact cc = ContactsManager.getContactById(c.getId(), false, true, false, false, false);
    ///                 Display.getInstance().callSerially(() -> {
    ///                     Image photo = cc.getPhoto();
    ///                     if(photo != null) {
    ///                         mb.setIcon(photo.fill(size, size));
    ///                         mb.revalidate();
    ///                     }
    ///                 });
    ///             });
    ///         }
    ///         hi.getContentPane().animateLayout(150);
    ///     });
    /// });
    /// ```
    ///
    /// #### Parameters
    ///
    /// - `withNumbers`: if true returns only contacts that has a number
    ///
    /// - `includesFullName`: if true try to fetch the full name of the Contact(not just display name)
    ///
    /// - `includesPicture`: if true try to fetch the Contact Picture if exists
    ///
    /// - `includesNumbers`: if true try to fetch all Contact numbers
    ///
    /// - `includesEmail`: if true try to fetch all Contact Emails
    ///
    /// - `includeAddress`: if true try to fetch all Contact Addresses
    ///
    /// #### Returns
    ///
    /// array of the contacts
    public Contact[] getAllContacts(boolean withNumbers, boolean includesFullName, boolean includesPicture, boolean includesNumbers, boolean includesEmail, boolean includeAddress) {
        return impl.getAllContacts(withNumbers, includesFullName, includesPicture, includesNumbers, includesEmail, includeAddress);
    }

    /// Indicates if the getAllContacts is platform optimized, notice that the method
    /// might still take seconds or more to run so you should still use a separate thread!
    ///
    /// #### Returns
    ///
    /// true if getAllContacts will perform faster that just getting each contact
    public boolean isGetAllContactsFast() {
        return impl.isGetAllContactsFast();
    }

    /// Gets IDs of all contacts that are linked to a given contact.  Some platforms, like iOS, allow for multiple distinct contact records to be "linked" to indicate that they refer to the same person.
    ///
    /// #### Parameters
    ///
    /// - `c`: The contact whose "linked" contacts are to be retrieved.
    ///
    /// #### Returns
    ///
    /// IDs of linked contacts.
    public String[] getLinkedContactIds(Contact c) {
        return impl.getLinkedContactIds(c);
    }

    /// Get a Contact according to it's contact id.
    ///
    /// #### Parameters
    ///
    /// - `id`: unique id of the Contact
    ///
    /// #### Returns
    ///
    /// a Contact Object
    public Contact getContactById(String id) {
        return impl.getContactById(id);
    }

    /// Gets all of the contacts that are linked to this contact.  Some platforms, like iOS, allow for multiple distinct contact records to be "linked" to indicate that they refer to the same person.
    ///
    /// #### Parameters
    ///
    /// - `c`: The contact whose "linked" contacts are to be retrieved.
    ///
    /// #### Returns
    ///
    /// Array of Contacts.  Should never be null, but may be a zero-sized array.
    ///
    /// #### See also
    ///
    /// - ContactsManager#getLinkedContacts(com.codename1.contacts.Contact)
    //public Contact[] getLinkedContacts(Contact c) {
    //    return impl.getLinkedContacts(c);
    //}

    /// This method returns a Contact by the contact id and fills it's data
    /// according to the given flags.
    ///
    /// The sample below demonstrates listing all the contacts within the device with their photos
    ///
    /// ```java
    /// Form hi = new Form("Contacts", new BoxLayout(BoxLayout.Y_AXIS));
    /// hi.add(new InfiniteProgress());
    /// int size = Display.getInstance().convertToPixels(5, true);
    /// FontImage fi = FontImage.createFixed("" + FontImage.MATERIAL_PERSON, FontImage.getMaterialDesignFont(), 0xff, size, size);
    ///
    /// Display.getInstance().scheduleBackgroundTask(() -> {
    ///     Contact[] contacts = Display.getInstance().getAllContacts(true, true, false, true, false, false);
    ///     Display.getInstance().callSerially(() -> {
    ///         hi.removeAll();
    ///         for(Contact c : contacts) {
    ///             MultiButton mb = new MultiButton(c.getDisplayName());
    ///             mb.setIcon(fi);
    ///             mb.setTextLine2(c.getPrimaryPhoneNumber());
    ///             hi.add(mb);
    ///             mb.putClientProperty("id", c.getId());
    ///             Display.getInstance().scheduleBackgroundTask(() -> {
    ///                 Contact cc = ContactsManager.getContactById(c.getId(), false, true, false, false, false);
    ///                 Display.getInstance().callSerially(() -> {
    ///                     Image photo = cc.getPhoto();
    ///                     if(photo != null) {
    ///                         mb.setIcon(photo.fill(size, size));
    ///                         mb.revalidate();
    ///                     }
    ///                 });
    ///             });
    ///         }
    ///         hi.getContentPane().animateLayout(150);
    ///     });
    /// });
    /// ```
    ///
    /// #### Parameters
    ///
    /// - `id`: of the Contact
    ///
    /// - `includesFullName`: if true try to fetch the full name of the Contact(not just display name)
    ///
    /// - `includesPicture`: if true try to fetch the Contact Picture if exists
    ///
    /// - `includesNumbers`: if true try to fetch all Contact numbers
    ///
    /// - `includesEmail`: if true try to fetch all Contact Emails
    ///
    /// - `includeAddress`: if true try to fetch all Contact Addresses
    ///
    /// #### Returns
    ///
    /// a Contact Object
    public Contact getContactById(String id, boolean includesFullName,
                                  boolean includesPicture, boolean includesNumbers, boolean includesEmail,
                                  boolean includeAddress) {
        return impl.getContactById(id, includesFullName, includesPicture,
                includesNumbers, includesEmail, includeAddress);
    }

    /// Some platforms allow the user to block contacts access on a per application basis this method
    /// returns true if the user denied permission to access contacts. This can allow you to customize the error
    /// message presented to the user.
    ///
    /// #### Returns
    ///
    /// true if contacts access is allowed or globally available, false otherwise
    public boolean isContactsPermissionGranted() {
        return impl.isContactsPermissionGranted();
    }

    /// Create a contact to the device contacts book
    ///
    /// #### Parameters
    ///
    /// - `firstName`: the Contact firstName
    ///
    /// - `familyName`: the Contact familyName
    ///
    /// - `officePhone`: the Contact work phone or null
    ///
    /// - `homePhone`: the Contact home phone or null
    ///
    /// - `cellPhone`: the Contact mobile phone or null
    ///
    /// - `email`: the Contact email or null
    ///
    /// #### Returns
    ///
    /// the contact id if creation succeeded or null  if failed
    public String createContact(String firstName, String familyName, String officePhone, String homePhone, String cellPhone, String email) {
        return impl.createContact(firstName, familyName, officePhone, homePhone, cellPhone, email);
    }

    /// removed a contact from the device contacts book
    ///
    /// #### Parameters
    ///
    /// - `id`: the contact id to remove
    ///
    /// #### Returns
    ///
    /// true if deletion succeeded false otherwise
    public boolean deleteContact(String id) {
        return impl.deleteContact(id);
    }

    /// Indicates if the native video player includes its own play/pause etc. controls so the movie player
    /// component doesn't need to include them
    ///
    /// #### Returns
    ///
    /// true if the movie player component doesn't need to include such controls
    public boolean isNativeVideoPlayerControlsIncluded() {
        return impl.isNativeVideoPlayerControlsIncluded();
    }

    /// Indicates if the underlying platform supports sharing capabilities
    ///
    /// #### Returns
    ///
    /// true if the underlying platform handles share.
    public boolean isNativeShareSupported() {
        return impl.isNativeShareSupported();
    }

    /// Share the required information using the platform sharing services.
    /// a Sharing service can be: mail, sms, facebook, twitter,...
    /// This method is implemented if isNativeShareSupported() returned true for
    /// a specific platform.
    ///
    /// Since 6.0, there is native sharing support in the Javascript port using the [navigator.share](https://developer.mozilla.org/en-US/docs/Web/API/Navigator/share)
    /// API.  Currently (2019) this is only supported on Chrome for Android, and will only work if the app is accessed over https:.
    ///
    /// #### Parameters
    ///
    /// - `toShare`: String to share.
    ///
    /// #### Deprecated
    ///
    /// use the method share that accepts an image and mime type
    public void share(String toShare) {
        share(toShare, null, null);
    }

    /// Share the required information using the platform sharing services.
    /// a Sharing service can be: mail, sms, facebook, twitter,...
    /// This method is implemented if isNativeShareSupported() returned true for
    /// a specific platform.
    ///
    /// Since 6.0, there is native sharing support in the Javascript port using the [navigator.share](https://developer.mozilla.org/en-US/docs/Web/API/Navigator/share)
    /// API.  Currently (2019) this is only supported on Chrome for Android, and will only work if the app is accessed over https:.
    ///
    /// #### Parameters
    ///
    /// - `text`: String to share.
    ///
    /// - `image`: file path to the image or null
    ///
    /// - `mimeType`: type of the image or null if no image to share
    public void share(String text, String image, String mimeType) {
        share(text, image, mimeType, null);

    }

    /// Share the required information using the platform sharing services.
    /// a Sharing service can be: mail, sms, facebook, twitter,...
    /// This method is implemented if isNativeShareSupported() returned true for
    /// a specific platform.
    ///
    /// Since 6.0, there is native sharing support in the Javascript port using the [navigator.share](https://developer.mozilla.org/en-US/docs/Web/API/Navigator/share)
    /// API.  Currently (2019) this is only supported on Chrome for Android, and will only work if the app is accessed over https:.
    ///
    /// Since 8.0, you can share files using using the file path in the text parameter.  The file must exist in file system storage, and
    /// you must define the appropriate mimeType in the mimeType parameter.  E.g. `share("file:/.../myfile.pdf", null, "application.pdf")`
    ///
    /// #### Parameters
    ///
    /// - `textOrPath`: String to share, or path to file to share.
    ///
    /// - `image`: file path to the image or null
    ///
    /// - `mimeType`: type of the image or file.  null if just sharing text
    ///
    /// - `sourceRect`: @param sourceRect The source rectangle of the button that originated the share request.  This is used on
    ///                   some platforms to provide a hint as to where the share dialog overlay should pop up.  Particularly,
    ///                   on the iPad with iOS 8 and higher.
    public void share(String textOrPath, String image, String mimeType, Rectangle sourceRect) {
        impl.share(textOrPath, image, mimeType, sourceRect);
    }

    /// The localization manager allows adapting values for display in different locales thru parsing and formatting
    /// capabilities (similar to JavaSE's DateFormat/NumberFormat). It also includes language/locale/currency
    /// related API's similar to Locale/currency API's from JavaSE.
    ///
    /// The sample code below just lists the various capabilities of the API:
    ///
    /// ```java
    /// Form hi = new Form("L10N", new TableLayout(16, 2));
    /// L10NManager l10n = L10NManager.getInstance();
    /// hi.add("format(double)").add(l10n.format(11.11)).
    ///     add("format(int)").add(l10n.format(33)).
    ///     add("formatCurrency").add(l10n.formatCurrency(53.267)).
    ///     add("formatDateLongStyle").add(l10n.formatDateLongStyle(new Date())).
    ///     add("formatDateShortStyle").add(l10n.formatDateShortStyle(new Date())).
    ///     add("formatDateTime").add(l10n.formatDateTime(new Date())).
    ///     add("formatDateTimeMedium").add(l10n.formatDateTimeMedium(new Date())).
    ///     add("formatDateTimeShort").add(l10n.formatDateTimeShort(new Date())).
    ///     add("getCurrencySymbol").add(l10n.getCurrencySymbol()).
    ///     add("getLanguage").add(l10n.getLanguage()).
    ///     add("getLocale").add(l10n.getLocale()).
    ///     add("isRTLLocale").add("" + l10n.isRTLLocale()).
    ///     add("parseCurrency").add(l10n.formatCurrency(l10n.parseCurrency("33.77$"))).
    ///     add("parseDouble").add(l10n.format(l10n.parseDouble("34.35"))).
    ///     add("parseInt").add(l10n.format(l10n.parseInt("56"))).
    ///     add("parseLong").add("" + l10n.parseLong("4444444"));
    /// hi.show();
    /// ```
    ///
    /// #### Returns
    ///
    /// an instance of the localization manager
    public L10NManager getLocalizationManager() {
        return impl.getLocalizationManager();
    }

    /// User register to receive push notification
    ///
    /// #### Parameters
    ///
    /// - `id`: the id for the user
    ///
    /// - `noFallback`: @param noFallback some devices don't support an efficient push API and will resort to polling
    ///                   to provide push like functionality. If this flag is set to true no polling will occur and
    ///                   the error PushCallback.REGISTRATION_ERROR_SERVICE_NOT_AVAILABLE will be sent to the push interface.
    ///
    /// #### Deprecated
    ///
    /// use `#registerPush()` the Android push id should be set with the build hint `gcm.sender_id` which will work for Chrome JavaScript builds too
    public void registerPush(String id, boolean noFallback) {
        Hashtable h = new Hashtable();
        h.put("googlePlay", id);
        registerPush(h, noFallback);
    }

    /// Register to receive push notification, invoke this method once (ever) to receive push
    /// notifications.
    ///
    /// #### Parameters
    ///
    /// - `metaData`: @param metaData   meta data for push, this is relevant on some platforms such as google where
    ///                   a push id is necessary,
    ///
    /// - `noFallback`: @param noFallback some devices don't support an efficient push API and will resort to polling
    ///                   to provide push like functionality. If this flag is set to true no polling will occur and
    ///                   the error PushCallback.REGISTRATION_ERROR_SERVICE_NOT_AVAILABLE will be sent to the push interface.
    ///
    /// #### Deprecated
    ///
    /// use `#registerPush()` the Android push id should be set with the build hint `gcm.sender_id` which will work for Chrome JavaScript builds too
    public void registerPush(Hashtable metaData, boolean noFallback) {
        if (Preferences.get("push_id", (long) -1) == -1) {
            impl.registerPush(metaData, noFallback);
        }
    }

    /// Register to receive push notification, invoke this method once (ever) to receive push
    /// notifications.
    public void registerPush() {
        impl.registerPush(new Hashtable(), false);
    }

    /// Stop receiving push notifications to this client application
    public void deregisterPush() {
        impl.deregisterPush();
    }

    /// Creates a Media recorder Object which will record from the device mic to
    /// a file in the given path.
    /// The output format will be amr-nb if supported by the platform.
    ///
    /// #### Parameters
    ///
    /// - `path`: @param path a file path to where to store the recording, if the file does
    ///             not exists it will be created.
    public Media createMediaRecorder(String path) throws IOException {
        return createMediaRecorder(path, getAvailableRecordingMimeTypes()[0]);
    }

    /// #### Parameters
    ///
    /// - `builder`: A MediaRecorderBuilder
    ///
    /// #### Returns
    ///
    /// a MediaRecorder
    ///
    /// #### Throws
    ///
    /// - `IOException`
    ///
    /// #### Since
    ///
    /// 7.0
    ///
    /// #### Deprecated
    ///
    /// use MediaRecorderBuilder#build()
    ///
    /// #### See also
    ///
    /// - MediaRecorderBuilder#build()
    public Media createMediaRecorder(MediaRecorderBuilder builder) throws IOException {
        return impl.createMediaRecorder(builder);
    }

    /// Creates a Media recorder Object which will record from the device mic to
    /// a file in the given path.
    ///
    /// #### Parameters
    ///
    /// - `path`: @param path     a file path to where to store the recording, if the file does
    ///                 not exists it will be created.
    ///
    /// - `mimeType`: @param mimeType the output mime type that is supported see
    ///                 getAvailableRecordingMimeTypes()
    public Media createMediaRecorder(String path, String mimeType) throws IOException {
        return impl.createMediaRecorder(path, mimeType);
    }

    /// Returns the image IO instance that allows scaling image files.
    ///
    /// #### Returns
    ///
    /// the image IO instance or null if image IO isn't supported for the given platform
    public ImageIO getImageIO() {
        return impl.getImageIO();
    }

    /// Gets the recording mime type for the returned Media from the
    /// createMediaRecorder method
    ///
    /// #### Returns
    ///
    /// the recording mime type
    ///
    /// #### Deprecated
    ///
    /// see getAvailableRecordingMimeTypes() instead
    public String getMediaRecorderingMimeType() {
        return impl.getAvailableRecordingMimeTypes()[0];
    }

    /// Opens a database or create one if not exists.  On platforms where `#isDatabaseCustomPathSupported()`
    /// this method can optionally accept a file path.
    ///
    /// #### Parameters
    ///
    /// - `databaseName`: the name of the database
    ///
    /// #### Returns
    ///
    /// Database Object or null if not supported on the platform
    ///
    /// #### Throws
    ///
    /// - `IOException`: if database cannot be created
    public Database openOrCreate(String databaseName) throws IOException {
        return impl.openOrCreateDB(databaseName);
    }

    public boolean isDatabaseCustomPathSupported() {
        return impl.isDatabaseCustomPathSupported();
    }

    /// Deletes database
    ///
    /// #### Parameters
    ///
    /// - `databaseName`: the name of the database
    ///
    /// #### Throws
    ///
    /// - `IOException`: if database cannot be deleted
    public void delete(String databaseName) throws IOException {
        impl.deleteDB(databaseName);
    }

    /// Indicates weather a database exists
    ///
    /// #### Parameters
    ///
    /// - `databaseName`: the name of the database
    ///
    /// #### Returns
    ///
    /// true if database exists
    public boolean exists(String databaseName) {
        return impl.existsDB(databaseName);
    }

    /// Returns the file path of the Database if support for database exists
    /// on the platform.
    ///
    /// #### Parameters
    ///
    /// - `databaseName`: @param databaseName the name of the database with out / or path
    ///                     elements e.g. `mydatabase.db`
    ///
    /// #### Returns
    ///
    /// the file path of the database or null if database isn't supported
    public String getDatabasePath(String databaseName) {
        return impl.getDatabasePath(databaseName);
    }

    /// Sets the frequency for polling the server in case of polling based push notification
    ///
    /// #### Parameters
    ///
    /// - `freq`: the frequency in milliseconds
    ///
    /// #### Deprecated
    ///
    /// we no longer support push polling
    public void setPollingFrequency(int freq) {
        impl.setPollingFrequency(freq);
    }

    /// Start a Codename One thread that supports crash protection and similar Codename One features.
    ///
    /// #### Parameters
    ///
    /// - `r`: runnable to run, **NOTICE** the thread MUST be explicitly started!
    ///
    /// - `name`: the name for the thread
    ///
    /// #### Returns
    ///
    /// a thread instance which must be explicitly started!
    public Thread createThread(Runnable r, String name) {
        return new CodenameOneThread(r, name);
    }

    /// Start a Codename One thread that supports crash protection and similar Codename One features.
    ///
    /// #### Parameters
    ///
    /// - `r`: runnable to run, **NOTICE** the thread MUST be explicitly started!
    ///
    /// - `name`: the name for the thread
    ///
    /// #### Returns
    ///
    /// a thread instance which must be explicitly started!
    ///
    /// #### Deprecated
    ///
    /// confusing name use `java.lang.String)` instead
    public Thread startThread(Runnable r, String name) {
        return new CodenameOneThread(r, name);
    }

    /// Indicates if the title of the Form is native title(in android ICS devices
    /// if the command behavior is native the ActionBar is used to display the title
    /// and the menu)
    ///
    /// #### Returns
    ///
    /// true if platform would like to show the Form title
    public boolean isNativeTitle() {
        return impl.isNativeTitle();
    }

    /// if the title is native(e.g the android action bar), notify the native title
    /// that is needs to be refreshed
    public void refreshNativeTitle() {
        impl.refreshNativeTitle();
    }

    /// The crash reporter gets invoked when an uncaught exception is intercepted
    ///
    /// #### Returns
    ///
    /// the crashReporter
    public CrashReport getCrashReporter() {
        return crashReporter;
    }

    /// The crash reporter gets invoked when an uncaught exception is intercepted
    ///
    /// #### Parameters
    ///
    /// - `crashReporter`: the crashReporter to set
    public void setCrashReporter(CrashReport crashReporter) {
        this.crashReporter = crashReporter;
    }

    /// Returns the UDID for devices that support it
    ///
    /// #### Returns
    ///
    /// the UDID or null
    public String getUdid() {
        return impl.getUdid();
    }

    /// Returns the MSISDN for devices that expose it
    ///
    /// #### Returns
    ///
    /// the msisdn or null
    public String getMsisdn() {
        return impl.getMsisdn();
    }

    /// Returns the native OS purchase implementation if applicable, if unavailable this
    /// method will try to fallback to a custom purchase implementation and failing that
    /// will return null
    ///
    /// #### Returns
    ///
    /// instance of the purchase class or null
    public Purchase getInAppPurchase() {
        return impl.getInAppPurchase();
    }

    /// #### Deprecated
    ///
    /// @deprecated use the version that accepts no arguments, the physical goods purchase is always
    /// manual payment if applicable
    public Purchase getInAppPurchase(boolean d) {
        return getInAppPurchase();
    }

    /// Returns the native implementation of the code scanner or null
    ///
    /// #### Returns
    ///
    /// code scanner instance
    ///
    /// #### Deprecated
    ///
    /// Use the cn1-codescanner cn1lib.
    public CodeScanner getCodeScanner() {
        if (!hasCamera()) {
            return null;
        }
        return impl.getCodeScanner();
    }

    /// Gets the available recording MimeTypes
    public String[] getAvailableRecordingMimeTypes() {
        return impl.getAvailableRecordingMimeTypes();
    }

    /// Checks if the device supports disabling the screen display from dimming, allowing
    /// the developer to keep the screen display on.
    public boolean isScreenSaverDisableSupported() {
        return impl.isScreenLockSupported();
    }

    /// Checks is the scroll-wheel mouse is currently scrolling.  The scroll-wheel simulates pointer presses and drags
    /// so there are cases when you are processing pointer events when you may want to know if it was driggered by
    /// a scroll wheel.
    ///
    /// #### Returns
    ///
    /// True if the scroll-wheel is responsible for current pointer events.
    ///
    /// #### Since
    ///
    /// 8.0
    public boolean isScrollWheeling() {
        return impl.isScrollWheeling();
    }

    /// If isScreenSaverDisableSupported() returns true calling this method will
    /// lock the screen display on
    ///
    /// #### Parameters
    ///
    /// - `e`: @param e when set to true the screen saver will work as usual and when set to false the screen
    ///          will not turn off automatically
    public void setScreenSaverEnabled(boolean e) {
        if (e) {
            impl.unlockScreen();
        } else {
            impl.lockScreen();
        }
    }

    /// Returns true if the device has camera false otherwise.
    public boolean hasCamera() {
        return impl.hasCamera();
    }

    /// Indicates whether the native picker dialog is supported for the given type
    /// which can include one of PICKER_TYPE_DATE_AND_TIME, PICKER_TYPE_TIME, PICKER_TYPE_DATE
    ///
    /// #### Parameters
    ///
    /// - `pickerType`: the picker type constant
    ///
    /// #### Returns
    ///
    /// true if the native platform supports this picker type
    public boolean isNativePickerTypeSupported(int pickerType) {
        return impl.isNativePickerTypeSupported(pickerType);
    }

    /// Shows a native modal dialog allowing us to perform the picking for the given type
    /// which can include one of PICKER_TYPE_DATE_AND_TIME, PICKER_TYPE_TIME, PICKER_TYPE_DATE
    ///
    /// #### Parameters
    ///
    /// - `type`: the picker type constant
    ///
    /// - `source`: @param source       the source component (optional) the native dialog will be placed in relation to this
    ///                     component if applicable
    ///
    /// - `currentValue`: the currently selected value
    ///
    /// - `data`: additional meta data specific to the picker type when applicable
    ///
    /// #### Returns
    ///
    /// the value from the picker or null if the operation was canceled.
    public Object showNativePicker(int type, Component source, Object currentValue, Object data) {
        return impl.showNativePicker(type, source, currentValue, data);
    }

    /// When set to true Codename One allows multiple hardware keys to be pressed at once,
    /// this isn't on by default since it can trigger some complexities with UI navigation to/from
    /// native code
    ///
    /// #### Returns
    ///
    /// the multiKeyMode
    public boolean isMultiKeyMode() {
        return multiKeyMode;
    }

    /// When set to true Codename One allows multiple hardware keys to be pressed at once,
    /// this isn't on by default since it can trigger some complexities with UI navigation to/from
    /// native code
    ///
    /// #### Parameters
    ///
    /// - `multiKeyMode`: the multiKeyMode to set
    public void setMultiKeyMode(boolean multiKeyMode) {
        this.multiKeyMode = multiKeyMode;
    }

    /// Long pointer press is invoked after the given interval, this allows making long press events shorter/longer
    ///
    /// #### Returns
    ///
    /// time in milliseconds
    public int getLongPointerPressInterval() {
        return longPressInterval;
    }

    /// Long pointer press is invoked after the given interval, this allows making long press events shorter/longer
    ///
    /// #### Parameters
    ///
    /// - `v`: time in milliseconds
    public void setLongPointerPressInterval(int v) {
        longPressInterval = v;
    }

    /// Schedules a local notification that will occur after the given time elapsed.
    ///
    /// The sample below combines this with the geofence API to show a local notification
    /// when entering a radius with the app in the background:
    ///
    /// ```java
    /// // File: GeofenceListenerImpl.java
    /// public class GeofenceListenerImpl implements GeofenceListener {
    /// @Override
    ///     public void onExit(String id) {
    ///     }
    /// @Override
    ///     public void onEntered(String id) {
    ///         if(!Display.getInstance().isMinimized()) {
    ///             Display.getInstance().callSerially(() -> {
    ///                 Dialog.show("Welcome", "Thanks for arriving", "OK", null);
    ///             });
    ///         } else {
    ///             LocalNotification ln = new LocalNotification();
    ///             ln.setId("LnMessage");
    ///             ln.setAlertTitle("Welcome");
    ///             ln.setAlertBody("Thanks for arriving!");
    ///             Display.getInstance().scheduleLocalNotification(ln, System.currentTimeMillis() + 10, LocalNotification.REPEAT_NONE);
    ///         }
    ///     }
    /// }
    /// ```
    ///
    /// ```java
    /// // File: GeofenceSample.java
    /// Geofence gf = new Geofence("test", loc, 100, 100000);
    /// LocationManager.getLocationManager().addGeoFencing(GeofenceListenerImpl.class, gf);
    /// ```
    ///
    /// #### Parameters
    ///
    /// - `n`: The notification to schedule.
    ///
    /// - `firstTime`: time in milliseconds when to schedule the notification
    ///
    /// - `repeat`: @param repeat    repeat one of the following: REPEAT_NONE, REPEAT_FIFTEEN_MINUTES,
    ///                  REPEAT_HALF_HOUR, REPEAT_HOUR, REPEAT_DAY, REPEAT_WEEK
    public void scheduleLocalNotification(LocalNotification n, long firstTime, int repeat) {
        if (n.getId() == null || n.getId().length() == 0) {
            throw new IllegalArgumentException("Notification ID must be set");
        }
        if (firstTime < System.currentTimeMillis()) {
            throw new IllegalArgumentException("Cannot schedule a notification to a past time");
        }
        if (n.getAlertSound() != null && n.getAlertSound().length() > 0 && !n.getAlertSound().startsWith("/notification_sound")) {
            throw new IllegalArgumentException("Alert sound file name must start with the 'notification_sound' prefix");
        }
        impl.scheduleLocalNotification(n, firstTime, repeat);
    }

    /// Cancels a local notification by ID.
    ///
    /// #### Parameters
    ///
    /// - `notificationId`
    ///
    /// #### See also
    ///
    /// - com.codename1.notifications.LocalNotification
    public void cancelLocalNotification(String notificationId) {
        impl.cancelLocalNotification(notificationId);
    }

    /// Sets the preferred time interval between background fetches.  This is only a
    /// preferred interval and is not guaranteed.  Some platforms, like iOS, maintain sovereign
    /// control over when and if background fetches will be allowed. This number is used
    /// only as a guideline.
    ///
    /// **This method must be called in order to activate background fetch.**>
    ///
    /// Note: If the platform doesn't support background fetch (i.e. `#isBackgroundFetchSupported()` returns `false`,
    /// then this method does nothing.
    ///
    /// #### Parameters
    ///
    /// - `seconds`: The time interval in seconds.
    ///
    /// #### See also
    ///
    /// - #isBackgroundFetchSupported()
    ///
    /// - #getPreferredBackgroundFetchInterval(int) ()
    ///
    /// - com.codename1.background.BackgroundFetch
    public void setPreferredBackgroundFetchInterval(int seconds) {
        impl.setPreferredBackgroundFetchInterval(seconds);
    }

    /// Gets the preferred time (in seconds) between background fetches.
    ///
    /// #### Returns
    ///
    /// The time interval in seconds.
    ///
    /// #### See also
    ///
    /// - #isBackgroundFetchSupported()
    ///
    /// - #setPreferredBackgroundFetchInterval(int)
    ///
    /// - com.codename1.background.BackgroundFetch
    public int getPreferredBackgroundFetchInterval(int seconds) {
        return impl.getPreferredBackgroundFetchInterval();
    }

    /// Checks to see if the current platform supports background fetch.
    ///
    /// #### Returns
    ///
    /// True if the current platform supports background fetch.
    ///
    /// #### See also
    ///
    /// - #setPreferredBackgroundFetchInterval(int)
    ///
    /// - #getPreferredBackgroundFetchInterval(int) ()
    ///
    /// - com.codename1.background.BackgroundFetch
    public boolean isBackgroundFetchSupported() {
        return impl.isBackgroundFetchSupported();
    }

    /// Allows detecting development mode so debugging code and special cases can be used to simplify flow
    ///
    /// #### Returns
    ///
    /// true if we are running in the simulator, false otherwise
    public boolean isSimulator() {
        return impl.isSimulator();
    }

    /// Creates an audio media that can be played in the background.
    ///
    /// #### Parameters
    ///
    /// - `uri`: @param uri the uri of the media can start with jar://, file://, http://
    ///            (can also use rtsp:// if supported on the platform)
    ///
    /// #### Returns
    ///
    /// @return Media a Media Object that can be used to control the playback
    /// of the media or null if background playing is not supported on the platform
    ///
    /// #### Throws
    ///
    /// - `IOException`: if creation of media from the given URI has failed
    public Media createBackgroundMedia(String uri) throws IOException {
        return impl.createBackgroundMedia(uri);
    }

    /// Creates an audio media that can be played in the background.  This call is
    /// asynchronous, so that it will return perhaps before the media object is ready.
    ///
    /// #### Parameters
    ///
    /// - `uri`: @param uri the uri of the media can start with jar://, file://, http://
    ///            (can also use rtsp:// if supported on the platform)
    ///
    /// #### Returns
    ///
    /// @return Media a Media Object that can be used to control the playback
    /// of the media or null if background playing is not supported on the platform
    public AsyncResource<Media> createBackgroundMediaAsync(String uri) {
        return impl.createBackgroundMediaAsync(uri);
    }

    /// Create a blur image from the given image.
    /// The algorithm is gaussian blur - https://en.wikipedia.org/wiki/Gaussian_blur
    ///
    /// #### Parameters
    ///
    /// - `image`: the image to blur
    ///
    /// - `radius`: the radius to be used in the algorithm
    public Image gaussianBlurImage(Image image, float radius) {
        return impl.gaussianBlurImage(image, radius);
    }

    /// Returns true if gaussian blur is supported on this platform
    ///
    /// #### Returns
    ///
    /// true if gaussian blur is supported.
    public boolean isGaussianBlurSupported() {
        return impl.isGaussianBlurSupported();
    }

    /// Refreshes the native list of contacts on devices that require this see `com.codename1.contacts.ContactsManager#refresh()`
    public void refreshContacts() {
        impl.refreshContacts();
    }

    /// Returns true if this device is jailbroken or rooted, false if not or unknown. Notice that this method isn't
    /// accurate and can't detect all jailbreak/rooting cases
    ///
    /// #### Returns
    ///
    /// true if this device is jailbroken or rooted, false if not or unknown.
    public boolean isJailbrokenDevice() {
        return impl.isJailbrokenDevice();
    }

    /// Returns the build hints for the simulator, this will only work in the debug environment and it's
    /// designed to allow extensions/API's to verify user settings/build hints exist
    ///
    /// #### Returns
    ///
    /// map of the build hints that isn't modified without the codename1.arg. prefix
    public Map<String, String> getProjectBuildHints() {
        return impl.getProjectBuildHints();
    }

    /// Sets a build hint into the settings while overwriting any previous value. This will only work in the
    /// debug environment and it's designed to allow extensions/API's to verify user settings/build hints exist.
    /// Important: this will throw an exception outside of the simulator!
    ///
    /// #### Parameters
    ///
    /// - `key`: the build hint without the codename1.arg. prefix
    ///
    /// - `value`: the value for the hint
    public void setProjectBuildHint(String key, String value) {
        impl.setProjectBuildHint(key, value);
    }

    /// Checks to see if you can prompt the user to install the app on their homescreen.
    /// This is only relevant for the Javascript port with PWAs.  This is not a "static" property, as it
    /// only returns true if the app is in a state that allows you to prompt the user.  E.g. if you have
    /// previously prompted the user and they have declined, then this will return false.
    ///
    /// Best practice is to use `#onCanInstallOnHomescreen(java.lang.Runnable)` to be notified
    /// when you are allowed to prompt the user for installation.  Then call `#promptInstallOnHomescreen()`
    /// inside that method - or sometime after.
    ///
    /// Example
    ///
    /// ```java
    /// `onCanInstallOnHomescreen(()->{
    ///      if (canInstallOnHomescreen()) {
    ///           if (promptInstallOnHomescreen()) {
    ///               // User accepted installation` else {
    ///               // user rejected installation
    ///           }
    ///      }
    /// });
    /// }
    /// ```
    ///
    /// https://developers.google.com/web/fundamentals/app-install-banners/
    ///
    /// #### Returns
    ///
    /// True if you are able to prompt the user to install the app on their homescreen.
    ///
    /// #### See also
    ///
    /// - #promptInstallOnHomescreen()
    ///
    /// - #onCanInstallOnHomescreen(java.lang.Runnable)
    public boolean canInstallOnHomescreen() {
        return impl.canInstallOnHomescreen();
    }

    /// Prompts the user to install this app on their homescreen.  This is only relevant in the
    /// javascript port.
    ///
    /// #### Returns
    ///
    /// @return The result of the user prompt.  true if the user accepts the installation,
    /// false if they reject it.
    ///
    /// #### See also
    ///
    /// - #canInstallOnHomescreen()
    ///
    /// - #onCanInstallOnHomescreen(java.lang.Runnable)
    public boolean promptInstallOnHomescreen() {
        return impl.promptInstallOnHomescreen();
    }

    /// A callback fired when you are allowed to prompt the user to install the app on their homescreen.
    /// Only relevant in the javascript port.
    ///
    /// #### Parameters
    ///
    /// - `r`: @param r Runnable that will be run when/if you are permitted to prompt the user to install
    ///          the app on their homescreen.
    public void onCanInstallOnHomescreen(Runnable r) {
        impl.onCanInstallOnHomescreen(r);
    }

    /// Captures a screenshot of the screen.
    ///
    /// #### Returns
    ///
    /// An image of the screen, or null if it failed.
    ///
    /// #### Since
    ///
    /// 7.0
    ///
    /// #### Deprecated
    ///
    /// use screenshot(SuccessCallback) instead
    public Image captureScreen() {
        return impl.captureScreen();
    }

    /// Captures a screenshot in the native layer which should include peer
    /// components as well.
    ///
    /// #### Parameters
    ///
    /// - `callback`: will be invoked on the EDT with a screenshot
    ///
    /// #### Since
    ///
    /// 7.0.211
    public void screenshot(SuccessCallback<Image> callback) {
        impl.screenshot(callback);
    }

    /// Notifies the platform that push notification processing is complete.
    /// This is useful on iOS where the app is woken up in the background to handle
    /// a push notification and needs to signal completion to avoid being suspended
    /// prematurely.
    ///
    /// If the `ios.delayPushCompletion` build hint (or property) is set to "true",
    /// Codename One will NOT automatically signal completion after the `com.codename1.push.PushCallback#push(String)`
    /// method returns. Instead, the application MUST invoke this method manually
    /// when it has finished its background work (e.g. playing audio, downloading content).
    public void notifyPushCompletion() {
        impl.notifyPushCompletion();
    }

    /// Convenience method to schedule a task to run on the EDT after timeoutms.
    ///
    /// #### Parameters
    ///
    /// - `timeout`: The timeout in milliseconds.
    ///
    /// - `r`: The task to run.
    ///
    /// #### Returns
    ///
    /// The Timer object that can be used to cancel the task.
    ///
    /// #### Since
    ///
    /// 7.0
    ///
    /// #### See also
    ///
    /// - #setInterval(int, java.lang.Runnable)
    public Timer setTimeout(int timeout, @Async.Schedule final Runnable r) {

        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                executeTimeoutRunnable(r);
            }
        }, timeout);
        return t;
    }

    private void executeTimeoutRunnable(@Async.Execute Runnable r) {
        CN.callSerially(r);
    }

    /// Convenience method to schedule a task to run on the EDT after periodms
    /// repeating every periodms.
    ///
    /// #### Parameters
    ///
    /// - `period`: The delay and repeat in milliseconds.
    ///
    /// - `r`: The runnable to run on the EDT.
    ///
    /// #### Returns
    ///
    /// The timer object which can be used to cancel the task.
    ///
    /// #### Since
    ///
    /// 7.0
    ///
    /// #### See also
    ///
    /// - #setTimeout(int, java.lang.Runnable)
    public Timer setInterval(int period, @Async.Schedule final Runnable r) {
        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                executeTimeoutRunnable(r);
            }
        }, period, period);


        return t;
    }

    /// Gets a reference to an application-wide shared Javascript context that can be used for running
    /// Javascript commands.  When running in the Javascript port, this Javascript context will be the
    /// same context in which the application itself is running, so it gives you the ability to interact
    /// with the browser and DOM directly using the familiar `BrowserComponent` API.
    ///
    /// When running on other platforms, this shared context will be an off-screen browser component.
    ///
    /// Sample code allowing user to execute arbitrary Javascript code inside the shared context:
    ///
    /// ```java
    /// Form hi = new Form("Hi World", new BorderLayout());
    /// TextArea input = new TextArea();
    /// TextArea output = new TextArea();
    /// output.setEditable(false);
    ///
    /// Button execute = new Button("Run");
    /// execute.addActionListener(evt->{
    ///     BrowserComponent bc = CN.getSharedJavascriptContext().ready().get();
    ///     bc.execute("callback.onSuccess(window.eval(${0}))", new Object[]{input.getText()}, res->{
    ///         output.setText(res.toString());
    ///     });
    /// });
    /// SplitPane split = new SplitPane(SplitPane.VERTICAL_SPLIT, input, output, "0", "50%", "99%");
    /// hi.add(CENTER, split);
    /// hi.add(NORTH, execute);
    ///
    /// hi.show();
    /// ```
    ///
    /// #### Returns
    ///
    /// A shared BrowserComponent
    ///
    /// #### Since
    ///
    /// 7.0
    public BrowserComponent getSharedJavascriptContext() {
        return impl.getSharedJavscriptContext();
    }

    private static class EdtException extends RuntimeException {
        private Throwable cause;
        private EdtException parent;

        @Override
        public Throwable getCause() {
            return cause;
        }

        public void setCause(Throwable t) {
            this.cause = t;
        }

        private void throwRoot(Throwable cause) {
            HashSet<Throwable> circuitCheck = new HashSet<Throwable>();
            circuitCheck.add(cause);
            EdtException root = this;
            if (root != cause) { //NOPMD CompareObjectsWithEquals
                root.setCause(cause);
                circuitCheck.add(root);
            } else {
                root = (EdtException) cause;
            }
            while (root.parent != null) {
                if (circuitCheck.contains(root.parent)) {
                    break;
                }
                root.parent.setCause(root);
                circuitCheck.add(root.parent);
                root = root.parent;
            }
            throw root;
        }

    }

    /// A wrapper around Runnable that records the stack trace so that
    /// if an exception occurs, it is easier to track it back to the original
    /// source.
    private static class DebugRunnable implements Runnable {
        private final Runnable internal;
        private final EdtException exceptionWrapper;
        private DebugRunnable parentContext;
        private int depth;
        private int totalDepth;

        DebugRunnable(Runnable internal) {
            this.internal = internal;
            this.parentContext = INSTANCE.currentEdtContext;
            if (parentContext != null) {
                depth = parentContext.depth + 1;
                totalDepth = parentContext.totalDepth + 1;
            }

            if (INSTANCE.isEnableAsyncStackTraces()) {
                exceptionWrapper = new EdtException();

                if (parentContext != null) {
                    if (depth < MAX_ASYNC_EXCEPTION_DEPTH) {
                        exceptionWrapper.parent = parentContext.exceptionWrapper;
                        parentContext = null;
                    } else {
                        depth = 0;
                    }
                }
            } else {
                exceptionWrapper = null;
                parentContext = null;
            }
        }


        @Override
        public void run() {
            if (exceptionWrapper != null) {
                try {
                    INSTANCE.currentEdtContext = this;
                    internal.run();
                } catch (RuntimeException t) {
                    exceptionWrapper.throwRoot(t);
                }
            } else {
                internal.run();
            }
        }

    }

}
