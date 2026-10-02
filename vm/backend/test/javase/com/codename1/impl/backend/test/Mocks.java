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
package com.codename1.impl.backend.test;

import java.lang.reflect.Method;

/// `@MockitoBean`'s mocks, through Mockito -- reached by reflection, so this jar
/// does not depend on Mockito and a project that mocks nothing needs none.
public final class Mocks {
    private Mocks() {
    }

    /// A Mockito mock of `type`.
    public static Object create(Class<?> type) {
        try {
            Method mock = mockito().getMethod("mock", Class.class);
            return mock.invoke(null, type);
        } catch (ReflectiveOperationException err) {
            throw new IllegalStateException("Could not mock " + type.getName() + ": " + err, err);
        }
    }

    /// Clears a mock's stubbing and recorded calls, between tests.
    public static void reset(Object mock) {
        if (mock == null) {
            return;
        }
        try {
            Method reset = mockito().getMethod("reset", Object[].class);
            reset.invoke(null, new Object[] {new Object[] {mock}});
        } catch (ReflectiveOperationException err) {
            throw new IllegalStateException("Could not reset a mock: " + err, err);
        }
    }

    private static Class<?> mockito() {
        try {
            return Class.forName("org.mockito.Mockito");
        } catch (ClassNotFoundException missing) {
            throw new IllegalStateException("@MockitoBean needs Mockito on the test classpath: "
                    + "add org.mockito:mockito-core with test scope.", missing);
        }
    }
}
