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
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.annotations.PostConstruct;
import com.codename1.backend.annotations.Service;
import com.codename1.backend.annotations.Transactional;

import java.io.IOException;

// tag::backend-bean-service[]
@Service
public class Signups {
    private final DataSource db;

    @Autowired
    private Mailer mailer;

    public Signups(DataSource db) {
        this.db = db;
    }

    @PostConstruct
    void createTable() throws IOException {
        db.execute("CREATE TABLE IF NOT EXISTS signup (email VARCHAR(200))", null);
    }
// end::backend-bean-service[]

// tag::backend-transactional[]
    @Transactional
    public void register(String email) throws IOException {
        db.execute("INSERT INTO signup (email) VALUES (?)", new Object[] {email});
        // Throws when the mail server refuses: the insert above is rolled back,
        // because both run in the one transaction this method began.
        mailer.send(email, "Welcome", "Thanks for signing up.");
    }
// end::backend-transactional[]

    @Transactional(readOnly = true)
    public int count() throws IOException {
        java.util.Map row = db.queryOne("SELECT COUNT(*) AS n FROM signup", null);
        return ((Number) row.get("n")).intValue();
    }
}
