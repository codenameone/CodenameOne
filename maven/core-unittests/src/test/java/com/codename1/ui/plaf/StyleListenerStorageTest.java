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
package com.codename1.ui.plaf;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.events.StyleListener;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// A Style holds its FIRST listener directly and only builds an EventDispatcher
/// when a second one arrives, because in practice there is exactly one (every
/// Component registers itself). These tests pin the behaviour that shortcut has
/// to preserve: one listener is notified, a second promotes without losing the
/// first, a repeat add is de-duplicated, and removal works in both shapes.
class StyleListenerStorageTest extends UITestBase {

    private static final class Recorder implements StyleListener {
        private final List<String> properties = new ArrayList<String>();

        @Override
        public void styleChanged(String propertyName, Style source) {
            properties.add(propertyName);
        }
    }

    /// Changes one property and reports how many notifications each recorder saw.
    private void touch(Style s) {
        s.setFgColor(0x123456);
    }

    @FormTest
    void theSingleListenerIsNotified() {
        Style s = new Style();
        Recorder a = new Recorder();
        s.addStyleListener(a);
        touch(s);
        assertEquals(1, a.properties.size(), "the only listener was not notified");
    }

    @FormTest
    void aSecondListenerPromotesWithoutLosingTheFirst() {
        Style s = new Style();
        Recorder a = new Recorder();
        Recorder b = new Recorder();
        s.addStyleListener(a);
        s.addStyleListener(b);
        touch(s);
        assertEquals(1, a.properties.size(), "the first listener was lost on promotion");
        assertEquals(1, b.properties.size(), "the second listener was not notified");
    }

    /// EventDispatcher.addListener ignores a duplicate, so the shortcut must too
    /// -- otherwise a component that registers twice would be told twice.
    @FormTest
    void addingTheSameListenerTwiceNotifiesItOnce() {
        Style s = new Style();
        Recorder a = new Recorder();
        s.addStyleListener(a);
        s.addStyleListener(a);
        touch(s);
        assertEquals(1, a.properties.size(), "a duplicate add was not de-duplicated");
    }

    @FormTest
    void removingTheOnlyListenerSilencesIt() {
        Style s = new Style();
        Recorder a = new Recorder();
        s.addStyleListener(a);
        s.removeStyleListener(a);
        touch(s);
        assertEquals(0, a.properties.size(), "a removed listener was still notified");
    }

    @FormTest
    void removingOneOfTwoLeavesTheOther() {
        Style s = new Style();
        Recorder a = new Recorder();
        Recorder b = new Recorder();
        s.addStyleListener(a);
        s.addStyleListener(b);
        s.removeStyleListener(a);
        touch(s);
        assertEquals(0, a.properties.size(), "a removed listener was still notified");
        assertEquals(1, b.properties.size(), "the remaining listener was not notified");
    }

    @FormTest
    void removeListenersSilencesBothShapes() {
        Style single = new Style();
        Recorder a = new Recorder();
        single.addStyleListener(a);
        single.removeListeners();
        touch(single);
        assertEquals(0, a.properties.size(), "removeListeners left the single listener");

        Style promoted = new Style();
        Recorder b = new Recorder();
        Recorder c = new Recorder();
        promoted.addStyleListener(b);
        promoted.addStyleListener(c);
        promoted.removeListeners();
        touch(promoted);
        assertEquals(0, b.properties.size(), "removeListeners left a promoted listener");
        assertEquals(0, c.properties.size(), "removeListeners left a promoted listener");
    }

    /// Removing a listener that was never added must not disturb the one that was.
    @FormTest
    void removingAnUnknownListenerIsHarmless() {
        Style s = new Style();
        Recorder a = new Recorder();
        Recorder never = new Recorder();
        s.addStyleListener(a);
        s.removeStyleListener(never);
        touch(s);
        assertEquals(1, a.properties.size(), "removing an unknown listener dropped a real one");
    }
}
