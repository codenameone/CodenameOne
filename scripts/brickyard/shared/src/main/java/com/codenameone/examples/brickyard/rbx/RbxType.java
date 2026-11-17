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
package com.codenameone.examples.brickyard.rbx;

import com.codenameone.examples.brickyard.luau.Interp;
import com.codenameone.examples.brickyard.luau.JavaFunction;
import com.codenameone.examples.brickyard.luau.LuaError;
import com.codenameone.examples.brickyard.luau.LuaTable;
import com.codenameone.examples.brickyard.luau.LuaUserdata;

/// Base of every engine value a script sees: the datatypes (`Vector3`,
/// `CFrame`, ...) and every `Instance`. One frozen metatable serves them all
/// and dispatches to the virtual methods below, so a new type is a subclass
/// and nothing else -- no per-type metatable, no reflection.
public abstract class RbxType extends LuaUserdata {
    private static final String[] OPS = {"__add", "__sub", "__mul", "__div", "__mod", "__idiv", "__pow"};
    static final LuaTable META = buildMeta();

    protected RbxType() {
        setMetatable(META);
    }

    /// The name `typeof` reports.
    @Override
    public abstract String typeName();

    /// `self.key`. The default reports the member as missing.
    public Object get(Interp in, String key) {
        throw LuaError.rt(key + " is not a valid member of " + typeName());
    }

    /// `self.key = v`. The default refuses: datatypes are immutable.
    public void set(Interp in, String key, Object v) {
        throw LuaError.rt(key + " cannot be assigned to");
    }

    /// A binary operator. `selfLeft` tells which operand this value was.
    public Object arith(String op, Object other, boolean selfLeft) {
        throw LuaError.rt("attempt to perform arithmetic (" + op + ") on " + typeName());
    }

    /// Unary minus.
    public Object negate() {
        throw LuaError.rt("attempt to perform arithmetic (unm) on " + typeName());
    }

    /// Value equality; identity unless a datatype overrides it.
    public boolean eq(Object o) {
        return this == o; // NOPMD CompareObjectsWithEquals - identity is the default equality of engine objects
    }

    /// `tostring(self)`.
    public String str() {
        return typeName();
    }

    /// Runs method `id` of this type. `a[0]` is self.
    protected Object[] invoke(int id, Interp in, Object[] a) {
        throw LuaError.rt("method " + id + " not implemented on " + typeName());
    }

    /// One return value.
    protected static Object[] ret(Object v) {
        return new Object[] {v};
    }

    /// The method `name` of some type; dispatches through [#invoke].
    public static final class Method extends JavaFunction {
        private final int id;
        private final String member;

        public Method(String member, int id) {
            super(member);
            this.member = member;
            this.id = id;
        }

        @Override
        public Object[] call(Interp interp, Object[] args) {
            Object self = arg(args, 0);
            if (!(self instanceof RbxType)) {
                throw LuaError.rt("Expected ':' not '.' calling member function " + member);
            }
            return ((RbxType) self).invoke(id, interp, args);
        }
    }

    private static LuaTable buildMeta() {
        LuaTable m = new LuaTable();
        m.rawset("__index", new JavaFunction("__index") {
            @Override
            public Object[] call(Interp in, Object[] a) {
                RbxType self = (RbxType) a[0];
                Object k = arg(a, 1);
                if (!(k instanceof String)) {
                    throw LuaError.rt(Interp.rawToString(k) + " is not a valid member of " + self.typeName());
                }
                return ret(self.get(in, (String) k));
            }
        });
        m.rawset("__newindex", new JavaFunction("__newindex") {
            @Override
            public Object[] call(Interp in, Object[] a) {
                RbxType self = (RbxType) a[0];
                Object k = arg(a, 1);
                if (!(k instanceof String)) {
                    throw LuaError.rt(Interp.rawToString(k) + " is not a valid member of " + self.typeName());
                }
                self.set(in, (String) k, arg(a, 2));
                return null;
            }
        });
        for (String metaName : OPS) {
            final String op = metaName.substring(2);
            m.rawset(metaName, new JavaFunction(metaName) {
                @Override
                public Object[] call(Interp in, Object[] a) {
                    Object l = arg(a, 0);
                    Object r = arg(a, 1);
                    if (l instanceof RbxType) {
                        return ret(((RbxType) l).arith(op, r, true));
                    }
                    return ret(((RbxType) r).arith(op, l, false));
                }
            });
        }
        m.rawset("__unm", new JavaFunction("__unm") {
            @Override
            public Object[] call(Interp in, Object[] a) {
                return ret(((RbxType) a[0]).negate());
            }
        });
        m.rawset("__eq", new JavaFunction("__eq") {
            @Override
            public Object[] call(Interp in, Object[] a) {
                Object l = arg(a, 0);
                return ret(Boolean.valueOf(l instanceof RbxType && ((RbxType) l).eq(arg(a, 1))));
            }
        });
        m.rawset("__tostring", new JavaFunction("__tostring") {
            @Override
            public Object[] call(Interp in, Object[] a) {
                return ret(((RbxType) a[0]).str());
            }
        });
        m.rawset("__metatable", "The metatable is locked");
        m.freeze();
        return m;
    }
}
