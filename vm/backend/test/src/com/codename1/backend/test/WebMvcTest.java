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
package com.codename1.backend.test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

/// Tests controllers without the rest of the application: what Spring's
/// `@WebMvcTest` does.
///
/// The build wires the controllers this names -- every controller when it names
/// none -- with what serves their requests: the `@ControllerAdvice` and
/// `@RestControllerAdvice` classes, and the `HandlerInterceptor` and
/// `ExceptionResolver` components. No service, repository or database is
/// built. Whatever a controller needs beyond that comes from the test, as a
/// [MockitoBean], a [TestConfiguration] bean, or a class named in [#includes].
/// A dependency nothing supplies fails the build, naming the controller and the
/// type it asked for, so a slice never starts half wired.
///
/// The test reaches the controllers through [MockMvc], in process.
///
/// ```java
/// @WebMvcTest(GreetingApi.class)
/// class GreetingApiSliceTest {
///     @Autowired MockMvc mvc;
///     @MockitoBean Greetings greetings;
///     ...
/// }
/// ```
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ExtendWith(com.codename1.impl.backend.test.BackendTestExtension.class)
// The same lock as BackendTest: one application runs per process, whichever
// annotation started it.
@org.junit.jupiter.api.parallel.ResourceLock("com.codename1.backend.test.BackendTest")
public @interface WebMvcTest {
    /// The controllers to test; every controller of the module when empty.
    Class<?>[] value() default {};

    /// The same as [#value], for a test that reads better naming it.
    Class<?>[] controllers() default {};

    /// Components and configurations of the application to wire into the slice
    /// as well -- what Spring's `@Import` adds to one. Each must be a bean class
    /// of the module.
    Class<?>[] includes() default {};

    /// Settings for this test, as `key=value`, as [BackendTest#properties].
    String[] properties() default {};

    /// The profile, as [BackendTest#profile].
    String profile() default "test";

    /// [TestConfiguration] classes to add, as [BackendTest#classes].
    Class<?>[] classes() default {};
}
