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
/// #### Known limitation
///
/// Trigger bodies are recognised by tracking `BEGIN`/`END` nesting after `CREATE TRIGGER`, which
/// also counts the `END` that closes a `CASE` expression. A trigger body whose `BEGIN`/`END`
/// structure is unbalanced by construction cannot be split correctly -- but such a script would not
/// parse in SQLite either.
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
        int beginDepth = 0;
        boolean sawCreate = false;
        boolean inTrigger = false;

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
                } else if ("BEGIN".equalsIgnoreCase(word) || "CASE".equalsIgnoreCase(word)) {
                    if (inTrigger && isDelimiterUse(sql, wordEnd)) {
                        beginDepth++;
                    }
                } else if ("END".equalsIgnoreCase(word)) {
                    if (inTrigger && beginDepth > 0 && isDelimiterUse(sql, wordEnd)) {
                        beginDepth--;
                    }
                } else if (!"TEMP".equalsIgnoreCase(word) && !"TEMPORARY".equalsIgnoreCase(word)
                        && !"IF".equalsIgnoreCase(word) && !"NOT".equalsIgnoreCase(word)
                        && !"EXISTS".equalsIgnoreCase(word)) {
                    // Any other keyword means this CREATE was not a CREATE TRIGGER.
                    sawCreate = false;
                }
                iter = wordEnd;
                continue;
            }

            if (c == ';' && (!inTrigger || beginDepth == 0)) {
                addIfNotBlank(statements, sql.substring(statementStart, iter));
                statementStart = iter + 1;
                sawCreate = false;
                inTrigger = false;
                beginDepth = 0;
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

    /// Reported by `#countPositionalParameters(String)` for a statement whose parameters cannot be
    /// counted by reading the text.
    public static final int PARAMETER_COUNT_UNKNOWN = -1;

    /// Counts the `?` placeholders in a statement.
    ///
    /// For ports whose engine cannot be asked. The SQLite C API answers this directly through
    /// `sqlite3_bind_parameter_count`, and JDBC through `ParameterMetaData`, but the Android
    /// engine exposes no equivalent, so the count has to come from the text. Quoted strings,
    /// quoted identifiers, bracketed identifiers and comments are skipped, so a literal `?` inside
    /// any of them is not miscounted.
    ///
    /// SQLite also accepts numbered and named placeholders -- `?NNN`, `:name`, `@name` and
    /// `$name` -- whose count is not the number of markers, because one may repeat and `?NNN` may
    /// skip indices. Rather than guess, this reports `#PARAMETER_COUNT_UNKNOWN` when it sees one,
    /// and the caller skips the check instead of rejecting a valid statement.
    ///
    /// #### Parameters
    ///
    /// - `sql`: the statement to inspect
    ///
    /// #### Returns
    ///
    /// the number of `?` placeholders, or `#PARAMETER_COUNT_UNKNOWN` if the statement uses a form
    /// this cannot count
    public static int countPositionalParameters(String sql) {
        if (sql == null) {
            return 0;
        }
        int count = 0;
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
                // A named parameter, but only when a name actually follows: "::" and a bare "@"
                // are not, and neither is the ":" of a cast or an assignment.
                if (iter + 1 < length && isWordPart(sql.charAt(iter + 1))) {
                    return PARAMETER_COUNT_UNKNOWN;
                }
                iter++;
                continue;
            }
            if (c == '?') {
                if (iter + 1 < length && sql.charAt(iter + 1) >= '0' && sql.charAt(iter + 1) <= '9') {
                    return PARAMETER_COUNT_UNKNOWN;
                }
                count++;
            }
            iter++;
        }
        return count;
    }

    /// Distinguishes a structural `BEGIN`, `CASE` or `END` from a column that happens to be
    /// called one of those.
    ///
    /// None of them are reserved words in SQLite, so `UPDATE t SET end = 1` is valid and used to
    /// decrement the trigger depth, splitting the trigger at the next semicolon and handing the
    /// engine two malformed fragments. What separates the two uses is what follows: a delimiter
    /// is followed by more statement, an identifier by an operator, a separator or a closing
    /// bracket.
    ///
    /// This does not catch every case -- `SELECT end FROM t` still reads as a delimiter, because
    /// telling those apart needs a real parser rather than a splitter. It covers the assignment
    /// and reference forms, which is where a column called `end` actually turns up.
    ///
    /// #### Parameters
    ///
    /// - `sql`: the script
    /// - `wordEnd`: index just past the word
    ///
    /// #### Returns
    ///
    /// true if the word reads as a structural delimiter
    private static boolean isDelimiterUse(String sql, int wordEnd) {
        int iter = wordEnd;
        int length = sql.length();
        while (iter < length && sql.charAt(iter) <= ' ') {
            iter++;
        }
        if (iter >= length) {
            return true;
        }
        // What follows an identifier is an operator, a separator or a closing bracket; what
        // follows a delimiter is more statement. "<" and ">" are included because "end <> 1" and
        // "end < 1" are both comparisons.
        return "=,).<>!+-*/|".indexOf(sql.charAt(iter)) < 0;
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
