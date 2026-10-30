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

/// Tests persistence without the rest of the application: what Spring's
/// `@DataJpaTest` does.
///
/// The slice has the database, the entity manager and the module's repositories,
/// and none of its components, controllers or routes. A test injects the
/// `EntityManager`, the `DataSource` or a repository interface and calls it
/// directly. Components it needs too are named in [#includes]; a dependency of
/// theirs that the slice lacks fails the build.
///
/// The database is the one [#database] names, a fresh in-memory SQLite one by
/// default, whatever the module's settings say, so a persistence test never
/// touches a database that outlives it. The generated daos create their tables
/// in it, and migrations run as they do on start-up.
///
/// Each test method runs in a transaction that is rolled back when it returns,
/// as Spring's are, so one test's rows are not the next one's. `@BeforeEach` and
/// `@AfterEach` methods run outside it; [#rollback] set to false commits instead.
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ExtendWith(com.codename1.impl.backend.test.BackendTestExtension.class)
@org.junit.jupiter.api.parallel.ResourceLock("com.codename1.backend.test.BackendTest")
public @interface DataJpaTest {
    /// Components and configurations of the application to wire into the slice,
    /// what Spring's `@Import` adds to one.
    Class<?>[] includes() default {};

    /// The database URL, as `cn1.datasource.url` takes it. An in-memory SQLite
    /// database by default, which is gone when the test run ends.
    String database() default ":memory:";

    /// Whether each test method's transaction is rolled back.
    boolean rollback() default true;

    /// Settings for this test, as `key=value`, as [BackendTest#properties].
    String[] properties() default {};

    /// The profile, as [BackendTest#profile].
    String profile() default "test";

    /// [TestConfiguration] classes to add, as [BackendTest#classes].
    Class<?>[] classes() default {};
}
