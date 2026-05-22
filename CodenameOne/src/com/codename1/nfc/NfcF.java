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

/// FeliCa (JIS X 6319-4) technology view -- the contactless protocol used
/// by Suica, PASMO, ICOCA and other Japanese transit / payment cards.
///
/// On iOS the app must declare its target system codes in the plist
/// `com.apple.developer.nfc.readersession.felica.systemcodes` and ship the
/// matching NFC entitlement -- the Codename One Maven plugin and build
/// daemon do this automatically when they see [Nfc] / [NfcF] in the
/// classpath.
public class NfcF extends TagTechnology {

    /// IDm (Manufacturer Identifier). 8 bytes on a normal FeliCa tag; empty
    /// when the platform did not expose it.
    public byte[] getIdm() {
        return new byte[0];
    }

    /// PMm (Manufacturer Parameter). 8 bytes; empty when not exposed.
    public byte[] getPmm() {
        return new byte[0];
    }

    /// Two-byte system code the tag is currently polled on. Empty when not
    /// exposed.
    public byte[] getSystemCode() {
        return new byte[0];
    }

    @Override
    public final TagType getType() {
        return TagType.NFC_F;
    }
}
