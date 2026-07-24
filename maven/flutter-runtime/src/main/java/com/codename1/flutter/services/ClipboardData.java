package com.codename1.flutter.services;

/**
 * Payload for {@link Clipboard}, mirroring Flutter's {@code ClipboardData}.
 */
public class ClipboardData {

    private String text;

    /** Named-parameter constructor {@code ClipboardData({text})}. */
    public ClipboardData(String text) {
        this.text = text;
    }

    /** Allocate-then-setters form for the {@code ClipboardData(text: ...)} named constructor. */
    public ClipboardData() {
    }

    public void text(String v) {
        this.text = v;
    }

    public String text() {
        return text;
    }
}
