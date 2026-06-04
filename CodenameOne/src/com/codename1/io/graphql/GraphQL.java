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
package com.codename1.io.graphql;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.Data;
import com.codename1.io.JSONParser;
import com.codename1.mapping.Mapper;
import com.codename1.mapping.Mappers;
import com.codename1.ui.CN;
import com.codename1.util.OnComplete;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// High-level GraphQL-over-HTTP invoker used by generated
/// `@GraphQLClient` implementations. Queries and mutations are sent as
/// an HTTP `POST` with a JSON body
/// `{"query":...,"operationName":...,"variables":{...}}` and a
/// `Content-Type: application/json` header; the JSON response envelope
/// (`{"data":...,"errors":[...]}`) is parsed into a typed
/// [GraphQLResponse]. Subscriptions are delegated to
/// [GraphQLSubscription] over a WebSocket.
///
/// Mirrors [com.codename1.io.grpc.GrpcWeb]. All methods are static;
/// generated impls call [#execute] / [#subscribe] and never touch
/// `ConnectionRequest` directly.
public final class GraphQL {

    /// Content type for GraphQL-over-HTTP requests.
    public static final String CONTENT_TYPE = "application/json";

    private GraphQL() {
    }

    /// Sends a unary query or mutation and invokes `callback` with the
    /// decoded [GraphQLResponse]. `variables` may be null or empty.
    /// The `operationName` may be null when the document declares a
    /// single operation.
    public static <T> void execute(
            String endpoint,
            String bearerToken,
            String operationName,
            String document,
            Map<String, Object> variables,
            Class<T> dataType,
            final OnComplete<GraphQLResponse<T>> callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        String body = buildRequestBody(operationName, document, variables);

        GraphQLConnection<T> conn = new GraphQLConnection<T>(dataType, callback);
        conn.setUrl(endpoint);
        conn.setHttpMethod("POST");
        conn.setPost(true);
        conn.setContentType(CONTENT_TYPE);
        conn.addRequestHeader("Accept", CONTENT_TYPE);
        if (bearerToken != null && bearerToken.length() > 0) {
            conn.addRequestHeader("Authorization", bearerToken);
        }
        conn.setRequestBody(new Data.StringData(body)); // StringData defaults to UTF-8
        CN.addToQueue(conn);
    }

    /// Opens a GraphQL subscription over a WebSocket and streams mapped
    /// `data` payloads to `handler`. Returns a handle whose
    /// [GraphQLSubscription#cancel()] tears the subscription down.
    ///
    /// The `endpoint` is the GraphQL HTTP endpoint; its scheme is
    /// rewritten to `ws`/`wss` for the WebSocket connection. Pass a
    /// `ws://`/`wss://` URL directly to override that heuristic.
    public static <T> GraphQLSubscription subscribe(
            String endpoint,
            String bearerToken,
            String operationName,
            String document,
            Map<String, Object> variables,
            Class<T> dataType,
            GraphQLSubscription.Handler<T> handler) {
        return GraphQLSubscription.start(
                toWebSocketUrl(endpoint), bearerToken, operationName,
                document, variables, dataType, handler);
    }

    /// Builds the JSON request body. Public so tests can verify it
    /// without the network path.
    public static String buildRequestBody(String operationName, String document,
                                          Map<String, Object> variables) {
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        root.put("query", document);
        if (operationName != null && operationName.length() > 0) {
            root.put("operationName", operationName);
        }
        if (variables != null && !variables.isEmpty()) {
            root.put("variables", JSONParser.rawJson(encodeVariables(variables)));
        }
        return JSONParser.toJson(root);
    }

    /// Serialises a variable-name -> value map to a JSON object string.
    /// Strings, numbers, booleans, `List`s, `Map`s and null pass
    /// through the framework JSON writer; enums serialise as their
    /// `name()`; any other object is assumed to be `@Mapped` and is
    /// converted via [Mappers#toJson(Object)] and spliced in verbatim.
    /// Public so generated impls and tests can reuse it.
    public static String encodeVariables(Map<String, Object> variables) {
        return JSONParser.toJson(toJsonTree(variables));
    }

    @SuppressWarnings("unchecked")
    private static Object toJsonTree(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String || value instanceof Number || value instanceof Boolean
                || value instanceof JSONParser.RawJson) {
            return value;
        }
        if (value instanceof Enum) {
            return ((Enum<?>) value).name();
        }
        if (value instanceof Map) {
            Map<Object, Object> src = (Map<Object, Object>) value;
            Map<String, Object> out = new LinkedHashMap<String, Object>();
            for (Map.Entry<Object, Object> e : src.entrySet()) {
                out.put(String.valueOf(e.getKey()), toJsonTree(e.getValue()));
            }
            return out;
        }
        if (value instanceof List) {
            List<Object> src = (List<Object>) value;
            List<Object> out = new ArrayList<Object>(src.size());
            for (Object element : src) {
                out.add(toJsonTree(element));
            }
            return out;
        }
        if (value instanceof Object[]) {
            Object[] src = (Object[]) value;
            List<Object> out = new ArrayList<Object>(src.length);
            for (Object element : src) {
                out.add(toJsonTree(element));
            }
            return out;
        }
        // Assume an @Mapped business object: serialise via the mapper
        // registry and splice the fragment in unescaped.
        return JSONParser.rawJson(Mappers.toJson(value));
    }

    /// Decodes a GraphQL JSON response envelope into a typed
    /// [GraphQLResponse]. Public and side-effect free so unit tests can
    /// replay canned bodies without a network round-trip.
    public static <T> GraphQLResponse<T> decodeJson(byte[] body, int httpCode, Class<T> dataType) {
        if (body == null || body.length == 0) {
            return new GraphQLResponse<T>(httpCode, null,
                    Collections.<GraphQLError>emptyList(), "Empty response body");
        }
        Map<String, Object> root;
        try {
            root = JSONParser.parseJSON(body);
        } catch (IOException ioe) {
            return new GraphQLResponse<T>(httpCode, null,
                    Collections.<GraphQLError>emptyList(),
                    "Failed to parse GraphQL response: " + ioe.getMessage());
        }
        if (root == null) {
            return new GraphQLResponse<T>(httpCode, null,
                    Collections.<GraphQLError>emptyList(), "Empty response body");
        }
        List<GraphQLError> errors = parseErrors(root.get("errors"));
        T data = mapData(root.get("data"), dataType);
        String message = errors.isEmpty() ? null : errors.get(0).getMessage();
        return new GraphQLResponse<T>(httpCode, data, errors, message);
    }

    @SuppressWarnings("unchecked")
    static <T> T mapData(Object dataObj, Class<T> dataType) {
        if (!(dataObj instanceof Map) || dataType == null) {
            return null;
        }
        Mapper<T> mapper = Mappers.get(dataType);
        if (mapper == null) {
            return null;
        }
        return mapper.fromMap((Map<String, Object>) dataObj);
    }

    @SuppressWarnings("unchecked")
    static List<GraphQLError> parseErrors(Object errorsObj) {
        if (!(errorsObj instanceof List)) {
            return Collections.emptyList();
        }
        List<Object> raw = (List<Object>) errorsObj;
        List<GraphQLError> out = new ArrayList<GraphQLError>(raw.size());
        for (Object o : raw) {
            if (!(o instanceof Map)) {
                continue;
            }
            Map<String, Object> m = (Map<String, Object>) o;
            Object msg = m.get("message");
            out.add(new GraphQLError(
                    msg == null ? "" : String.valueOf(msg),
                    (List<Object>) asList(m.get("path")),
                    (List<Map<String, Object>>) asList(m.get("locations")),
                    (Map<String, Object>) asMap(m.get("extensions"))));
        }
        return out;
    }

    private static List<?> asList(Object o) {
        return o instanceof List ? (List<?>) o : null;
    }

    private static Map<?, ?> asMap(Object o) {
        return o instanceof Map ? (Map<?, ?>) o : null;
    }

    /// Rewrites an `http`/`https` endpoint to `ws`/`wss`. URLs that
    /// already use a WebSocket scheme are returned unchanged.
    static String toWebSocketUrl(String endpoint) {
        if (endpoint == null) {
            return null;
        }
        if (endpoint.startsWith("https://")) {
            return "wss://" + endpoint.substring("https://".length());
        }
        if (endpoint.startsWith("http://")) {
            return "ws://" + endpoint.substring("http://".length());
        }
        return endpoint;
    }

    // ----------------------------------------------------------------
    // HTTP transport
    // ----------------------------------------------------------------

    /// Subclasses `ConnectionRequest` to suppress the framework's modal
    /// error dialog and surface every outcome through the user's
    /// callback instead. Mirrors `GrpcWeb.GrpcConnection`.
    private static final class GraphQLConnection<T> extends ConnectionRequest {
        private final Class<T> dataType;
        private final OnComplete<GraphQLResponse<T>> callback;
        private boolean failed;
        private int failedCode;
        private String failedMessage;

        GraphQLConnection(Class<T> dataType, OnComplete<GraphQLResponse<T>> callback) {
            this.dataType = dataType;
            this.callback = callback;
            setFailSilently(true);
            setReadResponseForErrors(true);
        }

        @Override
        protected void handleErrorResponseCode(int code, String message) {
            failed = true;
            failedCode = code;
            failedMessage = message;
            // Swallow the framework default; reported via the callback
            // in postResponse.
        }

        @Override
        protected void handleException(Exception err) {
            failed = true;
            failedCode = 0;
            failedMessage = err.getMessage();
        }

        @Override
        protected void postResponse() {
            super.postResponse();
            if (failed) {
                callback.completed(new GraphQLResponse<T>(failedCode, null,
                        Collections.<GraphQLError>emptyList(), failedMessage));
                return;
            }
            callback.completed(decodeJson(getResponseData(), getResponseCode(), dataType));
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }
    }
}
