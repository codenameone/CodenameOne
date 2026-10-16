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
 * Please contact Codename One through http://www.codenameone.com/ if
 * you need additional information or have any questions.
 */
package com.codename1.impl.linux;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LinuxSocketMetadataTest {
    @Test
    void loopbackCapabilityDoesNotEnableWildcardOrReleaseDebugging() throws Exception {
        java.lang.reflect.Field singleton = LinuxImplementation.class.getDeclaredField("INSTANCE");
        singleton.setAccessible(true);
        Object previous = singleton.get(null);
        try {
            LinuxImplementation port = new LinuxImplementation();
            assertTrue(port.isLoopbackServerSocketAvailable());
            assertFalse(port.isServerSocketAvailable());
            assertFalse(port.isDebuggableBuild());
            for (Object invalid : new Object[] {null, "not a socket"}) {
                port.disconnectSocket(invalid);
                port.writeToSocketStream(invalid, new byte[0]);
                assertFalse(port.isSocketConnected(invalid));
                assertEquals(0, port.getSocketAvailableInput(invalid));
                assertNull(port.readFromSocketStream(invalid));
                assertNull(port.getSocketErrorMessage(invalid));
                assertEquals(-1, port.getSocketErrorCode(invalid));
            }
        } finally {
            singleton.set(null, previous);
        }
    }

    @Test
    void emptySocketAndStreamsRemainSafeAfterRepeatedClose() throws Exception {
        LinuxSocket socket = new LinuxSocket(0);
        assertFalse(socket.isConnected());
        assertEquals(0, socket.available());
        assertNull(socket.readChunk());
        assertEquals(-1, socket.getErrorCode());
        assertNull(socket.getErrorMessage());
        assertEquals(-1, socket.getInputStream().read());
        socket.getInputStream().close();
        socket.getOutputStream().close();
        socket.close();
        try {
            socket.getOutputStream().write(1);
            fail("closed output must reject a write");
        } catch (IOException expected) {
            assertEquals("socket closed", expected.getMessage());
        }
    }
}
