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
package com.codename1.impl;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.io.NetworkManager;
import com.codename1.io.Preferences;
import com.codename1.l10n.L10NManager;
import com.codename1.security.Hash;
import com.codename1.ui.Display;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/// The [JdkHost] of a Codename One application: the Java class library's
/// file system, preferences, network, log and event thread, served by
/// `FileSystemStorage`, `Preferences`, `NetworkManager`, `Log` and `Display`.
///
/// Internal. The ParparVM ports (iOS and macOS, Windows, Linux, JavaScript)
/// install it from their `init` through [CoreJdkHostInstaller]. On Java SE and
/// Android the real JDK runs and nothing installs it. `JdkHost` itself is part
/// of the class library, not of the core: the core compiles against it, and
/// the Java SE and Android ports carry a copy so this class always has its
/// superclass.
///
/// Paths are `FileSystemStorage` paths. Nothing here runs before the display
/// is initialized except what checks for it.
public class CoreJdkHost extends JdkHost {

    private static FileSystemStorage fs() {
        return FileSystemStorage.getInstance();
    }

    @Override
    public FileStat fileStat(String path) {
        FileSystemStorage fs = fs();
        if (!fs.exists(path)) {
            return null;
        }
        boolean directory = fs.isDirectory(path);
        return new FileStat(directory, directory ? 0L : fs.getLength(path), fs.getLastModified(path));
    }

    @Override
    public String[] fileList(String path) throws IOException {
        FileSystemStorage fs = fs();
        if (!fs.exists(path) || !fs.isDirectory(path)) {
            return null;
        }
        return fs.listFiles(path);
    }

    @Override
    public InputStream fileRead(String path) throws IOException {
        FileSystemStorage fs = fs();
        if (!fs.exists(path) || fs.isDirectory(path)) {
            throw new FileNotFoundException(path);
        }
        return fs.openInputStream(path);
    }

    @Override
    public OutputStream fileWrite(String path, boolean append) throws IOException {
        FileSystemStorage fs = fs();
        if (append && fs.exists(path)) {
            long length = fs.getLength(path);
            if (length > Integer.MAX_VALUE) {
                // The storage API takes the offset as an int. Narrowed, it
                // wraps, and the write would land inside the file.
                throw new IOException(path + " (Too large to append to)");
            }
            return fs.openOutputStream(path, (int) length);
        }
        return fs.openOutputStream(path);
    }

    @Override
    public boolean fileMkdir(String path) {
        FileSystemStorage fs = fs();
        if (fs.exists(path)) {
            return false;
        }
        fs.mkdir(path);
        return fs.exists(path) && fs.isDirectory(path);
    }

    @Override
    public boolean fileDelete(String path) {
        FileSystemStorage fs = fs();
        if (!fs.exists(path)) {
            return false;
        }
        fs.delete(path);
        return !fs.exists(path);
    }

    /// Renames within one directory, which is all `FileSystemStorage` can do;
    /// a move to another directory answers false and the caller copies.
    @Override
    public boolean fileRename(String from, String to) {
        FileSystemStorage fs = fs();
        int fromSlash = lastSeparator(from);
        int toSlash = lastSeparator(to);
        if (fromSlash < 0 || toSlash < 0 || fs.exists(to) || !fs.exists(from)
                || !from.substring(0, fromSlash).equals(to.substring(0, toSlash))) {
            return false;
        }
        fs.rename(from, to.substring(toSlash + 1));
        return fs.exists(to) && !fs.exists(from);
    }

    private static int lastSeparator(String path) {
        int slash = path.lastIndexOf('/');
        char sep = fs().getFileSystemSeparator();
        return sep == '/' ? slash : Math.max(slash, path.lastIndexOf(sep));
    }

    @Override
    public String appHome() {
        return fs().getAppHomePath();
    }

    @Override
    public String cachesDir() {
        FileSystemStorage fs = fs();
        return fs.hasCachesDir() ? fs.getCachesDir() : null;
    }

    @Override
    public String[] fileRoots() {
        return fs().getRoots();
    }

    @Override
    public long fileTotalSpace(String path) {
        String root = rootOf(path);
        return root == null ? 0L : fs().getRootSizeBytes(root);
    }

    @Override
    public long fileUsableSpace(String path) {
        String root = rootOf(path);
        return root == null ? 0L : fs().getRootAvailableSpace(root);
    }

    /// The longest root `path` lies under, else the first root, else null.
    private static String rootOf(String path) {
        String[] roots = fs().getRoots();
        if (roots == null || roots.length == 0) {
            return null;
        }
        String best = null;
        for (String r : roots) {
            if (r != null && path.startsWith(r) && (best == null || r.length() > best.length())) {
                best = r;
            }
        }
        return best != null ? best : roots[0];
    }

    @Override
    public String preference(String key) {
        return Preferences.get(key, (String) null);
    }

    @Override
    public void setPreference(String key, String value) {
        Preferences.set(key, value);
    }

    @Override
    public void deletePreference(String key) {
        Preferences.delete(key);
    }

    @Override
    public InputStream openUrl(String url) throws IOException {
        ConnectionRequest request = new ConnectionRequest();
        request.setUrl(url);
        request.setPost(false);
        // Failures reach the caller as exceptions, not the user as the
        // default error dialog.
        request.setFailSilently(true);
        NetworkManager.getInstance().addToQueueAndWait(request);
        int code = request.getResponseCode();
        if (code == 404 || code == 410) {
            throw new FileNotFoundException(url);
        }
        if (code >= 400) {
            throw new IOException("Server returned HTTP response code: " + code + " for URL: " + url);
        }
        byte[] data = request.getResponseData();
        if (data == null) {
            throw new IOException("No response from " + url);
        }
        return new ByteArrayInputStream(data);
    }

    /// The levels of [JdkHost] are those of `Log`.
    @Override
    public void log(int level, String message, Throwable thrown) {
        if (message != null) {
            Log.p(message, Math.max(Log.DEBUG, Math.min(Log.ERROR, level)));
        }
        if (thrown != null) {
            Log.e(thrown);
        }
    }

    /// `os.name` is the platform name; anything else is the display property
    /// of that name.
    @Override
    public String property(String key) {
        if (!Display.isInitialized()) {
            return null;
        }
        if ("os.name".equals(key)) {
            return Display.getInstance().getPlatformName();
        }
        return Display.getInstance().getProperty(key, null);
    }

    /// Closes the application the way the platform does; the status has
    /// nowhere to go.
    @Override
    public void exit(int status) {
        if (Display.isInitialized()) {
            Display.getInstance().exitApplication();
        }
    }

    @Override
    public boolean isUiThread() {
        return Display.isInitialized() && Display.getInstance().isEdt();
    }

    /// On the event dispatch thread the task runs through `invokeAndBlock`,
    /// so the UI keeps serving events while it blocks.
    @Override
    public void blockUi(Runnable task) {
        if (isUiThread()) {
            Display.getInstance().invokeAndBlock(task);
        } else {
            task.run();
        }
    }

    @Override
    public InputStream resource(String name) {
        String absolute = name.startsWith("/") ? name : "/" + name;
        if (Display.isInitialized()) {
            return Display.getInstance().getResourceAsStream(CoreJdkHost.class, absolute);
        }
        return CoreJdkHost.class.getResourceAsStream(absolute);
    }

    /// Derived from what `L10NManager` formats, since it exposes the
    /// separators only through formatting.
    @Override
    public String[] localeNumberSymbols() {
        if (!Display.isInitialized()) {
            return null;
        }
        L10NManager l10n = L10NManager.getInstance();
        char decimal = firstSeparator(l10n.format(1.5, 1));
        char grouping = firstSeparator(l10n.format(1234567.0, 0));
        return new String[] {
            decimal == 0 ? null : String.valueOf(decimal),
            grouping == 0 || grouping == decimal ? null : String.valueOf(grouping),
            emptyToNull(l10n.getCurrencySymbol())
        };
    }

    /// `null` for a null or empty string. A port's `L10NManager` may answer
    /// either when it knows no currency symbol.
    private static String emptyToNull(String s) {
        return s == null || s.length() == 0 ? null : s;
    }

    /// The first character of `text` that sits between two ASCII digits, or 0.
    private static char firstSeparator(String text) {
        if (text == null) {
            return 0;
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < '0' || c > '9') {
                boolean between = i > 0 && i + 1 < text.length()
                        && text.charAt(i + 1) >= '0' && text.charAt(i + 1) <= '9';
                return between ? c : 0;
            }
        }
        return 0;
    }

    @Override
    public void randomBytes(byte[] out) {
        com.codename1.security.SecureRandom.fill(out);
    }

    @Override
    public Digest digest(String algorithm) {
        if (!Hash.MD5.equalsIgnoreCase(algorithm) && !Hash.SHA1.equalsIgnoreCase(algorithm)
                && !Hash.SHA224.equalsIgnoreCase(algorithm) && !Hash.SHA256.equalsIgnoreCase(algorithm)
                && !Hash.SHA384.equalsIgnoreCase(algorithm) && !Hash.SHA512.equalsIgnoreCase(algorithm)) {
            return null;
        }
        try {
            return new HashDigest(Hash.create(algorithm));
        } catch (com.codename1.security.CryptoException e) {
            // A port without this digest; the class library reports the
            // algorithm as unavailable.
            return null;
        }
    }

    /// A core `Hash` as a [JdkHost.Digest].
    private static final class HashDigest implements Digest {
        private final Hash hash;

        HashDigest(Hash hash) {
            this.hash = hash;
        }

        public void update(byte[] data, int offset, int length) {
            hash.update(data, offset, length);
        }

        public byte[] digest() {
            return hash.digest();
        }

        public void reset() {
            hash.reset();
        }

        public int length() {
            return hash.digestLength();
        }
    }
}
