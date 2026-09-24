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

/// A persistence context obtained from an entity manager's `openSession()` method.
/// Tracks changes to managed entities and maintains one instance per entity identity.
/// Sessions and their queries are not thread-safe; use and complete transactions on
/// one thread. Call `close()` when finished; it never commits pending work.
///
/// Writes require an explicit transaction. Queries flush pending changes when a
/// transaction is active. A failed flush marks that transaction rollback-only;
/// call `rollbackTransaction()` before continuing. Rollback detaches all entities
/// but does not restore the Java objects' previous field values.
///
/// Lazy relationships require an open session that still manages their owner.
/// Load relationships before detaching or serializing if they are needed later.
/// Implementations are supplied by the ORM; applications should not implement this
/// interface.
public interface Session {
    /// Creates a typed query using the supported JPQL subset.
    /// Parsing and mapping validation occur immediately, before SQL execution.
    /// @param <T> result type
    /// @param statement query using entity and Java attribute names
    /// @param resultType expected entity or scalar type; use `Object[].class` for tuples
    /// @return a query belonging to this session
    /// @throws IllegalArgumentException if syntax or a mapped name is unsupported
    <T> JpqlQuery<T> createQuery(String statement, Class<T> resultType);

    /// Creates an untyped JPQL query, including bulk update or delete statements.
    /// @param statement query using entity and Java attribute names
    /// @return a query yielding entities, scalars, or `Object[]` tuples
    /// @throws IllegalArgumentException if syntax or a mapped name is unsupported
    JpqlQuery<Object> createQuery(String statement);

    /// Starts a transaction for this session.
    /// @throws PersistenceException if the session is closed, a transaction is already
    /// active, or the database cannot begin the transaction
    void beginTransaction();

    /// Flushes pending changes and commits the active transaction.
    /// A failure leaves a still-active database transaction requiring rollback.
    /// If the database already ended the failed transaction, the session detaches
    /// its entities and becomes inactive; a new transaction can then be started.
    /// @throws PersistenceException if no usable transaction is active or commit fails
    void commitTransaction();

    /// Rolls back the active transaction and detaches all managed entities.
    /// Pending changes are discarded; Java field values are not reverted.
    /// @throws PersistenceException if no transaction is active or rollback fails
    void rollbackTransaction();

    /// Reports whether this session has an active transaction.
    /// @return true between a successful begin and commit or rollback
    boolean isTransactionActive();

    /// Reports whether a transaction failure prevents committing.
    /// @return true when the active transaction must be rolled back
    boolean isRollbackOnly();

    /// Tests whether this session manages an entity that is not scheduled for removal.
    /// @param entity instance to test; null is allowed
    /// @return true if the instance is currently managed and not removed
    boolean contains(Object entity);

    /// Stops tracking an instance, discarding its unflushed changes.
    /// Cascades DETACH only through already loaded relationships. Does nothing for
    /// an instance that is not managed by this session.
    /// @param entity instance to detach
    void detach(Object entity);

    /// Detaches every entity and discards all unflushed changes.
    /// Does not end the transaction or undo SQL already executed in it.
    void clear();

    /// Rolls back any active transaction, detaches entities, and releases session resources.
    /// Repeated calls have no effect. The entity manager retains ownership of its database.
    /// @throws PersistenceException if rollback or resource release fails
    void close();

    /// Finds an entity, reusing its managed instance when present.
    /// An uncached lookup flushes pending changes in an active transaction.
    /// @param <T> entity type
    /// @param type mapped entity class
    /// @param id non-null scalar key, embedded key, or composite {@link Identifier}
    /// @return the managed entity, or null if no matching row exists
    /// @throws IllegalArgumentException if the identifier shape is invalid
    /// @throws PersistenceException if no generated mapping exists for the entity type
    <T> T find(Class<T> type, Object id);

    /// Finds an entity and optionally locks its database row.
    /// A pessimistic lock requires an active transaction and a supporting backend.
    /// @param <T> entity type
    /// @param type mapped entity class
    /// @param id entity identifier
    /// @param mode requested lock mode
    /// @return the managed entity, or null if no matching row exists
    /// @throws UnsupportedOperationException if the database does not support row locks
    /// @throws OptimisticLockException if a managed version is stale
    /// @throws PersistenceException if the required transaction is not active
    <T> T find(Class<T> type, Object id, LockMode mode);

    /// Applies a lock mode to a managed entity's row.
    /// @param entity managed instance
    /// @param mode requested lock mode; pessimistic modes require an active transaction
    /// @throws UnsupportedOperationException if the database does not support row locks
    /// @throws OptimisticLockException if the row is missing or its version is stale
    /// @throws PersistenceException if the entity is not managed or a transaction is required
    void lock(Object entity, LockMode mode);

    /// Makes a new entity managed and schedules its insertion at flush.
    /// Traverses associations with PERSIST cascade. A to-one reference to an unsaved
    /// entity without that cascade is rejected at flush.
    /// @param <T> entity type
    /// @param entity new mapped instance
    /// @throws PersistenceException if no transaction is active or the instance cannot be persisted
    <T> void persist(T entity);

    /// Copies state into a managed instance, cascading through MERGE associations.
    /// Continue working with the returned instance; the supplied detached instance
    /// does not become managed merely because it was passed to this method.
    /// @param <T> entity type
    /// @param entity new or detached mapped instance
    /// @return the managed instance containing the merged state
    /// @throws PersistenceException if no transaction is active or merging fails
    <T> T merge(T entity);

    /// Schedules a managed entity for deletion, cascading REMOVE associations.
    /// @param entity managed instance to remove
    /// @throws PersistenceException if no transaction is active or the instance is not managed
    void remove(Object entity);

    /// Reloads a persisted managed instance, discarding its local changes.
    /// Traverses REFRESH cascades. Rejects new, unflushed instances before changing
    /// session state. A failure after reload begins clears the context and marks an
    /// active transaction rollback-only.
    /// @param entity persisted instance managed by this session
    /// @throws PersistenceException if the instance cannot be refreshed
    void refresh(Object entity);

    /// Writes pending inserts, updates, relationship changes, and removals.
    /// Does not commit. Failure marks the active transaction rollback-only.
    /// @throws OptimisticLockException if a versioned row was changed or removed elsewhere
    /// @throws PersistenceException if no usable transaction is active or a write fails
    void flush();

    /// Atomically adds to an integral counter in SQL after flushing pending changes.
    /// Also increments an optimistic version when present and refreshes a managed
    /// instance of the affected row. Guards against counter and version overflow.
    /// @param <T> entity type
    /// @param type mapped entity class
    /// @param id entity identifier
    /// @param field Java name of an int or long field that is neither key nor version
    /// @param amount signed amount to add
    /// @return true if a row changed; false for a missing row, null counter, or overflow
    /// @throws IllegalArgumentException if the field is not a supported counter
    /// @throws PersistenceException if no usable transaction is active or the update fails
    <T> boolean increment(Class<T> type, Object id, String field, long amount);

    /// Creates a fluent entity query.
    /// @param <T> entity type
    /// @param type mapped entity class
    /// @return an initially unrestricted query
    /// @throws PersistenceException if no generated mapping exists for the entity type
    <T> Query<T> query(Class<T> type);

    /// Creates missing tables, indexes, and constraints for registered mappings.
    /// Run outside application transactions. Does not migrate existing tables.
    /// @throws PersistenceException if a transaction is active or schema creation fails
    void createTables();

    /// Checks mapped columns, type families, nullability, and primary keys.
    /// Does not migrate the schema or exhaustively validate indexes and foreign keys.
    /// @throws PersistenceException if a mapped table or column is incompatible or inaccessible
    void validateSchema();

    /// Counts related rows without loading the relationship's entities.
    /// Flushes pending changes when a transaction is active.
    /// @param entity owner with a persisted identifier
    /// @param field Java name of the relationship or element collection
    /// @return relationship size; zero or one for a to-one association
    long count(Object entity, String field);

    /// Checks relationship initialization without fetching its contents.
    /// @param entity mapped instance
    /// @param field Java relationship name
    /// @return true if loaded or if the instance has no managed lazy state
    boolean isLoaded(Object entity, String field);

    /// Loads a relationship if it is still uninitialized.
    /// @param entity relationship owner
    /// @param field Java relationship name
    /// @throws LazyInitializationException if unloaded state belongs to a detached entity
    /// @throws PersistenceException if the session is closed or fetching fails
    void initialize(Object entity, String field);

}
