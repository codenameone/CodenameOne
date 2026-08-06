/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.hardening;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Guards against a ParparVM name collision. The translator mangles a Java class
 * name to a C symbol by turning {@code '.'}, {@code '/'} and {@code '$'} into
 * {@code '_'} ({@code ByteCodeClass}), so two distinct classes whose names differ
 * only in those separators -- {@code a.b_c} and {@code a.b.c} -- collapse to the
 * same C symbol {@code a_b_c} and the native build fails confusingly. The
 * {@link Cn1NameFactory} dictionary never emits {@code '_'}, so a collision should
 * be impossible; this check makes that a guarantee rather than an assumption.
 */
public final class MangleCollisionCheck {

    private MangleCollisionCheck() {
    }

    /**
     * @param internalNames output class names in internal form ({@code a/b/C})
     * @throws HardeningException naming the two classes that collide
     */
    public static void check(Set<String> internalNames) throws HardeningException {
        Map<String, String> byMangled = new HashMap<String, String>();
        for (String name : internalNames) {
            String mangled = mangle(name);
            String prev = byMangled.put(mangled, name);
            if (prev != null) {
                throw new HardeningException("Obfuscated class names '" + prev + "' and '" + name
                        + "' both mangle to the ParparVM C symbol '" + mangled
                        + "'. This would break the native build.");
            }
        }
    }

    static String mangle(String internalName) {
        StringBuilder b = new StringBuilder(internalName.length());
        for (int i = 0; i < internalName.length(); i++) {
            char c = internalName.charAt(i);
            if (c == '/' || c == '.' || c == '$') {
                b.append('_');
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }
}
