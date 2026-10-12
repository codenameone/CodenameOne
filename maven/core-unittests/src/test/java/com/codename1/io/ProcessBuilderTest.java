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
package com.codename1.io;

import com.codename1.impl.ImplementationFactory;
import com.codename1.impl.process.ProcessSupport;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProcessBuilderTest {
    private final ImplementationFactory previous = ImplementationFactory.getInstance();
    @AfterEach void restore() { ImplementationFactory.setInstance(previous); }

    @Test void unsupportedPortNeedsNoDisplayAndFailsExplicitly() {
        ImplementationFactory.setInstance(new ImplementationFactory() {
            public Object createProcessSupport() { return null; }
        });
        assertFalse(ProcessBuilder.isSupported());
        ProcessBuilder builder = new ProcessBuilder("tool");
        assertThrows(UnsupportedOperationException.class, builder::start);
        assertThrows(UnsupportedOperationException.class, builder::environment);
    }

    @Test void validatesMutatedCommandAndEnvironmentBeforeNativeBoundary() {
        final int[] calls = {0};
        ImplementationFactory.setInstance(new ImplementationFactory() {
            public Object createProcessSupport() {
                return new ProcessSupport() {
                    public Map<String, String> environment() { return new HashMap<String, String>(); }
                    public Process start(String[] args, String dir, Map<String, String> env, boolean merge) throws IOException {
                        calls[0]++; throw new IOException("adapter reached");
                    }
                };
            }
        });
        ProcessBuilder builder = new ProcessBuilder("tool");
        builder.command().add("bad\0arg");
        assertThrows(IllegalArgumentException.class, builder::start);
        builder.command("tool");
        builder.environment().put("KEY", "value");
        builder.environment().entrySet().iterator().next().setValue("bad\0value");
        assertThrows(IllegalArgumentException.class, builder::start);
        builder.environment().clear();
        builder.environment().put("KEY=bad", "value");
        assertThrows(IllegalArgumentException.class, builder::start);
        builder.environment().clear();
        builder.environment().put("KEY", null);
        assertThrows(NullPointerException.class, builder::start);
        assertEquals(0, calls[0]);
        builder.environment().clear();
        assertThrows(IOException.class, builder::start);
        assertEquals(1, calls[0]);
    }
}
