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
package com.codename1.impl.android.cipher;

import com.codename1.db.Database;
import com.codename1.db.DatabaseEncryptionException;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import java.io.File;
import java.io.IOException;

/**
 * Entry point into the SQLCipher-backed database implementation.
 *
 * This whole package compiles against net.zetetic, which is only on the classpath of app builds
 * that actually use encrypted databases. AndroidGradleBuilder deletes the package for every other
 * app, so AndroidImplementation must reach this class reflectively and never by a direct
 * reference - the same arrangement the ARCore-backed AR implementation uses.
 */
public class AndroidCipherFactory {

    private static boolean librariesLoaded;

    private AndroidCipherFactory() {
    }

    private static synchronized void loadLibraries() {
        if (!librariesLoaded) {
            System.loadLibrary("sqlcipher");
            librariesLoaded = true;
        }
    }

    /**
     * Reports whether the SQLCipher native library is present and loadable.
     *
     * Called reflectively. A false return means the platform should say encrypted databases are
     * unsupported, rather than letting the failure surface later at open time.
     */
    public static boolean isAvailable() {
        try {
            loadLibraries();
            return true;
        } catch (Throwable notAvailable) {
            return false;
        }
    }

    /**
     * Opens an encrypted database.
     *
     * @param path absolute path to the database file
     * @param databaseName the name the database was opened under, needed to resolve a managed key on rekey
     * @param key  the key literal, either a passphrase or an x'...' raw key
     * @return the open database
     * @throws IOException if the database cannot be opened, or the key does not decrypt it
     */
    public static Database open(String path, String databaseName, String key) throws IOException {
        loadLibraries();
        File file = new File(path);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openOrCreateDatabase(file, key, null, null);
            // SQLCipher applies the key lazily, so without reading something now a wrong key
            // would not surface until some later and apparently unrelated query.
            db.rawQuery("SELECT count(*) FROM sqlite_master", null).close();
            return new AndroidCipherDB(db, databaseName);
        } catch (RuntimeException err) {
            if (db != null) {
                try {
                    db.close();
                } catch (RuntimeException ignored) {
                    // The original failure is the one worth reporting.
                }
            }
            throw new DatabaseEncryptionException(DatabaseEncryptionException.WRONG_KEY,
                    "The supplied key does not decrypt this database", err);
        }
    }
}
