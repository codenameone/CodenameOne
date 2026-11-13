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
import java.util.Map;

/// One matching document.
public final class SearchHit {
    private final String id;
    private final double score;
    private final Map document;
    private final Map highlights;

    /// A hit with its id, score, stored document and highlights (field to a list
    /// of fragments; may be empty).
    public SearchHit(String id, double score, Map document, Map highlights) {
        this.id = id;
        this.score = score;
        this.document = document;
        this.highlights = highlights == null ? Collections.EMPTY_MAP : highlights;
    }

    /// The document's id.
    public String getId() {
        return id;
    }

    /// The engine's relevance score; comparable only within one result.
    public double getScore() {
        return score;
    }

    /// The stored document.
    public Map getDocument() {
        return document;
    }

    /// Field name to a `List` of fragments with the matches in `<em>`; empty
    /// unless highlights were asked for and the engine supports them.
    public Map getHighlights() {
        return highlights;
    }
}
