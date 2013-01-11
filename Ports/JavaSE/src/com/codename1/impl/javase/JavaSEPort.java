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

import com.codename1.contacts.Address;
import com.codename1.contacts.Contact;
import com.codename1.db.Database;
import com.codename1.messaging.Message;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
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
import javax.swing.JFrame;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
//import java.awt.Menu;
//import java.awt.MenuBar;
//import java.awt.MenuItem;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
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
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.net.URI;
import java.sql.SQLException;
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
import com.codename1.io.NetworkManager;
import com.codename1.l10n.L10NManager;
import com.codename1.location.LocationManager;
import com.codename1.media.Media;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.Label;
import com.codename1.ui.PeerComponent;
import com.codename1.ui.TextArea;
import com.codename1.ui.animations.Motion;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Scrollbar;
import java.awt.TextComponent;
import java.awt.event.AdjustmentListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.awt.event.WindowAdapter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.sql.DriverManager;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.JTextComponent;

/**
 * An implementation of Codename One based on Java SE
 *
 * @author Shai Almog
 */
public class JavaSEPort extends CodenameOneImplementation {
    static {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
    }
    private static File baseResourceDir;
    private static final String DEFAULT_SKINS = "/iphone3gs.skin;/nexus.skin;/ipad.skin;/iphone4.skin;/iphone5.skin;/android.skin;/feature_phone.skin;/xoom.skin;/torch.skin;/lumia.skin";

    private TestRecorder testRecorder;
    
    /**
     * @return the showEDTWarnings
     */
    public static boolean isShowEDTWarnings() {
        return showEDTWarnings;
    }

    /**
     * @param aShowEDTWarnings the showEDTWarnings to set
     */
    public static void setShowEDTWarnings(boolean aShowEDTWarnings) {
        showEDTWarnings = aShowEDTWarnings;
    }

    /**
     * @return the showEDTViolationStacks
     */
    public static boolean isShowEDTViolationStacks() {
        return showEDTViolationStacks;
    }

    /**
     * @param aShowEDTViolationStacks the showEDTViolationStacks to set
     */
    public static void setShowEDTViolationStacks(boolean aShowEDTViolationStacks) {
        showEDTViolationStacks = aShowEDTViolationStacks;
    }
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
    private static boolean scrollableSkin = true;
    private JScrollBar hSelector = new JScrollBar(Scrollbar.HORIZONTAL);
    private JScrollBar vSelector = new JScrollBar(Scrollbar.VERTICAL);
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
    float zoomLevel = 1;
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
    private BufferedImage header;
    private BufferedImage headerLandscape;
    private String platformName = "ios";
    private String[] platformOverrides = new String[0];
    private static NetworkMonitor netMonitor;
    private static PerformanceMonitor perfMonitor;
    private static boolean blockMonitors;
    private static boolean fxExists = false;
    private JFrame window;
    private long lastIdleTime;
    private static boolean showEDTWarnings = true;
    private static boolean showEDTViolationStacks = false;
    private boolean inInit;
    private boolean showMenu = true;

    public static void blockMonitors() {
        blockMonitors = true;
    }

    static void disableNetworkMonitor() {
        netMonitor = null;
        Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
        pref.putBoolean("NetworkMonitor", false);
    }

    static void disablePerformanceMonitor() {
        perfMonitor = null;
        Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
        pref.putBoolean("PerformanceMonitor", false);
    }
    
    class EDTViolation extends Exception {
        public EDTViolation() {
            super("EDT Violation Stack!");
        }
    }
    
    private void checkEDT() {
        if(isShowEDTWarnings() && !Display.getInstance().isEdt() && !inInit) {
            System.out.println("EDT violation detected!");
            if(isShowEDTViolationStacks()) {
                new EDTViolation().printStackTrace();
            }
        } 
    }
    
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

    java.awt.Rectangle getScreenCoordinates() {
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
        checkEDT();
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

    class C extends JPanel implements KeyListener, MouseListener, MouseMotionListener, HierarchyBoundsListener, AdjustmentListener {

        private BufferedImage buffer;
        boolean painted;
        private Graphics2D g2dInstance;
        private java.awt.Dimension forcedSize;
        private boolean releaseLock;
        int x, y;

        C() {
            super(null);
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

        public void blit() {            
            if (buffer != null) {
                java.awt.Graphics g = getGraphics();
                if (g == null) {
                    return;
                }
                drawScreenBuffer(g);
                updateBufferSize();
                if(window != null){
                    
                    if (zoomLevel != 1) {
                        Graphics2D g2d = (Graphics2D) g;
                        g2d.setTransform(AffineTransform.getScaleInstance(1, 1));
                    }
                    
                    int count = window.getContentPane().getComponentCount();
                    boolean nativeCmp = false;
                    if(scrollableSkin){
                        if(count > 3){
                            nativeCmp = true;
                        }
                    }else{
                        if(count > 1){
                            nativeCmp = true;
                        }                    
                    }
                    if(nativeCmp){
                        java.awt.Component c = window.getContentPane().getComponent(0);
                        if(c.isVisible()){
                            g.translate(c.getX(), c.getY());
                            c.update(g);
                            g.translate(-c.getX(), -c.getY());
                        }
                    }
                    
                    
                    if(window.getJMenuBar() != null) {
                        for (int i = 0; i < window.getJMenuBar().getComponentCount(); i++) {
                            JMenu m = (JMenu) window.getJMenuBar().getComponent(i);
                            if(m.isPopupMenuVisible()){
                                JPopupMenu pop = m.getPopupMenu();
                                pop.getInvoker().getX();
                                g.translate(pop.getInvoker().getX(), 0);
                                pop.update(g);
                                g.translate(-pop.getInvoker().getX(), 0);
                            }                        
                        }
                    }
                }
                g.dispose();
            }
        }
        
        public void blit(int x, int y, int w, int h) {
            blit();
        }

        private void drawScreenBuffer(java.awt.Graphics g) {
            if (getScreenCoordinates() != null) {
                g.setColor(Color.WHITE);
                g.fillRect(x + (int) (getSkin().getWidth() * zoomLevel), y, getWidth(), getHeight());
                g.fillRect(x, y + (int) (getSkin().getHeight() * zoomLevel), getWidth(), getHeight());
                g.drawImage(buffer, (int) ((getScreenCoordinates().getX() + x) * zoomLevel), (int) ((getScreenCoordinates().getY() + y) * zoomLevel), this);
                updateGraphicsScale(g);
                g.drawImage(getSkin(), x, y, this);
            } else {
                g.drawImage(buffer, x, y, this);
            }
        }

        public void paint(java.awt.Graphics g) {
            super.paint(g);            
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
            buffer = createBufferedImage();
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
            int code = getCode(e);
            if(testRecorder != null) {
                testRecorder.eventKeyPressed(code);
            }
            JavaSEPort.this.keyPressed(code);
        }

        public void keyReleased(KeyEvent e) {
            // block key combos that might generate unreadable events
            if (e.isAltDown() || e.isControlDown() || e.isMetaDown() || e.isAltGraphDown()) {
                return;
            }
            int code = getCode(e);
            if(testRecorder != null) {
                testRecorder.eventKeyReleased(code);
            }
            JavaSEPort.this.keyReleased(code);
        }

        public void mouseClicked(MouseEvent e) {
            e.consume();
        }

        private int scaleCoordinateX(int coordinate) {
            if (getScreenCoordinates() != null) {
                return (int) (coordinate / zoomLevel - (getScreenCoordinates().x + x));
            }
            return coordinate;
        }

        private int scaleCoordinateY(int coordinate) {
            if (getScreenCoordinates() != null) {
                return (int) (coordinate / zoomLevel - (getScreenCoordinates().y + y));
            }
            return coordinate;
        }
        Integer triggeredKeyCode;

        public void mousePressed(MouseEvent e) {
            e.consume();
            if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0) {
                releaseLock = false;
                int x = scaleCoordinateX(e.getX());
                int y = scaleCoordinateY(e.getY());
                if (x >= 0 && x < getDisplayWidthImpl() && y >= 0 && y < getDisplayHeightImpl()) {
                    if (touchDevice) {
                        if(testRecorder != null) {
                            testRecorder.eventPointerPressed(x, y);
                        }
                        JavaSEPort.this.pointerPressed(x, y);
                    }
                } else {
                    if (getSkin() != null) {
                        java.awt.Point p = new java.awt.Point((int) ((e.getX() - canvas.x) / zoomLevel), (int) ((e.getY() - canvas.y) / zoomLevel));
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
                            int code = getCode(keyCode.intValue());
                            if(testRecorder != null) {
                                testRecorder.eventKeyPressed(code);
                            }
                            JavaSEPort.this.keyPressed(code);
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
                if (x >= 0 && x < getDisplayWidthImpl() && y >= 0 && y < getDisplayHeightImpl()) {
                    if (touchDevice) {
                        if(testRecorder != null) {
                            testRecorder.eventPointerReleased(x, y);
                        }
                        JavaSEPort.this.pointerReleased(x, y);
                    }
                }
                if (triggeredKeyCode != null) {
                    int code = getCode(triggeredKeyCode.intValue());
                    if(testRecorder != null) {
                        testRecorder.eventKeyReleased(code);
                    }
                    JavaSEPort.this.keyReleased(code);
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
            if (!releaseLock && (e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0) {
                int x = scaleCoordinateX(e.getX());
                int y = scaleCoordinateY(e.getY());
                if (x >= 0 && x < getDisplayWidthImpl() && y >= 0 && y < getDisplayHeightImpl()) {
                    if (touchDevice) {
                        if(testRecorder != null && hasDragStarted(x, y)) {
                            testRecorder.eventPointerDragged(x, y);
                        }
                        JavaSEPort.this.pointerDragged(x, y);
                    }
                } else {
                    x = Math.min(x, getDisplayWidthImpl());
                    x = Math.max(x, 0);
                    y = Math.min(y, getDisplayHeightImpl());
                    y = Math.max(y, 0);
                    if(testRecorder != null) {
                        testRecorder.eventPointerReleased(x, y);
                    }
                    JavaSEPort.this.pointerReleased(x, y);
                    releaseLock = true;
                }
            }
        }
        private Cursor handCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
        private Cursor defaultCursor = Cursor.getDefaultCursor();

        public void mouseMoved(MouseEvent e) {
            e.consume();
            if (getSkinHotspots() != null) {
                java.awt.Point p = new java.awt.Point((int) ((e.getX() - canvas.x) / zoomLevel), (int) ((e.getY() - canvas.y) / zoomLevel));
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
                if (!scrollableSkin) {
                    float w1 = ((float) getParent().getWidth()) / ((float) getSkin().getWidth());
                    float h1 = ((float) getParent().getHeight()) / ((float) getSkin().getHeight());
                    zoomLevel = Math.min(h1, w1);
                    Form f = Display.getInstance().getCurrent();
                    if (f != null) {
                        f.repaint();
                    }
                }
                getParent().repaint();
            } else {
                JavaSEPort.this.sizeChanged(getWidth(), getHeight());
            }
        }

        @Override
        public void adjustmentValueChanged(AdjustmentEvent e) {
            JScrollBar s = (JScrollBar) e.getSource();
            int val = s.getValue();
            if (s.getOrientation() == Scrollbar.HORIZONTAL) {
                x = -(int) ((float) (val / 100f) * getWidth());
            } else {
                y = -(int) ((float) (val / 100f) * getHeight());
            }
            repaint();

        }
    }
    C canvas;

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
     * Subclasses of this implementation might override this to return builtin
     * skins for a specific implementation
     *
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

    private void loadSkinFile(InputStream skin, final JFrame frm) {
        try {
            ZipInputStream z = new ZipInputStream(skin);
            ZipEntry e = z.getNextEntry();
            final Properties props = new Properties();
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
                if (name.equals("header.png")) {
                    header = ImageIO.read(z);
                    e = z.getNextEntry();
                    continue;
                }
                if (name.equals("header_l.png")) {
                    headerLandscape = ImageIO.read(z);
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
            String ua = null;
            if (platformName.equals("and")) {
                ua = "Mozilla/5.0 (Linux; U; Android 2.2; en-us; Nexus One Build/FRF91) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1";
            } else if (platformName.equals("rim")) {
                ua = "Mozilla/5.0 (BlackBerry; U; BlackBerry 9860; en-GB) AppleWebKit/534.11+ (KHTML, like Gecko) Version/7.0.0.296 Mobile Safari/534.11+";
            } else if (platformName.equals("ios")) {
                if (isTablet()) {
                    ua = "Mozilla/5.0 (iPad; U; CPU OS 4_3_1 like Mac OS X; en-us) AppleWebKit/533.17.9 (KHTML, like Gecko) Version/5.0.2 Mobile/8G4 Safari/6533.18.5";
                } else {
                    ua = "Mozilla/5.0 (iPod; U; CPU iPhone OS 4_3_3 like Mac OS X; en-us) AppleWebKit/533.17.9 (KHTML, like Gecko) Version/5.0.2 Mobile/8J2 Safari/6533.18.5";
                }
            } else if (platformName.equals("me")) {
                ua = "Mozilla/5.0 (SymbianOS/9.4; Series60/5.0 NokiaN97-1/20.0.019; Profile/MIDP-2.1 Configuration/CLDC-1.1) AppleWebKit/525 (KHTML, like Gecko) BrowserNG/7.1.18124";
            } else {
                if(platformName.equals("win")) {
                    ua = "Mozilla/5.0 (compatible; MSIE 9.0; Windows Phone OS 7.5; Trident/5.0; IEMobile/9.0; NOKIA; Lumia 800)";
                } else {
                    ua = "Mozilla/5.0 (Linux; U; Android 2.2; en-us; Nexus One Build/FRF91) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1";
                }
            }
            Display.getInstance().setProperty("User-Agent", ua);

            setFontFaces(props.getProperty("systemFontFamily", "Arial"),
                    props.getProperty("proportionalFontFamily", "SansSerif"),
                    props.getProperty("monospaceFontFamily", "Monospaced"));
            float factor = ((float) getDisplayHeightImpl()) / 480.0f;
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

            final byte[] nativeThemeFinalData = nativeThemeData;
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    if (nativeThemeFinalData != null) {
                        try {
                            nativeThemeRes = Resources.open(new ByteArrayInputStream(nativeThemeFinalData));
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    } else {
                        try {
                            String t = props.getProperty("nativeThemeAttribute", null);
                            if (t != null) {
                                Properties cnop = new Properties();
                                File cnopFile = new File("codenameone_settings.properties");
                                if (cnopFile.exists()) {
                                    cnop.load(new FileInputStream(cnopFile));
                                    t = cnop.getProperty(t, null);
                                    if (t != null && new File(t).exists()) {
                                        nativeThemeRes = Resources.open(new FileInputStream(t));
                                    }
                                }
                            }
                        } catch (IOException ioErr) {
                            ioErr.printStackTrace();
                        }
                    }
                }
            });

            JMenuBar bar = new JMenuBar();
            frm.setJMenuBar(bar);
            
            JMenu simulatorMenu = new JMenu("Simulate");
            simulatorMenu.setDoubleBuffered(true);
            
            JMenuItem rotate = new JMenuItem("Rotate");
            simulatorMenu.add(rotate);
            JMenu zoomMenu = new JMenu("Zoom");
            simulatorMenu.add(zoomMenu);
            
            JMenu debugEdtMenu = new JMenu("Debug EDT");
            simulatorMenu.add(debugEdtMenu);
            
            JMenuItem zoom50 = new JMenuItem("50%");
            zoomMenu.add(zoom50);
            JMenuItem zoom100 = new JMenuItem("100%");
            zoomMenu.add(zoom100);
            JMenuItem zoom200 = new JMenuItem("200%");
            zoomMenu.add(zoom200);
            
            JRadioButtonMenuItem debugEdtNone = new JRadioButtonMenuItem("None");
            JRadioButtonMenuItem debugEdtLight = new JRadioButtonMenuItem("Light");
            JRadioButtonMenuItem debugEdtFull = new JRadioButtonMenuItem("Full");
            debugEdtMenu.add(debugEdtNone);
            debugEdtMenu.add(debugEdtLight);
            debugEdtMenu.add(debugEdtFull);
            ButtonGroup bg = new ButtonGroup();
            bg.add(debugEdtNone);
            bg.add(debugEdtLight);
            bg.add(debugEdtFull);
            final Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
            int debugEdtSelection = pref.getInt("debugEDTMode", 0);
            switch(debugEdtSelection) {
                case 0:
                    debugEdtNone.setSelected(true);
                    setShowEDTWarnings(false);
                    setShowEDTViolationStacks(false);
                    break;
                case 2:
                    debugEdtFull.setSelected(true);
                    setShowEDTWarnings(true);
                    setShowEDTViolationStacks(true);
                    break;
                default:
                    debugEdtLight.setSelected(true);
                    setShowEDTWarnings(true);
                    setShowEDTViolationStacks(false);
                    break;
            }
            debugEdtNone.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setShowEDTWarnings(false);
                    setShowEDTViolationStacks(false);
                    pref.putInt("debugEDTMode", 0);
                }
            });
            debugEdtFull.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setShowEDTWarnings(true);
                    setShowEDTViolationStacks(true);
                    pref.putInt("debugEDTMode", 2);
                }
            });
            debugEdtLight.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setShowEDTWarnings(true);
                    setShowEDTViolationStacks(false);
                    pref.putInt("debugEDTMode", 1);
                }
            });
            
            JMenuItem screenshot = new JMenuItem("Screenshot");
            simulatorMenu.add(screenshot);
            screenshot.addActionListener(new ActionListener() {
                private File findScreenshotFile() {
                    int counter = 1;
                    File f = new File(System.getProperty("user.home"), "CodenameOne Screenshot " + counter + ".png");
                    while (f.exists()) {
                        counter++;
                        f = new File(System.getProperty("user.home"), "CodenameOne Screenshot " + counter + ".png");
                    }
                    return f;
                }

                @Override
                public void actionPerformed(ActionEvent ae) {
                    float zoom = zoomLevel;
                    zoomLevel = 1;
                    OutputStream out = null;
                    Form frm = Display.getInstance().getCurrent();
                    try {
                        BufferedImage headerImage;
                        if (isPortrait()) {
                            headerImage = header;
                        } else {
                            headerImage = headerLandscape;
                        }
                        int headerHeight = 0;
                        if (headerImage != null) {
                            headerHeight = headerImage.getHeight();
                        }
                        com.codename1.ui.Image img = com.codename1.ui.Image.createImage(frm.getWidth(), frm.getHeight());
                        com.codename1.ui.Graphics gr = img.getGraphics();
                        //gr.translate(0, statusBarHeight);
                        frm.paint(gr);
                        BufferedImage bi = new BufferedImage(frm.getWidth(), frm.getHeight() + headerHeight, BufferedImage.TYPE_INT_ARGB);
                        bi.setRGB(0, headerHeight, img.getWidth(), img.getHeight(), img.getRGB(), 0, img.getWidth());
                        if (headerImage != null) {
                            Graphics2D g2d = bi.createGraphics();
                            g2d.drawImage(headerImage, 0, 0, null);
                            g2d.dispose();
                        }

                        out = new FileOutputStream(findScreenshotFile());
                        ImageIO.write(bi, "png", out);
                        out.close();
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                        System.exit(1);
                    } finally {
                        zoomLevel = zoom;
                        try {
                            out.close();
                        } catch (Throwable ex) {
                        }
                        frm.repaint();
                        canvas.repaint();
                    }
                }
            });

            JMenuItem networkMonitor = new JMenuItem("Network Monitor");
            networkMonitor.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (netMonitor == null) {
                        showNetworkMonitor();
                        Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
                        pref.putBoolean("NetworkMonitor", true);
                    }
                }
            });
            simulatorMenu.add(networkMonitor);

            JMenuItem testRecorderMenu = new JMenuItem("Test Recorder");
            testRecorderMenu.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (testRecorder == null) {
                        showTestRecorder();
                    }
                }
            });
            simulatorMenu.add(testRecorderMenu);
            
            JMenuItem performanceMonitor = new JMenuItem("Performance Monitor");
            performanceMonitor.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (perfMonitor == null) {
                        showPerformanceMonitor();
                        Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
                        pref.putBoolean("PerformanceMonitor", true);
                    }
                }
            });
            simulatorMenu.add(performanceMonitor);

            JMenu skinMenu = new JMenu("Skins");
            String skinNames = pref.get("skins", DEFAULT_SKINS);
            if(!skinNames.contains("iphone5")) {
                skinNames = DEFAULT_SKINS;
            }
            if (skinNames != null) {
                if (skinNames.length() < DEFAULT_SKINS.length()) {
                    skinNames = DEFAULT_SKINS;
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
                    JMenuItem i = new JMenuItem(name);
                    i.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            if (netMonitor != null) {
                                netMonitor.dispose();
                                netMonitor = null;
                            }
                            if (perfMonitor != null) {
                                perfMonitor.dispose();
                                perfMonitor = null;
                            }
                            String mainClass = System.getProperty("MainClass");
                            if (mainClass != null) {
                                Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
                                pref.put("skin", current);
                                deinitializeSync();
                                frm.dispose();
                                System.setProperty("reload.simulator", "true");
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
            JMenuItem addSkin = new JMenuItem("Add");
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
                        if (netMonitor != null) {
                            netMonitor.dispose();
                            netMonitor = null;
                        }
                        if (perfMonitor != null) {
                            perfMonitor.dispose();
                            perfMonitor = null;
                        }
                        String mainClass = System.getProperty("MainClass");
                        if (mainClass != null) {
                            Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
                            pref.put("skin", picker.getDirectory() + File.separator + file);
                            deinitializeSync();
                            frm.dispose();
                            System.setProperty("reload.simulator", "true");
                        } else {
                            loadSkinFile(picker.getDirectory() + File.separator + file, frm);
                            refreshSkin(frm);
                        }
                    }
                }
            });

            final JCheckBoxMenuItem touchFlag = new JCheckBoxMenuItem("Touch", touchDevice);
            simulatorMenu.add(touchFlag);
            final JCheckBoxMenuItem nativeInputFlag = new JCheckBoxMenuItem("Native Input", useNativeInput);
            simulatorMenu.add(nativeInputFlag);

            final JCheckBoxMenuItem scrollFlag = new JCheckBoxMenuItem("Scrollable", scrollableSkin);
            simulatorMenu.add(scrollFlag);

            final JCheckBoxMenuItem slowMotionFlag = new JCheckBoxMenuItem("Slow Motion", false);
            simulatorMenu.add(slowMotionFlag);
            slowMotionFlag.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Motion.setSlowMotion(slowMotionFlag.isSelected());
                }
            });

            simulatorMenu.addSeparator();
            JMenuItem exit = new JMenuItem("Exit");
            simulatorMenu.add(exit);
            
            if(showMenu){
                bar.add(simulatorMenu);
                bar.add(skinMenu);
            }
            
            rotate.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    portrait = !portrait;
                    Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
                    pref.putBoolean("Portrait", portrait);

                    float w1 = ((float) canvas.getWidth()) / ((float) getSkin().getWidth());
                    float h1 = ((float) canvas.getHeight()) / ((float) getSkin().getHeight());
                    zoomLevel = Math.min(h1, w1);
                    frm.remove(canvas);
                    canvas.setForcedSize(new java.awt.Dimension(getSkin().getWidth(), getSkin().getHeight()));
                    frm.add(BorderLayout.CENTER, canvas);
                    frm.pack();

                    zoomLevel = 1;
                    JavaSEPort.this.sizeChanged(getScreenCoordinates().width, getScreenCoordinates().height);
                }
            });
            zoom100.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    frm.remove(canvas);
                    canvas.setForcedSize(new java.awt.Dimension(getSkin().getWidth(), getSkin().getHeight()));
                    frm.add(BorderLayout.CENTER, canvas);
                    frm.pack();
                    zoomLevel = 1;
                    Display.getInstance().getCurrent().repaint();
                    frm.repaint();
                }
            });
            zoom50.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    frm.remove(canvas);
                    canvas.setForcedSize(new java.awt.Dimension(getSkin().getWidth() / 2, getSkin().getHeight() / 2));
                    frm.add(BorderLayout.CENTER, canvas);
                    frm.pack();
                    zoomLevel = 0.5f;
                    Display.getInstance().getCurrent().repaint();
                    frm.repaint();
                }
            });
            zoom200.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    frm.remove(canvas);
                    canvas.setForcedSize(new java.awt.Dimension(getSkin().getWidth() * 2, getSkin().getHeight() * 2));
                    frm.add(BorderLayout.CENTER, canvas);
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

            scrollFlag.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent ie) {
                    scrollableSkin = !scrollableSkin;
                    Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
                    pref.putBoolean("Scrollable", scrollableSkin);

                    if (scrollableSkin) {
                        frm.add(java.awt.BorderLayout.SOUTH, hSelector);
                        frm.add(java.awt.BorderLayout.EAST, vSelector);
                    } else {
                        frm.remove(hSelector);
                        frm.remove(vSelector);
                    }
                    frm.remove(canvas);
                    canvas.setForcedSize(new java.awt.Dimension(getSkin().getWidth(), getSkin().getHeight()));
                    frm.add(BorderLayout.CENTER, canvas);
                    
                    canvas.x = 0;
                    canvas.y = 0;
                    zoomLevel = 1;
                    frm.invalidate();
                    frm.pack();
                    Display.getInstance().getCurrent().repaint();
                    frm.repaint();
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

    private void showTestRecorder() {
        if(testRecorder == null) {
            testRecorder = new TestRecorder();
            testRecorder.pack();
            testRecorder.setLocationByPlatform(true);
            testRecorder.setVisible(true);
            testRecorder.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    testRecorder = null;
                }
            });
        }
    }
    
    private void showNetworkMonitor() {
        if (netMonitor == null) {
            netMonitor = new NetworkMonitor();
            netMonitor.pack();
            netMonitor.setLocationByPlatform(true);
            netMonitor.setVisible(true);
        }
    }

    private void showPerformanceMonitor() {
        if (perfMonitor == null) {
            perfMonitor = new PerformanceMonitor();
            perfMonitor.pack();
            perfMonitor.setLocationByPlatform(true);
            perfMonitor.setVisible(true);
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

    private void refreshSkin(final JFrame frm) {
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
    
    public void deinitializeSync() {
        final Thread[] t = new Thread[1];
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            @Override
            public void run() {
                t[0] = Thread.currentThread();
            }
        }, 250);
        Display.deinitialize();
        NetworkManager.getInstance().shutdownSync();
        try {
            if (t[0] != null) {
                t[0].join();
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    private void loadSkinFile(String f, JFrame frm) {
        try {
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
        catch(Throwable t) {
            t.printStackTrace();
            Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
            pref.remove("skin");
        }
    }

    /**
     * @inheritDoc
     */
    public void init(Object m) {
        inInit = true;
        try {
            Class.forName("javafx.embed.swing.JFXPanel");
            Platform.setImplicitExit(false);
            fxExists = true;
        } catch (Throwable ex) {
        }
        
        
        String hide = System.getProperty("hideMenu", "false");
        if(hide != null && hide.equals("true")){
            showMenu = false;
        }

        URLConnection.setDefaultAllowUserInteraction(true);
        HttpURLConnection.setFollowRedirects(false);
        Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
        if (!blockMonitors && pref.getBoolean("NetworkMonitor", false)) {
            showNetworkMonitor();
        }
        if (!blockMonitors && pref.getBoolean("PerformanceMonitor", false)) {
            showPerformanceMonitor();
        }
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
            window = new JFrame();
            window.addWindowListener(new WindowListener() {
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


            window.setLocationByPlatform(true);
            window.setLayout(new java.awt.BorderLayout());
            hSelector = new JScrollBar(Scrollbar.HORIZONTAL);
            vSelector = new JScrollBar(Scrollbar.VERTICAL);
            hSelector.addAdjustmentListener(canvas);
            vSelector.addAdjustmentListener(canvas);


            scrollableSkin = pref.getBoolean("Scrollable", true);
            if (scrollableSkin) {
                window.add(java.awt.BorderLayout.SOUTH, hSelector);
                window.add(java.awt.BorderLayout.EAST, vSelector);
            }
            window.add(java.awt.BorderLayout.CENTER, canvas);

            if (hasSkins()) {
                String f = System.getProperty("skin");
                if (f != null) {
                    loadSkinFile(f, window);
                } else {
                    String d = System.getProperty("dskin");
                    f = pref.get("skin", d);
                    loadSkinFile(f, window);
                }
            } else {
                Resources.setRuntimeMultiImageEnabled(true);
                window.setUndecorated(true);
                window.setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
            window.pack();
            if (getSkin() != null && !scrollableSkin) {
                float w1 = ((float) canvas.getWidth()) / ((float) getSkin().getWidth());
                float h1 = ((float) canvas.getHeight()) / ((float) getSkin().getHeight());
                zoomLevel = Math.min(h1, w1);
            }

            portrait = pref.getBoolean("Portrait", true);
            if (!portrait) {
                canvas.setForcedSize(new java.awt.Dimension(getSkin().getWidth(), getSkin().getHeight()));
                window.setSize(new java.awt.Dimension(getSkin().getWidth(), getSkin().getHeight()));
            }
            window.setVisible(true);
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
        inInit = false;
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
        checkEDT();
    }

    /**
     * @inheritDoc
     */
    public int getDisplayWidth() {
        return getDisplayWidthImpl();
    }

    private int getDisplayWidthImpl() {
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
        return getDisplayHeightImpl();
    }

    private int getDisplayHeightImpl() {
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
     * yet caches it. This method is in the porting layer since CLDC only
     * includes weak references while some platforms include nothing at all and
     * some include the superior soft references.
     *
     * @param o object to cache
     * @return a caching object or null if caching isn't supported
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
        checkEDT();
        return useNativeInput;
    }

    public boolean isNativeInputImmediate() {
        checkEDT();
        return useNativeInput;
    }

    private void setText(java.awt.Component c, String text) {
        if (c instanceof java.awt.TextComponent) {
            ((java.awt.TextComponent) c).setText(text);
        } else {
            ((JTextComponent) c).setText(text);
        }
    }

    private String getText(java.awt.Component c) {
        if (c instanceof java.awt.TextComponent) {
            return ((java.awt.TextComponent) c).getText();
        } else {
            return ((JTextComponent) c).getText();
        }
    }

    private void setCaretPosition(java.awt.Component c, int p) {
        if (c instanceof java.awt.TextComponent) {
            ((java.awt.TextComponent) c).setCaretPosition(p);
        } else {
            ((JTextComponent) c).setCaretPosition(p);
        }
    }

    private int getCaretPosition(java.awt.Component c) {
        if (c instanceof java.awt.TextComponent) {
            return ((java.awt.TextComponent) c).getCaretPosition();
        } else {
            return ((JTextComponent) c).getCaretPosition();
        }
    }

    /**
     * @inheritDoc
     */
    public void editString(final Component cmp, int maxSize, int constraint, String text, int keyCode) {
        checkEDT();
        java.awt.Component awtTf;
        
        if (cmp instanceof com.codename1.ui.TextField) {
            java.awt.TextField t = new java.awt.TextField();
            awtTf = t;
            t.setSelectionEnd(0);
            t.setSelectionStart(0);
        } else {
            java.awt.TextArea t = new java.awt.TextArea("", 0, 0, java.awt.TextArea.SCROLLBARS_NONE);;
            awtTf = t;
            t.setSelectionEnd(0);
            t.setSelectionStart(0);
        }
        final java.awt.Component tf = awtTf;
        if (keyCode > 0) {
            text += ((char) keyCode);
            setText(tf, text);
            setCaretPosition(tf, text.length());
        } else {
            setText(tf, text);
        }
        canvas.add(tf);
        if (getSkin() != null) {
            tf.setBounds((int) ((cmp.getAbsoluteX() + getScreenCoordinates().x + canvas.x) * zoomLevel),
                    (int) ((cmp.getAbsoluteY() + getScreenCoordinates().y + canvas.y) * zoomLevel),
                    (int) (cmp.getWidth() * zoomLevel), (int) (cmp.getHeight() * zoomLevel));
            java.awt.Font f = font(cmp.getStyle().getFont().getNativeFont());
            tf.setFont(f.deriveFont(f.getSize2D() * zoomLevel));
        } else {
            tf.setBounds(cmp.getAbsoluteX(), cmp.getAbsoluteY(), cmp.getWidth(), cmp.getHeight());
            tf.setFont(font(cmp.getStyle().getFont().getNativeFont()));
        }
        setCaretPosition(tf, getText(tf).length());
        tf.requestFocus();
        class Listener implements ActionListener, FocusListener, KeyListener, TextListener, Runnable {

            public synchronized void run() {
                while (tf.getParent() != null) {
                    try {
                        wait(20);
                    } catch (InterruptedException ex) {
                    }
                }
            }

            public void actionPerformed(ActionEvent e) {
                String txt = getText(tf);
                if(testRecorder != null) {
                    testRecorder.editTextFieldCompleted(cmp, txt);
                }
                Display.getInstance().onEditingComplete(cmp, txt);
                if (tf instanceof java.awt.TextField) {
                    ((java.awt.TextField) tf).removeActionListener(this);
                }
                ((TextComponent)tf).removeTextListener(this);
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
                String t = getText(tf);
                
                if(t.length() >= ((TextArea)cmp).getMaxSize()){
                    e.consume();
                }
            }

            public void keyPressed(KeyEvent e) {
            }

            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    if (tf instanceof java.awt.TextField) {
                        actionPerformed(null);
                    } else {
                        if (getCaretPosition(tf) >= getText(tf).length() - 1) {
                            actionPerformed(null);
                        }
                    }
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    if (tf instanceof java.awt.TextField) {
                        actionPerformed(null);
                    } else {
                        if (getCaretPosition(tf) <= 2) {
                            actionPerformed(null);
                        }
                    }
                    return;
                }
            }

            public void textValueChanged(TextEvent e) {
                if(cmp instanceof com.codename1.ui.TextField){
                    ((com.codename1.ui.TextField)cmp).setText(getText(tf));
                }
                
            }
        };
        final Listener l = new Listener();
        if (tf instanceof java.awt.TextField) {
            ((java.awt.TextField) tf).addActionListener(l);            
        }
        ((TextComponent)tf).addTextListener(l);
        
        
        tf.addKeyListener(l);
        tf.addFocusListener(l);
        Display.getInstance().invokeAndBlock(l);
    }

    /**
     * @inheritDoc
     */
    public void saveTextEditingState() {
    }

    @Override
    public void edtIdle(boolean enter) {
        if(isShowEDTWarnings()) {
            if(enter) {
                checkLastFrame();
            } else {
                lastIdleTime = System.currentTimeMillis();
            }
        }
    }
    
    private void checkLastFrame() {
        long t = System.currentTimeMillis();
        if(lastIdleTime > 0) {
            long diff = t - lastIdleTime;
            if(diff > 150) {
                System.out.println("Rendering frame took too long " + diff + " milliseconds");
            }
        }
        lastIdleTime = t;
    }

    /**
     * @inheritDoc
     */
    public void flushGraphics(int x, int y, int width, int height) {
        if(isShowEDTWarnings()) {
            checkEDT();
            checkLastFrame();
        }
        canvas.blit(x, y, width, height);
    }

    /**
     * @inheritDoc
     */
    public void flushGraphics() {
        if(isShowEDTWarnings()) {
            checkEDT();
            checkLastFrame();
        }
        canvas.blit();
    }

    /**
     * @inheritDoc
     */
    public void getRGB(Object nativeImage, int[] arr, int offset, int x, int y, int width, int height) {
        checkEDT();
        ((BufferedImage) nativeImage).getRGB(x, y, width, height, arr, offset, width);
    }

    private BufferedImage createTrackableBufferedImage(final int width, final int height) {
        return createTrackableBufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    private BufferedImage createTrackableBufferedImage(final int width, final int height, int type) {
        if (perfMonitor != null) {
            BufferedImage i = new BufferedImage(width, height, type) {
                public void finalize() throws Throwable {
                    super.finalize();
                    if (perfMonitor != null) {
                        perfMonitor.removeImageRAM(width * height * 4);
                    }
                }
            };
            perfMonitor.addImageRAM(width * height * 4);
            return i;
        } else {
            return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }
    }

    /**
     * @inheritDoc
     */
    public Object createImage(int[] rgb, final int width, final int height) {
        BufferedImage i = createTrackableBufferedImage(width, height);
        i.setRGB(0, 0, width, height, rgb, 0, width);
        if (perfMonitor != null) {
            perfMonitor.printToLog("Created RGB image width: " + width + " height: " + height
                    + " size (bytes) " + (width * height * 4));
        }
        return i;
    }

    private BufferedImage cloneTrackableBufferedImage(BufferedImage b) {
        final int width = b.getWidth();
        final int height = b.getHeight();
        BufferedImage n = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB) {
            public void finalize() throws Throwable {
                super.finalize();
                if (perfMonitor != null) {
                    perfMonitor.removeImageRAM(width * height * 4);
                }
            }
        };
        Graphics2D g2d = n.createGraphics();
        g2d.drawImage(b, 0, 0, canvas);
        g2d.dispose();
        perfMonitor.addImageRAM(width * height * 4);
        return n;
    }

    /**
     * @inheritDoc
     */
    public Object createImage(String path) throws IOException {
        if (exists(path)) {
            InputStream is = null;
            try {
                is = openInputStream(path);
                return createImage(is);
            } finally {
                is.close();
            }
        }

        try {
            InputStream i = getResourceAsStream(clsInstance, path);

            // prevents a security exception due to a JDK bug which for some stupid reason chooses
            // to create a temporary file in the spi of Image IO
            BufferedImage b = ImageIO.read(new MemoryCacheImageInputStream(i));
            if (perfMonitor != null) {
                b = cloneTrackableBufferedImage(b);
                perfMonitor.printToLog("Created path image " + path + " width: " + b.getWidth() + " height: " + b.getHeight()
                        + " size (bytes) " + (b.getWidth() * b.getHeight() * 4));
            }
            return b;
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
            BufferedImage b = ImageIO.read(i);
            if (perfMonitor != null) {
                b = cloneTrackableBufferedImage(b);
                perfMonitor.printToLog("Created InputStream image width: " + b.getWidth() + " height: " + b.getHeight()
                        + " size (bytes) " + (b.getWidth() * b.getHeight() * 4));
            }
            return b;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new IOException(t.toString());
        }
    }

    /**
     * @inheritDoc
     */
    public Object createMutableImage(int width, int height, int fillColor) {
        checkEDT();
        if (perfMonitor != null) {
            perfMonitor.printToLog("Created mutable image width: " + width + " height: " + height
                    + " size (bytes) " + (width * height * 4));
        }
        int a = (fillColor >> 24) & 0xff;
        if (a == 0xff) {
            BufferedImage b = createTrackableBufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = b.createGraphics();
            g.setColor(new Color(fillColor));
            g.fillRect(0, 0, width, height);
            g.dispose();
            return b;
        }
        BufferedImage b = createTrackableBufferedImage(width, height);
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
        checkEDT();
        return true;
    }

    /**
     * @inheritDoc
     */
    public Object createImage(byte[] bytes, int offset, int len) {
        try {
            BufferedImage b = ImageIO.read(new ByteArrayInputStream(bytes, offset, len));
            if (perfMonitor != null) {
                b = cloneTrackableBufferedImage(b);
                perfMonitor.printToLog("Created data image width: " + b.getWidth() + " height: " + b.getHeight()
                        + " data size (bytes) " + bytes.length
                        + " unpacked size (bytes) " + (b.getWidth() * b.getHeight() * 4));
            }
            return b;
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
        checkEDT();
        if (i == null) {
            return 0;
        }
        return ((BufferedImage) i).getWidth();
    }

    /**
     * @inheritDoc
     */
    public int getImageHeight(Object i) {
        checkEDT();
        if (i == null) {
            return 0;
        }
        return ((BufferedImage) i).getHeight();
    }

    /**
     * @inheritDoc
     */
    public boolean isScaledImageDrawingSupported() {
        checkEDT();
        return true;
    }

    /**
     * @inheritDoc
     */
    public Object scale(Object nativeImage, int width, int height) {
        checkEDT();
        BufferedImage image = (BufferedImage) nativeImage;
        int srcWidth = image.getWidth();
        int srcHeight = image.getHeight();

        if (perfMonitor != null) {
            perfMonitor.printToLog("Scaling image from width: " + srcWidth + " height: " + srcHeight
                    + " to width: " + width + " height: " + height
                    + " size (bytes) " + (width * height * 4));
        }

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
        checkEDT();
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
        checkEDT();
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.setFont(font(font));
    }

    /**
     * @inheritDoc
     */
    public int getClipX(Object graphics) {
        checkEDT();
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
        checkEDT();
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
        checkEDT();
        Graphics2D nativeGraphics = getGraphics(graphics);
        java.awt.Rectangle r = nativeGraphics.getClipBounds();
        if (r == null) {
            if (graphics instanceof NativeScreenGraphics) {
                NativeScreenGraphics ng = (NativeScreenGraphics) graphics;
                if (ng.sourceImage != null) {
                    return ng.sourceImage.getWidth();
                }
            }
            return getDisplayWidthImpl();
        }
        return r.width;
    }

    /**
     * @inheritDoc
     */
    public int getClipHeight(Object graphics) {
        checkEDT();
        Graphics2D nativeGraphics = getGraphics(graphics);
        java.awt.Rectangle r = nativeGraphics.getClipBounds();
        if (r == null) {
            if (graphics instanceof NativeScreenGraphics) {
                NativeScreenGraphics ng = (NativeScreenGraphics) graphics;
                if (ng.sourceImage != null) {
                    return ng.sourceImage.getHeight();
                }
            }
            return getDisplayHeightImpl();
        }
        return r.height;
    }

    /**
     * @inheritDoc
     */
    public void setClip(Object graphics, int x, int y, int width, int height) {
        checkEDT();
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.setClip(x, y, width, height);
    }

    /**
     * @inheritDoc
     */
    public void clipRect(Object graphics, int x, int y, int width, int height) {
        checkEDT();
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.clipRect(x, y, width, height);
    }

    /**
     * @inheritDoc
     */
    public void drawLine(Object graphics, int x1, int y1, int x2, int y2) {
        checkEDT();
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.drawLine(x1, y1, x2, y2);
    }

    /**
     * @inheritDoc
     */
    public void fillRect(Object graphics, int x, int y, int w, int h) {
        checkEDT();
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.fillRect(x, y, w, h);
    }

    /**
     * @inheritDoc
     */
    public boolean isAlphaGlobal() {
        checkEDT();
        return true;
    }

    /**
     * @inheritDoc
     */
    public void drawRect(Object graphics, int x, int y, int width, int height) {
        checkEDT();
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.drawRect(x, y, width, height);
    }

    /**
     * @inheritDoc
     */
    public void drawRoundRect(Object graphics, int x, int y, int width, int height, int arcWidth, int arcHeight) {
        checkEDT();
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    /**
     * @inheritDoc
     */
    public void fillRoundRect(Object graphics, int x, int y, int width, int height, int arcWidth, int arcHeight) {
        checkEDT();
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    /**
     * @inheritDoc
     */
    public void fillArc(Object graphics, int x, int y, int width, int height, int startAngle, int arcAngle) {
        checkEDT();
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.fillArc(x, y, width, height, startAngle, arcAngle);
    }

    /**
     * @inheritDoc
     */
    public void drawArc(Object graphics, int x, int y, int width, int height, int startAngle, int arcAngle) {
        checkEDT();
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.drawArc(x, y, width, height, startAngle, arcAngle);
    }

    /**
     * @inheritDoc
     */
    public void setColor(Object graphics, int RGB) {
        checkEDT();
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.setColor(new Color(RGB));
    }

    /**
     * @inheritDoc
     */
    public int getColor(Object graphics) {
        checkEDT();
        Graphics2D nativeGraphics = getGraphics(graphics);
        return nativeGraphics.getColor().getRGB();
    }

    /**
     * @inheritDoc
     */
    public void setAlpha(Object graphics, int alpha) {
        checkEDT();
        Graphics2D nativeGraphics = getGraphics(graphics);
        float a = ((float) alpha) / 255.0f;
        nativeGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
    }

    /**
     * @inheritDoc
     */
    public int getAlpha(Object graphics) {
        checkEDT();
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
        checkEDT();
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
        checkEDT();
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.drawImage((BufferedImage) img, x, y, null);
    }

    /**
     * @inheritDoc
     */
    public void drawImage(Object graphics, Object img, int x, int y, int w, int h) {
        checkEDT();
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.drawImage((BufferedImage) img, x, y, w, h, null);
    }

    /**
     * @inheritDoc
     */
    public void fillTriangle(Object graphics, int x1, int y1, int x2, int y2, int x3, int y3) {
        checkEDT();
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.fillPolygon(new int[]{x1, x2, x3}, new int[]{y1, y2, y3}, 3);
    }
    private BufferedImage cache;

    /**
     * @inheritDoc
     */
    public void drawRGB(Object graphics, int[] rgbData, int offset, int x, int y, int w, int h, boolean processAlpha) {
        checkEDT();
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
        checkEDT();
        return stringWidth(nativeFont, new String(ch, offset, length));
    }

    /**
     * @inheritDoc
     */
    public int stringWidth(Object nativeFont, String str) {
        checkEDT();
        return (int) Math.ceil(font(nativeFont).getStringBounds(str, canvas.getFRC()).getWidth());
    }

    /**
     * @inheritDoc
     */
    public int charWidth(Object nativeFont, char ch) {
        checkEDT();
        return (int) Math.ceil(font(nativeFont).getStringBounds("" + ch, canvas.getFRC()).getWidth());
    }

    /**
     * @inheritDoc
     */
    public int getHeight(Object nativeFont) {
        checkEDT();
        return font(nativeFont).getSize() + 1;
    }

    /**
     * @inheritDoc
     */
    public Object createFont(int face, int style, int size) {
        checkEDT();
        return new int[]{face, style, size};
    }

    @Override
    public boolean isTrueTypeSupported() {
        return true;
    }

    @Override
    public Object loadTrueTypeFont(String fontName, String fileName) {
        File fontFile;
        if(baseResourceDir != null) {
            fontFile = new File(baseResourceDir, fileName);
        } else {
            fontFile = new File("src", fileName);
        }
        if(fontFile.exists()) {
            try {
                FileInputStream fs = new FileInputStream(fontFile);
                java.awt.Font fnt = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, fs);
                fs.close();
                if(fnt != null) {
                    if(!fontName.startsWith(fnt.getFamily())) {
                        System.out.println("Warning font name might be wrong for " + fileName + " should be: " + fnt.getName());
                    }
                }
                return fnt;
            } catch(Exception err) {
                err.printStackTrace();
                throw new RuntimeException(err);
            }
        } 
        throw new RuntimeException("The file wasn't found: " + fontFile.getAbsolutePath());
    }

    @Override
    public Object deriveTrueTypeFont(Object font, float size, int weight) {
        java.awt.Font fnt = (java.awt.Font)font;
        int style = java.awt.Font.PLAIN;
        if((weight & com.codename1.ui.Font.STYLE_BOLD) == com.codename1.ui.Font.STYLE_BOLD) {
            style = java.awt.Font.BOLD;
        }
        if((weight & com.codename1.ui.Font.STYLE_ITALIC) == com.codename1.ui.Font.STYLE_ITALIC) {
            style = style | java.awt.Font.ITALIC;
        }
        return fnt.deriveFont(style, size);
    }    
    
    private java.awt.Font createAWTFont(int[] i) {
        checkEDT();
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
        checkEDT();
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
        checkEDT();
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
        checkEDT();
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
        checkEDT();
        return java.awt.Font.decode(lookup.split(";")[0]);
    }

    /**
     * @inheritDoc
     */
    public void fillPolygon(Object graphics, int[] xPoints, int[] yPoints, int nPoints) {
        checkEDT();
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.fillPolygon(xPoints, yPoints, nPoints);
    }

    /**
     * @inheritDoc
     */
    public void drawPolygon(Object graphics, int[] xPoints, int[] yPoints, int nPoints) {
        checkEDT();
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.drawPolygon(xPoints, yPoints, nPoints);
    }

    @Override
    public boolean animateImage(Object nativeImage, long lastJFrame) {
        checkEDT();
        return false;
    }

    @Override
    public Object createSVGImage(String baseURL, byte[] data) throws IOException {
        checkEDT();
        return null;
    }

    @Override
    public boolean isSVGSupported() {
        checkEDT();
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
        if ("AppVersion".equals(key)) {
            File f = new File("codenameone_settings.properties");
            if (f.exists()) {
                try {
                    Properties p = new Properties();
                    p.load(new FileInputStream(f));
                    return p.getProperty("codename1.version");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            return null;
        }
        String s = System.getProperty(key);

        if(key.equals("built_by_user")) {
            Preferences p = Preferences.userNodeForPackage(com.codename1.ui.Component.class);
            String user = p.get("user", null);
            Display d = Display.getInstance();
            if (user == null) {
                JPanel pnl = new JPanel();
                JTextField tf = new JTextField(20);
                pnl.add(new JLabel("E-Mail For Push"));
                pnl.add(tf);
                int val = JOptionPane.showConfirmDialog(canvas, pnl, "Please Enter Build Email Account", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                if(val != JOptionPane.OK_OPTION) {
                    return null;
                }
                user = tf.getText();
                //p.put("user", user);
            }
            d.setProperty("built_by_user", user);
            return user;
        }
        
        if(key.equals("package_name")) {
            String mainClass = System.getProperty("MainClass");
            if (mainClass != null) {
                mainClass = mainClass.substring(0, mainClass.lastIndexOf('.'));
                Display.getInstance().setProperty("package_name", mainClass);
            }
            return mainClass;
        }
        
        
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
     * @param onCompletion invoked when the audio file finishes playing, may be
     * null
     * @return a handle that can be used to control the playback of the audio
     * @throws java.io.IOException if the URI access fails
     */
    public Media createMedia(final String uri, final boolean isVideo, final Runnable onCompletion) throws IOException {
        if (!fxExists) {
            String msg = "This fetaure is supported from Java version 1.7.0_06, update your Java to enable this feature";
            System.out.println(msg);
            throw new IOException(msg);
        }
        java.awt.Container cnt = canvas.getParent();
        while (!(cnt instanceof JFrame)) {
            cnt = cnt.getParent();
            if (cnt == null) {
                return null;
            }
        }
        final java.awt.Container c = cnt;

        final Media[] media = new Media[1];
        final Exception[] err = new Exception[1];
        final javafx.embed.swing.JFXPanel mediaContainer = new javafx.embed.swing.JFXPanel();
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                try {
                    if (uri.indexOf(':') < 0) {
                        String mimeType = "video/mp4";
                        media[0] = new CodenameOneMediaPlayer(getResourceAsStream(getClass(), uri), mimeType, (JFrame) c, mediaContainer, onCompletion);
                    }

                    media[0] = new CodenameOneMediaPlayer(uri, isVideo, (JFrame) c, mediaContainer, onCompletion);
                } catch (Exception ex) {
                    err[0] = ex;
                }
            }
        });

        Display.getInstance().invokeAndBlock(new Runnable() {
            @Override
            public void run() {
                while (media[0] == null && err[0] == null) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                    }
                }
            }
        });
        if (err[0] != null) {
            throw new IOException(err[0]);
        }
        return media[0];

    }

    /**
     * Plays the sound in the given stream
     *
     * @param stream the stream containing the media data
     * @param mimeType the type of the data in the stream
     * @param onCompletion invoked when the audio file finishes playing, may be
     * null
     * @return a handle that can be used to control the playback of the audio
     * @throws java.io.IOException if the URI access fails
     */
    public Media createMedia(final InputStream stream, final String mimeType, final Runnable onCompletion) throws IOException {
        if (!fxExists) {
            String msg = "This fetaure is supported from Java version 1.7.0_06, update your Java to enable this feature";
            System.out.println(msg);
            throw new IOException(msg);
        }
        java.awt.Container cnt = canvas.getParent();
        while (!(cnt instanceof JFrame)) {
            cnt = cnt.getParent();
            if (cnt == null) {
                return null;
            }
        }
        final java.awt.Container c = cnt;

        final Media[] media = new Media[1];
        final Exception[] err = new Exception[1];
        final javafx.embed.swing.JFXPanel mediaContainer = new javafx.embed.swing.JFXPanel();

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                try {
                    media[0] = new CodenameOneMediaPlayer(stream, mimeType, (JFrame) c, mediaContainer, onCompletion);
                } catch (Exception ex) {
                    err[0] = ex;
                }
            }
        });

        Display.getInstance().invokeAndBlock(new Runnable() {
            @Override
            public void run() {
                while (media[0] == null && err[0] == null) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                    }
                }
            }
        });
        if (err[0] != null) {
            throw new IOException(err[0]);
        }
        return media[0];
    }

    private class NativeScreenGraphics {
        BufferedImage sourceImage;
        Graphics2D cachedGraphics;
    }

    public boolean isAffineSupported() {
        checkEDT();
        return true;
    }

    public void resetAffine(Object nativeGraphics) {
        checkEDT();
        Graphics2D g = getGraphics(nativeGraphics);
        g.setTransform(new AffineTransform());
        if (zoomLevel != 1) {
            g.setTransform(AffineTransform.getScaleInstance(zoomLevel, zoomLevel));
        }
    }

    public void scale(Object nativeGraphics, float x, float y) {
        checkEDT();
        Graphics2D g = getGraphics(nativeGraphics);
        g.scale(x, y);
    }

    public void rotate(Object nativeGraphics, float angle) {
        checkEDT();
        Graphics2D g = getGraphics(nativeGraphics);
        g.rotate(angle);
    }

    public void rotate(Object nativeGraphics, float angle, int pX, int pY) {
        checkEDT();
        Graphics2D g = getGraphics(nativeGraphics);
        g.rotate(angle, pX, pY);
    }

    public void shear(Object nativeGraphics, float x, float y) {
        checkEDT();
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
        checkEDT();
        Graphics2D g2d = getGraphics(graphics);
        if (a) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        } else {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        }
    }

    public boolean isAntiAliased(Object graphics) {
        checkEDT();
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
            if (getStorageData() == null) {
                String mainClass = System.getProperty("MainClass");
                if (mainClass != null) {
                    setStorageData(mainClass);
                } else {
                    setStorageData("CodenameOneStorage");
                }
            }
            storageDir = new File(System.getProperty("user.home"), "." + ((String) getStorageData()));
            storageDir.mkdirs();
        }
        return storageDir;
    }

    /**
     * @inheritDoc
     */
    public Object connect(String url, boolean read, boolean write) throws IOException {
        /*if(currentAp == null || !currentAp.equals("22")){
         throw new IOException("apn error");
         }*/
        URL u = new URL(url);

        URLConnection con = u.openConnection();

        if (con instanceof HttpURLConnection) {
            HttpURLConnection c = (HttpURLConnection) con;
            c.setUseCaches(false);
            c.setDefaultUseCaches(false);
            c.setInstanceFollowRedirects(false);
        }

        con.setDoInput(read);
        con.setDoOutput(write);
        if (netMonitor != null) {
            NetworkRequestObject nr = new NetworkRequestObject();
            if (nr != null) {
                nr.setUrl(url);
            }
            netMonitor.addRequest(con, nr);
        }
        return con;
    }

    /**
     * @inheritDoc
     */
    public void setHeader(Object connection, String key, String val) {
        HttpURLConnection con = ((HttpURLConnection) connection);
        String url = con.getURL().toString();
        //a patch go get a readable login page for facebook
        if (key.equals("User-Agent") && url.contains("facebook.com")) {
            //blackberry user-agent gets an html without javascript.

            //con.setRequestProperty("User-Agent", "Profile/MIDP-2.1 Configuration/CLDC-1.1");        

            con.setRequestProperty("User-Agent", "Mozilla/5.0 (BlackBerry; U; BlackBerry 9860; en-GB) AppleWebKit/534.11+ (KHTML, like Gecko) Version/7.0.0.296 Mobile Safari/534.11+");
        } else {
            con.setRequestProperty(key, val);
        }
        updateRequestHeaders(con);
    }

    private void updateRequestHeaders(HttpURLConnection con) {
        if (netMonitor != null) {
            NetworkRequestObject nr = netMonitor.getByConnection(con);
            if (nr != null) {
                String requestHeaders = "";
                Map<String, List<String>> props = con.getRequestProperties();
                for (String header : props.keySet()) {
                    requestHeaders += header + "=" + props.get(header) + "\n";
                }
                nr.setHeaders(requestHeaders);
            }
        }
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
        if (netMonitor != null) {
            final NetworkRequestObject nr = netMonitor.getByConnection((URLConnection) connection);
            if (nr != null) {
                nr.setRequestBody("");
                HttpURLConnection con = (HttpURLConnection) connection;
                OutputStream o = new BufferedOutputStream(con.getOutputStream()) {
                    public void write(byte b[], int off, int len) throws IOException {
                        super.write(b, off, len);
                        nr.setRequestBody(nr.getRequestBody() + new String(b, off, len));
                    }
                };
                return o;
            }
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
        if (netMonitor != null) {
            final NetworkRequestObject nr = netMonitor.getByConnection((URLConnection) connection);
            if (nr != null) {
                HttpURLConnection con = (HttpURLConnection) connection;
                String headers = "";
                Map<String, List<String>> map = con.getHeaderFields();
                for (String header : map.keySet()) {
                    headers += header + "=" + map.get(header) + "\n";
                }
                nr.setResponseHeaders(headers);
                nr.setResponseBody("");
                InputStream i = new BufferedInputStream(con.getInputStream()) {
                    public synchronized int read(byte b[], int off, int len)
                            throws IOException {
                        int s = super.read(b, off, len);
                        if (s > -1) {
                            nr.setResponseBody(nr.getResponseBody() + new String(b, off, len));
                        }
                        return s;
                    }
                };
                return i;
            }
        }
        return new BufferedInputStream(((URLConnection) connection).getInputStream());
    }

    /**
     * @inheritDoc
     */
    public void setHttpMethod(Object connection, String method) throws IOException {
        ((HttpURLConnection) connection).setRequestMethod(method);
    }

    /**
     * @inheritDoc
     */
    public void setPostRequest(Object connection, boolean p) {
        try {
            String mtd = "GET";
            if (p) {
                mtd = "POST";
            }
            ((HttpURLConnection) connection).setRequestMethod(mtd);

            if (netMonitor != null) {
                NetworkRequestObject nr = netMonitor.getByConnection((URLConnection) connection);
                if (nr != null) {
                    nr.setMethod(mtd);
                }
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
        int code = ((HttpURLConnection) connection).getResponseCode();
        if (netMonitor != null) {
            NetworkRequestObject nr = netMonitor.getByConnection((URLConnection) connection);
            if (nr != null) {
                nr.setResponseCode("" + code);
            }
        }
        return code;
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
        int contentLength = ((HttpURLConnection) connection).getContentLength();
        if (netMonitor != null) {
            NetworkRequestObject nr = netMonitor.getByConnection((URLConnection) connection);
            if (nr != null) {
                nr.setContentLength("" + contentLength);
            }
        }
        return contentLength;
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
    public String[] getHeaderFieldNames(Object connection) throws IOException {
        Set<String> s = ((HttpURLConnection) connection).getHeaderFields().keySet();
        String[] resp = new String[s.size()];
        s.toArray(resp);
        return resp;
    }

    /**
     * @inheritDoc
     */
    public String[] getHeaderFields(String name, Object connection) throws IOException {
        HttpURLConnection c = (HttpURLConnection) connection;
        List r = new ArrayList();
        List<String> headers = c.getHeaderFields().get(name);
        if (headers != null && headers.size() > 0) {
            Vector v = new Vector<String>();
            v.addAll(headers);
            Collections.reverse(v);
            String[] s = new String[v.size()];
            v.toArray(s);
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
        return new File(unfile(directory)).list();
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
        new File(unfile(directory)).mkdirs();
    }

    private String unfile(String file) {
        if (file.startsWith("file://")) {
            return file.substring(7);
        }
        return file;
    }

    /**
     * @inheritDoc
     */
    public void deleteFile(String file) {
        new File(unfile(file)).delete();
    }

    /**
     * @inheritDoc
     */
    public boolean isHidden(String file) {
        return new File(unfile(file)).isHidden();
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
        return new File(unfile(file)).length();
    }

    /**
     * @inheritDoc
     */
    public boolean isDirectory(String file) {
        return new File(unfile(file)).isDirectory();
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
        return new FileOutputStream(unfile(file));
    }

    /**
     * @inheritDoc
     */
    public InputStream openFileInputStream(String file) throws IOException {
        return new FileInputStream(unfile(file));
    }

    /**
     * @inheritDoc
     */
    public boolean exists(String file) {
        return new File(unfile(file)).exists();
    }

    /**
     * @inheritDoc
     */
    public void rename(String file, String newName) {
        new File(unfile(file)).renameTo(new File(new File(file).getParentFile(), newName));
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

    @Override
    public void sendSMS(final String phoneNumber, final String message) throws IOException {
        System.out.println("sending sms to " + phoneNumber);
    }

    @Override
    public void dial(String phoneNumber) {
        System.out.println("dialing to " + phoneNumber);
    }

    @Override
    public String[] getAllContacts(boolean withNumbers) {
        return new String[]{"1", "2", "3", "4", "5"};
    }

    @Override
    public Contact getContactById(String id) {
        Contact contact = new Contact();
        contact.setId(id);
        if (id.equals("1")) {
            contact.setDisplayName("Chen Fishbein");
            contact.setFirstName("Chen");
            contact.setFamilyName("Fishbein");

            Hashtable phones = new Hashtable();
            phones.put("mobile", "+111111");
            phones.put("home", "+222222");
            contact.setPhoneNumbers(phones);
            Hashtable emails = new Hashtable();
            emails.put("work", "chen@codenameone.com");
            contact.setEmails(emails);

            Hashtable addresses = new Hashtable();
            Address addr = new Address();
            addr.setCountry("IL");
            addr.setStreetAddress("Sapir 20");
            addresses.put("home", addr);
            contact.setAddresses(addresses);
        } else if (id.equals("2")) {
            contact.setDisplayName("Shai Almog");
            contact.setFirstName("Shai");
            contact.setFamilyName("Almog");

            Hashtable phones = new Hashtable();
            phones.put("mobile", "+111111");
            phones.put("home", "+222222");
            contact.setPhoneNumbers(phones);
            Hashtable emails = new Hashtable();
            emails.put("work", "shai@codenameone.com");
            contact.setEmails(emails);

            Hashtable addresses = new Hashtable();
            Address addr = new Address();
            addr.setCountry("IL");
            addr.setStreetAddress("lev 1");
            addresses.put("home", addr);
            contact.setAddresses(addresses);
        }
        if (id.equals("3")) {

            contact.setDisplayName("Eric Cartman");
            contact.setFirstName("Eric");
            contact.setFamilyName("Cartman");


            Hashtable phones = new Hashtable();
            phones.put("mobile", "+111111");
            phones.put("home", "+222222");
            contact.setPhoneNumbers(phones);
            Hashtable emails = new Hashtable();
            emails.put("work", "Eric.Cartman@codenameone.com");
            contact.setEmails(emails);

            Hashtable addresses = new Hashtable();
            Address addr = new Address();
            addr.setCountry("US");
            addr.setStreetAddress("South Park");
            addresses.put("home", addr);
            contact.setAddresses(addresses);
        }
        if (id.equals("4")) {

            contact.setDisplayName("Kyle Broflovski");
            contact.setFirstName("Kyle");
            contact.setFamilyName("Broflovski");

            Hashtable phones = new Hashtable();
            phones.put("mobile", "+111111");
            phones.put("home", "+222222");
            contact.setPhoneNumbers(phones);
            Hashtable emails = new Hashtable();
            emails.put("work", "Kyle.Broflovski@codenameone.com");
            contact.setEmails(emails);

            Hashtable addresses = new Hashtable();
            Address addr = new Address();
            addr.setCountry("US");
            addr.setStreetAddress("South Park");
            addresses.put("home", addr);
            contact.setAddresses(addresses);
        } else if (id.equals("5")) {

            contact.setDisplayName("Kenny McCormick");
            contact.setFirstName("Kenny");
            contact.setFamilyName("McCormick");


            Hashtable phones = new Hashtable();
            phones.put("mobile", "+111111");
            phones.put("home", "+222222");
            contact.setPhoneNumbers(phones);
            Hashtable emails = new Hashtable();
            emails.put("work", "Kenny.McCormick@codenameone.com");
            contact.setEmails(emails);

            Hashtable addresses = new Hashtable();
            Address addr = new Address();
            addr.setCountry("US");
            addr.setStreetAddress("South Park");
            addresses.put("home", addr);
            contact.setAddresses(addresses);
        }

        return contact;
    }

    @Override
    public boolean shouldAutoDetectAccessPoint() {
        return false;
    }

    /**
     * Indicates whether looking up an access point is supported by this device
     *
     * @return true if access point lookup is supported
     */
    public boolean isAPSupported() {
        return true;
    }

    /**
     * Returns the ids of the access points available if supported
     *
     * @return ids of access points
     */
    public String[] getAPIds() {
        return new String[]{"11", "22"};
    }

    /**
     * Returns the type of the access point
     *
     * @param id access point id
     * @return one of the supported access point types from network manager
     */
    public int getAPType(String id) {
        if (id.indexOf("11") > -1) {
            return NetworkManager.ACCESS_POINT_TYPE_WLAN;
        } else if (id.indexOf("22") > -1) {
            return NetworkManager.ACCESS_POINT_TYPE_NETWORK3G;
        }
        return NetworkManager.ACCESS_POINT_TYPE_UNKNOWN;
    }

    /**
     * Returns the user displayable name for the given access point
     *
     * @param id the id of the access point
     * @return the name of the access point
     */
    public String getAPName(String id) {
        if (id.indexOf("11") > -1) {
            return "wifi";
        } else if (id.indexOf("22") > -1) {
            return "3g";
        }
        return null;
    }

    //private String currentAp;
    /**
     * Returns the id of the current access point
     *
     * @return id of the current access point
     */
    public String getCurrentAccessPoint() {
        return super.getCurrentAccessPoint();
        //return currentAp;
    }

    /**
     * Returns the id of the current access point
     *
     * @param id id of the current access point
     */
    public void setCurrentAccessPoint(String id) {
        //this.currentAp = id;
        super.setCurrentAccessPoint(id);
    }

    /**
     * Captures a photo and notifies with the image data when available
     *
     * @param response callback for the resulting image
     */
    public void capturePhoto(final com.codename1.ui.events.ActionListener response) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFileChooser fc = createImagesFileChooser();

                com.codename1.ui.events.ActionEvent result;
                if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    File selected = fc.getSelectedFile();
                    result = new com.codename1.ui.events.ActionEvent(selected.getAbsolutePath());
                } else {
                    result = null;
                }
                final com.codename1.ui.events.ActionEvent finalResult = result;
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        response.actionPerformed(finalResult);
                    }
                });
            }
        });
    }

    class CodenameOneMediaPlayer implements Media {

        private Runnable onCompletion;
        private javafx.scene.media.MediaPlayer player;
//        private MediaPlayer player;
        private boolean realized = false;
        private boolean isVideo;
        private javafx.embed.swing.JFXPanel videoPanel;
        private JFrame frm;
        private boolean playing = false;

        public CodenameOneMediaPlayer(String uri, boolean isVideo, JFrame f, javafx.embed.swing.JFXPanel fx, Runnable onCompletion) throws IOException {
            this.onCompletion = onCompletion;
            this.isVideo = isVideo;
            this.frm = f;
            try {
                player = new MediaPlayer(new javafx.scene.media.Media(uri));
                player.setOnEndOfMedia(onCompletion);
                if (isVideo) {
                    videoPanel = fx;
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                throw new RuntimeException(ex);
            }
        }

        public CodenameOneMediaPlayer(InputStream stream, String mimeType, JFrame f, javafx.embed.swing.JFXPanel fx, Runnable onCompletion) throws IOException {
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
                player = new MediaPlayer(new javafx.scene.media.Media(temp.toURI().toString()));
                player.setOnEndOfMedia(onCompletion);
                if (isVideo) {
                    videoPanel = fx;
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                throw new RuntimeException(ex);
            }
        }

        public void cleanup() {
            playing = false;

        }

        public void play() {
            player.play();
            playing = true;
        }

        public void pause() {
            player.stop();
            playing = false;
        }

        public int getTime() {
            return (int) player.getCurrentTime().toSeconds();
        }

        public void setTime(int time) {
            player.seek(new Duration(time * 1000));
        }

        public int getDuration() {
            return (int) player.getStopTime().toSeconds();
        }

        public void setVolume(int vol) {
            player.setVolume(((double) vol / 100d));
        }

        public int getVolume() {
            return (int) player.getVolume() * 100;
        }

        @Override
        public Component getVideoComponent() {
            if (videoPanel != null) {
                final Component[] retVal = new Component[1];
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        retVal[0] = new VideoComponent(frm, videoPanel, player);
                    }
                });
                Display.getInstance().invokeAndBlock(new Runnable() {
                    @Override
                    public void run() {
                        while (retVal[0] == null) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(JavaSEPort.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                });
                return retVal[0];
            }
            System.out.println("Video Playing is not supported on this platform");
            Label l = new Label("Video");
            l.getStyle().setAlignment(Component.CENTER);
            return l;
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
    }
        
    class VideoComponent extends PeerComponent {

        private javafx.embed.swing.JFXPanel vid;
        private JFrame frm;
        private JPanel cnt = new JPanel();
        private MediaView v;
        private boolean init = false;

        public VideoComponent(JFrame frm, final javafx.embed.swing.JFXPanel vid, javafx.scene.media.MediaPlayer player) {
            super(null);
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    cnt.setLayout(new BorderLayout());
                    cnt.add(BorderLayout.CENTER, vid);
                    cnt.setVisible(false);
                }
            });
            
            Group root = new Group();
            v = new MediaView(player);
            root.getChildren().add(v);
            vid.setScene(new Scene(root));

            this.vid = vid;
            this.frm = frm;
        }

        @Override
        protected void initComponent() {
            super.initComponent();
        }

        @Override
        protected void deinitialize() {
            super.deinitialize();
            if(testRecorder != null) {
                testRecorder.dispose();
                testRecorder = null;
            }
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    vid.removeAll();
                    cnt.remove(vid);
                    frm.remove(cnt);
                    frm.repaint();
                }
            });
        }

        protected void setLightweightMode(final boolean l) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {

                    if (!l) {
                        if (!init) {
                            init = true;
                            cnt.setVisible(true);
                            frm.add(cnt, 0);
                            frm.repaint();
                        } else {
                            cnt.setVisible(false);
                        }
                    } else {
                        if (init) {
                            cnt.setVisible(false);
                        }
                    }
                }
            });

        }
        
        

        @Override
        protected com.codename1.ui.geom.Dimension calcPreferredSize() {
            return new com.codename1.ui.geom.Dimension(vid.getWidth(), vid.getHeight());
        }

        @Override
        public void paint(Graphics g) {
            if(init){
                onPositionSizeChange();
            }
        }                
        
        @Override
        protected void onPositionSizeChange() {
            final int x = getAbsoluteX();
            final int y = getAbsoluteY();
            final int w = getWidth();
            final int h = getHeight();
            
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    v.setFitWidth(w * zoomLevel);
                    v.setFitHeight(h * zoomLevel);
                    
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            cnt.setBounds((int) ((x + getScreenCoordinates().x + canvas.x) * zoomLevel),
                                    (int) ((y + getScreenCoordinates().y + canvas.y) * zoomLevel),
                                    (int) (w * zoomLevel),
                                    (int) (h * zoomLevel));
                            cnt.validate();
                        }
                    });
                }
            });

        }
    }

    public InputStream getResourceAsStream(Class cls, String resource) {
        if (baseResourceDir != null) {
            try {
                File f = new File(baseResourceDir, resource);
                if (f.exists()) {
                    return new FileInputStream(f);
                }
            } catch (IOException err) {
                return null;
            }
        }
        return super.getResourceAsStream(cls, resource);
    }

    public void beforeComponentPaint(Component c) {
        if (perfMonitor != null) {
            perfMonitor.beforeComponentPaint(c);
        }
    }

    public void afterComponentPaint(Component c) {
        if (perfMonitor != null) {
            perfMonitor.afterComponentPaint(c);
        }
    }
    private L10NManager l10n;

    /**
     * @inheritDoc
     */
    public L10NManager getLocalizationManager() {
        if (l10n == null) {
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

    @Override
    public Media createMediaRecorder(String path) throws IOException {
        throw new IOException("Not supported on Simulator");
    }

    private com.codename1.ui.util.ImageIO imIO;

    @Override
    public com.codename1.ui.util.ImageIO getImageIO() {
        if (imIO == null) {
            imIO = new com.codename1.ui.util.ImageIO() {
                @Override
                public void save(InputStream image, OutputStream response, String format, int width, int height, float quality) throws IOException {
                    String f = "png";
                    if (format == FORMAT_JPEG) {
                        f = "jpeg";
                    }
                    Image img = Image.createImage(image).scaled(width, height);
                    if (width < 0) {
                        width = img.getWidth();
                    }
                    if (height < 0) {
                        width = img.getHeight();
                    }
                    ImageIO.write(((BufferedImage) img.getImage()), f, response);
                }

                @Override
                protected void saveImage(Image img, OutputStream response, String format, float quality) throws IOException {
                    String f = "png";
                    if (format == FORMAT_JPEG) {
                        f = "jpeg";
                    }
                    ImageIO.write(((BufferedImage) img.getImage()), f, response);
                }

                @Override
                public boolean isFormatSupported(String format) {
                    return format == FORMAT_JPEG || format == FORMAT_PNG;
                }
            };
        }
        return imIO;
    }

    @Override
    public void registerPush(String id, boolean noFallback) {
        Preferences p = Preferences.userNodeForPackage(com.codename1.ui.Component.class);
        String user = p.get("user", null);
        Display d = Display.getInstance();
        if (user == null) {
            JPanel pnl = new JPanel();
            JTextField tf = new JTextField(20);
            pnl.add(new JLabel("E-Mail For Push"));
            pnl.add(tf);
            JOptionPane.showMessageDialog(canvas, pnl, "Email For Push", JOptionPane.PLAIN_MESSAGE);
            user = tf.getText();
            p.put("user", user);
        }
        d.setProperty("built_by_user", user);
        String mainClass = System.getProperty("MainClass");
        if (mainClass != null) {
            mainClass = mainClass.substring(0, mainClass.lastIndexOf('.'));
            d.setProperty("package_name", mainClass);
        }
        super.registerPush(id, noFallback);
    }

    @Override
    public Database openOrCreateDB(String databaseName) throws IOException {
        try {
            // Load the HSQL Database Engine JDBC driver
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
        }
        try {
            // connect to the database.   This will load the db files and start the
            // database if it is not alread running.
            // db_file_name_prefix is used to open or create files that hold the state
            // of the db.
            // It can contain directory names relative to the
            // current working directory
            File dir = new File(getStorageDir() + "/database");
            if (!dir.exists()) {
                dir.mkdir();
            }
            java.sql.Connection conn = DriverManager.getConnection("jdbc:sqlite:"
                    + getStorageDir() + "/database/" + databaseName);

            return new SEDatabase(conn);
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    @Override
    public void deleteDB(String databaseName) throws IOException {
        File f = new File(getStorageDir() + "/database/" + databaseName);
        if (f.exists()) {
            f.delete();
        }
    }

    @Override
    public boolean existsDB(String databaseName) {
        File f = new File(getStorageDir() + "/database/" + databaseName);
        return f.exists();
    }

    @Override
    public void setCommandBehavior(int commandBehavior) {
        //cannot show native menus on the simulator
        if (commandBehavior == Display.COMMAND_BEHAVIOR_NATIVE) {
            if(getPlatformName().equals("win")) {
                commandBehavior = Display.COMMAND_BEHAVIOR_BUTTON_BAR;
            } else {
                if (isTablet() && getPlatformName().equals("and")) {
                    //simulate native ics with the lightweight ics
                    commandBehavior = Display.COMMAND_BEHAVIOR_ICS;
                } else {
                    return;
                }
            }
        }
        super.setCommandBehavior(commandBehavior);
    }

    public boolean isNativeBrowserComponentSupported() {
        return fxExists;
    }

    public PeerComponent createBrowserComponent(final Object parent) {
        java.awt.Container cnt = canvas.getParent();
        while (!(cnt instanceof JFrame)) {
            cnt = cnt.getParent();
            if (cnt == null) {
                return null;
            }
        }
        final java.awt.Container c = cnt;

        final Exception[] err = new Exception[1];
        final javafx.embed.swing.JFXPanel webContainer = new javafx.embed.swing.JFXPanel();
        final SEBrowserComponent[] bc = new SEBrowserComponent[1];

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                StackPane root = new StackPane();
                final WebView webView = new WebView();
                root.getChildren().add(webView);                
                webContainer.setScene(new Scene(root));
                bc[0] = new SEBrowserComponent(JavaSEPort.this, (JFrame) c, webContainer, webView, (BrowserComponent) parent);
            }
        });
        Display.getInstance().invokeAndBlock(new Runnable() {
            @Override
            public void run() {
                while (bc[0] == null && err[0] == null) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ex) {
                    }
                }
            }
        });
        return bc[0];
    }

    public void setBrowserProperty(PeerComponent browserPeer, String key, Object value) {
        ((SEBrowserComponent) browserPeer).setProperty(key, value);
    }

    public String getBrowserTitle(PeerComponent browserPeer) {
        return ((SEBrowserComponent) browserPeer).getTitle();
    }

    public String getBrowserURL(PeerComponent browserPeer) {
        return ((SEBrowserComponent) browserPeer).getURL();
    }

    public void browserExecute(PeerComponent browserPeer, String javaScript) {
        ((SEBrowserComponent) browserPeer).execute(javaScript);
    }
    
    @Override
    public String browserExecuteAndReturnString(PeerComponent browserPeer, String javaScript) {
        return ((SEBrowserComponent) browserPeer).executeAndReturnString(javaScript);
    }
    

    public void setBrowserURL(final PeerComponent browserPeer, final String url) {
        if (url.startsWith("jar:")) {
            super.setBrowserURL(browserPeer, url);
            return;
        }
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                ((SEBrowserComponent) browserPeer).setURL(url);
            }
        });
    }

    public void browserStop(final PeerComponent browserPeer) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                ((SEBrowserComponent) browserPeer).stop();
            }
        });
    }

    /**
     * Reload the current page
     *
     * @param browserPeer browser instance
     */
    public void browserReload(final PeerComponent browserPeer) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                ((SEBrowserComponent) browserPeer).reload();
            }
        });
    }

    /**
     * Indicates whether back is currently available
     *
     * @param browserPeer browser instance
     * @return true if back should work
     */
    public boolean browserHasBack(PeerComponent browserPeer) {
        return ((SEBrowserComponent) browserPeer).hasBack();
    }

    public boolean browserHasForward(PeerComponent browserPeer) {
        return ((SEBrowserComponent) browserPeer).hasForward();
    }

    public void browserBack(final PeerComponent browserPeer) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                ((SEBrowserComponent) browserPeer).back();
            }
        });
    }

    public void browserForward(final PeerComponent browserPeer) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                ((SEBrowserComponent) browserPeer).forward();
            }
        });
    }

    public void browserClearHistory(final PeerComponent browserPeer) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                ((SEBrowserComponent) browserPeer).clearHistory();
            }
        });
    }

    public void setBrowserPage(final PeerComponent browserPeer, final String html, final String baseUrl) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                ((SEBrowserComponent) browserPeer).setPage(html, baseUrl);
            }
        });
    }

    public void browserExposeInJavaScript(final PeerComponent browserPeer, final Object o, final String name) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                ((SEBrowserComponent) browserPeer).exposeInJavaScript(o, name);
            }
        });
    }

    private static JFileChooser createImagesFileChooser() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Camera simulation - pick an image");
        fc.addChoosableFileFilter(new ImageFilter());
        fc.setAcceptAllFileFilterUsed(false);
        fc.setAccessory(new ImagePreview(fc));
        return fc;
    }

    public static class ImageFilter extends FileFilter {

        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            }
            String extension = getExtension(f);
            if (extension != null) {
                if (extension.equals("png")
                        || extension.equals("jpg")
                        || extension.equals("jpeg")) {
                    return true;
                } else {
                    return false;
                }
            }

            return false;
        }

        public String getDescription() {
            return "Images";
        }

        private String getExtension(File f) {
            String ext = null;
            String s = f.getName();
            int i = s.lastIndexOf('.');

            if (i > 0 && i < s.length() - 1) {
                ext = s.substring(i + 1).toLowerCase();
            }
            return ext;
        }
    }

    public static class ImagePreview extends JComponent
            implements PropertyChangeListener {

        ImageIcon thumbnail = null;
        File file = null;

        public ImagePreview(JFileChooser fc) {
            setPreferredSize(new Dimension(100, 50));
            fc.addPropertyChangeListener(this);
        }

        public void loadImage() {
            if (file == null) {
                thumbnail = null;
                return;
            }

            //Don't use createImageIcon (which is a wrapper for getResource)
            //because the image we're trying to load is probably not one
            //of this program's own resources.
            ImageIcon tmpIcon = new ImageIcon(file.getPath());
            if (tmpIcon != null) {
                if (tmpIcon.getIconWidth() > 90) {
                    thumbnail = new ImageIcon(tmpIcon.getImage().
                            getScaledInstance(90, -1,
                            java.awt.Image.SCALE_DEFAULT));
                } else { //no need to miniaturize
                    thumbnail = tmpIcon;
                }
            }
        }

        public void propertyChange(PropertyChangeEvent e) {
            boolean update = false;
            String prop = e.getPropertyName();

            //If the directory changed, don't show an image.
            if (JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(prop)) {
                file = null;
                update = true;

                //If a file became selected, find out which one.
            } else if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(prop)) {
                file = (File) e.getNewValue();
                update = true;
            }

            //Update the preview accordingly.
            if (update) {
                thumbnail = null;
                if (isShowing()) {
                    loadImage();
                    repaint();
                }
            }
        }

        protected void paintComponent(java.awt.Graphics g) {
            if (thumbnail == null) {
                loadImage();
            }
            if (thumbnail != null) {
                int x = getWidth() / 2 - thumbnail.getIconWidth() / 2;
                int y = getHeight() / 2 - thumbnail.getIconHeight() / 2;

                if (y < 0) {
                    y = 0;
                }

                if (x < 5) {
                    x = 5;
                }
                thumbnail.paintIcon(this, g, x, y);
            }
        }
    }
}
