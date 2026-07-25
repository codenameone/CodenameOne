/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.ai.language;

import com.codename1.impl.LanguageImpl;
import com.codename1.junit.UITestBase;
import com.codename1.util.AsyncResource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageApiTest extends UITestBase {
    @Test
    void languageOperationsForwardBackendAndOptions() {
        RecordingLanguageImpl backend = new RecordingLanguageImpl();
        implementation.setLanguageImpl(backend);
        LanguageOptions options = new LanguageOptions()
                .backend(LanguageBackends.mlKit())
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

    private static final class RecordingLanguageImpl extends LanguageImpl {
        String feature;
        String backend;
        LanguageOptions options;

        public boolean isSupported(String value, String backendId) {
            feature = value;
            backend = backendId;
            return true;
        }

        public AsyncResource<LanguageCandidate[]> identify(
                String text, String backendId, LanguageOptions value) {
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
            AsyncResource<String> result = new AsyncResource<String>();
            result.complete("hello");
            return result;
        }

        public AsyncResource<String[]> suggestReplies(
                SmartReplyMessage[] conversation, String backendId,
                LanguageOptions value) {
            feature = "smart-reply";
            AsyncResource<String[]> result = new AsyncResource<String[]>();
            result.complete(new String[] {"Yes"});
            return result;
        }

        public void close() {
        }
    }
}
