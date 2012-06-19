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
import com.codename1.io.Util;
import com.codename1.l10n.L10NManager;
import com.codename1.location.LocationListener;
import com.codename1.location.LocationManager;
import com.codename1.media.Media;
import com.codename1.messaging.Message;
import com.codename1.push.PushCallback;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.Label;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.util.EventDispatcher;
import com.codename1.ui.util.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

/**
 *
 * @author Shai Almog
 */
public class IOSImplementation extends CodenameOneImplementation {
    private int timeout = 120000;
    private static Map<Long, NetworkConnection> connections = new HashMap<Long, NetworkConnection>();
    private NativeFont defaultFont;
    private NativeGraphics currentlyDrawingOn;
    //private NativeImage backBuffer;
    private NativeGraphics globalGraphics;
    static IOSImplementation instance;
    private TextArea currentEditing;
    private static boolean initialized;
    private boolean editingText;
    
    public void initEDT() {
        while(!initialized) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
            }
        }
        globalGraphics = new GlobalGraphics();
    }

    private static Runnable callback;
    
    public static void callback() {
        initialized = true;
        Display.getInstance().callSerially(callback);
    }
    
    public void postInit() {
        IOSNative.initVM();
    }
    
    public void init(Object m) {
        instance = this;
        Display.getInstance().setTransitionYield(10);
        Display.getInstance().setDefaultVirtualKeyboard(null);
        callback = (Runnable)m;
    }

    public void setThreadPriority(Thread t, int p) {
    }
    
    public int getDisplayWidth() {
        return IOSNative.getDisplayWidth();
    }

    public int getDisplayHeight() {
        if(editingText) {
            return IOSNative.getDisplayHeight() / 2; 
        }
        return IOSNative.getDisplayHeight();
    }

    public int getActualDisplayHeight() {
        return IOSNative.getDisplayHeight();
    }

    public boolean isNativeInputImmediate() {
        return true;
    }

    public boolean isNativeInputSupported() {
        return true;
    }

    public void exitApplication() {
        System.exit(0);
    }

    public boolean isTablet() {
        return IOSNative.isTablet();
    }
    
    public void editString(final Component cmp, final int maxSize, final int constraint, final String text, int i) {
        /*if(cmp.getAbsoluteY() > getDisplayHeight() / 3) {
            // if the text component is too low in the screen we "resize the screen" to fit
            editingText = true;
            sizeChangedImpl(getDisplayWidth(), getDisplayHeight());
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    cmp.getComponentForm().scrollComponentToVisible(cmp);
                    currentEditing = (TextArea)cmp;

                    NativeFont fnt = f(cmp.getStyle().getFont().getNativeFont());
                    editStringAt(cmp.getAbsoluteX(),
                            cmp.getAbsoluteY(),
                            cmp.getWidth(),
                            cmp.getHeight(),
                            fnt.peer, currentEditing.isSingleLineTextArea(),
                            currentEditing.getRows(), maxSize, constraint, text);
                }
            });
        } else {*/
            currentEditing = (TextArea)cmp;

            NativeFont fnt = f(cmp.getStyle().getFont().getNativeFont());
            editStringAt(cmp.getAbsoluteX(),
                    cmp.getAbsoluteY(),
                    cmp.getWidth(),
                    cmp.getHeight(),
                    fnt.peer, currentEditing.isSingleLineTextArea(),
                    currentEditing.getRows(), maxSize, constraint, text);
        //}
    }
    
    // callback for native code!
    public static void editingUpdate(String s, int cursorPositon, boolean finished) {
        instance.editingText = false;
        if(instance.currentEditing != null) {
            if(finished) {
                instance.currentEditing.setText(s);
                instance.currentEditing = null;
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

    private void editStringAt(int x, int y, int w, int h, long peer, boolean singleLine, int rows, int maxSize, int constraint, String text) {
        IOSNative.editStringAt(x, y, w, h, peer, singleLine, rows, maxSize, constraint, text);
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

    public static void pointerPressedCallback(int x, int y) {
        instance.pointerPressed(new int[] {x}, new int[] {y});
    }
    public static void pointerReleasedCallback(int x, int y) {
        instance.pointerReleased(new int[] {x}, new int[] {y});
    }
    public static void pointerDraggedCallback(int x, int y) {
        instance.pointerDragged(new int[] {x}, new int[] {y});
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
        IOSNative.flushBuffer(peer, x, y, width, height);
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
        IOSNative.imageRgbToIntArray(imagePeer, arr, x, y, width, height);
    }

    private long createImageFromARGB(int[] argb, int width, int height) {
        return IOSNative.createImageFromARGB(argb, width, height);
    }

    public Object createImage(int[] rgb, int width, int height) {
        NativeImage n = new NativeImage("Image created from ARGB array: " + rgb.length + " width " + width + " height " + height);
        n.peer = createImageFromARGB(rgb, width, height);
        n.width = width;
        n.height = height;
        return n;
    }

    public Object createImage(String path) throws IOException {
        InputStream i;
        if(path.startsWith("file:")) {
            i = openFileInputStream(path);
        } else {
            i = getResourceAsStream(getClass(), path);
        }
        Object o = createImage(i);
        Util.cleanup(i);
        return o;
    }

    public boolean hasNativeTheme() {
        return true;
    }

    /**
     * Installs the native theme, this is only applicable if hasNativeTheme() returned true. Notice that this method
     * might replace the DefaultLookAndFeel instance and the default transitions.
     */
    public void installNativeTheme() {
        try {
            Resources r = Resources.open("/iPhoneTheme.res");
            UIManager.getInstance().setThemeProps(r.getTheme(r.getThemeResourceNames()[0]));
        } catch (IOException ex) {
            ex.printStackTrace();
        }        
    }

    public Object createImage(InputStream i) throws IOException {
        byte[] buffer = new byte[8192];
        int size = i.read(buffer);
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        while(size > -1) {
            bo.write(buffer, 0, size);
            size = i.read(buffer);
        }
        buffer = bo.toByteArray();
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
        return IOSNative.createImage(data, widthHeight);
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
        IOSNative.retainPeer(n.peer);
        return n;
    }

    private long scale(long peer, int width, int height) {
        return IOSNative.scale(peer, width, height);
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
        if(((NativeGraphics)graphics).clipW < 0) {
            return ((NativeGraphics)graphics).associatedImage.width;
        }
        return ((NativeGraphics)graphics).clipW;
    }

    public int getClipHeight(Object graphics) {
        if(((NativeGraphics)graphics).clipH < 0) {
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
        IOSNative.setNativeClippingMutable(x, y, width, height, firstClip);
    }
    private static void setNativeClippingGlobal(int x, int y, int width, int height, boolean firstClip) {
        IOSNative.setNativeClippingGlobal(x, y, width, height, firstClip);
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
        Rectangle r = new Rectangle(ng.clipX, ng.clipY, ng.clipW, ng.clipH).intersection(x, y, width, height);
        Dimension d = r.getSize();
        if(d.getWidth() <= 0 || d.getHeight() <= 0) {
            ng.clipW = 0;
            ng.clipH = 0;
        } else {
            ng.clipX = r.getX();
            ng.clipY = r.getY();
            ng.clipW = d.getWidth();
            ng.clipH = d.getHeight();
            setClip(graphics, ng.clipX, ng.clipY, ng.clipW, ng.clipH);
        }
    }

    private static void nativeDrawLineMutable(int color, int alpha, int x1, int y1, int x2, int y2) {
        IOSNative.nativeDrawLineMutable(color, alpha, x1, y1, x2, y2);
    }
    private static void nativeDrawLineGlobal(int color, int alpha, int x1, int y1, int x2, int y2) {
        IOSNative.nativeDrawLineGlobal(color, alpha, x1, y1, x2, y2);
    }

    public void drawLine(Object graphics, int x1, int y1, int x2, int y2) {
        NativeGraphics ng = (NativeGraphics)graphics;
        ng.checkControl();
        ng.applyClip();
        ng.nativeDrawLine(ng.color, ng.alpha, x1, y1, x2, y2);
    }

    private static void nativeFillRectMutable(int color, int alpha, int x, int y, int width, int height) {
        IOSNative.nativeFillRectMutable(color, alpha, x, y, width, height);
    }
    private static void nativeFillRectGlobal(int color, int alpha, int x, int y, int width, int height) {
        IOSNative.nativeFillRectGlobal(color, alpha, x, y, width, height);
    }

    public void fillRect(Object graphics, int x, int y, int width, int height) {
        NativeGraphics ng = (NativeGraphics)graphics;
        ng.checkControl();
        ng.applyClip();
        ng.nativeFillRect(ng.color, ng.alpha, x, y, width, height);
    }

    private static void nativeDrawRectMutable(int color, int alpha, int x, int y, int width, int height) {
        IOSNative.nativeDrawRectMutable(color, alpha, x, y, width, height);
    }
    private static void nativeDrawRectGlobal(int color, int alpha, int x, int y, int width, int height) {
        IOSNative.nativeDrawRectGlobal(color, alpha, x, y, width, height);
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
        IOSNative.nativeDrawRoundRectMutable(color, alpha, x, y, width, height, arcWidth, arcHeight);
    }
    private static void nativeDrawRoundRectGlobal(int color, int alpha, int x, int y, int width, int height, int arcWidth, int arcHeight) {
        IOSNative.nativeDrawRoundRectGlobal(color, alpha, x, y, width, height, arcWidth, arcHeight);        
    }


    public void fillRoundRect(Object graphics, int x, int y, int width, int height, int arcWidth, int arcHeight) {
        NativeGraphics ng = (NativeGraphics)graphics;
        ng.checkControl();
        ng.applyClip();
        ng.nativeFillRoundRect(ng.color, ng.alpha, x, y, width, height, arcWidth, arcHeight);
    }

    private static void nativeFillRoundRectMutable(int color, int alpha, int x, int y, int width, int height, int arcWidth, int arcHeight) {
        IOSNative.nativeFillRoundRectMutable(color, alpha, x, y, width, height, arcWidth, arcHeight);
    }
    private static void nativeFillRoundRectGlobal(int color, int alpha, int x, int y, int width, int height, int arcWidth, int arcHeight) {
        IOSNative.nativeFillRoundRectGlobal(color, alpha, x, y, width, height, arcWidth, arcHeight);
    }

    public void fillArc(Object graphics, int x, int y, int width, int height, int startAngle, int arcAngle) {
        NativeGraphics ng = (NativeGraphics)graphics;
        ng.checkControl();
        ng.applyClip();
        ng.nativeFillArc(ng.color, ng.alpha, x, y, width, height, startAngle, arcAngle);
    }

    private static void nativeFillArcMutable(int color, int alpha, int x, int y, int width, int height, int startAngle, int arcAngle) {
        IOSNative.nativeFillArcMutable(color, alpha, x, y, width, height, startAngle, arcAngle);
    }
    private static void nativeDrawArcMutable(int color, int alpha, int x, int y, int width, int height, int startAngle, int arcAngle) {
        IOSNative.nativeDrawArcMutable(color, alpha, x, y, width, height, startAngle, arcAngle);
    }
    private static void nativeFillArcGlobal(int color, int alpha, int x, int y, int width, int height, int startAngle, int arcAngle) {
        IOSNative.nativeFillArcGlobal(color, alpha, x, y, width, height, startAngle, arcAngle);
    }
    private static void nativeDrawArcGlobal(int color, int alpha, int x, int y, int width, int height, int startAngle, int arcAngle) {
        IOSNative.nativeDrawArcGlobal(color, alpha, x, y, width, height, startAngle, arcAngle);
    }

    public void drawArc(Object graphics, int x, int y, int width, int height, int startAngle, int arcAngle) {
        NativeGraphics ng = (NativeGraphics)graphics;
        ng.checkControl();
        ng.applyClip();
        ng.nativeDrawArc(ng.color, ng.alpha, x, y, width, height, startAngle, arcAngle);
    }

    private static void nativeDrawStringMutable(int color, int alpha, long fontPeer, String str, int x, int y) {
        IOSNative.nativeDrawStringMutable(color, alpha, fontPeer, str, x, y);
    }
    private static void nativeDrawStringGlobal(int color, int alpha, long fontPeer, String str, int x, int y) {
        IOSNative.nativeDrawStringGlobal(color, alpha, fontPeer, str, x, y);
    }

    public void drawString(Object graphics, String str, int x, int y) {
        NativeGraphics ng = (NativeGraphics)graphics;
        ng.checkControl();
        ng.applyClip();
        NativeFont fnt = ng.getFont();
        ng.nativeDrawString(ng.color, ng.alpha, fnt.peer, str, x, y);
    }

    public void tileImage(Object graphics, Object img, int x, int y, int w, int h) {
        NativeGraphics ng = (NativeGraphics)graphics;
        if(ng instanceof GlobalGraphics) {
            ng.checkControl();
            ng.applyClip();
            NativeImage nm = (NativeImage)img;
            IOSNative.nativeTileImageGlobal(nm.peer, ng.alpha, x, y, w, h);
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
        IOSNative.nativeDrawImageMutable(peer, alpha, x, y, width, height);
    }
    private void nativeDrawImageGlobal(long peer, int alpha, int x, int y, int width, int height) {
        IOSNative.nativeDrawImageGlobal(peer, alpha, x, y, width, height);
    }

    public void drawRGB(Object graphics, int[] rgbData, int offset, int x, int y, int w, int h, boolean processAlpha) {
        Object nativeImage = createImage(rgbData, w, h);
        drawImage(graphics, nativeImage, x, y);
    }

    public Object getNativeGraphics() {
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
    private int stringWidthNative(long peer, String str) {
        if(str.length() < 50) {
            FontStringCache c = new FontStringCache(str, peer);
            Integer i = stringWidthCache.get(c);
            if(i != null) {
                return i.intValue();
            }
            int val = IOSNative.stringWidthNative(peer, str);
            stringWidthCache.put(c, new Integer(val));
            return val;
        }
        return IOSNative.stringWidthNative(peer, str);
    }

    public int charWidth(Object nativeFont, char ch) {
        return charWidthNative(f(nativeFont).peer, ch);
    }

    private int charWidthNative(long peer, char ch) {
        return IOSNative.charWidthNative(peer, ch);
    }

    public int getHeight(Object nativeFont) {
        return getFontHeightNative(f(nativeFont).peer);
    }


    private int getFontHeightNative(long peer) {
        return IOSNative.getFontHeightNative(peer);
    }

    public Object getDefaultFont() {
        if(defaultFont == null) {
            defaultFont = (NativeFont)createFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
        }
        return defaultFont;
    }

    private long createSystemFont(int face, int style, int size) {
        return IOSNative.createSystemFont(face, style, size);
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
        IOSNative.setImageName(((NativeImage)nativeImage).peer, name);
    }

    public InputStream getResourceAsStream(Class cls, String resource) {
        StringTokenizer t = new StringTokenizer(resource, "/.");
        int cnt = t.countTokens();
        while(cnt > 2) {
            t.nextToken();
        }
        String name = t.nextToken();
        String type = t.nextToken();
        byte[] b = loadResource(name, type);
        if(b == null) {
            return null;
        }
        return new ByteArrayInputStream(b);
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
                IOSNative.releasePeer(peer);
            }
        }

        private long getLocation() {
            if(peer < 0) {
                return peer;
            }
            if(peer == 0) {
                peer = IOSNative.createCLLocation();
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
            long c = IOSNative.getCurrentLocationObject(p);
            l.setAccuracy((float)IOSNative.getLocationAccuracy(c));
            l.setAltitude(IOSNative.getLocationAltitude(c));
            l.setDirection((float)IOSNative.getLocationDirection(c));
            l.setLatitude(IOSNative.getLocationLatitude(c));
            l.setLongitude(IOSNative.getLocationLongtitude(c));
            if(IOSNative.isGoodLocation(p)) {
                l.setStatus(LocationManager.AVAILABLE);
            } else {
                l.setStatus(LocationManager.TEMPORARILY_UNAVAILABLE);
            }
            l.setTimeStamp(IOSNative.getLocationTimeStamp(c));
            l.setVelocity((float)IOSNative.getLocationVelocity(c));
            IOSNative.releasePeer(c);
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
                IOSNative.startUpdatingLocation(p);
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
                IOSNative.stopUpdatingLocation(p);
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
                captureCallback.fireActionEvent(new ActionEvent("file:" + r));
            } else {
                captureCallback.fireActionEvent(null);
            }
            captureCallback = null;
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
        IOSNative.captureCamera(false);
    }

    public Media createMediaRecorder(String path) throws IOException{
        final long[] peer = new long[] { IOSNative.createAudioRecorder(path) };
        return new Media() {
            private boolean playing;
            @Override
            public void play() {
                IOSNative.startAudioRecord(peer[0]);
                playing = true;
            }

            @Override
            public void pause() {
                IOSNative.pauseAudioRecord(peer[0]);
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
                    IOSNative.pauseAudioRecord(peer[0]);
                }
                IOSNative.cleanupAudioRecord(peer[0]);
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
        };
    }
    
    /**
     * Captures a video and notifies with the data when available
     * @param response callback for the resulting video
     */
    public void captureVideo(ActionListener response) {
        captureCallback = new EventDispatcher();
        captureCallback.addListener(response);
        IOSNative.captureCamera(true);
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
                moviePlayerPeer = IOSNative.createAudio(uri, onCompletion);
            }
        }

        public IOSMedia(InputStream stream, String mimeType, Runnable onCompletion) {
            this.stream = stream;
            this.mimeType = mimeType;
            this.onCompletion = onCompletion;            
            isVideo = mimeType.indexOf("video") > -1;
            if(!isVideo) {
                try {
                    moviePlayerPeer = IOSNative.createAudio(Util.readInputStream(stream), onCompletion);
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
                        moviePlayerPeer = IOSNative.createVideoComponent(uri);
                    } else {
                        try {
                            byte[] data = Util.readInputStream(stream);
                            Util.cleanup(stream);
                            moviePlayerPeer = IOSNative.createVideoComponent(data);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                    IOSNative.showNativePlayerController(moviePlayerPeer);
                    return;
                }
                if(moviePlayerPeer != 0) {
                    IOSNative.startVideoComponent(moviePlayerPeer);
                }
            } else {
                IOSNative.playAudio(moviePlayerPeer);                
            }
        }

        @Override
        public void pause() {
            if(moviePlayerPeer != 0) {
                if(isVideo) {
                    IOSNative.stopVideoComponent(moviePlayerPeer);
                } else {
                    IOSNative.pauseAudio(moviePlayerPeer);
                }
            }
        }

        @Override
        public void cleanup() {
            if(moviePlayerPeer != 0) {
                if(isVideo) {
                    IOSNative.releasePeer(moviePlayerPeer);
                } else {
                    IOSNative.cleanupAudio(moviePlayerPeer);                    
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
                    return IOSNative.getMediaTimeMS(moviePlayerPeer);
                } else {
                    return IOSNative.getAudioTime(moviePlayerPeer);
                }
            }
            return 0;
        }

        @Override
        public void setTime(int time) {
            if(moviePlayerPeer != 0) {
                if(isVideo) {
                    IOSNative.setMediaTimeMS(moviePlayerPeer, time);
                } else {
                    IOSNative.setAudioTime(moviePlayerPeer, time);
                }
            }
        }

        @Override
        public int getDuration() {
            if(moviePlayerPeer != 0) {
                if(isVideo) {
                    return IOSNative.getMediaDuration(moviePlayerPeer);
                } else {
                    return IOSNative.getAudioDuration(moviePlayerPeer);
                }
            }
            return 0;
        }

        @Override
        public void setVolume(int vol) {
            IOSNative.setVolume(((float)vol) / 100);
        }

        @Override
        public int getVolume() {
            return (int)(IOSNative.getVolume() * 100);
        }

        @Override
        public boolean isPlaying() {
            if(moviePlayerPeer != 0) {
                if(isVideo) {
                    return IOSNative.isVideoPlaying(moviePlayerPeer);
                } else {
                    return IOSNative.isAudioPlaying(moviePlayerPeer);
                }
            }
            return false;
        }

        @Override
        public Component getVideoComponent() {
            if(uri != null) {
                moviePlayerPeer = IOSNative.createVideoComponent(uri);
                component = PeerComponent.create(new long[] { IOSNative.getVideoViewPeer(moviePlayerPeer) });
            } else {
                try {
                    byte[] data = Util.readInputStream(stream);
                    Util.cleanup(stream);
                    moviePlayerPeer = IOSNative.createVideoComponent(data);
                    component = PeerComponent.create(new long[] { IOSNative.getVideoViewPeer(moviePlayerPeer) });
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
                return IOSNative.isVideoFullScreen(p);
            }
            return false;
        }

        @Override
        public void setFullScreen(boolean fullScreen) {
            this.fullScreen = fullScreen;
            long p = get(component);
            if(p != 0) {
                IOSNative.setVideoFullScreen(p, fullScreen);
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
    }
    
    public Media createMedia(String uri, boolean isVideo, Runnable onCompletion) throws IOException {
        return new IOSMedia(uri, isVideo, onCompletion);
    }


    public Media createMedia(InputStream stream, String mimeType, Runnable onCompletion) throws IOException {
        return new IOSMedia(stream, mimeType, onCompletion);
    }
    
    private static byte[] loadResource(String name, String type) {
        return IOSNative.loadResource(name, type);
    }
    
    private static long createNativeMutableImage(int w, int h, int color) {
        return IOSNative.createNativeMutableImage(w, h, color);
    }

    // should delete the old peer!
    private static void startDrawingOnImage(int w, int h, long peer) {
        IOSNative.startDrawingOnImage(w, h, peer);
    }
    private static long finishDrawingOnImage() {
        return IOSNative.finishDrawingOnImage();
    }

    private static void deleteNativePeer(long peer) {
        IOSNative.deleteNativePeer(peer);
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

    public void rotate(Object nativeGraphics, float angle) {
        ((NativeGraphics)nativeGraphics).rotate(angle);
    }

    public void rotate(Object nativeGraphics, float angle, int x, int y) {
        ((NativeGraphics)nativeGraphics).rotate(angle, x, y);
    }

    public void shear(Object nativeGraphics, float x, float y) {
        ((NativeGraphics)nativeGraphics).shear(x, y);
    }


    class NativeGraphics {
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
            IOSNative.fillRectRadialGradientMutable(startColor, endColor, x, y, width, height, relativeX, relativeY, relativeSize);
        }
    
        public void fillLinearGradient(int startColor, int endColor, int x, int y, int width, int height, boolean horizontal) {
            IOSNative.fillLinearGradientMutable(startColor, endColor, x, y, width, height, horizontal);
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
            IOSNative.resetAffineGlobal();
        }

        public void scale(float x, float y) {
            IOSNative.scaleGlobal(x, y);
        }

        public void rotate(float angle) {
            IOSNative.rotateGlobal(angle);
        }

        public void rotate(float angle, int x, int y) {
            IOSNative.rotateGlobal(angle, x, y);
        }

        public void shear(float x, float y) {
            IOSNative.shearGlobal(x, y);
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
            IOSNative.fillRectRadialGradientGlobal(startColor, endColor, x, y, width, height, relativeX, relativeY, relativeSize);
        }
    
        public void fillLinearGradient(int startColor, int endColor, int x, int y, int width, int height, boolean horizontal) {
            IOSNative.fillLinearGradientGlobal(startColor, endColor, x, y, width, height, horizontal);
        }
    }

    class NativeFont {
        long peer;
        int style;
        int face;
        int size;
        
        public NativeFont() {
        }
        
        public boolean equals(Object o) {
            NativeFont f = (NativeFont)o;
            return f.style == style && f.face == face && f.size == size;
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

        protected void finalize() {
            if(peer != 0) {
                deleteNativePeer(peer);
                peer = 0;
            }
        }
    }

    @Override
    public boolean animateImage(Object nativeImage, long lastFrame) {
        return super.animateImage(nativeImage, lastFrame);
    }

    @Override
    public void browserBack(PeerComponent browserPeer) {
        IOSNative.browserBack(get(browserPeer));
    }

    @Override
    public void browserStop(PeerComponent browserPeer) {
        IOSNative.browserStop(get(browserPeer));
    }
    
    @Override
    public void browserClearHistory(PeerComponent browserPeer) {
        IOSNative.browserClearHistory(get(browserPeer));
    }

    @Override
    public void browserExecute(PeerComponent browserPeer, String javaScript) {
        IOSNative.browserExecute(get(browserPeer), javaScript);
    }

    @Override
    public void browserExposeInJavaScript(PeerComponent browserPeer, Object o, String name) {
        // TODO
    }

    @Override
    public void browserForward(PeerComponent browserPeer) {
        IOSNative.browserForward(get(browserPeer));
    }

    @Override
    public boolean browserHasBack(PeerComponent browserPeer) {
        return IOSNative.browserHasBack(get(browserPeer));
    }

    @Override
    public boolean browserHasForward(PeerComponent browserPeer) {
        return IOSNative.browserHasForward(get(browserPeer));
    }

    @Override
    public void browserReload(PeerComponent browserPeer) {
        IOSNative.browserReload(get(browserPeer));
    }

    @Override
    public boolean canForceOrientation() {
        return super.canForceOrientation();
    }

    /*@Override
    public void playAudio(Object handle) {
        long[] l = (long[])handle;
        if(l[0] == 0) {
            return;
        }
        IOSNative.playAudio(l[0]);
    }

    @Override
    public void cleanupAudio(Object handle) {
        long[] l = (long[])handle;
        if(l[0] == 0) {
            return;
        }
        l[0] = 0;
        IOSNative.cleanupAudio(l[0]);
    }*/

    @Override
    public int convertToPixels(int dipCount, boolean horizontal) {
        return super.convertToPixels(dipCount, horizontal);
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
        long[] p = new long[] {IOSNative.createAudio(uri, c)};
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
        long[] p = new long[] {IOSNative.createAudio(bo.toByteArray(), c)};
        c.peer = p;
        c.onCompletion = onCompletion;
        return p;
    }*/

    @Override
    public PeerComponent createBrowserComponent(Object browserComponent) {
        long browserPeer = IOSNative.createBrowserComponent(browserComponent);
        PeerComponent pc = createNativePeer(new long[] {browserPeer});
        IOSNative.releasePeer(browserPeer);
        return pc;
    }

    /*@Override
    public VideoComponent createVideoPeer(String url) throws IOException {
        return new NativeIPhoneVideoPeer(new long[] {IOSNative.createVideoComponent(url)});
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
            IOSNative.retainPeer(this.nativePeer[0]);
        }
        
        public void finalize() {
            if(nativePeer[0] != 0) {
                IOSNative.releasePeer(nativePeer[0]);            
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
            IOSNative.calcPreferredSize(nativePeer[0], getDisplayWidth(), getDisplayHeight(), p);
            return new Dimension(p[0], p[1]);
        }

        protected void onPositionSizeChange() {
            if(nativePeer != null && nativePeer[0] != 0) {
                IOSNative.updatePeerPositionSize(nativePeer[0], getAbsoluteX(), getAbsoluteY(), getWidth(), getHeight());
            }
        }

        protected void initComponent() {
            if(nativePeer != null && nativePeer[0] != 0) {
                IOSNative.peerInitialized(nativePeer[0], getAbsoluteX(), getAbsoluteY(), getWidth(), getHeight());
            }
        }

        protected void deinitialize() {
            if(nativePeer != null && nativePeer[0] != 0) {
                IOSNative.peerDeinitialized(nativePeer[0]);
            }
        }

        @Override
        public void start() {
            if(nativePeer != null && nativePeer[0] != 0) {
                IOSNative.startVideoComponent(nativePeer[0]);
            }
        }

        @Override
        public void stop() {
            if(nativePeer != null && nativePeer[0] != 0) {
                IOSNative.startVideoComponent(nativePeer[0]);
            }
        }

        @Override
        public void setLoopCount(int count) {
        }

        @Override
        public int getMediaTimeMS() {
            if(nativePeer != null && nativePeer[0] != 0) {
                return IOSNative.getMediaTimeMS(nativePeer[0]);
            }
            return -1;
        }

        @Override
        public int setMediaTimeMS(int now) {
            if(nativePeer != null && nativePeer[0] != 0) {
                return IOSNative.setMediaTimeMS(nativePeer[0], now);
            }
            return -1;
        }

        @Override
        public int getMediaDuration() {
            if(nativePeer != null && nativePeer[0] != 0) {
                return IOSNative.getMediaDuration(nativePeer[0]);
            }
            return -1;
        }

        @Override
        public boolean isPlaying() {
            if(nativePeer != null && nativePeer[0] != 0) {
                return IOSNative.isVideoPlaying(nativePeer[0]);
            }
            return false;
        }

        @Override
        public void setFullScreen(boolean fullscreen) {
            if(nativePeer != null && nativePeer[0] != 0) {
                IOSNative.setVideoFullScreen(nativePeer[0], fullscreen);
            }
        }

        @Override
        public boolean isFullScreen() {
            if(nativePeer != null && nativePeer[0] != 0) {
                return IOSNative.isVideoFullScreen(nativePeer[0]);
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
        IOSNative.execute(url);
    }

    @Override
    public void flashBacklight(int duration) {
        IOSNative.flashBacklight(duration);
    }

    /*@Override
    public int getAudioDuration(Object handle) {
        long[] l = (long[])handle;
        if(l[0] == 0) {
            return -1;
        }
        return IOSNative.getAudioDuration(l[0]);
    }

    @Override
    public int getAudioTime(Object handle) {
        long[] l = (long[])handle;
        if(l[0] == 0) {
            return -1;
        }
        return IOSNative.getAudioTime(l[0]);
    }*/

    @Override
    public String getBrowserTitle(PeerComponent browserPeer) {
        return IOSNative.getBrowserTitle(get(browserPeer));
    }

    @Override
    public String getBrowserURL(PeerComponent browserPeer) {
        return IOSNative.getBrowserURL(get(browserPeer));
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
    public static void fireWebViewError(BrowserComponent bc) {
        bc.fireWebEvent("onError", new ActionEvent(new Integer(-1)));
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
        if(key.equalsIgnoreCase("OS")) {
            return "iOS";
        }
        if(key.equalsIgnoreCase("User-Agent")) {
            if(isTablet()) {
                return "Mozilla/5.0 (iPad; U; CPU OS 3_2 like Mac OS X; en-us) AppleWebKit/531.21.10 (KHTML, like Gecko) Version/4.0.4 Mobile/7B334b Safari/531.21.10";
            } 
            return "Mozilla/5.0 (iPhone; U; CPU like Mac OS X; en) AppleWebKit/420+ (KHTML, like Gecko) Version/3.0 Mobile/1C25 Safari/419.3";
        }
        if(key.equalsIgnoreCase("UDID")) {
            return IOSNative.getUDID();
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
        return (int)(IOSNative.getVolume() * 100);
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
        return IOSNative.isMinimized();
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
        IOSNative.sendEmailMessage(recieptents[0], subject, msg.getContent(), msg.getAttachment(), msg.getMimeType());
    }
    
    @Override
    public String[] getAllContacts(boolean withNumbers) {
        int[] c = new int[IOSNative.getContactCount(withNumbers)];
        IOSNative.getContactRefIds(c, withNumbers);
        String[] r = new String[c.length];
        for(int iter = 0 ; iter < c.length ; iter++) {
            r[iter] = "" + c[iter];
        }
        return r;
    }

    @Override
    public Contact getContactById(String id) {
        int recId = Integer.parseInt(id);
        Contact c = new Contact();
        c.setId("" + id);
        long person = IOSNative.getPersonWithRecordID(recId);
        c.setFirstName(IOSNative.getPersonFirstName(person));
        c.setFamilyName(IOSNative.getPersonSurnameName(person));
        
        c.setPrimaryEmail(IOSNative.getPersonEmail(person));
        
        int phones = IOSNative.getPersonPhoneCount(person);
        Hashtable h = new Hashtable();
        for(int iter = 0 ; iter < phones ; iter++) {
            h.put(IOSNative.getPersonPhoneType(person, iter), IOSNative.getPersonPhone(person, iter));
        }
        c.setPhoneNumbers(h);
        
        c.setPrimaryPhoneNumber(IOSNative.getPersonPrimaryPhone(person));
        
        h = new Hashtable();
        h.put("Work", h);
        c.setAddresses(h);
        IOSNative.releasePeer(person);
        return c;
    }
    
    @Override
    public void dial(String phoneNumber) {        
        IOSNative.dial("tel:" + phoneNumber);
    }

    @Override
    public void sendSMS(String phoneNumber, String message) throws IOException{
        IOSNative.sendSMS(phoneNumber, message);
    }

    @Override
    public boolean isTrueTypeSupported() {
        // TODO
        return super.isTrueTypeSupported();
    }

    @Override
    public Object loadNativeFont(String lookup) {
        // TODO
        return super.loadNativeFont(lookup);
    }

    @Override
    public Object loadTrueTypeFont(InputStream stream) throws IOException {
        // TODO
        return super.loadTrueTypeFont(stream);
    }

    @Override
    public void lockOrientation(boolean portrait) {
        IOSNative.lockOrientation(portrait);
    }

    @Override
    public boolean minimizeApplication() {
        return IOSNative.minimizeApplication();
    }

    /*@Override
    public void pauseAudio(Object handle) {
        long[] l = (long[])handle;
        if(l[0] == 0) {
            return;
        }
        IOSNative.pauseAudio(l[0]);
    }*/

    @Override
    public void restoreMinimizedApplication() {
        IOSNative.restoreMinimizedApplication();
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
        IOSNative.setAudioTime(l[0], time);
    }*/

    private long get(PeerComponent p) {
        if(p == null) return 0;
        long[] l = (long[])p.getNativePeer();
        return l[0];
    }
    
    @Override
    public void setBrowserPage(PeerComponent browserPeer, String html, String baseUrl) {
        if(baseUrl != null && baseUrl.startsWith("jar://")) {
            baseUrl = "file://localhost" + IOSNative.getResourcesDir().replace(" ", "%20") + baseUrl.substring(6);
        }
        IOSNative.setBrowserPage(get(browserPeer), html, baseUrl);
    }

    @Override
    public void setBrowserProperty(PeerComponent browserPeer, String key, Object value) {
    }

    @Override
    public void setBrowserURL(PeerComponent browserPeer, String url) {
        if(url.startsWith("jar://")) {
            url = "file://localhost" + IOSNative.getResourcesDir().replace(" ", "%20") + url.substring(6);
        }
        IOSNative.setBrowserURL(get(browserPeer), url);
    }

    @Override
    public void setBuiltinSoundsEnabled(boolean enabled) {
        // TODO
        super.setBuiltinSoundsEnabled(enabled);
    }

    /*@Override
    public void setVolume(int vol) {
        IOSNative.setVolume(((float)vol) / 100.0f);
    }*/

    @Override
    public void showNativeScreen(Object nativeFullScreenPeer) {
        // TODO
        super.showNativeScreen(nativeFullScreenPeer);
    }

    @Override
    public void vibrate(int duration) {
        IOSNative.vibrate(duration);
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
            IOSNative.retainPeer(this.nativePeer[0]);
        }
        
        public void finalize() {
            if(nativePeer[0] != 0) {
                IOSNative.releasePeer(nativePeer[0]);            
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
            IOSNative.calcPreferredSize(nativePeer[0], getDisplayWidth(), getDisplayHeight(), p);
            return new Dimension(p[0], p[1]);
        }

        protected void onPositionSizeChange() {
            if(nativePeer != null && nativePeer[0] != 0) {
                IOSNative.updatePeerPositionSize(nativePeer[0], getAbsoluteX(), getAbsoluteY(), getWidth(), getHeight());
            }
        }

        protected void initComponent() {
            if(nativePeer != null && nativePeer[0] != 0) {
                IOSNative.peerInitialized(nativePeer[0], getAbsoluteX(), getAbsoluteY(), getWidth(), getHeight());
            }
        }

        protected void deinitialize() {
            if(nativePeer != null && nativePeer[0] != 0) {
                IOSNative.peerDeinitialized(nativePeer[0]);
            }
        }
        
        protected void setLightweightMode(boolean l) {
            if(nativePeer != null && nativePeer[0] != 0) {
                if(lightweightMode != l) {
                    lightweightMode = l;
                    IOSNative.peerSetVisible(nativePeer[0], !lightweightMode);
                    getComponentForm().repaint();
                }
            }
        }
        
    }

    public boolean areMutableImagesFast() {
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
        NetworkConnection n = connections.get(peer);
        synchronized(n.LOCK) {
            n.appendData(data);
            n.connected = true;
            n.LOCK.notifyAll();
        }
    }
    
    public static void streamComplete(long peer) {
        NetworkConnection n = connections.get(peer);
        synchronized(n.LOCK) {
            n.connected = true;
            n.streamComplete();
            n.LOCK.notifyAll();
        }
    }
    
    public static void networkError(long peer, String error) {
        NetworkConnection n = connections.get(peer);
        synchronized(n.LOCK) {
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
        public final Object LOCK = new Object();
        
        public void ensureConnection() {
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
                    IOSNative.setBody(peer, body.toByteArray());
                    body = null;
                }
                IOSNative.connect(peer);
                while(!connected) {
                    try {
                        LOCK.wait();
                    } catch (InterruptedException ex) {
                    }
                }
            }
        }
        
        public NetworkConnection(long peer) {
            this.peer = peer;
            connections.put(peer, this);
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
                return val;
            }
        }

        @Override
        public int available() throws IOException {
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
                IOSNative.closeConnection(peer);
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
    
    /**
     * @inheritDoc
     */
    public Object connect(String url, boolean read, boolean write) throws IOException {
        return new NetworkConnection(IOSNative.openConnection(url, timeout));
    }

    /**
     * @inheritDoc
     */
    public void setHeader(Object connection, String key, String val) {
        IOSNative.addHeader(((NetworkConnection)connection).peer, key, val);
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
    public void setPostRequest(Object connection, boolean p) {
        NetworkConnection n = (NetworkConnection)connection;
        if(p) {
            IOSNative.setMethod(n.peer, "POST");
        } else {
            IOSNative.setMethod(n.peer, "GET");
        }
    }

    /**
     * @inheritDoc
     */
    public int getResponseCode(Object connection) throws IOException {
        NetworkConnection n = (NetworkConnection)connection;
        n.ensureConnection();
        return IOSNative.getResponseCode(n.peer);
    }

    /**
     * @inheritDoc
     */
    public String getResponseMessage(Object connection) throws IOException {
        NetworkConnection n = (NetworkConnection)connection;
        n.ensureConnection();
        return IOSNative.getResponseMessage(n.peer);
    }

    /**
     * @inheritDoc
     */
    public int getContentLength(Object connection) {
        NetworkConnection n = (NetworkConnection)connection;
        n.ensureConnection();
        return IOSNative.getContentLength(n.peer);
    }

    /**
     * @inheritDoc
     */
    public String getHeaderField(String name, Object connection) throws IOException {
        NetworkConnection n = (NetworkConnection)connection;
        n.ensureConnection();
        return IOSNative.getResponseHeader(n.peer, name);
    }

    /**
     * @inheritDoc
     */
    public String[] getHeaderFieldNames(Object connection) throws IOException {
        NetworkConnection n = (NetworkConnection)connection;
        n.ensureConnection();
        String[] s = new String[IOSNative.getResponseHeaderCount(n.peer)];
        for(int iter = 0 ; iter < s.length ; iter++) {
            s[iter] = IOSNative.getResponseHeaderName(n.peer, iter);
        }
        return s;
    }

    /**
     * @inheritDoc
     */
    public String[] getHeaderFields(String name, Object connection) throws IOException {
        NetworkConnection n = (NetworkConnection)connection;
        n.ensureConnection();
        String s = IOSNative.getResponseHeader(n.peer, name);
        if(s == null) {
            return null;
        }
        return s.split(",");
    }

    /**
     * @inheritDoc
     */
    public void deleteStorageFile(String name) {
        IOSNative.deleteFile(IOSNative.getCachesDir() + "/" + name);
    }

    /**
     * @inheritDoc
     */
    public OutputStream createStorageOutputStream(String name) throws IOException {
        return new NSDataOutputStream(IOSNative.getCachesDir() + "/" + name);
    }

    /**
     * @inheritDoc
     */
    public InputStream createStorageInputStream(String name) throws IOException {
        return new NSDataInputStream(IOSNative.getCachesDir() + "/" + name);
    }

    /**
     * @inheritDoc
     */
    public boolean storageFileExists(String name) {
        return IOSNative.fileExists(IOSNative.getCachesDir() + "/" + name);
    }

    /**
     * @inheritDoc
     */
    public String[] listStorageEntries() {
        String c = IOSNative.getCachesDir();
        String[] a = new String[IOSNative.fileCountInDir(c)];
        IOSNative.listFilesInDir(c, a);
        return a;
    }

    /**
     * @inheritDoc
     */
    public String[] listFilesystemRoots() {
        String[] roots = new String[] {
            IOSNative.getCachesDir(),
            IOSNative.getDocumentsDir(),
            IOSNative.getResourcesDir()
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
        String[] a = new String[IOSNative.fileCountInDir(directory)];
        IOSNative.listFilesInDir(directory, a);
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
        IOSNative.createDirectory(directory);
    }

    /**
     * @inheritDoc
     */
    public void deleteFile(String file) {
        IOSNative.deleteFile(file);
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
        return IOSNative.getFileSize(file);
    }

    /**
     * @inheritDoc
     */
    public boolean isDirectory(String file) {
        return IOSNative.isDirectory(file);
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
        return new NSDataOutputStream(file);
    }

    /**
     * @inheritDoc
     */
    public InputStream openFileInputStream(String file) throws IOException {
        if(file.startsWith("file:/")) {
            file = file.substring(5);
        }
        return new NSDataInputStream(file);
    }

    /**
     * @inheritDoc
     */
    public boolean exists(String file) {
        return IOSNative.fileExists(file);
    }

    /**
     * @inheritDoc
     */
    public void rename(String file, String newName) {
        IOSNative.moveFile(file, newName);
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
    public void registerPush(String id, boolean noFallback) {
        IOSNative.registerPush();
    }

    @Override
    public void deregisterPush() {
        IOSNative.deregisterPush();
    }

    private static PushCallback pushCallback;
    
    public static void pushReceived(final String message) {
        if(pushCallback != null) {
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    pushCallback.push(message);
                }
            });
        }
    }
    public static void pushRegistered(final String deviceKey) {
        System.out.println("Push handleRegistration() Sending registration to server!");
        String clsName = callback.getClass().getName();
        clsName = clsName.substring(0, clsName.lastIndexOf('.'));
        CodenameOneImplementation.registerPushOnServer(deviceKey, getApplicationKey(), (byte)2, "", clsName);
        if(pushCallback != null) {
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    pushCallback.registeredForPush(deviceKey);
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
    
    private L10NManager l10n;

    /**
     * @inheritDoc
     */
    public L10NManager getLocalizationManager() {
        if(l10n == null) {
            Locale l = Locale.getDefault();
            l10n = new L10NManager(l.getLanguage(), l.getCountry()) {
                public String format(int number) {
                    return NumberFormat.getNumberInstance().format(number);
                }

                public String format(double number) {
                    return NumberFormat.getNumberInstance().format(number);
                }

                public String formatCurrency(double currency) {
                    return NumberFormat.getCurrencyInstance().format(currency);
                }

                public String formatDateLongStyle(Date d) {
                    return DateFormat.getDateInstance(DateFormat.LONG).format(d);
                }

                public String formatDateShortStyle(Date d) {
                    return DateFormat.getDateInstance(DateFormat.SHORT).format(d);
                }

                public String formatDateTime(Date d) {
                    return DateFormat.getDateTimeInstance().format(d);
                }

                public String getCurrencySymbol() {
                    return NumberFormat.getInstance().getCurrency().getSymbol();
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
                    long p = IOSNative.createImageFile(ni.peer, format == FORMAT_JPEG, width, height, quality);
                    writeNSData(p, response);
                }

                private void writeNSData(long p, OutputStream os) throws IOException {
                    byte[] b = new byte[IOSNative.getNSDataSize(p)];
                    IOSNative.nsDataToByteArray(p, b);
                    IOSNative.releasePeer(p);
                    os.write(b);
                }
                
                @Override
                protected void saveImage(Image img, OutputStream response, String format, float quality) throws IOException {
                    NativeImage ni = (NativeImage)img.getImage();
                    long p = IOSNative.createImageFile(ni.peer, format == FORMAT_JPEG, img.getWidth(), img.getHeight(), quality);
                    writeNSData(p, response);
                }

                @Override
                public boolean isFormatSupported(String format) {
                    return format == FORMAT_JPEG || format == FORMAT_PNG;
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
    public void deleteDB(String databaseName) throws IOException{
        IOSNative.sqlDbDelete(databaseName);
    }
    
    @Override
    public boolean existsDB(String databaseName){
        return IOSNative.sqlDbExists(databaseName);
    }
    
}
