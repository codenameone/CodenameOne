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
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage for the private {@code RequestBuilder.FetchAsMappedActionListener},
 * reached through {@link RequestBuilder#fetchAsMapped(Class, OnComplete)}.
 * Exercises mapping a single JSON object through a registered {@link Mapper}
 * and the empty result when no mapper is registered for the type.
 */
class FetchAsMappedActionListenerTest extends UITestBase {

    private static final String BASE_URL = "https://example.com";

    @BeforeEach
    void setUp() {
        implementation.clearConnections();
        implementation.clearQueuedRequests();
        Mappers.register(new AssetMapper());
    }

    @Test
    void mapsJsonObjectThroughRegisteredMapper() {
        prepareJson(BASE_URL + "/asset", "{\"title\":\"Widget\"}");

        Response<Asset> response = fetch(BASE_URL + "/asset", Asset.class);
        assertNotNull(response);
        assertEquals(200, response.getResponseCode());
        assertNotNull(response.getResponseData());
        assertEquals("Widget", response.getResponseData().title);
    }

    @Test
    void unregisteredTypeYieldsNullData() {
        prepareJson(BASE_URL + "/unmapped", "{\"title\":\"Widget\"}");

        Response<Unmapped> response = fetch(BASE_URL + "/unmapped", Unmapped.class);
        assertNotNull(response);
        assertEquals(200, response.getResponseCode());
        assertNull(response.getResponseData());
    }

    private <T> Response<T> fetch(String url, Class<T> type) {
        RequestBuilder builder = new RequestBuilder("GET", url);
        final AtomicReference<Response<T>> holder = new AtomicReference<Response<T>>();
        final CountDownLatch latch = new CountDownLatch(1);
        builder.fetchAsMapped(type, new OnComplete<Response<T>>() {
            public void completed(Response<T> value) {
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

    static final class Asset {
        String title;
    }

    static final class Unmapped {
    }

    static final class AssetMapper implements Mapper<Asset> {
        public Class<Asset> type() {
            return Asset.class;
        }

        public Map<String, Object> toMap(Asset instance) {
            return null;
        }

        public Asset fromMap(Map<String, Object> map) {
            Asset a = new Asset();
            Object t = map.get("title");
            a.title = t == null ? null : t.toString();
            return a;
        }

        public String xmlRootName() {
            return "asset";
        }

        public void writeXml(Asset instance, Element root) {
        }

        public Asset readXml(Element root) {
            return new Asset();
        }
    }
}
