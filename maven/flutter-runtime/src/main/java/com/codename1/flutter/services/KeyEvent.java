package com.codename1.flutter.services;

import dart.core.Duration;

/**
 * Base class for a keyboard event in Flutter's modern {@code HardwareKeyboard}
 * API ({@code KeyEvent}). The {@code Focus.onKeyEvent} / {@code KeyboardListener}
 * callbacks receive one of the concrete subclasses ({@link KeyDownEvent},
 * {@link KeyUpEvent}, {@link KeyRepeatEvent}); code switches on the runtime type
 * with {@code event is KeyDownEvent} and reads {@link #logicalKey()}.
 */
public abstract class KeyEvent {

    final LogicalKeyboardKey logicalKey;
    final PhysicalKeyboardKey physicalKey;
    final String character;
    final Duration timeStamp;

    KeyEvent(LogicalKeyboardKey logicalKey, PhysicalKeyboardKey physicalKey, String character) {
        this.logicalKey = logicalKey;
        this.physicalKey = physicalKey;
        this.character = character;
        this.timeStamp = null;
    }

    /** The logical (layout-dependent) key for this event. */
    public LogicalKeyboardKey logicalKey() {
        return logicalKey;
    }

    /** The physical (scan-code) key for this event. */
    public PhysicalKeyboardKey physicalKey() {
        return physicalKey;
    }

    /** The character produced, or {@code null} for non-printable keys. */
    public String character() {
        return character;
    }

    /** The event time, relative to an arbitrary epoch. */
    public Duration timeStamp() {
        return timeStamp;
    }
}
