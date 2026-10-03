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
package com.codenameone.examples.hellocodenameone.backend;

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMultipartFile;
import com.codename1.backend.test.MockMvc;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.multipart;
import static com.codename1.backend.test.MockMvcRequestBuilders.options;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcRequestBuilders.put;
import static com.codename1.backend.test.MockMvcRequestBuilders.delete;
import static com.codename1.backend.test.MockMvcResultMatchers.content;
import static com.codename1.backend.test.MockMvcResultMatchers.cookie;
import static com.codename1.backend.test.MockMvcResultMatchers.header;
import static com.codename1.backend.test.MockMvcResultMatchers.jsonPath;
import static com.codename1.backend.test.MockMvcResultMatchers.redirectedUrl;
import static com.codename1.backend.test.MockMvcResultMatchers.status;

/// The probe routes the app's networking tests use, checked in process first, so a
/// device failure points at the client rather than the server.
@BackendTest
class ProbeApiTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void echoesTheRequest() throws Exception {
        mvc.perform(post("/api/echo").param("a", "1").header("X-Test", "yes")
                        .contentType("text/plain").content("body text"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Probe", "echo"))
                .andExpect(jsonPath("$.method").value("POST"))
                .andExpect(jsonPath("$.params.a").value("1"))
                .andExpect(jsonPath("$.headers.X-Test").value("yes"))
                .andExpect(jsonPath("$.body").value("body text"));
    }

    @Test
    void mockMvcReportsWhatTheWireCarries() throws Exception {
        // A test's own lower-case Host is the Host the request carries; a default
        // added beside it under another spelling used to replace it.
        String echoed = mvc.perform(get("/api/echo").header("host", "api.example"))
                .andReturn().getResponse().getContentAsString();
        assertTrue(echoed.indexOf("api.example") >= 0, echoed);
        assertTrue(echoed.indexOf("localhost") < 0, echoed);
        // Content-Type is a header like any other, though the server keeps it apart.
        mvc.perform(get("/api/hello/{name}", "Ada"))
                .andExpect(header().exists("Content-Type"))
                .andExpect(header().string("content-type", "text/plain; charset=utf-8"));
    }

    @Test
    void aMultipartRequestKeepsItsQueryParametersInTheUrl() throws Exception {
        // queryParam on a multipart builder goes in the URL; param is a form field.
        mvc.perform(multipart("/api/echo").queryParam("q", "inUrl").param("a", "inForm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.target").value("/api/echo?q=inUrl"))
                .andExpect(jsonPath("$.params.q").value("inUrl"))
                .andExpect(jsonPath("$.params.a").value("inForm"));
    }

    @Test
    void aJsonNullDoesNotExist() throws Exception {
        // As in Spring: a null-valued property fails exists() and passes doesNotExist().
        mvc.perform(get("/api/json/map"))
                .andExpect(jsonPath("$.nothing").doesNotExist())
                .andExpect(jsonPath("$.absent").doesNotExist());
        boolean existed;
        try {
            mvc.perform(get("/api/json/map")).andExpect(jsonPath("$.nothing").exists());
            existed = true;
        } catch (AssertionError expected) {
            existed = false;
        }
        assertTrue(!existed, "exists() passed for a JSON null");
    }

    @Test
    void andExpectAllReportsAMatcherThatThrowsAndRunsTheRest() throws Exception {
        // A Content-Type is no number: longValue throws, and the status check after
        // it must still run and be reported with it.
        String report;
        try {
            mvc.perform(get("/api/hello/{name}", "Ada")).andExpectAll(
                    header().longValue("Content-Type", 1),
                    status().is(418));
            report = null;
        } catch (AssertionError expected) {
            report = expected.getMessage();
        }
        assertTrue(report != null, "two failed expectations passed");
        assertTrue(report.indexOf("NumberFormatException") >= 0, report);
        assertTrue(report.indexOf("418") >= 0, "the expectation after the throw did not run: " + report);
    }

    @Test
    void anAbsentJsonPathIsNotEmpty() throws Exception {
        boolean passed;
        try {
            mvc.perform(get("/api/json/map")).andExpect(jsonPath("$.absent").isEmpty());
            passed = true;
        } catch (AssertionError expected) {
            passed = false;
        }
        assertTrue(!passed, "isEmpty() passed for a path that is not there");
        mvc.perform(get("/api/json/map")).andExpect(jsonPath("$.nothing").isEmpty());
    }

    @Test
    void aPassingAssertThrowsNeverAsksForItsMessage() {
        // JUnit builds the message only for a failure; compiled must too.
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> { throw new IllegalStateException("expected"); },
                () -> { throw new AssertionError("the message supplier ran for a passing assertion"); });
    }

    @Test
    void nestedIterablesCompareByIteration() {
        java.util.List<Object> lists = new java.util.ArrayList<Object>();
        lists.add(java.util.Arrays.asList("a", "b"));
        java.util.List<Object> sets = new java.util.ArrayList<Object>();
        sets.add(new java.util.LinkedHashSet<Object>(java.util.Arrays.asList("a", "b")));
        org.junit.jupiter.api.Assertions.assertIterableEquals(lists, sets);
    }

    @Test
    void assertAllRethrowsAnOutOfMemoryError() {
        org.junit.jupiter.api.Assertions.assertThrows(OutOfMemoryError.class,
                () -> org.junit.jupiter.api.Assertions.assertAll(
                        () -> { throw new OutOfMemoryError("simulated"); },
                        () -> { throw new AssertionError("ran after an unrecoverable error"); }));
    }

    @Test
    void assertDoesNotThrowRethrowsAnOutOfMemoryError() {
        org.junit.jupiter.api.Assertions.assertThrows(OutOfMemoryError.class,
                () -> org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                        (org.junit.jupiter.api.function.Executable)
                                () -> { throw new OutOfMemoryError("simulated"); }));
    }

    @Test
    void nestedPrimitiveArraysCompareByContent() {
        // The rows of an int[][] are compared by content, compiled as on the JVM.
        assertArrayEquals(new int[][] {{1, 2}, {3}}, new int[][] {{1, 2}, {3}});
        assertArrayEquals(new double[][] {{0.5}}, new double[][] {{0.5}});
    }

    @Test
    void bodiesTheWireOmitsAreOmitted() throws Exception {
        // A 205 carries no body on the wire, though this handler builds "status 205".
        mvc.perform(get("/api/status/205")).andExpect(status().is(205))
                .andExpect(content().string(""));
    }

    @Test
    void statusesAndRedirects() throws Exception {
        mvc.perform(get("/api/status/418")).andExpect(status().is(418))
                .andExpect(content().string("status 418"));
        mvc.perform(get("/api/status/204")).andExpect(status().isNoContent());
        mvc.perform(get("/api/redirect/2")).andExpect(status().isFound())
                .andExpect(redirectedUrl("/api/redirect/1"));
        mvc.perform(get("/api/redirect/0")).andExpect(content().string("landed"));
    }

    @Test
    void cookiesAndAuthentication() throws Exception {
        mvc.perform(get("/api/cookie/set").param("name", "flavor").param("value", "oat"))
                .andExpect(cookie().value("flavor", "oat"));
        mvc.perform(get("/api/cookie/read").param("name", "flavor").cookie("flavor", "rye"))
                .andExpect(content().string("rye"));
        mvc.perform(get("/api/auth/basic")).andExpect(status().isUnauthorized())
                .andExpect(header().exists("WWW-Authenticate"));
        mvc.perform(get("/api/auth/basic").header("Authorization", ProbeApi.BASIC))
                .andExpect(content().string("user"));
        mvc.perform(get("/api/auth/bearer").header("Authorization", ProbeApi.TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void uploadsAndRawBodies() throws Exception {
        byte[] file = {0, 1, 2, (byte) 0xff, 13, 10};
        mvc.perform(multipart("/api/upload")
                        .file(new MockMultipartFile("file", "probe.bin", "application/octet-stream",
                                file))
                        .param("note", "hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("probe.bin"))
                .andExpect(jsonPath("$.size").value(6))
                .andExpect(jsonPath("$.sum").value(ProbeApi.checksum(file)))
                .andExpect(jsonPath("$.note").value("hello"));
        mvc.perform(post("/api/raw").contentType("application/octet-stream").content(file))
                .andExpect(jsonPath("$.size").value(6));
    }

    @Test
    void largeAnswersAreGzippedWhenAsked() throws Exception {
        mvc.perform(get("/api/big").param("size", "8192").header("Accept-Encoding", "gzip"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Encoding", "gzip"));
        String plain = mvc.perform(get("/api/big").param("size", "8192"))
                .andExpect(header().doesNotExist("Content-Encoding"))
                .andReturn().getResponse().getContentAsString();
        assertEquals(8192, plain.length());
    }

    @Test
    void corsAnswersAPreflight() throws Exception {
        mvc.perform(options("/api/echo").header("Origin", "http://127.0.0.1:9000")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"));
        String exposed = mvc.perform(get("/api/headers").header("Origin", "http://127.0.0.1:9000"))
                .andReturn().getResponse().getHeader("Access-Control-Expose-Headers");
        assertTrue(exposed != null && exposed.indexOf("X-Probe") >= 0, exposed);
    }

    @Test
    void jsonShapes() throws Exception {
        mvc.perform(get("/api/json/list"))
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[5]").value("six"));
        mvc.perform(get("/api/json/map"))
                .andExpect(jsonPath("$.nested.depth").value(2))
                .andExpect(jsonPath("$.nested.tags[0]").value(1))
                .andExpect(jsonPath("$.ratio").value(0.5))
                .andExpect(content().json("{\"title\":\"probe\",\"count\":3}"));
    }
}
