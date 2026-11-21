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
package com.codenameone.examples.bazaar.channel;

import com.codename1.backend.Web;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// The network, as a connector uses it: one request, one answer. The real one
/// is [Live]; tests hand a connector their own.
public interface ChannelHttp {
    /// @param headers `Name: value` lines
    /// @param body the request body, or null for none
    Reply send(String method, String url, List<String> headers, String body) throws IOException;

    /// A status and a body, whatever the status.
    final class Reply {
        public final int status;
        public final String body;
        public final String etag;

        public Reply(int status, String body, String etag) {
            this.status = status;
            this.body = body == null ? "" : body;
            this.etag = etag == null ? "" : etag;
        }
    }

    /// HTTPS through the backend's [Web] client.
    final class Live implements ChannelHttp {
        @Override
        public Reply send(String method, String url, List<String> headers, String body)
                throws IOException {
            List copy = new ArrayList(headers);
            Web.Result r = Web.request(method, url, copy, body == null ? null
                    : body.getBytes("UTF-8"));
            return new Reply(r.getStatus(), r.getBodyAsString(), r.getHeader("ETag"));
        }
    }
}
