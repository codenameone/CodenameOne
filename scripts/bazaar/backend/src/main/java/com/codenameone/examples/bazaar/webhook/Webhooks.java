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
package com.codenameone.examples.bazaar.webhook;

import com.codename1.backend.Config;
import com.codename1.backend.Crypto;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.Web;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.WebhookDeliveryDto;
import com.codenameone.examples.bazaar.api.WebhookDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.domain.Webhook;
import com.codenameone.examples.bazaar.domain.WebhookDelivery;
import com.codenameone.examples.bazaar.repository.WebhookDeliveryRepository;
import com.codenameone.examples.bazaar.repository.WebhookRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/// A store's webhooks and their deliveries.
///
/// [#emit] only queues: a delivery row per active webhook of the topic, in
/// the caller's transaction or its own. The scheduled [#deliverDue] sends
/// what is due as a JSON POST signed with the webhook's secret:
/// `X-Bazaar-Signature: sha256=<hex HMAC-SHA256 of the body>`. A delivery
/// not answered 2xx is tried again after [#backoffMillis] -- 1, 2, 4, 8 and
/// 16 minutes -- and is FAILED after [#MAX_ATTEMPTS] tries.
///
/// The URL is the merchant's to choose, so it must be http or https and
/// [com.codenameone.examples.bazaar.web.security.UrlGuard#refuse] turns away
/// private, loopback and link-local hosts
/// before anything is called.
@Component
public class Webhooks {
    public static final String PENDING = "PENDING";
    public static final String DELIVERED = "DELIVERED";
    public static final String FAILED = "FAILED";
    public static final int MAX_ATTEMPTS = 6;
    public static final String SIGNATURE_HEADER = "X-Bazaar-Signature";

    public static final String ORDERS_CREATE = "orders/create";
    public static final String ORDERS_PAID = "orders/paid";
    public static final String ORDERS_FULFILLED = "orders/fulfilled";
    public static final String ORDERS_CANCELLED = "orders/cancelled";
    public static final String REFUNDS_CREATE = "refunds/create";
    public static final String INVENTORY_LOW = "inventory/low";
    public static final String PRODUCTS_UPDATE = "products/update";
    public static final String CUSTOMERS_CREATE = "customers/create";

    /// Every topic a webhook may subscribe to.
    public static final List<String> TOPICS = Collections.unmodifiableList(Arrays.asList(
            ORDERS_CREATE, ORDERS_PAID, ORDERS_FULFILLED, ORDERS_CANCELLED, REFUNDS_CREATE,
            INVENTORY_LOW, PRODUCTS_UPDATE, CUSTOMERS_CREATE));

    private static final int BATCH = 50;

    /// How a delivery reaches its URL; answers the HTTP status, or throws
    /// when nothing answered.
    public interface Transport {
        int post(String url, List<String> headers, byte[] body) throws IOException;
    }

    /// Records each delivery instead of sending it and answers the status it
    /// is told to: the test profile's transport, so no test calls out.
    public static final class RecordingTransport implements Transport {
        private final List<String[]> sent = new ArrayList<String[]>();
        private int status = 200;

        @Override
        public synchronized int post(String url, List<String> headers, byte[] body)
                throws IOException {
            StringBuilder h = new StringBuilder();
            for (int iter = 0; iter < headers.size(); iter++) {
                h.append(headers.get(iter)).append('\n');
            }
            sent.add(new String[] {url, h.toString(), new String(body, "UTF-8")});
            return status;
        }

        /// The status every later delivery is answered with.
        public synchronized void answer(int status) {
            this.status = status;
        }

        /// `{url, headers (one per line), body}` of every delivery so far.
        public synchronized List<String[]> sent() {
            return new ArrayList<String[]>(sent);
        }
    }

    /// Sends over HTTP.
    static final class HttpTransport implements Transport {
        @Override
        public int post(String url, List<String> headers, byte[] body) throws IOException {
            // Checked again at delivery, against what the name resolves to now.
            String unsafe = com.codenameone.examples.bazaar.web.security.UrlGuard.refuse(url,
                    com.codenameone.examples.bazaar.web.security.UrlGuard.DNS);
            if (unsafe != null) {
                throw new IOException(unsafe);
            }
            List copy = new ArrayList(headers);
            Web.Result answer = Web.request("POST", url, copy, body);
            return answer.getStatus();
        }
    }

    private final WebhookRepository hooks;
    private final WebhookDeliveryRepository deliveries;
    private final StoreAccess access;
    private Transport transport;

    public Webhooks(WebhookRepository hooks, WebhookDeliveryRepository deliveries,
            StoreAccess access, Config config) {
        this.hooks = hooks;
        this.deliveries = deliveries;
        this.access = access;
        this.transport = "test".equals(config.getProfile()) ? new RecordingTransport()
                : new HttpTransport();
    }

    /// The transport deliveries go through.
    public Transport transport() {
        return transport;
    }

    /// Replaces the transport; for tests.
    public void useTransport(Transport transport) {
        this.transport = transport;
    }

    /// How long after failed attempt number `attempts` (1 based) the next one
    /// is made: a minute, doubling each time.
    public static long backoffMillis(int attempts) {
        int n = Math.max(1, Math.min(attempts, 20));
        return 60000L << (n - 1);
    }

    /// `sha256=` and the hex HMAC-SHA256 of `body` keyed with `secret`.
    // tag::bazaar-webhook-signature[]
    public static String signature(String secret, byte[] body) throws IOException {
        return "sha256=" + Ids.hex(Crypto.hmacSha256(secret.getBytes("UTF-8"), body));
    }
    // end::bazaar-webhook-signature[]

    /// Whether `header` is the signature of `body` under `secret`, compared
    /// in constant time: what a receiver does.
    public static boolean verify(String secret, byte[] body, String header) throws IOException {
        if (header == null) {
            return false;
        }
        return Crypto.equalsConstantTime(signature(secret, body).getBytes("UTF-8"),
                header.trim().getBytes("UTF-8"));
    }

    // -------------------------------------------------------------- queueing

    /// Queues a delivery of `payloadJson` to every active webhook of the store
    /// subscribed to `topic`. Answers how many were queued.
    @Transactional(rollbackFor = IOException.class)
    public int emit(String storeId, String topic, String payloadJson) throws IOException {
        List<Webhook> mine = hooks.findByStoreIdAndTopic(storeId, topic);
        int queued = 0;
        for (int iter = 0; iter < mine.size(); iter++) {
            Webhook hook = mine.get(iter);
            if (hook.isActive()) {
                queue(hook, topic, payloadJson);
                queued++;
            }
        }
        return queued;
    }

    /// Queues one delivery to one webhook whatever its topic: what the
    /// automation action `webhook.send` does.
    @Transactional(rollbackFor = IOException.class)
    public WebhookDelivery queueTo(String storeId, String webhookId, String topic,
            String payloadJson) throws IOException {
        Webhook hook = webhookId == null ? null : hooks.findById(webhookId);
        if (hook == null || !hook.getStoreId().equals(storeId) || !hook.isActive()) {
            return null;
        }
        return queue(hook, topic, payloadJson);
    }

    private WebhookDelivery queue(Webhook hook, String topic, String payloadJson)
            throws IOException {
        WebhookDelivery d = new WebhookDelivery();
        d.setId(Ids.next());
        d.setWebhookId(hook.getId());
        d.setTopic(Codes.cut(topic, 64));
        d.setPayload(payloadJson == null ? "{}" : payloadJson);
        d.setStatus(PENDING);
        d.setNextAttemptAt(Instant.now());
        d.setCreatedAt(Instant.now());
        deliveries.insert(d);
        return d;
    }

    // ------------------------------------------------------------ delivering

    /// Sends every delivery that is due.
    @Scheduled(fixedDelay = 15000, initialDelay = 20000, lock = "bazaar-webhooks")
    public void deliver() throws IOException {
        deliverDue(System.currentTimeMillis());
    }

    /// [#deliver] as of `now`; answers how many were attempted. Public so
    /// that tests can run the clock forward.
    public int deliverDue(long now) throws IOException {
        List<WebhookDelivery> due = dueRead(now);
        int tried = 0;
        for (int iter = 0; iter < due.size() && tried < BATCH; iter++) {
            attempt(due.get(iter).getId(), now);
            tried++;
        }
        return tried;
    }

    @Transactional(readOnly = true)
    private List<WebhookDelivery> dueRead(long now) {
        List<WebhookDelivery> pending = deliveries.findByStatusOrderByNextAttemptAtAsc(PENDING);
        List<WebhookDelivery> due = new ArrayList<WebhookDelivery>();
        for (int iter = 0; iter < pending.size(); iter++) {
            if (pending.get(iter).getNextAttemptAt().toEpochMilli() <= now) {
                due.add(pending.get(iter));
            }
        }
        return due;
    }

    /// One attempt at a delivery, recorded whatever happens. The call is made
    /// with no transaction open.
    private WebhookDelivery attempt(String deliveryId, long now) throws IOException {
        WebhookDelivery d = deliveryRead(deliveryId);
        Webhook hook = d == null ? null : hookRead(d.getWebhookId());
        if (d == null) {
            return null;
        }
        int code;
        if (hook == null) {
            code = 0;
        } else {
            byte[] body = d.getPayload().getBytes("UTF-8");
            List<String> headers = new ArrayList<String>();
            headers.add("Content-Type: application/json");
            headers.add("X-Bazaar-Topic: " + d.getTopic());
            headers.add("X-Bazaar-Delivery: " + d.getId());
            headers.add(SIGNATURE_HEADER + ": " + signature(hook.getSecret(), body));
            try {
                code = transport.post(hook.getUrl(), headers, body);
            } catch (IOException | RuntimeException unreachable) {
                code = 0;
            }
        }
        return recordAttempt(deliveryId, code, now, hook == null);
    }

    @Transactional(readOnly = true)
    private WebhookDelivery deliveryRead(String id) {
        return deliveries.findById(id);
    }

    @Transactional(readOnly = true)
    private Webhook hookRead(String id) {
        return hooks.findById(id);
    }

    @Transactional(rollbackFor = IOException.class)
    private WebhookDelivery recordAttempt(String deliveryId, int code, long now, boolean gone)
            throws IOException {
        WebhookDelivery d = deliveries.findById(deliveryId);
        d.setAttempts(d.getAttempts() + 1);
        d.setResponseCode(code);
        if (code >= 200 && code < 300) {
            d.setStatus(DELIVERED);
        } else if (gone || d.getAttempts() >= MAX_ATTEMPTS) {
            d.setStatus(FAILED);
        } else {
            d.setStatus(PENDING);
            d.setNextAttemptAt(Instant.ofEpochMilli(now + backoffMillis(d.getAttempts())));
        }
        deliveries.save(d);
        return d;
    }

    // -------------------------------------------------------------- merchant

    /// The topics a hook may follow. The list is the same for every store, but
    /// it is answered only to the store's staff: like every other route under
    /// a store, a store one does not work at answers 404 (MerchantIdorTest).
    public List<String> topics(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        return TOPICS;
    }

    @Transactional(readOnly = true)
    public List<WebhookDto> list(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        List<Webhook> all = hooks.findByStoreId(storeId);
        List<WebhookDto> out = new ArrayList<WebhookDto>();
        for (int iter = 0; iter < all.size(); iter++) {
            out.add(dto(all.get(iter)));
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public WebhookDto create(String username, String storeId, WebhookDto wanted)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Webhook hook = new Webhook();
        hook.setId(Ids.next());
        hook.setStoreId(storeId);
        hook.setCreatedAt(Instant.now());
        apply(hook, wanted);
        hooks.insert(hook);
        return dto(hook);
    }

    @Transactional(rollbackFor = IOException.class)
    public WebhookDto save(String username, String storeId, String webhookId, WebhookDto wanted)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Webhook hook = mine(storeId, webhookId);
        apply(hook, wanted);
        hooks.save(hook);
        return dto(hook);
    }

    @Transactional(rollbackFor = IOException.class)
    public List<WebhookDto> remove(String username, String storeId, String webhookId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        hooks.delete(mine(storeId, webhookId));
        return list(username, storeId);
    }

    @Transactional(readOnly = true)
    public List<WebhookDeliveryDto> deliveries(String username, String storeId, String webhookId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        mine(storeId, webhookId);
        List<WebhookDelivery> all = deliveries.findByWebhookIdOrderByCreatedAtDesc(webhookId);
        List<WebhookDeliveryDto> out = new ArrayList<WebhookDeliveryDto>();
        for (int iter = 0; iter < all.size() && iter < 100; iter++) {
            out.add(dto(all.get(iter)));
        }
        return out;
    }

    /// Queues a sample payload to one webhook and sends it at once.
    public WebhookDeliveryDto test(String username, String storeId, String webhookId)
            throws IOException {
        WebhookDelivery d = queueTest(username, storeId, webhookId);
        return dto(attempt(d.getId(), System.currentTimeMillis()));
    }

    @Transactional(rollbackFor = IOException.class)
    private WebhookDelivery queueTest(String username, String storeId, String webhookId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Webhook hook = mine(storeId, webhookId);
        return queue(hook, hook.getTopic(), "{\"test\":true,\"topic\":\"" + hook.getTopic()
                + "\",\"storeId\":\"" + storeId + "\",\"sentAt\":" + System.currentTimeMillis()
                + "}");
    }

    /// Sends a delivery again now: a failed one gets a fresh set of attempts.
    public WebhookDeliveryDto redeliver(String username, String storeId, String deliveryId)
            throws IOException {
        resetForRedelivery(username, storeId, deliveryId);
        return dto(attempt(deliveryId, System.currentTimeMillis()));
    }

    @Transactional(rollbackFor = IOException.class)
    private void resetForRedelivery(String username, String storeId, String deliveryId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        WebhookDelivery d = deliveryId == null ? null : deliveries.findById(deliveryId);
        if (d == null) {
            throw new ResponseStatusException(404, "No such delivery");
        }
        mine(storeId, d.getWebhookId());
        d.setStatus(PENDING);
        d.setAttempts(0);
        d.setNextAttemptAt(Instant.now());
        deliveries.save(d);
    }

    private Webhook mine(String storeId, String webhookId) {
        Webhook hook = webhookId == null ? null : hooks.findById(webhookId);
        if (hook == null || !hook.getStoreId().equals(storeId)) {
            throw new ResponseStatusException(404, "No such webhook");
        }
        return hook;
    }

    private static void apply(Webhook hook, WebhookDto wanted) throws IOException {
        if (wanted == null) {
            throw new ResponseStatusException(400, "Nothing to save");
        }
        String topic = wanted.topic() == null ? "" : wanted.topic().trim();
        if (!TOPICS.contains(topic)) {
            throw new ResponseStatusException(400, "The topic is one of " + TOPICS);
        }
        String url = wanted.url() == null ? "" : wanted.url().trim();
        if (!(url.regionMatches(true, 0, "https://", 0, 8)
                || url.regionMatches(true, 0, "http://", 0, 7)) || url.length() > 500
                || url.indexOf(' ') >= 0) {
            throw new ResponseStatusException(400, "The URL is an http or https address");
        }
        String unsafe = com.codenameone.examples.bazaar.web.security.UrlGuard.refuseByName(url);
        if (unsafe != null) {
            throw new ResponseStatusException(400, unsafe);
        }
        hook.setTopic(topic);
        hook.setUrl(url);
        String secret = wanted.secret() == null ? "" : wanted.secret().trim();
        if (secret.length() > 0) {
            if (secret.length() < 16 || secret.length() > 64) {
                throw new ResponseStatusException(400, "A secret is 16 to 64 characters");
            }
            hook.setSecret(secret);
        } else if (hook.getSecret().length() == 0) {
            hook.setSecret(Ids.hex(Crypto.randomBytes(24)));
        }
        hook.setActive(wanted.active());
    }

    static WebhookDto dto(Webhook h) {
        return new WebhookDto(h.getId(), h.getTopic(), h.getUrl(), h.getSecret(), h.isActive(),
                h.getCreatedAt().toEpochMilli());
    }

    static WebhookDeliveryDto dto(WebhookDelivery d) {
        return d == null ? null : new WebhookDeliveryDto(d.getId(), d.getWebhookId(), d.getTopic(),
                d.getPayload(), d.getStatus(), d.getAttempts(), d.getResponseCode(),
                d.getNextAttemptAt().toEpochMilli(), d.getCreatedAt().toEpochMilli());
    }
}
