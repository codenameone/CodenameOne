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

    Dialect() {
    }

    /**
     * The dialect for an engine name, or null when the name is not one of the
     * three. Accepts the spellings that appear in a URL scheme, ignoring case --
     * "postgres" and "postgresql" are the same engine, and so are "mysql" and
     * "mariadb".
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
        if(equalsIgnoreCaseAscii(engine, "mysql") || equalsIgnoreCaseAscii(engine, "mariadb")) {
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
    public abstract String generatedKeyColumn(int kind);

    /**
     * The declaration of a primary key column whose value the APPLICATION
     * assigns -- a UUID, a key issued by another service.
     */
    public String assignedKeyColumn(int kind) {
        return columnType(kind) + " PRIMARY KEY";
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
        return Placeholders.render(sql, paramCount, dollarPlaceholders(), nestedBlockComments());
    }

    /** Whether this engine's parameters are written $1, $2 rather than ?. */
    boolean dollarPlaceholders() {
        return false;
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
            return "INTEGER PRIMARY KEY AUTOINCREMENT";
        }
    }

    private static final class PostgresDialect extends Dialect {
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

        public boolean generatedKeysThroughReturning() {
            return true;
        }

        boolean dollarPlaceholders() {
            return true;
        }

        boolean nestedBlockComments() {
            return true;
        }

        /** PostgreSQL spells an unbounded limit LIMIT ALL. */
        String unboundedLimit() {
            return "ALL";
        }
    }

    private static final class MySqlDialect extends Dialect {
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
                    return "TEXT";
            }
        }

        public String generatedKeyColumn(int kind) {
            return (kind == INTEGER ? "INT" : "BIGINT") + " NOT NULL AUTO_INCREMENT PRIMARY KEY";
        }

        /**
         * VARCHAR rather than the TEXT this engine uses for an ordinary string
         * column: MySQL indexes a TEXT column only by a prefix whose length it
         * has to be given, so TEXT PRIMARY KEY is refused outright. 255 is the
         * longest a utf8mb4 key can be under the 767-byte index limit older
         * servers still have.
         */
        public String assignedKeyColumn(int kind) {
            if(kind == TEXT) {
                return "VARCHAR(255) PRIMARY KEY";
            }
            return columnType(kind) + " PRIMARY KEY";
        }

        /**
         * MySQL's parser requires a limit before it accepts an offset, and this
         * is the value its own documentation gives for "all rows from here on".
         */
        String unboundedLimit() {
            return "18446744073709551615";
        }
    }
}
