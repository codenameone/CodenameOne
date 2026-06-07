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
package com.codename1.testing.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a test (or every test in an annotated class) to execute on the
 * Codename One EDT rather than on the JUnit worker thread. The body of the
 * test method runs via {@code CN.callSeriallyAndWait}; assertion failures
 * and other throwables are rethrown to JUnit on the calling thread so
 * stack traces remain useful.
 *
 * <p>Use this when the test touches UI state that must be mutated from the
 * EDT &mdash; constructing forms, calling {@code Form.show()}, walking the
 * component tree, etc. Tests that only exercise pure model/utility code
 * can be left off the EDT and will run faster.
 *
 * <p>A method-level {@code @RunOnEdt} takes precedence over the class
 * level: place it on the class to opt the whole class in, and (rarely) on
 * a single method to override the default for that one test.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RunOnEdt {
    /**
     * Maximum time to wait for the EDT-dispatched test body to finish, in
     * milliseconds. Defaults to 30 seconds, which is generous for unit-style
     * UI tests but short enough to fail fast when the EDT deadlocks.
     */
    long timeoutMillis() default 30000L;
}
