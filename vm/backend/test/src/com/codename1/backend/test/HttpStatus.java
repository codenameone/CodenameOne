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
package com.codename1.backend.test;

/// The common response statuses, so `assertEquals(HttpStatus.OK,
/// response.getStatusCode())` reads as it does in Spring.
public enum HttpStatus implements HttpStatusCode {
    CONTINUE(100),
    SWITCHING_PROTOCOLS(101),
    OK(200),
    CREATED(201),
    ACCEPTED(202),
    NO_CONTENT(204),
    PARTIAL_CONTENT(206),
    MOVED_PERMANENTLY(301),
    FOUND(302),
    SEE_OTHER(303),
    NOT_MODIFIED(304),
    TEMPORARY_REDIRECT(307),
    PERMANENT_REDIRECT(308),
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    METHOD_NOT_ALLOWED(405),
    NOT_ACCEPTABLE(406),
    REQUEST_TIMEOUT(408),
    CONFLICT(409),
    GONE(410),
    LENGTH_REQUIRED(411),
    PRECONDITION_FAILED(412),
    PAYLOAD_TOO_LARGE(413),
    UNSUPPORTED_MEDIA_TYPE(415),
    REQUESTED_RANGE_NOT_SATISFIABLE(416),
    EXPECTATION_FAILED(417),
    UNPROCESSABLE_ENTITY(422),
    TOO_MANY_REQUESTS(429),
    REQUEST_HEADER_FIELDS_TOO_LARGE(431),
    INTERNAL_SERVER_ERROR(500),
    NOT_IMPLEMENTED(501),
    BAD_GATEWAY(502),
    SERVICE_UNAVAILABLE(503),
    GATEWAY_TIMEOUT(504),
    HTTP_VERSION_NOT_SUPPORTED(505);

    private final int code;

    HttpStatus(int code) {
        this.code = code;
    }

    @Override
    public int value() {
        return code;
    }

    @Override
    public boolean is1xxInformational() {
        return code / 100 == 1;
    }

    @Override
    public boolean is2xxSuccessful() {
        return code / 100 == 2;
    }

    @Override
    public boolean is3xxRedirection() {
        return code / 100 == 3;
    }

    @Override
    public boolean is4xxClientError() {
        return code / 100 == 4;
    }

    @Override
    public boolean is5xxServerError() {
        return code / 100 == 5;
    }

    @Override
    public boolean isError() {
        // 4xx or 5xx only, as Spring's HttpStatusCode: a 600 is not an error.
        return is4xxClientError() || is5xxServerError();
    }

    /// The constant for `code`, or null when there is none.
    public static HttpStatus resolve(int code) {
        HttpStatus[] all = values();
        for (HttpStatus value : all) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }

    /// The constant for `code`; IllegalArgumentException when there is none.
    public static HttpStatus valueOf(int code) {
        HttpStatus found = resolve(code);
        if (found == null) {
            throw new IllegalArgumentException("No HttpStatus constant for " + code);
        }
        return found;
    }

    /// `code` as a status: the constant when there is one, a plain code otherwise.
    static HttpStatusCode of(final int code) {
        HttpStatus known = resolve(code);
        if (known != null) {
            return known;
        }
        return new HttpStatusCode() {
            @Override
            public int value() {
                return code;
            }

            @Override
            public boolean is1xxInformational() {
                return code / 100 == 1;
            }

            @Override
            public boolean is2xxSuccessful() {
                return code / 100 == 2;
            }

            @Override
            public boolean is3xxRedirection() {
                return code / 100 == 3;
            }

            @Override
            public boolean is4xxClientError() {
                return code / 100 == 4;
            }

            @Override
            public boolean is5xxServerError() {
                return code / 100 == 5;
            }

            @Override
            public boolean isError() {
                return is4xxClientError() || is5xxServerError();
            }

            @Override
            public boolean equals(Object other) {
                return other instanceof HttpStatusCode && ((HttpStatusCode) other).value() == code;
            }

            @Override
            public int hashCode() {
                return code;
            }

            @Override
            public String toString() {
                return String.valueOf(code);
            }
        };
    }
}
