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
package com.codenameone.examples.hellocodenameone.backend;

import com.codename1.backend.DataSource;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.P;
import com.codename1.backend.annotations.PreAuthorize;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The notes, in the database the migrations under `db/migration` build: V1
/// creates `secure_notes` and V2 puts three rows in it. Nothing here creates the
/// table, so a server that answers [#all] is one whose migrations ran.
@Component
public class NoteService {
    /// How many rows migration V2 inserts, with the ids 1 to this.
    static final int SEEDED = 3;

    private final DataSource db;

    public NoteService(DataSource db) {
        this.db = db;
    }

    /// Every note, in id order.
    public List<Note> all() throws IOException {
        List<Note> out = new ArrayList<Note>();
        List rows = db.query("SELECT id, title, body FROM secure_notes ORDER BY id", new Object[0]);
        for (Object row : rows) {
            Map columns = (Map) row;
            out.add(new Note(((Number) columns.get("id")).longValue(),
                    String.valueOf(columns.get("title")), String.valueOf(columns.get("body"))));
        }
        return out;
    }

    /// Adds a note and answers it with the id the database gave it.
    public Note add(Note note) throws IOException {
        long id = db.insert("INSERT INTO secure_notes (title, body) VALUES (?, ?)",
                new Object[] {note.title, note.body}, "id");
        return new Note(id, note.title, note.body);
    }

    /// Removes a note a test added. The three the migration seeded stay: they are
    /// what every other test reads, so this answers false for them as it does for
    /// an id that is not there.
    public boolean remove(long id) throws IOException {
        return id > SEEDED && db.execute("DELETE FROM secure_notes WHERE id = ?",
                new Object[] {Long.valueOf(id)}) > 0;
    }

    /// What `owner` may read about themselves. The route that reaches this asks
    /// only for a signed-in caller; the rule that it must be the owner, holding
    /// the read scope, is this method's own -- so a 403 from that route is method
    /// security answering, not the chain's URL rules.
    @PreAuthorize("hasAuthority('SCOPE_notes:read') and #owner == authentication.name")
    public Map<String, Object> summaryFor(@P("owner") String owner) throws IOException {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("owner", owner);
        out.put("notes", Long.valueOf(all().size()));
        return out;
    }
}
