/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

import dart.async.Future;
import dart.runtime.Funcs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@code confirmDismiss} answers a {@code Future<bool?>}, and Flutter dismisses only on
 * {@code true}: its Dismissible reads the answer as {@code result ?? false}. A null --
 * a confirmation dialog closed without a choice -- used to count as consent.
 */
class DismissibleConfirmTest {

    private static Boolean decision(final Object answer) {
        Dismissible w = new Dismissible();
        w.child(new com.codename1.flutter.testsupport.ProbeBox(10, 10));
        w.confirmDismiss(new Funcs.Func1<DismissDirection, Object>() {
            @Override
            public Object call(DismissDirection d) {
                return answer;
            }
        });
        DismissibleRenderElement e = new DismissibleRenderElement(w);
        final Boolean[] seen = new Boolean[1];
        e.confirm(null, new Funcs.VoidFunc1<Boolean>() {
            @Override
            public void call(Boolean ok) {
                seen[0] = ok;
            }
        });
        return seen[0];
    }

    @Test
    @DisplayName("a Future completing with null vetoes the dismissal")
    void asyncNullVetoes() {
        assertEquals(Boolean.FALSE, decision(Future.value(null)));
    }

    @Test
    @DisplayName("a synchronous null vetoes the dismissal")
    void syncNullVetoes() {
        assertEquals(Boolean.FALSE, decision(null));
    }

    @Test
    @DisplayName("true, sync or async, still dismisses; false still vetoes")
    void trueConsentsFalseVetoes() {
        assertEquals(Boolean.TRUE, decision(Future.value(Boolean.TRUE)));
        assertEquals(Boolean.TRUE, decision(Boolean.TRUE));
        assertEquals(Boolean.FALSE, decision(Future.value(Boolean.FALSE)));
        assertEquals(Boolean.FALSE, decision(Boolean.FALSE));
    }
}
