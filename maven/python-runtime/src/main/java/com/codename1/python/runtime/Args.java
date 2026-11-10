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
package com.codename1.python.runtime;

/// Argument access for builtins implemented in Java: positional arguments by
/// index and keyword arguments by name, with CPython's error messages.
final class Args {
    final String fn;
    final Object[] args;
    final String[] kwNames;
    final int npos;

    Args(String fn, Object[] args, String[] kwNames) {
        this.fn = fn;
        this.args = args;
        this.kwNames = kwNames;
        this.npos = args.length - kwNames.length;
    }

    /// Binds `args` to `names` positionally or by keyword; an unbound name
    /// takes its entry of `defaults`, and a null default makes it required.
    static Object[] bind(String fn, Object[] args, String[] kwNames, String[] names, Object[] defaults) {
        Args a = new Args(fn, args, kwNames).check(names.length, names);
        Object[] r = new Object[names.length];
        for (int i = 0; i < names.length; i++) {
            r[i] = defaults[i] == null ? a.required(i, names[i]) : a.get(i, names[i], defaults[i]);
        }
        return r;
    }

    /// The argument at position `i`, else keyword `name`, else `def`.
    Object get(int i, String name, Object def) {
        if (i >= 0 && i < npos) {
            return args[i];
        }
        Object k = kw(name);
        return k != null ? k : def;
    }

    Object kw(String name) {
        if (name == null) {
            return null;
        }
        for (int i = 0; i < kwNames.length; i++) {
            if (kwNames[i].equals(name)) {
                return args[npos + i];
            }
        }
        return null;
    }

    Object required(int i, String name) {
        Object v = get(i, name, null);
        if (v == null) {
            throw Py.typeError(fn + "() missing required argument '" + name + "' (pos " + (i + 1) + ")");
        }
        return v;
    }

    /// Rejects keywords not in `allowed` and more than `maxPos` positionals.
    Args check(int maxPos, String... allowed) {
        if (npos > maxPos) {
            throw Py.typeError(fn + "() takes at most " + maxPos + " argument" + (maxPos == 1 ? "" : "s")
                    + " (" + npos + " given)");
        }
        for (String kwName : kwNames) {
            boolean ok = false;
            for (String e : allowed) {
                if (e.equals(kwName)) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                throw Py.typeError(fn + "() got an unexpected keyword argument '" + kwName + "'");
            }
        }
        return this;
    }

    /// The positional arguments only.
    Object[] positional() {
        if (kwNames.length == 0) {
            return args;
        }
        Object[] r = new Object[npos];
        System.arraycopy(args, 0, r, 0, npos);
        return r;
    }
}
