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
package com.codename1.compat.jdk;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// `java.util.Properties` for the Codename One runtime: string keys and
/// values, a second table of defaults behind them, and the `.properties`
/// text format.
///
/// The format is read as the JDK reads it: `#` and `!` comments, `=`, `:` or
/// white space between key and value, a trailing backslash continuing a line
/// (the next line's indentation dropped), and the escapes `\t`, `\n`, `\r`,
/// `\f` and backslash-u followed by four hexadecimal digits. A stream is
/// ISO 8859-1; a reader is whatever its characters are. [#store] writes text
/// that [#load] reads back to the same table, escaping what the format
/// requires and, on a stream, every character outside printable ASCII.
///
/// The XML form (`loadFromXML`, `storeToXML`) and `list` are not provided.
/// The class does not synchronize; see the package description.
public class Properties extends Hashtable<Object, Object> {

    private static final long serialVersionUID = 1L;

    private static final String HEX = "0123456789ABCDEF";

    /// The table consulted for a key this one does not have, or null.
    protected Properties defaults;

    public Properties() {
        this(null);
    }

    public Properties(Properties defaults) {
        this.defaults = defaults;
    }

    public Object setProperty(String key, String value) {
        return put(key, value);
    }

    /// The value for `key` here, else in the defaults, else null. A value
    /// that is not a `String` counts as absent, as in the JDK.
    public String getProperty(String key) {
        Object value = get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return defaults == null ? null : defaults.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value == null ? defaultValue : value;
    }

    /// Reads ISO 8859-1 `.properties` text, adding to what is here. The
    /// stream is left open.
    public void load(InputStream in) throws IOException {
        if (in == null) {
            throw new NullPointerException();
        }
        StringBuilder text = new StringBuilder();
        byte[] buffer = new byte[4096];
        int n = in.read(buffer);
        while (n >= 0) {
            for (int i = 0; i < n; i++) {
                text.append((char) (buffer[i] & 0xff));
            }
            n = in.read(buffer);
        }
        parse(text.toString());
    }

    /// Reads `.properties` text, adding to what is here. The reader is left
    /// open.
    public void load(Reader reader) throws IOException {
        if (reader == null) {
            throw new NullPointerException();
        }
        StringBuilder text = new StringBuilder();
        char[] buffer = new char[4096];
        int n = reader.read(buffer, 0, buffer.length);
        while (n >= 0) {
            text.append(buffer, 0, n);
            n = reader.read(buffer, 0, buffer.length);
        }
        parse(text.toString());
    }

    private void parse(String text) {
        Map<String, String> read = new HashMap<String, String>();
        PropertyResourceBundle.parse(text, read);
        for (Map.Entry<String, String> e : read.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    /// Writes this table (not its defaults) as ISO 8859-1 `.properties`
    /// text: the comment lines, a line with the current date, then one
    /// `key=value` line per entry. The stream is flushed and left open.
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
    public void store(Writer writer, String comments) throws IOException {
        if (writer == null) {
            throw new NullPointerException();
        }
        writer.write(render(comments, false));
        writer.flush();
    }

    private String render(String comments, boolean escapeUnicode) {
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

    /// Every key of this table and of its defaults.
    public Enumeration<?> propertyNames() {
        Map<Object, Object> all = new HashMap<Object, Object>();
        collect(all);
        return Collections.enumeration(new ArrayList<Object>(all.keySet()));
    }

    /// The keys, of this table and of its defaults, whose key and value are
    /// both strings.
    public Set<String> stringPropertyNames() {
        Map<Object, Object> all = new HashMap<Object, Object>();
        collect(all);
        Set<String> names = new HashSet<String>();
        for (Map.Entry<Object, Object> e : all.entrySet()) {
            if (e.getKey() instanceof String && e.getValue() instanceof String) {
                names.add((String) e.getKey());
            }
        }
        return Collections.unmodifiableSet(names);
    }

    /// Defaults first, so that this table's own entries replace them.
    private void collect(Map<Object, Object> into) {
        if (defaults != null) {
            defaults.collect(into);
        }
        List<Map.Entry<Object, Object>> own = new ArrayList<Map.Entry<Object, Object>>(entrySet());
        for (Map.Entry<Object, Object> e : own) {
            into.put(e.getKey(), e.getValue());
        }
    }
}
