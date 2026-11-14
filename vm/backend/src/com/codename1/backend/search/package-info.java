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

/// Full-text search behind one interface, over Elasticsearch, OpenSearch or the
/// application's own SQL database.
///
/// [SearchEngines#current()] opens the engine `cn1.search.url` names:
///
/// | URL | Engine |
/// |---|---|
/// | `https://user:pass@host:9200` | Elasticsearch or OpenSearch over its REST API |
/// | `aws://search-domain.us-east-1.es.amazonaws.com` | Amazon OpenSearch Service, signed with SigV4 |
/// | `aoss://id.us-east-1.aoss.amazonaws.com` | Amazon OpenSearch Serverless, signed with SigV4 |
/// | `database:` (the default) | the configured SQL database: PostgreSQL text search, MySQL `FULLTEXT`, or a scan on SQLite |
///
/// Code written against [SearchEngine] runs unchanged on each, so a project can
/// start on its database and move to a search cluster by changing one setting.
/// What differs is relevance: each engine ranks by its own scoring, and the SQL
/// engine applies filters, sorting and facets in memory over at most
/// [SqlSearchEngine#MAX_CANDIDATES] matches, which is right for a catalogue of
/// thousands and wrong for one of millions.
package com.codename1.backend.search;
