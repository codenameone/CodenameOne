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
import com.codename1.media.MediaManager;
import com.codename1.notifications.LocalNotification;
import com.codename1.notifications.LocalNotificationCallback;
import com.codename1.payment.RestoreCallback;
import com.codename1.ui.Accessor;
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
import com.codename1.util.Callback;
import com.codename1.util.StringUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;


/**
 *
 * @author Shai Almog
 */
public class IOSImplementation extends CodenameOneImplementation {
    public static IOSNative nativeInstance = new IOSNative();
    private static LocalNotificationCallback localNotificationCallback;
    private static PurchaseCallback purchaseCallback;
    private static RestoreCallback restoreCallback;
    private int timeout = 120000;
    private static final Object CONNECTIONS_LOCK = new Object();
    private Map<Long, NetworkConnection> connections = new HashMap<Long, NetworkConnection>();
    private NativeFont defaultFont;
    private NativeGraphics currentlyDrawingOn;
    //private NativeImage backBuffer;
    private NativeGraphics globalGraphics;
    static IOSImplementation instance;
    private TextArea currentEditing;
    private static boolean initialized;
    private Lifecycle life;
    private CodeScannerImpl scannerInstance;
    private static boolean minimized;
    private String userAgent;
    private TextureCache textureCache = new TextureCache();
    private static boolean dropEvents;
    
    private NativePathRenderer globalPathRenderer;
    private NativePathStroker globalPathStroker;
    
    private boolean isActive=false;
    private final ArrayList<Runnable> onActiveListeners = new ArrayList<Runnable>();
    private static BackgroundFetch backgroundFetchCallback;
    
    
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
        setUseNativeCookieStore(false);
        Display.getInstance().setTransitionYield(10);
        Display.getInstance().setDefaultVirtualKeyboard(null);
        callback = (Runnable)m;
        if(m instanceof Lifecycle) {
            life = (Lifecycle)m;
        }
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
        return nativeInstance.isTablet();
    }
    
    @Override
    public void addCookie(Cookie c) {
        if(isUseNativeCookieStore()) {
            nativeInstance.addCookie(c.getName(), c.getValue(), c.getDomain(), c.getPath(), c.isSecure(), c.isHttpOnly(), c.getExpires());
        } else {
            super.addCookie(c);
        }
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

    @Override
    public boolean isEditingText(Component c) {
        if(textEditorHidden) {
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
        foldKeyboard();
    }
    
    public static void foldKeyboard() {
        if(instance.isAsyncEditMode()) {
            final Component cmp = Display.getInstance().getCurrent().getFocused();
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
    
    @Override
    public int getInvisibleAreaUnderVKB() {
        if(isAsyncEditMode() && isEditingText()) {
            return nativeInstance.getVKBHeight();
        }
        return 0;
    }
    
    
    private static void updateNativeTextEditorFrame() {
        if (instance.currentEditing != null) {
            TextArea cmp = instance.currentEditing;
            final Style stl = cmp.getStyle();
            final boolean rtl = UIManager.getInstance().getLookAndFeel().isRTL();
            instance.doNotHideTextEditorSemaphore++;
            try {
                instance.currentEditing.requestFocus();
            } finally {
                instance.doNotHideTextEditorSemaphore--;
            }
            int x = cmp.getAbsoluteX() + cmp.getScrollX();
            int y = cmp.getAbsoluteY() + cmp.getScrollY();
            int w = cmp.getWidth();
            int h = cmp.getHeight();
            int pt = stl.getPaddingTop();
            int pb = stl.getPaddingBottom();
            int pl = stl.getPaddingLeft(rtl);
            int pr = stl.getPaddingRight(rtl);
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
            
            int maxH = Display.getInstance().getDisplayHeight() - nativeInstance.getVKBHeight();
            
            if (h > maxH ) {
                // For text areas, we don't want the keyboard to cover part of the 
                // typing region.  So we will try to size the component to 
                // to only go up to the top edge of the keyboard
                // that should allow the OS to enable scrolling properly.... at least
                // in theory.
                h = maxH;
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
    
    /**
     * Callback for native.  Called when keyboard is shown.  Used for async editing 
     * with formBottomPaddingEditingMode.
     */
    static void keyboardWillBeShown(){
        if(nativeInstance.isAsyncEditMode()) {
            // revalidate the parent since the size of form is now larger due to the vkb
            final Form current = Display.getInstance().getCurrent();
            //final Component currentEditingFinal = instance.currentEditing;
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
        
        if (Display.getInstance().getVirtualKeyboardListener() != null) {
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    ActionListener l = Display.getInstance().getVirtualKeyboardListener();
                    if (l != null) {
                        l.actionPerformed(new ActionEvent(true));
                    }
                }
            });
        }
    }
    
    /**
     * Callback for native.  Called when keyboard is hidden.  Used for async editing 
     * with formBottomPaddingEditingMode.
     */
    static void keyboardWillBeHidden(){
        Display.getInstance().callSerially(new Runnable(){

            @Override
            public void run() {
                Form current = Display.getInstance().getCurrent();
                if (current != null) {
                    current.revalidate();
                }
            }
            
        });
        if (Display.getInstance().getVirtualKeyboardListener() != null) {
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    ActionListener l = Display.getInstance().getVirtualKeyboardListener();
                    if (l != null) {
                        l.actionPerformed(new ActionEvent(false));
                    }
                }
            });
        }
    }
    
    public void setCurrentForm(Form f) {
        super.setCurrentForm(f);
        if(isAsyncEditMode() && isEditingText()) {
            foldKeyboard();
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
                        Display.getInstance().editString(cmp, maxSize, constraint, text, i);
                    }
                });
                return;
            }
            
           if(!cmp.hasFocus()) {
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
                    if(p.isScrollableY()  && p.getAbsoluteY() < keyboardClippingThresholdY) {
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
                    String hint = null;
                    if(currentEditing != null && currentEditing.getUIManager().isThemeConstant("nativeHintBool", true) && currentEditing.getHint() != null) {
                        hint = currentEditing.getHint();
                    }
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
                    if ( currentEditing != null ){
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
                                pr, hint, showToolbar);
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
                cmp.getComponentForm().revalidate();
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
                            if(editNext && instance.currentEditing != null && instance.currentEditing instanceof TextField) {
                                ((TextField)instance.currentEditing).fireDoneEvent();
                            }
                            instance.currentEditing = null;
                            instance.callHideTextEditor();
                            if (nativeInstance.isAsyncEditMode()) {
                                nativeInstance.setNativeEditingComponentVisible(false);
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
    
    public void releaseImage(Object image) {
        if(image instanceof NativeImage) {
            ((NativeImage)image).deleteImage();
        }
    }

    //private Graphics frontGraphics;
    private Image frontGraphicsMutableImage;
    
    
    @Override
    public Graphics getFrontGraphics() {
        Display d = Display.getInstance();
        if (frontGraphicsMutableImage == null || frontGraphicsMutableImage.getWidth() != d.getDisplayWidth()|| frontGraphicsMutableImage.getHeight() != d.getDisplayHeight()) {
            frontGraphicsMutableImage = Image.createImage(d.getDisplayWidth(), d.getDisplayHeight(), 0x0);
        }
        
        return frontGraphicsMutableImage.getGraphics();
    }

    
    @Override
    public void flushFrontGraphics(int x, int y, int width, int height) {
        if (frontGraphicsMutableImage != null) {
            globalGraphics.checkControl();
            NativeImage nm = (NativeImage)frontGraphicsMutableImage.getImage();
            nativeInstance.drawTopLayer(nm.peer, x, y, width, height);
        }
    }
    
    public void clearFrontGraphics() {
        frontGraphicsMutableImage = null;
    }

    private boolean frontGraphicsVisible;
    
    @Override
    public void setFrontGraphicsVisible(boolean visible) {
        if (visible != frontGraphicsVisible) {
            frontGraphicsVisible = visible;
            if (visible) {
                nativeInstance.showFrontGraphics();
            } else {
                nativeInstance.hideFrontGraphics();
            }
        }
    }
    
    @Override
    public boolean isFrontGraphicsSupported() {
        return true;
    }
    
    public void flushGraphics(int x, int y, int width, int height) {
        /*if(currentlyDrawingOn != null && backBuffer == currentlyDrawingOn.associatedImage) {
            backBuffer.peer = finishDrawingOnImage();
            flushBuffer(backBuffer.peer, x, y, width, height);
            startDrawingOnImage(backBuffer.width, backBuffer.height, backBuffer.peer);
        } else {
            flushBuffer(backBuffer.peer, x, y, width, height);
        }*/
        Graphics g = getCodenameOneGraphics();
        if (Accessor.isFrontGraphicsEnabled(g)) {
            
            Accessor.flush(g, x, y, width, height);
        }
        globalGraphics.clipApplied = false;
        flushBuffer(0, x, y, width, height);

        /*if(Display.getInstance().isEdt()) {
            Object lock = getDisplayLock();
            int count = 3;
            while(!isPainted()) {
                synchronized(lock) {
                    try {
                        lock.wait(10);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
                if(count == 0) {
                    break;
                }
                count--;
            }
        }*/
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

    static void sizeChangedImpl(int w, int h) {
        instance.sizeChanged(w, h);
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
            ns = IOSImplementation.nativeInstance.createNSData(path);
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
            
            if(iosMode.equals("modern")) {
                r = Resources.open("/iOS7Theme.res");
                Hashtable tp = r.getTheme(r.getThemeResourceNames()[0]);
                if(!nativeInstance.isIOS7()) {
                    tp.put("TitleArea.padding", "0,0,0,0");
                }
                UIManager.getInstance().setThemeProps(tp);
                return;
            }
            if(iosMode.equals("auto")) {
                if(nativeInstance.isIOS7()) {
                    r = Resources.open("/iOS7Theme.res");
                } else {
                    r = Resources.open("/iPhoneTheme.res");
                }
                UIManager.getInstance().setThemeProps(r.getTheme(r.getThemeResourceNames()[0]));
                return;
            }
            r = Resources.open("/iPhoneTheme.res");
            UIManager.getInstance().setThemeProps(r.getTheme(r.getThemeResourceNames()[0]));
        } catch (IOException ex) {
            ex.printStackTrace();
        }        
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
        } else if (shape.isPolygon()) {
            int pointsSize = shape.getPointsSize();
            if (polygonPointsBuffer == null || polygonPointsBuffer.length < pointsSize) {
                polygonPointsBuffer = new float[pointsSize];
            }
            shapeToPolygon(shape, polygonPointsBuffer);
            nativeInstance.setNativeClippingPolygonGlobal(polygonPointsBuffer);
        } else {
            
            TextureAlphaMask mask = (TextureAlphaMask)textureCache.get(shape, null);
            if ( mask == null ){
                mask = (TextureAlphaMask)this.createAlphaMask(shape, null);
                textureCache.add(shape, null, mask);
            }
            
           if ( mask != null ){
               //Log.p("Setting native clipping mask global with bounds "+mask.getBounds()+" : "+shape);
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
        ((Matrix)nativeTransform).transformCoord(in, out);
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

    private static void nativeDrawRoundRectMutable(int color, int alpha, int x, int y, int width, int height, int arcWidth, int arcHeight) {
        nativeInstance.nativeDrawRoundRectMutable(color, alpha, x, y, width, height, arcWidth, arcHeight);
    }
    private static void nativeDrawRoundRectGlobal(int color, int alpha, int x, int y, int width, int height, int arcWidth, int arcHeight) {
        nativeInstance.nativeDrawRoundRectGlobal(color, alpha, x, y, width, height, arcWidth, arcHeight);        
    }


    public void fillRoundRect(Object graphics, int x, int y, int width, int height, int arcWidth, int arcHeight) {
        NativeGraphics ng = (NativeGraphics)graphics;
        ng.checkControl();
        ng.applyTransform();
        ng.applyClip();
        ng.nativeFillRoundRect(ng.color, ng.alpha, x, y, width, height, arcWidth, arcHeight);
    }

    private static void nativeFillRoundRectMutable(int color, int alpha, int x, int y, int width, int height, int arcWidth, int arcHeight) {
        nativeInstance.nativeFillRoundRectMutable(color, alpha, x, y, width, height, arcWidth, arcHeight);
    }
    private static void nativeFillRoundRectGlobal(int color, int alpha, int x, int y, int width, int height, int arcWidth, int arcHeight) {
        nativeInstance.nativeFillRoundRectGlobal(color, alpha, x, y, width, height, arcWidth, arcHeight);
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
    
    

    private static void nativeFillArcMutable(int color, int alpha, int x, int y, int width, int height, int startAngle, int arcAngle) {
        nativeInstance.nativeFillArcMutable(color, alpha, x, y, width, height, startAngle, arcAngle);
    }
    private static void nativeDrawArcMutable(int color, int alpha, int x, int y, int width, int height, int startAngle, int arcAngle) {
        nativeInstance.nativeDrawArcMutable(color, alpha, x, y, width, height, startAngle, arcAngle);
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

    public void drawString(Object graphics, String str, int x, int y) {
        NativeGraphics ng = (NativeGraphics)graphics;
        ng.checkControl();
        ng.applyTransform();
        ng.applyClip();
        NativeFont fnt = ng.getFont();
        int l = str.length();
        int max = fnt.getMaxStringLength();
        if(l > max) {
            // really long string split it and draw multiple strings to avoid texture overload
            int one = 1;
            if(l % max == 0) {
                one = 0;
            }
            int stringCount = l / max + one;
            for(int iter = 0 ; iter < stringCount ; iter++) {
                int pos = iter * max;
                String s = str.substring(pos, Math.min(pos + max, str.length()));
                ng.nativeDrawString(ng.color, ng.alpha, fnt.peer, s, x, y);
                x += stringWidth(fnt, s);
            }
        } else {
            ng.nativeDrawString(ng.color, ng.alpha, fnt.peer, str, x, y);
        }
    }

    public void tileImage(Object graphics, Object img, int x, int y, int w, int h) {
        NativeGraphics ng = (NativeGraphics)graphics;
        if(ng instanceof GlobalGraphics) {
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
        NativeGraphics ng = (NativeGraphics)graphics;
        //System.out.println("Drawing image " + img);
        ng.checkControl();
        ng.applyTransform();
        ng.applyClip();
        NativeImage nm = (NativeImage)img;
        ng.nativeDrawImage(nm.peer, ng.alpha, x, y, nm.width, nm.height);
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
    public boolean transformEqualsImpl(Transform t1, Transform t2) {
        if ( t1 != null ){
            Matrix m1 = (Matrix)t1.getNativeTransform();
            Matrix m2 = (Matrix)t2.getNativeTransform();
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
        Map<String, Object> textures = new HashMap<String,Object>();
        
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
            String shapeID = getShapeID(s, stroke);
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
            String shapeID = getShapeID(s, stroke);
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
        String getShapeID(Shape shape, Stroke stroke){
            float[] bounds = shape.getBounds2D();
            float x = bounds[0];
            float y = bounds[1];
            StringBuilder sb = new StringBuilder();
            PathIterator it = shape.getPathIterator();
            float[] buf = new float[6];
            float tx, ty, tx2, ty2, tx3, ty3;
            if ( stroke != null ){
                sb.append(stroke.hashCode()).append(":");
            }
            sb.append(it.getWindingRule());
            sb.append(";");
            while ( !it.isDone() ){
                int type = it.currentSegment(buf);
                
                switch ( type ){
                    case PathIterator.SEG_MOVETO:
                       tx = buf[0]-x;
                       ty = buf[1]-y;
                        sb.append("M:").append((int)tx).append(",").append((int)ty);
                        break;
                    case PathIterator.SEG_LINETO:
                       tx = buf[0]-x;
                       ty = buf[1]-y;
                        sb.append("L:").append((int)tx).append(",").append((int)ty);
                        break;
                    case PathIterator.SEG_QUADTO:
                        tx = buf[0]-x;
                        ty = buf[1]-y;
                        tx2 = buf[2]-x;
                        ty2 = buf[3]-y;
                        sb.append("Q:").append((int)tx).append(",").append((int)ty).append(",").append((int)tx2).append(",").append((int)ty2);
                        break;
                    case PathIterator.SEG_CUBICTO:
                        tx = buf[0]-x;
                        ty = buf[1]-y;
                        tx2 = buf[2]-x;
                        ty2 = buf[3]-y;
                        tx3 = buf[4]-x;
                        ty3= buf[5]-y;
                        sb.append("C:").append((int)tx).append(",").append((int)ty).append(",").append((int)tx2).append(",").append((int)ty2)
                                .append(",").append((int)tx3).append(",").append((int)ty3);
                        break;
                        
                    case PathIterator.SEG_CLOSE:
                        sb.append(".");
                        
                }
                it.next();
            }
            return sb.toString();
            
        }
    }
    
    
    // END SHAPES AND TRANSFORMATION CODE
    
    private void nativeDrawImageMutable(long peer, int alpha, int x, int y, int width, int height) {
        nativeInstance.nativeDrawImageMutable(peer, alpha, x, y, width, height);
    }
    private void nativeDrawImageGlobal(long peer, int alpha, int x, int y, int width, int height) {
        nativeInstance.nativeDrawImageGlobal(peer, alpha, x, y, width, height);
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
            nativeInstance.addGeofencing(peer, gf.getLoc().getLatitude(), gf.getLoc().getLongitude(), gf.getRadius(), gf.getExpiration(), gf.getId());
            super.addGeoFencing(GeofenceListenerClass, gf); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void removeGeoFencing(String id) {
            geofenceListeners().remove(id);
            geofenceExpirations().remove(id);
            synchronizeGeofenceListeners();
            synchronizeGeofenceExpirations();
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
    
    public LocationManager getLocationManager() {
        if(lm == null) {
            lm = new Loc();
        }
        return lm;
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
                if(r.startsWith("file:")) {
                    captureCallback.fireActionEvent(new ActionEvent(r));
                } else {
                    captureCallback.fireActionEvent(new ActionEvent("file:" + r));
                }
            } else {
                captureCallback.fireActionEvent(null);
            }
            captureCallback = null;
        }
    }
    
    
    public void captureAudio(ActionListener response) {
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
        captureCallback = new EventDispatcher();
        captureCallback.addListener(response);
        nativeInstance.captureCamera(false);
        dropEvents = true;
    }

    @Override
    public String [] getAvailableRecordingMimeTypes() {
        return super.getAvailableRecordingMimeTypes();
    }
    
    @Override
    public Media createMediaRecorder(String path, String mimeType) throws IOException{
        final long[] peer = new long[] { nativeInstance.createAudioRecorder(path) };
        return new Media() {
            private boolean playing;
            @Override
            public void play() {
                if(peer[0] != 0) {
                    nativeInstance.startAudioRecord(peer[0]);
                    playing = true;
                }
            }

            @Override
            public void pause() {
                if(peer[0] != 0) {
                    nativeInstance.pauseAudioRecord(peer[0]);
                    playing = false;
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
        captureCallback = new EventDispatcher();
        captureCallback.addListener(response);
        nativeInstance.captureCamera(true);
        dropEvents = true;
    }

    @Override
    public void openImageGallery(ActionListener response) {    
        openGallery(response, Display.GALLERY_IMAGE);
    }

    @Override
    public void openGallery(ActionListener response, int type) {
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
    
    class IOSMedia implements Media {
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
        
        public IOSMedia(String uri, boolean isVideo, Runnable onCompletion) {
            this.uri = uri;
            this.isVideo = isVideo;
            this.onCompletionCallbackId = registerMediaCallback(onCompletion);
            if(!isVideo) {
                moviePlayerPeer = nativeInstance.createAudio(uri, onCompletion);
            }
        }

        public IOSMedia(InputStream stream, String mimeType, Runnable onCompletion) {
            this.stream = stream;
            this.mimeType = mimeType;
            this.onCompletionCallbackId = registerMediaCallback(onCompletion);            
            isVideo = mimeType.indexOf("video") > -1;
            if(!isVideo) {
                try {
                    moviePlayerPeer = nativeInstance.createAudio(Util.readInputStream(stream), onCompletion);
                    nativeInstance.retainPeer(moviePlayerPeer);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }

        @Override
        public void play() {
            if(isVideo) {
                if(nativePlayer) {
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
                            ex.printStackTrace();
                        }
                    }
                    nativeInstance.showNativePlayerController(moviePlayerPeer);
                    return;
                }
                if(moviePlayerPeer != 0) {
                    nativeInstance.startVideoComponent(moviePlayerPeer);
                }
            } else {
                nativeInstance.playAudio(moviePlayerPeer);                
            }
        }

        @Override
        public void pause() {
            if(moviePlayerPeer != 0) {
                if(isVideo) {
                    nativeInstance.pauseVideoComponent(moviePlayerPeer);
                } else {
                    nativeInstance.pauseAudio(moviePlayerPeer);
                }
            }
        }

        public void prepare() {
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
                    component = PeerComponent.create(new long[] { nativeInstance.getVideoViewPeer(moviePlayerPeer) });
                } else {
                    try {
                        byte[] data = toByteArray(stream);
                        Util.cleanup(stream);
                        moviePlayerPeer = nativeInstance.createVideoComponent(data, onCompletionCallbackId);
                        component = PeerComponent.create(new long[] { nativeInstance.getVideoViewPeer(moviePlayerPeer) });
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        return new Label("Error loading video " + ex);
                    }
                }
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


    public Media createMedia(InputStream stream, String mimeType, Runnable onCompletion) throws IOException {
        return new IOSMedia(stream, mimeType, onCompletion);
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
        
        
        void setClip(Shape newClip) {
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
                int commandsLen = shape.getTypesSize();
                int pointsLen = shape.getPointsSize();
                byte[] commandsArr = getTmpNativeDrawShape_commands(commandsLen);
                float[] pointsArr = getTmpNativeDrawShape_coords(pointsLen);
                shape.getTypes(commandsArr);
                shape.getPoints(pointsArr);
                nativeInstance.setNativeClippingMutable(commandsLen, commandsArr, pointsLen, pointsArr);
            }
        }

        void nativeDrawLine(int color, int alpha, int x1, int y1, int x2, int y2) {
            nativeDrawLineMutable(color, alpha, x1, y1, x2, y2);
        }

        void nativeFillRect(int color, int alpha, int x, int y, int width, int height) {
            nativeFillRectMutable(color, alpha, x, y, width, height);
        }

        void nativeDrawRect(int color, int alpha, int x, int y, int width, int height) {
            nativeDrawRectMutable(color, alpha, x, y, width, height);
        }

        void nativeDrawRoundRect(int color, int alpha, int x, int y, int width, int height, int arcWidth, int arcHeight) {
            nativeDrawRoundRectMutable(color, alpha, x, y, width, height, arcWidth, arcHeight);
        }

        void nativeFillRoundRect(int color, int alpha, int x, int y, int width, int height, int arcWidth, int arcHeight) {
            nativeFillRoundRectMutable(color, alpha, x, y, width, height, arcWidth, arcHeight);
        }

        void nativeDrawArc(int color, int alpha, int x, int y, int width, int height, int startAngle, int arcAngle) {
            nativeDrawArcMutable(color, alpha, x, y, width, height, startAngle, arcAngle);
        }

        void nativeFillArc(int color, int alpha, int x, int y, int width, int height, int startAngle, int arcAngle) {
            nativeFillArcMutable(color, alpha, x, y, width, height, startAngle, arcAngle);
        }

        void nativeDrawString(int color, int alpha, long fontPeer, String str, int x, int y) {
            nativeDrawStringMutable(color, alpha, fontPeer, str, x, y);
        }

        void nativeDrawImage(long peer, int alpha, int x, int y, int width, int height) {
            nativeDrawImageMutable(peer, alpha, x, y, width, height);
        }
        
        
        
        //----------------------------------------------------------------------
        // BEGIN DRAW SHAPE METHODS
        
        void nativeDrawAlphaMask(TextureAlphaMask mask){
            
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
        void nativeDrawShape(Shape shape, Stroke stroke){//float lineWidth, int capStyle, int miterStyle, float miterLimit){
            if (shape.getClass() == GeneralPath.class) {
                // GeneralPath gives us some easy access to the points
                GeneralPath p = (GeneralPath)shape;
                int commandsLen = p.getTypesSize();
                int pointsLen = p.getPointsSize();
                byte[] commandsArr = getTmpNativeDrawShape_commands(commandsLen);
                float[] pointsArr = getTmpNativeDrawShape_coords(pointsLen);
                p.getTypes(commandsArr);
                p.getPoints(pointsArr);
                
                nativeInstance.nativeDrawShapeMutable(color, alpha, commandsLen, commandsArr, pointsLen, pointsArr, stroke.getLineWidth(), stroke.getCapStyle(), stroke.getJoinStyle(), stroke.getMiterLimit());
            } else {
                Log.p("Drawing shapes that are not GeneralPath objects is not yet supported on mutable images.");
            }
            
            
        }
        
        /**
         * Fills a shape in the graphics context.
         * @param shape
         */
        void nativeFillShape(Shape shape) {
            if (shape.getClass() == GeneralPath.class) {
                // GeneralPath gives us some easy access to the points
                GeneralPath p = (GeneralPath)shape;
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
        
        boolean isTransformSupported(){
            return true;
        }
        
        boolean isPerspectiveTransformSupported(){
            return false;
        }
        
        boolean isShapeSupported(){
            return true;
        }
        
        boolean isAlphaMaskSupported(){
            return false;
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
    }

    class GlobalGraphics extends NativeGraphics {
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
            System.out.println("nativeClearRect() not yet supported in Global Graphics");
            // We don't support this yet in the global graphics.
            // we only added it for 
        }
        
        

        void nativeDrawRect(int color, int alpha, int x, int y, int width, int height) {
            nativeDrawRectGlobal(color, alpha, x, y, width, height);
        }

        void nativeDrawRoundRect(int color, int alpha, int x, int y, int width, int height, int arcWidth, int arcHeight) {
            nativeDrawRoundRectGlobal(color, alpha, x, y, width, height, arcWidth, arcHeight);
        }

        void nativeFillRoundRect(int color, int alpha, int x, int y, int width, int height, int arcWidth, int arcHeight) {
            nativeFillRoundRectGlobal(color, alpha, x, y, width, height, arcWidth, arcHeight);
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
            drawingArcPath.moveTo(x + width / 2, y + height / 2);
            drawingArcPath.arc(x, y, width, height, startAngle * Math.PI / 180, arcAngle * Math.PI / 180, true);
            drawingArcPath.closePath();
            nativeFillShape(drawingArcPath);
        }

        void nativeDrawString(int color, int alpha, long fontPeer, String str, int x, int y) {
            nativeDrawStringGlobal(color, alpha, fontPeer, str, x, y);
        }

        void nativeDrawImage(long peer, int alpha, int x, int y, int width, int height) {
            nativeDrawImageGlobal(peer, alpha, x, y, width, height);
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
                    GeneralPath p = (GeneralPath)shape;
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
                    // If the shape is very small and would be scaled dramatically
                    // by the transform, then we will want to rasterize the shape in a larger
                    // size to prevent the OGL transform from making the path too blurry.
                    // But we can't just apply the full transform because the renderer
                    // won't render the stroke correctly with transform
                    // So we need to factor the transformation matrix
                    Rectangle origBounds = reusableRect;
                    Rectangle transformedBounds = tmpRect2;
                    p.getBounds(origBounds);
                    tmpDrawShape.setShape(shape, transform);
                    tmpDrawShape.getBounds(transformedBounds);
                    
                    double h1 = Math.sqrt(origBounds.getWidth() * origBounds.getWidth() + origBounds.getHeight() * origBounds.getHeight());
                    double h2 = Math.sqrt(transformedBounds.getWidth() * transformedBounds.getWidth() + transformedBounds.getHeight() * transformedBounds.getHeight());
                    if (h2 < 1) h2 = 1;
                    if (h1 < 1) h1 = 1;
                    
                    
                    float scale = (float)(h2/h1);
                    tmpTransform.setScale(scale, scale);
                    tmpDrawShape.setShape(shape, tmpTransform);

                    tmpTransform.setTransform(transform);
                    tmpTransform.scale(1/scale, 1/scale);

                    tmpTransform2.setTransform(transform);
                    try {
                        this.setTransform(tmpTransform);
                        if (stroke != null) {

                            tmpDrawStroke.setStroke(stroke);
                            tmpDrawStroke.setLineWidth(tmpDrawStroke.getLineWidth() * scale);
                        }
                        //applyTransform();
                        TextureAlphaMask mask = textureCache.get(tmpDrawShape, stroke==null?null:tmpDrawStroke);
                        if ( mask == null ){
                            mask = (TextureAlphaMask)createAlphaMask(tmpDrawShape, stroke==null?null:tmpDrawStroke);
                            textureCache.add(tmpDrawShape, stroke==null?null:tmpDrawStroke, mask);

                        }
                        if (mask==null){
                            // A null mask generally means the shape had zero bounds
                            return;
                        }
                        //mask = (TextureAlphaMask)createAlphaMask(shape, stroke);
                        if (paint != null && paint instanceof RadialGradient) {
                            RadialGradient rgp = (RadialGradient)paint;
                            rgp.x *= scale;
                            rgp.y *= scale;
                            rgp.width *= scale;
                            rgp.height *= scale;
                            applyPaint();
                        }
                        nativeDrawAlphaMask(mask);
                    } finally {
                        setTransform(tmpTransform2);
                        //applyTransform();
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
            NativeFont f = (NativeFont)o;
            if(name != null) {
                return f.name != null && f.name.equals(name) && f.weight == weight && f.height == height;
            }
            return f.name == null && f.style == style && f.face == face && f.size == size;
        }
        
        public int hashCode() {
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
    
    class NativeAlphaMask {
        long peer;
        String debugText;
        public NativeAlphaMask(String debugText){
            this.debugText = debugText;
        }
        
        
        void deleteTexture(){
            if ( peer != 0 ){
                nativeDeleteTexture(peer);
            }
        }
        
        protected void finalize(){
            deleteTexture();
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
    
    @Override
    public String browserExecuteAndReturnString(final PeerComponent browserPeer, final String javaScript) {
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
        if(dDensity == -1) {
            if(Display.getInstance().getProperty("ios.densityOld", "false").equals("true")) {
                dDensity = super.getDeviceDensity();
                return dDensity;
            }
            int dispWidth = getDisplayWidth();
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
                int largest = Math.max(dispWidth, getDisplayHeight());
                if(largest > 2000) {
                    dDensity = Display.DENSITY_HD;
                    return dDensity;
                }
                dDensity = Display.DENSITY_VERY_HIGH;
                return dDensity;
            }
        }
        return dDensity;
    }
    
    double ppi = 0;
    
    @Override
    public int convertToPixels(int dipCount, boolean horizontal) {
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
                    int largest = Math.max(dispWidth, getDisplayHeight());
                    if(largest > 2000) {
                        // iphone 6 plus
                        ppi = 19.25429416;                    
                    } else {
                        if(largest > 1300) {
                            // iphone 6
                            ppi = 12.8369704749679;                    
                        } else {
                            ppi = 12.8369704749679;                    
                        }
                    }
                }
            }
        }
        return (int)Math.round((((float)dipCount) * ppi));
    }

    @Override
    public void copyToClipboard(Object obj) {
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
        long browserPeer = nativeInstance.createBrowserComponent(browserComponent);
        PeerComponent pc = createNativePeer(new long[] {browserPeer});
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
        if(nativeInstance.canExecute(url)) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }
    
    @Override
    public void execute(String url) {
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

    @Override
    public Object getPasteDataFromClipboard() {
        // TODO
        return super.getPasteDataFromClipboard();
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
                userAgent = nativeInstance.getUserAgentString();
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
        if(key.equalsIgnoreCase("UDID")) {
            return nativeInstance.getUDID();
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
        // TODO
        return super.isAntiAliased(graphics);
    }

    @Override
    public boolean isAntiAliasedText(Object graphics) {
        // TODO
        return super.isAntiAliasedText(graphics);
    }

    @Override
    public boolean isAntiAliasedTextSupported() {
        return true;
    }

    @Override
    public boolean isAntiAliasingSupported() {
        return true;
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
        return nativeInstance.createContact(firstName, surname, officePhone, homePhone, cellPhone, email);
    }

    @Override
    public boolean deleteContact(String id) {
        return nativeInstance.deleteContact(Integer.parseInt(id));
    }
    
    
    @Override
    public String[] getAllContacts(boolean withNumbers) {
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
        nativeInstance.refreshContacts();
    }

    @Override
    public String[] getLinkedContactIds(Contact c) {
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
        int recId = Integer.parseInt(id);
        Contact c = new Contact();
        c.setId(id);
        c.setAddresses(new Hashtable());
        if (includeAddress) {
            // This is a hack to make sure that 
            // Address and its methods aren't stripped out by the BytecodeCompiler
            Address tmp = new Address();
        }

        c.setEmails(new Hashtable());
        c.setPhoneNumbers(new Hashtable());
        nativeInstance.updatePersonWithRecordID(recId, c, includesFullName, includesPicture, includesNumbers, includesEmail, includeAddress);
        return c;
    }

    @Override
    public Contact getContactById(String id) {
        return getContactById(id, true, true, true, true, true);
        
        /*c.setId("" + id);
        long person = nativeInstance.getPersonWithRecordID(recId);
        String fname = nativeInstance.getPersonFirstName(person);
        c.setFirstName(fname);
        String sname = nativeInstance.getPersonSurnameName(person);
        c.setFamilyName(sname);
        if(c.getFirstName() != null) {
            StringBuilder s = new StringBuilder();
            if(fname != null && fname.length() > 0) {
                if(sname != null && sname.length() > 0) {
                    c.setDisplayName(fname + " " + sname);
                } else {
                    c.setDisplayName(fname);
                }
            } else {
                if(sname != null && sname.length() > 0) {
                    c.setDisplayName(sname);
                }
            }
        }
        c.setPrimaryEmail(nativeInstance.getPersonEmail(person));
        
        int phones = nativeInstance.getPersonPhoneCount(person);
        Hashtable h = new Hashtable();
        for(int iter = 0 ; iter < phones ; iter++) {
            String t = nativeInstance.getPersonPhoneType(person, iter);
            if(t == null) {
                t = "work";
            }
            String phone = nativeInstance.getPersonPhone(person, iter);
            if(phone != null) {
                h.put(t, phone);
            }
        }
        c.setPhoneNumbers(h);
        
        c.setPrimaryPhoneNumber(nativeInstance.getPersonPrimaryPhone(person));
        
        //h = new Hashtable();
        //h.put("Work", h);
        c.setAddresses(h);
        nativeInstance.releasePeer(person);
        return c;*/
    }
    
    @Override
    public void dial(String phoneNumber) {        
        nativeInstance.dial("tel://" + phoneNumber);
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
        // TODO
        super.setAntiAliased(graphics, a);
    }

    @Override
    public void setAntiAliasedText(Object graphics, boolean a) {
        // TODO
        super.setAntiAliasedText(graphics, a);
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
        if(Display.getInstance().getProperty("ios.zpeer", null) != null) {
            return new ZNativeIPhoneView(nativeComponent);
        }
        return new NativeIPhoneView(nativeComponent);
    }

    class ZNativeIPhoneView extends PeerComponent {
        private long[] nativePeer;
        private boolean lightweightMode; 
       
        public ZNativeIPhoneView(Object nativePeer) {
            super(nativePeer);
            this.nativePeer = (long[])nativePeer;
            nativeInstance.retainPeer(this.nativePeer[0]);
            nativeInstance.peerInitialized(this.nativePeer[0], getAbsoluteX(), getAbsoluteY(), getWidth(), getHeight());
        }
        
        public void finalize() {
            if(nativePeer != null && nativePeer[0] != 0) {
                nativeInstance.releasePeer(nativePeer[0]);            
                nativePeer = null;
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
            int[] p = widthHeight;
            nativeInstance.calcPreferredSize(nativePeer[0], getDisplayWidth(), getDisplayHeight(), p);
            return new Dimension(p[0], p[1]);
        }

        
        protected void setLightweightMode(boolean l) {
            /*if(nativePeer != null && nativePeer[0] != 0) {
                if(lightweightMode != l) {
                    lightweightMode = l;
                    nativeInstance.peerSetVisible(nativePeer[0], !lightweightMode);
                    getComponentForm().repaint();
                }
            }*/
        }
        
        protected Image generatePeerImage() {
            int[] wh = widthHeight;
            long imagePeer = nativeInstance.createPeerImage(this.nativePeer[0], wh);
            if(imagePeer == 0) {
                return null;
            }
            NativeImage ni = new NativeImage("PeerScreen");
            ni.peer = imagePeer;
            ni.width = wh[0];
            ni.height = wh[1];
            return Image.createImage(ni);
        }

        @Override
        public void paint(Graphics g) {
            if(nativePeer != null && nativePeer[0] != 0) {
                nativeInstance.updatePeerPositionSize(nativePeer[0], getAbsoluteX(), getAbsoluteY(), getWidth(), getHeight());
            }
        }
        
        
        
        protected boolean shouldRenderPeerImage() {
            return false;
        }
    }
    
    
    class NativeIPhoneView extends PeerComponent {
        private long[] nativePeer;
        private boolean lightweightMode; 
       
        public NativeIPhoneView(Object nativePeer) {
            super(nativePeer);
            this.nativePeer = (long[])nativePeer;
            nativeInstance.retainPeer(this.nativePeer[0]);
        }
        
        public void finalize() {
            if(nativePeer != null && nativePeer[0] != 0) {
                nativeInstance.releasePeer(nativePeer[0]);            
                nativePeer = null;
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
            int[] p = widthHeight;
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
                setPeerImage(generatePeerImage());
                nativeInstance.peerDeinitialized(nativePeer[0]);
            }
        }
        
        protected void setLightweightMode(boolean l) {
            if(nativePeer != null && nativePeer[0] != 0) {
                if(lightweightMode != l) {
                    lightweightMode = l;
                    nativeInstance.peerSetVisible(nativePeer[0], !lightweightMode);
                    getComponentForm().repaint();
                }
            }
        }
        
        protected Image generatePeerImage() {
            int[] wh = widthHeight;
            long imagePeer = nativeInstance.createPeerImage(this.nativePeer[0], wh);
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
    
    public static void appendData(long peer, byte[] data) {
        NetworkConnection n;
        synchronized(CONNECTIONS_LOCK) {
            n = instance.connections.get(peer);
        }
        if(n != null) {
            synchronized(n.LOCK) {
                n.appendData(data);
                n.connected = true;
                n.LOCK.notifyAll();
            }
        }
    }
    
    public static void streamComplete(long peer) {
        NetworkConnection n;
        synchronized(CONNECTIONS_LOCK) {
            n = instance.connections.get(peer);
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
        NetworkConnection n;
        synchronized(CONNECTIONS_LOCK) {
            n = instance.connections.get(peer);
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
        private long peer;
        private FileBackedOutputStream body;
        private Vector pendingData = new Vector();
        private boolean completed;
        private Hashtable headers = new Hashtable();
        private boolean connected;
        private boolean ensureConnectionLock;
        String error;
        public final Object LOCK = new Object();
        
        
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
                instance.connections.put(peer, this);
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
        
        @Override
        public int read() throws IOException {
            synchronized(LOCK) {
                if(pendingData.size() == 0) {
                    if(completed) {
                        return -1;
                    }

                    while(pendingData.size() == 0) {
                        try {
                            LOCK.wait();
                        } catch (InterruptedException ex) {
                        }
                        if(error != null) {
                            throw new IOException(error);
                        }
                        if(completed && pendingData.size() == 0) {
                            return -1;
                        }
                    }
                }

                byte[] chunk = (byte[])pendingData.elementAt(0);
                int val = chunk[0] & 0xff;
                if(chunk.length == 1) {
                    pendingData.removeElementAt(0);
                } else {
                    byte[] b = new byte[chunk.length - 1];
                    System.arraycopy(chunk, 1, b, 0, b.length);
                    pendingData.setElementAt(b, 0);
                }
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
            synchronized(LOCK) {
                int count = 0;
                for(int iter = 0 ; iter < pendingData.size() ; iter++) {
                    byte[] b = (byte[])pendingData.elementAt(iter);
                    count += b.length;
                }
                return count;
            }
        }

        @Override
        public void close() throws IOException {
            synchronized(LOCK) {
                if(pendingData == null) {
                    return;
                }
                completed = true;
                pendingData = null;
                super.close();
                nativeInstance.closeConnection(peer);
            }
            synchronized(CONNECTIONS_LOCK) {
                instance.connections.remove(peer);
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
                if(pendingData.size() == 0) {
                    if(completed) {
                        return -1;
                    }

                    while(pendingData.size() == 0) {
                        try {
                            LOCK.wait();
                        } catch (InterruptedException ex) {
                        }
                        if(completed && pendingData.size() == 0) {
                            return -1;
                        }
                    }
                }

                byte[] chunk = (byte[])pendingData.elementAt(0);
                if(chunk.length < len) {
                    len = chunk.length;
                }
                for(int iter = 0 ; iter < len ; iter++) {
                    bytes[iter + off] = chunk[iter];
                }

                if(chunk.length == len) {
                    pendingData.removeElementAt(0);
                } else {
                    byte[] b = new byte[chunk.length - len];
                    System.arraycopy(chunk, len, b, 0, b.length);
                    pendingData.setElementAt(b, 0);
                }
                if(error != null) {
                    throw new IOException(error);
                }
                return len;            
            }
        }
        
    }

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
            BufferedInputStream o = new BufferedInputStream(new NSFileInputStream((String)connection), (String)connection);
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
                                try {
                                    i.close();
                                } catch (IOException ex) {
                                    //throw new RuntimeException(ex.getMessage());
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
                roots[iter] = "file:/" + roots[iter];
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
        if(file.startsWith("file:/")) {
            file = file.substring(5);
            if(file.startsWith("//")) {
                 file = file.substring(1);
            }
        }
        return file;
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

    /**
     * @inheritDoc
     */
    public String[] getPlatformOverrides() {
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

    private static PushCallback pushCallback;
    
    public static void pushReceived(final String message, final String type) {
        if(pushCallback != null) {
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    if(type != null) {
                        Display.getInstance().setProperty("pushType", type);
                    }
                    pushCallback.push(message);
                }
            });
        } else {
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
            Display.getInstance().callSerially(new Runnable() {

                @Override
                public void run() {
                    if (getLocalNotificationCallback() != null) {
                        getLocalNotificationCallback().localNotificationReceived(notificationId);
                    }
                }
            });
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
                foldKeyboard();
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
    public void share(String text, String image, String mimeType, Rectangle sourceRect){
        if(image != null && image.length() > 0) {
            try {
                Image img = Image.createImage(image);
                if(img == null) {
                    nativeInstance.socialShare(text, 0, sourceRect );
                    return;
                }
                NativeImage n = (NativeImage)img.getImage();
                nativeInstance.socialShare(text, n.peer, sourceRect);
            } catch(IOException err) {
                err.printStackTrace();
                Dialog.show("Error", "Error loading image: " + image, "OK", null);
            }
        } else {
            nativeInstance.socialShare(text, 0, sourceRect);
        }
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
        return pickerType == Display.PICKER_TYPE_DATE || pickerType == Display.PICKER_TYPE_TIME || pickerType == Display.PICKER_TYPE_DATE_AND_TIME || pickerType == Display.PICKER_TYPE_STRINGS;
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
        } else {
            long time;
            if(currentValue instanceof Integer) {
                java.util.Calendar c = java.util.Calendar.getInstance();
                c.set(java.util.Calendar.HOUR_OF_DAY, ((Integer)currentValue).intValue() / 60);
                c.set(java.util.Calendar.MINUTE, ((Integer)currentValue).intValue() % 60);
                time = c.getTime().getTime();
            } else {
                time = ((java.util.Date)currentValue).getTime();
            }
            nativeInstance.openDatePicker(type, time, x, y, w, h, preferredWidth, preferredHeight);
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
    public Object connectSocket(String host, int port) {
        long i = nativeInstance.connectSocket(host, port);
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
    public void splitString(String source, char separator, ArrayList<String> out) {
        nativeInstance.splitString(source, separator, out);
    }
   
    public void scheduleLocalNotification(LocalNotification n, long firstTime, int repeat) {
        
        nativeInstance.sendLocalNotification(
                n.getId(),
                n.getAlertTitle(),
                n.getAlertBody(),
                n.getAlertSound(),
                n.getBadgeNumber(),
                firstTime,
                repeat
        );
        
       
    }

   
    
    public void cancelLocalNotification(String id) {
         nativeInstance.cancelLocalNotification(id);
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
}



