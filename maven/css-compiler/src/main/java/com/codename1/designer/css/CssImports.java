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
package com.codename1.designer.css;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/// Resolves `@import` by textual inclusion, before the stylesheet reaches the
/// parser.
///
/// The SAC parser reports an `@import` through a callback and reads nothing
/// itself, and that callback used to be empty: every rule of the imported
/// file was missing from the theme with no error and no warning. Inlining
/// the text ahead of the parse gives the imported rules exactly the position
/// CSS assigns them (where the `@import` stood, ahead of the importing file's
/// own rules) without a second, re-entrant parse into the same theme.
///
/// A relative `url()` inside an imported file refers to something beside
/// *that* file, while the compiler resolves every `url()` against the one
/// root stylesheet. Imported text is therefore rewritten so its relative
/// URLs are expressed relative to the root file's directory.
public final class CssImports {

    private CssImports() {
    }

    /// Returns `rootText` with every `@import` replaced by the text it names,
    /// recursively.
    ///
    /// #### Parameters
    ///
    /// - `rootFile`: the stylesheet `rootText` was read from; relative imports
    ///   and rebased URLs are computed from its directory.
    /// - `rootText`: the contents of `rootFile`.
    /// - `imported`: receives every file pulled in, in inclusion order, so a
    ///   caller can watch them or compare their modification times.
    ///
    /// #### Throws
    ///
    /// - `IOException`: an imported file is missing or unreadable, an import
    ///   is circular, or an import names something other than a local file.
    public static String inline(File rootFile, String rootText, Set<File> imported) throws IOException {
        File root = rootFile.getCanonicalFile();
        List<File> stack = new ArrayList<File>();
        stack.add(root);
        return inline(root, rootText, stack, imported);
    }

    /// Every file `css` imports, directly or transitively, in inclusion order.
    ///
    /// For a caller that needs to know what a stylesheet depends on without
    /// compiling it: a staleness check, a file watcher.
    public static Set<File> collect(File css) throws IOException {
        Set<File> imported = new java.util.LinkedHashSet<File>();
        inline(css, readFully(css), imported);
        return imported;
    }

    /// [#collect(File)] for a caller that still wants an answer when an
    /// import cannot be followed: the files found before the broken one, and
    /// the file that is missing. A watcher needs that one most of all, since
    /// creating it is what makes the stylesheet compile again.
    public static Set<File> collectReachable(File css) {
        Set<File> imported = new java.util.LinkedHashSet<File>();
        try {
            inline(css, readFully(css), imported);
        } catch (MissingImportException ex) {
            imported.add(ex.getFile());
        } catch (IOException ex) {
            // Unreadable or malformed: what was found so far is the answer.
        }
        return imported;
    }

    /// An `@import` that names a file which is not there.
    public static final class MissingImportException extends IOException {
        private static final long serialVersionUID = 1L;
        private final File file;

        MissingImportException(String message, File file) {
            super(message);
            this.file = file;
        }

        /// The file the import names.
        public File getFile() {
            return file;
        }
    }

    /// Every existing local file a `url()` of `css`, or of anything it
    /// imports, names: its images and fonts.
    ///
    /// These are compiled into the theme, so a caller deciding whether a
    /// theme is stale has to look at them as well as at the stylesheets.
    public static Set<File> assets(File css) throws IOException {
        final File dir = css.getCanonicalFile().getParentFile();
        final Set<File> found = new java.util.LinkedHashSet<File>();
        String text = inline(css, readFully(css), new java.util.LinkedHashSet<File>());
        rewriteUrls(text, new UrlRewriter() {
            @Override
            public String rewrite(String url) {
                if (isRelativeUrl(url)) {
                    File file = new File(dir, url);
                    if (file.isFile()) {
                        found.add(file);
                    }
                }
                return url;
            }
        });
        return found;
    }

    /// Expands the imports of `text`, the contents of `current`. Every `url()`
    /// in the result is relative to the directory of `current`.
    private static String inline(File current, String text, List<File> stack, Set<File> imported)
            throws IOException {
        StringBuilder out = new StringBuilder(text.length());
        int len = text.length();
        int i = 0;
        int depth = 0;
        while (i < len) {
            char c = text.charAt(i);
            if (c == '/' && i + 1 < len && text.charAt(i + 1) == '*') {
                int end = text.indexOf("*/", i + 2);
                end = end < 0 ? len : end + 2;
                out.append(text, i, end);
                i = end;
            } else if (c == '"' || c == '\'') {
                int end = skipString(text, i);
                out.append(text, i, end);
                i = end;
            } else if (c == '{') {
                depth++;
                out.append(c);
                i++;
            } else if (c == '}') {
                depth--;
                out.append(c);
                i++;
            } else if (c == '@' && depth == 0 && text.regionMatches(true, i, "@import", 0, 7)
                    && i + 7 < len && !isIdentChar(text.charAt(i + 7))) {
                int end = endOfStatement(text, i);
                String statement = text.substring(i + 7, end).trim();
                out.append(resolve(current, statement, stack, imported));
                i = end < len ? end + 1 : len;
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    private static String resolve(File current, String statement, List<File> stack, Set<File> imported)
            throws IOException {
        String target;
        String rest;
        if (statement.regionMatches(true, 0, "url(", 0, 4)) {
            int close = statement.indexOf(')');
            if (close < 0) {
                throw new IOException("Malformed @import in " + current + ": " + statement);
            }
            target = unquote(statement.substring(4, close).trim());
            rest = statement.substring(close + 1).trim();
        } else if (statement.length() > 0 && (statement.charAt(0) == '"' || statement.charAt(0) == '\'')) {
            int close = skipString(statement, 0);
            target = unquote(statement.substring(0, close));
            rest = statement.substring(close).trim();
        } else {
            throw new IOException("Malformed @import in " + current + ": " + statement);
        }
        if (target.length() == 0) {
            throw new IOException("@import with no file name in " + current);
        }
        if (target.indexOf("://") > 0) {
            throw new IOException("@import of " + target + " in " + current
                    + " is not supported: only local files can be imported.");
        }
        File file = new File(target);
        if (!file.isAbsolute()) {
            file = new File(current.getParentFile(), target);
        }
        if (!file.isFile()) {
            throw new MissingImportException(
                    "@import in " + current + " names a file that does not exist: " + file, file);
        }
        file = file.getCanonicalFile();
        if (stack.contains(file)) {
            StringBuilder chain = new StringBuilder();
            for (File f : stack) {
                chain.append(f.getName()).append(" -> ");
            }
            throw new IOException("Circular @import: " + chain + file.getName());
        }
        imported.add(file);
        String body;
        stack.add(file);
        try {
            body = inline(file, readFully(file), stack, imported);
        } finally {
            stack.remove(stack.size() - 1);
        }
        // Only now, with the file's own imports expanded and already expressed
        // relative to it, is everything moved up one level. Rebasing first
        // would also rewrite the paths of the nested imports themselves.
        body = rebaseUrls(body, file.getParentFile(), current.getParentFile());
        StringBuilder out = new StringBuilder(body.length() + 64);
        out.append("\n/* @import ").append(file.getName()).append(" */\n");
        if (rest.length() > 0) {
            // A media-qualified import applies its rules only under that
            // media, exactly as if they had been written in an `@media` block
            // here. The queries a theme evaluates are the Codename One ones
            // (`platform-*`, `density-*`, `device-*`); a browser media type
            // such as `print` or `screen` selects nothing in a theme and its
            // rules apply, in an import as in a block. Dropping the rules of
            // one here and not the other would make the two forms disagree.
            out.append("@media ").append(rest).append(" {\n").append(body).append("\n}\n");
        } else {
            out.append(body).append('\n');
        }
        return out.toString();
    }

    /// Decides what one `url()` of a stylesheet becomes, for [#rewriteUrls].
    public interface UrlRewriter {
        /// Returns the replacement for `url`, the unquoted content of one
        /// `url()`; returning `url` itself leaves it as written.
        String rewrite(String url) throws IOException;
    }

    /// Rewrites each relative `url()` in `text` from "relative to `fromDir`"
    /// to "relative to `toDir`". Absolute paths, URLs with a scheme and
    /// `data:` URIs are left alone.
    static String rebaseUrls(String text, final File fromDir, final File toDir) throws IOException {
        if (fromDir.getCanonicalFile().equals(toDir.getCanonicalFile())) {
            return text;
        }
        return rewriteUrls(text, new UrlRewriter() {
            @Override
            public String rewrite(String url) throws IOException {
                return rebase(url, fromDir, toDir);
            }
        });
    }

    /// Whether `url` is resolved against the stylesheet's own directory, as
    /// opposed to an absolute path, a fragment, or anything with a scheme
    /// (`http:`, `data:`).
    public static boolean isRelativeUrl(String url) {
        return url.length() > 0 && url.charAt(0) != '/' && url.charAt(0) != '#' && url.indexOf(':') <= 0;
    }

    /// Passes the content of every `url()` in `text` through `rewriter`,
    /// skipping comments and string literals. Each `url()` is written back
    /// double quoted.
    public static String rewriteUrls(String text, UrlRewriter rewriter) throws IOException {
        StringBuilder out = new StringBuilder(text.length() + 32);
        int len = text.length();
        int i = 0;
        while (i < len) {
            char c = text.charAt(i);
            if (c == '/' && i + 1 < len && text.charAt(i + 1) == '*') {
                int end = text.indexOf("*/", i + 2);
                end = end < 0 ? len : end + 2;
                out.append(text, i, end);
                i = end;
            } else if (text.regionMatches(true, i, "url(", 0, 4) && (i == 0 || !isIdentChar(text.charAt(i - 1)))) {
                // A quoted url may hold a `)` of its own, as in
                // url("icon(1).png"); the one that ends it comes after the
                // closing quote.
                int start = i + 4;
                while (start < len && Character.isWhitespace(text.charAt(start))) {
                    start++;
                }
                int from = start < len && (text.charAt(start) == '"' || text.charAt(start) == '\'')
                        ? skipString(text, start) : start;
                int close = text.indexOf(')', from);
                if (close < 0) {
                    out.append(text, i, len);
                    break;
                }
                String raw = unquote(text.substring(i + 4, close).trim());
                out.append("url(\"").append(rewriter.rewrite(raw)).append("\")");
                i = close + 1;
            } else if (c == '"' || c == '\'') {
                int end = skipString(text, i);
                out.append(text, i, end);
                i = end;
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    private static String rebase(String url, File fromDir, File toDir) throws IOException {
        if (!isRelativeUrl(url)) {
            return url;
        }
        java.nio.file.Path base = toDir.getCanonicalFile().toPath();
        // Canonicalise only the directory: the file itself may not exist yet,
        // and resolving it would also follow a link named by the stylesheet.
        java.nio.file.Path resolved = fromDir.getCanonicalFile().toPath().resolve(url).normalize();
        try {
            return base.relativize(resolved).toString().replace('\\', '/');
        } catch (IllegalArgumentException differentRoot) {
            // No relative path exists between two filesystem roots.
            return resolved.toUri().toString();
        }
    }

    private static int skipString(String text, int start) {
        char quote = text.charAt(start);
        int i = start + 1;
        int len = text.length();
        while (i < len) {
            char c = text.charAt(i);
            if (c == '\\') {
                i += 2;
            } else if (c == quote) {
                return i + 1;
            } else {
                i++;
            }
        }
        return len;
    }

    private static int endOfStatement(String text, int start) {
        int len = text.length();
        int i = start;
        while (i < len) {
            char c = text.charAt(i);
            if (c == '"' || c == '\'') {
                i = skipString(text, i);
            } else if (c == ';') {
                return i;
            } else {
                i++;
            }
        }
        return len;
    }

    private static String unquote(String value) {
        int len = value.length();
        if (len >= 2) {
            char first = value.charAt(0);
            if ((first == '"' || first == '\'') && value.charAt(len - 1) == first) {
                return value.substring(1, len - 1);
            }
        }
        return value;
    }

    private static boolean isIdentChar(char c) {
        return c == '-' || c == '_' || (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static String readFully(File file) throws IOException {
        InputStream in = new FileInputStream(file);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream((int) Math.max(64, file.length()));
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), "UTF-8");
        } finally {
            in.close();
        }
    }
}
