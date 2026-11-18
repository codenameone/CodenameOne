/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.widgets;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * {@code WidgetsBinding} and its {@code PlatformDispatcher} each hold the
 * other's singleton, so which class a program touches first decides the order
 * their initializers run in. One order used to leave
 * {@code WidgetsBinding.instance} null inside the dispatcher's initializer,
 * and the gallery -- which reaches the binding first -- died in its first
 * layout on every target.
 *
 * <p>A class is initialized once per class loader, so each order gets a loader
 * of its own; in the test JVM's loader whichever test ran earlier would have
 * decided the order already.</p>
 */
class WidgetsBindingInitOrderTest {

    private static final String BINDING = "com.codename1.flutter.widgets.WidgetsBinding";
    private static final String DISPATCHER = BINDING + "$PlatformDispatcher";

    private static URLClassLoader freshLoader() throws Exception {
        List<URL> urls = new ArrayList<URL>();
        for (String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
            urls.add(new File(entry).toURI().toURL());
        }
        // The platform loader as parent: the JDK is shared, nothing of ours is.
        return new URLClassLoader(urls.toArray(new URL[0]), ClassLoader.getPlatformClassLoader());
    }

    private static void assertOneDispatcher(ClassLoader loader) throws Exception {
        Class<?> binding = Class.forName(BINDING, true, loader);
        Class<?> dispatcher = Class.forName(DISPATCHER, true, loader);
        Object instance = binding.getField("instance").get(null);
        assertNotNull(instance, "WidgetsBinding.instance");
        Object fromBinding = binding.getMethod("platformDispatcher").invoke(instance);
        assertNotNull(fromBinding, "WidgetsBinding.instance.platformDispatcher()");
        assertSame(dispatcher.getField("instance").get(null), fromBinding,
                "PlatformDispatcher.instance is the binding's dispatcher");
    }

    @Test
    void bindingTouchedFirst() throws Exception {
        try (URLClassLoader loader = freshLoader()) {
            Class.forName(BINDING, true, loader);
            assertOneDispatcher(loader);
        }
    }

    @Test
    void dispatcherTouchedFirst() throws Exception {
        try (URLClassLoader loader = freshLoader()) {
            Class.forName(DISPATCHER, true, loader);
            assertOneDispatcher(loader);
        }
    }
}
