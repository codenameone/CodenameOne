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

package com.codename1.testing;

import com.codename1.junit.UITestBase;
import java.io.OutputStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class TestConnectionTest extends UITestBase {
    @Test
    void successiveRequestsKeepBothBodiesAfterClosingTheirStreams() throws Exception {
        TestCodenameOneImplementation.TestConnection connection =
                implementation.createConnection("http://collector.test/reused");
        try (OutputStream first = implementation.openOutputStream(connection)) {
            first.write(new byte[]{1, 2});
        }
        assertArrayEquals(new byte[]{1, 2}, connection.getOutputData());

        // Telemetry may export one attempt before the retry has ended. Both
        // batches must reach the mock collector even though the first closed.
        try (OutputStream second = implementation.openOutputStream(connection)) {
            second.write(new byte[]{3, 4});
        }
        assertArrayEquals(new byte[]{1, 2, 3, 4}, connection.getOutputData());

        connection.clearRequest();
        assertArrayEquals(new byte[0], connection.getOutputData());
        try (OutputStream third = implementation.openOutputStream(connection)) {
            third.write(new byte[]{5});
        }
        assertArrayEquals(new byte[]{5}, connection.getOutputData());
    }
}
