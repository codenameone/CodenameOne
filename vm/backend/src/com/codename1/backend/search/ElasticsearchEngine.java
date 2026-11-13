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

import com.codename1.backend.Base64;
import com.codename1.backend.Json;
import com.codename1.backend.Web;
import com.codename1.backend.aws.Aws;
import com.codename1.backend.aws.Credentials;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Elasticsearch (7 and 8) and OpenSearch over their REST API.
///
/// The request bodies used are the subset both projects share: `bool` queries
/// with `multi_match`, `match_phrase` and `term`, `terms` aggregations,
/// `highlight` and `track_total_hits`. Basic authentication comes from the URL,
/// an API key from `cn1.search.apiKey`, and an Amazon OpenSearch domain is signed
/// with SigV4 instead.
public final class ElasticsearchEngine implements SearchEngine {
    private final String base;
    private final String authorization;
    private final String awsHost;
    private final String awsService;
    private final String awsRegion;

    private ElasticsearchEngine(String base, String authorization, String awsHost,
            String awsService, String awsRegion) {
        this.base = base;
        this.authorization = authorization;
        this.awsHost = awsHost;
        this.awsService = awsService;
        this.awsRegion = awsRegion;
    }

    /// A cluster at `url` (`http://` or `https://`, user and password in the URL
    /// when it needs them); `apiKey` is the encoded key or null.
    public static ElasticsearchEngine connect(String url, String apiKey) throws IOException {
        String u = url;
        int scheme = u.indexOf("://");
        if (scheme < 0) {
            throw new IOException("Not a search URL: " + url);
        }
        String rest = u.substring(scheme + 3);
        int slash = rest.indexOf('/');
        String authority = slash < 0 ? rest : rest.substring(0, slash);
        String path = slash < 0 ? "" : rest.substring(slash);
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        String auth = null;
        int at = authority.lastIndexOf('@');
        if (at >= 0) {
            auth = "Basic " + Base64.encode(com.codename1.impl.backend.WireBuffer.utf8(
                    decode(authority.substring(0, at))));
            authority = authority.substring(at + 1);
        }
        if (apiKey != null && apiKey.length() > 0) {
            auth = "ApiKey " + apiKey;
        }
        return new ElasticsearchEngine(u.substring(0, scheme + 3) + authority + path, auth,
                null, null, null);
    }

    /// An Amazon OpenSearch domain (`service` "es") or serverless collection
    /// ("aoss") at `host`, signed with credentials resolved the usual way.
    public static ElasticsearchEngine aws(String host, String service, String region)
            throws IOException {
        String r = region;
        if (r == null || r.length() == 0) {
            r = System.getenv("AWS_REGION");
        }
        if (r == null || r.length() == 0) {
            throw new IOException("No AWS region for the search domain: add ?region= to "
                    + "cn1.search.url or set AWS_REGION");
        }
        return new ElasticsearchEngine(null, null, host, service, r);
    }

    /// Percent-decodes the user and password as UTF-8, so `p%C3%A4ss` is one
    /// character, not two.
    private static String decode(String userInfo) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        for (int i = 0; i < userInfo.length(); i++) {
            char c = userInfo.charAt(i);
            if (c == '%' && i + 2 < userInfo.length()) {
                int hi = Character.digit(userInfo.charAt(i + 1), 16);
                int lo = Character.digit(userInfo.charAt(i + 2), 16);
                if (hi < 0 || lo < 0) {
                    throw new IOException("Bad percent escape in the search URL");
                }
                out.write((hi << 4) | lo);
                i += 2;
            } else {
                int next = userInfo.indexOf('%', i + 1);
                if (next < 0) {
                    next = userInfo.length();
                }
                byte[] b = com.codename1.impl.backend.WireBuffer.utf8(userInfo.substring(i, next));
                out.write(b, 0, b.length);
                i = next - 1;
            }
        }
        return com.codename1.impl.backend.WireBuffer.fromUtf8(out.toByteArray());
    }

    /// Refuses an index name that is not one safe path segment on every engine.
    static void requireIndexName(String index) {
        boolean ok = index != null && index.length() > 0 && index.length() <= 200;
        for (int i = 0; ok && i < index.length(); i++) {
            char c = index.charAt(i);
            ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-' || c == '_';
        }
        if (!ok || index.charAt(0) == '-' || index.charAt(0) == '_') {
            throw new IllegalArgumentException("A search index name is lowercase letters, "
                    + "digits, '-' and '_', not starting with '-' or '_': " + index);
        }
    }

    private Web.Result send(String method, String path, Map query, Object body)
            throws IOException {
        byte[] bytes = body == null ? null : Json.write(body).getBytes("UTF-8");
        if (awsHost != null) {
            Map headers = new LinkedHashMap();
            if (bytes != null) {
                headers.put("content-type", "application/json");
            }
            return Aws.send(Credentials.resolve(), awsRegion, awsService, method, awsHost,
                    path, query, headers, bytes, null, true);
        }
        List headers = new ArrayList();
        if (bytes != null) {
            headers.add("Content-Type: application/json");
        }
        if (authorization != null) {
            headers.add("Authorization: " + authorization);
        }
        String q = query == null ? "" : Aws.canonicalQuery(query);
        return Web.request(method, base + Aws.encodePath(path)
                + (q.length() > 0 ? "?" + q : ""), headers, bytes);
    }

    private static Map json(Web.Result r) throws IOException {
        String text = r.getBodyAsString();
        return text == null || text.length() == 0 ? new LinkedHashMap() : Json.parseObject(text);
    }

    private static IOException failure(String what, Web.Result r) throws IOException {
        String body = r.getBodyAsString();
        if (body != null && body.length() > 500) {
            body = body.substring(0, 500);
        }
        return new IOException(what + " failed with HTTP " + r.getStatus() + ": " + body);
    }

    private static String docPath(String index, String id) {
        requireIndexName(index);
        // A deliberately narrow alphabet: the id goes into the path, where a slash
        // would address a different resource, and SigV4 and plain HTTP disagree on
        // how a percent escape is canonicalised. UUIDs, numbers and ObjectIds fit.
        boolean ok = id != null && id.length() > 0 && id.length() <= 512;
        for (int i = 0; ok && i < id.length(); i++) {
            char c = id.charAt(i);
            ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '_' || c == '.' || c == ':';
        }
        if (!ok || ".".equals(id) || "..".equals(id)) {
            throw new IllegalArgumentException("A search document id is 1 to 512 letters, "
                    + "digits, '-', '_', '.' or ':': " + id);
        }
        return "/" + index + "/_doc/" + id;
    }

    @Override
    public void index(String index, String id, Map document) throws IOException {
        Web.Result r = send("PUT", docPath(index, id), null, document);
        if (r.getStatus() != 200 && r.getStatus() != 201) {
            throw failure("Indexing " + id, r);
        }
    }

    @Override
    public boolean delete(String index, String id) throws IOException {
        Web.Result r = send("DELETE", docPath(index, id), null, null);
        if (r.getStatus() == 404) {
            return false;
        }
        if (r.getStatus() != 200) {
            throw failure("Deleting " + id, r);
        }
        return true;
    }

    @Override
    public Map get(String index, String id) throws IOException {
        Web.Result r = send("GET", docPath(index, id), null, null);
        if (r.getStatus() == 404) {
            return null;
        }
        if (r.getStatus() != 200) {
            throw failure("Reading " + id, r);
        }
        Object source = json(r).get("_source");
        return source instanceof Map ? (Map) source : null;
    }

    /// The request body for a query; package visible for tests.
    static Map body(SearchQuery q) {
        Map bool = new LinkedHashMap();
        if (q.getText().length() > 0) {
            Map mm = new LinkedHashMap();
            mm.put("query", q.getText());
            mm.put("operator", "and");
            if (!q.getFields().isEmpty()) {
                mm.put("fields", new ArrayList(q.getFields()));
            }
            Map must = new LinkedHashMap();
            must.put("multi_match", mm);
            bool.put("must", must);
        }
        List filters = new ArrayList();
        Iterator it = q.getFilters().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry e = (Map.Entry) it.next();
            Map clause = new LinkedHashMap();
            Map inner = new LinkedHashMap();
            inner.put(e.getKey(), e.getValue());
            // A string compared as a phrase works on text and keyword fields alike;
            // term on an analyzed text field would miss anything with capitals.
            clause.put(e.getValue() instanceof String ? "match_phrase" : "term", inner);
            filters.add(clause);
        }
        if (!filters.isEmpty()) {
            bool.put("filter", filters);
        }
        Map query = new LinkedHashMap();
        if (bool.isEmpty()) {
            query.put("match_all", new LinkedHashMap());
        } else {
            query.put("bool", bool);
        }
        Map body = new LinkedHashMap();
        body.put("query", query);
        body.put("from", Integer.valueOf(q.getFrom()));
        body.put("size", Integer.valueOf(q.getSize()));
        body.put("track_total_hits", Boolean.TRUE);
        if (q.getSortField() != null) {
            Map order = new LinkedHashMap();
            order.put("order", q.isSortAscending() ? "asc" : "desc");
            Map s = new LinkedHashMap();
            s.put(q.getSortField(), order);
            List sort = new ArrayList();
            sort.add(s);
            body.put("sort", sort);
        }
        if (q.isHighlight()) {
            Map fields = new LinkedHashMap();
            if (q.getFields().isEmpty()) {
                fields.put("*", new LinkedHashMap());
            } else {
                for (int i = 0; i < q.getFields().size(); i++) {
                    fields.put(q.getFields().get(i), new LinkedHashMap());
                }
            }
            Map h = new LinkedHashMap();
            h.put("fields", fields);
            body.put("highlight", h);
        }
        if (!q.getFacets().isEmpty()) {
            Map aggs = new LinkedHashMap();
            for (int i = 0; i < q.getFacets().size(); i++) {
                String f = (String) q.getFacets().get(i);
                Map terms = new LinkedHashMap();
                terms.put("field", f);
                terms.put("size", Integer.valueOf(50));
                Map agg = new LinkedHashMap();
                agg.put("terms", terms);
                aggs.put(f, agg);
            }
            body.put("aggs", aggs);
        }
        return body;
    }

    @Override
    public SearchResult search(String index, SearchQuery q) throws IOException {
        requireIndexName(index);
        Web.Result r = send("POST", "/" + index + "/_search", null, body(q));
        if (r.getStatus() == 404) {
            return new SearchResult(new ArrayList(), 0, true, null);
        }
        if (r.getStatus() != 200) {
            throw failure("Searching " + index, r);
        }
        return parse(json(r));
    }

    static SearchResult parse(Map response) {
        Map hitsNode = (Map) response.get("hits");
        List hits = new ArrayList();
        long total = 0;
        boolean exact = true;
        if (hitsNode != null) {
            Object t = hitsNode.get("total");
            if (t instanceof Map) {
                Object v = ((Map) t).get("value");
                total = v instanceof Number ? ((Number) v).longValue() : 0;
                exact = !"gte".equals(((Map) t).get("relation"));
            } else if (t instanceof Number) {
                total = ((Number) t).longValue();
            }
            Object list = hitsNode.get("hits");
            if (list instanceof List) {
                List l = (List) list;
                for (int i = 0; i < l.size(); i++) {
                    Map h = (Map) l.get(i);
                    Object score = h.get("_score");
                    hits.add(new SearchHit(String.valueOf(h.get("_id")),
                            score instanceof Number ? ((Number) score).doubleValue() : 0,
                            (Map) h.get("_source"), (Map) h.get("highlight")));
                }
            }
        }
        Map facets = new LinkedHashMap();
        Object aggs = response.get("aggregations");
        if (aggs instanceof Map) {
            Iterator it = ((Map) aggs).entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry e = (Map.Entry) it.next();
                Map counts = new LinkedHashMap();
                Object buckets = ((Map) e.getValue()).get("buckets");
                if (buckets instanceof List) {
                    List b = (List) buckets;
                    for (int i = 0; i < b.size(); i++) {
                        Map bucket = (Map) b.get(i);
                        Object key = bucket.get("key_as_string");
                        if (key == null) {
                            key = bucket.get("key");
                        }
                        Object n = bucket.get("doc_count");
                        counts.put(String.valueOf(key),
                                Long.valueOf(n instanceof Number ? ((Number) n).longValue() : 0));
                    }
                }
                facets.put(e.getKey(), counts);
            }
        }
        return new SearchResult(hits, total, exact, facets);
    }

    @Override
    public void refresh(String index) throws IOException {
        requireIndexName(index);
        Web.Result r = send("POST", "/" + index + "/_refresh", null, null);
        if (r.getStatus() != 200 && r.getStatus() != 404) {
            throw failure("Refreshing " + index, r);
        }
    }

    @Override
    public void deleteIndex(String index) throws IOException {
        requireIndexName(index);
        Web.Result r = send("DELETE", "/" + index, null, null);
        if (r.getStatus() != 200 && r.getStatus() != 404) {
            throw failure("Deleting index " + index, r);
        }
    }

    @Override
    public void close() {
        // Every request is independent; there is nothing held open.
    }
}
