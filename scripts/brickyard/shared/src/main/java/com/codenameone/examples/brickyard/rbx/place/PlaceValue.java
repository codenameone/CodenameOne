/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codenameone.examples.brickyard.rbx.place;

/// One typed property value of a place file. `type` is the XML tag name the
/// value is serialized under (`float`, `Vector3`, `token`, `Ref`, ...), so a
/// file read and written back keeps the encoding Studio expects, and `value`
/// is the engine object it decodes to:
///
/// | type | value |
/// |---|---|
/// | `string`, `ProtectedString`, `Content` | `String` |
/// | `BinaryString`, `SharedString` | `byte[]` |
/// | `bool` | `Boolean` |
/// | `int`, `int64`, `float`, `double` | `Double` |
/// | `token`, `BrickColor`, `Faces`, `Axes` | `Integer` |
/// | `Color3`, `Color3uint8` | `Color3` |
/// | `Vector3`, `Vector3int16` | `Vector3` |
/// | `CoordinateFrame`, `OptionalCoordinateFrame` | `CFrame` (or null) |
/// | `Ref` | the [PlaceNode], or null |
/// | `Font` | `String[]` {family, weight, style, cachedFaceId} |
///
/// Datatypes with their own engine class (`Vector2`, `UDim`, `UDim2`, `Ray`,
/// `NumberRange`, `NumberSequence`, `ColorSequence`, `Rect2D`,
/// `PhysicalProperties`) hold that class; a type the engine has no class for
/// (`UniqueId`, `SecurityCapabilities`) keeps its text so it survives a round
/// trip.
public final class PlaceValue {
    public final String type;
    public final Object value;

    public PlaceValue(String type, Object value) {
        this.type = type;
        this.value = value;
    }

    @Override
    public String toString() {
        return type + ":" + value;
    }
}
