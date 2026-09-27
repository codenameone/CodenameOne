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

/// A database operation failed: a statement the engine refused, a connection
/// that could not be opened or was lost, a query that returned more rows than
/// one.
///
/// Named after Spring's, and treated the same way by `@Transactional`:
/// it rolls the transaction back although it is a checked exception. Spring's
/// rule is that unchecked exceptions roll back and checked ones commit, and in
/// Spring a failed statement rolls back because its JDBC layer throws the
/// unchecked `DataAccessException`. This runtime's data methods declare
/// [IOException], so their failures are this subtype of it, and the build's
/// rollback rule lists it beside `RuntimeException` and `Error`. A
/// plain `IOException` of the application's own -- a file it could not
/// read -- still commits, as in Spring; `rollbackFor` and
/// `noRollbackFor` change either.
public class DataAccessException extends IOException {
    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    /// `err` as a data access failure, keeping its message and cause.
    static DataAccessException of(IOException err) {
        if (err instanceof DataAccessException) {
            return (DataAccessException) err;
        }
        return new DataAccessException(err.getMessage(), err);
    }
}
