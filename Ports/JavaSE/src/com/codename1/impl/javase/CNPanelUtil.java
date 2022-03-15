/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.javase;

import com.codename1.ui.Display;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.lang.reflect.Method;

/**
 *
 * @author shannah
 */
public class CNPanelUtil {
    private static final boolean isWindows;
    static {
        isWindows = File.separatorChar == '\\';
    }
    public static void initializeCN1(final java.awt.Container container, final Object mainApp, final File appHomeDir) {
        try {
            Class.forName("org.cef.CefApp");
            System.setProperty("cn1.javase.implementation", "cef");
            //System.setProperty("cn1.cef.bundled", "true");
        } catch (Throwable ex){}

        JavaSEPort.setNativeTheme("/NativeTheme.res");
        JavaSEPort.blockMonitors();
        JavaSEPort.setAppHomeDir(appHomeDir.getAbsolutePath());
        JavaSEPort.setExposeFilesystem(true);
        JavaSEPort.setTablet(true);
        JavaSEPort.setUseNativeInput(true);
        JavaSEPort.setShowEDTViolationStacks(false);
        JavaSEPort.setShowEDTWarnings(false);
        
        if(isWindows) {
            JavaSEPort.setFontFaces("ArialUnicodeMS", "SansSerif", "Monospaced");
        } else {
            JavaSEPort.setFontFaces("Arial", "SansSerif", "Monospaced");
        }

        Toolkit tk = Toolkit.getDefaultToolkit();
        JavaSEPort.setDefaultPixelMilliRatio(tk.getScreenResolution() / 25.4 * JavaSEPort.getRetinaScale());
        Display.init(container);
        Display.getInstance().setProperty("build_key", "");
        Display.getInstance().setProperty("package_name", "");
        Display.getInstance().setProperty("built_by_user", "");
        //placeholder
        Display.getInstance().setProperty("AppName", "");
        Display.getInstance().setProperty("AppVersion", "");
        Display.getInstance().setProperty("Platform", System.getProperty("os.name"));
        Display.getInstance().setProperty("OSVer", System.getProperty("os.version"));
        container.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e); //To change body of generated methods, choose Tools | Templates.
            }
            
            
        });
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                try {
                    Class mainAppClass = mainApp.getClass();
                    Method init = mainAppClass.getMethod("init", Object.class);
                    init.invoke(mainApp, container);
                    Method start = mainAppClass.getMethod("start");
                    start.invoke(mainApp);
                } catch (Exception ex) {
                    throw new RuntimeException("Failed to initialize Codename One applet for class "+mainApp.getClass(), ex);
                }
            }
        });
        
    }
}
