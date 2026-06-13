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
package com.codename1.gaming;

import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage for the {@link SoundEffect} handle's pool-independent surface: the
 * owning-pool back-reference, the loaded flag, and the native-sound accessor.
 * The effect is built with the package-private constructor against a real
 * {@link SoundPool} (the fallback mixer the test platform supplies). The
 * play/unload methods delegate to the pool's native peer, which casts the
 * handle to its own internal type, so they require a peer-loaded sound and are
 * not exercised here.
 */
class SoundEffectTest extends UITestBase {

    @Test
    void exposesItsOwningPool() {
        SoundPool pool = SoundPool.create(4);
        SoundEffect e = new SoundEffect(pool, new Object());
        assertSame(pool, e.getPool());
    }

    @Test
    void newEffectIsLoaded() {
        SoundPool pool = SoundPool.create(4);
        SoundEffect e = new SoundEffect(pool, new Object());
        assertTrue(e.isLoaded());
    }

    @Test
    void exposesItsNativeSoundHandle() {
        SoundPool pool = SoundPool.create(4);
        Object handle = new Object();
        SoundEffect e = new SoundEffect(pool, handle);
        assertSame(handle, e.getNativeSound());
    }
}
