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
package com.codenameone.developerguide.backend;

import com.codename1.annotations.db.Migration;
import com.codename1.migration.JavaMigration;
import com.codename1.migration.MigrationContext;
import java.io.IOException;
import java.util.List;

// tag::migrations-java[]
@Migration(version = "4", description = "normalize note bodies")
public class NormalizeNotes implements JavaMigration {
    @Override
    public void migrate(MigrationContext context) throws IOException {
        List<String[]> rows = context.query("SELECT id, body FROM note", null);
        for (String[] row : rows) {
            context.execute("UPDATE note SET body = ? WHERE id = ?",
                    new Object[] { row[1].trim(), Long.valueOf(row[0]) });
        }
    }
}
// end::migrations-java[]
