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

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/// One database of a [MongoClient].
public final class MongoDatabase {
    private final MongoClient client;
    private final String name;

    MongoDatabase(MongoClient client, String name) {
        this.client = client;
        this.name = name;
    }

    /// The database's name.
    public String getName() {
        return name;
    }

    /// A collection by name; nothing is sent until it is used.
    public MongoCollection collection(String collection) {
        return new MongoCollection(client, name, collection);
    }

    /// Runs a command against this database and returns the reply.
    public Map runCommand(Map command) throws IOException {
        return client.runCommand(name, command);
    }

    /// Drops the whole database.
    public void drop() throws IOException {
        Map cmd = new LinkedHashMap();
        cmd.put("dropDatabase", Integer.valueOf(1));
        runCommand(cmd);
    }
}
