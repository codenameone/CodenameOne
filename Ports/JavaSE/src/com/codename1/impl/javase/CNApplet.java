/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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

/**
 *
 * @author shannah
 */
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
