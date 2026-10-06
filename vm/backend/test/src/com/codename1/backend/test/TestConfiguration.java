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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// A class whose `@Bean` methods add beans for a [BackendTest], and only for it.
///
/// A static nested class of the test is added to that test; a top-level one is
/// added by listing it in [BackendTest#classes]. A test bean is an ordinary
/// singleton: it is injected, it receives its own injections, and it may be
/// marked `@Primary` to win over the application's bean of the same type --
/// which is how a test swaps an implementation without a mocking library:
///
/// ```java
/// @TestConfiguration
/// static class Fakes {
///     @Bean @Primary
///     Mailer mailer() {
///         return new RecordingMailer();
///     }
/// }
/// ```
///
/// Test beans may not be request-, session-scoped or lazy, and may not carry
/// `@Transactional`, `@Async` or the other woven annotations.
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TestConfiguration {
}
