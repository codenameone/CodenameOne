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
package com.codename1.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage for {@link Tool}: name validation, the description / schema
 * defaults, the optional {@link ToolHandler}, and {@code invoke} dispatch
 * (delegation, the no-handler failure, and exception propagation).
 */
class ToolTest {

    @Test
    void nameIsRequired() {
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                new Tool(null, "d", "{}");
            }
        });
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                new Tool("", "d", "{}");
            }
        });
    }

    @Test
    void nullDescriptionAndSchemaGetDefaults() {
        Tool t = new Tool("get_weather", null, null);
        assertEquals("get_weather", t.getName());
        assertEquals("", t.getDescription());
        assertEquals("{\"type\":\"object\",\"properties\":{}}", t.getParametersJsonSchema());
    }

    @Test
    void explicitFieldsRoundTrip() {
        Tool t = new Tool("n", "does a thing", "{\"type\":\"object\"}");
        assertEquals("n", t.getName());
        assertEquals("does a thing", t.getDescription());
        assertEquals("{\"type\":\"object\"}", t.getParametersJsonSchema());
    }

    @Test
    void descriptionOnlyToolHasNoHandler() {
        assertNull(new Tool("n", "d", "{}").getHandler());
    }

    @Test
    void invokeWithoutHandlerThrowsIllegalState() {
        final Tool t = new Tool("n", "d", "{}");
        assertThrows(IllegalStateException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() throws Throwable {
                t.invoke("{}");
            }
        });
    }

    @Test
    void invokeDelegatesToHandler() throws Exception {
        ToolHandler echo = new ToolHandler() {
            public String invoke(String argumentsJson) {
                return "got:" + argumentsJson;
            }
        };
        Tool t = new Tool("n", "d", "{}", echo);
        assertSame(echo, t.getHandler());
        assertEquals("got:{\"x\":1}", t.invoke("{\"x\":1}"));
    }

    @Test
    void invokePropagatesHandlerException() {
        final Tool t = new Tool("n", "d", "{}", new ToolHandler() {
            public String invoke(String argumentsJson) throws Exception {
                throw new IllegalArgumentException("bad args");
            }
        });
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                new org.junit.jupiter.api.function.Executable() {
                    public void execute() throws Throwable {
                        t.invoke("{}");
                    }
                });
        assertEquals("bad args", ex.getMessage());
    }
}
