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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The generated stub must not assume its lifecycle class is extensible.
 *
 * <p>The stub holds the app's lifecycle object in a field typed to the app's own main class, and
 * asks whether that object also implements {@code PushCallback}, {@code PushActionsProvider} or
 * {@code LocalNotificationCallback}. When the main class is FINAL, javac proves the conversion
 * impossible and rejects the code outright -- so a main class written in Kotlin, where every
 * class is final unless it says {@code open}, breaks a stub the developer never wrote. It was the
 * Wear module that found this, because a companion build roots its stub at the watch lifecycle
 * class, but the phone stub has always had it.</p>
 *
 * <p>Testing the emitted text rather than a compile is deliberate: the stub is assembled inline
 * across a few hundred lines of {@code AndroidGradleBuilder} with no seam to call, and what has
 * to stay true is a property of every one of those sites. Going through {@code Object} keeps the
 * runtime behaviour identical and makes the test legal for any type.</p>
 */
public class StubLifecycleCastTest {

    private static final String BUILDER =
            "src/main/java/com/codename1/builders/AndroidGradleBuilder.java";

    /** The interfaces the stub probes its lifecycle object for. */
    private static final String[] PROBED = {
        "PushCallback",
        "com.codename1.push.PushActionsProvider",
        "com.codename1.notifications.LocalNotificationCallback",
    };

    @Test
    void everyLifecycleProbeGoesThroughObject() throws IOException {
        File builder = new File(BUILDER);
        assertTrue(builder.isFile(), "the builder must be readable: " + builder.getAbsolutePath());
        String source = new String(Files.readAllBytes(builder.toPath()), StandardCharsets.UTF_8);

        List<String> bare = new ArrayList<String>();
        for (String probed : PROBED) {
            // The generated text, as it appears inside the Java string literals that build it.
            if (source.contains("i instanceof " + probed)) {
                bare.add("i instanceof " + probed);
            }
            if (source.contains("(" + probed + ")i")) {
                bare.add("(" + probed + ")i");
            }
        }

        assertTrue(bare.isEmpty(),
                "these probe the lifecycle field directly, so a final main class fails to "
                        + "compile; interpose (Object) as the neighbouring sites do: " + bare);
    }
}
