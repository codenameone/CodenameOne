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
package com.codename1.desktopcompat.rt;

import com.codename1.desktopcompat.java.awt.event.KeyEvent;
import com.codename1.ui.Display;
import java.util.HashMap;

/// The two key tables of the layer: Codename One key codes to AWT virtual
/// keys, and the names `KeyStroke.getKeyStroke(String)` reads to virtual
/// keys.
///
/// A Codename One key code is the character the key types, or a port
/// specific code for a key that types none. What can be told apart on
/// every port is mapped: letters, digits, punctuation (to the key that
/// carries it on a US keyboard, so `?` is `VK_SLASH`), space, enter,
/// backspace, tab, escape, delete, the modifier keys and the arrows.
/// The function keys and the navigation block (home, end, page up, page
/// down, insert) are not: the ports report them with a code that is also a
/// printable character (F1 arrives as the code of `p`), so they come
/// through as that character's key.
public final class KeyMap {

    private static final String SHIFTED = "!@#$%^&*()";
    private static final String DIGITS = "1234567890";
    private static final HashMap<String, Integer> names = new HashMap<String, Integer>();
    private static final HashMap<Integer, String> codes = new HashMap<Integer, String>();

    private KeyMap() {
    }

    /// The virtual key of a Codename One key code, `VK_UNDEFINED` when it
    /// has none.
    public static int virtualKey(int code) {
        if (code >= 'a' && code <= 'z') {
            return code - ('a' - 'A');
        }
        if ((code >= 'A' && code <= 'Z') || (code >= '0' && code <= '9')) {
            return code;
        }
        switch (code) {
            case ' ':
                return KeyEvent.VK_SPACE;
            case '\n':
            case '\r':
                return KeyEvent.VK_ENTER;
            case 8:
                return KeyEvent.VK_BACK_SPACE;
            case '\t':
                return KeyEvent.VK_TAB;
            case 27:
                return KeyEvent.VK_ESCAPE;
            case 127:
                return KeyEvent.VK_DELETE;
            case 16:
                return KeyEvent.VK_SHIFT;
            case 17:
                return KeyEvent.VK_CONTROL;
            case 18:
                return KeyEvent.VK_ALT;
            case 20:
                return KeyEvent.VK_CAPS_LOCK;
            case 157:
                return KeyEvent.VK_META;
            case ',':
            case '<':
                return KeyEvent.VK_COMMA;
            case '-':
            case '_':
                return KeyEvent.VK_MINUS;
            case '.':
            case '>':
                return KeyEvent.VK_PERIOD;
            case '/':
            case '?':
                return KeyEvent.VK_SLASH;
            case ';':
            case ':':
                return KeyEvent.VK_SEMICOLON;
            case '=':
            case '+':
                return KeyEvent.VK_EQUALS;
            case '[':
            case '{':
                return KeyEvent.VK_OPEN_BRACKET;
            case '\\':
            case '|':
                return KeyEvent.VK_BACK_SLASH;
            case ']':
            case '}':
                return KeyEvent.VK_CLOSE_BRACKET;
            case '`':
            case '~':
                return KeyEvent.VK_BACK_QUOTE;
            case '\'':
            case '"':
                return KeyEvent.VK_QUOTE;
            default:
                break;
        }
        int shifted = code > 0 && code < 128 ? SHIFTED.indexOf((char) code) : -1;
        if (shifted >= 0) {
            return DIGITS.charAt(shifted);
        }
        if (code > 127 && code < 0xffff) {
            return KeyEvent.getExtendedKeyCodeForChar(code);
        }
        if (Display.isInitialized()) {
            Display d = Display.getInstance();
            switch (d.getGameAction(code)) {
                case Display.GAME_UP:
                    return KeyEvent.VK_UP;
                case Display.GAME_DOWN:
                    return KeyEvent.VK_DOWN;
                case Display.GAME_LEFT:
                    return KeyEvent.VK_LEFT;
                case Display.GAME_RIGHT:
                    return KeyEvent.VK_RIGHT;
                case Display.GAME_FIRE:
                    return KeyEvent.VK_ENTER;
                default:
                    break;
            }
        }
        return KeyEvent.VK_UNDEFINED;
    }

    /// The character a Codename One key code types, `CHAR_UNDEFINED` when
    /// it types none.
    public static char keyChar(int code) {
        if (code == '\r') {
            return '\n';
        }
        if (code == '\n' || code == '\t' || code == 8 || code == 27 || code == 127 || (code >= ' ' && code < 0xffff
                && code != 157)) {
            return (char) code;
        }
        if (Display.isInitialized() && Display.getInstance().getGameAction(code) == Display.GAME_FIRE) {
            return '\n';
        }
        return KeyEvent.CHAR_UNDEFINED;
    }

    private static void name(String name, int code) {
        names.put(name, Integer.valueOf(code));
        codes.put(Integer.valueOf(code), name);
    }

    private static void init() {
        if (!names.isEmpty()) {
            return;
        }
        for (char c = 'A'; c <= 'Z'; c++) {
            name(String.valueOf(c), c);
        }
        for (char c = '0'; c <= '9'; c++) {
            name(String.valueOf(c), c);
            name("NUMPAD" + c, KeyEvent.VK_NUMPAD0 + (c - '0'));
        }
        for (int i = 1; i <= 12; i++) {
            name("F" + i, KeyEvent.VK_F1 + i - 1);
        }
        for (int i = 13; i <= 24; i++) {
            name("F" + i, KeyEvent.VK_F13 + i - 13);
        }
        name("ENTER", KeyEvent.VK_ENTER);
        name("BACK_SPACE", KeyEvent.VK_BACK_SPACE);
        name("TAB", KeyEvent.VK_TAB);
        name("CANCEL", KeyEvent.VK_CANCEL);
        name("CLEAR", KeyEvent.VK_CLEAR);
        name("SHIFT", KeyEvent.VK_SHIFT);
        name("CONTROL", KeyEvent.VK_CONTROL);
        name("ALT", KeyEvent.VK_ALT);
        name("PAUSE", KeyEvent.VK_PAUSE);
        name("CAPS_LOCK", KeyEvent.VK_CAPS_LOCK);
        name("ESCAPE", KeyEvent.VK_ESCAPE);
        name("SPACE", KeyEvent.VK_SPACE);
        name("PAGE_UP", KeyEvent.VK_PAGE_UP);
        name("PAGE_DOWN", KeyEvent.VK_PAGE_DOWN);
        name("END", KeyEvent.VK_END);
        name("HOME", KeyEvent.VK_HOME);
        name("LEFT", KeyEvent.VK_LEFT);
        name("UP", KeyEvent.VK_UP);
        name("RIGHT", KeyEvent.VK_RIGHT);
        name("DOWN", KeyEvent.VK_DOWN);
        name("COMMA", KeyEvent.VK_COMMA);
        name("MINUS", KeyEvent.VK_MINUS);
        name("PERIOD", KeyEvent.VK_PERIOD);
        name("SLASH", KeyEvent.VK_SLASH);
        name("SEMICOLON", KeyEvent.VK_SEMICOLON);
        name("EQUALS", KeyEvent.VK_EQUALS);
        name("OPEN_BRACKET", KeyEvent.VK_OPEN_BRACKET);
        name("BACK_SLASH", KeyEvent.VK_BACK_SLASH);
        name("CLOSE_BRACKET", KeyEvent.VK_CLOSE_BRACKET);
        name("MULTIPLY", KeyEvent.VK_MULTIPLY);
        name("ADD", KeyEvent.VK_ADD);
        name("SEPARATOR", KeyEvent.VK_SEPARATOR);
        name("SUBTRACT", KeyEvent.VK_SUBTRACT);
        name("DECIMAL", KeyEvent.VK_DECIMAL);
        name("DIVIDE", KeyEvent.VK_DIVIDE);
        name("DELETE", KeyEvent.VK_DELETE);
        name("NUM_LOCK", KeyEvent.VK_NUM_LOCK);
        name("SCROLL_LOCK", KeyEvent.VK_SCROLL_LOCK);
        name("PRINTSCREEN", KeyEvent.VK_PRINTSCREEN);
        name("INSERT", KeyEvent.VK_INSERT);
        name("HELP", KeyEvent.VK_HELP);
        name("META", KeyEvent.VK_META);
        name("BACK_QUOTE", KeyEvent.VK_BACK_QUOTE);
        name("QUOTE", KeyEvent.VK_QUOTE);
        name("KP_UP", KeyEvent.VK_KP_UP);
        name("KP_DOWN", KeyEvent.VK_KP_DOWN);
        name("KP_LEFT", KeyEvent.VK_KP_LEFT);
        name("KP_RIGHT", KeyEvent.VK_KP_RIGHT);
        name("PLUS", KeyEvent.VK_PLUS);
        name("WINDOWS", KeyEvent.VK_WINDOWS);
        name("CONTEXT_MENU", KeyEvent.VK_CONTEXT_MENU);
    }

    /// The virtual key named by the part of a `VK_` constant after the
    /// prefix (`"ENTER"`, `"F5"`, `"A"`), or `VK_UNDEFINED`.
    public static int codeOf(String name) {
        init();
        Integer i = names.get(name);
        return i == null ? KeyEvent.VK_UNDEFINED : i.intValue();
    }

    /// The name of a virtual key as [#codeOf] reads it, or `null`.
    public static String nameOf(int code) {
        init();
        return codes.get(Integer.valueOf(code));
    }
}
