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
package com.codenameone.developerguide;

// tag::kotlin-multiplatform-interop-session-card-test[]
import com.codename1.testing.AbstractTest;
import com.codenameone.developerguide.kmp.SessionCard;

public class SessionCardTest extends AbstractTest {
    @Override
    public boolean runTest() throws Exception {
        int[] taps = {0};
        SessionCard card = new SessionCard(() -> taps[0]++);
        card.bind("09:00  -  Hall A", "Keynote", "A. Speaker, Codename One", false);

        assertEqual("Keynote", card.title.getText());

        card.heart.released();   // a tap
        assertTrue(taps[0] == 1, "the heart reports the tap");
        return true;
    }
}
// end::kotlin-multiplatform-interop-session-card-test[]
