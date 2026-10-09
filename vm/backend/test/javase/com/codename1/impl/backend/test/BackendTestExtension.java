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
package com.codename1.impl.backend.test;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

/// The JUnit 5 side of `@BackendTest`: finds the context the build generated for
/// the test class, starts its application (or reuses the running one), fills the
/// test's fields, and resets its mocks after each test.
///
/// The context is found by its generated name -- `<TestClass>Cn1TestContext`, with
/// a nested class's `$` written `_` -- which this, the JVM half, may do by
/// reflection. The compiled half has no reflection and no JUnit; the build writes
/// the same calls into a generated runner instead.
public final class BackendTestExtension implements TestInstancePostProcessor,
        BeforeEachCallback, AfterEachCallback {
    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(BackendTestExtension.class);
    private static boolean hookInstalled;

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext extension)
            throws Exception {
        TestContext context = load(testInstance.getClass());
        TestEnvironment environment = TestContexts.acquire(context);
        installShutdownHook();
        context.inject(testInstance, environment);
        extension.getStore(NAMESPACE).put("environment", environment);
    }

    /// Before the test's own @BeforeEach methods, so they already run as the
    /// user the test names -- the order Spring's test support has too.
    @Override
    public void beforeEach(ExtensionContext extension) throws Exception {
        Object stored = extension.getStore(NAMESPACE).get("environment");
        if (!(stored instanceof TestEnvironment) || !extension.getTestMethod().isPresent()) {
            return;
        }
        // Asked of the generated context by name: the build read the
        // annotations, as it does for a compiled run.
        String[] who = ((TestEnvironment) stored).context().securityContext(
                extension.getTestMethod().get().getName());
        if (who != null) {
            TestSecurity.apply(who);
            extension.getStore(NAMESPACE).put("security", Boolean.TRUE);
        }
    }

    @Override
    public void afterEach(ExtensionContext extension) throws Exception {
        if (extension.getStore(NAMESPACE).remove("security") != null) {
            TestSecurity.clear();
        }
        // Found from a method context however the instance was made: a store
        // lookup falls back to the parent contexts, so with PER_CLASS the entry
        // put in the class context is read here too (PerClassMockedStoreTest).
        Object stored = extension.getStore(NAMESPACE).get("environment");
        if (!(stored instanceof TestEnvironment)) {
            return;
        }
        TestEnvironment environment = (TestEnvironment) stored;
        String[] mocks = environment.context().mockBeans();
        for (String value : mocks) {
            Mocks.reset(environment.bean(value));
        }
    }

    /// The generated context of `testClass`.
    static TestContext load(Class<?> testClass) throws Exception {
        String name = contextName(testClass.getName());
        Class<?> generated;
        try {
            generated = Class.forName(name, true, testClass.getClassLoader());
        } catch (ClassNotFoundException missing) {
            throw new IllegalStateException("No generated test context " + name + " for "
                    + testClass.getName() + ". The build generates it in the "
                    + "process-test-classes phase: bind the codenameone-maven-plugin goal "
                    + "process-test-annotations in this module's pom.", missing);
        }
        Object instance = generated.getDeclaredConstructor().newInstance();
        if (!(instance instanceof TestContext)) {
            throw new IllegalStateException(name + " is not a generated test context");
        }
        return (TestContext) instance;
    }

    /// The generated context's binary name for a test class's binary name.
    static String contextName(String testClass) {
        int dot = testClass.lastIndexOf('.');
        String pkg = dot < 0 ? "" : testClass.substring(0, dot + 1);
        return pkg + testClass.substring(dot + 1).replace('$', '_') + "Cn1TestContext";
    }

    private static synchronized void installShutdownHook() {
        if (hookInstalled) {
            return;
        }
        hookInstalled = true;
        Runtime.getRuntime().addShutdownHook(new Thread("cn1-backend-test-shutdown") {
            @Override
            public void run() {
                TestContexts.shutdown();
            }
        });
    }
}
