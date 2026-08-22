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
/// @Desktop(titleBar = DesktopTitleBar.NATIVE)
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
/// Generated from com.codename1.build.shared.BuildHints by
/// BuildHintCodeGenerator. Do not edit by hand -- edit the catalog and
/// re-run scripts/gen-build-hint-annotations.sh.
package com.codename1.annotations.buildhints;
