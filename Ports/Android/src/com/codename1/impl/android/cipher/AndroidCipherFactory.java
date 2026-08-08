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
     * Recognises the engine's "this is not a SQLite database" report.
     *
     * That is what a SQLCipher database decrypted with the wrong key looks like: the plaintext it
     * produces is not a valid header. Matched on the message because the engine raises a plain
     * SQLiteException here rather than a subclass that would distinguish it.
     */
    private static boolean isNotADatabase(Throwable err) {
        for (Throwable t = err; t != null; t = t.getCause()) {
            String message = t.getMessage();
            if (message != null) {
                String lower = message.toLowerCase();
                if (lower.indexOf("not a database") >= 0 || lower.indexOf("notadb") >= 0
                        || lower.indexOf("file is encrypted") >= 0) {
                    return true;
                }
            }
        }
        return false;
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
        // Shared with the plaintext open path, which needs it just as much: the migration
        // leaves the live name missing for an instant either way.
        com.codename1.impl.android.AndroidImplementation.recoverInterruptedDatabaseMigration(
                file.getPath());
        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openOrCreateDatabase(file, key, null, null);
        } catch (RuntimeException err) {
            // Two very different failures arrive here. Opening reads page one to settle the page
            // size, so a wrong key surfaces as "file is not a database" during the open itself
            // rather than waiting for the probe below - the same place the simulator's driver
            // reports it. Everything else is a filesystem failure that no key can solve, and
            // reporting that as WRONG_KEY would send an application following the error codes
            // into prompting for a passphrase forever.
            if (isNotADatabase(err)) {
                throw new DatabaseEncryptionException(DatabaseEncryptionException.WRONG_KEY,
                        "The supplied key does not decrypt this database", err);
            }
            throw new IOException("The database " + path + " could not be opened: "
                    + err.getMessage(), err);
        }
        try {
            // SQLCipher applies the key lazily, so without reading something now a wrong key
            // would not surface until some later and apparently unrelated query. This read is the
            // only failure here that actually means the key is wrong.
            db.rawQuery("SELECT count(*) FROM sqlite_master", null).close();
            return new AndroidCipherDB(db, databaseName, key);
        } catch (RuntimeException err) {
            try {
                db.close();
            } catch (RuntimeException ignored) {
                // The original failure is the one worth reporting.
            }
            if (isNotADatabase(err)) {
                throw new DatabaseEncryptionException(DatabaseEncryptionException.WRONG_KEY,
                        "The supplied key does not decrypt this database", err);
            }
            // A malformed image or a read error reaches here too, and no key repairs either.
            throw new IOException("The database " + path + " could not be read: "
                    + err.getMessage(), err);
        }
    }
}
