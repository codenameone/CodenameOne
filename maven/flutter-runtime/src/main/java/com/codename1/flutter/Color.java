package com.codename1.flutter;

/**
 * An immutable 32-bit ARGB color, mirroring Flutter's {@code Color}.
 * {@code new Color(0xFF2196F3)} is fully opaque material blue.
 */
public class Color {

    private final int value;

    public Color(int argb) {
        this.value = argb;
    }

    /**
     * The full 32-bit ARGB value.
     */
    public int value() {
        return value;
    }

    public int alpha() {
        return (value >> 24) & 0xFF;
    }

    public int red() {
        return (value >> 16) & 0xFF;
    }

    public int green() {
        return (value >> 8) & 0xFF;
    }

    public int blue() {
        return value & 0xFF;
    }

    /**
     * The 24-bit RGB portion — the form CN1 style colors use.
     */
    public int rgb() {
        return value & 0xFFFFFF;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Color && ((Color) o).value == value;
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public String toString() {
        return "Color(0x" + String.format("%08X", value) + ")";
    }
}
