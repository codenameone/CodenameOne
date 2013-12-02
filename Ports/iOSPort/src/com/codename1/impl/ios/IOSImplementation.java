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

import com.codename1.codescan.CodeScanner;
import com.codename1.codescan.ScanResult;
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
import java.io.ByteArrayInputStream;
import java.util.StringTokenizer;
import com.codename1.io.BufferedInputStream;
import com.codename1.io.BufferedOutputStream;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Storage;
import com.codename1.io.Util;
import com.codename1.l10n.L10NManager;
import com.codename1.location.LocationListener;
import com.codename1.location.LocationManager;
import com.codename1.media.Media;
import com.codename1.messaging.Message;
import com.codename1.payment.Product;
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
import java.io.PrintWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import com.codename1.io.Cookie;
import com.codename1.media.MediaManager;
import com.codename1.ui.Dialog;
import com.codename1.ui.plaf.Style;
import java.io.ByteArrayOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Shai Almog
 */
public class IOSImplementation extends CodenameOneImplementation {
    public static IOSNative nativeInstance = new IOSNative();
    private static PurchaseCallback purchaseCallback;
    private int timeout = 120000;
    private static final Object CONNECTIONS_LOCK = new Object();
    private static Map<Long, NetworkConnection> connections = new HashMap<Long, NetworkConnection>();
    private NativeFont defaultFont;
    private NativeGraphics currentlyDrawingOn;
    //private NativeImage backBuffer;
    private NativeGraphics globalGraphics;
    static IOSImplementation instance;
    private TextArea currentEditing;
    private static boolean initialized;
    private Lifecycle life;
    private static CodeScannerImpl scannerInstance;
    private static boolean minimized;
    private String userAgent;

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
    
    private static final Object EDITING_LOCK = new Object(); 
    private static boolean editNext;
    public void editString(final Component cmp, final int maxSize, final int constraint, final String text, int i) {
        currentEditing = (TextArea)cmp;
        
        //register the edited TextArea to support moving to the next field
        TextEditUtil.setCurrentEditComponent(cmp); 

        final NativeFont fnt = f(cmp.getStyle().getFont().getNativeFont());
        boolean forceSlideUpTmp = false;
        Form current = Display.getInstance().getCurrent();
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
                int x = cmp.getAbsoluteX();
                int y = cmp.getAbsoluteY();
                int w = cmp.getWidth();
                int h = cmp.getHeight();
                int pt = stl.getPadding(false, Component.TOP);
                int pb = stl.getPadding(false, Component.BOTTOM);
                int pl = stl.getPadding(rtl, Component.LEFT);
                int pr = stl.getPadding(rtl, Component.RIGHT);
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
                if(currentEditing.getUIManager().isThemeConstant("nativeHintBool", false) && currentEditing.getHint() != null) {
                    hint = currentEditing.getHint();
                }
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
                        pr, hint);
            }
        });
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
        if(editNext) {
            editNext = false;
            TextEditUtil.editNextTextArea();
        }
    }
    
    // callback for native code!
    public static void editingUpdate(String s, int cursorPositon, boolean finished) {
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
                    EDITING_LOCK.notify();
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

    public void releaseImage(Object image) {
        if(image instanceof NativeImage) {
            ((NativeImage)image).deleteImage();
        }
    }

    public void flushGraphics(int x, int y, int width, int height) {
        /*if(currentlyDrawingOn != null && backBuffer == currentlyDrawingOn.associatedImage) {
            backBuffer.peer = finishDrawingOnImage();
            flushBuffer(backBuffer.peer, x, y, width, height);
            startDrawingOnImage(backBuffer.width, backBuffer.height, backBuffer.peer);
        } else {
            flushBuffer(backBuffer.peer, x, y, width, height);
        }*/
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
        singleDimensionX[0] = x; singleDimensionY[0] = y;
        instance.pointerPressed(singleDimensionX, singleDimensionY);
    }
    public static void pointerReleasedCallback(int x, int y) {
        singleDimensionX[0] = x; singleDimensionY[0] = y;
        instance.pointerReleased(singleDimensionX, singleDimensionY);
    }
    public static void pointerDraggedCallback(int x, int y) {
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
        
        imageRgbToIntArray(((NativeImage)nativeImage).peer, arr, x, y, width, height);
    }

    private void imageRgbToIntArray(long imagePeer, int[] arr, int x, int y, int width, int height) {
        nativeInstance.imageRgbToIntArray(imagePeer, arr, x, y, width, height);
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

    public Object createImage(String path) throws IOException {
        long ns;
        if(path.startsWith("file:")) {
            ns = IOSImplementation.nativeInstance.createNSData(path);
        } else {
            ns = getResourceNSData(path);
        }
        int[] wh = new int[2];
        NativeImage n = new NativeImage(path);
        n.peer = nativeInstance.createImageNSData(ns, wh);
        n.width = wh[0];
        n.height = wh[1];
        nativeInstance.releasePeer(ns);
        return n;
    }

    public boolean hasNativeTheme() {
        return true;
    }

    private static String iosMode = "legacy";
    
    public static void setIosMode(String l) {
        iosMode = l;
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
            int[] wh = new int[2];
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

    public Object createImage(byte[] bytes, int offset, int len) {
        int[] wh = new int[2];
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
        return ((NativeGraphics)graphics).clipX;
    }

    public int getClipY(Object graphics) {
        return ((NativeGraphics)graphics).clipY;
    }

    public int getClipWidth(Object graphics) {
        if(((NativeGraphics)graphics).clipW < 0 && ((NativeGraphics)graphics).associatedImage != null) {
            return ((NativeGraphics)graphics).associatedImage.width;
        }
        return ((NativeGraphics)graphics).clipW;
    }

    public int getClipHeight(Object graphics) {
        if(((NativeGraphics)graphics).clipH < 0 && ((NativeGraphics)graphics).associatedImage != null) {
            return ((NativeGraphics)graphics).associatedImage.height;
        }
        return ((NativeGraphics)graphics).clipH;
    }

    public void setClip(Object graphics, int x, int y, int width, int height) {
        NativeGraphics ng = ((NativeGraphics)graphics);
        ng.checkControl();
        if(ng.clipX == x && ng.clipY == y && ng.clipW == width && ng.clipH == height) {
            return;
        }
        ng.clipApplied = (ng.clipX == x) && (ng.clipY == y) && (ng.clipW == width) && (ng.clipH == height);
        ng.clipX = x;
        ng.clipY = y;
        ng.clipW = width;
        ng.clipH = height;
        if(currentlyDrawingOn == graphics || graphics == globalGraphics) {
            ng.setNativeClipping(x, y, width, height, ng.clipApplied);
            ng.clipApplied = true;
        } 
    }

    private static void setNativeClippingMutable(int x, int y, int width, int height, boolean firstClip) {
        nativeInstance.setNativeClippingMutable(x, y, width, height, firstClip);
    }
    private static void setNativeClippingGlobal(int x, int y, int width, int height, boolean firstClip) {
        nativeInstance.setNativeClippingGlobal(x, y, width, height, firstClip);
    }

    public void clipRect(Object graphics, int x, int y, int width, int height) {
        NativeGraphics ng = (NativeGraphics)graphics;
        if(ng.clipH == 0 || ng.clipW == 0) {
            return;
        }
        if(ng.clipW == -1 || ng.clipH == -1) {
            ng.clipW = ng.associatedImage.width;
            ng.clipH = ng.associatedImage.height;
        }
        Rectangle.intersection(x, y, width, height, ng.clipX, ng.clipY, ng.clipW, ng.clipH, ng.reusableRect);
        Dimension d = ng.reusableRect.getSize();
        if(d.getWidth() <= 0 || d.getHeight() <= 0) {
            ng.clipW = 0;
            ng.clipH = 0;
        } else {
            ng.clipX = ng.reusableRect.getX();
            ng.clipY = ng.reusableRect.getY();
            ng.clipW = d.getWidth();
            ng.clipH = d.getHeight();
            setClip(graphics, ng.clipX, ng.clipY, ng.clipW, ng.clipH);
        }
    }

    private static void nativeDrawLineMutable(int color, int alpha, int x1, int y1, int x2, int y2) {
        nativeInstance.nativeDrawLineMutable(color, alpha, x1, y1, x2, y2);
    }
    private static void nativeDrawLineGlobal(int color, int alpha, int x1, int y1, int x2, int y2) {
        nativeInstance.nativeDrawLineGlobal(color, alpha, x1, y1, x2, y2);
    }

    public void drawLine(Object graphics, int x1, int y1, int x2, int y2) {
        NativeGraphics ng = (NativeGraphics)graphics;
        ng.checkControl();
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
        ng.applyClip();
        ng.nativeFillRect(ng.color, ng.alpha, x, y, width, height);
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
        ng.applyClip();
        ng.nativeDrawRect(ng.color, ng.alpha, x, y, width, height);
    }

    public void drawRoundRect(Object graphics, int x, int y, int width, int height, int arcWidth, int arcHeight) {
        NativeGraphics ng = (NativeGraphics)graphics;
        ng.checkControl();
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
        ng.applyClip();
        ng.nativeFillArc(ng.color, ng.alpha, x, y, width, height, startAngle, arcAngle);
    }

    private static void nativeFillArcMutable(int color, int alpha, int x, int y, int width, int height, int startAngle, int arcAngle) {
        nativeInstance.nativeFillArcMutable(color, alpha, x, y, width, height, startAngle, arcAngle);
    }
    private static void nativeDrawArcMutable(int color, int alpha, int x, int y, int width, int height, int startAngle, int arcAngle) {
        nativeInstance.nativeDrawArcMutable(color, alpha, x, y, width, height, startAngle, arcAngle);
    }
    private static void nativeFillArcGlobal(int color, int alpha, int x, int y, int width, int height, int startAngle, int arcAngle) {
        nativeInstance.nativeFillArcGlobal(color, alpha, x, y, width, height, startAngle, arcAngle);
    }
    private static void nativeDrawArcGlobal(int color, int alpha, int x, int y, int width, int height, int startAngle, int arcAngle) {
        nativeInstance.nativeDrawArcGlobal(color, alpha, x, y, width, height, startAngle, arcAngle);
    }

    public void drawArc(Object graphics, int x, int y, int width, int height, int startAngle, int arcAngle) {
        NativeGraphics ng = (NativeGraphics)graphics;
        ng.checkControl();
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
        ng.applyClip();
        NativeImage nm = (NativeImage)img;
        ng.nativeDrawImage(nm.peer, ng.alpha, x, y, nm.width, nm.height);
    }

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
            stringWidthCache.put(c, new Integer(val));
            return val;
        }
        return nativeInstance.stringWidthNative(peer, str);
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
        StringTokenizer t = new StringTokenizer(resource, "/.");
        int cnt = t.countTokens();
        while(cnt > 2) {
            t.nextToken();
        }
        String name = t.nextToken();
        String type = t.nextToken();
        int val = nativeInstance.getResourceSize(name, type);
        if(val <= 0) {
            return null;
        }
        return new BufferedInputStream(new NSDataInputStream(name, type), resource);
    }

    private static Map softReferenceMap = new HashMap();
    public static void flushSoftRefMap() {
        softReferenceMap = new HashMap();
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
        Object val = softReferenceMap.get(o);
        if(val != null) {
            return val;
        }
        return null;
    }

    public Object createSoftWeakRef(Object o) {
        Object key = new Object();
        softReferenceMap.put(key, o);
        return key;
        //return new SoftReference(o);
    }

    class Loc extends LocationManager {
        private long peer;
        private boolean locationUpdating;

        protected void finalize() throws Throwable {
            super.finalize();
            if(peer != 0) {
                nativeInstance.releasePeer(peer);
            }
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

        @Override
        protected void bindListener() {
            if(!locationUpdating) {
                long p = getLocation();
                if(p <= 0) {
                    return;
                }
                locationUpdating = true;
                nativeInstance.startUpdatingLocation(p);
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
            final LocationListener ls = lm.getLocationListener();
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
                nativeInstance.startAudioRecord(peer[0]);
                playing = true;
            }

            @Override
            public void pause() {
                nativeInstance.pauseAudioRecord(peer[0]);
                playing = false;
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
    }

    @Override
    public void openImageGallery(ActionListener response) {    
        captureCallback = new EventDispatcher();
        captureCallback.addListener(response);
        nativeInstance.openImageGallery();
    }
    
    
    class IOSMedia implements Media {
        private String uri;
        private boolean isVideo;
        private Runnable onCompletion;
        private InputStream stream;
        private String mimeType;
        private PeerComponent component;
        private boolean nativePlayer;
        private long moviePlayerPeer;
        private boolean fullScreen;
        
        public IOSMedia(String uri, boolean isVideo, Runnable onCompletion) {
            this.uri = uri;
            this.isVideo = isVideo;
            this.onCompletion = onCompletion;
            if(!isVideo) {
                moviePlayerPeer = nativeInstance.createAudio(uri, onCompletion);
            }
        }

        public IOSMedia(InputStream stream, String mimeType, Runnable onCompletion) {
            this.stream = stream;
            this.mimeType = mimeType;
            this.onCompletion = onCompletion;            
            isVideo = mimeType.indexOf("video") > -1;
            if(!isVideo) {
                try {
                    moviePlayerPeer = nativeInstance.createAudio(Util.readInputStream(stream), onCompletion);
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
                        moviePlayerPeer = nativeInstance.createVideoComponent(uri);
                    } else {
                        try {
                            long val = getNSData(stream);
                            if(val > 0) {
                                moviePlayerPeer = nativeInstance.createVideoComponentNSData(val);
                                Util.cleanup(stream);
                            } else {
                                byte[] data = Util.readInputStream(stream);
                                Util.cleanup(stream);
                                moviePlayerPeer = nativeInstance.createVideoComponent(data);
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
                    nativeInstance.stopVideoComponent(moviePlayerPeer);
                } else {
                    nativeInstance.pauseAudio(moviePlayerPeer);
                }
            }
        }

        @Override
        public void cleanup() {
            if(moviePlayerPeer != 0) {
                if(isVideo) {
                    nativeInstance.releasePeer(moviePlayerPeer);
                } else {
                    nativeInstance.cleanupAudio(moviePlayerPeer);                    
                }
                moviePlayerPeer = 0;
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
            if(uri != null) {
                moviePlayerPeer = nativeInstance.createVideoComponent(uri);
                component = PeerComponent.create(new long[] { nativeInstance.getVideoViewPeer(moviePlayerPeer) });
            } else {
                try {
                    byte[] data = toByteArray(stream);
                    Util.cleanup(stream);
                    moviePlayerPeer = nativeInstance.createVideoComponent(data);
                    component = PeerComponent.create(new long[] { nativeInstance.getVideoViewPeer(moviePlayerPeer) });
                } catch (IOException ex) {
                    ex.printStackTrace();
                    return new Label("Error loading video " + ex);
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
        ((NativeGraphics)nativeGraphics).rotate(angle * 57.2957795f, x, y);
    }

    public void shear(Object nativeGraphics, float x, float y) {
        ((NativeGraphics)nativeGraphics).shear(x, y);
    }


    class NativeGraphics {
        Rectangle reusableRect = new Rectangle();
        NativeImage associatedImage;
        int color;
        int alpha = 255;
        NativeFont font;
        int clipX, clipY, clipW = -1, clipH = -1;
        boolean clipApplied;


        public NativeFont getFont() {
            if(font != null) {
                return font;
            }
            return (NativeFont)getDefaultFont();
        }

        public void applyClip() {
            if(clipH > -1 && clipW > -1) {
                setNativeClipping(clipX, clipY, clipW, clipH, clipApplied);
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

        public void resetAffine() {
        }

        public void scale(float x, float y) {
        }

        public void rotate(float angle) {
        }

        public void rotate(float angle, int x, int y) {
        }

        public void shear(float x, float y) {
        }

        public void fillRectRadialGradient(int startColor, int endColor, int x, int y, int width, int height, float relativeX, float relativeY, float relativeSize) {
            nativeInstance.fillRectRadialGradientMutable(startColor, endColor, x, y, width, height, relativeX, relativeY, relativeSize);
        }
    
        public void fillLinearGradient(int startColor, int endColor, int x, int y, int width, int height, boolean horizontal) {
            nativeInstance.fillLinearGradientMutable(startColor, endColor, x, y, width, height, horizontal);
        }
    }

    class GlobalGraphics extends NativeGraphics {
        public void checkControl() {
            if(currentlyDrawingOn != this) {
                if(currentlyDrawingOn != null) {
                    currentlyDrawingOn.associatedImage.peer = finishDrawingOnImage();
                }
                currentlyDrawingOn = null;
            }
        }

        public void resetAffine() {
            nativeInstance.resetAffineGlobal();
        }

        public void scale(float x, float y) {
            nativeInstance.scaleGlobal(x, y);
        }

        public void rotate(float angle) {
            nativeInstance.rotateGlobal(angle);
        }

        public void rotate(float angle, int x, int y) {
            nativeInstance.rotateGlobal(angle, x, y);
        }

        public void shear(float x, float y) {
            nativeInstance.shearGlobal(x, y);
        }

        void setNativeClipping(int x, int y, int width, int height, boolean firstClip) {
            setNativeClippingGlobal(x, y, width, height, firstClip);
        }

        void nativeDrawLine(int color, int alpha, int x1, int y1, int x2, int y2) {
            nativeDrawLineGlobal(color, alpha, x1, y1, x2, y2);
        }

        void nativeFillRect(int color, int alpha, int x, int y, int width, int height) {
            nativeFillRectGlobal(color, alpha, x, y, width, height);
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

        void nativeDrawArc(int color, int alpha, int x, int y, int width, int height, int startAngle, int arcAngle) {
            nativeDrawArcGlobal(color, alpha, x, y, width, height, startAngle, arcAngle);
        }

        void nativeFillArc(int color, int alpha, int x, int y, int width, int height, int startAngle, int arcAngle) {
            nativeFillArcGlobal(color, alpha, x, y, width, height, startAngle, arcAngle);
        }

        void nativeDrawString(int color, int alpha, long fontPeer, String str, int x, int y) {
            nativeDrawStringGlobal(color, alpha, fontPeer, str, x, y);
        }

        void nativeDrawImage(long peer, int alpha, int x, int y, int width, int height) {
            nativeDrawImageGlobal(peer, alpha, x, y, width, height);
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
        private Map<Character, Integer> widthCache = new HashMap<Character, Integer>();
        
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

    @Override
    public int convertToPixels(int dipCount, boolean horizontal) {
        // ipad mini is ignored, there is no sensible way to detect it
        if(isTablet()) {
            if(getDisplayWidth() < 1100) {
                return (int)Math.round((((float)dipCount) * 5.1975051975052));
            }
            return (int)Math.round((((float)dipCount) * 10.3939299449122));
        } else {
            if(getDisplayWidth() < 500) {
                return (int)Math.round((((float)dipCount) * 6.4173236936575));
            }
            return (int)Math.round((((float)dipCount) * 12.8369704749679));
        }
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
    public void execute(String url) {
        nativeInstance.execute(url);
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
        bc.fireWebEvent("onError", new ActionEvent(new Integer(code), code));
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
    public String getProperty(String key, String defaultValue) {
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
        return minimized || nativeInstance.isMinimized();
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
        nativeInstance.getContactRefIds(c, withNumbers);
        String[] r = new String[c.length];
        for(int iter = 0 ; iter < c.length ; iter++) {
            r[iter] = "" + c[iter];
        }
        return r;
    }

    @Override
    public Contact getContactById(String id, boolean includesFullName, boolean includesPicture, boolean includesNumbers, boolean includesEmail, boolean includeAddress) {
        int recId = Integer.parseInt(id);
        Contact c = new Contact();
        c.setId(id);
        c.setAddresses(new Hashtable());
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
    public void sendSMS(String phoneNumber, String message) throws IOException{
        nativeInstance.sendSMS(phoneNumber, message);
    }

    public void systemOut(String content) {
        nativeInstance.log(content);
    }

    @Override
    public boolean isTrueTypeSupported() {
        // TODO
        return true;
    }

    @Override
    public Object loadTrueTypeFont(String fontName, String fileName) {
        NativeFont fnt = new NativeFont();
        fnt.face = com.codename1.ui.Font.FACE_SYSTEM;
        fnt.size = com.codename1.ui.Font.SIZE_MEDIUM;
        fnt.style = com.codename1.ui.Font.STYLE_PLAIN;
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
            baseUrl = "file://localhost" + nativeInstance.getResourcesDir().replace(" ", "%20") + baseUrl.substring(6);
        }
        nativeInstance.setBrowserPage(get(browserPeer), html, baseUrl);
    }

    @Override
    public void setBrowserProperty(PeerComponent browserPeer, String key, Object value) {
    }

    @Override
    public void setBrowserURL(PeerComponent browserPeer, String url) {
        if(url.startsWith("jar://")) {
            url = "file://localhost" + nativeInstance.getResourcesDir().replace(" ", "%20") + url.substring(6);
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
        return new NativeIPhoneView(nativeComponent);
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
            int[] wh = new int[2];
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
        ng.applyClip();
        ng.fillRectRadialGradient(startColor, endColor, x, y, width, height, relativeX, relativeY, relativeSize);
    }

    public void fillLinearGradient(Object graphics, int startColor, int endColor, int x, int y, int width, int height, boolean horizontal) {
        NativeGraphics ng = (NativeGraphics)graphics;
        ng.checkControl();
        ng.applyClip();
        ng.fillLinearGradient(startColor, endColor, x, y, width, height, horizontal);
    }

    public static void appendData(long peer, byte[] data) {
        NetworkConnection n;
        synchronized(CONNECTIONS_LOCK) {
            n = connections.get(peer);
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
            n = connections.get(peer);
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
            n = connections.get(peer);
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

    static class NetworkConnection extends InputStream {
        private long peer;
        private ByteArrayOutputStream body;
        private Vector pendingData = new Vector();
        private boolean completed;
        private Hashtable headers = new Hashtable();
        private boolean connected;
        private boolean ensureConnectionLock;
        String error;
        public final Object LOCK = new Object();
        
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
                    nativeInstance.setBody(peer, body.toByteArray());
                    body = null;
                }
                nativeInstance.connect(peer);
                while(!connected) {
                    try {
                        LOCK.wait();
                    } catch (InterruptedException ex) {
                    }
                }
                if(error != null) {
                    throw new IOException(error);
                }
            }
        }
        
        public NetworkConnection(long peer) {
            this.peer = peer;
            synchronized(CONNECTIONS_LOCK) {
                connections.put(peer, this);
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
            synchronized(LOCK) {
                pendingData.addElement(data);
                LOCK.notify();
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
                connections.remove(peer);
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
        n.body = new ByteArrayOutputStream();
        return n.body;
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
            BufferedInputStream o = new BufferedInputStream(new NSDataInputStream((String)connection), (String)connection);
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
        for(int iter = 0 ; iter < s.length ; iter++) {
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
        String[] tokenized = s.split(",");
        if(tokenized.length > 1) {
            List<String> result = new ArrayList<String>();
            String loaded = null;
            for(String current : tokenized) {
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
        return tokenized;
    }

    /**
     * @inheritDoc
     */
    public void deleteStorageFile(String name) {
        nativeInstance.deleteFile(nativeInstance.getCachesDir() + "/" + name);
    }

    /**
     * @inheritDoc
     */
    public OutputStream createStorageOutputStream(String name) throws IOException {
        name = nativeInstance.getCachesDir() + "/" + name;
        return new BufferedOutputStream(new NSDataOutputStream(name) , name);
    }

    /**
     * @inheritDoc
     */
    public InputStream createStorageInputStream(String name) throws IOException {
        name = nativeInstance.getCachesDir() + "/" + name;
        return new BufferedInputStream(new NSDataInputStream(name), name);
    }

    /**
     * @inheritDoc
     */
    public boolean storageFileExists(String name) {
        return nativeInstance.fileExists(nativeInstance.getCachesDir() + "/" + name);
    }

    /**
     * @inheritDoc
     */
    public String[] listStorageEntries() {
        String c = nativeInstance.getCachesDir();
        String[] a = new String[nativeInstance.fileCountInDir(c)];
        nativeInstance.listFilesInDir(c, a);
        return a;
    }

    /**
     * @inheritDoc
     */
    public int getStorageEntrySize(String name) {
        return nativeInstance.getFileSize(nativeInstance.getCachesDir() + "/" + name);
    }

    /**
     * @inheritDoc
     */
    public String[] listFilesystemRoots() {
        String[] roots = new String[] {
            nativeInstance.getCachesDir(),
            nativeInstance.getDocumentsDir(),
            nativeInstance.getResourcesDir()
        };
        return roots;
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
        nativeInstance.createDirectory(directory);
    }

    /**
     * @inheritDoc
     */
    public void deleteFile(String file) {
        nativeInstance.deleteFile(file);
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
        return nativeInstance.getFileSize(file);
    }

    public long getFileLastModified(String file) {
        return nativeInstance.getFileLastModified(file);
    }

    /**
     * @inheritDoc
     */
    public boolean isDirectory(String file) {
        return nativeInstance.isDirectory(file);
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
        return new BufferedOutputStream(new NSDataOutputStream(file), file);
    }

    /**
     * @inheritDoc
     */
    public InputStream openFileInputStream(String file) throws IOException {
        if(file.startsWith("file:/")) {
            file = file.substring(5);
        }
        return new BufferedInputStream(new NSDataInputStream(file), file);
    }

    /**
     * @inheritDoc
     */
    public boolean exists(String file) {
        if(file.startsWith("file:")) {
            int slash = file.indexOf('/');
            while(file.charAt(slash + 1) == '/') {
                slash++;
            }
            file = file.substring(slash - 1);
        }
        return nativeInstance.fileExists(file);
    }

    /**
     * @inheritDoc
     */
    public void rename(String file, String newName) {
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
        PrintWriter p = new PrintWriter(o);
        t.printStackTrace(p);
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
        System.out.println("Push handleRegistration() Sending registration to server!");
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
    
    public static void setMainClass(Object main) {
        if(main instanceof PushCallback) {
            pushCallback = (PushCallback)main;
        }
        if(main instanceof PurchaseCallback) {
            purchaseCallback = (PurchaseCallback)main;
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
     * Called as part of the transition from the background to the inactive state; 
     * here you can undo many of the changes made on entering the background.
     */
    public static void applicationDidBecomeActive() {
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
        return true;
    }
    
    @Override
    public void share(String text, String image, String mimeType){
        if(image != null && image.length() > 0) {
            try {
                Image img = Image.createImage(image);
                if(img == null) {
                    nativeInstance.socialShare(text, 0);
                    return;
                }
                NativeImage n = (NativeImage)img.getImage();
                nativeInstance.socialShare(text, n.peer);
            } catch(IOException err) {
                err.printStackTrace();
                Dialog.show("Error", "Error loading image: " + image, "OK", null);
            }
        } else {
            nativeInstance.socialShare(text, 0);
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
    
    Vector getPurchased() {
        if(purchasedItems == null) {
            purchasedItems = (Vector)Storage.getInstance().readObject("CN1PurchasedItemList.dat");
            if(purchasedItems == null) {
                purchasedItems = new Vector();
            }
        }
        return purchasedItems;
    }
    
    static void itemPurchased(final String sku) {
        if(purchaseCallback != null) {
            safeCallSerially(new Runnable() {
                @Override
                public void run() {
                    purchaseCallback.itemPurchased(sku);
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
        if(purchaseCallback != null) {
            safeCallSerially(new Runnable() {
                @Override
                public void run() {
                    purchaseCallback.itemRefunded(sku);
                }
            });
        }
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
                scannerInstance.callback.scanCompleted(contents, formatName, null);
                scannerInstance.callback = null;
                Display.getInstance().getCurrent().revalidate();
                Display.getInstance().getCurrent().repaint();
            }
        });
    }

    static void scanError(final int errorCode, final String message) {
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                scannerInstance.callback.scanError(errorCode, message);
                scannerInstance.callback = null;
                Display.getInstance().getCurrent().revalidate();
                Display.getInstance().getCurrent().repaint();
            }
        });
    }

    static void scanCanceled() {
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                scannerInstance.callback.scanCanceled();
                scannerInstance.callback = null;
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
        return pickerType == Display.PICKER_TYPE_DATE || pickerType == Display.PICKER_TYPE_TIME || pickerType == Display.PICKER_TYPE_DATE_AND_TIME;
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
        long time;
        if(currentValue instanceof Integer) {
            java.util.Calendar c = java.util.Calendar.getInstance();
            c.set(java.util.Calendar.HOUR_OF_DAY, ((Integer)currentValue).intValue() / 60);
            c.set(java.util.Calendar.MINUTE, ((Integer)currentValue).intValue() % 60);
            time = c.getTime().getTime();
        } else {
            time = ((java.util.Date)currentValue).getTime();
        }
        datePickerResult = -2;
        int x = 0, y = 0, w = 20, h = 20;
        if(source != null) {
            x = source.getAbsoluteX();
            y = source.getAbsoluteY();
            w = source.getWidth();
            h = source.getHeight();
        }
        nativeInstance.openDatePicker(type, time, x, y, w, h);
        
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
        });
        if(datePickerResult == -1) {
            return null;
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
}
