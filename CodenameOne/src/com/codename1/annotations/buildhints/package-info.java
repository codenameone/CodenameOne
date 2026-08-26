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
/// Build hints expressed as annotations, so the compiler checks them.
///
/// A build hint used to be a `codename1.arg.<name>=<value>` line in
/// `codenameone_settings.properties`. Nothing validated it, so a misspelled
/// name was copied into the build request, never read, and silently dropped:
/// the build stayed green and the setting simply did nothing. Written as an
/// annotation the same mistake is an unknown symbol, a wrong value type is a
/// type error, and a value outside a hint's supported set is an unknown enum
/// constant.
///
/// Put the annotations on your application's main class:
///
/// ```java
/// @Ios(newStorageLocation = true, themeMode = IosThemeMode.MODERN)
/// @Android(themeMode = AndroidThemeMode.MODERN)
/// @DesktopBuild(titleBar = DesktopTitleBar.NATIVE)
/// public class MyApplication {
/// }
/// ```
///
/// These annotations cover the hints most applications set. The rest, and the
/// open-ended families such as `android.permission.<NAME>` that an annotation
/// cannot express, are still set in `codenameone_settings.properties`, which
/// continues to work exactly as before. Setting the same hint in both places is
/// a build error.
///
/// A project generated recently already runs the goal that turns these into
/// build hints. An older one may not: a goal's default phase does not add an
/// execution to a project, so the annotations would compile and then be
/// ignored. The build refuses rather than shipping without them, and the module
/// that compiles the main class needs:
///
/// ```xml
/// <execution>
///   <id>cn1-process-classes</id>
///   <phase>process-classes</phase>
///   <goals>
///     <goal>process-annotations</goal>
///   </goals>
/// </execution>
/// ```
///
/// These annotations are the source of truth for the hints they expose. Add an
/// attribute here to add a hint: the Java type IS its type, an enum's constants
/// ARE its value domain, and `@Hint` carries the rest -- the wire key where it
/// differs from the attribute name, the prose the developer guide shows, the
/// builder's default, and whether a cn1lib may append to it.
///
/// Nothing restates any of that. The developer guide's table, the Settings
/// editor's schema and the processor's binding table are all read back out of
/// these classes by BuildHintAnnotationReader; run
/// scripts/gen-build-hint-annotations.sh after editing to refresh them.
///
/// A hint with no annotation -- a dynamic family such as
/// `android.permission.<NAME>`, or one only the build service reads -- is
/// described in maven/build-hint-catalog instead.
package com.codename1.annotations.buildhints;
