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

/// A statement broke a rule the schema declares: a duplicate key, a foreign key
/// with no row behind it, a NOT NULL or CHECK constraint.
///
/// Named after Spring's, and meant for the same use: telling a duplicate apart
/// from a [ConcurrencyFailureException] or any other [DataAccessException].
/// Running the same work again repeats it. The failures recognized are:
///
/// - PostgreSQL: every SQLSTATE of class 23 (23505 unique, 23503 foreign key,
///   23502 not null, 23514 check).
/// - MySQL and MariaDB: every SQLSTATE of class 23, and the codes that carry
///   it -- 1062 duplicate entry, 1451/1452 foreign key, 1048 column cannot be
///   null.
/// - SQLite: a "constraint failed" error, of whichever kind.
///
/// An ORM session reports a database failure as an unchecked exception with
/// this one as its cause; [#isCauseOf] finds it there.
public class DataIntegrityViolationException extends DataAccessException {
    /// How far down a chain of causes [#isCauseOf] looks.
    private static final int DEPTH = 16;

    public DataIntegrityViolationException(String message) {
        super(message);
    }

    public DataIntegrityViolationException(String message, Throwable cause) {
        super(message, cause);
    }

    /// Whether `failure` is an integrity violation or was caused by one, however
    /// many exceptions it has been wrapped in since.
    ///
    /// - `failure`: what was caught; null answers false
    public static boolean isCauseOf(Throwable failure) {
        Throwable at = failure;
        for (int iter = 0; iter < DEPTH && at != null; iter++) {
            if (at instanceof DataIntegrityViolationException) {
                return true;
            }
            at = at.getCause();
        }
        return false;
    }
}
