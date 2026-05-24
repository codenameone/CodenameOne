package com.codename1.router.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AasaBuilderTest {

    @Test
    void buildsCanonicalEnvelope() {
        String json = new AasaBuilder()
                .appId("ABCD1234.com.example.app")
                .addPath("/share/*")
                .addPath("NOT /admin/*")
                .build();
        assertTrue(json.contains("\"applinks\""));
        assertTrue(json.contains("\"ABCD1234.com.example.app\""));
        assertTrue(json.contains("\"/\": \"/share/*\""));
        assertTrue(json.contains("\"/\": \"/admin/*\""));
        assertTrue(json.contains("\"exclude\": true"));
    }

    @Test
    void routerPatternIsConvertedToAasaWildcards() {
        assertEquals("/users/*", AasaBuilder.toAasaPath("/users/:id"));
        assertEquals("/files/*", AasaBuilder.toAasaPath("/files/*"));
        assertEquals("/share/*", AasaBuilder.toAasaPath("/share/**"));
    }

    @Test
    void multipleAppEntries() {
        String json = new AasaBuilder()
                .appId("T.com.example.a").addPath("/a/*")
                .appId("T.com.example.b").addPath("/b/*")
                .build();
        // Crude but resilient: both team IDs present, two object entries.
        assertTrue(json.contains("T.com.example.a"));
        assertTrue(json.contains("T.com.example.b"));
        int firstAppIDs = json.indexOf("\"appIDs\"");
        int secondAppIDs = json.indexOf("\"appIDs\"", firstAppIDs + 1);
        assertTrue(secondAppIDs > firstAppIDs, "two appIDs blocks expected");
    }

    @Test
    void addPathBeforeAppIdThrows() {
        assertThrows(IllegalStateException.class, new org.junit.jupiter.api.function.Executable() {
            @Override public void execute() { new AasaBuilder().addPath("/x"); }
        });
    }
}
