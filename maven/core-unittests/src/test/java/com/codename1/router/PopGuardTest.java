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
package com.codename1.router;

import com.codename1.junit.UITestBase;
import com.codename1.ui.Form;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PopGuardTest extends UITestBase {

    @Test
    void noGuardAllowsPop() {
        Form f = new Form();
        assertTrue(f.checkPopGuard(PopReason.PROGRAMMATIC));
    }

    @Test
    void installedGuardCanDeny() {
        Form f = new Form();
        f.setPopGuard(new PopGuard() {
            public boolean canPop(Form form, PopReason reason) { return false; }
        });
        assertFalse(f.checkPopGuard(PopReason.BACK_COMMAND));
    }

    @Test
    void guardSeesReason() {
        final PopReason[] seen = new PopReason[1];
        Form f = new Form();
        f.setPopGuard(new PopGuard() {
            public boolean canPop(Form form, PopReason reason) {
                seen[0] = reason;
                return true;
            }
        });
        f.checkPopGuard(PopReason.HARDWARE_BACK);
        assertSame(PopReason.HARDWARE_BACK, seen[0]);
    }

    @Test
    void throwingGuardDefaultsToAllow() {
        Form f = new Form();
        f.setPopGuard(new PopGuard() {
            public boolean canPop(Form form, PopReason reason) {
                throw new RuntimeException("boom");
            }
        });
        // Throwing must not propagate; navigation should continue (true).
        assertTrue(f.checkPopGuard(PopReason.PROGRAMMATIC));
    }
}
