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
/// `X-Bazaar-Timestamp: <epoch seconds>` and
/// `X-Bazaar-Signature: sha256=<hex HMAC-SHA256 of timestamp + "." + body>`.
/// A receiver refuses a timestamp more than [#MAX_AGE_SECONDS] away from its
/// own clock, so a delivery it saw once cannot be sent to it again later
/// ([#verify]). Each attempt is signed afresh. A delivery
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
    /// When the request was signed, in epoch seconds; part of the signature.
    public static final String TIMESTAMP_HEADER = "X-Bazaar-Timestamp";
    /// How far a signed request's timestamp may be from the receiver's clock.
    public static final long MAX_AGE_SECONDS = 300;

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

    /// Sent to an app when a store removes it; its token is already revoked.
    public static final String APP_UNINSTALLED = "app/uninstalled";
    /// Sent to every installed app when a customer asks for their data.
    public static final String CUSTOMERS_DATA_REQUEST = "customers/data_request";
    /// Sent to every installed app when a customer's personal data is erased.
    public static final String CUSTOMERS_REDACT = "customers/redact";
    /// Sent to an app 48 hours after a store removed it: delete the store's data.
    public static final String SHOP_REDACT = "shop/redact";
    /// Sent to an app when one of its charges changes status (`AppBilling`).
    public static final String APP_SUBSCRIPTIONS_UPDATE = "app_subscriptions/update";
    /// The topics every installed app is subscribed to whether it asks or not:
    /// no app and no member of staff can subscribe to or remove them.
    public static final List<String> MANDATORY = Collections.unmodifiableList(Arrays.asList(
            APP_UNINSTALLED, CUSTOMERS_DATA_REQUEST, CUSTOMERS_REDACT, SHOP_REDACT,
            APP_SUBSCRIPTIONS_UPDATE));

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

    /// How long one delivery may take, all of it.
    static final int DELIVERY_TIMEOUT_MILLIS = 10000;
    /// The most of a receiver's answer that is read; only its status counts.
    static final int MAX_ANSWER_BYTES = 64 * 1024;

    /// Sends over HTTP.
    static final class HttpTransport implements Transport {
        @Override
        public int post(String url, List<String> headers, byte[] body) throws IOException {
            // Checked again at delivery, against what the name resolves to
            // now; timed, capped and never redirected (a 3xx is a failed
            // delivery): see OutboundHttp.
            return com.codenameone.examples.bazaar.web.security.OutboundHttp.send("POST", url,
                    headers, body, DELIVERY_TIMEOUT_MILLIS, MAX_ANSWER_BYTES).status;
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

    /// `sha256=` and the hex HMAC-SHA256 of `timestamp`, a `.` and `body`,
    /// keyed with `secret`.
    // tag::bazaar-webhook-signature[]
    public static String signature(String secret, long timestamp, byte[] body)
            throws IOException {
        byte[] prefix = (timestamp + ".").getBytes("UTF-8");
        byte[] message = new byte[prefix.length + body.length];
        System.arraycopy(prefix, 0, message, 0, prefix.length);
        System.arraycopy(body, 0, message, prefix.length, body.length);
        return "sha256=" + Ids.hex(Crypto.hmacSha256(secret.getBytes("UTF-8"), message));
    }
    // end::bazaar-webhook-signature[]

    /// Adds the timestamp and signature headers of a request whose body is
    /// `body`, signed now under `secret`.
    public static void sign(List<String> headers, String secret, byte[] body)
            throws IOException {
        long now = System.currentTimeMillis() / 1000L;
        headers.add(TIMESTAMP_HEADER + ": " + now);
        headers.add(SIGNATURE_HEADER + ": " + signature(secret, now, body));
    }

    /// Whether `signatureHeader` signs `body` with `timestampHeader` under
    /// `secret`, compared in constant time, and the timestamp is within
    /// [#MAX_AGE_SECONDS] of `nowSeconds`: what a receiver does.
    public static boolean verify(String secret, byte[] body, String signatureHeader,
            String timestampHeader, long nowSeconds) throws IOException {
        if (signatureHeader == null || timestampHeader == null || body == null) {
            return false;
        }
        String ts = timestampHeader.trim();
        if (ts.length() == 0 || ts.length() > 12) {
            return false;
        }
        long at = 0;
        for (int iter = 0; iter < ts.length(); iter++) {
            char c = ts.charAt(iter);
            if (c < '0' || c > '9') {
                return false;
            }
            at = at * 10 + (c - '0');
        }
        if (Math.abs(nowSeconds - at) > MAX_AGE_SECONDS) {
            return false;
        }
        return Crypto.equalsConstantTime(signature(secret, at, body).getBytes("UTF-8"),
                signatureHeader.trim().getBytes("UTF-8"));
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
        if (hook == null || !hook.getStoreId().equals(storeId) || !hook.isActive()
                || hook.getAppInstallationId().length() > 0) {
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
            sign(headers, hook.getSecret(), body);
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
            if (all.get(iter).getAppInstallationId().length() == 0) {
                out.add(dto(all.get(iter)));
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ apps

    /// An app installation's own subscriptions, the mandatory ones left out.
    @Transactional(readOnly = true)
    public List<WebhookDto> appList(String installationId) {
        List<Webhook> all = hooks.findByAppInstallationId(installationId);
        List<WebhookDto> out = new ArrayList<WebhookDto>();
        for (int iter = 0; iter < all.size(); iter++) {
            Webhook h = all.get(iter);
            if (!MANDATORY.contains(h.getTopic())) {
                out.add(appDto(h));
            }
        }
        return out;
    }

    /// Subscribes an app installation to a topic, signed with the app's key
    /// `signingKey`. The topic and address are checked as a staff hook's are.
    @Transactional(rollbackFor = IOException.class)
    public WebhookDto appCreate(String storeId, String installationId, String signingKey,
            WebhookDto wanted) throws IOException {
        if (hooks.findByAppInstallationId(installationId).size() >= 50) {
            throw new ResponseStatusException(400, "An app has at most 50 subscriptions a store");
        }
        Webhook hook = new Webhook();
        hook.setId(Ids.next());
        hook.setStoreId(storeId);
        hook.setAppInstallationId(installationId);
        hook.setCreatedAt(Instant.now());
        apply(hook, new WebhookDto(null, wanted == null ? null : wanted.topic(),
                wanted == null ? null : wanted.url(), signingKey, true, 0));
        hooks.insert(hook);
        return appDto(hook);
    }

    /// Removes one of an app installation's subscriptions; 404 for anybody
    /// else's, a mandatory one included.
    @Transactional(rollbackFor = IOException.class)
    public List<WebhookDto> appRemove(String installationId, String webhookId) {
        Webhook hook = webhookId == null ? null : hooks.findById(webhookId);
        if (hook == null || !installationId.equals(hook.getAppInstallationId())
                || MANDATORY.contains(hook.getTopic())) {
            throw new ResponseStatusException(404, "No such webhook");
        }
        hooks.delete(hook);
        return appList(installationId);
    }

    /// Makes (or points again and turns on) an installation's hooks for the
    /// mandatory topics, and turns its own subscriptions back on.
    @Transactional(rollbackFor = IOException.class)
    public void appActivate(String storeId, String installationId, String url,
            String signingKey) throws IOException {
        List<Webhook> mine = hooks.findByAppInstallationId(installationId);
        for (int t = 0; t < MANDATORY.size(); t++) {
            Webhook found = null;
            for (int iter = 0; iter < mine.size(); iter++) {
                if (MANDATORY.get(t).equals(mine.get(iter).getTopic())) {
                    found = mine.get(iter);
                }
            }
            if (found == null) {
                found = new Webhook();
                found.setId(Ids.next());
                found.setStoreId(storeId);
                found.setAppInstallationId(installationId);
                found.setTopic(MANDATORY.get(t));
                found.setCreatedAt(Instant.now());
                found.setUrl(url);
                found.setSecret(signingKey);
                found.setActive(true);
                hooks.insert(found);
            }
        }
        mine = hooks.findByAppInstallationId(installationId);
        for (int iter = 0; iter < mine.size(); iter++) {
            Webhook h = mine.get(iter);
            if (MANDATORY.contains(h.getTopic())) {
                h.setUrl(url);
            }
            h.setSecret(signingKey);
            h.setActive(true);
            hooks.save(h);
        }
    }

    /// Turns off every hook of an installation; the rows stay, so that what
    /// is owed after removal (`app/uninstalled`, `shop/redact`) still has
    /// somewhere to go through [#appQueue].
    @Transactional(rollbackFor = IOException.class)
    public void appDeactivate(String installationId) {
        List<Webhook> mine = hooks.findByAppInstallationId(installationId);
        for (int iter = 0; iter < mine.size(); iter++) {
            mine.get(iter).setActive(false);
            hooks.save(mine.get(iter));
        }
    }

    /// Queues `topic` to an installation's hook for it, on or off: the one
    /// way a delivery reaches an app that has been removed.
    @Transactional(rollbackFor = IOException.class)
    public WebhookDelivery appQueue(String installationId, String topic, String payloadJson)
            throws IOException {
        List<Webhook> mine = hooks.findByAppInstallationId(installationId);
        for (int iter = 0; iter < mine.size(); iter++) {
            if (topic.equals(mine.get(iter).getTopic())) {
                return queue(mine.get(iter), topic, payloadJson);
            }
        }
        return null;
    }

    /// An app's view of its hook: the signing key is the app's own, so it is
    /// never answered.
    private static WebhookDto appDto(Webhook h) {
        return new WebhookDto(h.getId(), h.getTopic(), h.getUrl(), "", h.isActive(),
                h.getCreatedAt().toEpochMilli());
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
        // An app's hook is the app's: staff neither see nor change it.
        if (hook == null || !hook.getStoreId().equals(storeId)
                || hook.getAppInstallationId().length() > 0) {
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
