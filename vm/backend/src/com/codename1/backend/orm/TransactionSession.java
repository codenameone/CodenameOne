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

import com.codename1.backend.Transactions;
import com.codename1.orm.session.JpqlQuery;
import com.codename1.orm.session.LockMode;
import com.codename1.orm.session.Query;
import com.codename1.orm.session.Session;

/**
 * The {@link Session} the build injects: the managed session of whatever
 * transaction the calling thread is in.
 *
 * <p>One object is injected into a singleton and used by every request, so it
 * cannot BE a session -- a session is a persistence context, one per unit of
 * work, and not thread-safe. Each call is forwarded to the session of the
 * calling thread's {@code @Transactional} method instead, which is opened on the
 * transaction's connection when first used, flushed before the transaction
 * commits and closed when it ends. That is the same contract as a Spring-managed
 * {@code EntityManager}.
 *
 * <p>Outside a transaction there is no unit of work to belong to, and every call
 * refuses with a message saying so. The transaction's boundaries belong to the
 * annotation, so beginning, committing, rolling back and closing through this
 * object are refused too.
 */
public final class TransactionSession implements Session {
    private final EntityManager entities;

    public TransactionSession(EntityManager entities) {
        this.entities = entities;
    }

    private Session current() {
        return Transactions.session(entities);
    }

    private static UnsupportedOperationException boundary(String what) {
        return new UnsupportedOperationException(what + " belongs to the @Transactional "
                + "method this session is part of. Throw to roll back, or call "
                + "Transactions.setRollbackOnly().");
    }

    public <T> JpqlQuery<T> createQuery(String statement, Class<T> resultType) {
        return current().createQuery(statement, resultType);
    }

    public JpqlQuery<Object> createQuery(String statement) {
        return current().createQuery(statement);
    }

    public void beginTransaction() {
        throw boundary("Beginning a transaction");
    }

    public void commitTransaction() {
        throw boundary("Committing");
    }

    public void rollbackTransaction() {
        throw boundary("Rolling back");
    }

    public boolean isTransactionActive() {
        return Transactions.isActive();
    }

    public boolean isRollbackOnly() {
        return Transactions.isRollbackOnly();
    }

    public boolean contains(Object entity) {
        return Transactions.isActive() && current().contains(entity);
    }

    public void detach(Object entity) {
        current().detach(entity);
    }

    public void clear() {
        current().clear();
    }

    public void close() {
        throw boundary("Closing the session");
    }

    public <T> T find(Class<T> type, Object id) {
        return current().find(type, id);
    }

    public <T> T find(Class<T> type, Object id, LockMode mode) {
        return current().find(type, id, mode);
    }

    public void lock(Object entity, LockMode mode) {
        current().lock(entity, mode);
    }

    public <T> void persist(T entity) {
        current().persist(entity);
    }

    public <T> T merge(T entity) {
        return current().merge(entity);
    }

    public void remove(Object entity) {
        current().remove(entity);
    }

    public void refresh(Object entity) {
        current().refresh(entity);
    }

    public void flush() {
        current().flush();
    }

    public <T> boolean increment(Class<T> type, Object id, String field, long amount) {
        return current().increment(type, id, field, amount);
    }

    public <T> Query<T> query(Class<T> type) {
        return current().query(type);
    }

    public void createTables() {
        current().createTables();
    }

    public void validateSchema() {
        current().validateSchema();
    }

    public long count(Object entity, String field) {
        return current().count(entity, field);
    }

    public boolean isLoaded(Object entity, String field) {
        return current().isLoaded(entity, field);
    }

    public void initialize(Object entity, String field) {
        current().initialize(entity, field);
    }
}
