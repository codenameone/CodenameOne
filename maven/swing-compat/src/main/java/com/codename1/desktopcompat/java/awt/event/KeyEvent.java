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
package com.codename1.desktopcompat.java.awt.event;

import com.codename1.desktopcompat.java.awt.Component;

/// A keyboard event: a key was pressed, released or typed.
///
/// Key texts are English; the JDK looks them up in a localized resource
/// bundle. The `FocusEvent` style cause and the input-method related members
/// of the JDK class are not provided.
public class KeyEvent extends InputEvent {

    public static final int KEY_FIRST = 400;

    public static final int KEY_LAST = 402;

    public static final int KEY_TYPED = 400;

    public static final int KEY_PRESSED = 401;

    public static final int KEY_RELEASED = 402;

    public static final char CHAR_UNDEFINED = 0xFFFF;

    public static final int KEY_LOCATION_UNKNOWN = 0;

    public static final int KEY_LOCATION_STANDARD = 1;

    public static final int KEY_LOCATION_LEFT = 2;

    public static final int KEY_LOCATION_RIGHT = 3;

    public static final int KEY_LOCATION_NUMPAD = 4;

    private static final int EXTENDED_FLAG = 0x01000000;

    public static final int VK_UNDEFINED = 0x0;

    public static final int VK_CANCEL = 0x3;

    public static final int VK_BACK_SPACE = 0x8;

    public static final int VK_TAB = 0x9;

    public static final int VK_ENTER = 0xA;

    public static final int VK_CLEAR = 0xC;

    public static final int VK_SHIFT = 0x10;

    public static final int VK_CONTROL = 0x11;

    public static final int VK_ALT = 0x12;

    public static final int VK_PAUSE = 0x13;

    public static final int VK_CAPS_LOCK = 0x14;

    public static final int VK_KANA = 0x15;

    public static final int VK_FINAL = 0x18;

    public static final int VK_KANJI = 0x19;

    public static final int VK_ESCAPE = 0x1B;

    public static final int VK_CONVERT = 0x1C;

    public static final int VK_NONCONVERT = 0x1D;

    public static final int VK_ACCEPT = 0x1E;

    public static final int VK_MODECHANGE = 0x1F;

    public static final int VK_SPACE = 0x20;

    public static final int VK_PAGE_UP = 0x21;

    public static final int VK_PAGE_DOWN = 0x22;

    public static final int VK_END = 0x23;

    public static final int VK_HOME = 0x24;

    public static final int VK_LEFT = 0x25;

    public static final int VK_UP = 0x26;

    public static final int VK_RIGHT = 0x27;

    public static final int VK_DOWN = 0x28;

    public static final int VK_COMMA = 0x2C;

    public static final int VK_MINUS = 0x2D;

    public static final int VK_PERIOD = 0x2E;

    public static final int VK_SLASH = 0x2F;

    public static final int VK_0 = 0x30;

    public static final int VK_1 = 0x31;

    public static final int VK_2 = 0x32;

    public static final int VK_3 = 0x33;

    public static final int VK_4 = 0x34;

    public static final int VK_5 = 0x35;

    public static final int VK_6 = 0x36;

    public static final int VK_7 = 0x37;

    public static final int VK_8 = 0x38;

    public static final int VK_9 = 0x39;

    public static final int VK_SEMICOLON = 0x3B;

    public static final int VK_EQUALS = 0x3D;

    public static final int VK_A = 0x41;

    public static final int VK_B = 0x42;

    public static final int VK_C = 0x43;

    public static final int VK_D = 0x44;

    public static final int VK_E = 0x45;

    public static final int VK_F = 0x46;

    public static final int VK_G = 0x47;

    public static final int VK_H = 0x48;

    public static final int VK_I = 0x49;

    public static final int VK_J = 0x4A;

    public static final int VK_K = 0x4B;

    public static final int VK_L = 0x4C;

    public static final int VK_M = 0x4D;

    public static final int VK_N = 0x4E;

    public static final int VK_O = 0x4F;

    public static final int VK_P = 0x50;

    public static final int VK_Q = 0x51;

    public static final int VK_R = 0x52;

    public static final int VK_S = 0x53;

    public static final int VK_T = 0x54;

    public static final int VK_U = 0x55;

    public static final int VK_V = 0x56;

    public static final int VK_W = 0x57;

    public static final int VK_X = 0x58;

    public static final int VK_Y = 0x59;

    public static final int VK_Z = 0x5A;

    public static final int VK_OPEN_BRACKET = 0x5B;

    public static final int VK_BACK_SLASH = 0x5C;

    public static final int VK_CLOSE_BRACKET = 0x5D;

    public static final int VK_NUMPAD0 = 0x60;

    public static final int VK_NUMPAD1 = 0x61;

    public static final int VK_NUMPAD2 = 0x62;

    public static final int VK_NUMPAD3 = 0x63;

    public static final int VK_NUMPAD4 = 0x64;

    public static final int VK_NUMPAD5 = 0x65;

    public static final int VK_NUMPAD6 = 0x66;

    public static final int VK_NUMPAD7 = 0x67;

    public static final int VK_NUMPAD8 = 0x68;

    public static final int VK_NUMPAD9 = 0x69;

    public static final int VK_MULTIPLY = 0x6A;

    public static final int VK_ADD = 0x6B;

    public static final int VK_SEPARATER = 0x6C;

    public static final int VK_SEPARATOR = 0x6C;

    public static final int VK_SUBTRACT = 0x6D;

    public static final int VK_DECIMAL = 0x6E;

    public static final int VK_DIVIDE = 0x6F;

    public static final int VK_F1 = 0x70;

    public static final int VK_F2 = 0x71;

    public static final int VK_F3 = 0x72;

    public static final int VK_F4 = 0x73;

    public static final int VK_F5 = 0x74;

    public static final int VK_F6 = 0x75;

    public static final int VK_F7 = 0x76;

    public static final int VK_F8 = 0x77;

    public static final int VK_F9 = 0x78;

    public static final int VK_F10 = 0x79;

    public static final int VK_F11 = 0x7A;

    public static final int VK_F12 = 0x7B;

    public static final int VK_DELETE = 0x7F;

    public static final int VK_DEAD_GRAVE = 0x80;

    public static final int VK_DEAD_ACUTE = 0x81;

    public static final int VK_DEAD_CIRCUMFLEX = 0x82;

    public static final int VK_DEAD_TILDE = 0x83;

    public static final int VK_DEAD_MACRON = 0x84;

    public static final int VK_DEAD_BREVE = 0x85;

    public static final int VK_DEAD_ABOVEDOT = 0x86;

    public static final int VK_DEAD_DIAERESIS = 0x87;

    public static final int VK_DEAD_ABOVERING = 0x88;

    public static final int VK_DEAD_DOUBLEACUTE = 0x89;

    public static final int VK_DEAD_CARON = 0x8A;

    public static final int VK_DEAD_CEDILLA = 0x8B;

    public static final int VK_DEAD_OGONEK = 0x8C;

    public static final int VK_DEAD_IOTA = 0x8D;

    public static final int VK_DEAD_VOICED_SOUND = 0x8E;

    public static final int VK_DEAD_SEMIVOICED_SOUND = 0x8F;

    public static final int VK_NUM_LOCK = 0x90;

    public static final int VK_SCROLL_LOCK = 0x91;

    public static final int VK_AMPERSAND = 0x96;

    public static final int VK_ASTERISK = 0x97;

    public static final int VK_QUOTEDBL = 0x98;

    public static final int VK_LESS = 0x99;

    public static final int VK_PRINTSCREEN = 0x9A;

    public static final int VK_INSERT = 0x9B;

    public static final int VK_HELP = 0x9C;

    public static final int VK_META = 0x9D;

    public static final int VK_GREATER = 0xA0;

    public static final int VK_BRACELEFT = 0xA1;

    public static final int VK_BRACERIGHT = 0xA2;

    public static final int VK_BACK_QUOTE = 0xC0;

    public static final int VK_QUOTE = 0xDE;

    public static final int VK_KP_UP = 0xE0;

    public static final int VK_KP_DOWN = 0xE1;

    public static final int VK_KP_LEFT = 0xE2;

    public static final int VK_KP_RIGHT = 0xE3;

    public static final int VK_ALPHANUMERIC = 0xF0;

    public static final int VK_KATAKANA = 0xF1;

    public static final int VK_HIRAGANA = 0xF2;

    public static final int VK_FULL_WIDTH = 0xF3;

    public static final int VK_HALF_WIDTH = 0xF4;

    public static final int VK_ROMAN_CHARACTERS = 0xF5;

    public static final int VK_ALL_CANDIDATES = 0x100;

    public static final int VK_PREVIOUS_CANDIDATE = 0x101;

    public static final int VK_CODE_INPUT = 0x102;

    public static final int VK_JAPANESE_KATAKANA = 0x103;

    public static final int VK_JAPANESE_HIRAGANA = 0x104;

    public static final int VK_JAPANESE_ROMAN = 0x105;

    public static final int VK_KANA_LOCK = 0x106;

    public static final int VK_INPUT_METHOD_ON_OFF = 0x107;

    public static final int VK_AT = 0x200;

    public static final int VK_COLON = 0x201;

    public static final int VK_CIRCUMFLEX = 0x202;

    public static final int VK_DOLLAR = 0x203;

    public static final int VK_EURO_SIGN = 0x204;

    public static final int VK_EXCLAMATION_MARK = 0x205;

    public static final int VK_INVERTED_EXCLAMATION_MARK = 0x206;

    public static final int VK_LEFT_PARENTHESIS = 0x207;

    public static final int VK_NUMBER_SIGN = 0x208;

    public static final int VK_PLUS = 0x209;

    public static final int VK_RIGHT_PARENTHESIS = 0x20A;

    public static final int VK_UNDERSCORE = 0x20B;

    public static final int VK_WINDOWS = 0x20C;

    public static final int VK_CONTEXT_MENU = 0x20D;

    public static final int VK_F13 = 0xF000;

    public static final int VK_F14 = 0xF001;

    public static final int VK_F15 = 0xF002;

    public static final int VK_F16 = 0xF003;

    public static final int VK_F17 = 0xF004;

    public static final int VK_F18 = 0xF005;

    public static final int VK_F19 = 0xF006;

    public static final int VK_F20 = 0xF007;

    public static final int VK_F21 = 0xF008;

    public static final int VK_F22 = 0xF009;

    public static final int VK_F23 = 0xF00A;

    public static final int VK_F24 = 0xF00B;

    public static final int VK_COMPOSE = 0xFF20;

    public static final int VK_BEGIN = 0xFF58;

    public static final int VK_ALT_GRAPH = 0xFF7E;

    public static final int VK_STOP = 0xFFC8;

    public static final int VK_AGAIN = 0xFFC9;

    public static final int VK_PROPS = 0xFFCA;

    public static final int VK_UNDO = 0xFFCB;

    public static final int VK_COPY = 0xFFCD;

    public static final int VK_PASTE = 0xFFCF;

    public static final int VK_FIND = 0xFFD0;

    public static final int VK_CUT = 0xFFD1;

    private int keyCode;

    private char keyChar;

    private final int keyLocation;

    private final int extendedKeyCode;

    public KeyEvent(Component source, int id, long when, int modifiers, int keyCode, char keyChar,
            int keyLocation) {
        super(source, id, when, modifiers);
        if (id == KEY_TYPED) {
            if (keyChar == CHAR_UNDEFINED) {
                throw new IllegalArgumentException("invalid keyChar");
            }
            if (keyCode != VK_UNDEFINED) {
                throw new IllegalArgumentException("invalid keyCode");
            }
            if (keyLocation != KEY_LOCATION_UNKNOWN) {
                throw new IllegalArgumentException("invalid keyLocation");
            }
        }
        this.keyCode = keyCode;
        this.keyChar = keyChar;
        this.keyLocation = keyLocation >= KEY_LOCATION_UNKNOWN && keyLocation <= KEY_LOCATION_NUMPAD
                ? keyLocation : KEY_LOCATION_UNKNOWN;
        this.extendedKeyCode = keyCode == VK_UNDEFINED ? getExtendedKeyCodeForChar(keyChar) : keyCode;
        this.modifiers = normalize(modifiers, false);
    }

    public KeyEvent(Component source, int id, long when, int modifiers, int keyCode, char keyChar) {
        this(source, id, when, modifiers, keyCode, keyChar, KEY_LOCATION_UNKNOWN);
    }

    @Deprecated
    public KeyEvent(Component source, int id, long when, int modifiers, int keyCode) {
        this(source, id, when, modifiers, keyCode, (char) keyCode);
    }

    public int getKeyCode() {
        return keyCode;
    }

    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
    }

    public char getKeyChar() {
        return keyChar;
    }

    public void setKeyChar(char keyChar) {
        this.keyChar = keyChar;
    }

    @Deprecated
    public void setModifiers(int modifiers) {
        this.modifiers = normalize(modifiers, false);
    }

    public int getKeyLocation() {
        return keyLocation;
    }

    public int getExtendedKeyCode() {
        return extendedKeyCode;
    }

    /// The extended key code of a character: the virtual key code when the
    /// character has an unambiguous one, otherwise the character with the
    /// extended flag set.
    public static int getExtendedKeyCodeForChar(int c) {
        if (c >= 'a' && c <= 'z') {
            return c - 'a' + 'A';
        }
        if (c >= 'A' && c <= 'Z' || c >= '0' && c <= '9') {
            return c;
        }
        switch (c) {
            case ' ':
                return VK_SPACE;
            case ',':
                return VK_COMMA;
            case '-':
                return VK_MINUS;
            case '.':
                return VK_PERIOD;
            case '/':
                return VK_SLASH;
            case ';':
                return VK_SEMICOLON;
            case '=':
                return VK_EQUALS;
            case '[':
                return VK_OPEN_BRACKET;
            case '\\':
                return VK_BACK_SLASH;
            case ']':
                return VK_CLOSE_BRACKET;
            case '`':
                return VK_BACK_QUOTE;
            case '\'':
                return VK_QUOTE;
            case '\t':
                return VK_TAB;
            case '\n':
                return VK_ENTER;
            case '\b':
                return VK_BACK_SPACE;
            case 0x1b:
                return VK_ESCAPE;
            case 0x7f:
                return VK_DELETE;
            default:
                return c | EXTENDED_FLAG;
        }
    }

    public boolean isActionKey() {
        switch (keyCode) {
            case VK_HOME:
            case VK_END:
            case VK_PAGE_UP:
            case VK_PAGE_DOWN:
            case VK_UP:
            case VK_DOWN:
            case VK_LEFT:
            case VK_RIGHT:
            case VK_BEGIN:
            case VK_KP_LEFT:
            case VK_KP_UP:
            case VK_KP_RIGHT:
            case VK_KP_DOWN:
            case VK_F1:
            case VK_F2:
            case VK_F3:
            case VK_F4:
            case VK_F5:
            case VK_F6:
            case VK_F7:
            case VK_F8:
            case VK_F9:
            case VK_F10:
            case VK_F11:
            case VK_F12:
            case VK_F13:
            case VK_F14:
            case VK_F15:
            case VK_F16:
            case VK_F17:
            case VK_F18:
            case VK_F19:
            case VK_F20:
            case VK_F21:
            case VK_F22:
            case VK_F23:
            case VK_F24:
            case VK_PRINTSCREEN:
            case VK_SCROLL_LOCK:
            case VK_CAPS_LOCK:
            case VK_NUM_LOCK:
            case VK_PAUSE:
            case VK_INSERT:
            case VK_FINAL:
            case VK_CONVERT:
            case VK_NONCONVERT:
            case VK_ACCEPT:
            case VK_MODECHANGE:
            case VK_KANA:
            case VK_KANJI:
            case VK_ALPHANUMERIC:
            case VK_KATAKANA:
            case VK_HIRAGANA:
            case VK_FULL_WIDTH:
            case VK_HALF_WIDTH:
            case VK_ROMAN_CHARACTERS:
            case VK_ALL_CANDIDATES:
            case VK_PREVIOUS_CANDIDATE:
            case VK_CODE_INPUT:
            case VK_JAPANESE_KATAKANA:
            case VK_JAPANESE_HIRAGANA:
            case VK_JAPANESE_ROMAN:
            case VK_KANA_LOCK:
            case VK_INPUT_METHOD_ON_OFF:
            case VK_AGAIN:
            case VK_UNDO:
            case VK_COPY:
            case VK_PASTE:
            case VK_CUT:
            case VK_FIND:
            case VK_PROPS:
            case VK_STOP:
            case VK_HELP:
            case VK_WINDOWS:
            case VK_CONTEXT_MENU:
                return true;
            default:
                return false;
        }
    }

    /// The English name of a key code.
    public static String getKeyText(int keyCode) {
        if (keyCode >= VK_0 && keyCode <= VK_9 || keyCode >= VK_A && keyCode <= VK_Z) {
            return String.valueOf((char) keyCode);
        }
        if (keyCode >= VK_NUMPAD0 && keyCode <= VK_NUMPAD9) {
            return "NumPad-" + (char) ('0' + keyCode - VK_NUMPAD0);
        }
        if (keyCode >= VK_F1 && keyCode <= VK_F12) {
            return "F" + (keyCode - VK_F1 + 1);
        }
        if (keyCode >= VK_F13 && keyCode <= VK_F24) {
            return "F" + (keyCode - VK_F13 + 13);
        }
        String name = namedKey(keyCode);
        if (name != null) {
            return name;
        }
        if ((keyCode & EXTENDED_FLAG) != 0) {
            return String.valueOf((char) (keyCode ^ EXTENDED_FLAG));
        }
        return "Unknown keyCode: 0x" + Integer.toHexString(keyCode);
    }

    private static String namedKey(int keyCode) {
        switch (keyCode) {
            case VK_ENTER:
                return "Enter";
            case VK_BACK_SPACE:
                return "Backspace";
            case VK_TAB:
                return "Tab";
            case VK_CANCEL:
                return "Cancel";
            case VK_CLEAR:
                return "Clear";
            case VK_COMPOSE:
                return "Compose";
            case VK_PAUSE:
                return "Pause";
            case VK_CAPS_LOCK:
                return "Caps Lock";
            case VK_ESCAPE:
                return "Escape";
            case VK_SPACE:
                return "Space";
            case VK_PAGE_UP:
                return "Page Up";
            case VK_PAGE_DOWN:
                return "Page Down";
            case VK_END:
                return "End";
            case VK_HOME:
                return "Home";
            case VK_LEFT:
                return "Left";
            case VK_UP:
                return "Up";
            case VK_RIGHT:
                return "Right";
            case VK_DOWN:
                return "Down";
            case VK_BEGIN:
                return "Begin";
            case VK_SHIFT:
                return "Shift";
            case VK_CONTROL:
                return "Control";
            case VK_ALT:
                return "Alt";
            case VK_META:
                return "Meta";
            case VK_ALT_GRAPH:
                return "Alt Graph";
            case VK_COMMA:
                return "Comma";
            case VK_PERIOD:
                return "Period";
            case VK_SLASH:
                return "Slash";
            case VK_SEMICOLON:
                return "Semicolon";
            case VK_EQUALS:
                return "Equals";
            case VK_OPEN_BRACKET:
                return "Open Bracket";
            case VK_BACK_SLASH:
                return "Back Slash";
            case VK_CLOSE_BRACKET:
                return "Close Bracket";
            case VK_MULTIPLY:
                return "NumPad *";
            case VK_ADD:
                return "NumPad +";
            case VK_SEPARATOR:
                return "NumPad ,";
            case VK_SUBTRACT:
                return "NumPad -";
            case VK_DECIMAL:
                return "NumPad .";
            case VK_DIVIDE:
                return "NumPad /";
            case VK_DELETE:
                return "Delete";
            case VK_NUM_LOCK:
                return "Num Lock";
            case VK_SCROLL_LOCK:
                return "Scroll Lock";
            case VK_WINDOWS:
                return "Windows";
            case VK_CONTEXT_MENU:
                return "Context Menu";
            case VK_PRINTSCREEN:
                return "Print Screen";
            case VK_INSERT:
                return "Insert";
            case VK_HELP:
                return "Help";
            case VK_BACK_QUOTE:
                return "Back Quote";
            case VK_QUOTE:
                return "Quote";
            case VK_KP_UP:
                return "NumPad Up";
            case VK_KP_DOWN:
                return "NumPad Down";
            case VK_KP_LEFT:
                return "NumPad Left";
            case VK_KP_RIGHT:
                return "NumPad Right";
            case VK_DEAD_GRAVE:
                return "Dead Grave";
            case VK_DEAD_ACUTE:
                return "Dead Acute";
            case VK_DEAD_CIRCUMFLEX:
                return "Dead Circumflex";
            case VK_DEAD_TILDE:
                return "Dead Tilde";
            case VK_DEAD_MACRON:
                return "Dead Macron";
            case VK_DEAD_BREVE:
                return "Dead Breve";
            case VK_DEAD_ABOVEDOT:
                return "Dead Above Dot";
            case VK_DEAD_DIAERESIS:
                return "Dead Diaeresis";
            case VK_DEAD_ABOVERING:
                return "Dead Above Ring";
            case VK_DEAD_DOUBLEACUTE:
                return "Dead Double Acute";
            case VK_DEAD_CARON:
                return "Dead Caron";
            case VK_DEAD_CEDILLA:
                return "Dead Cedilla";
            case VK_DEAD_OGONEK:
                return "Dead Ogonek";
            case VK_DEAD_IOTA:
                return "Dead Iota";
            case VK_DEAD_VOICED_SOUND:
                return "Dead Voiced Sound";
            case VK_DEAD_SEMIVOICED_SOUND:
                return "Dead Semivoiced Sound";
            case VK_AMPERSAND:
                return "Ampersand";
            case VK_ASTERISK:
                return "Asterisk";
            case VK_QUOTEDBL:
                return "Double Quote";
            case VK_LESS:
                return "Less";
            case VK_GREATER:
                return "Greater";
            case VK_BRACELEFT:
                return "Left Brace";
            case VK_BRACERIGHT:
                return "Right Brace";
            case VK_AT:
                return "At";
            case VK_COLON:
                return "Colon";
            case VK_CIRCUMFLEX:
                return "Circumflex";
            case VK_DOLLAR:
                return "Dollar";
            case VK_EURO_SIGN:
                return "Euro";
            case VK_EXCLAMATION_MARK:
                return "Exclamation Mark";
            case VK_INVERTED_EXCLAMATION_MARK:
                return "Inverted Exclamation Mark";
            case VK_LEFT_PARENTHESIS:
                return "Left Parenthesis";
            case VK_NUMBER_SIGN:
                return "Number Sign";
            case VK_MINUS:
                return "Minus";
            case VK_PLUS:
                return "Plus";
            case VK_RIGHT_PARENTHESIS:
                return "Right Parenthesis";
            case VK_UNDERSCORE:
                return "Underscore";
            case VK_FINAL:
                return "Final";
            case VK_CONVERT:
                return "Convert";
            case VK_NONCONVERT:
                return "No Convert";
            case VK_ACCEPT:
                return "Accept";
            case VK_MODECHANGE:
                return "Mode Change";
            case VK_KANA:
                return "Kana";
            case VK_KANJI:
                return "Kanji";
            case VK_ALPHANUMERIC:
                return "Alphanumeric";
            case VK_KATAKANA:
                return "Katakana";
            case VK_HIRAGANA:
                return "Hiragana";
            case VK_FULL_WIDTH:
                return "Full-Width";
            case VK_HALF_WIDTH:
                return "Half-Width";
            case VK_ROMAN_CHARACTERS:
                return "Roman Characters";
            case VK_ALL_CANDIDATES:
                return "All Candidates";
            case VK_PREVIOUS_CANDIDATE:
                return "Previous Candidate";
            case VK_CODE_INPUT:
                return "Code Input";
            case VK_JAPANESE_KATAKANA:
                return "Japanese Katakana";
            case VK_JAPANESE_HIRAGANA:
                return "Japanese Hiragana";
            case VK_JAPANESE_ROMAN:
                return "Japanese Roman";
            case VK_KANA_LOCK:
                return "Kana Lock";
            case VK_INPUT_METHOD_ON_OFF:
                return "Input Method On/Off";
            case VK_AGAIN:
                return "Again";
            case VK_UNDO:
                return "Undo";
            case VK_COPY:
                return "Copy";
            case VK_PASTE:
                return "Paste";
            case VK_CUT:
                return "Cut";
            case VK_FIND:
                return "Find";
            case VK_PROPS:
                return "Props";
            case VK_STOP:
                return "Stop";
            default:
                return null;
        }
    }

    /// The English names of the old-style modifier bits, joined by plus
    /// signs.
    public static String getKeyModifiersText(int modifiers) {
        StringBuilder buf = new StringBuilder();
        if ((modifiers & META_MASK) != 0) {
            add(buf, "Meta");
        }
        if ((modifiers & CTRL_MASK) != 0) {
            add(buf, "Ctrl");
        }
        if ((modifiers & ALT_MASK) != 0) {
            add(buf, "Alt");
        }
        if ((modifiers & SHIFT_MASK) != 0) {
            add(buf, "Shift");
        }
        if ((modifiers & ALT_GRAPH_MASK) != 0) {
            add(buf, "Alt Graph");
        }
        if ((modifiers & BUTTON1_MASK) != 0) {
            add(buf, "Button1");
        }
        return buf.toString();
    }

    private static void add(StringBuilder buf, String name) {
        if (buf.length() > 0) {
            buf.append('+');
        }
        buf.append(name);
    }

    @Override
    public String paramString() {
        StringBuilder str = new StringBuilder(100);
        switch (id) {
            case KEY_PRESSED:
                str.append("KEY_PRESSED");
                break;
            case KEY_RELEASED:
                str.append("KEY_RELEASED");
                break;
            case KEY_TYPED:
                str.append("KEY_TYPED");
                break;
            default:
                str.append("unknown type");
                break;
        }
        str.append(",keyCode=").append(keyCode);
        str.append(",keyText=").append(getKeyText(keyCode));
        str.append(",keyChar=");
        switch (keyChar) {
            case '\b':
                str.append("Backspace");
                break;
            case '\t':
                str.append("Tab");
                break;
            case '\n':
                str.append("Newline");
                break;
            case 0x18:
                str.append("Cancel");
                break;
            case 0x1b:
                str.append("Escape");
                break;
            case 0x7f:
                str.append("Delete");
                break;
            case CHAR_UNDEFINED:
                str.append("Undefined");
                break;
            default:
                if (keyChar < ' ') {
                    str.append("Unknown keyChar 0x").append(Integer.toHexString(keyChar));
                } else {
                    str.append('\'').append(keyChar).append('\'');
                }
                break;
        }
        if (getModifiers() != 0) {
            str.append(",modifiers=").append(getKeyModifiersText(modifiers));
        }
        if (getModifiersEx() != 0) {
            str.append(",extModifiers=").append(getModifiersExText(modifiers));
        }
        str.append(",keyLocation=");
        switch (keyLocation) {
            case KEY_LOCATION_UNKNOWN:
                str.append("KEY_LOCATION_UNKNOWN");
                break;
            case KEY_LOCATION_STANDARD:
                str.append("KEY_LOCATION_STANDARD");
                break;
            case KEY_LOCATION_LEFT:
                str.append("KEY_LOCATION_LEFT");
                break;
            case KEY_LOCATION_RIGHT:
                str.append("KEY_LOCATION_RIGHT");
                break;
            case KEY_LOCATION_NUMPAD:
                str.append("KEY_LOCATION_NUMPAD");
                break;
            default:
                str.append("KEY_LOCATION_UNKNOWN");
                break;
        }
        return str.toString();
    }
}
