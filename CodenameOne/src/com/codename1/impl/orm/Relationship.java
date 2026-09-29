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
package com.codename1.impl.orm;

/// Immutable generated relationship mapping.
///
/// Internal ORM runtime; not an application API.
/// @hidden
@com.codename1.impl.SharedWithBackend
public final class Relationship {
    public final String field;
    public final String mappedBy;
    public final String joinTable;
    public final String joinColumn;
    public final String inverseJoinColumn;
    public final String mapKey;
    public final String orderColumn;
    public final String orderBy;
    public final Class target;
    public final boolean many;
    public final boolean lazy;
    public final boolean orphanRemoval;
    public final boolean unique;
    public final boolean element;
    public final int column;
    public final int cascade;
    public static final int PERSIST = 1;
    public static final int MERGE = 2;
    public static final int REMOVE = 4;
    public static final int REFRESH = 8;
    public static final int DETACH = 16;
    public Relationship(String field, Class target, boolean many, boolean lazy, int column, String mappedBy,
            String joinTable, String joinColumn, String inverseJoinColumn, int cascade, boolean orphanRemoval) {
        this(field, target, many, lazy, column, mappedBy, joinTable, joinColumn, inverseJoinColumn, cascade,
                orphanRemoval, false);
    }
    public Relationship(String field, Class target, boolean many, boolean lazy, int column, String mappedBy,
            String joinTable, String joinColumn, String inverseJoinColumn, int cascade, boolean orphanRemoval,
            boolean unique) {
        this(field, target, many, lazy, column, mappedBy, joinTable, joinColumn, inverseJoinColumn, cascade,
                orphanRemoval, unique, "", "", "");
    }
    public Relationship(String field, Class target, boolean many, boolean lazy, int column, String mappedBy,
            String joinTable, String joinColumn, String inverseJoinColumn, int cascade, boolean orphanRemoval,
            boolean unique, String mapKey, String orderColumn, String orderBy) {
        this(field, target, many, lazy, column, mappedBy, joinTable, joinColumn, inverseJoinColumn, cascade,
                orphanRemoval, unique, mapKey, orderColumn, orderBy, false);
    }
    public Relationship(String field, Class target, boolean many, boolean lazy, int column, String mappedBy,
            String joinTable, String joinColumn, String inverseJoinColumn, int cascade, boolean orphanRemoval,
            boolean unique, String mapKey, String orderColumn, String orderBy, boolean element) {
        this.element = element;
        this.mapKey = mapKey;
        this.orderColumn = orderColumn;
        this.orderBy = orderBy;
        this.unique = unique;
        this.field = field;
        this.target = target;
        this.many = many;
        this.lazy = lazy;
        this.column = column;
        this.mappedBy = mappedBy;
        this.joinTable = joinTable;
        this.joinColumn = joinColumn;
        this.inverseJoinColumn = inverseJoinColumn;
        this.cascade = cascade;
        this.orphanRemoval = orphanRemoval;
    }
}
