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
package com.codename1.xml;

import java.util.Hashtable;

/// The character entity tables of the XML and HTML parsers, and the lookups
/// over them.
///
/// It lives here rather than in `com.codename1.ui.html` so that [XMLParser] can
/// decode entities without linking any UI class: the parser is shared with the
/// server runtime. `com.codename1.ui.html.HTMLUtils` delegates to it, so the
/// lenient HTML behaviour is unchanged.
@com.codename1.impl.SharedWithBackend
public final class CharEntities {

    /// The char entities strings supported in XML. When a char entity is found these will be compared against first.
    private static final String[] XML_CHAR_ENTITIES = {
            "lt", // lesser-than
            "gt", // greater-than
            "amp", // ampersand
            "quot", //quotation mark
            "apos", // apostrophe
    };
    /// The numericals value of char entities strings above.
    private static final int[] XML_CHAR_ENTITIES_VALS = {
            60, // "lt", // lesser-than
            62, // "gt", // greater-than
            38, // "amp", // ampersand
            34, // "quot", //quotation mark
            39, // "apos", // apostrophe
    };
    /// This is a list of ISO 8859-1 Symbols that can be used as HTML char entities
    private static final String[] HTML_BASIC_CHAR_ENTITY_STRINGS = {
            "nbsp", "iexcl", "cent", "pound", "curren", "yen", "brvbar", "sect", "uml", "copy", "ordf", "laquo", "not", "shy", "reg", "macr", "deg", "plusmn", "sup2", "sup3", "acute",
            "micro", "para", "middot", "cedil", "sup1", "ordm", "raquo", "frac14", "frac12", "frac34", "iquest", "Agrave", "Aacute", "Acirc", "Atilde", "Auml", "Aring", "AElig",
            "Ccedil", "Egrave", "Eacute", "Ecirc", "Euml", "Igrave", "Iacute", "Icirc", "Iuml", "ETH", "Ntilde", "Ograve", "Oacute", "Ocirc", "Otilde", "Ouml", "times", "Oslash",
            "Ugrave", "Uacute", "Ucirc", "Uuml", "Yacute", "THORN", "szlig", "agrave", "aacute", "acirc", "atilde", "auml", "aring", "aelig", "ccedil", "egrave", "eacute", "ecirc",
            "euml", "igrave", "iacute", "icirc", "iuml", "eth", "ntilde", "ograve", "oacute", "ocirc", "otilde", "ouml", "divide", "oslash", "ugrave", "uacute", "ucirc", "uuml",
            "yacute", "thorn", "yuml"};
    /// Common HTML 4 named entities outside the contiguous ISO-8859-1 range above. Keeping these in
    /// the shared parser utility lets all tolerant HTMLParser clients decode modern pasted fragments
    /// without maintaining their own entity tables.
    private static final String[] HTML_EXTENDED_CHAR_ENTITY_STRINGS = {
            "OElig", "oelig", "Scaron", "scaron", "Yuml", "fnof", "circ", "tilde",
            "ensp", "emsp", "thinsp", "zwnj", "zwj", "lrm", "rlm", "ndash", "mdash",
            "lsquo", "rsquo", "sbquo", "ldquo", "rdquo", "bdquo", "dagger", "Dagger", "bull",
            "hellip", "permil", "prime", "Prime", "lsaquo", "rsaquo", "oline", "frasl", "euro",
            "image", "weierp", "real", "trade", "alefsym"
    };
    private static final int[] HTML_EXTENDED_CHAR_ENTITY_VALUES = {
            338, 339, 352, 353, 376, 402, 710, 732,
            8194, 8195, 8201, 8204, 8205, 8206, 8207, 8211, 8212,
            8216, 8217, 8218, 8220, 8221, 8222, 8224, 8225, 8226,
            8230, 8240, 8242, 8243, 8249, 8250, 8254, 8260, 8364,
            8465, 8472, 8476, 8482, 8501
    };

    private CharEntities() {
    }

    /// Converts a char entity to the matching character, the lenient way the
    /// HTML parser always has: the XML names match ignoring case, and a numeric
    /// reference is truncated to one `char`.
    ///
    /// #### Parameters
    ///
    /// - `charEntity`: The char entity to convert (Not including the & and ;)
    ///
    /// - `lookupHTMLentities`: true to include the basic HTML named char entities (unicode 160-255), false otherwise
    ///
    /// - `userDefinedCharEntities`: A hashtable containing (String,int) denoting the char entity name and its unicode
    ///
    /// #### Returns
    ///
    /// A string containing a single char
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if the entity is unknown
    @SuppressWarnings("PMD.EmptyCatchBlock")
    public static String convert(String charEntity, boolean lookupHTMLentities, Hashtable userDefinedCharEntities) {
        int charCode = -1;
        if (charEntity.startsWith("#")) { //numbered char entity
            if (charEntity.startsWith("#x")) { //hex
                try {
                    charCode = Integer.parseInt(charEntity.substring(2), 16);
                } catch (NumberFormatException nfe) {
                    //if not a number - simply ignore char entity
                }
            } else {
                try {
                    charCode = Integer.parseInt(charEntity.substring(1));
                } catch (NumberFormatException nfe) {
                    //if not a number - simply ignore char entity
                }
            }
        } else { //not numbered, rather a symbol
            charCode = getCharEntityCode(charEntity, lookupHTMLentities, userDefinedCharEntities);
        }

        if (charCode != -1) {
            return String.valueOf((char) charCode);
        }
        throw new IllegalArgumentException("Unknown character entity - " + charEntity);
    }

    /// Decodes a reference the way XML defines it: the five predefined names,
    /// matched exactly, a decimal or hexadecimal character reference to a legal
    /// code point (a supplementary one becomes a surrogate pair), or a name in
    /// `userDefinedCharEntities`. Entities a document declares itself are never
    /// consulted, so nothing a document says can be expanded.
    ///
    /// #### Parameters
    ///
    /// - `ref`: the reference without the & and ;
    ///
    /// - `userDefinedCharEntities`: names the application added, or null
    ///
    /// #### Returns
    ///
    /// the decoded characters, or null when the reference is undefined or invalid
    public static String decodeXml(String ref, Hashtable userDefinedCharEntities) {
        for (int i = 0; i < XML_CHAR_ENTITIES.length; i++) {
            if (XML_CHAR_ENTITIES[i].equals(ref)) {
                return String.valueOf((char) XML_CHAR_ENTITIES_VALS[i]);
            }
        }
        if (ref.length() > 1 && ref.charAt(0) == '#') {
            int code = parseCharRef(ref);
            if (code <= 0 || (code >= 0xd800 && code <= 0xdfff)) {
                return null;
            }
            if (code >= 0x10000) {
                int v = code - 0x10000;
                char[] pair = {(char) (0xd800 + (v >> 10)), (char) (0xdc00 + (v & 0x3ff))};
                return new String(pair);
            }
            return String.valueOf((char) code);
        }
        if (userDefinedCharEntities != null) {
            Object code = userDefinedCharEntities.get(ref);
            if (code instanceof Integer) {
                return String.valueOf((char) ((Integer) code).intValue());
            }
        }
        return null;
    }

    /// The code point of `#123` or `#x7b`, or -1 when it is not one.
    private static int parseCharRef(String ref) {
        boolean hex = ref.length() > 2 && (ref.charAt(1) == 'x' || ref.charAt(1) == 'X');
        int start = hex ? 2 : 1;
        int radix = hex ? 16 : 10;
        int value = 0;
        for (int i = start; i < ref.length(); i++) {
            int digit = Character.digit(ref.charAt(i), radix);
            if (digit < 0) {
                return -1;
            }
            value = value * radix + digit;
            if (value > 0x10ffff) {
                return -1;
            }
        }
        return value;
    }

    private static int getCharEntityCode(String symbol, boolean html, Hashtable userDefined) {
        // First tries the XML basic char entities
        for (int i = 0; i < XML_CHAR_ENTITIES.length; i++) {
            if (symbol.equalsIgnoreCase(XML_CHAR_ENTITIES[i])) {
                return XML_CHAR_ENTITIES_VALS[i];
            }
        }
        if (html) {
            // Not one of the most popular char codes, proceed to check the ISO-8859-1 symbols array
            int val = indexOf(symbol, HTML_BASIC_CHAR_ENTITY_STRINGS);
            if (val != -1) {
                return val + 160;
            }
            val = indexOf(symbol, HTML_EXTENDED_CHAR_ENTITY_STRINGS);
            if (val != -1) {
                return HTML_EXTENDED_CHAR_ENTITY_VALUES[val];
            }
        }

        // Not found in the standard symbol table, see if it is in the user defined symbols table
        if (userDefined != null) {
            Object charObj = userDefined.get(symbol);
            if (charObj instanceof Integer) {
                return ((Integer) charObj).intValue();
            }
        }

        // Not found anywhere
        return -1;
    }

    private static int indexOf(String str, String[] options) {
        for (int i = 0; i < options.length; i++) {
            if (str.equals(options[i])) {
                return i;
            }
        }
        return -1;
    }
}
