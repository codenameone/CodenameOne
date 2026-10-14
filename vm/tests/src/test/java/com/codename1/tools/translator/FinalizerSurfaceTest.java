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

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinalizerSurfaceTest {
    private ByteCodeClass type(String name, ByteCodeClass parent, boolean declaresFinalizer) {
        ByteCodeClass cls = new ByteCodeClass(name.replace('/', '_'), name);
        cls.setBaseClass(parent == null ? null : parent.getClsName());
        cls.setBaseClassObject(parent);
        if (declaresFinalizer) {
            cls.getMethods().add(new BytecodeMethod(cls.getClsName(), Opcodes.ACC_PROTECTED,
                    "finalize", "()V", null, null));
        }
        return cls;
    }

    private boolean needsFinalization(ByteCodeClass cls) throws Exception {
        Method method = ByteCodeClass.class.getDeclaredMethod("hasRealFinalizerInHierarchy");
        method.setAccessible(true);
        return (Boolean) method.invoke(cls);
    }

    @Test
    void objectApiHookDoesNotMakeEveryObjectFinalizable() throws Exception {
        ByteCodeClass object = type("java/lang/Object", null, true);
        ByteCodeClass ordinary = type("example/Ordinary", object, false);
        ByteCodeClass resource = type("example/Resource", ordinary, true);
        ByteCodeClass inherited = type("example/Inherited", resource, false);
        assertFalse(object.hasFinalizer());
        assertFalse(needsFinalization(object));
        assertFalse(needsFinalization(ordinary));
        assertTrue(resource.hasFinalizer());
        assertTrue(needsFinalization(resource));
        assertTrue(needsFinalization(inherited));
    }
}
