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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// Finds the stylesheets a CSS directory pulls in with `@import` from
/// outside itself, and the images and fonts named from outside it.
///
/// A build decides whether `theme.res` is stale from the files under
/// `src/main/css`. A stylesheet there may import one that lives elsewhere --
/// `@import "../../../../shared/base.css"` -- and an edit to that file changes
/// the theme without touching anything in the directory. Both build plugins
/// add what this class finds to the inputs they compare, so the edit
/// recompiles instead of leaving the old theme in place.
///
/// This only has to name files, not compile them, so it reads `@import`
/// with a pattern and follows the chain. Anything it cannot read or resolve
/// is skipped: the compiler is what reports a broken import.
public final class CssImportDependencies {

    private static final Pattern COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    private static final Pattern IMPORT = Pattern.compile(
            "@import\\s+(?:url\\(\\s*)?(?:\"([^\"]*)\"|'([^']*)'|([^\"')\\s;]+))", Pattern.CASE_INSENSITIVE);

    private static final Pattern URL = Pattern.compile(
            "url\\(\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\"')]+?))\\s*\\)", Pattern.CASE_INSENSITIVE);

    private CssImportDependencies() {
    }

    /// Every file imported, directly or through another import, by a
    /// stylesheet under `cssDir`, and every file one of those stylesheets
    /// names in a `url()`, that is not itself under `cssDir`. A file that is
    /// named but missing is included.
    public static Set<File> outside(File cssDir) {
        Set<File> out = new LinkedHashSet<File>();
        if (cssDir == null || !cssDir.isDirectory()) {
            return out;
        }
        File root;
        try {
            root = cssDir.getCanonicalFile();
        } catch (IOException ex) {
            return out;
        }
        Deque<File> pending = new ArrayDeque<File>();
        collectStylesheets(root, pending);
        Set<File> seen = new LinkedHashSet<File>(pending);
        while (!pending.isEmpty()) {
            File css = pending.removeFirst();
            String text;
            try {
                text = new String(Files.readAllBytes(css.toPath()), StandardCharsets.UTF_8);
            } catch (IOException ex) {
                continue;
            }
            text = COMMENT.matcher(text).replaceAll("");
            // An image or a font the stylesheet names from outside the
            // directory is compiled into the theme just as an import is.
            Matcher asset = URL.matcher(text);
            while (asset.find()) {
                // A quoted name may hold a parenthesis of its own.
                String name = asset.group(1) != null ? asset.group(1)
                        : asset.group(2) != null ? asset.group(2) : asset.group(3);
                File file = resolve(css, name);
                if (file != null && !file.toPath().startsWith(root.toPath())) {
                    out.add(file);
                }
            }
            Matcher m = IMPORT.matcher(text);
            while (m.find()) {
                // A quoted name is taken whole, spaces included; a bare one
                // ends at the first space.
                String target = m.group(1) != null ? m.group(1) : m.group(2) != null ? m.group(2) : m.group(3);
                File file = resolve(css, target);
                if (file == null || !seen.add(file)) {
                    continue;
                }
                if (!file.toPath().startsWith(root.toPath())) {
                    out.add(file);
                }
                if (file.isFile()) {
                    pending.addLast(file);
                }
            }
        }
        return out;
    }

    /// The file `target` names from the stylesheet `css`, whether or not it
    /// exists, or null when it is remote or a `data:` URI.
    ///
    /// A file that is not there is still answered. Deleting something a
    /// stylesheet refers to from outside the directory changes no
    /// modification time anywhere, so the only way a build can notice is by
    /// being told the reference is now broken.
    private static File resolve(File css, String target) {
        if (target.length() == 0 || target.charAt(0) == '#'
                || (target.contains(":") && !new File(target).isAbsolute())) {
            return null;
        }
        File file = new File(target);
        if (!file.isAbsolute()) {
            file = new File(css.getParentFile(), target);
        }
        try {
            return file.getCanonicalFile();
        } catch (IOException ex) {
            return null;
        }
    }

    /// The newest modification time among [#outside(File)], or 0 when there
    /// are none.
    ///
    /// When one of them does not exist the answer is the largest time there
    /// is, which makes any existing output stale: the compile then runs and
    /// reports the missing file, instead of an old theme being kept.
    public static long lastModified(File cssDir) {
        long newest = 0;
        for (File f : outside(cssDir)) {
            if (!f.isFile()) {
                return Long.MAX_VALUE;
            }
            newest = Math.max(newest, f.lastModified());
        }
        return newest;
    }

    private static void collectStylesheets(File dir, Deque<File> into) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                collectStylesheets(child, into);
            } else if (child.getName().regionMatches(true, Math.max(0, child.getName().length() - 4), ".css", 0, 4)) {
                into.addLast(child);
            }
        }
    }
}
