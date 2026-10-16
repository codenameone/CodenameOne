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
package com.codename1.backend.push;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// What the push service answered to one [CloudPush] send.
///
/// Accepted means ADMITTED: the service has queued the message durably and
/// will hand it to the providers, but no provider or device has seen it yet.
/// Whether it arrived is reported later, through delivery feedback.
///
/// A refusal is a result too, not an exception: [#getStatus()] says which kind
/// (400 for a bad request, 401 for a missing or wrong key, 402 when the account
/// doesn't include the capability, 429 when the caller should slow down) and
/// [#getError()] carries the service's explanation.
public final class PushResult {
    private final int status;
    private final List<String> deliveryIds;
    private final String error;
    private final int retryAfterSeconds;

    /// A result, mostly for tests of code that sends; [CloudPush] makes the real
    /// ones.
    ///
    /// @param status the HTTP status
    /// @param deliveryIds the delivery ids, or null for none
    /// @param error the service's error text, or null
    /// @param retryAfterSeconds the seconds to wait before retrying, or -1
    public PushResult(int status, List<String> deliveryIds, String error, int retryAfterSeconds) {
        this.status = status;
        this.deliveryIds = deliveryIds == null ? new ArrayList<String>()
                : new ArrayList<String>(deliveryIds);
        this.error = error;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /// @return the HTTP status the service answered with
    public int getStatus() {
        return status;
    }

    /// @return true when the service admitted the message (202, or any 2xx)
    public boolean isAccepted() {
        return status >= 200 && status < 300;
    }

    /// One id per target the service admitted, for matching delivery feedback
    /// later.
    ///
    /// @return the ids, empty when there are none; never null
    public List<String> getDeliveryIds() {
        return Collections.unmodifiableList(deliveryIds);
    }

    /// @return the service's explanation of a refusal, or null when it was
    /// accepted
    public String getError() {
        return error;
    }

    /// How long a 429 asked the caller to wait.
    ///
    /// @return seconds from the `Retry-After` header, or -1 when there was none
    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    @Override
    public String toString() {
        return "PushResult[status=" + status + ", deliveries=" + deliveryIds.size()
                + (error == null ? "" : ", error=" + error) + "]";
    }
}
