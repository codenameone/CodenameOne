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
/// Testing a backend the way Spring Boot tests are written, on the JVM and as a
/// compiled native test.
///
/// A class annotated [BackendTest] gets the application the build wired, started
/// in the test with the `test` profile -- an in-memory database, tables created --
/// and its `@Autowired` fields filled from it:
///
/// ```java
/// @BackendTest
/// class GreeterApiTest {
///     @Autowired MockMvc mvc;
///     @Autowired Greeter greeter;
///
///     @Test
///     void greets() throws Exception {
///         mvc.perform(get("/greet/{name}", "Ada"))
///                 .andExpect(status().isOk())
///                 .andExpect(content().string("Hello, Ada"));
///     }
/// }
/// ```
///
/// The same source runs two ways. Under Maven Surefire it is an ordinary JUnit 5
/// test. With `-Dcn1.backend.compiledTests=true` the build translates it with the
/// application and runs it as a native binary, the way the server ships -- there
/// is no JUnit and no reflection in that binary, so the build finds the tests in
/// the bytecode and writes the calls itself.
///
/// The names follow Spring's so a test ports by changing its imports:
/// [MockMvc], `perform`, [MockMvcRequestBuilders], [MockMvcResultMatchers],
/// [TestConfiguration], [MockitoBean], [LocalServerPort] and [TestRestTemplate].
/// What is left out is what Spring keeps for compatibility with servlets.
package com.codename1.backend.test;
