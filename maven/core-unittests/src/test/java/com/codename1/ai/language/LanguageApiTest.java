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
/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.ai.language;

import com.codename1.impl.LanguageImpl;
import com.codename1.junit.UITestBase;
import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageApiTest extends UITestBase {
    @Test
    void optionsRejectNaNConfidence() {
        assertThrows(IllegalArgumentException.class,
                () -> new LanguageOptions().minimumConfidence(Float.NaN));
        assertEquals(0f, new LanguageOptions()
                .minimumConfidence(Float.NEGATIVE_INFINITY)
                .getMinimumConfidence());
        assertEquals(1f, new LanguageOptions()
                .minimumConfidence(Float.POSITIVE_INFINITY)
                .getMinimumConfidence());
    }

    @Test
    void languageOperationsForwardBackendAndOptions() {
        RecordingLanguageImpl backend = new RecordingLanguageImpl();
        implementation.setLanguageImpl(backend);
        LanguageOptions options = new LanguageOptions()
                .backend(LanguageBackends.mlKitLanguageIdentification())
                .minimumConfidence(.4f);

        assertTrue(LanguageIdentifier.isSupported(options));
        assertTrue(Translator.isSupported(options));
        assertTrue(SmartReply.isSupported(options));

        LanguageCandidate[] candidates =
                LanguageIdentifier.identify("bonjour", options).get();
        assertEquals("fr", candidates[0].getLanguageTag());
        assertEquals("language-id", backend.feature);
        assertEquals("ml-kit", backend.backend);
        assertEquals(.4f, backend.options.getMinimumConfidence());

        assertEquals("hello",
                Translator.translate("bonjour", "fr", "en", options).get());
        assertEquals("translation", backend.feature);

        String[] replies = SmartReply.suggest(new SmartReplyMessage[] {
                new SmartReplyMessage("Are you coming?", "remote", false, 1)
        }, options).get();
        assertEquals("Yes", replies[0]);
        assertEquals("smart-reply", backend.feature);
    }

    @Test
    void missingBackendReportsUnsupported() {
        implementation.setLanguageImpl(null);
        assertFalse(LanguageIdentifier.isSupported());
        AsyncResource<LanguageCandidate[]> result =
                LanguageIdentifier.identify("hello", null);
        assertTrue(result.isDone());
        assertThrows(AsyncResource.AsyncExecutionException.class, result::get);
    }

    @Test
    void languageOperationsSnapshotMutableArguments() {
        RecordingLanguageImpl backend = new RecordingLanguageImpl();
        implementation.setLanguageImpl(backend);
        LanguageOptions options = new LanguageOptions()
                .backend(LanguageBackends.mlKitLanguageIdentification())
                .minimumConfidence(.35f);

        LanguageIdentifier.identify("bonjour", options);
        options.backend(LanguageBackends.auto()).minimumConfidence(.9f);

        assertNotSame(options, backend.options);
        assertEquals("ml-kit", backend.backend);
        assertEquals(.35f, backend.options.getMinimumConfidence());

        SmartReplyMessage original = new SmartReplyMessage(
                "First", null, false, 1);
        assertEquals("remote", original.getParticipantId());
        SmartReplyMessage replacement = new SmartReplyMessage(
                "Replacement", "remote", false, 2);
        SmartReplyMessage[] conversation =
                new SmartReplyMessage[] {original};
        SmartReply.suggest(conversation, null);
        conversation[0] = replacement;
        assertNotSame(conversation, backend.conversation);
        assertEquals(original, backend.conversation[0]);
    }

    @Test
    void reusableSessionRetainsBackendUntilClosed() {
        RecordingLanguageImpl backend = new RecordingLanguageImpl();
        implementation.setLanguageImpl(backend);

        Translator.Session session = Translator.open(
                new LanguageOptions().backend(
                        LanguageBackends.mlKitTranslation()));
        assertEquals("hello",
                session.translate("bonjour", "fr", "en").get());
        assertEquals("hello",
                session.translate("salut", "fr", "en").get());
        assertEquals(2, backend.translationCalls);
        assertEquals(0, backend.closeCount);

        session.close();
        session.close();
        assertEquals(1, backend.closeCount);
        assertThrows(IllegalStateException.class,
                () -> session.translate("bonjour", "fr", "en"));
    }

    @Test
    void staticConvenienceOperationClosesItsEphemeralBackend() {
        RecordingLanguageImpl backend = new RecordingLanguageImpl();
        implementation.setLanguageImpl(backend);

        assertEquals("hello",
                Translator.translate("bonjour", "fr", "en",
                        new LanguageOptions().backend(
                                LanguageBackends.mlKitTranslation())).get());
        assertEquals(1, backend.closeCount);
    }

    @Test
    void cancelledOperationSuppressesLateBackendCallbacksAndCloses()
            throws Exception {
        assertCancelledSettlementDoesNotPublish(false);
        assertCancelledSettlementDoesNotPublish(true);
    }

    private void assertCancelledSettlementDoesNotPublish(boolean fail) {
        RecordingLanguageImpl backend = new RecordingLanguageImpl();
        backend.deferTranslation = true;
        implementation.setLanguageImpl(backend);
        final AtomicInteger successes = new AtomicInteger();
        final AtomicInteger failures = new AtomicInteger();

        AsyncResource<String> result = Translator.translate(
                "bonjour", "fr", "en",
                new LanguageOptions().backend(
                        LanguageBackends.mlKitTranslation()));
        result.ready(new SuccessCallback<String>() {
            public void onSucess(String value) {
                successes.incrementAndGet();
            }
        }).except(new SuccessCallback<Throwable>() {
            public void onSucess(Throwable error) {
                failures.incrementAndGet();
            }
        });

        assertTrue(result.cancel(false));
        if (fail) {
            backend.pendingTranslation.error(
                    new IllegalStateException("late failure"));
        } else {
            backend.pendingTranslation.complete("late success");
        }

        assertTrue(result.isCancelled());
        assertEquals(0, successes.get());
        assertEquals(0, failures.get());
        assertEquals(1, backend.closeCount);
    }

    private static final class RecordingLanguageImpl extends LanguageImpl {
        String feature;
        String backend;
        LanguageOptions options;
        SmartReplyMessage[] conversation;
        int translationCalls;
        int closeCount;
        boolean deferTranslation;
        AsyncResource<String> pendingTranslation;

        public boolean isSupported(String value, String backendId) {
            feature = value;
            backend = backendId;
            return true;
        }

        public AsyncResource<LanguageCandidate[]> identify(
                String text, String backendId, LanguageOptions value) {
            feature = "language-id";
            backend = backendId;
            options = value;
            AsyncResource<LanguageCandidate[]> result =
                    new AsyncResource<LanguageCandidate[]>();
            result.complete(new LanguageCandidate[] {
                    new LanguageCandidate("fr", .9f)
            });
            return result;
        }

        public AsyncResource<String> translate(
                String text, String sourceLanguage, String targetLanguage,
                String backendId, LanguageOptions value) {
            feature = "translation";
            translationCalls++;
            AsyncResource<String> result = new AsyncResource<String>();
            if (deferTranslation) {
                pendingTranslation = result;
            } else {
                result.complete("hello");
            }
            return result;
        }

        public AsyncResource<String[]> suggestReplies(
                SmartReplyMessage[] conversation, String backendId,
                LanguageOptions value) {
            feature = "smart-reply";
            this.conversation = conversation;
            AsyncResource<String[]> result = new AsyncResource<String[]>();
            result.complete(new String[] {"Yes"});
            return result;
        }

        public void close() {
            closeCount++;
        }
    }
}
