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
package com.codename1.backend.mail;

import com.codename1.backend.Base64;
import com.codename1.backend.DataSource;
import com.codename1.backend.Database;
import com.codename1.backend.Transactions;
import com.codename1.backend.messaging.RetryPolicy;
import com.codename1.backend.sql.Dialect;
import com.codename1.impl.backend.BackendAccess;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/// The queue between a call to send and the transport: one thread that hands
/// mails over, tries again when that fails, and gives up on a mail the server
/// will never take.
///
/// With a data source the queue is the table `cn1_mail_outbox`, written in
/// the caller's transaction -- the transactional outbox the messaging support
/// uses, for the same reason. A rollback sends nothing. A commit never loses
/// the mail: a row whose send did not happen, because the process died or the
/// server was down, is sent by the sweep every server runs. A mail can
/// therefore be sent twice and never zero times.
///
/// A mail that fails for good -- the server refused it, or it used up its
/// attempts -- moves to `cn1_mail_dead` with the reason, where an operator
/// can read it; nothing sends from that table.
///
/// Without a data source the queue is memory: retried the same way, and gone
/// when the process stops.
///
/// Sending is on this thread and never on the caller's for a second reason
/// than the transaction: an SMTP exchange is several round trips, and a
/// request handler that waited for one would hold its thread for all of them.
final class MailQueue {
    static final String TABLE = "cn1_mail_outbox";
    static final String DEAD = "cn1_mail_dead";
    /// How long a row a sweep claimed is left alone by the other sweeps.
    /// Longer than a send can take -- a connection, a TLS handshake, a sign-in
    /// and a large body each wait up to the read timeout -- since a claim
    /// that ran out mid-send would let another server send the same mail.
    private static final long CLAIM = 600000;

    private final MailTransport transport;
    private final DataSource db;
    private final RetryPolicy retry;
    private final long sweepAfter;
    private final AtomicBoolean running = new AtomicBoolean(true);
    /// Set when [#close] has stopped waiting: the transport closes next, so
    /// what the thread still holds is left to the next start.
    private final AtomicBoolean abandoned = new AtomicBoolean();
    private final AtomicInteger depth = new AtomicInteger();
    private final Object pause = new Object();
    /// Mails waiting for the thread, each with the time it is due.
    private final List<Item> waiting = new ArrayList<Item>();
    /// The rows this process holds in [#waiting], which its sweep leaves alone.
    private final Set<Object> held = new HashSet<Object>();
    private boolean created;
    private Thread thread;

    private static final class Item {
        Object id;
        final String from;
        final String[] recipients;
        final byte[] mime;
        int attempt;
        long due;
        /// What the row's `next_attempt` holds, which is what a claim of it
        /// is conditional on; 0 once this process has claimed it.
        long stamp;

        Item(String from, String[] recipients, byte[] mime) {
            this.from = from;
            this.recipients = recipients;
            this.mime = mime;
        }
    }

    MailQueue(MailTransport transport, DataSource db, RetryPolicy retry, long sweepAfterMillis) {
        this.transport = transport;
        this.db = db;
        this.retry = retry;
        this.sweepAfter = Math.max(100, sweepAfterMillis);
        if (db != null) {
            // The tables are made here, not by the first send. MySQL and
            // MariaDB end the open transaction on any DDL, a CREATE TABLE IF
            // NOT EXISTS of a table that exists included: a first send inside
            // a transaction then wrote its row outside it, and a mail the
            // caller rolled back was sent by the sweep. A database that is
            // down now is tried again by the sweep, and by a send as a last
            // resort.
            try {
                ensureTables();
            } catch (IOException | RuntimeException err) {
                System.err.println("cn1: the mail outbox tables could not be created yet: "
                        + err.getMessage());
            }
            // Not on the first send: what a stopped server left in the table
            // is sent by the next one whether or not it has mail of its own.
            start();
        }
    }

    /// Mails accepted and not yet handed over or given up on, in this process.
    int depth() {
        return depth.get();
    }

    private void ensureTables() throws IOException {
        synchronized (this) {
            if (created) {
                return;
            }
        }
        Dialect d = db.dialect();
        String text = d.columnType(Dialect.TEXT);
        String big = d.columnType(Dialect.BIGINT);
        String number = d.columnType(Dialect.INTEGER);
        db.execute("CREATE TABLE IF NOT EXISTS " + TABLE + " (id "
                + d.generatedKeyColumn(Dialect.BIGINT)
                + ", mail_from VARCHAR(320) NOT NULL, recipients " + text + " NOT NULL, mime "
                + text + " NOT NULL, attempt " + number + " NOT NULL, next_attempt " + big
                + " NOT NULL, last_error VARCHAR(500))", new Object[0]);
        db.execute("CREATE TABLE IF NOT EXISTS " + DEAD + " (id "
                + d.generatedKeyColumn(Dialect.BIGINT)
                + ", mail_from VARCHAR(320) NOT NULL, recipients " + text + " NOT NULL, mime "
                + text + " NOT NULL, attempts " + number + " NOT NULL, error VARCHAR(500), died_at "
                + big + " NOT NULL)", new Object[0]);
        synchronized (this) {
            created = true;
        }
    }

    /// Takes a mail for delivery: now when no transaction is open, and when
    /// it commits otherwise.
    void submit(String from, String[] recipients, byte[] mime) throws IOException {
        if (db == null && !running.get()) {
            // Nothing would ever send it, and nothing keeps it.
            throw new MailException("The mail support is closed");
        }
        start();
        final Item item = new Item(from, recipients, mime);
        if (db != null) {
            ensureTables();
            // Due after a sweep age, so the row is the fallback and the
            // commit callback below is what normally sends it.
            item.stamp = System.currentTimeMillis() + sweepAfter;
            item.id = Long.valueOf(db.insert("INSERT INTO " + TABLE + " (mail_from, recipients,"
                    + " mime, attempt, next_attempt) VALUES (?, ?, ?, 0, ?)",
                    new Object[]{from, join(recipients), Base64.encode(mime),
                        Long.valueOf(item.stamp)}, "id"));
        }
        if (Transactions.isActive()) {
            Transactions.registerSynchronization(new Transactions.Synchronization() {
                @Override
                public void afterCommit() {
                    enqueue(item);
                }
            });
        } else {
            enqueue(item);
        }
    }

    private void enqueue(Item item) {
        if (db == null && !running.get()) {
            // A transaction that committed after the server stopped.
            System.err.println("cn1: a mail to " + item.recipients.length + " recipient(s) is "
                    + "lost: the mail support closed before its transaction committed");
            return;
        }
        synchronized (pause) {
            waiting.add(item);
            if (item.id != null) {
                held.add(item.id);
            }
            depth.incrementAndGet();
            pause.notifyAll();
        }
    }

    private static String join(String[] recipients) {
        StringBuilder sb = new StringBuilder();
        for (String r : recipients) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(r);
        }
        return sb.toString();
    }

    private static String[] split(String recipients) {
        List<String> out = new ArrayList<String>();
        int from = 0;
        while (from <= recipients.length()) {
            int end = recipients.indexOf('\n', from);
            if (end < 0) {
                end = recipients.length();
            }
            if (end > from) {
                out.add(recipients.substring(from, end));
            }
            from = end + 1;
        }
        return out.toArray(new String[out.size()]);
    }

    /// A failure as it may be logged: the server's words with every address
    /// in them removed. A mail server quotes the recipient back in its reply,
    /// and a log is not where a user's address belongs.
    static String redact(String message) {
        if (message == null) {
            return "no reason given";
        }
        if (message.indexOf('@') < 0) {
            return message;
        }
        StringBuilder sb = new StringBuilder(message.length());
        int from = 0;
        while (from <= message.length()) {
            int end = message.indexOf(' ', from);
            if (end < 0) {
                end = message.length();
            }
            String word = message.substring(from, end);
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(word.indexOf('@') >= 0 ? "<address>" : word);
            from = end + 1;
        }
        return sb.toString();
    }

    private static String reason(Throwable err) {
        String m = err.getMessage();
        if (m == null) {
            m = err.toString();
        }
        return m.length() > 500 ? m.substring(0, 500) : m;
    }

    /// Makes a claimed row due now, for the next start to send.
    private void giveBack(Item item) {
        try {
            db.execute("UPDATE " + TABLE + " SET next_attempt = ? WHERE id = ?",
                    new Object[]{Long.valueOf(System.currentTimeMillis()), item.id});
        } catch (IOException | RuntimeException err) {
            // Its claim runs out by itself.
            System.err.println("cn1: a mail waits in " + TABLE + " for its claim to run "
                    + "out: " + err.getMessage());
        }
    }

    private void deliver(Item item) {
        if (item.id != null && abandoned.get()) {
            // Its row is there for the next start. One the sweep claimed is
            // given back, or that start would wait out the claim.
            if (item.stamp == 0) {
                giveBack(item);
            }
            settle(item);
            return;
        }
        if (item.id != null && item.stamp != 0 && !claim(item)) {
            // Another server's.
            settle(item);
            return;
        }
        Throwable failure = null;
        try {
            MailMetrics.transmit(transport, item.from, item.recipients, item.mime);
        } catch (IOException err) {
            failure = err;
        } catch (RuntimeException err) {
            failure = err;
        }
        if (failure == null) {
            MailMetrics.sent();
            settle(item);
            if (item.id != null) {
                try {
                    db.execute("DELETE FROM " + TABLE + " WHERE id = ?", new Object[]{item.id});
                } catch (IOException | RuntimeException err) {
                    System.err.println("cn1: a sent mail stays in " + TABLE + " and will be "
                            + "sent again: " + err.getMessage());
                }
            }
            return;
        }
        boolean permanent = failure instanceof MailException
                && ((MailException) failure).isPermanent();
        if (item.id != null && abandoned.get() && !permanent) {
            // The transport closed under it. That is no attempt at sending:
            // the row is given back as it was, due now, for the next start.
            // A refusal that is final was the server's answer and is buried
            // below; sending that mail again could reach a recipient twice.
            giveBack(item);
            settle(item);
            return;
        }
        item.attempt++;
        if (permanent || item.attempt >= retry.getMaxAttempts()) {
            MailMetrics.failed(permanent);
            settle(item);
            bury(item, failure, permanent);
            return;
        }
        MailMetrics.retried();
        long delay = retry.delayAfter(item.attempt);
        item.due = System.currentTimeMillis() + delay;
        System.err.println("cn1: a mail to " + item.recipients.length + " recipient(s) was not "
                + "sent and will be tried again in " + delay + "ms (attempt " + item.attempt
                + " of " + retry.getMaxAttempts() + "): " + redact(reason(failure)));
        if (item.id != null) {
            try {
                db.execute("UPDATE " + TABLE + " SET attempt = ?, next_attempt = ?, last_error = ?"
                        + " WHERE id = ?", new Object[]{Integer.valueOf(item.attempt),
                            Long.valueOf(item.due + sweepAfter), reason(failure), item.id});
                item.stamp = item.due + sweepAfter;
            } catch (IOException | RuntimeException err) {
                // The row keeps this process's claim; the sweep that finds
                // it once the claim runs out carries on from there.
                System.err.println("cn1: " + TABLE + " could not record a failed attempt: "
                        + err.getMessage());
                settle(item);
                return;
            }
        }
        synchronized (pause) {
            // Still held, still counted: only its due time changed.
            waiting.add(item);
        }
    }

    /// Makes the row of `item` this process's to send, by moving its time
    /// forward from the one it was last given. Every server on the database
    /// sweeps the table, and the set of rows a process holds is its own: a
    /// row this process is slow to reach -- a backlog, a slow server -- comes
    /// due and is another sweep's to take. Whoever moves the time has it;
    /// false is a row somebody else moved, or one that is gone.
    private boolean claim(Item item) {
        try {
            long until = System.currentTimeMillis() + CLAIM;
            if (db.execute("UPDATE " + TABLE + " SET next_attempt = ? WHERE id = ? AND "
                    + "next_attempt = ?", new Object[]{Long.valueOf(until), item.id,
                        Long.valueOf(item.stamp)}) != 1) {
                return false;
            }
            item.stamp = 0;
            return true;
        } catch (IOException | RuntimeException err) {
            // The row is there for the sweep, which sends it when the
            // database answers again.
            System.err.println("cn1: a mail waits in " + TABLE + " for the next sweep: "
                    + err.getMessage());
            return false;
        }
    }

    private void settle(Item item) {
        depth.decrementAndGet();
        if (item.id != null) {
            synchronized (pause) {
                held.remove(item.id);
            }
        }
    }

    /// A row moved to the dead table, as the body of a transaction.
    private static final class Bury implements DataSource.Work {
        private final Object[] dead;
        private final Object id;

        Bury(Object[] dead, Object id) {
            this.dead = dead;
            this.id = id;
        }

        @Override
        public Object run(Database tx) throws Exception {
            tx.execute("INSERT INTO " + DEAD + " (mail_from, recipients, mime, attempts,"
                    + " error, died_at) VALUES (?, ?, ?, ?, ?, ?)", dead);
            tx.execute("DELETE FROM " + TABLE + " WHERE id = ?", new Object[]{id});
            return null;
        }
    }

    private void bury(Item item, Throwable failure, boolean permanent) {
        System.err.println("cn1: a mail to " + item.recipients.length + " recipient(s) was "
                + (permanent ? "refused" : "given up on after " + item.attempt + " attempts")
                + (item.id != null ? " and moved to " + DEAD : " and is lost") + ": "
                + redact(reason(failure)));
        if (item.id == null) {
            return;
        }
        final Object[] dead = {item.from, join(item.recipients), Base64.encode(item.mime),
            Integer.valueOf(item.attempt), reason(failure),
            Long.valueOf(System.currentTimeMillis())};
        final Object id = item.id;
        try {
            // Both or neither: a row copied and not removed would be copied
            // again by every sweep.
            db.inTransaction(new Bury(dead, id));
        } catch (Exception err) {
            System.err.println("cn1: a failed mail could not be moved to " + DEAD + ": "
                    + err.getMessage());
            // Marked as out of attempts where it is, so that the sweep that
            // finds it moves it and does not send it: a mail the server
            // refused may have reached some of its recipients already.
            try {
                db.execute("UPDATE " + TABLE + " SET attempt = ?, next_attempt = ?, last_error = ?"
                        + " WHERE id = ?", new Object[]{
                            Integer.valueOf(Math.max(item.attempt, retry.getMaxAttempts())),
                            Long.valueOf(System.currentTimeMillis() + sweepAfter),
                            reason(failure), id});
            } catch (IOException | RuntimeException again) {
                // The database is not answering. The row keeps its claim, and
                // is tried again when that runs out: sent twice, never lost.
                System.err.println("cn1: nor could it be marked as failed: "
                        + again.getMessage());
            }
        }
    }

    /// Sends every row that is due and that this process is not already
    /// holding; answers how many it tried.
    int sweep() throws IOException {
        ensureTables();
        long now = System.currentTimeMillis();
        List rows = db.query("SELECT id, mail_from, recipients, mime, attempt, next_attempt, last_error FROM "
                + TABLE + " WHERE next_attempt <= ? ORDER BY id " + db.dialect().limit(50, 0),
                new Object[]{Long.valueOf(now)});
        int tried = 0;
        for (Object o : rows) {
            Map row = (Map) o;
            Object id = number(row.get("id"));
            synchronized (pause) {
                if (held.contains(id)) {
                    continue;
                }
            }
            Object mime = row.get("mime");
            if (!(mime instanceof String)) {
                // Never an empty mail in its place, counted as sent.
                System.err.println("cn1: a row of " + TABLE + " has no mail that can be read "
                        + "and is left where it is");
                continue;
            }
            // Claimed by moving its time forward, so the sweep of another
            // process leaves it alone while this one sends it.
            if (db.execute("UPDATE " + TABLE + " SET next_attempt = ? WHERE id = ? AND "
                    + "next_attempt = ?", new Object[]{
                        // From now, not from when the sweep began: the rows
                        // before this one may each have taken a minute.
                        Long.valueOf(System.currentTimeMillis() + CLAIM), id,
                        row.get("next_attempt")}) != 1) {
                continue;
            }
            Item item = new Item(String.valueOf(row.get("mail_from")),
                    split(String.valueOf(row.get("recipients"))),
                    Base64.decode((String) mime));
            item.id = id;
            Object attempt = row.get("attempt");
            item.attempt = attempt instanceof Number ? ((Number) attempt).intValue() : 0;
            synchronized (pause) {
                held.add(id);
            }
            depth.incrementAndGet();
            if (item.attempt >= retry.getMaxAttempts()) {
                // Out of attempts already, and still here because the move
                // to the dead table failed: move it, do not send it again.
                // Counted as failed when it failed.
                settle(item);
                bury(item, new MailException(String.valueOf(row.get("last_error"))), false);
            } else {
                deliver(item);
            }
            tried++;
        }
        return tried;
    }

    /// A key as the set of held rows keeps it: an id read back from a query
    /// is not always the type the insert answered.
    private static Object number(Object id) {
        return id instanceof Number ? (Object) Long.valueOf(((Number) id).longValue()) : id;
    }

    private void start() {
        synchronized (this) {
            if (thread != null || !running.get()) {
                return;
            }
            thread = new Thread(BackendAccess.get().inServer(new Runnable() {
                @Override
                public void run() {
                    loop();
                }
            }), "cn1-mail");
            thread.setDaemon(true);
            thread.start();
        }
    }

    private void loop() {
        long nextSweep = 0;
        while (true) {
            long now = System.currentTimeMillis();
            List<Item> due = new ArrayList<Item>();
            synchronized (pause) {
                for (int i = waiting.size() - 1; i >= 0; i--) {
                    if (waiting.get(i).due <= now) {
                        due.add(0, waiting.remove(i));
                    }
                }
            }
            for (Item item : due) {
                deliver(item);
            }
            if (db != null && running.get() && System.currentTimeMillis() >= nextSweep) {
                try {
                    sweep();
                } catch (IOException | RuntimeException err) {
                    // A row it cannot read must not end the thread every
                    // committed mail waits on.
                    System.err.println("cn1: the mail sweep failed: " + err.getMessage());
                }
                nextSweep = System.currentTimeMillis() + sweepAfter;
            }
            synchronized (pause) {
                long wake = db != null ? nextSweep : Long.MAX_VALUE;
                boolean ready = false;
                now = System.currentTimeMillis();
                for (Item item : waiting) {
                    if (item.due <= now) {
                        ready = true;
                    }
                    wake = Math.min(wake, item.due);
                }
                if (!running.get() && !ready) {
                    // What is left is waiting out a retry delay. With a data
                    // source the row is still there for the next start.
                    if (db == null && !waiting.isEmpty()) {
                        System.err.println("cn1: " + waiting.size() + " mail(s) waiting to be "
                                + "tried again are lost: the server stopped and has no data "
                                + "source to keep them in");
                    }
                    return;
                }
                if (!ready) {
                    try {
                        long left = wake - now;
                        if (left > 0) {
                            pause.wait(left == Long.MAX_VALUE - now ? 0 : left);
                        }
                    } catch (InterruptedException err) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
    }

    /// Stops the thread once it has handed over what is due, waiting at most
    /// `waitMillis` for that.
    void close(long waitMillis) {
        running.set(false);
        Thread t;
        synchronized (this) {
            t = thread;
        }
        synchronized (pause) {
            pause.notifyAll();
        }
        if (t != null && t != Thread.currentThread()) { //NOPMD CompareObjectsWithEquals - thread identity
            try {
                t.join(waitMillis);
            } catch (InterruptedException err) {
                Thread.currentThread().interrupt();
            }
        }
        abandoned.set(true);
    }
}
