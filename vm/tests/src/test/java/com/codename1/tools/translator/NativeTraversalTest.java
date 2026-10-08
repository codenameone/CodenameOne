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

import com.codename1.tools.translator.bytecodes.Invoke;
import com.codename1.tools.translator.classfile.Opcodes;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NativeTraversalTest {
    @Test
    void standaloneHashSetDoesNotRequireCulledHashMap() throws Exception {
        Parser.cleanup();
        try {
            // The initial cull can retain HashSet while removing HashMap. A later
            // foreach rewrite must not emit C field reads for the absent map.
            Field classes = Parser.class.getDeclaredField("classes");
            classes.setAccessible(true);
            classes.set(null, new ArrayList<>(Collections.singletonList(
                    new ByteCodeClass("java_util_HashSet", "java/util/HashSet"))));

            Invoke iterator = new Invoke(Opcodes.INVOKEINTERFACE,
                    "java/util/Collection", "iterator", "()Ljava/util/Iterator;", true);
            NativeTraversal traversal = new NativeTraversal(iterator, null, 0, 1, false);
            assertTrue(traversal.dependencies().contains("java_util_HashSet"));
            assertFalse(traversal.dependencies().contains("java_util_HashMap"));
            assertFalse(traversal.setup("__c = POP_OBJ();", "iteratorCall", 1)
                    .contains("java_util_HashMap"));
            assertFalse(traversal.next().contains("java_util_HashMap"));
            assertFalse(traversal.remove().contains("java_util_HashMap"));
        } finally {
            Parser.cleanup();
        }
    }
}
