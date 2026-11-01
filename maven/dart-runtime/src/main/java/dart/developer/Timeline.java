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
package dart.developer;

import dart.runtime.Funcs;

/**
 * dart:developer's {@code Timeline}. There is no timeline recorder to write to, so the
 * events are dropped; {@code timeSync} still runs its function and answers its result,
 * which is the part a program depends on.
 */
public final class Timeline {
    private Timeline() {
    }

    public static void startSync(String name, Object arguments, Object flow) {
    }

    public static void finishSync() {
    }

    public static void instantSync(String name, Object arguments) {
    }

    public static <T> T timeSync(String name, Funcs.Func0<T> function, Object arguments, Object flow) {
        return function.call();
    }
}
