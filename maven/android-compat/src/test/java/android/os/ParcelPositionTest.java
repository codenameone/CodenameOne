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
package android.os;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/// Writes happen at the current position and advance it, so a Parcelable can
/// reserve a size slot, write its payload and seek back to fill the slot in.
/// Writes used to append without moving the position, which turned the
/// back-patch into a trailing value and left the size slot at zero.
public class ParcelPositionTest {

    @Test
    public void seekBackOverwritesReservedSlot() {
        Parcel p = Parcel.obtain();
        int start = p.dataPosition();
        p.writeInt(0);
        p.writeString("payload");
        p.writeLong(42L);
        int end = p.dataPosition();
        assertEquals(3, end);
        p.setDataPosition(start);
        p.writeInt(end - start);
        p.setDataPosition(end);
        p.writeBoolean(true);
        assertEquals(4, p.dataSize());

        p.setDataPosition(0);
        assertEquals(3, p.readInt());
        assertEquals("payload", p.readString());
        assertEquals(42L, p.readLong());
        assertTrue(p.readBoolean());
        assertEquals(4, p.dataPosition());
    }
}
