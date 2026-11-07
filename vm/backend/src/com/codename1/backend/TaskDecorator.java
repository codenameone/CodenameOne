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
package com.codename1.backend;

/// Wraps every task an executor runs, as Spring's does: the place to carry
/// something from the submitting thread -- a tenant, a trace, a security
/// principal -- to the one that runs the task, and to clear it afterwards.
///
/// ```java
/// Tasks.executor("mail").setTaskDecorator(new TaskDecorator() {
///     public Runnable decorate(final Runnable task) {
///         final String tenant = Tenant.current();
///         return new Runnable() {
///             public void run() {
///                 Tenant.set(tenant);
///                 try {
///                     task.run();
///                 } finally {
///                     Tenant.clear();
///                 }
///             }
///         };
///     }
/// });
/// ```
public interface TaskDecorator {
    /// The runnable to run in place of `task`, called on the submitting thread.
    /// @param task the task as submitted
    /// @return what the executor runs; returning `task` leaves it as it is
    Runnable decorate(Runnable task);
}
