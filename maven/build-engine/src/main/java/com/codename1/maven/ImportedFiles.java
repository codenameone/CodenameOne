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

import com.codename1.build.Log;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/// The files an import copied into an application, and the record that lets
/// the next import of the same project tell its own copies from the
/// developer's work.
///
/// An import leaves one `<sha-256> <path>` line per file it copied in a record
/// file inside the directory it copied into. Importing again then updates
/// only what is still exactly as the earlier import wrote it: a file the
/// developer changed since is kept, a file that left the source project is
/// removed, and a file the developer added was never in the record at all.
/// Both project importers ([AndroidProjectImporter], [DesktopProjectImporter])
/// copy through this, so they behave the same on a repeated import.
final class ImportedFiles {

    private final File target;
    private final String recordName;
    private final String sourceName;
    private final Log log;
    private final Map<String, String> previous;
    private final Map<String, String> imported = new TreeMap<String, String>();
    private final List<String> skipNames = new ArrayList<String>();
    private final List<String> skipped = new ArrayList<String>();
    private String deferred;

    /// `target` is the directory copied into, `recordName` the record file's
    /// name inside it, and `sourceName` what a message calls the project
    /// imported from ("the Android project").
    ImportedFiles(File target, String recordName, String sourceName, Log log) throws IOException {
        this.target = target;
        this.recordName = recordName;
        this.sourceName = sourceName;
        this.log = log;
        this.previous = read(new File(target, recordName));
    }

    /// Names a path whose hash [#copy] leaves unrecorded, because the caller
    /// rewrites the file after copying it and records it itself ([#record]).
    /// Such a file is always part of an import, so it is never stale either.
    ImportedFiles deferring(String path) {
        this.deferred = path;
        return this;
    }

    /// Files with this name are not copied; each one met is listed in
    /// [#skipped].
    ImportedFiles skipping(String fileName) {
        skipNames.add(fileName);
        return this;
    }

    /// The paths, relative to the target, of the files [#skipping] left out.
    List<String> skipped() {
        return skipped;
    }

    /// Whether this import recorded `path`, by copying it or by keeping the
    /// developer's edited version of it.
    boolean has(String path) {
        return imported.containsKey(path);
    }

    /// Records `path` as holding the bytes it has now.
    void record(String path) throws IOException {
        imported.put(path, sha256(new File(target, path.replace('/', File.separatorChar))));
    }

    /// Copies `src` to `dest`, recording each copied file's path (relative to
    /// the target, `/` separated) and content hash. Answers the number of
    /// files copied.
    ///
    /// A file an earlier import wrote and the developer changed since (its
    /// bytes no longer match the earlier record) is kept, as
    /// [#removeStale] keeps one: it is recorded with its earlier hash, so the
    /// next import still recognizes the edit, and deleting it lets an import
    /// copy it afresh.
    int copy(File src, File dest, String path) throws IOException {
        if (src.isDirectory()) {
            int n = 0;
            File[] files = src.listFiles();
            if (files != null) {
                java.util.Arrays.sort(files);
                for (File f : files) {
                    n += copy(f, new File(dest, f.getName()), path + "/" + f.getName());
                }
            }
            return n;
        }
        if (skipNames.contains(src.getName())) {
            skipped.add(path);
            return 0;
        }
        String recorded = previous.get(path);
        if (recorded != null && dest.isFile() && !recorded.equals(sha256(dest))
                && !java.util.Arrays.equals(Files.readAllBytes(src.toPath()), Files.readAllBytes(dest.toPath()))) {
            log.warn("Kept " + dest + ": it was changed after the earlier import, so " + sourceName + "'s copy "
                    + "was not imported; delete it and import again to take that copy");
            imported.put(path, recorded);
            return 0;
        }
        File parent = dest.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Cannot create " + parent);
        }
        Files.copy(src.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        if (!path.equals(deferred)) {
            imported.put(path, sha256(dest));
        }
        return 1;
    }

    /// Removes what an earlier import copied and this one did not, so a file
    /// deleted or renamed upstream does not stay behind to be compiled or
    /// packaged. Only a file listed in the earlier import's record is a
    /// candidate, and only while it still holds exactly the bytes that import
    /// wrote: a file the developer added, or edited after importing it, is
    /// kept. Directories the removal empties go too.
    void removeStale() throws IOException {
        for (Map.Entry<String, String> e : previous.entrySet()) {
            String path = e.getKey();
            if (imported.containsKey(path) || path.equals(deferred) || path.contains("..")) {
                continue;
            }
            File f = new File(target, path.replace('/', File.separatorChar));
            if (!f.isFile()) {
                continue;
            }
            if (!e.getValue().equals(sha256(f))) {
                log.warn("Kept " + f + ": it is no longer in " + sourceName + ", but it was changed after "
                        + "the earlier import");
                continue;
            }
            Files.delete(f.toPath());
            log.info("Removed " + f + ": it is no longer in " + sourceName);
            File dir = f.getParentFile();
            while (dir != null && !dir.equals(target)) {
                String[] left = dir.list();
                if (left == null || left.length > 0 || !dir.delete()) {
                    break;
                }
                dir = dir.getParentFile();
            }
        }
    }

    /// Writes the record of this import.
    void write() throws IOException {
        StringBuilder b = new StringBuilder();
        for (Map.Entry<String, String> e : imported.entrySet()) {
            b.append(e.getValue()).append(' ').append(e.getKey()).append('\n');
        }
        if (!target.isDirectory() && !target.mkdirs()) {
            throw new IOException("Cannot create " + target);
        }
        Files.write(new File(target, recordName).toPath(), b.toString().getBytes(Charset.forName("UTF-8")));
    }

    private static Map<String, String> read(File f) throws IOException {
        Map<String, String> out = new TreeMap<String, String>();
        if (!f.isFile()) {
            return out;
        }
        for (String line : Files.readAllLines(f.toPath(), Charset.forName("UTF-8"))) {
            int sp = line.indexOf(' ');
            if (sp > 0 && sp < line.length() - 1) {
                out.put(line.substring(sp + 1), line.substring(0, sp));
            }
        }
        return out;
    }

    static String sha256(File f) throws IOException {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(f.toPath()));
            StringBuilder b = new StringBuilder(d.length * 2);
            for (byte x : d) {
                b.append(Character.forDigit((x >> 4) & 0xf, 16)).append(Character.forDigit(x & 0xf, 16));
            }
            return b.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 is unavailable", e);
        }
    }

    /// Whether any file under `dir` is Kotlin source.
    static boolean hasKotlin(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return false;
        }
        for (File f : files) {
            if (f.isDirectory() ? hasKotlin(f) : f.getName().endsWith(".kt")) {
                return true;
            }
        }
        return false;
    }
}
