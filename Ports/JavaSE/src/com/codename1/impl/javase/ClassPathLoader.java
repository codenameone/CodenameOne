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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Class loader for the given classpath
 *
 * @author Shai Almog
 */
class ClassPathLoader extends ClassLoader {

    private File[] classpath;
    private Map classes = new HashMap();
    private Map<String,URL> resources = new HashMap<>();
    private List<String> excludes = new ArrayList<String>();
    {
        excludes.add("com.github.sarxos.webcam");
        excludes.add("org.bridj");
        excludes.add("java");
        excludes.add("com.sun");
        excludes.add("org.jdesktop");
        excludes.add("netscape.javascript");
        excludes.add("javafx");
        excludes.add("org.w3c");
    }
    private List<String> includes = new ArrayList<String>();
    {
        //includes.add("com.sun.javafx");
    }

    public ClassPathLoader(File[] classpath) {
        this(ClassPathLoader.class.getClassLoader(), classpath);
    }
    
    public ClassPathLoader(ClassLoader parent, File[] classpath) {
        super(parent);
        this.classpath = classpath;
    }
    
    public void addExclude(String exclude) {
        excludes.add(exclude);
    }
    
    public void addInclude(String include) {
        includes.add(include);
    }
    
    public void removeInclude(String include) {
        includes.remove(include);
    }
    
    private boolean isExcluded(String className) {
        //if (isIncluded(className)) {
        //    return false;
        //}
        for (String prefix : excludes) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isIncluded(String className) {
        for (String prefix : includes) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    public Class loadClass(String className) throws ClassNotFoundException {
        if (isExcluded(className)) {
            return super.loadClass(className);
        }
        return findClass(className);
    }

    @Override
    protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
        if (isExcluded(className)) {
            return super.loadClass(className, resolve);
        }
        return findClass(className);
        
    }

    
    
    @Override
    protected String findLibrary(String libname) {
        String out = super.findLibrary(libname);
        if (out == null) {
            for (String libDir : System.getProperty("cn1.library.path", "").split(File.pathSeparator)) {
                if (isMac) {
                    File f = new File(libDir, "lib" + libname + ".dylib");
                    if (f.exists()) {
                        return f.getAbsolutePath();
                    }
                    f = new File(libDir, "lib" + libname + ".jnilib");
                    if (f.exists()) {
                        return f.getAbsolutePath();
                    }
                } else if (isWindows) {
                    File f = new File(libDir, libname + ".dll");
                    if (f.exists()) {
                        return f.getAbsolutePath();
                    }
                    f = new File(libDir, libname + ".jnilib");
                    if (f.exists()) {
                        return f.getAbsolutePath();
                    }
                } else {
                    File f = new File(libDir, libname + ".so");
                    if (f.exists()) {
                        return f.getAbsolutePath();
                    }
                    f = new File(libDir, "lib" + libname + ".so");
                    if (f.exists()) {
                        return f.getAbsolutePath();
                    }
                    f = new File(libDir, libname + ".jnilib");
                    if (f.exists()) {
                        return f.getAbsolutePath();
                    }
                }
                
                
            }
           
            
        }
        return out;
    }
    
   

    /*
    Tried to override findResource to support JavaFX, but was dismal failure.  Commenting out.
    @Override
    protected URL findResource(String name) {
        URL result = super.findResource(name);
        if (result == null && !name.endsWith(".dylib")) {
            System.out.println("Looking for resource "+name);
            result = (URL) resources.get(name); //checks in cached classes
            if (result != null) {
                return result;
            }

            try {
                for (File f : classpath) {
                    //InputStream is;
                    if (f.isDirectory()) {
                        File current = new File(f, name);
                        if (!current.exists()) {
                            continue;
                        }
                        result = current.toURI().toURL();
                    } else {
                        try {
                            JarFile jar = new JarFile(f);

                            JarEntry entry = jar.getJarEntry(name);
                            if (entry == null) {
                                continue;
                            }
                            result = new URL("jar:" + f.toURI().toURL() + "!/" + name);
                            InputStream is = result.openStream();
                            System.out.println("Opening "+result+" resulted in stream "+is);
                        } catch (Throwable t) {
                            continue;
                        }
                    }
                    
                    resources.put(name, result);
                    System.out.println("returning "+result);
                    return result;
                }
            } catch (Exception e) {
            }

        }
        return result;

    }
 */
    
    
    public Class findClass(String className) throws ClassNotFoundException {
        byte[] classByte;
        Class result = null;

        result = (Class) classes.get(className); //checks in cached classes
        if (result != null) {
            return result;
        }

        try {
            for (File f : classpath) {
                InputStream is;
                if (f.isDirectory()) {
                    File current = new File(f, className.replace('.', File.separatorChar) + ".class");
                    if (!current.exists()) {
                        continue;
                    }
                    is = new FileInputStream(current);
                } else {
                    try {
                        JarFile jar = new JarFile(f);
                        JarEntry entry = jar.getJarEntry(className.replace('.', '/') + ".class");
                        if (entry == null) {
                            continue;
                        }
                        is = jar.getInputStream(entry);
                        if (is == null) {
                            continue;
                        }
                    } catch (Throwable t) {
                        continue;
                    }
                }
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                int nextValue = is.read();
                while (-1 != nextValue) {
                    byteStream.write(nextValue);
                    nextValue = is.read();
                }
                is.close();
                classByte = byteStream.toByteArray();
                result = defineClass(className, classByte, 0, classByte.length, null);
                classes.put(className, result);
                return result;
            }
        } catch (Exception e) {
        }
        return findSystemClass(className);
    }
    
    private static String OS = System.getProperty("os.name").toLowerCase();
    private static boolean isWindows = (OS.indexOf("win") >= 0);
    

    private static boolean isMac =  (OS.indexOf("mac") >= 0);
    

    private static boolean isUnix = (OS.indexOf("nux") >= 0);
}
