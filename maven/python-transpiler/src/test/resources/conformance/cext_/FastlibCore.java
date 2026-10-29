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
package com.codename1.python.ext;

import com.codename1.python.runtime.NativeModule;
import com.codename1.python.runtime.Py;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class FastlibCore {
    private FastlibCore() {
    }

    public static void install() {
        new NativeModule("fastlib._core")
                .constant("VERSION", "1.0")
                .function("fast_sum", args -> {
                    if (!(args[0] instanceof List)) {
                        throw Py.error(Py.TYPE_ERROR, "fast_sum() needs a list");
                    }
                    long s = 0;
                    for (Object o : (List<?>) args[0]) {
                        s += ((Number) o).longValue();
                    }
                    return s;
                })
                .function("scale", args -> {
                    double k = ((Number) args[1]).doubleValue();
                    List<Object> out = new ArrayList<Object>();
                    for (Object o : (List<?>) args[0]) {
                        out.add(((Number) o).doubleValue() * k);
                    }
                    return out.toArray();
                })
                .function("join_words", args -> String.join("-", (List<String>) args[0]))
                .function("describe", args -> {
                    List<Object> out = new ArrayList<Object>();
                    for (Map.Entry<?, ?> e : new TreeMap<Object, Object>((Map<?, ?>) args[0]).entrySet()) {
                        out.add(new Object[] {e.getKey(), e.getValue()});
                    }
                    return out;
                })
                .install();
    }
}
