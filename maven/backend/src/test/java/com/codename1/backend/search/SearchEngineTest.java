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

import com.codename1.backend.DataSource;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// One scenario run against every engine: in-memory SQLite always, the database
/// CN1_TEST_DATABASE_URL names and the cluster CN1_TEST_SEARCH_URL names when set.
class SearchEngineTest {
    @Test
    void sqlite() throws Exception {
        SearchEngine e = new SqlSearchEngine(DataSource.open(":memory:", 1), true);
        try {
            scenario(e, "color");
        } finally {
            e.close();
        }
    }

    @Test
    void database() throws Exception {
        String url = System.getenv("CN1_TEST_DATABASE_URL");
        Assumptions.assumeTrue(url != null && url.length() > 0,
                "CN1_TEST_DATABASE_URL is not set");
        SearchEngine e = new SqlSearchEngine(DataSource.open(url, 2), true);
        try {
            scenario(e, "color");
        } finally {
            e.close();
        }
    }

    @Test
    void cluster() throws Exception {
        String url = System.getenv("CN1_TEST_SEARCH_URL");
        Assumptions.assumeTrue(url != null && url.length() > 0,
                "CN1_TEST_SEARCH_URL is not set");
        SearchEngine e = SearchEngines.open(url, null);
        try {
            scenario(e, "color.keyword");
        } finally {
            e.close();
        }
    }

    @Test
    void queryBodyAndTerms() {
        Map body = ElasticsearchEngine.body(new SearchQuery("red shoe").fields("name")
                .filter("brand", "acme").filter("stock", Integer.valueOf(3)).page(20, 10));
        assertEquals(Integer.valueOf(20), body.get("from"));
        String json = com.codename1.backend.Json.write(body);
        assertTrue(json.indexOf("\"multi_match\"") > 0);
        assertTrue(json.indexOf("\"match_phrase\":{\"brand\":\"acme\"}") > 0);
        assertTrue(json.indexOf("\"term\":{\"stock\":3}") > 0);
        assertEquals(Arrays.asList(new Object[] {"red", "shoes", "o", "brien"}),
                SqlSearchEngine.terms(" Red+SHOES  O'Brien% "));
        assertThrows(IllegalArgumentException.class,
                () -> ElasticsearchEngine.requireIndexName("../x"));
        assertThrows(IllegalArgumentException.class,
                () -> ElasticsearchEngine.requireIndexName("Upper"));
    }

    private static Map product(String name, String brand, String color, int price) {
        Map m = new LinkedHashMap();
        m.put("name", name);
        m.put("brand", brand);
        m.put("color", color);
        m.put("price", Integer.valueOf(price));
        return m;
    }

    private static void scenario(SearchEngine e, String colorFacet) throws Exception {
        String index = "products-" + Long.toString(System.nanoTime(), 36);
        try {
            e.index(index, "1", product("Red running shoes", "acme", "red", 80));
            e.index(index, "2", product("Blue running shoes", "acme", "blue", 60));
            e.index(index, "3", product("Red rain jacket", "zeta", "red", 120));
            e.index(index, "4", product("Trail shoes, red laces", "zeta", "green", 95));
            e.index(index, "4", product("Trail running shoes", "zeta", "green", 95));
            e.refresh(index);

            SearchResult r = e.search(index, new SearchQuery("running SHOES"));
            assertEquals(3, r.getTotal());
            r = e.search(index, new SearchQuery("red running").facet(colorFacet));
            assertEquals(1, r.getTotal());
            assertEquals("1", ((SearchHit) r.getHits().get(0)).getId());

            r = e.search(index, new SearchQuery("shoes").filter("brand", "zeta"));
            assertEquals(1, r.getTotal());
            assertEquals("4", ((SearchHit) r.getHits().get(0)).getId());

            r = e.search(index, new SearchQuery("red").fields("name"));
            assertEquals(2, r.getTotal());

            r = e.search(index, new SearchQuery("").sort("price", false).page(1, 2)
                    .facet(colorFacet));
            assertEquals(4, r.getTotal());
            List hits = r.getHits();
            assertEquals(2, hits.size());
            assertEquals("4", ((SearchHit) hits.get(0)).getId());
            assertEquals("1", ((SearchHit) hits.get(1)).getId());
            Map colors = (Map) r.getFacets().get(colorFacet);
            assertEquals(Long.valueOf(2), colors.get("red"));
            assertEquals("red", colors.keySet().iterator().next());

            assertEquals("Blue running shoes", e.get(index, "2").get("name"));
            assertTrue(e.delete(index, "2"));
            assertFalse(e.delete(index, "2"));
            assertNull(e.get(index, "2"));
            e.refresh(index);
            assertEquals(2, e.search(index, new SearchQuery("running")).getTotal());

            // Whole words, Unicode case, and words InnoDB does not index.
            e.index(index, "5", product("The Matrix on a go kart", "neo", "black", 10));
            e.index(index, "6", product("\u00c9lan category", "neo", "white", 10));
            e.refresh(index);
            assertEquals(1, e.search(index, new SearchQuery("the matrix")).getTotal());
            assertEquals(1, e.search(index, new SearchQuery("go kart")).getTotal());
            assertEquals(1, e.search(index, new SearchQuery("\u00e9lan")).getTotal());
            assertEquals(0, e.search(index, new SearchQuery("cat")).getTotal());
        } finally {
            e.deleteIndex(index);
        }
    }
}
