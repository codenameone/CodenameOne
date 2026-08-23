/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.widgets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * An {@link com.codename1.ui.EncodedImage} keeps its decoded bitmap behind a soft
 * reference and decodes again on demand, so a picture drawn every frame and
 * collected between them is decoded every frame. {@link com.codename1.ui.Label}
 * avoids that by locking its icon while it is on screen; {@code FittedImage}
 * paints its source directly rather than holding a scaled copy as an icon — one
 * bitmap in memory instead of two — and so has to do the locking itself.
 *
 * <p>What is asserted here is the bookkeeping, which is where the two failures
 * live: a lock left behind on a replaced picture is never released, and a second
 * lock on a picture already locked is never balanced.</p>
 */
class FittedImageLockTest {

    /// Records the calls instead of making them, so the bookkeeping can be
    /// exercised without an initialised Display — which constructing any real
    /// Image requires.
    private static final class Recording extends ImageRenderElement.ImageLock {

        final List<String> calls = new ArrayList<String>();

        @Override
        void lock(Object img) {
            calls.add("lock " + img);
        }

        @Override
        void unlock(Object img) {
            calls.add("unlock " + img);
        }
    }

    @Test
    void nothingIsLockedUntilSomethingIsOnScreen() {
        Recording r = new Recording();
        r.want(null);
        assertEquals(List.of(), r.calls, "nothing on screen means nothing pinned in memory");
        assertNull(r.held());
    }

    @Test
    void goingOnScreenLocksAndComingOffUnlocks() {
        Recording r = new Recording();
        r.want("picture");
        assertEquals(List.of("lock picture"), r.calls);
        assertSame("picture", r.held());

        r.want(null);
        assertEquals(List.of("lock picture", "unlock picture"), r.calls,
                "off screen, the decoded bitmap has to be reclaimable again");
        assertNull(r.held());
    }

    @Test
    void replacingTheSourceMovesTheLock() {
        Recording r = new Recording();
        r.want("first");
        r.want("second");
        assertEquals(List.of("lock first", "unlock first", "lock second"), r.calls,
                "the replaced picture must not keep a lock nobody will release");
        assertSame("second", r.held());
    }

    @Test
    void settingTheSameSourceAgainDoesNotStackLocks() {
        Recording r = new Recording();
        r.want("picture");
        r.want("picture");
        r.want("picture");
        assertEquals(List.of("lock picture"), r.calls, "one lock per component, or the unlock cannot balance it");

        r.want(null);
        assertEquals(List.of("lock picture", "unlock picture"), r.calls);
    }
}
