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
package java.io;

import com.codename1.impl.JdkHost;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/// A path in the file system, and the operations on the file or directory at
/// it.
///
/// Every operation goes to the platform's file system through
/// `com.codename1.impl.JdkHost`: on a device that is `FileSystemStorage`, in a
/// program translated without Codename One it is the native file system.
///
/// #### Paths
///
/// A path is kept with duplicate separators collapsed and no trailing one,
/// and a backslash is read as a separator. It is absolute when it starts with
/// `/`, with a drive (`C:/`), or with the `file:` scheme Codename One names its
/// files with. A relative path is resolved against the application's home
/// directory, which in a program run from a shell is the working directory.
///
/// #### What differs from a desktop file system
///
/// - Where the file system has no permissions, every file that exists may be
///   read and written, a directory may be entered and nothing may be run; the
///   permission setters answer whether the file already is as asked.
/// - [#setLastModified(long)] answers false: no platform lets it be set.
/// - [#deleteOnExit()] does nothing.
/// - Where the file system cannot resolve symbolic links,
///   [#getCanonicalPath()] answers the absolute path with `.` and `..`
///   resolved.
public class File implements Serializable, Comparable<File> {

    private static final long serialVersionUID = 1L;

    /// The separator of the names in a path.
    public static final char separatorChar = '/';
    /// [#separatorChar] as a string.
    public static final String separator = "/";
    /// The separator of the paths in a list of them.
    public static final char pathSeparatorChar = ':';
    /// [#pathSeparatorChar] as a string.
    public static final String pathSeparator = ":";

    private static final String SCHEME = "file:";

    private final String path;

    /// The file at `pathname`.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: when `pathname` is null
    public File(String pathname) {
        if (pathname == null) {
            throw new NullPointerException();
        }
        this.path = normalize(pathname);
    }

    /// The file `child` in the directory `parent`. A null `parent` makes it
    /// `new File(child)`, an empty one the root.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: when `child` is null
    public File(String parent, String child) {
        if (child == null) {
            throw new NullPointerException();
        }
        if (parent == null) {
            this.path = normalize(child);
        } else if (parent.length() == 0) {
            this.path = join(separator, child);
        } else {
            this.path = join(normalize(parent), child);
        }
    }

    /// The file `child` in the directory `parent`. A null `parent` makes it
    /// `new File(child)`, an empty one the root.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: when `child` is null
    public File(File parent, String child) {
        if (child == null) {
            throw new NullPointerException();
        }
        if (parent == null) {
            this.path = normalize(child);
        } else if (parent.path.length() == 0) {
            this.path = join(separator, child);
        } else {
            this.path = join(parent.path, child);
        }
    }

    /// The file a `file:` URI names.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when `uri` is not an absolute,
    ///   hierarchical `file:` URI with a path and without an authority, a query
    ///   or a fragment
    public File(URI uri) {
        if (!uri.isAbsolute()) {
            throw new IllegalArgumentException("URI is not absolute");
        }
        if (uri.isOpaque()) {
            throw new IllegalArgumentException("URI is not hierarchical");
        }
        if (!"file".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("URI scheme is not \"file\"");
        }
        if (present(uri.getRawAuthority())) {
            throw new IllegalArgumentException("URI has an authority component");
        }
        if (present(uri.getRawFragment())) {
            throw new IllegalArgumentException("URI has a fragment component");
        }
        if (present(uri.getRawQuery())) {
            throw new IllegalArgumentException("URI has a query component");
        }
        String p = uri.getPath();
        if (p == null || p.length() == 0) {
            throw new IllegalArgumentException("URI path component is empty");
        }
        this.path = normalize(p);
    }

    /// Whether a URI has the component. An empty one counts as none: the
    /// runtime's `URI` answers an empty authority for `file:///x`, where the
    /// JDK's answers null.
    private static boolean present(String component) {
        return component != null && component.length() > 0;
    }

    private static JdkHost host() {
        return JdkHost.get();
    }

    /// How many leading characters of `p` are its root: the scheme and its
    /// slashes, `/`, a drive with its separator, or none.
    private static int rootLength(String p) {
        if (p.startsWith(SCHEME)) {
            int i = SCHEME.length();
            while (i < p.length() && p.charAt(i) == '/') {
                i++;
            }
            return i;
        }
        if (p.startsWith("/")) {
            return 1;
        }
        if (p.length() >= 3 && p.charAt(1) == ':' && p.charAt(2) == '/' && isLetter(p.charAt(0))) {
            return 3;
        }
        return 0;
    }

    private static boolean isLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    /// Reads a backslash as a separator, collapses duplicate separators
    /// outside the scheme and drops a trailing one, as `java.io.File` does.
    private static String normalize(String p) {
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
        if (c.length() == 0) {
            return parent;
        }
        return parent.endsWith("/") ? parent + c : parent + "/" + c;
    }

    /// The path the host is asked about: this one when it is absolute, else
    /// resolved against the application's home directory.
    String hostPath() {
        if (path.startsWith("file:/") || (rootLength(path) > 0 && !path.startsWith(SCHEME))) {
            return path;
        }
        // A relative path, a "file:" with no slash after it included: a
        // device's file system throws for anything but an absolute path.
        String rel = path.startsWith(SCHEME) ? path.substring(SCHEME.length()) : path;
        String home = host().appHome();
        if (rel.length() == 0) {
            return home;
        }
        return home.endsWith("/") ? home + rel : home + "/" + rel;
    }

    /// The last name of the path, or the whole path when it has one name.
    public String getName() {
        int i = path.lastIndexOf('/');
        return i < 0 ? path : path.substring(i + 1);
    }

    /// The path as it was given, normalized.
    public String getPath() {
        return path;
    }

    /// The path resolved against the application's home directory when it is
    /// relative.
    public String getAbsolutePath() {
        return hostPath();
    }

    /// The file at [#getAbsolutePath()].
    public File getAbsoluteFile() {
        return new File(getAbsolutePath());
    }

    /// The absolute path with symbolic links, `.` and `..` resolved. Code
    /// checks a canonical path against a directory to reject a name like
    /// `root/../outside`, so where the file system cannot resolve links the
    /// dots are still resolved; a `..` at the root stays at the root.
    public String getCanonicalPath() throws IOException {
        String abs = hostPath();
        String canonical = host().fileCanonicalPath(abs);
        return canonical != null ? canonical : resolveDots(abs);
    }

    /// The file at [#getCanonicalPath()].
    public File getCanonicalFile() throws IOException {
        return new File(getCanonicalPath());
    }

    private static String resolveDots(String p) {
        int start = rootLength(p);
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

    /// The path without its last name, or null when it has no parent.
    public String getParent() {
        int i = path.lastIndexOf('/');
        if (i < 0) {
            return null;
        }
        int root = rootLength(path);
        if (i < root) {
            return path.length() > root ? path.substring(0, root) : null;
        }
        if (i == root - 1) {
            return path.length() > root ? path.substring(0, root) : null;
        }
        return path.substring(0, i);
    }

    /// The file at [#getParent()], or null.
    public File getParentFile() {
        String p = getParent();
        return p == null ? null : new File(p);
    }

    /// Whether the path is absolute.
    public boolean isAbsolute() {
        return rootLength(path) > 0;
    }

    /// Whether something is at the path.
    public boolean exists() {
        return host().fileTest(hostPath(), JdkHost.FILE_EXISTS);
    }

    /// Whether the path is a directory.
    public boolean isDirectory() {
        return host().fileTest(hostPath(), JdkHost.FILE_DIRECTORY);
    }

    /// Whether the path is a file that is not a directory.
    public boolean isFile() {
        return host().fileTest(hostPath(), JdkHost.FILE_REGULAR);
    }

    /// Whether the file is hidden; on most file systems, whether its name
    /// starts with a dot.
    public boolean isHidden() {
        return host().fileTest(hostPath(), JdkHost.FILE_HIDDEN);
    }

    /// Whether the file may be read.
    public boolean canRead() {
        return host().fileTest(hostPath(), JdkHost.FILE_READABLE);
    }

    /// Whether the file may be written.
    public boolean canWrite() {
        return host().fileTest(hostPath(), JdkHost.FILE_WRITABLE);
    }

    /// Whether the file may be run, or the directory entered.
    public boolean canExecute() {
        return host().fileTest(hostPath(), JdkHost.FILE_EXECUTABLE);
    }

    /// Lets the file be read, or not; false when it is not left as asked.
    public boolean setReadable(boolean readable, boolean ownerOnly) {
        return host().fileSet(hostPath(), JdkHost.FILE_READABLE, readable);
    }

    /// Lets the file be read, or not; false when it is not left as asked.
    public boolean setReadable(boolean readable) {
        return setReadable(readable, true);
    }

    /// Lets the file be written, or not; false when it is not left as asked.
    public boolean setWritable(boolean writable, boolean ownerOnly) {
        return host().fileSet(hostPath(), JdkHost.FILE_WRITABLE, writable);
    }

    /// Lets the file be written, or not; false when it is not left as asked.
    public boolean setWritable(boolean writable) {
        return setWritable(writable, true);
    }

    /// Lets the file be run, or not; false when it is not left as asked.
    public boolean setExecutable(boolean executable, boolean ownerOnly) {
        return host().fileSet(hostPath(), JdkHost.FILE_EXECUTABLE, executable);
    }

    /// Lets the file be run, or not; false when it is not left as asked.
    public boolean setExecutable(boolean executable) {
        return setExecutable(executable, true);
    }

    /// Stops the file being written; false when it still can be.
    public boolean setReadOnly() {
        return host().fileSet(hostPath(), JdkHost.FILE_WRITABLE, false);
    }

    /// Answers false: no platform lets a file's time be set.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when `time` is negative
    public boolean setLastModified(long time) {
        if (time < 0) {
            throw new IllegalArgumentException("Negative time");
        }
        return false;
    }

    /// The length of the file in bytes; 0 for a directory or when nothing is
    /// there.
    public long length() {
        return host().fileLength(hostPath());
    }

    /// When the file was last modified, in milliseconds since the epoch; 0
    /// when nothing is there.
    public long lastModified() {
        return host().fileLastModified(hostPath());
    }

    /// The size of the volume holding the file, or 0 when unknown.
    public long getTotalSpace() {
        return host().fileTotalSpace(hostPath());
    }

    /// The bytes free on the volume holding the file, or 0 when unknown.
    public long getFreeSpace() {
        return host().fileFreeSpace(hostPath());
    }

    /// The bytes this program may still write on the volume holding the
    /// file, or 0 when unknown.
    public long getUsableSpace() {
        return host().fileUsableSpace(hostPath());
    }

    /// Creates the file, empty, when nothing is at the path.
    ///
    /// #### Returns
    ///
    /// true when it was created, false when something was there
    ///
    /// #### Throws
    ///
    /// - `IOException`: when the directory it would be in does not exist, or
    ///   the file could not be created
    public boolean createNewFile() throws IOException {
        File parent = getParentFile();
        if (parent != null && !parent.isDirectory()) {
            throw new IOException("No such file or directory: " + getParent());
        }
        return host().fileCreate(hostPath());
    }

    /// Deletes the file, or the directory when it is empty.
    ///
    /// #### Returns
    ///
    /// whether it was deleted
    public boolean delete() {
        return host().fileDelete(hostPath());
    }

    /// Does nothing: nothing is deleted when the program ends.
    public void deleteOnExit() {
    }

    /// Creates the directory, in a parent that must exist.
    ///
    /// #### Returns
    ///
    /// whether it was created
    public boolean mkdir() {
        return host().fileMkdir(hostPath());
    }

    /// Creates the directory and the parents it needs.
    ///
    /// #### Returns
    ///
    /// whether it was created; false also when it already existed
    public boolean mkdirs() {
        if (exists()) {
            return false;
        }
        if (mkdir()) {
            return true;
        }
        File p = getParentFile();
        return p != null && (p.mkdirs() || p.exists()) && mkdir();
    }

    /// Moves the file or directory to `dest`. A file replaces a file already
    /// there. Where the platform cannot move a file in place, it is copied and
    /// the original deleted; when the original will not go, the copy is taken
    /// back unless it replaced something.
    ///
    /// #### Returns
    ///
    /// whether it was moved
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: when `dest` is null
    public boolean renameTo(File dest) {
        if (dest == null) {
            throw new NullPointerException();
        }
        JdkHost h = host();
        String from = hostPath();
        String to = dest.hostPath();
        if (!h.fileTest(from, JdkHost.FILE_EXISTS)) {
            return false;
        }
        // A file renamed onto itself is already where it was asked to be.
        // The copy below would open the same file for reading and writing,
        // truncating it, and then delete it.
        if (resolveDots(from).equals(resolveDots(to))) {
            return true;
        }
        boolean replacing = h.fileTest(to, JdkHost.FILE_EXISTS);
        if (replacing ? h.fileReplace(from, to) : h.fileRename(from, to)) {
            return true;
        }
        if (h.fileTest(from, JdkHost.FILE_DIRECTORY) || (replacing && h.fileTest(to, JdkHost.FILE_DIRECTORY))) {
            return false;
        }
        try {
            InputStream in = h.fileRead(from);
            try {
                OutputStream out = h.fileWrite(to, false);
                try {
                    byte[] buffer = new byte[8192];
                    int n;
                    while ((n = in.read(buffer, 0, buffer.length)) >= 0) {
                        out.write(buffer, 0, n);
                    }
                } finally {
                    out.close();
                }
            } finally {
                in.close();
            }
        } catch (IOException e) {
            return false;
        }
        // The move only happened if the source is gone. When it cannot be
        // removed the rename fails, and a copy that created the destination
        // is taken back so the file is left where it was; one that replaced
        // an existing destination stays, as deleting it would lose both.
        h.fileDelete(from);
        if (h.fileTest(from, JdkHost.FILE_EXISTS)) {
            if (!replacing) {
                h.fileDelete(to);
            }
            return false;
        }
        return true;
    }

    /// The names of the entries of the directory, or null when this is not a
    /// directory.
    public String[] list() {
        String[] names;
        try {
            names = host().fileList(hostPath());
        } catch (IOException e) {
            return null;
        }
        if (names == null) {
            return null;
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
    }

    /// The names of the entries of the directory that `filter` accepts, or
    /// null when this is not a directory. A null `filter` accepts all.
    public String[] list(FilenameFilter filter) {
        String[] names = list();
        if (names == null || filter == null) {
            return names;
        }
        List<String> out = new ArrayList<String>();
        for (int i = 0; i < names.length; i++) {
            if (filter.accept(this, names[i])) {
                out.add(names[i]);
            }
        }
        return out.toArray(new String[out.size()]);
    }

    /// The entries of the directory, or null when this is not a directory.
    public File[] listFiles() {
        return files(list());
    }

    /// The entries of the directory whose names `filter` accepts, or null when
    /// this is not a directory. A null `filter` accepts all.
    public File[] listFiles(FilenameFilter filter) {
        return files(list(filter));
    }

    /// The entries of the directory that `filter` accepts, or null when this
    /// is not a directory. A null `filter` accepts all.
    public File[] listFiles(FileFilter filter) {
        File[] all = listFiles();
        if (all == null || filter == null) {
            return all;
        }
        List<File> out = new ArrayList<File>();
        for (int i = 0; i < all.length; i++) {
            if (filter.accept(all[i])) {
                out.add(all[i]);
            }
        }
        return out.toArray(new File[out.size()]);
    }

    private File[] files(String[] names) {
        if (names == null) {
            return null;
        }
        File[] out = new File[names.length];
        for (int i = 0; i < names.length; i++) {
            out[i] = new File(this, names[i]);
        }
        return out;
    }

    /// Creates an empty file named `prefix`, a number and `suffix` in the
    /// directory for disposable files.
    public static File createTempFile(String prefix, String suffix) throws IOException {
        return createTempFile(prefix, suffix, null);
    }

    /// Creates an empty file named `prefix`, a number and `suffix` (`.tmp`
    /// when null) in `directory`, or in the directory for disposable files
    /// when that is null. Nothing deletes it.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when `prefix` is shorter than three
    ///   characters
    /// - `IOException`: when the file could not be created
    public static File createTempFile(String prefix, String suffix, File directory) throws IOException {
        if (prefix.length() < 3) {
            throw new IllegalArgumentException("Prefix string too short");
        }
        File dir = directory;
        if (dir == null) {
            JdkHost h = host();
            String caches = h.cachesDir();
            dir = new File(caches != null ? caches : h.appHome());
        }
        String s = suffix == null ? ".tmp" : suffix;
        long n = System.currentTimeMillis();
        while (true) {
            File f = new File(dir, prefix + n + s);
            if (f.createNewFile()) {
                return f;
            }
            if (!f.exists()) {
                throw new IOException("Unable to create temporary file " + f.getPath());
            }
            n++;
        }
    }

    /// The roots of the file system.
    public static File[] listRoots() {
        String[] roots = host().fileRoots();
        File[] out = new File[roots == null ? 0 : roots.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = new File(roots[i]);
        }
        return out;
    }

    /// Compares the paths.
    @Override
    public int compareTo(File other) {
        return path.compareTo(other.path);
    }

    /// Whether `o` is a file with the same path.
    @Override
    public boolean equals(Object o) {
        return o instanceof File && ((File) o).path.equals(path);
    }

    @Override
    public int hashCode() {
        return path.hashCode() ^ 1234321;
    }

    /// The path.
    @Override
    public String toString() {
        return path;
    }

    /// This file as a `java.nio.file.Path`.
    public Path toPath() {
        return Paths.get(path);
    }

    /// The absolute path, without the scheme and with a single leading
    /// separator, as a URI's path.
    private String uriPath() {
        String abs = getAbsolutePath();
        if (abs.startsWith(SCHEME)) {
            int at = SCHEME.length();
            while (at + 1 < abs.length() && abs.charAt(at) == '/' && abs.charAt(at + 1) == '/') {
                at++;
            }
            abs = abs.substring(at);
        }
        if (!abs.startsWith("/")) {
            abs = "/" + abs;
        }
        return abs;
    }

    /// The `file:` URI of the absolute path, ending in a separator when the
    /// file is a directory.
    public URI toURI() {
        String p = uriPath();
        if (!p.endsWith("/") && isDirectory()) {
            p = p + "/";
        }
        try {
            return new URI("file", null, p, null, null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /// The `file:` URL of the absolute path.
    ///
    /// #### Deprecated
    ///
    /// The path is not escaped; use `toURI().toURL()` where it may hold
    /// characters a URL does not allow.
    @Deprecated
    public URL toURL() throws java.net.MalformedURLException {
        return new URL("file", "", uriPath());
    }

    // ------------------------------------------------------------------
    // The native file system, for PosixFileAccess and nothing else. The
    // natives keep their names and signatures, which the C runtime and the
    // JavaScript bindings implement; they are reached only through these
    // bridges, so a program that never installs PosixJdkHost never calls
    // them.

    private static File nativeReceiver;

    private static File receiver() {
        File f = nativeReceiver;
        if (f == null) {
            f = new File(separator);
            nativeReceiver = f;
        }
        return f;
    }

    static boolean nExists(String p) {
        return receiver().existsImpl(p);
    }

    static boolean nIsDirectory(String p) {
        return receiver().isDirectoryImpl(p);
    }

    static boolean nIsFile(String p) {
        return receiver().isFileImpl(p);
    }

    static boolean nIsHidden(String p) {
        return receiver().isHiddenImpl(p);
    }

    static long nLastModified(String p) {
        return receiver().lastModifiedImpl(p);
    }

    static long nLength(String p) {
        return receiver().lengthImpl(p);
    }

    static boolean nCreateNewFile(String p) {
        return receiver().createNewFileImpl(p);
    }

    static boolean nDelete(String p) {
        return receiver().deleteImpl(p);
    }

    static String[] nList(String p) {
        return receiver().listImpl(p);
    }

    static boolean nMkdir(String p) {
        return receiver().mkdirImpl(p);
    }

    static boolean nRename(String p, String dest) {
        return receiver().renameToImpl(p, dest);
    }

    static boolean nSetReadOnly(String p) {
        return receiver().setReadOnlyImpl(p);
    }

    static boolean nSetWritable(String p, boolean on) {
        return receiver().setWritableImpl(p, on);
    }

    static boolean nSetReadable(String p, boolean on) {
        return receiver().setReadableImpl(p, on);
    }

    static boolean nSetExecutable(String p, boolean on) {
        return receiver().setExecutableImpl(p, on);
    }

    static boolean nCanRead(String p) {
        return receiver().canReadImpl(p);
    }

    static boolean nCanWrite(String p) {
        return receiver().canWriteImpl(p);
    }

    static boolean nCanExecute(String p) {
        return receiver().canExecuteImpl(p);
    }

    static long nTotalSpace(String p) {
        return receiver().getTotalSpaceImpl(p);
    }

    static long nFreeSpace(String p) {
        return receiver().getFreeSpaceImpl(p);
    }

    static long nUsableSpace(String p) {
        return receiver().getUsableSpaceImpl(p);
    }

    static String nAbsolutePath(String p) {
        return receiver().getAbsolutePathImpl(p);
    }

    static String nCanonicalPath(String p) {
        return receiver().getCanonicalPathImpl(p);
    }

    private native String getAbsolutePathImpl(String path);

    private native String getCanonicalPathImpl(String path);

    private native boolean existsImpl(String path);

    private native boolean isDirectoryImpl(String path);

    private native boolean isFileImpl(String path);

    private native boolean isHiddenImpl(String path);

    private native long lastModifiedImpl(String path);

    private native long lengthImpl(String path);

    private native boolean createNewFileImpl(String path);

    private native boolean deleteImpl(String path);

    private native String[] listImpl(String path);

    private native boolean mkdirImpl(String path);

    private native boolean renameToImpl(String path, String dest);

    private native boolean setReadOnlyImpl(String path);

    private native boolean setWritableImpl(String path, boolean writable);

    private native boolean setReadableImpl(String path, boolean readable);

    private native boolean setExecutableImpl(String path, boolean executable);

    private native boolean canReadImpl(String path);

    private native boolean canWriteImpl(String path);

    private native boolean canExecuteImpl(String path);

    private native long getTotalSpaceImpl(String path);

    private native long getFreeSpaceImpl(String path);

    private native long getUsableSpaceImpl(String path);
}
