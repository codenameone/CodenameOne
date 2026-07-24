package com.codename1.flutter.services;

import com.codename1.flutter.TextEditingValue;

/**
 * Filters edited text against a pattern — Flutter's
 * {@code FilteringTextInputFormatter}. new_gallery uses the {@link #digitsOnly}
 * preset; the {@link #allow}/{@link #deny} factories and
 * {@link #singleLineFormatter} are provided for API fidelity. This milestone
 * captures the API shape; the actual character filtering is deferred, so the
 * default {@link #formatEditUpdate} passes the edit through unchanged.
 */
public class FilteringTextInputFormatter extends TextInputFormatter {

    /** Allows only decimal digits ({@code 0-9}). */
    public static final FilteringTextInputFormatter digitsOnly = new FilteringTextInputFormatter();

    /** Collapses newlines so the field stays single-line. */
    public static final FilteringTextInputFormatter singleLineFormatter = new FilteringTextInputFormatter();

    public FilteringTextInputFormatter() {
    }

    /** Dart's {@code FilteringTextInputFormatter.allow} named constructor. */
    public static FilteringTextInputFormatter allow(Object filterPattern, String replacementString) {
        return new FilteringTextInputFormatter();
    }

    /** Dart's {@code FilteringTextInputFormatter.deny} named constructor. */
    public static FilteringTextInputFormatter deny(Object filterPattern, String replacementString) {
        return new FilteringTextInputFormatter();
    }

    @Override
    public TextEditingValue formatEditUpdate(TextEditingValue oldValue, TextEditingValue newValue) {
        return newValue;
    }
}
