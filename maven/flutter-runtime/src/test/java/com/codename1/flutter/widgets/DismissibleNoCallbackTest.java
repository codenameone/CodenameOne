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
package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildOwner;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.rendering.Size;
import com.codename1.flutter.testsupport.ProbeBox;
import com.codename1.flutter.testsupport.RasterDisplay;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@code fireDismissed} used to return before entering the gone/detached state when
 * the widget gave no {@code onDismissed} -- so a row dismissed with no callback stayed
 * mounted at its full laid-out size, dragged fully off-screen by the slide that had
 * already run: an invisible gap the surrounding list never closed.
 *
 * <p>Flutter runs a {@code resizeDuration} animation to zero between the slide and
 * {@code onDismissed}; this milestone defers it ({@link Dismissible#resizeDuration}
 * is a no-op setter), so the slide is the whole dismiss here, and it has to collapse
 * the row whether or not there is a callback to notify.</p>
 */
class DismissibleNoCallbackTest {

    @BeforeEach
    void install() {
        // fireDismissed touches Display.getInstance().getCurrent() on its deferred
        // revalidate/repaint step; headless (no Display at all) that NPEs on
        // Display.impl, same as any other Display-touching call in this suite.
        RasterDisplay.install();
    }

    @AfterEach
    void uninstall() {
        RasterDisplay.uninstall();
    }

    @Test
    @DisplayName("a dismiss with no onDismissed still collapses the row to zero size")
    void dismissWithNoCallbackStillGoesAway() {
        Dismissible w = new Dismissible();
        w.child(new ProbeBox(60, 40));
        RenderHost host = new RenderHost();
        RenderElement e = (RenderElement) FlutterUI.mount(w, host, new BuildOwner());
        Size before = e.layout(BoxConstraints.loose(200, 200));
        e.position(0, 0);
        assertEquals(60.0, before.width(), "laid out at the child's size before the dismiss");
        assertEquals(40.0, before.height(), "laid out at the child's size before the dismiss");

        ((DismissibleRenderElement) e).fireDismissed(DismissDirection.endToStart);

        e.markNeedsLayout();
        Size after = e.layout(BoxConstraints.loose(200, 200));
        assertEquals(0.0, after.width(), "collapsed to nothing although no onDismissed was given");
        assertEquals(0.0, after.height(), "collapsed to nothing although no onDismissed was given");
    }
}
