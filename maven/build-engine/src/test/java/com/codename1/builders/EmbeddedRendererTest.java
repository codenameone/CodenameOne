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
package com.codename1.builders;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbeddedRendererTest {
    private static BuildRequest request(String board, String renderer) {
        BuildRequest r = new BuildRequest();
        r.putArgument("linux.embedded", "true");
        if (board != null) {
            r.putArgument("linux.embedded.board", board);
        }
        if (renderer != null) {
            r.putArgument("linux.embedded.renderer", renderer);
        }
        return r;
    }

    private static boolean gpu(String board, String renderer) {
        BuildRequest r = request(board, renderer);
        return LinuxNativeBuilder.isGpuRenderer(r, EmbeddedBoardProfile.forBoard(board));
    }

    @Test
    void boardsWithADependableDriverDefaultToTheGpu() {
        assertTrue(gpu("rpi5", null));
        assertTrue(gpu("rpi4", null));
        assertTrue(gpu("imx8", null));
        assertTrue(gpu("rk3588", null));
    }

    @Test
    void unknownAndGpuLessBoardsDefaultToSoftware() {
        assertFalse(gpu(null, null));
        assertFalse(gpu("generic-arm64", null));
        assertFalse(gpu("visionfive2", null));
        assertFalse(gpu("no-such-board", null));
    }

    @Test
    void theHintOverridesTheBoardEitherWay() {
        assertFalse(gpu("rpi5", "software"));
        assertTrue(gpu("generic-arm64", "gpu"));
    }

    @Test
    void anUnknownRendererKeepsTheBoardDefault() {
        assertTrue(gpu("rpi5", "vulkan"));
        assertFalse(gpu("generic-arm64", "GPU"));
        assertEquals("software", EmbeddedBoardProfile.normalizeRenderer(null, "software"));
    }
}
