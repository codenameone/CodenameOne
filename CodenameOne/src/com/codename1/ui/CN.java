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

import com.codename1.io.ConnectionRequest;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.io.NetworkEvent;
import com.codename1.io.NetworkManager;
import com.codename1.io.Storage;
import com.codename1.messaging.Message;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.MessageEvent;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.plaf.Style;
import com.codename1.util.RunnableWithResultSync;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Timer;

/**
 * This is a global context static class designed for static import, this class allows us to write more 
 * terse code. However, its chief purpose is simplification by hiding some of the more esoteric methods of 
 * these underlying classes and bringing to the front the commonly used important methods.
 * This class includes code from Display, NetworkManager, Log and other important classes
 *
 * @author Shai Almog
 */
public class CN extends  CN1Constants {
    /**
     * Constant for the name of the main thin native font.
     */
    public static final String NATIVE_MAIN_THIN="native:MainThin";
    
    /**
     * Constant for the main light native font.
     */
    public static final String NATIVE_MAIN_LIGHT="native:MainLight";
    
    /**
     * Constant for the main regular native font.
     */
    public static final String NATIVE_MAIN_REGULAR="native:MainRegular";
    
    /**
     * Constant for the main bold native font.
     */
    public static final String NATIVE_MAIN_BOLD="native:MainBold";
    
    /**
     * Constant for the main black native font.
     */
    public static final String NATIVE_MAIN_BLACK="native:MainBlack";
    
    /**
     * Constant for the italic thin native font.
     */
    public static final String NATIVE_ITALIC_THIN="native:ItalicThin";
    
    /**
     * Constant for the italic light native font.
     */
    public static final String NATIVE_ITALIC_LIGHT="native:ItalicLight";
    
    /**
     * Constant for the italic regular native font.
     */
    public static final String NATIVE_ITALIC_REGULAR ="native:ItalicRegular";
    
    /**
     * Constant for the italic bold native font.
     */
    public static final String NATIVE_ITALIC_BOLD="native:ItalicBold";
    
    /**
     * Constant for the italic black native font.
     */
    public static final String NATIVE_ITALIC_BLACK="native:ItalicBlack";
    
    /**
     * Constant allowing us to author portable system fonts
     */
    public static final int FACE_MONOSPACE = 32;

    /**
     * Constant allowing us to author portable system fonts
     */
    public static final int FACE_PROPORTIONAL = 64;

    /**
     * Constant allowing us to author portable system fonts
     */
    public static final int FACE_SYSTEM = 0;

    /**
     * Constant allowing us to author portable system fonts
     */
    public static final int SIZE_LARGE = 16;

    /**
     * Constant allowing us to author portable system fonts
     */
    public static final int SIZE_MEDIUM = 0;

    /**
     * Constant allowing us to author portable system fonts
     */
    public static final int SIZE_SMALL = 8;

    /**
     * Constant allowing us to author portable system fonts
     */
    public static final int STYLE_BOLD = 1;

    /**
     * Constant allowing us to author portable system fonts
     */
    public static final int STYLE_ITALIC = 2;
    
    /**
     * Constant allowing us to author portable system fonts
     */
    public static final int STYLE_UNDERLINED = 4;
    
    /**
     * Constant allowing us to author portable system fonts
     */
    public static final int STYLE_PLAIN = 0;

    /**
     * The north layout constraint (top of container).
     */
    public static final String NORTH = "North";
    /**
     * The south layout constraint (bottom of container).
     */
    public static final String SOUTH = "South";
    
    /**
     * The west layout constraint (left of container).
     */
    public static final String WEST = "West";
    /**
     * The east layout constraint (right of container).
     */
    public static final String EAST = "East";

    /**
     * Indicates a Component center alignment
     */
    public static final int CENTER = 4;
    /** 
     * Box-orientation constant used to specify the top of a box.
     */
    public static final int TOP = 0;
    /** 
     * Box-orientation constant used to specify the left side of a box.
     */
    public static final int LEFT = 1;
    /** 
     * Box-orientation constant used to specify the bottom of a box.
     */
    public static final int BOTTOM = 2;
    /** 
     * Box-orientation constant used to specify the right side of a box.
     */
    public static final int RIGHT = 3;
    
    /**
     * Alignment to the baseline constraint
     */
    public static final int BASELINE = 5;
    
    /**
     * Defines the behavior of the component placed in the center position of the layout, by default it is scaled to the available space
     */
    public static final int CENTER_BEHAVIOR_SCALE = 0;

    /**
     * Defines the behavior of the component placed in the center position of the layout, places the component in the center of
     * the space available to the center component.
     */
    public static final int CENTER_BEHAVIOR_CENTER = 1;


    /**
     * Defines the behavior of the component placed in the center position of the layout, places the component in the center of
     * the surrounding container
     */
    public static final int CENTER_BEHAVIOR_CENTER_ABSOLUTE = 2;

    /**
     * The center component takes up the entire screens and the sides are automatically placed on top of it thus creating
     * a layered effect
     */
    public static final int CENTER_BEHAVIOR_TOTAL_BELOW = 3;
    
    CN() {}

    /**
     * Sets a bookmark that can restore the app to a particular state.  This takes a
     * {@link Runnable} that will be run when {@link #restoreToBookmark()} () } is called.
     *
     * <p>The primary purpose of this feature is live code refresh.</p>
     * @param bookmark A {@link Runnable} that can be run to restore the app to a particular point.
     * @since 8.0
     *
     */
    public static void setBookmark(Runnable bookmark) {
        Display.getInstance().setBookmark(bookmark);
    }

    /**
     * Runs the last bookmark that was set using {@link #setBookmark(java.lang.Runnable) }
     *
     * @since 8.0
     */
    public static void restoreToBookmark() {
        Display.getInstance().restoreToBookmark();
    }
    
    /**
     * This method allows us to manipulate the drag started detection logic.
     * If the pointer was dragged for more than this percentage of the display size it
     * is safe to assume that a drag is in progress.
     *
     * @return motion percentage
     */
    public static int getDragStartPercentage() {
        return Display.impl.getDragStartPercentage();
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
    public static Object createSoftWeakRef(Object o) {
        return Display.impl.createSoftWeakRef(o);
    }

    /**
     * Extracts the hard reference from the soft/weak reference given
     *
     * @param o the reference returned by createSoftWeakRef
     * @return the original object submitted or null
     */
    public static Object extractHardRef(Object o) {
        return Display.impl.extractHardRef(o);
    }

    /**
     * Checks if async stack traces are enabled.  If enabled, the stack trace
     * at the point of {@link #callSerially(java.lang.Runnable) } calls will 
     * be recorded, and logged in the case that there is an uncaught exception.
     * <p>Currently this is only supported in the JavaSE/Simulator port.</p>
     * @return Whether async stack traces are enabled.  
     * @see #setEnableAsyncStackTraces(boolean) 
     * @since 7.0
     */
    public static boolean isEnableAsyncStackTraces() {
        return Display.getInstance().isEnableAsyncStackTraces();
    }
    
    /**
     * Enables or disables async stack traces.  If enabled, the stack trace
     * at the point of {@link #callSerially(java.lang.Runnable) } calls will 
     * be recorded, and logged in the case that there is an uncaught exception.
     * 
     * <p>Currently this is only supported in the JavaSE/Simulator port.</p>
     * @param enable True to enable async stack traces.  
     * @see #isEnableAsyncStackTraces() 
     * @since 7.0
     */
    public static void setEnableAsyncStackTraces(boolean enable) {
        Display.getInstance().setEnableAsyncStackTraces(enable);
    }
    
    /**
     * This method allows us to manipulate the drag started detection logic.
     * If the pointer was dragged for more than this percentage of the display size it
     * is safe to assume that a drag is in progress.
     *
     * @param dragStartPercentage percentage of the screen required to initiate drag
     */
    public static void setDragStartPercentage(int dragStartPercentage) {
        Display.impl.setDragStartPercentage(dragStartPercentage);
    }

    /**
     * Vibrates the device for the given length of time, notice that this might ignore the time value completely 
     * on some OS's where this level of control isn't supported e.g. iOS see: https://github.com/codenameone/CodenameOne/issues/1904
     *
     * @param duration length of time to vibrate (might be ignored)
     */
    public static void vibrate(int duration) {
        Display.impl.vibrate(duration);
    }


    /**
     * Returns true if we are currently in the event dispatch thread.
     * This is useful for generic code that can be used both with the
     * EDT and outside of it.
     *
     * @return true if we are currently in the event dispatch thread;
     * otherwise false
     */
    public static boolean isEdt() {
        return Display.INSTANCE.isEdt();
    }


    /**
     * Causes the runnable to be invoked on the event dispatch thread. This method
     * returns immediately and will not wait for the serial call to occur
     *
     * @param r runnable (NOT A THREAD!) that will be invoked on the EDT serial to
     * the paint and key handling events
     */
    public static void callSerially(Runnable r){
        Display.INSTANCE.callSerially(r);
    }

    /**
     * Causes the runnable to be invoked on the event dispatch thread when the event 
     * dispatch thread is idle. This method returns immediately and will not wait for the serial call 
     * to occur. Notice this method is identical to call serially but will perform the runnable only when
     * the EDT is idle
     *
     * @param r runnable (NOT A THREAD!) that will be invoked on the EDT serial to
     * the paint and key handling events
     */
    public static void callSeriallyOnIdle(Runnable r){
        Display.INSTANCE.callSeriallyOnIdle(r);
    }    
    
    /**
     * Allows executing a background task in a separate low priority thread. Tasks are serialized
     * so they don't overload the CPU.
     * 
     * @param r the task to perform in the background
     */
    public static void scheduleBackgroundTask(Runnable r) {
        Display.INSTANCE.scheduleBackgroundTask(r);
    }
    

    /**
     * Identical to callSerially with the added benefit of waiting for the Runnable method to complete.
     *
     * @param r runnable (NOT A THREAD!) that will be invoked on the EDT serial to
     * the paint and key handling events
     * @throws IllegalStateException if this method is invoked on the event dispatch thread (e.g. during
     * paint or event handling).
     */
    public static void callSeriallyAndWait(Runnable r){
        Display.INSTANCE.callSeriallyAndWait(r);
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
    public static void callSeriallyAndWait(Runnable r, int timeout){
        Display.INSTANCE.callSeriallyAndWait(r, timeout);
    }

    /**
     * Invokes runnable and blocks the current thread, if the current thread is the
     * EDT it will still be blocked in a way that doesn't break event dispatch .
     * <b>Important:</b> calling this method spawns a new thread that shouldn't access the UI!<br />
     * See <a href="https://www.codenameone.com/manual/edt.html#_invoke_and_block">
     * this section</a> in the developer guide for further information.
     *
     * @param r runnable (NOT A THREAD!) that will be invoked synchroniously by this method
     */
    public static void invokeAndBlock(Runnable r){
        Display.INSTANCE.invokeAndBlock(r);
    }
    
    /**
     * Invokes a Runnable with blocking disabled.  If any attempt is made to block
     * (i.e. call {@link #invokeAndBlock(java.lang.Runnable) } from inside this Runnable,
     * it will result in a {@link BlockingDisallowedException} being thrown.
     * @param r Runnable to be run immediately.
     * @throws BlockingDisallowedException If {@link #invokeAndBlock(java.lang.Runnable) } is attempted
     * anywhere in the Runnable.
     * 
     * @since 7.0
     */
    public static void invokeWithoutBlocking(Runnable r) {
        Display.INSTANCE.invokeWithoutBlocking(r);
    }
    
    /**
     * Invokes a RunnableWithResultSync with blocking disabled.  If any attempt is made to block
     * (i.e. call {@link #invokeAndBlock(java.lang.Runnable) } from inside this Runnable,
     * it will result in a {@link BlockingDisallowedException} being thrown.
     * @param r Runnable to be run immediately.
     * @throws BlockingDisallowedException If {@link #invokeAndBlock(java.lang.Runnable) } is attempted
     * anywhere in the Runnable.
     * 
     * @since 7.0
     */
    public static <T> T invokeWithoutBlockingWithResultSync(RunnableWithResultSync<T> r) {
        return Display.INSTANCE.invokeWithoutBlockingWithResultSync(r);
    }

    /**
     * Minimizes the current application if minimization is supported by the platform (may fail).
     * Returns false if minimization failed.
     *
     * @return false if minimization failed true if it succeeded or seems to be successful
     */
    public static boolean minimizeApplication() {
        return  Display.impl.minimizeApplication();
    }

    /**
     * Indicates whether an application is minimized
     *
     * @return true if the application is minimized
     */
    public static boolean isMinimized() {
        return Display.impl.isMinimized();
    }

    /**
     * Restore the minimized application if minimization is supported by the platform
     */
    public static void restoreMinimizedApplication() {
        Display.impl.restoreMinimizedApplication();
    }

    /**
     * Return the form currently displayed on the screen or null if no form is
     * currently displayed.
     *
     * @return the form currently displayed on the screen or null if no form is
     * currently displayed
     */
    public static Form getCurrentForm() {
        return Display.INSTANCE.getCurrent();
    }

    /**
     * Return the width of the display
     *
     * @return the width of the display
     */
    public static int getDisplayWidth(){
        return Display.impl.getDisplayWidth();
    }

    /**
     * Return the height of the display
     *
     * @return the height of the display
     */
    public static int getDisplayHeight(){
        return Display.impl.getDisplayHeight();
    }


    /**
     * Converts the dips count to pixels, dips are roughly 1mm in length. This is a very rough estimate and not
     * to be relied upon
     * 
     * @param dipCount the dips that we will convert to pixels
     * @param horizontal indicates pixels in the horizontal plane
     * @return value in pixels
     */
    public static int convertToPixels(int dipCount, boolean horizontal) {
        return Display.impl.convertToPixels(dipCount, horizontal);
    }

    /**
     * Converts from specified unit to pixels.
     * @param value The value to convert, expressed in unitType.
     * @param unitType The unit type.  One of {@link Style#UNIT_TYPE_DIPS}, {@link Style#UNIT_TYPE_PIXELS},
     * {@link Style#UNIT_TYPE_REM}, {@link Style#UNIT_TYPE_SCREEN_PERCENTAGE}, {@link Style#UNIT_TYPE_VH},
     * {@link Style#UNIT_TYPE_VW}, {@link Style#UNIT_TYPE_VMIN}, {@link Style#UNIT_TYPE_VMAX}
     * @param horizontal Whether screen percentage units should be based on horitonzal or vertical percentage.
     * @return The value converted to pixels.
     * @since 8.0
     */
    public static int convertToPixels(float value, byte unitType, boolean horizontal) {
        return Display.INSTANCE.convertToPixels(value, unitType, horizontal);
    }

    /**
     * Converts from specified unit to pixels.
     * @param value The value to convert, expressed in unitType.
     * @param unitType The unit type.  One of {@link Style#UNIT_TYPE_DIPS}, {@link Style#UNIT_TYPE_PIXELS},
     * {@link Style#UNIT_TYPE_REM}, {@link Style#UNIT_TYPE_SCREEN_PERCENTAGE}, {@link Style#UNIT_TYPE_VH},
     * {@link Style#UNIT_TYPE_VW}, {@link Style#UNIT_TYPE_VMIN}, {@link Style#UNIT_TYPE_VMAX}
     * @return The value converted to pixels.
     * @since 8.0
     */
    public static int convertToPixels(float value, byte unitType) {
        return Display.INSTANCE.convertToPixels((float)value, unitType);
    }


    /**
     * Converts the dips count to pixels, dips are roughly 1mm in length. This is a very rough estimate and not
     * to be relied upon. This version of the method assumes square pixels which is pretty much the norm.
     * 
     * @param dipCount the dips that we will convert to pixels
     * @return value in pixels
     */
    public static int convertToPixels(float dipCount) {
        return Math.round(Display.impl.convertToPixels((int)(dipCount * 1000), true) / 1000.0f);
    }

    /**
     * This method is essentially equivalent to cls.getResourceAsStream(String)
     * however some platforms might define unique ways in which to load resources
     * within the implementation.
     *
     * @param resource relative/absolute URL based on the Java convention
     * @return input stream for the resource or null if not found
     */
    public static InputStream getResourceAsStream(String resource) {
        return Display.impl.getResourceAsStream(CN.class, resource);
    }


    /**
     * An error handler will receive an action event with the source exception from the EDT
     * once an error handler is installed the default Codename One error dialog will no longer appear
     *
     * @param e listener receiving the errors
     */
    public static void addEdtErrorHandler(ActionListener e) {
        Display.INSTANCE.addEdtErrorHandler(e);
    }

    /**
     * An error handler will receive an action event with the source exception from the EDT
     * once an error handler is installed the default Codename One error dialog will no longer appear
     *
     * @param e listener receiving the errors
     */
    public static void removeEdtErrorHandler(ActionListener e) {
        Display.INSTANCE.removeEdtErrorHandler(e);
    }

    /**
     * Exits the application...
     */
    public static void exitApplication() {
        Display.INSTANCE.exitApplication();
    }

    /**
     * Returns the property from the underlying platform deployment or the default
     * value if no deployment values are supported. This is equivalent to the
     * getAppProperty from the jad file.
     * <p>The implementation should be responsible for the following keys to return
     * reasonable valid values for the application:
     * <ol>
     * <li>AppName
     * <li>User-Agent
     * <li>AppVersion
     * <li>Platform - Similar to microedition.platform
     * <li>OS - returns what is the underlying platform e.g. - iOS, Android, RIM, SE...
     * <li>OSVer - OS version when available as a user readable string (not necessarily a number e.g: 3.2.1).
     *
     * </ol>
     * @param key the key of the property
     * @param defaultValue a default return value
     * @return the value of the property
     */
    public static String getProperty(String key, String defaultValue) {
        return Display.INSTANCE.getProperty(key, defaultValue);
    }

    /**
     * Sets a local property to the application, this method has no effect on the
     * implementation code and only allows the user to override the logic of getProperty
     * for internal application purposes.
     *
     * @param key key the key of the property
     * @param value the value of the property
     */
    public static void setProperty(String key, String value) {
        Display.INSTANCE.setProperty(key, value);
    }
    
    /**
     * <p>Returns true if executing this URL should work, returns false if it will not
     * and null if this is unknown.</p>
     * <script src="https://gist.github.com/codenameone/7aefb64909e75e10c396.js"></script>
     * 
     * @param url the url that would be executed
     * @return true if executing this URL should work, returns false if it will not
     * and null if this is unknown
     */
    public static Boolean canExecute(String url) {
        return Display.impl.canExecute(url);
    }

    /**
     * <p>Executes the given URL on the native platform</p>
     * <script src="https://gist.github.com/codenameone/7aefb64909e75e10c396.js"></script>
     *
     * @param url the url to execute
     */
    public static void execute(String url) {
        Display.impl.execute(url);
    }
    
    
    /**
     * Returns one of the density variables appropriate for this device, notice that
     * density doesn't always correspond to resolution and an implementation might
     * decide to change the density based on DPI constraints.
     *
     * @return one of the DENSITY constants of Display
     */
    public static int getDeviceDensity() {
        return Display.impl.getDeviceDensity();
    }

    /**
     * Returns true if the device is currently in portrait mode
     *
     * @return true if the device is in portrait mode
     */
    public static boolean isPortrait() {
        return Display.impl.isPortrait();
    }
    
    /**
     * Try to enter full-screen mode if the platform supports it.
     * 
     * <p>Currently only desktop and Javascript builds support full-screen mode; And Javascript
     * only supports this on certain browsers.  See the <a href="https://developer.mozilla.org/en-US/docs/Web/API/Fullscreen_API">MDN Fullscreen API docs</a>
     * for a list of browsers that support full-screen.</p>
     * 
     * <p>When running in the simulator, full-screen is only supported for the desktop skin.</p>
     * 
     * @return {@literal true} on success.  This will also return {@literal true} if the app is already running in full-screen mode.  It will return {@literal false}
     * if the app fails to enter full-screen mode.
     * @see #exitFullScreen() 
     * @see #isInFullScreenMode() 
     * @see #isFullScreenSupported() 
     * @since 6.0
     */
    public static boolean requestFullScreen() {
        return Display.impl.requestFullScreen();
    }
    
    /**
     * Try to exit full-screen mode if the platform supports it.
     * 
     * <p>Currently only desktop and Javascript builds support full-screen mode; And Javascript
     * only supports this on certain browsers.  See the <a href="https://developer.mozilla.org/en-US/docs/Web/API/Fullscreen_API">MDN Fullscreen API docs</a>
     * for a list of browsers that support full-screen.</p>
     * 
     * <p>When running in the simulator, full-screen is only supported for the desktop skin.</p>
     * 
     * @return {@literal true} on success.  This will also return {@literal true} if the app is already NOT in full-screen mode.  It will return {@literal false}
     * if the app fails to exit full-screen mode.
     * @see #requestFullScreen() 
     * @see #isInFullScreenMode() 
     * @see #isFullScreenSupported() 
     * @since 6.0
     */
    public static boolean exitFullScreen() {
        return Display.impl.exitFullScreen();
    }
    
    /**
     * Checks if the app is currently running in full-screen mode.
     * @return {@literal true} if the app is currently in full-screen mode.
     * @since 6.0
     * @see #requestFullScreen() 
     * @see #exitFullScreen() 
     * @see #isFullScreenSupported() 
     */
    public static boolean isInFullScreenMode() {
        return Display.impl.isInFullScreenMode();
    }
    
    /**
     * Checks if this platform supports full-screen mode.  If full-screen mode is supported, you can use
     * the {@link #requestFullScreen() }, {@link #exitFullScreen() }, and {@link #isInFullScreenMode() } methods
     * to enter and exit full-screen - and query the current state.
     * 
     * <p>Currently only desktop and Javascript builds support full-screen mode; And Javascript
     * only supports this on certain browsers.  See the <a href="https://developer.mozilla.org/en-US/docs/Web/API/Fullscreen_API">MDN Fullscreen API docs</a>
     * for a list of browsers that support full-screen.</p>
     * 
     * <p>When running in the simulator, full-screen is only supported for the desktop skin.</p>
     * @return {@literal true} if Full-screen mode is supported on this platform.
     * @since 6.0
     * @see #requestFullScreen() 
     * @see #exitFullScreen() 
     * @see #isInFullScreenMode() 
     */
    public static boolean isFullScreenSupported() {
        return Display.impl.isFullScreenSupported();
    }

    /**
     * Returns true if the device allows forcing the orientation via code, feature phones do not allow this
     * although some include a jad property allowing for this feature
     *
     * @return true if lockOrientation  would work
     */
    public static boolean canForceOrientation() {
        return Display.impl.canForceOrientation();
    }

    /**
     * On devices that return true for canForceOrientation() this method can lock the device orientation
     * either to portrait or landscape mode
     *
     * @param portrait true to lock to portrait mode, false to lock to landscape mode
     */
    public static void lockOrientation(boolean portrait) {
        Display.impl.lockOrientation(portrait);
    }

    /**
     * This is the reverse method for lock orientation allowing orientation lock to be disabled
     */
    public static void unlockOrientation() {
        Display.impl.unlockOrientation();
    }
    
    /**
     * Indicates whether the device is a tablet, notice that this is often a guess
     *
     * @return true if the device is assumed to be a tablet
     */
    public static boolean isTablet() {
        return Display.impl.isTablet();
    }
    
    /**
     * Returns true if this is a desktop application
     * @return true if this is a desktop application
     */
    public static boolean isDesktop() {
        return Display.impl.isDesktop();
    }
    
    /**
     * Returns true if the device has dialing capabilities
     * @return false if it cannot dial
     */
    public static boolean canDial() {
        return Display.impl.canDial();
    }
    
    /**
     * Returns true if the platform is in dark mode, null is returned for
     * unknown status
     * 
     * @return true in case of dark mode
     */    
    public static Boolean isDarkMode() {
        return Display.INSTANCE.isDarkMode();
    }
    
    /**
     * Override the default dark mode setting
     * @param darkMode can be set to null to reset to platform default
     */
    public static void setDarkMode(Boolean darkMode) {
        Display.INSTANCE.setDarkMode(darkMode);
    }
    
    /**
     * <p>Opens the device gallery to pick an image or a video.<br>
     * The method returns immediately and the response is sent asynchronously
     * to the given ActionListener Object as the source value of the event (as a String)</p>
     * 
     * <p>E.g. within the callback action performed call you can use this code: {@code String path = (String) evt.getSource();}.<br>
     * A more detailed sample of picking a video file can be seen here:
     * </p>
     * 
     * <script src="https://gist.github.com/codenameone/fb73f5d47443052f8956.js"></script>
     * <img src="https://www.codenameone.com/img/developer-guide/components-mediaplayer.png" alt="Media player sample" />
     * 
     * @param response a callback Object to retrieve the file path
     * @param type one of the following {@link #GALLERY_IMAGE}, {@link #GALLERY_VIDEO}, {@link #GALLERY_ALL}
     * @throws RuntimeException if this feature failed or unsupported on the platform
     */
    public static void openGallery(ActionListener response, int type){
        Display.impl.openGallery(response, type);
    }

    /**
     * Returns a 2-3 letter code representing the platform name for the platform override
     * 
     * @return the name of the platform e.g. ios, rim, win, and, me, HTML5
     */
    public static String getPlatformName() {
        return Display.impl.getPlatformName();
    }    

    
    /**
     * Opens the device Dialer application with the given phone number
     * @param phoneNumber 
     */
    public static void dial(String phoneNumber) {
        Display.impl.dial(phoneNumber);
    }    
    
    /**
     * <p>Indicates the level of SMS support in the platform as one of: 
     * {@link #SMS_NOT_SUPPORTED} (for desktop, tablet etc.), 
     * {@link #SMS_SEAMLESS} (no UI interaction), {@link #SMS_INTERACTIVE} (with compose UI), 
     * {@link #SMS_BOTH}.<br>
     * The sample below demonstrates the use case for this property:
     * </p>
     * <script src="https://gist.github.com/codenameone/da23d33b1a9e105efffd.js"></script>
     * 
     * @return one of the SMS_* values
     */
    public static int getSMSSupport() {
        return Display.impl.getSMSSupport();
    }

    
    /**
     * Sends a SMS message to the given phone number
     * @param phoneNumber to send the sms
     * @param message the content of the sms
     */
    public static void sendSMS(String phoneNumber, String message) throws IOException{
        Display.impl.sendSMS(phoneNumber, message, false);
    }
    
    /**
     * <p>Sends a SMS message to the given phone number, the code below demonstrates the logic
     * of detecting platform behavior for sending SMS.</p>
     * <script src="https://gist.github.com/codenameone/da23d33b1a9e105efffd.js"></script>
     * 
     * @see #getSMSSupport() 
     * @param phoneNumber to send the sms
     * @param message the content of the sms
     * @param interactive indicates the SMS should show a UI or should not show a UI if applicable see getSMSSupport
     */
    public static void sendSMS(String phoneNumber, String message, boolean interactive) throws IOException{
        Display.impl.sendSMS(phoneNumber, message, interactive);
    }
        
    
    
    /**
     * Share the required information using the platform sharing services.
     * a Sharing service can be: mail, sms, facebook, twitter,...
     * This method is implemented if isNativeShareSupported() returned true for 
     * a specific platform.
     * 
     * <p>Since 6.0, there is native sharing support in the Javascript port using the <a href="https://developer.mozilla.org/en-US/docs/Web/API/Navigator/share">navigator.share</a>
     * API.  Currently (2019) this is only supported on Chrome for Android, and will only work if the app is accessed over https:.</p>
     * 
     * @param text String to share.
     * @param image file path to the image or null
     * @param mimeType type of the image or null if no image to share
     */
    public static void share(String text, String image, String mimeType){
        Display.INSTANCE.share(text, image, mimeType);        
    }
    
    /**
     * Indicates if the underlying platform supports sharing capabilities
     * 
     * <p>Since 6.0, there is native sharing support in the Javascript port using the <a href="https://developer.mozilla.org/en-US/docs/Web/API/Navigator/share">navigator.share</a>
     * API.  Currently (2019) this is only supported on Chrome for Android, and will only work if the app is accessed over https:.</p>
     * 
     * @return true if the underlying platform handles share.
     */
    public static boolean isNativeShareSupported(){
        return Display.impl.isNativeShareSupported();        
    }   
    
   /**
     * Share the required information using the platform sharing services.
     * a Sharing service can be: mail, sms, facebook, twitter,...
     * This method is implemented if isNativeShareSupported() returned true for 
     * a specific platform.
     * 
     * @param text String to share.
     * @param image file path to the image or null
     * @param mimeType type of the image or null if no image to share
     * @param sourceRect The source rectangle of the button that originated the share request.  This is used on
     * some platforms to provide a hint as to where the share dialog overlay should pop up.  Particularly,
     * on the iPad with iOS 8 and higher.
     */
    public static void share(String text, String image, String mimeType, Rectangle sourceRect){
        Display.INSTANCE.share(text, image, mimeType, sourceRect);
    }
    
        /**
     * Register to receive push notification, invoke this method once (ever) to receive push
     * notifications.
     */
    public static void registerPush() {
        Display.impl.registerPush(new Hashtable(), false);
    }
    
    /**
     * Stop receiving push notifications to this client application
     */
    public static void deregisterPush() {
        Display.impl.deregisterPush();
    }

    /**
     * Start a Codename One thread that supports crash protection and similar Codename One features.
     * @param r runnable to run, <b>NOTICE</b> the thread MUST be explicitly started!
     * @param name the name for the thread
     * @return a thread instance which must be explicitly started!
     */
    public static Thread createThread(Runnable r, String name) {
        return Display.INSTANCE.createThread(r, name);
    }
    
    
    /**
     * Start a Codename One thread that supports crash protection and similar Codename One features.
     * @param r runnable to run, <b>NOTICE</b> the thread MUST be explicitly started!
     * @param name the name for the thread
     * @return a thread instance which must be explicitly started!
     * @deprecated confusing name, use {@link #createThread(java.lang.Runnable, java.lang.String)} instead
     */
    public static Thread startThread(Runnable r, String name) {
        return Display.INSTANCE.startThread(r, name);
    }

    
    /**
     * Checks if the device supports disabling the screen display from dimming, allowing 
     * the developer to keep the screen display on.
     */ 
    public static boolean isScreenSaverDisableSupported() {
        return Display.impl.isScreenLockSupported();
    }
    
    /** 
     * If isScreenSaverDisableSupported() returns true calling this method will 
     * lock the screen display on
     * @param e when set to true the screen saver will work as usual and when set to false the screen
     * will not turn off automatically
     */
    public static void setScreenSaverEnabled(boolean e){
        Display.INSTANCE.setScreenSaverEnabled(e);
    }

    /**
     * Returns true if the device has camera false otherwise.
     */ 
    public static boolean hasCamera() {
        return Display.impl.hasCamera();
    }

    /**
     * Indicates whether the native picker dialog is supported for the given type 
     * which can include one of PICKER_TYPE_DATE_AND_TIME, PICKER_TYPE_TIME, PICKER_TYPE_DATE
     * @param pickerType the picker type constant
     * @return true if the native platform supports this picker type
     */
    public static boolean isNativePickerTypeSupported(int pickerType) {
        return Display.impl.isNativePickerTypeSupported(pickerType);
    }
    
    /**
     * Shows a native modal dialog allowing us to perform the picking for the given type 
     * which can include one of PICKER_TYPE_DATE_AND_TIME, PICKER_TYPE_TIME, PICKER_TYPE_DATE
     * @param type the picker type constant
     * @param source the source component (optional) the native dialog will be placed in relation to this
     * component if applicable
     * @param currentValue the currently selected value
     * @param data additional meta data specific to the picker type when applicable
     * @return the value from the picker or null if the operation was canceled.
     */
    public static Object showNativePicker(int type, Component source, Object currentValue, Object data) {
        return Display.impl.showNativePicker(type, source, currentValue, data);
    }

    /**
     * Prints to the log
     * @param s the string
     */
    public static void log(String s) {
        Log.p(s);
    }


    /**
     * Prints to the log
     * @param s the exception
     */
    public static void log(Throwable s) {
        Log.e(s);
    }
    
    /**
     * Sends the log to your email account
     */
    public static void sendLog() {
        Log.sendLog();
    }

    /**
     * <p>Send an email using the platform mail client.<br>
     * The code below demonstrates sending a simple message with attachments using the devices
     * native email client:
     * </p>
     * <script src="https://gist.github.com/codenameone/3db47a2ff8b35cae6410.js"></script>
     * @param subject e-mail subject
     * @param msg the Message to send
     * @param recipients array of e-mail addresses
     */
    public static void sendMessage(String subject, Message msg, String... recipients) {
        Display.impl.sendMessage(recipients, subject, msg);
    }

    /**
     * Allows detecting development mode so debugging code and special cases can be used to simplify flow
     * @return true if we are running in the simulator, false otherwise
     */
    public static boolean isSimulator() {
        return Display.impl.isSimulator();
    }

    /**
     * Adds a header to the global default headers, this header will be implicitly added 
     * to all requests going out from this point onwards. The main use case for this is
     * for authentication information communication via the header.
     * 
     * @param key the key of the header
     * @param value the value of the header
     */
    public static void addDefaultHeader(String key, String value) {
        NetworkManager.getInstance().addDefaultHeader(key, value);
    }

    /**
     * Identical to add to queue but waits until the request is processed in the queue,
     * this is useful for completely synchronous operations. 
     * 
     * @param request the request object to add
     */
    public static void addToQueueAndWait(final ConnectionRequest request) {
        NetworkManager.getInstance().addToQueueAndWait(request);
    }

    /**
     * Adds the given network connection to the queue of execution
     *
     * @param request network request for execution
     */
    public static void addToQueue(ConnectionRequest request) {
        NetworkManager.getInstance().addToQueue(request);
    }

    /**
     * Kills the given request and waits until the request is killed if it is
     * being processed by one of the threads. This method must not be invoked from
     * a network thread!
     * @param request
     */
    public static void killAndWait(final ConnectionRequest request) {
        NetworkManager.getInstance().killAndWait(request);
    }

    /**
     * Adds a generic listener to a network error that is invoked before the exception is propagated.
     * Note that this handles also server error codes by default! You can change this default behavior setting to false
     * ConnectionRequest.setHandleErrorCodesInGlobalErrorHandler(boolean).
     * Consume the event in order to prevent it from propagating further.
     *
     * @param e callback will be invoked with the Exception as the source object
     */
    public static void addNetworkErrorListener(ActionListener<NetworkEvent> e) {
        NetworkManager.getInstance().addErrorListener(e);
    }

    /**
     * Removes the given error listener
     *
     * @param e callback to remove
     */
    public static void removeNetworkErrorListener(ActionListener<NetworkEvent> e) {
        NetworkManager.getInstance().removeErrorListener(e);
    }

    /**
     * Adds a listener to be notified when progress updates
     *
     * @param al action listener
     */
    public static void addNetworkProgressListener(ActionListener<NetworkEvent> al) {
        NetworkManager.getInstance().addProgressListener(al);
    }

    /**
     * Adds a listener to be notified when progress updates
     *
     * @param al action listener
     */
    public static void removeNetworkProgressListener(ActionListener<NetworkEvent> al) {
        NetworkManager.getInstance().removeProgressListener(al);
    }

    /**
     * Sets the number of network threads and restarts the network threads
     * @param threadCount the new number of threads
     */
    public static void updateNetworkThreadCount(int threadCount) {
        NetworkManager.getInstance().updateThreadCount(threadCount);
    }

    /**
     * Storage is cached for faster access, however this might cause a problem with refreshing
     * objects since they are not cloned. Clearing the cache allows to actually reload from the
     * storage file.
     */
    public static void clearStorageCache() {
        Storage.getInstance().clearCache();
    }
    
    /**
     * Flush the storage cache allowing implementations that cache storage objects
     * to store
     */
    public static void flushStorageCache() {
        Storage.getInstance().flushStorageCache();
    }

    /**
     * Deletes the given file name from the storage
     *
     * @param name the name of the storage file
     */
    public static void deleteStorageFile(String name) {
        Storage.getInstance().deleteStorageFile(name);
    }

    /**
     * Deletes all the files in the application storage
     */
    public static void clearStorage() {
        Storage.getInstance().clearStorage();
    }

    /**
     * Creates an output stream to the storage with the given name
     *
     * @param name the storage file name
     * @return an output stream of limited capacity
     */
    public static OutputStream createStorageOutputStream(String name) throws IOException {
        return Storage.getInstance().createOutputStream(name);
    }

    /**
     * Creates an input stream to the given storage source file
     *
     * @param name the name of the source file
     * @return the input stream
     */
    public static InputStream createStorageInputStream(String name) throws IOException {
        return Storage.getInstance().createInputStream(name);
    }

    /**
     * Returns true if the given storage file exists
     *
     * @param name the storage file name
     * @return true if it exists
     */
    public static boolean existsInStorage(String name) {
        return Storage.getInstance().exists(name);
    }

    /**
     * Lists the names of the storage files
     *
     * @return the names of all the storage files
     */
    public static String[] listStorageEntries() {
        return Storage.getInstance().listEntries();
    }

    /**
     * Returns the size in bytes of the given entry
     * @param name the name of the entry
     * @return the size in bytes
     */
    public static int storageEntrySize(String name) {
        return Storage.getInstance().entrySize(name);
    }
    
    /**
     * <p>Writes the given object to storage assuming it is an externalizable type
     * or one of the supported types.</p>
     * 
     * <p>
     * The sample below demonstrates the usage and registration of the {@link com.codename1.io.Externalizable} interface:
     * </p>
     * <script src="https://gist.github.com/codenameone/858d8634e3cf1a82a1eb.js"></script>
     *
     * @param name store name
     * @param o object to store
     * @return true for success, false for failure
     */
    public static boolean writeObjectToStorage(String name, Object o) {
        return Storage.getInstance().writeObject(name, o);
    }

    /**
     * <p>Reads the object from the storage, returns null if the object isn't there</p>
     * <p>
     * The sample below demonstrates the usage and registration of the {@link com.codename1.io.Externalizable} interface:
     * </p>
     * <script src="https://gist.github.com/codenameone/858d8634e3cf1a82a1eb.js"></script>
     *
     *
     * @param name name of the store
     * @return object stored under that name
     */
    public static Object readObjectFromStorage(String name) {
        return Storage.getInstance().readObject(name);
    }


    /**
     * Returns the filesystem roots from which the structure of the file system
     * can be traversed
     *
     * @return the roots of the filesystem
     */
    public static String[] getFileSystemRoots() {
        return FileSystemStorage.getInstance().getRoots();
    }

    /**
     * Returns the type of the root often by guessing
     *
     * @param root the root whose type we are checking
     * @return one of the type constants above
     */
    public static int getFileSystemRootType(String root) {
        return FileSystemStorage.getInstance().getRootType(root);
    }


    /**
     * Lists the files within the given directory, returns relative file names and not
     * full file names.
     *
     * @param directory the directory in which files should be listed
     * @return array of file names
     */
    public static String[] listFiles(String directory) throws IOException {
        return FileSystemStorage.getInstance().listFiles(directory);
    }

    /**
     * Returns the size of the given root directory
     *
     * @param root the root directory in the filesystem
     * @return the byte size of the directory
     */
    public static long getFileSystemRootSizeBytes(String root) {
        return FileSystemStorage.getInstance().getRootSizeBytes(root);
    }

    /**
     * Returns the available space in the given root directory
     *
     * @param root the root directory in the filesystem
     * @return the bytes available in the directory
     */
    public static long getFileSystemRootAvailableSpace(String root) {
        return FileSystemStorage.getInstance().getRootAvailableSpace(root);
    }

    /**
     * Creates the given directory
     *
     * @param directory the directory name to create
     */
    public static void mkdir(String directory) {
        FileSystemStorage.getInstance().mkdir(directory);
    }
    

    /**
     * Deletes the specific file or empty directory.
     *
     * @param file file or empty directory to delete
     */
    public static void delete(String file) {
        FileSystemStorage.getInstance().delete(file);
    }


    /**
     * Indicates whether a file exists
     *
     * @param file the file to check
     * @return true if the file exists and false otherwise
     */
    public static boolean existsInFileSystem(String file) {
        return FileSystemStorage.getInstance().exists(file);
    }

    /**
     * Indicates the hidden state of the file
     *
     * @param file file
     * @return true for a hidden file
     */
    public static boolean isHiddenFile(String file) {
        return FileSystemStorage.getInstance().isHidden(file);
    }

    /**
     * Toggles the hidden state of the file
     *
     * @param file file
     * @param h hidden state
     */
    public static void setHiddenFile(String file, boolean h) {
        FileSystemStorage.getInstance().setHidden(file, h);
    }

    /**
     * Renames a file to the given name, expects the new name to be relative to the
     * current directory
     *
     * @param file absolute file name
     * @param newName relative new name
     */
    public static void renameFile(String file, String newName) {
        FileSystemStorage.getInstance().rename(file, newName);
    }

    /**
     * Returns the length of the file
     *
     * @param file file
     * @return length of said file
     */
    public static long getFileLength(String file) {
        return FileSystemStorage.getInstance().getLength(file);
    }

    /**
     * Returns the time that the file denoted by this abstract pathname was 
     * last modified.
     * @return A long value representing the time the file was last modified, 
     * measured in milliseconds
     */ 
    public static long getFileLastModifiedFile(String file) {
        return FileSystemStorage.getInstance().getLastModified(file);
    }
    
    /**
     * Indicates whether the given file is a directory
     *
     * @param file file
     * @return true if its a directory
     */
    public static boolean isDirectory(String file) {
        return FileSystemStorage.getInstance().isDirectory(file);
    }

    /**
     * Opens an output stream to the given file
     * 
     * @param file the file
     * @return the output stream
     */
    public static OutputStream openFileOutputStream(String file) throws IOException {
        return FileSystemStorage.getInstance().openOutputStream(file);
    }

    /**
     * Opens an input stream to the given file
     *
     * @param file the file
     * @return the input stream
     */
    public static InputStream openFileInputStream(String file) throws IOException {
        return FileSystemStorage.getInstance().openInputStream(file);
    }

    /**
     * Opens an output stream to the given file
     *
     * @param file the file
     * @param offset position in the file
     * @return the output stream
     */
    public static OutputStream openFileOutputStream(String file, int offset) throws IOException {
        return FileSystemStorage.getInstance().openOutputStream(file, offset);
    }

    /**
     * <p>The application home directory is a "safe place" to store files for this application in a portable way.
     * On some platforms such as Android  &amp; iOS this path may be visible only to the 
     * application itself, other apps won't have permission to access this path.<br>
     * The sample below uses the app home directory to save a file so we can share it using the {@link com.codename1.components.ShareButton}:</p>
     * 
     *  <script src="https://gist.github.com/codenameone/6bf5e68b329ae59a25e3.js"></script>
     * 
     * @return a writable directory that represent the application home directory
     */
     public static String getAppHomePath(){
        return FileSystemStorage.getInstance().getAppHomePath();
    }
     
     /**
      * Returns true if the device has a directory dedicated for "cache" files
      * @return true if a caches style directory exists in this device type
      */
     public static boolean hasCachesDir() {
        return FileSystemStorage.getInstance().hasCachesDir();
     }

     /**
      * Returns a device specific directory designed for cache style files, or null if {@link #hasCachesDir()}
      * is false
      * @return file URL or null
      */
     public static String getCachesDir() {
        return FileSystemStorage.getInstance().getCachesDir();
     }
     
     /**
     * Checks to see if you can prompt the user to install the app on their homescreen.
     * This is only relevant for the Javascript port with PWAs.  This is not a "static" property, as it 
     * only returns true if the app is in a state that allows you to prompt the user.  E.g. if you have
     * previously prompted the user and they have declined, then this will return false.  
     * 
     * <p>Best practice is to use {@link #onCanInstallOnHomescreen(java.lang.Runnable) } to be notified 
     * when you are allowed to prompt the user for installation.  Then call {@link #promptInstallOnHomescreen() }
     * inside that method - or sometime after.</p>
     * 
     * <h3>Example</h3>
     * <pre>{@code 
     * onCanInstallOnHomescreen(()->{
     *      if (canInstallOnHomescreen()) {
     *           if (promptInstallOnHomescreen()) {
     *               // User accepted installation
     *           } else {
     *               // user rejected installation
     *           }
     *      }
     * });
     * }</pre>
     * 
     * https://developers.google.com/web/fundamentals/app-install-banners/
     * @return True if you are able to prompt the user to install the app on their homescreen.  
     * @see #promptInstallOnHomescreen() 
     * @see #onCanInstallOnHomescreen(java.lang.Runnable) 
     * @since 6.0
     */
    public static boolean canInstallOnHomescreen() {
        return Display.impl.canInstallOnHomescreen();
    }
    
    /**
     * Prompts the user to install this app on their homescreen.  This is only relevant in the 
     * javascript port. 
     * @return The result of the user prompt.  {@literal true} if the user accepts the installation,
     * {@literal false} if they reject it.
     * @see #canInstallOnHomescreen() 
     * @see #onCanInstallOnHomescreen(java.lang.Runnable) 
     * @since 6.0
     */
    public static boolean promptInstallOnHomescreen() {
        return Display.impl.promptInstallOnHomescreen();
    }
    
    /**
     * A callback fired when you are allowed to prompt the user to install the app on their homescreen.
     * Only relevant in the javascript port.
     * @param r Runnable that will be run when/if you are permitted to prompt the user to install
     * the app on their homescreen.
     * @since 6.0
     */
    public static void onCanInstallOnHomescreen(Runnable r) {
        Display.impl.onCanInstallOnHomescreen(r);
    }
    
    /**
     * Captures a screenshot of the screen.
     * @return An image of the screen, or null if it fails.
     * @since 7.0
     */
    public static Image captureScreen() {
        return Display.impl.captureScreen();
    }
    
    /**
     * Adds a listener to receive messages from the native platform.  This is a mechanism to communicate
     * between the app and the native platform.  Currently the Javascript port is the only port to use
     * this mechanism.
     * 
     * <p>In the Javascript port, javascript can send messages to the CN1 app by calling
     * 
     * {@code
     * window.dispatchEvent(new CustomEvent('cn1inbox', {detail:'The message', code: SOMEINTEGER}));
     * }
     * 
     * @param l The listener.
     * @since 7.0
     */
    public static void addMessageListener(ActionListener<MessageEvent> l) {
        Display.INSTANCE.addMessageListener(l);
    }
    
    /**
     * Removes a listener from receiving messages from the native platform.  This is a mechanism to communicate
     * between the app and the native platform.  Currently the Javascript port is the only port to use
     * this mechanism.
     * @param l The listener.
     * @since 7.0
     */
    public static void removeMessageListener(ActionListener<MessageEvent> l) {
        Display.INSTANCE.removeMessageListener(l);
    }
    
    /**
     * Posts a message to the native platform.  This is a mechanism to communicate
     * between the app and the native platform.  Currently the Javascript port is the only port to use
     * this mechanism.
     * 
     * <p>In the Javascript port these messages can be received in Javascript by adding an event listener
     * for 'cn1outbox' events to the 'window' object.  The message is contained in the event data "detail" key. And the 
     * code in the 'code' key.</p>
     * @param message The message to send to the native platform.
     * @since 7.0
     */
    public static void postMessage(MessageEvent message) {
        Display.INSTANCE.postMessage(message);
    }
    
    /**
     * Convenience method to schedule a task to run on the EDT after {@literal timeout}ms.
     * @param timeout The timeout in milliseconds.
     * @param r The task to run.
     * @return The Timer object that can be used to cancel the task.
     * @since 7.0
     * @see #setInterval(int, java.lang.Runnable) 
     */
    public static Timer setTimeout(int timeout, Runnable r) {
        return Display.INSTANCE.setTimeout(timeout, r);
    }
    
    /**
     * Convenience method to schedule a task to run on the EDT after {@literal period}ms
     * repeating every {@literal period}ms.
     * @param timeout The delay and repeat in milliseconds.
     * @param r The runnable to run on the EDT.
     * @return The timer object which can be used to cancel the task.
     * @since 7.0
     * @see #setTimeout(int, java.lang.Runnable) 
     */
    public static Timer setInterval(int timeout, Runnable r) {
        return Display.INSTANCE.setInterval(timeout, r);
    }
    
    /**
     * Gets a reference to an application-wide shared Javascript context that can be used for running
     * Javascript commands.  When running in the Javascript port, this Javascript context will be the
     * same context in which the application itself is running, so it gives you the ability to interact 
     * with the browser and DOM directly using the familiar {@link BrowserComponent} API.
     * 
     * <p>When running on other platforms, this shared context will be an off-screen browser component.</p>
     * 
     * <p>Sample code allowing user to execute arbitrary Javascript code inside the shared context:</p>
     * <script src="https://gist.github.com/shannah/60040d9b3cc520b28bc1fef5e31afd31.js"></script>
     * 
     * @return A shared BrowserComponent
     * @since 7.0
     */
    public static BrowserComponent getSharedJavascriptContext() {
        return Display.impl.getSharedJavscriptContext();
    }

}
