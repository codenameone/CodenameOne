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
package com.codename1.home;

/// One trait's value at one moment, or the reason there isn't one.
///
/// #### Three outcomes, not two
///
/// A reading can carry a value, carry an error, or carry neither. The third is
/// the one that catches people: **an accessory can legitimately have nothing
/// to report.** Matter marks a temperature it has not measured with a null
/// sentinel, a light in colour-temperature mode has no meaningful hue, and an
/// illuminance sensor in the dark reports "too dark to measure". None of those
/// is a failure and none of them is a value.
///
/// So [#hasValue()] is asked first, and [#getValue()] answers `null` when it
/// is false. There is no zero standing in for a missing measurement anywhere
/// in this API -- a thermostat reading 0 degrees and a thermostat that has not
/// measured are different facts, and conflating them is how a UI comes to
/// display a freezing living room.
///
/// A batch read produces one of these per requested trait, so a partial
/// success is the normal case: three readings with values and one unreachable
/// accessory is a successful read, not a failed one.
public final class TraitReading {

    private final String accessoryId;
    private final String serviceId;
    private final Trait trait;
    private final TraitValue value;
    private final long timestampMillis;
    private final HomeError error;
    private final String errorMessage;

    private TraitReading(String accessoryId, String serviceId, Trait trait,
            TraitValue value, long timestampMillis, HomeError error,
            String errorMessage) {
        this.accessoryId = accessoryId;
        this.serviceId = serviceId;
        this.trait = trait;
        this.value = value;
        this.timestampMillis = timestampMillis;
        this.error = error;
        this.errorMessage = errorMessage;
    }

    /// A reading that carries a value.
    ///
    /// #### Parameters
    ///
    /// - `accessoryId`: the accessory read
    ///
    /// - `serviceId`: the service on it
    ///
    /// - `trait`: the trait read
    ///
    /// - `value`: the value
    ///
    /// - `timestampMillis`: when the value was current, in milliseconds since
    ///   the epoch; zero when the backend did not say
    ///
    /// #### Returns
    ///
    /// the reading
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when `trait` or `value` is `null`, or
    ///   when the value's kind does not match the trait's
    public static TraitReading of(String accessoryId, String serviceId,
            Trait trait, TraitValue value, long timestampMillis) {
        if (trait == null) {
            throw new IllegalArgumentException("trait is required");
        }
        if (value == null) {
            throw new IllegalArgumentException(
                    "value is required; use absent() when there is none");
        }
        if (value.getKind() != trait.getValueKind()) {
            throw new IllegalArgumentException(trait.getId() + " carries a "
                    + trait.getValueKind().name() + " value, not a "
                    + value.getKind().name());
        }
        return new TraitReading(accessoryId, serviceId, trait, value,
                timestampMillis, null, null);
    }

    /// A reading with nothing to report and nothing wrong -- the accessory
    /// has no value for this trait right now.
    ///
    /// #### Parameters
    ///
    /// - `accessoryId`: the accessory read
    ///
    /// - `serviceId`: the service on it
    ///
    /// - `trait`: the trait read
    ///
    /// #### Returns
    ///
    /// the reading
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when `trait` is `null`
    public static TraitReading absent(String accessoryId, String serviceId,
            Trait trait) {
        if (trait == null) {
            throw new IllegalArgumentException("trait is required");
        }
        return new TraitReading(accessoryId, serviceId, trait, null, 0, null,
                null);
    }

    /// A reading that failed.
    ///
    /// #### Parameters
    ///
    /// - `accessoryId`: the accessory read
    ///
    /// - `serviceId`: the service on it
    ///
    /// - `trait`: the trait read
    ///
    /// - `error`: why it failed; `null` becomes [HomeError#UNKNOWN]
    ///
    /// - `message`: the platform's own text, or `null`
    ///
    /// #### Returns
    ///
    /// the reading
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when `trait` is `null`
    public static TraitReading failed(String accessoryId, String serviceId,
            Trait trait, HomeError error, String message) {
        if (trait == null) {
            throw new IllegalArgumentException("trait is required");
        }
        return new TraitReading(accessoryId, serviceId, trait, null, 0,
                error == null ? HomeError.UNKNOWN : error, message);
    }

    /// The accessory this was read from.
    ///
    /// #### Returns
    ///
    /// the accessory identifier, or `null`
    public String getAccessoryId() {
        return accessoryId;
    }

    /// The service on that accessory.
    ///
    /// #### Returns
    ///
    /// the service identifier, or `null`
    public String getServiceId() {
        return serviceId;
    }

    /// The trait read.
    ///
    /// #### Returns
    ///
    /// the trait, never `null`
    public Trait getTrait() {
        return trait;
    }

    /// Whether there is a value to read.
    ///
    /// Ask this before [#getValue()]. See the class note for why a missing
    /// value is a normal outcome rather than an error.
    ///
    /// #### Returns
    ///
    /// `true` when [#getValue()] is not `null`
    public boolean hasValue() {
        return value != null;
    }

    /// The value.
    ///
    /// #### Returns
    ///
    /// the value, or `null` when the accessory had none or the read failed
    public TraitValue getValue() {
        return value;
    }

    /// When this value was current, in milliseconds since the epoch.
    ///
    /// Zero when the backend did not say, which is common -- most accessory
    /// reads are answered from a cache the platform keeps and do not carry a
    /// timestamp. Do not render an age from zero.
    ///
    /// #### Returns
    ///
    /// the timestamp, or zero
    public long getTimestampMillis() {
        return timestampMillis;
    }

    /// Whether this reading failed, as opposed to succeeding with no value.
    ///
    /// #### Returns
    ///
    /// `true` when [#getError()] is not `null`
    public boolean isFailed() {
        return error != null;
    }

    /// Why this reading failed.
    ///
    /// #### Returns
    ///
    /// the error, or `null` when the read succeeded
    public HomeError getError() {
        return error;
    }

    /// The platform's own text for a failure.
    ///
    /// #### Returns
    ///
    /// the message, or `null`
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(accessoryId).append('/').append(serviceId).append(' ')
                .append(trait.getId()).append('=');
        if (error != null) {
            b.append("!").append(error.name());
        } else if (value == null) {
            b.append("(none)");
        } else {
            b.append(value);
        }
        return b.toString();
    }
}
