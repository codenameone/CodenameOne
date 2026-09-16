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
package com.codename1.security.vault;

/// The context an envelope is bound to, serialized to bytes exactly one way.
///
/// AES-GCM authenticates associated data without encrypting it, which is what lets an envelope
/// refuse to open in the wrong place. Without a binding, an attacker who can write to the store
/// can move a record: the ciphertext for account A, copied over account B's entry, decrypts
/// cleanly under the same data key and the application reads A's secret as B's. Every envelope
/// this package writes therefore carries the application namespace, the vault, the record and the
/// purpose in its associated data, and a decrypt that is handed a different four fails the tag.
///
/// #### Wire format
///
/// The serialization is fixed because two ports have to produce identical bytes or nothing
/// interoperates. There is no map, no JSON and no separator character -- a separator invites the
/// ambiguity this exists to remove, where `("ab", "c")` and `("a", "bc")` serialize alike and the
/// binding stops distinguishing them.
///
/// ```text
/// uint32  field count (always 4 in version 1)
/// repeat: uint32 byte length, then that many UTF-8 bytes
/// ```
///
/// Fields, in order: application namespace, vault id, record id, purpose. A field the caller did
/// not set is the empty string and still carries its length prefix. All integers are big-endian;
/// the UTF-8 encoding is the one specified in this package rather than the platform's.
public final class AssociatedData {

    private final String application;
    private final String vault;
    private final String record;
    private final String purpose;

    private AssociatedData(String application, String vault, String record, String purpose) {
        this.application = application == null ? "" : application;
        this.vault = vault == null ? "" : vault;
        this.record = record == null ? "" : record;
        this.purpose = purpose == null ? "" : purpose;
    }

    /// Binds to an application, a vault and a record.
    ///
    /// #### Parameters
    ///
    /// - `application`: the application namespace, normally the package name
    ///
    /// - `vault`: the vault name within the application
    ///
    /// - `record`: the record, account or entry name
    public static AssociatedData of(String application, String vault, String record) {
        return new AssociatedData(application, vault, record, "");
    }

    /// The same with a purpose, which separates two envelopes that would otherwise share every
    /// field -- the password wrap of a data key and its device wrap, for instance.
    public static AssociatedData of(String application, String vault, String record, String purpose) {
        return new AssociatedData(application, vault, record, purpose);
    }

    /// The application namespace this is bound to.
    public String getApplication() {
        return application;
    }

    /// The vault name this is bound to.
    public String getVault() {
        return vault;
    }

    /// The record name this is bound to.
    public String getRecord() {
        return record;
    }

    /// The purpose this is bound to, or the empty string.
    public String getPurpose() {
        return purpose;
    }

    /// A copy with a different purpose, so a caller can derive the several bindings one vault
    /// needs without repeating the first three fields.
    public AssociatedData withPurpose(String newPurpose) {
        return new AssociatedData(application, vault, record, newPurpose);
    }

    /// A copy bound to a different record.
    public AssociatedData withRecord(String newRecord) {
        return new AssociatedData(application, vault, newRecord, purpose);
    }

    /// The bytes fed to AES-GCM as associated data, in the format documented on this class.
    public byte[] serialize() {
        byte[] a = Bytes.utf8(application);
        byte[] v = Bytes.utf8(vault);
        byte[] r = Bytes.utf8(record);
        byte[] p = Bytes.utf8(purpose);
        byte[] out = new byte[4 + 16 + a.length + v.length + r.length + p.length];
        int at = 0;
        Bytes.putInt(out, at, 4);
        at += 4;
        at = append(out, at, a);
        at = append(out, at, v);
        at = append(out, at, r);
        append(out, at, p);
        return out;
    }

    private static int append(byte[] out, int at, byte[] field) {
        Bytes.putInt(out, at, field.length);
        System.arraycopy(field, 0, out, at + 4, field.length);
        return at + 4 + field.length;
    }

    /// A description safe to log: the field names are not secret, and none of them is the value
    /// an envelope protects.
    @Override
    public String toString() {
        return "AssociatedData[application=" + application + ", vault=" + vault
                + ", record=" + record + ", purpose=" + purpose + "]";
    }

    /// Equality over the four fields, so a caller can assert a binding round-tripped.
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AssociatedData)) {
            return false;
        }
        AssociatedData o = (AssociatedData) other;
        return application.equals(o.application) && vault.equals(o.vault)
                && record.equals(o.record) && purpose.equals(o.purpose);
    }

    @Override
    public int hashCode() {
        int h = application.hashCode();
        h = h * 31 + vault.hashCode();
        h = h * 31 + record.hashCode();
        h = h * 31 + purpose.hashCode();
        return h;
    }
}
