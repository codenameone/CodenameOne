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

import com.codename1.location.LocationManager;
import com.codename1.messaging.Message;
import com.codename1.ui.animations.Animation;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.impl.ImplementationFactory;
import com.codename1.impl.CodenameOneImplementation;
import com.codename1.impl.VirtualKeyboardInterface;
import com.codename1.io.ConnectionRequest;
import com.codename1.media.Media;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.EventDispatcher;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Central class for the API that manages rendering/events and is used to place top
 * level components ({@link Form}) on the "display". Before any Form is shown the Developer must
 * invoke Display.init(Object m) in order to register the current MIDlet.
 * <p>This class handles the main thread for the toolkit referenced here on as the EDT
 * (Event Dispatch Thread) similar to the Swing EDT. This thread encapsulates the platform
 * specific event delivery and painting semantics and enables threading features such as
 * animations etc...
 * <p>The EDT should not be blocked since paint operations and events would also be blocked
 * in much the same way as they would be in other platforms. In order to serialize calls back
 * into the EDT use the methods {@link Display#callSerially} &amp; {@link Display#callSeriallyAndWait}.
 * <p>Notice that all Codename One calls occur on the EDT (events, painting, animations etc...), Codename One
 * should normally be manipulated on the EDT as well (hence the {@link Display#callSerially} &amp;
 * {@link Display#callSeriallyAndWait} methods). Theoretically it should be possible to manipulate
 * some Codename One features from other threads but this can't be guaranteed to work for all use cases.
 *
 * @author Chen Fishbein, Shai Almog
 */
public final class Display {
    private EventDispatcher errorHandler;
    boolean codenameOneExited;

    /**
     * A common sound type that can be used with playBuiltinSound
     */
    public static final String SOUND_TYPE_ALARM = "alarm";

    /**
     * A common sound type that can be used with playBuiltinSound
     */
    public static final String SOUND_TYPE_CONFIRMATION = "confirmation";

    /**
     * A common sound type that can be used with playBuiltinSound
     */
    public static final String SOUND_TYPE_ERROR = "error";

    /**
     * A common sound type that can be used with playBuiltinSound
     */
    public static final String SOUND_TYPE_INFO = "info";

    /**
     * A common sound type that can be used with playBuiltinSound
     */
    public static final String SOUND_TYPE_WARNING = "warning";

    /**
     * A common sound type that can be used with playBuiltinSound
     */
    public static final String SOUND_TYPE_BUTTON_PRESS = "press";

    /**
     * Unknown keyboard type is the default indicating the software should try
     * to detect the keyboard type if necessary
     */
    public static final int KEYBOARD_TYPE_UNKNOWN = 0;

    /**
     * Numeric keypad keyboard type
     */
    public static final int KEYBOARD_TYPE_NUMERIC = 1;

    /**
     * Full QWERTY keypad keyboard type, even if a numeric keyboard also exists
     */
    public static final int KEYBOARD_TYPE_QWERTY = 2;

    /**
     * Touch device without a physical keyboard that should popup a keyboad
     */
    public static final int KEYBOARD_TYPE_VIRTUAL = 3;

    /**
     * Half QWERTY which needs software assistance for completion
     */
    public static final int KEYBOARD_TYPE_HALF_QWERTY = 4;

    private static final int POINTER_PRESSED = 1;
    private static final int POINTER_RELEASED = 2;
    private static final int POINTER_DRAGGED = 3;
    private static final int POINTER_HOVER = 8;
    private static final int POINTER_HOVER_RELEASED = 11;
    private static final int POINTER_HOVER_PRESSED = 12;
    private static final int KEY_PRESSED = 4;
    private static final int KEY_RELEASED = 5;
    private static final int KEY_LONG_PRESSED = 6;
    private static final int SIZE_CHANGED = 7;
    private static final int HIDE_NOTIFY = 9;
    private static final int SHOW_NOTIFY = 10;

    /**
     * Very Low Density 176x220 And Smaller
     */
    public static final int DENSITY_VERY_LOW = 10;

    /**
     * Low Density Up To 240x320
     */
    public static final int DENSITY_LOW = 20;

    /**
     * Medium Density Up To 360x480
     */
    public static final int DENSITY_MEDIUM = 30;

    /**
     * Hi Density Up To 480x854
     */
    public static final int DENSITY_HIGH = 40;

    /**
     * Very Hi Density Up To 1440x720
     */
    public static final int DENSITY_VERY_HIGH = 50;

    /**
     * HD Up To 1920x1080
     */
    public static final int DENSITY_HD = 60;


    /**
     * A pure touch device has no focus showing when the user is using the touch
     * interface. Selection only shows when the user actually touches the screen
     * or suddenly switches to using a keypad/trackball. This sort of interface
     * is common in Android devices
     */
    private boolean pureTouch;

    private Graphics codenameOneGraphics;

    /**
     * Indicates whether this is a touch device
     */
    private boolean touchScreen;

    private Hashtable localProperties;

    /**
     * Indicates whether the edt should sleep between each loop
     */
    private boolean noSleep = false;

    /**
     * Normally Codename One folds the VKB when switching forms this field allows us
     * to block that behavior.
     */
    private boolean autoFoldVKBOnFormSwitch = true;

    /**
     * Indicates the maximum drawing speed of no more than 10 frames per second
     * by default (this can be increased or decreased) the advantage of limiting
     * framerate is to allow the CPU to perform other tasks besides drawing.
     * Notice that when no change is occurring on the screen no frame is drawn and
     * so a high/low FPS will have no effect then.
     */
    private int framerateLock = 30;

    /**
     * Game action for fire
     */
    public static final int GAME_FIRE = 8;

    /**
     * Game action for left key
     */
    public static final int GAME_LEFT = 2;

    /**
     * Game action for right key
     */
    public static final int GAME_RIGHT = 5;

    /**
     * Game action for UP key
     */
    public static final int GAME_UP = 1;

    /**
     * Game action for down key
     */
    public static final int GAME_DOWN = 6;

    /**
     * An attribute that encapsulates '#' int value.
     */
    public static final int KEY_POUND = '#';

    private static final Display INSTANCE = new Display();

    static int transitionDelay = -1;

    private CodenameOneImplementation impl;

    private boolean codenameOneRunning = false;


    /**
     * Contains the call serially pending elements
     */
    private Vector pendingSerialCalls = new Vector();

    /**
     * This is the instance of the EDT used internally to indicate whether
     * we are executing on the EDT or some arbitrary thread
     */
    private Thread edt;

    /**
     * Contains animations that must be played in full by the EDT before anything further
     * may be processed. This is useful for transitions/intro's etc... that animate without
     * user interaction.
     */
    private Vector animationQueue;

    /**
     * Indicates whether the 3rd softbutton should be supported on this device
     */
    private boolean thirdSoftButton = false;

    private boolean editingText;

    /**
     * Ignore all calls to show occurring during edit, they are discarded immediately
     */
    public static final int SHOW_DURING_EDIT_IGNORE = 1;

    /**
     * If show is called while editing text in the native text box an exception is thrown
     */
    public static final int SHOW_DURING_EDIT_EXCEPTION = 2;

    /**
     * Allow show to occur during edit and discard all user input at this moment
     */
    public static final int SHOW_DURING_EDIT_ALLOW_DISCARD = 3;

    /**
     * Allow show to occur during edit and save all user input at this moment
     */
    public static final int SHOW_DURING_EDIT_ALLOW_SAVE = 4;

    /**
     * Show will update the current form to which the OK button of the text box
     * will return
     */
    public static final int SHOW_DURING_EDIT_SET_AS_NEXT = 5;

    private int showDuringEdit;

    static final Object lock = new Object();

    /**
     * Events to broadcast on the EDT
     */
    private Vector inputEvents = new Vector();

    private boolean longPointerCharged;
    private boolean pointerPressedAndNotReleasedOrDragged;
    private int pointerX, pointerY;
    private boolean keyRepeatCharged;
    private boolean longPressCharged;
    private long longKeyPressTime;
    private int longPressInterval = 800;
    private long nextKeyRepeatEvent;
    private int keyRepeatValue;
    private int keyRepeatInitialIntervalTime = 800;
    private int keyRepeatNextIntervalTime = 10;
    private boolean lastInteractionWasKeypad;
    private boolean dragOccured;
    private boolean processingSerialCalls;

    private int PATHLENGTH;
    private float[] dragPathX;
    private float[] dragPathY;
    private long[] dragPathTime;
    private int dragPathOffset = 0;
    private int dragPathLength = 0;

     /**
     * Internally track display initialization time as a fixed point to allow tagging of pointer
     * events with an integer timestamp (System.currentTimeMillis() - displayInitTime)
     * and not a long value.
     */
    private long displayInitTime = 0;


    /**
     * Allows a Codename One application to minimize without forcing it to the front whenever
     * a new dialog is poped up
     */
    private boolean allowMinimizing;

    /**
     * Indicates that the Codename One implementation should decide internally the command
     * behavior most appropriate for this platform.
     */
    public static final int COMMAND_BEHAVIOR_DEFAULT = 1;

    /**
     * Indicates the classic Codename One command behavior where the commands are placed in
     * a list within a dialog. This is the most customizable approach for none touch devices.
     */
    public static final int COMMAND_BEHAVIOR_SOFTKEY = 2;

    /**
     * Indicates the touch menu dialog rendered by Codename One where commands are placed
     * into a scrollable dialog
     */
    public static final int COMMAND_BEHAVIOR_TOUCH_MENU = 3;

    /**
     * Indicates that commands should be added to an always visible bar at the
     * bottom of the form.
     */
    public static final int COMMAND_BEHAVIOR_BUTTON_BAR = 4;

    /**
     * Identical to the bar behavior, places the back command within the title bar
     * of the form/dialg
     */
    public static final int COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_BACK = 5;

    /**
     * Places all commands on the right side of the title bar with a uniform size
     * grid layout
     */
    public static final int COMMAND_BEHAVIOR_BUTTON_BAR_TITLE_RIGHT = 6;

    /**
     * Commands are placed in the same was as they are in the ice cream sandwich Android
     * OS update where the back button has a theme icon the application icon appears next
     * to the 
     */
    public static final int COMMAND_BEHAVIOR_ICS = 7;

    /**
     * Indicates that commands should try to add themselves to the native menus
     */
    public static final int COMMAND_BEHAVIOR_NATIVE = 10;

    private int commandBehavior = COMMAND_BEHAVIOR_DEFAULT;

    private static String selectedVirtualKeyboard = VirtualKeyboard.NAME;

    private static Hashtable virtualKeyboards = new Hashtable();

    private boolean dropEvents;

    /**
     * Private constructor to prevent instanciation
     */
    private Display() {
    }

    Vector getAnimationQueue() {
        return animationQueue;
    }

    /**
     * This is the Display initialization method.
     * This method must be called before any Form is shown
     *
     * @param m the main running MIDlet
     */
    public static void init(Object m) {
        if(!INSTANCE.codenameOneRunning) {
            INSTANCE.codenameOneRunning = true;
            INSTANCE.displayInitTime = System.currentTimeMillis();
            INSTANCE.impl = (CodenameOneImplementation)ImplementationFactory.getInstance().createImplementation();

            INSTANCE.impl.setDisplayLock(lock);
            INSTANCE.impl.init(m);
            INSTANCE.codenameOneGraphics = new Graphics(INSTANCE.impl.getNativeGraphics());
            INSTANCE.impl.setCodenameOneGraphics(INSTANCE.codenameOneGraphics);

            // only enable but never disable the third softbutton
            if(INSTANCE.impl.isThirdSoftButton()) {
                INSTANCE.thirdSoftButton = true;
            }
            if(INSTANCE.impl.getSoftkeyCount() > 0) {
                MenuBar.leftSK = INSTANCE.impl.getSoftkeyCode(0)[0];
                if(INSTANCE.impl.getSoftkeyCount() > 1) {
                    MenuBar.rightSK = INSTANCE.impl.getSoftkeyCode(1)[0];
                    if(INSTANCE.impl.getSoftkeyCode(1).length > 1){
                        MenuBar.rightSK2 = INSTANCE.impl.getSoftkeyCode(1)[1];
                    }
                }
            }
            MenuBar.backSK = INSTANCE.impl.getBackKeyCode();
            MenuBar.backspaceSK = INSTANCE.impl.getBackspaceKeyCode();
            MenuBar.clearSK = INSTANCE.impl.getClearKeyCode();

            INSTANCE.PATHLENGTH = INSTANCE.impl.getDragPathLength();
            INSTANCE.dragPathX = new float[INSTANCE.PATHLENGTH];
            INSTANCE.dragPathY = new float[INSTANCE.PATHLENGTH];
            INSTANCE.dragPathTime = new long[INSTANCE.PATHLENGTH];
            com.codename1.io.Util.setImplementation(INSTANCE.impl);
            
            // this can happen on some cases where an application was restarted etc...
            // generally its probably a bug but we can let it slide...
            if(INSTANCE.edt == null) {
                INSTANCE.touchScreen = INSTANCE.impl.isTouchDevice();

                // initialize the Codename One EDT which from now on will take all responsibility
                // for the event delivery.
                INSTANCE.edt = new Thread(new RunnableWrapper(null, 3), "EDT");
                INSTANCE.impl.setThreadPriority(INSTANCE.edt, Thread.NORM_PRIORITY + 1);
                INSTANCE.edt.start();
            }
            INSTANCE.impl.postInit();
            //sets the default device user agent if available by the platform, by default
            //we set Nokia, beacause if the user agent is empty it is most likely a J2ME device
            ConnectionRequest.setDefaultUserAgent(INSTANCE.getProperty("User-Agent", 
                    "Mozilla/5.0 (SymbianOS/9.4; Series60/5.0 NokiaN97-1/20.0.019; Profile/MIDP-2.1 Configuration/CLDC-1.1) AppleWebKit/525 (KHTML, like Gecko) BrowserNG/7.1.18124"));

        }else{
            INSTANCE.impl.confirmControlView();
        }
    }

    /**
     * Closes down the EDT and Codename One, under normal conditions this method is completely unnecessary
     * since exiting the application will shut down Codename One. However, if the application is minimized
     * and the user wishes to free all resources without exiting the application then this method can be used.
     * Once this method is used Codename One will no longer work and Display.init(Object) should be invoked
     * again for any further Codename One call!
     * Notice that minimize (being a Codename One method) MUST be invoked before invoking this method!
     */
    public static void deinitialize() {
        INSTANCE.codenameOneRunning = false;
        synchronized(lock) {
            lock.notifyAll();
        }
    }
    /**
     * This method returns true if the Display is initialized.
     *
     * @return true if the EDT is running
     */
    public static boolean isInitialized(){
        return INSTANCE.codenameOneRunning;
    }

    /**
     * Return the Display instance
     *
     * @return the Display instance
     */
    public static Display getInstance(){
        return INSTANCE;
    }

    /**
     * This method allows us to manipulate the drag started detection logic.
     * If the pointer was dragged for more than this percentage of the display size it
     * is safe to assume that a drag is in progress.
     *
     * @return motion percentage
     */
    public int getDragStartPercentage() {
        return getImplementation().getDragStartPercentage();
    }

    /**
     * This method allows us to manipulate the drag started detection logic.
     * If the pointer was dragged for more than this percentage of the display size it
     * is safe to assume that a drag is in progress.
     *
     * @param dragStartPercentage percentage of the screen required to initiate drag
     */
    public void setDragStartPercentage(int dragStartPercentage) {
        getImplementation().setDragStartPercentage(dragStartPercentage);
    }

    CodenameOneImplementation getImplementation() {
        return impl;
    }

    /**
     * Indicates the maximum frames the API will try to draw every second
     * by default this is set to 10. The advantage of limiting
     * framerate is to allow the CPU to perform other tasks besides drawing.
     * Notice that when no change is occurring on the screen no frame is drawn and
     * so a high/low FPS will have no effect then.
     * 10FPS would be very reasonable for a business application.
     *
     * @param rate the frame rate
     */
    public void setFramerate(int rate) {
        framerateLock = 1000 / rate;
    }

    /**
     * Vibrates the device for the given length of time
     *
     * @param duration length of time to vibrate
     */
    public void vibrate(int duration) {
        impl.vibrate(duration);
    }

    /**
     * Flash the backlight of the device for the given length of time
     *
     * @param duration length of time to flash the backlight
     */
    public void flashBacklight(int duration) {
        impl.flashBacklight(duration);
    }

    /**
     * Invoking the show() method of a form/dialog while the user is editing
     * text in the native text box can have several behaviors: SHOW_DURING_EDIT_IGNORE,
     * SHOW_DURING_EDIT_EXCEPTION, SHOW_DURING_EDIT_ALLOW_DISCARD,
     * SHOW_DURING_EDIT_ALLOW_SAVE, SHOW_DURING_EDIT_SET_AS_NEXT
     *
     * @param showDuringEdit one of the following: SHOW_DURING_EDIT_IGNORE,
     * SHOW_DURING_EDIT_EXCEPTION, SHOW_DURING_EDIT_ALLOW_DISCARD,
     * SHOW_DURING_EDIT_ALLOW_SAVE, SHOW_DURING_EDIT_SET_AS_NEXT
     */
    public void setShowDuringEditBehavior(int showDuringEdit) {
        this.showDuringEdit = showDuringEdit;
    }

    /**
     * Returns the status of the show during edit flag
     *
     * @return one of the following: SHOW_DURING_EDIT_IGNORE,
     * SHOW_DURING_EDIT_EXCEPTION, SHOW_DURING_EDIT_ALLOW_DISCARD,
     * SHOW_DURING_EDIT_ALLOW_SAVE, SHOW_DURING_EDIT_SET_AS_NEXT
     */
    public int getShowDuringEditBehavior() {
        return showDuringEdit;
    }

    /**
     * Indicates the maximum frames the API will try to draw every second
     *
     * @return the frame rate
     */
    public int getFrameRate() {
        return 1000 / framerateLock;
    }

    /**
     * Returns true if we are currently in the event dispatch thread.
     * This is useful for generic code that can be used both with the
     * EDT and outside of it.
     *
     * @return true if we are currently in the event dispatch thread;
     * otherwise false
     */
    public boolean isEdt() {
        return edt == Thread.currentThread();
    }

    /**
     * Plays sound for the dialog
     */
    void playDialogSound(final int type) {
        impl.playDialogSound(type);
    }

    /**
     * Causes the runnable to be invoked on the event dispatch thread. This method
     * returns immediately and will not wait for the serial call to occur
     *
     * @param r runnable (NOT A THREAD!) that will be invoked on the EDT serial to
     * the paint and key handling events
     */
    public void callSerially(Runnable r){
        synchronized(lock) {
            pendingSerialCalls.addElement(r);
            lock.notify();
        }
    }


    /**
     * Identical to callSerially with the added benefit of waiting for the Runnable method to complete.
     *
     * @param r runnable (NOT A THREAD!) that will be invoked on the EDT serial to
     * the paint and key handling events
     * @throws IllegalStateException if this method is invoked on the event dispatch thread (e.g. during
     * paint or event handling).
     */
    public void callSeriallyAndWait(Runnable r){
        RunnableWrapper c = new RunnableWrapper(r, 0);
        callSerially(c);
        flushEdt();
        synchronized(lock) {
            while(!c.isDone()) {
                try {
                    // poll doneness to prevent potential race conditions
                    lock.wait(50);
                } catch(InterruptedException err) {}
            }
        }
    }

    /**
     * Identical to callSerially with the added benefit of waiting for the Runnable method to complete.
     *
     * @param r runnable (NOT A THREAD!) that will be invoked on the EDT serial to
     * the paint and key handling events
     * @param timeout timeout duration, on timeout the method just returns
     * @throws IllegalStateException if this method is invoked on the event dispatch thread (e.g. during
     * paint or event handling).
     */
    public void callSeriallyAndWait(Runnable r, int timeout){
        RunnableWrapper c = new RunnableWrapper(r, 0);
        callSerially(c);
        synchronized(lock) {
            long t = System.currentTimeMillis();
            while(!c.isDone()) {
                try {
                    // poll doneness to prevent potential race conditions
                    lock.wait(20);
                } catch(InterruptedException err) {}
                if(System.currentTimeMillis() - t >= timeout) {
                    return;
                }
            }
        }
    }

    /**
     * Allows us to "flush" the edt to allow any pending transitions and input to go
     * by before continuing with our other tasks.
     */
    void flushEdt() {
        if(!isEdt()){
            return;
        }
        while(!shouldEDTSleepNoFormAnimation()) {
            edtLoopImpl();
        }
        while(animationQueue != null && animationQueue.size() > 0){
            edtLoopImpl();
        }
    }

    /**
     * Restores the menu in the given form
     */
    private void restoreMenu(Form f) {
        if(f != null) {
            f.restoreMenu();
        }
    }


    private void paintTransitionAnimation() {
        Animation ani = (Animation) animationQueue.elementAt(0);
        if (!ani.animate()) {
            animationQueue.removeElementAt(0);
            if (ani instanceof Transition) {
                Form source = (Form) ((Transition)ani).getSource();
                restoreMenu(source);

                if (animationQueue.size() > 0) {
                    ani = (Animation) animationQueue.elementAt(0);
                    if (ani instanceof Transition) {
                        ((Transition) ani).initTransition();
                    }
                }else{
                    Form f = (Form) ((Transition)ani).getDestination();
                    restoreMenu(f);
                    if (source == null || source == impl.getCurrentForm() || source == getCurrent()) {
                        setCurrentForm(f);
                    }
                    ((Transition) ani).cleanup();
                }
                return;
            }
        }
        ani.paint(codenameOneGraphics);
        impl.flushGraphics();

        if(transitionDelay > 0) {
            // yield for a fraction, some devices don't "properly" implement
            // flush and so require the painting thread to get CPU too.
            try {
                synchronized(lock){
                    lock.wait(transitionDelay);
                }
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * This method represents the event thread for the UI library on which
     * all events are carried out. It differs from the MIDP event thread to
     * prevent blocking of actual input and drawing operations. This also
     * enables functionality such as "true" modal dialogs etc...
     */
    void mainEDTLoop() {
        impl.initEDT();
        UIManager.getInstance();
        com.codename1.ui.VirtualKeyboard vkb = new com.codename1.ui.VirtualKeyboard();
        INSTANCE.registerVirtualKeyboard(vkb);
        try {
            synchronized(lock){
                // when there is no current form the EDT is useful only
                // for features such as call serially
                while(impl.getCurrentForm() == null) {
                    if(shouldEDTSleep()) {
                        lock.wait();
                    }

                    // paint transition or intro animations and don't do anything else if such
                    // animations are in progress...
                    if(animationQueue != null && animationQueue.size() > 0) {
                        paintTransitionAnimation();
                        continue;
                    }
                    processSerialCalls();
                }
            }
        } catch(Throwable err) {
            err.printStackTrace();
            if(!impl.handleEDTException(err)) {
                if(errorHandler != null) {
                    errorHandler.fireActionEvent(new ActionEvent(err));
                } else {
                    Dialog.show("Error", "An internal application error occurred: " + err.toString(), "OK", null);
                }
            }
        }

        while(codenameOneRunning) {
            try {
                // wait indefinetly Lock surrounds the should method to prevent serial calls from
                // getting "lost"
                 synchronized(lock){
                     if(shouldEDTSleep()) {
                         impl.edtIdle(true);
                         lock.wait();
                         impl.edtIdle(false);
                     }
                 }

                edtLoopImpl();
            } catch(Throwable err) {
                err.printStackTrace();
                if(!impl.handleEDTException(err)) {
                    if(errorHandler != null) {
                        errorHandler.fireActionEvent(new ActionEvent(err));
                    } else {
                        Dialog.show("Error", "An internal application error occurred: " + err.toString(), "OK", null);
                    }
                }
            }
        }
        INSTANCE.impl.deinitialize();
        //INSTANCE.impl = null;
        //INSTANCE.codenameOneGraphics = null;
        INSTANCE.edt = null;
    }

    long time;

    /**
     * Implementation of the event dispatch loop content
     */
    void edtLoopImpl() {
        try {
            // transitions shouldn't be bound by framerate
            if(animationQueue == null || animationQueue.size() == 0) {
                // prevents us from waking up the EDT too much and
                // thus exhausting the systems resources. The + 1
                // prevents us from ever waiting 0 milliseconds which
                // is the same as waiting with no time limit
                if(!noSleep){
                    synchronized(lock){
                        lock.wait(Math.max(1, framerateLock - (time)));
                    }
                }
            } else {
                // paint transition or intro animations and don't do anything else if such
                // animations are in progress...
                paintTransitionAnimation();
                return;
            }
        } catch(Exception ignor) {
            ignor.printStackTrace();
        }
        long currentTime = System.currentTimeMillis();

        while(inputEvents.size() > 0) {
            int[] i = (int[])inputEvents.elementAt(0);
            inputEvents.removeElementAt(0);
            handleEvent(i);
        }

        codenameOneGraphics.setGraphics(impl.getNativeGraphics());
        impl.paintDirty();

        // draw the animations
        Form current = impl.getCurrentForm();
        if(current != null){
            current.repaintAnimations();
            // check key repeat events
            long t = System.currentTimeMillis();
            if(keyRepeatCharged && nextKeyRepeatEvent <= t) {
                current.keyRepeated(keyRepeatValue);
                nextKeyRepeatEvent = t + keyRepeatNextIntervalTime;
            }
            if(longPressCharged && longPressInterval <= t - longKeyPressTime) {
                longPressCharged = false;
                current.longKeyPress(keyRepeatValue);
            }
            if(longPointerCharged && longPressInterval <= t - longKeyPressTime) {
                longPointerCharged = false;
                current.longPointerPress(pointerX, pointerY);
            }
        }
        processSerialCalls();
        time = System.currentTimeMillis() - currentTime;
    }

    boolean hasNoSerialCallsPending() {
        return pendingSerialCalls.size() == 0;
    }

    /**
     * Called by the underlying implementation to indicate that editing in the native
     * system has completed and changes should propogate into Codename One
     *
     * @param c edited component
     * @param text new text for the component
     */
    public void onEditingComplete(Component c, String text) {
        c.onEditComplete(text);
        c.fireActionEvent();
    }

    /**
     * Used by the EDT to process all the calls submitted via call serially
     */
    void processSerialCalls() {
        processingSerialCalls = true;
        int size = pendingSerialCalls.size();
        if(size > 0) {
            Runnable[] array = null;
            synchronized(lock) {
                size = pendingSerialCalls.size();
                array = new Runnable[size];

                // copy all elements to an array and remove them otherwise invokeAndBlock from
                // within a callSerially() can cause an infinite loop...
                pendingSerialCalls.copyInto(array);

                if(size == pendingSerialCalls.size()) {
                    // this is faster
                    pendingSerialCalls.removeAllElements();
                } else {
                    // this can occur if an element was added during the loop
                    for(int iter = 0 ; iter < size ; iter++) {
                        pendingSerialCalls.removeElementAt(0);
                    }
                }
            }

            for(int iter = 0 ; iter < size ; iter++) {
                array[iter].run();
            }

            // after finishing an event cycle there might be serial calls waiting
            // to return.
            synchronized(lock){
                lock.notify();
            }
        }
        processingSerialCalls = false;
    }

    boolean isProcessingSerialCalls() {
        return processingSerialCalls;
    }

    void notifyDisplay(){
        synchronized (lock) {
            lock.notify();
        }
    }

/**
     * Invokes runnable and blocks the current thread, if the current thread is the
     * edt it will still be blocked however a separate thread would be launched
     * to perform the duties of the EDT while it is blocked. Once blocking is finished
     * the EDT would be restored to its original position. This is very similar to the
     * "foxtrot" Swing toolkit and allows coding "simpler" logic that requires blocking
     * code in the middle of event sensitive areas.
     *
     * @param r runnable (NOT A THREAD!) that will be invoked synchroniously by this method
     * @param dropEvents indicates if the display should drop all events
     * while this runnable is running
     */
    public void invokeAndBlock(Runnable r, boolean dropEvents){
        this.dropEvents = dropEvents;
        try {
            if(isEdt()) {
                // this class allows a runtime exception to propogate correctly out of the
                // internal thread
                RunnableWrapper w = new RunnableWrapper(r, 1);
                RunnableWrapper.pushToThreadPool(w);

                synchronized(lock) {
                    try {
                        // yeald the CPU for a very short time to let the invoke thread
                        // get started
                        lock.wait(2);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
                // loop over the EDT until the thread completes then return
                while(!w.isDone() && codenameOneRunning) {
                    edtLoopImpl();
                }
                // if the thread thew an exception we need to throw it onwards
                if(w.getErr() != null) {
                    throw w.getErr();
                }
            } else {
                r.run();
            }
        } finally {
            this.dropEvents = false;
        }
    }

    /**
     * Invokes runnable and blocks the current thread, if the current thread is the
     * edt it will still be blocked however a separate thread would be launched
     * to perform the duties of the EDT while it is blocked. Once blocking is finished
     * the EDT would be restored to its original position. This is very similar to the
     * "foxtrot" Swing toolkit and allows coding "simpler" logic that requires blocking
     * code in the middle of event sensitive areas.
     *
     * @param r runnable (NOT A THREAD!) that will be invoked synchroniously by this method
     */
    public void invokeAndBlock(Runnable r){
        invokeAndBlock(r, false);
    }

    /**
     * Indicates if this is a touch screen device that will return pen events,
     * defaults to true if the device has pen events but can be overriden by
     * the developer.
     *
     * @return true if this device supports touch events
     */
    public boolean isTouchScreenDevice() {
        return touchScreen;
    }

    /**
     * Indicates if this is a touch screen device that will return pen events,
     * defaults to true if the device has pen events but can be overriden by
     * the developer.
     *
     * @param touchScreen false if this is not a touch screen device
     */
    public void setTouchScreenDevice(boolean touchScreen) {
        this.touchScreen = touchScreen;
    }
    /**
     * Calling this method with noSleep=true will cause the edt to run without sleeping.
     *
     * @param noSleep causes the edt to stop the sleeping periods between 2 cycles
     */
    public void setNoSleep(boolean noSleep){
        this.noSleep = noSleep;
    }

    /**
     * Displays the given Form on the screen.
     *
     * @param newForm the Form to Display
     */
    void setCurrent(final Form newForm, boolean reverse){
        if(edt == null) {
            throw new IllegalStateException("Initialize must be invoked before setCurrent!");
        }

        if(autoFoldVKBOnFormSwitch && !(newForm instanceof Dialog)) {
            setShowVirtualKeyboard(false);
        }

        if(editingText) {
            switch(showDuringEdit) {
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
            }
        }

        if(!isEdt()) {
            callSerially(new RunnableWrapper(newForm, null, reverse));
            return;
        }

        Form current = impl.getCurrentForm();
        if(current != null){
            if(current.isInitialized()) {
                current.deinitializeImpl();
            }
        }
        if(!newForm.isInitialized()) {
            newForm.initComponentImpl();
        }

        if(newForm.getWidth() != getDisplayWidth() || newForm.getHeight() != getDisplayHeight()) {
            newForm.setSize(new Dimension(getDisplayWidth(), getDisplayHeight()));
            newForm.setShouldCalcPreferredSize(true);
            newForm.layoutContainer();
        }

        synchronized(lock) {
            boolean transitionExists = false;
            if(animationQueue != null && animationQueue.size() > 0) {
                Object o = animationQueue.lastElement();
                if(o instanceof Transition) {
                    current = (Form)((Transition)o).getDestination();
                    impl.setCurrentForm(current);
                }
            }

            if(current != null) {
                // make sure the fold menu occurs as expected then set the current
                // to the correct parent!
                if(current instanceof Dialog && ((Dialog)current).isMenu()) {
                    Transition t = current.getTransitionOutAnimator();
                    if(t != null) {
                        // go back to the parent form first
                        if(((Dialog)current).getPreviousForm() != null) {
                            initTransition(t.copy(false), current, ((Dialog)current).getPreviousForm());
                        }
                    }
                    current = ((Dialog)current).getPreviousForm();
                    impl.setCurrentForm(current);
                }

                // prevent the transition from occurring from a form into itself
                if(newForm != current) {
                    if((current != null && current.getTransitionOutAnimator() != null) || newForm.getTransitionInAnimator() != null) {
                        if(animationQueue == null) {
                            animationQueue = new Vector();
                        }
                        // prevent form transitions from breaking our dialog based
                        // transitions which are a bit sensitive
                        if(current != null && (!(newForm instanceof Dialog))) {
                            Transition t = current.getTransitionOutAnimator();
                            if(current != null && t != null) {
                                transitionExists = initTransition(t.copy(reverse), current, newForm);
                            }
                        }
                        if(current != null && !(current instanceof Dialog)) {
                            Transition t = newForm.getTransitionInAnimator();
                            if(t != null) {
                                transitionExists = initTransition(t.copy(reverse), current, newForm);
                            }
                        }
                    }
                }
            }
            lock.notify();

            if(!transitionExists) {
                if(animationQueue == null || animationQueue.size() == 0) {
                    setCurrentForm(newForm);
                } else {
                    // we need to add an empty transition to "serialize" this
                    // screen change...
                    Transition t = CommonTransitions.createEmpty();
                    initTransition(t, current, newForm);
                }
            }
        }
    }

    /**
     * Initialize the transition and add it to the queue
     */
    private boolean initTransition(Transition transition, Form source, Form dest) {
         try {
            dest.setVisible(true);
            transition.init(source, dest);
            if(source != null) {
                source.setLightweightMode(true);
            }
            if(dest != null) {
                dest.setLightweightMode(true);
            }
            animationQueue.addElement(transition);

            if (animationQueue.size() == 1) {
                transition.initTransition();
            }
        } catch (Throwable e) {
            e.printStackTrace();
            transition.cleanup();
            animationQueue.removeElement(transition);
            return false;
        }
        return true;
    }

    void setCurrentForm(Form newForm){
        boolean forceShow = false;
        Form current = impl.getCurrentForm();
        if(current != null){
            current.setVisible(false);
        } else {
            forceShow = true;
        }
        keyRepeatCharged = false;
        longPressCharged = false;
        longPointerCharged = false;
        current = newForm;
        impl.setCurrentForm(current);
        current.setVisible(true);
        if(forceShow || !allowMinimizing) {
            impl.confirmControlView();
        }
        int w = current.getWidth();
        int h = current.getHeight();
        if(isEdt() && ( w != impl.getDisplayWidth() || h != impl.getDisplayHeight())){
           current.sizeChangedInternal(impl.getDisplayWidth(), impl.getDisplayHeight());
        }else{
            repaint(current);
        }
        lastKeyPressed = 0;
        previousKeyPressed = 0;
        newForm.onShowCompletedImpl();
    }

    /**
     * Indicates whether a delay should exist between calls to flush graphics during
     * transition. In some devices flushGraphics is asynchronious causing it to be
     * very slow with our background thread. The solution is to add a short wait allowing
     * the implementation time to paint the screen. This value is set automatically by default
     * but can be overriden for some devices.
     *
     * @param transitionD -1 for no delay otherwise delay in milliseconds
     */
    public void setTransitionYield(int transitionD) {
        transitionDelay = transitionD;
    }

    /**
     * Encapsulates the editing code which is specific to the platform, some platforms
     * would allow "in place editing" MIDP does not.
     *
     * @param cmp the {@link TextArea} component
     * @param maxSize the maximum size from the text area
     * @param constraint the constraints of the text area
     * @param text the string to edit
     */
    public void editString(Component cmp, int maxSize, int constraint, String text) {
        editString(cmp, maxSize, constraint, text, 0);
    }

    /**
     * Encapsulates the editing code which is specific to the platform, some platforms
     * would allow "in place editing" MIDP does not.
     *
     * @param cmp the {@link TextArea} component
     * @param maxSize the maximum size from the text area
     * @param constraint the constraints of the text area
     * @param text the string to edit
     * @param initiatingKeycode the keycode used to initiate the edit.
     */
    public void editString(Component cmp, int maxSize, int constraint, String text, int initiatingKeycode) {
        editingText = true;
        keyRepeatCharged = false;
        longPressCharged = false;
        lastKeyPressed = 0;
        previousKeyPressed = 0;
        impl.editString(cmp, maxSize, constraint, text, initiatingKeycode);
        editingText = false;
    }
    
    boolean isTextEditing() {
        return editingText;
    }

    /**
     * Minimizes the current application if minimization is supported by the platform (may fail).
     * Returns false if minimization failed.
     *
     * @return false if minimization failed true if it succeeded or seems to be successful
     */
    public boolean minimizeApplication() {
        return getImplementation().minimizeApplication();
    }

    /**
     * Indicates whether an application is minimized
     *
     * @return true if the application is minimized
     */
    public boolean isMinimized() {
        return getImplementation().isMinimized();
    }

    /**
     * Restore the minimized application if minimization is supported by the platform
     */
    public void restoreMinimizedApplication() {
        getImplementation().restoreMinimizedApplication();
    }

    private void addInputEvent(int[] ev) {
        synchronized(lock) {
            if (this.dropEvents && (ev[0] == KEY_PRESSED || ev[0] == KEY_RELEASED
                    || ev[0] == POINTER_PRESSED || ev[0] == POINTER_RELEASED
                    || ev[0] == POINTER_DRAGGED || ev[0] == POINTER_HOVER)) {
                return;
            }
            inputEvents.addElement(ev);
            lock.notify();
        }
    }

    /**
     * Creates a pointer event with the following properties
     */
    private int[] createPointerEvent(int[] x, int[] y, int eventType) {
        // apply timestamp early to ensure the timing happens on the native UI
        // thread and not later on the EDT.
        final int stamp = (int) (System.currentTimeMillis() - displayInitTime);
        if (x.length == 1) {
            return new int[]{eventType, x[0], y[0], stamp};
         }
        int[] arr = new int[2 + x.length * 2];

        arr[0] = eventType;
        int arrayOffset = 1;
        for(int iter = 0 ; iter < x.length ; iter++) {
            arr[arrayOffset] = x[iter];
            arrayOffset++;
            arr[arrayOffset] = y[iter];
            arrayOffset++;
        }
        arr[arrayOffset] = stamp;
        return arr;
    }


    private int[] createKeyEvent(int keyCode, boolean pressed) {
        if(pressed) {
            return new int[] {KEY_PRESSED, keyCode};
        } else {
            return new int[] {KEY_RELEASED, keyCode};
        }
    }

    private int previousKeyPressed;
    private int lastKeyPressed;

    /**
     * Pushes a key press event with the given keycode into Codename One
     *
     * @param keyCode keycode of the key event
     */
    public void keyPressed(final int keyCode){
        if(impl.getCurrentForm() == null){
            return;
        }
        addInputEvent(createKeyEvent(keyCode, true));

        lastInteractionWasKeypad = lastInteractionWasKeypad || (keyCode != MenuBar.leftSK && keyCode != MenuBar.clearSK && keyCode != MenuBar.backSK);

        // this solves a Sony Ericsson bug where on slider open/close someone "brilliant" chose
        // to send a keyPress with a -43/-44 keycode... Without ever sending a key release!
        keyRepeatCharged = (keyCode >= 0 || getGameAction(keyCode) > 0) || keyCode == impl.getClearKeyCode();
        longPressCharged = keyRepeatCharged;
        longKeyPressTime = System.currentTimeMillis();
        keyRepeatValue = keyCode;
        nextKeyRepeatEvent = System.currentTimeMillis() + keyRepeatInitialIntervalTime;
        previousKeyPressed = lastKeyPressed;
        lastKeyPressed = keyCode;
    }

    /**
     * Pushes a key release event with the given keycode into Codename One
     *
     * @param keyCode keycode of the key event
     */
    public void keyReleased(final int keyCode){
        keyRepeatCharged = false;
        longPressCharged = false;
        if(impl.getCurrentForm() == null){
            return;
        }
        // this can happen when traversing from the native form to the current form
        // caused by a keypress
        // We need the previous key press for Codename One issue 108 which can occur when typing into
        // text field rapidly and pressing two buttons at once. Originally I had a patch
        // here specifically to the native edit but that patch doesn't work properly for
        // all native phone bugs (e.g. incoming phone call rejected and the key release is
        // sent to the java application).
        if(keyCode != lastKeyPressed) {
            if(keyCode != previousKeyPressed) {
                return;
            } else {
                previousKeyPressed = 0;
            }
        } else {
            lastKeyPressed = 0;
        }
        addInputEvent(createKeyEvent(keyCode, false));
    }

    void keyRepeatedInternal(final int keyCode){
    }

    /**
     * Pushes a pointer drag event with the given coordinates into Codename One
     *
     * @param x the x position of the pointer
     * @param y the y position of the pointer
     */
    public void pointerDragged(final int[] x, final int[] y){
        if(impl.getCurrentForm() == null){
            return;
        }
        longPointerCharged = false;
        addInputEvent(createPointerEvent(x, y, POINTER_DRAGGED));
    }

    /**
     * Pushes a pointer hover event with the given coordinates into Codename One
     *
     * @param x the x position of the pointer
     * @param y the y position of the pointer
     */
    public void pointerHover(final int[] x, final int[] y){
        if(impl.getCurrentForm() == null){
            return;
        }
        addInputEvent(createPointerEvent(x, y, POINTER_HOVER));
    }


    /**
     * Pushes a pointer hover release event with the given coordinates into Codename One
     *
     * @param x the x position of the pointer
     * @param y the y position of the pointer
     */
    public void pointerHoverPressed(final int[] x, final int[] y){
        if(impl.getCurrentForm() == null){
            return;
        }
        addInputEvent(createPointerEvent(x, y, POINTER_HOVER_PRESSED));
    }

    /**
     * Pushes a pointer hover release event with the given coordinates into Codename One
     *
     * @param x the x position of the pointer
     * @param y the y position of the pointer
     */
    public void pointerHoverReleased(final int[] x, final int[] y){
        if(impl.getCurrentForm() == null){
            return;
        }
        addInputEvent(createPointerEvent(x, y, POINTER_HOVER_RELEASED));
    }

    /**
     * Pushes a pointer press event with the given coordinates into Codename One
     *
     * @param x the x position of the pointer
     * @param y the y position of the pointer
     */
    public void pointerPressed(final int[] x,final int[] y){
        if(impl.getCurrentForm() == null){
            return;
        }

        lastInteractionWasKeypad = false;
        longPointerCharged = true;
        longKeyPressTime = System.currentTimeMillis();
        pointerX = x[0];
        pointerY = y[0];
        addInputEvent(createPointerEvent(x, y, POINTER_PRESSED));
    }

    /**
     * Pushes a pointer release event with the given coordinates into Codename One
     *
     * @param x the x position of the pointer
     * @param y the y position of the pointer
     */
    public void pointerReleased(final int[] x, final int[] y){
        longPointerCharged = false;
        if(impl.getCurrentForm() == null){
            return;
        }
        addInputEvent(createPointerEvent(x, y, POINTER_RELEASED));
    }

    /**
     * Notifies Codename One of display size changes, this method is invoked by the implementation
     * class and is for internal use
     *
     * @param w the width of the drawing surface
     * @param h the height of the drawing surface
     */
    public void sizeChanged(int w, int h){
        Form current = impl.getCurrentForm();
        if(current == null) {
            return;
        }
        if(w == current.getWidth() && h == current.getHeight()) {
            return;
        }

        addInputEvent(createSizeChangedEvent(w, h));
    }

    private int[] createSizeChangedEvent(int w, int h) {
        return new int[] {SIZE_CHANGED, w, h};
    }


    /**
     * Broadcasts hide notify into Codename One, this method is invoked by the Codename One implementation
     * to notify Codename One of hideNotify events
     */
    public void hideNotify(){
        keyRepeatCharged = false;
        longPressCharged = false;
        longPointerCharged = false;
        pointerPressedAndNotReleasedOrDragged = false;
        addInputEvent(new int[]{HIDE_NOTIFY});
    }

    /**
     * Broadcasts show notify into Codename One, this method is invoked by the Codename One implementation
     * to notify Codename One of showNotify events
     */
    public void showNotify(){
        addInputEvent(new int[]{SHOW_NOTIFY});
    }


    /**
     * Used by the flush functionality which doesn't care much about component
     * animations
     */
    boolean shouldEDTSleepNoFormAnimation() {
        boolean b;
        synchronized(lock){
            b = inputEvents.size() == 0 &&
                    hasNoSerialCallsPending() &&
                    (!keyRepeatCharged || !longPressCharged);
        }
        return b;
    }

    private void updateDragSpeedStatus(int[] ev) {
            //save dragging input to calculate the dragging speed later
            dragPathX[dragPathOffset] = pointerEvent(1, ev)[0];
            dragPathY[dragPathOffset] = pointerEvent(2, ev)[0];
            dragPathTime[dragPathOffset] = displayInitTime + (long) ev[ev.length - 1];
            if (dragPathLength < PATHLENGTH) {
                dragPathLength++;
            }
            dragPathOffset++;
            if (dragPathOffset >= PATHLENGTH) {
                dragPathOffset = 0;
            }
    }

    /**
     * Invoked on the EDT to propagate the event
     */
    private void handleEvent(int[] ev) {
        Form f = getCurrentUpcomingForm(true);

        // might happen when returning from a deinitialized version of Codename One
        if(f == null) {
            return;
        }

        switch(ev[0]) {
        case KEY_PRESSED:
            f.keyPressed(ev[1]);
            break;
        case KEY_RELEASED:
            f.keyReleased(ev[1]);
            break;
        case POINTER_PRESSED:
            dragOccured = false;
            dragPathLength = 0;
            pointerPressedAndNotReleasedOrDragged = true;
            f.pointerPressed(pointerEvent(1, ev), pointerEvent(2, ev));
            break;
        case POINTER_RELEASED:
            pointerPressedAndNotReleasedOrDragged = false;
            f.pointerReleased(pointerEvent(1, ev), pointerEvent(2, ev));
            break;
        case POINTER_DRAGGED:
            dragOccured = true;
            updateDragSpeedStatus(ev);
            pointerPressedAndNotReleasedOrDragged = false;
            f.pointerDragged(pointerEvent(1, ev), pointerEvent(2, ev));
            break;
        case POINTER_HOVER:
            updateDragSpeedStatus(ev);
            f.pointerHover(pointerEvent(1, ev), pointerEvent(2, ev));
            break;
        case POINTER_HOVER_RELEASED:
            f.pointerHoverReleased(pointerEvent(1, ev), pointerEvent(2, ev));
            break;
        case POINTER_HOVER_PRESSED:
            f.pointerHoverPressed(pointerEvent(1, ev), pointerEvent(2, ev));
            break;
        case SIZE_CHANGED:
            f.sizeChangedInternal(ev[1], ev[2]);
            break;
        case HIDE_NOTIFY:
            f.hideNotify();
            break;
        case SHOW_NOTIFY:
            f.showNotify();
            break;
        }
    }

    /**
     * This method should be invoked by components that broadcast events on the pointerReleased callback.
     * This method will indicate if a drag occured since the pointer press event, notice that this method will not
     * behave as expected for multi-touch events.
     *
     * @return true if a drag has occured since the last pointer pressed
     */
    public boolean hasDragOccured() {
       return dragOccured;
    }

    private int[] pointerEvent(int off, int[] event) {
        int[] peX = new int[(event.length - 1) / 2];
        int offset = 0;
        for (int iter = off; iter < (event.length - 1); iter += 2) {
            peX[offset] = event[iter];
            offset++;
        }
        return peX;
    }

    /**
     * Returns true for a case where the EDT has nothing at all to do
     */
    boolean shouldEDTSleep() {
        Form current = impl.getCurrentForm();
        return (current == null || (!current.hasAnimations())) &&
                (animationQueue == null || animationQueue.size() == 0) &&
                inputEvents.size() == 0 &&
                (!impl.hasPendingPaints()) &&
                hasNoSerialCallsPending() && !keyRepeatCharged
                && !longPointerCharged;
    }


    Form getCurrentInternal() {
        return impl.getCurrentForm();
    }

    /**
     * Same as getCurrent with the added exception of looking into the future
     * transitions and returning the last current in the transition (the upcoming
     * value for current)
     *
     * @return the form currently displayed on the screen or null if no form is
     * currently displayed
     */
    Form getCurrentUpcoming() {
        return getCurrentUpcomingForm(false);
    }

    private Form getCurrentUpcomingForm(boolean includeMenus) {
        Form upcoming = null;

        // we are in the middle of a transition so we should extract the next form
        if(animationQueue != null) {
            int size = animationQueue.size();
            for(int iter = 0 ; iter < size ; iter++) {
                Object o = animationQueue.elementAt(iter);
                if(o instanceof Transition) {
                    upcoming = (Form)((Transition)o).getDestination();
                }
            }
        }
        if(upcoming == null) {
            if(includeMenus){
                Form f = impl.getCurrentForm();
                if(f instanceof Dialog) {
                    if(((Dialog)f).isDisposed()) {
                        return getCurrent();
                    }
                }
                return f;
            }else{
                return getCurrent();
            }
        }
        return upcoming;
    }

    /**
     * Return the form currently displayed on the screen or null if no form is
     * currently displayed.
     *
     * @return the form currently displayed on the screen or null if no form is
     * currently displayed
     */
    public Form getCurrent(){
        Form current = impl.getCurrentForm();
        if(current != null && current instanceof Dialog) {
            if(((Dialog)current).isMenu() || ((Dialog)current).isDisposed()) {
                Form p = current.getPreviousForm();
                if(p != null) {
                    return p;
                }

                // we are in the middle of a transition so we should extract the next form
                if(animationQueue != null) {
                    int size = animationQueue.size();
                    for(int iter = 0 ; iter < size ; iter++) {
                        Object o = animationQueue.elementAt(iter);
                        if(o instanceof Transition) {
                            return (Form)((Transition)o).getDestination();
                        }
                    }
                }
            }
        }
        return current;
    }

    /**
     * Return the number of alpha levels supported by the implementation.
     *
     * @return the number of alpha levels supported by the implementation
     */
    public int numAlphaLevels(){
        return impl.numAlphaLevels();
    }

    /**
     * Returns the number of colors applicable on the device, note that the API
     * does not support gray scale devices.
     *
     * @return the number of colors applicable on the device
     */
    public int numColors() {
        return impl.numColors();
    }

    /**
     * Return the width of the display
     *
     * @return the width of the display
     */
    public int getDisplayWidth(){
        return impl.getDisplayWidth();
    }

    /**
     * Return the height of the display
     *
     * @return the height of the display
     */
    public int getDisplayHeight(){
        return impl.getDisplayHeight();
    }

    /**
     * Causes the given component to repaint, used internally by Form
     *
     * @param cmp the given component to repaint
     */
    void repaint(final Animation cmp){
        impl.repaint(cmp);
    }

    /**
     * Converts the dips count to pixels, dips are roughly 1mm in length. This is a very rough estimate and not
     * to be relied upon
     * 
     * @param dipCount the dips that we will convert to pixels
     * @param horizontal indicates pixels in the horizontal plane
     * @return value in pixels
     */
    public int convertToPixels(int dipCount, boolean horizontal) {
        return impl.convertToPixels(dipCount, horizontal);
    }

    /**
     * Returns the game action code matching the given key combination
     *
     * @param keyCode key code received from the event
     * @return game action matching this keycode
     */
    public int getGameAction(int keyCode){
        return impl.getGameAction(keyCode);
    }

    /**
     * Returns the keycode matching the given game action constant (the opposite of getGameAction).
     * On some devices getKeyCode returns numeric keypad values for game actions,
     * this breaks the code since we filter these values (to prevent navigation on '2').
     * We pick unused negative values for game keys and assign them to game keys for
     * getKeyCode so they will work with getGameAction.
     *
     * @param gameAction game action constant from this class
     * @return keycode matching this constant
     * @deprecated this method doesn't work properly across device and is mocked up here
     * mostly for the case of unit testing. Do not use it for anything other than that! Do
     * not rely on getKeyCode(GAME_*) == keyCodeFromKeyEvent, this will never actually happen!
     */
    public int getKeyCode(int gameAction){
        return impl.getKeyCode(gameAction);
    }

    /**
     * Indicates whether the 3rd softbutton should be supported on this device
     *
     * @return true if a third softbutton should be used
     */
    public boolean isThirdSoftButton() {
        return thirdSoftButton;
    }

    /**
     * Indicates whether the 3rd softbutton should be supported on this device
     *
     * @param thirdSoftButton true if a third softbutton should be used
     */
    public void setThirdSoftButton(boolean thirdSoftButton) {
        this.thirdSoftButton = thirdSoftButton;
    }


    /**
     * Displays the virtual keyboard on devices that support manually poping up
     * the vitual keyboard
     *
     * @param show toggles the virtual keyboards visibility
     */
    public void setShowVirtualKeyboard(boolean show) {
        if(isTouchScreenDevice()){
            VirtualKeyboardInterface vkb = getDefaultVirtualKeyboard();
            if(vkb != null){
                vkb.showKeyboard(show);
            }
        }
    }

    /**
     * Indicates if the virtual keyboard is currently showing or not
     *
     * @return true if the virtual keyboard is showing
     */
    public boolean isVirtualKeyboardShowing() {
        if(!isTouchScreenDevice()){
            return false;
        }
        return getDefaultVirtualKeyboard() != null && getDefaultVirtualKeyboard().isVirtualKeyboardShowing();
    }

    /**
     * Returns all platform supported virtual keyboards names
     * @return all platform supported virtual keyboards names
     */
    public String [] getSupportedVirtualKeyboard(){
        String [] retVal = new String[virtualKeyboards.size()];
        int index = 0;
        Enumeration keys = virtualKeyboards.keys();
        while (keys.hasMoreElements()) {
            retVal[index++] = (String) keys.nextElement();
        }
        return retVal;
    }

    /**
     * Register a virtual keyboard
     * @param vkb
     */
    public void registerVirtualKeyboard(VirtualKeyboardInterface vkb){
        virtualKeyboards.put(vkb.getVirtualKeyboardName(), vkb);
    }

    /**
     * Sets the default virtual keyboard to be used by the platform
     *
     * @param vkb a VirtualKeyboard to be used or null to disable the
     * VirtualKeyboard
     */
    public void setDefaultVirtualKeyboard(VirtualKeyboardInterface vkb){
        if(vkb != null){
            selectedVirtualKeyboard = vkb.getVirtualKeyboardName();
            if(!virtualKeyboards.containsKey(selectedVirtualKeyboard)){
                registerVirtualKeyboard(vkb);
            }
        }else{
            selectedVirtualKeyboard = null;
        }
    }

    /**
     * Get the default virtual keyboard or null if the VirtualKeyboard is disabled
     * @return the default vkb
     */
    public VirtualKeyboardInterface getDefaultVirtualKeyboard(){
        if(selectedVirtualKeyboard == null){
            return null;
        }
        return (VirtualKeyboardInterface)virtualKeyboards.get(selectedVirtualKeyboard);
    }

    /**
     * Returns the type of the input device one of:
     * KEYBOARD_TYPE_UNKNOWN, KEYBOARD_TYPE_NUMERIC, KEYBOARD_TYPE_QWERTY,
     * KEYBOARD_TYPE_VIRTUAL, KEYBOARD_TYPE_HALF_QWERTY
     *
     * @return KEYBOARD_TYPE_UNKNOWN
     */
    public int getKeyboardType() {
        return impl.getKeyboardType();
    }

    /**
     * Indicates whether the device supports native in place editing in which case
     * lightweight input logic shouldn't be used for input.
     *
     * @return false by default
     */
    public boolean isNativeInputSupported() {
        return impl.isNativeInputSupported();
    }

    /**
     * Indicates whether the device supports multi-touch events, this is only
     * relevant when touch events are supported
     *
     * @return false by default
     */
    public boolean isMultiTouch() {
        return impl.isMultiTouch();
    }

    /**
     * Indicates whether the device has a double layer screen thus allowing two
     * stages to touch events: click and hover. This is true for devices such
     * as the storm but can also be true for a PC with a mouse pointer floating
     * on top.
     * <p>A click touch screen will also send pointer hover events to the underlying
     * software and will only send the standard pointer events on click.
     *
     * @return false by default
     */
    public boolean isClickTouchScreen() {
        return impl.isClickTouchScreen();
    }

    /**
     * This method returns the dragging speed based on the latest dragged
     * events
     * @param yAxis indicates what axis speed is required
     * @return the dragging speed
     */
    public float getDragSpeed(boolean yAxis){
        float speed;
        if(yAxis){
            speed = impl.getDragSpeed(dragPathY, dragPathTime, dragPathOffset, dragPathLength);
        }else{
            speed = impl.getDragSpeed(dragPathX, dragPathTime, dragPathOffset, dragPathLength);
        }
        return speed;
    }

    /**
     * Indicates whether Codename One should consider the bidi RTL algorithm
     * when drawing text or navigating with the text field cursor.
     *
     * @return true if the bidi algorithm should be considered
     */
    public boolean isBidiAlgorithm() {
        return impl.isBidiAlgorithm();
    }

    /**
     * Indicates whether Codename One should consider the bidi RTL algorithm
     * when drawing text or navigating with the text field cursor.
     *
     * @param activate set to true to activate the bidi algorithm, false to
     * disable it
     */
    public void setBidiAlgorithm(boolean activate) {
        impl.setBidiAlgorithm(activate);
    }

    /**
     * Converts the given string from logical bidi layout to visual bidi layout so
     * it can be rendered properly on the screen. This method is only necessary
     * for devices/platforms that don't have "built in" bidi support such as
     * Sony Ericsson devices.
     * See <a href="http://www.w3.org/International/articles/inline-bidi-markup/#visual">this</a>
     * for more on visual vs. logical ordering.
     *
     *
     * @param s a "logical" string with RTL characters
     * @return a "visual" renderable string
     */
    public String convertBidiLogicalToVisual(String s) {
        return impl.convertBidiLogicalToVisual(s);
    }

    /**
     * Returns the index of the given char within the source string, the actual
     * index isn't necessarily the same when bidi is involved
     * See <a href="http://www.w3.org/International/articles/inline-bidi-markup/#visual">this</a>
     * for more on visual vs. logical ordering.
     *
     * @param source the string in which we are looking for the position
     * @param index the "logical" location of the cursor
     * @return the "visual" location of the cursor
     */
	public int getCharLocation(String source, int index) {
        return impl.getCharLocation(source, index);
    }

    /**
     * Returns true if the given character is an RTL character
     *
     * @param c character to test
     * @return true if the charcter is an RTL character
     */
	public boolean isRTL(char c) {
        return impl.isRTL(c);
    }

    /**
     * This method is essentially equivalent to cls.getResourceAsStream(String)
     * however some platforms might define unique ways in which to load resources
     * within the implementation.
     *
     * @param cls class to load the resource from
     * @param resource relative/absolute URL based on the Java convention
     * @return input stream for the resource or null if not found
     */
    public InputStream getResourceAsStream(Class cls, String resource) {
        return impl.getResourceAsStream(cls, resource);
    }


    /**
     * An error handler will receive an action event with the source exception from the EDT
     * once an error handler is installed the default Codename One error dialog will no longer appear
     *
     * @param e listener receiving the errors
     */
    public void addEdtErrorHandler(ActionListener e) {
        if(errorHandler == null) {
            errorHandler = new EventDispatcher();
        }
        errorHandler.addListener(e);
    }

    /**
     * An error handler will receive an action event with the source exception from the EDT
     * once an error handler is installed the default Codename One error dialog will no longer appear
     *
     * @param e listener receiving the errors
     */
    public void removeEdtErrorHandler(ActionListener e) {
        if(errorHandler != null) {
            errorHandler.removeListener(e);
            Vector v = errorHandler.getListenerVector();
            if(v == null || v.size() == 0) {
                errorHandler = null;
            }
        }
    }

    /**
     * Allows a Codename One application to minimize without forcing it to the front whenever
     * a new dialog is poped up
     *
     * @param allowMinimizing value
     */
    public void setAllowMinimizing(boolean allowMinimizing) {
        this.allowMinimizing = allowMinimizing;
    }


    /**
     * Allows a Codename One application to minimize without forcing it to the front whenever
     * a new dialog is poped up
     *
     * @return allowMinimizing value
     */
    public boolean isAllowMinimizing() {
        return allowMinimizing;
    }

    /**
     * This is an internal state flag relevant only for pureTouch mode (otherwise it
     * will always be true). A pureTouch mode is stopped if a user switches to using
     * the trackball/navigation pad and this flag essentially toggles between those two modes.
     *
     * @return the shouldRenderSelection
     */
    public boolean shouldRenderSelection() {
        return !pureTouch || pointerPressedAndNotReleasedOrDragged || lastInteractionWasKeypad;
    }

    /**
     * This is an internal state flag relevant only for pureTouch mode (otherwise it
     * will always be true). A pureTouch mode is stopped if a user switches to using
     * the trackball/navigation pad and this flag essentially toggles between those two modes.
     *
     * @param c the component to test against, this prevents a touch outside of the component that triggers a repaint from painting the component selection
     * @return the shouldRenderSelection
     */
    public boolean shouldRenderSelection(Component c) {
        if(c.isCellRenderer()) {
            return shouldRenderSelection();
        }
        return !pureTouch || lastInteractionWasKeypad || (pointerPressedAndNotReleasedOrDragged && c.contains(pointerX, pointerY));
    }

    /**
     * A pure touch device has no focus showing when the user is using the touch
     * interface. Selection only shows when the user actually touches the screen
     * or suddenly switches to using a keypad/trackball. This sort of interface
     * is common in Android devices
     *
     * @return the pureTouch flag
     */
    public boolean isPureTouch() {
        return pureTouch;
    }

    /**
     * A pure touch device has no focus showing when the user is using the touch
     * interface. Selection only shows when the user actually touches the screen
     * or suddenly switches to using a keypad/trackball. This sort of interface
     * is common in Android devices
     *
     * @param pureTouch the value for pureTouch
     */
    public void setPureTouch(boolean pureTouch) {
        this.pureTouch = pureTouch;
    }

    /**
     * Indicates whether Codename One commands should be mapped to the native menus
     *
     * @return the nativeCommands status
     * @deprecated use getCommandBehavior() == Display.COMMAND_BEHAVIOR_NATIVE
     */
    public boolean isNativeCommands() {
        return getCommandBehavior() == COMMAND_BEHAVIOR_NATIVE;
    }

    /**
     * Indicates whether Codename One commands should be mapped to the native menus
     *
     * @param nativeCommands the flag to set
     * @deprecated use setCommandBehavior(Display.COMMAND_BEHAVIOR_NATIVE)
     */
    public void setNativeCommands(boolean nativeCommands) {
        setCommandBehavior(COMMAND_BEHAVIOR_NATIVE);
    }

    /**
     * Exits the application...
     */
    public void exitApplication() {
        codenameOneExited = true;
        impl.exitApplication();
    }

    /**
     * Shows a native Form/Canvas or some other heavyweight native screen
     *
     * @param nativeFullScreenPeer the native screen peer
     */
    public void showNativeScreen(Object nativeFullScreenPeer) {
        impl.showNativeScreen(nativeFullScreenPeer);
    }

    /**
     * Normally Codename One folds the VKB when switching forms this field allows us
     * to block that behavior.
     * @return the autoFoldVKBOnFormSwitch
     */
    public boolean isAutoFoldVKBOnFormSwitch() {
        return autoFoldVKBOnFormSwitch;
    }

    /**
     * Normally Codename One folds the VKB when switching forms this field allows us
     * to block that behavior.
     * @param autoFoldVKBOnFormSwitch the autoFoldVKBOnFormSwitch to set
     */
    public void setAutoFoldVKBOnFormSwitch(boolean autoFoldVKBOnFormSwitch) {
        this.autoFoldVKBOnFormSwitch = autoFoldVKBOnFormSwitch;
    }

    /**
     * Indicates the way commands should be added to a form as one of the ocmmand constants defined
     * in this class
     *
     * @return the commandBehavior
     */
    public int getCommandBehavior() {
        return commandBehavior;
    }

    /**
     * Indicates the way commands should be added to a form as one of the ocmmand constants defined
     * in this class
     *
     * @param commandBehavior the commandBehavior to set
     */
    public void setCommandBehavior(int commandBehavior) {
        if(!isTouchScreenDevice())
            if(commandBehavior == COMMAND_BEHAVIOR_BUTTON_BAR) {
                commandBehavior = COMMAND_BEHAVIOR_SOFTKEY;
            }
        this.commandBehavior = commandBehavior;
        impl.notifyCommandBehavior(commandBehavior);
    }

    /**
     * Returns the property from the underlying platform deployment or the default
     * value if no deployment values are supported. This is equivalent to the
     * getAppProperty from the jad file.
     * <p>The implementation should be responsible for the following keys to return
     * reasonable valid values for the application:
     * <ol>
     * <li>AppName
     * <li>UserAgent - ideally although not required
     * <li>AppVersion
     * <li>Platform - Similar to microedition.platform
     * <li>OS - returns what is the underlying platform e.g. - J2ME, RIM, SE...
     *
     * </ol>
     * @param key the key of the property
     * @param defaultValue a default return value
     * @return the value of the property
     */
    public String getProperty(String key, String defaultValue) {
        if(localProperties != null) {
            String v = (String)localProperties.get(key);
            if(v != null) {
                return v;
            }
        }
        return impl.getProperty(key, defaultValue);
    }

    /**
     * Sets a local property to the application, this method has no effect on the
     * implementation code and only allows the user to override the logic of getProperty
     * for internal application purposes.
     *
     * @param key key the key of the property
     * @param value the value of the property
     */
    public void setProperty(String key, String value) {
        if(localProperties == null) {
            localProperties = new Hashtable();
        }
        localProperties.put(key, value);
    }

    /**
     * Executes the given URL on the native platform
     *
     * @param url the url to execute
     */
    public void execute(String url) {
        impl.execute(url);
    }

    /**
     * Returns one of the density variables appropriate for this device, notice that
     * density doesn't alwyas correspond to resolution and an implementation might
     * decide to change the density based on DPI constraints.
     *
     * @return one of the DENSITY constants of Display
     */
    public int getDeviceDensity() {
        return impl.getDeviceDensity();
    }

    /**
     * Plays a builtin device sound matching the given identifier, implementations
     * and themes can offer additional identifiers to the ones that are already built
     * in.
     *
     * @param soundIdentifier the sound identifier which can match one of the
     * common constants in this class or be a user/implementation defined sound
     */
    public void playBuiltinSound(String soundIdentifier) {
        impl.playBuiltinSound(soundIdentifier);
    }

    /**
     * Installs a replacement sound as the builtin sound responsible for the given
     * sound identifier (this will override the system sound if such a sound exists).
     *
     * @param soundIdentifier the sound string passed to playBuiltinSound
     * @param data an input stream containing platform specific audio file, its usually safe
     * to assume that wav/mp3 would be supported.
     * @throws IOException if the stream throws an exception
     */
    public void installBuiltinSound(String soundIdentifier, InputStream data) throws IOException {
        impl.installBuiltinSound(soundIdentifier, data);
    }

    /**
     * Indicates whether a user installed or system sound is available
     *
     * @param soundIdentifier the sound string passed to playBuiltinSound
     * @return true if a sound of this given type is avilable
     */
    public boolean isBuiltinSoundAvailable(String soundIdentifier) {
        return impl.isBuiltinSoundAvailable(soundIdentifier);
    }

    /**
     * Allows muting/unmuting the builtin sounds easily
     *
     * @param enabled indicates whether the sound is muted
     */
    public void setBuiltinSoundsEnabled(boolean enabled) {
        impl.setBuiltinSoundsEnabled(enabled);
    }

    /**
     * Allows muting/unmuting the builtin sounds easily
     *
     * @return true if the sound is *not* muted
     */
    public boolean isBuiltinSoundsEnabled() {
        return impl.isBuiltinSoundsEnabled();
    }

    /**
     * Creates a sound in the given URI which is partially platform specific.
     * Notice that an audio is "auto destroyed" on completion and cannot be played
     * twice!
     *
     * @param uri the platform specific location for the sound
     * @param onCompletion invoked when the audio file finishes playing, may be null
     * @return a handle that can be used to control the playback of the audio
     * @throws java.io.IOException if the URI access fails
     */
    public Media createMedia(String uri, boolean isVideo, Runnable onCompletion) throws IOException {
        return impl.createMedia(uri, isVideo, onCompletion);
    }

    /**
     * Create the sound in the given stream
     * Notice that an audio is "auto destroyed" on completion and cannot be played
     * twice!
     *
     * @param stream the stream containing the media data
     * @param mimeType the type of the data in the stream
     * @param onCompletion invoked when the audio file finishes playing, may be null
     * @return a handle that can be used to control the playback of the audio
     * @throws java.io.IOException if the URI access fails
     */
    public Media createMedia(InputStream stream, String mimeType, Runnable onCompletion) throws IOException {
        return impl.createMedia(stream, mimeType, onCompletion);
    }


    /**
     * Creates a soft/weak reference to an object that allows it to be collected
     * yet caches it. This method is in the porting layer since CLDC only includes
     * weak references while some platforms include nothing at all and some include
     * the superior soft references.
     *
     * @param o object to cache
     * @return a caching object or null  if caching isn't supported
     */
    public Object createSoftWeakRef(Object o) {
        return impl.createSoftWeakRef(o);
    }

    /**
     * Extracts the hard reference from the soft/weak reference given
     *
     * @param o the reference returned by createSoftWeakRef
     * @return the original object submitted or null
     */
    public Object extractHardRef(Object o) {
        return impl.extractHardRef(o);
    }

    /**
     * Indicates if the implemenetation has a native underlying theme
     *
     * @return true if the implementation has a native theme available
     */
    public boolean hasNativeTheme() {
        return impl.hasNativeTheme();
    }

    /**
     * Installs the native theme, this is only applicable if hasNativeTheme() returned true. Notice that this method
     * might replace the DefaultLookAndFeel instance and the default transitions.
     */
    public void installNativeTheme() {
        impl.installNativeTheme();
    }

    /**
     * Performs a clipboard copy operation, if the native clipboard is supported by the implementation it would be used
     *
     * @param obj object to copy, while this can be any arbitrary object it is recommended that only Strings or Codename One
     * image objects be used to copy
     */
    public void copyToClipboard(Object obj) {
        impl.copyToClipboard(obj);
    }

    /**
     * Returns the current content of the clipboard
     *
     * @return can be any object or null see copyToClipboard
     */
    public Object getPasteDataFromClipboard() {
        return impl.getPasteDataFromClipboard();
    }

    /**
     * Returns true if the device is currently in portrait mode
     *
     * @return true if the device is in portrait mode
     */
    public boolean isPortrait() {
        return impl.isPortrait();
    }

    /**
     * Returns true if the device allows forcing the orientation via code, feature phones do not allow this
     * although some include a jad property allowing for this feature
     *
     * @return true if lockOrientation  would work
     */
    public boolean canForceOrientation() {
        return impl.canForceOrientation();
    }

    /**
     * On devices that return true for canForceOrientation() this method can lock the device orientation
     * either to portrait or landscape mode
     *
     * @param portrait true to lock to portrait mode, false to lock to landscape mode
     */
    public void lockOrientation(boolean portrait) {
        impl.lockOrientation(portrait);
    }

    /**
     * Indicates whether the device is a tablet, notice that this is often a guess
     *
     * @return true if the device is assumed to be a tablet
     */
    public boolean isTablet() {
        return impl.isTablet();
    }

    /**
     * On most platforms it is quite fast to draw on a mutable image and then render that
     * image, however some platforms have much slower mutable images in comparison to just
     * drawing on the screen. These platforms should return false here and Codename One will try
     * to use less mutable image related optimizations in transitions and other operations.
     * 
     * @return true if mutable images are fast on this platform
     */
    public boolean areMutableImagesFast() {
        return impl.areMutableImagesFast();
    }

    /**
     * This method returns the platform Location Control
     * @return LocationManager Object
     */
    public LocationManager getLocationManager() {
        return impl.getLocationManager();
    }

    public void capturePhoto(ActionListener response){    
        impl.capturePhoto(response);
    }

    public void captureAudio(ActionListener response) {
        impl.captureAudio(response);
    }

    public void captureVideo(ActionListener response) {
        impl.captureVideo(response);
    }

    /**
     * Returns a 2-3 letter code representing the platform name for the platform override
     * 
     * @return the name of the platform e.g. ios, rim, win, and, me
     */
    public String getPlatformName() {
        return impl.getPlatformName();
    }    

    /**
     * Returns the suffixes for ovr files that should be used when loading a layered resource file on this platform
     * 
     * @return a string array with the proper order of resource override layers
     */
    public String[] getPlatformOverrides() {
        return impl.getPlatformOverrides();
    }

      /**
     * Send an email using the platform mail client
     * @param recieptents array of e-mail addresses
     * @param subject e-mail subject
     * @param msg the Message to send
     */
    public void sendMessage(String[] recieptents, String subject, Message msg) {
        impl.sendMessage(recieptents, subject, msg);
    }
    
    /**
     * Opens the device Dialer application with the given phone number
     * @param phoneNumber 
     */
    public void dial(String phoneNumber) {
        impl.dial(phoneNumber);
    }    
}
