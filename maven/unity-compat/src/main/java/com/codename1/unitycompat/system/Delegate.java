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
package com.codename1.unitycompat.system;

/// `System.Delegate`. A delegate over one method is an instance of a class
/// the translator generates, whose `Invoke` calls that method on
/// [#$target]. A delegate over several is an instance of the delegate
/// type's own `Multi` class, holding the others in [#$list].
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public abstract class Delegate {
    /// The object the method is called on; null for a static method.
    public Object $target;
    /// The delegates a multicast delegate calls, in order. Null for a
    /// delegate over a single method.
    public Delegate[] $list;
    /// The closed delegate type, ``System.Action`1<int32>`` as a string
    /// constant, for a delegate
    /// type with type arguments: the translator stores it where the
    /// delegate is made and the type is known. Made inside generic code
    /// from its own type parameters (`Action<T>` in a method of `Pool<T>`),
    /// the delegate takes the same constant from the context of the
    /// constructed type the code runs as, where there is one: a generic
    /// type with static state, one that names such a type through its own
    /// parameters, or a generic method that does.
    ///
    /// Null otherwise -- such code in a generic type with no static state,
    /// in an instance method of a generic struct, and in a lambda's closure
    /// class -- because the closed type is not known there without building
    /// a name on every creation; and null for a delegate type without type
    /// arguments and for one the runtime makes. A null key compares as the
    /// erased type alone, as matching any key: no closed type can be ruled
    /// out for it, so an `Action<T>` made by `Plain<int>` combines with one
    /// made by `Plain<string>`, and with a closed `Action<string>`, where
    /// .NET throws; and two such delegates over one method and one target
    /// are equal, where .NET's are not. A `Combine` or `Remove` that .NET
    /// would reject is a bug in the program, so what this leaves is a
    /// missing exception and that one `Equals`.
    public Object $typeKey;

    /// A multicast delegate of the same delegate type as this one.
    protected abstract Delegate $multi(Delegate[] list);

    /// Whether `other` is of this delegate's type. A delegate type is an
    /// abstract class here -- the runtime's own, or one the translator
    /// writes -- and the class of a delegate is some subclass of it, one
    /// for each method. Nothing but that abstract class knows its name
    /// without reflection, so it answers, with an `instanceof` of itself.
    ///
    /// Type arguments are erased here, so an `Action<int>` and an
    /// `Action<string>` are one abstract class; [#$typeKey] tells them
    /// apart, in [#sameType(Delegate, Delegate)].
    protected abstract boolean $sameType(Delegate other);

    /// What .NET throws from `Combine` and `Remove` alike when both are
    /// there and are not one delegate type. C# lets `+=` join only delegates
    /// of one static type, but variance lets an `Action<object>` be kept in
    /// an `Action<string>`, and .NET wants the same run-time type, exactly:
    /// so the closed types are compared too. Two of the same type share one
    /// interned constant, and the test is a single reference compare; the
    /// `equals` behind it runs only for two keys that differ by identity,
    /// which a target that does not intern constants across classes could
    /// give for one type.
    private static void sameType(Delegate a, Delegate b) {
        Object x = a.$typeKey;
        Object y = b.$typeKey;
        if (!a.$sameType(b) || x != y && x != null && y != null && !x.equals(y)) { // NOPMD CompareObjectsWithEquals
            throw new ArgumentException("Delegates must be of the same type.");
        }
    }

    /// A multicast delegate of `a`'s type, closed type included.
    private static Delegate multi(Delegate a, Delegate[] list) {
        Delegate d = a.$multi(list);
        d.$typeKey = a.$typeKey;
        return d;
    }

    private Delegate[] invocationList() {
        return $list != null ? $list : new Delegate[] {this};
    }

    public static Delegate Combine(Delegate a, Delegate b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        sameType(a, b);
        Delegate[] x = a.invocationList();
        Delegate[] y = b.invocationList();
        Delegate[] all = new Delegate[x.length + y.length];
        System.arraycopy(x, 0, all, 0, x.length);
        System.arraycopy(y, 0, all, x.length, y.length);
        return multi(a, all);
    }

    /// Removes the last occurrence of `value`'s invocation list from
    /// `source`'s, which is what `-=` on an event does.
    public static Delegate Remove(Delegate source, Delegate value) {
        if (source == null) {
            return null;
        }
        if (value == null) {
            return source;
        }
        sameType(source, value);
        Delegate[] x = source.invocationList();
        Delegate[] y = value.invocationList();
        for (int at = x.length - y.length; at >= 0; at--) {
            boolean match = true;
            for (int i = 0; i < y.length && match; i++) {
                match = x[at + i].equals(y[i]);
            }
            if (!match) {
                continue;
            }
            int rest = x.length - y.length;
            if (rest == 0) {
                return null;
            }
            Delegate[] kept = new Delegate[rest];
            System.arraycopy(x, 0, kept, 0, at);
            System.arraycopy(x, at + y.length, kept, at, rest - at);
            return rest == 1 ? kept[0] : multi(source, kept); // NOPMD AvoidBranchingStatementAsLastInLoop
        }
        return source;
    }

    public static boolean op_Equality(Delegate a, Delegate b) {
        return a == b || (a != null && a.equals(b)); // NOPMD CompareObjectsWithEquals
    }

    public static boolean op_Inequality(Delegate a, Delegate b) {
        return !op_Equality(a, b);
    }

    /// Two delegates are equal when they call the same method on the same
    /// object. The generated class identifies the method.
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Delegate) || o.getClass() != getClass()) {
            return false;
        }
        Delegate d = (Delegate) o;
        Object k = d.$typeKey;
        if ($typeKey != k && $typeKey != null && k != null && !$typeKey.equals(k)) { // NOPMD CompareObjectsWithEquals
            // One method as an `Action<object>` and as an `Action<string>`:
            // two delegate types, which .NET never calls equal.
            return false;
        }
        if ($list == null) {
            return d.$list == null && d.$target == $target; // NOPMD CompareObjectsWithEquals
        }
        if (d.$list == null || d.$list.length != $list.length) {
            return false;
        }
        for (int i = 0; i < $list.length; i++) {
            if (!$list[i].equals(d.$list[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
