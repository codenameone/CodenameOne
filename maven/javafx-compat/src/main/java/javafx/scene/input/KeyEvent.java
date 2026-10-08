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

import javafx.beans.NamedArg;
import javafx.event.EventTarget;
import javafx.event.EventType;

/// A key going down, coming up or typing a character. Key events go to the
/// node that has the focus, or to the scene when no node has it.
///
/// A control backed by a native text editor consumes the keys it edits
/// with; while it is being edited the scene does not see them.
public final class KeyEvent extends InputEvent {

    private static final long serialVersionUID = 1L;

    /// Every key event.
    public static final EventType<KeyEvent> ANY = new EventType<KeyEvent>(InputEvent.ANY, "KEY");

    /// A key went down.
    public static final EventType<KeyEvent> KEY_PRESSED = new EventType<KeyEvent>(ANY, "KEY_PRESSED");

    /// A key came up.
    public static final EventType<KeyEvent> KEY_RELEASED = new EventType<KeyEvent>(ANY, "KEY_RELEASED");

    /// A character was typed.
    public static final EventType<KeyEvent> KEY_TYPED = new EventType<KeyEvent>(ANY, "KEY_TYPED");

    /// The character of an event that typed none.
    public static final String CHAR_UNDEFINED = KeyCode.UNDEFINED.getChar();

    private final String character;
    private final String text;
    private final KeyCode code;
    private final boolean shiftDown;
    private final boolean controlDown;
    private final boolean altDown;
    private final boolean metaDown;

    /// Creates a key event.
    public KeyEvent(@NamedArg("source") Object source, @NamedArg("target") EventTarget target,
            @NamedArg("eventType") EventType<KeyEvent> eventType, @NamedArg("character") String character,
            @NamedArg("text") String text, @NamedArg("code") KeyCode code, @NamedArg("shiftDown") boolean shiftDown,
            @NamedArg("controlDown") boolean controlDown, @NamedArg("altDown") boolean altDown,
            @NamedArg("metaDown") boolean metaDown) {
        super(source, target, eventType);
        boolean typed = eventType == KEY_TYPED;
        this.character = typed ? character : CHAR_UNDEFINED;
        this.text = typed ? "" : text;
        this.code = typed ? KeyCode.UNDEFINED : code;
        this.shiftDown = shiftDown;
        this.controlDown = controlDown;
        this.altDown = altDown;
        this.metaDown = metaDown;
    }

    /// Creates a key event with no source and no target.
    public KeyEvent(@NamedArg("eventType") EventType<KeyEvent> eventType, @NamedArg("character") String character,
            @NamedArg("text") String text, @NamedArg("code") KeyCode code, @NamedArg("shiftDown") boolean shiftDown,
            @NamedArg("controlDown") boolean controlDown, @NamedArg("altDown") boolean altDown,
            @NamedArg("metaDown") boolean metaDown) {
        this(null, null, eventType, character, text, code, shiftDown, controlDown, altDown, metaDown);
    }

    /// Returns the character typed; [#CHAR_UNDEFINED] unless this is a
    /// typed event.
    public final String getCharacter() {
        return character;
    }

    /// Returns a description of the key; empty for a typed event.
    public final String getText() {
        return text;
    }

    /// Returns the key; `UNDEFINED` for a typed event.
    public final KeyCode getCode() {
        return code;
    }

    /// Returns whether Shift is down.
    public final boolean isShiftDown() {
        return shiftDown;
    }

    /// Returns whether Control is down.
    public final boolean isControlDown() {
        return controlDown;
    }

    /// Returns whether Alt is down.
    public final boolean isAltDown() {
        return altDown;
    }

    /// Returns whether Meta is down.
    public final boolean isMetaDown() {
        return metaDown;
    }

    /// Returns whether the platform's shortcut modifier is down: Meta on
    /// Apple platforms, Control elsewhere.
    public final boolean isShortcutDown() {
        return SceneInput.shortcutIsMeta() ? metaDown : controlDown;
    }

    @Override
    @SuppressWarnings("unchecked")
    public KeyEvent copyFor(Object newSource, EventTarget newTarget) {
        KeyEvent copy = new KeyEvent(newSource, newTarget, (EventType<KeyEvent>) getEventType(), character, text,
                code, shiftDown, controlDown, altDown, metaDown);
        return copy;
    }

    /// Returns a copy with another source, target and type.
    public KeyEvent copyFor(Object source, EventTarget target, EventType<KeyEvent> type) {
        return new KeyEvent(source, target, type, character, text, code, shiftDown, controlDown, altDown, metaDown);
    }

    @Override
    @SuppressWarnings("unchecked")
    public EventType<KeyEvent> getEventType() {
        return (EventType<KeyEvent>) super.getEventType();
    }

    @Override
    public String toString() {
        return "KeyEvent [source = " + getSource() + ", target = " + getTarget() + ", eventType = "
                + getEventType() + ", consumed = " + isConsumed() + ", character = " + character + ", text = "
                + text + ", code = " + code + "]";
    }
}
