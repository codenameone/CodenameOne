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
package com.codename1.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// The thumbnail for an [IntentEntity] instance, on a member returning
/// `com.codename1.ui.EncodedImage`.
///
/// Encoded rather than a plain `Image` because the bytes cross to the platform
/// as they are. Returning something that still needs rasterizing would move
/// that work here, which is the kind of cost that looks free in the simulator
/// and stalls on a device.
///
/// It reaches two places: the search index entry, and the row the platform
/// draws when it asks the user to pick between entities. Both render it small,
/// so a thumbnail is what this wants -- a full-size picture is dropped from the
/// picker rather than carried, since a synchronous query reply is what the
/// platform is waiting on while it builds that list.
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface EntityImage {
}
