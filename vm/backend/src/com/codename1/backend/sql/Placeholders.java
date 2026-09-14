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
 * Rewrites the portable placeholder form into one engine's.
 *
 * <p>The whole job is to find the question marks that are PARAMETERS, which means
 * knowing where a question mark is something else: inside a string literal, a
 * quoted identifier, a dollar-quoted body or a comment. A search-and-replace that
 * skips that step corrupts the first statement whose text contains a question
 * mark -- a LIKE pattern, a stored JSON document, an explanatory comment -- and
 * does it silently, because the result is usually still valid SQL.
 *
 * <p>Nothing is allocated until something actually has to change. A SQLite or
 * MySQL statement in the portable form is already in its engine's form, so the
 * scan reaches the end having found nothing to do and the caller gets the string
 * it passed in.
 */
final class Placeholders {
    private Placeholders() {
    }

    /**
     * {@code sql} with each parameter placeholder rendered for the target engine
     * and each {@code ??} escape collapsed to a single question mark.
     *
     * @param dollars   true to render parameters as $1, $2 (PostgreSQL), false to
     *                  leave them as ?
     * @param nestedComments true where a block comment can contain another one
     * @param backslashEscapes true where a backslash escapes the next character
     *                  inside an ordinary string literal, which is MySQL in its
     *                  default SQL mode
     * @param hashComments true where # begins a line comment, which is MySQL
     * @param dollarQuotedStrings true where $tag$...$tag$ is a string literal,
     *                  which is PostgreSQL. MySQL allows $ inside an unquoted
     *                  identifier, so scanning for one there read a valid
     *                  statement as an unterminated literal and refused it
     * @throws IOException when the statement carries placeholders and their count
     *                     is not {@code paramCount}, or when a literal, comment or
     *                     dollar-quoted body is left unterminated
     */
    static String render(String sql, int paramCount, boolean dollars, boolean nestedComments,
                         boolean backslashEscapes, boolean hashComments,
                         boolean dollarQuotedStrings, boolean dashCommentNeedsSpace,
                         boolean bracketIdentifiers, boolean executableComments)
            throws IOException {
        StringBuilder out = null;
        int found = 0;
        int at = 0;
        int length = sql.length();
        // Whether the scan is INSIDE a version-gated executable comment. What is
        // in one runs only on a server new enough, so a placeholder there is a
        // parameter on some servers and not on others -- and the count is a claim
        // this method checks against the values supplied. See the refusal below.
        boolean gated = false;
        while(at < length) {
            char c = sql.charAt(at);
            if(executableComments && c == '/' && at + 3 < length && sql.charAt(at + 1) == '*'
                    && sql.charAt(at + 2) == '!' && sql.charAt(at + 3) >= '0'
                    && sql.charAt(at + 3) <= '9') {
                gated = true;
            } else if(executableComments && gated && c == '*' && at + 1 < length
                    && sql.charAt(at + 1) == '/') {
                gated = false;
            }
            if(c == '?') {
                if(at + 1 < length && sql.charAt(at + 1) == '?') {
                    // The escape for a literal question mark. It collapses to one
                    // on EVERY engine rather than only where it was needed, so a
                    // statement means the same thing wherever it runs. PostgreSQL
                    // is where it is needed: its jsonb containment operators are
                    // spelled ?, ?| and ?&.
                    out = pending(out, sql, at);
                    out.append('?');
                    at += 2;
                    continue;
                }
                if(gated) {
                    // UNKNOWABLE, and it has to be refused here rather than
                    // counted: MySQL prepares "SELECT 1 /*!99999 + ? */" with no
                    // parameters at all on a server the gate excludes, so the one
                    // value counted here reaches runPrepared as a parameter the
                    // statement does not have and the query fails on a count
                    // mismatch. An UNGATED "/*! ... ? ... */" always runs, so its
                    // placeholder is a placeholder and is counted.
                    throw new IOException("A parameter placeholder sits inside a MySQL "
                            + "version-gated comment, so whether it is a parameter depends "
                            + "on the server and this client cannot ask. Put the placeholder "
                            + "outside the gate, or spell the statement for one server with "
                            + "execute(): [" + sql + "]");
                }
                found++;
                if(dollars) {
                    out = pending(out, sql, at);
                    out.append('$').append(found);
                }
                at++;
                continue;
            }
            int next = skip(sql, at, nestedComments, backslashEscapes, hashComments,
                    dollarQuotedStrings, dashCommentNeedsSpace, bracketIdentifiers, executableComments);
            if(next > at) {
                if(out != null) {
                    out.append(sql, at, next);
                }
                at = next;
                continue;
            }
            if(out != null) {
                out.append(c);
            }
            at++;
        }
        // A statement with no placeholder in it is not in the portable form and
        // is not this method's business: engine-native SQL that spells its own
        // parameters, or a statement with no parameters at all. Only once one has
        // been found does the count become a claim worth checking -- and it is
        // worth checking, because the engines answer a mismatch three different
        // ways and SQLite's answer is to bind NULL and commit the row.
        if(found > 0 && found != paramCount) {
            throw new IOException("The statement has " + found + " parameter placeholder"
                    + (found == 1 ? "" : "s") + " and " + paramCount + " value"
                    + (paramCount == 1 ? "" : "s") + " were supplied: [" + sql + "]");
        }
        return out == null ? sql : out.toString();
    }

    /**
     * Starts the output buffer at the first character that has to change, copying
     * everything before it. Returns the existing buffer when there is one.
     */
    private static StringBuilder pending(StringBuilder out, String sql, int upTo) {
        if(out != null) {
            return out;
        }
        StringBuilder started = new StringBuilder(sql.length() + 8);
        started.append(sql, 0, upTo);
        return started;
    }

    /**
     * The index just past the last character that is part of the statement
     * ITSELF: not trailing whitespace, not a trailing terminator, and not a
     * trailing comment.
     *
     * <p>Where a clause has to be appended INSIDE the statement -- PostgreSQL's
     * RETURNING is the one -- this is where it goes. Appending at the end put it
     * after a semicolon, which makes it a second statement, or inside a trailing
     * "-- explanation", which comments it out; the insert then reported a key of
     * zero for a row it had written. Through the same scanner as everything else
     * here, so a semicolon or a double dash inside a literal is not mistaken for
     * either.
     */
    static int endOfStatement(String sql, boolean nestedComments, boolean backslashEscapes,
                              boolean hashComments, boolean dollarQuotedStrings,
                              boolean dashCommentNeedsSpace, boolean bracketIdentifiers,
                              boolean executableComments) throws IOException {
        int at = 0;
        int end = 0;
        int length = sql.length();
        while(at < length) {
            char c = sql.charAt(at);
            int next = skip(sql, at, nestedComments, backslashEscapes, hashComments,
                    dollarQuotedStrings, dashCommentNeedsSpace, bracketIdentifiers, executableComments);
            if(next > at) {
                // A literal or a quoted identifier is part of the statement; a
                // comment is not. skip() answers for both, so which one this was
                // is decided by what it started with.
                if(c != '-' && c != '/' && c != '#') {
                    end = next;
                }
                at = next;
                continue;
            }
            if(c != ';' && c > ' ') {
                end = at + 1;
            }
            at++;
        }
        return end;
    }

    /**
     * The number of tuples in an INSERT's VALUES clause, or -1 when there is no
     * VALUES clause to count.
     *
     * <p>Through the same scanner as everything else here, so a literal
     * containing the word VALUES or a stray parenthesis counts for nothing. The
     * keyword is recognised at nesting depth zero only, which keeps a subquery's
     * own VALUES out of the answer, and what is counted after it is the
     * top-level groups: "VALUES (?, ?), (?, ?)" is two rows of two columns, not
     * four of anything.
     */
    static int countInsertRows(String sql, boolean nestedComments, boolean backslashEscapes,
                               boolean hashComments, boolean dollarQuotedStrings,
                               boolean dashCommentNeedsSpace, boolean bracketIdentifiers,
                               boolean executableComments) throws IOException {
        if(executableComments && hasVersionGate(sql)) {
            // UNKNOWABLE, which is not the same as "no tuples to count".
            // "/*!50100 , (2) */" adds a row on a server past 5.1 and nothing on
            // an older one; "/*!99999 ..." adds nothing anywhere today. This
            // scanner has no connection to ask, and both wrong answers are bad:
            // counting the contents refuses a statement that inserts one row,
            // ignoring them runs a multi-row insert through a method that
            // promises one key. So it says it cannot tell, and Database.insert
            // refuses with a message naming the reason.
            return VERSION_GATED;
        }
        int at = 0;
        int depth = 0;
        int length = sql.length();
        int valuesAt = -1;
        while(at < length) {
            int next = skip(sql, at, nestedComments, backslashEscapes, hashComments,
                    dollarQuotedStrings, dashCommentNeedsSpace, bracketIdentifiers, executableComments);
            if(next > at) {
                at = next;
                continue;
            }
            char c = sql.charAt(at);
            if(c == '(') {
                depth++;
            } else if(c == ')') {
                depth--;
            } else if(depth == 0 && valuesAt < 0 && (c == 'v' || c == 'V')
                    && (isWord(sql, at, "values") || isWord(sql, at, "value"))) {
                // The FIRST one at the top level, and nothing replaces it.
                //
                // MySQL has a VALUES(column) EXPRESSION, used in ON DUPLICATE KEY
                // UPDATE, and it sits at the top level too: taking the last match
                // made "INSERT INTO t (a) VALUES (?), (?) ON DUPLICATE KEY UPDATE
                // a = VALUES(a)" count from that function call, find one group,
                // and report a single row. The multi-row insert then ran.
                //
                // VALUE is MySQL's accepted singular spelling of the same clause,
                // and "VALUE (?), (?)" is as multi-row as VALUES is. Recognised
                // on every dialect: the other two reject the word outright, so
                // counting the tuples of a statement they will refuse anyway
                // changes nothing.
                int afterWord = at + (isWord(sql, at, "values") ? 6 : 5);
                int tuple = skipBlanks(sql, afterWord, nestedComments, backslashEscapes,
                        hashComments, dollarQuotedStrings, dashCommentNeedsSpace,
                        bracketIdentifiers, executableComments);
                // A TUPLE HAS TO FOLLOW, or this is not the row clause.
                //
                // PostgreSQL's "OVERRIDING USER VALUE" and "OVERRIDING SYSTEM
                // VALUE" put the singular keyword in front of the real VALUES,
                // and latching the first match made
                // "INSERT INTO t(id, a) OVERRIDING USER VALUE VALUES (?,?), (?,?)"
                // count one row for a statement that writes two -- so the
                // multi-row check passed and PostgreSQL only objected after both
                // rows had committed, from the number of RETURNING rows.
                //
                // "DEFAULT VALUES" has nothing after it and still reaches
                // withoutATupleList, which answers one for it.
                if(tuple < length && (sql.charAt(tuple) == '('
                        || ((sql.charAt(tuple) == 'r' || sql.charAt(tuple) == 'R')
                                && isWord(sql, tuple, "row")))) {
                    valuesAt = afterWord;
                }
            }
            at++;
        }
        if(valuesAt < 0) {
            return withoutATupleList(sql, nestedComments, backslashEscapes, hashComments,
                    dollarQuotedStrings, dashCommentNeedsSpace, bracketIdentifiers,
                    executableComments);
        }
        // The tuple list, and NOTHING AFTER IT. Counting every top-level group
        // to the end of the statement swept up whatever followed the tuples --
        // MySQL's "ON DUPLICATE KEY UPDATE a = VALUES(a)" made a two-row insert
        // count as three. A tuple list is groups separated by commas, and it
        // ends at the first top-level token that is neither.
        int rows = 0;
        at = valuesAt;
        while(true) {
            at = skipBlanks(sql, at, nestedComments, backslashEscapes, hashComments,
                    dollarQuotedStrings, dashCommentNeedsSpace, bracketIdentifiers, executableComments);
            // MySQL writes its tuples as ROW(...) since 8.0.19, and the keyword
            // is optional: "VALUES ROW(?), ROW(?)" is two rows exactly as
            // "VALUES (?), (?)" is. Requiring the parenthesis immediately made
            // the count -1, which means "cannot tell" -- and the multi-row insert
            // then ran. Recognised on every dialect: the other two reject the
            // word, so counting a statement they will refuse costs nothing.
            if(at < length && (sql.charAt(at) == 'r' || sql.charAt(at) == 'R')
                    && isWord(sql, at, "row")) {
                at = skipBlanks(sql, at + 3, nestedComments, backslashEscapes, hashComments,
                        dollarQuotedStrings, dashCommentNeedsSpace, bracketIdentifiers,
                        executableComments);
            }
            if(at >= length || sql.charAt(at) != '(') {
                break;
            }
            rows++;
            int depthHere = 0;
            while(at < length) {
                int next = skip(sql, at, nestedComments, backslashEscapes, hashComments,
                        dollarQuotedStrings, dashCommentNeedsSpace, bracketIdentifiers, executableComments);
                if(next > at) {
                    at = next;
                    continue;
                }
                char c = sql.charAt(at);
                at++;
                if(c == '(') {
                    depthHere++;
                } else if(c == ')') {
                    depthHere--;
                    if(depthHere == 0) {
                        break;
                    }
                }
            }
            at = skipBlanks(sql, at, nestedComments, backslashEscapes, hashComments,
                    dollarQuotedStrings, dashCommentNeedsSpace, bracketIdentifiers, executableComments);
            if(at >= length || sql.charAt(at) != ',') {
                break;
            }
            at++;
        }
        if(rows == 0) {
            // The word VALUES with no tuple after it, which is "DEFAULT VALUES".
            return withoutATupleList(sql, nestedComments, backslashEscapes, hashComments,
                    dollarQuotedStrings, dashCommentNeedsSpace, bracketIdentifiers,
                    executableComments);
        }
        return rows;
    }

    /**
     * How many rows an INSERT with no tuple list writes: one, or an unknown
     * number.
     *
     * <p>Three shapes reach here and only one of them is unbounded. "INSERT INTO
     * t DEFAULT VALUES" and MySQL's "INSERT INTO t SET a = ?" write exactly one
     * row, and refusing them would refuse an ordinary single-row insert. An
     * insert whose rows come from a QUERY -- INSERT ... SELECT, or MySQL's
     * INSERT ... TABLE -- writes a number of rows only the server knows, which
     * is what {@code -1} means here and what Database.insert refuses.
     *
     * <p>The keyword has to be at the TOP LEVEL to count: "INSERT INTO t SET a =
     * (SELECT max(x) FROM u)" is a single row whose value happens to come from a
     * subquery, and the parentheses are what say so.
     */
    private static int withoutATupleList(String sql, boolean nestedComments,
                                         boolean backslashEscapes, boolean hashComments,
                                         boolean dollarQuotedStrings,
                                         boolean dashCommentNeedsSpace,
                                         boolean bracketIdentifiers,
                                         boolean executableComments) throws IOException {
        int at = 0;
        int depth = 0;
        int length = sql.length();
        while(at < length) {
            int next = skip(sql, at, nestedComments, backslashEscapes, hashComments,
                    dollarQuotedStrings, dashCommentNeedsSpace, bracketIdentifiers, executableComments);
            if(next > at) {
                at = next;
                continue;
            }
            char c = sql.charAt(at);
            if(c == '(') {
                depth++;
            } else if(c == ')') {
                depth--;
            } else if(depth == 0 && (isWord(sql, at, "select") || isWord(sql, at, "table"))) {
                return -1;
            }
            at++;
        }
        return 1;
    }

    /** The index of the next character that is neither whitespace nor a comment. */
    private static int skipBlanks(String sql, int at, boolean nestedComments,
                                  boolean backslashEscapes, boolean hashComments,
                                  boolean dollarQuotedStrings, boolean dashCommentNeedsSpace,
                                  boolean bracketIdentifiers, boolean executableComments)
            throws IOException {
        while(at < sql.length()) {
            char c = sql.charAt(at);
            if(c <= ' ') {
                at++;
                continue;
            }
            // Only a COMMENT is skipped here: skip() also steps over literals,
            // and a literal where a tuple was expected ends the list rather than
            // being passed over.
            if(c == '-' || c == '/' || c == '*' || (hashComments && c == '#')) {
                int next = skip(sql, at, nestedComments, backslashEscapes, hashComments,
                        dollarQuotedStrings, dashCommentNeedsSpace, bracketIdentifiers, executableComments);
                if(next > at) {
                    at = next;
                    continue;
                }
            }
            return at;
        }
        return at;
    }

    /** countInsertRows: the statement's row count depends on the server version. */
    static final int VERSION_GATED = -2;

    /**
     * Whether the statement carries a MySQL version-gated executable comment --
     * "/*!" followed by digits.
     *
     * <p>Scanned crudely on purpose: this runs before the tuple walk and only has
     * to notice that a gate EXISTS. A "/*!" inside a string literal would be a
     * false positive, and the cost of one is a refusal with a clear message
     * rather than a wrong count.
     */
    private static boolean hasVersionGate(String sql) {
        int at = sql.indexOf("/*!");
        while(at >= 0) {
            int after = at + 3;
            if(after < sql.length() && sql.charAt(after) >= '0' && sql.charAt(after) <= '9') {
                return true;
            }
            at = sql.indexOf("/*!", at + 3);
        }
        return false;
    }

    /**
     * Whether the statement updates an existing row when it conflicts --
     * "ON CONFLICT ... DO UPDATE" or MySQL's "ON DUPLICATE KEY UPDATE".
     *
     * <p>Scanned as SQL rather than searched for as text, so a DO UPDATE inside
     * a string literal or a comment is not one. The question is asked of the
     * statement's SHAPE and not of a particular execution on purpose: which
     * branch an upsert takes depends on the rows that happen to be there, so a
     * statement that CAN update is one whose generated key is undefined on an
     * engine that reads the key from connection state.
     */
    static boolean updatesOnConflict(String sql, boolean nestedComments,
                                     boolean backslashEscapes, boolean hashComments,
                                     boolean dollarQuotedStrings, boolean dashCommentNeedsSpace,
                                     boolean bracketIdentifiers, boolean executableComments)
            throws IOException {
        int at = 0;
        int length = sql.length();
        while(at < length) {
            int next = skip(sql, at, nestedComments, backslashEscapes, hashComments,
                    dollarQuotedStrings, dashCommentNeedsSpace, bracketIdentifiers, executableComments);
            if(next > at) {
                at = next;
                continue;
            }
            if(isWord(sql, at, "DO")) {
                int after = skipBlanks(sql, at + 2, nestedComments, backslashEscapes,
                        hashComments, dollarQuotedStrings, dashCommentNeedsSpace,
                        bracketIdentifiers, executableComments);
                if(after < length && isWord(sql, after, "UPDATE")) {
                    return true;
                }
            }
            if(isWord(sql, at, "DUPLICATE")) {
                int key = skipBlanks(sql, at + 9, nestedComments, backslashEscapes,
                        hashComments, dollarQuotedStrings, dashCommentNeedsSpace,
                        bracketIdentifiers, executableComments);
                if(key < length && isWord(sql, key, "KEY")) {
                    int update = skipBlanks(sql, key + 3, nestedComments, backslashEscapes,
                            hashComments, dollarQuotedStrings, dashCommentNeedsSpace,
                            bracketIdentifiers, executableComments);
                    if(update < length && isWord(sql, update, "UPDATE")) {
                        return true;
                    }
                }
            }
            at++;
        }
        return false;
    }

    /**
     * Whether {@code word} sits at {@code at} as a whole word, ignoring case --
     * so the VALUES in "revalues" or "values_of" is not the keyword.
     */
    private static boolean isWord(String sql, int at, String word) {
        if(!sql.regionMatches(true, at, word, 0, word.length())) {
            return false;
        }
        if(at > 0 && isWordChar(sql.charAt(at - 1))) {
            return false;
        }
        int after = at + word.length();
        return after >= sql.length() || !isWordChar(sql.charAt(after));
    }

    /**
     * The character before {@code at}, or a space when there is none -- which is
     * a boundary, and is what the start of a statement is.
     */
    private static char charBefore(String sql, int at) {
        return at == 0 ? ' ' : sql.charAt(at - 1);
    }

    /**
     * Whether {@code at} is whitespace, a control character, or the end of the
     * statement.
     *
     * <p>MySQL asks this of the character after a double dash before it will
     * call one a comment, which is why "VALUES (5--1), (2)" is two rows of
     * arithmetic there and one row plus a comment everywhere else. Reading it as
     * a comment hid the second tuple from the multi-row insert check.
     */
    private static boolean isBlankOrEnd(String sql, int at) {
        return at >= sql.length() || sql.charAt(at) <= ' ';
    }

    private static boolean isWordChar(char c) {
        // ANYTHING ABOVE ASCII COUNTS. PostgreSQL's unquoted identifiers admit
        // the letters of the server encoding, not just a-z, and this predicate
        // decides where a token begins: with ASCII alone, the $ in "cafe$usd$"
        // -- e-acute in place of that e -- looked like the start of a
        // dollar-quoted string rather than part of the identifier, and a
        // perfectly good statement was refused as unterminated.
        //
        // A superset of PostgreSQL's rule, deliberately. The question asked here
        // is only whether a character joins the token before it, and a $ or a
        // keyword sitting immediately after a non-ASCII character is part of an
        // identifier under every reading -- a dollar-quote opener follows
        // whitespace, an operator or a bracket.
        return c == '_' || c == '$' || c >= 0x80 || (c >= '0' && c <= '9')
                || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    /**
     * The index just past the literal, identifier, comment or dollar-quoted body
     * beginning at {@code at}, or {@code at} itself when nothing begins there.
     */
    private static int skip(String sql, int at, boolean nestedComments,
                            boolean backslashEscapes, boolean hashComments,
                            boolean dollarQuotedStrings, boolean dashCommentNeedsSpace,
                            boolean bracketIdentifiers, boolean executableComments)
            throws IOException {
        char c = sql.charAt(at);
        int length = sql.length();
        if(c == '\'') {
            // MySQL escapes with a backslash in EVERY literal; PostgreSQL only in
            // an E'' one; SQLite not at all, where a backslash is an ordinary
            // character. Reading them all one way breaks the other two: with the
            // escape assumed, "SELECT 'a\\'" swallows the rest of the statement
            // on SQLite, and without it "SELECT 'it\\'s ?'" ends the literal at
            // the escaped quote on MySQL and counts the ? that follows.
            return skipQuoted(sql, at, '\'',
                    backslashEscapes || eStringBackslash(sql, at));
        }
        if(hashComments && c == '#') {
            // MySQL's other line comment. Missing it counted every ? in a
            // commented-out line as a parameter.
            int end = sql.indexOf('\n', at + 1);
            return end < 0 ? length : end + 1;
        }
        if(c == '"') {
            // A QUOTED IDENTIFIER on SQLite and PostgreSQL; on MySQL, in its
            // default SQL mode, a STRING LITERAL with the same backslash escapes
            // its single-quoted strings have. Scanned without them, SELECT
            // "it\"s ?" ended at the escaped quote and the ? after it was
            // counted as a parameter. The flag is only ever set for MySQL, so
            // passing it here is right for all three.
            return skipQuoted(sql, at, '"', backslashEscapes);
        }
        if(c == '`') {
            return skipQuoted(sql, at, '`', false);
        }
        if(bracketIdentifiers && c == '[') {
            // SQLite accepts [identifier] as well, and a ? inside one was being
            // counted as a parameter. SQLITE ONLY: PostgreSQL spells an ARRAY
            // SUBSCRIPT with brackets, so skipping them there would swallow a[1]
            // and everything after it.
            int end = sql.indexOf(']', at + 1);
            if(end < 0) {
                throw unterminated(sql, "bracket-quoted identifier");
            }
            return end + 1;
        }
        if(c == '-' && at + 1 < length && sql.charAt(at + 1) == '-'
                && (!dashCommentNeedsSpace || isBlankOrEnd(sql, at + 2))) {
            int end = sql.indexOf('\n', at + 2);
            // A line comment with no newline after it runs to the end of the
            // statement, which is not an error -- unlike the cases below, there
            // is nothing left unterminated.
            return end < 0 ? length : end + 1;
        }
        if(c == '/' && at + 1 < length && sql.charAt(at + 1) == '*') {
            if(executableComments && at + 2 < length && sql.charAt(at + 2) == '!') {
                // MySQL's EXECUTABLE comment. "/*! ... */" and "/*!50100 ... */"
                // are not comments there at all: the delimiters are stripped and
                // what is between them RUNS. Skipped as a comment,
                // "VALUES (1) /*! , (2) */" counted one tuple for an insert that
                // writes two, which walked straight past the multi-row check.
                //
                // The opener is stepped over as though it were whitespace, so the
                // contents are scanned as the SQL they are; the closer below does
                // the same.
                int after = at + 3;
                while(after < length && sql.charAt(after) >= '0' && sql.charAt(after) <= '9') {
                    after++;
                }
                return after;
                // The VERSION GATE those digits carry is deliberately not read
                // here; see countInsertRows, which refuses to answer for a
                // statement carrying one. "/*!50100 ..." runs on any server past
                // 5.1 and "/*!99999 ..." runs on none, so what the statement
                // MEANS depends on a version this scanner has no connection to
                // ask for.
            }
            return skipBlockComment(sql, at, nestedComments);
        }
        if(executableComments && c == '*' && at + 1 < length && sql.charAt(at + 1) == '/') {
            // The other half of the executable comment above. Outside one, a
            // bare "*/" cannot occur in valid SQL.
            return at + 2;
        }
        if(dollarQuotedStrings && c == '$' && !isWordChar(charBefore(sql, at))) {
            // AT A TOKEN BOUNDARY ONLY. PostgreSQL allows a dollar after the
            // first character of an unquoted identifier, and a dollar-quoted
            // string may not adjoin one -- so price$usd$ is an identifier, and
            // reading it as an opener left the body unterminated and refused a
            // valid statement.
            return skipDollarQuoted(sql, at);
        }
        return at;
    }

    /**
     * Whether the literal starting at {@code at} is one where a backslash escapes
     * the following character.
     *
     * <p>It is exactly PostgreSQL's E'' form, and this is asked only where the
     * dialect does not already escape with a backslash everywhere. In an ordinary literal a backslash
     * is an ordinary character -- that is what standard_conforming_strings means,
     * on by default for fifteen years -- so treating one as an escape would read
     * the closing quote of 'a\' as escaped and swallow the rest of the statement
     * into the literal. Reading the prefix is what keeps both forms right.
     */
    private static boolean eStringBackslash(String sql, int at) {
        if(at == 0) {
            return false;
        }
        char prefix = sql.charAt(at - 1);
        if(prefix != 'e' && prefix != 'E') {
            return false;
        }
        // And the E has to BE the prefix rather than the tail of an identifier:
        // the literal in "SELECT type'x'" is an ordinary one.
        if(at >= 2) {
            char before = sql.charAt(at - 2);
            if(before == '_' || (before >= '0' && before <= '9')
                    || (before >= 'a' && before <= 'z') || (before >= 'A' && before <= 'Z')) {
                return false;
            }
        }
        return true;
    }

    private static int skipQuoted(String sql, int at, char quote, boolean backslash)
            throws IOException {
        int length = sql.length();
        int iter = at + 1;
        while(iter < length) {
            char c = sql.charAt(iter);
            if(backslash && c == '\\' && iter + 1 < length) {
                iter += 2;
                continue;
            }
            if(c == quote) {
                if(iter + 1 < length && sql.charAt(iter + 1) == quote) {
                    // A doubled quote is the quote character itself, in a literal
                    // and in an identifier alike, and the literal does not end.
                    iter += 2;
                    continue;
                }
                return iter + 1;
            }
            iter++;
        }
        throw unterminated(sql, quote == '\'' ? "string literal" : "quoted identifier");
    }

    private static int skipBlockComment(String sql, int at, boolean nested) throws IOException {
        int length = sql.length();
        int depth = 1;
        int iter = at + 2;
        while(iter + 1 < length) {
            char c = sql.charAt(iter);
            char next = sql.charAt(iter + 1);
            if(nested && c == '/' && next == '*') {
                depth++;
                iter += 2;
                continue;
            }
            if(c == '*' && next == '/') {
                depth--;
                iter += 2;
                if(depth == 0) {
                    return iter;
                }
                continue;
            }
            iter++;
        }
        throw unterminated(sql, "block comment");
    }

    /**
     * A dollar-quoted body, $$like this$$ or $tag$like this$tag$, which is how a
     * function body gets into a statement without escaping anything inside it.
     *
     * <p>A dollar that does not open one is an ordinary character and the scan
     * continues from it -- which is what leaves a hand-written PostgreSQL $1
     * alone. The tag rule is the engine's: letters, digits and underscores, not
     * starting with a digit.
     */
    private static int skipDollarQuoted(String sql, int at) throws IOException {
        int length = sql.length();
        int iter = at + 1;
        while(iter < length) {
            char c = sql.charAt(iter);
            if(c == '$') {
                break;
            }
            boolean letter = c == '_' || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
            // A digit is a tag character everywhere except first, which is the
            // rule that leaves a hand-written $1 alone: it is a parameter that
            // PostgreSQL spells for itself, not the opening of a quoted body.
            boolean digit = c >= '0' && c <= '9' && iter > at + 1;
            if(!letter && !digit) {
                return at;
            }
            iter++;
        }
        if(iter >= length) {
            return at;
        }
        String tag = sql.substring(at, iter + 1);
        int end = sql.indexOf(tag, iter + 1);
        if(end < 0) {
            throw unterminated(sql, "dollar-quoted string " + tag);
        }
        return end + tag.length();
    }

    private static IOException unterminated(String sql, String what) {
        return new IOException("The statement ends inside a " + what
                + ", so where its parameters are cannot be determined: [" + sql + "]");
    }
}
