package com.codename1.flutter.services;

/**
 * The kind of soft keyboard for a text field — Flutter's {@code TextInputType}.
 * Flutter models these as static const instances; the constant names are what
 * matter for the app, so an enum with the matching names is sufficient here.
 */
public enum TextInputType {
    text, multiline, number, phone, datetime, emailAddress, url,
    visiblePassword, name, streetAddress, none
}
