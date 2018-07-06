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
package com.codename1.impl.javase;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.payment.PurchaseCallback;
import com.codename1.push.PushCallback;
import com.codename1.push.PushContent;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.prefs.Preferences;
import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 * @author Shai Almog
 */
public class Executor {
    
    private static Class c;
    private static Object app;
    
    
    
    public static void main(final String[] argv) throws Exception {
        
        setProxySettings();
        if (CSSWatcher.isSupported()) {
            CSSWatcher cssWatcher = new CSSWatcher();
            cssWatcher.start();
        }
        final Properties p = new Properties();
        String currentDir = System.getProperty("user.dir");
        File props = new File(currentDir, "codenameone_settings.properties");
        if(props.exists()) {
            FileInputStream f = null;
            try {
                f = new FileInputStream(props);
                p.load(f);
                f.close();
            } catch (Exception ex) {
            } finally {
                try {
                    f.close();
                } catch (IOException ex) {
                }
            }
        }

            SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    String packageName = p.getProperty("codename1.packageName");
                    String mainName = p.getProperty("codename1.mainName");
                    if(argv.length > 1) {
                        if(argv[1].equalsIgnoreCase("-force") || packageName == null) {
                            c = Class.forName(argv[0]);
                        } else {
                            c = Class.forName(packageName + "." + mainName);
                        }
                    } else {
                        if(packageName == null || System.getenv("FORCE_CLASS") != null) {
                            c = Class.forName(argv[0]);
                        } else {
                            c = Class.forName(packageName + "." + mainName);
                        }
                    }
                    try {
                        Method m = c.getDeclaredMethod("main", String[].class);
                        m.invoke(null, new Object[]{null});
                    } catch (NoSuchMethodException noMain) {
                        try {
                            Method m = c.getDeclaredMethod("startApp");
                            m.invoke(c.newInstance());
                        } catch (NoSuchMethodException noStartApp) {
                            if (Display.isInitialized()) {
                                Display.deinitialize();
                            }
                            final Method m = c.getDeclaredMethod("init", Object.class);
                            if(m.getExceptionTypes() != null && m.getExceptionTypes().length > 0) {
                                System.err.println("ERROR: the init method can't declare a throws clause");
                                System.exit(1);
                            }
                            app = c.newInstance();
                            if(app instanceof PushCallback) {
                                CodenameOneImplementation.setPushCallback((PushCallback)app);
                            }
                            if(app instanceof PurchaseCallback) {
                                CodenameOneImplementation.setPurchaseCallback((PurchaseCallback)app);
                            }
                            Display.init(null);
                            Display.getInstance().callSerially(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        m.invoke(app, new Object[]{null});
                                        Method start = c.getDeclaredMethod("start", new Class[0]);
                                        if(start.getExceptionTypes() != null && start.getExceptionTypes().length > 0) {
                                            System.err.println("ERROR: the start method can't declare a throws clause");
                                            System.exit(1);
                                        }
                                        start.invoke(app, new Object[0]);
                                    } catch (NoSuchMethodException err) {
                                        System.out.println("Couldn't find a main or a startup in " + argv[0]);
                                    } catch (InvocationTargetException err) {
                                        err.getTargetException().printStackTrace();
                                        System.exit(1);
                                    } catch (Exception err) {
                                        err.printStackTrace();
                                        System.exit(1);
                                    }
                                }
                            });
                        }
                    }
                } catch(Exception err) {
                    err.printStackTrace();
                    System.exit(1);
                }
            }
        });
    }

    public static void stopApp(){
        if(c != null && app != null){
            try {
                Method stop = c.getDeclaredMethod("stop", new Class[0]);
                if(stop.getExceptionTypes() != null && stop.getExceptionTypes().length > 0) {
                    System.err.println("ERROR: the stop method can't declare a throws clause");
                    System.exit(1);
                }
                stop.invoke(app, new Object[0]);
            } catch (Exception ex) {
                ex.printStackTrace();
            } 
        }
    }

    public static void destroyApp(){
        if(c != null && app != null){
            try {
                Method stop = c.getDeclaredMethod("destroy", new Class[0]);
                stop.invoke(app, new Object[0]);
            } catch (Exception ex) {
                ex.printStackTrace();
            } 
        }
    }
    
    public static void startApp(){
        if(c != null && app != null){
            try {
                Method start = c.getDeclaredMethod("start", new Class[0]);
                if(start.getExceptionTypes() != null && start.getExceptionTypes().length > 0) {
                    System.err.println("ERROR: the start method can't declare a throws clause");
                    System.exit(1);
                }
                start.invoke(app, new Object[0]);
            } catch (Exception ex) {
                ex.printStackTrace();
            } 
        }
    
    }

    public static void registerForPush(final String key){
        if(c != null && app != null){
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    try {
                        Method registeredForPush = c.getDeclaredMethod("registeredForPush", String.class);
                        registeredForPush.invoke(app, key);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    } 
                }
            });
        }
    
    }

    public static void pushRegistrationError(final String message, final int code){
        if(c != null && app != null){
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    try {
                        Method pushRegistrationError = c.getDeclaredMethod("pushRegistrationError", String.class, Integer.class);
                        pushRegistrationError.invoke(app, message, code);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    } 
                }               
            });
        }
    }
    


    public static void push(final String message, final int type){
        if(c != null && app != null){
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    try {
                        String messageBody = message;
                        Element messageBodyEl = null;
                        String messageType = "" + type;
                        byte messageTypeByte = (byte) type;
                        if (type == 99) {
                            
                            try {
                                DocumentBuilderFactory factory
                                        = DocumentBuilderFactory.newInstance();
                                DocumentBuilder builder = factory.newDocumentBuilder();
                                if (!messageBody.startsWith("<?xml")) {
                                    messageBody = "<?xml version=\"1.0\"?>" + messageBody;
                                }
                                ByteArrayInputStream input = new ByteArrayInputStream(
                                        messageBody.getBytes("UTF-8"));
                                Document doc = builder.parse(input);
                                Element root = doc.getDocumentElement();
                                messageType = "1";
                                messageTypeByte = 1;
                                if (root.hasAttribute("type")) {
                                    messageTypeByte = (byte) Integer.parseInt(root.getAttribute("type"));
                                    messageType = "" + messageTypeByte;
                                }
                                messageBody = "";
                                if (root.hasAttribute("body")) {
                                    messageBody = root.getAttribute("body");
                                }
                                messageBodyEl = root;
                            } catch (Exception x) {
                                System.err.println("Failed to parse XML messagse body");
                                x.printStackTrace();
                                return;

                            }

                        }

                        Method push = c.getDeclaredMethod("push", String.class);
                        String[] actionIds = null;
                        String[] actionLabels = null;
                        java.awt.Image image;
                        javax.swing.ImageIcon imageIcon = null;
                        PushContent.reset();
                        if (messageBodyEl != null) {
                            // This was an XML request
                            NodeList images = messageBodyEl.getElementsByTagName("img");
                            if (images.getLength() > 0) {
                                Element img = (Element) images.item(0);

                                PushContent.setImageUrl(img.getAttribute("src"));
                                if (!img.getAttribute("src").startsWith("https://")) {
                                    System.err.println("Push message includes image attachment at non-secure URL.  Image will not be displayed on iOS.  Make sure all image attachments use https://");
                                }
                                image = javax.imageio.ImageIO.read(new java.net.URL(img.getAttribute("src")));
                                imageIcon = new javax.swing.ImageIcon(fitImage(image, 512, 512));
                                JavaSEPort.instance.checkRichPushBuildHints();
                            }
                            if (messageBodyEl.hasAttribute("category")) {
                                PushContent.setCategory(messageBodyEl.getAttribute("category"));
                                JavaSEPort.instance.checkRichPushBuildHints();
                                try {
                                    Method getPushActionCategories = c.getDeclaredMethod("getPushActionCategories", new Class[0]);
                                    Class pushActionCategoryCls = c.getClassLoader().loadClass("com.codename1.push.PushActionCategory");
                                    Method getCategoryId = pushActionCategoryCls.getDeclaredMethod("getId", new Class[0]);
                                    Object foundCategory = null;
                                    if (getPushActionCategories != null) {
                                        Object[] categories = (Object[])getPushActionCategories.invoke(app, new Object[0]);
                                        if (categories != null) {
                                            for (Object category : categories) {
                                                if (messageBodyEl.getAttribute("category").equals(getCategoryId.invoke(category, new Object[0]))) {
                                                    foundCategory = category;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                    if (foundCategory != null) {
                                        Method getActions = pushActionCategoryCls.getDeclaredMethod("getActions", new Class[0]);
                                        Object[] actions = (Object[])getActions.invoke(foundCategory, new Object[0]);
                                        actionIds = new String[actions.length];
                                        actionLabels = new String[actions.length];
                                        Class pushActionCls = c.getClassLoader().loadClass("com.codename1.push.PushAction");
                                        Method getActionId = pushActionCls.getDeclaredMethod("getId", new Class[0]);
                                        Method getActionTitle = pushActionCls.getDeclaredMethod("getTitle", new Class[0]);
                                        Method getActionIcon = pushActionCls.getDeclaredMethod("getIcon", new Class[0]);
                                        for (int i=0; i<actions.length; i++) {
                                            actionIds[i] = (String)getActionId.invoke(actions[i], new Object[0]);
                                            actionLabels[i] = (String)getActionTitle.invoke(actions[i], new Object[0]);
                                        }
                                    }
                                    
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                                
                                
                            }

                        }
                        
                        
                        

                        String[] parts = null;
                        int result = 0;
                        Display.getInstance().setProperty("pushType", ""+messageTypeByte);
                        switch (messageTypeByte) {
                            case 0:
                            case 1:
                                PushContent.setBody(messageBody);
                                PushContent.setType(1);
                                
                                if (Display.getInstance().isMinimized()) {
                                    
                                    if (actionIds != null) {
                                        result = javax.swing.JOptionPane.showOptionDialog(null, messageBody, messageBody, 0, javax.swing.JOptionPane.INFORMATION_MESSAGE, imageIcon, actionLabels, null);
                                        if (result >= 0) {
                                            PushContent.setActionId(actionIds[result]);
                                        }
                                    } else {
                                        result = javax.swing.JOptionPane.showOptionDialog(null, messageBody, messageBody, 0, javax.swing.JOptionPane.INFORMATION_MESSAGE, imageIcon, new String[]{"OK"}, null);
                                        
                                    }
                                    if (result >= 0) {
                                        JavaSEPort.resumeApp();
                                    }
                                }
                                if (result >= 0) {
                                    push.invoke(app, messageBody);
                                }
                                
                                break;
                            case 2:
                                PushContent.setMetaData(messageBody);
                                PushContent.setType(2);
                                push.invoke(app, messageBody);
                                break;
                            case 3:
                                parts = messageBody.split(";");
                                PushContent.setMetaData(parts[1]);
                                PushContent.setBody(parts[0]);
                                PushContent.setType(3);
                                if (Display.getInstance().isMinimized()) {
                                    
                                    if (actionIds != null) {
                                        result = javax.swing.JOptionPane.showOptionDialog(null, parts[0], parts[0], 0, javax.swing.JOptionPane.INFORMATION_MESSAGE, imageIcon, actionLabels, null);
                                        if (result >= 0) {
                                            PushContent.setActionId(actionIds[result]);
                                        }
                                    } else {
                                        result = javax.swing.JOptionPane.showOptionDialog(null, parts[0], parts[0], 0, javax.swing.JOptionPane.INFORMATION_MESSAGE, imageIcon, new String[]{"OK"}, null);
                                        
                                    }
                                    if (result >= 0) {
                                        JavaSEPort.resumeApp();
                                    }
                                }
                                if (result >= 0) {
                                    Display.getInstance().setProperty("pushType", "1");
                                    push.invoke(app, parts[0]);
                                    Display.getInstance().setProperty("pushType", "2");
                                    push.invoke(app, parts[1]);
                                }
                                break;
                            case 4:
                                parts = messageBody.split(";");
                                PushContent.setTitle(parts[0]);
                                PushContent.setBody(parts[1]);
                                PushContent.setType(4);
                                if (Display.getInstance().isMinimized()) {
                                    
                                    if (actionIds != null) {
                                        result = javax.swing.JOptionPane.showOptionDialog(null, parts[1], parts[0], 0, javax.swing.JOptionPane.INFORMATION_MESSAGE, imageIcon, actionLabels, null);
                                        if (result >= 0) {
                                            PushContent.setActionId(actionIds[result]);
                                        }
                                    } else {
                                        result = javax.swing.JOptionPane.showOptionDialog(null, parts[1], parts[0], 0, javax.swing.JOptionPane.INFORMATION_MESSAGE, imageIcon, new String[]{"OK"}, null);
                                        
                                    }
                                    if (result >= 0) {
                                        JavaSEPort.resumeApp();
                                    }
                                }
                                if (result >= 0) {
                                    Display.getInstance().setProperty("pushType", "4");
                                    push.invoke(app, parts[0]+";"+parts[1]);
                                }
                                break;
                            case 5:
                                PushContent.setBody(messageBody);
                                PushContent.setType(1);
                                if (Display.getInstance().isMinimized()) {
                                    
                                    if (actionIds != null) {
                                        result = javax.swing.JOptionPane.showOptionDialog(null, messageBody, messageBody, 0, javax.swing.JOptionPane.INFORMATION_MESSAGE, imageIcon, actionLabels, null);
                                        if (result >= 0) {
                                            PushContent.setActionId(actionIds[result]);
                                        }
                                    } else {
                                        result = javax.swing.JOptionPane.showOptionDialog(null, messageBody, messageBody, 0, javax.swing.JOptionPane.INFORMATION_MESSAGE, imageIcon, new String[]{"OK"}, null);
                                        
                                    }
                                    if (result >= 0) {
                                        JavaSEPort.resumeApp();
                                    }
                                }
                                if (result >= 0) {
                                    Display.getInstance().setProperty("pushType", "1");
                                    push.invoke(app, messageBody);
                                }
                                break;
                            case 101:
                                PushContent.setBody(messageBody.substring(messageBody.indexOf(" ") + 1));
                                PushContent.setType(1);
                                if (Display.getInstance().isMinimized()) {
                                    
                                    if (actionIds != null) {
                                        result = javax.swing.JOptionPane.showOptionDialog(null, messageBody.substring(messageBody.indexOf(" ") + 1), messageBody.substring(messageBody.indexOf(" ") + 1), 0, javax.swing.JOptionPane.INFORMATION_MESSAGE, imageIcon, actionLabels, null);
                                        if (result >= 0) {
                                            PushContent.setActionId(actionIds[result]);
                                        }
                                    } else {
                                        result = javax.swing.JOptionPane.showOptionDialog(null, messageBody.substring(messageBody.indexOf(" ") + 1), messageBody.substring(messageBody.indexOf(" ") + 1), 0, javax.swing.JOptionPane.INFORMATION_MESSAGE, imageIcon, new String[]{"OK"}, null);
                                        
                                    }
                                    if (result >= 0) {
                                        JavaSEPort.resumeApp();
                                    }
                                }
                                if (result >= 0) {
                                    push.invoke(app, messageBody.substring(messageBody.indexOf(" ") + 1));
                                }
                                break;
                            case 102:
                                parts = messageBody.split(";");
                                PushContent.setTitle(parts[1]);
                                PushContent.setBody(parts[2]);
                                PushContent.setType(2);
                                
                                if (Display.getInstance().isMinimized()) {
                                    
                                    if (actionIds != null) {
                                        result = javax.swing.JOptionPane.showOptionDialog(null, parts[2], parts[1], 0, javax.swing.JOptionPane.INFORMATION_MESSAGE, imageIcon, actionLabels, null);
                                        if (result >= 0) {
                                            PushContent.setActionId(actionIds[result]);
                                        }
                                    } else {
                                        result = javax.swing.JOptionPane.showOptionDialog(null, parts[2], parts[1], 0, javax.swing.JOptionPane.INFORMATION_MESSAGE, imageIcon, new String[]{"OK"}, null);
                                        
                                    }
                                    if (result >= 0) {
                                        JavaSEPort.resumeApp();
                                    }
                                }
                                if (result >= 0) {
                                    push.invoke(app, parts[1] + ";" + parts[2]);
                                }
                                break;
                            default:
                                throw new RuntimeException("Unsupported push type: " + messageTypeByte);

                        }

                        //push.invoke(app, message);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
        }
    }

    private static void setProxySettings() {
        Preferences proxyPref = Preferences.userNodeForPackage(Component.class);
        int proxySel = proxyPref.getInt("proxySel", 2);
        String proxySelHttp = proxyPref.get("proxySel-http", "");
        String proxySelPort = proxyPref.get("proxySel-port", "");

        switch (proxySel) {
            case 1:
                System.getProperties().remove("java.net.useSystemProxies");
                System.getProperties().remove("http.proxyHost");
                System.getProperties().remove("http.proxyPort");
                System.getProperties().remove("https.proxyHost");
                System.getProperties().remove("https.proxyPort");
                break;
            case 2:
                System.setProperty("java.net.useSystemProxies", "true");
                System.getProperties().remove("http.proxyHost");
                System.getProperties().remove("http.proxyPort");
                System.getProperties().remove("https.proxyHost");
                System.getProperties().remove("https.proxyPort");
                break;
            case 3:
                System.setProperty("http.proxyHost", proxySelHttp);
                System.setProperty("http.proxyPort", proxySelPort);
                System.setProperty("https.proxyHost", proxySelHttp);
                System.setProperty("https.proxyPort", proxySelPort);
                break;
        }
    }
    
    static Object getApp() {
        return app;
    }
    
    private static java.awt.Image fitImage(java.awt.Image img, int w, int h) {
        java.awt.image.BufferedImage resizedimage = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2 = resizedimage.createGraphics();
        g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(img, 0, 0, w, h, null);
        g2.dispose();
        return resizedimage;
    }
    
}
