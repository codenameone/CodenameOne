/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bastion.chain;

/// Ethereum addresses: derived from a public key, and checked when a person
/// types one.
///
/// An address is the last 20 bytes of the Keccak-256 of the public key. EIP-55
/// spells it in mixed case so that the case is a checksum: an address typed in
/// mixed case must match it exactly, or one mistyped character is caught
/// before coins go to an address nobody holds. An all-lowercase or
/// all-uppercase address carries no checksum and is accepted as it is, as
/// every wallet does -- the cooling-off on a new withdrawal address is what
/// gives the customer time to notice one they did not mean.
public final class EthAddress {
    private EthAddress() {
    }

    /// The lowercase `0x` address of a 64-byte public key.
    public static String of(byte[] publicKey64) {
        byte[] hash = EthKeys.keccak256(publicKey64);
        byte[] address = new byte[20];
        System.arraycopy(hash, 12, address, 0, 20);
        return "0x" + Hex32.encode(address);
    }

    /// `address` in EIP-55 mixed case.
    public static String checksum(String address) {
        String lower = normalize(address).substring(2);
        String hash = Hex32.encode(EthKeys.keccak256(ascii(lower)));
        StringBuilder out = new StringBuilder("0x");
        for (int iter = 0; iter < lower.length(); iter++) {
            char c = lower.charAt(iter);
            if (c >= 'a' && c <= 'f' && hash.charAt(iter) >= '8') {
                out.append((char) (c - 'a' + 'A'));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /// True when `address` is 0x and 40 hex digits, and -- when written in
    /// mixed case -- its case is the EIP-55 checksum.
    public static boolean valid(String address) {
        if (address == null || address.length() != 42 || !address.startsWith("0x")) {
            return false;
        }
        boolean lower = false;
        boolean upper = false;
        for (int iter = 2; iter < 42; iter++) {
            char c = address.charAt(iter);
            if (c >= 'a' && c <= 'f') {
                lower = true;
            } else if (c >= 'A' && c <= 'F') {
                upper = true;
            } else if (c < '0' || c > '9') {
                return false;
            }
        }
        if (lower && upper) {
            return checksum(address).equals(address);
        }
        return true;
    }

    /// `address` lowercased. It must be 0x and 40 hex digits; the checksum is
    /// [#valid]'s business, not this one's.
    public static String normalize(String address) {
        if (address == null || address.length() != 42 || !address.startsWith("0x")) {
            throw new IllegalArgumentException("not an address");
        }
        StringBuilder out = new StringBuilder(42).append("0x");
        for (int iter = 2; iter < 42; iter++) {
            char c = address.charAt(iter);
            if (c >= 'A' && c <= 'F') {
                c = (char) (c - 'A' + 'a');
            } else if (!(c >= '0' && c <= '9') && !(c >= 'a' && c <= 'f')) {
                throw new IllegalArgumentException("not an address");
            }
            out.append(c);
        }
        return out.toString();
    }

    private static byte[] ascii(String text) {
        byte[] out = new byte[text.length()];
        for (int iter = 0; iter < out.length; iter++) {
            out[iter] = (byte) text.charAt(iter);
        }
        return out;
    }

    /// The 20 bytes of `address`.
    public static byte[] bytes(String address) {
        return Hex32.decode(normalize(address).substring(2));
    }
}
