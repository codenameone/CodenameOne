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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * A combined build ships several Apple slices from one shared hardened jar, so every slice's
 * harden.&lt;platform&gt;.enabled must participate in the opt-out: hardening runs unless EVERY slice is
 * opted out. Matches the daemon builder so the local and cloud decisions agree.
 */
class IPhoneBuilderHardeningOptOutTest {

    @Test
    void combinedBuildListsEveryAppleSlice() {
        BuildRequest plain = new BuildRequest();
        assertEquals(Arrays.asList("ios"), IPhoneBuilder.appleHardeningSlices(plain),
                "a plain iOS build ships only the iOS slice");

        BuildRequest combined = new BuildRequest();
        combined.putArgument("macNative.enabled", "true");
        combined.putArgument("watchNative.enabled", "true");
        assertEquals(Arrays.asList("ios", "mac", "watch"),
                IPhoneBuilder.appleHardeningSlices(combined),
                "a combined build lists the iOS app plus its native-Mac and watch slices");
    }

    @Test
    void hardeningRunsUnlessEverySliceOptedOut() {
        BuildRequest req = new BuildRequest();
        java.util.List<String> iosMac = Arrays.asList("ios", "mac");

        // Neither opted out -> harden.
        assertTrue(Executor.anySliceHardeningEnabled(iosMac, req));

        // Only the iOS slice opted out, Mac still on -> still harden (the shared jar is hardened for Mac).
        req.putArgument("harden.ios.enabled", "false");
        assertTrue(Executor.anySliceHardeningEnabled(iosMac, req),
                "harden.ios.enabled=false alone must NOT skip hardening the shared jar the Mac slice wants");

        // Only the Mac slice opted out, iOS still on -> still harden (fixes the old 'consult only mac' bug).
        req = new BuildRequest();
        req.putArgument("harden.mac.enabled", "off");
        assertTrue(Executor.anySliceHardeningEnabled(iosMac, req),
                "harden.mac.enabled=off must no longer leave the iOS artifact unhardened");

        // EVERY slice opted out -> skip.
        req = new BuildRequest();
        req.putArgument("harden.ios.enabled", "false");
        req.putArgument("harden.mac.enabled", "0");
        assertFalse(Executor.anySliceHardeningEnabled(iosMac, req),
                "hardening is skipped only when every shipped slice opted out");
    }
}
