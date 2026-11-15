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
package com.codename1.backend.orm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.codename1.backend.Database;
import com.codename1.backend.DataSource;
import com.codename1.backend.search.SearchEngine;
import com.codename1.backend.search.SearchIndexer;
import com.codename1.backend.search.SqlSearchEngine;
import com.codename1.impl.orm.BackendSqlAccess;
import com.codename1.impl.orm.EntityModel;
import com.codename1.impl.orm.SessionImpl;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/// A searchable entity is indexed when its transaction commits, never when it
/// rolls back, and leaves the index when it is removed.
class SearchIndexerTest {
    static class Searchable extends ManagedSessionTest.Model {
        @Override
        public String searchIndex() {
            return "records";
        }
        @Override
        public boolean searchField(int index) {
            return index != 2;
        }
    }

    static com.codename1.impl.orm.Attribute[] textId(com.codename1.impl.orm.Attribute[] base) {
        com.codename1.impl.orm.Attribute[] out = base.clone();
        out[0] = new com.codename1.impl.orm.Attribute("id", "id", com.codename1.impl.orm.Attribute.TEXT,
                true, false, false, false);
        return out;
    }

    @Test
    void documentIdsFitTheEngineAlphabet() {
        assertEquals("42", SearchIndexer.documentId(new Searchable(), Long.valueOf(42)));
        Searchable textKeyed = new Searchable() {
            private final com.codename1.impl.orm.Attribute[] attrs = textId(super.attributes());
            @Override
            public com.codename1.impl.orm.Attribute[] attributes() {
                return attrs;
            }
        };
        assertEquals("a.0040b.002ecom", SearchIndexer.documentId(textKeyed, "a@b.com"));
        assertEquals("x.003ay_z", SearchIndexer.documentId(textKeyed, "x:y_z"));
    }

    @Test
    void indexesOnCommitOnly() throws Exception {
        Database db = Database.open(":memory:");
        SearchEngine engine = new SqlSearchEngine(DataSource.open(":memory:", 1), true);
        Map<String, EntityModel<?>> models = new LinkedHashMap<String, EntityModel<?>>();
        models.put(ManagedSessionTest.Record.class.getName(), new Searchable());
        SessionImpl session = new SessionImpl(new BackendSqlAccess(null, db, db.dialect()), models);
        try {
            session.setCommitObserver(SearchIndexer.to(engine));
            session.createTables();

            session.beginTransaction();
            ManagedSessionTest.Record dropped = new ManagedSessionTest.Record();
            dropped.name = "rolled back";
            session.persist(dropped);
            session.flush();
            String droppedId = String.valueOf(dropped.id);
            session.rollbackTransaction();
            engine.refresh("records");
            assertNull(engine.get("records", droppedId));

            session.beginTransaction();
            ManagedSessionTest.Record kept = new ManagedSessionTest.Record();
            kept.name = "blue shoes";
            kept.counter = 7;
            session.persist(kept);
            session.commitTransaction();
            String id = String.valueOf(kept.id);
            engine.refresh("records");
            Map doc = engine.get("records", id);
            assertNotNull(doc);
            assertEquals("blue shoes", doc.get("name"));
            assertFalse(doc.containsKey("counter"));
            assertFalse(doc.containsKey("bytes"));

            session.beginTransaction();
            kept.name = "red shoes";
            session.commitTransaction();
            engine.refresh("records");
            assertEquals("red shoes", engine.get("records", id).get("name"));

            session.beginTransaction();
            session.remove(kept);
            session.commitTransaction();
            engine.refresh("records");
            assertNull(engine.get("records", id));
        } finally {
            session.close();
            engine.close();
            db.close();
        }
    }
}
