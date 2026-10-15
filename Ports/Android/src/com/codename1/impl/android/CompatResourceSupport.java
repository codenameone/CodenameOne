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
package com.codename1.impl.android;

import com.codename1.impl.compat.Resources;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Collections;
import java.util.Enumeration;

/// What an application built from desktop or Android sources needs of the
/// runtime this port runs on to find the resources its build flattened
/// ([com.codename1.impl.compat.Resources]).
///
/// - The JDK's `URL` does not know the `cn1res:` scheme, so the URLs
///   `Resources` answers are made here with a handler that reads them back
///   through it.
/// - The JDK's `ResourceBundle` and `ServiceLoader` look a nested name up
///   through a class loader, so the loader installed here asks the
///   application's own loader first and `Resources` after it.
///
/// The JavaSE port carries the same class for the same reasons.
final class CompatResourceSupport {
    private static final String SCHEME = "cn1res";

    private CompatResourceSupport() {
    }

    /// Installs both into `Resources`. Idempotent.
    static void install() {
        Resources.cn1SetUrlFactory(new Factory());
        if (Resources.cn1ClassLoader() == null) {
            ClassLoader parent = Resources.class.getClassLoader();
            Resources.cn1SetClassLoader(new FlatResourceLoader(parent));
        }
    }

    private static URL create(String encodedPath) throws MalformedURLException {
        return new URL(SCHEME, null, -1, encodedPath, Handler.INSTANCE);
    }

    private static final class Factory implements Resources.UrlFactory {
        @Override
        public URL create(String encodedPath) throws IOException {
            return CompatResourceSupport.create(encodedPath);
        }
    }

    private static final class Handler extends URLStreamHandler {
        static final Handler INSTANCE = new Handler();

        @Override
        protected URLConnection openConnection(URL u) {
            return new Connection(u);
        }
    }

    private static final class Connection extends URLConnection {
        Connection(URL url) {
            super(url);
        }

        @Override
        public void connect() {
            connected = true;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            InputStream in = Resources.openUrlPath(getURL().getPath());
            if (in == null) {
                throw new FileNotFoundException(getURL().toExternalForm());
            }
            return in;
        }
    }

    /// The application's loader, then the resources the build flattened.
    private static final class FlatResourceLoader extends ClassLoader {
        FlatResourceLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        protected URL findResource(String name) {
            if (name == null || name.startsWith("/") || !Resources.exists(name)) {
                return null;
            }
            return Resources.getSystemResource(name);
        }

        @Override
        protected Enumeration<URL> findResources(String name) {
            URL url = findResource(name);
            if (url == null) {
                return Collections.<URL>emptyEnumeration();
            }
            return Collections.enumeration(Collections.singletonList(url));
        }

        /// A services file is the generated registry's list once one is
        /// installed, and nothing else: the files the build flattened would
        /// shadow one another, and would name providers that do not ship.
        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            if (name != null && name.startsWith(Resources.SERVICES)) {
                String file = Resources.cn1ServicesFile(name.substring(Resources.SERVICES.length()));
                if (file != null) {
                    URL url = new URL(SCHEME, null, -1, "/" + name, new TextHandler(file));
                    return Collections.enumeration(Collections.singletonList(url));
                }
            }
            return super.getResources(name);
        }
    }

    /// Serves one text, as UTF-8, whatever the URL.
    private static final class TextHandler extends URLStreamHandler {
        private final String text;

        TextHandler(String text) {
            this.text = text;
        }

        @Override
        protected URLConnection openConnection(URL u) {
            return new URLConnection(u) {
                @Override
                public void connect() {
                    connected = true;
                }

                @Override
                public InputStream getInputStream() throws IOException {
                    return new ByteArrayInputStream(text.getBytes("UTF-8"));
                }
            };
        }
    }
}
