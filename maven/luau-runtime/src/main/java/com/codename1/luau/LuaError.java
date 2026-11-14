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
package com.codename1.luau;

/// A Luau runtime error. The thrown value is any Luau value, usually a
/// string message prefixed with the chunk name and line, as `error` does.
public class LuaError extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private transient Object value;
    private String luaTraceback;
    private boolean wantsPosition;
    private boolean fatal;

    /// An error carrying the given Luau value, raised as is.
    public LuaError(Object value) {
        super(value instanceof String ? (String) value : "(error object is a " + Interp.typeName(value) + " value)");
        this.value = value;
    }

    /// A runtime error whose message gets the position of the Luau code
    /// that was running, as `error(msg, 1)` does.
    public static LuaError rt(String msg) {
        LuaError e = new LuaError(msg);
        e.wantsPosition = true;
        return e;
    }

    /// An error that `pcall` cannot catch: the script exhausted its budget.
    static LuaError fatal(String msg) {
        LuaError e = new LuaError(msg);
        e.fatal = true;
        return e;
    }

    /// The Luau value passed to `error`, or the message of an internal error.
    public Object getValue() {
        return value;
    }

    @Override
    public String getMessage() {
        return value instanceof String ? (String) value : super.getMessage();
    }

    /// The Luau stack trace captured where the error was raised, or null.
    public String getLuaTraceback() {
        return luaTraceback;
    }

    boolean isFatal() {
        return fatal;
    }

    void position(String where) {
        if (wantsPosition) {
            wantsPosition = false;
            if (value instanceof String) {
                value = where + (String) value;
            }
        }
    }

    void setLuaTraceback(String t) {
        if (luaTraceback == null) {
            luaTraceback = t;
        }
    }
}
