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
package com.codename1.io.mqtt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Wildcard matching and validation of topic names and filters.
class MqttTopicTest {
    @Test
    void matching() {
        assertTrue(MqttTopic.matches("a/b", "a/b"));
        assertFalse(MqttTopic.matches("a/b", "a/c"));
        assertFalse(MqttTopic.matches("a/b", "a/b/c"));
        assertFalse(MqttTopic.matches("a/b/c", "a/b"));
        assertTrue(MqttTopic.matches("a/+", "a/b"));
        assertFalse(MqttTopic.matches("a/+", "a/b/c"));
        assertTrue(MqttTopic.matches("a/+/c", "a/b/c"));
        assertFalse(MqttTopic.matches("a/+/c", "a/b/d"));
        assertTrue(MqttTopic.matches("+/+", "/finance"));
        assertTrue(MqttTopic.matches("#", "a/b/c"));
        assertTrue(MqttTopic.matches("a/#", "a/b/c"));
        assertTrue(MqttTopic.matches("a/#", "a"));
        assertTrue(MqttTopic.matches("a/#", "a/"));
        assertFalse(MqttTopic.matches("a/#", "b/c"));
        assertFalse(MqttTopic.matches("ab", "a"));
        assertFalse(MqttTopic.matches("a", "ab"));
        assertTrue(MqttTopic.matches("sport/+/player1", "sport/tennis/player1"));
    }

    @Test
    void systemTopicsNeedAnExplicitFilter() {
        assertFalse(MqttTopic.matches("#", "$SYS/x"));
        assertFalse(MqttTopic.matches("+/x", "$SYS/x"));
        assertTrue(MqttTopic.matches("$SYS/#", "$SYS/x"));
    }

    @Test
    void validation() {
        MqttTopic.validateFilter("a/+/b/#");
        MqttTopic.validateFilter("#");
        MqttTopic.validateName("a/b");
        String[] badFilters = {"", "a+", "+a", "a/#/b", "a#", "a/b#"};
        for (int i = 0; i < badFilters.length; i++) {
            final String f = badFilters[i];
            assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
                public void execute() {
                    MqttTopic.validateFilter(f);
                }
            }, f);
        }
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                MqttTopic.validateName("a/+");
            }
        });
    }
}
