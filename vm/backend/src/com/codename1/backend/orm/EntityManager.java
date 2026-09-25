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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.codename1.backend.Database;
import com.codename1.backend.DataSource;
import com.codename1.backend.sql.Dialect;

/**
 * The entry point to the build-time ORM: entities in, daos out.
 *
 * <pre>
 *   EntityManager em = EntityManager.open(dataSource);
 *   Dao&lt;Note&gt; notes = em.dao(Note.class);
 * </pre>
 *
 * <p>The conventions are the client's, deliberately. An entity is a class with
 * {@code @Entity}, {@code @Id}, {@code @Column} and {@code @DbTransient} on it;
 * the build reads those out of the compiled class and writes the dao; the dao
 * issues prepared statements and never reflects. What differs on this side is
 * what it issues them THROUGH: {@link com.codename1.backend.Database}, so the
 * same entity class is stored in the app's SQLite file and in the server's
 * PostgreSQL, and only the connection knows which.
 *
 * <h2>How a dao gets here</h2>
 *
 * <p>Nothing looks a class up by name. The build writes one
 * {@code <Entity>Cn1BackendDao} per entity and a
 * {@code cn1app.BackendDaoBootstrap} whose constructor registers every one of
 * them, and the generated server entry point runs that before it starts
 * listening.
 *
 * <p>That entry point is the ONLY thing that can run it. The bootstrap is
 * generated during process-classes, after javac has compiled the module's own
 * sources, so a {@code main} written by hand cannot name it -- a module that
 * sets its own mainClass reaches the database through {@link DataSource} and not
 * through this.
 *
 * <p>The names are not the client's -- there the dao is {@code <Entity>Cn1Dao}
 * and the bootstrap is {@code cn1app.DaoBootstrap} -- because an entity shared
 * between an application and its server has BOTH generated for it, and the two
 * have to be able to sit on one classpath.
 *
 * <p>That indirection is not ceremony. The translator drops a class nothing
 * references, and obfuscation renames the ones that survive, so a registry
 * populated by scanning or by {@code Class.forName} would be empty in exactly the
 * builds that ship. A generated class holding a direct reference to each dao is
 * what keeps them alive, and the registry is keyed on {@code Class.getName()},
 * which registration and lookup agree about within one execution however the
 * names were rewritten.
 *
 * <p>An entity manager takes its snapshot of the registry when it is opened, so
 * everything is resolved once at start-up and a request never contends on a map.
 */
public final class EntityManager {
    /**
     * Every definition the build generated, by class name.
     *
     * <p>Written during start-up by the generated bootstrap, read when an entity
     * manager is opened. Synchronized rather than concurrent because both of
     * those happen once: this is not on the request path, and the cheapest
     * correct thing is a lock nobody is waiting for.
     */
    private static final Map REGISTRY = new LinkedHashMap();

    private final DataSource pool;
    private final Database pinned;
    /**
     * Whether this manager exists only for the duration of a transaction
     * somebody else opened.
     *
     * <p>Both kinds are pinned to one connection, and telling them apart is what
     * two operations depend on. {@link #transaction} JOINS an open transaction
     * rather than opening a nested one, which every engine refuses -- but a
     * manager the caller built over its own connection is not in a transaction
     * at all, and running its body without a BEGIN left earlier writes committed
     * when a later one threw. {@link #close} is the other: the caller's manager
     * owns its connection and this one owns nothing.
     */
    private final boolean transactionScoped;
    private final Dialect dialect;
    private final Map tables;
    private final Map daos;

    private EntityManager(DataSource pool, Database pinned, Dialect dialect, Map tables,
                          boolean transactionScoped) {
        this.pool = pool;
        this.pinned = pinned;
        this.transactionScoped = transactionScoped;
        this.dialect = dialect;
        this.tables = tables;
        this.daos = new HashMap();
        Iterator entries = tables.entrySet().iterator();
        while(entries.hasNext()) {
            Map.Entry entry = (Map.Entry)entries.next();
            daos.put(entry.getKey(), new Dao(this, (Table)entry.getValue()));
        }
    }

    /** Installs a generated definition. The generated bootstrap calls this. */
    public static void register(EntityDefinition definition) {
        if(definition == null) {
            throw new IllegalArgumentException("No entity definition");
        }
        synchronized(REGISTRY) {
            REGISTRY.put(definition.type().getName(), definition);
        }
    }

    /**
     * Removes a definition, for a test that registered a deliberately broken one.
     *
     * <p>Nothing in a server calls this: the registry is filled once at start-up
     * by generated code and never emptied, which is why there is no public way to
     * unregister. A test that installs a definition the runtime must REFUSE would
     * otherwise leave it there for every test class sharing the JVM.
     */
    static void forgetForTest(Class entity) {
        synchronized(REGISTRY) {
            REGISTRY.remove(entity.getName());
        }
    }

    /** Every registered definition, in registration order. */
    public static EntityDefinition[] registered() {
        synchronized(REGISTRY) {
            return (EntityDefinition[])REGISTRY.values()
                    .toArray(new EntityDefinition[REGISTRY.size()]);
        }
    }

    /**
     * An entity manager over a pool, which is the usual form: each operation
     * borrows a connection for its own statement and gives it straight back.
     */
    public static EntityManager open(DataSource pool) throws IOException {
        if(pool == null) {
            throw new IOException("No data source");
        }
        return new EntityManager(pool, null, pool.dialect(), tablesFor(pool.dialect()), false);
    }

    /**
     * An entity manager over one connection. Everything it does is serialized on
     * that connection, which is what a single-connection SQLite server wants and
     * what a server engine under load does not.
     */
    public static EntityManager open(Database db) throws IOException {
        if(db == null) {
            throw new IOException("No database");
        }
        return new EntityManager(null, db, db.dialect(), tablesFor(db.dialect()), false);
    }

    /**
     * The dao for an entity class.
     *
     * <p>Resolved from the snapshot taken when this manager was opened, so it
     * allocates nothing and cannot fail for a class that was registered before
     * then. A class that was not registered says so, because the cause is nearly
     * always one of two things: the annotation is missing, or the generated
     * bootstrap was never run.
     */
    public <T> Dao<T> dao(Class<T> entity) {
        if(entity == null) {
            throw new IllegalArgumentException("No entity class");
        }
        if (com.codename1.impl.orm.Models.requiresSession(entity))
            throw new IllegalStateException("This entity mapping requires openSession() managed persistence");
        Dao<T> dao = (Dao<T>)daos.get(entity.getName());
        if(dao == null) {
            throw new IllegalStateException("No dao was generated for " + entity.getName()
                    + ". Either the class has no @Entity on it, or cn1:process-annotations did "
                    + "not run over this module, or this server has a hand-written main: the "
                    + "generated cn1app.BackendDaoBootstrap is what registers the daos, only "
                    + "the generated entry point can construct it -- it is written after your "
                    + "sources are compiled, so nothing you write can name it -- and a module "
                    + "that sets its own mainClass therefore has no daos.");
        }
        return dao;
    }

    /**
     * Opens an independent managed persistence context. The caller must close it.
     * Begin and complete each transaction on the same thread; other users of its
     * connection wait until commit, rollback, or session close. Closing never commits.
     * @return a new persistence context with independent managed entity state
     * @throws IllegalStateException if called on a transaction-scoped manager
     */
    public com.codename1.orm.session.Session openSession() {
        if (transactionScoped) throw new IllegalStateException("Open a session on the outer manager; the session owns its transaction");
        return new com.codename1.impl.orm.SessionImpl(new com.codename1.impl.orm.BackendSqlAccess(pool, pinned, dialect));
    }

    /**
     * Creates the table of every registered entity that has none.
     *
     * <p>For development and for tests. Production schemas are migrations, and
     * this creates a table without ever altering one, so it cannot be that.
     */
    public void createTables() throws IOException {
        EntityDefinition[] all = registered();
        Map<String, com.codename1.impl.orm.EntityModel<?>> models = com.codename1.impl.orm.Models.snapshot();
        boolean managedSchema = false;
        for (com.codename1.impl.orm.EntityModel model : models.values()) {
            if (model.requiresSession()) { managedSchema = true; break; }
        }
        if (managedSchema) {
            com.codename1.orm.session.Session session = openSession();
            try { session.createTables(); } finally { session.close(); }
        }
        for (EntityDefinition definition : all) {
            // Hand-written legacy definitions have no generated model. They still
            // need their tables when an application also uses managed entities.
            if (!managedSchema || !models.containsKey(definition.type().getName())) {
                dao(definition.type()).createTable();
            }
        }
    }

    /**
     * Runs {@code body} inside one transaction on one connection.
     *
     * <p>The entity manager the body is handed is pinned to that connection, so
     * every dao reached through it is inside the transaction. A dao taken from
     * the OUTER manager is not -- that borrows a second connection, which the
     * database sees as another session.
     *
     * <p>An entity manager that is already pinned joins the transaction it is in
     * rather than opening another: all three engines refuse a nested BEGIN, and a
     * service method that works alone should not break when another one calls it.
     */
    public Object transaction(final Work body) throws Exception {
        if(body == null) {
            throw new IOException("No work to run");
        }
        final EntityManager self = this;
        if(transactionScoped) {
            // Already inside one. Every engine refuses a nested BEGIN, and a
            // service method that works alone should not break when another one
            // calls it.
            return body.run(this);
        }
        if(pinned != null) {
            // Pinned, but by the CALLER rather than by a transaction: this is
            // the manager EntityManager.open(Database) hands back, and it is not
            // in a transaction until this opens one. Running the body without a
            // BEGIN left the writes before a failure committed.
            return pinned.transaction(new Database.Work() {
                public Object run(Database inner) throws Exception {
                    return body.run(new EntityManager(null, inner, self.dialect,
                            self.tables, true));
                }
            });
        }
        return pool.withConnection(new DataSource.Work() {
            public Object run(final Database db) throws Exception {
                return db.transaction(new Database.Work() {
                    public Object run(Database inner) throws Exception {
                        return body.run(new EntityManager(null, inner, self.dialect,
                                self.tables, true));
                    }
                });
            }
        });
    }

    /**
     * Runs one unit of work against a connection: the pinned one, or one borrowed
     * from the pool and released however the work ends.
     *
     * <p>The daos call this for every statement they issue, which is what makes
     * the difference between a pooled manager and a pinned one invisible to them
     * -- and what keeps a transaction's statements on the connection that opened
     * it.
     */
    Object run(DataSource.Work work) throws IOException {
        if(pinned != null) {
            try {
                return work.run(pinned);
            } catch (IOException err) {
                throw err;
            } catch (Exception err) {
                throw failed(err);
            }
        }
        Database db = pool.borrow();
        try {
            return work.run(db);
        } catch (IOException err) {
            throw err;
        } catch (Exception err) {
            throw failed(err);
        } finally {
            pool.release(db);
        }
    }

    /**
     * A non-IOException from inside a dao, as one.
     *
     * <p>The work these run is this package's own and throws IOException, so this
     * is the unreachable arm of a checked-exception signature rather than a
     * conversion anything depends on. It keeps the message and the type name
     * because the one way to get here is a bug in generated field access, and
     * losing what it said would make that bug anonymous.
     */
    private static IOException failed(Exception err) {
        return new IOException(err.getClass().getName() + ": " + err.getMessage());
    }

    /** Whether DAO work must join a transaction already opened by this manager. */
    boolean isTransactionScoped() {
        return transactionScoped;
    }

    /** The pool behind this manager, or null when it is pinned to one connection. */
    public DataSource dataSource() {
        return pool;
    }

    /** The connection this manager is pinned to, or null when it holds a pool. */
    public Database database() {
        return pinned;
    }

    /** How this manager's engine spells things. See {@link Dialect}. */
    public Dialect dialect() {
        return dialect;
    }

    /**
     * Closes the pool, or the connection, this manager was opened over.
     *
     * <p>The manager handed to a transaction body owns nothing and closes
     * nothing: the connection under it belongs to the transaction, which is not
     * over. One opened over a caller's {@link Database} does close it, which is
     * what the sentence above promises and what the client-side entity manager
     * does -- leaving it open made repeated open/use/close cycles leak a SQLite
     * handle or a network session each time.
     */
    public void close() {
        if(transactionScoped) {
            return;
        }
        if(pool != null) {
            pool.close();
            return;
        }
        if(pinned != null) {
            pinned.close();
        }
    }

    /** A unit of work run inside {@link #transaction}. */
    public interface Work {
        Object run(EntityManager em) throws Exception;
    }

    /**
     * Every registered entity's statements, built for one engine.
     *
     * <p>Built once here rather than per dao, because the strings depend on
     * nothing else and a request should pay for its parameters and not for its
     * SQL.
     */
    private static Map tablesFor(Dialect dialect) throws IOException {
        Map out = new LinkedHashMap();
        EntityDefinition[] all = registered();
        List clashes = null;
        // TABLE NAME -> the entity that claimed it. The map above is keyed by
        // CLASS, so two entities on one table both register and neither is
        // reported: foo.User and bar.User default to the same table "User",
        // createTables runs CREATE TABLE IF NOT EXISTS for the first and skips
        // the second, and the second dao then selects columns that were never
        // created. The failure surfaces as a missing column on a query, far from
        // the two classes that explain it.
        // TWO PARALLEL LISTS AND A SCAN, not a map keyed by a folded name.
        //
        // asciiLower is what this used, and it only folds ASCII: the tables
        // "Aerenden" and "aerenden" collide under it, but spell the first letter
        // with an A-umlaut and they do not, while MySQL with
        // lower_case_table_names=1 -- its default on Windows and macOS, which is
        // the configuration this check exists for -- still resolves both to one
        // table. CREATE TABLE IF NOT EXISTS then silently reuses the first
        // entity's schema and the second dao queries columns that were never
        // created, which is exactly the failure the check was written to stop and
        // exactly the case it let through.
        //
        // equalsIgnoreCase, never toLowerCase: this runtime has no Locale to ask
        // for the root one, and on a Turkish device an I folds to a dotless i.
        // It compares character by character and is locale independent, and it
        // folds non-ASCII, so it is what the column-collision check in Table uses
        // for the same question. The cost is a scan over the ENTITIES, of which an
        // application has a handful, once when the table set is built.
        List claimedNames = new ArrayList();
        List claimedBy = new ArrayList();
        for(int iter = 0 ; iter < all.length ; iter++) {
            EntityDefinition definition = all[iter];
            String claimed = definition.table();
            Object owner = null;
            for(int earlier = 0 ; earlier < claimedNames.size() ; earlier++) {
                if(((String)claimedNames.get(earlier)).equalsIgnoreCase(claimed)) {
                    owner = claimedBy.get(earlier);
                    break;
                }
            }
            if(owner != null) {
                if(clashes == null) {
                    clashes = new ArrayList();
                }
                // Case insensitively, because the engines differ on whether an
                // unquoted name folds -- and the ORM quotes every identifier, so
                // on PostgreSQL "User" and "user" WOULD be two tables while on
                // MySQL's default macOS and Windows configurations they are one.
                // An entity pair that works on one engine and not another is the
                // thing this whole layer exists to prevent.
                clashes.add(definition.type().getName() + " and " + owner
                        + " are both stored in table '" + definition.table()
                        + "'; give one of them @Entity(table = \"...\")");
                continue;
            }
            claimedNames.add(claimed);
            claimedBy.add(definition.type().getName());
            try {
                if(!com.codename1.impl.orm.Models.requiresSession(definition.type()))
                    out.put(definition.type().getName(), new Table(definition, dialect));
            } catch (IllegalStateException err) {
                // An entity with no @Id, which the generator refuses at build
                // time -- so this is a hand-written definition. Collected rather
                // than thrown, so opening a server reports every broken one at
                // once instead of one per restart.
                if(clashes == null) {
                    clashes = new ArrayList();
                }
                clashes.add(definition.type().getName() + ": " + err.getMessage());
            }
        }
        if(clashes != null) {
            throw new IOException("These entities cannot be mapped: " + clashes);
        }
        return out;
    }

}
