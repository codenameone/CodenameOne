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

/// A closure that carries its reified Dart signature, for `f is int Function(Object)`.
/// The transpiler gives every closure and tear-off a generated marker interface per
/// signature whose default method answers a constant array -- intersected with the SAM
/// its consumer holds it by, or through a generated sub-interface of that SAM -- so a
/// closure costs nothing more to build or call; one without it is tested by arity alone.
public interface Signed {
    /// The return type's witness followed by each positional parameter's; an entry may be
    /// null where the type could not be named (a type variable), and is then not tested.
    TypeWitness[] $sig();

    /// Whether the closure is a generic function (`T Function<T>(T)`, a generic function's
    /// tear-off not instantiated by its context), which no non-generic function type is a
    /// supertype of.
    default boolean $generic() {
        return false;
    }

    /// How many of the positional parameters are required; -1 when all are.
    default int $required() {
        return -1;
    }

    /// The named parameters' names, whose witnesses follow the positional ones in
    /// {@link #$sig()}; null when there are none. A required one is written `!name`.
    default String[] $named() {
        return null;
    }

    /// For a generic closure: its type parameters as Dart writes them (`T, U`), then, for
    /// each entry of {@link #$sig()}, the type parameter that slot is or null. Null for a
    /// closure that is not generic. Only what runtimeType prints reads it.
    default String[] $typeVars() {
        return null;
    }
}
