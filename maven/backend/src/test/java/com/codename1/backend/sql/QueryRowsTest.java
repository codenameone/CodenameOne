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
package com.codename1.backend.sql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class QueryRowsTest {
    @Test void projectionMetadataPreservesPublicListEquality() {
        QueryRows rows = new QueryRows(2);
        rows.add(Collections.singletonMap("duplicate_label", Long.valueOf(1)));
        List<?> ordinary = new ArrayList<Object>(rows);
        assertEquals(ordinary, rows);
        assertEquals(rows, ordinary);
        assertEquals(ordinary.hashCode(), rows.hashCode());
        rows.setColumnCount(3);
        assertEquals(3, rows.getColumnCount());
        assertEquals(ordinary, rows);
        assertEquals(rows, ordinary);
        assertEquals(ordinary.hashCode(), rows.hashCode());
    }
}
