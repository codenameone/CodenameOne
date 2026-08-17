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
package com.codename1.interp;

/// The digest that binds a pairing code to the computer that showed it.
///
/// The IDE prints a six-digit code and sends the device a peer id plus this
/// digest over the two. The device asks the user to type the code, recomputes,
/// and pairs only on a match -- so a program that cannot see the IDE's terminal
/// cannot pair, and a code observed once cannot be replayed by a different
/// computer, because the peer id is bound in.
///
/// It lives in core rather than in the runtime app because it has two callers
/// that share no code: the device, and the desktop push tool. One authoritative
/// definition is the difference between "pairing broke" and "pairing broke and
/// a test said which end changed".
///
/// #### What this is not
///
/// A string hash, not a MAC, over an unencrypted channel. That is honest for a
/// loopback link reachable only through USB debugging or a simulator's own
/// loopback, where anyone who could observe the exchange already has code
/// execution on one of the two machines. It is **not** adequate for a listener
/// on a network; moving the transport there means replacing this with a real
/// key exchange.
///
/// @author Shai Almog
public final class InterpPairingDigest {
    private InterpPairingDigest() {
    }

    /// The digest over a typed code and a peer id, as sixteen lowercase hex
    /// digits. Surrounding whitespace in the code is ignored, since it arrives
    /// from a text field on a phone.
    public static String of(String code, String peerId) {
        String material = "cn1-device-runtime " + code.trim() + " " + peerId;
        long h = 1125899906842597L;
        for (int i = 0; i < material.length(); i++) {
            h = 31 * h + material.charAt(i);
        }
        // Formatted by hand rather than with Long.toHexString, which the
        // ParparVM JavaAPI does not have. Sixteen fixed digits also removes the
        // leading-zero question that would otherwise let two implementations of
        // "the same" digest disagree on one value in sixteen.
        char[] hex = new char[16];
        for (int i = 15; i >= 0; i--) {
            int nibble = (int)(h & 0xf);
            hex[i] = (char)(nibble < 10 ? '0' + nibble : 'a' + nibble - 10);
            h >>>= 4;
        }
        return new String(hex);
    }
}
