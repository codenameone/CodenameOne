// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.browser;

/*
import com.codename1.impl.javase.JavaSEPort;
import com.codename1.impl.javase.JavaSEPort.CN1JPanel;
import com.codename1.impl.javase.PeerComponentBuffer;
import com.codename1.ui.CN;
*/
import com.codename1.impl.javase.cef.CEFUIPlatform;
import org.cef.CefClient;
import org.cef.callback.CefDragData;
import org.cef.handler.CefRenderHandler;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
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
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;
import javax.swing.JPanel;

import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import org.cef.OS;

/**
 * This class represents an off-screen rendered browser.
 * The visibility of this class is "package". To create a new
 * CefBrowser instance, please use CefBrowserFactory.
 */
public class CN1CefBrowser extends CefBrowser_N implements CefRenderHandler {
    private CefRenderer renderer_;
    //private GLCanvas canvas_;
    private long window_handle_ = 0;
    private Rectangle browser_rect_ = new Rectangle(0, 0, 1, 1); // Work around CEF issue #1437.
    private Point screenPoint_ = new Point(0, 0);
    private boolean isTransparent_;
    private JPanel component_;
    //private BufferedImage bufferedImage_;
    private PixelBuffer buffer_;
    private byte[] buf_;
    

    CN1CefBrowser(CefClient client, String url, boolean transparent, CefRequestContext context) {
        this(client, url, transparent, context, null, null);
    }

    private CN1CefBrowser(CefClient client, String url, boolean transparent,
            CefRequestContext context, CN1CefBrowser parent, Point inspectAt) {
        super(client, url, context, parent, inspectAt);
        isTransparent_ = transparent;
        renderer_ = new CefRenderer(transparent);
        createComponent();
    }
    
    public void setPeerComponentBuffer(PixelBuffer buf) {
        buffer_ = buf;
    }
   
    @Override
    public void createImmediately() {
        // Create the browser immediately.
        createBrowserIfRequired(false);
    }

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

    private static long getWindowHandle(Component component) {
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
                browser_rect_.setBounds(x, y, w, h);
                screenPoint_ = component_.getLocationOnScreen();
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
                System.out.println("Key typed "+e);
                sendKeyEvent(e);
            }

            @Override
            public void keyPressed(KeyEvent e) {
                System.out.println("Key pressed"+e);
                sendKeyEvent(e);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                System.out.println("Key released "+e);
                sendKeyEvent(e);
            }
        });

        component_.setFocusable(true);
        component_.addFocusListener(new FocusListener() {
            @Override
            public void focusLost(FocusEvent e) {
                setFocus(false);
            }

            @Override
            public void focusGained(FocusEvent e) {
                // Dismiss any Java menus that are currently displayed.
                MenuSelectionManager.defaultManager().clearSelectedPath();
                setFocus(true);
            }
        });

        // Connect the Canvas with a drag and drop listener.
        new DropTarget(component_, new CN1CefDropTargetListener(this));
    }
    
    
    @Override
    public Rectangle getViewRect(CefBrowser browser) {
        return browser_rect_;
    }

    @Override
    public Point getScreenPoint(CefBrowser browser, Point viewPoint) {
        Point screenPoint = new Point(screenPoint_);
        screenPoint.translate(viewPoint.x, viewPoint.y);
        return screenPoint;
    }

    @Override
    public void onPopupShow(CefBrowser browser, boolean show) {
        if (!show) {
            renderer_.clearPopupRects();
            invalidate();
        }
    }

    @Override
    public void onPopupSize(CefBrowser browser, Rectangle size) {
        renderer_.onPopupSize(size);
    }

    private boolean firstPaint=true;
    
    @Override
    public void onPaint(CefBrowser browser, boolean popup, final Rectangle[] dirtyRects,
            ByteBuffer buffer, int width, int height) {
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
        System.out.println("CN1CefBrowser::loadURL("+url+")");
        super.loadURL(url);
        System.out.println("After CN1CefBrowser::loadURL("+url+")");
    }

    public static void setUIPlatform(UIPlatform p) {
        platform = p;
    }
    
    public static void setComponentFactory(ComponentFactory factory) {
        componentFactory = factory;
    }
    
    
}
