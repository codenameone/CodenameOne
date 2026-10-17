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
package java.nio.charset;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/// A named character encoding. This runtime has the three charsets every
/// Java platform must: UTF-8, US-ASCII and ISO-8859-1, under their canonical
/// names and the JDK's aliases. Encoding and decoding go through the
/// String constructors and `getBytes` that take a Charset, and the readers
/// and writers of `java.io`.
public class Charset implements Comparable<Charset> {

    private static final String[] UTF8_ALIASES = {"unicode-1-1-utf-8", "UTF8"};
    private static final String[] USASCII_ALIASES = {
        "iso-ir-6", "ANSI_X3.4-1986", "ISO_646.irv:1991", "ASCII", "iso_646.irv:1983", "ANSI_X3.4-1968",
        "646", "default", "cp367", "csASCII", "us", "ISO646-US", "IBM367", "ascii7"
    };
    private static final String[] ISO88591_ALIASES = {
        "819", "ISO8859-1", "l1", "ISO_8859-1:1987", "ISO_8859-1", "8859_1", "iso-ir-100", "latin1",
        "cp819", "ISO8859_1", "IBM819", "ISO_8859_1", "IBM-819", "csISOLatin1"
    };
    private static final Charset UTF8 = new SimpleCharset("UTF-8", UTF8_ALIASES);
    private static final Charset USASCII = new SimpleCharset("US-ASCII", USASCII_ALIASES);
    private static final Charset ISO88591 = new SimpleCharset("ISO-8859-1", ISO88591_ALIASES);

    private final String name;
    private final String[] aliases;

    protected Charset(String canonicalName, String[] aliases) {
        checkName(canonicalName);
        name = canonicalName;
        this.aliases = aliases == null ? new String[0] : aliases;
    }

    /// Compares canonical names, ignoring case.
    public final int compareTo(Charset another) {
        return name.compareToIgnoreCase(another.name);
    }

    /// The canonical name.
    public final String name() {
        return name;
    }

    /// The canonical name.
    public String displayName() {
        return name;
    }

    /// The canonical name, whatever the locale.
    public String displayName(Locale locale) {
        return name;
    }

    /// The aliases of the charset.
    public final Set<String> aliases() {
        HashSet<String> out = new HashSet<String>();
        for (int i = 0; i < aliases.length; i++) {
            out.add(aliases[i]);
        }
        return Collections.unmodifiableSet(out);
    }

    /// Whether the charset is registered with IANA; the three this runtime
    /// has are.
    public final boolean isRegistered() {
        return true;
    }

    /// Whether the charset can encode; the three this runtime has can.
    public boolean canEncode() {
        return true;
    }

    /// Whether `obj` is a charset of the same canonical name.
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Charset)) {
            return false;
        }
        return name.equals(((Charset) obj).name);
    }

    /// The hash of the canonical name.
    public final int hashCode() {
        return name.hashCode();
    }

    /// The canonical name.
    public final String toString() {
        return name;
    }

    /// UTF-8, the default charset of this runtime on every platform.
    public static Charset defaultCharset() {
        return UTF8;
    }

    /// Whether `charsetName` names a charset this runtime has.
    ///
    /// #### Throws
    ///
    /// - `IllegalCharsetNameException`: if the name is not a legal charset name
    /// - `IllegalArgumentException`: if it is null
    public static boolean isSupported(String charsetName) {
        return lookup(charsetName) != null;
    }

    /// The charset `charsetName` names, by canonical name or alias, in any
    /// case.
    ///
    /// #### Throws
    ///
    /// - `IllegalCharsetNameException`: if the name is not a legal charset name
    /// - `UnsupportedCharsetException`: if this runtime has no such charset
    /// - `IllegalArgumentException`: if it is null
    public static Charset forName(String charsetName) {
        Charset cs = lookup(charsetName);
        if (cs == null) {
            throw new UnsupportedCharsetException(charsetName);
        }
        return cs;
    }

    /// The charset `charsetName` names, or `fallback` when the name is
    /// illegal or names a charset this runtime does not have.
    public static Charset forName(String charsetName, Charset fallback) {
        try {
            Charset cs = lookup(charsetName);
            return cs == null ? fallback : cs;
        } catch (IllegalCharsetNameException e) {
            return fallback;
        }
    }

    private static Charset lookup(String charsetName) {
        if (charsetName == null) {
            throw new IllegalArgumentException("Null charset name");
        }
        checkName(charsetName);
        if (matches(UTF8, charsetName)) {
            return UTF8;
        }
        if (matches(ISO88591, charsetName)) {
            return ISO88591;
        }
        if (matches(USASCII, charsetName)) {
            return USASCII;
        }
        return null;
    }

    // Charset names are ASCII; equalsIgnoreCase folds them without the
    // device's locale.
    private static boolean matches(Charset cs, String charsetName) {
        if (cs.name.equalsIgnoreCase(charsetName)) {
            return true;
        }
        for (int i = 0; i < cs.aliases.length; i++) {
            if (cs.aliases[i].equalsIgnoreCase(charsetName)) {
                return true;
            }
        }
        return false;
    }

    private static void checkName(String s) {
        int n = s.length();
        if (n == 0) {
            throw new IllegalCharsetNameException(s);
        }
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            boolean ok = c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9'
                    || i > 0 && (c == '-' || c == '+' || c == ':' || c == '_' || c == '.');
            if (!ok) {
                throw new IllegalCharsetNameException(s);
            }
        }
    }

    private static final class SimpleCharset extends Charset {
        private SimpleCharset(String canonicalName, String[] aliases) {
            super(canonicalName, aliases);
        }
    }
}
