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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/// The services the Java class library asks of the platform it runs on.
///
/// Internal API. The `java.*` classes of the ParparVM runtime (`vm/JavaAPI`)
/// need a file system, preferences, the network, a log and an event thread,
/// and on a device those belong to Codename One, which the class library
/// cannot name: it is translated alone for the translator itself, the backend
/// and the VM tests. So the class library calls this class, and whoever runs
/// it installs the implementation that fits:
///
/// - a Codename One application gets `com.codename1.impl.CoreJdkHost`, which
///   delegates to `FileSystemStorage`, `Preferences`, `NetworkManager`, `Log`
///   and `Display`. The ParparVM ports install it from their `init`.
/// - a program translated without Codename One (the self-hosted translator,
///   the backend, a VM test) gets `PosixJdkHost`, which reaches the POSIX file
///   natives through `java.io.File`.
/// - anything else gets the defaults below, which are plain Java and touch no
///   native method, so an application target that never installs a host
///   cannot link the file natives through this class.
///
/// On Java SE and Android the real JDK runs and nothing calls this class.
/// Copies of it ship in those ports only so that `CoreJdkHost`, which is part
/// of the core sources, always has its superclass.
///
/// The file is kept byte for byte identical in `vm/JavaAPI`, `Ports/CLDC11`,
/// `Ports/JavaSE` and `Ports/Android`; `CldcJavaApiParityTest` holds them
/// to that. It may use only API all four class libraries have.
///
/// Paths are in the host's own form: absolute, as [#appHome] answers them.
/// Turning a relative path into one is the caller's job.
public abstract class JdkHost {
    /// [#log] level of a debug message.
    public static final int LOG_DEBUG = 1;
    /// [#log] level of an informational message.
    public static final int LOG_INFO = 2;
    /// [#log] level of a warning.
    public static final int LOG_WARNING = 3;
    /// [#log] level of an error.
    public static final int LOG_ERROR = 4;

    private static final JdkHost DEFAULTS = new Defaults();
    private static JdkHost current;
    private static Registry registry;
    private static Map<String, String> memoryPreferences;

    /// Subclasses are installed with [#install].
    protected JdkHost() {
    }

    /// The installed host, or the plain Java defaults when none was installed.
    public static JdkHost get() {
        JdkHost h = current;
        return h == null ? DEFAULTS : h;
    }

    /// Makes `host` the one [#get] answers; null restores the defaults.
    /// Called once, while the program starts and before it starts a thread
    /// of its own.
    public static void install(JdkHost host) {
        current = host;
    }

    /// Makes `r` the registry [#providers] and [#newInstance] read. An
    /// application's generated start-up code installs it.
    public static void setRegistry(Registry r) {
        registry = r;
    }

    /// The registry [#setRegistry] installed, or null.
    public static Registry getRegistry() {
        return registry;
    }

    // ------------------------------------------------------------------
    // Files

    /// What is known of `path`, or null when nothing is there.
    public FileStat fileStat(String path) {
        return null;
    }

    /// The names of the entries of the directory `path`, or null when it is
    /// not a directory.
    public String[] fileList(String path) throws IOException {
        return null;
    }

    /// Opens the file `path` for reading.
    public InputStream fileRead(String path) throws IOException {
        throw new IOException("No file system: " + path);
    }

    /// Opens the file `path` for writing, at its end when `append` is true
    /// and from empty otherwise, creating it when it is not there.
    public OutputStream fileWrite(String path, boolean append) throws IOException {
        throw new IOException("No file system: " + path);
    }

    /// Creates the directory `path`; false when it was not created, also
    /// when something was there already.
    public boolean fileMkdir(String path) {
        return false;
    }

    /// Deletes the file or empty directory `path`; false when it is still
    /// there or never was.
    public boolean fileDelete(String path) {
        return false;
    }

    /// Moves `from` to `to`, which must not exist. False when it did not
    /// happen; a host may refuse a move to another directory, and the
    /// caller then copies.
    public boolean fileRename(String from, String to) {
        return false;
    }

    /// The application's home directory, where a relative path resolves.
    public String appHome() {
        return "/";
    }

    /// A directory for disposable files, or null when the platform has no
    /// separate one and [#appHome] serves.
    public String cachesDir() {
        return null;
    }

    /// The roots of the file system.
    public String[] fileRoots() {
        return new String[] {appHome()};
    }

    /// The size in bytes of the volume holding `path`, or 0 when unknown.
    public long fileTotalSpace(String path) {
        return 0L;
    }

    /// The bytes still free for this program on the volume holding `path`,
    /// or 0 when unknown.
    public long fileUsableSpace(String path) {
        return 0L;
    }

    /// The bytes free on the volume holding `path`, counting what only a
    /// privileged user may use; [#fileUsableSpace] unless the host knows
    /// better.
    public long fileFreeSpace(String path) {
        return fileUsableSpace(path);
    }

    /// [#fileTest]: something is at the path.
    public static final int FILE_EXISTS = 1;
    /// [#fileTest]: the path is a directory.
    public static final int FILE_DIRECTORY = 2;
    /// [#fileTest]: the path is a file that is not a directory.
    public static final int FILE_REGULAR = 3;
    /// [#fileTest] and [#fileSet]: the path may be read.
    public static final int FILE_READABLE = 4;
    /// [#fileTest] and [#fileSet]: the path may be written.
    public static final int FILE_WRITABLE = 5;
    /// [#fileTest] and [#fileSet]: the path may be executed, or for a
    /// directory, entered.
    public static final int FILE_EXECUTABLE = 6;
    /// [#fileTest]: the path is hidden.
    public static final int FILE_HIDDEN = 7;

    /// Whether `path` has the property `what`, one of the `FILE_` constants.
    ///
    /// The defaults read [#fileStat]: a file system without permissions lets
    /// everything that exists be read and written, a directory be entered and
    /// nothing be run, and hides a name that starts with a dot.
    public boolean fileTest(String path, int what) {
        if (what == FILE_HIDDEN) {
            int slash = path.lastIndexOf('/');
            return path.startsWith(".", slash + 1);
        }
        FileStat st = fileStat(path);
        if (st == null) {
            return false;
        }
        if (what == FILE_DIRECTORY || what == FILE_EXECUTABLE) {
            return st.isDirectory();
        }
        if (what == FILE_REGULAR) {
            return !st.isDirectory();
        }
        return true;
    }

    /// Grants (`on`) or takes away the permission `what` on `path`, one of
    /// [#FILE_READABLE], [#FILE_WRITABLE] and [#FILE_EXECUTABLE]; false when
    /// the path is not left that way.
    ///
    /// The defaults change nothing, so they answer whether the path already is
    /// as asked, by the rules of [#fileTest].
    public boolean fileSet(String path, int what, boolean on) {
        FileStat st = fileStat(path);
        if (st == null) {
            return false;
        }
        if (what == FILE_EXECUTABLE) {
            return on == st.isDirectory();
        }
        return on;
    }

    /// The length in bytes of the file `path`; 0 for a directory or when
    /// nothing is there.
    public long fileLength(String path) {
        FileStat st = fileStat(path);
        return st == null || st.isDirectory() ? 0L : st.length();
    }

    /// When `path` was last modified, in milliseconds since the epoch; 0 when
    /// nothing is there.
    public long fileLastModified(String path) {
        FileStat st = fileStat(path);
        return st == null ? 0L : st.lastModified();
    }

    /// Creates the empty file `path` when nothing is there; false when
    /// something was.
    ///
    /// The default checks and then creates, which is not atomic: two threads
    /// racing on one path can both be answered true.
    public boolean fileCreate(String path) throws IOException {
        if (fileStat(path) != null) {
            return false;
        }
        fileWrite(path, false).close();
        return true;
    }

    /// Moves `from` over `to`, replacing the file there. False when it did not
    /// happen, which is all the defaults answer; the caller then copies.
    public boolean fileReplace(String from, String to) {
        return false;
    }

    /// The absolute `path` with symbolic links, `.` and `..` resolved, or
    /// null when the host cannot resolve links and the caller should resolve
    /// the dots by itself.
    public String fileCanonicalPath(String path) throws IOException {
        return null;
    }

    // ------------------------------------------------------------------
    // Preferences

    /// The stored preference `key`, or null. The defaults keep preferences
    /// in memory, for this run only.
    public String preference(String key) {
        return memoryPreference(key, null, false);
    }

    /// Stores `value` under `key`.
    public void setPreference(String key, String value) {
        memoryPreference(key, value, true);
    }

    /// Removes the preference `key`.
    public void deletePreference(String key) {
        memoryPreference(key, null, true);
    }

    /// Reads `key` from the in-memory preferences, or, when `write` is true,
    /// stores `value` there (null removes it). One lock: the class library
    /// runs on several threads where a host leaves this to the defaults.
    private static synchronized String memoryPreference(String key, String value, boolean write) {
        if (memoryPreferences == null) {
            memoryPreferences = new HashMap<String, String>();
        }
        if (!write) {
            return memoryPreferences.get(key);
        }
        if (value == null) {
            return memoryPreferences.remove(key);
        }
        return memoryPreferences.put(key, value);
    }

    // ------------------------------------------------------------------
    // Network, log and process

    /// Reads `url` with an HTTP GET. A missing resource (404, 410) throws
    /// `java.io.FileNotFoundException`, any other failure an `IOException`.
    public InputStream openUrl(String url) throws IOException {
        throw new IOException("No network: " + url);
    }

    /// Logs `message` and `thrown`, either of which may be null, at `level`
    /// ([#LOG_DEBUG] to [#LOG_ERROR]).
    public void log(int level, String message, Throwable thrown) {
        if (message != null) {
            System.err.println(message);
        }
        if (thrown != null) {
            thrown.printStackTrace();
        }
    }

    /// The platform's answer for the system property `key`, or null. The
    /// class library answers the properties it defines itself.
    public String property(String key) {
        return null;
    }

    /// Asks the platform to close the application, after the shutdown hooks
    /// ran; `Runtime.exit` ends the process itself if this returns. The
    /// defaults leave it to that.
    public void exit(int status) {
    }

    /// Called when the program installs a default uncaught exception
    /// handler, so that a host whose UI thread catches what its tasks throw
    /// can hand that to `Thread.getDefaultUncaughtExceptionHandler()` too.
    /// Called on every such install; the defaults do nothing.
    public void watchUncaughtExceptions() {
    }

    // ------------------------------------------------------------------
    // Threads

    /// Whether the caller is the UI thread, which must not block.
    public boolean isUiThread() {
        return false;
    }

    /// Runs `task`, which blocks, where blocking is allowed: off the UI
    /// thread while that thread keeps serving events, or right here.
    public void blockUi(Runnable task) {
        task.run();
    }

    // ------------------------------------------------------------------
    // Resources, services and locale

    /// Opens the application resource `name` (a path such as
    /// `META-INF/services/x`, without a leading slash), or null.
    public InputStream resource(String name) {
        return null;
    }

    /// The class names of the providers of `service` the application
    /// declares, from the [Registry]; empty when there is none.
    public String[] providers(String service) {
        Registry r = registry;
        String[] names = r == null ? null : r.providers(service);
        return names == null ? new String[0] : names;
    }

    /// A new instance of the application class `className`, made by the
    /// [Registry] without reflection; null when it cannot make one.
    public Object newInstance(String className) {
        Registry r = registry;
        return r == null ? null : r.newInstance(className);
    }

    /// The class of the application class `className`, as the [Registry]
    /// knows it, so that its type can be tested without creating one; null
    /// when it does not know the class.
    public Class<?> type(String className) {
        Registry r = registry;
        return r == null ? null : r.type(className);
    }

    /// The decimal separator, grouping separator and currency symbol of the
    /// device's locale, any of them null when unknown; or null when the
    /// platform cannot tell.
    public String[] localeNumberSymbols() {
        return null;
    }

    // ------------------------------------------------------------------
    // Security

    /// Fills `out` with cryptographically secure random bytes.
    public void randomBytes(byte[] out) {
        throw new UnsupportedOperationException("No secure random source");
    }

    /// A new digest for `algorithm` (`MD5`, `SHA-1`, `SHA-256` and the like),
    /// or null when the platform has none.
    public Digest digest(String algorithm) {
        return null;
    }

    /// The attributes [#fileStat] reports.
    public static final class FileStat {
        private final boolean directory;
        private final long length;
        private final long lastModified;

        /// A file or, when `directory` is true, a directory.
        public FileStat(boolean directory, long length, long lastModified) {
            this.directory = directory;
            this.length = length;
            this.lastModified = lastModified;
        }

        /// Whether it is a directory.
        public boolean isDirectory() {
            return directory;
        }

        /// The length in bytes of a file; 0 for a directory.
        public long length() {
            return length;
        }

        /// When it was last modified, in milliseconds since the epoch, or 0.
        public long lastModified() {
            return lastModified;
        }
    }

    /// A running message digest, as [#digest] makes it.
    public interface Digest {
        /// Feeds `length` bytes of `data` from `offset`.
        void update(byte[] data, int offset, int length);

        /// Finishes the digest and resets it.
        byte[] digest();

        /// Discards what was fed.
        void reset();

        /// The length of the digest in bytes.
        int length();
    }

    /// What an application declares for `java.util.ServiceLoader` and
    /// `java.util.ResourceBundle`, generated with the application. Once one
    /// is installed it is the whole answer: the class library creates no
    /// class from its name and reads no services file.
    public interface Registry {
        /// The provider class names declared for `service`, or null.
        String[] providers(String service);

        /// A new instance of `className`, or null.
        Object newInstance(String className);

        /// The class `className`, or null when the registry does not know it.
        Class<?> type(String className);
    }

    /// The host [#get] answers when none was installed.
    private static final class Defaults extends JdkHost {
    }
}
