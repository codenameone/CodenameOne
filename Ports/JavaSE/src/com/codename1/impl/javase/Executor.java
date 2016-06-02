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
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.prefs.Preferences;
import javax.swing.SwingUtilities;

/**
 *
 * @author Shai Almog
 */
public class Executor {
    
    private static Class c;
    private static Object app;
    
    public static void main(final String[] argv) throws Exception {
        
        setProxySettings();
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    c = Class.forName(argv[0]);
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
                                        String currentDir = System.getProperty("user.dir");
                                        File props = new File(currentDir, "codenameone_settings.properties");
                                        if(props.exists()) {
                                            FileInputStream f = null;
                                            try {
                                                Properties p = new Properties();
                                                f = new FileInputStream(props);
                                                p.load(f);
                                                f.close();
                                                String zone = p.getProperty("codename1.arg.vserv.zone", null);
                                                if(zone != null && zone.length() > 0) {
                                                    com.codename1.impl.VServAds v = new com.codename1.impl.VServAds();
                                                    v.showWelcomeAd(); 
                                                    v.bindTransitionAd(Integer.parseInt(p.getProperty("codename1.arg.vserv.transition", "300000")));
                                                }
                                            } catch (Exception ex) {
                                            } finally {
                                                try {
                                                    f.close();
                                                } catch (IOException ex) {
                                                }
                                            }
                                        }
                                        Method start = c.getDeclaredMethod("start", new Class[0]);
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
                start.invoke(app, new Object[0]);
            } catch (Exception ex) {
                ex.printStackTrace();
            } 
        }
    
    }
    
    private static void setProxySettings() {
        Preferences proxyPref = Preferences.userNodeForPackage(Component.class);
        int proxySel = proxyPref.getInt("proxySel", 1);
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
    
    
}
