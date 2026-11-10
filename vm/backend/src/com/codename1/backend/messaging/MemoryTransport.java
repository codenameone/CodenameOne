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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/// Queues inside this process: `cn1.messaging.url=memory:`. What development
/// and tests run on when there is no database to queue in. Nothing survives a
/// restart, and another process sees nothing.
final class MemoryTransport extends PollingTransport {
    private final Map<String, LinkedList<Message>> queues =
            new HashMap<String, LinkedList<Message>>();
    private long ids;

    MemoryTransport(RetryPolicy retry) {
        super(retry);
    }

    @Override
    public String getName() {
        return "memory";
    }

    @Override
    public void send(Message message) {
        synchronized (queues) {
            LinkedList<Message> q = queues.get(message.getDestination());
            if (q == null) {
                q = new LinkedList<Message>();
                queues.put(message.getDestination(), q);
            }
            message.setId(Long.toString(++ids));
            q.addLast(message);
            queues.notifyAll();
        }
    }

    @Override
    protected List<Delivery> poll(String destination, String group, int max, long waitMillis)
            throws IOException {
        long deadline = System.currentTimeMillis() + waitMillis;
        List<Delivery> out = new ArrayList<Delivery>();
        synchronized (queues) {
            while (true) {
                long now = System.currentTimeMillis();
                long soonest = deadline;
                LinkedList<Message> q = queues.get(destination);
                if (q != null) {
                    java.util.Iterator<Message> it = q.iterator();
                    while (it.hasNext() && out.size() < max) {
                        Message m = it.next();
                        if (notYet(m, now)) {
                            soonest = Math.min(soonest,
                                    Long.parseLong(m.getHeader(Message.NOT_BEFORE)));
                            continue;
                        }
                        it.remove();
                        out.add(new Delivery(m, null));
                    }
                }
                if (!out.isEmpty() || now >= deadline) {
                    return out;
                }
                try {
                    queues.wait(Math.max(1, soonest - now));
                } catch (InterruptedException err) {
                    Thread.currentThread().interrupt();
                    return out;
                }
            }
        }
    }

    @Override
    protected void ack(String destination, String group, Delivery delivery) {
        // Taken off the queue when polled.
    }

    /// Messages waiting in `destination`, for tests and the dev tools.
    int depth(String destination) {
        synchronized (queues) {
            LinkedList<Message> q = queues.get(destination);
            return q == null ? 0 : q.size();
        }
    }

    @Override
    public void close() {
        stopSubscriptions();
    }
}
