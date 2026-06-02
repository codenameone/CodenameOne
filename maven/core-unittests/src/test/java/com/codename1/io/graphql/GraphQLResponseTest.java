/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 */
package com.codename1.io.graphql;

import com.codename1.io.JSONParser;
import com.codename1.mapping.Mapper;
import com.codename1.mapping.Mappers;
import com.codename1.xml.Element;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Unit tests for the GraphQL runtime envelope: request-body building,
/// variable encoding, and the `data` / `errors` response decode path.
/// None of these touch the network, so they exercise the pure helpers
/// directly.
class GraphQLResponseTest {

    enum Episode { NEWHOPE, EMPIRE, JEDI }

    static final class HeroData {
        String hero;
    }

    /// Minimal hand-written mapper so `decodeJson` can map `data` to a
    /// type without the build-time annotation processor.
    private static void registerHeroMapper() {
        Mappers.register(new Mapper<HeroData>() {
            public Class<HeroData> type() { return HeroData.class; }
            public Map<String, Object> toMap(HeroData instance) {
                Map<String, Object> m = new LinkedHashMap<String, Object>();
                m.put("hero", instance.hero);
                return m;
            }
            public HeroData fromMap(Map<String, Object> map) {
                HeroData d = new HeroData();
                Object h = map.get("hero");
                d.hero = h == null ? null : String.valueOf(h);
                return d;
            }
            public String xmlRootName() { return "heroData"; }
            public void writeXml(HeroData instance, Element root) { }
            public HeroData readXml(Element root) { return new HeroData(); }
        });
    }

    @Test
    void buildRequestBodyCarriesQueryOperationAndVariables() throws Exception {
        Map<String, Object> vars = new LinkedHashMap<String, Object>();
        vars.put("episode", Episode.JEDI);
        String body = GraphQL.buildRequestBody("HeroName",
                "query HeroName($episode: Episode) { hero(episode: $episode) { name } }", vars);

        Map<String, Object> parsed = JSONParser.parseJSON(body);
        assertEquals("query HeroName($episode: Episode) { hero(episode: $episode) { name } }",
                parsed.get("query"));
        assertEquals("HeroName", parsed.get("operationName"));
        Object v = parsed.get("variables");
        assertTrue(v instanceof Map, "variables should be an object");
        assertEquals("JEDI", ((Map<?, ?>) v).get("episode"));
    }

    @Test
    void encodeVariablesSerialisesScalarsEnumsListsAndMaps() throws Exception {
        Map<String, Object> vars = new LinkedHashMap<String, Object>();
        vars.put("episode", Episode.EMPIRE);
        vars.put("count", Integer.valueOf(3));
        vars.put("flag", Boolean.TRUE);
        List<String> tags = new ArrayList<String>();
        tags.add("a");
        tags.add("b");
        vars.put("tags", tags);
        Map<String, Object> nested = new LinkedHashMap<String, Object>();
        nested.put("k", "v");
        vars.put("nested", nested);

        String encoded = GraphQL.encodeVariables(vars);
        // Boolean is asserted against the raw JSON: JSONParser.parseJSON
        // re-reads `true` as the String "true" with its default config.
        assertTrue(encoded.contains("\"flag\":true"), "boolean literal; was: " + encoded);

        Map<String, Object> parsed = JSONParser.parseJSON(encoded);
        assertEquals("EMPIRE", parsed.get("episode"));
        assertEquals(3L, ((Number) parsed.get("count")).longValue());
        assertTrue(parsed.get("tags") instanceof List);
        assertEquals(2, ((List<?>) parsed.get("tags")).size());
        assertEquals("v", ((Map<?, ?>) parsed.get("nested")).get("k"));
    }

    @Test
    void decodeMapsDataToTypedObject() {
        registerHeroMapper();
        byte[] body = "{\"data\":{\"hero\":\"R2-D2\"}}".getBytes(StandardCharsets.UTF_8);
        GraphQLResponse<HeroData> r = GraphQL.decodeJson(body, 200, HeroData.class);
        assertTrue(r.isOk());
        assertFalse(r.hasErrors());
        assertNotNull(r.getData());
        assertEquals("R2-D2", r.getData().hero);
        assertEquals(200, r.getResponseCode());
    }

    @Test
    void decodeExtractsErrorsAlongsidePartialData() {
        registerHeroMapper();
        byte[] body = ("{\"data\":{\"hero\":null},"
                + "\"errors\":[{\"message\":\"boom\",\"path\":[\"hero\"]}]}")
                .getBytes(StandardCharsets.UTF_8);
        GraphQLResponse<HeroData> r = GraphQL.decodeJson(body, 200, HeroData.class);
        assertTrue(r.hasErrors());
        assertFalse(r.isOk());
        assertEquals(1, r.getErrors().size());
        assertEquals("boom", r.getErrors().get(0).getMessage());
        assertEquals("boom", r.getResponseErrorMessage());
        assertNotNull(r.getData()); // partial result: data object present, hero null
        assertNull(r.getData().hero);
    }

    @Test
    void decodeEmptyBodyIsTransportFailure() {
        GraphQLResponse<HeroData> r = GraphQL.decodeJson(new byte[0], 0, HeroData.class);
        assertNull(r.getData());
        assertNotNull(r.getResponseErrorMessage());
    }

    @Test
    void webSocketUrlRewritesScheme() {
        assertEquals("wss://api.example.com/graphql",
                GraphQL.toWebSocketUrl("https://api.example.com/graphql"));
        assertEquals("ws://localhost:8080/graphql",
                GraphQL.toWebSocketUrl("http://localhost:8080/graphql"));
        assertEquals("wss://already/ws", GraphQL.toWebSocketUrl("wss://already/ws"));
    }
}
