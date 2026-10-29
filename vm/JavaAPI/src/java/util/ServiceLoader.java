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

/// The providers of a service, as in the JDK -- declared at build time.
///
/// A device loads no classes at run time, so there is no class path to search. The
/// translator reads every `META-INF/services` file among the classes it translates and
/// generates [Registry#providers(Class)], which instantiates the providers a file
/// names, in the order it names them; a provider that is not among the translated
/// classes is left out. A service with no file has no providers. The class loader
/// argument is accepted and ignored: there is only one.
public final class ServiceLoader<S> implements Iterable<S> {
    private final Class<S> service;
    private List<S> providers;

    private ServiceLoader(Class<S> service) {
        this.service = service;
    }

    public static <S> ServiceLoader<S> load(Class<S> service) {
        return new ServiceLoader<S>(service);
    }

    public static <S> ServiceLoader<S> load(Class<S> service, ClassLoader loader) {
        return new ServiceLoader<S>(service);
    }

    public static <S> ServiceLoader<S> loadInstalled(Class<S> service) {
        return new ServiceLoader<S>(service);
    }

    public void reload() {
        providers = null;
    }

    @SuppressWarnings("unchecked")
    public Iterator<S> iterator() {
        if (providers == null) {
            Object[] found = Registry.providers(service);
            List<S> l = new ArrayList<S>(found.length);
            for (int i = 0; i < found.length; i++) {
                l.add((S) found[i]);
            }
            providers = l;
        }
        return Collections.unmodifiableList(providers).iterator();
    }

    public String toString() {
        return "java.util.ServiceLoader[" + service.getName() + "]";
    }

    /// Replaced by the translator with the providers of every `META-INF/services`
    /// file it was given; this body is what an application with none of them gets.
    static final class Registry {
        private Registry() {
        }

        static Object[] providers(Class<?> service) {
            return new Object[0];
        }
    }
}
