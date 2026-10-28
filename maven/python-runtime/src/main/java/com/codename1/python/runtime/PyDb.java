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
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.python.runtime;

import java.io.IOException;

/// The server databases (PostgreSQL, MySQL, MariaDB) behind the `_cn1db`
/// module, which SQLAlchemy's non-SQLite dialects run on.
///
/// The runtime does not depend on the backend: a build that has it generates
/// an implementation over `com.codename1.backend.Database` and installs it,
/// the way `BackendServer` is installed for `_cn1server`. Without one, opening
/// a URL raises an error that names its scheme.
public abstract class PyDb {
    private static PyDb current;

    /// The installed driver, or null when this build has none.
    static PyDb get() {
        return current;
    }

    /// Replaces the driver.
    public static void install(PyDb db) {
        current = db;
    }

    /// Opens a connection to the database `url` names
    /// (`postgresql://user:pw@host:port/db`, `mysql://...`, `mariadb://...`).
    public abstract Connection open(String url) throws IOException;

    /// One open connection. `run` answers rows for a query and the change
    /// count (and, where the engine has one, the generated id) otherwise.
    public abstract static class Connection extends PySql.Connection {
        /// The engine the server reported: `postgresql`, `mysql` or `mariadb`.
        public abstract String dialect();
    }
}
