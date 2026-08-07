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
package com.codename1.impl;

import java.util.ArrayList;
import java.util.List;

/// Splits a SQL script into individual statements, the way SQLite itself would.
///
/// `Database#execute(java.lang.String)` accepts a script containing several statements, but not
/// every engine behind this API does: Android's `execSQL` rejects anything after the first
/// statement, and a JDBC `PreparedStatement` silently ignores it. Ports in that position use this
/// class to run the statements one at a time, and the parameterized methods use
/// `#countStatements(java.lang.String)` to reject a script rather than silently dropping its tail.
///
/// Semicolons inside string literals, quoted identifiers and comments do not split, and neither do
/// those inside a `CREATE TRIGGER` body.
///
/// #### Trigger bodies
///
/// None of `BEGIN`, `CASE` or `END` is reserved in SQLite, so a column may be called any of them
/// and counting the words alone splits a valid trigger into fragments. The body is tracked by
/// position instead: it opens at the first `BEGIN` after `CREATE TRIGGER`, and closes at an `END`
/// that follows a completed statement -- one that comes straight after that statement's semicolon.
/// A column called `end` never appears there. It follows a keyword in `SELECT end FROM t`, an
/// operator in `SET x = end`, and the `END` of a `CASE` follows the expression it returns.
public final class SQLStatementSplitter {

    private SQLStatementSplitter() {
    }

    /// Splits a SQL script into its individual statements.
    ///
    /// Trailing whitespace, comments and empty statements produced by a trailing semicolon are
    /// discarded, so a single statement written with or without a terminating semicolon yields one
    /// element either way.
    ///
    /// #### Parameters
    ///
    /// - `sql`: the script to split
    ///
    /// #### Returns
    ///
    /// the statements, never null and never containing an empty element
    public static String[] split(String sql) {
        List statements = new ArrayList();
        if (sql == null) {
            return new String[0];
        }
        int length = sql.length();
        int statementStart = 0;
        boolean sawCreate = false;
        boolean inTrigger = false;
        boolean bodyOpen = false;
        // The last character that was neither whitespace nor part of a comment, outside quotes.
        // This is what tells a structural END from a column called "end".
        char lastSignificant = 0;

        int iter = 0;
        while (iter < length) {
            char c = sql.charAt(iter);

            if (c == '\'' || c == '"' || c == '`') {
                iter = skipQuoted(sql, iter, c);
                lastSignificant = 'x';
                continue;
            }
            if (c == '[') {
                iter = skipUntil(sql, iter + 1, ']');
                lastSignificant = 'x';
                continue;
            }
            if (c == '-' && iter + 1 < length && sql.charAt(iter + 1) == '-') {
                iter = skipLineComment(sql, iter + 2);
                continue;
            }
            if (c == '/' && iter + 1 < length && sql.charAt(iter + 1) == '*') {
                iter = skipBlockComment(sql, iter + 2);
                continue;
            }

            if (isWordStart(c)) {
                int wordEnd = iter;
                while (wordEnd < length && isWordPart(sql.charAt(wordEnd))) {
                    wordEnd++;
                }
                String word = sql.substring(iter, wordEnd);
                if ("CREATE".equalsIgnoreCase(word)) {
                    sawCreate = true;
                } else if ("TRIGGER".equalsIgnoreCase(word)) {
                    if (sawCreate) {
                        inTrigger = true;
                    }
                } else if ("BEGIN".equalsIgnoreCase(word)) {
                    // The body opens at the first BEGIN after CREATE TRIGGER and only there, so a
                    // later column called "begin" cannot reopen it.
                    if (inTrigger && !bodyOpen) {
                        bodyOpen = true;
                    }
                } else if ("END".equalsIgnoreCase(word)) {
                    // The END that closes the body is the one that follows a completed statement,
                    // so it comes straight after that statement's semicolon - or after the BEGIN
                    // itself. Anything else called "end" is a column: "SELECT end FROM t" follows
                    // a keyword, "SET x = end" follows an operator, and the END of a CASE follows
                    // the expression it returns. That is what separates them, rather than what
                    // happens to come next.
                    if (bodyOpen && (lastSignificant == ';' || lastSignificant == 0)) {
                        bodyOpen = false;
                    }
                } else if (!"TEMP".equalsIgnoreCase(word) && !"TEMPORARY".equalsIgnoreCase(word)
                        && !"IF".equalsIgnoreCase(word) && !"NOT".equalsIgnoreCase(word)
                        && !"EXISTS".equalsIgnoreCase(word)) {
                    // Any other keyword means this CREATE was not a CREATE TRIGGER.
                    sawCreate = false;
                }
                lastSignificant = 'x';
                iter = wordEnd;
                continue;
            }

            if (c == ';' && !bodyOpen) {
                addIfNotBlank(statements, sql.substring(statementStart, iter));
                statementStart = iter + 1;
                sawCreate = false;
                inTrigger = false;
                lastSignificant = 0;
                iter++;
                continue;
            }
            if (c > ' ') {
                lastSignificant = c;
            }
            iter++;
        }
        addIfNotBlank(statements, sql.substring(statementStart));

        String[] out = new String[statements.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = (String) statements.get(i);
        }
        return out;
    }

    /// Counts the statements in a script.
    ///
    /// #### Parameters
    ///
    /// - `sql`: the script to inspect
    ///
    /// #### Returns
    ///
    /// the number of statements, zero for a script that is entirely blank or comments
    public static int countStatements(String sql) {
        return split(sql).length;
    }

    /// Reports whether a script holds anything beyond a single statement.
    ///
    /// #### Parameters
    ///
    /// - `sql`: the script to inspect
    ///
    /// #### Returns
    ///
    /// true if the script contains two or more statements
    public static boolean isMultiStatement(String sql) {
        return countStatements(sql) > 1;
    }

    /// Reported by `#countParameters(String)` for a statement whose parameters cannot be counted
    /// by reading the text.
    public static final int PARAMETER_COUNT_UNKNOWN = -1;

    /// Counts the parameters a statement declares, the way `sqlite3_bind_parameter_count` does.
    ///
    /// For ports whose engine cannot be asked. The SQLite C API answers this directly and JDBC
    /// answers it through `ParameterMetaData`, but the Android engine exposes no equivalent, so
    /// the count has to come from the text. Quoted strings, quoted identifiers, bracketed
    /// identifiers and comments are skipped, so a marker inside any of them is not counted.
    ///
    /// #### What the count is
    ///
    /// It is the **largest index assigned**, not the number of markers, because SQLite assigns
    /// indices rather than counting occurrences:
    ///
    /// - `?` takes the next index, one past the largest assigned so far.
    /// - `?NNN` takes index NNN, and raises the largest if NNN is above it. That is what makes
    ///   `SELECT ?3` a three parameter statement with two unbound slots.
    /// - `:name`, `@name` and `$name` take the next index the first time the name appears and
    ///   reuse that index every time after, so `WHERE a = :x OR b = :x` declares one parameter.
    ///
    /// Reading these as one-per-marker is what would let a caller supply one argument to a two
    /// parameter statement and have the second silently stay SQL NULL, which is the outcome this
    /// exists to prevent.
    ///
    /// `#PARAMETER_COUNT_UNKNOWN` is reported only for a form no count is defined for -- `?0`,
    /// which SQLite rejects, and an index too large to be meaningful. The caller skips the check
    /// there rather than rejecting a statement it cannot judge.
    ///
    /// #### Parameters
    ///
    /// - `sql`: the statement to inspect
    ///
    /// #### Returns
    ///
    /// the number of parameters, or `#PARAMETER_COUNT_UNKNOWN` if no count is defined
    public static int countParameters(String sql) {
        if (sql == null) {
            return 0;
        }
        // Names already seen. Only whether a name has appeared before matters -- if it has, it
        // reuses the index it was given and adds nothing to the count, and if it has not, it takes
        // the next one. Kept as a list because a statement has a handful of parameters, not
        // thousands, and core targets a Java without generics.
        List names = new ArrayList();
        int largest = 0;
        int length = sql.length();
        int iter = 0;
        while (iter < length) {
            char c = sql.charAt(iter);
            if (c == '\'' || c == '"' || c == '`') {
                iter = skipQuoted(sql, iter, c);
                continue;
            }
            if (c == '[') {
                iter = skipUntil(sql, iter + 1, ']');
                continue;
            }
            if (c == '-' && iter + 1 < length && sql.charAt(iter + 1) == '-') {
                iter = skipLineComment(sql, iter + 2);
                continue;
            }
            if (c == '/' && iter + 1 < length && sql.charAt(iter + 1) == '*') {
                iter = skipBlockComment(sql, iter + 2);
                continue;
            }
            if (c == ':' || c == '@' || c == '$') {
                // Only a parameter when a name actually follows: "::" is not one, nor a bare "@",
                // nor the ":" of an assignment.
                int nameEnd = iter + 1;
                while (nameEnd < length && isParameterNamePart(sql.charAt(nameEnd))) {
                    nameEnd++;
                }
                if (nameEnd == iter + 1) {
                    iter++;
                    continue;
                }
                String name = sql.substring(iter, nameEnd);
                if (!containsName(names, name)) {
                    largest++;
                    names.add(name);
                }
                iter = nameEnd;
                continue;
            }
            if (c == '?') {
                int digitsEnd = iter + 1;
                while (digitsEnd < length && sql.charAt(digitsEnd) >= '0'
                        && sql.charAt(digitsEnd) <= '9') {
                    digitsEnd++;
                }
                if (digitsEnd == iter + 1) {
                    largest++;
                    iter++;
                    continue;
                }
                int explicit = parseIndex(sql.substring(iter + 1, digitsEnd));
                if (explicit <= 0) {
                    // "?0" is not a parameter SQLite will accept, and an index this cannot
                    // represent is not one it is worth guessing at.
                    return PARAMETER_COUNT_UNKNOWN;
                }
                if (explicit > largest) {
                    largest = explicit;
                }
                iter = digitsEnd;
                continue;
            }
            iter++;
        }
        return largest;
    }

    /// Whether a parameter name has already been seen.
    ///
    /// Names are compared exactly, including the leading sigil: SQLite treats `:x` and `@x` as
    /// different parameters, and it is case sensitive about the rest.
    private static boolean containsName(List names, String name) {
        for (Object seen : names) {
            if (name.equals(seen)) {
                return true;
            }
        }
        return false;
    }

    /// Parses a numbered placeholder's index, or -1 if it is zero or too large to be one.
    private static int parseIndex(String digits) {
        int value = 0;
        for (int iter = 0; iter < digits.length(); iter++) {
            value = value * 10 + (digits.charAt(iter) - '0');
            if (value > MAX_PARAMETER_INDEX) {
                return -1;
            }
        }
        return value == 0 ? -1 : value;
    }

    /// SQLite's own default ceiling on a parameter index. Above it the engine rejects the
    /// statement, so there is no count to report.
    private static final int MAX_PARAMETER_INDEX = 32766;

    /// Whether a character continues a named parameter, matching SQLite's own rule.
    private static boolean isParameterNamePart(char c) {
        return isWordPart(c) || c == '$' || c > 127;
    }

    private static void addIfNotBlank(List statements, String candidate) {
        String trimmed = stripToContent(candidate);
        if (trimmed.length() > 0) {
            statements.add(candidate.trim());
        }
    }

    /// Returns the statement with comments and whitespace removed, purely to decide whether it
    /// carries any actual SQL. A chunk that is only a trailing comment is not a statement.
    private static String stripToContent(String sql) {
        StringBuilder out = new StringBuilder();
        int length = sql.length();
        int iter = 0;
        while (iter < length) {
            char c = sql.charAt(iter);
            if (c == '-' && iter + 1 < length && sql.charAt(iter + 1) == '-') {
                iter = skipLineComment(sql, iter + 2);
                continue;
            }
            if (c == '/' && iter + 1 < length && sql.charAt(iter + 1) == '*') {
                iter = skipBlockComment(sql, iter + 2);
                continue;
            }
            if (c > ' ') {
                out.append(c);
            }
            iter++;
        }
        return out.toString();
    }

    private static int skipQuoted(String sql, int openIndex, char quote) {
        int length = sql.length();
        int iter = openIndex + 1;
        while (iter < length) {
            char c = sql.charAt(iter);
            if (c == quote) {
                // A doubled quote is an escaped quote, not the end of the literal.
                if (iter + 1 < length && sql.charAt(iter + 1) == quote) {
                    iter += 2;
                    continue;
                }
                return iter + 1;
            }
            iter++;
        }
        return length;
    }

    private static int skipUntil(String sql, int from, char terminator) {
        int length = sql.length();
        int iter = from;
        while (iter < length) {
            if (sql.charAt(iter) == terminator) {
                return iter + 1;
            }
            iter++;
        }
        return length;
    }

    private static int skipLineComment(String sql, int from) {
        int length = sql.length();
        int iter = from;
        while (iter < length && sql.charAt(iter) != '\n') {
            iter++;
        }
        return iter;
    }

    private static int skipBlockComment(String sql, int from) {
        int length = sql.length();
        int iter = from;
        while (iter < length) {
            if (sql.charAt(iter) == '*' && iter + 1 < length && sql.charAt(iter + 1) == '/') {
                return iter + 2;
            }
            iter++;
        }
        return length;
    }

    private static boolean isWordStart(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private static boolean isWordPart(char c) {
        return isWordStart(c) || (c >= '0' && c <= '9');
    }
}
