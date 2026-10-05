/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.androidcompat.jdk;

import com.codename1.io.FileSystemStorage;
import com.codename1.io.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/// `java.io.File` for the Codename One runtime, over `FileSystemStorage`.
/// Android code is compiled against the JDK class and the build's remap step
/// points it here.
///
/// Paths map onto the application's sandbox: a `file:` path is used as it
/// is, an absolute path (`/data/...`) becomes a `file://` path, and a
/// relative path is resolved against the application home, the directory
/// `Context.getDataDir()` answers and `getFilesDir()` is under.
/// Serializable like the JDK class: Android code compiled against
/// `java.io.File` passes one to `Intent.putExtra(String, Serializable)` and
/// `Bundle.putSerializable`, and the remapped call still names that
/// parameter type.
public class File implements Comparable<File>, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    public static final char separatorChar = '/';
    public static final String separator = "/";
    public static final char pathSeparatorChar = ':';
    public static final String pathSeparator = ":";

    private final String path;

    public File(String pathname) {
        if (pathname == null) {
            throw new NullPointerException();
        }
        this.path = normalize(pathname);
    }

    public File(String parent, String child) {
        if (child == null) {
            throw new NullPointerException();
        }
        this.path = parent == null ? normalize(child) : join(normalize(parent), child);
    }

    public File(File parent, String child) {
        if (child == null) {
            throw new NullPointerException();
        }
        this.path = parent == null ? normalize(child) : join(parent.path, child);
    }

    private static String normalize(String p) {
        // Collapse duplicate separators (outside the scheme) and drop a
        // trailing one, as java.io.File does.
        StringBuilder sb = new StringBuilder(p.length());
        int start = 0;
        if (p.startsWith("file://")) {
            sb.append("file://");
            start = 7;
        }
        char prev = 0;
        for (int i = start; i < p.length(); i++) {
            char c = p.charAt(i);
            if (c == '\\') {
                c = '/';
            }
            if (c == '/' && prev == '/') {
                continue;
            }
            sb.append(c);
            prev = c;
        }
        int min = start + 1;
        while (sb.length() > min && sb.charAt(sb.length() - 1) == '/') {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    private static String join(String parent, String child) {
        String c = normalize(child);
        if (parent.length() == 0) {
            return c;
        }
        if (c.startsWith("/")) {
            c = c.substring(1);
        }
        return parent.endsWith("/") ? parent + c : parent + "/" + c;
    }

    /// The `FileSystemStorage` path for this file.
    public String storagePath() {
        if (path.startsWith("file:")) {
            return path;
        }
        if (path.startsWith("/")) {
            return "file://" + path;
        }
        String home = FileSystemStorage.getInstance().getAppHomePath();
        if (!home.endsWith("/")) {
            home = home + "/";
        }
        return home + path;
    }

    private static FileSystemStorage fs() {
        return FileSystemStorage.getInstance();
    }

    public String getName() {
        int i = path.lastIndexOf('/');
        return i < 0 ? path : path.substring(i + 1);
    }

    public String getPath() {
        return path;
    }

    public String getAbsolutePath() {
        return storagePath();
    }

    public File getAbsoluteFile() {
        return new File(getAbsolutePath());
    }

    /// The absolute path with `.` and `..` segments resolved, as the JDK
    /// does: code checks a canonical path against a directory to reject a
    /// name like `root/../outside`, and the native file system would resolve
    /// the `..` the check never saw. A `..` at the root stays at the root.
    /// Symbolic links are not followed.
    public String getCanonicalPath() throws IOException {
        return resolveDots(getAbsolutePath());
    }

    public File getCanonicalFile() throws IOException {
        return new File(getCanonicalPath());
    }

    private static String resolveDots(String p) {
        // The prefix is the scheme and the slashes that follow it (`file:///`
        // or `file://` before an authority-less home such as `home/`).
        int start = p.startsWith("file:") ? 5 : 0;
        while (start < p.length() && p.charAt(start) == '/') {
            start++;
        }
        ArrayList<String> segs = new ArrayList<String>();
        int s = start;
        for (int i = start; i <= p.length(); i++) {
            if (i == p.length() || p.charAt(i) == '/') {
                String seg = p.substring(s, i);
                if (seg.equals("..")) {
                    if (!segs.isEmpty()) {
                        segs.remove(segs.size() - 1);
                    }
                } else if (seg.length() > 0 && !seg.equals(".")) {
                    segs.add(seg);
                }
                s = i + 1;
            }
        }
        StringBuilder sb = new StringBuilder(p.length());
        sb.append(p.substring(0, start));
        for (int i = 0; i < segs.size(); i++) {
            if (i > 0) {
                sb.append('/');
            }
            sb.append(segs.get(i));
        }
        return sb.toString();
    }

    public String getParent() {
        int i = path.lastIndexOf('/');
        if (i < 0) {
            return null;
        }
        if (i == 0) {
            return path.length() > 1 ? "/" : null;
        }
        String p = path.substring(0, i);
        return p.equals("file:/") ? null : p;
    }

    public File getParentFile() {
        String p = getParent();
        return p == null ? null : new File(p);
    }

    public boolean isAbsolute() {
        return path.startsWith("/") || path.startsWith("file:");
    }

    public boolean exists() {
        return fs().exists(storagePath());
    }

    public boolean isDirectory() {
        String p = storagePath();
        return fs().exists(p) && fs().isDirectory(p);
    }

    public boolean isFile() {
        String p = storagePath();
        return fs().exists(p) && !fs().isDirectory(p);
    }

    public boolean isHidden() {
        return getName().startsWith(".");
    }

    public boolean canRead() {
        return exists();
    }

    public boolean canWrite() {
        return exists();
    }

    public boolean canExecute() {
        return false;
    }

    public boolean setReadable(boolean readable) {
        return exists();
    }

    public boolean setWritable(boolean writable) {
        return exists();
    }

    public boolean setReadOnly() {
        return exists();
    }

    public boolean setLastModified(long time) {
        return false;
    }

    public long length() {
        String p = storagePath();
        return fs().exists(p) && !fs().isDirectory(p) ? fs().getLength(p) : 0L;
    }

    public long lastModified() {
        String p = storagePath();
        return fs().exists(p) ? fs().getLastModified(p) : 0L;
    }

    public long getFreeSpace() {
        return getUsableSpace();
    }

    public long getUsableSpace() {
        String[] roots = fs().getRoots();
        return roots == null || roots.length == 0 ? 0L : fs().getRootAvailableSpace(roots[0]);
    }

    public long getTotalSpace() {
        String[] roots = fs().getRoots();
        return roots == null || roots.length == 0 ? 0L : fs().getRootSizeBytes(roots[0]);
    }

    public boolean createNewFile() throws IOException {
        if (exists()) {
            return false;
        }
        File parent = getParentFile();
        if (parent != null && !parent.exists()) {
            throw new IOException("No such file or directory: " + getParent());
        }
        OutputStream out = fs().openOutputStream(storagePath());
        out.close();
        return true;
    }

    public boolean delete() {
        String p = storagePath();
        if (!fs().exists(p)) {
            return false;
        }
        if (fs().isDirectory(p)) {
            String[] children = list();
            if (children != null && children.length > 0) {
                return false;
            }
        }
        fs().delete(p);
        return !fs().exists(p);
    }

    public void deleteOnExit() {
    }

    public boolean mkdir() {
        String p = storagePath();
        if (fs().exists(p)) {
            return false;
        }
        File parent = getParentFile();
        if (parent != null && !parent.isDirectory()) {
            return false;
        }
        fs().mkdir(p);
        return fs().exists(p);
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

    /// Renames within a directory through the storage; elsewhere, copies and
    /// deletes, which is what a move across file systems does on Android.
    public boolean renameTo(File dest) {
        if (!exists() || dest == null) {
            return false;
        }
        String parent = getParent();
        String destParent = dest.getParent();
        if (parent != null && parent.equals(destParent) && !dest.exists()) {
            fs().rename(storagePath(), dest.getName());
            return dest.exists();
        }
        if (isDirectory()) {
            return false;
        }
        try {
            InputStream in = fs().openInputStream(storagePath());
            OutputStream out = fs().openOutputStream(dest.storagePath());
            Util.copy(in, out);
            fs().delete(storagePath());
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public String[] list() {
        String p = storagePath();
        if (!fs().exists(p) || !fs().isDirectory(p)) {
            return null;
        }
        try {
            String[] names = fs().listFiles(p);
            if (names == null) {
                return new String[0];
            }
            String[] out = new String[names.length];
            for (int i = 0; i < names.length; i++) {
                String n = names[i];
                while (n.endsWith("/")) {
                    n = n.substring(0, n.length() - 1);
                }
                int slash = n.lastIndexOf('/');
                out[i] = slash < 0 ? n : n.substring(slash + 1);
            }
            return out;
        } catch (IOException e) {
            return null;
        }
    }

    public String[] list(FilenameFilter filter) {
        String[] names = list();
        if (names == null || filter == null) {
            return names;
        }
        List<String> out = new ArrayList<String>();
        for (String n : names) {
            if (filter.accept(this, n)) {
                out.add(n);
            }
        }
        return out.toArray(new String[out.size()]);
    }

    public File[] listFiles() {
        String[] names = list();
        if (names == null) {
            return null;
        }
        File[] out = new File[names.length];
        for (int i = 0; i < names.length; i++) {
            out[i] = new File(this, names[i]);
        }
        return out;
    }

    public File[] listFiles(FilenameFilter filter) {
        String[] names = list(filter);
        if (names == null) {
            return null;
        }
        File[] out = new File[names.length];
        for (int i = 0; i < names.length; i++) {
            out[i] = new File(this, names[i]);
        }
        return out;
    }

    public File[] listFiles(FileFilter filter) {
        File[] all = listFiles();
        if (all == null || filter == null) {
            return all;
        }
        List<File> out = new ArrayList<File>();
        for (File f : all) {
            if (filter.accept(f)) {
                out.add(f);
            }
        }
        return out.toArray(new File[out.size()]);
    }

    public static File createTempFile(String prefix, String suffix) throws IOException {
        return createTempFile(prefix, suffix, null);
    }

    public static File createTempFile(String prefix, String suffix, File directory) throws IOException {
        if (prefix == null || prefix.length() < 3) {
            throw new IllegalArgumentException("Prefix string too short");
        }
        File dir = directory;
        if (dir == null) {
            String caches = FileSystemStorage.getInstance().hasCachesDir()
                    ? FileSystemStorage.getInstance().getCachesDir() : null;
            dir = new File(caches != null ? caches : FileSystemStorage.getInstance().getAppHomePath());
        }
        String s = suffix == null ? ".tmp" : suffix;
        long n = System.currentTimeMillis();
        while (true) {
            File f = new File(dir, prefix + n + s);
            if (f.createNewFile()) {
                return f;
            }
            n++;
        }
    }

    public static File[] listRoots() {
        String[] roots = FileSystemStorage.getInstance().getRoots();
        File[] out = new File[roots == null ? 0 : roots.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = new File(roots[i]);
        }
        return out;
    }

    @Override
    public int compareTo(File other) {
        return path.compareTo(other.path);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof File && ((File) o).path.equals(path);
    }

    @Override
    public int hashCode() {
        return path.hashCode() ^ 1234321;
    }

    @Override
    public String toString() {
        return path;
    }
}
