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

package com.codename1.io;

import com.codename1.ui.Display;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * This class provides a similar API to {@code java.io.File} making it almost into a "drop in" replacement. 
 * It is placed in a different package because it is incompatible to {@code java.io.File} by definition.  It is useful
 * in getting some simple code to work without too many changes
 * 
 * @author Shai Almog
 */
public class File {
    public static final char separatorChar = '/';
    public static final java.lang.String separator = "/";
    private String path;
    
    public File(java.lang.String path) {
        if(!path.startsWith("file:")) {
            this.path = FileSystemStorage.getInstance().getAppHomePath() + path;
        } else {
            this.path = path;
        }
    }
    
    public File(java.lang.String dir, java.lang.String file) {
        if(!dir.startsWith("file:")) {
            dir = FileSystemStorage.getInstance().getAppHomePath() + dir;
        } 
        if(!dir.endsWith("/")) {
            this.path = dir + "/" + file;
        } else {
            this.path = dir + file;
        }
    }
    
    public File(File parent, java.lang.String path) {
        if(!parent.path.endsWith("/")) {
            this.path = parent.path + "/" + path;
        } else {
            this.path = parent.path + path;
        }
    }
    
    public java.lang.String getName() {
        return path.substring(path.lastIndexOf('/') + 1);
    }
    
    public java.lang.String getParent() {
        return path.substring(0, path.lastIndexOf('/'));
    }
    
    public File getParentFile() {
        return new File(getParent());
    }
    
    public java.lang.String getPath() {
        return path;
    }
    
    public boolean isAbsolute() {
        return true;
    }
    
    public java.lang.String getAbsolutePath() {
        return path;
    }
    
    public File getAbsoluteFile() {
        return this;
    }
    
    public boolean exists() {
        return FileSystemStorage.getInstance().exists(path);
    }
    
    public boolean isDirectory() {
        return FileSystemStorage.getInstance().isDirectory(path);
    }
    
    public boolean isFile() {
        return !isDirectory();
    }
    
    public boolean isHidden() {
        return FileSystemStorage.getInstance().isHidden(path);
    }
    
    public long lastModified() {
        return FileSystemStorage.getInstance().getLastModified(path);
    }
    
    public long length() {
        return FileSystemStorage.getInstance().getLength(path);
    }
    
    public boolean createNewFile() throws java.io.IOException {
        OutputStream os = FileSystemStorage.getInstance().openOutputStream(path);
        os.close();
        return exists();
    }
    
    public boolean delete() {
        FileSystemStorage.getInstance().delete(path);
        return FileSystemStorage.getInstance().exists(path);
    }
    
    public java.lang.String[] list() {
        try {
            String[] result = FileSystemStorage.getInstance().listFiles(path);
            for(int iter = 0 ; iter < result.length ; iter++) {
                int len = result[iter].length();
                if(result[iter].endsWith("/")) {
                    result[iter] = result[iter].substring(0, len - 1);
                }
                if (result[iter].indexOf("/") != -1) {
                    result[iter] = result[iter].substring(result[iter].lastIndexOf("/")+1, len);
                }
            }
            return result;
        } catch(IOException err) {
            return null;
        }
    }
    
    public java.lang.String[] list(FilenameFilter filter) {
        String[] arr = list();
        if(arr.length > 0) {
            ArrayList<String> result = new ArrayList<String>();
            for(String s : arr) {
                if(filter.accept(this, s)) {
                    result.add(s);
                }
            }
            String[] res = new String[result.size()];
            result.toArray(res);
            return res;
        }
        return arr;
    }
    
    public static interface FilenameFilter {
        public abstract boolean accept(File f, String name);
    }
    
    public File[] listFiles() {
        String[] r = list();
        File[] files = new File[r.length];
        for(int iter = 0 ; iter < r.length ; iter++) {
            files[iter]  = new File(this, r[iter]);
        }
        return files;
    }
    
    public File[] listFiles(FilenameFilter ff) {
        String[] r = list(ff);
        File[] files = new File[r.length];
        for(int iter = 0 ; iter < r.length ; iter++) {
            files[iter]  = new File(this, r[iter]);
        }
        return files;
    }
    
    public static interface FileFilter {
        public abstract boolean accept(File f);
    }
    
    public File[] listFiles(FileFilter ff) {
        File[] arr = listFiles();
        if(arr.length > 0) {
            ArrayList<File> result = new ArrayList<File>();
            for(File s : arr) {
                if(ff.accept(s)) {
                    result.add(s);
                }
            }
            File[] res = new File[result.size()];
            result.toArray(res);
            return res;
        }
        return arr;
    }
    
    public boolean mkdir() {
        return mkdirs();
    }
    
    public boolean mkdirs() {
        FileSystemStorage.getInstance().mkdir(path);
        return exists() && isDirectory();
    }
    
    public boolean renameTo(File f) {
        FileSystemStorage.getInstance().rename(path, f.getName());
        return f.exists();
    }
    
    public boolean canExecute() {
        return Display.getInstance().canExecute(path);
    }
    
    public static File[] listRoots() {
        return new File[] {
            new File(FileSystemStorage.getInstance().getAppHomePath())
        };
    }
    public long getTotalSpace() {
        return FileSystemStorage.getInstance().getRootSizeBytes(FileSystemStorage.getInstance().getRoots()[0]);
    }
    public long getFreeSpace() {
        return FileSystemStorage.getInstance().getRootAvailableSpace(FileSystemStorage.getInstance().getRoots()[0]);
    }
    public long getUsableSpace() {
        return getFreeSpace();
    }
    
    public static File createTempFile(java.lang.String prefix, java.lang.String suffix) throws java.io.IOException {
        String p = FileSystemStorage.getInstance().getAppHomePath() + "/temp/";
        FileSystemStorage.getInstance().mkdir(p);
        return new File(p + prefix + System.currentTimeMillis() + suffix);
        
    }
    
    public boolean equals(java.lang.Object o) {
        return o instanceof File && ((File)o).path.equals(path);
    }
    
    public int hashCode() {
        return path.hashCode();
    }
    
    public java.lang.String toString() {
        return path;
    }
}
