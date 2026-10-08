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
package javafx.scene.input;

import java.util.HashMap;

/// The keys of a keyboard, each with the name it is shown by.
public enum KeyCode {

    /// The `Enter` key.
    ENTER(0x0A, "Enter", KeyCodeClass.WHITESPACE),

    /// The `Backspace` key.
    BACK_SPACE(0x08, "Backspace"),

    /// The `Tab` key.
    TAB(0x09, "Tab", KeyCodeClass.WHITESPACE),

    /// The `Cancel` key.
    CANCEL(0x03, "Cancel"),

    /// The `Clear` key.
    CLEAR(0x0C, "Clear"),

    /// The `Shift` key.
    SHIFT(0x10, "Shift", KeyCodeClass.MODIFIER),

    /// The `Ctrl` key.
    CONTROL(0x11, "Ctrl", KeyCodeClass.MODIFIER),

    /// The `Alt` key.
    ALT(0x12, "Alt", KeyCodeClass.MODIFIER),

    /// The `Pause` key.
    PAUSE(0x13, "Pause"),

    /// The `Caps Lock` key.
    CAPS(0x14, "Caps Lock"),

    /// The `Esc` key.
    ESCAPE(0x1B, "Esc"),

    /// The `Space` key.
    SPACE(0x20, "Space", KeyCodeClass.WHITESPACE),

    /// The `Page Up` key.
    PAGE_UP(0x21, "Page Up", KeyCodeClass.NAVIGATION),

    /// The `Page Down` key.
    PAGE_DOWN(0x22, "Page Down", KeyCodeClass.NAVIGATION),

    /// The `End` key.
    END(0x23, "End", KeyCodeClass.NAVIGATION),

    /// The `Home` key.
    HOME(0x24, "Home", KeyCodeClass.NAVIGATION),

    /// The `Left` key.
    LEFT(0x25, "Left", KeyCodeClass.ARROW | KeyCodeClass.NAVIGATION),

    /// The `Up` key.
    UP(0x26, "Up", KeyCodeClass.ARROW | KeyCodeClass.NAVIGATION),

    /// The `Right` key.
    RIGHT(0x27, "Right", KeyCodeClass.ARROW | KeyCodeClass.NAVIGATION),

    /// The `Down` key.
    DOWN(0x28, "Down", KeyCodeClass.ARROW | KeyCodeClass.NAVIGATION),

    /// The `Comma` key.
    COMMA(0x2C, "Comma"),

    /// The `Minus` key.
    MINUS(0x2D, "Minus"),

    /// The `Period` key.
    PERIOD(0x2E, "Period"),

    /// The `Slash` key.
    SLASH(0x2F, "Slash"),

    /// The `0` key.
    DIGIT0(0x30, "0", KeyCodeClass.DIGIT),

    /// The `1` key.
    DIGIT1(0x31, "1", KeyCodeClass.DIGIT),

    /// The `2` key.
    DIGIT2(0x32, "2", KeyCodeClass.DIGIT),

    /// The `3` key.
    DIGIT3(0x33, "3", KeyCodeClass.DIGIT),

    /// The `4` key.
    DIGIT4(0x34, "4", KeyCodeClass.DIGIT),

    /// The `5` key.
    DIGIT5(0x35, "5", KeyCodeClass.DIGIT),

    /// The `6` key.
    DIGIT6(0x36, "6", KeyCodeClass.DIGIT),

    /// The `7` key.
    DIGIT7(0x37, "7", KeyCodeClass.DIGIT),

    /// The `8` key.
    DIGIT8(0x38, "8", KeyCodeClass.DIGIT),

    /// The `9` key.
    DIGIT9(0x39, "9", KeyCodeClass.DIGIT),

    /// The `Semicolon` key.
    SEMICOLON(0x3B, "Semicolon"),

    /// The `Equals` key.
    EQUALS(0x3D, "Equals"),

    /// The `A` key.
    A(0x41, "A", KeyCodeClass.LETTER),

    /// The `B` key.
    B(0x42, "B", KeyCodeClass.LETTER),

    /// The `C` key.
    C(0x43, "C", KeyCodeClass.LETTER),

    /// The `D` key.
    D(0x44, "D", KeyCodeClass.LETTER),

    /// The `E` key.
    E(0x45, "E", KeyCodeClass.LETTER),

    /// The `F` key.
    F(0x46, "F", KeyCodeClass.LETTER),

    /// The `G` key.
    G(0x47, "G", KeyCodeClass.LETTER),

    /// The `H` key.
    H(0x48, "H", KeyCodeClass.LETTER),

    /// The `I` key.
    I(0x49, "I", KeyCodeClass.LETTER),

    /// The `J` key.
    J(0x4A, "J", KeyCodeClass.LETTER),

    /// The `K` key.
    K(0x4B, "K", KeyCodeClass.LETTER),

    /// The `L` key.
    L(0x4C, "L", KeyCodeClass.LETTER),

    /// The `M` key.
    M(0x4D, "M", KeyCodeClass.LETTER),

    /// The `N` key.
    N(0x4E, "N", KeyCodeClass.LETTER),

    /// The `O` key.
    O(0x4F, "O", KeyCodeClass.LETTER),

    /// The `P` key.
    P(0x50, "P", KeyCodeClass.LETTER),

    /// The `Q` key.
    Q(0x51, "Q", KeyCodeClass.LETTER),

    /// The `R` key.
    R(0x52, "R", KeyCodeClass.LETTER),

    /// The `S` key.
    S(0x53, "S", KeyCodeClass.LETTER),

    /// The `T` key.
    T(0x54, "T", KeyCodeClass.LETTER),

    /// The `U` key.
    U(0x55, "U", KeyCodeClass.LETTER),

    /// The `V` key.
    V(0x56, "V", KeyCodeClass.LETTER),

    /// The `W` key.
    W(0x57, "W", KeyCodeClass.LETTER),

    /// The `X` key.
    X(0x58, "X", KeyCodeClass.LETTER),

    /// The `Y` key.
    Y(0x59, "Y", KeyCodeClass.LETTER),

    /// The `Z` key.
    Z(0x5A, "Z", KeyCodeClass.LETTER),

    /// The `Open Bracket` key.
    OPEN_BRACKET(0x5B, "Open Bracket"),

    /// The `Back Slash` key.
    BACK_SLASH(0x5C, "Back Slash"),

    /// The `Close Bracket` key.
    CLOSE_BRACKET(0x5D, "Close Bracket"),

    /// The `Numpad 0` key.
    NUMPAD0(0x60, "Numpad 0", KeyCodeClass.DIGIT | KeyCodeClass.KEYPAD),

    /// The `Numpad 1` key.
    NUMPAD1(0x61, "Numpad 1", KeyCodeClass.DIGIT | KeyCodeClass.KEYPAD),

    /// The `Numpad 2` key.
    NUMPAD2(0x62, "Numpad 2", KeyCodeClass.DIGIT | KeyCodeClass.KEYPAD),

    /// The `Numpad 3` key.
    NUMPAD3(0x63, "Numpad 3", KeyCodeClass.DIGIT | KeyCodeClass.KEYPAD),

    /// The `Numpad 4` key.
    NUMPAD4(0x64, "Numpad 4", KeyCodeClass.DIGIT | KeyCodeClass.KEYPAD),

    /// The `Numpad 5` key.
    NUMPAD5(0x65, "Numpad 5", KeyCodeClass.DIGIT | KeyCodeClass.KEYPAD),

    /// The `Numpad 6` key.
    NUMPAD6(0x66, "Numpad 6", KeyCodeClass.DIGIT | KeyCodeClass.KEYPAD),

    /// The `Numpad 7` key.
    NUMPAD7(0x67, "Numpad 7", KeyCodeClass.DIGIT | KeyCodeClass.KEYPAD),

    /// The `Numpad 8` key.
    NUMPAD8(0x68, "Numpad 8", KeyCodeClass.DIGIT | KeyCodeClass.KEYPAD),

    /// The `Numpad 9` key.
    NUMPAD9(0x69, "Numpad 9", KeyCodeClass.DIGIT | KeyCodeClass.KEYPAD),

    /// The `Multiply` key.
    MULTIPLY(0x6A, "Multiply"),

    /// The `Add` key.
    ADD(0x6B, "Add"),

    /// The `Separator` key.
    SEPARATOR(0x6C, "Separator"),

    /// The `Subtract` key.
    SUBTRACT(0x6D, "Subtract"),

    /// The `Decimal` key.
    DECIMAL(0x6E, "Decimal"),

    /// The `Divide` key.
    DIVIDE(0x6F, "Divide"),

    /// The `Delete` key.
    DELETE(0x7F, "Delete"),

    /// The `Num Lock` key.
    NUM_LOCK(0x90, "Num Lock"),

    /// The `Scroll Lock` key.
    SCROLL_LOCK(0x91, "Scroll Lock"),

    /// The `F1` key.
    F1(0x70, "F1", KeyCodeClass.FUNCTION),

    /// The `F2` key.
    F2(0x71, "F2", KeyCodeClass.FUNCTION),

    /// The `F3` key.
    F3(0x72, "F3", KeyCodeClass.FUNCTION),

    /// The `F4` key.
    F4(0x73, "F4", KeyCodeClass.FUNCTION),

    /// The `F5` key.
    F5(0x74, "F5", KeyCodeClass.FUNCTION),

    /// The `F6` key.
    F6(0x75, "F6", KeyCodeClass.FUNCTION),

    /// The `F7` key.
    F7(0x76, "F7", KeyCodeClass.FUNCTION),

    /// The `F8` key.
    F8(0x77, "F8", KeyCodeClass.FUNCTION),

    /// The `F9` key.
    F9(0x78, "F9", KeyCodeClass.FUNCTION),

    /// The `F10` key.
    F10(0x79, "F10", KeyCodeClass.FUNCTION),

    /// The `F11` key.
    F11(0x7A, "F11", KeyCodeClass.FUNCTION),

    /// The `F12` key.
    F12(0x7B, "F12", KeyCodeClass.FUNCTION),

    /// The `F13` key.
    F13(0xF000, "F13", KeyCodeClass.FUNCTION),

    /// The `F14` key.
    F14(0xF001, "F14", KeyCodeClass.FUNCTION),

    /// The `F15` key.
    F15(0xF002, "F15", KeyCodeClass.FUNCTION),

    /// The `F16` key.
    F16(0xF003, "F16", KeyCodeClass.FUNCTION),

    /// The `F17` key.
    F17(0xF004, "F17", KeyCodeClass.FUNCTION),

    /// The `F18` key.
    F18(0xF005, "F18", KeyCodeClass.FUNCTION),

    /// The `F19` key.
    F19(0xF006, "F19", KeyCodeClass.FUNCTION),

    /// The `F20` key.
    F20(0xF007, "F20", KeyCodeClass.FUNCTION),

    /// The `F21` key.
    F21(0xF008, "F21", KeyCodeClass.FUNCTION),

    /// The `F22` key.
    F22(0xF009, "F22", KeyCodeClass.FUNCTION),

    /// The `F23` key.
    F23(0xF00A, "F23", KeyCodeClass.FUNCTION),

    /// The `F24` key.
    F24(0xF00B, "F24", KeyCodeClass.FUNCTION),

    /// The `Print Screen` key.
    PRINTSCREEN(0x9A, "Print Screen"),

    /// The `Insert` key.
    INSERT(0x9B, "Insert"),

    /// The `Help` key.
    HELP(0x9C, "Help"),

    /// The `Meta` key.
    META(0x9D, "Meta", KeyCodeClass.MODIFIER),

    /// The `Back Quote` key.
    BACK_QUOTE(0xC0, "Back Quote"),

    /// The `Quote` key.
    QUOTE(0xDE, "Quote"),

    /// The `Numpad Up` key.
    KP_UP(0xE0, "Numpad Up", KeyCodeClass.ARROW | KeyCodeClass.KEYPAD | KeyCodeClass.NAVIGATION),

    /// The `Numpad Down` key.
    KP_DOWN(0xE1, "Numpad Down", KeyCodeClass.ARROW | KeyCodeClass.KEYPAD | KeyCodeClass.NAVIGATION),

    /// The `Numpad Left` key.
    KP_LEFT(0xE2, "Numpad Left", KeyCodeClass.ARROW | KeyCodeClass.KEYPAD | KeyCodeClass.NAVIGATION),

    /// The `Numpad Right` key.
    KP_RIGHT(0xE3, "Numpad Right", KeyCodeClass.ARROW | KeyCodeClass.KEYPAD | KeyCodeClass.NAVIGATION),

    /// The `Dead Grave` key.
    DEAD_GRAVE(0x80, "Dead Grave"),

    /// The `Dead Acute` key.
    DEAD_ACUTE(0x81, "Dead Acute"),

    /// The `Dead Circumflex` key.
    DEAD_CIRCUMFLEX(0x82, "Dead Circumflex"),

    /// The `Dead Tilde` key.
    DEAD_TILDE(0x83, "Dead Tilde"),

    /// The `Dead Macron` key.
    DEAD_MACRON(0x84, "Dead Macron"),

    /// The `Dead Breve` key.
    DEAD_BREVE(0x85, "Dead Breve"),

    /// The `Dead Abovedot` key.
    DEAD_ABOVEDOT(0x86, "Dead Abovedot"),

    /// The `Dead Diaeresis` key.
    DEAD_DIAERESIS(0x87, "Dead Diaeresis"),

    /// The `Dead Abovering` key.
    DEAD_ABOVERING(0x88, "Dead Abovering"),

    /// The `Dead Doubleacute` key.
    DEAD_DOUBLEACUTE(0x89, "Dead Doubleacute"),

    /// The `Dead Caron` key.
    DEAD_CARON(0x8A, "Dead Caron"),

    /// The `Dead Cedilla` key.
    DEAD_CEDILLA(0x8B, "Dead Cedilla"),

    /// The `Dead Ogonek` key.
    DEAD_OGONEK(0x8C, "Dead Ogonek"),

    /// The `Dead Iota` key.
    DEAD_IOTA(0x8D, "Dead Iota"),

    /// The `Dead Voiced Sound` key.
    DEAD_VOICED_SOUND(0x8E, "Dead Voiced Sound"),

    /// The `Dead Semivoiced Sound` key.
    DEAD_SEMIVOICED_SOUND(0x8F, "Dead Semivoiced Sound"),

    /// The `Ampersand` key.
    AMPERSAND(0x96, "Ampersand"),

    /// The `Asterisk` key.
    ASTERISK(0x97, "Asterisk"),

    /// The `Double Quote` key.
    QUOTEDBL(0x98, "Double Quote"),

    /// The `Less` key.
    LESS(0x99, "Less"),

    /// The `Greater` key.
    GREATER(0xA0, "Greater"),

    /// The `Left Brace` key.
    BRACELEFT(0xA1, "Left Brace"),

    /// The `Right Brace` key.
    BRACERIGHT(0xA2, "Right Brace"),

    /// The `At` key.
    AT(0x0200, "At"),

    /// The `Colon` key.
    COLON(0x0201, "Colon"),

    /// The `Circumflex` key.
    CIRCUMFLEX(0x0202, "Circumflex"),

    /// The `Dollar` key.
    DOLLAR(0x0203, "Dollar"),

    /// The `Euro Sign` key.
    EURO_SIGN(0x0204, "Euro Sign"),

    /// The `Exclamation Mark` key.
    EXCLAMATION_MARK(0x0205, "Exclamation Mark"),

    /// The `Inverted Exclamation Mark` key.
    INVERTED_EXCLAMATION_MARK(0x0206, "Inverted Exclamation Mark"),

    /// The `Left Parenthesis` key.
    LEFT_PARENTHESIS(0x0207, "Left Parenthesis"),

    /// The `Number Sign` key.
    NUMBER_SIGN(0x0208, "Number Sign"),

    /// The `Plus` key.
    PLUS(0x0209, "Plus"),

    /// The `Right Parenthesis` key.
    RIGHT_PARENTHESIS(0x020A, "Right Parenthesis"),

    /// The `Underscore` key.
    UNDERSCORE(0x020B, "Underscore"),

    /// The `Windows` key.
    WINDOWS(0x020C, "Windows", KeyCodeClass.MODIFIER),

    /// The `Context Menu` key.
    CONTEXT_MENU(0x020D, "Context Menu"),

    /// The `Final` key.
    FINAL(0x0018, "Final"),

    /// The `Convert` key.
    CONVERT(0x001C, "Convert"),

    /// The `Nonconvert` key.
    NONCONVERT(0x001D, "Nonconvert"),

    /// The `Accept` key.
    ACCEPT(0x001E, "Accept"),

    /// The `Mode Change` key.
    MODECHANGE(0x001F, "Mode Change"),

    /// The `Kana` key.
    KANA(0x0015, "Kana"),

    /// The `Kanji` key.
    KANJI(0x0019, "Kanji"),

    /// The `Alphanumeric` key.
    ALPHANUMERIC(0x00F0, "Alphanumeric"),

    /// The `Katakana` key.
    KATAKANA(0x00F1, "Katakana"),

    /// The `Hiragana` key.
    HIRAGANA(0x00F2, "Hiragana"),

    /// The `Full Width` key.
    FULL_WIDTH(0x00F3, "Full Width"),

    /// The `Half Width` key.
    HALF_WIDTH(0x00F4, "Half Width"),

    /// The `Roman Characters` key.
    ROMAN_CHARACTERS(0x00F5, "Roman Characters"),

    /// The `All Candidates` key.
    ALL_CANDIDATES(0x0100, "All Candidates"),

    /// The `Previous Candidate` key.
    PREVIOUS_CANDIDATE(0x0101, "Previous Candidate"),

    /// The `Code Input` key.
    CODE_INPUT(0x0102, "Code Input"),

    /// The `Japanese Katakana` key.
    JAPANESE_KATAKANA(0x0103, "Japanese Katakana"),

    /// The `Japanese Hiragana` key.
    JAPANESE_HIRAGANA(0x0104, "Japanese Hiragana"),

    /// The `Japanese Roman` key.
    JAPANESE_ROMAN(0x0105, "Japanese Roman"),

    /// The `Kana Lock` key.
    KANA_LOCK(0x0106, "Kana Lock"),

    /// The `Input Method On Off` key.
    INPUT_METHOD_ON_OFF(0x0107, "Input Method On Off"),

    /// The `Cut` key.
    CUT(0xFFD1, "Cut"),

    /// The `Copy` key.
    COPY(0xFFCD, "Copy"),

    /// The `Paste` key.
    PASTE(0xFFCF, "Paste"),

    /// The `Undo` key.
    UNDO(0xFFCB, "Undo"),

    /// The `Again` key.
    AGAIN(0xFFC9, "Again"),

    /// The `Find` key.
    FIND(0xFFD0, "Find"),

    /// The `Properties` key.
    PROPS(0xFFCA, "Properties"),

    /// The `Stop` key.
    STOP(0xFFC8, "Stop"),

    /// The `Compose` key.
    COMPOSE(0xFF20, "Compose"),

    /// The `Alt Graph` key.
    ALT_GRAPH(0xFF7E, "Alt Graph", KeyCodeClass.MODIFIER),

    /// The `Begin` key.
    BEGIN(0xFF58, "Begin"),

    /// The `Undefined` key.
    UNDEFINED(0x0, "Undefined"),

    /// The `Softkey 0` key.
    SOFTKEY_0(0x1000, "Softkey 0"),

    /// The `Softkey 1` key.
    SOFTKEY_1(0x1001, "Softkey 1"),

    /// The `Softkey 2` key.
    SOFTKEY_2(0x1002, "Softkey 2"),

    /// The `Softkey 3` key.
    SOFTKEY_3(0x1003, "Softkey 3"),

    /// The `Softkey 4` key.
    SOFTKEY_4(0x1004, "Softkey 4"),

    /// The `Softkey 5` key.
    SOFTKEY_5(0x1005, "Softkey 5"),

    /// The `Softkey 6` key.
    SOFTKEY_6(0x1006, "Softkey 6"),

    /// The `Softkey 7` key.
    SOFTKEY_7(0x1007, "Softkey 7"),

    /// The `Softkey 8` key.
    SOFTKEY_8(0x1008, "Softkey 8"),

    /// The `Softkey 9` key.
    SOFTKEY_9(0x1009, "Softkey 9"),

    /// The `Game A` key.
    GAME_A(0x100A, "Game A"),

    /// The `Game B` key.
    GAME_B(0x100B, "Game B"),

    /// The `Game C` key.
    GAME_C(0x100C, "Game C"),

    /// The `Game D` key.
    GAME_D(0x100D, "Game D"),

    /// The `Star` key.
    STAR(0x100E, "Star"),

    /// The `Pound` key.
    POUND(0x100F, "Pound"),

    /// The `Power` key.
    POWER(0x199, "Power"),

    /// The `Info` key.
    INFO(0x1C9, "Info"),

    /// The `Colored Key 0` key.
    COLORED_KEY_0(0x193, "Colored Key 0"),

    /// The `Colored Key 1` key.
    COLORED_KEY_1(0x194, "Colored Key 1"),

    /// The `Colored Key 2` key.
    COLORED_KEY_2(0x195, "Colored Key 2"),

    /// The `Colored Key 3` key.
    COLORED_KEY_3(0x196, "Colored Key 3"),

    /// The `Eject` key.
    EJECT_TOGGLE(0x19E, "Eject", KeyCodeClass.MEDIA),

    /// The `Play` key.
    PLAY(0x19F, "Play", KeyCodeClass.MEDIA),

    /// The `Record` key.
    RECORD(0x1A0, "Record", KeyCodeClass.MEDIA),

    /// The `Fast Forward` key.
    FAST_FWD(0x1A1, "Fast Forward", KeyCodeClass.MEDIA),

    /// The `Rewind` key.
    REWIND(0x19C, "Rewind", KeyCodeClass.MEDIA),

    /// The `Previous Track` key.
    TRACK_PREV(0x1A8, "Previous Track", KeyCodeClass.MEDIA),

    /// The `Next Track` key.
    TRACK_NEXT(0x1A9, "Next Track", KeyCodeClass.MEDIA),

    /// The `Channel Up` key.
    CHANNEL_UP(0x1AB, "Channel Up", KeyCodeClass.MEDIA),

    /// The `Channel Down` key.
    CHANNEL_DOWN(0x1AC, "Channel Down", KeyCodeClass.MEDIA),

    /// The `Volume Up` key.
    VOLUME_UP(0x1bf, "Volume Up", KeyCodeClass.MEDIA),

    /// The `Volume Down` key.
    VOLUME_DOWN(0x1C0, "Volume Down", KeyCodeClass.MEDIA),

    /// The `Mute` key.
    MUTE(0x1C1, "Mute", KeyCodeClass.MEDIA),

    /// The `Command` key.
    COMMAND(0x300, "Command", KeyCodeClass.MODIFIER),

    /// The `Shortcut` key.
    SHORTCUT(-1, "Shortcut");

    private static final class KeyCodeClass {
        static final int FUNCTION = 1;
        static final int NAVIGATION = 1 << 1;
        static final int ARROW = 1 << 2;
        static final int MODIFIER = 1 << 3;
        static final int LETTER = 1 << 4;
        static final int DIGIT = 1 << 5;
        static final int KEYPAD = 1 << 6;
        static final int WHITESPACE = 1 << 7;
        static final int MEDIA = 1 << 8;

        private KeyCodeClass() {
        }
    }

    private static HashMap<String, KeyCode> byName;

    private final int code;
    private final String ch;
    private final String name;
    private final int mask;

    KeyCode(int code, String name, int mask) {
        this.code = code;
        this.name = name;
        this.mask = mask;
        this.ch = String.valueOf((char) code);
    }

    KeyCode(int code, String name) {
        this(code, name, 0);
    }

    /// Returns whether this is a function key, `F1` and the like.
    public final boolean isFunctionKey() {
        return (mask & KeyCodeClass.FUNCTION) != 0;
    }

    /// Returns whether this is a navigation key: arrows, Home, End, Page
    /// Up, Page Down.
    public final boolean isNavigationKey() {
        return (mask & KeyCodeClass.NAVIGATION) != 0;
    }

    /// Returns whether this is an arrow key.
    public final boolean isArrowKey() {
        return (mask & KeyCodeClass.ARROW) != 0;
    }

    /// Returns whether this is a modifier key.
    public final boolean isModifierKey() {
        return (mask & KeyCodeClass.MODIFIER) != 0;
    }

    /// Returns whether this is a letter.
    public final boolean isLetterKey() {
        return (mask & KeyCodeClass.LETTER) != 0;
    }

    /// Returns whether this is a digit, on the main block or the keypad.
    public final boolean isDigitKey() {
        return (mask & KeyCodeClass.DIGIT) != 0;
    }

    /// Returns whether this key is on the numeric keypad.
    public final boolean isKeypadKey() {
        return (mask & KeyCodeClass.KEYPAD) != 0;
    }

    /// Returns whether this key types white space.
    public final boolean isWhitespaceKey() {
        return (mask & KeyCodeClass.WHITESPACE) != 0;
    }

    /// Returns whether this is a media key.
    public final boolean isMediaKey() {
        return (mask & KeyCodeClass.MEDIA) != 0;
    }

    /// Returns the name the key is shown by, `Page Up`.
    public final String getName() {
        return name;
    }

    /// Returns the character of the key's code as a string.
    public String getChar() {
        return ch;
    }

    /// Returns the numeric code of the key.
    public int getCode() {
        return code;
    }

    /// Returns the key with a name as [#getName()] answers it, or `null`.
    public static KeyCode getKeyCode(String name) {
        if (byName == null) {
            HashMap<String, KeyCode> map = new HashMap<String, KeyCode>();
            KeyCode[] all = values();
            for (int i = 0; i < all.length; i++) {
                map.put(all[i].name, all[i]);
            }
            byName = map;
        }
        return byName.get(name);
    }
}
