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
package com.codename1.io.rest;

import com.codename1.junit.UITestBase;
import com.codename1.mapping.Mapper;
import com.codename1.mapping.Mappers;
import com.codename1.testing.TestCodenameOneImplementation.TestConnection;
import com.codename1.util.OnComplete;
import com.codename1.xml.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage for the private {@code RequestBuilder.FetchAsMappedListActionListener},
 * reached through {@link RequestBuilder#fetchAsMappedList(Class, OnComplete)}.
 * Exercises the happy path (a JSON array mapped through a registered
 * {@link Mapper}) and the two empty-list fallbacks (no mapper registered, and a
 * non-array JSON body with no synthetic {@code "root"} list).
 */
class FetchAsMappedListTest extends UITestBase {

    private static final String BASE_URL = "https://example.com";

    @BeforeEach
    void clearConnections() {
        implementation.clearConnections();
        implementation.clearQueuedRequests();
        Mappers.register(new AlbumMapper());
    }

    @Test
    void mapsJsonArrayThroughRegisteredMapper() {
        prepareJson(BASE_URL + "/albums", "[{\"title\":\"A\"},{\"title\":\"B\"}]");

        Response<List<Album>> response = fetch(BASE_URL + "/albums", Album.class);
        assertNotNull(response);
        assertEquals(200, response.getResponseCode());
        List<Album> albums = response.getResponseData();
        assertEquals(2, albums.size());
        assertEquals("A", albums.get(0).title);
        assertEquals("B", albums.get(1).title);
    }

    @Test
    void unregisteredTypeYieldsEmptyList() {
        prepareJson(BASE_URL + "/unmapped", "[{\"title\":\"A\"}]");

        Response<List<Unmapped>> response = fetch(BASE_URL + "/unmapped", Unmapped.class);
        assertNotNull(response);
        assertTrue(response.getResponseData().isEmpty());
    }

    @Test
    void nonArrayBodyYieldsEmptyList() {
        // A top-level object has no synthetic "root" list, so the listener
        // returns an empty list rather than failing.
        prepareJson(BASE_URL + "/object", "{\"title\":\"A\"}");

        Response<List<Album>> response = fetch(BASE_URL + "/object", Album.class);
        assertNotNull(response);
        assertTrue(response.getResponseData().isEmpty());
    }

    private <T> Response<List<T>> fetch(String url, Class<T> type) {
        RequestBuilder builder = new RequestBuilder("GET", url);
        final AtomicReference<Response<List<T>>> holder = new AtomicReference<Response<List<T>>>();
        final CountDownLatch latch = new CountDownLatch(1);
        builder.fetchAsMappedList(type, new OnComplete<Response<List<T>>>() {
            public void completed(Response<List<T>> value) {
                holder.set(value);
                latch.countDown();
            }
        });
        waitFor(latch, 2000);
        return holder.get();
    }

    private void prepareJson(String url, String json) {
        TestConnection connection = implementation.createConnection(url);
        byte[] payload = json.getBytes(StandardCharsets.UTF_8);
        connection.setInputData(payload);
        connection.setContentLength(payload.length);
        connection.setResponseCode(200);
        connection.setResponseMessage("OK");
    }

    /** Simple value object the mapper produces. */
    static final class Album {
        String title;
    }

    /** Type with no registered mapper, used to drive the empty-list fallback. */
    static final class Unmapped {
    }

    /** Hand-written mapper (no generated code, no Mockito) for {@link Album}. */
    static final class AlbumMapper implements Mapper<Album> {
        public Class<Album> type() {
            return Album.class;
        }

        public Map<String, Object> toMap(Album instance) {
            return null;
        }

        public Album fromMap(Map<String, Object> map) {
            Album a = new Album();
            Object t = map.get("title");
            a.title = t == null ? null : t.toString();
            return a;
        }

        public String xmlRootName() {
            return "album";
        }

        public void writeXml(Album instance, Element root) {
        }

        public Album readXml(Element root) {
            return new Album();
        }
    }
}
