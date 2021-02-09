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

#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

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
 * A wrapper class around a Codename One app, allows building desktop Java
 * applications.
 *
 * @author Shai Almog
 */
public class ${mainName}Stub implements Runnable, WindowListener {
    private static final String APP_TITLE = "Hi World";
    private static final String APP_NAME = "${mainName}";
    private static final String APP_VERSION = "1.0";
    private static final int APP_WIDTH = 800;
    private static final int APP_HEIGHT = 600;
    private static final boolean APP_ADAPT_TO_RETINA = true;
    private static final boolean APP_RESIZEABLE = true;
    private static final boolean APP_FULLSCREEN = false;
    public static final String BUILD_KEY = "";
    public static final String PACKAGE_NAME = "";
    public static final String BUILT_BY_USER = "";
    private static final int fontSizeSmall = -1;
    private static final int fontSizeMedium = -1;
    private static final int fontSizeLarge = -1;
    private static final boolean isWindows;
    static {
        isWindows = File.separatorChar == '\\';
    }

    private static final String[] fontFaces = null;

    private static JFrame frm;
    private ${mainName} mainApp;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            Class.forName("org.cef.CefApp");
            System.setProperty("cn1.javase.implementation", "cef");
            //System.setProperty("cn1.cef.bundled", "true");
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
            // workaround for a bug in Windows where Arials unicode version isn't used
            if(isWindows) {
                JavaSEPort.setFontFaces("ArialUnicodeMS", "SansSerif", "Monospaced");
            } else {
                JavaSEPort.setFontFaces("Arial", "SansSerif", "Monospaced");
            }
        }


        frm = new JFrame(APP_TITLE);
        Display.init(frm.getContentPane());
        Display.getInstance().setProperty("build_key", BUILD_KEY);
        Display.getInstance().setProperty("package_name", PACKAGE_NAME);
        Display.getInstance().setProperty("built_by_user", BUILT_BY_USER);
        //placeholder
        Display.getInstance().setProperty("AppName", APP_NAME);
        Display.getInstance().setProperty("AppVersion", APP_VERSION);
        Display.getInstance().setProperty("Platform", System.getProperty("os.name"));
        Display.getInstance().setProperty("OSVer", System.getProperty("os.version"));

        SwingUtilities.invokeLater(new ${mainName}Stub());
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
            if(APP_ADAPT_TO_RETINA &&
                    Math.max(Toolkit.getDefaultToolkit().getScreenSize().width, Toolkit.getDefaultToolkit().getScreenSize().height) > 2000) {
                JavaSEPort.setDefaultPixelMilliRatio(new Double(20));
                if(fontSizeSmall > -1) {
                    JavaSEPort.setFontSize(fontSizeSmall * 2, fontSizeMedium * 2, fontSizeLarge * 2);
                } else {
                    JavaSEPort.setFontSize(22, 30, 38);
                }
            } else {
                JavaSEPort.setDefaultPixelMilliRatio(new Double(10));
                if(fontSizeSmall > -1) {
                    JavaSEPort.setFontSize(fontSizeSmall, fontSizeMedium, fontSizeLarge);
                }
            }
        } else {
            frm.setLocationByPlatform(true);
            frm.setResizable(APP_RESIZEABLE);
            int w = APP_WIDTH;
            int h = APP_HEIGHT;
            if(APP_ADAPT_TO_RETINA &&
                    Math.max(Toolkit.getDefaultToolkit().getScreenSize().width, Toolkit.getDefaultToolkit().getScreenSize().height) > 2000) {
                w *= 2;
                h *= 2;
                JavaSEPort.setDefaultPixelMilliRatio(new Double(20));
                if(fontSizeSmall > -1) {
                    JavaSEPort.setFontSize(fontSizeSmall * 2, fontSizeMedium * 2, fontSizeLarge * 2);
                } else {
                    JavaSEPort.setFontSize(22, 30, 38);
                }
            } else {
                JavaSEPort.setDefaultPixelMilliRatio(new Double(10));
                if(fontSizeSmall > -1) {
                    JavaSEPort.setFontSize(fontSizeSmall, fontSizeMedium, fontSizeLarge);
                }
            }
            frm.getContentPane().setPreferredSize(new java.awt.Dimension(w, h));
            frm.getContentPane().setMinimumSize(new java.awt.Dimension(w, h));
            frm.getContentPane().setMaximumSize(new java.awt.Dimension(w, h));

            // replaceable with the build hint desktop.framePrepare
            framePrepare(frm);
        }
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                if(Display.getInstance().isEdt()) {
                    mainApp = new ${mainName}();
                    mainApp.init(this);
                    mainApp.start();
                    SwingUtilities.invokeLater(this);
                } else {

                    // replaceable with the build hint desktop.frameShow
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
        // fix for https://stackoverflow.com/questions/6178132/fullscreen-java-app-minimizes-when-screensaver-turns-on
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
