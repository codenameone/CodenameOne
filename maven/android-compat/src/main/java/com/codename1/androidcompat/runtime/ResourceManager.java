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
package com.codename1.androidcompat.runtime;

import android.content.res.Configuration;
import android.util.DisplayMetrics;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.plaf.UIManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/// Owns the framework and application resource tables and the device
/// configuration they are resolved against. One per application; every
/// `Resources` object is a view onto it.
public final class ResourceManager {

    public static final String FRAMEWORK_TABLE = "/cn1_android_framework.bin";
    /// The AndroidX and Material resources the runtime ships, compiled once
    /// with it into package `0x7e`. An application sees them in its own
    /// namespace, as the Android Gradle plugin merges a library's resources
    /// into the application.
    public static final String LIBRARY_TABLE = "/cn1_android_library.bin";
    public static final int LIBRARY_PACKAGE_ID = 0x7e;

    private static ResourceManager instance;

    private ResTable framework;
    private ResTable library;
    private boolean libraryLoaded;
    private ResTable app;
    private final DeviceConfig device = new DeviceConfig();
    private final DisplayMetrics metrics = new DisplayMetrics();
    private final Configuration configuration = new Configuration();
    private final Map<Integer, Map<Integer, ResValue>> styleCache = new HashMap<Integer, Map<Integer, ResValue>>();
    private int styleCacheGeneration = -1;
    private String appTableName;

    private ResourceManager() {
    }

    public static ResourceManager get() {
        if (instance == null) {
            instance = new ResourceManager();
            instance.refresh();
        }
        return instance;
    }

    /// Installs the application table; called once by the runtime at startup.
    public void setAppTable(String resourceName) {
        appTableName = resourceName;
        app = null;
    }

    private ResTable load(String name) {
        InputStream in = Display.getInstance().getResourceAsStream(getClass(), name);
        if (in == null) {
            throw new IllegalStateException("Android resource table " + name + " is missing from the application. "
                    + "Was the project built with the compile-android-res and remap-android goals?");
        }
        try {
            try {
                return ResTable.read(in);
            } finally {
                in.close();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read Android resource table " + name + ": " + e.getMessage());
        }
    }

    public ResTable framework() {
        if (framework == null) {
            framework = load(FRAMEWORK_TABLE);
        }
        return framework;
    }

    /// The library table, or null when the runtime was built without one.
    public ResTable library() {
        if (!libraryLoaded) {
            libraryLoaded = true;
            InputStream probe = Display.getInstance().getResourceAsStream(getClass(), LIBRARY_TABLE);
            if (probe != null) {
                try {
                    probe.close();
                } catch (IOException e) {
                    // Only a probe; load() reports a real failure.
                }
                library = load(LIBRARY_TABLE);
            }
        }
        return library;
    }

    /// The application table, or null when the application has no resources
    /// (a build that never ran the resource compiler).
    public ResTable app() {
        if (app == null && appTableName != null) {
            app = load(appTableName.startsWith("/") ? appTableName : "/" + appTableName);
        }
        return app;
    }

    public ResTable.Entry entry(int id) {
        if ((id >>> 24) == 0x01) {
            return framework().get(id);
        }
        ResTable a = app();
        ResTable.Entry e = a == null ? null : a.get(id);
        if (e == null && (id >>> 24) == LIBRARY_PACKAGE_ID) {
            // An application resource named like a library one keeps the
            // library's id, so the application table is asked first: that is
            // how an application overrides a library value.
            ResTable l = library();
            e = l == null ? null : l.get(id);
        }
        return e;
    }

    /// The best variant of `id` for the current configuration, or null.
    public Object item(int id) {
        ResTable.Entry e = entry(id);
        return e == null ? null : e.best(device);
    }

    /// Looks a resource up by package, type and name: what `getIdentifier`
    /// answers.
    public int identifier(String pkg, String type, String name) {
        boolean fw = "android".equals(pkg);
        ResTable t = fw ? framework() : app();
        ResTable.Entry e = t == null ? null : t.get(type + "/" + name);
        if (e == null && !fw) {
            ResTable l = library();
            e = l == null ? null : l.get(type + "/" + name);
        }
        return e == null ? 0 : e.id;
    }

    /// The style `id` flattened over its parent chain: attr id -> value, a child
    /// overriding its parents. Cached until the configuration changes, because
    /// a style can have variants per qualifier.
    public Map<Integer, ResValue> style(int id) {
        if (styleCacheGeneration != device.generation) {
            styleCache.clear();
            styleCacheGeneration = device.generation;
        }
        Integer key = Integer.valueOf(id);
        Map<Integer, ResValue> m = styleCache.get(key);
        if (m != null) {
            return m;
        }
        m = new HashMap<Integer, ResValue>();
        styleCache.put(key, m);
        Object it = item(id);
        if (it instanceof ResTable.Bag) {
            ResTable.Bag b = (ResTable.Bag) it;
            if (b.parent != 0 && b.parent != id) {
                m.putAll(style(b.parent));
            }
            for (int i = 0; i < b.keys.length; i++) {
                m.put(Integer.valueOf(b.keys[i]), b.values[i]);
            }
        }
        return m;
    }

    public DeviceConfig device() {
        return device;
    }

    public DisplayMetrics metrics() {
        return metrics;
    }

    public Configuration configuration() {
        return configuration;
    }

    /// Re-reads the display: size, density, orientation, locale, dark mode
    /// and direction. Returns true when anything resources depend on changed.
    public boolean refresh() {
        Display d = Display.getInstance();
        float pxPerInch = d.convertToPixels(1000, true) * 25.4f / 1000f;
        if (pxPerInch <= 0) {
            pxPerInch = 160;
        }
        float density = pxPerInch / 160f;
        int w = d.getDisplayWidth();
        int h = d.getDisplayHeight();
        metrics.widthPixels = w;
        metrics.heightPixels = h;
        metrics.density = density;
        metrics.scaledDensity = density;
        metrics.densityDpi = Math.round(pxPerInch);
        metrics.xdpi = pxPerInch;
        metrics.ydpi = pxPerInch;

        String lang = "en";
        String region = "US";
        try {
            com.codename1.l10n.L10NManager l10n = d.getLocalizationManager();
            if (l10n != null) {
                String loc = l10n.getLocale();
                String[] parsed = languageAndRegion(loc);
                if (parsed != null) {
                    lang = parsed[0];
                    region = parsed[1];
                }
            }
        } catch (RuntimeException ignored) {
            // A port without localization support keeps the defaults.
        }
        boolean rtl = UIManager.getInstance().getLookAndFeel().isRTL();
        Boolean dark = CN.isDarkMode();
        int wDp = (int) (w / density);
        int hDp = (int) (h / density);
        int orientation = w > h ? 2 : 1;
        int night = dark != null && dark.booleanValue() ? 2 : 1;

        boolean changed = device.generation == 0 || !lang.equals(device.language) || !region.equals(device.region)
                || rtl != device.rtl || wDp != device.widthDp || hDp != device.heightDp
                || orientation != device.orientation || night != device.night
                || metrics.densityDpi != device.densityDpi;
        device.language = lang;
        device.region = region;
        device.rtl = rtl;
        device.widthDp = wDp;
        device.heightDp = hDp;
        device.smallestWidthDp = Math.min(wDp, hDp);
        device.orientation = orientation;
        device.night = night;
        device.densityDpi = metrics.densityDpi;
        if (changed) {
            device.generation++;
        }

        configuration.locale = new Locale(lang, region);
        configuration.orientation = orientation;
        configuration.uiMode = Configuration.UI_MODE_TYPE_NORMAL
                | (night == 2 ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO);
        configuration.screenWidthDp = wDp;
        configuration.screenHeightDp = hDp;
        configuration.smallestScreenWidthDp = Math.min(wDp, hDp);
        configuration.densityDpi = metrics.densityDpi;
        int sizeClass = configuration.smallestScreenWidthDp >= 720 ? Configuration.SCREENLAYOUT_SIZE_XLARGE
                : configuration.smallestScreenWidthDp >= 600 ? Configuration.SCREENLAYOUT_SIZE_LARGE
                : Configuration.SCREENLAYOUT_SIZE_NORMAL;
        configuration.screenLayout = sizeClass | (rtl ? Configuration.SCREENLAYOUT_LAYOUTDIR_RTL
                : Configuration.SCREENLAYOUT_LAYOUTDIR_LTR);
        return changed;
    }

    /// The language and region of a platform locale such as `en_US`,
    /// `fil_PH` or `pt-BR`: the language is everything before the separator,
    /// so three-letter languages (`fil`, matched by `values-fil`) survive.
    /// Null when there is no language.
    static String[] languageAndRegion(String loc) {
        if (loc == null) {
            return null;
        }
        int sep = loc.indexOf('_') >= 0 ? loc.indexOf('_') : loc.indexOf('-');
        String lang = sep < 0 ? loc : loc.substring(0, sep);
        if (lang.length() < 2) {
            return null;
        }
        String region = sep > 0 && loc.length() >= sep + 3 ? asciiUpper(loc.substring(sep + 1, sep + 3)) : "";
        return new String[] {asciiLower(lang), region};
    }

    static String asciiLower(String s) {
        char[] c = s.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (c[i] >= 'A' && c[i] <= 'Z') {
                c[i] = (char) (c[i] + 32);
            }
        }
        return new String(c);
    }

    static String asciiUpper(String s) {
        char[] c = s.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (c[i] >= 'a' && c[i] <= 'z') {
                c[i] = (char) (c[i] - 32);
            }
        }
        return new String(c);
    }
}
