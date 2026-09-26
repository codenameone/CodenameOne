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
package com.codenameone.developerguide.backend.beans;

import com.codename1.backend.DataSource;
import com.codename1.backend.annotations.Propagation;
import com.codename1.backend.annotations.Service;
import com.codename1.backend.annotations.Transactional;

import java.io.IOException;

// tag::backend-tx-requires-new[]
@Service
public class AuditLog {
    private final DataSource db;

    public AuditLog(DataSource db) {
        this.db = db;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String event) throws IOException {
        // Commits on its own connection, whatever the caller's transaction does.
        db.execute("INSERT INTO audit (event) VALUES (?)", new Object[] {event});
    }
}
// end::backend-tx-requires-new[]
