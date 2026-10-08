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
package com.codename1.compat.jdk;

/// How a classpath resource is named once it ships: the one definition the
/// build and the runtime share.
///
/// A desktop application keeps its resources in package directories
/// (`com/example/img/logo.png`) and asks for them by that path. A Codename
/// One application bundle is flat: every resource sits at its root. The build
/// therefore ships each nested resource under a flat name, and the runtime
/// turns the path an application asks for into that same name. Both call
/// [#flatName(String)], so they cannot disagree; the build tool depends on
/// this artifact for that reason alone.
///
/// #### The rule
///
/// - A path with no directory -- `logo.png` -- keeps its name.
/// - In any other path every `_` becomes `_u` and every `/` becomes `__`:
///   `com/example/img/my_logo.png` ships as
///   `com__example__img__my_ulogo.png`.
///
/// `_` is an escape character there, so two different nested paths never
/// share a flat name, and the original path can be read back from it. The
/// file extension is the end of the last segment and is never touched, which
/// matters because a Codename One build treats some extensions specially. A
/// nested path can still produce the name of a file that is at the root on
/// its own account; the build reports that as an error rather than letting
/// one replace the other.
///
/// This class depends on nothing but `java.lang`, and must stay that way.
public final class ResourceNames {

    private ResourceNames() {
    }

    /// The resource path, absolute and without its leading slash, that
    /// `Class.getResource(name)` means for a class called `className`: a name
    /// starting with `/` is absolute, any other is relative to the class's
    /// package. `.` and `..` segments are resolved. Answers null for a path
    /// that climbs above the root or names nothing.
    ///
    /// #### Parameters
    ///
    /// - `className`: the class's name in either form, `com.example.Main` or
    ///   `com/example/Main`; null for the absolute lookup a class loader does
    ///
    /// - `name`: the name as the application passed it
    public static String resolve(String className, String name) {
        if (name == null) {
            return null;
        }
        if (name.startsWith("/")) {
            return normalize(name);
        }
        if (className == null) {
            return normalize(name);
        }
        String cls = className.replace('.', '/');
        int slash = cls.lastIndexOf('/');
        return normalize(slash < 0 ? name : cls.substring(0, slash + 1) + name);
    }

    /// `path` with leading slashes dropped, empty and `.` segments removed
    /// and `..` segments resolved; null when it climbs above the root or is
    /// empty once resolved.
    public static String normalize(String path) {
        if (path == null) {
            return null;
        }
        int n = path.length();
        StringBuilder out = new StringBuilder(n);
        int i = 0;
        while (i < n) {
            int end = path.indexOf('/', i);
            if (end < 0) {
                end = n;
            }
            int len = end - i;
            if (len == 0 || (len == 1 && path.charAt(i) == '.')) {
                // An empty segment or the directory itself: nothing to add.
                i = end + 1;
                continue;
            }
            if (len == 2 && path.charAt(i) == '.' && path.charAt(i + 1) == '.') {
                if (out.length() == 0) {
                    return null;
                }
                int cut = out.length() - 1;
                while (cut >= 0 && out.charAt(cut) != '/') {
                    cut--;
                }
                out.setLength(cut < 0 ? 0 : cut);
                i = end + 1;
                continue;
            }
            if (out.length() > 0) {
                out.append('/');
            }
            out.append(path.substring(i, end));
            i = end + 1;
        }
        return out.length() == 0 ? null : out.toString();
    }

    /// The name `path` ships under at the root of the application bundle.
    /// `path` is a normalized resource path ([#normalize(String)]); see the
    /// class description for the rule.
    public static String flatName(String path) {
        if (path.indexOf('/') < 0) {
            return path;
        }
        int n = path.length();
        StringBuilder out = new StringBuilder(n + 16);
        for (int i = 0; i < n; i++) {
            char c = path.charAt(i);
            if (c == '/') {
                out.append("__");
            } else if (c == '_') {
                out.append("_u");
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /// The nested path a flat name was made from, the inverse of
    /// [#flatName(String)]; null when `flat` is not the flat name of any
    /// nested path, which includes every name of a file that was at the root
    /// all along.
    public static String nestedPath(String flat) {
        int n = flat.length();
        StringBuilder out = new StringBuilder(n);
        boolean nested = false;
        for (int i = 0; i < n; i++) {
            char c = flat.charAt(i);
            if (c != '_') {
                out.append(c);
                continue;
            }
            if (i + 1 >= n) {
                return null;
            }
            char next = flat.charAt(++i);
            if (next == '_') {
                out.append('/');
                nested = true;
            } else if (next == 'u') {
                out.append('_');
            } else {
                return null;
            }
        }
        return nested ? out.toString() : null;
    }
}
