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
package java.io;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Basic implementation of {@link java.io.File} backed by platform native code.
 */
public class File implements Comparable<File>, Serializable {
    private static final long serialVersionUID = 301077366599181567L;

    public static final String pathSeparator = ":";
    public static final char pathSeparatorChar = ':';
    public static final String separator = "/";
    public static final char separatorChar = '/';

    private final String path;

    static interface FileAccess {
        boolean canExecute(String path);
        boolean canRead(String path);
        boolean canWrite(String path);
        boolean createFile(String path);
        String createTempFile(String prefix, String suffix, String directory);
        boolean delete(String path);
        boolean exists(String path);
        boolean isDirectory(String path);
        boolean isFile(String path);
        boolean isHidden(String path);
        boolean mkdir(String path);
        boolean rename(String source, String dest);
        boolean setExecutable(String path, boolean executable, boolean ownerOnly);
        boolean setReadable(String path, boolean readable, boolean ownerOnly);
        boolean setWritable(String path, boolean writable, boolean ownerOnly);
        boolean setLastModified(String path, long time);
        String absolutePath(String path);
        String canonicalPath(String path);
        long lastModified(String path);
        long length(String path);
        long totalSpace(String path);
        long freeSpace(String path);
        long usableSpace(String path);
        String[] list(String path);
        String[] listRoots();
    }

    private static final class NativeAccess implements FileAccess {
        public boolean canExecute(String path) { return nativeCanExecute(path); }
        public boolean canRead(String path) { return nativeCanRead(path); }
        public boolean canWrite(String path) { return nativeCanWrite(path); }
        public boolean createFile(String path) { return nativeCreateFile(path); }
        public String createTempFile(String prefix, String suffix, String directory) { return nativeCreateTempFile(prefix, suffix, directory); }
        public boolean delete(String path) { return nativeDelete(path); }
        public boolean exists(String path) { return nativeExists(path); }
        public boolean isDirectory(String path) { return nativeIsDirectory(path); }
        public boolean isFile(String path) { return nativeIsFile(path); }
        public boolean isHidden(String path) { return nativeIsHidden(path); }
        public boolean mkdir(String path) { return nativeMkdir(path); }
        public boolean rename(String source, String dest) { return nativeRename(source, dest); }
        public boolean setExecutable(String path, boolean executable, boolean ownerOnly) { return nativeSetExecutable(path, executable, ownerOnly); }
        public boolean setReadable(String path, boolean readable, boolean ownerOnly) { return nativeSetReadable(path, readable, ownerOnly); }
        public boolean setWritable(String path, boolean writable, boolean ownerOnly) { return nativeSetWritable(path, writable, ownerOnly); }
        public boolean setLastModified(String path, long time) { return nativeSetLastModified(path, time); }
        public String absolutePath(String path) { return nativeAbsolutePath(path); }
        public String canonicalPath(String path) { return nativeCanonicalPath(path); }
        public long lastModified(String path) { return nativeLastModified(path); }
        public long length(String path) { return nativeLength(path); }
        public long totalSpace(String path) { return nativeTotalSpace(path); }
        public long freeSpace(String path) { return nativeFreeSpace(path); }
        public long usableSpace(String path) { return nativeUsableSpace(path); }
        public String[] list(String path) { return nativeList(path); }
        public String[] listRoots() { return nativeListRoots(); }
    }

    private static FileAccess access = new NativeAccess();

    static void setFileAccess(FileAccess a) {
        access = a == null ? new NativeAccess() : a;
    }

    public File(File parent, String child) {
        this(checkNullParent(parent).getPath(), child);
    }

    public File(String pathname) {
        if (pathname == null) {
            throw new NullPointerException("Pathname cannot be null");
        }
        this.path = normalize(pathname);
    }

    public File(String parent, String child) {
        if (child == null) {
            throw new NullPointerException("Child cannot be null");
        }
        if (parent == null || parent.length() == 0) {
            this.path = normalize(child);
            return;
        }
        this.path = normalize(resolve(parent, child));
    }

    public File(URI uri) {
        if (uri == null) {
            throw new NullPointerException("URI cannot be null");
        }
        if (!"file".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("URI scheme is not " + uri.getScheme());
        }
        this.path = normalize(uri.getPath());
    }

    public boolean canExecute() {
        return access.canExecute(path);
    }

    public boolean canRead() {
        return access.canRead(path);
    }

    public boolean canWrite() {
        return access.canWrite(path);
    }

    public int compareTo(File pathname) {
        return getPath().compareTo(pathname.getPath());
    }

    public boolean createNewFile() throws IOException {
        if (exists()) {
            return false;
        }
        if (!access.createFile(path)) {
            throw new IOException("Failed to create file: " + path);
        }
        return true;
    }

    public static File createTempFile(String prefix, String suffix) throws IOException {
        return createTempFile(prefix, suffix, null);
    }

    public static File createTempFile(String prefix, String suffix, File directory) throws IOException {
        if (prefix == null) {
            throw new NullPointerException("Prefix cannot be null");
        }
        String dirPath = directory == null ? null : directory.getPath();
        String result = access.createTempFile(prefix, suffix, dirPath);
        if (result == null) {
            throw new IOException("Failed to create temp file");
        }
        return new File(result);
    }

    public boolean delete() {
        return access.delete(path);
    }

    public void deleteOnExit() {
        // No-op placeholder for compatibility
    }

    public boolean exists() {
        return access.exists(path);
    }

    public File getAbsoluteFile() {
        return new File(getAbsolutePath());
    }

    public String getAbsolutePath() {
        String abs = access.absolutePath(path);
        return abs == null ? normalize(path) : abs;
    }

    public String getCanonicalPath() throws IOException {
        String canonical = access.canonicalPath(path);
        if (canonical == null) {
            throw new IOException("Cannot resolve canonical path for " + path);
        }
        return canonical;
    }

    public File getCanonicalFile() throws IOException {
        return new File(getCanonicalPath());
    }

    public long getFreeSpace() {
        return access.freeSpace(path);
    }

    public String getName() {
        int idx = lastSeparatorIndex(path);
        return idx >= 0 ? path.substring(idx + 1) : path;
    }

    public String getParent() {
        int idx = lastSeparatorIndex(path);
        return idx <= 0 ? null : path.substring(0, idx);
    }

    public File getParentFile() {
        String parent = getParent();
        return parent == null ? null : new File(parent);
    }

    public String getPath() {
        return path;
    }

    public long getTotalSpace() {
        return access.totalSpace(path);
    }

    public long getUsableSpace() {
        return access.usableSpace(path);
    }

    public boolean isAbsolute() {
        return isAbsolutePath(path);
    }

    public boolean isDirectory() {
        return access.isDirectory(path);
    }

    public boolean isFile() {
        return access.isFile(path);
    }

    public boolean isHidden() {
        return access.isHidden(path);
    }

    public long lastModified() {
        return access.lastModified(path);
    }

    public long length() {
        return access.length(path);
    }

    public String[] list() {
        return access.list(path);
    }

    public String[] list(FilenameFilter filter) {
        String[] names = list();
        if (names == null || filter == null) {
            return names;
        }
        int count = 0;
        for (int i = 0; i < names.length; i++) {
            if (filter.accept(this, names[i])) {
                count++;
            }
        }
        String[] out = new String[count];
        int idx = 0;
        for (int i = 0; i < names.length; i++) {
            if (filter.accept(this, names[i])) {
                out[idx++] = names[i];
            }
        }
        return out;
    }

    public File[] listFiles() {
        String[] names = list();
        if (names == null) {
            return null;
        }
        File[] files = new File[names.length];
        for (int i = 0; i < names.length; i++) {
            files[i] = new File(this, names[i]);
        }
        return files;
    }

    public File[] listFiles(FilenameFilter filter) {
        String[] names = list(filter);
        if (names == null) {
            return null;
        }
        File[] files = new File[names.length];
        for (int i = 0; i < names.length; i++) {
            files[i] = new File(this, names[i]);
        }
        return files;
    }

    public File[] listFiles(FileFilter filter) {
        File[] files = listFiles();
        if (files == null || filter == null) {
            return files;
        }
        int count = 0;
        for (int i = 0; i < files.length; i++) {
            if (filter.accept(files[i])) {
                count++;
            }
        }
        File[] out = new File[count];
        int idx = 0;
        for (int i = 0; i < files.length; i++) {
            if (filter.accept(files[i])) {
                out[idx++] = files[i];
            }
        }
        return out;
    }

    public static File[] listRoots() {
        String[] roots = access.listRoots();
        if (roots == null) {
            return new File[0];
        }
        File[] files = new File[roots.length];
        for (int i = 0; i < roots.length; i++) {
            files[i] = new File(roots[i]);
        }
        return files;
    }

    public boolean mkdir() {
        return access.mkdir(path);
    }

    public boolean mkdirs() {
        if (exists()) {
            return false;
        }
        File parent = getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        return mkdir();
    }

    public boolean renameTo(File f) {
        if (f == null) {
            throw new NullPointerException("Target file cannot be null");
        }
        return access.rename(path, f.getPath());
    }

    public boolean setExecutable(boolean executable) {
        return setExecutable(executable, true);
    }

    public boolean setExecutable(boolean executable, boolean ownerOnly) {
        return access.setExecutable(path, executable, ownerOnly);
    }

    public boolean setReadable(boolean readable) {
        return setReadable(readable, true);
    }

    public boolean setReadable(boolean readable, boolean ownerOnly) {
        return access.setReadable(path, readable, ownerOnly);
    }

    public boolean setReadOnly() {
        return setWritable(false, true);
    }

    public boolean setWritable(boolean writable) {
        return setWritable(writable, true);
    }

    public boolean setWritable(boolean writable, boolean ownerOnly) {
        return access.setWritable(path, writable, ownerOnly);
    }

    public boolean setLastModified(long time) {
        return access.setLastModified(path, time);
    }

    public URI toURI() {
        try {
            String absPath = getAbsolutePath();
            String normalized = absPath.replace(separatorChar, '/');
            if (!normalized.startsWith("/")) {
                normalized = "/" + normalized;
            }
            return new URI("file", null, normalized, null, null);
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }

    public URL toURL() throws MalformedURLException {
        return new URL(toURI().toString());
    }

    public Path toPath() {
        return java.nio.file.Paths.get(getPath());
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof File)) {
            return false;
        }
        File other = (File) obj;
        return path.equals(other.path);
    }

    public int hashCode() {
        return path.hashCode();
    }

    public String toString() {
        return path;
    }

    private static String normalize(String path) {
        String value = path.replace('\\', separatorChar);
        while (value.indexOf("//") >= 0) {
            int idx = value.indexOf("//");
            value = value.substring(0, idx + 1) + value.substring(idx + 2);
        }
        return value;
    }

    private static boolean isAbsolutePath(String value) {
        return value.length() > 0 && (value.charAt(0) == separatorChar || (value.length() > 1 && value.charAt(1) == ':'));
    }

    private static int lastSeparatorIndex(String value) {
        int idx = value.lastIndexOf(separatorChar);
        int alt = value.lastIndexOf('\\');
        return idx > alt ? idx : alt;
    }

    private static String resolve(String parent, String child) {
        if (child.length() > 0 && child.charAt(0) == separatorChar) {
            return child;
        }
        if (parent.endsWith(separator)) {
            return parent + child;
        }
        return parent + separatorChar + child;
    }

    private static File checkNullParent(File parent) {
        if (parent == null) {
            throw new NullPointerException("Parent cannot be null");
        }
        return parent;
    }

    private static native boolean nativeCanExecute(String path);
    private static native boolean nativeCanRead(String path);
    private static native boolean nativeCanWrite(String path);
    private static native boolean nativeCreateFile(String path);
    private static native String nativeCreateTempFile(String prefix, String suffix, String directory);
    private static native boolean nativeDelete(String path);
    private static native boolean nativeExists(String path);
    private static native boolean nativeIsDirectory(String path);
    private static native boolean nativeIsFile(String path);
    private static native boolean nativeIsHidden(String path);
    private static native boolean nativeMkdir(String path);
    private static native boolean nativeRename(String source, String dest);
    private static native boolean nativeSetExecutable(String path, boolean executable, boolean ownerOnly);
    private static native boolean nativeSetReadable(String path, boolean readable, boolean ownerOnly);
    private static native boolean nativeSetWritable(String path, boolean writable, boolean ownerOnly);
    private static native boolean nativeSetLastModified(String path, long time);
    private static native String nativeAbsolutePath(String path);
    private static native String nativeCanonicalPath(String path);
    private static native long nativeLastModified(String path);
    private static native long nativeLength(String path);
    private static native long nativeTotalSpace(String path);
    private static native long nativeFreeSpace(String path);
    private static native long nativeUsableSpace(String path);
    private static native String[] nativeList(String path);
    private static native String[] nativeListRoots();
}
