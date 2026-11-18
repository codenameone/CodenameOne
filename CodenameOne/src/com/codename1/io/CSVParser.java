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
package com.codename1.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Vector;

/// The `CSVParser` allows importing data into applications quickly from a CSV source.  CSV
/// is a [file format](https://en.wikipedia.org/wiki/Comma-separated_values)
/// that is supported by all spreadsheets and most databases for data exchange.
///
/// The sample below lists a simple hardcoded CSV within the table:
///
/// ```java
/// Form hi = new Form("CSV Parsing", new BorderLayout());
/// CSVParser parser = new CSVParser();
/// try(Reader r = new com.codename1.io.CharArrayReader("1997,Ford,E350,\"Super, \"\"luxurious\"\" truck\"".toCharArray())) {
///     String[][] data = parser.parse(r);
///     String[] columnNames = new String[data[0].length];
///     for(int iter=  0 ; iter < columnNames.length ; iter++) {
///         columnNames[iter] = "Col " + (iter + 1);
///     }
///     TableModel tm = new DefaultTableModel(columnNames, data);
///     hi.add(BorderLayout.CENTER, new Table(tm));
/// } catch(IOException err) {
///     Log.e(err);
/// }
/// hi.show();
/// ```
///
/// A large file does not have to be held in memory as a whole: `parse(Reader, RowHandler)`
/// hands over one row at a time. That method is also the stricter of the two. It keeps
/// empty lines as empty rows and keeps a trailing empty value, where the array returning
/// methods skip both, as they always have.
///
/// Files that spreadsheets save differ in ways a file name does not tell: the separator
/// follows the locale of the machine (a semicolon where the comma is the decimal mark) and
/// "Unicode text" is tab separated UTF-16. `setDetectSeparator(boolean)` picks the separator
/// from the content, and the `InputStream` methods read a UTF-16 byte order mark. A UTF-8 byte
/// order mark is dropped by every method.
public class CSVParser {
    private static final int BOM = 0xFEFF;
    private static final String SEPARATOR_CANDIDATES = ",;\t|";
    private char separatorChar;
    private char quoteChar = '"';
    private boolean detectSeparator;
    private final char[] buffer = new char[8192];
    private Reader currentReader;
    private int bufferSize = -1;
    private int bufferOffset;

    /// Initializes a parser with the default comma (',') separator char
    public CSVParser() {
        this(',');
    }

    /// Allows creating a parser with a custom separator char
    ///
    /// #### Parameters
    ///
    /// - `separatorChar`: custom separator character such as semi-colon (';') etc.
    public CSVParser(char separatorChar) {
        this.separatorChar = separatorChar;
    }

    /// The separator in use. After a parse with `setDetectSeparator(boolean)` on, this is the
    /// separator that was detected.
    ///
    /// #### Returns
    ///
    /// the separator character
    public char getSeparatorChar() {
        return separatorChar;
    }

    /// The character that encloses a value containing a separator or a line break, a double
    /// quote unless changed.
    ///
    /// #### Returns
    ///
    /// the quote character
    public char getQuoteChar() {
        return quoteChar;
    }

    /// Changes the character that encloses a value containing a separator or a line break.
    /// Within such a value the character is written twice to stand for itself.
    ///
    /// #### Parameters
    ///
    /// - `quoteChar`: the quote character, a double quote by default
    public void setQuoteChar(char quoteChar) {
        this.quoteChar = quoteChar;
    }

    /// Whether the separator is picked from the content of each file.
    ///
    /// #### Returns
    ///
    /// true if the separator is detected
    public boolean isDetectSeparator() {
        return detectSeparator;
    }

    /// Picks the separator from the content of each file instead of using the one this parser
    /// was created with. A first line of the form `sep=;`, which spreadsheets write and read,
    /// names the separator outright and is not returned as a row. Otherwise the comma, the
    /// semicolon, the tab and the vertical bar are counted outside quoted values in the first
    /// lines, and the one that splits every line into the same number of values wins. A file
    /// where none of them appears keeps the separator this parser was created with.
    ///
    /// #### Parameters
    ///
    /// - `detectSeparator`: true to detect the separator
    public void setDetectSeparator(boolean detectSeparator) {
        this.detectSeparator = detectSeparator;
    }

    /// Picks the separator of a piece of separated text: the comma, the semicolon, the tab or
    /// the vertical bar, whichever splits its lines most consistently. Quoted values are skipped,
    /// so a comma inside a quoted address does not count.
    ///
    /// #### Parameters
    ///
    /// - `sample`: the beginning of the text, a few lines are enough
    ///
    /// - `fallback`: the answer when none of the candidates appears
    ///
    /// #### Returns
    ///
    /// the separator
    public static char detectSeparator(String sample, char fallback) {
        return detectSeparator(sample, '"', fallback);
    }

    private static char detectSeparator(String sample, char quote, char fallback) {
        int candidates = SEPARATOR_CANDIDATES.length();
        char best = fallback;
        int bestScore = 0;
        for (int c = 0; c < candidates; c++) {
            char candidate = SEPARATOR_CANDIDATES.charAt(c);
            int score = separatorScore(sample, candidate, quote);
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    /// A consistent candidate (the same count on every line) always beats an inconsistent
    /// one, whatever the totals: free text is full of commas, and they are never the same
    /// number on two lines.
    private static int separatorScore(String sample, char candidate, char quote) {
        int len = sample.length();
        int total = 0;
        int lines = 0;
        int lineCount = 0;
        int firstLineCount = -1;
        boolean consistent = true;
        boolean quoted = false;
        boolean lineHasText = false;
        for (int i = 0; i < len && lines < 20; i++) {
            char ch = sample.charAt(i);
            if (ch == quote) {
                quoted = !quoted;
                lineHasText = true;
            } else if (quoted) {
                continue;
            } else if (ch == '\n' || ch == '\r') {
                if (lineHasText) {
                    lines++;
                    if (firstLineCount < 0) {
                        firstLineCount = lineCount;
                    } else if (firstLineCount != lineCount) {
                        consistent = false;
                    }
                }
                lineCount = 0;
                lineHasText = false;
            } else {
                lineHasText = true;
                if (ch == candidate) {
                    lineCount++;
                    total++;
                }
            }
        }
        if (total == 0) {
            return 0;
        }
        // the last line may be cut short by the end of the sample, so it is not held to the count
        if (firstLineCount < 0) {
            firstLineCount = lineCount;
        }
        if (consistent && firstLineCount > 0) {
            return 1000000 + total;
        }
        return total;
    }

    /// Parses input from the given stream and returns the tokens broken into rows and columns
    ///
    /// #### Parameters
    ///
    /// - `r`: the input stream
    ///
    /// #### Returns
    ///
    /// array of rows and columns
    public String[][] parse(InputStream r) throws IOException {
        return parse(openReader(r));
    }

    /// Parses input from the given stream one row at a time, see `parse(Reader, RowHandler)`.
    /// The stream is read as UTF-8 unless it starts with a UTF-16 byte order mark.
    ///
    /// #### Parameters
    ///
    /// - `r`: the input stream, closed when the method returns
    ///
    /// - `handler`: receives the rows
    ///
    /// #### Throws
    ///
    /// - `java.io.IOException`: if the stream cannot be read
    public void parse(InputStream r, RowHandler handler) throws IOException {
        parse(openReader(r), handler);
    }

    /// Opens a stream as text. Spreadsheets save "Unicode text" as UTF-16 with a byte order
    /// mark, and no encoding name passed by the caller could say which of the two orders a
    /// file has, so the mark is read here. UTF-16 is decoded in this class: the encodings a
    /// platform decodes by name are not the same everywhere.
    private static Reader openReader(InputStream in) throws IOException {
        int first = in.read();
        if (first < 0) {
            return Util.getReader(in);
        }
        int second = in.read();
        if (second < 0) {
            return Util.getReader(new PrefixedInputStream(first, -1, in));
        }
        if (first == 0xFF && second == 0xFE) {
            return new Utf16Reader(in, true);
        }
        if (first == 0xFE && second == 0xFF) {
            return new Utf16Reader(in, false);
        }
        return Util.getReader(new PrefixedInputStream(first, second, in));
    }

    /// Parses input from the given stream and returns the tokens broken into rows and columns
    ///
    /// #### Parameters
    ///
    /// - `r`: the input stream
    ///
    /// - `encoding`: the encoding of the stream
    ///
    /// #### Returns
    ///
    /// array of rows and columns
    public String[][] parse(InputStream r, String encoding) throws IOException {
        return parse(new InputStreamReader(r, encoding));
    }

    private int nextChar() throws IOException {
        int response = peekNextChar();
        bufferOffset++;
        return response;
    }

    private int peekNextChar() throws IOException {
        if (bufferOffset >= bufferSize) {
            bufferSize = currentReader.read(buffer);
            if (bufferSize == -1) {
                return -1;
            }
            bufferOffset = 0;
        }
        return buffer[bufferOffset];
    }

    /// Parses input from the given reader and returns the tokens broken into rows and columns
    ///
    /// #### Parameters
    ///
    /// - `r`: the reader stream
    ///
    /// #### Returns
    ///
    /// array of rows and columns
    public String[][] parse(Reader r) throws IOException {
        currentReader = r;
        StringBuilder stringBuf = new StringBuilder();
        boolean isQuoteMode = false;
        Vector returnValue = new Vector();
        Vector currentVector = new Vector();
        returnValue.addElement(currentVector);
        if (detectSeparator) {
            prefill();
        }
        int currentChar = nextChar();
        if (currentChar == BOM) {
            currentChar = nextChar();
        }
        if (detectSeparator && currentChar > -1) {
            // nextChar() already consumed the first character, the detection reads the buffer
            bufferOffset--;
            detectFromBuffer();
            currentChar = nextChar();
        }
        while (currentChar > -1) {
            if (isQuoteMode) {
                if (currentChar == quoteChar) {
                    int next = peekNextChar();
                    if (next == quoteChar) {
                        stringBuf.append(quoteChar);
                        nextChar();
                    } else {
                        isQuoteMode = false;
                    }
                } else {
                    stringBuf.append((char) currentChar);
                }
            } else {
                if (stringBuf.length() == 0) {
                    if (currentChar == quoteChar) {
                        isQuoteMode = true;
                        currentChar = nextChar();
                        continue;
                    }
                }
                if (currentChar == separatorChar) {
                    currentVector.addElement(stringBuf.toString());
                    stringBuf.setLength(0);
                    currentChar = nextChar();
                    continue;
                }
                if (currentChar == 10 || currentChar == 13) {
                    while (currentChar == 10 || currentChar == 13) {
                        currentChar = nextChar();
                    }
                    currentVector.addElement(stringBuf.toString());
                    stringBuf.setLength(0);
                    currentVector = new Vector();
                    returnValue.addElement(currentVector);
                    continue;
                }
                stringBuf.append((char) currentChar);
            }
            currentChar = nextChar();
        }
        if (stringBuf.length() > 0) {
            currentVector.addElement(stringBuf.toString());
        }
        String[][] actualReturnValue = new String[returnValue.size()][];
        int arlen = actualReturnValue.length;
        for (int iter = 0; iter < arlen; iter++) {
            Vector e = (Vector) returnValue.elementAt(iter);
            actualReturnValue[iter] = new String[e.size()];
            int arlen2 = actualReturnValue[iter].length;
            for (int i = 0; i < arlen2; i++) {
                actualReturnValue[iter][i] = (String) e.elementAt(i);
            }
        }
        currentReader.close();
        currentReader = null;
        return actualReturnValue;
    }

    /// Parses input from the given reader one row at a time, without holding the file in
    /// memory.
    ///
    /// This method reads the way a spreadsheet does, which differs from the array returning
    /// methods in three places: an empty line is a row with no values, where they skip it; a
    /// separator at the very end of the file is followed by an empty value, where they drop
    /// it; and a line ending in CR LF ends one row. A quoted value may contain separators and
    /// line breaks, and the quote character written twice inside it stands for itself.
    ///
    /// #### Parameters
    ///
    /// - `r`: the reader, closed when the method returns
    ///
    /// - `handler`: receives the rows
    ///
    /// #### Throws
    ///
    /// - `java.io.IOException`: if an exception is thrown by the reader
    public void parse(Reader r, RowHandler handler) throws IOException {
        currentReader = r;
        try {
            prefill();
            if (peekNextChar() == BOM) {
                nextChar();
            }
            if (detectSeparator) {
                detectFromBuffer();
            }
            ArrayList<String> row = new ArrayList<String>();
            StringBuilder value = new StringBuilder();
            boolean quoted = false;
            // a quoted empty value ("") has no characters, yet it is a value
            boolean valueStarted = false;
            boolean rowStarted = false;
            int index = 0;
            int c = nextChar();
            while (c > -1) {
                if (quoted) {
                    if (c == quoteChar) {
                        if (peekNextChar() == quoteChar) {
                            value.append(quoteChar);
                            nextChar();
                        } else {
                            quoted = false;
                        }
                    } else {
                        value.append((char) c);
                    }
                } else if (c == quoteChar && !valueStarted) {
                    quoted = true;
                    valueStarted = true;
                } else if (c == separatorChar) {
                    row.add(value.toString());
                    value.setLength(0);
                    valueStarted = false;
                    rowStarted = true;
                } else if (c == '\n' || c == '\r') {
                    if (c == '\r' && peekNextChar() == '\n') {
                        nextChar();
                    }
                    if (rowStarted || valueStarted) {
                        row.add(value.toString());
                    }
                    value.setLength(0);
                    valueStarted = false;
                    rowStarted = false;
                    String[] cells = new String[row.size()];
                    row.toArray(cells);
                    row.clear();
                    if (!handler.row(index, cells)) {
                        return;
                    }
                    index++;
                } else {
                    value.append((char) c);
                    valueStarted = true;
                }
                c = nextChar();
            }
            if (rowStarted || valueStarted) {
                row.add(value.toString());
                String[] cells = new String[row.size()];
                row.toArray(cells);
                handler.row(index, cells);
            }
        } finally {
            currentReader = null;
            bufferSize = -1;
            bufferOffset = 0;
            r.close();
        }
    }

    /// Fills the buffer as far as the reader allows, so that separator detection has more than
    /// the few characters a single read may return.
    private void prefill() throws IOException {
        int total = 0;
        while (total < buffer.length) {
            int n = currentReader.read(buffer, total, buffer.length - total);
            if (n <= 0) {
                break;
            }
            total += n;
        }
        bufferSize = total;
        bufferOffset = 0;
    }

    /// Sets the separator from what the buffer holds, starting at the current offset.
    private void detectFromBuffer() {
        int available = bufferSize - bufferOffset;
        if (available <= 0) {
            return;
        }
        StringBuilder sample = new StringBuilder(available);
        sample.append(buffer, bufferOffset, available);
        String text = sample.toString();
        if (text.length() > 5 && text.regionMatches(true, 0, "sep=", 0, 4)) {
            char after = text.charAt(5);
            if (after == '\n' || after == '\r') {
                separatorChar = text.charAt(4);
                int skip = 6;
                if (after == '\r' && text.length() > 6 && text.charAt(6) == '\n') {
                    skip = 7;
                }
                bufferOffset += skip;
                return;
            }
        }
        separatorChar = detectSeparator(text, quoteChar, separatorChar);
    }

    /// Receives the rows of `parse(Reader, RowHandler)` as they are read.
    public interface RowHandler {
        /// Invoked for every row of the file, in order.
        ///
        /// #### Parameters
        ///
        /// - `index`: the zero based index of the row
        ///
        /// - `cells`: the values of the row, an empty array for an empty line
        ///
        /// #### Returns
        ///
        /// true to go on reading, false to stop
        boolean row(int index, String[] cells);
    }

    /// Replays the bytes that were read to look for a byte order mark and were not one.
    private static final class PrefixedInputStream extends InputStream {
        private final InputStream in;
        private int first;
        private int second;

        PrefixedInputStream(int first, int second, InputStream in) {
            this.first = first;
            this.second = second;
            this.in = in;
        }

        @Override
        public int read() throws IOException {
            if (first >= 0) {
                int b = first;
                first = second;
                second = -1;
                return b;
            }
            return in.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (len == 0) {
                return 0;
            }
            if (first >= 0) {
                int count = 0;
                while (first >= 0 && count < len) {
                    b[off + count] = (byte) read();
                    count++;
                }
                return count;
            }
            return in.read(b, off, len);
        }

        @Override
        public void close() throws IOException {
            in.close();
        }
    }

    /// Decodes UTF-16, two bytes to a character. Surrogate pairs need nothing more: each half
    /// is a character of its own in a Java string as well.
    private static final class Utf16Reader extends Reader {
        private final InputStream in;
        private final boolean littleEndian;
        private final byte[] bytes = new byte[4096];
        private int pending = -1;

        Utf16Reader(InputStream in, boolean littleEndian) {
            this.in = in;
            this.littleEndian = littleEndian;
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            if (len == 0) {
                return 0;
            }
            int count = 0;
            while (count == 0) {
                int want = Math.min(bytes.length, len * 2);
                int start = 0;
                if (pending >= 0) {
                    bytes[0] = (byte) pending;
                    pending = -1;
                    start = 1;
                }
                int n = in.read(bytes, start, want - start);
                if (n < 0) {
                    // a single byte left over at the end of the file is not a character
                    return -1;
                }
                int total = start + n;
                int pairs = total / 2;
                for (int i = 0; i < pairs; i++) {
                    int a = bytes[i * 2] & 0xFF;
                    int b = bytes[i * 2 + 1] & 0xFF;
                    cbuf[off + count] = (char) (littleEndian ? (b << 8) | a : (a << 8) | b);
                    count++;
                }
                if ((total & 1) == 1) {
                    pending = bytes[total - 1] & 0xFF;
                }
            }
            return count;
        }

        @Override
        public void close() throws IOException {
            in.close();
        }
    }
}
