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
            "@import\\s+(?:url\\(\\s*)?[\"']?([^\"')\\s;]+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern URL = Pattern.compile(
            "url\\(\\s*[\"']?([^\"')]+?)[\"']?\\s*\\)", Pattern.CASE_INSENSITIVE);

    private CssImportDependencies() {
    }

    /// Every file imported, directly or through another import, by a
    /// stylesheet under `cssDir`, and every file one of those stylesheets
    /// names in a `url()`, that is not itself under `cssDir`.
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
                File file = resolve(css, asset.group(1));
                if (file != null && !file.toPath().startsWith(root.toPath())) {
                    out.add(file);
                }
            }
            Matcher m = IMPORT.matcher(text);
            while (m.find()) {
                File file = resolve(css, m.group(1));
                if (file == null || !seen.add(file)) {
                    continue;
                }
                pending.addLast(file);
                if (!file.toPath().startsWith(root.toPath())) {
                    out.add(file);
                }
            }
        }
        return out;
    }

    /// The existing file `target` names from the stylesheet `css`, or null
    /// when it is remote, a `data:` URI, or not there.
    private static File resolve(File css, String target) {
        if (target.contains(":") && !new File(target).isAbsolute()) {
            return null;
        }
        File file = new File(target);
        if (!file.isAbsolute()) {
            file = new File(css.getParentFile(), target);
        }
        try {
            file = file.getCanonicalFile();
        } catch (IOException ex) {
            return null;
        }
        return file.isFile() ? file : null;
    }

    /// The newest modification time among [#outside(File)], or 0 when there
    /// are none.
    public static long lastModified(File cssDir) {
        long newest = 0;
        for (File f : outside(cssDir)) {
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
