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
package com.codename1.certificatewizard;

import com.codename1.impl.javase.JavaSEPort;
import com.codename1.certificatewizard.project.AndroidKeystoreGenerator;
import com.codename1.ui.Display;
import java.awt.Desktop;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.KeyboardFocusManager;
import java.awt.Taskbar;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Generated desktop wrapper for a Codename One app. Generated at build time by
 * the codenameone-maven-plugin's generate-desktop-app-wrapper goal from values
 * in codenameone_settings.properties and the codename1.arg.desktop.* build
 * hints. Drop a hand-written CertificateWizardStub.java into javase/src/desktop/java
 * to override generation completely.
 */
public class CertificateWizardStub implements Runnable, WindowListener {
    static final String APP_DISPLAY_NAME = "Certificate Wizard";
    private static final String APP_TITLE = "Certificate Wizard";
    private static final String APP_STORAGE_NAME = "CertificateWizard";
    private static final String APP_VERSION = "1.0";
    private static final int APP_WIDTH = 1260;
    private static final int APP_HEIGHT = 820;
    private static final boolean APP_ADAPT_TO_RETINA = true;
    private static final boolean APP_RESIZEABLE = true;
    private static final boolean APP_FULLSCREEN = false;
    // Desktop integration: title bar mode ("native"/"custom"/"toolbar") + interactive scrollbars.
    private static final String APP_DESKTOP_TITLEBAR = "native";
    private static final boolean APP_DESKTOP_INTERACTIVE_SCROLLBARS = true;
    public static final String BUILD_KEY = "";
    public static final String PACKAGE_NAME = "";
    public static final String BUILT_BY_USER = "";
    private static final boolean isWindows;
    static {
        isWindows = File.separatorChar == '\\';
    }

    private static final String[] fontFaces = null;

    private static JFrame frm;
    private CertificateWizard mainApp;

    public static void main(String[] args) {
        configureDesktopAppIdentity();
        try {
            Class.forName("org.cef.CefApp");
            System.setProperty("cn1.javase.implementation", "cef");
        } catch (Throwable ex){}

        JavaSEPort.setNativeTheme("/NativeTheme.res");
        JavaSEPort.blockMonitors();
        JavaSEPort.setAppHomeDir("." + APP_STORAGE_NAME);
        JavaSEPort.setExposeFilesystem(true);
        JavaSEPort.setTablet(true);
        JavaSEPort.setUseNativeInput(true);
        JavaSEPort.setShowEDTViolationStacks(false);
        JavaSEPort.setShowEDTWarnings(false);
        JavaSEPort.setFullScreen(APP_FULLSCREEN);

        // Desktop integration (only effective when running on the desktop).
        JavaSEPort.setDesktopTitleBarMode(APP_DESKTOP_TITLEBAR);
        JavaSEPort.setDesktopInteractiveScrollbars(APP_DESKTOP_INTERACTIVE_SCROLLBARS);

        if(fontFaces != null) {
            JavaSEPort.setFontFaces(fontFaces[0], fontFaces[1], fontFaces[2]);
        } else {
            if(isWindows) {
                JavaSEPort.setFontFaces("ArialUnicodeMS", "SansSerif", "Monospaced");
            } else {
                JavaSEPort.setFontFaces("Arial", "SansSerif", "Monospaced");
            }
        }

        frm = new JFrame(APP_TITLE);
        Toolkit tk = Toolkit.getDefaultToolkit();
        JavaSEPort.setDefaultPixelMilliRatio(tk.getScreenResolution() / 25.4 * JavaSEPort.getRetinaScale());
        Display.init(frm.getContentPane());
        Display.getInstance().setProperty("build_key", BUILD_KEY);
        Display.getInstance().setProperty("package_name", PACKAGE_NAME);
        Display.getInstance().setProperty("built_by_user", BUILT_BY_USER);
        Display.getInstance().setProperty("AppName", APP_DISPLAY_NAME);
        Display.getInstance().setProperty("AppVersion", APP_VERSION);
        Display.getInstance().setProperty("Platform", System.getProperty("os.name"));
        Display.getInstance().setProperty("OSVer", System.getProperty("os.version"));
        CertificateWizard.setAndroidKeystoreProvider(new AndroidKeystoreGenerator());
        installFontShortcutDispatcher();

        SwingUtilities.invokeLater(new CertificateWizardStub());
    }

    static void configureDesktopAppIdentity() {
        System.setProperty("apple.awt.application.name", APP_DISPLAY_NAME);
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", APP_DISPLAY_NAME);
        System.setProperty("sun.awt.application.name", APP_DISPLAY_NAME);
        System.setProperty("sun.awt.X11.XWMClass", APP_STORAGE_NAME);
    }

    private static void installFontShortcutDispatcher() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() != KeyEvent.KEY_PRESSED) {
                return false;
            }
            if (!e.isMetaDown() && !e.isControlDown()) {
                return false;
            }
            switch (e.getKeyCode()) {
                case KeyEvent.VK_PLUS:
                case KeyEvent.VK_EQUALS:
                case KeyEvent.VK_ADD:
                    CertificateWizard.adjustActiveFontSizeForDesktopShortcut(2);
                    return true;
                case KeyEvent.VK_MINUS:
                case KeyEvent.VK_SUBTRACT:
                    CertificateWizard.adjustActiveFontSizeForDesktopShortcut(-2);
                    return true;
                default:
                    return false;
            }
        });
    }

    public void run() {
        frm.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frm.setName(APP_DISPLAY_NAME);
        frm.addWindowListener(this);
        applyApplicationIcon(frm);
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if(APP_FULLSCREEN && gd.isFullScreenSupported()) {
            frm.setResizable(false);
            frm.setUndecorated(true);
            gd.setFullScreenWindow(frm);
        } else {
            frm.setLocationByPlatform(true);
            frm.setResizable(APP_RESIZEABLE);
            // custom desktop chrome draws its own title bar on an undecorated window
            if ("custom".equals(APP_DESKTOP_TITLEBAR)) {
                frm.setUndecorated(true);
            }
            int w = APP_WIDTH;
            int h = APP_HEIGHT;

            frm.getContentPane().setPreferredSize(new java.awt.Dimension(w, h));
            frm.getContentPane().setMinimumSize(new java.awt.Dimension(w, h));
            frm.getContentPane().setMaximumSize(new java.awt.Dimension(w, h));

            framePrepare(frm);
        }
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                if(Display.getInstance().isEdt()) {
                    mainApp = new CertificateWizard();
                    mainApp.init(this);
                    mainApp.start();
                    SwingUtilities.invokeLater(this);
                } else {
                    frameShow(frm);
                }
            }
        });
    }

    private void framePrepare(JFrame frm) {
        frm.pack();
    }

    private void frameShow(JFrame frm) {
        frm.setVisible(true);
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
        URL url = CertificateWizardStub.class.getResource(resource);
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

    @Override
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void windowClosing(WindowEvent e) {
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                mainApp.stop();
                mainApp.destroy();
                Display.getInstance().exitApplication();
            }
        });
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
        if(APP_FULLSCREEN) {
            GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            frm.setExtendedState(JFrame.MAXIMIZED_BOTH);
            if(gd.isFullScreenSupported()) {
                frm.setResizable(false);
                frm.setUndecorated(true);
                gd.setFullScreenWindow(frm);
            }
        }
    }
}
