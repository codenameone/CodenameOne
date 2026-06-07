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

import com.codename1.io.JSONParser;
import com.codename1.io.Log;
import com.codename1.io.WebSocket;
import com.codename1.ui.CN;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Handle for a live GraphQL subscription. Implements the
/// `graphql-transport-ws` message protocol over
/// [com.codename1.io.WebSocket] (introduced in core alongside this
/// package): `connection_init` -> `connection_ack` -> `subscribe`,
/// then a stream of `next` payloads terminated by `complete` or
/// `error`.
///
/// Each `next` payload's `data` object is mapped to `T` and delivered
/// to [Handler#onNext(Object)]. All handler callbacks are dispatched on
/// the Codename One EDT (matching the `OnComplete` semantics the query
/// and mutation paths use), even though the underlying WebSocket fires
/// on a background thread.
///
/// The connection offers the `graphql-transport-ws` subprotocol during
/// the WebSocket handshake (RFC 6455 `Sec-WebSocket-Protocol`), as the
/// graphql-ws specification requires.
public final class GraphQLSubscription {

    /// The graphql-ws (graphql-transport-ws) WebSocket subprotocol name.
    static final String SUBPROTOCOL = "graphql-transport-ws";

    /// Receives the events of one subscription. All methods run on the
    /// Codename One EDT.
    public interface Handler<T> {
        /// A `next` payload arrived and its `data` mapped to `T`. When
        /// the payload also carried `errors`, `response.hasErrors()` is
        /// true and `response.getData()` may still be non-null.
        void onNext(GraphQLResponse<T> response);

        /// The subscription failed (an `error` message, a transport
        /// error, or a non-clean close). No further events follow.
        void onError(GraphQLResponse<T> response);

        /// The subscription ended cleanly (server `complete`, or a
        /// caller [#cancel()]). No further events follow.
        void onComplete();
    }

    private static final String OPERATION_ID = "1";

    private final Class<?> dataType;
    private final String operationName;
    private final String document;
    private final Map<String, Object> variables;
    private final String bearerToken;
    private final Handler<?> handler;

    private WebSocket socket;
    private boolean terminated;

    private GraphQLSubscription(Class<?> dataType, String operationName, String document,
                                Map<String, Object> variables, String bearerToken,
                                Handler<?> handler) {
        this.dataType = dataType;
        this.operationName = operationName;
        this.document = document;
        this.variables = variables;
        this.bearerToken = bearerToken;
        this.handler = handler;
    }

    static <T> GraphQLSubscription start(String wsUrl, String bearerToken, String operationName,
                                         String document, Map<String, Object> variables,
                                         Class<T> dataType, Handler<T> handler) {
        if (handler == null) {
            throw new IllegalArgumentException("handler must not be null");
        }
        final GraphQLSubscription sub = new GraphQLSubscription(
                dataType, operationName, document, variables, bearerToken, handler);
        WebSocket ws = WebSocket.build(wsUrl)
                .subprotocols(SUBPROTOCOL)
                .onConnect(new WebSocket.ConnectHandler() {
                    @Override
                    public void onConnect(WebSocket w) {
                        sub.sendConnectionInit(w);
                    }
                })
                .onTextMessage(new WebSocket.TextHandler() {
                    @Override
                    public void onText(WebSocket w, String message) {
                        sub.onMessage(w, message);
                    }
                })
                .onClose(new WebSocket.CloseHandler() {
                    @Override
                    public void onClose(WebSocket w, int statusCode, String reason) {
                        // 1000 (normal) and 1005 (no status) are clean.
                        if (statusCode == 1000 || statusCode == 1005) {
                            sub.finishComplete();
                        } else {
                            sub.finishError(transportError(
                                    "WebSocket closed: " + statusCode
                                            + (reason == null || reason.length() == 0 ? "" : " " + reason)));
                        }
                    }
                })
                .onError(new WebSocket.ErrorHandler() {
                    @Override
                    public void onError(WebSocket w, Exception ex) {
                        sub.finishError(transportError(
                                ex == null ? "WebSocket error" : ex.getMessage()));
                    }
                });
        sub.socket = ws;
        ws.connect();
        return sub;
    }

    /// Cancels the subscription: sends a `complete` to the server and
    /// closes the socket. Idempotent; safe to call from any thread.
    public void cancel() {
        WebSocket w;
        synchronized (this) {
            if (terminated) {
                return;
            }
            terminated = true;
            w = socket;
        }
        if (w != null) {
            try {
                w.send(message("complete", OPERATION_ID, null));
            } catch (Throwable ignored) {
                // Socket may already be closing; closing below covers it.
            }
            try {
                w.close();
            } catch (Throwable ignored) {
                Log.e(ignored);
            }
        }
    }

    // ----------------------------------------------------------------
    // Protocol handling (runs on the WebSocket background thread)
    // ----------------------------------------------------------------

    private void sendConnectionInit(WebSocket w) {
        Map<String, Object> init = new LinkedHashMap<String, Object>();
        init.put("type", "connection_init");
        if (bearerToken != null && bearerToken.length() > 0) {
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("Authorization", bearerToken);
            init.put("payload", JSONParser.rawJson(JSONParser.toJson(payload)));
        }
        safeSend(w, JSONParser.toJson(init));
    }

    @SuppressWarnings("unchecked")
    private void onMessage(WebSocket w, String text) {
        Map<String, Object> msg;
        try {
            msg = JSONParser.parseJSON(text);
        } catch (Exception e) {
            // Malformed frame -- log and drop it rather than tear the
            // subscription down over one bad message.
            Log.e(e);
            return;
        }
        if (msg == null) {
            return;
        }
        String type = str(msg.get("type"));
        if ("connection_ack".equals(type)) {
            safeSend(w, subscribeMessage());
        } else if ("next".equals(type)) {
            deliverNext(msg.get("payload"));
        } else if ("error".equals(type)) {
            List<GraphQLError> errors = GraphQL.parseErrors(asErrorsArray(msg.get("payload")));
            finishError(new GraphQLResponse<Object>(0, null, errors,
                    errors.isEmpty() ? "Subscription error" : errors.get(0).getMessage()));
        } else if ("complete".equals(type)) {
            finishComplete();
            WebSocket s = socket;
            if (s != null) {
                try {
                    s.close();
                } catch (Throwable ignored) {
                    Log.e(ignored);
                }
            }
        } else if ("ping".equals(type)) {
            safeSend(w, "{\"type\":\"pong\"}");
        }
        // connection_error / unknown types are ignored.
    }

    @SuppressWarnings("unchecked")
    private void deliverNext(Object payload) {
        synchronized (this) {
            if (terminated) {
                return;
            }
        }
        Object dataObj = null;
        Object errorsObj = null;
        if (payload instanceof Map) {
            Map<String, Object> p = (Map<String, Object>) payload;
            dataObj = p.get("data");
            errorsObj = p.get("errors");
        }
        Object data = GraphQL.mapData(dataObj, dataType);
        List<GraphQLError> errors = GraphQL.parseErrors(errorsObj);
        String message = errors.isEmpty() ? null : errors.get(0).getMessage();
        final GraphQLResponse<Object> response =
                new GraphQLResponse<Object>(200, data, errors, message);
        dispatch(new Runnable() {
            @Override
            public void run() {
                ((Handler<Object>) handler).onNext(response);
            }
        });
    }

    private void finishComplete() {
        synchronized (this) {
            if (terminated) {
                return;
            }
            terminated = true;
        }
        dispatch(new Runnable() {
            @Override
            public void run() {
                handler.onComplete();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void finishError(final GraphQLResponse<Object> response) {
        synchronized (this) {
            if (terminated) {
                return;
            }
            terminated = true;
        }
        dispatch(new Runnable() {
            @Override
            public void run() {
                ((Handler<Object>) handler).onError(response);
            }
        });
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private String subscribeMessage() {
        return message("subscribe", OPERATION_ID,
                GraphQL.buildRequestBody(operationName, document, variables));
    }

    /// Builds a `graphql-transport-ws` envelope. When `payloadJson` is
    /// non-null it is spliced in as the raw `payload` object.
    private static String message(String type, String id, String payloadJson) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", id);
        m.put("type", type);
        if (payloadJson != null) {
            m.put("payload", JSONParser.rawJson(payloadJson));
        }
        return JSONParser.toJson(m);
    }

    private static void safeSend(WebSocket w, String text) {
        try {
            w.send(text);
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    private static void dispatch(Runnable r) {
        CN.callSerially(r);
    }

    private static GraphQLResponse<Object> transportError(String message) {
        return new GraphQLResponse<Object>(0, null,
                Collections.<GraphQLError>emptyList(), message);
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    /// The `error` message `payload` is an array of error objects;
    /// wrap it so [GraphQL#parseErrors(Object)] (which expects the
    /// value of an `errors` key) can consume it.
    private static Object asErrorsArray(Object payload) {
        if (payload instanceof List) {
            return payload;
        }
        if (payload instanceof Map) {
            List<Object> one = new ArrayList<Object>(1);
            one.add(payload);
            return one;
        }
        return null;
    }
}
