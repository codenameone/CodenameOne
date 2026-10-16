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
package com.codename1.flutter.material;

import com.codename1.flutter.Locale;
import com.codename1.flutter.l10n.LocalizationsDelegate;

import dart.core.DartList;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A rebuilt MaterialApp with a same-typed delegate kept the old resources whatever the
 * new delegate said: Flutter asks {@code newDelegate.shouldReload(oldDelegate)}, and
 * the scope compared runtime classes only.
 */
class LocalizationsReloadTest {

    static final class Configurable extends LocalizationsDelegate<Object> {
        final String flavour;

        Configurable(String flavour) {
            this.flavour = flavour;
        }

        @Override
        public boolean shouldReload(LocalizationsDelegate<Object> old) {
            return !flavour.equals(((Configurable) old).flavour);
        }
    }

    private static LocalizationsScope scope(LocalizationsDelegate<?> d) {
        LocalizationsScope s = new LocalizationsScope(new ArrayList<Object>());
        s.reloadKey(new Locale("en", null), DartList.of((Object) d));
        return s;
    }

    @Test
    void aSameTypedDelegateThatAsksToReloadIsNotTheSameResources() {
        assertFalse(scope(new Configurable("b")).sameResourcesAs(scope(new Configurable("a"))));
    }

    @Test
    void aSameTypedDelegateThatDoesNotAskKeepsTheResources() {
        assertTrue(scope(new Configurable("a")).sameResourcesAs(scope(new Configurable("a"))));
    }
}
