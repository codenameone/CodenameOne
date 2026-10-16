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
package com.codename1.orm.repository;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Marks a repository `@Query` method whose statement is a bulk UPDATE or DELETE.
///
/// The statement runs in the active transaction and answers the number of rows it
/// changed. It goes straight to the database: per-entity callbacks and cascades do
/// not run, and entities this session already manages are not changed by it. An
/// entity read before the statement keeps the values it had then, while the row
/// may now hold others. It stays managed, so a change made to it afterwards is
/// still written when the transaction commits. Read the new values with
/// `Session.refresh(entity)`, or set `clearAutomatically` so the session is emptied
/// after the statement and the next read goes to the database.
///
/// A row that a bulk DELETE removed can still be held by the session as an
/// entity. Changing that entity, or removing it, fails at flush with an
/// `OptimisticLockException`, because its row is gone.
@com.codename1.impl.SharedWithBackend
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Modifying {
    /// Whether to flush the session's pending changes before the statement.
    ///
    /// Pending changes are flushed before every bulk statement whatever this says,
    /// so the statement sees what the transaction already changed. That is what a
    /// JPA provider's default `AUTO` flush mode does before a statement that reads
    /// tables with pending writes; here every pending change is written, as the
    /// session does not narrow a flush to the tables a statement reads. Skipping
    /// the flush could only make the statement miss changes the transaction made,
    /// so the attribute is accepted for source compatibility with Spring Data and
    /// has no further effect.
    ///
    /// @return ignored; the session is always flushed first
    boolean flushAutomatically() default false;

    /// Whether to detach every entity this session manages once the statement has
    /// run, so that what is read afterwards is read from the database.
    ///
    /// Off by default, as in Spring Data. Turning it on detaches entities the
    /// caller may still be holding: a change made to one of them after this
    /// method returns is not tracked and is never written. Make such changes
    /// first, or read the entity again after the call.
    ///
    /// @return true to clear the session after the statement
    boolean clearAutomatically() default false;
}
