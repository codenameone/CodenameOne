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
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Class names read out of JVM field descriptors.
 *
 * <p>The scanner reports every class a field or local variable is typed
 * as, and it read those names out of the descriptor by cutting two
 * characters off the tail -- one too many, so every such name lost its
 * last letter. Prefix checks still matched, which is why it went
 * unnoticed for so long; any check comparing a whole name silently did
 * not, and that is how a Bluetooth-only app declaring a
 * {@code HealthSample} field was classified as using the health store and
 * had Health Connect injected into its build.</p>
 */
public class DescriptorNameTest {

    @Test
    void aDescriptorYieldsTheWholeClassName() {
        assertEquals("com/codename1/health/HealthSample",
                Executor.descriptorToInternalName(
                        "Lcom/codename1/health/HealthSample;"));
        assertEquals("com/codename1/health/QuantitySample",
                Executor.descriptorToInternalName(
                        "Lcom/codename1/health/QuantitySample;"));
        // Single-letter class, the shortest thing this has to handle.
        assertEquals("A", Executor.descriptorToInternalName("LA;"));
    }

    /** Primitives and arrays are not class descriptors; pass them through. */
    @Test
    void nonClassDescriptorsAreUntouched() {
        assertEquals("I", Executor.descriptorToInternalName("I"));
        assertEquals("[Lcom/foo/Bar;",
                Executor.descriptorToInternalName("[Lcom/foo/Bar;"));
        assertNull(Executor.descriptorToInternalName(null));
    }
}
