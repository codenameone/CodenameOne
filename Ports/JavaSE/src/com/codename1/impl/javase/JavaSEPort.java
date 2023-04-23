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

import com.codename1.background.BackgroundFetch;
import com.codename1.capture.VideoCaptureConstraints;
import com.codename1.charts.util.ColorUtil;
import com.codename1.components.SpanLabel;
import com.codename1.components.ToastBar;
import com.codename1.contacts.Address;
import com.codename1.contacts.Contact;
import com.codename1.db.Database;
import com.codename1.impl.javase.simulator.*;
import com.codename1.impl.javase.util.MavenUtils;
import com.codename1.impl.javase.util.SwingUtils;
import com.codename1.messaging.Message;
import com.codename1.payment.PromotionalOffer;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.impl.CodenameOneImplementation;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.EventDispatcher;
import com.codename1.ui.util.Resources;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FontFormatException;
import javax.swing.JFrame;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.FilenameFilter;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import com.codename1.io.Properties;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import com.codename1.io.BufferedInputStream;
import com.codename1.io.BufferedOutputStream;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.io.NetworkManager;
import com.codename1.io.Storage;
import com.codename1.io.Util;
import com.codename1.l10n.L10NManager;
import com.codename1.location.Location;
import com.codename1.location.LocationManager;
import com.codename1.media.AbstractMedia;
import com.codename1.media.AudioBuffer;
import com.codename1.media.Media;
import com.codename1.media.MediaManager;
import com.codename1.media.MediaRecorderBuilder;
import com.codename1.notifications.LocalNotification;
import com.codename1.payment.Product;
import com.codename1.payment.Purchase;
import com.codename1.payment.Receipt;
import com.codename1.ui.Accessor;
import com.codename1.ui.BrowserWindow;
import com.codename1.ui.CN;
import com.codename1.ui.ComponentSelector;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.FontImage;
import com.codename1.ui.PeerComponent;
import com.codename1.ui.Sheet;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextSelection;
import com.codename1.ui.Transform;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.util.UITimer;
import com.codename1.util.AsyncResource;
import com.codename1.util.Callback;
import com.jhlabs.image.GaussianFilter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.sql.DriverManager;
import java.text.AttributedString;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.DefaultCaret;
import javax.swing.text.JTextComponent;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.jhlabs.image.ShadowFilter;
import org.sqlite.SQLiteConfig;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static com.codename1.impl.javase.util.MavenUtils.isRunningInMaven;

/**
 * An implementation of Codename One based on Java SE
 *
 * @author Shai Almog
 */
public class JavaSEPort extends CodenameOneImplementation {

    
    private static final int ICON_SIZE=24;
    public final static boolean IS_MAC;
    private static boolean isIOS;
    public static boolean blockNativeBrowser;
    private static final boolean isWindows;
    private static String fontFaceSystem;
    private Boolean darkMode;

    /**
     * @return the fullScreen
     */
    public static boolean isFullScreen() {
        return fullScreen;
    }

    /**
     * @param aFullScreen the fullScreen to set
     */
    public static void setFullScreen(boolean aFullScreen) {
        fullScreen = aFullScreen;
    }

    private JFrame findTopFrame() {
        java.awt.Component c = canvas;
        return (JFrame)canvas.getTopLevelAncestor();
        /*
        if (c == null) return null;
        while (c.getParent() != null) {
            c = c.getParent();
            if (c instanceof JFrame) {
                return (JFrame)c;
            }
        }
        return null;
        */
    }

    @Override
    public boolean isFullScreenSupported() {
        Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
        boolean desktopSkin = pref.getBoolean("desktopSkin", false);
        if (isSimulator() && !desktopSkin) {
            return false;
        }
        return true;
    }
    
    private java.awt.Rectangle restoreWindowBounds;
    
    @Override
    public boolean requestFullScreen() {
        if (!isFullScreenSupported()) return false;
        if (!fullScreen) {
            if (!EventQueue.isDispatchThread()) {
                try {
                    EventQueue.invokeAndWait(new Runnable() {
                        public void run() {
                            requestFullScreen();
                            
                        }
                    });
                } catch (InterruptedException ex) {
                    Logger.getLogger(JavaSEPort.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InvocationTargetException ex) {
                    Logger.getLogger(JavaSEPort.class.getName()).log(Level.SEVERE, null, ex);
                }
                return fullScreen;
            }
            GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            JFrame frm = findTopFrame();
            
            if (frm == null) {
                return false;
            }
            
            if(gd.isFullScreenSupported()) {
                restoreWindowBounds = frm.getBounds();
                frm.dispose();
                frm.setUndecorated(true);
                frm.setResizable(false);
                gd.setFullScreenWindow(frm);
            }
            fullScreen = true;
        }
        return fullScreen;
    }

    @Override
    public boolean exitFullScreen() {
        if (!isFullScreenSupported()) return false;
        if (fullScreen) {
            if (!EventQueue.isDispatchThread()) {
                try {
                    EventQueue.invokeAndWait(new Runnable() {
                        public void run() {
                            exitFullScreen();
                            
                        }
                    });
                } catch (InterruptedException ex) {
                    Logger.getLogger(JavaSEPort.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InvocationTargetException ex) {
                    Logger.getLogger(JavaSEPort.class.getName()).log(Level.SEVERE, null, ex);
                }
                return !fullScreen;
            }
            GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            JFrame frm = findTopFrame();
            if (frm == null) {
                return false;
            }
            if(gd.isFullScreenSupported()) {
                frm.dispose();
                frm.setUndecorated(false);
                frm.setResizable(true);
                gd.setFullScreenWindow(null);
                if (restoreWindowBounds != null) {
                    frm.setBounds(restoreWindowBounds);
                } else {
                    frm.setBounds(new java.awt.Rectangle(0, 0, 800, 600));
                }
                frm.setVisible(true);
            }
            fullScreen = false;
        }
        return !fullScreen;
    }

    @Override
    public boolean isInFullScreenMode() {
        return fullScreen;
    }

    @Override
    public Boolean isDarkMode() {
        return darkMode;
    }

    
    
    public boolean takingScreenshot;
    private static boolean fullScreen;
    public float screenshotActualZoomLevel;
    private InputEvent lastInputEvent;
    public static double retinaScale = 1.0;
    
    static JMenuItem pause;
    
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

    public static boolean isRetina() {
        boolean isRetina = false;
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
                
                if (scaleX >= 2 && scaleY >= 2) {
                    isRetina = true;
                }
            } else {

                Field field = graphicsDevice.getClass().getDeclaredField("scale");
                if (field != null) {
                    field.setAccessible(true);
                    Object scale = field.get(graphicsDevice);
                    if (scale instanceof Integer && ((Integer) scale).intValue() >= 2) {
                        isRetina = true;
                    }
                }
            }
        } catch (Throwable e) {
            //e.printStackTrace();
        }
        return isRetina;
    }
    
    public static double calcRetinaScale() {
        
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
    
    public static double getRetinaScale() {
        return retinaScale;
    }
    
    /**
     * When set to true pointer hover events will be called for mouse move events
     */
    private static boolean invokePointerHover;

    private static String defaultCodenameOneComProtocol = "https";
    
    static {
        String n = System.getProperty("os.name");
        if (n != null && n.startsWith("Mac")) {
            IS_MAC = true;
        } else {
            IS_MAC = false;
        }
        isWindows = File.separatorChar == '\\';        
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        
        if(isWindows) {
            fontFaceSystem = "ArialUnicodeMS";
        } else {
            fontFaceSystem = "Arial";
        }
    }

    /**
     * When set to true pointer hover events will be called for mouse move events
     * @return the invokePointerHover
     */
    public static boolean isInvokePointerHover() {
        return invokePointerHover;
    }

    /**
     * When set to true pointer hover events will be called for mouse move events
     * @param aInvokePointerHover the invokePointerHover to set
     */
    public static void setInvokePointerHover(boolean aInvokePointerHover) {
        invokePointerHover = aInvokePointerHover;
    }
    
    private boolean minimized;
    //private javafx.embed.swing.JFXPanel mediaContainer;

    private static File baseResourceDir;
    private static final String DEFAULT_SKIN = "/iPhoneX.skin";
    private static final String DEFAULT_SKINS = DEFAULT_SKIN+";";
    private static String appHomeDir = ".cn1";
    
    /**
     * Allowed video extensions for the gallery.
     */
    private String[] videoExtensions = new String[] {
        "mp4", "h264", "3pg", "mov", "scmov", "gbmov",
        "f4v", "m2ts", "mts", "ts", "wmv", "vob", "m4v", 
        "flv", "mod", "mkv", "avi", "mpg", "3gp"
    };
    
    /**
     * Allowed image extensions for the gallery.
     */
    private String[] imageExtensions = new String[] {"png", "jpg", "jpeg"};
    
    private boolean menuDisplayed = false;
    
    private static boolean android6PermissionsFlag = false;
    
    private static boolean waitForPermission = false;
    
    private static Map android6Permissions = new HashMap();
    
    
    /**
     * Allows the simulator to use the native filesystem completely rather than the "fake" filesystem
     * used, this is important when running a real application rather than just a simulator skin
     * @return the exposeFilesystem
     */
    public static boolean isExposeFilesystem() {
        return exposeFilesystem;
    }

    /**
     * Allows the simulator to use the native filesystem completely rather than the "fake" filesystem
     * used, this is important when running a real application rather than just a simulator skin
     * @param aExposeFilesystem the exposeFilesystem to set
     */
    public static void setExposeFilesystem(boolean aExposeFilesystem) {
        exposeFilesystem = aExposeFilesystem;
    }

    /**
     * @return the designMode
     */
    public static boolean isDesignMode() {
        return designMode;
    }

    /**
     * @param aDesignMode the designMode to set
     */
    public static void setDesignMode(boolean aDesignMode) {
        designMode = aDesignMode;
    }

    public int getDeviceDensity() {
        if(defaultPixelMilliRatio != null) {
            /*
            if(Math.round(defaultPixelMilliRatio.doubleValue()) == 10) {
                return Display.DENSITY_MEDIUM;
            }
            if(Math.round(defaultPixelMilliRatio.doubleValue()) == 20) {
                return Display.DENSITY_VERY_HIGH;
            }
            System.out.println("Ratio "+defaultPixelMilliRatio.doubleValue());
            */
            if (retinaScale > 1.5) {
                return Display.DENSITY_VERY_HIGH;
            } else {
                return Display.DENSITY_MEDIUM;
            }
        }
        return super.getDeviceDensity();
    }
    
    /**
     * @return the defaultPixelMilliRatio
     */
    public static Double getDefaultPixelMilliRatio() {
        return defaultPixelMilliRatio;
    }

    /**
     * @param aDefaultPixelMilliRatio the defaultPixelMilliRatio to set
     */
    public static void setDefaultPixelMilliRatio(Double aDefaultPixelMilliRatio) {
        defaultPixelMilliRatio = aDefaultPixelMilliRatio;
    }

    /**
     * @return the appHomeDir
     */
    public static String getAppHomeDir() {
        return appHomeDir;
    }

    /**
     * @param aAppHomeDir the appHomeDir to set
     */
    public static void setAppHomeDir(String aAppHomeDir) {
        appHomeDir = aAppHomeDir;
    }
    protected TestRecorder testRecorder;
    private Hashtable contacts;
    private static boolean designMode;
    
    /**
     * @return the showEDTWarnings
     */
    public static boolean isShowEDTWarnings() {
        return showEDTWarnings;
    }

    @Override
    public void setPlatformHint(String key, String value) {
        if (key.equalsIgnoreCase("platformHint.showEDTWarnings")) {
            setShowEDTWarnings("true".equalsIgnoreCase(value));
        }
        super.setPlatformHint(key, value);

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
    static {
        retinaScale = calcRetinaScale();
        if (System.getProperty("cn1.retinaScale", null) != null) {
            try {
                retinaScale = Double.parseDouble(System.getProperty("cn1.retinaScale"));
            } catch (Throwable t){}
        } else if (System.getenv("CN1_RETINA_SCALE") != null) {
            try {
                retinaScale = Double.parseDouble(System.getenv("CN1_RETINA_SCALE"));
            } catch (Throwable t) {}
        }
        System.out.println("Retina Scale: "+retinaScale);
    
        if (retinaScale > 1.5) {
            medianFontSize = (int)(medianFontSize * retinaScale);
            smallFontSize = (int)(smallFontSize * retinaScale);
            largeFontSize = (int)(largeFontSize * retinaScale);
            
        }
    }
    private static String fontFaceProportional = "SansSerif";
    private static String fontFaceMonospace = "Monospaced";
    private static boolean alwaysOnTop = false;
    private static boolean useNativeInput = true;
    private static boolean simulateAndroidKeyboard = false;
    private static boolean scrollableSkin = false;
    protected JScrollBar hSelector = new JScrollBar(Scrollbar.HORIZONTAL);
    protected JScrollBar vSelector = new JScrollBar(Scrollbar.VERTICAL);
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
    public float zoomLevel = 1;
    private File storageDir;
    // skin related variables
    private boolean portrait = true;
    private BufferedImage portraitSkin;
    private BufferedImage landscapeSkin;
    private boolean roundedSkin;
    private Rectangle safeAreaPortrait = null;
    private Rectangle safeAreaLandscape = null;
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
    private ComponentTreeInspector componentTreeInspector;
    private static PerformanceMonitor perfMonitor;
    static LocationSimulation locSimulation;
    static PushSimulator pushSimulation;
    private static boolean blockMonitors;
    private static boolean useAppFrame = Boolean.getBoolean("cn1.simulator.useAppFrame");
    static {
        try {
            if (useAppFrame) {
                // If  the app frame is enabled in System properties, it can be disabled
                // by the user preferences.
                // If the system property is false, however, then it should not be overridden
                // by the preference. The app frame must be DOUBLE activated - in system property
                // and preferences to be active to prevent it from accendentally being enabled
                // in other contexts, like unit tests or desktop app distributions.
                Preferences prefs = Preferences.userNodeForPackage(JavaSEPort.class);
                useAppFrame = prefs.getBoolean("cn1.simulator.useAppFrame", useAppFrame);
            }

        } catch (Exception ex){}
    }
    protected static boolean fxExists = false;
    private JFrame window;
    // Application frame used for simulator
    private AppFrame appFrame;
    private long lastIdleTime;
    private static boolean showEDTWarnings = true;
    private static boolean showEDTViolationStacks = false;
    private boolean inInit;
    private boolean showMenu = true;
    private static Double defaultPixelMilliRatio;
    private Double pixelMilliRatio = defaultPixelMilliRatio;
    private boolean manualPurchaseSupported;
    private boolean managedPurchaseSupported;
    private boolean subscriptionSupported;
    private boolean refundSupported;
    private int timeout = -1;
    private JLabel widthLabel;
    private JLabel heightLabel;

    private boolean includeHeaderInScreenshot = true;
    private boolean includeSkinInScreenshot = false;

    private boolean slowConnectionMode;
    private boolean disconnectedMode;

    private static boolean exposeFilesystem;
    private boolean scrollWheeling;
    
    private JComponent textCmp;

    private java.util.Timer backgroundFetchTimer;
    
    
    private void startBackgroundFetchService() {
        if (isBackgroundFetchSupported()) {
            checkIosBackgroundFetch();
            stopBackgroundFetchService();
            if (getPreferredBackgroundFetchInterval() > 0) {
                backgroundFetchTimer = new java.util.Timer();

                TimerTask tt = new TimerTask() {

                    @Override
                    public void run() {
                        performBackgroundFetch();
                    }

                };
                backgroundFetchTimer.schedule(tt, getPreferredBackgroundFetchInterval() * 1000, getPreferredBackgroundFetchInterval() * 1000);
            }
        }
    }
    
    private void stopBackgroundFetchService() {
        if (isBackgroundFetchSupported()) {
            if (backgroundFetchTimer != null) {
                backgroundFetchTimer.cancel();
                backgroundFetchTimer = null;
            }
        }
    }

    @Override
    public boolean isBackgroundFetchSupported() {
        return Display.getInstance().isSimulator();
    }

    private boolean backgroundFetchInitialized;
    
    @Override
    public void setPreferredBackgroundFetchInterval(int seconds) {
        if (isBackgroundFetchSupported()) {
            int oldInterval = getPreferredBackgroundFetchInterval();
            super.setPreferredBackgroundFetchInterval(seconds);
            if (!backgroundFetchInitialized || oldInterval != seconds) {
                backgroundFetchInitialized = true;
                startBackgroundFetchService();
            }
        }
    }
    
    
    
    private static void performBackgroundFetch() {
        if (Display.getInstance().isMinimized()) {
            // By definition, background fetch should only occur if the app is minimized.
            // This keeps it consistent with the iOS implementation that doesn't have a 
            // choice
            
            final Object lifecycle = Executor.getApp();
            final boolean completed[] = new boolean[1];
            final java.util.Timer t = new java.util.Timer();
            final java.util.TimerTask overtimeChecker = new TimerTask() {
                public void run() {
                    if (!completed[0]) {
                        com.codename1.io.Log.p("WARNING: performBackgroundFetch() was called over 30 seconds ago and has not called its completion callback.  This may cause problems on iOS devices.  performBackgroundFetch() must complete in under 30 seconds and call the completion callback when done.");
                    }
                }
            };

            if (lifecycle instanceof BackgroundFetch) {
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        // In callSerially
                        t.schedule(overtimeChecker, 30*60*1000);
                        
                        ((BackgroundFetch)lifecycle).performBackgroundFetch(System.currentTimeMillis()+25*60*1000, new Callback<Boolean>() {

                            @Override
                            public void onSucess(Boolean value) {
                                // On JavaSE the OS doesn't care whether it worked or not
                                // So we'll just consume this.
                                completed[0] = true;
                            }

                            @Override
                            public void onError(Object sender, Throwable err, int errorCode, String errorMessage) {
                                completed[0] = false;
                                com.codename1.io.Log.e(err);
                            }

                        });
                    }
                });
                
            }
        }
    }

    private static long getRepeatPeriod(int repeat) {
        switch (repeat) {
            case LocalNotification.REPEAT_DAY:
                return 24 * 60 * 60 * 1000L;
            case LocalNotification.REPEAT_HOUR:
                return 60 * 60 * 1000L;
            case LocalNotification.REPEAT_MINUTE:
                return 60 * 1000L;
            case LocalNotification.REPEAT_WEEK:
                return 7 * 24 * 60 * 60 * 1000L;
            default:
                return 0L;
        }
    }
    
    private Map<String,TimerTask> localNotifications = new HashMap<String,TimerTask>();
    private java.util.Timer localNotificationsTimer;
    
    @Override
    public void scheduleLocalNotification(final LocalNotification notif, long firstTime, int repeat) {
        if (isSimulator()) {
            if (localNotificationsTimer == null) {
                localNotificationsTimer = new java.util.Timer();
            }
            TimerTask task = new TimerTask() {
                public void run() {
                    if (!SystemTray.isSupported()) {
                        System.out.println("Local notification not supported on this OS!!!");
                        return;
                    }
                    if (isMinimized()) {
                        SystemTray sysTray = SystemTray.getSystemTray();
                        TrayIcon tray = new TrayIcon(Toolkit.getDefaultToolkit().getImage("/CodenameOne_Small.png"));
                        tray.setImageAutoSize(true);
                        tray.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                Display.getInstance().callSerially(new Runnable() {
                                    public void run() {
                                        Executor.startApp();
                                        minimized = false;
                                    }
                                });
                                canvas.setEnabled(true);
                                pause.setText("Pause App");
                            }
                        });
                        try {
                            sysTray.add(tray);
                            tray.displayMessage(notif.getAlertTitle(), notif.getAlertBody(), TrayIcon.MessageType.INFO);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            };
            if (localNotifications.containsKey(notif.getId())) {
                TimerTask old = localNotifications.get(notif.getId());
                old.cancel();
            }
            localNotifications.put(notif.getId(), task);
            if (repeat == LocalNotification.REPEAT_NONE) {
                localNotificationsTimer.schedule(task, new Date(firstTime));
            } else {
                localNotificationsTimer.schedule(task, new Date(firstTime), getRepeatPeriod(repeat));
            }
        }
    }

    @Override
    public void cancelLocalNotification(String notificationId) {
        if (isSimulator()) {
            if (localNotifications.containsKey(notificationId)) {
                TimerTask n = localNotifications.get(notificationId);
                n.cancel();
                localNotifications.remove(notificationId);
            }
        }
    }
    
    
    
    
    public static void blockMonitors() {
        blockMonitors = true;
    }

    public static void useAppFrame() {
        useAppFrame = true;
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
        if (isShowEDTWarnings() && !Display.getInstance().isEdt() && !inInit) {
            System.out.println("EDT violation detected!");
            if (isShowEDTViolationStacks()) {
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

    public java.awt.Rectangle getScreenCoordinates() {
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

    @Override
    public void copyToClipboard(Object obj) {
        if (obj instanceof String) {
            final String text = (String)obj;
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    Toolkit toolkit = Toolkit.getDefaultToolkit();
                    Clipboard clipboard = toolkit.getSystemClipboard();
                    StringSelection strSel = new StringSelection(text.trim());
                    clipboard.setContents(strSel, null);
                }
            });
        } else {
            final String text = "cn1lightweightclipboard://"+obj.toString();
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    Toolkit toolkit = Toolkit.getDefaultToolkit();
                    Clipboard clipboard = toolkit.getSystemClipboard();
                    StringSelection strSel = new StringSelection(text.trim());
                    clipboard.setContents(strSel, null);
                }
            });
        }
        super.copyToClipboard(obj); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object getPasteDataFromClipboard() {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Clipboard clipboard = toolkit.getSystemClipboard();

        for (DataFlavor flavor : clipboard.getAvailableDataFlavors()) {
            try {

                Object out = clipboard.getData(flavor);
                String str = null;
                if (out != null) {
                    str = out.toString();
                }
                if (str != null && str.startsWith("cn1lightweightclipboard://")) {
                    return super.getPasteDataFromClipboard();
                }
            } catch (Exception ex) {

            }

        }

        for (DataFlavor flavor : clipboard.getAvailableDataFlavors()) {
            try {
                Object out = clipboard.getData(flavor);
                if (out != null) {
                    String str = out.toString();
                    if (str != null) return str;
                }
            } catch (Exception ex) {

            }

        }

        return super.getPasteDataFromClipboard();
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

    @Override
    public boolean isSetCursorSupported() {
        return true;
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
                Hashtable h = r.getTheme(r.getThemeResourceNames()[0]);
                Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
                boolean desktopSkin = pref.getBoolean("desktopSkin", false);
                if(desktopSkin) {
                    safeAreaLandscape = null;
                    safeAreaPortrait = null;
                    h.remove("@paintsTitleBarBool");
                }
                UIManager.getInstance().setThemeProps(h);
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

    @Override
    public boolean isRightMouseButtonDown() {
        if (lastInputEvent != null) {
            if (lastInputEvent instanceof MouseEvent) {
                MouseEvent me = (MouseEvent) lastInputEvent;
                return SwingUtilities.isRightMouseButton(me);
            }
        }
        return false;
    }

    
    
    @Override
    public boolean isShiftKeyDown() {
        if (lastInputEvent != null) {
            return lastInputEvent.isShiftDown();
        }
        return false;
    }

    @Override
    public boolean isAltKeyDown() {
        if (lastInputEvent != null) {
            return lastInputEvent.isAltDown();
        }
        return false;
    }

    @Override
    public boolean isAltGraphKeyDown() {
        if (lastInputEvent != null) {
            return lastInputEvent.isAltGraphDown();
        }
        return false;
    }

    @Override
    public boolean isControlKeyDown() {
        if (lastInputEvent != null) {
            return lastInputEvent.isControlDown();
        }
        return false;
    }

    @Override
    public boolean isMetaKeyDown() {
        if (lastInputEvent != null) {
            return lastInputEvent.isMetaDown();
        }
        return false;
    }
    
    private static void dumpSwingHierarchy(java.awt.Component root, String indent) {
        System.out.println(indent + root.getName()+" "+root.getClass() + " "+root.getBounds());
        if (root instanceof Container) {
            Container rootc = (Container)root;
            for (int i=0; i<rootc.getComponentCount(); i++) {
                dumpSwingHierarchy(rootc.getComponent(i), indent + "    ");
            }
        }
        
    }

    public static void dumpComponentProperties(Component cmp) {
        dumpComponentProperties(cmp, "");
    }


    private static String methodPropertyName_(String name) {
        return name.startsWith("get") ? name.substring(3) : name.startsWith("is") ? name.substring(2) : name;
    }

    /**
     * Prints an object's properties to the console using reflection.  This is used by the component inspector.
     * Right click on a node in the component tree and select "Print to Console".
     *
     * @param cmp The component to print.
     * @param indent Indent string printed at start of each line.
     * @since 8.0
     */
    public static void dumpComponentProperties(Object cmp, String indent) {
        Class cls = cmp.getClass();
        Method[] methods = cls.getMethods();
        Arrays.sort(methods, new Comparator<Method>() {

            @Override
            public int compare(Method o1, Method o2) {
                return methodPropertyName_(o1.getName()).toLowerCase().compareTo(methodPropertyName_(o2.getName()).toLowerCase());
            }
        });
        System.out.println(indent + cmp.getClass().getName() + "{");
        for (int i=0; i<methods.length; i++) {
            Method method = methods[i];
            method.setAccessible(true);
            String name = method.getName();
            String propertyName = methodPropertyName_(name);
            if ((name.startsWith("get") || name.startsWith("is") || name.equalsIgnoreCase("scrollableYFlag") || name.equalsIgnoreCase("scrollableXFlag")) && method.getParameterCount() == 0 && method.getReturnType() != Void.class) {
                try {
                    System.out.println(indent + "  " + propertyName + ": " + method.invoke(cmp, new Object[0]));
                    if (propertyName.equalsIgnoreCase("style")) {
                        dumpComponentProperties(method.invoke(cmp, new Object[0]), indent + "  ");
                    }
                } catch (Exception ex){}
            }
        }
        System.out.println(indent + "}");
    }
    
    public int getCanvasX() {
        return canvas.x;
    }
    
    public int getCanvasY() {
        return canvas.y;
    }
    
    protected class C extends JPanel implements KeyListener, MouseListener, MouseMotionListener, HierarchyBoundsListener, AdjustmentListener, MouseWheelListener {
        private BufferedImage buffer;
        boolean painted;
        private Graphics2D g2dInstance;
        private java.awt.Dimension forcedSize;
        private boolean releaseLock;
        public int x, y;

        C() {
            super(null);
            setFocusTraversalKeysEnabled(false);
            addKeyListener(this);
            addMouseListener(this);
            addMouseWheelListener(this);
            addMouseMotionListener(this);
            addHierarchyBoundsListener(this);
            setFocusable(true);
            setOpaque(false);
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
        
        private BufferedImage updateBufferSize(BufferedImage buffer) {
            if (getScreenCoordinates() == null) {
                java.awt.Dimension d = getSize();
                if (buffer == null || buffer.getWidth() != (int)(d.width * retinaScale) || buffer.getHeight() != (int)(d.height*retinaScale)) {
                    buffer = createBufferedImage();
                }
            } else {
                if (buffer == null || buffer.getWidth() != (int) (getScreenCoordinates().width * zoomLevel)
                        || buffer.getHeight() != (int) (getScreenCoordinates().height * zoomLevel)) {
                    buffer = createBufferedImage();
                }
            }
            return buffer;
        }
        
        private void updateEdtBufferSize() {
            edtBuffer = updateBufferSize(edtBuffer);
        }
        
        // Only call on AWT Event thread
        private void updateBuffer(BufferedImage inputBuf) {
            if (buffer == null || buffer.getWidth() != inputBuf.getWidth() || buffer.getHeight() != inputBuf.getHeight()) {
                buffer = new BufferedImage(inputBuf.getWidth(), inputBuf.getHeight(), BufferedImage.TYPE_INT_RGB);
            }
            Graphics2D g = (Graphics2D)buffer.createGraphics();
            g.drawImage(inputBuf, 0, 0, this);
            g.dispose();
        }
        
        /**
         * We synchronize on this lock when copying buffer to/from edtBuffer
         */
        private final Object bufferLock = new Object();
        
        // Only use on EDT
        private BufferedImage edtBuffer;
        
        double blitTx;
        double blitTy;
        
        /**
         * We keep a counter to keep track of successive blits between 
         * AWT paints.  Generally AWT doesn't paint at all.  Only when 
         * there is a native peer, the window resizes, or text editing is 
         * going on.  If none of these are happening we can achieve efficiencies
         * by sharing the same image buffer on both the CN1 thread and the AWT
         * thread, since AWT will only use the buffer inside blit() , and
         * in that case, it is called inside SwingUtilities.invokeAndWait()
         * so it won't conflict with the EDT.
         * 
         * If AWT starts to do some painting, then this counter gets reset to 
         * 0 and we use two image buffers instead.
         */
        int blitCounter;
        
        public void blit() {
            if(menuDisplayed){
                return;
            }
            
            // We keep a blitCounter that gets reset in paintComponent()
            // If blit is called a number of times with no call to paintComponet
            // in between then it is probably safe to just use a shared 
            // image buffer between the CN1 EDT and the AWT EDT because 
            // AWT only uses this buffer inside SwingUtilities.invokeAndWait()
            // in this method.
            // If paintComponent() is being called, then it is likely that there
            // is a peer component being displayed or a text field, so 
            // we need to work in a thread-safe way - since we can't control
            // when paintComponent() is called.
            blitCounter++;
            boolean bufferUpdated = false;
            if (blitCounter > 5) {
                // blit() has been called more than 5 times since last
                // paintComponent() - we'll disable buffer thread safety
                // to maximize performance.
                blitCounter = 5;
                if (bufferSafeMode) {
                    bufferSafeMode = false;
                    buffer = null;
                }
                
            } else if (blitCounter < 5) {
                // blit() has not been called more than 5 times since 
                // last paintComponent() call - so we'll enable buffer
                // thread safety.
                
                if (!bufferSafeMode) {
                    bufferSafeMode = true;
                    synchronized(bufferLock) {
                        buffer = null;
                        updateBuffer(edtBuffer);
                        updateEdtBufferSize();
                        bufferUpdated = true;
                    }
                    //System.out.println("On");
                }
            }
            if (bufferSafeMode) {
                
                // When using buffer safe mode, we copy the edtBuffer
                // to the buffer in a synchronized block so that 
                // there is no possible conflict when paintComponent()
                // is called.
                if (!bufferUpdated) {
                    synchronized (bufferLock) {
                        updateBuffer(edtBuffer);
                        updateEdtBufferSize();
                    }
                }
            } else {
                
                // When not using buffer safe mode, we just use the same
                // edt buffer from AWT
                buffer = edtBuffer;
                
            }
            
            try {
                Runnable r = new Runnable() {
                    public void run() {
                        if (buffer != null) {
                            
                            java.awt.Graphics g = getGraphics();
                            if (g == null) {
                                return;
                            }
                            blitTx = ((Graphics2D)g).getTransform().getTranslateX();
                            blitTy = ((Graphics2D)g).getTransform().getTranslateY();
                            if (bufferSafeMode) {
                                // If AWT is painting and CN1 is painting
                                // then we need to be more careful.  Safe mode
                                // is enabled whenever AWT starts painting - this
                                // generally only occurs when there is a native
                                // peer on the screen
                                synchronized(bufferLock) {
                                    drawScreenBuffer(g);
                                }
                            } else {
                                drawScreenBuffer(g);
                                updateEdtBufferSize();
                            }
                            
                            if (window != null) {

                                if (zoomLevel != 1) {
                                    Graphics2D g2d = (Graphics2D) g;
                                    g2d.setTransform(AffineTransform.getScaleInstance(1, 1));
                                }
                                
                                if (window.getJMenuBar() != null) {
                                    for (int i = 0; i < window.getJMenuBar().getComponentCount(); i++) {
                                        JMenu m = (JMenu) window.getJMenuBar().getComponent(i);
                                        if (m.isPopupMenuVisible()) {
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
                };
                if(isDesignMode()) {
                    SwingUtilities.invokeLater(r);
                } else {
                    SwingUtilities.invokeAndWait(r);
                }
            } catch(Exception err) {
                err.printStackTrace();
            }
        }

        public void blit(int x, int y, int w, int h) {
            blit();
        }
        
        @Override
        protected void paintChildren(java.awt.Graphics g) {
        }

        private boolean drawScreenBuffer(java.awt.Graphics g) {
            if (buffer == null) {
                return false;
            }
            //g.setColor(Color.white);
            //g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
            AffineTransform t = ((Graphics2D)g).getTransform();
            AffineTransform t2 = AffineTransform.getScaleInstance(1/retinaScale, 1/retinaScale);
            
            t2.concatenate(t);
            
            ((Graphics2D)g).setTransform(t2);
            boolean painted = false;
            Rectangle screenCoord = getScreenCoordinates();
            if (screenCoord != null) {
                
                if(getComponentCount() > 0) {
                    Graphics2D bg = buffer.createGraphics();
                    if(zoomLevel != 1) {
                        AffineTransform af = bg.getTransform();
                        bg.setTransform(AffineTransform.getScaleInstance(1, 1));
                        bg.translate(-(screenCoord.x + x )* zoomLevel, -(screenCoord.y + y ) * zoomLevel);
                        super.paintChildren(bg);
                        bg.setTransform(af);
                    } else {
                        bg.translate(-screenCoord.x - x, -screenCoord.y - y);
                        super.paintChildren(bg);
                    }                    
                    bg.dispose();
                    painted = true;
                }
                
                if(roundedSkin) {
                    Graphics2D bg = buffer.createGraphics();
                    BufferedImage skin = getSkin();
                    bg.drawImage(skin, -(int) ((getScreenCoordinates().getX()) * zoomLevel), -(int) ((getScreenCoordinates().getY()) * zoomLevel), 
                            (int)(skin.getWidth() * zoomLevel), (int)(skin.getHeight() * zoomLevel), null);
                    bg.dispose();
                }
                
                if (isEnabled()) {
                    g.drawImage(buffer, (int) ((getScreenCoordinates().getX() + x) * zoomLevel), (int) ((getScreenCoordinates().getY() + y) * zoomLevel), this);
                } else {
                    g.setColor(Color.WHITE);
                    g.fillRect(x + (int) (getSkin().getWidth() * zoomLevel), y, getWidth(), getHeight());
                    g.fillRect(x, y + (int) (getSkin().getHeight() * zoomLevel), getWidth(), getHeight());
                    java.awt.Graphics g1 = buffer.getGraphics();
                    g1.setColor(Color.WHITE);
                    g1.fillRect(0, 0, buffer.getWidth(), buffer.getHeight());
                    g1.setColor(Color.BLACK);

                    java.awt.Font f = new java.awt.Font("Arial", Font.STYLE_BOLD, 20);
                    g1.setFont(f);
                    int sw = (int) Math.ceil(f.getStringBounds("Paused", canvas.getFRC()).getWidth());
                    g1.drawString("Paused", buffer.getWidth() / 2 - sw / 2, buffer.getHeight() / 2 - f.getSize() / 2);
                    g.drawImage(buffer, (int) ((getScreenCoordinates().getX() + x) * zoomLevel), (int) ((getScreenCoordinates().getY() + y) * zoomLevel), this);
                }
                //updateGraphicsScale(g);
                BufferedImage skin = getSkin();
                g.drawImage(skin, (int) (x * zoomLevel), (int) (( y) * zoomLevel), 
                        (int)(skin.getWidth() * zoomLevel), (int)(skin.getHeight() * zoomLevel), null);

                if (zoomLevel != 1) {
                    AffineTransform t3 = ((Graphics2D)g).getTransform();
                    t3.scale(zoomLevel/t3.getScaleX()/retinaScale, zoomLevel/t3.getScaleX()/retinaScale);
                    
                    //AffineTransform t3 = AffineTransform.getScaleInstance(zoomLevel, zoomLevel);
                    ((Graphics2D)g).setTransform(t3);
                    
                }
                
                //((Graphics2D)g).setTransform(t2);
                /*
                g.drawImage(getSkin(), 
                        (int)(x * retinaScale), 
                        (int)(y * retinaScale), 
                        (int)(getSkin().getWidth() * retinaScale), 
                        (int)(getSkin().getHeight() * retinaScale), this);
                */
                
            } else {
                if(getComponentCount() > 0) {
                    Graphics2D bg = buffer.createGraphics();
                    if(zoomLevel != 1) {
                        AffineTransform af = bg.getTransform();
                        bg.setTransform(AffineTransform.getScaleInstance(1, 1));
                        super.paintChildren(bg);
                        bg.setTransform(af);
                    } else {
                        super.paintChildren(bg);
                    }
                    bg.dispose();
                    painted = true;
                }
                g.drawImage(buffer, x, y, this);
            }
            ((Graphics2D)g).setTransform(t);
            return painted;
        }

        
        private boolean bufferSafeMode;
        public void paintComponent(java.awt.Graphics g) {
            //if (true) return;
            // This will turn on buffer safe mode
            // next time blit() is run
            blitCounter=0;
            
            if (buffer != null) {
                Graphics2D g2 = (Graphics2D)g.create();

                AffineTransform t = g2.getTransform();
                double tx = t.getTranslateX();
                double ty = t.getTranslateY();
                AffineTransform t2 = AffineTransform.getScaleInstance(retinaScale, retinaScale);
                t2.translate(tx, ty);
                if (getJavaVersion() < 9) {
                    // Java 8 didn't have full retina support
                    t2 = AffineTransform.getScaleInstance(1, 1);
                    t2.translate(tx * retinaScale, ty * retinaScale);
                }



                g2.setTransform(t2);

                synchronized(bufferLock) {
                    drawScreenBuffer(g2);
                }
                g2.dispose();
                //updateBufferSize();
                if (Display.isInitialized()) {
                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            Form f = getCurrentForm();
                            if (f != null) {
                                f.repaint();
                            }
                        }
                    });

                }    
            } else {
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        flushGraphics();
                    }
                });
            }
            
        }

        private void updateGraphicsScale(java.awt.Graphics g) {
            if (zoomLevel != 1) {
                Graphics2D g2d = (Graphics2D) g;
                AffineTransform t= g2d.getTransform();
                //t.translate(-t.getTranslateX(), -t.getTranslateY());
                t.scale(1/t.getScaleX(), 1/t.getScaleY());
                //System.out.println("Updating graphics scale to "+zoomLevel);
                t.scale(zoomLevel, zoomLevel);
                g2d.setTransform(t);
            }
        }

        public java.awt.Dimension getPreferredSize() {
            if (forcedSize != null) {
                return forcedSize;
            }
            if (getSkin() != null) {
                return new java.awt.Dimension((int)(getSkin().getWidth() / retinaScale), (int)(getSkin().getHeight() / retinaScale));
            }
            Form f = Display.getInstance().getCurrent();
            if (f != null) {
                return new java.awt.Dimension((int)(f.getPreferredW() / retinaScale), (int)(f.getPreferredH() / retinaScale));
            }
            return new java.awt.Dimension(800, 480);
        }

        public FontRenderContext getFRC() {
            return getGraphics2D().getFontRenderContext();
        }

        public Graphics2D getGraphics2D() {
            updateEdtBufferSize();
            while(g2dInstance == null) {
                
                g2dInstance = edtBuffer.createGraphics();
                updateGraphicsScale(g2dInstance);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                    Logger.getLogger(JavaSEPort.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            return g2dInstance;
        }

        private BufferedImage createBufferedImage() {
            g2dInstance = null;
            if (getScreenCoordinates() != null) {
                return new BufferedImage(Math.max(20, (int) (getScreenCoordinates().width * zoomLevel)), Math.max(20, (int) (getScreenCoordinates().height * zoomLevel)), BufferedImage.TYPE_INT_RGB);
            }
            return new BufferedImage(Math.max(20, (int)(getWidth() * retinaScale)), Math.max(20, (int)(getHeight() * retinaScale)), BufferedImage.TYPE_INT_RGB);
        }

        public void validate() {
            super.validate();
            //buffer = createBufferedImage();
            Form current = getCurrentForm();
            if (current == null) {
                return;
            }
        }

        private int getCode(java.awt.event.KeyEvent evt) {
            int code = evt.getKeyCode();
            switch (code) {
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
            char c = evt.getKeyChar();
            if(c == java.awt.event.KeyEvent.CHAR_UNDEFINED) {
                return evt.getKeyCode();
            }
            return c;
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
        // We only know if meta/ctrl/alt etc is down when the key is pressed, but we 
        // are taking action when the key is released... so we need to track whether the
        // control key was down while a key was pressed.
        private HashSet<Integer> ignorePressedKeys = new HashSet<Integer>();
        public void keyPressed(KeyEvent e) {
            if (!isEnabled()) {
                return;
            }
            if (e.isMetaDown() && e.getKeyChar() == 'c') {
                Form f = CN.getCurrentForm();
                if (f != null) {
                    final TextSelection ts = f.getTextSelection();
                    if (ts.isEnabled()) {
                        CN.callSerially(new Runnable() {
                            public void run() {
                                final String text = ts.getSelectionAsText();
                                if (text != null && !text.trim().isEmpty()) {
                                    EventQueue.invokeLater(new Runnable() {
                                        public void run() {
                                            Toolkit toolkit = Toolkit.getDefaultToolkit();
                                            Clipboard clipboard = toolkit.getSystemClipboard();
                                            StringSelection strSel = new StringSelection(text.trim());
                                            clipboard.setContents(strSel, null);
                                        }
                                    });
                                    
                                }
                            }
                        });
                    }
                }
                
            }
            
            if (e.isMetaDown() && e.getKeyChar() == 'a') {
                Form f = CN.getCurrentForm();
                if (f != null) {
                    final TextSelection ts = f.getTextSelection();
                    if (ts.isEnabled()) {
                        CN.callSerially(new Runnable() {
                            public void run() {
                                ts.selectAll();
                            }
                        });
                    }
                }
                
            }
            lastInputEvent = e;
            // block key combos that might generate unreadable events
            if (e.isAltDown() || e.isControlDown() || e.isMetaDown() || e.isAltGraphDown()) {
                ignorePressedKeys.add(e.getKeyCode());
                return;
            }
            int code = getCode(e);
            if (testRecorder != null) {
                testRecorder.eventKeyPressed(code);
            }
            JavaSEPort.this.keyPressed(code);
        }

        public void keyReleased(KeyEvent e) {
            boolean ignore = ignorePressedKeys.contains(e.getKeyCode());
            if (ignore) ignorePressedKeys.remove(e.getKeyCode());
            if (!isEnabled()) {
                return;
            }
            lastInputEvent = e;
            // block key combos that might generate unreadable events
            if (ignore || e.isAltDown() || e.isControlDown() || e.isMetaDown() || e.isAltGraphDown()) {
                return;
            }
            int code = getCode(e);
            if (testRecorder != null) {
                testRecorder.eventKeyReleased(code);
            }
            JavaSEPort.this.keyReleased(code);
        }

        public void mouseClicked(MouseEvent e) {
            e.consume();
        }

        private boolean showContextMenu(final MouseEvent me) {
            if (componentTreeInspector == null ||
                    !componentTreeInspector.isSimulatorRightClickEnabled() ||
                    !CN.isSimulator()) {
                return false;
            }

            Form f = Display.getInstance().getCurrent();
            if (f != null) {
                int x = scaleCoordinateX(me.getX());
                int y = scaleCoordinateY(me.getY());
                Component cmp = f.getComponentAt(x, y);
                if (cmp == null || cmp instanceof PeerComponent) {
                    return false;
                }
            }

            JPopupMenu menu = new JPopupMenu();
            registerMenuWithBlit(menu);
            JMenuItem inspectElement = new JMenuItem("Inspect Component");
            inspectElement.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (componentTreeInspector != null && componentTreeInspector.isSimulatorRightClickEnabled()) {
                        Form f = Display.getInstance().getCurrent();
                        if (f != null) {
                            int x = scaleCoordinateX(me.getX());
                            int y = scaleCoordinateY(me.getY());
                            Component cmp = f.getComponentAt(x, y);
                            componentTreeInspector.inspectComponent(cmp);
                        }
                    }
                }
            });
            menu.add(inspectElement);
            menu.show(me.getComponent(), me.getX(), me.getY());
            return true;
        }

        private int scaleCoordinateX(int coordinate) {
            if (getScreenCoordinates() != null) {
                return (int) (retinaScale * coordinate / zoomLevel - (getScreenCoordinates().x + x));
            }
            return (int)(coordinate * retinaScale);
        }

        private int scaleCoordinateY(int coordinate) {
            if (getScreenCoordinates() != null) {
                return (int) (retinaScale * coordinate / zoomLevel - (getScreenCoordinates().y + y));
            }
            return (int)(coordinate * retinaScale);
        }
        Integer triggeredKeyCode;
        private boolean mouseDown;
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                if (showContextMenu(e)) {
                    return;
                }
            }
            this.mouseDown = true;
            Form f = Display.getInstance().getCurrent();
            if (f != null) {
                int x = scaleCoordinateX(e.getX());
                int y = scaleCoordinateY(e.getY());
                Component cmp = f.getComponentAt(x, y);
                if (!(cmp instanceof PeerComponent)) {
                    cn1GrabbedDrag = true;
                }
            }
            e.consume();
            if (!isEnabled()) {
                return;
            }
            lastInputEvent = e;
            if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0 || (e.getModifiers() & MouseEvent.BUTTON3_MASK) != 0) {
                releaseLock = false;
                int x = scaleCoordinateX(e.getX());
                int y = scaleCoordinateY(e.getY());
                if (x >= 0 && x < getDisplayWidthImpl() && y >= 0 && y < getDisplayHeightImpl()) {
                    if (touchDevice) {
                        if (testRecorder != null) {
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
                            if (testRecorder != null) {
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
            if (e.isPopupTrigger()) {

                if (showContextMenu(e)) {
                    return;
                }
            }
            boolean mouseDown = this.mouseDown;
            this.mouseDown = false;
            cn1GrabbedDrag = false;
            e.consume();
            if (!isEnabled()) {
                return;
            }
            lastInputEvent = e;
            if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0 || (e.getModifiers() & MouseEvent.BUTTON3_MASK) != 0) {
                int x = scaleCoordinateX(e.getX());
                int y = scaleCoordinateY(e.getY());
                if (mouseDown || (x >= 0 && x < getDisplayWidthImpl() && y >= 0 && y < getDisplayHeightImpl())) {
                    if (touchDevice) {
                        x = Math.min(getDisplayWidthImpl(), Math.max(0, x));
                        y = Math.min(getDisplayHeightImpl(), Math.max(0, y));
                        if (testRecorder != null) {
                            testRecorder.eventPointerReleased(x, y);
                        }
                        JavaSEPort.this.pointerReleased(x, y);
                    }
                }
                if (triggeredKeyCode != null) {
                    int code = getCode(triggeredKeyCode.intValue());
                    if (testRecorder != null) {
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
            if (!isEnabled()) {
                return;
            }
            lastInputEvent = e;
            if (!releaseLock && (e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0) {
                int x = scaleCoordinateX(e.getX());
                int y = scaleCoordinateY(e.getY());
                if (mouseDown || (x >= 0 && x < getDisplayWidthImpl() && y >= 0 && y < getDisplayHeightImpl())) {
                    if (touchDevice) {
                        x = Math.min(getDisplayWidthImpl(), Math.max(0, x));
                        y = Math.min(getDisplayHeightImpl(), Math.max(0, y));
                        if (testRecorder != null && hasDragStarted(x, y)) {
                            testRecorder.eventPointerDragged(x, y);
                        }
                        JavaSEPort.this.pointerDragged(x, y);
                    }
                }
                return;
            }
            
            // right click dragging means a pinch to zoom
            if (!releaseLock && isPinchZoom(e)) {
                int x = scaleCoordinateX(e.getX());
                int y = scaleCoordinateY(e.getY());
                if (mouseDown || (x >= 0 && x < getDisplayWidthImpl() && y >= 0 && y < getDisplayHeightImpl())) {
                    if (touchDevice) {
                        JavaSEPort.this.pointerDragged(new int[]{Math.min(getDisplayWidthImpl(), Math.max(0,x)), 0}, new int[]{Math.min(getDisplayHeightImpl(), Math.max(0, y)), 0});
                    }
                } 
                return;
            }  
        }

        private boolean isPinchZoom(MouseEvent e) {
            return ((e.getModifiers() & MouseEvent.BUTTON3_MASK) != 0)
                    || ((e.getModifiers() & MouseEvent.SHIFT_MASK) != 0);
        }
        private Cursor handCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
        private Cursor defaultCursor = Cursor.getDefaultCursor();        
        private int currentCursor = 0;
        private java.util.Timer reSize;
        
        public void mouseMoved(MouseEvent e) {
            e.consume();
            if (!isEnabled()) {
                return;
            }
            lastInputEvent = e;
            if(invokePointerHover) {
                int x = scaleCoordinateX(e.getX());
                int y = scaleCoordinateY(e.getY());
                if (x >= 0 && x < getDisplayWidthImpl() && y >= 0 && y < getDisplayHeightImpl()) {
                    JavaSEPort.this.pointerHover(x, y);
                }
                
                
            }
            Form f = Display.getInstance().getCurrent();
            if (f != null && f.isEnableCursors()) {
                int x = scaleCoordinateX(e.getX());
                int y = scaleCoordinateY(e.getY());
                if (x >= 0 && x < getDisplayWidthImpl() && y >= 0 && y < getDisplayHeightImpl()) {
                    Component cmp = f.getComponentAt(x, y);
                    if (cmp != null) {
                        int cursor = cmp.getCursor();
                        if (cursor != currentCursor) {
                            currentCursor = cursor;
                            setCursor(Cursor.getPredefinedCursor(cursor));
                        }
                    } else {
                        if (currentCursor != 0) {
                            currentCursor = 0;
                            setCursor(defaultCursor);
                        }
                    }
                } else {
                    if (currentCursor != 0) {
                        setCursor(defaultCursor);
                    }
                            
                }
            } else {
                if (currentCursor != 0) {
                    setCursor(defaultCursor);
                }
            }
            if (getSkinHotspots() != null) {
                java.awt.Point p = new java.awt.Point((int) ((e.getX() - canvas.x) / zoomLevel), (int) ((e.getY() - canvas.y) / zoomLevel));
                if (getSkinHotspots().containsKey(p)) {
                    setCursor(handCursor);
                } else {
                    setCursor(currentCursor == 0 ? defaultCursor : Cursor.getPredefinedCursor(currentCursor));
                }
            } 
        }

        public void ancestorMoved(HierarchyEvent e) {
        }

        public void setBounds(int x, int y, int w, int h) {
            super.setBounds(x, y, w, h);
            Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
            boolean desktopSkin = pref.getBoolean("desktopSkin", false);
            if (getSkin() == null && !desktopSkin) {
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        JavaSEPort.this.sizeChanged((int)(getWidth() * retinaScale), (int)(getHeight() * retinaScale));
                    }
                });                
            }

        }
        
        
        public void ancestorResized(HierarchyEvent e) {
            
            /*
            if (e.getChanged() != getParent()) {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        System.out.println("Parent size: "+getParent().getSize()+"; Window size "+((JFrame)getTopLevelAncestor()).getContentPane().getSize());
                        
                        getParent().setSize(((JFrame)getTopLevelAncestor()).getContentPane().getSize());
                    }
                });
                return;
            } */
            if (getSkin() != null) {
                if (!scrollableSkin) {
                    float w1 = ((float) getParent().getWidth() * (float)retinaScale) / ((float) getSkin().getWidth());
                    float h1 = ((float) getParent().getHeight() * (float)retinaScale) / ((float) getSkin().getHeight());
                    zoomLevel = Math.min(h1, w1);
                    Form f = Display.getInstance().getCurrent();
                    if (f != null) {
                        f.repaint();
                    }
                }
                getParent().repaint();
            } else {  
                //some ugly hacks to workaround black screen issue
                if (canvas != null && !fullScreen) {
                    
                    //dumpSwingHierarchy(getTopLevelAncestor(), "");
                    Dimension topSize = ((JFrame)getTopLevelAncestor()).getContentPane().getSize();
                    Dimension parentSize = getParent().getSize();
                    if (e.getChanged() != getParent() && !topSize.equals(parentSize)) {
                        getParent().setSize(topSize);
                        getParent().doLayout();
                    }
                    setSize((int)topSize.getWidth(), (int)topSize.getHeight());
                    canvas.setForcedSize(new Dimension(getWidth(), getHeight()));
                    
                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            
                            JavaSEPort.this.sizeChanged((int)(getWidth() * retinaScale), (int)(getHeight() * retinaScale));
                            
                            Form f = Display.getInstance().getCurrent();
                            if (f != null) {
                                f.revalidate();
                            }
                        }
                    });
                    return;
                } 
                
                if(reSize == null){
                    reSize = new java.util.Timer();
                }else{
                    reSize.cancel();
                    reSize = new java.util.Timer();
                }
                
                reSize.schedule(new TimerTask(){
                    @Override
                    public void run() {
                        //if(mediaContainer != null){
                        //    System.out.println("Resize with media container");
                        //    JavaSEPort.this.sizeChanged((int)(mediaContainer.getWidth() * retinaScale), (int)(mediaContainer.getHeight() * retinaScale));
                        //}else{
                            Display.getInstance().callSerially(new Runnable() {
                                public void run() {
                                    JavaSEPort.this.sizeChanged((int)(getWidth() * retinaScale), (int)(getHeight() * retinaScale));
                                    g2dInstance = null;
                                    Form f = Display.getInstance().getCurrent();
                                    if (f != null) {
                                        f.forceRevalidate();
                                    }
                                    
                                    // probably not necessary
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                Thread.sleep(1500);
                                            } catch (Exception e) {
                                            }
                                            if (window != null) {
                                                window.repaint();
                                            }
                                        }
                                    }).start();

                                    reSize = null;
                                }
                            });
                            
                        //}
                        
                    }
                    
                }, 200);
            }
        }

        @Override
        public void adjustmentValueChanged(AdjustmentEvent e) {
            JScrollBar s = (JScrollBar) e.getSource();
            int val = s.getValue();
            if(getSkin() != null) {
                if (s.getOrientation() == Scrollbar.HORIZONTAL) {
                    x = -(int) ((float) (val / 100f) * getSkin().getWidth());
                } else {
                    y = -(int) ((float) (val / 100f) * getSkin().getHeight());
                }
            } else {
                if (s.getOrientation() == Scrollbar.HORIZONTAL) {
                    x = -(int) ((float) (val / 100f) * getWidth());
                } else {
                    y = -(int) ((float) (val / 100f) * getHeight());
                }
            }
            repaint();

        }

        int lastUnits = 0;
        boolean ignoreWheelMovements = false;
        int lastX;
        int lastY;
        public void mouseWheelMoved(final MouseWheelEvent e) {
            e.consume();
            if (!isEnabled()) {
                return;
            }
            lastInputEvent = e;
            final int x = scrollWheeling ? lastX : scaleCoordinateX(e.getX());
            final int y = scrollWheeling ? lastY : scaleCoordinateY(e.getY());
            if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
                Form f = getCurrentForm();
                if(f != null){
                    Component cmp;
                    try {
                        cmp = f.getComponentAt(x, y);
                    } catch (Throwable t) {
                        // Since this is called off the edt, we sometimes hit 
                        // NPEs and Array Index out of bounds errors here
                        cmp = null;
                    }
                    if(cmp != null && Accessor.isScrollDecelerationMotionInProgress(cmp)) {
                        if (!ignoreWheelMovements) {
                            ignoreWheelMovements = true;
                        }
                        return;
                    } else {
                        ignoreWheelMovements = false;
                    }
                }
                requestFocus();
                
                final int units = convertToPixels(e.getUnitsToScroll() * 5, true) * -1;

                if (units * lastUnits < 0 || Math.abs(units) - Math.abs(lastUnits) > 100) {
                    ignoreWheelMovements = false;
                }
                lastUnits = units;
                lastX = x;
                lastY = y;
                if (ignoreWheelMovements) {
                    return;
                }
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        scrollWheeling = true;
                        Form f = getCurrentForm();
                        if(f != null){
                            Component cmp = f.getComponentAt(x, y);
                            
                            if(cmp != null && cmp.isFocusable()) {
                                cmp.setFocusable(false);
                                f.pointerPressed(x, y);
                                f.pointerDragged(x, y + units / 4);
                                cmp.setFocusable(true);
                            } else {
                                f.pointerPressed(x, y);
                                f.pointerDragged(x, y + units / 4);
                            }
                        }
                    }
                });
                
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        Form f = getCurrentForm();
                        if(f != null){
                            Component cmp = f.getComponentAt(x, y);
                            if (cmp != null && Accessor.isScrollDecelerationMotionInProgress(cmp)) {
                                return;
                            }
                            if(cmp != null && cmp.isFocusable()) {
                                cmp.setFocusable(false);
                                f.pointerDragged(x, y + units / 4 * 2);
                                cmp.setFocusable(true);
                            } else {
                                f.pointerDragged(x, y + units / 4 * 2);
                            }
                        }
                    }
                });
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        Form f = getCurrentForm();
                        if(f != null){
                            Component cmp = f.getComponentAt(x, y);
                            if (cmp != null && Accessor.isScrollDecelerationMotionInProgress(cmp)) {
                                return;
                            }
                            if(cmp != null && cmp.isFocusable()) {
                                cmp.setFocusable(false);
                                f.pointerDragged(x, y + units / 4 * 3);
                                cmp.setFocusable(true);
                            } else {
                                f.pointerDragged(x, y + units / 4 * 3);
                            }
                        }
                    }
                });
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        Form f = getCurrentForm();
                        if(f != null){
                            Component cmp = f.getComponentAt(x, y);
                            if (cmp != null && Accessor.isScrollDecelerationMotionInProgress(cmp)) {
                                f.pointerReleased(x, y + units);
                                return;
                            }
                            if(cmp != null && cmp.isFocusable()) {
                                cmp.setFocusable(false);
                                f.pointerDragged(x, y + units);
                                f.pointerReleased(x, y + units);
                                cmp.setFocusable(true);
                            } else {
                                f.pointerDragged(x, y + units);
                                f.pointerReleased(x, y + units);
                            }
                        }
                        scrollWheeling = false;
                    }
                });
            } 
        }
        
    }
    protected boolean cn1GrabbedDrag=false;
    public C canvas;

    public java.awt.Container getCanvas() {
        return canvas;
    }

    public static JavaSEPort instance;
    
    public JavaSEPort() {
        canvas = new C();
        instance = this;
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
        double scale = 1.0; // retinaScale
        screenPosition.x = (int)(screenX1 / scale);
        screenPosition.y = (int)(screenY1 / scale); 
        screenPosition.width = (int)((screenX2 - screenX1 + 1)/scale);
        screenPosition.height = (int)((screenY2 - screenY1 + 1)/scale);
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

    @Override
    public boolean isMinimized() {
        if (Display.getInstance().isSimulator()) {
            return minimized;
        } else {
            return super.isMinimized();
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
                    long esize = e.getSize();
                    if(esize > 0) {
                        nativeThemeData = new byte[(int) esize];
                        readFully(z, nativeThemeData);
                    } else {
                        ByteArrayOutputStream b = new ByteArrayOutputStream();
                        Util.copyNoClose(z, b, 8192);
                        nativeThemeData = b.toByteArray();
                    }
                    e = z.getNextEntry();
                    continue;
                }
                if (name.endsWith(".ttf")) {
                    try {
                        java.awt.Font result = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, z);
                        GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(result);
                    } catch (FontFormatException ex) {
                        ex.printStackTrace();
                    } catch (IOException ex) {
                        if (ex.getMessage().contains("Problem reading font data")) {
                            System.err.println("Problem reading entry "+name+" from skin file");
                            System.err.println("This issue may be related to https://github.com/codenameone/CodenameOne/issues/2640");
                            System.err.println("The application should still function normally, although this font may not render correctly.");
                            System.err.println("Please help us out by posting your build output log to https://github.com/codenameone/CodenameOne/issues/2640 and any other information you think may be helpful in tracking down the cause.");
                            System.err.println("Skin Properties:");
                            System.err.println(props);
                            System.err.println("Stack Trace:");
                            ex.printStackTrace(System.err);
                        } else {
                            throw ex;
                        }
                    }

                    e = z.getNextEntry();
                    continue;
                }
                e = z.getNextEntry();
            }
            z.close();

            String ppi = props.getProperty("ppi");
            if(ppi != null) {
                double ppiD = Double.valueOf(ppi);
                pixelMilliRatio = ppiD / 25.4;
            } else {
                String pix = props.getProperty("pixelRatio");
                if (pix != null && pix.length() > 0) {
                    try {
                        pixelMilliRatio = Double.valueOf(pix);
                    } catch (NumberFormatException err) {
                        err.printStackTrace();
                        pixelMilliRatio = null;
                    }
                } else {
                    pixelMilliRatio = null;
                }
            }

            portraitSkinHotspots = new HashMap<Point, Integer>();
            portraitScreenCoordinates = new Rectangle();

            landscapeSkinHotspots = new HashMap<Point, Integer>();
            landscapeScreenCoordinates = new Rectangle();
            if(props.getProperty("roundScreen", "false").equalsIgnoreCase("true")) {
                safeAreaLandscape = new Rectangle();
                safeAreaPortrait = new Rectangle();

            
                portraitScreenCoordinates.x = Integer.parseInt(props.getProperty("displayX"));
                portraitScreenCoordinates.y = Integer.parseInt(props.getProperty("displayY"));
                portraitScreenCoordinates.width = Integer.parseInt(props.getProperty("displayWidth"));
                portraitScreenCoordinates.height = Integer.parseInt(props.getProperty("displayHeight"));
                landscapeScreenCoordinates.x = portraitScreenCoordinates.y;
                landscapeScreenCoordinates.y = portraitScreenCoordinates.x;
                landscapeScreenCoordinates.width = portraitScreenCoordinates.height;
                landscapeScreenCoordinates.height = portraitScreenCoordinates.width;
                safeAreaPortrait.setBounds(
                        Integer.parseInt(props.getProperty("safePortraitX", "0")),
                        Integer.parseInt(props.getProperty("safePortraitY", "0")),
                        Integer.parseInt(props.getProperty("safePortraitWidth", ""+portraitScreenCoordinates.width)),
                        Integer.parseInt(props.getProperty("safePortraitHeight", ""+portraitScreenCoordinates.height))
                );
                safeAreaLandscape.setBounds(
                        Integer.parseInt(props.getProperty("safeLandscapeX", "0")),
                        Integer.parseInt(props.getProperty("safeLandscapeY", "0")),
                        Integer.parseInt(props.getProperty("safeLandscapeWidth", ""+landscapeScreenCoordinates.width)),
                        Integer.parseInt(props.getProperty("safeLandscapeHeight", ""+landscapeScreenCoordinates.height))
                );
                roundedSkin = true;
            } else {
                initializeCoordinates(map, props, portraitSkinHotspots, portraitScreenCoordinates);
                initializeCoordinates(landscapeMap, props, landscapeSkinHotspots, landscapeScreenCoordinates);
            }


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
                if (platformName.equals("win")) {
                    ua = "Mozilla/5.0 (compatible; MSIE 9.0; Windows Phone OS 7.5; Trident/5.0; IEMobile/9.0; NOKIA; Lumia 800)";
                } else {
                    ua = "Mozilla/5.0 (Linux; U; Android 2.2; en-us; Nexus One Build/FRF91) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1";
                }
            }
            Display.getInstance().setProperty("User-Agent", ua);

            isIOS = props.getProperty("systemFontFamily", "Arial").toLowerCase().contains("helvetica");
            setFontFaces(props.getProperty("systemFontFamily", "Arial"),
                    props.getProperty("proportionalFontFamily", "SansSerif"),
                    props.getProperty("monospaceFontFamily", "Monospaced"));
            int med;
            int sm;
            int la;
            if(pixelMilliRatio == null) {
                float factor = ((float) getDisplayHeightImpl()) / 480.0f;
                med = (int) (15.0f * factor);
                sm = (int) (11.0f * factor);
                la = (int) (19.0f * factor);
            } else {
                med = (int) Math.round(2.6 * pixelMilliRatio.doubleValue());
                sm = (int) Math.round(2 * pixelMilliRatio.doubleValue());
                la = (int) Math.round(3.3 * pixelMilliRatio.doubleValue());
            }
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
                            boolean isJ2me = props.getProperty("platformName", "").equals("me");
                            String t = props.getProperty("nativeThemeAttribute", null);
                            if (t != null) {
                                Properties cnop = new Properties();
                                File cnopFile = new File(getCWD(), "codenameone_settings.properties");
                                if (cnopFile.exists()) {
                                    cnop.load(new FileInputStream(cnopFile));
                                    int themeConst = Integer.parseInt(cnop.getProperty("codename1.j2me.nativeThemeConst", "3"));
                                    t = cnop.getProperty(t, null); 
                                    if (isJ2me && themeConst == 3 && t != null && new File(t).exists()) {
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
            installMenu(frm, false);
            
        } catch (IOException err) {
            err.printStackTrace();
        }
    }



    @Override
    public com.codename1.ui.geom.Rectangle getDisplaySafeArea(com.codename1.ui.geom.Rectangle rect) {
        if (!isSimulator() || safeAreaPortrait == null || safeAreaLandscape == null) {
            return super.getDisplaySafeArea(rect);
        }
        if (rect == null) {
            rect = new com.codename1.ui.geom.Rectangle();
        }
        if (portrait) {
            rect.setBounds((int)safeAreaPortrait.getX(), (int)safeAreaPortrait.getY(), (int)safeAreaPortrait.getWidth(), (int)safeAreaPortrait.getHeight());
        } else {
            rect.setBounds((int)safeAreaLandscape.getX(), (int)safeAreaLandscape.getY(), (int)safeAreaLandscape.getWidth(), (int)safeAreaLandscape.getHeight());
        }
        return rect;
    }
    
    private JMenuItem debugInChromeMenuItem;
    public void setChromeDebugPort(int port) {
        System.setProperty("cef.debugPort", ""+port);
        if (debugInChromeMenuItem != null) {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    debugInChromeMenuItem.setEnabled(true);
                }
            });
            
        }
    }

    private static ImageIcon getZoomIcon(boolean scrollableSkinValue) {
        if (scrollableSkinValue) {
            return SwingUtils.getImageIcon(JavaSEPort.class.getResource("baseline_zoom_in_black_24dp.png"), ICON_SIZE, ICON_SIZE);
        } else {
            return SwingUtils.getImageIcon(JavaSEPort.class.getResource("baseline_zoom_out_black_24dp.png"), ICON_SIZE, ICON_SIZE);
        }
    }

    /**
     * Mutator for scrollable skin that will trigger a UI update on
     * change.
     * @param scrollableSkinValue
     */
    private static void setScrollableSkin(boolean scrollableSkinValue) {
        if (scrollableSkin != scrollableSkinValue) {
            scrollableSkin = scrollableSkinValue;
            Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
            pref.putBoolean("Scrollable", scrollableSkin);
            instance.updateFrameUI();
        }
    }

    private void setPortrait(boolean portraitValue) {
        if (portrait != portraitValue) {
            portrait = portraitValue;
            Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
            pref.putBoolean("Portrait", portrait);
            updateFrameUI();

        }
    }


    private void updateFrameUI() {
        if (instance.appFrame != null) {
            if (EventQueue.isDispatchThread()) {
                instance.appFrame.updateAppFrameUI();
            } else {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        instance.appFrame.updateAppFrameUI();
                    }
                });
            }
        }
    }


    public class OpenJavadocsAction extends AbstractAction implements AppFrame.UpdatableUI {
        public OpenJavadocsAction() {
            super("", SwingUtils.getImageIcon(JavaSEPort.class.getResource("baseline_help_center_black_24dp.png"), ICON_SIZE, ICON_SIZE));
            putValue(SHORT_DESCRIPTION, "Open JavaDocs");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Component currentComponent = componentTreeInspector.getCurrentComponent();
            if (currentComponent == null) return;
            Class componentClass = currentComponent.getClass();
            while (componentClass != null) {
                File jarFile = locateJar(componentClass);
                if (jarFile != null && jarFile.exists() && jarFile.getName().endsWith(".jar")) {
                    File javadocsJar = new File(jarFile.getParentFile(), jarFile.getName());
                    if (!javadocsJar.getName().endsWith("-javadoc.jar")) {
                        String jnameBase = javadocsJar.getName().substring(0, javadocsJar.getName().lastIndexOf(".jar"));

                        javadocsJar = new File(javadocsJar.getParentFile(), jnameBase+"-javadoc.jar");

                    }
                    if (javadocsJar.exists()) {
                        File extractedDir = extractJar(javadocsJar);
                        File htmlFile = new File(extractedDir, getJavadocPath(componentClass));
                        if (htmlFile.exists()) {
                            if (openHtmlFile(htmlFile)) {
                                return;
                            }
                        }

                    }
                }
                componentClass = componentClass.getSuperclass();
            }

            JOptionPane.showMessageDialog(window, "No javadocs found for this component.");
        }

        protected void update() {


        }

        private boolean openHtmlFile(File file) {
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                try {
                    desktop.browse(file.toURI());
                    return true;
                } catch (Exception ex) {
                    return false;
                }
            }
            return false;
        }

        private File locateJar(Class clazz) {
            try {

                String uri = clazz.getResource("/"+getClassPathPathWithUnixSlash(clazz)).toURI().toString();
                if (uri.startsWith("jar:")) {
                    uri = uri.substring(4);
                }
                if (uri.contains("!")) {
                    uri = uri.substring(0, uri.indexOf("!"));
                }
                return new File(new URI(uri));
            } catch (Exception ex) {
                return null;
            }
        }
        private File extractJar(File jarFile) {
            File extractedDir = new File(jarFile.getParentFile(), jarFile.getName()+"-extracted");
            if (extractedDir.exists()) {
                return extractedDir;
            }

            extractedDir.mkdir();
            ProcessBuilder pb = new ProcessBuilder(findJarToolPath(), "xf", jarFile.getAbsolutePath());
            pb.directory(extractedDir);
            try {
                if (pb.start().waitFor() == 0) {
                    return extractedDir;
                } else {
                    extractedDir.delete();
                    return null;
                }
            } catch (Exception ex) {
                extractedDir.delete();
                return null;
            }

        }

        private String findJarToolPath() {
            if (MavenUtils.isRunningInJDK()) {
                File javac = MavenUtils.findJavac();
                if (javac.exists()) {
                    File jar = new File(javac.getParentFile(), "jar");
                    if (!jar.exists()) {
                        jar = new File(javac.getParentFile(), "jar.exe");
                    }
                    if (jar.exists()) {
                        return jar.getAbsolutePath();
                    }
                }
            }
            String javaHome = System.getProperty("java.home");
            if (javaHome != null) {
                File fJavaHome = new File(javaHome);
                if (fJavaHome.exists()) {
                    File binDir = new File(fJavaHome, "bin");
                    File jar = new File(binDir, "jar");
                    if (jar.exists()) {
                        return jar.getAbsolutePath();
                    }
                    jar = new File(binDir, "jar.exe");
                    if (jar.exists()) {
                        return jar.getAbsolutePath();
                    }
                }
            }
            return "jar";
        }

        private String getClassPathPathWithUnixSlash(Class cls) {
            return cls.getName().replace(".", "/")+".class";
        }

        private String getJavadocPath(Class cls) {
            return cls.getName().replace(".", File.separator) + ".html";
        }

        @Override
        public void onUpdateAppFrameUI(AppFrame frame) {

        }
    }

    public class ZoomAction extends AbstractAction implements AppFrame.UpdatableUI {

        private final boolean scrollableSkinValue;

        public ZoomAction(boolean scrollableSkinValue) {
            super("", getZoomIcon(scrollableSkinValue));
            this.scrollableSkinValue = scrollableSkinValue;
            if (scrollableSkinValue) {
                putValue(SHORT_DESCRIPTION, "Zoom in");
            } else {
                putValue(SHORT_DESCRIPTION, "Zoom out");
            }
            update();


        }




        protected void update() {
            Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
            if (pref.getBoolean("desktopSkin", false)) {
                setEnabled(false);
                return;
            }
            if (scrollableSkin == scrollableSkinValue) {
                setEnabled(false);
            } else {
                setEnabled(true);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JFrame frm = window;
            setScrollableSkin(scrollableSkinValue);


            if (scrollableSkin) {
                if (appFrame == null) {
                    frm.add(java.awt.BorderLayout.SOUTH, hSelector);
                    frm.add(java.awt.BorderLayout.EAST, vSelector);
                } else {
                    canvas.getParent().add(java.awt.BorderLayout.SOUTH, hSelector);
                    canvas.getParent().add(java.awt.BorderLayout.EAST, vSelector);
                }

            } else {
                java.awt.Container selectorParent = hSelector.getParent();
                if (selectorParent != null) {
                    selectorParent.remove(hSelector);
                    selectorParent.remove(vSelector);
                }
            }
            Container parent = canvas.getParent();
            parent.remove(canvas);
            if (scrollableSkin) {
                canvas.setForcedSize(new java.awt.Dimension((int)(getSkin().getWidth() / retinaScale), (int)(getSkin().getHeight() / retinaScale)));
            } else {
                int screenH = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getHeight();
                int screenW = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getWidth();
                float zoomY = getSkin().getHeight() > screenH ? screenH/(float)getSkin().getHeight() : 1f;
                float zoomX = getSkin().getWidth() > screenW ? screenW/(float)getSkin().getWidth() : 1f;
                float zoom = Math.min(zoomX, zoomY);
                canvas.setForcedSize(new java.awt.Dimension((int)(getSkin().getWidth()  * zoom), (int)(getSkin().getHeight() * zoom)));
                if (window != null) {
                    if (appFrame == null) {
                        window.setSize(new java.awt.Dimension((int) (getSkin().getWidth() * zoom), (int) (getSkin().getHeight() * zoom)));
                    } else {

                        // THis is a hack to get it to repaint.
                        // Probably this is because a full refresh of the canvas
                        // is triggered by the ancestorResized event, but
                        // Can't figure out how to trigger it otherwise, we
                        // this ugly thing increases the decreases the window size by
                        // one pixel to trigger the refresh.
                        // Without this, the simulator canvas doesn't update
                        // until the window is resized.
                        java.awt.Container top = (parent instanceof JComponent) ?
                                ((JComponent)parent).getTopLevelAncestor() :
                                window;
                        if (top == null) top = window;

                        int currW = top.getWidth();
                        int currH = top.getHeight();
                        top.setSize(new Dimension(currW+1, currH+1));
                        top.revalidate();
                        top.setSize(new Dimension(currW, currH));
                        top.revalidate();
                    }
                }
            }
            parent.add(BorderLayout.CENTER, canvas);

            canvas.x = 0;
            canvas.y = 0;
            zoomLevel = 1;

            if (appFrame != null) {
                appFrame.revalidate();
                Display.getInstance().getCurrent().repaint();
                appFrame.repaint();
                parent.revalidate();
                parent.revalidate();
                canvas.repaint();
                Timer timer = new Timer();
                TimerTask tt = new TimerTask() {

                    @Override
                    public void run() {
                        EventQueue.invokeLater(new Runnable() {
                            public void run() {
                                canvas.revalidate();
                                canvas.repaint();

                            }
                        });
                    }
                };
                timer.schedule(tt, 100L);

            } else {
                frm.invalidate();
                frm.pack();
                Display.getInstance().getCurrent().repaint();
                frm.repaint();
            }

        }

        @Override
        public void onUpdateAppFrameUI(AppFrame frame) {
            update();
        }
    }


    private static ImageIcon getRotateActionImageIcon(boolean portraitValue) {
        if (portraitValue) {
            return SwingUtils.getImageIcon(JavaSEPort.class.getResource("baseline_stay_current_portrait_black_24dp.png"), ICON_SIZE, ICON_SIZE);
        } else {
            return SwingUtils.getImageIcon(JavaSEPort.class.getResource("baseline_stay_current_landscape_black_24dp.png"), ICON_SIZE, ICON_SIZE);
        }
    }


    public class ScreenshotSettingsAction extends AbstractAction implements CompanionMenuAction {

        public ScreenshotSettingsAction() {
            super("", SwingUtils.getImageIcon(JavaSEPort.class.getResource("baseline_expand_more_black_24dp.png"), ICON_SIZE/2, ICON_SIZE/2));
            putValue(SHORT_DESCRIPTION, "Screenshot settings");
        }



        @Override
        public void actionPerformed(ActionEvent e) {
            final Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
            includeHeaderInScreenshot = pref.getBoolean("includeHeaderScreenshot", true);
            final JCheckBoxMenuItem includeHeaderMenu = new JCheckBoxMenuItem("Screenshot StatusBar");
            includeHeaderMenu.setToolTipText("Include status bar area in Screenshots");
            includeHeaderMenu.setSelected(includeHeaderInScreenshot);

            includeHeaderMenu.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    includeHeaderInScreenshot = includeHeaderMenu.isSelected();
                    pref.putBoolean("includeHeaderScreenshot", includeHeaderInScreenshot);
                }
            });

            includeSkinInScreenshot = pref.getBoolean("includeSkinInScreenshot", false);
            final JCheckBoxMenuItem includeSkinMenu = new JCheckBoxMenuItem("Screenshot Skin");
            includeSkinMenu.setToolTipText("Include skin in Screenshots");
            includeSkinMenu.setSelected(includeSkinInScreenshot);

            includeSkinMenu.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    includeSkinInScreenshot = includeSkinMenu.isSelected();
                    pref.putBoolean("includeSkinInScreenshot", includeSkinInScreenshot);
                }
            });


            JPopupMenu popupMenu = new JPopupMenu();
            registerMenuWithBlit(popupMenu);
            popupMenu.add(includeHeaderMenu);
            popupMenu.add(includeSkinMenu);

            if (e.getSource() instanceof java.awt.Component) {

                java.awt.Component cmp = (java.awt.Component)e.getSource();
                popupMenu.show(cmp, 0, cmp.getHeight());
            }
        }
    }

    public class ScreenshotAction extends AbstractAction {

        public ScreenshotAction() {
            super("", SwingUtils.getImageIcon(JavaSEPort.class.getResource("baseline_photo_camera_black_24dp.png"), ICON_SIZE, ICON_SIZE));
            putValue(SHORT_DESCRIPTION, "Screenshot");


        }

        public void actionPerformedWithSkin(ActionEvent e) {
            final float zoom = zoomLevel;
            zoomLevel = 1;

            final Form frm = Display.getInstance().getCurrent();
            BufferedImage headerImageTmp;
            if (isPortrait()) {
                headerImageTmp = header;
            } else {
                headerImageTmp = headerLandscape;
            }
            if (!includeHeaderInScreenshot) {
                headerImageTmp = null;
            }
            int headerHeightTmp = 0;
            if (headerImageTmp != null) {
                headerHeightTmp = headerImageTmp.getHeight();
            }
            final int headerHeight = headerHeightTmp;
            final BufferedImage headerImage = headerImageTmp;
            //gr.translate(0, statusBarHeight);
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    final com.codename1.ui.Image img = com.codename1.ui.Image.createImage(frm.getWidth(), frm.getHeight());
                    com.codename1.ui.Graphics gr = img.getGraphics();
                    takingScreenshot = true;
                    screenshotActualZoomLevel = zoom;
                    try {
                        frm.paint(gr);
                    } finally {
                        takingScreenshot = false;
                    }
                    final int imageWidth = img.getWidth();
                    final int imageHeight = img.getHeight();
                    final int[] imageRGB = img.getRGB();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            BufferedImage bi = new BufferedImage(frm.getWidth(), frm.getHeight() + headerHeight, BufferedImage.TYPE_INT_ARGB);
                            bi.setRGB(0, headerHeight, imageWidth, imageHeight, imageRGB, 0, imageWidth);
                            BufferedImage skin = getSkin();
                            BufferedImage newSkin = new BufferedImage(skin.getWidth(), skin.getHeight(), BufferedImage.TYPE_INT_ARGB);
                            Graphics2D g2d = newSkin.createGraphics();
                            g2d.drawImage(bi, getScreenCoordinates().x, getScreenCoordinates().y, null);
                            if (headerImage != null) {
                                g2d.drawImage(headerImage, getScreenCoordinates().x, getScreenCoordinates().y, null);
                            }
                            g2d.drawImage(skin, 0, 0, null);
                            g2d.dispose();
                            OutputStream out = null;
                            try {
                                out = new FileOutputStream(findScreenshotFile());
                                ImageIO.write(newSkin, "png", out);
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
                }
            });
        }

        public void actionPerformed(ActionEvent ae) {
            boolean includeSkin = includeSkinInScreenshot;
            if (includeSkin) {
                actionPerformedWithSkin(ae);
                return;
            }
            final float zoom = zoomLevel;
            zoomLevel = 1;

            final Form frm = Display.getInstance().getCurrent();
            BufferedImage headerImageTmp;
            if (isPortrait()) {
                headerImageTmp = header;
            } else {
                headerImageTmp = headerLandscape;
            }
            if (!includeHeaderInScreenshot) {
                headerImageTmp = null;
            }
            int headerHeightTmp = 0;
            if (headerImageTmp != null) {
                headerHeightTmp = headerImageTmp.getHeight();
            }
            final int headerHeight = headerHeightTmp;
            final BufferedImage headerImage = headerImageTmp;
            //gr.translate(0, statusBarHeight);
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    final com.codename1.ui.Image img = com.codename1.ui.Image.createImage(frm.getWidth(), frm.getHeight());
                    com.codename1.ui.Graphics gr = img.getGraphics();
                    takingScreenshot = true;
                    screenshotActualZoomLevel = zoom;
                    try {
                        frm.paint(gr);
                    } finally {
                        takingScreenshot = false;
                    }
                    final int imageWidth = img.getWidth();
                    final int imageHeight = img.getHeight();
                    final int[] imageRGB = img.getRGB();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            BufferedImage bi = new BufferedImage(frm.getWidth(), frm.getHeight() + headerHeight, BufferedImage.TYPE_INT_ARGB);
                            bi.setRGB(0, headerHeight, imageWidth, imageHeight, imageRGB, 0, imageWidth);
                            if (headerImage != null) {
                                Graphics2D g2d = bi.createGraphics();
                                g2d.drawImage(headerImage, 0, 0, null);
                                g2d.dispose();
                            }
                            OutputStream out = null;
                            try {
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
                }
            });
        }

    }

    public class RotateAction extends AbstractAction implements AppFrame.UpdatableUI, SelectableAction {
        private boolean portraitValue;
        public RotateAction(boolean portraitValue) {
            super("", getRotateActionImageIcon(portraitValue));
            this.portraitValue = portraitValue;
            if (portraitValue) {
                putValue(SHORT_DESCRIPTION, "Rotate to portrait mode");
            } else {
                putValue(SHORT_DESCRIPTION, "Rotate to landscape mode");
            }
            update();
        }

        private void update() {
            Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
            boolean desktopSkin = pref.getBoolean("desktopSkin", false);
            setEnabled(!desktopSkin);
            Boolean selected = (Boolean)getValue(SELECTED_KEY);
            if (selected == null) {
                selected = false;
            }
            if (selected != (portraitValue == portrait)) {
                putValue(SELECTED_KEY, portraitValue == portrait);
            }


        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setPortrait(portraitValue);

            Container parent = canvas.getParent();
            parent.remove(canvas);
            canvas.setForcedSize(new java.awt.Dimension((int)(getSkin().getWidth()*zoomLevel), (int)(getSkin().getHeight()*zoomLevel)));
            if (appFrame == null) {
                if (window != null) {
                    window.setSize(new java.awt.Dimension((int) (getSkin().getWidth() * zoomLevel), (int) (getSkin().getHeight() * zoomLevel)));
                }
            }
            java.awt.Container top = ((JComponent)parent).getTopLevelAncestor();
            top.revalidate();
            top.repaint();
            parent.add(BorderLayout.CENTER, canvas);
            if (appFrame == null) {
                window.pack();
            }

            //zoomLevel = 1;
            JavaSEPort.this.sizeChanged(getScreenCoordinates().width, getScreenCoordinates().height);

        }

        @Override
        public void onUpdateAppFrameUI(AppFrame frame) {
            update();
        }
    }

    public void registerSplitPaneWithBlit(JSplitPane splitPane) {
        SplitPaneUI spui = splitPane.getUI();
        if (spui instanceof BasicSplitPaneUI) {
            // Setting a mouse listener directly on split pane does not work, because no events are being received.
            ((BasicSplitPaneUI) spui).getDivider().addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    menuDisplayed = true;
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    menuDisplayed = false;
                }
            });
        }

    }

    public void registerMenuWithBlit(JPopupMenu menu) {
        menu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                menuDisplayed = true;
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                menuDisplayed = false;
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                menuDisplayed = false;
            }
        });
    }

    /**
     * The simulator blit() function tends to draw over menus, so this method will
     * register a menu to disable blit while the menu is opened.
     * @param menu
     */
    public void registerMenuWithBlit(JMenu menu) {
        menu.setDoubleBuffered(true);
        menu.addMenuListener(new MenuListener(){

            @Override
            public void menuSelected(MenuEvent e) {
                menuDisplayed = true;
            }

            @Override
            public void menuCanceled(MenuEvent e) {
                menuDisplayed = false;
            }

            @Override
            public void menuDeselected(MenuEvent e) {
                menuDisplayed = false;
            }
        });
    }

    private void installMenu(final JFrame frm, boolean desktopSkin) throws IOException{
        final Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
        JMenuBar bar = new JMenuBar();
        frm.setJMenuBar(bar);

        JMenu simulatorMenu = new JMenu("Simulator");
        registerMenuWithBlit(simulatorMenu);
        JMenu simulateMenu = new JMenu("Simulate");
        registerMenuWithBlit(simulateMenu);
        JMenu toolsMenu = new JMenu("Tools");
        registerMenuWithBlit(toolsMenu);

        JMenuItem buildHintEditor = new JMenuItem("Edit Build Hints...");
        ActionListener l = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new BuildHintEditor(JavaSEPort.this).show();
            }
        };
        buildHintEditor.addActionListener(l);
        toolsMenu.add(buildHintEditor);

        final JCheckBoxMenuItem useAppFrameMenu = new JCheckBoxMenuItem("Single Window Mode", useAppFrame);
        useAppFrameMenu.setToolTipText("Check this option to enable Single Window mode, in which the simulator, component inspector, network monitor and other tools are all included in a single, multi-panel window");
        useAppFrameMenu.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {

                try {
                    pref.putBoolean("cn1.simulator.useAppFrame", useAppFrameMenu.isSelected());
                    deinitializeSync();
                    frm.dispose();
                    System.setProperty("reload.simulator", "true");
                } catch (Exception ex) {
                    Log.e(ex);
                }

            }
        });
        simulatorMenu.add(useAppFrameMenu);

        final JCheckBoxMenuItem zoomMenu = new JCheckBoxMenuItem("Zoom", scrollableSkin);
        if (appFrame == null) simulatorMenu.add(zoomMenu);

        JMenu debugEdtMenu = new JMenu("Debug EDT");
        toolsMenu.add(debugEdtMenu);

        zoomMenu.setEnabled(!desktopSkin);

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

        int debugEdtSelection = pref.getInt("debugEDTMode", 0);
        switch (debugEdtSelection) {
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
        if (appFrame == null) simulatorMenu.add(screenshot);
        KeyStroke f2 = KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0);
        screenshot.setAccelerator(f2);
        screenshot.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                final float zoom = zoomLevel;
                zoomLevel = 1;

                final Form frm = Display.getInstance().getCurrent();
                BufferedImage headerImageTmp;
                if (isPortrait()) {
                    headerImageTmp = header;
                } else {
                    headerImageTmp = headerLandscape;
                }
                if (!includeHeaderInScreenshot) {
                    headerImageTmp = null;
                }
                int headerHeightTmp = 0;
                if (headerImageTmp != null) {
                    headerHeightTmp = headerImageTmp.getHeight();
                }
                final int headerHeight = headerHeightTmp;
                final BufferedImage headerImage = headerImageTmp;
                //gr.translate(0, statusBarHeight);
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        final com.codename1.ui.Image img = com.codename1.ui.Image.createImage(frm.getWidth(), frm.getHeight());
                        com.codename1.ui.Graphics gr = img.getGraphics();
                        takingScreenshot = true;
                        screenshotActualZoomLevel = zoom;
                        try {
                            frm.paint(gr);
                        } finally {
                            takingScreenshot = false;
                        }
                        final int imageWidth = img.getWidth();
                        final int imageHeight = img.getHeight();
                        final int[] imageRGB = img.getRGB();
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                BufferedImage bi = new BufferedImage(frm.getWidth(), frm.getHeight() + headerHeight, BufferedImage.TYPE_INT_ARGB);
                                bi.setRGB(0, headerHeight, imageWidth, imageHeight, imageRGB, 0, imageWidth);
                                if (headerImage != null) {
                                    Graphics2D g2d = bi.createGraphics();
                                    g2d.drawImage(headerImage, 0, 0, null);
                                    g2d.dispose();
                                }
                                OutputStream out = null;
                                try {
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
                    }
                });
            }
        });

        JMenuItem screenshotWithSkin = new JMenuItem("Screenshot With Skin");
        if (appFrame == null) simulatorMenu.add(screenshotWithSkin);
        screenshotWithSkin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final float zoom = zoomLevel;
                zoomLevel = 1;

                final Form frm = Display.getInstance().getCurrent();
                BufferedImage headerImageTmp;
                if (isPortrait()) {
                    headerImageTmp = header;
                } else {
                    headerImageTmp = headerLandscape;
                }
                if (!includeHeaderInScreenshot) {
                    headerImageTmp = null;
                }
                int headerHeightTmp = 0;
                if (headerImageTmp != null) {
                    headerHeightTmp = headerImageTmp.getHeight();
                }
                final int headerHeight = headerHeightTmp;
                final BufferedImage headerImage = headerImageTmp;
                //gr.translate(0, statusBarHeight);
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        final com.codename1.ui.Image img = com.codename1.ui.Image.createImage(frm.getWidth(), frm.getHeight());
                        com.codename1.ui.Graphics gr = img.getGraphics();
                        takingScreenshot = true;
                        screenshotActualZoomLevel = zoom;
                        try {
                            frm.paint(gr);
                        } finally {
                            takingScreenshot = false;
                        }
                        final int imageWidth = img.getWidth();
                        final int imageHeight = img.getHeight();
                        final int[] imageRGB = img.getRGB();
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                BufferedImage bi = new BufferedImage(frm.getWidth(), frm.getHeight() + headerHeight, BufferedImage.TYPE_INT_ARGB);
                                bi.setRGB(0, headerHeight, imageWidth, imageHeight, imageRGB, 0, imageWidth);
                                BufferedImage skin = getSkin();
                                BufferedImage newSkin = new BufferedImage(skin.getWidth(), skin.getHeight(), BufferedImage.TYPE_INT_ARGB);
                                Graphics2D g2d = newSkin.createGraphics();
                                g2d.drawImage(bi, getScreenCoordinates().x, getScreenCoordinates().y, null);
                                if (headerImage != null) {
                                    g2d.drawImage(headerImage, getScreenCoordinates().x, getScreenCoordinates().y, null);
                                }
                                g2d.drawImage(skin, 0, 0, null);
                                g2d.dispose();
                                OutputStream out = null;
                                try {
                                    out = new FileOutputStream(findScreenshotFile());
                                    ImageIO.write(newSkin, "png", out);
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
                    }
                });
            }
        });


        includeHeaderInScreenshot = pref.getBoolean("includeHeaderScreenshot", true);
        final JCheckBoxMenuItem includeHeaderMenu = new JCheckBoxMenuItem("Screenshot StatusBar");
        includeHeaderMenu.setToolTipText("Include status bar area in Screenshots");
        includeHeaderMenu.setSelected(includeHeaderInScreenshot);
        if (appFrame == null) simulatorMenu.add(includeHeaderMenu);
        includeHeaderMenu.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                includeHeaderInScreenshot = includeHeaderMenu.isSelected();
                pref.putBoolean("includeHeaderScreenshot", includeHeaderInScreenshot);
            }
        });


        JMenu networkDebug = new JMenu("Network");
        toolsMenu.add(networkDebug);

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
        networkDebug.add(networkMonitor);

        JMenuItem proxy = new JMenuItem("Proxy Settings");
        proxy.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                final JDialog proxy;
                if(window !=null){
                    proxy = new JDialog(window);
                }else{
                    proxy = new JDialog();
                }
                final Preferences pref = Preferences.userNodeForPackage(Component.class);
                int proxySel = pref.getInt("proxySel", 2);
                String proxySelHttp = pref.get("proxySel-http", "");
                String proxySelPort = pref.get("proxySel-port", "");

                JPanel panel = new JPanel();
                panel.setAlignmentX( java.awt.Component.LEFT_ALIGNMENT );
                panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

                JPanel proxyUrl= new JPanel();
                proxyUrl.setLayout(new FlowLayout(FlowLayout.LEFT));
                proxyUrl.add(new JLabel("Http Proxy:"));
                final JTextField http = new JTextField(proxySelHttp);
                http.setColumns(20);
                proxyUrl.add(http);
                proxyUrl.add(new JLabel("Port:"));
                final JTextField port = new JTextField(proxySelPort);
                port.setColumns(4);
                proxyUrl.add(port);

                final JRadioButton noproxy = new JRadioButton("No Proxy");
                JPanel rbPanel= new JPanel();
                rbPanel.setLayout(new java.awt.GridLayout(1, 0));
                rbPanel.setAlignmentX( java.awt.Component.LEFT_ALIGNMENT );
                rbPanel.add(noproxy);
                Dimension d = rbPanel.getPreferredSize();
                d.width = proxyUrl.getPreferredSize().width;
                rbPanel.setMinimumSize(d);
                //noproxy.setPreferredSize(d);
                panel.add(rbPanel);

                final JRadioButton systemProxy = new JRadioButton("Use System Proxy");
                rbPanel= new JPanel();
                rbPanel.setLayout(new java.awt.GridLayout(1, 0));
                rbPanel.setAlignmentX( java.awt.Component.LEFT_ALIGNMENT );
                rbPanel.add(systemProxy);
                d = rbPanel.getPreferredSize();
                d.width = proxyUrl.getPreferredSize().width;
                rbPanel.setPreferredSize(d);
                panel.add(rbPanel);

                final JRadioButton manual = new JRadioButton("Manual Proxy Settings:");
                rbPanel= new JPanel();
                rbPanel.setLayout(new java.awt.GridLayout(1, 0));
                rbPanel.setAlignmentX( java.awt.Component.LEFT_ALIGNMENT );
                rbPanel.add(manual);
                d = rbPanel.getPreferredSize();
                d.width = proxyUrl.getPreferredSize().width;
                rbPanel.setPreferredSize(d);
                panel.add(rbPanel);

                rbPanel= new JPanel();
                rbPanel.setLayout(new java.awt.GridLayout(1, 0));
                rbPanel.setAlignmentX( java.awt.Component.LEFT_ALIGNMENT );
                rbPanel.add(proxyUrl);
                panel.add(rbPanel);

                ButtonGroup group = new ButtonGroup();
                group.add(noproxy);
                group.add(systemProxy);
                group.add(manual);
                noproxy.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        http.setEnabled(false);
                        port.setEnabled(false);
                    }
                });
                systemProxy.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        http.setEnabled(false);
                        port.setEnabled(false);
                    }
                });
                manual.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        http.setEnabled(true);
                        port.setEnabled(true);
                    }
                });

                switch (proxySel){
                    case 1:
                        noproxy.setSelected(true);
                        http.setEnabled(false);
                        port.setEnabled(false);
                        break;
                    case 2:
                        systemProxy.setSelected(true);
                        http.setEnabled(false);
                        port.setEnabled(false);
                        break;
                    case 3:
                        manual.setSelected(true);
                        break;
                }
                JPanel closePanel = new JPanel();
                JButton close = new JButton("Ok");
                close.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        if (noproxy.isSelected()) {
                            pref.putInt("proxySel", 1);
                        } else if (systemProxy.isSelected()) {
                            pref.putInt("proxySel", 2);
                        } else if (manual.isSelected()) {
                            pref.putInt("proxySel", 3);
                            pref.put("proxySel-http", http.getText());
                            pref.put("proxySel-port", port.getText());
                        }
                        proxy.dispose();

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
                            deinitializeSync();
                            frm.dispose();
                            System.setProperty("reload.simulator", "true");
                        } else {
                            refreshSkin(frm);
                    }

                    }
                });
                closePanel.add(close);
                panel.add(closePanel);

                proxy.add(panel);
                proxy.pack();
                if(window != null){
                    proxy.setLocationRelativeTo(window);
                }
                proxy.setResizable(false);
                proxy.setVisible(true);


            }
        });
        networkDebug.add(proxy);
        networkDebug.addSeparator();


        JRadioButtonMenuItem regularConnection = new JRadioButtonMenuItem("Regular Connection");
        regularConnection.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                slowConnectionMode = false;
                disconnectedMode = false;
                pref.putInt("connectionStatus", 0);
            }
        });
        networkDebug.add(regularConnection);

        JRadioButtonMenuItem slowConnection = new JRadioButtonMenuItem("Slow Connection");
        slowConnection.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                slowConnectionMode = true;
                disconnectedMode = false;
                pref.putInt("connectionStatus", 1);
            }
        });
        networkDebug.add(slowConnection);

        JRadioButtonMenuItem disconnected = new JRadioButtonMenuItem("Disconnected");
        disconnected.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                slowConnectionMode = false;
                disconnectedMode = true;
                pref.putInt("connectionStatus", 2);
            }
        });
        networkDebug.add(disconnected);

        ButtonGroup connectionGroup = new ButtonGroup();
        connectionGroup.add(regularConnection);
        connectionGroup.add(slowConnection);
        connectionGroup.add(disconnected);

        switch(pref.getInt("connectionStatus", 0)) {
            case 0:
                regularConnection.setSelected(true);
                break;
            case 1:
                slowConnection.setSelected(true);
                slowConnectionMode = true;
                break;
            case 2:
                disconnected.setSelected(true);
                disconnectedMode = true;
                break;
        }

        JMenuItem componentTreeInspector = new JMenuItem("Component Inspector");
        componentTreeInspector.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                if (appFrame != null) return;

                new ComponentTreeInspector().showInFrame();
            }
        });

        JMenuItem scriptingConsole = new JMenuItem("Groovy Console");
        scriptingConsole.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new CN1Console().open((java.awt.Component)e.getSource());
            }
        });


        List<String> inputArgs = java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments();
        //final boolean isDebug = inputArgs.toString().indexOf("-agentlib:jdwp") > 0;
        //final boolean usingHotswapAgent = inputArgs.toString().indexOf("-XX:HotswapAgent") > 0;
        ButtonGroup hotReloadGroup = new ButtonGroup();
        JRadioButtonMenuItem disableHotReload = new JRadioButtonMenuItem("Disabled");
        disableHotReload.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                pref.putInt("hotReload", 0);
                System.setProperty("hotReload", "0");
            }
        });
        JRadioButtonMenuItem reloadSimulator = new JRadioButtonMenuItem("Reload Simulator");
        reloadSimulator.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                pref.putInt("hotReload", 1);
                System.setProperty("hotReload", "1");
            }
        });
        JRadioButtonMenuItem reloadCurrentForm = new JRadioButtonMenuItem("Reload Current Form (Requires CodeRAD)");

        reloadCurrentForm.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                pref.putInt("hotReload", 2);
                System.setProperty("hotReload", "2");
            }
        });

        switch (pref.getInt("hotReload", 0)) {
            case 0:
                disableHotReload.setSelected(true);
                System.setProperty("hotReload", "0");
                break;
            case 1:
                reloadSimulator.setSelected(true);
                System.setProperty("hotReload", "1");
                break;
            case 2:
                reloadCurrentForm.setSelected(true);
                System.setProperty("hotReload", "2");
                break;

        }

        JMenu hotReloadMenu = new JMenu("Hot Reload");
        hotReloadMenu.add(disableHotReload);
        hotReloadMenu.add(reloadSimulator);
        hotReloadMenu.add(reloadCurrentForm);
        hotReloadGroup.add(disableHotReload);
        hotReloadGroup.add(reloadSimulator);
        hotReloadGroup.add(reloadCurrentForm);
        if (isRunningInMaven() && MavenUtils.isRunningInJDK()) {
            toolsMenu.add(hotReloadMenu);
        }


        scriptingConsole.setToolTipText("Open interactive console");

        JMenuItem appArg = new JMenuItem("Send App Argument");
        appArg.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Executor.stopApp();
                JPanel pnl = new JPanel();
                JTextField tf = new JTextField(20);
                pnl.add(new JLabel("Argument to The App"));
                pnl.add(tf);
                int val = JOptionPane.showConfirmDialog(canvas, pnl, "Please Enter The Argument", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (val != JOptionPane.OK_OPTION) {
                    Executor.startApp();
                    return;
                }
                String arg = tf.getText();
                Display.getInstance().setProperty("AppArg", arg);
                Executor.startApp();
            }
        });
        simulateMenu.add(appArg);


        JMenuItem debugWebViews = new JMenuItem("Debug Web Views");
        debugWebViews.setEnabled(false);
        debugInChromeMenuItem = debugWebViews;
        debugWebViews.setToolTipText("Debug app's BrowserComponents' Javascript and DOM inside Chrome's debugger");
        debugWebViews.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                CN.callSerially(new Runnable() {
                    public void run() {
                        String port = System.getProperty("cef.debugPort", null);
                        if (port != null) {
                            final Sheet sheet = new Sheet(null, "Debug Web Views");
                            SpanLabel info = new SpanLabel("You can debug this app's web views in Chrome's "
                                    + "debugger by opening the following URL in Chrome:");

                            SpanLabel warning = new SpanLabel("Debugging only works in Chrome.  If Chrome is not your default browser "
                                    + "then you'll need to copy and paste the URL above into Chrome");
                            ComponentSelector.select("*", warning).add(warning, true)
                                    .selectAllStyles()
                                    .setFontSizeMillimeters(2)
                                    .setFgColor(0x555555);
                            FontImage.setMaterialIcon(warning, FontImage.MATERIAL_WARNING, 3);
                            final com.codename1.ui.TextField tf = new com.codename1.ui.TextField("http://localhost:"+port);
                            tf.addPointerPressedListener(new com.codename1.ui.events.ActionListener() {
                                @Override
                                public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                                    Display.getInstance().copyToClipboard(tf.getText());
                                    ToastBar.showInfoMessage("URL Copied to Clipboard");
                                    sheet.back();
                                }
                            });
                            tf.setEditable(false);

                            com.codename1.ui.Button copy = new com.codename1.ui.Button(com.codename1.ui.FontImage.MATERIAL_CONTENT_COPY);
                            copy.addActionListener(new com.codename1.ui.events.ActionListener() {
                                @Override
                                public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                                    Display.getInstance().copyToClipboard(tf.getText());
                                    ToastBar.showInfoMessage("URL Copied to Clipboard");
                                    sheet.back();
                                }
                            });

                            com.codename1.ui.Button open = new com.codename1.ui.Button("Open In Default Browser");
                            open.addActionListener(new com.codename1.ui.events.ActionListener() {
                                @Override
                                public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                                    CN.execute(tf.getText());
                                    sheet.back();
                                }
                            });

                            sheet.getContentPane().setLayout(com.codename1.ui.layouts.BoxLayout.y());
                            sheet.getContentPane().add(info);
                            sheet.getContentPane().add(com.codename1.ui.layouts.BorderLayout.centerEastWest(tf, copy, null));
                            sheet.getContentPane().add(open);
                            sheet.getContentPane().add(warning);
                            sheet.setPosition(com.codename1.ui.layouts.BorderLayout.CENTER);
                            sheet.show();


                        } else {
                            ToastBar.showErrorMessage("Debugger not available.  The Chrome debugger is only available in apps that contain a BrowserComponent");
                        }
                    }
                });
            }
        });
        toolsMenu.add(debugWebViews);

        JMenuItem locationSim = new JMenuItem("Location Simulation");
        locationSim.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if(locSimulation==null) {
                        locSimulation = new LocationSimulation();
                }
                locSimulation.setVisible(true);
            }
        });
        simulateMenu.add(locationSim);

        JMenuItem pushSim = new JMenuItem("Push Simulation");
        pushSim.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if(pushSimulation == null) {
                        pushSimulation = new PushSimulator();
                }
                pref.putBoolean("PushSimulator", true);
                pushSimulation.setVisible(true);
            }
        });
        simulateMenu.add(pushSim);

        if (appFrame == null) {
            toolsMenu.add(componentTreeInspector);
        }
        toolsMenu.add(scriptingConsole);


        JMenuItem testRecorderMenu = new JMenuItem("Test Recorder");
        testRecorderMenu.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                if (testRecorder == null) {
                    showTestRecorder();
                }
            }
        });
        toolsMenu.add(testRecorderMenu);

        /*
        JMenu darkLightModeMenu = new JMenu("Dark/Light Mode");
        simulatorMenu.add(darkLightModeMenu);
        final JRadioButtonMenuItem darkMode = new JRadioButtonMenuItem("Dark Mode");
        final JRadioButtonMenuItem lightMode = new JRadioButtonMenuItem("Light Mode");
        final JRadioButtonMenuItem unsupportedMode = new JRadioButtonMenuItem("Unsupported");
        ButtonGroup group = new ButtonGroup();
        group.add(darkMode);
        group.add(lightMode);
        group.add(unsupportedMode);
        darkMode.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JavaSEPort.this.darkMode = true;
            }
        });

        lightMode.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JavaSEPort.this.darkMode = false;
            }
        });

        unsupportedMode.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JavaSEPort.this.darkMode = null;
            }
        });
        */

        manualPurchaseSupported = pref.getBoolean("manualPurchaseSupported", true);
        managedPurchaseSupported = pref.getBoolean("managedPurchaseSupported", true);
        subscriptionSupported = pref.getBoolean("subscriptionSupported", true);
        refundSupported = pref.getBoolean("refundSupported", true);
        JMenu purchaseMenu = new JMenu("In App Purchase");
        simulateMenu.add(purchaseMenu);
        final JCheckBoxMenuItem manualPurchaseSupportedMenu = new JCheckBoxMenuItem("Manual Purchase");
        manualPurchaseSupportedMenu.setSelected(manualPurchaseSupported);
        final JCheckBoxMenuItem managedPurchaseSupportedMenu = new JCheckBoxMenuItem("Managed Purchase");
        managedPurchaseSupportedMenu.setSelected(managedPurchaseSupported);
        final JCheckBoxMenuItem subscriptionSupportedMenu = new JCheckBoxMenuItem("Subscription");
        subscriptionSupportedMenu.setSelected(subscriptionSupported);
        final JCheckBoxMenuItem refundSupportedMenu = new JCheckBoxMenuItem("Refunds");
        refundSupportedMenu.setSelected(refundSupported);
        manualPurchaseSupportedMenu.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                manualPurchaseSupported = manualPurchaseSupportedMenu.isSelected();
                pref.putBoolean("manualPurchaseSupported", manualPurchaseSupported);
            }
        });
        managedPurchaseSupportedMenu.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                managedPurchaseSupported = managedPurchaseSupportedMenu.isSelected();
                pref.putBoolean("managedPurchaseSupported", managedPurchaseSupported);
            }
        });
        subscriptionSupportedMenu.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                subscriptionSupported = subscriptionSupportedMenu.isSelected();
                pref.putBoolean("subscriptionSupported", subscriptionSupported);
            }
        });
        refundSupportedMenu.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                refundSupported = refundSupportedMenu.isSelected();
                pref.putBoolean("refundSupported", refundSupported);
            }
        });
        purchaseMenu.add(manualPurchaseSupportedMenu);
        purchaseMenu.add(managedPurchaseSupportedMenu);
        purchaseMenu.add(subscriptionSupportedMenu);
        purchaseMenu.add(refundSupportedMenu);

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
        toolsMenu.add(performanceMonitor);

        JMenuItem clean = new JMenuItem("Clean Storage");
        clean.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                File home = new File(System.getProperty("user.home") + File.separator + appHomeDir);
                if(!home.exists()){
                    return;
                }
                if(JOptionPane.showConfirmDialog(frm,
                        "Are you sure you want to Clean all Storage under "
                                + home.getAbsolutePath() + " ?",
                        "Clean Storage",
                        JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION){
                File [] files = home.listFiles();
                for (int i = 0; i < files.length; i++) {
                    File file = files[i];
                    file.delete();
                }
            }
            }
        });
        toolsMenu.add(clean);



        JMenu skinMenu = createSkinsMenu(frm, null);
        skinMenu.addMenuListener(new MenuListener(){

            @Override
            public void menuSelected(MenuEvent e) {
                menuDisplayed = true;
            }

            @Override
            public void menuCanceled(MenuEvent e) {
                menuDisplayed = false;
            }

            @Override
            public void menuDeselected(MenuEvent e) {
                menuDisplayed = false;
            }



        });


        //final JCheckBoxMenuItem touchFlag = new JCheckBoxMenuItem("Touch", touchDevice);
        //simulatorMenu.add(touchFlag);
        //final JCheckBoxMenuItem nativeInputFlag = new JCheckBoxMenuItem("Native Input", useNativeInput);
        //simulatorMenu.add(nativeInputFlag);
        //final JCheckBoxMenuItem simulateAndroidVKBFlag = new JCheckBoxMenuItem("Simulate Android VKB", simulateAndroidKeyboard);
        //simulatorMenu.add(simulateAndroidVKBFlag);

        /*final JCheckBoxMenuItem slowMotionFlag = new JCheckBoxMenuItem("Slow Motion", false);
        toolsMenu.add(slowMotionFlag);
        slowMotionFlag.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Motion.setSlowMotion(slowMotionFlag.isSelected());
            }
        });*/

        final JCheckBoxMenuItem permFlag = new JCheckBoxMenuItem("Android 6 Permissions", android6PermissionsFlag);
        simulateMenu.add(permFlag);
        permFlag.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                android6PermissionsFlag = !android6PermissionsFlag;
                Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
                pref.putBoolean("Android6Permissions", android6PermissionsFlag);

            }
        });

        pause = new JMenuItem("Pause App");
        simulateMenu.addSeparator();
        simulateMenu.add(pause);
        pause.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (pause.getText().startsWith("Pause")) {
                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            Executor.stopApp();
                            minimized = true;
                        }
                    });
                    canvas.setEnabled(false);
                    pause.setText("Resume App");
                } else {
                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            Executor.startApp();
                            minimized = false;
                        }
                    });
                    canvas.setEnabled(true);
                    pause.setText("Pause App");
                }
            }
        });

        final JCheckBoxMenuItem alwaysOnTopFlag = new JCheckBoxMenuItem("Always on Top", alwaysOnTop);
        if (appFrame == null) simulatorMenu.add(alwaysOnTopFlag);

        if (appFrame == null) simulatorMenu.addSeparator();


        JMenuItem exit = new JMenuItem("Exit");
        simulatorMenu.add(exit);

        JMenu helpMenu = new JMenu("Help");
        helpMenu.setDoubleBuffered(true);
        helpMenu.addMenuListener(new MenuListener(){

            @Override
            public void menuSelected(MenuEvent e) {
                menuDisplayed = true;
            }

            @Override
            public void menuCanceled(MenuEvent e) {
                menuDisplayed = false;
            }

            @Override
            public void menuDeselected(MenuEvent e) {
                menuDisplayed = false;
            }
        });


        JMenuItem javadocs = new JMenuItem("Javadocs");
        javadocs.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                launchBrowserThatWorks("https://www.codenameone.com/javadoc/");
            }
        });
        helpMenu.add(javadocs);

        JMenuItem how = new JMenuItem("How Do I?...");
        how.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                launchBrowserThatWorks("https://www.codenameone.com/how-do-i.html");
            }
        });
        helpMenu.add(how);

        JMenuItem forum = new JMenuItem("Community Forum");
        forum.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                launchBrowserThatWorks("https://www.codenameone.com/discussion-forum.html");
            }
        });
        helpMenu.add(forum);

        JMenuItem bserver = new JMenuItem("Build Server");
        bserver.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                launchBrowserThatWorks("https://cloud.codenameone.com/secure/index.html");
            }
        });
        helpMenu.addSeparator();
        helpMenu.add(bserver);
        helpMenu.addSeparator();

        JMenuItem about = new JMenuItem("About");
        about.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                final JDialog about;
                if(window !=null){
                    about = new JDialog(window);
                }else{
                    about = new JDialog();
                }
                JPanel panel = new JPanel();
                panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

                JPanel imagePanel = new JPanel();

                JLabel image = new JLabel(new javax.swing.ImageIcon(getClass().getResource("/CodenameOne_Small.png")));
                image.setHorizontalAlignment(SwingConstants.CENTER);
                imagePanel.add(image);

                panel.add(imagePanel);

                JPanel linkPanel = new JPanel();

                JButton link = new JButton();
                link.setText("<HTML>For more information, please <br>visit <FONT color=\"#000099\"><U>www.codenameone.com</U></FONT></HTML>");
                link.setHorizontalAlignment(SwingConstants.LEFT);
                link.setBorderPainted(false);
                link.setOpaque(false);
                link.setBackground(Color.WHITE);
                link.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                    launchBrowserThatWorks("https://www.codenameone.com");
                    }
                });
                linkPanel.add(link);
                panel.add(linkPanel);

                JPanel closePanel = new JPanel();
                JButton close = new JButton("close");
                close.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                      about.dispose();
                    }
                });
                closePanel.add(close);
                panel.add(closePanel);

                about.add(panel);
                about.pack();
                if(window != null){
                    about.setLocationRelativeTo(window);
                }
                about.setVisible(true);
            }
        });
        helpMenu.add(about);

        if (showMenu) {
            bar.add(simulatorMenu);
            bar.add(simulateMenu);
            bar.add(toolsMenu);
            bar.add(skinMenu);
            bar.add(helpMenu);
        }



        alwaysOnTopFlag.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent ie) {
                alwaysOnTop = !alwaysOnTop;
                Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
                pref.putBoolean("AlwaysOnTop", alwaysOnTop);
                window.setAlwaysOnTop(alwaysOnTop);
            }
        });

        ItemListener zoomListener = new ItemListener() {

            public void itemStateChanged(ItemEvent ie) {
                setScrollableSkin(!scrollableSkin);

                if (scrollableSkin) {
                    if (appFrame == null) {
                        frm.add(java.awt.BorderLayout.SOUTH, hSelector);
                        frm.add(java.awt.BorderLayout.EAST, vSelector);
                    } else {
                        canvas.getParent().add(java.awt.BorderLayout.SOUTH, hSelector);
                        canvas.getParent().add(java.awt.BorderLayout.EAST, vSelector);
                    }

                } else {

                    frm.remove(hSelector);
                    frm.remove(vSelector);
                }
                Container parent = canvas.getParent();
                parent.remove(canvas);
                if (scrollableSkin) {
                    canvas.setForcedSize(new java.awt.Dimension((int)(getSkin().getWidth() / retinaScale), (int)(getSkin().getHeight() / retinaScale)));
                } else {
                    int screenH = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getHeight();
                    int screenW = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getWidth();
                    float zoomY = getSkin().getHeight() > screenH ? screenH/(float)getSkin().getHeight() : 1f;
                    float zoomX = getSkin().getWidth() > screenW ? screenW/(float)getSkin().getWidth() : 1f;
                    float zoom = Math.min(zoomX, zoomY);
                    canvas.setForcedSize(new java.awt.Dimension((int)(getSkin().getWidth()  * zoom), (int)(getSkin().getHeight() * zoom)));
                    if (window != null) {
                        if (appFrame == null) {
                            window.setSize(new java.awt.Dimension((int) (getSkin().getWidth() * zoom), (int) (getSkin().getHeight() * zoom)));
                        }
                    }
                }
                parent.add(BorderLayout.CENTER, canvas);

                canvas.x = 0;
                canvas.y = 0;
                zoomLevel = 1;
                frm.invalidate();
                frm.pack();
                Display.getInstance().getCurrent().repaint();
                frm.repaint();
            }
        };

        zoomMenu.addItemListener(zoomListener);

        exit.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                exitApplication();
            }
        });
    }
    
    public static void resumeApp() {
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                Executor.startApp();
                instance.minimized = false;
            }
        });
        instance.canvas.setEnabled(true);
        pause.setText("Pause App");
    }
    
    File findScreenshotFile() {
        int counter = 1;
        File f = new File(System.getProperty("user.home"), "CodenameOne Screenshot " + counter + ".png");
        while (f.exists()) {
            counter++;
            f = new File(System.getProperty("user.home"), "CodenameOne Screenshot " + counter + ".png");
        }
        return f;
    }

    
    private String getCurrentSkinName() {
         Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
         String skin = pref.get("skin", DEFAULT_SKIN);
         while (skin.indexOf("/") >= 0) {
             skin = skin.substring(skin.indexOf("/")+1);
         }
         while (skin.indexOf("\\") >= 0) {
             skin = skin.substring(skin.indexOf("\\")+1);
         }
         return skin;
    }
    
    private JMenu createSkinsMenu(final JFrame frm, final JMenu menu) throws MalformedURLException {
        JMenu m;
        if (menu == null) {
            m = new JMenu("Skins");
            m.setDoubleBuffered(true);
        } else {
            m = menu;
            m.removeAll();
        }
        final JMenu skinMenu = m;
        Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
        String skinNames = pref.get("skins", DEFAULT_SKINS);
        if (skinNames != null) {
            if (skinNames.length() < DEFAULT_SKINS.length()) {
                skinNames = DEFAULT_SKINS;
            }
            ButtonGroup skinGroup = new ButtonGroup();
            StringTokenizer tkn = new StringTokenizer(skinNames, ";");
            while (tkn.hasMoreTokens()) {
                final String current = tkn.nextToken();
                String name = current;
                if (current.contains(":")) {
                    try {
                        URL u = new URL(current);
                        File f = new File(u.getFile());
                        if (!f.exists()) {
                            continue;
                        }
                        name = f.getName();

                    } catch (Exception e) {
                        continue;
                    }
                } else {
                    // remove the old builtin skins from the menu
                    if(current.startsWith("/") && !current.equals(DEFAULT_SKIN)) {
                        continue;
                    }
                }
                String d = System.getProperty("dskin");
                JRadioButtonMenuItem i = new JRadioButtonMenuItem(name, name.equals(pref.get("skin", d)));
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
                        Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
                        pref.putBoolean("desktopSkin", false);
                        String mainClass = System.getProperty("MainClass");
                        if (mainClass != null) {
                            pref.put("skin", current);
                            frm.dispose();
                            System.setProperty("reload.simulator", "true");
                        } else {
                            loadSkinFile(current, frm);
                            refreshSkin(frm);
                        }
                    }
                });
                skinGroup.add(i);
                skinMenu.add(i);
            }
        }
        JMenuItem dSkin = new JMenuItem("Desktop.skin");
        
        dSkin.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                if (netMonitor != null) {
                    netMonitor.dispose();
                    netMonitor = null;
                }
                if (perfMonitor != null) {
                    perfMonitor.dispose();
                    perfMonitor = null;
                }
                Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
                pref.putBoolean("desktopSkin", true);
                pref.putBoolean("uwpDesktopSkin", false);
                String mainClass = System.getProperty("MainClass");
                if (mainClass != null) {
                    deinitializeSync();
                    frm.dispose();
                    System.setProperty("reload.simulator", "true");
                } 
            }
        });
        JMenuItem uwpSkin = new JMenuItem("UWP Desktop.skin");
        uwpSkin.setToolTipText("Windows 10 Desktop Skin");
        uwpSkin.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                if (netMonitor != null) {
                    netMonitor.dispose();
                    netMonitor = null;
                }
                if (perfMonitor != null) {
                    perfMonitor.dispose();
                    perfMonitor = null;
                }
                Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
                pref.putBoolean("desktopSkin", true);
                pref.putBoolean("uwpDesktopSkin", true);
                String mainClass = System.getProperty("MainClass");
                if (mainClass != null) {
                    deinitializeSync();
                    frm.dispose();
                    System.setProperty("reload.simulator", "true");
                } 
            }
        });
        skinMenu.addSeparator();
        skinMenu.add(dSkin);
        skinMenu.add(uwpSkin);
        
        skinMenu.addSeparator();
        JMenuItem more = new JMenuItem("More...");
        skinMenu.add(more);
        more.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                
                final JDialog pleaseWait = new JDialog(frm, false);
                pleaseWait.setLocationRelativeTo(frm);
                pleaseWait.setTitle("Message");
                pleaseWait.setLayout(new BorderLayout());
                pleaseWait.add(new JLabel("  Please Wait...  "), BorderLayout.CENTER);
                pleaseWait.pack();
                pleaseWait.setVisible(true);
                
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        
                        final JDialog d = new JDialog(frm, true);
                        d.setLocationRelativeTo(frm);
                        d.setTitle("Skins");
                        d.getContentPane().setLayout(new BorderLayout());
                        String userDir = System.getProperty("user.home");
                        final File skinDir = new File(userDir + "/.codenameone/");
                        if (!skinDir.exists()) {
                            skinDir.mkdir();
                        }

                        Vector data = new Vector();
                        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                        final Document[] doc = new Document[1];
                        try {
                            DocumentBuilder db = dbf.newDocumentBuilder();

                            Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);

                            InputStream is = openSkinsURL();
                            doc[0] = db.parse(is);
                            NodeList skins = doc[0].getElementsByTagName("Skin");

                            for (int i = 0; i < skins.getLength(); i++) {
                                Node skin = skins.item(i);
                                NamedNodeMap attr = skin.getAttributes();
                                String url = attr.getNamedItem("url").getNodeValue();
                                int ver = 0;
                                Node n = attr.getNamedItem("version");
                                if(n != null){
                                    ver = Integer.parseInt(n.getNodeValue());
                                }
                                boolean exists = new File(skinDir.getAbsolutePath() + url).exists();
                                if (!(exists) || Integer.parseInt(pref.get(url, "0")) < ver) { 
                                    Vector row = new Vector();
                                    row.add(new Boolean(false));
                                    row.add(new ImageIcon(new URL(defaultCodenameOneComProtocol + "://www.codenameone.com/OTA" + attr.getNamedItem("icon").getNodeValue())));
                                    row.add(attr.getNamedItem("name").getNodeValue());
                                    if(exists){
                                        row.add("Update");                                                        
                                    }else{
                                        row.add("New");                            
                                    }
                                    data.add(row);
                                }
                            }

                        } catch (Exception ex) {
                            Logger.getLogger(JavaSEPort.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        if (data.size() == 0) {
                            pleaseWait.setVisible(false);
                            JOptionPane.showMessageDialog(frm, "No New Skins to Install");
                            return;
                        }

                        Vector cols = new Vector();
                        cols.add("Install");
                        cols.add("Icon");
                        cols.add("Name");
                        cols.add("");

                        final DefaultTableModel tableModel = new DefaultTableModel(data, cols) {

                            @Override
                            public boolean isCellEditable(int row, int column) {
                                return column == 0;
                            }
                        };
                        JTable skinsTable = new JTable(tableModel) {

                            @Override
                            public Class<?> getColumnClass(int column) {
                                if (column == 0) {
                                    return Boolean.class;
                                }
                                if (column == 1) {
                                    return Icon.class;
                                }
                                return super.getColumnClass(column);
                            }
                        };
                        
                        
                        
                        skinsTable.setRowHeight(112);
                        skinsTable.getTableHeader().setReorderingAllowed(false);
                        final JTextField filter = new JTextField();
                        final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<DefaultTableModel>(((DefaultTableModel) skinsTable.getModel())); 
                        
                        filter.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                            
                            private void updateFilter() {
                                try {
                                    
                                   RowFilter rf = RowFilter.regexFilter("(?i)"+filter.getText(),2);
                                   sorter.setRowFilter(rf);
                                } catch (java.util.regex.PatternSyntaxException e) {
                                    return;
                                }
                                
                            }
                            
                            @Override
                            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                                updateFilter();
                            }

                            @Override
                            public void removeUpdate(DocumentEvent e) {
                                updateFilter();
                            }

                            @Override
                            public void changedUpdate(DocumentEvent e) {
                                updateFilter();
                            }
                            
                        });
                        skinsTable.setRowSorter(sorter);
                        d.getContentPane().add(filter, BorderLayout.NORTH);
                        
                        
                        d.getContentPane().add(new JScrollPane(skinsTable), BorderLayout.CENTER);
                        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
                        JButton download = new JButton("Download");
                        download.addActionListener(new ActionListener() {

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                final Vector toDowload = new Vector();

                                NodeList skins = doc[0].getElementsByTagName("Skin");
                                for (int i = 0; i < tableModel.getRowCount(); i++) {
                                    if (((Boolean) tableModel.getValueAt(i, 0)).booleanValue()) {
                                        Node skin;
                                        for (int j = 0; j < skins.getLength(); j++) {
                                            skin = skins.item(j);
                                            NamedNodeMap attr = skin.getAttributes();
                                            if(attr.getNamedItem("name").getNodeValue().equals(tableModel.getValueAt(i, 2))){
                                                String url = attr.getNamedItem("url").getNodeValue();
                                                String [] data = new String[2];
                                                data[0] = defaultCodenameOneComProtocol + "://www.codenameone.com/OTA" + url;
                                                data[1] = attr.getNamedItem("version").getNodeValue();                                        
                                                toDowload.add(data);
                                                break;
                                            }                                    
                                        }
                                    }
                                }

                                if (toDowload.size() > 0) {
                                    final JDialog downloadMessage = new JDialog(d, true);
                                    downloadMessage.setTitle("Downloading");
                                    downloadMessage.setLayout(new FlowLayout());
                                    downloadMessage.setLocationRelativeTo(d);
                                    final JLabel details = new JLabel("<br><br>Details");
                                    downloadMessage.add(details);
                                    final JLabel progress = new JLabel("Progress<br><br>");
                                    downloadMessage.add(progress);
                                    new Thread() {

                                        @Override
                                        public void run() {
                                            for (Iterator it = toDowload.iterator(); it.hasNext();) {
                                                String [] data = (String []) it.next();
                                                String url = data[0];
                                                details.setText(url.substring(url.lastIndexOf("/")));
                                                details.repaint();
                                                progress.setText("");
                                                progress.repaint();

                                                try {
                                                    File skin = downloadSkin(skinDir, url, data[1], progress);
                                                    if (skin.exists()) {
                                                        addSkinName(skin.toURI().toString());
                                                    }
                                                } catch (Exception e) {
                                                }
                                            }
                                            downloadMessage.setVisible(false);
                                            d.setVisible(false);
                                            try {
                                                createSkinsMenu(frm, skinMenu);
                                            } catch (MalformedURLException ex) {
                                                Logger.getLogger(JavaSEPort.class.getName()).log(Level.SEVERE, null, ex);
                                            }
                                        }
                                    }.start();
                                    downloadMessage.pack();
                                    downloadMessage.setSize(200, 70);
                                    downloadMessage.setVisible(true);
                                }else{
                                    JOptionPane.showMessageDialog(d, "Choose a Skin to Download");
                                }
                            }
                        });
                        p.add(download);
                        d.getContentPane().add(p, BorderLayout.SOUTH);
                        d.pack();
                        pleaseWait.dispose();
                        d.setVisible(true);
                        
                    }
                });


            }
        });

        skinMenu.addSeparator();
        JMenuItem addSkin = new JMenuItem("Add New...");
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

        skinMenu.addSeparator();
        JMenuItem reset = new JMenuItem("Reset Skins");
        skinMenu.add(reset);
        reset.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                    if(JOptionPane.showConfirmDialog(frm,
                            "Are you sure you want to reset skins to default?", 
                            "Clean Storage", 
                            JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION){
                        Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
                        pref.put("skins", DEFAULT_SKINS);
                        
                        String userDir = System.getProperty("user.home");
                        final File skinDir = new File(userDir + "/.codenameone/");
                        if(skinDir.exists()){
                            File [] childs = skinDir.listFiles();
                            for (int i = 0; i < childs.length; i++) {
                                File child = childs[i];
                                if(child.getName().endsWith(".skin")){
                                    child.delete();
                                }                                
                            }
                        }
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
                            pref.put("skin", DEFAULT_SKIN);
                            deinitializeSync();
                            frm.dispose();
                            System.setProperty("reload.simulator", "true");
                        } else {
                            loadSkinFile(DEFAULT_SKIN, frm);
                            refreshSkin(frm);
                        }
                        
                    }
                
            }
        });
        return skinMenu;
    }

    InputStream openSkinsURL() throws IOException {
        try {
            URL u = new URL(defaultCodenameOneComProtocol + "://www.codenameone.com/OTA/Skins.xml");
            HttpURLConnection uc = (HttpURLConnection)u.openConnection();
            uc.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; U; Android 2.2; en-us; Nexus One Build/FRF91) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1");
            InputStream is = uc.getInputStream();
            return is;
        } catch(IOException err) {
            if(defaultCodenameOneComProtocol.equals("https")) {
                defaultCodenameOneComProtocol = "http";
            } else {
                throw err;
            }
            System.out.println("Failed to connect thru secure socket, trying http instead");
            URL u = new URL("http://www.codenameone.com/OTA/Skins.xml");
            HttpURLConnection uc = (HttpURLConnection)u.openConnection();
            uc.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; U; Android 2.2; en-us; Nexus One Build/FRF91) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1");
            InputStream is = uc.getInputStream();
            return is;
        }
    }
    
    private void showTestRecorder() {
        if (testRecorder == null) {
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
            if (appFrame == null) {

                netMonitor.showInNewWindow();

            } else {
                AppPanel existing = appFrame.getAppPanelById("NetworkMonitor");
                if (existing == null) {
                    netMonitor = new NetworkMonitor();
                    existing = new AppPanel("NetworkMonitor", "Network Monitor", netMonitor);
                    existing.setPreferredFrame(AppFrame.FrameLocation.BottomPanel);
                    existing.setScrollable(false, true);
                    appFrame.add(existing);
                }
            }
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
        try {
            pref.flush();
        } catch(Throwable t) {
            t.printStackTrace();
        }
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

    private float zoomLevel() {
        float w1 = ((float) canvas.getWidth() * (float)retinaScale) / ((float) getSkin().getWidth());
        float h1 = ((float) canvas.getHeight() * (float)retinaScale) / ((float) getSkin().getHeight());
        return Math.min(h1, w1);
    }
    
    private void refreshSkin(final JFrame frm) {
        Display.getInstance().callSerially(new Runnable() {

            public void run() {

                zoomLevel = zoomLevel();
                Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_DEFAULT);
                deepRevaliate(Display.getInstance().getCurrent());

                if (hasNativeTheme()) {
                    Display.getInstance().installNativeTheme();
                }
                Display.getInstance().getCurrent().refreshTheme();
                deepRevaliate(Display.getInstance().getCurrent());
                JavaSEPort.this.sizeChanged(getScreenCoordinates().width, getScreenCoordinates().height);
                Display.getInstance().getCurrent().revalidate();
                canvas.setForcedSize(new java.awt.Dimension((int)(getSkin().getWidth()  * zoomLevel), (int)(getSkin().getHeight() * zoomLevel)));
                zoomLevel = 1;
                frm.pack();
            }
        });
    }


    private ArrayList<Runnable> deinitializeHooks = new ArrayList<>();
    public void addDeinitializeHook(Runnable r) {
        deinitializeHooks.add(r);
    }

    public void removeDeinitializeHook(Runnable r) {
        deinitializeHooks.remove(r);
    }

    public void deinitializeSync() {
        final Thread[] t = new Thread[1];
        final boolean[] finished = new boolean[1];
        Display.getInstance().callSeriallyAndWait(new Runnable() {

            @Override
            public void run() {
                try {
                    t[0] = Thread.currentThread();

                    Form currForm = CN.getCurrentForm();
                    if (currForm != null) {
                        // Change to a dummy form to allow the current form to run its shutdown hooks.
                        Form dummy = new Form();
                        dummy.setTransitionInAnimator(null);
                        dummy.setTransitionOutAnimator(null);
                        currForm.setTransitionInAnimator(null);
                        currForm.setTransitionOutAnimator(null);
                        dummy.show();
                    }

                    ArrayList<Runnable> toDeinitialize = new ArrayList<Runnable>(deinitializeHooks);
                    deinitializeHooks.clear();
                    for (Runnable r : toDeinitialize) {
                        r.run();
                    }
                } finally {
                    finished[0] = true;
                }


            }
        }, 250);

        Display.deinitialize();
        if (netMonitor != null) {
            netMonitor.dispose();
            netMonitor = null;
        }
        NetworkManager.getInstance().shutdownSync();
        try {

            if (t[0] != null) {
                long maxWait = 5000L;
                long startTime = System.currentTimeMillis();
                while (!finished[0] && (System.currentTimeMillis() - maxWait < startTime)) {
                    Thread.sleep(100);
                }
                //t[0].join();
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
            if (f.contains("://") || f.startsWith("file:")) {

                try {
                    // load Via URL loading
                    loadSkinFile(new URL(f).openStream(), frm);
                } catch (FileNotFoundException ex) {
                    String d = System.getProperty("dskin");
                    loadSkinFile(d, frm);
                    return;
                } catch (MalformedURLException ex) {
                    loadSkinFile(getResourceAsStream(getClass(), DEFAULT_SKIN), frm);
                    
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } else {
                InputStream is = getResourceAsStream(getClass(), f);
                if(is != null) {
                    loadSkinFile(is, frm);
                } else {
                    loadSkinFile(getResourceAsStream(getClass(), DEFAULT_SKIN), frm);
                }
            }
            Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
            pref.put("skin", f);
            addSkinName(f);
        } catch (Throwable t) {
            System.out.println("Failed loading the skin file: " + f);
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

/*        File updater = new File(System.getProperty("user.home") + File.separator + ".codenameone" + File.separator + "UpdateCodenameOne.jar");
        if(!updater.exists()) {
            System.out.println("******************************************************************************");
            System.out.println("* It seems that you are using an old plugin version please upate to the latest plugin and invoke Codename One -> Codename One Settings -> Basic -> Update Client Libs");
            System.out.println("******************************************************************************");
        }*/
                
        Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
        boolean desktopSkin = pref.getBoolean("desktopSkin", false);
        if (desktopSkin && m == null) {
            safeAreaLandscape = null;
            safeAreaPortrait = null;
            Toolkit tk = Toolkit.getDefaultToolkit();
            setDefaultPixelMilliRatio(tk.getScreenResolution() / 25.4 * getRetinaScale());
            pixelMilliRatio = getDefaultPixelMilliRatio();
            JPanel panel = new javax.swing.JPanel();  
            panel.setLayout(new BorderLayout());
            JPanel bottom = new javax.swing.JPanel(); 
            panel.setOpaque(false);
            bottom.setLayout(new FlowLayout(FlowLayout.RIGHT));
            widthLabel = new JLabel("Width:   ");
            heightLabel = new JLabel(" Height:   ");
            bottom.add(widthLabel);
            bottom.add(heightLabel);
            panel.add(bottom, BorderLayout.SOUTH);
            
            JFrame frame = new JFrame();
            //frame.addWindowListener(new WindowListener() {
            //    
            //});
            frame.setLayout(new BorderLayout());
            frame.add(panel, BorderLayout.CENTER);
            frame.setSize(new Dimension(300, 400));
            m = panel;
            window = frame;
            if (pref.getBoolean("uwpDesktopSkin", false)) {
                setNativeTheme("/winTheme.res");
            } else {
                setNativeTheme("/iOS7Theme.res");
            }
        }
        setInvokePointerHover(desktopSkin || invokePointerHover);
        
        // this is essential for push and other things to work in the simulator
        Preferences p = Preferences.userNodeForPackage(com.codename1.ui.Component.class);
        String user = p.get("user", null);
        if(user != null) {
            Display d = Display.getInstance();
            d.setProperty("built_by_user", user);
            String mainClass = System.getProperty("MainClass");
            if (mainClass != null) {
                mainClass = mainClass.substring(0, mainClass.lastIndexOf('.'));
                d.setProperty("package_name", mainClass);
            }
        }
        


        String hide = System.getProperty("hideMenu", "false");
        if (hide != null && hide.equals("true")) {
            showMenu = false;
        }

        URLConnection.setDefaultAllowUserInteraction(true);
        HttpURLConnection.setFollowRedirects(false);

        final ArrayList<Runnable> delayedTasks = new ArrayList<Runnable>();
        Timer delayedTasksTimer = new Timer();
        TimerTask delayedTimerTask = new TimerTask() {
            @Override
            public void run() {

                while (!delayedTasks.isEmpty()) {
                    EventQueue.invokeLater(delayedTasks.remove(0));
                }
            }
        };
        delayedTasksTimer.schedule(delayedTimerTask, 1000L);

        if (!blockMonitors && pref.getBoolean("NetworkMonitor", false)) {

            delayedTasks.add(new Runnable() {
                public void run() {
                    showNetworkMonitor();
                }

            });
        }
        if (!blockMonitors && pref.getBoolean("PushSimulator", false)) {
            pushSimulation = new PushSimulator();
            pushSimulation.setVisible(true);
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

        if (hasSkins()) {

            hSelector = new JScrollBar(Scrollbar.HORIZONTAL);
            vSelector = new JScrollBar(Scrollbar.VERTICAL);
            hSelector.addAdjustmentListener(canvas);
            vSelector.addAdjustmentListener(canvas);
        }
        if (hasSkins() && useAppFrame) {
            appFrame = new AppFrame("Simulator") {
                @Override
                protected void decoratePanelWindow(AppPanel panel, Window window) {
                    try {
                        Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
                        boolean desktopSkin = pref.getBoolean("desktopSkin", false);
                        installMenu((JFrame) window, desktopSkin);
                    } catch (Exception ex) {
                        throw new RuntimeException("Failed to decorate panel window in app frame.", ex);
                    }
                }
            };
            JPanel canvasWrapper = new JPanel();
            canvasWrapper.setLayout(new BorderLayout());
            canvasWrapper.add(canvas, java.awt.BorderLayout.CENTER);
            canvasWrapper.setPreferredSize(new Dimension(640, 640));
            scrollableSkin = pref.getBoolean("Scrollable", scrollableSkin);
            if (scrollableSkin) {
                canvasWrapper.add(hSelector, java.awt.BorderLayout.SOUTH);
                canvasWrapper.add(vSelector, java.awt.BorderLayout.EAST);
            }


            componentTreeInspector = new ComponentTreeInspector();
            AppPanel componentTreeInspectorPanel = new AppPanel("Components", "Components", componentTreeInspector.removeComponentTree());
            componentTreeInspectorPanel.setPreferredFrame(AppFrame.FrameLocation.LeftPanel);
            componentTreeInspectorPanel.addAction(componentTreeInspector.new RefreshAction());
            componentTreeInspectorPanel.addAction(componentTreeInspector.new ValidateAction());
            componentTreeInspectorPanel.addAction(componentTreeInspector.new ToggleInspectSimulatorAction());
            AppPanel canvasPanel = new AppPanel("Simulator", "Simulator", canvasWrapper);
            canvasPanel.setPreferredFrame(AppFrame.FrameLocation.CenterPanel);
            RotateAction portraitAction = new RotateAction(true);
            RotateAction landscapeAction = new RotateAction(false);
            canvasPanel.addAction(portraitAction);
            canvasPanel.addAction(landscapeAction);
            ZoomAction zoomIn = new ZoomAction(true);
            ZoomAction zoomOut = new ZoomAction(false);
            canvasPanel.addAction(zoomIn);
            canvasPanel.addAction(zoomOut);
            canvasPanel.addAction(new SeparatorAction());
            canvasPanel.addAction(new ScreenshotAction());
            canvasPanel.addAction(new ScreenshotSettingsAction());

            appFrame.registerUpdateCallback(zoomIn);
            appFrame.registerUpdateCallback(zoomOut);
            appFrame.registerUpdateCallback(portraitAction);
            appFrame.registerUpdateCallback(landscapeAction);

            AppPanel detailsPanel = new AppPanel("Details", "Component Details", componentTreeInspector);
            detailsPanel.setPreferredFrame(AppFrame.FrameLocation.BottomPanel);
            detailsPanel.setScrollable(false, true);

            AppPanel propertiesPanel = new AppPanel("Properties", "Properties", componentTreeInspector.getPropertyDetailsPanel());
            propertiesPanel.setPreferredFrame(AppFrame.FrameLocation.RightPanel);
            propertiesPanel.setScrollable(false, false);
            propertiesPanel.addAction(new JavaSEPort.OpenJavadocsAction());
            appFrame.add(propertiesPanel);
            appFrame.add(detailsPanel);
            appFrame.add(canvasPanel);
            appFrame.add(componentTreeInspectorPanel);



        }

        if (m != null && m instanceof java.awt.Container) {
            java.awt.Container cnt = (java.awt.Container) m;
            java.awt.Component mainContents = appFrame == null ? canvas : appFrame;
            if (cnt.getLayout() instanceof java.awt.BorderLayout) {
                cnt.add(java.awt.BorderLayout.CENTER, mainContents);
            } else {
                cnt.add(mainContents);
            }
        } else {
            window = new JFrame();

            window.setLayout(new java.awt.BorderLayout());
            if (appFrame == null) {

                scrollableSkin = pref.getBoolean("Scrollable", scrollableSkin);
                if (scrollableSkin) {
                    window.add(hSelector, java.awt.BorderLayout.SOUTH);
                    window.add(vSelector, java.awt.BorderLayout.EAST);
                }
            }
            java.awt.Component mainContents = appFrame == null ? canvas : appFrame;
            window.add(mainContents, java.awt.BorderLayout.CENTER);
            if (appFrame != null) {
                window.addComponentListener(new ComponentAdapter() {
                    @Override
                    public void componentResized(ComponentEvent e) {
                        appFrame.setPreferredSize(new Dimension(window.getContentPane().getSize()));
                        appFrame.setSize(new Dimension(window.getContentPane().getSize()));
                        appFrame.revalidate();
                    }
                });
            }
        }
        if (findTopFrame() != null && retinaScale > 1.0) {
            findTopFrame().setGlassPane(new CN1GlassPane());
            findTopFrame().getGlassPane().setVisible(true);
        }
        if(window != null){
            
            java.awt.Image large = Toolkit.getDefaultToolkit().createImage(getClass().getResource("/application64.png"));
            java.awt.Image small = Toolkit.getDefaultToolkit().createImage(getClass().getResource("/application48.png"));
            try {
                // setIconImages is only available in JDK 1.6
                window.setIconImages(Arrays.asList(new java.awt.Image[] {large, small}));
            } catch (Throwable err) {
                window.setIconImage(small);
            }
            
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
            window.addComponentListener(new ComponentAdapter() {
                
                private void saveBounds(ComponentEvent e) {
                    if (e.getComponent() instanceof JFrame) {
                        Frame f = (JFrame)e.getComponent();
                        if (f.getExtendedState() == JFrame.NORMAL) {
                            Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
                            Rectangle bounds = f.getBounds();
                            pref.put("window.bounds", bounds.x+","+bounds.y+","+bounds.width+","+bounds.height);
                        }
                    }
                }
                
                @Override
                public void componentResized(ComponentEvent e) {
                    saveBounds(e);
                }

                @Override
                public void componentMoved(ComponentEvent e) {
                    saveBounds(e);
                }
                
                
                
            });
            window.setLocationByPlatform(true);

            android6PermissionsFlag = pref.getBoolean("Android6Permissions", false);
            
            alwaysOnTop = pref.getBoolean("AlwaysOnTop", false);
            if (appFrame == null) window.setAlwaysOnTop(alwaysOnTop);
            
            String reset = System.getProperty("resetSkins");
            if(reset != null && reset.equals("true")){
                System.setProperty("resetSkins", "");
                pref = Preferences.userNodeForPackage(JavaSEPort.class);
                pref.put("skins", DEFAULT_SKINS);
           }
            
            if (hasSkins()) {
                if(m == null){
                    String f = System.getProperty("skin");
                    if (f != null) {
                        loadSkinFile(f, window);
                    } else {
                        String d = System.getProperty("dskin");
                        f = pref.get("skin", d);
                        loadSkinFile(f, window);
                    }
                }else{
                    try{
                        installMenu(window, true);
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
            } else {
                Resources.setRuntimeMultiImageEnabled(true);
                
                window.setUndecorated(true);
                window.setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
            window.pack();
            if (getSkin() != null && !scrollableSkin) {
                zoomLevel = zoomLevel();
            }

            setPortrait(pref.getBoolean("Portrait", true));
            
            if (getSkin() != null) {
                if (scrollableSkin) {
                    canvas.setForcedSize(new java.awt.Dimension((int)(getSkin().getWidth() / retinaScale), (int)(getSkin().getHeight() / retinaScale)));
                    if (window != null) {
                        int screenH = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getHeight();
                        int screenW = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getWidth();
                        float zoomY = getSkin().getHeight() > screenH ? screenH/(float)getSkin().getHeight() : 1f;
                        float zoomX = getSkin().getWidth() > screenW ? screenW/(float)getSkin().getWidth() : 1f;
                        float zoom = Math.min(1,Math.min(zoomX, zoomY));
                        window.setSize(new java.awt.Dimension((int)(getSkin().getWidth() * retinaScale * zoom), (int)(getSkin().getHeight() * retinaScale * zoom)));
                    }
                } else {
                    int screenH = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getHeight();
                    int screenW = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getWidth();
                    float zoomY = getSkin().getHeight() > screenH ? screenH/(float)getSkin().getHeight() : 1f;
                    float zoomX = getSkin().getWidth() > screenW ? screenW/(float)getSkin().getWidth() : 1f;
                    float zoom = Math.min(1, Math.min(zoomX, zoomY));
                    canvas.setForcedSize(new java.awt.Dimension((int)(getSkin().getWidth()  * zoom), (int)(getSkin().getHeight() * zoom)));
                    if (window != null) {
                        window.setSize(new java.awt.Dimension((int)(getSkin().getWidth() * zoom), (int)(getSkin().getHeight() * zoom)));
                    }
                }
            }
            String lastBounds = pref.get("window.bounds", null);
            if (lastBounds != null) {
                String[] parts = lastBounds.split(",");
                Rectangle r = new Rectangle(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
                Rectangle bounds = new Rectangle(0, 0, 0, 0);
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                GraphicsDevice lstGDs[] = ge.getScreenDevices();
                for (GraphicsDevice gd : lstGDs) {
                    bounds.add(gd.getDefaultConfiguration().getBounds());
                }
                
                if (bounds.intersects(r)) {
                
                    window.setBounds(r);
                }
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
    
    protected void sizeChanged(int w, int h) {
        try{
            super.sizeChanged(w, h);
            if(widthLabel != null){
                widthLabel.setText("Width: " + w);
                heightLabel.setText("Height: " + h);
                widthLabel.getParent().revalidate();
                canvas.blit();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * @inheritDoc
     */
    public void vibrate(int duration) {
        System.out.println("vibrate(" + duration + ")");
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
            return getScreenCoordinates().width ;
        }
        int w = (int)(canvas.getWidth() * retinaScale);
        if (w < 10 && canvas.getParent() != null) {
            return (int)(canvas.getParent().getWidth() * retinaScale);
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
        int h = (int)(canvas.getHeight() * retinaScale);
        if (h < 10 && canvas.getParent() != null) {
            return (int)(canvas.getParent().getHeight() * retinaScale);
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

    @Override
    public boolean isSimulator() {
        // differentiate simulator from JavaSE port and detect designer
        return designMode || getSkin() != null || widthLabel != null;
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

    @Override
    public boolean nativeEditorPaintsHint() {
        return false;
    }
    
    
    
    public void editStringLegacy(final Component cmp, int maxSize, int constraint, String text, int keyCode) {
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
            ((com.codename1.ui.TextField) cmp).setText(getText(tf));
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
                if (testRecorder != null) {
                    testRecorder.editTextFieldCompleted(cmp, txt);
                }
                Display.getInstance().onEditingComplete(cmp, txt);
                if (tf instanceof java.awt.TextField) {
                    ((java.awt.TextField) tf).removeActionListener(this);
                }
                ((TextComponent) tf).removeTextListener(this);
                tf.removeFocusListener(this);
                canvas.remove(tf);
                synchronized (this) {
                    notify();
                }
                canvas.repaint();
            }

            public void focusGained(FocusEvent e) {
                setCaretPosition(tf, getText(tf).length());
            }

            public void focusLost(FocusEvent e) {
                actionPerformed(null);
            }

            public void keyTyped(KeyEvent e) {
                String t = getText(tf);

                if (t.length() >= ((TextArea) cmp).getMaxSize()) {
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
                if (cmp instanceof com.codename1.ui.TextField) {
                    ((com.codename1.ui.TextField) cmp).setText(getText(tf));
                }

            }
        };
        final Listener l = new Listener();
        if (tf instanceof java.awt.TextField) {
            ((java.awt.TextField) tf).addActionListener(l);
        }
        ((TextComponent) tf).addTextListener(l);


        tf.addKeyListener(l);
        tf.addFocusListener(l);
        if(simulateAndroidKeyboard) {
            java.util.Timer t = new java.util.Timer();
            TimerTask tt = new TimerTask() {
                @Override
                public void run() {
                    if(!Display.getInstance().isEdt()) {
                        Display.getInstance().callSerially(this);
                        return;
                    }
                    if(tf.getParent() != null) {
                        final int height = getScreenCoordinates().height;
                        JavaSEPort.this.sizeChanged(getScreenCoordinates().width, height / 2);
                        new UITimer(new Runnable() {
                            public void run() {
                                if(tf.getParent() != null) {
                                    new UITimer(this).schedule(100, false, Display.getInstance().getCurrent());
                                } else {
                                    JavaSEPort.this.sizeChanged(getScreenCoordinates().width, height);
                                }
                            }
                        }).schedule(100, false, Display.getInstance().getCurrent());
                    }
                }
            };
            t.schedule(tt, 300);
        }
        Display.getInstance().invokeAndBlock(l);
    }

    @Override
    public void stopTextEditing() {
        if (textCmp != null && textCmp.getParent() != null) {
            canvas.remove(textCmp);
        }
    }

    @Override
    public boolean usesInvokeAndBlockForEditString() {
        return false;
    }

    @Override
    public boolean isAsyncEditMode() {
        return true;
    }

    
    
    
    private interface EditingInProgress {
        void invokeAfter(Runnable r);
        void endEditing();
    }
    
    private EditingInProgress editingInProgress;
    private Component currentlyEditingField;
    
    private Process tabTipProcess;
    
    /**
     * @inheritDoc
     */
    public void editString(final Component cmp, final int maxSize, final int constraint, String text, final int keyCode) {
        if(scrollWheeling) {
            return;
        }
        if(System.getProperty("TextCompatMode") != null) {
            editStringLegacy(cmp, maxSize, constraint, text, keyCode);
            return;
        }
        if (editingInProgress != null) {
            final String fText = text;
            editingInProgress.invokeAfter(new Runnable() {
                public void run() {
                    CN.callSerially(new Runnable() {
                        public void run() {
                            editString(cmp, maxSize, constraint, fText, keyCode);
                        }
                    });
                    
                }
            });
            editingInProgress.endEditing();
            return;
        }
        //a workaround to fix an issue where the previous Text Component wasn't removed properly. 
        //java.awt.Component [] cmps = canvas.getComponents();
        //for (int i = 0; i < cmps.length; i++) {
        //    java.awt.Component cmp1 = cmps[i];
        //    if(cmp1 instanceof JScrollPane || cmp1 instanceof javax.swing.text.JTextComponent){
        //        canvas.remove(cmp1);
        //    }
        //}
        
        checkEDT();
        
        class Repainter {
            JComponent jcmp;
            javax.swing.border.Border origBorder;
            
            Repainter(JComponent jcmp) {
                this.jcmp = jcmp;
            }
            void repaint(long tm, int x, int y, int width, int height) {
                boolean oldShowEdtWarnings = showEDTWarnings;
                showEDTWarnings = false;
                int marginTop = 0;//cmp.getSelectedStyle().getPadding(Component.TOP);
                int marginLeft = 0;//cmp.getSelectedStyle().getPadding(Component.LEFT);
                int marginRight = 0;//cmp.getSelectedStyle().getPadding(Component.RIGHT);
                int marginBottom = 0;//cmp.getSelectedStyle().getPadding(Component.BOTTOM);
                int paddingTop = Math.round(cmp.getSelectedStyle().getPadding(Component.TOP) * zoomLevel);
                int paddingLeft = Math.round(cmp.getSelectedStyle().getPadding(Component.LEFT) * zoomLevel);
                int paddingRight = Math.round(cmp.getSelectedStyle().getPadding(Component.RIGHT) * zoomLevel);
                int paddingBottom = Math.round(cmp.getSelectedStyle().getPadding(Component.BOTTOM) * zoomLevel);
                Rectangle bounds;
                if (getSkin() != null) {
                    bounds = new Rectangle((int) ((cmp.getAbsoluteX() + cmp.getScrollX() + getScreenCoordinates().x + canvas.x + marginLeft) * zoomLevel),
                            (int) ((cmp.getAbsoluteY() + cmp.getScrollY() + getScreenCoordinates().y + canvas.y + marginTop) * zoomLevel),
                            (int) ((cmp.getWidth() - marginLeft - marginRight) * zoomLevel),
                            (int) ((cmp.getHeight() - marginTop - marginBottom) * zoomLevel));

                } else {
                    bounds = new Rectangle(cmp.getAbsoluteX() + cmp.getScrollX() + marginLeft, cmp.getAbsoluteY() + cmp.getScrollY() + marginTop, cmp.getWidth() - marginRight - marginLeft, cmp.getHeight() - marginTop - marginBottom);
                }
                if (!jcmp.getBounds().equals(bounds)) {
                    jcmp.setBounds(bounds);
                    if (origBorder == null) {
                        origBorder = jcmp.getBorder();
                    }
                    //jcmp.setBorder(BorderFactory.createCompoundBorder(
                    //    origBorder, 
                    //    BorderFactory.createEmptyBorder(paddingTop, paddingLeft, paddingBottom, paddingRight))
                    //);
                    jcmp.setBorder( BorderFactory.createEmptyBorder(paddingTop, paddingLeft, paddingBottom, paddingRight));
                }

                showEDTWarnings = oldShowEdtWarnings;
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        cmp.repaint();
                    }
                });
            }
        }
        
        javax.swing.text.JTextComponent swingT;
        if (((com.codename1.ui.TextArea)cmp).isSingleLineTextArea()) {
            JTextComponent t;
            if((constraint & TextArea.PASSWORD) == TextArea.PASSWORD) {
                t = new JPasswordField() {
                    Repainter repainter = new Repainter(this);
                    @Override
                    public void repaint(long tm, int x, int y, int width, int height) {
                        if (repainter != null) {
                            repainter.repaint(tm, x, y, width, height);
                        }
                    }
                };
            } else {
                t = new JTextField() {
                    Repainter repainter = new Repainter(this);
                    @Override
                    public void repaint(long tm, int x, int y, int width, int height) {
                        if (repainter != null) {
                            repainter.repaint(tm, x, y, width, height);
                        }
                    }
                };
                
                /*
                ((JTextField)t).addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (cmp instanceof com.codename1.ui.TextField) {
                            final com.codename1.ui.TextField tf = (com.codename1.ui.TextField)cmp;
                            if (tf.getDoneListener() != null) {
                                Display.getInstance().callSerially(new Runnable() {
                                    public void run() {
                                        if (tf.getDoneListener() != null) {
                                            tf.fireDoneEvent();
                                        }
                                    }
                                });
                            }
                        }
                    }
                    
                });
                */
            }
            swingT = t;
            textCmp = swingT;
        } else {

            // Forward references so that we can access the scroll pane and its
            // repainter from inside the JTextArea.
            final Repainter[] fRepainter = new Repainter[1];
            final JScrollPane[] fPane = new JScrollPane[1];

            final com.codename1.ui.TextArea ta = (com.codename1.ui.TextArea)cmp;
            JTextArea t = new JTextArea() {
                @Override

                public void repaint(long tm, int x, int y, int width, int height) {
                    // We need to catch JTextArea repaints in addition to the
                    // JScrollPane repaints because the ScrollPane doesn't seem to repaint
                    // enough.
                    if (fRepainter[0] != null && fPane[0] != null) {
                        Point p = SwingUtilities.convertPoint(this, x, y, fPane[0]);
                        fRepainter[0].repaint(tm, p.x, p.y, width, height);
                    }
                }
            };
            t.setWrapStyleWord(true);
            t.setLineWrap(true);
            swingT = t;
            JScrollPane pane = new JScrollPane(swingT){
                
                Repainter repainter = new Repainter(this);
                {
                    fRepainter[0] = repainter;
                }
                
                @Override
                public void repaint(long tm, int x, int y, int width, int height) {

                    
                    if (repainter != null) {
                        repainter.repaint(tm, x, y, width, height); 
                    }
                }

            };
            fPane[0] = pane;

            if (ta.isGrowByContent()) {
                pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            }
            pane.setBorder(null);
            pane.setOpaque(false);
            pane.getViewport().setOpaque(false);
            // Without these scrollbars, it seems terribly difficult
            // to work with TextAreas that contain more text than can fit.
            // Commenting these out for better usability - at least on OS X.
            //pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
            //pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            
            
            textCmp = pane;
        }
        if (cmp.isRTL()) {
            textCmp.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        }
        DefaultCaret caret = (DefaultCaret) swingT.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);            
        swingT.setFocusTraversalKeysEnabled(false);
        TextEditUtil.setCurrentEditComponent(cmp);
        final javax.swing.text.JTextComponent txt = swingT;
        txt.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_TAB) {
                    if (e.isShiftDown()) {
                        TextEditUtil.editPrevTextArea();
                    } else {
                        TextEditUtil.editNextTextArea();
                    }
                }
            }
        });
        //swingT.setBorder(null);
        swingT.setOpaque(false);
        swingT.setForeground(new Color(cmp.getUnselectedStyle().getFgColor()));
        swingT.setCaretColor(new Color(cmp.getUnselectedStyle().getFgColor()));
        final javax.swing.text.JTextComponent tf = swingT;
        if (keyCode > 0) {
            text += ((char) keyCode);
            setText(tf, text);
            setCaretPosition(tf, text.length());
            if(cmp instanceof com.codename1.ui.TextField) {
                ((com.codename1.ui.TextField) cmp).setText(getText(tf));
            }
        } else {
            setText(tf, text);
        }
        textCmp.setBorder(null);
        textCmp.setOpaque(false);
                
        canvas.add(textCmp);
        int marginTop = cmp.getSelectedStyle().getPadding(Component.TOP);
        int marginLeft = cmp.getSelectedStyle().getPadding(Component.LEFT);
        int marginRight = cmp.getSelectedStyle().getPadding(Component.RIGHT);
        int marginBottom = cmp.getSelectedStyle().getPadding(Component.BOTTOM);
        if (getSkin() != null) {
            textCmp.setBounds((int) ((cmp.getAbsoluteX() + cmp.getScrollX() + getScreenCoordinates().x + canvas.x + marginLeft) * zoomLevel),
                    (int) ((cmp.getAbsoluteY() + cmp.getScrollY() + getScreenCoordinates().y + canvas.y + marginTop) * zoomLevel),
                    (int) ((cmp.getWidth() - marginLeft - marginRight) * zoomLevel), 
                    (int) ((cmp.getHeight() - marginTop - marginBottom)* zoomLevel));
            //System.out.println("Set bounds to "+textCmp.getBounds());
            java.awt.Font f = font(cmp.getStyle().getFont().getNativeFont());
            tf.setFont(f.deriveFont(f.getSize2D() * zoomLevel));  
        } else {
            textCmp.setBounds(cmp.getAbsoluteX() + cmp.getScrollX() + marginLeft, cmp.getAbsoluteY() + cmp.getScrollY() + marginTop, cmp.getWidth() - marginRight - marginLeft, cmp.getHeight() - marginTop - marginBottom);
            //System.out.println("Set bounds to "+textCmp.getBounds());
            tf.setFont(font(cmp.getStyle().getFont().getNativeFont()));
        }
        if (tf instanceof JPasswordField && tf.getFont() != null && tf.getFont().getFontName().contains("Roboto")) {
            java.awt.Font fallback = new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, tf.getFont().getSize());
            tf.setFont(fallback);
        }
        setCaretPosition(tf, getText(tf).length());
        
        
        // Windows Tablet Show Virtual Keyboard
        // REf https://stackoverflow.com/a/25783041/2935174
        final String sysroot = System.getenv("SystemRoot");
        String tabTipExe = "C:\\Program Files\\Common Files\\microsoft shared\\ink\\TabTip.exe";
        
        if(exposeFilesystem) {
            final boolean useTabTip = "tabtip".equalsIgnoreCase(Display.getInstance().getProperty("javase.win.vkb", "tabtip"));
            if (new File(tabTipExe).exists()) {
                try {

                    if (useTabTip) {
                        //System.out.println("Opening TabTip");
                        ProcessBuilder pb = new ProcessBuilder("cmd", "/c", tabTipExe);
                        tabTipProcess = pb.start();
                    } else {
                        //System.out.println("Opening OSK");
                        ProcessBuilder pb = new ProcessBuilder(sysroot + "/system32/osk.exe");
                        tabTipProcess = pb.start();
                    }
                } catch (Exception e) {
                    System.err.println("Failed to open VKB: " + e.getMessage());
                }

                tf.addFocusListener(new FocusListener() {
                    @Override
                    public void focusLost(FocusEvent arg0) {
                        //System.out.println("Lost focus...");
                        try {
                            if (tabTipProcess != null) {
                                tabTipProcess.destroy();
                            } 
                        } catch (Exception ex){}
                        try {
                            if (useTabTip) {
                                Runtime.getRuntime().exec("cmd /c taskkill /IM TabTip.exe");
                            } else {
                                Runtime.getRuntime().exec("cmd /c taskkill /IM osk.exe");
                            }
                        } catch (IOException e) {
                            System.err.println("Problem closing VKB: " + e.getMessage());
                        }
                    }

                    @Override
                    public void focusGained(FocusEvent arg0) {

                    }
                });
            }
        }
        
        tf.requestFocus();
        tf.setSelectionStart(0);
        tf.setSelectionEnd(0);


        class Listener implements ActionListener, FocusListener, KeyListener, TextListener, Runnable, DocumentListener, EditingInProgress {
            private final JTextComponent textCmp;
            private final JComponent swingComponentToRemove;
            private boolean performed;
            private boolean fireDone;
            
            Listener(JTextComponent textCmp, JComponent swingComponentToRemove) {
                this.textCmp = textCmp;
                this.swingComponentToRemove = swingComponentToRemove;
                if (textCmp instanceof JTextArea) {
                    if (((com.codename1.ui.TextArea)cmp).getDoneListener() != null) {
                        InputMap input = textCmp.getInputMap();
                        KeyStroke enter = KeyStroke.getKeyStroke("ENTER");
                        KeyStroke shiftEnter = KeyStroke.getKeyStroke("shift ENTER");
                        input.put(shiftEnter, "insert-break");  // input.get(enter)) = "insert-break"
                        input.put(enter, "text-submit");

                        ActionMap actions = textCmp.getActionMap();
                        actions.put("text-submit", new AbstractAction() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                fireDone = true;
                                Listener.this.actionPerformed(null);
                            }
                        });
                    }
                }
                
            }
            public void run() {
                while (swingComponentToRemove.getParent() != null) {
                    synchronized(this) {
                        try {
                            wait(20);
                        } catch (InterruptedException ex) {
                        }
                    }
                }
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        actionPerformed(null);
                    }
                });
            }

            public void actionPerformed(ActionEvent e) {
                if (performed) {
                    return;
                }
                performed = true;
                String txt = getText(tf);
                if (testRecorder != null) {
                    testRecorder.editTextFieldCompleted(cmp, txt);
                }
                Display.getInstance().onEditingComplete(cmp, txt);
                if (e != null && cmp instanceof com.codename1.ui.TextField || fireDone) {
                    final com.codename1.ui.TextArea cn1Tf = (com.codename1.ui.TextArea)cmp;
                    if (cmp != null && cn1Tf.getDoneListener() != null) {
                        cn1Tf.fireDoneEvent();
                    }
                }
                if (tf instanceof JTextField) {
                    ((JTextField) tf).removeActionListener(this);
                }
                ((JTextComponent) tf).getDocument().removeDocumentListener(this);
                
                tf.removeFocusListener(this);
                canvas.remove(swingComponentToRemove);
                editingInProgress = null;
                currentlyEditingField = null;
                synchronized (this) {
                    notify();
                }
                canvas.repaint();
                if (invokeAfter != null) {
                    for (Runnable r : invokeAfter) {
                        r.run();
                    }
                    invokeAfter = null;
                }
            }

            public void focusGained(FocusEvent e) {
                setCaretPosition(tf, getText(tf).length());
                
                
            }

            public void focusLost(FocusEvent e) {
                actionPerformed(null);
            }

            public void keyTyped(KeyEvent e) {
                String t = getText(tf);

                if (t.length() >= ((TextArea) cmp).getMaxSize()) {
                    e.consume();
                } 
            }

            public void keyPressed(KeyEvent e) {
            }

            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    if (tf instanceof JTextField) {
                        actionPerformed(null);
                    } else {
                        if (getCaretPosition(tf) >= getText(tf).length() - 1) {
                            actionPerformed(null);
                        }
                    }
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    if (tf instanceof JTextField) {
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
                //if (cmp instanceof com.codename1.ui.TextField) {
                    updateText();
                //}

            }

            private void updateText() {
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        //if(cmp instanceof com.codename1.ui.TextField) {
                            ((com.codename1.ui.TextArea) cmp).setText(getText(tf));
                        //}
                    }
                });
            }
            
            public void insertUpdate(DocumentEvent e) {
                updateText();
            }

            public void removeUpdate(DocumentEvent e) {
                updateText();
            }

            public void changedUpdate(DocumentEvent e) {
                updateText();
            }

            private ArrayList<Runnable> invokeAfter;
            
            @Override
            public void invokeAfter(Runnable r) {
                if (invokeAfter == null) {
                    invokeAfter = new ArrayList<Runnable>();
                }
                invokeAfter.add(r);
            }

            @Override
            public void endEditing() {
                if (!EventQueue.isDispatchThread()) {
                    EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            endEditing();
                        }
                    });
                    
                    return;
                }
                actionPerformed(null);
            }
        }
;
        final Listener l = new Listener(tf, textCmp);
        if (tf instanceof JTextField) {
            ((JTextField) tf).addActionListener(l);
        }
        ((JTextComponent) tf).getDocument().addDocumentListener(l);
        

        tf.addKeyListener(l);
        tf.addFocusListener(l);
        if(simulateAndroidKeyboard) {
            java.util.Timer t = new java.util.Timer();
            TimerTask tt = new TimerTask() {
                @Override
                public void run() {
                    if(!Display.getInstance().isEdt()) {
                        Display.getInstance().callSerially(this);
                        return;
                    }
                    if(tf.getParent() != null) {
                        final int height = getScreenCoordinates().height;
                        JavaSEPort.this.sizeChanged(getScreenCoordinates().width, height / 2);
                        new UITimer(new Runnable() {
                            public void run() {
                                if(tf.getParent() != null) {
                                    new UITimer(this).schedule(100, false, Display.getInstance().getCurrent());
                                } else {
                                    JavaSEPort.this.sizeChanged(getScreenCoordinates().width, height);
                                }
                            }
                        }).schedule(100, false, Display.getInstance().getCurrent());
                    }
                }
            };
            t.schedule(tt, 300);
        }
        editingInProgress = l;
        currentlyEditingField = cmp;
        new Thread(l).start();
    }

    @Override
    public boolean isEditingText(Component c) {
        return currentlyEditingField == c && editingInProgress != null;
    }

    @Override
    public boolean isEditingText() {
        return editingInProgress != null;
    }
    
    
    
    

    /**
     * @inheritDoc
     */
    public void saveTextEditingState() {
    }

    @Override
    public void edtIdle(boolean enter) {
        if (isShowEDTWarnings()) {
            if (enter) {
                checkLastFrame();
            } else {
                lastIdleTime = System.currentTimeMillis();
            }
        }
    }

    private void checkLastFrame() {
        long t = System.currentTimeMillis();
        if (lastIdleTime > 0) {
            long diff = t - lastIdleTime;
            if (diff > 150) {
                System.out.println("Rendering frame took too long " + diff + " milliseconds");
            }
        }
        lastIdleTime = t;
    }

    /**
     * @inheritDoc
     */
    public void flushGraphics(int x, int y, int width, int height) {
        if (isShowEDTWarnings()) {
            checkEDT();
            checkLastFrame();
        }
        canvas.blit(x, y, width, height);
    }

    /**
     * @inheritDoc
     */
    public void flushGraphics() {
        if (isShowEDTWarnings()) {
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
        // disable EDT logging for this method
        boolean edtLog = showEDTWarnings;
        showEDTWarnings = false;
        
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
        showEDTWarnings = edtLog;
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
        if (perfMonitor != null) {
            perfMonitor.setClip(x, y, width, height);
        }
    }

    /**
     * @inheritDoc
     */
    public boolean isShapeClipSupported(Object graphics){
        return true;
    }
    
    /**
     * @inheritDoc
     */
    public void setClip(Object graphics, com.codename1.ui.geom.Shape shape){
        checkEDT();
        Graphics2D nativeGraphics = getGraphics(graphics);
        Shape s = cn1ShapeToAwtShape(shape);
        nativeGraphics.setClip(s);
    }
    
    /**
     * @inheritDoc
     */
    public void clipRect(Object graphics, int x, int y, int width, int height) {
        checkEDT();
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.clipRect(x, y, width, height);
        if (perfMonitor != null) {
            perfMonitor.clipRect(x, y, width, height);
        }
    }

    
    
    @Override
    public void pushClip(Object graphics) {
        checkEDT();
        Graphics2D g2d = getGraphics(graphics);
        Shape currentClip = g2d.getClip();
        AffineTransform at = g2d.getTransform();
        if (!at.isIdentity()) {
            try {
                at.invert();
            } catch (Exception ex){}
        }
        
        currentClip = at.createTransformedShape(currentClip);
        
        if ( graphics instanceof NativeScreenGraphics ){
            NativeScreenGraphics g = (NativeScreenGraphics)graphics;
            g.clipStack.push(currentClip);  
        } else {
            synchronized(clipStack) {
                if (!clipStack.containsKey(graphics)) {
                    clipStack.put(graphics, new LinkedList<Shape>());
                }
                clipStack.get(graphics).push(currentClip);
            }
        }
        
    }

    @Override
    public void popClip(Object graphics) {
        checkEDT();
        Graphics2D g2d = getGraphics(graphics);
        
        if ( graphics instanceof NativeScreenGraphics ){
            NativeScreenGraphics g = (NativeScreenGraphics)graphics;
            Shape oldClip = g.clipStack.pop();
            
            g2d.setClip(oldClip);
        } else {
            synchronized(clipStack) {
                if (clipStack.containsKey(graphics)) {
                    Shape oldClip = clipStack.get(graphics).pop();
                    if (oldClip != null) {
                        g2d.setClip(oldClip);
                    }
                }
            }
        }
        
    }

    private final Map<Object,LinkedList<Shape>> clipStack = new HashMap<Object,LinkedList<Shape>>();
    
    @Override
    public void disposeGraphics(Object graphics) {
        synchronized(clipStack) {
            clipStack.remove(graphics);
        }
    }
    
    
    
    
    
    
    /**
     * @inheritDoc
     */
    public void drawLine(Object graphics, int x1, int y1, int x2, int y2) {
        checkEDT();
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.drawLine(x1, y1, x2, y2);
        if (perfMonitor != null) {
            perfMonitor.drawLine(x1, y1, x2, y2);
        }
    }
    
    public boolean drawingNativePeer;
    public void drawNativePeer(final Object graphics, final PeerComponent cmp, final JComponent jcmp) {
        drawingNativePeer = true;
    
        try {
            
            // This should only be run on EDT to avoid deadlocks
            if (Display.getInstance().isEdt()) {
                synchronized(cmp) {
                    drawNativePeerImpl(graphics, cmp, jcmp);
                }
            } else if (!EventQueue.isDispatchThread()){ // I can just imagine bad things if we're already inside an EventQueue.invokeAndWait()
                Display.getInstance().callSeriallyAndWait(new Runnable() {
                    public void run() {
                        drawNativePeer(graphics, cmp, jcmp);
                    }
                });
            }
        } finally {
            drawingNativePeer = false;
        }
        
    }
    
    static BufferedImage deepCopy(BufferedImage bi) {
        java.awt.image.ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        java.awt.image.WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }
  
    private void drawNativePeerImpl(Object graphics, PeerComponent cmp, JComponent jcmp) {
        if (cmp instanceof Peer) {
            Peer peer = (Peer)cmp;
            if (peer.peerBuffer != null) {
                // PeerBuffer is used in cases like CEF where the peer component is rendered
                // using a BufferedImage and the pixels are piped directly from that 
                // Native CEF callback - no swing paint() involvement
                Graphics2D nativeGraphics = getGraphics(graphics);
                
                int tx = cmp.getAbsoluteX();
                int ty = cmp.getAbsoluteY();
                double scale = 1/zoomLevel;
                if (isScreenGraphics(nativeGraphics)) {
                    nativeGraphics.scale(scale, scale);
                }
                nativeGraphics.translate(tx/scale, ty/scale);
                peer.peerBuffer.paint(nativeGraphics, jcmp);
                nativeGraphics.translate(-tx/scale, -ty/scale);
                if (isScreenGraphics(nativeGraphics)) {
                    nativeGraphics.scale(1/scale, 1/scale);
                }
                return;
            }
        }
        if (cmp.getClientProperty("__buffer") != null) {
            // Swing peer components have an internal BufferedImage where they are 
            // rendered to on the swing paint() thread.  Then the image is drawn
            // to the CN1 graphics context.  This differs from the peerBuffer approach
            // in that the 
            BufferedImage img = (BufferedImage)cmp.getClientProperty("__buffer");
            Graphics2D nativeGraphics = getGraphics(graphics);
            nativeGraphics.drawImage(img, cmp.getAbsoluteX(), cmp.getAbsoluteY(), jcmp);
        }
    }

    //@Override
    public void fillLinearGradient(Object graphics, int startColor, int endColor, int x, int y, int width, int height, boolean horizontal) {
        checkEDT();
        Graphics2D nativeGraphics = getGraphics(graphics);
        
        Color c1 = new Color(startColor);
        int alphaStart = ColorUtil.alpha(startColor);
        int alphaEnd = ColorUtil.alpha(endColor);
        c1 = new Color(c1.getRed(), c1.getGreen(), c1.getBlue(), alphaStart);
        Color c2 = new Color(endColor);
        c2 = new Color(c2.getRed(), c2.getGreen(), c2.getBlue(), alphaEnd);
        Paint oldPaint = nativeGraphics.getPaint();
        GradientPaint paint = horizontal ?
                new GradientPaint(x, y + height/2, c1, x + width, y + height/2, c2) :
                new GradientPaint(x + width/2, y, c1, x + width/2, y + height, c2);
        nativeGraphics.setPaint(paint);
        nativeGraphics.fillRect(x, y, width, height);
        nativeGraphics.setPaint(oldPaint);
        
        
    }
    
    

    /**
     * @inheritDoc
     */
    public void fillRect(Object graphics, int x, int y, int w, int h) {
        checkEDT();
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.fillRect(x, y, w, h);
        if (perfMonitor != null) {
            perfMonitor.fillRect(x, y, w, h);
        }
    }

    @Override
    public void clearRect(Object graphics, int x, int y, int width, int height) {
        checkEDT();
        Graphics2D nativeGraphics = getGraphics(graphics);
        Composite c = nativeGraphics.getComposite();
        nativeGraphics.setComposite(AlphaComposite.Clear);
        nativeGraphics.fillRect(x, y, width, height);
        if (perfMonitor != null) {
            perfMonitor.clearRect(x, y, width, height);
        }
        nativeGraphics.setComposite(c);
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
        if (perfMonitor != null) {
            perfMonitor.drawRect(x, y, width, height);
        }
    }

    /**
     * @inheritDoc
     */
    public void drawRoundRect(Object graphics, int x, int y, int width, int height, int arcWidth, int arcHeight) {
        checkEDT();
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
        if (perfMonitor != null) {
            perfMonitor.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
        }
    }

    /**
     * @inheritDoc
     */
    public void fillRoundRect(Object graphics, int x, int y, int width, int height, int arcWidth, int arcHeight) {
        checkEDT();
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
        if (perfMonitor != null) {
            perfMonitor.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
        }
    }

    /**
     * @inheritDoc
     */
    public void fillArc(Object graphics, int x, int y, int width, int height, int startAngle, int arcAngle) {
        checkEDT();
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.fillArc(x, y, width, height, startAngle, arcAngle);
        if (perfMonitor != null) {
            perfMonitor.fillArc(x, y, width, height, startAngle, arcAngle);
        }
    }

    @Override
    public void fillRadialGradient(Object graphics, int startColor, int endColor, int x, int y, int width, int height) {
        checkEDT();
        Graphics2D nativeGraphics = (Graphics2D)getGraphics(graphics).create();
        Paint p = new RadialGradientPaint(x+width/2, y+height/2, width/2, new float[]{0,1}, new Color[]{new Color(startColor), new Color(endColor)});
        nativeGraphics.setPaint(p);
        nativeGraphics.fillOval(x+1, y+1, width-2, height-2);
    }

    
    
    /**
     * @inheritDoc
     */
    public void drawArc(Object graphics, int x, int y, int width, int height, int startAngle, int arcAngle) {
        checkEDT();
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.drawArc(x, y, width, height, startAngle, arcAngle);
        if (perfMonitor != null) {
            perfMonitor.drawArc(x, y, width, height, startAngle, arcAngle);
        }
    }

    /**
     * @inheritDoc
     */
    public void setColor(Object graphics, int RGB) {
        checkEDT();
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.setColor(new Color(RGB));
        if (perfMonitor != null) {
            perfMonitor.setColor(RGB);
        }
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
        if(alpha > 255 || alpha < 0) {
            throw new IllegalArgumentException("Invalid value for alpha: " + alpha);
        }
        checkEDT();
        Graphics2D nativeGraphics = getGraphics(graphics);
        float a = ((float) alpha) / 255.0f;
        nativeGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
        if (perfMonitor != null) {
            perfMonitor.setAlpha(alpha);
        }
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

    
    
    private Map<Integer,java.awt.Font> fallbackFonts = new HashMap<>();
    
    
    /*
     The following comprise a simple LRU cache to keep track of the value of
     fonts' canDisplayUpTo(String) method, since it may be called a lot.
    */
    
    // Cache to keep track of Font canDisplayUpTo(String) mechod.
    // key = <fontname>:<string>
    // Value integer return value of canDisplayUpTo(String) for the font.
    private Map<String,Integer> canDisplayUpToCache = new HashMap<>();
    
    // Linked list Linked list that is kept in sync with canDisplayUpToCache
    // so we know the order in which the strings were added.
    // Each value of the form <fontname>:<string>
    private LinkedList<String> canDisplayUpToCacheList = new LinkedList<>();
    
    // The maximum size of the cache in chars
    private static final int canDisplayUpToCacheMaxSize = 1024 * 256;
    
    // Half the max size of the cache in chars.  When cache fills up, it will
    // purge until it has at least this amount available.
    private static final int canDisplayUpToCacheHalfMaxSize =canDisplayUpToCacheMaxSize/2;
    
    // Variable to keep track of available chars in the cache.  Each time a string
    // is added, this is decreased, and when a string is removed, it is increased.
    private int canDisplayUpToCacheAvailable = canDisplayUpToCacheMaxSize;
    
    
    /*
     Wraps Font.canDisplayUpTo(String), but uses an LRU cache to keep
     track of the values
    */
    private int canDisplayUpTo(java.awt.Font fnt, String str) {
        String key = fnt.getName() + ":" + str;
        if (canDisplayUpToCache.containsKey(key)) {
            return canDisplayUpToCache.get(key);
        }
        int len = key.length();
        if (canDisplayUpToCacheAvailable - len < 0) {
            while (canDisplayUpToCacheAvailable < canDisplayUpToCacheHalfMaxSize) {
                String toRemove = canDisplayUpToCacheList.remove(0);
                canDisplayUpToCacheAvailable += toRemove.length();
                canDisplayUpToCache.remove(toRemove);
            }
        }
        
        canDisplayUpToCacheAvailable -= len;
        canDisplayUpToCacheList.add(key);
        int canDisplayUpTo = fnt.canDisplayUpTo(str);
        canDisplayUpToCache.put(key, canDisplayUpTo);
        return canDisplayUpTo;
        
    }
    
    /**
     * Checks to see if the given font is Roboto and the string contains the password 
     * char {@literal \u25cf}.  Roboto font is missing that glyph so we need to be 
     * able to substitute it out.
     * 
     * https://github.com/google/roboto/issues/291
     * 
     * @param fnt A font to check
     * @param str A string to check
     * @return True if the font is roboto and the string contains the password dot.
     */
    private boolean requiresFallbackFont(java.awt.Font fnt, String str) {
        return (fnt != null && fnt.getFontName().contains("Roboto") && str.contains("\u25cf"));
    }
    
    /**
     * Used only for roboto fonts because they cannot display the password "dot" glyph.  Usually this
     * should just return the font object that is passed into it.  However, if the font is Roboto,
     * and the string {@literal str} contains the password "dot" character ({@literal \u25cf}, then 
     * it will return a fallback font - a default sans-serif font.
     * @param fnt The font to check.
     * @param str The string to check for password chars.
     * @return Either the original font, or a fallback font if the original font can't render the string properly.
     */
    private java.awt.Font fallback(java.awt.Font fnt, String str) {
        if (requiresFallbackFont(fnt, str)) {
            return getFallbackFont(fnt);
        }
        return fnt;
    }
    
    /**
     * This can be used to get a default sans-serif font for the given font
     * @param f A font
     * @return A default sans-serif font of the same size.
     */
    private java.awt.Font getFallbackFont(java.awt.Font f) {
        int size = f.getSize();
        if (fallbackFonts.containsKey(size)) {
            return fallbackFonts.get(new Integer(size));
        }
        java.awt.Font fallbackFont = new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, size);
        fallbackFonts.put(size, fallbackFont);
        return fallbackFont;
    }
    
    /**
     * @inheritDoc
     */
    public void drawString(Object graphics, String str, int x, int y) {
        checkEDT();
        Graphics2D nativeGraphics = getGraphics(graphics);
        // the latter indicates mutable image graphics
        java.awt.Font origFont = nativeGraphics.getFont();
        java.awt.Font fnt = fallback(origFont, str);
        if (origFont != fnt) {
            // We need to deal with Roboto and password fields
            // https://github.com/google/roboto/issues/291
            nativeGraphics.setFont(fnt);
        }
        if (canDisplayUpTo(fnt, str) != -1) {
            // This might have emojis
            // render as attributed string
            AttributedString astr = createAttributedString(fnt, str);
            nativeGraphics.drawString(astr.getIterator(), x, y + nativeGraphics.getFontMetrics().getAscent());
        } else {
            nativeGraphics.drawString(str, x, y + nativeGraphics.getFontMetrics().getAscent());
        }
        if (origFont != fnt) {
            nativeGraphics.setFont(origFont);
        }
        if (perfMonitor != null) {
            perfMonitor.drawString(str, x, y);
        }
    }

    @Override
    public void drawingEncodedImage(EncodedImage img) {
        if (perfMonitor != null && !img.isLocked()) {
            perfMonitor.printToLog("Drawing unlocked image: " + img.getImageName());
        }
    }

    /**
     * @inheritDoc
     */
    public void drawImage(Object graphics, Object img, int x, int y) {
        checkEDT();
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.drawImage((BufferedImage) img, x, y, null);
        if (perfMonitor != null) {
            perfMonitor.drawImage(img, x, y);
        }
    }

    /**
     * @inheritDoc
     */
    public void drawImage(Object graphics, Object img, int x, int y, int w, int h) {
        checkEDT();
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.drawImage((BufferedImage) img, x, y, w, h, null);
        if (perfMonitor != null) {
            perfMonitor.drawImage(img, x, y, w, h);
        }
    }

    /**
     * @inheritDoc
     */
    public void fillTriangle(Object graphics, int x1, int y1, int x2, int y2, int x3, int y3) {
        checkEDT();
        Graphics2D nativeGraphics = getGraphics(graphics);
        nativeGraphics.fillPolygon(new int[]{x1, x2, x3}, new int[]{y1, y2, y3}, 3);
        if (perfMonitor != null) {
            perfMonitor.fillTriangle(x1, y1, x2, y2, x3, y3);
        }
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
        if (perfMonitor != null) {
            perfMonitor.drawRGB(rgbData, offset, x, y, w, h, processAlpha);
        }
    }

    NativeScreenGraphics ng;
    
    /**
     * @inheritDoc
     */
    public Object getNativeGraphics() {
        //if (ng == null) {
            ng = new NativeScreenGraphics();
        //}
        return ng;
        //return new NativeScreenGraphics();
    }

    /**
     * @inheritDoc
     */
    public Object getNativeGraphics(Object image) {
        /*
         * NativeScreenGraphics n = new NativeScreenGraphics(); n.sourceImage =
         * (BufferedImage)image; return n;
         */
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

    private Rectangle2D getStringBoundsWithEmojis(java.awt.Font font, String str) {
        if (hasUnsupportedChars(font, str)) {
            TextLayout textLayout = new TextLayout( 
                    createAttributedString(font, str).getIterator(), 
                    canvas.getFRC()
            );
            
            Rectangle2D.Float textBounds = ( Rectangle2D.Float ) textLayout.getBounds();
            return textBounds;
        } else {
            return font.getStringBounds(str, canvas.getFRC());
        }
    }
    
    private java.awt.Font emojiFont;
    private boolean attemptedToLoadEmojiFont;
    private Map<Integer,java.awt.Font> emojiFontCache;
    
    private boolean isEmojiFontLoaded() {
        return getEmojiFont() != null;
    }
    
    private java.awt.Font getEmojiFont() {
        if (emojiFont == null) {
            if (!attemptedToLoadEmojiFont) {
                attemptedToLoadEmojiFont = true;
                try {
                    emojiFont = (java.awt.Font)loadTrueTypeFont("Noto Emoji", "NotoEmoji-Regular.ttf");
                    //emojiFont = (java.awt.Font)loadTrueTypeFont("OpenSansEmoji", "OpenSansEmoji.ttf");
                } catch (Throwable t){
                    System.out.println("Failed to load emoji font "+t.getMessage());
                }
            }
        }
        return emojiFont;
    }
    
    private java.awt.Font deriveEmojiFont(float size) {
        if (emojiFont == null) {
            return null;
        }
        if (emojiFontCache == null) {
            emojiFontCache = new HashMap<Integer,java.awt.Font>();
        }
        int key = (int)Math.round(size);
        if (!emojiFontCache.containsKey(key)) {
            java.awt.Font fnt = emojiFont.deriveFont(size);
            emojiFontCache.put(key, fnt);
            return fnt;
        }
        return emojiFontCache.get(key);
    }
    
    
    private Map<String,AttributedString> attributedStringCache = new HashMap<>();
    private LinkedList<String> attributedStringCacheList = new LinkedList<>();
    private static final int attributedStringCacheMaxSize = 1024 * 256;
    private static final int attributedStringCacheHalfMaxSize = attributedStringCacheMaxSize/2;
    private int attributedStringCacheAvailable = attributedStringCacheMaxSize;
    
    
    private AttributedString createAttributedString(java.awt.Font font, String str) {
        String key = font.getName() + ":" + font.getSize()+":"+ str;
        if (attributedStringCache.containsKey(key)) {
            return attributedStringCache.get(key);
        }
        int keyLen = key.length();
        if (attributedStringCacheAvailable - keyLen < 0) {
            while (attributedStringCacheAvailable < attributedStringCacheHalfMaxSize) {
                String toRemove = attributedStringCacheList.remove(0);
                attributedStringCacheAvailable += toRemove.length();
                attributedStringCache.remove(toRemove);
            }
        }
        java.awt.Font emojiFont = isEmojiFontLoaded() ? deriveEmojiFont(font.getSize2D()) : null;
        AttributedString astr = new AttributedString(str);
        attributedStringCacheList.add(key);
        attributedStringCache.put(key, astr);
        attributedStringCacheAvailable -= keyLen;
        
        java.awt.Font fallbackFont = getFallbackFont(font);
        astr.addAttribute(TextAttribute.FONT, font);
        int pos = font.canDisplayUpTo(str);
        if (pos == -1) {
            return astr;
        }
        int len = str.length();
        char[] chars = str.toCharArray();
        while (pos < len) {
            // find next char that the font can render
            int spanEnd = len;
            for (int j=pos; j<len; j++) {
                char c = chars[j];
                
                if (j < len-1 && Character.isSurrogatePair(c, chars[j+1])) {
                    int codePoint = Character.toCodePoint(c, chars[j+1]);
                    if (font.canDisplay(codePoint)) {
                        spanEnd = j;
                        break;
                    }
                    j++;
                } else {
                    if (font.canDisplay(c)) {
                        spanEnd = j;
                        break;
                    }
                }
            }
            
            
            String spanStr = new String(chars, pos, spanEnd - pos);
            if (emojiFont == null || canDisplayUpTo(fallbackFont, spanStr) == -1) {
                astr.addAttribute(TextAttribute.FONT, fallbackFont, pos, spanEnd);
            } else {
            
                astr.addAttribute(TextAttribute.FONT, emojiFont, pos, spanEnd);
            }
            
            if (spanEnd < len) {
                pos = font.canDisplayUpTo(chars, spanEnd, len);
                if (pos == -1) {
                    pos = len;
                }
            } else {
                pos = spanEnd;
            }
            
        }
        
        return astr;
    }
    
    private boolean hasUnsupportedChars(java.awt.Font font, String str) {
        return canDisplayUpTo(font, str) != -1;
    }
    
    /**
     * @inheritDoc
     */
    public int stringWidth(final Object nativeFont, final String str) {
        if (perfMonitor != null) {
            perfMonitor.stringWidth(nativeFont, str);
        }
        checkEDT();
        if(str == null) {
            return 0;
        }
        java.awt.Font fnt = fallback(font(nativeFont), str);
        java.awt.geom.Rectangle2D r2d = getStringBoundsWithEmojis(fnt, str);//fnt.getStringBounds(str, canvas.getFRC());
        int w = (int) Math.ceil(r2d.getWidth());
        return w;
    }

    /**
     * @inheritDoc
     */
    public int charWidth(Object nativeFont, char ch) {
        if (perfMonitor != null) {
            perfMonitor.charWidth(nativeFont, ch);
        }
        checkEDT();
        String strch = ""+ch;
        java.awt.Font fnt = fallback(font(nativeFont), strch);
        if (!fnt.canDisplay(ch)) {
            fnt = getFallbackFont(fnt);
        }
        
        if (isEmojiFontLoaded() && !Character.isHighSurrogate(ch) && !fnt.canDisplay(ch)) {
            fnt = deriveEmojiFont(fnt.getSize2D());
        }
        int w = (int) Math.ceil(fnt.getStringBounds(strch, canvas.getFRC()).getWidth());
        return w;
    }

    @Override
    public int getFontAscent(Object nativeFont) {
        checkEDT();
        return Math.abs(canvas.getGraphics2D().getFontMetrics(font(nativeFont)).getAscent());
    }

    @Override
    public int getFontDescent(Object nativeFont) {
        checkEDT();
        return Math.abs(canvas.getGraphics2D().getFontMetrics(font(nativeFont)).getDescent());
    }

    @Override
    public boolean isBaselineTextSupported() {
        return true;
    }
    
    
    
    

    /**
     * @inheritDoc
     */
    public int getHeight(Object nativeFont) {
        checkEDT();
        FontMetrics metrics = canvas.getGraphics2D().getFontMetrics(font(nativeFont));
        if (metrics.getDescent() < 0) {
            return metrics.getAscent() - metrics.getDescent() + metrics.getLeading();
        } else {
            return metrics.getHeight();
        }
        /*
        int out = metrics.getHeight();
        int ascent = metrics.getAscent();
        int descent = metrics.getDescent();
        int leading = metrics.getLeading();
        int maxAscent = metrics.getMaxAscent();
        int maxDescent = metrics.getMaxDescent();
        
        if (font(nativeFont).getName().contains("Hyundai")) {
            int foo = 1;
        }
        return out;
        */
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
        return null;
    }
    
    @Override
    public Object loadTrueTypeFont(String fontName, String fileName) {
        File fontFile = null;
        try {
            if(fontName.startsWith("native:")) {
                if(IS_MAC && isIOS) {
                    String nn = nativeFontName(fontName);
                    java.awt.Font nf = new java.awt.Font(nn, java.awt.Font.PLAIN, medianFontSize);
                    return nf;
                }
                String res; 
                switch(fontName) {
                    case "native:MainThin":
                        res = "Thin";
                        break;

                    case "native:MainLight":
                        res = "Light";
                        break;

                    case "native:MainRegular":
                        res = "Medium";
                        break;

                    case "native:MainBold":
                        res = "Bold";
                        break;

                    case "native:MainBlack":
                        res = "Black";
                        break;

                    case "native:ItalicThin":
                        res = "ThinItalic";
                        break;

                    case "native:ItalicLight": 
                        res = "LightItalic";
                        break;

                    case "native:ItalicRegular":
                        res = "Italic";
                        break;

                    case "native:ItalicBold":
                        res = "BoldItalic";
                        break;

                    case "native:ItalicBlack":
                        res = "BlackItalic";
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported native font type: " + fontName);
                }
                String fontResourcePath = "/com/codename1/impl/javase/Roboto-" + res + ".ttf";
                InputStream is = getClass().getResourceAsStream(fontResourcePath);
                if(is != null) {
                    java.awt.Font fnt;
                    try {
                         fnt = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, is);
                    } catch (Exception e) {
                        System.err.println("Exception while reading font from resource path "+fontResourcePath);
                        throw e;
                    }
                    is.close();
                    return fnt;
                }
            }
            if (baseResourceDir != null) {
                fontFile = new File(baseResourceDir, fileName);
            } else {
                fontFile = new File(getSourceResourcesDir(), fileName);
            }
            if (fontFile.exists()) {
                try {
                    FileInputStream fs = new FileInputStream(fontFile);
                    java.awt.Font fnt = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, fs);
                    fs.close();
                    if (fnt != null) {
                        if (!fontName.startsWith(fnt.getFamily())) {
                            System.out.println("Warning font name might be wrong for " + fileName + " should be: " + fnt.getName());
                        }
                    }
                    return fnt;
                } catch (Exception e) {
                    System.err.println("Exception thrown while trying to create font from file "+fontFile);
                    throw e;
                }
            } else {
                InputStream is = getResourceAsStream(getClass(), "/" + fileName);
                if(is != null) {
                    try {
                        java.awt.Font fnt = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, is);
                        is.close();
                        if (fnt != null) {
                            if (!fontName.startsWith(fnt.getFamily())) {
                                System.out.println("Warning font name might be wrong for " + fileName + " should be: " + fnt.getName());
                            }
                        }
                        return fnt;
                    } catch (Exception e) {
                        System.err.println("Exception thrown while trying to create font file from resource path "+"/"+fileName);
                        throw e;
                    }
                }
            }
        } catch (Exception err) {
            err.printStackTrace();
            throw new RuntimeException(err);
        }
        if(fontFile != null) {
            throw new RuntimeException("The file wasn't found: " + fontFile.getAbsolutePath());
        }
        throw new RuntimeException("The file wasn't found: " + fontName);
    }

    private static double fontScale=1.0;
    
    public static double getFontScale() {
        return fontScale;
    }
    
    
    public static void setFontScale(double scale) {
        fontScale = scale;
    }
    
    
    
    @Override
    public Object deriveTrueTypeFont(Object font, float size, int weight) {
        java.awt.Font fnt;
        if(font instanceof int []){
            fnt = createAWTFont((int [])font);
        }else{
            fnt = (java.awt.Font) font;        
        }
        int style = java.awt.Font.PLAIN;
        if ((weight & com.codename1.ui.Font.STYLE_BOLD) == com.codename1.ui.Font.STYLE_BOLD) {
            style = java.awt.Font.BOLD;
        }
        if ((weight & com.codename1.ui.Font.STYLE_ITALIC) == com.codename1.ui.Font.STYLE_ITALIC) {
            style = style | java.awt.Font.ITALIC;
        }
        java.awt.Font fff = fnt.deriveFont(style, (float)(size * getFontScale()));
        
        if(Math.abs(size / 2 - fff.getSize())  < 3) {
            // retina display bug!
            return fnt.deriveFont(style, (float)(size * 2 * getFontScale()));
        }
        return fff;
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
    
    public java.awt.Font createAWTFont(int face, int style, int size, double scale) {
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
                fontName += (int)Math.round(largeFontSize*scale);
                break;
            case Font.SIZE_SMALL:
                fontName += (int)Math.round(smallFontSize*scale);
                break;
            default:
                fontName += (int)Math.round(medianFontSize*scale);
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

    @Override
    public boolean isShapeSupported(Object graphics) {
        return true;
    }

    @Override
    public boolean isTransformSupported(Object graphics) {
        return true;
    }

    private static final double[] IDENTITY = new double[6]; 
    {
        AffineTransform.getScaleInstance(1, 1).getMatrix(IDENTITY);
    }
    
    private static AffineTransform clamp(AffineTransform at){
        double[] mat = new double[6];
        at.getMatrix(mat);
        clamp(mat);
        at.setTransform(mat[0], mat[1], mat[2], mat[3], mat[4], mat[5]);
        return at;
    }
    
    
    private static void clamp(double[] mat) {
        for (int i=0; i<6; i++) {
            double d = mat[i];
            double clampVal = IDENTITY[i];
            if (Math.abs(d-clampVal) < 0.001) {
                mat[i] = clampVal;
            } else {
                mat[i] = d;
            }
        }
    }
    
    private static void clamp(float[] mat) {
        for (int i=0; i<6; i++) {
            float d = mat[i];
            float clampVal = (float)IDENTITY[i];
            if (Math.abs(d-clampVal) < 0.001) {
                mat[i] = clampVal;
            } else {
                mat[i] = d;
            }
        }
    }
    
    private static float[] clampCoord(float[] in) {
        for ( int i=0; i<in.length; i++){
            in[i] = clampScalar(in[i]);
        }
        return in;
    }
    
    private static float clampScalar(float d){
        float abs = Math.abs(d);
        if ( Math.abs(abs-Math.round(abs)) < 0.001){
            return Math.round(d);
        }
        return d;
    }

    
    /*
    private double clamp(double d){
        double abs = Math.abs(d);
        if ( Math.abs(abs-Math.round(abs)) < 0.001){
            return Math.round(d);
        }
        return d;
    }
    private float clamp(float d){
        float abs = Math.abs(d);
        if ( Math.abs(abs-Math.round(abs)) < 0.001){
            return Math.round(d);
        }
        return d;
    }
    
    private double[] clamp(double[] in){
        for ( int i=0; i<in.length; i++){
            in[i] = clamp(in[i]);
        }
        return in;
    }
    private float[] clamp(float[] in){
        for ( int i=0; i<in.length; i++){
            in[i] = clamp(in[i]);
        }
        return in;
    }
    */

    @Override
    public boolean transformNativeEqualsImpl(Object t1, Object t2) {
        if ( t1 != null ){
            AffineTransform at1 = (AffineTransform)t1;
            AffineTransform at2 = (AffineTransform)t2;
            return at1.equals(at2);
            
        } else {
            return t2 == null;
        }
    }
    
    
    
    
    @Override
    public void fillShape(Object graphics, com.codename1.ui.geom.Shape shape) {
        checkEDT();
        Graphics2D nativeGraphics = getGraphics(graphics);
        Shape s = cn1ShapeToAwtShape(shape);
        nativeGraphics.fill(s);
        
    }

    @Override
    public void drawShadow(Object graphics, Object image, int x, int y, int offsetX, int offsetY, int blurRadius, int spreadRadius, int color, float opacity) {

        checkEDT();
        Graphics2D nativeGraphics = getGraphics(graphics);
        /*
        Shape s = cn1ShapeToAwtShape(shape);
        Rectangle shapeBounds = s.getBounds();
        BufferedImage buf = new BufferedImage(shapeBounds.width + Math.abs(offsetX) + 2*(blurRadius + spreadRadius),
                shapeBounds.height + Math.abs(offsetY) + 2*(blurRadius + spreadRadius), BufferedImage.TYPE_INT_ARGB);
        Graphics2D bufGraphics = (Graphics2D)buf.createGraphics();
        bufGraphics.translate(-shapeBounds.x + blurRadius + spreadRadius + Math.abs(offsetX), -shapeBounds.y + blurRadius + spreadRadius + Math.abs(offsetY));
        bufGraphics.setColor(Color.black);
        bufGraphics.fill(s);
        bufGraphics.dispose();
        */

        //BufferedImage buf = (BufferedImage) image;

        ShadowFilter filter = new ShadowFilter();
        filter.setAddMargins(false);
        filter.setAngle((float)Math.atan(-offsetY/(double)offsetX));
        filter.setDistance((float)Math.sqrt(offsetX*offsetX+offsetY*offsetY));
        // There is a bug in ShadowFilter opacity setting that applies opacity twice, so we'll do this in two ways.
        filter.setOpacity(1f);
        //filter.setOpacity(opacity);
        filter.setRadius(blurRadius);
        filter.setShadowOnly(true);
        //BufferedImage dst = new BufferedImage(buf.getWidth(), buf.getHeight(), BufferedImage.TYPE_INT_ARGB);
        //filter.filter(buf, dst);
        //int tx = x - blurRadius - spreadRadius - Math.abs(offsetX);
        //int ty = y - blurRadius - spreadRadius - Math.abs(offsetY);
        //nativeGraphics.translate(tx, ty);
        BufferedImage buf = (BufferedImage)image;

        if (spreadRadius != 0) {
            int scaledWidth = buf.getWidth() + 2 * spreadRadius;
            int scaledHeight = buf.getHeight() + 2 * spreadRadius;
            if (scaledWidth < 1 || scaledHeight < 1) return;
            java.awt.Image scaledImage = buf.getScaledInstance(scaledWidth, scaledHeight, BufferedImage.SCALE_FAST);
            BufferedImage scaledBuf = new BufferedImage(buf.getWidth(), buf.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D bufG = (Graphics2D) scaledBuf.createGraphics();
            bufG.drawImage(scaledImage, -spreadRadius, -spreadRadius, null);
            bufG.dispose();
            buf = scaledBuf;

        }
        Composite prevComposite = nativeGraphics.getComposite();
        nativeGraphics.setComposite( AlphaComposite.getInstance( AlphaComposite.SRC_OVER, opacity ) );
        nativeGraphics.drawImage(buf, filter, x, y);
        nativeGraphics.setComposite(prevComposite);

        //nativeGraphics.translate(-tx, -ty);



    }

    @Override
    public boolean isDrawShadowSupported() {
        /**
         * Return true here as the platform does support drawing shadows.  However implementation is very slow.
         */
        return true;
    }

    @Override
    public boolean isDrawShadowFast() {
        return false;
    }

    @Override
    public void drawShape(Object graphics, com.codename1.ui.geom.Shape shape, com.codename1.ui.Stroke stroke) {
        checkEDT();
        Graphics2D nativeGraphics = getGraphics(graphics);
        Shape s = cn1ShapeToAwtShape(shape);
        
        Stroke oldStroke = nativeGraphics.getStroke();
        BasicStroke bs = new BasicStroke(stroke.getLineWidth(), stroke.getCapStyle() , stroke.getJoinStyle(), stroke.getMiterLimit());
        nativeGraphics.setStroke(bs);
       
        nativeGraphics.draw(s);
        nativeGraphics.setStroke(oldStroke);
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
        return true;
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

    @Override
    public Object makeTransformAffine(double m00, double m10, double m01, double m11, double m02, double m12) {
        return new AffineTransform(m00, m10, m01, m11, m02, m12);
    }

    @Override
    public void setTransformAffine(Object nativeTransform, double m00, double m10, double m01, double m11, double m02, double m12) {
        ((AffineTransform)nativeTransform).setTransform(m00, m10, m01, m11, m02, m12);
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
        return clamp(AffineTransform.getTranslateInstance(translateX, translateY));
    }

    @Override
    public void setTransformTranslation(Object nativeTransform, float translateX, float translateY, float translateZ) {
        AffineTransform at = (AffineTransform)nativeTransform;
        at.setToTranslation(translateX, translateY);
        at.setTransform(clamp(at));
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
        return clamp(AffineTransform.getScaleInstance(scaleX, scaleY));
    }
    
    


    @Override
    public void setTransformScale(Object nativeTransform, float scaleX, float scaleY, float scaleZ) {
        AffineTransform at = (AffineTransform)nativeTransform;
        at.setToScale(scaleX, scaleY);
        at.setTransform(clamp(at));
       
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
        return clamp(AffineTransform.getRotateInstance(angle, x, y));
    }

    @Override
    public void setTransformRotation(Object nativeTransform, float angle, float x, float y, float z) {
        AffineTransform at = (AffineTransform)nativeTransform;
        at.setToRotation(angle, x, y);
        at.setTransform(clamp(at));
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
        throw new RuntimeException("Perspective transform not supported");
    }

    @Override
    public void setTransformPerspective(Object nativeTransform, float fovy, float aspect, float zNear, float zFar) {
        throw new RuntimeException("Perspective transforms not supported");
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
        throw new RuntimeException("Perspective transforms not supported");
    }

    @Override
    public void setTransformOrtho(Object nativeGraphics, float left, float right, float bottom, float top, float near, float far) {
        throw new RuntimeException("Perspective transforms not supported");
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

    @Override
    public void setTransformCamera(Object nativeGraphics, float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ) {
        throw new RuntimeException("Perspective transforms not supported");
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
       ((AffineTransform)nativeTransform).rotate(angle, x, y);
       clamp((AffineTransform)nativeTransform);
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
        ((AffineTransform)nativeTransform).translate(x, y);
        clamp((AffineTransform)nativeTransform);
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
        ((AffineTransform)nativeTransform).scale(x, y);
        clamp((AffineTransform)nativeTransform);
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
       try {
           return clamp(((AffineTransform)nativeTransform).createInverse());
       } catch ( Exception ex){
           return null;
       }
    }

    @Override
    public void setTransformInverse(Object nativeTransform) throws com.codename1.ui.Transform.NotInvertibleException {
        AffineTransform at = (AffineTransform)nativeTransform;
        
        try {
            at.invert();
            at.setTransform(clamp(at));
        } catch (Exception ex) {
            throw new com.codename1.ui.Transform.NotInvertibleException();
        }
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
        return new AffineTransform();
    }

    @Override
    public void setTransformIdentity(Object transform) {
        AffineTransform at = (AffineTransform)transform;
        at.setToIdentity();
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
       AffineTransform t1 = (AffineTransform)src;
       AffineTransform t2 = (AffineTransform)dest;
       t2.setTransform(t1);
    }

    /**
     * Concatenates two transforms and sets the first transform to be the result of the concatenation.
     * <p>This can only be used if {@link #isTransformSupported()} returns true.</p>
     * @param t1 The left native transform.  The result will also be stored in this transform.
     * @param t2 The right native transform.  The result will also be stored in this transform.
     * @see #isTransformSupported()
     */
    public void concatenateTransform(Object t1, Object t2) {
        ((AffineTransform)t1).concatenate((AffineTransform)t2);
        clamp((AffineTransform)t1);
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
        AffineTransform t = (AffineTransform)nativeTransform;
        t.transform(in, 0, out, 0, 1);
        clampCoord(out);
    }
    
    
    
    @Override
    public void setTransform(Object graphics, Transform transform) {
        checkEDT();
        Graphics2D g = getGraphics(graphics);
        AffineTransform t = (graphics == g) ? new AffineTransform() :
                AffineTransform.getScaleInstance(zoomLevel, zoomLevel);
        t.concatenate((AffineTransform)transform.getNativeTransform());
        clamp(t);
        g.setTransform(t);
        Transform existing = getNativeScreenGraphicsTransform(graphics);
        if (existing == null) {
            existing = transform.copy();
            setNativeScreenGraphicsTransform(graphics, existing);
        } else {
            existing.setTransform(transform);
        }
        
    }

    private com.codename1.ui.Transform getTransformInternal(Object graphics) {
        checkEDT();
        com.codename1.ui.Transform t = getNativeScreenGraphicsTransform(graphics);
        if ( t == null ){
            return Transform.makeIdentity();
        }
        return t;
    }
    
    @Override
    public com.codename1.ui.Transform getTransform(Object graphics) {
        return getTransformInternal(graphics).copy();
    }

    @Override
    public void getTransform(Object graphics, Transform transform) {
        checkEDT();
        com.codename1.ui.Transform t = getNativeScreenGraphicsTransform(graphics);
        if ( t == null ){
            transform.setIdentity();
        } else {
            transform.setTransform(t);
        }

    }
    
    
    
    // END TRANSFORM STUFF
    
    
    private com.codename1.ui.geom.Shape awtShapeToCn1Shape(Shape shape){
        com.codename1.ui.geom.GeneralPath p = new com.codename1.ui.geom.GeneralPath();
        PathIterator it = shape.getPathIterator(AffineTransform.getScaleInstance(1, 1));
        p.setWindingRule(it.getWindingRule()==PathIterator.WIND_EVEN_ODD ? com.codename1.ui.geom.PathIterator.WIND_EVEN_ODD : com.codename1.ui.geom.PathIterator.WIND_NON_ZERO);
        float[] buf = new float[6];
        while (!it.isDone()){
            int type = it.currentSegment(buf);
            switch ( type ){
                case PathIterator.SEG_MOVETO:
                    p.moveTo(buf[0], buf[1]);
                    break;
                case PathIterator.SEG_LINETO: 
                    p.lineTo(buf[0], buf[1]);
                    break;
                case PathIterator.SEG_QUADTO:
                    p.quadTo(buf[0], buf[1], buf[2], buf[3]);
                    break;
                case PathIterator.SEG_CUBICTO:
                    p.curveTo(buf[0], buf[1], buf[2], buf[3], buf[4], buf[5]);
                    break;
                case PathIterator.SEG_CLOSE:
                    p.closePath();
                    break;
                    
            }
            it.next();
        }
        
        return p;
        
        
    }
    
    

    private Shape cn1ShapeToAwtShape(com.codename1.ui.geom.Shape shape){
        GeneralPath p = new GeneralPath();
        com.codename1.ui.geom.PathIterator it = shape.getPathIterator();
        p.setWindingRule(it.getWindingRule()==com.codename1.ui.geom.PathIterator.WIND_EVEN_ODD ? GeneralPath.WIND_EVEN_ODD : GeneralPath.WIND_NON_ZERO);
        float[] buf = new float[6];
        while (!it.isDone()){
            int type = it.currentSegment(buf);
            switch ( type ){
                case com.codename1.ui.geom.PathIterator.SEG_MOVETO:
                    p.moveTo(buf[0], buf[1]);
                    break;
                case com.codename1.ui.geom.PathIterator.SEG_LINETO: 
                    p.lineTo(buf[0], buf[1]);
                    break;
                case com.codename1.ui.geom.PathIterator.SEG_QUADTO:
                    p.quadTo(buf[0], buf[1], buf[2], buf[3]);
                    break;
                case com.codename1.ui.geom.PathIterator.SEG_CUBICTO:
                    p.curveTo(buf[0], buf[1], buf[2], buf[3], buf[4], buf[5]);
                    break;
                case com.codename1.ui.geom.PathIterator.SEG_CLOSE:
                    p.closePath();
                    break;
                    
            }
            it.next();
        }
        
        return p;
        
        
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
        // causes a simulator with a dialog open to freeze
        /*try {
            Executor.stopApp();
            Executor.destroyApp();
        } catch (Throwable t) {
            t.printStackTrace();
        }*/
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
        if ("platformHint.showEDTWarnings".equals(key)) {
            return String.valueOf(showEDTWarnings);
        }
        if ("simulator.skin".equalsIgnoreCase(key)) {
            return getCurrentSkinName();
        }
        if(key.equalsIgnoreCase("cn1_push_prefix") 
                || key.equalsIgnoreCase("cellId") 
                || key.equalsIgnoreCase("IMEI") 
                || key.equalsIgnoreCase("UDID") 
                || key.equalsIgnoreCase("MSISDN")) {
            if(!checkForPermission("android.permission.READ_PHONE_STATE", "This is required to get the phone state")){
                return "";
            }
            return defaultValue;
        }
        if ("OS".equals(key)) {
            return "SE";
        }
        if ("AppName".equals(key)) {
            File f = new File(getCWD(),"codenameone_settings.properties");
            if (f.exists()) {
                try {
                    Properties p = new Properties();
                    p.load(new FileInputStream(f));
                    return p.getProperty("codename1.displayName");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            return defaultValue;
        }
        if ("AppVersion".equals(key)) {
            File f = new File(getCWD(), "codenameone_settings.properties");
            if (f.exists()) {
                try {
                    Properties p = new Properties();
                    p.load(new FileInputStream(f));
                    return p.getProperty("codename1.version");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            return defaultValue;
        }
        String s = System.getProperty(key);

        if (key.equals("built_by_user")) {
            Preferences p = Preferences.userNodeForPackage(com.codename1.ui.Component.class);
            String user = p.get("user", null);
            Display d = Display.getInstance();
            if (user == null) {
                JPanel pnl = new JPanel();
                JTextField tf = new JTextField(20);
                pnl.add(new JLabel("E-Mail For Push"));
                pnl.add(tf);
                int val = JOptionPane.showConfirmDialog(canvas, pnl, "Please Enter Build Email Account", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (val != JOptionPane.OK_OPTION) {
                    return null;
                }
                user = tf.getText();
                //p.put("user", user);
            }
            d.setProperty("built_by_user", user);
            return user;
        }

        if (key.equals("package_name")) {
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

    private void launchBrowserThatWorks(String url) {
        Preferences p = Preferences.userNodeForPackage(com.codename1.ui.Component.class);
        String externalBrowserExe = p.get("externalBrowserExe", null);
        try {
            try {
                if (externalBrowserExe != null && new File(externalBrowserExe).exists()) {
                    ProcessBuilder pb = new ProcessBuilder(externalBrowserExe, url);
                    pb.start();
                    return;
                }
            } catch (Exception err) {
                err.printStackTrace();
                p.remove("externalBrowserExe");
            }
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception ex) {
            try {
                if (url.startsWith("file:") && new File(new URI(url)).exists()) {
                    Desktop.getDesktop().open(new File(new URI(url)));
                } else {
                    int val = JOptionPane.showConfirmDialog(window, "Error Launching Browser", "Do you want to pick a browser executable manually?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if(val == JOptionPane.YES_OPTION) {
                        File file = pickFile(new String[] {"*"}, "Browser Executable");
                        if (file != null && file.exists() && file.canExecute()) {
                            p.put("externalBrowserExe", file.getAbsolutePath());
                            launchBrowserThatWorks(url);
                        }
                    }

                }
            } catch (Exception ex2) {
                ex2.printStackTrace();
            }
        }

    }

    /**
     * @inheritDoc
     */
    public void execute(String url) {
        try {
            url = url.trim();
            if(url.startsWith("file:")) {
                if(!checkForPermission("android.permission.WRITE_EXTERNAL_STORAGE", "This is required to open the file")){
                    return;
                }
                
                url = new File(unfile(url)).toURI().toURL().toExternalForm();
            }
            final String fUrl = url;
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    launchBrowserThatWorks(fUrl);
                }
            });
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public Graphics2D getGraphics(Object nativeG) {
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
    
    public boolean isScreenGraphics(Graphics2D g) {
        return g == canvas.getGraphics2D();
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

    public Media createBackgroundMedia(String uri) throws IOException {
        if(!checkForPermission("android.permission.READ_PHONE_STATE", "This is required to play media")){
            return null;
        }
        
        return super.createBackgroundMedia(uri);
    }

    @Override
    public AsyncResource<Media> createBackgroundMediaAsync(String uri) {
        AsyncResource<Media> out = new AsyncResource<Media>();
        
        if(!checkForPermission("android.permission.READ_PHONE_STATE", "This is required to play media")){
            out.error(new IOException("android.permission.READ_PHONE_STATE is required to play media"));
            return out;
        }
        
        return super.createBackgroundMediaAsync(uri);
    }
    
    
    
    
    
    public static class CN1JPanel extends JPanel {
        
        double zoom_;

        @Override
        public void revalidate() {
            // We need to override this with an empty implementation to workaround
            // Deadlock bug  http://bugs.java.com/view_bug.do?bug_id=8058870
            // If we allow the default implementation, then it will periodically deadlock
            // when displaying a browser component
        }

        
        
        @Override
        protected void processMouseEvent(MouseEvent e) {
            //super.processMouseEvent(e); //To change body of generated methods, choose Tools | Templates.


            if (!sendToCn1(e)) {
                if (isOnCanvas(e)) {
                    if (isFocusable() && !hasFocus()) {
                        if (e.getID() == MouseEvent.MOUSE_PRESSED) {
                            requestFocus();
                        }
                    }

                    super.processMouseEvent(e);
                }

            }
            
        }

        @Override
        public boolean contains(int x, int y) {
            Point p = SwingUtilities.convertPoint(this, new Point(x, y), instance.canvas);
            return instance.canvas.getVisibleRect().contains(p);
        }

        @Override
        protected void processMouseMotionEvent(MouseEvent e) {
            if (!sendToCn1(e)) {
                if (isOnCanvas(e)) {
                    super.processMouseMotionEvent(e); //To change body of generated methods, choose Tools | Templates.
                }
            }
            
        }

        @Override
        protected void processMouseWheelEvent(MouseWheelEvent e) {
            if (!sendToCn1(e)) {
                if (isOnCanvas(e)) {
                    super.processMouseWheelEvent(e); //To change body of generated methods, choose Tools | Templates.
                }
            }
        }


        private boolean isOnCanvas(MouseEvent e) {
            Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), instance.canvas);
            return instance.canvas.getVisibleRect().contains(p);

        }
        
        private boolean peerGrabbedDrag=false;
        
        private boolean sendToCn1(MouseEvent e) {

            int cn1X = getCN1X(e);
            int cn1Y = getCN1Y(e);
            if ((!peerGrabbedDrag || true) && Display.isInitialized()) {
                if (!isOnCanvas(e)) return false;
                Form f = Display.getInstance().getCurrent();
                if (f != null) {
                    Component cmp = f.getComponentAt(cn1X, cn1Y);
                    //if (!(cmp instanceof PeerComponent) || cn1GrabbedDrag) {
                        // It's not a peer component, so we should pass the event to the canvas
                        e = SwingUtilities.convertMouseEvent(this, e, instance.canvas);
                        switch (e.getID()) {
                            case MouseEvent.MOUSE_CLICKED:
                                instance.canvas.mouseClicked(e);
                                break;
                            case MouseEvent.MOUSE_DRAGGED:
                                instance.canvas.mouseDragged(e);
                                break;
                            case MouseEvent.MOUSE_MOVED:
                                instance.canvas.mouseMoved(e);
                                break;
                            case MouseEvent.MOUSE_PRESSED:
                                // Mouse pressed in native component - passed to lightweight cmp
                                if (!(cmp instanceof PeerComponent)) {
                                    instance.cn1GrabbedDrag = true;
                                }
                                instance.canvas.mousePressed(e);
                                break;
                            case MouseEvent.MOUSE_RELEASED:
                                instance.cn1GrabbedDrag = false;
                                instance.canvas.mouseReleased(e);
                                break;
                            case MouseEvent.MOUSE_WHEEL:
                                instance.canvas.mouseWheelMoved((MouseWheelEvent)e);
                                break;
                                
                        }
                        //return true;
                        if (instance.cn1GrabbedDrag) {
                            return true;
                        }
                        if (cmp instanceof PeerComponent) {
                            return false;
                        }
                        return true;
                    //}
                }
            }
            if (e.getID() == MouseEvent.MOUSE_RELEASED) {
                instance.cn1GrabbedDrag = false;
                peerGrabbedDrag = false;
            } else if (e.getID() == MouseEvent.MOUSE_PRESSED) {
                peerGrabbedDrag = true;
            }
            return false;
        }
        
        private int getCN1X(MouseEvent e) {
            if (instance.canvas == null) {
                int out = e.getXOnScreen();
                if (out == 0) {
                    // For some reason the web browser would return 0 for screen coordinates
                    // but would still have correct values for getX() and getY() when 
                    // dealing with mouse wheel events.  In these cases we need to 
                    // get the screen coordinate from the component
                    // and add it to the relative coordinate.
                    out = e.getX(); // In some cases absX is set to zero for mouse wheel events
                    Object source = e.getSource();
                    if (source instanceof java.awt.Component) {
                        Point pt = ((java.awt.Component)source).getLocationOnScreen();
                        out  += pt.x;
                    }
                }
                return out;
            }
            java.awt.Rectangle screenCoords = instance.getScreenCoordinates();
            if (screenCoords == null) {
                screenCoords = new java.awt.Rectangle(0, 0, 0, 0);
            }
            int x = e.getXOnScreen();
            if (x == 0) {
                // For some reason the web browser would return 0 for screen coordinates
                // but would still have correct values for getX() and getY() when 
                // dealing with mouse wheel events.  In these cases we need to 
                // get the screen coordinate from the component
                // and add it to the relative coordinate.
                x = e.getX();
                Object source = e.getSource();
                if (source instanceof java.awt.Component) {
                    Point pt = ((java.awt.Component)source).getLocationOnScreen();
                    x += pt.x;
                }
            }
            
            double zoom = zoom_ > 0 ? zoom_ : instance.zoomLevel;
            return (int)((x - instance.canvas.getLocationOnScreen().x - (instance.canvas.x + screenCoords.x) * zoom / retinaScale) / zoom * retinaScale);
        }

        private int getCN1Y(MouseEvent e) {
            if (instance.canvas == null) {
                int out = e.getYOnScreen();
                if (out == 0) {
                    // For some reason the web browser would return 0 for screen coordinates
                    // but would still have correct values for getX() and getY() when 
                    // dealing with mouse wheel events.  In these cases we need to 
                    // get the screen coordinate from the component
                    // and add it to the relative coordinate.
                    out = e.getY();
                    Object source = e.getSource();
                    if (source instanceof java.awt.Component) {
                        Point pt = ((java.awt.Component)source).getLocationOnScreen();
                        out  += pt.y;
                    }
                }
                return out;
            }
            java.awt.Rectangle screenCoords = instance.getScreenCoordinates();
            if (screenCoords == null) {
                screenCoords = new java.awt.Rectangle(0, 0, 0, 0);
            }
            int y = e.getYOnScreen();
            if (y == 0) {
                // For some reason the web browser would return 0 for screen coordinates
                // but would still have correct values for getX() and getY() when 
                // dealing with mouse wheel events.  In these cases we need to 
                // get the screen coordinate from the component
                // and add it to the relative coordinate.
                y = e.getY();
                Object source = e.getSource();
                if (source instanceof java.awt.Component) {
                    Point pt = ((java.awt.Component)source).getLocationOnScreen();
                    y += pt.y;
                }
            }
            double zoom = zoom_ > 0 ? zoom_ : instance.zoomLevel;
            return (int)((y - instance.canvas.getLocationOnScreen().y - (instance.canvas.y + screenCoords.y) * zoom / retinaScale) / zoom * retinaScale);

        }
        
        public void setZoom(double zoom) {
            zoom_ = zoom;
        }
        
        public CN1JPanel() {
            setBorder(new EmptyBorder(0, 0, 0, 0));
        }

        
    }
    
    

    @Override
    public AsyncResource<Media> createMediaAsync(String uriAddress, final boolean isVideo, final Runnable onCompletion) {
        throw new UnsupportedOperationException("Not implemented");
        
    }
    
    public String getMimetype(File file) throws IOException {
       return Files.probeContentType(file.toPath());
    }
    
    
    public String guessSuffixForMimetype(String mimeType) {
        String suffix = "";
        if (mimeType.contains("mp3") || mimeType.contains("audio/mpeg")) {
            suffix = ".mp3";
        } else if (mimeType.contains("wav")) {
            suffix = ".wav";
        }
        if (mimeType.contains("aiff")) {
            suffix = ".aiff";
        }
        if (mimeType.contains("amr")) {
            suffix = ".amr";
        }
        if (mimeType.contains("aiff")) {
            suffix = ".aiff";
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
        return suffix;
    }
    
    /**
     * Plays the sound in the given URI which is partially platform specific.
     *
     * @param uriAddress the platform specific location for the sound
     * @param onCompletion invoked when the audio file finishes playing, may be
     * null
     * @return a handle that can be used to control the playback of the audio
     * @throws java.io.IOException if the URI access fails
     */
    public Media createMedia(String uriAddress, final boolean isVideo, final Runnable onCompletion) throws IOException {
        AsyncResource<Media> res = createMediaAsync(uriAddress, isVideo, onCompletion);
        try {
            return res.get();
        } catch (Throwable t) {
            Throwable cause = t.getCause();
            if (cause instanceof IOException) {
                throw (IOException)cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException)cause;
            }
            throw new IOException(cause);
        }
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
    @Override
    public AsyncResource<Media> createMediaAsync(final InputStream stream, final String mimeType, final Runnable onCompletion) {
        throw new UnsupportedOperationException("Not implemented");
        
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
        try {
            return createMediaAsync(stream, mimeType, onCompletion).get();
        } catch (Throwable t) {
            t = t.getCause();
            if (t instanceof IOException) {
                throw (IOException)t;
            } else if (t instanceof RuntimeException) {
                throw (RuntimeException)t;
            } else {
                throw new IOException(t);
            }
        }
    }

    
    
    
    
    private class NativeScreenGraphics {

        BufferedImage sourceImage;
        Graphics2D cachedGraphics;
        Transform transform;
        LinkedList<Shape> clipStack = new LinkedList<Shape>();
    }
    
    private Object lastNativeGraphics;
    private Transform lastNativeGraphicsTransform;
    
    private void setNativeScreenGraphicsTransform(Object nativeGraphics, com.codename1.ui.Transform transform){
        if ( nativeGraphics instanceof NativeScreenGraphics ){
            ((NativeScreenGraphics)nativeGraphics).transform = transform;
        } else {
            lastNativeGraphics = nativeGraphics;
            lastNativeGraphicsTransform = transform;
        }
    }
    
    private com.codename1.ui.Transform getNativeScreenGraphicsTransform(Object nativeGraphics){
        if ( nativeGraphics instanceof NativeScreenGraphics ){
            return ((NativeScreenGraphics)nativeGraphics).transform;
        } else if (lastNativeGraphics == nativeGraphics) {
            return lastNativeGraphicsTransform;
        }
        return null;
    }

    @Override
    public boolean isAffineSupported() {
        checkEDT();
        return true;
    }

    
    /*@Override
    public void drawShape(Object graphics, com.codename1.ui.geom.Shape shape, com.codename1.ui.Stroke stroke) {
    }
    
    @Override
    public void fillShape(Object graphics, com.codename1.ui.geom.Shape shape) {
    }
    
    @Override
    public Matrix getTransform(Object graphics){
        NativeScreenGraphics ng = ((NativeScreenGraphics)graphics;
        return Matrix.makeIdentity();
    }
    
    @Override
    public boolean isTransformSupported(Object graphics){
        checkEDT();
        return true;
    }

    @Override
    public boolean isPerspectiveTransformSupported(Object graphics){
        checkEDT();
        return false;
    }
    
    @Override
    public boolean isShapeSupported(Object graphics){
        checkEDT();
        return true;
    }*/
    
    
    public void resetAffine(Object nativeGraphics) {
        checkEDT();
        setTransform(nativeGraphics, com.codename1.ui.Transform.makeIdentity());
        /*
        Graphics2D g = getGraphics(nativeGraphics);
        g.setTransform(new AffineTransform());
        if (zoomLevel != 1 && g != nativeGraphics) {
            g.setTransform(AffineTransform.getScaleInstance(zoomLevel, zoomLevel));
        }*/
    }

    public void scale(Object nativeGraphics, float x, float y) {
        checkEDT();
        //Graphics2D g = getGraphics(nativeGraphics);
        //g.scale(x, y);
        com.codename1.ui.Transform tf = getTransform(nativeGraphics);
        tf.scale(x, y);
        setTransform(nativeGraphics, tf);
    }

    public void rotate(Object nativeGraphics, float angle) {
        /*
        checkEDT();
        Graphics2D g = getGraphics(nativeGraphics);
        g.rotate(angle);
        */
        com.codename1.ui.Transform tf = getTransform(nativeGraphics);
        tf.rotate(angle, 0, 0);
        setTransform(nativeGraphics, tf);
        
    }

    public void rotate(Object nativeGraphics, float angle, int pX, int pY) {
        checkEDT();
        //Graphics2D g = getGraphics(nativeGraphics);
        //g.rotate(angle, pX, pY);
        com.codename1.ui.Transform tf = getTransform(nativeGraphics);
        tf.rotate(angle, pX, pY);
        setTransform(nativeGraphics, tf);
    }

    public void shear(Object nativeGraphics, float x, float y) {
        checkEDT();
        Graphics2D g = getGraphics(nativeGraphics);
        g.shear(x, y);
    }

    public boolean isTablet() {
        return tablet || isDesktop();
    }

    public boolean isDesktop() {
        return portraitSkin == null;
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
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                   RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        } else {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                   RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
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

    public Object getStorageData() {
        return appHomeDir;
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
            storageDir = new File(System.getProperty("user.home"), ((String) getStorageData()));
            storageDir.mkdirs();
        }
        return storageDir;
    }

    @Override
    public boolean isTimeoutSupported() {
        return true;
    }

    @Override
    public void setTimeout(int t) {
        timeout = t;
    }

    boolean warnAboutHttpChecked;
    boolean warnAboutHttp = true;
    
    /**
     * @inheritDoc
     */
    public Object connect(String url, boolean read, boolean write, int timeout) throws IOException {
        if(disconnectedMode && url.toLowerCase().startsWith("http")) {
            throw new IOException("Unreachable");
        }
        if(url.toLowerCase().startsWith("http:"))  {
            if(!warnAboutHttpChecked) {
                warnAboutHttpChecked = true;
                Map<String, String> m = getProjectBuildHints();
                if(m != null) {
                    String s = m.get("ios.plistInject");
                    if(s != null && s.contains("NSAppTransportSecurity")) {
                        warnAboutHttp = false;
                    }
                }
            }
            if(warnAboutHttp && url.indexOf("localhost") < 0 && url.indexOf("127.0.0") < 0) {
                System.out.println("WARNING: Apple will no longer accept http URL connections from applications you tried to connect to " + 
                        url +" to learn more check out https://www.codenameone.com/blog/ios-http-urls.html" );
            }
        }
        URL u = new URL(url);        

        URLConnection con = u.openConnection();

        if (con instanceof HttpURLConnection) {
            HttpURLConnection c = (HttpURLConnection) con;
            c.setUseCaches(false);
            c.setDefaultUseCaches(false);
            c.setInstanceFollowRedirects(false);
            if(timeout > -1) {
                c.setConnectTimeout(timeout);
            }
        }

        con.setDoInput(read);
        con.setDoOutput(write);
        if (netMonitor != null) {
            NetworkRequestObject nr = new NetworkRequestObject();
            if (nr != null) {
                nr.setUrl(url);
                nr.setTimeSent(System.currentTimeMillis());
            }
            netMonitor.addRequest(con, nr);
        }
        return con;
    }

    @Override
    public void setReadTimeout(Object connection, int readTimeout) {
        if (connection instanceof URLConnection) {
            ((URLConnection)connection).setReadTimeout(readTimeout);
        }
    }

    @Override
    public boolean isReadTimeoutSupported() {
        return true;
    }

    
    
    
    @Override
    public void addConnectionToQueue(ConnectionRequest req) {
        super.addConnectionToQueue(req);
        if (netMonitor != null) {
            NetworkRequestObject o = new NetworkRequestObject();
            o.setTimeQueued(System.currentTimeMillis());
            netMonitor.addQueuedRequest(req, o);
        }
    }

    @Override
    public void setInsecure(Object connection, boolean insecure) {
        if (insecure) {
            if (connection instanceof HttpsURLConnection) {
                HttpsURLConnection conn = (HttpsURLConnection)connection;
                try {
                    TrustModifier.relaxHostChecking(conn);
                } catch (Exception ex) {
                    Log.e(ex);
                }
            }
        }
    }

    
    
    
    
    @Override
    public void setConnectionId(Object connection, int id) {
        super.setConnectionId(connection, id); 
        if (netMonitor != null && connection instanceof URLConnection) {
            NetworkRequestObject queuedRequest = netMonitor.findQueuedRequest(id);
            if (queuedRequest != null) {
                NetworkRequestObject existingRequest = netMonitor.getByConnection((URLConnection)connection);
                if (existingRequest != null) {
                    existingRequest.setTimeQueued(queuedRequest.getTimeQueued());
                } else {
                    netMonitor.addRequest((URLConnection)connection, queuedRequest);
                }
                netMonitor.removeQueuedRequest(queuedRequest);
            }
        }
    }
    
    
   
    /**
     * @inheritDoc
     */
    public Object connect(String url, boolean read, boolean write) throws IOException {
        return connect(url, read, write, timeout);
    }

    private static final char[] HEX_CHARS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    
    private static String dumpHex(byte[] data) {
        final int n = data.length;
        final StringBuilder sb = new StringBuilder(n * 3 - 1);
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(HEX_CHARS[(data[i] >> 4) & 0x0F]);
            sb.append(HEX_CHARS[data[i] & 0x0F]);
        }
        return sb.toString();
    }

    @Override
    public String[] getSSLCertificates(Object connection, String url) throws IOException {
        if (connection instanceof HttpsURLConnection) {
            HttpsURLConnection conn = (HttpsURLConnection)connection;
            
            try {    
                conn.connect();
                java.security.cert.Certificate[] certs = conn.getServerCertificates();
                String[] out = new String[certs.length*2];
                int i=0;
                for (java.security.cert.Certificate cert : certs) {
                    {
                        MessageDigest md = MessageDigest.getInstance("SHA-256");
                        md.update(cert.getEncoded());
                        out[i++] = "SHA-256:" + dumpHex(md.digest());
                    }
                    {
                        MessageDigest md = MessageDigest.getInstance("SHA1");
                        md.update(cert.getEncoded());
                        out[i++] = "SHA1:" + dumpHex(md.digest());
                    }

                }
                return out;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return new String[0];
        
    }

    @Override
    public boolean canGetSSLCertificates() {
        return true;
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
    
    /**
     * @inheritDoc
     */
    public void setChunkedStreamingMode(Object connection, int bufferLen){    
        HttpURLConnection con = ((HttpURLConnection) connection);
        con.setChunkedStreamingMode(bufferLen);
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
    
    private NetworkRequestObject getByConnection(URLConnection con) {
        if(netMonitor != null) {
            return netMonitor.getByConnection((URLConnection) con);
        }
        return null;
    }

    /**
     * @inheritDoc
     */
    public OutputStream openOutputStream(Object connection) throws IOException {
        if (connection instanceof String) {
            FileOutputStream fc = new FileOutputStream(unfile((String) connection));
            BufferedOutputStream o = new BufferedOutputStream(fc, (String) connection);
            return o;
        }
        if (netMonitor != null || slowConnectionMode || disconnectedMode) {
            final NetworkRequestObject nr = getByConnection((URLConnection) connection);
            if (nr != null || slowConnectionMode || disconnectedMode) {
                if(disconnectedMode) {
                    throw new IOException("Unreachable");
                }
                if(nr != null) {
                    nr.setRequestBody("");
                }
                HttpURLConnection con = (HttpURLConnection) connection;
                OutputStream o = new BufferedOutputStream(con.getOutputStream()) {

                    public void write(byte b[], int off, int len) throws IOException {
                        super.write(b, off, len);
                        if(nr != null) {
                            nr.setRequestBody(nr.getRequestBody() + new String(b, off, len));
                        }
                        if(slowConnectionMode) {
                            try {
                                Thread.sleep(250);
                            } catch(Exception e) {}
                        }
                        if(disconnectedMode) {
                            throw new IOException("Unreachable");
                        }
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
        RandomAccessFile rf = new RandomAccessFile(unfile((String) connection), "rw");
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
            FileInputStream fc = new FileInputStream(unfile((String) connection));
            BufferedInputStream o = new BufferedInputStream(fc, (String) connection);
            return o;
        }
        if (netMonitor != null || slowConnectionMode || disconnectedMode) {
            final NetworkRequestObject nr = getByConnection((URLConnection) connection);
            if (nr != null || slowConnectionMode || disconnectedMode) {
                if(slowConnectionMode) {
                    try {
                        Thread.sleep(1000);
                    } catch(Exception e) {}
                }
                if (nr != null) {
                    nr.setTimeServerResponse(System.currentTimeMillis());
                }
                if(disconnectedMode) {
                    throw new IOException("Unreachable");
                }
                HttpURLConnection con = (HttpURLConnection) connection;
                String headers = "";
                Map<String, List<String>> map = con.getHeaderFields();
                for (String header : map.keySet()) {
                    headers += header + "=" + map.get(header) + "\n";
                }
                if(nr != null) {
                    nr.setResponseHeaders(headers);
                    nr.setResponseBody("");
                }
                InputStream is;
                if(con.getResponseCode() >= 200 && con.getResponseCode() < 300){
                    is = con.getInputStream();
                }else{
                    is = con.getErrorStream();
                }
                boolean isText = false;
                String contentType = con.getContentType();
                if (contentType != null) {
                    if (contentType.startsWith("text/") || contentType.contains("json") || contentType.contains("css") || contentType.contains("javascript")) {
                        isText = true;
                    }
                }
                final boolean fIsText = isText;
                InputStream i = new BufferedInputStream(is) {

                    public synchronized int read(byte b[], int off, int len)
                            throws IOException {
                        int s = super.read(b, off, len);
                        if(nr != null) {
                            if (fIsText && s > -1) {
                                nr.setResponseBody(nr.getResponseBody() + new String(b, off, len));
                            }
                        }
                        if(slowConnectionMode) {
                            try {
                                Thread.sleep(len);
                            } catch(Exception e) {}
                        }
                        if(disconnectedMode) {
                            throw new IOException("Unreachable");
                        }
                        return s;
                    }

                    @Override
                    public void close() throws IOException {
                        super.close();
                        if (nr != null) {
                            nr.setTimeComplete(System.currentTimeMillis());
                        }
                    }
                    
                    
                };
                return i;
            }
        }
        if(connection instanceof HttpURLConnection) {
            HttpURLConnection ht = (HttpURLConnection)connection;
            if(ht.getResponseCode() < 400) {
                return new BufferedInputStream(ht.getInputStream());
            }
            return new BufferedInputStream(ht.getErrorStream());
        } else {
            return new BufferedInputStream(((URLConnection) connection).getInputStream());
        }        
    }

    /**
     * @inheritDoc
     */
    public void setHttpMethod(Object connection, String method) throws IOException {
        if (netMonitor != null) {
            NetworkRequestObject nr = netMonitor.getByConnection((URLConnection) connection);
            if (nr != null) {
                nr.setMethod(method.toUpperCase());
            }
        }
        if(method.equalsIgnoreCase("patch")) {
            allowPatch((HttpURLConnection) connection);
        }
        ((HttpURLConnection) connection).setRequestMethod(method);
    }
    
    // the following block is based on a few suggestions in this stack overflow 
    // answer https://stackoverflow.com/questions/25163131/httpurlconnection-invalid-http-method-patch
    private static boolean enabledPatch;
    private static boolean patchFailed;
    private static void allowPatch(HttpURLConnection connection) {
        if(enabledPatch) {
            return;
        }
        if(patchFailed) {
            connection.setRequestProperty("X-HTTP-Method-Override", "PATCH");
            return;
        }
        try {
            Field methodsField = HttpURLConnection.class.getDeclaredField("methods");

            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(methodsField, methodsField.getModifiers() & ~Modifier.FINAL);

            methodsField.setAccessible(true);

            String[] oldMethods = (String[]) methodsField.get(null);
            Set<String> methodsSet = new LinkedHashSet<String>(Arrays.asList(oldMethods));
            methodsSet.addAll(Arrays.asList("PATCH"));
            String[] newMethods = methodsSet.toArray(new String[0]);

            methodsField.set(null/*static field*/, newMethods);
            enabledPatch = true;
        } catch (NoSuchFieldException e) {
            patchFailed = true;
            connection.setRequestProperty("X-HTTP-Method-Override", "PATCH");
        } catch(IllegalAccessException ee) {
            patchFailed = true;
            connection.setRequestProperty("X-HTTP-Method-Override", "PATCH");
        }
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
        if (netMonitor != null || slowConnectionMode || disconnectedMode) {
            if(slowConnectionMode) {
                try {
                    Thread.sleep(250);
                } catch(Exception e) {}
            }
            if(disconnectedMode) {
                throw new IOException("Unreachable");
            }
            if(netMonitor != null) {
                NetworkRequestObject nr = netMonitor.getByConnection((URLConnection) connection);
                if (nr != null) {
                    nr.setResponseCode("" + code);
                }
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
        List<String> headers = new ArrayList<String>();
        
        // we need to merge headers with differing case since this should be case insensitive
        for(String key : c.getHeaderFields().keySet()) {
            if(key != null && key.equalsIgnoreCase(name)) {
                headers.addAll(c.getHeaderFields().get(key));
            }
        }
        if (headers.size() > 0) {
            List<String> v = new ArrayList<String>();
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
        if (name.indexOf('/') > -1) {
            throw new IOException("Illegal charcter '/' in storage name: " + name);
        }
        if (name.indexOf('\\') > -1) {
            throw new IOException("Illegal charcter '\\' in storage name: " + name);
        }
        if (name.indexOf('*') > -1) {
            throw new IOException("Illegal charcter '*' in storage name: " + name);
        }
        if (name.indexOf('?') > -1) {
            throw new IOException("Illegal charcter '?' in storage name: " + name);
        }
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
    public int getStorageEntrySize(String name) {
        return (int)new File(getStorageDir(), name).length();
    }
    
    /**
     * @inheritDoc
     */
    public String[] listFilesystemRoots() {
        if(!checkForPermission("android.permission.READ_EXTERNAL_STORAGE", "This is required to browse the file system")){
            return new String[]{};
        }
        if(exposeFilesystem) {
            File[] f = File.listRoots();
            String[] roots = new String[f.length];
            for (int iter = 0; iter < f.length; iter++) {
                roots[iter] = f[iter].getAbsolutePath();
            }
            return roots;
        }
        return new String[] {getAppHomePath()};
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
    
    protected String unfile(String file) {
        
        if(!exposeFilesystem){
            if(!file.startsWith("file:/")){
                throw new IllegalArgumentException( file + " is not a valid path, use "
                        + "FileSystemStorage.getInstance().getAppHomePath() to get a valid dir path to read/write files");
            }
        } else {
            if(file.indexOf("%") > 0) {
                // this is an encoded file URL convert it to a regular file
                try {
                    File f = new File(new URI(file));
                    return f.getAbsolutePath();
                } catch(Exception err) {
                    Log.e(err);
                    throw new RuntimeException(err);
                }
            }
        }
        
        if(file.startsWith("file://home")) {
            return System.getProperty("user.home").replace('\\', '/') + File.separator + appHomeDir + file.substring(11).replace('/', File.separatorChar);
        }
        if (file.startsWith("file:///")) {
            return file.substring(7);
        }
        if (file.startsWith("file://")) {
            return file.substring(6);
        }
        if (file.startsWith("file:/")) {
            return file.substring(5);
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
    public long getFileLastModified(String file) {
        return new File(unfile(file)).lastModified();
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
        return '/';
    }
    
    public String getLineSeparator() {
        return System.lineSeparator();
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
        try {
            return new File(unfile(file)).exists();            
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @inheritDoc
     */
    public void rename(String file, String newName) {
        File f = new File(unfile(file));
        f.renameTo(new File(f.getParentFile(), newName));
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
        if(getSkin() == null) {
            if(IS_MAC) {
                return "mac";
            }
            return "win";
        }
        return platformName;
    }

    /**
     * @inheritDoc
     */
    public String[] getPlatformOverrides() {
        if(isDesktop()) {
            return new String[] {"desktop", "tablet"};
        }
        return platformOverrides;
    }

    public LocationManager getLocationManager() {
        if(!checkForPermission("android.permission.ACCESS_FINE_LOCATION", "This is required to get the location")){
            return null;
        }
        // the location simulation should ONLY apply to the simulator and not to JavaSE port, designer etc.
        if(portraitSkin != null) {
            return StubLocationManager.getLocationManager();
        }
        return new LocationManager() {
            
            
            
            @Override
            public Location getCurrentLocation() throws IOException {
                return new Location();
            }

            @Override
            public Location getLastKnownLocation() {
                return new Location();
            }

            @Override
            protected void bindListener() {
            }

            @Override
            protected void clearListener() {
            }

            
            
        };
    }

    @Override
    public void sendMessage(String[] recieptents, String subject, Message msg) {
        if(recieptents != null){
            try {
                String mailto = "mailto:" + recieptents[0];
                for(int iter = 1 ; iter < recieptents.length ; iter++) {
                    mailto += "," + recieptents[iter];
                }
                mailto += "?body=" + Util.encodeUrl(msg.getContent()) + "&subject=" + Util.encodeUrl(subject);
                Desktop.getDesktop().mail(new URI(mailto));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            System.out.println("sending message to " + recieptents[0]);
        }
    }

    @Override
    public void sendSMS(final String phoneNumber, final String message, boolean i) throws IOException {
        if(!checkForPermission("android.permission.SEND_SMS", "This is required to send a SMS")){
            return;
        }
        if(!checkForPermission("android.permission.READ_PHONE_STATE", "This is required to send a SMS")){
            return;
        }
        System.out.println("sending sms to " + phoneNumber);
    }

    @Override
    public int getSMSSupport() {
        return Display.SMS_NOT_SUPPORTED;
    }

    @Override
    public void dial(String phoneNumber) {
        System.out.println("dialing to " + phoneNumber);
    }
    
    
    @Override
    public String[] getAllContacts(boolean withNumbers) {
        if(!checkForPermission("android.permission.READ_CONTACTS", "This is required to get the contacts")){
            return new String[]{};
        }
        if(contacts == null){
            contacts = initContacts();
        }
        Enumeration keys = contacts.keys();
        String [] ids = new String[contacts.size()];
        int index = 0;
        while(keys.hasMoreElements()){
            ids[index++] = (String)keys.nextElement();
        }
        return ids;
    }

    @Override
    public Contact getContactById(String id) {
        if(!checkForPermission("android.permission.READ_CONTACTS", "This is required to get the contacts")){
            return null;
        }
        if(contacts == null){
            contacts = initContacts();
        }
        return (Contact) contacts.get(id);
    }
    
    @Override
    public Contact getContactById(String id, boolean includesFullName, boolean includesPicture,
            boolean includesNumbers, boolean includesEmail, boolean includeAddress) {

        if(!checkForPermission("android.permission.READ_CONTACTS", "This is required to get the contacts")){
            return null;
        }
        Contact c = new Contact();
        Contact contact = getContactById(id);
        c.setId(contact.getId());
        c.setDisplayName(contact.getDisplayName());
        
        if(includesPicture){
            c.setPhoto(contact.getPhoto());
        }
        
        if (includesFullName) {
            c.setFirstName(contact.getFirstName());
            c.setFamilyName(contact.getFamilyName());
        }
        if (includesNumbers) {
            c.setPhoneNumbers(contact.getPhoneNumbers());            
        }
        if(includesEmail){
            c.setEmails(contact.getEmails());
        }
        if(includeAddress){
            c.setAddresses(contact.getAddresses());
        }
        
        return c;
    }

    public String createContact(String firstName, String familyName, String officePhone, String homePhone, String cellPhone, String email) {
        if(!checkForPermission("android.permission.WRITE_CONTACTS", "This is required to create a contact")){
            return null;
        }
        if(contacts == null){
            contacts = initContacts();
        }
        //get a unique id for the new contact
        String id = "" + contacts.size();
        while (contacts.get(id) != null) {
            id = "" + (contacts.size() + 1);
        }
        Contact contact = new Contact();
        contact.setId(id);

        String displayName = "";
        if (firstName != null) {
            displayName += firstName;
        }
        if (familyName != null) {
            displayName += " " + familyName;
        }
        contact.setDisplayName(displayName);
        contact.setFirstName(firstName);
        contact.setFamilyName(familyName);

        Hashtable phones = new Hashtable();
        if(cellPhone != null){
            phones.put("mobile", cellPhone);
            contact.setPrimaryPhoneNumber(cellPhone);
        }
        if(homePhone != null){
            phones.put("home", homePhone);
        }
        if(officePhone != null){
            phones.put("work", officePhone);
        }
        
        contact.setPhoneNumbers(phones);
        if(email != null){
            Hashtable emails = new Hashtable();
            emails.put("work", email);
            contact.setEmails(emails);
            contact.setPrimaryEmail(email);
        }
        contacts.put(id, contact);
        return id;
    }

    public boolean deleteContact(String id) {
        if(!checkForPermission("android.permission.WRITE_CONTACTS", "This is required to delete a contact")){
            return false;
        }
        if(contacts == null){
            contacts = initContacts();
        }
        return contacts.remove(id) != null;
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

    @Override
    public void setCurrentAccessPoint(String id) {
        //this.currentAp = id;
        super.setCurrentAccessPoint(id);
    }

    @Override
    public void openImageGallery(final com.codename1.ui.events.ActionListener response){    
        if(!checkForPermission("android.permission.WRITE_EXTERNAL_STORAGE", "This is required to browse the photos")){
            return;
        }
        capturePhoto(response);
    }
    
    
    private String getGlobsForExtensions(String[] extensions, String separator){
        StringBuilder sb = new StringBuilder();
        for (String ext : extensions){
            sb.append("*.").append(ext).append(separator);
        }
        return sb.substring(0, sb.length()-separator.length());
    }
    
    @Override
    public boolean isGalleryTypeSupported(int type) {
        if (super.isGalleryTypeSupported(type)) {
            return true;
        }
        switch (type) {
            case -9999:
            case -9998:
            case Display.GALLERY_IMAGE_MULTI:
            case Display.GALLERY_VIDEO_MULTI:
            case Display.GALLERY_ALL_MULTI:
                return true;
        }
        
        return false;
    }
    
    @Override
    public void openGallery(final com.codename1.ui.events.ActionListener response, int type){
        if(!checkForPermission("android.permission.WRITE_EXTERNAL_STORAGE", "This is required to browse the photos")){
            return;
        }
        if (!isGalleryTypeSupported(type)) {
            throw new IllegalArgumentException("Gallery type "+type+" not supported on this platform.");
        }
        boolean multi=false;
        if (type == -9998) {
            multi=true;
            type = -9999;
        }
        
        if (type == Display.GALLERY_IMAGE_MULTI) {
            checkGalleryMultiselect();
            checkPhotoLibraryUsageDescription();
            captureMulti(response, imageExtensions, getGlobsForExtensions(imageExtensions, ";"));
        }else if(type == Display.GALLERY_VIDEO_MULTI){
            checkGalleryMultiselect();
            checkAppleMusicUsageDescription();
            captureMulti(response, videoExtensions, getGlobsForExtensions(videoExtensions, ";"));
        }else if(type == Display.GALLERY_ALL_MULTI) {
            checkGalleryMultiselect();
            checkPhotoLibraryUsageDescription();
            checkAppleMusicUsageDescription();
            String[] exts = new String[videoExtensions.length+imageExtensions.length];
            System.arraycopy(videoExtensions, 0, exts,0, videoExtensions.length);
            System.arraycopy(imageExtensions, 0, exts, videoExtensions.length, imageExtensions.length);
            captureMulti(response, exts, getGlobsForExtensions(exts, ";"));
        }
        else if(type == Display.GALLERY_VIDEO){
            checkAppleMusicUsageDescription();
            capture(response, videoExtensions, getGlobsForExtensions(videoExtensions, ";"));
        }else if(type == Display.GALLERY_IMAGE){
            checkPhotoLibraryUsageDescription();
            capture(response, imageExtensions, getGlobsForExtensions(imageExtensions, ";"));
        } else if (type==-9999) {
            checkPhotoLibraryUsageDescription();
            checkAppleMusicUsageDescription();
            String[] exts = Display.getInstance().getProperty("javase.openGallery.accept", "").split(",");
            if (multi) {
                captureMulti(response, exts, getGlobsForExtensions(exts, ";"));
            } else {
                capture(response, exts, getGlobsForExtensions(exts, ";"));
            }
        }else{
            checkPhotoLibraryUsageDescription();
            checkAppleMusicUsageDescription();
            String[] exts = new String[videoExtensions.length+imageExtensions.length];
            System.arraycopy(videoExtensions, 0, exts,0, videoExtensions.length);
            System.arraycopy(imageExtensions, 0, exts, videoExtensions.length, imageExtensions.length);
            
            capture(response, exts, getGlobsForExtensions(exts, ";"));
        }
    }

    private boolean richPushBuildHintsChecked;
    
    public void checkRichPushBuildHints() {
        if (!richPushBuildHintsChecked) {
            richPushBuildHintsChecked = true;
            Map<String, String> m = Display.getInstance().getProjectBuildHints();
            if(m != null) {
                if(!m.containsKey("ios.useNotificationServiceExtension")) {
                    Display.getInstance().setProjectBuildHint("ios.useNotificationServiceExtension", "true");
                }
            }
        }
    }
    
    private boolean cameraUsageDescriptionChecked;
    
    private void checkCameraUsageDescription() {
        if (!cameraUsageDescriptionChecked) {
            cameraUsageDescriptionChecked = true;
            
            Map<String, String> m = Display.getInstance().getProjectBuildHints();
            if(m != null) {
                if(!m.containsKey("ios.NSCameraUsageDescription")) {
                    Display.getInstance().setProjectBuildHint("ios.NSCameraUsageDescription", "Some functionality of the application requires your camera");
                }
            }
        }
    }
    
    private boolean enableGalleryMultiselectChecked;
    private void checkGalleryMultiselect() {
        if (!enableGalleryMultiselectChecked) {
            enableGalleryMultiselectChecked = true;
            
            Map<String, String> m = Display.getInstance().getProjectBuildHints();
            if(m != null) {
                if(!m.containsKey("ios.enableGalleryMultiselect")) {
                    Display.getInstance().setProjectBuildHint("ios.enableGalleryMultiselect", "true");
                }
            }
        }
    }
    
    
    private boolean contactsUsageDescriptionChecked;
    
    private void checkContactsUsageDescription() {
        if (!contactsUsageDescriptionChecked) {
            contactsUsageDescriptionChecked = true;
            
            Map<String, String> m = Display.getInstance().getProjectBuildHints();
            if(m != null) {
                if(!m.containsKey("ios.NSContactsUsageDescription")) {
                    Display.getInstance().setProjectBuildHint("ios.NSContactsUsageDescription", "Some functionality of the application requires access to your contacts");
                }
            }
        }
    }
    
    private boolean photoLibraryUsageDescriptionChecked;
    
    private void checkPhotoLibraryUsageDescription() {
        if (!photoLibraryUsageDescriptionChecked) {
            photoLibraryUsageDescriptionChecked = true;
            
            Map<String, String> m = Display.getInstance().getProjectBuildHints();
            if(m != null) {
                if(!m.containsKey("ios.NSPhotoLibraryUsageDescription")) {
                    Display.getInstance().setProjectBuildHint("ios.NSPhotoLibraryUsageDescription", "Some functionality of the application requires access to your photo library");
                }
            }
        }
    }
    
    private void checkIosBackgroundFetch() {
        if (!iosBackgroundFetchChecked) {
            iosBackgroundFetchChecked = true;
            
            Map<String, String> m = Display.getInstance().getProjectBuildHints();
            if(m != null) {
                
                String existingModes = m.get("ios.background_modes");
                if (existingModes == null) {
                    existingModes = "fetch";
                } else if (!existingModes.contains("fetch")) {
                    existingModes += ",fetch";
                }
                Display.getInstance().setProjectBuildHint("ios.background_modes", existingModes);
                
            }
        }
    }
    
    private boolean iosBackgroundFetchChecked;
    
    private boolean appleMusicUsageDescriptionChecked;
    
    private void checkAppleMusicUsageDescription() {
        if (!appleMusicUsageDescriptionChecked) {
            appleMusicUsageDescriptionChecked = true;
            
            Map<String, String> m = Display.getInstance().getProjectBuildHints();
            if(m != null) {
                if(!m.containsKey("ios.NSAppleMusicUsageDescription")) {
                    Display.getInstance().setProjectBuildHint("ios.NSAppleMusicUsageDescription", "Some functionality of the application requires access to your media library");
                }
            }
        }
    }
    
    private boolean photoLibraryAddUsageDescriptionChecked;
    
    private void checkPhotoLibraryAddUsageDescription() {
        if (!photoLibraryAddUsageDescriptionChecked) {
            photoLibraryAddUsageDescriptionChecked = true;
            
            Map<String, String> m = Display.getInstance().getProjectBuildHints();
            if(m != null) {
                if(!m.containsKey("ios.NSPhotoLibraryAddUsageDescription")) {
                    Display.getInstance().setProjectBuildHint("ios.NSPhotoLibraryAddUsageDescription", "Some functionality of the application requires write-only access to your photo library");
                }
            }
        }
    }
    
    
    private boolean microphoneUsageDescriptionChecked;
    
    private void checkMicrophoneUsageDescription() {
        if (!microphoneUsageDescriptionChecked) {
            microphoneUsageDescriptionChecked = true;
            
            Map<String, String> m = Display.getInstance().getProjectBuildHints();
            if(m != null) {
                if(!m.containsKey("ios.NSMicrophoneUsageDescription")) {
                    Display.getInstance().setProjectBuildHint("ios.NSMicrophoneUsageDescription", "Some functionality of the application requires your microphone");
                }
            }
        }
    }
    
    @Override
    public void capturePhoto(final com.codename1.ui.events.ActionListener response) {
        if(!checkForPermission("android.permission.WRITE_EXTERNAL_STORAGE", "This is required to take a picture")){
            return;
        }
        checkCameraUsageDescription();
        capture(response, new String[] {"png", "jpg", "jpeg"}, "*.png;*.jpg;*.jpeg");
    }
    
    private void captureMulti(final com.codename1.ui.events.ActionListener response, final String[] imageTypes, final String desc) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                File[] selectedFiles = pickFiles(imageTypes, desc, true);
                ArrayList<String> resultList = new ArrayList<String>();
                com.codename1.ui.events.ActionEvent result = null;
                if(!exposeFilesystem) { 
                    if (selectedFiles != null) {
                        for (File selected : selectedFiles) {
                            try {
                                String ext = selected.getName();
                                int idx = ext.lastIndexOf(".");
                                if(idx > 0) {
                                    ext = ext.substring(idx);
                                } else {
                                    ext= imageTypes[0];
                                }
                                File tmp = selected;
                                if (!"true".equals(Display.getInstance().getProperty("openGallery.openFilesInPlace", "false"))) {
                                    tmp = File.createTempFile("temp", "." + ext);
                                    tmp.deleteOnExit();
                                    copyFile(selected, tmp);
                                }
                                resultList.add("file://" + tmp.getAbsolutePath().replace('\\', '/'));
                                //result = new com.codename1.ui.events.ActionEvent("file://" + tmp.getAbsolutePath().replace('\\', '/'));
                            } catch(IOException err) {
                                err.printStackTrace();
                            }
                        }
                    } 
                } else {
                    if(selectedFiles != null) {
                        for (File selected : selectedFiles) {
                            resultList.add("file://" + selected.getAbsolutePath().replace('\\', '/'));
                            //result = new com.codename1.ui.events.ActionEvent("file://" + selected.getAbsolutePath().replace('\\', '/'));
                        }
                    }
                }
                result = new com.codename1.ui.events.ActionEvent(resultList.toArray(new String[resultList.size()]));
                final com.codename1.ui.events.ActionEvent finalResult = result;
                Display.getInstance().callSerially(new Runnable() {

                    public void run() {
                        response.actionPerformed(finalResult);
                    }
                });
            }
        });
    }
    
    private void capture(final com.codename1.ui.events.ActionListener response, final String[] imageTypes, final String desc) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                File selected = pickFile(imageTypes, desc);

                com.codename1.ui.events.ActionEvent result = null;
                if(!exposeFilesystem) { 
                    if (selected != null) {
                        try {
                            String ext = selected.getName();
                            int idx = ext.lastIndexOf(".");
                            if(idx > 0) {
                                ext = ext.substring(idx);
                            } else {
                                ext= imageTypes[0];
                            }
                            if (ext.length() > 0 && ext.charAt(0) != '.') {
                                ext = "." + ext;
                            }
                            File tmp = selected;
                            if (!"true".equals(Display.getInstance().getProperty("openGallery.openFilesInPlace", "false"))) {
                                tmp = File.createTempFile("temp",  ext);
                                tmp.deleteOnExit();
                                copyFile(selected, tmp);
                            }
                            
                            result = new com.codename1.ui.events.ActionEvent("file://" + tmp.getAbsolutePath().replace('\\', '/'));
                        } catch(IOException err) {
                            err.printStackTrace();
                        }
                    } 
                } else {
                    if(selected != null) {
                        result = new com.codename1.ui.events.ActionEvent("file://" + selected.getAbsolutePath().replace('\\', '/'));
                    }
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

    @Override
    public void captureAudio(com.codename1.ui.events.ActionListener response) {
        if(!checkForPermission("android.permission.RECORD_AUDIO", "This is required to record the audio")){
            return;
        }
        checkMicrophoneUsageDescription();
        super.captureAudio(response); 
    }
    

    

    @Override
    public void captureVideo(com.codename1.ui.events.ActionListener response) {
        captureVideo(null, response);
    }

    private boolean includeVideoJS;
    
    @Override
    public void captureVideo(VideoCaptureConstraints constraints, com.codename1.ui.events.ActionListener response) {
        if(!checkForPermission("android.permission.WRITE_EXTERNAL_STORAGE", "This is required to take a video")){
            return;
        }
        if (constraints != null && !includeVideoJS) {
            includeVideoJS = true;
            Map<String, String> m = Display.getInstance().getProjectBuildHints();
            if(m != null) {
                if(!m.containsKey("javascript.includeVideoJS")) {
                    Display.getInstance().setProjectBuildHint("javascript.includeVideoJS", "true");
                }
            }
        }
        checkCameraUsageDescription();
        checkMicrophoneUsageDescription();
        capture(response, new String[] {"mp4", "avi", "mpg", "3gp"}, "*.mp4;*.avi;*.mpg;*.3gp");
    }
    
    
    
    
    
    

   

    public InputStream getResourceAsStream(Class cls, String resource) {
        if (!resource.startsWith("/")) {
            System.out.println("ERROR: resources must reside in the root directory thus must start with a '/' character in Codename One! Invalid resource: " + resource);
            return null;
        }
        if (resource.indexOf('/', 1) > -1) {
            System.out.println("ERROR: resources cannont be nested in directories in Codename One! Invalid resource: " + resource);
            return null;
        }
        
        if(resource.indexOf("notification_sound") > -1) {
            throw new RuntimeException("notification_sound is a reserved file name and can't be used in getResource()!");
        }
        
        if(resource.startsWith("raw")) {
            throw new RuntimeException("Files starting with 'raw' are reserved file names and can't be used in getResource()!");
        }
        if ("/theme.res".equals(resource)) {
            File srcThemeRes = new File(getSourceResourcesDir(),  "theme.res");
            if (srcThemeRes.exists()) {
                try {
                    return new FileInputStream(srcThemeRes);
                } catch (IOException err){
                    System.err.println("Failed to load "+srcThemeRes+" . "+err.getMessage());
                }
            }
        }
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

    @Override
    public void beforeComponentPaint(Component c, Graphics g) {
        if (perfMonitor != null) {
            perfMonitor.beforeComponentPaint(c);
        }
    }

    @Override
    public void afterComponentPaint(Component c, Graphics g) {
        if (perfMonitor != null) {
            perfMonitor.afterComponentPaint(c);
        }
    }
    
    @Override
    public void nothingWithinComponentPaint(Component c) {
        if (perfMonitor != null) {
            perfMonitor.nothingWithinComponentPaint(c);
        }
    }
    
    private L10NManager l10n;

    /**
     * @inheritDoc
     */
    public L10NManager getLocalizationManager() {
        if (l10n == null) {
            final Locale l = Locale.getDefault();
            l10n = new L10NManager(l.getLanguage(), l.getCountry()) {

                public double parseDouble(String localeFormattedDecimal) {
                    try {
                        return NumberFormat.getNumberInstance().parse(localeFormattedDecimal).doubleValue();
                    } catch (ParseException err) {
                        return Double.parseDouble(localeFormattedDecimal);
                    }
                }
                
                @Override
                public String getLongMonthName(Date date) {
                    java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("MMMM", l);
                    return fmt.format(date);
                }

                @Override
                public String getShortMonthName(Date date) {
                    java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("MMM", l);
                    return fmt.format(date);
                }
                
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

                public String formatDateTimeMedium(Date d) {
                    DateFormat dd = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
                    return dd.format(d);
                }

                public String formatDateTimeShort(Date d) {
                    DateFormat dd = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
                    return dd.format(d);
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
    public String[] getAvailableRecordingMimeTypes() {
        if (isMP3EncodingSupported()) {
            return new String[]{"audio/mp3", "audio/wav"};
        } else {
            return new String[]{"audio/wav"};
        }
    }

    private boolean isMP3EncodingSupported() {
        try {
            Class.forName("com.codename1.media.javase.MP3Encoder");
        } catch (ClassNotFoundException ex) {
            return false;
        }
        return FileEncoder.getEncoder("audio/wav", "audio/mp3") != null;
    }

    @Override
    public Media createMediaRecorder(MediaRecorderBuilder builder) throws IOException {
        
        return createMediaRecorder(builder.getPath(), builder.getMimeType(), builder.getSamplingRate(), builder.getBitRate(), builder.getAudioChannels(), 0, builder.isRedirectToAudioBuffer());
    }
    
    @Override
    public Media createMediaRecorder(final String path, String mime) throws IOException {
        MediaRecorderBuilder builder = new MediaRecorderBuilder()
                .path(path)
                .mimeType(mime);
        return createMediaRecorder(builder);
    }

    private  Media createMediaRecorder(final String path, String mime, final int samplingRate, final int bitRate, final int audioChannels, final int maxDuration, final boolean redirectToAudioBuffer) throws IOException {
        checkMicrophoneUsageDescription();
        if(!checkForPermission("android.permission.READ_PHONE_STATE", "This is required to access the mic")){
            return null;
        }

        if (!redirectToAudioBuffer) {
            if (mime == null) {
                if (path.endsWith(".wav") || path.endsWith(".WAV")) {
                    mime = "audio/wav";
                } else if (path.endsWith(".mp3") || path.endsWith(".MP3")) {
                    mime = "audio/mp3";
                }
            }
            if (mime == null) {
                mime = getAvailableRecordingMimeTypes()[0];
            }
            boolean foundMimetype = false;
            for (String mt : getAvailableRecordingMimeTypes()) {
                if (mt.equalsIgnoreCase(mime)) {
                    foundMimetype = true;
                    break;
                }


            }

            if (!foundMimetype) {
                throw new IOException("Mimetype "+mime+" not supported on this platform.  Use getAvailableMimetypes() to find out what is supported");
            }
        }
        final File file = redirectToAudioBuffer ? null : new File(unfile(path));
        if (!redirectToAudioBuffer) {
            if (!file.getParentFile().exists()) {
                throw new IOException("Cannot write file "+path+" because the parent directory does not exist.");
            }
        }
        File tmpFile = file;
        if (!redirectToAudioBuffer) {
            if (!"audio/wav".equalsIgnoreCase(mime) && !(tmpFile.getName().endsWith(".wav") || tmpFile.getName().endsWith(".WAV"))) {
                tmpFile = new File(tmpFile.getParentFile(), tmpFile.getName()+".wav");
            }
        }
        final File fTmpFile = tmpFile;
        final String fMime = mime;
        return new AbstractMedia() {
            java.io.File wavFile = fTmpFile;
            File outFile = file;
            AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;

            javax.sound.sampled.TargetDataLine line;
            boolean recording;
            
            javax.sound.sampled.AudioFormat getAudioFormat() {
                if (redirectToAudioBuffer) {
                    javax.sound.sampled.AudioFormat format = new javax.sound.sampled.AudioFormat(
                            samplingRate, 
                            16,
                            audioChannels,
                            true,
                            false
                    );
                    
                    return format;
                }
                float sampleRate = samplingRate;
                int sampleSizeInBits = 8;
                int channels = audioChannels;
                boolean signed = true;
                boolean bigEndian = false;
                javax.sound.sampled.AudioFormat format = new javax.sound.sampled.AudioFormat(sampleRate, sampleSizeInBits,
                                                     channels, signed, bigEndian);
                return format;
            }
            
            
            @Override
            protected void playImpl() {
                if (line == null) {
                    try {
                        final AudioFormat format = getAudioFormat();
                        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

                        if (!AudioSystem.isLineSupported(info)) {
                            fireMediaStateChange(State.Playing);
                            fireMediaStateChange(State.Paused);
                            
                            throw new RuntimeException("Failed to access microphone. Check that the microphone is connected and that the app has permission to use it.");
                        }
                        line = (TargetDataLine) AudioSystem.getLine(info);
                        line.open(format);
                        line.start();   // start capturing



                        recording = true;
                        fireMediaStateChange(State.Playing);
                        // start recording
                        new Thread(new Runnable() {
                            public void run() {
                                try {
                                    AudioInputStream ais = new AudioInputStream(line);
                                    if (redirectToAudioBuffer) {
                                        
                                        AudioBuffer buf = MediaManager.getAudioBuffer(path, true, 256);
                                        int maxBufferSize = buf.getMaxSize();
                                        float[] sampleBuffer = new float[maxBufferSize];
                                        byte[] byteBuffer = new byte[samplingRate * audioChannels];
                                        int bytesRead = -1;
                                        while ((bytesRead = ais.read(byteBuffer)) >= 0) {
                                            if (bytesRead > 0) {
                                                int sampleBufferPos = 0;
                                                
                                                for (int i = 0; i < bytesRead; i += 2) {
                                                    sampleBuffer[sampleBufferPos] = ((float)ByteBuffer.wrap(byteBuffer, i, 2)
                                                            .order(ByteOrder.LITTLE_ENDIAN)
                                                            .getShort())/ 0x8000;
                                                    sampleBufferPos++;
                                                    if (sampleBufferPos >= sampleBuffer.length) {
                                                        buf.copyFrom(samplingRate, audioChannels, sampleBuffer, 0, sampleBuffer.length);
                                                        sampleBufferPos = 0;
                                                    }
                                                
                                                }
                                                if (sampleBufferPos > 0) {
                                                    buf.copyFrom(samplingRate, audioChannels, sampleBuffer, 0, sampleBufferPos);
                                                }
                                            }
                                        }
                                    } else {
                                        AudioSystem.write(ais, fileType, wavFile);
                                    }
                                    
                                } catch (IOException ioe) {
                                    fireMediaError(new MediaException(MediaErrorType.Unknown, ioe));
                                }
                            }
                        }).start();
                        
                   } catch (LineUnavailableException ex) {
                        fireMediaError(new MediaException(MediaErrorType.LineUnavailable, ex));
                    }    
                } else {
                    if (!line.isActive()) {
                        line.start();
                        recording = true;
                        fireMediaStateChange(State.Playing);
                    }
                }

                
            }

            @Override
            protected void pauseImpl() {
                if (line == null) {
                    return;
                }
                if (!recording) {
                    return;
                }
                recording = false;
                fireMediaStateChange(State.Paused);
                line.stop();
                
                
            }
            

            @Override
            public void prepare() {
                
            }

            @Override
            public void cleanup() {
                if (recording) {
                    pause();
                }
                recording = false;
                if (redirectToAudioBuffer) {
                    MediaManager.releaseAudioBuffer(path);
                }
                if (line == null) {
                    return;
                }
                line.close();
                
                if (!redirectToAudioBuffer && isMP3EncodingSupported() && "audio/mp3".equalsIgnoreCase(fMime)) {
                    final Throwable[] t = new Throwable[1];
                    CN.invokeAndBlock(new Runnable() {
                        public void run() {
                            try {

                                FileEncoder.getEncoder("audio/wav", "audio/mp3").encode(wavFile, outFile, getAudioFormat());
                                wavFile.delete();
                            } catch (Throwable ex) {
                                com.codename1.io.Log.e(ex);
                                t[0] = ex;
                                fireMediaError(new MediaException(MediaErrorType.Encode, ex));
                            }
                        }
                    });
                    //if (t[0] != null) {
                    //    throw new RuntimeException(t[0]);
                    //}
                    
                }
                line = null;
            }

            @Override
            public int getTime() {
                return (int)(line.getMicrosecondPosition() / 1000l);
            }

            @Override
            public void setTime(int time) {
                throw new RuntimeException("setTime() not supported on recordable Media");
            }

            @Override
            public int getDuration() {
                return (int)(line.getMicrosecondPosition() / 1000l);
            }

            @Override
            public void setVolume(int vol) {
                
            }

            @Override
            public int getVolume() {
                return 100;
            }

            @Override
            public boolean isPlaying() {
                return recording;
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
    private com.codename1.ui.util.ImageIO imIO;

    @Override
    public com.codename1.ui.util.ImageIO getImageIO() {
        if (imIO == null) {
            imIO = new com.codename1.ui.util.ImageIO() {
                private BufferedImage fixImage(Image img) {
                    BufferedImage bi = (BufferedImage)img.getImage();
                    if(bi.getType() != BufferedImage.TYPE_INT_RGB) {
                        BufferedImage b = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_RGB);
                        Graphics2D g2d = b.createGraphics();
                        g2d.drawImage(bi, 0, 0, null);
                        g2d.dispose();;
                        return b;
                    }
                    return bi;
                }
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
                        height = img.getHeight();
                    }
                    if(format == FORMAT_JPEG) {
                        ImageIO.write(fixImage(img), f, response);
                        return;
                    }
                    ImageIO.write(((BufferedImage) img.getImage()), f, response);
                }

                @Override
                protected void saveImage(Image img, OutputStream response, String format, float quality) throws IOException {
                    String f = "png";
                    if (format == FORMAT_JPEG) {
                        f = "jpeg";
                        ImageIO.write(fixImage(img), f, response);
                        return;
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
    protected int getDragAutoActivationThreshold() {
        return 10000;
    }
    
    @Override
    public void registerPush(Hashtable meta, boolean noFallback) {
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
        super.registerPush(meta, noFallback);
        
        if(pushSimulation != null && pushSimulation.isVisible()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(window, "registerForPush invoked. Use the buttons in the push simulator to send the appropriate callback to your app", 
                            "Push Registration", JOptionPane.INFORMATION_MESSAGE);
                }
            });
        }
    }

    @Override
    public String getDatabasePath(String databaseName) {
        if(exposeFilesystem){
            File f = getDatabaseFile(databaseName);
        
            return f.getAbsolutePath();
        }else{
            if (databaseName.startsWith("file://")) {
                return databaseName;
            }
            return getAppHomePath() + "database/" + databaseName;        
        }
    }

    @Override
    public Database openOrCreateDB(String databaseName) throws IOException {
        try {
            // Load the sqlite database Engine JDBC driver
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
            SQLiteConfig config = new SQLiteConfig();
            config.enableLoadExtension(true);
            File file = getDatabaseFile(databaseName);
             
            
            File dir = file.getParentFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }
            java.sql.Connection conn = DriverManager.getConnection("jdbc:sqlite:" +
                    file.getAbsolutePath(),
                    config.toProperties()
            );

            return new SEDatabase(conn);
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    
    private File getDatabaseFile(String databaseName) {
        File f = new File(getStorageDir() + File.separator+ "database" + File.separator + databaseName);
        if (exposeFilesystem) {
            if (databaseName.contains("/") || databaseName.contains("\\")) {
                f = new File(databaseName);
            }
        } else {
            if (databaseName.startsWith("file://")) {
                f = new File(FileSystemStorage.getInstance().toNativePath(databaseName));
            }
            
        }
        return f;
    }
    
    //@Override
    public boolean isDatabaseCustomPathSupported() {
        return true;
    }
    
    @Override
    public void deleteDB(String databaseName) throws IOException {
        System.out.println("**** Database.delete() is not supported in the Javascript port.  If you plan to deploy to Javascript, you should avoid this method. *****");
        File f = getDatabaseFile(databaseName);
        if (f.exists()) {
            if (!f.delete()) {
                throw new IOException("Failed to delete database file "+f+".  It may be in use.  Make sure to close the database connection before deleting the database.");
            }
            
        }
    }
    
    

    @Override
    public boolean existsDB(String databaseName) {
        System.out.println("**** Database.exists() is not supported in the Javascript port.  If you plan to deploy to Javascript, you should avoid this method. *****");
        File f = getDatabaseFile(databaseName);
        return f.exists();
    }

    @Override
    public void setCommandBehavior(int commandBehavior) {
        //cannot show native menus on the simulator
        if (commandBehavior == Display.COMMAND_BEHAVIOR_NATIVE) {
            if (getPlatformName().equals("win")) {
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
        return false;
    }
    protected boolean useWKWebViewChecked;
    
    
    
    

    public void setBrowserProperty(PeerComponent browserPeer, String key, Object value) {
        ((IBrowserComponent) browserPeer).setProperty(key, value);
    }

    public String getBrowserTitle(PeerComponent browserPeer) {
        return ((IBrowserComponent) browserPeer).getTitle();
    }

    public String getBrowserURL(PeerComponent browserPeer) {
        return ((IBrowserComponent) browserPeer).getURL();
    }

    public void browserExecute(PeerComponent browserPeer, String javaScript) {
        ((IBrowserComponent) browserPeer).execute(javaScript);
    }

    
    public boolean supportsBrowserExecuteAndReturnString(PeerComponent browserPeer) {
        return ((IBrowserComponent) browserPeer).supportsExecuteAndReturnString();
    }
    
    @Override
    public String browserExecuteAndReturnString(PeerComponent browserPeer, String javaScript) {
        return ((IBrowserComponent) browserPeer).executeAndReturnString(javaScript);
    }

    @Override
    public void setBrowserURL(PeerComponent browserPeer, String url, Map<String, String> headers) {
        setBrowserURL(browserPeer, url);
    }

    
    
    public void setBrowserURL(final PeerComponent browserPeer, String url) {
        if(url.startsWith("file:") && (url.indexOf("/html/") < 0 || !exposeFilesystem)) {
            try {
                try {
                    if(url.startsWith("file://home/")) {
                        url = new File(unfile(url)).
                            toURI().toURL().toExternalForm();
                    } else {
                        URI uri = new URI(url);
                        com.codename1.io.File cf = new com.codename1.io.File(uri);
                        File f = new File(unfile(cf.getAbsolutePath()));
                        url = f.toURI().toString();
                    }
                } catch (URISyntaxException sex) {
                    Log.p("Attempt to set invalid URL "+url+".  Allowing this to continue on the simulator, but this will likely crash on device.");
                    Log.e(sex);
                    File f = new File(unfile(url));
                    url = f.toURI().toString();
                }
                
            } catch (Throwable t){
                url = "file://" + unfile(url);
            }
        }
        if (url.startsWith("jar:")) {
            url = url.substring(6);
            url = this.getClass().getResource(url).toExternalForm();
        }
        final String theUrl = url;
        ((IBrowserComponent)browserPeer).runLater(new Runnable() {

            @Override
            public void run() {
                ((IBrowserComponent) browserPeer).setURL(theUrl);
            }
        });
    }

    public void browserStop(final PeerComponent browserPeer) {
        ((IBrowserComponent)browserPeer).runLater(new Runnable() {

            @Override
            public void run() {
                ((IBrowserComponent) browserPeer).stop();
            }
        });
    }

    /**
     * Reload the current page
     *
     * @param browserPeer browser instance
     */
    public void browserReload(final PeerComponent browserPeer) {
        ((IBrowserComponent)browserPeer).runLater(new Runnable() {

            @Override
            public void run() {
                ((IBrowserComponent) browserPeer).reload();
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
        return ((IBrowserComponent) browserPeer).hasBack();
    }

    public boolean browserHasForward(PeerComponent browserPeer) {
        return ((IBrowserComponent) browserPeer).hasForward();
    }

    public void browserBack(final PeerComponent browserPeer) {
        ((IBrowserComponent)browserPeer).runLater(new Runnable() {

            @Override
            public void run() {
                ((IBrowserComponent) browserPeer).back();
            }
        });
    }

    public void browserForward(final PeerComponent browserPeer) {
        ((IBrowserComponent)browserPeer).runLater(new Runnable() {

            @Override
            public void run() {
                ((IBrowserComponent) browserPeer).forward();
            }
        });
    }

    public void browserClearHistory(final PeerComponent browserPeer) {
        ((IBrowserComponent)browserPeer).runLater(new Runnable() {

            @Override
            public void run() {
                ((IBrowserComponent) browserPeer).clearHistory();
            }
        });
    }

    public void setBrowserPage(final PeerComponent browserPeer, final String html, final String baseUrl) {
        ((IBrowserComponent)browserPeer).runLater(new Runnable() {

            @Override
            public void run() {
                ((IBrowserComponent) browserPeer).setPage(html, baseUrl);
            }
        });
    }


    
    public void browserExposeInJavaScript(final PeerComponent browserPeer, final Object o, final String name) {
        
    }

    public Purchase getInAppPurchase() {
        return new Purchase() {

            private Vector purchases;

            @Override
            public Product[] getProducts(String[] skus) {
                return null;
            }

            @Override
            public boolean isItemListingSupported() {
                return false;
            }

            @Override
            public boolean isManagedPaymentSupported() {
                return managedPurchaseSupported;
            }

            @Override
            public boolean isManualPaymentSupported() {
                return manualPurchaseSupported;
            }

            @Override
            public boolean isRefundable(String sku) {
                if (!refundSupported) {
                    return false;
                }
                int val = JOptionPane.showConfirmDialog(window, "Is " + sku + " refundable?", "Purchase", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                return val == JOptionPane.YES_OPTION;
            }

            private Vector getPurchases() {
                if (purchases == null) {
                    purchases = (Vector) Storage.getInstance().readObject("CN1InAppPurchases");
                    if (purchases == null) {
                        purchases = new Vector();
                    }
                }
                return purchases;
            }

            private void savePurchases() {
                if (purchases != null) {
                    Storage.getInstance().writeObject("CN1InAppPurchases", purchases);
                }
            }

            @Override
            public boolean isSubscriptionSupported() {
                return subscriptionSupported;
            }

            @Override
            public boolean isUnsubscribeSupported() {
                return subscriptionSupported;
            }

            @Override
            public String pay(final double amount, final String currency) {
                try {
                    if (Display.getInstance().isEdt()) {
                        final String[] response = new String[1];
                        Display.getInstance().invokeAndBlock(new Runnable() {

                            public void run() {
                                response[0] = pay(amount, currency);
                            }
                        });
                        return response[0];
                    }
                    if (!manualPurchaseSupported) {
                        throw new RuntimeException("Manual payment isn't supported check the isManualPaymentSupported() method!");
                    }
                    final String[] result = new String[1];
                    final boolean[] completed = new boolean[]{false};
                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {
                            int res = JOptionPane.showConfirmDialog(window, "A payment of " + amount + " was made\nDo you wish to accept it?", "Payment", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                            if (res == JOptionPane.YES_OPTION) {
                                result[0] = UUID.randomUUID().toString();
                            }
                            completed[0] = true;
                        }
                    });
                    Display.getInstance().invokeAndBlock(new Runnable() {

                        public void run() {
                            while (!completed[0]) {
                                try {
                                    Thread.sleep(20);
                                } catch (InterruptedException err) {
                                }
                            }
                        }
                    });

                    if (getPurchaseCallback() != null) {
                        Display.getInstance().callSerially(new Runnable() {

                            public void run() {
                                if (result[0] != null) {
                                    getPurchaseCallback().paymentSucceeded(result[0], amount, currency);
                                } else {
                                    getPurchaseCallback().paymentFailed(UUID.randomUUID().toString(), null);
                                }
                            }
                        });
                    }


                    return result[0];
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return null;
            }

            @Override
            public void purchase(final String sku) {
                if (managedPurchaseSupported) {
                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {
                            final int res = JOptionPane.showConfirmDialog(window, "An in-app purchase of " + sku + " was made\nDo you wish to accept it?", "Payment", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                            Display.getInstance().callSerially(new Runnable() {

                                public void run() {
                                    if (res == JOptionPane.YES_OPTION) {
                                        com.codename1.payment.Purchase.postReceipt(Receipt.STORE_CODE_SIMULATOR, sku, "cn1-iap-sim-"+UUID.randomUUID().toString(), System.currentTimeMillis(), "");
                                        getPurchaseCallback().itemPurchased(sku);
                                        getPurchases().addElement(sku);
                                        savePurchases();
                                    } else {
                                        getPurchaseCallback().itemPurchaseError(sku, "Purchase failed");
                                    }
                                }
                            });
                        }
                    });
                } else {
                    throw new RuntimeException("In app purchase isn't supported on this platform!");
                }
            }

            @Override
            public void purchase(String sku, PromotionalOffer promotionalOffer) {
                purchase(sku);
            }

            @Override
            public void refund(final String sku) {
                if (refundSupported) {
                    Display.getInstance().callSerially(new Runnable() {

                        public void run() {
                            getPurchaseCallback().itemRefunded(sku);
                            getPurchases().removeElement(sku);
                            savePurchases();
                        }
                    });
                }
            }

            @Override
            public void subscribe(final String sku) {
                if (getReceiptStore() != null) {
                    purchase(sku);
                    return;
                }
                if (subscriptionSupported) {
                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {
                            final int res = JOptionPane.showConfirmDialog(window, "An in-app subscription to " + sku + " was made\nDo you wish to accept it?", "Payment", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                            Display.getInstance().callSerially(new Runnable() {

                                public void run() {
                                    if (res == JOptionPane.YES_OPTION) {
                                        getPurchaseCallback().subscriptionStarted(sku);
                                        getPurchases().addElement(sku);
                                        savePurchases();
                                    } else {
                                        getPurchaseCallback().itemPurchaseError(sku, "Subscription failed");
                                    }
                                }
                            });
                        }
                    });
                }
            }

            @Override
            public void subscribe(String sku, PromotionalOffer promotionalOffer) {
                subscribe(sku);
            }

            @Override
            public void unsubscribe(final String sku) {
                if (subscriptionSupported) {
                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {
                            final int res = JOptionPane.showConfirmDialog(window, "In-app unsubscription request for " + sku + " was made\nDo you wish to accept it?", "Payment", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                            Display.getInstance().callSerially(new Runnable() {

                                public void run() {
                                    if (res == JOptionPane.YES_OPTION) {
                                        getPurchaseCallback().subscriptionCanceled(sku);
                                        getPurchases().removeElement(sku);
                                        savePurchases();
                                    } else {
                                        getPurchaseCallback().itemPurchaseError(sku, "Error in unsubscribe");
                                    }
                                }
                            });
                        }
                    });
                }
            }

            @Override
            public boolean wasPurchased(String sku) {
                return getPurchases().contains(sku);
            }

            @Override
            public String getStoreCode() {
                return Receipt.STORE_CODE_SIMULATOR;
            }
        };
    }

    private File pickImage() {
        return pickFile(new String[] {"png", "jpg", "jpeg"}, "*.png;*.jpg;*.jpeg");
    }

    private File pickFile(String[] types, String name) {
        File[] out = pickFiles(types, name, false);
        if (out == null || out.length < 1) {
            return null;
        }
        return out[0];
        
    }
    
    private File[] pickFiles(final String[] types, String name, boolean multipleMode) {
        FileDialog fd = new FileDialog(java.awt.Frame.getFrames()[0]);
        fd.setFilenameFilter(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                name = name.toLowerCase();
                for(String t : types) {
                    if(name.endsWith(t) || "*".equals(t)) {
                        return true;
                    }
                }
                return false;
            }
        });
        fd.setFile(name);
        if (multipleMode) {
            fd.setMultipleMode(true);
        }
        fd.pack();
        fd.setLocationByPlatform(true);
        fd.setVisible(true);

        if (multipleMode) {
            File[] files = fd.getFiles();
            ArrayList<File> out = new ArrayList<File>();
            for (File f : files) {
                name = f.getName().toLowerCase();
                for (String t : types) {
                    if (name.endsWith(t)) {
                        if (!f.exists()) {
                            out.add(f.getParentFile());
                        } else {
                            out.add(f);
                        }
                        break;
                    }
                }
            }
            return out.toArray(new File[out.size()]);
        } else {
            if (fd.getFile() != null) {
                name = fd.getFile().toLowerCase();
                for(String t : types) {
                    if(name.endsWith(t)) {
                        File f = new File(fd.getDirectory(), fd.getFile());
                        if(!f.exists()){
                            return new File[]{new File(fd.getDirectory())};
                        }else{
                            return new File[]{f};
                        }
                    }
                }
            }
            return new File[0];
        }
    }

    @Override
    public String toNativePath(String path) {
        return unfile(path);
    }

    
    
    public String getAppHomePath() {
        if(exposeFilesystem) {
            File home = new File(System.getProperty("user.home") + File.separator + appHomeDir);
            home.mkdirs();
            try {
                return home.toURI().toURL().toExternalForm();
            } catch(MalformedURLException err) {
                // this is just moronic
                throw new RuntimeException(err);
            }
        }
        new File(System.getProperty("user.home") + File.separator + appHomeDir).mkdirs();
        return "file://home/";
    }

    
    
    public int convertToPixels(int dipCount, boolean horizontal) {
        if (pixelMilliRatio != null) {
            return (int) Math.round(dipCount * pixelMilliRatio.doubleValue());
        }
        return super.convertToPixels(dipCount, horizontal);
    }

    private File downloadSkin(File skinDir, String url, String version, JLabel label) throws IOException {
        String fileName = url.substring(url.lastIndexOf("/"));
        File skin = new File(skinDir.getAbsolutePath() + "/" + fileName);
        HttpURLConnection.setFollowRedirects(true);
        URL u = new URL(url);
        FileOutputStream os = new FileOutputStream(skin);
        URLConnection uc = u.openConnection();
        InputStream is = uc.getInputStream();
        int length = uc.getContentLength();
        byte[] buffer = new byte[65536];
        int size = is.read(buffer);
        int offset = 0;
        int percent = 0;
        String msg = label.getText();

        if (length > 0) {
            System.out.println("Downloading " + length + " bytes");
        }
        while (size > -1) {
            offset += size;
            if (length > 0) {
                float f = ((float) offset) / ((float) length) * 100;
                if (percent != ((int) f)) {
                    percent = (int) f;
                    label.setText(msg + " " + percent + "%");
                    label.repaint();
                }
            } else {
                if (percent < offset / 102400) {
                    percent = offset / 102400;
                    System.out.println("Downloaded " + percent + "00Kb");
                }
            }
            os.write(buffer, 0, size);
            size = is.read(buffer);
        }
        is.close();
        os.close();
        
        //store the skin version
        Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
        pref.put(fileName, version);

        
        return skin;
    }
    
    private Hashtable initContacts() {
        checkContactsUsageDescription();
        Hashtable retVal = new Hashtable();
        
        Image img = null;
        try {
            img = Image.createImage(getClass().getResourceAsStream("/com/codename1/impl/javase/user.jpg"));
        } catch (IOException ex) {
        }
        
        Contact contact = new Contact();
        contact.setId("1");

        contact.setDisplayName("Chen Fishbein");
        contact.setFirstName("Chen");
        contact.setFamilyName("Fishbein");
        contact.setPhoto(img);

        Hashtable phones = new Hashtable();
        phones.put("mobile", "+111111");
        phones.put("home", "+222222");
        contact.setPhoneNumbers(phones);
        
        Hashtable emails = new Hashtable();
        emails.put("work", "----");
        contact.setEmails(emails);

        Hashtable addresses = new Hashtable();
        Address addr = new Address();
        addr.setCountry("IL");
        addr.setStreetAddress("Sapir 20");
        addresses.put("home", addr);
        contact.setAddresses(addresses);

        retVal.put(contact.getId(), contact);

        contact = new Contact();
        contact.setId("2");
        contact.setDisplayName("Shai Almog");
        contact.setFirstName("Shai");
        contact.setFamilyName("Almog");
        contact.setPhoto(img);

        phones = new Hashtable();
        phones.put("mobile", "+111111");
        phones.put("home", "+222222");
        contact.setPhoneNumbers(phones);
        emails = new Hashtable();
        emails.put("work", "----");
        contact.setEmails(emails);

        addresses = new Hashtable();
        addr = new Address();
        addr.setCountry("IL");
        addr.setStreetAddress("lev 1");
        addresses.put("home", addr);
        contact.setAddresses(addresses);
        retVal.put(contact.getId(), contact);

        contact = new Contact();
        contact.setId("3");

        contact.setDisplayName("Eric Cartman");
        contact.setFirstName("Eric");
        contact.setFamilyName("Cartman");


        phones = new Hashtable();
        phones.put("mobile", "+111111");
        phones.put("home", "+222222");
        contact.setPhoneNumbers(phones);
        emails = new Hashtable();
        emails.put("work", "Eric.Cartman@codenameone.com");
        contact.setEmails(emails);

        addresses = new Hashtable();
        addr = new Address();
        addr.setCountry("US");
        addr.setStreetAddress("South Park");
        addresses.put("home", addr);
        contact.setAddresses(addresses);

        retVal.put(contact.getId(), contact);

        contact = new Contact();
        contact.setId("4");
        contact.setDisplayName("Kyle Broflovski");
        contact.setFirstName("Kyle");
        contact.setFamilyName("Broflovski");

        phones = new Hashtable();
        phones.put("mobile", "+111111");
        phones.put("home", "+222222");
        contact.setPhoneNumbers(phones);
        emails = new Hashtable();
        emails.put("work", "Kyle.Broflovski@codenameone.com");
        contact.setEmails(emails);

        addresses = new Hashtable();
        addr = new Address();
        addr.setCountry("US");
        addr.setStreetAddress("South Park");
        addresses.put("home", addr);
        contact.setAddresses(addresses);
        retVal.put(contact.getId(), contact);

        contact = new Contact();
        contact.setId("5");

        contact.setDisplayName("Kenny McCormick");
        contact.setFirstName("Kenny");
        contact.setFamilyName("McCormick");
        contact.setPhoto(img);


        phones = new Hashtable();
        phones.put("mobile", "+111111");
        phones.put("home", "+222222");
        contact.setPhoneNumbers(phones);
        emails = new Hashtable();
        emails.put("work", "Kenny.McCormick@codenameone.com");
        contact.setEmails(emails);

        addresses = new Hashtable();
        addr = new Address();
        addr.setCountry("US");
        addr.setStreetAddress("South Park");
        addresses.put("home", addr);
        contact.setAddresses(addresses);
        retVal.put(contact.getId(), contact);

        return retVal;

    }

    private ServerSockets serverSockets;
    private synchronized ServerSockets getServerSockets() {
        if (serverSockets == null) {
            serverSockets = new ServerSockets();
        }
        return serverSockets;
    }
    
    class ServerSockets {
        Map<Integer,ServerSocket> socks = new HashMap<Integer,ServerSocket>();
        
        public synchronized ServerSocket get(int port) throws IOException {
            if (socks.containsKey(port)) {
                ServerSocket sock = socks.get(port);
                if (sock.isClosed()) {
                    sock = new ServerSocket(port);
                    socks.put(port, sock);
                }
                return sock;
            } else {
                ServerSocket sock = new ServerSocket(port);
                socks.put(port, sock);
                return sock;
            }
        }
        
        
    }
    
    class SocketImpl {
        java.net.Socket socketInstance;
        int errorCode = -1;
        String errorMessage = null;
        InputStream is;
        OutputStream os;

        public boolean connect(String param, int param1, int connectTimeout) {
            
            try {
                
                socketInstance = new java.net.Socket();
                socketInstance.connect(new InetSocketAddress(param, param1), connectTimeout);
                //socketInstance = new java.net.Socket(param, param1);
                return true;
            } catch(Exception err) {
                err.printStackTrace();
                errorMessage = err.toString();
                return false;
            }
        }

        private InputStream getInput() throws IOException {
            if(is == null) {
                if(socketInstance != null) {
                    is = socketInstance.getInputStream();
                } 
            }
            return is;
        }

        private OutputStream getOutput() throws IOException {
            if(os == null) {
                os = socketInstance.getOutputStream();
            }
            return os;
        }

        public int getAvailableInput() {
            try {
                return getInput().available();
            } catch(IOException err) {
                errorMessage = err.toString();
                err.printStackTrace();
            }
            return 0;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public byte[] readFromStream() {
            try {
                int av = getAvailableInput();
                if(av > 0) {
                    byte[] arr = new byte[av];
                    int size = getInput().read(arr);
                    if(size == arr.length) {
                        return arr;
                    }
                    return shrink(arr, size);
                }
                byte[] arr = new byte[8192];
                int size = getInput().read(arr);
                if(size == arr.length) {
                    return arr;
                }
                if(size < 0) {
                    return null;
                }
                return shrink(arr, size);
            } catch(IOException err) {
                socketInstance = null;	// no longer connected
                err.printStackTrace();
                errorMessage = err.toString();
                return null;
            }
        }

        private byte[] shrink(byte[] arr, int size) {
            if(size == -1) {
                return null;
            }
            byte[] n = new byte[size];
            System.arraycopy(arr, 0, n, 0, size);
            return n;
        }

        public void writeToStream(byte[] param) {
            try {
                OutputStream os = getOutput();
                os.write(param);
                os.flush();
            } catch(IOException err) {
                socketInstance = null;	// no longer connected
                errorMessage = err.toString();
                err.printStackTrace();
            }
        }

        public void disconnect() {
            try {
                if(socketInstance != null) {
                    if(is != null) {
                        try {
                            is.close();
                        } catch(IOException err) {}
                    }
                    if(os != null) {
                        try {
                            os.close();
                        } catch(IOException err) {}
                    }
                    socketInstance.close();
                    socketInstance = null;
                }
            } catch(IOException err) {
                errorMessage = err.toString();
                err.printStackTrace();
            }
        }

        public Object listen(int param) {
            try {
                ServerSocket serverSocketInstance = getServerSockets().get(param);
                socketInstance = serverSocketInstance.accept();
                return this;
            } catch(Exception err) {
                errorMessage = err.toString();
                err.printStackTrace();
                return null;
            }
        }

        public boolean isConnected() {
            return socketInstance != null;
        }

        public int getErrorCode() {
            return errorCode;
        }
    }
    
    @Override
    public Object connectSocket(String host, int port, int connectTimeout) {
        SocketImpl i = new SocketImpl();
        if(i.connect(host, port, connectTimeout)) {
            return i;
        }
        return null;
    }

    
    @Override
    public Object listenSocket(int port) {
        return new SocketImpl().listen(port);
    }
    
    @Override
    public String getHostOrIP() {
        try {
            InetAddress i = java.net.InetAddress.getLocalHost();
            if(i.isLoopbackAddress()) {
                Enumeration<NetworkInterface> nie = NetworkInterface.getNetworkInterfaces();
                while(nie.hasMoreElements()) {
                    NetworkInterface current = nie.nextElement();
                    if(!current.isLoopback()) {
                        Enumeration<InetAddress> iae = current.getInetAddresses();
                        while(iae.hasMoreElements()) {
                            InetAddress currentI = iae.nextElement();
                            if(!currentI.isLoopbackAddress()) {
                                return currentI.getHostAddress();
                            }
                        }
                    }
                }
            }
            return i.getHostAddress();
        } catch(Throwable t) {
            Log.e(t);
            return null;
        }
    }

    @Override
    public void disconnectSocket(Object socket) {
        ((SocketImpl)socket).disconnect();
    }    
    
    @Override
    public boolean isSocketConnected(Object socket) {
        return ((SocketImpl)socket).isConnected();
    }
    
    @Override
    public boolean isServerSocketAvailable() {
        return true;
    }

    @Override
    public boolean isSocketAvailable() {
        return true;
    }
    
    @Override
    public String getSocketErrorMessage(Object socket) {
        if (socket == null) {
            return null;
        }
        return ((SocketImpl)socket).getErrorMessage();
    }
    
    @Override
    public int getSocketErrorCode(Object socket) {
        if (socket == null) {
            return 0;
        }
        return ((SocketImpl)socket).getErrorCode();
    }
    
    @Override
    public int getSocketAvailableInput(Object socket) {
        return ((SocketImpl)socket).getAvailableInput();
    }
    
    @Override
    public byte[] readFromSocketStream(Object socket) {
        return ((SocketImpl)socket).readFromStream();
    }
    
    @Override
    public void writeToSocketStream(Object socket, byte[] data) {
        ((SocketImpl)socket).writeToStream(data);
    }

    /**
     * Overriden to work well in the simulator
     */
    @Override
    public void installTar() throws IOException {
        File f = new File(getCWD(),"codenameone_settings.properties");
        if(!f.exists()) {
            super.installTar();
        }
    }    
    
    /**
     * Overriden to work well in the simulator
     */
    @Override
    public void setBrowserPageInHierarchy(PeerComponent browserPeer, String url) throws IOException {
        File f = new File(getCWD(), "codenameone_settings.properties");
        if(!f.exists()) {
            super.setBrowserPageInHierarchy(browserPeer, url);
            return;
        }

        String sep = File.separator;
        File[] searchPaths = new File[]{
            new File(f.getParent(), "target" + sep + "classes"+ sep + "html"),
            new File(f.getParent(), "build" + sep + "classes"+ sep + "html"),
            new File(f.getParent(), "src" + sep + "main"+ sep + "resources" + sep +"html"),
            new File(f.getParent(), "src" + sep + "html"),
            new File(f.getParent(), "lib" + sep + "impl" + sep + "cls" + sep + "html")
        };
        
        File u = null;
        boolean found = false;
        for (File htmldir : searchPaths) {
            u = new File(htmldir, url);
            if (u.exists()) {
                u = htmldir;
                found = true;
                break;
            }
        }
        if (!found) {
            throw new RuntimeException("Could not display browser page "+url+" because it doesn't exist in bundle html hierarchy.");
        }
        /*
        File u = new File(f.getParent(), "build" + File.separator + "classes"+ File.separator + "html");
        if (!u.exists()) {
            u = new File(f.getParent(), "src" + File.separator + "html");
        }
        if (!u.exists()) {
            u = new File(f.getParent(), "lib" + File.separator + "impl" + File.separator + "cls" + File.separator )
        }*/
        String base = u.toURI().toURL().toExternalForm(); 
        if(base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if(url.startsWith("/")) {
            setBrowserURL(browserPeer, base + url);
        } else {
            setBrowserURL(browserPeer, base  + "/" + url);
        }
    }

    private static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    /**
     * @inheritDoc
     */
    public PeerComponent createNativePeer(Object nativeComponent) {
        if (!(nativeComponent instanceof java.awt.Component)) {
            throw new IllegalArgumentException(nativeComponent.getClass().getName());
        }
        java.awt.Container cnt = canvas.getParent();
        while (!(cnt instanceof JFrame)) {
            cnt = cnt.getParent();
            if (cnt == null) {
                return null;
            }
        }
        
        return new JavaSEPort.Peer((JFrame)cnt, (java.awt.Component) nativeComponent);
    }
    
    public Image gaussianBlurImage(Image image, float radius) {
        GaussianFilter gf = new GaussianFilter(radius);
        Image bim = Image.createImage(image.getWidth(), image.getHeight());        
        BufferedImage blurredImage = gf.filter((BufferedImage)image.getImage(), (BufferedImage)bim.getImage());        
        return new NativeImage(blurredImage);
    }

    public boolean isGaussianBlurSupported() {
        return true;
    }
 
    class NativeImage extends Image {

        public NativeImage(BufferedImage nativeImage) {
            super(nativeImage);
        }
    }
    
    public static class Peer extends PeerComponent implements HierarchyListener {
        
        // Container that will hold the native peer component.
        // Wrapping the component in a container allows us to
        // override key methods like paint() and setBounds()
        // so that we can bend them to our needs
        private JPanel cnt = new JPanel();
        private boolean init = false;
        
        // Reference to the JFrame into which cnt will be added.
        private JFrame frm;
        
        // The native peer component.
        private java.awt.Component cmp;
        
        private boolean matchCN1Style;
        
        private Image peerImage;
        private boolean lightweightMode;
        private PeerComponentBuffer peerBuffer;
        
        // Buffered image that will be drawn to by AWT and read from
        // by CN1
        BufferedImage buf;
        
        /**
         * Gets a buffered image on which we paint the native peer.
         * We draw to this image from the AWT thread, and we draw from
         * this image on the CN1 EDT.
         * @return 
         * @see #paintOnBuffer() 
         * @see #drawNativePeer(java.lang.Object, com.codename1.ui.PeerComponent, javax.swing.JComponent) 
         */
        private BufferedImage getBuffer() {
            if (buf == null || buf.getWidth() != cnt.getWidth() * retinaScale / instance.zoomLevel || buf.getHeight() != cnt.getHeight() * retinaScale / instance.zoomLevel) {

                buf = new BufferedImage((int)(cnt.getWidth() * retinaScale / instance.zoomLevel), (int)(cnt.getHeight() * retinaScale / instance.zoomLevel), BufferedImage.TYPE_INT_ARGB);
            }
            return buf;
        }
        
        public void setPeerComponentBuffer(PeerComponentBuffer buf) {
            if (peerBuffer != null) {
                peerBuffer.setPeer(null);
            }
            buf.setPeer(this);
            peerBuffer = buf;
        }
                
        
        /**
         * Paints the native peer onto a buffered image and stores the image
         * in the peer to be later drawn by {@link #drawNativePeer(java.lang.Object, com.codename1.ui.PeerComponent, javax.swing.JComponent) } (in the CN1 pipeline).
         * 
         * THis method should be called only on the AWT dispatch thread.
         */
        public void paintOnBuffer() {
            if (cnt.getWidth() == 0 || cnt.getHeight() == 0) {
                return;
            }
            if (EventQueue.isDispatchThread()) {
                synchronized(Peer.this) {
                    paintOnBufferImpl();

                }
            } else if (!Display.getInstance().isEdt()){
                try {
                    EventQueue.invokeAndWait(new Runnable() {
                        public void run() {
                            paintOnBuffer();
                        }
                    });
                } catch (Throwable t ) {
                    t.printStackTrace();
                }
                
            }
            
        }

        @Override
        public Image toImage() {
            if (getWidth() <= 0 || getHeight() <= 0) {
                return null;
            }
            Image image = Image.createImage(getWidth(), getHeight(),0x0);
            Graphics g = image.getGraphics();

            g.translate(-getX(), -getY());
            //paintComponentBackground(g);
            paint(g);

            g.translate(getX(), getY());
            return image;
        }
        
        

        private void applyStyle() {
            Style source = getStyle();
            
            if (true) {
                int fgColor = source.getFgColor();
                final int r = ColorUtil.red(fgColor);
                final int g = ColorUtil.green(fgColor);
                final int b = ColorUtil.blue(fgColor);
                EventQueue.invokeLater(new Runnable() { 
                    public void run() {
                        cmp.setForeground(new Color(r, g, b));
                    }
                });
                //super.styleChanged(propertyName, getStyle());
                return;
            }
            if (true) {
                int fgColor = source.getBgColor();
                final int r = ColorUtil.red(fgColor);
                final int g = ColorUtil.green(fgColor);
                final int b = ColorUtil.blue(fgColor);
                EventQueue.invokeLater(new Runnable() { 
                    public void run() {
                        cmp.setBackground(new Color(r, g, b));
                    }
                });
                //super.styleChanged(propertyName, getStyle());
                return;
            }
            if (true) {
                Font f = source.getFont();
                final java.awt.Font nf = (java.awt.Font)f.getNativeFont();
                
                EventQueue.invokeLater(new Runnable() { 
                    public void run() {
                        java.awt.Font nff = nf;
                        if ((cmp instanceof JPasswordField || cmp instanceof JComboBox) && nf.getFontName().contains("Roboto")) {
                            java.awt.Font fallback = new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, nf.getSize());
                            nff = fallback;
                        }
                        cmp.setFont(nff);
                    }
                });
                //super.styleChanged(propertyName, getStyle());
            }
        }
        
        @Override
        public void styleChanged(String propertyName, Style source) {
            super.styleChanged(propertyName, source);
            if (!matchCN1Style || getParent() == null) {
                return;
            }
            
            if (source != getParent().getStyle()) {
                return;
            }
            
            if (Style.FG_COLOR.equals(propertyName)) {
                int fgColor = source.getFgColor();
                final int r = ColorUtil.red(fgColor);
                final int g = ColorUtil.green(fgColor);
                final int b = ColorUtil.blue(fgColor);
                EventQueue.invokeLater(new Runnable() { 
                    public void run() {
                        cmp.setForeground(new Color(r, g, b));
                    }
                });
                super.styleChanged(propertyName, getStyle());
                return;
            }
            if (Style.BG_COLOR.equals(propertyName)) {
                int fgColor = source.getBgColor();
                final int r = ColorUtil.red(fgColor);
                final int g = ColorUtil.green(fgColor);
                final int b = ColorUtil.blue(fgColor);
                EventQueue.invokeLater(new Runnable() { 
                    public void run() {
                        cmp.setBackground(new Color(r, g, b));
                    }
                });
                super.styleChanged(propertyName, getStyle());
                return;
            }
            if (Style.FONT.equals(propertyName)) {
                Font f = source.getFont();
                final java.awt.Font nf = (java.awt.Font)f.getNativeFont();
                EventQueue.invokeLater(new Runnable() { 
                    public void run() {
                        java.awt.Font nff = nf;
                        if ((cmp instanceof JPasswordField || cmp instanceof JComboBox) && nf.getFontName().contains("Roboto")) {
                            java.awt.Font fallback = new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, nf.getSize());
                            nff = fallback;
                        }
                        cmp.setFont(nff);
                    }
                });
                super.styleChanged(propertyName, getStyle());
            }
            
            
        }
        
        
        
        private void paintOnBufferImpl() {
            final BufferedImage buf = getBuffer();
            Graphics2D g2d = buf.createGraphics();
            g2d.scale(retinaScale / instance.zoomLevel, retinaScale / instance.zoomLevel);

            cmp.paintAll(g2d);
            g2d.dispose();
            Peer.this.putClientProperty("__buffer", buf);
        }
        
        // HOLY LORD!! Because we're adding the peer directly to the JFrame
        // and the JFrame has BorderLayout, it will try sometimes to treat
        // this component like it is in the center and lay it out as such.
        // So we keep this lock that only allows the bounds of the container
        // to be changed when we decide it is OK.
        private boolean allowSetCntBounds;
        
        /**
         * Sets the container bounds.  Use this rather than cnt.setBounds()
         * as that has been modified to do nothing in order to prevent other layout
         * managers from messing with our layout. 
         * @param x
         * @param y
         * @param w
         * @param h 
         */
        private void setCntBounds(int x, int y, int w, int h) {
            allowSetCntBounds = true;
            if (cnt != null) {
                cnt.setBounds(x, y, w, h);
            }
            allowSetCntBounds = false;
        }
        
        private static JPanel createPanel(Peer p) {
            final java.lang.ref.WeakReference<Peer> selfRef = new java.lang.ref.WeakReference<Peer>(p);
            return new JPanel() {
                @Override
                public void paint(java.awt.Graphics g) {
                    final Peer self = selfRef.get();
                    if (self == null) {
                        return;
                    }
                    if (self.peerBuffer != null) {
                        //peerBuffer.paint((Graphics2D)g, cnt);
                        // If we are using a peer buffer, we won't *actually* paint the 
                        // component because the peer buffer will be painted
                        // in the CN1 paint cycle on the CN1 context
                        return;
                    }
                    self.paintOnBuffer();

                    // We need to tell CN1 to repaint now
                    // since the native peer has been updated
                    // There should be a new buffer to paint now.
                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            self.repaint();
                        }
                    });
                }

                @Override
                public void setBounds(int x, int y, int w, int h) {
                    Peer self = selfRef.get();
                    if (self == null) {
                        return;
                    }
                    if (self.allowSetCntBounds) {
                        super.setBounds(x, y, w, h);
                    } 
                }   
            };
        }
        
        public Peer(JFrame f, java.awt.Component c) {
            super(null);
            this.frm = f;
            this.cmp = c;
            if (c instanceof JComponent) {
                JComponent jc = (JComponent)c;
                if (null != jc.getClientProperty("cn1-match-style")) {
                    matchCN1Style = true;
                    applyStyle();
                }

            }
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    cnt = createPanel(Peer.this);
                    cnt.setOpaque(false);
                    cnt.setLayout(new BorderLayout());
                    cnt.add(BorderLayout.CENTER, cmp);
                    cnt.setVisible(false);
                    cnt.setBorder(new EmptyBorder(0,0,0,0));
                }
            });
        }

        @Override
        protected void initComponent() {
            bounds.setBounds(0,0,0,0);
            lastX=0;
            lastY=0;
            lastH=0;
            lastW=0;
            lastZoom=1;
            peerImage = null;
            super.initComponent();
            if (!init) {
                addNativeCnt();
                instance.canvas.addHierarchyListener(this);

            }
            
        }

        @Override
        protected void deinitialize() {
            super.deinitialize();
            if (instance.testRecorder != null) {
                instance.testRecorder.dispose();
                instance.testRecorder = null;
            }

            if (init) {
                instance.canvas.removeHierarchyListener(this);
                removeNativeCnt();

            }
            
            // We set visibility to false, and then schedule removal
            // for 1000ms from now.  This will deal with the situation where
            // a modal dialog is shown to avoid having to fully remove the native 
            // container.
            //cnt.setVisible(false);
            //removeNativeCnt(3000);
        }
        
        /**
         * Adds the native container to the swing component hierarchy.
         * 
         * This can be called off the Swing event thread, in which case it will just schedule a call
         * to itself later.
         * 
         * 
         */
        protected void addNativeCnt() {
            
            if (!EventQueue.isDispatchThread()) {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        addNativeCnt();
                    }
                });
                return;
            }
            
            if (!init && isInitialized()) {

                init = true;
                cnt.setVisible(true);
                frm.add(cnt, 0);
                frm.repaint();
            }
            
        }
        
        /**
         * Removes the native container from the Swing component hierarchy.
         * This can be called on or off the swing event thread.  If called off the swing event
         * thread, it will just schedule itself on the event thread later - async.
         */
        protected void removeNativeCnt() {
            if (!EventQueue.isDispatchThread()) {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        removeNativeCnt();
                    }
                });
                return;
            }
            if (peerImage == null) {
                peerImage = generatePeerImage();
            }
            init = false;
            frm.remove(cnt);
            frm.repaint();
            
        }

        @Override
        protected com.codename1.ui.Image generatePeerImage() {
            if (peerImage != null) {
                return peerImage;
            }
            if (peerBuffer != null) {
                peerBuffer.modifyBuffer(new Runnable() {
                    public void run() {
                        BufferedImage bimg = peerBuffer.getBufferedImage();
                        peerImage = instance.new NativeImage(bimg);
                    }
                   
                });
            }
            return super.generatePeerImage();
        }

        @Override
        protected boolean shouldRenderPeerImage() {
            return lightweightMode && peerImage != null;
        }
        
        
        
        

        protected void setLightweightMode(final boolean l) {
            if (lightweightMode == l) {
                if (l || init) {
                    return;
                }
            }
            lightweightMode = l;
            if (l) {
                if (peerImage == null) {
                    peerImage = generatePeerImage();
                } 
            } else {
                peerImage = null;
            }
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    if (!l) {
                        if (!init) {
                            init = true;
                            cnt.setVisible(true);
                            frm.add(cnt, 0);
                            frm.repaint();
                            peerImage = null;
                        } else {
                            cnt.setVisible(true);
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
            return new com.codename1.ui.geom.Dimension((int)(cmp.getPreferredSize().getWidth()* retinaScale / instance.zoomLevel), 
                    (int)(cmp.getPreferredSize().getHeight() * retinaScale / instance.zoomLevel));
        }

        
        @Override
        public void paint(final Graphics g) {
            if (lightweightMode) {
                super.paint(g);
                return;
            }
            if (init) {
                onPositionSizeChange();
                instance.drawNativePeer(Accessor.getNativeGraphics(g), this, cnt);
                if (peerBuffer != null) {
                    // Peer buffers are kept "painted" on their own
                    // we don't need to initiate any further calls to paintOnBuffer or such.
                    return;
                }
                
                
                // Tell AWT to do a repaint so that we know we have the latest.
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        // NOTICE We use paintOnBuffer() here and not
                        // cnt.repaint()
                        // This prevents an infinite loop since cnt.repaint()
                        // will tell CN1 to repaint - and the cycle would continue
                        
                        paintOnBuffer();
                    }
                });
                
            }else{
                if(getComponentForm() != null && getComponentForm() == instance.getCurrentForm()){
                    setLightweightMode(false);
                }
            }
        }
        
        java.awt.Rectangle bounds = new java.awt.Rectangle();
        
        int lastX, lastY, lastW, lastH;
        double lastZoom;

        @Override
        protected void onPositionSizeChange() {

            if (cnt == null) {
                return;
            }

            Form f = getComponentForm();
            if (cnt.getParent() == null
                    && f != null
                    && Display.getInstance().getCurrent() == f) {
                //();
                return;
            }

            final int x = getAbsoluteX();
            final int y = getAbsoluteY();
            final int w = getWidth();
            final int h = getHeight();
            double zoom_ = instance.zoomLevel;
            if (lastZoom == zoom_ && x == lastX && y == lastY && w == lastW && h == lastH) {
                return;
            }
            final int screenX;
            final int screenY;
            if (instance.getScreenCoordinates() != null) {
                screenX = instance.getScreenCoordinates().x;
                screenY = instance.getScreenCoordinates().y;
            } else {
                screenX = 0;
                screenY = 0;
            }

            lastX = x;
            lastY = y;
            lastW = w;
            lastH = h;
            lastZoom = instance.zoomLevel;
            final double zoom = lastZoom;

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    if (cnt.getParent() == null) return;
                    Point absCanvasLocation = SwingUtilities.convertPoint(instance.canvas, new Point(0, 0), cnt.getParent());
                    if (peerBuffer == null) {
                        double scale = zoom/retinaScale;

                        setCntBounds(
                                (int) ((x + screenX + instance.canvas.x) * scale) + absCanvasLocation.x,
                                (int) ((y + screenY + instance.canvas.y) * scale) + absCanvasLocation.y,
                                (int) (w * scale),
                                (int) (h * scale)
                        );
                    } else {
                        double scale = zoom/retinaScale;
                        setCntBounds(
                                (int) ((x + screenX + instance.canvas.x) * scale) + absCanvasLocation.x,
                                (int) ((y + screenY + instance.canvas.y) * scale) + absCanvasLocation.y,
                                (int) (w * scale),
                                (int) (h * scale)
                        );
                    }
                    
                    cnt.doLayout();
                    if (cmp instanceof Container) {
                        ((Container)cmp).doLayout();
                    }
                    if (peerBuffer != null) {
                        paintOnBuffer();
                    }
                }
            };
            if (SwingUtilities.isEventDispatchThread()) {
                r.run();
                return;
            }
            SwingUtilities.invokeLater(r);
        }

        private boolean _inHierarchyChanged;
        @Override
        public void hierarchyChanged(HierarchyEvent e) {
            if (_inHierarchyChanged) return;
            _inHierarchyChanged = true;
            try {
                java.awt.Container win = instance.canvas.getTopLevelAncestor();
                if (win != frm) {
                    removeNativeCnt();
                    lastH=0;
                    lastW=0;
                    lastX=0;
                    lastY=0;
                    if (win instanceof JFrame) {
                        frm = (JFrame) win;
                        addNativeCnt();
                    }

                }
                onPositionSizeChange();
            } finally {
                _inHierarchyChanged = false;
            }

        }
    }
    
    public static boolean checkForPermission(String permission, String description){
        return checkForPermission(permission, description, false);
    }

    public static boolean checkForPermission(String permission, String description, boolean forceAsk){
               
        if(!android6PermissionsFlag){
            return true;
        }

        String prompt = Display.getInstance().getProperty(permission, description);
        Boolean granted = (Boolean)android6Permissions.get(permission);
        //if granted
        if (granted == null || !granted.booleanValue()) {
            waitForPermission = true;
            
            Boolean wasAsked = (Boolean)android6Permissions.get(permission + ".asked");
            // Should we show an explanation?
            if (!forceAsk && (wasAsked != null)) {
                
                // Show an explanation to the user *asynchronously* -- don't block
                if(com.codename1.ui.Dialog.show("Requires permission", prompt, "Ask again", "Don't Ask")){
                    return checkForPermission(permission, description, true);
                }else {
                    waitForPermission = false;
                    return false;
                }
            } else {
                
                android6Permissions.put(permission + ".asked", true);
                
                boolean response = askPermission(permission);
                android6Permissions.put(permission, response);
                waitForPermission = false;
                return response;
            }
        }
        return true;
    }
    
    private static boolean askPermission(String permission) {
        String appname = Display.getInstance().getProperty("AppName", "");
       
        int selectedOption = JOptionPane.showConfirmDialog(null,
                "Allow " + appname + " to access your " + permission + "?",
                "Permission Request",
                JOptionPane.YES_NO_OPTION);
        return selectedOption == JOptionPane.YES_OPTION;
    }

    @Override
    public boolean isScrollWheeling() {
        return scrollWheeling;
    }

    @Override
    public boolean isJailbrokenDevice() {
        Map<String, String> m = getProjectBuildHints();
        if(m != null) {
            String s = m.get("ios.applicationQueriesSchemes");
            if(s == null || s.length() == 0) {
                setProjectBuildHint("ios.applicationQueriesSchemes", "cydia");
            } else {
                if(s.indexOf("cydia") < 0) {
                    setProjectBuildHint("ios.applicationQueriesSchemes", s + ",cydia");
                }
            }
        }
        return super.isJailbrokenDevice();
    }

    @Override
    public Boolean canExecute(String url) {
        if(!url.startsWith("http")) {
            int pos = url.indexOf(":");
            if(pos > -1) {
                String prefix = url.substring(0, pos);
                Map<String, String> m = getProjectBuildHints();
                if(m != null) {
                    String s = m.get("ios.applicationQueriesSchemes");
                    if(s == null || s.length() == 0) {
                        setProjectBuildHint("ios.applicationQueriesSchemes", prefix);
                    } else {
                        if(s.indexOf("cydia") < 0) {
                            setProjectBuildHint("ios.applicationQueriesSchemes", s + "," + prefix);
                        }
                    }
                }
            }
        }
        return super.canExecute(url);
    }

    
    public static File getCWD() {
        return new File(System.getProperty("user.dir"));
    }
    
    public static File getSourceResourcesDir() {
        File resDir = new File(getCWD(), "src" + File.separator + "main" + File.separator + "resources");
        if (!resDir.exists()) {
            resDir = new File(getCWD(), "src");
        }
        return resDir;
    }
    
    
    @Override
    public Map<String, String> getProjectBuildHints() {
        File cnopFile = new File(getCWD(), "codenameone_settings.properties");
        if(cnopFile.exists()) {
            java.util.Properties cnop = new java.util.Properties();
            try(InputStream is = new FileInputStream(cnopFile)) {
                cnop.load(is);
            } catch(IOException err) {
                return null;
            }
            HashMap<String, String> result = new HashMap<>();
            for(Object kk : cnop.keySet()) {
                String key = (String)kk;
                if(key.startsWith("codename1.arg.")) {
                    String val = cnop.getProperty(key);
                    key = key.substring(14);
                    result.put(key, val);
                }
            }
            return Collections.unmodifiableMap(result);
        }
        return null;
    }

    @Override
    public void setProjectBuildHint(String key, String value) {
         File cnopFile = new File(getCWD(),"codenameone_settings.properties");
        if(cnopFile.exists()) {
            Properties cnop = new Properties();
            try(InputStream is = new FileInputStream(cnopFile)) {
                cnop.load(is);
            } catch(IOException err) {
                throw new RuntimeException(err);
            }
            cnop.setProperty("codename1.arg." + key, value);
            try(OutputStream os = new FileOutputStream(cnopFile)) {
                cnop.store(os, null);
            } catch(IOException err) {
                throw new RuntimeException(err);
            }
            return;
        }
        throw new RuntimeException("Illegal state, file not found: " + cnopFile.getAbsolutePath());
   }
    
    /**
    * A component used in the JFrame's GlassPane to transform mouse events
    * so that they are directed at the correct place.  This is necessary on
    * Retina/HiDPI displays where native Text Components are rendered
    * in the CN1 pipeline transformed for retina so the mouse events
    * don't line up.  This checks if the mouse event should target the
    * current native text editor.  If so, it transforms the event and
    * dispatches it to the native editor.  If not, it just passes the
    * event through to the content pane unchanged.
    */
   class CN1GlassPane extends JComponent {
       CN1EventDispatcher dispatcher;
       CN1GlassPane() {
           //setLayout(new java.awt.BorderLayout());
           //add(new JButton("Test"), java.awt.BorderLayout.CENTER);
           //setVisible(true);
           dispatcher = new CN1EventDispatcher(this);
           addMouseListener(dispatcher);
           addMouseMotionListener(dispatcher);
           addMouseWheelListener(dispatcher);

           instance.canvas.addHierarchyListener(new HierarchyListener() {
               @Override
               public void hierarchyChanged(HierarchyEvent e) {
                   java.awt.Container canvasTop = instance.canvas.getTopLevelAncestor();
                   java.awt.Container glassTop = CN1GlassPane.this.getTopLevelAncestor();
                   if (glassTop != canvasTop && glassTop instanceof JFrame && canvasTop instanceof JFrame) {
                       JFrame glassFrame = (JFrame)glassTop;
                       JFrame canvasFrame = (JFrame)canvasTop;
                       CN1GlassPane.this.getParent().remove(CN1GlassPane.this);
                       canvasFrame.setGlassPane(CN1GlassPane.this);

                   }
               }
           });
       }
       
        

        @Override
        public boolean contains(int x, int y) {
            // We only want the glasspane to catch events that were targeting the 
            // canvas, so the hit-tesr for the glasspane should see if the x,y coordinate
            // would go to Canvas.  If we don't do this, then the glasspane will 
            // intercept all events, even those destined for the menu items - and
            // that causes all hell to break loose on Windows.
            Point p = SwingUtilities.convertPoint(this, new Point(x, y), instance.canvas);
            return instance.canvas.getVisibleRect().contains(p);

        }
   }
   
   private static boolean containsInHierarchy(java.awt.Component parent, java.awt.Component cmp) {
        Container p = cmp.getParent();
        while (p != parent && p != null) {
            p = p.getParent();
        }
        return p == parent;
    }
    
   /**
    * Event dispatcher used in glass pane.  
    * @see CN1GlassPane
    */
   class CN1EventDispatcher extends MouseInputAdapter implements MouseWheelListener {
        Toolkit toolkit;
        java.awt.Container contentPane;
        CN1GlassPane glassPane;
        boolean isPress;

        public CN1EventDispatcher(CN1GlassPane glassPane) {
            toolkit = Toolkit.getDefaultToolkit();
            this.glassPane = glassPane;
            this.contentPane = findTopFrame().getContentPane();
        }

        public void mouseMoved(MouseEvent e) {
            redispatchMouseEvent(e);
        }

        public void mouseDragged(MouseEvent e) {
            redispatchMouseEvent(e);
        }

        public void mouseClicked(MouseEvent e) {
            redispatchMouseEvent(e);
        }

        public void mouseEntered(MouseEvent e) {
            redispatchMouseEvent(e);
        }

        public void mouseExited(MouseEvent e) {
            redispatchMouseEvent(e);
        }

        public void mousePressed(MouseEvent e) {
            isPress = true;
            redispatchMouseEvent(e);
            isPress = false;
        }

        public void mouseReleased(MouseEvent e) {
            redispatchMouseEvent(e);
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            redispatchMouseWheelEvent(e);
        }
        
        //A basic implementation of redispatching events.
        private void redispatchMouseWheelEvent(MouseWheelEvent e) {
            Point glassPanePoint = e.getPoint();
            Point containerPoint = SwingUtilities.convertPoint(
                                            glassPane,
                                            glassPanePoint,
                                            canvas);
            boolean inCanvas = !(containerPoint.y < 0 || containerPoint.x < 0 || containerPoint.x > canvas.getWidth() || containerPoint.y > canvas.getHeight());
            if (inCanvas) {
                // The mouse it probably over the canvas 
                
                Point scaledPoint = new Point((int)(containerPoint.x * retinaScale),
                        (int)(containerPoint.y * retinaScale));
                
                if (textCmp != null && SwingUtilities.getWindowAncestor(textCmp) != null && textCmp.getX() <= scaledPoint.x && textCmp.getY() <= scaledPoint.y && textCmp.getWidth() + textCmp.getX() > scaledPoint.x && textCmp.getHeight() + textCmp.getY() > scaledPoint.y) {
                    
                    Point componentPoint = SwingUtilities.convertPoint(canvas, scaledPoint, textCmp);
                    java.awt.Component target = SwingUtilities.getDeepestComponentAt(
                                            textCmp,
                                            componentPoint.x,
                                            componentPoint.y);
                    try {
                        componentPoint = SwingUtilities.convertPoint(textCmp, componentPoint, target);
                        target.dispatchEvent(new MouseWheelEvent(target,
                                                                 e.getID(),
                                                                 e.getWhen(),
                                                                 e.getModifiers(),
                                                                 componentPoint.x,
                                                                 componentPoint.y,
                                                                 e.getClickCount(),
                                                                 e.isPopupTrigger(),
                                                                 e.getScrollType(),
                                                                 e.getScrollAmount(),
                                                                 e.getWheelRotation()));
                    } catch(Exception err) {
                        // swallow this exception, it happens during scroll wheel
                        // and I don't think there's much we can do about it
                    }
                    return;
                }
            }
            
            //we're not in the canvas
            // redispatch to the content pane
            containerPoint = SwingUtilities.convertPoint(
                                        glassPane,
                                        glassPanePoint,
                                        contentPane);
            java.awt.Component component = 
                SwingUtilities.getDeepestComponentAt(
                                        contentPane,
                                        containerPoint.x,
                                        containerPoint.y);
            if (component != null) {
                Point componentPoint = SwingUtilities.convertPoint(
                                                glassPane,
                                                glassPanePoint,
                                                component);

                component.dispatchEvent(new MouseWheelEvent(component,
                                                         e.getID(),
                                                         e.getWhen(),
                                                         e.getModifiers(),
                                                         componentPoint.x,
                                                         componentPoint.y,
                                                         e.getClickCount(),
                                                         e.isPopupTrigger(),
                                                         e.getScrollType(),
                                                         e.getScrollAmount(),
                                                         e.getWheelRotation()
                ));
            }
        }

        
        
        //A basic implementation of redispatching events.
        private void redispatchMouseEvent(MouseEvent e) {
            Point glassPanePoint = e.getPoint();
            Point containerPoint = null;
            java.awt.Component component = null;
            containerPoint = SwingUtilities.convertPoint(
                                            glassPane,
                                            glassPanePoint,
                                            canvas);
            boolean inCanvas = !(containerPoint.y < 0 || containerPoint.x < 0 || containerPoint.x > canvas.getWidth() || containerPoint.y > canvas.getHeight());
            
            if (inCanvas) {
                Point scaledPoint = new Point((int)(containerPoint.x * retinaScale),
                        (int)(containerPoint.y * retinaScale));
                boolean isTextEditing=false;
                try {
                    isTextEditing = isEditingText();
                } catch (Throwable t){
                    isTextEditing = false;
                }
                if (isTextEditing && textCmp != null && textCmp.getX() <= scaledPoint.x && textCmp.getY() <= scaledPoint.y && textCmp.getWidth() + textCmp.getX() > scaledPoint.x && textCmp.getHeight() + textCmp.getY() > scaledPoint.y) {
                    
                    Point componentPoint = SwingUtilities.convertPoint(canvas, scaledPoint, textCmp);
                    java.awt.Component target = SwingUtilities.getDeepestComponentAt(
                                            textCmp,
                                            componentPoint.x,
                                            componentPoint.y);
                    componentPoint = SwingUtilities.convertPoint(textCmp, componentPoint, target);
                    target.dispatchEvent(new MouseEvent(target,
                                                             e.getID(),
                                                             e.getWhen(),
                                                             e.getModifiers(),
                                                             componentPoint.x,
                                                             componentPoint.y,
                                                             e.getClickCount(),
                                                             e.isPopupTrigger()));
                    return;
                }
                
            }
            
            
            //we're not in the canvas
            // redispatch to the content pane
            containerPoint = SwingUtilities.convertPoint(
                                        glassPane,
                                        glassPanePoint,
                                        contentPane);
            component = 
                SwingUtilities.getDeepestComponentAt(
                                        contentPane,
                                        containerPoint.x,
                                        containerPoint.y);
            
            if (component != null) {
                Point componentPoint = SwingUtilities.convertPoint(
                                                glassPane,
                                                glassPanePoint,
                                                component);

                component.dispatchEvent(new MouseEvent(component,
                                                         e.getID(),
                                                         e.getWhen(),
                                                         e.getModifiers(),
                                                         componentPoint.x,
                                                         componentPoint.y,
                                                         e.getClickCount(),
                                                         e.isPopupTrigger()));
            } 
                
            

        }
    }
   
    // START NATIVE BROWSER WINDOW METHODS---------------------------------------
   
    /**
     * We create a default browser window factory that always creates a JavaFX 
     * browser.  We use a factory to make it easier for libraries to provide their 
     * own browser implementation depending on their needs.  For example, the AppleSignin cn1lib
     * provides its own WebView implementation because Apple login doesn't seem to work
     * in JavaFX's webview.
     */
    private BrowserWindowFactory browserWindowFactory = createBrowserWindowFactory();
    
    protected BrowserWindowFactory createBrowserWindowFactory() {
        return null;
    }
   
    /**
     * Gets the factory used for creating BrowserWindows.
     * @return 
     */
    public BrowserWindowFactory getBrowserWindowFactory() {
        return browserWindowFactory;
    }

    /**
     * Sets the browser window factory used to create new browser windows.
     * @param newFactory The new factory.
     * @return The old factory.
     */
    public BrowserWindowFactory setBrowserWindowFactory(BrowserWindowFactory newFactory) {
        BrowserWindowFactory old = browserWindowFactory;
        browserWindowFactory = newFactory;
        return old;
    }
   
    @Override
    public Object createNativeBrowserWindow(String startURL) {
        
        return browserWindowFactory.createBrowserWindow(startURL);
    }
    
   @Override
    public void addNativeBrowserWindowOnLoadListener(Object window, com.codename1.ui.events.ActionListener l) {
        ((AbstractBrowserWindowSE)window).addLoadListener(l);
    }
    public void removeNativeBrowserWindowOnLoadListener(Object window, com.codename1.ui.events.ActionListener l) {
        ((AbstractBrowserWindowSE)window).removeLoadListener(l);
    }
    
    @Override
    public void nativeBrowserWindowSetSize(Object window, int width, int height) {
        ((AbstractBrowserWindowSE)window).setSize(width, height);
    }
    
    @Override
    public void nativeBrowserWindowSetTitle(Object window, String title) {
        ((AbstractBrowserWindowSE)window).setTitle(title);
    }
    
    @Override
    public void nativeBrowserWindowShow(Object window) {
        ((AbstractBrowserWindowSE)window).show();
    }
    
    @Override
    public void nativeBrowserWindowHide(Object window) {
        ((AbstractBrowserWindowSE)window).hide();
    }
    
    @Override
    public void nativeBrowserWindowCleanup(Object window) {
        ((AbstractBrowserWindowSE)window).cleanup();
    }
    
    @Override
    public void nativeBrowserWindowEval(Object window, BrowserWindow.EvalRequest req) {
        ((AbstractBrowserWindowSE)window).eval(req);
    }
    
    @Override
    public void nativeBrowserWindowAddCloseListener(Object window, com.codename1.ui.events.ActionListener l) {
        ((AbstractBrowserWindowSE)window).addCloseListener(l);
    }

    @Override
    public void nativeBrowserWindowRemoveCloseListener(Object window, com.codename1.ui.events.ActionListener l) {
        ((AbstractBrowserWindowSE)window).removeCloseListener(l);
        
    }
    // END NATIVE BROWSER WINDOW METHODS---------------------------------------------------------
}