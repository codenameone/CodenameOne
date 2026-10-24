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
package com.codename1.compat.testing;

import com.codename1.ui.Display;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

/// Teaches the Java runtime the headless implementation runs on the `cn1res:` URL scheme, for
/// tests that run the desktop runtime as compiled rather than relocated.
/// A built application's `Class.getResource` answers the desktop runtime's
/// own `java.net.URL` shim, which reads the scheme itself; the unrelocated
/// runtime answers a JDK `URL`, which without this does not know it.
///
/// A `cn1res:` URL's path is the resource's original absolute path. The
/// build ships a nested resource at the root of the application under a flat
/// name -- each `_` written `_u` and each `/` written `__` -- and the
/// handler reads that name, then the path as it was, through the display,
/// as the shim does. It knows nothing else of the desktop runtime.
///
/// A JVM takes one stream handler factory, and every test in it shares this
/// one. A test that reads resources from somewhere else -- a `Resources`
/// class another loader defined, a fake bundle -- points the scheme there
/// with [#setReader(Reader)] and restores it with `setReader(null)`.
public final class JdkResourceSupport {
    static final String SCHEME = "cn1res";

    private static boolean installed;

    private static Reader reader;

    /// Reads the resource a `cn1res:` URL names, given the URL's path as it
    /// is written (percent-encoded).
    public interface Reader {
        /// The resource's bytes, or null when there is none.
        InputStream open(String encodedPath) throws IOException;
    }

    /// Replaces how a `cn1res:` URL is read; null restores the port's way.
    /// Installs the scheme if it is not installed yet.
    public static synchronized void setReader(Reader r) {
        install();
        reader = r;
    }

    private JdkResourceSupport() {
    }

    /// Registers the scheme once. A JVM takes a single stream handler
    /// factory; when another is in place already the scheme stays unknown,
    /// and `Class.getResource` answers null for a flattened resource.
    public static synchronized void install() {
        if (installed) {
            return;
        }
        installed = true;
        try {
            URL.setURLStreamHandlerFactory(new Factory());
        } catch (Error alreadySet) {
            // Another factory owns URL; see above.
        }
    }

    static String flatName(String path) {
        if (path.indexOf('/') < 0) {
            return path;
        }
        StringBuilder out = new StringBuilder(path.length() + 16);
        for (int i = 0; i < path.length(); i++) {
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

    static InputStream open(String path) throws IOException {
        String p = path.startsWith("/") ? path.substring(1) : path;
        if (p.length() == 0) {
            return null;
        }
        InputStream in = read("/" + flatName(p));
        return in != null ? in : read("/" + p);
    }

    private static InputStream read(String name) throws IOException {
        if (Display.isInitialized()) {
            return Display.getInstance().getResourceAsStream(JdkResourceSupport.class, name);
        }
        return JdkResourceSupport.class.getResourceAsStream(name);
    }

    private static final class Factory implements URLStreamHandlerFactory {
        @Override
        public URLStreamHandler createURLStreamHandler(String protocol) {
            return SCHEME.equals(protocol) ? new Handler() : null;
        }
    }

    private static final class Handler extends URLStreamHandler {
        @Override
        protected URLConnection openConnection(URL u) {
            return new URLConnection(u) {
                @Override
                public void connect() {
                    connected = true;
                }

                @Override
                public InputStream getInputStream() throws IOException {
                    Reader custom = reader;
                    if (custom != null) {
                        InputStream in = custom.open(getURL().getPath());
                        if (in == null) {
                            throw new FileNotFoundException(getURL().toExternalForm());
                        }
                        return in;
                    }
                    String path;
                    try {
                        path = new URI(getURL().toExternalForm()).getPath();
                    } catch (URISyntaxException e) {
                        path = null;
                    }
                    if (path == null) {
                        path = getURL().getPath();
                    }
                    InputStream in = open(path);
                    if (in == null) {
                        throw new FileNotFoundException(getURL().toExternalForm());
                    }
                    return in;
                }
            };
        }
    }
}
