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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// One request for [MockMvc], built by [MockMvcRequestBuilders].
public class MockRequestBuilder {
    final String method;
    private final String path;
    private final List headerNames = new ArrayList();
    private final Map headerValues = new LinkedHashMap();
    private final List params = new ArrayList();
    private final List cookies = new ArrayList();
    private final List postProcessors = new ArrayList();
    /// What post-processors asked to run just before and just after the request
    /// is sent, in the order they asked.
    private List before;
    private List after;
    byte[] content;
    String contentType;

    MockRequestBuilder(String method, String uriTemplate, Object[] uriVariables) {
        this.method = method;
        this.path = expand(uriTemplate, uriVariables);
    }

    /// Adds a header; several values are sent as one comma-separated field.
    public MockRequestBuilder header(String name, Object... values) {
        String key = lower(name);
        List existing = (List) headerValues.get(key);
        if (existing == null) {
            existing = new ArrayList();
            headerValues.put(key, existing);
            headerNames.add(name);
        }
        for (Object value : values) {
            existing.add(String.valueOf(value));
        }
        return this;
    }

    /// Adds a request parameter: a query parameter, which `@RequestParam` reads.
    public MockRequestBuilder param(String name, String... values) {
        return addQuery(name, values);
    }

    /// Adds a query parameter: the same as [#param], except on a multipart
    /// builder, whose param() adds a form field instead and whose queryParam()
    /// still puts the value in the URL.
    public MockRequestBuilder queryParam(String name, String... values) {
        return addQuery(name, values);
    }

    private MockRequestBuilder addQuery(String name, String... values) {
        for (String value : values) {
            params.add(new String[] {name, value});
        }
        return this;
    }

    /// The body, as UTF-8 text.
    public MockRequestBuilder content(String body) {
        this.content = body == null ? null : utf8(body);
        return this;
    }

    /// The body, as bytes.
    public MockRequestBuilder content(byte[] body) {
        this.content = body == null ? null : body.clone();
        return this;
    }

    /// The Content-Type header.
    public MockRequestBuilder contentType(String type) {
        this.contentType = type;
        return this;
    }

    /// The Accept header.
    public MockRequestBuilder accept(String... types) {
        StringBuilder sb = new StringBuilder();
        for (int iter = 0 ; iter < types.length ; iter++) {
            if (iter > 0) {
                sb.append(", ");
            }
            sb.append(types[iter]);
        }
        headerValues.remove("accept");
        return header("Accept", sb.toString());
    }

    /// A cookie, sent in the Cookie header.
    public MockRequestBuilder cookie(String name, String value) {
        cookies.add(name + "=" + value);
        return this;
    }

    /// Applies `postProcessor` to the request when it is sent: who it is from, a
    /// CSRF token, credentials. See [SecurityMockMvcRequestPostProcessors].
    public MockRequestBuilder with(RequestPostProcessor postProcessor) {
        if (postProcessor == null) {
            throw new IllegalArgumentException("postProcessor cannot be null");
        }
        postProcessors.add(postProcessor);
        return this;
    }

    /// Runs `first` just before this request is sent and `last` just after,
    /// whether or not sending it threw: for a post-processor that changes
    /// something outside the request for as long as the request takes.
    void around(Runnable first, Runnable last) {
        before.add(first);
        after.add(last);
    }

    /// Sets a header, in place of any value it had.
    void replaceHeader(String name, String value) {
        String key = lower(name);
        if (headerValues.remove(key) != null) {
            for (int iter = headerNames.size() - 1 ; iter >= 0 ; iter--) {
                if (key.equals(lower((String) headerNames.get(iter)))) {
                    headerNames.remove(iter);
                }
            }
        }
        header(name, value);
    }

    /// Sets a request parameter, in place of any value it had.
    void replaceParam(String name, String value) {
        for (int iter = params.size() - 1 ; iter >= 0 ; iter--) {
            if (name.equals(((String[]) params.get(iter))[0])) {
                params.remove(iter);
            }
        }
        param(name, value);
    }

    /// Applies the post-processors and returns the builder to send, with what
    /// they asked to run around the request collected on it.
    MockRequestBuilder prepare() {
        MockRequestBuilder prepared = this;
        prepared.before = new ArrayList();
        prepared.after = new ArrayList();
        for (Object each : postProcessors) {
            MockRequestBuilder next = ((RequestPostProcessor) each).postProcessRequest(prepared);
            if (next != null && next != prepared) { //NOPMD CompareObjectsWithEquals - the builder itself
                if (next.before == null) {
                    next.before = new ArrayList();
                    next.after = new ArrayList();
                }
                next.before.addAll(0, prepared.before);
                next.after.addAll(0, prepared.after);
                prepared = next;
            }
        }
        return prepared;
    }

    /// Runs what [#around] collected to run first.
    void runBefore() {
        for (Object each : before) {
            ((Runnable) each).run();
        }
    }

    /// Runs what [#around] collected to run last, the latest asked first.
    void runAfter() {
        for (int iter = after.size() - 1 ; iter >= 0 ; iter--) {
            ((Runnable) after.get(iter)).run();
        }
    }

    /// Accepted for familiarity; bodies are always UTF-8 here.
    public MockRequestBuilder characterEncoding(String encoding) {
        return this;
    }

    /// The request as MockMvc sends it.
    Built build() {
        Built out = new Built();
        out.method = method;
        StringBuilder target = new StringBuilder(path);
        for (Object entry : params) {
            String[] p = (String[]) entry;
            target.append(target.toString().indexOf('?') < 0 ? '?' : '&').append(encode(p[0])).append('=')
                    .append(encode(p[1]));
        }
        out.target = target.toString();
        out.headers = new LinkedHashMap();
        for (Object entry : headerNames) {
            String name = (String) entry;
            List values = (List) headerValues.get(lower(name));
            if (values == null) {
                continue;
            }
            StringBuilder joined = new StringBuilder();
            for (int v = 0 ; v < values.size() ; v++) {
                if (v > 0) {
                    joined.append(", ");
                }
                joined.append(values.get(v));
            }
            out.headers.put(name, joined.toString());
        }
        if (!cookies.isEmpty()) {
            StringBuilder joined = new StringBuilder();
            for (int iter = 0 ; iter < cookies.size() ; iter++) {
                if (iter > 0) {
                    joined.append("; ");
                }
                joined.append(cookies.get(iter));
            }
            out.headers.put("Cookie", joined.toString());
        }
        byte[] body = body();
        String type = bodyType();
        if (type != null) {
            out.headers.put("Content-Type", type);
        }
        out.body = body;
        if (body != null) {
            out.headers.put("Content-Length", String.valueOf(body.length));
        }
        // By name in any case: a test's .header("host", ...) is the Host the
        // request carries, and the dispatch folds names, so a default added
        // beside it under another spelling replaced it.
        if (!containsName(out.headers, "Host")) {
            out.headers.put("Host", "localhost");
        }
        return out;
    }

    private static boolean containsName(Map headers, String name) {
        for (Object key : headers.keySet()) {
            if (name.equalsIgnoreCase(String.valueOf(key))) {
                return true;
            }
        }
        return false;
    }

    /// The body to send; a multipart builder assembles its own.
    byte[] body() {
        return content;
    }

    /// The Content-Type to send; a multipart builder names its boundary.
    String bodyType() {
        return contentType;
    }

    /// A built request: what [MockMvc] hands the server.
    static final class Built {
        String method;
        String target;
        Map headers;
        byte[] body;
    }

    /// `/pets/{id}` with its variables substituted in order, each percent-encoded
    /// as a path segment. A placeholder with no variable left for it throws, rather
    /// than sending the literal `{id}` for a route to bind.
    static String expand(String template, Object[] variables) {
        if (template.indexOf('{') < 0) {
            return template;
        }
        if (variables == null) {
            variables = new Object[0];
        }
        StringBuilder sb = new StringBuilder();
        int next = 0;
        int pos = 0;
        while (pos < template.length()) {
            int open = template.indexOf('{', pos);
            if (open < 0) {
                break;
            }
            int close = template.indexOf('}', open);
            if (close < 0) {
                break;
            }
            sb.append(template, pos, open);
            if (next >= variables.length) {
                throw new IllegalArgumentException("Not enough variables for " + template);
            }
            sb.append(encode(String.valueOf(variables[next++])));
            pos = close + 1;
        }
        sb.append(template.substring(pos));
        return sb.toString();
    }

    /// Percent-encodes everything but the unreserved characters of RFC 3986.
    static String encode(String value) {
        byte[] bytes = utf8(value);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            int c = b & 0xff;
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '.' || c == '_' || c == '~') {
                sb.append((char) c);
            } else {
                sb.append('%').append(HEX.charAt(c >> 4)).append(HEX.charAt(c & 15));
            }
        }
        return sb.toString();
    }

    private static final String HEX = "0123456789ABCDEF";

    static byte[] utf8(String value) {
        try {
            return value.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException err) {
            throw new IllegalStateException(err.toString(), err);
        }
    }

    static String lower(String value) {
        char[] chars = value.toCharArray();
        for (int iter = 0 ; iter < chars.length ; iter++) {
            if (chars[iter] >= 'A' && chars[iter] <= 'Z') {
                chars[iter] = (char) (chars[iter] + 32);
            }
        }
        return new String(chars);
    }
}
