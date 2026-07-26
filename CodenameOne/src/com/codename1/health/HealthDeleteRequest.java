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

/// Describes what to delete from [HealthStore], either by identifier or
/// by type and time range.
///
/// #### You can only delete your own data
///
/// Both platforms restrict deletion to samples your app wrote. A request
/// covering another app's data silently deletes nothing rather than
/// failing, which is a deliberate platform choice: an app must not be able
/// to discover what other apps recorded by watching which deletes
/// succeed.
public final class HealthDeleteRequest {

    private final List<String> sampleIds = new ArrayList<String>();
    private final List<HealthDataType> types = new ArrayList<HealthDataType>();
    private HealthTimeRange timeRange;

    /// Deletes specific samples of `type` by their platform identifiers.
    ///
    /// The type is required, not redundant: Health Connect deletes by
    /// record class plus id and cannot resolve an id on its own, so a
    /// request without one could only be honoured by guessing. Asking for
    /// it here means the caller states what they are deleting rather than
    /// the platform silently deleting nothing.
    public static HealthDeleteRequest byIds(HealthDataType type,
            List<String> sampleIds) {
        HealthDeleteRequest r = new HealthDeleteRequest();
        if (type != null) {
            r.types.add(type);
        }
        if (sampleIds != null) {
            r.sampleIds.addAll(sampleIds);
        }
        return r;
    }

    /// Deletes one sample of `type` by its platform identifier.
    public static HealthDeleteRequest byId(HealthDataType type,
            String sampleId) {
        HealthDeleteRequest r = new HealthDeleteRequest();
        if (type != null) {
            r.types.add(type);
        }
        if (sampleId != null) {
            r.sampleIds.add(sampleId);
        }
        return r;
    }

    /// Deletes everything of `type` that this app wrote inside `range`.
    public static HealthDeleteRequest byRange(HealthDataType type,
            HealthTimeRange range) {
        HealthDeleteRequest r = new HealthDeleteRequest();
        if (type != null) {
            r.types.add(type);
        }
        r.timeRange = range;
        return r;
    }

    private HealthDeleteRequest() {
    }

    /// Adds another type to a range-based request.
    public HealthDeleteRequest addType(HealthDataType type) {
        if (type != null && !types.contains(type)) {
            types.add(type);
        }
        return this;
    }

    /// The identifiers to delete, empty for a range-based request.
    public List<String> getSampleIds() {
        return Collections.unmodifiableList(sampleIds);
    }

    /// The types to delete, empty for an identifier-based request.
    public List<HealthDataType> getTypes() {
        return Collections.unmodifiableList(types);
    }

    /// The span to delete within, null for an identifier-based request.
    public HealthTimeRange getTimeRange() {
        return timeRange;
    }

    /// `true` when this request names specific samples rather than a span.
    public boolean isById() {
        return !sampleIds.isEmpty();
    }

    /// Validates the request.
    ///
    /// #### Throws
    ///
    /// - `HealthException`: [HealthError#INVALID_ARGUMENT] when the
    ///   request names neither identifiers nor a type and range, or
    ///   ambiguously names both.
    public void validate() throws HealthException {
        boolean hasIds = !sampleIds.isEmpty();
        boolean hasRange = timeRange != null;
        if (!hasIds && !hasRange) {
            throw new HealthException(HealthError.INVALID_ARGUMENT,
                    "a delete request needs either sample ids or a type"
                            + " and a time range");
        }
        if (hasIds && hasRange) {
            throw new HealthException(HealthError.INVALID_ARGUMENT,
                    "a delete request must name either sample ids or a"
                            + " type and range, not both");
        }
        if (types.isEmpty()) {
            throw new HealthException(HealthError.INVALID_ARGUMENT,
                    "a delete request needs a data type: Health Connect"
                            + " deletes by record class, so an id alone"
                            + " cannot be resolved");
        }
    }
}
