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
package com.codename1.impl;

import com.codename1.codescan.CodeScanner;
import com.codename1.components.FileTree;
import com.codename1.components.FileTreeModel;
import com.codename1.contacts.Contact;
import com.codename1.db.Cursor;
import com.codename1.db.Database;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.Cookie;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.io.NetworkManager;
import com.codename1.io.Preferences;
import com.codename1.io.Storage;
import com.codename1.io.Util;
import com.codename1.io.tar.TarEntry;
import com.codename1.io.tar.TarInputStream;
import com.codename1.l10n.L10NManager;
import com.codename1.location.LocationManager;
import com.codename1.media.Media;
import com.codename1.messaging.Message;
import com.codename1.notifications.LocalNotification;
import com.codename1.payment.Purchase;
import com.codename1.payment.PurchaseCallback;
import com.codename1.push.PushCallback;
import com.codename1.ui.*;
import com.codename1.ui.animations.Animation;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.geom.Shape;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.util.ImageIO;
import com.codename1.util.FailureCallback;
import com.codename1.util.StringUtil;
import com.codename1.util.SuccessCallback;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

/**
 * Represents a vendor extension mechanizm for Codename One, <b>WARNING: this class is for internal
 * use only and is subject to change in future API revisions</b>. To replace the way in which
 * Codename One performs its task this class can be extended and its functionality replaced or
 * enhanced.
 * <p>It is the responsibility of the implementation class to grab and fire all events to the 
 * Display specifically for key, pointer events and screen resolution.
 * 
 * @author Shai Almog
 */
public abstract class CodenameOneImplementation {
    /**
     * Indicates the range of "hard" RTL bidi characters in unicode
     */
    private static final int RTL_RANGE_BEGIN = 0x590;
    private static final int RTL_RANGE_END = 0x7BF;

    private Object lightweightClipboard;

    private Hashtable linearGradientCache;
    private Hashtable radialGradientCache;

    private boolean builtinSoundEnabled = true;
    private boolean dragStarted = false;
    private int dragActivationCounter = 0;
    private int dragActivationX = 0;
    private int dragActivationY = 0;
    private int dragStartPercentage = 3;
    private Form currentForm;
    private static Object displayLock;
    private Animation[] paintQueue = new Animation[100];
    private Animation[] paintQueueTemp = new Animation[100];
    private int paintQueueFill = 0;
    private Graphics codenameOneGraphics;

    private static boolean bidi;
    private String packageName;
    private static int pollingMillis = 3 * 60 * 60000;
    private Component editingText;
    private String appArg;
    
    /**
     * Useful since the content of a single element touch event is often recycled
     * and always arrives on 1 thread. Even on multi-tocuh devices a single coordinate
     * touch event should be very efficient
     */
    private int[] xPointerEvent = new int[1];
    /**
     * Useful since the content of a single element touch event is often recycled
     * and always arrives on 1 thread. Even on multi-tocuh devices a single coordinate
     * touch event should be very efficient
     */
    private int[] yPointerEvent = new int[1];

    private int pointerPressedX;
    private int pointerPressedY;

    private Hashtable builtinSounds = new Hashtable();

    private Object storageData;
    private Hashtable cookies;
    private ActionListener logger;
    private static boolean pollingThreadRunning;
    private static PushCallback callback;
    private static PurchaseCallback purchaseCallback;
    private int commandBehavior = Display.COMMAND_BEHAVIOR_DEFAULT;
    private static Runnable onCurrentFormChange;
    private static Runnable onExit;    
    private boolean useNativeCookieStore = true;
    private boolean initiailized = false;
    
        
    static void setOnCurrentFormChange(Runnable on) {
        onCurrentFormChange = on;
    }
    
    /**
     * Set a task to be executed once the implementation is being destroyed
     */ 
    public static void setOnExit(Runnable on) {
        onExit = on;
    }
    
    /**
     * Invoked by the display init method allowing the implementation to "bind"
     * 
     * @param m the object passed to the Display init method
     */
    public final void initImpl(Object m) {
        init(m);
        if(m != null) {
            String clsName = m.getClass().getName();
            packageName = clsName.substring(0, clsName.lastIndexOf('.'));
        }
        initiailized = true;
    }
    
    /**
     * Returns true if the implementation is initialized.
     */ 
    public boolean isInitialized(){
        return initiailized;
    }
    
    /**
     * Allows implementations to send an error to the push callback
     * @param message the error message
     * @param errorCode the error code
     */
    protected void sendPushRegistrationError(String message, int errorCode) {
        if(callback != null) {
            callback.pushRegistrationError(message, errorCode);
        }
    }
    
    /**
     * Allows the system to register to receive push callbacks
     * @param push the callback object
     */
    public static void setPushCallback(PushCallback push) {
        callback = push;
    }

    /**
     * Allows the system to register the purchase callback instance
     * 
     * @param pc the pc callback
     */
    public static void setPurchaseCallback(PurchaseCallback pc) {
        purchaseCallback = pc;
    }
    
    /**
     * Returns the purchase callback instance
     */
    public static PurchaseCallback getPurchaseCallback() {
        return purchaseCallback;
    }
    
    /**
     * Invoked by the display init method allowing the implementation to "bind"
     * 
     * @param m the object passed to the Display init method
     */
    public abstract void init(Object m);

    /**
     * Some implementations might need to perform initializations of the EDT thread
     */
    public void initEDT() {
    }

    /**
     * Allows subclasses to cleanup if necessary
     */
    public void deinitialize() {
        initiailized = false;
    }

    /**
     * Invoked when a dialog is shown, this method allows a dialog to play a sound
     * 
     * @param type the type of the dialog matching the dialog classes defined types
     */
    public void playDialogSound(final int type) {
    }

    /**
     * Vibrates the device for the given length of time
     * 
     * @param duration length of time to vibrate
     */
    public void vibrate(int duration) {
    }

    /**
     * Flash the backlight of the device for the given length of time
     * 
     * @param duration length of time to flash the backlight
     */
    public void flashBacklight(int duration) {
    }

    /**
     * Returns the width dimension of the display controlled by this implementation
     * 
     * @return the width
     */
    public abstract int getDisplayWidth();

    /**
     * Returns the height dimension of the display controlled by this implementation
     * 
     * @return the height
     */
    public abstract int getDisplayHeight();

    /**
     * Returns the display height ignoring manipulations performed by the VKB
     * 
     * @return the height
     */
    public int getActualDisplayHeight() {
        return getDisplayHeight();
    }

    /**
     * Invoked when an exception occurs on the EDT, allows the implementation to
     * take control of the device to produce testing information.
     * 
     * @param err the exception that was caught in the EDT loop
     * @return false by default, true if the exception shouldn't be handled further 
     * by the EDT
     */
    public boolean handleEDTException(Throwable err) {
        return false;
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
    public final void editStringImpl(Component cmp, int maxSize, int constraint, String text, int initiatingKeycode) {
        editingText = cmp;
        editString(cmp, maxSize, constraint, text, initiatingKeycode);
        if(!isAsyncEditMode()) {
            editingText = null;
        }
    }
    
    /**
     * Sets current editingText value and sets it focused.
     * NB! it not call editString, that is it should be called only internally and
     * actually the methdo should not be added :)
     */
    public void setFocusedEditingText(Component cmp) {
        editingText = cmp;
        if (cmp != null) {
            Form form = cmp.getComponentForm();
            if (form != null) {
                form.setFocused(cmp);
            }
        }
    }

    /**
     * Invoked for special cases to stop text editing and clear native editing state
     */
    public void stopTextEditing() {    
    }
    
    /**
     * Using invokeAndBlock inside EditString creates peculiar behaviour that needs
     * to be worked around.  Ideally no port should use invokeAndBlock for this
     * but currently JavaSE and UWP both do.  Need to be able to detect this
     * for workarounds.
     * @return 
     */
    public boolean usesInvokeAndBlockForEditString() {
        return false;
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
    public abstract void editString(Component cmp, int maxSize, int constraint, String text, int initiatingKeycode);

    /**
     * Returns true if we are currently editing a component
     * @return whether a component is being edited
     */
    public boolean isEditingText() {
        return editingText != null;
    }
    
    /**
     * Checks whether the native text editor is currently visible over top of the 
     * given component (usually a {@code TextArea}
     * @param c The textarea/component we are checking
     * @return True if the native editor is visible.
     */
    public boolean isNativeEditorVisible(Component c) {
        return this.isNativeInputSupported() && this.isEditingText(c);
    }
    
    /**
     * Called when TextArea text is changed.  Can be used by the native 
     * implementation to trigger an update to the native editor if in async edit
     * mode.
     * @param c The TextArea that is being edited.
     * @param text 
     */
    public void updateNativeEditorText(Component c, String text) {
        
    }
    
    /**
     * In case of scrolling we can hide the text editor unless the user starts typing again,
     * this is only relevant for the async mode...
     */
    public void hideTextEditor() {
        Component c = editingText;
        editingText = null;
        
        // this might happen when the component is no longer a part of the form e.g. in the case of table editing.
        if(c != null) {
            c.repaint();
        }
    }
      
    public String getAppArg() {
        return appArg;
    }
    
    public void setAppArg(String arg) {
        appArg = arg;
    }
    
    /**
     * Allows the implementation to refresh the text field
     */
    protected final void repaintTextEditor(final boolean focus) {
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                if(editingText != null) {
                    editingText.repaint();
                    if(focus) {
                        editingText.requestFocus();
                    }
                }
            }
        });
    }
    
    /**
     * Returns true if we are currently editing this component
     * @return whether a component is being edited
     */
    public boolean isEditingText(Component c) {
        return editingText == c;
    }
    
    /**
     * Gets the component that is currently editing text
     * @return 
     */
    public Component getEditingText() {
        return editingText;
    }

    /**
     * Returns true if edit string will return immediately and broadcast editing events directly to the text field
     * @return false by default
     */
    public boolean isAsyncEditMode() {
        return false;
    }
    
    /**
     * Returns the height of the VKB when it is open for an implementation that requires
     * us to allow scrolling further
     * @return height in pixels
     */
    public int getInvisibleAreaUnderVKB() {
        return 0;
    }
    
    /**
     * Invoked if Codename One needs to dispose the native text editing but would like the editor
     * to store its state.
     */
    public void saveTextEditingState() {
    }

    /**
     * Returns true if the implementation still has elements to paint.
     * 
     * @return false by default
     */
    public boolean hasPendingPaints() {
        return paintQueueFill != 0;
    }


    /**
     * Return the number of alpha levels supported by the implementation.
     * 
     * @return the number of alpha levels supported by the implementation
     * @deprecated this method isn't implemented in most modern devices
     */
    public int numAlphaLevels() {
        return 255;
    }

    /**
     * Returns the number of colors applicable on the device, note that the API
     * does not support gray scale devices.
     * 
     * @return the number of colors applicable on the device
     * @deprecated this method isn't implemented in most modern devices
     */
    public int numColors() {
        return 65536;
    }

    /**
     * This method allows customizing/creating a graphics context per component which is useful for
     * some elaborate implementations of Codename One. This method is only relevant for elborate components
     * such as container which render their own components rather than invoke repaint()
     * 
     * @param cmp component being rendered
     * @param currentContext the current graphics context
     * @return a graphics object thats appropriate for the given component.
     */
    public Graphics getComponentScreenGraphics(Component cmp, Graphics  currentContext) {
        return currentContext;
    }

    /**
     * Allows for painting an overlay on top of the implementation for notices during
     * testing etc.
     * 
     * @param g graphics context on which to draw the overlay
     */
    protected void paintOverlay(Graphics g) {
    }

    /**
     * Calculates the paintable bounds of a component.  The paintable bounds is 
     * the bounds (in screen coordinates) that will be vislble on the screen.  This
     * accounts for possible clipping by parent components.
     * @param c The component whose paintable bounds we are interested in.
     * @param out A rectangle to return the bounds in.
     */
    private void getPaintableBounds(Component c, Rectangle out) {
        int x = c.getAbsoluteX() + c.getScrollX();
        int y = c.getAbsoluteY() + c.getScrollY();
        int x2 = x + c.getWidth();
        int y2 = y + c.getHeight();
        
        Container parent = null;
        if ((parent = c.getParent()) != null) {
            getPaintableBounds(parent, out);
            x = Math.max(out.getX(), x);
            y = Math.max(out.getY(), y);
            x2 = Math.min(out.getX() + out.getWidth(), x2);
            y2 = Math.min(out.getY() + out.getHeight(), y2);
            
            
        }
        out.setBounds(x, y, x2-x, y2-y);
        
    }
   
    /**
     * For use inside paintDirty() so that we don't have to instantiate
     * a rectangle each time it is called.
     */
    private Rectangle paintDirtyTmpRect = new Rectangle();
    
    /**
     * Invoked by the EDT to paint the dirty regions
     */
    public void paintDirty() {
        int size = 0;
        synchronized (displayLock) {
            size = paintQueueFill;
            Animation[] array = paintQueue;
            paintQueue = paintQueueTemp;
            paintQueueTemp = array;
            paintQueueFill = 0;
        }
        if (size > 0) {
            Graphics wrapper = getCodenameOneGraphics();
            int dwidth = getDisplayWidth();
            int dheight = getDisplayHeight();
            int topX = dwidth;
            int topY = dheight;
            int bottomX = 0;
            int bottomY = 0;
            for (int iter = 0; iter < size; iter++) {
                Animation ani = paintQueueTemp[iter];
                
                // might happen due to paint queue removal
                if(ani == null) {
                    continue;
                }
                paintQueueTemp[iter] = null;
                wrapper.translate(-wrapper.getTranslateX(), -wrapper.getTranslateY());
                wrapper.setClip(0, 0, dwidth, dheight);
                if (ani instanceof Component) {
                    Component cmp = (Component) ani;
                    Rectangle dirty = cmp.getDirtyRegion();
                    if (dirty != null) {
                        Dimension d = dirty.getSize();
                        wrapper.setClip(dirty.getX(), dirty.getY(), d.getWidth(), d.getHeight());
                        cmp.setDirtyRegion(null);
                    }

                    cmp.paintComponent(wrapper);
                    getPaintableBounds(cmp, paintDirtyTmpRect);
                    int cmpAbsX = paintDirtyTmpRect.getX();
                    topX = Math.min(cmpAbsX, topX);
                    bottomX = Math.max(cmpAbsX + paintDirtyTmpRect.getWidth(), bottomX);
                    int cmpAbsY = paintDirtyTmpRect.getY();
                    topY = Math.min(cmpAbsY, topY);
                    bottomY = Math.max(cmpAbsY + paintDirtyTmpRect.getHeight(), bottomY);
                } else {
                    bottomX = dwidth;
                    bottomY = dheight;
                    topX = 0;
                    topY = 0;
                    ani.paint(wrapper);
                }
            }

            paintOverlay(wrapper);
            //Log.p("Flushing graphics : "+topX+","+topY+","+bottomX+","+bottomY);
            flushGraphics(topX, topY, bottomX - topX, bottomY - topY);
        }
    }

    /**
     * This method is a callback from the edt before the edt enters to an idle 
     * state
     * @param enter true before the edt sleeps and false when exits from the
     * idle state
     */
    public void edtIdle(boolean enter){
    }
    
    /**
     * Flush the currently painted drawing onto the screen if using a double buffer
     * 
     * @param x position of the dirty region
     * @param y position of the dirty region
     * @param width width of the dirty region
     * @param height height of the dirty region
     */
    public abstract void flushGraphics(int x, int y, int width, int height);

    /**
     * Flush the currently painted drawing onto the screen if using a double buffer
     */
    public abstract void flushGraphics();
    
    /**
     * Returns a graphics object for use by the painting
     * 
     * @return a graphics object, either recycled or new, this object will be 
     * used on the EDT
     */
    protected Graphics getCodenameOneGraphics() {
        return codenameOneGraphics;
    }

    /**
     * Installs the Codename One graphics object into the implementation
     * 
     * @param g graphics object for use by the implementation
     */
    public void setCodenameOneGraphics(Graphics g) {
        codenameOneGraphics = g;
    }

    /**
     * A flag that can be overridden by a platform to indicate that native 
     * peers are rendered behind the main codename one graphics layer.  The main
     * effect of this is that Graphics will call clearRect() any time a native
     * component is "painted" to poke a hole through the CN1 layer.
     * @return 
     */
    public boolean paintNativePeersBehind() {
        return false;
    }
    
    
    /**
     * Installs the display lock allowing implementors to synchronize against the 
     * Display mutex, this method is invoked internally and should not be used.
     * 
     * @param lock the mutex from display
     */
    public void setDisplayLock(Object lock) {
        displayLock = lock;
    }

    /**
     * Returns a lock object which can be synchronized against, this lock is used
     * by the EDT.
     * 
     * @return a lock object
     */
    public Object getDisplayLock() {
        return displayLock;
    }

    /**
     * Removes an entry from the paint queue if it exists, this is important for cases
     * in which a component was repainted and immediately removed from its parent container
     * afterwards. This happens sometimes in cases where a replace() operation changes 
     * a component to a new component that has an animation() the animation might have triggered
     * a repaint before the removeComponent method was invoked
     * 
     * @param cmp the component to 
     */
    public void cancelRepaint(Animation cmp) {
        synchronized (displayLock) {
            for (int iter = 0; iter < paintQueueFill; iter++) {
                if (paintQueue[iter] == cmp) {
                    paintQueue[iter] = null;
                    return;
                }
            }

        }
    }

    /**
     * Invoked to add an element to the paintQueue
     * 
     * @param cmp component or animation to push into the paint queue
     */
    public void repaint(Animation cmp) {
        synchronized (displayLock) {
            for (int iter = 0; iter < paintQueueFill; iter++) {
                Animation ani = paintQueue[iter];
                if (ani == cmp) {
                    return;
                }
                //no need to paint a Component if one of its parent is already in the queue
                if(ani instanceof Container && cmp instanceof Component){
                    Component parent = ((Component)cmp).getParent();
                    while (parent != null) {
                        if (parent == ani) {
                            return;
                        }
                        parent = parent.getParent();
                    }
                }
            }
            // overcrowding the queue don't try to grow the array!
            if (paintQueueFill >= paintQueue.length) {
                System.out.println("Warning paint queue size exceeded, please watch the amount of repaint calls");
                return;
            }

            paintQueue[paintQueueFill] = cmp;
            paintQueueFill++;
            displayLock.notify();
        }
    }

    /**
     * Extracts RGB data from the given native image and places it in the given array
     * 
     * @param nativeImage native platform image object
     * @param arr int array to store RGB data
     * @param offset position within the array to start
     * @param x x position within the image
     * @param y y position within the image
     * @param width width to extract
     * @param height height to extract
     */
    public abstract void getRGB(Object nativeImage, int[] arr, int offset, int x, int y, int width, int height);

    /**
     * Create a platform native image object from the given RGB data
     * 
     * @param rgb ARGB data from which to create a platform image
     * @param width width for the resulting image
     * @param height height for the resulting image
     * @return platform image object
     */
    public abstract Object createImage(int[] rgb, int width, int height);

    /**
     * Creates a native image from a file in the system jar
     * 
     * @param path within the jar
     * @return native system image
     * @throws java.io.IOException if thrown by loading
     */
    public abstract Object createImage(String path) throws IOException;

    /**
     * Creates a native image from a given input stream
     * 
     * @param i input stream from which to load the image
     * @return native system image
     * @throws java.io.IOException if thrown by loading
     */
    public abstract Object createImage(InputStream i) throws IOException;

    /**
     * Creates a modifable native image that can return a graphics object
     * 
     * @param width the width of the mutable image
     * @param height the height of the mutable image
     * @param fillColor the ARGB fill color, alpha may be ignored based on the value of
     * isAlphaMutableImageSupported
     * @return the native image
     */
    public abstract Object createMutableImage(int width, int height, int fillColor);

    /**
     * Indicates whether mutable images respect alpha values when constructed
     * 
     * @return true if mutable images can have an alpha value when initially created
     */
    public boolean isAlphaMutableImageSupported() {
        return false;
    }

    /**
     * Create a nativate image from its compressed byte data
     * 
     * @param bytes the byte array representing the image data
     * @param offset offset within the byte array
     * @param len the length for the image within the byte array
     * @return a native image
     */
    public abstract Object createImage(byte[] bytes, int offset, int len);

    /**
     * Returns the width of a native image
     * 
     * @param i the native image
     * @return the width of the native image
     */
    public abstract int getImageWidth(Object i);

    /**
     * Returns the height of a native image
     * 
     * @param i the native image
     * @return the height of the native image
     */
    public abstract int getImageHeight(Object i);

    /**
     * Scales a native image and returns the scaled version
     * 
     * @param nativeImage image to scale
     * @param width width of the resulting image
     * @param height height of the resulting image
     * @return scaled image instance
     */
    public abstract Object scale(Object nativeImage, int width, int height);

    private static int round(double d) {
        double f = Math.floor(d);
        double c = Math.ceil(d);
        if (c - d < d - f) {
            return (int) c;
        }
        return (int) f;
    }

    /**
     * Returns an instance of this image rotated by the given number of degrees. By default 90 degree
     * angle divisions are supported, anything else is implementation dependent. This method assumes 
     * a square image. Notice that it is inefficient in the current implementation to rotate to
     * non-square angles, 
     * <p>E.g. rotating an image to 45, 90 and 135 degrees is inefficient. Use rotatate to 45, 90
     * and then rotate the 45 to another 90 degrees to achieve the same effect with less memory.
     * 
     * @param degrees A degree in right angle must be larger than 0 and up to 359 degrees
     * @return new image instance with the closest possible rotation
     */
    public Object rotate(Object image, int degrees) {
        int width = getImageWidth(image);
        int height = getImageHeight(image);
        int[] arr = new int[width * height];
        int[] dest = new int[arr.length];
        getRGB(image, arr, 0, 0, 0, width, height);
        int centerX = width / 2;
        int centerY = height / 2;

        double radians = Math.toRadians(-degrees);
        double cosDeg = Math.cos(radians);
        double sinDeg = Math.sin(radians);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int x2 = round(cosDeg * (x - centerX) - sinDeg * (y - centerY) + centerX);
                int y2 = round(sinDeg * (x - centerX) + cosDeg * (y - centerY) + centerY);
                if (!(x2 < 0 || y2 < 0 || x2 >= width || y2 >= height)) {
                    int destOffset = x2 + y2 * width;
                    if (destOffset >= 0 && destOffset < dest.length) {
                        dest[x + y * width] = arr[destOffset];
                    }
                }
            }
        }
        return createImage(dest, width, height);
    }

    /**
     * Rotates the given image by 90 degrees while changing the ratio of the picture 
     * @param image the image
     * @param maintainOpacity whether the opacity in the image should be maintained
     * @return a new image rotated by 90 degrees
     */
    public Image rotate90Degrees(Image image, boolean maintainOpacity) {
        int[] rgb = image.getRGB();
        int[] newRGB = new int[rgb.length];
        int width = image.getWidth();
        int height = image.getHeight();
        
        for(int y = 0 ; y < height ; y++) {
            for(int x = 0 ; x < width ; x++) {
                int destX = height - y - 1;
                newRGB[destX + x * height] = rgb[x + y * width];
            }
        }
        
        // we reverse width/height
        return EncodedImage.createFromRGB(newRGB, height, width, !maintainOpacity);
    }
    
    /**
     * Rotates the given image by 180 degrees 
     * @param image the image
     * @param maintainOpacity whether the opacity in the image should be maintained
     * @return a new image rotated by 180 degrees
     */
    public Image rotate180Degrees(Image image, boolean maintainOpacity) {
        int[] rgb = image.getRGB();
        int[] newRGB = new int[rgb.length];
        int width = image.getWidth();
        int height = image.getHeight();
        
        for(int y = 0 ; y < height ; y++) {
            for(int x = 0 ; x < width ; x++) {
                int destX = width - x - 1;
                newRGB[destX + (height - y - 1) * width] = rgb[x + y * width];
            }
        }
        
        return EncodedImage.createFromRGB(newRGB, width, height, !maintainOpacity);
    }
    
    /**
     * Rotates the given image by 270 degrees while changing the ratio of the picture 
     * @param image the image
     * @param maintainOpacity whether the opacity in the image should be maintained
     * @return a new image rotated by 270 degrees
     */
    public Image rotate270Degrees(Image image, boolean maintainOpacity) {
        int[] rgb = image.getRGB();
        int[] newRGB = new int[rgb.length];
        int width = image.getWidth();
        int height = image.getHeight();
        
        for(int y = 0 ; y < height ; y++) {
            for(int x = 0 ; x < width ; x++) {
                newRGB[y + x * height] = rgb[x + y * width];
            }
        }
        
        // we reverse width/height
        return EncodedImage.createFromRGB(newRGB, height, width, !maintainOpacity);
    }
    
    /**
     * Flips the given image on the horizontal axis
     * @param image the image
     * @param maintainOpacity whether the opacity in the image should be maintained
     * @return a new image flipped
     */
    public Image flipImageHorizontally(Image image, boolean maintainOpacity) {
        int[] rgb = image.getRGB();
        int[] newRGB = new int[rgb.length];
        int width = image.getWidth();
        int height = image.getHeight();
        
        for(int y = 0 ; y < height ; y++) {
            for(int x = 0 ; x < width ; x++) {
                newRGB[(width - x - 1) + y * width] = rgb[x + y * width];
            }
        }
        
        return EncodedImage.createFromRGB(newRGB, width, height, !maintainOpacity);
    }
    
    /**
     * Flips the given image on the vertical axis
     * @param image the image
     * @param maintainOpacity whether the opacity in the image should be maintained
     * @return a new image flipped
     */
    public Image flipImageVertically(Image image, boolean maintainOpacity) {
        int[] rgb = image.getRGB();
        int[] newRGB = new int[rgb.length];
        int width = image.getWidth();
        int height = image.getHeight();
        
        for(int y = 0 ; y < height ; y++) {
            for(int x = 0 ; x < width ; x++) {
                newRGB[x + (height - y - 1) * width] = rgb[x + y * width];
            }
        }
        
        return EncodedImage.createFromRGB(newRGB, width, height, !maintainOpacity);
    }
    
    /**
     * Returns true if the platform supports a native image cache.  The native image cache
     * is different than just {@link FileSystemStorage#hasCachesDir()}.  A native image cache
     * is an image cache that the platform provides that is full transparent to Codename One
     * with respect to how images are stored, and whether they are cached.  Currently only
     * the Javascript port supprts a native image cache.
     * 
     * <p>This is used by {@link URLImage#createCachedImage(java.lang.String, java.lang.String, com.codename1.ui.Image, int) }
     * to determine if it should use a cached image, or to defer to its storage and filesystem methods.</p>
     * @return True on platforms that support a native image cache.  Currently only Javascript.
     * @see Display#supportsNativeImageCache() 
     */
    public boolean supportsNativeImageCache() {
        return false;
    }
    
    /**
     * Downloads an image from a URL to the cache. Platforms
     * that support a native image cache {@link #supportsNativeImageCache() } (e.g. Javascript) override this method to defer to the 
     * platform's handling of cached images.  Platforms that have a caches directory ({@link FileSystemStorage#hasCachesDir() }
     * will use that directory to cache the image.  Other platforms will just download to storage.
     * 
     * @param url The URL of the image to download.
     * @param onSuccess Callback on success.
     * @param onFail Callback on fail.
     * 
     * @see URLImage#createToCache(com.codename1.ui.EncodedImage, java.lang.String, com.codename1.ui.URLImage.ImageAdapter) 
     */
    public void downloadImageToCache(String url, SuccessCallback<Image> onSuccess, final FailureCallback<Image> onFail) {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        if (fs.hasCachesDir()) {
            String name = "cn1_image_cache["+url+"]";
            name = StringUtil.replaceAll(name, "/", "_");
            name = StringUtil.replaceAll(name, "\\", "_");
            name = StringUtil.replaceAll(name, "%", "_");
            name = StringUtil.replaceAll(name, "?", "_");
            name = StringUtil.replaceAll(name, "*", "_");
            name = StringUtil.replaceAll(name, ":", "_");
            name = StringUtil.replaceAll(name, "=", "_");   
            
            String filePath = fs.getCachesDir() + fs.getFileSystemSeparator() + name;
            
            // We use Util.downloadImageToFileSystem rather than CodenameOneImplementation.downloadImageToFileSystem
            // because we want it to try to load from file system first.
            Util.downloadImageToFileSystem(url, filePath, onSuccess, onFail);
        } else {
            // We use Util.downloadImageToStorage rather than CodenameOneImplementation.downloadImageToStorage
            // because we want it to try to load from storage first.
            Util.downloadImageToStorage(url, "cn1_image_cache["+url+"]", onSuccess, onFail);
        }
    }
    
    /**
     * Downloads an image to storage. This will *not* first check to see if the image is located in storage
     * already.  It will download and overwrite any existing image at the provided location.
     * 
     * <p>Some platforms may override this method to use platform-level caching.  E.g. Javascript will use
     * the browser cache for downloading the image.</p>
     * 
     * @param url The URL of the image to download.
     * @param fileName The storage key to be used to store the image.
     * @param onSuccess Callback on success.  Will be executed on EDT.
     * @param onFail Callback on failure.  Will be executed on EDT.
     */
    public void downloadImageToStorage(String url, String fileName, SuccessCallback<Image> onSuccess, FailureCallback<Image> onFail) {
        ConnectionRequest cr = new ConnectionRequest();
        cr.setPost(false);
        cr.setFailSilently(true);
        cr.setReadResponseForErrors(false);
        cr.setDuplicateSupported(true);
        cr.setUrl(url);
        cr.downloadImageToStorage(fileName, onSuccess, onFail);
    }
    
    /**
     * Downloads an image to file system. This will *not* first check to see if the file exists already.  
     * It will download and overwrite any existing image at the provided location.
     * 
     * <p>Some platforms may override this method to use platform-level caching.  E.g. Javascript will use
     * the browser cache for downloading the image.</p>
     * 
     * @param url The URL of the image to download.
     * @param fileName The storage key to be used to store the image.
     * @param onSuccess Callback on success.  Will be executed on EDT.
     * @param onFail Callback on failure.  Will be executed on EDT.
     */
    public void downloadImageToFileSystem(String url, String fileName, SuccessCallback<Image> onSuccess, FailureCallback<Image> onFail) {
        ConnectionRequest cr = new ConnectionRequest();
        cr.setPost(false);
        cr.setFailSilently(true);
        cr.setReadResponseForErrors(false);
        cr.setDuplicateSupported(true);
        cr.setUrl(url);
        cr.downloadImageToFileSystem(fileName, onSuccess, onFail);
    }
    
    /**
     * Returns the number of softkeys on the device
     * 
     * @return the number of softkey buttons on the device
     */
    public abstract int getSoftkeyCount();

    /**
     * Returns the softkey keycode for the given softkey index
     * 
     * @param index the index of the softkey
     * @return the set of keycodes which can indicate the softkey, multiple keycodes
     * might apply to the same functionality
     */
    public abstract int[] getSoftkeyCode(int index);

    /**
     * Returns the keycode for the clear key
     * 
     * @return the system key code for this device
     */
    public abstract int getClearKeyCode();

    /**
     * Returns the keycode for the backspace key
     * 
     * @return the system key code for this device
     */
    public abstract int getBackspaceKeyCode();

    /**
     * Returns the keycode for the back key
     * 
     * @return the system key code for this device
     */
    public abstract int getBackKeyCode();

    /**
     * Returns the display game action for the given keyCode if applicable to match
     * the contrct of Codename One for the game action behavior
     * 
     * @param keyCode the device keycode
     * @return a game action or 0
     */
    public abstract int getGameAction(int keyCode);

    /**
     * Returns a keycode which can be sent to getGameAction
     * 
     * @param gameAction the game action
     * @return key code matching the given game action
     */
    public abstract int getKeyCode(int gameAction);

    /**
     * Returns true if the device will send touch events
     * 
     * @return true if the device will send touch events
     */
    public abstract boolean isTouchDevice();

    /**
     * Callback before showing a specific form
     * @param f the form shown
     */
    public void onShow(Form f) {
        if(onCurrentFormChange != null) {
            onCurrentFormChange.run();
        }
    }
    
    /**
     * This method is used internally to determine the actual current form
     * it doesn't perform the logic of transitions etc. and shouldn't be invoked
     * by developers
     * 
     * @param f the current form
     */
    public void setCurrentForm(Form f) {
        currentForm = f;
    }

    /**
     * Callback method allowing the implementation to confirm that it controls the
     * view just before a new form is installed.
     */
    public void confirmControlView() {
    }

    /**
     * Returns the current form, this method is for internal use only and does not
     * take transitions/menus into consideration
     * 
     * @return The internal current form
     */
    public Form getCurrentForm() {
        return currentForm;
    }

    /**
     * Codename One can translate all coordinates and never requires a call to translate
     * this works well for some devices which have hairy issues with translate.
     * However for some platforms where translate can be leveraged with affine transforms
     * this can be a problem. These platforms can choose to translate on their own
     * 
     * @return true if the implementation is interested in receiving translate calls
     * and handling them.
     */
    public boolean isTranslationSupported() {
        return false;
    }

    /**
     * Translates the X/Y location for drawing on the underlying surface. Translation
     * is incremental so the new value will be added to the current translation and
     * in order to reset translation we have to invoke 
     * {@code translate(-getTranslateX(), -getTranslateY()) }
     * 
     * @param graphics the graphics context
     * @param x the x coordinate
     * @param y the y coordinate
     */
    public void translate(Object graphics, int x, int y) {
    }

    /**
     * Returns the current x translate value 
     * 
     * @param graphics the graphics context
     * @return the current x translate value 
     */
    public int getTranslateX(Object graphics) {
        return 0;
    }

    /**
     * Returns the current y translate value 
     * 
     * @param graphics the graphics context
     * @return the current y translate value 
     */
    public int getTranslateY(Object graphics) {
        return 0;
    }

    /**
     * Returns the current color
     * 
     * @param graphics the graphics context
     * @return the RGB graphics color 
     */
    public abstract int getColor(Object graphics);

    /**
     * Sets the current rgb color while ignoring any potential alpha component within
     * said color value.
     * 
     * @param graphics the graphics context
     * @param RGB the RGB value for the color.
     */
    public abstract void setColor(Object graphics, int RGB);

    /**
     * Alpha value from 0-255 can be ignored for some operations
     * 
     * @param graphics the graphics context
     * @param alpha the alpha channel
     */
    public abstract void setAlpha(Object graphics, int alpha);

    /**
     * Alpha value from 0-255 can be ignored for some operations
     * 
     * @param graphics the graphics context
     * @return the alpha channel
     */
    public abstract int getAlpha(Object graphics);

    /**
     * Returns true if alpha can be applied for all elements globally and efficiently
     * otherwise alpha should be ignored.
     * Notice that fillRect MUST always support alpha regardless of the value of this
     * variable!
     * 
     * @return true if alpha support is natively implemented
     */
    public boolean isAlphaGlobal() {
        return false;
    }

    /**
     * Indicates whether the underlying implementation allows for anti-aliasing in regular
     * drawing operations
     * 
     * @return false by default
     */
    public boolean isAntiAliasingSupported() {
        return false;
    }

    /**
     * Indicates whether the underlying implementation allows for anti-aliased fonts
     * 
     * @return false by default
     */
    public boolean isAntiAliasedTextSupported() {
        return false;
    }

    /**
     * Toggles anti-aliasing mode for regular rendering operations
     * 
     * @param graphics the graphics context
     * @param a true to activate Anti-aliasing, false to disable it
     */
    public void setAntiAliased(Object graphics, boolean a) {
    }

    /**
     * Returns anti-aliasing mode for regular rendering operations
     * 
     * @param graphics the graphics context
     * @return true if Anti-aliasing is active, false otherwise
     */
    public boolean isAntiAliased(Object graphics) {
        return false;
    }

    /**
     * Toggles anti-aliasing mode for font rendering operations
     * 
     * @param graphics the graphics context
     * @param a true to activate Anti-aliasing, false to disable it
     */
    public void setAntiAliasedText(Object graphics, boolean a) {
    }

    /**
     * Returns anti-aliasing mode for font rendering operations
     * 
     * @param graphics the graphics context
     * @return true if Anti-aliasing is active, false otherwise
     */
    public boolean isAntiAliasedText(Object graphics) {
        return false;
    }

    /**
     * Installs a native font object
     * 
     * @param graphics the graphics context
     * @param font the native font object
     */
    public abstract void setNativeFont(Object graphics, Object font);

    /**
     * Returns the internal clipping rectangle. This method must create a new
     * rectangle object to prevent corruption by modification.
     * 
     * @param graphics the graphics context
     * @return the clipping rectangle.
     */
    public Rectangle getClipRect(Object graphics) {
        return new Rectangle(getClipX(graphics), getClipY(graphics), new Dimension(getClipWidth(graphics), getClipHeight(graphics)));
    }

    /**
     * Returns the clipping coordinate
     * 
     * @param graphics the graphics context
     * @return the clipping coordinate
     */
    public abstract int getClipX(Object graphics);

    /**
     * Returns the clipping coordinate
     * 
     * @param graphics the graphics context
     * @return the clipping coordinate
     */
    public abstract int getClipY(Object graphics);

    /**
     * Returns the clipping coordinate
     * 
     * @param graphics the graphics context
     * @return the clipping coordinate
     */
    public abstract int getClipWidth(Object graphics);

    /**
     * Returns the clipping coordinate
     * 
     * @param graphics the graphics context
     * @return the clipping coordinate
     */
    public abstract int getClipHeight(Object graphics);

    /**
     * Installs a new clipping rectangle
     * 
     * @param graphics the graphics context
     * @param rect rectangle representing the new clipping area
     */
    public void setClipRect(Object graphics, Rectangle rect) {
        Dimension d = rect.getSize();
        setClip(graphics, rect.getX(), rect.getY(), d.getWidth(), d.getHeight());
    }
    
    /*
    public void setClipShape(Object graphics, Shape shape){
        if ( shape.isRectangle() ){
            setClipRect(graphics, (Rectangle)shape);
        } else {
            throw new RuntimeException("Only rectangle clips supported in this port");
        }
    }
    
    public Shape getClipShape(Object graphics){
        return this.getClipRect(graphics);
    }
    */
   

    /**
     * Installs a new clipping rectangle
     * 
     * @param graphics the graphics context
     * @param x coordinate
     * @param y coordinate
     * @param width size
     * @param height size
     * @param rect rectangle representing the new clipping area
     */
    public abstract void setClip(Object graphics, int x, int y, int width, int height);

    /**
     * Clips the Graphics context to the Shape.
     * 
     * @param graphics the graphics context
     * @param shape The shape to clip.
     */
    public void setClip(Object graphics, Shape shape){
        System.out.println("Shape clip is not supported");
    }

    /**
     * Changes the current clipping rectangle to subset the current clipping with
     * the given clipping.
     * 
     * @param graphics the graphics context
     * @param rect rectangle representing the new clipping area
     */
    public void clipRect(Object graphics, Rectangle rect) {
        Dimension d = rect.getSize();
        clipRect(graphics, rect.getX(), rect.getY(), d.getWidth(), d.getHeight());
    }

    /**
     * Changes the current clipping rectangle to subset the current clipping with
     * the given clipping.
     * 
     * @param graphics the graphics context
     * @param x coordinate
     * @param y coordinate
     * @param width size
     * @param height size
     * @param rect rectangle representing the new clipping area
     */
    public abstract void clipRect(Object graphics, int x, int y, int width, int height);
    
    // ----- BEGIN CLIP STACK METHODS ---  ADDED TO HELP SUPPORT TRANSFORMATIONS
    // in the clip.
    
    
    /**
     * Pushes the current clip onto the clip stack so that it can be retrieved later
     * by {@link #popClip}.
     * @param graphics The native graphics context.
     */
    public void pushClip(Object graphics){
        
    }
    
    /**
     * Cleans up resources used by graphics object
     * @param graphics 
     */
    public void disposeGraphics(Object graphics) {
        
    }
    
    
    /**
     * Pops the clip from the top of the clip stack and sets it as the current clip.
     * @param graphics The native graphics context.
     * @return The clip that was popped off the top of the clip stack.
     */
    public void popClip(Object graphics){
        // NOt implemented yet... need to implement.
        
        
    }
    
    
    // ----- END CLIP STACK METHODS

    /**
     * Draws a line between the 2 X/Y coordinates
     * 
     * @param graphics the graphics context
     * @param x1 first x position
     * @param y1 first y position
     * @param x2 second x position
     * @param y2 second y position
     */
    public abstract void drawLine(Object graphics, int x1, int y1, int x2, int y2);

    /**
     * Fills the rectangle from the given position according to the width/height
     * minus 1 pixel according to the convention in Java.
     * 
     * @param graphics the graphics context
     * @param x the x coordinate of the rectangle to be filled.
     * @param y the y coordinate of the rectangle to be filled.
     * @param width the width of the rectangle to be filled.
     * @param height the height of the rectangle to be filled.
     */
    public abstract void fillRect(Object graphics, int x, int y, int width, int height);
    
    public void clearRect(Object graphics, int x, int y, int width, int height) {
        System.out.println("clearRect() not implemented on this platform");
    }

    /**
     * Draws a rectangle in the given coordinates
     * 
     * @param graphics the graphics context
     * @param x the x coordinate of the rectangle to be drawn.
     * @param y the y coordinate of the rectangle to be drawn.
     * @param width the width of the rectangle to be drawn.
     * @param height the height of the rectangle to be drawn.
     */
    public abstract void drawRect(Object graphics, int x, int y, int width, int height);

    /**
     * Draws a rectangle in the given coordinates
     * 
     * @param graphics the graphics context
     * @param x the x coordinate of the rectangle to be drawn.
     * @param y the y coordinate of the rectangle to be drawn.
     * @param width the width of the rectangle to be drawn.
     * @param height the height of the rectangle to be drawn.
     * @param thickness the thickness in pixels
     */
    public void drawRect(Object graphics, int x, int y, int width, int height, int thickness) {
        width--;
        height--;
        for(int iter = 0 ; iter < thickness ; iter++) {
            drawRect(graphics, x + iter, y + iter, width, height);
            width -= 2; height -= 2;
        }
    }

    /**
     * Draws a rounded corner rectangle in the given coordinates with the arcWidth/height
     * matching the last two arguments respectively.
     * 
     * @param graphics the graphics context
     * @param x the x coordinate of the rectangle to be drawn.
     * @param y the y coordinate of the rectangle to be drawn.
     * @param width the width of the rectangle to be drawn.
     * @param height the height of the rectangle to be drawn.
     * @param arcWidth the horizontal diameter of the arc at the four corners.
     * @param arcHeight the vertical diameter of the arc at the four corners.
     */
    public abstract void drawRoundRect(Object graphics, int x, int y, int width, int height, int arcWidth, int arcHeight);

    /**
     * Fills a rounded rectangle in the same way as drawRoundRect
     * 
     * @param graphics the graphics context
     * @param x the x coordinate of the rectangle to be filled.
     * @param y the y coordinate of the rectangle to be filled.
     * @param width the width of the rectangle to be filled.
     * @param height the height of the rectangle to be filled.
     * @param arcWidth the horizontal diameter of the arc at the four corners.
     * @param arcHeight the vertical diameter of the arc at the four corners.
     * @see #drawRoundRect
     */
    public abstract void fillRoundRect(Object graphics, int x, int y, int width, int height, int arcWidth, int arcHeight);

    /**
     * Fills a circular or elliptical arc based on the given angles and bounding 
     * box. The resulting arc begins at startAngle and extends for arcAngle 
     * degrees.
     * 
     * @param graphics the graphics context
     * @param x the x coordinate of the upper-left corner of the arc to be filled.
     * @param y the y coordinate of the upper-left corner of the arc to be filled.
     * @param width the width of the arc to be filled.
     * @param height the height of the arc to be filled.
     * @param startAngle the beginning angle.
     * @param arcAngle the angular extent of the arc, relative to the start angle.
     */
    public abstract void fillArc(Object graphics, int x, int y, int width, int height, int startAngle, int arcAngle);

    /**
     * Draws a circular or elliptical arc based on the given angles and bounding 
     * box
     * 
     * @param graphics the graphics context
     * @param x the x coordinate of the upper-left corner of the arc to be drawn.
     * @param y the y coordinate of the upper-left corner of the arc to be drawn.
     * @param width the width of the arc to be drawn.
     * @param height the height of the arc to be drawn.
     * @param startAngle the beginning angle.
     * @param arcAngle the angular extent of the arc, relative to the start angle.
     */
    public abstract void drawArc(Object graphics, int x, int y, int width, int height, int startAngle, int arcAngle);

    /**
     * Draw a string using the current font and color in the x,y coordinates. The font is drawn
     * from the top position and not the baseline.
     * 
     * @param graphics the graphics context
     * @param str the string to be drawn.
     * @param x the x coordinate.
     * @param y the y coordinate.
     */
    public abstract void drawString(Object graphics, String str, int x, int y);

    /**
     * Draws the image so its top left coordinate corresponds to x/y
     * 
     * @param graphics the graphics context
     * @param img the specified native image to be drawn
     * @param x the x coordinate.
     * @param y the y coordinate.
     */
    public abstract void drawImage(Object graphics, Object img, int x, int y);

    /**
     * Draws the image so its top left coordinate corresponds to x/y
     *
     * @param graphics the graphics context
     * @param img the specified native image to be drawn
     * @param x the x coordinate.
     * @param y the y coordinate.
     * @param w the width
     * @param h the height
     */
    public void drawImage(Object graphics, Object img, int x, int y, int w, int h) {
    }
    
    
    // METHODS FOR DEALING WITH 2-D Paths
    
    public Image createImage(Shape shape, Stroke stroke, int color){
        
        return null;
    }
    

    /**
     * Draws outline of shape on the given graphics context.
     * <p>The last 4 parameters specify a bounding box for drawing the Shape.  The shape's bounds will
     * be made to fit this box exactly for drawing.  This allows for resizing the shape on the GPU
     * if graphics acceleration is supported.</p>
     * @param graphics the graphics context
     * @param shape the shape to draw.
	 * @param stroke The stroke to use for drawing the contour.
     * @see isShapeSupported() to determine of the graphics context supports drawing
     * shapes.
     */
    public void drawShape(Object graphics, Shape shape, Stroke stroke){}
    
    /**
     * Fills the given shape in the specified graphics context using the graphics context's 
     * currently selected color and alpha.
     * @param graphics
     * @param shape
     * @see drawShape To learn what x, y, w, and h do.
     */
    public void fillShape(Object graphics, Shape shape){}
    
    /**
     * Sets the transformation matrix to be applied to all drawing operations. If 
     * originX, originY are non-zero, then the the transformation will first be translated
     * to the origin, then applied, and then translated back.
     * 
     * <p>If isTransformSupported() returns false, then this method won't do anything.</p>
     * <p>If isPerspectiveTransformSupported() returns false, then this method will only 
     * deal with 2D transformation matrices (i.e. the upper left 3x3 matrix of the provided
     * transformation matrix.</p>
     * @param graphics 
     * @param m The transformation matrix.  Can be 3x3 or 4x4.
     * 
     * @see isTransformSupported() To check if this graphics context supports transformations.
     * @see isPerspectiveTransformSupported() To check if this graphics context
     * supports perspective/3D transformations. 
     */
    public void setTransform(Object graphics, Transform transform){
        
    }
    
    /**
     * Gets the current transformation matrix.  This will populate the provided 
     * matrix with the data of the current transformation.
     * @param graphics
     * @see isTransformSupported()
     * @see isPerspectiveTransformSupported()
     * @deprecated Use {@link #getTransform(java.lang.Object, com.codename1.ui.Transform) } instead.
     */
    public Transform getTransform(Object graphics){
        return Transform.makeIdentity();
    }
    
    /**
     * Checks if matrix transformations are supported in the provided graphics context.
     * @param graphics
     * @return True if matrix transformations are supported by this graphics context.
     * 
     * @see setTransform()
     * @see getTransform()
     * @see isPerspectiveTransformSupported()
     */
    public boolean isTransformSupported(Object graphics){
        return false;
    }
    
    
    
    
    /**
     * Checks if 3d/perspective transformations are supported in the provided graphics context.
     * @param graphics
     * @return 
     * 
     * @see setTransform()
     * @see getTransform()
     * @see isTransformSupported()
     * 
     */
    public boolean isPerspectiveTransformSupported(Object graphics){
        return false;
    }
    
    
    
    
    /**
     *  Checks if drawing shapes is supported by the provided graphics context.
     * @param graphics
     * @return 
     * 
     */
    public boolean isShapeSupported(Object graphics){
        return false;
    }
    
    /**
     * Checks if clipping shapes is supported by the provided graphics context.
     * @param graphics
     * @return 
     */
    public boolean isShapeClipSupported(Object graphics){
        return false;
    }
    
    

    // END METHODS FOR DEALING WITH 2-D Paths
    
    /**
     * Allows an implementation to optimize image tiling rendering logic
     * 
     * @param graphics the graphics object
     * @param img the image
     * @param x coordinate to tile the image along
     * @param y coordinate to tile the image along
     * @param w coordinate to tile the image along
     * @param h coordinate to tile the image along 
     */
    public void tileImage(Object graphics, Object img, int x, int y, int w, int h) {
        int iW = getImageWidth(img);
        int iH = getImageHeight(img);
        int clipX = getClipX(graphics);
        int clipW = getClipWidth(graphics);
        int clipY = getClipY(graphics);
        int clipH = getClipHeight(graphics);
        clipRect(graphics, x, y, w, h);
        for (int xPos = 0; xPos <= w; xPos += iW) {
            for (int yPos = 0; yPos < h; yPos += iH) {
                int actualX = xPos + x;
                int actualY = yPos + y;
                if(actualX > clipX + clipW) {
                    continue;
                }
                if(actualX + iW < clipX) {
                    continue;
                }
                if(actualY > clipY + clipH) {
                    continue;
                }
                if(actualY + iH < clipY) {
                    continue;
                }
                drawImage(graphics, img, actualX, actualY);
            }
        }
        setClip(graphics, clipX, clipY, clipW, clipH);
    }

    /**
     * Indicates if the native video player includes its own play/pause etc. controls so the movie player
     * component doesn't need to include them
     * 
     * @return true if the movie player component doesn't need to include such controls
     */
    public boolean isNativeVideoPlayerControlsIncluded() {
        return false;
    }
    
    /**
     * Indicates if image scaling on the fly is supported by the platform, if not Codename One will just scale the images on its own before drawing
     */
    public boolean isScaledImageDrawingSupported() {
        return false;
    }

    /**
     * Draws a portion of the image
     *
     * @param nativeGraphics the graphics context
     * @param img the specified native image to be drawn
     * @param x the x coordinate.
     * @param y the y coordinate.
     * @param imageX location within the image to draw
     * @param imageY location within the image to draw
     * @param imageWidth size of the location within the image to draw
     * @param imageHeight size of the location within the image to draw
     */
    public void drawImageArea(Object nativeGraphics, Object img, int x, int y, int imageX, int imageY, int imageWidth, int imageHeight) {
        int clipX = getClipX(nativeGraphics);
        int clipY = getClipY(nativeGraphics);
        int clipWidth = getClipWidth(nativeGraphics);
        int clipHeight = getClipHeight(nativeGraphics);
        //pushClip(nativeGraphics);
        clipRect(nativeGraphics, x, y, imageWidth, imageHeight);
        if (getClipWidth(nativeGraphics) > 0 && getClipHeight(nativeGraphics) > 0) {
            drawImage(nativeGraphics, img, x - imageX, y - imageY);
        }
        //popClip(nativeGraphics);
        setClip(nativeGraphics, clipX, clipY, clipWidth, clipHeight);
    }

    /**
     * Draws the image so its top left coordinate corresponds to x/y with a fast
     * native rotation in a square angle which must be one of 0, 90, 180 or 270
     * 
     * @param graphics the graphics context
     * @param img the specified native image to be drawn
     * @param x the x coordinate.
     * @param y the y coordinate.
     * @param degrees either 0, 90, 180 or 270 degree rotation for the image drawing
     */
    public void drawImageRotated(Object graphics, Object img, int x, int y, int degrees) {
    }

    /**
     * Indicates whether drawImageRotated is supported by the platform for FAST drawing,
     * if not then its not worth calling the method which will be unimplemented!
     * 
     * @return true if drawImageRotated will draw an image
     */
    public boolean isRotationDrawingSupported() {
        return false;
    }

    /**
     * Draws a filled triangle with the given coordinates
     * 
     * @param graphics the graphics context
     * @param x1 the x coordinate of the first vertex of the triangle
     * @param y1 the y coordinate of the first vertex of the triangle
     * @param x2 the x coordinate of the second vertex of the triangle
     * @param y2 the y coordinate of the second vertex of the triangle
     * @param x3 the x coordinate of the third vertex of the triangle
     * @param y3 the y coordinate of the third vertex of the triangle
     */
    public void fillTriangle(Object graphics, int x1, int y1, int x2, int y2, int x3, int y3) {
        fillPolygon(graphics, new int[]{x1, x2, x3}, new int[]{y1, y2, y3}, 3);
    }

    /**
     * Draws the RGB values based on the MIDP API of a similar name. Renders a 
     * series of device-independent RGB+transparency values in a specified 
     * region. The values are stored in rgbData in a format with 24 bits of 
     * RGB and an eight-bit alpha value (0xAARRGGBB), with the first value 
     * stored at the specified offset. The scanlength  specifies the relative 
     * offset within the array between the corresponding pixels of consecutive 
     * rows. Any value for scanlength is acceptable (even negative values) 
     * provided that all resulting references are within the bounds of the 
     * rgbData array. The ARGB data is rasterized horizontally from left to 
     * right within each row. The ARGB values are rendered in the region 
     * specified by x, y, width and height, and the operation is subject 
     * to the current clip region and translation for this Graphics object.
     * 
     * @param graphics the graphics context
     * @param rgbData an array of ARGB values in the format 0xAARRGGBB
     * @param offset the array index of the first ARGB value
     * @param x the horizontal location of the region to be rendered
     * @param y the vertical location of the region to be rendered
     * @param w the width of the region to be rendered
     * @param h the height of the region to be rendered
     * @param processAlpha true if rgbData has an alpha channel, false if
     * all pixels are fully opaque
     */
    public abstract void drawRGB(Object graphics, int[] rgbData, int offset, int x, int y, int w, int h, boolean processAlpha);

    /**
     * Returns the native graphics object on which all rendering operations occur
     * 
     * @return a native graphics context
     */
    public abstract Object getNativeGraphics();

    /**
     * Returns the native graphics object on the given native image occur
     * 
     * @param image the native image on which the graphics will draw
     * @return a native graphics context
     */
    public abstract Object getNativeGraphics(Object image);

    /**
     * Return the width of the given characters in the given native font instance
     * 
     * @param nativeFont the font for which the string width should be calculated
     * @param ch array of characters
     * @param offset characters offsets
     * @param length characters length
     * @return the width of the given characters in this font instance
     */
    public abstract int charsWidth(Object nativeFont, char[] ch, int offset, int length);

   
    /**
     * Returns the ascent of the specified native font instance.  Should always
     * return a non-negative value.
     * @param nativeFont
     * @return The ascent of the native font instance
     */
    public int getFontAscent(Object nativeFont){
        return (int)(((float)getHeight(nativeFont))*0.7);
    }
    
    /**
     * Returns the descent below the baseline that a font can span.  Should always
     * be non-negative.
     * @param nativeFont
     * @return 
     */
    public int getFontDescent(Object nativeFont){
        return getHeight(nativeFont)-getFontAscent(nativeFont);
    }
    
    /**
     * Checks whether the implementation supports drawing text on the baseline.
     * @return 
     */
    public boolean isBaselineTextSupported(){
        return false;
    }
    /**
     * Return the width of the given string in this font instance
     * 
     * @param nativeFont the font for which the string width should be calculated
     * @param str the given string     * 
     * @return the width of the given string in this font instance
     */
    public abstract int stringWidth(Object nativeFont, String str);

    /**
     * Return the width of the specific character when rendered alone
     * 
     * @param nativeFont the font for which the string width should be calculated
     * @param ch the specific character
     * @return the width of the specific character when rendered alone
     */
    public abstract int charWidth(Object nativeFont, char ch);

    /**
     * Return the total height of the font
     * 
     * @param nativeFont the font for which the string width should be calculated
     * @return the total height of the font
     */
    public abstract int getHeight(Object nativeFont);

    /**
     * Return the global default font instance, if font is passed as null
     * this font should be used
     * 
     * @return the global default font instance
     */
    public abstract Object getDefaultFont();

    /**
     * Optional operation returning the font face for the font 
     * 
     * @param nativeFont the font for which the string width should be calculated
     * @return Optional operation returning the font face for system fonts
     */
    public int getFace(Object nativeFont) {
        return 0;
    }

    /**
     * Optional operation returning the font size for system fonts
     * 
     * @param nativeFont the font for which the string width should be calculated
     * @return Optional operation returning the font size for system fonts
     */
    public int getSize(Object nativeFont) {
        return 0;
    }

    /**
     * Optional operation returning the font style for system fonts
     * 
     * @param nativeFont the font for which the string width should be calculated
     * @return Optional operation returning the font style for system fonts
     */
    public int getStyle(Object nativeFont) {
        return 0;
    }

    /**
     * Creates a new instance of a native font
     * 
     * @param face the face of the font, can be one of FACE_SYSTEM, 
     * FACE_PROPORTIONAL, FACE_MONOSPACE.
     * @param style the style of the font. 
     * The value is an OR'ed  combination of STYLE_BOLD, STYLE_ITALIC, and 
     * STYLE_UNDERLINED; or the value is zero (STYLE_PLAIN).
     * @param size the size of the font, can be one of SIZE_SMALL, 
     * SIZE_MEDIUM, SIZE_LARGE
     * @return a native font object
     */
    public abstract Object createFont(int face, int style, int size);

    /**
     * Subclasses should invoke this method, it delegates the event to the display and into
     * Codename One.
     * 
     * @param keyCode the key for the event
     */
    protected void keyPressed(final int keyCode) {
        Display.getInstance().keyPressed(keyCode);
    }

    /**
     * Subclasses should invoke this method, it delegates the event to the display and into
     * Codename One.
     * 
     * @param keyCode the key for the event
     */
    protected void keyReleased(final int keyCode) {
        Display.getInstance().keyReleased(keyCode);
    }
    
    /**
     * Checks whether the alt key is currently down.  Only relevant on desktop ports.
     * @return 
     */
    public boolean isAltKeyDown() {
        return false;
    }
    
    /**
     * Checks whether the shift key is currently down.  Only relevant on desktop ports.
     * @return 
     */
    public boolean isShiftKeyDown() {
        return false;
    }
    
    /**
     * Checks whether the altgraph key is currently down.  Only relevant on desktop ports.
     * @return 
     */
    public boolean isAltGraphKeyDown() {
        return false;
    }
    
    /**
     * Checks whether the control key is currently down.  Only relevant on desktop ports.
     * @return 
     */
    public boolean isControlKeyDown() {
        return false;
    }
    
    /**
     * Checks whether the meta key is currently down.  Only relevant on desktop ports.
     * @return 
     */
    public boolean isMetaKeyDown() {
        return false;
    }
    
    
    
   

    /**
     * Subclasses should invoke this method, it delegates the event to the display and into
     * Codename One.
     * 
     * @param x the position of the event
     * @param y the position of the event
     */
    protected void pointerDragged(final int x, final int y) {
        xPointerEvent[0] = x;
        yPointerEvent[0] = y;
        pointerDragged(xPointerEvent, yPointerEvent);
    }

    /**
     * Subclasses should invoke this method, it delegates the event to the display and into
     * Codename One.
     * 
     * @param x the position of the event
     * @param y the position of the event
     */
    protected void pointerPressed(final int x, final int y) {
        xPointerEvent[0] = x;
        yPointerEvent[0] = y;
        pointerPressed(xPointerEvent, yPointerEvent);
    }

    /**
     * Subclasses should invoke this method, it delegates the event to the display and into
     * Codename One.
     * 
     * @param x the position of the event
     * @param y the position of the event
     */
    protected void pointerReleased(final int x, final int y) {
        xPointerEvent[0] = x;
        yPointerEvent[0] = y;
        pointerReleased(xPointerEvent, yPointerEvent);
    }

    /**
     * Subclasses should invoke this method, it delegates the event to the display and into
     * Codename One.
     * 
     * @param x the position of the event
     * @param y the position of the event
     */
    protected void pointerHover(final int[] x, final int[] y) {
        Display.getInstance().pointerHover(x, y);
    }

    /**
     * Subclasses should invoke this method, it delegates the event to the display and into
     * Codename One.
     *
     * @param x the position of the event
     * @param y the position of the event
     */
    protected void pointerHoverReleased(final int[] x, final int[] y) {
        Display.getInstance().pointerHoverReleased(x, y);
    }

    /**
     * Subclasses should invoke this method, it delegates the event to the display and into
     * Codename One.
     *
     * @param x the position of the event
     * @param y the position of the event
     */
    protected void pointerHoverReleased(final int x, final int y) {
        xPointerEvent[0] = x;
        yPointerEvent[0] = y;
        pointerHoverReleased(xPointerEvent, yPointerEvent);
    }

    /**
     * Subclasses should invoke this method, it delegates the event to the display and into
     * Codename One.
     *
     * @param x the position of the event
     * @param y the position of the event
     */
    protected void pointerHoverPressed(final int[] x, final int[] y) {
        Display.getInstance().pointerHoverPressed(x, y);
    }

    /**
     * Subclasses should invoke this method, it delegates the event to the display and into
     * Codename One.
     *
     * @param x the position of the event
     * @param y the position of the event
     */
    protected void pointerHoverPressed(final int x, final int y) {
        xPointerEvent[0] = x;
        yPointerEvent[0] = y;
        pointerHoverPressed(xPointerEvent, yPointerEvent);
    }

    /**
     * Subclasses should invoke this method, it delegates the event to the display and into
     * Codename One.
     * 
     * @param x the position of the event
     * @param y the position of the event
     */
    protected void pointerHover(final int x, final int y) {
        xPointerEvent[0] = x;
        yPointerEvent[0] = y;
        pointerHover(xPointerEvent, yPointerEvent);
    }

    /**
     * Subclasses should invoke this method, it delegates the event to the display and into
     * Codename One.
     * 
     * @param x the position of the event
     * @param y the position of the event
     */
    protected void pointerDragged(final int[] x, final int[] y) {
        
        if (dragStarted || hasDragStarted(x, y)) {
            dragStarted = true;
            Display.getInstance().pointerDragged(x, y);
        }
    }


    /**
     * This method can be overriden by subclasses to indicate whether a drag
     * event has started or whether the device is just sending out "noise".
     * This method is invoked by pointer dragged to determine whether to propogate
     * the actual pointer drag event to Codename One.
     *
     * @param x the position of the current drag event
     * @param y the position of the current drag event
     * @return true if the drag should propagate into Codename One
     */
    protected boolean hasDragStarted(final int[] x, final int[] y) {
        return hasDragStarted(x[0], y[0]);
    }

    /**
     * This method can be overriden by subclasses to indicate whether a drag
     * event has started or whether the device is just sending out "noise".
     * This method is invoked by pointer dragged to determine whether to propagate
     * the actual pointer drag event to Codename One.
     * 
     * @param x the position of the current drag event
     * @param y the position of the current drag event
     * @return true if the drag should propagate into Codename One
     */
    protected boolean hasDragStarted(final int x, final int y) {
        // can happen if a user dragged before init, this happens on iOS during splash screen
        if(getCurrentForm() == null) {
            return false;
        }
        if (dragActivationCounter == 0) {
            dragActivationX = x;
            dragActivationY = y;
            dragActivationCounter++;
            return false;
        }
        int dragRegion = getCurrentForm().getDragRegionStatus(x, y);

        //send the drag events to the form only after latency of 7 drag events,
        //most touch devices are too sensitive and send too many drag events.
        //7 is just a latency const number that is pretty good for most devices
        //this may be tuned for specific devices.
        dragActivationCounter++;
        float startX = getDragStartPercentage();
        float startY = startX;
        switch(dragRegion) {
            case Component.DRAG_REGION_NOT_DRAGGABLE:
                if (dragActivationCounter > getDragAutoActivationThreshold()) {
                    return true;
                }
                startX = Math.max(5, startX);
                startY = startX;
                break;
            case Component.DRAG_REGION_LIKELY_DRAG_X:
                startY = Math.max(5, startY);
                startX = 0.9f;
                break;
            case Component.DRAG_REGION_LIKELY_DRAG_Y:
                startX = Math.max(5, startX);
                startY = 0.9f;
                break;
            case Component.DRAG_REGION_LIKELY_DRAG_XY:
                startX = 0.9f;
                startY = 0.9f;
                break;
            case Component.DRAG_REGION_IMMEDIATELY_DRAG_X:
                startX = 0f;
                startY = Math.max(5, startY);
                break;
                
            case Component.DRAG_REGION_IMMEDIATELY_DRAG_Y:
                startY = 0f;
                startX = Math.max(5, startX);
                break;
                
            case Component.DRAG_REGION_IMMEDIATELY_DRAG_XY:
                startX = 0f;
                startY = 0f;
                break;
            case Component.DRAG_REGION_POSSIBLE_DRAG_X:
                startY = Math.max(5, startY);
                startX = Math.min(startY, 2f);
                break;
            case Component.DRAG_REGION_POSSIBLE_DRAG_Y:
                startX = Math.max(5, startX);
                startY = Math.min(startY, 2f);
                break;
            case Component.DRAG_REGION_POSSIBLE_DRAG_XY:
                startX = Math.min(startX, 2f);;
                startY = Math.min(startY, 2f);;
                break;
        }
        
        // have we passed the motion threshold on the X axis?
        if (((float) getDisplayWidth()) / 100.0f * startX <=
                Math.abs(dragActivationX - x)) {
            dragActivationCounter = getDragAutoActivationThreshold() + 1;
            return true;
        }

        // have we passed the motion threshold on the Y axis?
        if (((float) getDisplayHeight()) / 100.0f * startY <=
                Math.abs(dragActivationY - y)) {
            dragActivationCounter = getDragAutoActivationThreshold() + 1;
            return true;
        }

        return false;
    }

    /**
     * This method allows us to manipulate the drag started detection logic.
     * If the pointer was dragged for more than this percentage of the display size it
     * is safe to assume that a drag is in progress.
     *
     * @return motion percentage
     */
    public int getDragStartPercentage() {
        return dragStartPercentage;
    }

    /**
     * This method allows us to manipulate the drag started detection logic.
     * If the pointer was dragged for more than this percentage of the display size it
     * is safe to assume that a drag is in progress.
     *
     * @param dragStartPercentage percentage of the screen required to initiate drag
     */
    public void setDragStartPercentage(int dragStartPercentage) {
        this.dragStartPercentage = dragStartPercentage;
    }

    /**
     * This method allows subclasses to manipulate the drag started detection logic.
     * If more than this number of drag events were delivered it is safe to assume a drag has started
     * This number must be bigger than 0!
     *
     * @return number representing a minimum number of motion events to start a drag operation
     */
    protected int getDragAutoActivationThreshold() {
        return 7;
    }

    /**
     * Subclasses should invoke this method, it delegates the event to the display and into
     * Codename One.
     * 
     * @param x the position of the event
     * @param y the position of the event
     */
    protected void pointerPressed(final int[] x, final int[] y) {
        pointerPressedX = x[0];
        pointerPressedY = y[0];
        Display.getInstance().pointerPressed(x, y);
    }

    /**
     * Subclasses should invoke this method, it delegates the event to the display and into
     * Codename One.
     * 
     * @param x the position of the event
     * @param y the position of the event
     */
    protected void pointerReleased(final int[] x, final int[] y) {
        // this is a special case designed to detect a "flick" event on some Samsung devices
        // that send a pointerPressed/Released with widely differing X/Y values but don't send
        // the pointerDrag events in between
        if(dragActivationCounter == 0 && x[0] != pointerPressedX && y[0] != pointerPressedY) {
            hasDragStarted(pointerPressedX, pointerPressedY);
            if(hasDragStarted(x, y)) {
                pointerDragged(pointerPressedX, pointerPressedY);
                pointerDragged(x, y);
            }
        }
        dragStarted = false;        
        dragActivationCounter = 0;
        Display.getInstance().pointerReleased(x, y);
    }

    /**
     * Subclasses should invoke this method, it delegates the event to the display and into
     * Codename One.
     * 
     * @param w the size of the screen
     * @param h the size of the screen
     */
    protected void sizeChanged(int w, int h) {
        Display.getInstance().sizeChanged(w, h);
    }

    /**
     * Subclasses should invoke this method, it delegates the event to the display and into
     * Codename One.
     */
    protected void hideNotify() {
        Display.getInstance().hideNotify();
    }

    /**
     * Subclasses should invoke this method, it delegates the event to the display and into
     * Codename One.
     */
    protected void showNotify() {
        Display.getInstance().showNotify();
    }

    private Object findCachedGradient(Hashtable cache, int startColor, int endColor, int x, int y, int width, int height, boolean horizontal, int centerX, int centerY, int size) {
        if(cache != null) {
            Enumeration e = cache.keys();
            while(e.hasMoreElements()) {
                int[] current = (int[])e.nextElement();
                Object currentRef = cache.get(current);
                if(currentRef == null) {
                    cache.remove(current);
                    e = cache.keys();
                    continue;
                }
                Object currentImage = extractHardRef(currentRef);
                if(currentImage == null) {
                    cache.remove(current);
                    e = cache.keys();
                    continue;
                }
                if(current[0] == startColor &&
                        current[1] == endColor &&
                        current[2] == x &&
                        current[3] == y &&
                        current[5] == centerX &&
                        current[6] == centerY &&
                        current[7] == size &&
                        getImageWidth(currentImage) == width &&
                        getImageHeight(currentImage) == height) {
                    if((horizontal && current[4] == 1) || ((!horizontal) && current[4] == 0)) {
                        return currentImage;
                    }
                }
            }
        }
        return null;
    }

    private void storeCachedGradient(Object img, Hashtable cache, int startColor, int endColor, int x, int y, boolean horizontal, int centerX, int centerY, int size) {
        int[] key;
        if(horizontal) {
            key = new int[] {startColor, endColor, x, y, 1, centerX, centerY, size};
        } else {
            key = new int[] {startColor, endColor, x, y, 0, centerX, centerY, size};
        }
        cache.put(key, createSoftWeakRef(img));
    }

    /**
     * Draws a radial gradient in the given coordinates with the given colors,
     * doesn't take alpha into consideration when drawing the gradient.
     * Notice that a radial gradient will result in a circular shape, to create
     * a square use fillRect or draw a larger shape and clip to the appropriate size.
     *
     * @param graphics the graphics context
     * @param startColor the starting RGB color
     * @param endColor  the ending RGB color
     * @param x the x coordinate
     * @param y the y coordinate
     * @param width the width of the region to be filled
     * @param height the height of the region to be filled
     * @param relativeX indicates the relative position of the gradient within the drawing region
     * @param relativeY indicates the relative position of the gradient within the drawing region
     * @param relativeSize  indicates the relative size of the gradient within the drawing region
     */
    public void fillRectRadialGradient(Object graphics, int startColor, int endColor, int x, int y, int width, int height, float relativeX, float relativeY, float relativeSize) {
        int centerX = (int) (width * (1 - relativeX));
        int centerY = (int) (height * (1 - relativeY));
        int size = (int)(Math.min(width, height) * relativeSize);
        int x2 = (int)(width / 2 - (size * relativeX));
        int y2 = (int)(height / 2 - (size * relativeY));
        boolean aa = isAntiAliased(graphics);
        setAntiAliased(graphics, false);

        if(cacheRadialGradients()) {
            Object r = findCachedGradient(radialGradientCache, startColor, endColor, x, y, width, height, true, centerX, centerY, size);
            if(r != null) {
                drawImage(graphics, r, x, y);
            } else {
                r = createMutableImage(width, height, 0xffffffff);
                Object imageGraphics = getNativeGraphics(r);
                setColor(imageGraphics, endColor);
                fillRect(imageGraphics, 0, 0, width, height);
                fillRadialGradientImpl(imageGraphics, startColor, endColor, x2, y2, size, size, 0, 360);
                drawImage(graphics, r, x, y);
                if(radialGradientCache == null) {
                    radialGradientCache = new Hashtable();
                }
                storeCachedGradient(r, radialGradientCache, startColor, endColor, x, y, true, centerX, centerY, size);
            }
        } else {
            setColor(graphics, endColor);
            fillRect(graphics, x, y, width, height);

            fillRadialGradientImpl(graphics, startColor, endColor, x + x2, y + y2, size, size, 0, 360);
        }
        if(aa) {
            setAntiAliased(graphics, true);
        }
    }

    /**
     * Draws a radial gradient in the given coordinates with the given colors,
     * doesn't take alpha into consideration when drawing the gradient.
     * Notice that a radial gradient will result in a circular shape, to create
     * a square use fillRect or draw a larger shape and clip to the appropriate size.
     *
     * @param graphics the graphics context
     * @param startColor the starting RGB color
     * @param endColor  the ending RGB color
     * @param x the x coordinate
     * @param y the y coordinate
     * @param width the width of the region to be filled
     * @param height the height of the region to be filled
     */
    public void fillRadialGradient(Object graphics, int startColor, int endColor, int x, int y, int width, int height) {
        fillRadialGradientImpl(graphics, startColor, endColor, x, y, width, height, 0, 360);
    }
    
    
    /**
     * Draws a radial gradient in the given coordinates with the given colors,
     * doesn't take alpha into consideration when drawing the gradient.
     * Notice that a radial gradient will result in a circular shape, to create
     * a square use fillRect or draw a larger shape and clip to the appropriate size.
     *
     * @param graphics the graphics context
     * @param startColor the starting RGB color
     * @param endColor  the ending RGB color
     * @param x the x coordinate
     * @param y the y coordinate
     * @param width the width of the region to be filled
     * @param height the height of the region to be filled
     * @param startAngle the beginning angle.  Zero is at 3 o'clock.  Positive angles are counter-clockwise.
     * @param arcAngle the angular extent of the arc, relative to the start angle. Positive angles are counter-clockwise.
     */
    public void fillRadialGradient(Object graphics, int startColor, int endColor, int x, int y, int width, int height, int startAngle, int arcAngle) {
        fillRadialGradientImpl(graphics, startColor, endColor, x, y, width, height, startAngle, arcAngle);
    }

    private void fillRadialGradientImpl(Object graphics, int startColor, int endColor, int x, int y, int width, int height, int startAngle, int arcAngle) {
        boolean aa = isAntiAliased(graphics);
        setAntiAliased(graphics, false);
        int sourceR = startColor >> 16 & 0xff;
        int sourceG = startColor >> 8 & 0xff;
        int sourceB = startColor & 0xff;
        int destR = endColor >> 16 & 0xff;
        int destG = endColor >> 8 & 0xff;
        int destB = endColor & 0xff;
        int oldColor = getColor(graphics);
        int originalHeight = height;
        boolean outermost = true;
        while (width > 0 && height > 0) {
            if (outermost) {
                setAntiAliased(graphics, true);
            }
            updateGradientColor(graphics, sourceR, sourceG, sourceB, destR,
                    destG, destB, originalHeight, height);
            fillArc(graphics, x, y, width, height, startAngle, arcAngle);
            x++;
            y++;
            width -= 2;
            height -= 2;
            if (outermost) {
                outermost = false;
                setAntiAliased(graphics, false);
            }
        }
        setColor(graphics, oldColor);
        if(aa) {
            setAntiAliased(graphics, true);
        }
    }

    private void updateGradientColor(Object nativeGraphics, int sourceR, int sourceG, int sourceB, int destR,
            int destG, int destB, int distance, int offset) {
        //int a = calculateGraidentChannel(sourceA, destA, distance, offset);
        int r = calculateGraidentChannel(sourceR, destR, distance, offset);
        int g = calculateGraidentChannel(sourceG, destG, distance, offset);
        int b = calculateGraidentChannel(sourceB, destB, distance, offset);
        int color = /*((a << 24) & 0xff000000) |*/ ((r << 16) & 0xff0000) |
                ((g << 8) & 0xff00) | (b & 0xff);
        setColor(nativeGraphics, color);
    }

    /**
     * Converts the color channel value according to the offest within the distance
     */
    private int calculateGraidentChannel(int sourceChannel, int destChannel, int distance, int offset) {
        if (sourceChannel == destChannel) {
            return sourceChannel;
        }
        float ratio = ((float) offset) / ((float) distance);
        int pos = (int) (Math.abs(sourceChannel - destChannel) * ratio);
        if (sourceChannel > destChannel) {
            return sourceChannel - pos;
        } else {
            return sourceChannel + pos;
        }
    }

    /**
     * Draws a linear gradient in the given coordinates with the given colors, 
     * doesn't take alpha into consideration when drawing the gradient
     * 
     * @param graphics the graphics context
     * @param startColor the starting RGB color
     * @param endColor  the ending RGB color
     * @param x the x coordinate
     * @param y the y coordinate
     * @param width the width of the region to be filled
     * @param height the height of the region to be filled
     * @param horizontal indicating wheter it is a horizontal fill or vertical
     */
    public void fillLinearGradient(Object graphics, int startColor, int endColor, int x, int y, int width, int height, boolean horizontal) {
        // this can happen in the resource editor
        if(width <= 0 || height <=0) {
            return;
        }
        boolean aa = isAntiAliased(graphics);
        setAntiAliased(graphics, false);
        if(cacheLinearGradients()) {
            Object r = findCachedGradient(linearGradientCache, startColor, endColor, x, y, width, height, horizontal, 0, 0, 0);
            if(r != null) {
                drawImage(graphics, r, x, y);
            } else {
                r = createMutableImage(width, height, 0xffffffff);
                fillLinearGradientImpl(getNativeGraphics(r), startColor, endColor, 0, 0, width, height, horizontal);
                drawImage(graphics, r, x, y);
                if(linearGradientCache == null) {
                    linearGradientCache = new Hashtable();
                }
                storeCachedGradient(r, linearGradientCache, startColor, endColor, x, y, horizontal, 0, 0, 0);
            }
        } else {
            fillLinearGradientImpl(graphics, startColor, endColor, x, y, width, height, horizontal);
        }
        if(aa) {
            setAntiAliased(graphics, true);
        }
    }

    private void fillLinearGradientImpl(Object graphics, int startColor, int endColor, int x, int y, int width, int height, boolean horizontal) {
        int sourceR = startColor >> 16 & 0xff;
        int sourceG = startColor >> 8 & 0xff;
        int sourceB = startColor & 0xff;
        int destR = endColor >> 16 & 0xff;
        int destG = endColor >> 8 & 0xff;
        int destB = endColor & 0xff;
        int oldColor = getColor(graphics);
        if (horizontal) {
            for (int iter = 0; iter < width; iter++) {
                updateGradientColor(graphics, sourceR, sourceG, sourceB, destR,
                        destG, destB, width, iter);
                drawLine(graphics, x + iter, y, x + iter, y + height);
            }
        } else {
            for (int iter = 0; iter < height; iter++) {
                updateGradientColor(graphics, sourceR, sourceG, sourceB, destR,
                        destG, destB, height, iter);
                drawLine(graphics, x, y + iter, x + width, y + iter);
            }
        }
        setColor(graphics, oldColor);
    }

    private boolean checkIntersection(Object g, int y0, int x1, int x2, int y1, int y2, int[] intersections, int intersectionsCount) {
        if (y0 > y1 && y0 < y2 || y0 > y2 && y0 < y1) {
            if (y1 == y2) {
                drawLine(g, x1, y0, x2, y0);
                return false;
            }
            intersections[intersectionsCount] = x1 + ((y0 - y1) * (x2 - x1)) / (y2 - y1);
            return true;
        }
        return false;
    }

    private int markIntersectionEdge(Object g, int idx, int[] yPoints, int[] xPoints, int nPoints, int[] intersections, int intersectionsCount) {
        intersections[intersectionsCount] = xPoints[idx];

        if ((yPoints[idx] - yPoints[(idx + 1) % nPoints]) * (yPoints[idx] - yPoints[(idx + nPoints - 1) % nPoints]) > 0) {
            intersections[intersectionsCount + 1] = xPoints[idx];
            return 2;

        }

        //Check for special case horizontal line
        if (yPoints[idx] == yPoints[(idx + 1) % nPoints]) {

            drawLine(g, xPoints[idx], yPoints[idx], xPoints[(idx + 1) % nPoints], yPoints[(idx + 1) % nPoints]);

            if ((yPoints[(idx + 1) % nPoints] - yPoints[(idx + 2) % nPoints]) * (yPoints[idx] - yPoints[(idx + nPoints - 1) % nPoints]) > 0) {
                return 1;
            } else {
                intersections[intersectionsCount + 1] = xPoints[idx];
                return 2;
            }

        }
        return 1;
    }

    /**
     *  Fills a closed polygon defined by arrays of x and y coordinates. 
     *  Each pair of (x, y) coordinates defines a point.
     * 
     * @param graphics the graphics context
     *  @param xPoints - a an array of x coordinates.
     *  @param yPoints - a an array of y coordinates.
     *  @param nPoints - a the total number of points.
     */
    public void fillPolygon(Object graphics, int[] xPoints, int[] yPoints, int nPoints) {

        int[] intersections = new int[nPoints];
        int intersectionsCount = 0;


        int yMax = (int) yPoints[0];
        int yMin = (int) yPoints[0];


        for (int i = 0; i < nPoints; i++) {
            yMax = Math.max(yMax, yPoints[i]);
            yMin = Math.min(yMin, yPoints[i]);
        }
        //  Loop through the rows of the image.
        for (int row = yMin; row <= yMax; row++) {

            intersectionsCount = 0;

            for (int i = 1; i < nPoints; i++) {
                if (checkIntersection(graphics, row, xPoints[i - 1], xPoints[i], yPoints[i - 1], yPoints[i], intersections, intersectionsCount)) {
                    intersectionsCount++;
                }
            }
            if (checkIntersection(graphics, row, xPoints[nPoints - 1], xPoints[0], yPoints[nPoints - 1], yPoints[0], intersections, intersectionsCount)) {
                intersectionsCount++;
            }

            for (int j = 0; j < nPoints; j++) {
                if (row == yPoints[j]) {
                    intersectionsCount += markIntersectionEdge(graphics, j, yPoints, xPoints, nPoints, intersections, intersectionsCount);
                }
            }

            int swap = 0;
            for (int i = 0; i < intersectionsCount; i++) {
                for (int j = i; j < intersectionsCount; j++) {
                    if (intersections[j] < intersections[i]) {
                        swap = intersections[i];
                        intersections[i] = intersections[j];
                        intersections[j] = swap;
                    }
                }
            }


            for (int i = 1; i < intersectionsCount; i = i + 2) {
                drawLine(graphics, intersections[i - 1], row, intersections[i], row);
            }
        }
    }

    /**
     *  Draws a closed polygon defined by arrays of x and y coordinates. 
     *  Each pair of (x, y) coordinates defines a point.
     * 
     * @param graphics the graphics context
     *  @param xPoints - a an array of x coordinates.
     *  @param yPoints - a an array of y coordinates.
     *  @param nPoints - a the total number of points.
     */
    public void drawPolygon(Object graphics, int[] xPoints, int[] yPoints, int nPoints) {
        for (int i = 1; i < nPoints; i++) {
            drawLine(graphics, xPoints[i - 1], yPoints[i - 1], xPoints[i], yPoints[i]);
        }
        drawLine(graphics, xPoints[nPoints - 1], yPoints[nPoints - 1], xPoints[0], yPoints[0]);
    }

    /**
     * Returns the type of the input device one of:
     * KEYBOARD_TYPE_UNKNOWN, KEYBOARD_TYPE_NUMERIC, KEYBOARD_TYPE_QWERTY, 
     * KEYBOARD_TYPE_VIRTUAL, KEYBOARD_TYPE_HALF_QWERTY
     * 
     * @return KEYBOARD_TYPE_UNKNOWN
     */
    public int getKeyboardType() {
        return Display.KEYBOARD_TYPE_UNKNOWN;
    }

    /**
     * Indicates whether the device supports native in place editing in which case
     * lightweight input logic shouldn't be used for input.
     * 
     * @return false by default
     */
    public boolean isNativeInputSupported() {
        return false;
    }

    /**
     * Indicates whether the device should switch to native input immediately on first touch
     * 
     * @return false by default
     */
    public boolean isNativeInputImmediate() {
        return false;
    }

    /**
     * Indicates whether the device supports multi-touch events, this is only
     * relevant when touch events are supported
     * 
     * @return false by default
     */
    public boolean isMultiTouch() {
        return false;
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
        return false;
    }

    /**
     * Returns true if indexed images should be used natively
     * 
     * @return true if a native image should be used for indexed images
     */
    public boolean isNativeIndexed() {
        return false;
    }

    /**
     * Creates a native image representing the indexed image
     * 
     * @param image the indexed image
     * @return a native version of the indexed image
     */
    public Object createNativeIndexed(Image image) {
        return null;
    }

    /**
     * Returns true if the image was opaque
     * 
     * @param codenameOneImage the Codename One image 
     * @param nativeImage the image object to test
     * @return true if the image is opaque
     */
    public boolean isOpaque(Image codenameOneImage, Object nativeImage) {
        int[] rgb = codenameOneImage.getRGBCached();
        int rlen = rgb.length;
        for (int iter = 0; iter < rlen; iter++) {
            if ((rgb[iter] & 0xff000000) != 0xff000000) {
                return false;
            }
        }
        return true;
    }

    /**
     * Indicates whether the underlying implementation can draw using an affine
     * transform hence methods such as rotate, scale and shear would work
     *
     * @return true if an affine transformation matrix is present
     */
    public boolean isAffineSupported() {
        return false;
    }

    /**
     * Resets the affine transform to the default value
     * 
     * @param nativeGraphics the native graphics object
     */
    public void resetAffine(Object nativeGraphics) {
        System.out.println("Affine unsupported");
    }

    /**
     * Scales the coordinate system using the affine transform
     *
     * @param nativeGraphics the native graphics object
     * @param x factor for x
     * @param y factor for y
     */
    public void scale(Object nativeGraphics, float x, float y) {
        System.out.println("Affine unsupported");
    }

    /**
     * Rotates the coordinate system around a radian angle using the affine transform
     *
     * @param angle the rotation angle in radians
     * @param nativeGraphics the native graphics object
     */
    public void rotate(Object nativeGraphics, float angle) {
        rotate(nativeGraphics, angle, 0, 0);
    }

    /**
     * Rotates the coordinate system around a radian angle using the affine transform
     *
     * @param angle the rotation angle in radians
     * @param pivotX the pivot location
     * @param pivotY the pivot location
     * @param nativeGraphics the native graphics object
     */
    public void rotate(Object nativeGraphics, float angle, int pivotX, int pivotY) {
        System.out.println("Affine unsupported");
    }

    /**
     * Shear the graphics coordinate system using the affine transform
     *
     * @param x factor for x
     * @param y factor for y
     * @param nativeGraphics the native graphics object
     */
    public void shear(Object nativeGraphics, float x, float y) {
        System.out.println("Affine unsupported");
    }

    /**
     * Indicates whether the underlying platform supports creating an SVG Image
     *
     * @return true if the method create SVG image would return a valid image object
     * from an SVG Input stream
     */
    public boolean isSVGSupported() {
        return false;
    }

    /**
     * Creates an SVG Image from the given byte array data and the base URL
     *
     * @param baseURL URL which is used to resolve relative references within the SVG file
     * @param data the content of the SVG file
     * @return a native image that can be used within the image object
     * @throws IOException if resource lookup fail SVG is unsupported
     */
    public Object createSVGImage(String baseURL, byte[] data) throws IOException {
        throw new IOException("SVG is not supported by this implementation");
    }

    /**
     * Returns a platform specific DOM object that can be manipulated by the user
     * to change the SVG Image
     *
     * @param svgImage the underlying image object
     * @return Platform dependent object, when JSR 226 is supported an SVGSVGElement might
     * be returned.
     */
    public Object getSVGDocument(Object svgImage) {
        throw new RuntimeException("SVG is not supported by this implementation");
    }

    /**
     * Callback to allow images animated by the underlying system to change their state
     * e.g. for SVG or animated gif support. This method returns true if an animation
     * state has changed requiring a repaint.
     *
     * @param nativeImage a native image used within the image object
     * @param lastFrame the time the last frame of animation was shown
     * @return true if a repaint is required since the image state changed, false otherwise
     */
    public boolean animateImage(Object nativeImage, long lastFrame) {
        return false;
    }

    /**
     * Returns a list of the platform names ordered by priority, platform names are
     * used to choose a font based on platform. Since a platform might support several
     * layers for choice in narrowing platform font selection
     *
     * @return the platform names ordered according to priority.
     */
    public String[] getFontPlatformNames() {
        return new String[]{"MIDP", "MIDP2"};
    }

    /**
     * Creates a true type font with the given name/filename (font name might be different from the file name
     * and is required by some devices e.g. iOS). The font file must reside in the src root of the project in
     * order to be detectable. The file name should contain no slashes or any such value.
     *
     * @param fontName the name of the font
     * @param fileName the file name of the font as it appears in the src directory of the project 
     * @return the native font created from the stream
     */
    public Object loadTrueTypeFont(String fontName, String fileName) {
        return null;
    }

    /**
     * Indicates whether the implementation supports loading a font "natively" to handle one of the common
     * native prefixes
     * @return true if the "native:" prefix is supported by loadTrueTypeFont
     */
    public boolean isNativeFontSchemeSupported() {
        return false;
    }
    
    /**
     * Creates a font based on this truetype font with the given pixel, <b>WARNING</b>! This method
     * will only work in the case of truetype fonts!
     * @param font the native font instance
     * @param size the size of the font in pixels
     * @param weight PLAIN, BOLD or ITALIC weight based on the constants in this class
     * @return scaled font instance
     */
    public Object deriveTrueTypeFont(Object font, float size, int weight) {
        throw new RuntimeException("Unsupported operation");
    }
    
    /**
     * Returns true if the system supports dynamically loading truetype fonts from
     * a file.
     *
     * @return true if the system supports dynamically loading truetype fonts from
     * a file.
     */
    public boolean isTrueTypeSupported() {
        return false;
    }

    /**
     * Loads a native font based on a lookup for a font name and attributes. Font lookup
     * values can be separated by commas and thus allow fallback if the primary font
     * isn't supported by the platform.
     *
     * @param lookup string describing the font
     * @return the native font object
     */
    public Object loadNativeFont(String lookup) {
        return null;
    }

    /**
     * Indicates whether loading a font by a string is supported by the platform
     *
     * @return true if the platform supports font lookup
     */
    public boolean isLookupFontSupported() {
        return false;
    }

    /**
     * Minimizes the current application if minimization is supported by the platform (may fail).
     * Returns false if minimization failed.
     *
     * @return false if minimization failed true if it succeeded or seems to be successful
     */
    public boolean minimizeApplication() {
        return false;
    }

    /**
     * Restore the minimized application if minimization is supported by the platform
     */
    public void restoreMinimizedApplication() {
    }

    /**
     * Indicates whether an application is minimized
     *
     * @return true if the application is minimized
     */
    public boolean isMinimized() {
        return false;
    }

    /**
     * Indicates whether the implementation is interested in caching radial gradients for
     * drawing.
     *
     * @return true to activate radial gradient caching
     */
    protected boolean cacheRadialGradients() {
        return true;
    }


    /**
     * Indicates whether the implementation is interested in caching linear gradients for
     * drawing.
     *
     * @return true to activate linear gradient caching
     */
    protected boolean cacheLinearGradients() {
        return true;
    }

    /**
     * Indicates the default status to apply to the 3rd softbutton variable
     *
     * @return true if the 3rd softbutton should be set as true
     * @see com.codename1.ui.Display#isThirdSoftButton()
     * @see com.codename1.ui.Display#setThirdSoftButton()
     */
    public boolean isThirdSoftButton() {
        return false;
    }

    /**
     * Indicates how many drag points are used to calculate dragging speed
     * 
     * @return the size of points to calculate the speed
     */
    public int getDragPathLength(){
        return 10;
    }

    /**
     * Indicates what drag points are valid for the drag speed calculation.
     * Points that are older then the current time - the path time are ignored
     * 
     * @return the relevance time per point
     */
    public int getDragPathTime(){
        return 200;
    }
    /**
     * This method returns the dragging speed based on the latest dragged
     * events
     * @param points array of locations
     * @param dragPathTime the time difference between each point
     * @param dragPathOffset the offset in the arrays
     * @param dragPathLength
     */
    public float getDragSpeed(float[] points, long[] dragPathTime,
            int dragPathOffset, int dragPathLength){
        long now = System.currentTimeMillis();
        final long tooold = now - getDragPathTime();
        int offset = dragPathOffset - dragPathLength;
        if (offset < 0) {
            offset = getDragPathLength() + offset;
        }
        long old = 0;
        float oldPoint = 0;
        float speed = 0;
        long timediff;
        float diff;
        float velocity;
        float f = dragPathLength;
        while (dragPathLength > 0) {
            if (dragPathTime[offset] > tooold) {
                if (old == 0) {
                    old = dragPathTime[offset];
                    oldPoint = points[offset];
                }
                timediff = now - old;
                diff = points[offset] - oldPoint;
                if (timediff > 0) {
                    velocity = (diff / timediff) * 1.5f;
                    speed += velocity;
                }
            }
            dragPathLength--;
            offset++;
            if (offset >= getDragPathLength()) {
                offset = 0;
            }
        }
        f = Math.max(1, f);
        return -speed / f;
    }

    /**
     * Indicates whether Codename One should consider the bidi RTL algorithm
     * when drawing text or navigating with the text field cursor.
     *
     * @return true if the bidi algorithm should be considered
     */
    public boolean isBidiAlgorithm() {
        return bidi;
    }

    /**
     * Indicates whether Codename One should consider the bidi RTL algorithm
     * when drawing text or navigating with the text field cursor.
     *
     * @param activate set to true to activate the bidi algorithm, false to
     * disable it
     */
    public void setBidiAlgorithm(boolean activate) {
        bidi = activate;
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
        if (bidi) {
            if (s.length() >= 2) {
                char[] c = s.toCharArray();
                swapBidiChars(c, 0, s.length(), -1);
                return new String(c);
            }
        }
        return s;
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
        if (bidi) {
            return swapBidiChars(source.toCharArray(), 0, source.length(), index);
        }
        return index;
    }

    private boolean isWhitespace(char c) {
        return c == ' ' || (c == '\n') || (c == '\t') || (c == 10) || (c == 13);
    }

    /**
     * Returns true if the given character is an RTL character or a space
     * character
     *
     * @param c character to test
     * @return true if bidi is active and this is a
     */
    public boolean isRTLOrWhitespace(char c) {
        if (bidi) {
            return isRTL(c) || isWhitespace(c);
        }
        return false;
    }

    /**
     * Returns true if the given character is an RTL character
     *
     * @param c character to test
     * @return true if the charcter is an RTL character
     */
    public boolean isRTL(char c) {
        return (c >= RTL_RANGE_BEGIN && c <= RTL_RANGE_END);
    }

    private final int swapBidiChars(char[] chars, int ixStart, int len, int index) {
        int destIndex = -1;

        int ixEnd = ixStart + len;
        int ix0, ix1;

        ix0 = ix1 = ixStart;

        boolean doSwap = false;
        for (int i1 = ixStart; i1 < ixEnd; i1++) {
            if (isRTL(chars[i1])) {
                doSwap = true;
                break;
            }
        }

        if (doSwap) {
            while (ix0 < ixEnd) {
                if ((ix1 = scanSecond(chars, ix0, ixEnd)) < 0) {
                    break;
                } else {
                    ix0 = ix1;
                    ix1 = scanBackFirst(chars, ix0, ixEnd);
                    // swap
                    for (int iy0 = ix0, iy1 = ix1 - 1; iy0 < iy1; iy0++, iy1--) {
                        char tmp = chars[iy0];
                        chars[iy0] = chars[iy1];
                        chars[iy1] = tmp;

                        if (index == iy1) {
                            //System.out.println("IY: Found char: new index="+iy0);
                            destIndex = iy0;
                            index = iy0;
                        }
                    }

                    ix0 = ix1;
                }
            }
        }

        if (doSwap) {
            // swap the line
            for (ix0 = ixStart, ix1 = ixEnd - 1; ix0 <= ix1; ix0++, ix1--) {
                char ch0 = chars[ix0];
                char ch1 = chars[ix1];

                chars[ix0] = ch1;
                chars[ix1] = ch0;

                if (index == ix0) {
                    destIndex = ix1;
                } else if (index == ix1) {
                    destIndex = ix0;
                }
            }
        }

        return destIndex;

    }

    private boolean isRTLBreak(char ch1) {
        return ch1 == ')' || ch1 == ']' || ch1 == '}' || ch1 == '(' || ch1 == '[' || ch1 == '{';
    }

    private boolean isLTR(char c) {
        return !isRTL(c) && !isRTLBreak(c);
    }

    private final int scanSecond(char[] chars, int ixStart, int ixEnd) {
        int ixFound = -1;
        for (int ix = ixStart; ixFound < 0 && ix < ixEnd; ix++) {
            if (!isRTLOrWhitespace(chars[ix])) {
                ixFound = ix;
            }
        }
        return ixFound;
    }

    private final int scanBackFirst(char[] chars, int ixStart, int ixEnd) {
        int ix, ixFound = ixEnd;
        for (ix = ixStart + 1; ix < ixEnd; ix++) {
            if (isRTL(chars[ix]) || isRTLBreak(chars[ix])) {
                ixFound = ix;
                break;
            }
        }

        for (ix = ixFound - 1; ix >= ixStart; ix--) {
            if (isLTR(chars[ix]) && !isWhitespace(chars[ix])) {
                ixFound = ix + 1;
                break;
            }
        }

        return ixFound;
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
        if(cls != null){
            return cls.getResourceAsStream(resource);
        }
        return getClass().getResourceAsStream(resource);
    }

    /**
     * Animations should return true to allow the native image animation to update
     *
     * @param nativeImage underlying native imae
     * @return true if this is an animation
     */
    public boolean isAnimation(Object nativeImage) {
        return false;
    }

    /**
     * Creates a peer component for the given lightweight component
     *
     * @param nativeComponent a platform specific "native component"
     * @return a Codename One peer component that can be manipulated just like any other
     * Codename One component but would internally encapsulate the given native peer
     */
    public PeerComponent createNativePeer(Object nativeComponent) {
        throw new IllegalArgumentException(nativeComponent.getClass().getName());
    }

    /**
     * Shows a native Form/Canvas or some other heavyweight native screen
     *
     * @param nativeFullScreenPeer the native screen peer
     */
    public void showNativeScreen(Object nativeFullScreenPeer) {
    }

    /**
     * Places the following commands on the native menu system
     *
     * @param commands the Codename One commands to use
     */
    public void setNativeCommands(Vector commands) {
    }

    /**
     * Exits the application...
     */
    public void exitApplication() {
    }

    
    /**
     * Exits the application...
     */
    public void exit() {
        if(onExit != null) {
            onExit.run();
        }
        exitApplication();
    }
    
    /**
     * Returns the property from the underlying platform deployment or the default
     * value if no deployment values are supported. This is equivalent to the
     * getAppProperty from the jad file.
     * <p>The implementation should be responsible for the following keys to return
     * reasonable valid values for the application:
     * <ol>
     * <li>AppName
     * <li>User-Agent - ideally although not required
     * <li>AppVersion
     * <li>Platform - Similar to microedition.platform
     * </ol>
     *
     * @param key the key of the property
     * @param defaultValue a default return value
     * @return the value of the property
     */
    public String getProperty(String key, String defaultValue) {
        return defaultValue;
    }

    /**
     * Returns true if executing this URL should work, returns false if it will not
     * and null if this is unknown.
     * @param url the url that would be executed
     * @return true if executing this URL should work, returns false if it will not
     * and null if this is unknown
     */
    public Boolean canExecute(String url) {
        if(url.startsWith("http:") || url.startsWith("https:")) {
            return Boolean.TRUE;
        }
        return null;
    }
    
    /**
     * Executes the given URL on the native platform
     *
     * @param url the url to execute
     */
    public void execute(String url) {
    }
    
    /**
     * Executes the given URL on the native platform, this method is useful if
     * the platform has the ability to send an event to the app when the execution
     * has ended, currently this works only for Android platform to invoke other
     * intents.
     * 
     * @param url the url to execute
     * @param response a callback from the platform when this execution returned
     * to the application
     */
    public void execute(String url, ActionListener response){
        execute(url);
    }
    
    /**
     * Returns one of the density variables appropriate for this device, notice that
     * density doesn't always correspond to resolution and an implementation might
     * decide to change the density based on DPI constraints.
     *
     * @return one of the DENSITY constants of Display
     */
    public int getDeviceDensity() {
        int d = getActualDisplayHeight() * getDisplayWidth();
        if(isTablet()) {
            // tablets have lower density and allow fitting more details in the screen despite a high resolution
            if(d >= 1440*720) {
                return Display.DENSITY_HIGH;
            }
            return Display.DENSITY_MEDIUM;
        }
        if(d <= 176*220) {
            return Display.DENSITY_VERY_LOW;
        }
        if(d <= 240*320) {
            return Display.DENSITY_LOW;
        }
        if(d <= 360*480) {
            return Display.DENSITY_MEDIUM;
        }
        if(d <= 480*854) {
            return Display.DENSITY_HIGH;
        }
        if(d <= 1440*720) {
            return Display.DENSITY_VERY_HIGH;
        }
        return Display.DENSITY_HD;
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
        playUserSound(soundIdentifier);
    }

    /**
     * Plays a sound defined by the user
     *
     * @param soundIdentifier the sound identifier which can match one of the
     * common constants in this class or be a user/implementation defined sound
     * @return true if a user sound exists and was sent to playback
     */
    protected boolean playUserSound(String soundIdentifier) {
        Object sound = builtinSounds.get(soundIdentifier);
        if(sound == null) {
            return false;
        }
        //playAudio(sound);
        return true;
    }

    /**
     * This method allows implementations to store sound objects natively e.g.
     * in files, byte arrays whatever
     * 
     * @param data native data object
     */
    protected void playNativeBuiltinSound(Object data) {
    }

    /**
     * Converts a sound object to a form which will be easy for the implementation
     * to play later on. E.g. a byte array or a file/file name and return an object
     * that will allow playNativeBuiltinSound() to use
     *
     * @param i stream containing a sound file
     * @return native playback object
     * @throws java.io.IOException thrown by the stream
     */
    protected Object convertBuiltinSound(InputStream i) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int size = i.read(buffer);
        while(size > -1) {
            b.write(buffer, 0, size);
            size = i.read(buffer);
        }
        b.close();
        i.close();
        return b.toByteArray();
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
        builtinSounds.put(soundIdentifier, convertBuiltinSound(data));
    }

    /**
     * Indicates whether a user installed or system sound is available
     *
     * @param soundIdentifier the sound string passed to playBuiltinSound
     * @return true if a sound of this given type is avilable
     */
    public boolean isBuiltinSoundAvailable(String soundIdentifier) {
        return builtinSounds.containsKey(soundIdentifier);
    }

    /**
     * Allows muting/unmuting the builtin sounds easily
     *
     * @param enabled indicates whether the sound is muted
     */
    public void setBuiltinSoundsEnabled(boolean enabled) {
        builtinSoundEnabled = enabled;
    }

    /**
     * Allows muting/unmuting the builtin sounds easily
     *
     * @return true if the sound is *not* muted
     */
    public boolean isBuiltinSoundsEnabled() {
        return builtinSoundEnabled;
    }

    /**
     * Plays the sound in the given URI which is partially platform specific.
     *
     * @param uri the platform specific location for the sound
     * @param onCompletion invoked when the audio file finishes playing, may be null
     * @return a handle that can be used to control the playback of the audio
     * @throws java.io.IOException if the URI access fails
     */
    public Media createMedia(String uri, boolean isVideo, Runnable onCompletion) throws IOException {
        return null;
    }

    /**
     * Plays the sound in the given stream
     *
     * @param stream the stream containing the media data
     * @param mimeType the type of the data in the stream
     * @param onCompletion invoked when the audio file finishes playing, may be null
     * @return a handle that can be used to control the playback of the audio
     * @throws java.io.IOException if the URI access fails
     */
    public Media createMedia(InputStream stream, String mimeType, Runnable onCompletion) throws IOException {
        return null;
    }
    
    /**
     * Creates an audio media that can be played in the background.
     * 
     * @param uri the uri of the media can start with jar://, file://, http:// 
     * (can also use rtsp:// if supported on the platform)
     * 
     * @return Media a Media Object that can be used to control the playback 
     * of the media
     * 
     * @throws IOException if creation of media from the given URI has failed
     */ 
    public Media createBackgroundMedia(String uri) throws IOException {
        if(uri.startsWith("jar://")){
            uri = uri.substring(6);
            if(!uri.startsWith("/")){
                uri = "/" + uri;
            }
            InputStream is = getResourceAsStream(this.getClass(), uri);
            String mime = "";
            if(uri.endsWith(".mp3")){
                mime = "audio/mp3";
            }else if(uri.endsWith(".wav")){
                mime = "audio/x-wav";            
            }else if(uri.endsWith(".amr")){
                mime = "audio/amr";            
            }else if(uri.endsWith(".3gp")){
                mime = "audio/3gpp";            
            }

            return createMedia(is, mime, null);
        }
        return createMedia(uri, false, null);
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
        return new WeakReference(o);
    }

    /**
     * Extracts the hard reference from the soft/weak reference given
     *
     * @param o the reference returned by createSoftWeakRef
     * @return the original object submitted or null
     */
    public Object extractHardRef(Object o) {
        WeakReference w = (WeakReference)o;
        if(w != null) {
            return w.get();
        }
        return null;
    }

    /**
     * This method notifies the implementation about the chosen commands
     * behavior
     * @param commandBehavior see Display.COMMAND_BEHAVIOR...
     */
    public void notifyCommandBehavior(int commandBehavior){
    }

    /**
     * Indicates if the implemenetation has a native underlying theme
     * 
     * @return true if the implementation has a native theme available
     */
    public boolean hasNativeTheme() {
        return false;
    }

    /**
     * Installs the native theme, this is only applicable if hasNativeTheme() returned true. Notice that this method
     * might replace the DefaultLookAndFeel instance and the default transitions.
     */
    public void installNativeTheme() {
        throw new RuntimeException();
    }

    /**
     * Performs a clipboard copy operation, if the native clipboard is supported by the implementation it would be used
     *
     * @param obj object to copy, while this can be any arbitrary object it is recommended that only Strings or Codename One
     * image objects be used to copy
     */
    public void copyToClipboard(Object obj) {
        lightweightClipboard = obj;
    }

    /**
     * Returns the current content of the clipboard
     *
     * @return can be any object or null see copyToClipboard
     */
    public Object getPasteDataFromClipboard() {
        return lightweightClipboard;
    }

    /**
     * Returns true if the device is currently in portrait mode
     *
     * @return true if the device is in portrait mode
     */
    public boolean isPortrait() {
        return getDisplayWidth() < getActualDisplayHeight();
    }

    /**
     * Returns true if the device allows forcing the orientation via code, feature phones do not allow this
     * although some include a jad property allowing for this feature
     *
     * @return true if lockOrientation  would work
     */
    public boolean canForceOrientation() {
        return false;
    }

    /**
     * On devices that return true for canForceOrientation() this method can lock the device orientation
     * either to portrait or landscape mode
     *
     * @param portrait true to lock to portrait mode, false to lock to landscape mode
     */
    public void lockOrientation(boolean portrait) {
    }

    /**
     * This is the reverse method for lock orientation allowing orientation lock to be disabled
     */
    public void unlockOrientation() {   
    }

    /**
     * An implementation can return true if it supports embedding a native browser widget
     *
     * @return true if the implementation supports embedding a native browser widget
     */
    public boolean isNativeBrowserComponentSupported() {
        return false;
    }

    /**
     * Some platforms require that you enable pinch to zoom explicitly. This method has no
     * effect if pinch to zoom isn't supported by the platform
     * 
     * @param browserPeer browser instance
     * @param e true to enable pinch to zoom, false to disable it
     */
    public void setPinchToZoomEnabled(PeerComponent browserPeer, boolean e) {
    }
    
    /**
     * Allows disabling the browsers native scrolling on devices that support it
     * 
     * @param browserPeer browser instance
     * @param e true to enables scrolling and false disables it
     */
    public void setNativeBrowserScrollingEnabled(PeerComponent browserPeer, boolean e) {
    }
    
    /**
     * If the implementation supports the creation of a browser component it should be returned in this
     * method
     *
     * @param  browserComponent instance of the browser component thru which events should be fired
     * @return an instance of the native browser peer or null
     */
    public PeerComponent createBrowserComponent(Object browserComponent) {
        return null;
    }

    /**
     * This method allows customizing the properties of a web view in various ways including platform specific settings.
     * When a property isn't supported by a specific platform it is just ignored.
     *
     * @param browserPeer browser instance
     * @param key see the documentation with the Codename One Implementation for further details
     * @param value see the documentation with the Codename One Implementation for further details
     */
    public void setBrowserProperty(PeerComponent browserPeer, String key, Object value) {
    }

    /**
     * The page title
     * @param browserPeer browser instance
     * @return the title
     */
    public String getBrowserTitle(PeerComponent browserPeer) {
        return null;
    }

    /**
     * The page URL
     * @param browserPeer browser instance
     * @return the URL
     */
    public String getBrowserURL(PeerComponent browserPeer) {
        return null;
    }

    /**
     * Sets a relative URL from the html hierarchy
     * 
     * @param browserPeer the peer component
     * @param url the url relative to the HTML directory
     */
    public void setBrowserPageInHierarchy(PeerComponent browserPeer, String url) throws IOException {
        installTar();

        FileSystemStorage fs = FileSystemStorage.getInstance();
        String tardir = fs.getAppHomePath() + "cn1html";
        if(tardir.startsWith("/")) {
            tardir = "file://" + tardir;
        }
        if(url.startsWith("/")) {
            setBrowserURL(browserPeer, tardir + url);
        } else {
            setBrowserURL(browserPeer, tardir  + "/" + url);            
        }
    }
    
    /**
     * Sets the page URL, jar: URL's must be supported by the implementation
     * @param browserPeer browser instance
     * @param url  the URL
     */
    public void setBrowserURL(PeerComponent browserPeer, String url) {
        // load from jar:// URL's
        try {
            InputStream i = Display.getInstance().getResourceAsStream(getClass(), url.substring(6));
            if(i == null) {
                System.out.println("Local resource not found: " + url);
                return;
            }
            byte[] buffer = new byte[4096];
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            int size = i.read(buffer);
            while(size > -1) {
                bo.write(buffer, 0, size);
                size = i.read(buffer);
            }
            i.close();
            bo.close();
            String htmlText = new String(bo.toByteArray(), "UTF-8");
            String baseUrl = url.substring(0, url.lastIndexOf('/'));
            setBrowserPage(browserPeer, htmlText, baseUrl);
            return;
        } catch (IOException ex) {
            Log.e(ex);
        }
    }

    /**
     * Reload the current page
     * @param browserPeer browser instance
     */
    public void browserReload(PeerComponent browserPeer) {
    }

    /**
     * Indicates whether back is currently available
     * @param browserPeer browser instance
     * @return true if back should work
     */
    public boolean browserHasBack(PeerComponent browserPeer) {
        return false;
    }

    /**
     * Indicates whether forward is currently available
     * @param browserPeer browser instance
     * @return true if forward should work
     */
    public boolean browserHasForward(PeerComponent browserPeer) {
        return false;
    }

    /**
     * Navigates back in the history
     * @param browserPeer browser instance
     */
    public void browserBack(PeerComponent browserPeer) {
    }

    /**
     * Stops loading the current page
     * @param browserPeer browser instance
     */
    public void browserStop(PeerComponent browserPeer) {
    }

    /**
     * Release browser native resources
     * @param internal browser instance
     */
    public void browserDestroy(PeerComponent internal) {
    }
    
    /**
     * Navigates forward in the history
     * @param browserPeer browser instance
     */
    public void browserForward(PeerComponent browserPeer) {
    }

    /**
     * Clears navigation history
     * @param browserPeer browser instance
     */
    public void browserClearHistory(PeerComponent browserPeer) {
    }

    /**
     * Shows the given HTML in the native viewer
     *
     * @param browserPeer browser instance
     * @param html HTML web page
     * @param baseUrl base URL to associate with the HTML
     */
    public void setBrowserPage(PeerComponent browserPeer, String html, String baseUrl) {
    }

    /**
     * Executes the given JavaScript string within the current context
     *
     * @param browserPeer browser instance
     * @param javaScript the JavaScript string
     */
    public void browserExecute(PeerComponent browserPeer, String javaScript) {
        setBrowserURL(browserPeer, "javascript:(function(){" + javaScript + "})()");
    }

    /**
     * Executes javascript and returns string. The default implementation
     * just wraps the browserExecute() method that doesn't return anything. It will
     * return null always. You need to override this in the native implementation
     * to return meaningful values.
     * @param internal The peer browser component.
     * @param javaScript The javascript to execute.
     * @return String result of the javascript expression.
     */
    public String browserExecuteAndReturnString(PeerComponent internal, String javaScript) {
        browserExecute(internal, javaScript);
        return null;
    }

    /**
     * Allows exposing the given object to JavaScript code so the JavaScript code can invoke methods
     * and access fields on the given object. Notice that on RIM devices which don't support reflection
     * this object must implement the propriatery Scriptable interface
     * http://www.blackberry.com/developers/docs/5.0.0api/net/rim/device/api/script/Scriptable.html
     *
     * @param browserPeer browser instance
     * @param o the object to invoke, notice all public fields and methods would be exposed to JavaScript
     * @param name the name to expose within JavaScript
     */
    public void browserExposeInJavaScript(PeerComponent browserPeer, Object o, String name) {
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
        switch(getDeviceDensity()) {
            case Display.DENSITY_VERY_LOW:
                return dipCount;
            case Display.DENSITY_LOW:
                return dipCount * 2;
            case Display.DENSITY_MEDIUM:
                return dipCount * 5;
            case Display.DENSITY_HIGH:
                return dipCount * 10;
            case Display.DENSITY_VERY_HIGH:
                return dipCount * 14;
            case Display.DENSITY_HD:
                return dipCount * 20;
        }
        return dipCount;
    }

    /**
     * Indicates whether the device is a tablet, notice that this is often a guess
     *
     * @return true if the device is assumed to be a tablet
     */
    public boolean isTablet() {
        return false;
    }
    
    /**
     * Returns true if this is a desktop application
     * @return true if this is a desktop application
     */
    public boolean isDesktop() {
        return false;
    }
    
    /**
     * Returns true if the device has dialing capabilities
     * @return false if it cannot dial
     */
    public boolean canDial() {
        return !isTablet() && !isDesktop();
    }
    
    /**
     * Allows an implementation to modify setting thread priority, some implementations
     * don't handle thread priorities well
     * 
     * @param t the thread
     * @param p the priority
     */
    public void setThreadPriority(Thread t, int p) {
        t.setPriority(p);
    }

    /**
     * Callback allowing the implementation to perform an operation on the init thread
     * after initialization was completed
     */
    public void postInit() {
        initDefaultUserAgent();
    }

    /**
     * Some old platforms might need this but for modern platforms the user agent should "just work".
     */
    protected void initDefaultUserAgent() {
        //sets the default device user agent if available by the platform, by default
        //we set Nokia, beacause if the user agent is empty it is most likely a J2ME device
        ConnectionRequest.setDefaultUserAgent(Display.getInstance().getProperty("User-Agent", 
                "Mozilla/5.0 (SymbianOS/9.4; Series60/5.0 NokiaN97-1/20.0.019; Profile/MIDP-2.1 Configuration/CLDC-1.1) AppleWebKit/525 (KHTML, like Gecko) BrowserNG/7.1.18124"));
    }
    
    /**
     * Allows for easier debugging of native implementations by setting the image name to
     * the native image object
     * 
     * @param nativeImage the native image
     * @param name the name/file name identifying the image
     */
    public void setImageName(Object nativeImage, String name) { 
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
        return true;
    }

    public void addCookie(Cookie [] cookiesArray) {
        if(cookies == null){
            cookies = new Hashtable();
        }
        int calen = cookiesArray.length;
        for (int i = 0; i < calen; i++) {
            Cookie cookie = cookiesArray[i];
            Hashtable h = (Hashtable)cookies.get(cookie.getDomain());
            if(h == null){
                h = new Hashtable();
                cookies.put(cookie.getDomain(), h);
            }
            h.put(cookie.getName(), cookie);
        }
        
        if(Cookie.isAutoStored()){
            if(Storage.getInstance().exists(Cookie.STORAGE_NAME)){
                Storage.getInstance().deleteStorageFile(Cookie.STORAGE_NAME);
            }
            Storage.getInstance().writeObject(Cookie.STORAGE_NAME, cookies);
        }
    }

    /**
     * Adds/replaces a cookie to be sent to the given domain
     * 
     * @param c cookie to add
     */
    public void addCookie(Cookie c) {
        if(cookies == null){
            cookies = new Hashtable();
        }
        Hashtable h = (Hashtable)cookies.get(c.getDomain());
        if(h == null){
            h = new Hashtable();
            cookies.put(c.getDomain(), h);
        }
        h.put(c.getName(), c);
        if(Cookie.isAutoStored()){
            if(Storage.getInstance().exists(Cookie.STORAGE_NAME)){
                Storage.getInstance().deleteStorageFile(Cookie.STORAGE_NAME);
            }
            Storage.getInstance().writeObject(Cookie.STORAGE_NAME, cookies);
        }
    }

    /**
     * Returns the domain for the given URL
     * 
     * @param url a url
     * @return the domain
     */
    public String getURLDomain(String url) {
        String domain = url.substring(url.indexOf("//") + 2);
        int i = domain.indexOf('/');
        if(i > -1) {
            domain = domain.substring(0, i);
        }
        return domain;
    }
    
    public String getURLPath(String url){
        String path = url.substring(url.indexOf("//") + 2);
        int i = path.indexOf('/');
        if(i > -1) {
            path = path.substring(i);
        }
        i = path.indexOf('?');
        if ( i > -1 ){
            path = path.substring(0, i);
        }
        
        i = path.indexOf('#');
        if ( i > -1 ){
            path = path.substring(0, i);
        }
        
        return path; 
    }

    /**
     * Returns the cookies for this URL
     * 
     * @param url the url on which we are checking for cookies
     * @return the cookies to submit to the given URL
     */
    public Vector getCookiesForURL(String url) {
        Vector response = null;
        if (Cookie.isAutoStored()) {
            cookies = (Hashtable) Storage.getInstance().readObject(Cookie.STORAGE_NAME);
        }
        
        String protocol = "";
        int pos = -1;
        if ( (pos = url.indexOf(":")) >= 0 ){
            protocol = url.substring(0, pos);
        }
        boolean isHttp = ("http".equals(protocol) || "https".equals(protocol) );
        boolean isSecure = "https".equals(protocol);
        String path = getURLPath(url);
                
        
        if(cookies != null && cookies.size() > 0) {
            String domain = getURLDomain(url);
            Enumeration e = cookies.keys();
            while (e.hasMoreElements()) {
                String domainKey = (String) e.nextElement();
                if (domain.indexOf(domainKey) > -1) {
                    Hashtable h = (Hashtable) cookies.get(domainKey);
                    if (h != null) {
                        Enumeration enumCookies = h.elements();
                        if(response == null){
                            response = new Vector();
                        }
                        while (enumCookies.hasMoreElements()) {
                            Cookie nex = (Cookie)enumCookies.nextElement();
                            if ( nex.isHttpOnly() && !isHttp ){
                                continue;
                            }
                            if ( nex.isSecure() && !isSecure ){
                                continue;
                            }
                            if ( path.indexOf(nex.getPath()) != 0 ){
                                continue;
                            }
                            response.addElement(nex);
                        }
                    }
                }
            }
        }
        return response;
    }

    public void clearNativeCookies(){
        
    }
    
    /**
     * Connects to a given URL, returns a connection object to be used with the implementation
     * later
     *
     * @param url the URL to connect to
     * @param read indicates whether the connection will be read from
     * @param write indicates whether writing will occur into the connection
     * @return a URL instance
     */
    public abstract Object connect(String url, boolean read, boolean write) throws IOException;
    
    /**
     * Gets the SSL certificates for a connection
     * @param connection The connection.
     * @param url The url of the connection.
     * @return String array where each certificate is in form {@literal <ALGORITHM>:<FINGERPRINT>}
     * @throws IOException 
     */
    public String[] getSSLCertificates(Object connection, String url) throws IOException {
        return new String[0];
    }
    
    /**
     * Checks if the platform supports getting SSL certificates.
     * @return True if the platform supports SSL certificates.
     */
    public boolean canGetSSLCertificates() {
        return false;
    }

    /**
     * Connects to a given URL, returns a connection object to be used with the implementation
     * later
     *
     * @param url the URL to connect to
     * @param read indicates whether the connection will be read from
     * @param write indicates whether writing will occur into the connection
     * @param timeout the timeout version of this method
     * @return a URL instance
     */
    public Object connect(String url, boolean read, boolean write, int timeout) throws IOException {
        return connect(url, read, write);
    }

    /**
     * Requests special http method such as put or delete
     * @param connection the connection object
     * @param method the method string
     */
    public void setHttpMethod(Object connection, String method) throws IOException {
    }
    
    /**
     * Indicates the HTTP header value for an HTTP connection
     *
     * @param connection the connection object
     * @param key the key for the header
     * @param val the value for the header
     */
    public abstract void setHeader(Object connection, String key, String val);

    /**
     * This method is used to enable streaming of a HTTP request body without 
     * internal buffering, when the content length is not known in advance. 
     * In this mode, chunked transfer encoding is used to send the request body. 
     * Note, not all HTTP servers support this mode.
     * This mode is supported on Android and the Desktop ports.
     * 
     * @param connection the connection object
     * @param bufferLen The number of bytes to write in each chunk. If chunklen 
     * is less than or equal to zero, a default value will be used.
     */ 
    public void setChunkedStreamingMode(Object connection, int bufferLen){    
    }
    
    /**
     * Closes the object (connection, stream etc.) without throwing any exception, even if the
     * object is null
     *
     * @param o Connection, Stream or other closeable object
     */
    public void cleanup(Object o) {
        try {
            if(o != null) {
                if(o instanceof InputStream) {
                    ((InputStream) o).close();
                    return;
                }
                if(o instanceof OutputStream) {
                    ((OutputStream) o).close();
                    return;
                }
                if(o instanceof Reader) {
                    ((Reader) o).close();
                    return;
                }
                if(o instanceof Writer) {
                    ((Writer) o).close();
                }
                if(o instanceof Database) {
                    ((Database)o).close();
                }
                if(o instanceof Cursor) {
                    ((Cursor)o).close();
                }
            }
        } catch (Throwable ex) {
            Log.e(ex);
        }
    }

    /**
     * Checks if this platform supports custom cursors.  
     * @return True if the platform supports custom cursors.
     * @see Form#setEnableCursors(boolean) 
     * @see Component#setCursor(int) 
     * @see ComponentSelector#setCursor(int) 
     */
    public boolean isSetCursorSupported() {
        return false;
    }
    
    /**
     * Returns the content length for this connection
     * 
     * @param connection the connection 
     * @return the content length
     */
    public abstract int getContentLength(Object connection);

    /**
     * Returns an output stream for the given connection
     *
     * @param connection the connection to open an output stream on
     * @return the created output stream
     * @throws IOException thrown by underlying implemnetation
     */
    public abstract OutputStream openOutputStream(Object connection) throws IOException;

    /**
     * Returns an output stream for the given connection
     *
     * @param connection the connection to open an output stream on
     * @param offset position in the file
     * @return the created output stream
     * @throws IOException thrown by underlying implemnetation
     */
    public abstract OutputStream openOutputStream(Object connection, int offset) throws IOException;

    /**
     * Returns an input stream for the given connection
     *
     * @param connection the connection to open an input stream on
     * @return the created input stream
     * @throws IOException thrown by underlying implemnetation
     */
    public abstract InputStream openInputStream(Object connection) throws IOException;

    /**
     * Returns an output stream for the given file
     *
     * @param file the file to which we should open a stream
     * @return the created output stream
     * @throws IOException thrown by underlying implemnetation
     */
    public OutputStream openFileOutputStream(String file) throws IOException {
        return openOutputStream(file);
    }

    /**
     * Returns an input stream for the given connection
     *
     * @param file the file to which we should open a stream
     * @return the created input stream
     * @throws IOException thrown by underlying implemnetation
     */
    public InputStream openFileInputStream(String file) throws IOException {
        return openInputStream(file);
    }

    /**
     * Indicates the whether the request method is GET or POST
     *
     * @param connection the connection object
     * @param p true for post false for get
     */
    public abstract void setPostRequest(Object connection, boolean p);

    /**
     * Returns the server response code for the request
     *
     * @param connection the connection object
     * @return a numeric HTTP response code
     * @throws IOException if the request failed
     */
    public abstract int getResponseCode(Object connection) throws IOException;

    /**
     * Returns the server response message for the request
     *
     * @param connection the connection object
     * @return a text message to go along with the response code
     * @throws IOException if the request failed
     */
    public abstract String getResponseMessage(Object connection) throws IOException;

    /**
     * Returns the HTTP response header field
     *
     * @param name field name for http header
     * @param connection the connection object
     * @return the value of the header field
     * @throws IOException if the request failed
     */
    public abstract String getHeaderField(String name, Object connection) throws IOException;

    /**
     * Returns the HTTP response header field
     *
     * @param connection the connection object
     * @return the value of the header field
     * @throws IOException if the request failed
     */
    public abstract String[] getHeaderFieldNames(Object connection) throws IOException;
    
    /**
     * Returns the HTTP response header fields, returns optionally more than one result or null if
     * no field is present.
     *
     * @param name field name for http header
     * @param connection the connection object
     * @return the values of the header fields
     * @throws IOException if the request failed
     */
    public abstract String[] getHeaderFields(String name, Object connection) throws IOException;

    /**
     * Indicates whether the underlying implementation supports the notion of a network operation
     * timeout. If not timeout is "faked"
     * @return true if HTTP timeout can be configured for this IO implementation
     */
    public boolean isTimeoutSupported() {
        return false;
    }

    /**
     * This will work only if http timeout is supported
     * 
     * @param t time in milliseconds
     */
    public void setTimeout(int t) {
    }

    /**
     * Flush the storage cache allowing implementations that cache storage objects
     * to store
     */
    public void flushStorageCache() {
    }


    /**
     * The storage data is used by some storage implementations (e.g. CDC) to place the
     * storage object in a "proper" location matching the application name. This needs to
     * be set by the user, the name might be ignored in platforms (such as MIDP) where storage
     * is mapped to a native application specific storage.
     *
     * @param storageData the name for the storage or its context
     */
    public void setStorageData(Object storageData) {
        this.storageData = storageData;
    }

    /**
     * The storage data is used by some storage implementations (e.g. CDC) to place the
     * storage object in a "proper" location matching the application name. This needs to
     * be set by the user, the name might be ignored in platforms (such as MIDP) where storage
     * is mapped to a native application specific storage.
     *
     * @return the name for the storage
     */
    public Object getStorageData() {
        return storageData;
    }

    /**
     * Deletes the given file name from the storage
     *
     * @param name the name of the storage file
     */
    public abstract void deleteStorageFile(String name);

    /**
     * Deletes all the files in the application storage
     */
    public void clearStorage() {
        String[] l = listStorageEntries();
        int llen = l.length;
        for(int iter = 0 ; iter < llen ; iter++) {
            deleteStorageFile(l[iter]);
        }
    }

    /**
     * Creates an output stream to the storage with the given name
     *
     * @param name the storage file name
     * @return an output stream of limited capcity
     */
    public abstract OutputStream createStorageOutputStream(String name) throws IOException;

    /**
     * Creates an input stream to the given storage source file
     *
     * @param name the name of the source file
     * @return the input stream
     */
    public abstract InputStream createStorageInputStream(String name) throws IOException;

    /**
     * Returns true if the given storage file exists
     *
     * @param name the storage file name
     * @return true if it exists
     */
    public abstract boolean storageFileExists(String name);

    /**
     * Lists the names of the storage files
     *
     * @return the names of all the storage files
     */
    public abstract String[] listStorageEntries();

    /**
     * Returns the size of the entry in bytes
     * @param name the entry name
     * @return the size
     */
    public int getStorageEntrySize(String name) {
        long size = -1;
        try {
            InputStream i = createStorageInputStream(name);
            long val = i.skip(1000000);
            if(val > -1) {
                size = 0;
                while(val > -1) {
                    size += val;
                    val = i.skip(1000000);
                }
            }
            Util.cleanup(i);
        } catch(IOException err) {
            Log.e(err);
        }
        return (int)size;
    }
    
    /**
     * Returns the filesystem roots from which the structure of the file system
     * can be traversed
     *
     * @return the roots of the filesystem
     */
    public abstract String[] listFilesystemRoots();

    /**
     * Lists the files within the given directory, returns relative file names and not
     * full file names.
     *
     * @param directory the directory in which files should be listed
     * @return array of file names
     */
    public abstract String[] listFiles(String directory) throws IOException;

    /**
     * Returns the size of the given root directory
     *
     * @param root the root directory in the filesystem
     * @return the byte size of the directory
     */
    public abstract long getRootSizeBytes(String root);

    /**
     * Returns the available space in the given root directory
     *
     * @param root the root directory in the filesystem
     * @return the bytes available in the directory
     */
    public abstract long getRootAvailableSpace(String root);

    /**
     * Creates the given directory
     *
     * @param directory the directory name to create
     */
    public abstract void mkdir(String directory);
    
    /**
     * Deletes the specific file or empty directory.
     *
     * @param file file or empty directory to delete
     */
    public abstract void deleteFile(String file);

    /**
     * Indicates the hidden state of the file
     *
     * @param file file
     * @return true for a hidden file
     */
    public abstract boolean isHidden(String file);

    /**
     * Toggles the hidden state of the file
     *
     * @param file file
     * @param h hidden state
     */
    public abstract void setHidden(String file, boolean h);

    /**
     * Returns the length of the file
     *
     * @param file file
     * @return length of said file
     */
    public abstract long getFileLength(String file);

    /**
     * Returns the time that the file denoted by this abstract pathname was 
     * last modified.
     * @return A long value representing the time the file was last modified, 
     * measured in milliseconds
     */ 
    public long getFileLastModified(String file) {
        return -1;
    }
    
    /**
     * Indicates whether the given file is a directory
     *
     * @param file file
     * @return true if its a directory
     */
    public abstract boolean isDirectory(String file);

    /**
     * Indicates whether the given file exists
     *
     * @param file file
     * @return true if it exists
     */
    public abstract boolean exists(String file);

    /**
     * Renames a file to the given name, expects the new name to be relative to the
     * current directory
     *
     * @param file absolute file name
     * @param newName relative new name
     */
    public abstract void rename(String file, String newName);

    /**
     * Returns the file system separator char normally '/'
     *
     * @return the separator char
     */
    public abstract char getFileSystemSeparator();

    /**
     * Indicates whether looking up an access point is supported by this device
     * 
     * @return true if access point lookup is supported
     */
    public boolean isAPSupported() {
        return false;
    }

    /**
     * Returns the ids of the access points available if supported
     *
     * @return ids of access points
     */
    public String[] getAPIds() {
       return null;
    }

    /**
     * Returns the type of the access point
     *
     * @param id access point id
     * @return one of the supported access point types from network manager
     */
    public int getAPType(String id) {
        return NetworkManager.ACCESS_POINT_TYPE_UNKNOWN;
    }

    /**
     * Returns the user displayable name for the given access point
     *
     * @param id the id of the access point
     * @return the name of the access point
     */
    public String getAPName(String id) {
        return null;
    }

    /**
     * Returns the id of the current access point
     *
     * @return id of the current access point
     */
    public String getCurrentAccessPoint() {
        return null;
    }

    /**
     * Returns the id of the current access point
     *
     * @param id id of the current access point
     */
    public void setCurrentAccessPoint(String id) {
    }

    /**
     * For some reason the standard code for writing UTF8 output in a server request
     * doesn't work as expected on SE/CDC stacks.
     *
     * @return true if the getBytes() approach should be used
     */
    public boolean shouldWriteUTFAsGetBytes() {
        return false;
    }

    /**
     * Some devices need more elaborate thread creation logic e.g. to increase the
     * default stack size or might use a pooling strategy
     *
     * @param name the name of the thread
     * @param r the runnable
     */
    public void startThread(String name, Runnable r) {
        new CodenameOneThread(r, name).start();
    }

    /**
     * Allows binding logic to occur before closing the output stream
     * such as syncing
     *
     * @param s the closing stream
     */
    public void closingOutput(OutputStream s) {
    }

    /**
     * Allows the logger to print the stack trace into the log when the native
     * platform supports that
     *
     * @param t the exception
     * @param o the writer
     */
    public void printStackTraceToStream(Throwable t, Writer o) {
    }

    /**
     * This method is useful strictly for debugging, the logger can use it to track
     * file opening/closing thus detecting potential file resource leaks that
     * can cause serious problems in some OS's.
     *
     * @param al action listener to receive the callback
     */
    public void setLogListener(ActionListener al) {
        logger = al;
    }

    /**
     * Indicates whether logging is turned on
     * @return true or false
     */
    protected boolean isLogged() {
        return logger != null;
    }

    /**
     * Dispatch the message to the logger
     * @param content content of the message
     */
    protected void log(String content) {
        logger.actionPerformed(new ActionEvent(content,ActionEvent.Type.Log));
    }

    /**
     * System print
     * 
     * @param content 
     */
    public void systemOut(String content){
        System.out.println(content);
    }
    
    /**
     * Logs the creation of a stream
     *
     * @param name the name of the stream
     * @param isInput whether the stream is an input or output stream
     * @param count the number of streams of this type
     */
    public void logStreamCreate(String name, boolean isInput, int count) {
        if(isLogged()) {
            if(isInput) {
                log("Creating input stream " + name + " total streams: " + count);
            } else {
                log("Creating output stream " + name + " total streams: " + count);
            }
        }
    }


    /**
     * Logs the closing of a stream
     *
     * @param name the name of the stream
     * @param isInput whether the stream is an input or output stream
     * @param count the number of streams of this type
     */
    public void logStreamClose(String name, boolean isInput, int count) {
        if(isLogged()) {
            if(isInput) {
                log("Closing input stream " + name + " remaining streams: " + count);
            } else {
                log("Closing output stream " + name + " remaining streams: " + count);
            }
        }
    }

    /**
     * Logs the closing of a stream
     *
     * @param name the name of the stream
     * @param isInput whether the stream is an input or output stream
     */
    public void logStreamDoubleClose(String name, boolean isInput) {
        if(isLogged()) {
            if(isInput) {
                log("Double closing input stream " + name);
            } else {
                log("Double closing output stream " + name);
            }
        }
    }

    /**
     * Returns the type of the root often by guessing
     *
     * @param root the root whose type we are checking
     * @return one of the type constants above
     */
    public int getRootType(String root) {
        root = root.toLowerCase();
        String sdCard = Display.getInstance().getProperty("sdcard", null);
        if(sdCard != null) {
            if(root.indexOf(sdCard) > -1) {
                return FileSystemStorage.ROOT_TYPE_SDCARD;
            }
        } else {
            if(root.indexOf("file:///f:") > -1 || root.indexOf("file:///e:") > -1 || root.indexOf("memorycard") > -1 ||
                    root.indexOf("mmc") > -1 || root.indexOf("sdcard") > -1 ||
                    root.indexOf("store") > -1) {
                return FileSystemStorage.ROOT_TYPE_SDCARD;
            }
        }
        if(root.indexOf("c:") > -1 || root.indexOf("phone memory") > -1 || root.indexOf("store") > -1) {
            return FileSystemStorage.ROOT_TYPE_MAINSTORAGE;
        }
        return FileSystemStorage.ROOT_TYPE_UNKNOWN;
    }

    
    /**
     * This method returns the platform Location Control
     * @return LocationManager Object
     */
    public LocationManager getLocationManager() {
        return null;
    }

    /**
     * Allows buggy implementations (Android) to release image objects  
     * @param image native image object
     */
    public void releaseImage(Object image) {
    }
    
    /**
     * Captures a photo and notifies with the image data when available
     * @param response callback for the resulting image
     */
    public void capturePhoto(ActionListener response) {
    }

    /**
     * Captures a audio and notifies with the raw data when available
     * @param response callback for the resulting data
     */
    public void captureAudio(ActionListener response) {
    }

    /**
     * Captures a video and notifies with the data when available
     * @param response callback for the resulting video
     */
    public void captureVideo(ActionListener response) {
    }

    /**
     * Opens the device gallery
     * The method returns immediately and the response will be sent asynchronously
     * to the given ActionListener Object
     * 
     * use this in the actionPerformed to retrieve the file path
     * String path = (String) evt.getSource();
     * 
     * @param response a callback Object to retrieve the file path
     * @param type one of the following GALLERY_IMAGE, GALLERY_VIDEO, GALLERY_ALL
     * @throws RuntimeException if this feature failed or unsupported on the platform
     */
    public void openGallery(final ActionListener response, int type){
        final Dialog d = new Dialog("Select a picture");
        d.setLayout(new BorderLayout());
        FileTreeModel model = new FileTreeModel(true);
        if(type == Display.GALLERY_IMAGE){
            model.addExtensionFilter("jpg");
            model.addExtensionFilter("png");
        } else if(type == Display.GALLERY_VIDEO){
            model.addExtensionFilter("mp4");
            model.addExtensionFilter("3pg");
            model.addExtensionFilter("avi");
            model.addExtensionFilter("mov");
        } else if(type == Display.GALLERY_ALL){
            model.addExtensionFilter("jpg");
            model.addExtensionFilter("png");
            model.addExtensionFilter("mp4");
            model.addExtensionFilter("3pg");
            model.addExtensionFilter("avi");
            model.addExtensionFilter("mov");
        }
        
        FileTree t = new FileTree(model){

            protected Button createNodeComponent(final Object node, int depth) {
                if (node == null || !getModel().isLeaf(node)) {
                    return super.createNodeComponent(node, depth);
                }
                Hashtable t = (Hashtable) Storage.getInstance().readObject("thumbnails");
                if (t == null) {
                    t = new Hashtable();
                }
                final Hashtable thumbs = t;
                final Button b = super.createNodeComponent(node, depth);
                b.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent evt) {
                        response.actionPerformed(new ActionEvent(node,ActionEvent.Type.Other));
                        d.dispose();
                    }
                });
                final ImageIO imageio = ImageIO.getImageIO();
                if (imageio != null) {

                    Display.getInstance().scheduleBackgroundTask(new Runnable() {

                        public void run() {
                            byte[] data = (byte[]) thumbs.get(node);
                            if (data == null) {
                                ByteArrayOutputStream out = new ByteArrayOutputStream();
                                try {
                                    imageio.save(FileSystemStorage.getInstance().openInputStream((String) node),
                                            out,
                                            ImageIO.FORMAT_JPEG,
                                            b.getIcon().getWidth(), b.getIcon().getHeight(), 1);
                                    data = out.toByteArray();
                                    thumbs.put(node, data);
                                    Storage.getInstance().writeObject("thumbnails", thumbs);
                                } catch (IOException ex) {
                                    Log.e(ex);
                                }
                            }
                            Image im = Image.createImage(data, 0, data.length);
                            b.setIcon(im);
                        }
                    });

                }
                return b;
            }
        
        };
        
        d.addComponent(BorderLayout.CENTER, t);
        
        d.placeButtonCommands(new Command[]{new Command("Cancel")});
        Command c = d.showAtPosition(2, 2, 2, 2, true);
        if(c != null){
            response.actionPerformed(null);   
        }
        
    }
    /**
     * Opens the device image gallery
     * @param response callback for the resulting image
     */
    public void openImageGallery(final ActionListener response){    
        openGallery(response, Display.GALLERY_IMAGE);
    }

    /**
     * Returns a 2-3 letter code representing the platform name for the platform override
     * 
     * @return the name of the platform e.g. ios, rim, win, and, me
     */
    public abstract String getPlatformName();

    /**
     * Returns the suffixes for ovr files that should be used when loading a layered resource file on this platform
     * 
     * @return a string array with the proper order of resource override layers
     */
    public String[] getPlatformOverrides() {
        return new String[0];
    }
    
    /**
     * This callback allows highly broken devices like the blackberry to automatically detect the network
     * type
     */
    public boolean shouldAutoDetectAccessPoint() {
        return false;
    }

    /**
     * Send an email using the platform mail client
     * @param recipients array of e-mail addresses
     * @param subject e-mail subject
     * @param msg the Message to send
     */
    public void sendMessage(String[] recipients, String subject, Message msg) {
    }
    
    /**
     * Opens the device Dialer application with the given phone number
     * @param phoneNumber 
     */
    public void dial(String phoneNumber) {        
    }
    
    /**
     * Sends a SMS message to the given phone number
     * @param phoneNumber to send the sms
     * @param message the content of the sms
     * @throws IOException if for some reason sending failed
     */
    public void sendSMS(String phoneNumber, String message, boolean interactive) throws IOException{
    }

    /**
     * Indicates the level of SMS support in the platform as one of: SMS_NOT_SUPPORTED (for desktop, tablet etc.), 
     * SMS_SEAMLESS (no UI interaction), SMS_INTERACTIVE (with compose UI), SMS_BOTH.
     * @return one of the SMS_* values
     */
    public int getSMSSupport() {
        return Display.SMS_SEAMLESS;
    }
    
    /**
     * Returns an image representing the application icon, or null if not supported. This is used on
     * Android to support the title bar icon
     */
    public Image getApplicationIconImage() {
        InputStream i = getResourceAsStream(getClass(), "/icon.png");
        if(i != null) {
            try {
                return EncodedImage.create(i);
            } catch (IOException ex) {
                Log.e(ex);
            }
        }
        return null;
    }
    
    /**
     * This is a temporary workaround for an XMLVM Bug!
     */
    public static Class getStringArrayClass() {
        try {
            return String[].class;
        } catch(Throwable t) {
            return new String[0].getClass();
        }
    }

    /**
     * This is a temporary workaround for an XMLVM Bug!
     */
    public static Class getStringArray2DClass() {
        try {
            return String[][].class;
        } catch(Throwable t) {
            return new String[1][].getClass();
        }
    }

    /**
     * This is a temporary workaround for an XMLVM Bug!
     */
    public static Class getImageArrayClass() {
        try {
            return Image[].class;
        } catch(Throwable t) {
            return new Image[0].getClass();
        }
    }

    /**
     * This is a temporary workaround for an XMLVM Bug!
     */
    public static Class getObjectArrayClass() {
        try {
            return Object[].class;
        } catch(Throwable t) {
            return new Object[0].getClass();
        }
    }

    /**
     * Gets all contacts from the address book of the device
     * @param withNumbers if true returns only contacts that has a number
     * @return array of contacts unique ids
     */
    public String [] getAllContacts(boolean withNumbers) {
        return null;
    }

    /**
     * Gets all of the contacts that are linked to this contact.  Some platforms, like iOS, allow for multiple distinct contact records to be "linked" to indicate that they refer to the same person.
     * 
     * Implementations should override the {@link #getLinkedContactIds(com.codename1.contacts.Contact) } method.
     * @param c The contact whose "linked" contacts are to be retrieved.
     * @return Array of Contacts.  Should never be null, but may be a zero-sized array.
     * @see com.codename1.contacts.ContactsManager#getLinkedContacts(com.codename1.contacts.Contact) 
     * 
     */
    //public final Contact[] getLinkedContacts(Contact c) {
    //    String[] ids = getLinkedContactIds(c);
    //    if (ids != null) {
    //        Contact[] out = new Contact[ids.length];
    //        int len = ids.length;
    //        for (int i=0; i< len; i++) {
    //            out[i] = getContactById(ids[i]);
    //        }
    //        return out;
    //    }
    //    return new Contact[0];
    //}
    
    
    /**
     * Gets the IDs of all contacts that are linked to the provided contact.
     * @param c The contact
     * @return Array of IDs for contacts that are linked to {@code c}.
     */
    public String[] getLinkedContactIds(Contact c) {
        if (c == null || c.getId() == null) {
            return new String[0];
        }
        return new String[]{c.getId()};
    }
    
    /**
     * Get a Contact according to it's contact id.
     * @param id unique id of the Contact
     * @return a Contact Object
     */
    public Contact getContactById(String id) {
        return null;
    }

    /**
     * Notice: this method might be very slow and should be invoked on a separate thread!
     * It might have platform specific optimizations over getAllContacts followed by looping
     * over individual contacts but that isn't guaranteed. See isGetAllContactsFast for
     * information.
     * 
     * @param withNumbers if true returns only contacts that has a number
     * @param includesFullName if true try to fetch the full name of the Contact(not just display name)
     * @param includesPicture if true try to fetch the Contact Picture if exists
     * @param includesNumbers if true try to fetch all Contact numbers
     * @param includesEmail if true try to fetch all Contact Emails
     * @param includeAddress if true try to fetch all Contact Addresses
     * @return array of the contacts
     */
    public Contact[] getAllContacts(boolean withNumbers, boolean includesFullName, boolean includesPicture, boolean includesNumbers, boolean includesEmail, boolean includeAddress) {
        String[] arr = getAllContacts(withNumbers);
        if(arr == null) {
            return null;
        }
        Contact[] retVal = new Contact[arr.length];
        int alen = arr.length;
        for(int iter = 0 ; iter  < alen ; iter++) {
            retVal[iter] = getContactById(arr[iter], includesFullName, includesPicture, includesNumbers, includesEmail, includeAddress);
        }
        return retVal;
    }

    /**
     * Indicates if the getAllContacts is platform optimized, notice that the method
     * might still take seconds or more to run so you should still use a separate thread!
     * @return true if getAllContacts will perform faster that just getting each contact
     */
    public boolean isGetAllContactsFast() {
        return false;
    }
    
    /**
     * This method returns a Contact by the contact id and fills it's data
     * according to the given flags
     * 
     * @param id of the Contact
     * @param includesFullName if true try to fetch the full name of the Contact(not just display name)
     * @param includesPicture if true try to fetch the Contact Picture if exists
     * @param includesNumbers if true try to fetch all Contact numbers
     * @param includesEmail if ture try to fetch all Contact Emails
     * @param includeAddress if ture try to fetch all Contact Addresses
     *  
     * @return a Contact Object
     */ 
    public Contact getContactById(String id, boolean includesFullName, boolean includesPicture, 
            boolean includesNumbers, boolean includesEmail, boolean includeAddress){
        return null;
    }
    
    /**
     * Create a contact to the device contacts book
     * 
     * @param firstName the Contact firstName
     * @param surname the Contact familyName
     * @param officePhone the Contact work phone or null
     * @param homePhone the Contact home phone or null
     * @param cellPhone the Contact mobile phone or null
     * @param email the Contact email or null
     * 
     * @return the contact id if creation succeeded or null  if failed
     */ 
    public String createContact(String firstName, String surname, String officePhone, String homePhone, String cellPhone, String email) {
         return null;
    }

    /**
     * Some platforms allow the user to block contacts access on a per application basis (specifically iOS).
     * 
     * @return true if contacts access is allowed or globally available, false otherwise
     */
    public boolean isContactsPermissionGranted() {
        return true;
    }
    
    /**
     * removed a contact from the device contacts book
     * @param id the contact id to remove
     * @return true if deletion succeeded false otherwise
     */ 
    public boolean deleteContact(String id) {
        return false;
    }
    
    /**
     * Indicates if the underlying platform supports sharing capabilities
     * @return true if the underlying platform handles share.
     */
    public boolean isNativeShareSupported(){
        return false;
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
     */
    public void share(String text, String image, String mimeType){
        share(text, image, mimeType, null);
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
     * @param sourceRect The bounds of the button that was clicked to initiate 
     * the share.  This is used by some platforms (e.g. iPad2 on iOS 8 or 
     * higher) to dictate where the popover dialog should be placed.
     */
    public void share(String text, String image, String mimeType, Rectangle sourceRect){
        
    }

    /**
     * Called before internal paint of component starts
     * 
     * @param c the component about to be painted
     */
    public void beforeComponentPaint(Component c, Graphics g) {
    }

    /**
     * Called after internal paint of component finishes
     * 
     * @param c the component that was painted
     */
    public void afterComponentPaint(Component c, Graphics g) {
    }
    
    /**
     * Indicates to the port that the component won't be painted due to clipping
     * 
     * @param c the component that won't be painted
     */
    public void nothingWithinComponentPaint(Component c) {
    }
    
    /**
     * Indicates to the port that the component was removed from the view and its
     * UI should be removed in the next flush operation. 
     * 
     * @param c the removed component.
     */
    public void componentRemoved(Component c) {
    }
    
    /**
     * {@inheritDoc}
     */
    public abstract L10NManager getLocalizationManager();
    
    /**
     * Returns the package name for the application
     */
    protected String getPackageName() {
        if(packageName == null) {
            return Display.getInstance().getProperty("package_name", null);
        }
        return packageName;
    }

    // BEGIN TRANSFORMATION METHODS---------------------------------------------------------
    
    /**
     * Checks if the Transform class can be used on this platform.  This is similar to
     * {@link #isTransformSupported(java.lang.Object)} but it is more general as it only verifies 
     * that transforms can be performed, but not necessarily that they will be respected
     * by any particular graphics context.
     * @return True if this platform supports transforms.
     * @see #isTransformSupported(java.lang.Object) 
     */
    public boolean isTransformSupported(){
        return false;
    }
    
    /**
     * Checks of the Transform class can be used on this platform to perform perspective transforms. 
     *  This is similar to
     * {@link #isPerspectiveTransformSupported(java.lang.Object)} but it is more general as it only verifies 
     * that transforms can be performed, but not necessarily that they will be respected
     * by any particular graphics context.
     * @return True if this platform supports perspective transforms.
     */
    public boolean isPerspectiveTransformSupported(){
        return false;
    }
    
    public boolean transformEqualsImpl(Transform t1, Transform t2){
        Object o1 = null;
        if(t1 != null) {
            o1 = t1.getNativeTransform();
        }
        Object o2 = null;
        if(t2 != null) {
            o2 = t2.getNativeTransform();
        }
        return transformNativeEqualsImpl(o1, o2);
    }
    
    public boolean transformNativeEqualsImpl(Object t1, Object t2){
        throw new RuntimeException("Transforms not supported");
    }

    /**
     * Makes a new native translation transform.  Each implementation can decide the format
     * to use internally for transforms.  This should return a transform in that internal format.
     * This is used by the {@link com.codename1.ui.Transform} class.
     * <p>This can only be used if {@link #isTransformSupported()} returns true.</p>
     * @param translateX The x-coordinate of the translation.
     * @param translateY The y-coordinate of the translation.
     * @param translateZ The z-coordinate of the translation.
     * @return A native transform object encapsulating the specified translation.
     * @see #isTransformSupported()
     */
    public Object makeTransformTranslation(float translateX, float translateY, float translateZ) {
        throw new RuntimeException("Transforms not supported");
    }
    
    public void setTransformTranslation(Object nativeTransform, float translateX, float translateY, float translateZ) {
        setTransformIdentity(nativeTransform);
        transformTranslate(nativeTransform, translateX, translateY, translateZ);
    }

    /**
     * Makes a new native scale transform.  Each implementation can decide the format
     * to use internally for transforms.  This should return a transform in that internal format.
     * This is used by the {@link com.codename1.ui.Transform} class.
     * <p>This can only be used if {@link #isTransformSupported()} returns true.</p>
     * @param scaleX The x-scale factor of the transform.
     * @param scaleY The y-scale factor of the transform.
     * @param scaleZ The z-scale factor of the transform.
     * @return A native transform object encapsulating the specified scale.
     * @see #isTransformSupported()
     */
    public Object makeTransformScale(float scaleX, float scaleY, float scaleZ) {
        throw new RuntimeException("Transforms not supported");
    }
    
    public void setTransformScale(Object nativeTransform, float scaleX, float scaleY, float scaleZ) {
        setTransformIdentity(nativeTransform);
        transformScale(nativeTransform, scaleX, scaleY, scaleZ);
    }

    /**
     * Makes a new native rotation transform.  Each implementation can decide the format
     * to use internally for transforms.  This should return a transform in that internal format.
     * This is used by the {@link com.codename1.ui.Transform} class.
     * <p>This can only be used if {@link #isTransformSupported()} returns true.</p>
     * @param angle The angle to rotate.
     * @param x The x-component of the vector around which to rotate.
     * @param y The y-component of the vector around which to rotate.
     * @param z The z-component of the vector around which to rotate.
     * @return A native transform object encapsulating the specified rotation.
     * @see #isTransformSupported()
     */
    public Object makeTransformRotation(float angle, float x, float y, float z) {
        throw new RuntimeException("Transforms not supported");
    }
    
    public void setTransformRotation(Object nativeTransform, float angle, float x, float y, float z) {
        setTransformIdentity(nativeTransform);
        transformRotate(nativeTransform, angle, x, y, z);
    }

    /**
     * Makes a new perspective transform. Each implementation can decide the format
     * to use internally for transforms.  This should return a transform in that internal format.
     * This is used by the {@link com.codename1.ui.Transform} class.
     * <p>This can only be used if {@link #isPerspectiveTransformSupported()} returns true.</p>
     * @param fovy The y field of view angle.
     * @param aspect The aspect ratio.
     * @param zNear The nearest visible z coordinate.
     * @param zFar The farthest z coordinate.
     * @return A native transform object encapsulating the given perspective.
     * @see #isPerspectiveTransformSupported()
     */
    public Object makeTransformPerspective(float fovy, float aspect, float zNear, float zFar) {
        throw new RuntimeException("Transforms not supported");
    }

    public void setTransformPerspective(Object nativeTransform, float fovy, float aspect, float zNear, float zFar) {
        Object persp = makeTransformPerspective(fovy, aspect, zNear, zFar);
        copyTransform(persp, nativeTransform);
    }
    
    /**
     * Makes a new orthographic projection transform.  Each implementation can decide the format
     * to use internally for transforms.  This should return a transform in that internal format.
     * This is used by the {@link com.codename1.ui.Transform} class.
     * <p>This can only be used if {@link #isPerspectiveTransformSupported()} returns true.</p>
     * @param left x-coordinate that is the left edge of the view.
     * @param right The x-coordinate that is the right edge of the view.
     * @param bottom The y-coordinate that is the bottom edge of the view.
     * @param top The y-coordinate that is the top edge of the view.
     * @param near The nearest visible z-coordinate.
     * @param far The farthest visible z-coordinate.
     * @return A native transform with the provided orthographic projection.
     * @see #isPerspectiveTransformSupported()
     */
    public Object makeTransformOrtho(float left, float right, float bottom, float top, float near, float far) {
        throw new RuntimeException("Transforms not supported");
    }

    public void setTransformOrtho(Object nativeTransform, float left, float right, float bottom, float top, float near, float far) {
        Object ortho = makeTransformOrtho(left, right, bottom, top, near, far);
        copyTransform(ortho, nativeTransform);
    }
    
    /**
     * Makes a transform to simulate a camera's perspective at a given location. Each implementation can decide the format
     * to use internally for transforms.  This should return a transform in that internal format.
     * This is used by the {@link com.codename1.ui.Transform} class.
     * <p>This can only be used if {@link #isTransformSupported()} returns true.</p>
     * @param eyeX The x-coordinate of the camera's eye.
     * @param eyeY The y-coordinate of the camera's eye.
     * @param eyeZ The z-coordinate of the camera's eye.
     * @param centerX The center x coordinate of the view.
     * @param centerY The center y coordinate of the view.
     * @param centerZ The center z coordinate of the view.
     * @param upX The x-coordinate of the up vector for the camera.
     * @param upY The y-coordinate of the up vector for the camera.
     * @param upZ The z-coordinate of the up vector for the camera.
     * @return A native transform with the provided camera's view perspective.
     * @see #isPerspectiveTransformSupported()
     */
    public Object makeTransformCamera(float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ) {
        throw new RuntimeException("Transforms not supported");
    }

    public void setTransformCamera(Object nativeTransform, float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ) {
        Object cam = makeTransformCamera(eyeX, eyeY, eyeZ,  centerX, centerY, centerZ, upX, upY, upZ);
        copyTransform(cam, nativeTransform);
    }
    
    /**
     * Rotates the provided  transform.
     * @param nativeTransform The transform to rotate. Each implementation can decide the format
     * to use internally for transforms.  This should return a transform in that internal format.
     * This is used by the {@link com.codename1.ui.Transform} class.
     * <p>This can only be used if {@link #isTransformSupported()} returns true.</p>
     * @param angle The angle to rotate.
     * @param x The x-coordinate of the vector around which to rotate.
     * @param y The y-coordinate of the vector around which to rotate.
     * @param z  The z-coordinate of the vector around which to rotate.
     * @see #isTransformSupported()
     */
    public void transformRotate(Object nativeTransform, float angle, float x, float y, float z) {
       Object rot = makeTransformRotation(angle, x, y, z);
       concatenateTransform(nativeTransform, rot);
    }

    
    /**
     * Translates the transform by the specified amounts.  
     * with the specified translation.
     * @param nativeTransform The native transform to translate. Each implementation can decide the format
     * to use internally for transforms.  This should return a transform in that internal format.
     * This is used by the {@link com.codename1.ui.Transform} class.
     * <p>This can only be used if {@link #isTransformSupported()} returns true.</p>
     * @param x The x translation.
     * @param y The y translation.
     * @param z The z translation.
     * @see #isTransformSupported()
     */
    public void transformTranslate(Object nativeTransform, float x, float y, float z) {
        Object tr = makeTransformTranslation(x, y, z);
        concatenateTransform(nativeTransform, tr);
    }

    /**
     * Scales the provided transform by the provide scale factors. 
     * @param nativeTransform Each implementation can decide the format
     * to use internally for transforms.  This should return a transform in that internal format.
     * This is used by the {@link com.codename1.ui.Transform} class.
     * <p>This can only be used if {@link #isTransformSupported()} returns true.</p>
     * @param x The x-scale factor
     * @param y The y-scale factor
     * @param z The z-scale factor
     * @see #isTransformSupported()
     */
    public void transformScale(Object nativeTransform, float x, float y, float z) {
        Object scale = makeTransformScale(x, y, z);
        concatenateTransform(nativeTransform, scale);
    }

    /**
     * Gets the inverse transformation for the provided transform.
     * @param nativeTransform The native transform of which to make the inverse.  Each implementation can decide the format
     * to use internally for transforms.  This should return a transform in that internal format.
     * This is used by the {@link com.codename1.ui.Transform} class.
     * <p>This can only be used if {@link #isTransformSupported()} returns true.</p>
     * @return The inverse transform as a native transform object.  Each implementation can decide the format
     * to use internally for transforms.  This should return a transform in that internal format.
     * This is used by the {@link com.codename1.ui.Transform} class.
     * @see #isTransformSupported()
     */
    public Object makeTransformInverse(Object nativeTransform) {
       throw new RuntimeException("Transforms not supported");
    }
    
    public void setTransformInverse(Object nativeTransform) throws Transform.NotInvertibleException {
       copyTransform(makeTransformInverse(nativeTransform), nativeTransform);
    }
    
    /**
     * Makes a new identity native transform. Each implementation can decide the format
     * to use internally for transforms.  This should return a transform in that internal format.
     * This is used by the {@link com.codename1.ui.Transform} class.
     * <p>This can only be used if {@link #isTransformSupported()} returns true.</p>
     * @return An identity native transform.
     * @see #isTransformSupported()
     */
    public Object makeTransformIdentity(){
        throw new RuntimeException("Transforms not supported");
    }

    /**
     * Sets the given native transform to the identiy transform
     * @param transform 
     */
    public void setTransformIdentity(Object transform) {
        copyTransform(makeTransformIdentity(), transform);
    }
    
    /**
     * Copies the setting of one transform into another.  Each implementation can decide the format
     * to use internally for transforms.  This should return a transform in that internal format.
     * This is used by the {@link com.codename1.ui.Transform} class.
     * <p>This can only be used if {@link #isTransformSupported()} returns true.</p>
     * @param src The source native transform.
     * @param dest The destination native transform.
     * @see #isTransformSupported()
     */
    public void copyTransform(Object src, Object dest) {
       throw new RuntimeException("Transforms not supported");
    }

    /**
     * Concatenates two transforms and sets the first transform to be the result of the concatenation.
     * <p>This can only be used if {@link #isTransformSupported()} returns true.</p>
     * @param t1 The left native transform.  The result will also be stored in this transform.
     * @param t2 The right native transform.
     * @see #isTransformSupported()
     */
    public void concatenateTransform(Object t1, Object t2) {
        throw new RuntimeException("Transforms not supported");
    }

    
    /**
     * Transforms a point and stores the result in a provided array.
     * @param nativeTransform The native transform to use for the transformation. Each implementation can decide the format
     * to use internally for transforms.  This should return a transform in that internal format.
     * <p>This can only be used if {@link #isTransformSupported()} returns true.</p>
     * This is used by the {@link com.codename1.ui.Transform} class.
     * @param in A 2 or 3 element array representing either an (x,y) or (x,y,z) tuple to be transformed.
     * @param out A 2 or 3 element array (length should match {@var in}) to store the result of the transformation.
     * @see #isTransformSupported()
     */
    public void transformPoint(Object nativeTransform, float[] in, float[] out) {
        throw new RuntimeException("Transforms not supported");
    }
    
    /**
     * Transforms a set of points using the provided transform.
     * @param nativeTransform The transform to use for transforming the points
     * @param pointSize The size of the points (either 2 or 3)
     * @param in Input array of points.
     * @param srcPos The start position of the input array
     * @param out The output array of points
     * @param destPos The start position of the output array.
     * @param numPoints The number of points to transform.
     */
    public void transformPoints(Object nativeTransform, int pointSize, float[] in, int srcPos, float[] out, int destPos, int numPoints) {
        float[] bufIn = new float[pointSize];
        float[] bufOut = new float[pointSize];
        int len = numPoints * pointSize;
        for (int i=0; i<len; i+= pointSize) {
            System.arraycopy(in, srcPos + i, bufIn, 0, pointSize);
            transformPoint(nativeTransform, bufIn, bufOut);
            System.arraycopy(bufOut, 0, out, destPos + i, pointSize);
        }
    }
    
    /**
     * Translates a set of points.
     * @param pointSize The size of each point (2 or 3)
     * @param tX Size of translation along x-axis
     * @param tY Size of translation along y-axis
     * @param tZ Size of translation along z-axis (only used if pointSize == 3)
     * @param in Input array of points.
     * @param srcPos Start position in input array
     * @param out Output array of points
     * @param destPos Start position in output array
     * @param numPoints Number of points to translate.
     */
    public void translatePoints(int pointSize, float tX, float tY, float tZ, float[] in, int srcPos, float[] out, int destPos, int numPoints) {
        int len = numPoints * pointSize;
        for (int i=0; i<len; i+=pointSize) {
            int d0 = destPos + i;
            int s0 = srcPos + i;
            out[d0++] = in[s0++] + tX;
            out[d0++] = in[s0++] + tY;
            if (pointSize > 2) {
                out[d0] = in[s0] + tZ;
            }
        }
    }
    
    /**
     * Scales a set of points.
     * @param pointSize The size of each point (2 or 3)
     * @param sX Scale factor along x-axis
     * @param sY Scale factor along y-axis
     * @param sZ Scale factor along z-axis (only used if pointSize == 3)
     * @param in Input array of points.
     * @param srcPos Start position in input array
     * @param out Output array of points
     * @param destPos Start position in output array
     * @param numPoints Number of points to translate.
     */
    public void scalePoints(int pointSize, float sX, float sY, float sZ, float[] in, int srcPos, float[] out, int destPos, int numPoints) {
        int len = numPoints * pointSize;
        for (int i=0; i<len; i+=pointSize) {
            int d0 = destPos + i;
            int s0 = srcPos + i;
            out[d0++] = in[s0++] * sX;
            out[d0++] = in[s0++] * sY;
            if (pointSize > 2) {
                out[d0] = in[s0] * sZ;
            }
        }
    }
    

    /**
     * Clears the addressbook cache.  This is only necessary on iOS since its AddressBookRef is transactional.
     */
    public void refreshContacts() {
        
    }

    /**
     * Sets the given transform to the current transform in the given graphics object.
     * @param nativeGraphics
     * @param t 
     */
    public void getTransform(Object nativeGraphics, Transform t) {
        t.setIdentity();
    }

    public boolean isScrollWheeling() {
        return false;
    }

    /**
     * Blocks or enables copy and paste in the entire app.
     * @param blockCopyPaste True to block copy and paste.  False to enable it.
     */
    public void blockCopyPaste(boolean blockCopyPaste) {
        
    }

    // END TRANSFORMATION METHODS--------------------------------------------------------------------    
    
    class RPush implements Runnable {
        public void run() {
            final long pushId = Preferences.get("push_id", (long) -1);
            if (pushId > -1 && callback != null) {
                callback.registeredForPush("" + pushId);
            }
        }
    }
    
    /**
     * User register to receive push notification
     * 
     * @param noFallback some devices don't support an efficient push API and will resort to polling 
     * to provide push like functionality. If this flag is set to true no polling will occur and 
     * the error PushCallback.REGISTRATION_ERROR_SERVICE_NOT_AVAILABLE will be sent to the push interface.
     */
    public void registerPush(Hashtable metaData, boolean noFallback) {
        if(!noFallback) {
            Preferences.set("PollingPush", true);
            registerPushOnServer(getPackageName(), getApplicationKey(), (byte)10, "", getPackageName());

            // Call pushCallback's registeredForPush
            Display.getInstance().callSerially(new RPush());
            registerPollingFallback();
        }
    }

    /**
     * Stop receiving push notifications to this client application
     */
    public void deregisterPush() {
        Preferences.delete("PollingPush");
        stopPolling();
    }

    /**
     * Creates a Media recorder Object which will record from the device mic to
     * a file in the given path.
     * 
     * @param path a file path to where to store the recording, if the file does
     * not exists it will be created.
     * @param mimeType the output mime type that is supported see 
     * getAvailableRecordingMimeTypes()
     */
    public Media createMediaRecorder(String path, String mimeType) throws IOException{
        return null;
    }
    
    /**
     * Stops the polling push loop
     */
    protected static void stopPolling() {
        pollingThreadRunning = false;
    }
    
    /**
     * Returns the key for the application comprised of the builders email coupled with the 
     * package name. It should uniquely identify the application across different builds 
     * which allows interaction with the cloud.
     * 
     * @return a unique string with the format builders_email/packagename
     */
    protected static String getApplicationKey() {
        Display d = Display.getInstance();
        return d.getProperty("built_by_user", "Unknown Build Key") + '/' +
                d.getProperty("package_name", "Unknown Build Key");
    }
    
    /**
     * Sends a server request to register push support. This is a method for use
     * by implementations.
     * 
     * @param id the platform specific push ID
     * @param applicationKey the unique id of the application
     * @param pushType for server side type
     * @param packageName the application package name used by the push service
     * @return true for success, false otherwise
     */
    public static boolean registerServerPush(String id, String applicationKey, byte pushType, String udid,
            String packageName) {
        //Log.p("registerPushOnServer invoked for id: " + id + " app key: " + applicationKey + " push type: " + pushType);
        Preferences.set("push_key", id);
        /*if(Preferences.get("push_id", (long)-1) == -1) {
            Preferences.set("push_key", id);
            ConnectionRequest r = new ConnectionRequest() {
                protected void readResponse(InputStream input) throws IOException  {
                    DataInputStream d = new DataInputStream(input);
                    long pid = d.readLong();
                    Preferences.set("push_id", pid);
                    Log.p("registerPushOnServer push id received from server: " + pid);
                }
            };
            r.setPost(false);
            r.setFailSilently(true);
            r.setReadResponseForErrors(false);
            r.setUrl(Display.getInstance().getProperty("cloudServerURL", "https://codename-one.appspot.com/") + "registerPush");
            long val = Preferences.get("push_id", (long)-1);
            if(val > -1) {
                r.addArgument("i", "" + val);
            }
            r.addArgument("p", id);
            r.addArgument("k", applicationKey);
            r.addArgument("os", Display.getInstance().getPlatformName());
            r.addArgument("t", "" + pushType);
            r.addArgument("ud", udid);
            r.addArgument("r", packageName);
            NetworkManager.getInstance().addToQueueAndWait(r);
            return r.getResponseCode() == 200;
        }*/
        return true;
    }
    
    /**
     * Sends a server request to register push support. This is a method for use
     * by implementations.
     * 
     * @param id the platform specific push ID
     * @param applicationKey the unique id of the application
     * @param pushType for server side type
     * @param packageName the application package name used by the push service
     */
    public static void registerPushOnServer(String id, String applicationKey, byte pushType, String udid,
            String packageName) {
        registerServerPush(id, applicationKey, pushType, udid, packageName);
    }
    
    protected final void sendRegisteredForPush(String id) {
        if (callback != null) {
            callback.registeredForPush(id);
        }
    }
    
    
    protected final void pushReceived(String data) {
        if (callback != null) {
            callback.push(data);
        }
    }
    
    /**
     * For use by implementations, stop receiving push notifications from the server
     */
    public static void deregisterPushFromServer() {
        /*long i = Preferences.get("push_id", (long)-1);
        if(i > -1) {
            ConnectionRequest r = new ConnectionRequest();
            r.setPost(false);
            r.setUrl(Display.getInstance().getProperty("cloudServerURL", "https://codename-one.appspot.com/") + "deregisterPush");
            r.addArgument("p", "" + i);
            r.addArgument("a", getApplicationKey());
            NetworkManager.getInstance().addToQueue(r);
            Preferences.delete("push_id");
            Preferences.delete("push_key");
        }*/
    }
    
    /**
     * Sets the frequency for polling the server in case of polling based push notification
     * 
     * @param freq the frequency in milliseconds
     */
    public void setPollingFrequency(int freq) {
        pollingMillis = freq;
        if(callback != null && pollingThreadRunning) {
            synchronized(callback) {
                callback.notify();
            }
        }
    }
    
    /**
     * Registers a polling thread to simulate push notification
     */
    protected static void registerPollingFallback() {
        if(pollingThreadRunning || callback == null) {
            return;
        }
        pollingThreadRunning = true;
        final long pushId = Preferences.get("push_id", (long)-1);
        if(pushId > -1) {
            new CodenameOneThread(new Runnable() {
                public void run() {
                    String lastReq = Preferences.get("last_push_req", "0");
                    while(pollingThreadRunning) {
                        try {
                            ConnectionRequest cr = new ConnectionRequest();
                            cr.setUrl(Display.getInstance().getProperty("cloudServerURL", "https://codename-one.appspot.com/") + "pollManualPush");
                            cr.setPost(false);
                            cr.setFailSilently(true);
                            cr.addArgument("i", "" + pushId);
                            cr.addArgument("last", lastReq);
                            NetworkManager.getInstance().addToQueueAndWait(cr);
                            if(cr.getResponseCode() != 200) {
                                callback.pushRegistrationError("Server registration error", 1);
                            } else {
                                DataInputStream di = new DataInputStream(new ByteArrayInputStream(cr.getResponseData()));
                                if(di.readBoolean()) {
                                    byte type = di.readByte();
                                    String message = di.readUTF();
                                    lastReq = "" + di.readLong();
                                    Preferences.set("last_push_req", lastReq);
                                    callback.push(message);
                                }
                            }
                        } catch (IOException ex) {
                            Log.e(ex);
                        }
                        try {
                            synchronized(callback) {
                                callback.wait(pollingMillis);
                            }
                        } catch(Throwable t) {
                            Log.e(t);
                        }
                    }
                }
            }, "Polling Thread").start();
        }
    }

    /**
     * Returns the image IO instance that allows scaling image files.
     * @return the image IO instance
     */
    public ImageIO getImageIO() {
        return null;
    }

    /**
     * Workaround for XMLVM bug
     */
    public boolean instanceofObjArray(Object o) {
        return o instanceof Object[];
    }
    
    /**
     * Workaround for XMLVM bug
     */
    public boolean instanceofByteArray(Object o) {
        return o instanceof byte[];
    }
    
    /**
     * Workaround for XMLVM bug
     */
    public boolean instanceofShortArray(Object o) {
        return o instanceof short[];
    }
    
    /**
     * Workaround for XMLVM bug
     */
    public boolean instanceofLongArray(Object o) {
        return o instanceof long[];
    }
    
    /**
     * Workaround for XMLVM bug
     */
    public boolean instanceofIntArray(Object o) {
        return o instanceof int[];
    }
    
    /**
     * Workaround for XMLVM bug
     */
    public boolean instanceofFloatArray(Object o) {
        return o instanceof float[];
    }
    
    /**
     * Workaround for XMLVM bug
     */
    public boolean instanceofDoubleArray(Object o) {
        return o instanceof double[];
    }

    /**
     * Gets the available recording MimeTypes
     */ 
    public String [] getAvailableRecordingMimeTypes(){
        return new String[]{"audio/amr", "audio/aac"};
    }
    
    /**
     * Opens a database or create one if not exists
     * 
     * @param databaseName the name of the database
     * @return Database Object or null if not supported on the platform
     * 
     * @throws IOException if database cannot be created
     */
    public Database openOrCreateDB(String databaseName) throws IOException{
        return null;
    }
    
    /**
     * Deletes database
     * 
     * @param databaseName the name of the database
     * @throws IOException if database cannot be deleted
     */
    public void deleteDB(String databaseName) throws IOException{
    }
    
    /**
     * Indicates weather a database exists
     * 
     * @param databaseName the name of the database
     * @return true if database exists
     */
    public boolean existsDB(String databaseName){
        return false;
    }
    
    /**
     * Returns the file path of the Database if exists and if supported on 
     * the platform.
     * @return the file path of the database or null if not exists
     */
    public String getDatabasePath(String databaseName) {
        return null;
    }
    
    /**
     * Indicates if the title of the Form is native title(in android ICS devices
     * if the command behavior is native the ActionBar is used to display the title
     * and the menu)
     * @return true if platform would like to show the Form title
     */
    public boolean isNativeTitle() {
        return false;
    }
    
    /**
     * if the title is native(e.g the android action bar), notify the native title
     * that is needs to be refreshed
     */
    public void refreshNativeTitle(){
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
        if(!isTouchDevice()) {
            if(commandBehavior == Display.COMMAND_BEHAVIOR_BUTTON_BAR) {
                commandBehavior = Display.COMMAND_BEHAVIOR_SOFTKEY;
            }
        }
        this.commandBehavior = commandBehavior;
        notifyCommandBehavior(commandBehavior);
    }

    /**
     * Place a notification on the device status bar (if device has this 
     * functionality).
     * The notification will re-start the Application.
     * 
     * @param tickerText the ticker text of the Notification
     * @param contentTitle the title of the Notification
     * @param contentBody the content of the Notification
     * @param vibrate enable/disable notification alert
     * @param flashLights enable/disable notification flashing
     * @param args additional arguments to the notification
     * @return a platform native object that allows modifying notification state
     * @deprecated use scheduleLocalNotification instead
     */
    public Object notifyStatusBar(String tickerText, String contentTitle,
            String contentBody, boolean vibrate, boolean flashLights, Hashtable args) {
        return null;
    }
    
    /**
     * Indicates whether the notify status bar method will present a notification to the user
     * @return true if the notify status bar method will present a notification to the user
     */
    public boolean isNotificationSupported() {
        return false;
    }

    /**
     * Removes the notification previously posted with the notify status bar method
     * @param o the object returned from the notifyStatusBar method
     */
    public void dismissNotification(Object o) {
    }
    
    
    /**
     * Returns true if the underlying OS supports numeric badges on icons. Notice this is only available on iOS
     * and only when push notification is enabled
     * @return true if the underlying OS supports numeric badges
     */
    public boolean isBadgingSupported() {
        return false;
    }

    /**
     * Sets the number that appears on the application icon in iOS 
     * @param number number to show on the icon
     */
    public void setBadgeNumber(int number) {
    }    
    

    /**
     * Returns true if the underlying OS supports opening the native navigation
     * application
     * @return true if the underlying OS supports launch of native navigation app
     */
    public boolean isOpenNativeNavigationAppSupported(){
        return false;
    }
    
    /**
     * Opens the native navigation app in the given coordinate.
     * @param latitude 
     * @param longitude 
     */ 
    public void openNativeNavigationApp(double latitude, double longitude){    
    }

    /**
     * Opens the native navigation app with the given search location
     * @param location the location to search for in the native navigation map
     */ 
    public void openNativeNavigationApp(String location) {    
        execute("http://maps.google.com/?q=" + Util.encodeUrl(location));
    }
    
    /**
     * Returns the UDID for devices that support it
     * 
     * @return the UDID or null
     */
    public String getUdid() {
        return getProperty("UDID", null);
    }
    
    /**
     * Returns the MSISDN for devices that expose it
     * @return the msisdn or null
     */
    public String getMsisdn() {
        return getProperty("MSISDN", null);
    }    

    /**
     * Returns the native OS purchase implementation if applicable, if not this
     * method will fallback to a cross platform purchase manager. 
     * 
     * @return instance of the purchase class
     */
    public Purchase getInAppPurchase() {
        return null;
    }
    
    /**
     * Returns the native implementation of the code scanner or null
     * @return code scanner instance
     * @deprecated Use cn1-codescan cn1lib instead.
     */
    public CodeScanner getCodeScanner() {
        return null;
    }

    /**
     * This will return the application home directory.
     * 
     * @return a writable directory that represent the application home directory
     */
    public String getAppHomePath() {
        String home = listFilesystemRoots()[0];
        String name = getProperty("AppName", packageName);
        if(!home.endsWith("" + getFileSystemSeparator())) {
            home += getFileSystemSeparator();
        }
        home = home + name + getFileSystemSeparator();
        if(!exists(home)){
            mkdir(home);
        }
        return home;
    }

     /**
      * Returns true if the device has a directory dedicated for "cache" files
      * @return true if a caches style directory exists in this device type
      */
     public boolean hasCachesDir() {
         return false;
     }

     /**
      * Returns a device specific directory designed for cache style files, or null if {@link #hasCachesDir()}
      * is false
      * @return file URL or null
      */
     public String getCachesDir() {
         return null;
     }
    
    /**
     * Uses the native cookie store if applicable, this might break simulator compatibility
     * @return the useNativeCookieStore
     */
    public boolean isUseNativeCookieStore() {
        return useNativeCookieStore;
    }

    /**
     * Uses the native cookie store if applicable, this might break simulator compatibility
     * @param useNativeCookieStore the useNativeCookieStore to set
     */
    public void setUseNativeCookieStore(boolean useNativeCookieStore) {
        this.useNativeCookieStore = useNativeCookieStore;
    }

    /**
     * Indicates the implementation is capable of keeping the background painted by being non-destructive.
     * 
     * @return whether to paint the background
     */
    public boolean shouldPaintBackground() {
        return true;
    }
    
    /**
     * This method allows a native implementation to implement a native version of a given transition that
     * can be faster
     * 
     * @param t the transition that is about to execute
     * @return the given transition or a native version of that transition
     */
    public Transition getNativeTransition(Transition t) {
       return t; 
    }

    /**
     * Checks if the device supports locking the screen display from dimming, allowing 
     * the developer to keep the screen display on.
     */ 
    public boolean isScreenLockSupported() {
        return false;
    }
    
    /**
     * If Locking isScreenLockSupported() returns true calling this method will 
     * lock the screen display on
     */
    public void lockScreen(){
    }
    
    /**
     * Unlock the screen display allowing the screen to dim.
     */ 
    public void unlockScreen(){
    }

    /**
     * Returns true if the device has camera false otherwise.
     */ 
    public boolean hasCamera() {
        return true;
    }
    
    /**
     * Returns the platform EDT thread priority
     */
    public int getEDTThreadPriority(){
        return Thread.NORM_PRIORITY + 1;
    }

    /**
     * This method is used by the JavaSE implementation for performance logging
     * @param img the image being drawn
     */
    public void drawingEncodedImage(EncodedImage img) {}

    /**
     * Indicates whether the native picker dialog is supported for the given type 
     * which can include one of PICKER_TYPE_DATE_AND_TIME, PICKER_TYPE_TIME, PICKER_TYPE_DATE
     * @param pickerType the picker type constant
     * @return true if the native platform supports this picker type
     */
    public boolean isNativePickerTypeSupported(int pickerType) {
        return false;
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
    public Object showNativePicker(int type, Component source, Object currentValue, Object data) {
        return null;
    }
    
    /**
     * Creates a socket to connect to the given host on the given port
     * @param host the host
     * @param port the port
     * @return the socket object to use
     */
    public Object connectSocket(String host, int port) {
        throw new RuntimeException("Not supported");
    }
    
    /**
     * Listens on the given port similar to the accept method of server socket in Java. This method
     * will only work if isServerSocketAvailable() is true.
     * @param port the port to listen on
     * @return server socket instance
     */
    public Object listenSocket(int port) {
        throw new RuntimeException("Not supported");
    }
    
    /**
     * Returns the device host or ip address if available
     * @return device host or ip
     */
    public String getHostOrIP() {
        return null;
    }

    /**
     * Disconnects the current socket from the server/client on the other side
     * @param socket the socket instance
     */
    public void disconnectSocket(Object socket) {
    }
    
    
    /**
     * Indicates whether the socket is currently connected
     * @param socket is the socket we are connected to
     * @return true if the socket is connected
     */
    public boolean isSocketConnected(Object socket) {
        return false;
    }
    
    /**
     * Indicates whether the underlying implementation supports server sockets
     * @return false by default
     */
    public boolean isServerSocketAvailable() {
        return false;
    }

    /**
     * Indicates whether the underlying implementation supports sockets
     * @return false by default
     */
    public boolean isSocketAvailable() {
        return false;
    }
    
    /**
     * Return the pending error message on the given socket
     * @param socket the socket instance
     * @return the error message if available
     */
    public String getSocketErrorMessage(Object socket) {
        return null;
    }
    
    /**
     * Returns the pending error code on the given socket
     * @param socket the socket instance
     * @return the error code
     */
    public int getSocketErrorCode(Object socket) {
        return -1;
    }
    
    /**
     * Returns whether data is available for input on the socket
     * @param socket the socket instance
     * @return a none zero value if data is available for input
     */
    public int getSocketAvailableInput(Object socket) {
        return 0;
    }
    
    /**
     * Read pending bytes from the socket
     * @param socket the socket object
     * @return byte array with data read from the socket
     */
    public byte[] readFromSocketStream(Object socket) {
        return null;
    }
    
    /**
     * Write the following byte array to the socket
     * @param socket the socket instance
     * @param data the data written
     */
    public void writeToSocketStream(Object socket, byte[] data) {
    }
    
    private void mkdirs(FileSystemStorage fs, String path) {
        int lastPos = path.lastIndexOf('/');
        if (lastPos >= 0) {
            mkdirs(fs, path.substring(0, lastPos));
        }
        if (!fs.exists(path)) {
            mkdir(path);
        }
        
    }
    
    /**
     * Installs a tar file from the build server into the file system storage so it can be used with respect for hierarchy
     */
    public void installTar() throws IOException {
        String p = Preferences.get("cn1$InstallKey", null);
        String buildKey = Display.getInstance().getProperty("build_key", null);
        if(p == null || !p.equals(buildKey)) {
            FileSystemStorage fs = FileSystemStorage.getInstance();
            String tardir = fs.getAppHomePath() + "cn1html/";
            fs.mkdir(tardir);
            TarInputStream is = new TarInputStream(Display.getInstance().getResourceAsStream(getClass(), "/html.tar"));
            
            TarEntry t = is.getNextEntry();
            byte[] data = new byte[8192];
            while(t != null) {
                String name = t.getName();
                if(t.isDirectory()) {
                    fs.mkdir(tardir + name);
                } else {
                    String path = tardir + name;
                    String dir = path.substring(0,path.lastIndexOf('/'));
                    if (!fs.exists(dir)) {
                        mkdirs(fs, dir);
                    }

                    OutputStream os = fs.openOutputStream(tardir + name);
                    int count;
                    while((count = is.read(data)) != -1) {
                        os.write(data, 0, count);
                    }

                    os.close();
                }
                
                t = is.getNextEntry();
            }
            
            Util.cleanup(is);
            Preferences.set("cn1$InstallKey", buildKey);
        }
    }
    
    public void splitString(String source, char separator, ArrayList<String> out) {
        int len = source.length();
        boolean lastSeparator = false;
        StringBuilder buf = new StringBuilder();
        for(int iter = 0 ; iter < len ; iter++) {
            char current = source.charAt(iter);
            if(current == separator) {
                if(lastSeparator) {
                    buf.append(separator);
                    lastSeparator = false;
                    continue;
                }
                lastSeparator = true;
                if(buf.length() > 0) {
                    out.add(buf.toString());
                    buf.setLength(0);
                }
            } else {
                lastSeparator = false;
                buf.append(current);
            }
        }
        if(buf.length() > 0) {
            out.add(buf.toString());
        }
    }

    /**
     * Allows detecting development mode so debugging code and special cases can be used to simplify flow
     * @return true if we are running in the simulator, false otherwise
     */
    public boolean isSimulator() {
        return false;
    }

    /**
     * Paints the background of a component based on the style values on the
     * given graphics context, the style could be accessed from the drawing
     * thread in read only capacity to make the code slightly more efficient
     *
     * @param nativeGraphics the graphics context
     * @param x coordinate to draw
     * @param y coordinate to draw
     * @param width coordinate to draw
     * @param height coordinate to draw
     * @param s the style object to draw
     */
    public void paintComponentBackground(Object nativeGraphics, int x, int y, int width, int height, Style s) {
        if (width <= 0 || height <= 0) {
            return;
        }
        Image bgImageOrig = s.getBgImage();
        if (bgImageOrig == null) {
            if (s.getBackgroundType() >= Style.BACKGROUND_GRADIENT_LINEAR_VERTICAL) {
                drawGradientBackground(s, nativeGraphics, x, y, width, height);
                return;
            }
            setColor(nativeGraphics, s.getBgColor());
            fillRect(nativeGraphics, x, y, width, height, s.getBgTransparency());
        } else {
            int iW = bgImageOrig.getWidth();
            int iH = bgImageOrig.getHeight();
            Object bgImage = bgImageOrig.getImage();
            switch (s.getBackgroundType()) {
                case Style.BACKGROUND_NONE:
                    if (s.getBgTransparency() != 0) {
                        setColor(nativeGraphics, s.getBgColor());
                        fillRect(nativeGraphics, x, y, width, height, s.getBgTransparency());
                    }
                    return;
                case Style.BACKGROUND_IMAGE_SCALED:
                    if (isScaledImageDrawingSupported()) {
                        drawImage(nativeGraphics, bgImage, x, y, width, height);
                    } else {
                        if (iW != width || iH != height) {
                            bgImageOrig = bgImageOrig.scaled(width, height);
                            s.setBgImage(bgImageOrig, true);
                            bgImage = bgImageOrig.getImage();
                        }
                        drawImage(nativeGraphics, bgImage, x, y);
                    }
                    return;
                case Style.BACKGROUND_IMAGE_SCALED_FILL:
                    float r = Math.max(((float) width) / ((float) iW), ((float) height) / ((float) iH));
                    int bwidth = (int) (((float) iW) * r);
                    int bheight = (int) (((float) iH) * r);
                    if (isScaledImageDrawingSupported()) {
                        drawImage(nativeGraphics, bgImage, x + (width - bwidth) / 2, y + (height - bheight) / 2, bwidth, bheight);
                    } else {
                        if (iW != bwidth || iH != bheight) {
                            bgImageOrig = bgImageOrig.scaled(bwidth, bheight);
                            s.setBgImage(bgImageOrig, true);
                            bgImage = bgImageOrig.getImage();
                        }
                        drawImage(nativeGraphics, bgImage, x + (width - bwidth) / 2, y + (height - bheight) / 2);
                    }
                    return;
                case Style.BACKGROUND_IMAGE_SCALED_FIT:
                    if (s.getBgTransparency() != 0) {
                        setColor(nativeGraphics, s.getBgColor());
                        fillRect(nativeGraphics, x, y, width, height, s.getBgTransparency());
                    }
                    float r2 = Math.min(((float) width) / ((float) iW), ((float) height) / ((float) iH));
                    int awidth = (int) (((float) iW) * r2);
                    int aheight = (int) (((float) iH) * r2);
                    if (isScaledImageDrawingSupported()) {
                        drawImage(nativeGraphics, bgImage, x + (width - awidth) / 2, y + (height - aheight) / 2, awidth, aheight);
                    } else {
                        if (iW != awidth || iH != aheight) {
                            bgImageOrig = bgImageOrig.scaled(awidth, aheight);
                            s.setBgImage(bgImageOrig, true);
                            bgImage = bgImageOrig.getImage();
                        }
                        drawImage(nativeGraphics, bgImage, x + (width - awidth) / 2, y + (height - aheight) / 2, awidth, aheight);
                    }
                    return;
                case Style.BACKGROUND_IMAGE_TILE_BOTH:
                    tileImage(nativeGraphics, bgImage, x, y, width, height);
                    return;
                case Style.BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_TOP:
                    setColor(nativeGraphics, s.getBgColor());
                    fillRect(nativeGraphics, x, y, width, height, s.getBgTransparency());
                    tileImage(nativeGraphics, bgImage, x, y, width, iH);
                    return;
                case Style.BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_CENTER:
                    setColor(nativeGraphics, s.getBgColor());
                    fillRect(nativeGraphics, x, y, width, height, s.getBgTransparency());
                    tileImage(nativeGraphics, bgImage, x, y + (height / 2 - iH / 2), width, iH);
                    return;
                case Style.BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_BOTTOM:
                    setColor(nativeGraphics, s.getBgColor());
                    fillRect(nativeGraphics, x, y, width, height, s.getBgTransparency());
                    tileImage(nativeGraphics, bgImage, x, y + (height - iH), width, iH);
                    return;
                case Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_LEFT:
                    setColor(nativeGraphics, s.getBgColor());
                    fillRect(nativeGraphics, x, y, width, height, s.getBgTransparency());
                    for (int yPos = 0; yPos <= height; yPos += iH) {
                        drawImage(nativeGraphics, bgImage, x, y + yPos);
                    }
                    return;
                case Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_CENTER:
                    setColor(nativeGraphics, s.getBgColor());
                    fillRect(nativeGraphics, x, y, width, height, s.getBgTransparency());
                    for (int yPos = 0; yPos <= height; yPos += iH) {
                        drawImage(nativeGraphics, bgImage, x + (width / 2 - iW / 2), y + yPos);
                    }
                    return;
                case Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_RIGHT:
                    setColor(nativeGraphics, s.getBgColor());
                    fillRect(nativeGraphics, x, y, width, height, s.getBgTransparency());
                    for (int yPos = 0; yPos <= height; yPos += iH) {
                        drawImage(nativeGraphics, bgImage, x + width - iW, y + yPos);
                    }
                    return;
                case Style.BACKGROUND_IMAGE_ALIGNED_TOP:
                    setColor(nativeGraphics, s.getBgColor());
                    fillRect(nativeGraphics, x, y, width, height, s.getBgTransparency());
                    drawImage(nativeGraphics, bgImage, x + (width / 2 - iW / 2), y);
                    return;
                case Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM:
                    setColor(nativeGraphics, s.getBgColor());
                    fillRect(nativeGraphics, x, y, width, height, s.getBgTransparency());
                    drawImage(nativeGraphics, bgImage, x + (width / 2 - iW / 2), y + (height - iH));
                    return;
                case Style.BACKGROUND_IMAGE_ALIGNED_LEFT:
                    setColor(nativeGraphics, s.getBgColor());
                    fillRect(nativeGraphics, x, y, width, height, s.getBgTransparency());
                    drawImage(nativeGraphics, bgImage, x, y + (height / 2 - iH / 2));
                    return;
                case Style.BACKGROUND_IMAGE_ALIGNED_RIGHT:
                    setColor(nativeGraphics, s.getBgColor());
                    fillRect(nativeGraphics, x, y, width, height, s.getBgTransparency());
                    drawImage(nativeGraphics, bgImage, x + width - iW, y + (height / 2 - iH / 2));
                    return;
                case Style.BACKGROUND_IMAGE_ALIGNED_CENTER:
                    setColor(nativeGraphics, s.getBgColor());
                    fillRect(nativeGraphics, x, y, width, height, s.getBgTransparency());
                    drawImage(nativeGraphics, bgImage, x + (width / 2 - iW / 2), y + (height / 2 - iH / 2));
                    return;
                case Style.BACKGROUND_IMAGE_ALIGNED_TOP_LEFT:
                    setColor(nativeGraphics, s.getBgColor());
                    fillRect(nativeGraphics, x, y, width, height, s.getBgTransparency());
                    drawImage(nativeGraphics, bgImage, x, y);
                    return;
                case Style.BACKGROUND_IMAGE_ALIGNED_TOP_RIGHT:
                    setColor(nativeGraphics, s.getBgColor());
                    fillRect(nativeGraphics, x, y, width, height, s.getBgTransparency());
                    drawImage(nativeGraphics, bgImage, x + width - iW, y);
                    return;
                case Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM_LEFT:
                    setColor(nativeGraphics, s.getBgColor());
                    fillRect(nativeGraphics, x, y, width, height, s.getBgTransparency());
                    drawImage(nativeGraphics, bgImage, x, y + (height - iH));
                    return;
                case Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM_RIGHT:
                    setColor(nativeGraphics, s.getBgColor());
                    fillRect(nativeGraphics, x, y, width, height, s.getBgTransparency());
                    drawImage(nativeGraphics, bgImage, x + width - iW, y + (height - iH));
                    return;
                case Style.BACKGROUND_GRADIENT_LINEAR_HORIZONTAL:
                case Style.BACKGROUND_GRADIENT_LINEAR_VERTICAL:
                case Style.BACKGROUND_GRADIENT_RADIAL:
                    drawGradientBackground(s, nativeGraphics, x, y, width, height);
                    return;
            }
        }
    }

    private void drawGradientBackground(Style s, Object nativeGraphics, int x, int y, int width, int height) {
        switch (s.getBackgroundType()) {
            case Style.BACKGROUND_GRADIENT_LINEAR_HORIZONTAL:
                fillLinearGradient(nativeGraphics, s.getBackgroundGradientStartColor(), s.getBackgroundGradientEndColor(),
                        x, y, width, height, true);
                return;
            case Style.BACKGROUND_GRADIENT_LINEAR_VERTICAL:
                fillLinearGradient(nativeGraphics, s.getBackgroundGradientStartColor(), s.getBackgroundGradientEndColor(),
                        x, y, width, height, false);
                return;
            case Style.BACKGROUND_GRADIENT_RADIAL:
                fillRectRadialGradient(nativeGraphics, s.getBackgroundGradientStartColor(), s.getBackgroundGradientEndColor(),
                        x, y, width, height, s.getBackgroundGradientRelativeX(), s.getBackgroundGradientRelativeY(),
                        s.getBackgroundGradientRelativeSize());
                return;
        }
        setColor(nativeGraphics, s.getBgColor());
        fillRect(nativeGraphics, x, y, width, height, s.getBgTransparency());
    }

    /**
     * Fills a rectangle with an optionally translucent fill color
     *
     * @param nativeGraphics the underlying native graphics object
     * @param x the x coordinate of the rectangle to be filled
     * @param y the y coordinate of the rectangle to be filled
     * @param w the width of the rectangle to be filled
     * @param h the height of the rectangle to be filled
     * @param alpha the alpha values specify semitransparency
     */
    public void fillRect(Object nativeGraphics, int x, int y, int w, int h, byte alpha) {
        if (alpha != 0) {
            int oldAlpha = getAlpha(nativeGraphics);
            setAlpha(nativeGraphics, alpha & 0xff);
            fillRect(nativeGraphics, x, y, w, h);
            setAlpha(nativeGraphics, oldAlpha);
        }
    }

    /**
     * Draws a label on the given graphics context, this method allows optimizing the very common drawing operation
     * using platform native code
     */
    public void drawLabelComponent(Object nativeGraphics, int cmpX, int cmpY, int cmpHeight, int cmpWidth,
            Style style, String text, Object icon, Object stateIcon, int preserveSpaceForState, int gap, boolean rtl,
            boolean isOppositeSide, int textPosition, int stringWidth, boolean isTickerRunning, int tickerShiftText,
            boolean endsWith3Points, int valign) {
        Font font = style.getFont();
        Object nativeFont = font.getNativeFont();
        setNativeFont(nativeGraphics, nativeFont);
        setColor(nativeGraphics, style.getFgColor());

        int iconWidth = 0;
        int iconHeight = 0;
        if(icon != null) {
            iconWidth = getImageWidth(icon);
            iconHeight = getImageHeight(icon);
        }

        int textDecoration = style.getTextDecoration();
        int stateIconSize = 0;
        int stateIconYPosition = 0;

        int leftPadding = style.getPaddingLeft(rtl);
        int rightPadding = style.getPaddingRight(rtl);
        int topPadding = style.getPaddingTop();
        int bottomPadding = style.getPaddingBottom();

        int fontHeight = 0;
        if (text == null) {
            text = "";
        }
        if (text.length() > 0) {
            fontHeight = font.getHeight();
        }

        if (stateIcon != null) {
            stateIconSize = getImageWidth(stateIcon);
            stateIconYPosition = cmpY + topPadding
                    + (cmpHeight - topPadding
                    - bottomPadding) / 2 - stateIconSize / 2;
            int tX = cmpX;
            if (isOppositeSide) {
                if (rtl) {
                    tX += leftPadding;
                } else {
                    tX = tX + cmpWidth - leftPadding - stateIconSize;
                }
                cmpWidth -= leftPadding - stateIconSize;
            } else {
                preserveSpaceForState = stateIconSize + gap;
                if (rtl) {
                    tX = tX + cmpWidth - leftPadding - stateIconSize;
                } else {
                    tX += leftPadding;
                }
            }

            drawImage(nativeGraphics, stateIcon, tX, stateIconYPosition);
        }

        //default for bottom left alignment
        int x = cmpX + leftPadding + preserveSpaceForState;
        int y = cmpY + topPadding;

        int align = reverseAlignForBidi(rtl, style.getAlignment());

        int textPos = reverseAlignForBidi(rtl, textPosition);

        //set initial x,y position according to the alignment and textPosition
        switch (align) {
            case Component.LEFT:
                switch (textPos) {
                    case Label.LEFT:
                    case Label.RIGHT:
                        y = y + (cmpHeight - (topPadding + bottomPadding + Math.max(((icon != null) ? iconHeight : 0), fontHeight))) / 2;
                        break;
                    case Label.BOTTOM:
                    case Label.TOP:
                        y = y + (cmpHeight - (topPadding + bottomPadding + ((icon != null) ? iconHeight + gap : 0) + fontHeight)) / 2;
                        break;
                }
                break;
            case Component.CENTER:
                switch (textPos) {
                    case Label.LEFT:
                    case Label.RIGHT:
                        x = x + (cmpWidth - (preserveSpaceForState
                                + leftPadding
                                + rightPadding
                                + ((icon != null) ? iconWidth + gap : 0)
                                + stringWidth)) / 2;
                        x = Math.max(x, cmpX + leftPadding + preserveSpaceForState);
                        y = y + (cmpHeight - (topPadding
                                + bottomPadding
                                + Math.max(((icon != null) ? iconHeight : 0),
                                        fontHeight))) / 2;
                        break;
                    case Label.BOTTOM:
                    case Label.TOP:
                        x = x + (cmpWidth - (preserveSpaceForState + leftPadding
                                + rightPadding
                                + Math.max(((icon != null) ? iconWidth + gap : 0),
                                        stringWidth))) / 2;
                        x = Math.max(x, cmpX + leftPadding + preserveSpaceForState);
                        y = y + (cmpHeight - (topPadding
                                + bottomPadding
                                + ((icon != null) ? iconHeight + gap : 0)
                                + fontHeight)) / 2;
                        break;
                }
                break;
            case Component.RIGHT:
                switch (textPos) {
                    case Label.LEFT:
                    case Label.RIGHT:
                        x = cmpX + cmpWidth - rightPadding
                                - (((icon != null) ? (iconWidth + gap) : 0)
                                + stringWidth);
                        if (rtl) {
                            x = Math.max(x - preserveSpaceForState, cmpX + leftPadding);
                        } else {
                            x = Math.max(x, cmpX + leftPadding + preserveSpaceForState);
                        }
                        y = y + (cmpHeight - (topPadding
                                + bottomPadding
                                + Math.max(((icon != null) ? iconHeight : 0),
                                        fontHeight))) / 2;
                        break;
                    case Label.BOTTOM:
                    case Label.TOP:
                        x = cmpX + cmpWidth - rightPadding
                                - (Math.max(((icon != null) ? (iconWidth) : 0),
                                        stringWidth));
                        x = Math.max(x, cmpX + leftPadding + preserveSpaceForState);
                        y = y + (cmpHeight - (topPadding
                                + bottomPadding
                                + ((icon != null) ? iconHeight + gap : 0) + fontHeight)) / 2;
                        break;
                }
                break;
            default:
                break;
        }

        int textSpaceW = cmpWidth - rightPadding - leftPadding;

        if (icon != null && (textPos == Label.RIGHT || textPos == Label.LEFT)) {
            textSpaceW = textSpaceW - iconWidth;
        }

        if (stateIcon != null) {
            textSpaceW = textSpaceW - stateIconSize;
        } else {
            textSpaceW = textSpaceW - preserveSpaceForState;
        }

        if (icon == null) { 
            // no icon only string 
            drawLabelString(nativeGraphics, nativeFont, text, x, y, textSpaceW, isTickerRunning, tickerShiftText,
                    textDecoration, rtl, endsWith3Points, stringWidth, fontHeight);
        } else {
            int strWidth = stringWidth;
            int iconStringWGap;
            int iconStringHGap;

            switch (textPos) {
                case Label.LEFT:
                    if (iconHeight > fontHeight) {
                        iconStringHGap = (iconHeight - fontHeight) / 2;
                        strWidth = drawLabelStringValign(nativeGraphics, nativeFont, text, x, y, textSpaceW, isTickerRunning,
                                tickerShiftText, textDecoration, rtl, endsWith3Points, strWidth, iconStringHGap, iconHeight,
                                fontHeight, valign);

                        drawImage(nativeGraphics, icon, x + strWidth + gap, y);
                    } else {
                        iconStringHGap = (fontHeight - iconHeight) / 2;
                        strWidth = drawLabelString(nativeGraphics, nativeFont, text, x, y, textSpaceW, isTickerRunning,
                                tickerShiftText, textDecoration, rtl, endsWith3Points, strWidth, fontHeight);

                        drawImage(nativeGraphics, icon, x + strWidth + gap, y + iconStringHGap);
                    }
                    break;
                case Label.RIGHT:
                    if (iconHeight > fontHeight) {
                        iconStringHGap = (iconHeight - fontHeight) / 2;
                        drawImage(nativeGraphics, icon, x, y);
                        drawLabelStringValign(nativeGraphics, nativeFont, text, x + iconWidth + gap, y, textSpaceW, isTickerRunning,
                                tickerShiftText, textDecoration, rtl, endsWith3Points, stringWidth, iconStringHGap, iconHeight, fontHeight, valign);
                    } else {
                        iconStringHGap = (fontHeight - iconHeight) / 2;
                        drawImage(nativeGraphics, icon, x, y + iconStringHGap);
                        drawLabelString(nativeGraphics, nativeFont, text, x + iconWidth + gap, y, textSpaceW, isTickerRunning,
                                tickerShiftText, textDecoration, rtl, endsWith3Points, stringWidth, fontHeight);
                    }
                    break;
                case Label.BOTTOM:
                    //center align the smaller
                    if (iconWidth > strWidth) { 
                        iconStringWGap = (iconWidth - strWidth) / 2;
                        drawImage(nativeGraphics, icon, x, y);
                        drawLabelString(nativeGraphics, nativeFont, text, x + iconStringWGap, y + iconHeight + gap, textSpaceW,
                                isTickerRunning, tickerShiftText, textDecoration, rtl, endsWith3Points, stringWidth, fontHeight);
                    } else {
                        iconStringWGap = (Math.min(strWidth, textSpaceW) - iconWidth) / 2;
                        drawImage(nativeGraphics, icon, x + iconStringWGap, y);

                        drawLabelString(nativeGraphics, nativeFont, text, x, y + iconHeight + gap, textSpaceW, isTickerRunning,
                                tickerShiftText, textDecoration, rtl, endsWith3Points, stringWidth, fontHeight);
                    }
                    break;
                case Label.TOP:
                    //center align the smaller
                    if (iconWidth > strWidth) { 
                        iconStringWGap = (iconWidth - strWidth) / 2;
                        drawLabelString(nativeGraphics, nativeFont, text, x + iconStringWGap, y, textSpaceW, isTickerRunning,
                                tickerShiftText, textDecoration, rtl, endsWith3Points, stringWidth, fontHeight);
                        drawImage(nativeGraphics, icon, x, y + fontHeight + gap);
                    } else {
                        iconStringWGap = (Math.min(strWidth, textSpaceW) - iconWidth) / 2;
                        drawLabelString(nativeGraphics, nativeFont, text, x, y, textSpaceW, isTickerRunning, tickerShiftText,
                                textDecoration, rtl, endsWith3Points, stringWidth, fontHeight);
                        drawImage(nativeGraphics, icon, x + iconStringWGap, y + fontHeight + gap);
                    }
                    break;
            }
        }
    }

    /**
     * Implements the drawString for the text component and adjust the valign
     * assuming the icon is in one of the sides
     */
    private int drawLabelStringValign(
            Object nativeGraphics, Object nativeFont, String str, int x, int y, int textSpaceW,
            boolean isTickerRunning, int tickerShiftText, int textDecoration, boolean rtl,
            boolean endsWith3Points, int textWidth,
            int iconStringHGap, int iconHeight, int fontHeight, int valign) {
        switch (valign) {
            case Component.TOP:
                return drawLabelString(nativeGraphics, nativeFont, str, x, y, textSpaceW, isTickerRunning, tickerShiftText, textDecoration, rtl, endsWith3Points, textWidth, fontHeight);
            case Component.CENTER:
                return drawLabelString(nativeGraphics, nativeFont, str, x, y + iconHeight / 2 - fontHeight / 2, textSpaceW, isTickerRunning, tickerShiftText, textDecoration, rtl, endsWith3Points, textWidth, fontHeight);
            default:
                return drawLabelString(nativeGraphics, nativeFont, str, x, y + iconStringHGap, textSpaceW, isTickerRunning, tickerShiftText, textDecoration, rtl, endsWith3Points, textWidth, fontHeight);
        }
    }

    /**
     * Implements the drawString for the text component and adjust the valign
     * assuming the icon is in one of the sides
     */
    private int drawLabelString(Object nativeGraphics, Object nativeFont, String text, int x, int y, int textSpaceW,
            boolean isTickerRunning, int tickerShiftText, int textDecoration, boolean rtl, boolean endsWith3Points, int textWidth,
            int fontHeight) {
        int cx = getClipX(nativeGraphics);
        int cy = getClipY(nativeGraphics);
        int cw = getClipWidth(nativeGraphics);
        int ch = getClipHeight(nativeGraphics);
        clipRect(nativeGraphics, x, cy, textSpaceW, ch);

        int drawnW = drawLabelText(nativeGraphics, textDecoration, rtl, isTickerRunning, endsWith3Points, nativeFont,
                textWidth, textSpaceW, tickerShiftText, text, x, y, fontHeight);

        setClip(nativeGraphics, cx, cy, cw, ch);

        return drawnW;
    }

    private boolean fastCharWidthCheck(String s, int length, int width, int charWidth, Object f) {
        if (length * charWidth < width) {
            return true;
        }
        length = Math.min(s.length(), length);
        return stringWidth(f, s.substring(0, length)) < width;
    }

    /**
     * Draws the text of a label
     *
     * @param nativeGraphics graphics context
     * @param textDecoration decoration information for the text
     * @param text the text for the label
     * @param x position for the label
     * @param y position for the label
     * @param txtW stringWidth(text) equivalent which is faster than just
     * invoking string width all the time
     * @param textSpaceW the width available for the component
     * @return the space used by the drawing
     */
    protected int drawLabelText(Object nativeGraphics, int textDecoration, boolean rtl, boolean isTickerRunning,
            boolean endsWith3Points, Object nativeFont, int txtW, int textSpaceW, int shiftText, String text, int x, int y, int fontHeight) {
        if ((!isTickerRunning) || rtl) {
            //if there is no space to draw the text add ... at the end
            if (txtW > textSpaceW && textSpaceW > 0) {
                // Handling of adding 3 points and in fact all text positioning when the text is bigger than
                // the allowed space is handled differently in RTL, this is due to the reverse algorithm
                // effects - i.e. when the text includes both Hebrew/Arabic and English/numbers then simply
                // trimming characters from the end of the text (as done with LTR) won't do.
                // Instead we simple reposition the text, and draw the 3 points, this is quite simple, but
                // the downside is that a part of a letter may be shown here as well.

                if (rtl) {
                    if ((!isTickerRunning) && endsWith3Points) {
                        String points = "...";
                        int pointsW = stringWidth(nativeFont, points);
                        drawString(nativeGraphics, nativeFont, points, shiftText + x, y, textDecoration, fontHeight);
                        clipRect(nativeGraphics, pointsW + shiftText + x, y, textSpaceW - pointsW, fontHeight);
                    }
                    x = x - txtW + textSpaceW;
                } else if (endsWith3Points) {
                    String points = "...";
                    int index = 1;
                    int widest = charWidth(nativeFont, 'W');
                    int pointsW = stringWidth(nativeFont, points);
                    int textLen = text.length();
                    while (fastCharWidthCheck(text, index, textSpaceW - pointsW, widest, nativeFont) && index < textLen) {
                        index++;
                    }
                    text = text.substring(0, Math.min(textLen, Math.max(1, index - 1))) + points;
                    txtW = stringWidth(nativeFont, text);
                }
            }
        }

        drawString(nativeGraphics, nativeFont, text, shiftText + x, y, textDecoration, fontHeight);
        return Math.min(txtW, textSpaceW);
    }
    
    /**
     * Draw a string using the current font and color in the x,y coordinates.
     * The font is drawn from the top position and not the baseline.
     *
     * @param nativeGraphics the graphics context
     * @param nativeFont the font used
     * @param str the string to be drawn.
     * @param x the x coordinate.
     * @param y the y coordinate.
     * @param textDecoration Text decoration bitmask (See Style's
     * TEXT_DECORATION_* constants)
     */
    public void drawString(Object nativeGraphics, Object nativeFont, String str, int x, int y, int textDecoration) {
        drawString(nativeGraphics, nativeFont, str, x, y, textDecoration, getHeight(nativeFont));
    }

    /**
     * Draw a string using the current font and color in the x,y coordinates.
     * The font is drawn from the top position and not the baseline.
     *
     * @param nativeGraphics the graphics context
     * @param nativeFont the font used
     * @param str the string to be drawn.
     * @param x the x coordinate.
     * @param y the y coordinate.
     * @param textDecoration Text decoration bitmask (See Style's
     * TEXT_DECORATION_* constants)
     */
    private void drawString(Object nativeGraphics, Object nativeFont, String str, int x, int y, int textDecoration, int fontHeight) {
        if (str.length() == 0) {
            return;
        }

        // this if has only the minor effect of providing a slighly faster execution path
        if (textDecoration != 0) {
            boolean raised = (textDecoration & Style.TEXT_DECORATION_3D) != 0;
            boolean lowerd = (textDecoration & Style.TEXT_DECORATION_3D_LOWERED) != 0;
            boolean north = (textDecoration & Style.TEXT_DECORATION_3D_SHADOW_NORTH) != 0;
            if (raised || lowerd || north) {
                textDecoration = textDecoration & (~Style.TEXT_DECORATION_3D) & (~Style.TEXT_DECORATION_3D_LOWERED) & (~Style.TEXT_DECORATION_3D_SHADOW_NORTH);
                int c = getColor(nativeGraphics);
                int a = getAlpha(nativeGraphics);
                int newColor = 0;
                int offset = -2;
                if (lowerd) {
                    offset = 2;
                    newColor = 0xffffff;
                } else if (north) {
                    offset = 2;
                }
                setColor(nativeGraphics, newColor);
                if (a == 0xff) {
                    setAlpha(nativeGraphics, 140);
                }
                drawString(nativeGraphics, nativeFont, str, x, y + offset, textDecoration, fontHeight);
                setAlpha(nativeGraphics, a);
                setColor(nativeGraphics, c);
                drawString(nativeGraphics, nativeFont, str, x, y, textDecoration, fontHeight);
                return;
            }
            drawString(nativeGraphics, str, x, y);
            if ((textDecoration & Style.TEXT_DECORATION_UNDERLINE) != 0) {
                drawLine(nativeGraphics, x, y + fontHeight - 1, x + stringWidth(nativeFont, str), y + fontHeight - 1);
            }
            if ((textDecoration & Style.TEXT_DECORATION_STRIKETHRU) != 0) {
                drawLine(nativeGraphics, x, y + fontHeight / 2, x + stringWidth(nativeFont, str), y + fontHeight / 2);
            }
            if ((textDecoration & Style.TEXT_DECORATION_OVERLINE) != 0) {
                drawLine(nativeGraphics, x, y, x + stringWidth(nativeFont, str), y);
            }
        } else {
            drawString(nativeGraphics, str, x, y);
        }
    }

    /**
     * Reverses alignment in the case of bidi
     */
    private int reverseAlignForBidi(boolean rtl, int align) {
        if (rtl) {
            switch (align) {
                case Component.RIGHT:
                    return Component.LEFT;
                case Component.LEFT:
                    return Component.RIGHT;
            }
        }
        return align;
    }

    /**
     * Makes it easier to pass hints to the underlying implementation for quicker hacks/pipelines
     * @param key the key
     * @param value the value
     */
    public void setPlatformHint(String key, String value) {
    }
    
    //METHODS FOR DEALING Local Notifications
    public void scheduleLocalNotification(LocalNotification notif, long firstTime, int repeat) {
    }

    public void cancelLocalNotification(String notificationId) {
    }
    //ENDS METHODS FOR DEALING Local Notifications

    /**
     * Sets the preferred time interval between background fetches.  This is only a
     * preferred interval and is not guaranteed.  Some platforms, like iOS, maintain sovereign 
     * control over when and if background fetches will be allowed. This number is used
     * only as a guideline.
     * 
     * <p><strong>This method must be called in order to activate background fetch.</strong>></p>
     * <p>Note: If the platform doesn't support background fetch (i.e. {@link #isBackgroundFetchSupported() } returns {@code false},
     * then this method does nothing.</p>
     * @param seconds The time interval in seconds.
     * 
     * @see #isBackgroundFetchSupported() 
     * @see #getPreferredBackgroundFetchInterval() 
     * @see com.codename1.background.BackgroundFetch
     * @see com.codename1.ui.Display.setPreferredBackgroundFetchInterval(int)
     */
    public void setPreferredBackgroundFetchInterval(int seconds) {
        if (isBackgroundFetchSupported()) {
            Preferences.set("$$CN1_BACKGROUND_FETCH_INTERVAL", seconds);
        }
    }
    
    /**
     * Gets the preferred time (in seconds) between background fetches.
     * @return The time interval in seconds.
     * @see #isBackgroundFetchSupported() 
     * @see #setPreferredBackgroundFetchInterval(int) 
     * @see com.codename1.background.BackgroundFetch
     * @see com.codename1.ui.Display.setPreferredBackgroundFetchInterval(int)
     */
    public int getPreferredBackgroundFetchInterval() {
        if (isBackgroundFetchSupported()) {
            return Preferences.get("$$CN1_BACKGROUND_FETCH_INTERVAL", 60 * 60);
        } else {
            return -1;
        }
    }
    
    /**
     * Checks to see if the current platform supports background fetch.
     * @return True if the current platform supports background fetch.
     * @see #setPreferredBackgroundFetchInterval(int) 
     * @see #getPreferredBackgroundFetchInterval() 
     * @see com.codename1.background.BackgroundFetch
     * @see com.codename1.ui.Display.setPreferredBackgroundFetchInterval(int)
     */
    public boolean isBackgroundFetchSupported() {
        return false;
    }
    
    public Image gaussianBlurImage(Image image, float radius) {
        return image;
    }

    public boolean isGaussianBlurSupported() {
        return false;
    }
    
    /**
     * Returns true if this device is jailbroken or rooted, false if not or unknown. Notice that this method isn't
     * accurate and can't detect all jailbreak/rooting cases
     * @return true if this device is jailbroken or rooted, false if not or unknown. 
     */
    public boolean isJailbrokenDevice() {
        return false;
    }
    
    /**
     * Returns the build hints for the simulator, this will only work in the debug environment and it's 
     * designed to allow extensions/API's to verify user settings/build hints exist
     * @return map of the build hints that isn't modified without the codename1.arg. prefix
     */
    public Map<String, String> getProjectBuildHints() {
        return null;
    }

    /**
     * Sets a build hint into the settings while overwriting any previous value. This will only work in the 
     * debug environment and it's designed to allow extensions/API's to verify user settings/build hints exist.
     * Important: this will throw an exception outside of the simulator!
     * @param key the build hint without the codename1.arg. prefix
     * @param value the value for the hint
     */
    public void setProjectBuildHint(String key, String value) {
        throw new RuntimeException();
    }
}
