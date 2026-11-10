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
package com.codename1.backend.messaging;

import com.codename1.impl.backend.BackendAccess;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/// A transport whose consumers ask the broker for messages in a loop: the
/// database, Redis Streams, Kafka. It runs each subscription's consumer threads,
/// and turns a handler's failure into a later delivery or a dead letter, so a
/// subclass only says how to poll, acknowledge and reschedule.
///
/// Consumer threads run as part of the server that subscribed, so a handler
/// reaches that server's beans, data source and key/value store.
public abstract class PollingTransport extends MessageTransport {
    /// How long a consumer waits after the broker failed before asking again.
    private static final long FAILURE_PAUSE = 1000;

    /// The retry policy of every subscription.
    protected final RetryPolicy retry;
    private final List<Consumers> subscriptions = new ArrayList<Consumers>();
    private final AtomicBoolean closed = new AtomicBoolean();

    /// A transport retrying by `retry`.
    protected PollingTransport(RetryPolicy retry) {
        this.retry = retry;
    }

    /// One message as polled, with whatever the broker needs to acknowledge it.
    protected static final class Delivery {
        /// The message.
        public final Message message;
        /// The broker's handle: a row id, a stream entry id, an offset.
        public final Object handle;

        /// A delivery of `message` acknowledged through `handle`.
        public Delivery(Message message, Object handle) {
            this.message = message;
            this.handle = handle;
        }
    }

    /// Up to `max` messages of `destination` for `group`, waiting up to
    /// `waitMillis` for the first; an empty list when none came.
    protected abstract List<Delivery> poll(String destination, String group, int max,
                                           long waitMillis) throws IOException;

    /// Acknowledges a handled delivery.
    protected abstract void ack(String destination, String group, Delivery delivery)
            throws IOException;

    /// Has `delivery` delivered again, as `attempt`, no sooner than
    /// `notBefore`. The default sends a copy carrying the attempt and the time
    /// in its headers and acknowledges the original; a consumer that polls the
    /// copy early puts it back with [#notYet].
    protected void redeliver(String destination, String group, Delivery delivery, int attempt,
                             long notBefore) throws IOException {
        send(delivery.message.withHeader(Message.ATTEMPT, Integer.toString(attempt))
                .withHeader(Message.NOT_BEFORE, Long.toString(notBefore)));
        ack(destination, group, delivery);
    }

    /// Sends `delivery` to the dead-letter destination and acknowledges it.
    protected void deadLetter(String destination, String group, Delivery delivery, String error)
            throws IOException {
        send(delivery.message.withHeader(Message.ERROR, error)
                .withHeader(Message.ORIGIN, destination)
                .withHeader(Message.NOT_BEFORE, null)
                .to(RetryPolicy.deadLetter(destination)));
        ack(destination, group, delivery);
    }

    /// `message`, given the identifier the broker knows it by.
    protected static Message identified(Message message, String id) {
        message.setId(id);
        return message;
    }

    /// Whether `message` carries a retry time still in the future.
    protected static boolean notYet(Message message, long now) {
        String nb = message.getHeader(Message.NOT_BEFORE);
        if (nb == null) {
            return false;
        }
        try {
            return Long.parseLong(nb) > now;
        } catch (NumberFormatException err) {
            return false;
        }
    }

    @Override
    public Subscription subscribe(String destination, String group, int concurrency,
                                  MessageHandler handler) throws IOException {
        if (closed.get()) {
            throw new IOException("The " + getName() + " transport is closed");
        }
        Consumers c = new Consumers(destination, group == null ? "default" : group,
                Math.max(1, concurrency), handler);
        synchronized (subscriptions) {
            subscriptions.add(c);
        }
        c.start();
        return c;
    }

    /// Stops every subscription, waiting for the messages being handled.
    protected void stopSubscriptions() {
        closed.set(true);
        List<Consumers> all;
        synchronized (subscriptions) {
            all = new ArrayList<Consumers>(subscriptions);
            subscriptions.clear();
        }
        for (Consumers c : all) {
            c.close();
        }
    }

    /// Hands one delivery to `handler` and settles it. Never throws.
    final void dispatch(String destination, String group, MessageHandler handler, Delivery d) {
        try {
            handler.handle(d.message);
        } catch (Throwable err) { // NOPMD AvoidCatchingThrowable - any failure is a redelivery
            settleFailure(destination, group, d, err);
            return;
        }
        try {
            ack(destination, group, d);
        } catch (IOException err) {
            System.err.println("cn1: a handled message of " + destination
                    + " could not be acknowledged and will be delivered again: "
                    + err.getMessage());
        }
    }

    private void settleFailure(String destination, String group, Delivery d, Throwable err) {
        String text = err.getClass().getName() + (err.getMessage() == null ? ""
                : ": " + err.getMessage());
        int attempt = d.message.getAttempt();
        try {
            if (attempt >= retry.getMaxAttempts()) {
                System.err.println("cn1: a message of " + destination + " failed " + attempt
                        + " times and goes to " + RetryPolicy.deadLetter(destination) + ": "
                        + text);
                deadLetter(destination, group, d, text);
            } else {
                redeliver(destination, group, d, attempt + 1,
                        System.currentTimeMillis() + retry.delayAfter(attempt));
            }
        } catch (IOException io) {
            System.err.println("cn1: a failed message of " + destination
                    + " could not be rescheduled and will be delivered again: "
                    + io.getMessage());
        }
    }

    /// The consumer threads of one subscription.
    private final class Consumers implements Subscription {
        private final String destination;
        private final String group;
        private final MessageHandler handler;
        private final Thread[] threads;
        private final AtomicBoolean running = new AtomicBoolean(true);

        Consumers(String destination, String group, int concurrency, MessageHandler handler) {
            this.destination = destination;
            this.group = group;
            this.handler = handler;
            this.threads = new Thread[concurrency];
            Runnable loop = BackendAccess.get().inServer(new Runnable() {
                @Override
                public void run() {
                    consume();
                }
            });
            for (int i = 0; i < concurrency; i++) {
                threads[i] = new Thread(loop, "cn1-consumer-" + destination + "-" + i);
                threads[i].setDaemon(true);
            }
        }

        void start() {
            for (Thread t : threads) {
                t.start();
            }
        }

        void consume() {
            boolean failing = false;
            while (running.get()) {
                List<Delivery> batch;
                try {
                    batch = poll(destination, group, 1, 1000);
                } catch (IOException err) {
                    if (!running.get()) {
                        return;
                    }
                    if (!failing) {
                        // Once per outage, not once a second for as long as it lasts.
                        System.err.println("cn1: polling " + destination + " failed, retrying: "
                                + err.getMessage());
                        failing = true;
                    }
                    pause(FAILURE_PAUSE);
                    continue;
                }
                if (failing) {
                    System.err.println("cn1: polling " + destination + " works again");
                    failing = false;
                }
                for (Delivery d : batch) {
                    dispatch(destination, group, handler, d);
                }
            }
        }

        private void pause(long millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException err) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public void close() {
            if (!running.getAndSet(false)) {
                return;
            }
            synchronized (subscriptions) {
                subscriptions.remove(this);
            }
            for (Thread t : threads) {
                try {
                    t.join(5000);
                } catch (InterruptedException err) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}
