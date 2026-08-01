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
package com.codename1.health;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// Describes a standing subscription to changes in the health store.
///
/// #### The id must never change
///
/// The identifier keys the persisted cursor that says how far the app has
/// already read. Reuse it across launches and app updates and you resume
/// exactly where you left off; change it -- even to fix a typo -- and the
/// framework treats it as a brand-new subscription and resynchronizes from
/// scratch. Hard-code it as a constant, and version it deliberately
/// (`"steps-v1"`) if you ever need a clean start.
public final class SubscriptionRequest {

    /// The default cap on how many samples one change batch carries.
    public static final int DEFAULT_MAX_SAMPLES_PER_BATCH = 1000;

    private final String id;
    private final List<HealthDataType> types = new ArrayList<HealthDataType>();
    private boolean includeDeletions = true;
    private boolean deliverSamples = true;
    private int maxSamplesPerBatch = DEFAULT_MAX_SAMPLES_PER_BATCH;

    /// Creates a request under a stable identifier.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if `id` is null, blank, or contains a
    ///   control character.
    public SubscriptionRequest(String id) {
        if (id == null || id.trim().length() == 0) {
            throw new IllegalArgumentException(
                    "a subscription needs a stable, non-blank id");
        }
        this.id = id.trim();
        requireNoControlCharacters(this.id);
    }

    /// The registry that survives process death is newline-delimited by
    /// record and tab-delimited by field, and it is written without
    /// escaping.
    ///
    /// An id carrying either delimiter therefore came back as a *different*
    /// id after a restart -- or as two entries, or as none -- and the
    /// symptom was a subscription that simply stopped delivering with no
    /// error anywhere. Rejecting the character at the point it is supplied
    /// turns a silent, restart-delayed data loss into an immediate
    /// programming error. Every control character is refused rather than
    /// just the two delimiters, because none of them belong in an
    /// identifier and the persisted format is free to grow another one.
    private static void requireNoControlCharacters(String id) {
        for (int iter = 0; iter < id.length(); iter++) {
            char c = id.charAt(iter);
            if (c < ' ' || c == 0x7f) {
                throw new IllegalArgumentException("a subscription id is"
                        + " persisted in a line-and-tab delimited registry"
                        + " and cannot contain control characters; found"
                        + " 0x" + Integer.toHexString(c) + " at offset "
                        + iter);
            }
        }
    }

    /// The stable identifier for this subscription.
    public String getId() {
        return id;
    }

    /// Adds a type to watch. At least one is required.
    public SubscriptionRequest addType(HealthDataType type) {
        if (type != null && !types.contains(type)) {
            types.add(type);
        }
        return this;
    }

    /// The types being watched.
    public List<HealthDataType> getTypes() {
        return Collections.unmodifiableList(types);
    }

    /// Whether deletions are reported alongside additions. Defaults to
    /// `true`; turn it off only if your app genuinely never needs to
    /// un-count something a user removed.
    public SubscriptionRequest setIncludeDeletions(boolean includeDeletions) {
        this.includeDeletions = includeDeletions;
        return this;
    }

    /// `true` when deletions are reported.
    public boolean isIncludeDeletions() {
        return includeDeletions;
    }

    /// Whether batches carry the changed samples themselves. Defaults to
    /// `true`. Set `false` for a notify-only subscription -- cheaper when
    /// all you want is a signal to refresh a screen.
    public SubscriptionRequest setDeliverSamples(boolean deliverSamples) {
        this.deliverSamples = deliverSamples;
        return this;
    }

    /// `true` when batches carry samples.
    public boolean isDeliverSamples() {
        return deliverSamples;
    }

    /// Caps how many samples one batch carries. Batches beyond the cap are
    /// reported through [HealthChangeBatch#hasMore()].
    ///
    /// The default exists because a background delivery has only a few
    /// seconds of wall clock before the OS suspends the process; handing
    /// the listener fifty thousand samples guarantees it does not finish.
    public SubscriptionRequest setMaxSamplesPerBatch(int maxSamplesPerBatch) {
        this.maxSamplesPerBatch = maxSamplesPerBatch;
        return this;
    }

    /// The per-batch sample cap.
    public int getMaxSamplesPerBatch() {
        return maxSamplesPerBatch;
    }

    /// Validates the request.
    ///
    /// #### Throws
    ///
    /// - `HealthException`: [HealthError#INVALID_ARGUMENT] when no type is
    ///   named or the batch cap is not positive.
    public void validate() throws HealthException {
        if (types.isEmpty()) {
            throw new HealthException(HealthError.INVALID_ARGUMENT,
                    "a subscription needs at least one data type");
        }
        if (maxSamplesPerBatch < 1) {
            throw new HealthException(HealthError.INVALID_ARGUMENT,
                    "maxSamplesPerBatch must be positive, got "
                            + maxSamplesPerBatch);
        }
    }
}
