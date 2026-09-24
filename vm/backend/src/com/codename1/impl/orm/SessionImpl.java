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
import com.codename1.orm.session.LazyInitializationException;
import com.codename1.orm.session.LockMode;
import com.codename1.orm.session.OptimisticLockException;
import com.codename1.orm.session.PersistenceException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// A bounded, non-thread-safe persistence context. Writes require an explicit
/// transaction; closing never commits. Instances returned by find are unique by
/// entity type and identifier until clear, detach, rollback, or close.
///
/// Internal ORM runtime; not an application API.
/// @hidden
public final class SessionImpl implements com.codename1.orm.session.Session {
    private final SqlAccess sql;
    private final Map<String, EntityModel<?>> models;
    private final Map<Key, Object> identities = new LinkedHashMap<Key, Object>();
    private final IdentityHashMap<Object, Entry> entries = new IdentityHashMap<Object, Entry>();
    private int loading;
    private int deferredFetch;
    private int aliasSequence;
    String deleteFrom(String table, String alias) {
        // Older platform SQLite versions do not accept a DELETE target alias.
        if ("sqlite".equals(sql.dialect())) {
            return "DELETE FROM " + q(table);
        }
        // MariaDB before 11.6 needs the target alias before FROM.
        String prefix = "mysql".equals(sql.dialect()) ? "DELETE " + alias + " FROM " : "DELETE FROM ";
        return prefix + q(table) + " AS " + alias;
    }
    String updateTable(String table, String alias) {
        return "UPDATE " + q(table) + ("sqlite".equals(sql.dialect()) ? "" : " AS " + alias);
    }
    QueryImpl queryForMutation(EntityModel model) {
        return new QueryImpl(this, model, "sqlite".equals(sql.dialect()) ? q(model.table()) : nextAlias());
    }
    String orderValue(String expression, int kind) {
        return sql.orderValue(expression, kind);
    }
    String orderBy(String expression, boolean ascending, int kind) {
        return sql.orderBy(expression, ascending, kind);
    }
    String likeOperator(boolean escaped) {
        return sql.likeOperator(escaped);
    }
    String likePattern(String pattern, String escape) {
        return sql.likePattern(pattern, escape);
    }
    String likeExpression(String expression) {
        return sql.likeExpression(expression);
    }
    static void checkParameterCount(int count) {
        if (count > 999) {
            throw new IllegalArgumentException("Query exceeds the portable limit of 999 bound parameters");
        }
    }
    String integralSum(String expression) {
        return "CAST(" + expression + " AS " + ("mysql".equals(sql.dialect()) ? "SIGNED" : "BIGINT") + ")";
    }
    String functionName(String function) {
        return "LENGTH".equals(function) && "mysql".equals(sql.dialect()) ? "CHAR_LENGTH" : function;
    }
    String numericOperand(String value, int kind) {
        if ("postgresql".equals(sql.dialect())) {
            return "CAST(" + value + " AS " + (kind == Attribute.REAL ? "DOUBLE PRECISION" : "BIGINT") + ")";
        }
        if (kind == Attribute.REAL) {
            return "mysql".equals(sql.dialect()) ? "(1e0 * " + value + ")" : "CAST(" + value + " AS REAL)";
        }
        return value;
    }
    String arithmetic(String left, String op, String right, int kind) {
        left = numericOperand(left, kind);
        right = numericOperand(right, kind);
        if ("/".equals(op) || "%".equals(op)) {
            right = "NULLIF(" + right + ", 0)";
            if ("/".equals(op) && kind != Attribute.REAL && "mysql".equals(sql.dialect())) {
                op = "DIV";
            }
        }
        return checkedArithmetic("(" + left + " " + op + " " + right + ")", kind);
    }
    String checkedArithmetic(String expression, int kind) {
        if (kind != Attribute.REAL && "sqlite".equals(sql.dialect())) {
            // SQLite promotes overflowing integer operations to REAL. Force an
            // execution error before a promoted value can escape or be stored.
            // CASE is lazy; ABS(MIN_VALUE) is evaluated only on overflow.
            return "(CASE WHEN typeof(" + expression + ") = 'real' THEN abs(-9223372036854775808) ELSE "
                    + expression + " END)";
        }
        return expression;
    }
    String checkedIntegralAssignment(String expression, long min, long max) {
        String value = numericOperand(expression, Attribute.BIGINT);
        String overflow;
        if ("sqlite".equals(sql.dialect())) {
            overflow = "abs(-9223372036854775808)";
        } else {
            // Keep overflow dependent on the row value so constant folding
            // cannot reject a valid CASE branch. Signed arithmetic also fails
            // on MySQL when column assignment clipping is enabled.
            String type = "mysql".equals(sql.dialect()) ? "SIGNED" : "BIGINT";
            String signed = "CAST(" + value + " AS " + type + ")";
            overflow = "(" + signed + " + CASE WHEN " + signed + " < 0 THEN CAST('-9223372036854775808' AS "
                    + type + ") ELSE CAST('9223372036854775807' AS " + type + ") END)";
        }
        return "(CASE WHEN " + value + " < " + min + " OR " + value + " > " + max
                + " THEN " + overflow + " ELSE " + value + " END)";
    }
    String sqlDialect() {
        return sql.dialect();
    }
    String nextAlias() {
        return "q" + (aliasSequence++);
    }
    EntityModel model(String name) {
        EntityModel result = null;
        for (EntityModel candidate : models.values()) {
            String binary = candidate.type().getName();
            if (binary.equals(name) ||
                    binary.substring(binary.lastIndexOf('.') + 1).equals(name)) {
                if (result != null) {
                    throw new IllegalArgumentException("Ambiguous entity name: " + name);
                }
                result = candidate;
            }
        }
        if (result == null) {
            throw new IllegalArgumentException("Unknown entity: " + name);
        }
        return result;
    }
    @Override
    public <T> JpqlQueryImpl<T> createQuery(String statement, Class<T> resultType) {
        check();
        return new JpqlQueryImpl<T>(this, statement, resultType);
    }
    @Override
    public JpqlQueryImpl<Object> createQuery(String statement) {
        return createQuery(statement, Object.class);
    }

    private boolean closed;
    private boolean transaction;
    private boolean rollbackOnly;
    private boolean flushing;

    public SessionImpl(SqlAccess sql) {
        this(sql, Models.snapshot());
    }
    public SessionImpl(SqlAccess sql, Map<String, EntityModel<?>> models) {
        if (sql == null || models == null) {
            throw new IllegalArgumentException("sql/models is null");
        }
        this.sql = sql;
        this.models = new LinkedHashMap<String, EntityModel<?>>(models);
        if ("mysql".equals(sql.dialect())) {
            for (EntityModel model : this.models.values()) {
                if (hasRequiredPath(model, model, new ArrayList<EntityModel>())) {
                    throw new PersistenceException("MySQL/MariaDB does not support required relationship cycles: "
                            + model.type().getName());
                }
            }
        }
    }
    private boolean hasRequiredPath(EntityModel current, EntityModel goal, List<EntityModel> visited) {
        if (visited.contains(current)) {
            return false;
        }
        visited.add(current);
        for (Relationship relation : current.relationships()) {
            if (relation.column < 0 || current.attributes()[relation.column].nullable) {
                continue;
            }
            for (EntityModel candidate : models.values()) {
                if (relation.target.isAssignableFrom(candidate.type())
                        && (sameInstance(candidate, goal) || hasRequiredPath(candidate, goal, visited))) {
                    return true;
                }
            }
        }
        return false;
    }
    @Override
    public void beginTransaction() {
        check();
        if (transaction) {
            throw new PersistenceException("Transaction already active");
        }
        try {
            sql.begin();
            transaction = true;
            rollbackOnly = false;
        } catch (IOException e) {
            throw failure(e);
        }
    }
    @Override
    public void commitTransaction() {
        requireTransaction();
        if (rollbackOnly) {
            throw new PersistenceException("Transaction requires rollback");
        }
        try {
            flush();
            sql.commit();
            transaction = false;
        } catch (IOException e) {
            recoverTransactionFailure();
            throw failure(e);
        } catch (RuntimeException e) {
            recoverTransactionFailure();
            throw e;
        }
    }
    @Override
    public void rollbackTransaction() {
        check();
        if (!transaction) {
            throw new PersistenceException("No active transaction");
        }
        try {
            sql.rollback();
            transaction = false;
            rollbackOnly = false;
        } catch (IOException e) {
            recoverTransactionFailure();
            throw failure(e);
        } finally {
            clear();
        }
    }
    private void recoverTransactionFailure() {
        transaction = sql.isTransactionActive();
        rollbackOnly = transaction;
        if (!transaction) {
            clear();
        }
    }
    @Override
    public boolean isTransactionActive() {
        return transaction;
    }
    @Override
    public boolean isRollbackOnly() {
        return rollbackOnly;
    }
    @Override
    public boolean contains(Object entity) {
        check();
        Entry entry = entries.get(entity);
        return entry != null && !entry.removed;
    }
    @Override
    public void detach(Object entity) {
        check();
        detach(entity, new IdentityHashMap<Object, Boolean>());
    }
    private void detach(Object entity, IdentityHashMap<Object, Boolean> visited) {
        if (visited.put(entity, Boolean.TRUE) != null) {
            return;
        }
        Entry entry = entries.get(entity);
        if (entry == null) {
            return;
        }
        EntityState state = state(entity);
        Relationship[] relations = entry.model.relationships();
        for (int i = 0; i < relations.length; i++) {
            if ((relations[i].cascade & Relationship.DETACH) == 0 || (state != null && !state.loaded[i])) {
                continue;
            }
            Object value = entry.model.relation(entity, i);
            if (value == null) {
                continue;
            }
            if (relations[i].many) {
                for (Object child : relatedValues(value)) {
                    detach(child, visited);
                }
            } else {
                detach(value, visited);
            }
        }
        detachOne(entity);
    }
    private void detachOne(Object entity) {
        Entry entry = entries.remove(entity);
        if (entry != null) {
            identities.remove(new Key(entry.model.hierarchyRoot().getName(),
                    entry.snapshot == null ? entry.initialId : entry.model.identifierFromRow(entry.snapshot)));
        }
        EntityState state = state(entity);
        if (state != null) {
            state.attached = false;
            state.session = null;
        }
    }
    @Override
    public void clear() {
        check();
        for (Object entity : entries.keySet()) {
            EntityState state = state(entity);
            if (state != null) {
                state.attached = false;
                state.session = null;
            }
        }
        entries.clear();
        identities.clear();
    }
    @Override
    public void close() {
        if (closed) {
            return;
        }
        try {
            if (transaction) {
                rollbackTransaction();
            }
        } finally {
            clear();
            closed = true;
            try {
                sql.close();
            } catch (IOException e) {
                throw failure(e);
            }
        }
    }
    @Override
    public <T> T find(Class<T> type, Object id) {
        check();
        EntityModel<T> model = model(type);
        if (id == null) {
            throw new IllegalArgumentException("id is null");
        }
        Object[] keyValues = model.keyValues(id);
        Key key = new Key(
                model.hierarchyRoot().getName(), keyValues.length == 1 ? keyValues[0] : Identifier.of(keyValues));
        Object cached = identities.get(key);
        if (cached != null) {
            checkManagedIdentity(entries.get(cached));
            return entries.get(cached).removed || !type.isInstance(cached) ? null : (T) cached;
        }
        autoFlush();
        List<Object[]> rows = read(select(model) + " WHERE " + keyCondition(model, null), keyValues, kinds(model));
        return rows.isEmpty() ? null : hydrate(model, rows.get(0));
    }
    /// Reads or locks a row inside this session's transaction.
    @Override
    public <T> T find(Class<T> type, Object id, LockMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("lock mode is null");
        }
        if (mode == LockMode.NONE) {
            return find(type, id);
        }
        requireTransaction();
        String clause = sql.lockClause(mode);
        autoFlush();
        EntityModel<T> model = model(type);
        Object[] keyValues = model.keyValues(id);
        List<Object[]> rows =
                read(select(model) + " WHERE " + keyCondition(model, null) + clause, keyValues, kinds(model));
        if (rows.isEmpty()) {
            return null;
        }
        T cached = (T) identities.get(new Key(
                model.hierarchyRoot().getName(), keyValues.length == 1 ? keyValues[0] : Identifier.of(keyValues)));
        int version = model.versionIndex();
        if (cached != null && version >= 0 && !same(model.get(cached, version), rows.get(0)[version])) {
            rollbackOnly = true;
            throw new OptimisticLockException("Stale entity while acquiring lock: " + model.table());
        }
        return hydrate(model, rows.get(0));
    }
    @Override
    public void lock(Object entity, LockMode mode) {
        if (!contains(entity)) {
            throw new PersistenceException("lock requires a managed entity");
        }
        if (mode != LockMode.NONE) {
            requireTransaction();
            autoFlush();
        }
        checkManagedIdentity(entries.get(entity));
        EntityModel model = model(entity.getClass());
        if (find(model.type(), model.identifier(entity), mode) == null) {
            rollbackOnly = true;
            throw new OptimisticLockException("Row no longer exists");
        }
    }
    @Override
    public <T> void persist(T entity) {
        try {
            persistInternal(entity, new IdentityHashMap<Object, Boolean>());
        } catch (RuntimeException error) {
            if (transaction) {
                rollbackOnly = true;
            }
            throw error;
        }
    }
    private <T> void persistInternal(T entity, IdentityHashMap<Object, Boolean> visited) {
        requireTransaction();
        if (entity == null) {
            throw new IllegalArgumentException("entity is null");
        }
        if (visited.put(entity, Boolean.TRUE) != null) {
            return;
        }
        if (entries.containsKey(entity)) {
            Entry entry = entries.get(entity);
            entry.removed = false;
            cascadePersist(entry, visited);
            return;
        }
        EntityState previous = state(entity);
        if (previous != null && previous.attached && !sameInstance(previous.session, this)) {
            throw new PersistenceException("Entity already belongs to another session");
        }
        EntityModel<T> model = model((Class<T>) entity.getClass());
        int id = model.idIndex();
        Object value = model.identifier(entity);
        if (model.generation() != 0) {
            if (value != null && (!(value instanceof Number) || ((Number) value).longValue() != 0)) {
                throw new PersistenceException("persist requires a new entity; use merge for detached entities");
            }
            try {
                model.set(entity, id,
                        sql.nextIdentifier(model.generation(), model.generator(), model.attributes()[id].kind));
            } catch (IOException error) {
                rollbackOnly = true;
                throw failure(error);
            }
            value = model.identifier(entity);
        }
        if (model.attributes()[id].generated && value != null &&
                (!(value instanceof Number) || ((Number) value).longValue() != 0)) {
            throw new PersistenceException("persist requires a new entity; use merge for detached entities");
        }
        if (!model.attributes()[id].generated) {
            model.keyValues(value);
        }
        Entry e = new Entry(model, entity, null);
        e.fresh = true;
        e.initialId = value instanceof byte[] ? ((byte[]) value).clone() : value;
        if (!model.attributes()[id].generated) {
            claim(model, entity);
        }
        entries.put(entity, e);
        attachState(e, true, null);
        cascadePersist(e, visited);
    }
    @Override
    public <T> T merge(T entity) {
        try {
            return merge(entity, new IdentityHashMap<Object, Object>());
        } catch (RuntimeException error) {
            if (transaction) {
                rollbackOnly = true;
            }
            throw error;
        }
    }
    private <T> T merge(T entity, IdentityHashMap<Object, Object> merging) {
        requireTransaction();
        if (merging.containsKey(entity)) {
            return (T) merging.get(entity);
        }
        if (entity == null) {
            throw new IllegalArgumentException("entity is null");
        }
        if (entries.containsKey(entity)) {
            merging.put(entity, entity);
            mergeRelations(entries.get(entity).model, entity, entity, merging);
            return entity;
        }
        EntityModel<T> model = model((Class<T>) entity.getClass());
        Object id = model.identifier(entity);
        boolean generated = model.attributes()[model.idIndex()].generated || model.generation() != 0;
        T managed = id == null || (generated && id instanceof Number && ((Number) id).longValue() == 0)
                            ? null
                            : find(model.type(), id);
        if (managed == null) {
            if (generated && id != null && (!(id instanceof Number) || ((Number) id).longValue() != 0)) {
                throw new OptimisticLockException("Detached row no longer exists: " + model.table());
            }
            managed = model.create();
            copy(model, entity, managed);
            persist(managed);
        } else {
            int version = model.versionIndex();
            if (version >= 0 && !same(model.get(entity, version), model.get(managed, version))) {
                throw new OptimisticLockException("Stale entity: " + model.table());
            }
            copy(model, entity, managed);
        }
        merging.put(entity, managed);
        mergeRelations(model, entity, managed, merging);
        return managed;
    }
    private void mergeRelations(EntityModel model, Object entity, Object managed,
            IdentityHashMap<Object, Object> merging) {
        Relationship[] relations = model.relationships();
        EntityState sourceState = state(entity);
        for (int i = 0; i < relations.length; i++) {
            if (sourceState != null && !sourceState.loaded[i]) {
                continue;
            }
            Relationship relation = relations[i];
            // A managed source already owns its values; only MERGE cascades
            // need traversal. In particular, leave unloaded associations alone.
            if (sameInstance(entity, managed) && (relation.element || (relation.cascade & Relationship.MERGE) == 0)) {
                continue;
            }
            Object value = model.relation(entity, i);
            if (relation.element) {
                List rows = elementRows(relation, value);
                List copy = new ArrayList();
                Map mapping = new LinkedHashMap();
                for (Object item : rows) {
                    ElementRow row = (ElementRow) item;
                    Object element = Elements.read(relation.target, row.value);
                    if (relation.mapKey.length() > 0) {
                        mapping.put(row.key, element);
                    } else {
                        copy.add(element);
                    }
                }
                initialize(managed, i);
                model.relation(managed, i, relation.mapKey.length() > 0 ? mapping : copy);
                continue;
            }
            if (relation.many && value != null) {
                List children = new ArrayList();
                for (Object child : relatedValues(value)) {
                    children.add(mergeTarget(relation, child, merging));
                }
                value = children;
            } else if (value != null) {
                value = mergeTarget(relation, value, merging);
            }
            initialize(managed, i);
            model.relation(managed, i, value);
            EntityState targetState = state(managed);
            if (targetState != null) {
                targetState.loaded[i] = true;
            }
        }
    }
    private Object mergeTarget(Relationship relation, Object child, IdentityHashMap<Object, Object> merging) {
        if ((relation.cascade & Relationship.MERGE) != 0) {
            return merge(child, merging);
        }
        EntityModel target = model(relation.target);
        Object id = target.identifier(child);
        Object managed = id == null ? null : find(relation.target, id);
        if (managed == null) {
            throw new PersistenceException("Transient association without cascade MERGE: " + relation.field);
        }
        return managed;
    }
    @Override
    public void remove(Object entity) {
        try {
            removeInternal(entity);
        } catch (RuntimeException error) {
            if (transaction) {
                rollbackOnly = true;
            }
            throw error;
        }
    }
    private void removeInternal(Object entity) {
        requireTransaction();
        Entry entry = entries.get(entity);
        if (entry == null) {
            throw new PersistenceException("remove requires a managed entity");
        }
        if (entry.removed) {
            return;
        }
        checkManagedIdentity(entry);
        entry.removed = true;
        Relationship[] relations = entry.model.relationships();
        for (int i = 0; i < relations.length; i++) {
            if ((relations[i].cascade & Relationship.REMOVE) == 0 && !relations[i].orphanRemoval) {
                continue;
            }
            initialize(entity, i);
            Object value = entry.model.relation(entity, i);
            if (value == null) {
                continue;
            }
            if (relations[i].many) {
                for (Object child : relatedValues(value)) {
                    remove(child);
                }
            } else {
                remove(value);
            }
        }
        if (entry.fresh) {
            detachOne(entity);
        }
    }
    @Override
    public void refresh(Object entity) {
        check();
        refreshEntry(entity);
        try {
            refresh(entity, new IdentityHashMap<Object, Boolean>());
        } catch (RuntimeException error) {
            if (transaction) {
                rollbackOnly = true;
            }
            clear();
            throw error;
        }
    }
    private Entry refreshEntry(Object entity) {
        Entry entry = entries.get(entity);
        if (entry == null || entry.fresh || entry.removed) {
            throw new PersistenceException("refresh requires a persisted managed entity");
        }
        return entry;
    }
    private void refresh(Object entity, IdentityHashMap<Object, Boolean> visited) {
        if (visited.put(entity, Boolean.TRUE) != null) {
            return;
        }
        Entry entry = refreshEntry(entity);
        EntityModel model = entry.model;
        checkManagedIdentity(entry);
        List<Object[]> rows = read(select(model) + " WHERE " + keyCondition(model, null),
                model.keyValues(model.identifier(entity)), kinds(model));
        if (rows.isEmpty()) {
            if (transaction) {
                rollbackOnly = true;
            }
            throw new OptimisticLockException("Row no longer exists: " + model.table());
        }
        attachState(entry, false, rows.get(0));
        assign(model, entity, rows.get(0));
        entry.snapshot = snapshot(model, entity);
        Relationship[] relations = model.relationships();
        loading++;
        try {
            for (int i = 0; i < relations.length; i++) {
                boolean cascade = (relations[i].cascade & Relationship.REFRESH) != 0;
                if (!relations[i].lazy || cascade) {
                    initialize(entity, i);
                }
                if (!cascade || !state(entity).loaded[i]) {
                    continue;
                }
                Object value = model.relation(entity, i);
                if (value == null) {
                    continue;
                }
                if (relations[i].many) {
                    for (Object child : relatedValues(value)) {
                        refresh(child, visited);
                    }
                } else {
                    refresh(value, visited);
                }
            }
            model.lifecycle(entity, 6);
        } finally {
            loading--;
        }
    }
    @Override
    public void flush() {
        requireTransaction();
        if (rollbackOnly) {
            throw new PersistenceException("Transaction requires rollback");
        }
        if (flushing) {
            return;
        }
        flushing = true;
        boolean completed = false;
        try {
            int previous;
            do {
                previous = entries.size();
                for (Entry entry : new ArrayList<Entry>(entries.values())) {
                    if (!entry.removed) {
                        cascadePersist(entry);
                    }
                }
            } while (previous != entries.size());
            List<Entry> inserted = new ArrayList<Entry>();
            insertPending(inserted);
            // PrePersist callbacks may have introduced new cascaded entities.
            List<Entry> pending = new ArrayList<Entry>(entries.values());
            List<Entry> updated = new ArrayList<Entry>();
            for (Entry e : pending) {
                if (!e.removed && !inserted.contains(e) && update(e, inserted)) {
                    updated.add(e);
                }
            }
            // PreUpdate callbacks may have introduced entities with collections too.
            pending = new ArrayList<Entry>(entries.values());
            // Release all old collection links before inserting moved children.
            for (Entry e : pending) {
                if (!e.removed) {
                    syncCollections(e, true);
                }
            }
            for (Entry e : pending) {
                if (!e.removed) {
                    syncCollections(e, false);
                }
            }
            // Relationship writes are part of the update. Fire the post callback
            // only after they succeed, including for collection-only changes.
            for (Entry e : updated) {
                e.model.lifecycle(e.entity, 3);
            }
            prepareDeletes();
            for (Entry e : new ArrayList<Entry>(entries.values())) {
                if (e.removed && entries.containsKey(e.entity)) {
                    delete(e);
                }
            }
            completed = true;
        } finally {
            // Preserve rollback-only behavior without catching implicit generic
            // casts: ParparVM does not throw ClassCastException for those casts.
            if (!completed) {
                rollbackOnly = true;
            }
            flushing = false;
        }
    }
    /// Increments in SQL and refreshes an already managed instance.
    @Override
    public <T> boolean increment(Class<T> type, Object id, String field, long amount) {
        requireTransaction();
        flush();
        EntityModel<T> model = model(type);
        int index = model.queryIndex(field);
        int version = model.versionIndex();
        Attribute a = model.attributes()[index];
        if (a.id || a.version || !model.counter(index)) {
            throw new IllegalArgumentException("Counter must be a non-key integral field: " + field);
        }
        String statement = "UPDATE " + q(model.table()) + " SET " + q(a.column) + " = " + q(a.column) + " + ?";
        if (version >= 0) {
            String v = q(model.attributes()[version].column);
            statement += ", " + v + " = " + v + " + 1";
        }
        long min = a.kind == Attribute.INTEGER ? Integer.MIN_VALUE : Long.MIN_VALUE;
        long max = a.kind == Attribute.INTEGER ? Integer.MAX_VALUE : Long.MAX_VALUE;
        long lower = amount < 0 ? min - amount : min;
        long upper = amount > 0 ? max - amount : max;
        if (lower > upper) {
            return false;
        }
        statement +=
                " WHERE " + keyCondition(model, null) + " AND " + q(a.column) + " >= ? AND " + q(a.column) + " <= ?";
        if (version >= 0) {
            statement += " AND " + q(model.attributes()[version].column) + " < " +
                         (model.attributes()[version].kind == Attribute.INTEGER ? Integer.MAX_VALUE : Long.MAX_VALUE);
        }
        String discriminator = discriminatorCondition(model, null);
        if (discriminator.length() > 0) {
            statement += " AND " + discriminator;
        }
        List<Object> arguments = new ArrayList<Object>();
        arguments.add(Long.valueOf(amount));
        Object[] keyValues = model.keyValues(id);
        arguments.addAll(Arrays.asList(keyValues));
        arguments.add(Long.valueOf(lower));
        arguments.add(Long.valueOf(upper));
        int changed = write(statement, arguments.toArray());
        Object managed = identities.get(new Key(
                model.hierarchyRoot().getName(), keyValues.length == 1 ? keyValues[0] : Identifier.of(keyValues)));
        if (changed > 0 && managed != null) {
            refresh(managed);
        }
        return changed > 0;
    }
    @Override
    public <T> QueryImpl<T> query(Class<T> type) {
        check();
        return new QueryImpl<T>(this, model(type));
    }
    /// Creates missing tables and their constraints. Existing schemas require migrations.
    @Override
    public void createTables() {
        check();
        if (transaction) {
            throw new PersistenceException("Create schemas outside application transactions");
        }
        validateKeyWidths();
        List<String> created = new ArrayList<String>();
        List<ForeignKey> foreignKeys = new ArrayList<ForeignKey>();
        for (EntityModel<?> model : models.values()) {
            if (model.generation() != 0) {
                try {
                    sql.prepareGenerator(model.generation(), model.generator());
                } catch (IOException error) {
                    throw failure(error);
                }
            }
            if (!describe(model.table()).isEmpty()) {
                continue;
            }
            StringBuilder statement = new StringBuilder("CREATE TABLE ").append(q(model.table())).append(" (");
            Attribute[] attrs = model.attributes();
            for (int i = 0; i < attrs.length; i++) {
                if (i > 0) {
                    statement.append(", ");
                }
                Attribute a = attrs[i];
                statement.append(q(a.column)).append(' ');
                if (a.generated) {
                    statement.append(sql.generatedKeyColumn(a.kind, q(a.column)));
                } else {
                    statement.append(columnType(model, i)).append(a.nullable && !a.id ? "" : " NOT NULL");
                }
            }
            if (!attrs[model.idIndex()].generated) {
                statement.append(", PRIMARY KEY (").append(quoted(keyColumns(model))).append(')');
            }
            List<ForeignKey> keys = new ArrayList<ForeignKey>();
            for (Relationship relation : model.relationships()) {
                if (relation.column >= 0) {
                    keys.add(new ForeignKey(model.table(), foreignColumns(model, relation),
                            model(relation.target).table(), keyColumns(model(relation.target))));
                }
            }
            if ("sqlite".equals(sql.dialect())) {
                for (ForeignKey key : keys) {
                    statement.append(", ").append(foreignDeclaration(key));
                }
            } else {
                foreignKeys.addAll(keys);
            }
            write(statement.append(')').toString(), new Object[0]);
            created.add(model.table());
        }
        for (EntityModel<?> model : models.values()) {
            for (Relationship relation : model.relationships()) {
                if (!relation.many || relation.mappedBy.length() > 0 || !describe(relation.joinTable).isEmpty()) {
                    continue;
                }
                if (relation.element) {
                    createElementTable(model, relation, foreignKeys);
                    created.add(relation.joinTable);
                    continue;
                }
                EntityModel target = model(relation.target);
                String[] owner = joinColumns(relation.joinColumn, model);
                String[] child = joinColumns(relation.inverseJoinColumn, target);
                StringBuilder definition =
                        new StringBuilder("CREATE TABLE ").append(q(relation.joinTable)).append(" (");
                appendJoinDefinition(definition, owner, model);
                definition.append(", ");
                appendJoinDefinition(definition, child, target);
                if (relation.orderColumn.length() > 0) {
                    definition.append(", ").append(q(relation.orderColumn)).append(" INTEGER NOT NULL");
                }
                definition.append(", PRIMARY KEY (")
                        .append(quoted(owner))
                        .append(", ")
                        .append(relation.orderColumn.length() == 0 ? quoted(child) : q(relation.orderColumn))
                        .append(')');
                ForeignKey ownerKey = new ForeignKey(relation.joinTable, owner, model.table(), keyColumns(model));
                ForeignKey childKey = new ForeignKey(relation.joinTable, child, target.table(), keyColumns(target));
                if ("sqlite".equals(sql.dialect())) {
                    definition.append(", ")
                            .append(foreignDeclaration(ownerKey))
                            .append(", ")
                            .append(foreignDeclaration(childKey));
                } else {
                    foreignKeys.add(ownerKey);
                    foreignKeys.add(childKey);
                }
                write(definition.append(')').toString(), new Object[0]);
                created.add(relation.joinTable);
                createIndex(relation.joinTable, "", relation.unique, child);
            }
        }
        for (ForeignKey key : foreignKeys) {
            write("ALTER TABLE " + q(key.table) + " ADD " + foreignDeclaration(key), new Object[0]);
        }
        List<String> indexed = new ArrayList<String>();
        for (EntityModel<?> model : models.values()) {
            if (created.contains(model.table()) && !indexed.contains(model.table())) {
                indexed.add(model.table());
                for (Relationship relation : model.relationships()) {
                    if (relation.column >= 0) {
                        createIndex(model.table(), "", relation.unique, foreignColumns(model, relation));
                    }
                }
                for (Index index : model.indexes()) {
                    String[] fields = index.fields();
                    String[] columns = new String[fields.length];
                    if (fields.length == 0) {
                        throw new PersistenceException("An index needs at least one field");
                    }
                    for (int i = 0; i < fields.length; i++) {
                        columns[i] = model.attributes()[model.index(fields[i])].column;
                    }
                    createIndex(model.table(), index.name, index.unique, columns);
                }
            }
        }
    }
    private List<Object[]> describe(String table) {
        try {
            return sql.describe(table);
        } catch (IOException error) {
            throw failure(error);
        }
    }
    private void validateKeyWidths() {
        for (EntityModel model : models.values()) {
            int primary = keyWidth(model, model.idIndexes());
            checkKeyWidth(model.table(), primary);
            for (Index index : model.indexes()) {
                int width = 0;
                for (String field : index.fields()) {
                    width += keyWidth(model, new int[] {model.index(field)});
                }
                checkKeyWidth(model.table(), width);
            }
            for (Relationship relation : model.relationships()) {
                if (relation.many && relation.mappedBy.length() == 0) {
                    int suffix = relation.element ? (relation.mapKey.length() > 0 ? 1020 : 4)
                            : relation.orderColumn.length() > 0 ? 4
                            : keyWidth(model(relation.target), model(relation.target).idIndexes());
                    checkKeyWidth(relation.joinTable, primary + suffix);
                }
            }
        }
    }
    private int keyWidth(EntityModel model, int[] indexes) {
        int width = 0;
        for (int index : indexes) {
            Attribute attribute = model.attributes()[index];
            // Explicit declarations remain the application's schema contract.
            if (attribute.declaredType == null) {
                width += attribute.kind == Attribute.TEXT ? 255 * 4
                        : attribute.kind == Attribute.INTEGER ? 4 : attribute.kind == Attribute.BOOLEAN ? 2 : 8;
            }
        }
        return width;
    }
    private void checkKeyWidth(String table, int width) {
        if (width > 3072) {
            throw new PersistenceException("Generated key exceeds the portable limit of 3072 bytes: " + table);
        }
    }
    private String columnType(EntityModel model, int index) {
        Attribute attr = model.attributes()[index];
        if (attr.declaredType != null) {
            return attr.declaredType;
        }
        if (!keyColumn(model, index)) {
            return sql.columnType(attr.kind);
        }
        String declaration = sql.assignedKeyColumn(attr.kind);
        int end = declaration.indexOf(" PRIMARY KEY");
        return end < 0 ? sql.columnType(attr.kind) : declaration.substring(0, end).replace(" NOT NULL", "");
    }
    private boolean keyColumn(EntityModel model, int index) {
        Attribute attr = model.attributes()[index];
        if (attr.id) {
            return true;
        }
        for (Index definition : model.indexes()) {
            for (String field : definition.fields()) {
                if (field.equals(attr.field)) {
                    return true;
                }
            }
        }
        for (Relationship relation : model.relationships()) {
            if (relation.column >= 0 && index >= relation.column &&
                    index < relation.column + model(relation.target).idIndexes().length) {
                return true;
            }
        }
        return false;
    }
    private void validateTextKey(EntityModel model, int index, Object value) {
        Attribute attr = model.attributes()[index];
        if (attr.kind == Attribute.TEXT && attr.declaredType == null && value instanceof String
                && ((String) value).length() > 255 && keyColumn(model, index)) {
            throw new PersistenceException("Text key exceeds the portable limit of 255 characters: "
                    + model.table() + "." + attr.column);
        }
    }
    private boolean columnNameMatches(String mapped, String actual) {
        return "postgresql".equals(sql.dialect()) ? mapped.equals(actual) : mapped.equalsIgnoreCase(actual);
    }
    private String constraintName(String prefix, String table, String[] columns) {
        StringBuilder text = new StringBuilder(table);
        for (String column : columns) {
            text.append('/').append(column);
        }
        return "cn1_" + prefix + "_" + Integer.toHexString(text.toString().hashCode());
    }
    private String foreignDeclaration(ForeignKey key) {
        String statement = "CONSTRAINT " + q(constraintName("fk", key.table, key.columns)) + " FOREIGN KEY (" +
                           quoted(key.columns) + ") REFERENCES " + q(key.target) + " (" + quoted(key.targetColumns) +
                           ")";
        if (!"mysql".equals(sql.dialect())) {
            statement += " DEFERRABLE INITIALLY DEFERRED";
        }
        return statement;
    }
    private void createIndex(String table, String name, boolean unique, String[] columns) {
        if (name.length() == 0) {
            name = constraintName(unique ? "unique" : "index", table, columns);
        }
        write("CREATE " + (unique ? "UNIQUE " : "") + "INDEX " + q(name) + " ON " + q(table) + " (" + quoted(columns) +
                        ")",
                new Object[0]);
    }
    private static final class ForeignKey {
        final String table;
        final String target;
        final String[] columns;
        final String[] targetColumns;
        ForeignKey(String table, String[] columns, String target, String[] targetColumns) {
            this.table = table;
            this.columns = columns;
            this.target = target;
            this.targetColumns = targetColumns;
        }
    }
    /// Read-only validation of mapped columns, nullability, storage types, and primary keys.
    @Override
    public void validateSchema() {
        check();
        List<String> errors = new ArrayList<String>();
        for (EntityModel model : models.values()) {
            List<Object[]> columns = describe(model.table());
            if (columns.isEmpty()) {
                errors.add("Missing table " + model.table());
                continue;
            }
            Attribute[] attrs = model.attributes();
            for (int i = 0; i < attrs.length; i++) {
                Attribute attr = attrs[i];
                Object[] found = null;
                for (Object[] column : columns) {
                    if (columnNameMatches(attr.column, (String) column[0])) {
                        found = column;
                        break;
                    }
                }
                if (found == null) {
                    errors.add("Missing column " + model.table() + "." + attr.column);
                    continue;
                }
                boolean primary = ((Number) found[3]).intValue() != 0;
                boolean required = ((Number) found[2]).intValue() != 0 || primary;
                if (primary != attr.id) {
                    errors.add("Primary key mismatch on " + model.table() + "." + attr.column);
                }
                if (attr.nullable == required) {
                    errors.add((required ? "Unexpected NOT NULL on " : "Missing NOT NULL on ")
                            + model.table() + "." + attr.column);
                }
                if (typeFamily((String) found[1]) != typeFamily(columnType(model, i))) {
                    errors.add("Storage type mismatch on " + model.table() + "." + attr.column);
                }
            }
            for (Object[] column : columns) {
                if (((Number) column[3]).intValue() != 0) {
                    boolean mapped = false;
                    for (Attribute attr : attrs) {
                        if (attr.id && columnNameMatches(attr.column, (String) column[0])) {
                            mapped = true;
                        }
                    }
                    if (!mapped) {
                        errors.add("Unmapped primary key column " + model.table() + "." + column[0]);
                    }
                }
            }
        }
        for (EntityModel owner : models.values()) {
            for (Relationship relation : owner.relationships()) {
                if (relation.many && relation.mappedBy.length() == 0) {
                    List<Object[]> columns = describe(relation.joinTable);
                    if (columns.isEmpty()) {
                        errors.add("Missing collection table " + relation.joinTable);
                        continue;
                    }
                    List<Attribute> expected = new ArrayList<Attribute>();
                    collectionKeys(expected, joinColumns(relation.joinColumn, owner), owner, true);
                    if (relation.element) {
                        expected.add(new Attribute("", relation.inverseJoinColumn, Elements.kind(relation.target),
                                false, false, true, false));
                        if (relation.mapKey.length() > 0) {
                            expected.add(new Attribute("", relation.mapKey, Attribute.TEXT, true, false, false, false));
                        }
                    } else {
                        EntityModel target = model(relation.target);
                        collectionKeys(expected, joinColumns(relation.inverseJoinColumn, target), target,
                                relation.orderColumn.length() == 0);
                    }
                    if (relation.orderColumn.length() > 0) {
                        expected.add(new Attribute("", relation.orderColumn, Attribute.INTEGER,
                                !relation.element || relation.mapKey.length() == 0, false, false, false));
                    }
                    validateCollectionColumns(relation.joinTable, expected, columns, errors);
                }
            }
        }
        if (!errors.isEmpty()) {
            throw new PersistenceException("Schema validation failed: " + errors);
        }
    }
    private void collectionKeys(List<Attribute> expected, String[] names, EntityModel model, boolean primary) {
        int[] ids = model.idIndexes();
        for (int i = 0; i < names.length; i++) {
            Attribute id = model.attributes()[ids[i]];
            expected.add(new Attribute("", names[i], id.kind, primary, false, false, false, id.declaredType));
        }
    }
    private void validateCollectionColumns(String table, List<Attribute> expected, List<Object[]> columns,
            List<String> errors) {
        for (Attribute attr : expected) {
            Object[] found = null;
            for (Object[] column : columns) {
                if (columnNameMatches(attr.column, (String) column[0])) {
                    found = column;
                    break;
                }
            }
            String name = table + "." + attr.column;
            if (found == null) {
                errors.add("Missing collection column " + name);
                continue;
            }
            boolean primary = ((Number) found[3]).intValue() != 0;
            boolean required = ((Number) found[2]).intValue() != 0 || primary;
            if (attr.id != primary) {
                errors.add("Primary key mismatch on collection " + name);
            }
            if (attr.nullable == required) {
                errors.add("Nullability mismatch on collection " + name);
            }
            String type = attr.declaredType == null ? sql.columnType(attr.kind) : attr.declaredType;
            if (typeFamily((String) found[1]) != typeFamily(type)) {
                errors.add("Storage type mismatch on collection " + name);
            }
        }
        for (Object[] column : columns) {
            if (((Number) column[3]).intValue() != 0) {
                boolean mapped = false;
                for (Attribute attr : expected) {
                    if (attr.id && columnNameMatches(attr.column, (String) column[0])) {
                        mapped = true;
                    }
                }
                if (!mapped) {
                    errors.add("Unmapped primary key column " + table + "." + column[0]);
                }
            }
        }
    }
    private static String[] split(String text, char separator) {
        List<String> parts = new ArrayList<String>();
        int start = 0;
        for (int i = 0; i <= text.length(); i++) {
            if (i == text.length() ||
                    (separator == ' ' ? Character.isWhitespace(text.charAt(i)) : text.charAt(i) == separator)) {
                String value = text.substring(start, i).trim();
                if (value.length() > 0) {
                    parts.add(value);
                }
                start = i + 1;
            }
        }
        return parts.toArray(new String[parts.size()]);
    }
    private int typeFamily(String type) {
        StringBuilder lower = new StringBuilder();
        for (int i = 0; i < type.length(); i++) {
            char ch = type.charAt(i);
            lower.append(ch >= 'A' && ch <= 'Z' ? (char) (ch + 32) : ch);
        }
        String name = lower.toString();
        String[] tokens = split(name.replace('(', ' '), ' ');
        String token = tokens.length == 0 ? "" : tokens[0];
        if ("postgresql".equals(sql.dialect()) && ("bool".equals(token) || "boolean".equals(token))) {
            return 6;
        }
        if (" int integer tinyint smallint mediumint bigint int2 int4 int8 serial smallserial bigserial serial2 serial4 serial8 bool boolean ".contains(" " + token + " ")) {
            return 1;
        }
        if (name.indexOf("char") >= 0 || name.indexOf("text") >= 0 || name.indexOf("clob") >= 0) {
            return 2;
        }
        if (name.indexOf("real") >= 0 || name.indexOf("double") >= 0 || name.indexOf("float") >= 0 ||
                name.indexOf("decimal") >= 0 || name.indexOf("numeric") >= 0) {
            return 3;
        }
        if (name.indexOf("blob") >= 0 || name.indexOf("binary") >= 0 || name.indexOf("bytea") >= 0) {
            return 4;
        }
        if (name.indexOf("date") >= 0 || name.indexOf("time") >= 0) {
            return 5;
        }
        return 0;
    }
    /// Counts a relationship without materializing its entities.
    @Override
    public long count(Object entity, String field) {
        check();
        EntityModel owner = model(entity.getClass());
        Relationship relation = owner.relationships()[owner.relationIndex(field)];
        Object id = owner.identifier(entity);
        autoFlush();
        if (relation.element) {
            return countJoin(relation.joinTable, joinColumns(relation.joinColumn, owner), owner.keyValues(id));
        }
        EntityModel target = model(relation.target);
        if (relation.column >= 0) {
            return owner.get(entity, relation.column) == null ? 0 : 1;
        }
        if (relation.mappedBy.length() > 0) {
            Relationship inverse = target.relationships()[target.relationIndex(relation.mappedBy)];
            if (inverse.column >= 0) {
                return inverseQuery(target, inverse, owner, id).count();
            }
            return countJoin(inverse.joinTable, joinColumns(inverse.inverseJoinColumn, owner), owner.keyValues(id));
        }
        return countJoin(relation.joinTable, joinColumns(relation.joinColumn, owner), owner.keyValues(id));
    }
    private long countJoin(String table, String[] columns, Object[] key) {
        return ((Number) read("SELECT COUNT(*) FROM " + q(table) + " WHERE " + matches(columns, null), key,
                        new int[] {Attribute.BIGINT})
                        .get(0)[0])
                .longValue();
    }
    private QueryImpl inverseQuery(EntityModel target, Relationship inverse, EntityModel owner, Object id) {
        QueryImpl query = query(target.type());
        Object[] values = owner.keyValues(id);
        for (int i = 0; i < values.length; i++) {
            query.eq(target.attributes()[inverse.column + i].field, values[i]);
        }
        return query;
    }
    <T> List<T> hydrateAll(EntityModel<T> model, List<Object[]> rows, List<String> fetches) {
        List<T> result = new ArrayList<T>();
        deferredFetch++;
        try {
            for (Object[] row : rows) {
                result.add(hydrate(model, row));
            }
        } finally {
            deferredFetch--;
        }
        fetchAll(model, result, fetches);
        return result;
    }
    private <T> void fetchAll(EntityModel<T> owner, List<T> roots, List<String> fetches) {
        Relationship[] relations = owner.relationships();
        loading++;
        try {
            for (int i = 0; i < relations.length; i++) {
                Relationship relation = relations[i];
                if (relation.lazy && !fetches.contains(relation.field)) {
                    continue;
                }
                List<Object> keys = new ArrayList<Object>();
                List<T> unloaded = new ArrayList<T>();
                for (T entity : roots) {
                    EntityState state = state(entity);
                    if (state == null || state.loaded[i] || state.fetching[i]) {
                        continue;
                    }
                    unloaded.add(entity);
                    Object key = relation.column >= 0 ? state.keys[i] : owner.identifier(entity);
                    if (key != null && !keys.contains(key)) {
                        keys.add(key);
                    }
                }
                if (unloaded.isEmpty()) {
                    continue;
                }
                if (relation.element) {
                    for (T entity : unloaded) {
                        initialize(entity, i);
                    }
                    continue;
                }
                EntityModel target = model(relation.target);
                for (T entity : unloaded) {
                    state(entity).fetching[i] = true;
                }
                try {
                    if (owner.idIndexes().length > 1 || target.idIndexes().length > 1 ||
                            relation.orderBy.length() > 0 || relation.mapKey.length() > 0) {
                        for (T entity : unloaded) {
                            initialize(entity, i);
                        }
                    } else if (relation.column >= 0) {
                        if (!keys.isEmpty()) {
                            readBatches(relation.target, target.attributes()[target.idIndex()].field, keys);
                        }
                        for (T entity : unloaded) {
                            initialize(entity, i);
                        }
                    } else if (relation.mappedBy.length() > 0) {
                        Relationship inverse = target.relationships()[target.relationIndex(relation.mappedBy)];
                        if (inverse.column < 0) {
                            for (T entity : unloaded) {
                                initialize(entity, i);
                            }
                            continue;
                        }
                        List children = readBatches(relation.target, target.attributes()[inverse.column].field, keys);
                        for (T entity : unloaded) {
                            List matches = new ArrayList();
                            Object id = owner.identifier(entity);
                            for (Object child : children) {
                                if (same(target.get(child, inverse.column), id)) {
                                    matches.add(child);
                                }
                            }
                            if (!relation.many && matches.size() > 1) {
                                throw new PersistenceException("One-to-one relationship returned multiple rows");
                            }
                            Object value = relation.many ? matches : matches.isEmpty() ? null : matches.get(0);
                            owner.relation(entity, i, value);
                            state(entity).loaded[i] = true;
                            Entry entry = entries.get(entity);
                            if (entry != null) {
                                entry.collections[i] = relationshipKeys(relation, value);
                            }
                        }
                    } else {
                        for (T entity : unloaded) {
                            initialize(entity, i);
                        }
                    }
                } finally {
                    for (T entity : unloaded) {
                        state(entity).fetching[i] = false;
                    }
                }
            }
        } finally {
            loading--;
        }
    }

    private List readBatches(Class type, String field, List<Object> keys) {
        List result = new ArrayList();
        for (int start = 0; start < keys.size(); start += 400) {
            result.addAll(
                    query(type).in(field, keys.subList(start, Math.min(start + 400, keys.size())).toArray()).list());
        }
        return result;
    }

    @Override
    public boolean isLoaded(Object entity, String field) {
        check();
        EntityModel model = model(entity.getClass());
        EntityState state = state(entity);
        return state == null || state.loaded[model.relationIndex(field)];
    }
    @Override
    public void initialize(Object entity, String field) {
        initializeForAccess(entity, model(entity.getClass()).relationIndex(field));
    }
    void initializeForAccess(Object entity, int index) {
        check();
        EntityState state = state(entity);
        if (state == null || state.loaded[index]) {
            return;
        }
        if (!state.attached || !sameInstance(state.session, this)) {
            throw new LazyInitializationException("Entity is detached");
        }
        autoFlush();
        initialize(entity, index);
    }
    void beforeAssignment(Object entity, int index) {
        Relationship relation = model(entity.getClass()).relationships()[index];
        if (relation.many || relation.orphanRemoval && relation.column < 0) {
            initialize(entity, index);
        }
    }
    void initialize(Object entity, int index) {
        check();
        EntityState state = state(entity);
        if (state == null || state.loaded[index]) {
            return;
        }
        if (!state.attached || !sameInstance(state.session, this)) {
            throw new LazyInitializationException("Entity is detached");
        }
        EntityModel model = model(entity.getClass());
        Relationship relation = model.relationships()[index];
        if (relation.element) {
            Object value = loadElements(model, entity, relation);
            model.relation(entity, index, value);
            state.loaded[index] = true;
            Entry entry = entries.get(entity);
            if (entry != null) {
                entry.collections[index] = elementRows(relation, value);
            }
            return;
        }
        EntityModel target = model(relation.target);
        Object value;
        loading++;
        try {
            if (relation.column >= 0) {
                value = state.keys[index] == null ? null : find(relation.target, state.keys[index]);
                if (value == null && state.keys[index] != null) {
                    throw new PersistenceException(
                            "Missing relationship target: " + model.type().getName() + "." + relation.field);
                }
            } else {
                List found;
                if (relation.mappedBy.length() > 0) {
                    Relationship inverse = target.relationships()[target.relationIndex(relation.mappedBy)];
                    if (inverse.column >= 0) {
                        QueryImpl query = inverseQuery(target, inverse, model, model.identifier(entity));
                        for (String clause : split(relation.orderBy, ',')) {
                            if (clause.trim().length() > 0) {
                                String[] parts = split(clause.trim(), ' ');
                                query.orderBy(parts[0], parts.length == 1 || !"DESC".equalsIgnoreCase(parts[1]));
                            }
                        }
                        found = query.list();
                    } else {
                        found = readJoin(model, target, inverse.joinTable, inverse.inverseJoinColumn,
                                inverse.joinColumn, model.identifier(entity), relation);
                    }
                } else {
                    found = readJoin(model, target, relation.joinTable, relation.joinColumn, relation.inverseJoinColumn,
                            model.identifier(entity), relation);
                }
                if (relation.many) {
                    value = found;
                } else {
                    if (found.size() > 1) {
                        throw new PersistenceException(
                                "One-to-one relationship returned multiple rows: " + relation.field);
                    }
                    value = found.isEmpty() ? null : found.get(0);
                }
            }
            model.relation(entity, index, value);
            state.loaded[index] = true;
            Entry entry = entries.get(entity);
            if (entry != null) {
                entry.collections[index] = relationshipKeys(relation, value);
            }
        } finally {
            loading--;
        }
    }
    private List readJoin(EntityModel owner, EntityModel target, String table, String ownerColumn, String targetColumn,
            Object id, Relationship relation) {
        if (table.length() == 0) {
            throw new PersistenceException("Missing join table");
        }
        String[] targetColumns = joinColumns(targetColumn, target);
        String[] ownerColumns = joinColumns(ownerColumn, owner);
        StringBuilder statement = new StringBuilder("SELECT ");
        for (Attribute attribute : target.attributes()) {
            if (statement.length() > 7) {
                statement.append(", ");
            }
            statement.append("t.").append(q(attribute.column));
        }
        statement.append(" FROM ")
                .append(tableSource(target))
                .append(" t INNER JOIN ")
                .append(q(table))
                .append(" l ON ");
        statement.append(joinEquality(keyColumns(target), "t", targetColumns, "l"));
        statement.append(" WHERE ").append(matches(ownerColumns, "l"));
        if (relation.orderBy.length() > 0) {
            statement.append(" ORDER BY ");
            boolean first = true;
            for (String clause : split(relation.orderBy, ',')) {
                String[] parts = split(clause.trim(), ' ');
                if (!first) {
                    statement.append(',');
                }
                first = false;
                Attribute attribute = target.attributes()[target.index(parts[0])];
                statement.append(orderBy("t." + q(attribute.column),
                        !(parts.length > 1 && "DESC".equalsIgnoreCase(parts[1])), attribute.kind));
            }
        } else if (relation.orderColumn.length() > 0) {
            statement.append(" ORDER BY l.").append(q(relation.orderColumn));
        }
        return hydrateAll(
                target, read(statement.toString(), owner.keyValues(id), kinds(target)), new ArrayList<String>());
    }
    private EntityState state(Object entity) {
        return entity instanceof ManagedEntity ? ((ManagedEntity) entity).__cn1OrmState() : null;
    }
    private void attachState(Entry entry, boolean loaded, Object[] values) {
        Relationship[] relations = entry.model.relationships();
        entry.collections = new List[relations.length];
        if (relations.length == 0 && !(entry.entity instanceof ManagedEntity)) {
            return;
        }
        if (!(entry.entity instanceof ManagedEntity)) {
            throw new PersistenceException("Entity has not been enhanced: " + entry.model.type().getName());
        }
        EntityState state = new EntityState(this, entry.entity, relations.length);
        ((ManagedEntity) entry.entity).__cn1OrmState(state);
        for (int i = 0; i < relations.length; i++) {
            state.loaded[i] = loaded || !entry.model.hasRelationship(entry.entity, i);
            if (values != null && relations[i].column >= 0) {
                EntityModel target = model(relations[i].target);
                int count = target.idIndexes().length;
                Object[] key = new Object[count];
                boolean any = false;
                for (int part = 0; part < count; part++) {
                    key[part] = values[relations[i].column + part];
                    any |= key[part] != null;
                }
                if (any) {
                    target.keyValues(key);
                    state.keys[i] = count == 1 ? key[0] : Identifier.of(key);
                }
            }
            if (state.loaded[i]) {
                entry.collections[i] = new ArrayList();
            } else if (relations[i].column >= 0) {
                entry.collections[i] = new ArrayList();
                if (state.keys[i] != null) {
                    entry.collections[i].add(state.keys[i]);
                }
            }
        }
    }
    private void cascadePersist(Entry entry) {
        IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<Object, Boolean>();
        visited.put(entry.entity, Boolean.TRUE);
        cascadePersist(entry, visited);
    }
    private void cascadePersist(Entry entry, IdentityHashMap<Object, Boolean> visited) {
        Relationship[] relations = entry.model.relationships();
        for (int i = 0; i < relations.length; i++) {
            if ((relations[i].cascade & Relationship.PERSIST) == 0) {
                continue;
            }
            EntityState state = state(entry.entity);
            if (state != null && !state.loaded[i]) {
                continue;
            }
            Object value = entry.model.relation(entry.entity, i);
            if (value == null) {
                continue;
            }
            if (relations[i].many) {
                for (Object child : relatedValues(value)) {
                    persistInternal(child, visited);
                }
            } else {
                persistInternal(value, visited);
            }
        }
    }
    private static final class ElementRow {
        final Object key;
        final Object value;
        ElementRow(Object key, Object value) {
            this.key = key;
            this.value = Values.storage(value);
        }
        @Override
        public boolean equals(Object other) {
            return other instanceof ElementRow && same(key, ((ElementRow) other).key) &&
                    same(value, ((ElementRow) other).value);
        }
        @Override
        public int hashCode() {
            return (key == null ? 0 : key.hashCode()) * 31 + (value == null ? 0 : value.hashCode());
        }
    }
    private List elementRows(Relationship relation, Object collection) {
        List result = new ArrayList();
        if (collection == null) {
            return result;
        }
        if (relation.mapKey.length() > 0) {
            for (Object item : ((Map) collection).entrySet()) {
                Map.Entry entry = (Map.Entry) item;
                if (!(entry.getKey() instanceof String)) {
                    throw new PersistenceException("Element maps require non-null String keys");
                }
                if (((String) entry.getKey()).length() > 255) {
                    throw new PersistenceException("Element map key exceeds the portable limit of 255 characters: "
                            + relation.field);
                }
                checkElement(relation, entry.getValue());
                result.add(new ElementRow(entry.getKey(), entry.getValue()));
            }
        } else {
            for (Object value : relatedValues(collection)) {
                checkElement(relation, value);
                result.add(new ElementRow(null, value));
            }
        }
        return result;
    }
    private void checkElement(Relationship relation, Object value) {
        if (value != null && !relation.target.isInstance(value)) {
            throw new PersistenceException("Wrong element type: " + relation.field);
        }
    }
    private void createElementTable(EntityModel owner, Relationship relation, List<ForeignKey> foreignKeys) {
        String[] columns = joinColumns(relation.joinColumn, owner);
        StringBuilder definition = new StringBuilder("CREATE TABLE ").append(q(relation.joinTable)).append(" (");
        appendJoinDefinition(definition, columns, owner);
        definition.append(", ")
                .append(q(relation.orderColumn))
                .append(" INTEGER NOT NULL, ")
                .append(q(relation.inverseJoinColumn))
                .append(' ')
                .append(sql.columnType(Elements.kind(relation.target)));
        if (relation.mapKey.length() > 0) {
            String type = sql.assignedKeyColumn(Attribute.TEXT).replace(" PRIMARY KEY", "").replace(" NOT NULL", "");
            definition.append(", ").append(q(relation.mapKey)).append(' ').append(type).append(" NOT NULL");
        }
        definition.append(", PRIMARY KEY (")
                .append(quoted(columns))
                .append(", ")
                .append(q(relation.mapKey.length() > 0 ? relation.mapKey : relation.orderColumn))
                .append(')');
        ForeignKey key = new ForeignKey(relation.joinTable, columns, owner.table(), keyColumns(owner));
        if ("sqlite".equals(sql.dialect())) {
            definition.append(", ").append(foreignDeclaration(key));
        } else {
            foreignKeys.add(key);
        }
        write(definition.append(')').toString(), new Object[0]);
    }
    private Object loadElements(EntityModel owner, Object entity, Relationship relation) {
        boolean map = relation.mapKey.length() > 0;
        String selection = q(relation.inverseJoinColumn) + (map ? ", " + q(relation.mapKey) : "");
        int[] kinds = map ? new int[] {Elements.kind(relation.target), Attribute.TEXT}
                          : new int[] {Elements.kind(relation.target)};
        List<Object[]> rows = read("SELECT " + selection + " FROM " + q(relation.joinTable) + " WHERE " +
                                           matches(joinColumns(relation.joinColumn, owner), null) + " ORDER BY " +
                                           q(relation.orderColumn),
                owner.keyValues(owner.identifier(entity)), kinds);
        List values = new ArrayList();
        Map mapping = new LinkedHashMap();
        for (Object[] row : rows) {
            Object value = Elements.read(relation.target, row[0]);
            if (map) {
                mapping.put(row[1], value);
            } else {
                values.add(value);
            }
        }
        return map ? mapping : values;
    }
    private void syncElements(Entry entry, int index, Relationship relation, Object collection) {
        List rows = elementRows(relation, collection);
        if (rows.equals(entry.collections[index])) {
            return;
        }
        String[] ownerColumns = joinColumns(relation.joinColumn, entry.model);
        Object[] owner = entry.model.keyValues(entry.model.identifier(entry.entity));
        write("DELETE FROM " + q(relation.joinTable) + " WHERE " + matches(ownerColumns, null), owner);
        for (int i = 0; i < rows.size(); i++) {
            ElementRow row = (ElementRow) rows.get(i);
            boolean map = relation.mapKey.length() > 0;
            Object[] values = map ? new Object[] {Integer.valueOf(i), row.value, row.key}
                                  : new Object[] {Integer.valueOf(i), row.value};
            Object[] args = concat(owner, values);
            write("INSERT INTO " + q(relation.joinTable) + " (" + quoted(ownerColumns) + ", " +
                            q(relation.orderColumn) + ", " + q(relation.inverseJoinColumn) +
                            (map ? ", " + q(relation.mapKey) : "") + ") VALUES (" + placeholders(args.length) + ")",
                    args);
        }
        entry.collections[index] = rows;
    }

    private static Iterable relatedValues(Object value) {
        return value instanceof Map ? ((Map) value).values() : (Iterable) value;
    }
    private List relationshipKeys(Relationship relation, Object value) {
        if (relation.many) {
            return relationKeys(relation, value);
        }
        List result = new ArrayList();
        if (value != null) {
            EntityModel target = model(relation.target);
            result.add(target.identifier(value));
        }
        return result;
    }
    private List relationKeys(Relationship relation, Object value) {
        if (relation.element) {
            return elementRows(relation, value);
        }
        List result = new ArrayList();
        if (value != null) {
            EntityModel target = model(relation.target);
            if (value instanceof Map) {
                for (Object item : ((Map) value).entrySet()) {
                    Map.Entry mapping = (Map.Entry) item;
                    if (mapping.getKey() == null || mapping.getValue() == null ||
                            !mapping.getKey().equals(
                                    target.domainValue(mapping.getValue(), target.index(relation.mapKey)))) {
                        throw new PersistenceException(
                                "Map key does not match its entity attribute: " + relation.field);
                    }
                }
            }
            for (Object child : relatedValues(value)) {
                if (child == null) {
                    throw new PersistenceException("Null relationship element: " + relation.field);
                }
                Object id = target.identifier(child);
                if (id == null || (target.attributes()[target.idIndex()].generated && id instanceof Number &&
                                          ((Number) id).longValue() == 0)) {
                    throw new PersistenceException("Transient association without cascade PERSIST: " + relation.field);
                }
                result.add(id);
            }
        }
        return result;
    }
    private void syncCollections(Entry entry, boolean removals) {
        Relationship[] relations = entry.model.relationships();
        EntityState state = state(entry.entity);
        for (int i = 0; i < relations.length; i++) {
            Relationship relation = relations[i];
            if (state == null || !state.loaded[i]) {
                continue;
            }
            if (removals && (relation.element || !relation.many || relation.mappedBy.length() > 0)) {
                continue;
            }
            Object value = entry.model.relation(entry.entity, i);
            if (relation.element) {
                syncElements(entry, i, relation, value);
                continue;
            }
            List keys = relationshipKeys(relation, value);
            List old = entry.collections[i];
            if (old == null) {
                old = new ArrayList();
            }
            Object owner = entry.model.identifier(entry.entity);
            if (relation.many && relation.mappedBy.length() == 0) {
                EntityModel target = model(relation.target);
                String[] ownerColumns = joinColumns(relation.joinColumn, entry.model);
                String[] targetColumns = joinColumns(relation.inverseJoinColumn, target);
                Object[] ownerKey = entry.model.keyValues(owner);
                if (relation.orderColumn.length() > 0) {
                    if (!keys.equals(old)) {
                        if (removals) {
                            write("DELETE FROM " + q(relation.joinTable) + " WHERE " + matches(ownerColumns, null),
                                    ownerKey);
                            continue;
                        }
                        for (int position = 0; position < keys.size(); position++) {
                            Object[] args = concat(concat(ownerKey, target.keyValues(keys.get(position))),
                                    new Object[] {Integer.valueOf(position)});
                            write("INSERT INTO " + q(relation.joinTable) + " (" + quoted(ownerColumns) + ", " +
                                            quoted(targetColumns) + ", " + q(relation.orderColumn) + ") VALUES (" +
                                            placeholders(args.length) + ")",
                                    args);
                        }
                    }
                } else {
                    for (Object id : old) {
                        if (removals && !keys.contains(id)) {
                            write("DELETE FROM " + q(relation.joinTable) + " WHERE " + matches(ownerColumns, null) +
                                            " AND " + matches(targetColumns, null),
                                    concat(ownerKey, target.keyValues(id)));
                        }
                    }
                    for (Object id : keys) {
                        if (!removals && !old.contains(id)) {
                            Object[] args = concat(ownerKey, target.keyValues(id));
                            write("INSERT INTO " + q(relation.joinTable) + " (" + quoted(ownerColumns) + ", " +
                                            quoted(targetColumns) + ") VALUES (" + placeholders(args.length) + ")",
                                    args);
                        }
                    }
                }
            }
            if (removals) {
                continue;
            }
            if (relation.orphanRemoval) {
                for (Object id : old) {
                    if (!keys.contains(id)) {
                        Object orphan = find(relation.target, id);
                        if (orphan != null) {
                            remove(orphan);
                        }
                    }
                }
            }
            entry.collections[i] = keys;
        }
    }

    <T> EntityModel<T> model(Class<T> type) {
        EntityModel<T> model = (EntityModel<T>) models.get(type.getName());
        if (model == null) {
            throw new PersistenceException("No generated model registered for " + type.getName());
        }
        return model;
    }
    void check() {
        if (closed) {
            throw new PersistenceException("Session is closed");
        }
    }
    void requireTransaction() {
        check();
        if (!transaction) {
            throw new PersistenceException("An active transaction is required");
        }
    }
    void autoFlush() {
        check();
        if (transaction && !flushing && loading == 0) {
            flush();
        }
    }
    String q(String value) {
        return sql.quote(value);
    }
    String limit(int count, int offset) {
        return sql.limit(count, offset);
    }
    String discriminatorCondition(EntityModel model, String alias) {
        if (model.discriminatorIndex() < 0) {
            return "";
        }
        String[] values = model.discriminatorValues();
        if (values.length == 0) {
            return "1=0";
        }
        StringBuilder condition = new StringBuilder(alias == null ? "" : alias + ".")
                                          .append(q(model.attributes()[model.discriminatorIndex()].column))
                                          .append(" IN (");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                condition.append(',');
            }
            condition.append("'").append(values[i].replace("'", "''")).append("'");
        }
        return condition.append(')').toString();
    }
    String tableSource(EntityModel model) {
        String filter = discriminatorCondition(model, null);
        return filter.length() == 0 ? q(model.table())
                                    : "(SELECT * FROM " + q(model.table()) + " WHERE " + filter + ")";
    }
    String select(EntityModel model) {
        StringBuilder out = new StringBuilder("SELECT ");
        Attribute[] attrs = model.attributes();
        for (int i = 0; i < attrs.length; i++) {
            if (i > 0) {
                out.append(", ");
            }
            out.append(q(attrs[i].column));
        }
        return out.append(" FROM ").append(tableSource(model)).append(" cn1_entity").toString();
    }
    int[] kinds(EntityModel model) {
        Attribute[] attrs = model.attributes();
        int[] kinds = new int[attrs.length];
        for (int i = 0; i < attrs.length; i++) {
            kinds[i] = attrs[i].kind;
        }
        return kinds;
    }
    List<Object[]> read(String statement, Object[] params, int[] kinds) {
        check();
        checkParameterCount(params.length);
        try {
            return sql.query(statement, params, kinds);
        } catch (IOException e) {
            if (transaction) {
                rollbackOnly = true;
            }
            throw failure(e);
        }
    }
    int write(String statement, Object[] params) {
        check();
        checkParameterCount(params.length);
        try {
            return sql.execute(statement, params);
        } catch (IOException e) {
            if (transaction) {
                rollbackOnly = true;
            }
            throw failure(e);
        }
    }
    <T> T hydrate(EntityModel<T> model, Object[] values) {
        try {
            return hydrateInternal(model, values);
        } catch (RuntimeException error) {
            if (transaction) {
                rollbackOnly = true;
            }
            clear();
            throw error;
        }
    }
    private <T> T hydrateInternal(EntityModel<T> model, Object[] values) {
        if (model.discriminatorIndex() >= 0) {
            Object tag = values[model.discriminatorIndex()];
            EntityModel selected = null;
            for (EntityModel candidate : models.values()) {
                if (candidate.hierarchyRoot() == model.hierarchyRoot() && candidate.discriminatorValue().equals(tag)) {
                    selected = candidate;
                    break;
                }
            }
            if (selected == null || !model.type().isAssignableFrom(selected.type())) {
                throw new PersistenceException("Unknown or incompatible discriminator: " + tag);
            }
            model = selected;
        }
        Key key = new Key(model.hierarchyRoot().getName(), model.identifierFromRow(values));
        T entity = (T) identities.get(key);
        if (entity != null) {
            checkManagedIdentity(entries.get(entity));
            return entity;
        }
        entity = model.create();
        Entry entry = new Entry(model, entity, null);
        attachState(entry, false, values);
        assign(model, entity, values);
        claim(model, entity);
        entries.put(entity, entry);
        entry.snapshot = snapshot(model, entity);
        Relationship[] relations = model.relationships();
        loading++;
        try {
            if (deferredFetch == 0) {
                for (int i = 0; i < relations.length; i++) {
                    if (!relations[i].lazy) {
                        initialize(entity, i);
                    }
                }
            }
        } finally {
            loading--;
        }
        model.lifecycle(entity, 6);
        return entity;
    }
    private void insert(Entry entry, List<Entry> inserted) {
        if (!entry.fresh || entry.inserting || entry.removed) {
            return;
        }
        entry.inserting = true;
        inserted.add(entry);
        EntityModel model = entry.model;
        Attribute[] attrs = model.attributes();
        model.lifecycle(entry.entity, 0);
        cascadePersist(entry);
        checkTransientAssociations(entry);
        if (!attrs[model.idIndex()].generated && !same(entry.initialId, model.identifier(entry.entity))) {
            throw new PersistenceException("Managed primary key cannot change");
        }
        for (int i = 0; i < model.relationships().length; i++) {
            Relationship relation = model.relationships()[i];
            if (relation.column < 0) {
                continue;
            }
            Object target = model.relation(entry.entity, i);
            Entry dependency = entries.get(target);
            if (dependency != null && dependency.fresh) {
                insert(dependency, inserted);
            }
        }
        StringBuilder cols = new StringBuilder();
        StringBuilder marks = new StringBuilder();
        List<Object> args = new ArrayList<Object>();
        int version = model.versionIndex();
        if (version >= 0) {
            model.set(entry.entity, version, Long.valueOf(0));
        }
        for (int i = 0; i < attrs.length; i++) {
            if (attrs[i].generated) {
                continue;
            }
            if (!args.isEmpty()) {
                cols.append(", ");
                marks.append(", ");
            }
            cols.append(q(attrs[i].column));
            marks.append('?');
            Object bound = model.get(entry.entity, i);
            validateTextKey(model, i, bound);
            for (int ri = 0; ri < model.relationships().length; ri++) {
                if (model.relationships()[ri].column >= 0 && i >= model.relationships()[ri].column &&
                        i < model.relationships()[ri].column +
                                        model(model.relationships()[ri].target).idIndexes().length) {
                    Entry dependency = entries.get(model.relation(entry.entity, ri));
                    if (dependency != null && dependency.fresh &&
                            (dependency.model.attributes()[dependency.model.idIndex()].generated || "mysql".equals(sql.dialect()))) {
                        // MySQL also defers known keys until their rows exist.
                        // Required cycles are rejected when the session opens.
                        bound = null;
                        if (entry.deferredForeignKeys == null) {
                            entry.deferredForeignKeys = new boolean[attrs.length];
                        }
                        entry.deferredForeignKeys[i] = true;
                    }
                }
            }
            args.add(bound);
        }
        String statement = args.isEmpty()
                                   ? sql.insertDefaults(q(model.table()))
                                   : "INSERT INTO " + q(model.table()) + " (" + cols + ") VALUES (" + marks + ")";
        int id = model.idIndex();
        if (attrs[id].generated) {
            try {
                model.set(entry.entity, id, Long.valueOf(sql.insert(statement, args.toArray(), attrs[id].column)));
            } catch (IOException e) {
                throw failure(e);
            }
        } else {
            write(statement, args.toArray());
        }
        claim(model, entry.entity);
        entry.fresh = false;
        entry.inserting = false;
        entry.snapshot = snapshot(model, entry.entity);
        int arg = 0;
        for (int i = 0; i < attrs.length; i++) {
            if (!attrs[i].generated) {
                Object value = args.get(arg++);
                entry.snapshot[i] = value instanceof byte[] ? ((byte[]) value).clone() : value;
            }
        }
        model.lifecycle(entry.entity, 1);
    }
    private void completeInsert(Entry entry) {
        checkManagedIdentity(entry);
        checkTransientAssociations(entry);
        EntityModel model = entry.model;
        int version = model.versionIndex();
        if (version >= 0 && !same(model.get(entry.entity, version), entry.snapshot[version])) {
            throw new PersistenceException("Version is managed by the ORM");
        }
        if (entry.deferredForeignKeys == null) {
            return;
        }
        // Finish identity-key cycles after all generated IDs exist. These are
        // insert writes, with no update callbacks or version increment.
        List<Object> args = new ArrayList<Object>();
        StringBuilder columns = new StringBuilder();
        for (int i = 0; i < entry.deferredForeignKeys.length; i++) {
            if (!entry.deferredForeignKeys[i]) {
                continue;
            }
            if (columns.length() > 0) {
                columns.append(", ");
            }
            columns.append(q(model.attributes()[i].column)).append(" = ?");
            Object value = model.get(entry.entity, i);
            args.add(value);
        }
        args.addAll(Arrays.asList(model.keyValues(model.identifier(entry.entity))));
        if (write("UPDATE " + q(model.table()) + " SET " + columns + " WHERE " + keyCondition(model, null),
                    args.toArray()) != 1) {
            throw new OptimisticLockException("Inserted row no longer exists: " + model.table());
        }
        int arg = 0;
        for (int i = 0; i < entry.deferredForeignKeys.length; i++) {
            if (entry.deferredForeignKeys[i]) {
                Object value = args.get(arg++);
                entry.snapshot[i] = value instanceof byte[] ? ((byte[]) value).clone() : value;
            }
        }
        entry.deferredForeignKeys = null;
    }
    private void checkTransientAssociations(Entry entry) {
        EntityModel owner = entry.model;
        Attribute[] attributes = owner.attributes();
        for (int i = 0; i < attributes.length; i++) {
            validateTextKey(owner, i, owner.get(entry.entity, i));
            boolean association = false;
            for (Relationship relation : owner.relationships()) {
                if (relation.column >= 0 && i >= relation.column
                        && i < relation.column + model(relation.target).idIndexes().length) {
                    association = true;
                }
            }
            if (!association && !attributes[i].generated && !attributes[i].version && owner.required(entry.entity, i)
                    && owner.get(entry.entity, i) == null) {
                throw new PersistenceException("Required attribute is null: " + owner.type().getName() + "."
                        + attributes[i].field);
            }
        }
        Relationship[] relations = owner.relationships();
        for (int i = 0; i < relations.length; i++) {
            Relationship relation = relations[i];
            if (relation.column < 0) {
                continue;
            }
            Object target = owner.relation(entry.entity, i);
            if (target == null && owner.required(entry.entity, relation.column)
                    && owner.get(entry.entity, relation.column) == null) {
                throw new PersistenceException("Required relationship is null: " + relation.field);
            }
            if (target != null && entries.get(target) == null) {
                EntityModel targetModel = model(relation.target);
                Object targetId = targetModel.identifier(target);
                if (targetId == null || (targetModel.attributes()[targetModel.idIndex()].generated &&
                        targetId instanceof Number && ((Number) targetId).longValue() == 0)) {
                    throw new PersistenceException("Transient association without cascade PERSIST: " + relation.field);
                }
                try {
                    targetModel.keyValues(targetId);
                } catch (IllegalArgumentException error) {
                    throw new PersistenceException("Incomplete relationship identifier: " + relation.field, error);
                }
            }
        }
    }
    private void insertPending(List<Entry> inserted) {
        int first = inserted.size();
        int previous;
        do {
            previous = inserted.size();
            for (Entry entry : new ArrayList<Entry>(entries.values())) {
                insert(entry, inserted);
            }
        } while (previous != inserted.size());
        for (int i = first; i < inserted.size(); i++) {
            completeInsert(inserted.get(i));
        }
    }
    private boolean update(Entry entry, List<Entry> inserted) {
        checkTransientAssociations(entry);
        EntityModel model = entry.model;
        Attribute[] attrs = model.attributes();
        Object[] now = snapshot(model, entry.entity);
        int version = model.versionIndex();
        if (!same(model.identifierFromRow(now), model.identifierFromRow(entry.snapshot))) {
            throw new PersistenceException("Managed primary key cannot change");
        }
        if (version >= 0 && !same(now[version], entry.snapshot[version])) {
            throw new PersistenceException("Version is managed by the ORM");
        }
        boolean dirty = false;
        for (int i = 0; i < attrs.length; i++) {
            if (!attrs[i].id && !attrs[i].version && !same(now[i], entry.snapshot[i])) {
                dirty = true;
            }
        }
        EntityState state = state(entry.entity);
        Relationship[] relations = model.relationships();
        for (int i = 0; i < relations.length; i++) {
            if (state != null && state.loaded[i] && relations[i].many) {
                List current = relationKeys(relations[i], model.relation(entry.entity, i));
                List previous = entry.collections[i];
                if (previous == null ||
                        ((relations[i].element || relations[i].orderColumn.length() > 0)
                                        ? !current.equals(previous)
                                        : current.size() != previous.size() || !current.containsAll(previous))) {
                    dirty = true;
                }
            }
        }
        if (!dirty) {
            return false;
        }
        model.lifecycle(entry.entity, 2);
        cascadePersist(entry);
        insertPending(inserted);
        checkTransientAssociations(entry);
        now = snapshot(model, entry.entity);
        if (!same(model.identifierFromRow(now), model.identifierFromRow(entry.snapshot)) ||
                version >= 0 && !same(now[version], entry.snapshot[version])) {
            throw new PersistenceException("Lifecycle callback changed an identifier or version");
        }
        List<Object> args = new ArrayList<Object>();
        StringBuilder set = new StringBuilder();
        for (int i = 0; i < attrs.length; i++) {
            if (attrs[i].id || attrs[i].version || same(now[i], entry.snapshot[i])) {
                continue;
            }
            if (!args.isEmpty()) {
                set.append(", ");
            }
            set.append(q(attrs[i].column)).append(" = ?");
            args.add(now[i]);
        }
        if (args.isEmpty() && version < 0) {
            return true;
        }
        Long next = null;
        if (version >= 0) {
            long previous = ((Number) entry.snapshot[version]).longValue();
            if (previous == (attrs[version].kind == Attribute.INTEGER ? Integer.MAX_VALUE : Long.MAX_VALUE)) {
                throw new PersistenceException("Version overflow");
            }
            next = Long.valueOf(previous + 1);
            if (set.length() > 0) {
                set.append(", ");
            }
            set.append(q(attrs[version].column)).append(" = ?");
            args.add(next);
        }
        String where = condition(entry, args);
        int changed = write("UPDATE " + q(model.table()) + " SET " + set + " WHERE " + where, args.toArray());
        if (changed != 1) {
            throw new OptimisticLockException("Stale or missing row: " + model.table());
        }
        if (version >= 0) {
            model.set(entry.entity, version, next);
        }
        entry.snapshot = snapshot(model, entry.entity);
        return true;
    }
    private void prepareDeletes() {
        for (Entry entry : new ArrayList<Entry>(entries.values())) {
            if (entry.removed) {
                checkManagedIdentity(entry);
                StringBuilder changes = new StringBuilder();
                for (Relationship relation : entry.model.relationships()) {
                    if (relation.column >= 0 && entry.model.attributes()[relation.column].nullable) {
                        for (String column : foreignColumns(entry.model, relation)) {
                            if (changes.length() > 0) {
                                changes.append(", ");
                            }
                            changes.append(q(column)).append(" = NULL");
                        }
                    }
                }
                if (changes.length() > 0) {
                    List<Object> args = new ArrayList<Object>();
                    String where = condition(entry, args);
                    if (write("UPDATE " + q(entry.model.table()) + " SET " + changes + " WHERE " + where,
                                args.toArray()) != 1) {
                        throw new OptimisticLockException("Stale row during deletion: " + entry.model.table());
                    }
                }
            }
        }
    }
    private void delete(Entry entry) {
        if (!entries.containsKey(entry.entity)) {
            return;
        }
        if (entry.deleting) {
            if ("sqlite".equals(sql.dialect()) || "postgresql".equals(sql.dialect())) {
                return;
            }
            throw new PersistenceException("Cyclic non-nullable delete dependencies");
        }
        entry.deleting = true;
        for (Entry dependent : new ArrayList<Entry>(entries.values())) {
            if (!sameInstance(dependent, entry) && dependent.removed) {
                for (Relationship relation : dependent.model.relationships()) {
                    if (relation.column < 0 || dependent.model.attributes()[relation.column].nullable ||
                            !relation.target.isInstance(entry.entity)) {
                        continue;
                    }
                    Object[] fk = new Object[entry.model.idIndexes().length];
                    for (int i = 0; i < fk.length; i++) {
                        fk[i] = dependent.snapshot[relation.column + i];
                    }
                    Object key = fk.length == 1 ? fk[0] : Identifier.of(fk);
                    if (same(key, entry.model.identifier(entry.entity))) {
                        delete(dependent);
                    }
                }
            }
        }
        entry.model.lifecycle(entry.entity, 4);
        checkManagedIdentity(entry);
        for (EntityModel owner : models.values()) {
            for (Relationship relation : owner.relationships()) {
                if (relation.many && relation.mappedBy.length() == 0) {
                    if (owner.type().isInstance(entry.entity)) {
                        write("DELETE FROM " + q(relation.joinTable) + " WHERE " +
                                        matches(joinColumns(relation.joinColumn, owner), null),
                                owner.keyValues(owner.identifier(entry.entity)));
                    }
                    if (!relation.element && relation.target.isInstance(entry.entity)) {
                        write("DELETE FROM " + q(relation.joinTable) + " WHERE " +
                                        matches(joinColumns(relation.inverseJoinColumn, entry.model), null),
                                entry.model.keyValues(entry.model.identifier(entry.entity)));
                    }
                }
            }
        }
        List<Object> args = new ArrayList<Object>();
        String where = condition(entry, args);
        int changed = write("DELETE FROM " + q(entry.model.table()) + " WHERE " + where, args.toArray());
        if (changed != 1) {
            throw new OptimisticLockException("Stale or missing row: " + entry.model.table());
        }
        detachOne(entry.entity);
        entry.model.lifecycle(entry.entity, 5);
    }
    private void checkManagedIdentity(Entry entry) {
        if (entry.snapshot == null) {
            return;
        }
        if (!same(entry.model.identifierFromRow(entry.snapshot), entry.model.identifier(entry.entity))) {
            throw new PersistenceException("Managed primary key cannot change");
        }
        int version = entry.model.versionIndex();
        if (version >= 0 && !same(entry.snapshot[version], entry.model.get(entry.entity, version))) {
            throw new PersistenceException("Version is managed by the ORM");
        }
    }
    private String condition(Entry entry, List<Object> args) {
        EntityModel model = entry.model;
        int version = model.versionIndex();
        for (int index : model.idIndexes()) {
            args.add(entry.snapshot[index]);
        }
        String where = keyCondition(model, null);
        if (version >= 0) {
            where += " AND " + q(model.attributes()[version].column) + " = ?";
            args.add(entry.snapshot[version]);
        }
        return where;
    }
    static Object[] concat(Object[] left, Object[] right) {
        Object[] out = new Object[left.length + right.length];
        System.arraycopy(left, 0, out, 0, left.length);
        System.arraycopy(right, 0, out, left.length, right.length);
        return out;
    }
    static String placeholders(int count) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                result.append(", ");
            }
            result.append('?');
        }
        return result.toString();
    }
    String[] keyColumns(EntityModel model) {
        int[] keys = model.idIndexes();
        String[] columns = new String[keys.length];
        for (int i = 0; i < keys.length; i++) {
            columns[i] = model.attributes()[keys[i]].column;
        }
        return columns;
    }
    String[] joinColumns(String prefix, EntityModel model) {
        String[] keys = keyColumns(model);
        String[] columns = new String[keys.length];
        for (int i = 0; i < keys.length; i++) {
            columns[i] = prefix + (keys.length == 1 ? "" : "_" + keys[i]);
        }
        return columns;
    }
    String[] foreignColumns(EntityModel owner, Relationship relation) {
        String[] columns = new String[model(relation.target).idIndexes().length];
        for (int i = 0; i < columns.length; i++) {
            columns[i] = owner.attributes()[relation.column + i].column;
        }
        return columns;
    }
    String matches(String[] columns, String alias) {
        StringBuilder result = new StringBuilder();
        for (String column : columns) {
            if (result.length() > 0) {
                result.append(" AND ");
            }
            if (alias != null) {
                result.append(alias).append('.');
            }
            result.append(q(column)).append(" = ?");
        }
        return result.toString();
    }
    String quoted(String[] columns) {
        StringBuilder result = new StringBuilder();
        for (String column : columns) {
            if (result.length() > 0) {
                result.append(", ");
            }
            result.append(q(column));
        }
        return result.toString();
    }
    String joinEquality(String[] left, String leftAlias, String[] right, String rightAlias) {
        if (left.length != right.length) {
            throw new PersistenceException("Relationship key widths differ");
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < left.length; i++) {
            if (i > 0) {
                result.append(" AND ");
            }
            result.append(leftAlias)
                    .append('.')
                    .append(q(left[i]))
                    .append(" = ")
                    .append(rightAlias)
                    .append('.')
                    .append(q(right[i]));
        }
        return result.toString();
    }
    private void appendJoinDefinition(StringBuilder statement, String[] names, EntityModel model) {
        int[] ids = model.idIndexes();
        for (int i = 0; i < ids.length; i++) {
            if (i > 0) {
                statement.append(", ");
            }
            Attribute attr = model.attributes()[ids[i]];
            String type = attr.declaredType;
            if (type == null) {
                String keyType = sql.assignedKeyColumn(attr.kind);
                int end = keyType.indexOf(" PRIMARY KEY");
                type = end < 0 ? sql.columnType(attr.kind) : keyType.substring(0, end).replace(" NOT NULL", "");
            }
            statement.append(q(names[i])).append(' ').append(type).append(" NOT NULL");
        }
    }
    String keyCondition(EntityModel model, String alias) {
        StringBuilder result = new StringBuilder();
        for (int id : model.idIndexes()) {
            if (result.length() > 0) {
                result.append(" AND ");
            }
            if (alias != null) {
                result.append(alias).append('.');
            }
            result.append(q(model.attributes()[id].column)).append(" = ?");
        }
        return result.toString();
    }
    private void claim(EntityModel model, Object entity) {
        Key key = key(model, entity);
        Object existing = identities.get(key);
        if (existing != null && !sameInstance(existing, entity)) {
            throw new PersistenceException("Two instances have the same identity: " + model.table());
        }
        identities.put(key, entity);
    }
    private static Key key(EntityModel model, Object entity) {
        return new Key(model.hierarchyRoot().getName(), model.identifier(entity));
    }
    private static void assign(EntityModel model, Object entity, Object[] values) {
        model.read(entity, values);
    }
    private static void copy(EntityModel model, Object from, Object to) {
        assign(model, to, snapshot(model, from));
    }
    private static Object[] snapshot(EntityModel model, Object entity) {
        Object[] result = new Object[model.attributes().length];
        for (int i = 0; i < result.length; i++) {
            Object value = model.get(entity, i);
            result[i] = value instanceof byte[] ? ((byte[]) value).clone() : value;
        }
        return result;
    }
    // Sessions, managed instances, and generated model descriptors have identity
    // semantics. Domain equals() must never determine their context ownership.
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    static boolean sameInstance(Object left, Object right) {
        return left == right;
    }
    private static boolean same(Object a, Object b) {
        if (a instanceof byte[] && b instanceof byte[]) {
            return Arrays.equals((byte[]) a, (byte[]) b);
        }
        return sameInstance(a, b) || (a != null && a.equals(b));
    }
    private static PersistenceException failure(IOException e) {
        return new PersistenceException(e.getMessage(), e);
    }
    private static final class Entry {
        final EntityModel model;
        final Object entity;
        Object[] snapshot;
        Object initialId;
        boolean fresh;
        boolean removed;
        boolean inserting;
        boolean[] deferredForeignKeys;
        boolean deleting;
        List[] collections;
        Entry(EntityModel model, Object entity, Object[] snapshot) {
            this.model = model;
            this.entity = entity;
            this.snapshot = snapshot;
        }
    }
    private static final class Key {
        final String type;
        final Object id;
        Key(String type, Object id) {
            this.type = type;
            this.id = id instanceof byte[] ? ((byte[]) id).clone()
                      : id instanceof Integer || id instanceof Short || id instanceof Byte
                              ? Long.valueOf(((Number) id).longValue())
                              : id;
        }
        @Override
        public int hashCode() {
            return 31 * type.hashCode() + (id == null                    ? 0
                                                  : id instanceof byte[] ? Arrays.hashCode((byte[]) id)
                                                                         : id.hashCode());
        }
        @Override
        public boolean equals(Object o) {
            return o instanceof Key && type.equals(((Key) o).type) && same(id, ((Key) o).id);
        }
    }
}
