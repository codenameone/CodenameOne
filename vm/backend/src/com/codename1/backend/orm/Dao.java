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

import com.codename1.impl.orm.Values;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.codename1.backend.Database;
import com.codename1.backend.DataSource;

/**
 * Typed access to one entity's table.
 *
 * <p>Reached through {@link EntityManager#dao}, never constructed: the statements
 * it issues were generated from the entity at build time, and the instance is
 * bound to the entity manager that supplies its connections.
 *
 * <pre>
 *   Dao&lt;Note&gt; notes = em.dao(Note.class);
 *   Note note = new Note();
 *   note.title = "first";
 *   notes.insert(note);              // note.id is now the generated key
 *   List&lt;Note&gt; recent = notes.query()
 *           .eq("author", author)
 *           .orderBy("created", false)
 *           .limit(20)
 *           .list();
 * </pre>
 *
 * <p>Each method takes a connection from the pool for exactly its own statement
 * and gives it straight back, so a dao is safe to share between handlers and
 * holds nothing between calls. Several statements that have to be one go through
 * {@link EntityManager#transaction}, which pins one connection for the body.
 */
public final class Dao<T> {
    private final EntityManager owner;
    private final Table table;

    Dao(EntityManager owner, Table table) {
        this.owner = owner;
        this.table = table;
    }

    /** The entity class. */
    public Class type() {
        return table.definition.type();
    }

    /** The table name, from @Entity(table) or the class's simple name. */
    public String tableName() {
        return table.definition.table();
    }

    /**
     * CREATE TABLE IF NOT EXISTS, in this engine's spelling.
     *
     * <p>This is a convenience for development and for tests, not a migration
     * tool: it creates a table that is not there and does nothing at all to one
     * that is, so a column added to the entity later does not appear. A schema
     * that changes over time wants whatever the team runs migrations with.
     */
    public void createTable() throws IOException {
        execute(table.getCreateTableSql(), null);
    }

    /** DROP TABLE IF EXISTS. */
    public void dropTable() throws IOException {
        execute(table.getDropTableSql(), null);
    }

    /**
     * Inserts {@code entity}.
     *
     * <p>When the key is the database's to assign -- @Id(autoIncrement = true),
     * which is the default -- the generated key is read back into the entity
     * before this returns, on every engine. That is the operation the three
     * disagree about most: PostgreSQL has no last-insert-id and answers through
     * INSERT ... RETURNING instead. See {@link Database#insert}.
     */
    public void insert(final T entity) throws IOException {
        require(entity);
        final Object[] params = table.insertParams(entity);
        if(!table.columns[table.idIndex].isGenerated()) {
            execute(table.getInsertSql(), params);
            return;
        }
        owner.run(new DataSource.Work() {
            public Object run(Database db) throws Exception {
                long key = db.insert(table.getInsertSql(), params, table.getIdColumn());
                table.setId(entity, key);
                return null;
            }
        });
    }

    /**
     * Puts the generated key back in step after rows were inserted with keys of
     * their own.
     *
     * <p>Supplying your own key is a supported thing -- a data import, a test
     * fixture, a restore -- and on SQLite and MySQL doing it also moves the
     * counter, so the next generated insert carries on above what is there. On
     * PostgreSQL it does not: an explicit value leaves the identity's sequence
     * where it was, and the next generated insert reuses a key that already
     * exists. MEASURED on a fresh table, explicit id 1 followed by a generated
     * insert: SQLite and MySQL answer 2, PostgreSQL fails with "duplicate key
     * value violates unique constraint".
     *
     * <p>Call this once after an import. It is a no-op on the engines that need
     * none, so the caller writes the same line whatever it is deployed against --
     * which is the whole reason it is here rather than in every importer.
     *
     * <p>Nothing calls it for you: the ORM's own insert never supplies a key for
     * a generated column, so it cannot tell that an import happened.
     */
    public void resyncGeneratedKey() throws IOException {
        if(!table.columns[table.idIndex].isGenerated()) {
            return;
        }
        // RAW NAMES: the dialect spells each one for the position it lands in --
        // an identifier stays quoted, a text argument becomes an escaped literal.
        final String sql = table.dialect.resyncGeneratedKey(
                table.definition.table(), table.columns[table.idIndex].getColumn());
        if(sql == null) {
            return;
        }
        // ONE TRANSACTION, so the lock is still held when the setval lands.
        // Taken outside it the lock would be released at the end of its own
        // statement and exclude nothing; the read of max() and the write of the
        // sequence have to be inside the same one, because a concurrent insert
        // between them can commit the key this is about to hand out next.
        final String lock = table.dialect.lockForResync(table.definition.table());
        owner.run(new DataSource.Work() {
            public Object run(final Database db) throws Exception {
                if(lock == null) {
                    db.query(sql, null);
                    return null;
                }
                // A nested BEGIN/COMMIT on PostgreSQL commits the caller's
                // transaction. Keep its lock and writes inside that boundary.
                if(owner.isTransactionScoped()) {
                    db.execute(lock, null);
                    db.query(sql, null);
                    return null;
                }
                return db.transaction(new Database.Work() {
                    public Object run(Database inTransaction) throws Exception {
                        inTransaction.execute(lock, null);
                        inTransaction.query(sql, null);
                        return null;
                    }
                });
            }
        });
    }

    /**
     * Updates every non-key column of the row whose key {@code entity} carries.
     *
     * @return whether a row matched. False is the answer a handler turns into a
     *         404, and it is why this returns something where the client's dao
     *         returns void: on a server the row that was not there is a case,
     *         not an impossibility.
     */
    public boolean update(T entity) throws IOException {
        require(entity);
        String sql = table.getUpdateSql();
        if(sql == null) {
            throw new IOException(tableName() + " has no columns but its key, so there is "
                    + "nothing for an update to set");
        }
        return execute(sql, table.updateParams(entity)) > 0;
    }

    /** Deletes the row whose key {@code entity} carries. */
    public boolean delete(T entity) throws IOException {
        require(entity);
        return execute(table.getDeleteSql(), new Object[] {table.idOf(entity)}) > 0;
    }

    /** Deletes the row with this key. */
    public boolean deleteById(Object id) throws IOException {
        return execute(table.getDeleteSql(), new Object[] {table.checkedKey(id)}) > 0;
    }

    /** The row with this key, or null. */
    public T findById(Object id) throws IOException {
        List found = list(table.select(" WHERE " + table.getIdCondition()),
                new Object[] {table.checkedKey(id)});
        return found.isEmpty() ? (T)null : (T)found.get(0);
    }

    /** Every row. */
    public List<T> findAll() throws IOException {
        return list(table.select(null), null);
    }

    /** How many rows the table holds. */
    public long count() throws IOException {
        return query().count();
    }

    /**
     * A query built from field names, which is the portable way to ask: the
     * names are the entity's Java fields and the builder quotes the columns they
     * map to, so the same query runs on all three engines.
     */
    public Query<T> query() {
        return new Query<T>(this, table);
    }

    /**
     * Rows matching a WHERE clause written by hand, for the query the builder
     * cannot express.
     *
     * <p>{@code where} is appended after WHERE and its ? are bound from
     * {@code params}. It is SQL, so what it names is COLUMNS rather than fields,
     * and an unquoted name in it folds to lower case on PostgreSQL the way any
     * unquoted name does -- which is the portability the builder keeps and this
     * gives up. Never build it by concatenating a value: that is the injection
     * this whole surface exists to make unnecessary.
     */
    public List<T> find(String where, Object[] params) throws IOException {
        String tail = where == null || where.length() == 0 ? null : " WHERE " + where;
        return list(table.select(tail), params);
    }

    /**
     * The first row matching a hand-written WHERE clause, or null.
     *
     * <p>Asks the database for ONE row. Reading them all and keeping the first
     * is the same answer and an unbounded amount of work to get it: a predicate
     * that matches a large table materialised the whole of it, mapped every row
     * into an entity, and then dropped all but one. {@link Query#first} has
     * always done this; this is the hand-written half catching up.
     */
    public T findOne(String where, Object[] params) throws IOException {
        String tail = where == null || where.length() == 0 ? "" : " WHERE " + where;
        List found = list(table.select(tail + table.dialect.limit(1, 0)), params);
        return found.isEmpty() ? (T)null : (T)found.get(0);
    }

    /** Runs a SELECT this dao's table maps and reads every row as an entity. */
    List<T> list(final String sql, final Object[] params) throws IOException {
        return (List<T>)owner.run(new DataSource.Work() {
            public Object run(Database db) throws Exception {
                List rows = db.query(sql, params);
                return table.readAll(rows, new ArrayList(rows.size()));
            }
        });
    }

    /** Runs a statement that returns no rows and answers how many it changed. */
    int execute(final String sql, final Object[] params) throws IOException {
        Object changed = owner.run(new DataSource.Work() {
            public Object run(Database db) throws Exception {
                return Integer.valueOf(db.execute(sql, params));
            }
        });
        return changed instanceof Number ? ((Number)changed).intValue() : 0;
    }

    /** The single number a COUNT(*) answers with. */
    long scalar(final String sql, final Object[] params) throws IOException {
        Object value = owner.run(new DataSource.Work() {
            public Object run(Database db) throws Exception {
                Map row = db.queryOne(sql, params);
                if(row == null || row.isEmpty()) {
                    return null;
                }
                return row.values().iterator().next();
            }
        });
        return Values.asLong(value, 0);
    }

    private void require(Object entity) throws IOException {
        if(entity == null) {
            throw new IOException("No " + type().getName() + " to store");
        }
    }
}
