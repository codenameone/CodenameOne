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
package com.codename1.impl.compat;

import com.codename1.impl.JdkHost;
import com.codename1.ui.Display;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.ServiceLoader;
import java.util.Set;

/// Classpath resources for an application built from desktop or Android
/// sources: what `Class.getResource`, `Class.getResourceAsStream` and the
/// `ClassLoader` lookups do on a desktop. `com.codename1.impl.CoreJdkHost`
/// reads application resources through [#open(String)] as well, so the class
/// library's `java.util.ResourceBundle`, `java.util.ServiceLoader` and
/// `cn1res:` URLs find what the build shipped.
///
/// Nothing calls these methods by name. The build's remap step rewrites an
/// application's calls so that they arrive here: `getClass().getResource(n)`
/// becomes `Resources.getResource(getClass(), n)`, and so on for each method
/// below that takes the original receiver as its first parameter.
///
/// #### How a resource is found
///
/// A device has no classpath and no directories: every resource of the
/// application sits at the root of its bundle. The build ships each nested
/// resource under the flat name [ResourceNames#flatName(String)] gives its
/// path, and records the paths in an index (the registry generated with the
/// application, `com.codename1.generated.compat.CompatRegistry`, which calls
/// [#cn1BeginIndex()] and [#cn1AddResource(String)]). A lookup resolves
/// the name the way the JDK does -- relative to the class's package unless it
/// starts with `/`, with `.` and `..` resolved -- answers null when the index
/// has no such path, and otherwise reads the flat name. Nothing is opened to
/// find out whether a nested resource exists.
///
/// A name with no directory is not flattened, so the build's index may not
/// know it (a file from the ordinary Codename One resource directory is one
/// the desktop sources never listed). Such a name is looked for in the
/// bundle.
///
/// Where no build step ran -- this module's own tests, a layer's tests -- no
/// index is installed, and a resource is read under the path that was asked
/// for, then under its flat name.
///
/// #### URLs
///
/// `getResource` answers a [URL] in the `cn1res:` scheme whose path is the
/// resource's ORIGINAL absolute path: `cn1res:/com/example/img/logo.png`.
/// `openStream()` reads it through this class, and `toString()`,
/// `toExternalForm()`, `getPath()` and `getFile()` are the same on every
/// platform and every run, so code that passes `url.toExternalForm()` to an
/// image or stylesheet loader keeps working: the loader opens the string as a
/// URL again, or hands its path to [#open(String)].
///
/// The device's own `java.net.URL` knows the scheme. A port that runs on a
/// real JDK, whose `URL` does not, installs a [UrlFactory] that attaches a
/// handler reading through [#openUrlPath(String)].
///
/// #### Bundles and services on a real JDK
///
/// The JDK's `ResourceBundle` and `ServiceLoader` look a nested name up
/// through a class loader, which cannot see a resource the build flattened.
/// A port on a real JDK therefore installs, with
/// [#cn1SetClassLoader(ClassLoader)], a loader that falls back to this
/// class, and the build sends an application's `ResourceBundle.getBundle`
/// and `ServiceLoader.load` calls to the methods here that pass it. Where
/// none is installed -- a device, whose class library reads the generated
/// registry -- those methods are the calls they stand in for.
///
/// #### Class names
///
/// A relative name is resolved against `Class.getName()`. A build that
/// renames classes after the remap step (an obfuscator) changes that name,
/// so relative lookups need the application's classes kept under their
/// names; absolute names are unaffected.
public final class Resources {

    /// The paths the build shipped, or null when no build step recorded any.
    private static Set<String> index;

    private static Provider provider;

    private static ClassLoader contextLoader;

    private static UrlFactory urlFactory;

    private static ClassLoader portLoader;

    private Resources() {
    }

    /// Where the bytes of a flat resource name come from. The default reads
    /// the application bundle; a test installs its own with
    /// [#cn1SetProvider(Provider)].
    public interface Provider {
        /// The resource shipped as `flatName` (no leading slash), or null
        /// when the bundle has none.
        InputStream open(String flatName) throws IOException;
    }

    /// Replaces the source of resource bytes; null restores the default.
    public static void cn1SetProvider(Provider p) {
        provider = p;
    }

    /// Makes the `cn1res:` URLs [#getResource(Class, String)] answers, for a
    /// port whose `java.net.URL` does not know the scheme.
    public interface UrlFactory {
        /// The `cn1res:` URL of `encodedPath`, an absolute path that is
        /// already percent-encoded; [Resources#openUrlPath(String)] reads it.
        URL create(String encodedPath) throws IOException;
    }

    /// Installs the port's URL factory; null restores the default
    /// `new URL("cn1res", null, path)`.
    public static void cn1SetUrlFactory(UrlFactory factory) {
        urlFactory = factory;
    }

    /// Installs the class loader a port on a real JDK resolves flattened
    /// resources through; see the class description. Null removes it.
    public static void cn1SetClassLoader(ClassLoader loader) {
        portLoader = loader;
    }

    /// The loader [#cn1SetClassLoader(ClassLoader)] installed, or null.
    public static ClassLoader cn1ClassLoader() {
        return portLoader;
    }

    /// Opens the resource a `cn1res:` URL names by `encodedPath`, its
    /// percent-encoded path: what such a URL's stream reads.
    public static InputStream openUrlPath(String encodedPath) {
        String path = decode(encodedPath);
        return path == null ? null : open(path);
    }

    /// `ResourceBundle.getBundle(baseName)`, through the port's loader when
    /// one is installed.
    public static ResourceBundle getBundle(String baseName) {
        return getBundle(baseName, Locale.getDefault());
    }

    /// `ResourceBundle.getBundle(baseName, locale)`, through the port's
    /// loader when one is installed.
    public static ResourceBundle getBundle(String baseName, Locale locale) {
        ClassLoader loader = portLoader;
        if (loader == null) {
            return ResourceBundle.getBundle(baseName, locale);
        }
        return ResourceBundle.getBundle(baseName, locale, loader);
    }

    /// The folder of the services files `ServiceLoader` reads on a JDK.
    public static final String SERVICES = "META-INF/services/";

    /// For a port on a real JDK, whose `ServiceLoader` reads services files
    /// through the installed loader: the services file to read for `service`
    /// (dotted, as `Class.getName()` answers it), listing what the generated
    /// registry lists -- the application's providers, then each bundled
    /// library's, and only those that ship, as on a device. Empty when the
    /// registry lists none; null when no generated registry is installed, and
    /// the files on the class path are all there is.
    public static String cn1ServicesFile(String service) {
        if (!CompatRegistryBase.cn1Installed()) {
            return null;
        }
        String[] providers = JdkHost.getRegistry().providers(service);
        StringBuilder out = new StringBuilder();
        if (providers != null) {
            for (int i = 0; i < providers.length; i++) {
                out.append(providers[i]).append('\n');
            }
        }
        return out.toString();
    }

    /// `ServiceLoader.load(service)`, through the port's loader when one is
    /// installed.
    public static <S> ServiceLoader<S> load(Class<S> service) {
        ClassLoader loader = portLoader;
        if (loader == null) {
            return ServiceLoader.load(service);
        }
        return ServiceLoader.load(service, loader);
    }

    /// Starts an index: from here on a nested path exists only if
    /// [#cn1AddResource(String)] named it. The generated registry calls
    /// this once, then adds every path.
    public static void cn1BeginIndex() {
        index = new HashSet<String>();
    }

    /// Records that the build shipped the resource at `path`, an absolute
    /// path without its leading slash. Only after [#cn1BeginIndex()]: a path
    /// added to no index would be one forgotten in silence.
    public static void cn1AddResource(String path) {
        Set<String> current = index;
        if (current == null) {
            throw new IllegalStateException("cn1BeginIndex() has not been called");
        }
        current.add(path);
    }

    /// Every path the build recorded, in no particular order; empty when
    /// no build step recorded any. A loader that has to find a resource by
    /// what is IN it -- a font by the name it gives itself -- reads these.
    public static String[] cn1ResourcePaths() {
        Set<String> current = index;
        if (current == null) {
            return new String[0];
        }
        String[] out = new String[current.size()];
        int i = 0;
        for (String path : current) {
            out[i++] = path;
        }
        return out;
    }

    /// Forgets the index, as if no build step had run. For tests.
    public static void cn1ClearIndex() {
        index = null;
    }

    /// `Class.getResourceAsStream(name)` for `cls`.
    public static InputStream getResourceAsStream(Class<?> cls, String name) {
        return open(ResourceNames.resolve(cls.getName(), requireName(name)));
    }

    /// `Class.getResource(name)` for `cls`.
    public static URL getResource(Class<?> cls, String name) {
        return url(ResourceNames.resolve(cls.getName(), requireName(name)));
    }

    /// `ClassLoader.getResourceAsStream(name)`. An application has one
    /// loader, so `loader` only has to be non-null. As on a desktop, a class
    /// loader's names are absolute and written WITHOUT a leading slash; one
    /// that has it finds nothing.
    public static InputStream getResourceAsStream(ClassLoader loader, String name) {
        requireLoader(loader);
        return getSystemResourceAsStream(name);
    }

    /// `ClassLoader.getResource(name)`; see
    /// [#getResourceAsStream(ClassLoader, String)].
    public static URL getResource(ClassLoader loader, String name) {
        requireLoader(loader);
        return getSystemResource(name);
    }

    /// `ClassLoader.getResources(name)`: the one resource of that name, or
    /// none.
    public static Enumeration<URL> getResources(ClassLoader loader, String name) {
        requireLoader(loader);
        return getSystemResources(name);
    }

    /// `ClassLoader.getSystemResourceAsStream(name)`.
    public static InputStream getSystemResourceAsStream(String name) {
        return requireName(name).startsWith("/") ? null : open(ResourceNames.normalize(name));
    }

    /// `ClassLoader.getSystemResource(name)`.
    public static URL getSystemResource(String name) {
        return requireName(name).startsWith("/") ? null : url(ResourceNames.normalize(name));
    }

    /// `ClassLoader.getSystemResources(name)`.
    public static Enumeration<URL> getSystemResources(String name) {
        List<URL> found = new ArrayList<URL>(1);
        URL url = getSystemResource(name);
        if (url != null) {
            found.add(url);
        }
        return new UrlEnumeration(found);
    }

    /// `Thread.getContextClassLoader()`: the application's one class loader,
    /// or whatever [#setContextClassLoader(Thread, ClassLoader)] was last
    /// given. It is good for the resource lookups above and for nothing
    /// else; the build rejects any other use of a class loader.
    public static ClassLoader getContextClassLoader(Thread thread) {
        if (thread == null) {
            throw new NullPointerException();
        }
        if (contextLoader != null) {
            return contextLoader;
        }
        return portLoader != null ? portLoader : ClassLoader.getSystemClassLoader();
    }

    /// `loader.loadClass(name)`: the class of that name when the application
    /// has it, which is all a class loader can answer where no class is ever
    /// loaded at run time. Desktop code asks this way to find out whether an
    /// optional class is present; one that is not throws
    /// `ClassNotFoundException`, as on a desktop.
    public static Class<?> loadClass(ClassLoader loader, String name) throws ClassNotFoundException {
        if (loader == null || name == null) {
            throw new NullPointerException();
        }
        return Class.forName(name);
    }

    /// `Thread.setContextClassLoader(loader)`. There is one loader, so this
    /// only remembers the object for [#getContextClassLoader(Thread)] to
    /// hand back, for every thread alike.
    public static void setContextClassLoader(Thread thread, ClassLoader loader) {
        if (thread == null) {
            throw new NullPointerException();
        }
        contextLoader = loader;
    }

    /// Whether the application ships a resource at `path`, an absolute path
    /// with or without its leading slash.
    public static boolean exists(String path) {
        String p = ResourceNames.normalize(path);
        if (p == null) {
            return false;
        }
        if (index != null && (index.contains(p) || p.indexOf('/') >= 0)) {
            return index.contains(p);
        }
        InputStream in = read(p);
        if (in == null) {
            return false;
        }
        try {
            in.close();
        } catch (IOException e) {
            // It was there to be opened, which is all that was asked.
            return true;
        }
        return true;
    }

    /// Opens the resource at `path`, an absolute path with or without its
    /// leading slash, as the application named it on a desktop
    /// (`/com/example/img/logo.png`). Answers null when there is none. This
    /// is what a `cn1res:` URL reads, and what a loader that was handed such
    /// a URL's path should call.
    public static InputStream open(String path) {
        String p = ResourceNames.normalize(path);
        if (p == null) {
            return null;
        }
        if (index != null && p.indexOf('/') >= 0 && !index.contains(p)) {
            return null;
        }
        return read(p);
    }

    private static InputStream read(String p) {
        boolean nested = p.indexOf('/') >= 0;
        if (index == null && nested && provider == null) {
            // Nothing was flattened by a build: the resource may still be
            // where the application's own build put it.
            InputStream direct = Resources.class.getResourceAsStream("/" + p);
            if (direct != null) {
                return direct;
            }
        }
        String flat = ResourceNames.flatName(p);
        if (provider != null) {
            try {
                return provider.open(flat);
            } catch (IOException e) {
                // A resource that cannot be read is one that is not there.
                return null;
            }
        }
        if (Display.isInitialized()) {
            return Display.getInstance().getResourceAsStream(Resources.class, "/" + flat);
        }
        return Resources.class.getResourceAsStream("/" + flat);
    }

    private static URL url(String p) {
        if (p == null || !exists(p)) {
            return null;
        }
        String encoded = "/" + encode(p);
        UrlFactory factory = urlFactory;
        try {
            if (factory != null) {
                return factory.create(encoded);
            }
            return new URL("cn1res", null, encoded);
        } catch (MalformedURLException e) {
            // The device's URL knows cn1res, and a port that runs elsewhere
            // installs a factory; a URL nobody can make is no resource.
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    /// The inverse of [#encode(String)]: UTF-8 percent escapes decoded,
    /// anything else as it stands. Null for a malformed escape.
    static String decode(String encoded) {
        if (encoded == null || encoded.indexOf('%') < 0) {
            return encoded;
        }
        int n = encoded.length();
        byte[] bytes = new byte[n];
        StringBuilder out = new StringBuilder(n);
        int i = 0;
        while (i < n) {
            char c = encoded.charAt(i);
            if (c != '%') {
                out.append(c);
                i++;
                continue;
            }
            int count = 0;
            while (i < n && encoded.charAt(i) == '%') {
                if (i + 2 >= n) {
                    return null;
                }
                int high = Character.digit(encoded.charAt(i + 1), 16);
                int low = Character.digit(encoded.charAt(i + 2), 16);
                if (high < 0 || low < 0) {
                    return null;
                }
                bytes[count++] = (byte) ((high << 4) | low);
                i += 3;
            }
            try {
                out.append(new String(bytes, 0, count, "UTF-8"));
            } catch (java.io.UnsupportedEncodingException e) {
                return null;
            }
        }
        return out.toString();
    }

    private static String requireName(String name) {
        if (name == null) {
            throw new NullPointerException();
        }
        return name;
    }

    private static void requireLoader(ClassLoader loader) {
        if (loader == null) {
            throw new NullPointerException();
        }
    }

    private static final String HEX = "0123456789ABCDEF";

    /// Percent-encodes what would otherwise end a URL's path or be read as
    /// an escape: everything but letters, digits and `/ - . _ ~ $ + ! ' ( )
    /// , @ = & ;`, as UTF-8.
    static String encode(String path) {
        int n = path.length();
        StringBuilder out = null;
        for (int i = 0; i < n; i++) {
            char c = path.charAt(i);
            boolean plain = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || "/-._~$+!'(),@=&;".indexOf(c) >= 0;
            if (plain) {
                if (out != null) {
                    out.append(c);
                }
                continue;
            }
            if (out == null) {
                out = new StringBuilder(n + 16);
                out.append(path.substring(0, i));
            }
            int code = c;
            if (c >= 0xd800 && c <= 0xdbff && i + 1 < n) {
                char low = path.charAt(i + 1);
                if (low >= 0xdc00 && low <= 0xdfff) {
                    code = 0x10000 + ((c - 0xd800) << 10) + (low - 0xdc00);
                    i++;
                }
            }
            if (code < 0x80) {
                hex(out, code);
            } else if (code < 0x800) {
                hex(out, 0xc0 | (code >> 6));
                hex(out, 0x80 | (code & 0x3f));
            } else if (code < 0x10000) {
                hex(out, 0xe0 | (code >> 12));
                hex(out, 0x80 | ((code >> 6) & 0x3f));
                hex(out, 0x80 | (code & 0x3f));
            } else {
                hex(out, 0xf0 | (code >> 18));
                hex(out, 0x80 | ((code >> 12) & 0x3f));
                hex(out, 0x80 | ((code >> 6) & 0x3f));
                hex(out, 0x80 | (code & 0x3f));
            }
        }
        return out == null ? path : out.toString();
    }

    private static void hex(StringBuilder out, int b) {
        out.append('%').append(HEX.charAt((b >> 4) & 0xf)).append(HEX.charAt(b & 0xf));
    }

    private static final class UrlEnumeration implements Enumeration<URL> {
        private final List<URL> urls;
        private int next;

        UrlEnumeration(List<URL> urls) {
            this.urls = urls;
        }

        @Override
        public boolean hasMoreElements() {
            return next < urls.size();
        }

        @Override
        public URL nextElement() {
            if (next >= urls.size()) {
                throw new java.util.NoSuchElementException();
            }
            return urls.get(next++);
        }
    }
}
