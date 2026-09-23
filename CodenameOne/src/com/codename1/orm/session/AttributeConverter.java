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
package com.codename1.orm.session;

/// Reflection-free conversion between a domain value and one SQL scalar.
/// Implementations used by Convert must have a public no-argument constructor.
/// Both methods receive null values and must define how to represent them.
/// @param <T> domain value type
/// @param <S> SQL scalar type declared by the mapping
public interface AttributeConverter<T, S> {
    /// Converts an entity field value to its stored scalar representation.
    /// @param value domain value, possibly null
    /// @return scalar value accepted by the declared storage type, or null
    S toDatabase(T value);
    /// Converts a stored scalar to the entity field's domain type.
    /// @param value stored value, possibly null
    /// @return reconstructed domain value, or null
    T fromDatabase(S value);
}
