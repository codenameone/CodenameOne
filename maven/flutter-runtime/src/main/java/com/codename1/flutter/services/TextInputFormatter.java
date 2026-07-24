package com.codename1.flutter.services;

import com.codename1.flutter.TextEditingValue;

/**
 * The base class every text input formatter extends — Flutter's
 * {@code TextInputFormatter}. Transpiled Dart may subclass this (as
 * new_gallery's {@code _UsNumberTextInputFormatter} does) and override
 * {@link #formatEditUpdate}. The default implementation is the identity
 * transform (the new value passes through unchanged).
 */
public class TextInputFormatter {

    public TextInputFormatter() {
    }

    /**
     * Transforms an edit from {@code oldValue} to {@code newValue}, returning
     * the value that should actually be applied. The default is the identity.
     */
    public TextEditingValue formatEditUpdate(TextEditingValue oldValue, TextEditingValue newValue) {
        return newValue;
    }
}
