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
package com.codename1.health.sensors;

/// Selects which stored records a glucose meter should replay, for
/// [SensorSession#requestStoredRecords(GlucoseRecordFilter)].
///
/// Glucose meters keep hundreds of readings and hand them over only on
/// request, through the Record Access Control Point (`0x2A52`) -- a small
/// request/response protocol layered on top of GATT. This class is the
/// portable description of such a request; the session drives the protocol
/// and delivers each retrieved record through the normal sample listener.
public final class GlucoseRecordFilter {

    /// What a filter selects on.
    public enum Kind {
        /// Every record the meter holds.
        ALL,
        /// Records with a sequence number at or above a bound.
        SEQUENCE_GREATER_OR_EQUAL,
        /// Records within an inclusive sequence-number range.
        SEQUENCE_RANGE,
        /// Only the record with the highest sequence number.
        LAST,
        /// Only the record with the lowest sequence number.
        FIRST
    }

    private final Kind kind;
    private final int from;
    private final int to;

    private GlucoseRecordFilter(Kind kind, int from, int to) {
        this.kind = kind;
        this.from = from;
        this.to = to;
    }

    /// Every stored record. On a meter that has been in use for months
    /// this can be several hundred notifications, so prefer
    /// [#sinceSequence(int)] once you have seen a record before.
    public static GlucoseRecordFilter all() {
        return new GlucoseRecordFilter(Kind.ALL, 0, 0);
    }

    /// Records at or after `sequenceNumber`.
    ///
    /// This is the incremental-sync filter: remember the highest
    /// [GlucoseMeasurement#getSequenceNumber()] you have stored and ask
    /// for everything after it.
    public static GlucoseRecordFilter sinceSequence(int sequenceNumber) {
        return new GlucoseRecordFilter(Kind.SEQUENCE_GREATER_OR_EQUAL,
                sequenceNumber, 0);
    }

    /// Records with sequence numbers in `[from, to]`.
    public static GlucoseRecordFilter sequenceRange(int from, int to) {
        return new GlucoseRecordFilter(Kind.SEQUENCE_RANGE, from, to);
    }

    /// The most recent record only.
    public static GlucoseRecordFilter last() {
        return new GlucoseRecordFilter(Kind.LAST, 0, 0);
    }

    /// The oldest record only.
    public static GlucoseRecordFilter first() {
        return new GlucoseRecordFilter(Kind.FIRST, 0, 0);
    }

    /// What this filter selects on.
    public Kind getKind() {
        return kind;
    }

    /// The lower sequence bound, for the sequence-based kinds.
    public int getFromSequence() {
        return from;
    }

    /// The upper sequence bound, for [Kind#SEQUENCE_RANGE].
    public int getToSequence() {
        return to;
    }
}
