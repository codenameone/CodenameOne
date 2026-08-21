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
package com.codename1.certificatewizard.api;

/**
 * Turns an HTTP failure from the Codename One signing service into something
 * worth showing a developer, plus the kind of thing it is so the wizard can
 * offer the right next step.
 *
 * <p>The cloud service answers with a plain-text sentence written for this
 * screen, so the body is the message whenever it looks like one. The previous
 * behaviour -- collapsing every 5xx to "cloud signing service failed (HTTP
 * 502). Try again later." -- threw that sentence away, which is how a revoked
 * App Store Connect key ended up being reported as a server fault the developer
 * was told to wait out.
 *
 * <p>Bodies are not trusted blindly: a proxy or CDN sitting in front of the
 * service answers 5xx with its own HTML page or a stub like {@code error code:
 * 522}, and neither belongs in the UI.
 */
public final class SigningError {

    /** What went wrong, in terms of what the developer can do about it. */
    public enum Kind {
        /** The Codename One sign-in expired -- re-run the wizard. */
        AUTH,
        /** The stored App Store Connect API key is missing, rejected or under-privileged. */
        CREDENTIAL,
        /** The key is fine; Apple refused this particular request. */
        APPLE_REJECTED,
        /** Apple no longer has the object; a sync reconciles it. */
        GONE,
        /** Apple is rate-limiting; waiting genuinely helps. */
        RATE_LIMITED,
        /** The service or Apple is down; waiting genuinely helps. */
        UNAVAILABLE,
        /** The request never made it out. */
        NETWORK,
        /** Anything else. */
        OTHER
    }

    /** Longest server sentence we will render; past this it is not a message. */
    private static final int MAX_BODY = 600;
    /** A 5xx body has to look like prose before we believe it explains anything. */
    private static final int MIN_SENTENCE = 20;

    private final Kind kind;
    private final String message;

    private SigningError(Kind kind, String message) {
        this.kind = kind;
        this.message = message;
    }

    public Kind kind() {
        return kind;
    }

    public String message() {
        return message;
    }

    /** True when trying the same thing again in a minute could plausibly work. */
    public boolean retryable() {
        return kind == Kind.RATE_LIMITED || kind == Kind.UNAVAILABLE || kind == Kind.NETWORK;
    }

    /**
     * @param code             HTTP status, or 0/negative when the request failed to complete
     * @param body             the response body, if the transport captured one
     * @param transportMessage the connection-level message, if any
     */
    public static SigningError from(int code, String body, String transportMessage) {
        if (code == 401 || code == 403) {
            return new SigningError(Kind.AUTH,
                    "Codename One login expired. Run the wizard again to refresh the sign-in token.");
        }
        if (code <= 0) {
            return new SigningError(Kind.NETWORK, transportFailure(transportMessage));
        }
        Kind kind = kindFor(code);
        String server = usableBody(body, code);
        return new SigningError(kind, server != null ? server : canned(code, kind));
    }

    private static Kind kindFor(int code) {
        if (code == 409) {
            return Kind.CREDENTIAL;
        }
        if (code == 404) {
            return Kind.GONE;
        }
        if (code == 429) {
            return Kind.RATE_LIMITED;
        }
        if (code >= 500) {
            return Kind.UNAVAILABLE;
        }
        if (code == 422 || code == 400) {
            return Kind.APPLE_REJECTED;
        }
        return Kind.OTHER;
    }

    /**
     * The body, when it is plainly a message from the signing service rather
     * than a gateway's error page. For 5xx we additionally insist it reads like
     * a sentence, because that is exactly where CDNs inject their own stubs.
     */
    private static String usableBody(String body, int code) {
        if (body == null) {
            return null;
        }
        String b = body.trim();
        if (b.isEmpty() || b.length() > MAX_BODY || b.charAt(0) == '<' || b.charAt(0) == '{'
                || isTransportArtifact(b)) {
            return null;
        }
        if (code >= 500 && (b.length() < MIN_SENTENCE || b.indexOf(' ') < 0)) {
            return null;
        }
        return b;
    }

    private static String canned(int code, Kind kind) {
        switch (kind) {
            case CREDENTIAL:
                return "Your App Store Connect API key needs attention before this can work. "
                        + "Open the ASC API Key page and save a current key.";
            case GONE:
                return "Apple no longer has this item. Use \"Sync with Apple\" to bring your account "
                        + "back in step.";
            case RATE_LIMITED:
                return "Apple is rate-limiting requests for your team. Wait a few minutes and try again.";
            case UNAVAILABLE:
                return "The Codename One signing service is temporarily unavailable (HTTP " + code
                        + "). Wait a few minutes and try again.";
            case APPLE_REJECTED:
                return "Apple rejected the request (HTTP " + code + ").";
            default:
                return "The request failed (HTTP " + code + ").";
        }
    }

    private static String transportFailure(String transportMessage) {
        if (transportMessage == null || transportMessage.trim().isEmpty()) {
            return "Could not reach the Codename One cloud service. Check your network connection "
                    + "and try again.";
        }
        if (isTransportArtifact(transportMessage)) {
            return "The connection to the Codename One cloud service dropped before the reply arrived. "
                    + "Try again.";
        }
        return "Connection failed: " + transportMessage.trim();
    }

    private static boolean isTransportArtifact(String msg) {
        if (msg == null) {
            return false;
        }
        String m = msg.trim().toLowerCase();
        return m.equals("stream closed") || m.equals("socket closed") || m.equals("unexpected end of stream")
                || m.equals("premature eof") || m.equals("connection reset")
                || m.startsWith("error code:");
    }
}
