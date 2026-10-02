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
package com.codename1.backend.test;

import com.codename1.backend.Json;
import com.codename1.backend.Web;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/// An HTTP client for a [BackendTest] served on a port -- `webEnvironment =
/// RANDOM_PORT` -- through the backend's own outbound client, so a compiled test
/// sends real requests from the native binary too.
///
/// A path is relative to the server; an absolute URL is sent as it is. A 4xx or
/// 5xx is a response to assert on, not an exception. Bodies convert to and from
/// `String`, `byte[]`, and `Map` or `List` as JSON.
///
/// The Java SE client behind `cn1:backend` cannot send PATCH (its HTTP client has
/// a fixed set of verbs); use [MockMvc] for a PATCH route, or a compiled run.
public final class TestRestTemplate {
    private final String rootUri;
    private final String authorization;

    /// A client for the server at `rootUri`, such as `http://127.0.0.1:8080`.
    public TestRestTemplate(String rootUri) {
        this(rootUri, null);
    }

    private TestRestTemplate(String rootUri, String authorization) {
        String root = rootUri;
        while (root.endsWith("/")) {
            root = root.substring(0, root.length() - 1);
        }
        this.rootUri = root;
        this.authorization = authorization;
    }

    public String getRootUri() {
        return rootUri;
    }

    /// This client, sending HTTP Basic credentials with every request.
    public TestRestTemplate withBasicAuth(String username, String password) {
        return new TestRestTemplate(rootUri, "Basic " + com.codename1.backend.Base64.encode(
                MockRequestBuilder.utf8(username + ":" + password)));
    }

    public <T> ResponseEntity<T> getForEntity(String url, Class<T> responseType,
                                              Object... uriVariables) throws IOException {
        return exchange(url, HttpMethod.GET, null, responseType, uriVariables);
    }

    public <T> T getForObject(String url, Class<T> responseType, Object... uriVariables)
            throws IOException {
        return getForEntity(url, responseType, uriVariables).getBody();
    }

    public <T> ResponseEntity<T> postForEntity(String url, Object request, Class<T> responseType,
                                               Object... uriVariables) throws IOException {
        return exchange(url, HttpMethod.POST, entity(request), responseType, uriVariables);
    }

    public <T> T postForObject(String url, Object request, Class<T> responseType,
                               Object... uriVariables) throws IOException {
        return postForEntity(url, request, responseType, uriVariables).getBody();
    }

    public void put(String url, Object request, Object... uriVariables) throws IOException {
        exchange(url, HttpMethod.PUT, entity(request), Void.class, uriVariables);
    }

    public void delete(String url, Object... uriVariables) throws IOException {
        exchange(url, HttpMethod.DELETE, null, Void.class, uriVariables);
    }

    /// Sends any method with any body and headers.
    public <T> ResponseEntity<T> exchange(String url, HttpMethod method, HttpEntity<?> requestEntity,
                                          Class<T> responseType, Object... uriVariables)
            throws IOException {
        String target = MockRequestBuilder.expand(url, uriVariables);
        if (!target.startsWith("http://") && !target.startsWith("https://")) {
            target = rootUri + (target.startsWith("/") ? "" : "/") + target;
        }
        HttpHeaders headers = requestEntity == null ? new HttpHeaders() : requestEntity.getHeaders();
        byte[] body = null;
        if (requestEntity != null && requestEntity.hasBody()) {
            Object value = requestEntity.getBody();
            String type = headers.getContentType();
            if (value instanceof byte[]) {
                body = (byte[]) value;
                type = type == null ? MediaType.APPLICATION_OCTET_STREAM : type;
            } else if (value instanceof String) {
                body = MockRequestBuilder.utf8((String) value);
                type = type == null ? "text/plain; charset=utf-8" : type;
            } else if (value instanceof Map || value instanceof List) {
                body = MockRequestBuilder.utf8(Json.write(value));
                type = type == null ? MediaType.APPLICATION_JSON : type;
            } else {
                throw new IllegalArgumentException("A request body is a String, a byte[], a Map or "
                        + "a List; " + value.getClass().getName() + " has no conversion here");
            }
            if (headers.getContentType() == null) {
                headers = copy(headers);
                headers.setContentType(type);
            }
        }
        List lines = headers.lines();
        if (authorization != null && !headers.containsKey("Authorization")) {
            lines = new ArrayList(lines);
            lines.add("Authorization: " + authorization);
        }
        Web.Result result = Web.request(method.name(), target, lines, body);
        if (result.getStatus() < 0) {
            throw new IOException("The request to " + target + " failed: " + result.getError());
        }
        HttpHeaders responseHeaders = new HttpHeaders();
        Iterator it = result.getHeaders().keySet().iterator();
        while (it.hasNext()) {
            String name = String.valueOf(it.next());
            List values = result.getHeaderValues(name);
            for (int iter = 0 ; values != null && iter < values.size() ; iter++) {
                responseHeaders.add(name, String.valueOf(values.get(iter)));
            }
        }
        return new ResponseEntity<T>(convert(result.getBody(), responseType), responseHeaders,
                result.getStatus());
    }

    private static HttpEntity<?> entity(Object request) {
        if (request == null) {
            return null;
        }
        if (request instanceof HttpEntity) {
            return (HttpEntity<?>) request;
        }
        return new HttpEntity<Object>(request);
    }

    private static HttpHeaders copy(HttpHeaders headers) {
        HttpHeaders out = new HttpHeaders();
        List names = headers.names();
        for (Object entry : names) {
            String name = (String) entry;
            List values = headers.get(name);
            for (int v = 0 ; values != null && v < values.size() ; v++) {
                out.add(name, (String) values.get(v));
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static <T> T convert(byte[] body, Class<T> type) throws IOException {
        if (type == null || type == Void.class) {
            return null;
        }
        if (type == byte[].class) {
            return (T) (body == null ? new byte[0] : body);
        }
        String text = body == null ? "" : new String(body, "UTF-8");
        if (type == String.class) {
            return (T) text;
        }
        if (type == Map.class || type == List.class || type == Object.class) {
            Object parsed = text.trim().length() == 0 ? null : Json.parse(text);
            if (parsed != null && !type.isInstance(parsed)) {
                throw new IOException("The response is JSON but not a " + type.getName() + ": "
                        + text);
            }
            return (T) parsed;
        }
        throw new IllegalArgumentException("A response converts to String, byte[], Map, List or "
                + "Void; " + type.getName() + " has no conversion here");
    }
}
