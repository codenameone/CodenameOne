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

import com.codename1.analytics.Analytics;
import com.codename1.annotations.Async;
import com.codename1.capture.VideoCaptureConstraints;
import com.codename1.codescan.CodeScanner;
import com.codename1.contacts.Contact;
import com.codename1.db.Database;
import com.codename1.db.DatabaseConfig;
import com.codename1.impl.CodenameOneImplementation;
import com.codename1.impl.CodenameOneThread;
import com.codename1.impl.ImplementationFactory;
import com.codename1.impl.VirtualKeyboardInterface;
import com.codename1.impl.WindowManager;
import com.codename1.io.Log;
import com.codename1.io.Preferences;
import com.codename1.io.Util;
import com.codename1.l10n.L10NManager;
import com.codename1.location.LocationManager;
import com.codename1.printing.PrintResult;
import com.codename1.printing.PrintResultListener;
import com.codename1.security.Biometrics;
import com.codename1.security.SecureStorage;
import com.codename1.share.ShareResult;
import com.codename1.share.ShareResultListener;
import com.codename1.media.Media;
import com.codename1.media.MediaRecorderBuilder;
import com.codename1.media.VideoIO;
import com.codename1.messaging.Message;
import com.codename1.notifications.LocalNotification;
import com.codename1.notifications.NotificationChannelBuilder;
import com.codename1.notifications.NotificationPermissionCallback;
import com.codename1.notifications.NotificationPermissionRequest;
import com.codename1.background.ForegroundService;
import com.codename1.background.WorkRequest;
import com.codename1.payment.Purchase;
import com.codename1.plugin.PluginSupport;
import com.codename1.plugin.event.IsGalleryTypeSupportedEvent;
import com.codename1.plugin.event.OpenGalleryEvent;
import com.codename1.system.CrashReport;
import com.codename1.ui.accessibility.AccessibilityManager;
import com.codename1.ui.animations.Animation;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.MessageEvent;
import com.codename1.ui.events.PointerEvent;
import com.codename1.ui.events.WindowEvent;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.EventDispatcher;
import com.codename1.ui.util.ImageIO;
import com.codename1.util.AsyncResource;
import com.codename1.util.Simd;
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
    private Simd simd;
    private CrashReport crashReporter;
    private EventDispatcher errorHandler;
    private boolean inNativeUI;
    private Runnable bookmark;
    private EventDispatcher messageListeners;
    private EventDispatcher windowListeners;
    private EventDispatcher postureListeners;
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
    /// Pointer metadata slot per queued packet, indexed by the packet's type-word
    /// offset in `inputEventStack`. Kept beside the stack rather than inside the packet
    /// so the packet layout -- and every offset computation and `skipEvent` count that
    /// depends on it -- is unchanged. A slot is only written when a pointer packet is
    /// queued and only read for a pointer type at that same offset, so it is always the
    /// slot belonging to the packet being dispatched.
    private int[] pointerMetaStack = new int[1000];
    private int[] pointerMetaStackTmp = new int[1000];
    private int inputEventStackPointer;
    private int[] inputEventStackTmp = new int[1000];
    private int inputEventStackPointerTmp;
    private boolean pointerPressedAndNotReleasedOrDragged;
    private boolean recursivePointerReleaseA;
    private boolean recursivePointerReleaseB;
    private int pointerX;
    private int pointerY;
    private PointerEvent currentPointerEvent;
    private int longPressInterval = 500;
    private boolean lastInteractionWasKeypad;
    private boolean processingSerialCalls;
    private int PATHLENGTH;
    /// Drag sample history, one ring per window.
    ///
    /// Singleton before: two touchscreen contacts dragging in two windows appended
    /// to the same ring, so releasing either scrolling component computed its
    /// inertia from the other window's samples -- wrong or wildly extreme fling.
    /// Index 0 is the main surface; `#dragHistoryWindows` maps the rest.
    private float[][] dragPathX;
    private float[][] dragPathY;
    private long[][] dragPathTime;
    private int[] dragPathOffset;
    private int[] dragPathLength;
    private int[] dragHistoryWindows;

    /// The window whose drag samples `#getDragSpeed(boolean)` should report. Set
    /// while that window's pointer events are dispatched, since the public accessor
    /// takes no window and is called by components during their own release.
    private int dragHistoryCurrent;
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
    /// The window the coalescable drag packet at lastDragOffset belongs to. Coalescing
    /// across windows would overwrite one window's coordinates with another's while
    /// the packet still carries the first window's id.
    private int lastDragWindowId;
    private boolean lockOrientation;
    private boolean disableScreenshots;

    /// Tapjacking settings supplied through setProperty before an implementation existed, applied
    /// in init(). See the deferral in init() and the setProperty branches below.
    private com.codename1.security.TapjackingPolicy pendingTapjackingPolicy;
    private boolean pendingHideOverlayWindows;

    // huge false positive from PMD...
    /// Ids of the windows with a pointer press in flight, paired with
    /// `#pointerPressTargets` by index.
    private final int[] pointerPressWindows = new int[TRACKED_KEY_PRESSES];

    /// The top level that received each in-flight pointer press.
    ///
    /// One entry per window rather than one for the pointer, for the same reason the
    /// key targets are per key: two touchscreen contacts can be down in two windows
    /// at once -- the Linux handlers deliberately track a sequence per window -- and
    /// a single field made the second press erase the first, so both releases were
    /// dropped and both components stayed latched down.
    private final Container[] pointerPressTargets = new Container[TRACKED_KEY_PRESSES];

    /// Guards `#monitorsChangedPending`, which is set from the port's native event
    /// thread and cleared on the event dispatch thread.
    private final Object monitorsChangedLock = new Object();

    /// Whether a monitor-topology notification is already queued. See
    /// `#monitorsChanged()`.
    private boolean monitorsChangedPending;

    /// How many entries the per-key and per-window input tables hold.
    ///
    /// CN1_MAX_DESKTOP_WINDOWS in the native ports, *plus one* for the application's
    /// main surface, which holds a permanent entry of its own. Sizing this to 32
    /// alone left only 31 usable secondary slots, so the last window the ports allow
    /// still lost its drag state.
    ///
    /// It was originally 8, a size chosen for simultaneous key presses that I reused
    /// for the window-keyed tables without asking what bounds those. Exhaustion is
    /// silent -- the lookup returns -1 and the setter no-ops -- so the window simply
    /// behaves as though the drag never happened.
    private static final int TRACKED_KEY_PRESSES = 33;

    /// Key codes currently held, paired with `#keyPressTargets` by index.
    private final int[] keyPressCodes = new int[TRACKED_KEY_PRESSES];

    /// The top level that received the press of each held key.
    ///
    /// One field per key rather than one for the keyboard: hold a key in window A,
    /// focus window B and press another key there, and a single field names B, so
    /// A's release matches nothing and is dropped while clearing the field -- which
    /// then drops B's release too, latching a component in each window.
    private final Container[] keyPressTargets = new Container[TRACKED_KEY_PRESSES];

    /// Window ids remembered so a key repeat or long press started in a window is
    /// delivered back to that window rather than to the main form.

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
            INSTANCE.simd = null;

            impl.setDisplayLock(lock);
            impl.initImpl(m);
            INSTANCE.codenameOneGraphics = new Graphics(impl.getNativeGraphics());
            INSTANCE.codenameOneGraphics.paintPeersBehind = impl.paintNativePeersBehind();
            impl.setCodenameOneGraphics(INSTANCE.codenameOneGraphics);

            if (INSTANCE.disableScreenshots) {
                impl.setDisableScreenshots(true);
                INSTANCE.disableScreenshots = false;
            }

            // Same deferral as disableScreenshots above: the android.tapjackingGuard build hint
            // sets these properties from the activity stub, which can run before there is an impl
            // to hand them to.
            if (INSTANCE.pendingTapjackingPolicy != null) {
                impl.setTapjackingProtection(INSTANCE.pendingTapjackingPolicy);
                INSTANCE.pendingTapjackingPolicy = null;
            }
            if (INSTANCE.pendingHideOverlayWindows) {
                impl.setHideOverlayWindows(true);
                INSTANCE.pendingHideOverlayWindows = false;
            }

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
            INSTANCE.dragPathX = new float[TRACKED_KEY_PRESSES][];
            INSTANCE.dragPathY = new float[TRACKED_KEY_PRESSES][];
            INSTANCE.dragPathTime = new long[TRACKED_KEY_PRESSES][];
            INSTANCE.dragPathOffset = new int[TRACKED_KEY_PRESSES];
            INSTANCE.dragPathLength = new int[TRACKED_KEY_PRESSES];
            INSTANCE.dragHistoryWindows = new int[TRACKED_KEY_PRESSES];
            // Slot 0 is the main surface and is always present; the rings for the
            // other slots are allocated the first time a window drags.
            INSTANCE.dragHistoryWindows[0] = MAIN_LONG_PRESS_ID;
            INSTANCE.dragPathX[0] = new float[INSTANCE.PATHLENGTH];
            INSTANCE.dragPathY[0] = new float[INSTANCE.PATHLENGTH];
            INSTANCE.dragPathTime[0] = new long[INSTANCE.PATHLENGTH];
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
    public void setBookmark(Runnable bookmark) {
        this.bookmark = bookmark;
    }

    /// Runs the last bookmark that was set using `#setBookmark(java.lang.Runnable)`
    ///
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

    /// Returns the platform's WiFi implementation. Used by
    /// `com.codename1.io.wifi.WiFi`; applications normally talk to that
    /// static facade rather than calling this directly.
    public com.codename1.io.wifi.WifiPlatform getWifiPlatform() {
        return impl.getWifiPlatform();
    }

    /// Returns the platform's WiFi-Direct implementation.
    public com.codename1.io.wifi.WifiDirectPlatform getWifiDirectPlatform() {
        return impl.getWifiDirectPlatform();
    }

    /// Returns the platform's Bonjour / mDNS implementation.
    public com.codename1.io.bonjour.BonjourPlatform getBonjourPlatform() {
        return impl.getBonjourPlatform();
    }

    /// Returns the platform's USB host implementation.
    public com.codename1.io.usb.UsbPlatform getUsbPlatform() {
        return impl.getUsbPlatform();
    }

    /// Returns the platform's network-type tracker used by
    /// `NetworkManager.addNetworkTypeListener(...)`.
    public com.codename1.io.NetworkTypePlatform getNetworkTypePlatform() {
        return impl.getNetworkTypePlatform();
    }

    /// Returns the SIMD API instance bound to the current implementation.
    public Simd getSimd() {
        if (simd == null) {
            if (impl == null) {
                // Runtime not yet initialized (e.g. plain unit tests): hand out
                // the portable scalar fallback without caching it, so the real
                // implementation's SIMD is still installed once init() runs.
                return new Simd();
            }
            Simd created = impl.createSimd();
            if (created == null) {
                created = new Simd();
            }
            simd = created;
        }
        return simd;
    }

    /// Returns true if the current platform provides a hardware accelerated 3D
    /// GPU backend for `com.codename1.gpu.RenderView`.
    public boolean isGpuSupported() {
        return impl.getGpuImplementation() != null;
    }

    /// Creates the native GPU peer backing a `RenderView`. Intended for use by
    /// `RenderView`; returns null on platforms without a 3D backend.
    public PeerComponent createGpuPeer(com.codename1.gpu.RenderView view) {
        com.codename1.impl.gpu.GpuImplementation gpu = impl.getGpuImplementation();
        return gpu != null ? gpu.createPeer(view) : null;
    }

    /// Sets whether a GPU peer renders continuously or only on demand. Intended
    /// for use by `RenderView`.
    public void gpuSetContinuous(PeerComponent peer, boolean continuous) {
        com.codename1.impl.gpu.GpuImplementation gpu = impl.getGpuImplementation();
        if (gpu != null) {
            gpu.setContinuous(peer, continuous);
        }
    }

    /// Requests a single frame from a GPU peer. Intended for use by `RenderView`.
    public void gpuRequestRender(PeerComponent peer) {
        com.codename1.impl.gpu.GpuImplementation gpu = impl.getGpuImplementation();
        if (gpu != null) {
            gpu.requestRender(peer);
        }
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

    /// Notifies the native port that the portable semantic tree changed. This is
    /// primarily an internal bridge used by the accessibility subsystem.
    ///
    /// #### Parameters
    ///
    /// - `changeType`: bit mask of `AccessibilityManager.CHANGE_*` constants
    public void accessibilityTreeChanged(int changeType) {
        impl.accessibilityTreeChanged(changeType);
    }

    /// Returns true when the active port exposes lightweight components through
    /// a native virtual accessibility tree.
    public boolean isAccessibilityTreeSupported() {
        return impl.isAccessibilityTreeSupported();
    }

    /// Returns true when the active port currently needs semantic changes to be
    /// projected eagerly. This is an internal bridge used to avoid rebuilding
    /// an unused accessibility tree from hot component setters.
    public boolean isAccessibilityTreeUpdateRequired() {
        return impl.isAccessibilityTreeUpdateRequired();
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
    /// SHOW_DURING_EDIT_EXCEPTION, SHOW_DURING_EDIT_ALLOW_DISCARD,
    /// SHOW_DURING_EDIT_ALLOW_SAVE, SHOW_DURING_EDIT_SET_AS_NEXT
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
    public void stopRemoteControl() {
        impl.stopRemoteControl();
    }

    /// Starts the remote control service.  This should be implemented
    /// in the platform to handle binding the `RemoteControlListener` with
    /// the platform's remote control.
    ///
    /// This is executed when the user registers a new listener using `MediaManager#setRemoteControlListener(com.codename1.media.RemoteControlListener)`
    ///
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
    public float getLargerTextScale() {
        return impl.getLargerTextScale();
    }

    /// Returns true when the user requests stronger foreground/background contrast.
    public boolean isHighContrastEnabled() {
        return impl.isHighContrastEnabled();
    }

    /// Returns true when the user requests that information isn't conveyed by color alone.
    public boolean isDifferentiateWithoutColorEnabled() {
        return impl.isDifferentiateWithoutColorEnabled();
    }

    /// Returns the selected color-vision correction, or {@link AccessibilityColorVisionDeficiency#UNKNOWN}
    /// when the platform doesn't expose it.
    public AccessibilityColorVisionDeficiency getColorVisionDeficiency() {
        return impl.getColorVisionDeficiency();
    }

    /// Returns true when the user requests reduced or disabled nonessential motion.
    public boolean isReduceMotionEnabled() {
        return impl.isReduceMotionEnabled();
    }

    /// Returns true when the user requests reduced transparency and blur effects.
    public boolean isReduceTransparencyEnabled() {
        return impl.isReduceTransparencyEnabled();
    }

    /// Returns true when the user requests heavier text weight.
    public boolean isBoldTextEnabled() {
        return impl.isBoldTextEnabled();
    }

    /// Returns true when the operating system is inverting displayed colors.
    public boolean isInvertColorsEnabled() {
        return impl.isInvertColorsEnabled();
    }

    /// Returns true when the operating system requests a grayscale presentation.
    public boolean isGrayscaleEnabled() {
        return impl.isGrayscaleEnabled();
    }

    /// Returns true when switches should include visible on/off labels.
    public boolean isOnOffSwitchLabelsEnabled() {
        return impl.isOnOffSwitchLabelsEnabled();
    }

    /// Returns true when a screen reader or touch-exploration service is active.
    public boolean isScreenReaderEnabled() {
        return impl.isScreenReaderEnabled();
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
    /// the paint and key handling events
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
    /// the paint and key handling events
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
    /// the paint and key handling events
    ///
    /// #### Throws
    ///
    /// - `IllegalStateException`: @throws IllegalStateException if this method is invoked on the event dispatch thread (e.g. during
    /// paint or event handling).
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
    /// the paint and key handling events
    ///
    /// - `timeout`: timeout duration, on timeout the method just returns
    ///
    /// #### Throws
    ///
    /// - `IllegalStateException`: @throws IllegalStateException if this method is invoked on the event dispatch thread (e.g. during
    /// paint or event handling).
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
            // A window shown before the first Form.show() must not be starved: this
            // phase never calls edtLoopImpl(), so it neither drains input nor paints.
            while (impl.getCurrentForm() == null
                    && !Desktop.getInstance().hasVisibleWindows()) { // PMD Fix: AvoidBranchingStatementAsLastInLoop
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
                    // Hand the actual throwable to the registered reporter
                    // BEFORE impl.handleEDTException gets a chance to short
                    // circuit (legacy AndroidImplementation returns true
                    // after showing its own AlertDialog, which used to
                    // silently lose the exception for anyone hooking via
                    // setCrashReporter -- including CrashProtection).
                    crashReporter.exception(err);
                }
                CodenameOneThread.handleException(err);
                if (!impl.handleEDTException(err)) {
                    if (errorHandler != null) {
                        errorHandler.fireActionEvent(new ActionEvent(err, ActionEvent.Type.Exception));
                    } else {
                        Dialog.show("Error", "An internal application error occurred: " + err, "OK", null);
                    }
                }
            }
        }
        // Dispose any window still open, on the EDT, before the implementation goes
        // away. Doing this from the static deinitialize() would run the teardown off
        // the EDT, which is exactly the thread the window's tree expects.
        Desktop.getInstance().disposeAll();
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
            // The metadata slots are addressed by offset into the stack being
            // dispatched, so they have to change hands with it; leaving them behind
            // would have every packet read the slot of whatever packet last occupied
            // that offset in the other buffer.
            int[] qtMeta = pointerMetaStackTmp;
            pointerMetaStackTmp = pointerMetaStack;

            // We have a special flag here for a case where the input event stack might still be processing this can
            // happen if an event callback calls something like invokeAndBlock while processing and might reach
            // this code again
            if (qt[qt.length - 1] == Integer.MAX_VALUE) {
                inputEventStack = new int[qt.length];
                pointerMetaStack = new int[qt.length];
            } else {
                inputEventStack = qt;
                pointerMetaStack = qtMeta;
                qt[qt.length - 1] = 0;
            }
        }

        // we copy the variables to the stack since the array might be replaced while we are working if the EDT
        // is nested into an "invokeAndBlock"
        int actualTmpPointer = inputEventStackPointerTmp;
        inputEventStackPointerTmp = 0;
        int[] actualStack = inputEventStackTmp;
        // Copied to the stack for the same reason as the event stack itself: a nested
        // invokeAndBlock can swap the field while this loop is still dispatching.
        int[] actualMeta = pointerMetaStackTmp;
        int offset = 0;
        actualStack[actualStack.length - 1] = Integer.MAX_VALUE;
        while (offset < actualTmpPointer) {
            offset = handleEvent(offset, actualStack, actualMeta);
        }
        // The restored metadata is only authoritative while this batch is being
        // dispatched. Leaving it selected made every later read answer from the last
        // packet dispatched, so a port that staged fresh metadata and then asked --
        // without going through the queue -- got the previous event's button back.
        impl.clearPointerEventMetadataSelection();

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
        }

        // Additional native windows, painted after the main surface so its call
        // ordering is untouched. One field read per frame when none are open.
        if (Desktop.getInstance().hasOpenWindows()) {
            paintOpenWindows();
        }

        // Key repeat and long press are routed back to whichever top level the
        // originating press came from, not blindly to the current form.
        long t = System.currentTimeMillis();
        for (int iter = 0; iter < TRACKED_KEY_PRESSES; iter++) {
            if (keyRepeatWindows[iter] == 0) {
                continue;
            }
            int repeatWindow = keyRepeatWindows[iter] == MAIN_LONG_PRESS_ID
                    ? 0 : keyRepeatWindows[iter];
            Container repeatTarget = repeatTarget(repeatWindow, current);
            if (repeatTarget == null) {
                continue;
            }
            if (keyRepeatArmed[iter] && keyRepeatNext[iter] <= t) {
                repeatTarget.keyRepeated(keyRepeatValues[iter]);
                int keyRepeatNextIntervalTime = 10;
                keyRepeatNext[iter] = t + keyRepeatNextIntervalTime;
            }
            if (keyLongPressArmed[iter] && longPressInterval <= t - keyLongPressStart[iter]) {
                keyLongPressArmed[iter] = false;
                repeatTarget.longKeyPress(keyRepeatValues[iter]);
            }
        }
        for (int iter = 0; iter < TRACKED_KEY_PRESSES; iter++) {
            if (!longPressArmed[iter] || longPressInterval > t - longPressStart[iter]) {
                continue;
            }
            int pressWindow = longPressWindows[iter] == MAIN_LONG_PRESS_ID
                    ? 0 : longPressWindows[iter];
            Container pointerTarget = repeatTarget(pressWindow, current);
            longPressArmed[iter] = false;
            longPressWindows[iter] = 0;
            if (pointerTarget != null) {
                pointerTarget.longPointerPress(longPressPointerX[iter], longPressPointerY[iter]);
            }
        }
        processSerialCalls();

        time = System.currentTimeMillis() - currentTime;
    }

    /// Resolves the top level a repeat or long press should be delivered to, or null
    /// when it must not receive one.
    ///
    /// These timers are armed when the press is accepted and fire from the paint loop,
    /// which calls keyRepeated, longKeyPress and longPointerPress directly rather than
    /// through the packed queue -- so they never meet the modality filter. A handler
    /// that opens a modal window from the very press still being held would otherwise
    /// go on receiving repeats and a long press behind that modal, which is the one
    /// case where the press was legitimately accepted and the block arrived afterwards.
    /// The same reasoning covers a window hidden while its press is held.
    private Container repeatTarget(int windowId, Form current) {
        if (isBlockedByModal(windowId)) {
            return null;
        }
        if (windowId == 0) {
            return current;
        }
        Window w = Desktop.getInstance().windowById(windowId);
        return w != null && w.isWindowShowing() ? w : null;
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
    /// anywhere in the Runnable.
    ///
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
    /// anywhere in the Runnable.
    ///
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
    /// while this runnable is running
    ///
    /// #### Throws
    ///
    /// - `BlockingDisallowedException`: @throws BlockingDisallowedException if this method is called while blocking is disabled (i.e. we are running
    /// inside a call to `#invokeWithoutBlocking(java.lang.Runnable)` on the EDT).
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
        if (!isEdt()) {
            // when not running callSerially executes synchronously and would recurse here forever (#4811)
            if (!codenameOneRunning) {
                throw new IllegalStateException("Display.setCurrent must be invoked after Codename One has started running. Call it from start() or via callSerially.");
            }
            // The direction is not recorded here: this call comes back round on the EDT and
            // records it there. Doing it in both places would leave one entry behind for a
            // later navigation to the same form to take.
            callSerially(new RunnableWrapper(newForm, null, reverse));
            return;
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
                    newForm.setShownWithReverse(reverse);
                    impl.setCurrentForm(newForm);
                    return;
                default:
                    break;
            }
        }

        // Recorded once the call is known to be going through: on the EDT, with the form
        // changing, and past the cases that decline to show anything while text is being
        // edited. Recording it any earlier would queue a direction that no arrival takes, and a
        // later navigation to the same form would take that stale one instead of its own.
        newForm.setShownWithReverse(reverse);

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
                if (current != null) {
                    // Coming out of a menu back to the form underneath it, which is a backward
                    // move: without saying so, this arrival takes whatever direction that form
                    // has left over and a port that keeps browser history in step reads it as a
                    // step forward, pushing an entry for a form that was already behind.
                    //
                    // At the head of the queue, because this arrival comes first: when the form
                    // being revealed is also the one being shown, its own direction is already
                    // waiting, and appending would have this restoration take that one and leave
                    // this one for the show.
                    current.insertShownWithReverse(true);
                }
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
        cancelAllKeyRepeats();
        cancelAllLongPresses();
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
        AccessibilityManager.getInstance().invalidate(newForm,
                AccessibilityManager.CHANGE_STRUCTURE | AccessibilityManager.CHANGE_PANE);
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
        // The top level rather than the form. getComponentForm() is null by design
        // inside a Window, so this guard rejected every editor in a window before any
        // of the port level editor routing could run -- native text editing in a window
        // was unreachable from here however correct the ports were.
        TopLevelContainer f = cmp.getTopLevelContainer();

        // this can happen in the spinner in the simulator where the key press should in theory start native
        // edit
        if (f == null) {
            return;
        }
        Component.setDisableSmoothScrolling(true);
        f.scrollComponentToVisible(cmp);
        Component.setDisableSmoothScrolling(false);
        cancelAllKeyRepeats();
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
            if (isTerminationEvent(type)
                    ? !hasInputEventStackCapacity(2)
                    : !hasDroppableInputEventStackCapacity(2)) {
                return;
            }
            inputEventStack[inputEventStackPointer] = type;
            inputEventStackPointer++;
            inputEventStack[inputEventStackPointer] = code;
            inputEventStackPointer++;
            lock.notifyAll();
        }
    }

    /// Slots at the end of the input stack that only a termination may use.
    ///
    /// The native queues protect a release from being dropped on overflow, and that is
    /// worth nothing if this second queue drops it instead: a lost release leaves the
    /// component the press went to stuck down, or a drag never finished. Ordinary input
    /// -- moves, drags, presses, hovers -- therefore stops short of the end of the
    /// stack, leaving room for the releases and size changes that cannot be
    /// reconstructed.
    ///
    /// A press is ordinary on purpose: a release arriving with no press behind it finds
    /// no recorded target and is discarded harmlessly, so when something has to go it
    /// must never be the release.
    private static final int TERMINATION_RESERVE = 64;

    private boolean hasInputEventStackCapacity(int additionalSlots) {
        return inputEventStackPointer + additionalSlots < inputEventStack.length;
    }

    /// Capacity for an event that may be dropped, which stops short of the reserve.
    private boolean hasDroppableInputEventStackCapacity(int additionalSlots) {
        return inputEventStackPointer + additionalSlots
                < inputEventStack.length - TERMINATION_RESERVE;
    }

    /// Whether this packed event type is one whose loss the framework cannot recover
    /// from, and which may therefore use the reserve.
    private static boolean isTerminationEvent(int packedType) {
        int type = packedType & 0xFF;
        return type == POINTER_RELEASED || type == POINTER_RELEASED_MULTI
                || type == POINTER_HOVER_RELEASED || type == KEY_RELEASED
                || type == SIZE_CHANGED;
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
    public boolean isRightMouseButtonDown() {
        return impl.isRightMouseButtonDown();
    }

    /// Checks if shift key is currently down.  Only relevant for desktop ports.
    public boolean isShiftKeyDown() {
        return impl.isShiftKeyDown();
    }

    /// Returns a snapshot of the rich detail for the pointer event currently being dispatched
    /// such as the mouse button, pointer type (finger/mouse/stylus), pressure and stylus tilt.
    ///
    /// This is most useful when called from within a pointer listener. When no pointer event has
    /// been dispatched yet a default snapshot at the last known pointer location is returned.
    ///
    /// #### Returns
    ///
    /// the current `PointerEvent`, never null
    public PointerEvent getCurrentPointerEvent() {
        if (currentPointerEvent != null) {
            return currentPointerEvent;
        }
        return impl.buildPointerEvent(pointerX, pointerY, impl.isPointerHovering());
    }

    /// The mouse button associated with the current pointer event, one of the
    /// `PointerEvent` `BUTTON_*` constants.
    public int getPointerButton() {
        return impl.getPointerButton();
    }

    /// A bitmask of the mouse buttons currently held down, built from the
    /// `PointerEvent` `MASK_*` constants.
    public int getPressedButtonMask() {
        return impl.getPointerButtonMask();
    }

    /// The current pointing device type, one of the `PointerEvent` `TYPE_*`
    /// constants (finger, mouse, stylus or eraser).
    public int getPointerType() {
        return impl.getPointerType();
    }

    /// The normalized pressure of the current pointer event between `0.0` and `1.0`. Devices and
    /// ports that do not report pressure return `1.0`.
    public float getPointerPressure() {
        return impl.getPointerPressure();
    }

    /// The stylus tilt across the x axis of the current pointer event in degrees, or `0` when not reported.
    public float getPointerTiltX() {
        return impl.getPointerTiltX();
    }

    /// The stylus tilt across the y axis of the current pointer event in degrees, or `0` when not reported.
    public float getPointerTiltY() {
        return impl.getPointerTiltY();
    }

    /// The normalized contact size of the current pointer event between `0.0` and `1.0`, or `0` when not reported.
    public float getPointerContactSize() {
        return impl.getPointerContactSize();
    }

    /// True if the current pointer is a stylus or pen (Apple Pencil, S-Pen and similar).
    public boolean isStylusPointer() {
        int t = impl.getPointerType();
        return t == PointerEvent.TYPE_STYLUS
                || t == PointerEvent.TYPE_ERASER;
    }

    /// Dispatches a mouse wheel event to the component under the given coordinates. Invoked by the
    /// implementation on the EDT before the default scrolling gesture is synthesized. Returns true
    /// if a listener consumed the event, in which case the default scroll should be skipped.
    ///
    /// #### Parameters
    ///
    /// - `x`: the pointer x position in display pixels
    ///
    /// - `y`: the pointer y position in display pixels
    ///
    /// - `scrollX`: the horizontal scroll amount in display pixels
    ///
    /// - `scrollY`: the vertical scroll amount in display pixels
    ///
    /// - `precise`: true if the deltas come from a high resolution device such as a trackpad
    ///
    /// - `modifiers`: bitmask of the held keyboard modifiers
    ///
    /// #### Returns
    ///
    /// true if a listener consumed the wheel event
    public boolean fireMouseWheelEvent(int x, int y, int scrollX, int scrollY, boolean precise, int modifiers) {
        return windowMouseWheelEvent(0, x, y, scrollX, scrollY, precise, modifiers);
    }

    /// Same as `#fireMouseWheelEvent(int, int, int, int, boolean, int)`, for a wheel
    /// event that arrived over a specific native window.
    ///
    /// A port with desktop windows has to route the wheel explicitly: the main form
    /// version resolves the component from `#getCurrent()`, so a wheel over a second
    /// window would either do nothing or scroll the main form's content instead.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created, or 0 for
    ///   the application's main surface
    ///
    /// - `x`: the pointer x position in window pixels
    ///
    /// - `y`: the pointer y position in window pixels
    ///
    /// - `scrollX`: the horizontal scroll amount in display pixels
    ///
    /// - `scrollY`: the vertical scroll amount in display pixels
    ///
    /// - `precise`: true if the deltas come from a high resolution device such as a trackpad
    ///
    /// - `modifiers`: bitmask of the held keyboard modifiers
    ///
    /// #### Returns
    ///
    /// true if a listener consumed the wheel event
    public boolean windowMouseWheelEvent(int windowId, int x, int y, int scrollX, int scrollY,
            boolean precise, int modifiers) {
        if (isBlockedByModal(windowId)) {
            return true;
        }
        Container root;
        if (windowId > 0) {
            Window w = Desktop.getInstance().windowById(windowId);
            // The same hidden check the packed input path applies. A wheel callback
            // queued before its window was hidden still finds the window registered,
            // and would dispatch into an invisible tree -- an unconsumed listener that
            // hides its own window is the immediate case, since the synthetic press,
            // drag and release queued after it start against a window that is gone.
            if (w == null || !w.isWindowShowing()) {
                return false;
            }
            root = w;
        } else {
            root = getCurrent();
        }
        if (root == null) {
            return false;
        }
        Component cmp;
        try {
            cmp = root.getComponentAt(x, y);
        } catch (Throwable t) {
            cmp = null;
        }
        if (cmp == null) {
            return false;
        }
        com.codename1.ui.events.WheelEvent we = new com.codename1.ui.events.WheelEvent(cmp, x, y, scrollX, scrollY, precise, modifiers);
        return cmp.fireMouseWheelEvent(we);
    }

    /// Dispatches a magnify (pinch) gesture to the component under the given coordinates, walking up
    /// the hierarchy until a component handles it. Invoked by the implementation for native
    /// trackpad / multi touch magnify gestures; routes to `com.codename1.ui.Component#pinch(float)`.
    ///
    /// #### Parameters
    ///
    /// - `x`: the gesture x position in display pixels
    ///
    /// - `y`: the gesture y position in display pixels
    ///
    /// - `scale`: the magnification scale, larger than 1 zooms in and smaller than 1 zooms out
    public void fireMagnifyGesture(int x, int y, float scale) {
        windowMagnifyGesture(0, x, y, scale);
    }

    /// Dispatches a magnify (pinch) gesture aimed at one native window. Invoked by the
    /// implementation for a gesture that arrived over a secondary window; window 0 is
    /// the application's main surface, which is what `#fireMagnifyGesture(int, int, float)`
    /// reports.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created
    ///
    /// - `x`: the gesture x position in pixels, relative to that window
    ///
    /// - `y`: the gesture y position in pixels, relative to that window
    ///
    /// - `scale`: the magnification scale, larger than 1 zooms in and smaller than 1 zooms out
    public void windowMagnifyGesture(int windowId, int x, int y, float scale) {
        Container f = gestureRoot(windowId);
        if (f == null) {
            return;
        }
        Component cmp = gestureComponentAt(f, x, y);
        if (cmp == null) {
            cmp = f;
        }
        while (cmp != null) {
            if (cmp.pinch(scale)) {
                return;
            }
            cmp = cmp.getParent();
        }
    }

    /// Dispatches a rotation (twist) gesture to the component under the given coordinates, walking
    /// up the hierarchy until a component handles it. Invoked by the implementation for native
    /// trackpad / multi touch rotation gestures; routes to `com.codename1.ui.Component#rotation(float)`.
    ///
    /// #### Parameters
    ///
    /// - `x`: the gesture x position in display pixels
    ///
    /// - `y`: the gesture y position in display pixels
    ///
    /// - `radians`: the incremental rotation in radians, positive is clockwise
    public void fireRotationGesture(int x, int y, float radians) {
        windowRotationGesture(0, x, y, radians);
    }

    /// Dispatches a rotation (twist) gesture aimed at one native window. Invoked by the
    /// implementation for a gesture that arrived over a secondary window; window 0 is
    /// the application's main surface, which is what `#fireRotationGesture(int, int, float)`
    /// reports.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created
    ///
    /// - `x`: the gesture x position in pixels, relative to that window
    ///
    /// - `y`: the gesture y position in pixels, relative to that window
    ///
    /// - `radians`: the incremental rotation in radians, positive is clockwise
    public void windowRotationGesture(int windowId, int x, int y, float radians) {
        Container f = gestureRoot(windowId);
        if (f == null) {
            return;
        }
        Component cmp = gestureComponentAt(f, x, y);
        while (cmp != null) {
            if (cmp.rotation(radians)) {
                return;
            }
            cmp = cmp.getParent();
        }
    }

    /// The top level a gesture was aimed at, or null when it is gone or currently
    /// blocked by a modal window. Gestures are filtered like every other input event:
    /// pinching a window a modal is blocking has to do nothing, the same way clicking
    /// it does.
    private Container gestureRoot(int windowId) {
        if (isBlockedByModal(windowId)) {
            return null;
        }
        if (windowId > 0) {
            // Hidden as well as blocked. A pinch or rotation callback queued before
            // the window was hidden still finds it registered, and would drive the
            // gesture handlers of an invisible tree -- the same stale-callback case
            // the wheel path and the packed queue already reject.
            Window w = Desktop.getInstance().windowById(windowId);
            return w != null && w.isWindowShowing() ? w : null;
        }
        return getCurrent();
    }

    private static Component gestureComponentAt(Container root, int x, int y) {
        try {
            return root.getComponentAt(x, y);
        } catch (Throwable t) {
            return null;
        }
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
        keyPressedImpl(0, keyCode);
    }

    /// Pushes a key press event aimed at one native window into Codename One.
    /// Invoked by the implementation, off the event dispatch thread.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created
    ///
    /// - `keyCode`: keycode of the key event
    public void windowKeyPressed(int windowId, int keyCode) {
        if (windowId > 0) {
            keyPressedImpl(windowId, keyCode);
        }
    }

    private void keyPressedImpl(int windowId, final int keyCode) {
        addSingleArgumentEvent(KEY_PRESSED | (windowId << 8), keyCode);

        lastInteractionWasKeypad = lastInteractionWasKeypad || (keyCode != MenuBar.leftSK && keyCode != MenuBar.clearSK && keyCode != MenuBar.backSK);

        // this solves a Sony Ericsson bug where on slider open/close someone "brilliant" chose
        // to send a keyPress with a -43/-44 keycode... Without ever sending a key release!
        boolean armed = (keyCode >= 0 || getGameAction(keyCode) > 0) || keyCode == impl.getClearKeyCode();
        long now = System.currentTimeMillis();
        int keyRepeatInitialIntervalTime = 800;
        chargeKeyRepeat(windowId, keyCode, armed, now, now + keyRepeatInitialIntervalTime);
        previousKeyPressed = lastKeyPressed;
        lastKeyPressed = keyCode;
    }

    /// Pushes a key release event with the given keycode into Codename One
    ///
    /// #### Parameters
    ///
    /// - `keyCode`: keycode of the key event
    public void keyReleased(final int keyCode) {
        cancelKeyRepeatForCode(keyCode);
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
            if (isTerminationEvent(type)
                    ? !hasInputEventStackCapacity(3)
                    : !hasDroppableInputEventStackCapacity(3)) {
                return;
            }
            pointerMetaStack[inputEventStackPointer] = impl.capturePointerEventMetadata();
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
            if (isTerminationEvent(type)
                    ? !hasInputEventStackCapacity(3 + x.length + y.length)
                    : !hasDroppableInputEventStackCapacity(3 + x.length + y.length)) {
                return;
            }
            pointerMetaStack[inputEventStackPointer] = impl.capturePointerEventMetadata();
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

    private void addPointerDragEventWithTimestamp(int windowId, int x, int y) {
        synchronized (lock) {
            if (this.dropEvents) {
                return;
            }
            try {
                if (lastDragOffset > -1 && lastDragWindowId == windowId) {
                    // A coalesced drag replaces the queued one, so it must also carry
                    // the newest metadata rather than the metadata of the drag it just
                    // overwrote. The type word sits one slot before the payload.
                    //
                    // The existing slot is overwritten rather than a new one taken:
                    // coalescing keeps one packet however many updates arrive, so
                    // taking a slot per update would run the ring forward without
                    // bound -- with the event dispatch thread blocked it would wrap
                    // and clobber slots belonging to presses and releases that are
                    // still queued, which is exactly the mix-up this prevents.
                    pointerMetaStack[lastDragOffset - 1] =
                            impl.recapturePointerEventMetadata(pointerMetaStack[lastDragOffset - 1]);
                    inputEventStack[lastDragOffset] = x;
                    inputEventStack[lastDragOffset + 1] = y;
                    inputEventStack[lastDragOffset + 2] = (int) (System.currentTimeMillis() - displayInitTime);
                } else {
                    // A drag is ordinary input, so it stops short of the reserve.
                    if (!hasDroppableInputEventStackCapacity(4)) {
                        return;
                    }
                    pointerMetaStack[inputEventStackPointer] = impl.capturePointerEventMetadata();
                    inputEventStack[inputEventStackPointer] = POINTER_DRAGGED | (windowId << 8);
                    inputEventStackPointer++;
                    lastDragOffset = inputEventStackPointer;
                    lastDragWindowId = windowId;
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
                if (isTerminationEvent(type)
                        ? !hasInputEventStackCapacity(4)
                        : !hasDroppableInputEventStackCapacity(4)) {
                    return;
                }
                pointerMetaStack[inputEventStackPointer] = impl.capturePointerEventMetadata();
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
        pointerDraggedImpl(0, x, y);
    }

    /// Pushes a pointer drag aimed at one native window into Codename One.
    /// Invoked by the implementation, off the event dispatch thread.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created
    ///
    /// - `x`: the x positions of the pointer
    ///
    /// - `y`: the y positions of the pointer
    public void windowPointerDragged(int windowId, int[] x, int[] y) {
        if (windowId > 0) {
            pointerDraggedImpl(windowId, x, y);
        }
    }

    private void pointerDraggedImpl(int windowId, final int[] x, final int[] y) {
        if (x.length == 0) {
            // Native ports have been observed to deliver zero-length pointer arrays
            return;
        }
        cancelLongPress(windowId);
        if (x.length == 1) {
            addPointerDragEventWithTimestamp(windowId, x[0], y[0]);
        } else {
            addPointerEvent(POINTER_DRAGGED_MULTI | (windowId << 8), x, y);
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
        pointerHoverImpl(0, x, y);
    }

    /// Pushes a pointer hover event that arrived over a specific native window.
    ///
    /// A port with desktop windows has to say which window the pointer was over, or
    /// hovering a second window sends the event to whatever the main form has at the
    /// same coordinates -- so the window gets no tooltips and the main form gets
    /// spurious ones.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created
    ///
    /// - `x`: the x position of the pointer, in window coordinates
    ///
    /// - `y`: the y position of the pointer, in window coordinates
    public void windowPointerHover(int windowId, final int[] x, final int[] y) {
        if (windowId > 0) {
            pointerHoverImpl(windowId, x, y);
        }
    }

    private void pointerHoverImpl(int windowId, final int[] x, final int[] y) {
        if (windowId == 0 && impl.getCurrentForm() == null) {
            return;
        }
        if (windowId > 0 && Desktop.getInstance().windowById(windowId) == null) {
            return;
        }
        if (x.length == 1) {
            addPointerEventWithTimestamp(POINTER_HOVER | (windowId << 8), x[0], y[0]);
        } else {
            addPointerEvent(POINTER_HOVER | (windowId << 8), x, y);
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

    /// Pushes a hover press aimed at one native window into Codename One. Invoked by
    /// the implementation, off the event dispatch thread.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created
    ///
    /// - `x`: the x position of the pointer, in window coordinates
    ///
    /// - `y`: the y position of the pointer, in window coordinates
    public void windowPointerHoverPressed(int windowId, final int[] x, final int[] y) {
        if (windowId > 0 && Desktop.getInstance().windowById(windowId) != null) {
            addPointerEvent(POINTER_HOVER_PRESSED | (windowId << 8), x[0], y[0]);
        }
    }

    /// Pushes a hover release aimed at one native window into Codename One. Invoked by
    /// the implementation, off the event dispatch thread.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created
    ///
    /// - `x`: the x position of the pointer, in window coordinates
    ///
    /// - `y`: the y position of the pointer, in window coordinates
    public void windowPointerHoverReleased(int windowId, final int[] x, final int[] y) {
        if (windowId > 0 && Desktop.getInstance().windowById(windowId) != null) {
            addPointerEvent(POINTER_HOVER_RELEASED | (windowId << 8), x[0], y[0]);
        }
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
        pointerPressedImpl(0, x, y);
    }

    /// Pushes a key release aimed at one native window into Codename One.
    /// Invoked by the implementation, off the event dispatch thread.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created
    ///
    /// - `keyCode`: keycode of the key event
    public void windowKeyReleased(int windowId, int keyCode) {
        if (windowId > 0) {
            cancelKeyRepeatForCode(keyCode);
            addSingleArgumentEvent(KEY_RELEASED | (windowId << 8), keyCode);
        }
    }

    /// Returns the native window peer owning the given component, or null when it
    /// belongs to the application's main surface. Ports use this to place native
    /// peers and native text editors into the correct window.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: the component to locate
    ///
    /// #### Returns
    ///
    /// the owning window's native peer, or null for the main surface
    public Object getWindowPeerForComponent(Component cmp) {
        if (cmp == null) {
            return null;
        }
        TopLevelContainer top = cmp.getTopLevelContainer();
        if (top instanceof Window) {
            return ((Window) top).getNativePeer();
        }
        return null;
    }

    /// Pushes a pointer press aimed at one native window into Codename One.
    /// Invoked by the implementation, off the event dispatch thread.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created
    ///
    /// - `x`: the x positions of the pointer
    ///
    /// - `y`: the y positions of the pointer
    public void windowPointerPressed(int windowId, int[] x, int[] y) {
        if (windowId > 0) {
            pointerPressedImpl(windowId, x, y);
        }
    }

    private void pointerPressedImpl(int windowId, final int[] x, final int[] y) {
        lastInteractionWasKeypad = false;
        chargeLongPress(windowId, x[0], y[0]);
        // Still tracked globally: this is "where the pointer last was", which
        // getCurrentPointerEvent reports and which is not per window.
        pointerX = x[0];
        pointerY = y[0];
        if (x.length == 1) {
            addPointerEvent(POINTER_PRESSED | (windowId << 8), x[0], y[0]);
        } else {
            addPointerEvent(POINTER_PRESSED_MULTI | (windowId << 8), x, y);
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
        cancelLongPress(0);
        if (impl.getCurrentForm() == null) {
            return;
        }
        pointerReleasedImpl(0, x, y);
    }

    /// Pushes a pointer release aimed at one native window into Codename One.
    /// Invoked by the implementation, off the event dispatch thread.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created
    ///
    /// - `x`: the x positions of the pointer
    ///
    /// - `y`: the y positions of the pointer
    public void windowPointerReleased(int windowId, int[] x, int[] y) {
        if (windowId > 0) {
            cancelLongPress(windowId);
            pointerReleasedImpl(windowId, x, y);
        }
    }

    private void pointerReleasedImpl(int windowId, final int[] x, final int[] y) {
        if (x.length == 1) {
            addPointerEvent(POINTER_RELEASED | (windowId << 8), x[0], y[0]);
        } else {
            addPointerEvent(POINTER_RELEASED_MULTI | (windowId << 8), x, y);
        }
    }

    private void addSizeChangeEvent(int type, int w, int h) {
        synchronized (lock) {
            // A size change is state, and a lost one leaves the hierarchy laid out at
            // a size the surface no longer has -- painting and hit testing stay
            // misaligned until something else resizes the window.
            if (!hasInputEventStackCapacity(3)) {
                return;
            }
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

    /// Notifies Codename One that a native window changed size. Invoked by the
    /// implementation.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created
    ///
    /// - `w`: the new drawable width
    ///
    /// - `h`: the new drawable height
    public void windowSizeChanged(int windowId, int w, int h) {
        if (windowId <= 0) {
            return;
        }
        // Coalesced onto the event dispatch thread rather than queued as a packet.
        // The packed stack drops when it is full, which live resizing does easily, and
        // the dropped packet can be the *final* size -- the native surface has already
        // adopted it, so the hierarchy stays laid out for an earlier one with nothing
        // guaranteed to correct it, leaving painting and hit testing misaligned.
        //
        // Coalescing is what makes a non-droppable path affordable here: only one
        // notification per window is ever outstanding, and it carries whatever the
        // latest dimensions are when it runs, so a drag that produces hundreds of
        // resizes still costs one queued runnable at a time.
        final Integer key = Integer.valueOf(windowId);
        boolean queue;
        synchronized (pendingWindowSizes) {
            int[] latest = (int[]) pendingWindowSizes.get(key);
            if (latest == null) {
                latest = new int[2];
                pendingWindowSizes.put(key, latest);
                queue = true;
            } else {
                queue = false;
            }
            latest[0] = w;
            latest[1] = h;
        }
        if (!queue) {
            return;
        }
        final int id = windowId;
        callSerially(new Runnable() {
            @Override
            public void run() {
                int width;
                int height;
                synchronized (pendingWindowSizes) {
                    int[] latest = (int[]) pendingWindowSizes.remove(key);
                    if (latest == null) {
                        return;
                    }
                    width = latest[0];
                    height = latest[1];
                }
                Window w = Desktop.getInstance().windowById(id);
                if (w != null) {
                    w.sizeChangedInternal(width, height);
                }
            }
        });
    }

    /// The most recent size reported for each open window that has not been delivered
    /// yet. See `#windowSizeChanged(int, int, int)`.
    private final Hashtable pendingWindowSizes = new Hashtable();

    /// Notifies Codename One that a native window became visible.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created
    public void windowShowNotify(int windowId) {
        if (windowId > 0) {
            // Deliberately not the packed input queue. That queue drops events when it
            // is full and while invokeAndBlock is running in drop mode, and nothing
            // reconciles a lost one afterwards: a dropped show leaves a visible window
            // the framework believes is iconified and never paints again, and a
            // dropped hide leaves a hidden window painting and keeping the event
            // dispatch thread awake. Lifecycle notifications are not droppable.
            callSerially(new WindowCallback(windowId, WindowCallback.SHOWN));
        }
    }

    /// Notifies Codename One that the platform refused to create a window's native
    /// surface, so the window will never appear.
    ///
    /// Separate from `#windowHideNotify(int)` because that one means "minimized",
    /// which keeps a modal window's registration: a modal that never appeared would
    /// otherwise block input to every other window while `showModal()` waited for it.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the window whose native surface could not be created
    public void windowActivationFailed(int windowId) {
        if (windowId > 0) {
            WindowCallback failure = new WindowCallback(windowId,
                    WindowCallback.ACTIVATION_FAILED);
            if (isEdt()) {
                // Applied in the caller's own turn when it is already on the event
                // dispatch thread. A port validates this failure against the request
                // token it belongs to and then reports it, and queueing again splits
                // those two steps across turns: a retrying show() can run in between,
                // start a new request, and be marked hidden and stripped of its
                // modality by a failure that no longer applies to it. Running here
                // keeps the check and its consequence in one unit, which is what the
                // check is for.
                failure.run();
                return;
            }
            // Not droppable, for the same reason as the other lifecycle notifications.
            callSerially(failure);
        }
    }

    /// Notifies Codename One that a native window stopped being visible.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created
    public void windowHideNotify(int windowId) {
        if (windowId > 0) {
            // See windowShowNotify: not droppable.
            callSerially(new WindowCallback(windowId, WindowCallback.HIDDEN));
        }
    }

    /// Notifies Codename One that a native window gained or lost keyboard focus.
    /// Marshalled onto the event dispatch thread, since it runs application code.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created
    ///
    /// - `gained`: true when the window gained focus
    public void windowFocusChanged(int windowId, boolean gained) {
        callSerially(new WindowCallback(windowId,
                gained ? WindowCallback.FOCUS_GAINED : WindowCallback.FOCUS_LOST));
    }

    /// Notifies Codename One that the user activated a native window's close control.
    /// Marshalled onto the event dispatch thread, since it runs application code and
    /// may dispose the window.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created
    public void windowCloseRequested(int windowId) {
        // Queued first, and the modality check made on the event dispatch thread inside
        // the callback. The modal stack is mutated there by show, hide and dispose, so
        // testing it from the port's callback thread raced: isBlockedByModal takes the
        // stack's size and then indexes it, which a concurrent removal turns into an
        // exception, and a stale read could let a blocked window's close through.
        callSerially(new WindowCallback(windowId, WindowCallback.CLOSE_REQUESTED));
    }

    /// Notifies Codename One that the platform has already destroyed a window's
    /// native surface, so the window is gone whatever the application would prefer.
    ///
    /// Distinct from `#windowCloseRequested(int)`, which asks. Some platforms do not
    /// offer the close control as a question: a Mac Catalyst scene is disconnected
    /// after the fact, with nothing left to veto. Reporting that as a request would
    /// let `DO_NOTHING_ON_CLOSE` leave a registered window painting into a surface
    /// that no longer exists, so it is reported as what it is and the window is
    /// disposed.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created
    public void windowClosedNatively(int windowId) {
        callSerially(new WindowCallback(windowId, WindowCallback.CLOSED_NATIVELY));
    }

    /// Notifies Codename One that a native window moved to a monitor with different
    /// characteristics, so that its scale and layout are recomputed.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created
    public void windowMonitorChanged(int windowId) {
        callSerially(new WindowCallback(windowId, WindowCallback.MONITOR_CHANGED));
    }

    /// Notifies Codename One that the user moved a native window.
    ///
    /// Separate from `#windowMonitorChanged(int)`, which is only for a move that
    /// carried the window onto a different display: an ordinary move within one
    /// monitor still has to reach the listeners, or nothing can persist a window's
    /// position.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created
    public void windowMoved(int windowId) {
        if (windowId > 0) {
            callSerially(new WindowCallback(windowId, WindowCallback.MOVED));
        }
    }

    /// Notifies Codename One that the set of attached monitors changed.
    public void monitorsChanged() {
        synchronized (monitorsChangedLock) {
            // Genuinely coalesced rather than merely documented as such. One physical
            // display change is reported many times over: Windows broadcasts
            // WM_DISPLAYCHANGE to every top level window, and GTK fires geometry,
            // work-area and scale-factor notifications separately for each monitor.
            // Each notification relays out every open window and fires every monitor
            // listener, so without this a single resolution change did that work N
            // times. A change arriving while one is queued is already covered by it.
            if (monitorsChangedPending) {
                return;
            }
            monitorsChangedPending = true;
        }
        callSerially(new WindowCallback(0, WindowCallback.MONITORS_CHANGED));
    }

    /// Whether this release finishes a press the framework already accepted.
    ///
    /// A press handler is allowed to open a modal window, and then the matching
    /// release arrives with its own window blocked. Dropping it leaves the component
    /// that took the press latched down for good and the recorded target never
    /// cleared, so the next release matches the wrong thing. Modality is there to stop
    /// *new* interaction, not to strand a gesture that was already under way.
    ///
    /// Only a release with a recorded press passes. A press that was itself filtered
    /// leaves no record, so clicking a blocked window still does nothing.
    private boolean completesAcceptedPress(int type, int windowId, int offset, int[] stack) {
        switch (type) {
            case KEY_RELEASED:
                // The key code is the packet's first argument.
                return hasKeyPressTarget(stack[offset + 1]);
            case POINTER_RELEASED:
            case POINTER_RELEASED_MULTI:
                return hasPointerPressTarget(windowId);
            default:
                return false;
        }
    }

    /// Whether a press for this key is recorded, without consuming it.
    private boolean hasKeyPressTarget(int keyCode) {
        for (int iter = 0; iter < TRACKED_KEY_PRESSES; iter++) {
            if (keyPressTargets[iter] != null && keyPressCodes[iter] == keyCode) {
                return true;
            }
        }
        return false;
    }

    /// Whether this packet is user input, and so subject to modal blocking.
    ///
    /// Modality blocks what the user does to a window, not what the platform tells the
    /// framework about it. A blocked window is still resized, hidden and shown by the
    /// window system, and dropping those left the hierarchy at stale dimensions once
    /// the modal closed -- painting and hit testing then disagreed with the native
    /// canvas until something else forced a resize.
    /// True for the event types that carry rich pointer metadata, i.e. the ones whose
    /// dispatch builds a `com.codename1.ui.events.PointerEvent`.
    private static boolean isPointerEvent(int type) {
        switch (type) {
            case POINTER_PRESSED:
            case POINTER_RELEASED:
            case POINTER_DRAGGED:
            case POINTER_PRESSED_MULTI:
            case POINTER_RELEASED_MULTI:
            case POINTER_DRAGGED_MULTI:
            case POINTER_HOVER:
            case POINTER_HOVER_PRESSED:
            case POINTER_HOVER_RELEASED:
                return true;
            default:
                return false;
        }
    }

    private static boolean isUserInputEvent(int type) {
        switch (type) {
            case POINTER_PRESSED:
            case POINTER_RELEASED:
            case POINTER_DRAGGED:
            case POINTER_PRESSED_MULTI:
            case POINTER_RELEASED_MULTI:
            case POINTER_DRAGGED_MULTI:
            case POINTER_HOVER:
            case POINTER_HOVER_PRESSED:
            case POINTER_HOVER_RELEASED:
            case KEY_PRESSED:
            case KEY_RELEASED:
                return true;
            default:
                // SIZE_CHANGED, HIDE_NOTIFY and SHOW_NOTIFY are the platform
                // reporting what it did, not the user reaching the window.
                return false;
        }
    }

    /// Whether a drag has happened on the *main* surface since its press.
    ///
    /// The main surface keeps the original single flag deliberately. Routing window 0
    /// through the per-window table changed behaviour for ordinary single-window
    /// applications -- it regressed an unrelated component test -- and the defect
    /// being fixed here is specifically that a *secondary* window's press clobbered
    /// another window's state. Windows above 0 get their own entry.
    private boolean dragOccured;

    /// Whether a drag has happened in each secondary window since its press, paired
    /// by index with `#longPressWindows`.
    ///
    /// Global before: pressing in one window cleared the flag after another had
    /// already dragged, so releasing the first made `List` and friends read
    /// `hasDragOccured()` as false and treat a completed drag as a click.
    private final boolean[] dragOccuredPerWindow = new boolean[TRACKED_KEY_PRESSES];

    /// Key repeat and long-key-press state, per window for the same reason the
    /// pointer equivalents are: a key held in one window and another pressed in a
    /// second window shared one id, value and clock, so each cancelled the other.
    private final int[] keyRepeatWindows = new int[TRACKED_KEY_PRESSES];
    private final boolean[] keyRepeatArmed = new boolean[TRACKED_KEY_PRESSES];
    private final boolean[] keyLongPressArmed = new boolean[TRACKED_KEY_PRESSES];
    private final int[] keyRepeatValues = new int[TRACKED_KEY_PRESSES];
    private final long[] keyRepeatNext = new long[TRACKED_KEY_PRESSES];
    private final long[] keyLongPressStart = new long[TRACKED_KEY_PRESSES];

    /// Ids of windows with a long press being timed, paired by index with the arrays
    /// below. Zero means the entry is unused; window 0 uses `#MAIN_LONG_PRESS_ID`.
    private final int[] longPressWindows = new int[TRACKED_KEY_PRESSES];
    private final boolean[] longPressArmed = new boolean[TRACKED_KEY_PRESSES];
    private final int[] longPressPointerX = new int[TRACKED_KEY_PRESSES];
    private final int[] longPressPointerY = new int[TRACKED_KEY_PRESSES];
    private final long[] longPressStart = new long[TRACKED_KEY_PRESSES];

    /// Stand-in id for the main surface, so 0 can mean "unused" in
    /// `#longPressWindows`.
    private static final int MAIN_LONG_PRESS_ID = -1;

    private static int longPressKey(int windowId) {
        return windowId == 0 ? MAIN_LONG_PRESS_ID : windowId;
    }

    /// The key-repeat slot for a window, allocating one if needed.
    private int keyRepeatSlot(int windowId, boolean create) {
        int key = longPressKey(windowId);
        int free = -1;
        for (int iter = 0; iter < TRACKED_KEY_PRESSES; iter++) {
            if (keyRepeatWindows[iter] == key) {
                return iter;
            }
            if (free < 0 && keyRepeatWindows[iter] == 0) {
                free = iter;
            }
        }
        if (!create || free < 0) {
            return -1;
        }
        keyRepeatWindows[free] = key;
        return free;
    }

    /// Arms key repeat and the long-key-press timer for one window.
    private void chargeKeyRepeat(int windowId, int keyCode, boolean armed, long now,
            long firstRepeatAt) {
        int slot = keyRepeatSlot(windowId, true);
        if (slot < 0) {
            return;
        }
        keyRepeatArmed[slot] = armed;
        keyLongPressArmed[slot] = armed;
        keyRepeatValues[slot] = keyCode;
        keyLongPressStart[slot] = now;
        keyRepeatNext[slot] = firstRepeatAt;
    }

    /// Cancels whichever window armed a repeat for this key code.
    ///
    /// Keyed by the code rather than by the window the key-up packet names: the
    /// physical key was armed by the *press*, and focus can move between the two, so
    /// cancelling the releasing window's slot left the pressing window repeating
    /// every 10ms with the key physically up.
    private void cancelKeyRepeatForCode(int keyCode) {
        for (int iter = 0; iter < TRACKED_KEY_PRESSES; iter++) {
            if (keyRepeatWindows[iter] != 0 && keyRepeatValues[iter] == keyCode) {
                keyRepeatArmed[iter] = false;
                keyLongPressArmed[iter] = false;
                keyRepeatWindows[iter] = 0;
            }
        }
    }

    /// Cancels key repeat for one window, leaving the others alone.
    private void cancelKeyRepeat(int windowId) {
        int slot = keyRepeatSlot(windowId, false);
        if (slot >= 0) {
            keyRepeatArmed[slot] = false;
            keyLongPressArmed[slot] = false;
            keyRepeatWindows[slot] = 0;
        }
    }

    /// Cancels key repeat everywhere, for the paths that reset all input state.
    private void cancelAllKeyRepeats() {
        for (int iter = 0; iter < TRACKED_KEY_PRESSES; iter++) {
            keyRepeatArmed[iter] = false;
            keyLongPressArmed[iter] = false;
            keyRepeatWindows[iter] = 0;
        }
    }

    /// Whether any window has *both* key repeat and a long key press armed. The
    /// single-flag predicate this replaces was `!keyRepeatCharged ||
    /// !longPressCharged`, i.e. false only when both were set.
    private boolean anyKeyRepeatAndLongPressArmed() {
        for (int iter = 0; iter < TRACKED_KEY_PRESSES; iter++) {
            if (keyRepeatArmed[iter] && keyLongPressArmed[iter]) {
                return true;
            }
        }
        return false;
    }

    /// Whether any window still has key repeat or a long key press pending.
    private boolean anyKeyRepeatArmed() {
        for (int iter = 0; iter < TRACKED_KEY_PRESSES; iter++) {
            if (keyRepeatArmed[iter] || keyLongPressArmed[iter]) {
                return true;
            }
        }
        return false;
    }

    /// Records that a drag happened in one window.
    private void setDragOccured(int windowId, boolean value) {
        if (windowId == 0) {
            dragOccured = value;
            return;
        }
        int slot = dragHistorySlot(windowId);
        if (slot >= 0) {
            dragOccuredPerWindow[slot] = value;
        }
    }

    /// Starts timing a long press for one window.
    ///
    /// Per window rather than singleton for the same reason the press targets are:
    /// with a contact down in two windows, pressing in the second replaced the
    /// first's coordinates and timer, and releasing either cancelled the other's
    /// pending long press.
    private void chargeLongPress(int windowId, int x, int y) {
        int key = longPressKey(windowId);
        int free = -1;
        for (int iter = 0; iter < TRACKED_KEY_PRESSES; iter++) {
            if (longPressWindows[iter] == key) {
                free = iter;
                break;
            }
            if (free < 0 && longPressWindows[iter] == 0) {
                free = iter;
            }
        }
        if (free < 0) {
            return;
        }
        longPressWindows[free] = key;
        longPressArmed[free] = true;
        longPressPointerX[free] = x;
        longPressPointerY[free] = y;
        longPressStart[free] = System.currentTimeMillis();
    }

    /// Cancels the long press pending for one window, leaving other windows alone.
    private void cancelLongPress(int windowId) {
        int key = longPressKey(windowId);
        for (int iter = 0; iter < TRACKED_KEY_PRESSES; iter++) {
            if (longPressWindows[iter] == key) {
                longPressArmed[iter] = false;
                longPressWindows[iter] = 0;
                return;
            }
        }
    }

    /// Whether any window is still timing a long press; the event dispatch thread
    /// must not park while one is pending.
    private boolean anyLongPressArmed() {
        for (int iter = 0; iter < TRACKED_KEY_PRESSES; iter++) {
            if (longPressArmed[iter]) {
                return true;
            }
        }
        return false;
    }

    /// Cancels every pending long press, for the paths that reset all input state.
    private void cancelAllLongPresses() {
        for (int iter = 0; iter < TRACKED_KEY_PRESSES; iter++) {
            longPressArmed[iter] = false;
            longPressWindows[iter] = 0;
        }
    }

    /// Records which top level saw a pointer press in the given window.
    private void rememberPointerPress(int windowId, Container target) {
        int free = -1;
        for (int iter = 0; iter < TRACKED_KEY_PRESSES; iter++) {
            if (pointerPressTargets[iter] != null && pointerPressWindows[iter] == windowId) {
                pointerPressTargets[iter] = target;
                return;
            }
            if (free < 0 && pointerPressTargets[iter] == null) {
                free = iter;
            }
        }
        if (free >= 0) {
            pointerPressWindows[free] = windowId;
            pointerPressTargets[free] = target;
        }
    }

    /// Returns and forgets the top level that saw this window's pointer press.
    private Container takePointerPressTarget(int windowId) {
        for (int iter = 0; iter < TRACKED_KEY_PRESSES; iter++) {
            if (pointerPressTargets[iter] != null && pointerPressWindows[iter] == windowId) {
                Container out = pointerPressTargets[iter];
                pointerPressTargets[iter] = null;
                pointerPressWindows[iter] = 0;
                return out;
            }
        }
        return null;
    }

    /// Whether a pointer press is recorded for the window, without consuming it.
    private boolean hasPointerPressTarget(int windowId) {
        for (int iter = 0; iter < TRACKED_KEY_PRESSES; iter++) {
            if (pointerPressTargets[iter] != null && pointerPressWindows[iter] == windowId) {
                return true;
            }
        }
        return false;
    }

    /// Records which top level saw a key press, so its release can be matched to it.
    /// Called on the event dispatch thread only.
    /// Records which top level is holding this key, and answers the one the press
    /// belongs to.
    ///
    /// #### Returns
    ///
    /// the top level that saw this key go down, which is `target` for a fresh press
    /// and the remembered one for a repeat
    private Container rememberKeyPress(int keyCode, Container target) {
        int free = -1;
        for (int iter = 0; iter < TRACKED_KEY_PRESSES; iter++) {
            if (keyPressTargets[iter] != null && keyPressCodes[iter] == keyCode) {
                // Already held. The native ports forward every autorepeat as another
                // press, so replacing the target here handed the key to whichever
                // window had focus when the repeat arrived -- and the eventual key-up
                // then went there instead of to the window that saw the original
                // press, leaving a fire-key-activated Button stuck down.
                return keyPressTargets[iter];
            }
            if (free < 0 && keyPressTargets[iter] == null) {
                free = iter;
            }
        }
        if (free >= 0) {
            keyPressCodes[free] = keyCode;
            keyPressTargets[free] = target;
        }
        return target;
    }

    /// Returns and forgets the top level that saw this key's press, or null when
    /// there is no record of one.
    private Container takeKeyPressTarget(int keyCode) {
        for (int iter = 0; iter < TRACKED_KEY_PRESSES; iter++) {
            if (keyPressTargets[iter] != null && keyPressCodes[iter] == keyCode) {
                Container out = keyPressTargets[iter];
                keyPressTargets[iter] = null;
                keyPressCodes[iter] = 0;
                return out;
            }
        }
        return null;
    }

    /// Lets the queued notification re-arm the coalescing guard. See
    /// `#monitorsChanged()`.
    void clearMonitorsChangedPending() {
        synchronized (monitorsChangedLock) {
            monitorsChangedPending = false;
        }
    }

    /// Marshals a window notification that arrived on the platform's own thread onto
    /// the event dispatch thread. A named static class rather than an anonymous one so
    /// it does not retain the `Display` it was created from.
    private static final class WindowCallback implements Runnable {
        private static final int FOCUS_GAINED = 0;
        private static final int FOCUS_LOST = 1;
        private static final int CLOSE_REQUESTED = 2;
        private static final int MONITOR_CHANGED = 3;
        private static final int MONITORS_CHANGED = 4;
        private static final int MOVED = 5;
        private static final int CLOSED_NATIVELY = 6;
        private static final int SHOWN = 7;
        private static final int HIDDEN = 8;
        private static final int ACTIVATION_FAILED = 9;

        private final int windowId;
        private final int kind;

        WindowCallback(int windowId, int kind) {
            this.windowId = windowId;
            this.kind = kind;
        }

        @Override
        public void run() {
            Desktop desktop = Desktop.getInstance();
            Window w = desktop.windowById(windowId);
            switch (kind) {
                case FOCUS_GAINED:
                    desktop.setFocusedWindow(w);
                    break;
                case FOCUS_LOST:
                    if (desktop.getFocusedWindow() == w) { //NOPMD CompareObjectsWithEquals
                        desktop.setFocusedWindow(null);
                    }
                    if (w != null) {
                        // The fifth way a window stops being reachable, after hide,
                        // minimize, dispose and modal blocking. The physical key-up
                        // goes to whatever has focus now, so without this a held key
                        // repeats into this window for as long as it stays open and
                        // a pressed component stays latched.
                        w.cancelPendingInput();
                    }
                    break;
                case CLOSE_REQUESTED:
                    // A close arrives outside the packed input queue, so it bypasses the
                    // modality filter that guards every other event. A port that cannot
                    // disable a blocked window natively -- Catalyst has no such control
                    // -- would otherwise let the user close a window an application
                    // modal is supposed to be blocking. Checked here rather than at the
                    // callback, so the modal stack is only ever read on this thread.
                    if (w != null && !Display.getInstance().isBlockedByModal(windowId)) {
                        w.closeRequested();
                    }
                    break;
                case MONITOR_CHANGED:
                    // One window moved to another display. Deliberately not
                    // desktop.fireMonitorChanged(): Desktop.addMonitorListener is
                    // documented for a monitor being attached, removed or
                    // reconfigured, and firing it for every drag across a mixed-DPI
                    // desktop turned an ordinary window move into a topology event --
                    // repeatedly re-running whatever display reconfiguration work the
                    // application does there. The window itself re-reads its scale and
                    // lays out below, and an application that wants to follow one
                    // window across displays sees it through that window's Moved
                    // event plus getMonitor().
                    if (w != null) {
                        w.monitorChanged();
                    }
                    break;
                case MONITORS_CHANGED:
                    // Cleared before the work, not after: a display change that
                    // happens while this runs describes a topology this pass has not
                    // read yet, so it has to queue another one rather than be
                    // swallowed as a duplicate.
                    Display.getInstance().clearMonitorsChangedPending();
                    for (Window each : desktop.getWindows()) {
                        each.monitorChanged();
                    }
                    desktop.fireMonitorChanged();
                    break;
                case MOVED:
                    if (w != null) {
                        w.moved();
                    }
                    break;
                case SHOWN:
                    if (w != null) {
                        w.showNotify();
                    }
                    break;
                case HIDDEN:
                    if (w != null) {
                        w.hideNotify();
                    }
                    break;
                case ACTIVATION_FAILED:
                    if (w != null) {
                        w.activationFailed();
                    }
                    break;
                case CLOSED_NATIVELY:
                    if (w != null) {
                        // A window a modal is blocking must not be closable. Where the
                        // platform's close control cannot be disabled the close has
                        // already happened, so the only way to honour the contract is
                        // to put the window back; a port that cannot returns false and
                        // the window is disposed, because the surface is genuinely gone.
                        if (!Display.getInstance().isBlockedByModal(windowId)
                                || !w.reopenNativeSurface()) {
                            w.dispose();
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void addNotifyEvent(int type) {
        synchronized (lock) {
            if (!hasInputEventStackCapacity(1)) {
                return;
            }
            inputEventStack[inputEventStackPointer] = type;
            inputEventStackPointer++;
            lock.notifyAll();
        }
    }

    /// Broadcasts hide notify into Codename One, this method is invoked by the Codename One implementation
    /// to notify Codename One of hideNotify events
    public void hideNotify() {
        cancelAllKeyRepeats();
        cancelAllLongPresses();
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
                    // Deliberately "not both", which is what the single-flag version
                    // meant: (!keyRepeatCharged || !longPressCharged). Collapsing it
                    // to "neither armed" is a different predicate and made this
                    // report not-idle far more often, which stalled the flush.
                    !anyKeyRepeatAndLongPressArmed();
        }
        return b;
    }

    private void updateDragSpeedStatus(int windowId, int x, int y, int timestamp) {
        //save dragging input to calculate the dragging speed later
        int slot = dragHistorySlot(windowId);
        if (slot < 0) {
            return;
        }
        dragPathX[slot][dragPathOffset[slot]] = x;
        dragPathY[slot][dragPathOffset[slot]] = y;
        dragPathTime[slot][dragPathOffset[slot]] = displayInitTime + (long) timestamp;
        if (dragPathLength[slot] < PATHLENGTH) {
            dragPathLength[slot]++;
        }
        dragPathOffset[slot]++;
        if (dragPathOffset[slot] >= PATHLENGTH) {
            dragPathOffset[slot] = 0;
        }
    }

    /// The drag ring for a window, allocating it on first use. Returns -1 only when
    /// every slot is taken, in which case the samples are dropped rather than
    /// corrupting another window's history.
    private int dragHistorySlot(int windowId) {
        return dragHistorySlot(windowId, true);
    }

    /// Looks a window's drag ring up, optionally allocating one.
    ///
    /// `create` is false for readers. Allocating from `hasDragOccured()` or
    /// `getDragSpeed()` -- which is what the first version of this did -- makes a
    /// query mutate state: it claimed a slot and zeroed the ring, so simply asking
    /// about a window could wipe another's history once the table filled.
    private int dragHistorySlot(int windowId, boolean create) {
        int key = longPressKey(windowId);
        int free = -1;
        for (int iter = 0; iter < TRACKED_KEY_PRESSES; iter++) {
            if (dragHistoryWindows[iter] == key) {
                return iter;
            }
            if (free < 0 && dragHistoryWindows[iter] == 0) {
                free = iter;
            }
        }
        if (!create || free < 0) {
            return -1;
        }
        dragHistoryWindows[free] = key;
        if (dragPathX[free] == null) {
            dragPathX[free] = new float[PATHLENGTH];
            dragPathY[free] = new float[PATHLENGTH];
            dragPathTime[free] = new long[PATHLENGTH];
        }
        dragPathOffset[free] = 0;
        dragPathLength[free] = 0;
        return free;
    }

    /// Frees a disposed window's drag ring so the slot can be reused.
    private void releaseDragHistory(int windowId) {
        int key = longPressKey(windowId);
        for (int iter = 1; iter < TRACKED_KEY_PRESSES; iter++) {
            if (dragHistoryWindows[iter] == key) {
                dragHistoryWindows[iter] = 0;
                dragPathLength[iter] = 0;
                dragPathOffset[iter] = 0;
                return;
            }
        }
    }

    /// Clears one window's drag history, on press and on disposal.
    private void resetDragHistory(int windowId) {
        int key = longPressKey(windowId);
        for (int iter = 0; iter < TRACKED_KEY_PRESSES; iter++) {
            if (dragHistoryWindows[iter] == key) {
                dragPathLength[iter] = 0;
                dragPathOffset[iter] = 0;
                return;
            }
        }
    }

    boolean isRecursivePointerRelease() {
        return recursivePointerReleaseB;
    }

    private int[] readArrayStackArgument(int[] stack, int offset) {
        int length = stack[offset];
        int[] a = new int[length];
        offset++;
        int alen = a.length;
        System.arraycopy(stack, offset + 0, a, 0, alen);
        return a;
    }

    /// Invoked on the EDT to propagate the event
    private int handleEvent(int offset, int[] inputEventStackTmp, int[] pointerMetaTmp) {
        // The window id is packed into the high bits of the type word. Window 0 is
        // the main surface, and for it the packed word is numerically identical to
        // what it always was, so the main path is unchanged.
        int packed = inputEventStackTmp[offset];
        int type = packed & 0xFF;
        int windowId = packed >>> 8;

        // Restore the metadata that arrived with this packet. The port reports it into
        // a single mutable record, and a port that drains a burst of pointer messages
        // overwrites that record several times before any of them is dispatched -- so
        // without this every event in the burst would build its PointerEvent from the
        // last packet's button and device type.
        if (isPointerEvent(type) && offset < pointerMetaTmp.length) {
            impl.selectPointerEventMetadata(pointerMetaTmp[offset]);
        }

        Container f;
        if (windowId == 0) {
            f = getCurrentUpcomingForm(true);
        } else {
            f = Desktop.getInstance().windowById(windowId);
        }

        // might happen when returning from a deinitialized version of Codename One,
        // or when a window was disposed while its events were still in flight
        // A packet already queued when the window was hidden would otherwise be
        // dispatched into an invisible tree -- and a press among them would re-arm
        // the very timers the hide just cancelled. Cancelling at the transition
        // cannot close that race on its own, because these are already in flight.
        boolean hidden = windowId > 0 && f instanceof Window && !((Window) f).isWindowShowing();
        if (f == null || (isUserInputEvent(type) && hidden)
                || (isUserInputEvent(type) && isBlockedByModal(windowId)
                && !completesAcceptedPress(type, windowId, offset, inputEventStackTmp))) {
            // A press that is being filtered must not leave its long-press timer
            // armed. The timer is charged off the event dispatch thread when the
            // press is queued, before modality has had a say, and the event
            // dispatch thread later fires longPointerPress directly without
            // consulting the filter again -- so a context menu could open behind an
            // application modal for a press the component never received.
            if (type == POINTER_PRESSED || type == POINTER_PRESSED_MULTI) {
                cancelLongPress(windowId);
            }
            // The same for the keyboard. keyPressedImpl arms this window's key repeat
            // and long-key timers before modality has had a say, and the paint loop
            // fires keyRepeated and longKeyPress directly without consulting the
            // filter again -- so holding a key could drive a component behind a modal
            // that never received the press. I fixed the pointer half of this and did
            // not check the keyboard half at the time.
            if (type == KEY_PRESSED) {
                cancelKeyRepeat(windowId);
            }
            // NOTE: drain the packet rather than returning offset unchanged. The
            // caller loops while (offset < end), so returning it unchanged spins the
            // EDT forever, and returning a sentinel would drop the rest of the batch
            // -- which may contain main form events.
            return skipEvent(type, offset + 1, inputEventStackTmp);
        }

        // Which window's samples getDragSpeed should report while this packet's
        // handlers run. Saved and restored around the dispatch rather than simply
        // assigned: a listener may call invokeAndBlock, whose nested event loop
        // dispatches another window's packets, and without restoring it the rest of
        // *this* release would read the nested window's drag state.
        final int previousDragHistory = dragHistoryCurrent;
        dragHistoryCurrent = windowId;
        try {

            // no need to synchronize since we are reading only and modifying the stack frame offset
            offset++;

            switch (type) {
                case KEY_PRESSED:
                    // Dispatched to the top level that saw the key go down, not to the
                    // one this packet names -- the same rule the release already
                    // follows. A repeat names whichever window has focus now, so once
                    // focus moves mid-hold the repeats landed in the new window while
                    // the key-up still went to the old one: the new window entered its
                    // pressed state and no release was ever coming for it.
                    Container pressTarget = rememberKeyPress(inputEventStackTmp[offset], f);
                    pressTarget.keyPressed(inputEventStackTmp[offset]);
                    offset++;
                    break;
                case KEY_RELEASED:
                    // pointer release can cycle into invoke and block which will cause this method
                    // to recurse if a pointer will be released while we are in an invoke and block state
                    // this is the case in http://code.google.com/p/codenameone/issues/detail?id=265
                    //make sure the released event is sent to the same Form who got a
                    //pressed event
                    int releasedKey = inputEventStackTmp[offset];
                    Container xf = takeKeyPressTarget(releasedKey);
                    offset++;
                    if (xf != null) {
                        // Delivered to the top level that saw the press, not to the one
                        // the key-up packet names. A desktop window system sends key-up
                        // to whatever is focused now, so releasing a key after focus has
                        // moved reports the new window -- and matching on that dropped
                        // the release, latching the pressed component in the old one.
                        // For the single window case the two are the same top level, so
                        // this is the behaviour that was always there.
                        xf.keyReleased(releasedKey);
                    } else if (multiKeyMode) {
                        // No record of the press: either it arrived before this window
                        // existed or the tracking table was full. Multi key mode has
                        // always delivered these anyway.
                        f.keyReleased(releasedKey);
                    }
                    break;
                case POINTER_PRESSED:
                    if (recursivePointerReleaseA) {
                        recursivePointerReleaseB = true;
                    }
                    setDragOccured(windowId, false);
                    resetDragHistory(windowId);
                    pointerPressedAndNotReleasedOrDragged = true;
                    xArray1[0] = inputEventStackTmp[offset];
                    offset++;
                    yArray1[0] = inputEventStackTmp[offset];
                    offset++;
                    currentPointerEvent = impl.buildPointerEvent(xArray1[0], yArray1[0], false);
                    // Recorded before the dispatch, not after. A pressed callback can
                    // enter a nested loop -- showModal() does -- and the matching
                    // release can be processed inside it; with the record made
                    // afterwards that release saw no accepted press and was
                    // discarded, and the record then landed stale, latching the
                    // component and misrouting the next release.
                    rememberPointerPress(windowId, f);
                    f.pointerPressed(xArray1, yArray1);
                    break;
                case POINTER_PRESSED_MULTI: {
                    if (recursivePointerReleaseA) {
                        recursivePointerReleaseB = true;
                    }
                    setDragOccured(windowId, false);
                    resetDragHistory(windowId);
                    pointerPressedAndNotReleasedOrDragged = true;
                    int[] array1 = readArrayStackArgument(inputEventStackTmp, offset);
                    offset += array1.length + 1;
                    int[] array2 = readArrayStackArgument(inputEventStackTmp, offset);
                    offset += array2.length + 1;
                    currentPointerEvent = impl.buildPointerEvent(array1[0], array2[0], false);
                    // Same ordering as the single-pointer branch above.
                    rememberPointerPress(windowId, f);
                    f.pointerPressed(array1, array2);
                    break;
                }
                case POINTER_RELEASED:
                    recursivePointerReleaseA = true;
                    pointerPressedAndNotReleasedOrDragged = false;

                    // pointer release can cycle into invoke and block which will cause this method
                    // to recurse if a pointer will be released while we are in an invoke and block state
                    // this is the case in http://code.google.com/p/codenameone/issues/detail?id=265
                    Container x = takePointerPressTarget(windowId);

                    // make sure the released event is sent to the same Form that got a
                    // pressed event
                    int releasedX = inputEventStackTmp[offset];
                    offset++;
                    int releasedY = inputEventStackTmp[offset];
                    offset++;
                    if (x == f || f.shouldSendPointerReleaseToOtherForm()) { //NOPMD CompareObjectsWithEquals
                        xArray1[0] = releasedX;
                        yArray1[0] = releasedY;
                        currentPointerEvent = impl.buildPointerEvent(xArray1[0], yArray1[0], false);
                        f.pointerReleased(xArray1, yArray1);
                    }
                    recursivePointerReleaseA = false;
                    recursivePointerReleaseB = false;
                    // The gesture is over, so hand the ring back. Reclaimed here rather
                    // than only on disposal: entries were held for the life of the
                    // window, so a handful of long-lived windows could exhaust the table
                    // and leave a later window unable to record drag state at all.
                    // After the dispatch, since the release handlers read it.
                    //
                    // Unless a newer gesture has already started in this window: a
                    // release handler can enter invokeAndBlock, whose nested loop
                    // dispatches a fresh press, and that press records a target. Freeing
                    // the ring then would strip the replacement gesture of its velocity.
                    if (!hasPointerPressTarget(windowId)) {
                        releaseDragHistory(windowId);
                    }
                    break;
                case POINTER_RELEASED_MULTI:
                    recursivePointerReleaseA = true;
                    pointerPressedAndNotReleasedOrDragged = false;

                    // pointer release can cycle into invoke and block which will cause this method
                    // to recurse if a pointer will be released while we are in an invoke and block state
                    // this is the case in http://code.google.com/p/codenameone/issues/detail?id=265
                    Container xy = takePointerPressTarget(windowId);

                    // make sure the released event is sent to the same Form that got a
                    // pressed event
                    int[] releasedMultiX = readArrayStackArgument(inputEventStackTmp, offset);
                    offset += releasedMultiX.length + 1;
                    int[] releasedMultiY = readArrayStackArgument(inputEventStackTmp, offset);
                    offset += releasedMultiY.length + 1;
                    if (xy == f || f.shouldSendPointerReleaseToOtherForm()) { //NOPMD CompareObjectsWithEquals
                        currentPointerEvent = impl.buildPointerEvent(releasedMultiX[0], releasedMultiY[0], false);
                        f.pointerReleased(releasedMultiX, releasedMultiY);
                    }
                    recursivePointerReleaseA = false;
                    recursivePointerReleaseB = false;
                    // The gesture is over, so hand the ring back. Reclaimed here rather
                    // than only on disposal: entries were held for the life of the
                    // window, so a handful of long-lived windows could exhaust the table
                    // and leave a later window unable to record drag state at all.
                    // After the dispatch, since the release handlers read it.
                    //
                    // Unless a newer gesture has already started in this window: a
                    // release handler can enter invokeAndBlock, whose nested loop
                    // dispatches a fresh press, and that press records a target. Freeing
                    // the ring then would strip the replacement gesture of its velocity.
                    if (!hasPointerPressTarget(windowId)) {
                        releaseDragHistory(windowId);
                    }
                    break;
                case POINTER_DRAGGED: {
                    setDragOccured(windowId, true);
                    int arg1 = inputEventStackTmp[offset];
                    offset++;
                    int arg2 = inputEventStackTmp[offset];
                    offset++;
                    int timestamp = inputEventStackTmp[offset];
                    offset++;
                    updateDragSpeedStatus(windowId, arg1, arg2, timestamp);
                    pointerPressedAndNotReleasedOrDragged = false;
                    xArray1[0] = arg1;
                    yArray1[0] = arg2;
                    currentPointerEvent = impl.buildPointerEvent(arg1, arg2, false);
                    f.pointerDragged(xArray1, yArray1);
                    break;
                }
                case POINTER_DRAGGED_MULTI: {
                    setDragOccured(windowId, true);
                    pointerPressedAndNotReleasedOrDragged = false;
                    int[] array1 = readArrayStackArgument(inputEventStackTmp, offset);
                    offset += array1.length + 1;
                    int[] array2 = readArrayStackArgument(inputEventStackTmp, offset);
                    offset += array2.length + 1;
                    currentPointerEvent = impl.buildPointerEvent(array1[0], array2[0], false);
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
                    updateDragSpeedStatus(windowId, arg1, arg2, timestamp);
                    xArray1[0] = arg1;
                    yArray1[0] = arg2;
                    currentPointerEvent = impl.buildPointerEvent(arg1, arg2, true);
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
                    currentPointerEvent = impl.buildPointerEvent(arg1, arg2, true);
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
                    currentPointerEvent = impl.buildPointerEvent(arg1, arg2, true);
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

        } finally {
            dragHistoryCurrent = previousDragHistory;
        }
    }

    /// Consumes one event's payload without dispatching it, so that a packet aimed at
    /// a window that has gone away does not desynchronise the rest of the batch.
    ///
    /// The lengths here mirror the switch in `#handleEvent(int, int[], int[])` exactly; the
    /// multi touch forms are self describing, each array being a length followed by
    /// that many values.
    ///
    /// #### Parameters
    ///
    /// - `type`: the event type, with the window id already stripped
    ///
    /// - `offset`: the offset just past the type word
    ///
    /// - `stack`: the event stack
    ///
    /// #### Returns
    ///
    /// the offset of the next event
    private int skipEvent(int type, int offset, int[] stack) {
        switch (type) {
            case KEY_PRESSED:
            case KEY_RELEASED:
                return offset + 1;
            case POINTER_PRESSED:
            case POINTER_RELEASED:
            case POINTER_HOVER_RELEASED:
            case POINTER_HOVER_PRESSED:
            case SIZE_CHANGED:
                return offset + 2;
            case POINTER_DRAGGED:
            case POINTER_HOVER:
                return offset + 3;
            case POINTER_PRESSED_MULTI:
            case POINTER_RELEASED_MULTI:
            case POINTER_DRAGGED_MULTI: {
                int len1 = stack[offset];
                offset += len1 + 1;
                int len2 = stack[offset];
                return offset + len2 + 1;
            }
            case HIDE_NOTIFY:
            case SHOW_NOTIFY:
            default:
                return offset;
        }
    }

    /// This method should be invoked by components that broadcast events on the pointerReleased callback.
    /// This method will indicate if a drag occured since the pointer press event, notice that this method will not
    /// behave as expected for multi-touch events.
    ///
    /// #### Returns
    ///
    /// true if a drag has occured since the last pointer pressed
    public boolean hasDragOccured() {
        // The window whose events are being dispatched, for the same reason
        // getDragSpeed uses it: components ask during their own release handling.
        if (dragHistoryCurrent == 0) {
            return dragOccured;
        }
        int slot = dragHistorySlot(dragHistoryCurrent, false);
        return slot >= 0 && dragOccuredPerWindow[slot];
    }

    /// Returns true for a case where the EDT has nothing at all to do
    boolean shouldEDTSleep() {
        Form current = impl.getCurrentForm();
        return ((current == null || (!current.hasAnimations())) &&
                !anyWindowHasAnimations() &&
                (animationQueue == null || animationQueue.isEmpty()) &&
                inputEventStackPointer == 0 &&
                (!impl.hasPendingPaints()) &&
                hasNoSerialCallsPending() && !anyKeyRepeatArmed()
                && !anyLongPressArmed())
                // a minimized main window must not park the EDT while a tool window
                // is still on screen and animating
                || (isMinimized() && !Desktop.getInstance().hasVisibleWindows()
                        && hasNoSerialCallsPending());
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
    /// the initial window size, or `null` to clear a previously stored hint
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

    // ---- desktop windows ---------------------------------------------------------

    /// Windows blocking input, innermost last. A modal window drops input aimed at
    /// anything it blocks; enforcing this here rather than in the ports means
    /// modality behaves identically on every platform, whether or not the platform
    /// implements its own.
    private final ArrayList<Window> modalWindows = new ArrayList<Window>();

    /// Creates the `Graphics` a window paints through and hands it to the
    /// implementation. `Graphics` cannot be constructed outside this package, which
    /// is why this lives here rather than on the window or the implementation --
    /// exactly as `#init(java.lang.Object)` does for the main surface.
    Graphics createWindowGraphics(Window w) {
        Graphics g = new Graphics(impl.getWindowManager().getNativeGraphics(w.getNativePeer()));
        g.paintPeersBehind = impl.paintNativePeersBehind();
        impl.setPaintSurfaceGraphics(w.getPaintSurface(), g);
        return g;
    }

    /// Wakes the event dispatch thread, used when a window becomes visible before
    /// the first form has been shown and the loop would otherwise still be parked.
    void wakeEdt() {
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    void pushModalWindow(Window w) {
        modalWindows.add(w);
        syncNativeModalBlocking();
    }

    void popModalWindow(Window w) {
        modalWindows.remove(w);
        syncNativeModalBlocking();
    }

    /// Tells every native window whether input to it is currently blocked.
    ///
    /// The framework already decides this, in `#isBlockedByModal(int)`, and it is the
    /// only place that can: the answer depends on the whole modal stack, on each
    /// window's scope and on who owns it. A port that tried to derive it from a single
    /// "this window became modal" call has to reinvent nesting and ownership, and gets
    /// them wrong -- releasing an inner modal re-enabled everything the outer one was
    /// still blocking, application modality left the other secondary windows enabled,
    /// and an unowned window modal disabled a main window it never claimed.
    ///
    /// This matters beyond appearances, because a blocked window's own title bar is
    /// outside the framework's input filter: its close button still reaches the
    /// application.
    void syncNativeModalBlocking() {
        WindowManager wm = impl.getWindowManager();
        if (wm == null) {
            return;
        }
        wm.setMainWindowInputEnabled(!isBlockedByModal(0));
        for (Window each : Desktop.getInstance().getWindows()) {
            Object peer = each.getNativePeer();
            if (peer != null) {
                wm.setInputEnabled(peer, !isBlockedByModal(each.getWindowId()));
            }
        }
    }

    /// Cancels every input timer and recorded press for a window that is no longer
    /// reachable, without deregistering it. Called when a window is hidden: it stays
    /// registered, so a repeat armed before it went away would keep firing into a
    /// component tree the user cannot see.
    void windowInputCancelled(Window w) {
        int id = w.getWindowId();
        cancelKeyRepeat(id);
        cancelLongPress(id);
        for (int iter = 0; iter < TRACKED_KEY_PRESSES; iter++) {
            if (keyPressTargets[iter] == w) { //NOPMD CompareObjectsWithEquals
                keyPressTargets[iter] = null;
                keyPressCodes[iter] = 0;
            }
            if (pointerPressTargets[iter] == w) { //NOPMD CompareObjectsWithEquals
                pointerPressTargets[iter] = null;
                pointerPressWindows[iter] = 0;
            }
        }
    }

    void windowDisposed(Window w) {
        modalWindows.remove(w);
        syncNativeModalBlocking();
        for (int iter = 0; iter < TRACKED_KEY_PRESSES; iter++) {
            if (pointerPressTargets[iter] == w) { //NOPMD CompareObjectsWithEquals
                pointerPressTargets[iter] = null;
                pointerPressWindows[iter] = 0;
            }
        }
        for (int iter = 0; iter < TRACKED_KEY_PRESSES; iter++) {
            if (keyPressTargets[iter] == w) { //NOPMD CompareObjectsWithEquals
                keyPressTargets[iter] = null;
                keyPressCodes[iter] = 0;
            }
        }
        cancelKeyRepeat(w.getWindowId());
        cancelLongPress(w.getWindowId());
        releaseDragHistory(w.getWindowId());
    }

    /// Whether input aimed at the given window is currently blocked by a modal.
    ///
    /// Public because the implementation needs it: a wheel gesture is played as four
    /// steps queued on the event dispatch thread, and a listener can show a modal
    /// between the first check and the last step.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created
    ///
    /// #### Returns
    ///
    /// true when input to that window is currently blocked
    public boolean isWindowInputBlocked(int windowId) {
        return isBlockedByModal(windowId);
    }

    /// Indicates whether input aimed at the given window is currently blocked by a
    /// modal window above it.
    /// The drag-region status at a point inside one of the additional native windows,
    /// used by the implementation's drag activation filter.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the window to ask
    ///
    /// - `x`: x in the window's coordinates
    ///
    /// - `y`: y in the window's coordinates
    ///
    /// #### Returns
    ///
    /// the drag region status, or `Component#DRAG_REGION_NOT_DRAGGABLE` when there is
    /// no such window
    public int windowDragRegionStatus(int windowId, int x, int y) {
        Window w = Desktop.getInstance().windowById(windowId);
        return w == null ? Component.DRAG_REGION_NOT_DRAGGABLE : w.getDragRegionStatus(x, y);
    }

    /// The width of one of the additional native windows, or 0 when there is no such
    /// window.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the window to ask
    ///
    /// #### Returns
    ///
    /// the window's width in Codename One coordinates
    public int windowWidth(int windowId) {
        Window w = Desktop.getInstance().windowById(windowId);
        return w == null ? 0 : w.getWidth();
    }

    /// The height of one of the additional native windows, or 0 when there is no such
    /// window.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the window to ask
    ///
    /// #### Returns
    ///
    /// the window's height in Codename One coordinates
    public int windowHeight(int windowId) {
        Window w = Desktop.getInstance().windowById(windowId);
        return w == null ? 0 : w.getHeight();
    }

    private boolean isBlockedByModal(int windowId) {
        // Every registered blocker is consulted rather than only the newest one.
        // Modal windows nest: a window modal opened from inside an application modal
        // blocks only its own owner, and stopping at the top of the stack would let
        // input back into the main form and every unrelated window for as long as the
        // narrower one was up.
        Window self = windowId > 0 ? Desktop.getInstance().windowById(windowId) : null;
        int len = modalWindows.size();
        for (int iter = len - 1; iter >= 0; iter--) {
            Window modal = modalWindows.get(iter);
            if (modal.getWindowId() == windowId) {
                // Never blocked by itself -- but keep looking at the outer blockers.
                // Returning here exempted a modal window from *every* other modal,
                // so an unrelated modal shown while an application modal was up
                // accepted input that application modality is meant to stop.
                continue;
            }
            if (self != null && ownedBy(self, modal)) {
                // A modal opened from inside another is not blocked by the one it was
                // opened from. That is the exemption the self check was reaching for,
                // and it applies to the owner chain rather than to any modal at all.
                continue;
            }
            if (blocks(modal, windowId)) {
                return true;
            }
        }
        return false;
    }

    /// Whether `w` sits inside `candidateOwner`'s ownership chain.
    private static boolean ownedBy(Window w, Window candidateOwner) {
        TopLevelContainer owner = w.getOwnerWindow();
        while (owner instanceof Window) {
            if (owner == candidateOwner) { //NOPMD CompareObjectsWithEquals
                return true;
            }
            owner = ((Window) owner).getOwnerWindow();
        }
        return false;
    }

    /// Whether one modal window blocks input to the window with the given id.
    private boolean blocks(Window modal, int windowId) {
        if (modal.getModalityType() == Window.MODALITY_APPLICATION) {
            return true;
        }
        // window modal: only the owner is blocked
        TopLevelContainer owner = modal.getOwnerWindow();
        if (owner instanceof Window) {
            return ((Window) owner).getWindowId() == windowId;
        }
        if (owner != null) {
            // owned by the main form
            return windowId == 0;
        }
        // No owner at all. Window modality blocks the owning window, and there is
        // none, so it blocks nothing -- treating this as main-form ownership would
        // block the main form on a window that never claimed it.
        return false;
    }

    /// Paints every open window after the main surface. Iterates by index and
    /// re-reads the size because a nested event loop -- a modal dialog, or
    /// invokeAndBlock -- can dispose a window part way through.
    /// Repaints the main form and every open window.
    ///
    /// For work that finishes without knowing which top level is showing its result --
    /// an image that has just decoded, say. Repainting only the current form left that
    /// result invisible in every window until something else happened to dirty one.
    ///
    /// Lives here rather than at the call site so `Desktop` and `Window` are not
    /// referenced from code every application uses: on ParparVM that reference would
    /// keep the whole window implementation alive in binaries that never open one.
    /// `Display` already reaches `Desktop`, so this adds nothing.
    void repaintTopLevels() {
        Form current = getCurrent();
        if (current != null) {
            current.repaint();
        }
        ArrayList<Window> open = Desktop.getInstance().windowList();
        for (int iter = 0; iter < open.size(); iter++) { // NOPMD ForLoopCanBeForeach
            open.get(iter).repaint();
        }
    }

    private void paintOpenWindows() {
        ArrayList<Window> open = Desktop.getInstance().windowList();
        for (int iter = 0; iter < open.size(); iter++) { // NOPMD ForLoopCanBeForeach
            Window w = open.get(iter);
            if (!w.isWindowShowing()) {
                continue;
            }
            Graphics g = w.getWindowGraphics();
            Object peer = w.getNativePeer();
            // The manager as well as the graphics and the peer. A window stays
            // registered until it is disposed, so one can outlive the platform's
            // window manager -- and dereferencing it here throws on the event dispatch
            // thread, which catches the exception, comes straight back round the loop
            // and throws again. That spins forever rather than losing a frame, so the
            // one thing this must not do is assume the manager is still there.
            WindowManager wm = impl.getWindowManager();
            if (g == null || peer == null || wm == null) {
                continue;
            }
            g.setGraphics(wm.getNativeGraphics(peer));
            w.flushRevalidateQueue();
            impl.paintDirtyWindow(w.getPaintSurface(), w.getWidth(), w.getHeight());
            w.repaintAnimations();
            // The window's raster exists from the moment it is shown, so a capture
            // before this point returns a blank frame of the right size. Recording
            // that a cycle completed is what lets a caller wait for real content.
            w.markPainted();
        }
    }

    private boolean anyWindowHasAnimations() {
        ArrayList<Window> open = Desktop.getInstance().windowList();
        for (int iter = 0; iter < open.size(); iter++) { // NOPMD ForLoopCanBeForeach
            Window w = open.get(iter);
            if (w.isWindowShowing() && w.hasAnimations()) {
                return true;
            }
        }
        return false;
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
    /// `Style#UNIT_TYPE_REM`, `Style#UNIT_TYPE_SCREEN_PERCENTAGE`, `Style#UNIT_TYPE_VH`,
    /// `Style#UNIT_TYPE_VW`, `Style#UNIT_TYPE_VMIN`, `Style#UNIT_TYPE_VMAX`
    ///
    /// #### Returns
    ///
    /// The value converted to pixels.
    ///
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
    /// `Style#UNIT_TYPE_REM`, `Style#UNIT_TYPE_SCREEN_PERCENTAGE`, `Style#UNIT_TYPE_VH`,
    /// `Style#UNIT_TYPE_VW`, `Style#UNIT_TYPE_VMIN`, `Style#UNIT_TYPE_VMAX`
    ///
    /// - `horizontal`: Whether screen percentage units should be based on horitonzal or vertical percentage.
    ///
    /// #### Returns
    ///
    /// The value converted to pixels.
    ///
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
    /// VirtualKeyboard
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
        // The window whose events are being dispatched. Components call this from
        // their own pointerReleased, so "the window currently being serviced" is the
        // one that owns the samples they mean.
        int readSlot = dragHistorySlot(dragHistoryCurrent, false);
        if (readSlot < 0) {
            return 0;
        }
        if (yAxis) {
            speed = impl.getDragSpeed(dragPathY[readSlot], dragPathTime[readSlot],
                    dragPathOffset[readSlot], dragPathLength[readSlot]);
        } else {
            speed = impl.getDragSpeed(dragPathX[readSlot], dragPathTime[readSlot],
                    dragPathOffset[readSlot], dragPathLength[readSlot]);
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
    /// disable it
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
    /// Returns a snapshot of recent platform log output (e.g. logcat
    /// tail on Android). Used by [com.codename1.crash.CrashProtection]
    /// to attach device-log context to a crash report. Returns `null`
    /// on platforms that have no readable process log (`javase`,
    /// `javascript`).
    public String getNativeLogSnapshot() {
        return impl.getNativeLogSnapshot();
    }

    /// Installs the platform native crash handler used by crash
    /// protection. On platforms where a native crash (a signal, an
    /// uncaught Objective-C exception, a segfault in JNI code) cannot
    /// reach the JVM error path, the handler writes a structured
    /// record to disk before the process dies. The record is replayed
    /// on the next launch via [#consumePendingNativeCrash()].
    /// Idempotent.
    public void installNativeCrashHandler() {
        impl.installNativeCrashHandler();
    }

    /// Returns the captured native crash evidence (raw backtrace +
    /// signal info as a text blob) from [#installNativeCrashHandler()],
    /// or `null` if none. The implementation deletes the underlying
    /// record before returning so the same crash isn't replayed on
    /// subsequent launches. Crash protection wraps the returned blob
    /// in a synthetic report payload.
    public String consumePendingNativeCrash() {
        return impl.consumePendingNativeCrash();
    }

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

    /// Heuristic test for URL-shaped strings. Accepts anything containing
    /// `://` or a `scheme:` prefix; falls through for `AppArg` payloads that
    /// happen to be non-URL data.
    private static boolean looksLikeUrl(String v) {
        if (v == null) {
            return false;
        }
        if (v.indexOf("://") >= 0) {
            return true;
        }
        int colon = v.indexOf(':');
        if (colon <= 0) {
            return false;
        }
        for (int i = 0; i < colon; i++) {
            char c = v.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '+' || c == '-' || c == '.')) {
                return false;
            }
        }
        return true;
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
            // Every CN1 port (iOS cn1OpenURL / cn1ContinueUserActivity, Android
            // onNewIntent, JS URL navigation) already pipes deep links through
            // setProperty("AppArg", url). Treat URL-shaped values as deep links
            // and route them through the build-time-generated dispatcher; other
            // AppArg payloads (free-form launch data) are untouched.
            if (value != null && value.length() > 0 && looksLikeUrl(value)) {
                com.codename1.router.Navigation.dispatchExternalUrl(value);
            }
            return;
        }
        if ("blockOverdraw".equals(key)) {
            Container.setBlockOverdraw(true);
            return;
        }
        if ("blockCopyPaste".equals(key)) {
            impl.blockCopyPaste("true".equals(value));
        }
        if ("DisableScreenshots".equals(key)) {
            disableScreenshots = false;
            if (impl == null) {
                disableScreenshots = "true".equalsIgnoreCase(value);
                return;
            }
            impl.setDisableScreenshots("true".equalsIgnoreCase(value));
        }
        if ("TapjackingProtection".equals(key)) {
            com.codename1.security.TapjackingPolicy policy = parseTapjackingPolicy(value);
            if (impl == null) {
                pendingTapjackingPolicy = policy;
                return;
            }
            impl.setTapjackingProtection(policy);
        }
        if ("HideOverlayWindows".equals(key)) {
            boolean hide = "true".equalsIgnoreCase(value);
            if (impl == null) {
                pendingHideOverlayWindows = hide;
                return;
            }
            impl.setHideOverlayWindows(hide);
        }
        if ("Component.revalidateOnStyleChange".equals(key)) {
            Component.setRevalidateOnStyleChange("true".equalsIgnoreCase(value));
        }
        if ("db.legacy".equals(key)) {
            // Keep the static flag and the property in step, so setting this through the
            // generated application stub, through CN.setProperty or through
            // Database.setLegacyBehavior all behave identically.
            Database.setLegacyBehavior("true".equalsIgnoreCase(value));
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

    /// Maps the `TapjackingProtection` property value onto the enum. The value reaches us from a
    /// build hint, so an unrecognised one is a project configuration mistake rather than something
    /// the app can handle: fall back to the safest useful reading, which is BLOCK, because the
    /// property is only ever set when the developer asked for protection in the first place.
    private static com.codename1.security.TapjackingPolicy parseTapjackingPolicy(String value) {
        if (value == null) {
            return com.codename1.security.TapjackingPolicy.OFF;
        }
        String v = value.trim();
        if ("off".equalsIgnoreCase(v)) {
            return com.codename1.security.TapjackingPolicy.OFF;
        }
        if ("report".equalsIgnoreCase(v)) {
            return com.codename1.security.TapjackingPolicy.REPORT;
        }
        if ("strict".equalsIgnoreCase(v)) {
            return com.codename1.security.TapjackingPolicy.STRICT;
        }
        return com.codename1.security.TapjackingPolicy.BLOCK;
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

    /// Executes the given URL on the native platform.
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
    /// On the JavaSE simulator this method also serves as the cross-platform
    /// entry point for the simulator hook system. The simulator scans cn1libs
    /// (and the running app) for `META-INF/codenameone/simulator-hooks.properties`
    /// files, and a URL of the form `namespace:itemN` that matches a registered
    /// hook is intercepted and dispatched on the CN1 EDT instead of being
    /// handed to the native URL opener. On Android, iOS, JavaScript and other
    /// production targets no hooks are ever registered, so a hook-style URL
    /// falls through to the normal native execute and (almost always) becomes
    /// a no-op. CN1 UnitTests running cross-platform should guard with
    /// [#canExecute(String)] before invoking a hook URL:
    ///
    /// ```java
    /// if (Boolean.TRUE.equals(Display.getInstance().canExecute("bluetooth:item1"))) {
    ///     Display.getInstance().execute("bluetooth:item1"); // toggle the simulated adapter
    /// }
    /// ```
    ///
    /// See the developer guide's "Creating CN1Libs" chapter for the
    /// `simulator-hooks.properties` format and the positional `itemN` / `labelN`
    /// conventions.
    ///
    /// #### JavaScript port
    ///
    /// Browsers only let a page open a new window/tab from inside a live user
    /// gesture, and Codename One dispatches events on its own EDT so by the time
    /// your listener calls this method the browser no longer considers a gesture
    /// to be in progress. The JavaScript port therefore resolves the
    /// `javascript.execute.target` property to decide what to do:
    ///
    /// - `auto` (the default) opens a new tab when the page still has user
    ///   activation and otherwise navigates the page the app is running in. No
    ///   confirmation prompt is ever shown, but note that navigating the current
    ///   page unloads the app.
    /// - `_blank` only ever opens a new tab. When the browser would block it the
    ///   port shows a confirmation `Sheet` whose OK button supplies the missing
    ///   gesture. This was the behavior before the property existed.
    /// - `_self` always navigates the page the app is running in.
    ///
    /// Set it before the call, for example in your `init` method:
    ///
    /// ```java
    /// Display.getInstance().setProperty("javascript.execute.target", "_self");
    /// ```
    ///
    /// The property applies to any URL carrying a URI scheme the browser can
    /// hand off, custom deep links like the `imdb:///find` example above
    /// included. It is ignored on every other platform, and on all targets a
    /// `javascript:` URL, a `data:` URL, a `file:` URL or a path into local
    /// storage keeps its existing meaning.
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
    /// to the application
    public void execute(String url, ActionListener response) {
        impl.execute(url, response);
    }

    /// Offers the given in-memory bytes to the user as a downloadable file,
    /// bypassing local storage. This exists for platforms (currently the
    /// JavaScript port) where the storage-backed {@link #execute(String)}
    /// download path is unavailable. Returns {@code true} if the platform
    /// handled the download, {@code false} if unsupported (callers should then
    /// fall back to writing the file and calling {@link #execute(String)}).
    ///
    /// #### Parameters
    ///
    /// - `fileName`: the suggested file name for the download
    ///
    /// - `bytes`: the file contents
    public boolean downloadBytesAsFile(String fileName, byte[] bytes) {
        return impl.downloadBytesAsFile(fileName, bytes);
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
    /// common constants in this class or be a user/implementation defined sound
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
    /// to assume that wav/mp3 would be supported.
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

    /// Indicates whether this platform provides a native low latency sound pool
    /// backing `com.codename1.gaming.SoundPool`. When false the gaming layer uses a
    /// `com.codename1.media.MediaManager` based fallback.
    public boolean isSoundPoolSupported() {
        return impl.isSoundPoolSupported();
    }

    /// Creates a native low latency sound pool peer for `com.codename1.gaming.SoundPool`,
    /// or returns null when this platform has no native backend.
    ///
    /// #### Parameters
    ///
    /// - `maxStreams`: the maximum number of simultaneously playing voices
    public com.codename1.media.SoundPoolPeer createSoundPool(int maxStreams) {
        return impl.createSoundPool(maxStreams);
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
    /// image objects be used to copy
    public void copyToClipboard(Object obj) {
        impl.copyToClipboard(obj);
    }

    /// Copies a set of alternative clipboard representations. The first entry should normally be
    /// `text/plain`; richer consumers can negotiate HTML, RTF, Markdown, AsciiDoc, or custom MIME data.
    public void copyToClipboard(ClipboardContent content) {
        impl.copyToClipboard(content);
    }

    /// Returns the current content of the clipboard
    ///
    /// #### Returns
    ///
    /// can be any object or null see copyToClipboard
    public Object getPasteDataFromClipboard() {
        return impl.getPasteDataFromClipboard();
    }

    /// Returns all clipboard representations exposed by the current port, or null if none are available.
    public ClipboardContent getClipboardContent() {
        return impl.getClipboardContent();
    }

    /// Returns true if the device is currently in portrait mode
    ///
    /// #### Returns
    ///
    /// true if the device is in portrait mode
    public boolean isPortrait() {
        return impl.isPortrait();
    }


    /// Returns true if orientation was locked using #lockOrientation(boolean) and not yet unlocked via #unlockOrientation().
    ///
    /// #### Returns
    ///
    /// true if orientation is currently marked as locked
    public boolean isLockOrientation() {
        return lockOrientation;
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
        lockOrientation = true;
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
        lockOrientation = false;
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

    /// Indicates whether the application is running on a smartwatch form factor
    /// (Apple Watch / Wear OS). Notice that this is often a guess derived from
    /// the device metadata.
    ///
    /// #### Returns
    ///
    /// true if the device is assumed to be a smartwatch
    public boolean isWatch() {
        return impl.isWatch();
    }

    /// Indicates whether the application is running on a television form factor
    /// (Apple TV / Android TV / Google TV). Notice that this is often a guess
    /// derived from the device metadata.
    ///
    /// #### Returns
    ///
    /// true if the device is assumed to be a TV
    public boolean isTV() {
        return impl.isTV();
    }

    /// Indicates whether a head unit (Apple CarPlay / Google Android Auto) is currently connected and
    /// projecting the `com.codename1.car` experience. See `com.codename1.car.Car#isCarConnected()`.
    ///
    /// #### Returns
    ///
    /// true if a car is connected
    public boolean isCarConnected() {
        return impl.isCarConnected();
    }

    /// True if the device is a foldable or dual screen device such as a Galaxy Fold, Galaxy Flip,
    /// Pixel Fold or Surface Duo.
    ///
    /// #### Returns
    ///
    /// true if the device is foldable
    public boolean isFoldable() {
        return impl.isFoldable();
    }

    /// Returns the live device fold posture. See `com.codename1.ui.DevicePosture` for details.
    ///
    /// #### Returns
    ///
    /// the device posture, never null
    public DevicePosture getDevicePosture() {
        return DevicePosture.getInstance();
    }

    /// Adds a listener that is notified when the device is folded, unfolded or changes posture. The
    /// delivered `com.codename1.ui.events.ActionEvent` has the type `PostureChange`; query the new
    /// posture from `com.codename1.ui.DevicePosture#getInstance()`.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to add
    public void addPostureListener(ActionListener l) {
        if (postureListeners == null) {
            postureListeners = new EventDispatcher();
        }
        postureListeners.addListener(l);
    }

    /// Removes a posture listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to remove
    public void removePostureListener(ActionListener l) {
        if (postureListeners != null) {
            postureListeners.removeListener(l);
        }
    }

    /// Invoked by the implementation when the device fold posture changes. Fires the registered
    /// posture listeners on the EDT.
    public void postureChanged() {
        if (postureListeners != null && postureListeners.hasListeners()) {
            callSerially(new Runnable() {
                @Override
                public void run() {
                    postureListeners.fireActionEvent(new ActionEvent(DevicePosture.getInstance(),
                            ActionEvent.Type.PostureChange));
                }
            });
        }
    }

    /// True if the application is currently running in a desktop windowing mode such as Samsung DeX,
    /// Android desktop windowing or iPad Stage Manager. This is distinct from `#isDesktop()` which
    /// reports a genuine desktop platform (Windows, macOS or Linux).
    ///
    /// #### Returns
    ///
    /// true if running in a desktop windowing mode
    public boolean isDesktopMode() {
        return impl.isDesktopMode();
    }

    /// Returns the number of displays (monitors or external screens) currently attached.
    ///
    /// #### Returns
    ///
    /// the number of attached displays, at least 1
    public int getDisplayCount() {
        return impl.getDisplayCount();
    }

    /// True if an external or secondary display is currently attached.
    ///
    /// #### Returns
    ///
    /// true if an external display is connected
    public boolean isExternalDisplayConnected() {
        return impl.isExternalDisplayConnected();
    }

    /// Returns the platform bridge used by the `com.codename1.car` API to render in-car templates, or
    /// null when in-car projection is unsupported on this port. Internal -- application code uses the
    /// `com.codename1.car` API rather than this bridge directly.
    ///
    /// #### Returns
    ///
    /// the car bridge, or null
    public com.codename1.car.spi.CarBridge getCarBridge() {
        return impl.getCarBridge();
    }

    /// Returns the platform bridge used by the `com.codename1.wearable` API to talk to the
    /// counterpart watch or phone app, or null when this device has no wearable counterpart.
    /// Internal -- application code uses the `com.codename1.wearable` API rather than this bridge
    /// directly.
    ///
    /// #### Returns
    ///
    /// the wearable bridge, or null
    public com.codename1.wearable.spi.WearableBridge getWearableBridge() {
        return impl.getWearableBridge();
    }

    /// Returns the platform bridge used by the `com.codename1.home` API to reach HomeKit, the
    /// Google Home APIs or a local simulated home, or null when this port has no smart-home
    /// support. Internal -- application code uses `com.codename1.home.SmartHome` rather than this
    /// bridge directly.
    ///
    /// #### Returns
    ///
    /// the smart-home bridge, or null
    public com.codename1.home.spi.HomeBridge getHomeBridge() {
        return impl.getHomeBridge();
    }

    /// Returns the platform bridge used by the `com.codename1.surfaces` API to render external
    /// surfaces (home-screen widgets and live activities), or null when unsupported on this port.
    /// Internal -- application code uses the `com.codename1.surfaces` API rather than this bridge
    /// directly.
    ///
    /// #### Returns
    ///
    /// the surface bridge, or null
    public com.codename1.surfaces.spi.SurfaceBridge getSurfaceBridge() {
        return impl.getSurfaceBridge();
    }

    /// Returns the platform bridge used by the `com.codename1.intents` API to expose the
    /// application's capabilities to the system, or null when unsupported on this port. Internal --
    /// application code uses the `com.codename1.intents` API rather than this bridge directly.
    ///
    /// #### Returns
    ///
    /// the intent bridge, or null
    public com.codename1.intents.spi.IntentBridge getIntentBridge() {
        return impl.getIntentBridge();
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

    /// Returns the platform motion sensor entry point or {@code null} when the
    /// current port does not provide motion sensors. Prefer
    /// {@link com.codename1.sensors.MotionSensorManager#getInstance()} in
    /// application code --- it handles the fallback to a no-op manager when the
    /// current port returns {@code null}.
    public com.codename1.sensors.MotionSensorManager getMotionSensorManager() {
        return impl.getMotionSensorManager();
    }

    /// Returns the platform biometric authentication entry point. Prefer
    /// {@link com.codename1.security.Biometrics#getInstance()} in application
    /// code --- it handles the fallback to a no-op stub when the current port
    /// does not implement biometrics.
    public Biometrics getBiometrics() {
        return impl.getBiometrics();
    }

    /// Returns the platform biometric-gated secure storage. Prefer
    /// {@link com.codename1.security.SecureStorage#getInstance()} in
    /// application code.
    public SecureStorage getSecureStorage() {
        return impl.getSecureStorage();
    }

    /// Returns the platform NFC entry point. Prefer
    /// {@link com.codename1.nfc.Nfc#getInstance()} in application code ---
    /// it handles the fallback to a no-op stub when the current port does
    /// not implement NFC.
    public com.codename1.nfc.Nfc getNfc() {
        return impl.getNfc();
    }

    /// Returns the active port's local device-calendar source. Applications
    /// should normally use
    /// {@link com.codename1.calendar.LocalCalendarSource#getInstance()}.
    public com.codename1.calendar.LocalCalendarSource getLocalCalendarSource() {
        return impl.getLocalCalendarSource();
    }

    /// Returns the platform Bluetooth entry point. Prefer
    /// {@link com.codename1.bluetooth.Bluetooth#getInstance()} in
    /// application code --- it handles the fallback to a no-op stub when
    /// the current port does not implement Bluetooth.
    public com.codename1.bluetooth.Bluetooth getBluetooth() {
        return impl.getBluetooth();
    }

    /// Returns the platform health entry point. Prefer
    /// {@link com.codename1.health.Health#getInstance()} in application
    /// code --- it handles the fallback to a no-op stub when the current
    /// port does not implement health data.
    public com.codename1.health.Health getHealth() {
        return impl.getHealth();
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
    /// of the ActionEvent sent this callback will be a String[].  For other types, it will be a String.  If the dialog was cancelled, it will be null.
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

    /// Opens a file chooser for arbitrary user-selected files.
    ///
    /// The callback source is a `String` path that can be read with
    /// `FileSystemStorage.openInputStream()`, or `null` if the user cancelled.
    /// The `accept` argument is a comma-separated list of file extensions
    /// (`"pdf,txt"`, `"p8"`) or MIME types (`"application/pdf"`). Platforms with
    /// native document pickers use them; other ports fall back to a Codename One
    /// file tree.
    ///
    /// Unlike `openGallery()`, this API is not for media-library access and does
    /// not add photo/music build hints.
    ///
    /// #### Parameters
    ///
    /// - `response`: callback receiving the selected file path
    /// - `accept`: comma-separated extensions or MIME types, or `null` for all files
    public void openFileChooser(ActionListener response, String accept) {
        impl.openFileChooser(response, accept);
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

    /// Indicates whether this platform can attempt to detect active phone-call interruptions.
    ///
    /// A `true` result means the platform provides a best-effort heuristic only.
    /// It does **not** guarantee exact telephony state.
    ///
    /// #### Returns
    ///
    /// `true` if call detection is implemented on this platform.
    public boolean isCallDetectionSupported() {
        return impl.isCallDetectionSupported();
    }

    /// Best-effort check for whether the platform currently believes an active phone call
    /// is interrupting the app.
    ///
    /// This API is intentionally heuristic. It can produce false positives
    /// (e.g. non-call interruptions like Control Center or app-switching) and false negatives.
    /// Use it for UX hints and telemetry, not as a security or business-critical gate.
    ///
    /// #### Returns
    ///
    /// `true` if the platform currently believes a call interruption is active.
    public boolean isInCall() {
        return impl.isInCall();
    }

    /// Indicates the level of SMS support in the platform as one of:
    /// `#SMS_NOT_SUPPORTED` (for desktop, tablet etc.),
    /// `#SMS_SEAMLESS` (no UI interaction), `#SMS_INTERACTIVE` (with compose UI),
    /// `#SMS_BOTH`.
    ///
    /// The sample below demonstrates the use case for this property:
    ///
    /// ```java
    /// void sendMessage(String phone, String data) {
    ///     switch(Display.getInstance().getSMSSupport()) {
    ///     case Display.SMS_NOT_SUPPORTED:
    ///         return;
    ///     case Display.SMS_SEAMLESS:
    ///         showUIDialogToEditMessageData();
    ///         Display.getInstance().sendSMS(phone, data);
    ///         return;
    ///     default:
    ///         Display.getInstance().sendSMS(phone, data);
    ///         return;
    ///     }
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
    /// void sendMessage(String phone, String data) {
    ///     switch(Display.getInstance().getSMSSupport()) {
    ///     case Display.SMS_NOT_SUPPORTED:
    ///         return;
    ///     case Display.SMS_SEAMLESS:
    ///         showUIDialogToEditMessageData();
    ///         Display.getInstance().sendSMS(phone, data);
    ///         return;
    ///     default:
    ///         Display.getInstance().sendSMS(phone, data);
    ///         return;
    ///     }
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

    /// Indicates whether the platform exposes a native in-app review/rating
    /// prompt (the OS-sanctioned "rate this app" sheet). When false the
    /// [com.codename1.appreview.AppReview] API falls back to a Codename One
    /// drawn rating widget.
    ///
    /// #### Returns
    ///
    /// true if the platform can present a native review prompt.
    public boolean isNativeInAppReviewSupported() {
        return impl.isNativeInAppReviewSupported();
    }

    /// Requests the native in-app review prompt. Should only be invoked when
    /// [#isNativeInAppReviewSupported] returns true. The platforms hide whether
    /// the user actually rated and may throttle the prompt; `done` reports
    /// whether the request reached the native review controller.
    ///
    /// #### Parameters
    ///
    /// - `done`: invoked with `true` once the native prompt was requested or
    ///   `false` when the platform did not handle it. May be null.
    public void requestNativeInAppReview(SuccessCallback<Boolean> done) {
        impl.requestNativeInAppReview(done);
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
    /// Since 7.0, you can share files using using the file path in the text parameter.  The file must exist in file system storage, and
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
    /// some platforms to provide a hint as to where the share dialog overlay should pop up.  Particularly,
    /// on the iPad with iOS 8 and higher.
    public void share(String textOrPath, String image, String mimeType, Rectangle sourceRect) {
        share(textOrPath, image, mimeType, sourceRect, null);
    }

    /// Like [#share(String,String,String,Rectangle)] but reports the
    /// outcome through `listener` on the EDT.
    ///
    /// `listener` may be `null`. If the underlying platform cannot report
    /// the outcome (older Android, Web Share API), the listener is still
    /// invoked with [ShareResult#sharedTo] passing a `null` package name
    /// so the app can resume its flow.
    ///
    /// #### Parameters
    ///
    /// - `textOrPath`: String to share, or path to file to share.
    ///
    /// - `image`: file path to the image or null
    ///
    /// - `mimeType`: type of the image or file. null if just sharing text
    ///
    /// - `sourceRect`: source rectangle hint for the share popover. May be null.
    ///
    /// - `listener`: callback for the share outcome. May be null.
    public void share(String textOrPath, String image, String mimeType, Rectangle sourceRect, ShareResultListener listener) {
        // Analytics auto-instrumentation: this 5-arg overload is the chokepoint
        // that every share(...) variant funnels into. The autoEvent path is
        // consent-gated, a no-op when no provider is registered, and never
        // throws into the caller. "type" is the coarse share content type.
        Map<String, Object> shareParams = new HashMap<String, Object>();
        shareParams.put("type", image != null ? "image" : "text");
        Analytics.autoEvent("share", "engagement", shareParams);
        if (listener == null) {
            impl.share(textOrPath, image, mimeType, sourceRect);
            return;
        }
        final ShareResultListener finalListener = listener;
        impl.share(textOrPath, image, mimeType, sourceRect, new ShareResultListener() {
            @Override
            public void onResult(final ShareResult result) {
                final ShareResult r = result != null ? result : ShareResult.sharedTo(null);
                callSerially(new Runnable() {
                    @Override
                    public void run() {
                        finalListener.onResult(r);
                    }
                });
            }
        });
    }

    /// Indicates if the underlying platform can print documents through
    /// [#print(String,String,PrintResultListener)].
    ///
    /// #### Returns
    ///
    /// true if the underlying platform handles printing.
    public boolean isPrintingSupported() {
        return impl.isPrintingSupported();
    }

    /// Print a document file through the platform printing system,
    /// typically showing the native print dialog where the user picks a
    /// printer and options. The outcome is reported through `listener` on
    /// the EDT.
    ///
    /// All printing platforms accept PDF (`application/pdf`) and common
    /// image types (`image/png`, `image/jpeg`); other mime types fail with
    /// [PrintResult#STATUS_FAILED] on platforms that can't render them.
    /// See [com.codename1.printing.Printer] for a friendlier facade.
    ///
    /// #### Parameters
    ///
    /// - `filePath`: path of the document in [com.codename1.io.FileSystemStorage]
    ///
    /// - `mimeType`: the document type, e.g. `application/pdf`, `image/png`
    ///
    /// - `listener`: callback for the print outcome. May be null.
    public void print(String filePath, String mimeType, PrintResultListener listener) {
        if (listener == null) {
            impl.print(filePath, mimeType, null);
            return;
        }
        final PrintResultListener finalListener = listener;
        impl.print(filePath, mimeType, new PrintResultListener() {
            @Override
            public void onResult(final PrintResult result) {
                final PrintResult r = result != null ? result : PrintResult.completed();
                callSerially(new Runnable() {
                    @Override
                    public void run() {
                        finalListener.onResult(r);
                    }
                });
            }
        });
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
    /// to provide push like functionality. If this flag is set to true no polling will occur and
    /// the error PushCallback.REGISTRATION_ERROR_SERVICE_NOT_AVAILABLE will be sent to the push interface.
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
    /// a push id is necessary,
    ///
    /// - `noFallback`: @param noFallback some devices don't support an efficient push API and will resort to polling
    /// to provide push like functionality. If this flag is set to true no polling will occur and
    /// the error PushCallback.REGISTRATION_ERROR_SERVICE_NOT_AVAILABLE will be sent to the push interface.
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
    /// not exists it will be created.
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
    /// not exists it will be created.
    ///
    /// - `mimeType`: @param mimeType the output mime type that is supported see
    /// getAvailableRecordingMimeTypes()
    public Media createMediaRecorder(String path, String mimeType) throws IOException {
        return impl.createMediaRecorder(path, mimeType);
    }

    /// Whether [com.codename1.media.SpeechRecognizer] is implemented
    /// on the current platform. The user may still deny mic / speech
    /// permission at call time even when this returns true.
    public boolean isSpeechRecognitionSupported() {
        return impl.speechRecognitionIsSupported();
    }

    /// Begins a speech-recognition session. See
    /// [com.codename1.media.SpeechRecognizer#recognize] for the
    /// callable surface; this hook is the direct delegation point
    /// that platform ports override.
    public void startSpeechRecognition(com.codename1.media.RecognitionOptions options,
                                       com.codename1.media.RecognitionCallback callback) {
        impl.startSpeechRecognition(options, callback);
    }

    public void stopSpeechRecognition() {
        impl.stopSpeechRecognition();
    }

    /// Whether [com.codename1.media.TextToSpeech] is implemented on
    /// the current platform.
    public boolean isTextToSpeechSupported() {
        return impl.textToSpeechIsSupported();
    }

    public void textToSpeechSpeak(String text, com.codename1.media.TtsOptions options) {
        impl.textToSpeechSpeak(text, options);
    }

    public void textToSpeechStop() {
        impl.textToSpeechStop();
    }

    public String[] textToSpeechAvailableVoices() {
        return impl.textToSpeechAvailableVoices();
    }

    /// Returns the image IO instance that allows scaling image files.
    ///
    /// #### Returns
    ///
    /// the image IO instance or null if image IO isn't supported for the given platform
    public ImageIO getImageIO() {
        return impl.getImageIO();
    }

    /// Returns the video IO instance for video encoding and frame accurate decoding, or
    /// null if video IO isn't supported on the given platform. See
    /// `com.codename1.media.VideoIO`.
    ///
    /// #### Returns
    ///
    /// the video IO instance or null if unsupported
    public VideoIO getVideoIO() {
        return impl.getVideoIO();
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

    /// Opens an encrypted database or creates one if it does not exist.
    ///
    /// Prefer `com.codename1.db.Database#openOrCreate(java.lang.String, com.codename1.db.DatabaseConfig)`,
    /// which validates the name and the platform's capability before delegating here.
    ///
    /// #### Parameters
    ///
    /// - `databaseName`: the name of the database
    ///
    /// - `config`: how the database should be keyed
    ///
    /// #### Returns
    ///
    /// the open database
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the database cannot be opened, created or decrypted
    public Database openOrCreate(String databaseName, DatabaseConfig config) throws IOException {
        return impl.openOrCreateDB(databaseName, config);
    }

    /// Indicates whether this platform can open encrypted databases.
    ///
    /// #### Returns
    ///
    /// true if encrypted databases are supported
    public boolean isDatabaseEncryptionSupported() {
        return impl.isDatabaseEncryptionSupported();
    }

    /// Opens a plaintext database through an engine able to encrypt it in place.
    ///
    /// #### Parameters
    ///
    /// - `databaseName`: the name of the database
    ///
    /// #### Returns
    ///
    /// the open database
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the database cannot be opened
    public Database openOrCreateForRekey(String databaseName) throws IOException {
        return impl.openOrCreateDBForRekey(databaseName);
    }

    /// Indicates whether managed database keys are held in hardware backed storage here.
    ///
    /// #### Returns
    ///
    /// true when a hardware backed key store protects managed keys
    public boolean isDatabaseManagedKeyHardwareBacked() {
        return impl.isDatabaseManagedKeyHardwareBacked();
    }

    /// Reports whether a database is encrypted, when the platform can tell without reading the
    /// file itself.
    ///
    /// #### Parameters
    ///
    /// - `databaseName`: the name of the database
    ///
    /// #### Returns
    ///
    /// one of the `CodenameOneImplementation` DATABASE_ENCRYPT* constants
    /// The identity a managed database key with no explicit alias is stored under.
    ///
    /// See `com.codename1.impl.CodenameOneImplementation#databaseManagedKeyIdentity(String)`.
    ///
    /// #### Parameters
    ///
    /// - `databaseName`: the name the application opens the database under
    ///
    /// #### Returns
    ///
    /// the identity, which is the name itself unless the port resolves it
    public String databaseManagedKeyIdentity(String databaseName) {
        return impl.databaseManagedKeyIdentity(databaseName);
    }

    /// See `com.codename1.impl.CodenameOneImplementation#databaseRegistryIdentity(String)`.
    ///
    /// #### Parameters
    ///
    /// - `databaseName`: the database, as an application named it
    ///
    /// #### Returns
    ///
    /// the key it is registered under
    public String databaseRegistryIdentity(String databaseName) {
        return impl.databaseRegistryIdentity(databaseName);
    }

    /// See `com.codename1.impl.CodenameOneImplementation#isRelativeAttachmentNameResolvable()`.
    ///
    /// #### Returns
    ///
    /// whether a relative attachment name resolves to the database this port would open
    public boolean isRelativeAttachmentNameResolvable() {
        return impl.isRelativeAttachmentNameResolvable();
    }

    /// See `com.codename1.impl.CodenameOneImplementation#databaseIdentityForEngineFile(String)`.
    ///
    /// #### Parameters
    ///
    /// - `engineFile`: the filename the engine reported
    ///
    /// #### Returns
    ///
    /// the registry identity for it
    public String databaseIdentityForEngineFile(String engineFile) {
        return impl.databaseIdentityForEngineFile(engineFile);
    }

    /// See `com.codename1.impl.CodenameOneImplementation#openDatabaseConnections(String)`.
    ///
    /// #### Parameters
    ///
    /// - `databaseName`: the name or path being deleted
    ///
    /// #### Returns
    ///
    /// the number of connections the port has open on it
    public int openDatabaseConnections(String databaseName) {
        return impl.openDatabaseConnections(databaseName);
    }

    public int isDatabaseFileEncrypted(String databaseName) {
        return impl.isDatabaseFileEncrypted(databaseName);
    }

    /// Indicates whether `byte[]` values may be used as query parameters.
    ///
    /// #### Returns
    ///
    /// true if blobs are accepted as query parameters
    public boolean isBlobQueryParameterSupported() {
        return impl.isBlobQueryParameterSupported();
    }

    /// Indicates whether this platform accepts a file path as a database name.
    ///
    /// #### Returns
    ///
    /// true if custom database paths are supported
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
    /// elements e.g. `mydatabase.db`
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
    public boolean isScrollWheeling() {
        return impl.isScrollWheeling();
    }

    /// If isScreenSaverDisableSupported() returns true calling this method will
    /// lock the screen display on
    ///
    /// #### Parameters
    ///
    /// - `e`: @param e when set to true the screen saver will work as usual and when set to false the screen
    /// will not turn off automatically
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

    /// Creates a fresh per-session backend for the low-level
    /// `com.codename1.camera.Camera` API. Returns `null` on platforms that do
    /// not implement the new API. Application code should use `Camera.open(...)`
    /// rather than calling this directly.
    ///
    /// @hidden
    public com.codename1.impl.CameraImpl getCameraBackend() {
        return impl.createCameraImpl();
    }

    /// Creates a fresh per-session backend for the `com.codename1.ar.AR`
    /// augmented reality API. Returns `null` on platforms without AR support.
    /// Application code should use `AR.open(...)` rather than calling this
    /// directly.
    ///
    /// @hidden
    public com.codename1.impl.ARImpl getARBackend() {
        return impl.createARImpl();
    }

    /// Creates a fresh backend for `com.codename1.ai.vision`.
    ///
    /// @hidden
    public com.codename1.impl.VisionImpl getVisionBackend() {
        return impl.createVisionImpl();
    }

    /// Creates a fresh backend for `com.codename1.ai.inference`.
    ///
    /// @hidden
    public com.codename1.impl.InferenceImpl getInferenceBackend() {
        return impl.createInferenceImpl();
    }

    /// Creates a fresh backend for `com.codename1.ai.language`.
    ///
    /// @hidden
    public com.codename1.impl.LanguageImpl getLanguageBackend() {
        return impl.createLanguageImpl();
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
    /// component if applicable
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
    /// REPEAT_HALF_HOUR, REPEAT_HOUR, REPEAT_DAY, REPEAT_WEEK
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

    /// Requests permission to post notifications using a default request (alert, sound and
    /// badge). The result is delivered to the callback on the EDT. On platforms without a
    /// notification permission model the callback reports the permission as granted.
    ///
    /// #### Parameters
    ///
    /// - `callback`: the callback to receive the result
    public void requestNotificationPermission(NotificationPermissionCallback callback) {
        requestNotificationPermission(new NotificationPermissionRequest(), callback);
    }

    /// Requests permission to post notifications with the capabilities described by the
    /// given request. The result is delivered to the callback on the EDT.
    ///
    /// #### Parameters
    ///
    /// - `request`: describes which notification capabilities to request
    ///
    /// - `callback`: the callback to receive the result
    public void requestNotificationPermission(NotificationPermissionRequest request, NotificationPermissionCallback callback) {
        impl.requestNotificationPermission(request, callback);
    }

    /// Registers a notification channel (Android). No-op on platforms without channels.
    ///
    /// #### Parameters
    ///
    /// - `builder`: the channel definition
    public void registerNotificationChannel(NotificationChannelBuilder builder) {
        impl.registerNotificationChannel(builder);
    }

    /// Deletes a notification channel (Android). No-op on platforms without channels.
    ///
    /// #### Parameters
    ///
    /// - `channelId`: the channel id to delete
    public void deleteNotificationChannel(String channelId) {
        impl.deleteNotificationChannel(channelId);
    }

    /// Creates a notification channel group (Android). No-op on platforms without channels.
    ///
    /// #### Parameters
    ///
    /// - `groupId`: the group id
    ///
    /// - `groupName`: the user-visible group name
    public void createNotificationChannelGroup(String groupId, String groupName) {
        impl.createNotificationChannelGroup(groupId, groupName);
    }

    /// Schedules constraint-aware background work. Used internally by
    /// `com.codename1.background.BackgroundWork`.
    ///
    /// #### Parameters
    ///
    /// - `request`: the work request
    public void scheduleBackgroundWork(WorkRequest request) {
        impl.scheduleBackgroundWork(request);
    }

    /// Cancels scheduled background work by id.
    ///
    /// #### Parameters
    ///
    /// - `workId`: the work id
    public void cancelBackgroundWork(String workId) {
        impl.cancelBackgroundWork(workId);
    }

    /// Returns true if constraint-aware background work is supported.
    ///
    /// #### Returns
    ///
    /// true if supported
    public boolean isBackgroundWorkSupported() {
        return impl.isBackgroundWorkSupported();
    }

    /// Schedules a deferrable background processing task. Used internally by
    /// `com.codename1.background.BackgroundTask`.
    ///
    /// #### Parameters
    ///
    /// - `id`: the task id
    ///
    /// - `earliestBeginEpochMs`: the earliest begin time in milliseconds since the epoch, or 0
    ///
    /// - `requiresNetwork`: true if network is required
    ///
    /// - `requiresPower`: true if charging is required
    ///
    /// - `task`: the work to run
    public void scheduleBackgroundProcessing(String id, long earliestBeginEpochMs, boolean requiresNetwork, boolean requiresPower, Runnable task) {
        impl.scheduleBackgroundProcessing(id, earliestBeginEpochMs, requiresNetwork, requiresPower, task);
    }

    /// Cancels a scheduled background processing task.
    ///
    /// #### Parameters
    ///
    /// - `id`: the task id
    public void cancelBackgroundProcessing(String id) {
        impl.cancelBackgroundProcessing(id);
    }

    /// Returns true if deferrable background processing is supported.
    ///
    /// #### Returns
    ///
    /// true if supported
    public boolean isBackgroundProcessingSupported() {
        return impl.isBackgroundProcessingSupported();
    }

    /// Starts a foreground service. Used internally by
    /// `com.codename1.background.ForegroundService`.
    ///
    /// #### Parameters
    ///
    /// - `channelId`: the notification channel id
    ///
    /// - `title`: the notification title
    ///
    /// - `body`: the notification body
    ///
    /// - `iconName`: the small icon resource name, or null
    ///
    /// - `task`: the task to run
    ///
    /// - `handle`: the service handle passed to the task
    ///
    /// #### Returns
    ///
    /// an opaque native handle
    public Object startForegroundService(String channelId, String title, String body, String iconName, ForegroundService.Task task, ForegroundService handle) {
        return impl.startForegroundService(channelId, title, body, iconName, task, handle);
    }

    /// Updates a foreground service notification.
    ///
    /// #### Parameters
    ///
    /// - `nativeHandle`: the handle returned by `#startForegroundService`
    ///
    /// - `title`: the new title
    ///
    /// - `body`: the new body
    public void updateForegroundServiceNotification(Object nativeHandle, String title, String body) {
        impl.updateForegroundServiceNotification(nativeHandle, title, body);
    }

    /// Stops a foreground service.
    ///
    /// #### Parameters
    ///
    /// - `nativeHandle`: the handle returned by `#startForegroundService`
    public void stopForegroundService(Object nativeHandle) {
        impl.stopForegroundService(nativeHandle);
    }

    /// Returns true if foreground services are supported.
    ///
    /// #### Returns
    ///
    /// true if supported
    public boolean isForegroundServiceSupported() {
        return impl.isForegroundServiceSupported();
    }

    /// Returns true if the platform can receive shared content from other apps.
    ///
    /// #### Returns
    ///
    /// true if supported
    public boolean isReceiveSharedContentSupported() {
        return impl.isReceiveSharedContentSupported();
    }

    /// Returns true if the platform supports publishing data to a Wallet
    /// issuer-provisioning extension. Used internally by
    /// `com.codename1.payment.WalletExtension`.
    public boolean isWalletExtensionSupported() {
        return impl.isWalletExtensionSupported();
    }

    /// Publishes the Wallet extension pass entries, replacing the previous
    /// list. Used internally by `com.codename1.payment.WalletExtension`.
    ///
    /// #### Parameters
    ///
    /// - `remote`: true for the Apple Watch list, false for the iPhone list
    ///
    /// - `entries`: the available cards; null or empty clears the list
    public void walletExtensionSetPassEntries(boolean remote, com.codename1.payment.WalletPassEntry[] entries) {
        impl.walletExtensionClearPassEntries(remote);
        if (entries != null) {
            for (com.codename1.payment.WalletPassEntry e : entries) {
                if (e == null) {
                    continue;
                }
                impl.walletExtensionAddPassEntry(remote, e.getIdentifier(), e.getTitle(),
                        e.getCardholderName(), e.getPrimaryAccountSuffix(), e.getPaymentNetwork(),
                        e.getLocalizedDescription(), e.getArtPng());
            }
        }
    }

    /// Sets the Wallet extension requires-authentication flag. Used
    /// internally by `com.codename1.payment.WalletExtension`.
    public void walletExtensionSetRequiresAuthentication(boolean requiresAuthentication) {
        impl.walletExtensionSetRequiresAuthentication(requiresAuthentication);
    }

    /// Publishes the Wallet extension auth token. Used internally by
    /// `com.codename1.payment.WalletExtension`.
    public void walletExtensionSetAuthToken(String token) {
        impl.walletExtensionSetAuthToken(token);
    }

    /// Clears all published Wallet extension data. Used internally by
    /// `com.codename1.payment.WalletExtension`.
    public void walletExtensionClear() {
        impl.walletExtensionClear();
    }

    /// Subscribes the device to a push topic. Used internally by
    /// `com.codename1.push.Push`.
    ///
    /// #### Parameters
    ///
    /// - `topic`: the topic name
    public void subscribeToPushTopic(String topic) {
        impl.subscribeToPushTopic(topic);
    }

    /// Unsubscribes the device from a push topic. Used internally by
    /// `com.codename1.push.Push`.
    ///
    /// #### Parameters
    ///
    /// - `topic`: the topic name
    public void unsubscribeFromPushTopic(String topic) {
        impl.unsubscribeFromPushTopic(topic);
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

    /// Whether this build is a development build rather than a release build headed for
    /// an app store. This is broader than [#isSimulator()], which reports the JavaSE
    /// simulator and designer specifically and is false on a device however the build was
    /// signed. Use it to gate a facility that belongs in a build you are working on but
    /// not in one a user installs.
    ///
    /// What each port reports:
    ///
    /// - Android: true when the package carries the debuggable flag, which a debug build
    /// sets and a release build clears.
    ///
    /// - iOS: true when the provisioning profile grants get-task-allow, the entitlement
    /// that permits a debugger to attach. Development and ad-hoc profiles carry it; App
    /// Store and enterprise profiles do not.
    ///
    /// - JavaSE: ALWAYS true. That port runs the simulator, the designer and the desktop
    /// tooling, and it cannot distinguish those from a desktop application packaged for
    /// distribution, so a packaged desktop app also reports true. Do not rely on this
    /// method alone to withhold something from a shipped DESKTOP build; combine it with
    /// your own signal there.
    ///
    /// - Any other port: false, because it cannot tell. The answer errs towards treating a
    /// build as a release and withholding the facility.
    ///
    /// #### Returns
    ///
    /// true if this is a development build
    public boolean isDebuggableBuild() {
        return impl.isDebuggableBuild();
    }

    /// Creates an audio media that can be played in the background.
    ///
    /// #### Parameters
    ///
    /// - `uri`: @param uri the uri of the media can start with jar://, file://, http://
    /// (can also use rtsp:// if supported on the platform)
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
    /// (can also use rtsp:// if supported on the platform)
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

    /// Renders an Apple SF Symbol to an image on iOS (null elsewhere / if the symbol
    /// is unavailable). name = SF Symbol name (e.g. "star.fill"); color = 0xRRGGBB;
    /// sizePixels = target point size in PIXELS; weight 0=regular..higher bolder.
    public Image createSFSymbolImage(String name, int color, float sizePixels, int weight) {
        return impl.createSFSymbolImage(name, color, sizePixels, weight);
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

    /// Requests a signed device-attestation token (Play Integrity / App Attest) bound to the server
    /// nonce. See `com.codename1.security.DeviceIntegrity#requestIntegrityToken(String)`.
    public AsyncResource<String> requestIntegrityToken(String nonce) {
        return impl.requestIntegrityToken(nonce);
    }

    /// Returns true if device-attestation (Play Integrity / App Attest) is supported and bundled.
    public boolean isAttestationSupported() {
        return impl.isAttestationSupported();
    }

    /// Non-exiting RASP check, true if the device appears rooted/jailbroken/instrumented/tampered.
    public boolean isDeviceCompromised() {
        return impl.isDeviceCompromised();
    }

    /// Returns the reason codes behind `isDeviceCompromised()` (e.g. "root", "frida", "emulator").
    public String[] getCompromiseReasons() {
        return impl.getCompromiseReasons();
    }

    /// Returns the component ids of the accessibility services currently enabled on the device.
    public String[] getEnabledAccessibilityServices() {
        return impl.getEnabledAccessibilityServices();
    }

    /// Discards cached platform attestation state, forcing the next attestation to start from a fresh
    /// hardware key. See `com.codename1.security.DeviceIntegrity#resetAttestation()`.
    public void resetAttestation() {
        impl.resetAttestation();
    }

    /// Acknowledges that a backend recorded the attested key. See
    /// `com.codename1.security.DeviceIntegrity#confirmAttestation()`.
    public void confirmAttestation(String keyId) {
        impl.confirmAttestation(keyId);
    }

    /// Returns digests of the certificates the running app is signed with. Low level hook for the
    /// attestation layer, which reports them to a verifying service; an on-device comparison proves
    /// nothing on its own. Empty where the platform has no such concept.
    public String[] getAppSignerDigests() {
        return impl.getAppSignerDigests();
    }

    /// Marks the current screen secure (Android `FLAG_SECURE`), blocking screenshots/recording/scraping.
    public void setSecureScreen(boolean secure) {
        impl.setSecureScreen(secure);
    }

    /// Sets the tapjacking policy. See
    /// `com.codename1.security.DeviceIntegrity#setTapjackingProtection(TapjackingPolicy)`.
    public void setTapjackingProtection(com.codename1.security.TapjackingPolicy policy) {
        impl.setTapjackingProtection(policy);
    }

    /// The tapjacking policy currently in force, never null.
    public com.codename1.security.TapjackingPolicy getTapjackingPolicy() {
        return impl.getTapjackingPolicy();
    }

    /// True when the most recently observed touch arrived over an obscured window. See
    /// `com.codename1.security.DeviceIntegrity#isScreenObscured()`.
    public boolean isScreenObscured() {
        return impl.isScreenObscured();
    }

    /// Registers a listener notified when the obscured state changes.
    public void addTapjackingListener(ActionListener l) {
        impl.addTapjackingListener(l);
    }

    /// Removes a listener added by `addTapjackingListener()`.
    public void removeTapjackingListener(ActionListener l) {
        impl.removeTapjackingListener(l);
    }

    /// Asks the OS to hide overlay windows drawn over this app (Android 12+).
    public void setHideOverlayWindows(boolean hide) {
        impl.setHideOverlayWindows(hide);
    }

    /// True where `setHideOverlayWindows()` is actually enforced by the platform.
    public boolean isHideOverlayWindowsSupported() {
        return impl.isHideOverlayWindowsSupported();
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
    /// the app on their homescreen.
    public void onCanInstallOnHomescreen(Runnable r) {
        impl.onCanInstallOnHomescreen(r);
    }

    /// Captures a screenshot of the screen.
    ///
    /// #### Returns
    ///
    /// An image of the screen, or null if it failed.
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

        // Overrides nothing -- Throwable has no setCause, and adding the
        // annotation PMD asks for does not compile. Suppressed rather than
        // "fixed": this file only reaches the analyser at all because the
        // health entry point was added to it.
        public void setCause(Throwable t) { //NOPMD MissingOverride
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
