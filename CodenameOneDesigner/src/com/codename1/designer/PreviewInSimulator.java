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
package com.codename1.designer;

import com.codename1.impl.javase.Simulator;
import com.codename1.ui.util.Resources;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.JComponent;
import javax.swing.JOptionPane;

/**
 *
 * @author Shai Almog
 */
public class PreviewInSimulator {
    public static void execute(final JComponent parent, final String theme, final File resource, final String selection) {
        new Thread() {
            public void run() {                
                Preferences pref = Preferences.userNodeForPackage(PreviewInSimulator.class);
                if(theme == null) {
                    pref.put("previewTheme", "");
                } else {
                    pref.put("previewTheme", theme);
                }
                pref.put("previewResource", resource.getAbsolutePath());
                pref.put("previewSelection", selection);
                try {
                    pref.sync();
                } catch (BackingStoreException ex) {
                    ex.printStackTrace();
                }
                String javaHome = System.getProperty("java.home");
                String os = System.getProperty("os.name");
                String javaBin = javaHome + File.separator + "bin" + File.separator + "java";

                if (os.toLowerCase().contains("win")) {
                    javaBin += "w.exe";
                }
                ProcessBuilder b = new ProcessBuilder(javaBin, "-classpath", System.getProperty("java.class.path"), 
                        Simulator.class.getName(), PreviewInSimulator.class.getName());
                try {
                    executeProcess(b, -1);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(parent, "Error executing simulator: " + ex, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.start();
    }
    
    private static void executeProcess(ProcessBuilder pb, final int timeout) throws Exception {
        pb.redirectErrorStream(true);
        final Process p = pb.start();
        final boolean[] destroyed = new boolean[] {false};
        final InputStream stream = p.getInputStream();
        final boolean[] running = new boolean[] {true};
        new Thread() {
            public void run() {
                try {
                    byte[] buffer = new byte[8192];
                    int i = stream.read(buffer);
                    while (i > -1) {
                        String str = new String(buffer, 0, i);
                        System.out.print(str);
                        i = stream.read(buffer);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }.start();
        if(timeout > -1) {
            new Thread() {
                public void run() {
                    long t = System.currentTimeMillis();
                    while(running[0]) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ex) {
                        }
                        if(System.currentTimeMillis() - t > timeout) {
                            destroyed[0] = true;
                            p.destroy();
                        }
                    }
                }
            }.start();
        }
        int val = p.waitFor();
        if(destroyed[0]) {
            return;
        }
        running[0] = false;
    }

    /**
     * Called back from simulateDeviceActionPerformed to show the simulator skin
     */
    public static void main(String[] argv) {
        com.codename1.ui.Display.init(new Runnable() {
            public void run() {
                try {
                    Preferences pref = Preferences.userNodeForPackage(PreviewInSimulator.class);
                    String theme = pref.get("previewTheme", null);
                    File resFile = new File(pref.get("previewResource", null));
                    String selection = pref.get("previewSelection", null);
                    Resources res = Resources.open(new FileInputStream(resFile));
                    if(theme == null || theme.length() == 0) {
                        if(com.codename1.ui.Display.getInstance().hasNativeTheme()) {
                            com.codename1.ui.Display.getInstance().installNativeTheme();
                        }
                    } else {
                        com.codename1.ui.plaf.UIManager.getInstance().setThemeProps(res.getTheme(theme));
                    }
                    com.codename1.ui.util.UIBuilder.registerCustomComponent("Table", com.codename1.ui.table.Table.class);
                    com.codename1.ui.util.UIBuilder.registerCustomComponent("MediaPlayer", com.codename1.components.MediaPlayer.class);
                    com.codename1.ui.util.UIBuilder.registerCustomComponent("ContainerList", com.codename1.ui.list.ContainerList.class);
                    com.codename1.ui.util.UIBuilder.registerCustomComponent("ComponentGroup", com.codename1.ui.ComponentGroup.class);
                    com.codename1.ui.util.UIBuilder.registerCustomComponent("Tree", com.codename1.ui.tree.Tree.class);
                    com.codename1.ui.util.UIBuilder.registerCustomComponent("HTMLComponent", com.codename1.ui.html.HTMLComponent.class);
                    com.codename1.ui.util.UIBuilder.registerCustomComponent("RSSReader", com.codename1.components.RSSReader.class);
                    com.codename1.ui.util.UIBuilder.registerCustomComponent("FileTree", com.codename1.components.FileTree.class);
                    com.codename1.ui.util.UIBuilder.registerCustomComponent("WebBrowser", com.codename1.components.WebBrowser.class);
                    com.codename1.ui.util.UIBuilder.registerCustomComponent("NumericSpinner", com.codename1.ui.spinner.NumericSpinner.class);
                    com.codename1.ui.util.UIBuilder.registerCustomComponent("DateSpinner", com.codename1.ui.spinner.DateSpinner.class);
                    com.codename1.ui.util.UIBuilder.registerCustomComponent("TimeSpinner", com.codename1.ui.spinner.TimeSpinner.class);
                    com.codename1.ui.util.UIBuilder.registerCustomComponent("DateTimeSpinner", com.codename1.ui.spinner.DateTimeSpinner.class);
                    com.codename1.ui.util.UIBuilder.registerCustomComponent("GenericSpinner", com.codename1.ui.spinner.GenericSpinner.class);
                    com.codename1.ui.util.UIBuilder.registerCustomComponent("LikeButton", com.codename1.facebook.ui.LikeButton.class);
                    com.codename1.ui.util.UIBuilder.registerCustomComponent("InfiniteProgress", com.codename1.components.InfiniteProgress.class);
                    com.codename1.ui.util.UIBuilder.registerCustomComponent("MultiButton", com.codename1.components.MultiButton.class);
                    com.codename1.ui.util.UIBuilder.registerCustomComponent("SpanButton", com.codename1.components.SpanButton.class);
                    com.codename1.ui.util.UIBuilder.registerCustomComponent("Ads", com.codename1.components.Ads.class);
                    com.codename1.ui.util.UIBuilder.registerCustomComponent("MapComponent", com.codename1.maps.MapComponent.class);
                    com.codename1.ui.util.UIBuilder.registerCustomComponent("MultiList", com.codename1.ui.list.MultiList.class);
                    com.codename1.ui.util.UIBuilder.registerCustomComponent("ShareButton", com.codename1.components.ShareButton.class);
                    com.codename1.ui.util.UIBuilder.registerCustomComponent("OnOffSwitch", com.codename1.components.OnOffSwitch.class);
                    com.codename1.ui.util.UIBuilder.registerCustomComponent("ImageViewer", com.codename1.components.ImageViewer.class);
                    com.codename1.ui.util.UIBuilder builder = new com.codename1.ui.util.UIBuilder();
                    com.codename1.ui.Container c = builder.createContainer(res, selection);
                    if(c instanceof com.codename1.ui.Form) {
                        ((com.codename1.ui.Form)c).refreshTheme();
                        if(c instanceof com.codename1.ui.Dialog) {
                            ((com.codename1.ui.Dialog)c).showModeless();
                        } else {
                            ((com.codename1.ui.Form)c).show();
                        }
                    } else {
                        com.codename1.ui.Form f = new com.codename1.ui.Form();
                        f.setLayout(new com.codename1.ui.layouts.BorderLayout());
                        f.addComponent(com.codename1.ui.layouts.BorderLayout.CENTER, c);
                        f.refreshTheme();
                        f.show();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Error While Running In Simulator: " + ex, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }    
}
