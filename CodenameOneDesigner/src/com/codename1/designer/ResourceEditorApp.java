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
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.EditorTTFFont;
import com.codename1.ui.Font;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.resource.util.QuitAction;
import com.codename1.ui.util.EditableResources;
import com.codename1.ui.util.Resources;
import com.codename1.ui.util.UIBuilderOverride;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;
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
        
        // creates a deadlock between FX, Swing and CN1. Horrible horrible deadlock...
        JavaSEPortWithSVGSupport.blockNativeBrowser = true;
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
            if(args[0].equalsIgnoreCase("-img")) {
                java.awt.Container cnt = new java.awt.Container();
                com.codename1.ui.Display.init(cnt);
                String imageName;
                String fileName;
                if(args.length == 3) {
                    imageName = args[2];
                    fileName = args[2];
                } else {
                    if(args.length == 4) {
                        imageName = args[3];
                        fileName = args[2];
                    } else {
                        System.out.println("The img command works as: -img path_to_resourceFile.res pathToImageFile [image name]");
                        System.exit(1);
                        return;
                    }
                }
                
                File imageFile = new File(fileName);
                if(!imageFile.exists()) {
                    System.out.println("File not found: " + imageFile.getAbsolutePath());
                    System.exit(1);
                    return;
                }
                com.codename1.ui.Image img = ImageRGBEditor.createImageStatic(imageFile);
                
                boolean isXMLEnabled = Preferences.userNodeForPackage(ResourceEditorView.class).getBoolean("XMLFileMode", true);
                EditableResources.setXMLEnabled(isXMLEnabled);
                EditableResources res = new EditableResources();
                File resourceFile = new File(args[1]);
                res.openFileWithXMLSupport(resourceFile);
                res.setImage(imageName, img);
                try(FileOutputStream fos = new FileOutputStream(resourceFile)) {
                    res.save(fos);
                }
                res.saveXML(resourceFile);
                System.exit(0);
                return;
            }
            if(args[0].equalsIgnoreCase("-mimg")) {
                java.awt.Container cnt = new java.awt.Container();
                com.codename1.ui.Display.init(cnt);
                String fileName;
                if(args.length == 4) {
                    fileName = args[3];
                } else {
                    System.out.println("The mimg command works as: -img path_to_resourceFile.res dpi pathToImageFile");
                    System.out.println("dpi can be one of:  high, veryhigh, hd, 560, 2hd, 4k");
                    System.exit(1);
                    return;
                }
                String dpi = args[2];
                int dpiInt = -1;
                switch(dpi.toLowerCase()) {
                    case "high": 
                        dpiInt = 3;
                        break;
                    case "veryhigh": 
                        dpiInt = 4;
                        break;
                    case "hd": 
                        dpiInt = 5;
                        break;
                    case "560": 
                        dpiInt = 6;
                        break;
                    case "2hd": 
                        dpiInt = 7;
                        break;
                    case "4k": 
                        dpiInt = 8;
                        break;
                    default:
                        System.out.println("dpi can be one of:  high, veryhigh, hd, 560, 2hd, 4k");
                        System.exit(1);
                        return;
                }
                
                File imageFile = new File(fileName);
                if(!imageFile.exists()) {
                    System.out.println("File not found: " + imageFile.getAbsolutePath());
                    System.exit(1);
                    return;
                }
                
                boolean isXMLEnabled = Preferences.userNodeForPackage(ResourceEditorView.class).getBoolean("XMLFileMode", true);
                EditableResources.setXMLEnabled(isXMLEnabled);
                EditableResources res = new EditableResources();
                File resourceFile = new File(args[1]);
                res.openFileWithXMLSupport(resourceFile);
                AddAndScaleMultiImage.generateImpl(new File[] {imageFile}, 
                        res, dpiInt);
                try(FileOutputStream fos = new FileOutputStream(resourceFile)) {
                    res.save(fos);
                }
                res.saveXML(resourceFile);
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
            if(args[0].equalsIgnoreCase("mig")) {
                java.awt.Container cnt = new java.awt.Container();
                com.codename1.ui.Display.init(cnt);
                File projectDir = new File(args[1]);
                EditableResources res = new EditableResources();
                res.openFileWithXMLSupport(new File(args[2]));
                migrateGuiBuilder(projectDir, res, args[3]);
                System.exit(0);
                return;
            }
            if(args[0].equalsIgnoreCase("-regen")) {
                java.awt.Container cnt = new java.awt.Container();
                com.codename1.ui.Display.init(cnt);
                File output = new File(args[1]);
                EditableResources.setXMLEnabled(true);
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
        if(themeName.equalsIgnoreCase("flatblue")) {
            template = "FlatBlueTheme";
        }
        if(themeName.equalsIgnoreCase("flatred")) {
            template = "FlatRedTheme";
        }
        if(themeName.equalsIgnoreCase("flatorange")) {
            template = "FlatOrangeTheme";
        }
        if(themeName.equalsIgnoreCase("business")) {
            template = "BusinessTheme";
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
                } else {
                    if("NewHi".equalsIgnoreCase(ui)) {
                        importImage("android-icon.png", res);
                        importImage("apple-icon.png", res);
                        importImage("windows-icon.png", res);
                        importImage("duke-no-logos.png", res);
                        Map m = res.getTheme("Theme");
                        m.put("GetStarted.fgColor", "ffffff");
                        m.put("GetStarted.sel#fgColor", "ffffff");
                        m.put("GetStarted.press#fgColor", "ffffff");
                        m.put("GetStarted.bgColor", "339900");
                        m.put("GetStarted.sel#bgColor", "339900");
                        m.put("GetStarted.press#bgColor", "339900");
                        m.put("GetStarted.transparency", "255");
                        m.put("GetStarted.sel#transparency", "255");
                        m.put("GetStarted.press#transparency", "255");
                        Integer centerAlign = new Integer(4);
                        m.put("GetStarted.align", centerAlign);
                        m.put("GetStarted.sel#align", centerAlign);
                        m.put("GetStarted.press#align", centerAlign);
                        m.put("GetStarted.padding", "1,1,1,1");
                        m.put("GetStarted.padUnit", new byte[] {Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS});
                        m.put("GetStarted.sel#padding", "1,1,1,1");
                        m.put("GetStarted.sel#padUnit", new byte[] {Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS});
                        m.put("GetStarted.press#padding", "1,1,1,1");
                        m.put("GetStarted.press#padUnit", new byte[] {Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS});
                        m.put("GetStarted.font", new EditorTTFFont("native:MainLight", 3, 2.5f, Font.createSystemFont(Font.FACE_SYSTEM,
                                Font.STYLE_PLAIN, Font.SIZE_MEDIUM)));
                    }
                }
            }
        }
        
        FileOutputStream os = new FileOutputStream(f);
        res.save(os);
        os.close();
    }

    private static void importImage(String path, EditableResources res) {
        try {
            BufferedImage bi = ImageIO.read(ResourceEditorApp.class.getResourceAsStream("/" + path));
            AddAndScaleMultiImage.generateMulti(4, bi, path, res);
        } catch(IOException err) {
            err.printStackTrace();
        }
    }
    
    private static String convertToVarName(String s) {
        StringBuilder sb = new StringBuilder();
        if(!Character.isJavaIdentifierStart(s.charAt(0))) {
            sb.append("_");
        }
        for (char c : s.toCharArray()) {
            if(!Character.isJavaIdentifierPart(c)) {
                sb.append("_");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
    
    private static final Collection<String> RESEVERVED_WORDS = Arrays.asList("abstract",
            "assert", "boolean",
            "break",
            "byte",
            "case",
            "catch",
            "char",
            "class",
            "const",
            "continue",
            "default",
            "do",
            "double",
            "else",
            "enum",
            "extends",
            "false",
            "final",
            "finally",
            "float",
            "for",
            "goto",
            "if",
            "implements",
            "import",
            "instanceof",
            "int",
            "interface",
            "long",
            "native",
            "new",
            "null",
            "package",
            "private",
            "protected",
            "public",
            "return",
            "short",
            "static",
            "strictfp",
            "super",
            "switch",
            "synchronized",
            "this",
            "throw",
            "throws",
            "transient",
            "true",
            "try",
            "void",
            "volatile",
            "while");
    
    private static void migrateGuiBuilder(File projectDir, EditableResources res, String destPackageName) 
        throws IOException {
        File propertiesFile = new File(projectDir, "codenameone_settings.properties");
        Properties props = new Properties();
        FileInputStream pIn = new FileInputStream(propertiesFile);
        props.load(pIn);
        pIn.close();
        
        if(props.getProperty("guiResource") == null) {
            System.out.println("Not a legacy GUI builder project!\nConversion failed!");
            System.exit(1);
            return;
        }
        
        UserInterfaceEditor.exportToNewGuiBuilderMode = true;
        
        String mainForm = props.getProperty("mainForm");
        
        File stateMachineBase = new File(projectDir, "src" + File.separatorChar + "generated" + File.separator + "StateMachineBase.java");
        StringBuilder stateMachineBaseSource = new StringBuilder("/**\n * This class was generated by the migration wizard, ultimately both it and the Statemachine can be removed.\n");
        stateMachineBaseSource.append(" * This class is no longer updated automatically\n");
        stateMachineBaseSource.append("*/\n");
        stateMachineBaseSource.append("package generated;\n");
        stateMachineBaseSource.append("\nimport com.codename1.ui.*;\n");
        stateMachineBaseSource.append("import com.codename1.ui.util.*;\n");
        stateMachineBaseSource.append("import com.codename1.ui.plaf.*;\n");
        stateMachineBaseSource.append("import java.util.Hashtable;\n");
        stateMachineBaseSource.append("import com.codename1.ui.events.*;\n\n");
        stateMachineBaseSource.append("public abstract class StateMachineBase extends UIBuilder {\n");
        stateMachineBaseSource.append("    private static final java.util.HashMap<String, Class> formNameToClassHashMap = new java.util.HashMap<String, Class>();");
        stateMachineBaseSource.append("    public static StateMachineBase instance;");
        stateMachineBaseSource.append("    protected void initVars() {}\n\n");
        stateMachineBaseSource.append("    protected void initVars(Resources res) {}\n\n");
        stateMachineBaseSource.append("    public StateMachineBase(Resources res, String resPath, boolean loadTheme) {\n    instance = this;\n");
        stateMachineBaseSource.append("        startApp(res, resPath, loadTheme);\n");
        stateMachineBaseSource.append("    }\n\n\n");
        stateMachineBaseSource.append("    public Container startApp(Resources res, String resPath, boolean loadTheme) {\n");
        stateMachineBaseSource.append("        initVars();\n");
        stateMachineBaseSource.append("        if(loadTheme) {\n");
        stateMachineBaseSource.append("            if(res == null) {\n");
        stateMachineBaseSource.append("                try {\n");
        stateMachineBaseSource.append("                    if(resPath.endsWith(\".res\")) {\n");
        stateMachineBaseSource.append("                        res = Resources.open(resPath);\n");
        stateMachineBaseSource.append("                        System.out.println(\"Warning: you should construct the state machine without the .res extension to allow theme overlays\");\n");
        stateMachineBaseSource.append("                    } else {\n");
        stateMachineBaseSource.append("                        res = Resources.openLayered(resPath);\n");
        stateMachineBaseSource.append("                    }\n");
        stateMachineBaseSource.append("                } catch(java.io.IOException err) { err.printStackTrace(); }\n");
        stateMachineBaseSource.append("            }\n");
        stateMachineBaseSource.append("            initTheme(res);\n");
        stateMachineBaseSource.append("        }\n");
        stateMachineBaseSource.append("        if(res != null) {\n");
        stateMachineBaseSource.append("            setResourceFilePath(resPath);\n");
        stateMachineBaseSource.append("            setResourceFile(res);\n");
        stateMachineBaseSource.append("            Resources.setGlobalResources(res);");
        stateMachineBaseSource.append("            initVars(res);\n");
        stateMachineBaseSource.append("            return showForm(getFirstFormName(), null);\n");
        stateMachineBaseSource.append("        } else {\n");
        stateMachineBaseSource.append("            Form f = (Form)createContainer(resPath, getFirstFormName());\n");
        stateMachineBaseSource.append("            Resources.setGlobalResources(fetchResourceFile());");
        stateMachineBaseSource.append("            initVars(fetchResourceFile());\n");
        stateMachineBaseSource.append("            beforeShow(f);\n");
        stateMachineBaseSource.append("            f.show();\n");
        stateMachineBaseSource.append("            postShow(f);\n");
        stateMachineBaseSource.append("            return f;\n");
        stateMachineBaseSource.append("        }\n");
        stateMachineBaseSource.append("    }\n\n\n");
        stateMachineBaseSource.append("    protected String getFirstFormName() {\n");
        stateMachineBaseSource.append("        return \"");
        stateMachineBaseSource.append(mainForm);
        stateMachineBaseSource.append("\";\n");
        stateMachineBaseSource.append("    }\n\n\n");
        stateMachineBaseSource.append("    protected void initTheme(Resources res) {\n");
        stateMachineBaseSource.append("            String[] themes = res.getThemeResourceNames();\n");
        stateMachineBaseSource.append("            Resources.setGlobalResources(res);\n");
        stateMachineBaseSource.append("            if(themes != null && themes.length > 0) {\n");
        stateMachineBaseSource.append("                UIManager.getInstance().setThemeProps(res.getTheme(themes[0]));\n");
        stateMachineBaseSource.append("            }\n");
        stateMachineBaseSource.append("    }\n\n\n");
        stateMachineBaseSource.append("    public StateMachineBase() {\n    instance = this;\n");
        stateMachineBaseSource.append("    }\n\n");
        stateMachineBaseSource.append("    public StateMachineBase(String resPath) {\n");
        stateMachineBaseSource.append("        this(null, resPath, true);\n    instance = this;\n");
        stateMachineBaseSource.append("    }\n\n");
        stateMachineBaseSource.append("    public StateMachineBase(Resources res) {\n");
        stateMachineBaseSource.append("        this(res, null, true);\n    instance = this;\n");
        stateMachineBaseSource.append("    }\n\n");
        stateMachineBaseSource.append("    public StateMachineBase(String resPath, boolean loadTheme) {\n");
        stateMachineBaseSource.append("        this(null, resPath, loadTheme);\n    instance = this;\n");
        stateMachineBaseSource.append("    }\n\n");
        stateMachineBaseSource.append("    public StateMachineBase(Resources res, boolean loadTheme) {\n");
        stateMachineBaseSource.append("        this(res, null, loadTheme);\n    instance = this;\n");
        stateMachineBaseSource.append("    }\n\n");
        stateMachineBaseSource.append("    public Form showForm(String resourceName, Command sourceCommand) {\n");
        stateMachineBaseSource.append("        try {\n");
        stateMachineBaseSource.append("            Form f = (Form)formNameToClassHashMap.get(resourceName).newInstance();\n");
        stateMachineBaseSource.append("            Form current = Display.getInstance().getCurrent();\n");
        stateMachineBaseSource.append("            if(current != null && isBackCommandEnabled() && allowBackTo(resourceName)) {\n");
        stateMachineBaseSource.append("                f.putClientProperty(\"previousForm\", current);\n");
        stateMachineBaseSource.append("                setBackCommand(f, new Command(getBackCommandText(current.getTitle())) {\n");
        stateMachineBaseSource.append("                    public void actionPerformed(ActionEvent evt) {\n");
        stateMachineBaseSource.append("                          back(null);\n");
        stateMachineBaseSource.append("                    }\n");
        stateMachineBaseSource.append("                });\n");
        stateMachineBaseSource.append("            }\n");
        stateMachineBaseSource.append("            if(sourceCommand != null && current != null && current.getBackCommand() == sourceCommand) {\n");
        stateMachineBaseSource.append("                f.showBack();\n");
        stateMachineBaseSource.append("            } else {\n");
        stateMachineBaseSource.append("                f.show();\n");
        stateMachineBaseSource.append("            }\n");
        stateMachineBaseSource.append("            return f;\n");
        stateMachineBaseSource.append("        } catch(Exception err) {\n");
        stateMachineBaseSource.append("            err.printStackTrace();\n");
        stateMachineBaseSource.append("            throw new RuntimeException(\"Form not found: \" + resourceName);\n");
        stateMachineBaseSource.append("        }\n");
        stateMachineBaseSource.append("    }\n\n");
        stateMachineBaseSource.append("    protected void beforeShow(Form f) {\n");
        stateMachineBaseSource.append("    }\n\n");
        stateMachineBaseSource.append("    public final void beforeShow__(Form f) {\n        beforeShow(f);\n");
        stateMachineBaseSource.append("        if(Display.getInstance().getCurrent() != null) {\n");
        stateMachineBaseSource.append("            exitForm(Display.getInstance().getCurrent());\n");
        stateMachineBaseSource.append("            invokeFormExit__(Display.getInstance().getCurrent());\n");
        stateMachineBaseSource.append("        }\n");
        stateMachineBaseSource.append("    }\n\n");
        stateMachineBaseSource.append("    protected void exitForm(Form f) {\n");
        stateMachineBaseSource.append("    }\n\n");
        stateMachineBaseSource.append("    protected void postShow(Form f) {\n");
        stateMachineBaseSource.append("    }\n\n");
        stateMachineBaseSource.append("    public final void postShow__(Form f) {\n        postShow(f);\n");
        stateMachineBaseSource.append("    }\n\n");
        stateMachineBaseSource.append("    private Container getRootComponent__(Component rootComponent) {\n");
        stateMachineBaseSource.append("        if(rootComponent.getParent() != null) {\n");
        stateMachineBaseSource.append("            return getRoot__(rootComponent.getParent());\n");
        stateMachineBaseSource.append("        }\n");
        stateMachineBaseSource.append("        return (Container)rootComponent;\n");
        stateMachineBaseSource.append("    }\n\n");
        stateMachineBaseSource.append("    private Container getRoot__(Container rootComponent) {\n");
        stateMachineBaseSource.append("        Container p = rootComponent.getParent();\n");
        stateMachineBaseSource.append("        while(p != null) {\n");
        stateMachineBaseSource.append("            rootComponent = p;\n");
        stateMachineBaseSource.append("            p = rootComponent.getParent();\n");
        stateMachineBaseSource.append("        }\n");
        stateMachineBaseSource.append("        return rootComponent;\n");
        stateMachineBaseSource.append("    }\n\n");
        stateMachineBaseSource.append("    public Component findByName(String componentName, Container rootComponent) {\n");
        stateMachineBaseSource.append("        Container root = getRoot__(rootComponent);\n");
        stateMachineBaseSource.append("        return findByName__(componentName, root);\n");
        stateMachineBaseSource.append("    }\n\n");
        stateMachineBaseSource.append("    public Component findByName__(String componentName, Container root) {\n");
        stateMachineBaseSource.append("        int count = root.getComponentCount();\n");
        stateMachineBaseSource.append("        for(int iter = 0 ; iter < count ; iter++) {\n");
        stateMachineBaseSource.append("            Component c = root.getComponentAt(iter);\n");
        stateMachineBaseSource.append("            String n = c.getName();\n");
        stateMachineBaseSource.append("            if(n != null && n.equals(componentName)) {\n");
        stateMachineBaseSource.append("                return c;\n");
        stateMachineBaseSource.append("            }\n");
        stateMachineBaseSource.append("            if(c instanceof Container && ((Container)c).getLeadComponent() == null) {\n");
        stateMachineBaseSource.append("                c = findByName__(componentName, (Container)c);\n");
        stateMachineBaseSource.append("                if(c != null) {\n");
        stateMachineBaseSource.append("                    return c;\n");
        stateMachineBaseSource.append("                }\n");
        stateMachineBaseSource.append("            }\n");
        stateMachineBaseSource.append("        }\n");
        stateMachineBaseSource.append("        return null;\n");
        stateMachineBaseSource.append("    }\n\n");
        stateMachineBaseSource.append("    protected void handleComponentAction(Component c, ActionEvent event) {\n");
        stateMachineBaseSource.append("    }\n\n");
        stateMachineBaseSource.append("    public void handleComponentAction__(Component c, ActionEvent event) {\n");
        stateMachineBaseSource.append("    }\n\n");
        stateMachineBaseSource.append("    public void processCommand__(ActionEvent ev, Command cmd) {\n");
        stateMachineBaseSource.append("        processCommand(ev, cmd);\n");
        stateMachineBaseSource.append("    }\n\n");
        stateMachineBaseSource.append("    public void back() {\n");
        stateMachineBaseSource.append("        back(null);\n");
        stateMachineBaseSource.append("    }\n\n");
        stateMachineBaseSource.append("    public void back(Component sourceComponent) {\n");
        stateMachineBaseSource.append("        Form current = (Form)Display.getInstance().getCurrent().getClientProperty(\"previousForm\");\n");
        stateMachineBaseSource.append("        current.showBack();\n");
        stateMachineBaseSource.append("    }\n\n");

        StringBuilder formNameMapBuilder = new StringBuilder("static {");
        StringBuilder invokeFormExitBuilder = new StringBuilder("    private void invokeFormExit__(Form f) {\n");
        
        UserInterfaceEditor.componentNames = new HashMap<String, Class>();
        UserInterfaceEditor.commandList = new ArrayList<ActionCommand>();

        for(String uiName : res.getUIResourceNames()) {
            System.out.println("Processing: " + uiName);
            String fileName = convertToVarName(uiName);
            formNameMapBuilder.append("    formNameToClassHashMap.put(\"");
            formNameMapBuilder.append(uiName);
            formNameMapBuilder.append("\", ");
            formNameMapBuilder.append(destPackageName);
            formNameMapBuilder.append(".");
            formNameMapBuilder.append(fileName);
            formNameMapBuilder.append(".class);\n");
            String normalizedUiName = ResourceEditorView.normalizeFormName(uiName);
            
            if(RESEVERVED_WORDS.contains(fileName)) {
                fileName += "X";
            } else {
                try {
                    if(Class.forName("java.lang." + fileName) != null) {
                        fileName += "X";
                    }
                } catch(Throwable t) {
                    // passed...
                }
            }
            File guiFile = new File(projectDir, "res" + File.separatorChar + "guibuilder" + File.separatorChar + 
                    destPackageName.replace('.', File.separatorChar) + File.separatorChar + fileName + ".gui");
            guiFile.getParentFile().mkdirs();
            File sourcePackageDir = new File(projectDir, "src" + File.separatorChar + destPackageName.replace('.', File.separatorChar) );
            sourcePackageDir.mkdirs();
            File sourceFile = new File(sourcePackageDir, fileName + ".java");
            UIBuilderOverride u = new UIBuilderOverride();
            com.codename1.ui.Container cnt = u.createContainer(res, uiName);
            FileOutputStream fos = new FileOutputStream(guiFile);
            Writer w = new OutputStreamWriter(fos, "UTF-8");
            w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n");
            StringBuilder bld = new StringBuilder();
            
            UserInterfaceEditor.actionEventNames = new ArrayList<String>();
            UserInterfaceEditor.listNames = new ArrayList<String>();
            UserInterfaceEditor.persistToXML(cnt, cnt, bld, res, "");
            w.write(bld.toString());
            w.flush();
            w.close();
            
            fos = new FileOutputStream(sourceFile);
            w = new OutputStreamWriter(fos, "UTF-8");
            w.write("package ");
            w.write(destPackageName);
            w.write(";\n");
            w.write("\n");
            w.write("/**\n");
            w.write(" * GUI builder created Form\n");
            w.write(" */\n");
            w.write("public class ");
            w.write(fileName);
            String prePostCode;
            w.write(" extends com.codename1.ui.");
            if(cnt instanceof com.codename1.ui.Form) {
                invokeFormExitBuilder.append("        if(f.getName().equals(\"");
                invokeFormExitBuilder.append(uiName);
                invokeFormExitBuilder.append("\")) {\n");
                invokeFormExitBuilder.append("            exit");
                invokeFormExitBuilder.append(normalizedUiName);
                invokeFormExitBuilder.append("(f);\n        }\n");

                stateMachineBaseSource.append("    protected void before");
                stateMachineBaseSource.append(normalizedUiName);
                stateMachineBaseSource.append("(Form f) {\n");
                stateMachineBaseSource.append("    }\n\n");
                stateMachineBaseSource.append("    public final void before");
                stateMachineBaseSource.append(normalizedUiName);
                stateMachineBaseSource.append("__(Form f) {\n        before");
                stateMachineBaseSource.append(normalizedUiName);
                stateMachineBaseSource.append("(f);\n    }\n\n");
                stateMachineBaseSource.append("    protected void post");
                stateMachineBaseSource.append(normalizedUiName);
                stateMachineBaseSource.append("(Form f) {\n");
                stateMachineBaseSource.append("    }\n\n");
                stateMachineBaseSource.append("    public final void post");
                stateMachineBaseSource.append(normalizedUiName);
                stateMachineBaseSource.append("__(Form f) {\n        post");
                stateMachineBaseSource.append(normalizedUiName);
                stateMachineBaseSource.append("(f);\n    }\n\n");                
                stateMachineBaseSource.append("    protected void exit");
                stateMachineBaseSource.append(normalizedUiName);
                stateMachineBaseSource.append("(Form f) {\n");
                stateMachineBaseSource.append("    }\n\n");
            }
            if(cnt instanceof com.codename1.ui.Dialog) {
                w.write("Dialog");
                prePostCode = "\n    public void initComponent() {\n        generated.StateMachineBase.instance.beforeShow__(this);\n";
                prePostCode += "        generated.StateMachineBase.instance.before";
                prePostCode += normalizedUiName;
                prePostCode += "__(this);\n    }\n";
                prePostCode = "\n    public void onShow() {\n        generated.StateMachineBase.instance.postShow__(this);\n";
                prePostCode += "        generated.StateMachineBase.instance.post";
                prePostCode += normalizedUiName;
                prePostCode += "__(this);\n    }\n";

                prePostCode += "    protected void actionCommand(com.codename1.ui.Command cmd) {\n";
                prePostCode += "        generated.StateMachineBase.instance.processCommand__(new com.codename1.ui.events.ActionEvent(cmd), cmd);\n";
                prePostCode += "    }\n\n";
            } else {
                if(cnt instanceof com.codename1.ui.Form) {
                    w.write("Form");
                    prePostCode = "\n    public void show() {\n        generated.StateMachineBase.instance.beforeShow__(this);\n";
                    prePostCode += "        generated.StateMachineBase.instance.before";
                    prePostCode += normalizedUiName;
                    prePostCode += "__(this);\n        super.show();\n        generated.StateMachineBase.instance.post";
                    prePostCode += normalizedUiName;
                    prePostCode += "__(this);\n    }\n";

                    prePostCode += "    protected void actionCommand(com.codename1.ui.Command cmd) {\n";
                    prePostCode += "        generated.StateMachineBase.instance.processCommand__(new com.codename1.ui.events.ActionEvent(cmd), cmd);\n";
                    prePostCode += "    }\n\n";
                } else {
                    w.write("Container");
                    prePostCode = "";
                }
            }
            w.write(" {\n    public ");
            w.write(fileName);
            w.write("() {\n");
            w.write("        this(com.codename1.ui.util.Resources.getGlobalResources());\n");
            w.write("    }\n    \n    public ");
            w.write(fileName);
            w.write("(com.codename1.ui.util.Resources resourceObjectInstance) {\n");
            w.write("        initGuiBuilderComponents(resourceObjectInstance);\n");
            w.write("    }\n\n");
            w.write("//-- DON'T EDIT BELOW THIS LINE!!!\n\n    private void initGuiBuilderComponents(com.codename1.ui.util.Resources resourceObjectInstance) {}\n\n");
            w.write("//-- DON'T EDIT ABOVE THIS LINE!!!\n");

            for(String actionListenerNames : UserInterfaceEditor.actionEventNames) {
                w.write("\n    public void on");
                w.write(actionListenerNames);
                w.write("ActionEvent(com.codename1.ui.events.ActionEvent ev) {\n        ");
                w.write("generated.StateMachineBase.instance.handleComponentAction__((com.codename1.ui.Component)ev.getSource(), ev);\n        ");
                w.write("generated.StateMachineBase.instance.on");
                w.write(normalizedUiName);
                w.write("_");
                String normalizedActionListenerName = ResourceEditorView.normalizeFormName(actionListenerNames);
                w.write(normalizedActionListenerName);
                w.write("Action__((com.codename1.ui.Component)ev.getSource(), ev);\n    }\n\n");
                stateMachineBaseSource.append("    protected void on");
                stateMachineBaseSource.append(normalizedUiName);
                stateMachineBaseSource.append("_");
                stateMachineBaseSource.append(normalizedActionListenerName);
                stateMachineBaseSource.append("Action(Component cmp, ActionEvent ev) {\n    }\n\n");
                stateMachineBaseSource.append("    public void on");
                stateMachineBaseSource.append(normalizedUiName);
                stateMachineBaseSource.append("_");
                stateMachineBaseSource.append(normalizedActionListenerName);
                stateMachineBaseSource.append("Action__(Component cmp, ActionEvent ev) {\n        on");
                stateMachineBaseSource.append(normalizedUiName);
                stateMachineBaseSource.append("_");
                stateMachineBaseSource.append(normalizedActionListenerName);
                stateMachineBaseSource.append("Action(cmp, ev);\n    }\n\n");
            }
                        
            w.write(prePostCode);
            w.write("}\n");
            
            w.flush();
            w.close();
        }
        
        formNameMapBuilder.append("}\n");
        invokeFormExitBuilder.append("}\n");
        
        stateMachineBaseSource.append(formNameMapBuilder);
        stateMachineBaseSource.append(invokeFormExitBuilder);

        ArrayList<String> uniqueNames = new ArrayList<String>();
        for(String cmpName : UserInterfaceEditor.componentNames.keySet()) {            
            String nomName = ResourceEditorView.normalizeFormName(cmpName);
            if(uniqueNames.contains(nomName)) {
                continue;
            }
            uniqueNames.add(nomName);
            stateMachineBaseSource.append("    public ");
            stateMachineBaseSource.append(UserInterfaceEditor.componentNames.get(cmpName).getName());
            stateMachineBaseSource.append(" find");
            stateMachineBaseSource.append(nomName);
            stateMachineBaseSource.append("(Component root) {\n        return (");
            stateMachineBaseSource.append(UserInterfaceEditor.componentNames.get(cmpName).getName());
            stateMachineBaseSource.append(")findByName(\"");
            stateMachineBaseSource.append(cmpName);
            stateMachineBaseSource.append("\", getRootComponent__(root));\n    }\n\n");            
            stateMachineBaseSource.append("    public ");
            stateMachineBaseSource.append(UserInterfaceEditor.componentNames.get(cmpName).getName());
            stateMachineBaseSource.append(" find");
            stateMachineBaseSource.append(nomName);
            stateMachineBaseSource.append("() {\n        return (");
            stateMachineBaseSource.append(UserInterfaceEditor.componentNames.get(cmpName).getName());
            stateMachineBaseSource.append(")findByName(\"");
            stateMachineBaseSource.append(cmpName);
            stateMachineBaseSource.append("\", Display.getInstance().getCurrent());\n    }\n\n");            
        }

        ArrayList<Integer> commandIdsAdded = new ArrayList<Integer>();
        ArrayList<String> commandNamesAdded = new ArrayList<String>();
        for(ActionCommand cmd : UserInterfaceEditor.commandList) {
            String formName = (String)cmd.getClientProperty("FORMNAME");
            if(formName == null) {
                continue;
            }
            String normalizedCommandName = ResourceEditorView.normalizeFormName(formName) +
                    ResourceEditorView.normalizeFormName(cmd.getCommandName());
            if(commandNamesAdded.contains(normalizedCommandName)) {
                continue;
            }
            if(commandIdsAdded.contains(cmd.getId())) {
                continue;
            }
            commandIdsAdded.add(cmd.getId());
            commandNamesAdded.add(normalizedCommandName);
            stateMachineBaseSource.append("    public static final int COMMAND_");
            stateMachineBaseSource.append(normalizedCommandName);
            stateMachineBaseSource.append(" = ");
            stateMachineBaseSource.append(cmd.getId());
            stateMachineBaseSource.append(";\n\n    protected boolean on");
            stateMachineBaseSource.append(normalizedCommandName);
            stateMachineBaseSource.append("() {\n        return false;\n    }\n\n");
        }

        stateMachineBaseSource.append("    protected void processCommand(ActionEvent ev, Command cmd) {\n");
        stateMachineBaseSource.append("        switch(cmd.getId()) {\n");
        
        commandIdsAdded.clear();
        commandNamesAdded.clear();

        for(ActionCommand cmd : UserInterfaceEditor.commandList) {
            String formName = (String)cmd.getClientProperty("FORMNAME");
            if(formName == null) {
                continue;
            }
            String normalizedCommandName = ResourceEditorView.normalizeFormName(formName) +
                    ResourceEditorView.normalizeFormName(cmd.getCommandName());
            if(commandNamesAdded.contains(normalizedCommandName)) {
                continue;
            }
            if(commandIdsAdded.contains(cmd.getId())) {
                continue;
            }
            commandIdsAdded.add(cmd.getId());
            commandNamesAdded.add(normalizedCommandName);
            stateMachineBaseSource.append("\n        case COMMAND_");
            stateMachineBaseSource.append(normalizedCommandName);
            stateMachineBaseSource.append(":\n");
            
            if(cmd.getAction() != null && cmd.getAction().length() > 0) {
                if(!cmd.getAction().startsWith("$")) {
                    stateMachineBaseSource.append("            showForm(\"");                    
                    stateMachineBaseSource.append(cmd.getAction());                    
                    stateMachineBaseSource.append("\", null);\n");                    
                }
            }
            
            stateMachineBaseSource.append("            if(on");
            stateMachineBaseSource.append(normalizedCommandName);
            stateMachineBaseSource.append("()) {\n");
            stateMachineBaseSource.append("                ev.consume();\n");
            stateMachineBaseSource.append("                return;\n");
            stateMachineBaseSource.append("            }\n");
            stateMachineBaseSource.append("            break;\n\n");
        }
        stateMachineBaseSource.append("        }\n");
        stateMachineBaseSource.append("        if(ev.getComponent() != null) {\n");
        stateMachineBaseSource.append("            handleComponentAction(ev.getComponent(), ev);\n");
        stateMachineBaseSource.append("        }\n");
        stateMachineBaseSource.append("    }\n\n");
        
        stateMachineBaseSource.append("\n}\n");
        
        
        FileOutputStream sbout = new FileOutputStream(stateMachineBase);
        sbout.write(stateMachineBaseSource.toString().getBytes("UTF-8"));
        sbout.close();
        
        props.remove("mainForm");
        props.remove("package");
        props.remove("guiResource");
        props.remove("baseClass");
        props.remove("userClass");
        
        
        FileOutputStream pOut = new FileOutputStream(propertiesFile);
        props.store(pOut, "Updated by GUI builder migration wizard");
        pOut.close();
        System.out.println("Conversion completed successfully!");
        System.exit(0);
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
