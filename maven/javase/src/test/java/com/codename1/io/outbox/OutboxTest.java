/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.io.outbox;

import com.codename1.impl.javase.SEDatabase;
import com.codename1.orm.EntityManager;
import com.codename1.orm.session.PersistenceException;
import com.codename1.orm.session.Session;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OutboxTest {
    private EntityManager manager;
    private Session session;

    @BeforeEach
    void open() throws Exception {
        Class.forName("org.sqlite.JDBC");
        manager = EntityManager.open(new SEDatabase(java.sql.DriverManager.getConnection("jdbc:sqlite::memory:")));
        session = manager.openSession();
    }

    @AfterEach
    void close() throws IOException {
        session.close();
        manager.close();
    }

    private static byte[] bytes(String s) {
        return s.getBytes();
    }

    /// Records every delivery and answers from a script; past its end, DELIVERED.
    static final class Recorder implements Outbox.Transport {
        final List<String> seen = new ArrayList<String>();
        final int[] answers;
        int next;

        Recorder(int... answers) {
            this.answers = answers;
        }

        @Override
        public int deliver(OutboxMessage m) throws IOException {
            seen.add(m.getKey() + " " + m.getTopic() + " " + new String(m.getPayload()) + " #" + m.getAttempts());
            int answer = next < answers.length ? answers[next] : Outbox.DELIVERED;
            next++;
            if (answer < 0) {
                throw new IOException("link down");
            }
            return answer;
        }
    }

    @Test
    void deliversInOrderAndRemoves() {
        Outbox box = Outbox.open(session, "dev1");
        OutboxMessage a = box.enqueue("/a", bytes("one"));
        box.enqueue("/b", bytes("two"));
        assertEquals("dev1/" + a.getId(), a.getKey());
        assertEquals(2, box.size());
        Recorder r = new Recorder();
        assertEquals(2, box.flush(r));
        assertEquals("dev1/" + a.getId() + " /a one #0", r.seen.get(0));
        assertTrue(r.seen.get(1).endsWith(" /b two #0"));
        assertEquals(0, box.size());
        assertEquals(-1, box.nextAttemptTime());
        assertEquals(0, box.flush(r));
    }

    @Test
    void retryHoldsTheHeadAndBacksOff() {
        Outbox box = Outbox.open(session, "dev1");
        box.setBackoff(60000, 120000);
        box.enqueue("/a", bytes("one"));
        box.enqueue("/b", bytes("two"));
        Recorder r = new Recorder(-1);
        long before = System.currentTimeMillis();
        assertEquals(0, box.flush(r));
        assertEquals(1, r.seen.size());
        assertTrue(box.nextAttemptTime() >= before + 60000);
        assertEquals(0, box.flush(r), "the head waits out its backoff");
        assertEquals(1, r.seen.size());

        Outbox quick = Outbox.open(session, "dev2");
        quick.setBackoff(0, 0);
        quick.enqueue("/c", bytes("three"));
        assertEquals(0, quick.flush(new Recorder(Outbox.RETRY)));
        assertEquals(0, quick.flush(new Recorder(-1)));
        Recorder ok = new Recorder();
        assertEquals(1, quick.flush(ok));
        assertTrue(ok.seen.get(0).endsWith(" /c three #2"), ok.seen.get(0));
    }

    @Test
    void rejectedAndExhaustedMessagesAreDropped() {
        Outbox box = Outbox.open(session, "dev1");
        box.setBackoff(0, 0);
        box.setMaxAttempts(2);
        box.enqueue("/bad", bytes("x"));
        box.enqueue("/flaky", bytes("y"));
        box.enqueue("/good", bytes("z"));
        Recorder r = new Recorder(Outbox.REJECTED, Outbox.RETRY);
        assertEquals(0, box.flush(r));
        assertEquals(1, box.getRejected());
        assertEquals(1, box.flush(new Recorder(Outbox.RETRY)), "second failure exhausts /flaky");
        assertEquals(2, box.getRejected());
        assertEquals(0, box.size());
    }

    @Test
    void enqueueBelongsToTheCallersTransaction() {
        Outbox box = Outbox.open(session, "dev1");
        session.beginTransaction();
        box.enqueue("/a", bytes("lost"));
        session.rollbackTransaction();
        assertEquals(0, box.size());
        session.beginTransaction();
        box.enqueue("/a", bytes("kept"));
        session.commitTransaction();
        assertEquals(1, box.size());
        session.beginTransaction();
        try {
            assertThrows(IllegalStateException.class, () -> box.flush(new Recorder()));
        } finally {
            session.rollbackTransaction();
        }
    }

    @Test
    void capacityDropsOldestOrRefuses() {
        Outbox box = Outbox.open(session, "dev1");
        box.setCapacity(2, true);
        box.enqueue("/1", bytes("1"));
        box.enqueue("/2", bytes("2"));
        box.enqueue("/3", bytes("3"));
        assertEquals(2, box.size());
        assertEquals(1, box.getRejected());
        Recorder r = new Recorder();
        box.flush(r);
        assertTrue(r.seen.get(0).endsWith(" /2 2 #0"), r.seen.get(0));

        box.setCapacity(1, false);
        box.enqueue("/4", bytes("4"));
        assertThrows(PersistenceException.class, () -> box.enqueue("/5", bytes("5")));
        assertEquals(1, box.size());
    }

    @Test
    void outboxesOfDifferentNamesAreSeparate() {
        Outbox a = Outbox.open(session, "a");
        Outbox b = Outbox.open(session, "b");
        a.enqueue("/x", bytes("from a"));
        assertEquals(0, b.size());
        Recorder r = new Recorder();
        assertEquals(0, b.flush(r));
        assertEquals(1, a.flush(r));
        assertTrue(r.seen.get(0).startsWith("a/"));
    }

    @Test
    void enqueuedPayloadIsCopied() {
        Outbox box = Outbox.open(session, "dev1");
        byte[] payload = bytes("abc");
        box.enqueue("/a", payload);
        payload[0] = 'X';
        Recorder r = new Recorder();
        box.flush(r);
        assertTrue(r.seen.get(0).endsWith(" /a abc #0"));
    }
}
