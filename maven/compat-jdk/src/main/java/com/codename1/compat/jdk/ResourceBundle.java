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
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/// `java.util.ResourceBundle` for the Codename One runtime: locale specific
/// strings and objects, looked up by key, with a chain of less specific
/// bundles behind each one.
///
/// #### How a bundle is found
///
/// `getBundle("com.example.Messages", locale)` looks for three bundles, most
/// specific first -- `Messages_<language>_<COUNTRY>`, `Messages_<language>`
/// and `Messages` -- and chains the ones that exist, so a key missing from
/// the first is answered by the next. When nothing more specific than the
/// base bundle exists for the requested locale, the default locale is tried
/// the same way before the base bundle is settled for. Finding none at all
/// throws `MissingResourceException`.
///
/// Each of the three is resolved in this order:
///
/// 1. A resource registered through [#cn1Register(String, String, String)].
/// 2. A class of that name which extends `ResourceBundle`, a
///    `ListResourceBundle` typically, created through its public no-argument
///    constructor.
/// 3. The resource `/com/example/Messages_<suffix>.properties`, read through
///    `Class.getResourceAsStream`.
///
/// #### Flat resource namespaces
///
/// Some Codename One targets package resources in one flat directory, where
/// `/com/example/Messages_fr.properties` does not exist under that name.
/// `cn1Register` is the hook for that: the build step that flattens the
/// resources generates one call per bundle file, mapping the bundle to the
/// name it was given.
///
/// Bundles are cached for the life of the application, or until
/// [#clearCache()]. `ResourceBundle.Control`, and the lookups that take one,
/// are not provided.
public abstract class ResourceBundle {

    /// Bundle files a build step has renamed, keyed by base name and locale
    /// suffix.
    private static final Map<String, String> REGISTERED = new HashMap<String, String>();

    /// Every bundle already loaded, under the same key. `MISSING` records a
    /// bundle that is known not to exist, so it is looked for once.
    private static final Map<String, Object> CACHE = new HashMap<String, Object>();

    private static final Object MISSING = new Object();

    /// The bundle consulted for a key this one does not have, or `null`.
    protected ResourceBundle parent;

    private Locale locale;
    private String baseName;

    public ResourceBundle() {
    }

    /// Maps one bundle file to the resource it is packaged as. A build step
    /// that flattens or renames resources calls this, before the application
    /// asks for the bundle, once for every `.properties` file it moved.
    ///
    /// #### Parameters
    ///
    /// - `baseName`: the bundle's base name as `getBundle` receives it,
    ///   `com.example.Messages`. The path form `com/example/Messages` is
    ///   accepted too.
    ///
    /// - `localeSuffix`: the part of the file name that selects the locale,
    ///   with its leading underscore -- `_fr`, `_fr_CA` -- or the empty string
    ///   for the base bundle.
    ///
    /// - `resourceName`: the name to pass to `Class.getResourceAsStream`,
    ///   `/Messages_fr.properties`. A missing leading slash is added.
    public static void cn1Register(String baseName, String localeSuffix, String resourceName) {
        if (baseName == null || resourceName == null) {
            throw new NullPointerException();
        }
        String suffix = localeSuffix == null ? "" : localeSuffix;
        String resource = resourceName.startsWith("/") ? resourceName : "/" + resourceName;
        String key = cacheKey(baseName.replace('/', '.'), suffix);
        REGISTERED.put(key, resource);
        CACHE.remove(key);
    }

    public static final ResourceBundle getBundle(String baseName) {
        return getBundle(baseName, Locale.getDefault());
    }

    /// Looks the bundle up as `getBundle(String, Locale)` does. The class
    /// loader is not used: an application has one.
    public static ResourceBundle getBundle(String baseName, Locale locale, ClassLoader loader) {
        return getBundle(baseName, locale);
    }

    public static final ResourceBundle getBundle(String baseName, Locale locale) {
        if (baseName == null || locale == null) {
            throw new NullPointerException();
        }
        String name = baseName.replace('/', '.');
        String language = nonNull(locale.getLanguage());
        String country = nonNull(locale.getCountry());
        ResourceBundle found = findChain(name, language, country);
        if (found == null || found.locale.getLanguage().length() == 0) {
            // Nothing specific to the requested locale: the default locale's
            // bundle is preferred to the base bundle.
            Locale fallback = Locale.getDefault();
            String fallbackLanguage = nonNull(fallback.getLanguage());
            String fallbackCountry = nonNull(fallback.getCountry());
            boolean requestedRoot = language.length() == 0;
            boolean sameLocale = fallbackLanguage.equals(language) && fallbackCountry.equals(country);
            if (!sameLocale && !(requestedRoot && found != null)) {
                ResourceBundle viaDefault = findChain(name, fallbackLanguage, fallbackCountry);
                if (viaDefault != null) {
                    found = viaDefault;
                }
            }
        }
        if (found == null) {
            String localeName = localeSuffix(language, country);
            throw new MissingResourceException(
                    "Can't find bundle for base name " + baseName + ", locale "
                            + (localeName.length() == 0 ? "" : localeName.substring(1)),
                    baseName + localeName, "");
        }
        return found;
    }

    public static final void clearCache() {
        CACHE.clear();
    }

    private static String nonNull(String s) {
        return s == null ? "" : s;
    }

    private static String cacheKey(String baseName, String suffix) {
        return baseName + '\n' + suffix;
    }

    private static String localeSuffix(String language, String country) {
        if (language.length() == 0) {
            return "";
        }
        return country.length() == 0 ? "_" + language : "_" + language + "_" + country;
    }

    /// The most specific bundle that exists for the locale, with the less
    /// specific ones chained behind it, or `null` when not even the base
    /// bundle exists.
    private static ResourceBundle findChain(String baseName, String language, String country) {
        ResourceBundle result = load(baseName, "", "", "", null);
        if (language.length() > 0) {
            ResourceBundle forLanguage = load(baseName, "_" + language, language, "", result);
            if (forLanguage != null) {
                result = forLanguage;
            }
            if (country.length() > 0) {
                ResourceBundle forCountry =
                        load(baseName, "_" + language + "_" + country, language, country, result);
                if (forCountry != null) {
                    result = forCountry;
                }
            }
        }
        return result;
    }

    private static ResourceBundle load(String baseName, String suffix, String language, String country,
                                       ResourceBundle parent) {
        String key = cacheKey(baseName, suffix);
        Object cached = CACHE.get(key);
        if (cached instanceof ResourceBundle) {
            return (ResourceBundle) cached;
        }
        if (cached != null) {
            return null;
        }
        ResourceBundle bundle = create(baseName, suffix, REGISTERED.get(key));
        if (bundle == null) {
            CACHE.put(key, MISSING);
            return null;
        }
        if (bundle.parent == null) {
            // The less specific bundle of a name is the same whichever locale
            // led here, so the chain is fixed the first time it is built.
            bundle.parent = parent;
        }
        bundle.locale = new Locale(language, country);
        bundle.baseName = baseName;
        CACHE.put(key, bundle);
        return bundle;
    }

    private static ResourceBundle create(String baseName, String suffix, String registered) {
        if (registered != null) {
            ResourceBundle fromRegistered = read(registered);
            if (fromRegistered != null) {
                return fromRegistered;
            }
        }
        try {
            Object instance = Class.forName(baseName + suffix).newInstance();
            if (instance instanceof ResourceBundle) {
                return (ResourceBundle) instance;
            }
        } catch (ClassNotFoundException e) {
            // No class of this name: the bundle is a properties file, or absent.
        } catch (InstantiationException e) {
            // Not a class this lookup can create; fall through to the file.
        } catch (IllegalAccessException e) {
            // As above.
        }
        return read("/" + baseName.replace('.', '/') + suffix + ".properties");
    }

    private static ResourceBundle read(String resource) {
        InputStream in = ResourceBundle.class.getResourceAsStream(resource);
        if (in == null) {
            return null;
        }
        try {
            return new PropertyResourceBundle(in);
        } catch (IOException e) {
            // A bundle that cannot be read is a bundle that is not there.
            return null;
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                // Nothing to do about a failed close of a stream already read.
            }
        }
    }

    public final String getString(String key) {
        Object value = getObject(key);
        if (value instanceof String) {
            return (String) value;
        }
        throw new ClassCastException("Resource " + key + " is not a String");
    }

    public final String[] getStringArray(String key) {
        Object value = getObject(key);
        if (value instanceof String[]) {
            return (String[]) value;
        }
        throw new ClassCastException("Resource " + key + " is not a String array");
    }

    public final Object getObject(String key) {
        if (key == null) {
            throw new NullPointerException();
        }
        for (ResourceBundle b = this; b != null; b = b.parent) {
            Object value = b.handleGetObject(key);
            if (value != null) {
                return value;
            }
        }
        throw new MissingResourceException(
                "Can't find resource for bundle " + getClass().getName() + ", key " + key,
                getClass().getName(), key);
    }

    /// The locale this bundle was found for, which may be less specific than
    /// the one asked for. The base bundle's has an empty language.
    public Locale getLocale() {
        return locale;
    }

    public String getBaseBundleName() {
        return baseName;
    }

    protected void setParent(ResourceBundle parent) {
        this.parent = parent;
    }

    public boolean containsKey(String key) {
        if (key == null) {
            throw new NullPointerException();
        }
        for (ResourceBundle b = this; b != null; b = b.parent) {
            if (b.handleKeySet().contains(key)) {
                return true;
            }
        }
        return false;
    }

    /// Every key of this bundle and of the bundles behind it.
    public Set<String> keySet() {
        Set<String> keys = new HashSet<String>();
        for (ResourceBundle b = this; b != null; b = b.parent) {
            keys.addAll(b.handleKeySet());
        }
        return keys;
    }

    /// The keys this bundle itself defines. The default derives them from
    /// [#getKeys()]; a subclass that has them at hand should override it.
    protected Set<String> handleKeySet() {
        Set<String> keys = new HashSet<String>();
        Enumeration<String> all = getKeys();
        while (all.hasMoreElements()) {
            String key = all.nextElement();
            if (handleGetObject(key) != null) {
                keys.add(key);
            }
        }
        return keys;
    }

    protected abstract Object handleGetObject(String key);

    public abstract Enumeration<String> getKeys();

    /// An `Enumeration` over an iterator, for the `getKeys` implementations.
    static final class KeyEnumeration implements Enumeration<String> {
        private final Iterator<String> it;

        KeyEnumeration(Iterator<String> it) {
            this.it = it;
        }

        @Override
        public boolean hasMoreElements() {
            return it.hasNext();
        }

        @Override
        public String nextElement() {
            return it.next();
        }
    }
}
