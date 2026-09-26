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

import com.codename1.orm.session.Identifier;
import com.codename1.orm.session.PersistenceException;

/// Generated, reflection-free access to persistent entity values.
///
/// Internal ORM runtime; not an application API.
/// @hidden
@com.codename1.impl.SharedWithBackend
public abstract class EntityModel<T> {
    public abstract Class<T> type();
    public abstract String table();
    public Class hierarchyRoot() {
        return type();
    }
    public int discriminatorIndex() {
        return -1;
    }
    public String discriminatorValue() {
        return "";
    }
    public String[] discriminatorValues() {
        return new String[0];
    }
    public boolean hasRelationship(T entity, int index) {
        return true;
    }
    public abstract Attribute[] attributes();
    public abstract T create();
    public abstract Object get(T entity, int index);
    public Object domainValue(T entity, int index) {
        return get(entity, index);
    }
    public abstract void set(T entity, int index, Object value);
    public boolean requiresSession() {
        return relationships().length > 0 || versionIndex() >= 0 || generation() != 0 || idIndexes().length > 1 ||
                indexes().length > 0 || discriminatorIndex() >= 0;
    }
    /// 0: assigned/identity, 1: UUID, 2: native sequence with table fallback, 3: table.
    public Index[] indexes() {
        return new Index[0];
    }
    public int generation() {
        return 0;
    }
    public String generator() {
        return table() + "_id";
    }
    /// Returns the declared non-null requirement for this concrete entity's attribute.
    public boolean required(T entity, int index) {
        return !attributes()[index].nullable;
    }
    /// Whether the unconverted Java field is primitive and cannot hold null.
    public boolean primitive(int index) {
        return false;
    }
    /// Whether this field always supplies a value in a query of this model's entity type.
    /// Generated models account for optional embedded values and subtype-only fields.
    public boolean nonNullQueryValue(int index) {
        return !attributes()[index].nullable;
    }
    /// Returns whether any mapped subtype requires this column to contain a value.
    public boolean required(int index) {
        return !attributes()[index].nullable;
    }
    /// Smallest stored integral value the mapped attribute can hold.
    public long minimumIntegralValue(int index) {
        return attributes()[index].kind == Attribute.INTEGER ? Integer.MIN_VALUE : Long.MIN_VALUE;
    }
    /// Largest stored integral value the mapped attribute can hold.
    public long maximumIntegralValue(int index) {
        return attributes()[index].kind == Attribute.INTEGER ? Integer.MAX_VALUE : Long.MAX_VALUE;
    }
    /// Whether reading this REAL attribute narrows its stored value to a float.
    public boolean singlePrecision(int index) {
        return false;
    }
    /// True only for unconverted int/Integer or long/Long values, including numeric properties.
    public boolean counter(int index) {
        return false;
    }
    /// Converts a scalar column projection to its mapped Java value.
    public Object project(int index, Object value) {
        if (value == null) {
            return null;
        }
        int kind = attributes()[index].kind;
        try {
            return kind == Attribute.INTEGER ? Values.asIntObject(value)
                    : kind == Attribute.BIGINT ? Values.asLongObject(value)
                    : kind == Attribute.BOOLEAN ? Values.asBooleanObject(value)
                    : kind == Attribute.TIMESTAMP ? Values.asDate(value) : value;
        } catch (java.io.IOException error) {
            throw new PersistenceException("Invalid scalar projection", error);
        }
    }
    /// Identifies enum or converter encoding; empty for ordinary storage values.
    public String mapping(int index) {
        return "";
    }
    public Object parameter(int index, Object value) {
        return Values.storage(value);
    }
    public void read(T entity, Object[] values) {
        for (int i = 0; i < values.length; i++) {
            set(entity, i, values[i]);
        }
    }
    /// Generated lifecycle dispatch: pre/post persist, pre/post update, pre/post remove, post load.
    public void lifecycle(T entity, int event) {
    }
    public Relationship[] relationships() {
        return new Relationship[0];
    }
    public Object relation(T entity, int index) {
        if (index < 0 || index >= relationships().length) {
            throw new IllegalArgumentException("Unknown relationship");
        }
        return null;
    }
    public void relation(T entity, int index, Object value) {
        throw new IllegalArgumentException("Unknown relationship");
    }
    /// Whether an attribute belongs to a type included by this query model.
    public boolean queryAttribute(int index) {
        return true;
    }
    /// Whether a relationship belongs to a type included by this query model.
    public boolean queryRelationship(int index) {
        return true;
    }
    /// Resolves a query attribute without exposing sibling-only table columns.
    public final int queryIndex(String field) {
        int index = index(field);
        if (!queryAttribute(index)) {
            throw new IllegalArgumentException("Attribute is not applicable to this entity type: " + field);
        }
        return index;
    }
    /// Resolves a query relationship without exposing sibling-only associations.
    public final int queryRelationIndex(String field) {
        int index = relationIndex(field);
        if (!queryRelationship(index)) {
            throw new IllegalArgumentException("Relationship is not applicable to this entity type: " + field);
        }
        return index;
    }
    public final int relationIndex(String field) {
        Relationship[] all = relationships();
        for (int i = 0; i < all.length; i++) {
            if (all[i].field.equals(field)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Unknown relationship: " + field);
    }
    public final int index(String field) {
        Attribute[] attrs = attributes();
        for (int i = 0; i < attrs.length; i++) {
            if (attrs[i].field.equals(field)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Unknown field " + type().getName() + "." + field);
    }
    public final int idIndex() {
        Attribute[] attrs = attributes();
        int found = -1;
        for (int i = 0; i < attrs.length; i++) {
            if (attrs[i].id) {
                if (found < 0) {
                    found = i;
                }
            }
        }
        if (found < 0) {
            throw new PersistenceException("Missing id: " + type().getName());
        }
        return found;
    }
    public final int[] idIndexes() {
        Attribute[] attrs = attributes();
        int count = 0;
        for (Attribute attr : attrs) {
            if (attr.id) {
                count++;
            }
        }
        if (count == 0) {
            throw new PersistenceException("Missing id: " + type().getName());
        }
        int[] result = new int[count];
        int next = 0;
        for (int i = 0; i < attrs.length; i++) {
            if (attrs[i].id) {
                result[next++] = i;
            }
        }
        return result;
    }
    public final Object identifier(T entity) {
        int[] ids = idIndexes();
        Object[] values = new Object[ids.length];
        for (int i = 0; i < ids.length; i++) {
            values[i] = get(entity, ids[i]);
        }
        return ids.length == 1 ? values[0] : Identifier.of(values);
    }
    public Object[] keyValues(Object key) {
        Object[] values = key instanceof Identifier ? ((Identifier) key).values()
                          : key instanceof Object[] ? ((Object[]) key).clone()
                                                    : new Object[] {key};
        int[] ids = idIndexes();
        if (values.length != ids.length) {
            throw new IllegalArgumentException("Wrong identifier width for " + type().getName());
        }
        for (int i = 0; i < values.length; i++) {
            values[i] = Values.storage(values[i]);
            if (values[i] == null) {
                throw new IllegalArgumentException("Null identifier component for " + type().getName());
            }
            int kind = attributes()[ids[i]].kind;
            Values.requireStorageKind(values[i], kind);
            if ((kind == Attribute.INTEGER || kind == Attribute.BIGINT || kind == Attribute.BOOLEAN
                    || kind == Attribute.TIMESTAMP) && !(values[i] instanceof Long)) {
                throw new IllegalArgumentException("Identifier component requires integral storage");
            }
            if (values[i] instanceof Long && (((Long) values[i]).longValue() < minimumIntegralValue(ids[i])
                    || ((Long) values[i]).longValue() > maximumIntegralValue(ids[i]))) {
                throw new IllegalArgumentException("Identifier component is outside the mapped integral range");
            }
        }
        return values;
    }
    public final Object identifierFromRow(Object[] row) {
        int[] ids = idIndexes();
        Object[] values = new Object[ids.length];
        for (int i = 0; i < ids.length; i++) {
            values[i] = Values.storage(project(ids[i], row[ids[i]]));
        }
        return ids.length == 1 ? values[0] : Identifier.of(values);
    }
    public final int versionIndex() {
        Attribute[] attrs = attributes();
        int found = -1;
        for (int i = 0; i < attrs.length; i++) {
            if (attrs[i].version) {
                if (found >= 0) {
                    throw new PersistenceException("Multiple version fields: " + type().getName());
                }
                found = i;
            }
        }
        return found;
    }
}
