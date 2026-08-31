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
package com.codename1.builders;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What counts as turning {@code ios.includePush} off.
 *
 * <p>The builder normalises this hint by rewriting anything that is not a
 * trimmed, case-insensitive {@code "true"} into {@code "false"}. The VoIP
 * conflict check has to agree with that exactly, because disagreement is
 * silent in the direction that matters: a value the normalisation reads as
 * off and the check reads as on produces a build carrying PushKit and the
 * {@code voip} background mode with push disabled -- signed, installable,
 * and unable to receive the one kind of push the integration exists for.</p>
 *
 * <p>The check used to compare against the literal {@code "false"}, so every
 * other spelling went through.</p>
 */
class IPhonePushHintTest {

    @Test
    void anyExplicitValueThatIsNotTrueDisablesPush() {
        // Each of these is rewritten to "false" by the normalisation, so each
        // has to be refused for a VoIP app. " false " is the one that was
        // reported; the rest are the same hole with different spelling.
        for (String hint : new String[] {"false", "FALSE", " false ", "0",
                "no", "off", "", "   ", "tru", "true false"}) {
            assertTrue(IPhoneBuilder.pushExplicitlyDisabled(hint),
                    "\"" + hint + "\" disables push, so a VoIP app has to be"
                    + " refused for it");
        }
    }

    @Test
    void trueInAnyCasingOrPaddingLeavesPushOn() {
        for (String hint : new String[] {"true", "TRUE", "True", " true ",
                "\ttrue\n"}) {
            assertFalse(IPhoneBuilder.pushExplicitlyDisabled(hint),
                    "\"" + hint + "\" is the normalisation's idea of true, so"
                    + " it must not be refused");
        }
    }

    @Test
    void anAbsentHintIsNotAnExplicitRefusal() {
        // Null is the project saying nothing, which the builder answers by
        // defaulting push ON for a VoIP app. Treating silence as a refusal
        // would fail every VoIP build that never set the hint at all.
        assertFalse(IPhoneBuilder.pushExplicitlyDisabled(null),
                "an unset hint is not a refusal");
    }
}
