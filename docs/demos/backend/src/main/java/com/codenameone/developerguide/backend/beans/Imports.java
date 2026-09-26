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
import java.util.List;

// tag::backend-tx-nested[]
@Service
public class Imports {
    private final DataSource db;

    public Imports(DataSource db) {
        this.db = db;
    }

    @Transactional
    public int importAll(List<String> lines) {
        int imported = 0;
        for (String line : lines) {
            try {
                importLine(line);           // a call through this: still transactional
                imported++;
            } catch (Exception bad) {
                // Only this line's rows were rolled back, to its savepoint.
            }
        }
        return imported;                    // the good lines commit together
    }

    @Transactional(propagation = Propagation.NESTED, rollbackFor = IOException.class)
    void importLine(String line) throws IOException {
        String[] fields = line.split(",");
        db.execute("INSERT INTO contact (name) VALUES (?)", new Object[] {fields[0]});
        db.execute("INSERT INTO phone (number) VALUES (?)", new Object[] {fields[1]});
    }
}
// end::backend-tx-nested[]
