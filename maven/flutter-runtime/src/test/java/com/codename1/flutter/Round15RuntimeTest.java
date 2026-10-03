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

import com.codename1.flutter.foundation.Characters;
import com.codename1.flutter.provider.ChangeNotifierProvider;
import com.codename1.flutter.provider.Selector;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;
import com.codename1.flutter.widgets.Column;

import dart.core.DartList;
import dart.runtime.Funcs;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/// Combining marks of every script extend a character; a Selector mounted twice keeps
/// a cache per place; ObjectKey follows Dart's identical for boxed numbers.
class Round15RuntimeTest {

    @Test
    void combiningMarksOfEveryScriptExtendTheCharacter() {
        assertEquals(1, Characters.count("\u05D0\u05B0"), "Hebrew alef with sheva");
        assertEquals(1, Characters.count("\u0628\u064E"), "Arabic beh with fatha");
        assertEquals(1, Characters.count("\u0915\u093F"), "Devanagari ka with the i vowel sign");
        assertEquals(1, Characters.count("\u1100\u1161\u11A8"), "a Hangul syllable written as jamo");
        assertEquals("\u05D0\u05B0", Characters.take("\u05D0\u05B0\u05D1", 1));
        assertEquals(2, Characters.count("ab"));
    }

    static final class Model implements com.codename1.flutter.foundation.ChangeNotifier {
        String slice = "same";

        void touch() {
            notifyListeners();
        }
    }

    @Test
    void oneSelectorMountedTwiceCachesPerPlace() {
        final List<String> built = new ArrayList<String>();
        Model model = new Model();
        Selector<Model, String> s = new Selector<Model, String>();
        s.providedType(Model.class);
        s.selector(new Funcs.Func2<BuildContext, Model, String>() {
            @Override
            public String call(BuildContext c, Model m) {
                return m.slice;
            }
        });
        s.builder(new Funcs.Func3<BuildContext, String, Widget, Widget>() {
            @Override
            public Widget call(BuildContext c, String v, Widget child) {
                built.add(v);
                return new ProbeBox(1, 1);
            }
        });
        Column col = new Column();
        DartList<Widget> kids = new DartList<Widget>();
        kids.add(s);
        kids.add(s);   // the SAME widget instance in two places
        col.children(kids);
        ChangeNotifierProvider p = new ChangeNotifierProvider();
        p.value(model);
        p.child(col);
        BuildOwner owner = new BuildOwner();
        FlutterUI.mount(p, new RenderHost(), owner);
        assertEquals(2, built.size(), "each place runs its own builder: " + built);
        model.touch();
        owner.flushSync();
        assertEquals(2, built.size(), "an unchanged selection rebuilds neither place");
    }

    @Test
    void objectKeysOfEqualBoxedNumbersAreTheSameKey() {
        ObjectKey a = new ObjectKey(Long.valueOf(100000));
        ObjectKey b = new ObjectKey(Long.valueOf(100000));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(new ObjectKey(new StringBuilder("x").toString()), new ObjectKey("x"),
                "an object other than a number is still compared by identity");
    }
}
