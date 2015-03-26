/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codename1.util.regex;

/**
 *
 * @author Nikolay Neizvesny
 */
public class RECharacter {

    public static final byte UNASSIGNED = 0;
    public static final byte UPPERCASE_LETTER = 1;
    public static final byte LOWERCASE_LETTER = 2;
    public static final byte TITLECASE_LETTER = 3;
    public static final byte MODIFIER_LETTER = 4;
    public static final byte OTHER_LETTER = 5;
    public static final byte NON_SPACING_MARK = 6;
    public static final byte ENCLOSING_MARK = 7;
    public static final byte COMBINING_SPACING_MARK = 8;
    public static final byte DECIMAL_DIGIT_NUMBER = 9;
    public static final byte LETTER_NUMBER = 10;
    public static final byte OTHER_NUMBER = 11;
    public static final byte SPACE_SEPARATOR = 12;
    public static final byte LINE_SEPARATOR = 13;
    public static final byte PARAGRAPH_SEPARATOR = 14;
    public static final byte CONTROL = 15;
    public static final byte FORMAT = 16;
    public static final byte PRIVATE_USE = 18;
    public static final byte SURROGATE = 19;
    public static final byte DASH_PUNCTUATION = 20;
    public static final byte START_PUNCTUATION = 21;
    public static final byte END_PUNCTUATION = 22;
    public static final byte CONNECTOR_PUNCTUATION = 23;
    public static final byte OTHER_PUNCTUATION = 24;
    public static final byte MATH_SYMBOL = 25;
    public static final byte CURRENCY_SYMBOL = 26;
    public static final byte MODIFIER_SYMBOL = 27;
    public static final byte OTHER_SYMBOL = 28;
    
    public static char toLowerCase(char c) {
//#ifdef RE_UNICODE
//#         for (int i = 0; i < CharacterCaseMap.CAPITAL_TO_SMALL_INDEX.length; i++) {
//#             int index = CharacterCaseMap.CAPITAL_TO_SMALL_INDEX[i];
//#             if (c < index) return c;
//#             if (c >= index + CharacterCaseMap.CAPITAL_TO_SMALL_CHAR_MAP[i].length)
//#                 continue;
//#             return CharacterCaseMap.CAPITAL_TO_SMALL_CHAR_MAP[i][c - index];
//#         }
//#         return c;
//#else
        return Character.toLowerCase(c);
//#endif
    }

    public static char toUpperCase(char c) {
//#ifdef RE_UNICODE
//#         for (int i = 0; i < CharacterCaseMap.SMALL_TO_CAPITAL_INDEX.length; i++) {
//#             int index = CharacterCaseMap.SMALL_TO_CAPITAL_INDEX[i];
//#             if (c < index) return c;
//#             if (c >= index + CharacterCaseMap.SMALL_TO_CAPITAL_CHAR_MAP[i].length)
//#                 continue;
//#             return CharacterCaseMap.SMALL_TO_CAPITAL_CHAR_MAP[i][c - index];
//#         }
//#         return c;
//#else
        return Character.toUpperCase(c);
//#endif
    }

    public static boolean isWhitespace(char c) {
        byte type = getType(c);
        return ((type == SPACE_SEPARATOR || type == LINE_SEPARATOR ||
                type == PARAGRAPH_SEPARATOR) && !(c == 0x00A0 || c == 0x2007 ||
                c == 0x202F)) || c == 0x0009 || c == 0x000A || c == 0x000B ||
                c == 0x000C || c == 0x000D || c == 0x0009 || c == 0x001C ||
                c == 0x001D || c == 0x001E || c == 0x001F;
    }

    public static boolean isDigit(char c) {
        byte type = getType(c);
        return type == DECIMAL_DIGIT_NUMBER;
    }

    public static boolean isLetter(char c) {
        byte type = getType(c);
        return type == LOWERCASE_LETTER || type == UPPERCASE_LETTER ||
                type == TITLECASE_LETTER || type == OTHER_LETTER;
    }

    public static boolean isLetterOrDigit(char c) {
        return RECharacter.isDigit(c) || RECharacter.isLetter(c);
    }

    public static boolean isSpaceChar(char c) {
        byte type = getType(c);
        return type == SPACE_SEPARATOR || type == LINE_SEPARATOR ||
                type == PARAGRAPH_SEPARATOR;
    }

    public static boolean isJavaIdentifierStart(char c) {
        byte type = getType(c);
        return isLetter(c) || type == LETTER_NUMBER || c == '$' || c == '_';
    }

    public static boolean isJavaIdentifierPart(char c) {
        return isJavaIdentifierStart(c) || Character.isDigit(c);
    }

    public static byte getType(char c) {
//#ifdef RE_UNICODE
//#         for (int i = 0; i < CHAR_CLASSES_SPACE_INDEX.length; i++) {
//#             int spaceIndex = CHAR_CLASSES_SPACE_INDEX[i];
//#             if (c - spaceIndex < 0) {
//#                 return UNASSIGNED;
//#             }
//#             if (c - spaceIndex < CHAR_CLASSES[i].length) {
//#                 return CHAR_CLASSES[i][c - spaceIndex];
//#             }
//#         }
//#else
        if (c < CHAR_CLASSES.length) {
            return CHAR_CLASSES[c];
        }
//#endif
        return UNASSIGNED;
    }

//#ifdef RE_UNICODE
//#     private static final char[] CHAR_CLASSES_SPACE_INDEX = {
//#         0, 1329, 2305, 3713, 3840, 4096, 4256, 6400, 6912, 7424, 7678, 8400, 9216,
//#         9280, 9312, 9985, 11264, 11776, 11904, 12272, 12784, 19893, 40891, 40960,
//#         42752, 43008, 43072, 44032, 55203, 55296, 56191, 56319, 57343, 63743,
//#         64256, 64467, 65008
//#     };
//#     private static byte[][] CHAR_CLASSES;
//# 
//#     static {
//#         CHAR_CLASSES = new byte[CHAR_CLASSES_SPACE_INDEX.length][];
//#         CHAR_CLASSES[0] = CharacterClassMap1.CHAR_CLASSES_0;
//#         CHAR_CLASSES[1] = CharacterClassMap1.CHAR_CLASSES_1;
//#         CHAR_CLASSES[2] = CharacterClassMap1.CHAR_CLASSES_2;
//#         CHAR_CLASSES[3] = CharacterClassMap1.CHAR_CLASSES_3;
//#         CHAR_CLASSES[4] = CharacterClassMap1.CHAR_CLASSES_4;
//#         CHAR_CLASSES[5] = CharacterClassMap1.CHAR_CLASSES_5;
//#         CHAR_CLASSES[6] = CharacterClassMap2.CHAR_CLASSES_6;
//#         CHAR_CLASSES[7] = CharacterClassMap2.CHAR_CLASSES_7;
//#         CHAR_CLASSES[8] = CharacterClassMap2.CHAR_CLASSES_8;
//#         CHAR_CLASSES[9] = CharacterClassMap2.CHAR_CLASSES_9;
//#         CHAR_CLASSES[10] = CharacterClassMap2.CHAR_CLASSES_10;
//#         CHAR_CLASSES[11] = CharacterClassMap2.CHAR_CLASSES_11;
//#         CHAR_CLASSES[12] = CharacterClassMap2.CHAR_CLASSES_12;
//#         CHAR_CLASSES[13] = CharacterClassMap2.CHAR_CLASSES_13;
//#         CHAR_CLASSES[14] = CharacterClassMap2.CHAR_CLASSES_14;
//#         CHAR_CLASSES[15] = CharacterClassMap3.CHAR_CLASSES_15;
//#         CHAR_CLASSES[16] = CharacterClassMap3.CHAR_CLASSES_16;
//#         CHAR_CLASSES[17] = CharacterClassMap3.CHAR_CLASSES_17;
//#         CHAR_CLASSES[18] = CharacterClassMap3.CHAR_CLASSES_18;
//#         CHAR_CLASSES[19] = CharacterClassMap3.CHAR_CLASSES_19;
//#         CHAR_CLASSES[20] = CharacterClassMap3.CHAR_CLASSES_20;
//#         CHAR_CLASSES[21] = CharacterClassMap3.CHAR_CLASSES_21;
//#         CHAR_CLASSES[22] = CharacterClassMap3.CHAR_CLASSES_22;
//#         CHAR_CLASSES[23] = CharacterClassMap3.CHAR_CLASSES_23;
//#         CHAR_CLASSES[24] = CharacterClassMap3.CHAR_CLASSES_24;
//#         CHAR_CLASSES[25] = CharacterClassMap3.CHAR_CLASSES_25;
//#         CHAR_CLASSES[26] = CharacterClassMap3.CHAR_CLASSES_26;
//#         CHAR_CLASSES[27] = CharacterClassMap3.CHAR_CLASSES_27;
//#         CHAR_CLASSES[28] = CharacterClassMap3.CHAR_CLASSES_28;
//#         CHAR_CLASSES[29] = CharacterClassMap3.CHAR_CLASSES_29;
//#         CHAR_CLASSES[30] = CharacterClassMap3.CHAR_CLASSES_30;
//#         CHAR_CLASSES[31] = CharacterClassMap3.CHAR_CLASSES_31;
//#         CHAR_CLASSES[32] = CharacterClassMap3.CHAR_CLASSES_32;
//#         CHAR_CLASSES[33] = CharacterClassMap3.CHAR_CLASSES_33;
//#         CHAR_CLASSES[34] = CharacterClassMap3.CHAR_CLASSES_34;
//#         CHAR_CLASSES[35] = CharacterClassMap4.CHAR_CLASSES_35;
//#         CHAR_CLASSES[36] = CharacterClassMap4.CHAR_CLASSES_36;
//#     }
//#else
    static final byte[] CHAR_CLASSES = {
        15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15,
        15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 12, 23, 23, 23,
        25, 23, 23, 23, 20, 21, 23, 24, 23, 19, 23, 23, 9, 9, 9, 9, 9, 9, 9, 9, 9,
        9, 23, 23, 24, 24, 24, 23, 23, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 20, 23, 21, 26, 22, 26, 2, 2, 2, 2, 2,
        2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 20, 24, 21,
        24, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15,
        15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 12, 23,
        25, 25, 25, 25, 27, 27, 26, 27, 2, 28, 24, 16, 27, 26, 27, 24, 11, 11, 26,
        2, 27, 23, 26, 11, 2, 29, 11, 11, 11, 23, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 24, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2,
        2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 24, 2, 2, 2,
        2, 2, 2, 2, 2
    };
//#endif

}
