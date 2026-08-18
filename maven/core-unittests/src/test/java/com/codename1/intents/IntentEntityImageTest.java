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
     * The name was a 32-bit content hash, which is fine as a cache key and wrong as an
     * identity: two different thumbnails that collide took the same name and the second
     * overwrote the first, so one entity displayed another entity's picture. Two distinct
     * images in one request must never share a key.
     */
    @FormTest
    void twoDifferentThumbnailsNeverShareAName() {
        byte[] a = png();
        byte[] b = png();
        b[10] = (byte) (b[10] ^ 0xFF);

        Map<String, byte[]> images = new HashMap<String, byte[]>();
        IntentSerializer.serializeEntities(Arrays.asList(
                new AppEntity("order", "1").setTitle("One")
                        .setImage(EncodedImage.create(a, 4, 4, false)),
                new AppEntity("order", "2").setTitle("Two")
                        .setImage(EncodedImage.create(b, 4, 4, false))), images);

        assertEquals(2, images.size(), "two distinct pictures need two names");
        for (Map.Entry<String, byte[]> e : images.entrySet()) {
            assertTrue(Arrays.equals(e.getValue(), a) || Arrays.equals(e.getValue(), b));
        }
    }

    /** The same picture twice is one blob, which is the point of hashing the content. */
    @FormTest
    void anIdenticalThumbnailIsStoredOnce() {
        Map<String, byte[]> images = new HashMap<String, byte[]>();
        IntentSerializer.serializeEntities(Arrays.asList(
                new AppEntity("order", "1").setTitle("One")
                        .setImage(EncodedImage.create(png(), 4, 4, false)),
                new AppEntity("order", "2").setTitle("Two")
                        .setImage(EncodedImage.create(png(), 4, 4, false))), images);

        assertEquals(1, images.size());
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
     * The timeout path, which needs a real Display: without one the framework runs the handler
     * inline and no timeout thread ever starts, so this is the only suite that can prove it.
     *
     * A handler that overruns has already had the platform told it failed. Navigating afterwards
     * contradicts that -- the app foregrounding onto a new form for an action the assistant just
     * reported as not having happened.
     *
     * The waiting goes through invokeAndBlock rather than Thread.sleep because this test body is
     * dispatched onto the event thread. Sleeping here blocks the very thread that delivers the
     * navigation, so the assertion would pass whether or not the fix is present -- which it did,
     * before this was written properly.
     */
    @FormTest
    void aHandlerThatOverrunsItsDeadlineDoesNotNavigate() throws Exception {
        final java.util.List<String> navigated =
                java.util.Collections.synchronizedList(new java.util.ArrayList<String>());
        final java.util.List<IntentResult> reported =
                java.util.Collections.synchronizedList(new java.util.ArrayList<IntentResult>());
        com.codename1.router.Navigation.setDispatcher(new com.codename1.router.RouteDispatcher() {
            public com.codename1.ui.Form dispatch(String url) {
                navigated.add(url);
                return null;
            }
        });
        try {
            Intents.setDispatcher(new SlowDispatcher());
            Intents.dispatchInvocation("slow", null, IntentSource.SHORTCUT, true,
                    new IntentCompletion() {
                        public void onIntentResult(IntentResult r) {
                            reported.add(r);
                        }
                    });

            // Declared budget is one second, handler takes two. Waiting past both, off the
            // event thread, so anything the framework marshals onto it can actually run.
            com.codename1.ui.Display.getInstance().invokeAndBlock(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            flushSerialCalls();

            assertEquals(1, reported.size(),
                    "the platform must be told exactly once, got " + reported.size());
            assertTrue(reported.get(0).isFailed(), "and told that it timed out");
            assertTrue(navigated.isEmpty(),
                    "a late result must not move the user's screen, got " + navigated);
        } finally {
            com.codename1.router.Navigation.setDispatcher(null);
            Intents.reset();
        }
    }

    /**
     * A model is the caller least able to notice a missed deadline -- it receives a string and
     * has no view of how long anything took. Calling the dispatcher directly made it the one
     * caller whose handlers had no enforced budget: the deadline passed, the context reported
     * cancelled, and the late result was serialized back anyway.
     *
     * Needs a Display for the same reason as the test above: without one there is no timeout
     * thread to enforce anything.
     */
    @FormTest
    void aModelInvocationIsCutOffAtItsDeadline() throws Exception {
        Intents.setDispatcher(new SlowDispatcher());
        final String[] answer = new String[1];
        try {
            final java.util.List<com.codename1.ai.Tool> tools = Intents.asTools();
            assertEquals(1, tools.size(), "the fixture is exposed to a model");

            // invoke() blocks, so it runs off the event thread; the event thread has to stay
            // free for the framework to marshal onto.
            com.codename1.ui.Display.getInstance().invokeAndBlock(new Runnable() {
                public void run() {
                    try {
                        answer[0] = tools.get(0).invoke("{}");
                    } catch (Exception e) {
                        answer[0] = "threw: " + e;
                    }
                }
            });

            assertTrue(answer[0] != null && answer[0].contains("\"ok\":false"),
                    "an overrun must come back as a failure, got " + answer[0]);
            assertFalse(answer[0].contains("/orders/42"),
                    "and must not carry the late handler's result, got " + answer[0]);
        } finally {
            Intents.reset();
        }
    }

    /** Declares a one-second budget and takes two, so the timeout always wins. */
    private static final class SlowDispatcher implements IntentDispatcher {
        public java.util.List<IntentDeclaration> describe() {
            return Arrays.asList(new IntentDeclaration("slow", "Slow", "", true, true, false,
                    "", 1, java.util.Collections.<String>emptyList(),
                    java.util.Collections.<IntentParameterInfo>emptyList(),
                    Arrays.asList(Exposure.ASSISTANT, Exposure.MODEL)));
        }

        public IntentResult invoke(String intentId, Map<String, Object> params,
                                   IntentContext ctx) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return IntentResult.opens("/orders/42");
        }

        public java.util.List<AppEntity> queryEntities(String entityType, String kind,
                                                       String argument) {
            return java.util.Collections.emptyList();
        }
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
