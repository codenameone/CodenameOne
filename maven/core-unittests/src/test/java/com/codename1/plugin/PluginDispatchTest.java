/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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

package com.codename1.plugin;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.plugin.event.IsGalleryTypeSupportedEvent;
import com.codename1.plugin.event.OpenGalleryEvent;
import com.codename1.plugin.event.PluginEvent;
import com.codename1.ui.CN;
import com.codename1.ui.Display;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Covers the dispatch itself rather than [PluginSupport] in isolation: that the
/// framework entry points a plugin is meant to intercept actually reach it.
///
/// Every plugin here consumes the event it recognizes, which is also what keeps
/// the test honest -- an unconsumed OpenGalleryEvent would fall through to the
/// platform and open a real file dialog.
class PluginDispatchTest extends UITestBase {

    /// The regression this class exists for. `CN.openGallery` is the shortcut
    /// most application code reaches for, and it used to call `Display.impl`
    /// directly, so a registered plugin never saw the event and failed with no
    /// diagnostic at all.
    @FormTest
    void testOpenGalleryReachesPluginsThroughCN() {
        AtomicInteger seen = new AtomicInteger();
        Plugin p = evt -> {
            if (evt instanceof OpenGalleryEvent) {
                seen.incrementAndGet();
                evt.consume();
            }
        };
        PluginSupport support = CN.getPluginSupport();
        support.registerPlugin(p);
        try {
            CN.openGallery(e -> { }, Display.GALLERY_IMAGE);
            assertEquals(1, seen.get(),
                    "CN.openGallery must dispatch to plugins, not go straight to the implementation");
        } finally {
            support.deregisterPlugin(p);
        }
    }

    @FormTest
    void testOpenGalleryReachesPluginsThroughDisplay() {
        AtomicInteger seen = new AtomicInteger();
        Plugin p = evt -> {
            if (evt instanceof OpenGalleryEvent) {
                seen.incrementAndGet();
                evt.consume();
            }
        };
        PluginSupport support = Display.getInstance().getPluginSupport();
        support.registerPlugin(p);
        try {
            Display.getInstance().openGallery(e -> { }, Display.GALLERY_VIDEO);
            assertEquals(1, seen.get());
        } finally {
            support.deregisterPlugin(p);
        }
    }

    /// A consuming plugin answers instead of the platform, so the response it
    /// sets is what the caller gets back.
    @FormTest
    void testIsGalleryTypeSupportedAnswersFromThePlugin() {
        Plugin p = evt -> {
            if (evt instanceof IsGalleryTypeSupportedEvent) {
                IsGalleryTypeSupportedEvent e = (IsGalleryTypeSupportedEvent) evt;
                e.setPluginEventResponse(Boolean.TRUE);
                e.consume();
            }
        };
        PluginSupport support = Display.getInstance().getPluginSupport();
        support.registerPlugin(p);
        try {
            assertTrue(Display.getInstance().isGalleryTypeSupported(Display.GALLERY_VIDEO));
        } finally {
            support.deregisterPlugin(p);
        }
    }

    /// The event type carried to the plugin is the one the caller asked for --
    /// a plugin that answers only for video must be able to tell them apart.
    @FormTest
    void testPluginSeesTheRequestedGalleryType() {
        AtomicInteger type = new AtomicInteger(-1);
        Plugin p = evt -> {
            if (evt instanceof OpenGalleryEvent) {
                type.set(((OpenGalleryEvent) evt).getType());
                evt.consume();
            }
        };
        PluginSupport support = CN.getPluginSupport();
        support.registerPlugin(p);
        try {
            CN.openGallery(e -> { }, Display.GALLERY_VIDEO);
            assertEquals(Display.GALLERY_VIDEO, type.get());
        } finally {
            support.deregisterPlugin(p);
        }
    }

    /// A plugin that recognizes nothing must leave the event alone, so the
    /// framework still falls through to its normal path.
    @FormTest
    void testUnrelatedPluginDoesNotConsume() {
        PluginEvent evt = new IsGalleryTypeSupportedEvent(Display.GALLERY_IMAGE);
        Plugin p = e -> { };
        PluginSupport support = new PluginSupport();
        support.registerPlugin(p);
        support.firePluginEvent(evt);
        assertEquals(false, evt.isConsumed());
    }
}
