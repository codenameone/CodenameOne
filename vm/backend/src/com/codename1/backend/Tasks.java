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

/// Where background work runs: the named [TaskExecutor]s, and two
/// shorthands for running something once.
///
/// ```java
///   Tasks.platform(new Runnable() { public void run() { rebuildIndex(); } });
///   Tasks.virtual(new Runnable() { public void run() { pingWebhooks(); } });
/// ```
///
/// ## Whose executors
///
/// Every server has its own executors, sized from its own configuration and
/// stopped with it: two servers in one process -- or one stopped and started
/// again -- must not share a pool, or stopping one would shut down the executor
/// the other's scheduler and `@Async` methods still submit to. The calls
/// here are static because generated code makes them, so they find the server by
/// the calling thread: a request thread, a task thread and a scheduled job all
/// carry the server they work for, and so does the thread that builds and starts
/// the beans. A thread that carries none -- one the program started itself --
/// gets the most recently started server that is still running.
///
/// ## Which thread
///
/// A virtual thread is the cheaper one, and the right one for work that waits
/// on sockets. It is the wrong one for work that talks to a database: on this
/// runtime a database read blocks the HOST thread under the virtual thread, and
/// every other virtual thread on that host with it. See
/// `com.codename1.backend.annotations.ThreadKind`.
public final class Tasks {
    public static final int AUTO = 0;
    public static final int VIRTUAL = 1;
    public static final int PLATFORM = 2;

    /// The executor `@Async` methods use when they name none.
    public static final String DEFAULT = "default";
    /// The executor scheduled jobs use when they name none.
    public static final String SCHEDULING = "scheduling";

    /// The registry of the server the calling thread works for, when it carries one.
    private static final ThreadLocal CURRENT = new ThreadLocal();
    /// Every registry not yet shut down, oldest first.
    private static final List LIVE = new ArrayList();

    private Tasks() {
    }

    /// One server's executors.
    static final class Registry {
        private final Map executors = new LinkedHashMap();
        private final Config config;
        private boolean reportedAuto;
        private boolean shutdown;
        /// The server whose hosts this registry's virtual tasks run on, once it is
        /// listening; null before that. Only meaningful when [#ownedByServer].
        HttpServer server;
        /// Whether a server opened this registry, as opposed to a bare process.
        final boolean ownedByServer;

        Registry(Config config, boolean ownedByServer) {
            this.config = config;
            this.ownedByServer = ownedByServer;
        }

        /// The server a virtual task of this registry may go to: its own, never
        /// another backend's -- whose stop would then wait on, or abandon, a task
        /// this one still wants. Null means "run it on a platform thread".
        synchronized HttpServer virtualHost() {
            return ownedByServer ? server : HttpServer.activeServer();
        }
    }

    /// A new registry for a server that is starting, which threads without one then use.
    static Registry open(Config config) {
        if (config != null) {
            // Every executor kind the files set is checked NOW, at start-up: an
            // executor is created on first use, and a typo found then is a start
            // that looked fine and a job running on the thread it was meant to
            // leave. One set only in the environment is checked on creation.
            for (Object name : config.keys()) {
                String key = (String) name;
                if (key.startsWith("cn1.task.executor.") && key.endsWith(".kind")) {
                    try {
                        checkedKind(key, config.get(key));
                    } catch (java.io.IOException err) {
                        throw new IllegalStateException(key + " cannot be read: "
                                + err.getMessage(), err);
                    }
                }
            }
        }
        Registry r = new Registry(config, true);
        synchronized (Tasks.class) {
            LIVE.add(r);
        }
        return r;
    }

    /// Makes `registry` the calling thread's until [#leave]; answers
    /// what to restore.
    static Object enter(Registry registry) {
        Object previous = CURRENT.get();
        CURRENT.set(registry);
        return previous;
    }

    /// The calling thread's registry as it is, for restoring later; may be null.
    static Object peek() {
        return CURRENT.get();
    }

    static void leave(Object previous) {
        CURRENT.set(previous);
    }

    /// The calling thread's registry, the newest live one, or a new one of defaults.
    private static Registry current() {
        Registry r = (Registry) CURRENT.get();
        if (r != null) {
            return r;
        }
        synchronized (Tasks.class) {
            if (LIVE.isEmpty()) {
                // No server is running -- a test calling an @Async method
                // directly: executors take their defaults.
                LIVE.add(new Registry(null, false));
            }
            return (Registry) LIVE.get(LIVE.size() - 1);
        }
    }

    /// The executor called `name` of the calling thread's server, created
    /// on first use.
    ///
    /// #### Parameters
    ///
    /// - `kind`: @param kind [#AUTO], [#VIRTUAL] or [#PLATFORM]: what the
    /// code asked for, which `cn1.task.executor..kind`
    /// overrides
    public static TaskExecutor executor(String name, int kind) {
        return executor(current(), name, kind);
    }

    static TaskExecutor executor(Registry registry, String name, int kind) {
        // An unnamed executor is named by the kind of thread asked for, so an
        // @Async(thread = VIRTUAL) method never lands on the platform pool an
        // unmarked method created first -- they would otherwise share "default".
        String key = name == null || name.length() == 0
                ? (kind == VIRTUAL ? DEFAULT + "-virtual" : DEFAULT) : name;
        synchronized (registry) {
            TaskExecutor existing = (TaskExecutor) registry.executors.get(key);
            if (existing != null) {
                return existing;
            }
            if (registry.shutdown) {
                throw new IllegalStateException("The server these tasks belong to has "
                        + "stopped; executor " + key + " cannot be created");
            }
            String prefix = "cn1.task.executor." + key + ".";
            int threads = SCHEDULING.equals(key) ? 2 : 8;
            String configured = null;
            if (registry.config != null) {
                try {
                    threads = registry.config.getInt(prefix + "threads", threads);
                    configured = registry.config.get(prefix + "kind");
                } catch (java.io.IOException err) {
                    throw new IllegalStateException("Executor " + key
                            + " cannot be configured: " + err.getMessage(), err);
                }
            }
            configured = checkedKind(prefix + "kind", configured);
            boolean virtual;
            if ("virtual".equalsIgnoreCase(configured)) {
                virtual = true;
            } else if ("platform".equalsIgnoreCase(configured)) {
                virtual = false;
            } else if (kind == AUTO) {
                virtual = HttpServer.acceptsVirtualTasks();
                if (!registry.reportedAuto) {
                    registry.reportedAuto = true;
                    System.out.println("cn1: background tasks marked AUTO run on "
                            + (virtual ? "virtual" : "platform") + " threads");
                }
            } else {
                virtual = kind == VIRTUAL;
            }
            TaskExecutor created = new TaskExecutor(key, virtual, threads, registry);
            registry.executors.put(key, created);
            return created;
        }
    }

    /// A configured executor kind, trimmed; null when unset or empty. Refused,
    /// not ignored, when it is neither platform nor virtual: the setting exists to
    /// OVERRIDE the code -- typically to move database work off virtual hosts --
    /// and a typo falling back to the annotation's kind would leave it there.
    static String checkedKind(String key, String value) {
        String kind = value == null ? null : value.trim();
        if (kind == null || kind.length() == 0) {
            return null;
        }
        if (!"virtual".equalsIgnoreCase(kind) && !"platform".equalsIgnoreCase(kind)) {
            throw new IllegalStateException(key + " is \"" + kind + "\"; it must be "
                    + "platform or virtual");
        }
        return kind;
    }

    /// Runs `task` once on a virtual thread, or a platform one where there are none.
    public static void virtual(Runnable task) {
        executor(null, VIRTUAL).execute(task);
    }

    /// Runs `task` once on the default pool of platform threads.
    public static void platform(Runnable task) {
        executor(null, PLATFORM).execute(task);
    }

    /// The body of a background task's virtual thread: the native entry point
    /// calls this with the token the task was queued under. Nothing in Java calls
    /// it, which is why the native source names it -- that keeps it alive through
    /// dead-code elimination.
    static void runVirtual(long token) {
        Runnable task = HttpServer.takeVirtualTask(token);
        if (task == null) {
            return;
        }
        try {
            task.run();
        } catch (Throwable err) {
            // The top of a virtual thread: nothing above this can catch it.
            System.err.println("A virtual-thread task failed: " + err);
        }
    }

    /// The executors of every running server, for a listing.
    public static List executors() {
        List registries;
        synchronized (Tasks.class) {
            registries = new ArrayList(LIVE);
        }
        List out = new ArrayList();
        for (Object element : registries) {
            Registry r = (Registry) element;
            synchronized (r) {
                out.addAll(r.executors.values());
            }
        }
        return out;
    }

    /// Stops the executors of the calling thread's server, waiting up to
    /// `waitMillis` in total for what is running.
    public static void shutdown(long waitMillis) {
        shutdown(current(), waitMillis);
    }

    /// Stops `registry`'s executors, waiting up to `waitMillis` in
    /// total, and retires it: nothing new is accepted, and threads that fell
    /// back to it move on to another running server's.
    static void shutdown(Registry registry, long waitMillis) {
        List all;
        synchronized (Tasks.class) {
            LIVE.remove(registry);
        }
        synchronized (registry) {
            registry.shutdown = true;
            all = new ArrayList(registry.executors.values());
        }
        long deadline = System.currentTimeMillis() + Math.max(0, waitMillis);
        for (Object element : all) {
            long left = deadline - System.currentTimeMillis();
            ((TaskExecutor) element).shutdown(left > 0 ? left : 0);
        }
    }
}
