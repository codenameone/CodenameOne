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
package com.codename1.backend.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.impl.orm.Attribute;
import com.codename1.impl.orm.EntityModel;
import com.codename1.impl.orm.Models;
import com.codename1.orm.session.OptimisticLockException;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

public class DocumentSessionTest {
    public static final class Note {
        String id;
        Long version;
        String title;
        long stars;
        boolean done;
        Date due;
        double score;
    }

    static final class NoteModel extends EntityModel<Note> {
        private final String table;
        NoteModel(String table) {
            this.table = table;
        }
        private static final Attribute[] ATTRS = {
            new Attribute("id", "id", Attribute.TEXT, true, true, false, false),
            new Attribute("version", "version", Attribute.BIGINT, false, false, true, true),
            new Attribute("title", "title", Attribute.TEXT, false, false, true, false),
            new Attribute("stars", "stars", Attribute.BIGINT, false, false, false, false),
            new Attribute("done", "done", Attribute.BOOLEAN, false, false, false, false),
            new Attribute("due", "due_at", Attribute.TIMESTAMP, false, false, true, false),
            new Attribute("score", "score", Attribute.REAL, false, false, false, false)
        };
        public Class<Note> type() { return Note.class; }
        public String table() { return table; }
        public Attribute[] attributes() { return ATTRS; }
        public Note create() { return new Note(); }
        public Object get(Note n, int i) {
            switch (i) {
                case 0: return n.id;
                case 1: return n.version;
                case 2: return n.title;
                case 3: return Long.valueOf(n.stars);
                case 4: return Boolean.valueOf(n.done);
                case 5: return n.due;
                default: return Double.valueOf(n.score);
            }
        }
        public void set(Note n, int i, Object v) {
            switch (i) {
                case 0: n.id = (String) v; break;
                case 1: n.version = (Long) v; break;
                case 2: n.title = (String) v; break;
                case 3: n.stars = ((Number) v).longValue(); break;
                case 4: n.done = ((Number) v).longValue() != 0; break;
                case 5: n.due = v == null ? null : new Date(((Number) v).longValue()); break;
                default: n.score = ((Number) v).doubleValue(); break;
            }
        }
    }

    public static final class Message {
        String channel;
        long sentAt;
        String text;
    }

    /// Partition key `channel`, sort key `sentAt`, declared in the opposite
    /// order so the session has to read the @SortKey to tell them apart.
    static final class MessageModel extends EntityModel<Message> {
        private final String table;
        MessageModel(String table) {
            this.table = table;
        }
        private static final Attribute[] ATTRS = {
            new Attribute("sentAt", "sent_at", Attribute.BIGINT, true, false, false, false),
            new Attribute("channel", "channel", Attribute.TEXT, true, false, false, false),
            new Attribute("text", "text", Attribute.TEXT, false, false, true, false)
        };
        public Class<Message> type() { return Message.class; }
        public String table() { return table; }
        public Attribute[] attributes() { return ATTRS; }
        public int sortKeyIndex() { return 0; }
        public Message create() { return new Message(); }
        public Object get(Message m, int i) {
            return i == 0 ? Long.valueOf(m.sentAt) : i == 1 ? m.channel : m.text;
        }
        public void set(Message m, int i, Object v) {
            if (i == 0) {
                m.sentAt = ((Number) v).longValue();
            } else if (i == 1) {
                m.channel = (String) v;
            } else {
                m.text = (String) v;
            }
        }
    }

    private static void sortKeyScenario(DocumentSession s) throws Exception {
        Models.register(new MessageModel("cn1_msgs_" + Long.toHexString(System.nanoTime())));
        s.createCollections(Message.class);
        for (int i = 0; i < 10; i++) {
            Message m = new Message();
            m.channel = i % 2 == 0 ? "general" : "random";
            m.sentAt = 1000 + i;
            m.text = "hello " + i;
            s.persist(m);
        }
        Message dup = new Message();
        dup.channel = "general";
        dup.sentAt = 1000;
        assertThrows(com.codename1.orm.session.PersistenceException.class, () -> s.persist(dup));
        // The partition alone, a range of the sort key, and a non-key filter.
        assertEquals(5, s.query(Message.class).eq("channel", "general").count());
        List<Message> range = s.query(Message.class).eq("channel", "general").ge("sentAt", 1004L)
                .orderBy("sentAt", false).list();
        assertEquals(3, range.size());
        assertEquals(1008, range.get(0).sentAt);
        assertEquals(1, s.query(Message.class).eq("channel", "random")
                .eq("text", "hello 3").count());
        // A second condition on the sort key is answered in memory.
        assertEquals(3, s.query(Message.class).eq("channel", "random").gt("sentAt", 1002L)
                .lt("sentAt", 1009L).count());
        // The whole key: a point read, by array and through the session.
        assertEquals(1, s.query(Message.class).eq("channel", "random").eq("sentAt", 1005L).count());
        Message one = s.find(Message.class, new Object[] {Long.valueOf(1005), "random"});
        assertEquals("hello 5", one.text);
        one.text = "edited";
        s.merge(one);
        assertEquals("edited", s.find(Message.class, new Object[] {Long.valueOf(1005), "random"}).text);
        // No partition key: a scan.
        assertEquals(4, s.query(Message.class).gt("sentAt", 1005L).count());
        assertTrue(s.remove(one));
        assertNull(s.find(Message.class, new Object[] {Long.valueOf(1005), "random"}));
        assertEquals(4, s.query(Message.class).eq("channel", "random").count());
    }

    private static void scenario(DocumentSession s) throws Exception {
        Models.register(new NoteModel("cn1_notes_" + Long.toHexString(System.nanoTime())));
        s.createCollections(Note.class);
        for (int i = 0; i < 12; i++) {
            Note n = new Note();
            n.title = "note " + i + (i % 3 == 0 ? " urgent" : "");
            n.stars = i;
            n.done = i % 2 == 0;
            n.due = i < 6 ? new Date(1700000000000L + i * 1000L) : null;
            n.score = i / 4.0;
            s.persist(n);
            assertNotNull(n.id);
            assertEquals(Long.valueOf(0), n.version);
        }
        assertEquals(12, s.query(Note.class).count());
        assertEquals(6, s.query(Note.class).eq("done", Boolean.TRUE).count());
        assertEquals(6, s.query(Note.class).isNull("due").count());
        assertEquals(6, s.query(Note.class).isNotNull("due").count());
        assertEquals(4, s.query(Note.class).like("title", "%urgent").count());
        assertEquals(3, s.query(Note.class).in("stars", 1L, 2L, 99L, 11L).count());
        assertEquals(0, s.query(Note.class).in("stars").count());
        List<Note> top = s.query(Note.class).ge("stars", 3L).lt("stars", 9L)
                .orderBy("stars", false).offset(1).limit(2).list();
        assertEquals(2, top.size());
        assertEquals(7, top.get(0).stars);
        assertEquals(6, top.get(1).stars);
        assertEquals(1.75, top.get(0).score, 0);
        assertEquals(2, s.query(Note.class).gt("due", new Date(1700000003000L)).count());

        Note first = s.query(Note.class).eq("stars", 4L).first();
        Note again = s.find(Note.class, first.id);
        assertEquals("note 4", again.title);
        assertTrue(again.done);
        assertEquals(1700000004000L, again.due.getTime());
        Note stale = s.find(Note.class, first.id);
        again.title = "renamed";
        s.merge(again);
        assertEquals(Long.valueOf(1), again.version);
        assertEquals("renamed", s.find(Note.class, first.id).title);
        stale.title = "lost";
        assertThrows(OptimisticLockException.class, () -> s.merge(stale));
        assertThrows(OptimisticLockException.class, () -> s.remove(stale));
        assertTrue(s.remove(again));
        assertNull(s.find(Note.class, first.id));
        assertEquals(11, s.query(Note.class).count());
        assertThrows(UnsupportedOperationException.class, () -> s.query(Note.class).join("x"));
        assertFalse(s.query(Note.class).eq("id", "missing").count() > 0);
    }

    @Test
    public void mongo() throws Exception {
        String uri = System.getenv("CN1_TEST_MONGODB_URI");
        Assumptions.assumeTrue(uri != null && uri.length() > 0, "CN1_TEST_MONGODB_URI unset");
        try (DocumentSession s = DocumentSession.open(uri)) {
            scenario(s);
            sortKeyScenario(s);
        }
    }

    @Test
    public void dynamo() throws Exception {
        String endpoint = System.getenv("CN1_TEST_DYNAMODB_ENDPOINT");
        Assumptions.assumeTrue(endpoint != null && endpoint.length() > 0,
                "CN1_TEST_DYNAMODB_ENDPOINT unset");
        try (DocumentSession s = DocumentSession.open("dynamodb://us-east-1?endpoint=" + endpoint)) {
            scenario(s);
            sortKeyScenario(s);
        }
    }

    @Test
    public void likeMatcher() {
        assertTrue(Criterion.like("note 3 urgent", "%urgent"));
        assertTrue(Criterion.like("a_b", "a_b"));
        assertFalse(Criterion.like("ab", "a_b"));
    }
}
