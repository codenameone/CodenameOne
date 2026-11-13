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
package com.codename1.backend.search;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/// One page of hits, the total and the facet counts.
public final class SearchResult {
    private final List hits;
    private final long total;
    private final boolean totalExact;
    private final Map facets;

    /// A result. `facets` maps a field to a map of value to `Long` count.
    public SearchResult(List hits, long total, boolean totalExact, Map facets) {
        this.hits = hits;
        this.total = total;
        this.totalExact = totalExact;
        this.facets = facets == null ? Collections.EMPTY_MAP : facets;
    }

    /// The [SearchHit]s of the page, best first unless a sort was given.
    public List getHits() {
        return hits;
    }

    /// How many documents matched in all.
    public long getTotal() {
        return total;
    }

    /// False when [#getTotal()] is a lower bound: the engine stopped counting.
    public boolean isTotalExact() {
        return totalExact;
    }

    /// Facet field to a map of value to `Long` count, most frequent first.
    public Map getFacets() {
        return facets;
    }
}
