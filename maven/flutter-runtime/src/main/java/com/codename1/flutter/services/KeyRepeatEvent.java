package com.codename1.flutter.services;

/**
 * A key auto-repeat event, mirroring Flutter's {@code KeyRepeatEvent}. Matched
 * with {@code event is KeyRepeatEvent} in key handlers.
 */
public class KeyRepeatEvent extends KeyEvent {

    public KeyRepeatEvent(LogicalKeyboardKey logicalKey, PhysicalKeyboardKey physicalKey, String character) {
        super(logicalKey, physicalKey, character);
    }
}
