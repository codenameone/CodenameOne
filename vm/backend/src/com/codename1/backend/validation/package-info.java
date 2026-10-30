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
/// Build-time checked constraints on controller input.
///
/// Put [Valid] on a `@RequestBody` or `@ModelAttribute` parameter, or a
/// constraint such as [NotBlank], [Size], [Min] or [Max] directly on a
/// `@RequestParam`, `@PathVariable` or `@RequestHeader` parameter, and the
/// generated router checks it before the controller method runs. A failure is a
/// [ValidationException] carrying a [FieldError] per broken constraint, with a
/// stable path for each.
///
/// The checks are generated Java, not reflection, so they cost what a
/// hand-written `if` costs and survive translation. Regular expressions are not
/// among the constraints, because the backend's class library has none; check
/// such a value in the method and throw a [ValidationException] yourself.
package com.codename1.backend.validation;
