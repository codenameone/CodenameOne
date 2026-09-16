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
package com.codename1.backend.orm;

/**
 * One persisted field of an entity: what it is called in Java, what it is called
 * in the database, and what kind of value it holds.
 *
 * <p>Built by the generated code at class-initialization time and never changed,
 * so one instance is shared by every request that touches the entity.
 *
 * <p>The SQL type is deliberately NOT here. It is the {@link
 * com.codename1.backend.sql.Dialect} that turns a kind into a type name, because
 * the same entity has to declare TEXT on SQLite, TEXT on PostgreSQL and VARCHAR
 * on MySQL. {@link #getDeclaredType()} is the escape hatch for the schema that
 * needs a specific one, and it carries the cost of naming an engine.
 */
public final class ColumnDefinition {
    private final String field;
    private final String column;
    private final int kind;
    private final boolean nullable;
    private final String declaredType;
    private final boolean id;
    private final boolean generated;

    public ColumnDefinition(String field, String column, int kind, boolean nullable,
                            String declaredType, boolean id, boolean generated) {
        this.field = field;
        this.column = column;
        this.kind = kind;
        this.nullable = nullable;
        this.declaredType = declaredType == null || declaredType.length() == 0
                ? null : declaredType;
        this.id = id;
        this.generated = generated;
    }

    /** The Java field name, which is how a query names it. */
    public String getField() {
        return field;
    }

    /** The column name, from @Column(name) or the field name. */
    public String getColumn() {
        return column;
    }

    /** One of the kind constants on {@link com.codename1.backend.sql.Dialect}. */
    public int getKind() {
        return kind;
    }

    /** Whether the column is declared without NOT NULL. */
    public boolean isNullable() {
        return nullable;
    }

    /** An explicit SQL type from @Column(type), or null to let the dialect name it. */
    public String getDeclaredType() {
        return declaredType;
    }

    /** Whether this is the primary key. */
    public boolean isId() {
        return id;
    }

    /** Whether the DATABASE assigns this key rather than the application. */
    public boolean isGenerated() {
        return generated;
    }

    public String toString() {
        return field + " -> " + column;
    }
}
