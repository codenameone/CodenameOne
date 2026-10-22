/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.plugins.dio;

import java.util.List;
import java.util.Map;

import com.codename1.io.Util;

import dart.core.DartMap;
import dart.core.DartUri;
import dart.core.Duration;

/// dio's `RequestOptions`: one request, fully resolved -- what interceptors see
/// and may change before it is sent.
public class RequestOptions extends OptionsBase {
    String path = "";
    String baseUrl = "";
    DartMap<String, Object> queryParameters;
    Object data;
    Duration connectTimeout;
    CancelToken cancelToken;

    public String path() {
        return path;
    }

    public void path(String v) {
        path = v == null ? "" : v;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public void baseUrl(String v) {
        baseUrl = v == null ? "" : v;
    }

    public DartMap<String, Object> queryParameters() {
        if (queryParameters == null) {
            queryParameters = new DartMap<String, Object>();
        }
        return queryParameters;
    }

    public void queryParameters(Map<String, Object> v) {
        queryParameters = v == null ? null : copy(v);
    }

    public Object data() {
        return data;
    }

    public void data(Object v) {
        data = v;
    }

    public Duration connectTimeout() {
        return connectTimeout;
    }

    public void connectTimeout(Duration v) {
        connectTimeout = v;
    }

    public CancelToken cancelToken() {
        return cancelToken;
    }

    /// The URL that will be requested: the path, against `baseUrl` unless it is
    /// absolute, with the query parameters appended (a list value repeats its key).
    public String url() {
        String url = path;
        if (!(path.startsWith("http://") || path.startsWith("https://"))) {
            String base = baseUrl == null ? "" : baseUrl;
            if (base.endsWith("/") && path.startsWith("/")) {
                url = base + path.substring(1);
            } else if (!base.isEmpty() && !base.endsWith("/") && !path.isEmpty() && !path.startsWith("/")) {
                url = base + "/" + path;
            } else {
                url = base + path;
            }
        }
        StringBuilder q = new StringBuilder();
        if (queryParameters != null) {
            for (Map.Entry<String, Object> e : queryParameters.entrySet()) {
                Object v = e.getValue();
                if (v instanceof List) {
                    for (Object o : (List<?>) v) {
                        appendParam(q, e.getKey(), o);
                    }
                } else if (v != null) {
                    appendParam(q, e.getKey(), v);
                }
            }
        }
        if (q.length() == 0) {
            return url;
        }
        return url + (url.indexOf('?') >= 0 ? "&" : "?") + q;
    }

    private static void appendParam(StringBuilder q, String key, Object value) {
        if (q.length() > 0) {
            q.append('&');
        }
        q.append(Util.encodeUrl(key)).append('=').append(Util.encodeUrl(dart.runtime.DartRuntime.str(value)));
    }

    public DartUri uri() {
        return DartUri.parse(url());
    }
}
