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
                         boolean dollarQuotedStrings) throws IOException {
        StringBuilder out = null;
        int found = 0;
        int at = 0;
        int length = sql.length();
        while(at < length) {
            char c = sql.charAt(at);
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
                found++;
                if(dollars) {
                    out = pending(out, sql, at);
                    out.append('$').append(found);
                }
                at++;
                continue;
            }
            int next = skip(sql, at, nestedComments, backslashEscapes, hashComments,
                    dollarQuotedStrings);
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
                              boolean hashComments, boolean dollarQuotedStrings)
            throws IOException {
        int at = 0;
        int end = 0;
        int length = sql.length();
        while(at < length) {
            char c = sql.charAt(at);
            int next = skip(sql, at, nestedComments, backslashEscapes, hashComments,
                    dollarQuotedStrings);
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
     * The index just past the literal, identifier, comment or dollar-quoted body
     * beginning at {@code at}, or {@code at} itself when nothing begins there.
     */
    private static int skip(String sql, int at, boolean nestedComments,
                            boolean backslashEscapes, boolean hashComments,
                            boolean dollarQuotedStrings) throws IOException {
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
            return skipQuoted(sql, at, '"', false);
        }
        if(c == '`') {
            return skipQuoted(sql, at, '`', false);
        }
        if(c == '-' && at + 1 < length && sql.charAt(at + 1) == '-') {
            int end = sql.indexOf('\n', at + 2);
            // A line comment with no newline after it runs to the end of the
            // statement, which is not an error -- unlike the cases below, there
            // is nothing left unterminated.
            return end < 0 ? length : end + 1;
        }
        if(c == '/' && at + 1 < length && sql.charAt(at + 1) == '*') {
            return skipBlockComment(sql, at, nestedComments);
        }
        if(dollarQuotedStrings && c == '$') {
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
