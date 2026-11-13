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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// What to search for: text, exact filters, an order, a page, and the fields to
/// count facet values of.
///
/// ```java
/// SearchQuery q = new SearchQuery("red running shoes")
///         .fields("name", "description")
///         .filter("brand", "acme")
///         .facet("color")
///         .page(0, 20);
/// ```
public final class SearchQuery {
    private final String text;
    private final List fields = new ArrayList();
    private final Map filters = new LinkedHashMap();
    private final List facets = new ArrayList();
    private String sortField;
    private boolean sortAscending = true;
    private int from;
    private int size = 10;
    private boolean highlight;

    /// A query for `text`; null or empty matches every document.
    public SearchQuery(String text) {
        this.text = text == null ? "" : text.trim();
    }

    /// Matches only in these fields instead of every string.
    public SearchQuery fields(String... names) {
        for (int i = 0; i < names.length; i++) {
            fields.add(names[i]);
        }
        return this;
    }

    /// Keeps only documents whose `field` equals `value` exactly. Several
    /// filters must all hold.
    public SearchQuery filter(String field, Object value) {
        filters.put(field, value);
        return this;
    }

    /// Orders by a field instead of by relevance.
    public SearchQuery sort(String field, boolean ascending) {
        this.sortField = field;
        this.sortAscending = ascending;
        return this;
    }

    /// The page: skip `from` hits and return at most `size`.
    public SearchQuery page(int from, int size) {
        this.from = Math.max(0, from);
        this.size = Math.max(0, size);
        return this;
    }

    /// Counts the values of `field` over all matches, in
    /// [SearchResult#getFacets()]. On Elasticsearch and OpenSearch the field must
    /// be a keyword field -- with dynamic mapping, `name.keyword` for `name`.
    public SearchQuery facet(String field) {
        facets.add(field);
        return this;
    }

    /// Asks for highlighted fragments of the matched text.
    public SearchQuery highlight(boolean on) {
        this.highlight = on;
        return this;
    }

    /// The text, trimmed; empty matches everything.
    public String getText() {
        return text;
    }

    /// The fields to match in; empty for all.
    public List getFields() {
        return Collections.unmodifiableList(fields);
    }

    /// Field to exact value.
    public Map getFilters() {
        return Collections.unmodifiableMap(filters);
    }

    /// The facet fields.
    public List getFacets() {
        return Collections.unmodifiableList(facets);
    }

    /// The sort field, or null for relevance.
    public String getSortField() {
        return sortField;
    }

    /// Whether the sort is ascending.
    public boolean isSortAscending() {
        return sortAscending;
    }

    /// Hits skipped.
    public int getFrom() {
        return from;
    }

    /// Hits returned at most.
    public int getSize() {
        return size;
    }

    /// Whether highlights were asked for.
    public boolean isHighlight() {
        return highlight;
    }
}
