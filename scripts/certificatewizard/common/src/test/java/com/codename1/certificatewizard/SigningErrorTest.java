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
package com.codename1.certificatewizard;

import com.codename1.certificatewizard.api.SigningError;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SigningErrorTest {

    private static final String REVOKED =
            "Apple rejected your App Store Connect API key. It has most likely been revoked, or the Key ID "
            + "and Issuer ID do not match the .p8 file.";

    @Test
    void aRejectedKeyIsReportedAsSomethingToFixNotSomethingToWaitOut() {
        SigningError e = SigningError.from(409, REVOKED, null);

        assertEquals(SigningError.Kind.CREDENTIAL, e.kind());
        assertEquals(REVOKED, e.message());
        assertFalse(e.retryable());
    }

    @Test
    void applesOwnWordingSurvivesToTheUi() {
        String apple = "Apple rejected the request: You already have a current Distribution certificate.";

        SigningError e = SigningError.from(422, apple, null);

        assertEquals(SigningError.Kind.APPLE_REJECTED, e.kind());
        assertEquals(apple, e.message());
        assertFalse(e.retryable());
    }

    /**
     * The regression this whole change exists for: a 5xx used to be flattened
     * to "cloud signing service failed (HTTP 502). Try again later.", throwing
     * away whatever the service had said.
     */
    @Test
    void aServerSentenceIsShownEvenOnA5xx() {
        String explained = "Codename One could not reach Apple's App Store Connect API. That is usually a "
                + "brief outage on Apple's side -- wait a few minutes and try again.";

        SigningError e = SigningError.from(502, explained, null);

        assertEquals(SigningError.Kind.UNAVAILABLE, e.kind());
        assertEquals(explained, e.message());
        assertTrue(e.retryable());
    }

    @Test
    void aGatewayErrorPageIsNeverShownToTheDeveloper() {
        SigningError e = SigningError.from(502,
                "<html><head><title>502 Bad Gateway</title></head><body>nginx</body></html>", null);

        assertEquals(SigningError.Kind.UNAVAILABLE, e.kind());
        assertFalse(e.message().contains("<"), e.message());
        assertTrue(e.message().contains("502"), e.message());
    }

    @Test
    void aCdnStubIsNotASentence() {
        SigningError e = SigningError.from(522, "error code: 522", null);

        assertEquals(SigningError.Kind.UNAVAILABLE, e.kind());
        assertFalse(e.message().contains("error code"), e.message());
    }

    @Test
    void anOverlongBodyIsNotAMessage() {
        StringBuilder huge = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            huge.append("stack frame ");
        }

        SigningError e = SigningError.from(500, huge.toString(), null);

        assertTrue(e.message().length() < 200, e.message());
    }

    @Test
    void expiredCodenameOneLoginIsItsOwnThing() {
        assertEquals(SigningError.Kind.AUTH, SigningError.from(401, "whatever", null).kind());
        assertEquals(SigningError.Kind.AUTH, SigningError.from(403, "whatever", null).kind());
        assertFalse(SigningError.from(401, null, null).retryable());
    }

    @Test
    void rateLimitAndOutageBothInviteARetry() {
        assertTrue(SigningError.from(429, null, null).retryable());
        assertEquals(SigningError.Kind.RATE_LIMITED, SigningError.from(429, null, null).kind());
        assertTrue(SigningError.from(503, null, null).retryable());
    }

    @Test
    void aDeletedObjectPointsAtASync() {
        SigningError e = SigningError.from(404, null, null);

        assertEquals(SigningError.Kind.GONE, e.kind());
        assertTrue(e.message().contains("Sync with Apple"), e.message());
    }

    @Test
    void aDroppedConnectionSaysSoWithoutJargon() {
        SigningError e = SigningError.from(0, null, "Stream closed");

        assertEquals(SigningError.Kind.NETWORK, e.kind());
        assertFalse(e.message().toLowerCase().contains("stream closed"), e.message());
        assertTrue(e.retryable());
    }

    @Test
    void aRealConnectionErrorKeepsItsDetail() {
        SigningError e = SigningError.from(0, null, "Unable to resolve host cloud.codenameone.com");

        assertTrue(e.message().contains("cloud.codenameone.com"), e.message());
    }

    @Test
    void thereIsAlwaysSomethingToShow() {
        int[] codes = {0, 400, 401, 403, 404, 409, 422, 429, 500, 502, 503, 522};
        for (int code : codes) {
            SigningError e = SigningError.from(code, null, null);
            assertTrue(e.message() != null && e.message().trim().length() > 10, "code " + code);
        }
    }
}
