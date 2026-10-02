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

/// Runs a test class against the application the build wired: what
/// `@SpringBootTest` does.
///
/// The build generates, for each class carrying this, a wiring of the
/// application's beans with the class's test beans added -- its
/// [TestConfiguration]s and [MockitoBean]s -- and the code that fills the class's
/// `@Autowired`, [LocalServerPort] and [MockitoBean] fields. Nothing is looked up
/// by reflection, so the same test runs translated, as a native binary.
///
/// One application runs at a time, as one runs per server process, and JUnit runs
/// no two classes carrying this concurrently, even with parallel execution on. Test classes
/// whose configuration is the same -- the same test beans, properties, profile
/// and web environment -- share it, as Spring caches a context; a class that
/// differs stops it and starts its own.
///
/// It is inherited, as `@SpringBootTest` is: an abstract base class can carry it,
/// with the shared fields and tests, for concrete subclasses to run.
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ExtendWith(com.codename1.impl.backend.test.BackendTestExtension.class)
// One application runs per process, and a class whose configuration differs stops
// it to start its own -- so under JUnit's parallel execution two such classes would
// tear each other's server down mid-test. Holding one shared resource lock makes
// JUnit run @BackendTest classes one at a time; other tests stay parallel.
@org.junit.jupiter.api.parallel.ResourceLock("com.codename1.backend.test.BackendTest")
public @interface BackendTest {
    /// How the application is reached. [WebEnvironment#MOCK], the default, calls it
    /// in the same process through [MockMvc]; [WebEnvironment#RANDOM_PORT] serves
    /// it on a free port for [TestRestTemplate] or any HTTP client.
    WebEnvironment webEnvironment() default WebEnvironment.MOCK;

    /// Settings for this test, as `key=value`, over the module's
    /// `application.properties`, the profile's file and the environment: a
    /// shell's `PORT` or `DATABASE_URL` does not reach a test that names its own.
    String[] properties() default {};

    /// The profile. `test` is a development profile: a database neither the test
    /// nor the profile's file names is an in-memory SQLite one, whatever the
    /// environment says, and the generated daos create their tables.
    String profile() default "test";

    /// [TestConfiguration] classes to add. A static nested [TestConfiguration]
    /// class of the test class is added without being listed.
    Class<?>[] classes() default {};

    /// How a [BackendTest] reaches the application.
    enum WebEnvironment {
        /// In process, through [MockMvc]: no request crosses a socket.
        MOCK,
        /// Served on a free port, which [LocalServerPort] names.
        RANDOM_PORT,
        /// Served on the configured `cn1.server.port`.
        DEFINED_PORT,
        /// The beans only; no requests are made.
        NONE
    }
}
