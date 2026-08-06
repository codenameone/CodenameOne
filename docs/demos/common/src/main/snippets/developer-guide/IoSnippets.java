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
// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::io-java-001[]
if (Database.isEncryptionSupported()) {
    DatabaseConfig config = DatabaseConfig.managed();
    Database db = Database.openOrCreate("secure.db", config);
    config.wipe();
}
// end::io-java-001[]

// tag::io-java-002[]
if (!Database.isEncrypted("myapp.db")) {
    Database.encrypt("myapp.db", DatabaseConfig.managed());
}
// end::io-java-002[]

// tag::io-java-003[]
Database db = new ThreadSafeDatabase(Database.openOrCreate("shared.db"));
// end::io-java-003[]
