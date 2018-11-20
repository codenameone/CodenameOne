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
import com.codename1.util.StringUtil;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
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
    
    /**
     * Creates a new File object from the given URI
     * @param uri 
     */
    public File(URI uri) {
        this("file:/"+uri.getPath());
    }
    
    /**
     * Creates a new file object with given path. Paths that do not begin with the "file:" prefix 
     * will automatically be prefixed with the app home path.
     * @param path The path of the file.  Relative or absolute.
     */
    public File(java.lang.String path) {
        if(!path.startsWith("file:")) {
            this.path = FileSystemStorage.getInstance().getAppHomePath() + path;
        } else {
            this.path = path;
        }
    }
    
    /**
     * Creates a new file object in a given directory.
     * @param dir The parent directory path.
     * @param file The file name
     */
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
    
    /**
     * Creates a new file in the given parent directory, and subpath.
     * @param parent The parent directory.
     * @param path The subpath, beginning with the parent directory.
     */
    public File(File parent, java.lang.String path) {
        if(!parent.path.endsWith("/")) {
            this.path = parent.path + "/" + path;
        } else {
            this.path = parent.path + path;
        }
    }
    
    /**
     * Returns the file name.
     * @return The file name.
     */
    public java.lang.String getName() {
        return path.substring(path.lastIndexOf('/') + 1);
    }
    
    /**
     * Gets the parent directory path.
     * @return The parent directory path.
     */
    public java.lang.String getParent() {
        
        if ("file://".equals(path) || "file:///".equals(path) || path.length() == 0) {
            return null;
        }
        
        String out = path;
        if (out.endsWith("/")) {
            out = out.substring(0, out.length()-1);
            if (out.endsWith("/")) {
                return null;
            }
        }
        return out.substring(0, out.lastIndexOf('/'));
        
        
    }
    
    /**
     * Returns the file object for the parent directory.
     * @return 
     */
    public File getParentFile() {
        String parentPath = getParent();
        if (parentPath == null) {
            return null;
        }
        return new File(parentPath);
    }
    
    /**
     * Gets the path to the file.
     * @return 
     */
    public java.lang.String getPath() {
        return path;
    }
    
    /**
     * Checks if the path is absolute.  This always returns {@literal true} as all File objects
     * use absolute paths - even if they were created with relative paths.  Relative paths are automatically
     * prefixed with the app home directory path.
     * @return 
     */
    public boolean isAbsolute() {
        return true;
    }
    
    /**
     * Gets the absolute path of the file as a string,
     * @return 
     */
    public java.lang.String getAbsolutePath() {
        return path;
    }
    
    /**
     * Gets the absolute file - which is always itself, since {@link #isAbsolute() } always returns true.
     * @return The same file object.
     */
    public File getAbsoluteFile() {
        return this;
    }
    
    /**
     * Checks if the file described by this object exists on the file system.
     * @return 
     */
    public boolean exists() {
        return FileSystemStorage.getInstance().exists(path);
    }
    
    /**
     * Checks if this file is a directory.
     * @return 
     */
    public boolean isDirectory() {
        return FileSystemStorage.getInstance().isDirectory(path);
    }
    
    /**
     * Checks if this file object represents a regular file.
     * @return 
     */
    public boolean isFile() {
        return !isDirectory();
    }
    
    /**
     * Checks if this is a hidden file.
     * @return 
     */
    public boolean isHidden() {
        return FileSystemStorage.getInstance().isHidden(path);
    }
    
    /**
     * Gets the last modified time as a unix timestamp in milliseconds.
     * @return 
     */
    public long lastModified() {
        return FileSystemStorage.getInstance().getLastModified(path);
    }
    
    /**
     * Gets the file size in bytes.
     * @return The file size in bytes.
     */
    public long length() {
        return FileSystemStorage.getInstance().getLength(path);
    }
    
    /**
     * Creates this file as a new blank file in the file system.
     * @return True if it succeeds.
     * @throws java.io.IOException 
     */
    public boolean createNewFile() throws java.io.IOException {
        OutputStream os = FileSystemStorage.getInstance().openOutputStream(path);
        os.close();
        return exists();
    }
    
    /**
     * Deletes the file described by this object on the file system.
     * @return True if delete succeeds.
     */
    public boolean delete() {
        FileSystemStorage.getInstance().delete(path);
        return FileSystemStorage.getInstance().exists(path);
    }
    
    /**
     * Returns the list of child files of this directory.
     * @return 
     */
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
    
    /**
     * Returns list of child files of this directory
     * @param filter
     * @return 
     */
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
    
    /**
     * Interface to filter filenames.
     */
    public static interface FilenameFilter {
        /**
         * Checks if the given file should be included in results.
         * @param f The parent directory of the file to check.
         * @param name The file name.
         * @return True if the file should be included in results.
         */
        public abstract boolean accept(File f, String name);
    }
    
    /**
     * Gets a list of child files of this directory.
     * @return 
     */
    public File[] listFiles() {
        String[] r = list();
        File[] files = new File[r.length];
        for(int iter = 0 ; iter < r.length ; iter++) {
            files[iter]  = new File(this, r[iter]);
        }
        return files;
    }
    
    /**
     * Gets a list of child files of this directory, filtered using the provided filter.
     * @param ff The filter to use.
     * @return 
     */
    public File[] listFiles(FilenameFilter ff) {
        String[] r = list(ff);
        File[] files = new File[r.length];
        for(int iter = 0 ; iter < r.length ; iter++) {
            files[iter]  = new File(this, r[iter]);
        }
        return files;
    }
    
    /**
     * Interface for filtering files.
     */
    public static interface FileFilter {
        /**
         * Returns {@literal true} if the file should be included in results.
         * @param f The file to check
         * @return True to include.  False to not include in results.
         */
        public abstract boolean accept(File f);
    }
    
    /**
     * Gets a list of child files of this directory, filtering them using the provided filter.
     * @param ff The filter to use to filter output.
     * @return 
     */
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
    
    /**
     * Attempts to make the directory described by this object.
     * @return True on success.
     */
    public boolean mkdir() {
        FileSystemStorage.getInstance().mkdir(path);
        return exists() && isDirectory();
    }
    
    /**
     * Attempts to make the directory (and all parent directories) of this object.
     * @return True on success.
     */
    public boolean mkdirs() {
        File parentFile = getParentFile();
        if (parentFile!= null  && !parentFile.exists()) {
            boolean res = getParentFile().mkdirs();
            if (!res) {
                return res;
            }
        }
        return mkdir();
    }
    
    /**
     * Renames the file to the provided file object.
     * @param f The file object that we are renaming the file to.
     * @return True on success.
     */
    public boolean renameTo(File f) {
        FileSystemStorage.getInstance().rename(path, f.getName());
        return f.exists();
    }
    
    /**
     * Checks if this file is executable.
     * @return 
     */
    public boolean canExecute() {
        return Display.getInstance().canExecute(path);
    }
    
    /**
     * List the file system roots.
     * @return 
     */
    public static File[] listRoots() {
        return new File[] {
            new File(FileSystemStorage.getInstance().getAppHomePath())
        };
    }
    
    /**
     * Returns the total space on the root file system.
     * @return 
     */
    public long getTotalSpace() {
        return FileSystemStorage.getInstance().getRootSizeBytes(FileSystemStorage.getInstance().getRoots()[0]);
    }
    
    /**
     * Gets the free space on the root file system.
     * @return 
     */
    public long getFreeSpace() {
        return FileSystemStorage.getInstance().getRootAvailableSpace(FileSystemStorage.getInstance().getRoots()[0]);
    }
    
    /**
     * Gets the usable space on this file system.
     * @return 
     */
    public long getUsableSpace() {
        return getFreeSpace();
    }
    
    /**
     * Creates a temporary file.
     * @param prefix The file name prefix.
     * @param suffix The file name suffix
     * @return The resulting temporary file.
     * @throws java.io.IOException 
     */
    public static File createTempFile(java.lang.String prefix, java.lang.String suffix) throws java.io.IOException {
        String p = FileSystemStorage.getInstance().getAppHomePath() + "/temp/";
        FileSystemStorage.getInstance().mkdir(p);
        return new File(p + prefix + System.currentTimeMillis() + suffix);
        
    }
    
    /**
     * Checks if the given object refers to the same file.
     * @param o
     * @return 
     */
    public boolean equals(java.lang.Object o) {
        return o instanceof File && ((File)o).path.equals(path);
    }
    
    public int hashCode() {
        return path.hashCode();
    }
    
    public java.lang.String toString() {
        return path;
    }
    
    /**
     * Converts this file to a URL.
     * @return
     * @throws MalformedURLException 
     */
    public URL toURL() throws MalformedURLException {
        try {
            return new URL(toURI());
        } catch (URISyntaxException ex) {
            throw new MalformedURLException("Invalid URL format: "+ex.getMessage());
        }
    }
    
    /**
     * Converts this file to a URI.
     * @return
     * @throws URISyntaxException 
     */
    public URI toURI() throws URISyntaxException {
        String path = getAbsolutePath();
        path = path.substring(6);
        path = StringUtil.replaceAll(path, "\\", "/");
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        path = "/" + path;
        return new URI("file", null, path, null, null);
        
        
    }
}
