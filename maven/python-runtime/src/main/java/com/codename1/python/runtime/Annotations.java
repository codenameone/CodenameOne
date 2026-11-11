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

/// Evaluates the annotations of a function from the table the build wrote
/// for it, when something reads `__annotations__`.
///
/// Nearly every annotation is a name, an attribute of one, a subscript or a
/// union of those. Compiled, each function carried a method that builds
/// them and a class to hold it, for a value most programs never ask for;
/// written down, it is one string constant and this loop.
public final class Annotations {
    private Annotations() {
    }

    /// Records the annotation of one name of a class body, `name: T`, in the
    /// class's `__annotations__`. `entry` is a table of that one annotation.
    public static void set(Object cls, PyModule module, String entry) {
        Object[] kv = build(module, entry);
        Py.setitem(Py.getattr(cls, "__annotations__"), kv[0], kv[1]);
    }

    /// The annotations as `name, value` pairs, in the order written.
    ///
    /// The table is one instruction a line: `=name` opens an annotation,
    /// `!text` is what it is when evaluating it raises (None without one),
    /// and the rest is its expression in postfix order -- `g`/`b` a name
    /// looked up in the module first or in the builtins first, `a` an
    /// attribute, `s` a subscript, `|` a union, `t`/`l` a tuple or list of
    /// the count that follows, `c` a str and `N`, `T`, `F`, `E` the
    /// constants.
    static Object[] build(PyModule module, String table) {
        int count = 0;
        for (int i = 0; i < table.length(); i++) {
            if (table.charAt(i) == '=' && (i == 0 || table.charAt(i - 1) == '\n')) {
                count++;
            }
        }
        Object[] out = new Object[count * 2];
        Object[] stack = new Object[8];
        int sp = 0;
        int entry = -1;
        boolean failed = false;
        String otherwise = null;
        int at = 0;
        while (at < table.length()) {
            int end = table.indexOf('\n', at);
            char op = table.charAt(at);
            String arg = table.substring(at + 1, end);
            at = end + 1;
            if (op == '=') {
                if (entry >= 0) {
                    out[entry * 2 + 1] = value(stack, sp, failed, otherwise);
                }
                entry++;
                out[entry * 2] = arg;
                sp = 0;
                failed = false;
                otherwise = null;
                continue;
            }
            if (op == '!') {
                otherwise = arg;
                continue;
            }
            if (failed) {
                continue;
            }
            if (sp == stack.length) {
                Object[] wider = new Object[sp * 2];
                System.arraycopy(stack, 0, wider, 0, sp);
                stack = wider;
            }
            try {
                sp = step(module, op, arg, stack, sp);
            } catch (PyException e) {
                // A forward reference, a generic the runtime does not model:
                // the annotation is its name, never a failed definition.
                failed = true;
            }
        }
        if (entry >= 0) {
            out[entry * 2 + 1] = value(stack, sp, failed, otherwise);
        }
        return out;
    }

    private static Object value(Object[] stack, int sp, boolean failed, String otherwise) {
        if (failed || sp != 1) {
            return otherwise == null ? Py.NONE : otherwise;
        }
        return stack[0];
    }

    private static int step(PyModule module, char op, String arg, Object[] stack, int sp) {
        switch (op) {
            case 'g':
            case 'b': {
                Object v = op == 'b' ? Builtins.lookup(arg) : module.getGlobal(arg);
                if (v == null) {
                    v = op == 'b' ? module.getGlobal(arg) : Builtins.lookup(arg);
                }
                if (v == null) {
                    throw Py.nameError(arg);
                }
                stack[sp] = v;
                return sp + 1;
            }
            case 'a':
                stack[sp - 1] = Py.getattr(stack[sp - 1], arg);
                return sp;
            case 's':
                stack[sp - 2] = Py.getitem(stack[sp - 2], stack[sp - 1]);
                return sp - 1;
            case '|':
                stack[sp - 2] = Py.or(stack[sp - 2], stack[sp - 1]);
                return sp - 1;
            case 't':
            case 'l': {
                int n = Integer.parseInt(arg);
                Object[] items = new Object[n];
                System.arraycopy(stack, sp - n, items, 0, n);
                stack[sp - n] = op == 't' ? (Object) new PyTuple(items) : new PyList(items);
                return sp - n + 1;
            }
            case 'c':
                stack[sp] = arg.length() == 0 ? Py.EMPTY_STR : arg;
                return sp + 1;
            case 'N':
                stack[sp] = Py.NONE;
                return sp + 1;
            case 'T':
                stack[sp] = Py.TRUE;
                return sp + 1;
            case 'F':
                stack[sp] = Py.FALSE;
                return sp + 1;
            default:
                stack[sp] = Py.ELLIPSIS;
                return sp + 1;
        }
    }
}
