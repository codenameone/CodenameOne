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
package com.codename1.db;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.impl.SQLStatementSplitter;
import com.codename1.io.FileSystemStorage;
import com.codename1.ui.Display;

import java.io.IOException;
import java.io.InputStream;

/// Allows access to SQLite specifically connecting to a database and executing sql queries on the data.
/// There is more thorough coverage of the `Database API here`.
///
/// The Database class abstracts the underlying SQLite of the device if
/// available.
///
/// Notice that this might not be supported on all platforms in which case the `Database` will be null.
///
/// SQLite should be used for very large data handling, for small storage
/// refer to `com.codename1.io.Storage` which is more portable.
///
/// #### Example
///
/// ```java
/// Database db = null;
/// Cursor cur = null;
/// try {
///     db = Database.openOrCreate("MyDB.db");
///     db.execute("CREATE TABLE IF NOT EXISTS people (id INTEGER PRIMARY KEY, name TEXT)");
///     db.execute("INSERT INTO people (name) VALUES (?)", new Object[] {"Alice"});
///
///     cur = db.executeQuery("SELECT id, name FROM people ORDER BY id");
///     while (cur.next()) {
///         Row row = cur.getRow();
///         System.out.println(row.getInteger(0) + " " + row.getString(1));
///     }
/// } finally {
///     if (cur != null) {
///         cur.close();
///     }
///     if (db != null) {
///         db.close();
///     }
/// }
/// ```
///
/// #### Encryption
///
/// Pass a `DatabaseConfig` to `#openOrCreate(java.lang.String, com.codename1.db.DatabaseConfig)`
/// to encrypt the database at rest. Check `#isEncryptionSupported()` first, and read the security
/// notes on `DatabaseConfig` before choosing how to key it.
///
/// @author Chen
public abstract class Database {

    /// The first 16 bytes of every unencrypted SQLite database file.
    private static final byte[] PLAINTEXT_HEADER = {
        'S', 'Q', 'L', 'i', 't', 'e', ' ', 'f', 'o', 'r', 'm', 'a', 't', ' ', '3', 0
    };

    /// Backs `#isLegacyBehavior()`. Deliberately not volatile: it is read once, lazily, and
    /// treated as a startup-time constant thereafter. PMD forbids volatile in the core anyway.
    private static boolean legacyBehavior;

    private static boolean legacyBehaviorChecked;

    /// Tracks whether a transaction is open on this instance, so the flat-transaction rules in
    /// the package documentation are enforced identically on every port rather than being
    /// re-derived from each engine's very different native semantics.
    protected boolean inTransaction;

    /// The savepoints standing open, outermost first, when a savepoint opened this transaction.
    ///
    /// A stack rather than a name and a count, because SQLite releases every savepoint above the
    /// one named as well as that one. `SAVEPOINT s; SAVEPOINT t; SAVEPOINT s; RELEASE t` ends the
    /// inner `s` along with `t`, which a count of the outer name cannot express -- it would still
    /// read two and never reach zero, leaving a transaction reported open after the engine ended
    /// it, and every later begin and key change refused until the connection closed.
    private final java.util.Vector openSavepoints = new java.util.Vector();

    /// Whether a savepoint opened the current transaction, rather than a BEGIN.
    ///
    /// Only then does releasing the last savepoint end it. Under a BEGIN the savepoints are marks
    /// inside a transaction that BEGIN owns, and releasing all of them ends none of it.
    private boolean savepointOwnsTransaction;

    /// How many nested `beginTransaction()` calls are outstanding.
    ///
    /// One, except under the legacy hint on Android, where nesting is allowed because that is what
    /// the port used to do. The engine ref-counts there, so the first commit ends only the inner
    /// one: clearing the flag on it would report no transaction while the outer still holds
    /// uncommitted rows, and a key change would be allowed over them.
    private int transactionDepth;

    /// Checks if this platform supports custom database paths.  On platforms that
    /// support this, you can pass a file path to `#openOrCreate(java.lang.String)`, `#exists(java.lang.String)`,
    /// `#delete(java.lang.String)`, and `#getDatabasePath(java.lang.String)`.
    ///
    /// #### Returns
    ///
    /// True on platorms that support custom database paths.
    public static boolean isCustomPathSupported() {
        return Display.getInstance().isDatabaseCustomPathSupported();
    }

    /// Returns whether the database API is running in legacy compatibility mode.
    ///
    /// The behaviour of this API used to differ substantially between platforms. Those
    /// differences have been reconciled into the single contract documented in the
    /// `com.codename1.db` package, but applications written against the old, divergent
    /// behaviour may depend on it. Legacy mode restores each platform's previous behaviour
    /// exactly, and is intended as a transition aid rather than a permanent setting.
    ///
    /// Enable it with the `db.legacy` build hint, or from code before the first database call:
    ///
    /// ```java
    /// Database.setLegacyBehavior(true);
    /// ```
    ///
    /// The package documentation lists precisely which behaviours the flag covers. Fixes for
    /// outright defects, and capabilities that previously threw and now work, are **not**
    /// covered, because no application can depend on those.
    ///
    /// #### Returns
    ///
    /// true when the pre-normalization behaviour is in effect
    public static boolean isLegacyBehavior() {
        if (!legacyBehaviorChecked) {
            // Read lazily rather than in a static initializer. The generated application stubs
            // on iOS and desktop call setProperty AFTER Display.init, and Android's runs inside
            // an isInitialized guard, so an eager read would miss the build hint entirely.
            try {
                legacyBehavior = "true".equals(Display.getInstance().getProperty("db.legacy", "false"));
                // Latch only once a real answer came back, so that a caller reaching a database
                // before the display is up does not freeze the default in place.
                legacyBehaviorChecked = true;
            } catch (Throwable notReadyYet) {
                return false;
            }
        }
        return legacyBehavior;
    }

    /// Turns legacy compatibility mode on or off.
    ///
    /// Call this before opening any database; cursors and connections capture the mode as they
    /// are created, so flipping it mid-session gives inconsistent results.
    ///
    /// #### Parameters
    ///
    /// - `legacy`: true to restore the pre-normalization behaviour
    ///
    /// #### See also
    ///
    /// - #isLegacyBehavior()
    public static void setLegacyBehavior(boolean legacy) {
        legacyBehavior = legacy;
        legacyBehaviorChecked = true;
    }

    private static void validateDatabaseNameArgument(String databaseName) {
        // PMD Fix (CollapsibleIfStatements): Merge the custom path support and separator checks into one condition.
        if (!isCustomPathSupported() && (databaseName.indexOf("/") != -1 || databaseName.indexOf("\\") != -1)) {
            throw new IllegalArgumentException("This platform does not support custom database paths.  The database name cannot contain file separators.");
        }
    }

    /// Opens a database or create one if not exists.
    ///
    /// #### Parameters
    ///
    /// - `databaseName`: @param databaseName the name of the database.  Platforms that support custom database
    ///                     paths (i.e. `#isCustomPathSupported()` return true), will also accept a file path here.
    ///
    /// #### Returns
    ///
    /// Database Object or null if not supported on the platform
    ///
    /// #### Throws
    ///
    /// - `IOException`: if database cannot be created
    public static Database openOrCreate(String databaseName) throws IOException {
        validateDatabaseNameArgument(databaseName);
        return Display.getInstance().openOrCreate(databaseName);
    }

    /// Indicates weather a database exists
    ///
    /// **NOTE:** Not supported in the  Javascript port.  Will always return false.
    ///
    /// #### Parameters
    ///
    /// - `databaseName`: @param databaseName the name of the database.  Platforms that support custom database
    ///                     paths (i.e. `#isCustomPathSupported()` return true), will also accept a file path here.
    ///
    /// #### Returns
    ///
    /// true if database exists
    public static boolean exists(String databaseName) {
        validateDatabaseNameArgument(databaseName);
        return Display.getInstance().exists(databaseName);
    }

    /// Deletes database
    ///
    /// **NOTE:** This method is not supported in the  Javascript port.  Will silently fail.
    ///
    /// #### Parameters
    ///
    /// - `databaseName`: @param databaseName the name of the database. Platforms that support custom database
    ///                     paths (i.e. `#isCustomPathSupported()` return true), will also accept a file path here.
    ///
    /// #### Throws
    ///
    /// - `IOException`: if database cannot be deleted
    public static void delete(String databaseName) throws IOException {
        validateDatabaseNameArgument(databaseName);
        String claimed = claimForDelete(databaseName);
        try {
            Display.getInstance().delete(databaseName);
        } finally {
            releaseDeleteClaim(claimed);
        }
    }

    /// Files being deleted right now.
    ///
    /// Checking that nothing is open and then unlinking are two steps, and an open that lands
    /// between them gets a connection to a file that is about to lose its name -- an outcome
    /// neither order of the two operations produces. The claim closes that window the way
    /// `REKEYING_DATABASES` closes it for a rewrite.
    private static final java.util.Hashtable DELETING_DATABASES = new java.util.Hashtable();

    /// Refuses the delete if anything holds the database, and claims it if nothing does.
    ///
    /// #### Parameters
    ///
    /// - `databaseName`: the name or path being deleted
    ///
    /// #### Returns
    ///
    /// the claimed key, or null when this port could not identify the file
    ///
    /// #### Throws
    ///
    /// - `IOException`: if a connection is open, or another delete is already running
    private static String claimForDelete(String databaseName) throws IOException {
        String key;
        try {
            // The identity the ports register under, which is not the same string as the path
            // reported to an application: a custom "file://" name is handed back unchanged by
            // getDatabasePath, while the connection was registered under the native path it
            // resolves to. Checking the URL would find nothing and unlink the file anyway.
            key = Display.getInstance().databaseRegistryIdentity(databaseName);
        } catch (RuntimeException cannotResolve) {
            // A port that will not resolve a name cannot be checked this way; its own delete
            // answers for it. The JavaScript port is the one that does this, and it counts its
            // open databases itself.
            return null;
        }
        if (key != null && !takeDeleteClaim(key)) {
            // One call, not a look followed by a claim: two deletes of the same database would
            // both pass the look before either claimed it, and the first to finish would release
            // the entry while the second was still unlinking -- reopening the door this exists
            // to hold shut.
            throw new IOException("The database " + databaseName + " is already being deleted.");
        }
        // Counted after the claim is taken, and deliberately not while holding this class's lock.
        //
        // A port with its own registry -- Android, whose implementation tracks connections for
        // the conversion it runs outside this class -- answers from its own monitor, and an open
        // running there checks this claim from ours. Asking it while holding ours would have the
        // two threads take the same pair of locks in opposite orders, which deadlocks. Claiming
        // first and counting afterwards needs neither thread to hold both: an open that got in
        // before the claim has already incremented the count this reads, and one that arrives
        // after it is refused by the claim.
        if (unidentifiedConnectionsOpen()) {
            // Something is open that could not say which file it holds, so it could be this one.
            // A connection taken by SEDatabase(Connection) whose URL names no file counts here
            // and nowhere else, and unlinking underneath it loses everything it writes from then
            // on. Refusing is the only safe reading of "I do not know", which is what a key
            // change does with the same counter.
            releaseDeleteClaim(key);
            throw new IOException("A database opened from a connection is still open, and there "
                    + "is no way to tell whether it holds " + databaseName + ". Close it before "
                    + "deleting this database.");
        }
        boolean open;
        try {
            open = openDatabaseCount(key) > 0
                    || Display.getInstance().openDatabaseConnections(databaseName) > 0;
        } catch (RuntimeException cannotCount) {
            releaseDeleteClaim(key);
            throw cannotCount;
        }
        if (open) {
            releaseDeleteClaim(key);
            throw new IOException("The database " + databaseName + " is still open. Close it "
                    + "before deleting it: deleting an open database leaves the connection "
                    + "attached to a file that no longer has a name, and everything written "
                    + "through it afterwards is lost when it closes.");
        }
        return key;
    }

    /// Claims a file for deletion, unless a delete already holds it.
    ///
    /// #### Parameters
    ///
    /// - `key`: the identity the ports register connections under
    ///
    /// #### Returns
    ///
    /// true if this call took the claim and must give it back
    private static synchronized boolean takeDeleteClaim(String key) {
        if (DELETING_DATABASES.containsKey(key) || CONVERTING_DATABASES.containsKey(key)
                || REKEYING_DATABASES.containsKey(key)) {
            // A conversion holds the file from before it checks the database exists until the
            // rewrite is over. Deleting inside that window is what lets encrypt() report success
            // over a database that is no longer there.
            return false;
        }
        DELETING_DATABASES.put(key, Boolean.TRUE);
        return true;
    }

    /// Whether any connection is open that could not say which file it holds.
    private static synchronized boolean unidentifiedConnectionsOpen() {
        return unidentifiedOpenDatabases > 0;
    }

    /// Files a conversion is working on, from the existence check to the end of the rewrite.
    ///
    /// `REKEYING_DATABASES` covers the rewrite itself, which is claimed once the database is
    /// open. This one is taken before that, because `#encrypt(String, DatabaseConfig)` checks
    /// that the database exists and then opens it, and the open creates one where a delete
    /// landing in between removed it -- so the conversion would re-key an empty database and
    /// report success.
    private static final java.util.Hashtable CONVERTING_DATABASES = new java.util.Hashtable();

    /// Claims a database for a conversion, so a delete cannot run underneath it.
    ///
    /// #### Parameters
    ///
    /// - `databaseName`: the name being converted
    ///
    /// #### Returns
    ///
    /// the claimed key, or null when this port could not identify the file
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the database is being deleted, or already being converted
    private static synchronized String claimForConversion(String databaseName) throws IOException {
        String key;
        try {
            key = Display.getInstance().databaseRegistryIdentity(databaseName);
        } catch (RuntimeException cannotResolve) {
            return null;
        }
        if (key == null) {
            return null;
        }
        if (DELETING_DATABASES.containsKey(key)) {
            throw new DatabaseEncryptionException(DatabaseEncryptionException.MIGRATION_FAILED,
                    "The database " + databaseName + " is being deleted.");
        }
        if (CONVERTING_DATABASES.put(key, Boolean.TRUE) != null) {
            throw new DatabaseEncryptionException(DatabaseEncryptionException.MIGRATION_FAILED,
                    "The database " + databaseName + " is already being converted.");
        }
        return key;
    }

    private static synchronized void releaseConversionClaim(String key) {
        if (key != null) {
            CONVERTING_DATABASES.remove(key);
        }
    }

    private static synchronized void releaseDeleteClaim(String key) {
        if (key != null) {
            DELETING_DATABASES.remove(key);
        }
    }

    /// Whether a delete is running on this file, for a port that opens outside this class.
    ///
    /// #### Parameters
    ///
    /// - `key`: the identity the port registers connections under
    ///
    /// #### Returns
    ///
    /// true while a delete holds the file
    public static synchronized boolean isDatabaseBeingDeleted(String key) {
        return key != null && DELETING_DATABASES.containsKey(key);
    }

    /// How many connections are open on a registry key, for callers that only want to look.
    ///
    /// #### Parameters
    ///
    /// - `key`: a normalized path, or null
    ///
    /// #### Returns
    ///
    /// the number of open connections, or 0 when the key is unknown
    protected static synchronized int openDatabaseCount(String key) {
        if (key == null) {
            return 0;
        }
        Integer count = (Integer) OPEN_DATABASES.get(key);
        return count == null ? 0 : count.intValue();
    }

    /// Returns the file path of the Database if exists and if supported on
    /// the platform.
    ///
    /// #### Parameters
    ///
    /// - `databaseName`: @param databaseName The name of the database. Platforms that support custom database
    ///                     paths (i.e. `#isCustomPathSupported()` return true), will also accept a file path here.
    ///
    ///
    /// **NOTE:** Where `#isCustomPathSupported()` is false the databases are not filesystem
    /// backed, so what comes back identifies the database inside the platform's storage but is
    /// not a path `com.codename1.io.FileSystemStorage` can open.
    ///
    /// #### Returns
    ///
    /// the file path of the database
    public static String getDatabasePath(String databaseName) {
        validateDatabaseNameArgument(databaseName);
        return Display.getInstance().getDatabasePath(databaseName);
    }

    /// Opens an encrypted database, creating it if it does not exist.
    ///
    /// The database is encrypted at rest using the key described by `config`. Every platform
    /// that supports encryption writes the same on-disk format, so a database created on one
    /// device can be opened on another and in the simulator.
    ///
    /// If `config` is null or describes a plaintext database this behaves exactly like
    /// `#openOrCreate(java.lang.String)`.
    ///
    /// #### Example
    ///
    /// ```java
    /// if (!Database.isEncryptionSupported()) {
    ///     throw new IOException("This build cannot store data securely");
    /// }
    /// DatabaseConfig config = DatabaseConfig.managed();
    /// Database db = Database.openOrCreate("secure.db", config);
    /// config.wipe();
    /// ```
    ///
    /// #### Parameters
    ///
    /// - `databaseName`: @param databaseName the name of the database. Platforms that support custom
    ///                     database paths (see `#isCustomPathSupported()`) also accept a file path.
    ///
    /// - `config`: how to key the database, or null for plaintext
    ///
    /// #### Returns
    ///
    /// the open database
    ///
    /// #### Throws
    ///
    /// - `DatabaseEncryptionException`: @throws DatabaseEncryptionException with `DatabaseEncryptionException#NOT_SUPPORTED`
    ///                     if encryption was requested on a platform that cannot provide it, or with
    ///                     `DatabaseEncryptionException#WRONG_KEY` if the key does not decrypt an
    ///                     existing database
    ///
    /// - `IOException`: if the database cannot be opened or created
    public static Database openOrCreate(String databaseName, DatabaseConfig config) throws IOException {
        validateDatabaseNameArgument(databaseName);
        if (config == null || !config.isEncrypted()) {
            return Display.getInstance().openOrCreate(databaseName);
        }
        // Refuse rather than silently handing back a plaintext database. An application that
        // asked for encryption and got none is the worst possible outcome here.
        if (!isEncryptionSupported()) {
            throw new DatabaseEncryptionException(DatabaseEncryptionException.NOT_SUPPORTED,
                    "Encrypted databases are not supported on this platform");
        }
        return Display.getInstance().openOrCreate(databaseName, config);
    }

    /// Indicates whether this platform can open encrypted databases.
    ///
    /// #### Returns
    ///
    /// true if `#openOrCreate(java.lang.String, com.codename1.db.DatabaseConfig)` accepts an
    /// encrypting config
    public static boolean isEncryptionSupported() {
        return Display.getInstance().isDatabaseEncryptionSupported();
    }

    /// Indicates whether managed keys on this platform are held in hardware backed storage.
    static boolean isManagedKeyHardwareBacked() {
        return Display.getInstance().isDatabaseManagedKeyHardwareBacked();
    }

    /// Indicates whether a database file appears to be encrypted.
    ///
    /// This inspects the file header: an unencrypted SQLite database begins with the ASCII bytes
    /// `SQLite format 3` followed by a zero byte, and an encrypted one does not. It is therefore a
    /// **header sniff, not a cryptographic assertion** -- a truncated or corrupt file also reports
    /// true, and a false result only means the file is a readable plaintext SQLite database.
    ///
    /// #### Parameters
    ///
    /// - `databaseName`: the name of the database
    ///
    /// #### Returns
    ///
    /// false if the file exists and starts with a plaintext SQLite header, true otherwise
    public static boolean isEncrypted(String databaseName) {
        validateDatabaseNameArgument(databaseName);
        if (!exists(databaseName)) {
            return false;
        }
        // Ports whose databases are not files answer directly; reading a header that is not there
        // would fail, and a failed read is indistinguishable from ciphertext.
        int platformAnswer = Display.getInstance().isDatabaseFileEncrypted(databaseName);
        if (platformAnswer != CodenameOneImplementation.DATABASE_ENCRYPTION_UNKNOWN) {
            return platformAnswer == CodenameOneImplementation.DATABASE_ENCRYPTED;
        }
        String path = getDatabasePath(databaseName);
        if (path == null) {
            return false;
        }
        try {
            InputStream in = FileSystemStorage.getInstance().openInputStream(path);
            try {
                byte[] header = new byte[PLAINTEXT_HEADER.length];
                int offset = 0;
                while (offset < header.length) {
                    int read = in.read(header, offset, header.length - offset);
                    if (read < 0) {
                        return true;
                    }
                    offset += read;
                }
                for (int iter = 0; iter < PLAINTEXT_HEADER.length; iter++) {
                    if (header[iter] != PLAINTEXT_HEADER[iter]) {
                        return true;
                    }
                }
                return false;
            } finally {
                in.close();
            }
        } catch (IOException err) {
            // Unreadable or truncated. Reporting "encrypted" is the conservative answer, since the
            // one thing we can say is that it is not a readable plaintext SQLite file.
            return true;
        }
    }

    /// Encrypts an existing plaintext database in place.
    ///
    /// The conversion is performed by the database engine as a single transaction, so an
    /// interruption leaves the original file intact rather than half-converted. Schema metadata
    /// such as `PRAGMA user_version` is preserved.
    ///
    /// #### Parameters
    ///
    /// - `databaseName`: the name of an existing plaintext database
    ///
    /// - `config`: how the encrypted database should be keyed
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the database cannot be converted
    public static void encrypt(String databaseName, DatabaseConfig config) throws IOException {
        if (config == null || !config.isEncrypted()) {
            throw new IllegalArgumentException("encrypt() requires a config that describes an encrypted database");
        }
        if (!isEncryptionSupported()) {
            throw new DatabaseEncryptionException(DatabaseEncryptionException.NOT_SUPPORTED,
                    "Encrypted databases are not supported on this platform");
        }
        // Not openOrCreate: on Android the system SQLite has no cipher, so a database opened
        // through it could never be re-keyed. The platform decides which engine can do this.
        validateDatabaseNameArgument(databaseName);
        // Claimed before the database is even looked for, and held until the rewrite is done:
        // the open below creates what it does not find, so a delete landing between the check
        // and the open would leave this re-keying an empty database and reporting success.
        String conversion = claimForConversion(databaseName);
        try {
            requireExistingDatabase(databaseName, "encrypt");
            Database db = Display.getInstance().openOrCreateForRekey(databaseName);
            try {
                db.changeKey(config);
            } finally {
                db.close();
            }
        } finally {
            releaseConversionClaim(conversion);
        }
    }

    /// Decrypts an existing encrypted database in place, leaving a plain SQLite file.
    ///
    /// #### Parameters
    ///
    /// - `databaseName`: the name of an existing encrypted database
    ///
    /// - `config`: the config that currently opens the database
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the database cannot be converted
    public static void decrypt(String databaseName, DatabaseConfig config) throws IOException {
        if (config == null || !config.isEncrypted()) {
            throw new IllegalArgumentException("decrypt() requires the config that currently opens the database");
        }
        validateDatabaseNameArgument(databaseName);
        // Claimed before the database is even looked for, and held until the rewrite is done:
        // the open below creates what it does not find, so a delete landing between the check
        // and the open would leave this re-keying an empty database and reporting success.
        String conversion = claimForConversion(databaseName);
        try {
            requireExistingDatabase(databaseName, "decrypt");
            Database db = openOrCreate(databaseName, config);
            try {
                db.changeKey(DatabaseConfig.plain());
            } finally {
                db.close();
            }
        } finally {
            releaseConversionClaim(conversion);
        }
    }

    /// Refuses to migrate a database that is not there.
    ///
    /// Both migrations open their source through an open-or-create hook, so a missing or mistyped
    /// name used to create an empty database, convert that, and report success -- leaving the
    /// database the caller meant untouched while telling them it had been converted. Both
    /// document an existing database as their input, so the absence is an error.
    ///
    /// #### Parameters
    ///
    /// - `databaseName`: the name to check
    /// - `operation`: the calling method, for the message
    ///
    /// #### Throws
    ///
    /// - `IOException`: if no such database exists
    private static void requireExistingDatabase(String databaseName, String operation)
            throws IOException {
        if (!exists(databaseName)) {
            throw new IOException(operation + "() works on an existing database, and there is no "
                    + "database named " + databaseName);
        }
    }

    /// Removes the stored managed key for an alias.
    ///
    /// `#delete(java.lang.String)` deliberately leaves the managed key in place, because deleting
    /// and recreating a database is a normal thing to do and should not discard the identity that
    /// protects it. Call this explicitly when the key really should be forgotten -- after which any
    /// remaining database encrypted with it is permanently unreadable.
    ///
    /// #### Parameters
    ///
    /// - `keyAlias`: @param keyAlias the alias passed to `DatabaseConfig#managed(java.lang.String)`, or
    ///                 the database name when `DatabaseConfig#managed()` was used
    ///
    /// #### Returns
    ///
    /// true if a key was removed
    public static boolean forgetManagedKey(String keyAlias) {
        // Two interpretations, and only one of them can be meant. An explicit alias is stored
        // exactly as it was given, so that is tried first; the resolved identity is the fallback
        // for a key stored by managed() with no alias, which is filed under the file the name
        // resolves to and would otherwise be unreachable through this method.
        //
        // The fallback runs only when nothing is stored under the alias. That is asked with a
        // lookup rather than inferred from the delete, which reports success on every platform for
        // an entry that was not there -- so the fallback was unreachable and the implicit key
        // survived. One application's explicit alias can also be another database's name --
        // managed("shared") over here, a database called "shared" over there -- and deleting both
        // would take out a key its owner never named.
        int stored = ManagedKeys.state(keyAlias);
        if (stored == com.codename1.security.SecureStorage.ENTRY_PRESENT) {
            return ManagedKeys.forget(keyAlias);
        }
        if (stored != com.codename1.security.SecureStorage.ENTRY_ABSENT) {
            // The store could not be asked, so nothing here is known -- and the fallback below
            // deletes a key filed under a different name. Where an explicit alias happens to be
            // another database's name, which is the collision this method is careful about, a
            // guess taken here removes that database's key and its data can never be read again.
            // Reporting failure costs a retry; guessing costs somebody else's database.
            return false;
        }
        String identity = Display.getInstance().databaseManagedKeyIdentity(keyAlias);
        if (identity != null && !identity.equals(keyAlias)) {
            // Asked before removing, because every platform's remove reports success for an entry
            // that was not there -- which is documented on has() a few lines up and is exactly why
            // this method looks before it deletes. Removing blind here made forgetManagedKey
            // answer true for a database that never had a key, and this method's true is supposed
            // to mean a key was removed.
            if (ManagedKeys.state(identity) != com.codename1.security.SecureStorage.ENTRY_PRESENT) {
                return false;
            }
            return ManagedKeys.forget(identity);
        }
        return false;
    }

    /// Rewinds a cursor to before its first row.
    ///
    /// Uses `CursorExt#beforeFirst()` when the cursor provides it, and falls back to
    /// `Cursor#position(int)` with -1 otherwise.
    ///
    /// #### Parameters
    ///
    /// - `cursor`: the cursor to rewind
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the cursor is closed or the rewind fails
    public static void beforeFirst(Cursor cursor) throws IOException {
        if (cursor instanceof CursorExt) {
            ((CursorExt) cursor).beforeFirst();
        } else {
            cursor.position(-1);
        }
    }

    /// Returns the number of rows a cursor holds, or -1 where the port cannot determine it.
    ///
    /// **This can be expensive.** Only Android's engine knows the count without looking; every
    /// other port walks the result set to the end and rewinds, so this costs what the query costs
    /// and should not be called on the EDT for a large one. See `CursorExt#getCount()`.
    ///
    /// #### Parameters
    ///
    /// - `cursor`: the cursor to measure
    ///
    /// #### Returns
    ///
    /// the row count, or -1 where the port cannot determine it
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the cursor is closed
    public static int count(Cursor cursor) throws IOException {
        if (cursor instanceof CursorExt) {
            return ((CursorExt) cursor).getCount();
        }
        return -1;
    }

    /// Indicates whether `#executeQuery(java.lang.String, java.lang.Object[])` accepts `byte[]`
    /// parameters on this platform.
    ///
    /// Blob values can always be written with `#execute(java.lang.String, java.lang.Object[])`.
    /// Using one as a query parameter, for example in `WHERE digest = ?`, needs engine support
    /// that not every port can provide.
    ///
    /// #### Returns
    ///
    /// true if blobs may be used as query parameters
    public static boolean isBlobQueryParameterSupported() {
        return Display.getInstance().isBlobQueryParameterSupported();
    }

    /// Changes the key of this open database, or removes it entirely.
    ///
    /// Passing a plaintext config decrypts the database. The engine performs the conversion as a
    /// single transaction and preserves schema metadata such as `PRAGMA user_version`.
    ///
    /// Ports that support encryption override this. The default implementation reports that the
    /// platform cannot do it; it is deliberately concrete rather than abstract, because `Database`
    /// is public and is subclassed outside this repository.
    ///
    /// #### Parameters
    ///
    /// - `config`: the new key, or `DatabaseConfig#plain()` to decrypt
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the key cannot be changed
    public void changeKey(DatabaseConfig config) throws IOException {
        throw new DatabaseEncryptionException(DatabaseEncryptionException.NOT_SUPPORTED,
                "Changing the database key is not supported on this platform");
    }

    /// Checks if the last value accessed from a given row was null.  Not all platforms
    /// support wasNull().  If the platform does not support it, this will just return false.
    ///
    /// Check `#supportsWasNull(com.codename1.db.Row)` to see if the platform supports
    /// wasNull().
    ///
    /// Currently wasNull() is supported on UWP, iOS, Android, and JavaSE (Simulator).
    ///
    /// #### Parameters
    ///
    /// - `row`: The row to check.
    ///
    /// #### Returns
    ///
    /// True if the last value accessed was null.
    ///
    /// #### Throws
    ///
    /// - `IOException`
    ///
    /// #### See also
    ///
    /// - RowExt#wasNull()
    ///
    /// - #supportsWasNull(com.codename1.db.Row)
    public static boolean wasNull(Row row) throws IOException {
        if (row instanceof RowExt) {
            return ((RowExt) row).wasNull();
        }
        return false;
    }

    /// Checks to see if the given row supports `#wasNull(com.codename1.db.Row)`.
    ///
    /// #### Parameters
    ///
    /// - `row`: The row to check.
    ///
    /// #### Returns
    ///
    /// True if the row supports wasNull().
    ///
    /// #### Throws
    ///
    /// - `IOException`
    ///
    /// #### See also
    ///
    /// - #wasNull(com.codename1.db.Row)
    ///
    /// - RowExt#wasNull()
    public static boolean supportsWasNull(Row row) throws IOException {
        return row instanceof RowExt;
    }

    /// Reports whether a transaction is currently open on this database.
    ///
    /// #### Returns
    ///
    /// true between a successful `#beginTransaction()` and its commit or rollback
    public boolean isInTransaction() {
        return inTransaction;
    }

    /// Renders a key literal for use as a `PRAGMA` argument.
    ///
    /// Raw keys are already the blob literal `x'...'`, which has to reach the engine unquoted as a
    /// literal rather than as a string. Passphrases are arbitrary text, so they are single quoted
    /// with any embedded single quote doubled. Interpolating a passphrase directly would let one
    /// containing a quote change the statement.
    ///
    /// #### Parameters
    ///
    /// - `keyMaterial`: the value from `DatabaseConfig#resolveKeyMaterial(java.lang.String)`
    ///
    /// #### Returns
    ///
    /// the text to place after `PRAGMA key =` or `PRAGMA rekey =`
    protected static String toPragmaLiteral(String keyMaterial) {
        if (keyMaterial == null) {
            return "''";
        }
        // The exact literal, not merely the framing: "x'not-hex'" is a perfectly good passphrase,
        // and passphrase() only refuses the exact form, so anything looser than that predicate
        // sends a passphrase to the engine as an unquoted token and the re-key fails on syntax.
        if (DatabaseConfig.looksLikeRawKeyLiteral(keyMaterial)) {
            return keyMaterial;
        }
        StringBuilder b = new StringBuilder(keyMaterial.length() + 8);
        b.append('\'');
        for (int iter = 0; iter < keyMaterial.length(); iter++) {
            char c = keyMaterial.charAt(iter);
            if (c == '\'') {
                b.append('\'');
            }
            b.append(c);
        }
        b.append('\'');
        return b.toString();
    }

    /// Records what a script did to the transaction state.
    ///
    /// `#execute(java.lang.String)` hands SQL straight to the engine, so `execute("BEGIN")` opens
    /// a real transaction that `#beginTransaction()` never saw. Left untracked, the two ways of
    /// saying the same thing disagree: a key change would be allowed inside a transaction opened
    /// this way, and `beginTransaction(); execute("COMMIT")` would leave the flag set over a
    /// transaction that has already ended, so the next `commitTransaction()` addresses one that is
    /// not there.
    ///
    /// Ports call this after `#execute(java.lang.String)`, and after a parameterized call, with
    /// the SQL they ran. A `BEGIN` opening a trigger body is not transaction control -- the
    /// splitter keeps a trigger together, so its body is never a statement here -- and SAVEPOINT
    /// is not tracked at all, because it nests and this API's transactions do not.
    ///
    /// A script that failed partway had already run everything before the statement that failed,
    /// and the engine does not undo it. The control statements are read in order either way: they
    /// are the least likely statement to be the one that failed, since `BEGIN` and `COMMIT`
    /// reference nothing that can be missing. So `BEGIN; INSERT INTO missing_table VALUES(1)` is
    /// left open, which it is, and `BEGIN; COMMIT; INSERT INTO missing_table VALUES(1)` is left
    /// closed, which it also is -- where treating any `BEGIN` as still open would hold the flag
    /// over a committed transaction and block every key change until the connection was closed.
    ///
    /// The one case left wrong is a script whose own `BEGIN` failed, reported as open when nothing
    /// is. That is the recoverable direction -- `#rollbackTransaction()` clears it -- and the one
    /// that refuses a key change rather than allowing one underneath a live transaction.
    ///
    /// #### Parameters
    ///
    /// - `sql`: the SQL that was run, whether or not it finished
    protected void noteScriptTransactionControl(String sql) {
        if (sql == null) {
            return;
        }
        String[] statements = SQLStatementSplitter.split(sql);
        for (String statement : statements) {
            String keyword = transactionControlKeyword(statement);
            if ("BEGIN".equals(keyword)) {
                if (inTransaction && isLegacyBehavior() && supportsNestedTransactions()) {
                    // Android's wrapper intercepts transaction control in SQL and ref-counts it
                    // exactly as it counts beginTransaction(), so a BEGIN inside a transaction
                    // nests rather than replacing it. Counting it as a fresh one would lose the
                    // outer transaction's depth and end the tracking a level early.
                    transactionDepth++;
                    continue;
                }
                inTransaction = true;
                transactionDepth = 1;
                forgetSavepoints();
                continue;
            }
            if (keyword != null) {
                // Ends one level, not all of them. Where the engine ref-counts -- Android under
                // the legacy hint, which is the only place nesting is allowed -- a COMMIT in SQL
                // is intercepted by the same wrapper and ends only the innermost transaction.
                // Clearing here would report no transaction while the outer one still held
                // uncommitted rows, and a key change is allowed on that answer: it would copy
                // them into the replacement database. Everywhere else the depth is never above
                // one, so this ends the transaction as it always did.
                markTransactionEnded();
                continue;
            }
            noteSavepointControl(statement);
        }
        if (mentionsAttachment(sql)) {
            // Only when the script could have changed what is attached: this asks the engine, and
            // asking it after every statement would cost a query per execute.
            reconcileAttachments();
        }
    }

    /// Whether a script could have changed what is attached, cheaply enough to ask every time.
    private static boolean mentionsAttachment(String sql) {
        String upper = sql.toUpperCase();
        // Both words, because one is not a substring of the other: a standalone DETACH left the
        // reservation in place until the connection closed, and every delete, conversion and key
        // change on that database was refused in the meantime.
        return upper.indexOf("ATTACH") >= 0 || upper.indexOf("DETACH") >= 0;
    }

    /// The databases this connection has attached, by the schema name they were attached as.
    ///
    /// Held per connection because that is what owns them: SQLite drops every attachment when the
    /// connection closes, so this has to as well.
    private java.util.Hashtable attachments;

    /// Whether a reconciliation is already running on this connection, so the DETACH it may
    /// issue does not start another one.
    private boolean reconciling;

    /// A refusal a reconciliation could not report from where it happened, waiting for the first
    /// place that can throw it.
    private IOException attachmentRefusal;

    /// Reserves the databases a script is about to attach, before the engine attaches them.
    ///
    /// Called by every port at the top of `#execute(String)`. Reserving first is what makes this
    /// safe rather than merely watchful: if the file is being deleted the reservation is refused
    /// and this throws, so the ATTACH never runs and there is nothing to undo. Compensating
    /// afterwards -- attaching, then detaching again when the reservation lost -- could itself
    /// fail, on a locked database or inside a transaction, and left the attachment live with the
    /// delete already under way.
    ///
    /// Over-reserving is the deliberate direction. A statement that is reserved and then fails to
    /// execute leaves a reservation that is given back when the connection closes; the cost is a
    /// delete refused until then. The other direction loses data.
    ///
    /// A **relative** name is reserved under this port's database directory, which is not always
    /// the file the engine opens: SQLite resolves a relative name against the process working
    /// directory, and only the ports whose engine has no filesystem -- the browser, where a name
    /// is a pool entry -- resolve it the same way this does. Predicting the other answer is not
    /// possible from here; the working directory belongs to the process, differs per platform,
    /// and is not something this API exposes or controls. So the reservation covers the file a
    /// Codename One name means, which is what an application attaching `'data.db'` almost
    /// certainly intends, and the reconciliation afterwards is what covers the file the engine
    /// really opened -- including undoing an attachment that turns out to be unholdable. Attach
    /// by an absolute path from `#getDatabasePath(String)` to be reserved exactly.
    ///
    /// #### Parameters
    ///
    /// - `sql`: the script about to run
    ///
    /// #### Throws
    ///
    /// - `IOException`: if a database it attaches is being deleted or converted, or if an earlier
    ///   attachment had to be undone and nothing has reported that yet
    protected void reserveAttachments(String sql) throws IOException {
        // Every execute path on every port passes through here, which makes it the first place a
        // refusal from the previous statement's reconciliation can be thrown from.
        requireAttachmentsHeld();
        if (sql == null) {
            return;
        }
        for (String statement : SQLStatementSplitter.split(sql)) {
            // The leading keyword, read past comments and whitespace, rather than what the text
            // happens to start with. "/* migration */ ATTACH ..." is an ordinary statement in a
            // migration script and SQLite runs it, but it does not start with ATTACH, so its
            // target went unreserved and the delete guard never saw it.
            if (!"ATTACH".equals(leadingKeyword(statement))) {
                continue;
            }
            if (!isLegacyBehavior() && !isResolvableAttachment(statement)) {
                // SQLite evaluates an expression here and this cannot. Reserving the first token
                // of "'/tmp/' || 'b.db'" reserved "/tmp/" -- a claim on the wrong file, which
                // reads as protection and is not -- and a function call reserved nothing at all.
                // Refused before it runs, because that is the last moment anything can refuse:
                // afterwards the engine holds the file, and the detach that would undo it is
                // itself rejected inside a transaction that has written through the attachment.
                //
                // Not gated on the file being claimed, because that cannot be known either. The
                // statement is one this API cannot account for, whoever else is holding the file.
                throw new IOException("The file an ATTACH names has to be a single quoted "
                        + "literal ('name.db') or a bound parameter, and a name this resolves "
                        + "to the same file SQLite does, so that a database being deleted or "
                        + "re-keyed can be recognized before the attach happens: "
                        + statement.trim() + ". An expression is evaluated by the engine, and a "
                        + "double quoted name is an identifier that SQLite reads as a filename "
                        + "only when no column answers to it -- neither can be resolved here. "
                        + "Build the name in the application and bind it as a parameter.");
            }
            String file = attachmentFileOf(statement);
            if (file == null || file.length() == 0 || ":memory:".equals(file)) {
                continue;
            }
            // A relative name is a literal this can read and still cannot resolve: SQLite opens it
            // against the process working directory and this resolves it in the port's database
            // directory, so the reservation would be taken on one file while the engine attached
            // another. That is the same position an expression leaves this in, and it fails the
            // same way -- the file the engine really opened goes unregistered, and the detach that
            // would undo it is refused inside a transaction that has written through the
            // attachment. Refused here, where a refusal still stops it.
            //
            // Ports whose engine has no working directory to resolve against say so, and there a
            // bare name means exactly the database this would have reserved.
            if (!isLegacyBehavior() && !file.startsWith("file://")
                    && !Display.getInstance().isRelativeAttachmentNameResolvable()) {
                throw new IOException("The file an ATTACH names has to be an absolute path on "
                        + "this platform, because SQLite resolves a relative name against the "
                        + "process directory and Codename One resolves it in the database "
                        + "directory -- so the database protected would not be the database "
                        + "attached: " + statement.trim() + ". Pass Database.getDatabasePath(name)"
                        + " as the filename.");
            }
            String key;
            try {
                key = Display.getInstance().databaseRegistryIdentity(file);
            } catch (RuntimeException cannotResolve) {
                continue;
            }
            if (key == null) {
                continue;
            }
            if (attachments != null && attachments.containsKey(key)) {
                continue;
            }
            registerOpenDatabase(key);
            if (attachments == null) {
                attachments = new java.util.Hashtable();
            }
            // Held per file, and nothing is released here. A script can name one schema twice --
            // "ATTACH a AS aux; DETACH aux; ATTACH b AS aux" -- and letting b replace a would
            // unprotect a before the script has run, while the engine still has it attached and
            // may never reach that detach. Both are held, and the reconciliation afterwards
            // decides which the engine really ended up with.
            attachments.put(key, key);
        }
    }

    /// Reserves what a parameterized script is about to attach, including the bound values.
    ///
    /// `ATTACH DATABASE ? AS aux` names its file in the parameters, so the statement text alone
    /// cannot say what is about to be attached -- and the reservation has to exist before the
    /// engine attaches it, because a reservation refused afterwards cannot undo an attach.
    ///
    /// Every parameter that resolves to a database identity is reserved, not just the one the
    /// placeholder stands for. Working out which parameter belongs to the ATTACH would mean
    /// counting placeholders through quoting and comments for no gain: an over-reservation costs
    /// a delete refused until the reconciliation gives it back, moments later.
    ///
    /// #### Parameters
    ///
    /// - `sql`: the script about to run
    /// - `params`: the values bound to it, any of which may be the file
    ///
    /// #### Throws
    ///
    /// - `IOException`: if a database it may attach is being deleted or converted
    protected void reserveAttachments(String sql, Object[] params) throws IOException {
        reserveAttachments(sql);
        if (params == null || params.length == 0 || sql == null || !mentionsAttachment(sql)) {
            return;
        }
        if ("ATTACH".equals(leadingKeyword(sql))) {
            // This statement really is an ATTACH, so the placeholder that stands where the
            // filename goes names the file. Which parameter that is comes from the placeholder
            // itself: "?" and the named forms take the first, since a parameterized call is a
            // single statement and nothing binds before the filename, but "?2" takes the second
            // whatever precedes it. Reading the first parameter for every form reserved the
            // wrong value for "ATTACH ?2 AS aux" and then returned, so the file the engine
            // opened was reserved by nobody.
            //
            // Named exactly rather than over-reserved, which also keeps a second parameter --
            // the key, in "ATTACH ? AS aux KEY ?" -- from being read as a path.
            String[] target = attachmentTargetTokens(sql);
            if (target.length == 1 && isParameterPlaceholder(target[0])) {
                int slot = placeholderSlot(target[0]);
                if (slot >= 1 && slot <= params.length && params[slot - 1] instanceof String) {
                    reserveAttachmentParameter((String) params[slot - 1]);
                    return;
                }
                // The slot cannot be worked out -- an index past the parameters given, or a
                // value that is not a name at all. The loop below then over-reserves every
                // string, which costs a delete refused until the reconciliation gives it back
                // and is the only direction that does not leave the real file unheld.
            }
        }
        for (Object param : params) {
            if (!(param instanceof String)) {
                continue;
            }
            String value = (String) param;
            if (value.length() == 0 || ":memory:".equals(value)) {
                continue;
            }
            String key;
            try {
                key = Display.getInstance().databaseRegistryIdentity(asDatabaseArgument(value));
            } catch (RuntimeException cannotResolve) {
                continue;
            }
            if (key == null || (attachments != null && attachments.containsKey(key))) {
                continue;
            }
            registerOpenDatabase(key);
            if (attachments == null) {
                attachments = new java.util.Hashtable();
            }
            attachments.put(key, key);
        }
    }

    /// Reserves the file an `ATTACH ?` is about to open, bound as a parameter.
    ///
    /// Held to the same rule as a literal: a relative name is resolved against the process
    /// directory by SQLite and in this port's database directory here, so what would be reserved
    /// is not what would be attached. The refusal has to happen now, before the engine opens it.
    ///
    /// #### Parameters
    ///
    /// - `value`: the bound filename
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the file is claimed, or if it is a name this cannot resolve
    private void reserveAttachmentParameter(String value) throws IOException {
        if (value.length() == 0 || ":memory:".equals(value)) {
            return;
        }
        if (!isLegacyBehavior() && isAmbiguousUriFilename(value)) {
            // Same refusal as a literal URI, and for the same reason: the engine resolves it and
            // this cannot, while the string looks exactly like a path this had already resolved.
            throw new IOException("This file: URI does not name the same file to SQLite as it "
                    + "does here -- the engine strips a query and percent-decodes the rest, so "
                    + "the database protected would not be the database attached: " + value
                    + ". Pass the path, or the plain file:// form of it.");
        }
        String file = asDatabaseArgument(value);
        if (!isLegacyBehavior() && !file.startsWith("file://")
                && !Display.getInstance().isRelativeAttachmentNameResolvable()) {
            throw new IOException("The file an ATTACH names has to be an absolute path on this "
                    + "platform, because SQLite resolves a relative name against the process "
                    + "directory and Codename One resolves it in the database directory -- so "
                    + "the database protected would not be the database attached: " + value
                    + ". Pass Database.getDatabasePath(name) as the parameter.");
        }
        String key;
        try {
            key = Display.getInstance().databaseRegistryIdentity(file);
        } catch (RuntimeException cannotResolve) {
            return;
        }
        if (key == null || (attachments != null && attachments.containsKey(key))) {
            return;
        }
        registerOpenDatabase(key);
        if (attachments == null) {
            attachments = new java.util.Hashtable();
        }
        attachments.put(key, key);
    }

    /// Brings the reservations into line with what the engine actually has attached.
    ///
    /// Asked of the engine rather than worked out from the SQL, because the SQL cannot answer it.
    /// Three separate ways of being wrong came from trying: a DETACH the engine rejected because
    /// the database was locked still read as detached; a script that reused a schema name --
    /// attach a, detach, attach b -- left b unprotected and a released; and a relative filename
    /// resolves against the process directory for SQLite and against the Codename One database
    /// directory for a port, so the name reserved was not the file opened. `PRAGMA database_list`
    /// has none of those problems: it reports the schemas that are attached right now and the
    /// absolute file behind each one.
    ///
    /// Reservations are added for anything newly attached and given back for anything no longer
    /// attached, so a failed DETACH keeps its reservation and a reused schema follows the file it
    /// now names.
    ///
    /// Failure to *ask* leaves every reservation as it is. That holds a delete off a file that may
    /// no longer be attached, which is the recoverable direction; the other one unlinks a database
    /// somebody is writing through. An answer that cannot be held is a different matter: the
    /// attachment is undone here and the refusal left for `#requireAttachmentsHeld()` to report,
    /// because ports run this from a `finally` and a throw from there would replace the failure
    /// the statement itself is reporting.
    private void reconcileAttachments() {
        if (reconciling) {
            // The detach below runs through the port's own execute, which lands back here. One
            // reconciliation is enough: the statement it runs is a DETACH, so it cannot attach
            // anything this pass would have to account for.
            return;
        }
        reconciling = true;
        try {
            reconcileAttachmentsOnce();
        } finally {
            reconciling = false;
        }
    }

    private void reconcileAttachmentsOnce() {
        java.util.Hashtable live = new java.util.Hashtable();
        Cursor cursor = null;
        try {
            cursor = executeQuery("PRAGMA database_list");
            while (cursor.next()) {
                Row row = cursor.getRow();
                String schema = row.getString(1);
                String file = row.getString(2);
                if (schema == null || "main".equalsIgnoreCase(schema)
                        || "temp".equalsIgnoreCase(schema)) {
                    continue;
                }
                if (file == null || file.length() == 0) {
                    // Attached in memory, so there is no file for anything to delete.
                    continue;
                }
                String key = Display.getInstance().databaseIdentityForEngineFile(file);
                if (key != null) {
                    // Every schema on the file, not just the last one seen. The schemas are the
                    // only handles a DETACH can be written against, and SQLite lets one file be
                    // attached under several names at once -- "ATTACH 'x.db' AS a; ATTACH 'x.db'
                    // AS b" gives two rows here with one path between them. Keeping one name
                    // detached one of them and left the other live and unregistered, which is
                    // the whole failure this exists to prevent.
                    //
                    // Read back through instanceof rather than a cast. This runs inside a try
                    // that catches RuntimeException, and a failed cast does not raise one on
                    // ParparVM -- it hands the wrong object to the next instruction, which is a
                    // native crash no catch here would ever see.
                    Object held = live.get(key);
                    java.util.Vector schemas;
                    if (held instanceof java.util.Vector) {
                        schemas = (java.util.Vector) held;
                    } else {
                        schemas = new java.util.Vector();
                        live.put(key, schemas);
                    }
                    schemas.addElement(schema);
                }
            }
        } catch (IOException cannotAsk) {
            return;
        } catch (RuntimeException cannotAsk) {
            return;
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (IOException ignored) {
                    // The reconciliation is what matters.
                }
            }
        }
        if (attachments != null) {
            java.util.Vector gone = new java.util.Vector();
            java.util.Enumeration held = attachments.keys();
            while (held.hasMoreElements()) {
                String key = (String) held.nextElement();
                if (!live.containsKey(key)) {
                    gone.addElement(key);
                }
            }
            java.util.Enumeration removing = gone.elements();
            while (removing.hasMoreElements()) {
                String key = (String) removing.nextElement();
                attachments.remove(key);
                releaseOpenDatabase(key);
            }
        }
        java.util.Enumeration attached = live.keys();
        while (attached.hasMoreElements()) {
            String key = (String) attached.nextElement();
            if (attachments != null && attachments.containsKey(key)) {
                continue;
            }
            try {
                registerOpenDatabase(key);
            } catch (IOException beingDeleted) {
                // Attached already, and something else has claimed the file: it is being deleted
                // or converted right now. The reservation taken before the statement ran is the
                // first line of defence, but it cannot always be keyed on the file the engine
                // ends up opening -- a relative name resolves against the process directory for
                // SQLite and against this port's database directory here, and a bound parameter
                // is not read at all. So this is where it has to be stopped, and swallowing the
                // refusal is what let a delete run against a database SQLite still had open.
                //
                // An attach cannot be undone, but it can be reversed: the attachment is detached
                // and the caller told. Best effort, because a DETACH is itself refused inside a
                // transaction or while another connection holds the file -- the throw happens
                // either way, since a caller that is told nothing carries on writing through an
                // attachment whose file is about to be unlinked.
                detachEveryQuietly(live.get(key));
                attachmentRefusal = new IOException("An ATTACH named a database that is being "
                        + "deleted or converted, so it could not be attached: " + key + ". The "
                        + "attachment has been undone. Complete the delete or the key change "
                        + "first, or attach a database nothing else is claiming.", beingDeleted);
                continue;
            }
            if (attachments == null) {
                attachments = new java.util.Hashtable();
            }
            attachments.put(key, key);
        }
    }

    /// Reports an attachment a reconciliation had to undo.
    ///
    /// Thrown from the start of the next statement rather than from the one that attached,
    /// because ports reconcile from a `finally` and a throw from there replaces whatever failure
    /// the statement was already reporting -- the one error the caller most needs. So the attach
    /// is reversed as it is discovered, and the news waits for a place that can carry it.
    ///
    /// Late, but not lost, and not misattributed either: the message names an ATTACH rather than
    /// "this statement". The alternative is silence, and an application whose attachment quietly
    /// did not happen reads the absence of its tables as corruption.
    ///
    /// #### Throws
    ///
    /// - `IOException`: if a reconciliation undid an attachment and nothing has reported it yet
    protected void requireAttachmentsHeld() throws IOException {
        if (reconciling) {
            // The DETACH a reconciliation issues runs through the port's own execute and arrives
            // back here. Reporting into it would hand the refusal to the catch inside
            // `#detachQuietly(String)`, which swallows it -- losing the one thing this is for.
            return;
        }
        IOException refusal = attachmentRefusal;
        if (refusal != null) {
            attachmentRefusal = null;
            throw refusal;
        }
    }

    /// Detaches every schema the engine reported for one file.
    ///
    /// All of them, because the file is what is being protected and one file can be attached
    /// under several schema names at once. Leaving any of them attached leaves the file open to
    /// SQLite, which is the state this is undoing.
    ///
    /// #### Parameters
    ///
    /// - `held`: the schema names `PRAGMA database_list` gave for that file
    private void detachEveryQuietly(Object held) {
        // Typed by instanceof rather than by a cast, and taken as Object for that reason: a cast
        // that failed would not raise anything catchable on ParparVM, and the one thing this must
        // not do is fail silently.
        if (!(held instanceof java.util.Vector)) {
            return;
        }
        java.util.Vector schemas = (java.util.Vector) held;
        for (int iter = 0; iter < schemas.size(); iter++) {
            Object schema = schemas.elementAt(iter);
            if (schema instanceof String) {
                detachQuietly((String) schema);
            }
        }
    }

    /// Detaches a schema the engine reported, swallowing whatever it says about it.
    ///
    /// The caller is on its way to throwing already, and the reason it throws is the one worth
    /// reporting. A DETACH is refused inside a transaction and while the file is locked, and
    /// neither refusal changes what the caller has to be told.
    ///
    /// #### Parameters
    ///
    /// - `schema`: the schema name as `PRAGMA database_list` gave it
    private void detachQuietly(String schema) {
        if (schema == null || schema.length() == 0) {
            return;
        }
        try {
            // Quoted as an identifier, doubling any quote inside it, because a schema name is
            // whatever the ATTACH said it was and it reaches here unaltered.
            execute("DETACH DATABASE \"" + replaceAll(schema, "\"", "\"\"") + "\"");
        } catch (IOException stillAttached) {
            // Reported by the throw the caller is about to make.
        } catch (RuntimeException stillAttached) {
            // As above: a port that fails a DETACH some other way changes nothing here.
        }
    }

    /// The file an ATTACH statement names, or null when the statement does not name one outright.
    ///
    /// Only a quoted literal counts. `ATTACH DATABASE ? AS aux` is the ordinary parameterized
    /// form, and reading the `?` as a filename registered a path nobody asked for while the file
    /// actually attached went untracked -- worse than not looking, because it reads as protection.
    /// Anything that is not a literal is left alone: the attachment is then invisible to a delete,
    /// which is where this started, and that is the conservative end of a choice between doing
    /// nothing and doing something wrong.
    ///
    /// #### Parameters
    ///
    /// - `statement`: one ATTACH statement
    ///
    /// #### Returns
    ///
    /// the file it names, or null
    private static String attachmentFileOf(String statement) {
        String[] target = attachmentTargetTokens(statement);
        if (target.length != 1) {
            // Nothing, or an expression: neither names one file this can read.
            return null;
        }
        String found = target[0];
        if (found.length() == 0) {
            return null;
        }
        // Quoted, and single-quoted at that: a double-quoted word is an identifier to SQLite, and
        // a bare word is an expression -- a placeholder, a column, a concatenation. None of those
        // is a filename this can resolve.
        if (found.length() < 2 || found.charAt(0) != '\''
                || found.charAt(found.length() - 1) != '\'') {
            return null;
        }
        // Unescaped, so the name this resolves is the name the engine opened.
        String file = unquoteLiteral(found);
        if (file.length() == 0) {
            return null;
        }
        return asDatabaseArgument(file);
    }

    /// The tokens an ATTACH gives as its filename, between the keyword and `AS`.
    ///
    /// SQLite takes an expression there, not just a string: `ATTACH '/tmp/' || 'b.db' AS aux`
    /// and `ATTACH replace('/tmp/XXX.db','XXX','b') AS aux` both attach `/tmp/b.db`. Reading only
    /// the first token called the first one `/tmp/` -- a reservation on the wrong file, which
    /// reads as protection and is not -- and the second one nothing at all. Keeping every token
    /// is what lets the caller tell one literal apart from an expression it cannot evaluate.
    ///
    /// #### Parameters
    ///
    /// - `statement`: one ATTACH statement
    ///
    /// #### Returns
    ///
    /// the tokens of the filename, empty when the statement names none
    private static String[] attachmentTargetTokens(String statement) {
        // Split from the end of the keyword rather than from the second word, because a word is
        // whitespace delimited and SQL is not: "ATTACH('x.db')AS aux" attaches a database with no
        // space anywhere in it, and reading words would have kept "ATTACH('x.db')AS" together and
        // found no target at all.
        String[] words = splitWords(statement.substring(leadingKeywordEnd(statement)));
        java.util.Vector target = new java.util.Vector();
        for (String word : words) {
            String upper = word.toUpperCase();
            if (target.isEmpty() && "DATABASE".equals(upper)) {
                // The optional keyword, which is not part of the filename.
                continue;
            }
            if ("AS".equals(upper)) {
                break;
            }
            target.addElement(word);
        }
        // "ATTACH ('x.db') AS aux" is the same attachment as without the brackets, so a target
        // wrapped in balanced ones is unwrapped rather than counted as three tokens and refused.
        // Only when they are the outermost pair: "('a' || 'b')" still has an operator inside and
        // stays an expression this cannot resolve.
        while (target.size() > 2 && "(".equals(target.elementAt(0))
                && ")".equals(target.elementAt(target.size() - 1))) {
            target.removeElementAt(target.size() - 1);
            target.removeElementAt(0);
        }
        String[] out = new String[target.size()];
        target.copyInto(out);
        return out;
    }

    /// Whether the filename an ATTACH gives is one this can account for before it runs.
    ///
    /// A single quoted literal names its file outright. A single placeholder names it in the
    /// parameters, which the parameterized overload reserves. Anything else is an expression the
    /// engine evaluates and this cannot -- a concatenation, a function call, a column -- and the
    /// file it produces would be attached with nothing holding it.
    ///
    /// #### Parameters
    ///
    /// - `statement`: one ATTACH statement
    ///
    /// #### Returns
    ///
    /// whether the target can be resolved or reserved before the statement runs
    private static boolean isResolvableAttachment(String statement) {
        String[] target = attachmentTargetTokens(statement);
        if (target.length == 0) {
            // Not an attach of anything -- malformed, and the engine says so better than this can.
            return true;
        }
        if (target.length > 1) {
            return false;
        }
        String only = target[0];
        if (only.length() > 1 && only.charAt(0) == '\''
                && only.charAt(only.length() - 1) == '\'') {
            return !isAmbiguousUriFilename(unquoteLiteral(only));
        }
        return isParameterPlaceholder(only);
    }

    /// Which bound value a placeholder stands for, counting from one, or 0 when it cannot be told.
    ///
    /// `?NNN` names its own slot: `ATTACH ?2 AS aux` opens whatever the *second* parameter holds,
    /// whatever the first one is for. A bare `?` and the named forms take their position in the
    /// statement instead, and in an ATTACH the filename is the first thing that can be bound, so
    /// they are slot one -- a `KEY ?` after it is slot two.
    ///
    /// #### Parameters
    ///
    /// - `token`: the placeholder standing where the filename goes
    ///
    /// #### Returns
    ///
    /// the one-based parameter index, or 0 if the digits cannot be read
    private static int placeholderSlot(String token) {
        if (token.charAt(0) != '?' || token.length() == 1) {
            return 1;
        }
        int slot = 0;
        for (int iter = 1; iter < token.length(); iter++) {
            char c = token.charAt(iter);
            if (c < '0' || c > '9') {
                return 0;
            }
            slot = slot * 10 + (c - '0');
            if (slot > 999999) {
                // Beyond anything a call could bind, and past this a longer run of digits would
                // overflow into a slot that looks reasonable.
                return 0;
            }
        }
        return slot;
    }

    /// Whether a name is a URI whose meaning to SQLite differs from what this resolves it to.
    ///
    /// The collision is unavoidable: "file://" + an absolute path is this framework's own way of
    /// naming a database outside the database directory -- `#getDatabasePath(String)` hands one
    /// back for exactly that case -- and it is also the prefix of a SQLite URI. Refusing every
    /// name that begins with it would refuse the string this API tells applications to pass.
    ///
    /// They only disagree in specific shapes, and those are what this catches. SQLite strips a
    /// `?query` -- where `vfs=` can even choose something that is not a filesystem -- and
    /// percent-decodes what is left, while resolving "file://" here is a plain prefix strip. So
    /// `file:///data/x.db` means the same file either way and is allowed through, while
    /// `file:///data/x.db?mode=ro` and `file:///data/my%20db.sqlite` do not: the engine opens
    /// `/data/x.db` and `/data/my db.sqlite`, and the reservation would have been keyed on the
    /// query string and the escape. A guard that reports success on the wrong file is worse than
    /// one that refuses, so those are refused.
    ///
    /// A `file:` with anything other than two slashes after it -- `file:/data/x.db`, which SQLite
    /// also accepts -- is refused for the same reason: it is not the form this resolves.
    ///
    /// #### Parameters
    ///
    /// - `file`: the unquoted name an ATTACH gave
    ///
    /// #### Returns
    ///
    /// whether the engine would open something other than what this would key on
    private static boolean isAmbiguousUriFilename(String file) {
        if (file.length() < 5 || !"FILE:".equals(upperAscii(file.substring(0, 5)))) {
            return false;
        }
        if (!file.startsWith("file://")) {
            // Any other number of slashes is a URI spelling this does not resolve.
            return true;
        }
        return file.indexOf('?') >= 0 || file.indexOf('#') >= 0 || file.indexOf('%') >= 0;
    }

    /// Strips the single quotes from a literal and unescapes the doubled ones inside it.
    private static String unquoteLiteral(String token) {
        return replaceAll(token.substring(1, token.length() - 1), "''", "'");
    }

    /// Whether a token is a bound parameter in any of the spellings SQLite accepts.
    private static boolean isParameterPlaceholder(String token) {
        if (token.length() == 0) {
            return false;
        }
        char first = token.charAt(0);
        if (first == '?') {
            // ? and ?NNN. The digits are not checked: a malformed one is the engine's to reject,
            // and either way it is a parameter rather than a name this could have resolved.
            return true;
        }
        // :name, @name, $name and #name -- all four spellings SQLite binds by name, and the same
        // set SQLStatementSplitter.countParameters recognizes. Leaving one out here made a
        // statement the splitter counts as parameterized read as an expression instead, so
        // "ATTACH #file AS aux" was refused before the engine ever saw it.
        return (first == ':' || first == '@' || first == '$' || first == '#')
                && token.length() > 1;
    }

    /// The form of a file name a port resolves, for a name an ATTACH names either way.
    ///
    /// An absolute path is a custom database to every port, and they resolve one only when it
    /// arrives as a file URL. A bare name is left as it is, which is what a port expects for a
    /// database in its own directory. Both the quoted literal and the bound parameter go through
    /// here, so the same file keys the same whichever way the script named it.
    ///
    /// #### Parameters
    ///
    /// - `file`: a file name read from a statement or bound to one
    ///
    /// the name to resolve an identity from
    private static String asDatabaseArgument(String file) {
        if (file.charAt(0) == '/' || file.charAt(0) == '\\' || isDriveAbsolute(file)) {
            return "file://" + file;
        }
        return file;
    }

    /// Whether a Windows drive letter is followed by a root, rather than by a relative name.
    ///
    /// "C:\\db.sqlite" names one file; "C:db.sqlite" names whatever is beside the current
    /// directory *of drive C*, which is per-drive state the process carries and this API neither
    /// sees nor controls. Reading the second as absolute made it look resolvable, so the
    /// reservation was keyed on the literal while SQLite opened something else -- the same trap
    /// as an ordinary relative name, wearing a drive letter. Treating it as relative is what
    /// hands it to the check that refuses those.
    private static boolean isDriveAbsolute(String file) {
        if (file.length() < 3 || file.charAt(1) != ':') {
            return false;
        }
        char drive = file.charAt(0);
        if (!((drive >= 'a' && drive <= 'z') || (drive >= 'A' && drive <= 'Z'))) {
            return false;
        }
        char after = file.charAt(2);
        return after == '/' || after == '\\';
    }

    /// Splits a statement into words, keeping quoted runs together.
    private static String[] splitWords(String statement) {
        java.util.Vector words = new java.util.Vector();
        int at = 0;
        int length = statement.length();
        while (at < length) {
            // Comments count as space between words. "ATTACH /* here */ DATABASE 'x.db' AS aux"
            // is valid, and reading the comment as a word made "/*" the filename -- which parses
            // as nothing quoted, so the statement reserved no database at all.
            int trivia = skipLeadingTrivia(statement, at);
            if (trivia != at) {
                at = trivia;
                continue;
            }
            char c = statement.charAt(at);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == ';') {
                at++;
                continue;
            }
            if (c == '\'' || c == '"' || c == '`') {
                // Doubled delimiters are an escaped one, not the end: 'quoted''name.db' is a
                // single literal for a file whose name has an apostrophe in it. Stopping at the
                // first half of the pair read that as a database called "quoted" and registered
                // it, while the file SQLite really attached went untracked.
                int end = at + 1;
                while (end < length) {
                    if (statement.charAt(end) == c) {
                        if (end + 1 < length && statement.charAt(end + 1) == c) {
                            end += 2;
                            continue;
                        }
                        break;
                    }
                    end++;
                }
                if (end >= length) {
                    break;
                }
                words.addElement(statement.substring(at, end + 1));
                at = end + 1;
                continue;
            }
            if (c == '(' || c == ')' || c == ',') {
                // Punctuation is a token of its own. SQL does not need whitespace around it, so
                // reading words alone kept "('x.db')AS" together as one -- and a filename glued
                // to a bracket matches nothing this can resolve, which turned a statement SQLite
                // runs perfectly well into one this refused.
                words.addElement(statement.substring(at, at + 1));
                at++;
                continue;
            }
            int end = at;
            while (end < length) {
                char e = statement.charAt(end);
                if (e == ' ' || e == '\t' || e == '\n' || e == '\r' || e == ';') {
                    break;
                }
                if (e == '(' || e == ')' || e == ',') {
                    break;
                }
                // A comment ends a word without any space around it: DATABASE/*c*/'x.db' is three
                // tokens to SQLite, and reading it as one lost the filename inside a word that
                // matched nothing.
                if (e == '/' && end + 1 < length && statement.charAt(end + 1) == '*') {
                    break;
                }
                if (e == '-' && end + 1 < length && statement.charAt(end + 1) == '-') {
                    break;
                }
                end++;
            }
            words.addElement(statement.substring(at, end));
            at = end;
        }
        String[] out = new String[words.size()];
        words.copyInto(out);
        return out;
    }

    /// Replaces every occurrence of one string with another.
    private static String replaceAll(String text, String find, String with) {
        StringBuilder out = new StringBuilder(text.length());
        int at = 0;
        while (at < text.length()) {
            int next = text.indexOf(find, at);
            if (next < 0) {
                out.append(text.substring(at));
                break;
            }
            out.append(text.substring(at, next)).append(with);
            at = next + find.length();
        }
        return out.toString();
    }

    /// Releases what this connection held besides its own file.
    ///
    /// Every port calls this as it closes. SQLite drops a connection's attachments when it closes,
    /// so the registrations taken for them have to go at the same moment -- otherwise a database
    /// that was attached once could never be deleted again for the life of the process.
    protected void noteConnectionClosed() {
        if (attachments == null) {
            return;
        }
        java.util.Enumeration keys = attachments.elements();
        while (keys.hasMoreElements()) {
            releaseOpenDatabase((String) keys.nextElement());
        }
        attachments.clear();
    }

    /// Records the transaction control in the first statement of a script, and only that one.
    ///
    /// For the legacy hint, where a script runs as far as its first statement and the rest is
    /// discarded. Reading the whole string there would credit statements that never ran: a
    /// `BEGIN; COMMIT` would be read as opening and closing, when only the `BEGIN` was executed
    /// and the transaction is still open -- the direction that lets a key change run over it.
    ///
    /// #### Parameters
    ///
    /// - `sql`: the script that was handed to the engine
    protected void noteFirstStatementTransactionControl(String sql) {
        if (sql == null) {
            return;
        }
        String[] statements = SQLStatementSplitter.split(sql);
        if (statements.length > 0) {
            noteScriptTransactionControl(statements[0]);
        }
    }

    /// Reads a SAVEPOINT or RELEASE, which start and end a transaction when they are the outer one.
    ///
    /// A savepoint inside a transaction is a mark within it and changes nothing here. A savepoint
    /// outside one starts a real transaction, which SQLite holds open until that same savepoint is
    /// released -- so `SAVEPOINT s` followed by writes leaves work uncommitted exactly as `BEGIN`
    /// does, and a key change allowed underneath it installs rows that were never committed.
    ///
    /// Only the outermost name ends it: releasing an inner savepoint leaves the transaction open,
    /// and a `RELEASE` naming something else entirely is not this transaction's ending.
    ///
    /// Releasing an intermediate savepoint also releases everything above it, including any reuse
    /// of the outermost name, which this does not follow -- so a transaction can be reported open
    /// after SQLite has ended it. That is the direction that refuses a key change rather than
    /// allowing one over live work, and the ports that can ask their engine correct it on the next
    /// statement anyway.
    private void noteSavepointControl(String statement) {
        String keyword = leadingKeyword(statement);
        if ("SAVEPOINT".equals(keyword)) {
            String name = savepointName(statement, false);
            if (!inTransaction) {
                inTransaction = true;
                transactionDepth = 1;
                forgetSavepoints();
                savepointOwnsTransaction = true;
            }
            // Pushed whether or not a savepoint opened this transaction: inside a BEGIN the stack
            // never empties into anything, because the BEGIN owns the transaction.
            openSavepoints.addElement(name == null ? "" : name);
            return;
        }
        boolean rollbackTo = "ROLLBACK".equals(keyword) && hasToSavepoint(statement);
        if (!"RELEASE".equals(keyword) && !rollbackTo) {
            return;
        }
        if (openSavepoints.isEmpty()) {
            return;
        }
        // ROLLBACK TO names its savepoint after an optional TRANSACTION and an optional SAVEPOINT
        // keyword, so the name is read past both.
        int at = lastIndexOfSavepoint(rollbackTo
                ? rollbackToSavepointName(statement) : savepointName(statement, true));
        if (at < 0) {
            // Names something that is not open. SQLite answers that with an error and unwinds
            // nothing, so neither does this.
            return;
        }
        // Everything above it goes either way. The difference is the savepoint named: RELEASE
        // takes it too, ROLLBACK TO keeps it open so the caller can roll back to it again.
        if (rollbackTo) {
            at++;
        }
        while (openSavepoints.size() > at) {
            openSavepoints.removeElementAt(openSavepoints.size() - 1);
        }
        if (openSavepoints.isEmpty() && savepointOwnsTransaction) {
            // The outermost savepoint was the transaction, so releasing it ended the transaction.
            // Under a BEGIN these were marks inside somebody else's transaction, which ends on its
            // own COMMIT and not here.
            endTransactionCompletely();
        }
    }

    /// The savepoint a `ROLLBACK TO` names, upper cased, or null.
    ///
    /// `ROLLBACK [TRANSACTION] TO [SAVEPOINT] name`, so up to three optional words stand between
    /// the keyword and the name.
    private static String rollbackToSavepointName(String statement) {
        int at = skipLeadingTrivia(statement, endOfKeyword(statement, skipLeadingTrivia(statement, 0)));
        if ("TRANSACTION".equals(keywordAt(statement, at))) {
            at = skipLeadingTrivia(statement, endOfKeyword(statement, at));
        }
        if (!"TO".equals(keywordAt(statement, at))) {
            return null;
        }
        at = skipLeadingTrivia(statement, endOfKeyword(statement, at));
        if ("SAVEPOINT".equals(keywordAt(statement, at))) {
            at = skipLeadingTrivia(statement, endOfKeyword(statement, at));
        }
        return savepointNameAt(statement, at);
    }

    /// The position of the innermost open savepoint with this name, or -1.
    private int lastIndexOfSavepoint(String name) {
        if (name == null) {
            return -1;
        }
        for (int iter = openSavepoints.size() - 1; iter >= 0; iter--) {
            if (name.equals(openSavepoints.elementAt(iter))) {
                return iter;
            }
        }
        return -1;
    }

    /// The savepoint a SAVEPOINT or RELEASE names, upper cased, or null.
    ///
    /// `RELEASE` takes an optional `SAVEPOINT` keyword before the name. Quoting is stripped rather
    /// than honoured: SQLite compares savepoint names without case, so `SAVEPOINT s` is released by
    /// `RELEASE "S"`, and comparing the quoted spellings literally would leave the transaction
    /// looking open forever.
    ///
    /// #### Parameters
    ///
    /// - `statement`: a single statement whose first keyword is SAVEPOINT or RELEASE
    /// - `optionalSavepointKeyword`: whether to skip a `SAVEPOINT` word before the name
    private static String savepointName(String statement, boolean optionalSavepointKeyword) {
        int at = endOfKeyword(statement, skipLeadingTrivia(statement, 0));
        at = skipLeadingTrivia(statement, at);
        if (optionalSavepointKeyword && "SAVEPOINT".equals(keywordAt(statement, at))) {
            at = skipLeadingTrivia(statement, endOfKeyword(statement, at));
        }
        return savepointNameAt(statement, at);
    }

    /// The identifier starting at `from`, upper cased and unquoted, or null.
    private static String savepointNameAt(String statement, int at) {
        int length = statement.length();
        if (at >= length) {
            return null;
        }
        char quote = statement.charAt(at);
        if (quote == '"' || quote == '\'' || quote == '`' || quote == '[') {
            char closing = quote == '[' ? ']' : quote;
            int end = at + 1;
            StringBuilder name = new StringBuilder();
            while (end < length) {
                char c = statement.charAt(end);
                if (c == closing) {
                    if (quote != '[' && end + 1 < length && statement.charAt(end + 1) == closing) {
                        name.append(c);
                        end += 2;
                        continue;
                    }
                    break;
                }
                name.append(c);
                end++;
            }
            return upperAscii(name.toString());
        }
        int end = at;
        while (end < length && isIdentifierChar(statement.charAt(end))) {
            end++;
        }
        return end > at ? upperAscii(statement.substring(at, end)) : null;
    }

    /// Records the transaction state a port read back from its engine.
    ///
    /// The reliable answer where a script runs as a whole. SQLite stops at the first statement
    /// that fails and nothing outside can see which one that was, so reading the script cannot
    /// tell an unexecuted trailing `COMMIT` from an executed one -- and getting that wrong either
    /// clears the flag over a live transaction, which lets a key change replace the database
    /// underneath uncommitted work, or holds it over a finished one, which blocks every key change
    /// until the connection closes. The engine knows; ports that can ask it should.
    ///
    /// #### Parameters
    ///
    /// - `open`: whether the engine reports a transaction in progress
    protected void noteEngineTransactionState(boolean open) {
        if (open) {
            inTransaction = true;
            if (transactionDepth < 1) {
                transactionDepth = 1;
            }
            return;
        }
        endTransactionCompletely();
    }

    /// The transaction-control keyword a statement starts with, or null if it is not one.
    ///
    /// Shared so that a port which has to act on transaction control -- the simulator routes it
    /// through JDBC, because there the transaction is the connection's autocommit flag rather than
    /// something the driver reads back out of the SQL -- classifies it exactly as the tracking
    /// here does. Two copies of this drifted apart once already.
    ///
    /// Only a bare `ROLLBACK` counts: `ROLLBACK TO <savepoint>` unwinds within the transaction
    /// rather than ending it, as SAVEPOINT and RELEASE do.
    ///
    /// #### Parameters
    ///
    /// - `statement`: a single statement
    ///
    /// #### Returns
    ///
    /// `BEGIN`, `COMMIT`, `END`, `ROLLBACK`, or null
    protected static String transactionControlKeyword(String statement) {
        if (statement == null) {
            return null;
        }
        String keyword = leadingKeyword(statement);
        if ("BEGIN".equals(keyword) || "COMMIT".equals(keyword) || "END".equals(keyword)) {
            return keyword;
        }
        if ("ROLLBACK".equals(keyword) && !hasToSavepoint(statement)) {
            return "ROLLBACK";
        }
        return null;
    }

    /// The locking mode a `BEGIN` asks for: `IMMEDIATE`, `EXCLUSIVE` or `DEFERRED`.
    ///
    /// Reads the word after `BEGIN` rather than searching the statement for those names. The words
    /// are ordinary text anywhere else, so `/* IMMEDIATE migration */ BEGIN` and
    /// `BEGIN /* EXCLUSIVE note */ TRANSACTION` are both deferred -- and a port that searched would
    /// take a write lock on them that the same SQL does not take on any other platform.
    ///
    /// Anything that is not one of the three, including a bare `BEGIN` and the optional
    /// `TRANSACTION` keyword, is deferred, which is what SQLite does with it.
    ///
    /// #### Parameters
    ///
    /// - `statement`: a statement whose first keyword is `BEGIN`
    ///
    /// #### Returns
    ///
    /// `IMMEDIATE`, `EXCLUSIVE` or `DEFERRED`
    protected static String beginTransactionMode(String statement) {
        if (statement == null) {
            return "DEFERRED";
        }
        int after = endOfKeyword(statement, skipLeadingTrivia(statement, 0));
        String next = keywordAt(statement, skipLeadingTrivia(statement, after));
        if ("IMMEDIATE".equals(next) || "EXCLUSIVE".equals(next)) {
            return next;
        }
        return "DEFERRED";
    }

    /// The first word of a statement, upper cased, or an empty string.
    ///
    /// Comments count as whitespace here, because they do to the engine: `/* migration */ BEGIN`
    /// opens a transaction, and reading the keyword as empty would leave this believing none was
    /// opened. The Android port relies on the same fact deliberately, prefixing a comment to a
    /// ROLLBACK to get it past a statement classifier that reads the first three characters.
    /// Refuses transaction control handed to `executeQuery`.
    ///
    /// A cursor runs its statement when it is stepped, so `executeQuery("BEGIN")` opens a real
    /// transaction that nothing here recorded: `#isInTransaction()` answers false over an open
    /// one, a typed commit fails, and a key change is allowed across live work. Navigating the
    /// cursor could run the control statement a second time on top of that.
    ///
    /// The tracked ways in are `#beginTransaction()` and `execute`, both of which record what
    /// they ran. This is not a capability being withdrawn: a transaction control statement
    /// returns no rows, so asking for a cursor over one was never useful.
    ///
    /// Skipped under the legacy hint, which restores what each port used to do with it.
    ///
    /// #### Parameters
    ///
    /// - `sql`: the statement handed to executeQuery
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the statement is transaction control
    protected void requireQueryStatement(String sql) throws IOException {
        // The query counterpart of the same funnel `#reserveAttachments(String)` uses: a caller
        // that only ever reads would otherwise never be told its ATTACH was undone, and would
        // read "no such table" as a database that has lost its contents.
        requireAttachmentsHeld();
        if (isLegacyBehavior() || sql == null) {
            return;
        }
        String keyword = leadingKeyword(sql);
        if ("BEGIN".equals(keyword) || "COMMIT".equals(keyword) || "END".equals(keyword)
                || "ROLLBACK".equals(keyword) || "SAVEPOINT".equals(keyword)
                || "RELEASE".equals(keyword)) {
            throw new IOException("Transaction control cannot be run through executeQuery: a "
                    + "cursor runs its statement when it is stepped, so the transaction would be "
                    + "opened or closed without this database knowing. Use beginTransaction(), "
                    + "commitTransaction(), rollbackTransaction(), or execute(\"" + keyword
                    + " ...\").");
        }
        if ("ATTACH".equals(keyword) || "DETACH".equals(keyword)) {
            // The same fault as transaction control, in the one place it is most dangerous. A
            // cursor runs its statement when it is stepped, which is after the reservation would
            // have been taken and after the reconciliation would have read the engine back -- so
            // an attachment made this way is held by SQLite and absent from the registry, and a
            // delete or a key change on that file goes ahead underneath it.
            //
            // Also the only statement of the pair that can be made safe by refusing it. ATTACH
            // and DETACH return no rows, so a cursor over one was never useful, and execute()
            // runs both with the bookkeeping around them.
            throw new IOException("Attachment control cannot be run through executeQuery: a "
                    + "cursor runs its statement when it is stepped, so the database would be "
                    + attachedOrDetached(keyword) + " without this database knowing, and a "
                    + "delete or a key change on that file would not see it. Use execute(\""
                    + keyword + " ...\").");
        }
    }

    private static String attachedOrDetached(String keyword) {
        return "ATTACH".equals(keyword) ? "attached" : "detached";
    }

    private static String leadingKeyword(String statement) {
        return upperAscii(statement.substring(
                skipLeadingTrivia(statement, 0), leadingKeywordEnd(statement)));
    }

    /// Where the leading keyword ends, which is where the rest of the statement starts.
    private static int leadingKeywordEnd(String statement) {
        int length = statement.length();
        int end = skipLeadingTrivia(statement, 0);
        while (end < length && isKeywordChar(statement.charAt(end))) {
            end++;
        }
        return end;
    }

    /// Upper cases the ASCII letters and leaves everything else alone.
    ///
    /// Not `String#toUpperCase()`, which follows the process locale: in Turkish it maps `i` to a
    /// dotted capital, so `SAVEPOINT i` and `RELEASE I` stop matching each other while SQLite --
    /// which folds ASCII and only ASCII -- releases the savepoint. The tracker would then hold a
    /// transaction open forever and refuse every later begin and key change.
    private static String upperAscii(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder out = new StringBuilder(value.length());
        for (int iter = 0; iter < value.length(); iter++) {
            char c = value.charAt(iter);
            out.append(c >= 'a' && c <= 'z' ? (char) (c - ('a' - 'A')) : c);
        }
        return out.toString();
    }

    /// Whether a character continues an unquoted identifier.
    ///
    /// SQLite takes anything above ASCII as part of one. Stopping at the first such character
    /// would read two savepoints whose names share an ASCII prefix as one name, so releasing the
    /// inner one would end the outer -- clearing the flag while SQLite still holds the
    /// transaction, which is what lets a key change run underneath uncommitted work.
    private static boolean isIdentifierChar(char c) {
        return isKeywordChar(c) || (c >= '0' && c <= '9') || c == '_' || c == '$' || c >= 128;
    }

    private static boolean isKeywordChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    /// Skips whitespace and comments, which is what stands between a statement's start and its
    /// first keyword.
    private static int skipLeadingTrivia(String statement, int from) {
        int length = statement.length();
        int iter = from;
        while (iter < length) {
            char c = statement.charAt(iter);
            if (c <= ' ') {
                iter++;
            } else if (c == '-' && iter + 1 < length && statement.charAt(iter + 1) == '-') {
                iter += 2;
                while (iter < length && statement.charAt(iter) != '\n') {
                    iter++;
                }
            } else if (c == '/' && iter + 1 < length && statement.charAt(iter + 1) == '*') {
                iter += 2;
                while (iter + 1 < length
                        && !(statement.charAt(iter) == '*' && statement.charAt(iter + 1) == '/')) {
                    iter++;
                }
                iter = iter + 1 < length ? iter + 2 : length;
            } else {
                return iter;
            }
        }
        return length;
    }

    /// Whether a ROLLBACK names a savepoint, which unwinds to it rather than ending anything.
    ///
    /// Reads keywords rather than searching for the text " TO". SQL separates words with any
    /// whitespace or a comment, so `ROLLBACK\nTO x` and `ROLLBACK /* here */ TO x` are the same
    /// statement to the engine -- and `TRANSACTION` is optional in between, so
    /// `ROLLBACK TRANSACTION TO x` is that statement too. Missing any of them clears the flag
    /// while SQLite stays inside the transaction: the caller's own commit is then rejected as
    /// having nothing to commit, a key change is allowed underneath it, and on the simulator the
    /// whole transaction is discarded by a `conn.rollback()` that should have been a savepoint
    /// unwind.
    private static boolean hasToSavepoint(String statement) {
        int after = endOfKeyword(statement, skipLeadingTrivia(statement, 0));
        String next = keywordAt(statement, after);
        if ("TRANSACTION".equals(next)) {
            after = endOfKeyword(statement, skipLeadingTrivia(statement, after));
            next = keywordAt(statement, after);
        }
        return "TO".equals(next);
    }

    /// The index one past the keyword starting at `from`.
    private static int endOfKeyword(String statement, int from) {
        int length = statement.length();
        int end = from;
        while (end < length && isKeywordChar(statement.charAt(end))) {
            end++;
        }
        return end;
    }

    /// The keyword following `from`, upper cased, or an empty string.
    private static String keywordAt(String statement, int from) {
        int start = skipLeadingTrivia(statement, from);
        int end = endOfKeyword(statement, start);
        return upperAscii(statement.substring(start, end));
    }

    /// Connections open on each database file, by a key the port supplies.
    ///
    /// Only a key change needs this, and every port needs it: rotating a key rewrites the file's
    /// pages under a new key and updates only the connection that asked. Another connection to the
    /// same file keeps the old one, and its next read of a rewritten page fails -- on the engines
    /// that convert by export and rename, that connection is left writing to a file that is no
    /// longer the database at all. There is no way to rotate a key underneath a second connection,
    /// so the ports refuse instead.
    private static final java.util.Hashtable OPEN_DATABASES = new java.util.Hashtable();

    /// Files whose key is being changed right now.
    ///
    /// Counting open connections answers "is anybody else here" only at the instant it is asked.
    /// A rewrite takes longer than that, so the answer has to be held for its whole length or a
    /// connection opened a moment later still ends up reading a file that changed underneath it.
    private static final java.util.Hashtable REKEYING_DATABASES = new java.util.Hashtable();

    /// Open connections whose file could not be identified.
    ///
    /// A port that cannot say which file a connection holds -- the simulator wrapping a JDBC
    /// connection whose URL names nothing it can resolve -- has to be counted anyway. Leaving such
    /// a connection out of the registry only refuses its own key change, while another handle to
    /// the same file still sees itself as alone and rewrites the file underneath it. Counting
    /// makes every key change wait until it closes, which is the only answer available without
    /// knowing what it holds.
    private static int unidentifiedOpenDatabases;

    /// A path reduced to one spelling, for use as an open-database registry key.
    ///
    /// Two names for one file have to reach the registry as one entry, or the claim a key change
    /// takes does not cover the other connection and the file is rewritten underneath it. The ports
    /// with a real filesystem behind them (Android, the simulator) ask it to canonicalize, which
    /// also resolves symlinks. The ports translated ahead of time have no such call to make, so
    /// this collapses what can be collapsed without touching the disk: repeated separators, `.`
    /// segments, and `..` against the segment before it.
    ///
    /// A symlink still reaches the registry under two names. That is a smaller hole than `/a/./b`
    /// and `/a/b` counting as different databases, which is what an application writing a custom
    /// path actually produces.
    ///
    /// #### Parameters
    ///
    /// - `path`: a native filesystem path, or null
    ///
    /// #### Returns
    ///
    /// the reduced path, or null for a null input
    /// The shared path reduction, for a port that needs it outside a Database instance.
    ///
    /// The implementations resolve a managed key's implicit alias from this, so that two accepted
    /// spellings of one file derive one key rather than two.
    ///
    /// #### Parameters
    ///
    /// - `path`: a native filesystem path, or null
    ///
    /// #### Returns
    ///
    /// the reduced path, or null for a null input
    public static String normalizeDatabaseKey(String path) {
        return normalizeDatabasePathKey(path);
    }

    protected static String normalizeDatabasePathKey(String path) {
        if (path == null) {
            return null;
        }
        boolean absolute = path.length() > 0 && path.charAt(0) == '/';
        java.util.Vector segments = new java.util.Vector();
        int at = 0;
        int length = path.length();
        while (at < length) {
            int slash = path.indexOf('/', at);
            String segment = slash < 0 ? path.substring(at) : path.substring(at, slash);
            at = slash < 0 ? length : slash + 1;
            if (segment.length() == 0 || ".".equals(segment)) {
                continue;
            }
            if ("..".equals(segment) && !segments.isEmpty()
                    && !"..".equals(segments.elementAt(segments.size() - 1))) {
                segments.removeElementAt(segments.size() - 1);
                continue;
            }
            if ("..".equals(segment) && absolute) {
                // The root has no parent, so this segment names nothing and SQLite would not find
                // it either. Dropping it keeps two spellings of the same nonexistent path equal.
                continue;
            }
            segments.addElement(segment);
        }
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < segments.size(); iter++) {
            if (iter > 0 || absolute) {
                out.append('/');
            }
            out.append((String) segments.elementAt(iter));
        }
        if (out.length() == 0) {
            return absolute ? "/" : path;
        }
        return out.toString();
    }

    /// Records that a connection to a database file has been opened.
    ///
    /// Ports call this once they have a connection, and `#releaseOpenDatabase(String)` when they
    /// let it go. A port whose engine cannot be given two connections to one file need not call
    /// either.
    ///
    /// #### Parameters
    ///
    /// - `key`: identifies the file, canonically enough that two spellings of one path agree
    protected static synchronized void registerOpenDatabase(String key) throws IOException {
        if (key == null) {
            unidentifiedOpenDatabases++;
            return;
        }
        if (DELETING_DATABASES.containsKey(key)) {
            // The file is being unlinked. A connection opened now would be attached to it as it
            // loses its name, which is the outcome the delete guard exists to prevent -- and it
            // cannot see this connection, because it checked before this call.
            throw new IOException("The database " + key + " is being deleted. Opening it now "
                    + "would return a connection to a file that is about to be removed.");
        }
        if (REKEYING_DATABASES.containsKey(key)) {
            // A key change is a file rewrite, and it is not over when the claim is taken -- it is
            // over when the last page is written. Letting an open through in the middle hands back
            // a connection keyed to whichever half of the file it happened to read.
            throw new DatabaseEncryptionException(DatabaseEncryptionException.MIGRATION_FAILED,
                    "The database " + key + " is having its key changed. Opening it now would key"
                    + " the connection to a file that is being rewritten under it. Retry once the"
                    + " key change returns.");
        }
        Integer count = (Integer) OPEN_DATABASES.get(key);
        OPEN_DATABASES.put(key, Integer.valueOf(count == null ? 1 : count.intValue() + 1));
    }

    /// Records that a connection to a database file has been closed.
    ///
    /// #### Parameters
    ///
    /// - `key`: the key the connection was registered under
    protected static synchronized void releaseOpenDatabase(String key) {
        if (key == null) {
            if (unidentifiedOpenDatabases > 0) {
                unidentifiedOpenDatabases--;
            }
            return;
        }
        Integer count = (Integer) OPEN_DATABASES.get(key);
        if (count == null) {
            return;
        }
        if (count.intValue() <= 1) {
            OPEN_DATABASES.remove(key);
        } else {
            OPEN_DATABASES.put(key, Integer.valueOf(count.intValue() - 1));
        }
    }

    /// Rejects a key change while the same file is open more than once.
    ///
    /// Ports call this from `#changeKey(DatabaseConfig)`, after
    /// `#checkNoTransactionForKeyChange()`. The count includes the connection asking, so more than
    /// one means somebody else holds the file too.
    ///
    /// #### Parameters
    ///
    /// - `key`: the key this connection was registered under
    ///
    /// #### Throws
    ///
    /// - `IOException`: if another connection has the same file open
    protected static synchronized void requireSoleConnectionForKeyChange(String key)
            throws IOException {
        if (key == null) {
            // A connection that never said which file it holds cannot be checked against the ones
            // that did, so there is no answer to give -- and the failure of a wrong answer here is
            // a database rewritten under another connection. Refusing is the only safe reading of
            // "I do not know". The simulator's connection-taking constructor is how this arises.
            throw new DatabaseEncryptionException(DatabaseEncryptionException.MIGRATION_FAILED,
                    "This database was opened from a connection rather than by name, so there is no"
                    + " way to tell whether anything else has the same file open. Open it with"
                    + " Database.openOrCreate to change its key.");
        }
        if (unidentifiedOpenDatabases > 0) {
            // Something is open that could not say which file it holds, so it could be this one.
            // Rewriting the file while it reads through the change is the failure this guard
            // exists for, and an unidentified connection cannot be ruled out.
            throw new DatabaseEncryptionException(DatabaseEncryptionException.MIGRATION_FAILED,
                    "A database opened from a connection is still open, and there is no way to tell"
                    + " whether it holds this file. Close it before changing this database's key.");
        }
        Integer count = (Integer) OPEN_DATABASES.get(key);
        if (count != null && count.intValue() > 1) {
            throw new DatabaseEncryptionException(DatabaseEncryptionException.MIGRATION_FAILED,
                    "The database " + key + " is open more than once, and changing its key rewrites"
                    + " it under the new one for this connection only. Close the others first;"
                    + " reads through them would fail once they reached a rewritten page.");
        }
        if (REKEYING_DATABASES.put(key, key) != null) {
            throw new DatabaseEncryptionException(DatabaseEncryptionException.MIGRATION_FAILED,
                    "The database " + key + " is already having its key changed. Two rewrites of"
                    + " one file interleave into a database that opens under neither key.");
        }
    }

    /// Ends the exclusive claim `#requireSoleConnectionForKeyChange(String)` took.
    ///
    /// Ports call this from a `finally` around the rewrite, so a key change that throws does not
    /// leave the file barred from opening for the rest of the process.
    ///
    /// #### Parameters
    ///
    /// - `key`: the key the claim was taken under
    protected static synchronized void releaseKeyChangeClaim(String key) {
        if (key != null) {
            REKEYING_DATABASES.remove(key);
        }
    }

    /// Rejects a key change while a transaction is open.
    ///
    /// Ports call this at the top of `#changeKey(DatabaseConfig)`. Re-keying is not a statement
    /// inside the transaction: depending on the engine it either rewrites the file in place or
    /// exports into a new one and swaps it under the connection. Either way the open transaction
    /// has nowhere to land -- an export copies the uncommitted rows into the file that becomes the
    /// database, and a following commit or rollback addresses a connection that has no transaction
    /// to end. Refusing is the only outcome that keeps `commit` and `rollback` meaning what they
    /// say, and the caller loses nothing: it can end the transaction and change the key after.
    ///
    /// #### Throws
    ///
    /// - `IOException`: if a transaction is open
    protected void checkNoTransactionForKeyChange() throws IOException {
        if (inTransaction) {
            throw new DatabaseEncryptionException(DatabaseEncryptionException.MIGRATION_FAILED,
                    "The database key cannot be changed while a transaction is open, because the "
                    + "conversion replaces the database under it. Commit or roll back first.");
        }
    }

    /// Rejects a nested `#beginTransaction()`, then records that one is open.
    ///
    /// Transactions are flat: only that model is expressible on all of the engines behind this
    /// API. Ports call this at the top of `#beginTransaction()`. In legacy mode the check is
    /// skipped, because a nested begin used to be accepted on Android.
    ///
    /// #### Throws
    ///
    /// - `IOException`: if a transaction is already open
    /// Whether this engine counts nested transactions rather than rejecting the second one.
    ///
    /// Only Android's wrapper does, and only that port's legacy behaviour allowed nesting. On the
    /// others a second BEGIN reaches SQLite and fails, and the port clears its flag on the way out
    /// -- so allowing the call would report no transaction while the first one is still open, and
    /// a caller that caught the expected failure could then change the key across it.
    protected boolean supportsNestedTransactions() {
        return false;
    }

    protected void checkBeginTransaction() throws IOException {
        if (inTransaction && !(isLegacyBehavior() && supportsNestedTransactions())) {
            throw new IOException("A transaction is already in progress on this database. "
                    + "Transactions do not nest; commit or roll back the current one first.");
        }
        if (inTransaction) {
            // Legacy nesting: the engine ref-counts, so this begin has to be counted too.
            transactionDepth++;
            return;
        }
        inTransaction = true;
        transactionDepth = 1;
        forgetSavepoints();
    }

    /// Rejects a commit or rollback with no open transaction.
    ///
    /// Ports call this at the top of `#commitTransaction()` and `#rollbackTransaction()`, and
    /// `#markTransactionEnded()` once the engine has ended it. The two are separate so that a
    /// port can end the transaction on a path that does not commit it, which is what
    /// `#abandonFailedCommit(Throwable)` does.
    ///
    /// In legacy mode the check is skipped.
    ///
    /// #### Throws
    ///
    /// - `IOException`: if no transaction is open
    protected void checkEndTransaction() throws IOException {
        if (!inTransaction && !isLegacyBehavior()) {
            throw new IOException("No transaction is in progress on this database");
        }
    }

    /// Records that a transaction has actually ended. Call only after the engine has committed or
    /// rolled back successfully.
    protected void markTransactionEnded() {
        if (transactionDepth > 1) {
            // An inner end under the legacy hint. The engine is still holding the outer one.
            transactionDepth--;
            return;
        }
        endTransactionCompletely();
    }

    /// Drops what is remembered about savepoints, for a transaction that has ended by other means.
    private void forgetSavepoints() {
        openSavepoints.removeAllElements();
        savepointOwnsTransaction = false;
    }

    /// Ends the transaction outright, however many begins are outstanding.
    ///
    /// For the paths where the engine itself has ended it -- a COMMIT in a script, an engine that
    /// reports autocommit -- rather than one nested end.
    private void endTransactionCompletely() {
        inTransaction = false;
        transactionDepth = 0;
        forgetSavepoints();
    }

    /// Discards a transaction whose commit failed, and builds the exception to report it with.
    ///
    /// A commit that fails cannot be retried, so the only remaining outcome is a rollback. The
    /// engines disagree about what they leave behind: Android has already ended the transaction
    /// by the time it reports the failure, while the SQLite C API and JDBC leave it open. Ports
    /// call this from the failure path of `#commitTransaction()`, after making a best effort to
    /// roll back, so that callers see one behavior everywhere -- no transaction is open, and
    /// `#beginTransaction()` works again.
    ///
    /// #### Parameters
    ///
    /// - `cause`: the failure the engine reported
    ///
    /// #### Returns
    ///
    /// the exception the caller should throw
    protected IOException abandonFailedCommit(Throwable cause) {
        markTransactionEnded();
        String message = cause == null ? null : cause.getMessage();
        if (message == null) {
            message = "The transaction could not be committed";
        }
        return new IOException(message, cause);
    }

    /// Starts a transaction.
    ///
    /// Transactions are flat. Calling this while a transaction is already open throws, and
    /// committing or rolling back returns the connection to autocommit. Closing a database with
    /// an open transaction rolls it back.
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the database is not open, or a transaction is already in progress
    public abstract void beginTransaction() throws IOException;

    /// Commits current transaction
    ///
    /// **NOTE:** Not supported in Javascript port.   This method will do nothing when running in Javascript.
    ///
    /// #### Throws
    ///
    /// - `IOException`: if database is not opened or transaction was not started
    public abstract void commitTransaction() throws IOException;

    /// Rolls back current transaction
    ///
    /// **NOTE:** Not supported in Javascript port.   This method will do nothing when running in Javascript.
    ///
    /// #### Throws
    ///
    /// - `IOException`: if database is not opened or transaction was not started
    public abstract void rollbackTransaction() throws IOException;

    /// Closes the database
    ///
    /// #### Throws
    ///
    /// - `IOException`
    public abstract void close() throws IOException;

    /// Execute an update query.
    /// Used for INSERT, UPDATE, DELETE and similar sql statements.
    ///
    /// #### Parameters
    ///
    /// - `sql`: the sql to execute
    ///
    /// #### Throws
    ///
    /// - `IOException`
    public abstract void execute(String sql) throws IOException;

    /// Execute an update query with params.
    /// Used for INSERT, UPDATE, DELETE and similar sql statements.
    /// The sql can be constructed with '?' and the params will be binded to the
    /// query
    ///
    /// #### Parameters
    ///
    /// - `sql`: the sql to execute
    ///
    /// - `params`: to bind to the query where the '?' exists
    ///
    /// #### Throws
    ///
    /// - `IOException`
    public abstract void execute(String sql, String[] params) throws IOException;

    /// Execute an update query with params.
    /// Used for INSERT, UPDATE, DELETE and similar sql statements.
    /// The sql can be constructed with '?' and the params will be binded to the
    /// query
    ///
    /// #### Parameters
    ///
    /// - `sql`: the sql to execute
    ///
    /// - `params`: @param params to bind to the query where the '?' exists, supported object
    ///               types are String, byte[], Double, Long and null
    ///
    /// #### Throws
    ///
    /// - `IOException`
    public void execute(String sql, Object... params) throws IOException {
        if (params == null || params.length == 0) {
            execute(sql);
        } else {
            execute(sql, coerceToText(params, "execute"));
        }
    }

    /// Renders parameters as text for ports that have not implemented typed binding.
    ///
    /// This is the fallback path only. Ports that can bind by type override the varargs methods
    /// and never reach here, which is why hitting a `byte[]` is an error rather than something to
    /// paper over: silently storing the result of `byte[].toString()` would write the array's
    /// identity hash into the database.
    ///
    /// #### Parameters
    ///
    /// - `params`: the parameters supplied by the caller
    ///
    /// - `operation`: the calling method name, used in the error message
    ///
    /// #### Returns
    ///
    /// the parameters rendered as text, preserving nulls
    ///
    /// #### Throws
    ///
    /// - `IOException`: if a parameter is a `byte[]` and this port cannot bind blobs
    protected static String[] coerceToText(Object[] params, String operation) throws IOException {
        int len = params.length;
        String[] strParams = new String[len];
        for (int i = 0; i < len; i++) {
            if (params[i] instanceof byte[]) {
                throw new IOException("This platform cannot bind a byte[] parameter in " + operation
                        + "(). Check Database.isBlobQueryParameterSupported() before passing blobs.");
            }
            if (params[i] == null) {
                strParams[i] = null;
            } else {
                strParams[i] = params[i].toString();
            }
        }
        return strParams;
    }

    /// This method should be called with SELECT type statements that return
    /// row set.
    ///
    /// #### Parameters
    ///
    /// - `sql`: the sql to execute
    ///
    /// - `params`: to bind to the query where the '?' exists
    ///
    /// #### Returns
    ///
    /// a cursor to iterate over the results
    ///
    /// #### Throws
    ///
    /// - `IOException`
    public abstract Cursor executeQuery(String sql, String[] params) throws IOException;

    /// This method should be called with SELECT type statements that return
    /// row set it accepts object with params.
    ///
    /// #### Parameters
    ///
    /// - `sql`: the sql to execute
    ///
    /// - `params`: @param params to bind to the query where the '?' exists, supported object
    ///               types are String, byte[], Double, Long and null
    ///
    /// #### Returns
    ///
    /// a cursor to iterate over the results
    ///
    /// #### Throws
    ///
    /// - `IOException`
    public Cursor executeQuery(String sql, Object... params) throws IOException {
        if (params == null || params.length == 0) {
            return executeQuery(sql);
        }
        return executeQuery(sql, coerceToText(params, "executeQuery"));
    }

    /// This method should be called with SELECT type statements that return
    /// row set.
    ///
    /// #### Parameters
    ///
    /// - `sql`: the sql to execute
    ///
    /// #### Returns
    ///
    /// a cursor to iterate over the results
    ///
    /// #### Throws
    ///
    /// - `IOException`
    public abstract Cursor executeQuery(String sql) throws IOException;

}
