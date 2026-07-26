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

/// The outcome of a write. A write can partially succeed -- a batch of a
/// thousand samples with three bad timestamps writes 997 -- so this
/// reports per-sample results rather than a single boolean.
public final class HealthWriteResult {

    private final List<String> sampleIds = new ArrayList<String>();
    private final List<String> rejections = new ArrayList<String>();

    /// Creates an empty result. Populated by [HealthStore] and ports.
    public HealthWriteResult() {
    }

    /// How many samples were stored.
    public int getWrittenCount() {
        return sampleIds.size();
    }

    /// The platform identifiers assigned to the stored samples, in write
    /// order. Read the warning on [HealthSample#getId()] before persisting
    /// any of them.
    public List<String> getSampleIds() {
        return Collections.unmodifiableList(sampleIds);
    }

    /// One human-readable message per rejected sample, empty when
    /// everything was stored. Surface these rather than discarding them --
    /// a silently dropped sample is the hardest kind of health bug to
    /// track down later.
    public List<String> getRejections() {
        return Collections.unmodifiableList(rejections);
    }

    /// `true` when at least one sample was rejected.
    public boolean hasRejections() {
        return !rejections.isEmpty();
    }

    /// Records a stored sample. Called by [HealthStore] and ports.
    public void addSampleId(String id) {
        sampleIds.add(id);
    }

    /// Records a rejected sample. Called by [HealthStore] and ports.
    public void addRejection(String reason) {
        if (reason != null) {
            rejections.add(reason);
        }
    }

    public String toString() {
        return "HealthWriteResult[" + sampleIds.size() + " written, "
                + rejections.size() + " rejected]";
    }
}
