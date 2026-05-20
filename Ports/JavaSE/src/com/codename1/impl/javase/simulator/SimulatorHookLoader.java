/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.impl.javase.simulator;

import com.codename1.system.SimulatorHookExecutor;
import com.codename1.ui.Display;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Discovers cn1lib-contributed simulator menu items by scanning the classpath
 * for {@code META-INF/codenameone/simulator-hooks.properties}. Each file
 * contributes one named group of items:
 *
 * <pre>
 * name=Bluetooth
 * namespace=bluetooth          # optional; defaults to slugified `name`
 *
 * item1.id=toggleAdapter       # optional; defaults to the property key (item1)
 * item1.label=Toggle adapter
 * item1.action=com.example.bt.sim.Hooks#toggleAdapter
 *
 * # Label omitted → API-only hook. Callable from tests via
 * # CN.executeHook("bluetooth:internalTrigger"), invisible in the menu.
 * item2.id=internalTrigger
 * item2.action=com.example.bt.sim.Hooks#internalTrigger
 * </pre>
 *
 * Actions are resolved to {@code public static void method()} via reflection
 * using the same classloader that loaded {@link Display}, so the method has
 * full access to {@code Display}, {@code NativeLookup}-installed impls, and
 * any cn1lib internals. Invocations are dispatched on the CN1 EDT via
 * {@link Display#callSerially(Runnable)} so hook authors can freely interact
 * with the running app.
 *
 * In addition to returning the hook list for the menu, every successful load
 * also registers each hook with {@link SimulatorHookExecutor} under its
 * {@code namespace:id} key so the cross-platform
 * {@link com.codename1.ui.CN#executeHook} entry point can drive it.
 */
public final class SimulatorHookLoader {

    private static final String RESOURCE_PATH = "META-INF/codenameone/simulator-hooks.properties";

    private SimulatorHookLoader() {}

    /**
     * Discovers all hooks visible to the JavaSE port's classloader (the one
     * that loaded {@link Display}). Safe to call multiple times; each call
     * re-scans the classpath and re-registers the executor. Errors in any
     * single file (missing keys, unresolvable class, no such method) are
     * logged and that entry is skipped; the rest are returned.
     */
    public static List<SimulatorHook> load() {
        ClassLoader cl = Display.class.getClassLoader();
        if (cl == null) {
            cl = ClassLoader.getSystemClassLoader();
        }
        return load(cl);
    }

    /**
     * Same as {@link #load()} but scans an explicit classloader. Primary
     * caller is tests that want to inject a fixture classpath; production
     * code should prefer {@link #load()}.
     */
    public static List<SimulatorHook> load(ClassLoader cl) {
        List<SimulatorHook> out = new ArrayList<SimulatorHook>();
        Enumeration<URL> urls;
        try {
            urls = cl.getResources(RESOURCE_PATH);
        } catch (IOException ex) {
            System.err.println("SimulatorHookLoader: failed to enumerate " + RESOURCE_PATH);
            ex.printStackTrace();
            // Reset the registry so a previous load doesn't survive a
            // failed scan (tests that mutate the classpath rely on this).
            SimulatorHookExecutor.register(Collections.<String, Runnable>emptyMap());
            return out;
        }
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            try {
                loadOne(url, cl, out);
            } catch (Throwable t) {
                System.err.println("SimulatorHookLoader: failed to parse " + url);
                t.printStackTrace();
            }
        }
        // Republish the registry so CN.executeHook reflects what we just loaded.
        Map<String, Runnable> registered = new LinkedHashMap<String, Runnable>();
        for (SimulatorHook h : out) {
            registered.put(h.getExecutorKey(), h.getInvoke());
        }
        SimulatorHookExecutor.register(registered);
        return out;
    }

    private static void loadOne(URL url, ClassLoader cl, List<SimulatorHook> out) throws IOException {
        OrderedProperties props = new OrderedProperties();
        InputStream in = url.openStream();
        try {
            // Reader form forces UTF-8; the default load(InputStream) is ISO-8859-1.
            props.load(new BufferedReader(new InputStreamReader(in, "UTF-8")));
        } finally {
            in.close();
        }
        String menuName = props.getProperty("name");
        if (menuName == null || menuName.trim().length() == 0) {
            System.err.println("SimulatorHookLoader: " + url + " is missing required 'name' property; skipping");
            return;
        }
        menuName = menuName.trim();
        String namespace = props.getProperty("namespace");
        if (namespace == null || namespace.trim().length() == 0) {
            namespace = slugify(menuName);
        } else {
            namespace = namespace.trim();
        }

        // Group keys by their "itemN" id, preserving declaration order.
        LinkedHashMap<String, String> labels = new LinkedHashMap<String, String>();
        LinkedHashMap<String, String> actions = new LinkedHashMap<String, String>();
        LinkedHashMap<String, String> ids = new LinkedHashMap<String, String>();
        for (String key : props.orderedKeys()) {
            if (key.endsWith(".label")) {
                labels.put(key.substring(0, key.length() - ".label".length()), props.getProperty(key));
            } else if (key.endsWith(".action")) {
                actions.put(key.substring(0, key.length() - ".action".length()), props.getProperty(key));
            } else if (key.endsWith(".id")) {
                ids.put(key.substring(0, key.length() - ".id".length()), props.getProperty(key));
            }
        }

        // The union of label-keys and id-keys is the set of declared items;
        // items can be label-less (API-only) or label-only (no explicit id,
        // defaults to the prefix). Walk in the order labels-then-id-only so
        // menu items come first when both forms appear in the same file.
        LinkedHashMap<String, Boolean> itemIds = new LinkedHashMap<String, Boolean>();
        for (String prefix : labels.keySet()) itemIds.put(prefix, Boolean.TRUE);
        for (String prefix : ids.keySet()) {
            if (!itemIds.containsKey(prefix)) itemIds.put(prefix, Boolean.TRUE);
        }

        for (String prefix : itemIds.keySet()) {
            String action = actions.get(prefix);
            if (action == null || action.trim().length() == 0) {
                System.err.println("SimulatorHookLoader: " + url + " item '" + prefix
                        + "' has no matching .action; skipping");
                continue;
            }
            String label = labels.get(prefix); // may be null (API-only)
            if (label != null) label = label.trim();
            String explicitId = ids.get(prefix);
            String id = explicitId != null && explicitId.trim().length() > 0
                    ? explicitId.trim()
                    : prefix;
            Runnable invoke = buildInvoker(cl, action.trim(), url);
            if (invoke == null) {
                continue;
            }
            out.add(new SimulatorHook(namespace, id, menuName, label, invoke));
        }
    }

    /**
     * Reduces a free-form menu name to a stable, ASCII-only namespace token.
     * Used when the properties file doesn't declare {@code namespace=...}
     * explicitly. Lowercases letters, keeps digits, replaces anything else
     * with a single hyphen, trims leading/trailing hyphens.
     */
    static String slugify(String name) {
        StringBuilder sb = new StringBuilder(name.length());
        boolean lastDash = true;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                sb.append((char)(c + 32));
                lastDash = false;
            } else if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                sb.append(c);
                lastDash = false;
            } else if (!lastDash) {
                sb.append('-');
                lastDash = true;
            }
        }
        // Strip trailing dash.
        int end = sb.length();
        while (end > 0 && sb.charAt(end - 1) == '-') end--;
        return sb.substring(0, end);
    }

    private static Runnable buildInvoker(ClassLoader cl, String action, URL source) {
        int hash = action.indexOf('#');
        if (hash <= 0 || hash == action.length() - 1) {
            System.err.println("SimulatorHookLoader: " + source + " has malformed action '" + action + "'; expected fqcn#methodName");
            return null;
        }
        String fqcn = action.substring(0, hash).trim();
        final String methodName = action.substring(hash + 1).trim();
        final Class<?> targetClass;
        final Method method;
        try {
            targetClass = Class.forName(fqcn, false, cl);
        } catch (ClassNotFoundException ex) {
            System.err.println("SimulatorHookLoader: " + source + " references unknown class '" + fqcn + "'");
            return null;
        }
        try {
            method = targetClass.getDeclaredMethod(methodName, new Class<?>[0]);
        } catch (NoSuchMethodException ex) {
            System.err.println("SimulatorHookLoader: " + source + " references unknown no-arg method '" + fqcn + "#" + methodName + "'");
            return null;
        }
        if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
            System.err.println("SimulatorHookLoader: " + source + " references non-static method '" + fqcn + "#" + methodName + "'");
            return null;
        }
        method.setAccessible(true);
        final URL src = source;
        return new Runnable() {
            @Override
            public void run() {
                Display.getInstance().callSerially(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            method.invoke(null);
                        } catch (Throwable t) {
                            System.err.println("SimulatorHookLoader: action from " + src + " threw");
                            t.printStackTrace();
                        }
                    }
                });
            }
        };
    }

    /**
     * Subclass of Properties that records insertion order so the menu reflects
     * the order keys appear in the file rather than hash order. We only rely
     * on {@code load(Reader)}, which routes through {@link #put(Object, Object)}.
     */
    private static final class OrderedProperties extends Properties {
        private final LinkedHashMap<String, String> ordered = new LinkedHashMap<String, String>();

        @Override
        public synchronized Object put(Object key, Object value) {
            if (key instanceof String && value instanceof String) {
                ordered.put((String) key, (String) value);
            }
            return super.put(key, value);
        }

        List<String> orderedKeys() {
            return Collections.unmodifiableList(new ArrayList<String>(ordered.keySet()));
        }
    }
}
