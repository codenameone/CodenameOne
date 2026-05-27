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
package com.codename1.mapping.generated;

/// Compile-time stub for the @Mapped annotation processor output. Projects
/// that ship one or more `@Mapped` classes get a real `MappersIndex`
/// generated under `target/classes`, shadowing this stub at runtime; projects
/// with no `@Mapped` classes fall through to this no-op and the
/// `com.codename1.mapping.Mappers` registry stays empty.
///
/// Application code never references this class directly -- the lazy
/// instantiation happens inside `Mappers#bootstrap`, by direct symbol
/// reference, so the iOS `Class.forName` ban does not apply.
///
/// The compiler-generated default no-arg public constructor is intentionally
/// inherited (no explicit constructor here) so the cn1 PMD gate's
/// `UnnecessaryConstructor` rule stays satisfied. The build-time-shadowing
/// `MappersIndex` written by `cn1:process-annotations` declares its own
/// constructor body that does the `Mappers.register(...)` work.
public class MappersIndex {
}
