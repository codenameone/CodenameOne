/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.mcp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Edits the `mcp_servers` tables of a Codex style `config.toml` in place, so a
/// Codename One tool can register and unregister itself without a TOML library and
/// without disturbing the rest of the user's configuration.
///
/// This is deliberately a STRUCTURAL editor rather than a parser: it walks the document
/// far enough to know where every table and every key assignment begins and ends, and
/// then rewrites only the byte range that belongs to one server. Everything else -
/// comments, key order, formatting, values whose types it never inspects - survives
/// untouched, which a parse-and-reserialize round trip could not promise.
///
/// The other half of that promise is refusing. A document the walk cannot make sense of,
/// or one that defines this server in a shape the editor does not rewrite (a dotted key,
/// an array of tables, or twice), is reported as a problem and left exactly as it was.
/// Losing the user's Codex configuration is a far worse outcome than not registering.
final class MCPToml {
    /// The Codex table that holds one sub table per MCP server.
    static final String TABLE_NAME = "mcp_servers";

    private static final int KIND_HEADER = 0;
    private static final int KIND_ARRAY_HEADER = 1;
    private static final int KIND_ASSIGNMENT = 2;

    private MCPToml() {
    }

    /// The outcome of an edit: either the new document text, or the reason the document
    /// was left alone.
    static final class Result {
        private final String text;
        private final String problem;

        private Result(String text, String problem) {
            this.text = text;
            this.problem = problem;
        }

        static Result applied(String text) {
            return new Result(text, null);
        }

        static Result refused(String problem) {
            return new Result(null, problem);
        }

        /// True when [#getText()] holds the document to write.
        boolean isApplied() {
            return problem == null;
        }

        /// The new document text, or null when the edit was refused.
        String getText() {
            return text;
        }

        /// Why the document was left untouched, or null when the edit was applied.
        String getProblem() {
            return problem;
        }
    }

    /// Adds, replaces or removes the `[mcp_servers.<serverName>]` table.
    ///
    /// #### Parameters
    ///
    /// - `toml`: the current document, null or empty for a config that does not exist yet
    /// - `serverName`: the server to write or remove
    /// - `descriptor`: the entry to write, or null to remove the entry
    static Result applyServerEntry(String toml, String serverName, MCPClientDescriptor descriptor) {
        if (serverName == null || serverName.length() == 0) {
            return Result.refused("no server name was given");
        }
        String text = toml == null ? "" : toml;
        // A byte order mark is legal at the head of a UTF-8 config and is not part of the
        // TOML grammar, so it is set aside for the walk and put back afterwards.
        String bom = "";
        if (text.length() > 0 && text.charAt(0) == '\uFEFF') {
            bom = text.substring(0, 1);
            text = text.substring(1);
        }
        Walker walker = new Walker(text);
        List<Item> items = walker.walk();
        if (items == null) {
            return Result.refused("it is not valid TOML: " + walker.getError());
        }
        int exactHeaders = 0;
        for (Item item : items) {
            if (item.kind == KIND_ASSIGNMENT) {
                if (item.table.isEmpty() && !item.fullKey.isEmpty()
                        && TABLE_NAME.equals(item.fullKey.get(0))) {
                    // The root table assigns mcp_servers itself, either as a value or
                    // through a dotted key. TOML then forbids a later [mcp_servers.x]
                    // header, so appending one would produce a file Codex cannot read.
                    return Result.refused("'" + TABLE_NAME + "' is declared as a value or with "
                            + "dotted keys, which this editor does not rewrite");
                }
                // An assignment INSIDE the server's own table is replaced wholesale with
                // the rest of it. One that reaches into it from anywhere else declares the
                // server in a shape this editor does not rewrite.
                if (startsWithServer(item.fullKey, serverName)
                        && !startsWithServer(item.table, serverName)) {
                    return Result.refused("the server is declared with a dotted key or an inline "
                            + "table, which this editor does not rewrite");
                }
            } else if (item.kind == KIND_ARRAY_HEADER
                    && isMcpServersRelated(item.fullKey, serverName)) {
                return Result.refused("'" + TABLE_NAME + "' is declared as an array of tables");
            } else if (item.kind == KIND_HEADER && isServer(item.fullKey, serverName)) {
                exactHeaders++;
            }
        }
        if (exactHeaders > 1) {
            // Two [mcp_servers.<name>] headers is not valid TOML in the first place, and
            // guessing which one Codex would honour is not this editor's job.
            return Result.refused("it declares the server more than once");
        }
        // The runs of lines the server owns: each of its tables, and everything up to the
        // next table that is not also the server's. A config written out of order can hold
        // more than one such run.
        List<int[]> regions = new ArrayList<int[]>();
        int i = 0;
        while (i < items.size()) {
            Item item = items.get(i);
            i++;
            if (item.kind != KIND_HEADER || !startsWithServer(item.fullKey, serverName)) {
                continue;
            }
            int runEnd = lastLineOfRun(items, i - 1, serverName);
            regions.add(new int[] {item.lineStart, runEnd});
            while (i < items.size() && items.get(i).lineStart < runEnd) {
                i++;
            }
        }
        String newline = detectNewline(text);
        String block = descriptor == null ? "" : renderBlock(serverName, descriptor, newline);
        String updated;
        if (regions.isEmpty()) {
            if (descriptor == null) {
                // Nothing to remove. Reporting success with an unchanged document lets the
                // caller skip the write entirely.
                updated = text;
            } else {
                updated = append(text, block, newline);
            }
        } else {
            // Copied front to back rather than spliced in place: StringBuilder.replace is
            // a JDK method the Codename One runtime does not have (see vm/JavaAPI and
            // Ports/CLDC11), so core cannot call it however well it compiles on a desktop.
            StringBuilder sb = new StringBuilder();
            int cursor = 0;
            for (int r = 0; r < regions.size(); r++) {
                int[] region = regions.get(r);
                int start = region[0];
                int end = region[1];
                // Only the first run keeps the entry; a later run is a stray table the
                // config should never have had two of.
                boolean removing = descriptor == null || r > 0;
                if (removing) {
                    // Removing has to undo what appending did, or a config that is
                    // registered and unregistered repeatedly grows a blank line each time.
                    // The separator goes with the entry: the one after it when there is
                    // one, otherwise the one before it.
                    int afterBlanks = skipBlankLines(text, end);
                    if (afterBlanks > end) {
                        end = afterBlanks;
                    } else {
                        start = backOverBlankLines(text, start);
                    }
                }
                if (start > cursor) {
                    // Two runs separated by blank lines only: backOverBlankLines can reach
                    // behind the previous run's end, and that text is already consumed.
                    sb.append(text.substring(cursor, start));
                }
                if (!removing) {
                    sb.append(block);
                }
                if (end > cursor) {
                    cursor = end;
                }
            }
            sb.append(text.substring(cursor));
            updated = sb.toString();
        }
        return Result.applied(bom + updated);
    }

    /// The end of the byte range that belongs to the server's tables starting at `found`:
    /// its own lines plus the lines of every sub table such as `.env` that follows it
    /// directly. Blank lines and comments around it belong to the neighbours and are left
    /// where they are.
    private static int lastLineOfRun(List<Item> items, int found, String serverName) {
        int end = items.get(found).end;
        for (int i = found + 1; i < items.size(); i++) {
            Item item = items.get(i);
            if ((item.kind == KIND_HEADER || item.kind == KIND_ARRAY_HEADER)
                    && !startsWithServer(item.fullKey, serverName)) {
                break;
            }
            end = item.end;
        }
        return end;
    }

    private static int skipBlankLines(String text, int from) {
        int end = from;
        while (end < text.length()) {
            int lineEnd = endOfLine(text, end);
            if (!isBlank(text.substring(end, lineEnd))) {
                break;
            }
            end = lineEnd;
        }
        return end;
    }

    private static int backOverBlankLines(String text, int from) {
        int start = from;
        while (start > 0) {
            int previousStart = startOfLine(text, start - 1);
            if (!isBlank(text.substring(previousStart, start))) {
                break;
            }
            start = previousStart;
        }
        return start;
    }

    private static int startOfLine(String text, int from) {
        int start = from;
        while (start > 0 && text.charAt(start - 1) != '\n') {
            start--;
        }
        return start;
    }

    private static int endOfLine(String text, int from) {
        int i = from;
        while (i < text.length() && text.charAt(i) != '\n') {
            i++;
        }
        return i < text.length() ? i + 1 : i;
    }

    private static boolean isBlank(String line) {
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c != ' ' && c != '\t' && c != '\r' && c != '\n') {
                return false;
            }
        }
        return true;
    }

    private static String append(String text, String block, String newline) {
        StringBuilder sb = new StringBuilder(text);
        if (sb.length() > 0) {
            if (sb.charAt(sb.length() - 1) != '\n') {
                sb.append(newline);
            }
            // One blank line between the previous table and ours, unless the document
            // already ends with one.
            if (!endsWithBlankLine(sb)) {
                sb.append(newline);
            }
        }
        sb.append(block);
        return sb.toString();
    }

    private static boolean endsWithBlankLine(StringBuilder sb) {
        int i = sb.length() - 1;
        if (i < 0 || sb.charAt(i) != '\n') {
            return false;
        }
        i--;
        if (i >= 0 && sb.charAt(i) == '\r') {
            i--;
        }
        return i < 0 || sb.charAt(i) == '\n';
    }

    /// Keeps the document's own line ending, so editing a config written on Windows does
    /// not leave one table in LF among CRLF neighbours.
    private static String detectNewline(String text) {
        int nl = text.indexOf('\n');
        if (nl > 0 && text.charAt(nl - 1) == '\r') {
            return "\r\n";
        }
        return "\n";
    }

    private static String renderBlock(String serverName, MCPClientDescriptor descriptor, String nl) {
        String table = TABLE_NAME + "." + renderKey(serverName);
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(table).append(']').append(nl);
        sb.append("command = ").append(renderString(descriptor.getCommand())).append(nl);
        sb.append("args = [");
        List<String> args = descriptor.getArgs();
        if (args != null) {
            for (int i = 0; i < args.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(renderString(args.get(i)));
            }
        }
        sb.append(']').append(nl);
        Map<String, String> env = descriptor.getEnv();
        if (env != null && !env.isEmpty()) {
            sb.append(nl).append('[').append(table).append(".env]").append(nl);
            for (Map.Entry<String, String> entry : env.entrySet()) {
                sb.append(renderKey(entry.getKey())).append(" = ")
                        .append(renderString(entry.getValue())).append(nl);
            }
        }
        return sb.toString();
    }

    /// A bare key where TOML allows one, a quoted key otherwise. A server name carrying a
    /// dot is the case that matters: written bare it would silently become two nested
    /// tables rather than one server.
    static String renderKey(String key) {
        if (key == null || key.length() == 0) {
            return "\"\"";
        }
        for (int i = 0; i < key.length(); i++) {
            if (!isBareKeyChar(key.charAt(i))) {
                return renderString(key);
            }
        }
        return key;
    }

    /// A TOML basic string. Non ASCII characters are emitted as themselves because the
    /// file is written as UTF-8; control characters have no literal form and are escaped.
    static String renderString(String value) {
        String s = value == null ? "" : value;
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') {
                sb.append("\\\"");
            } else if (c == '\\') {
                sb.append("\\\\");
            } else if (c == '\b') {
                sb.append("\\b");
            } else if (c == '\t') {
                sb.append("\\t");
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\f') {
                sb.append("\\f");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c < 0x20 || c == 0x7f) {
                sb.append("\\u").append(hex4(c));
            } else {
                sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    private static String hex4(int c) {
        String digits = "0123456789ABCDEF";
        StringBuilder sb = new StringBuilder();
        sb.append(digits.charAt((c >> 12) & 0xf));
        sb.append(digits.charAt((c >> 8) & 0xf));
        sb.append(digits.charAt((c >> 4) & 0xf));
        sb.append(digits.charAt(c & 0xf));
        return sb.toString();
    }

    private static boolean isBareKeyChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                || c == '_' || c == '-';
    }

    private static boolean isServer(List<String> key, String serverName) {
        return key.size() == 2 && TABLE_NAME.equals(key.get(0)) && serverName.equals(key.get(1));
    }

    private static boolean startsWithServer(List<String> key, String serverName) {
        return key.size() >= 2 && TABLE_NAME.equals(key.get(0)) && serverName.equals(key.get(1));
    }

    /// True for a key that would make `mcp_servers` itself, or this one server, an array
    /// of tables. An unrelated server declared that way is left alone: it does not change
    /// where ours goes.
    private static boolean isMcpServersRelated(List<String> key, String serverName) {
        if (key.isEmpty() || !TABLE_NAME.equals(key.get(0))) {
            return false;
        }
        return key.size() == 1 || serverName.equals(key.get(1));
    }

    /// One table header or one key assignment, with the line range it occupies.
    private static final class Item {
        private int kind;
        /// The key as resolved against the enclosing table.
        private List<String> fullKey;
        /// The table this item sits in, empty for the root table.
        private List<String> table;
        /// Offset of the first character of the item's first line.
        private int lineStart;
        /// Offset just past the newline that ends the item's last line.
        private int end;
    }

    /// Walks the document into a list of items. It reads the grammar's SHAPE - keys,
    /// strings, arrays, inline tables, comments - and deliberately not its values, which
    /// is all that is needed to find and replace one table, and is what lets everything
    /// it does not understand pass through unchanged.
    private static final class Walker {
        private final String s;
        private int i;
        private String error;

        Walker(String s) {
            this.s = s;
        }

        String getError() {
            return error;
        }

        /// Returns the items, or null when the document is not valid TOML.
        List<Item> walk() {
            List<Item> items = new ArrayList<Item>();
            List<String> currentTable = new ArrayList<String>();
            while (true) {
                skipIgnorable();
                if (i >= s.length()) {
                    return items;
                }
                Item item = new Item();
                item.lineStart = lineStartAt(i);
                item.table = currentTable;
                if (s.charAt(i) == '[') {
                    boolean arrayHeader = i + 1 < s.length() && s.charAt(i + 1) == '[';
                    i += arrayHeader ? 2 : 1;
                    List<String> key = readDottedKey(arrayHeader ? "]]" : "]");
                    if (key == null || !finishLine()) {
                        return null;
                    }
                    item.kind = arrayHeader ? KIND_ARRAY_HEADER : KIND_HEADER;
                    item.fullKey = key;
                    currentTable = key;
                } else {
                    List<String> key = readDottedKey("=");
                    if (key == null || !skipValue() || !finishLine()) {
                        return null;
                    }
                    item.kind = KIND_ASSIGNMENT;
                    List<String> full = new ArrayList<String>(currentTable);
                    full.addAll(key);
                    item.fullKey = full;
                }
                item.end = i;
                items.add(item);
            }
        }

        private int lineStartAt(int pos) {
            int start = pos;
            while (start > 0 && s.charAt(start - 1) != '\n') {
                start--;
            }
            return start;
        }

        private void skipIgnorable() {
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                    i++;
                } else if (c == '#') {
                    skipToEndOfLine();
                } else {
                    return;
                }
            }
        }

        private void skipToEndOfLine() {
            while (i < s.length() && s.charAt(i) != '\n') {
                i++;
            }
        }

        private void skipSpaces() {
            while (i < s.length() && (s.charAt(i) == ' ' || s.charAt(i) == '\t')) {
                i++;
            }
        }

        /// Reads a dotted key up to and including `terminator`, which is `=` for an
        /// assignment and `]` or `]]` for a table header. Each part is unquoted, so the
        /// parts compare equal however the user chose to write them.
        private List<String> readDottedKey(String terminator) {
            List<String> parts = new ArrayList<String>();
            boolean more = true;
            while (more) {
                skipSpaces();
                if (i >= s.length()) {
                    error = "a key runs off the end of the file";
                    return null;
                }
                char c = s.charAt(i);
                String part = c == '"' || c == '\'' ? readQuotedKey(c) : readBareKey();
                if (part == null) {
                    return null;
                }
                parts.add(part);
                skipSpaces();
                if (i >= s.length()) {
                    error = "a key runs off the end of the file";
                    return null;
                }
                more = s.charAt(i) == '.';
                if (more) {
                    i++;
                }
            }
            char c = s.charAt(i);
            if ("=".equals(terminator)) {
                if (c == '=') {
                    i++;
                    return parts;
                }
            } else if (c == ']') {
                if (terminator.length() == 1) {
                    i++;
                    return parts;
                }
                if (i + 1 < s.length() && s.charAt(i + 1) == ']') {
                    i += 2;
                    return parts;
                }
                error = "an array of tables header is not closed";
                return null;
            }
            error = "unexpected '" + c + "' in a key";
            return null;
        }

        private String readBareKey() {
            int start = i;
            while (i < s.length() && isBareKeyChar(s.charAt(i))) {
                i++;
            }
            if (i == start) {
                error = i < s.length() ? "unexpected '" + s.charAt(i) + "' where a key was expected"
                        : "a key is missing at the end of the file";
                return null;
            }
            return s.substring(start, i);
        }

        private String readQuotedKey(char quote) {
            int start = i;
            if (!skipString()) {
                return null;
            }
            String raw = s.substring(start + 1, i - 1);
            if (quote == '\'') {
                // A literal key has no escapes at all.
                return raw;
            }
            return unescape(raw);
        }

        /// Resolves the escapes of a basic string. Only the value of a KEY is ever
        /// unescaped, because only keys are compared; every other string is skipped.
        private String unescape(String raw) {
            StringBuilder sb = new StringBuilder();
            int p = 0;
            while (p < raw.length()) {
                char c = raw.charAt(p);
                if (c != '\\') {
                    sb.append(c);
                    p++;
                    continue;
                }
                p++;
                if (p >= raw.length()) {
                    error = "a key ends with an incomplete escape";
                    return null;
                }
                char e = raw.charAt(p);
                p++;
                if (e == 'b') {
                    sb.append('\b');
                } else if (e == 't') {
                    sb.append('\t');
                } else if (e == 'n') {
                    sb.append('\n');
                } else if (e == 'f') {
                    sb.append('\f');
                } else if (e == 'r') {
                    sb.append('\r');
                } else if (e == '"' || e == '\\') {
                    sb.append(e);
                } else if (e == 'u' || e == 'U') {
                    int digits = e == 'u' ? 4 : 8;
                    if (p + digits > raw.length()) {
                        error = "a key has a truncated unicode escape";
                        return null;
                    }
                    int value = 0;
                    for (int k = 0; k < digits; k++) {
                        int d = hexValue(raw.charAt(p + k));
                        if (d < 0) {
                            error = "a key has a malformed unicode escape";
                            return null;
                        }
                        value = (value << 4) + d;
                    }
                    p += digits;
                    appendCodePoint(sb, value);
                } else {
                    error = "a key has an unknown escape";
                    return null;
                }
            }
            return sb.toString();
        }

        private void appendCodePoint(StringBuilder sb, int value) {
            if (value > 0xffff) {
                int v = value - 0x10000;
                sb.append((char) (0xd800 + (v >> 10)));
                sb.append((char) (0xdc00 + (v & 0x3ff)));
            } else {
                sb.append((char) value);
            }
        }

        private int hexValue(char c) {
            if (c >= '0' && c <= '9') {
                return c - '0';
            }
            if (c >= 'a' && c <= 'f') {
                return c - 'a' + 10;
            }
            if (c >= 'A' && c <= 'F') {
                return c - 'A' + 10;
            }
            return -1;
        }

        /// Skips a value of any type without interpreting it.
        private boolean skipValue() {
            skipSpaces();
            if (i >= s.length()) {
                error = "a value is missing at the end of the file";
                return false;
            }
            char c = s.charAt(i);
            if (c == '"' || c == '\'') {
                return skipString();
            }
            if (c == '[' || c == '{') {
                return skipContainer();
            }
            if (c == '\n' || c == '\r') {
                error = "a value is missing";
                return false;
            }
            // A number, boolean or date: it runs to the end of the line or to a comment.
            while (i < s.length() && s.charAt(i) != '\n' && s.charAt(i) != '#') {
                i++;
            }
            return true;
        }

        /// Skips a basic or literal string, single line or multi line, leaving `i` just
        /// past the closing quote.
        private boolean skipString() {
            char quote = s.charAt(i);
            boolean basic = quote == '"';
            if (i + 2 < s.length() && s.charAt(i + 1) == quote && s.charAt(i + 2) == quote) {
                i += 3;
                boolean closed = false;
                while (!closed && i < s.length()) {
                    char c = s.charAt(i);
                    if (basic && c == '\\') {
                        i += 2;
                        continue;
                    }
                    if (c != quote) {
                        i++;
                        continue;
                    }
                    int run = quoteRun(quote);
                    if (run < 3) {
                        // One or two quotes are ordinary content inside a multi line string.
                        i += run;
                    } else {
                        // The delimiter is the LAST three of the run: TOML lets the value
                        // itself end in one or two quotes, so a string opened with three
                        // quotes can close on four or five. A longer run is not legal, so
                        // consume three and let the leftovers be reported rather than
                        // swallowed.
                        i += run <= 5 ? run : 3;
                        closed = true;
                    }
                }
                if (!closed) {
                    error = "a multi line string is not closed";
                }
                return closed;
            }
            i++;
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == '\n') {
                    break;
                }
                if (basic && c == '\\') {
                    i += 2;
                    continue;
                }
                if (c == quote) {
                    i++;
                    return true;
                }
                i++;
            }
            error = "a string is not closed";
            return false;
        }

        /// The number of consecutive `quote` characters starting at the cursor.
        private int quoteRun(char quote) {
            int run = 0;
            while (i + run < s.length() && s.charAt(i + run) == quote) {
                run++;
            }
            return run;
        }

        /// Skips an array or an inline table, including nested ones and any strings or
        /// comments inside them, so a `[` at the head of a line inside an array is never
        /// mistaken for a table header.
        private boolean skipContainer() {
            // A stack of the closers still owed, not a depth count: counting alone accepts
            // `[}` as balanced, and this walk is what decides whether the document is
            // trustworthy enough to edit at all.
            StringBuilder expected = new StringBuilder();
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == '"' || c == '\'') {
                    if (!skipString()) {
                        return false;
                    }
                    continue;
                }
                if (c == '#') {
                    skipToEndOfLine();
                    continue;
                }
                if (c == '[' || c == '{') {
                    expected.append(c == '[' ? ']' : '}');
                    i++;
                    continue;
                }
                if (c == ']' || c == '}') {
                    int last = expected.length() - 1;
                    if (last < 0 || expected.charAt(last) != c) {
                        error = "an array or inline table closes with the wrong delimiter";
                        return false;
                    }
                    expected.deleteCharAt(last);
                    i++;
                    if (expected.length() == 0) {
                        return true;
                    }
                    continue;
                }
                i++;
            }
            error = "an array or inline table is not closed";
            return false;
        }

        /// Consumes the rest of the line, which may hold only spaces and a comment, and
        /// the newline that ends it.
        private boolean finishLine() {
            skipSpaces();
            if (i < s.length() && s.charAt(i) == '#') {
                skipToEndOfLine();
            }
            if (i >= s.length()) {
                return true;
            }
            char c = s.charAt(i);
            if (c == '\r') {
                i++;
                if (i < s.length() && s.charAt(i) == '\n') {
                    i++;
                }
                return true;
            }
            if (c == '\n') {
                i++;
                return true;
            }
            error = "unexpected '" + c + "' at the end of a line";
            return false;
        }
    }
}
