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

/// One page of samples, plus the token needed to fetch the next.
///
/// ```java
/// String token = null;
/// do {
///     SamplePage page = store.readSamplePage(query.setPageToken(token)).get();
///     process(page.getSamples());
///     token = page.getNextPageToken();
/// } while (token != null);
/// ```
public final class SamplePage {

    private final List<HealthSample> samples;
    private final String nextPageToken;
    private final boolean truncated;

    /// Creates a page. The sample list is copied defensively.
    public SamplePage(List<HealthSample> samples, String nextPageToken,
            boolean truncated) {
        List<HealthSample> copy = new ArrayList<HealthSample>();
        if (samples != null) {
            copy.addAll(samples);
        }
        this.samples = copy;
        this.nextPageToken = nextPageToken;
        this.truncated = truncated;
    }

    /// The samples in this page, never null.
    public List<HealthSample> getSamples() {
        return Collections.unmodifiableList(samples);
    }

    /// The token that fetches the following page, or null when this is the
    /// last one. Pass it to [SampleQuery#setPageToken(String)].
    public String getNextPageToken() {
        return nextPageToken;
    }

    /// `true` when the query's limit cut the result short and more data
    /// matched than was returned.
    ///
    /// Distinct from having a next-page token: a page can be the last one
    /// and still be truncated, which means the store stopped early rather
    /// than running out of data. Worth surfacing rather than quietly
    /// showing a partial total.
    public boolean isTruncated() {
        return truncated;
    }

    /// How many samples this page holds.
    public int size() {
        return samples.size();
    }

    /// `true` when this page holds no samples.
    public boolean isEmpty() {
        return samples.isEmpty();
    }

    public String toString() {
        return "SamplePage[" + samples.size() + " samples"
                + (nextPageToken == null ? "" : ", more") + "]";
    }
}
