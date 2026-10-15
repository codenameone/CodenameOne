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
package java.util;

import com.codename1.impl.JdkHost;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

/// The providers of a service, as the `META-INF/services` files of the
/// application and of the libraries bundled with it list them.
///
/// #### Where the providers come from
///
/// A device discovers nothing at run time and creates no class from its
/// name. The build reads every `META-INF/services/<service>` file the
/// application ships and generates a registry with the application that
/// names the providers of each service, in class path order, and creates each
/// of them with `new` (`com.codename1.impl.JdkHost.Registry`). So the set of
/// providers is fixed when the application is built. A listed class that the
/// application does not ship, that is abstract or that has no public
/// constructor without parameters is left out by the build with a warning;
/// the JDK throws `ServiceConfigurationError` for such a provider when the
/// iteration reaches it, and here it is simply not there.
///
/// Where no registry was generated -- a program translated without Codename
/// One, a test -- the services file is read through the platform
/// (`JdkHost.resource`), as the JDK reads it, and the classes are created
/// from their names.
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
/// The class loader a loader is asked for selects nothing: an application
/// has one. Module layers, and providers that are a static `provider()`
/// method of a named module, are not provided.
public final class ServiceLoader<S> implements Iterable<S> {

    private final Class<S> service;

    /// The provider class names of this service, or null until an iteration asks.
    private List<String> entries;

    /// The instances created so far, in order.
    private final List<S> instances = new ArrayList<S>();

    /// The index in [#entries] of the next provider to create.
    private int next;

    /// Counts the reloads, so that an iterator taken before one can tell.
    private int generation;

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
        S get();
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

    private List<String> entries() {
        if (entries == null) {
            entries = installed ? new ArrayList<String>() : lookup(service.getName());
        }
        return entries;
    }

    private static List<String> lookup(String name) {
        List<String> out = new ArrayList<String>();
        JdkHost host = JdkHost.get();
        if (JdkHost.getRegistry() != null) {
            // The build registered every provider the application has.
            String[] registered = host.providers(name);
            for (int i = 0; i < registered.length; i++) {
                out.add(registered[i]);
            }
            return out;
        }
        String text = read(host.resource("META-INF/services/" + name));
        if (text != null) {
            providerNames(text, out);
        }
        return out;
    }

    /// The provider class names a services file lists, in order: each line
    /// up to its first `#`, trimmed, when anything is left, and a name that
    /// appears twice once. A line that is not a class name is left out; the
    /// JDK refuses the whole file for one.
    private static void providerNames(String text, List<String> out) {
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
            if (line.length() > 0 && isClassName(line) && !out.contains(line)) {
                out.add(line);
            }
            start = end + 1;
        }
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

    private static String read(InputStream in) {
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

    /// The class of the provider `className`, or null when there is none.
    private static Class<?> typeOf(String className) {
        if (JdkHost.getRegistry() != null) {
            return JdkHost.get().type(className);
        }
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /// A new instance of the provider `className`, or null when none can be made.
    private static Object instantiate(String className) {
        if (JdkHost.getRegistry() != null) {
            return JdkHost.get().newInstance(className);
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

    /// The instance of `className` as this service's type, or null when it
    /// could not be created or is not one. Tested rather than cast: a cast
    /// that fails does not throw on every device.
    @SuppressWarnings("unchecked")
    private S create(String className) {
        Object made = instantiate(className);
        return service.isInstance(made) ? (S) made : null;
    }

    /// Whether a provider is left to create. Skips what cannot be one -- a
    /// class that is not there, or is not of the service's type -- without
    /// creating anything: `hasNext()` creates no provider in the JDK either.
    private boolean more() {
        List<String> all = entries();
        while (next < all.size()) {
            Class<?> type = typeOf(all.get(next));
            if (type != null && service.isAssignableFrom(type)) {
                return true;
            }
            next++;
        }
        return false;
    }

    /// The providers, each created when `next()` reaches it. The instances
    /// an earlier iteration created come first, without being created again.
    public Iterator<S> iterator() {
        return new ProviderIterator<S>(this);
    }

    /// The iterator [#iterator()] answers. A named class: the class library
    /// is translated without lambdas, and an anonymous one reads no better.
    private static final class ProviderIterator<S> implements Iterator<S> {
        private final ServiceLoader<S> loader;
        private final int born;
        private int index;

        ProviderIterator(ServiceLoader<S> loader) {
            this.loader = loader;
            this.born = loader.generation;
        }

        /// After a reload this iterator's place means nothing: the instances
        /// it walked are gone and the providers are counted from the start
        /// again. Going on would create a provider it has already handed
        /// out, so it refuses, as the JDK's does.
        private void check() {
            if (born != loader.generation) {
                throw new ConcurrentModificationException();
            }
        }

        public boolean hasNext() {
            check();
            return index < loader.instances.size() || loader.more();
        }

        public S next() {
            check();
            if (index < loader.instances.size()) {
                return loader.instances.get(index++);
            }
            while (loader.more()) {
                S made = loader.create(loader.entries().get(loader.next++));
                if (made != null) {
                    loader.instances.add(made);
                    index = loader.instances.size();
                    return made;
                }
            }
            throw new NoSuchElementException();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

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
        List<String> all = entries();
        for (int i = 0; i < all.size(); i++) {
            String className = all.get(i);
            Class<?> type = typeOf(className);
            if (type != null && service.isAssignableFrom(type)) {
                out.add(new Lazy<S>(this, className, type));
            }
        }
        return out.stream();
    }

    private static final class Lazy<S> implements Provider<S> {
        private final ServiceLoader<S> loader;
        private final String className;
        private final Class<?> type;

        Lazy(ServiceLoader<S> loader, String className, Class<?> type) {
            this.loader = loader;
            this.className = className;
            this.type = type;
        }

        @SuppressWarnings("unchecked")
        public Class<? extends S> type() {
            return (Class<? extends S>) type;
        }

        public S get() {
            S made = loader.create(className);
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
        generation++;
        instances.clear();
        next = 0;
        entries = null;
    }

    public String toString() {
        return "java.util.ServiceLoader[" + service.getName() + "]";
    }
}
