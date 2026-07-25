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
package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ai.language.LanguageBackends;
import com.codename1.ai.language.LanguageCandidate;
import com.codename1.ai.language.LanguageIdentifier;
import com.codename1.ai.language.LanguageOptions;
import com.codename1.ai.language.SmartReply;
import com.codename1.ai.language.SmartReplyMessage;
import com.codename1.ai.language.Translator;
import com.codename1.io.Log;
import com.codename1.util.AsyncResource;

/**
 * Cross-port, non-visual contract coverage for on-device language services.
 *
 * <p>Capability checks are safe on every port and make the three independent
 * services visible to the dependency scanner. Unsupported ports additionally
 * exercise their immediate failed-resource fallback. Supported ports do not
 * download mutable language models during the unattended suite.</p>
 */
public class LanguageOnDeviceApiTest extends BaseTest {
    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() {
        try {
            checkValuesAndOptions();
            checkCapabilities();
            done();
            return true;
        } catch (Throwable t) {
            fail("On-device language API test failed: " + t);
            return false;
        }
    }

    private void checkValuesAndOptions() {
        LanguageOptions options = new LanguageOptions()
                .minimumConfidence(2f);
        checkEqual(1f, options.getMinimumConfidence(),
                "confidence upper clamp");
        check("auto".equals(options.getBackend().getId()),
                "default language backend");
        options.backend(null).minimumConfidence(-1f);
        check(options.getBackend() == LanguageBackends.auto(),
                "null language backend must restore auto");
        checkEqual(0f, options.getMinimumConfidence(),
                "confidence lower clamp");

        LanguageCandidate candidate = new LanguageCandidate("fr", .75f);
        check("fr".equals(candidate.getLanguageTag()),
                "language candidate tag");
        checkEqual(.75f, candidate.getConfidence(),
                "language candidate confidence");

        SmartReplyMessage message =
                new SmartReplyMessage(null, "remote", false, 42L);
        check("".equals(message.getText()),
                "null smart-reply text must normalize to empty");
        check("remote".equals(message.getParticipantId()),
                "smart-reply participant");
        check(!message.isLocalUser(), "smart-reply remote participant");
        checkEqual(42L, message.getTimestampMillis(),
                "smart-reply timestamp");
    }

    private void checkCapabilities() {
        boolean languageId = LanguageIdentifier.isSupported();
        boolean translation = Translator.isSupported();
        boolean smartReply = SmartReply.isSupported();
        Log.p("LanguageOnDeviceApiTest: languageId=" + languageId
                + " translation=" + translation
                + " smartReply=" + smartReply);

        if (!languageId) {
            assertImmediateFailure(LanguageIdentifier.identify("hello", null),
                    "language identification");
        }
        if (!translation) {
            assertImmediateFailure(Translator.translate(
                    "bonjour", "fr", "en", null), "translation");
        }
        if (!smartReply) {
            assertImmediateFailure(SmartReply.suggest(
                    new SmartReplyMessage[] {
                            new SmartReplyMessage("Hello", "remote", false, 1)
                    }, null), "smart reply");
        }
    }

    private void assertImmediateFailure(AsyncResource<?> resource,
                                        String label) {
        check(resource.isDone(), label
                + " unsupported fallback must complete immediately");
        try {
            resource.get();
            throw new IllegalStateException(label
                    + " unsupported fallback unexpectedly succeeded");
        } catch (AsyncResource.AsyncExecutionException expected) {
            // The documented unsupported-resource contract.
        }
    }

    private void check(boolean value, String label) {
        if (!value) {
            throw new IllegalStateException(label);
        }
    }

    private void checkEqual(long expected, long actual, String label) {
        if (expected != actual) {
            throw new IllegalStateException(label + ": expected "
                    + expected + " got " + actual);
        }
    }

    private void checkEqual(float expected, float actual, String label) {
        if (Math.abs(expected - actual) > 0.0001f) {
            throw new IllegalStateException(label + ": expected "
                    + expected + " got " + actual);
        }
    }
}
