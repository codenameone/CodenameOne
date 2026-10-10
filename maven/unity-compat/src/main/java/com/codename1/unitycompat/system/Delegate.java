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

    /// A multicast delegate of the same delegate type as this one.
    protected abstract Delegate $multi(Delegate[] list);

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
        Delegate[] x = a.invocationList();
        Delegate[] y = b.invocationList();
        Delegate[] all = new Delegate[x.length + y.length];
        System.arraycopy(x, 0, all, 0, x.length);
        System.arraycopy(y, 0, all, x.length, y.length);
        return a.$multi(all);
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
            return rest == 1 ? kept[0] : source.$multi(kept); // NOPMD AvoidBranchingStatementAsLastInLoop
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
