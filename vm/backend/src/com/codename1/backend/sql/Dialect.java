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
package com.codename1.backend.sql;

import java.io.IOException;

/**
 * What the three engines spell differently, in one place.
 *
 * A {@link com.codename1.backend.Database} already hides which engine answered a
 * query: rows come back as the same Java types whichever one produced them. What
 * it could not hide was the STATEMENT. PostgreSQL binds $1 where SQLite and MySQL
 * bind ?, it has no last-insert-id to ask for, and the three name their column
 * types and quote their identifiers differently -- so portable-looking code still
 * carried an "am I on PostgreSQL" branch at every call site. This repository's own
 * database check is the proof: it grew a placeholders(postgres, n) helper and a
 * separate INSERT ... RETURNING arm, and any application would grow the same.
 *
 * <p>A Dialect is that branch, written once. Statements are written in ONE
 * portable form -- ? for every parameter, plain unquoted names -- and the dialect
 * renders them for whichever engine the connection turns out to be.
 *
 * <pre>
 *   Dialect d = db.dialect();
 *   db.execute("INSERT INTO notes (title) VALUES (?)", new Object[] {title});
 * </pre>
 *
 * The rewrite happens inside Database, so the example above needs no dialect at
 * all; the type is public because schema generation -- the ORM's, or an
 * application's own migration code -- has to ask what this engine calls a 64-bit
 * integer.
 *
 * <p>Nothing here queries the server: a Dialect is chosen from the URL scheme
 * before a connection exists, and it holds no state, so the three instances are
 * shared constants.
 */
public abstract class Dialect {

    /**
     * The portable column kinds. These are the Java types an entity can hold,
     * not SQL types: what each engine calls them is the point of this class.
     *
     * <p>BOOLEAN and TIMESTAMP are deliberately stored as integers on every
     * engine -- 0/1, and epoch milliseconds. A native BOOLEAN column reads back
     * as a Long from all three anyway (PostgreSQL decodes bool to 0/1, MySQL
     * sends TINYINT), but a native timestamp does NOT: it arrives as text whose
     * format follows the server's DateStyle and time zone, which is a second
     * portability problem underneath the first one. Milliseconds in a BIGINT are
     * the same number everywhere and need no parsing.
     */
    public static final int TEXT = 0;
    /** A 32-bit integer. */
    public static final int INTEGER = 1;
    /** A 64-bit integer. */
    public static final int BIGINT = 2;
    /** A double-precision floating point number. */
    public static final int REAL = 3;
    /** Arbitrary bytes. */
    public static final int BLOB = 4;
    /** True or false, stored as 0 or 1. */
    public static final int BOOLEAN = 5;
    /** A moment in time, stored as epoch milliseconds. */
    public static final int TIMESTAMP = 6;

    public static final Dialect SQLITE = new SqliteDialect();
    public static final Dialect POSTGRES = new PostgresDialect();
    public static final Dialect MYSQL = new MySqlDialect();

    /**
     * MariaDB, which is the MySQL dialect with one difference: the collation it
     * can name for a text column.
     *
     * <p>Both need a collation that is case sensitive AND NO PAD, so "A" and
     * "a" are two keys and "token " keeps its space. Neither server has the
     * other's. Measured, creating a key column under each name:
     *
     * <pre>
     *                        mariadb 10.11   mariadb 11.8   mysql 8.0
     *   utf8mb4_0900_bin     unknown         ok             ok
     *   utf8mb4_nopad_bin    ok              ok             unknown
     *   utf8mb4_bin          collides        collides       collides
     * </pre>
     *
     * <p>So there is no single name, and utf8mb4_bin -- the one both have -- is
     * PAD SPACE on both. Which is chosen comes from the SERVER's handshake
     * banner rather than from the URL scheme, because a mysql:// URL points at
     * a MariaDB server perfectly often. {@link #getName} still answers "mysql":
     * this is the same wire protocol and the same SQL, and everything that
     * branches on the engine name means that family.
     */
    public static final Dialect MARIADB = new MariaDbDialect();

    Dialect() {
    }

    /**
     * The dialect for an engine name, or null when the name is not one of the
     * three. Accepts the spellings that appear in a URL scheme, ignoring case --
     * "postgres" and "postgresql" are the same engine. "mysql" and "mariadb" are
     * the same PROTOCOL but not the same dialect: they have no case-sensitive
     * NO PAD collation in common, so each name answers its own. A connection
     * picks between them by the server's handshake banner instead, which is the
     * better answer when there is a server to ask; this is for callers that
     * have only a name.
     */
    public static Dialect forName(String engine) {
        if(engine == null) {
            return null;
        }
        if(equalsIgnoreCaseAscii(engine, "sqlite")) {
            return SQLITE;
        }
        if(equalsIgnoreCaseAscii(engine, "postgres")
                || equalsIgnoreCaseAscii(engine, "postgresql")) {
            return POSTGRES;
        }
        if(equalsIgnoreCaseAscii(engine, "mariadb")) {
            // The MariaDB name answers the MariaDB dialect. They differ only in
            // the collation they can name, and MySQL's is unknown to MariaDB
            // 10.11 -- so a schema generator that asked by name and got MYSQL
            // emitted DDL that server refuses outright.
            return MARIADB;
        }
        if(equalsIgnoreCaseAscii(engine, "mysql")) {
            return MYSQL;
        }
        return null;
    }

    /** "sqlite", "postgresql" or "mysql". */
    public abstract String getName();

    /**
     * {@code identifier} quoted so that its case is preserved and a reserved word
     * is still usable as a name.
     *
     * <p>Quoting is not decoration here, it is what keeps a schema portable:
     * PostgreSQL folds an UNQUOTED name to lower case while SQLite and MySQL
     * preserve it, so a column written createdAt is createdat on one engine and
     * createdAt on the other two, and code that reads rows by name stops finding
     * it on exactly one of the three.
     *
     * <p>The quote character itself is doubled, which is the escape all three
     * accept. A name carrying a NUL is refused rather than escaped: no engine can
     * hold one, and SQLite reads statements as C strings, so passing it through
     * would truncate the statement instead of failing.
     */
    public abstract String quote(String identifier);

    /** What this engine calls one of the portable kinds above. */
    public abstract String columnType(int kind);

    /**
     * The full declaration of a primary key column whose value the DATABASE
     * assigns -- everything after the column name.
     *
     * <p>This is the least portable line in any schema: SQLite wants INTEGER
     * PRIMARY KEY AUTOINCREMENT and refuses the keyword on any other type,
     * MySQL wants AUTO_INCREMENT on the type it was given, and PostgreSQL has
     * neither and uses an identity column.
     */
    public String generatedKeyColumn(int kind, String quotedColumn) {
        return generatedKeyColumn(kind);
    }

    public abstract String generatedKeyColumn(int kind);

    /**
     * The declaration of a primary key column whose value the APPLICATION
     * assigns -- a UUID, a key issued by another service.
     *
     * <p>NOT NULL is not redundant, whatever the standard says. SQLite lets a
     * PRIMARY KEY column that is not INTEGER PRIMARY KEY hold null, and even
     * several nulls, where PostgreSQL and MySQL refuse: an insert that forgot
     * its key succeeded in development and failed in production, and the row it
     * wrote could not be found again by the generated id = ? predicate. The
     * other two already imply the constraint, so saying it costs nothing there.
     */
    public String assignedKeyColumn(int kind) {
        return columnType(kind) + " NOT NULL PRIMARY KEY";
    }

    /**
     * Whether a generated key comes back from the INSERT itself rather than from
     * a follow-up question.
     *
     * <p>True for PostgreSQL alone, and it is not a preference: PostgreSQL has no
     * last-insert-id concept, so INSERT ... RETURNING is the only way to learn the
     * key at all. {@link com.codename1.backend.Database#insert} is what reads this;
     * it exists so callers do not have to.
     */
    public boolean generatedKeysThroughReturning() {
        return false;
    }

    /**
     * An INSERT that supplies NO columns, for the entity whose only persisted
     * field is a key the database generates.
     *
     * <p>Another line the three spell differently, and the naive construction is
     * not merely ugly but invalid: "INSERT INTO t () VALUES ()" is what an empty
     * column list builds, and SQLite and PostgreSQL both refuse it. They want
     * DEFAULT VALUES; MySQL wants the empty lists and has no DEFAULT VALUES form
     * at all.
     */
    public String insertDefaults(String quotedTable) {
        return "INSERT INTO " + quotedTable + " DEFAULT VALUES";
    }

    /**
     * {@code count} rows starting at {@code offset}, or an empty string when both
     * are unbounded. All three accept LIMIT n OFFSET m; MySQL needs a limit before
     * it will accept an offset at all, so an offset-only request is given the
     * largest limit its parser takes.
     */
    public String limit(int count, int offset) {
        if(count < 0 && offset <= 0) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        out.append(" LIMIT ").append(count < 0 ? unboundedLimit() : String.valueOf(count));
        if(offset > 0) {
            out.append(" OFFSET ").append(offset);
        }
        return out.toString();
    }

    /** The limit that means "all of them", for the offset-without-limit case. */
    String unboundedLimit() {
        return "-1";
    }

    /**
     * {@code sql} written in the portable form, rendered for this engine.
     *
     * <p>The portable form is the one SQLite and MySQL already use: ? for each
     * parameter, in order. PostgreSQL is the engine that differs, and this is
     * where that difference stops -- it becomes $1, $2 and so on here rather than
     * at every call site. A literal question mark that is NOT a parameter is
     * written ?? -- which matters only on PostgreSQL, whose jsonb operators are
     * spelled ?, ?| and ?&amp; -- and collapses to a single ? on every engine, so
     * the escape means the same thing everywhere.
     *
     * <p>A statement with no ? in it is passed through untouched and unchecked.
     * That is what keeps engine-native SQL working: code that already writes $1
     * for PostgreSQL, or that calls a function whose name contains no placeholder
     * at all, is not this method's business. Once a statement DOES carry a
     * placeholder it is in the portable form, and then the count has to match
     * {@code paramCount} -- a mismatch is refused here, naming both numbers,
     * rather than reaching an engine that answers for it in three different ways
     * (SQLite binds the missing ones to NULL and commits the row).
     *
     * <p>Placeholders are recognised only where a parameter can appear. String
     * literals, quoted identifiers, dollar-quoted bodies and comments are scanned
     * through, so a ? inside any of them stays what it was.
     */
    public String bind(String sql, int paramCount) throws IOException {
        if(sql == null) {
            throw new IOException("No statement");
        }
        return Placeholders.render(sql, paramCount, dollarPlaceholders(), nestedBlockComments(),
                backslashEscapesInLiterals(), hashLineComments(), dollarQuotedStrings(),
                dashCommentNeedsSpace(), bracketIdentifiers(),
                executableComments());
    }

    /**
     * How many rows an INSERT's VALUES clause writes, or -1 when the shape does
     * not say -- an INSERT ... SELECT, or a statement with no VALUES at all.
     *
     * <p>{@link com.codename1.backend.Database#insert} answers with one key, and
     * a multi-row insert has no single key that means the same thing on three
     * engines. Counting the tuples is what lets it refuse BEFORE the rows are
     * written rather than after.
     *
     * <p>{@link #VERSION_GATED} is the third answer: a MySQL statement whose
     * tuples sit behind a version-gated executable comment inserts a number of
     * rows that depends on the server, and this has no connection to ask.
     */
    /** countInsertRows: the row count depends on the server version. */
    public static final int VERSION_GATED = Placeholders.VERSION_GATED;

    public int countInsertRows(String sql) throws IOException {
        if(sql == null) {
            throw new IOException("No statement");
        }
        return Placeholders.countInsertRows(sql, nestedBlockComments(),
                backslashEscapesInLiterals(), hashLineComments(), dollarQuotedStrings(),
                dashCommentNeedsSpace(), bracketIdentifiers(),
                executableComments());
    }

    /**
     * The operator a portable {@code like} renders to, and the pattern it binds.
     *
     * <p>SQLite's LIKE folds ASCII case and PostgreSQL's does not, so the same
     * query answered different rows depending on the engine. The obvious fix --
     * PRAGMA case_sensitive_like -- is connection wide, and that reaches further
     * than the ORM: measured, a table declared CHECK(v LIKE 'A%') accepts 'abc'
     * under the default and REFUSES it once the pragma is on, so merely opening
     * an existing database would change what writes it accepts, and an
     * expression index built on LIKE would no longer agree with its own rows.
     *
     * <p>So SQLite renders GLOB instead, which is case sensitive, always
     * present, and scoped to the one comparison being made. The pattern is
     * translated to match: see {@link #likePattern}.
     */
    public String likeOperator() {
        // ESCAPE '' DISABLES THE ESCAPE. PostgreSQL and MySQL give LIKE a
        // default escape of backslash and SQLite gives it none, so
        // like("name", "100\\%") looked for the literal "100%" on two engines
        // and for a "100\\" prefix on the third. Measured, matching 1, 1 and 0
        // rows. An empty ESCAPE makes both of them agree with SQLite, where a
        // backslash is an ordinary character -- and with the GLOB rendering
        // below, whose translation leaves a backslash alone for the same reason.
        //
        // SQLite cannot say ESCAPE '' at all ("ESCAPE expression must be a
        // single character"), which is another reason the SQLite branch renders
        // something else entirely rather than sharing this string.
        //
        // AND THE MySQL FAMILY CAN, which review has now asked about twice: the
        // suggestion is that those engines require exactly one character here
        // and reject the statement, so every like() would fail on them. They do
        // not. MySQL's own manual says the escape expression "must evaluate to a
        // string that is empty or one character long", and measured by running
        // SELECT 1 WHERE 'abc' LIKE ? ESCAPE '' with 'a%' bound:
        //
        //   MySQL 8.0.46        accepted, 1 row
        //   MariaDB 11.8.9      accepted, 1 row
        //   MariaDB 10.11.19    accepted, 1 row
        //   MariaDB 10.4.34     accepted, 1 row
        //   PostgreSQL 16.15    accepted, 1 row
        //
        // OrmCheck holds this without needing a case of its own: its five like()
        // checks are not gated by dialect, so they run on all four MySQL-family
        // arms of the matrix, and a rejected ESCAPE clause would fail every one
        // of them rather than go unnoticed.
        return " LIKE ? ESCAPE ''";
    }

    /**
     * {@code pattern} as {@link #likeOperator} expects it. Unchanged except on
     * SQLite, where GLOB spells its wildcards differently.
     */
    public String likePattern(String pattern) {
        return pattern;
    }

    /**
     * One ORDER BY term, with NULLs placed the same way on every engine.
     *
     * <p>The engines disagree by default, measured over one null and two values:
     *
     * <pre>
     *                   ASC       DESC
     *   sqlite          null,a,b  b,a,null
     *   postgresql      a,b,null  null,b,a
     *   mysql           null,a,b  b,a,null
     * </pre>
     *
     * <p>So {@code orderBy("name", true).first()} answered the null row on two
     * engines and a real one on the third. NULL sorts LOWEST here -- first
     * ascending, last descending -- which is what SQLite and MySQL already do,
     * and PostgreSQL is brought to match rather than the other way round
     * because two of three and the usual reading of NULL agree on it.
     *
     * <p>MySQL cannot say NULLS FIRST at all: it answers a syntax error, so the
     * order is expressed there as a leading {@code (col IS NULL)} term, which is
     * the documented way to get the same effect.
     */
    public String orderBy(String quotedColumn, boolean ascending) {
        return orderBy(quotedColumn, ascending, false);
    }

    /**
     * {@link #orderBy(String, boolean)} for a column whose kind is known.
     *
     * <p>A TEXT column needs its collation pinned as well as its nulls placed.
     * Measured over "Z" and "a": SQLite orders them Z then a, because its
     * default collation is byte order, while a PostgreSQL database initialised
     * with a locale-aware collation orders a then Z -- so
     * {@code orderBy("name", true).first()} answered a different entity
     * depending on the database. Byte order is the one all three can agree on:
     * SQLite is already there, MySQL's text columns carry a binary collation
     * for the case-sensitivity reason, and PostgreSQL says it as COLLATE "C".
     *
     * <p>The kind has to be passed because COLLATE is only valid on text. An
     * integer column with one is a type error, not a no-op.
     */
    public String orderBy(String quotedColumn, boolean ascending, boolean text) {
        return quotedColumn + (ascending ? " ASC NULLS FIRST" : " DESC NULLS LAST");
    }

    /**
     * The left side of an ORDERING comparison -- &gt;, &gt;=, &lt;, &lt;= -- on a
     * column of this kind.
     *
     * <p>The same reason {@link #orderBy(String, boolean, boolean)} exists, for
     * the same columns, and leaving the two inconsistent is worse than leaving
     * both alone: ordering was pinned to byte order while the comparisons that
     * decide which rows come back were not, so a query could exclude a row that
     * its own ORDER BY would have placed first.
     *
     * <p>MEASURED on a database created the way the standard image creates one,
     * with lc_collate en_US.utf8, against rows holding 'Z' and 'a':
     *
     * <pre>
     * WHERE v &gt; 'Z'    SQLite: a    MySQL: a    PostgreSQL: (none)
     * </pre>
     *
     * <p>SQLite compares BINARY and the generated MySQL column carries a binary
     * collation, so both answer in byte order; PostgreSQL follows the database's
     * locale, where 'a' sorts before 'Z'. One entity, one query, two answers.
     *
     * <p>EQUALITY is deliberately NOT routed through here. PostgreSQL requires a
     * deterministic collation by default, and under one, equality is byte
     * equality whatever the sort order is -- so eq, ne and in already agree
     * across the three, and collating them would be noise in every statement
     * that uses them.
     */
    public String comparison(String quotedColumn, boolean text) {
        return quotedColumn;
    }

    /**
     * A statement that puts a generated key back in step with the rows that are
     * there, or null when the engine needs none.
     *
     * <p>SUPPLYING YOUR OWN KEY IS A DOCUMENTED CAPABILITY -- it is why
     * PostgreSQL's column is GENERATED BY DEFAULT rather than ALWAYS, so a data
     * import or a test fixture can insert the value it already has. On SQLite and
     * MySQL doing that also moves the counter, so the next generated insert
     * carries on above it. On PostgreSQL it does NOT: the identity's sequence is
     * untouched by an explicit value, and the next generated insert reuses a key
     * that is already there.
     *
     * <p>MEASURED on a fresh table, explicit id 1 then a generated insert:
     *
     * <pre>
     * SQLite: 2   MySQL: 2   PostgreSQL: duplicate key value violates unique
     *                                    constraint "idsync_pkey"
     * </pre>
     *
     * <p>So the capability is portable only if the caller can put the sequence
     * back, and this is what lets the ORM offer that in one call rather than
     * leaving every importer to write engine-specific SQL it has no reason to
     * know about.
     */
    /**
     * A SQL string literal holding {@code value}, in PostgreSQL's E'' form --
     * which means the same thing WHATEVER standard_conforming_strings is set to.
     *
     * <p>It exists rather than each caller writing quotes around a name because
     * a table called owner's_items is a perfectly ordinary identifier, and
     * pasting it between apostrophes ends the literal early and leaves the rest
     * of the statement as syntax. Doubling the apostrophe answers that.
     *
     * <p>Doubling the apostrophe is NOT the whole of the escape, which is the
     * part that took a second look. It is right only while
     * standard_conforming_strings is on. With it off --
     * and a session can turn it off -- a backslash in a plain literal starts an
     * escape, so an @Entity(table) or column name containing one, which a quoted
     * identifier creates perfectly happily, decodes to different text inside the
     * literal: a name holding the two characters backslash and n reaches
     * pg_get_serial_sequence as a newline, it looks for a relation nobody has,
     * and the resync fails on a table that exists.
     *
     * <p>E'' always interprets escapes, so doubling the backslashes here is
     * correct under both settings rather than under one of them. Deliberately
     * NOT on the base class: E'' is a syntax error on MySQL, where the E would
     * be read as an identifier.
     */
    static String postgresLiteral(String value) {
        StringBuilder out = new StringBuilder(value.length() + 3);
        out.append("E'");
        for(int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            if(c == '\'' || c == '\\') {
                out.append(c);
            }
            out.append(c);
        }
        return out.append('\'').toString();
    }

    public String resyncGeneratedKey(String table, String column) {
        return null;
    }

    /**
     * The statement that holds inserts off {@code table} while
     * {@link #resyncGeneratedKey} runs, or null where none is needed.
     *
     * <p>Null here, and that is not an oversight for the two engines that answer
     * it. Neither SQLite nor MySQL needs a resync at all -- both advance their
     * counter past an explicitly assigned key on their own -- so
     * resyncGeneratedKey is null for them and this is never reached.
     *
     * <p>Must run in the SAME TRANSACTION as the resync, which is the only thing
     * that makes it a lock rather than a gesture: a lock taken by a statement of
     * its own is released the moment that statement ends.
     */
    public String lockForResync(String table) {
        return null;
    }

    /**
     * Whether anything follows the first statement in {@code sql}.
     *
     * <p>What {@link com.codename1.backend.Database} refuses before dispatching,
     * because SQLite would run the first statement and drop the rest in silence.
     * See {@link Placeholders#hasTrailingStatement}.
     */
    public boolean hasTrailingStatement(String sql) throws IOException {
        if(sql == null) {
            throw new IOException("No statement");
        }
        return Placeholders.hasTrailingStatement(sql, nestedBlockComments(),
                backslashEscapesInLiterals(), hashLineComments(), dollarQuotedStrings(),
                dashCommentNeedsSpace(), bracketIdentifiers(),
                executableComments());
    }

    /**
     * Whether {@code sql} updates an existing row when it conflicts.
     *
     * <p>What {@link com.codename1.backend.Database#insert} needs before it
     * trusts a key that came from connection state. See
     * {@link Placeholders#updatesOnConflict}.
     */
    public boolean updatesOnConflict(String sql) throws IOException {
        if(sql == null) {
            throw new IOException("No statement");
        }
        return Placeholders.updatesOnConflict(sql, nestedBlockComments(),
                backslashEscapesInLiterals(), hashLineComments(), dollarQuotedStrings(),
                dashCommentNeedsSpace(), bracketIdentifiers(),
                executableComments());
    }

    /**
     * Where {@code sql} stops being the statement and starts being its
     * terminator, its trailing comment or trailing space.
     *
     * <p>What {@link com.codename1.backend.Database#insert} needs to put
     * RETURNING somewhere it will run. See {@link Placeholders#endOfStatement}.
     */
    public int endOfStatement(String sql) throws IOException {
        if(sql == null) {
            throw new IOException("No statement");
        }
        return Placeholders.endOfStatement(sql, nestedBlockComments(),
                backslashEscapesInLiterals(), hashLineComments(), dollarQuotedStrings(),
                dashCommentNeedsSpace(), bracketIdentifiers(),
                executableComments());
    }

    /** Whether this engine's parameters are written $1, $2 rather than ?. */
    boolean dollarPlaceholders() {
        return false;
    }

    /**
     * Whether a backslash escapes the next character inside an ordinary string
     * literal.
     *
     * <p>True for MySQL, whose default SQL mode works that way. False for SQLite,
     * where a backslash is an ordinary character, and for PostgreSQL, where it is
     * one unless the literal carries an E prefix.
     *
     * <p>A MySQL server started with NO_BACKSLASH_ESCAPES is the case this reads
     * the wrong way, and it is the right way round to be wrong: that mode is
     * off by default, and the statement it mis-scans has to end a literal with a
     * backslash.
     */
    boolean backslashEscapesInLiterals() {
        return false;
    }

    /** Whether # begins a line comment. MySQL alone. */
    boolean hashLineComments() {
        return false;
    }

    /**
     * Whether $tag$...$tag$ is a string literal. PostgreSQL alone.
     *
     * <p>Scanning for one everywhere refused valid MySQL: $ is legal inside an
     * unquoted identifier there, so a column pair like total$usd$ opened a
     * dollar-quoted body that never closed and the statement was rejected before
     * the server saw it.
     */
    boolean dollarQuotedStrings() {
        return false;
    }

    /**
     * Whether a double dash begins a comment only when whitespace follows it.
     *
     * <p>MySQL alone. There, "5--1" is five minus minus-one and only "-- " opens
     * a comment; SQLite and PostgreSQL take the two dashes whatever follows. Read
     * MySQL the other way, "VALUES (5--1), (2)" looked like one row with a
     * comment after it instead of the two rows it is -- which is exactly what the
     * multi-row insert check was reading.
     */
    boolean dashCommentNeedsSpace() {
        return false;
    }

    /**
     * Whether [brackets] quote an identifier.
     *
     * <p>SQLite alone, and deliberately not PostgreSQL: brackets are an ARRAY
     * SUBSCRIPT there, so treating a[1] as a quoted name would swallow the rest
     * of the statement.
     */
    boolean bracketIdentifiers() {
        return false;
    }

    /**
     * Whether a block comment opening "/*!" is SQL that RUNS rather than text
     * that is ignored.
     *
     * <p>MySQL alone. Its version-gated executable comments are how a statement
     * says "run this on MySQL and nowhere else", so what is inside one has to be
     * scanned as the statement it is.
     */
    int executableComments() {
        return Placeholders.NO_EXECUTABLE_COMMENTS;
    }

    /**
     * Whether block comments nest. PostgreSQL's do -- it is the one engine where
     * a comment opened inside another comment has to be closed before the outer
     * one ends.
     */
    boolean nestedBlockComments() {
        return false;
    }

    public String toString() {
        return getName();
    }

    /**
     * ASCII case-insensitive comparison. String.equalsIgnoreCase is already
     * locale independent, so this exists only for the length check that keeps
     * it from allocating on a mismatch.
     */
    private static boolean equalsIgnoreCaseAscii(String value, String constant) {
        return value.length() == constant.length() && value.equalsIgnoreCase(constant);
    }

    /** The shared identifier quoting, parameterised on the quote character. */
    static String quoteWith(String identifier, char quote) {
        if(identifier == null || identifier.length() == 0) {
            throw new IllegalArgumentException("An SQL identifier cannot be empty");
        }
        StringBuilder out = new StringBuilder(identifier.length() + 4);
        out.append(quote);
        for(int iter = 0 ; iter < identifier.length() ; iter++) {
            char c = identifier.charAt(iter);
            if(c == 0) {
                throw new IllegalArgumentException("An SQL identifier cannot hold a NUL: '"
                        + identifier + "'");
            }
            if(c == quote) {
                out.append(quote);
            }
            out.append(c);
        }
        out.append(quote);
        return out.toString();
    }

    // ------------------------------------------------------------------
    // The three engines
    // ------------------------------------------------------------------

    private static final class SqliteDialect extends Dialect {
        public String likeOperator() {
            return " GLOB ?";
        }

        /**
         * A LIKE pattern as GLOB spells it; see {@link Dialect#likeOperator}.
         *
         * <p>GLOB's wildcards are * and ?, and a literal one of those -- or of
         * the [ that opens its character classes -- is written by bracketing it.
         * LIKE has no escape character unless ESCAPE names one, which this
         * builder does not offer, so every other character is literal and
         * passes through.
         */
        public String likePattern(String pattern) {
            if(pattern == null) {
                return null;
            }
            StringBuilder out = new StringBuilder(pattern.length() + 8);
            for(int iter = 0 ; iter < pattern.length() ; iter++) {
                char c = pattern.charAt(iter);
                if(c == '%') {
                    out.append('*');
                } else if(c == '_') {
                    out.append('?');
                } else if(c == '*' || c == '?' || c == '[') {
                    out.append('[').append(c).append(']');
                } else {
                    out.append(c);
                }
            }
            return out.toString();
        }


        public String getName() {
            return "sqlite";
        }

        public String quote(String identifier) {
            return quoteWith(identifier, '"');
        }

        public String columnType(int kind) {
            switch(kind) {
                case INTEGER:
                case BIGINT:
                case BOOLEAN:
                case TIMESTAMP:
                    return "INTEGER";
                case REAL:
                    return "REAL";
                case BLOB:
                    return "BLOB";
                default:
                    return "TEXT";
            }
        }

        /**
         * AUTOINCREMENT is legal after INTEGER PRIMARY KEY and nothing else, which
         * is why the kind is ignored rather than mapped: an int id and a long id
         * are the same column here, and a String one cannot be generated at all.
         */
        public String generatedKeyColumn(int kind) {
            return generatedKeyColumn(kind, null);
        }

        /**
         * A generated key declared as {@code int} is BOUNDED to the Java int range
         * here, which the other two get from their column type for nothing.
         *
         * <p>PostgreSQL declares INTEGER and MySQL declares INT for that kind, so
         * both refuse a key past 2147483647 at the database. SQLite has one
         * generated-key form -- AUTOINCREMENT is legal only after the exact words
         * INTEGER PRIMARY KEY -- and that column is 64 bit whatever the entity
         * said. So SQLite alone would hand back a key that does not fit the field
         * it has to be written into: the row COMMITS, and then Values.asInt throws
         * while narrowing it. The caller sees a failed insert over a row that
         * exists and an entity with no usable id, and retrying duplicates it.
         *
         * <p>A CHECK is the only way to say it here, since the type cannot change.
         * It costs nothing until the counter actually reaches the bound, and at
         * that point it turns a committed row with an unusable key into a refused
         * insert -- which is what the other two already do.
         */
        public String generatedKeyColumn(int kind, String quotedColumn) {
            if(kind == INTEGER && quotedColumn != null) {
                return "INTEGER PRIMARY KEY AUTOINCREMENT CHECK(" + quotedColumn
                        + " BETWEEN -2147483648 AND 2147483647)";
            }
            return "INTEGER PRIMARY KEY AUTOINCREMENT";
        }

        boolean bracketIdentifiers() {
            return true;
        }
    }

    private static final class PostgresDialect extends Dialect {
        public String comparison(String quotedColumn, boolean text) {
            return text ? quotedColumn + " COLLATE \"C\"" : quotedColumn;
        }

        public String orderBy(String quotedColumn, boolean ascending, boolean text) {
            // COLLATE "C" is byte order, which is what the other two already
            // give. Without it a locale-aware database ordered "a" before "Z"
            // and the other two ordered "Z" first.
            return (text ? quotedColumn + " COLLATE \"C\"" : quotedColumn)
                    + (ascending ? " ASC NULLS FIRST" : " DESC NULLS LAST");
        }

        public String getName() {
            return "postgresql";
        }

        public String quote(String identifier) {
            return quoteWith(identifier, '"');
        }

        public String columnType(int kind) {
            switch(kind) {
                case INTEGER:
                    return "INTEGER";
                case BIGINT:
                case TIMESTAMP:
                    return "BIGINT";
                case BOOLEAN:
                    // SMALLINT rather than BOOLEAN, so that every engine stores
                    // 0/1 and every read decodes the same way. See the note on
                    // the constants above.
                    return "SMALLINT";
                case REAL:
                    return "DOUBLE PRECISION";
                case BLOB:
                    return "BYTEA";
                default:
                    return "TEXT";
            }
        }

        /**
         * An identity column, which is the standard spelling and has been
         * available since PostgreSQL 10. BY DEFAULT rather than ALWAYS: a caller
         * that supplies its own key -- a data import, a test fixture -- is then
         * inserting a value the column accepts rather than one it refuses.
         */
        public String generatedKeyColumn(int kind) {
            return (kind == INTEGER ? "INTEGER" : "BIGINT")
                    + " GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY";
        }

        /**
         * pg_get_serial_sequence finds the identity's sequence from the table and
         * column rather than from a name this would otherwise have to guess --
         * the sequence is named after them, but the rule has exceptions and a
         * truncated name is one of them.
         *
         * <p>The third argument of setval is false so the NEXT value is the one
         * given: coalesce(max, 0) + 1 on an empty table is 1, which is where a
         * fresh identity starts. Passing the max with the default true would skip
         * a key on an empty table, which is harmless but wrong.
         */
        public String resyncGeneratedKey(String table, String column) {
            // RAW NAMES IN, and each one spelled for the position it lands in.
            // This took the QUOTED forms and dropped them into a SQL string
            // literal, which breaks on two ordinary names:
            //
            //   @Entity(table = "owner's_items") creates fine, because the
            //   identifier is double quoted -- and then closed the literal here,
            //   making the statement invalid;
            //
            //   a column with a double quote in it arrives already escaped as
            //   "" inside the quoted form, and trimming the outer quotes left
            //   the doubling behind, so pg_get_serial_sequence looked for a
            //   column nobody has.
            //
            // pg_get_serial_sequence takes TEXT, and its first argument is parsed
            // as an identifier -- so the table goes in as its quoted form, which
            // preserves case, and the column goes in raw, because that argument
            // is matched against the column name as given. Both are escaped as
            // string literals by doubling the apostrophes, which is the only
            // escape a PostgreSQL literal needs. The identifier positions below
            // keep the quoted forms.
            //
            // CLAMPED TO 1, because an import can carry negative keys and
            // PostgreSQL accepts them: max(id) + 1 is then zero or negative, and
            // setval refuses that outright because the identity's sequence starts
            // at 1. SQLite and MySQL are left ready to generate a positive key
            // after such an import, so without the clamp the resync turns a
            // portable capability into one that throws on one engine only -- the
            // exact shape it was added to remove.
            String quotedTable = quote(table);
            String quotedColumn = quote(column);
            // READ AND WRITE UNDER THE LOCK lockForResync takes, because the two
            // halves are not atomic on their own: max() is a snapshot, and a
            // concurrent insert can take the very value this then installs as the
            // sequence's next one, so the following generated insert fails on a
            // duplicate key. EXCLUSIVE mode is what excludes that insert.
            return "SELECT setval(pg_get_serial_sequence(" + postgresLiteral(quotedTable)
                    + ", " + postgresLiteral(column) + "), "
                    + "greatest(coalesce((SELECT max(" + quotedColumn + ") FROM "
                    + quotedTable + "), 0) + 1, 1), false)";
        }

        /**
         * EXCLUSIVE, which is the weakest mode that excludes an insert: it
         * conflicts with ROW EXCLUSIVE, which every INSERT, UPDATE and DELETE
         * takes, and does not conflict with ACCESS SHARE, so ordinary reads of
         * the table continue while this runs.
         *
         * <p>ACCESS EXCLUSIVE would also work and is worse -- it would stop
         * readers as well, for a maintenance statement that only needs writers
         * held still.
         */
        public String lockForResync(String table) {
            return "LOCK TABLE " + quote(table) + " IN EXCLUSIVE MODE";
        }

        public boolean generatedKeysThroughReturning() {
            return true;
        }

        boolean dollarPlaceholders() {
            return true;
        }

        boolean nestedBlockComments() {
            return true;
        }

        boolean dollarQuotedStrings() {
            return true;
        }

        /** PostgreSQL spells an unbounded limit LIMIT ALL. */
        String unboundedLimit() {
            return "ALL";
        }
    }

    /** See {@link #MARIADB}. */
    private static final class MariaDbDialect extends MySqlDialect {
        String textCollation() {
            return "utf8mb4_nopad_bin";
        }

        int executableComments() {
            // "/*M! ... */" as well as "/*! ... */". Only MariaDB runs the
            // former: measured, "VALUES (1) /*M! , (2) */" inserts one row on
            // SQLite, PostgreSQL and MySQL and TWO here, so a scanner blind to
            // it counted one tuple for a statement writing two. Reading it as
            // executable on MySQL would be the mirror mistake.
            return Placeholders.MARIADB_EXECUTABLE_COMMENTS;
        }
    }

    private static class MySqlDialect extends Dialect {
        /**
         * The case-sensitive NO PAD collation THIS server has; see
         * {@link Dialect#MARIADB} for why the two differ.
         */
        String textCollation() {
            return "utf8mb4_0900_bin";
        }

        public String getName() {
            return "mysql";
        }

        public String quote(String identifier) {
            return quoteWith(identifier, '`');
        }

        public String columnType(int kind) {
            switch(kind) {
                case INTEGER:
                    return "INT";
                case BIGINT:
                case TIMESTAMP:
                    return "BIGINT";
                case BOOLEAN:
                    return "TINYINT";
                case REAL:
                    return "DOUBLE";
                case BLOB:
                    return "LONGBLOB";
                default:
                    // LONGTEXT, for the reason the LONGBLOB above it already
                    // takes: MySQL's TEXT holds 65,535 BYTES, while SQLite and
                    // PostgreSQL leave a string column unbounded. A document
                    // body past that limit stored fine on two engines and was
                    // rejected by a strict MySQL or silently TRUNCATED by a
                    // permissive one -- and nothing in a generated write could
                    // have caught it, because the limit is the column's, not the
                    // field's. The blob branch got this right and the string one
                    // did not, in the same switch.
                    //
                    // A KEY is unaffected: assignedKeyColumn overrides this with
                    // VARCHAR(255), because MySQL cannot index an unbounded
                    // column at all. See Table.MAX_ASSIGNED_TEXT_KEY.
                    //
                    // The same BINARY COLLATION the key column pins, and for the
                    // same reason: MySQL's default is case and accent
                    // insensitive, so with the server's default collation
                    // dao.query().eq("name", "A") matched a stored "a" here and
                    // matched nothing on the other two. Measured -- eq returned
                    // 2 rows on MySQL against 1 on SQLite and PostgreSQL. eq and
                    // in() are the operations an application builds its
                    // behaviour on, so they have to mean one thing.
                    // The NO PAD collation here too, so a trailing space is
                    // part of an ordinary value exactly as it is part of a key.
                    return "LONGTEXT CHARACTER SET utf8mb4 COLLATE " + textCollation();
            }
        }

        public String generatedKeyColumn(int kind) {
            return (kind == INTEGER ? "INT" : "BIGINT") + " NOT NULL AUTO_INCREMENT PRIMARY KEY";
        }

        /**
         * True, which is this server's default and NOT its only setting: a
         * session with NO_BACKSLASH_ESCAPES in its sql_mode treats a backslash
         * as an ordinary character, and the scanner then disagrees with the
         * server about where a literal ends.
         *
         * <p>The session is deliberately NOT pinned, because the disagreement
         * FAILS CLOSED. A backslash immediately before a quote is the only way
         * the two readings diverge: this scanner swallows that quote, which
         * flips the quote parity for the whole rest of the statement, and every
         * further tuple contributes an even number of quotes -- so the parity
         * stays odd and the scan ends inside an unterminated literal, which
         * countInsertRows refuses outright. Measured against a MySQL session
         * holding sql_mode='NO_BACKSLASH_ESCAPES', six crafted statements
         * including VALUES ('x\'), ('y') and the three- and four-tuple
         * versions of it: all six refused, ROWS WRITTEN 0 in every case. The
         * one that did restore the parity was then refused by the server itself
         * with a 1064. Nothing was committed and no key was answered, so the
         * single-row contract of Database#insert holds in that mode too.
         *
         * <p>The refusal of an unterminated literal is what does that work, and
         * it is load bearing rather than incidental: with that one throw removed
         * and nothing else changed, the same six statements go to 3 accepted and
         * 8 ROWS WRITTEN. So the protection is real and this is where it lives.
         *
         * <p>What the mode does cost is a false REFUSAL -- an application that
         * has turned it on can write VALUES ('C:\') as one legal row and be
         * told the statement ends inside a literal. That is the residual, and
         * it is preferred to the two alternatives. Pinning sql_mode on connect
         * would silently reinterpret the application's OWN literals, which is a
         * data change rather than a refusal; and reading the active mode per
         * connection cannot reach this method, because the dialects are shared
         * singletons with no connection to ask. OrmCheck.aBackslashModeStillFailsClosed
         * is what holds the measured behaviour in place.
         */
        boolean backslashEscapesInLiterals() {
            return true;
        }

        boolean hashLineComments() {
            return true;
        }

        boolean dashCommentNeedsSpace() {
            return true;
        }

        int executableComments() {
            return Placeholders.MYSQL_EXECUTABLE_COMMENTS;
        }

        /** MySQL has no DEFAULT VALUES; the empty lists are its spelling. */
        public String insertDefaults(String quotedTable) {
            return "INSERT INTO " + quotedTable + " () VALUES ()";
        }

        /**
         * VARCHAR rather than the TEXT this engine uses for an ordinary string
         * column: MySQL indexes a TEXT column only by a prefix whose length it
         * has to be given, so TEXT PRIMARY KEY is refused outright.
         *
         * 255 is the conventional bound and it fits InnoDB's modern 3072-byte
         * index limit with utf8mb4 to spare (255 * 4 = 1020). An earlier version
         * of this comment justified it by the 767-byte limit of much older
         * servers, which is arithmetic that does not work -- utf8mb4 would allow
         * only 191 characters under that one. The number is right; the reason
         * given for it was not.
         *
         * The other two engines leave a string key unbounded, so 255 is also the
         * portable bound and Table refuses a longer one on every engine rather
         * than letting the same entity work on two and fail on the third. See
         * Table.MAX_ASSIGNED_TEXT_KEY.
         */
        public String orderBy(String quotedColumn, boolean ascending, boolean text) {
            // NULLS FIRST is a syntax error here, so the same placement is spelled
            // with the boolean term MySQL's own documentation gives for it:
            // (col IS NULL) is 1 for a null and 0 otherwise, so ordering that
            // DESC puts the nulls first and ASC puts them last.
            //
            // The THREE-argument form is the one to override. Overriding only
            // the two-argument one left Query calling the base version, which
            // emitted NULLS FIRST and was refused outright -- text needs no
            // COLLATE here, because these columns already carry a binary one.
            return "(" + quotedColumn + " IS NULL) " + (ascending ? "DESC" : "ASC")
                    + ", " + quotedColumn + (ascending ? " ASC" : " DESC");
        }

        public String assignedKeyColumn(int kind) {
            if(kind == TEXT) {
                // BINARY COLLATION, because MySQL's default one is case AND
                // accent insensitive: with it, "A" and "a" are the same primary
                // key. Measured against a live server -- inserting both gives
                // "Duplicate entry 'a' for key 'PRIMARY'" here, while SQLite and
                // PostgreSQL store two rows. An application that issued keys as
                // mixed-case tokens lost rows on one engine only, and the loss
                // is a duplicate-key error at best and a silent overwrite of
                // meaning at worst.
                //
                // The charset is pinned with it: COLLATE alone conflicts when
                // the table or database charset is not utf8mb4, and the other
                // two engines are UTF-8 throughout, so naming it is what makes
                // the three agree rather than depending on server defaults.
                // utf8mb4_0900_bin rather than utf8mb4_bin, because the older
                // binary collation is still PAD SPACE: measured, "token" and
                // "token " are the same primary key under utf8mb4_bin and the
                // second insert answers "Duplicate entry 'token '", while
                // SQLite and PostgreSQL keep them apart. The _0900_ collations
                // are NO PAD and keep the trailing space, which is what the
                // other two do.
                //
                // This is the one place the backend needs MySQL 8.0: the _0900_
                // collations arrived with it. 5.7 has been out of support since
                // October 2023, and the alternative -- VARBINARY -- would stop
                // the column being text at all and change what every read of it
                // returns.
                return "VARCHAR(255) CHARACTER SET utf8mb4 COLLATE " + textCollation()
                        + " NOT NULL PRIMARY KEY";
            }
            return columnType(kind) + " NOT NULL PRIMARY KEY";
        }

        /**
         * MySQL's parser requires a limit before it accepts an offset, and this
         * is the value its own documentation gives for "all rows from here on".
         */
        String unboundedLimit() {
            return "18446744073709551615";
        }
    }

    /**
     * The name, if it is a plain SQL identifier: an ASCII letter or underscore,
     * then letters, digits and underscores. For the few statements that cannot
     * take a parameter where a name goes -- a savepoint -- so a name reaching the
     * text of a statement can never carry anything else.
     */
    public static String checkIdentifier(String name) throws IOException {
        if(name == null || name.length() == 0 || name.length() > 63) {
            throw new IOException("Not an identifier: " + name);
        }
        for(int iter = 0 ; iter < name.length() ; iter++) {
            char c = name.charAt(iter);
            boolean letter = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
            if(!letter && (iter == 0 || c < '0' || c > '9')) {
                throw new IOException("Not an identifier: " + name);
            }
        }
        return name;
    }
}
