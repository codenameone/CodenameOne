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

/// A statement failed because another transaction got there first, and not
/// because anything is wrong with it: the transaction was chosen to end a
/// deadlock, could not be serialized with one that committed before it, or
/// waited for a lock longer than the engine allows.
///
/// Named after Spring's, and meant for the same use. The statement is sound and
/// so is the data, so the caller may run its transaction again from the start,
/// or answer that somebody else made the change first. Every other
/// [DataAccessException] is a failure that running the work again would repeat.
///
/// What an engine reports this way depends on the engine and on how it is
/// configured, which is the reason to recognize the whole family and not one
/// code. Two transactions that read a row and then update it with a condition
/// are settled by most engines quietly: the second update changes no rows. An
/// engine that keeps a transaction to the snapshot it first read from refuses
/// the second update instead, and that refusal is this exception. The failures
/// recognized are:
///
/// - PostgreSQL: a serialization failure (SQLSTATE 40001), a deadlock (40P01)
///   and a lock that was not available (55P03).
/// - MySQL and MariaDB: a deadlock (error 1213), a lock wait that timed out
///   (1205), a lock refused under NOWAIT (3572), a record that changed since
///   the transaction read it (1020), and any other error of SQLSTATE class 40.
///
/// SQLite reports a database that stayed locked past its busy timeout as a
/// plain [DataAccessException].
///
/// The engine has usually undone the whole transaction by the time this is
/// thrown, and PostgreSQL refuses every further statement in it. So let it
/// leave the `@Transactional` method, which rolls back for it as for any data
/// access failure, and decide what to do from outside that method. An ORM
/// session reports a database failure as an unchecked exception with this one
/// as its cause; [#isCauseOf] finds it there.
public class ConcurrencyFailureException extends DataAccessException {
    /// How far down a chain of causes [#isCauseOf] looks.
    private static final int DEPTH = 16;

    public ConcurrencyFailureException(String message) {
        super(message);
    }

    public ConcurrencyFailureException(String message, Throwable cause) {
        super(message, cause);
    }

    /// Whether `failure` is a concurrency failure or was caused by one, however
    /// many exceptions it has been wrapped in since.
    ///
    /// @param failure what was caught; null answers false
    public static boolean isCauseOf(Throwable failure) {
        Throwable at = failure;
        for (int iter = 0; iter < DEPTH && at != null; iter++) {
            if (at instanceof ConcurrencyFailureException) {
                return true;
            }
            at = at.getCause();
        }
        return false;
    }
}
