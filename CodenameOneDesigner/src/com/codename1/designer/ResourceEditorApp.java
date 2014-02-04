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

package com.codename1.designer;

import com.codename1.ui.Display;
import com.codename1.impl.javase.JavaSEPortWithSVGSupport;
import com.codename1.ui.resource.util.QuitAction;
import com.codename1.ui.util.EditableResources;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Properties;
import javax.swing.JPanel;
import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;

/**
 * The main class of the application.
 *
 * @author Shai Almog
 */
public class ResourceEditorApp extends SingleFrameApplication {
    private File fileToLoad;
    public final static boolean IS_MAC;
    private static ResourceEditorView ri;
    
    static void setMacApplicationEventHandled(Object event, boolean handled) {
        if (event != null) {
            try {
                Method setHandledMethod = event.getClass().getDeclaredMethod("setHandled", new Class[] { boolean.class });

                setHandledMethod.invoke(event, new Object[] { Boolean.valueOf(handled) });
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }    
    

    static {
        String n = System.getProperty("os.name");
        if(n != null && n.startsWith("Mac")) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Codename One Designer");
            try {
                Class applicationClass = Class.forName("com.apple.eawt.Application");

                Object macApp = applicationClass.getConstructor((Class[])null).newInstance((Object[])null);

                Class applicationListenerClass = Class.forName("com.apple.eawt.ApplicationListener");

                Method addListenerMethod = applicationClass.getDeclaredMethod("addApplicationListener", new Class[] { applicationListenerClass });
                
                Object proxy = Proxy.newProxyInstance(ResourceEditorApp.class.getClassLoader(), new Class[] { applicationListenerClass }, 
                        new InvocationHandler() {
                    public Object invoke(Object o, Method method, Object[] os) throws Throwable {
                        if(method.getName().equals("handleQuit")) {
                            setMacApplicationEventHandled(os[0], true);
                            QuitAction.INSTANCE.quit();
                            return null;
                        }
                        if(method.getName().equals("handleAbout")) {
                            setMacApplicationEventHandled(os[0], true);
                            ri.aboutActionPerformed();
                            return null;
                        }
                        return null;
                    }
                });

                addListenerMethod.invoke(macApp, new Object[] { proxy });
                
                Method enableAboutMethod = applicationClass.getDeclaredMethod("setEnabledAboutMenu", new Class[] { boolean.class });
                enableAboutMethod.invoke(macApp, new Object[] { Boolean.TRUE });
                //ImageIcon i = new ImageIcon("/application64.png");
                //Method setDockIconImage = applicationClass.getDeclaredMethod("setDockIconImage", new Class[] { java.awt.Image.class });
                //setDockIconImage.invoke(macApp, new Object[] { i.getImage() });
            } catch(Throwable t) {
                t.printStackTrace();
            }
            IS_MAC = true;
        } else {
            IS_MAC = false;
        }
    }
        
    /**
     * At startup create and show the main frame of the application.
     */
    @Override 
    protected void startup() {
        ri = new ResourceEditorView(this, fileToLoad);
        show(ri);
        Image large = Toolkit.getDefaultToolkit().createImage(getClass().getResource("/application64.png"));
        Image small = Toolkit.getDefaultToolkit().createImage(getClass().getResource("/application48.png"));
        try {
            // setIconImages is only available in JDK 1.6
            getMainFrame().setIconImages(Arrays.asList(new Image[] {large, small}));
        } catch (Throwable err) {
            getMainFrame().setIconImage(small);
        }
    }

    @Override 
    protected void initialize(String[] argv) {
        if(argv != null && argv.length > 0) {
            File f = new File(argv[0]);
            if(f.exists()) {
                fileToLoad = f;
            }
        }
    }
 
    /**
     * A convenient static getter for the application instance.
     * @return the instance of ResourceEditorApp
     */
    public static ResourceEditorApp getApplication() {
        return (ResourceEditorApp) Application.getInstance();
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) throws Exception {
        JavaSEPortWithSVGSupport.blockMonitors();
        JavaSEPortWithSVGSupport.setDesignMode(true);
        JavaSEPortWithSVGSupport.setShowEDTWarnings(false);
        JavaSEPortWithSVGSupport.setShowEDTViolationStacks(false);
        if(args.length > 0) {
            if(args[0].equalsIgnoreCase("-buildVersion")) {
                Properties p = new Properties();
                try {
                    p.load(ResourceEditorApp.class.getResourceAsStream("/version.properties"));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                System.out.println(p.getProperty("build", "1"));
                System.exit(0);
                return;
            }
            if(args[0].equalsIgnoreCase("gen")) {
                java.awt.Container cnt = new java.awt.Container();
                com.codename1.ui.Display.init(cnt);
                File output = new File(args[1]);
                generateResourceFile(output, args[2], args[3]);
                System.exit(0);
                return;
            }
            if(args[0].equalsIgnoreCase("-regen")) {
                java.awt.Container cnt = new java.awt.Container();
                com.codename1.ui.Display.init(cnt);
                File output = new File(args[1]);
                EditableResources res = new EditableResources();
                res.openFileWithXMLSupport(output);
                FileOutputStream fos = new FileOutputStream(output);
                res.save(fos);
                fos.close();
                generate(res, output);
                System.exit(0);
                return;
            }
        }
        JavaSEPortWithSVGSupport.setDefaultInitTarget(new JPanel());
        Display.init(null);
        launch(ResourceEditorApp.class, args);
    }
    
    private static void generateResourceFile(File f, String themeName, String ui) throws Exception {        
        System.out.println("Generating resource file " + f + " theme " + themeName + " template " + ui);
        EditableResources res = new EditableResources();
        
        //"native", "leather", "tzone", "tipster", "blank"
        String template = "Native_Theme";
        if(themeName.equalsIgnoreCase("leather")) {
            template = "Leather";
        }
        if(themeName.equalsIgnoreCase("chrome")) {
            template = "Chrome";
        }
        if(themeName.equalsIgnoreCase("tzone")) {
            template = "tzone_theme";
        }
        if(themeName.equalsIgnoreCase("tipster")) {
            template = "tipster_theme";
        }
        if(themeName.equalsIgnoreCase("socialboo")) {
            template = "socialboo";
        }
        if(themeName.equalsIgnoreCase("mapper")) {
            template = "mapper";
        }
        
        res.setTheme("Theme", importRes(res, template));
        
        if("HiWorld".equalsIgnoreCase(ui)) {
            importRes(res, "HiWorldTemplate");
            generate(res, f);
        } else {
            if("Tabs".equalsIgnoreCase(ui)) {
                importRes(res, "Tabs");
                generate(res, f);
            } else {
                if("List".equalsIgnoreCase(ui)) {
                    importRes(res, "ListOfItems");
                    generate(res, f);
                }
            }
        }
        
        FileOutputStream os = new FileOutputStream(f);
        res.save(os);
        os.close();
    }
    
    private static void generate(EditableResources res, File f) {
        ResourceEditorView.generateStateMachineCodeEx("Main", 
                new File(f.getParent() + File.separator + "generated" + File.separator + "StateMachineBase.java"), 
                false, res, null);
    }
    
    private static Hashtable importRes(EditableResources res, String file) {
        InputStream is = ResourceEditorApp.class.getResourceAsStream("/templates/" + file + ".res");
        Hashtable theme = new Hashtable();
        if(is != null) {
            try {
                EditableResources r = new EditableResources();
                r.openFile(is);
                is.close();
                if(r.getThemeResourceNames().length > 0) {
                    theme = r.getTheme(r.getThemeResourceNames()[0]);
                }
                ResourceEditorView.checkDuplicateResourcesLoop(r, res.getImageResourceNames(),
                        r.getImageResourceNames(), "Rename Image", "Image ", true, null);
                ResourceEditorView.checkDuplicateResourcesLoop(r, res.getL10NResourceNames(),
                        r.getL10NResourceNames(), "Rename Localization", "Localization ", true, null);
                ResourceEditorView.checkDuplicateResourcesLoop(r, res.getDataResourceNames(),
                        r.getDataResourceNames(), "Rename Data", "Data ", true, null);
                ResourceEditorView.checkDuplicateResourcesLoop(r, res.getUIResourceNames(),
                        r.getUIResourceNames(), "Rename GUI", "GUI ", true, null);
                ResourceEditorView.checkDuplicateResourcesLoop(r, res.getFontResourceNames(),
                        r.getFontResourceNames(), "Rename Font", "Font ", true, null);

                for (String s : r.getImageResourceNames()) {
                    if(r.isMultiImage(s)) {
                        res.setMultiImage(s, (EditableResources.MultiImage)r.getResourceObject(s));
                    } else {
                        res.setImage(s, r.getImage(s));
                    }
                }
                for (String s : r.getL10NResourceNames()) {
                    res.setL10N(s, (Hashtable)r.getResourceObject(s));
                }
                for (String s : r.getDataResourceNames()) {
                    res.setData(s, (byte[])r.getResourceObject(s));
                }
                for (String s : r.getUIResourceNames()) {
                    res.setUi(s, (byte[])r.getResourceObject(s));
                }
                for (String s : r.getFontResourceNames()) {
                    res.setFont(s, r.getFont(s));
                }
            } catch(IOException err) {
                err.printStackTrace();
            }
        }
        return theme;
    }
}
