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

/// Expectations of the response status, from [MockMvcResultMatchers#status].
public final class StatusResultMatchers {
    StatusResultMatchers() {
    }

    /// Exactly this status.
    public ResultMatcher is(int status) {
        return isMatcher(status);
    }

    private static ResultMatcher range(final int hundreds, final String name) {
        return new ResultMatcher() {
            @Override
            public void match(MvcResult result) {
                int status = result.getResponse().getStatus();
                Matchers.check(status / 100 == hundreds, "Range for response status value "
                        + status + " expected:<" + name + "> but was:<" + (status / 100) + "xx>");
            }
        };
    }

    public ResultMatcher is1xxInformational() {
        return range(1, "1xx");
    }

    public ResultMatcher is2xxSuccessful() {
        return range(2, "2xx");
    }

    public ResultMatcher is3xxRedirection() {
        return range(3, "3xx");
    }

    public ResultMatcher is4xxClientError() {
        return range(4, "4xx");
    }

    public ResultMatcher is5xxServerError() {
        return range(5, "5xx");
    }

    public ResultMatcher isOk() {
        return is(200);
    }

    public ResultMatcher isCreated() {
        return is(201);
    }

    public ResultMatcher isAccepted() {
        return is(202);
    }

    public ResultMatcher isNoContent() {
        return is(204);
    }

    public ResultMatcher isPartialContent() {
        return is(206);
    }

    public ResultMatcher isMovedPermanently() {
        return is(301);
    }

    public ResultMatcher isFound() {
        return is(302);
    }

    public ResultMatcher isSeeOther() {
        return is(303);
    }

    public ResultMatcher isNotModified() {
        return is(304);
    }

    public ResultMatcher isTemporaryRedirect() {
        return is(307);
    }

    public ResultMatcher isPermanentRedirect() {
        return is(308);
    }

    public ResultMatcher isBadRequest() {
        return is(400);
    }

    public ResultMatcher isUnauthorized() {
        return is(401);
    }

    public ResultMatcher isForbidden() {
        return is(403);
    }

    public ResultMatcher isNotFound() {
        return is(404);
    }

    public ResultMatcher isMethodNotAllowed() {
        return is(405);
    }

    public ResultMatcher isNotAcceptable() {
        return is(406);
    }

    public ResultMatcher isConflict() {
        return is(409);
    }

    public ResultMatcher isGone() {
        return is(410);
    }

    public ResultMatcher isPayloadTooLarge() {
        return is(413);
    }

    public ResultMatcher isUnsupportedMediaType() {
        return is(415);
    }

    public ResultMatcher isUnprocessableEntity() {
        return is(422);
    }

    public ResultMatcher isTooManyRequests() {
        return is(429);
    }

    public ResultMatcher isInternalServerError() {
        return is(500);
    }

    public ResultMatcher isNotImplemented() {
        return is(501);
    }

    public ResultMatcher isServiceUnavailable() {
        return is(503);
    }

    private static ResultMatcher isMatcher(final int status) {
        return new ResultMatcher() {
            @Override
            public void match(MvcResult result) {
                Matchers.equal("Status", Integer.valueOf(status),
                        Integer.valueOf(result.getResponse().getStatus()));
            }
        };
    }
}
