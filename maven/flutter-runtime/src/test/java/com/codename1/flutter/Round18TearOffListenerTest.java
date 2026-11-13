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
package com.codename1.flutter;

import com.codename1.flutter.foundation.ChangeNotifier;
import com.codename1.flutter.foundation.ValueNotifier;

import dart.runtime.TearOff;
import dart.runtime.Funcs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// `addListener(_onTick)` then `removeListener(_onTick)`: the transpiler makes each
/// tear-off a new object, equal to the other by Dart's `==` (dart.runtime.TearOff),
/// so the notifiers must remove by that `==`, not by Java identity.
class Round18TearOffListenerTest {

    static final class Ticker {
        int calls;

        void tick() {
            calls++;
        }
    }

    static Funcs.VoidFunc0 tearOff(Ticker t) {
        return TearOff.v0(t, "tick", t::tick);
    }

    static final class Model implements ChangeNotifier {
    }

    @Test
    void valueNotifierRemovesAnEqualTearOff() {
        Ticker t = new Ticker();
        ValueNotifier<Integer> n = new ValueNotifier<Integer>(0);
        n.addListener(tearOff(t));
        n.value(1);
        n.removeListener(tearOff(t));
        n.value(2);
        assertEquals(1, t.calls);
    }

    @Test
    void changeNotifierRemovesAnEqualTearOff() {
        Ticker t = new Ticker();
        Ticker other = new Ticker();
        Model m = new Model();
        m.addListener(tearOff(t));
        m.addListener(tearOff(other));
        m.notifyListeners();
        m.removeListener(tearOff(t));
        m.notifyListeners();
        m.removeListener(tearOff(other));
        m.notifyListeners();
        assertEquals(1, t.calls);
        assertEquals(2, other.calls);
    }
}
