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
/// SQLite database access, with optional encryption at rest.
///
/// SQLite is a small embedded SQL database available on every platform Codename One targets. This
/// package is a thin, portable API over it: similar in spirit to JDBC, but without the pluggable
/// driver abstractions that make no sense for a local file.
///
/// For a handful of values prefer `com.codename1.io.Storage`, which is simpler and more portable.
/// Reach for SQL when you have tabular data, need queries over it, or have enough of it that
/// loading the lot into memory is not reasonable.
///
/// ```java
/// Database db = Database.openOrCreate("myapp.db");
/// db.execute("CREATE TABLE IF NOT EXISTS notes (id INTEGER PRIMARY KEY, body TEXT)");
/// db.execute("INSERT INTO notes (body) VALUES (?)", new Object[] {"remember the milk"});
/// Cursor cur = db.executeQuery("SELECT id, body FROM notes ORDER BY id");
/// while (cur.next()) {
///     Row row = cur.getRow();
///     System.out.println(row.getInteger(0) + ": " + row.getString(1));
/// }
/// cur.close();
/// db.close();
/// ```
///
/// # The portable contract
///
/// Everything below holds on every platform that provides a database. Where a platform used to
/// behave differently, `Database#isLegacyBehavior()` restores the old behaviour; see
/// **Legacy compatibility** at the end.
///
/// ## Cursors and positions
///
/// Positions are counted from **zero**. A freshly returned cursor sits **before** the first row.
///
/// - `Cursor#getPosition()` reports -1 before any successful move, then 0 on the first row. Once
///   the result set is exhausted it reports the row count, one past the last row.
/// - `Cursor#next()` advances one row and returns false at the end.
/// - `Cursor#first()` moves **onto** the first row and returns true only if a row exists. It
///   returns false for an empty result set.
/// - `Cursor#last()` moves onto the last row, false if there are none.
/// - `Cursor#prev()` moves back one row, false when already at or before the first.
/// - `Cursor#position(int)` moves to an absolute row, false if out of range. Passing -1 rewinds to
///   before the first row and returns false.
/// - `Cursor#getRow()` is valid only while the cursor is on a row, and throws otherwise.
/// - Column metadata -- `getColumnCount`, `getColumnName`, `getColumnIndex` -- is available as soon
///   as the query returns, before the first `next()`.
/// - `getColumnIndex` is case-insensitive, returns -1 for an unknown name, and matches the
///   **result set label**, so a column selected as `SELECT a AS b` is found under `b`.
/// - A column index the result set does not have raises `java.io.IOException`. It is never
///   reported as a null value: SQLite's own C API answers an out-of-range index with SQL NULL, and
///   passing that on would make a mistyped index indistinguishable from stored data.
///
/// Forward iteration with `next()` costs the same everywhere. Seeking backwards or to an absolute
/// row is cheap on Android and costs O(distance from the start) elsewhere, because SQLite
/// statements only step forward and a backward seek is a rewind and re-step. A consequence worth
/// knowing: a cursor is a repeatable read only inside a transaction, since a concurrent write
/// between the two passes can change what the second one sees.
///
/// ## Text
///
/// Text is stored and returned exactly as given, for every character a Java `String` can hold. That
/// includes characters outside ASCII, characters outside the Basic Multilingual Plane, and the
/// character with code point zero, which SQLite stores in a TEXT value like any other. A string
/// written on one platform reads back identical on every other.
///
/// ## Statements
///
/// `Database#execute(java.lang.String)` runs **every** statement in the string, separated by
/// semicolons. Semicolons inside string literals, quoted identifiers, comments and
/// `CREATE TRIGGER` bodies do not separate statements.
///
/// The parameterized forms -- `execute(String, String[])`, `execute(String, Object[])` and all the
/// `executeQuery` variants -- take exactly **one** statement, and throw if given more. They do not
/// silently discard the remainder.
///
/// `executeQuery` validates and executes the statement before it returns, so a malformed query
/// fails there rather than from the first `next()`.
///
/// ## Parameters
///
/// The `Object[]` forms bind by runtime type:
///
/// | Java type | bound as |
/// | --- | --- |
/// | `null` | NULL |
/// | `byte[]` | BLOB |
/// | `String`, `Character` | TEXT |
/// | `Byte`, `Short`, `Integer`, `Long` | INTEGER |
/// | `Float`, `Double` | REAL |
/// | `Boolean` | INTEGER, 0 or 1 |
/// | anything else | TEXT, via `toString()` |
///
/// `java.util.Date` is deliberately not special-cased; it falls through to `toString()`. Store
/// dates as an explicit epoch value if you want them comparable in SQL.
///
/// The `String[]` forms bind every element as TEXT, and a null element binds SQL NULL rather than
/// throwing. Passing null instead of a parameter array is the same as calling the form that takes
/// no parameters. Supplying a different number of parameters than the statement has placeholders
/// throws.
///
/// Blob values can always be written. Using one as a *query* parameter needs engine support that
/// not every port has, so check `Database#isBlobQueryParameterSupported()` first.
///
/// ## Reading values
///
/// Column indexes are zero-based. `getString` and `getBlob` return null for a SQL NULL, and the
/// numeric getters return 0. `RowExt#wasNull()` distinguishes a stored zero from a NULL and
/// reports **false** before any value has been read.
///
/// `getInteger` and `getShort` narrow the stored 64-bit value; `getFloat` narrows the stored
/// double. That narrowing is defined, not undefined.
///
/// ## Transactions
///
/// Transactions are **flat**. Only that model is expressible on all of the engines behind this
/// API, so it is the one guaranteed here.
///
/// - `beginTransaction` throws if a transaction is already open.
/// - `commitTransaction` and `rollbackTransaction` throw if none is open, and both return the
///   connection to autocommit.
/// - A commit that throws still ends the transaction. A deferred constraint is checked at commit
///   time, so that is where a commit fails, and the engines disagree about what they leave
///   behind: Android has already ended the transaction by the time it reports the failure, while
///   the SQLite C API and JDBC leave it open. The port reconciles that, so a failed commit always
///   leaves the database with no transaction open and ready for a new one. Do not roll back
///   afterwards -- there is nothing left to roll back, and the call throws.
/// - Closing a database with an open transaction rolls it back.
/// - Transactions belong to a single `Database` instance, not to the process.
///
/// ## Errors and lifecycle
///
/// Every failure is an `java.io.IOException` carrying the engine's message and, where there is
/// one, the underlying cause. No port throws `RuntimeException` from these methods or logs a stack
/// trace on its way out.
///
/// `close()` is idempotent on both `Database` and `Cursor`. Any other method on a closed object
/// throws. Closing a database invalidates its cursors.
///
/// A `Database` and its cursors are **not thread safe**. Use one per thread, or wrap it in
/// `ThreadSafeDatabase`.
///
/// ## Paths
///
/// When `Database#isCustomPathSupported()` is true the name may instead be a `file://` URL from
/// `com.codename1.io.FileSystemStorage`. When it is false the name must not contain a path
/// separator, and passing one throws `IllegalArgumentException`.
///
/// # Encryption
///
/// Pass a `DatabaseConfig` to encrypt a database at rest:
///
/// ```java
/// if (Database.isEncryptionSupported()) {
///     DatabaseConfig config = DatabaseConfig.managed();
///     Database db = Database.openOrCreate("secure.db", config);
///     config.wipe();
/// }
/// ```
///
/// Requesting encryption on a platform that cannot provide it always fails with
/// `DatabaseEncryptionException#NOT_SUPPORTED`. It never quietly returns a plaintext database.
///
/// An existing database can be converted in place with `Database#encrypt(java.lang.String,
/// com.codename1.db.DatabaseConfig)` and back with `Database#decrypt(java.lang.String,
/// com.codename1.db.DatabaseConfig)`, and an open one re-keyed with
/// `Database#changeKey(com.codename1.db.DatabaseConfig)`. The engine performs each conversion as a
/// single transaction and preserves schema metadata such as `PRAGMA user_version`.
///
/// ## On-disk format
///
/// Every platform reads and writes one format, so a database file is portable between them and can
/// be opened in the simulator for debugging. The parameters are fixed: AES-256 in CBC mode,
/// PBKDF2-HMAC-SHA512 key derivation at 256000 iterations, a 4096 byte page size and per-page
/// HMAC-SHA512. Raw and managed keys are applied directly, with no key derivation.
///
/// ## Security
///
/// Read `DatabaseConfig` before choosing a key mode. In short: a passphrase compiled into your
/// source is not a secret, encryption protects data at rest and nothing else, managed keys are not
/// recoverable if the platform key store entry is lost, and the simulator's key storage is
/// software only and must not be treated as evidence that a real device is protected.
///
/// # Legacy compatibility
///
/// This API predates the contract above, and its behaviour used to differ between platforms.
/// Setting the `db.legacy` build hint, or calling `Database#setLegacyBehavior(boolean)` before the
/// first database call, restores each platform's previous behaviour exactly.
///
/// An Ant project that references this package is built in compatibility mode by default, because
/// it predates the contract and a rebuild should not quietly change how its queries behave. A
/// Maven project gets the contract above. An explicit `db.legacy` overrides that in either
/// direction, so an Ant project can opt in with `false` and a Maven project can pin compatibility
/// mode with `true`.
///
/// What compatibility mode restores:
///
/// | Restored behaviour | Platforms |
/// | --- | --- |
/// | `first()` rewinds without landing on a row and always reports success | iOS |
/// | `getPosition()` counts from one | Simulator |
/// | `wasNull()` reports true before any value has been read | Android, iOS |
/// | Parameters are bound as text rather than by type | iOS |
/// | `execute(String)` runs only the first statement of a script | Android, Simulator |
/// | The parameterized forms silently discard statements after the first | all |
/// | A nested `beginTransaction()` is accepted | Android |
/// | Malformed SQL surfaces from `next()` rather than from `executeQuery` | Android |
/// | `rollbackTransaction` leaves the connection outside autocommit | Simulator |
/// | `getColumnName` reports the table column rather than the result set label | Simulator |
///
/// The flag deliberately does **not** cover defects, nor capabilities that previously threw and
/// now work -- `last()`, `prev()` and `position(int)` on iOS and the simulator, blob reads on iOS,
/// blob query parameters, binding null in a `String[]`, or the existence of a database at all on
/// the native Windows and Linux ports. No application can depend on those.
///
/// # Platform notes
///
/// The UWP port is not maintained and is outside this contract.
package com.codename1.db;
