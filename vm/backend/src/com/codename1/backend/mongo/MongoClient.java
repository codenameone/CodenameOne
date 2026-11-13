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
package com.codename1.backend.mongo;

import com.codename1.backend.Config;
import com.codename1.impl.backend.BackendAccess;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// A pool of connections to one MongoDB deployment.
///
/// ```java
/// MongoClient mongo = MongoClient.current();
/// MongoCollection users = mongo.database().collection("users");
/// users.insertOne(doc);
/// List found = users.find(Filters.eq("email", email));
/// ```
///
/// | Setting | |
/// |---|---|
/// | `cn1.mongodb.uri` | the connection string; `MONGODB_URI` in the environment |
///
/// Without `cn1.mongodb.uri`, Spring Boot's `spring.data.mongodb.uri` is read, so
/// a Spring configuration carries over. The database is the one the URI names,
/// or `spring.data.mongodb.database`, or `test` as the official drivers default.
///
/// Calls borrow a connection, use it and give it back. With none idle another is
/// opened rather than making the caller wait, and at most `maxPoolSize` (16) are
/// kept idle. Hosts are tried in order and the first that answers is used; of
/// several, the first that is the replica set's primary.
public final class MongoClient implements java.io.Closeable {
    private static final String RESOURCE = "cn1.mongodb";

    private final MongoUri uri;
    private final String defaultDatabase;
    private final List idle = new ArrayList();
    private final java.util.concurrent.atomic.AtomicBoolean closed =
            new java.util.concurrent.atomic.AtomicBoolean();

    private MongoClient(MongoUri uri, String defaultDatabase) {
        this.uri = uri;
        this.defaultDatabase = defaultDatabase;
    }

    /// A client for this connection string; the caller closes it.
    public static MongoClient open(String connectionString) throws IOException {
        MongoUri u = MongoUri.parse(connectionString);
        return new MongoClient(u, u.database == null ? "test" : u.database);
    }

    /// A client for the deployment `config` names; the caller closes it.
    public static MongoClient open(Config config) throws IOException {
        String url = config.get("cn1.mongodb.uri");
        if (url == null || url.trim().length() == 0) {
            url = config.get("spring.data.mongodb.uri");
        }
        if (url == null || url.trim().length() == 0) {
            throw new IOException("No MongoDB configured: set cn1.mongodb.uri (or "
                    + "MONGODB_URI)");
        }
        MongoUri u = MongoUri.parse(url.trim());
        String db = u.database;
        if (db == null) {
            db = config.get("spring.data.mongodb.database");
        }
        return new MongoClient(u, db == null ? "test" : db);
    }

    /// The client of the calling thread's server: opened from its configuration
    /// the first time it is asked for, and closed when the server stops.
    public static MongoClient current() throws IOException {
        return (MongoClient) BackendAccess.get().serverResource(RESOURCE,
                new BackendAccess.ResourceFactory() {
                    @Override
                    public Object open(Config config) throws IOException {
                        return MongoClient.open(config);
                    }
                });
    }

    /// The database the connection string names.
    public MongoDatabase database() {
        return new MongoDatabase(this, defaultDatabase);
    }

    /// A database by name.
    public MongoDatabase database(String name) {
        return new MongoDatabase(this, name);
    }

    /// Runs one command against `database` on a pooled connection.
    public Map runCommand(String database, Map command) throws IOException {
        MongoConnection c = borrow();
        try {
            return c.command(database, command);
        } catch (IOException err) {
            if (!c.isBroken()) {
                // A command the server refused; the connection is fine.
                throw err;
            }
            // Whatever dropped this connection (a restart, a proxy's idle
            // timeout) has likely dropped the idle ones beside it too.
            closeIdle();
            throw err;
        } finally {
            release(c);
        }
    }

    private MongoConnection borrow() throws IOException {
        if (closed.get()) {
            throw new IOException("The MongoDB client is closed");
        }
        long now = System.currentTimeMillis();
        while (true) {
            MongoConnection pooled;
            synchronized (idle) {
                int n = idle.size();
                if (n == 0) {
                    break;
                }
                pooled = (MongoConnection) idle.remove(n - 1);
            }
            if (uri.maxIdleTimeMillis <= 0 || now - pooled.releasedAt() <= uri.maxIdleTimeMillis) {
                return pooled;
            }
            pooled.close();
        }
        IOException last = null;
        // With several hosts (a replica set) only the primary takes writes; a
        // secondary that answers first would fail every write. One host is a
        // direct connection and is used as it is.
        boolean needPrimary = uri.hosts.size() > 1 && !uri.directConnection;
        for (int i = 0; i < uri.hosts.size(); i++) {
            String h = (String) uri.hosts.get(i);
            try {
                MongoConnection c = MongoConnection.open(uri, MongoUri.hostName(h), MongoUri.port(h));
                if (!needPrimary || c.isWritablePrimary()) {
                    return c;
                }
                c.close();
                last = new IOException("MongoDB host " + h + " is not the primary");
            } catch (MongoException err) {
                // Authentication refused: another host of the same deployment
                // would refuse the same credentials.
                throw err;
            } catch (IOException err) {
                last = err;
            }
        }
        throw last == null ? new IOException("No MongoDB host answered") : last;
    }

    private void release(MongoConnection c) {
        if (c.isBroken()) {
            c.close();
            return;
        }
        synchronized (idle) {
            if (!closed.get() && idle.size() < uri.maxPoolSize) {
                c.markReleased(System.currentTimeMillis());
                idle.add(c);
                return;
            }
        }
        c.close();
    }

    /// Closes every idle connection; ones in use close when they are returned.
    @Override
    public void close() {
        closed.set(true);
        closeIdle();
    }

    private void closeIdle() {
        List drained;
        synchronized (idle) {
            drained = new ArrayList(idle);
            idle.clear();
        }
        for (Object eachDrainedItem : drained) {
            ((MongoConnection) eachDrainedItem).close();
        }
    }
}
