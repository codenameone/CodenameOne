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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/// `java.util.ServiceLoader` for the Codename One runtime: the providers of a
/// service, as the `META-INF/services` files of the application and of the
/// libraries bundled with it list them.
///
/// #### Where the providers come from
///
/// A device discovers nothing at run time and creates no class from its
/// name. The build reads every `META-INF/services/<service>` file the
/// application ships, and generates into [CompatRegistry] one registration
/// per provider class ([#cn1RegisterProvider(String, Cn1Factory, int)]) and a
/// factory that creates each of them with `new`. So the set of providers is
/// fixed when the application is built: the files are read in class path
/// order, the application's own first, and each file from top to bottom, with
/// comments and blank lines dropped and a class listed twice kept once, as the
/// JDK reads them.
///
/// A listed class that the application does not ship, that is abstract or
/// that has no public constructor without parameters is left out by the build
/// with a warning. The JDK throws `ServiceConfigurationError` for such a
/// provider when the iteration reaches it; here it is simply not there.
///
/// #### What is the same as the JDK
///
/// Providers are created lazily, one at a time as an iteration reaches them,
/// and once per loader: a second `iterator()` answers the instances the first
/// one created and goes on from where it stopped. [#reload()] forgets them.
/// [#stream()] creates nothing until [Provider#get()] is called, which
/// answers a new instance on every call.
///
/// #### What is not there
///
/// The class loader and the module layer a loader is asked for select
/// nothing: an application has one of each. A provider that is a static
/// `provider()` method of a named module is not found, since nothing here is
/// a named module.
///
/// Where no build step ran -- this module's own tests, a layer's tests -- the
/// services file is read through [Resources] and the classes are created
/// from their names.
public final class ServiceLoader<S> implements Iterable<S> {

    /// The providers the build registered, by the service's class name.
    private static final Map<String, List<Entry>> REGISTERED = new HashMap<String, List<Entry>>();

    /// Set once the build's registry has registered everything there is.
    private static boolean sealed;

    private final Class<S> service;

    /// The providers of this service, or null until an iteration asks.
    private List<Entry> entries;

    /// The instances created so far, in order.
    private final List<S> instances = new ArrayList<S>();

    /// The index in [#entries] of the next provider to create.
    private int next;

    /// Whether this is the loader of the installed providers, which has none.
    private final boolean installed;

    private ServiceLoader(Class<S> service, boolean installed) {
        if (service == null) {
            throw new NullPointerException("Service interface cannot be null");
        }
        this.service = service;
        this.installed = installed;
    }

    /// One provider of a service, not yet created: what [#stream()] answers.
    public interface Provider<S> extends Supplier<S> {
        /// The provider's class.
        Class<? extends S> type();

        /// A new instance of the provider.
        @Override
        S get();
    }

    /// Creates the provider classes an application has, for
    /// [#cn1RegisterProvider(String, Cn1Factory, int)]. A device has no
    /// reflection to create a class from its name, so the build generates
    /// one implementation whose methods are a `switch` over `new`
    /// expressions and class constants.
    public interface Cn1Factory {
        /// A new instance of the provider registered under `id`.
        Object cn1CreateService(int id);

        /// The class of the provider registered under `id`.
        Class<?> cn1ServiceType(int id);
    }

    /// One provider: registered by the build, or named by a services file
    /// that was read at run time.
    private static final class Entry {
        final Cn1Factory factory;
        final int id;
        final String className;

        Entry(Cn1Factory factory, int id, String className) {
            this.factory = factory;
            this.id = id;
            this.className = className;
        }

        Class<?> type() {
            if (factory != null) {
                return factory.cn1ServiceType(id);
            }
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                return null;
            }
        }

        Object create() {
            if (factory != null) {
                return factory.cn1CreateService(id);
            }
            try {
                return Class.forName(className).newInstance();
            } catch (ClassNotFoundException e) {
                return null;
            } catch (InstantiationException e) {
                return null;
            } catch (IllegalAccessException e) {
                return null;
            }
        }
    }

    /// Registers one provider of a service. The generated [CompatRegistry]
    /// calls this once per provider class, in the order the services files
    /// list them.
    ///
    /// #### Parameters
    ///
    /// - `service`: the service's class name, as `Class.getName()` answers
    ///   it in the application that ships
    ///
    /// - `factory`: creates the instance
    ///
    /// - `id`: what to pass to `factory` for this provider
    public static void cn1RegisterProvider(String service, Cn1Factory factory, int id) {
        if (service == null || factory == null) {
            throw new NullPointerException();
        }
        List<Entry> list = REGISTERED.get(service);
        if (list == null) {
            list = new ArrayList<Entry>();
            REGISTERED.put(service, list);
        }
        list.add(new Entry(factory, id, null));
    }

    /// Declares the registrations complete: from here on a service has the
    /// providers that were registered and no others, and no class is looked
    /// up by name. The generated [CompatRegistry] calls this after
    /// registering every provider the build found.
    public static void cn1Seal() {
        sealed = true;
    }

    /// Undoes [#cn1Seal()] and forgets every registration. For tests.
    public static void cn1Reset() {
        sealed = false;
        REGISTERED.clear();
    }

    /// The provider class names a services file lists, in order: each line
    /// up to its first `#`, trimmed, when anything is left, and a name that
    /// appears twice once. This is the rule the build applies to the files
    /// it reads, and the one applied here where no build step ran.
    ///
    /// A line that is not a class name -- it has a space or a tab inside, or
    /// a character no Java identifier has -- is left out, and added to
    /// `illegal` when that is not null. The JDK refuses the whole file for
    /// one.
    public static List<String> cn1ProviderNames(String text, List<String> illegal) {
        List<String> out = new ArrayList<String>();
        int n = text.length();
        int start = 0;
        while (start <= n) {
            int end = start;
            while (end < n && text.charAt(end) != '\n' && text.charAt(end) != '\r') {
                end++;
            }
            String line = text.substring(start, end);
            int hash = line.indexOf('#');
            if (hash >= 0) {
                line = line.substring(0, hash);
            }
            line = line.trim();
            if (line.length() > 0) {
                if (!isClassName(line)) {
                    if (illegal != null) {
                        illegal.add(line);
                    }
                } else if (!out.contains(line)) {
                    out.add(line);
                }
            }
            start = end + 1;
        }
        return out;
    }

    private static boolean isClassName(String name) {
        int n = name.length();
        for (int i = 0; i < n; i++) {
            char c = name.charAt(i);
            boolean letter = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || c == '$' || c >= 0x80;
            boolean digit = c >= '0' && c <= '9';
            if (i == 0 ? !letter : !(letter || digit || c == '.')) {
                return false;
            }
        }
        return n > 0;
    }

    /// A loader of the providers of `service`. Nothing is looked up or
    /// created until it is iterated.
    public static <S> ServiceLoader<S> load(Class<S> service) {
        return new ServiceLoader<S>(service, false);
    }

    /// As [#load(Class)]: an application has one class loader, so `loader`,
    /// which may be null as in the JDK, selects nothing.
    public static <S> ServiceLoader<S> load(Class<S> service, ClassLoader loader) {
        return new ServiceLoader<S>(service, false);
    }

    /// As [#load(Class)], for a layer that is not null. Nothing here hands
    /// out a module layer -- [Module#getLayer()] answers null -- so code
    /// reaches this only with the null the JDK refuses as well.
    public static <S> ServiceLoader<S> load(ModuleLayer layer, Class<S> service) {
        if (layer == null) {
            throw new NullPointerException();
        }
        return new ServiceLoader<S>(service, false);
    }

    /// The providers installed in the platform: on a desktop, the ones the
    /// platform class loader sees. An application's own providers are not
    /// among those, and a device installs none, so the loader is empty.
    public static <S> ServiceLoader<S> loadInstalled(Class<S> service) {
        return new ServiceLoader<S>(service, true);
    }

    private List<Entry> entries() {
        if (entries == null) {
            entries = installed ? new ArrayList<Entry>() : lookup(service.getName());
        }
        return entries;
    }

    private static List<Entry> lookup(String name) {
        CompatBoot.cn1Init();
        List<Entry> registered = REGISTERED.get(name);
        if (registered != null) {
            return new ArrayList<Entry>(registered);
        }
        List<Entry> out = new ArrayList<Entry>();
        if (sealed) {
            // The build registered every provider the application has.
            return out;
        }
        String text = read("/META-INF/services/" + name);
        if (text != null) {
            for (String provider : cn1ProviderNames(text, null)) {
                out.add(new Entry(null, 0, provider));
            }
        }
        return out;
    }

    private static String read(String resource) {
        InputStream in = Resources.open(resource);
        if (in == null) {
            return null;
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            for (int n = in.read(buffer); n >= 0; n = in.read(buffer)) {
                bytes.write(buffer, 0, n);
            }
            return new String(bytes.toByteArray(), "UTF-8");
        } catch (IOException e) {
            // A file that cannot be read lists nothing.
            return null;
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                // Nothing to do about a failed close of a stream already read.
            }
        }
    }

    /// The instance of `entry` as this service's type, or null when it could
    /// not be created or is not one. Tested rather than cast: a cast that
    /// fails does not throw on every device.
    @SuppressWarnings("unchecked")
    private S create(Entry entry) {
        Object made = entry.create();
        return service.isInstance(made) ? (S) made : null;
    }

    /// Whether a provider is left to create. Skips what cannot be one -- a
    /// class that is not there, or is not of the service's type -- without
    /// creating anything: `hasNext()` creates no provider in the JDK either.
    private boolean more() {
        List<Entry> all = entries();
        while (next < all.size()) {
            Class<?> type = all.get(next).type();
            if (type != null && service.isAssignableFrom(type)) {
                return true;
            }
            next++;
        }
        return false;
    }

    /// The providers, each created when `next()` reaches it. The instances
    /// an earlier iteration created come first, without being created again.
    @Override
    public Iterator<S> iterator() {
        return new Iterator<S>() {
            private int index;

            @Override
            public boolean hasNext() {
                return index < instances.size() || more();
            }

            @Override
            public S next() {
                if (index < instances.size()) {
                    return instances.get(index++);
                }
                while (more()) {
                    S made = create(entries().get(next++));
                    if (made != null) {
                        instances.add(made);
                        index = instances.size();
                        return made;
                    }
                }
                throw new NoSuchElementException();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public void forEach(Consumer<? super S> action) {
        if (action == null) {
            throw new NullPointerException();
        }
        Iterator<S> it = iterator();
        while (it.hasNext()) {
            action.accept(it.next());
        }
    }

    /// The first provider, created if it has not been, or an empty optional
    /// when the service has none.
    public Optional<S> findFirst() {
        Iterator<S> it = iterator();
        return it.hasNext() ? Optional.<S>of(it.next()) : Optional.<S>empty();
    }

    /// The providers, none of them created: [Provider#get()] creates one,
    /// anew on every call.
    public Stream<Provider<S>> stream() {
        List<Provider<S>> out = new ArrayList<Provider<S>>();
        for (Entry entry : entries()) {
            Class<?> type = entry.type();
            if (type != null && service.isAssignableFrom(type)) {
                out.add(new Lazy<S>(this, entry, type));
            }
        }
        return JdkCollections.defaultStream(out);
    }

    private static final class Lazy<S> implements Provider<S> {
        private final ServiceLoader<S> loader;
        private final Entry entry;
        private final Class<?> type;

        Lazy(ServiceLoader<S> loader, Entry entry, Class<?> type) {
            this.loader = loader;
            this.entry = entry;
            this.type = type;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Class<? extends S> type() {
            return (Class<? extends S>) type;
        }

        @Override
        public S get() {
            S made = loader.create(entry);
            if (made == null) {
                throw new ServiceConfigurationError(loader.service.getName() + ": Provider " + type.getName()
                        + " could not be instantiated");
            }
            return made;
        }
    }

    /// Forgets the providers created so far, so that the next iteration
    /// creates them again.
    public void reload() {
        instances.clear();
        next = 0;
        entries = null;
    }

    @Override
    public String toString() {
        return "java.util.ServiceLoader[" + service.getName() + "]";
    }
}
