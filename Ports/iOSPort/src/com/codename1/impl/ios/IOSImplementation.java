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
package com.codename1.impl.ios;

import com.codename1.background.BackgroundFetch;
import com.codename1.capture.VideoCaptureConstraints;
import com.codename1.codescan.CodeScanner;
import com.codename1.codescan.ScanResult;
import com.codename1.contacts.Address;
import com.codename1.contacts.Contact;
import com.codename1.db.Database;
import com.codename1.impl.CodenameOneImplementation;
import com.codename1.location.Location;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Image;
import com.codename1.ui.PeerComponent;
import com.codename1.ui.Sheet;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import java.util.StringTokenizer;
import com.codename1.io.BufferedInputStream;
import com.codename1.io.BufferedOutputStream;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Storage;
import com.codename1.io.Util;
import com.codename1.l10n.L10NManager;
import com.codename1.location.LocationListener;
import com.codename1.location.LocationManager;
import com.codename1.media.Media;
import com.codename1.messaging.Message;
import com.codename1.payment.Purchase;
import com.codename1.payment.PurchaseCallback;
import com.codename1.push.PushCallback;
import com.codename1.push.PushActionsProvider;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.util.EventDispatcher;
import com.codename1.ui.util.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import com.codename1.io.Cookie;
import com.codename1.io.Log;
import com.codename1.io.Preferences;
import com.codename1.location.Geofence;
import com.codename1.location.GeofenceListener;
import com.codename1.location.LocationRequest;
import com.codename1.media.AbstractMedia;
import com.codename1.media.AudioBuffer;
import com.codename1.media.MediaManager;
import com.codename1.media.MediaRecorderBuilder;
import com.codename1.notifications.LocalNotification;
import com.codename1.notifications.LocalNotificationCallback;
import com.codename1.notifications.NotificationPermissionCallback;
import com.codename1.notifications.NotificationPermissionRequest;
import com.codename1.notifications.NotificationPermissionResult;
import com.codename1.background.BackgroundWorker;
import com.codename1.background.ForegroundService;
import com.codename1.background.WorkRequest;
import com.codename1.share.SharedContent;
import com.codename1.payment.RestoreCallback;
import com.codename1.push.PushAction;
import com.codename1.push.PushActionCategory;
import com.codename1.push.PushContent;
import com.codename1.ui.Accessor;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.Stroke;
import com.codename1.ui.Transform;
import com.codename1.ui.geom.PathIterator;
import com.codename1.ui.geom.Shape;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.spinner.Picker;
import com.codename1.util.AsyncResource;
import com.codename1.util.Callback;
import com.codename1.util.StringUtil;
import com.codename1.util.SuccessCallback;
import com.codename1.util.Simd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.util.Collections;
import com.codename1.ui.plaf.DefaultLookAndFeel;


/**
 *
 * @author Shai Almog
 */
public class IOSImplementation extends CodenameOneImplementation {
    // Flag to indicate if the current openGallery process is selecting multiple files
    private boolean disableUIWebView=true;
    private static boolean gallerySelectMultiple;
    public static IOSNative nativeInstance = new IOSNative();
    private static LocalNotificationCallback localNotificationCallback;
    private static PurchaseCallback purchaseCallback;
    private static RestoreCallback restoreCallback;
    private int timeout = 120000;
    private static final Object CONNECTIONS_LOCK = new Object();
    private ArrayList<NetworkConnection> connections = new ArrayList<NetworkConnection>();
    private NativeFont defaultFont;
    private NativeGraphics currentlyDrawingOn;
    //private NativeImage backBuffer;
    private NativeGraphics globalGraphics;
    /// True when iOS was built with -Dios.metal=true. The mutable-image
    /// alpha-mask routing in MutableGraphics relies on the C-side
    /// drawTextureAlphaMaskImpl tagging the op with currentMutableImage so
    /// drawFrame's drain switches the encoder to the mutable's MTLTexture
    /// before drawing -- a Metal-only code path (`#ifdef CN1_USE_METAL`
    /// guard around `setTarget` in CodenameOne_GLViewController.m). On a
    /// GL build the same alpha-mask op runs against the screen encoder,
    /// so the round-rect mask lands on the screen instead of inside the
    /// mutable's UIImage and Switch's track / thumb come out empty.
    /// `metalRendering` keeps the CG-bitmap-then-DrawImage fallback in
    /// place for GL while letting Metal use the unified alpha-mask
    /// pipeline.
    ///
    /// Static so inner-class accessors don't trip over javac's synthesized
    /// outer-instance lookup (CI bisect 25259320137 traced "mutable shape
    /// ops render via the CG fallback instead of the alpha-mask Metal
    /// pipeline" to that gate not firing).
    static boolean metalRendering;
    static IOSImplementation instance;
    private TextArea currentEditing;
    private static boolean initialized;
    private Lifecycle life;
    private CodeScannerImpl scannerInstance;
    private static boolean minimized;
    private String userAgent;
    private TextureCache textureCache = new TextureCache();
    private static boolean dropEvents;
    private static boolean callInterruptionActive;
    
    private NativePathRenderer globalPathRenderer;
    private NativePathStroker globalPathStroker;
    
    private boolean isActive=false;
    private final ArrayList<Runnable> onActiveListeners = new ArrayList<Runnable>();
    private static BackgroundFetch backgroundFetchCallback;

    private boolean useContentBasedRTLStringDetection = false;
    
    
    /**
     * A pool that will cause java objects to be retained if they are passed 
     * to a non-managed thread via a mechanism like dispatch_async
     */
    private static ArrayList autoreleasePool = new ArrayList();
    
    static void retain(Object o){
        if (o != null){
            autoreleasePool.add(o);
        }
    }
    
    static void release(Object o){
        if (o != null){
            autoreleasePool.remove(o);
        }
    }
    

    public void initEDT() {
        while(!initialized) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
            }
        }
        if(globalGraphics == null) {
            globalGraphics = new GlobalGraphics();
        }
        
    }

    private static Runnable callback;
    
    public static void callback() {
        initialized = true;
        Display.getInstance().callSerially(callback);
    }
    
    public void postInit() {
        nativeInstance.initVM();
        super.postInit();
    }
    
    @Override
    protected void initDefaultUserAgent() {
        String ue = getProperty("User-Agent", null);
        if(ue != null) {
            ConnectionRequest.setDefaultUserAgent(Display.getInstance().getProperty("User-Agent", ue));
        }
    }

    
    public void init(Object m) {
        instance = this;
        // Set the metalRendering static gate as early as possible -- before
        // any NativeImage / NativeGraphics is constructed -- so mutable-image
        // rendering routes through the alpha-mask Metal pipeline from the
        // very first paint on Metal builds, and through the CG-bitmap
        // fallback on GL builds (where the alpha-mask op can't target a
        // mutable, see comment on the static field above).
        metalRendering = nativeInstance.isMetalRendering();
        setUseNativeCookieStore(false);
        Display.getInstance().setTransitionYield(10);
        Display.getInstance().setDefaultVirtualKeyboard(new IOSVirtualKeyboard(this));
        callback = (Runnable)m;
        if(m instanceof Lifecycle) {
            life = (Lifecycle)m;
        }
        VideoCaptureConstraints.init(new IOSVideoCaptureConstraintsCompiler());
        if("true".equals(Display.getInstance().getProperty("DisableScreenshots", ""))) {
            nativeInstance.setDisableScreenshots(true);
        }
    }
    
    @Override
    public void setDisableScreenshots(boolean disable) {
        nativeInstance.setDisableScreenshots(disable);
    }

    public void setThreadPriority(Thread t, int p) {
    }
    
    public int getDisplayWidth() {
        return nativeInstance.getDisplayWidth();
    }

    public int getDisplayHeight() {
        return nativeInstance.getDisplayHeight();
    }

    public int getActualDisplayHeight() {
        return nativeInstance.getDisplayHeight();
    }
    
    public static void displaySafeAreaChanged(final boolean revalidate) {
        if (!CN.isEdt()) {
            CN.callSerially(new Runnable() {
                public void run() {
                    displaySafeAreaChanged(revalidate);
                }
            });
            return;
        }
        Form f = CN.getCurrentForm();
        if (f != null) {
            f.setSafeAreaChanged();
            f.revalidateWithAnimationSafety();
        }
    }
    
    @Override
    public Rectangle getDisplaySafeArea(Rectangle rect) {
        if (rect == null) {
            rect = new Rectangle();
        }
        try {
            int x = nativeInstance.getDisplaySafeInsetLeft();
            int y = nativeInstance.getDisplaySafeInsetTop();
            int w = getDisplayWidth() - nativeInstance.getDisplaySafeInsetRight() - x;
            int h = getDisplayHeight() - nativeInstance.getDisplaySafeInsetBottom() - y;
            rect.setBounds(x, y, w, h);
        } catch (NullPointerException err) {
            Log.p("Invalid bounds in getDisplaySafeArea, if this message repeats frequently please let us know...");
        }
        
        return rect;
    }

    public boolean isNativeInputImmediate() {
        return true;
    }
    
    @Override
    protected int getDragAutoActivationThreshold() {
        return 1000000;
    }

    public boolean isNativeInputSupported() {
        return true;
    }

    public void exitApplication() {
        System.exit(0);
    }

    public boolean isTablet() {
        return isDesktop() || nativeInstance.isTablet();
    }

    @Override
    public boolean isDesktop() {
        return nativeInstance.isRunningOnMac();
    }

    @Override
    public boolean isWatch() {
        return nativeInstance.isRunningOnWatch();
    }

    @Override
    public boolean isTV() {
        return nativeInstance.isRunningOnTV();
    }

    @Override
    public void addCookie(Cookie c) {
        if(isUseNativeCookieStore()) {
            nativeInstance.addCookie(c.getName(), c.getValue(), c.getDomain(), c.getPath(), c.isSecure(), c.isHttpOnly(), c.getExpires());
        } else {
            super.addCookie(c);
        }
    }

    private static SuccessCallback<Image> screenshotCallback;
    @Override
    public void screenshot(final SuccessCallback<Image> callback) {
        if (callback == null) {
            return;
        }

        if (screenshotCallback != null) {
            Log.p("Screenshot request ignored: another capture is already in progress.");
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    callback.onSucess(null);
                }
            });
            return;
        }

        screenshotCallback = callback;
        try {
            forceScreenRenderForCapture();
            nativeInstance.screenshot();
        } catch (Throwable t) {
            screenshotCallback = null;
            Log.e(t);
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    callback.onSucess(null);
                }
            });
        }
    }

    /// On Mac Catalyst (desktop) the native capture reads pixels back from the
    /// Metal screenTexture (see cn1_copyMetalScreenTextureImage in IOSNative.m),
    /// which is the genuine on-screen render target. On the headless Catalyst
    /// window a static form's show() doesn't reliably re-drive a screen frame
    /// (no display-link present), so the screenTexture can still hold an earlier
    /// form. Animated screens flush continuously and are fine; static ones are
    /// not. Force the current form through the real EDT screen-render pipeline
    /// -- the same paintComponent-to-screen + flushGraphics that paintDirty()
    /// runs -- so the texture reflects the live UI before we capture it. This
    /// renders through the actual Metal draw path (not an off-screen re-paint),
    /// so the screenshot remains a genuine test of the display pipeline.
    private void forceScreenRenderForCapture() {
        if (!isDesktop()) {
            return;
        }
        final Runnable paintAndFlush = new Runnable() {
            @Override
            public void run() {
                Form f = Display.getInstance().getCurrent();
                if (f == null) {
                    return;
                }
                Graphics wrapper = getCodenameOneGraphics();
                wrapper.translate(-wrapper.getTranslateX(), -wrapper.getTranslateY());
                wrapper.resetAffine();
                wrapper.setClip(0, 0, getDisplayWidth(), getDisplayHeight());
                f.paintComponent(wrapper, true);
                flushGraphics();
            }
        };
        if (Display.getInstance().isEdt()) {
            paintAndFlush.run();
        } else {
            Display.getInstance().callSeriallyAndWait(paintAndFlush);
        }
    }

    static void onScreenshot(final byte[] imageData) {
        final SuccessCallback<Image> callback = screenshotCallback;
        screenshotCallback = null;
        if (callback == null) {
            return;
        }

        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                if (imageData != null && imageData.length > 0) {
                    try {
                        Image image = Image.createImage(imageData, 0, imageData.length);
                        if (image != null) {
                            if (image.getGraphics() == null) {
                                int width = Math.max(1, image.getWidth());
                                int height = Math.max(1, image.getHeight());
                                try {
                                    int[] rgb = image.getRGB();
                                    if (rgb != null && rgb.length >= width * height) {
                                        Image mutable = Image.createImage(rgb, width, height);
                                        if (mutable != null && mutable.getGraphics() != null) {
                                            image = mutable;
                                        }
                                    }
                                } catch (OutOfMemoryError oom) {
                                    Log.e(oom);
                                } catch (Throwable t) {
                                    Log.e(t);
                                }
                            }

                            if (image != null && image.getGraphics() != null) {
                                callback.onSucess(image);
                                return;
                            }
                        }
                    } catch (Throwable t) {
                        Log.e(t);
                    }
                }
                callback.onSucess(null);
            }
        });
    }

    /**
     * Used to enable/disable native cookies from native code.
     * @param cookiesArray 
     */
    static void setUseNativeCookiesNativeCallback(boolean useNative){
        instance.setUseNativeCookieStore(useNative);
    }
    
    static boolean isUseNativeCookiesNativeCallback(){
        return instance.isUseNativeCookieStore();
    }

    @Override
    public void clearNativeCookies() {
        nativeInstance.clearNativeCookies();
    }

    /**
     *
     * {@inheritDoc }
     */
    @Override
    public boolean isNativeCookieSharingSupported() {
        return true;
    }

    @Override
    public void addCookie(Cookie[] cookiesArray) {
        if(isUseNativeCookieStore()) {
            int len = cookiesArray.length;
            for(int i = 0 ; i < len ; i++){
                addCookie(cookiesArray[i]);
            }
        } else {
            super.addCookie(cookiesArray);
        }
    }

    @Override
    public Vector getCookiesForURL(String url) {
        if(isUseNativeCookieStore()) {
            Vector v = new Vector();
            nativeInstance.getCookiesForURL(url, v);
            return v;
        } 
        return super.getCookiesForURL(url);
    }

    public void setPlatformHint(String key, String value) {
        if ("platformHint.ios.useContentBasedRTLStringDetection".equals(key)) {
            useContentBasedRTLStringDetection = Boolean.parseBoolean(value);
        }
    }
    
    private boolean textEditorHidden;
    
    @Override
    public boolean isAsyncEditMode() {
        return nativeInstance.isAsyncEditMode();
    }
    
    // This is a bit of a hack to work around the fact that setScrollY() automatically
    // calls hideTextEditor when async editing is enabled.  Sometimes we want to
    // just scroll the text field into view and don't want this to happen.
    private int doNotHideTextEditorSemaphore=0;
    
    /**
     * A way to get the *actual* root content pane of a form without exposing Form.getActualPane().
     * @param f The form whose root pane we want.
     * @return The root pane of the form.  If there is no layered pane, then this should just
     * return the content pane.  Otherwise it may return the parent of the layered pane and content pane.
     */
    private static Container getRootPane(Form f) {
        Container root = f.getContentPane();
        Container parent = null;
        while ((parent = root.getParent()) != null && parent != f) {
            root = parent;
        }
        return root;
    }
    
    @Override
    public void hideTextEditor() {
        if (doNotHideTextEditorSemaphore > 0) {
            return;
        }
        if(textEditorHidden) {
            return;
        }
        Form current = getCurrentForm();
        if(nativeInstance.isAsyncEditMode() && current.isFormBottomPaddingEditingMode() && getRootPane(current).getUnselectedStyle().getPaddingBottom()> 0) {
            getRootPane(current).getUnselectedStyle().setPadding(Component.BOTTOM, 0);
            current.forceRevalidate();
        } 
        nativeInstance.hideTextEditing();
        textEditorHidden = true;
        repaintTextEditor(false);
    }

    private boolean pendingEditingText;
    @Override
    public boolean isEditingText(Component c) {
        if(textEditorHidden) {
            return false;
        }
        if (pendingEditingText) {
            return false;
        }
        //return c == currentEditing;
        return super.isEditingText(c);
    }

    @Override
    public boolean isEditingText() {
        /*if(textEditorHidden) {
            return false;
        }*/
        //return currentEditing != null;
        
        return super.isEditingText();
    }

    @Override
    public void stopTextEditing() {
        if (isAsyncEditMode()) {
            foldKeyboard();
        } else {
            if (currentEditing != null) {
                editingUpdate(currentEditing.getText(), currentEditing.getCursorPosition(), true);
                nativeInstance.foldVKB();
            }
        }
    }
    
    public static void foldKeyboard() {
        if(instance.isAsyncEditMode()) {
            Form f = Display.getInstance().getCurrent();
            
            final Component cmp = f == null ? null : f.getFocused();
            instance.callHideTextEditor();
            nativeInstance.foldVKB();

            // after folding the keyboard the screen layout might shift
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    if(cmp != null) {
                        Form f = Display.getInstance().getCurrent();
                        if(f == cmp.getComponentForm()) {
                            cmp.requestFocus();
                        }
                        if(nativeInstance.isAsyncEditMode() && f.isFormBottomPaddingEditingMode() && getRootPane(f).getUnselectedStyle().getPaddingBottom() > 0) {
                            getRootPane(f).getUnselectedStyle().setPadding(Component.BOTTOM, 0);
                            f.forceRevalidate();
                            return;
                        } 
                        
                        // revalidate even if we transition to a different form since the 
                        // spacing might have remained during the transition
                        f.revalidate();
                    }
                }
            });
        }
    }
    
    private void callHideTextEditor() {
        super.hideTextEditor();
    }
    
    /**
     * Invoked from native do not remove
     */
    static void showTextEditorAgain() {
        instance.textEditorHidden = false;
        instance.repaintTextEditor(true);
    }
    
    // A flag to override the invisible area under VKB.  This
    // is used when hiding the keyboard, but the keyboard may still
    // be visible so that we can perform revalidation of the form
    // using a supposed state.
    private int areaUnderVKBOverride=-1;
    
    @Override
    public int getInvisibleAreaUnderVKB() {
        if (areaUnderVKBOverride >= 0) {
            return areaUnderVKBOverride;
        }
        if(isAsyncEditMode()) {
            return nativeInstance.getVKBHeight();
        }
        return 0;
    }
    
    private static final String LAST_UPDATED_EDITOR_BOUNDS_KEY = "$$ios.updateNativeTextEditorFrame.lastUpdatedBounds";
    private static void updateNativeTextEditorFrame() {
        updateNativeTextEditorFrame(true);
    }
    private static void updateNativeTextEditorFrame(boolean requestFocus) {
        if (instance.currentEditing != null) {
            TextArea cmp = instance.currentEditing;
            Form form = cmp.getComponentForm();
            if (form == null || form != CN.getCurrentForm() ) {
                instance.stopTextEditing();
                return;
            }

            int x = cmp.getAbsoluteX() + cmp.getScrollX();
            int y = cmp.getAbsoluteY() + cmp.getScrollY();
            int w = cmp.getWidth();
            int h = cmp.getHeight();
            String key = LAST_UPDATED_EDITOR_BOUNDS_KEY;
            Rectangle lastUpdatedBounds = (Rectangle)cmp.getClientProperty(key);
            if (lastUpdatedBounds != null) {
                if (lastUpdatedBounds.getX() == x && lastUpdatedBounds.getY() == y && lastUpdatedBounds.getWidth() == w && lastUpdatedBounds.getHeight() == h) {
                    return;
                }
                lastUpdatedBounds.setBounds(x, y, w, h);
            } else {
                
                lastUpdatedBounds = new Rectangle(x, y, w, h);
                cmp.putClientProperty(key, lastUpdatedBounds);
            }
            
            
            final Style stl = cmp.getStyle();
            final boolean rtl = UIManager.getInstance().getLookAndFeel().isRTL();
            if (requestFocus) {
                instance.doNotHideTextEditorSemaphore++;
                try {
                    instance.currentEditing.requestFocus();
                } finally {
                    instance.doNotHideTextEditorSemaphore--;
                }
            }
            x = cmp.getAbsoluteX() + cmp.getScrollX();
            y = cmp.getAbsoluteY() + cmp.getScrollY();
            w = cmp.getWidth();
            h = cmp.getHeight();
            int pt = stl.getPaddingTop();
            int pb = stl.getPaddingBottom();
            int pl = stl.getPaddingLeft(rtl);
            int pr = stl.getPaddingRight(rtl);
            /*
            if(cmp.isSingleLineTextArea()) {
                switch(cmp.getVerticalAlignment()) {
                    case TextArea.CENTER:
                        if(h > cmp.getPreferredH()) {
                            y += (h / 2 - cmp.getPreferredH() / 2);
                        }
                        break;
                    case TextArea.BOTTOM:
                        if(h > cmp.getPreferredH()) {
                            y += (h - cmp.getPreferredH());
                        }
                        break;
                }
            }
            */
            Container contentPane = form.getContentPane();
            if (!contentPane.contains(cmp)) {
                contentPane = form;
            }
            Style contentPaneStyle = contentPane.getStyle();

            int minY = contentPane.getAbsoluteY() + contentPane.getScrollY() + contentPaneStyle.getPaddingTop();
            int maxH = Display.getInstance().getDisplayHeight() - minY - nativeInstance.getVKBHeight();
            
            if (y < minY) {
                h -= (minY - y);
                y = minY;
            }
            
            if (h > maxH ) {
                // For text areas, we don't want the keyboard to cover part of the 
                // typing region.  So we will try to size the component to 
                // to only go up to the top edge of the keyboard
                // that should allow the OS to enable scrolling properly.... at least
                // in theory.
                h = maxH;
            }
            
            if (h < 0) {
                // There isn't room for the editor at all.
                Log.p("No room for text editor.  h="+h);
                instance.stopTextEditing();
                return;
            }
            if (x < 0 || y < 0 || w <= 0 || h <= 0) {
                instance.stopTextEditing();
                return;
            }
            nativeInstance.resizeNativeTextView(x,
                    y,
                    w,
                    h,
                    pt,
                    pr,
                    pb,
                    pl
            );

        }
    }
    
    boolean keyboardShowing;
    
    /**
     * Callback for native.  Called when keyboard is shown.  Used for async editing 
     * with formBottomPaddingEditingMode.
     */
    static void keyboardWillBeShown(){
        instance.keyboardShowing = true;
        if(nativeInstance.isAsyncEditMode()) {
            // revalidate the parent since the size of form is now larger due to the vkb
            final Form current = Display.getInstance().getCurrent();
            //final Component currentEditingFinal = instance.currentEditing;
            if (current != null) {
                if(current.isFormBottomPaddingEditingMode()) {
                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            if (current != null) {
                                getRootPane(current).getUnselectedStyle().setPaddingUnit(new byte[] {Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS});
                                getRootPane(current).getUnselectedStyle().setPadding(Component.BOTTOM, nativeInstance.getVKBHeight());
                                current.revalidate();
                                Display.getInstance().callSerially(new Runnable() {
                                    public void run() {
                                        updateNativeTextEditorFrame();
                                    }
                                });
                            }
                        }
                    });
                } else {
                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            if (current != null) {
                                if (instance.currentEditing != null) {
                                    instance.doNotHideTextEditorSemaphore++;
                                    try {
                                        instance.currentEditing.requestFocus();
                                    } finally {
                                        instance.doNotHideTextEditorSemaphore--;
                                    }
                                    current.revalidate();
                                    Display.getInstance().callSerially(new Runnable() {
                                        public void run() {
                                            updateNativeTextEditorFrame();
                                        }
                                    });
                                }

                            }
                        }
                    });
                }
            }
        }
        
        Display.getInstance().fireVirtualKeyboardEvent(true);
    }
    
    /**
     * Callback for native.  Called when keyboard is hidden.  Used for async editing 
     * with formBottomPaddingEditingMode.
     */
    static void keyboardWillBeHidden(){
        instance.keyboardShowing = false;
        Display.getInstance().callSerially(new Runnable(){

            @Override
            public void run() {
                Form current = Display.getInstance().getCurrent();
                if (current != null) {
                    instance.areaUnderVKBOverride = 0;
                    try {
                        current.revalidate();
                        //Now that screen size is changed, the scroll positions may
                        // be caught in a negative state, leaving a gap at the
                        // top.
                        //https://github.com/codenameone/CodenameOne/issues/2476
                        Accessor.fixNegativeScrolls(current);
                    } finally {
                        instance.areaUnderVKBOverride = -1;
                    }
                }
            }
            
        });
        Display.getInstance().fireVirtualKeyboardEvent(false);
    }
    
    public void setCurrentForm(Form f) {
        if (isEditingText()) {
            stopTextEditing();
        }
        super.setCurrentForm(f);
        syncMacWindowAppearance(f);
        syncMacDesktopChrome(f);
        // Push the form title to the OS window for every desktop mode (unchanged from before); in
        // "custom" mode the title bar is hidden so this is invisible but harmless.
        if (isDesktop() && f != null && !(f instanceof Dialog)) {
            pushMacWindowTitle(f);
        }
    }

    @Override
    public boolean isNativeTitle() {
        // On Mac Catalyst, only the "native" desktop title-bar mode puts the form title into the OS
        // window title bar (and hides the CN1 Toolbar). In "custom" mode the visible Toolbar is the
        // title bar, so the OS title is not used. Opt-in only: defaults to toolbar (unchanged).
        return isDesktop() && "native".equals(getDesktopTitleBarMode());
    }

    // Tracks the last desktop title-bar mode pushed to the native window chrome so the (idempotent)
    // native call is only made when the mode actually changes.
    private String lastMacChromeMode;

    /// Applies the desktop title-bar mode to the host macOS window chrome: the {@code custom} mode
    /// undecorates the window so the CN1 Toolbar becomes the title bar. The {@code native} and
    /// {@code toolbar} modes leave the window chrome completely untouched, so existing Catalyst apps
    /// are byte-for-byte unaffected. No-op off the Mac desktop.
    private void syncMacDesktopChrome(Form f) {
        if (f == null || !isDesktop()) {
            return;
        }
        String mode = getDesktopTitleBarMode();
        if (mode.equals(lastMacChromeMode)) {
            return;
        }
        lastMacChromeMode = mode;
        // Only the "custom" mode touches the native window. Non-custom modes never call into the
        // native chrome, preserving the exact prior window appearance for existing apps.
        if ("custom".equals(mode)) {
            nativeInstance.setMacWindowUndecorated(true);
        }
    }

    @Override
    public String getDesktopTitleBarMode() {
        if (!isDesktop()) {
            return "toolbar";
        }
        // Opt-in via the desktop.titleBar build hint (codename1.arg.desktop.titleBar), surfaced as a
        // Display property by the generated iOS stub. Default toolbar = unchanged legacy behavior.
        return Display.getInstance().getProperty("desktop.titleBar", "toolbar");
    }

    @Override
    public void refreshNativeTitle() {
        Form f = getCurrentForm();
        if (f != null && isDesktop() && !(f instanceof Dialog)) {
            pushMacWindowTitle(f);
        }
    }

    private void pushMacWindowTitle(Form f) {
        String t = f.getTitle();
        nativeInstance.setWindowTitle(t == null ? "" : t);
    }

    // Commands currently exposed in the Mac native menu, index-aligned with the labels pushed to
    // native; fireMacMenuCommand(int) (invoked from the native menu action) resolves through this.
    private static List<com.codename1.ui.Command> macNativeCommands;

    @Override
    public void setNativeCommands(Vector commands) {
        if (!isDesktop()) {
            return;
        }
        ArrayList<com.codename1.ui.Command> filtered = new ArrayList<com.codename1.ui.Command>();
        // Encode one row per command as "<menuHint>\t<label>\t<shortcutKeyChar>\t<shortcutModifiers>",
        // rows separated by '\n'. The native side groups rows into the matching standard macOS menus
        // (App/File/Edit/View/Window/Help) or a top-level menu named by the hint; an empty hint means
        // the default commands menu. A non-zero shortcutKeyChar produces a UIKeyCommand so the menu
        // item shows (and responds to) the keyboard accelerator.
        StringBuilder sb = new StringBuilder();
        if (commands != null) {
            for (int i = 0; i < commands.size(); i++) {
                Object o = commands.elementAt(i);
                if (!(o instanceof com.codename1.ui.Command)) {
                    continue;
                }
                com.codename1.ui.Command c = (com.codename1.ui.Command) o;
                String name = c.getCommandName();
                if (name == null || name.length() == 0) {
                    continue;
                }
                String hint = c.getDesktopMenu();
                if (hint == null) {
                    hint = "";
                }
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(hint).append('\t').append(name).append('\t')
                        .append(c.getDesktopShortcutKeyChar()).append('\t')
                        .append(c.getDesktopShortcutModifiers());
                filtered.add(c);
            }
        }
        macNativeCommands = filtered;
        nativeInstance.setNativeMenuCommands(sb.toString());
    }

    /**
     * Invoked from the native Mac menu action when the user selects the command at the given
     * index. Dispatches the corresponding Codename One command on the EDT.
     */
    public static void fireMacMenuCommand(final int index) {
        final List<com.codename1.ui.Command> cmds = macNativeCommands;
        if (cmds == null || index < 0 || index >= cmds.size()) {
            return;
        }
        final com.codename1.ui.Command c = cmds.get(index);
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                c.actionPerformed(new ActionEvent(c));
            }
        });
    }

    private Boolean lastMacWindowDark;
    private void syncMacWindowAppearance(Form f) {
        if (f == null || !isDesktop()) return;
        int bg = f.getContentPane().getStyle().getBgColor();
        int r = (bg >> 16) & 0xff;
        int g = (bg >> 8) & 0xff;
        int b = bg & 0xff;
        int luma = (r * 299 + g * 587 + b * 114) / 1000;
        boolean dark = luma < 128;
        if (lastMacWindowDark != null && lastMacWindowDark.booleanValue() == dark) return;
        lastMacWindowDark = Boolean.valueOf(dark);
        nativeInstance.setMacWindowDarkAppearance(dark);
    }

    @Override
    public void afterComponentPaint(Component c, Graphics g) {
        super.afterComponentPaint(c, g);
        if (isEditingText(c)) {
            updateNativeTextEditorFrame(false);
        }
    }
    
    
    
    private static final Object EDITING_LOCK = new Object(); 
    private static boolean editNext;
    public void editString(final Component cmp, final int maxSize, final int constraint, final String text, final int i) {
        
        // The very first time we try to edit a string, let's determine if the 
        // system default is to do async editing.  If the system default
        // is not yet set, we set it here, and it will be used as the default from now on
        //  We do this because the nativeInstance.isAsyncEditMode() value changes
        // to reflect the currently edited field so it isn't a good way to keep a
        // system default.
        pendingEditingText = false;
        String defaultAsyncEditingSetting = Display.getInstance().getProperty("ios.VKBAlwaysOpen", null);
        if (defaultAsyncEditingSetting == null) {
            defaultAsyncEditingSetting = nativeInstance.isAsyncEditMode() ? "true" : "false";
            Display.getInstance().setProperty("ios.VKBAlwaysOpen", defaultAsyncEditingSetting);
            
        }
        boolean asyncEdit = "true".equals(defaultAsyncEditingSetting) ? true : false;
        //Log.p("Application default for async editing is "+asyncEdit);
        
        try {
            if (currentEditing != cmp && currentEditing != null && currentEditing instanceof TextArea) {
                Display.getInstance().onEditingComplete(currentEditing, ((TextArea)currentEditing).getText());
                currentEditing = null;
                callHideTextEditor();
                if (nativeInstance.isAsyncEditMode()) {
                    nativeInstance.setNativeEditingComponentVisible(false);
                }
                synchronized(EDITING_LOCK) {
                    EDITING_LOCK.notify();
                }
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        pendingEditingText = true;
                        Display.getInstance().editString(cmp, maxSize, constraint, text, i);
                    }
                });
                return;
            }
            
           if(cmp.isFocusable() && !cmp.hasFocus()) {
                doNotHideTextEditorSemaphore++;
                try {
                    cmp.requestFocus();
                } finally {
                    doNotHideTextEditorSemaphore--;
                }
                
                // Notice here that we are checking isAsyncEditMode() which looks
                // at the previously edited text area.  Not the async mode
                // of our upcoming field.
                if(isAsyncEditMode()) {
                    // flush the EDT so the focus will work...

                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            pendingEditingText = true;
                            Display.getInstance().editString(cmp, maxSize, constraint, text, i);
                        }
                    });
                    return;
                }
            }
            
           // Check if the form has any setting for asyncEditing that should override
           // the application defaults.
            Form parentForm = cmp.getComponentForm();
            if (parentForm == null) {
                //Log.p("Attempt to edit text area that is not on a form.  This is not supported");
                return;
            }
            if (parentForm.getClientProperty("asyncEditing") != null) {
                Object async = parentForm.getClientProperty("asyncEditing");
                if (async instanceof Boolean) {
                    asyncEdit = ((Boolean)async).booleanValue();
                    //Log.p("Form overriding asyncEdit due to asyncEditing client property: "+asyncEdit);
                }
            }
            
            if (parentForm.getClientProperty("ios.asyncEditing") != null) {
                Object async = parentForm.getClientProperty("ios.asyncEditing");
                if (async instanceof Boolean) {
                    asyncEdit = ((Boolean)async).booleanValue();
                    //Log.p("Form overriding asyncEdit due to ios.asyncEditing client property: "+asyncEdit);
                }
                
            }
            
            // If the system default is to use async editing, we need to check
            // the form to make sure that it is scrollable.  If it is not 
            // scrollable, then this field should default to Non-async
            // editing - and should instead revert to legacy editing mode.
            if(asyncEdit && !parentForm.isFormBottomPaddingEditingMode()) {
                Container p = cmp.getParent();
                
                // A crude estimate of how far the component needs to be able to scroll to make 
                // async editing viable.  We start with half-way down the screen.
                int keyboardClippingThresholdY = Display.getInstance().getDisplayWidth() / 2;
                while(p != null) {
                    if(Accessor.scrollableYFlag(p)  && p.getAbsoluteY() < keyboardClippingThresholdY) {
                        break;
                    }
                    p = p.getParent();
                }
                // no scrollabel parent automatically configure the text field for legacy mode
                //nativeInstance.setAsyncEditMode(p != null);
                asyncEdit = p != null;
                //Log.p("Overriding asyncEdit due to form scrollability: "+asyncEdit);
                
            } else if (parentForm.isFormBottomPaddingEditingMode()){
                // If form uses bottom padding mode, then we will always
                // use async edit (unless the field explicitly overrides it).
                asyncEdit = true;
                //Log.p("Overriding asyncEdit due to form bottom padding edit mode: "+asyncEdit);
            }

            
            // If the field itself explicitly sets async editing behaviour
            // then this will override all other settings.
            if (cmp.getClientProperty("asyncEditing") != null) {
                Object async = cmp.getClientProperty("asyncEditing");
                if (async instanceof Boolean) {
                    asyncEdit = ((Boolean)async).booleanValue();
                    //Log.p("Overriding asyncEdit due to field asyncEditing client property: "+asyncEdit);
                }
            }
            
            if (cmp.getClientProperty("ios.asyncEditing") != null) {
                Object async = cmp.getClientProperty("ios.asyncEditing");
                if (async instanceof Boolean) {
                    asyncEdit = ((Boolean)async).booleanValue();
                    //Log.p("Overriding asyncEdit due to field ios.asyncEditing client property: "+asyncEdit);
                }
                
            }
            
            // Finally we set the async edit mode for this field.
            //System.out.println("Async edit mode is "+asyncEdit);
            nativeInstance.setAsyncEditMode(asyncEdit);
            
            textEditorHidden = false;
            currentEditing = (TextArea)cmp;

            //register the edited TextArea to support moving to the next field
            TextEditUtil.setCurrentEditComponent(cmp); 

            final NativeFont fnt = f(cmp.getStyle().getFont().getNativeFont());
            boolean forceSlideUpTmp = false;
            final Form current = Display.getInstance().getCurrent();
            if(current instanceof Dialog && !isTablet()) {
                // special case, if we are editing a small dialog we want to move it
                // so the bottom of the dialog shows within the screen. This is
                // described in issue 505
                Dialog dlg = (Dialog)current;
                Component c = dlg.getDialogComponent();
                if(c.getHeight() < Display.getInstance().getDisplayHeight() / 2 && 
                        c.getAbsoluteY() + c.getHeight() > Display.getInstance().getDisplayHeight() / 2) {
                    forceSlideUpTmp = true;
                }
            }
            final boolean forceSlideUp = forceSlideUpTmp;
            
            cmp.repaint();
            // give the repaint one cycle to "do its magic...
            final Style stl = currentEditing.getStyle();
            final boolean rtl = UIManager.getInstance().getLookAndFeel().isRTL();
            final Style hintStyle = currentEditing.getHintLabel() != null ? currentEditing.getHintLabel().getStyle() : stl;
            
            if (current != null) {
                Component nextComponent = current.getNextComponent(cmp);
                TextEditUtil.setNextEditComponent(nextComponent);
            }
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    int x = cmp.getAbsoluteX() + cmp.getScrollX();
                    int y = cmp.getAbsoluteY() + cmp.getScrollY();
                    int w = cmp.getWidth();
                    int h = cmp.getHeight();
                    int pt = stl.getPaddingTop();
                    int pb = stl.getPaddingBottom();
                    int pl = stl.getPaddingLeft(rtl);
                    int pr = stl.getPaddingRight(rtl);
                    /*
                    if(currentEditing != null && currentEditing.isSingleLineTextArea()) {
                        switch(currentEditing.getVerticalAlignment()) {
                            case TextArea.CENTER:
                                if(h > cmp.getPreferredH()) {
                                    y += (h / 2 - cmp.getPreferredH() / 2);
                                }
                                break;
                            case TextArea.BOTTOM:
                                if(h > cmp.getPreferredH()) {
                                    y += (h - cmp.getPreferredH());
                                }
                                break;
                        }
                    }
                    */
                    String hint = null;
                    if(currentEditing != null && currentEditing.getUIManager().isThemeConstant("nativeHintBool", true) && currentEditing.getHint() != null) {
                        hint = currentEditing.getHint();
                    }
                    int hintColor = hintStyle.getFgColor();
                    
                    if(isAsyncEditMode()) {
                        // request focus triggers a scroll which flicks the textEditorHidden flag
                        doNotHideTextEditorSemaphore++;
                        try {
                            cmp.requestFocus();
                        } finally {
                            doNotHideTextEditorSemaphore--;
                        }
                        textEditorHidden = false;
                    }
                    boolean showToolbar = cmp.getClientProperty("iosHideToolbar") == null;
                    if(showToolbar && Display.getInstance().getProperty("iosHideToolbar", "false").equalsIgnoreCase("true")) {
                        showToolbar = false;
                    }
                    if ( currentEditing != null ){
                        int align = currentEditing.getStyle().getAlignment();
                        // iosReturnExitsEditing: opt-in client property that makes Return on a
                        // multi-line TextArea exit editing (firing the Done listener) instead of
                        // inserting a newline -- mirrors iOS Reminders task-title behavior.
                        boolean returnExitsEditing = Boolean.TRUE.equals(cmp.getClientProperty("iosReturnExitsEditing"))
                                && !currentEditing.isSingleLineTextArea();
                        nativeInstance.editStringAt(x,
                                y,
                                w,
                                h,
                                fnt.peer, currentEditing.isSingleLineTextArea(),
                                currentEditing.getRows(), maxSize, constraint, text, forceSlideUp,
                                stl.getFgColor(), 0,//peer,
                                pt,
                                pb,
                                pl,
                                pr,
                                hint,
                                hintColor,
                                showToolbar,
                                Boolean.TRUE.equals(cmp.getClientProperty("blockCopyPaste")),
                                DefaultLookAndFeel.reverseAlignForBidi(cmp, align),
                                currentEditing.getVerticalAlignment(),
                                returnExitsEditing);
                    }
                }
            });
            if(isAsyncEditMode()) {
                return;
            }
            editNext = false;
            
            Display.getInstance().invokeAndBlock(new Runnable() {
                @Override
                public void run() {
                    synchronized(EDITING_LOCK) {
                        while(instance.currentEditing == cmp) {
                            try {
                                EDITING_LOCK.wait(20);
                            } catch (InterruptedException ex) {
                            }
                        }
                    }
                }
            });
            
            if(cmp instanceof TextArea && !((TextArea)cmp).isSingleLineTextArea()) {
                Form form = cmp.getComponentForm();
                if (form != null) {
                    form.revalidate();
                }
            }
            if(editNext) {
                editNext = false;
                TextEditUtil.editNextTextArea();
            }
        } finally {
            
        }
    }
    
    // Callback for native code
    public static void resizeNativeTextComponentCallback() {
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                updateNativeTextEditorFrame();
            }
        });
    }
    
    
    // callback for native code!
    public static void editingUpdate(final String s, final int cursorPositon, final boolean finished) {
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                if(instance.currentEditing != null) {
                    if(finished) {
                        editNext = cursorPositon == -2;
                        synchronized(EDITING_LOCK) {
                            instance.currentEditing.setText(s);
                            Display.getInstance().onEditingComplete(instance.currentEditing, s);
                            if(editNext && instance.currentEditing != null && instance.currentEditing instanceof TextArea) {
                                ((TextArea)instance.currentEditing).fireDoneEvent();
                            }
                            Component cmp = instance.currentEditing;
                            instance.currentEditing = null;
                            instance.callHideTextEditor();
                            if (nativeInstance.isAsyncEditMode()) {
                                nativeInstance.setNativeEditingComponentVisible(false);
                            }
                            if(cmp != null) {
                                cmp.putClientProperty(LAST_UPDATED_EDITOR_BOUNDS_KEY, null);
                            }
                            EDITING_LOCK.notify();
                        }
                        Form current = Display.getInstance().getCurrent();
                        if (current != null && current.isFormBottomPaddingEditingMode()) {
                            getRootPane(current).getUnselectedStyle().setPadding(Component.BOTTOM, 0);
                        }
                    } else {
                        instance.currentEditing.setText(s);
                    }
                    if(instance.currentEditing instanceof TextField && cursorPositon > -1) {
                        ((TextField)instance.currentEditing).setCursorPosition(cursorPositon);
                    }
                } else {
                    System.out.println("Editing null component!!" + s);
                }
            }
        });
        
    }

    @Override
    public void updateNativeEditorText(Component c, String text) {
        if (isEditingText(c)) {
            nativeInstance.updateNativeEditorText(text);
        }
    }

    @Override
    public boolean nativeEditorPaintsHint() {
        return true;
    }

    public void releaseImage(Object image) {
        if(image instanceof NativeImage) {
            ((NativeImage)image).deleteImage();
        }
    }
    
    @Override
    public boolean paintNativePeersBehind() {
        return true;
    }
    
    static boolean isPaintPeersBehindEnabled() {
        return instance.paintNativePeersBehind();
    }
    
    /**
     * Checks to see if a given coordinate is contained by a CN1 light-weight component.
     * This is used by native code to determine if a touch event should be passed through
     * to the peer component layer. (TRUE = don't pass to native peer layer, FALSE - do pass to native peer layer)
     * @param x x-coordinate to test (screen coordinates)
     * @param y y-coordinate to test (screen coordinates)
     * @return true if events should be handed by CN1 and not passed to the native layer.
     */
    static boolean hitTest(int x, int y) {
        Form f = Display.getInstance().getCurrent();
        if (f != null) {
            Component cmp = f.getResponderAt(x, y);
            if (cmp == null || !(cmp instanceof PeerComponent)) {
                return true;
            }
            return Sheet.isSheetVisibleAt(x, y);
        }
        return true;
    }
    
    public void flushGraphics(int x, int y, int width, int height) {
        globalGraphics.clipApplied = false;
        flushBuffer(0, x, y, width, height);
        if (isDesktop()) {
            // Form-show isn't the only path that changes dark mode -- a theme
            // refresh or a system appearance toggle re-styles the contentPane
            // without dropping a new Form on the EDT. Re-check after every
            // flush so the host NSWindow titlebar tracks the live form.
            // syncMacWindowAppearance is no-op when the state hasn't changed.
            syncMacWindowAppearance(Display.getInstance().getCurrent());
        }
    }

    private final static int[] singleDimensionX = new int[1];
    private final static int[] singleDimensionY = new int[1];
    public static void pointerPressedCallback(int x, int y) {
        if(dropEvents) {
            return;
        }
        singleDimensionX[0] = x; singleDimensionY[0] = y;
        instance.pointerPressed(singleDimensionX, singleDimensionY);
    }
    public static void pointerReleasedCallback(int x, int y) {
        if(dropEvents) {
            return;
        }
        singleDimensionX[0] = x; singleDimensionY[0] = y;
        instance.pointerReleased(singleDimensionX, singleDimensionY);
    }
    public static void pointerDraggedCallback(int x, int y) {
        if(dropEvents) {
            return;
        }
        singleDimensionX[0] = x; singleDimensionY[0] = y;
        instance.pointerDragged(singleDimensionX, singleDimensionY);
    }
    
    protected void pointerPressed(final int[] x, final int[] y) {
        super.pointerPressed(x, y);
    }

    protected void pointerReleased(final int[] x, final int[] y) {
        super.pointerReleased(x, y);
    }

    protected void pointerDragged(final int[] x, final int[] y) {
        if(dropEvents) {
            return;
        }
        super.pointerDragged(x, y);
    }

    // Sentinel keycodes forwarded from the native iOS hardware-keyboard handler
    // for non-printable keys. Values match Android's DROID_IMPL_KEY_* sentinels
    // so apps can write platform-agnostic key handlers.
    static final int IOS_IMPL_KEY_LEFT = -23446;
    static final int IOS_IMPL_KEY_RIGHT = -23447;
    static final int IOS_IMPL_KEY_UP = -23448;
    static final int IOS_IMPL_KEY_DOWN = -23449;
    static final int IOS_IMPL_KEY_FIRE = -23450;
    static final int IOS_IMPL_KEY_BACKSPACE = -23453;
    static final int IOS_IMPL_KEY_ENTER = -23460;
    static final int IOS_IMPL_KEY_TAB = -23461;
    static final int IOS_IMPL_KEY_ESCAPE = -23462;
    static final int IOS_IMPL_KEY_HOME = -23463;
    static final int IOS_IMPL_KEY_END = -23464;
    static final int IOS_IMPL_KEY_PAGE_UP = -23465;
    static final int IOS_IMPL_KEY_PAGE_DOWN = -23466;
    static final int IOS_IMPL_KEY_INSERT = -23467;
    static final int IOS_IMPL_KEY_FORWARD_DEL = -23468;
    static final int IOS_IMPL_KEY_F1 = -23469;
    static final int IOS_IMPL_KEY_F2 = -23470;
    static final int IOS_IMPL_KEY_F3 = -23471;
    static final int IOS_IMPL_KEY_F4 = -23472;
    static final int IOS_IMPL_KEY_F5 = -23473;
    static final int IOS_IMPL_KEY_F6 = -23474;
    static final int IOS_IMPL_KEY_F7 = -23475;
    static final int IOS_IMPL_KEY_F8 = -23476;
    static final int IOS_IMPL_KEY_F9 = -23477;
    static final int IOS_IMPL_KEY_F10 = -23478;
    static final int IOS_IMPL_KEY_F11 = -23479;
    static final int IOS_IMPL_KEY_F12 = -23480;

    public static void keyPressedCallback(int keyCode) {
        if (dropEvents) {
            return;
        }
        Display.getInstance().keyPressed(keyCode);
    }

    public static void keyReleasedCallback(int keyCode) {
        if (dropEvents) {
            return;
        }
        Display.getInstance().keyReleased(keyCode);
    }

    public static void pointerHoverPressedCallback(int x, int y) {
        if (dropEvents) {
            return;
        }
        singleDimensionX[0] = x; singleDimensionY[0] = y;
        instance.pointerHoverPressed(singleDimensionX, singleDimensionY);
    }

    public static void pointerHoverCallback(int x, int y) {
        if (dropEvents) {
            return;
        }
        singleDimensionX[0] = x; singleDimensionY[0] = y;
        instance.pointerHover(singleDimensionX, singleDimensionY);
    }

    public static void pointerHoverReleasedCallback(int x, int y) {
        if (dropEvents) {
            return;
        }
        singleDimensionX[0] = x; singleDimensionY[0] = y;
        instance.pointerHoverReleased(singleDimensionX, singleDimensionY);
    }

    protected void pointerHover(final int[] x, final int[] y) {
        super.pointerHover(x, y);
    }

    protected void pointerHoverPressed(final int[] x, final int[] y) {
        super.pointerHoverPressed(x, y);
    }

    protected void pointerHoverReleased(final int[] x, final int[] y) {
        super.pointerHoverReleased(x, y);
    }

    static void sizeChangedImpl(int w, int h) {
        instance.sizeChanged(w, h);
    }

    @Override
    public Boolean isDarkMode() {
        if(nativeInstance.isDarkModeDetectionSupported()) {
            return nativeInstance.isDarkMode();
        }
        return null;
    }

    @Override
    public boolean isVPNDetectionSupported() {
        return true;
    }

    @Override
    public boolean isVPNActive() {
        return nativeInstance.isVPNActive();
    }

    @Override
    protected com.codename1.io.wifi.WifiPlatform createWifiPlatform() {
        return new IOSWifiPlatform();
    }

    @Override
    protected com.codename1.io.bonjour.BonjourPlatform createBonjourPlatform() {
        return new IOSBonjourPlatform();
    }

    @Override
    protected com.codename1.io.NetworkTypePlatform createNetworkTypePlatform() {
        return new IOSNetworkTypePlatform();
    }

    @Override
    public boolean isLargerTextEnabled() {
        return nativeInstance.isLargerTextEnabled();
    }

    @Override
    public float getLargerTextScale() {
        return nativeInstance.getLargerTextScale();
    }
    

    public void flushGraphics() {
        flushGraphics(0, 0, getDisplayWidth(), getDisplayHeight());
    }

    private static void flushBuffer(long peer, int x, int y, int width, int height) {
        nativeInstance.flushBuffer(peer, x, y, width, height);
    }

    public void getRGB(Object nativeImage, int[] arr, int offset, int x, int y, int width, int height) {
        if(offset != 0) {
            int[] newArr = new int[arr.length - offset];
            System.arraycopy(arr, offset, newArr, 0, newArr.length);
            arr = newArr;
            offset = 0;
        }
        NativeImage nimg = (NativeImage)nativeImage;
        if(nimg.scaled) {
            Object mute = createMutableImage(nimg.width, nimg.height, 0);
            Object graph = getNativeGraphics(mute);
            drawImage(graph, nimg, 0, 0);
            nimg = (NativeImage)mute;
        }
        imageRgbToIntArray(nimg.peer, arr, x, y, width, height, nimg.width, nimg.height);
    }

    private void imageRgbToIntArray(long imagePeer, int[] arr, int x, int y, int width, int height, int imgWidth, int imgHeight) {
        nativeInstance.imageRgbToIntArray(imagePeer, arr, x, y, width, height, imgWidth, imgHeight);
    }

    private long createImageFromARGB(int[] argb, int width, int height) {
        return nativeInstance.createImageFromARGB(argb, width, height);
    }

    public Object createImage(int[] rgb, int width, int height) {
        NativeImage n = new NativeImage("Image created from ARGB array: " + rgb.length + " width " + width + " height " + height);
        n.peer = createImageFromARGB(rgb, width, height);
        n.width = width;
        n.height = height;
        return n;
    }

    private static final int[] widthHeight = new int[2];
    public Object createImage(String path) throws IOException {
        long ns;
        if(path.startsWith("file:")) {
            ns = IOSImplementation.nativeInstance.createNSData(unfile(path));
        } else {
            ns = getResourceNSData(path);
        }
        NativeImage n = new NativeImage(path);
        n.peer = nativeInstance.createImageNSData(ns, widthHeight);
        n.width = widthHeight[0];
        n.height = widthHeight[1];
        nativeInstance.releasePeer(ns);
        return n;
    }

    public boolean hasNativeTheme() {
        return true;
    }

    private static String iosMode = "auto";
    
    public static void setIosMode(String l) {
        iosMode = l;
    }
    
    private static boolean waitForAnimationLock(Form f) {
        while (!f.grabAnimationLock()) {
            Display.getInstance().invokeAndBlock(new Runnable() {
                public void run() {
                    Util.sleep(20);
                }
            });
        }
        boolean obtained =  Display.getInstance().getCurrent() == f;
        if (!obtained) {
            f.releaseAnimationLock();
        }
        return obtained;
    }
    
    
    
    /**
     * Installs the native theme, this is only applicable if hasNativeTheme() returned true. Notice that this method
     * might replace the DefaultLookAndFeel instance and the default transitions.
     */
    public void installNativeTheme() {
        try {
            Resources r;
            String mode = iosMode == null ? "auto" : iosMode.toLowerCase();
            // Modern (liquid-glass) theme is opt-in via ios.themeMode=modern /
            // liquid / material. Keep the default ("auto" or unset) on the
            // legacy iOS 7 / pre-flat theme so existing apps and screenshot
            // goldens aren't disturbed. Apps that want the new look set
            // ios.themeMode=modern in their build hints or via
            // Display.setProperty("ios.themeMode", "modern") before the
            // first Form is shown.
            if(mode.equals("modern") || mode.equals("liquid")) {
                InputStream in = getResourceAsStream("/iOSModernTheme.res");
                if (in != null) {
                    r = Resources.open(in);
                    Hashtable tp = r.getTheme(r.getThemeResourceNames()[0]);
                    injectDesktopThemeConstants(tp);
                    UIManager.getInstance().setThemeProps(tp);
                    return;
                }
                // Modern theme isn't in the jar (e.g. framework build hasn't
                // generated it yet) - fall back to iOS 7 so the app still boots.
            }
            if(mode.equals("ios7") || mode.equals("flat") || mode.equals("auto") || mode.equals("modern") || mode.equals("liquid")) {
                r = Resources.open("/iOS7Theme.res");
                Hashtable tp = r.getTheme(r.getThemeResourceNames()[0]);
                if(!nativeInstance.isIOS7()) {
                    tp.put("TitleArea.padding", "0,0,0,0");
                }
                injectDesktopThemeConstants(tp);
                UIManager.getInstance().setThemeProps(tp);
                return;
            }
            // "legacy" / "iphone" / anything else: pre-flat iPhone theme.
            r = Resources.open("/iPhoneTheme.res");
            Hashtable tp = r.getTheme(r.getThemeResourceNames()[0]);
            injectDesktopThemeConstants(tp);
            UIManager.getInstance().setThemeProps(tp);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * On Mac Catalyst (isDesktop()) the app should feel like a desktop app: enable the
     * cross-platform interactive scrollbars and the native window chrome (OS title bar +
     * native menu bar). These theme constants are injected only on the desktop so iOS
     * phones/tablets are unaffected. Mirrors JavaSEPort.injectDesktopThemeConstants.
     */
    private void injectDesktopThemeConstants(Hashtable tp) {
        if (tp == null || !isDesktop()) {
            return;
        }
        // Opt-in via the desktop.interactiveScrollbars build hint; default off so existing Catalyst
        // apps render scrollbars exactly as before.
        if ("true".equalsIgnoreCase(Display.getInstance().getProperty("desktop.interactiveScrollbars", "false"))) {
            tp.put("@interactiveScrollBool", "true");
        }
    }

    private InputStream getResourceAsStream(String name) {
        return IOSImplementation.class.getResourceAsStream(name);
    }

    private long getNSData(InputStream i) {
        if(i instanceof BufferedInputStream) {
            InputStream inp = ((BufferedInputStream)i).getInternal();
            return getNSData(inp);
        }
        if(i instanceof NSDataInputStream) {
            return ((NSDataInputStream)i).getNSData();
        }
        return 0;
    }
    
    private byte[] toByteArray(InputStream i) throws IOException {
        if(i instanceof BufferedInputStream) {
            InputStream inp = ((BufferedInputStream)i).getInternal();
            if(inp instanceof NSDataInputStream) {
                return ((NSDataInputStream)inp).getArray();
            }
        }
        return Util.readInputStream(i);
    }
    
    public Object createImage(InputStream i) throws IOException {
        long ns = getNSData(i);
        if(ns > 0) {
            int[] wh = widthHeight;
            NativeImage n = new NativeImage("Image created from stream");
            n.peer = nativeInstance.createImageNSData(ns, wh);
            n.width = wh[0];
            n.height = wh[1];
            Util.cleanup(i);
            return n;
        }
        byte[] buffer = toByteArray(i);
        return createImage(buffer, 0, buffer.length);
    }

    public Object createMutableImage(int width, int height, int fillColor) {
        long peer = createNativeMutableImage(width, height, fillColor);
        NativeImage n = new NativeImage("Mutable image of width " + width + " height " + height + " fillColor " + fillColor);
        n.peer = peer;
        n.width = width;
        n.height = height;
        return n;
    }

    @Override
    public boolean isGaussianBlurSupported() {
        return true;
    }

    @Override
    public Image gaussianBlurImage(Image image, float radius) {
        NativeImage im = (NativeImage)image.getImage();
        NativeImage n = new NativeImage("blurred:" + im.debugText );
        n.width = im.width;
        n.height = im.height;
        n.peer = nativeInstance.gausianBlurImage(im.peer, radius);
        return Image.createImage(n);
    }

    /// Parses a theme-constant string as an int, returning {@code def} on null/blank/malformed.
    private static int parseIntConstant(String v, int def) {
        if (v == null) {
            return def;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException nfe) {
            return def;
        }
    }

    @Override
    public Image createSFSymbolImage(String name, int color, float sizePixels, int weight) {
        // wh[0],[1] receive the rendered pixel w/h. wh[2],[3] pass optional layout
        // tuning to the native render: a uniform icon SLOT height (percent of size)
        // and the glyph's VERTICAL bias in that slot (percent; 50 = centred). This
        // lets a native-style tab bar give a tall glyph (e.g. star.fill) a full-height
        // slot positioned like UIKit's SF baseline instead of shrinking it to the
        // nominal size. Defaults 100/50 reproduce the legacy centred behaviour, so
        // non-tab icons are unaffected unless the theme opts in.
        int[] wh = new int[4];
        com.codename1.ui.plaf.UIManager uim = com.codename1.ui.plaf.UIManager.getInstance();
        wh[2] = parseIntConstant(uim.getThemeConstant("iosSFSlotPct", "100"), 100);
        wh[3] = parseIntConstant(uim.getThemeConstant("iosSFVBias", "50"), 50);
        long peer = nativeInstance.nativeCreateSFSymbol(name, color, sizePixels, weight, wh);
        if (peer == 0) {
            return null;
        }
        NativeImage n = new NativeImage("SF Symbol " + name);
        n.peer = peer;
        n.width = wh[0];
        n.height = wh[1];
        return Image.createImage(n);
    }

    @Override
    public boolean blurRegion(Object graphics, int x, int y, int width, int height, float radius) {
        if (radius <= 0f || width <= 0 || height <= 0) {
            return true;
        }
        NativeGraphics ng = (NativeGraphics) graphics;
        // Live screen (no backing mutable image): enqueue a BlurRegion op in paint
        // order. During the drain it blurs the already-drawn screenTexture region
        // (the backdrop) and draws it back, so the component's translucent fill +
        // foreground (queued right after this returns) paint on top -- real
        // "Liquid Glass" on a running app, not just the offscreen fidelity tiles.
        if (ng.associatedImage == null) {
            nativeInstance.nativeBlurScreenRegion(x, y, width, height, radius);
            return true;
        }
        // Flush whatever has been painted into the image so its peer is current, read
        // the region behind us, Gaussian-blur it (Metal-backed CIGaussianBlur) and draw
        // the blurred patch back where it was read.
        ng.checkControl();
        ng.associatedImage.peer = finishDrawingOnImage();
        currentlyDrawingOn = null;
        NativeImage target = ng.associatedImage;
        int rx = Math.max(0, x), ry = Math.max(0, y);
        int rw = Math.min(width, target.width - rx), rh = Math.min(height, target.height - ry);
        if (rw <= 0 || rh <= 0) {
            return true;
        }
        int[] rgb = new int[rw * rh];
        getRGB(target, rgb, 0, rx, ry, rw, rh);
        // UIKit "Liquid Glass" doesn't just blur the backdrop -- it boosts the
        // backdrop's saturation (vibrancy) so colours pop through the frost. A plain
        // CIGaussianBlur leaves the glass washed-out vs the native material; lift
        // saturation here (this is the backdrop-filter path only -- blurRegion is
        // never invoked for a plain filter:blur) before blurring.
        saturateInPlace(rgb, GLASS_SATURATION);
        NativeImage blurred = new NativeImage("backdrop-filter blur");
        blurred.peer = nativeInstance.gausianBlurImage(createImageFromARGB(rgb, rw, rh), radius);
        blurred.width = rw;
        blurred.height = rh;
        // drawImage applies this graphics' transform; pass coordinates relative to that
        // transform's translation so the blurred patch lands back where we read it.
        int tx = (int) Math.round(ng.transform.getTranslateX());
        int ty = (int) Math.round(ng.transform.getTranslateY());
        drawImage(ng, blurred, rx - tx, ry - ty);
        return true;
    }

    @Override
    public boolean glassRegion(Object graphics, int x, int y, int width, int height, float radius, float cornerRadius, float sat, float scale, float offset, float refract, float specular) {
        if (radius <= 0f || width <= 0 || height <= 0) {
            return true;
        }
        NativeGraphics ng = (NativeGraphics) graphics;
        // Live screen path: the full colour-transform material is a follow-up; for
        // now fall back to a plain in-place blur of the running screen region.
        if (ng.associatedImage == null) {
            return blurRegion(graphics, x, y, width, height, radius);
        }
        // Flush whatever has been painted into the image so its peer is current, read
        // the region behind us, apply the "Liquid Glass" affine colour material and
        // Gaussian-blur it (Metal-backed CIGaussianBlur) and draw the patch back where
        // it was read.
        ng.checkControl();
        ng.associatedImage.peer = finishDrawingOnImage();
        currentlyDrawingOn = null;
        NativeImage target = ng.associatedImage;
        int rx = Math.max(0, x), ry = Math.max(0, y);
        int rw = Math.min(width, target.width - rx), rh = Math.min(height, target.height - ry);
        if (rw <= 0 || rh <= 0) {
            return true;
        }
        // Build a buffer PADDED by the full blur radius on every side and fill the
        // out-of-component area with EDGE-REPLICATED backdrop pixels. CIGaussianBlur
        // fades to transparency at its buffer edge; without a full radius of margin
        // (e.g. when the component sits within ~1mm of the tile edge, less than the
        // blur radius) that fade reaches into the component and feathers its edge,
        // making the glass read smaller than native's crisp panel. Replicating the
        // edge gives the blur a clean clamp-to-extent margin so the component edge
        // stays crisp. We blur the padded buffer then crop the centre back out.
        // CIGaussianBlur's kernel spreads ~3*radius, so the buffer-edge fade reaches
        // that far in. Pad by 3*radius of replicated backdrop so the fade is fully
        // contained outside the component and its own edge stays crisp like native.
        int pad = (int) Math.ceil(radius) * 3 + 1;
        int bw = rw + 2 * pad, bh = rh + 2 * pad;
        // Available (clamped) slice of the real backdrop around the component.
        int ax0 = Math.max(0, rx - pad), ay0 = Math.max(0, ry - pad);
        int ax1 = Math.min(target.width, rx + rw + pad), ay1 = Math.min(target.height, ry + rh + pad);
        int aw = ax1 - ax0, ah = ay1 - ay0;
        int[] avail = new int[aw * ah];
        getRGB(target, avail, 0, ax0, ay0, aw, ah);
        // Padded buffer origin in absolute coords is (rx-pad, ry-pad); sample the
        // available slice with edge clamping to replicate beyond the tile.
        int[] prgb = new int[bw * bh];
        for (int by = 0; by < bh; by++) {
            int ay = (ry - pad + by) - ay0;
            if (ay < 0) ay = 0; else if (ay >= ah) ay = ah - 1;
            int arow = ay * aw, brow = by * bw;
            for (int bx = 0; bx < bw; bx++) {
                int ax = (rx - pad + bx) - ax0;
                if (ax < 0) ax = 0; else if (ax >= aw) ax = aw - 1;
                prgb[brow + bx] = avail[arow + ax];
            }
        }
        // Reverse-engineered iOS UIVisualEffectView material: an affine colour
        // transform (saturation boost + scale + offset floor) of the backdrop before
        // blurring (this is the backdrop-filter path only).
        glassMaterialInPlace(prgb, sat, scale, offset);
        NativeImage blurredPadded = new NativeImage("backdrop-filter glass");
        blurredPadded.peer = nativeInstance.gausianBlurImage(createImageFromARGB(prgb, bw, bh), radius);
        blurredPadded.width = bw;
        blurredPadded.height = bh;
        // Read the blurred padded buffer back, then apply the Liquid Glass OPTICS:
        // edge refraction (lensing) + specular rim, with a rounded-rect SDF used for
        // both the displacement profile and the anti-aliased shape mask. The component
        // sits at offset (pad,pad) in the padded buffer; refraction samples that buffer
        // (its replicated margin keeps edge samples valid).
        int[] pbargb = new int[bw * bh];
        getRGB(blurredPadded, pbargb, 0, 0, 0, bw, bh);
        int[] out = new int[rw * rh];
        applyGlassOptics(pbargb, bw, bh, pad, out, rw, rh, cornerRadius, refract, specular);
        NativeImage blurred = new NativeImage("backdrop-filter glass");
        blurred.peer = createImageFromARGB(out, rw, rh);
        blurred.width = rw;
        blurred.height = rh;
        // drawImage applies this graphics' transform; pass coordinates relative to that
        // transform's translation so the blurred patch lands back where we read it.
        int tx = (int) Math.round(ng.transform.getTranslateX());
        int ty = (int) Math.round(ng.transform.getTranslateY());
        drawImage(ng, blurred, rx - tx, ry - ty);
        return true;
    }

    /**
     * Applies the Liquid Glass OPTICS to the blurred, colour-transformed backdrop
     * (src, the bw x bh padded buffer; the component occupies rw x rh at offset
     * (pad,pad)) and writes the rw x rh result into out. Three effects, all keyed off
     * a rounded-rect signed distance field so they follow the host shape (capsule when
     * cornerRadius &lt; 0):
     * <ul>
     * <li><b>Edge refraction / lensing</b>: near the edges the backdrop sample is
     * displaced radially toward the centre following a quarter-circle profile
     * (1 - sqrt(1 - t^2)), magnifying/bending the backdrop so the panel reads as a
     * real glass layer ON TOP rather than a flat see-through hole. Invisible over a
     * flat backdrop (displacing a uniform field is a no-op), pronounced over busy
     * content -- exactly like iOS.</li>
     * <li><b>Specular rim</b>: a bright highlight in a thin band at the very edge,
     * brightest at the top (the iOS "glint"). </li>
     * <li><b>Shape mask</b>: anti-aliased coverage from the SDF, so the glass clips to
     * the rounded/pill shape with a crisp 1px edge.</li>
     * </ul>
     */
    private static void applyGlassOptics(int[] src, int bw, int bh, int pad, int[] out,
            int rw, int rh, float cornerRadius, float refract, float specular) {
        float hw = rw / 2f, hh = rh / 2f;
        float r = cornerRadius < 0f ? Math.min(hw, hh) : Math.min(cornerRadius, Math.min(hw, hh));
        if (r < 0f) r = 0f;
        float band = Math.min(hw, hh) * 0.6f;       // refraction active in the outer 60%
        float rimW = 3.0f;                          // specular rim width (px)
        for (int y = 0; y < rh; y++) {
            float py = y + 0.5f;
            for (int x = 0; x < rw; x++) {
                float px = x + 0.5f;
                // Rounded-rect signed distance: negative inside, 0 at the edge.
                float dx = Math.abs(px - hw) - (hw - r);
                float dy = Math.abs(py - hh) - (hh - r);
                float ax = dx > 0 ? dx : 0, ay = dy > 0 ? dy : 0;
                float outside = (float) Math.sqrt(ax * ax + ay * ay);
                float inside = Math.min(Math.max(dx, dy), 0f);
                float sdf = outside + inside - r;
                float depth = -sdf;                 // >0 inside the shape, 0 at edge
                if (depth <= 0f) { out[y * rw + x] = 0; continue; }
                float alpha = depth >= 1f ? 1f : depth;   // 1px AA edge
                // Edge refraction: sample the backdrop displaced toward the centre.
                // Base on the integer coord so a zero displacement samples the source
                // pixel EXACTLY (a px+0.5 base would bilinear-soften the whole patch).
                float sx = x, sy = y;
                if (refract > 0f && band > 0f && depth < band) {
                    float t = 1f - depth / band;            // 1 at edge -> 0 at band
                    float distortion = 1f - (float) Math.sqrt(Math.max(0f, 1f - t * t));
                    sx = x - (px - hw) * distortion * refract;
                    sy = y - (py - hh) * distortion * refract;
                }
                int col = sampleBilinear(src, bw, bh, sx + pad, sy + pad);
                int rr = (col >> 16) & 0xff, gg = (col >> 8) & 0xff, bb = col & 0xff;
                // Specular rim: bright glint in the outer rimW px, brightest at top.
                if (specular > 0f && depth < rimW) {
                    float rim = 1f - depth / rimW;
                    float topBias = 0.55f + 0.45f * (1f - py / rh);
                    int add = (int) (specular * rim * topBias * 70f);
                    rr = rr + add > 255 ? 255 : rr + add;
                    gg = gg + add > 255 ? 255 : gg + add;
                    bb = bb + add > 255 ? 255 : bb + add;
                }
                int a = (int) (alpha * 255f);
                out[y * rw + x] = (a << 24) | (rr << 16) | (gg << 8) | bb;
            }
        }
    }

    /** Bilinear ARGB sample with edge clamping; used by the glass edge refraction. */
    private static int sampleBilinear(int[] buf, int w, int h, float fx, float fy) {
        if (fx < 0f) fx = 0f; else if (fx > w - 1) fx = w - 1;
        if (fy < 0f) fy = 0f; else if (fy > h - 1) fy = h - 1;
        int x0 = (int) fx, y0 = (int) fy;
        int x1 = x0 + 1 < w ? x0 + 1 : x0, y1 = y0 + 1 < h ? y0 + 1 : y0;
        float tx = fx - x0, ty = fy - y0;
        int p00 = buf[y0 * w + x0], p10 = buf[y0 * w + x1];
        int p01 = buf[y1 * w + x0], p11 = buf[y1 * w + x1];
        int r = bilerp((p00 >> 16) & 0xff, (p10 >> 16) & 0xff, (p01 >> 16) & 0xff, (p11 >> 16) & 0xff, tx, ty);
        int g = bilerp((p00 >> 8) & 0xff, (p10 >> 8) & 0xff, (p01 >> 8) & 0xff, (p11 >> 8) & 0xff, tx, ty);
        int b = bilerp(p00 & 0xff, p10 & 0xff, p01 & 0xff, p11 & 0xff, tx, ty);
        return (r << 16) | (g << 8) | b;
    }

    private static int bilerp(int c00, int c10, int c01, int c11, float tx, float ty) {
        float top = c00 + (c10 - c00) * tx;
        float bot = c01 + (c11 - c01) * tx;
        return (int) (top + (bot - top) * ty + 0.5f);
    }

    /**
     * Reverse-engineered iOS "Liquid Glass" material (empirically derived from a
     * real UIVisualEffectView, validated &lt;1 LSB): an affine colour transform of
     * each (blurred) backdrop pixel. For each channel c:
     * c' = clamp( (lum + (c - lum) * sat) * scale + offset ) where lum is the
     * pixel luma. The offset term is the white/dark frost floor. Alpha preserved.
     */
    private static void glassMaterialInPlace(int[] argb, float sat, float scale, float offset) {
        for (int i = 0; i < argb.length; i++) {
            int p = argb[i];
            int a = p & 0xff000000;
            float r = (p >> 16) & 0xff, g = (p >> 8) & 0xff, b = p & 0xff;
            float lum = 0.2126f * r + 0.7152f * g + 0.0722f * b;
            r = (lum + (r - lum) * sat) * scale + offset;
            g = (lum + (g - lum) * sat) * scale + offset;
            b = (lum + (b - lum) * sat) * scale + offset;
            int ri = r < 0 ? 0 : (r > 255 ? 255 : (int) r);
            int gi = g < 0 ? 0 : (g > 255 ? 255 : (int) g);
            int bi = b < 0 ? 0 : (b > 255 ? 255 : (int) b);
            argb[i] = a | (ri << 16) | (gi << 8) | bi;
        }
    }

    /** Liquid-glass vibrancy: how far backdrop colours are pushed from grey (1.0 = off). */
    private static final float GLASS_SATURATION = 1.35f;

    /**
     * Boosts the saturation of an ARGB buffer in place by interpolating each pixel
     * away from its perceptual luminance (the standard saturation-matrix approach):
     * c' = lum + (c - lum) * factor. Alpha is preserved. Used to give the
     * backdrop-filter glass the vibrancy UIKit's real material has.
     */
    private static void saturateInPlace(int[] argb, float factor) {
        if (factor == 1f) {
            return;
        }
        for (int i = 0; i < argb.length; i++) {
            int p = argb[i];
            int a = p & 0xff000000;
            int r = (p >> 16) & 0xff, g = (p >> 8) & 0xff, b = p & 0xff;
            float lum = 0.2126f * r + 0.7152f * g + 0.0722f * b;
            r = (int) (lum + (r - lum) * factor);
            g = (int) (lum + (g - lum) * factor);
            b = (int) (lum + (b - lum) * factor);
            if (r < 0) { r = 0; } else if (r > 255) { r = 255; }
            if (g < 0) { g = 0; } else if (g > 255) { g = 255; }
            if (b < 0) { b = 0; } else if (b > 255) { b = 255; }
            argb[i] = a | (r << 16) | (g << 8) | b;
        }
    }

    
    public Object createImage(byte[] bytes, int offset, int len) {
        int[] wh = widthHeight;
        if(offset != 0 || len != bytes.length) {
            byte[] b = new byte[len];
            System.arraycopy(bytes, offset, b, 0, len);
            bytes = b;
        }
        NativeImage n = new NativeImage("Native PNG of " + bytes.length);
        n.peer = createImage(bytes, wh);
        n.width = wh[0];
        n.height = wh[1];
        return n;
    }

    private long createImage(byte[] data, int[] widthHeight) {
        return nativeInstance.createImage(data, widthHeight);
    }

    public int getImageWidth(Object i) {
        return ((NativeImage)i).width;
    }

    public int getImageHeight(Object i) {
        return ((NativeImage)i).height;
    }

    public Object scale(Object nativeImage, int width, int height) {
        NativeImage original = (NativeImage)nativeImage;
        NativeImage n = new NativeImage("Scaled image from peer: " + original.peer + " width " + width + " height " + height);
        n.scaled = true;
        n.peer = original.peer;
        n.width = width;
        n.height = height;
        nativeInstance.retainPeer(n.peer);
        return n;
    }

    private long scale(long peer, int width, int height) {
        return nativeInstance.scale(peer, width, height);
    }

    public int getSoftkeyCount() {
        return 0;
    }

    public int[] getSoftkeyCode(int index) {
        return null;
    }

    public int getClearKeyCode() {
        return -1;
    }

    public int getBackspaceKeyCode() {
        return -1;
    }

    public int getBackKeyCode() {
        return -1;
    }

    public int getGameAction(int keyCode) {
        if(keyCode <= -20) {
            // this effectively maps negative numbers to media game keys
            return keyCode * -1;
        }
        return -1;
    }

    public int getKeyCode(int gameAction) {
        return -1;
    }

    public boolean isTouchDevice() {
        return true;
    }

    public int getColor(Object graphics) {
        return ((NativeGraphics)graphics).color;
    }


    public void setColor(Object graphics, int RGB) {
        ((NativeGraphics)graphics).color = RGB;
    }

    public void setAlpha(Object graphics, int alpha) {
        ((NativeGraphics)graphics).alpha = alpha;
    }

    public int getAlpha(Object graphics) {
        return ((NativeGraphics)graphics).alpha;
    }

    public boolean isAlphaGlobal() {
        return true;
    }

    public void setNativeFont(Object graphics, Object font) {
        ((NativeGraphics)graphics).font = (NativeFont)font;
    }

    
    
    
    public int getClipX(Object graphics) {
        
        return ((NativeGraphics)graphics).getClipX();
    }

    public int getClipY(Object graphics) {
         
        return ((NativeGraphics)graphics).getClipY();
    }

    public int getClipWidth(Object graphics) {
        return ((NativeGraphics)graphics).getClipW();
    }

    public int getClipHeight(Object graphics) {
        
        return ((NativeGraphics)graphics).getClipH();
    }

    @Override
    public boolean isShapeClipSupported(Object graphics) {
        NativeGraphics ng = (NativeGraphics)graphics;
        return ng.isShapeClipSupported();
    }

    @Override
    public void setClip(Object graphics, Shape shape) {
        ((NativeGraphics)graphics).setClip(shape);
    }
   
    
    public void pushClip(Object graphics){
        ((NativeGraphics)graphics).pushClip();
    }
    
    public void popClip(Object graphics){
        ((NativeGraphics)graphics).popClip();
    }
    
    public void setClip(Object graphics, int x, int y, int width, int height) {
        width = Math.max(0, width);
        height = Math.max(0, height);
        NativeGraphics ng = ((NativeGraphics)graphics);
        ng.checkControl();
        ng.setClip(x, y, width, height);
    }

    private  void setNativeClippingMutable(int x, int y, int width, int height, boolean firstClip) {
        nativeInstance.setNativeClippingMutable(x, y, width, height, firstClip);
    }
    
    
    private  void setNativeClippingGlobal(int x, int y, int width, int height, boolean firstClip) {
        nativeInstance.setNativeClippingGlobal(x, y, width, height, firstClip);
    }
    
    float[] polygonPointsBuffer;
    
    private  void setNativeClippingGlobal(ClipShape shape){
        Rectangle bounds = shape.getBounds();
        if ( shape.isRectangle() || bounds.getWidth() <= 0 || bounds.getHeight() <= 0){
            setNativeClippingGlobal(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), true);
            return;
        }
        // Curved clips (anything containing QUADTO / CUBICTO) get
        // flattened first so the polygon path below sees real polyline
        // vertices instead of interleaved control / anchor pairs. Without
        // this, setClip(circularPath) reaches the native side as 17 raw
        // floats that include 8 outside-the-curve control points, and
        // the triangle-fan stencil writer turns the circle into the
        // visible "triangle clip" on gradient_circle.svg and
        // clipped_badge.svg (see SVGStaticScreenshotTest).
        ClipShape polyShape = flattenClipShapeIfNeeded(shape);
        if (polyShape.isPolygon()) {
            int pointsSize = polyShape.getPointsSize();
            // Reallocate when the buffer doesn't EXACTLY match -- previously
            // this only reallocated when undersized, so a smaller polygon
            // reused a larger buffer and the trailing slots retained the
            // previous (larger) polygon's vertices. The native side reads
            // the JAVA_ARRAY's allocated length, not a separate count, so
            // those stale vertices became "real" polygon corners and
            // produced visible spike artefacts in the rendered clip on
            // iOS Metal (#3921 / PR #4924). Allocate exactly the size we
            // need so trailing garbage can't appear.
            if (polygonPointsBuffer == null || polygonPointsBuffer.length != pointsSize) {
                polygonPointsBuffer = new float[pointsSize];
            }
            shapeToPolygon(polyShape, polygonPointsBuffer);
            nativeInstance.setNativeClippingPolygonGlobal(polygonPointsBuffer);
        } else {
            // The path didn't reduce to a polygon (still has multiple
            // disjoint sub-paths or other oddities). Fall back to the
            // alpha-mask Renderer; on the GL backend this paints the
            // shape into the stencil, on the Metal backend the texture
            // handle isn't compatible with MTLTexture and the bounding
            // box is used as a coarse fallback (see ClipRect.m).
            TextureAlphaMask mask = (TextureAlphaMask)textureCache.get(shape, null);
            if ( mask == null ){
                mask = (TextureAlphaMask)this.createAlphaMask(shape, null);
                textureCache.add(shape, null, mask);
            }

           if ( mask != null ){
                nativeInstance.setNativeClippingMaskGlobal(mask.getTextureName(), mask.getBounds().getX(), mask.getBounds().getY(), mask.getBounds().getWidth(), mask.getBounds().getHeight());
            } else {
               Log.p("Failed to create texture mask for clipping region");
            }

        }
    }

    
    public void clipRect(Object graphics, int x, int y, int width, int height) {
        width = Math.max(0, width);
        height = Math.max(0, height);
        NativeGraphics ng = (NativeGraphics)graphics;
        ng.checkControl();
        ng.clipRect(x, y, width, height);
    }

    @Override
    public boolean isTransformSupported() {
        return true;
    }

    @Override
    public boolean isPerspectiveTransformSupported() {
        return true;
    }

    @Override
    public Object makeTransformAffine(double m00, double m10, double m01, double m11, double m02, double m12) {
        return Matrix.make(new float[]{
           (float)m00, (float)m10, 0, 0,
           (float)m01, (float)m11, 0, 0,
           0, 0, 1, 0,
           (float)m02, (float)m12, 0, 1
        });
    }

    @Override
    public void setTransformAffine(Object nativeTransform, double m00, double m10, double m01, double m11, double m02, double m12) {
        ((Matrix)nativeTransform).setData(new float[]{
           (float)m00, (float)m10, 0, 0,
           (float)m01, (float)m11, 0, 0,
           0, 0, 1, 0,
           (float)m02, (float)m12, 0, 1
        });
    }
    
    
    

    @Override
    public Object makeTransformTranslation(float translateX, float translateY, float translateZ) {
        return Matrix.makeTranslation(translateX, translateY, translateZ);
    }

    @Override
    public void setTransformTranslation(Object nativeTransform, float translateX, float translateY, float translateZ) {
        Matrix m = (Matrix)nativeTransform;
        m.setTranslation(translateX, translateY, translateZ);
    }
    
    @Override
    public Object makeTransformScale(float scaleX, float scaleY, float scaleZ) {
        Matrix out = Matrix.makeIdentity();
        out.scale(scaleX, scaleY, scaleZ);
        return out;
    }
    
    @Override
    public void setTransformScale(Object nativeTransform, float scaleX, float scaleY, float scaleZ) {
        Matrix out = (Matrix)nativeTransform;
        out.reset();
        out.scale(scaleX, scaleY, scaleZ);
    }

    @Override
    public Object makeTransformRotation(float angle, float x, float y, float z) {
        return Matrix.makeRotation(angle, x, y, z);
    }
    
    @Override
    public void setTransformRotation(Object nativeTransform, float angle, float x, float y, float z) {
        Matrix m = (Matrix)nativeTransform;
        m.reset();
        m.rotate(angle, x, y, z);
    }

    @Override
    public Object makeTransformPerspective(float fovy, float aspect, float zNear, float zFar) {
        return Matrix.makePerspective(fovy, aspect, zNear, zFar);
    }
    
    public void setTransformPerspective(Object nativeGraphics, float fovy, float aspect, float zNear, float zFar) {
        Matrix m = (Matrix)nativeGraphics;
        m.setPerspective(fovy, aspect, zNear, zFar);
    }

    @Override
    public Object makeTransformOrtho(float left, float right, float bottom, float top, float near, float far) {
        return Matrix.makeOrtho(left, right, bottom, top, near, far);
    }
    
    public void setTransformOrtho(Object nativeGraphics, float left, float right, float bottom, float top, float near, float far) {
        Matrix m = (Matrix)nativeGraphics;
        m.setOrtho(left, right, bottom, top, near, far);
    }

    @Override
    public Object makeTransformCamera(float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ) {
        return Matrix.makeCamera(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
    }
    
    @Override
    public void setTransformCamera(Object nativeGraphics, float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ) {
        Matrix m = (Matrix)nativeGraphics;
        m.setCamera(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
    }

    @Override
    public void transformRotate(Object nativeTransform, float angle, float x, float y, float z) {
        ((Matrix)nativeTransform).rotate(angle, x, y, z);
    }

    @Override
    public void transformTranslate(Object nativeTransform, float x, float y, float z) {
        ((Matrix)nativeTransform).translate(x, y, z);
    }

    
    @Override
    public void transformScale(Object nativeTransform, float x, float y, float z) {
        ((Matrix)nativeTransform).scale(x, y, z);
    }

    @Override
    public Object makeTransformInverse(Object nativeTransform) {
        Matrix copy = ((Matrix)nativeTransform).copy();
        if ( copy.invert() ){
            return copy;
        } else {
            return null;
        }
    }
    
    @Override
    public void setTransformInverse(Object nativeTransform) throws com.codename1.ui.Transform.NotInvertibleException {
        Matrix m = (Matrix)nativeTransform;
        if (!m.invert()) {
            throw new com.codename1.ui.Transform.NotInvertibleException();
        }
    }
    
    @Override
    public Object makeTransformIdentity(){
        return Matrix.makeIdentity();
    }
    
    @Override
    public void setTransformIdentity(Object nativeTransform){
        ((Matrix)nativeTransform).setIdentity();
    }

    @Override
    public void copyTransform(Object src, Object dest) {
        Matrix srcM = (Matrix)src;
        Matrix destM = (Matrix)dest;
        System.arraycopy(srcM.data, 0, destM.data, 0, 16);
    }

    @Override
    public void concatenateTransform(Object t1, Object t2) {
        ((Matrix)t1).concatenate((Matrix)t2);
    }

    
    @Override
    public void transformPoint(Object nativeTransform, float[] in, float[] out) {
        ((Matrix)nativeTransform).transformPoints(Math.min(3, in.length), in, 0, out, 0, 1);
    }

    @Override
    public void transformPoints(Object nativeTransform, int pointSize, float[] in, int srcPos, float[] out, int destPos, int numPoints) {
        Matrix m = (Matrix)nativeTransform;
        m.transformPoints(pointSize, in, srcPos, out, destPos, numPoints);
    }

    @Override
    public void translatePoints(int pointSize, float tX, float tY, float tZ, float[] in, int srcPos, float[] out, int destPos, int numPoints) {
        nativeInstance.translatePoints(pointSize, tX, tY, tX, in, srcPos, out, destPos, numPoints);
    }

    @Override
    public void scalePoints(int pointSize, float sX, float sY, float sZ, float[] in, int srcPos, float[] out, int destPos, int numPoints) {
        nativeInstance.scalePoints(pointSize, sX, sY, sZ, in, srcPos, out, destPos, numPoints);
    }
   
    // END TRANSFORMATION METHODS--------------------------------------------------------------------
    
    
    private static void nativeDrawLineMutable(int color, int alpha, int x1, int y1, int x2, int y2) {
        nativeInstance.nativeDrawLineMutable(color, alpha, x1, y1, x2, y2);
    }
    private static void nativeDrawLineGlobal(int color, int alpha, int x1, int y1, int x2, int y2) {
        nativeInstance.nativeDrawLineGlobal(color, alpha, x1, y1, x2, y2);
    }

    public void drawLine(Object graphics, int x1, int y1, int x2, int y2) {
        NativeGraphics ng = (NativeGraphics)graphics;
        ng.checkControl();
        ng.applyTransform();
        ng.applyClip();
        ng.nativeDrawLine(ng.color, ng.alpha, x1, y1, x2, y2);
    }
    

    static void nativeFillRectMutable(int color, int alpha, int x, int y, int width, int height) {
        nativeInstance.nativeFillRectMutable(color, alpha, x, y, width, height);
    }
    
    static void nativeFillRectGlobal(int color, int alpha, int x, int y, int width, int height) {
        nativeInstance.nativeFillRectGlobal(color, alpha, x, y, width, height);
    }
    
    static void nativeClearRectGlobal(int x, int y, int width, int height) {
        nativeInstance.nativeClearRectGlobal(x, y, width, height);
    }

    public void fillRect(Object graphics, int x, int y, int width, int height) {
        NativeGraphics ng = (NativeGraphics)graphics;
        if(ng.alpha == 0) {
            return;
        }
        ng.checkControl();
        ng.applyTransform();
        ng.applyClip();
        ng.nativeFillRect(ng.color, ng.alpha, x, y, width, height);
    }

    @Override
    public void fillPolygon(Object graphics, int[] xPoints, int[] yPoints, int nPoints) {
        NativeGraphics ng = (NativeGraphics)graphics;
        if(ng.alpha == 0) {
            return;
        }
        ng.checkControl();
        ng.applyTransform();
        ng.applyClip();
        ng.fillPolygon(ng.color, ng.alpha, xPoints, yPoints, nPoints);
    }
    
    
    
    public void clearRect(Object graphics, int x, int y, int width, int height) {
        NativeGraphics ng = (NativeGraphics)graphics;
        ng.checkControl();
        ng.applyTransform();
        ng.applyClip();
        ng.nativeClearRect(x, y, width, height);
    }

    private static void nativeDrawRectMutable(int color, int alpha, int x, int y, int width, int height) {
        nativeInstance.nativeDrawRectMutable(color, alpha, x, y, width, height);
    }
    private static void nativeDrawRectGlobal(int color, int alpha, int x, int y, int width, int height) {
        nativeInstance.nativeDrawRectGlobal(color, alpha, x, y, width, height);
    }

    public void drawRect(Object graphics, int x, int y, int width, int height) {
        NativeGraphics ng = (NativeGraphics)graphics;
        ng.checkControl();
        ng.applyTransform();
        ng.applyClip();
        ng.nativeDrawRect(ng.color, ng.alpha, x, y, width, height);
    }

    public void drawRoundRect(Object graphics, int x, int y, int width, int height, int arcWidth, int arcHeight) {
        NativeGraphics ng = (NativeGraphics)graphics;
        ng.checkControl();
        ng.applyTransform();
        ng.applyClip();
        ng.nativeDrawRoundRect(ng.color, ng.alpha, x, y, width, height, arcWidth, arcHeight);
    }

    public void fillRoundRect(Object graphics, int x, int y, int width, int height, int arcWidth, int arcHeight) {
        NativeGraphics ng = (NativeGraphics)graphics;
        ng.checkControl();
        ng.applyTransform();
        ng.applyClip();
        ng.nativeFillRoundRect(ng.color, ng.alpha, x, y, width, height, arcWidth, arcHeight);
    }

    public void fillArc(Object graphics, int x, int y, int width, int height, int startAngle, int arcAngle) {
        NativeGraphics ng = (NativeGraphics)graphics;
        ng.checkControl();
        ng.applyTransform();
        ng.applyClip();
        ng.nativeFillArc(ng.color, ng.alpha, x, y, width, height, startAngle, arcAngle);
    }

    @Override
    public void fillRadialGradient(Object graphics, int startColor, int endColor, int x, int y, int width, int height, int startAngle, int arcAngle) {
        NativeGraphics ng = (NativeGraphics)graphics;
        ng.checkControl();
        ng.applyTransform();
        ng.applyClip();
        Paint oldPaint = ng.paint;
        ng.paint = new RadialGradient(startColor, endColor, x, y, width, height);
        ng.applyPaint();
        ng.nativeFillArc(ng.color, ng.alpha, x, y, width, height, startAngle, arcAngle);
        ng.unapplyPaint();
        ng.paint = oldPaint;
    }

    @Override
    public void fillRadialGradient(Object graphics, int startColor, int endColor, int x, int y, int width, int height) {
        fillRadialGradient(graphics, startColor, endColor, x, y, width, height, 0, 360); 
    }
    
    

    public void drawArc(Object graphics, int x, int y, int width, int height, int startAngle, int arcAngle) {
        NativeGraphics ng = (NativeGraphics)graphics;
        ng.checkControl();
        ng.applyTransform();
        ng.applyClip();
        ng.nativeDrawArc(ng.color, ng.alpha, x, y, width, height, startAngle, arcAngle);
    }

    private static void nativeDrawStringMutable(int color, int alpha, long fontPeer, String str, int x, int y) {
        nativeInstance.nativeDrawStringMutable(color, alpha, fontPeer, str, x, y);
    }
    private static void nativeDrawStringGlobal(int color, int alpha, long fontPeer, String str, int x, int y) {
        nativeInstance.nativeDrawStringGlobal(color, alpha, fontPeer, str, x, y);
    }

    @Override
    public void drawString(Object graphics, Object nativeFont, String str, int x, int y, int textDecoration) {
        // Re-sync ng.font with the Java-side current font before drawing.
        // Display.impl.drawLabelComponent calls setNativeFont(ng, labelStyleFont)
        // directly to push the label's style font into NativeGraphics for fast
        // native rendering, but does NOT update Graphics.current. After the
        // title bar (or any Label) renders, ng.font holds the label's font
        // while Graphics.current holds the Java-side font from before the
        // label's draw. The user's next g.drawString() on the same Graphics
        // expects to use Graphics.current; the iOS 4-arg drawString below
        // reads ng.font instead, so they diverge. Graphics.drawString already
        // passes Graphics.current as the nativeFont parameter -- pin ng.font
        // to it here so the 4-arg drawString picks up the correct font.
        if (nativeFont != null && graphics instanceof NativeGraphics) {
            ((NativeGraphics) graphics).font = (NativeFont) nativeFont;
        }
        super.drawString(graphics, nativeFont, str, x, y, textDecoration);
    }

    public void drawString(Object graphics, String str, int x, int y) {
        NativeGraphics ng = (NativeGraphics)graphics;
        ng.checkControl();
        ng.applyTransform();
        ng.applyClip();
        NativeFont fnt = ng.getFont();
        int l = str.length();
        int max = fnt.getMaxStringLength();
        if(l > max) {
            boolean rtl = useContentBasedRTLStringDetection
                ? nativeInstance.isRTLString(str)
                : UIManager.getInstance().getLookAndFeel().isRTL();
            // really long string split it and draw multiple strings to avoid texture overload
            int one = 1;
            if(l % max == 0) {
                one = 0;
            }
            if (rtl) {
                x += stringWidth(fnt, str);
            }
            int stringCount = l / max + one;
            for(int iter = 0 ; iter < stringCount ; iter++) {
                int pos = iter * max;
                String s = str.substring(pos, Math.min(pos + max, str.length()));
                int substrWidth = stringWidth(fnt, s);
                int rtlOffset = rtl ? -substrWidth : 0;
                ng.nativeDrawString(ng.color, ng.alpha, fnt.peer, s, x + rtlOffset, y);
                x += (rtl ? -substrWidth : substrWidth);
            }
        } else {
            ng.nativeDrawString(ng.color, ng.alpha, fnt.peer, str, x, y);
        }
    }

    public void tileImage(Object graphics, Object img, int x, int y, int w, int h) {
        if (img == null) return;
        NativeGraphics ng = (NativeGraphics)graphics;
        if (ng instanceof GlobalGraphics) {
            ng.checkControl();
            ng.applyTransform();
            ng.applyClip();
            NativeImage nm = (NativeImage)img;
            nativeInstance.nativeTileImageGlobal(nm.peer, ng.alpha, x, y, w, h);
        } else if (metalRendering) {
            // Phase 3 v2 (Metal only): queue a single TileImage op tagged
            // with the current mutable image as target. nativeTileImage-
            // Global's C side picks up currentMutableImage and tags
            // accordingly. Mirrors the GlobalGraphics branch above except
            // ng.checkControl already set currentMutableImage. Avoids
            // super.tileImage's 1500-iter drawImage loop which would
            // queue ~1500 ops per panel and stall the EDT past the test
            // timeout on slow CI runners. On GL the same tagging doesn't
            // happen (drawTextureAlphaMask/TileImage setTarget is gated
            // by `#ifdef CN1_USE_METAL`) so the op would land on the
            // screen instead of inside the mutable -- fall back to the
            // EDT-side super.tileImage there.
            ng.checkControl();
            ng.applyTransform();
            ng.applyClip();
            NativeImage nm = (NativeImage)img;
            nativeInstance.nativeTileImageGlobal(nm.peer, ng.alpha, x, y, w, h);
        } else {
            super.tileImage(graphics, img, x, y, w, h);
        }
    }
    
    public void drawImage(Object graphics, Object img, int x, int y) {
        if (img == null) return;
        NativeGraphics ng = (NativeGraphics)graphics;
        //System.out.println("Drawing image " + img);
        ng.checkControl();
        ng.applyTransform();
        ng.applyClip();
        NativeImage nm = (NativeImage)img;
        ng.nativeDrawImage(nm.peer, ng.alpha, x, y, nm.width, nm.height);
    }

    
    
    @Override
    public void setRenderingHints(Object nativeGraphics, int hints) {
        NativeGraphics ng = (NativeGraphics)nativeGraphics;
        ng.setRenderingHints(hints);
    }

    @Override
    public int getRenderingHints(Object nativeGraphics) {
        NativeGraphics ng = (NativeGraphics)nativeGraphics;
        return ng.renderingHints;
    }
    
    
    
    

    // -------------------------------------------------------------------------
    // METHODS FOR DRAWING SHAPES AND TRANSFORMATIONS
    // -------------------------------------------------------------------------
    /**
     * Creates a platform-specific alpha mask for a shape.  This is used to cache 
     * masks in the {@link com.codename1.ui.GeneralPath} class.  On iOS the alpha
     * mask is an OpenGL texture ID (not a raster of alpha pixels), but other platforms 
     * may use different representations if they like.
     * 
     * <p>The {@link com.codename1.ui.Graphics#drawAlphaMask} method
     * is used to draw a mask on the graphics context and this will ultimately call {@link #drawAlphaMask}
     * which can be platform specific also.
     * </p>
     * @param shape The shape that will have an alpha mask created.
     * @param stroke The stroke settings for stroking the outline of the mask.  Leave null to produce a fill 
     * mask.
     * @return The platform specific alpha mask object or null if it is not supported or failed.
     * @see #deleteAlphaMask
     * @see #drawAlphaMask
     * @see #isAlphaMaskSupported
     * @see com.codename1.ui.Graphics#drawAlphaMask 
     * @see com.codename1.ui.GeneralPath#getAlphaMask
     */
    public TextureAlphaMask createAlphaMask(Shape shape, Stroke stroke) {
        int[] bounds = new int[]{0,0,0,0};
        long tex = nativeCreateAlphaMaskForShape(shape, stroke, bounds);
        Rectangle shapeBounds = shape.getBounds();
        int[] padding = new int[]{
            //top
            shapeBounds.getY()-bounds[1],   
            // right
            bounds[2] - (shapeBounds.getX()+shapeBounds.getWidth()), 
            // bottom
            bounds[3] - (shapeBounds.getY()+shapeBounds.getHeight()), 
            // left
            shapeBounds.getX()-bounds[0]
        };
        
        if ( tex == 0 ){
            return null;
        }
        return new TextureAlphaMask(tex, new Rectangle(bounds[0], bounds[1], bounds[2]-bounds[0], bounds[3]-bounds[1]), padding);
    }
    
    @Override
    public Image createImage(Shape shape, Stroke stroke, int color){
        NativePathRenderer renderer = renderShape(shape, stroke);
        int[] argb = renderer.toARGB(color);
        int[] bounds = new int[4];
        renderer.getOutputBounds(bounds);
        Image out = Image.createImage(argb, bounds[2]-bounds[0], bounds[3]-bounds[1]);
        renderer.destroy();
        return out;
    }
    
    private NativePathRenderer renderShape(Shape shape, Stroke stroke){
        if ( stroke != null ){
            float lineWidth = stroke.getLineWidth();
            int capStyle = stroke.getCapStyle();
            int miterStyle = stroke.getJoinStyle();
            float miterLimit = stroke.getMiterLimit();
            
            PathIterator path = shape.getPathIterator();
            Rectangle rb = shape.getBounds();
            // Notice that these will be cleaned up in the dealloc method of the DrawPath objective-c class
            int padding = (int)Math.ceil(lineWidth);
            int padding2 = padding * 2;
            NativePathRenderer renderer = new NativePathRenderer(rb.getX()-padding, rb.getY()-padding, rb.getWidth()+padding2, rb.getHeight()+padding2, path.getWindingRule());
            NativePathStroker stroker = new NativePathStroker(renderer, lineWidth, capStyle, miterStyle, miterLimit);
            NativePathConsumer c = stroker.consumer;
            fillPathConsumer(path, c);

            // We don't need the stroker anymore because it has passed the strokes to the renderer.
            stroker.destroy();
            return renderer;

        } else {
            Rectangle rb = shape.getBounds();
            PathIterator path = shape.getPathIterator();

            // Notice that this will be cleaned up in the dealloc method of the DrawPath objective-c class.
            NativePathRenderer renderer = new NativePathRenderer(rb.getX(), rb.getY(), rb.getWidth(), rb.getHeight(), path.getWindingRule());
            
            NativePathConsumer c = renderer.consumer;
            fillPathConsumer(path, c);
            
            return renderer;
            
        }
    }
    
    private long nativeCreateAlphaMaskForShape(Shape shape, Stroke stroke, int[] bounds) {
        
        NativePathRenderer renderer = renderShape(shape, stroke);
        long tex = renderer.createTexture();
        renderer.getOutputBounds(bounds);
        renderer.destroy();
        return tex;
 
    }
    
    private void shapeToPolygon(ClipShape shape, float[] pointsOut){
        int size = shape.getPointsSize();
        if (size > pointsOut.length) {
            throw new RuntimeException("shapeToPolygon requires out array at least the size of the points in the polygon.  Requires "+size+" but found "+pointsOut.length);
        }
        shape.getPoints(pointsOut);

    }

    // Reusable buffer for flattening curves into a polyline GeneralPath
    // before handing the clip down to the native polygon path. Reused
    // across clip applications to avoid per-frame allocation.
    private GeneralPath flattenedClipPath;
    private ClipShape flattenedClipShape;

    /// Walks `src` and builds a polyline GeneralPath in `dst` by replacing
    /// every QUADTO / CUBICTO with a chain of straight LINETO segments
    /// produced by midpoint subdivision. The native iOS clip pipeline
    /// (GL ES2 FillPolygon and Metal CN1MetalApplyPolygonStencilClip) both
    /// consume their input as a flat polygon: the only points they look
    /// at are the (x, y) pairs in the buffer. When the source path is a
    /// curve (e.g. a circle built from arc() emits 8 quadTos) the raw
    /// points buffer contains alternating control / anchor pairs, and the
    /// stencil writer treats every control point as a real polygon
    /// vertex. The result is the degenerate "triangle clip" described in
    /// the SVG tests on gradient_circle.svg / clipped_badge.svg. Flatten
    /// first so only true vertices survive.
    private void flattenShapeToPolyline(Shape src, GeneralPath dst) {
        dst.reset();
        PathIterator it = src.getPathIterator();
        dst.setWindingRule(it.getWindingRule());
        float[] coords = new float[6];
        float curX = 0f, curY = 0f, moveX = 0f, moveY = 0f;
        while (!it.isDone()) {
            int seg = it.currentSegment(coords);
            switch (seg) {
                case PathIterator.SEG_MOVETO:
                    dst.moveTo(coords[0], coords[1]);
                    curX = moveX = coords[0];
                    curY = moveY = coords[1];
                    break;
                case PathIterator.SEG_LINETO:
                    dst.lineTo(coords[0], coords[1]);
                    curX = coords[0];
                    curY = coords[1];
                    break;
                case PathIterator.SEG_QUADTO:
                    flattenQuadInto(dst, curX, curY, coords[0], coords[1], coords[2], coords[3], 0);
                    curX = coords[2];
                    curY = coords[3];
                    break;
                case PathIterator.SEG_CUBICTO:
                    flattenCubicInto(dst, curX, curY,
                            coords[0], coords[1], coords[2], coords[3], coords[4], coords[5], 0);
                    curX = coords[4];
                    curY = coords[5];
                    break;
                case PathIterator.SEG_CLOSE:
                    dst.closePath();
                    curX = moveX;
                    curY = moveY;
                    break;
            }
            it.next();
        }
    }

    // Squared distance threshold (in user-space units) for the
    // subdivision flatness test. 0.25 px is well below 1 device pixel
    // even after the typical retina upscale and matches the precision of
    // the alpha-mask Renderer used by the rest of the iOS port.
    private static final float FLATTEN_TOLERANCE_SQ = 0.25f * 0.25f;
    // Safety cap on the recursion depth. 18 = 2^18 sub-segments which is
    // far past anything a real SVG path needs; the flatness test should
    // always converge well before this.
    private static final int FLATTEN_MAX_DEPTH = 18;

    private static void flattenQuadInto(GeneralPath dst,
                                        float x0, float y0,
                                        float x1, float y1,
                                        float x2, float y2,
                                        int depth) {
        // Distance from the control point to the chord P0-P2. For a
        // quadratic Bezier the maximum deviation between the curve and
        // its chord is bounded by half the control-point-to-chord
        // distance, so testing the control point against the threshold
        // is a safe (slightly conservative) flatness criterion.
        float dx = x2 - x0;
        float dy = y2 - y0;
        float lenSq = dx * dx + dy * dy;
        float distSq;
        if (lenSq < 1e-6f) {
            distSq = (x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0);
        } else {
            float cross = (x1 - x0) * dy - (y1 - y0) * dx;
            distSq = (cross * cross) / lenSq;
        }
        if (distSq <= FLATTEN_TOLERANCE_SQ || depth >= FLATTEN_MAX_DEPTH) {
            dst.lineTo(x2, y2);
            return;
        }
        float mx1 = (x0 + x1) * 0.5f, my1 = (y0 + y1) * 0.5f;
        float mx2 = (x1 + x2) * 0.5f, my2 = (y1 + y2) * 0.5f;
        float mx = (mx1 + mx2) * 0.5f, my = (my1 + my2) * 0.5f;
        flattenQuadInto(dst, x0, y0, mx1, my1, mx, my, depth + 1);
        flattenQuadInto(dst, mx, my, mx2, my2, x2, y2, depth + 1);
    }

    private static void flattenCubicInto(GeneralPath dst,
                                         float x0, float y0,
                                         float x1, float y1,
                                         float x2, float y2,
                                         float x3, float y3,
                                         int depth) {
        // Max distance from either inner control point to the chord
        // P0-P3. A cubic curve never strays farther than its furthest
        // control point from its chord, so the larger of the two
        // perpendicular distances is a conservative flatness bound.
        float dx = x3 - x0;
        float dy = y3 - y0;
        float lenSq = dx * dx + dy * dy;
        float d1Sq, d2Sq;
        if (lenSq < 1e-6f) {
            d1Sq = (x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0);
            d2Sq = (x2 - x0) * (x2 - x0) + (y2 - y0) * (y2 - y0);
        } else {
            float c1 = (x1 - x0) * dy - (y1 - y0) * dx;
            float c2 = (x2 - x0) * dy - (y2 - y0) * dx;
            d1Sq = (c1 * c1) / lenSq;
            d2Sq = (c2 * c2) / lenSq;
        }
        float distSq = d1Sq > d2Sq ? d1Sq : d2Sq;
        if (distSq <= FLATTEN_TOLERANCE_SQ || depth >= FLATTEN_MAX_DEPTH) {
            dst.lineTo(x3, y3);
            return;
        }
        float mx01 = (x0 + x1) * 0.5f, my01 = (y0 + y1) * 0.5f;
        float mx12 = (x1 + x2) * 0.5f, my12 = (y1 + y2) * 0.5f;
        float mx23 = (x2 + x3) * 0.5f, my23 = (y2 + y3) * 0.5f;
        float mxA = (mx01 + mx12) * 0.5f, myA = (my01 + my12) * 0.5f;
        float mxB = (mx12 + mx23) * 0.5f, myB = (my12 + my23) * 0.5f;
        float mx = (mxA + mxB) * 0.5f, my = (myA + myB) * 0.5f;
        flattenCubicInto(dst, x0, y0, mx01, my01, mxA, myA, mx, my, depth + 1);
        flattenCubicInto(dst, mx, my, mxB, myB, mx23, my23, x3, y3, depth + 1);
    }

    // True if the path has only MOVETO / LINETO / CLOSE segments, i.e.
    // it is already a polyline and flattening would just copy it.
    private boolean isAlreadyFlat(Shape s) {
        if (s instanceof ClipShape && ((ClipShape) s).isRect()) {
            return true;
        }
        PathIterator it = s.getPathIterator();
        float[] coords = new float[6];
        while (!it.isDone()) {
            int seg = it.currentSegment(coords);
            if (seg == PathIterator.SEG_QUADTO || seg == PathIterator.SEG_CUBICTO) {
                return false;
            }
            it.next();
        }
        return true;
    }

    // Flatten if necessary and return the ClipShape that should be sent
    // through the native polygon clip path. When the input is already a
    // polyline (the common case for rectangular clipRect intersections
    // built by NativeGraphics.clipRect) the input is returned as-is. The
    // returned ClipShape is reused across calls (not shared with the
    // input), so callers must finish reading from it before the next
    // clip is applied.
    private ClipShape flattenClipShapeIfNeeded(ClipShape src) {
        if (isAlreadyFlat(src)) {
            return src;
        }
        if (flattenedClipPath == null) {
            flattenedClipPath = new GeneralPath();
        }
        flattenShapeToPolyline(src, flattenedClipPath);
        if (flattenedClipShape == null) {
            flattenedClipShape = new ClipShape();
        }
        flattenedClipShape.setShape(flattenedClipPath, null);
        return flattenedClipShape;
    }
    /*
    public void drawConvexPolygon(Object graphics, Shape shape, Stroke stroke, int color, int alpha){
        NativeGraphics ng = (NativeGraphics)graphics;
        if ( ng.isShapeSupported()){
            ng.checkControl();
            ng.applyTransform();
            ng.applyClip();
            float[] points = shapeToPolygon(shape);
            if ( stroke == null ){
                ng.fillConvexPolygon(points, color, alpha);

            } else {
                ng.drawConvexPolygon(points, color, alpha, stroke.getLineWidth(), stroke.getJoinStyle(), stroke.getCapStyle(), stroke.getMiterLimit());
            }
        }
        
    }
    */

    /**
     * Deletes an alpha mask that was created with {@link #createAlphaMask}.
     * @param texture The alpha mask to be deleted.
     * @see #createAlphaMask
     * @see #isAlphaMaskSupported
     */
    public void deleteAlphaMask(TextureAlphaMask mask) {
        mask.dispose();
        
    }

    /**
     * Draws the given alpha mask (created by {@link #createAlphaMask} to the given graphics context.
     * @param graphics The graphics context to which to draw the alpha mask.
     * @param mask The mask to be drawn.
     * @see #createAlphaMask
     * @see #deleteAlphaMask
     * @see #isAlphaMaskSupported
     * @see com.codename1.ui.Graphics#drawAlphaMask 
     * @see com.codename1.ui.GeneralPath#getAlphaMask
     */
    public void drawAlphaMask(Object graphics, TextureAlphaMask mask) {
        
        TextureAlphaMask nt = (TextureAlphaMask)mask;
        NativeGraphics ng = (NativeGraphics)graphics;
        ng.checkControl();
        ng.applyTransform();
        ng.applyClip();
        ng.nativeDrawAlphaMask(nt);
        
    }

    /**
     * Checks to see if alpha masks are supported.  If alpha masks are supported, then {@link com.codename1.ui.Graphics#drawShape}
     * will try to first convert the shape to a platform-specific alpha mask (which can be cached) and then draw the alpha mask.
     * @param graphics The graphics context.
     * @return True if alpha masks are supported.
     * @see #createAlphaMask
     * @see #deleteAlphaMask
     * @see #drawAlphaMask
     * @see com.codename1.ui.Graphics#drawAlphaMask 
     * @see com.codename1.ui.GeneralPath#getAlphaMask
     */
    public boolean isAlphaMaskSupported(Object graphics) {
        return ((NativeGraphics)graphics).isAlphaMaskSupported();
    }
    
    void nativeDeleteTexture(long textureID){
        nativeInstance.nativeDeleteTexture(textureID);
    }
    /**
     * Draws the outline of a shape in the given graphics context.
     * @param graphics the graphics context
     * @param shape The shape to be drawn.
     */
    @Override
    public void drawShape(Object graphics, Shape shape, Stroke stroke){// float lineWidth, int capStyle, int miterStyle, float miterLimit){
        
        NativeGraphics ng = (NativeGraphics)graphics;
        if ( ng.isShapeSupported()){
            ng.checkControl();
            ng.applyTransform();
            ng.applyClip();
            ng.nativeDrawShape(shape, stroke);
        }
    }
    
    /**
     * Draws a path on the current graphics context.
     * @param graphics the graphics context
     * @param path the path to draw.
     */
    @Override
    public void fillShape(Object graphics, Shape shape){
        NativeGraphics ng = (NativeGraphics)graphics;
        if ( ng.isShapeSupported()){
            ng.checkControl();
            ng.applyTransform();
            ng.applyClip();
            ng.nativeFillShape(shape);
        }
        
        
    }

    @Override
    public boolean isDrawShadowSupported() {
        return true;
    }

    @Override
    public boolean isDrawShadowFast() {
        return false;
    }

    @Override
    public void drawShadow(Object graphics, Object image, int x, int y, int offsetX, int offsetY, int blurRadius, int spreadRadius, int color, float opacity) {
        NativeGraphics ng = (NativeGraphics)graphics;
        NativeImage ni = (NativeImage)image;
        if (ng.isDrawShadowSupported()) {
            ng.checkControl();
            ng.applyTransform();
            ng.applyClip();
            ng.nativeDrawShadow(ni.peer, x, y, offsetX, offsetY, blurRadius, spreadRadius, color, opacity);
        }

    }

    private void drawPath(NativePathRenderer r, int color, int alpha){
        this.nativeInstance.nativeDrawPath(color, alpha, r.ptr);
    }
    
    private void fillPathConsumer(PathIterator path, NativePathConsumer c){
        float p[] = new float[6];
        while ( !path.isDone()){
            int segment = path.currentSegment(p);
            switch ( segment ){
                case PathIterator.SEG_MOVETO:
                    c.moveTo(p[0], p[1]);
                    break;
                case PathIterator.SEG_LINETO:
                    c.lineTo(p[0], p[1]);
                    break;
                case PathIterator.SEG_QUADTO:
                    c.quadTo(p[0], p[1], p[2], p[3]);
                    break;
                case PathIterator.SEG_CUBICTO:
                    c.curveTo(p[0], p[1], p[2], p[3], p[4], p[5]);
                    break;
                case PathIterator.SEG_CLOSE:
                    c.close();
                    break;
            }
            path.next();
        }
        c.done();
        
    }

    @Override
    public Transform getTransform(Object graphics) {
        return ((NativeGraphics)graphics).transform.copy();
    }

    @Override
    public void getTransform(Object nativeGraphics, Transform t) {
        NativeGraphics ng = (NativeGraphics)nativeGraphics;
        if (ng.transform != null) {
            t.setTransform(ng.transform);
        } else {
            t.setIdentity();
        }
    }

    
    
    @Override
    public void setTransform(Object graphics, Transform transform) {
        NativeGraphics ng = (NativeGraphics)graphics;
        if (ng.transform != null) {
            if (transform == null) {
                ng.transform.setIdentity();
            } else {
                ng.transform.setTransform(transform);
            }
        } else {
            ng.transform = transform == null ? null : transform.copy();
        }
        ng.transformApplied = false;
        // The cached clip / inverseClip / inverseTransform are derived from
        // the current transform; replacing the transform leaves them
        // pointing at the previous transform's space. Subsequent draw ops
        // (e.g. fillRect or fillLinearGradient on the form Graphics) read
        // those caches via loadClipBounds / inverseClip and end up clipped
        // to the wrong region, which is why TransformRotation and
        // Scale/AffineScale produced empty top cells on iOS Metal while
        // the equivalent rotation via g.rotate (which DOES invalidate
        // these flags, line 5513) rendered correctly. Match the
        // rotate/scale/translate/resetAffine paths so the cache is rebuilt
        // before the next draw.
        ng.clipDirty = true;
        ng.inverseClipDirty = true;
        ng.inverseTransformDirty = true;
        ng.checkControl();
        ng.applyTransform();
    }
    
    public void setNativeTransformGlobal(Transform transform){
        Matrix t = (Matrix)transform.getNativeTransform();
        float[] m = t.getData();
        
        
        // Note that Matrix is stored in column-major format but GLKMatrix is stored in row-major
        // that's why we transpose it here.
        //Log.p("....Setting transform.....");
        nativeInstance.nativeSetTransform(
            m[0], m[4], m[8], m[12],
            m[1], m[5], m[9], m[13],
            m[2], m[6], m[10], m[14],
            m[3], m[7], m[11], m[15],
            0, 0
        );
    }
    
    public void setNativeTransformMutable(Transform transform){
        Matrix t = (Matrix)transform.getNativeTransform();
        float[] m = t.getData();
        
        
        // Note that Matrix is stored in column-major format but GLKMatrix is stored in row-major
        // that's why we transpose it here.
        //Log.p("....Setting transform.....");
        nativeInstance.nativeSetTransformMutable(
            m[0], m[4], m[8], m[12],
            m[1], m[5], m[9], m[13],
            m[2], m[6], m[10], m[14],
            m[3], m[7], m[11], m[15],
            0, 0
        );
    }

    @Override
    public boolean transformNativeEqualsImpl(Object t1, Object t2) {
        if ( t1 != null ){
            Matrix m1 = (Matrix)t1;
            Matrix m2 = (Matrix)t2;
            return m1.equals(m2);
        } else {
            return t2 == null;
        }
        
    }
    
    @Override
    public boolean isTransformSupported(Object graphics) {
        return ((NativeGraphics)graphics).isTransformSupported();
    }

    @Override
    public boolean isPerspectiveTransformSupported(Object graphics) {
        return ((NativeGraphics)graphics).isPerspectiveTransformSupported();
    }

    @Override
    public boolean isShapeSupported(Object graphics) {
        return ((NativeGraphics)graphics).isShapeSupported();
    }
    
    /**
     * A map to cache textures.
     */
    class TextureCache {
        /**
         * Stores weak references to TextureAlphaMask objects.
         */
        Map<Long, Object> textures = new HashMap<Long,Object>();
        
        /**
         * Gets the alpha mask for a given shape/stroke from the
         * texture cache.  The mask will be take the bounds of the provided
         * shape rather than the bounds of the original shape from which the
         * mask was created.
         * @param s The shape.
         * @param stroke The stroke.  If null, then it will get a fill alpha mask.
         * @return The alpha mask for the shape/stroke or null if it is not currently
         * in the cache.
         */
        TextureAlphaMaskProxy get(Shape s, Stroke stroke){
            long shapeID = getShapeID(s, stroke);
            Object out = textures.get(shapeID);
            if ( out != null ){
                
                out = Display.getInstance().extractHardRef(out);
                
                if ( out != null ){
                    TextureAlphaMask mask = (TextureAlphaMask)out;
                    Rectangle bounds = s.getBounds();
                    return new TextureAlphaMaskProxy(mask, bounds);
                    
                } else {
                    textures.remove(shapeID);
                }
            }
            return null;
        }
        
        /**
         * Adds a shape/stroke => TextureAlphaMask to the cache.
         * @param s The shape.
         * @param stroke The stroke.  Null for a fill alpha mask.
         * @param mask The alpha mask
         */
        void add(Shape s, Stroke stroke, TextureAlphaMask mask){
            long shapeID = getShapeID(s, stroke);
            textures.put(shapeID, Display.getInstance().createSoftWeakRef(mask));
            
        }
        
        /**
         * Generates a key to be used in the texture map for a given shape/stroke.
         * Shapes that are identical but just translated will have identical keys.  
         * The bounds will be adjusted as part of the {@link #get} method.
         * @param shape The shape for which to retrieve the mask.
         * @param stroke The stroke.  If null, then it is a fill mask.  Otherwise it
         * is a contour mask.
         * @return The string ID used in the map.
         */
        long getShapeID(Shape shape, Stroke stroke){
            long result = 17; // Prime number to start the hash computation

            float referenceX = 0;
            float referenceY = 0;
            boolean referencePointSet = false;

            PathIterator it = shape.getPathIterator();
            float[] buf = new float[6];

            result = 31 * result + it.getWindingRule();

            while (!it.isDone()){
                int type = it.currentSegment(buf);

                if (!referencePointSet && type != PathIterator.SEG_CLOSE) {
                    referencePointSet = true;
                    referenceX = buf[0];
                    referenceY = buf[1];
                }

                float tx, ty, tx2, ty2, tx3, ty3;

                switch (type) {
                    case PathIterator.SEG_MOVETO:
                        tx = buf[0] - referenceX;
                        ty = buf[1] - referenceY;
                        result = 31 * result + Float.floatToIntBits(tx);
                        result = 31 * result + Float.floatToIntBits(ty);
                        break;
                    case PathIterator.SEG_LINETO:
                        tx = buf[0] - referenceX;
                        ty = buf[1] - referenceY;
                        result = 31 * result + Float.floatToIntBits(tx);
                        result = 31 * result + Float.floatToIntBits(ty);
                        break;
                    case PathIterator.SEG_QUADTO:
                        tx = buf[0] - referenceX;
                        ty = buf[1] - referenceY;
                        tx2 = buf[2] - referenceX;
                        ty2 = buf[3] - referenceY;
                        result = 31 * result + Float.floatToIntBits(tx);
                        result = 31 * result + Float.floatToIntBits(ty);
                        result = 31 * result + Float.floatToIntBits(tx2);
                        result = 31 * result + Float.floatToIntBits(ty2);
                        break;
                    case PathIterator.SEG_CUBICTO:
                        tx = buf[0] - referenceX;
                        ty = buf[1] - referenceY;
                        tx2 = buf[2] - referenceX;
                        ty2 = buf[3] - referenceY;
                        tx3 = buf[4] - referenceX;
                        ty3 = buf[5] - referenceY;
                        result = 31 * result + Float.floatToIntBits(tx);
                        result = 31 * result + Float.floatToIntBits(ty);
                        result = 31 * result + Float.floatToIntBits(tx2);
                        result = 31 * result + Float.floatToIntBits(ty2);
                        result = 31 * result + Float.floatToIntBits(tx3);
                        result = 31 * result + Float.floatToIntBits(ty3);
                        break;
                    case PathIterator.SEG_CLOSE:
                        result = 31 * result + type;
                        break;
                }

                it.next();
            }

            if (stroke != null) {
                result = 31 * result + stroke.hashCode();
            }

            return result;
        }

    }
    
    
    // END SHAPES AND TRANSFORMATION CODE
    
    private void nativeDrawImageMutable(long peer, int alpha, int x, int y, int width, int height, int renderingHints) {
        nativeInstance.nativeDrawImageMutable(peer, alpha, x, y, width, height, renderingHints);
    }
    private void nativeDrawImageGlobal(long peer, int alpha, int x, int y, int width, int height, int renderingHints) {
        nativeInstance.nativeDrawImageGlobal(peer, alpha, x, y, width, height, renderingHints);
    }

    public void drawRGB(Object graphics, int[] rgbData, int offset, int x, int y, int w, int h, boolean processAlpha) {
        Object nativeImage = createImage(rgbData, w, h);
        drawImage(graphics, nativeImage, x, y);
    }

    public Object getNativeGraphics() {
        if(globalGraphics == null) {
            globalGraphics = new GlobalGraphics();
        }
        return globalGraphics;
    }

    public Object getNativeGraphics(Object image) {
        return ((NativeImage)image).getGraphics();
    }

    public int charsWidth(Object nativeFont, char[] ch, int offset, int length) {
        NativeFont fnt = f(nativeFont);
        return stringWidthNative(fnt.peer, new String(ch, offset, length));
    }

    @Override
    public boolean isBaselineTextSupported() {
        return true;
    }

    
    
    @Override
    public int getFontAscent(Object nativeFont) {
        NativeFont fnt = f(nativeFont);
        return fontAscentNative(fnt.peer);
    }

    @Override
    public int getFontDescent(Object nativeFont) {
        NativeFont fnt = f(nativeFont);
        return Math.abs(fontDescentNative(fnt.peer));
    }

    
    
    private NativeFont f(Object o) {
        if(o == null) {
            return (NativeFont)getDefaultFont();
        }
        return (NativeFont)o;
    }

    public int stringWidth(Object nativeFont, String str) {
        NativeFont fnt = f(nativeFont);
        return stringWidthNative(fnt.peer, str);
    }
    
    class FontStringCache {
        String txt;
        long peer;
        
        public FontStringCache(String t, long i) {
            txt = t;
            peer = i;
        }
        
        public int hashCode() {
            return txt.hashCode() + ((int)peer);
        }
        
        public boolean equals(Object o) {
            FontStringCache c = (FontStringCache)o;
            return c.peer == peer && txt.equalsIgnoreCase(c.txt);
        }
    }
    private Map<FontStringCache, Integer> stringWidthCache = new HashMap<FontStringCache, Integer>();
    private FontStringCache recycle = new FontStringCache("", 1);
    private int stringWidthNative(long peer, String str) {
        if(str.length() < 50) {
            // we don't need to allocate for the case of a cache hit
            recycle.peer = peer;
            recycle.txt = str;
            
            Integer i = stringWidthCache.get(recycle);
            if(i != null) {
                return i.intValue();
            }
            int val = nativeInstance.stringWidthNative(peer, str);
            FontStringCache c = new FontStringCache(str, peer);
            if (stringWidthCache.size() > 10000) {
                // If the cache grows too big, let's clear it out.
                // We could use a more advanced algorithm, but right now
                // I just want to fix possible memory leak.
                
                // Each FontStringCache object is 48 bytes.  So 48 x 10000 = 480K
                // So we will allow a maximum footprint of 480K for this cache.
                // When it reaches 480K, we'll just clear it out.
                stringWidthCache.clear();
            }
            stringWidthCache.put(c, new Integer(val));
            return val;
        }
        return nativeInstance.stringWidthNative(peer, str);
    }
    
    private int fontAscentNative(long peer){
        return nativeInstance.fontAscentNative(peer);
    }
    
    private int fontDescentNative(long peer){
        return nativeInstance.fontDescentNative(peer);
    }
    
    

    public int charWidth(Object nativeFont, char ch) {
        return f(nativeFont).charWidth(ch);
    }

    private int charWidthNative(long peer, char ch) {
        return nativeInstance.charWidthNative(peer, ch);
    }

    public int getHeight(Object nativeFont) {
        return getFontHeightNative(f(nativeFont).peer);
    }


    private int getFontHeightNative(long peer) {
        return nativeInstance.getFontHeightNative(peer);
    }

    public Object getDefaultFont() {
        if(defaultFont == null) {
            defaultFont = (NativeFont)createFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
        }
        return defaultFont;
    }

    private long createSystemFont(int face, int style, int size) {
        return nativeInstance.createSystemFont(face, style, size);
    }

    Map<NativeFont, Long> fontMap = new HashMap<NativeFont, Long>();
    
    public Object createFont(int face, int style, int size) {
        NativeFont fnt = new NativeFont();
        fnt.face = face;
        fnt.size = size;
        fnt.style = style;

        Long val = fontMap.get(fnt);
        if(val != null) {
            fnt.peer = val;
            return fnt;
        }
        
        fnt.peer = createSystemFont(face, style, size);
        
        return fnt;
    }

    public void setImageName(Object nativeImage, String name) { 
        nativeInstance.setImageName(((NativeImage)nativeImage).peer, name);
    }

    private long getResourceNSData(String resource) {
        StringTokenizer t = new StringTokenizer(resource, "/.");
        int cnt = t.countTokens();
        while(cnt > 2) {
            t.nextToken();
            cnt--;
        }
        String name = t.nextToken();
        String type = t.nextToken();
        int val = nativeInstance.getResourceSize(name, type);
        if(val <= 0) {
            return -1;
        }
        return IOSImplementation.nativeInstance.createNSDataResource(name, type);
    }
    
    public InputStream getResourceAsStream(Class cls, String resource) {
        // Flatten resources
        int lastSlash = resource.lastIndexOf("/");
        if ( lastSlash != -1 ){
            resource = resource.substring(lastSlash+1);
        }
        
        int val = nativeInstance.getResourceSize(resource, null);
        if(val <= 0) {
            return null;
        }
        return new BufferedInputStream(new NSFileInputStream(resource, null), resource);
    }

    // this might be accessed on multiple threads
    private Hashtable softReferenceMap = new Hashtable();
    public static void flushSoftRefMap() {
        instance.softReferenceMap = new Hashtable();
    }
    
    /**
     * Extracts the hard reference from the soft/weak reference given
     *
     * @param o the reference returned by createSoftWeakRef
     * @return the original object submitted or null
     */
    public Object extractHardRef(Object o) {
        /*SoftReference w = (SoftReference)o;
        if(w != null) {
            return w.get();
        }
        return null;*/
        if(o == null) {
            return null;
        }
        Object val = softReferenceMap.get(o);
        if(val != null) {
            return val;
        }
        return null;
    }

    public Object createSoftWeakRef(Object o) {
        Object key = new Object();
        if(o == null) {
            return key;
        }
        softReferenceMap.put(key, o);
        return key;
        //return new SoftReference(o);
    }

    class Loc extends LocationManager {
        private long peer;
        private boolean locationUpdating, backgroundLocationUpdating;
        private static final String PREFS_BACKGROUND_LOCATION_LISTENER_CLASS = "ios.backgroundLocationListener";
        private static final String PREFS_BACKGROUND_LOCATION_UPDATING = "ios.backgroundLocationUpdating";
        private static final String PREFS_GEOFENCE_LISTENER_CLASS = "ios.geofenceListenerClass";
        private LocationListener backgroundLocationListenerInstance;
        private Map<String,String> geofenceListeners;
        private Map<String,Long> geofenceExpirations;

        @Override
        public boolean isGPSDetectionSupported() {
            return true;
        }

        @Override
        public boolean isGPSEnabled() {
            return nativeInstance.isGPSEnabled();
        }
        
        
        
        
        protected void finalize() throws Throwable {
            //super.finalize();
            if(peer != 0) {
                nativeInstance.releasePeer(peer);
            }
        }
        
        LocationListener getBackgroundLocationListenerInstance() {
            if (backgroundLocationListenerInstance == null) {
                Class cls = getBackgroundLocationListener();
                if (cls != null) {
                    try {
                        backgroundLocationListenerInstance = (LocationListener)cls.newInstance();
                    } catch (Throwable t) {
                        Log.e(t);
                        throw new RuntimeException(t.getMessage());
                    }
                }
            }
            return backgroundLocationListenerInstance;
        }
        
        @Override
        public Class getBackgroundLocationListener() {
            Class superVal = super.getBackgroundLocationListener();
            if (superVal == null && !"".equals(Preferences.get(PREFS_BACKGROUND_LOCATION_LISTENER_CLASS, ""))) {
                String backgroundLocationListenerClassName = Preferences.get(PREFS_BACKGROUND_LOCATION_LISTENER_CLASS, "");
                try {
                    Class backgroundLocationListenerClass = (Class)Class.forName(backgroundLocationListenerClassName);
                    super.setBackgroundLocationListener(backgroundLocationListenerClass);
                } catch (Throwable t) {}
            }
            return super.getBackgroundLocationListener(); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void setBackgroundLocationListener(Class locationListener) {
            if (locationListener != null) {
                Preferences.set(PREFS_BACKGROUND_LOCATION_LISTENER_CLASS, locationListener.getCanonicalName());
            } else {
                Preferences.set(PREFS_BACKGROUND_LOCATION_LISTENER_CLASS, null);
            }
            super.setBackgroundLocationListener(locationListener); //To change body of generated methods, choose Tools | Templates.
        }
        
        private long getLocation() {
            if(peer < 0) {
                return peer;
            }
            if(peer == 0) {
                peer = nativeInstance.createCLLocation();
            }
            if(peer == 0) {
                peer = -1;
            }
            return peer;
        }

        /**
         * If the app is running in the background and a background listener
         * is registered, and active, then this will return the background listener
         * instance.  Otherwise this should return the regular location listener.
         * @return 
         */
        public LocationListener getActiveLocationListener() {
            if (Display.getInstance().isMinimized() 
                    && Preferences.get(PREFS_BACKGROUND_LOCATION_UPDATING, false)
                    && getBackgroundLocationListenerInstance() != null) {
                return getBackgroundLocationListenerInstance();
            } else {
                return getLocationListener();
            }
        }
        
        public LocationListener getLocationListener() {
            return super.getLocationListener();
        }
 
        @Override
        public Location getCurrentLocation() {
            long p = getLocation();
            if(p <= 0) {
                return null;
            }
            bindListener();
            Location l = new Location();
            long c = nativeInstance.getCurrentLocationObject(p);
            l.setAccuracy((float)nativeInstance.getLocationAccuracy(c));
            l.setAltitude(nativeInstance.getLocationAltitude(c));
            l.setDirection((float)nativeInstance.getLocationDirection(c));
            l.setLatitude(nativeInstance.getLocationLatitude(c));
            l.setLongitude(nativeInstance.getLocationLongtitude(c));
            if(nativeInstance.isGoodLocation(p)) {
                l.setStatus(LocationManager.AVAILABLE);
            } else {
                l.setStatus(LocationManager.TEMPORARILY_UNAVAILABLE);
            }
            l.setTimeStamp(nativeInstance.getLocationTimeStamp(c));
            l.setVelocity((float)nativeInstance.getLocationVelocity(c));
            nativeInstance.releasePeer(c);
            return l;
        }
        
        private boolean statusInitialized;
        
        public void setStatus() {
            if(!statusInitialized) {
                statusInitialized = true;
                if(nativeInstance.isGoodLocation(getLocation())) {
                    super.setStatus(AVAILABLE);
                } else {
                    super.setStatus(TEMPORARILY_UNAVAILABLE);
                }
            }
        }

        private Map<String,String> geofenceListeners() {
            if (geofenceListeners == null) {
                if (Storage.getInstance().exists("ios.geofenceListeners")) {
                    geofenceListeners = (Map)Storage.getInstance().readObject("ios.geofenceListeners");
                } else {
                    geofenceListeners = new HashMap<String,String>();
                }
            }
            return geofenceListeners;
        }
        
        private Map<String,Long> geofenceExpirations() {
            if (geofenceExpirations == null) {
                if (Storage.getInstance().exists("ios.geofenceExpirations")) {
                    geofenceExpirations = (Map)Storage.getInstance().readObject("ios.geofenceExpirations");
                } else {
                    geofenceExpirations = new HashMap<String,Long>();
                }
            }
            return geofenceExpirations;
        }
        
        private void synchronizeGeofenceListeners() {
            if (geofenceListeners != null) {
                Storage.getInstance().writeObject("ios.geofenceListeners", geofenceListeners);
            }
        }
        private void synchronizeGeofenceExpirations() {
            if (geofenceExpirations != null) {
                Storage.getInstance().writeObject("ios.geofenceExpirations", geofenceExpirations);
            }
        }
        
        GeofenceListener getGeofenceListener(String id) {
            if (geofenceListeners().containsKey(id)) {
                Class cls = null;
                try {
                    cls = Class.forName(geofenceListeners.get(id)); 
                    if (cls == null) {
                        return null;
                    }
                    return (GeofenceListener)cls.newInstance();
                } catch (Throwable t) {
                    Log.e(t);
                }
                
            }
            return null;
        }
        
        synchronized void clearExpiredGeofences() {
            List<String> toRemove = new ArrayList<String>();
            for (String id : geofenceExpirations().keySet()) {
                if (geofenceExpirations().get(id) < System.currentTimeMillis()) {
                    toRemove.add(id);
                }
            }
            for (String id : toRemove) {
                geofenceListeners().remove(id);
                geofenceExpirations().remove(id);
                nativeInstance.removeGeofencing(peer, id);
            }
            if (!toRemove.isEmpty()) {
                synchronizeGeofenceExpirations();
                synchronizeGeofenceListeners();
            }
            
        }
        
        @Override
        public void addGeoFencing(Class GeofenceListenerClass, Geofence gf) {
            clearExpiredGeofences();
            
            if (gf.getExpiration() > 0) {
                long expiresAt = System.currentTimeMillis() + gf.getExpiration();
                geofenceExpirations().put(gf.getId(), expiresAt);
                synchronizeGeofenceExpirations();
            }
            geofenceListeners().put(gf.getId(), GeofenceListenerClass.getCanonicalName());
            synchronizeGeofenceListeners();
            long p = getLocation();
            if (p <= 0) {
                throw new RuntimeException("Failed to load location manager.  Check that you have included all applicable location permissions.");
            }
            nativeInstance.addGeofencing(peer, gf.getLoc().getLatitude(), gf.getLoc().getLongitude(), gf.getRadius(), gf.getExpiration(), gf.getId());
            super.addGeoFencing(GeofenceListenerClass, gf); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void removeGeoFencing(String id) {
            geofenceListeners().remove(id);
            geofenceExpirations().remove(id);
            synchronizeGeofenceListeners();
            synchronizeGeofenceExpirations();
            long p = getLocation();
            if (p <= 0) {
                throw new RuntimeException("Failed to load location manager.  Check that you have included all applicable location permissions.");
            }
            nativeInstance.removeGeofencing(peer, id);
        }

        @Override
        public boolean isGeofenceSupported() {
            return true;
        }
        
        @Override
        protected void bindListener() {
            if(!locationUpdating) {
                long p = getLocation();
                if(p <= 0) {
                    return;
                }
                locationUpdating = true;
                int priority = LocationRequest.PRIORITY_MEDIUM_ACCUARCY;
                if (this.getRequest() != null) {
                    priority = this.getRequest().getPriority();
                }
                nativeInstance.startUpdatingLocation(p, priority);
            }
        }

        @Override
        protected void clearListener() {
            if(locationUpdating) {
                long p = getLocation();
                if(p <= 0) {
                    return;
                }
                locationUpdating = false;
                nativeInstance.stopUpdatingLocation(p);
            }
        }
        
        @Override
        protected void bindBackgroundListener() {
            //boolean backgroundLocationUpdatingPref = Preferences.get(PREFS_BACKGROUND_LOCATION_UPDATING, false);
            if (!backgroundLocationUpdating) {
                long p = getLocation();
                if(p <= 0) {
                    return;
                }
                Preferences.set(PREFS_BACKGROUND_LOCATION_UPDATING, true);
                backgroundLocationUpdating = true;
                nativeInstance.startUpdatingBackgroundLocation(p);
            }
        }
        
        /**
         * Method called specially when the app is started with the significant
         * location change service.  It shoudl start up the location listener
         * to receive location updates while in the background.
         */
        void startBackgroundListener() {
            // This should kick start the background listener
            // and significant change service.
            getBackgroundLocationListenerInstance();
            
        }

        @Override
        protected void clearBackgroundListener() {
            //boolean backgroundLocationUpdating = Preferences.get(PREFS_BACKGROUND_LOCATION_UPDATING, false);
            if(backgroundLocationUpdating) {
                long p = getLocation();
                if(p <= 0) {
                    return;
                }
                Preferences.set(PREFS_BACKGROUND_LOCATION_UPDATING, false);
                backgroundLocationUpdating = false;
                nativeInstance.stopUpdatingBackgroundLocation(p);
            }
        }

        @Override
        public boolean isBackgroundLocationSupported() {
            return true;
        }
        
        @Override
        public Location getLastKnownLocation() {
            return getCurrentLocation();
        }
    }
    
    private static Loc lm;

    /**
     * Callback for native
     */
    public static void locationUpdate() {
        if(lm != null) {
            final LocationListener ls = lm.getActiveLocationListener();
            lm.setStatus();
            if(ls != null) {
                Display.getInstance().callSerially(new Runnable() {
                    @Override
                    public void run() {
                        ls.locationUpdated(lm.getCurrentLocation());
                    }
                });
            }
        }
    }
    
    public static void onGeofenceEnter(final String id) {
        if (lm != null) {
            final GeofenceListener ls = lm.getGeofenceListener(id);
            if (ls != null) {
                Display.getInstance().callSerially(new Runnable() {

                    @Override
                    public void run() {
                        ls.onEntered(id);
                    }
                    
                });
            }
            lm.clearExpiredGeofences();
        }
    }
    
    public static void onGeofenceExit(final String id) {
        if (lm != null) {
            final GeofenceListener ls = lm.getGeofenceListener(id);
            if (ls != null) {
                Display.getInstance().callSerially(new Runnable() {

                    @Override
                    public void run() {
                        ls.onExit(id);
                    }
                    
                });
            }
            lm.clearExpiredGeofences();
        }
    }
    
    public static void appDidLaunchWithLocation() {
        ((Loc)LocationManager.getLocationManager()).startBackgroundListener();
        
    }
    
    private IOSBiometrics biometrics;
    private IOSSecureStorage secureStorage;
    private IOSNfc nfc;
    private IOSDeviceIntegrity deviceIntegrity;

    @Override
    public com.codename1.security.Biometrics getBiometrics() {
        if (biometrics == null) {
            biometrics = new IOSBiometrics(nativeInstance);
        }
        return biometrics;
    }

    @Override
    public boolean isAttestationSupported() {
        return nativeInstance.isAppAttestSupported();
    }

    @Override
    public com.codename1.util.AsyncResource<String> requestIntegrityToken(String nonce) {
        if (deviceIntegrity == null) {
            deviceIntegrity = new IOSDeviceIntegrity(nativeInstance);
        }
        return deviceIntegrity.requestToken(nonce);
    }

    @Override
    public com.codename1.security.SecureStorage getSecureStorage() {
        if (secureStorage == null) {
            secureStorage = new IOSSecureStorage(nativeInstance);
        }
        return secureStorage;
    }

    @Override
    public com.codename1.nfc.Nfc getNfc() {
        if (nfc == null) {
            nfc = new IOSNfc(nativeInstance);
        }
        return nfc;
    }

    public LocationManager getLocationManager() {
        if (!nativeInstance.checkLocationUsage()) {
            throw new RuntimeException("Please add the ios.NSLocationUsageDescription or ios.NSLocationAlwaysUsageDescription build hint");
        }
        synchronized (IOSImplementation.class) {
            if (lm == null) {
                lm = new Loc();
            }
            return lm;
        }
    }

    /**
     * @inheritDoc
     */
    public String getMediaRecorderingMimeType() {
        return "audio/aac";
    }
    
    /**
     * Callback for the native layer
     */
    public static void capturePictureResult(String r) {
        dropEvents = false;
        if(captureCallback != null) {
            if(r != null) {
                if (gallerySelectMultiple) {
                    String[] paths = Util.split(r, "\n");
                    int len = paths.length;
                    for (int i=0; i<len; i++) {
                        if (!paths[i].startsWith("file:")) {
                            paths[i] = "file:"+paths[i];
                        }
                    }
                    captureCallback.fireActionEvent(new ActionEvent(paths));
                } else {
                    if(r.startsWith("file:")) {
                        captureCallback.fireActionEvent(new ActionEvent(r));
                    } else {
                        captureCallback.fireActionEvent(new ActionEvent("file:" + r));
                    }
                }
            } else {
                captureCallback.fireActionEvent(new ActionEvent(null));
            }
            captureCallback = null;
        }
    }
    
    
    public void captureAudio(ActionListener response) {
        if (!nativeInstance.checkMicrophoneUsage()) {
            throw new RuntimeException("Please add the ios.NSMicrophoneUsageDescription build hint");
        }
        dropEvents = false;
        String p = FileSystemStorage.getInstance().getAppHomePath();
        if(!p.endsWith("/")) {
            p += "/";
        }
        try {
            final Media media = MediaManager.createMediaRecorder(p + "cn1TempAudioFile", MediaManager.getAvailableRecordingMimeTypes()[0]);
            media.play();

            boolean b = Dialog.show("Recording", "", "Save", "Cancel");
            final Dialog d = new Dialog("Recording");

            media.pause();
            media.cleanup();
            d.dispose();
            if(b) {
                response.actionPerformed(new ActionEvent(p + "cn1TempAudioFile"));
            } else {
                FileSystemStorage.getInstance().delete(p + "cn1TempAudioFile");
                response.actionPerformed(null);
            }
        } catch(IOException err) {
            err.printStackTrace();
            response.actionPerformed(null);
        }
    }

    /**
     * Callback for the native layer
     */
    public static void captureMovieResult(String r) {
        dropEvents = false;
        capturePictureResult(r);
    }
    
    private static EventDispatcher captureCallback;
    
    /**
     * Captures a photo and notifies with the image data when available
     * @param response callback for the resulting image
     */
    public void capturePhoto(ActionListener response) {
        if (!nativeInstance.checkCameraUsage()) {
            throw new RuntimeException("Please add the ios.NSCameraUsageDescription build hint");
        }
        gallerySelectMultiple = false;
        captureCallback = new EventDispatcher();
        captureCallback.addListener(response);
        nativeInstance.captureCamera(false, 0, 0);
        dropEvents = true;
    }

    @Override
    public com.codename1.impl.CameraImpl createCameraImpl() {
        return new IOSCameraImpl();
    }

    @Override
    public String [] getAvailableRecordingMimeTypes() {
        // All of these amount to the same thing.
        // We record in AAC format, wrapped in an mp4 container.
        return new String[]{"audio/mp4", "audio/aac", "audio/m4a"};
    }
    
    private static boolean finishedCreatingAudioRecorder;
    private static Object createAudioRecorderLock = new Object();
    private static IOException createAudioRecorderException = null;
    
    public static void finishedCreatingAudioRecorder(IOException ex) {
        createAudioRecorderException = ex;
        finishedCreatingAudioRecorder = true;
        synchronized(createAudioRecorderLock) {
            createAudioRecorderLock.notifyAll();
        }
    }

    @Override
    public Media createMediaRecorder(MediaRecorderBuilder builder) throws IOException {
        return createMediaRecorder(builder.getPath(), builder.getMimeType(), builder.getSamplingRate(), builder.getBitRate(), builder.getAudioChannels(), 0, builder.isRedirectToAudioBuffer());
    }
    
    
    
    @Override
    public Media createMediaRecorder(final String path, final String mimeType) throws IOException {
        MediaRecorderBuilder builder = new MediaRecorderBuilder()
                .path(path)
                .mimeType(mimeType);
        return createMediaRecorder(builder);
    }
    

    private  Media createMediaRecorder(final String path, final String mimeType, final int sampleRate, final int bitRate, final int audioChannels, final int maxDuration, final boolean redirectToAudioBuffer) throws IOException {
        if (!nativeInstance.checkMicrophoneUsage()) {
            throw new RuntimeException("Please add the ios.NSMicrophoneUsageDescription build hint");
        }
        if (redirectToAudioBuffer) {
            AudioBuffer buf = MediaManager.getAudioBuffer(path, true, 4096);
            return new AbstractMedia() {
                long peer = nativeInstance.createAudioUnit(path, audioChannels, sampleRate, new float[64]);
                boolean isPlaying;
                @Override
                protected void playImpl() {
                    isPlaying = true;
                    nativeInstance.startAudioUnit(peer);
                    fireMediaStateChange(State.Playing);
                }

                @Override
                protected void pauseImpl() {
                    isPlaying = false;
                    nativeInstance.stopAudioUnit(peer);
                    fireMediaStateChange(State.Paused);
                }

                @Override
                public void prepare() {
                    
                }

                @Override
                public void cleanup() {
                    if (peer == 0) {
                        return;
                    }
                    if (isPlaying) {
                        pauseImpl();
                    }
                    MediaManager.releaseAudioBuffer(path);
                    nativeInstance.destroyAudioUnit(peer);
                }

                @Override
                public int getTime() {
                    return -1;
                }

                @Override
                public void setTime(int time) {
                    
                }

                @Override
                public int getDuration() {
                    return -1;
                }

                @Override
                public void setVolume(int vol) {
                    
                }

                @Override
                public int getVolume() {
                    return -1;
                }

                @Override
                public boolean isPlaying() {
                    return isPlaying;
                }

                @Override
                public Component getVideoComponent() {
                    return null;
                }

                @Override
                public boolean isVideo() {
                    return false;
                }

                @Override
                public boolean isFullScreen() {
                    return false;
                }

                @Override
                public void setFullScreen(boolean fullScreen) {
                    
                }

                @Override
                public void setNativePlayerMode(boolean nativePlayer) {
                    
                }

                @Override
                public boolean isNativePlayerMode() {
                    return false;
                }

                @Override
                public void setVariable(String key, Object value) {
                    
                }

                @Override
                public Object getVariable(String key) {
                    return null;
                }
                
            };
        }
        
        finishedCreatingAudioRecorder = false;
        createAudioRecorderException = null;
        final long[] peer = new long[] { nativeInstance.createAudioRecorder(path, mimeType, sampleRate, bitRate, audioChannels, maxDuration) };
        Display.getInstance().invokeAndBlock(new Runnable() {
            public void run() {
                while (!finishedCreatingAudioRecorder) {
                    synchronized(createAudioRecorderLock) {
                        Util.wait(createAudioRecorderLock);
                    }
                }
            }
        });
        if (createAudioRecorderException != null) {
            throw createAudioRecorderException;
        }
        return new AbstractMedia() {
            private boolean playing;
            @Override
            protected void playImpl() {
                if(peer[0] != 0) {
                    nativeInstance.startAudioRecord(peer[0]);
                    playing = true;
                    fireMediaStateChange(State.Playing);
                }
            }

            @Override
            protected void pauseImpl() {
                if(peer[0] != 0) {
                    nativeInstance.pauseAudioRecord(peer[0]);
                    playing = false;
                    fireMediaStateChange(State.Paused);
                }
            }
            
            protected void finalize() {
                if(peer[0] != 0) {
                    cleanup();
                }
            }

            @Override
            public void cleanup() {
                if(playing) {
                    nativeInstance.pauseAudioRecord(peer[0]);
                    fireMediaStateChange(State.Paused);
                }
                nativeInstance.cleanupAudioRecord(peer[0]);
                peer[0] = 0;
            }

            @Override
            public int getTime() {
                return -1;
            }

            @Override
            public void setTime(int time) {
            }

            @Override
            public int getDuration() {
                return -1;
            }

            @Override
            public void setVolume(int vol) {
            }

            @Override
            public int getVolume() {
                return -1;
            }

            @Override
            public boolean isPlaying() {
                return playing;
            }

            @Override
            public Component getVideoComponent() {
                return null;
            }

            @Override
            public boolean isVideo() {
                return false;
            }

            @Override
            public boolean isFullScreen() {
                return false;
            }

            @Override
            public void setFullScreen(boolean fullScreen) {
            }

            @Override
            public void setNativePlayerMode(boolean nativePlayer) {
            }

            @Override
            public boolean isNativePlayerMode() {
                return false;
            }

            public void setVariable(String key, Object value) {
            }

            public Object getVariable(String key) {
                return null;
            }

            public void prepare() {
            }
        };
    }
    
    /**
     * Captures a video and notifies with the data when available
     * @param response callback for the resulting video
     */
    public void captureVideo(ActionListener response) {
        captureVideo(null, response);
    }
        
    /**
     * Captures a video and notifies with the data when available
     * @param response callback for the resulting video
     */
    public void captureVideo(VideoCaptureConstraints cnst, ActionListener response) {
        if (!nativeInstance.checkCameraUsage() || !nativeInstance.checkMicrophoneUsage()) {
            throw new RuntimeException("Please add the ios.NSCameraUsageDescription and ios.NSMicrophoneUsageDescription build hints");
        }
        gallerySelectMultiple = false;
        captureCallback = new EventDispatcher();
        captureCallback.addListener(response);
        nativeInstance.captureCamera(true, getUIPickerControllerQualityType(cnst), cnst != null ? cnst.getPreferredMaxLength() : 0);
        dropEvents = true;
    }
    
    private static int getUIPickerControllerQualityType(VideoCaptureConstraints cnst) {
        if (cnst == null) {
            return 1; //UIImagePickerControllerQualityTypeMedium = 1
        }
        int w = cnst.getWidth();
        int h = cnst.getHeight();
        if (w == 640 && h == 480) {
            return 3; //UIImagePickerControllerQualityType640x480 = 3
        }
        if (w == 1280 && h == 720) {
            return 4; //UIImagePickerControllerQualityTypeIFrame1280x720 = 4
        }
        if (w == 960 && h == 540) {
            return 5; //UIImagePickerControllerQualityTypeIFrame960x540 = 5
        }
        int quality = cnst.getQuality();
        switch (quality) {
            case VideoCaptureConstraints.QUALITY_LOW:
                return 2; //UIImagePickerControllerQualityTypeLow = 2
            case VideoCaptureConstraints.QUALITY_HIGH:
                return 0; //UIImagePickerControllerQualityTypeHigh = 0
            default:
                return 1; //UIImagePickerControllerQualityTypeMedium = 1
        }
    }
    


    @Override
    public void openImageGallery(ActionListener response) {    
        openGallery(response, Display.GALLERY_IMAGE);
    }

    @Override
    public boolean isGalleryTypeSupported(int type) {
        if (super.isGalleryTypeSupported(type)) {
            return true;
        }
        if (type == -9999) {
            return true;
        }
        switch (type) {
            case -9998:
            case Display.GALLERY_ALL_MULTI:
            case Display.GALLERY_IMAGE_MULTI:
            case Display.GALLERY_VIDEO_MULTI:
                return nativeInstance.isMultiGallerySelectSupported();
        }
        return false;
    }
    
    

    @Override
    public void openGallery(ActionListener response, int type) {
        if (!isGalleryTypeSupported(type)) {
            throw new IllegalArgumentException("Gallery type "+type+" not supported on this platform.");
        }
        if (!nativeInstance.checkPhotoLibraryUsage()) {
            throw new RuntimeException("Please add the ios.NSPhotoLibraryUsageDescription build hint");
        }
        switch (type) {
            case -9998:
            case Display.GALLERY_ALL_MULTI:
            case Display.GALLERY_IMAGE_MULTI:
            case Display.GALLERY_VIDEO_MULTI:
                gallerySelectMultiple = true;
                break;
            default:
                gallerySelectMultiple = false;
                
        }
        captureCallback = new EventDispatcher();
        captureCallback.addListener(response);
        nativeInstance.openGallery(type);
    }
    
    
    
    static class IOSMediaCallback {
        Runnable onCompletion;
        long nsObserverPeer;
        
    }
    
    /**
     * Map of media callbacks.  This allows onCompletion callbacks to be fired
     * from native code.
     */
    final HashMap<Integer,IOSMediaCallback> mediaCallbacks = new HashMap<Integer,IOSMediaCallback>();
    
    /**
     * Serial id for media callbacks
     */
    int nextMediaCallbackId = 1;
    
    /**
     * Registers a media callback and assigns it an ID.
     * @param r The callback associated with the given id.
     * @return An ID that can be used from {@link #fireMediaCallback} to execute
     * the callback.
     */
    int registerMediaCallback(Runnable r) {
        if (r != null) {
            IOSMediaCallback cb = new IOSMediaCallback();
            cb.onCompletion = r;
            synchronized(instance.mediaCallbacks) {
                int id = instance.nextMediaCallbackId++;
                instance.mediaCallbacks.put(id, cb);
                return id;
            }
        }
        return 0;
    }
    
    /**
     * Called from native code to fire media callback.
     * @param id ID that was assigned in {@link #registerMediaCallback(java.lang.Runnable) }
     */
    static void fireMediaCallback(int id) {
        IOSMediaCallback cb = instance.mediaCallbacks.get(id);
        if (cb != null) {
            Display.getInstance().callSerially(cb.onCompletion);
        }
    }
    
    /**
     * Removes a media callback
     * @param id ID of the media callback to remove.  Generated by the {@link #registerMediaCallback(java.lang.Runnable) } method.
     */
    void removeMediaCallback(int id) {
        IOSMediaCallback cb = null;
        synchronized(mediaCallbacks) {
            cb = mediaCallbacks.get(id);
            mediaCallbacks.remove(id);
        }
        if (cb != null && cb.nsObserverPeer != 0) {
            // TODO.. implement this... need to remove the observer
            nativeInstance.removeNotificationCenterObserver(cb.nsObserverPeer);
        }
    }
    
    /**
     * Called from native code to bind an opaque objective-c object that is
     * the registered observer from NSNotificationCenter with the ID of
     * the callback that it calls.  This allows it to later be removed
     * from the Java side.
     * @param callbackId The callback ID of the media callback (as generated by {@link #registerMediaCallback(java.lang.Runnable) }
     * @param nsObserverPeer The Objective-C observer that was registered with NSNotificationCenter
     */
    static void bindNSObserverPeerToMediaCallback(long nsObserverPeer, int callbackId) {
        IOSMediaCallback cb = instance.mediaCallbacks.get(callbackId);
        if (cb != null) {
            cb.nsObserverPeer = nsObserverPeer;
        }
    }
    // To prevent media from being GC'd before they are finished playing
    // https://github.com/codenameone/CodenameOne/issues/2380
    private List<IOSMedia> activeMedia;
    
    class IOSMedia extends AbstractMedia {
        private String uri;
        private boolean isVideo;
        //private Runnable onCompletion;
        int onCompletionCallbackId;
        private InputStream stream;
        private String mimeType;
        private PeerComponent component;
        private boolean nativePlayer;
        private long moviePlayerPeer;
        private boolean fullScreen;
        private boolean embedNativeControls=true;
        private List<Runnable> completionHandlers;
        private boolean prepareToPlay;
        
        
        
        public IOSMedia(String uri, boolean isVideo, Runnable onCompletion) {
            this.uri = uri;
            this.isVideo = isVideo;
            if (onCompletion != null) {
                addCompletionHandler(onCompletion);
            }
            onCompletion = new Runnable() {

                @Override
                public void run() {
                    unmarkActive();
                    fireMediaStateChange(State.Paused);
                    fireCompletionHandlers();
                    
                }
                
            };
            this.onCompletionCallbackId = registerMediaCallback(onCompletion);
            if(!isVideo) {
                moviePlayerPeer = nativeInstance.createAudio(uri, onCompletion);
            }
        }

        public IOSMedia(InputStream stream, String mimeType, Runnable onCompletion) {
            this.stream = stream;
            this.mimeType = mimeType;
            if (onCompletion != null) {
                addCompletionHandler(onCompletion);
            }
            onCompletion = new Runnable() {

                @Override
                public void run() {
                    unmarkActive();
                    fireMediaStateChange(State.Paused);
                    fireCompletionHandlers();
                    
                }
                
            };
            this.onCompletionCallbackId = registerMediaCallback(onCompletion);            
            isVideo = mimeType.indexOf("video") > -1;
            if(!isVideo) {
                try {
                    moviePlayerPeer = nativeInstance.createAudio(Util.readInputStream(stream), onCompletion);
                    nativeInstance.retainPeer(moviePlayerPeer);
                } catch (final IOException ex) {
                    ex.printStackTrace();
                    CN.callSerially(new Runnable() {
                        public void run() {
                            fireMediaError(new MediaException(MediaErrorType.Network, ex));
                        }
                    });
                }
            }
            
        }
        
        private void markActive() {
            if (activeMedia == null) {
                activeMedia = Collections.synchronizedList(new ArrayList<IOSMedia>());
            }
            // Prevent premature GC
            // https://github.com/codenameone/CodenameOne/issues/2380
            activeMedia.add(this);
        }
        
        private void fireCompletionHandlers() {
            if (completionHandlers != null && !completionHandlers.isEmpty()) {
                Display.getInstance().callSerially(new Runnable() {

                    @Override
                    public void run() {
                        if (completionHandlers != null && !completionHandlers.isEmpty()) {
                            List<Runnable>  toRun;

                            synchronized(IOSMedia.this) {
                                toRun = new ArrayList<Runnable>(completionHandlers);
                            }
                            for (Runnable r : toRun) {
                                r.run();
                            }
                        }
                    }

                });
            }
        }

        public void addCompletionHandler(Runnable onCompletion) {
            synchronized(this) {
                if (completionHandlers == null) {
                    completionHandlers = new ArrayList<Runnable>();
                }

                completionHandlers.add(onCompletion);
            }
        }

        public void removeCompletionHandler(Runnable onCompletion) {
            if (completionHandlers != null) {
                synchronized(this) {
                    completionHandlers.remove(onCompletion);
                }
            }
        }
        
        @Override
        protected void playImpl() {
            if(isVideo) {
                if(component == null && nativePlayer) {
                    // Mass source of confusion.  If getVideoComponent() has been called, then
                    // we can't use the native player.
                    if(uri != null) {
                        moviePlayerPeer = nativeInstance.createNativeVideoComponent(uri, onCompletionCallbackId);
                    } else {
                        try {
                            long val = getNSData(stream);
                            if(val > 0) {
                                moviePlayerPeer = nativeInstance.createNativeVideoComponentNSData(val, onCompletionCallbackId);
                                Util.cleanup(stream);
                            } else {
                                byte[] data = Util.readInputStream(stream);
                                Util.cleanup(stream);
                                moviePlayerPeer = nativeInstance.createNativeVideoComponent(data, onCompletionCallbackId);
                            }
                        } catch (IOException ex) {
                            fireMediaError(new MediaException(MediaErrorType.Decode, ex));
                        }
                    }
                    nativeInstance.showNativePlayerController(moviePlayerPeer);
                }
                if(moviePlayerPeer != 0) {
                    nativeInstance.startVideoComponent(moviePlayerPeer);
                }
            } else {
                nativeInstance.playAudio(moviePlayerPeer);                
            }
            markActive();
            fireMediaStateChange(State.Playing);
        }

        private void unmarkActive() {
            if (activeMedia != null && activeMedia.contains(this)) {
                activeMedia.remove(this);
            }
        }
        
        @Override
        protected void pauseImpl() {
            if(moviePlayerPeer != 0) {
                if(isVideo) {
                    nativeInstance.pauseVideoComponent(moviePlayerPeer);
                } else {
                    nativeInstance.pauseAudio(moviePlayerPeer);
                }
            }
            unmarkActive();
            fireMediaStateChange(State.Paused);
        }

        public void prepare() {
            prepareToPlay = true;
            if(moviePlayerPeer != 0) {
                if(isVideo) {
                    nativeInstance.prepareVideoComponent(moviePlayerPeer);
                }
            }
        }
        
        @Override
        public void cleanup() {
            if(moviePlayerPeer != 0) {
                pause();
                if(!isVideo) {
                    nativeInstance.cleanupAudio(moviePlayerPeer);
                    moviePlayerPeer = 0;
                }
                removeMediaCallback(onCompletionCallbackId);
                // SJH Nov. 13, 2015:  Uncommenting this because it seems that 
                // we do need to release the peer when we're cleaning up.
                if (isVideo) {
                    nativeInstance.releasePeer(moviePlayerPeer);
                    moviePlayerPeer = 0;
                }
                unmarkActive();
            }
            
        }
        
        protected void finalize() {
            cleanup();
        }

        @Override
        public int getTime() {
            if(moviePlayerPeer != 0) {
                if(isVideo) {
                    return nativeInstance.getMediaTimeMS(moviePlayerPeer);
                } else {
                    return nativeInstance.getAudioTime(moviePlayerPeer);
                }
            }
            return 0;
        }

        @Override
        public void setTime(int time) {
            if(moviePlayerPeer != 0) {
                if(isVideo) {
                    nativeInstance.setMediaTimeMS(moviePlayerPeer, time);
                } else {
                    nativeInstance.setAudioTime(moviePlayerPeer, time);
                }
            }
        }

        @Override
        public int getDuration() {
            if(moviePlayerPeer != 0) {
                if(isVideo) {
                    return nativeInstance.getMediaDuration(moviePlayerPeer);
                } else {
                    return nativeInstance.getAudioDuration(moviePlayerPeer);
                }
            }
            return 0;
        }

        @Override
        public void setVolume(int vol) {
            nativeInstance.setVolume(((float)vol) / 100);
        }

        @Override
        public int getVolume() {
            return (int)(nativeInstance.getVolume() * 100);
        }

        @Override
        public boolean isPlaying() {
            if(moviePlayerPeer != 0) {
                if(isVideo) {
                    return nativeInstance.isVideoPlaying(moviePlayerPeer);
                } else {
                    return nativeInstance.isAudioPlaying(moviePlayerPeer);
                }
            }
            return false;
        }

        @Override
        public Component getVideoComponent() {
            if (component == null) {
                if(uri != null) {
                    moviePlayerPeer = nativeInstance.createVideoComponent(uri, onCompletionCallbackId);
                    nativeInstance.setNativeVideoControlsEmbedded(moviePlayerPeer, embedNativeControls);
                    component = PeerComponent.create(new long[] { nativeInstance.getVideoViewPeer(moviePlayerPeer) });
                } else {
                    try {
                        byte[] data = toByteArray(stream);
                        Util.cleanup(stream);
                        moviePlayerPeer = nativeInstance.createVideoComponent(data, onCompletionCallbackId);
                        nativeInstance.setNativeVideoControlsEmbedded(moviePlayerPeer, embedNativeControls);
                        component = PeerComponent.create(new long[] { nativeInstance.getVideoViewPeer(moviePlayerPeer) });
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        fireMediaError(new MediaException(MediaErrorType.Decode, ex));
                        return new Label("Error loading video " + ex);
                    }
                }
            }
            if (prepareToPlay && isVideo && !isPlaying()) {
                prepare();
            }
            return component;
        }

        @Override
        public boolean isVideo() {
            return isVideo;
        }

        @Override
        public boolean isFullScreen() {
            long p = get(component);
            if(p != 0) {
                return nativeInstance.isVideoFullScreen(p);
            }
            return false;
        }

        @Override
        public void setFullScreen(boolean fullScreen) {
            this.fullScreen = fullScreen;
            long p = get(component);
            if(p != 0) {
                nativeInstance.setVideoFullScreen(p, fullScreen);
            }
        }

        @Override
        public void setNativePlayerMode(boolean nativePlayer) {
            this.nativePlayer = nativePlayer;
        }

        @Override
        public boolean isNativePlayerMode() {
            return nativePlayer;
        }

        public void setVariable(String key, Object value) {
            if(key.equals(Media.VARIABLE_BACKGROUND_ALBUM_COVER)) {
                NativeImage ni = (NativeImage)((Image)value).getImage();
                nativeInstance.setMediaBgAlbumCover(ni.peer);
                return;
            }
            if(key.equals(Media.VARIABLE_BACKGROUND_ARTIST)) {
                nativeInstance.setMediaBgArtist((String)value);
                return;
            }
            if(key.equals(Media.VARIABLE_BACKGROUND_DURATION)) {
                nativeInstance.setMediaBgDuration(((Long)value).longValue());
                return;
            }
            if(key.equals(Media.VARIABLE_BACKGROUND_POSITION)) {
                nativeInstance.setMediaBgPosition(((Long)value).longValue());
                return;
            }
            if(key.equals(Media.VARIABLE_BACKGROUND_TITLE)) {
                nativeInstance.setMediaBgTitle((String)value);
            }
            if(Media.VARIABLE_NATIVE_CONTRLOLS_EMBEDDED.equals(key) && value instanceof Boolean) {
                embedNativeControls = (Boolean)value;
                if (moviePlayerPeer != 0) {
                    nativeInstance.setNativeVideoControlsEmbedded(moviePlayerPeer, (Boolean)value);
                }
            }
        }

        public Object getVariable(String key) {
            if(Media.VARIABLE_BACKGROUND_SUPPORTED.equals(key)) {
                return Boolean.TRUE;
            }
            return null;
        }
    }
    
    public Media createMedia(String uri, boolean isVideo, Runnable onCompletion) throws IOException {
        return new IOSMedia(uri, isVideo, onCompletion);
    }

    @Override
    public void addCompletionHandler(Media media, Runnable onCompletion) {
        super.addCompletionHandler(media, onCompletion);
        if (media instanceof IOSMedia) {
            ((IOSMedia)media).addCompletionHandler(onCompletion);
        }
    }

    @Override
    public void removeCompletionHandler(Media media, Runnable onCompletion) {
        super.removeCompletionHandler(media, onCompletion);
        if (media instanceof IOSMedia) {
            ((IOSMedia)media).removeCompletionHandler(onCompletion);
        }
    }

    
    

    public Media createMedia(InputStream stream, String mimeType, Runnable onCompletion) throws IOException {
        return new IOSMedia(stream, mimeType, onCompletion);
    }

    @Override
    public boolean isSoundPoolSupported() {
        return true;
    }

    @Override
    public com.codename1.media.SoundPoolPeer createSoundPool(int maxStreams) {
        return new IOSSoundPool(maxStreams);
    }

    /// Native low latency sound pool peer backed by CN1SoundPool.m (an AVAudioPlayer
    /// ring per sound). Handles (pool, sound) are native pointers carried as longs.
    class IOSSoundPool implements com.codename1.media.SoundPoolPeer {
        private final long pool;
        private final int ringSize;

        IOSSoundPool(int maxStreams) {
            this.pool = nativeInstance.nativeCreateSoundPool(maxStreams);
            this.ringSize = Math.min(maxStreams, 4);
        }

        public Object loadSound(InputStream data, String mimeType) throws IOException {
            byte[] bytes = com.codename1.io.Util.readInputStream(data);
            com.codename1.io.Util.cleanup(data);
            return Long.valueOf(nativeInstance.nativeLoadSound(pool, bytes, ringSize));
        }

        public Object loadSound(String uri) throws IOException {
            InputStream in = getResourceAsStream(getClass(), uri);
            if (in == null) {
                throw new IOException("sound not found: " + uri);
            }
            return loadSound(in, null);
        }

        private long sound(Object s) {
            return ((Long) s).longValue();
        }

        public int play(Object s, float volume, float pan, float rate, int loop) {
            return nativeInstance.nativePlaySound(pool, sound(s), volume, pan, rate, loop);
        }

        public void setVolume(int voiceId, float volume) {
            nativeInstance.nativeSetSoundVolume(pool, voiceId, volume);
        }

        public void setRate(int voiceId, float rate) {
            nativeInstance.nativeSetSoundRate(pool, voiceId, rate);
        }

        public void setPan(int voiceId, float pan) {
            nativeInstance.nativeSetSoundPan(pool, voiceId, pan);
        }

        public void pauseVoice(int voiceId) {
            nativeInstance.nativePauseSound(pool, voiceId);
        }

        public void resumeVoice(int voiceId) {
            nativeInstance.nativeResumeSound(pool, voiceId);
        }

        public void stopVoice(int voiceId) {
            nativeInstance.nativeStopSound(pool, voiceId);
        }

        public void stopAll() {
            nativeInstance.nativeStopAllSounds(pool);
        }

        public void autoPause() {
            nativeInstance.nativeAutoPauseSoundPool(pool);
        }

        public void autoResume() {
            nativeInstance.nativeAutoResumeSoundPool(pool);
        }

        public void unloadSound(Object s) {
            nativeInstance.nativeUnloadSound(pool, sound(s));
        }

        public void release() {
            nativeInstance.nativeReleaseSoundPool(pool);
        }
    }

    private static long createNativeMutableImage(int w, int h, int color) {
        return nativeInstance.createNativeMutableImage(w, h, color);
    }

    // should delete the old peer!
    private static void startDrawingOnImage(int w, int h, long peer) {
        nativeInstance.startDrawingOnImage(w, h, peer);
    }
    private static long finishDrawingOnImage() {
        return nativeInstance.finishDrawingOnImage();
    }

    private static void deleteNativePeer(long peer) {
        nativeInstance.deleteNativePeer(peer);
    }
    
    public boolean isAffineSupported() {
        return true;
    }

    public void resetAffine(Object nativeGraphics) {
        ((NativeGraphics)nativeGraphics).resetAffine();
    }

    public void scale(Object nativeGraphics, float x, float y) {
        ((NativeGraphics)nativeGraphics).scale(x, y);
    }

    public void rotate(Object nativeGraphics, float angle, int x, int y) {
        ((NativeGraphics)nativeGraphics).rotate(angle, x, y);
    }

    @Override
    public boolean isTranslateMatrixSupported() {
        // iOS dispatches translateMatrix into NativeGraphics.transform the
        // same way it dispatches scale/rotate, so the impl matrix sees the
        // translate as a real composition step.
        return true;
    }

    @Override
    public void translateMatrix(Object nativeGraphics, float x, float y) {
        ((NativeGraphics)nativeGraphics).translateMatrix(x, y);
    }

    @Override
    public boolean isTranslationSupported() {
        //return true;
        // We'll leave this as false until the next iteration...
        // ES2 should allow us to do all of this using transforms but
        // let's take small steps first
        return false;
    }

    public void shear(Object nativeGraphics, float x, float y) {
        ((NativeGraphics)nativeGraphics).shear(x, y);
    }


    /**
     * A utility class to encapsulate the Pisces Stroker.
     * @see Stroker.h and Stroker.c in nativeSources
     */
    static class NativePathStroker {
        
        static final int JOIN_MITER = 0;
        static final int JOIN_ROUND = 1;
        static final int JOIN_BEVEL = 2;
        static final int CAP_BUTT = 0;
        static final int CAP_ROUND = 1;
        static final int CAP_SQUARE = 2;
        
        /**
         * Pointer to the native Stroker struct.
         */
        final long ptr;
        final NativePathRenderer renderer;
        final NativePathConsumer consumer;
        
        /**
         * Creates a stroker with the given settings and renderer.
         * @param renderer
         * @param lineWidth
         * @param capStyle
         * @param joinStyle
         * @param miterLimit 
         */
        NativePathStroker(NativePathRenderer renderer, float lineWidth, int capStyle, int joinStyle, float miterLimit){
            ptr = nativeInstance.nativePathStrokerCreate(renderer.consumer.ptr, lineWidth, capStyle, joinStyle, miterLimit);
            this.renderer = renderer;
            this.consumer = new NativePathConsumer(nativeInstance.nativePathStrokerGetConsumer(ptr));
        }
        
        /**
         * Resets the stroker with the specified settings.
         * @param lineWidth
         * @param capStyle
         * @param joinStyle
         * @param miterLimit 
         */
        void reset(float lineWidth, int capStyle, int joinStyle, float miterLimit){
            nativeInstance.nativePathStrokerReset(ptr, lineWidth, capStyle, joinStyle, miterLimit);
        }
        
        /**
         * This should be called when the stroker is not needed anymore.
         * DON'T PUT THIS INSIDE finalize() because the stroker may need to 
         * outlive it's java wrapper in objective-c space.
         */
        void destroy(){
            nativeInstance.nativePathStrokerCleanup(ptr);
        }
        
        
        
        
    }
    
    /**
     * Encapsulates the pisces native path consumer for consuming paths.
     * See PathConsumer.h, Renderer.h, Renderer.c
     */
    static class NativePathConsumer {
        final long ptr;
        
        NativePathConsumer(long ptr){
            this.ptr = ptr;
        }
         public void moveTo(float x, float y){
            nativeInstance.nativePathConsumerMoveTo(ptr, x, y);
        }
        
        public void lineTo(float x, float y){
            nativeInstance.nativePathConsumerLineTo(ptr, x, y);
        }
        
        public void quadTo(float xc, float yc, float x1, float y1){
            nativeInstance.nativePathConsumerQuadTo(ptr, xc, yc, x1, y1);
        }
        
        public void curveTo(float xc1, float yc1, float xc2, float yc2, float x1, float y1){
            nativeInstance.nativePathConsumerCurveTo(ptr, xc1, yc1, xc2, yc2, x1, y1);
        }
        
        public void close(){
            nativeInstance.nativePathConsumerClose(ptr);
        }
        
        public void done(){
            nativeInstance.nativePathConsumerDone(ptr);
        }
    }
    
    /**
     * Encapsulation of a native pisces path renderer.
     * See Renderer.h, Renderer.c
     */
    static class NativePathRenderer {
        
        
        static final int WIND_EVEN_ODD = 0;
        static final int WIND_NON_ZERO = 1;
        final long ptr;
        final NativePathConsumer consumer;
        
        
        NativePathRenderer(int pix_boundsX, int pix_boundsY,
                           int pix_boundsWidth, int pix_boundsHeight,
                           int windingRule){
            ptr = nativeInstance.nativePathRendererCreate(pix_boundsX, pix_boundsY, pix_boundsWidth, pix_boundsHeight, windingRule);
            consumer = new NativePathConsumer(nativeInstance.nativePathRendererGetConsumer(ptr));
            
            
        }
        
        
        static void setup(int subpixelLgPositionsX, int subpixelLgPositionsY){
            nativeInstance.nativePathRendererSetup(subpixelLgPositionsX, subpixelLgPositionsY);
        }
        
        void reset(int pix_boundsX, int pix_boundsY,
                           int pix_boundsWidth, int pix_boundsHeight,
                           int windingRule){
            nativeInstance.nativePathRendererReset(ptr, pix_boundsX, pix_boundsY, pix_boundsWidth, pix_boundsHeight, windingRule);
            
        }
        
        /**
         * This can be called to destroy the underlying Renderer C struct. 
         * DON'T call this inside finalize() because the Renderer may need to outlive
         * the java wrapper in objective-c space.  Specifically, it is passed to the
         * DrawPath object for the rendering pipeline.  It will be destroyed in
         * the DrawPath dealloc method.
         */
        private void destroy(){
            nativeInstance.nativePathRendererCleanup(ptr);
        }
        
        void getOutputBounds(int[] bounds){
            nativeInstance.nativePathRendererGetOutputBounds(ptr, bounds);
        }
        
       /**
        * This creates a texture with the renderer.  Note that you'll need to 
        * @return 
        */
       long createTexture(){
           return nativeInstance.nativePathRendererCreateTexture(ptr);
       }
        
       
       int[] toARGB(int color){
           return nativeInstance.nativePathRendererToARGB(ptr, color);
       }
       
    }
    
    
    
    
    class TextureAlphaMask {
        private Rectangle bounds;
        private long textureName;
        private int[] padding;
        
        TextureAlphaMask(long textureName, Rectangle bounds, int[] padding){
            this.bounds = bounds;
            this.textureName = textureName;
            this.padding = padding;
        }
        
        void setPadding(int n, int e, int s, int w){
            padding[0] = n;
            padding[1] = e;
            padding[2] = s;
            padding[3] = w;
        }
        
        void setPadding(int[] padding){
            this.padding = padding;
        }
        
        int[] getPadding(){
            return padding;
        }
        
        void dispose(){
            if ( getTextureName() != 0 ){
                nativeDeleteTexture(getTextureName());
                setTextureName(0);
            }
        }
        
        

        protected void finalize() throws Throwable {
            dispose();
            //super.finalize(); 
        }

        /**
         * @return the bounds
         */
        public Rectangle getBounds() {
            return bounds;
        }

        /**
         * @param bounds the bounds to set
         */
        public void setBounds(Rectangle bounds) {
            this.bounds = bounds;
        }

        /**
         * @return the textureName
         */
        public long getTextureName() {
            return textureName;
        }

        /**
         * @param textureName the textureName to set
         */
        public void setTextureName(long textureName) {
            this.textureName = textureName;
        }
        
        
    }
    
    class TextureAlphaMaskProxy extends TextureAlphaMask {
        private TextureAlphaMask mask;
        private Rectangle bounds;
        
       
        
        public TextureAlphaMaskProxy(TextureAlphaMask m, Rectangle bounds){
            super(m.textureName, m.bounds, m.padding);
            mask = m;
            this.bounds = bounds;
            this.bounds.setX(bounds.getX()-m.padding[3]);
            this.bounds.setY(bounds.getY()-m.padding[0]);
            this.bounds.setWidth(bounds.getWidth()+m.padding[3]+m.padding[1]);
            this.bounds.setHeight(bounds.getHeight()+m.padding[0]+m.padding[2]);
        }
        
        public Rectangle getBounds(){
            return bounds;
        }
        
        public void setBounds(Rectangle r){
            bounds = r;
        }
        
        public long getTextureName(){
            return mask.getTextureName();
        }
        
        public void dispose(){
            // Don't do anything here... all disposal should 
            // be done by the original proxy
        }
        
    }
    
    abstract class Paint {
        
    }
    
    abstract class Gradient extends Paint {
        int startColor;
        int endColor;
    }
    
    class RadialGradient extends Gradient {
        int x, y, width, height;
        
        RadialGradient(int startColor, int endColor, int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.startColor = startColor;
            this.endColor = endColor;
        }
    }
    
    class NativeGraphics {
        Paint paint;
        final Rectangle reusableRect = new Rectangle();
        final Rectangle reusableRect2 = new Rectangle();
        NativeImage associatedImage;
        int color;
        int alpha = 255;
        NativeFont font;
        int clipX, clipY, clipW = -1, clipH = -1;
        boolean clipApplied;
        ClipShape clip;
        final ClipShape reusableClipShape = new ClipShape();
        /**
         * Used with the ES2 pipeline (or any engine where transforms are supported)
         * to record if the clipX, clipY, clipW, and clipH parameters need to be updated.
         */
        boolean clipDirty = true;

        GeneralPath inverseClip;
        boolean inverseClipDirty=true;
        Rectangle inverseClipBounds;
        
        
        Transform transform = Transform.makeIdentity();
        Transform inverseTransform;
        boolean inverseTransformDirty=true;
        
        
        boolean transformApplied = false;
        ClipShape[] clipStack = new ClipShape[20];
        private int clipStackPtr = 0; 
        private boolean antialiased;
        private boolean antialiasedSet;
        private boolean antialiasedText;
        private boolean antialiasedTextSet;
        int renderingHints;
        
        boolean isAntiAliasingSupported() {
            return true;
        }

        boolean isAntiAliasTextSupported() {
            return true;
        }
        
        void setAntiAliasedText(boolean a) {
            antialiasedText = a;
            antialiasedTextSet = true;
            
        }
        
        boolean isAntiAliasedText() {
            return !antialiasedTextSet || antialiasedText;
        }
        
        void setAntiAliased(boolean antialiased) {
            antialiasedSet = true;
            this.antialiased = antialiased;
            nativeInstance.setAntiAliasedMutable(antialiased);
        }
        
        
        boolean isAntiAliased() {
            // If antialiasing hasn't been set, then it defaults to 
            // antialiased
            return !antialiasedSet || antialiased;
        }
        
        void setClip(Shape newClip) {
            if ( clip == null) {
                clip = new ClipShape();
            }
            if (!clip.equals(newClip, transform)) { 
                clip.setShape(newClip, transform);
                clipDirty = true;
                clipApplied = false;
                inverseClipDirty = true;
                applyClip();
            }
        }
        
        
        void setClip(int x, int y, int w, int h) {
            if (clip == null) {
                clip = new ClipShape();
            }
            if (transform == null || transform.isIdentity()) {
                if (!clip.equals(x, y, w, h)) {
                    clip.setBounds(x, y, w, h);
                    clipDirty = true;
                    clipApplied = false;
                    inverseClipDirty = true;
                    applyClip();
                }
            } else {
                reusableRect.setBounds(x, y, w, h);
                if (!clip.equals(reusableRect, transform)) {
                    clip.setShape(reusableRect, transform);
                    clipDirty = true;
                    clipApplied = false;
                    inverseClipDirty = true;
                    applyClip();
                }
            }
            
        }
        
        void clipRect(int x, int y, int w, int h) {
            if (clip == null) {
                setClip(x, y, w, h);
                return;
            }
            
            if (transform == null || transform.isIdentity()) {
                // Preliminary checks to see if clipping is unnecessary
                clip.getBounds(reusableRect);
                if (reusableRect.getWidth() <= 0 || reusableRect.getHeight() <= 0) {
                    // The existing clip is null so we don't need to do anything here.
                    return;
                }
                reusableRect2.setBounds(x, y, w, h);
                
                boolean clipIsRect = clip.isRect();
                if (clipIsRect && reusableRect2.contains(reusableRect)) {
                    // The intersection did not change the resulting clip shape
                    // Just retrun here.
                    return;
                } 
                if (!clipIsRect) {
                    reusableClipShape.setShape(clip, null);
                }
                if (!clip.intersect(x, y, w, h)) {
                    clip.setBounds(0, 0, 0, 0);
                }
                if (!clipIsRect && clip.equals(reusableClipShape, null)) {
                    return;
                }
                clipDirty = true;
                clipApplied = false;
                inverseClipDirty = true;
                applyClip();
            } else {
                reusableClipShape.setShape(clip, null);
            
                GeneralPath inverseClip = inverseClip();
                if (!inverseClip.intersect(x, y, w, h)) {
                    clip.setBounds(0,0,0,0);
                } else {
                    clip.setShape(inverseClip, transform);
                }
                if (clip.equals(reusableClipShape, null)) {
                    return;
                }
                clipDirty = true;
                clipApplied = false;
                inverseClipDirty = true;
                applyClip();
            }
            
        }
        
        void loadClipBounds(){
            NativeGraphics ng = this;
            if ( ng.clipDirty){
                ng.clipDirty = false;
                if ( ng.transform == null ){
                    ng.transform = Transform.makeIdentity();
                }
                if ( ng.clip == null ){
                    ng.clip = ClipShape.create();
                    if (associatedImage == null) {
                        ng.clip.setBounds(0,0,Display.getInstance().getDisplayWidth(), Display.getInstance().getDisplayHeight());
                    } else {
                        ng.clip.setBounds(0, 0, associatedImage.width, associatedImage.height);
                    }
                }
                if ( ng.transform.isIdentity() ){
                    Rectangle r = reusableRect;
                
                    ng.clip.getBounds(r);
                    ng.clipX = r.getX();
                    ng.clipY = r.getY();
                    ng.clipW = r.getWidth();
                    ng.clipH = r.getHeight();
                } else {
                    
                    GeneralPath inverseClip = ng.inverseClip();
                    Rectangle r = reusableRect;
                    inverseClip.getBounds(r);
                    ng.clipX = r.getX();
                    ng.clipY = r.getY();
                    ng.clipW = r.getWidth();
                    ng.clipH = r.getHeight();
                } 

            }
        }
        
        int getClipX() {
            loadClipBounds();
            return clipX;
        }
        
        int getClipY() {
            loadClipBounds();
            return clipY;
        }
        
        int getClipW() {
            loadClipBounds();
            if(clipW < 0 && associatedImage != null) {
                return associatedImage.width;
            }
            return clipW;
        }
        
        int getClipH() {
            loadClipBounds();
            if(clipH < 0 && associatedImage != null) {
                return associatedImage.height;
            }
            return clipH;
        }
        
        void setTransform(Transform t) {
            if (transform == null) {
                transform = Transform.makeIdentity();
            }
            transform.setTransform(t);
            inverseTransformDirty = true;
            clipDirty = true;
            transformApplied = false;
            applyTransform();
        }
        
        Transform inverseTransform() {
            
            if (inverseTransformDirty) {
                if (inverseTransform == null) {
                    inverseTransform = Transform.makeIdentity();
                }
                if (transform == null) {
                    inverseTransform.setIdentity();
                } else {
                    try {
                        transform.getInverse(inverseTransform);
                    } catch (Transform.NotInvertibleException ex) {
                        throw new RuntimeException("The transform "+transform+" cannot be inverted");
                    }
                }
                inverseTransformDirty = false;
            }
            return inverseTransform;
            
        }
        
        
        GeneralPath inverseClip() {
            if (inverseClipDirty) {

                if (clip == null) {
                    return null;
                }
                if (inverseClip == null) {
                    inverseClip = new GeneralPath();
                }
                inverseClip.setShape(clip, inverseTransform());
                inverseClipDirty = false;
            }
            return inverseClip;
        }
        
        

        public NativeFont getFont() {
            if(font != null) {
                return font;
            }
            return (NativeFont)getDefaultFont();
        }

        public void applyTransform(){
            if (!transformApplied) {
                setNativeTransformMutable(this.transform);
                transformApplied = true;
            }
        }
        
        public void pushClip(){
            
            ClipShape newClip = ClipShape.create();
            newClip.setShape(clip, null);
            clipStack[clipStackPtr++] = newClip;
            
        }
        
        public Shape popClip(){
            ClipShape s = clipStack[--clipStackPtr];
            //Log.p("Popping clip "+s);
            clipApplied = false;
            clip.setShape(s, null);
            ClipShape.recycle(s);
            applyClip();
            return s;
        }
        
        public void applyClip() {
            if ( clipApplied ){
                return;
            }
            //Log.p("In applyClip");
            if ( this.clip == null ){
                //Log.p("Clip is null");
                int w = associatedImage == null ? Display.getInstance().getDisplayWidth() : associatedImage.width;
                int h = associatedImage == null ? Display.getInstance().getDisplayHeight() : associatedImage.height;
                clipX = 0;
                clipY = 0;
                clipW = w;
                clipH = h;
                this.clip = new ClipShape();
                this.clip.setBounds(0,0,w,h);
                setNativeClipping(0,0,w,h,clipApplied);
                clipApplied = true;
                return;
            }
            if ( this.clip.isRect() ){
                //Log.p("Clip is a rectangle");
                //Log.p(""+this.clip);
                Rectangle r = this.reusableRect;
                this.clip.getBounds(r);
                setNativeClipping(r.getX(), r.getY(), r.getWidth(), r.getHeight(), clipApplied);
                clipApplied = true;
            } else {
                //Log.p("Clip is not a rectangle");
                //Log.p(""+this.clip);
                setNativeClipping(this.clip);
                clipApplied = true;
            }
            
        }

        public void checkControl() {
            if(currentlyDrawingOn != this) {
                if(currentlyDrawingOn != null) {
                    currentlyDrawingOn.associatedImage.peer = finishDrawingOnImage();
                }
                startDrawingOnImage(associatedImage.width, associatedImage.height, associatedImage.peer);
                currentlyDrawingOn = this;
            }
        }

        
        void setNativeClipping(int x, int y, int width, int height, boolean firstClip) {
            setNativeClippingMutable(x, y, width, height, firstClip);
        }
        
        void setNativeClipping(ClipShape shape){

            if (shape.isRect()) {
                shape.getBounds(reusableRect);
                setNativeClippingMutable(reusableRect.getX(), reusableRect.getY(), reusableRect.getWidth(), reusableRect.getHeight(), clipApplied);

            } else {
                // The native side (setNativeClippingShapeMutableImpl in
                // CodenameOne_GLViewController.m) ignores the commands
                // array and treats every (x, y) pair in the points buffer
                // as a polygon vertex. For a path with curves that means
                // control points appear as polygon vertices, producing
                // the SVG "triangle clip" symptom for gradient_circle.svg
                // and clipped_badge.svg. Flatten on the Java side so the
                // points buffer contains only true polyline vertices.
                ClipShape polyShape = flattenClipShapeIfNeeded(shape);
                int commandsLen = polyShape.getTypesSize();
                int pointsLen = polyShape.getPointsSize();
                byte[] commandsArr = getTmpNativeDrawShape_commands(commandsLen);
                float[] pointsArr = getTmpNativeDrawShape_coords(pointsLen);
                polyShape.getTypes(commandsArr);
                polyShape.getPoints(pointsArr);
                nativeInstance.setNativeClippingMutable(commandsLen, commandsArr, pointsLen, pointsArr);
            }
        }

        void nativeDrawLine(int color, int alpha, int x1, int y1, int x2, int y2) {
            // The C-side nativeDrawLineMutableImpl already short-circuits
            // through Metal under #ifdef CN1_USE_METAL (queues a DrawLine op
            // tagged with currentMutableImage). No Java-side Metal/GL gate
            // needed.
            nativeDrawLineMutable(color, alpha, x1, y1, x2, y2);
        }

        void nativeFillRect(int color, int alpha, int x, int y, int width, int height) {
            // Same as nativeDrawLine: the C-side nativeFillRectMutableImpl
            // routes through the Metal pipeline under #ifdef CN1_USE_METAL.
            nativeFillRectMutable(color, alpha, x, y, width, height);
        }

        void nativeDrawRect(int color, int alpha, int x, int y, int width, int height) {
            // Same as nativeDrawLine / nativeFillRect: the C-side
            // nativeDrawRectMutableImpl routes through the Metal pipeline
            // under #ifdef CN1_USE_METAL.
            nativeDrawRectMutable(color, alpha, x, y, width, height);
        }

        void nativeDrawRoundRect(int color, int alpha, int x, int y, int width, int height, int arcWidth, int arcHeight) {
            if (metalRendering) {
                // Build a round-rect GeneralPath and stroke it via the
                // alpha-mask Metal pipeline (Renderer.c -> R8 MTLTexture ->
                // DrawTextureAlphaMask op tagged with currentMutableImage).
                GeneralPath p = roundRectPath(x, y, width, height, arcWidth, arcHeight);
                if (tmpStroke1px == null) tmpStroke1px = new Stroke(1, Stroke.CAP_BUTT, Stroke.JOIN_ROUND, 1f);
                renderShapeViaAlphaMask(p, tmpStroke1px);
                return;
            }
            // GL: drawTextureAlphaMaskImpl can't tag the alpha-mask op with
            // a mutable target on a non-Metal build, so the mask would land
            // on the screen instead of inside the mutable's UIImage. Fall
            // back to the legacy CG-rasterise-and-DrawImage JNI which
            // writes directly to the mutable's CGContextRef.
            nativeInstance.nativeDrawRoundRectMutable(color, alpha, x, y, width, height, arcWidth, arcHeight);
        }

        void nativeFillRoundRect(int color, int alpha, int x, int y, int width, int height, int arcWidth, int arcHeight) {
            if (metalRendering) {
                GeneralPath p = roundRectPath(x, y, width, height, arcWidth, arcHeight);
                renderShapeViaAlphaMask(p, null);
                return;
            }
            nativeInstance.nativeFillRoundRectMutable(color, alpha, x, y, width, height, arcWidth, arcHeight);
        }

        void nativeDrawArc(int color, int alpha, int x, int y, int width, int height, int startAngle, int arcAngle) {
            if (metalRendering) {
                if (drawingArcPath == null) drawingArcPath = new GeneralPath();
                if (tmpStroke1px == null) tmpStroke1px = new Stroke(1, Stroke.CAP_BUTT, Stroke.JOIN_ROUND, 1f);
                drawingArcPath.reset();
                drawingArcPath.arc(x, y, width, height, startAngle * Math.PI / 180, arcAngle * Math.PI / 180, false);
                renderShapeViaAlphaMask(drawingArcPath, tmpStroke1px);
                return;
            }
            nativeInstance.nativeDrawArcMutable(color, alpha, x, y, width, height, startAngle, arcAngle);
        }

        void nativeFillArc(int color, int alpha, int x, int y, int width, int height, int startAngle, int arcAngle) {
            if (metalRendering) {
                if (drawingArcPath == null) drawingArcPath = new GeneralPath();
                drawingArcPath.reset();
                if (arcAngle >= 360 || arcAngle <= -360) {
                    // Full circle/ellipse: omit the moveTo(center). With it the
                    // path is center -> arc start -> 360 -> close back to
                    // center, which Renderer.c rasterises with a visible slice
                    // line from center to the start point.
                    drawingArcPath.arc(x, y, width, height, startAngle * Math.PI / 180, arcAngle * Math.PI / 180, false);
                } else {
                    drawingArcPath.moveTo(x + width / 2, y + height / 2);
                    drawingArcPath.arc(x, y, width, height, startAngle * Math.PI / 180, arcAngle * Math.PI / 180, true);
                }
                drawingArcPath.closePath();
                renderShapeViaAlphaMask(drawingArcPath, null);
                return;
            }
            nativeInstance.nativeFillArcMutable(color, alpha, x, y, width, height, startAngle, arcAngle);
        }

        private Stroke tmpStroke1px;
        private GeneralPath drawingArcPath;

        // Build a round-rect path from the parametric (x,y,w,h,arcW,arcH) form
        // so the alpha-mask pipeline can rasterise it. arcW/arcH are full
        // ellipse-axis lengths (matching the Java2D / cn1 roundRect contract);
        // half each gives the corner radii.
        private GeneralPath roundRectPath(int x, int y, int width, int height, int arcWidth, int arcHeight) {
            GeneralPath p = new GeneralPath();
            float rx = Math.min(arcWidth / 2f, width / 2f);
            float ry = Math.min(arcHeight / 2f, height / 2f);
            if (rx <= 0 || ry <= 0) {
                // Degenerate: just emit a rectangle outline.
                p.moveTo(x, y);
                p.lineTo(x + width, y);
                p.lineTo(x + width, y + height);
                p.lineTo(x, y + height);
                p.closePath();
                return p;
            }
            // Trace the round-rect outline as a single closed sub-path going
            // CW in screen coords (Y-down): top edge -> top-right corner ->
            // right edge -> bottom-right corner -> bottom edge -> bottom-left
            // corner -> left edge -> top-left corner -> close.
            //
            // Each corner arc starts where the previous edge ended and ends
            // where the next edge begins, so every joinPath=true draws a
            // zero-length connector. cn1's GeneralPath.arc internally negates
            // the angles (math-Y-up convention -> screen-Y-down via
            // -startAngle / -sweepAngle in addToPath), and getPointAtAngle
            // uses cy + b*sin(theta), which means a user-facing angle of
            // +pi/2 evaluates to (cx, cy - b) in screen coords (top of bbox)
            // and 0 evaluates to (cx + a, cy) (right). Sweep -pi/2 traces a
            // single quadrant CW visually. Concretely:
            //   top-right    : start +pi/2 (top),    sweep -pi/2 -> right
            //   bottom-right : start  0    (right),  sweep -pi/2 -> bottom
            //   bottom-left  : start -pi/2 (bottom), sweep -pi/2 -> left
            //   top-left     : start +pi   (left),   sweep -pi/2 -> top
            // The previous version used sweep +pi/2 with the opposite start
            // angles, which produced an arc traversing the *opposite*
            // quadrant of the corner bbox. For pills (where adjacent corner
            // bboxes overlap because h-2ry == 0) the resulting path looped
            // back through the bbox interior and Renderer.c's winding-fill
            // pass interpreted that as a tear -- visible as the Switch
            // track's right-half collapsing into a triangular wedge.
            //
            // Skip the inter-corner lineTos when their endpoints would
            // coincide with the next arc's join target (pill case: rx ==
            // width/2 collapses the top/bottom edges; ry == height/2
            // collapses the left/right edges). Emitting a zero-length lineTo
            // would leave a phantom edge that the winding pass also reads
            // as a tear.
            boolean hasTopBottomEdges = rx < width / 2f;
            boolean hasLeftRightEdges = ry < height / 2f;
            float twoRx = 2f * rx;
            float twoRy = 2f * ry;
            p.moveTo(x + rx, y);
            if (hasTopBottomEdges) p.lineTo(x + width - rx, y);
            p.arc(x + width - twoRx, y,                  twoRx, twoRy,  Math.PI / 2, -Math.PI / 2, true);
            if (hasLeftRightEdges) p.lineTo(x + width, y + height - ry);
            p.arc(x + width - twoRx, y + height - twoRy, twoRx, twoRy,  0,           -Math.PI / 2, true);
            if (hasTopBottomEdges) p.lineTo(x + rx, y + height);
            p.arc(x,                 y + height - twoRy, twoRx, twoRy, -Math.PI / 2, -Math.PI / 2, true);
            if (hasLeftRightEdges) p.lineTo(x, y + ry);
            p.arc(x,                 y,                  twoRx, twoRy,  Math.PI,     -Math.PI / 2, true);
            p.closePath();
            return p;
        }

        void nativeDrawString(int color, int alpha, long fontPeer, String str, int x, int y) {
            boolean antialiasTextChanged = false;
            if (isAntiAliased() != isAntiAliasedText()) {
                // We want text to be antialiased
                antialiasTextChanged = true;
                setAntiAliased(isAntiAliasedText());
                
            }
            nativeDrawStringMutable(color, alpha, fontPeer, str, x, y);
            if (antialiasTextChanged) {
                setAntiAliased(!isAntiAliasedText());
            }
        }

        void nativeDrawImage(long peer, int alpha, int x, int y, int width, int height) {
            nativeDrawImageMutable(peer, alpha, x, y, width, height, renderingHints);
        }
        
        
        
        //----------------------------------------------------------------------
        // BEGIN DRAW SHAPE METHODS
        
        void nativeDrawAlphaMask(TextureAlphaMask mask){
            // Mirror GlobalGraphics: hand the alpha-mask MTLTexture handle
            // off to drawTextureAlphaMask. The JNI side picks up
            // currentMutableImage and tags the queued op so drawFrame's
            // drain (Phase 3 v2) routes it to the mutable's encoder.
            if (mask != null && mask.getTextureName() != 0) {
                Rectangle r = mask.getBounds();
                nativeInstance.drawTextureAlphaMask(mask.getTextureName(), this.color, this.alpha, r.getX(), r.getY(), r.getWidth(), r.getHeight());
            }
        }
        
        
        private float[] tmpNativeDrawShape_coords;
        
        private float[] getTmpNativeDrawShape_coords(int size) {
            if (tmpNativeDrawShape_coords == null) {
                tmpNativeDrawShape_coords = new float[size];
            }
            if (tmpNativeDrawShape_coords.length < size) {
                float[] newArray = new float[size];
                System.arraycopy(tmpNativeDrawShape_coords, 0, newArray, 0, tmpNativeDrawShape_coords.length);
                tmpNativeDrawShape_coords = newArray;
            }
            return tmpNativeDrawShape_coords;
        }
        
        private float[] growTmpNativeDrawShape_coords(int size, int factor) {
            if (tmpNativeDrawShape_coords.length < size) {
                float[] newArray = new float[size * factor];
                System.arraycopy(tmpNativeDrawShape_coords, 0, newArray, 0, tmpNativeDrawShape_coords.length);
                tmpNativeDrawShape_coords = newArray;
            }
            return tmpNativeDrawShape_coords;
        }
        
        private byte[] getTmpNativeDrawShape_commands(int size) {
            if (tmpNativeDrawShape_commands == null) {
                tmpNativeDrawShape_commands = new byte[size];
            }
            if (tmpNativeDrawShape_commands.length < size) {
                byte[] newArray = new byte[size];
                System.arraycopy(tmpNativeDrawShape_commands, 0, newArray, 0, tmpNativeDrawShape_commands.length);
                tmpNativeDrawShape_commands = newArray;
            }
            return tmpNativeDrawShape_commands;
        }
        
        private byte[] tmpNativeDrawShape_commands;
        
        /**
         * Draws a shape in the graphics context
         * @param shape
         * @param stroke
         */
        void nativeDrawShape(Shape shape, Stroke stroke) {
            if (metalRendering) {
                renderShapeViaAlphaMask(shape, stroke);
                return;
            }
            // GL: serialize the path commands and call the legacy CG-based
            // JNI which strokes the shape into the mutable's CGContextRef.
            // The alpha-mask path can't target a mutable on a non-Metal
            // build (drawTextureAlphaMaskImpl's setTarget is gated by
            // #ifdef CN1_USE_METAL), so the mask would otherwise land on
            // the screen and the mutable would come back empty.
            if (shape.getClass() == GeneralPath.class) {
                GeneralPath p = (GeneralPath) shape;
                int commandsLen = p.getTypesSize();
                int pointsLen = p.getPointsSize();
                byte[] commandsArr = getTmpNativeDrawShape_commands(commandsLen);
                float[] pointsArr = getTmpNativeDrawShape_coords(pointsLen);
                p.getTypes(commandsArr);
                p.getPoints(pointsArr);
                nativeInstance.nativeDrawShapeMutable(color, alpha, commandsLen, commandsArr, pointsLen, pointsArr,
                        stroke.getLineWidth(), stroke.getCapStyle(), stroke.getJoinStyle(), stroke.getMiterLimit());
            } else {
                Log.p("Drawing shapes that are not GeneralPath objects is not yet supported on mutable images.");
            }
        }

        // Render a shape on the current mutable target via Renderer.c
        // -> R8 MTLTexture -> DrawTextureAlphaMask op tagged with the
        // mutable's GLUIImage. Stroke == null means fill.
        //
        // Caches the resulting MTLTexture in textureCache keyed on
        // (shape, stroke) so repeated draws of the same path (typical
        // theme rendering) hit the cache instead of re-rasterising.
        //
        // For non-identity transforms, bakes the transform's scale into
        // a copy of the shape (so the alpha-mask is rasterised at the
        // displayed scale, not at unit scale and then scaled), then
        // draws with the inverse-scale transform applied so the result
        // lands at the right coordinates. Mirrors what GlobalGraphics
        // does on the screen-side path.
        private void renderShapeViaAlphaMask(Shape shape, Stroke stroke) {
            if (!(shape instanceof GeneralPath)) {
                Log.p("Drawing shapes that are not GeneralPath objects is not yet supported on mutable images.");
                return;
            }
            if (transform == null || transform.isIdentity()) {
                TextureAlphaMask mask = textureCache.get(shape, stroke);
                if (mask == null) {
                    mask = createAlphaMask(shape, stroke);
                    textureCache.add(shape, stroke, mask);
                }
                if (mask == null) return;
                nativeDrawAlphaMask(mask);
                return;
            }
            if (tmpDrawShape == null) tmpDrawShape = new GeneralPath();
            if (tmpTransform == null) tmpTransform = Transform.makeIdentity();
            if (tmpDrawStroke == null) tmpDrawStroke = new Stroke();
            if (tmpRect2 == null) tmpRect2 = new Rectangle();
            // Metal-only path (entry to this method is already gated on
            // metalRendering). Factor the user transform into a non-uniform
            // pre-rasterisation scale (sx, sy) plus a residual GPU transform
            // -- see the matching block in GlobalGraphics.nativeDrawShape for
            // the rationale (GH-3302 inscribed-shape drift).
            Matrix nm = (Matrix) transform.getNativeTransform();
            float[] m = nm.getData();
            float c0x = m[0], c0y = m[1];
            float c1x = m[4], c1y = m[5];
            float sx = (float) Math.sqrt((double) c0x * c0x + (double) c0y * c0y);
            float sy = (float) Math.sqrt((double) c1x * c1x + (double) c1y * c1y);
            if (sx < 1e-6f) sx = 1f;
            if (sy < 1e-6f) sy = 1f;
            float strokeScale = (sx == sy) ? sx : (float) Math.sqrt((double) sx * (double) sy);
            tmpTransform.setScale(sx, sy);
            tmpDrawShape.setShape(shape, tmpTransform);
            Stroke scaledStroke = null;
            if (stroke != null) {
                tmpDrawStroke.setStroke(stroke);
                tmpDrawStroke.setLineWidth(tmpDrawStroke.getLineWidth() * strokeScale);
                scaledStroke = tmpDrawStroke;
            }
            TextureAlphaMask mask = textureCache.get(tmpDrawShape, scaledStroke);
            if (mask == null) {
                mask = createAlphaMask(tmpDrawShape, scaledStroke);
                textureCache.add(tmpDrawShape, scaledStroke, mask);
            }
            if (mask == null) return;
            // Apply the residual S(1/sx, 1/sy) via the impl-side scale path
            // -- the same path g.scale uses. Going through setTransform with
            // a separately-built composed Transform has been documented to
            // silently fail to update the Metal-side currentTransform in
            // some cases (see the Transform.setTransform comment about
            // "iOS Metal port has shown that without this flag
            // setTransform(composed) silently fails to apply"). The scale
            // path queues a SetTransform op that reliably reaches both the
            // screen and mutable-image encoders.
            scale(1f / sx, 1f / sy);
            try {
                nativeDrawAlphaMask(mask);
            } finally {
                // Restore by composing the inverse residual back onto the
                // matrix. After this call the impl matrix is back at
                // T(...) * S(sx, sy) -- exactly what the caller expects.
                scale(sx, sy);
            }
        }

        private GeneralPath tmpDrawShape;
        private Transform tmpTransform;
        private Stroke tmpDrawStroke;
        private Rectangle tmpRect2;

        /**
         * Fills a shape in the graphics context.
         * @param shape
         */
        void nativeFillShape(Shape shape) {
            if (metalRendering) {
                // Fill is the same alpha-mask path as draw with a null
                // stroke. Renderer.c on the C side decides fill-vs-stroke
                // from the stroke being NULL.
                renderShapeViaAlphaMask(shape, null);
                return;
            }
            if (shape.getClass() == GeneralPath.class) {
                GeneralPath p = (GeneralPath) shape;
                int commandsLen = p.getTypesSize();
                int pointsLen = p.getPointsSize();
                byte[] commandsArr = getTmpNativeDrawShape_commands(commandsLen);
                float[] pointsArr = getTmpNativeDrawShape_coords(pointsLen);
                p.getTypes(commandsArr);
                p.getPoints(pointsArr);
                nativeInstance.nativeFillShapeMutable(color, alpha, commandsLen, commandsArr, pointsLen, pointsArr);
            } else {
                Log.p("Drawing shapes that are not GeneralPath objects is not yet supported on mutable images.");
            }
        }

        boolean isDrawShadowSupported() {
            return true;
        }

        void nativeDrawShadow(long image, int x, int y, int offsetX, int offsetY, int blurRadius, int spreadRadius, int color, float opacity) {
            nativeInstance.nativeDrawShadowMutable(image, x, y, offsetX, offsetY, blurRadius, spreadRadius, color, opacity);
        }
        
        boolean isTransformSupported(){
            return true;
        }
        
        boolean isPerspectiveTransformSupported(){
            return false;
        }
        
        boolean isShapeSupported(){
            return true;
        }
        
        boolean isAlphaMaskSupported() {
            // On Metal nativeDrawShape / nativeFillShape route through the
            // Renderer.c-driven alpha-mask pipeline (same as GlobalGraphics
            // on screen). On GL the alpha-mask op can't target a mutable,
            // so we fall back to the CG path; tell the framework not to
            // bother building alpha masks for mutable on GL.
            return metalRendering;
        }

        // END DRAW SHAPE METHODS
        //----------------------------------------------------------------------
        
        public void resetAffine() {
            this.transform.setIdentity();
            transformApplied = false;
            clipDirty = true;
            inverseClipDirty = true;
            inverseTransformDirty = true;
            this.applyTransform();
        }

        public void scale(float x, float y) {
            this.transform.scale(x, y, 1);
            clipDirty = true;
            transformApplied = false;
            inverseClipDirty = true;
            inverseTransformDirty = true;
            this.applyTransform();
        }

        public void rotate(float angle) {
            this.transform.rotate(angle, 0, 0);
            clipDirty = true;
            transformApplied = false;
            inverseClipDirty = true;
            inverseTransformDirty = true;
            applyTransform();
        }

        public void rotate(float angle, int x, int y) {
            this.transform.rotate(angle, x, y);
            transformApplied = false;
            clipDirty = true;
            inverseClipDirty = true;
            inverseTransformDirty = true;
            this.applyTransform();
        }

        public void translateMatrix(float x, float y) {
            // Composes T(x, y) onto the impl-side matrix, exactly like
            // scale/rotate. NOTE: deliberately does NOT touch the
            // framework-level xTranslate/yTranslate accumulator that the
            // legacy g.translate(int, int) path uses. Mixing them is well-
            // defined (xTranslate is added to draw coords first, then this
            // matrix applies) but apps that switch to translateMatrix
            // should generally avoid g.translate so the two don't fight.
            this.transform.translate(x, y, 0);
            clipDirty = true;
            transformApplied = false;
            inverseClipDirty = true;
            inverseTransformDirty = true;
            this.applyTransform();
        }

        public void translate(int x, int y){

        }
        
        public int getTranslateX(){
            return 0;
        }
        public int getTranslateY(){
            return 0;
        }

        public void shear(float x, float y) {
        }

        public void fillRectRadialGradient(int startColor, int endColor, int x, int y, int width, int height, float relativeX, float relativeY, float relativeSize) {
            nativeInstance.fillRectRadialGradientMutable(startColor, endColor, x, y, width, height, relativeX, relativeY, relativeSize);
        }
    
        public void fillLinearGradient(int startColor, int endColor, int x, int y, int width, int height, boolean horizontal) {
            nativeInstance.fillLinearGradientMutable(startColor, endColor, x, y, width, height, horizontal);
        }

        void fillConvexPolygon(float[] points, int color, int alpha) {
            
        }

        void drawConvexPolygon(float[] points, int color, int alpha, float lineWidth, int joinStyle, int capStyle, float miterLimit) {
            
        }

        boolean isShapeClipSupported() {
            return true;
        }
        
        public void applyPaint() {
            if (paint != null && paint instanceof RadialGradient) {
                RadialGradient g = (RadialGradient)paint;
                nativeInstance.applyRadialGradientPaintMutable(g.startColor, g.endColor, g.x, g.y, g.width, g.height);
            }
        }
        
        public void unapplyPaint() {
            if (paint != null && paint instanceof RadialGradient) {
                nativeInstance.clearRadialGradientPaintMutable();
            }
        }

        public void nativeClearRect(int x, int y, int width, int height) {
            nativeInstance.clearRectMutable(x, y, width, height);
        }

        void fillPolygon(int color, int alpha, int[] xPoints, int[] yPoints, int nPoints) {
            
            // With mutable contexts the performance should be similar between
            // drawing a shape and drawing a polygon, so let's just use
            // the more generate fillShape code.
            GeneralPath path = GeneralPath.createFromPool();
            try {
                for (int i=0; i<nPoints; i++) {
                    if (i==0) {
                        path.moveTo(xPoints[0], yPoints[0]);
                    } else {
                        path.lineTo(xPoints[i], yPoints[i]);
                    }
                }
                path.closePath();
                this.nativeFillShape(path);
            } finally {
                GeneralPath.recycle(path);
            }
        }

        private void setRenderingHints(int hints) {
            renderingHints = hints;
        }
        
        private int getRenderingHints() {
            return renderingHints;
        }

        
    }

    class GlobalGraphics extends NativeGraphics {

        @Override
        void setAntiAliased(boolean antialiased) {
            // Don't do anything here because the global graphcis doesn't support antialiasing.
        }

        @Override
        boolean isAntiAliased() {
            // Currently global graphics doesn't support antialiasing.
            return false;
        }

        @Override
        boolean isDrawShadowSupported() {
            return false;
        }

        @Override
        void nativeDrawShadow(long image, int x, int y, int offsetX, int offsetY, int blurRadius, int spreadRadius, int color, float opacity) {

        }



        @Override
        void fillPolygon(int color, int alpha, int[] xPoints, int[] yPoints, int nPoints) {
            if (GeneralPath.isConvexPolygon(xPoints, yPoints)) {
                nativeInstance.fillPolygonGlobal(color, alpha, xPoints, yPoints, nPoints);
            } else {
                GeneralPath path = GeneralPath.createFromPool();
                try {
                    for (int i=0; i<nPoints; i++) {
                        if (i==0) {
                            path.moveTo(xPoints[0], yPoints[0]);
                        } else {
                            path.lineTo(xPoints[i], yPoints[i]);
                        }
                    }
                    path.closePath();
                    this.nativeFillShape(path);
                } finally {
                    GeneralPath.recycle(path);
                }
            }
        }
        
        
        
        @Override
        boolean isAntiAliasingSupported() {
            // Currently global graphics are drawn with opengl
            // and don't support antialiasing on drawLine, drawRect, functions
            // etc...
            return false;
        }

        @Override
        boolean isAntiAliasTextSupported() {
            
            // In global context antialias text is the default, and we don't
            // support turning it off right now.  I guess the most appropriate
            // value here is "false" to indicate that this setting
            // can't be manipulated
            return false;
        }

        @Override
        void setAntiAliasedText(boolean a) {
            
        }

        @Override
        boolean isAntiAliasedText() {
            // Currently text is always antialiased in global context.
            return true;
        }
        
        public void applyPaint() {
            if (paint != null && paint instanceof RadialGradient) {
                RadialGradient g = (RadialGradient)paint;
                nativeInstance.applyRadialGradientPaintGlobal(g.startColor, g.endColor, g.x, g.y, g.width, g.height);
            }
        }
        
        public void unapplyPaint() {
            if (paint != null && paint instanceof RadialGradient) {
                nativeInstance.clearRadialGradientPaintGlobal();
            }
        }
        
        public void checkControl() {
            if(currentlyDrawingOn != this) {
                if(currentlyDrawingOn != null) {
                    currentlyDrawingOn.associatedImage.peer = finishDrawingOnImage();
                    // Returning to the screen after drawing into a mutable image:
                    // on the Metal backend the mutable-image draw runs on its own
                    // render encoder, so the screen encoder's scissor is whatever
                    // it was last set to -- NOT necessarily the current screen
                    // clip. clipApplied still reads true, so applyClip() would
                    // skip re-emitting it and the next screen draw would use a
                    // stale (often full-screen) scissor. That makes a clip set
                    // before the mutable-image draw silently not apply to the
                    // draw after it -> content drawn outside its clip (#5171).
                    // Invalidate so the screen clip is re-applied for the next draw.
                    clipApplied = false;
                }
                currentlyDrawingOn = null;
            }
        }

        public void applyTransform(){
            if ( !transformApplied){
                setNativeTransformGlobal(this.transform);
                transformApplied = true;
            }
        }
        
        public void resetAffine() {
            this.transform.setIdentity();
            transformApplied = false;
            inverseClipDirty = true;
            clipDirty = true;
            inverseTransformDirty = true;
            this.applyTransform();
        }

        public void scale(float x, float y) {
            this.transform.scale(x, y, 1);
            transformApplied = false;
            inverseClipDirty = true;
            inverseTransformDirty = true;
            clipDirty = true;
            this.applyTransform();
        }

        public void rotate(float angle) {
            this.transform.rotate(angle, 0, 0);
            transformApplied = false;
            inverseClipDirty = true;
            inverseTransformDirty = true;
            clipDirty = true;
            applyTransform();
            
        }

        public void rotate(float angle, int x, int y) {
            this.transform.rotate(angle, x, y);
            transformApplied = false;
            this.applyTransform();
            inverseClipDirty = true;
            inverseTransformDirty = true;
            clipDirty = true;

        }

        public void shear(float x, float y) {
            nativeInstance.shearGlobal(x, y);
        }
        
        @Override
        void setNativeClipping(int x, int y, int width, int height, boolean firstClip) {
            setNativeClippingGlobal(x, y, width, height, firstClip);
        }
        
        @Override
        void setNativeClipping(ClipShape clip){
            if (clip.isRect()) {
                clip.getBounds(reusableRect);
                setNativeClippingGlobal(reusableRect.getX(), reusableRect.getY(), reusableRect.getWidth(), reusableRect.getHeight(), clipApplied);
            } else {
                setNativeClippingGlobal(clip);
            }
        }

        void nativeDrawLine(int color, int alpha, int x1, int y1, int x2, int y2) {
            nativeDrawLineGlobal(color, alpha, x1, y1, x2, y2);
        }

        void nativeFillRect(int color, int alpha, int x, int y, int width, int height) {
            nativeFillRectGlobal(color, alpha, x, y, width, height);
        }

        @Override
        public void nativeClearRect(int x, int y, int width, int height) {
            nativeClearRectGlobal(x, y, width, height);
            
        }
        
        

        void nativeDrawRect(int color, int alpha, int x, int y, int width, int height) {
            nativeDrawRectGlobal(color, alpha, x, y, width, height);
        }

        void nativeDrawRoundRect(int color, int alpha, int x, int y, int width, int height, int arcWidth, int arcHeight) {
            if (metalRendering) {
                // Route through the alpha-mask Metal pipeline (build a path,
                // then nativeDrawShape uses Renderer.c -> R8 MTLTexture ->
                // DrawTextureAlphaMask op).
                GeneralPath p = roundRectPath(x, y, width, height, arcWidth, arcHeight);
                if (tmpStroke1px == null) tmpStroke1px = new Stroke(1, Stroke.CAP_BUTT, Stroke.JOIN_ROUND, 1f);
                nativeDrawShape(p, tmpStroke1px);
                return;
            }
            // GL screen: legacy CG path. Pre-c764fd4 GlobalGraphics already
            // gated with metalRendering; the unification commit collapsed
            // it which made the GL screen alpha-mask render diverge from
            // the GL goldens captured against the CG path. Restoring the
            // gate keeps existing GL goldens valid.
            nativeInstance.nativeDrawRoundRectGlobal(color, alpha, x, y, width, height, arcWidth, arcHeight);
        }

        void nativeFillRoundRect(int color, int alpha, int x, int y, int width, int height, int arcWidth, int arcHeight) {
            if (metalRendering) {
                GeneralPath p = roundRectPath(x, y, width, height, arcWidth, arcHeight);
                nativeFillShape(p);
                return;
            }
            nativeInstance.nativeFillRoundRectGlobal(color, alpha, x, y, width, height, arcWidth, arcHeight);
        }

        // Build a round-rect path from the parametric (x,y,w,h,arcW,arcH) form
        // so the alpha-mask Metal pipeline can rasterise it. arcW/arcH are
        // full ellipse-axis lengths (cn1 / Java2D contract); half each gives
        // the corner radii. Mirrors MutableGraphics.roundRectPath.
        private GeneralPath roundRectPath(int x, int y, int width, int height, int arcWidth, int arcHeight) {
            GeneralPath p = new GeneralPath();
            float rx = Math.min(arcWidth / 2f, width / 2f);
            float ry = Math.min(arcHeight / 2f, height / 2f);
            if (rx <= 0 || ry <= 0) {
                p.moveTo(x, y);
                p.lineTo(x + width, y);
                p.lineTo(x + width, y + height);
                p.lineTo(x, y + height);
                p.closePath();
                return p;
            }
            // CW screen-coord traversal with sweep=-pi/2 per corner -- see
            // MutableGraphics.roundRectPath above for the angle-convention
            // analysis and why sweep=+pi/2 (the prior code) traced the
            // opposite quadrant of each corner bbox and produced a
            // triangular tear on pills.
            boolean hasTopBottomEdges = rx < width / 2f;
            boolean hasLeftRightEdges = ry < height / 2f;
            float twoRx = 2f * rx;
            float twoRy = 2f * ry;
            p.moveTo(x + rx, y);
            if (hasTopBottomEdges) p.lineTo(x + width - rx, y);
            p.arc(x + width - twoRx, y,                  twoRx, twoRy,  Math.PI / 2, -Math.PI / 2, true);
            if (hasLeftRightEdges) p.lineTo(x + width, y + height - ry);
            p.arc(x + width - twoRx, y + height - twoRy, twoRx, twoRy,  0,           -Math.PI / 2, true);
            if (hasTopBottomEdges) p.lineTo(x + rx, y + height);
            p.arc(x,                 y + height - twoRy, twoRx, twoRy, -Math.PI / 2, -Math.PI / 2, true);
            if (hasLeftRightEdges) p.lineTo(x, y + ry);
            p.arc(x,                 y,                  twoRx, twoRy,  Math.PI,     -Math.PI / 2, true);
            p.closePath();
            return p;
        }

        private Stroke tmpStroke1px;
        void nativeDrawArc(int color, int alpha, int x, int y, int width, int height, int startAngle, int arcAngle) {
            // Turns out that using a Shape instead of using a Shader is much faster so we just pipe this
            // through to DrawShape.
            // See https://gist.github.com/shannah/85d93674d709c7733e98 for Shader implementation that we decided 
            // not to use.
            if (drawingArcPath == null) {
                drawingArcPath = new GeneralPath();
            }
            if (tmpStroke1px == null) {
                tmpStroke1px = new Stroke(1, Stroke.CAP_BUTT, Stroke.JOIN_ROUND, 1f);
            }
            drawingArcPath.reset();
            //drawingArcPath.moveTo(x + width / 2, y + height / 2);
            drawingArcPath.arc(x, y, width, height, startAngle * Math.PI / 180, arcAngle * Math.PI / 180, false);
            //drawingArcPath.closePath();
            nativeDrawShape(drawingArcPath, tmpStroke1px);
        }

        // path used by fillArc to fill arcs.
        private GeneralPath drawingArcPath;
        
        void nativeFillArc(int color, int alpha, int x, int y, int width, int height, int startAngle, int arcAngle) {
            // Turns out that using a Shape instead of using a Shader is much faster so we just pipe this
            // through to DrawShape.
            // See https://gist.github.com/shannah/85d93674d709c7733e98 for Shader implementation that we decided
            // not to use.
            if (drawingArcPath == null) {
                drawingArcPath = new GeneralPath();
            }
            drawingArcPath.reset();
            if (arcAngle >= 360 || arcAngle <= -360) {
                // Full circle/ellipse: skip moveTo(center). Without this the
                // path is center -> arc start -> 360 -> close back to
                // center, which rasterises as a pacman with a visible
                // slice line through the fill (broken thumb on Switch).
                drawingArcPath.arc(x, y, width, height, startAngle * Math.PI / 180, arcAngle * Math.PI / 180, false);
            } else {
                drawingArcPath.moveTo(x + width / 2, y + height / 2);
                drawingArcPath.arc(x, y, width, height, startAngle * Math.PI / 180, arcAngle * Math.PI / 180, true);
            }
            drawingArcPath.closePath();
            nativeFillShape(drawingArcPath);
        }

        void nativeDrawString(int color, int alpha, long fontPeer, String str, int x, int y) {
            nativeDrawStringGlobal(color, alpha, fontPeer, str, x, y);
        }

        void nativeDrawImage(long peer, int alpha, int x, int y, int width, int height) {
            nativeDrawImageGlobal(peer, alpha, x, y, width, height, renderingHints);
        }

        @Override
        void nativeDrawAlphaMask(TextureAlphaMask mask) {
            if ( mask != null && mask.getTextureName() != 0 ){
                Rectangle r = mask.getBounds();
                //Log.p("Drawing shape with bounds "+r);
                nativeInstance.drawTextureAlphaMask(mask.getTextureName(), this.color, this.alpha, r.getX(), r.getY(), r.getWidth(), r.getHeight() );
            }
        }
     
        void fillConvexPolygon(float[] points, int color, int alpha) {
            nativeInstance.fillConvexPolygonGlobal(points, color, alpha);
        }

        void drawConvexPolygon(float[] points, int color, int alpha, float lineWidth, int joinStyle, int capStyle, float miterLimit) {
            nativeInstance.drawConvexPolygonGlobal(points, color, alpha, lineWidth, joinStyle, capStyle, miterLimit);
        }
        
        private GeneralPath tmpDrawShape;
        private Transform tmpTransform, tmpTransform2;
        private Rectangle tmpRect2;
        private Stroke tmpDrawStroke;
        private Image coreGraphicsBuffer;
        void nativeDrawShape(Shape shape, Stroke stroke){//float lineWidth, int capStyle, int miterStyle, float miterLimit) {
            
            if (shape instanceof GeneralPath) {
                if (transform == null || transform.isIdentity()) {

                    TextureAlphaMask mask = textureCache.get(shape, stroke);
                    if ( mask == null ){
                        mask = (TextureAlphaMask)createAlphaMask(shape, stroke);
                        textureCache.add(shape, stroke, mask);

                    }
                    if (mask==null){
                        // A null mask generally means the shape had zero bounds
                        return;
                    }
                    //mask = (TextureAlphaMask)createAlphaMask(shape, stroke);
                    nativeDrawAlphaMask(mask);


                } else {
                    if (tmpDrawShape == null) {
                        tmpDrawShape = new GeneralPath();
                    }
                    if (tmpTransform == null) {
                        tmpTransform = Transform.makeIdentity();
                    }
                    if (tmpTransform2 == null) {
                        tmpTransform2 = Transform.makeIdentity();
                    }
                    if (tmpRect2 == null) {
                        tmpRect2 = new Rectangle();
                    }
                    if (tmpDrawStroke == null) {
                        tmpDrawStroke = new Stroke();
                    }
                    // Factor the user transform into a pre-rasterisation scale
                    // (sx, sy) and a residual GPU transform = transform *
                    // S(1/sx, 1/sy). Two strategies:
                    //
                    // - Metal: take sx, sy from the column norms of the 2x2
                    //   linear part of the transform. The path is rasterised
                    //   at the actual per-axis scale so the residual GPU
                    //   transform is pure rotation/shear -- no non-uniform
                    //   texture stretch. This fixes GH-3302: under
                    //   g.translate + non-uniform g.scale + fillShape the
                    //   inscribed shape used to drift off the axis-aligned
                    //   drawRect because the uniform-scale rasterise +
                    //   non-uniform GPU stretch round to different pixel
                    //   grids.
                    //
                    // - GL ES2: keep the legacy uniform h2/h1 diagonal ratio.
                    //   Existing GL goldens are calibrated against this
                    //   behaviour; only Metal opts in to the per-axis
                    //   decomposition.
                    float sx, sy;
                    if (metalRendering) {
                        Matrix nm = (Matrix) transform.getNativeTransform();
                        float[] m = nm.getData();
                        // Column-major 4x4: column 0 = [m[0], m[1], ...],
                        // column 1 = [m[4], m[5], ...]. Length of each column
                        // is the per-axis scale magnitude (true for pure
                        // scale, scale-then-rotate, and rotate-then-scale;
                        // shear contributes to both norms equally).
                        float c0x = m[0], c0y = m[1];
                        float c1x = m[4], c1y = m[5];
                        sx = (float) Math.sqrt((double) c0x * c0x + (double) c0y * c0y);
                        sy = (float) Math.sqrt((double) c1x * c1x + (double) c1y * c1y);
                        if (sx < 1e-6f) sx = 1f;
                        if (sy < 1e-6f) sy = 1f;
                    } else {
                        GeneralPath p = (GeneralPath) shape;
                        Rectangle origBounds = reusableRect;
                        Rectangle transformedBounds = tmpRect2;
                        p.getBounds(origBounds);
                        tmpDrawShape.setShape(shape, transform);
                        tmpDrawShape.getBounds(transformedBounds);
                        double h1 = Math.sqrt(origBounds.getWidth() * origBounds.getWidth() + origBounds.getHeight() * origBounds.getHeight());
                        double h2 = Math.sqrt(transformedBounds.getWidth() * transformedBounds.getWidth() + transformedBounds.getHeight() * transformedBounds.getHeight());
                        if (h2 < 1) h2 = 1;
                        if (h1 < 1) h1 = 1;
                        float scale = (float) (h2 / h1);
                        sx = sy = scale;
                    }
                    // Stroke widening: in path space the renderer can only
                    // produce a circular pen, but the residual GPU transform
                    // does not scale (Metal) or applies a non-uniform stretch
                    // (GL legacy). Use the geometric mean of the per-axis
                    // scales so the on-screen stroke matches what the user
                    // asked for on average; when sx == sy this collapses to
                    // the uniform legacy behaviour.
                    float strokeScale = (sx == sy) ? sx : (float) Math.sqrt((double) sx * (double) sy);
                    tmpTransform.setScale(sx, sy);
                    tmpDrawShape.setShape(shape, tmpTransform);

                    if (stroke != null) {
                        tmpDrawStroke.setStroke(stroke);
                        tmpDrawStroke.setLineWidth(tmpDrawStroke.getLineWidth() * strokeScale);
                    }
                    TextureAlphaMask mask = textureCache.get(tmpDrawShape, stroke==null?null:tmpDrawStroke);
                    if ( mask == null ){
                        mask = (TextureAlphaMask)createAlphaMask(tmpDrawShape, stroke==null?null:tmpDrawStroke);
                        textureCache.add(tmpDrawShape, stroke==null?null:tmpDrawStroke, mask);
                    }
                    if (mask==null){
                        return;
                    }
                    if (paint != null && paint instanceof RadialGradient) {
                        RadialGradient rgp = (RadialGradient)paint;
                        rgp.x = (int) (rgp.x * sx);
                        rgp.y = (int) (rgp.y * sy);
                        rgp.width = (int) (rgp.width * sx);
                        rgp.height = (int) (rgp.height * sy);
                        applyPaint();
                    }
                    // Apply the residual S(1/sx, 1/sy) via the impl-side scale
                    // path (the same path g.scale uses). Going through
                    // setTransform with a separately-built composed Transform
                    // has been documented to silently fail to update the
                    // Metal-side currentTransform (see the comment in
                    // Transform.setTransform). The scale path reliably queues
                    // a SetTransform op that reaches both the screen and the
                    // mutable-image encoders.
                    scale(1f / sx, 1f / sy);
                    try {
                        nativeDrawAlphaMask(mask);
                    } finally {
                        scale(sx, sy);
                    }
                }
            } else {
                Log.p("drawShape() only supported for GeneralPaths currently");
            }
        }

        
        /**
         * Draws a path on the current graphics context.
         *
         * @param graphics the graphics context
         * @param path the path to draw.
         */
        void nativeFillShape(Shape shape) {
            nativeDrawShape(shape, null);
        }
        
        boolean isTransformSupported(){
            //return nativeInstance.nativeIsTransformSupportedGlobal();
            return true; // Since they both support it now.
        }
        
        boolean isPerspectiveTransformSupported(){
            return nativeInstance.nativeIsPerspectiveTransformSupportedGlobal();
        }
        
        boolean isShapeSupported(){
            //return nativeInstance.nativeIsShapeSupportedGlobal();
            return true;
        }
        
        
        boolean isAlphaMaskSupported(){
            return true;
            //return nativeInstance.nativeIsAlphaMaskSupportedGlobal();
        }
        
        public void fillRectRadialGradient(int startColor, int endColor, int x, int y, int width, int height, float relativeX, float relativeY, float relativeSize) {
            nativeInstance.fillRectRadialGradientGlobal(startColor, endColor, x, y, width, height, relativeX, relativeY, relativeSize);
        }
    
        public void fillLinearGradient(int startColor, int endColor, int x, int y, int width, int height, boolean horizontal) {
            nativeInstance.fillLinearGradientGlobal(startColor, endColor, x, y, width, height, horizontal);
        }
        
    }

    public static long getFontPeer(NativeFont font) {
        return font.peer;
    }
    
    class NativeFont {
        long peer;
        int style;
        int face;
        int size;
        String name;
        int weight;
        float height;
        int maxStringLength = -1;
        private final Map<Character, Integer> widthCache = new HashMap<Character, Integer>();
        
        public NativeFont() {
        }
        
        public int getMaxStringLength() {
            if(maxStringLength == -1) {
                int w = charWidth('X');
                maxStringLength = Math.max(getDisplayWidth(), getDisplayHeight()) * 2 / w;
            }
            return maxStringLength;
        }
        
        public int charWidth(char c) {
            Character chr = new Character(c);
            Integer w = widthCache.get(chr);
            if(w != null) {
                return w.intValue();
            }
            int v = charWidthNative(peer, c);
            widthCache.put(chr, v);
            return v;
        }
        
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof NativeFont)) {
                return false;
            }
            NativeFont f = (NativeFont)o;
            if (name != null || f.name != null) {
                return name != null && name.equals(f.name) && f.weight == weight && f.height == height;
            }
            if (weight != 0 || height != 0 || f.weight != 0 || f.height != 0) {
                return f.style == style && f.face == face && f.weight == weight && f.height == height;
            }
            return f.style == style && f.face == face && f.size == size;
        }
        
        public int hashCode() {
            int result;
            if (name != null) {
                result = name.hashCode();
                result = 31 * result + weight;
                result = 31 * result + Float.floatToIntBits(height);
                return result;
            }
            if (weight != 0 || height != 0) {
                result = style;
                result = 31 * result + face;
                result = 31 * result + weight;
                result = 31 * result + Float.floatToIntBits(height);
                return result;
            }
            return style | face | size;
        }

        // this might be a problem with font caching
        /*protected void finalize() {
            if(peer != 0) {
                CodenameOneiPhoneNative.deleteNativeFontPeer(peer);
            }
        }*/
    }

    class NativeImage {
        boolean scaled;
        NativeGraphics child;
        int width;
        int height;
        long peer;
        String debugText;
        public NativeImage(String debugText) {
            this.debugText = debugText;
        }
        public String toString() {
            return debugText;
        }

        public NativeGraphics getGraphics() {
            if(child == null) {
                child = new NativeGraphics();
                child.associatedImage = this;
            }
            return child;
        }
        
        void deleteImage() {
            if(peer != 0) {
                deleteNativePeer(peer);
                peer = 0;
            }            
        }

        protected void finalize() {
            deleteImage();
        }
    }
    
    @Override
    public boolean animateImage(Object nativeImage, long lastFrame) {
        return super.animateImage(nativeImage, lastFrame);
    }

    @Override
    public void browserBack(PeerComponent browserPeer) {
        nativeInstance.browserBack(get(browserPeer));
    }

    @Override
    public void browserStop(PeerComponent browserPeer) {
        nativeInstance.browserStop(get(browserPeer));
    }
    
    @Override
    public void browserClearHistory(PeerComponent browserPeer) {
        nativeInstance.browserClearHistory(get(browserPeer));
    }

    @Override
    public void browserExecute(PeerComponent browserPeer, String javaScript) {
        nativeInstance.browserExecute(get(browserPeer), javaScript);
    }
    
    public boolean supportsExecuteAndReturnString(final PeerComponent browserPeer) {
        return true;
    }
    
    @Override
    public String browserExecuteAndReturnString(final PeerComponent browserPeer, final String javaScript) {
        if (disableUIWebView || !Boolean.FALSE.equals(browserPeer.getClientProperty("BrowserComponent.useWKWebView"))) {
            final String[] res = new String[1];
            final boolean[] complete = new boolean[1];
            nativeInstance.browserExecuteAndReturnStringCallback(get(browserPeer), javaScript, new SuccessCallback<String>() {
                @Override
                public void onSucess(String value) {
                    synchronized(complete) {
                        res[0] = value;
                        complete[0] = true;
                        complete.notify();
                    }
                }
                
            });
            while (!complete[0]) {
                synchronized(complete) {
                    Util.wait(complete);
                }
            }
            return res[0];
        }
        if(Display.getInstance().isEdt()) {
            final String[] result = new String[1];

            // We cannot block the EDT so we use invokeAndBlock. This is very
            // important since Javascript may try to communicate with the EDT
            // from inside the script.
            Display.getInstance().invokeAndBlock(new Runnable(){
                public void run() {
                    result[0] = nativeInstance.browserExecuteAndReturnString(get(browserPeer), javaScript);
                }
            });
            return result[0];
        } 
        return nativeInstance.browserExecuteAndReturnString(get(browserPeer), javaScript);        
    }

    @Override
    public void browserExposeInJavaScript(PeerComponent browserPeer, Object o, String name) {
        // TODO
    }

    @Override
    public void browserForward(PeerComponent browserPeer) {
        nativeInstance.browserForward(get(browserPeer));
    }

    @Override
    public boolean browserHasBack(PeerComponent browserPeer) {
        return nativeInstance.browserHasBack(get(browserPeer));
    }

    @Override
    public boolean browserHasForward(PeerComponent browserPeer) {
        return nativeInstance.browserHasForward(get(browserPeer));
    }

    @Override
    public void browserReload(PeerComponent browserPeer) {
        nativeInstance.browserReload(get(browserPeer));
    }

    @Override
    public void lockScreen(){
        nativeInstance.lockScreen();
    }
    
    @Override
    public void unlockScreen(){
        nativeInstance.unlockScreen();
    }

    @Override
    public boolean isScreenLockSupported() {
        return true;
    }
    
    
    @Override
    public boolean canForceOrientation() {
        return true;
    }

    /*@Override
    public void playAudio(Object handle) {
        long[] l = (long[])handle;
        if(l[0] == 0) {
            return;
        }
        nativeInstance.playAudio(l[0]);
    }

    @Override
    public void cleanupAudio(Object handle) {
        long[] l = (long[])handle;
        if(l[0] == 0) {
            return;
        }
        l[0] = 0;
        nativeInstance.cleanupAudio(l[0]);
    }*/

    private int dDensity = -1;
    
    @Override
    public int getDeviceDensity() {
        // IMPORTANT:  If you modify this method, you MUST make the equivalent changes
        // to the getDeviceDensity() method in the Shooter project or the iOS screenshots
        // will produce slightly different results than the actual device.
        
        if(dDensity == -1) {
            if(Display.getInstance().getProperty("ios.densityOld", "false").equals("true")) {
                dDensity = super.getDeviceDensity();
                return dDensity;
            }
            int dispWidth = getDisplayWidth();
            int dispHeight = getDisplayHeight();
            // ipad mini is ignored, there is no sensible way to detect it
            if(isTablet()) {
                if(dispWidth < 1100) {
                    dDensity = Display.DENSITY_MEDIUM;
                    return dDensity;
                }
                dDensity = Display.DENSITY_VERY_HIGH;
                return dDensity;
            } else {
                if(dispWidth < 500) {
                    dDensity = Display.DENSITY_MEDIUM;
                    return dDensity;
                }
                int largest = Math.max(dispWidth, dispHeight);
                int smallest = Math.min(dispWidth, dispHeight);
                if (largest == 2340 && smallest == 1080) {
                    // 12 mini
                    //ppi = PPI_476;
                    dDensity = Display.DENSITY_560;
                    return dDensity;
                }
                else if (largest == 2532 && smallest == 1170) {
                    // iPhone 12, 12 Pro, 13, 13 Pro, 14
                    dDensity = Display.DENSITY_560;
                    return dDensity;
                }
                else if (largest == 2556 && smallest == 1179) {
                    // iPhone 14 Pro, 15, 15 Pro, 16
                    //ppi = PPI_460;
                    dDensity = Display.DENSITY_560;
                    return dDensity;
                }
                else if (largest == 2796 && smallest == 1290) {
                    // iPhone 14 Pro Max, 15 Plus, 15 Pro Max, 16 Plus
                    //ppi = PPI_460;
                    dDensity = Display.DENSITY_560;
                    return dDensity;
                }
                else if (largest == 2622 && smallest == 1206) {
                    // iPhone 16 Pro
                    //ppi = PPI_460;
                    dDensity = Display.DENSITY_560;
                    return dDensity;
                }
                else if (largest == 2868 && smallest == 1320) {
                    // iPhone 16 Pro Max
                    //ppi = PPI_460;
                    dDensity = Display.DENSITY_560;
                    return dDensity;
                }
                else if (largest == 2778 && smallest == 1284) {
                    // iPhone 12 Pro Max, 13 Pro Max, 14 Plus
                    //ppi = PPI_458;
                    dDensity = Display.DENSITY_560;
                    return dDensity;
                }
                else if (largest == 1792 && smallest == 828) {
                    // iPhone 11, XR
                    //ppi = PPI_326;
                    dDensity = Display.DENSITY_VERY_HIGH;
                    return dDensity;
                } else if (largest == 2688 && smallest == 1242) {
                    // iPhone 11 Pro Max, Xs Max
                    //ppi = PPI_458;
                    dDensity = Display.DENSITY_560;
                    return dDensity;
                } else if (largest == 2208 && smallest == 1242) {
                    // 6+, 6s, 7+, 8+
                    //ppi = PPI_401;
                    dDensity = Display.DENSITY_HD;
                    return dDensity;
                } else if (largest == 1334 && smallest == 750) {
                    // 6, 6s, 7, 8
                    //ppi = PPI_326;
                    dDensity = Display.DENSITY_VERY_HIGH;
                    return dDensity;
                } else if (largest == 1136 && smallest == 640) {
                    //5, 5s, 5c, SE
                    //ppi = PPI_326;
                    dDensity = Display.DENSITY_VERY_HIGH;
                    return dDensity;
                } else if (largest == 960 && smallest == 640) {
                    // 4, 4s
                    //ppi = PPI_326;
                    dDensity = Display.DENSITY_VERY_HIGH;
                    return dDensity;
                } else if (largest == 480 && smallest == 320) {
                    //2G, 3G, 3GS
                    //ppi = PPI_163;
                    dDensity = Display.DENSITY_MEDIUM;
                    return dDensity;
                }
                else if (largest == 2436) {
                    // iphone X
                    //ppi = 18.031496062992126;
                    dDensity = Display.DENSITY_560;
                    return dDensity;
                } 
                else if(largest > 2000) {
                    dDensity = Display.DENSITY_560;
                    return dDensity;
                }
                dDensity = Display.DENSITY_VERY_HIGH;
                return dDensity;
            }
        }
        return dDensity;
    }
    
    double ppi = 0;
    private static final double PPI_458 = 18.031496062992126;
    private static final double PPI_326 = 12.834645669291339;
    private static final double PPI_401 = 15.78740157480315;
    private static final double PPI_163 = 6.417322834645669;
    private static final double PPI_476 = 18.740157480314963;
    private static final double PPI_460 = 18.11023622047244;
    
    @Override
    public int convertToPixels(int dipCount, boolean horizontal) {
        // IMPORTANT:  If you modify this method, you MUST make the equivalent changes
        // to the convertToPixels() method in the Shooter project or the iOS screenshots
        // will produce slightly different results than the actual device.
        
        // ipad mini is ignored, there is no sensible way to detect it
        if(ppi == 0) {
            int dispWidth = getDisplayWidth();
            if(isTablet()) {
                if(dispWidth < 1100) {
                    ppi = 5.1975051975052;
                } else {
                    ppi = 10.3939299449122;
                }
            } else {
                if(dispWidth < 500) {
                    ppi = 6.4173236936575;
                } else {
                    int dispHeight = getDisplayHeight();
                    int largest = Math.max(dispWidth, dispHeight);
                    int smallest = Math.min(dispWidth, dispHeight);
                    if (largest == 2340 && smallest == 1080) {
                        // 12 mini
                        ppi = PPI_476;
                    }
                    else if (largest == 2532 && smallest == 1170) {
                        // iPhone 12, 12 Pro, 13, 13 Pro, 14
                        ppi = PPI_460;
                    }
                    else if (largest == 2556 && smallest == 1179) {
                        // iPhone 14 Pro, 15, 15 Pro, 16
                        ppi = PPI_460;
                    }
                    else if (largest == 2796 && smallest == 1290) {
                        // iPhone 14 Pro Max, 15 Plus, 15 Pro Max, 16 Plus
                        ppi = PPI_460;
                    }
                    else if (largest == 2622 && smallest == 1206) {
                        // iPhone 16 Pro
                        ppi = PPI_460;
                    }
                    else if (largest == 2868 && smallest == 1320) {
                        // iPhone 16 Pro Max
                        ppi = PPI_460;
                    }
                    else if (largest == 2778 && smallest == 1284) {
                        // iPhone 12 Pro Max, 13 Pro Max, 14 Plus
                        ppi = PPI_458;
                    }
                    else if (largest == 1792 && smallest == 828) {
                        // iPhone 11, XR
                        ppi = PPI_326;
                    } else if (largest == 2688 && smallest == 1242) {
                        // iPhone 11 Pro Max, Xs Max
                        ppi = PPI_458;
                    } else if (largest == 2208 && smallest == 1242) {
                        // 6+, 6s+, 7+, 8+
                        ppi = PPI_401;
                    } else if (largest == 1334 && smallest == 750) {
                        // 6, 6s, 7, 8, SE 2nd/3rd gen
                        ppi = PPI_326;
                    } else if (largest == 1136 && smallest == 640) {
                        //5, 5s, 5c, SE 1st gen
                        ppi = PPI_326;
                    } else if (largest == 960 && smallest == 640) {
                        // 4, 4s
                        ppi = PPI_326;
                    } else if (largest == 480 && smallest == 320) {
                        //2G, 3G, 3GS
                        ppi = PPI_163;
                    }
                    else if (largest == 2436) {
                        // iPhone X, Xs, 11 Pro
                        ppi = PPI_458;
                    } else {
                        // Unknown 3x device. Apple has held 460 ppi for every
                        // non-Plus iPhone since the iPhone 12, so default future
                        // phones to that rather than the legacy 6 Plus value.
                        if (largest > 2000) {
                            ppi = PPI_460;
                        } else {
                            // Older 2x device fallback (~PPI_326).
                            ppi = 12.8369704749679;
                        }
                    }
                }
            }
        }
        return (int)Math.round((((float)dipCount) * ppi));
    }
    
    
    @Override
    public Object getPasteDataFromClipboard() {
        String s = nativeInstance.getClipboardString();
        if(s != null) {
            return s;
        }
        return super.getPasteDataFromClipboard();
    }
    
    @Override
    public void copyToClipboard(Object obj) {
        if(obj instanceof String) {
            nativeInstance.setClipboardString((String)obj);
            super.copyToClipboard(obj);
            return;
        }
        nativeInstance.setClipboardString(null);
        super.copyToClipboard(obj);
    }

    /*class RunnableCleanup implements Runnable {
        long[] peer;
        Runnable onCompletion;
        public void run() {
            if(onCompletion != null) {
                onCompletion.run();
            }
            if(peer != null && peer[0] != 0) {
                cleanupAudio(peer);
            }
        }
    }
    
    @Override
    public Object createAudio(String uri, Runnable onCompletion) throws IOException {
        RunnableCleanup c = new RunnableCleanup();
        long[] p = new long[] {nativeInstance.createAudio(uri, c)};
        c.peer = p;
        c.onCompletion = onCompletion;
        return p;
    }

    
    @Override
    public Object createAudio(InputStream stream, String mimeType, Runnable onCompletion) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int size = stream.read(buffer);
        while(size > -1) {
            bo.write(buffer, 0, size);
            size = stream.read(buffer);
        }
        bo.close();
        stream.close();
        RunnableCleanup c = new RunnableCleanup();
        long[] p = new long[] {nativeInstance.createAudio(bo.toByteArray(), c)};
        c.peer = p;
        c.onCompletion = onCompletion;
        return p;
    }*/

    @Override
    public PeerComponent createBrowserComponent(Object browserComponent) {
        boolean useWKWebView = disableUIWebView || 
                (browserComponent instanceof Component && 
                !Boolean.FALSE.equals(((Component)browserComponent).getClientProperty("BrowserComponent.useWKWebView")));
        if (disableUIWebView && (browserComponent instanceof Component && 
                Boolean.FALSE.equals(((Component)browserComponent).getClientProperty("BrowserComponent.useWKWebView")))) {
            Log.p("The BrowserComponent.useWKWebView flag is currently disabled because Apple no longer allows apps that use the old UIWebView into the App Store.  You should remove calls to Display.setProperty(\"BrowserComponent.useWKWebView\", \"false\") from your codebase.");
        }
        long browserPeer = useWKWebView ? 
                nativeInstance.createWKBrowserComponent(browserComponent) : 
                nativeInstance.createBrowserComponent(browserComponent);
        PeerComponent pc = createNativePeer(new long[] {browserPeer});
        pc.putClientProperty("BrowserComponent.useWKWebView", useWKWebView);
        nativeInstance.releasePeer(browserPeer);
        return pc;
    }

    /*@Override
    public VideoComponent createVideoPeer(String url) throws IOException {
        return new NativeIPhoneVideoPeer(new long[] {nativeInstance.createVideoComponent(url)});
    }

    @Override
    public VideoComponent createVideoPeer(InputStream stream, String type) throws IOException {
        System.out.println("Unfortunately iPhone's don't support video streaming");
        throw new UnsupportedOperationException("Unfortunately iPhone's don't support video streaming");
    }
    
    class NativeIPhoneVideoPeer extends VideoComponent {
        private long[] nativePeer;
        
        public NativeIPhoneVideoPeer(Object nativePeer) {
            super(nativePeer);
            this.nativePeer = (long[])nativePeer;
            nativeInstance.retainPeer(this.nativePeer[0]);
        }
        
        public void finalize() {
            if(nativePeer[0] != 0) {
                nativeInstance.releasePeer(nativePeer[0]);            
            }
        }
        
        public boolean isFocusable() {
            return true;
        }

        public void setFocus(boolean b) {
        }

        protected Dimension calcPreferredSize() {
            if(nativePeer == null || nativePeer[0] == 0) {
                return new Dimension();
            }
            int[] p = new int[2];
            nativeInstance.calcPreferredSize(nativePeer[0], getDisplayWidth(), getDisplayHeight(), p);
            return new Dimension(p[0], p[1]);
        }

        protected void onPositionSizeChange() {
            if(nativePeer != null && nativePeer[0] != 0) {
                nativeInstance.updatePeerPositionSize(nativePeer[0], getAbsoluteX(), getAbsoluteY(), getWidth(), getHeight());
            }
        }

        protected void initComponent() {
            if(nativePeer != null && nativePeer[0] != 0) {
                nativeInstance.peerInitialized(nativePeer[0], getAbsoluteX(), getAbsoluteY(), getWidth(), getHeight());
            }
        }

        protected void deinitialize() {
            if(nativePeer != null && nativePeer[0] != 0) {
                nativeInstance.peerDeinitialized(nativePeer[0]);
            }
        }

        @Override
        public void start() {
            if(nativePeer != null && nativePeer[0] != 0) {
                nativeInstance.startVideoComponent(nativePeer[0]);
            }
        }

        @Override
        public void stop() {
            if(nativePeer != null && nativePeer[0] != 0) {
                nativeInstance.startVideoComponent(nativePeer[0]);
            }
        }

        @Override
        public void setLoopCount(int count) {
        }

        @Override
        public int getMediaTimeMS() {
            if(nativePeer != null && nativePeer[0] != 0) {
                return nativeInstance.getMediaTimeMS(nativePeer[0]);
            }
            return -1;
        }

        @Override
        public int setMediaTimeMS(int now) {
            if(nativePeer != null && nativePeer[0] != 0) {
                return nativeInstance.setMediaTimeMS(nativePeer[0], now);
            }
            return -1;
        }

        @Override
        public int getMediaDuration() {
            if(nativePeer != null && nativePeer[0] != 0) {
                return nativeInstance.getMediaDuration(nativePeer[0]);
            }
            return -1;
        }

        @Override
        public boolean isPlaying() {
            if(nativePeer != null && nativePeer[0] != 0) {
                return nativeInstance.isVideoPlaying(nativePeer[0]);
            }
            return false;
        }

        @Override
        public void setFullScreen(boolean fullscreen) {
            if(nativePeer != null && nativePeer[0] != 0) {
                nativeInstance.setVideoFullScreen(nativePeer[0], fullscreen);
            }
        }

        @Override
        public boolean isFullScreen() {
            if(nativePeer != null && nativePeer[0] != 0) {
                return nativeInstance.isVideoFullScreen(nativePeer[0]);
            }
            return false;
        }

        @Override
        public void close() {
        }

        @Override
        public void setMediaListener(MediaListener l) {
        }
    }*/

    @Override
    public void drawImage(Object graphics, Object img, int x, int y, int w, int h) {
        if (img == null) return;
        NativeGraphics ng = (NativeGraphics)graphics;
        //System.out.println("Drawing image " + img);
        ng.checkControl();
        ng.applyTransform();
        ng.applyClip();
        NativeImage nm = (NativeImage)img;
        ng.nativeDrawImage(nm.peer, ng.alpha, x, y, w, h);
    }

    @Override
    public void drawImageArea(Object nativeGraphics, Object img, int x, int y, int imageX, int imageY, int imageWidth, int imageHeight) {
        super.drawImageArea(nativeGraphics, img, x, y, imageX, imageY, imageWidth, imageHeight);
    }

    @Override
    public void drawPolygon(Object graphics, int[] xPoints, int[] yPoints, int nPoints) {
        super.drawPolygon(graphics, xPoints, yPoints, nPoints);
    }

    @Override
    public boolean isBadgingSupported() {
        return true;
    }

    @Override
    public void setBadgeNumber(int number) {
        nativeInstance.setBadgeNumber(number);
    }

    @Override
    public Boolean canExecute(String url) {
        if (url.startsWith("file:")) {
            url = "file:"+unfile(url);
        }
        if(nativeInstance.canExecute(url)) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }
    
    @Override
    public void execute(String url) {
        if (url.startsWith("file:")) {
            url = "file:"+unfile(url);
        }
        nativeInstance.execute(url);
    }

    @Override
    public boolean isOpenNativeNavigationAppSupported(){
        return true;
    }
    
    @Override
    public void openNativeNavigationApp(double latitude, double longitude){    
        String s = "http://maps.apple.com/?daddr=" + latitude+ "," + longitude;
        if(canExecute(s)) {
            execute(s);
        } else {
            execute("http://maps.apple.com/?ll=" + latitude+ "," + longitude);
        }
    }
    
    @Override
    public void openNativeNavigationApp(String location) {    
        execute("http://maps.apple.com/?q=" + Util.encodeUrl(location));
    }

    @Override
    public void flashBacklight(int duration) {
        nativeInstance.flashBacklight(duration);
    }

    /*@Override
    public int getAudioDuration(Object handle) {
        long[] l = (long[])handle;
        if(l[0] == 0) {
            return -1;
        }
        return nativeInstance.getAudioDuration(l[0]);
    }

    @Override
    public int getAudioTime(Object handle) {
        long[] l = (long[])handle;
        if(l[0] == 0) {
            return -1;
        }
        return nativeInstance.getAudioTime(l[0]);
    }*/

    @Override
    public String getBrowserTitle(PeerComponent browserPeer) {
        return nativeInstance.getBrowserTitle(get(browserPeer));
    }

    @Override
    public String getBrowserURL(PeerComponent browserPeer) {
        return nativeInstance.getBrowserURL(get(browserPeer));
    }


    @Override
    public int getFace(Object nativeFont) {
        return f(nativeFont).face;
    }

    @Override
    public String[] getFontPlatformNames() {
        // TODO
        return super.getFontPlatformNames();
    }

    @Override
    public int getKeyboardType() {
        return Display.KEYBOARD_TYPE_VIRTUAL;
    }

    /**
     * Callback for the native layer
     */
    public static void fireWebViewError(BrowserComponent bc, int code) {
        bc.fireWebEvent("onError", new ActionEvent("", code));
    }

    /**
     * Callback for the native layer
     */
    public static void fireWebViewDidFinishLoad(BrowserComponent bc, String url) {
        bc.fireWebEvent("onLoad", new ActionEvent(url));
    }
    
    /**
     * Callback for the native layer
     */
    public static void fireWebViewDidStartLoad(BrowserComponent bc, String url) {
        bc.fireWebEvent("onStart", new ActionEvent(url));
    }

    @Override
    public String getAppArg() {
        // We need special handling of AppArg to avoid race conditions.
        // AppArg is guaranteed to be set by the time 
        // applicationDidBecomeActive() is called, so in some cases
        // calling AppArg inside the start() method of the lifecycle will
        // get a stale value.
        // See the lifecycle here:
        // https://developer.apple.com/library/ios/documentation/iPhone/Conceptual/iPhoneOSProgrammingGuide/Inter-AppCommunication/Inter-AppCommunication.html#//apple_ref/doc/uid/TP40007072-CH6-SW13
        if (!minimized && !isActive && Display.getInstance().isEdt()) {
            // !minimized = applicationWillEnterForeground has already run
            // !isActive = applicationDidBecomeActive hasn't been called yet.
            // => We will do some "waiting" to give the AppArg a chance
            // to be changed.
            // We only defer access to AppArg if we are on the EDT
            // to avoid a possible dead-lock when on the main thread
            // The case we are concerned about is only when
            // calling inside the start() method, so this will be
            // on the EDT.
            // In all other cases, this property should just return
            // unhindered.
            Display.getInstance().invokeAndBlock(new Runnable() {
                @Override
                public void run() {
                    final Object lock = new Object();
                    final boolean[] complete = new boolean[1];
                    callOnActive(new Runnable() {

                        @Override
                        public void run() {
                            complete[0] = true;
                            synchronized(lock) {
                                lock.notifyAll();
                            }
                        }

                    });
                    while (!complete[0]) {
                        synchronized(lock) {
                            try {
                                lock.wait(100); // Wait long enough for the url handler
                                                // to kick in.
                                // I think it's better just to skip and move on
                                // after 100ms rather than wait indefinitely just
                                // in case we are running in the background
                                break;
                            } catch (InterruptedException ex) {
                                break;
                            }
                        }
                    }
                }
            });

            
        }
        return super.getAppArg();
    }

    private static Map<String,AsyncResource> callbacks = new HashMap<String,AsyncResource>();
    
    static void completeStringCallback(String callbackId, String value) {
        AsyncResource<String> res = (AsyncResource<String>)callbacks.get(callbackId);
        if (res != null) {
            res.complete(value);
        }
    }

    
    @Override
    public String getProperty(String key, String defaultValue) {
        if(key.equalsIgnoreCase("cn1_push_prefix")) {
            return "ios";
        }
        if(key.equalsIgnoreCase("Platform")) {
            return "iOS";
        }
        if(key.equalsIgnoreCase("os.gzip")) {
            return "true";
        }
        if(key.equalsIgnoreCase("OS")) {
            return "iOS";
        }

        if(key.equalsIgnoreCase("User-Agent")) {
            /*if(isTablet()) {
                return "Mozilla/5.0 (iPad; U; CPU OS 3_2 like Mac OS X; en-us) AppleWebKit/531.21.10 (KHTML, like Gecko) Version/4.0.4 Mobile/7B334b Safari/531.21.10";
            } 
            return "Mozilla/5.0 (iPhone; U; CPU like Mac OS X; en) AppleWebKit/420+ (KHTML, like Gecko) Version/3.0 Mobile/1C25 Safari/419.3";*/
            if(userAgent == null) {
                final String callbackId = key+System.currentTimeMillis();
                AsyncResource<String> out = new AsyncResource<String>() {
                    @Override
                    public void complete(String value) {
                        callbacks.remove(callbackId);
                        super.complete(value); 
                    }

                    @Override
                    public void error(Throwable t) {
                        callbacks.remove(callbackId);
                        super.error(t);
                    }
                };
                callbacks.put(callbackId, out);
                userAgent = nativeInstance.getUserAgentString(callbackId);
                if (userAgent == null) {
                    try {
                        userAgent = out.get();
                    } catch (Exception ex) {
                        Log.e(ex);
                    }
                }
            }
            return userAgent;
        }
        if(key.equalsIgnoreCase("AppVersion")) {
            // make app version case insensitive
            return super.getProperty("AppVersion", "");
        }
        if("OSVer".equals(key)) {
            return nativeInstance.getOSVersion();
        }
        if("DeviceName".equals(key)) {
            return nativeInstance.getDeviceName();
        }
        if("DeviceHardwareModel".equals(key)) {
            return nativeInstance.getDeviceHardwareModel();
        }
        if(key.equalsIgnoreCase("UDID")) {
            return nativeInstance.getUDID();
        }
        if("cn1.iosStatusBarTap.count".equals(key)) {
            return String.valueOf(nativeInstance.getStatusBarTapCount());
        }
        if("cn1.iosStatusBarTap.lastEpochMillis".equals(key)) {
            return String.valueOf(nativeInstance.getStatusBarTapLastEpochMillis());
        }
        if("cn1.iosStatusBarTap.lastX".equals(key)) {
            return String.valueOf(nativeInstance.getStatusBarTapLastX());
        }
        if("cn1.iosStatusBarTap.lastY".equals(key)) {
            return String.valueOf(nativeInstance.getStatusBarTapLastY());
        }
        if("cn1.iosStatusBarTap.proxyInstalled".equals(key)) {
            return String.valueOf(nativeInstance.isStatusBarTapProxyInstalled());
        }
        if("cn1.iosStatusBarTap.diagnostics".equals(key)) {
            int count = nativeInstance.getStatusBarTapCount();
            long lastTime = nativeInstance.getStatusBarTapLastEpochMillis();
            int lastX = nativeInstance.getStatusBarTapLastX();
            int lastY = nativeInstance.getStatusBarTapLastY();
            boolean installed = nativeInstance.isStatusBarTapProxyInstalled();
            StringBuilder sb = new StringBuilder();
            sb.append("count=").append(count);
            sb.append(", lastEpochMillis=").append(lastTime);
            sb.append(", lastX=").append(lastX);
            sb.append(", lastY=").append(lastY);
            sb.append(", proxyInstalled=").append(installed);
            return sb.toString();
        }

        return super.getProperty(key, defaultValue);
    }

    @Override
    public int getSize(Object nativeFont) {
        return f(nativeFont).size;
    }

    @Override
    public int getStyle(Object nativeFont) {
        return f(nativeFont).style;
    }

    /*@Override
    public int getVolume() {
        return (int)(nativeInstance.getVolume() * 100);
    }*/

    @Override
    public boolean isAlphaMutableImageSupported() {
        return true;
    }

    @Override
    public boolean isAnimation(Object nativeImage) {
        // TODO
        return super.isAnimation(nativeImage);
    }

    @Override
    public boolean isAntiAliased(Object graphics) {
        return ((NativeGraphics)graphics).isAntiAliased();
    }
    

    @Override
    public boolean isAntiAliasedText(Object graphics) {
        return ((NativeGraphics)graphics).isAntiAliasedText();
    }

    @Override
    public boolean isAntiAliasedTextSupported() {
        return true;
    }

    @Override
    public boolean isAntiAliasedTextSupported(Object graphics) {
        return ((NativeGraphics)graphics).isAntiAliasTextSupported();
    }
    
    @Override
    public boolean isAntiAliasingSupported() {
        return true;
    }
    
    public boolean isAntiAliasingSupported(Object graphics) {
        return ((NativeGraphics)graphics).isAntiAliasingSupported();
    }

    @Override
    public boolean isLookupFontSupported() {
        // TODO
        return super.isLookupFontSupported();
    }

    @Override
    public boolean isMinimized() {
        // SJH Nov. 17, 2015 : Removing native isMinimized() method because it conflicted with
        // tracking on the java side.  It caused the app to still be minimized inside start()
        // method.  
        // Related to this issue https://groups.google.com/forum/?utm_medium=email&utm_source=footer#!msg/codenameone-discussions/Ajo2fArN8mc/KrF_e9cTDwAJ
        //return minimized || nativeInstance.isMinimized();
        return minimized;
    }

    @Override
    public boolean isMultiTouch() {
        return true;
    }

    @Override
    public boolean isNativeBrowserComponentSupported() {
        return true;
    }

    @Override
    public void setPinchToZoomEnabled(PeerComponent browserComponent, boolean e) {
        nativeInstance.setPinchToZoomEnabled(get(browserComponent), e);
    }

    @Override
    public void setNativeBrowserScrollingEnabled(PeerComponent browserComponent, boolean e) {
        nativeInstance.setNativeBrowserScrollingEnabled(get(browserComponent), e);
    }

    @Override
    public boolean isOpaque(Image codenameOneImage, Object nativeImage) {
        // TODO
        return super.isOpaque(codenameOneImage, nativeImage);
    }

    @Override
    public boolean isScaledImageDrawingSupported() {
        return true;
    }
    
    @Override
    public boolean isNativeVideoPlayerControlsIncluded() {
        return true;
    }
    
    @Override
    public void sendMessage(String[] recieptents, String subject, Message msg) {
        String[] attachments = null;
        String[] attachmentMime = null;
        
        if(msg.getAttachments().size() > 0) {
            int counter = 0;
            attachments = new String[msg.getAttachments().size()];
            attachmentMime = new String[attachments.length];
            for(String s : msg.getAttachments().keySet()) {
                String mime = msg.getAttachments().get(s);
                attachments[counter] = s;
                attachmentMime[counter] = mime;
                counter++;
            }
        }
        
        nativeInstance.sendEmailMessage(recieptents, subject, msg.getContent(),  
                attachments, attachmentMime, msg.getMimeType().equals(Message.MIME_HTML));
    }

    @Override
    public boolean isContactsPermissionGranted() {
        final boolean[] f = new boolean[1];
        Display.getInstance().invokeAndBlock(new Runnable() {

            @Override
            public void run() {
                f[0] = nativeInstance.isContactsPermissionGranted();
            }
        });
        return f[0];
    }

    @Override
    public String createContact(String firstName, String surname, String officePhone, String homePhone, String cellPhone, String email) {
        if (!nativeInstance.checkContactsUsage()) {
            throw new RuntimeException("Please add the ios.NSContactsUsageDescription build hint");
        }
        return nativeInstance.createContact(firstName, surname, officePhone, homePhone, cellPhone, email);
    }

    @Override
    public boolean deleteContact(String id) {
        if (!nativeInstance.checkContactsUsage()) {
            throw new RuntimeException("Please add the ios.NSContactsUsageDescription build hint");
        }
        return nativeInstance.deleteContact(Integer.parseInt(id));
    }
    
    
    @Override
    public String[] getAllContacts(boolean withNumbers) {
        if (!nativeInstance.checkContactsUsage()) {
            throw new RuntimeException("Please add the ios.NSContactsUsageDescription build hint");
        }
        int[] c = new int[nativeInstance.getContactCount(withNumbers)];
        int clen = c.length;
        nativeInstance.getContactRefIds(c, withNumbers);
        String[] r = new String[clen];
        for(int iter = 0 ; iter < clen ; iter++) {
            r[iter] = "" + c[iter];
        }
        return r;
    }

    @Override
    public void refreshContacts() {
        if (!nativeInstance.checkContactsUsage()) {
            throw new RuntimeException("Please add the ios.NSContactsUsageDescription build hint");
        }
        nativeInstance.refreshContacts();
    }

    @Override
    public String[] getLinkedContactIds(Contact c) {
        if (!nativeInstance.checkContactsUsage()) {
            throw new RuntimeException("Please add the ios.NSContactsUsageDescription build hint");
        }
        int recId = Integer.parseInt(c.getId());
        int num = nativeInstance.countLinkedContacts(recId);
        String[] out = new String[num];
        if (num > 0) {
            int[] iout = new int[num];
            nativeInstance.getLinkedContactIds(num, recId, iout);
            for (int i=0; i<num; i++) {
                out[i] = String.valueOf(iout[i]);
            }
        }
        return out;
        
    }
    
    
    
    @Override
    public Contact getContactById(String id, boolean includesFullName, boolean includesPicture, boolean includesNumbers, boolean includesEmail, boolean includeAddress) {
        if (!nativeInstance.checkContactsUsage()) {
            throw new RuntimeException("Please add the ios.NSContactsUsageDescription build hint");
        }
        int recId = Integer.parseInt(id);
        Contact c = new Contact();
        c.setId(id);
        c.setAddresses(new Hashtable());
        if (includeAddress) {
            // This is a hack to make sure that 
            // Address and its methods aren't stripped out by the BytecodeCompiler
            if (System.currentTimeMillis() == 0) {
                Address tmp = new Address();
                tmp.setCountry("");
                tmp.setLocality("");
                tmp.setRegion("");
                tmp.setPostalCode("");
                tmp.setStreetAddress("");
                c.getAddresses().put("", tmp);
            }
        }

        c.setEmails(new Hashtable());
        c.setPhoneNumbers(new Hashtable());
        nativeInstance.updatePersonWithRecordID(recId, c, includesFullName, includesPicture, includesNumbers, includesEmail, includeAddress);
        return c;
    }

    @Override
    public Contact getContactById(String id) {
        if (!nativeInstance.checkContactsUsage()) {
            throw new RuntimeException("Please add the ios.NSContactsUsageDescription build hint");
        }
        return getContactById(id, true, true, true, true, true);
    }
    
    @Override
    public void dial(String phoneNumber) {        
        nativeInstance.dial("tel://" + phoneNumber);
    }

    @Override
    public boolean isCallDetectionSupported() {
        return true;
    }

    @Override
    public boolean isInCall() {
        return callInterruptionActive;
    }

    @Override
    public boolean canDial() {
        boolean s = super.canDial(); 
        return s && nativeInstance.canExecute("tel://911");
    }
    
    @Override
    public int getSMSSupport() {
        return Display.SMS_INTERACTIVE;
    }
    
    @Override
    public void sendSMS(String phoneNumber, String message, boolean i) throws IOException{
        nativeInstance.sendSMS(phoneNumber, message);
    }

    public void systemOut(String content) {
        nativeInstance.log(content);
    }

    @Override
    public boolean isTrueTypeSupported() {
        return true;
    }

    @Override
    public boolean isNativeFontSchemeSupported() {
        return true;
    }
    
    

    private String nativeFontName(String fontName) {
        if(fontName != null && fontName.startsWith("native:")) {
            if("native:MainThin".equals(fontName)) {
                return "HelveticaNeue-UltraLight";
            }
            if("native:MainLight".equals(fontName)) {
                return "HelveticaNeue-Light";
            }
            if("native:MainRegular".equals(fontName)) {
                return "HelveticaNeue-Medium";
            }
            
            if("native:MainBold".equals(fontName)) {
                return "HelveticaNeue-Bold";
            }
            
            if("native:MainBlack".equals(fontName)) {
                return "HelveticaNeue-CondensedBlack";
            }
            
            if("native:ItalicThin".equals(fontName)) {
                return "HelveticaNeue-UltraLightItalic";
            }
            
            if("native:ItalicLight".equals(fontName)) {
                return "HelveticaNeue-LightItalic";
            }
            
            if("native:ItalicRegular".equals(fontName)) {
                return "HelveticaNeue-MediumItalic";
            }
            
            if("native:ItalicBold".equals(fontName) || "native:ItalicBlack".equals(fontName)) {
                return "HelveticaNeue-BoldItalic";
            }
        }            
        return fontName;
    }

    @Override
    public Object loadTrueTypeFont(String fontName, String fileName) {
        NativeFont fnt = new NativeFont();
        fnt.face = com.codename1.ui.Font.FACE_SYSTEM;
        fnt.size = com.codename1.ui.Font.SIZE_MEDIUM;
        fnt.style = com.codename1.ui.Font.STYLE_PLAIN;
        fontName = nativeFontName(fontName);
        fnt.name = fontName;
        fnt.peer = nativeInstance.createTruetypeFont(fontName);
        return fnt;
    }

    @Override
    public Object deriveTrueTypeFont(Object font, float size, int weight) {
        NativeFont original = (NativeFont)font;
        NativeFont fnt = new NativeFont();
        fnt.face = com.codename1.ui.Font.FACE_SYSTEM;
        fnt.size = com.codename1.ui.Font.SIZE_MEDIUM;
        fnt.style = com.codename1.ui.Font.STYLE_PLAIN;
        fnt.name = original.name;
        fnt.weight = weight;
        fnt.height = size;
        fnt.peer = nativeInstance.deriveTruetypeFont(original.peer, 
                (weight & com.codename1.ui.Font.STYLE_BOLD) == com.codename1.ui.Font.STYLE_BOLD, 
                (weight & com.codename1.ui.Font.STYLE_ITALIC) == com.codename1.ui.Font.STYLE_ITALIC, size);
        return fnt;
    }
    
    @Override
    public void lockOrientation(boolean portrait) {
        nativeInstance.lockOrientation(portrait);
    }

    @Override
    public void unlockOrientation() {
        nativeInstance.unlockOrientation();
    }

    @Override
    public boolean minimizeApplication() {
        return nativeInstance.minimizeApplication();
    }

    /*@Override
    public void pauseAudio(Object handle) {
        long[] l = (long[])handle;
        if(l[0] == 0) {
            return;
        }
        nativeInstance.pauseAudio(l[0]);
    }*/

    @Override
    public void restoreMinimizedApplication() {
        nativeInstance.restoreMinimizedApplication();
    }

    @Override
    public void setAntiAliased(Object graphics, boolean a) {
        NativeGraphics ng = (NativeGraphics)graphics;
        ng.checkControl();
        ng.setAntiAliased(a);
    }

    @Override
    public void setAntiAliasedText(Object graphics, boolean a) {
        NativeGraphics ng = (NativeGraphics)graphics;
        ng.checkControl();
        ng.setAntiAliasedText(a);
    }

    /*@Override
    public void setAudioTime(Object handle, int time) {
        long[] l = (long[])handle;
        if(l[0] == 0) {
            return;
        }
        nativeInstance.setAudioTime(l[0], time);
    }*/

    private long get(PeerComponent p) {
        if(p == null) return 0;
        long[] l = (long[])p.getNativePeer();
        return l[0];
    }
    
    @Override
    public void setBrowserPage(PeerComponent browserPeer, String html, String baseUrl) {
        if(baseUrl != null && baseUrl.startsWith("jar://")) {
            String str = StringUtil.replaceAll(nativeInstance.getResourcesDir(), " ", "%20");
            baseUrl = "file://localhost" + str + baseUrl.substring(6);
        }
        nativeInstance.setBrowserPage(get(browserPeer), html, baseUrl);
    }

    @Override
    public void setBrowserProperty(PeerComponent browserPeer, String key, Object value) {
        if(key.equalsIgnoreCase("useragent")) {
            nativeInstance.setBrowserUserAgent(datePickerResult, (String)value);
            return;
        }
        if (BrowserComponent.BROWSER_PROPERTY_FOLLOW_TARGET_BLANK.equals(key)) {
            nativeInstance.setBrowserFollowTargetBlank(get(browserPeer), Boolean.TRUE.equals(value));
        }
        if (BrowserComponent.BROWSER_PROPERTY_INTERFACE_STYLE.equals(key)) {
            // Maps to UIUserInterfaceStyle: 0 = unspecified/auto, 1 = light, 2 = dark.
            int style = 0;
            if (value != null) {
                String v = value.toString();
                if ("light".equalsIgnoreCase(v)) {
                    style = 1;
                } else if ("dark".equalsIgnoreCase(v)) {
                    style = 2;
                }
            }
            nativeInstance.setBrowserInterfaceStyle(get(browserPeer), style);
        }
    }

    /**
     * https://github.com/codenameone/CodenameOne/issues/2551
     * 
     * @param path The path to fix.  This should not include the file:// prefix
     * @return The fixed path.  Does not include file:// prefix
     */
    private String fixAppRoot(String path) {
        String base = "/var/mobile/Containers/Data/Application/";
        String containerRoot = getContainerRoot();
        if (path.startsWith(base) && !path.startsWith(containerRoot)) {
            String theRest = path.substring(base.length(), path.length());
            int slashPos = theRest.indexOf("/");
            if (slashPos <= 0) {
                return path;
            }
            
            return containerRoot + theRest.substring(slashPos+1, theRest.length());
        }
        return path;
    }
    
    // Gets the container root -- does not include file:// prefix
    private String getContainerRoot() {
        String appRoot = nativeInstance.getDocumentsDir();
        if (appRoot.endsWith("/")) {
            appRoot = appRoot.substring(0, appRoot.length()-1);
        }
        return appRoot.substring(0, appRoot.lastIndexOf("/")+1);
        
    }
    
    @Override
    public void setBrowserURL(PeerComponent browserPeer, String url) {
        url = unfile(url);
        if(url.startsWith("jar://")) {
            String str = StringUtil.replaceAll(nativeInstance.getResourcesDir(), " ", "%20");
            url = "file://localhost" + str + url.substring(6);
        }
        nativeInstance.setBrowserURL(get(browserPeer), url);
    }

    @Override
    public boolean isURLWithCustomHeadersSupported() {
        return true;
    }        
    
    @Override
    public void setBrowserURL(PeerComponent browserPeer, String url, Map<String, String> headers) {
        url = unfile(url);
        if(url.startsWith("jar://")) {
            String str = StringUtil.replaceAll(nativeInstance.getResourcesDir(), " ", "%20");
            url = "file://localhost" + str + url.substring(6);
        } 
        
        String[] keys = new String[headers.size()];
        headers.keySet().toArray(keys);
        String[] values = new String[keys.length];
        for(int iter = 0 ; iter < keys.length ; iter++) {
            values[iter] = headers.get(keys[iter]);
        }
        
        nativeInstance.setBrowserURL(get(browserPeer), url, keys, values);
    }

    @Override
    public void setBuiltinSoundsEnabled(boolean enabled) {
        // TODO
        super.setBuiltinSoundsEnabled(enabled);
    }

    /*@Override
    public void setVolume(int vol) {
        nativeInstance.setVolume(((float)vol) / 100.0f);
    }*/

    @Override
    public void showNativeScreen(Object nativeFullScreenPeer) {
        // TODO
        super.showNativeScreen(nativeFullScreenPeer);
    }

    @Override
    public void vibrate(int duration) {
        nativeInstance.vibrate(duration);
    }
    
    @Override
    public PeerComponent createNativePeer(Object nativeComponent) {
        return new NativeIPhoneView(nativeComponent);
    }

    // Live Metal 3D surfaces keyed by their hosting peer, mirroring the
    // IdentityHashMap pattern the JavaSE port uses for its GL surfaces.
    private final java.util.Map<PeerComponent, IOSGLSurface> glSurfaces =
            new java.util.IdentityHashMap<PeerComponent, IOSGLSurface>();

    // The portable 3D API is implemented on the Metal pipeline only, so the
    // backend is exposed (getGpuImplementation returns non-null) only while
    // Metal rendering is active.
    private final com.codename1.impl.gpu.GpuImplementation gpuImpl =
            new com.codename1.impl.gpu.GpuImplementation() {
        @Override
        public PeerComponent createPeer(com.codename1.gpu.RenderView view) {
            long contextPeer = nativeInstance.gl3dCreateContext();
            if (contextPeer == 0) {
                return null;
            }
            long viewPeer = nativeInstance.gl3dGetViewPeer(contextPeer);
            if (viewPeer == 0) {
                nativeInstance.gl3dDestroyContext(contextPeer);
                return null;
            }
            IOSGLSurface surface = new IOSGLSurface(view, contextPeer);
            PeerComponent peer = createNativePeer(new long[] { viewPeer });
            if (peer != null) {
                glSurfaces.put(peer, surface);
            }
            return peer;
        }

        @Override
        public void setContinuous(PeerComponent peer, boolean continuous) {
            IOSGLSurface surface = glSurfaces.get(peer);
            if (surface != null) {
                surface.setContinuous(continuous);
            }
        }

        @Override
        public void requestRender(PeerComponent peer) {
            IOSGLSurface surface = glSurfaces.get(peer);
            if (surface != null) {
                surface.requestRender();
            }
        }
    };

    @Override
    public com.codename1.impl.gpu.GpuImplementation getGpuImplementation() {
        return metalRendering ? gpuImpl : null;
    }

    class NativeIPhoneView extends PeerComponent {

        private long nativePeer;

        private boolean lightweightMode; 
       
        public NativeIPhoneView(Object nativePeer) {
            super(nativePeer);
            this.nativePeer = ((long[])nativePeer)[0];
            nativeInstance.retainPeer(this.nativePeer);
        }
        
        public void finalize() {
            if(nativePeer != 0) {
                nativeInstance.releasePeer(nativePeer);
                nativePeer = 0;
            }
        }
        
        public boolean isFocusable() {
            return true;
        }

        public void setFocus(boolean b) {
        }
        
        protected Dimension calcPreferredSize() {
            if(nativePeer == 0) {
                return new Dimension();
            }
            int[] p = widthHeight;
            nativeInstance.calcPreferredSize(nativePeer, getDisplayWidth(), getDisplayHeight(), p);
            return new Dimension(p[0], p[1]);
        }

        protected void onPositionSizeChange() {
            if(nativePeer != 0) {
                nativeInstance.updatePeerPositionSize(nativePeer, getAbsoluteX(), getAbsoluteY(), getWidth(), getHeight());
            }
        }

        protected void initComponent() {
            super.initComponent();
            if(nativePeer != 0) {
                nativeInstance.peerInitialized(nativePeer, getAbsoluteX(), getAbsoluteY(), getWidth(), getHeight());
            }
        }

        protected void deinitialize() {
            if(nativePeer != 0) {
                setPeerImage(generatePeerImage());
                nativeInstance.peerDeinitialized(nativePeer);
            }
            super.deinitialize();
        }
        
        protected void setLightweightMode(boolean l) {
            if(nativePeer != 0) {
                if(lightweightMode != l) {
                    lightweightMode = l;
                    nativeInstance.peerSetVisible(nativePeer, !lightweightMode);
                    // fix for https://groups.google.com/d/msg/codenameone-discussions/LKxy16PhYEY/bvusdq-ICwAJ
                    Form f = getComponentForm();
                    if(f != null) {
                        f.repaint();
                    }
                }
            }
        }
        
        protected Image generatePeerImage() {
            int[] wh = widthHeight;
            long imagePeer = nativeInstance.createPeerImage(this.nativePeer, wh);
            if(imagePeer == 0) {
                return null;
            }
            NativeImage ni = new NativeImage("PeerScreen");
            ni.peer = imagePeer;
            ni.width = wh[0];
            ni.height = wh[1];
            return Image.createImage(ni);
        }
        
        protected boolean shouldRenderPeerImage() {
            return lightweightMode || !isInitialized();
        }

    }

    public boolean areMutableImagesFast() {
        return false;
    }

    protected boolean cacheRadialGradients() {
        return false;
    }

    protected boolean cacheLinearGradients() {
        return false;
    }

    public void fillRectRadialGradient(Object graphics, int startColor, int endColor, int x, int y, int width, int height, float relativeX, float relativeY, float relativeSize) {
        NativeGraphics ng = (NativeGraphics)graphics;
        ng.checkControl();
        ng.applyTransform();
        ng.applyClip();
        ng.fillRectRadialGradient(startColor, endColor, x, y, width, height, relativeX, relativeY, relativeSize);
    }

    public void fillLinearGradient(Object graphics, int startColor, int endColor, int x, int y, int width, int height, boolean horizontal) {
        NativeGraphics ng = (NativeGraphics)graphics;
        ng.checkControl();
        ng.applyTransform();
        ng.applyClip();
        ng.fillLinearGradient(startColor, endColor, x, y, width, height, horizontal);
    }

    // Metal builds route the multi-stop CSS Gradient API through a pure-GPU
    // shader (CN1MetalPipelineMultiStopGradient). GL builds (or Metal builds
    // that can't pack the gradient into the shader's 8-stop budget) fall back
    // to the base CodenameOneImplementation software rasterizer, which builds
    // an ARGB raster via Gradient.sampleArgb() and uploads it through
    // drawImage. The Java side caches that raster on the Gradient via a
    // WeakReference so repaint storms don't re-rasterise. gaussianBlurImage
    // wraps either the Metal-native two-pass blur or CIGaussianBlur for the
    // filter:blur effect on Image inputs.
    @Override
    public void fillGradient(Object graphics, com.codename1.ui.Gradient gradient, int x, int y, int width, int height) {
        if (gradient == null || width <= 0 || height <= 0) {
            return;
        }
        if (metalRendering && gradient.getColors().length <= 8) {
            NativeGraphics ng = (NativeGraphics) graphics;
            ng.checkControl();
            ng.applyTransform();
            ng.applyClip();
            int kind = gradient.getKind();
            int[] argb = gradient.getColors();
            float[] pos = gradient.getPositions();
            int stopCount = argb.length;
            float[] colors = new float[stopCount * 4];
            for (int i = 0; i < stopCount; i++) {
                int c = argb[i];
                int a8 = (c >>> 24) & 0xff;
                if (a8 == 0) {
                    a8 = 0xff;
                }
                float a = a8 / 255f;
                colors[i * 4] = ((c >> 16) & 0xff) / 255f * a;
                colors[i * 4 + 1] = ((c >> 8) & 0xff) / 255f * a;
                colors[i * 4 + 2] = (c & 0xff) / 255f * a;
                colors[i * 4 + 3] = a;
            }
            float angleOrFromAngle = 0f;
            float cx = 0.5f;
            float cy = 0.5f;
            float rx = 0.5f;
            float ry = 0.5f;
            int shape = 1;
            if (kind == com.codename1.ui.Gradient.KIND_LINEAR) {
                angleOrFromAngle = ((com.codename1.ui.LinearGradient) gradient).getAngleDegrees();
            } else if (kind == com.codename1.ui.Gradient.KIND_RADIAL) {
                com.codename1.ui.RadialGradient rg = (com.codename1.ui.RadialGradient) gradient;
                float[] geom = new float[4];
                rg.computeRadii(width, height, geom);
                cx = geom[0] / width;
                cy = geom[1] / height;
                rx = geom[2] / width;
                ry = geom[3] / height;
                shape = rg.getShape();
            } else if (kind == com.codename1.ui.Gradient.KIND_CONIC) {
                com.codename1.ui.ConicGradient cg = (com.codename1.ui.ConicGradient) gradient;
                angleOrFromAngle = cg.getFromAngleDegrees();
                cx = cg.getRelativeCenterX();
                cy = cg.getRelativeCenterY();
            }
            boolean mutable = !(ng instanceof GlobalGraphics);
            nativeInstance.fillGradient(kind, stopCount, pos, colors,
                    gradient.getCycleMethod(), angleOrFromAngle,
                    cx, cy, rx, ry, shape,
                    x, y, width, height, mutable);
            return;
        }
        super.fillGradient(graphics, gradient, x, y, width, height);
    }


    public static void appendData(long peer, long data) {
        NetworkConnection n = null;
        synchronized(CONNECTIONS_LOCK) {
            int len = instance.connections.size();
            for (int i=0; i<len; i++) {
                if (instance.connections.get(i).peer == peer) {
                    n = instance.connections.get(i);
                }
            }
        }
        if(n != null) {
            synchronized(n.LOCK) {
                nativeInstance.appendData(peer, data);
                n.connected = true;
                n.LOCK.notifyAll();
            }
        }
    }
    
    public static void streamComplete(long peer) {
        NetworkConnection n = null;
        synchronized(CONNECTIONS_LOCK) {
            int len = instance.connections.size();
            for (int i=0; i<len; i++) {
                if (instance.connections.get(i).peer == peer) {
                    n = instance.connections.get(i);
                }
            }
        }
        if(n != null) {
            synchronized(n.LOCK) {
                n.connected = true;
                n.streamComplete();
                n.LOCK.notifyAll();
            }
        }
    }
    
    public static void networkError(long peer, String error) {
        NetworkConnection n = null;
        synchronized(CONNECTIONS_LOCK) {
            int len = instance.connections.size();
            for (int i=0; i<len; i++) {
                if (instance.connections.get(i).peer == peer) {
                    n = instance.connections.get(i);
                }
            }
        }
        synchronized(n.LOCK) {
            if(error == null) {
                n.error = "Unknown server error";
            } else {
                n.error = error;
            }
            n.connected = true;
            n.LOCK.notifyAll();
        }
    }

    /**
     * An output stream that will start writing to a file once it reaches 
     * a certain size.  
     */
    static class FileBackedOutputStream extends OutputStream {

        private ByteArrayOutputStream buf;
        private NSDataOutputStream fos;
        private static int maxBufferSize=102400;
        boolean usingBuffer = true;
        private String file;
        
        public FileBackedOutputStream() {
            buf = new ByteArrayOutputStream();
        }
        
        @Override
        public void write(int b) throws IOException {
            if (usingBuffer && buf.size() < maxBufferSize) {
                buf.write(b);
            } else if (usingBuffer) {
                usingBuffer = false;
                file = createTempFile();
                fos = new NSDataOutputStream(file);
                fos.write(buf.toByteArray());
                fos.write(b);
                buf = null;
            } else {
                fos.write(b);
            }
        }

        @Override
        public void write(byte[] b) throws IOException {
            write(b, 0, b.length);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (usingBuffer && buf.size() + len < maxBufferSize) {
                buf.write(b, off, len);
            } else if (usingBuffer) {
                usingBuffer = false;
                file = createTempFile();
                fos = new NSDataOutputStream(file);
                fos.write(buf.toByteArray());
                fos.write(b, off, len);
                buf = null;
            } else {
                fos.write(b, off, len);
            }
        }

        @Override
        public void close() throws IOException {
            if (buf != null) {
                buf.close();
            }
            if (fos != null) {
                fos.flush();
                fos.close();
            }
        }

        @Override
        public void flush() throws IOException {
            if (buf != null) {
                buf.flush();
            }
            if (fos != null) {
                fos.flush();
            }
        }
        
        
        
        public String getFilePath() {
            if (fos != null) {
                return file;
            } else {
                return null;
            }
        }
        
        public boolean isBackedByFile() {
            return !usingBuffer;
        }
        
        
        public byte[] toByteArray() throws IOException {
            if (isBackedByFile()) {
                NSFileInputStream fis = null;
                
                fis = new NSFileInputStream(getFilePath());
                byte[] out = Util.readInputStream(fis);
                
                Util.cleanup(fis);
                return out;
                
                
                
            } else {
                return buf.toByteArray();
            }
        }
        
        public InputStream getInputStream() throws IOException {
            if (isBackedByFile()) {
                return new NSFileInputStream(getFilePath());
            } else {
                return new ByteArrayInputStream(toByteArray());
            }
        }
        
        
        
        private String createTempFile() {
            String p = FileSystemStorage.getInstance().getAppHomePath();
            if (p.lastIndexOf("/") != p.length()-1) {
                p += "/";
            }
            long t = System.currentTimeMillis();
            while (FileSystemStorage.getInstance().exists(p + "networkTmp_"+t)) {
                t++;
            }
            return p + "networkTmp_"+t;
        }
        
    }
    
    
    static class NetworkConnection extends InputStream {
        private int id;
        private long peer;
        private boolean closed;
        private FileBackedOutputStream body;
        //private Vector pendingData = new Vector();
        private boolean completed;
        private Hashtable headers = new Hashtable();
        private String[] sslCertificates;
        private boolean connected;
        private boolean ensureConnectionLock;
        private boolean insecure;
        String error;
        public final Object LOCK = new Object();
        
        public void setId(int id) {
            this.id = id;
            nativeInstance.setConnectionId(peer, id);
        }
        
        public void setInsecure(boolean insecure) {
            this.insecure = insecure;
            if (insecure) {
                nativeInstance.setInsecure(peer, insecure);
            }
        }
        
        public void setChunkedStreamingMode(int len) {
            nativeInstance.setChunkedStreamingMode(peer, len);
        }
        
        public void ensureConnection() throws IOException {
            synchronized(LOCK) {
                if(connected) {
                    return;
                }
                if(ensureConnectionLock) {
                    while(ensureConnectionLock) {
                        try {
                            LOCK.wait();
                        } catch (InterruptedException ex) {
                        }
                    }
                    return;
                }
                ensureConnectionLock = true;
                if(body != null) {
                    try {
                        body.flush();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    if (body.isBackedByFile()) {
                        nativeInstance.setBody(peer, body.getFilePath());
                    } else {
                        nativeInstance.setBody(peer, body.toByteArray());
                        body = null;
                    }
                    
                }
                nativeInstance.connect(peer);
                while(!connected) {
                    try {
                        LOCK.wait();
                    } catch (InterruptedException ex) {
                    }
                }
                if(error != null) {
                    Log.p(error);
                    throw new IOException(error);
                }
            }
        }
        
        public NetworkConnection(long peer) {
            this.peer = peer;
            synchronized(CONNECTIONS_LOCK) {
                instance.connections.add(this);
            }
        }
        
        public void addHeader(String key, String value) {
            headers.put(key, value);
        }
        
        public void streamComplete() {
            synchronized(LOCK) {
                completed = true;
                LOCK.notify();
            }
        }
        
        /*
        public void appendData(byte[] data) {
            boolean w = false;
            synchronized(LOCK) {
                pendingData.addElement(data);
                LOCK.notify();
                try {
                    if(pendingData.size() > 20) {
                        w = true;
                        LOCK.wait(1000);
                    }
                } catch(InterruptedException ie) {
                }
            }
            if(w) {
                System.gc();
            }
        }
        */
        
        private int shiftByte() {
            return nativeInstance.shiftByte(peer);
        }
        
        @Override
        public int read() throws IOException {
            synchronized(LOCK) {
                if(available() == 0) {
                    if(completed) {
                        return -1;
                    }

                    while(available() == 0) {
                        try {
                            LOCK.wait();
                        } catch (InterruptedException ex) {
                        }
                        if(error != null) {
                            throw new IOException(error);
                        }
                        if(completed && available() == 0) {
                            return -1;
                        }
                    }
                }

                //byte[] chunk = (byte[])pendingData.elementAt(0);
                int val = shiftByte() & 0xff;
                //if(chunk.length == 1) {
                //    pendingData.removeElementAt(0);
                //} else {
                //    byte[] b = new byte[chunk.length - 1];
                //    System.arraycopy(chunk, 1, b, 0, b.length);
                //    pendingData.setElementAt(b, 0);
                //}
                if(error != null) {
                    throw new IOException(error);
                }
                return val;
            }
        }

        @Override
        public int available() throws IOException {
            if(error != null) {
                throw new IOException(error);
            }
            return nativeInstance.available(peer);
            /*
            synchronized(LOCK) {
                int count = 0;
                for(int iter = 0 ; iter < pendingData.size() ; iter++) {
                    byte[] b = (byte[])pendingData.elementAt(iter);
                    count += b.length;
                }
                return count;
            }
            */
        }

        @Override
        public void close() throws IOException {
            synchronized(LOCK) {
                //if(pendingData == null) {
                //    return;
                //}
                if (closed) {
                    return;
                }
                closed = true;
                completed = true;
                //pendingData = null;
                super.close();
                nativeInstance.closeConnection(peer);
                peer = 0;
            }
            synchronized(CONNECTIONS_LOCK) {
                instance.connections.remove(this);
                if (body != null && body.isBackedByFile() && FileSystemStorage.getInstance().exists(body.getFilePath())) {
                    FileSystemStorage.getInstance().delete(body.getFilePath());
                }
            }
        }

        @Override
        public int read(byte[] bytes) throws IOException {
            return read(bytes, 0, bytes.length);
        }

        @Override
        public int read(byte[] bytes, int off, int len) throws IOException {
            synchronized(LOCK) {
                if(available() == 0) {
                    if(completed) {
                        return -1;
                    }

                    while(available() == 0) {
                        try {
                            LOCK.wait();
                        } catch (InterruptedException ex) {
                        }
                        if(completed && available() == 0) {
                            return -1;
                        }
                    }
                }
                len = nativeInstance.readData(peer, bytes, off, len);
                //byte[] chunk = (byte[])pendingData.elementAt(0);
                //if(chunk.length < len) {
                //    len = chunk.length;
                //}
                //for(int iter = 0 ; iter < len ; iter++) {
                //    bytes[iter + off] = chunk[iter];
                //}

                //if(chunk.length == len) {
                //    pendingData.removeElementAt(0);
                //} else {
                //    byte[] b = new byte[chunk.length - len];
                //    System.arraycopy(chunk, len, b, 0, b.length);
                //    pendingData.setElementAt(b, 0);
                //}
                if(error != null) {
                    throw new IOException(error);
                }
                return len;            
            }
        }

        private String[] getSSLCertificates(String url) {
            if (sslCertificates == null) {
                try {
                    com.codename1.io.URL uUrl = new com.codename1.io.URL(url);
                    String key = uUrl.getHost()+":"+uUrl.getPort();
                    String certs = nativeInstance.getSSLCertificates(peer);
                    if (certs == null) {
                        //if (sslCertificatesCache.containsKey(key)) {
                        //    sslCertificates = sslCertificatesCache.get(key);
                        //}
                        if (sslCertificates == null) {
                            return new String[0];
                        }
                        return sslCertificates;
                    }
                    sslCertificates = Util.split(certs, ",");
                    //sslCertificatesCache.put(key, sslCertificates);
                    return sslCertificates;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return new String[0];
                }
            }
            return sslCertificates;
        }
        
    }

    //private static Map<String, String[]> sslCertificatesCache = new HashMap<String,String[]>();
    
    public boolean isTimeoutSupported() {
        return true;
    }

    public void setTimeout(int t) {
        timeout = t;
    }

    public Object connect(String url, boolean read, boolean write, int timeout) throws IOException {
        return new NetworkConnection(nativeInstance.openConnection(url, timeout));
    }
    
    /**
     * @inheritDoc
     */
    public Object connect(String url, boolean read, boolean write) throws IOException {
        return new NetworkConnection(nativeInstance.openConnection(url, timeout));
    }

    @Override
    public String[] getSSLCertificates(Object connection, String url) throws IOException {
        NetworkConnection conn =  (NetworkConnection)connection;
        //conn.ensureConnection();
        return conn.getSSLCertificates(url);
    }

    @Override
    public boolean canGetSSLCertificates() {
        return true;
    }

    /**
     * Checking SSL certificates uses a native callback, instead of the direct approach
     * which is used in other ports.
     * @return 
     */
    @Override
    public boolean checkSSLCertificatesRequiresCallbackFromNative() {
        return true;
    }
    
    

    /**
     * @inheritDoc
     */
    @Override
    public void setChunkedStreamingMode(Object connection, int bufferLen) {
        ((NetworkConnection)connection).setChunkedStreamingMode(bufferLen);
    }
    
    /**
     * @inheritDoc
     */
    public void setHeader(Object connection, String key, String val) {
        nativeInstance.addHeader(((NetworkConnection)connection).peer, key, val);
    }

    /**
     * @inheritDoc
     */
    public OutputStream openOutputStream(Object connection) throws IOException {
        if(connection instanceof String) {
            BufferedOutputStream o = new BufferedOutputStream(new NSDataOutputStream((String)connection), (String)connection);
            return o;
        }
        NetworkConnection n = (NetworkConnection)connection;
        n.body = new FileBackedOutputStream();
        return new BufferedOutputStream(n.body);
    }

    /**
     * @inheritDoc
     */
    public OutputStream openOutputStream(Object connection, int offset) throws IOException {
        BufferedOutputStream o = new BufferedOutputStream(new NSDataOutputStream((String)connection, offset), (String)connection);
        return o;
    }

    /**
     * @inheritDoc
     */
    public InputStream openInputStream(Object connection) throws IOException {
        if(connection instanceof String) {
            // Match openFileInputStream(String): if the path is missing, throw
            // a FileNotFoundException instead of silently opening an empty
            // NSFileInputStream (which Apple's fileHandleForReadingAtPath:
            // returns when the file does not exist). See #1502.
            String path = (String) connection;
            if(!nativeInstance.fileExists(path)) {
                throw new FileNotFoundException("File not found: " + path);
            }
            BufferedInputStream o = new BufferedInputStream(new NSFileInputStream(path), path);
            return o;
        }
        NetworkConnection n = (NetworkConnection)connection;
        n.ensureConnection();
        return new BufferedInputStream(n);
    }


    /**
     * @inheritDoc
     */
    public void setHttpMethod(Object connection, String method) throws IOException {
        NetworkConnection n = (NetworkConnection)connection;
        nativeInstance.setMethod(n.peer, method);
    }

    /**
     * @inheritDoc
     */
    public void setPostRequest(Object connection, boolean p) {
        NetworkConnection n = (NetworkConnection)connection;
        if(p) {
            nativeInstance.setMethod(n.peer, "POST");
        } else {
            nativeInstance.setMethod(n.peer, "GET");
        }
    }

    @Override
    public void setConnectionId(Object connection, int id) {
        NetworkConnection n = (NetworkConnection)connection;
        n.setId(id);
    }

    @Override
    public void setInsecure(Object connection, boolean insecure) {
        NetworkConnection n = (NetworkConnection)connection;
        n.setInsecure(insecure);
    }
    
    
    
    

    /**
     * @inheritDoc
     */
    public int getResponseCode(Object connection) throws IOException {
        NetworkConnection n = (NetworkConnection)connection;
        n.ensureConnection();
        return nativeInstance.getResponseCode(n.peer);
    }

    /**
     * @inheritDoc
     */
    public String getResponseMessage(Object connection) throws IOException {
        NetworkConnection n = (NetworkConnection)connection;
        n.ensureConnection();
        return nativeInstance.getResponseMessage(n.peer);
    }

    /**
     * @inheritDoc
     */
    public int getContentLength(Object connection) {
        NetworkConnection n = (NetworkConnection)connection;
        try {
            n.ensureConnection();
            return nativeInstance.getContentLength(n.peer);
        } catch(IOException err) {
            return -1;
        }
    }

    /**
     * @inheritDoc
     */
    public String getHeaderField(String name, Object connection) throws IOException {
        NetworkConnection n = (NetworkConnection)connection;
        n.ensureConnection();
        return nativeInstance.getResponseHeader(n.peer, name);
    }

    /**
     * @inheritDoc
     */
    public String[] getHeaderFieldNames(Object connection) throws IOException {
        NetworkConnection n = (NetworkConnection)connection;
        n.ensureConnection();
        String[] s = new String[nativeInstance.getResponseHeaderCount(n.peer)];
        int slen = s.length;
        for(int iter = 0 ; iter < slen ; iter++) {
            s[iter] = nativeInstance.getResponseHeaderName(n.peer, iter);
        }
        return s;
    }

    /**
     * @inheritDoc
     */
    public String[] getHeaderFields(String name, Object connection) throws IOException {
        NetworkConnection n = (NetworkConnection)connection;
        n.ensureConnection();
        String s = nativeInstance.getResponseHeader(n.peer, name);
        if(s == null) {
            return null;
        }
        
        // iOS has a bug where it concates identical headers using a comma
        // but since cookies use a comma in their expires header we need to 
        // join them back together...
        List<String> stringList = StringUtil.tokenize(s, ",");
        if(stringList.size() > 1) {
            List<String> result = new ArrayList<String>();
            String loaded = null;
            for(String current : stringList) {
                if(loaded != null) {
                    result.add(loaded + current);
                    loaded = null;
                } else {
                    int p = current.toLowerCase().indexOf("expires=");
                    int c = current.lastIndexOf(";");
                    if(c < p && p > 0) {
                        loaded = current;
                    } else {
                        result.add(current);
                    }
                }
            }
            String[] resultArr = new String[result.size()];
            result.toArray(resultArr);
            return resultArr;
        }
        String[] resultArr = new String[stringList.size()];
        stringList.toArray(resultArr);
        return resultArr;
    }

    private String storageDirectory;
    public String getStorageDirectory() {
        if(storageDirectory == null) {
            storageDirectory = nativeInstance.getDocumentsDir();
            if(!storageDirectory.endsWith("/")) {
                storageDirectory = storageDirectory + "/";
            }
            storageDirectory += "cn1storage/";
            if(!Display.getInstance().getProperty("iosNewStorage", "false").equals("true")) {
                if(!exists(storageDirectory)) {
                    // migrate existing storage
                    mkdir(storageDirectory);

                    String cachesDir = nativeInstance.getCachesDir();
                    String[] a = new String[nativeInstance.fileCountInDir(cachesDir)];
                    nativeInstance.listFilesInDir(cachesDir, a);
                    if(!cachesDir.endsWith("/")) {
                        cachesDir = cachesDir + "/";
                    }
                    for(String current : a) {
                        if(!isDirectory(cachesDir + current)) {
                            InputStream i = null;
                            try {
                                i = FileSystemStorage.getInstance().openInputStream(cachesDir + current);
                                OutputStream o = FileSystemStorage.getInstance().openOutputStream(storageDirectory + current);
                                Util.copy(i, o);
                                FileSystemStorage.getInstance().delete(cachesDir + current);
                            } catch (IOException ex) {
                                throw new RuntimeException(ex.getMessage());
                            } finally {
                                if (i != null) {
                                    try {
                                        i.close();
                                    } catch (IOException ex) {
                                        //throw new RuntimeException(ex.getMessage());
                                    }
                                }
                            }
                        }
                    }
                } 
            } else {
                mkdir(storageDirectory);
            }
        }
        return storageDirectory;
    }
    
    /**
     * @inheritDoc
     */
    public void deleteStorageFile(String name) {
        nativeInstance.deleteFile(getStorageDirectory() + "/" + name);
    }

    /**
     * @inheritDoc
     */
    public OutputStream createStorageOutputStream(String name) throws IOException {
        name = getStorageDirectory() + "/" + name;
        return new BufferedOutputStream(new NSDataOutputStream(name) , name);
    }

    /**
     * @inheritDoc
     */
    public InputStream createStorageInputStream(String name) throws IOException {
        name = getStorageDirectory() + "/" + name;
        return new BufferedInputStream(new NSFileInputStream(name), name);
    }

    /**
     * @inheritDoc
     */
    public boolean storageFileExists(String name) {
        return nativeInstance.fileExists(getStorageDirectory() + "/" + name);
    }

    /**
     * @inheritDoc
     */
    public String[] listStorageEntries() {
        String c = getStorageDirectory();
        String[] a = new String[nativeInstance.fileCountInDir(c)];
        nativeInstance.listFilesInDir(c, a);
        return a;
    }

    /**
     * @inheritDoc
     */
    public int getStorageEntrySize(String name) {
        return nativeInstance.getFileSize(getStorageDirectory() + "/" + name);
    }

    @Override
    public String toNativePath(String path) {
        return unfile(path);
    }

    
    
    /**
     * @inheritDoc
     */
    public String[] listFilesystemRoots() {
        String[] roots;
        if(Display.getInstance().getProperty("iosNewStorage", "false").equals("true")) {
            roots = new String[] {
                    nativeInstance.getDocumentsDir(),
                    nativeInstance.getCachesDir(),
                    nativeInstance.getResourcesDir()
                };
        } else {
            roots = new String[] {
                    nativeInstance.getCachesDir(),
                    nativeInstance.getDocumentsDir(),
                    nativeInstance.getResourcesDir()
                };
        }
        int rlen = roots.length;
        for(int iter = 0 ; iter < rlen ; iter++) {
            if(roots[iter].startsWith("/")) {
                roots[iter] = "file://" + roots[iter];
            }
            if(!roots[iter].endsWith("/")) {
                roots[iter] = roots[iter] + "/";
            }
        }
        return roots;
    }

    @Override
    public boolean hasCachesDir() {
        return true;
    }

    @Override
    public String getCachesDir() {
        return listFilesystemRoots()[1];
    }

    /**
     * @inheritDoc
     */
    public int getRootType(String root) {
        return FileSystemStorage.ROOT_TYPE_UNKNOWN;
    }
    
    /**
     * @inheritDoc
     */
    public String[] listFiles(String directory) throws IOException {
        directory = unfile(directory);
        String[] a = new String[nativeInstance.fileCountInDir(directory)];
        nativeInstance.listFilesInDir(directory, a);
        return a;
    }

    /**
     * @inheritDoc
     */
    public long getRootSizeBytes(String root) {
        return -1;
    }

    /**
     * @inheritDoc
     */
    public long getRootAvailableSpace(String root) {
        return -1;
    }

    /**
     * @inheritDoc
     */
    public void mkdir(String directory) {
        nativeInstance.createDirectory(unfile(directory));
    }

    /**
     * @inheritDoc
     */
    public void deleteFile(String file) {
        nativeInstance.deleteFile(unfile(file));
    }

    /**
     * @inheritDoc
     */
    public boolean isHidden(String file) {
        return file.startsWith(".");
    }

    /**
     * @inheritDoc
     */
    public void setHidden(String file, boolean h) {
    }

    /**
     * @inheritDoc
     */
    public long getFileLength(String file) {
        return nativeInstance.getFileSize(unfile(file));
    }

    public long getFileLastModified(String file) {
        return nativeInstance.getFileLastModified(unfile(file));
    }

    private String unfile(String file) {
        if (file.startsWith("file:///")) {
            return fixAppRoot(file.substring(7));
        }
        if (file.startsWith("file://")) {
            return fixAppRoot(file.substring(6));
        }
        if (file.startsWith("file:/")) {
            return fixAppRoot(file.substring(5));
        }
        return fixAppRoot(file);
    }
    
    /**
     * @inheritDoc
     */
    public boolean isDirectory(String file) {
        return nativeInstance.isDirectory(unfile(file));
    }

    /**
     * @inheritDoc
     */
    public char getFileSystemSeparator() {
        return '/';
    }

    /**
     * @inheritDoc
     */
    public OutputStream openFileOutputStream(String file) throws IOException {
        file = unfile(file);
        return new BufferedOutputStream(new NSDataOutputStream(file), file);
    }

    /**
     * @inheritDoc
     */
    public InputStream openFileInputStream(String file) throws IOException {
        file = unfile(file);
        if(!nativeInstance.fileExists(file)) {
            // FileNotFoundException is more precise than IOException and
            // matches what FileInputStream throws on JavaSE, so callers can
            // distinguish "missing" from other I/O errors. See #1502.
            throw new FileNotFoundException("File not found: " + file);
        }
        return new BufferedInputStream(new NSFileInputStream(file), file);
    }

    /**
     * @inheritDoc
     */
    public boolean exists(String file) {
        file = unfile(file);
        return nativeInstance.fileExists(file);
    }

    /**
     * @inheritDoc
     */
    public void rename(String file, String newName) {
        file = unfile(file);
        if(newName.indexOf('/') < 0) {
            // good this is a relative filename, prepend file
            if(file.endsWith("/")) {
                file = file.substring(0, file.length() - 1);
            }
            int pos = file.lastIndexOf('/');
            if(pos > -1) {
                newName = file.substring(0, pos) + "/" + newName;
            }
        }
        nativeInstance.moveFile(file, newName);
    }

    /**
     * @inheritDoc
     */
    public boolean shouldWriteUTFAsGetBytes() {
        return true;
    }

    /**
     * @inheritDoc
     */
    public void printStackTraceToStream(Throwable t, Writer o) {
        nativeInstance.printStackTraceToStream(t, o);
        /*try {
            o.write(nativeInstance.stackTraceToString(t));
        } catch(IOException err) {}*/
    }

    /**
     * @inheritDoc
     */
    public String getPlatformName() {
        return "ios";
    }

    @Override
    public String getNativeLogSnapshot() {
        try {
            return nativeInstance.crashProtectionLogSnapshot();
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    public void installNativeCrashHandler() {
        try {
            nativeInstance.crashProtectionInstall();
        } catch (Throwable ignored) {
        }
    }

    @Override
    public String consumePendingNativeCrash() {
        try {
            return nativeInstance.crashProtectionConsumePending();
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    public Simd createSimd() {
        return new IOSSimd();
    }

    /**
     * @inheritDoc
     */
    public String[] getPlatformOverrides() {
        if(isWatch()) {
            return new String[] {"watch", "ios", "applewatch"};
        }
        if(isTV()) {
            return new String[] {"tv", "ios", "appletv"};
        }
        if(isTablet()) {
            return new String[] {"tablet", "ios", "ipad"};
        } else {
            return new String[] {"phone", "ios", "iphone"};
        }
    }

    @Override
    public native void paintComponentBackground(Object nativeGraphics, int x, int y, int width, int height, Style s);
    
    @Override
    public native void fillRect(Object nativeGraphics, int x, int y, int w, int h, byte alpha);
    
    @Override
    public native void drawLabelComponent(Object nativeGraphics, int cmpX, int cmpY, int cmpHeight, int cmpWidth,
            Style style, String text, Object icon, Object stateIcon, int preserveSpaceForState, int gap, boolean rtl,
            boolean isOppositeSide, int textPosition, int stringWidth, boolean isTickerRunning, int tickerShiftText,
            boolean endsWith3Points, int valign);
    
    @Override
    public void registerPush(Hashtable metaData, boolean noFallback) {
        nativeInstance.registerPush();
    }

    @Override
    public void deregisterPush() {
        nativeInstance.deregisterPush();
    }

    @Override
    public void blockCopyPaste(boolean blockCopyPaste) {
        nativeInstance.blockCopyPaste(blockCopyPaste);
    }

    
    
    private static PushCallback pushCallback;
    
    public static void pushReceived(final String message, final String type) {
        if(pushCallback != null) {
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    try {
                        if(type != null) {
                            Display.getInstance().setProperty("pushType", type);
                            PushContent.setType(Integer.parseInt(type));
                        }

                        pushCallback.push(message);
                    } finally {
                        if (!"true".equals(Display.getInstance().getProperty("delayPushCompletion", "false")) &&
                            !"true".equals(Display.getInstance().getProperty("ios.delayPushCompletion", "false"))) {
                            nativeInstance.firePushCompletionHandler();
                        }
                    }
                }
            });
        } else {
            nativeInstance.firePushCompletionHandler();
            /*
            // Removing this section because the race condition shouldn't happen
            // anymore as setMainClass() is now called before initialization.
            
            // could be a race condition against the native code... Retry in 2 seconds
            new Thread() {
                public void run() {
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException ex) {
                    }
                    // prevent infinite loop
                    if(pushCallback != null) {
                        pushReceived(message, type); 
                    }
                }
            }.start();
            */
        }
    }
    public static void pushRegistered(final String deviceKey) {
        if(instance != null) {
            instance.systemOut("Push handleRegistration() Sending registration to server: " + deviceKey);
        }
        String c = callback.getClass().getName();
        final String clsName = c.substring(0, c.lastIndexOf('.'));
        if(pushCallback != null) {
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    if(CodenameOneImplementation.registerServerPush(deviceKey, getApplicationKey(), (byte)2, "", clsName)) {
                        pushCallback.registeredForPush(deviceKey);
                    } else {
                        pushCallback.pushRegistrationError("Server registration error", 1);
                        pushCallback.registeredForPush(deviceKey);
                    }
                }
            });
        }
    }

    public static void pushRegistrationError(final String message) {
        if(pushCallback != null) {
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    pushCallback.pushRegistrationError(message, 0);
                }
            });
        }
    }
    
    public static void initPushActionCategories() {
        if (pushCallback instanceof PushActionsProvider) {
            PushActionsProvider actionsProvider = (PushActionsProvider)pushCallback;
            PushActionCategory[] categories = actionsProvider.getPushActionCategories();
            if (categories != null) {
                PushAction[] actions = PushActionCategory.getAllActions(categories);
                for (PushAction action : actions) {
                    nativeInstance.registerPushAction(action.getId(), action.getTitle(), action.getTextInputPlaceholder(), action.getTextInputButtonText());
                }
                for (PushActionCategory category : categories) {
                    nativeInstance.startPushActionCategory(category.getId());
                    for (PushAction action : category.getActions()) {
                        nativeInstance.addPushActionToCategory(action.getId());
                    }
                    nativeInstance.endPushActionCategory();
                }
                nativeInstance.registerPushCategories();
            }
        }
    }

    public static void setPushCallback(PushCallback callback) {
        pushCallback = callback;
    }
    
    public static void setLocalNotificationCallback(LocalNotificationCallback callback) {
        localNotificationCallback = callback;
    }
    
    public static LocalNotificationCallback getLocalNotificationCallback() {
        return localNotificationCallback;
    }
    
    
    public static void localNotificationReceived(final String notificationId) {
        if (localNotificationCallback != null) {
            // this should be invoked off the EDT...
            localNotificationCallback.localNotificationReceived(notificationId);
        } else { // could be a race condition against the native code... Retry in 2 seconds
            new Thread() {
                public void run() {
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException ex) {
                    }
                    // prevent infinite loop
                    if(pushCallback != null) {
                        localNotificationReceived(notificationId);
                    }
                }
            }.start();
        }
    }
    
    
    
    public static void setMainClass(Object main) {
        setCurrentApplicationInstance(main);
        if(main instanceof PushCallback) {
            pushCallback = (PushCallback)main;
        }
        if(main instanceof PurchaseCallback) {
            purchaseCallback = (PurchaseCallback)main;
        }
        if(main instanceof RestoreCallback) {
            restoreCallback = (RestoreCallback)main;
        }
        if (main instanceof LocalNotificationCallback) {
            setLocalNotificationCallback((LocalNotificationCallback) main);
        }
        if (main instanceof BackgroundFetch) {
            backgroundFetchCallback = (BackgroundFetch)main;
        }
    }        
    
    private L10NManager l10n;

    /**
     * @inheritDoc
     */
    public L10NManager getLocalizationManager() {
        if(l10n == null) {
            Locale l = Locale.getDefault();
            l10n = new L10NManager(l.getLanguage(), l.getCountry()) {
                public String format(int number) {
                    return nativeInstance.formatInt(number);
                }

                @Override
                public String getLongMonthName(Date date) {
                    return nativeInstance.getLongMonthName(date.getTime());
                }

                @Override
                public String getShortMonthName(Date date) {
                    return nativeInstance.getShortMonthName(date.getTime());
                }
                
                public String format(double number) {
                    return nativeInstance.formatDouble(number);
                }

                public String formatCurrency(double currency) {
                    return nativeInstance.formatCurrency(currency);
                }

                public String formatDateLongStyle(Date d) {
                    return nativeInstance.formatDate(d.getTime());
                }

                public String formatDateShortStyle(Date d) {
                    return nativeInstance.formatDateShort(d.getTime());
                }

                public String formatDateTime(Date d) {
                    return nativeInstance.formatDateTime(d.getTime());
                }
                
                public double parseDouble(String localeFormattedDecimal) {
                    return nativeInstance.parseDouble(localeFormattedDecimal);
                }
                
                public String formatDateTimeMedium(Date d) {
                    return nativeInstance.formatDateTimeMedium(d.getTime());
                }

                public String formatDateTimeShort(Date d) {
                    return nativeInstance.formatDateTimeShort(d.getTime());
                }

                public String getCurrencySymbol() {
                    return nativeInstance.getCurrencySymbol();
                }

                public void setLocale(String locale, String language) {
                    super.setLocale(locale, language);
                    Locale l = new Locale(language, locale);
                    Locale.setDefault(l);
                    nativeInstance.setLocale(language+"_"+locale);
                }
            };
        }
        return l10n;
    }
    
    private ImageIO imageIO;
    
    @Override
    public ImageIO getImageIO() {
        if(imageIO == null) {
            imageIO = new ImageIO() {
                @Override
                public void save(InputStream image, OutputStream response, String format, int width, int height, float quality) throws IOException {
                    Image img = Image.createImage(image);
                    NativeImage ni = (NativeImage)img.getImage();
                    long p = nativeInstance.createImageFile(ni.peer, format.equals(FORMAT_JPEG), width, height, quality);
                    writeNSData(p, response);
                }

                private void writeNSData(long p, OutputStream os) throws IOException {
                    int size = nativeInstance.getNSDataSize(p);
                    if(size < 128 * 1024) {
                        byte[] b = new byte[size];
                        nativeInstance.nsDataToByteArray(p, b);
                        nativeInstance.releasePeer(p);
                        os.write(b);
                        os.close();
                    } else {
                        NSDataInputStream ni = new NSDataInputStream(p, size);
                        Util.copy(ni, os);
                        ni.close();
                    }
                }
                
                @Override
                protected void saveImage(Image img, OutputStream response, String format, float quality) throws IOException {
                    globalGraphics.checkControl();
                    NativeImage ni = (NativeImage)img.getImage();
                    long p = nativeInstance.createImageFile(ni.peer, format.equals(FORMAT_JPEG), img.getWidth(), img.getHeight(), quality);
                    writeNSData(p, response);
                }

                @Override
                public boolean isFormatSupported(String format) {
                    return format.equals(FORMAT_JPEG) || format.equals(FORMAT_PNG);
                }
            };
        }
        return imageIO;
    }

    
    /**
     * Workaround for XMLVM bug
     */
    public boolean instanceofObjArray(Object o) {
        return instanceofObjArrayI(o);        
    }
    
    /**
     * Workaround for XMLVM bug
     */
    public boolean instanceofByteArray(Object o) {
        return instanceofByteArrayI(o);
    }
    
    /**
     * Workaround for XMLVM bug
     */
    public boolean instanceofShortArray(Object o) {
        return instanceofShortArrayI(o);        
    }
    
    /**
     * Workaround for XMLVM bug
     */
    public boolean instanceofLongArray(Object o) {
        return instanceofLongArrayI(o);        
    }
    
    /**
     * Workaround for XMLVM bug
     */
    public boolean instanceofIntArray(Object o) {
        return instanceofIntArrayI(o);        
    }
    
    /**
     * Workaround for XMLVM bug
     */
    public boolean instanceofFloatArray(Object o) {
        return instanceofFloatArrayI(o);        
    }
    
    /**
     * Workaround for XMLVM bug
     */
    public boolean instanceofDoubleArray(Object o) {
        return instanceofDoubleArrayI(o);        
    }

    /**
     * Workaround for XMLVM bug
     */
    private static native boolean instanceofObjArrayI(Object o);
    
    /**
     * Workaround for XMLVM bug
     */
    private static native boolean instanceofByteArrayI(Object o);
    
    /**
     * Workaround for XMLVM bug
     */
    private static native boolean instanceofShortArrayI(Object o);
    
    /**
     * Workaround for XMLVM bug
     */
    private static native boolean instanceofLongArrayI(Object o);
    
    /**
     * Workaround for XMLVM bug
     */
    private static native boolean instanceofIntArrayI(Object o);
    
    /**
     * Workaround for XMLVM bug
     */
    private static native boolean instanceofFloatArrayI(Object o);
    
    /**
     * Workaround for XMLVM bug
     */
    private static native boolean instanceofDoubleArrayI(Object o);

    @Override
    public Database openOrCreateDB(String databaseName) throws IOException{
        return new DatabaseImpl(databaseName);
    }

    @Override
    public String getDatabasePath(String databaseName) {
        String s = nativeInstance.getDocumentsDir();
        if(!s.endsWith("/")) {
            s += "/";
        }
        return s + databaseName;
    }
    
    @Override
    public void deleteDB(String databaseName) throws IOException{
        nativeInstance.sqlDbDelete(databaseName);
    }
    
    @Override
    public boolean existsDB(String databaseName){
        return nativeInstance.sqlDbExists(databaseName);
    }

    /**
     * Sent when the application is about to move from active to inactive state. 
     * This can occur for certain types of temporary interruptions (such as an 
     * incoming phone call or SMS message) or when the user quits the application 
     * and it begins the transition to the background state.
     * Use this method to pause ongoing tasks, disable timers, and throttle down 
     * OpenGL ES frame rates. Games should use this method to pause the game.
     */
    public static void applicationWillResignActive() {
        minimized = true;
        callInterruptionActive = true;
        if(instance.life != null) {
            instance.life.applicationWillResignActive();
        }
        instance.isActive = false;
    }
    
    /**
     * Headphones connected callback
     */
    public static void headphonesConnected() {
        if(instance.life != null) {
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    instance.life.headphonesConnected();
                }
            });
        }        
    }

    /**
     * Headphones disconnected callback
     */
    public static void headphonesDisconnected() {
        if(instance.life != null) {
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    instance.life.headphonesDisconnected();
                }
            });
        }        
    }
    
    
    public static long beginBackgroundTask() {
        return nativeInstance.beginBackgroundTask();
    }
    
    public static void endBackgroundTask(long taskId) {
        nativeInstance.endBackgroundTask(taskId);
    }
    
    /**
     * Use this method to release shared resources, save user data, invalidate 
     * timers, and store enough application state information to restore your 
     * application to its current state in case it is terminated later.
     * If your application supports background execution, this method is called 
     * instead of applicationWillTerminate: when the user quits.
     */
    public static void applicationDidEnterBackground() {
        minimized = true;
        if(instance.life != null) {
            instance.life.applicationDidEnterBackground();
            if (instance.isEditingText()) {
                instance.stopTextEditing();
            }
        }
    }
    /**
     * Indicates whether the application should handle the given URL, defaults to true
     * @param url the URL to handle
     * @param caller the invoking application
     * @return true to handle the URL, false otherwise
     */
    public static boolean shouldApplicationHandleURL(String url, String caller) {
        if(instance.life != null) {
            instance.life.shouldApplicationHandleURL(url, caller);
        }
        if(Display.getInstance() != null) {
            Display.getInstance().setProperty("AppArg", url);
        }
        return true;
    }

    /**
     * Use this method to release shared resources, save user data, invalidate 
     * timers, and store enough application state information to restore your 
     * application to its current state in case it is terminated later.
     * If your application supports background execution, this method is called 
     * instead of applicationWillTerminate: when the user quits.
     */
    public static void applicationWillEnterForeground() {
        minimized = false;
        if(instance.life != null) {
            instance.life.applicationWillEnterForeground();
        }
        
    }
    
    /**
     * Called by the application delegate's <a href="https://developer.apple.com/documentation/uikit/core_app/allowing_apps_and_websites_to_link_to_your_content/handling_universal_links?language=objc">universal links handler</a>.
     * This will stop the app, set the AppArg, and then start the app again.
     * @param url 
     */
    public static void applicationReceivedUniversalLink(String url) {
        applicationDidEnterBackground();
        if(Display.getInstance() != null) {
            Display.getInstance().setProperty("AppArg", url);
        }
        applicationWillEnterForeground();
    }
    
    public static void performBackgroundFetch() {
        
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                // Note we have to check for backgroundFetchCallback inside this callSerially
                // because it might not have been set yet if we call it outside.
                if (backgroundFetchCallback != null) {
                    backgroundFetchCallback.performBackgroundFetch(System.currentTimeMillis()+25*60*1000, new Callback<Boolean>() {

                        @Override
                        public void onSucess(Boolean value) {
                            if (!value) {
                                nativeInstance.fireUIBackgroundFetchResultNoData();
                            } else {
                                nativeInstance.fireUIBackgroundFetchResultNewData();
                            }
                        }

                        @Override
                        public void onError(Object sender, Throwable err, int errorCode, String errorMessage) {
                            Log.e(err);
                            nativeInstance.fireUIBackgroundFetchResultFailed();
                        }
                    });

                }
            }
        });
  
    }

    @Override
    public void setPreferredBackgroundFetchInterval(int seconds) {
        super.setPreferredBackgroundFetchInterval(seconds);
        nativeInstance.setPreferredBackgroundFetchInterval(seconds);
    }

    @Override
    public boolean isBackgroundFetchSupported() {
        return nativeInstance.isBackgroundFetchSupported();
    }
    
    
    
    
    
    /**
     * Calls the given runnable when the app is active.  If the app is already
     * active, it will call it immediatly.  If not, it will be called in
     * applicationDidBecomeActive().
     * This is used for getting the AppArg property in a way that avoids
     * race conditions.
     * @param r 
     */
    private void callOnActive(Runnable r) {
        synchronized(onActiveListeners) {
            if (isActive) {
                r.run();
            } else {
                onActiveListeners.add(r);
            }
        }
    }
    
    /**
     * Called as part of the transition from the background to the inactive state; 
     * here you can undo many of the changes made on entering the background.
     */
    public static void applicationDidBecomeActive() {
        callInterruptionActive = false;
        ArrayList<Runnable> callbacks = null;
        synchronized(instance.onActiveListeners) {
            instance.isActive = true;
            callbacks = new ArrayList<Runnable>(instance.onActiveListeners.size());
        
            callbacks.addAll(instance.onActiveListeners);
            instance.onActiveListeners.clear();
        }
        for (Runnable callback : callbacks) {
            callback.run();
        }
        minimized = false;
        if(instance.life != null) {
            instance.life.applicationDidBecomeActive();
        }
        if(Display.getInstance() != null) {
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    Form f = Display.getInstance().getCurrent();
                    if(f != null) {
                        f.revalidate();
                    }
                }
            });
        }
    }
    
    public static void paintNow() {
        final Display d = Display.getInstance();
        d.callSeriallyAndWait(new Runnable() {
            @Override
            public void run() {
                Form f = d.getCurrent();
                f.paintComponent(instance.getCodenameOneGraphics(), true);
            }
        }, 50);
    }
    
    /**
     * Restart any tasks that were paused (or not yet started) while the 
     * application was inactive. If the application was previously in the background, 
     * optionally refresh the user interface.
     */
    public static void applicationWillTerminate() {
        if(instance.life != null) {
            instance.life.applicationWillTerminate();
        }
    }
    
    @Override
    public boolean isNativeShareSupported(){
        String ver = nativeInstance.getOSVersion();
        if(ver.startsWith("5.") || ver.startsWith("4.")) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isNativeInAppReviewSupported() {
        // SKStoreReviewController.requestReview is available since iOS 10.3.
        String ver = nativeInstance.getOSVersion();
        int dot = ver.indexOf('.');
        try {
            int major = Integer.parseInt(dot < 0 ? ver : ver.substring(0, dot));
            if (major > 10) {
                return true;
            }
            if (major < 10) {
                return false;
            }
            String rest = dot < 0 ? "" : ver.substring(dot + 1);
            int dot2 = rest.indexOf('.');
            int minor = Integer.parseInt(dot2 < 0 ? rest : rest.substring(0, dot2));
            return minor >= 3;
        } catch (NumberFormatException err) {
            // Unknown/odd version string -- assume a modern OS supports it.
            return true;
        }
    }

    @Override
    public void requestNativeInAppReview(SuccessCallback<Boolean> done) {
        // StoreKit gives no callback and may silently throttle the prompt, so
        // we simply report that the request was handed off to the controller.
        nativeInstance.requestAppStoreReview();
        if (done != null) {
            done.onSucess(Boolean.TRUE);
        }
    }


    
    @Override
    public void share(String text, String image, String mimeType, Rectangle sourceRect){
        share(text, image, mimeType, sourceRect, null);
    }

    @Override
    public void share(String text, String image, String mimeType, Rectangle sourceRect, com.codename1.share.ShareResultListener listener) {
        long imagePeer = 0;
        if (image != null && image.length() > 0) {
            try {
                Image img = Image.createImage(image);
                if (img != null) {
                    NativeImage n = (NativeImage) img.getImage();
                    imagePeer = n.peer;
                }
            } catch (IOException err) {
                err.printStackTrace();
                if (listener != null) {
                    listener.onResult(com.codename1.share.ShareResult.failed("Error loading image: " + image));
                    return;
                }
                Dialog.show("Error", "Error loading image: " + image, "OK", null);
                return;
            }
        }
        if (listener == null) {
            nativeInstance.socialShare(text, imagePeer, sourceRect);
            return;
        }
        int callbackId = registerShareCallback(listener);
        nativeInstance.socialShareWithCallback(text, imagePeer, sourceRect, callbackId);
    }

    // Pending share-result callbacks. Native code invokes
    // socialShareCallback(...) once per id.
    private static final java.util.HashMap<Integer, com.codename1.share.ShareResultListener> pendingShareCallbacks = new java.util.HashMap<Integer, com.codename1.share.ShareResultListener>();
    private static int nextShareCallbackId = 1;

    private static synchronized int registerShareCallback(com.codename1.share.ShareResultListener l) {
        int id = nextShareCallbackId++;
        pendingShareCallbacks.put(Integer.valueOf(id), l);
        return id;
    }

    /// Invoked from native code with the outcome of a share. Public so the
    /// VM-emitted symbol stays stable. `status` matches
    /// [com.codename1.share.ShareResult]: 1=SHARED_TO, 2=DISMISSED, 3=FAILED.
    public static void socialShareCallback(int callbackId, int status, String activityType, String errorMessage) {
        com.codename1.share.ShareResultListener listener;
        synchronized (IOSImplementation.class) {
            listener = pendingShareCallbacks.remove(Integer.valueOf(callbackId));
        }
        if (listener == null) {
            return;
        }
        com.codename1.share.ShareResult result;
        switch (status) {
            case 1:
                result = com.codename1.share.ShareResult.sharedTo(activityType);
                break;
            case 2:
                result = com.codename1.share.ShareResult.dismissed();
                break;
            default:
                result = com.codename1.share.ShareResult.failed(errorMessage);
                break;
        }
        listener.onResult(result);
    }

    @Override
    public boolean isPrintingSupported() {
        return nativeInstance.isPrintingAvailable();
    }

    @Override
    public void print(String filePath, String mimeType, com.codename1.printing.PrintResultListener listener) {
        int callbackId = registerPrintCallback(listener);
        nativeInstance.printDocument(filePath, mimeType, callbackId);
    }

    // Pending print-result callbacks. Native code invokes
    // printDocumentCallback(...) once per id.
    private static final java.util.HashMap<Integer, com.codename1.printing.PrintResultListener> pendingPrintCallbacks = new java.util.HashMap<Integer, com.codename1.printing.PrintResultListener>();
    private static int nextPrintCallbackId = 1;

    private static synchronized int registerPrintCallback(com.codename1.printing.PrintResultListener l) {
        int id = nextPrintCallbackId++;
        pendingPrintCallbacks.put(Integer.valueOf(id), l);
        return id;
    }

    /// Invoked from native code with the outcome of a print job. Public so
    /// the VM-emitted symbol stays stable. `status` matches
    /// [com.codename1.printing.PrintResult]: 1=COMPLETED, 2=CANCELLED, 3=FAILED.
    public static void printDocumentCallback(int callbackId, int status, String errorMessage) {
        com.codename1.printing.PrintResultListener listener;
        synchronized (IOSImplementation.class) {
            listener = pendingPrintCallbacks.remove(Integer.valueOf(callbackId));
        }
        if (listener == null) {
            return;
        }
        com.codename1.printing.PrintResult result;
        switch (status) {
            case 1:
                result = com.codename1.printing.PrintResult.completed();
                break;
            case 2:
                result = com.codename1.printing.PrintResult.cancelled();
                break;
            default:
                result = com.codename1.printing.PrintResult.failed(errorMessage);
                break;
        }
        listener.onResult(result);
    }

    private Purchase pur;
    private Vector purchasedItems;

    /**
     * Call serially will fail if Display isn't initialized yet this will not
     */
    private static void safeCallSerially(final Runnable r) {
        if(Display.isInitialized()) {
            Display.getInstance().callSerially(r);
            return;
        }
        new Thread() {
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch(Throwable t) {}
                safeCallSerially(r);
            }
        }.start();
    }
    
    private static final String PURCHASES_KEY="CN1PurchasedItemList.dat";
    
    List getPurchased() {
        synchronized(PURCHASES_KEY){
            if(purchasedItems == null) {
                purchasedItems = new Vector();
                List items = (List)Storage.getInstance().readObject(PURCHASES_KEY);
                if (items != null){
                    purchasedItems.addAll(items);
                }

            }
            return purchasedItems;
        }
    }
    
    void addPurchase(String sku){
        List purchased = getPurchased();
        synchronized(PURCHASES_KEY){
            if (!purchased.contains(sku)){
                purchased.add(sku);
                commitPurchased();
            }
        }
    }
    
    void removePurchase(String sku){
        List purchased = getPurchased();
        synchronized(PURCHASES_KEY){
            if (purchased.contains(sku)){
                purchased.remove(sku);
                commitPurchased();
            }
        }
    }
    
    void commitPurchased(){
        if (purchasedItems != null){
            Storage.getInstance().writeObject(PURCHASES_KEY, purchasedItems);
        }
    }
    
    static void itemPurchased(final String sku) {
        safeCallSerially(new Runnable() {
            @Override
            public void run() {
                instance.addPurchase(sku);
                if (purchaseCallback != null){
                    purchaseCallback.itemPurchased(sku);
                }
            }
        });
    }
    
    static void itemRestored(final String sku) {
        safeCallSerially(new Runnable() {
            @Override
            public void run() {
                instance.addPurchase(sku);
                if (restoreCallback != null){
                    restoreCallback.itemRestored(sku);
                }
            }
        });
    }
    
    static void restoreRequestComplete() {
        if(restoreCallback != null) {
            safeCallSerially(new Runnable() {
                @Override
                public void run() {
                    restoreCallback.restoreRequestComplete();
                }
            });
        }
    }
    
    static void restoreRequestError(final String errorMessage) {
        if(restoreCallback != null) {
            safeCallSerially(new Runnable() {
                @Override
                public void run() {
                    restoreCallback.restoreRequestError(errorMessage);
                }
            });
        }
    }
    
    static void itemPurchaseError(final String sku, final String errorMessage) {
        if(purchaseCallback != null) {
            safeCallSerially(new Runnable() {
                @Override
                public void run() {
                    purchaseCallback.itemPurchaseError(sku, errorMessage);
                }
            });
        }
    }

    static void itemRefunded(final String sku) {
        safeCallSerially(new Runnable() {
            @Override
            public void run() {
                instance.removePurchase(sku);
                if (purchaseCallback != null){
                    purchaseCallback.itemRefunded(sku);
                }
            }
        });
    }


    static void subscriptionStarted(final String sku) {
        if(purchaseCallback != null) {
            safeCallSerially(new Runnable() {
                @Override
                public void run() {
                    purchaseCallback.subscriptionStarted(sku);
                }
            });
        }
    }

    static void subscriptionCanceled(final String sku) {
        if(purchaseCallback != null) {
            safeCallSerially(new Runnable() {
                @Override
                public void run() {
                    purchaseCallback.subscriptionCanceled(sku);
                }
            });
        }
    }
    
    static void paymentFailed(final String paymentCode, final String failureReason) {
        if(purchaseCallback != null) {
            safeCallSerially(new Runnable() {
                @Override
                public void run() {
                    purchaseCallback.paymentFailed(paymentCode, failureReason);
                }
            });
        }
    }
    
    static void paymentSucceeded(final String paymentCode, final double amount, final String currency) {
        if(purchaseCallback != null) {
            safeCallSerially(new Runnable() {
                @Override
                public void run() {
                    purchaseCallback.paymentSucceeded(paymentCode, amount, currency);
                }
            });
        }
    }
    
    public Purchase getInAppPurchase() {
        return new ZoozPurchase(this, nativeInstance, purchaseCallback);
    }
    
    @Override
    public CodeScanner getCodeScanner() {
        if(scannerInstance == null) {
            scannerInstance = new CodeScannerImpl();
        }
        return scannerInstance;
    }
    
    static void scanCompleted(final String contents, final String formatName) {
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                instance.scannerInstance.callback.scanCompleted(contents, formatName, null);
                instance.scannerInstance.callback = null;
                Display.getInstance().getCurrent().revalidate();
                Display.getInstance().getCurrent().repaint();
            }
        });
    }

    static void scanError(final int errorCode, final String message) {
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                instance.scannerInstance.callback.scanError(errorCode, message);
                instance.scannerInstance.callback = null;
                Display.getInstance().getCurrent().revalidate();
                Display.getInstance().getCurrent().repaint();
            }
        });
    }

    static void scanCanceled() {
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                instance.scannerInstance.callback.scanCanceled();
                instance.scannerInstance.callback = null;
                Display.getInstance().getCurrent().revalidate();
                Display.getInstance().getCurrent().repaint();
            }
        });
    }
    
    class CodeScannerImpl extends CodeScanner  {
        private ScanResult callback;
        
        @Override
        public void scanQRCode(ScanResult callback) {
            this.callback = callback;
            nativeInstance.scanQRCode();
        }

        @Override
        public void scanBarCode(ScanResult callback) {
            this.callback = callback;
            nativeInstance.scanBarCode();
        }
    }

    @Override
    public boolean isNativePickerTypeSupported(int pickerType) {
        return pickerType == Display.PICKER_TYPE_DATE || pickerType == Display.PICKER_TYPE_TIME || pickerType == Display.PICKER_TYPE_DATE_AND_TIME || pickerType == Display.PICKER_TYPE_STRINGS || pickerType == Display.PICKER_TYPE_DURATION;
    }
    
    private static long datePickerResult;
    private static final Object PICKER_LOCK = new Object();
    static void datePickerResult(long val) {
        synchronized(PICKER_LOCK) {
            datePickerResult = val;
            PICKER_LOCK.notify();
        }
    }
    
    @Override
    public Object showNativePicker(final int type, final Component source, final Object currentValue, final Object data) {
        datePickerResult = -2;
        int x = 0, y = 0, w = 20, h = 20, preferredHeight = 0, preferredWidth = 0;
        
        if(source != null) {
            x = source.getAbsoluteX();
            y = source.getAbsoluteY();
            w = source.getWidth();
            h = source.getHeight();
        }
        
        if (source instanceof Picker) {
            Picker p = (Picker)source;
            preferredHeight = p.getPreferredPopupHeight();
            preferredWidth = p.getPreferredPopupWidth();
        }
        
        if(type == Display.PICKER_TYPE_STRINGS) {
            String[] strs = (String[])data;
            int offset = -1;
            if(currentValue != null) {
                int slen = strs.length;
                for(int iter = 0 ; iter < slen ; iter++) {
                    if(strs[iter].equals(currentValue)) {
                        offset = iter;
                        break;
                    }
                }
            }
            nativeInstance.openStringPicker(strs, offset, x, y, w, h, preferredWidth, preferredHeight);
        } else if (type == Display.PICKER_TYPE_DURATION) {
            long time;
            if (currentValue instanceof Long) {
                time = (Long)currentValue;
            } else {
                time = 0l;
            }
            int minuteStep = 5;
            if (data instanceof String) {
                String strData = (String)data;
                String[] parts = Util.split(strData, "\n");
                for (String part : parts) {
                    if (part.indexOf("minuteStep=") != -1) {
                        minuteStep = Integer.parseInt(part.substring(part.indexOf("=")+1));
                    }
                }
            }
            nativeInstance.openDatePicker(type, time, x, y, w, h, preferredWidth, preferredHeight, minuteStep);
        } else {
            long time;
            if(currentValue instanceof Integer) {
                java.util.Calendar c = java.util.Calendar.getInstance();
                c.set(java.util.Calendar.HOUR_OF_DAY, ((Integer)currentValue).intValue() / 60);
                c.set(java.util.Calendar.MINUTE, ((Integer)currentValue).intValue() % 60);
                time = c.getTime().getTime();
            } else if (currentValue != null) {
                time = ((java.util.Date)currentValue).getTime();
            } else {
                time = new java.util.Date().getTime();
            }
            int minuteStep = 5;
            if (data instanceof String) {
                String strData = (String)data;
                String[] parts = Util.split(strData, "\n");
                for (String part : parts) {
                    if (part.indexOf("minuteStep=") != -1) {
                        minuteStep = Integer.parseInt(part.substring(part.indexOf("=")+1));
                    }
                }
            }
            nativeInstance.openDatePicker(type, time, x, y, w, h, preferredWidth, preferredHeight, minuteStep);
        }
        // wait for the native code to complete
        Display.getInstance().invokeAndBlock(new Runnable() {
            public void run() {
                while(datePickerResult == -2) {
                    synchronized(PICKER_LOCK) {
                        try {
                            PICKER_LOCK.wait(100);
                        } catch(InterruptedException err) {}
                    }
                }
            }
        }, true);
        if(datePickerResult == -1) {
            // there is no cancel option in the phone device
            // Commented out because now iOS7 and higher have a cancel button
            //  And should we even care about this case if there is no
            // cancel button?
            //if(!isTablet()) {
            //    return currentValue;
            //}
            return null;
        }
        if(type == Display.PICKER_TYPE_STRINGS) {
            if(datePickerResult < 0) {
                return null;
            }
            return ((String[])data)[(int)datePickerResult];
        }
        Object result;
        if (type == Display.PICKER_TYPE_DURATION || type == Display.PICKER_TYPE_DURATION_HOURS || type == Display.PICKER_TYPE_DURATION_MINUTES) {
            if (datePickerResult < 0) {
                return null;
            }
            return new Long(datePickerResult);
        }
        if(type == Display.PICKER_TYPE_TIME) {
            java.util.Calendar c = java.util.Calendar.getInstance();
            c.setTime(new Date(datePickerResult));
            result = new Integer(c.get(java.util.Calendar.HOUR_OF_DAY) * 60 + c.get(java.util.Calendar.MINUTE));
        } else {
            result = new Date(datePickerResult);
        }
        return result;
    }

    @Override
    public Object connectSocket(String host, int port, int connectTimeout) {
        long i = nativeInstance.connectSocket(host, port, connectTimeout);
        if(i != 0) {
            return new Long(i);
        }
        return null;
    }
    
    @Override
    public Object listenSocket(int port) {
        return null;
    }
    
    @Override
    public String getHostOrIP() {
        return nativeInstance.getHostOrIP();
    }

    @Override
    public void disconnectSocket(Object socket) {
        nativeInstance.disconnectSocket(((Long)socket).longValue());
    }    
    
    @Override
    public boolean isSocketConnected(Object socket) {
        return nativeInstance.isSocketConnected(((Long)socket).longValue());
    }
    
    @Override
    public boolean isServerSocketAvailable() {
        return false;
    }

    @Override
    public boolean isSocketAvailable() {
        return true;
    }
    
    @Override
    public String getSocketErrorMessage(Object socket) {
        return nativeInstance.getSocketErrorMessage(((Long)socket).longValue());
    }
    
    @Override
    public int getSocketErrorCode(Object socket) {
        return nativeInstance.getSocketErrorCode(((Long)socket).longValue());
    }
    
    @Override
    public int getSocketAvailableInput(Object socket) {
        return nativeInstance.getSocketAvailableInput(((Long)socket).longValue());
    }
    
    @Override
    public byte[] readFromSocketStream(Object socket) {
        return nativeInstance.readFromSocketStream(((Long)socket).longValue());
    }
    
    @Override
    public void writeToSocketStream(Object socket, byte[] data) {
        nativeInstance.writeToSocketStream(((Long)socket).longValue(), data);
    }

    @Override
    public boolean isWebSocketSupported() {
        return true;
    }

    @Override
    public com.codename1.impl.WebSocketImpl createWebSocketImpl(String url) {
        return new IOSWebSocketImpl(url);
    }

    @Override
    public void writeToSocketStream(Object socket, byte[] data, int offset, int len) {
        nativeInstance.writeToSocketStream(((Long)socket).longValue(), data, offset, len);
    }

    @Override
    public void splitString(String source, char separator, ArrayList<String> out) {
        nativeInstance.splitString(source, separator, out);
    }
   
    public void scheduleLocalNotification(LocalNotification n, long firstTime, int repeat) {
        boolean enriched = !n.getActions().isEmpty() || n.getGroupId() != null
                || n.isTimeSensitive() || (n.getAlertImage() != null && n.getAlertImage().length() > 0);
        if (enriched) {
            String categoryId = null;
            String actionsEncoded = null;
            if (!n.getActions().isEmpty()) {
                categoryId = "cn1-ln-" + n.getId();
                StringBuilder sb = new StringBuilder();
                for (LocalNotification.Action a : n.getActions()) {
                    if (sb.length() > 0) {
                        sb.append('\u0002');
                    }
                    sb.append(nullToEmpty(a.getId())).append('\u0001')
                      .append(nullToEmpty(a.getTitle())).append('\u0001')
                      .append(nullToEmpty(a.getTextInputPlaceholder())).append('\u0001')
                      .append(nullToEmpty(a.getTextInputButtonText()));
                }
                actionsEncoded = sb.toString();
            }
            nativeInstance.sendLocalNotification2(
                    n.getId(), n.getAlertTitle(), n.getAlertBody(), n.getAlertSound(),
                    n.getBadgeNumber(), firstTime, repeat, n.isForeground(),
                    categoryId, n.getGroupId(), n.isTimeSensitive(), n.getAlertImage(), actionsEncoded);
        } else {
            nativeInstance.sendLocalNotification(
                    n.getId(),
                    n.getAlertTitle(),
                    n.getAlertBody(),
                    n.getAlertSound(),
                    n.getBadgeNumber(),
                    firstTime,
                    repeat,
                    n.isForeground()
            );
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    public void cancelLocalNotification(String id) {
         nativeInstance.cancelLocalNotification(id);
    }

    // ---- notification permission ----

    private static NotificationPermissionCallback pendingNotificationPermissionCallback;

    @Override
    public void requestNotificationPermission(NotificationPermissionRequest request, NotificationPermissionCallback callback) {
        pendingNotificationPermissionCallback = callback;
        nativeInstance.requestNotificationPermission(request == null ? 7 : request.toAuthorizationOptionsMask());
    }

    /// Invoked from native once the authorization request resolves. authLevel is the
    /// ordinal of NotificationPermissionResult.AuthorizationLevel as produced by the
    /// native UNAuthorizationStatus mapping (granted is derived from the level).
    public static void notificationPermissionResult(final boolean granted, final int authLevel) {
        final NotificationPermissionCallback cb = pendingNotificationPermissionCallback;
        pendingNotificationPermissionCallback = null;
        if (cb != null) {
            final NotificationPermissionResult.AuthorizationLevel[] levels =
                    NotificationPermissionResult.AuthorizationLevel.values();
            final NotificationPermissionResult.AuthorizationLevel level =
                    (authLevel >= 0 && authLevel < levels.length)
                            ? levels[authLevel]
                            : NotificationPermissionResult.AuthorizationLevel.NOT_DETERMINED;
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    cb.notificationPermissionResult(new NotificationPermissionResult(level));
                }
            });
        }
    }

    // ---- constraint-aware background work / processing (BGTaskScheduler) ----

    @Override
    public boolean isBackgroundWorkSupported() {
        return nativeInstance.isBackgroundProcessingSupported();
    }

    @Override
    public boolean isBackgroundProcessingSupported() {
        return nativeInstance.isBackgroundProcessingSupported();
    }

    @Override
    public void scheduleBackgroundWork(WorkRequest request) {
        // persist worker class and input so the work can be reconstructed after a cold launch
        com.codename1.io.Preferences.set("$$CN1_BGWORK_CLASS_" + request.getId(), request.getWorkerClass());
        StringBuilder input = new StringBuilder();
        for (java.util.Map.Entry<String, String> e : request.getInputData().entrySet()) {
            if (input.length() > 0) {
                input.append('\u0002');
            }
            input.append(e.getKey()).append('\u0001').append(e.getValue());
        }
        com.codename1.io.Preferences.set("$$CN1_BGWORK_INPUT_" + request.getId(), input.toString());
        com.codename1.io.Preferences.set("$$CN1_BGWORK_PERIODIC_" + request.getId(), request.isPeriodic());
        double earliest = (System.currentTimeMillis() + Math.max(0, request.getInitialDelayMillis())) / 1000.0;
        nativeInstance.submitBackgroundProcessingTask(request.getId(), earliest,
                request.isRequiresNetwork() || request.isRequiresUnmeteredNetwork(), request.isRequiresCharging());
    }

    @Override
    public void cancelBackgroundWork(String workId) {
        nativeInstance.cancelBackgroundTask(workId);
    }

    @Override
    public void scheduleBackgroundProcessing(String id, long earliestBeginEpochMs, boolean requiresNetwork, boolean requiresPower, Runnable task) {
        if (task != null) {
            backgroundProcessingRunnables.put(id, task);
        }
        double earliest = earliestBeginEpochMs <= 0 ? System.currentTimeMillis() / 1000.0 : earliestBeginEpochMs / 1000.0;
        nativeInstance.submitBackgroundProcessingTask(id, earliest, requiresNetwork, requiresPower);
    }

    @Override
    public void cancelBackgroundProcessing(String id) {
        backgroundProcessingRunnables.remove(id);
        nativeInstance.cancelBackgroundTask(id);
    }

    private static final java.util.Map<String, Runnable> backgroundProcessingRunnables = new java.util.HashMap<String, Runnable>();

    /// Invoked from the BGTaskScheduler launch handler. Runs the worker (reconstructed from
    /// persisted state) or a live processing runnable for the given identifier.
    public static void runBackgroundProcessing(final String id) {
        Runnable live = backgroundProcessingRunnables.remove(id);
        if (live != null) {
            try {
                live.run();
            } catch (Throwable t) {
                com.codename1.io.Log.e(t);
            }
            return;
        }
        String workerClass = com.codename1.io.Preferences.get("$$CN1_BGWORK_CLASS_" + id, null);
        if (workerClass == null) {
            return;
        }
        try {
            Class<?> cls = Class.forName(workerClass);
            BackgroundWorker worker = (BackgroundWorker) cls.newInstance();
            java.util.Map<String, String> input = new java.util.HashMap<String, String>();
            String enc = com.codename1.io.Preferences.get("$$CN1_BGWORK_INPUT_" + id, "");
            if (enc != null && enc.length() > 0) {
                for (String pair : com.codename1.io.Util.split(enc, "\u0002")) {
                    int idx = pair.indexOf('\u0001');
                    if (idx >= 0) {
                        input.put(pair.substring(0, idx), pair.substring(idx + 1));
                    }
                }
            }
            final boolean periodic = com.codename1.io.Preferences.get("$$CN1_BGWORK_PERIODIC_" + id, false);
            worker.performWork(id, input, System.currentTimeMillis() + 25000, new com.codename1.util.Callback<Boolean>() {
                public void onSucess(Boolean value) {
                    if (periodic) {
                        // resubmit to approximate periodic behavior on iOS
                        instance.nativeInstance.submitBackgroundProcessingTask(id, (System.currentTimeMillis() + 60000) / 1000.0, false, false);
                    }
                }
                public void onError(Object sender, Throwable err, int errorCode, String errorMessage) {
                    com.codename1.io.Log.e(err);
                }
            });
        } catch (Throwable t) {
            com.codename1.io.Log.e(t);
        }
    }

    @Override
    public void subscribeToPushTopic(String topic) {
        com.codename1.io.Log.p("Push topics are not supported on iOS APNs; topic '" + topic
                + "' must be handled server side");
    }

    @Override
    public void unsubscribeFromPushTopic(String topic) {
        com.codename1.io.Log.p("Push topics are not supported on iOS APNs; topic '" + topic
                + "' must be handled server side");
    }

    @Override
    public boolean isReceiveSharedContentSupported() {
        return true;
    }

    @Override
    public boolean isWalletExtensionSupported() {
        return nativeInstance.isWalletExtensionSupported();
    }

    @Override
    public void walletExtensionClearPassEntries(boolean remote) {
        nativeInstance.walletExtensionClearPassEntries(remote);
    }

    @Override
    public void walletExtensionAddPassEntry(boolean remote, String identifier, String title,
            String cardholderName, String accountSuffix, String network, String description, byte[] artPng) {
        if (identifier == null || identifier.length() == 0 || artPng == null || artPng.length == 0) {
            return;
        }
        nativeInstance.walletExtensionAddPassEntry(remote, identifier, title,
                cardholderName, accountSuffix, network, description, artPng);
    }

    @Override
    public void walletExtensionSetRequiresAuthentication(boolean requiresAuthentication) {
        nativeInstance.walletExtensionSetRequiresAuthentication(requiresAuthentication);
    }

    @Override
    public void walletExtensionSetAuthToken(String token) {
        nativeInstance.walletExtensionSetAuthToken(token);
    }

    @Override
    public void walletExtensionClear() {
        nativeInstance.walletExtensionClear();
    }

    /// Invoked from native (on app activation) with the JSON payload written by the share
    /// extension. Parses it into a SharedContent and dispatches to the app.
    public static void fireSharedContentFromNative(String json) {
        if (json == null || json.length() == 0) {
            return;
        }
        try {
            com.codename1.io.JSONParser parser = new com.codename1.io.JSONParser();
            java.util.Map parsed = parser.parseJSON(new java.io.StringReader(json));
            SharedContent.Builder b = SharedContent.builder();
            Object subject = parsed.get("subject");
            if (subject instanceof String) {
                b.subject((String) subject);
            }
            Object items = parsed.get("items");
            if (items instanceof java.util.List) {
                for (Object o : (java.util.List) items) {
                    if (!(o instanceof java.util.Map)) {
                        continue;
                    }
                    java.util.Map item = (java.util.Map) o;
                    String kind = (String) item.get("kind");
                    String value = (String) item.get("value");
                    if ("url".equals(kind)) {
                        b.addUrl(value);
                    } else if ("image".equals(kind)) {
                        b.addImage(null, value, null);
                    } else if ("file".equals(kind)) {
                        b.addFile(null, value, null);
                    } else {
                        b.addText(value);
                    }
                }
            }
            if (instance != null) {
                instance.fireSharedContentReceived(b.build());
            }
        } catch (Throwable t) {
            com.codename1.io.Log.e(t);
        }
    }

    
    static class ClipShape implements Shape {
        
        private final Rectangle rect = new Rectangle();
        private final GeneralPath p = new GeneralPath();
        private boolean isRect;
        private static ArrayList<ClipShape> pool = new ArrayList<ClipShape>();
        
        public static synchronized ClipShape create() {
            if (!pool.isEmpty()) {
                return pool.remove(pool.size()-1);
            }
            return new ClipShape();
        }
        
        public synchronized static void recycle(ClipShape shape) {
            if (pool.size() <= 20 && shape != null) {
                pool.add(shape);
            }
        }
        
        public boolean isRect() {
            return isRect;
        }
        
        public String toString() {
            if (isRect()) {
                return rect.toString();
            } else {
                return p.toString();
            }
        }
        
        
        @Override
        public PathIterator getPathIterator() {
            if (isRect) {
                return rect.getPathIterator();
            } else {
                return p.getPathIterator();
            }
        }

        @Override
        public PathIterator getPathIterator(Transform transform) {
            if (isRect) {
                return rect.getPathIterator(transform);
            } else {
                return p.getPathIterator(transform);
            }
        }

        @Override
        public Rectangle getBounds() {
            if (isRect) {
                return rect.getBounds();
            } else {
                return p.getBounds();
            }
        }
        
        
        public void getBounds(Rectangle r) {
            if (isRect) {
                r.setBounds(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
            } else {
                p.getBounds(r);
            }
        }

        @Override
        public float[] getBounds2D() {
            if (isRect) {
                return rect.getBounds2D();
            } else {
                return p.getBounds2D();
            }
        }
        
        public void getBounds2D(float[] out) {
            if (isRect) {
                out[0] = rect.getX();
                out[1] = rect.getY();
                out[2] = rect.getWidth();
                out[3] = rect.getHeight();
            } else {
                p.getBounds2D(out);
            }
        }

        @Override
        public boolean isRectangle() {
            if (isRect) {
                return true;
            } else {
                return p.isRectangle();
            }
        }

        @Override
        public boolean contains(int x, int y) {
            if (isRect) {
                return rect.contains(x, y);
            } else {
                return p.contains(x, y);
            }
        }

        @Override
        public Shape intersection(Rectangle rect) {
            if (isRect) {
                return this.rect.intersection(rect);
            } else {
                return this.p.intersection(rect);
            }
        }
        
        
        public boolean intersect(Rectangle r) {
            if (isRect) {
                rect.intersection(r, rect);
                return rect.getWidth() > 0 && rect.getHeight() > 0;
            } else {
                if (!p.intersect(r)) {
                    rect.setBounds(0,0,0,0);
                    isRect = true;
                    return false;
                } else {
                    if (p.isRectangle()) {
                        p.getBounds(rect);
                        isRect = true;
                    }
                    return true;
                }
            }
        }
        
        public boolean intersect(int x, int y, int w, int h) {
            if (isRect) {
                Rectangle.intersection(x, y, w, h, rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight(), rect);
                return rect.getWidth() >0 && rect.getHeight() > 0;
            } else {
                if (!p.intersect(x, y, w, h)) {
                    rect.setBounds(0,0,0,0);
                    isRect = true;
                    return false;
                } else {
                    if (p.isRectangle()) {
                        p.getBounds(rect);
                        isRect = true;
                    }
                    return true;
                }
            }
        }
        
        public void setBounds(int x, int y, int w, int h) {
            rect.setBounds(x, y, w, h);
            isRect = true;
        }
        
        public boolean equals(int x, int y, int w, int h) {
            return isRect &&
                    rect.getX() == x &&
                    rect.getY() == y &&
                    rect.getWidth() == w &&
                    rect.getHeight() == h;
        }
        
        
        public boolean equals(Shape s, Transform t) {
            if (t != null && !t.isIdentity()) {
                GeneralPath tmp = GeneralPath.createFromPool();
                try {
                    tmp.setShape(s, t);
                    return equals(tmp, null);
                } finally {
                    GeneralPath.recycle(tmp);
                }
            }
            
            // At this point we know that t is null or the identity
            if (s == this) {
                return true;
            }
            
            if (s instanceof ClipShape) {
                ClipShape cs = (ClipShape)s;
                return cs.isRect ? equals(cs.rect, t) : equals(cs.p, t);
            } else if (s instanceof Rectangle) {
                if (isRect) {
                    return rect.equals((Rectangle)s);
                } else {
                    return p.equals(s, (Transform) null);
                }
            } else if (s instanceof GeneralPath) {
                GeneralPath sPath = (GeneralPath)s;
                if (isRect) {
                    return sPath.equals(rect, (Transform)null);
                } else {
                    return sPath.equals(p, (Transform)null);
                }
            } else {
                GeneralPath p2 = GeneralPath.createFromPool();
                try {
                    p2.setShape(s, null);
                    return equals(p2, null);
                } finally {
                    GeneralPath.recycle(p2);
                }
            }
            
        }
        
        
        
        public void setShape(Shape s, Transform t) {
            if (s.isRectangle() && (t == null || t.isIdentity())) {
                if (s.getClass() == GeneralPath.class) {
                    ((GeneralPath)s).getBounds(rect);
                } else if (s.getClass() == Rectangle.class) {
                    Rectangle r = (Rectangle)s;
                    rect.setBounds(r.getX(), r.getY(), r.getWidth(), r.getHeight());
                } else {
                    Rectangle r = s.getBounds();
                    rect.setBounds(r.getX(), r.getY(), r.getWidth(), r.getHeight());
                }
                isRect = true;
            } else {
                p.setShape(s, t);
                if (p.isRectangle()) {
                    p.getBounds(rect);
                    isRect = true;
                } else {
                    isRect = false;
                }
            }
        }
        
        /**
        * Returns the number of path commands in this path.
        * @return The number of path commands in this path.
        */
        public int getTypesSize() {
            if (isRect) {
                p.setShape(rect, null);
                return p.getTypesSize();
            } else {
                return p.getTypesSize();
            }
        }

        /**
         * Returns the number of points in this path.
         * @return The number of points in this path.
         */
        public int getPointsSize() {
            if (isRect) {
                p.setShape(rect, null);
                
            }
            return p.getPointsSize();
        }

        /**
         * Returns a copy of the types (aka path commands) in this path.
         * @param out An array to copy the path commands into.
         */
        public void getTypes(byte[] out) {
            if (isRect) {
                p.setShape(rect, null);
            }
            p.getTypes(out);
        }

        /**
         * Returns a copy of the points in this path.
         * @param out An array to copy the points into.
         */
        public void getPoints(float[] out) {
            if (isRect) {
                p.setShape(rect, null);
            }
            p.getPoints(out);
        }
        
        public boolean isPolygon() {
            if (isRect) {
                return true;
            }
            return p.isPolygon();
        }
    }

    @Override
    public boolean isJailbrokenDevice() {
        Boolean b = canExecute("cydia://package/com.example.package");
        return b != null && b.booleanValue();
    }

    @Override
    public void announceForAccessibility(final Component cmp, final String text) {
        IOSNative.announceForAccessibility(text);
    }

    // ================================================================
    // Crypto bridge -- routes through CN1Crypto.{h,m} in nativeSources/
    // (the corresponding native methods live on IOSNative). The defaults
    // inherited from CodenameOneImplementation use java.security via
    // reflection, which isn't on the ParparVM runtime classpath.

    private static byte[] cryptoTrim(byte[] buf, int len) {
        if (len < 0) {
            throw new RuntimeException("crypto operation failed with code " + len);
        }
        if (len == buf.length) return buf;
        byte[] out = new byte[len];
        System.arraycopy(buf, 0, out, 0, len);
        return out;
    }

    @Override
    public void secureRandomBytes(byte[] out) {
        nativeInstance.secureRandomBytes(out);
    }

    @Override
    public byte[] aesEncrypt(String transformation, byte[] key, byte[] iv, byte[] aad, byte[] plaintext) {
        return doAes(transformation, key, iv, aad, plaintext, 1);
    }

    @Override
    public byte[] aesDecrypt(String transformation, byte[] key, byte[] iv, byte[] aad, byte[] ciphertext) {
        return doAes(transformation, key, iv, aad, ciphertext, 0);
    }

    private byte[] doAes(String transformation, byte[] key, byte[] iv, byte[] aad, byte[] input, int encrypt) {
        String t = transformation == null ? "" : transformation.toUpperCase();
        if (t.indexOf("GCM") >= 0) {
            // Encrypt output = ciphertext + 16-byte tag; decrypt output is
            // the same length as the ciphertext minus the tag.
            int outLen = encrypt == 1 ? input.length + 16 : Math.max(0, input.length - 16);
            byte[] outBuf = new byte[outLen];
            int written = nativeInstance.aesGcm(encrypt, key, iv, aad, input, outBuf);
            return cryptoTrim(outBuf, written);
        }
        boolean padded = t.indexOf("NOPADDING") < 0;
        // CBC ciphertext is at most input + one extra block (16 bytes).
        int outLen = input.length + 16;
        byte[] outBuf = new byte[outLen];
        int written = nativeInstance.aesCbc(encrypt, key, iv, input, outBuf, padded ? 1 : 0);
        return cryptoTrim(outBuf, written);
    }

    @Override
    public byte[] rsaEncrypt(String transformation, byte[] publicKeyX509, byte[] plaintext) {
        int padding = rsaPaddingKind(transformation);
        // Modern key sizes never exceed 2048 bytes of output.
        byte[] outBuf = new byte[2048];
        int written = nativeInstance.rsaEncrypt(padding, publicKeyX509, plaintext, outBuf);
        return cryptoTrim(outBuf, written);
    }

    @Override
    public byte[] rsaDecrypt(String transformation, byte[] privateKeyPkcs8, byte[] ciphertext) {
        int padding = rsaPaddingKind(transformation);
        byte[] outBuf = new byte[2048];
        int written = nativeInstance.rsaDecrypt(padding, privateKeyPkcs8, ciphertext, outBuf);
        return cryptoTrim(outBuf, written);
    }

    private static int rsaPaddingKind(String transformation) {
        if (transformation == null) return 1;
        return transformation.toUpperCase().indexOf("OAEP") >= 0 ? 2 : 1;
    }

    @Override
    public byte[] cryptoSign(String algorithm, String keyAlgorithm, byte[] privateKeyPkcs8, byte[] data) {
        int alg = signatureAlgorithmKind(algorithm);
        byte[] outBuf = new byte[2048];
        int written = nativeInstance.sign(alg, privateKeyPkcs8, data, outBuf);
        return cryptoTrim(outBuf, written);
    }

    @Override
    public boolean cryptoVerify(String algorithm, String keyAlgorithm, byte[] publicKeyX509, byte[] data, byte[] signature) {
        int alg = signatureAlgorithmKind(algorithm);
        int rc = nativeInstance.verify(alg, publicKeyX509, data, signature);
        if (rc < 0) throw new RuntimeException("verify failed: code " + rc);
        return rc == 1;
    }

    private static int signatureAlgorithmKind(String algorithm) {
        if ("SHA256withRSA".equals(algorithm)) return 0;
        if ("SHA384withRSA".equals(algorithm)) return 1;
        if ("SHA512withRSA".equals(algorithm)) return 2;
        if ("SHA256withECDSA".equals(algorithm)) return 3;
        if ("SHA384withECDSA".equals(algorithm)) return 4;
        if ("SHA512withECDSA".equals(algorithm)) return 5;
        throw new RuntimeException("unsupported signature algorithm: " + algorithm);
    }

    @Override
    public byte[][] generateRsaKeyPair(int bits) {
        // 4096-bit RSA produces ~600 bytes of DER for the public side and
        // ~2300 for the private; round up generously.
        byte[] pubBuf = new byte[bits + 1024];
        byte[] privBuf = new byte[bits * 3];
        int[] lens = new int[2];
        int rc = nativeInstance.generateRsaKeyPair(bits, pubBuf, privBuf, lens);
        if (rc < 0) throw new RuntimeException("RSA keypair generation failed: code " + rc);
        return new byte[][]{ cryptoTrim(pubBuf, lens[0]), cryptoTrim(privBuf, lens[1]) };
    }
}
