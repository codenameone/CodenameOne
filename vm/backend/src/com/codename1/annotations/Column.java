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
package com.codename1.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Renames or constrains an {@link Entity} field's column. Optional: with no
 * annotation the column takes the field's name and a type chosen from the field's
 * Java type.
 *
 * <p>The server-side twin of the core annotation of the same name; see
 * {@link Entity} for why the name is shared.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface Column {
    /** Column name. Defaults to the field name when blank. */
    String name() default "";

    /** When false the column is declared NOT NULL at table-create time. */
    boolean nullable() default true;
    boolean unique() default false;

    /**
     * An explicit SQL type, for the case where the inferred one is not what the
     * schema needs -- VARCHAR(64) rather than TEXT, say.
     *
     * <p>THIS IS ENGINE SPECIFIC BY CONSTRUCTION and is written through to the
     * CREATE TABLE unchanged, so a value that only PostgreSQL understands makes
     * that entity PostgreSQL-only. Leaving it blank is what keeps an entity
     * portable: the dialect then names the type each engine spells differently
     * (TEXT against VARCHAR, REAL against DOUBLE PRECISION, BLOB against BYTEA)
     * and the same class creates its table on all three.
     */
    String type() default "";
}
