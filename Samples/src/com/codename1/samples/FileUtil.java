/*
 * Copyright (c) 2019, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.samples;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FileUtil {
    public static File createTempDirectory(String prefix, String suffix, File parentDirectory) throws IOException {
        final File tmp = File.createTempFile(prefix, suffix, parentDirectory);
        tmp.delete();
        tmp.mkdir();
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            if (tmp.exists()) {
                delTree(tmp);
            }
        }));
        return tmp;
        
    }
    
    public static boolean delTree(File directory) {
        if (directory.isDirectory()) {
            for (File child : directory.listFiles()) {
                delTree(child);
            }
        }
        return directory.delete();
    }
    
    public static void writeStringToFile(String string, File file) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileOutputStream(file))) {
            writer.append(string);
        }
    }
    
    public static String readFileToString(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return IOUtil.readToString(fis);
        }
    }
    
    public static long readFileToLong(File file) throws IOException {
        return Long.parseLong(readFileToString(file).trim());
    }
    
    public static List<File> find(File root, String prefix, String suffix) {
        return find(new ArrayList<File>(), root, prefix, suffix);
    }
    
    
    private static List<File> find(List<File> out, File root, String prefix, String suffix) {
        String name = root.getName();
        boolean match = true;
        if (prefix != null && !name.startsWith(prefix)) match = false;
        if (suffix != null && !name.endsWith(suffix)) match = false;
        if (match) out.add(root);
        if (root.isDirectory()) {
            for (File child : root.listFiles()) {
                find(out, child, prefix, suffix);
            }
        }
        return out;
    }
    
    public static void copy(File src, File dest) throws IOException {
        try (FileInputStream fis = new FileInputStream(src)) {
            try (FileOutputStream fos = new FileOutputStream(dest)) {
                IOUtil.copy(fis, fos);
            }
        }
    }
    
}