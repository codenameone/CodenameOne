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
package org.cef.browser;

/*
import com.codename1.impl.javase.JavaSEPort;
import com.codename1.impl.javase.JavaSEPort.CN1JPanel;
import com.codename1.impl.javase.PeerComponentBuffer;
import com.codename1.ui.CN;
*/
import org.cef.CefClient;
import org.cef.callback.CefDragData;
import org.cef.handler.CefRenderHandler;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.dnd.DropTarget;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import org.cef.OS;
import org.cef.handler.CefScreenInfo;

/**
 * This class represents an off-screen rendered browser.
 * The visibility of this class is "package". To create a new
 * CefBrowser instance, please use CefBrowserFactory.
 */
public class CN1CefBrowser extends CefBrowser_N implements CefRenderHandler {
    //private CefRenderer renderer_;
    //private GLCanvas canvas_;
    private long window_handle_ = 0;
    private Rectangle browser_rect_ = new Rectangle(0, 0, 1, 1); // Work around CEF issue #1437.
    private Point screenPoint_ = new Point(0, 0);
    private boolean isTransparent_;
    private JPanel component_;
    //private BufferedImage bufferedImage_;
    private WeakReference<PixelBuffer> bufferRef;
    private byte[] buf_;
    

    CN1CefBrowser(CefClient client, String url, boolean transparent, CefRequestContext context) {
        this(client, url, transparent, context, null, null);
    }

    private CN1CefBrowser(CefClient client, String url, boolean transparent,
            CefRequestContext context, CN1CefBrowser parent, Point inspectAt) {
        super(client, url, context, parent, inspectAt);
        isTransparent_ = transparent;
        //renderer_ = new CefRenderer(transparent);
        createComponent();
    }
    
    /**
     * Sets the buffer to which the browser should draw to.
     * @param buf 
     */
    public void setPeerComponentBuffer(PixelBuffer buf) {
        bufferRef = new WeakReference<PixelBuffer>(buf);
    }
   
    @Override
    public void createImmediately() {
        // Create the browser immediately.
        createBrowserIfRequired(false);
    }

    /**
     * Gets the swing component that the CEF browser uses for receiving events.
     * @return 
     */
    @Override
    public Component getUIComponent() {
        //return canvas_;
        return component_;
    }

    @Override
    public CefRenderHandler getRenderHandler() {
        return this;
    }

    @Override
    protected CefBrowser_N createDevToolsBrowser(CefClient client, String url,
            CefRequestContext context, CefBrowser_N parent, Point inspectAt) {
        return new CN1CefBrowser(
                client, url, isTransparent_, context, (CN1CefBrowser) this, inspectAt);
    }
    
    private synchronized long getWindowHandle() {
        if (window_handle_ == 0 && OS.isMacintosh()) {
            window_handle_ = getWindowHandle(component_);
        }
        return window_handle_;
    }

    /**
     * Pulled this straight from the CEF examples.  Looks like on Mac, it needs to keep the window
     * handle.
     * @param component
     * @return 
     */
    private static long getWindowHandle(Component component) {
        try {
            if (OS.isMacintosh()) {
                try {
                    Class<?> cls = Class.forName("org.cef.browser.mac.CefBrowserWindowMac");
                    CefBrowserWindow browserWindow = (CefBrowserWindow) cls.newInstance();
                    if (browserWindow != null) {
                        return browserWindow.getWindowHandle(component);
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            return 0;
        } catch (Throwable t) {
            System.out.println("Exception in thread "+Thread.currentThread().getName());
            t.printStackTrace();
            return 0;
        }
    }

    /*
    private synchronized long getWindowHandle() {
        if (window_handle_ == 0) {
            NativeSurface surface = canvas_.getNativeSurface();
            if (surface != null) {
                surface.lockSurface();
                window_handle_ = getWindowHandle(surface.getSurfaceHandle());
                surface.unlockSurface();
                assert (window_handle_ != 0);
            }
        }
        return window_handle_;
    }
    */

    private static ComponentFactory componentFactory;
    private static UIPlatform platform;
    
    
    
    private void createComponent() {
        component_ = componentFactory.createComponent(new ComponentDelegate() {
            @Override
            public void boundsChanged(int x, int y, int w, int h) {
                if (browser_rect_.getX() == x && browser_rect_.getY() == y && browser_rect_.getWidth() == w && browser_rect_.getHeight() == h) {
                    return;
                }
                browser_rect_.setBounds(x, y, w, h);
                try {
                    screenPoint_ = component_.getLocationOnScreen();
                } catch (IllegalComponentStateException ex) {
                    System.err.println("Failed to get location on screen:"+ex.getMessage());
                    screenPoint_ = new Point(0,0);
                }
                wasResized(w, h);
            }

            @Override
            public void wasResized(int width, int height) {
                CN1CefBrowser.this.wasResized(width, height);
            }

            @Override
            public void createBrowserIfRequired(boolean b) {
                CN1CefBrowser.this.createBrowserIfRequired(b);
            }
            
        });
        
        
        
        component_.addMouseListener(new MouseListener() {
            @Override
            public void mousePressed(MouseEvent e) {
                sendMouseEvent(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                sendMouseEvent(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                sendMouseEvent(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                sendMouseEvent(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                sendMouseEvent(e);
            }
        });

        component_.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseMoved(MouseEvent e) {
                sendMouseEvent(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                sendMouseEvent(e);
            }
        });

        component_.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                final int units = platform.convertToPixels(e.getUnitsToScroll(), true) * -1;

                
                if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
                    MouseWheelEvent e2 = new MouseWheelEvent(
                            e.getComponent(),
                            e.getID(),
                            e.getWhen(),
                            e.getModifiers(),
                            e.getX(),
                            e.getY(),
                            e.getClickCount(),
                            e.isPopupTrigger(),
                            MouseWheelEvent.WHEEL_BLOCK_SCROLL,
                            e.getScrollAmount(),
                            units
                    );
                    sendMouseWheelEvent(e2);
                    return;
                }
                
                sendMouseWheelEvent(e);
            }
        });

        component_.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                sendKeyEvent(e);
            }

            @Override
            public void keyPressed(KeyEvent e) {
                sendKeyEvent(e);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                sendKeyEvent(e);
            }
        });

        component_.setFocusable(true);
        component_.addFocusListener(new FocusListener() {
            private boolean inFocusGained, inFocusLost;
            @Override
            public void focusLost(FocusEvent e) {
                if (inFocusLost) return;
                inFocusLost = true;
                try {
                    setFocus(false);
                } finally {
                    inFocusLost = false;
                }
            }

            @Override
            public void focusGained(FocusEvent e) {
                if (inFocusGained) {
                    return;
                }
                inFocusGained = true;
                try {
                    // Dismiss any Java menus that are currently displayed.
                    MenuSelectionManager.defaultManager().clearSelectedPath();
                    setFocus(true);
                } finally {
                    inFocusGained = false;
                }
                
            }
        });

        // Connect the Canvas with a drag and drop listener.
        new DropTarget(component_, new CN1CefDropTargetListener(this));
    }

    private boolean inSetFocus;
    
    @Override
    public void setFocus(boolean enable) {
        if (inSetFocus) {
            return;
        }
        inSetFocus = true;
        try {
            super.setFocus(enable);
        } finally {
            inSetFocus = false;
        }
    }
    
    
    
    
    @Override
    public Rectangle getViewRect(CefBrowser browser) {
        return browser_rect_;
    }

    @Override
    public Point getScreenPoint(CefBrowser browser, Point viewPoint) {
        try {
            Point screenPoint = new Point(screenPoint_);
            screenPoint.translate(viewPoint.x, viewPoint.y);
            return screenPoint;
        } catch (Throwable t) {
            System.out.println("Exception in thread "+Thread.currentThread().getName());
            t.printStackTrace();
            return new Point(screenPoint_);
        }
    }

    @Override
    public void onPopupShow(CefBrowser browser, boolean show) {
        try {
            if (!show) {
                //renderer_.clearPopupRects();
                invalidate();
            }
        } catch (Throwable t) {
            System.out.println("Exception in thread "+Thread.currentThread().getName());
            t.printStackTrace();
        }
    }

    @Override
    public void onPopupSize(CefBrowser browser, Rectangle size) {
        try {
            //renderer_.onPopupSize(size);
        } catch (Throwable t) {
            System.out.println("Exception in thread "+Thread.currentThread().getName());
            t.printStackTrace();
        }
    }

    private boolean firstPaint=true;
    
    @Override
    public void onPaint(final CefBrowser browser, final boolean popup, final Rectangle[] dirtyRects,
            final ByteBuffer buffer, final int width, final int height) {
        try {
            _onPaint(browser, popup, dirtyRects, buffer, width, height);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    public void _onPaint(final CefBrowser browser, final boolean popup, final Rectangle[] dirtyRects,
            final ByteBuffer buffer, final int width, final int height) {    
        final PixelBuffer buffer_ = bufferRef.get();
        if (buffer_ == null) {
            return;
        }
        BufferedImage img = buffer_.getBufferedImage();
        boolean imgUpdated = false;
         if (img == null || img.getWidth() != width || img.getHeight() != height) {
            img = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
            imgUpdated = true;
        }
        if (buf_ == null || buf_.length != width * height * 4) {
            buf_ = new byte[width * height * 4];
        }
        final BufferedImage fImg = img;
        final boolean fImgUpdated = imgUpdated;
        if (imgUpdated || firstPaint ) {
            firstPaint = false;
            buffer_.modifyBuffer(new Runnable() {
                public void run() {
                    WritableRaster raster = fImg.getRaster();
                    buffer.get(buf_);
                    int len = width * height * 4;
                    for (int i=0; i<len; i+=4) {
                        byte tmp = buf_[i];
                        buf_[i] = buf_[i+2];
                        buf_[i+2] = tmp;
                    }
                    raster.setDataElements(0, 0, width, height, buf_);
                    if (fImgUpdated) {
                        buffer_.setBufferedImage(fImg);
                    }
                }
            });
            
            platform.runLater(new Runnable() {
                public void run() {
                    buffer_.repaint();
                }
            });
        } else {
            
            buffer_.modifyBuffer(new Runnable() {
                public void run() {
                    WritableRaster raster = fImg.getRaster();
                    for (Rectangle rect : dirtyRects) {
                        int dx = rect.x;
                        int dy = rect.y;
                        int rw = rect.width;
                        int rh = rect.height;
                        int dy2 = dy+rh;
                        for (int row=dy; row<dy2; row++) {
                            buffer.position(row * width * 4 + dx * 4);
                            buffer.get(buf_, rw * (row-dy) * 4, rw * 4);
                       }
                        int len = rw * rh * 4;
                        for (int i=0; i<len; i+=4) {
                            byte tmp = buf_[i];
                            buf_[i] = buf_[i+2];
                            buf_[i+2] = tmp;
                        }
                       raster.setDataElements(dx, dy, rw, rh, buf_);
                    }
                }
            });
            
            platform.runLater(new Runnable() {
                public void run() {
                    for (Rectangle rect : dirtyRects) {
                        buffer_.repaint(rect.x, rect.y, rect.width, rect.height);
                    }
                }
            });
        }
    }

    @Override
    public void onCursorChange(CefBrowser browser, final int cursorType) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                component_.setCursor(new Cursor(cursorType));
            }
        });
    }

    @Override
    public boolean startDragging(CefBrowser browser, CefDragData dragData, int mask, int x, int y) {
        // TODO(JCEF) Prepared for DnD support using OSR mode.
        return false;
    }

    @Override
    public void updateDragCursor(CefBrowser browser, int operation) {
        // TODO(JCEF) Prepared for DnD support using OSR mode.
    }

    private void createBrowserIfRequired(boolean hasParent) {
        long windowHandle = 0;
        if (hasParent) {
            windowHandle = getWindowHandle();
        }

        if (getNativeRef("CefBrowser") == 0) {
            if (getParentBrowser() != null) {
                createDevTools(getParentBrowser(), getClient(), windowHandle, true, isTransparent_,
                        null, getInspectAt());
            } else {
                createBrowser(getClient(), windowHandle, getUrl(), true, isTransparent_, null,
                        getRequestContext());
            }
        } else {
            // OSR windows cannot be reparented after creation.
            setFocus(true);
        }
    }

    @Override
    public void loadURL(String url) {
        
        super.loadURL(url);
        
    }

    public static void setUIPlatform(UIPlatform p) {
        platform = p;
    }
    
    public static void setComponentFactory(ComponentFactory factory) {
        componentFactory = factory;
    }

    @Override
    public boolean getScreenInfo(CefBrowser browser, CefScreenInfo screenInfo) {
        try {
            GraphicsDevice graphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            GraphicsConfiguration graphicsConfig = graphicsDevice.getDefaultConfiguration();

            screenInfo.Set(calcRetinaScale(),
                        graphicsConfig.getColorModel().getPixelSize(),
                        graphicsConfig.getColorModel().getComponentSize(0), 
                        false, 
                        graphicsConfig.getBounds(), graphicsConfig.getBounds());
        } catch (Throwable t) {
            System.out.println("Exception in thread "+Thread.currentThread().getName());
            t.printStackTrace();
            
        }
        return true;
    }
    
    private static double calcRetinaScale() {
        
        GraphicsDevice graphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        
        try {
            if (getJavaVersion() >= 9) {
                // JDK9 Doesn't like the old hack for getting the scale via reflection.
                // https://bugs.openjdk.java.net/browse/JDK-8172962
                GraphicsConfiguration graphicsConfig = graphicsDevice 
                        .getDefaultConfiguration(); 

                AffineTransform tx = graphicsConfig.getDefaultTransform(); 
                double scaleX = tx.getScaleX(); 
                double scaleY = tx.getScaleY(); 
                return Math.max(1.0, Math.min(scaleX, scaleY));
            } else {

                Field field = graphicsDevice.getClass().getDeclaredField("scale");
                if (field != null) {
                    field.setAccessible(true);
                    Object scale = field.get(graphicsDevice);
                    if (scale instanceof Integer && ((Integer) scale).intValue() >= 2) {
                        return ((Integer)scale).doubleValue();
                    }
                }
            }
        } catch (Throwable e) {
            //e.printStackTrace();
        }
        return 1.0;
    }
    
    private static int cachedJavaVersion=-1;
    /**
     * Returns the Java version as an int value.
     *
     * @return the Java version as an int value (8, 9, etc.)
     * @since 12130
     */
    private static int getJavaVersion() {
        if (cachedJavaVersion < 0) {

            String version = System.getProperty("java.version");
            if (version.startsWith("1.")) {
                version = version.substring(2);
            }
            // Allow these formats:
            // 1.8.0_72-ea
            // 9-ea
            // 9
            // 9.0.1
            int dotPos = version.indexOf('.');
            int dashPos = version.indexOf('-');
            if (dotPos < 0 && dashPos < 0) {
                cachedJavaVersion = Integer.parseInt(version);
                return cachedJavaVersion;
            }
            cachedJavaVersion = Integer.parseInt(version.substring(0,
                    dotPos > -1 ? dotPos : dashPos > -1 ? dashPos : 1));
            return cachedJavaVersion;
        }
        return cachedJavaVersion;
    }
    
    
}
