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
package com.codename1.backend.messaging.pubsub;

import com.codename1.backend.Base64;
import com.codename1.backend.Config;
import com.codename1.backend.Json;
import com.codename1.backend.Web;
import com.codename1.backend.messaging.Message;
import com.codename1.backend.messaging.MessageTransport;
import com.codename1.backend.messaging.Messaging;
import com.codename1.backend.messaging.PollingTransport;
import com.codename1.backend.messaging.RetryPolicy;
import com.codename1.backend.security.oauth2.client.credentials.AccessToken;
import com.codename1.backend.security.oauth2.client.credentials.AccessTokenGrant;
import com.codename1.backend.security.oauth2.client.credentials.AccessTokenProvider;
import com.codename1.backend.security.oauth2.client.credentials.GoogleServiceAccountCredentials;
import com.codename1.backend.security.oauth2.client.credentials.TokenAcquisitionException;
import com.codename1.impl.backend.WireBuffer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/// Messaging over Google Cloud Pub/Sub's REST API, with no Google library.
///
/// ```properties
/// cn1.messaging.url=pubsub://my-project
/// ```
///
/// A destination is a topic, and a listener's group a subscription to it named
/// `<topic>.<group>`; both are created on first use. Every group therefore
/// sees every message, and the listeners of one group compete for them.
/// Pub/Sub keeps a message only for the subscriptions that exist when it is
/// published, so a message sent before its group ever subscribed is not
/// delivered to that group: create the subscriptions at deployment time, or
/// call [#declare(String, String)] before the first send. Pub/Sub allows
/// letters, digits and `-_.~+%` in a name that starts with a letter; any
/// other character becomes `-`, and a name that does not start with a letter
/// gains a `t-`.
///
/// Credentials, in order: the service-account key file named by
/// `cn1.messaging.pubsub.credentials` or `GOOGLE_APPLICATION_CREDENTIALS`,
/// then the metadata server of the Google Cloud machine the server runs on.
/// `PUBSUB_EMULATOR_HOST` (or `cn1.messaging.pubsub.emulator-host`) sends
/// everything, unauthenticated, to the emulator at that `host:port` instead.
///
/// A message is handed to one consumer for `cn1.messaging.lease` milliseconds
/// (the subscription's ack deadline, at most ten minutes); one not settled in
/// that time is delivered again. A failed message is not copied, since a
/// copy published to the topic would reach every other group too: its
/// deadline is moved to when the retry is due, at most ten minutes away, and
/// its attempt is counted in this server's memory. After a restart, or when
/// another replica receives the retry, the count starts again, so a message
/// may be tried more often than the retry policy says before it reaches the
/// dead-letter topic.
public final class PubSubTransport extends PollingTransport {
    private static final String API = "https://pubsub.googleapis.com/v1/";
    private static final String SCOPE = "https://www.googleapis.com/auth/pubsub";
    private static final String METADATA_TOKEN = "http://metadata.google.internal/computeMetadata"
            + "/v1/instance/service-accounts/default/token";
    private static final String KEY = "cn1-key";
    private static final String EMPTY = "cn1-empty";
    private static final int MAX_DEADLINE = 600;
    private static final int MAX_TRACKED = 10000;

    private final String project;
    private final String base;
    private final AccessTokenProvider tokens;
    private final int leaseSeconds;
    private final Set<String> declared = Collections.synchronizedSet(new HashSet<String>());
    /// Attempts of messages this server put back, by subscription and id.
    private final Map<String, Integer> attempts = new Attempts();
    private final AtomicBoolean closed = new AtomicBoolean();

    /// The newest attempts this server counted; the oldest are forgotten.
    private static final class Attempts extends LinkedHashMap<String, Integer> {
        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
            return size() > MAX_TRACKED;
        }
    }

    /// Registers this transport with [Messaging]; the generated wiring does it.
    public static final class Factory implements Messaging.TransportFactory {
        @Override
        public MessageTransport open(String url, Config config, RetryPolicy retry)
                throws IOException {
            String emulator = config == null ? null
                    : config.get("cn1.messaging.pubsub.emulator-host");
            if (emulator == null) {
                emulator = System.getenv("PUBSUB_EMULATOR_HOST");
            }
            String key = config == null ? null : config.get("cn1.messaging.pubsub.credentials");
            if (key == null) {
                key = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
            }
            int lease = config == null ? 60000 : config.getInt("cn1.messaging.lease", 60000);
            return new PubSubTransport(url, emulator, key, retry, lease);
        }
    }

    /// A transport to the project `url` names (`pubsub://project`), through
    /// the emulator at `emulatorHost` when it is set, otherwise authenticated
    /// with the key file `keyFile` or, when that is null, the metadata server.
    public PubSubTransport(String url, String emulatorHost, String keyFile, RetryPolicy retry,
                           int leaseMillis) throws IOException {
        super(retry);
        int scheme = url.indexOf("://");
        if (scheme < 0 || !"pubsub".equalsIgnoreCase(url.substring(0, scheme))) {
            throw new IOException("Not a Pub/Sub URL: " + url);
        }
        String rest = url.substring(scheme + 3);
        int slash = rest.indexOf('/');
        project = slash < 0 ? rest : rest.substring(0, slash);
        if (project.length() == 0) {
            throw new IOException("The Pub/Sub URL names no project: " + url);
        }
        if (emulatorHost != null && emulatorHost.trim().length() > 0) {
            base = "http://" + emulatorHost.trim() + "/v1/";
            tokens = null;
        } else {
            base = API;
            AccessTokenGrant grant = keyFile != null && keyFile.trim().length() > 0
                    ? GoogleServiceAccountCredentials.fromFile(keyFile.trim()).grant(SCOPE)
                    : new MetadataServerGrant();
            tokens = new AccessTokenProvider(grant);
        }
        leaseSeconds = Math.max(10, Math.min(MAX_DEADLINE, leaseMillis / 1000));
    }

    @Override
    public String getName() {
        return "pubsub";
    }

    /// The default service account of the Google Cloud machine this runs on.
    private static final class MetadataServerGrant implements AccessTokenGrant {
        @Override
        public AccessToken fetch() {
            List<String> headers = new ArrayList<String>(1);
            headers.add("Metadata-Flavor: Google");
            try {
                Web.Result r = Web.request("GET", METADATA_TOKEN, headers, null);
                if (!r.isSuccess()) {
                    throw new TokenAcquisitionException("The metadata server refused a token ("
                            + r.getStatus() + ")", r.getStatus(), null, true, null);
                }
                Map reply = Json.parseObject(r.getBodyAsString());
                Object token = reply.get("access_token");
                Object expires = reply.get("expires_in");
                if (!(token instanceof String)) {
                    throw new TokenAcquisitionException("The metadata server sent no token",
                            r.getStatus(), null, true, null);
                }
                long seconds = expires instanceof Number ? ((Number) expires).longValue() : 300;
                return new AccessToken((String) token, "Bearer",
                        System.currentTimeMillis() + seconds * 1000,
                        Collections.<String>emptySet());
            } catch (IOException err) {
                throw new TokenAcquisitionException("No Google credentials: no key file is "
                        + "configured and the metadata server did not answer: "
                        + err.getMessage(), 0, null, true, err);
            }
        }
    }

    /// One REST call; the reply, or null when `tolerate` is its status.
    private Map call(String method, String path, Map<String, Object> body, int tolerate)
            throws IOException {
        if (closed.get()) {
            throw new IOException("The pubsub transport is closed");
        }
        List<String> headers = new ArrayList<String>(2);
        headers.add("Content-Type: application/json");
        if (tokens != null) {
            try {
                headers.add("Authorization: Bearer " + tokens.getTokenValue());
            } catch (TokenAcquisitionException err) {
                throw new IOException(err.getMessage(), err);
            }
        }
        Web.Result r = Web.request(method, base + path, headers,
                body == null ? null : WireBuffer.utf8(Json.write(body)));
        if (r.getStatus() == tolerate) {
            return null;
        }
        String text = r.getBodyAsString();
        if (!r.isSuccess()) {
            if (r.getStatus() == 401 && tokens != null) {
                tokens.invalidate();
            }
            throw new IOException("Pub/Sub refused " + method + " " + path + " ("
                    + r.getStatus() + "): " + (text == null ? r.getError() : text));
        }
        return text == null || text.trim().length() == 0 ? new LinkedHashMap()
                : Json.parseObject(text);
    }

    /// The Pub/Sub name `name` maps to.
    static String resourceName(String name) {
        StringBuilder sb = new StringBuilder(name.length() + 2);
        char first = name.length() == 0 ? '0' : name.charAt(0);
        if (!(first >= 'a' && first <= 'z' || first >= 'A' && first <= 'Z')) {
            sb.append("t-");
        }
        for (int i = 0; i < name.length() && sb.length() < 255; i++) {
            char c = name.charAt(i);
            boolean ok = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9'
                    || c == '-' || c == '_' || c == '.' || c == '~' || c == '+' || c == '%';
            sb.append(ok ? c : '-');
        }
        while (sb.length() < 3) {
            sb.append('-');
        }
        return sb.toString();
    }

    private String topic(String destination) {
        return "projects/" + project + "/topics/" + resourceName(destination);
    }

    private String subscription(String destination, String group) {
        return "projects/" + project + "/subscriptions/"
                + resourceName(destination + "." + (group == null ? "default" : group));
    }

    private String declareTopic(String destination) throws IOException {
        String topic = topic(destination);
        if (!declared.contains(topic)) {
            call("PUT", topic, new LinkedHashMap<String, Object>(), 409);
            declared.add(topic);
        }
        return topic;
    }

    /// Creates `destination`'s topic and `group`'s subscription to it unless
    /// they exist, so messages sent from now on are kept for that group even
    /// before its listener starts.
    public String declare(String destination, String group) throws IOException {
        String topic = declareTopic(destination);
        String sub = subscription(destination, group);
        if (!declared.contains(sub)) {
            Map<String, Object> req = new LinkedHashMap<String, Object>();
            req.put("topic", topic);
            req.put("ackDeadlineSeconds", Integer.valueOf(leaseSeconds));
            call("PUT", sub, req, 409);
            declared.add(sub);
        }
        return sub;
    }

    @Override
    public void send(Message message) throws IOException {
        Map<String, Object> attributes = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, String> h : message.getHeaders().entrySet()) {
            if (h.getValue() != null) {
                attributes.put(h.getKey(), h.getValue());
            }
        }
        if (message.getKey() != null) {
            attributes.put(KEY, message.getKey());
        }
        byte[] payload = message.getPayload();
        if (payload.length == 0 && attributes.isEmpty()) {
            // Pub/Sub refuses a message with neither data nor attributes.
            attributes.put(EMPTY, "1");
        }
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        if (payload.length > 0) {
            m.put("data", Base64.encode(payload));
        }
        if (!attributes.isEmpty()) {
            m.put("attributes", attributes);
        }
        List<Object> messages = new ArrayList<Object>(1);
        messages.add(m);
        Map<String, Object> req = new LinkedHashMap<String, Object>();
        req.put("messages", messages);
        call("POST", declareTopic(message.getDestination()) + ":publish", req, -1);
    }

    @Override
    protected List<Delivery> poll(String destination, String group, int max, long waitMillis)
            throws IOException {
        String sub = declare(destination, group);
        Map<String, Object> req = new LinkedHashMap<String, Object>();
        req.put("maxMessages", Integer.valueOf(Math.max(1, max)));
        // A pull that waits holds the consumer past its subscription's close;
        // an empty answer is followed by a short sleep instead.
        req.put("returnImmediately", Boolean.TRUE);
        Object received = call("POST", sub + ":pull", req, -1).get("receivedMessages");
        List<Delivery> out = new ArrayList<Delivery>();
        if (received instanceof List) {
            for (Object o : (List) received) {
                if (o instanceof Map) {
                    Delivery d = delivery(destination, sub, (Map) o);
                    if (d != null) {
                        out.add(d);
                    }
                }
            }
        }
        if (out.isEmpty()) {
            try {
                Thread.sleep(Math.min(waitMillis, 200));
            } catch (InterruptedException err) {
                Thread.currentThread().interrupt();
            }
        }
        return out;
    }

    private Delivery delivery(String destination, String sub, Map r) throws IOException {
        Object ackId = r.get("ackId");
        Object m = r.get("message");
        if (!(ackId instanceof String) || !(m instanceof Map)) {
            return null;
        }
        Map message = (Map) m;
        Map<String, String> headers = new LinkedHashMap<String, String>();
        Object attributes = message.get("attributes");
        if (attributes instanceof Map) {
            Iterator it = ((Map) attributes).entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry e = (Map.Entry) it.next();
                if (e.getValue() instanceof String) {
                    headers.put(String.valueOf(e.getKey()), (String) e.getValue());
                }
            }
        }
        headers.remove(EMPTY);
        String key = headers.remove(KEY);
        Object id = message.get("messageId");
        String messageId = id instanceof String ? (String) id : (String) ackId;
        Integer attempt;
        synchronized (attempts) {
            attempt = attempts.get(sub + "\n" + messageId);
        }
        if (attempt != null) {
            headers.put(Message.ATTEMPT, attempt.toString());
        }
        Object data = message.get("data");
        byte[] payload = data instanceof String ? Base64.decode((String) data) : new byte[0];
        return new Delivery(identified(new Message(destination, key, payload, headers),
                messageId), new String[] {sub, (String) ackId, messageId});
    }

    @Override
    protected void ack(String destination, String group, Delivery delivery) throws IOException {
        String[] h = (String[]) delivery.handle;
        synchronized (attempts) {
            attempts.remove(h[0] + "\n" + h[2]);
        }
        List<String> ids = new ArrayList<String>(1);
        ids.add(h[1]);
        Map<String, Object> req = new LinkedHashMap<String, Object>();
        req.put("ackIds", ids);
        call("POST", h[0] + ":acknowledge", req, -1);
    }

    /// Keeps the message in its subscription, due again at `notBefore` (at
    /// most ten minutes away), and remembers which attempt that will be.
    @Override
    protected void redeliver(String destination, String group, Delivery delivery, int attempt,
                             long notBefore) throws IOException {
        String[] h = (String[]) delivery.handle;
        synchronized (attempts) {
            attempts.put(h[0] + "\n" + h[2], Integer.valueOf(attempt));
        }
        long seconds = (notBefore - System.currentTimeMillis() + 999) / 1000;
        List<String> ids = new ArrayList<String>(1);
        ids.add(h[1]);
        Map<String, Object> req = new LinkedHashMap<String, Object>();
        req.put("ackIds", ids);
        req.put("ackDeadlineSeconds", Integer.valueOf((int) Math.max(0,
                Math.min(MAX_DEADLINE, seconds))));
        call("POST", h[0] + ":modifyAckDeadline", req, -1);
    }

    @Override
    public boolean isHealthy() {
        return !closed.get();
    }

    @Override
    public void close() {
        if (!closed.getAndSet(true)) {
            stopSubscriptions();
        }
    }
}
