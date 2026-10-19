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
package java.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

/// String keys and values, a second table of defaults behind them, and the
/// `.properties` text format.
///
/// The format is read as the JDK reads it: `#` and `!` comments, `=`, `:` or
/// white space between key and value, a trailing backslash continuing a line
/// (the next line's indentation dropped), and the escapes `\t`, `\n`, `\r`,
/// `\f` and backslash-u followed by four hexadecimal digits; a backslash
/// before any other character stands for that character. A stream is
/// ISO 8859-1; a reader is whatever its characters are. [#store] writes text
/// that [#load] reads back to the same table, escaping what the format
/// requires and, on a stream, every character outside printable ASCII.
///
/// The XML form (`loadFromXML`, `storeToXML`) and `list` are not provided.
public class Properties extends Hashtable<Object, Object> {

    private static final long serialVersionUID = 4112578634029874840L;

    private static final String HEX = "0123456789ABCDEF";

    /// The table consulted for a key this one does not have, or null.
    protected Properties defaults;

    private static final int NONE = 0, SLASH = 1, UNICODE = 2, CONTINUE = 3,
            KEY_DONE = 4, IGNORE = 5;

    /// An empty table with no defaults.
    public Properties() {
    }

    /// An empty table with `defaults` behind it.
    public Properties(Properties defaults) {
        this.defaults = defaults;
    }

    /// The value for `name` here, else in the defaults, else null. A value
    /// that is not a `String` counts as absent, as in the JDK.
    public String getProperty(String name) {
        Object result = get(name);
        if (result instanceof String) {
            return (String) result;
        }
        return defaults == null ? null : defaults.getProperty(name);
    }

    /// The value for `name` as [#getProperty(String)] finds it, or
    /// `defaultValue` when there is none.
    public String getProperty(String name, String defaultValue) {
        String property = getProperty(name);
        return property == null ? defaultValue : property;
    }

    /// Maps `name` to `value` in this table, answering the value it replaced.
    public Object setProperty(String name, String value) {
        return put(name, value);
    }

    /// Reads ISO 8859-1 `.properties` text, adding to what is here. The
    /// stream is left open.
    public synchronized void load(InputStream in) throws IOException {
        if (in == null) {
            throw new NullPointerException("in == null");
        }
        load(new Latin1Reader(in));
    }

    /// Reads `.properties` text, adding to what is here. The reader is left
    /// open.
    @SuppressWarnings("fallthrough")
    public synchronized void load(Reader in) throws IOException {
        if (in == null) {
            throw new NullPointerException("in == null");
        }
        int mode = NONE, unicode = 0, count = 0;
        char nextChar, buf[] = new char[40];
        int offset = 0, keyLength = -1, intVal;
        // Whether this entry's = or : has been read: a key ended by blanks
        // and a continuation still takes one on the next line.
        boolean separated = false;
        boolean firstChar = true;

        Reader br = in;

        while (true) {
            intVal = br.read();
            if (intVal == -1) {
                break;
            }
            nextChar = (char) intVal;

            if (offset == buf.length) {
                char[] newBuf = new char[buf.length * 2];
                System.arraycopy(buf, 0, newBuf, 0, offset);
                buf = newBuf;
            }
            if (mode == UNICODE) {
                int digit = Character.digit(nextChar, 16);
                if (digit >= 0) {
                    unicode = (unicode << 4) + digit;
                    if (++count < 4) {
                        continue;
                    }
                } else if (count <= 4) {
                    throw new IllegalArgumentException("Malformed \\uxxxx encoding.");
                }
                mode = NONE;
                buf[offset++] = (char) unicode;
                if (nextChar != '\n') {
                    continue;
                }
            }
            if (mode == SLASH) {
                mode = NONE;
                switch (nextChar) {
                case '\r':
                    mode = CONTINUE; // Look for a following \n
                    continue;
                case '\n':
                    mode = IGNORE; // Ignore whitespace on the next line
                    continue;
                case 'f':
                    nextChar = '\f';
                    break;
                case 'n':
                    nextChar = '\n';
                    break;
                case 'r':
                    nextChar = '\r';
                    break;
                case 't':
                    nextChar = '\t';
                    break;
                case 'u':
                    mode = UNICODE;
                    unicode = count = 0;
                    continue;
                default:
                    // Any other escaped character is that character, `\b`
                    // included: the JDK has no backspace escape.
                    break;
                }
            } else {
                switch (nextChar) {
                case '#':
                case '!':
                    if (firstChar) {
                        while (true) {
                            intVal = br.read();
                            if (intVal == -1) {
                                break;
                            }
                            nextChar = (char) intVal;
                            if (nextChar == '\r' || nextChar == '\n') {
                                break;
                            }
                        }
                        continue;
                    }
                    break;
                case '\n':
                    if (mode == CONTINUE) { // Part of a \r\n sequence
                        mode = IGNORE; // Ignore whitespace on the next line
                        continue;
                    }
                    // fall into the next case
                case '\r':
                    mode = NONE;
                    firstChar = true;
                    if (offset > 0 || (offset == 0 && keyLength == 0)) {
                        if (keyLength == -1) {
                            keyLength = offset;
                        }
                        String temp = new String(buf, 0, offset);
                        put(temp.substring(0, keyLength), temp.substring(keyLength));
                    }
                    keyLength = -1;
                    separated = false;
                    offset = 0;
                    continue;
                case '\\':
                    if (mode == KEY_DONE) {
                        keyLength = offset;
                    }
                    mode = SLASH;
                    continue;
                case ':':
                case '=':
                    if (keyLength == -1 || (!separated && offset == keyLength)) { // if parsing the key
                        mode = NONE;
                        keyLength = offset;
                        separated = true;
                        // Past the separator nothing starts a comment, not even
                        // for an empty key ("=#token" is the value "#token").
                        firstChar = false;
                        continue;
                    }
                    break;
                default:
                    break;
                }
                if (nextChar == ' ' || nextChar == '\n' || nextChar == '\r' || nextChar == '\t'
                        || nextChar == '\f') {
                    if (mode == CONTINUE) {
                        mode = IGNORE;
                    }
                    // if key length == 0 or value length == 0
                    if (offset == 0 || offset == keyLength || mode == IGNORE) {
                        continue;
                    }
                    if (keyLength == -1) { // if parsing the key
                        mode = KEY_DONE;
                        continue;
                    }
                }
                if (mode == IGNORE || mode == CONTINUE) {
                    mode = NONE;
                }
            }
            firstChar = false;
            if (mode == KEY_DONE) {
                keyLength = offset;
                mode = NONE;
            }
            buf[offset++] = nextChar;
        }
        if (mode == UNICODE && count <= 4) {
            throw new IllegalArgumentException("Malformed \\uxxxx encoding.");
        }
        if (keyLength == -1 && offset > 0) {
            keyLength = offset;
        }
        if (keyLength >= 0) {
            String temp = new String(buf, 0, offset);
            put(temp.substring(0, keyLength), temp.substring(keyLength));
        }
    }

    /// Every key of this table and of its defaults.
    public Enumeration<?> propertyNames() {
        Hashtable<Object, Object> all = new Hashtable<Object, Object>();
        collect(all, false);
        return all.keys();
    }

    /// The keys, of this table and of its defaults, whose key and value are
    /// both strings.
    public Set<String> stringPropertyNames() {
        Hashtable<Object, Object> all = new Hashtable<Object, Object>();
        collect(all, true);
        Set<String> names = new HashSet<String>();
        for (Object key : all.keySet()) {
            if (key instanceof String) {
                names.add((String) key);
            }
        }
        return Collections.unmodifiableSet(names);
    }

    /// Defaults first, so that this table's own entries replace them.
    private void collect(Hashtable<Object, Object> into, boolean stringsOnly) {
        if (defaults != null) {
            defaults.collect(into, stringsOnly);
        }
        for (Map.Entry<Object, Object> e : entrySet()) {
            Object key = e.getKey();
            Object value = e.getValue();
            if (stringsOnly && !(key instanceof String && value instanceof String)) {
                continue;
            }
            into.put(key, value);
        }
    }

    /// Writes this table as [#store(OutputStream, String)] does, ignoring any
    /// `IOException`.
    ///
    /// #### Deprecated
    ///
    /// An error writing is lost; use [#store(OutputStream, String)].
    @Deprecated
    public void save(OutputStream out, String comments) {
        try {
            store(out, comments);
        } catch (IOException e) {
            // As documented: save reports nothing.
        }
    }

    /// Writes this table (not its defaults) as ISO 8859-1 `.properties`
    /// text: the comment lines, a line with the current date, then one
    /// `key=value` line per entry. The stream is flushed and left open.
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: if a key or value is not a `String`
    public void store(OutputStream out, String comments) throws IOException {
        if (out == null) {
            throw new NullPointerException();
        }
        String text = render(comments, true);
        byte[] bytes = new byte[text.length()];
        for (int i = 0; i < bytes.length; i++) {
            // Everything outside ASCII was escaped by render.
            bytes[i] = (byte) text.charAt(i);
        }
        out.write(bytes);
        out.flush();
    }

    /// Writes this table as `.properties` text, leaving characters outside
    /// ASCII as they are. The writer is flushed and left open.
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: if a key or value is not a `String`
    public void store(Writer writer, String comments) throws IOException {
        if (writer == null) {
            throw new NullPointerException();
        }
        writer.write(render(comments, false));
        writer.flush();
    }

    private synchronized String render(String comments, boolean escapeUnicode) {
        StringBuilder out = new StringBuilder();
        if (comments != null) {
            writeComments(out, comments, escapeUnicode);
        }
        out.append('#').append(new Date().toString()).append('\n');
        for (Map.Entry<Object, Object> e : entrySet()) {
            Object key = e.getKey();
            Object value = e.getValue();
            if (!(key instanceof String) || !(value instanceof String)) {
                // The JDK fails here with a ClassCastException, which a
                // device cannot be relied on to raise from a cast.
                throw new ClassCastException("Properties holds a key or value that is not a String");
            }
            escape(out, (String) key, true, escapeUnicode);
            out.append('=');
            escape(out, (String) value, false, escapeUnicode);
            out.append('\n');
        }
        return out.toString();
    }

    /// Every line of `comments` as a comment line: a line that does not
    /// start one already gets `#`.
    private static void writeComments(StringBuilder out, String comments, boolean escapeUnicode) {
        out.append('#');
        int n = comments.length();
        for (int i = 0; i < n; i++) {
            char c = comments.charAt(i);
            if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < n && comments.charAt(i + 1) == '\n') {
                    i++;
                }
                out.append('\n');
                if (i + 1 >= n || (comments.charAt(i + 1) != '#' && comments.charAt(i + 1) != '!')) {
                    out.append('#');
                }
            } else if (escapeUnicode && c > 0xff) {
                unicode(out, c);
            } else {
                out.append(c);
            }
        }
        out.append('\n');
    }

    private static void unicode(StringBuilder out, char c) {
        out.append('\\').append('u');
        out.append(HEX.charAt((c >> 12) & 0xf)).append(HEX.charAt((c >> 8) & 0xf));
        out.append(HEX.charAt((c >> 4) & 0xf)).append(HEX.charAt(c & 0xf));
    }

    private static void escape(StringBuilder out, String s, boolean key, boolean escapeUnicode) {
        int n = s.length();
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            switch (c) {
                case ' ':
                    // Only a leading space of a value would be dropped on
                    // reading; in a key every space ends the key.
                    if (key || i == 0) {
                        out.append('\\');
                    }
                    out.append(' ');
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\f':
                    out.append("\\f");
                    break;
                case '=':
                case ':':
                case '#':
                case '!':
                case '\\':
                    out.append('\\').append(c);
                    break;
                default:
                    if (escapeUnicode && (c < 0x20 || c > 0x7e)) {
                        unicode(out, c);
                    } else {
                        out.append(c);
                    }
            }
        }
    }

    /// A stream's bytes as ISO 8859-1 characters, one for one.
    private static final class Latin1Reader extends Reader {
        private final InputStream in;

        Latin1Reader(InputStream in) {
            this.in = in;
        }

        @Override
        public int read() throws IOException {
            return in.read();
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            int n = 0;
            while (n < len) {
                int b = in.read();
                if (b < 0) {
                    return n == 0 ? -1 : n;
                }
                cbuf[off + n++] = (char) b;
            }
            return n;
        }

        @Override
        public void close() throws IOException {
            // The caller's stream stays open.
        }
    }
}
