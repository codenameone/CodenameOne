package com.codename1.settings;

import com.codename1.impl.javase.JavaSEPort;
import com.codename1.ui.Display;

import java.awt.Desktop;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.KeyboardFocusManager;
import java.awt.Taskbar;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.KeyStroke;

public class CodenameOneSettingsStub implements Runnable, WindowListener {
    static final String APP_DISPLAY_NAME = "Codename One Settings";
    private static final String APP_TITLE = "Codename One Settings";
    private static final String APP_STORAGE_NAME = "CodenameOneSettings";
    static final String APP_VERSION = resolveApplicationVersion();
    private static final int APP_WIDTH = 1470;
    private static final int APP_HEIGHT = 612;
    private static final double APP_UI_SCALE = 1.0;
    private static final boolean APP_RESIZEABLE = true;
    private static final boolean APP_FULLSCREEN = false;
    private static final String APP_DESKTOP_TITLEBAR = "native";
    private static final boolean APP_DESKTOP_INTERACTIVE_SCROLLBARS = true;
    private static final boolean isWindows = File.separatorChar == '\\';

    private static JFrame frm;
    private CodenameOneSettings mainApp;

    public static void main(String[] args) {
        configureDesktopAppIdentity();
        if (System.getProperty("settings.version") == null) {
            System.setProperty("settings.version", APP_VERSION);
        }
        try {
            Class.forName("org.cef.CefApp");
            System.setProperty("cn1.javase.implementation", "cef");
        } catch (Throwable ex) {
        }

        JavaSEPort.setNativeTheme("/NativeTheme.res");
        JavaSEPort.blockMonitors();
        JavaSEPort.setAppHomeDir("." + APP_STORAGE_NAME);
        JavaSEPort.setExposeFilesystem(true);
        JavaSEPort.setTablet(true);
        JavaSEPort.setUseNativeInput(true);
        JavaSEPort.setShowEDTViolationStacks(false);
        JavaSEPort.setShowEDTWarnings(false);
        JavaSEPort.setFullScreen(APP_FULLSCREEN);
        JavaSEPort.setDesktopTitleBarMode(APP_DESKTOP_TITLEBAR);
        JavaSEPort.setDesktopInteractiveScrollbars(APP_DESKTOP_INTERACTIVE_SCROLLBARS);
        if (isWindows) {
            JavaSEPort.setFontFaces("ArialUnicodeMS", "SansSerif", "Monospaced");
        } else {
            JavaSEPort.setFontFaces("Arial", "SansSerif", "Monospaced");
        }

        frm = new JFrame(APP_TITLE);
        Toolkit tk = Toolkit.getDefaultToolkit();
        JavaSEPort.setDefaultPixelMilliRatio(tk.getScreenResolution() / 25.4
                * JavaSEPort.getRetinaScale() * APP_UI_SCALE);
        Display.init(frm.getContentPane());
        Display.getInstance().setProperty("AppName", APP_DISPLAY_NAME);
        Display.getInstance().setProperty("AppVersion", APP_VERSION);
        Display.getInstance().setProperty("Platform", System.getProperty("os.name"));
        Display.getInstance().setProperty("OSVer", System.getProperty("os.version"));
        installFontShortcutDispatcher();
        SwingUtilities.invokeLater(new CodenameOneSettingsStub());
    }

    static void configureDesktopAppIdentity() {
        System.setProperty("apple.awt.application.name", APP_DISPLAY_NAME);
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", APP_DISPLAY_NAME);
        System.setProperty("sun.awt.application.name", APP_DISPLAY_NAME);
        System.setProperty("sun.awt.X11.XWMClass", APP_STORAGE_NAME);
    }

    static String resolveApplicationVersion() {
        Package appPackage = CodenameOneSettingsStub.class.getPackage();
        String version = appPackage == null ? null : appPackage.getImplementationVersion();
        return version == null || version.length() == 0 ? "development" : version;
    }

    private static void installFontShortcutDispatcher() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() != KeyEvent.KEY_PRESSED || (!e.isMetaDown() && !e.isControlDown())) {
            return false;
        }
        switch (e.getKeyCode()) {
                case KeyEvent.VK_PLUS:
                case KeyEvent.VK_EQUALS:
                case KeyEvent.VK_ADD:
                    CodenameOneSettings.adjustActiveFontSizeForDesktopShortcut(2);
                    return true;
            case KeyEvent.VK_MINUS:
            case KeyEvent.VK_SUBTRACT:
                CodenameOneSettings.adjustActiveFontSizeForDesktopShortcut(-2);
                return true;
            case KeyEvent.VK_0:
            case KeyEvent.VK_NUMPAD0:
                CodenameOneSettings.resetActiveFontSizeForDesktopShortcut();
                return true;
            default:
                return false;
        }
        });
    }

    @Override
    public void run() {
        frm.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frm.setName(APP_DISPLAY_NAME);
        frm.addWindowListener(this);
        applyApplicationIcon(frm);
        installFileMenu(frm);
        installAboutHandler();
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (APP_FULLSCREEN && gd.isFullScreenSupported()) {
            frm.setResizable(false);
            frm.setUndecorated(true);
            gd.setFullScreenWindow(frm);
        } else {
            frm.setLocationByPlatform(true);
            frm.setResizable(APP_RESIZEABLE);
            frm.getContentPane().setPreferredSize(new java.awt.Dimension(APP_WIDTH, APP_HEIGHT));
            frm.getContentPane().setMinimumSize(new java.awt.Dimension(900, 560));
            frm.pack();
        }
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                if (Display.getInstance().isEdt()) {
                    mainApp = new CodenameOneSettings();
                    mainApp.init(this);
                    mainApp.start();
                    SwingUtilities.invokeLater(this);
                } else {
                    frm.setVisible(true);
                    scheduleScreenshotIfRequested();
                }
            }
        });
    }

    private static void scheduleScreenshotIfRequested() {
        String screenshot = System.getProperty("settings.screenshot");
        if (screenshot == null || screenshot.length() == 0) {
            return;
        }
        Timer timer = new Timer(1200, e -> {
            try {
                BufferedImage image = new BufferedImage(frm.getContentPane().getWidth(),
                        frm.getContentPane().getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = image.createGraphics();
                frm.getContentPane().paint(graphics);
                graphics.dispose();
                ImageIO.write(image, "png", new File(screenshot));
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                if (!"false".equals(System.getProperty("settings.screenshot.exit"))) {
                    Display.getInstance().exitApplication();
                }
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private static void applyApplicationIcon(JFrame frame) {
        List<Image> icons = loadApplicationIcons();
        if (!icons.isEmpty()) {
            frame.setIconImages(icons);
            Image largestIcon = icons.get(icons.size() - 1);
            applyTaskbarIcon(largestIcon);
            applyAppleApplicationIcon(largestIcon);
            requestDesktopForeground();
        }
    }

    private static List<Image> loadApplicationIcons() {
        ArrayList<Image> icons = new ArrayList<Image>();
        addIcon(icons, "/applicationIconImage_16x16.png");
        addIcon(icons, "/applicationIconImage_20x20.png");
        addIcon(icons, "/applicationIconImage_32x32.png");
        addIcon(icons, "/applicationIconImage_40x40.png");
        addIcon(icons, "/applicationIconImage_64x64.png");
        addIcon(icons, "/icon.png");
        return icons;
    }

    private static void addIcon(List<Image> icons, String resource) {
        URL url = CodenameOneSettingsStub.class.getResource(resource);
        if (url != null) {
            icons.add(new ImageIcon(url).getImage());
        }
    }

    private static void applyTaskbarIcon(Image icon) {
        try {
            if (Taskbar.isTaskbarSupported()) {
                Taskbar taskbar = Taskbar.getTaskbar();
                if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                    taskbar.setIconImage(icon);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void applyAppleApplicationIcon(Image icon) {
        try {
            Class<?> applicationClass = Class.forName("com.apple.eawt.Application");
            Object application = applicationClass.getMethod("getApplication").invoke(null);
            applicationClass.getMethod("setDockIconImage", Image.class).invoke(application, icon);
        } catch (Throwable ignored) {
        }
    }

    private static void requestDesktopForeground() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.APP_REQUEST_FOREGROUND)) {
                    desktop.requestForeground(true);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void installFileMenu(JFrame frame) {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        file.add(menuItem("Save", KeyEvent.VK_S, () -> CodenameOneSettings.saveActiveSettingsForDesktopMenu()));
        file.add(menuItem("Open Project Folder", KeyEvent.VK_O, () -> CodenameOneSettings.openActiveProjectFolderForDesktopMenu()));
        file.addSeparator();
        file.add(menuItem("Basic", KeyEvent.VK_1, () -> CodenameOneSettings.goActiveSectionForDesktopMenu(CodenameOneSettings.Section.BASIC)));
        file.add(menuItem("Build Hints", KeyEvent.VK_2, () -> CodenameOneSettings.goActiveSectionForDesktopMenu(CodenameOneSettings.Section.BUILD_HINTS)));
        file.add(menuItem("Extensions", KeyEvent.VK_3, () -> CodenameOneSettings.goActiveSectionForDesktopMenu(CodenameOneSettings.Section.EXTENSIONS)));
        file.addSeparator();
        file.add(menuItem("Toggle Dark Mode", KeyEvent.VK_D, () -> CodenameOneSettings.toggleActiveDarkModeForDesktopMenu()));
        file.add(menuItem("Increase Font Size", KeyEvent.VK_EQUALS, () -> CodenameOneSettings.adjustActiveFontSizeForDesktopShortcut(2)));
        file.add(menuItem("Decrease Font Size", KeyEvent.VK_MINUS, () -> CodenameOneSettings.adjustActiveFontSizeForDesktopShortcut(-2)));
        file.add(menuItem("Reset Font Size", KeyEvent.VK_0, () -> CodenameOneSettings.resetActiveFontSizeForDesktopShortcut()));
        bar.add(file);
        frame.setJMenuBar(bar);
    }

    private static void installAboutHandler() {
        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.APP_ABOUT)) {
                Desktop.getDesktop().setAboutHandler(e -> CodenameOneSettings.showActiveAboutForDesktopMenu());
            }
        } catch (Throwable ignored) {
        }
    }

    private static JMenuItem menuItem(String text, int key, Runnable action) {
        JMenuItem item = new JMenuItem(text);
        if (key > 0) {
            int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
            item.setAccelerator(KeyStroke.getKeyStroke(key, mask));
        }
        item.addActionListener(e -> action.run());
        return item;
    }

    @Override
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void windowClosing(WindowEvent e) {
        Display.getInstance().exitApplication();
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }
}
