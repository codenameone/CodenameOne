package com.codename1.flutter.services;

/**
 * A physical (scan-code) keyboard key, mirroring Flutter's
 * {@code PhysicalKeyboardKey}. Present so {@code KeyEvent.physicalKey} resolves;
 * new_gallery never compares against it.
 */
public class PhysicalKeyboardKey {

    private final int usbHidUsage;
    private final String debugName;

    public PhysicalKeyboardKey(int usbHidUsage) {
        this(usbHidUsage, null);
    }

    public PhysicalKeyboardKey(int usbHidUsage, String debugName) {
        this.usbHidUsage = usbHidUsage;
        this.debugName = debugName;
    }

    public int usbHidUsage() {
        return usbHidUsage;
    }

    public String debugName() {
        return debugName;
    }
}
