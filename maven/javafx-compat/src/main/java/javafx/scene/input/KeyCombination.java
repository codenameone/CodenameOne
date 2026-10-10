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

import com.codename1.fxcompat.runtime.SceneInput;

/// A key together with the state every modifier must be in for a key
/// event to count as it: what a menu item's accelerator is.
///
/// The only kind in this layer is [KeyCodeCombination], a key named by
/// its [KeyCode]. `KeyCharacterCombination`, which names a key by the
/// character it types on the current keyboard layout, is not part of
/// this layer, so [#valueOf(String)] accepts key names only.
///
/// A device has no keyboard most of the time. An accelerator is matched
/// against the key events a hardware keyboard sends and is otherwise
/// inert; it is never the only way to reach a command.
public abstract class KeyCombination {

    /// Shift must be down.
    public static final Modifier SHIFT_DOWN = new Modifier(KeyCode.SHIFT, ModifierValue.DOWN);
    /// Shift may be down or up.
    public static final Modifier SHIFT_ANY = new Modifier(KeyCode.SHIFT, ModifierValue.ANY);
    /// Control must be down.
    public static final Modifier CONTROL_DOWN = new Modifier(KeyCode.CONTROL, ModifierValue.DOWN);
    /// Control may be down or up.
    public static final Modifier CONTROL_ANY = new Modifier(KeyCode.CONTROL, ModifierValue.ANY);
    /// Alt must be down.
    public static final Modifier ALT_DOWN = new Modifier(KeyCode.ALT, ModifierValue.DOWN);
    /// Alt may be down or up.
    public static final Modifier ALT_ANY = new Modifier(KeyCode.ALT, ModifierValue.ANY);
    /// Meta must be down.
    public static final Modifier META_DOWN = new Modifier(KeyCode.META, ModifierValue.DOWN);
    /// Meta may be down or up.
    public static final Modifier META_ANY = new Modifier(KeyCode.META, ModifierValue.ANY);
    /// The platform's shortcut modifier must be down: Meta on Apple
    /// platforms, Control elsewhere.
    public static final Modifier SHORTCUT_DOWN = new Modifier(KeyCode.SHORTCUT, ModifierValue.DOWN);
    /// The platform's shortcut modifier may be down or up.
    public static final Modifier SHORTCUT_ANY = new Modifier(KeyCode.SHORTCUT, ModifierValue.ANY);

    /// A combination no key event matches.
    public static final KeyCombination NO_MATCH = new KeyCombination() {
        @Override
        public boolean match(KeyEvent event) {
            return false;
        }
    };

    private static final Modifier[] POSSIBLE = {SHIFT_DOWN, SHIFT_ANY, CONTROL_DOWN, CONTROL_ANY, ALT_DOWN, ALT_ANY,
        META_DOWN, META_ANY, SHORTCUT_DOWN, SHORTCUT_ANY};

    private final ModifierValue shift;
    private final ModifierValue control;
    private final ModifierValue alt;
    private final ModifierValue meta;
    private final ModifierValue shortcut;

    /// Creates a combination from the state of each modifier.
    protected KeyCombination(ModifierValue shift, ModifierValue control, ModifierValue alt, ModifierValue meta,
            ModifierValue shortcut) {
        if (shift == null || control == null || alt == null || meta == null || shortcut == null) {
            throw new NullPointerException("Modifier value must not be null!");
        }
        this.shift = shift;
        this.control = control;
        this.alt = alt;
        this.meta = meta;
        this.shortcut = shortcut;
    }

    /// Creates a combination from the modifiers that are not up; naming
    /// one twice is an error.
    protected KeyCombination(Modifier... modifiers) {
        this(value(KeyCode.SHIFT, modifiers), value(KeyCode.CONTROL, modifiers), value(KeyCode.ALT, modifiers),
                value(KeyCode.META, modifiers), value(KeyCode.SHORTCUT, modifiers));
    }

    private static ModifierValue value(KeyCode key, Modifier[] modifiers) {
        ModifierValue found = null;
        if (modifiers != null) {
            for (int i = 0; i < modifiers.length; i++) {
                Modifier m = modifiers[i];
                if (m == null) {
                    throw new NullPointerException("Modifier must not be null!");
                }
                if (m.getKey() == key) {
                    if (found != null) {
                        throw new IllegalArgumentException("Duplicate modifiers [" + m.getKey().getName() + "]");
                    }
                    found = m.getValue();
                }
            }
        }
        return found == null ? ModifierValue.UP : found;
    }

    /// Returns the state Shift must be in.
    public final ModifierValue getShift() {
        return shift;
    }

    /// Returns the state Control must be in.
    public final ModifierValue getControl() {
        return control;
    }

    /// Returns the state Alt must be in.
    public final ModifierValue getAlt() {
        return alt;
    }

    /// Returns the state Meta must be in.
    public final ModifierValue getMeta() {
        return meta;
    }

    /// Returns the state the platform's shortcut modifier must be in.
    public final ModifierValue getShortcut() {
        return shortcut;
    }

    private static boolean agrees(ModifierValue wanted, boolean down) {
        return wanted == ModifierValue.ANY || (wanted == ModifierValue.DOWN) == down;
    }

    /// Returns whether the modifiers of a key event are what this
    /// combination asks for. The shortcut modifier stands for Meta or
    /// Control, whichever the platform uses, and the state asked of that
    /// key is then the stronger of the two.
    public boolean match(KeyEvent event) {
        boolean shortcutIsMeta = SceneInput.shortcutIsMeta();
        ModifierValue wantControl = control;
        ModifierValue wantMeta = meta;
        if (shortcut != ModifierValue.UP) {
            if (shortcutIsMeta) {
                wantMeta = stronger(meta, shortcut);
            } else {
                wantControl = stronger(control, shortcut);
            }
        }
        return agrees(shift, event.isShiftDown()) && agrees(wantControl, event.isControlDown())
                && agrees(alt, event.isAltDown()) && agrees(wantMeta, event.isMetaDown());
    }

    private static ModifierValue stronger(ModifierValue a, ModifierValue b) {
        if (a == ModifierValue.DOWN || b == ModifierValue.DOWN) {
            return ModifierValue.DOWN;
        }
        return a == ModifierValue.ANY || b == ModifierValue.ANY ? ModifierValue.ANY : ModifierValue.UP;
    }

    /// Returns the modifiers as [#valueOf(String)] reads them, each
    /// followed by a plus sign: `Shift+Ctrl+`.
    public String getName() {
        StringBuilder sb = new StringBuilder();
        append(sb, shift, "Shift");
        append(sb, control, "Ctrl");
        append(sb, alt, "Alt");
        append(sb, meta, "Meta");
        append(sb, shortcut, "Shortcut");
        return sb.toString();
    }

    private static void append(StringBuilder sb, ModifierValue value, String name) {
        if (value == ModifierValue.DOWN) {
            sb.append(name).append('+');
        } else if (value == ModifierValue.ANY) {
            sb.append("Ignore ").append(name).append('+');
        }
    }

    /// Returns the modifiers as a menu shows them, the shortcut modifier
    /// by the name of the key it stands for.
    public String getDisplayText() {
        StringBuilder sb = new StringBuilder();
        if (shift == ModifierValue.DOWN) {
            sb.append("Shift+");
        }
        boolean shortcutIsMeta = SceneInput.shortcutIsMeta();
        if (control == ModifierValue.DOWN || (shortcut == ModifierValue.DOWN && !shortcutIsMeta)) {
            sb.append("Ctrl+");
        }
        if (alt == ModifierValue.DOWN) {
            sb.append("Alt+");
        }
        if (meta == ModifierValue.DOWN || (shortcut == ModifierValue.DOWN && shortcutIsMeta)) {
            sb.append("Meta+");
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof KeyCombination)) {
            return false;
        }
        KeyCombination other = (KeyCombination) obj;
        return shift == other.shift && control == other.control && alt == other.alt && meta == other.meta
                && shortcut == other.shortcut;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 23 * hash + shift.ordinal();
        hash = 23 * hash + control.ordinal();
        hash = 23 * hash + alt.ordinal();
        hash = 23 * hash + meta.ordinal();
        hash = 23 * hash + shortcut.ordinal();
        return hash;
    }

    @Override
    public String toString() {
        return getName();
    }

    /// Reads a combination written as modifier names and one key name
    /// joined by plus signs, `Shortcut+Shift+S`; an FXML `accelerator`
    /// attribute is read this way. The modifier names are `Shift`, `Ctrl`,
    /// `Alt`, `Meta` and `Shortcut`, each optionally after `Ignore `, and
    /// the key is named as `KeyCode.getName()` or the enum constant spell
    /// it.
    public static KeyCombination valueOf(String value) {
        return keyCombination(value);
    }

    /// Reads a combination; see [#valueOf(String)].
    public static KeyCombination keyCombination(String name) {
        if (name == null) {
            throw new NullPointerException("Key combination name must not be null!");
        }
        ModifierValue[] values = {ModifierValue.UP, ModifierValue.UP, ModifierValue.UP, ModifierValue.UP,
            ModifierValue.UP};
        boolean[] seen = new boolean[5];
        KeyCode key = null;
        int from = 0;
        int n = name.length();
        while (from <= n) {
            int plus = name.indexOf('+', from);
            // A trailing plus sign is the key itself.
            if (plus < 0 || plus == n - 1) {
                plus = n;
            }
            String token = name.substring(from, plus).trim();
            from = plus + 1;
            if (token.length() == 0) {
                throw new IllegalArgumentException("Key combination name not valid: " + name);
            }
            Modifier modifier = modifier(token);
            if (modifier != null) {
                int slot = slot(modifier.getKey());
                if (seen[slot]) {
                    throw new IllegalArgumentException("Duplicate modifiers [" + modifier.getKey().getName() + "]");
                }
                seen[slot] = true;
                values[slot] = modifier.getValue();
                continue;
            }
            if (key != null || from <= n) {
                throw new IllegalArgumentException("Key combination name not valid: " + name);
            }
            key = key(token);
            if (key == null) {
                throw new IllegalArgumentException("Unknown key in a key combination: " + token
                        + " (keys are named as KeyCode names them; a key named by the character it types is "
                        + "not supported)");
            }
        }
        if (key == null) {
            throw new IllegalArgumentException("Key combination name not valid: " + name);
        }
        return new KeyCodeCombination(key, values[0], values[1], values[2], values[3], values[4]);
    }

    private static int slot(KeyCode key) {
        if (key == KeyCode.SHIFT) {
            return 0;
        }
        if (key == KeyCode.CONTROL) {
            return 1;
        }
        if (key == KeyCode.ALT) {
            return 2;
        }
        return key == KeyCode.META ? 3 : 4;
    }

    private static Modifier modifier(String token) {
        for (int i = 0; i < POSSIBLE.length; i++) {
            if (POSSIBLE[i].toString().equals(token)) {
                return POSSIBLE[i];
            }
        }
        return null;
    }

    private static KeyCode key(String token) {
        KeyCode byName = KeyCode.getKeyCode(token);
        if (byName != null) {
            return byName;
        }
        KeyCode[] all = KeyCode.values();
        for (int i = 0; i < all.length; i++) {
            if (all[i].name().equals(token) || all[i].getName().equalsIgnoreCase(token)) {
                return all[i];
            }
        }
        return null;
    }

    /// One modifier key and the state a combination asks of it.
    public static final class Modifier {

        private final KeyCode key;
        private final ModifierValue value;

        private Modifier(KeyCode key, ModifierValue value) {
            this.key = key;
            this.value = value;
        }

        /// Returns the modifier key.
        public KeyCode getKey() {
            return key;
        }

        /// Returns the state asked of the key.
        public ModifierValue getValue() {
            return value;
        }

        @Override
        public String toString() {
            return (value == ModifierValue.ANY ? "Ignore " : "") + key.getName();
        }
    }

    /// The state a combination asks of a modifier key.
    public enum ModifierValue {
        /// The key must be down.
        DOWN,
        /// The key must be up.
        UP,
        /// The key may be either.
        ANY
    }
}
