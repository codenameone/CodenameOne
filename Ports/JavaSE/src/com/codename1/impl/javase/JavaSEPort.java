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
package com.codename1.impl.javase;

import com.codename1.messaging.Message;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.VirtualKeyboard;
import com.codename1.impl.CodenameOneImplementation;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.EventDispatcher;
import com.codename1.ui.util.Resources;
import java.awt.AlphaComposite;
import java.awt.CheckboxMenuItem;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FontFormatException;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.FilenameFilter;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import com.codename1.io.BufferedInputStream;
import com.codename1.io.BufferedOutputStream;
import com.codename1.io.Storage;
import com.codename1.location.LocationManager;
import com.codename1.media.Media;
import com.codename1.ui.PeerComponent;
import java.awt.Container;
import java.awt.GridBagLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.RealizeCompleteEvent;
import javax.media.Time;
import javax.media.bean.playerbean.MediaPlayer;
import jmapps.ui.VideoPanel;

/**
 * An implementation of Codename One based on Java SE
 *
 * @author Shai Almog
 */
public class JavaSEPort extends CodenameOneImplementation {
    private static File baseResourceDir;
    private static final String DEFAULT_SKINS = "/iphone3gs.skin;/nexus.skin;/ipad.skin;/iphone4.skin;/android.skin;/feature_phone.skin;/torch.skin";
    private boolean touchDevice = true;
    private boolean rotateTouchKeysOnLandscape;
    private int keyboardType = Display.KEYBOARD_TYPE_UNKNOWN;
    private static int medianFontSize = 15;
    private static int smallFontSize = 11;
    private static int largeFontSize = 19;
    private static String fontFaceSystem = "Arial";
    private static String fontFaceProportional = "SansSerif";
    private static String fontFaceMonospace = "Monospaced";
    private static boolean useNativeInput = true;
    static final int GAME_KEY_CODE_FIRE = -90;
    static final int GAME_KEY_CODE_UP = -91;
    static final int GAME_KEY_CODE_DOWN = -92;
    static final int GAME_KEY_CODE_LEFT = -93;
    static final int GAME_KEY_CODE_RIGHT = -94;
    private static String nativeTheme;
    private static Resources nativeThemeRes;
    private static int softkeyCount = 1;
    private static boolean tablet;
    private static String DEFAULT_FONT = "Arial-plain-11";
    private static EventDispatcher formChangeListener;
    private static boolean autoAdjustFontSize = true;
    private static Object defaultInitTarget;
    private float zoomLevel = 1;
    private File storageDir;
    // skin related variables
    private boolean portrait = true;
    private BufferedImage portraitSkin;
    private BufferedImage landscapeSkin;
    private Map<java.awt.Point, Integer> portraitSkinHotspots;
    private java.awt.Rectangle portraitScreenCoordinates;
    private Map<java.awt.Point, Integer> landscapeSkinHotspots;
    private java.awt.Rectangle landscapeScreenCoordinates;
    private static Class clsInstance;

    private String platformName;
    private String[] platformOverrides = new String[0];
    
    public static void setBaseResourceDir(File f) {
        baseResourceDir = f;
    }
    
    public static void setClassLoader(Class cls) {
        clsInstance = cls;
    }

    public static Class getClassLoader() {
        return clsInstance;
    }

    public static void setDefaultInitTarget(Object o) {
        defaultInitTarget = o;
    }

    private Map<java.awt.Point, Integer> getSkinHotspots() {
        if (portrait) {
            return portraitSkinHotspots;
        }
        return landscapeSkinHotspots;
    }

    private java.awt.Rectangle getScreenCoordinates() {
        if (portrait) {
            return portraitScreenCoordinates;
        }
        return landscapeScreenCoordinates;
    }

    private BufferedImage getSkin() {
        if (portrait) {
            return portraitSkin;
        }
        return landscapeSkin;
    }

    public static void setAutoAdjustFontSize(boolean autoAdjustFontSize_) {
        autoAdjustFontSize = autoAdjustFontSize_;
    }

    public static void setFontSize(int medium, int small, int large) {
        medianFontSize = medium;
        smallFontSize = small;
        largeFontSize = large;
        DEFAULT_FONT = fontFaceSystem + "-plain-" + medium;
        autoAdjustFontSize = false;
    }

    public static void setFontFaces(String system, String proportional, String monospace) {
        fontFaceSystem = system;
        fontFaceProportional = proportional;
        fontFaceMonospace = monospace;
        DEFAULT_FONT = fontFaceSystem + "-plain-" + medianFontSize;
        autoAdjustFontSize = false;
    }

    /**
     * This is useful for debugging tools used in software automation
     */
    public static void addFormChangeListener(com.codename1.ui.events.ActionListener al) {
        if (formChangeListener == null) {
            formChangeListener = new EventDispatcher();
        }
        formChangeListener.addListener(al);
    }

    public void setCurrentForm(Form f) {
        super.setCurrentForm(f);
        if (formChangeListener != null) {
            formChangeListener.fireActionEvent(new com.codename1.ui.events.ActionEvent(f));
        }
    }

    public static void setNativeTheme(String resFile) {
        nativeTheme = resFile;
    }

    public static void setNativeTheme(Resources resFile) {
        nativeThemeRes = resFile;
    }
    
    public static Resources getNativeTheme() {
        return nativeThemeRes;
    }
    
    public boolean hasNativeTheme() {
        return nativeTheme != null || nativeThemeRes != null;
    }

    public void installNativeTheme() {
        if (nativeTheme != null) {
            try {
                Resources r = Resources.open(nativeTheme);
                UIManager.getInstance().setThemeProps(r.getTheme(r.getThemeResourceNames()[0]));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            if (nativeThemeRes != null) {
                UIManager.getInstance().setThemeProps(nativeThemeRes.getTheme(nativeThemeRes.getThemeResourceNames()[0]));
            }
        }
    }

    /**
     * @return the useNativeInput
     */
    public static boolean isUseNativeInput() {
        return useNativeInput;
    }

    /**
     * @param aUseNativeInput the useNativeInput to set
     */
    public static void setUseNativeInput(boolean aUseNativeInput) {
        useNativeInput = aUseNativeInput;
    }

    /**
     * @param aSoftkeyCount the softkeyCount to set
     */
    public static void setSoftkeyCount(int aSoftkeyCount) {
        softkeyCount = aSoftkeyCount;
    }

    private class C extends java.awt.Container implements KeyListener, MouseListener, MouseMotionListener, HierarchyBoundsListener {

        private BufferedImage buffer;
        boolean painted;
        private Graphics2D g2dInstance;
        private java.awt.Dimension forcedSize;

        C() {
            addKeyListener(this);
            addMouseListener(this);
            addMouseMotionListener(this);
            addHierarchyBoundsListener(this);
            setFocusable(true);
            requestFocus();
        }

        public void setForcedSize(Dimension d) {
            forcedSize = d;
        }

        public boolean isDoubleBuffered() {
            return true;
        }

        public boolean isOpaque() {
            return true;
        }

        public void update(java.awt.Graphics g) {
            paint(g);
        }

        public void blit() {
            if (buffer != null) {
                java.awt.Graphics g = getGraphics();
                if (g == null) {
                    return;
                }
                drawScreenBuffer(g);
                updateBufferSize();
            }
        }

        private void updateBufferSize() {
            if (getScreenCoordinates() == null) {
                java.awt.Dimension d = getSize();
                if (buffer == null || buffer.getWidth() != d.width || buffer.getHeight() != d.height) {
                    buffer = createBufferedImage();
                }
            } else {
                if (buffer == null || buffer.getWidth() != (int) (getScreenCoordinates().width * zoomLevel)
                        || buffer.getHeight() != (int) (getScreenCoordinates().height * zoomLevel)) {
                    buffer = createBufferedImage();
                }
            }
        }

        public void blit(int x, int y, int w, int h) {
            if (buffer != null) {
                java.awt.Graphics g = getGraphics();
                if (g == null) {
                    return;
                }
                drawScreenBuffer(g);
                updateBufferSize();
            }
        }

        private void drawScreenBuffer(java.awt.Graphics g) {
            if (getScreenCoordinates() != null) {
                g.setColor(Color.WHITE);
                g.fillRect((int) (getSkin().getWidth() * zoomLevel), 0, getWidth(), getHeight());
                g.fillRect(0, (int) (getSkin().getHeight() * zoomLevel), getWidth(), getHeight());
                g.drawImage(buffer, (int) (getScreenCoordinates().getX() * zoomLevel), (int) (getScreenCoordinates().getY() * zoomLevel), this);
                updateGraphicsScale(g);
                g.drawImage(getSkin(), 0, 0, this);
            } else {
                g.drawImage(buffer, 0, 0, this);
            }
        }

        public void paint(java.awt.Graphics g) {
            if (buffer != null) {
                //g = getGraphics();
                drawScreenBuffer(g);
                updateBufferSize();
                if (Display.getInstance().isInitialized()) {
                    Form f = getCurrentForm();
                    if (f != null) {
                        f.repaint();
                    }
                }
            }
        }

        private void updateGraphicsScale(java.awt.Graphics g) {
            if (zoomLevel != 1) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setTransform(AffineTransform.getScaleInstance(zoomLevel, zoomLevel));
            }
        }

        public java.awt.Dimension getPreferredSize() {
            if (forcedSize != null) {
                return forcedSize;
            }
            if (getSkin() != null) {
                return new java.awt.Dimension(getSkin().getWidth(), getSkin().getHeight());
            }
            Form f = Display.getInstance().getCurrent();
            if (f != null) {
                return new java.awt.Dimension(f.getPreferredW(), f.getPreferredH());
            }
            return new java.awt.Dimension(800, 480);
        }

        public FontRenderContext getFRC() {
            return getGraphics2D().getFontRenderContext();
        }

        public Graphics2D getGraphics2D() {
            updateBufferSize();
            if (g2dInstance == null) {
                g2dInstance = buffer.createGraphics();
                updateGraphicsScale(g2dInstance);
            }
            return g2dInstance;
        }

        private BufferedImage createBufferedImage() {
            g2dInstance = null;
            if (getScreenCoordinates() != null) {
                return new BufferedImage(Math.max(20, (int) (getScreenCoordinates().width * zoomLevel)), Math.max(20, (int) (getScreenCoordinates().height * zoomLevel)), BufferedImage.TYPE_INT_RGB);
            }
            return new BufferedImage(Math.max(20, getWidth()), Math.max(20, getHeight()), BufferedImage.TYPE_INT_RGB);
        }

        public void validate() {
            super.validate();
            java.awt.Dimension d = getPreferredSize();
            if (buffer == null || d.width != buffer.getWidth() || d.height != buffer.getHeight()) {
                buffer = createBufferedImage();
            }
            Form current = getCurrentForm();
            if (current == null) {
                return;
            }
        }

        private int getCode(java.awt.event.KeyEvent evt) {
            return getCode(evt.getKeyCode());
        }

        private int getCode(int k) {
            switch (k) {
                case KeyEvent.VK_UP:
                    return GAME_KEY_CODE_UP;
                case KeyEvent.VK_DOWN:
                    return GAME_KEY_CODE_DOWN;
                case KeyEvent.VK_LEFT:
                    return GAME_KEY_CODE_LEFT;
                case KeyEvent.VK_RIGHT:
                    return GAME_KEY_CODE_RIGHT;
                case KeyEvent.VK_SPACE:
                case KeyEvent.VK_ENTER:
                    return GAME_KEY_CODE_FIRE;
            }
            return k;
        }

        public void keyTyped(KeyEvent e) {
        }

        public void keyPressed(KeyEvent e) {
            // block key combos that might generate unreadable events
            if (e.isAltDown() || e.isControlDown() || e.isMetaDown() || e.isAltGraphDown()) {
                return;
            }
            JavaSEPort.this.keyPressed(getCode(e));
        }

        public void keyReleased(KeyEvent e) {
            // block key combos that might generate unreadable events
            if (e.isAltDown() || e.isControlDown() || e.isMetaDown() || e.isAltGraphDown()) {
                return;
            }
            JavaSEPort.this.keyReleased(getCode(e));
        }

        public void mouseClicked(MouseEvent e) {
            e.consume();
        }

        private int scaleCoordinateX(int coordinate) {
            if (getScreenCoordinates() != null) {
                return (int) (coordinate / zoomLevel - getScreenCoordinates().x);
            }
            return coordinate;
        }

        private int scaleCoordinateY(int coordinate) {
            if (getScreenCoordinates() != null) {
                return (int) (coordinate / zoomLevel - getScreenCoordinates().y);
            }
            return coordinate;
        }
        Integer triggeredKeyCode;

        public void mousePressed(MouseEvent e) {
            e.consume();
            if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0) {
                int x = scaleCoordinateX(e.getX());
                int y = scaleCoordinateY(e.getY());
                if (x >= 0 && x < getDisplayWidth() && y >= 0 && y < getDisplayHeight()) {
                    if (touchDevice) {
                        JavaSEPort.this.pointerPressed(x, y);
                    }
                } else {
                    if (getSkin() != null) {
                        java.awt.Point p = new java.awt.Point((int) (e.getX() / zoomLevel), (int) (e.getY() / zoomLevel));
                        Integer keyCode;
                        keyCode = getSkinHotspots().get(p);

                        if (keyCode != null) {
                            if (rotateTouchKeysOnLandscape && !isPortrait()) {
                                // rotate touch keys on landscape mode
                                switch (keyCode) {
                                    case KeyEvent.VK_UP:
                                        keyCode = KeyEvent.VK_LEFT;
                                        break;
                                    case KeyEvent.VK_DOWN:
                                        keyCode = KeyEvent.VK_RIGHT;
                                        break;
                                    case KeyEvent.VK_LEFT:
                                        keyCode = KeyEvent.VK_DOWN;
                                        break;
                                    case KeyEvent.VK_RIGHT:
                                        keyCode = KeyEvent.VK_UP;
                                        break;
                                }
                            }
                            triggeredKeyCode = keyCode;
                            JavaSEPort.this.keyPressed(getCode(keyCode.intValue()));
                        }
                    }
                }
                requestFocus();
            }
        }

        public void mouseReleased(MouseEvent e) {
            e.consume();
            if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0) {
                int x = scaleCoordinateX(e.getX());
                int y = scaleCoordinateY(e.getY());
                if (x >= 0 && x < getDisplayWidth() && y >= 0 && y < getDisplayHeight()) {
                    if (touchDevice) {
                        JavaSEPort.this.pointerReleased(x, y);
                    }
                }
                if (triggeredKeyCode != null) {
                    JavaSEPort.this.keyReleased(getCode(triggeredKeyCode.intValue()));
                    triggeredKeyCode = null;
                }
            }
        }

        public void mouseEntered(MouseEvent e) {
            e.consume();
        }

        public void mouseExited(MouseEvent e) {
            e.consume();
        }

        public void mouseDragged(MouseEvent e) {
            e.consume();
            if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0) {
                int x = scaleCoordinateX(e.getX());
                int y = scaleCoordinateY(e.getY());
                if (x >= 0 && x < getDisplayWidth() && y >= 0 && y < getDisplayHeight()) {
                    if (touchDevice) {
                        JavaSEPort.this.pointerDragged(x, y);
                    }
                }
            }
        }
        private Cursor handCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
        private Cursor defaultCursor = Cursor.getDefaultCursor();

        public void mouseMoved(MouseEvent e) {
            e.consume();
            if (getSkinHotspots() != null) {
                java.awt.Point p = new java.awt.Point((int) (e.getX() / zoomLevel), (int) (e.getY() / zoomLevel));
                if (getSkinHotspots().containsKey(p)) {
                    setCursor(handCursor);
                } else {
                    setCursor(defaultCursor);
                }
            }
        }

        public void ancestorMoved(HierarchyEvent e) {
        }

        public void setBounds(int x, int y, int w, int h) {
            super.setBounds(x, y, w, h);
            if (getSkin() == null) {
                JavaSEPort.this.sizeChanged(getWidth(), getHeight());
            }
        }

        public void ancestorResized(HierarchyEvent e) {
            
            if (getSkin() != null) {
                float w1 = ((float) getParent().getWidth()) / ((float) getSkin().getWidth());
                float h1 = ((float) getParent().getHeight()) / ((float) getSkin().getHeight());
                zoomLevel = Math.min(h1, w1);
                Form f = Display.getInstance().getCurrent();
                if (f != null) {
                    f.repaint();
                }
                getParent().repaint();
            } else {
                JavaSEPort.this.sizeChanged(getWidth(), getHeight());
            }
        }
    }
    private C canvas;

    protected java.awt.Container getCanvas() {
        return canvas;
    }

    public JavaSEPort() {
        canvas = new C();
    }

    public void paintDirty() {
        super.paintDirty();
    }

    /**
     * @inheritDoc
     */
    public void deinitialize() {
        if (canvas.getParent() != null) {
            canvas.getParent().remove(canvas);
        }
    }

    /**
     * Subclasses of this implementation might override this to return builtin skins for a specific implementation
     * @return true if skins are used
     */
    public boolean hasSkins() {
        return System.getProperty("skin") != null || System.getProperty("dskin") != null;
    }

    private void initializeCoordinates(BufferedImage map, Properties props, Map<Point, Integer> coordinates, java.awt.Rectangle screenPosition) {
        int[] buffer = new int[map.getWidth() * map.getHeight()];
        map.getRGB(0, 0, map.getWidth(), map.getHeight(), buffer, 0, map.getWidth());
        int screenX1 = Integer.MAX_VALUE;
        int screenY1 = Integer.MAX_VALUE;
        int screenX2 = 0;
        int screenY2 = 0;
        for (int iter = 0; iter < buffer.length; iter++) {
            int pixel = buffer[iter];
            // white pixels are blank 
            if (pixel != 0xffffffff) {
                int x = iter % map.getWidth();
                int y = iter / map.getWidth();

                // black pixels represent the screen region
                if (pixel == 0xff000000) {
                    if (x < screenX1) {
                        screenX1 = x;
                    }
                    if (y < screenY1) {
                        screenY1 = y;
                    }
                    if (x > screenX2) {
                        screenX2 = x;
                    }
                    if (y > screenY2) {
                        screenY2 = y;
                    }
                } else {
                    String prop = "c" + Integer.toHexString(0xffffff & pixel);
                    String val = props.getProperty(prop);
                    int code = 0;
                    if (val == null) {
                        val = props.getProperty("x" + Integer.toHexString(pixel));
                        if (val == null) {
                            continue;
                        }
                        code = Integer.parseInt(val, 16);
                    } else {
                        code = Integer.parseInt(val);
                    }
                    coordinates.put(new Point(x, y), code);
                }
            }
        }
        screenPosition.x = screenX1;
        screenPosition.y = screenY1;
        screenPosition.width = screenX2 - screenX1 + 1;
        screenPosition.height = screenY2 - screenY1 + 1;
    }

    private static void readFully(InputStream i, byte b[]) throws IOException {
        readFully(i, b, 0, b.length);
    }

    private static final void readFully(InputStream i, byte b[], int off, int len) throws IOException {
        if (len < 0) {
            throw new IndexOutOfBoundsException();
        }
        int n = 0;
        while (n < len) {
            int count = i.read(b, off + n, len - n);
            if (count < 0) {
                throw new EOFException();
            }
            n += count;
        }
    }

    private void loadSkinFile(InputStream skin, final Frame frm) {
        try {
            ZipInputStream z = new ZipInputStream(skin);
            ZipEntry e = z.getNextEntry();
            Properties props = new Properties();
            BufferedImage map = null;
            BufferedImage landscapeMap = null;

            // if we load the native theme imediately the multi-image's will be loaded with the size of the old skin
            byte[] nativeThemeData = null;
            nativeThemeRes = null;
            nativeTheme = null;
            while (e != null) {
                String name = e.getName();
                if (name.equals("skin.png")) {
                    portraitSkin = ImageIO.read(z);
                    e = z.getNextEntry();
                    continue;
                }
                if (name.equals("skin.properties")) {
                    props.load(z);
                    e = z.getNextEntry();
                    continue;
                }
                if (name.equals("skin_l.png")) {
                    landscapeSkin = ImageIO.read(z);
                    e = z.getNextEntry();
                    continue;
                }
                if (name.equals("skin_map.png")) {
                    map = ImageIO.read(z);
                    e = z.getNextEntry();
                    continue;
                }
                if (name.equals("skin_map_l.png")) {
                    landscapeMap = ImageIO.read(z);
                    e = z.getNextEntry();
                    continue;
                }
                if (name.endsWith(".res")) {
                    nativeThemeData = new byte[(int) e.getSize()];
                    readFully(z, nativeThemeData);
                    e = z.getNextEntry();
                    continue;
                }
                if (name.endsWith(".ttf")) {
                    try {
                        java.awt.Font result = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, z);
                        GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(result);
                    } catch (FontFormatException ex) {
                        ex.printStackTrace();
                    }

                    e = z.getNextEntry();
                    continue;
                }
                e = z.getNextEntry();
            }
            z.close();

            portraitSkinHotspots = new HashMap<Point, Integer>();
            portraitScreenCoordinates = new Rectangle();
            initializeCoordinates(map, props, portraitSkinHotspots, portraitScreenCoordinates);

            landscapeSkinHotspots = new HashMap<Point, Integer>();
            landscapeScreenCoordinates = new Rectangle();
            initializeCoordinates(landscapeMap, props, landscapeSkinHotspots, landscapeScreenCoordinates);

            platformName = props.getProperty("platformName", "se");
            platformOverrides = props.getProperty("overrideNames", "").split(",");

            setFontFaces(props.getProperty("systemFontFamily", "Arial"),
                    props.getProperty("proportionalFontFamily", "SansSerif"),
                    props.getProperty("monospaceFontFamily", "Monospaced"));
            float factor = ((float) getDisplayHeight()) / 480.0f;
            int med = (int) (15.0f * factor);
            int sm = (int) (11.0f * factor);
            int la = (int) (19.0f * factor);
            setFontSize(Integer.parseInt(props.getProperty("mediumFontSize", "" + med)),
                    Integer.parseInt(props.getProperty("smallFontSize", "" + sm)),
                    Integer.parseInt(props.getProperty("largeFontSize", "" + la)));
            tablet = props.getProperty("tablet", "false").equalsIgnoreCase("true");
            rotateTouchKeysOnLandscape = props.getProperty("rotateKeys", "false").equalsIgnoreCase("true");
            touchDevice = props.getProperty("touch", "true").equalsIgnoreCase("true");
            keyboardType = Integer.parseInt(props.getProperty("keyboardType", "0"));
            softkeyCount = Integer.parseInt(props.getProperty("softbuttonCount", "1"));
            if (softkeyCount < 2) {
                // patch the MenuBar class in case we change the softkey count in runtime we need
                // the values of the static variables to be correct!
                try {
                    Field f = com.codename1.ui.MenuBar.class.getDeclaredField("leftSK");
                    f.setAccessible(true);
                    f.setInt(null, KeyEvent.VK_F1);
                    f = com.codename1.ui.MenuBar.class.getDeclaredField("rightSK");
                    f.setAccessible(true);
                    f.setInt(null, KeyEvent.VK_F2);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }

            if (nativeThemeData != null) {
                nativeThemeRes = Resources.open(new ByteArrayInputStream(nativeThemeData));
            }

            MenuBar bar = new MenuBar();
            frm.setMenuBar(bar);
            Menu simulatorMenu = new Menu("Simulate");
            MenuItem rotate = new MenuItem("Rotate");
            simulatorMenu.add(rotate);
            Menu zoomMenu = new Menu("Zoom");
            simulatorMenu.add(zoomMenu);
            MenuItem zoom50 = new MenuItem("50%");
            zoomMenu.add(zoom50);
            MenuItem zoom100 = new MenuItem("100%");
            zoomMenu.add(zoom100);
            MenuItem zoom200 = new MenuItem("200%");
            zoomMenu.add(zoom200);

            Menu skinMenu = new Menu("Skins");
            Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
            String skinNames = pref.get("skins", DEFAULT_SKINS);
            if (skinNames != null) {
                if(skinNames.indexOf("torch.skin") < 0) {
                    skinNames += ";/torch.skin";
                }
                StringTokenizer tkn = new StringTokenizer(skinNames, ";");
                while (tkn.hasMoreTokens()) {
                    final String current = tkn.nextToken();
                    String name = current;
                    if (current.contains(":")) {
                        URL u = new URL(current);
                        File f = new File(u.getFile());
                        if (!f.exists()) {
                            continue;
                        }
                        name = f.getName();
                    }
                    MenuItem i = new MenuItem(name);
                    i.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent ae) {
                            String mainClass = System.getProperty("MainClass");
                            if (mainClass != null) {
                                Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
                                pref.put("skin", current);
                                Display.deinitialize();
                                frm.dispose();
                                try {
                                    Class sim = ClassLoader.getSystemClassLoader().loadClass("com.codename1.impl.javase.Simulator");
                                    sim.getDeclaredMethod("main", String[].class).invoke(null, new Object[]{new String[]{mainClass}});
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            } else {
                                loadSkinFile(current, frm);
                                refreshSkin(frm);
                            }
                        }
                    });
                    skinMenu.add(i);
                }
            }
            skinMenu.addSeparator();
            MenuItem addSkin = new MenuItem("Add");
            skinMenu.add(addSkin);
            addSkin.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent ae) {
                    FileDialog picker = new FileDialog(frm, "Add Skin");
                    picker.setMode(FileDialog.LOAD);
                    picker.setFilenameFilter(new FilenameFilter() {

                        public boolean accept(File file, String string) {
                            return string.endsWith(".skin");
                        }
                    });
                    picker.setModal(true);
                    picker.setVisible(true);
                    String file = picker.getFile();
                    if (file != null) {
                        String mainClass = System.getProperty("MainClass");
                        if (mainClass != null) {
                            Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
                            pref.put("skin", picker.getDirectory() + File.separator + file);
                            Display.deinitialize();
                            frm.dispose();
                            try {
                                Class sim = ClassLoader.getSystemClassLoader().loadClass("com.codename1.impl.javase.Simulator");
                                sim.getDeclaredMethod("main", String[].class).invoke(null, new Object[]{new String[]{mainClass}});
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        } else {
                            loadSkinFile(picker.getDirectory() + File.separator + file, frm);
                            refreshSkin(frm);
                        }
                    }
                }
            });

            final CheckboxMenuItem touchFlag = new CheckboxMenuItem("Touch", touchDevice);
            simulatorMenu.add(touchFlag);
            final CheckboxMenuItem nativeInputFlag = new CheckboxMenuItem("Native Input", useNativeInput);
            simulatorMenu.add(nativeInputFlag);

            simulatorMenu.addSeparator();
            MenuItem exit = new MenuItem("Exit");
            simulatorMenu.add(exit);
            bar.add(simulatorMenu);
            bar.add(skinMenu);
            rotate.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent ae) {
                    portrait = !portrait;
                    float w1 = ((float) canvas.getWidth()) / ((float) getSkin().getWidth());
                    float h1 = ((float) canvas.getHeight()) / ((float) getSkin().getHeight());
                    zoomLevel = Math.min(h1, w1);
                    canvas.setForcedSize(new java.awt.Dimension(getSkin().getWidth(), getSkin().getHeight()));
                    frm.setSize(new java.awt.Dimension(getSkin().getWidth(), getSkin().getHeight()));
                    frm.repaint();
                    
                    zoomLevel = 1;
                    JavaSEPort.this.sizeChanged(getScreenCoordinates().width, getScreenCoordinates().height);
                }
            });
            zoom100.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent ae) {
                    canvas.setForcedSize(new java.awt.Dimension(getSkin().getWidth(), getSkin().getHeight()));
                    frm.pack();
                    zoomLevel = 1;
                    Display.getInstance().getCurrent().repaint();
                    frm.repaint();
                }
            });
            zoom50.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent ae) {
                    canvas.setForcedSize(new java.awt.Dimension(getSkin().getWidth() / 2, getSkin().getHeight() / 2));
                    frm.pack();
                    zoomLevel = 0.5f;
                    Display.getInstance().getCurrent().repaint();
                    frm.repaint();
                }
            });
            zoom200.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent ae) {
                    canvas.setForcedSize(new java.awt.Dimension(getSkin().getWidth() * 2, getSkin().getHeight() * 2));
                    frm.pack();
                    zoomLevel = 2;
                    Display.getInstance().getCurrent().repaint();
                    frm.repaint();
                }
            });
            touchFlag.addItemListener(new ItemListener() {

                public void itemStateChanged(ItemEvent ie) {
                    touchDevice = !touchDevice;
                    Display.getInstance().setTouchScreenDevice(touchDevice);
                    Display.getInstance().getCurrent().repaint();
                }
            });
            nativeInputFlag.addItemListener(new ItemListener() {

                public void itemStateChanged(ItemEvent ie) {
                    useNativeInput = !useNativeInput;
                    if (useNativeInput) {
                        Display.getInstance().setDefaultVirtualKeyboard(null);
                    } else {
                        Display.getInstance().setDefaultVirtualKeyboard(new VirtualKeyboard());
                    }
                }
            });
            exit.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent ae) {
                    exitApplication();
                }
            });
        } catch (IOException err) {
            err.printStackTrace();
        }
    }

    private void addSkinName(String f) {
        Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
        String skinNames = pref.get("skins", DEFAULT_SKINS);
        if (skinNames != null) {
            if (!skinNames.contains(f)) {
                skinNames += ";" + f;
            }
        } else {
            skinNames = f;
        }
        pref.put("skins", skinNames);
    }

    private void deepRevaliate(com.codename1.ui.Container c) {
        c.setShouldCalcPreferredSize(true);
        for (int iter = 0; iter < c.getComponentCount(); iter++) {
            com.codename1.ui.Component cmp = c.getComponentAt(iter);
            cmp.setShouldCalcPreferredSize(true);
            if (cmp instanceof com.codename1.ui.Container) {
                deepRevaliate((com.codename1.ui.Container) cmp);
            }
        }
    }

    private void refreshSkin(final Frame frm) {
        Display.getInstance().callSerially(new Runnable() {

            public void run() {
                float w1 = ((float) canvas.getWidth()) / ((float) getSkin().getWidth());
                float h1 = ((float) canvas.getHeight()) / ((float) getSkin().getHeight());
                zoomLevel = Math.min(h1, w1);
                Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_DEFAULT);
                deepRevaliate(Display.getInstance().getCurrent());
                if (hasNativeTheme()) {
                    Display.getInstance().installNativeTheme();
                }
                Display.getInstance().getCurrent().refreshTheme();
                deepRevaliate(Display.getInstance().getCurrent());
                JavaSEPort.this.sizeChanged(getScreenCoordinates().width, getScreenCoordinates().height);
                Display.getInstance().getCurrent().revalidate();
                canvas.setForcedSize(new java.awt.Dimension(getSkin().getWidth(), getSkin().getHeight()));
                zoomLevel = 1;
                frm.pack();
            }
        });
    }

    private void loadSkinFile(String f, Frame frm) {
        File fsFile = new File(f);
        if (fsFile.exists()) {
            f = fsFile.toURI().toString();
        }
        if (f.contains(":")) {
            try {
                // load Via URL loading
                loadSkinFile(new URL(f).openStream(), frm);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            loadSkinFile(getResourceAsStream(getClass(), f), frm);
        }
        Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
        pref.put("skin", f);
        addSkinName(f);
    }

    /**
     * @inheritDoc
     */
    public void init(Object m) {
        if (defaultInitTarget != null && m == null) {
            m = defaultInitTarget;
        }
        if (canvas.getParent() != null) {
            canvas.getParent().remove(canvas);
        }
        if (m != null && m instanceof java.awt.Container) {
            java.awt.Container cnt = (java.awt.Container) m;
            if (cnt.getLayout() instanceof java.awt.BorderLayout) {
                cnt.add(java.awt.BorderLayout.CENTER, canvas);
            } else {
                cnt.add(canvas);
            }
        } else {
            final Frame frm = new Frame();
            frm.addWindowListener(new WindowListener() {

                public void windowOpened(WindowEvent e) {
                }

                public void windowClosing(WindowEvent e) {
                    Display.getInstance().exitApplication();
                }

                public void windowClosed(WindowEvent e) {
                }

                public void windowIconified(WindowEvent e) {
                }

                public void windowDeiconified(WindowEvent e) {
                }

                public void windowActivated(WindowEvent e) {
                }

                public void windowDeactivated(WindowEvent e) {
                }
            });
            frm.setLocationByPlatform(true);           
            frm.setLayout(new java.awt.BorderLayout());
            frm.add(java.awt.BorderLayout.CENTER, canvas);
            frm.add(canvas);
            if (hasSkins()) {
                String f = System.getProperty("skin");
                if (f != null) {
                    loadSkinFile(f, frm);
                } else {
                    Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
                    String d = System.getProperty("dskin");
                    f = pref.get("skin", d);
                    loadSkinFile(f, frm);
                }
            } else {
                Resources.setRuntimeMultiImageEnabled(true);
                frm.setUndecorated(true);
                frm.setExtendedState(Frame.MAXIMIZED_BOTH);
            }
            frm.pack();
            if (getSkin() != null) {
                float w1 = ((float) canvas.getWidth()) / ((float) getSkin().getWidth());
                float h1 = ((float) canvas.getHeight()) / ((float) getSkin().getHeight());
                zoomLevel = Math.min(h1, w1);
            }
            //frm.setSize(getSkin().getWidth(), getSkin().getHeight());
            frm.setVisible(true);
        }
        if (useNativeInput) {
            Display.getInstance().setDefaultVirtualKeyboard(null);
        }

        float factor = ((float) getDisplayHeight()) / 480.0f;
        if (factor > 0 && autoAdjustFontSize && getSkin() != null) {
            // set a reasonable default font size
            setFontSize((int) (15.0f * factor), (int) (11.0f * factor), (int) (19.0f * factor));
        }
        if (m instanceof Runnable) {
            Display.getInstance().callSerially((Runnable) m);
        }
    }

    /**
     * @inheritDoc
     */
    public void vibrate(int duration) {
    }

    /**
     * @inheritDoc
     */
    public void flashBacklight(int duration) {
    }

    /**
     * @inheritDoc
     */
    public int getDisplayWidth() {
        if (getScreenCoordinates() != null) {
            return getScreenCoordinates().width;
        }
        int w = canvas.getWidth();
        if (w < 10 && canvas.getParent() != null) {
            return canvas.getParent().getWidth();
        }
        return Math.max(w, 100);
    }

    /**
     * @inheritDoc
     */
    public int getDisplayHeight() {
        if (getScreenCoordinates() != null) {
            return getScreenCoordinates().height;
        }
        int h = canvas.getHeight();
        if (h < 10 && canvas.getParent() != null) {
            return canvas.getParent().getHeight();
        }
        return Math.max(h, 100);
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
        return new SoftReference(o);
    }

    /**
     * Extracts the hard reference from the soft/weak reference given
     *
     * @param o the reference returned by createSoftWeakRef
     * @return the original object submitted or null
     */
    public Object extractHardRef(Object o) {
        SoftReference w = (SoftReference) o;
        if (w != null) {
            return w.get();
        }
        return null;
    }

    /**
     * @inheritDoc
     */
    public boolean isNativeInputSupported() {
        return useNativeInput;
    }

    public boolean isNativeInputImmediate() {
        return useNativeInput;
    }

    /**
     * @inheritDoc
     */
    public void editString(final Component cmp, int maxSize, int constraint, String text, int keyCode) {
        java.awt.TextComponent awtTf;
        if (cmp instanceof com.codename1.ui.TextField) {
            awtTf = new java.awt.TextField();
        } else {
            awtTf = new java.awt.TextArea("", 0, 0, java.awt.TextArea.SCROLLBARS_NONE);
        }
        final java.awt.TextComponent tf = awtTf;
        if (keyCode > 0) {
            text += ((char) keyCode);
            tf.setText(text);
            tf.setCaretPosition(text.length());
        } else {
            tf.setText(text);
        }
        canvas.add(tf);
        if (getSkin() != null) {
            tf.setBounds((int) ((cmp.getAbsoluteX() + getScreenCoordinates().x) * zoomLevel),
                    (int) ((cmp.getAbsoluteY() + getScreenCoordinates().y) * zoomLevel),
                    (int) (cmp.getWidth() * zoomLevel), (int) (cmp.getHeight() * zoomLevel));
            java.awt.Font f = font(cmp.getStyle().getFont().getNativeFont());
            tf.setFont(f.deriveFont(f.getSize2D() * zoomLevel));
        } else {
            tf.setBounds(cmp.getAbsoluteX(), cmp.getAbsoluteY(), cmp.getWidth(), cmp.getHeight());
            tf.setFont(font(cmp.getStyle().getFont().getNativeFont()));
        }
        tf.requestFocus();
        class Listener implements ActionListener, FocusListener, KeyListener, Runnable {

            public synchronized void run() {
                while (tf.getParent() != null) {
                    try {
                        wait(20);
                    } catch (InterruptedException ex) {
                    }
                }
            }

            public void actionPerformed(ActionEvent e) {
                Display.getInstance().onEditingComplete(cmp, tf.getText());
                if (tf instanceof java.awt.TextField) {
                    ((java.awt.TextField) tf).removeActionListener(this);
                }
                tf.removeFocusListener(this);
                canvas.remove(tf);
                synchronized (this) {
                    notify();
                }
                canvas.repaint();
            }

            public void focusGained(FocusEvent e) {
            }

            public void focusLost(FocusEvent e) {
                actionPerformed(null);
            }

            public void keyTyped(KeyEvent e) {
            }

            public void keyPressed(KeyEvent e) {
            }

            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    if (tf instanceof java.awt.TextField) {
                        actionPerformed(null);
                    } else {
                        if (tf.getCaretPosition() >= tf.getText().length() - 1) {
                            actionPerformed(null);
                        }
                    }
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    if (tf instanceof java.awt.TextField) {
                        actionPerformed(null);
                    } else {
                        if (tf.getCaretPosition() <= 2) {
                            actionPerformed(null);
                        }
                    }
                    return;
                }
            }
        };
        final Listener l = new Listener();
        if (tf instanceof java.awt.TextField) {
            ((java.awt.TextField) tf).addActionListener(l);
        }
        tf.addKeyListener(l);
        tf.addFocusListener(l);
        tf.setSelectionEnd(0);
        tf.setSelectionStart(0);
        Display.getInstance().invokeAndBlock(l);
    }

    /**
     * @inheritDoc
     */
    public void saveTextEditingState() {
    }

    /**
     * @inheritDoc
     */
    public void flushGraphics(int x, int y, int width, int height) {
        canvas.blit(x, y, width, height);
    }

    /**
     * @inheritDoc
     */
    public void flushGraphics() {
        canvas.blit();
    }

    /**
     * @inheritDoc
     */
    public void getRGB(Object nativeImage, int[] arr, int offset, int x, int y, int width, int height) {
        ((BufferedImage) nativeImage).getRGB(x, y, width, height, arr, offset, width);
    }

    /**
     * @inheritDoc
     */
    public Object createImage(int[] rgb, int width, int height) {
        BufferedImage i = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        i.setRGB(0, 0, width, height, rgb, 0, width);
        return i;
    }

    /**
     * @inheritDoc
     */
    public Object createImage(String path) throws IOException {
        try {
            InputStream i = getResourceAsStream(clsInstance, path);

            // prevents a security exception due to a JDK bug which for some stupid reason chooses
            // to create a temporary file in the spi of Image IO
            return ImageIO.read(new MemoryCacheImageInputStream(i));
        } catch (Throwable t) {
            t.printStackTrace();
            throw new IOException(t.toString());
        }
    }

    /**
     * @inheritDoc
     */
    public Object createImage(InputStream i) throws IOException {
        try {
            return ImageIO.read(i);
        } catch (Throwable t) {
            t.printStackTrace();
            throw new IOException(t.toString());
        }
    }

    /**
     * @inheritDoc
     */
    public Object createMutableImage(int width, int height, int fillColor) {
        int a = (fillColor >> 24) & 0xff;
        if (a == 0xff) {
            BufferedImage b = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = b.createGraphics();
            g.setColor(new Color(fillColor));
            g.fillRect(0, 0, width, height);
            g.dispose();
            return b;
        }
        BufferedImage b = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        if (a != 0) {
            Graphics2D g = b.createGraphics();
            g.setColor(new Color(fillColor));
            g.fillRect(0, 0, width, height);
            g.dispose();
        }
        return b;
    }

    /**
     * @inheritDoc
     */
    public boolean isAlphaMutableImageSupported() {
        return true;
    }

    /**
     * @inheritDoc
     */
    public Object createImage(byte[] bytes, int offset, int len) {
        try {
            return ImageIO.read(new ByteArrayInputStream(bytes, offset, len));
        } catch (IOException ex) {
            // never happens
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * @inheritDoc
     */
    public int getImageWidth(Object i) {
        return ((BufferedImage) i).getWidth();
    }

    /**
     * @inheritDoc
     */
    public int getImageHeight(Object i) {
        return ((BufferedImage) i).getHeight();
    }

    /**
     * @inheritDoc
     */
    public Object scale(Object nativeImage, int width, int height) {
        BufferedImage image = (BufferedImage) nativeImage;
        int srcWidth = image.getWidth();
        int srcHeight = image.getHeight();

        // no need to scale
        if (srcWidth == width && srcHeight == height) {
            return image;
        }

        int[] currentArray = new int[srcWidth];
        int[] destinationArray = new int[width * height];
        scaleArray(image, srcWidth, srcHeight, height, width, currentArray, destinationArray);

        return createImage(destinationArray, width, height);
    }

    private void scaleArray(BufferedImage currentImage, int srcWidth, int srcHeight, int height, int width, int[] currentArray, int[] destinationArray) {
        // Horizontal Resize
        int yRatio = (srcHeight << 16) / height;
        int xRatio = (srcWidth << 16) / width;
        int xPos = xRatio / 2;
        int yPos = yRatio / 2;

        // if there is more than 16bit color there is no point in using mutable
        // images since they won't save any memory
        for (int y = 0; y < height; y++) {
            int srcY = yPos >> 16;
            getRGB(currentImage, currentArray, 0, 0, srcY, srcWidth, 1);
            for (int x = 0; x < width; x++) {
                int srcX = xPos >> 16;
                int destPixel = x + y * width;
                if ((destPixel >= 0 && destPixel < destinationArray.length) && (srcX < currentArray.length)) {
                    destinationArray[destPixel] = currentArray[srcX];
                }
                xPos += xRatio;
            }
            yPos += yRatio;
            xPos = xRatio / 2;
        }
    }

    private static int round(double d) {
        double f = Math.floor(d);
        double c = Math.ceil(d);
        if (c - d < d - f) {
            return (int) c;
        }
        return (int) f;
    }

    /**
     * @inheritDoc
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
     * @inheritDoc
     */
    public int getSoftkeyCount() {
        return softkeyCount;
    }

    /**
     * @inheritDoc
     */
    public int[] getSoftkeyCode(int index) {
        switch (softkeyCount) {
            case 0:
                return null;
            case 2:
                if (index == 0) {
                    return new int[]{KeyEvent.VK_F1};
                } else {
                    return new int[]{KeyEvent.VK_F2};
                }
            default:
                return new int[]{KeyEvent.VK_F1};
        }
    }

    /**
     * @inheritDoc
     */
    public int getClearKeyCode() {
        return KeyEvent.VK_DELETE;
    }

    /**
     * @inheritDoc
     */
    public int getBackspaceKeyCode() {
        return KeyEvent.VK_BACK_SPACE;
    }

    /**
     * @inheritDoc
     */
    public int getBackKeyCode() {
        return KeyEvent.VK_ESCAPE;
    }

    /**
     * @inheritDoc
     */
    public int getGameAction(int keyCode) {
        switch (keyCode) {
            case GAME_KEY_CODE_UP:
                return Display.GAME_UP;
            case GAME_KEY_CODE_DOWN:
                return Display.GAME_DOWN;
            case GAME_KEY_CODE_RIGHT:
                return Display.GAME_RIGHT;
            case GAME_KEY_CODE_LEFT:
                return Display.GAME_LEFT;
            case GAME_KEY_CODE_FIRE:
                return Display.GAME_FIRE;
        }
        return 0;
    }

    /**
     * @inheritDoc
     */
    public int getKeyCode(int gameAction) {
        switch (gameAction) {
            case Display.GAME_UP:
                return GAME_KEY_CODE_UP;
            case Display.GAME_DOWN:
                return GAME_KEY_CODE_DOWN;
            case Display.GAME_RIGHT:
                return GAME_KEY_CODE_RIGHT;
            case Display.GAME_LEFT:
                return GAME_KEY_CODE_LEFT;
            case Display.GAME_FIRE:
                return GAME_KEY_CODE_FIRE;
        }
        return 0;
    }

    /**
     * @inheritDoc
     */
    public boolean isTouchDevice() {
        return touchDevice;
    }

    /**
     * @inheritDoc
     */
    public void setNativeFont(Object graphics, Object font) {
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.setFont(font(font));
    }

    /**
     * @inheritDoc
     */
    public int getClipX(Object graphics) {
        Graphics2D nativeGraphics = getGraphics(graphics);
        java.awt.Rectangle r = nativeGraphics.getClipBounds();
        if (r == null) {
            return 0;
        }
        return r.x;
    }

    /**
     * @inheritDoc
     */
    public int getClipY(Object graphics) {
        Graphics2D nativeGraphics = getGraphics(graphics);
        java.awt.Rectangle r = nativeGraphics.getClipBounds();
        if (r == null) {
            return 0;
        }
        return r.y;
    }

    /**
     * @inheritDoc
     */
    public int getClipWidth(Object graphics) {
        Graphics2D nativeGraphics = getGraphics(graphics);
        java.awt.Rectangle r = nativeGraphics.getClipBounds();
        if (r == null) {
            if (graphics instanceof NativeScreenGraphics) {
                NativeScreenGraphics ng = (NativeScreenGraphics) graphics;
                if (ng.sourceImage != null) {
                    return ng.sourceImage.getWidth();
                }
            }
            return getDisplayWidth();
        }
        return r.width;
    }

    /**
     * @inheritDoc
     */
    public int getClipHeight(Object graphics) {
        Graphics2D nativeGraphics = getGraphics(graphics);
        java.awt.Rectangle r = nativeGraphics.getClipBounds();
        if (r == null) {
            if (graphics instanceof NativeScreenGraphics) {
                NativeScreenGraphics ng = (NativeScreenGraphics) graphics;
                if (ng.sourceImage != null) {
                    return ng.sourceImage.getHeight();
                }
            }
            return getDisplayHeight();
        }
        return r.height;
    }

    /**
     * @inheritDoc
     */
    public void setClip(Object graphics, int x, int y, int width, int height) {
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.setClip(x, y, width, height);
    }

    /**
     * @inheritDoc
     */
    public void clipRect(Object graphics, int x, int y, int width, int height) {
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.clipRect(x, y, width, height);
    }

    /**
     * @inheritDoc
     */
    public void drawLine(Object graphics, int x1, int y1, int x2, int y2) {
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.drawLine(x1, y1, x2, y2);
    }

    /**
     * @inheritDoc
     */
    public void fillRect(Object graphics, int x, int y, int w, int h) {
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.fillRect(x, y, w, h);
    }

    /**
     * @inheritDoc
     */
    public boolean isAlphaGlobal() {
        return true;
    }

    /**
     * @inheritDoc
     */
    public void drawRect(Object graphics, int x, int y, int width, int height) {
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.drawRect(x, y, width, height);
    }

    /**
     * @inheritDoc
     */
    public void drawRoundRect(Object graphics, int x, int y, int width, int height, int arcWidth, int arcHeight) {
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    /**
     * @inheritDoc
     */
    public void fillRoundRect(Object graphics, int x, int y, int width, int height, int arcWidth, int arcHeight) {
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    /**
     * @inheritDoc
     */
    public void fillArc(Object graphics, int x, int y, int width, int height, int startAngle, int arcAngle) {
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.fillArc(x, y, width, height, startAngle, arcAngle);
    }

    /**
     * @inheritDoc
     */
    public void drawArc(Object graphics, int x, int y, int width, int height, int startAngle, int arcAngle) {
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.drawArc(x, y, width, height, startAngle, arcAngle);
    }

    /**
     * @inheritDoc
     */
    public void setColor(Object graphics, int RGB) {
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.setColor(new Color(RGB));
    }

    /**
     * @inheritDoc
     */
    public int getColor(Object graphics) {
        Graphics2D nativeGraphics = getGraphics(graphics);
        return nativeGraphics.getColor().getRGB();
    }

    /**
     * @inheritDoc
     */
    public void setAlpha(Object graphics, int alpha) {
        Graphics2D nativeGraphics = getGraphics(graphics);
        float a = ((float) alpha) / 255.0f;
        nativeGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
    }

    /**
     * @inheritDoc
     */
    public int getAlpha(Object graphics) {
        Graphics2D nativeGraphics = getGraphics(graphics);
        Object c = nativeGraphics.getComposite();
        if (c != null && c instanceof AlphaComposite) {
            return (int) (((AlphaComposite) c).getAlpha() * 255);
        }
        return 255;
    }

    /**
     * @inheritDoc
     */
    public void drawString(Object graphics, String str, int x, int y) {
        Graphics2D nativeGraphics = getGraphics(graphics);
        if (zoomLevel != 1) {
            nativeGraphics = (Graphics2D) nativeGraphics.create();
            nativeGraphics.setTransform(AffineTransform.getTranslateInstance(0, 0));
            java.awt.Font currentFont = nativeGraphics.getFont();
            float fontSize = currentFont.getSize2D();
            fontSize *= zoomLevel;
            int ascent = nativeGraphics.getFontMetrics().getAscent();
            nativeGraphics.setFont(currentFont.deriveFont(fontSize));
            nativeGraphics.drawString(str, x * zoomLevel, (y + ascent) * zoomLevel);
        } else {
            nativeGraphics.drawString(str, x, y + nativeGraphics.getFontMetrics().getAscent());
        }
    }

    /**
     * @inheritDoc
     */
    public void drawImage(Object graphics, Object img, int x, int y) {
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.drawImage((BufferedImage) img, x, y, null);
    }

    /**
     * @inheritDoc
     */
    public void fillTriangle(Object graphics, int x1, int y1, int x2, int y2, int x3, int y3) {
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.fillPolygon(new int[]{x1, x2, x3}, new int[]{y1, y2, y3}, 3);
    }
    private BufferedImage cache;

    /**
     * @inheritDoc
     */
    public void drawRGB(Object graphics, int[] rgbData, int offset, int x, int y, int w, int h, boolean processAlpha) {
        Graphics2D nativeGraphics = getGraphics(graphics);
        if (cache == null || cache.getWidth() != w || cache.getHeight() != h) {
            cache = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        }
        cache.setRGB(0, 0, w, h, rgbData, offset, w);
        nativeGraphics.drawImage(cache, x, y, null);
    }

    /**
     * @inheritDoc
     */
    public Object getNativeGraphics() {
        return new NativeScreenGraphics();
    }

    /**
     * @inheritDoc
     */
    public Object getNativeGraphics(Object image) {
        /*NativeScreenGraphics n = new NativeScreenGraphics();
        n.sourceImage = (BufferedImage)image;
        return n;*/
        return ((BufferedImage) image).getGraphics();
    }

    /**
     * @inheritDoc
     */
    public void translate(Object graphics, int x, int y) {
        // does nothing, we expect translate to occur in the graphics for
        // better device portability
    }

    /**
     * @inheritDoc
     */
    public int getTranslateX(Object graphics) {
        return 0;
    }

    /**
     * @inheritDoc
     */
    public int getTranslateY(Object graphics) {
        return 0;
    }

    /**
     * @inheritDoc
     */
    public int charsWidth(Object nativeFont, char[] ch, int offset, int length) {
        return stringWidth(nativeFont, new String(ch, offset, length));
    }

    /**
     * @inheritDoc
     */
    public int stringWidth(Object nativeFont, String str) {
        return (int) Math.ceil(font(nativeFont).getStringBounds(str, canvas.getFRC()).getWidth());
    }

    /**
     * @inheritDoc
     */
    public int charWidth(Object nativeFont, char ch) {
        return (int) Math.ceil(font(nativeFont).getStringBounds("" + ch, canvas.getFRC()).getWidth());
    }

    /**
     * @inheritDoc
     */
    public int getHeight(Object nativeFont) {
        return font(nativeFont).getSize() + 1;
    }

    /**
     * @inheritDoc
     */
    public Object createFont(int face, int style, int size) {
        return new int[]{face, style, size};
    }

    private java.awt.Font createAWTFont(int[] i) {
        int face = i[0];
        int style = i[1];
        int size = i[2];
        String fontName;
        switch (face) {
            case Font.FACE_MONOSPACE:
                fontName = fontFaceMonospace + "-";
                break;
            case Font.FACE_PROPORTIONAL:
                fontName = fontFaceProportional + "-";
                break;
            default: //Font.FACE_SYSTEM:
                fontName = fontFaceSystem + "-";
                break;
        }
        switch (style) {
            case Font.STYLE_BOLD:
                fontName += "bold-";
                break;
            case Font.STYLE_ITALIC:
                fontName += "italic-";
                break;
            case Font.STYLE_PLAIN:
                fontName += "plain-";
                break;
            case Font.STYLE_UNDERLINED:
                // unsupported...
                fontName += "plain-";
                break;
            default:
                // probably bold/italic
                fontName += "bold-";
                break;
        }
        switch (size) {
            case Font.SIZE_LARGE:
                fontName += largeFontSize;
                break;
            case Font.SIZE_SMALL:
                fontName += smallFontSize;
                break;
            default:
                fontName += medianFontSize;
                break;
        }
        return java.awt.Font.decode(fontName);
    }

    /**
     * @inheritDoc
     */
    public Object getDefaultFont() {
        return DEFAULT_FONT;
    }

    /**
     * @inheritDoc
     */
    public int getFace(Object nativeFont) {
        if (font(nativeFont).getFamily().equals(fontFaceMonospace)) {
            return Font.FACE_MONOSPACE;
        }
        if (font(nativeFont).getFamily().equals(fontFaceProportional)) {
            return Font.FACE_PROPORTIONAL;
        }
        if (font(nativeFont).getFamily().equals(fontFaceSystem)) {
            return Font.FACE_SYSTEM;
        }
        return Font.FACE_SYSTEM;
    }

    /**
     * @inheritDoc
     */
    public int getSize(Object nativeFont) {
        if (nativeFont == null) {
            return Font.SIZE_MEDIUM;
        }
        if (nativeFont instanceof int[]) {
            return ((int[]) nativeFont)[2];
        }
        int size = font(nativeFont).getSize();
        if (size == largeFontSize) {
            return Font.SIZE_LARGE;
        }
        if (size == smallFontSize) {
            return Font.SIZE_SMALL;
        }
        return Font.SIZE_MEDIUM;
    }

    /**
     * @inheritDoc
     */
    public int getStyle(Object nativeFont) {
        if (font(nativeFont).isBold()) {
            if (font(nativeFont).isItalic()) {
                return Font.STYLE_BOLD | Font.STYLE_ITALIC;
            } else {
                return Font.STYLE_BOLD;
            }
        }
        if (font(nativeFont).isItalic()) {
            return Font.STYLE_ITALIC;
        }
        return Font.STYLE_PLAIN;
    }

    private java.awt.Font font(Object f) {
        if (f == null) {
            return java.awt.Font.decode(DEFAULT_FONT);
        }
        // for bitmap fonts
        if (f instanceof java.awt.Font) {
            return (java.awt.Font) f;
        }
        return createAWTFont((int[]) f);
    }

    /**
     * @inheritDoc
     */
    public Object loadNativeFont(String lookup) {
        return java.awt.Font.decode(lookup.split(";")[0]);
    }

    /**
     * @inheritDoc
     */
    public void fillPolygon(Object graphics, int[] xPoints, int[] yPoints, int nPoints) {
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.fillPolygon(xPoints, yPoints, nPoints);
    }

    /**
     * @inheritDoc
     */
    public void drawPolygon(Object graphics, int[] xPoints, int[] yPoints, int nPoints) {
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.drawPolygon(xPoints, yPoints, nPoints);
    }

    @Override
    public boolean animateImage(Object nativeImage, long lastFrame) {
        return false;
    }

    @Override
    public Object createSVGImage(String baseURL, byte[] data) throws IOException {
        return null;
    }

    @Override
    public boolean isSVGSupported() {
        return false;
    }

    /**
     * @inheritDoc
     */
    public Object getSVGDocument(Object svgImage) {
        return svgImage;
    }

    /**
     * @inheritDoc
     */
    public void exitApplication() {
        try {
            System.exit(0);
        } catch (Throwable t) {
            System.out.println("Can't exit from applet");
        }
    }

    /**
     * @inheritDoc
     */
    public String getProperty(String key, String defaultValue) {
        if ("OS".equals(key)) {
            return "SE";
        }
        String s = System.getProperty(key);
        if (s == null) {
            return defaultValue;
        }
        return s;
    }

    /**
     * @inheritDoc
     */
    public void execute(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private Graphics2D getGraphics(Object nativeG) {
        if (nativeG instanceof Graphics2D) {
            Graphics2D g2d = (Graphics2D) nativeG;
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            return (Graphics2D) nativeG;
        }
        NativeScreenGraphics ng = (NativeScreenGraphics) nativeG;
        if (ng.sourceImage != null) {
            return ng.sourceImage.createGraphics();
        }
        Graphics2D g2d = canvas.getGraphics2D();
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g2d;
    }

    /**
     * @inheritDoc
     */
    protected void playNativeBuiltinSound(Object data) {
        Toolkit.getDefaultToolkit().beep();
    }

    /**
     * @inheritDoc
     */
    public boolean isBuiltinSoundAvailable(String soundIdentifier) {
        if (soundIdentifier.equals(Display.SOUND_TYPE_ALARM)) {
            return true;
        }
        if (soundIdentifier.equals(Display.SOUND_TYPE_CONFIRMATION)) {
            return true;
        }
        if (soundIdentifier.equals(Display.SOUND_TYPE_ERROR)) {
            return true;
        }
        if (soundIdentifier.equals(Display.SOUND_TYPE_INFO)) {
            return true;
        }
        if (soundIdentifier.equals(Display.SOUND_TYPE_WARNING)) {
            return true;
        }
        return super.isBuiltinSoundAvailable(soundIdentifier);
    }

//    /**
//     * @inheritDoc
//     */
//    public Object createAudio(String uri, Runnable onCompletion) throws IOException {
//        return new CodenameOneMediaPlayer(uri, frm, onCompletion);
//    }
    /**
     * Plays the sound in the given URI which is partially platform specific.
     *
     * @param uri the platform specific location for the sound
     * @param onCompletion invoked when the audio file finishes playing, may be null
     * @return a handle that can be used to control the playback of the audio
     * @throws java.io.IOException if the URI access fails
     */
    public Media createMedia(String uri, boolean isVideo, Runnable onCompletion) throws IOException {
        java.awt.Container cnt = canvas.getParent();
        while (!(cnt instanceof Frame)) {
            cnt = cnt.getParent();
            if(cnt == null) {
                return null;
            }
        }
        
        if(uri.indexOf(':') < 0) {
            String mimeType = "video/mp4";
            return new CodenameOneMediaPlayer(getResourceAsStream(getClass(), uri), mimeType, (Frame) cnt, onCompletion);
        }
        
        return new CodenameOneMediaPlayer(uri, isVideo, (Frame) cnt, onCompletion);
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
        java.awt.Container cnt = canvas.getParent();
        while (!(cnt instanceof Frame)) {
            cnt = cnt.getParent();
            if(cnt == null) {
                return null;
            }
        }
        return new CodenameOneMediaPlayer(stream, mimeType, (Frame) cnt, onCompletion);
    }

    private class NativeScreenGraphics {

        BufferedImage sourceImage;
        Graphics2D cachedGraphics;
    }

    public boolean isAffineSupported() {
        return true;
    }

    public void resetAffine(Object nativeGraphics) {
        Graphics2D g = getGraphics(nativeGraphics);
        g.setTransform(new AffineTransform());
        if (zoomLevel != 1) {
            g.setTransform(AffineTransform.getScaleInstance(zoomLevel, zoomLevel));
        }
    }

    public void scale(Object nativeGraphics, float x, float y) {
        Graphics2D g = getGraphics(nativeGraphics);
        g.scale(x, y);
    }

    public void rotate(Object nativeGraphics, float angle) {
        Graphics2D g = getGraphics(nativeGraphics);
        g.rotate(angle);
    }

    public void shear(Object nativeGraphics, float x, float y) {
        Graphics2D g = getGraphics(nativeGraphics);
        g.shear(x, y);
    }

    public boolean isTablet() {
        return tablet;
    }

    public static void setTablet(boolean b) {
        tablet = b;
    }

    public boolean isAntiAliasingSupported() {
        return true;
    }

    public boolean isAntiAliasedTextSupported() {
        return true;
    }

    public void setAntiAliased(Object graphics, boolean a) {
        Graphics2D g2d = getGraphics(graphics);
        if (a) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        } else {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        }
    }

    public boolean isAntiAliased(Object graphics) {
        Graphics2D g2d = getGraphics(graphics);
        return g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING) == RenderingHints.VALUE_ANTIALIAS_ON;
    }

    public void setAntiAliasedText(Object graphics, boolean a) {
        Graphics2D g2d = getGraphics(graphics);
        if (a) {
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        } else {
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        }
    }

    public boolean isAntiAliasedText(Object graphics) {
        return true;
    }

    public int getKeyboardType() {
        return keyboardType;
    }

    private File getStorageDir() {
        if (storageDir == null) {
            storageDir = new File(System.getProperty("user.home"), "." + ((String) getStorageData()));
            storageDir.mkdirs();
        }
        return storageDir;
    }

    /**
     * @inheritDoc
     */
    public Object connect(String url, boolean read, boolean write) throws IOException {
        URL u = new URL(url);
        URLConnection con = u.openConnection();
        con.setDoInput(read);
        con.setDoOutput(write);
        return con;
    }

    /**
     * @inheritDoc
     */
    public void setHeader(Object connection, String key, String val) {
        ((URLConnection) connection).setRequestProperty(key, val);
    }

    /**
     * @inheritDoc
     */
    public OutputStream openOutputStream(Object connection) throws IOException {
        if (connection instanceof String) {
            FileOutputStream fc = new FileOutputStream((String) connection);
            BufferedOutputStream o = new BufferedOutputStream(fc, (String) connection);
            return o;
        }
        return new BufferedOutputStream(((URLConnection) connection).getOutputStream());
    }

    /**
     * @inheritDoc
     */
    public OutputStream openOutputStream(Object connection, int offset) throws IOException {
        RandomAccessFile rf = new RandomAccessFile((String) connection, "rw");
        rf.seek(offset);
        FileOutputStream fc = new FileOutputStream(rf.getFD());
        BufferedOutputStream o = new BufferedOutputStream(fc, (String) connection);
        o.setConnection(rf);
        return o;
    }

    /**
     * @inheritDoc
     */
    public InputStream openInputStream(Object connection) throws IOException {
        if (connection instanceof String) {
            FileInputStream fc = new FileInputStream((String) connection);
            BufferedInputStream o = new BufferedInputStream(fc, (String) connection);
            return o;
        }
        return new BufferedInputStream(((URLConnection) connection).getInputStream());
    }

    /**
     * @inheritDoc
     */
    public void setPostRequest(Object connection, boolean p) {
        try {
            if (p) {
                ((HttpURLConnection) connection).setRequestMethod("POST");
            } else {
                ((HttpURLConnection) connection).setRequestMethod("GET");
            }
        } catch (IOException err) {
            // an exception here doesn't make sense
            err.printStackTrace();
        }
    }

    /**
     * @inheritDoc
     */
    public int getResponseCode(Object connection) throws IOException {
        return ((HttpURLConnection) connection).getResponseCode();
    }

    /**
     * @inheritDoc
     */
    public String getResponseMessage(Object connection) throws IOException {
        return ((HttpURLConnection) connection).getResponseMessage();
    }

    /**
     * @inheritDoc
     */
    public int getContentLength(Object connection) {
        return ((HttpURLConnection) connection).getContentLength();
    }

    /**
     * @inheritDoc
     */
    public String getHeaderField(String name, Object connection) throws IOException {
        return ((HttpURLConnection) connection).getHeaderField(name);
    }

    /**
     * @inheritDoc
     */
    public String[] getHeaderFields(String name, Object connection) throws IOException {
        HttpURLConnection c = (HttpURLConnection) connection;
        List r = new ArrayList();
        List<String> headers = c.getHeaderFields().get(name);
        if(headers != null && headers.size() > 0) {
            String[] s = new String[headers.size()];
            headers.toArray(s);
            return s;
        }
        return null;
    }

    /**
     * @inheritDoc
     */
    public void deleteStorageFile(String name) {
        new File(getStorageDir(), name).delete();
    }

    /**
     * @inheritDoc
     */
    public OutputStream createStorageOutputStream(String name) throws IOException {
        return new FileOutputStream(new File(getStorageDir(), name));
    }

    /**
     * @inheritDoc
     */
    public InputStream createStorageInputStream(String name) throws IOException {
        return new FileInputStream(new File(getStorageDir(), name));
    }

    /**
     * @inheritDoc
     */
    public boolean storageFileExists(String name) {
        return new File(getStorageDir(), name).exists();
    }

    /**
     * @inheritDoc
     */
    public String[] listStorageEntries() {
        return getStorageDir().list();
    }

    /**
     * @inheritDoc
     */
    public String[] listFilesystemRoots() {
        File[] f = File.listRoots();
        String[] roots = new String[f.length];
        for (int iter = 0; iter < f.length; iter++) {
            roots[iter] = f[iter].getAbsolutePath();
        }
        return roots;
    }

    /**
     * @inheritDoc
     */
    public String[] listFiles(String directory) throws IOException {
        return new File(directory).list();
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
        new File(directory).mkdirs();
    }

    /**
     * @inheritDoc
     */
    public void deleteFile(String file) {
        new File(file).delete();
    }

    /**
     * @inheritDoc
     */
    public boolean isHidden(String file) {
        return new File(file).isHidden();
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
        return new File(file).length();
    }

    /**
     * @inheritDoc
     */
    public boolean isDirectory(String file) {
        return new File(file).isDirectory();
    }

    /**
     * @inheritDoc
     */
    public char getFileSystemSeparator() {
        return File.separatorChar;
    }

    /**
     * @inheritDoc
     */
    public OutputStream openFileOutputStream(String file) throws IOException {
        return new FileOutputStream(file);
    }

    /**
     * @inheritDoc
     */
    public InputStream openFileInputStream(String file) throws IOException {
        return new FileInputStream(file);
    }

    /**
     * @inheritDoc
     */
    public boolean exists(String file) {
        return new File(file).exists();
    }

    /**
     * @inheritDoc
     */
    public void rename(String file, String newName) {
        new File(file).renameTo(new File(new File(file).getParentFile(), newName));
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
        return platformName;
    }
    
    /**
     * @inheritDoc
     */
    public String[] getPlatformOverrides() {
        return platformOverrides;
    }
    
    public LocationManager getLocationManager() {
        return StubLocationManager.getLocationManager();
    }

    @Override
    public void sendMessage(String[] recieptents, String subject, Message msg) {
        System.out.println("sending message to " + recieptents[0]);
    }

    

    class CodenameOneMediaPlayer implements Media, ControllerListener {

        private Runnable onCompletion;
        private MediaPlayer player;
        private boolean realized = false;
        private boolean isVideo;
        private VideoPanel video;
        private Frame frm;
        private boolean playing = false;

        public CodenameOneMediaPlayer(String uri, boolean isVideo, Frame f, Runnable onCompletion) throws IOException {
            this.onCompletion = onCompletion;
            this.isVideo = isVideo;
            this.frm = f;
            try {
                player = jmapps.util.JMFUtils.createMediaPlayer(uri, f, "", "");
                player.setPlaybackLoop(false);
                player.setPopupActive(false);
                player.addControllerListener(this);
                player.realize();
                Display.getInstance().invokeAndBlock(new Runnable() {

                    @Override
                    public void run() {
                        while (!realized) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(CodenameOneMediaPlayer.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                });
                if (isVideo) {
                    video = new VideoPanel(player);
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                throw new RuntimeException(ex);
            }
        }

        public CodenameOneMediaPlayer(InputStream stream, String mimeType, Frame f, Runnable onCompletion) throws IOException {
            String suffix = "";
            if (mimeType.contains("mp3") || mimeType.contains("audio/mpeg")) {
                suffix = ".mp3";
            } else if (mimeType.contains("wav")) {
                suffix = ".wav";
            }
            if (mimeType.contains("amr")) {
                suffix = ".amr";
            }
            if (mimeType.contains("3gpp")) {
                suffix = ".3gp";
            }
            if (mimeType.contains("mp4") || mimeType.contains("mpeg4")) {
                suffix = ".mp4";
            }
            if (mimeType.contains("h264")) {
                suffix = ".h264";
            }
            if (mimeType.equals("video/mpeg")) {
                suffix = ".mpeg";
            }

            File temp = File.createTempFile("mtmp", suffix);
            temp.deleteOnExit();
            FileOutputStream out = new FileOutputStream(temp);
            byte buf[] = new byte[256];
            int len = 0;
            while ((len = stream.read(buf, 0, buf.length)) > -1) {
                out.write(buf, 0, len);
            }
            stream.close();

            this.onCompletion = onCompletion;
            this.isVideo = mimeType.contains("video");
            this.frm = f;
            try {
                player = jmapps.util.JMFUtils.createMediaPlayer(temp.toURI().toURL().toString(), f, null, null);
                player.setPlaybackLoop(false);
                player.setPopupActive(false);
                player.addControllerListener(this);
                player.realize();
                Display.getInstance().invokeAndBlock(new Runnable() {

                    @Override
                    public void run() {
                        while (!realized) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(CodenameOneMediaPlayer.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                });
                if (isVideo) {
                    video = new VideoPanel(player);
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                throw new RuntimeException(ex);
            }
        }

        public void cleanup() {
            player.close();
            playing = false;            
        }

        public void play() {
            player.start();
            playing = true;
        }

        public void pause() {
            player.stop();
            playing = false;            
        }

        public int getTime() {
            return (int) player.getMediaTime().getSeconds();
        }

        public void setTime(int time) {
            player.setMediaTime(new Time(time));
        }

        public int getDuration() {
            return (int) player.getDuration().getSeconds();
        }

        public void setVolume(int vol) {
            int level = vol / 20;
            player.setVolumeLevel(level + "");
        }

        public int getVolume() {
            int level = Integer.parseInt(player.getVolumeLevel());
            return level * 20;
        }

        @Override
        public Component getVideoComponent() {
            return new VideoComponent(frm, video);
        }

        public boolean isVideo() {
            return isVideo;
        }

        public boolean isFullScreen() {
            return false;
        }

        public void setFullScreen(boolean fullScreen) {
        }

        @Override
        public boolean isPlaying() {
            return playing;
        }

        @Override
        public void setNativePlayerMode(boolean nativePlayer) {
        }

        @Override
        public boolean isNativePlayerMode() {
            return false;
        }

        @Override
        public void controllerUpdate(ControllerEvent ce) {
            if (ce instanceof RealizeCompleteEvent) {
                realized = true;
            }
        }
    }

    class VideoComponent extends PeerComponent {

        private VideoPanel vid;
        private Frame frm;
        private Container cnt = new Container();
        
        public VideoComponent(Frame frm, VideoPanel vid) {
            super(vid);
            this.vid = vid;
            this.frm = frm;
            cnt.setLayout(null);
            cnt.add(vid);
           
        }

        @Override
        protected void initComponent() {
            super.initComponent();
            frm.add(cnt, 0);
            frm.validate();
        }

        @Override
        protected void deinitialize() {
            super.deinitialize();
            frm.remove(cnt);
            frm.validate();
        }

       
        @Override
        protected com.codename1.ui.geom.Dimension calcPreferredSize() {
            return new com.codename1.ui.geom.Dimension(vid.getWidth(), vid.getHeight());
        }

        @Override
        public void paint(Graphics g) {
            onPositionSizeChange();
        }
        
        
        @Override
        protected void onPositionSizeChange() {
            int x = getAbsoluteX();
            int y = getAbsoluteY();
            int w = getWidth();
            int h = getHeight();
            
            vid.setBounds((int) ((x + getScreenCoordinates().x) * zoomLevel), 
                    (int) ((y + getScreenCoordinates().y) * zoomLevel),
                    (int) (w * zoomLevel),
                    (int) (h * zoomLevel)
                    );
        }
    }

    public InputStream getResourceAsStream(Class cls, String resource) {
        if(baseResourceDir != null) {
            try {
                File f = new File(baseResourceDir, resource);
                if(f.exists()) {
                    return new FileInputStream(f);
                }
            } catch(IOException err) {
                return null;
            }
        }
        return super.getResourceAsStream(cls, resource);
    }
}
