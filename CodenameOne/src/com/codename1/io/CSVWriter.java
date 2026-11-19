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
package com.codename1.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;

/// Writes rows of values as CSV, the counterpart of `CSVParser`.
///
/// A value is quoted when it has to be: when it contains the separator or any other common
/// one (comma, semicolon, tab, bar), so a reader that detects the separator can't split it,
/// the quote character or a line break, or begins or ends with a space. The quote character inside a quoted value
/// is written twice.
///
/// ```java
/// CSVWriter csv = new CSVWriter();
/// try (OutputStream out = FileSystemStorage.getInstance().openOutputStream(path)) {
///     csv.write(out, new String[][] {
///         {"Name", "Notes"},
///         {"Doe, John", "said \"hello\""}
///     });
/// }
/// ```
///
/// Spreadsheets open a `.csv` file with the separator of the machine's locale and, without a
/// byte order mark, in a legacy code page. `setWriteByteOrderMark(boolean)` makes them read
/// UTF-8, and `setWriteSeparatorHint(boolean)` writes the `sep=` first line that names the
/// separator. Other consumers of CSV read both as data, which is why neither is on by default.
public class CSVWriter {
    private final char separatorChar;
    private char quoteChar = '"';
    private String lineEnding = "\r\n";
    private boolean quoteAll;
    private boolean escapeFormulas;
    private boolean writeByteOrderMark;
    private boolean writeSeparatorHint;

    /// Creates a writer with the comma separator.
    public CSVWriter() {
        this(',');
    }

    /// Creates a writer with a custom separator.
    ///
    /// #### Parameters
    ///
    /// - `separatorChar`: the separator, a tab for tab separated values
    public CSVWriter(char separatorChar) {
        this.separatorChar = separatorChar;
    }

    /// The separator written between the values of a row.
    ///
    /// #### Returns
    ///
    /// the separator character
    public char getSeparatorChar() {
        return separatorChar;
    }

    /// The character that encloses a quoted value.
    ///
    /// #### Returns
    ///
    /// the quote character
    public char getQuoteChar() {
        return quoteChar;
    }

    /// Changes the character that encloses a quoted value.
    ///
    /// #### Parameters
    ///
    /// - `quoteChar`: the quote character, a double quote by default
    public void setQuoteChar(char quoteChar) {
        this.quoteChar = quoteChar;
    }

    /// The text written after every row.
    ///
    /// #### Returns
    ///
    /// the line ending
    public String getLineEnding() {
        return lineEnding;
    }

    /// Changes the text written after every row. The default is CR LF, which is what the CSV
    /// specification (RFC 4180) asks for and what every reader accepts.
    ///
    /// #### Parameters
    ///
    /// - `lineEnding`: the line ending
    public void setLineEnding(String lineEnding) {
        if (lineEnding == null) {
            throw new IllegalArgumentException("A line ending is needed; \"\\n\" and \"\\r\\n\" are the usual ones");
        }
        this.lineEnding = lineEnding;
    }

    /// Whether every value is quoted.
    ///
    /// #### Returns
    ///
    /// true if every value is quoted
    public boolean isQuoteAll() {
        return quoteAll;
    }

    /// Quotes every value, including those that do not need it.
    ///
    /// #### Parameters
    ///
    /// - `quoteAll`: true to quote every value
    public void setQuoteAll(boolean quoteAll) {
        this.quoteAll = quoteAll;
    }

    /// Whether values a spreadsheet would run as a formula are neutralized.
    ///
    /// #### Returns
    ///
    /// true if such values are prefixed
    public boolean isEscapeFormulas() {
        return escapeFormulas;
    }

    /// Neutralizes values a spreadsheet would run as a formula. A spreadsheet that opens a CSV
    /// file treats a value starting with `=`, `+`, `-` or `@` as a formula, and a formula can
    /// send the rest of the sheet to a web address. When the values come from people other than
    /// the one who opens the file, turn this on: such a value is written with a leading
    /// apostrophe, which a spreadsheet shows as plain text. A value that is a number, such as
    /// `-12.5`, is left as it is.
    ///
    /// #### Parameters
    ///
    /// - `escapeFormulas`: true to prefix such values
    public void setEscapeFormulas(boolean escapeFormulas) {
        this.escapeFormulas = escapeFormulas;
    }

    /// Whether `write(OutputStream, String[][])` starts the file with a byte order mark.
    ///
    /// #### Returns
    ///
    /// true if the mark is written
    public boolean isWriteByteOrderMark() {
        return writeByteOrderMark;
    }

    /// Starts a file written by `write(OutputStream, String[][])` with the UTF-8 byte order
    /// mark. Spreadsheets need it to read anything outside ASCII correctly.
    ///
    /// #### Parameters
    ///
    /// - `writeByteOrderMark`: true to write the mark
    public void setWriteByteOrderMark(boolean writeByteOrderMark) {
        this.writeByteOrderMark = writeByteOrderMark;
    }

    /// Whether a `sep=` line is written before the rows.
    ///
    /// #### Returns
    ///
    /// true if the line is written
    public boolean isWriteSeparatorHint() {
        return writeSeparatorHint;
    }

    /// Writes a first line of the form `sep=,` that tells a spreadsheet which separator the
    /// file uses, whatever the locale of the machine. `CSVParser` reads that line when
    /// separator detection is on.
    ///
    /// #### Parameters
    ///
    /// - `writeSeparatorHint`: true to write the line
    public void setWriteSeparatorHint(boolean writeSeparatorHint) {
        this.writeSeparatorHint = writeSeparatorHint;
    }

    /// Writes rows as UTF-8. The stream is flushed and left open.
    ///
    /// #### Parameters
    ///
    /// - `out`: the destination
    ///
    /// - `rows`: the rows, each an array of values; a null value is written as an empty one
    ///
    /// #### Throws
    ///
    /// - `java.io.IOException`: if the stream cannot be written
    public void write(OutputStream out, String[][] rows) throws IOException {
        if (writeByteOrderMark) {
            out.write(0xEF);
            out.write(0xBB);
            out.write(0xBF);
        }
        Writer w = new OutputStreamWriter(out, "UTF-8");
        write(w, rows);
        w.flush();
    }

    /// Writes rows. The writer is left open.
    ///
    /// #### Parameters
    ///
    /// - `out`: the destination
    ///
    /// - `rows`: the rows, each an array of values; a null value is written as an empty one
    ///
    /// #### Throws
    ///
    /// - `java.io.IOException`: if the writer fails
    public void write(Writer out, String[][] rows) throws IOException {
        writeHeader(out);
        int count = rows.length;
        // CSVParser.parse answers a last row with no cells for a file that ends in a line
        // ending, as most do. Written back it would be a blank line more on every round.
        if (count > 0 && (rows[count - 1] == null || rows[count - 1].length == 0)) {
            count--;
        }
        for (int i = 0; i < count; i++) {
            writeRow(out, rows[i]);
        }
    }

    /// Writes the `sep=` line if `setWriteSeparatorHint(boolean)` asked for one. Call it once,
    /// before the first `writeRow(Writer, String...)`, when writing row by row;
    /// `write(Writer, String[][])` calls it itself.
    ///
    /// #### Parameters
    ///
    /// - `out`: the destination
    ///
    /// #### Throws
    ///
    /// - `java.io.IOException`: if the writer fails
    public void writeHeader(Writer out) throws IOException {
        if (writeSeparatorHint) {
            out.write("sep=");
            out.write(separatorChar);
            out.write(lineEnding);
        }
    }

    /// Writes one row followed by the line ending, for output that is produced row by row.
    ///
    /// #### Parameters
    ///
    /// - `out`: the destination
    ///
    /// - `cells`: the values of the row; a null value is written as an empty one
    ///
    /// #### Throws
    ///
    /// - `java.io.IOException`: if the writer fails
    public void writeRow(Writer out, String... cells) throws IOException {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                line.append(separatorChar);
            }
            appendValue(line, cells[i]);
        }
        // a row holding one empty value would otherwise be an empty line, which reads back
        // as a row with no values at all
        if (cells.length == 1 && line.length() == 0) {
            line.append(quoteChar).append(quoteChar);
        }
        line.append(lineEnding);
        out.write(line.toString());
    }

    /// Returns rows as CSV text.
    ///
    /// #### Parameters
    ///
    /// - `rows`: the rows, each an array of values
    ///
    /// #### Returns
    ///
    /// the text
    public String toString(String[][] rows) {
        StringWriter w = new StringWriter();
        try {
            write(w, rows);
        } catch (IOException err) {
            // a StringWriter does not fail
            throw new IllegalStateException(err.toString(), err);
        }
        return w.toString();
    }

    private void appendValue(StringBuilder line, String cell) {
        if (cell == null) {
            return;
        }
        String value = cell;
        if (escapeFormulas && isFormulaLike(value)) {
            value = "'" + value;
        }
        if (!quoteAll && !needsQuotes(value)) {
            line.append(value);
            return;
        }
        line.append(quoteChar);
        int len = value.length();
        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);
            if (c == quoteChar) {
                line.append(quoteChar);
            }
            line.append(c);
        }
        line.append(quoteChar);
    }

    private boolean needsQuotes(String value) {
        int len = value.length();
        if (len == 0) {
            return false;
        }
        if (value.charAt(0) == ' ' || value.charAt(len - 1) == ' ') {
            return true;
        }
        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);
            // every character a reader might take for the separator is quoted, not only this
            // writer's: a reader that detects the separator would otherwise split the cell
            if (c == separatorChar || c == quoteChar || c == '\n' || c == '\r'
                    || c == ',' || c == ';' || c == '\t' || c == '|') {
                return true;
            }
        }
        return false;
    }

    private static boolean isFormulaLike(String value) {
        if (value.length() == 0) {
            return false;
        }
        char first = value.charAt(0);
        if (first == '=' || first == '@' || first == '\t' || first == '\r') {
            return true;
        }
        if (first != '+' && first != '-') {
            return false;
        }
        // A sign followed by nothing but what a number is written with -- digits, the
        // separators of thousands and decimals, spaces, a percent sign -- is a negative
        // amount, a phone number or a dash, and has nothing to run. Anything else after a
        // sign could be an expression.
        int len = value.length();
        for (int i = 1; i < len; i++) {
            char c = value.charAt(i);
            if ((c == 'e' || c == 'E') && isDigit(value, i - 1)) {
                // the exponent of a number written in scientific notation, -1.5e3 or 2E+10
                int next = i + 1 < len && (value.charAt(i + 1) == '+' || value.charAt(i + 1) == '-') ? i + 2 : i + 1;
                if (isDigit(value, next)) {
                    i = next;
                    continue;
                }
            }
            if ((c < '0' || c > '9') && c != '.' && c != ',' && c != ' ' && c != '%') {
                return true;
            }
        }
        return false;
    }

    private static boolean isDigit(String s, int i) {
        return i >= 0 && i < s.length() && s.charAt(i) >= '0' && s.charAt(i) <= '9';
    }
}
