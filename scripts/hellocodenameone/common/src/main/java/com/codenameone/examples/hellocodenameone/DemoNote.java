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
package com.codenameone.examples.hellocodenameone;

import com.codename1.annotations.EntityId;
import com.codename1.annotations.EntityQuery;
import com.codename1.annotations.EntitySubtitle;
import com.codename1.annotations.EntityTitle;
import com.codename1.annotations.IntentEntity;

import java.util.ArrayList;
import java.util.List;

/// An app noun for the intents suite, so the build actually generates entity code.
///
/// Declaring one is what makes the iOS builder emit an `AppEntity` struct with its
/// `EntityQuery`, and what makes the generated Java registry emit the `BY_ID` resolution the
/// coercion depends on. Without a declared entity none of that is compiled by CI, which is
/// how a generated-Swift mistake reaches a device instead of a build log.
///
/// The data is a fixed in-memory list on purpose: the point is the generated plumbing, and a
/// test that also had to arrange storage would fail for reasons that are not the plumbing.
@IntentEntity(value = "cn1_note", title = "Note", indexed = true)
public class DemoNote {

    private static final String[][] NOTES = {
        {"n1", "Buy milk", "Groceries"},
        {"n2", "Ship the release", "Work"},
        {"n3", "Call the dentist", "Health"},
    };

    private final String id;
    private final String title;
    private final String folder;

    DemoNote(String id, String title, String folder) {
        this.id = id;
        this.title = title;
        this.folder = folder;
    }

    @EntityId
    public String getId() {
        return id;
    }

    @EntityTitle
    public String getTitle() {
        return title;
    }

    @EntitySubtitle
    public String getFolder() {
        return folder;
    }

    /// Mandatory for any entity-typed parameter: the platform hands back an id and this is what
    /// turns it into the object the handler is called with.
    @EntityQuery(EntityQuery.Kind.BY_ID)
    public static DemoNote byId(String id) {
        for (int i = 0; i < NOTES.length; i++) {
            if (NOTES[i][0].equals(id)) {
                return new DemoNote(NOTES[i][0], NOTES[i][1], NOTES[i][2]);
            }
        }
        return null;
    }

    /// What the system offers before the user has typed anything.
    @EntityQuery(EntityQuery.Kind.SUGGESTED)
    public static List<DemoNote> recent() {
        List<DemoNote> out = new ArrayList<DemoNote>();
        for (int i = 0; i < NOTES.length; i++) {
            out.add(new DemoNote(NOTES[i][0], NOTES[i][1], NOTES[i][2]));
        }
        return out;
    }

    /// What it offers once they have.
    @EntityQuery(EntityQuery.Kind.SEARCH)
    public static List<DemoNote> matching(String query) {
        List<DemoNote> out = new ArrayList<DemoNote>();
        if (query == null) {
            return out;
        }
        String needle = query.toLowerCase();
        for (int i = 0; i < NOTES.length; i++) {
            if (NOTES[i][1].toLowerCase().indexOf(needle) >= 0) {
                out.add(new DemoNote(NOTES[i][0], NOTES[i][1], NOTES[i][2]));
            }
        }
        return out;
    }
}
