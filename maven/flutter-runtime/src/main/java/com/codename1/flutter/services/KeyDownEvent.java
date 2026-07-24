package com.codename1.flutter.services;

/**
 * A key-press event, mirroring Flutter's {@code KeyDownEvent}. Matched with
 * {@code event is KeyDownEvent} in key handlers.
 */
public class KeyDownEvent extends KeyEvent {

    public KeyDownEvent(LogicalKeyboardKey logicalKey, PhysicalKeyboardKey physicalKey, String character) {
        super(logicalKey, physicalKey, character);
    }
}
