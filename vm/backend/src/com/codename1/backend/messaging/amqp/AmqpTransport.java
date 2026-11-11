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
package com.codename1.backend.messaging.amqp;

import com.codename1.backend.Config;
import com.codename1.backend.messaging.Message;
import com.codename1.backend.messaging.MessageHandler;
import com.codename1.backend.messaging.MessageTransport;
import com.codename1.backend.messaging.Messaging;
import com.codename1.backend.messaging.PollingTransport;
import com.codename1.backend.messaging.RetryPolicy;
import com.codename1.backend.messaging.Subscription;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/// Messaging over RabbitMQ, or any AMQP 0-9-1 broker, with no client library.
///
/// ```properties
/// cn1.messaging.url=amqp://app:secret@rabbit:5672/%2F
/// ```
///
/// A destination is a durable queue of that name, declared on first use and
/// published to through the default exchange, persistent and with publisher
/// confirms: [#send] returns once the broker has the message. Every listener
/// of a queue competes for its messages, whatever its group says -- a fan-out
/// to several groups is an exchange the application binds itself.
///
/// A subscription is one connection consuming with a prefetch equal to its
/// concurrency, so the broker holds back what no consumer thread is free to
/// take; one reader thread receives deliveries and the consumer threads
/// acknowledge each after its handler returns. A message whose handler failed
/// waits in `<queue>.retry.<millis>`, a queue whose time to live dead-letters it
/// back to the original, so a retry occupies no consumer while it waits. One
/// that fails every attempt goes to `<queue>.dlq`.
public final class AmqpTransport extends PollingTransport {
    private static final String KEY = "cn1-key";
    private final String url;
    private final int timeout;
    private final Object publishLock = new Object();
    private AmqpConnection publisher;
    private final Set<String> declared = new HashSet<String>();
    private final Map<String, Consumer> consumers = new HashMap<String, Consumer>();
    private final AtomicBoolean closed = new AtomicBoolean();

    /// Registers this transport with [Messaging]; the generated wiring does it.
    public static final class Factory implements Messaging.TransportFactory {
        @Override
        public MessageTransport open(String url, Config config, RetryPolicy retry)
                throws IOException {
            return new AmqpTransport(url, retry,
                    config == null ? 5000 : config.getInt("cn1.messaging.timeout", 5000));
        }
    }

    /// A transport to the broker `url` names; it connects on first use.
    public AmqpTransport(String url, RetryPolicy retry, int timeoutMillis) {
        super(retry);
        this.url = url;
        this.timeout = timeoutMillis;
    }

    @Override
    public String getName() {
        return "amqp";
    }

    // ------------------------------------------------------------- sending

    @Override
    public void send(Message message) throws IOException {
        publish(message, message.getDestination(), null);
    }

    private void publish(Message message, String queue, Map<String, Object> queueArguments)
            throws IOException {
        Map<String, Object> headers = new LinkedHashMap<String, Object>();
        headers.putAll(message.getHeaders());
        String contentType = message.getHeader(Message.CONTENT_TYPE);
        headers.remove(Message.CONTENT_TYPE);
        if (message.getKey() != null) {
            headers.put(KEY, message.getKey());
        }
        synchronized (publishLock) {
            boolean fresh = publisher == null;
            try {
                publishOnce(queue, queueArguments, contentType, headers, message.getPayload());
            } catch (IOException err) {
                dropPublisher();
                if (fresh || closed.get()) {
                    throw err;
                }
                // The connection was idle and the broker or the network dropped
                // it; once more on a new one.
                publishOnce(queue, queueArguments, contentType, headers, message.getPayload());
            }
        }
    }

    private void publishOnce(String queue, Map<String, Object> queueArguments, String contentType,
                             Map<String, Object> headers, byte[] body) throws IOException {
        if (closed.get()) {
            throw new IOException("The amqp transport is closed");
        }
        if (publisher == null) {
            AmqpConnection c = AmqpConnection.open(url, timeout);
            try {
                AmqpConnection.Out select = new AmqpConnection.Out();
                select.octet(0);
                c.rpc(AmqpConnection.CONFIRM, 10, select, 11);
            } catch (IOException err) {
                c.close();
                throw err;
            }
            publisher = c;
            declared.clear();
        }
        // A retry queue is declared on every use: a declare is what keeps its
        // expiry from deleting it while messages still wait in it.
        if (queueArguments != null || declared.add(queue)) {
            publisher.declare(queue, queueArguments);
        }
        publisher.publish(queue, contentType, headers, body);
        publisher.awaitConfirm();
    }

    private void dropPublisher() {
        if (publisher != null) {
            publisher.close();
            publisher = null;
        }
    }

    @Override
    protected void redeliver(String destination, String group, Delivery delivery, int attempt,
                             long notBefore) throws IOException {
        long delay = Math.max(1, notBefore - System.currentTimeMillis());
        Map<String, Object> args = new LinkedHashMap<String, Object>();
        args.put("x-message-ttl", Long.valueOf(delay));
        args.put("x-dead-letter-exchange", "");
        args.put("x-dead-letter-routing-key", destination);
        // Longer than any message in it can wait, so it is deleted only once
        // empty and unused.
        args.put("x-expires", Long.valueOf(delay * 2 + 60000));
        publish(delivery.message.withHeader(Message.ATTEMPT, Integer.toString(attempt)),
                destination + ".retry." + delay, args);
        ack(destination, group, delivery);
    }

    // ----------------------------------------------------------- receiving

    private static String key(String destination, String group) {
        return destination + "\n" + group;
    }

    @Override
    public Subscription subscribe(String destination, String group, int concurrency,
                                  MessageHandler handler) throws IOException {
        final String k = key(destination, group == null ? "default" : group);
        final Consumer c;
        synchronized (consumers) {
            Consumer existing = consumers.get(k);
            c = existing == null ? new Consumer(destination) : existing;
            c.prefetch += Math.max(1, concurrency);
            c.users++;
            if (existing == null) {
                consumers.put(k, c);
            }
        }
        try {
            c.connect();
        } catch (IOException err) {
            release(k, c, concurrency);
            throw err;
        }
        final Subscription inner;
        try {
            inner = super.subscribe(destination, group, concurrency, handler);
        } catch (IOException err) {
            release(k, c, concurrency);
            throw err;
        }
        final int n = Math.max(1, concurrency);
        return new Subscription() {
            private final AtomicBoolean done = new AtomicBoolean();

            @Override
            public void close() {
                if (!done.getAndSet(true)) {
                    inner.close();
                    release(k, c, n);
                }
            }
        };
    }

    private void release(String k, Consumer c, int concurrency) {
        boolean last;
        synchronized (consumers) {
            c.users--;
            c.prefetch -= Math.max(1, concurrency);
            last = c.users <= 0;
            if (last) {
                consumers.remove(k);
            }
        }
        if (last) {
            c.close();
        }
    }

    @Override
    protected List<Delivery> poll(String destination, String group, int max, long waitMillis)
            throws IOException {
        Consumer c;
        synchronized (consumers) {
            c = consumers.get(key(destination, group));
        }
        if (c == null) {
            throw new IOException("Not subscribed to " + destination);
        }
        return c.take(max, waitMillis);
    }

    @Override
    protected void ack(String destination, String group, Delivery delivery) throws IOException {
        Tag tag = (Tag) delivery.handle;
        if (tag.link.isBroken()) {
            // The broker puts back what a lost connection had not acknowledged,
            // so this message comes again.
            return;
        }
        tag.link.ack(tag.deliveryTag);
    }

    /// A delivery tag and the connection it belongs to.
    private static final class Tag {
        final AmqpConnection link;
        final long deliveryTag;

        Tag(AmqpConnection link, long deliveryTag) {
            this.link = link;
            this.deliveryTag = deliveryTag;
        }
    }

    /// The connection consuming one queue for one group, and what its reader
    /// thread has received.
    private final class Consumer {
        private final String queue;
        int prefetch;
        int users;
        private final LinkedList<Delivery> received = new LinkedList<Delivery>();
        private AmqpConnection link;
        private int applied;
        private boolean stopped;

        Consumer(String queue) {
            this.queue = queue;
        }

        /// Connects, or applies a new prefetch to the connection there is.
        synchronized void connect() throws IOException {
            if (stopped) {
                throw new IOException("The amqp transport is closed");
            }
            if (link != null && !link.isBroken()) {
                if (applied != prefetch) {
                    qos(link);
                }
                return;
            }
            received.clear();
            AmqpConnection c = AmqpConnection.open(url, timeout);
            try {
                c.declare(queue, null);
                qos(c);
                AmqpConnection.Out consume = new AmqpConnection.Out();
                consume.short16(0);
                consume.shortstr(queue);
                consume.shortstr("");
                consume.octet(0);
                consume.table(null);
                c.rpc(AmqpConnection.BASIC, 20, consume, 21);
            } catch (IOException err) {
                c.close();
                throw err;
            }
            link = c;
            final AmqpConnection reading = c;
            Thread reader = new Thread(new Runnable() {
                @Override
                public void run() {
                    readLoop(reading);
                }
            }, "cn1-amqp-" + queue);
            reader.setDaemon(true);
            reader.start();
        }

        private void qos(AmqpConnection c) throws IOException {
            AmqpConnection.Out qos = new AmqpConnection.Out();
            qos.int32(0);
            qos.short16(Math.min(prefetch, 65535));
            qos.octet(0);
            c.rpc(AmqpConnection.BASIC, 10, qos, 11);
            applied = prefetch;
        }

        /// Receives the deliveries of `c` until it is lost or closed.
        void readLoop(AmqpConnection c) {
            try {
                while (true) {
                    Delivery d = readDelivery(c);
                    if (d != null) {
                        synchronized (this) {
                            received.add(d);
                            notifyAll();
                        }
                    }
                }
            } catch (IOException err) {
                synchronized (this) {
                    if (!stopped && link == c) { // NOPMD CompareObjectsWithEquals - identity
                        System.err.println("cn1: the AMQP connection consuming " + queue
                                + " was lost: " + err.getMessage());
                    }
                    notifyAll();
                }
                c.close();
            }
        }

        /// The next delivery, or null after a frame that was not one.
        private Delivery readDelivery(AmqpConnection c) throws IOException {
            AmqpConnection.Frame f = c.read();
            if (f.type != AmqpConnection.METHOD) {
                return null;
            }
            AmqpConnection.In in = new AmqpConnection.In(f.payload);
            int cls = in.shortU();
            int m = in.shortU();
            if (cls == AmqpConnection.BASIC && m == 30) {
                // The broker cancelled the consumer: the queue was deleted.
                throw new IOException("The broker cancelled the consumer of " + queue);
            }
            if (cls != AmqpConnection.BASIC || m != 60) {
                c.closed(in, cls, m, "deliver from " + queue);
                return null;
            }
            in.shortstr();
            long deliveryTag = in.int64();
            AmqpConnection.Frame header = c.read();
            if (header.type != AmqpConnection.HEADER) {
                throw new IOException("The AMQP broker sent a delivery with no header");
            }
            AmqpConnection.In h = new AmqpConnection.In(header.payload);
            h.shortU();
            h.shortU();
            long size = h.int64();
            if (size > Integer.MAX_VALUE - 8) {
                throw new IOException("A message of " + size + " bytes is too large");
            }
            int flags = h.shortU();
            String contentType = null;
            Map<String, String> headers = new LinkedHashMap<String, String>();
            if ((flags & (1 << 15)) != 0) {
                contentType = h.shortstr();
            }
            if ((flags & (1 << 14)) != 0) {
                h.shortstr();
            }
            if ((flags & (1 << 13)) != 0) {
                headers = h.table();
            }
            byte[] body = new byte[(int) size];
            int at = 0;
            while (at < body.length) {
                AmqpConnection.Frame part = c.read();
                if (part.type != AmqpConnection.BODY || at + part.payload.length > body.length) {
                    throw new IOException("The AMQP broker sent a malformed message body");
                }
                System.arraycopy(part.payload, 0, body, at, part.payload.length);
                at += part.payload.length;
            }
            String key = headers.remove(KEY);
            if (contentType != null && !headers.containsKey(Message.CONTENT_TYPE)) {
                headers.put(Message.CONTENT_TYPE, contentType);
            }
            Message message = identified(new Message(queue, key, body, headers),
                    Long.toString(deliveryTag));
            return new Delivery(message, new Tag(c, deliveryTag));
        }

        /// Up to `max` deliveries, waiting up to `waitMillis` for the first;
        /// reconnects first when the connection was lost.
        List<Delivery> take(int max, long waitMillis) throws IOException {
            synchronized (this) {
                if (!stopped && (link == null || link.isBroken())) {
                    connect();
                }
                long until = System.currentTimeMillis() + waitMillis;
                while (received.isEmpty() && !stopped && link != null && !link.isBroken()) {
                    long left = until - System.currentTimeMillis();
                    if (left <= 0) {
                        break;
                    }
                    try {
                        wait(left);
                    } catch (InterruptedException err) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                List<Delivery> out = new ArrayList<Delivery>();
                while (!received.isEmpty() && out.size() < max) {
                    out.add(received.removeFirst());
                }
                return out;
            }
        }

        void close() {
            AmqpConnection c;
            synchronized (this) {
                stopped = true;
                c = link;
                received.clear();
                notifyAll();
            }
            if (c != null) {
                c.close();
            }
        }
    }

    @Override
    public boolean isHealthy() {
        if (closed.get()) {
            return false;
        }
        synchronized (publishLock) {
            return publisher == null || !publisher.isBroken();
        }
    }

    @Override
    public void close() {
        if (closed.getAndSet(true)) {
            return;
        }
        stopSubscriptions();
        List<Consumer> all;
        synchronized (consumers) {
            all = new ArrayList<Consumer>(consumers.values());
            consumers.clear();
        }
        for (Consumer c : all) {
            c.close();
        }
        synchronized (publishLock) {
            dropPublisher();
        }
    }
}
