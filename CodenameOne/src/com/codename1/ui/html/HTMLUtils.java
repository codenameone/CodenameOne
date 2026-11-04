/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui.html;

import com.codename1.xml.CharEntities;
import java.util.Hashtable;

/// This class contains several useful static methods for HTML
///
/// #### Deprecated
///
/// the HTML package is no longer used or maintained and may be removed in a future revision
public final class HTMLUtils {

    // Prevents instantiation - this class has static method only
    private HTMLUtils() {

    }

    /// Converts an XML char entity to the matching character or string.
    /// This is a convenience method that uses convertCharEntity with false for lookupHTMLentities and a null userDefinedCharEntities
    ///
    /// #### Parameters
    ///
    /// - `charEntity`: The char entity to convert (Not including the & and ;)
    ///
    /// #### Returns
    ///
    /// A string containing a single char, or the original char entity string  (with & and ;) if the char entity couldn't be resolved
    public static String convertXMLCharEntity(String charEntity) {
        return convertCharEntity(charEntity, false, null);
    }

    /// Converts an HTML char entity to the matching character or string.
    /// This is a convenience method that uses convertCharEntity with true for lookupHTMLentities and a null userDefinedCharEntities
    ///
    /// #### Parameters
    ///
    /// - `charEntity`: The char entity to convert (Not including the & and ;)
    ///
    /// #### Returns
    ///
    /// A string containing a single char, or the original char entity string  (with & and ;) if the char entity couldn't be resolved
    public static String convertHTMLCharEntity(String charEntity) {
        return convertCharEntity(charEntity, true, null);
    }


    /// Converts a char entity to the matching character or string.
    /// This handles both numbered and symbol char entities (The latter is done via getCharEntityCode)
    ///
    /// #### Parameters
    ///
    /// - `charEntity`: The char entity to convert (Not including the & and ;)
    ///
    /// - `lookupHTMLentities`: true to include the basic HTML named char entities (unicode 160-255), false otherwise
    ///
    /// - `userDefinedCharEntities`: A hashtable containing (String,int) dentoing the char entity name and its unicode
    ///
    /// #### Returns
    ///
    /// A string containing a single char, or the original char entity string  (with & and ;) if the char entity couldn't be resolved
    public static String convertCharEntity(String charEntity, boolean lookupHTMLentities, Hashtable userDefinedCharEntities) {
        return CharEntities.convert(charEntity, lookupHTMLentities, userDefinedCharEntities);
    }

    /// Encodes the specified string to "percent-encoding" or URL encoding.
    /// This encodes reserved, unsafe and unicode characters
    ///
    /// #### Parameters
    ///
    /// - `str`: The string to be encoded
    ///
    /// #### Returns
    ///
    /// A percent-encoding of the string (safe characters remain the same)
    public static String encodeString(String str) {
        if (str == null) {
            return "";
        }
        String encodedStr = "";
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (
                // Checks for unreserved characters that RFC 3986 defines that shouldn't be encoded
                    ((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z')) ||
                            ((c >= '0') && (c <= '9')) ||
                            (c == '-') || (c == '.') || (c == '_') || (c == '~')) {
                encodedStr += c;
            } else if (c >= 0x80) { // UTF encoding - See http://en.wikipedia.org/wiki/UTF-8
                int firstLiteral = c / 256;
                int secLiteral = c % 256;
                if (c <= 0x07ff) { // 2 literals unicode
                    firstLiteral = 192 + (firstLiteral << 2) + (secLiteral >> 6);
                    secLiteral = 128 + (secLiteral & 63);
                    encodedStr += "%" + Integer.toHexString(firstLiteral).toUpperCase() + "%" + Integer.toHexString(secLiteral).toUpperCase();
                } else { // 3 literals unicode
                    int thirdLiteral = 128 + (secLiteral & 63);
                    secLiteral = 128 + ((firstLiteral % 16) << 2) + (secLiteral >> 6);
                    firstLiteral = 224 + (firstLiteral >> 4);
                    encodedStr += "%" + Integer.toHexString(firstLiteral).toUpperCase() + "%" + Integer.toHexString(secLiteral).toUpperCase()
                            + "%" + Integer.toHexString(thirdLiteral).toUpperCase();
                }
//            The max value of a char is 0xffff, so though URL encoding supports values bigger than that, we can't provide for it
            /*} else if (c>0xffff) { // 4 literals unicode (CJK upper ranges and others)
                int z=c/65536;
                int y=c%65536;
                int x=y%256;
                y=y/256;
                int[] literal=new int[4];
                literal[0]=240+(z>>2);
                literal[1]=128+((z%4)<<4)+(y>>4);
                literal[2]=128+((y%16)<<2)+(x>>6);
                literal[3]=128+(x & 63);
                for(int l=0;l<literal.length;l++) {
                    encodedStr+="%"+Integer.toHexString(literal[l]).toUpperCase();
                }*/
            } else {
                String prefix = "%";
                if (c < 16) {
                    prefix += "0"; //For a value lesser than 16, we'd like to get %0F and not %F
                }
                encodedStr += prefix + Integer.toHexString(c).toUpperCase();

            }
        }
        return encodedStr;
    }


    /// Matches the given string to the given options and returns the matching value, or -1 if none found.
    ///
    /// #### Parameters
    ///
    /// - `str`: The string to compare
    ///
    /// - `options`: The options to match the string against
    ///
    /// #### Returns
    ///
    /// The appropriate matching value: If the string equals (case ignored) to the option in the X position of the options array, the int X will be returned. If the string didn't match any of the options -1 is returned.
    static int getStringVal(String str, String[] options) {
        return getStringVal(str, options, null, -1);
    }

    /// Matches the given string to the given options and returns the matching value, or -1 if none found.
    ///
    /// #### Parameters
    ///
    /// - `str`: The string to compare
    ///
    /// - `options`: The options to match the string against
    ///
    /// - `vals`: The values to match to each option (According to the position in the array), this can be null.
    ///
    /// #### Returns
    ///
    /// The appropriate matching value: If the string equals (case ignored) to the option in the X position of the options array, this returns the value in the X position of the vals array, or simply X if vals is null. If the string didn't match any of the options -1 is returned.
    static int getStringVal(String str, String[] options, int[] vals) {
        return getStringVal(str, options, vals, -1);
    }

    /// Matches the given string to the given options and returns the matching value, or the default one if none found.
    ///
    /// #### Parameters
    ///
    /// - `str`: The string to compare
    ///
    /// - `options`: The options to match the string against
    ///
    /// - `defaultValue`: The default value to return if the string was null or not found among the options
    ///
    /// #### Returns
    ///
    /// The appropriate matching value: If the string equals (case ignored) to the option in the X position of the options array, the int X will be returned. If the string didn't match any of the options the defaultValue is returned.
    static int getStringVal(String str, String[] options, int defaultValue) {
        return getStringVal(str, options, null, defaultValue);
    }

    /// Matches the given string to the given options and returns the matching value, or the default one if none found.
    ///
    /// #### Parameters
    ///
    /// - `str`: The string to compare
    ///
    /// - `options`: The options to match the string against
    ///
    /// - `vals`: The values to match to each option (According to the position in the array), this can be null.
    ///
    /// - `defaultValue`: The default value to return if the string was null or not found among the options
    ///
    /// #### Returns
    ///
    /// The appropriate matching value: If the string equals (case ignored) to the option in the X position of the options array, this returns the value in the X position of the vals array, or simply X if vals is null. If the string didn't match any of the options the defaultValue is returned.
    static int getStringVal(String str, String[] options, int[] vals, int defaultValue) {
        if (str != null) {
            for (int i = 0; i < options.length; i++) {
                if (str.equals(options[i])) {
                    if (vals != null) {
                        return vals[i];
                    } else {
                        return i;
                    }
                }
            }
        }
        return defaultValue;
    }


}
     
