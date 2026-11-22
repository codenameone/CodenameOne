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
package com.codename1.db.sql;

import java.io.IOException;

/// A database error that a caller can act on: a transaction that lost a race
/// with another one ([#CONCURRENCY]) or a write a constraint refused
/// ([#INTEGRITY]).
///
/// Applications see this type. The backend reports the same failures as its own
/// `ConcurrencyFailureException` and `DataIntegrityViolationException`.
@com.codename1.impl.SharedWithBackend
public class SqlFailure extends IOException {
    /// A deadlock, a serialization failure or a lock that could not be had: the
    /// transaction can be run again.
    public static final int CONCURRENCY = 1;

    /// A unique, foreign key, check or NOT NULL constraint refused the write.
    public static final int INTEGRITY = 2;

    private final int kind;

    /// A failure of `kind`, [#CONCURRENCY] or [#INTEGRITY].
    public SqlFailure(int kind, String message) {
        super(message);
        this.kind = kind;
    }

    /// [#CONCURRENCY] or [#INTEGRITY].
    public int getKind() {
        return kind;
    }
}
