/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.codename1.io;

import com.codename1.util.CaseInsensitiveOrder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

/// A `Properties` object is a `Hashtable` where the keys and values
/// must be `String`s. Each property can have a default
/// `Properties` list which specifies the default
/// values to be used when a given key is not found in this `Properties`
/// instance.
///
/// Character Encoding
///
/// Note that in some cases `Properties` uses ISO-8859-1 instead of UTF-8.
/// ISO-8859-1 is only capable of representing a tiny subset of Unicode.
/// Use either the `loadFromXML`/`storeToXML` methods (which use UTF-8 by
/// default) or the `load`/`store` overloads that take
/// an `OutputStreamWriter` (so you can supply a UTF-8 instance) instead.
///
/// #### See also
///
/// - Hashtable
///
/// - java.lang.System#getProperties
public class Properties extends HashMap<String, String> {

    private static final int NONE = 0;
    private static final int SLASH = 1;
    private static final int UNICODE = 2;
    private static final int CONTINUE = 3;
    private static final int KEY_DONE = 4;
    private static final int IGNORE = 5;
    /// The default values for keys not found in this `Properties`
    /// instance.
    protected Properties defaults;

    /// Constructs a new `Properties` object.
    public Properties() {
    }

    /// Constructs a new `Properties` object using the specified default
    /// `Properties`.
    ///
    /// #### Parameters
    ///
    /// - `properties`: the default `Properties`.
    public Properties(Properties properties) {
        defaults = properties;
    }

    private void dumpString(StringBuilder buffer, String string, boolean key) {
        int i = 0;
        if (!key && i < string.length() && string.charAt(i) == ' ') {
            buffer.append("\\ ");
            i++;
        }
        int slen = string.length();
        for (; i < slen; i++) {
            char ch = string.charAt(i);
            switch (ch) {
                case '\t':
                    buffer.append("\\t");
                    break;
                case '\n':
                    buffer.append("\\n");
                    break;
                case '\f':
                    buffer.append("\\f");
                    break;
                case '\r':
                    buffer.append("\\r");
                    break;
                default:
                    if ("\\#!=:".indexOf(ch) >= 0 || (key && ch == ' ')) {
                        buffer.append('\\');
                    }
                    if (ch >= ' ' && ch <= '~') {
                        buffer.append(ch);
                    } else {
                        String hex = Integer.toHexString(ch);
                        buffer.append("\\u");
                        int hlen = hex.length();
                        for (int j = 0; j < 4 - hlen; j++) {
                            buffer.append("0");
                        }
                        buffer.append(hex);
                    }
            }
        }
    }

    /// Searches for the property with the specified name. If the property is not
    /// found, the default `Properties` are checked. If the property is not
    /// found in the default `Properties`, `null` is returned.
    ///
    /// #### Parameters
    ///
    /// - `name`: the name of the property to find.
    ///
    /// #### Returns
    ///
    /// the named property value, or `null` if it can't be found.
    public String getProperty(String name) {
        Object result = super.get(name);
        String property = result instanceof String ? (String) result : null;
        if (property == null && defaults != null) {
            property = defaults.getProperty(name);
        }
        return property;
    }

    /// Searches for the property with the specified name. If the property is not
    /// found, it looks in the default `Properties`. If the property is not
    /// found in the default `Properties`, it returns the specified
    /// default.
    ///
    /// #### Parameters
    ///
    /// - `name`: the name of the property to find.
    ///
    /// - `defaultValue`: the default value.
    ///
    /// #### Returns
    ///
    /// the named property value.
    public String getProperty(String name, String defaultValue) {
        Object result = super.get(name);
        String property = result instanceof String ? (String) result : null;
        if (property == null && defaults != null) {
            property = defaults.getProperty(name);
        }
        if (property == null) {
            return defaultValue;
        }
        return property;
    }

    /// Loads properties from the specified `InputStream`, assumed to be ISO-8859-1.
    /// See "[Character Encoding](#character_encoding)".
    ///
    /// #### Parameters
    ///
    /// - `in`: the `InputStream`
    ///
    /// #### Throws
    ///
    /// - `IOException`
    public synchronized void load(InputStream in) throws IOException {
        if (in == null) {
            throw new NullPointerException("in == null");
        }
        load(new InputStreamReader(in, "UTF-8"));
    }

    /// Loads properties from the specified `Reader`.
    /// The properties file is interpreted according to the following rules:
    ///
    /// - Empty lines are ignored.
    ///
    /// - Lines starting with either a "#" or a "!" are comment lines and are
    /// ignored.
    ///
    /// - A backslash at the end of the line escapes the following newline
    /// character ("\r", "\n", "\r\n"). If there's whitespace after the
    /// backslash it will just escape that whitespace instead of concatenating
    /// the lines. This does not apply to comment lines.
    ///
    /// - A property line consists of the key, the space between the key and
    /// the value, and the value. The key goes up to the first whitespace, "=" or
    /// ":" that is not escaped. The space between the key and the value contains
    /// either one whitespace, one "=" or one ":" and any amount of additional
    /// whitespace before and after that character. The value starts with the
    /// first character after the space between the key and the value.
    ///
    /// - Following escape sequences are recognized: `\ `, `\\`, `\r`, `\n`,
    /// `\!`, `\#`, `\t`, `\b`, `\f`, and `\uffff` (unicode character).
    ///
    /// #### Parameters
    ///
    /// - `in`: the `Reader`
    ///
    /// #### Throws
    ///
    /// - `IOException`
    ///
    /// #### Since
    ///
    /// 1.6
    @SuppressWarnings({"fallthrough", "PMD.SwitchStmtsShouldHaveDefault"})
    public synchronized void load(Reader in) throws IOException {
        if (in == null) {
            throw new NullPointerException("in == null");
        }
        int mode = NONE;
        int unicode = 0;
        int count = 0;
        char nextChar;
        char[] buf = new char[40];
        int offset = 0;
        int keyLength = -1;
        int intVal;
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
                    count++;
                    if (count < 4) {
                        continue;
                    }
                } else {
                    if (count <= 4) {
                        throw new IllegalArgumentException("Invalid Unicode sequence: illegal character");
                    }
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
                    case 'b':
                        nextChar = '\b';
                        break;
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
                            put(temp.substring(0, keyLength), temp
                                    .substring(keyLength));
                        }
                        keyLength = -1;
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
                        if (keyLength == -1) { // if parsing the key
                            mode = NONE;
                            keyLength = offset;
                            continue;
                        }
                        break;
                }
                if (nextChar == ' ' || nextChar == '\n' || nextChar == '\r' || nextChar == '\t') {
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
            throw new IllegalArgumentException("Invalid Unicode sequence: expected format \\uxxxx");
        }
        if (keyLength == -1 && offset > 0) {
            keyLength = offset;
        }
        if (keyLength >= 0) {
            String temp = new String(buf, 0, offset);
            String key = temp.substring(0, keyLength);
            String value = temp.substring(keyLength);
            if (mode == SLASH) {
                value += "\u0000";
            }
            put(key, value);
        }
    }

    /// Returns all of the property names (keys) in this `Properties` object.
    public Enumeration<?> propertyNames() {
        Hashtable<Object, Object> selected = new Hashtable<Object, Object>();
        selectProperties(selected, false);
        return selected.keys();
    }

    /// Returns those property names (keys) in this `Properties` object for which
    /// both key and value are strings.
    ///
    /// #### Returns
    ///
    /// a set of keys in the property list
    ///
    /// #### Since
    ///
    /// 1.6
    public Set<String> stringPropertyNames() {
        Hashtable<String, Object> stringProperties = new Hashtable<String, Object>();
        selectProperties(stringProperties, true);
        return Collections.unmodifiableSet(stringProperties.keySet());
    }

    private <K> void selectProperties(Hashtable<K, Object> selectProperties, final boolean isStringOnly) {
        if (defaults != null) {
            defaults.selectProperties(selectProperties, isStringOnly);
        }
        for (Map.Entry<String, String> entry : entrySet()) {
            @SuppressWarnings("unchecked")
            K key = (K) entry.getKey();
            if (isStringOnly && !(key instanceof String)) {
                // Only select property with string key and value
                continue;
            }
            Object value = entry.getValue();
            selectProperties.put(key, value);
        }
    }

    /// Saves the mappings in this `Properties` to the specified `OutputStream`, putting the specified comment at the beginning. The output
    /// from this method is suitable for being read by the
    /// `#load(InputStream)` method.
    ///
    /// #### Parameters
    ///
    /// - `out`: the `OutputStream` to write to.
    ///
    /// - `comment`: the comment to add at the beginning.
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: @throws ClassCastException if the key or value of a mapping is not a
    ///                            String.
    ///
    /// #### Deprecated
    ///
    /// @deprecated This method ignores any `IOException` thrown while
    /// writing -- use `#store` instead for better exception
    /// handling.
    @Deprecated
    @SuppressWarnings("PMD.EmptyCatchBlock")
    public void save(OutputStream out, String comment) {
        try {
            store(out, comment);
        } catch (IOException e) {
        }
    }

    /// Maps the specified key to the specified value. If the key already exists,
    /// the old value is replaced. The key and value cannot be `null`.
    ///
    /// #### Parameters
    ///
    /// - `name`: the key.
    ///
    /// - `value`: the value.
    ///
    /// #### Returns
    ///
    /// the old value mapped to the key, or `null`.
    public Object setProperty(String name, String value) {
        return put(name, value);
    }

    /// Stores properties to the specified `OutputStream`, using ISO-8859-1.
    /// See "[Character Encoding](#character_encoding)".
    ///
    /// #### Parameters
    ///
    /// - `out`: the `OutputStream`
    ///
    /// - `comment`: an optional comment to be written, or null
    ///
    /// #### Throws
    ///
    /// - `IOException`
    ///
    /// - `ClassCastException`: if a key or value is not a string
    public synchronized void store(OutputStream out, String comment) throws IOException {
        store(new OutputStreamWriter(out, "UTF-8"), comment);
    }

    /// Stores the mappings in this `Properties` object to `out`,
    /// putting the specified comment at the beginning.
    ///
    /// #### Parameters
    ///
    /// - `writer`: the `Writer`
    ///
    /// - `comment`: an optional comment to be written, or null
    ///
    /// #### Throws
    ///
    /// - `IOException`
    ///
    /// - `ClassCastException`: if a key or value is not a string
    ///
    /// #### Since
    ///
    /// 1.6
    public synchronized void store(Writer writer, String comment) throws IOException {
        if (comment != null && comment.length() > 0) {
            writer.write("#");
            writer.write(comment);
            writer.write("\n");
            writer.write("#");
            writer.write(new Date().toString());
            writer.write("\n");
        }

        StringBuilder sb = new StringBuilder(200);
        ArrayList<String> k = new ArrayList<String>(keySet());
        Collections.sort(k, new CaseInsensitiveOrder());
        for (String key : k) {
            dumpString(sb, key, true);
            sb.append('=');
            dumpString(sb, get(key), false);
            sb.append("\n");
            writer.write(sb.toString());
            sb.setLength(0);
        }
        writer.flush();
    }
}
