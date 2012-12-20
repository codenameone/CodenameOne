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
import java.util.HashMap;
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

    public ClassPathLoader(File[] classpath) {
        super(ClassPathLoader.class.getClassLoader());
        this.classpath = classpath;
    }

    public Class loadClass(String className) throws ClassNotFoundException {
        return findClass(className);
    }

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
}
