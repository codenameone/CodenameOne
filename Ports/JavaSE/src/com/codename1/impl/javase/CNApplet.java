/*
 * Copyright (c) 2022, Codename One and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import javax.swing.JPanel;

public class CNApplet extends JPanel {
    private Class lifecycleClass;
    private final ClassPathLoader classPathLoader;
    private final Thread panelThread;
    
    
   
    public CNApplet(final File jarFile, final String mainClass, final File appHomeDir) throws ClassNotFoundException, IOException {
        JarFile jar = new JarFile(jarFile);
        //final String mainClass = jar.getManifest().getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
        String dependencies = jar.getManifest().getMainAttributes().getValue(Attributes.Name.CLASS_PATH);
        List<File> classPath = new ArrayList<File>();
        classPath.add(jarFile);
        for (String dir : dependencies.split(" ")) {
            classPath.add(new File(jarFile.getAbsoluteFile().getParentFile(), dir));
        }
        
        classPathLoader = new ClassPathLoader(Thread.currentThread().getContextClassLoader(),  classPath.toArray(new File[classPath.size()]));
        panelThread = new Thread(new Runnable() {
            public void run() {
                try {
                    lifecycleClass = classPathLoader.loadClass(mainClass);
                    Class cnPanelUtil = classPathLoader.loadClass(CNPanelUtil.class.getName());
                    Method initializeCN1 = cnPanelUtil.getMethod("initializeCN1", java.awt.Container.class, java.lang.Object.class, java.io.File.class);
                    Object lifecycleObject = lifecycleClass.newInstance();
                    initializeCN1.invoke(null, CNApplet.this, lifecycleObject, appHomeDir);
                    
                } catch (Exception ex) {
                    throw new RuntimeException("Failed to load CNPanelUtil class: "+CNPanelUtil.class.getName(), ex);
                }
                
            }
        });
        panelThread.setContextClassLoader(classPathLoader);
        panelThread.start();

    }
    
}
