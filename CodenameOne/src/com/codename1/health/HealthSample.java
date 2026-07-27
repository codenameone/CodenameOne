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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/// The base of every health record: what it measures and when.
///
/// Concrete subclasses correspond to [HealthDataKind] --
/// [QuantitySample], [CategorySample], [SeriesSample] and the
/// [SessionSample] family.
///
/// #### Instants and intervals
///
/// Every sample carries both a start and an end. When they are equal the
/// sample marks an instant -- see [#isInstantaneous()]. Cumulative types
/// such as steps are always intervals; see
/// [HealthDataType#isIntervalOnly()].
///
/// #### Identity
///
/// [#getId()] is assigned by the platform on write. It is scoped to this
/// platform and this installation and does **not** survive a reinstall, so
/// it must not be used as a primary key on your server.
///
/// [#getMetadata()] is **not persisted to HealthKit or Health Connect in
/// this release.** It round-trips through the local store used by the
/// simulator, desktop and JavaScript, and it travels with a sample you
/// hold in memory, but a sample written to a mobile platform and read back
/// comes back without it. Health Connect has no arbitrary metadata --
/// only a single `clientRecordId` -- so a general map cannot be carried
/// there at all. Correlate on your own identifier held in your own
/// storage, keyed by whatever you can reconstruct from the sample's type,
/// time range and value.
///
/// #### Mutability
///
/// Samples you build for a write are mutable so optional fields can be
/// filled in before handing them to [HealthStore]. Samples returned from a
/// query are snapshots: changing one does not change the store, and to
/// modify stored data you delete and re-write it.
public abstract class HealthSample {

    private final HealthDataType type;
    private final long startMillis;
    private final long endMillis;

    private String id;
    private HealthSource source;
    private RecordingMethod recordingMethod = RecordingMethod.UNKNOWN;
    private Map<String, String> metadata;

    /// Creates a sample spanning `[startMillis, endMillis]`.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if `type` is null or the end precedes
    ///   the start.
    protected HealthSample(HealthDataType type, long startMillis,
            long endMillis) {
        if (type == null) {
            throw new IllegalArgumentException("a sample requires a data type");
        }
        if (endMillis < startMillis) {
            throw new IllegalArgumentException(
                    "sample ends before it starts: " + startMillis + " .. "
                            + endMillis);
        }
        this.type = type;
        this.startMillis = startMillis;
        this.endMillis = endMillis;
    }

    /// What this sample measures.
    public final HealthDataType getType() {
        return type;
    }

    /// Inclusive start, epoch millis UTC.
    public final long getStartMillis() {
        return startMillis;
    }

    /// End, epoch millis UTC. Equal to the start for an instantaneous
    /// sample.
    public final long getEndMillis() {
        return endMillis;
    }

    /// `true` when this sample marks a moment rather than a span.
    public final boolean isInstantaneous() {
        return startMillis == endMillis;
    }

    /// The span in milliseconds; 0 for an instantaneous sample.
    public final long getDurationMillis() {
        return endMillis - startMillis;
    }

    /// The platform-assigned identifier, or null for a sample that has not
    /// been written yet. See the class documentation on identity before
    /// persisting this anywhere.
    public final String getId() {
        return id;
    }

    /// Sets the platform identifier. Called by ports when reading; setting
    /// it on a sample you are about to write has no effect.
    public final void setId(String id) {
        this.id = id;
    }

    /// Which app and device produced this sample, or null when the
    /// platform did not report it.
    public final HealthSource getSource() {
        return source;
    }

    /// Sets the originating source. Called by ports when reading.
    public final void setSource(HealthSource source) {
        this.source = source;
    }

    /// How this sample was recorded. Never null; defaults to
    /// [RecordingMethod#UNKNOWN].
    public final RecordingMethod getRecordingMethod() {
        return recordingMethod;
    }

    /// Declares how this sample was recorded. Worth setting on writes --
    /// other apps use it to decide how much to trust a value.
    public final void setRecordingMethod(RecordingMethod recordingMethod) {
        this.recordingMethod = recordingMethod == null
                ? RecordingMethod.UNKNOWN : recordingMethod;
    }

    /// Free-form metadata carried alongside the sample, never null. Use it
    /// for your own correlation identifier, since [#getId()] is not stable
    /// across a reinstall.
    public final Map<String, String> getMetadata() {
        if (metadata == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(metadata);
    }

    /// Attaches a metadata entry. A null value removes the key.
    public final void putMetadata(String key, String value) {
        if (key == null) {
            return;
        }
        if (value == null) {
            if (metadata != null) {
                metadata.remove(key);
            }
            return;
        }
        if (metadata == null) {
            metadata = new HashMap<String, String>();
        }
        metadata.put(key, value);
    }

    /// Two samples are equal when they carry the same platform
    /// identifier. Samples that have not been written yet have no
    /// identifier and are therefore only equal to themselves.
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HealthSample)) {
            return false;
        }
        HealthSample other = (HealthSample) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id == null ? System.identityHashCode(this) : id.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getName() + "[" + type.getId() + " "
                + startMillis + ".." + endMillis + "]";
    }
}
