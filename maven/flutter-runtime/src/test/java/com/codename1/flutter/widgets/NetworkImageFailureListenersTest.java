/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.flutter.widgets;

import com.codename1.flutter.testsupport.RasterDisplay;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.ConnectionRequestFailureTestAccess;
import com.codename1.io.Util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code ImageRenderElement.fetchNetworkImage} used to register ONLY a plain response
 * listener on the {@code ConnectionRequest} it builds. That listener only ever fires for
 * an actual response: a DNS failure, a refused or timed-out connection, or a TLS handshake
 * error never produces one at all -- {@code ConnectionRequest} routes those to its
 * exception listener instead -- and a non-2xx/non-redirect HTTP status (404, 500...) goes
 * to its response-code listener, also never reaching the plain one. {@link
 * ImageRenderElement#onLoadFailed} therefore never ran for either case, so the image
 * stayed blank -- and {@link ImageRenderElement#isLoadedForTest} stuck "loaded" on the
 * failed source, blocking even a later retry -- forever, rather than only for an empty or
 * undecodable response body.
 *
 * <p>Exercises the fix through {@link ImageRenderElement#addNetworkFailureListeners},
 * the exact method {@code fetchNetworkImage} wires onto its real request, and {@link
 * ConnectionRequestFailureTestAccess}, which reaches {@code ConnectionRequest}'s own
 * {@code handleException}/{@code handleErrorResponseCode} the way {@code
 * NetworkManagerAutoDetectAPNTest} does for the core module's tests -- so this needs no
 * real network round trip, {@code Storage} or {@code Display}.</p>
 */
class NetworkImageFailureListenersTest {

    // ConnectionRequest's own constructor asks NetworkManager.isAPSupported(), which reaches
    // Util.getImplementation() -- null in this module's usual headless tests (no Display is
    // ever installed), which NPEs. A plain CodenameOneImplementation is enough; RasterDisplay
    // is already a concrete one in this test tree, so it is reused rather than hand-rolling
    // another -- Display itself stays uninitialized (isInitialized() still false), exactly
    // the headless state the rest of this module's tests run in, since the fixed listeners
    // return before ever touching Display. Restored to null afterwards so every OTHER
    // (genuinely headless) test in this module keeps seeing no implementation at all.
    @BeforeEach
    void installNetworkImplementation() {
        Util.setImplementation(new RasterDisplay());
    }

    @AfterEach
    void uninstallNetworkImplementation() {
        Util.setImplementation(null);
    }

    private static ImageRenderElement element() {
        return new ImageRenderElement(
                Image.network("https://example.com/a.png", null, null, null, null));
    }

    @Test
    void anExceptionListenerClearsTheMarkerOnADnsOrConnectOrTlsOrTimeoutFailure() {
        ImageRenderElement e = element();
        String source = "url:https://example.com/a.png";
        e.simulateLoadStartedForTest(source);
        assertTrue(e.isLoadedForTest(source), "loadImage marks the source loaded up front");

        ConnectionRequest req = new ConnectionRequest("https://example.com/a.png", false);
        e.addNetworkFailureListeners(req, source);

        ConnectionRequestFailureTestAccess.simulateException(req, new java.io.IOException("simulated DNS failure"));

        assertFalse(e.isLoadedForTest(source),
                "an exception (DNS/connect/TLS/timeout) must reach onLoadFailed exactly "
                        + "like an empty or undecodable response does");
    }

    @Test
    void aResponseCodeListenerClearsTheMarkerOnAnHttpErrorStatus() {
        ImageRenderElement e = element();
        String source = "url:https://example.com/a.png";
        e.simulateLoadStartedForTest(source);

        ConnectionRequest req = new ConnectionRequest("https://example.com/a.png", false);
        e.addNetworkFailureListeners(req, source);

        ConnectionRequestFailureTestAccess.simulateErrorResponseCode(req, 404, "Not Found");

        assertFalse(e.isLoadedForTest(source),
                "a non-2xx/non-redirect status must reach onLoadFailed too");
    }

    @Test
    void failSilentlyIsSetSoNoDefaultErrorDialogIsShownForABackgroundImageLoad() {
        ConnectionRequest req = new ConnectionRequest("https://example.com/a.png", false);
        element().addNetworkFailureListeners(req, "url:https://example.com/a.png");

        assertTrue(req.isFailSilently(),
                "a background image load must not pop ConnectionRequest's own "
                        + "Error/Exception retry dialog");
    }

    @Test
    void aStaleFailureMustNotClearANewerLoadsMarker() {
        // Mirrors FailedNetworkImageRetryTest's guard for the pre-existing failure path:
        // the widget may have moved on to a different source by the time either listener
        // fires (a fast-scrolling list reusing this element), and onLoadFailed's own
        // equality check must still protect the NEW source's marker.
        ImageRenderElement e = element();
        String oldSource = "url:https://example.com/old.png";
        String newSource = "url:https://example.com/new.png";
        e.simulateLoadStartedForTest(oldSource);
        e.simulateLoadStartedForTest(newSource);   // the widget moved on

        ConnectionRequest req = new ConnectionRequest("https://example.com/old.png", false);
        e.addNetworkFailureListeners(req, oldSource);

        ConnectionRequestFailureTestAccess.simulateException(req, new java.io.IOException("stale"));

        assertTrue(e.isLoadedForTest(newSource), "the current load's marker must survive");
    }
}
