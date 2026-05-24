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
package com.codename1.ai;

/**
 * Test-only bridge that re-exposes the package-private
 * {@code JsonHelper} surface from inside the core artifact.
 *
 * <p>This class lives in {@code com.codename1.ai} purposely -- Java
 * package-private access requires the *same package, same module*
 * in JPMS-aware builds, but plain javac compilation is satisfied by
 * the package match alone. The core artifact ships without
 * {@code module-info.java}, so this works.</p>
 */
final class JsonHelperBridge {

    private JsonHelperBridge() {
    }

    static String serialize(Object o) {
        return JsonHelper.serialize(o);
    }

    static Object rawJson(String s) {
        return new JsonHelper.RawJson(s);
    }
}
