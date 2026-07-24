package com.codename1.flutter.services;

import com.codename1.flutter.TextEditingValue;

/**
 * Truncates edited text to a maximum length — Flutter's
 * {@code LengthLimitingTextInputFormatter}. API-shape only for this milestone;
 * the default {@link #formatEditUpdate} passes the edit through unchanged.
 */
public class LengthLimitingTextInputFormatter extends TextInputFormatter {

    private Long maxLength;
    private MaxLengthEnforcement maxLengthEnforcement;

    public LengthLimitingTextInputFormatter(Long maxLength) {
        this.maxLength = maxLength;
    }

    public void maxLengthEnforcement(MaxLengthEnforcement v) {
        this.maxLengthEnforcement = v;
    }

    public Long getMaxLength() {
        return maxLength;
    }

    public MaxLengthEnforcement getMaxLengthEnforcement() {
        return maxLengthEnforcement;
    }

    @Override
    public TextEditingValue formatEditUpdate(TextEditingValue oldValue, TextEditingValue newValue) {
        return newValue;
    }
}
