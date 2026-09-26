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
import com.codename1.backend.annotations.PostConstruct;
import com.codename1.backend.annotations.PreDestroy;
import com.codename1.backend.annotations.Repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// tag::backend-bean-lifecycle[]
@Repository
public class Outbox {
    private final DataSource db;
    private final List<String> pending = new ArrayList<String>();

    public Outbox(DataSource db) {
        this.db = db;
    }

    @PostConstruct
    void createTable() throws IOException {
        // Every dependency is built, injected and initialized by now.
        db.execute("CREATE TABLE IF NOT EXISTS outbox (message VARCHAR(500))", null);
    }

    public synchronized void add(String message) {
        pending.add(message);
    }

    @PreDestroy
    synchronized void flush() throws IOException {
        // After the last request has finished, before the pool closes.
        for (String message : pending) {
            db.execute("INSERT INTO outbox (message) VALUES (?)", new Object[] {message});
        }
        pending.clear();
    }
}
// end::backend-bean-lifecycle[]
