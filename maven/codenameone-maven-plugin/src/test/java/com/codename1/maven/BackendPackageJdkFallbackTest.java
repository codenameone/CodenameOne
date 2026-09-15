/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.maven;

import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// Packaging must work under the setup the project documents.
///
/// The instructions say to select Java 8 with JAVA_HOME. JDK_8_HOME is an extra
/// this plugin asked for, so a developer who followed them had the right compiler
/// -- the one already compiling the build -- and was told `cn1:backend-package`
/// could not find a JDK 8.
///
/// The assertion depends on the JDK running this test, and says so rather than
/// skipping: on a JDK 8 the fallback must produce a javac, and on anything else
/// it must still refuse, because translating with a newer compiler produces class
/// files the translator cannot read.
class BackendPackageJdkFallbackTest {

    @Test
    void resolvesTheRunningJdkWhenItIsEight() throws Exception {
        BackendPackageMojo mojo = new BackendPackageMojo();
        Field jdk8Home = BackendPackageMojo.class.getDeclaredField("jdk8Home");
        jdk8Home.setAccessible(true);
        jdk8Home.set(mojo, null);

        Method resolve = BackendPackageMojo.class.getDeclaredMethod("resolveJdk8");
        resolve.setAccessible(true);

        boolean runningEight = System.getProperty("java.version").startsWith("1.8");
        try {
            File home = (File) resolve.invoke(mojo);
            if (!runningEight) {
                fail("a JDK " + System.getProperty("java.version")
                        + " must not be accepted as a JDK 8: " + home);
            }
            assertNotNull(home, "the running JDK 8 is the compiler to translate with");
            assertTrue(new File(home, "bin/javac").isFile()
                            || new File(home, "bin/javac.exe").isFile(),
                    "and it must actually carry a javac: " + home);
        } catch (InvocationTargetException wrapped) {
            if (runningEight) {
                throw new AssertionError("a JDK 8 is running this test, so the"
                        + " fallback should have found it", wrapped.getCause());
            }
            assertTrue(wrapped.getCause() instanceof MojoFailureException,
                    "a non-8 JDK is refused, and with the error that says how to"
                            + " fix it: " + wrapped.getCause());
        }
    }
}
