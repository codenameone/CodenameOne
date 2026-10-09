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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/// The one implementation of [Path]: a normalized path string, split into a
/// root and names when a method needs them.
final class PathImpl implements Path {

    private static final String[] NO_NAMES = new String[0];
    private static final String SCHEME = "file:";

    /// Separators collapsed, no trailing separator unless the path is a root.
    private final String path;
    private final int rootLength;
    private String[] names;

    private PathImpl(String path) {
        this.path = path;
        this.rootLength = rootLength(path);
    }

    static PathImpl of(String first, String[] more) {
        if (first == null) {
            throw new NullPointerException();
        }
        String joined = first;
        if (more != null && more.length > 0) {
            StringBuilder sb = new StringBuilder(first);
            for (String m : more) {
                if (m == null) {
                    throw new NullPointerException();
                }
                if (m.length() > 0) {
                    if (sb.length() > 0) {
                        sb.append('/');
                    }
                    sb.append(m);
                }
            }
            joined = sb.toString();
        }
        if (joined.indexOf('\0') >= 0) {
            throw new InvalidPathException(joined, "Nul character not allowed");
        }
        return new PathImpl(clean(joined));
    }

    static PathImpl of(String path) {
        return of(path, null);
    }

    static PathImpl of(URI uri) {
        String scheme = uri.getScheme();
        if (scheme == null || !"file".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("URI scheme is not \"file\"");
        }
        String p = uri.getPath();
        if (p == null || p.length() == 0) {
            throw new IllegalArgumentException("URI has no path component");
        }
        return of(p);
    }

    /// How many leading characters are the root: one for `/`, the scheme and
    /// its slashes for a `file:` path, none for a relative path.
    private static int rootLength(String p) {
        if (p.startsWith(SCHEME)) {
            int i = SCHEME.length();
            while (i < p.length() && p.charAt(i) == '/') {
                i++;
            }
            return i;
        }
        return p.startsWith("/") ? 1 : 0;
    }

    /// Collapses runs of separators and drops a trailing one, leaving the
    /// slashes of a `file:` root as they were written.
    private static String clean(String p) {
        int root = 0;
        StringBuilder sb = new StringBuilder(p.length());
        if (p.startsWith(SCHEME)) {
            root = rootLength(p);
            sb.append(p.substring(0, root));
        }
        boolean separator = false;
        for (int i = root; i < p.length(); i++) {
            char c = p.charAt(i);
            if (c == '\\') {
                c = '/';
            }
            if (c == '/') {
                if (sb.length() == 0) {
                    sb.append('/');
                    root = 1;
                } else {
                    separator = sb.length() > root;
                }
                continue;
            }
            if (separator) {
                sb.append('/');
                separator = false;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private String[] names() {
        if (names == null) {
            if (path.length() == rootLength) {
                // The empty path is one empty name; a root has none.
                names = rootLength == 0 ? new String[] {""} : NO_NAMES;
            } else {
                List<String> out = new ArrayList<String>();
                int start = rootLength;
                for (int i = rootLength; i <= path.length(); i++) {
                    if (i == path.length() || path.charAt(i) == '/') {
                        out.add(path.substring(start, i));
                        start = i + 1;
                    }
                }
                names = out.toArray(new String[out.size()]);
            }
        }
        return names;
    }

    private boolean isEmpty() {
        return path.length() == 0;
    }

    private String root() {
        return path.substring(0, rootLength);
    }

    private static PathImpl build(String root, String[] names, int from, int to) {
        StringBuilder sb = new StringBuilder(root);
        for (int i = from; i < to; i++) {
            if (i > from) {
                sb.append('/');
            }
            sb.append(names[i]);
        }
        return new PathImpl(sb.toString());
    }

    private static PathImpl impl(Path other) {
        if (other == null) {
            throw new NullPointerException();
        }
        if (other instanceof PathImpl) {
            return (PathImpl) other;
        }
        return of(other.toString());
    }

    @Override
    public boolean isAbsolute() {
        return rootLength > 0;
    }

    @Override
    public Path getRoot() {
        return rootLength == 0 ? null : new PathImpl(root());
    }

    @Override
    public Path getFileName() {
        String[] n = names();
        return n.length == 0 ? null : new PathImpl(n[n.length - 1]);
    }

    @Override
    public Path getParent() {
        String[] n = names();
        if (n.length == 0) {
            return null;
        }
        if (n.length == 1) {
            return rootLength == 0 ? null : new PathImpl(root());
        }
        return build(root(), n, 0, n.length - 1);
    }

    @Override
    public int getNameCount() {
        return names().length;
    }

    @Override
    public Path getName(int index) {
        String[] n = names();
        if (index < 0 || index >= n.length) {
            throw new IllegalArgumentException();
        }
        return new PathImpl(n[index]);
    }

    @Override
    public Path subpath(int beginIndex, int endIndex) {
        String[] n = names();
        if (beginIndex < 0 || beginIndex >= n.length || endIndex > n.length || beginIndex >= endIndex) {
            throw new IllegalArgumentException();
        }
        return build("", n, beginIndex, endIndex);
    }

    @Override
    public boolean startsWith(Path other) {
        PathImpl o = impl(other);
        if (!root().equals(o.root())) {
            return false;
        }
        String[] a = names();
        String[] b = o.names();
        if (b.length > a.length) {
            return false;
        }
        for (int i = 0; i < b.length; i++) {
            if (!a[i].equals(b[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean startsWith(String other) {
        return startsWith(of(other));
    }

    @Override
    public boolean endsWith(Path other) {
        PathImpl o = impl(other);
        if (o.rootLength > 0) {
            return equals(o);
        }
        String[] a = names();
        String[] b = o.names();
        if (b.length > a.length) {
            return false;
        }
        for (int i = 0; i < b.length; i++) {
            if (!a[a.length - b.length + i].equals(b[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean endsWith(String other) {
        return endsWith(of(other));
    }

    @Override
    public Path normalize() {
        String[] n = names();
        List<String> out = new ArrayList<String>(n.length);
        for (String name : n) {
            if (".".equals(name) || name.length() == 0) {
                continue;
            }
            if ("..".equals(name)) {
                int last = out.size() - 1;
                if (last >= 0 && !"..".equals(out.get(last))) {
                    out.remove(last);
                    continue;
                }
                if (rootLength > 0) {
                    // Above the root there is the root.
                    continue;
                }
            }
            out.add(name);
        }
        if (out.size() == n.length) {
            return this;
        }
        return build(root(), out.toArray(new String[out.size()]), 0, out.size());
    }

    @Override
    public Path resolve(Path other) {
        PathImpl o = impl(other);
        if (o.rootLength > 0 || isEmpty()) {
            return o;
        }
        if (o.isEmpty()) {
            return this;
        }
        return new PathImpl(path.length() == rootLength ? path + o.path : path + "/" + o.path);
    }

    @Override
    public Path resolve(String other) {
        return resolve(of(other));
    }

    @Override
    public Path resolveSibling(Path other) {
        PathImpl o = impl(other);
        Path parent = getParent();
        return parent == null ? o : parent.resolve(o);
    }

    @Override
    public Path resolveSibling(String other) {
        return resolveSibling(of(other));
    }

    @Override
    public Path relativize(Path other) {
        PathImpl o = impl(other);
        if (!root().equals(o.root())) {
            throw new IllegalArgumentException("'other' is different type of Path");
        }
        if (path.equals(o.path)) {
            return new PathImpl("");
        }
        String[] a = isEmpty() ? NO_NAMES : names();
        String[] b = o.isEmpty() ? NO_NAMES : o.names();
        int common = 0;
        while (common < a.length && common < b.length && a[common].equals(b[common])) {
            common++;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = common; i < a.length; i++) {
            if ("..".equals(a[i])) {
                throw new IllegalArgumentException("Unable to compute relative path from " + this + " to " + other);
            }
            if (".".equals(a[i])) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('/');
            }
            sb.append("..");
        }
        for (int i = common; i < b.length; i++) {
            if (sb.length() > 0) {
                sb.append('/');
            }
            sb.append(b[i]);
        }
        return new PathImpl(sb.toString());
    }

    @Override
    public URI toUri() {
        String storage = toFile().storagePath();
        try {
            return new URI(storage);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    @Override
    public Path toAbsolutePath() {
        return rootLength > 0 ? this : new PathImpl(clean(toFile().getAbsolutePath()));
    }

    @Override
    public Path toRealPath(LinkOption... options) throws IOException {
        File file = toFile();
        if (!file.exists()) {
            throw new NoSuchFileException(path);
        }
        return new PathImpl(clean(file.getCanonicalPath()));
    }

    @Override
    public File toFile() {
        return new File(path);
    }

    @Override
    public Iterator<Path> iterator() {
        return new Names(names());
    }

    /// The names of a path, each as a path of its own.
    private static final class Names implements Iterator<Path> {
        private final String[] n;
        private int next;

        Names(String[] n) {
            this.n = n;
        }

        @Override
        public boolean hasNext() {
            return next < n.length;
        }

        @Override
        public Path next() {
            if (next >= n.length) {
                throw new NoSuchElementException();
            }
            return new PathImpl(n[next++]);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public int compareTo(Path other) {
        return path.compareTo(impl(other).path);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PathImpl && ((PathImpl) o).path.equals(path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public String toString() {
        return path;
    }
}
