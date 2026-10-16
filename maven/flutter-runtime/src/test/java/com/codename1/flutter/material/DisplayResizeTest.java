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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.BuildOwner;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.MediaQuery;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;
import com.codename1.flutter.widgets.Column;

import dart.core.DartList;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A real change of display size rebuilds the widgets that read {@code MediaQuery.of} --
 * an adaptive page picking a Row or a Column by width -- and nothing else; and the size
 * event a window reports when it is first shown, which repeats the size the tree was
 * built for, rebuilds nothing at all.
 */
class DisplayResizeTest {

    static final class Counting extends StatelessWidget {
        final boolean readsMetrics;
        int builds;

        Counting(boolean readsMetrics) {
            this.readsMetrics = readsMetrics;
        }

        @Override
        public Widget build(BuildContext context) {
            builds++;
            if (readsMetrics) {
                MediaQuery.of(context).size();
            }
            return new ProbeBox(1, 1);
        }
    }

    @Test
    @DisplayName("a resize rebuilds the metric readers of a retained page, and only them")
    void aResizeRebuildsTheReaders() {
        Counting reader = new Counting(true);
        Counting bystander = new Counting(false);
        Column page = new Column();
        page.children(DartList.of((Widget) reader, bystander));
        MaterialApp app = new MaterialApp();
        app.home(page);
        BuildOwner owner = new BuildOwner();
        MaterialAppElement root = (MaterialAppElement) FlutterUI.mount(app, new RenderHost(), owner);
        int readerBuilds = reader.builds;
        int bystanderBuilds = bystander.builds;

        // Headless, the tree was built for a 0x0 display: the same size again is the
        // event a desktop window sends right after it is shown.
        assertFalse(root.displaySizeChanged(0, 0), "the size the tree was built for is no change");
        owner.flushSync();
        assertEquals(readerBuilds, reader.builds, "the first-show event must not rebuild");

        assertTrue(root.displaySizeChanged(800, 600));
        owner.flushSync();
        assertEquals(readerBuilds + 1, reader.builds, "the reader must see the new size");
        assertEquals(bystanderBuilds, bystander.builds, "a widget that never read the metrics is left alone");

        assertFalse(root.displaySizeChanged(800, 600));
        owner.flushSync();
        assertEquals(readerBuilds + 1, reader.builds, "a repeated size is not a change");
    }
}
