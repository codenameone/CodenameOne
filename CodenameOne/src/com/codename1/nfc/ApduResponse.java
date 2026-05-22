/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.nfc;

/// Helpers for working with the ISO 7816 status word (SW1/SW2) trailer at
/// the end of every APDU response. Pair with [IsoDep] (reader mode) and
/// [HostCardEmulationService] (card mode).
public final class ApduResponse {

    private ApduResponse() {
    }

    // Status-word constants are exposed as accessor methods (returning a
    // fresh array each call) rather than public static byte[] fields,
    // because mutable static arrays would let one HCE service corrupt the
    // shared value for every other caller (SpotBugs MS_PKGPROTECT). Each
    // method allocates a 2-byte array on the heap; the cost is negligible
    // for HCE APDU response paths.

    /// SW = `90 00` -- command succeeded.
    public static byte[] swSuccess() {
        return new byte[] { (byte) 0x90, (byte) 0x00 };
    }

    /// SW = `6A 82` -- file or AID not found. Returned from an HCE
    /// service's `SELECT` when the requested AID is not the one it
    /// registered.
    public static byte[] swFileNotFound() {
        return new byte[] { (byte) 0x6A, (byte) 0x82 };
    }

    /// SW = `6D 00` -- INS not supported. Returned from an HCE service for
    /// any APDU whose instruction byte is not handled.
    public static byte[] swInsNotSupported() {
        return new byte[] { (byte) 0x6D, (byte) 0x00 };
    }

    /// SW = `6E 00` -- CLA not supported.
    public static byte[] swClaNotSupported() {
        return new byte[] { (byte) 0x6E, (byte) 0x00 };
    }

    /// SW = `67 00` -- wrong length / Lc.
    public static byte[] swWrongLength() {
        return new byte[] { (byte) 0x67, (byte) 0x00 };
    }

    /// SW = `69 82` -- security condition not satisfied.
    public static byte[] swSecurityNotSatisfied() {
        return new byte[] { (byte) 0x69, (byte) 0x82 };
    }

    /// SW = `6F 00` -- unknown / generic failure.
    public static byte[] swUnknownError() {
        return new byte[] { (byte) 0x6F, (byte) 0x00 };
    }

    /// `true` when the trailing two bytes of `apdu` are `90 00`.
    public static boolean isSuccess(byte[] apdu) {
        return IsoDep.isSuccess(apdu);
    }

    /// Slice helper -- returns the payload preceding the 2-byte SW
    /// trailer, or an empty array when the response is exactly 2 bytes.
    public static byte[] body(byte[] apdu) {
        if (apdu == null || apdu.length < 2) {
            return new byte[0];
        }
        byte[] out = new byte[apdu.length - 2];
        System.arraycopy(apdu, 0, out, 0, out.length);
        return out;
    }

    /// 16-bit status word from the last two bytes of `apdu`. Returns `0`
    /// for inputs shorter than 2 bytes.
    public static int statusWord(byte[] apdu) {
        if (apdu == null || apdu.length < 2) {
            return 0;
        }
        int hi = apdu[apdu.length - 2] & 0xFF;
        int lo = apdu[apdu.length - 1] & 0xFF;
        return (hi << 8) | lo;
    }

    /// Returns a 2-byte status-word array for the given SW1/SW2 pair.
    public static byte[] sw(int sw1, int sw2) {
        return new byte[] { (byte) (sw1 & 0xFF), (byte) (sw2 & 0xFF) };
    }

    /// Appends `sw` to the end of `body` and returns the combined APDU
    /// response. Helper for [HostCardEmulationService] implementations.
    public static byte[] withStatus(byte[] body, byte[] sw) {
        if (body == null) {
            body = new byte[0];
        }
        if (sw == null || sw.length != 2) {
            throw new IllegalArgumentException("sw must be 2 bytes");
        }
        byte[] out = new byte[body.length + 2];
        System.arraycopy(body, 0, out, 0, body.length);
        out[body.length] = sw[0];
        out[body.length + 1] = sw[1];
        return out;
    }
}
