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
package java.util.concurrent;

/// The threads the `...Async` methods of [CompletableFuture] run on when
/// they are given no executor.
///
/// A cached pool of daemon threads: a task goes to an idle worker or to a
/// new one, so a task that waits for another never holds that one up -- a
/// bounded pool deadlocks the moment every worker waits on work still in its
/// queue. A worker ends after a minute without work, and none keeps a
/// program from ending, as with the JDK's common pool.
final class AsyncPool {

    private static final Object LOCK = new Object();
    private static Executor pool;

    private AsyncPool() {
    }

    /// The pool, made when it is first needed.
    static Executor get() {
        synchronized (LOCK) {
            if (pool == null) {
                pool = new Executors.Pool(Integer.MAX_VALUE, 60000L, new Executors.DefaultThreadFactory(true));
            }
            return pool;
        }
    }
}
