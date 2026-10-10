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
package com.codename1.maven;

import com.codename1.build.BuildExecutionException;
import com.codename1.build.BuildFailureException;
import com.codename1.build.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/// Puts the browser build of an application where its server serves it from.
///
/// A project with a backend module usually wants one address that is both the
/// API and the application: a person opens it and is using the app, with
/// nothing installed. The JavaScript build already produces everything a
/// browser needs as one bundle, and the server already knows how to serve a
/// directory; this is the step between them, shared by the Maven
/// `cn1:backend-webapp` goal and anything else that stages a bundle.
///
/// Three things happen here, and each is here because doing it by hand goes
/// wrong quietly:
///
/// - **The bundle is found.** It is whichever module builds for the browser --
///   a `javascript` module, or `common` standing in for one -- and its name
///   carries the version, so a script that hard-codes it breaks at the next
///   release.
/// - **It is unpacked with its names checked.** An archive entry is a path
///   somebody else wrote. One that climbs out of the directory is refused
///   rather than followed.
/// - **Text is compressed once, here.** The translated application is several
///   megabytes of JavaScript that shrinks to about a fifth. The server sends a
///   file straight from the page cache without reading it, which is what makes
///   it fast and also what stops it compressing on the way out -- so the
///   compressed copy is written beside the file now, and the server picks it
///   for a browser that accepts it.
public final class BackendWebApp {

    /// The file whose presence makes a directory an application.
    public static final String INDEX = "index.html";

    /// The setting the server reads its application's directory from.
    public static final String ROOT_PROPERTY = "cn1.webapp.root";

    /// Extensions worth a compressed copy: text, and the formats that are text
    /// in all but name. Images, fonts in a compressed container and archives
    /// are already as small as gzip will make them.
    private static final Set<String> COMPRESSIBLE = new HashSet<String>(Arrays.asList(
            "html", "htm", "css", "js", "mjs", "json", "svg", "txt", "xml", "map",
            "wasm", "ttf", "otf", "res", "webmanifest"));

    /// Below this a compressed copy saves less than the header that announces it.
    private static final long SMALLEST_WORTH_COMPRESSING = 1024;

    private final Log log;

    public BackendWebApp(Log log) {
        this.log = log;
    }

    /// The browser bundle the application at `appRoot` last built, or null when
    /// it has built none.
    ///
    /// Looked for in the `javascript` module and then in `common`, which builds
    /// for the browser itself in a project that has no such module. Among
    /// several archives the newest that holds an `index.html` wins: `target`
    /// also collects the source archives sent to a build server, and those are
    /// not applications.
    public static File locateBundle(File appRoot) {
        for (String module : new String[] {"javascript", "common"}) {
            File best = locateBundleIn(new File(appRoot, module + "/target"));
            if (best != null) {
                return best;
            }
        }
        return null;
    }

    /// The browser bundle in `buildDirectory`, the directory a build writes its
    /// results to, or null when it holds none: the newest archive there that
    /// holds an `index.html`.
    ///
    /// This is the whole answer for a project whose one build directory is
    /// every platform's, as a Gradle project's is.
    public static File locateBundleIn(File buildDirectory) {
        File[] found = buildDirectory == null ? null : buildDirectory.listFiles();
        if (found == null) {
            return null;
        }
        File best = null;
        for (File candidate : found) {
            if (!candidate.isFile() || !endsWithIgnoreCase(candidate.getName(), ".zip")) {
                continue;
            }
            if (!holdsIndex(candidate)) {
                continue;
            }
            if (best == null || candidate.lastModified() > best.lastModified()) {
                best = candidate;
            }
        }
        return best;
    }

    /// Whether the application's settings already say how the browser build
    /// reaches other origins.
    ///
    /// The JavaScript build routes cross-origin requests through a proxy
    /// servlet it expects to find beside the page. A backend is not a servlet
    /// container and has no such path, so a hosted build is made without it --
    /// unless the project has chosen for itself, in which case its choice
    /// stands.
    public static boolean choosesProxy(File settings) throws BuildExecutionException {
        if (settings == null || !settings.isFile()) {
            return false;
        }
        Properties declared = new Properties();
        try {
            InputStream in = new FileInputStream(settings);
            try {
                declared.load(in);
            } finally {
                in.close();
            }
        } catch (IOException err) {
            throw new BuildExecutionException("Could not read " + settings, err);
        }
        return declared.getProperty("codename1.arg.javascript.inject_proxy") != null
                || declared.getProperty("codename1.arg.javascript.proxy.url") != null;
    }

    /// Whether the server module already says where its application is, so the
    /// build must not answer for it.
    ///
    /// A value passed as a system property outranks the module's own files, so
    /// supplying one unasked would silently override a project that had
    /// configured this itself. The three places a project can say so are the
    /// JVM arguments, the environment and the settings files in the module.
    public static boolean rootIsConfigured(File moduleDir, Map<String, String> environment, String jvmArgs)
            throws BuildExecutionException {
        if (jvmArgs != null && jvmArgs.indexOf("-D" + ROOT_PROPERTY + "=") >= 0) {
            return true;
        }
        if (environment != null && environment.get("CN1_WEBAPP_ROOT") != null) {
            return true;
        }
        File[] files = moduleDir.listFiles();
        if (files == null) {
            return false;
        }
        for (File file : files) {
            String name = file.getName();
            if (!file.isFile() || !name.startsWith("application") || !name.endsWith(".properties")) {
                continue;
            }
            Properties declared = new Properties();
            try {
                InputStream in = new FileInputStream(file);
                try {
                    declared.load(in);
                } finally {
                    in.close();
                }
            } catch (IOException err) {
                throw new BuildExecutionException("Could not read " + file, err);
            }
            if (declared.getProperty(ROOT_PROPERTY) != null) {
                return true;
            }
        }
        return false;
    }

    /// Replaces `output` with the application in `bundle`, which is the archive
    /// a browser build produces or a directory already unpacked from one.
    ///
    /// @return the number of files staged, compressed copies not counted
    public int stage(File bundle, File output) throws BuildFailureException, BuildExecutionException {
        if (bundle == null || !bundle.exists()) {
            throw new BuildFailureException("There is no browser build at " + bundle);
        }
        try {
            File canonicalOutput = output.getCanonicalFile();
            if (bundle.isDirectory() && isInside(canonicalOutput, bundle.getCanonicalFile())) {
                throw new BuildFailureException("The browser build " + bundle + " contains the "
                        + "directory it would be staged into, " + output);
            }
            delete(canonicalOutput);
            if (!canonicalOutput.mkdirs() && !canonicalOutput.isDirectory()) {
                throw new BuildExecutionException("Could not create " + canonicalOutput);
            }
            int count = bundle.isDirectory()
                    ? copyTree(bundle, canonicalOutput)
                    : unpack(bundle, canonicalOutput);
            if (!new File(canonicalOutput, INDEX).isFile()) {
                delete(canonicalOutput);
                throw new BuildFailureException(bundle + " holds no " + INDEX
                        + ", so it is not a browser build of an application");
            }
            long[] saved = new long[2];
            compress(canonicalOutput, saved);
            log.info("Staged " + count + " files of the web app in " + canonicalOutput
                    + (saved[0] > 0 ? " (" + saved[0] / 1024 + " KB of text, "
                            + saved[1] / 1024 + " KB compressed)" : ""));
            return count;
        } catch (IOException err) {
            throw new BuildExecutionException("Could not stage " + bundle + " into " + output, err);
        }
    }

    /// Copies a staged application to `destination`, for a server packaged
    /// somewhere other than beside it.
    public int copy(File staged, File destination) throws BuildExecutionException {
        try {
            File target = destination.getCanonicalFile();
            if (target.equals(staged.getCanonicalFile())) {
                return 0;
            }
            delete(target);
            if (!target.mkdirs() && !target.isDirectory()) {
                throw new BuildExecutionException("Could not create " + target);
            }
            return copyTree(staged, target);
        } catch (IOException err) {
            throw new BuildExecutionException("Could not copy " + staged + " to " + destination, err);
        }
    }

    private int unpack(File archive, File output) throws IOException, BuildFailureException {
        ZipFile zip = new ZipFile(archive);
        try {
            String strip = wrapperDirectory(zip);
            String root = output.getPath() + File.separator;
            int count = 0;
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName().replace('\\', '/');
                if (strip != null) {
                    if (!name.startsWith(strip)) {
                        continue;
                    }
                    name = name.substring(strip.length());
                }
                if (name.length() == 0) {
                    continue;
                }
                File target = new File(output, name).getCanonicalFile();
                // The entry's name was written by whoever made the archive. One
                // that resolves outside the directory is the whole of the
                // "zip slip" attack, so it stops the build rather than being
                // skipped: an archive that tries it is not one to serve.
                if (!target.getPath().startsWith(root)) {
                    throw new BuildFailureException(archive + " holds the entry " + entry.getName()
                            + ", which would be written outside " + output);
                }
                if (entry.isDirectory()) {
                    if (!target.mkdirs() && !target.isDirectory()) {
                        throw new IOException("Could not create " + target);
                    }
                    continue;
                }
                File parent = target.getParentFile();
                if (!parent.mkdirs() && !parent.isDirectory()) {
                    throw new IOException("Could not create " + parent);
                }
                InputStream in = zip.getInputStream(entry);
                try {
                    write(in, target);
                } finally {
                    in.close();
                }
                count++;
            }
            return count;
        } finally {
            zip.close();
        }
    }

    /// The single directory everything in `zip` sits under, with its trailing
    /// slash, or null when the application is at the top of the archive.
    private static String wrapperDirectory(ZipFile zip) {
        if (zip.getEntry(INDEX) != null) {
            return null;
        }
        String wrapper = null;
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            String name = entries.nextElement().getName().replace('\\', '/');
            int slash = name.indexOf('/');
            if (slash <= 0) {
                return null;
            }
            String top = name.substring(0, slash + 1);
            if (wrapper == null) {
                wrapper = top;
            } else if (!wrapper.equals(top)) {
                return null;
            }
        }
        return wrapper;
    }

    private static boolean holdsIndex(File archive) {
        try {
            ZipFile zip = new ZipFile(archive);
            try {
                if (zip.getEntry(INDEX) != null) {
                    return true;
                }
                String wrapper = wrapperDirectory(zip);
                return wrapper != null && zip.getEntry(wrapper + INDEX) != null;
            } finally {
                zip.close();
            }
        } catch (IOException notAnArchive) {
            return false;
        }
    }

    private int copyTree(File from, File to) throws IOException {
        File[] children = from.listFiles();
        if (children == null) {
            return 0;
        }
        int count = 0;
        for (File child : children) {
            File target = new File(to, child.getName());
            if (child.isDirectory()) {
                if (!target.mkdirs() && !target.isDirectory()) {
                    throw new IOException("Could not create " + target);
                }
                count += copyTree(child, target);
            } else {
                InputStream in = new FileInputStream(child);
                try {
                    write(in, target);
                } finally {
                    in.close();
                }
                // Kept, so the server's validators do not change for a file
                // that did not.
                if (!target.setLastModified(child.lastModified())) {
                    log.debug("Could not keep the modification time of " + target);
                }
                if (!child.getName().endsWith(".gz")) {
                    count++;
                }
            }
        }
        return count;
    }

    /// Writes a `.gz` beside every file under `dir` that is text and shrinks.
    private void compress(File dir, long[] saved) throws IOException {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        List<File> files = new ArrayList<File>(Arrays.asList(children));
        for (File child : files) {
            if (child.isDirectory()) {
                compress(child, saved);
                continue;
            }
            String name = child.getName();
            int dot = name.lastIndexOf('.');
            if (dot < 0 || child.length() < SMALLEST_WORTH_COMPRESSING
                    || !COMPRESSIBLE.contains(asciiLower(name.substring(dot + 1)))) {
                continue;
            }
            File packed = new File(dir, name + ".gz");
            InputStream in = new FileInputStream(child);
            try {
                OutputStream out = new GZIPOutputStream(new FileOutputStream(packed));
                try {
                    pump(in, out);
                } finally {
                    out.close();
                }
            } finally {
                in.close();
            }
            // A copy that saves under a tenth is not worth a second file and a
            // decompression in every browser.
            if (packed.length() * 10 > child.length() * 9) {
                if (!packed.delete()) {
                    throw new IOException("Could not remove " + packed);
                }
                continue;
            }
            saved[0] += child.length();
            saved[1] += packed.length();
        }
    }

    private static void write(InputStream in, File target) throws IOException {
        OutputStream out = new FileOutputStream(target);
        try {
            pump(in, out);
        } finally {
            out.close();
        }
    }

    private static void pump(InputStream in, OutputStream out) throws IOException {
        byte[] chunk = new byte[65536];
        int read = in.read(chunk);
        while (read >= 0) {
            out.write(chunk, 0, read);
            read = in.read(chunk);
        }
    }

    private static void delete(File file) throws IOException {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                delete(child);
            }
        }
        if (file.exists() && !file.delete()) {
            throw new IOException("Could not remove " + file);
        }
    }

    private static boolean isInside(File file, File dir) {
        return (file.getPath() + File.separator).startsWith(dir.getPath() + File.separator);
    }

    private static boolean endsWithIgnoreCase(String value, String suffix) {
        return value.length() >= suffix.length()
                && value.regionMatches(true, value.length() - suffix.length(), suffix, 0, suffix.length());
    }

    /// File extensions are ASCII; folding them with the default locale would
    /// turn `JS` into something else on a machine set to Turkish.
    private static String asciiLower(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            out.append(c >= 'A' && c <= 'Z' ? (char) (c + 32) : c);
        }
        return out.toString();
    }
}
