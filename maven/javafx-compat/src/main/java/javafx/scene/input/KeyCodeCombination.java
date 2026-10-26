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

import javafx.beans.NamedArg;

/// A key combination whose key is named by its [KeyCode]: the Q of
/// `Shortcut+Q`.
public final class KeyCodeCombination extends KeyCombination {

    private final KeyCode code;

    /// Creates a combination from a key and the state of each modifier.
    public KeyCodeCombination(@NamedArg("code") KeyCode code, @NamedArg("shift") ModifierValue shift,
            @NamedArg("control") ModifierValue control, @NamedArg("alt") ModifierValue alt,
            @NamedArg("meta") ModifierValue meta, @NamedArg("shortcut") ModifierValue shortcut) {
        super(shift, control, alt, meta, shortcut);
        this.code = valid(code);
    }

    /// Creates a combination from a key and the modifiers that are not up.
    public KeyCodeCombination(@NamedArg("code") KeyCode code, @NamedArg("modifiers") Modifier... modifiers) {
        super(modifiers);
        this.code = valid(code);
    }

    private static KeyCode valid(KeyCode code) {
        if (code == null) {
            throw new NullPointerException("Key code must not be null!");
        }
        if (code.isModifierKey() || code == KeyCode.UNDEFINED) {
            throw new IllegalArgumentException("Key code must not match modifier key!");
        }
        return code;
    }

    /// Returns the key.
    public KeyCode getCode() {
        return code;
    }

    @Override
    public boolean match(KeyEvent event) {
        return event.getCode() == code && super.match(event);
    }

    @Override
    public String getName() {
        return super.getName() + code.getName();
    }

    @Override
    public String getDisplayText() {
        return super.getDisplayText() + code.getName();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof KeyCodeCombination)) {
            return false;
        }
        return code == ((KeyCodeCombination) obj).code && super.equals(obj);
    }

    @Override
    public int hashCode() {
        return 23 * super.hashCode() + code.ordinal();
    }
}
