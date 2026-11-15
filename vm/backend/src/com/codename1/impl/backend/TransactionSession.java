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
package com.codename1.impl.backend;

import com.codename1.backend.orm.EntityManager;

import com.codename1.backend.Transactions;
import com.codename1.orm.session.JpqlQuery;
import com.codename1.orm.session.LockMode;
import com.codename1.orm.session.Query;
import com.codename1.orm.session.Session;

/// The [Session] the build injects: the managed session of whatever
/// transaction the calling thread is in.
///
/// One object is injected into a singleton and used by every request, so it
/// cannot BE a session -- a session is a persistence context, one per unit of
/// work, and not thread-safe. Each call is forwarded to the session of the
/// calling thread's `@Transactional` method instead, which is opened on the
/// transaction's connection when first used, flushed before the transaction
/// commits and closed when it ends. That is the same contract as a Spring-managed
/// `EntityManager`.
///
/// Outside a transaction a READ -- `find`, `query`, `createQuery`,
/// `createNativeQuery`, `count` -- runs in a session of its own with no
/// transaction, as Spring's shared `EntityManager` reads without one: what it
/// answers is not attached to any later unit of work, and a bulk `executeUpdate`
/// on such a query still requires a transaction. Every write refuses with a
/// message saying so. The transaction's boundaries belong to the annotation, so
/// beginning, committing, rolling back and closing through this object are
/// refused too.
public final class TransactionSession implements Session {
    private final EntityManager entities;

    public TransactionSession(EntityManager entities) {
        this.entities = entities;
    }

    private Session current() {
        return BackendAccess.get().session(entities);
    }

    /// The session a read goes to: the transaction's, or outside one a new
    /// session with no transaction. That session holds no connection between
    /// statements (each borrows from the pool), so it is left to be collected
    /// with what it loaded instead of being closed under a query that has not
    /// run yet (issue 6240).
    private Session reader() {
        if (Transactions.isActive()) {
            return current();
        }
        return entities.openSession();
    }

    private static UnsupportedOperationException boundary(String what) {
        return new UnsupportedOperationException(what + " belongs to the @Transactional "
                + "method this session is part of. Throw to roll back, or call "
                + "Transactions.setRollbackOnly().");
    }

    @Override
    public <T> JpqlQuery<T> createNativeQuery(String statement, Class<T> resultType) {
        return reader().createNativeQuery(statement, resultType);
    }

    @Override
    public <T> JpqlQuery<T> createQuery(String statement, Class<T> resultType) {
        return reader().createQuery(statement, resultType);
    }

    @Override
    public JpqlQuery<Object> createQuery(String statement) {
        return reader().createQuery(statement);
    }

    @Override
    public void beginTransaction() {
        throw boundary("Beginning a transaction");
    }

    @Override
    public void commitTransaction() {
        throw boundary("Committing");
    }

    @Override
    public void rollbackTransaction() {
        throw boundary("Rolling back");
    }

    @Override
    public boolean isTransactionActive() {
        return Transactions.isActive();
    }

    @Override
    public boolean isRollbackOnly() {
        return Transactions.isRollbackOnly();
    }

    @Override
    public boolean contains(Object entity) {
        return Transactions.isActive() && current().contains(entity);
    }

    @Override
    public void detach(Object entity) {
        current().detach(entity);
    }

    @Override
    public void clear() {
        current().clear();
    }

    @Override
    public void close() {
        throw boundary("Closing the session");
    }

    @Override
    public <T> T find(Class<T> type, Object id) {
        return reader().find(type, id);
    }

    @Override
    public <T> T find(Class<T> type, Object id, LockMode mode) {
        return current().find(type, id, mode);
    }

    @Override
    public void lock(Object entity, LockMode mode) {
        current().lock(entity, mode);
    }

    @Override
    public <T> void persist(T entity) {
        current().persist(entity);
    }

    @Override
    public <T> T merge(T entity) {
        return current().merge(entity);
    }

    @Override
    public <T> T save(T entity) {
        return current().save(entity);
    }

    @Override
    public void remove(Object entity) {
        current().remove(entity);
    }

    @Override
    public void refresh(Object entity) {
        current().refresh(entity);
    }

    @Override
    public void flush() {
        current().flush();
    }

    @Override
    public <T> boolean increment(Class<T> type, Object id, String field, long amount) {
        return current().increment(type, id, field, amount);
    }

    @Override
    public <T> Query<T> query(Class<T> type) {
        return reader().query(type);
    }

    @Override
    public void createTables() {
        current().createTables();
    }

    @Override
    public void validateSchema() {
        current().validateSchema();
    }

    @Override
    public com.codename1.orm.session.SchemaReport schemaReport() {
        return current().schemaReport();
    }

    @Override
    public long count(Object entity, String field) {
        return reader().count(entity, field);
    }

    @Override
    public boolean isLoaded(Object entity, String field) {
        return current().isLoaded(entity, field);
    }

    @Override
    public void initialize(Object entity, String field) {
        current().initialize(entity, field);
    }
}
