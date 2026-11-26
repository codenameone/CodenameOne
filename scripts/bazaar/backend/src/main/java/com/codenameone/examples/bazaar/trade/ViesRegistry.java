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
package com.codenameone.examples.bazaar.trade;

import com.codename1.backend.Json;
import com.codename1.backend.Web;
import java.io.IOException;
import java.util.Map;

/// The European Commission's VIES service, over its REST interface:
/// `GET <base>/ms/{country}/vat/{number}`, answered with JSON whose `isValid`
/// says whether the number is issued and `name` who holds it.
///
/// VIES relays each question to the member state's own service, and those are
/// often down. It reports that with status 200 and a `userError` such as
/// `MS_UNAVAILABLE`, `TIMEOUT` or `SERVICE_UNAVAILABLE` -- so an answer counts
/// only when `userError` is absent, `VALID` or `INVALID`; anything else, and
/// any other status, is an outage ([IOException]), which [VatVerification]
/// turns into "format valid, unverified" and never caches.
///
/// The client is held to canned answers through [Transport] only; it has not
/// been run against the live service in CI.
public class ViesRegistry implements VatRegistry {
    /// The service's REST root.
    public static final String BASE =
            "https://ec.europa.eu/taxation_customs/vies/rest-api";

    private final Transport transport;

    public ViesRegistry(String base) {
        this(new WebTransport(base));
    }

    public ViesRegistry(Transport transport) {
        this.transport = transport;
    }

    @Override
    public Answer lookup(String country, String number) throws IOException {
        Reply reply = transport.get("/ms/" + country + "/vat/" + number);
        if (reply.status != 200) {
            throw new IOException("VIES answered " + reply.status);
        }
        Map body;
        try {
            body = Json.parseObject(reply.body == null ? "" : reply.body);
        } catch (IOException | RuntimeException unreadable) {
            throw new IOException("VIES answered something that is not JSON");
        }
        Object error = body.get("userError");
        if (error instanceof String && !"VALID".equals(error) && !"INVALID".equals(error)) {
            throw new IOException("VIES could not answer: " + error);
        }
        Object valid = body.get("isValid");
        if (!(valid instanceof Boolean)) {
            throw new IOException("VIES answered without a verdict");
        }
        String name = text(body, "name");
        // VIES writes "---" for a name a member state does not disclose.
        if ("---".equals(name)) {
            name = "";
        }
        return new Answer(((Boolean) valid).booleanValue(), name,
                text(body, "requestIdentifier"));
    }

    private static String text(Map body, String key) {
        Object v = body.get(key);
        return v instanceof String ? (String) v : "";
    }

    /// The network: one GET, one answer.
    public interface Transport {
        /// @param path from the REST root, e.g. `/ms/DE/vat/123456789`
        Reply get(String path) throws IOException;
    }

    /// An answer: its status and body.
    public static final class Reply {
        public final int status;
        public final String body;

        public Reply(int status, String body) {
            this.status = status;
            this.body = body;
        }
    }

    static final class WebTransport implements Transport {
        private final String base;

        WebTransport(String base) {
            this.base = base;
        }

        @Override
        public Reply get(String path) throws IOException {
            Web.Result r = Web.get(base + path);
            return new Reply(r.getStatus(), r.getBodyAsString());
        }
    }
}
