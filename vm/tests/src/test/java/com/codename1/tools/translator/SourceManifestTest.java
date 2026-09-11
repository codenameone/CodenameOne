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
package com.codename1.tools.translator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The manifest records ONE translation.
 *
 * <p>It is reached through a static, so a second translation sharing a classloader --
 * an embedded caller invoking {@code ByteCodeTranslator.main} twice -- would otherwise
 * describe a project containing the previous application's files. The census would then
 * blame a diagnostic on a source that is not in the build.</p>
 *
 * <p>Tested here rather than through {@code CleanTargetIntegrationTest}, because that
 * harness hands every translation a {@code new URLClassLoader(urls, null)} and therefore
 * a fresh copy of the statics: an assertion there passes whether the reset exists or
 * not.</p>
 */
class SourceManifestTest {

    @Test
    void resetForgetsThePreviousTranslation() {
        SourceManifest m = new SourceManifest();
        m.recordPort("OnlyInTheFirstApp.m", null);
        m.recordGenerated("com_example_First.c");
        m.recordRuntime("cn1_globals.m", "/cn1_globals.m");
        assertEquals(3, m.size(), "the first translation should have recorded three files");

        m.reset();
        assertEquals(0, m.size(), "reset must forget the previous translation entirely");
        assertNull(m.originOf("OnlyInTheFirstApp.m"),
                "a file from the previous application must not survive into the next");

        // The next translation records its own, and only its own.
        m.recordGenerated("com_example_Second.c");
        assertEquals(1, m.size());
        assertEquals(SourceManifest.Origin.GENERATED, m.originOf("com_example_Second.c"));
        assertNull(m.originOf("com_example_First.c"));
    }

    @Test
    void aRenameReplacesTheEntryRatherThanAddingOne() {
        // The clean target writes cn1_class_method_index.m and then copies it to .c,
        // deleting the original. The manifest has to name the file that reaches the
        // compiler, because that is the name a diagnostic carries.
        SourceManifest m = new SourceManifest();
        m.recordGenerated("cn1_class_method_index.m");
        m.renameGenerated("cn1_class_method_index.m", "cn1_class_method_index.c");
        assertEquals(1, m.size(), "a rename must not leave the old name behind");
        assertNull(m.originOf("cn1_class_method_index.m"));
        assertEquals(SourceManifest.Origin.GENERATED, m.originOf("cn1_class_method_index.c"));
    }
}
