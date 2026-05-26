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
package __PACKAGE__;

import com.codename1.impl.javase.JavaSEPort;
import com.codename1.ui.Display;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.Arrays;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Generated desktop wrapper for a Codename One app. Generated at build time by
 * the codenameone-maven-plugin's generate-desktop-app-wrapper goal from values
 * in codenameone_settings.properties and the codename1.arg.desktop.* build
 * hints. Drop a hand-written __MAIN_NAME__Stub.java into javase/src/desktop/java
 * to override generation completely.
 */
public class __MAIN_NAME__Stub implements Runnable, WindowListener {
    private static final String APP_TITLE = "__APP_TITLE__";
    private static final String APP_NAME = "__MAIN_NAME__";
    private static final String APP_VERSION = "__APP_VERSION__";
    private static final int APP_WIDTH = __APP_WIDTH__;
    private static final int APP_HEIGHT = __APP_HEIGHT__;
    private static final boolean APP_ADAPT_TO_RETINA = __APP_ADAPT_TO_RETINA__;
    private static final boolean APP_RESIZEABLE = __APP_RESIZEABLE__;
    private static final boolean APP_FULLSCREEN = __APP_FULLSCREEN__;
    public static final String BUILD_KEY = "";
    public static final String PACKAGE_NAME = "";
    public static final String BUILT_BY_USER = "";
    private static final boolean isWindows;
    static {
        isWindows = File.separatorChar == '\\';
    }

    private static final String[] fontFaces = null;

    private static JFrame frm;
    private __MAIN_NAME__ mainApp;

    public static void main(String[] args) {
        try {
            Class.forName("org.cef.CefApp");
            System.setProperty("cn1.javase.implementation", "cef");
        } catch (Throwable ex){}

        JavaSEPort.setNativeTheme("/NativeTheme.res");
        JavaSEPort.blockMonitors();
        JavaSEPort.setAppHomeDir("." + APP_NAME);
        JavaSEPort.setExposeFilesystem(true);
        JavaSEPort.setTablet(true);
        JavaSEPort.setUseNativeInput(true);
        JavaSEPort.setShowEDTViolationStacks(false);
        JavaSEPort.setShowEDTWarnings(false);
        JavaSEPort.setFullScreen(APP_FULLSCREEN);

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
        Display.getInstance().setProperty("AppName", APP_NAME);
        Display.getInstance().setProperty("AppVersion", APP_VERSION);
        Display.getInstance().setProperty("Platform", System.getProperty("os.name"));
        Display.getInstance().setProperty("OSVer", System.getProperty("os.version"));

        SwingUtilities.invokeLater(new __MAIN_NAME__Stub());
    }

    public void run() {
        frm.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frm.addWindowListener(this);
        ImageIcon ic16 = new ImageIcon(getClass().getResource("/applicationIconImage_16x16.png"));
        ImageIcon ic20 = new ImageIcon(getClass().getResource("/applicationIconImage_16x16.png"));
        ImageIcon ic32 = new ImageIcon(getClass().getResource("/applicationIconImage_16x16.png"));
        ImageIcon ic40 = new ImageIcon(getClass().getResource("/applicationIconImage_16x16.png"));
        ImageIcon ic64 = new ImageIcon(getClass().getResource("/applicationIconImage_16x16.png"));
        frm.setIconImages(Arrays.asList(ic16.getImage(), ic20.getImage(), ic32.getImage(), ic40.getImage(), ic64.getImage()));
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if(APP_FULLSCREEN && gd.isFullScreenSupported()) {
            frm.setResizable(false);
            frm.setUndecorated(true);
            gd.setFullScreenWindow(frm);
        } else {
            frm.setLocationByPlatform(true);
            frm.setResizable(APP_RESIZEABLE);
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
                    mainApp = new __MAIN_NAME__();
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
