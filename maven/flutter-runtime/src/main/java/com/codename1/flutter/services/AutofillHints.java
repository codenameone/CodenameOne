package com.codename1.flutter.services;

/**
 * The set of autofill hint strings a text input can advertise to the platform
 * autofill service — Flutter's {@code AutofillHints}. Each constant is the
 * canonical hint token; only a representative subset is modelled here.
 */
public final class AutofillHints {

    private AutofillHints() {
    }

    public static final String username = "username";
    public static final String password = "password";
    public static final String newPassword = "newPassword";
    public static final String email = "email";
    public static final String name = "name";
    public static final String givenName = "givenName";
    public static final String familyName = "familyName";
    public static final String telephoneNumber = "telephoneNumber";
    public static final String oneTimeCode = "oneTimeCode";
    public static final String streetAddressLine1 = "streetAddressLine1";
    public static final String streetAddressLine2 = "streetAddressLine2";
    public static final String postalCode = "postalCode";
    public static final String creditCardNumber = "creditCardNumber";
    public static final String countryName = "countryName";
}
