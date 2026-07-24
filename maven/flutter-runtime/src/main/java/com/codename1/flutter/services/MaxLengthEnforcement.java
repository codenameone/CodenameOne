package com.codename1.flutter.services;

/**
 * How a text field's {@code maxLength} is enforced — Flutter's
 * {@code MaxLengthEnforcement}. new_gallery's text_field_demo passes
 * {@link #none} to disable enforcement while still showing the counter.
 */
public enum MaxLengthEnforcement {
    /** No enforcement; text may exceed {@code maxLength}. */
    none,
    /** Prevent input beyond {@code maxLength}. */
    enforced,
    /** Enforce only once the IME composing region resolves. */
    truncateAfterCompositionEnds
}
