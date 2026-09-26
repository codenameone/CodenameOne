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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Where background work runs: the named {@link TaskExecutor}s, and two
 * shorthands for running something once.
 *
 * <pre>
 *   Tasks.platform(new Runnable() { public void run() { rebuildIndex(); } });
 *   Tasks.virtual(new Runnable() { public void run() { pingWebhooks(); } });
 * </pre>
 *
 * <p>The build's generated code asks for an executor once per {@code @Async}
 * method and keeps it, so this map is read once per method, not once per call.
 *
 * <h2>Which thread</h2>
 *
 * <p>A virtual thread is the cheaper one, and the right one for work that waits
 * on sockets. It is the wrong one for work that talks to a database: on this
 * runtime a database read blocks the HOST thread under the virtual thread, and
 * every other virtual thread on that host with it. See
 * {@code com.codename1.backend.annotations.ThreadKind}.
 */
public final class Tasks {
    public static final int AUTO = 0;
    public static final int VIRTUAL = 1;
    public static final int PLATFORM = 2;

    /** The executor {@code @Async} methods use when they name none. */
    public static final String DEFAULT = "default";
    /** The executor scheduled jobs use when they name none. */
    public static final String SCHEDULING = "scheduling";

    private static final Map EXECUTORS = new LinkedHashMap();
    private static Config config;
    private static boolean reportedAuto;

    private Tasks() {
    }

    /**
     * The configuration executors are sized from. Set by the server when it
     * starts; before that -- a test calling an {@code @Async} method directly --
     * executors take their defaults.
     */
    public static synchronized void configure(Config configuration) {
        config = configuration;
    }

    /**
     * The executor called {@code name}, created on first use.
     *
     * @param kind {@link #AUTO}, {@link #VIRTUAL} or {@link #PLATFORM}: what the
     *        code asked for, which {@code cn1.task.executor.<name>.kind}
     *        overrides
     */
    public static synchronized TaskExecutor executor(String name, int kind) {
        // An unnamed executor is named by the kind of thread asked for, so an
        // @Async(thread = VIRTUAL) method never lands on the platform pool an
        // unmarked method created first -- they would otherwise share "default".
        String key = name == null || name.length() == 0
                ? (kind == VIRTUAL ? DEFAULT + "-virtual" : DEFAULT) : name;
        TaskExecutor existing = (TaskExecutor)EXECUTORS.get(key);
        if(existing != null) {
            return existing;
        }
        String prefix = "cn1.task.executor." + key + ".";
        int threads = SCHEDULING.equals(key) ? 2 : 8;
        String configured = null;
        if(config != null) {
            try {
                threads = config.getInt(prefix + "threads", threads);
                configured = config.get(prefix + "kind");
            } catch (java.io.IOException err) {
                throw new IllegalStateException("Executor " + key + " cannot be configured: "
                        + err.getMessage());
            }
        }
        boolean virtual;
        if("virtual".equalsIgnoreCase(configured)) {
            virtual = true;
        } else if("platform".equalsIgnoreCase(configured)) {
            virtual = false;
        } else if(kind == AUTO) {
            virtual = HttpServer.acceptsVirtualTasks();
            if(!reportedAuto) {
                reportedAuto = true;
                System.out.println("cn1: background tasks marked AUTO run on "
                        + (virtual ? "virtual" : "platform") + " threads");
            }
        } else {
            virtual = kind == VIRTUAL;
        }
        TaskExecutor created = new TaskExecutor(key, virtual, threads);
        EXECUTORS.put(key, created);
        return created;
    }

    /** Runs {@code task} once on a virtual thread, or a platform one where there are none. */
    public static void virtual(Runnable task) {
        executor(null, VIRTUAL).execute(task);
    }

    /** Runs {@code task} once on the default pool of platform threads. */
    public static void platform(Runnable task) {
        executor(null, PLATFORM).execute(task);
    }

    /**
     * The body of a background task's virtual thread: the native entry point
     * calls this with the token the task was queued under. Nothing in Java calls
     * it, which is why the native source names it -- that keeps it alive through
     * dead-code elimination.
     */
    static void runVirtual(long token) {
        Runnable task = HttpServer.takeVirtualTask(token);
        if(task == null) {
            return;
        }
        try {
            task.run();
        } catch (Throwable err) {
            // The top of a virtual thread: nothing above this can catch it.
            System.err.println("A virtual-thread task failed: " + err);
        }
    }

    /** Every executor created so far, for a listing. */
    public static synchronized List executors() {
        return new ArrayList(EXECUTORS.values());
    }

    /**
     * Stops every executor, waiting up to {@code waitMillis} in total for what
     * is running, and forgets them, so a server started again in the same
     * process -- a test -- gets fresh ones.
     */
    public static void shutdown(long waitMillis) {
        List all;
        synchronized(Tasks.class) {
            all = new ArrayList(EXECUTORS.values());
            EXECUTORS.clear();
        }
        long deadline = System.currentTimeMillis() + Math.max(0, waitMillis);
        for(int iter = 0 ; iter < all.size() ; iter++) {
            long left = deadline - System.currentTimeMillis();
            ((TaskExecutor)all.get(iter)).shutdown(left > 0 ? left : 0);
        }
    }
}
