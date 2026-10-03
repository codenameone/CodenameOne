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

import com.codename1.flutter.animation.AnimationController;
import com.codename1.flutter.foundation.Characters;
import com.codename1.flutter.material.Dialogs;
import com.codename1.flutter.material.Slider;
import com.codename1.flutter.material.SliderRenderElement;
import com.codename1.flutter.navigation.Navigator;
import com.codename1.flutter.provider.Selector;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;

import dart.runtime.Funcs;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// This round's runtime fixes: character counting, an awaitable dialog, Selector's
/// rebuild test, the slider's interaction callbacks and forward(from:).
class Round14RuntimeTest {

    @BeforeEach
    void reset() {
        Navigator.reset();
        Dialogs.reset();
    }

    @Test
    void charactersAreUserPerceived() {
        String family = "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67";   // man ZWJ woman ZWJ girl
        String flag = "\uD83C\uDDEE\uD83C\uDDF1";                             // two regional indicators
        String accented = "e\u0301";                                           // e + combining acute
        String thumb = "\uD83D\uDC4D\uD83C\uDFFD";                             // thumbs up + skin tone
        assertEquals(1, Characters.count(family));
        assertEquals(1, Characters.count(flag));
        assertEquals(1, Characters.count(accented));
        assertEquals(1, Characters.count(thumb));
        assertEquals(3, Characters.count("a" + flag + "b"));
        assertEquals("a" + flag, Characters.take("a" + flag + "b", 2), "never cut mid-character");
        assertEquals(family, Characters.take(family + family, 1));
    }

    @Test
    void showDialogCompletesWithThePopResult() {
        final Object[] result = {"pending"};
        Object f = Dialogs.showDialog(null, (context) -> new ProbeBox(5, 5));
        ((dart.async.Future<Object>) f).then(new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object v) {
                result[0] = v;
            }
        });
        assertEquals("pending", result[0], "still open");
        Navigator.pop(null, Boolean.TRUE);
        assertEquals(Boolean.TRUE, result[0], "the future carries Navigator.pop's result");
    }

    /** A model the selector reads a slice of. */
    static final class Model implements com.codename1.flutter.foundation.ChangeNotifier {
        String slice = "a";
        int unrelated;

        void touch() {
            notifyListeners();
        }
    }

    @Test
    void aSelectorRebuildsOnlyWhenTheSelectionChanges() {
        final int[] builds = {0};
        final Model model = new Model();
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
                builds[0]++;
                return new ProbeBox(1, 1);
            }
        });
        com.codename1.flutter.provider.ChangeNotifierProvider p =
                new com.codename1.flutter.provider.ChangeNotifierProvider();
        p.value(model);
        p.child(s);
        BuildOwner owner = new BuildOwner();
        FlutterUI.mount(p, new RenderHost(), owner);
        assertEquals(1, builds[0]);

        model.unrelated++;
        model.touch();
        owner.flushSync();
        assertEquals(1, builds[0], "an unrelated change leaves the selection, and the builder, alone");

        model.slice = "b";
        model.touch();
        owner.flushSync();
        assertEquals(2, builds[0], "a changed selection rebuilds");

        s.shouldRebuild(new Funcs.Func2<String, String, Boolean>() {
            @Override
            public Boolean call(String previous, String next) {
                return Boolean.TRUE;
            }
        });
        model.touch();
        owner.flushSync();
        assertEquals(3, builds[0], "shouldRebuild overrides the equality test");
    }

    @Test
    void aSliderReportsItsInteractionStartAndEnd() {
        final List<String> events = new ArrayList<String>();
        Slider s = new Slider();
        s.value(2.0);
        s.min(0.0);
        s.max(10.0);
        s.divisions(20L);
        s.onChanged(v -> events.add("changed " + v));
        s.onChangeStart(v -> events.add("start " + v));
        s.onChangeEnd(v -> events.add("end " + v));
        SliderRenderElement e = (SliderRenderElement) FlutterUI.mount(s, new RenderHost(), new BuildOwner());
        e.userDragStarted();
        e.userDragged(15);
        e.userDragEnded();
        e.userDragEnded();
        assertEquals("[start 2.0, changed 7.5, end 7.5]", events.toString());
    }

    @Test
    void forwardFromNotifiesThroughTheValueSetter() {
        AnimationController c = new AnimationController();
        final List<Double> seen = new ArrayList<Double>();
        c.addListener(() -> seen.add(c.value()));
        c.forward(Double.valueOf(0.5));
        // Flutter's forward(from:) goes through the value setter, which announces the from
        // value on its own before the run begins; the run then announces its frames (here,
        // headless, its start and its end). Assigning the field dropped the setter's
        // announcement, so dependants saw the from value only once the run had started.
        assertEquals(java.util.Arrays.asList(0.5, 0.5, 1.0), seen);
    }
}
