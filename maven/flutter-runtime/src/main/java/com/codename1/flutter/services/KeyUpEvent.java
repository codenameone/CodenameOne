package com.codename1.flutter.services;

/**
 * A key-release event, mirroring Flutter's {@code KeyUpEvent}.
 */
public class KeyUpEvent extends KeyEvent {

    public KeyUpEvent(LogicalKeyboardKey logicalKey, PhysicalKeyboardKey physicalKey) {
        super(logicalKey, physicalKey, null);
    }
}
