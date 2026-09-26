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
package com.codename1.backend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.codename1.backend.sql.Dialect;

/**
 * Runs the {@code @Scheduled} methods of a server.
 *
 * <p>The build registers each job from the entry point it generates -- the cron
 * masks already computed, the executor and thread kind already chosen -- and
 * starts this once the server is accepting. One platform thread keeps the
 * timetable; the jobs themselves run on their executors, so a slow job delays
 * nothing but its own next run.
 *
 * <p>A run never overlaps the previous run of the same job. A fire that finds
 * the previous run still going is skipped, and the job is next due at its
 * following time, which is what keeps a job that has fallen behind from
 * stacking up copies of itself.
 *
 * <p>With a {@code lock}, a run first claims a row in {@code cn1_scheduler_lock}
 * and is skipped when another instance holds it, so a job runs on one replica
 * at a time. The claim expires after {@code lockAtMostFor} milliseconds, so a
 * replica that dies mid-run does not keep it.
 */
public final class Scheduler {
    public static final int CRON = 0;
    public static final int FIXED_RATE = 1;
    public static final int FIXED_DELAY = 2;

    private static final String LOCK_TABLE = "cn1_scheduler_lock";
    private static final long DEFAULT_LOCK_MILLIS = 10 * 60 * 1000L;

    /** One registered job and what has happened to it. */
    public static final class Job {
        final String name;
        final int kind;
        final CronSchedule cron;
        final long period;
        final long initialDelay;
        final String executorName;
        final int threadKind;
        final String lock;
        final long lockAtMostFor;
        final Runnable body;
        TaskExecutor executor;
        long next = Long.MAX_VALUE;
        boolean running;
        long runs;
        long failures;
        long skipped;
        long lastStart;
        long lastDurationMillis = -1;
        String lastError;

        Job(String name, int kind, CronSchedule cron, long period, long initialDelay,
            String executorName, int threadKind, String lock, long lockAtMostFor,
            Runnable body) {
            this.name = name;
            this.kind = kind;
            this.cron = cron;
            this.period = period;
            this.initialDelay = initialDelay;
            // Named by kind when unnamed, for the reason Tasks.executor gives.
            this.executorName = executorName == null || executorName.length() == 0
                    ? (threadKind == Tasks.VIRTUAL ? Tasks.SCHEDULING + "-virtual"
                            : Tasks.SCHEDULING) : executorName;
            this.threadKind = threadKind;
            this.lock = lock == null || lock.length() == 0 ? null : lock;
            this.lockAtMostFor = lockAtMostFor > 0 ? lockAtMostFor : DEFAULT_LOCK_MILLIS;
            this.body = body;
        }

        public String getName() {
            return name;
        }

        /** What the schedule is, for a listing. */
        public String describeSchedule() {
            if(kind == CRON) {
                return "cron " + cron;
            }
            return (kind == FIXED_RATE ? "every " : "delay ") + period + "ms";
        }
    }

    private final List jobs = new ArrayList();
    private final DataSource locks;
    private final String instance;
    private Thread thread;
    private boolean started;
    private boolean stopped;
    private boolean lockTableReady;

    /**
     * @param locks the pool locked jobs claim their row in, or null when this
     *        server has no database -- the build refuses a lock without one
     */
    public Scheduler(DataSource locks) {
        this.locks = locks;
        String id;
        try {
            id = hex(Crypto.randomBytes(6));
        } catch (IOException err) {
            id = Long.toString(System.currentTimeMillis(), 16);
        }
        this.instance = id;
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for(int iter = 0 ; iter < bytes.length ; iter++) {
            sb.append("0123456789abcdef".charAt((bytes[iter] >> 4) & 15));
            sb.append("0123456789abcdef".charAt(bytes[iter] & 15));
        }
        return sb.toString();
    }

    /** Registers a cron job. Generated code calls this before {@link #start}. */
    public synchronized void cron(String name, CronSchedule schedule, String executor,
                                  int threadKind, String lock, long lockAtMostFor,
                                  Runnable body) {
        add(new Job(name, CRON, schedule, 0, 0, executor, threadKind, lock, lockAtMostFor,
                body));
    }

    /** Registers a job that starts every {@code period} milliseconds. */
    public synchronized void fixedRate(String name, long initialDelay, long period,
                                       String executor, int threadKind, String lock,
                                       long lockAtMostFor, Runnable body) {
        checkPeriod(name, period);
        add(new Job(name, FIXED_RATE, null, period, initialDelay, executor, threadKind, lock,
                lockAtMostFor, body));
    }

    /** Registers a job that starts {@code period} milliseconds after the last run ended. */
    public synchronized void fixedDelay(String name, long initialDelay, long period,
                                        String executor, int threadKind, String lock,
                                        long lockAtMostFor, Runnable body) {
        checkPeriod(name, period);
        add(new Job(name, FIXED_DELAY, null, period, initialDelay, executor, threadKind, lock,
                lockAtMostFor, body));
    }

    private static void checkPeriod(String name, long period) {
        if(period <= 0) {
            // Only a value read from configuration can be wrong here: the build
            // checks the literal ones.
            throw new IllegalArgumentException("Scheduled job " + name + " has period "
                    + period + "ms; it must be positive");
        }
    }

    private void add(Job job) {
        if(started) {
            throw new IllegalStateException("Jobs are registered before the scheduler starts");
        }
        if(job.lock != null && locks == null) {
            throw new IllegalStateException("Scheduled job " + job.name + " takes lock \""
                    + job.lock + "\", which needs a database, and this server has none");
        }
        jobs.add(job);
    }

    /** Starts the timetable. */
    public synchronized void start() {
        if(started) {
            return;
        }
        started = true;
        long now = System.currentTimeMillis();
        for(int iter = 0 ; iter < jobs.size() ; iter++) {
            Job job = (Job)jobs.get(iter);
            job.executor = Tasks.executor(job.executorName, job.threadKind);
            if(job.kind == CRON) {
                job.next = job.cron.next(now);
                if(job.next < 0) {
                    job.next = Long.MAX_VALUE;
                    System.err.println("Scheduled job " + job.name + " never fires: "
                            + job.cron);
                }
            } else {
                job.next = now + Math.max(0, job.initialDelay);
            }
        }
        if(jobs.isEmpty()) {
            return;
        }
        thread = new Thread(new Runnable() {
            public void run() {
                loop();
            }
        }, "cn1-scheduler");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Stops starting runs and waits up to {@code waitMillis} for the ones in
     * progress.
     */
    public void stop(long waitMillis) {
        synchronized(this) {
            stopped = true;
            notifyAll();
        }
        long deadline = System.currentTimeMillis() + Math.max(0, waitMillis);
        synchronized(this) {
            while(anyRunning()) {
                long left = deadline - System.currentTimeMillis();
                if(left <= 0) {
                    break;
                }
                try {
                    wait(left);
                } catch (InterruptedException err) {
                    break;
                }
            }
        }
    }

    private boolean anyRunning() {
        for(int iter = 0 ; iter < jobs.size() ; iter++) {
            if(((Job)jobs.get(iter)).running) {
                return true;
            }
        }
        return false;
    }

    private void loop() {
        synchronized(this) {
            while(!stopped) {
                long now = System.currentTimeMillis();
                Job due = null;
                long soonest = Long.MAX_VALUE;
                for(int iter = 0 ; iter < jobs.size() ; iter++) {
                    Job job = (Job)jobs.get(iter);
                    if(job.next < soonest) {
                        soonest = job.next;
                        due = job;
                    }
                }
                if(due == null || soonest > now) {
                    try {
                        if(due == null) {
                            wait();
                        } else {
                            wait(Math.min(soonest - now, 60000L));
                        }
                    } catch (InterruptedException err) {
                        return;
                    }
                    continue;
                }
                fire(due, now);
            }
        }
    }

    /** Called holding the monitor. */
    private void fire(final Job job, long now) {
        if(job.running) {
            job.skipped++;
            job.next = following(job, now);
            return;
        }
        job.running = true;
        job.next = job.kind == FIXED_DELAY ? Long.MAX_VALUE : following(job, now);
        try {
            job.executor.execute(new Runnable() {
                public void run() {
                    runJob(job);
                }
            });
        } catch (RuntimeException err) {
            job.running = false;
            job.failures++;
            job.lastError = String.valueOf(err);
            if(job.kind == FIXED_DELAY) {
                job.next = now + job.period;
            }
        }
    }

    private long following(Job job, long now) {
        if(job.kind == CRON) {
            long next = job.cron.next(now);
            return next < 0 ? Long.MAX_VALUE : next;
        }
        if(job.kind == FIXED_RATE) {
            long next = job.next;
            if(next == Long.MAX_VALUE) {
                next = now;
            }
            // Catch up without a burst: the next START after now on the rate's
            // own grid.
            while(next <= now) {
                next += job.period;
            }
            return next;
        }
        return now + job.period;
    }

    private void runJob(final Job job) {
        long start = System.currentTimeMillis();
        String error = null;
        boolean ran = false;
        try {
            if(job.lock == null || claim(job, start)) {
                ran = true;
                Tracing.inBackground("scheduled " + job.name, null, new Tracing.Work() {
                    public Object run(Span span) throws Exception {
                        job.body.run();
                        return null;
                    }
                });
            }
        } catch (Throwable err) {
            error = String.valueOf(err);
            System.err.println("Scheduled job " + job.name + " failed: " + err);
        } finally {
            if(ran && job.lock != null) {
                release(job);
            }
            long end = System.currentTimeMillis();
            if(ran) {
                com.codename1.backend.metrics.Metrics.jobRan(job.name, end - start,
                        error != null);
            }
            synchronized(this) {
                job.running = false;
                if(ran) {
                    job.runs++;
                    job.lastStart = start;
                    job.lastDurationMillis = end - start;
                } else if(error == null) {
                    job.skipped++;
                }
                if(error != null) {
                    job.failures++;
                    job.lastError = error;
                }
                if(job.kind == FIXED_DELAY && !stopped) {
                    job.next = end + job.period;
                }
                notifyAll();
            }
        }
    }

    private boolean claim(Job job, long now) throws IOException {
        prepareLockTable();
        long until = now + job.lockAtMostFor;
        int updated = locks.execute("UPDATE " + LOCK_TABLE + " SET lock_until = ?, locked_at = ?, "
                + "locked_by = ? WHERE name = ? AND lock_until <= ?",
                new Object[] {new Long(until), new Long(now), instance, job.lock, new Long(now)});
        if(updated > 0) {
            return true;
        }
        try {
            locks.execute("INSERT INTO " + LOCK_TABLE + " (name, lock_until, locked_at, "
                    + "locked_by) VALUES (?, ?, ?, ?)",
                    new Object[] {job.lock, new Long(until), new Long(now), instance});
            return true;
        } catch (IOException failed) {
            // Only a row that exists means another instance holds the claim.
            // Anything else -- a dropped connection, a missing permission -- is
            // rethrown so runJob records it as a failure; read as "held", it
            // would be counted as a skip and the job would silently never run.
            Map row;
            try {
                row = locks.queryOne("SELECT locked_by FROM " + LOCK_TABLE + " WHERE name = ?",
                        new Object[] {job.lock});
            } catch (IOException err) {
                throw failed;
            }
            if(row != null) {
                return false;
            }
            throw failed;
        }
    }

    private void release(Job job) {
        try {
            locks.execute("UPDATE " + LOCK_TABLE + " SET lock_until = ? WHERE name = ? AND "
                    + "locked_by = ?", new Object[] {new Long(System.currentTimeMillis()),
                    job.lock, instance});
        } catch (IOException err) {
            // The claim expires by itself; the only cost is that the next run on
            // another instance waits for it.
            System.err.println("Could not release scheduler lock " + job.lock + ": " + err);
        }
    }

    private synchronized void prepareLockTable() throws IOException {
        if(lockTableReady) {
            return;
        }
        Dialect d = locks.dialect();
        locks.execute("CREATE TABLE IF NOT EXISTS " + LOCK_TABLE + " (name "
                + d.assignedKeyColumn(Dialect.TEXT) + ", lock_until "
                + d.columnType(Dialect.BIGINT) + " NOT NULL, locked_at "
                + d.columnType(Dialect.BIGINT) + " NOT NULL, locked_by "
                + d.columnType(Dialect.TEXT) + ")", null);
        lockTableReady = true;
    }

    /**
     * Runs a job now, on its executor, whatever its schedule says. Answers
     * false when no job has that name or its previous run is still going.
     */
    public synchronized boolean trigger(String name) {
        for(int iter = 0 ; iter < jobs.size() ; iter++) {
            final Job job = (Job)jobs.get(iter);
            if(job.name.equals(name)) {
                if(job.running || job.executor == null) {
                    return false;
                }
                job.running = true;
                job.executor.execute(new Runnable() {
                    public void run() {
                        runJob(job);
                    }
                });
                return true;
            }
        }
        return false;
    }

    /** Every job and what has happened to it, for the management endpoint and MCP. */
    public synchronized List describe() {
        List out = new ArrayList();
        for(int iter = 0 ; iter < jobs.size() ; iter++) {
            Job job = (Job)jobs.get(iter);
            Map m = new LinkedHashMap();
            m.put("name", job.name);
            m.put("schedule", job.describeSchedule());
            m.put("executor", job.executorName);
            if(job.lock != null) {
                m.put("lock", job.lock);
            }
            m.put("running", Boolean.valueOf(job.running));
            m.put("runs", new Long(job.runs));
            m.put("failures", new Long(job.failures));
            m.put("skipped", new Long(job.skipped));
            if(job.next != Long.MAX_VALUE) {
                m.put("nextRunEpochMillis", new Long(job.next));
            }
            if(job.lastStart != 0) {
                m.put("lastRunEpochMillis", new Long(job.lastStart));
                m.put("lastDurationMillis", new Long(job.lastDurationMillis));
            }
            if(job.lastError != null) {
                m.put("lastError", job.lastError);
            }
            out.add(m);
        }
        return out;
    }

    /** The registered jobs. */
    public synchronized List getJobs() {
        return new ArrayList(jobs);
    }
}
