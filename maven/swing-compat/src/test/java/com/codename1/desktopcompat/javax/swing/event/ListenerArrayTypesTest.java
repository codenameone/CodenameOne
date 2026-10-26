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
package com.codename1.desktopcompat.javax.swing.event;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/// Every listener type the layer passes to `EventListenerList.getListeners`
/// has its array class named in that class's table.
///
/// `getListeners` creates its result reflectively, and ParparVM can only do
/// that for a type whose array class some bytecode names. Nothing on a JVM
/// shows a missing one -- the simulator creates array classes on demand -- so
/// this reads the call sites out of the sources instead: a new
/// `getListeners(FooListener.class)` in the layer without `FooListener[].class`
/// in the table fails here rather than on a device.
public class ListenerArrayTypesTest {

    private static final Pattern CALL = Pattern.compile("getListeners\\(\\s*([A-Za-z0-9_.]+)\\.class\\s*\\)");

    @Test
    public void everyListenerTypeTheLayerAsksForHasAnArrayClass() throws IOException {
        Set<String> registered = new HashSet<String>();
        for (Class<?> array : EventListenerList.listenerArrayTypes()) {
            assertTrue(array + " is not an array class", array.isArray());
            registered.add(array.getComponentType().getSimpleName());
        }
        File root = new File(System.getProperty("swingcompat.sources", "src/main/java"));
        Set<String> asked = new TreeSet<String>();
        int files = collect(root, asked);
        assertTrue("no sources under " + root.getAbsolutePath(), files > 0);
        assertTrue("the scan found no call site at all; the pattern no longer matches", asked.size() > 5);
        Set<String> missing = new TreeSet<String>(asked);
        missing.removeAll(registered);
        assertTrue("getListeners is called with listener types whose array class EventListenerList "
                + "does not name, so the call throws on a device: " + missing, missing.isEmpty());
    }

    private static int collect(File dir, Set<String> asked) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            return 0;
        }
        int count = 0;
        for (File f : files) {
            if (f.isDirectory()) {
                count += collect(f, asked);
            } else if (f.getName().endsWith(".java")) {
                count++;
                String text = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                Matcher m = CALL.matcher(text);
                while (m.find()) {
                    String name = m.group(1);
                    asked.add(name.substring(name.lastIndexOf('.') + 1));
                }
            }
        }
        return count;
    }
}
