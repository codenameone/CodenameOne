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
package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.events.StyleListener;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A safe-area container writes inset padding onto its own style, measures or lays out
 * with it, and hands the padding straight back. That round trip must be silent.
 *
 * <p>It used to announce the setting half as a PADDING style change, and
 * {@link Component#styleChanged} answers a PADDING change by queueing a
 * {@code revalidateLater()} on the parent. So laying out a Form with a safe-area
 * Toolbar queued a revalidate of the whole Form, which laid it out again, which queued
 * another - a treadmill that could never converge, because the padding is reverted
 * before anything could observe it as settled.</p>
 *
 * <p>An idle app hides it (the EDT sleeps, so the treadmill turns once per wake-up).
 * It surfaces the moment something keeps the EDT awake: measured on a real app, a drag
 * spent ~200ms of every frame in the revalidate queue against ~15ms of painting, i.e.
 * under 5fps.</p>
 */
class SafeAreaRevalidateLoopTest extends UITestBase {

    /** Records every style change the container announces about itself. */
    private static final class Recorder implements StyleListener {
        private final List<String> properties = new ArrayList<String>();

        @Override
        public void styleChanged(String propertyName, Style source) {
            properties.add(propertyName);
        }
    }

    private Container notchedSafeAreaForm(Form f) {
        // A device with insets on every edge; without them snapToSafeArea has nothing
        // to do and the test would pass vacuously.
        implementation.setDisplaySafeArea(new Rectangle(20, 40,
                Display.getInstance().getDisplayWidth() - 40,
                Display.getInstance().getDisplayHeight() - 80));
        Container inner = new Container(new BorderLayout());
        inner.setSafeArea(true);
        inner.add(BorderLayout.CENTER, new Label("content"));
        f.setLayout(new BorderLayout());
        f.add(BorderLayout.CENTER, inner);
        f.show();
        f.layoutContainer();
        return inner;
    }

    @FormTest
    void measuringASafeAreaContainerAnnouncesNoStyleChange() {
        Form f = Display.getInstance().getCurrent();
        Container inner = notchedSafeAreaForm(f);

        Recorder rec = new Recorder();
        inner.getStyle().addStyleListener(rec);
        inner.setShouldCalcPreferredSize(true);
        inner.getPreferredSize();
        inner.getStyle().removeStyleListener(rec);

        assertEquals(0, rec.properties.size(),
                "measuring a safe-area container announced " + rec.properties);
    }

    @FormTest
    void layingOutASafeAreaContainerAnnouncesNoStyleChange() {
        Form f = Display.getInstance().getCurrent();
        Container inner = notchedSafeAreaForm(f);

        Recorder rec = new Recorder();
        inner.getStyle().addStyleListener(rec);
        f.layoutContainer();
        inner.getStyle().removeStyleListener(rec);

        assertEquals(0, rec.properties.size(),
                "laying out a safe-area container announced " + rec.properties);
    }

    /**
     * The property that actually broke: a layout pass must settle rather than leave work
     * queued that makes the next pass queue the same work again.
     */
    @FormTest
    void aLayoutPassDoesNotQueueAnotherOne() {
        Form f = Display.getInstance().getCurrent();
        notchedSafeAreaForm(f);

        f.flushRevalidateQueue();
        boolean settled = true;
        for (int i = 0; i < 5 && settled; i++) {
            f.setShouldCalcPreferredSize(true);
            f.layoutContainer();
            f.getPreferredSize();
            f.flushRevalidateQueue();
            settled = !f.hasPendingRevalidations();
        }
        assertTrue(settled, "a layout/measure pass keeps re-queueing a revalidate of the Form");
    }
}
