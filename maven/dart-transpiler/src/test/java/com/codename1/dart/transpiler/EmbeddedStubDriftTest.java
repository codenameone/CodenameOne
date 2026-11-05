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
package com.codename1.dart.transpiler;

import com.codename1.dart.transpiler.analyze.StubRegistry;
import com.codename1.dart.transpiler.api.Diagnostics;
import com.codename1.dart.transpiler.harness.TestSupport;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code flutter_material.dart} has exactly one source: the runtime module's
 * {@code META-INF/dart/flutter_material.dart}. The transpiler's embedded fallback
 * ({@link StubRegistry#loadEmbedded}, used when a transpile is given no stub classpath)
 * is copied from it by this module's pom at build time.
 *
 * <p>There used to be a hand-maintained subset under this module's
 * {@code src/main/resources}; it drifted (an empty {@code BuildContext}, a {@code void}
 * {@code showDialog}, a {@code GridView.count} with its named parameters in the wrong
 * order), and each drift compiled a Dart call onto the wrong Java call. This test keeps it
 * from coming back: the embedded resource must be byte-identical to the runtime's file, and
 * no copy may exist in the source tree.</p>
 */
public class EmbeddedStubDriftTest {

    private static final File RUNTIME_STUB = new File(
            "../flutter-runtime/src/main/resources/META-INF/dart/flutter_material.dart");
    private static final File FORMER_COPY = new File(
            "src/main/resources/com/codename1/dart/stubs/flutter_material.dart");

    @Test
    public void embeddedStubIsTheRuntimeStub() throws Exception {
        TestSupport.prerequisite(RUNTIME_STUB.isFile(),
                "runtime stub not found at " + RUNTIME_STUB.getAbsolutePath());
        assertFalse(FORMER_COPY.exists(), "A second flutter_material.dart was added at " + FORMER_COPY
                + "; edit the runtime's META-INF/dart copy instead, the pom embeds it");
        byte[] embedded;
        try (InputStream in = StubRegistry.class.getResourceAsStream(
                "/com/codename1/dart/stubs/flutter_material.dart")) {
            assertNotNull(in, "embedded stub missing; the pom's embed-flutter-material-stub execution did not run");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            embedded = out.toByteArray();
        }
        assertTrue(Arrays.equals(Files.readAllBytes(RUNTIME_STUB.toPath()), embedded),
                "embedded flutter_material.dart differs from the runtime's; rebuild dart-transpiler");
        Diagnostics diags = new Diagnostics();
        StubRegistry.loadEmbedded(diags);
        assertFalse(diags.hasErrors(), "embedded stub failed to parse: " + diags.asList());
    }
}
