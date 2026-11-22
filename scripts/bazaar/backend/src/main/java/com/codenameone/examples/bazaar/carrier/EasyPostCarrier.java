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

import com.codename1.backend.Base64;
import com.codename1.backend.Crypto;
import com.codename1.backend.Json;
import com.codename1.backend.Web;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.RateQuoteDto;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// A real carrier account through EasyPost's v2 API, which fronts USPS, UPS,
/// FedEx, DHL and others with one interface.
///
/// - **Rates**: `POST /v2/shipments` with both addresses and the weight; each
///   rate comes back as a decimal string, parsed to cents by hand ([#cents]).
/// - **Label**: the shipment is created again and `POST
///   /v2/shipments/{id}/buy` buys the rate whose `carrier service` matches;
///   the answer carries the tracking code and the printable label's URL.
/// - **Tracking**: pushed by EasyPost's `tracker.updated` webhook
///   ([CarrierWebhook], signed with HMAC-SHA256 over the raw body,
///   [#verify]), and polled through `GET /v2/trackers` as a backstop -- at
///   most once per parcel every `pollMillis`, because the scheduler runs
///   every 20 seconds and the API is metered.
///
/// Configured by `bazaar.carrier.easypost.key` (the API key, Basic auth as
/// the user name), `bazaar.carrier.easypost.webhook-secret`, and the origin
/// `bazaar.carrier.origin.{country,postal,city,region}` used when a pickup
/// location has no postal address of its own. Never commit a key.
///
/// The client is held to canned answers through [Transport] only; it has not
/// been run against the live API in CI.
public class EasyPostCarrier implements CarrierProvider {
    public static final String NAME = "easypost";
    /// The API root.
    public static final String API = "https://api.easypost.com/v2";
    /// The header EasyPost signs webhooks in.
    public static final String SIGNATURE_HEADER = "X-Hmac-Signature";

    private final Transport transport;
    private final String webhookSecret;
    private final Place origin;
    private final long pollMillis;
    private final Map<String, Long> polled = new HashMap<String, Long>();

    public EasyPostCarrier(Transport transport, String webhookSecret, Place origin,
            long pollMillis) {
        this.transport = transport;
        this.webhookSecret = webhookSecret == null ? "" : webhookSecret;
        this.origin = origin;
        this.pollMillis = pollMillis;
    }

    /// The carrier over the network with `key`.
    public static EasyPostCarrier live(String key, String webhookSecret, Place origin,
            long pollMillis) {
        return new EasyPostCarrier(new WebTransport(key), webhookSecret, origin, pollMillis);
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String originCountry() {
        return origin == null ? "" : origin.country;
    }

    @Override
    public List<RateQuoteDto> rates(Parcel parcel, Place from, Place to) throws IOException {
        Map shipment = shipment(parcel, from, to);
        List<RateQuoteDto> out = new ArrayList<RateQuoteDto>();
        Object rates = shipment.get("rates");
        if (!(rates instanceof List)) {
            return out;
        }
        List list = (List) rates;
        for (int iter = 0; iter < list.size(); iter++) {
            Object o = list.get(iter);
            if (!(o instanceof Map)) {
                continue;
            }
            Map r = (Map) o;
            String carrier = text(r, "carrier");
            String service = text(r, "service");
            long c = cents(text(r, "rate"));
            if (carrier.length() == 0 || service.length() == 0 || c < 0) {
                continue;
            }
            Object days = r.get("delivery_days");
            out.add(new RateQuoteDto(NAME, carrier + " " + service, carrier + " " + service, c,
                    days instanceof Number ? ((Number) days).intValue() : 0));
        }
        return out;
    }

    @Override
    public Label buyLabel(Parcel parcel, Place from, Place to, String service,
            String reference) throws IOException {
        Map shipment = shipment(parcel, from, to);
        String id = text(shipment, "id");
        String rateId = null;
        Object rates = shipment.get("rates");
        if (rates instanceof List) {
            List list = (List) rates;
            for (int iter = 0; iter < list.size() && rateId == null; iter++) {
                Object o = list.get(iter);
                if (o instanceof Map && service.equals(text((Map) o, "carrier") + " "
                        + text((Map) o, "service"))) {
                    rateId = text((Map) o, "id");
                }
            }
        }
        if (id.length() == 0 || rateId == null) {
            throw new IOException("EasyPost offers no " + service + " for this parcel");
        }
        Map rate = new LinkedHashMap();
        rate.put("id", rateId);
        Map body = new LinkedHashMap();
        body.put("rate", rate);
        Map bought = call("POST", "/shipments/" + id + "/buy", Json.write(body));
        String tracking = text(bought, "tracking_code");
        Object label = bought.get("postage_label");
        String url = label instanceof Map ? text((Map) label, "label_url") : "";
        Object selected = bought.get("selected_rate");
        long c = selected instanceof Map ? cents(text((Map) selected, "rate")) : -1;
        if (tracking.length() == 0) {
            throw new IOException("EasyPost bought no label");
        }
        return new Label(tracking, service, Math.max(0, c), url);
    }

    @Override
    public List<Scan> track(String trackingNumber, long shippedAt, long now, Place from,
            Place to) throws IOException {
        // Only the tracking job calls this, under its scheduler lock, so the map
        // is never touched by two threads.
        Long last = polled.get(trackingNumber);
        if (last != null && now - last.longValue() < pollMillis) {
            return new ArrayList<Scan>();
        }
        polled.put(trackingNumber, Long.valueOf(now));
        Map answer = call("GET", "/trackers?tracking_code=" + com.codenameone.examples.bazaar.web.SiteUrls.encode(trackingNumber), null);
        Object trackers = answer.get("trackers");
        if (!(trackers instanceof List) || ((List) trackers).isEmpty()
                || !(((List) trackers).get(0) instanceof Map)) {
            return new ArrayList<Scan>();
        }
        return scans((Map) ((List) trackers).get(0));
    }

    /// Whether `signature` (the [#SIGNATURE_HEADER] value,
    /// `hmac-sha256-hex=<hex>`) is the HMAC-SHA256 of `body` under the webhook
    /// secret. No secret configured refuses everything.
    public boolean verify(byte[] body, String signature) throws IOException {
        if (webhookSecret.length() == 0 || signature == null || body == null) {
            return false;
        }
        String expected = "hmac-sha256-hex=" + Ids.hex(Crypto.hmacSha256(
                webhookSecret.getBytes("UTF-8"), body));
        return Crypto.equalsConstantTime(expected.getBytes("UTF-8"),
                signature.trim().getBytes("UTF-8"));
    }

    /// The tracking code and scans of a `tracker.updated` event, or null for
    /// any other event.
    public static Object[] event(String json) throws IOException {
        Map e = Json.parseObject(json);
        if (!"tracker.updated".equals(text(e, "description")) || !(e.get("result")
                instanceof Map)) {
            return null;
        }
        Map tracker = (Map) e.get("result");
        return new Object[] {text(tracker, "tracking_code"), scans(tracker)};
    }

    static List<Scan> scans(Map tracker) {
        List<Scan> out = new ArrayList<Scan>();
        Object details = tracker.get("tracking_details");
        if (!(details instanceof List)) {
            return out;
        }
        List list = (List) details;
        for (int iter = 0; iter < list.size(); iter++) {
            Object o = list.get(iter);
            if (!(o instanceof Map)) {
                continue;
            }
            Map d = (Map) o;
            String status = status(text(d, "status"));
            if (status == null) {
                continue;
            }
            String place = "";
            Object loc = d.get("tracking_location");
            if (loc instanceof Map) {
                place = text((Map) loc, "city");
            }
            long at;
            try {
                at = Instant.parse(text(d, "datetime")).toEpochMilli();
            } catch (RuntimeException bad) {
                at = 0;
            }
            out.add(new Scan(status, text(d, "message"), place, 0, 0, at));
        }
        return out;
    }

    /// EasyPost's tracking status as the store's, or null for one it keeps
    /// to itself (pre_transit, unknown, failure, ...).
    static String status(String s) {
        if ("in_transit".equals(s)) {
            return "IN_TRANSIT";
        }
        if ("out_for_delivery".equals(s)) {
            return "OUT_FOR_DELIVERY";
        }
        return "delivered".equals(s) ? "DELIVERED" : null;
    }

    /// A decimal amount (`"7.58"`, `"12"`, `"0.5"`) in cents, or -1 for
    /// anything else. No floating point: money stays integer end to end.
    public static long cents(String s) {
        if (s == null) {
            return -1;
        }
        String t = s.trim();
        long whole = 0;
        long frac = 0;
        int fracDigits = -1;
        int digits = 0;
        for (int iter = 0; iter < t.length(); iter++) {
            char c = t.charAt(iter);
            if (c == '.' && fracDigits < 0) {
                fracDigits = 0;
            } else if (c >= '0' && c <= '9') {
                if (fracDigits < 0) {
                    if (++digits > 12) {
                        return -1;
                    }
                    whole = whole * 10 + (c - '0');
                } else if (fracDigits < 2) {
                    frac = frac * 10 + (c - '0');
                    fracDigits++;
                } else {
                    return -1;
                }
            } else {
                return -1;
            }
        }
        if (digits == 0 && fracDigits <= 0) {
            return -1;
        }
        if (fracDigits == 1) {
            frac *= 10;
        }
        return whole * 100 + frac;
    }

    private Map shipment(Parcel parcel, Place from, Place to) throws IOException {
        Map p = new LinkedHashMap();
        // EasyPost weighs in ounces; round up so a label is never underpaid.
        p.put("weight", Long.valueOf((Math.max(1, parcel.weightGrams) * 1000L + 28349) / 28350));
        Map s = new LinkedHashMap();
        Place sender = from.postal.length() > 0 ? from : merge(from, origin);
        s.put("from_address", address(sender));
        s.put("to_address", address(to));
        s.put("parcel", p);
        // Decided on the address actually sent: a location's Place carries no
        // country of its own and takes the configured origin's.
        if (!parcel.items.isEmpty() && CarrierProvider.crossesBorder(sender.country, to.country)) {
            s.put("customs_info", customs(parcel.items, sender.name));
        }
        Map body = new LinkedHashMap();
        body.put("shipment", s);
        return call("POST", "/shipments", Json.write(body));
    }

    /// EasyPost's customs_info: one customs_item per line, each with its total
    /// value in the currency's major unit and total weight in ounces.
    static Map customs(List<CustomsItem> items, String signer) {
        List<Object> list = new ArrayList<Object>();
        for (int iter = 0; iter < items.size(); iter++) {
            CustomsItem it = items.get(iter);
            Map m = new LinkedHashMap();
            m.put("description", it.description.length() > 0 ? it.description : "Merchandise");
            m.put("quantity", Integer.valueOf(Math.max(1, it.quantity)));
            m.put("value", decimal(it.valueCents));
            m.put("weight", Long.valueOf((Math.max(1, it.weightGrams) * 1000L + 28349) / 28350));
            if (it.hsCode.length() > 0) {
                m.put("hs_tariff_number", it.hsCode);
            }
            if (it.originCountry.length() > 0) {
                m.put("origin_country", it.originCountry);
            }
            if (it.currency.length() > 0) {
                m.put("currency", it.currency);
            }
            if (it.sku.length() > 0) {
                m.put("code", it.sku);
            }
            list.add(m);
        }
        Map c = new LinkedHashMap();
        c.put("contents_type", "merchandise");
        c.put("customs_certify", Boolean.TRUE);
        c.put("customs_signer", signer == null || signer.length() == 0 ? "Shipping" : signer);
        c.put("non_delivery_option", "return");
        c.put("restriction_type", "none");
        c.put("eel_pfc", "NOEEI 30.37(a)");
        c.put("customs_items", list);
        return c;
    }

    /// Cents as a plain decimal: 1250 is "12.50".
    public static String decimal(long cents) {
        long c = Math.max(0, cents);
        long frac = c % 100;
        return (c / 100) + "." + (frac < 10 ? "0" : "") + frac;
    }

    private static Place merge(Place p, Place fallback) {
        if (fallback == null) {
            return p;
        }
        return new Place(p.name, p.address.length() > 0 ? p.address : fallback.address,
                fallback.city.length() > 0 ? fallback.city : p.city, p.lat, p.lng,
                fallback.postal, fallback.region, fallback.country);
    }

    private static Map address(Place p) {
        Map a = new LinkedHashMap();
        a.put("name", p.name);
        a.put("street1", p.address);
        a.put("city", p.city);
        a.put("state", p.region);
        a.put("zip", p.postal);
        a.put("country", p.country);
        return a;
    }

    private Map call(String method, String path, String json) throws IOException {
        Reply r = transport.send(method, path, json);
        if (r.status < 200 || r.status >= 300) {
            throw new IOException("EasyPost answered " + r.status);
        }
        try {
            return Json.parseObject(r.body == null ? "" : r.body);
        } catch (IOException | RuntimeException unreadable) {
            throw new IOException("EasyPost answered something that is not JSON");
        }
    }

    private static String text(Map m, String key) {
        Object v = m.get(key);
        return v instanceof String ? (String) v : "";
    }

    /// The network: one request, one answer.
    public interface Transport {
        /// @param path from the API root, e.g. `/shipments`
        /// @param json the body, or null
        Reply send(String method, String path, String json) throws IOException;
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
        private final String key;

        WebTransport(String key) {
            this.key = key;
        }

        @Override
        public Reply send(String method, String path, String json) throws IOException {
            List headers = new ArrayList();
            headers.add("Authorization: Basic " + Base64.encode((key + ":").getBytes("UTF-8")));
            if (json != null) {
                headers.add("Content-Type: application/json");
            }
            Web.Result r = Web.request(method, API + path, headers,
                    json == null ? null : json.getBytes("UTF-8"));
            return new Reply(r.getStatus(), r.getBodyAsString());
        }
    }
}
