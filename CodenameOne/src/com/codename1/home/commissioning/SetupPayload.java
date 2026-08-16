/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.home.commissioning;

/// A Matter onboarding payload: the `MT:` string behind the QR code on an
/// accessory, or the 11 or 21 digit code printed under it.
///
/// #### Why this is parsed here rather than handed straight to the platform
///
/// Commissioning opens an operating-system sheet, and if the payload is
/// garbage that sheet fails with the OS's own wording -- which tells a user
/// nothing about the fact that they scanned the barcode on the box instead of
/// the one on the device. Parsing first means a wrong code is caught in your
/// app, in your words, before anything is opened.
///
/// It also makes a scanner screen possible: [#getVendorId()] and
/// [#getProductId()] identify the accessory before it has joined anything, so
/// a UI can say what it is about to add.
///
/// #### What is deliberately not here
///
/// There is no way to **generate** a payload, and [#getPasscode()] exists
/// because the format contains it, not because anything in this API wants it.
/// Treat the passcode the way you would any pairing secret: do not log it and
/// do not persist it.
///
/// Only the standard compact encoding is understood. A vendor-extended TLV
/// payload -- the long form some accessories use to carry extra data -- is
/// rejected by [#parse(java.lang.String)] rather than half-read; the platform
/// commissioning UI understands those, so pass the raw string through with
/// [CommissioningRequest#setRawSetupPayload(java.lang.String)] when this
/// refuses one.
public final class SetupPayload {

    /// The base-38 alphabet the Matter QR payload uses.
    private static final String BASE38 =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-.";

    /// The QR payload prefix.
    private static final String QR_PREFIX = "MT:";

    /// Bit widths of the compact QR encoding, in order.
    private static final int VERSION_BITS = 3;
    private static final int VENDOR_BITS = 16;
    private static final int PRODUCT_BITS = 16;
    private static final int CUSTOM_FLOW_BITS = 2;
    private static final int DISCOVERY_BITS = 8;
    private static final int DISCRIMINATOR_BITS = 12;
    private static final int PASSCODE_BITS = 27;

    /// The compact encoding is 88 bits, so 11 bytes, so 18 base-38 characters
    /// (three groups of three bytes at five characters each, plus two bytes at
    /// four).
    private static final int QR_PAYLOAD_BYTES = 11;
    private static final int QR_PAYLOAD_CHARS = 19;

    /// Verhoeff multiplication table, used by the manual code's check digit.
    private static final int[][] VERHOEFF_D = {
        {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
        {1, 2, 3, 4, 0, 6, 7, 8, 9, 5},
        {2, 3, 4, 0, 1, 7, 8, 9, 5, 6},
        {3, 4, 0, 1, 2, 8, 9, 5, 6, 7},
        {4, 0, 1, 2, 3, 9, 5, 6, 7, 8},
        {5, 9, 8, 7, 6, 0, 4, 3, 2, 1},
        {6, 5, 9, 8, 7, 1, 0, 4, 3, 2},
        {7, 6, 5, 9, 8, 2, 1, 0, 4, 3},
        {8, 7, 6, 5, 9, 3, 2, 1, 0, 4},
        {9, 8, 7, 6, 5, 4, 3, 2, 1, 0}
    };

    /// Verhoeff permutation table.
    private static final int[][] VERHOEFF_P = {
        {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
        {1, 5, 7, 6, 2, 8, 3, 0, 9, 4},
        {5, 8, 0, 3, 7, 9, 6, 1, 4, 2},
        {8, 9, 1, 6, 0, 4, 3, 5, 2, 7},
        {9, 4, 5, 3, 1, 2, 6, 8, 7, 0},
        {4, 2, 8, 6, 5, 7, 3, 9, 0, 1},
        {2, 7, 9, 3, 8, 0, 6, 4, 1, 5},
        {7, 0, 4, 6, 9, 1, 3, 2, 5, 8}
    };

    /// Wi-Fi discovery bit of [#getDiscoveryCapabilities()].
    public static final int DISCOVERY_SOFT_AP = 0x01;

    /// Bluetooth Low Energy discovery bit of [#getDiscoveryCapabilities()].
    public static final int DISCOVERY_BLE = 0x02;

    /// On-network discovery bit of [#getDiscoveryCapabilities()] -- the
    /// accessory is already on the IP network and is discoverable over mDNS.
    public static final int DISCOVERY_ON_NETWORK = 0x04;

    private final String raw;
    private final int version;
    private final int vendorId;
    private final int productId;
    private final int customFlow;
    private final int discoveryCapabilities;
    private final int discriminator;
    private final int passcode;
    private final boolean fromQrCode;
    private final boolean shortDiscriminator;

    private SetupPayload(String raw, int version, int vendorId, int productId,
            int customFlow, int discoveryCapabilities, int discriminator,
            int passcode, boolean fromQrCode, boolean shortDiscriminator) {
        this.raw = raw;
        this.version = version;
        this.vendorId = vendorId;
        this.productId = productId;
        this.customFlow = customFlow;
        this.discoveryCapabilities = discoveryCapabilities;
        this.discriminator = discriminator;
        this.passcode = passcode;
        this.fromQrCode = fromQrCode;
        this.shortDiscriminator = shortDiscriminator;
    }

    /// Parses a scanned QR payload or a typed manual code.
    ///
    /// Accepts the `MT:` QR form and the 11 and 21 digit manual forms, and
    /// tolerates the separators people type into a manual code -- spaces and
    /// hyphens are stripped before the digits are read.
    ///
    /// #### Parameters
    ///
    /// - `text`: the scanned or typed code
    ///
    /// #### Returns
    ///
    /// the parsed payload
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when the text is not a Matter setup
    ///   payload, when its check digit does not match, or when it is a
    ///   vendor-extended payload this parser does not read. The message says
    ///   which, because it is written to be shown to whoever is holding the
    ///   accessory.
    public static SetupPayload parse(String text) {
        if (text == null) {
            throw new IllegalArgumentException(
                    "no setup code was supplied");
        }
        String trimmed = text.trim();
        if (trimmed.length() == 0) {
            throw new IllegalArgumentException(
                    "no setup code was supplied");
        }
        if (startsWithIgnoreCase(trimmed, QR_PREFIX)) {
            return parseQr(trimmed);
        }
        return parseManual(trimmed);
    }

    /// Whether a string looks like something [#parse(java.lang.String)] would
    /// accept, without throwing when it is not.
    ///
    /// For a scanner that is looking at every barcode in view and needs to
    /// ignore the ones that are not setup codes.
    ///
    /// #### Parameters
    ///
    /// - `text`: the candidate, or `null`
    ///
    /// #### Returns
    ///
    /// `true` when parsing would succeed
    public static boolean isValid(String text) {
        if (text == null) {
            return false;
        }
        try {
            parse(text);
            return true;
        } catch (IllegalArgumentException notAPayload) {
            // Asked as a question, so a "no" is the answer rather than a
            // failure. The exception carries the wording for a user who
            // scanned the wrong thing on purpose; a scanner sweeping every
            // barcode in the frame does not want it.
            return false;
        }
    }

    private static boolean startsWithIgnoreCase(String s, String prefix) {
        if (s.length() < prefix.length()) {
            return false;
        }
        return s.substring(0, prefix.length()).toUpperCase()
                .equals(prefix.toUpperCase());
    }

    private static SetupPayload parseQr(String text) {
        String body = text.substring(QR_PREFIX.length()).toUpperCase();
        if (body.length() != QR_PAYLOAD_CHARS) {
            throw new IllegalArgumentException(
                    "this is not a standard Matter QR payload: expected "
                            + QR_PAYLOAD_CHARS + " characters after \"MT:\""
                            + " and found " + body.length()
                            + ". Vendor-extended payloads are not read here;"
                            + " pass the code through unparsed instead.");
        }
        byte[] bytes = decodeBase38(body);
        int offset = 0;
        int version = (int) readBits(bytes, offset, VERSION_BITS);
        offset += VERSION_BITS;
        int vendorId = (int) readBits(bytes, offset, VENDOR_BITS);
        offset += VENDOR_BITS;
        int productId = (int) readBits(bytes, offset, PRODUCT_BITS);
        offset += PRODUCT_BITS;
        int customFlow = (int) readBits(bytes, offset, CUSTOM_FLOW_BITS);
        offset += CUSTOM_FLOW_BITS;
        int discovery = (int) readBits(bytes, offset, DISCOVERY_BITS);
        offset += DISCOVERY_BITS;
        int discriminator = (int) readBits(bytes, offset, DISCRIMINATOR_BITS);
        offset += DISCRIMINATOR_BITS;
        int passcode = (int) readBits(bytes, offset, PASSCODE_BITS);
        validatePasscode(passcode);
        return new SetupPayload(text, version, vendorId, productId, customFlow,
                discovery, discriminator, passcode, true, false);
    }

    private static byte[] decodeBase38(String body) {
        byte[] out = new byte[QR_PAYLOAD_BYTES];
        int outPos = 0;
        int pos = 0;
        while (pos < body.length()) {
            int chars = body.length() - pos >= 5 ? 5 : body.length() - pos;
            int bytesOut;
            if (chars == 5) {
                bytesOut = 3;
            } else if (chars == 4) {
                bytesOut = 2;
            } else if (chars == 2) {
                bytesOut = 1;
            } else {
                throw new IllegalArgumentException(
                        "this setup code is not valid base-38: it has a"
                                + " trailing group of " + chars
                                + " characters, which encodes nothing");
            }
            long value = 0;
            long multiplier = 1;
            for (int i = 0; i < chars; i++) {
                char c = body.charAt(pos + i);
                int index = BASE38.indexOf(c);
                if (index < 0) {
                    throw new IllegalArgumentException("'" + c
                            + "' cannot appear in a Matter setup code");
                }
                value += index * multiplier;
                multiplier *= 38;
            }
            for (int i = 0; i < bytesOut; i++) {
                if (outPos >= out.length) {
                    throw new IllegalArgumentException(
                            "this setup code decodes to more data than a"
                                    + " standard Matter payload holds");
                }
                out[outPos++] = (byte) ((value >> (8 * i)) & 0xFF);
            }
            pos += chars;
        }
        if (outPos != QR_PAYLOAD_BYTES) {
            throw new IllegalArgumentException(
                    "this setup code decodes to " + outPos
                            + " bytes rather than the " + QR_PAYLOAD_BYTES
                            + " a standard Matter payload holds");
        }
        return out;
    }

    /// Reads `count` bits starting at `offset`, least significant bit first
    /// within each byte and least significant byte first -- the order the
    /// Matter compact encoding packs them in.
    private static long readBits(byte[] bytes, int offset, int count) {
        long value = 0;
        for (int i = 0; i < count; i++) {
            int bit = offset + i;
            int b = (bytes[bit >> 3] >> (bit & 7)) & 1;
            value |= ((long) b) << i;
        }
        return value;
    }

    private static SetupPayload parseManual(String text) {
        StringBuilder digitsOnly = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '0' && c <= '9') {
                digitsOnly.append(c);
            } else if (c != '-' && c != ' ') {
                throw new IllegalArgumentException(
                        "a Matter setup code is either a scanned code"
                                + " starting \"MT:\" or 11 or 21 digits;"
                                + " '" + c + "' is neither");
            }
        }
        String digits = digitsOnly.toString();
        boolean hasVendorProduct;
        if (digits.length() == 11) {
            hasVendorProduct = false;
        } else if (digits.length() == 21) {
            hasVendorProduct = true;
        } else {
            throw new IllegalArgumentException(
                    "a typed Matter setup code is 11 or 21 digits; this one"
                            + " has " + digits.length());
        }
        if (!verhoeffValid(digits)) {
            throw new IllegalArgumentException(
                    "this setup code's check digit does not match, so a digit"
                            + " was mistyped or misread");
        }
        int first = digits.charAt(0) - '0';
        int vidPidPresent = (first >> 2) & 0x01;
        if ((vidPidPresent == 1) != hasVendorProduct) {
            throw new IllegalArgumentException(
                    "this setup code's length and its own header disagree"
                            + " about whether it carries a vendor and product"
                            + " id");
        }
        int discriminatorHigh = first & 0x03;
        int group2 = parseDigits(digits, 1, 5);
        int group3 = parseDigits(digits, 6, 4);
        int discriminatorMid = (group2 >> 14) & 0x03;
        int passcode = (group2 & 0x3FFF) | (group3 << 14);
        validatePasscode(passcode);
        // The manual code carries four discriminator bits where the QR
        // carries twelve, so it identifies a smaller set of accessories.
        // Shifted into the same 12-bit field the QR uses, with the missing
        // low bits zero, and flagged so nothing downstream mistakes it for a
        // full one.
        int discriminator = ((discriminatorHigh << 2) | discriminatorMid) << 8;
        int vendorId = 0;
        int productId = 0;
        if (hasVendorProduct) {
            vendorId = parseDigits(digits, 10, 5);
            productId = parseDigits(digits, 15, 5);
        }
        return new SetupPayload(text, 0, vendorId, productId, 0,
                DISCOVERY_BLE | DISCOVERY_ON_NETWORK, discriminator, passcode,
                false, true);
    }

    private static int parseDigits(String digits, int from, int count) {
        int value = 0;
        for (int i = 0; i < count; i++) {
            value = value * 10 + (digits.charAt(from + i) - '0');
        }
        return value;
    }

    /// Rejects the passcodes the Matter specification forbids.
    ///
    /// These are not arbitrary: a passcode of all one digit, or an ascending
    /// or descending run, is exactly what a vendor ships when they have not
    /// generated one, and a commissioning attempt against it is a security
    /// problem rather than a typo. Catching it here says so plainly instead of
    /// letting the OS sheet fail.
    private static void validatePasscode(int passcode) {
        if (passcode == 0 || passcode == 11111111 || passcode == 22222222
                || passcode == 33333333 || passcode == 44444444
                || passcode == 55555555 || passcode == 66666666
                || passcode == 77777777 || passcode == 88888888
                || passcode == 99999999 || passcode == 12345678
                || passcode == 87654321) {
            throw new IllegalArgumentException(
                    "this setup code carries a passcode the Matter"
                            + " specification forbids, which usually means the"
                            + " accessory shipped without one being generated");
        }
        if (passcode < 0 || passcode > 99999998) {
            throw new IllegalArgumentException(
                    "this setup code's passcode is outside the range Matter"
                            + " allows");
        }
    }

    private static boolean verhoeffValid(String digits) {
        int c = 0;
        int length = digits.length();
        for (int i = 0; i < length; i++) {
            int digit = digits.charAt(length - i - 1) - '0';
            c = VERHOEFF_D[c][VERHOEFF_P[i % 8][digit]];
        }
        return c == 0;
    }

    /// The code exactly as it was supplied, for passing through to a platform
    /// that would rather parse it itself.
    ///
    /// #### Returns
    ///
    /// the original text, never `null`
    public String getRaw() {
        return raw;
    }

    /// The payload format version. Zero for a manual code, which does not
    /// carry one.
    ///
    /// #### Returns
    ///
    /// the version
    public int getVersion() {
        return version;
    }

    /// The Matter vendor id of the accessory.
    ///
    /// Zero when unknown -- an 11-digit manual code does not carry one.
    ///
    /// #### Returns
    ///
    /// the vendor id, or zero
    public int getVendorId() {
        return vendorId;
    }

    /// The vendor's product id for the accessory. Zero when unknown; see
    /// [#getVendorId()].
    ///
    /// #### Returns
    ///
    /// the product id, or zero
    public int getProductId() {
        return productId;
    }

    /// Whether the vendor requires their own app to finish setup.
    ///
    /// A non-zero custom flow means the accessory's manufacturer has declared
    /// that standard commissioning alone will not fully configure it -- so it
    /// may join the home and still not work until the user opens the vendor's
    /// app. Worth telling the user before they start rather than after.
    ///
    /// #### Returns
    ///
    /// the custom-flow code; zero for a standard accessory
    public int getCustomFlow() {
        return customFlow;
    }

    /// How the accessory can be found, as a mask of [#DISCOVERY_SOFT_AP],
    /// [#DISCOVERY_BLE] and [#DISCOVERY_ON_NETWORK].
    ///
    /// A manual code does not carry this, so one is reported as BLE and
    /// on-network, which is what the platform will try anyway.
    ///
    /// #### Returns
    ///
    /// the discovery mask
    public int getDiscoveryCapabilities() {
        return discoveryCapabilities;
    }

    /// The discriminator that picks this accessory out of several in pairing
    /// mode at once.
    ///
    /// #### Returns
    ///
    /// the 12-bit discriminator
    public int getDiscriminator() {
        return discriminator;
    }

    /// Whether [#getDiscriminator()] is the short four-bit form.
    ///
    /// A manual code carries only the top four bits, so it narrows the field
    /// rather than identifying one accessory. Two devices in pairing mode in
    /// the same room can both match a typed code, which is a real situation and
    /// the reason the platform's own UI may still ask the user to choose.
    ///
    /// #### Returns
    ///
    /// `true` when only the top four bits are meaningful
    public boolean isShortDiscriminator() {
        return shortDiscriminator;
    }

    /// The pairing passcode.
    ///
    /// A secret. Do not log it, do not persist it, and do not display it
    /// beyond echoing back what the user just typed.
    ///
    /// #### Returns
    ///
    /// the passcode
    public int getPasscode() {
        return passcode;
    }

    /// Whether this came from a scanned QR payload rather than a typed code.
    ///
    /// #### Returns
    ///
    /// `true` for a QR payload
    public boolean isFromQrCode() {
        return fromQrCode;
    }

    /// A description safe to log or display.
    ///
    /// Names the vendor and product and **omits the passcode**, which is why
    /// it exists at all -- the obvious `toString` would have put a pairing
    /// secret into a log the first time someone debugged a scanner.
    ///
    /// #### Returns
    ///
    /// the description, never `null`
    @Override
    public String toString() {
        return "SetupPayload[vendor=" + vendorId + " product=" + productId
                + " discriminator=" + discriminator
                + (shortDiscriminator ? " (short)" : "")
                + (fromQrCode ? " qr" : " manual") + "]";
    }
}
