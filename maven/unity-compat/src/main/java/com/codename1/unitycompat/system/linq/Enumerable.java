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
package com.codename1.unitycompat.system.linq;

import com.codename1.unitycompat.system.ArgumentNullException;
import com.codename1.unitycompat.system.Func_2;
import com.codename1.unitycompat.system.Interop;
import com.codename1.unitycompat.system.InvalidOperationException;
import com.codename1.unitycompat.system.collections.generic.IEnumerable_1;
import com.codename1.unitycompat.system.collections.generic.IEnumerator_1;
import com.codename1.unitycompat.system.collections.generic.List_1;

/// `System.Linq.Enumerable`: the query methods games reach for.
///
/// Two things differ from .NET, both on purpose. A method that returns a
/// sequence -- `Where`, `Select` -- runs at once and returns a list, where
/// .NET returns a query that runs each time it is walked; code that walks
/// the result once, which is nearly all of it, cannot tell, and nothing is
/// allocated per element. And the type arguments are erased: each arrives
/// as a class after the declared parameters, which is how `FirstOrDefault`
/// knows that the default of an `int` is zero and not null.
///
/// Every method that asks for an enumerator disposes it, in a `finally`,
/// whether it walked to the end, stopped at the element it wanted or was
/// thrown out by a predicate. A C# iterator method runs its own `finally`
/// blocks -- and the `Dispose` of a `using` inside it -- from the
/// enumerator's `Dispose`, which is how `foreach` releases what a sequence
/// holds; `First` or `Any` abandoning the enumerator would leave that
/// undone for good.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class Enumerable {
    private Enumerable() {
    }

    private static IEnumerator_1 walk(IEnumerable_1 source) {
        if (source == null) {
            throw new ArgumentNullException();
        }
        return source.GetEnumerator();
    }

    private static boolean yes(Object o) {
        return ((Boolean) o).booleanValue();
    }

    /// What `default(T)` is for the class a type argument travelled as.
    private static Object zero(Class type) {
        if (type == Integer.class) {
            return Integer.valueOf(0);
        }
        if (type == Float.class) {
            return Float.valueOf(0f);
        }
        if (type == Boolean.class) {
            return Boolean.FALSE;
        }
        if (type == Long.class) {
            return Long.valueOf(0L);
        }
        if (type == Double.class) {
            return Double.valueOf(0d);
        }
        if (type == Character.class) {
            return Character.valueOf((char) 0);
        }
        if (type == Short.class) {
            return Short.valueOf((short) 0);
        }
        if (type == Byte.class) {
            return Byte.valueOf((byte) 0);
        }
        return null;
    }

    public static boolean Any(IEnumerable_1 source, Class t) {
        IEnumerator_1 e = walk(source);
        try {
            return e.MoveNext();
        } finally {
            e.Dispose();
        }
    }

    public static boolean Any(IEnumerable_1 source, Func_2 predicate, Class t) {
        IEnumerator_1 e = walk(source);
        try {
            while (e.MoveNext()) {
                if (yes(predicate.Invoke(e.get_Current()))) {
                    return true;
                }
            }
            return false;
        } finally {
            e.Dispose();
        }
    }

    public static boolean All(IEnumerable_1 source, Func_2 predicate, Class t) {
        IEnumerator_1 e = walk(source);
        try {
            while (e.MoveNext()) {
                if (!yes(predicate.Invoke(e.get_Current()))) {
                    return false;
                }
            }
            return true;
        } finally {
            e.Dispose();
        }
    }

    public static int Count(IEnumerable_1 source, Class t) {
        if (source instanceof List_1) {
            return ((List_1) source).get_Count();
        }
        int n = 0;
        IEnumerator_1 e = walk(source);
        try {
            while (e.MoveNext()) {
                n++;
            }
        } finally {
            e.Dispose();
        }
        return n;
    }

    public static int Count(IEnumerable_1 source, Func_2 predicate, Class t) {
        int n = 0;
        IEnumerator_1 e = walk(source);
        try {
            while (e.MoveNext()) {
                if (yes(predicate.Invoke(e.get_Current()))) {
                    n++;
                }
            }
        } finally {
            e.Dispose();
        }
        return n;
    }

    public static boolean Contains(IEnumerable_1 source, Object value, Class t) {
        IEnumerator_1 e = walk(source);
        try {
            while (e.MoveNext()) {
                if (Interop.areEqual(e.get_Current(), value)) {
                    return true;
                }
            }
            return false;
        } finally {
            e.Dispose();
        }
    }

    public static Object First(IEnumerable_1 source, Class t) {
        IEnumerator_1 e = walk(source);
        try {
            if (!e.MoveNext()) {
                throw new InvalidOperationException("Sequence contains no elements");
            }
            return e.get_Current();
        } finally {
            e.Dispose();
        }
    }

    public static Object First(IEnumerable_1 source, Func_2 predicate, Class t) {
        IEnumerator_1 e = walk(source);
        try {
            while (e.MoveNext()) {
                Object o = e.get_Current();
                if (yes(predicate.Invoke(o))) {
                    return o;
                }
            }
        } finally {
            e.Dispose();
        }
        throw new InvalidOperationException("Sequence contains no matching element");
    }

    public static Object FirstOrDefault(IEnumerable_1 source, Class t) {
        IEnumerator_1 e = walk(source);
        try {
            return e.MoveNext() ? e.get_Current() : zero(t);
        } finally {
            e.Dispose();
        }
    }

    public static Object FirstOrDefault(IEnumerable_1 source, Func_2 predicate, Class t) {
        IEnumerator_1 e = walk(source);
        try {
            while (e.MoveNext()) {
                Object o = e.get_Current();
                if (yes(predicate.Invoke(o))) {
                    return o;
                }
            }
        } finally {
            e.Dispose();
        }
        return zero(t);
    }

    public static Object Last(IEnumerable_1 source, Class t) {
        IEnumerator_1 e = walk(source);
        try {
            if (!e.MoveNext()) {
                throw new InvalidOperationException("Sequence contains no elements");
            }
            Object last = e.get_Current();
            while (e.MoveNext()) {
                last = e.get_Current();
            }
            return last;
        } finally {
            e.Dispose();
        }
    }

    public static Object LastOrDefault(IEnumerable_1 source, Class t) {
        IEnumerator_1 e = walk(source);
        try {
            Object last = zero(t);
            while (e.MoveNext()) {
                last = e.get_Current();
            }
            return last;
        } finally {
            e.Dispose();
        }
    }

    public static Object ElementAt(IEnumerable_1 source, int index, Class t) {
        IEnumerator_1 e = walk(source);
        try {
            int at = 0;
            while (e.MoveNext()) {
                if (at == index) {
                    return e.get_Current();
                }
                at++;
            }
        } finally {
            e.Dispose();
        }
        throw new com.codename1.unitycompat.system.ArgumentOutOfRangeException();
    }

    public static IEnumerable_1 Where(IEnumerable_1 source, Func_2 predicate, Class t) {
        List_1 out = new List_1();
        IEnumerator_1 e = walk(source);
        try {
            while (e.MoveNext()) {
                Object o = e.get_Current();
                if (yes(predicate.Invoke(o))) {
                    out.Add(o);
                }
            }
        } finally {
            e.Dispose();
        }
        return out;
    }

    public static IEnumerable_1 Select(IEnumerable_1 source, Func_2 selector, Class t, Class r) {
        List_1 out = new List_1();
        IEnumerator_1 e = walk(source);
        try {
            while (e.MoveNext()) {
                out.Add(selector.Invoke(e.get_Current()));
            }
        } finally {
            e.Dispose();
        }
        return out;
    }

    public static IEnumerable_1 Reverse(IEnumerable_1 source, Class t) {
        List_1 out = ToList(source, t);
        out.Reverse();
        return out;
    }

    public static IEnumerable_1 Take(IEnumerable_1 source, int count, Class t) {
        List_1 out = new List_1();
        IEnumerator_1 e = walk(source);
        try {
            while (out.get_Count() < count && e.MoveNext()) {
                out.Add(e.get_Current());
            }
        } finally {
            e.Dispose();
        }
        return out;
    }

    public static IEnumerable_1 Skip(IEnumerable_1 source, int count, Class t) {
        List_1 out = new List_1();
        IEnumerator_1 e = walk(source);
        try {
            int at = 0;
            while (e.MoveNext()) {
                if (at >= count) {
                    out.Add(e.get_Current());
                }
                at++;
            }
        } finally {
            e.Dispose();
        }
        return out;
    }

    public static IEnumerable_1 Distinct(IEnumerable_1 source, Class t) {
        List_1 out = new List_1();
        IEnumerator_1 e = walk(source);
        try {
            while (e.MoveNext()) {
                Object o = e.get_Current();
                if (!out.Contains(o)) {
                    out.Add(o);
                }
            }
        } finally {
            e.Dispose();
        }
        return out;
    }

    public static IEnumerable_1 Concat(IEnumerable_1 first, IEnumerable_1 second, Class t) {
        List_1 out = ToList(first, t);
        IEnumerator_1 e = walk(second);
        try {
            while (e.MoveNext()) {
                out.Add(e.get_Current());
            }
        } finally {
            e.Dispose();
        }
        return out;
    }

    public static List_1 ToList(IEnumerable_1 source, Class t) {
        List_1 out = new List_1();
        IEnumerator_1 e = walk(source);
        try {
            while (e.MoveNext()) {
                out.Add(e.get_Current());
            }
        } finally {
            e.Dispose();
        }
        return out;
    }

    public static Object[] ToArray(IEnumerable_1 source, Class t) {
        return ToList(source, t).ToArray();
    }
}
