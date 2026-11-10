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
package com.codename1.backend.messaging.sqs;

import com.codename1.backend.Base64;
import com.codename1.backend.Config;
import com.codename1.backend.Json;
import com.codename1.backend.Web;
import com.codename1.backend.aws.Aws;
import com.codename1.backend.aws.Credentials;
import com.codename1.backend.messaging.Message;
import com.codename1.backend.messaging.MessageTransport;
import com.codename1.backend.messaging.Messaging;
import com.codename1.backend.messaging.PollingTransport;
import com.codename1.backend.messaging.RetryPolicy;
import com.codename1.impl.backend.WireBuffer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/// Messaging over Amazon SQS, signed with the backend's own SigV4 signer and no
/// AWS SDK.
///
/// ```properties
/// cn1.messaging.url=sqs://eu-west-1
/// ```
///
/// The host part is a region, which addresses
/// `sqs.<region>.amazonaws.com`, or a `host:port` endpoint for a compatible
/// service (ElasticMQ, LocalStack), whose region comes from
/// `cn1.messaging.sqs.region` (default `us-east-1`). Only an endpoint on
/// loopback is spoken to over plain HTTP. Credentials are resolved the way
/// [Credentials#resolve()] documents: the environment, then the container,
/// then the instance metadata service.
///
/// A destination is a standard queue, created on first use. SQS allows
/// letters, digits, `-` and `_` in a queue name, so every other character of
/// a destination -- the `.` of `orders.dlq` -- becomes `-`. Listeners of one
/// queue compete for its messages whatever their group: SQS has no consumer
/// groups, and fan-out is SNS's job.
///
/// The body is sent base64 encoded, so any payload survives; the key and
/// headers travel in one message attribute, `cn1-headers`, which keeps clear of
/// SQS's limit of ten attributes. A message from a producer that is not this
/// transport is read as it is: its body as UTF-8 text and its string
/// attributes as headers. SQS limits a message to 256 KiB, which the encoding
/// makes about 190 KiB of payload.
///
/// A message is handed to one consumer for `cn1.messaging.lease` milliseconds
/// (SQS's visibility timeout); one not settled in that time is delivered
/// again. A failed message is not copied: its visibility is pushed back by the
/// retry delay, and SQS's own receive count is its attempt number. A delay is
/// at most twelve hours, SQS's limit.
public final class SqsTransport extends PollingTransport {
    private static final String JSON = "application/x-amz-json-1.0";
    private static final String HEADERS = "cn1-headers";
    private static final String ENCODING = "cn1-encoding";
    private static final String KEY = "cn1-key";
    private static final int MAX_VISIBILITY = 43200;

    private final String host;
    private final String region;
    private final boolean secure;
    private final int leaseSeconds;
    private final Map<String, String> queues = new HashMap<String, String>();
    private final Object credentialLock = new Object();
    private Credentials credentials;
    private final AtomicBoolean closed = new AtomicBoolean();

    /// Registers this transport with [Messaging]; the generated wiring does it.
    public static final class Factory implements Messaging.TransportFactory {
        @Override
        public MessageTransport open(String url, Config config, RetryPolicy retry)
                throws IOException {
            String region = config == null ? null : config.get("cn1.messaging.sqs.region");
            int lease = config == null ? 60000 : config.getInt("cn1.messaging.lease", 60000);
            return new SqsTransport(url, region, retry, lease);
        }
    }

    /// A transport to the region or endpoint `url` names; `region` signs
    /// requests to an endpoint, and may be null for `us-east-1`.
    public SqsTransport(String url, String region, RetryPolicy retry, int leaseMillis)
            throws IOException {
        super(retry);
        int scheme = url.indexOf("://");
        if (scheme < 0 || !"sqs".equalsIgnoreCase(url.substring(0, scheme))) {
            throw new IOException("Not an SQS URL: " + url);
        }
        String rest = url.substring(scheme + 3);
        int slash = rest.indexOf('/');
        if (slash >= 0) {
            rest = rest.substring(0, slash);
        }
        if (rest.length() == 0) {
            throw new IOException("The SQS URL names no region: " + url);
        }
        if (rest.indexOf(':') < 0 && rest.indexOf('.') < 0) {
            this.region = rest;
            this.host = "sqs." + rest + ".amazonaws.com";
            this.secure = true;
        } else {
            this.region = region == null || region.trim().length() == 0 ? "us-east-1"
                    : region.trim();
            this.host = rest;
            String name = rest.indexOf(':') < 0 ? rest : rest.substring(0, rest.lastIndexOf(':'));
            this.secure = !"localhost".equalsIgnoreCase(name) && !"127.0.0.1".equals(name) // NOPMD AvoidUsingHardCodedIP - loopback is the one place plain HTTP is allowed
                    && !"[::1]".equals(name);
        }
        this.leaseSeconds = Math.max(1, Math.min(MAX_VISIBILITY, leaseMillis / 1000));
    }

    @Override
    public String getName() {
        return "sqs";
    }

    private Credentials credentials() throws IOException {
        synchronized (credentialLock) {
            if (credentials == null || credentials.isExpiring(60000)) {
                credentials = Credentials.resolve();
            }
            return credentials;
        }
    }

    /// One SQS action; the reply's JSON object.
    private Map call(String action, Map<String, Object> request) throws IOException {
        if (closed.get()) {
            throw new IOException("The sqs transport is closed");
        }
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("content-type", JSON);
        headers.put("x-amz-target", "AmazonSQS." + action);
        Web.Result r = Aws.send(credentials(), region, "sqs", "POST", host, "/", null, headers,
                WireBuffer.utf8(Json.write(request)), null, secure);
        String body = r.getBodyAsString();
        if (!r.isSuccess()) {
            throw new SqsException(action, r.getStatus(), body == null ? r.getError() : body);
        }
        if (body == null || body.trim().length() == 0) {
            return new HashMap();
        }
        return Json.parseObject(body);
    }

    /// An SQS error reply.
    static final class SqsException extends IOException {
        private static final long serialVersionUID = 1L;

        SqsException(String action, int status, String body) {
            super("SQS refused " + action + " (" + status + "): " + body);
        }
    }

    /// The SQS queue name `destination` maps to.
    static String queueName(String destination) {
        StringBuilder sb = new StringBuilder(destination.length());
        for (int i = 0; i < destination.length() && sb.length() < 80; i++) {
            char c = destination.charAt(i);
            boolean ok = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9'
                    || c == '-' || c == '_';
            sb.append(ok ? c : '-');
        }
        return sb.toString();
    }

    private String queue(String destination) throws IOException {
        synchronized (queues) {
            String url = queues.get(destination);
            if (url != null) {
                return url;
            }
        }
        Map<String, Object> req = new LinkedHashMap<String, Object>();
        req.put("QueueName", queueName(destination));
        Object url = call("CreateQueue", req).get("QueueUrl");
        if (!(url instanceof String)) {
            throw new IOException("SQS created " + destination + " but named no queue URL");
        }
        synchronized (queues) {
            queues.put(destination, (String) url);
        }
        return (String) url;
    }

    @Override
    public void send(Message message) throws IOException {
        Map<String, String> headers = new LinkedHashMap<String, String>(message.getHeaders());
        if (message.getKey() != null) {
            headers.put(KEY, message.getKey());
        }
        Map<String, Object> attributes = new LinkedHashMap<String, Object>();
        // SQS refuses an empty body, so an empty payload travels as one space.
        byte[] payload = message.getPayload();
        attributes.put(ENCODING, stringAttribute(payload.length == 0 ? "empty" : "base64"));
        if (!headers.isEmpty()) {
            attributes.put(HEADERS, stringAttribute(Json.write(headers)));
        }
        Map<String, Object> req = new LinkedHashMap<String, Object>();
        req.put("QueueUrl", queue(message.getDestination()));
        req.put("MessageBody", payload.length == 0 ? " " : Base64.encode(payload));
        req.put("MessageAttributes", attributes);
        call("SendMessage", req);
    }

    private static Map<String, Object> stringAttribute(String value) {
        Map<String, Object> a = new LinkedHashMap<String, Object>();
        a.put("DataType", "String");
        a.put("StringValue", value);
        return a;
    }

    @Override
    protected List<Delivery> poll(String destination, String group, int max, long waitMillis)
            throws IOException {
        List<String> all = new ArrayList<String>(1);
        all.add("All");
        List<String> count = new ArrayList<String>(1);
        count.add("ApproximateReceiveCount");
        Map<String, Object> req = new LinkedHashMap<String, Object>();
        req.put("QueueUrl", queue(destination));
        req.put("MaxNumberOfMessages", Integer.valueOf(Math.max(1, Math.min(10, max))));
        req.put("WaitTimeSeconds", Integer.valueOf((int) Math.max(0, Math.min(20,
                waitMillis / 1000))));
        req.put("VisibilityTimeout", Integer.valueOf(leaseSeconds));
        req.put("MessageAttributeNames", all);
        req.put("AttributeNames", count);
        req.put("MessageSystemAttributeNames", count);
        Object messages = call("ReceiveMessage", req).get("Messages");
        List<Delivery> out = new ArrayList<Delivery>();
        if (!(messages instanceof List)) {
            return out;
        }
        for (Object o : (List) messages) {
            if (o instanceof Map) {
                Delivery d = delivery(destination, (Map) o);
                if (d != null) {
                    out.add(d);
                }
            }
        }
        return out;
    }

    private static Delivery delivery(String destination, Map m) throws IOException {
        Object handle = m.get("ReceiptHandle");
        Object body = m.get("Body");
        if (!(handle instanceof String)) {
            return null;
        }
        Map<String, String> headers = new LinkedHashMap<String, String>();
        String encoding = null;
        Object attributes = m.get("MessageAttributes");
        if (attributes instanceof Map) {
            Iterator it = ((Map) attributes).entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry e = (Map.Entry) it.next();
                Object v = e.getValue() instanceof Map ? ((Map) e.getValue()).get("StringValue")
                        : null;
                if (!(v instanceof String)) {
                    continue;
                }
                String name = String.valueOf(e.getKey());
                if (ENCODING.equals(name)) {
                    encoding = (String) v;
                } else if (HEADERS.equals(name)) {
                    Map parsed = Json.parseObject((String) v);
                    Iterator h = parsed.entrySet().iterator();
                    while (h.hasNext()) {
                        Map.Entry he = (Map.Entry) h.next();
                        if (he.getValue() instanceof String) {
                            headers.put(String.valueOf(he.getKey()), (String) he.getValue());
                        }
                    }
                } else {
                    headers.put(name, (String) v);
                }
            }
        }
        if (headers.get(Message.ATTEMPT) == null) {
            Object system = m.get("Attributes");
            Object count = system instanceof Map ? ((Map) system).get("ApproximateReceiveCount")
                    : null;
            if (count instanceof String) {
                headers.put(Message.ATTEMPT, (String) count);
            }
        }
        String key = headers.remove(KEY);
        String text = body instanceof String ? (String) body : "";
        byte[] payload = "empty".equals(encoding) ? new byte[0]
                : "base64".equals(encoding) ? Base64.decode(text) : WireBuffer.utf8(text);
        Object id = m.get("MessageId");
        return new Delivery(identified(new Message(destination, key, payload, headers),
                id instanceof String ? (String) id : null), handle);
    }

    @Override
    protected void ack(String destination, String group, Delivery delivery) throws IOException {
        Map<String, Object> req = new LinkedHashMap<String, Object>();
        req.put("QueueUrl", queue(destination));
        req.put("ReceiptHandle", delivery.handle);
        call("DeleteMessage", req);
    }

    /// Keeps the message where it is, hidden until `notBefore`: SQS counts
    /// the next receive as the next attempt.
    @Override
    protected void redeliver(String destination, String group, Delivery delivery, int attempt,
                             long notBefore) throws IOException {
        long seconds = (notBefore - System.currentTimeMillis() + 999) / 1000;
        Map<String, Object> req = new LinkedHashMap<String, Object>();
        req.put("QueueUrl", queue(destination));
        req.put("ReceiptHandle", delivery.handle);
        req.put("VisibilityTimeout", Integer.valueOf((int) Math.max(0,
                Math.min(MAX_VISIBILITY, seconds))));
        call("ChangeMessageVisibility", req);
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
