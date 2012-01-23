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

import java.util.Hashtable;

/**
 * This class contains several useful static methods for HTML
 *
 * @author Ofir Leitner
 */
public class HTMLUtils {

    // Prevents instantiation - this class has static method only
    private HTMLUtils() {

    }

    /**
     * The char entities strings supported in XML. When a char entity is found these will be compared against first.
     */
    private static final String[] XML_CHAR_ENTITIES = {
                                                          "lt", // lesser-than
                                                          "gt", // greater-than
                                                          "amp", // ampersand
                                                          "quot", //quotation mark
                                                          "apos", // apostrophe
                                                          //"bull", //bullet
                                                          //"euro" //euro
                                                            };

    /**
     * The numericals value of char entities strings above.
     */
    private static final int[] XML_CHAR_ENTITIES_VALS = {
                                                            60, // "lt", // lesser-than
                                                            62, // "gt", // greater-than
                                                            38, // "amp", // ampersand
                                                            34, // "quot", //quotation mark
                                                            39, // "apos", // apostrophe
                                                            //8226, // "bull", //bullet
                                                            //8364 // "euro"}; //euro
                                                            };

   /**
    * This is a list of ISO 8859-1 Symbols that can be used as HTML char entities
    */
    private static final String[] HTML_BASIC_CHAR_ENTITY_STRINGS = {
        "nbsp","iexcl","cent","pound","curren","yen","brvbar","sect","uml","copy","ordf","laquo","not","shy","reg","macr","deg","plusmn","sup2","sup3","acute",
        "micro","para","middot","cedil","sup1","ordm","raquo","frac14","frac12","frac34","iquest","Agrave","Aacute","Acirc","Atilde","Auml","Aring","AElig",
        "Ccedil","Egrave","Eacute","Ecirc","Euml","Igrave","Iacute","Icirc","Iuml","ETH","Ntilde","Ograve","Oacute","Ocirc","Otilde","Ouml","times","Oslash",
        "Ugrave","Uacute","Ucirc","Uuml","Yacute","THORN","szlig","agrave","aacute","acirc","atilde","auml","aring","aelig","ccedil","egrave","eacute","ecirc",
        "euml","igrave","iacute","icirc","iuml","eth","ntilde","ograve","oacute","ocirc","otilde","ouml","divide","oslash","ugrave","uacute","ucirc","uuml",
        "yacute","thorn","yuml"};


    /**
     * Converts an XML char entity to the matching character or string.
     * This is a convenience method that uses convertCharEntity with false for lookupHTMLentities and a null userDefinedCharEntities
     *
     * @param charEntity The char entity to convert (Not including the & and ;)
     * @return A string containing a single char, or the original char entity string (with & and ;) if the char entity couldn't be resolved
     */
    public static String convertXMLCharEntity(String charEntity) {
        return convertCharEntity(charEntity, false, null);
    }

    /**
     * Converts an HTML char entity to the matching character or string.
     * This is a convenience method that uses convertCharEntity with true for lookupHTMLentities and a null userDefinedCharEntities
     *
     * @param charEntity The char entity to convert (Not including the & and ;)
     * @return A string containing a single char, or the original char entity string (with & and ;) if the char entity couldn't be resolved
     */
    public static String convertHTMLCharEntity(String charEntity) {
        return convertCharEntity(charEntity, true, null);
    }


    /**
     * Converts a char entity to the matching character or string.
     * This handles both numbered and symbol char entities (The latter is done via getCharEntityCode)
     *
     * @param charEntity The char entity to convert (Not including the & and ;)
      * @param lookupHTMLentities true to include the basic HTML named char entities (unicode 160-255), false otherwise
      * @param userDefinedCharEntities A hashtable containing (String,int) dentoing the char entity name and its unicode
     * @return A string containing a single char, or the original char entity string (with & and ;) if the char entity couldn't be resolved
      */
    public static String convertCharEntity(String charEntity,boolean lookupHTMLentities,Hashtable userDefinedCharEntities) {
        int charCode=-1;
        if (charEntity.startsWith("#")) { //numbered char entity
            if (charEntity.startsWith("#x")) { //hex
                try {
                    charCode=Integer.parseInt(charEntity.substring(2),16);
                } catch (NumberFormatException nfe) {
                    //if not a number - simply ignore char entity
                }
            } else {
                try {
                    charCode=Integer.parseInt(charEntity.substring(1));
                } catch (NumberFormatException nfe) {
                    //if not a number - simply ignore char entity
                }
            }
        } else { //not numbered, rather a symbol
            charCode=getCharEntityCode(charEntity,lookupHTMLentities,userDefinedCharEntities);
        }

        if (charCode!=-1) {
            return ""+(char)charCode;
        } else {
            throw new IllegalArgumentException("Unknown character entity - "+charEntity);
            //notifyError(ParserCallback.ERROR_UNRECOGNIZED_CHAR_ENTITY,null,null,null, "Unrecognized char entity: "+charEntity);
            //return "&"+charEntity+";"; // Another option is to return an empty string, but returning the entity will unravel bugs and will also allow ignoring common mistakes such as using the & char (instead of &apos;)
        }

    }

    private static int getCharEntityCode(String symbol,boolean html,Hashtable userDefined) {
        // First tries the XML basic char entities
        int val=-1;
        for (int i=0;i<XML_CHAR_ENTITIES.length;i++) {
            if (symbol.equalsIgnoreCase(XML_CHAR_ENTITIES[i])) {
                return XML_CHAR_ENTITIES_VALS[i];
            }
        }
        if (val!=-1) {
            return val;
        } else {
            if (html) {
                // Not one of the most popular char codes, proceed to check the ISO-8859-1 symbols array
                val=getStringVal(symbol, HTML_BASIC_CHAR_ENTITY_STRINGS);
                if (val!=-1) {
                    return val+160;
                }
           }

            // Not found in the standard symbol table, see if it is in the user defined symbols table
            if (userDefined!=null) {
                Object charObj=userDefined.get(symbol);
                if (charObj!=null) {
                    return ((Integer)charObj).intValue();
                }
            }

            // Not found anywhere
            return -1;
        }
    }

    /**
     * Encodes the specified string to "percent-encoding" or URL encoding.
     * This encodes reserved, unsafe and unicode characters
     *
     * @param str The string to be encoded
     * @return A percent-encoding of the string (safe characters remain the same)
     */
    public static String encodeString(String str) {
        if (str==null) {
            return "";
        }
        String encodedStr="";
        for(int i=0;i<str.length();i++) {
            char c=str.charAt(i);
            if (
                // Checks for unreserved characters that RFC 3986 defines that shouldn't be encoded
                ((c>='a') && (c<='z')) || ((c>='A') && (c<='Z')) ||
                ((c>='0') && (c<='9')) ||
                (c=='-') || (c=='.') || (c=='_') || (c=='~'))

            {
                encodedStr+=c;
            } else if ((c>=0x80) && (c<=0xffff)) { // UTF encoding - See http://en.wikipedia.org/wiki/UTF-8
                int firstLiteral = c/256;
                int secLiteral = c%256;
                if (c<=0x07ff) { // 2 literals unicode
                    firstLiteral=192+(firstLiteral<<2)+(secLiteral>>6);
                    secLiteral=128+(secLiteral & 63);
                    encodedStr+="%"+Integer.toHexString(firstLiteral).toUpperCase()+"%"+Integer.toHexString(secLiteral).toUpperCase();
                } else { // 3 literals unicode
                    int thirdLiteral=128+(secLiteral & 63);
                    secLiteral=128+((firstLiteral%16)<<2)+(secLiteral>>6);
                    firstLiteral=224+(firstLiteral>>4);
                    encodedStr+="%"+Integer.toHexString(firstLiteral).toUpperCase()+"%"+Integer.toHexString(secLiteral).toUpperCase()
                            +"%"+Integer.toHexString(thirdLiteral).toUpperCase();
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
                String prefix="%";
                if (c<16) {
                    prefix+="0"; //For a value lesser than 16, we'd like to get %0F and not %F
                }
                encodedStr+=prefix+Integer.toHexString((int)c).toUpperCase();

            }
        }
        return encodedStr;
    }


    /**
     * Matches the given string to the given options and returns the matching value, or -1 if none found.
     *
     * @param str The string to compare
     * @param options The options to match the string against
     * @return The appropriate matching value: If the string equals (case ignored) to the option in the X position of the options array, the int X will be returned. If the string didn't match any of the options -1 is returned.
     */
    static int getStringVal(String str,String[] options) {
        return getStringVal(str, options, null, -1);
    }

    /**
     * Matches the given string to the given options and returns the matching value, or -1 if none found.
     *
     * @param str The string to compare
     * @param options The options to match the string against
     * @param vals The values to match to each option (According to the position in the array), this can be null.
     * @return The appropriate matching value: If the string equals (case ignored) to the option in the X position of the options array, this returns the value in the X position of the vals array, or simply X if vals is null. If the string didn't match any of the options -1 is returned.
     */
    static int getStringVal(String str,String[] options,int[] vals) {
        return getStringVal(str, options, vals, -1);
    }

    /**
     * Matches the given string to the given options and returns the matching value, or the default one if none found.
     *
     * @param str The string to compare
     * @param options The options to match the string against
     * @param defaultValue The default value to return if the string was null or not found among the options
     * @return The appropriate matching value: If the string equals (case ignored) to the option in the X position of the options array, the int X will be returned. If the string didn't match any of the options the defaultValue is returned.
     */
    static int getStringVal(String str,String[] options,int defaultValue) {
        return getStringVal(str, options, null, defaultValue);
    }

    /**
     * Matches the given string to the given options and returns the matching value, or the default one if none found.
     *
     * @param str The string to compare
     * @param options The options to match the string against
     * @param vals The values to match to each option (According to the position in the array), this can be null.
     * @param defaultValue The default value to return if the string was null or not found among the options
     * @return The appropriate matching value: If the string equals (case ignored) to the option in the X position of the options array, this returns the value in the X position of the vals array, or simply X if vals is null. If the string didn't match any of the options the defaultValue is returned.
     */
    static int getStringVal(String str,String[] options,int[] vals,int defaultValue) {
        if (str!=null) {
            for(int i=0;i<options.length;i++) {
                if (str.equalsIgnoreCase(options[i])) {
                    if (vals!=null) {
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
     