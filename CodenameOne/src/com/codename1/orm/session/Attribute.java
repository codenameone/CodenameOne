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
package com.codename1.orm.session;

/** Immutable generated column mapping. Kind numbers match the backend SQL dialect. */
public final class Attribute {
    public static final int TEXT=0, INTEGER=1, BIGINT=2, REAL=3, BLOB=4, BOOLEAN=5, TIMESTAMP=6;
    public final String field, column, declaredType;
    public final int kind;
    public final boolean id, generated, nullable, version;
    public Attribute(String field, String column, int kind, boolean id, boolean generated,
                     boolean nullable, boolean version) {
        this(field,column,kind,id,generated,nullable,version,null);
    }
    public Attribute(String field, String column, int kind, boolean id, boolean generated,
                     boolean nullable, boolean version,String declaredType) {
        this.declaredType=declaredType;
        this.field=field; this.column=column; this.kind=kind; this.id=id;
        this.generated=generated; this.nullable=nullable; this.version=version;
    }
}
