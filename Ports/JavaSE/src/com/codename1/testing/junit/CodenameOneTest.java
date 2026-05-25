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

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Convenience meta-annotation that registers
 * {@link CodenameOneExtension} on a JUnit 5 test class. Equivalent to
 * writing {@code @ExtendWith(CodenameOneExtension.class)} but reads more
 * naturally on a Codename One test:
 *
 * <pre>
 * &#64;CodenameOneTest
 * class GreetingFormTest {
 *
 *     &#64;Test
 *     &#64;RunOnEdt
 *     void formShowsExpectedTitle() {
 *         Form f = new GreetingForm();
 *         f.show();
 *         assertEquals("Hello", Display.getInstance().getCurrent().getTitle());
 *     }
 * }
 * </pre>
 *
 * <p>The extension boots {@link com.codename1.ui.Display} lazily on the
 * first test in the JVM and leaves it running for the rest of the test
 * run, which is faster than spinning Display up and down per class.
 * Combine with {@link SimulatorProperty} / {@link SimulatorProperties}
 * to seed property state, and with {@link RunOnEdt} to dispatch the test
 * body to the EDT.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ExtendWith(CodenameOneExtension.class)
public @interface CodenameOneTest {
}
