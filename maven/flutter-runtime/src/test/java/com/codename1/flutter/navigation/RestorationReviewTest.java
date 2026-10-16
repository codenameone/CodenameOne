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
package com.codename1.flutter.navigation;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.BuildOwner;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.RestorableDateTime;
import com.codename1.flutter.RestorableValue;
import com.codename1.flutter.RestorationBucket;
import com.codename1.flutter.RestorationMixin;
import com.codename1.flutter.State;
import com.codename1.flutter.StatefulWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.material.Dialogs;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;

import dart.core.DateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Restoration as the gallery uses it: restoreState was never called, so a state's
 * restorable properties were never registered; RestorableRouteFuture.present() set a
 * flag and opened nothing; RestorableDateTime was not the RestorableValue its stub says.
 */
class RestorationReviewTest {

    @BeforeEach
    void resetStacks() {
        Navigator.reset();
        Dialogs.reset();
    }

    static final class Page extends StatefulWidget {
        final RestorableRouteFuture<Object> future = new RestorableRouteFuture<Object>();
        final List<String> calls = new ArrayList<String>();

        @Override
        public State<? extends StatefulWidget> createState() {
            return new PageState();
        }
    }

    static final class PageState extends State<Page> implements RestorationMixin {
        @Override
        public void initState() {
            widget().calls.add("initState");
        }

        @Override
        public void restoreState(RestorationBucket oldBucket, boolean initialRestore) {
            widget().calls.add("restoreState:" + initialRestore);
            registerForRestoration(widget().future, "future");
        }

        @Override
        public Widget build(BuildContext context) {
            widget().calls.add("build");
            return new ProbeBox(10, 10);
        }
    }

    @Test
    void restoreStateRunsAfterInitStateAndBeforeTheFirstBuild() {
        Page page = new Page();
        FlutterUI.mount(page, new RenderHost(), new BuildOwner());
        assertEquals(java.util.Arrays.asList("initState", "restoreState:true", "build"), page.calls);
        assertTrue(page.future.isRegistered());
    }

    @Test
    void presentPushesThroughOnPresentAndDeliversTheResult() {
        Page page = new Page();
        final List<Object> presented = new ArrayList<Object>();
        final List<Object> results = new ArrayList<Object>();
        page.future.onPresent((NavigatorState nav, Object args) -> {
            presented.add(args);
            return nav.restorablePush((BuildContext ctx, Object a) -> {
                MaterialPageRoute r = new MaterialPageRoute();
                r.builder((c) -> new ProbeBox(10, 10));
                return r;
            }, args);
        });
        page.future.onComplete(results::add);
        FlutterUI.mount(page, new RenderHost(), new BuildOwner());

        page.future.present("args");
        assertEquals(java.util.Arrays.asList((Object) "args"), presented, "onPresent must run");
        assertTrue(page.future.isPresent());
        assertNotNull(page.future.route(), "the route's restoration id is kept while it shows");

        Navigator.pop(null, "the result");
        assertEquals(java.util.Arrays.asList((Object) "the result"), results);
        assertNull(page.future.route());
    }

    /// A presentation that pushed nothing (an unresolved route name) has no route to
    /// complete it, so it must not leave the future "present" for good.
    @Test
    void aFailedPresentationStaysInactive() {
        Navigator.installRouteTable(new java.util.HashMap<Object, Object>(), null, null);
        Page page = new Page();
        page.future.onPresent((NavigatorState nav, Object args) -> nav.restorablePushNamed("/nowhere", args));
        FlutterUI.mount(page, new RenderHost(), new BuildOwner());

        page.future.present(null);

        assertTrue(!page.future.isPresent(), "nothing was shown, so nothing is present");
        assertNull(page.future.route());
    }

    @Test
    void restorableDateTimeIsARestorableValue() {
        DateTime d = new DateTime(2024, 3, 1, 0, 0, 0, 0, 0);
        RestorableValue<DateTime> v = new RestorableDateTime(d);
        assertEquals(d, v.value());
    }
}
