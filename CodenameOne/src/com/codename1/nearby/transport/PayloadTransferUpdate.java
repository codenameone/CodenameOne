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
package com.codename1.nearby.transport;

/// Progress on one payload, delivered to
/// [TransportListener#payloadProgress].
///
/// A byte payload typically produces a single update with
/// [PayloadStatus#SUCCESS]; a file payload produces a stream of
/// [PayloadStatus#IN_PROGRESS] updates and then a terminal one.
public final class PayloadTransferUpdate {

    private final int payloadId;
    private final long bytesTransferred;
    private final long totalBytes;
    private final PayloadStatus status;

    /// Ports construct these.
    ///
    /// #### Parameters
    ///
    /// - `payloadId`: the payload this is about
    /// - `bytesTransferred`: bytes moved so far
    /// - `totalBytes`: the payload size, or -1 when unknown
    /// - `status`: where the transfer got to
    public PayloadTransferUpdate(int payloadId, long bytesTransferred,
            long totalBytes, PayloadStatus status) {
        this.payloadId = payloadId;
        this.bytesTransferred = bytesTransferred;
        this.totalBytes = totalBytes;
        this.status = status == null ? PayloadStatus.IN_PROGRESS : status;
    }

    /// The payload this update is about, matching [Payload#getId()].
    public int getPayloadId() {
        return payloadId;
    }

    /// How many bytes have moved so far.
    public long getBytesTransferred() {
        return bytesTransferred;
    }

    /// The payload size, or `-1` when the platform did not say. A stream
    /// payload legitimately has no total, so guard a progress bar on this
    /// being positive.
    public long getTotalBytes() {
        return totalBytes;
    }

    /// Where the transfer got to. Never null.
    public PayloadStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "PayloadTransferUpdate[" + payloadId + ", " + bytesTransferred
                + "/" + totalBytes + ", " + status + "]";
    }
}
