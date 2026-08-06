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
        Display.getInstance().delete(databaseName);
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
    /// **NOTE:** This method will return null in the Javascript port.
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
        Database db = Display.getInstance().openOrCreateForRekey(databaseName);
        try {
            db.changeKey(config);
        } finally {
            db.close();
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
        Database db = openOrCreate(databaseName, config);
        try {
            db.changeKey(DatabaseConfig.plain());
        } finally {
            db.close();
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
        return ManagedKeys.forget(keyAlias);
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

    /// Returns the number of rows a cursor holds, or -1 when that is not cheaply knowable.
    ///
    /// #### Parameters
    ///
    /// - `cursor`: the cursor to measure
    ///
    /// #### Returns
    ///
    /// the row count, or -1 when unknown
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
        if (keyMaterial.length() > 2 && keyMaterial.startsWith("x'") && keyMaterial.endsWith("'")) {
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

    /// Rejects a nested `#beginTransaction()`, then records that one is open.
    ///
    /// Transactions are flat: only that model is expressible on all of the engines behind this
    /// API. Ports call this at the top of `#beginTransaction()`. In legacy mode the check is
    /// skipped, because a nested begin used to be accepted on Android.
    ///
    /// #### Throws
    ///
    /// - `IOException`: if a transaction is already open
    protected void checkBeginTransaction() throws IOException {
        if (inTransaction && !isLegacyBehavior()) {
            throw new IOException("A transaction is already in progress on this database. "
                    + "Transactions do not nest; commit or roll back the current one first.");
        }
        inTransaction = true;
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
        inTransaction = false;
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
