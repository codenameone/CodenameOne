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

import java.io.IOException;

/**
 * What the build generated about one entity class: its table, its columns, and
 * reflection-free access to its fields.
 *
 * <p>Application code never implements this. The Codename One Maven plugin reads
 * @Entity, @Id, @Column and @DbTransient out of the compiled entity and writes a
 * subclass whose get and set are switch statements over field access -- which is
 * the whole reason the ORM exists as a build step rather than as a library. This
 * runtime has no reflection: a field reached by name at run time is a field that
 * is not there after the translator has renamed it.
 *
 * <p>One instance exists per entity class and is shared by every request, so an
 * implementation holds no state: {@link #get} and {@link #set} take the entity
 * they are working on.
 */
public abstract class EntityDefinition {
    /** The entity class. Used as the registry key. */
    public abstract Class type();

    /** The table name: @Entity(table) or the simple class name. */
    public abstract String table();

    /** Every persisted column, in a fixed order that indexes into get and set. */
    public abstract ColumnDefinition[] columns();

    /** A new, empty instance. The entity's public no-arg constructor. */
    public abstract Object newInstance();

    /**
     * The value of column {@code index} on {@code entity}, already in the form a
     * parameter is bound as: Long, Double, String, byte[] or null.
     */
    public abstract Object get(Object entity, int index);

    /**
     * Writes the value of column {@code index} into {@code entity}, converting
     * whatever the engine sent into the field's type. See {@link com.codename1.impl.orm.Values}.
     *
     * <p>Declared to throw because the conversion can fail and the failure is
     * worth reporting: a column holding text where the field is a number means
     * the table is not the one this entity describes, and that is a message
     * somebody can act on rather than a zero appearing in a field.
     */
    public abstract void set(Object entity, int index, Object value) throws IOException;

    /** The index of the primary key column, which every entity has exactly one of. */
    public final int idIndex() {
        ColumnDefinition[] all = columns();
        for(int iter = 0 ; iter < all.length ; iter++) {
            if(all[iter].isId()) {
                return iter;
            }
        }
        // Unreachable through the generator, which refuses an entity with no @Id
        // at build time. Worth saying out loud anyway: a hand-written definition
        // is allowed, and this is the assumption every statement here rests on.
        throw new IllegalStateException(table() + " has no @Id column");
    }

    /** The index of the column whose field is {@code name}, or -1. */
    public final int indexOfField(String name) {
        ColumnDefinition[] all = columns();
        for(int iter = 0 ; iter < all.length ; iter++) {
            if(all[iter].getField().equals(name)) {
                return iter;
            }
        }
        return -1;
    }

    public String toString() {
        return type().getName() + " as " + table();
    }
}
