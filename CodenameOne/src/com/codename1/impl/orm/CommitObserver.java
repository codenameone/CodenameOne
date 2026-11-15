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
package com.codename1.impl.orm;

/// Told about each entity a session wrote, once the transaction that wrote it
/// has committed. A transaction that rolls back reports nothing.
@com.codename1.impl.SharedWithBackend
public interface CommitObserver {
    /// The entity was inserted.
    int PERSISTED = 1;
    /// The entity was updated.
    int UPDATED = 3;
    /// The entity was deleted.
    int REMOVED = 5;

    /// `entity` of `model` was written with `event` by a transaction that has
    /// committed. The data is already durable: an exception thrown here does
    /// not stop the other writes being delivered, and reaches the caller of
    /// `commitTransaction` only after the commit, with the session intact.
    void committed(EntityModel<?> model, Object entity, int event);
}
