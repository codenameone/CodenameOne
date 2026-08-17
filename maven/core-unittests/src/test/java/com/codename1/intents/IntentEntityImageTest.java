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
package com.codename1.intents;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.EncodedImage;
import com.codename1.util.Base64;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How an entity's @EntityImage thumbnail crosses to a platform. Separate from IntentsTest
 * because constructing an EncodedImage needs a Display, and that suite's whole point is that
 * the intents runtime works without one.
 */
class IntentEntityImageTest extends UITestBase {

    private static byte[] png() {
        byte[] b = new byte[32];
        b[0] = (byte) 0x89;
        b[1] = 'P';
        b[2] = 'N';
        b[3] = 'G';
        for (int i = 4; i < b.length; i++) {
            b[i] = (byte) (i * 7);
        }
        return b;
    }

    private static AppEntity entityWithImage() {
        return new AppEntity("order", "42").setTitle("Two coffees")
                .setImage(EncodedImage.create(png(), 4, 4, false));
    }

    /**
     * The two consumers want the thumbnail delivered differently. An index takes blobs one at a
     * time through a side channel, so base64 in the document would be dead weight. A picker
     * query is answered synchronously -- the platform is building the picker and blocking on the
     * reply -- so there is no second call to hand bytes to, and inlining is the whole
     * transaction. Both forms have to work, and neither may quietly become the other.
     */
    @FormTest
    void aThumbnailInlinesOnlyWhenTheCallerAsksForIt() {
        AppEntity e = entityWithImage();

        Map<String, byte[]> images = new HashMap<String, byte[]>();
        String indexed = IntentSerializer.serializeEntities(Arrays.asList(e), images);
        String inlined = IntentSerializer.serializeEntities(Arrays.asList(e), null, true);

        assertEquals(1, images.size(), "the index form hands the bytes over separately");
        assertFalse(indexed.contains("imageData"),
                "an index takes the blobs one at a time; base64 in the document is dead weight");
        assertTrue(inlined.contains("imageData"),
                "a synchronous query reply has no side channel, so the bytes travel inside it");
        assertTrue(inlined.contains(Base64.encodeNoNewline(png())),
                "the inlined bytes have to be the image, not a name for it");
    }

    /**
     * An entity with no image must not gain an empty field in either form, since the Swift side
     * reads its absence as "this entity has no thumbnail" rather than checking for emptiness.
     */
    @FormTest
    void anEntityWithoutAnImageCarriesNoImageFields() {
        String inlined = IntentSerializer.serializeEntities(
                Arrays.asList(new AppEntity("order", "42").setTitle("Two coffees")), null, true);

        assertFalse(inlined.contains("imageData"));
        assertFalse(inlined.contains("\"image\""));
    }

    /**
     * A picker row renders a thumbnail at a few dozen points, so an image big enough to matter
     * here is a mistake rather than a picture. It still indexes and still names its blob; only
     * the inline copy is dropped, so one oversized image cannot turn a synchronous reply into
     * something the picker waits on.
     */
    @FormTest
    void anOversizedThumbnailIsNotInlined() {
        byte[] big = new byte[200 * 1024];
        big[0] = (byte) 0x89;
        big[1] = 'P';
        big[2] = 'N';
        big[3] = 'G';
        AppEntity e = new AppEntity("order", "42").setTitle("Two coffees")
                .setImage(EncodedImage.create(big, 4, 4, false));

        String inlined = IntentSerializer.serializeEntities(Arrays.asList(e), null, true);

        assertFalse(inlined.contains("imageData"));
        assertTrue(inlined.contains("\"image\""),
                "the entity is still published; only the inline copy is dropped");
    }
}
