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
package com.codenameone.examples.bazaar.carrier;

import com.codename1.backend.HttpServer;
import com.codename1.backend.annotations.Controller;
import com.codename1.backend.annotations.PostMapping;
import com.codenameone.examples.bazaar.fulfil.Fulfillments;
import java.util.List;

/// EasyPost's tracking events, at [#PATH]: signed with the webhook secret
/// ([EasyPostCarrier#verify]), not signed in. A bad signature is 401; an
/// event of another kind, or for a parcel the store does not know, is
/// acknowledged with 200 so EasyPost stops retrying it. Scans are recorded
/// the way polling records them ([Fulfillments#trackingUpdate]).
@Controller
public class CarrierWebhook {
    public static final String PATH = "/api/carrier/webhooks/easypost";

    private final CarrierProvider carrier;
    private final Fulfillments fulfillments;

    public CarrierWebhook(CarrierProvider carrier, Fulfillments fulfillments) {
        this.carrier = carrier;
        this.fulfillments = fulfillments;
    }

    @PostMapping(PATH)
    @SuppressWarnings("unchecked")
    public HttpServer.Response receive(HttpServer.Request request) throws Exception {
        if (!(carrier instanceof EasyPostCarrier)) {
            return HttpServer.Response.text(404, "no carrier webhook is configured");
        }
        byte[] body = request.getBodyBytes();
        if (!((EasyPostCarrier) carrier).verify(body,
                request.getHeader(EasyPostCarrier.SIGNATURE_HEADER))) {
            return HttpServer.Response.text(401, "bad signature");
        }
        Object[] event;
        try {
            event = EasyPostCarrier.event(new String(body, "UTF-8"));
        } catch (java.io.IOException | RuntimeException unreadable) {
            return HttpServer.Response.text(400, "not an event");
        }
        if (event != null && event[1] instanceof List) {
            fulfillments.trackingUpdate((String) event[0],
                    (List<CarrierProvider.Scan>) event[1]);
        }
        return HttpServer.Response.text(200, "ok");
    }
}
