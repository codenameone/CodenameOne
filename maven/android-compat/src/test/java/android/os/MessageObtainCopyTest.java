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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/// `Message.obtain(Message)` copies `replyTo` and gives the copy its own
/// bundle. It used to drop the reply channel and share the bundle, so an
/// edit through either message showed up in the other.
public class MessageObtainCopyTest {

    @Test
    public void copyKeepsReplyToAndOwnsItsBundle() {
        Message orig = new Message();
        orig.what = 7;
        orig.replyTo = new Messenger(null);
        orig.getData().putString("k", "v");

        Message copy = Message.obtain(orig);
        assertSame(orig.replyTo, copy.replyTo);
        assertEquals("v", copy.getData().getString("k"));
        assertNotSame(orig.getData(), copy.getData());

        copy.getData().putString("k", "changed");
        assertEquals("v", orig.getData().getString("k"));
    }

    @Test
    public void copyOfMessageWithoutDataHasNone() {
        assertNull(Message.obtain(new Message()).peekData());
    }
}
