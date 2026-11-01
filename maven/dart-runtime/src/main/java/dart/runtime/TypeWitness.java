/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package dart.runtime;

/// The runtime value of a type parameter that a generic class's body uses as a value.
///
/// Java erases type arguments, while Dart lets a body write `x is T`, `T == int` or
/// `Type get type => T`. The transpiler gives only the classes that do so one hidden
/// witness per such parameter, passed by the code that constructs them from the type
/// argument it wrote. A witness is the erased class -- the same `Class` a type literal
/// becomes -- and whether the type admits null, which `null is T` needs.
public final class TypeWitness {
    private final Class<?> type;
    private final boolean nullable;

    private TypeWitness(Class<?> type, boolean nullable) {
        this.type = type;
        this.nullable = nullable;
    }

    /// The witness of the type whose erasure is `type`.
    public static TypeWitness of(Class<?> type, boolean nullable) {
        return new TypeWitness(type, nullable);
    }

    /// `T?` for a witness of `T`.
    public static TypeWitness nullable(TypeWitness w) {
        return w.nullable ? w : new TypeWitness(w.type, true);
    }

    /// The `Type` value, as a type literal evaluates to.
    public Class<?> type() {
        return type;
    }

    /// `value is T`.
    public boolean test(Object value) {
        return value == null ? nullable : type.isInstance(value);
    }
}
