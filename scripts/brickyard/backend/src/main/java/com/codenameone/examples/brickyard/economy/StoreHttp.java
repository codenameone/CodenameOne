/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.brickyard.economy;

import com.codename1.backend.Web;
import com.codename1.backend.WebClient;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// The one HTTP call a store validator makes, so the validators can be
/// tested against canned answers without a network. [#web()] is the
/// backend runtime's own client.
public interface StoreHttp {
    /// What a store answered: the status and the body as text.
    final class Reply {
        private final int status;
        private final String body;

        public Reply(int status, String body) {
            this.status = status;
            this.body = body == null ? "" : body;
        }

        public int status() {
            return status;
        }

        public String body() {
            return body;
        }
    }

    Reply send(String method, String url, List<String> headers, String body) throws IOException;

    /// Calls through [WebClient], ten seconds a request.
    static StoreHttp web() {
        final WebClient client = new WebClient().setRequestTimeoutMillis(10000);
        return (method, url, headers, body) -> {
            List<Object> lines = new ArrayList<Object>();
            if (headers != null) {
                lines.addAll(headers);
            }
            Web.Result r = client.request(method, url, lines, body == null ? null : body.getBytes("UTF-8"));
            if (r.getError() != null && r.getStatus() <= 0) {
                throw new IOException("The store could not be reached: " + r.getError());
            }
            return new Reply(r.getStatus(), r.getBodyAsString());
        };
    }
}
