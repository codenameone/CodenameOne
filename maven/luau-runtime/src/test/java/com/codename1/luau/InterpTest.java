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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class InterpTest {
    private static Object[] run(Interp vm, String src) {
        return vm.call(vm.load(src, "t"));
    }

    @Test
    void syntaxErrorsNameTheLine() {
        Interp vm = new Interp();
        LuaError e = assertThrows(LuaError.class, () -> vm.load("local x = 1\nlocal = 2", "chunk"));
        assertTrue(e.getMessage().startsWith("chunk:2:"), e.getMessage());
        e = assertThrows(LuaError.class, () -> vm.load("if x then", "c"));
        assertTrue(e.getMessage().contains("'end' expected"), e.getMessage());
        e = assertThrows(LuaError.class, () -> vm.load("break", "c"));
        assertTrue(e.getMessage().contains("break outside"), e.getMessage());
    }

    @Test
    void budgetStopsRunawayScriptsAndPcallCannotCatchIt() {
        Interp vm = new Interp();
        vm.setBudget(100000);
        LuaError e = assertThrows(LuaError.class, () -> run(vm, "pcall(function() while true do end end)"));
        assertTrue(e.getMessage().contains("Script timeout"), e.getMessage());
        vm.resetSteps();
        assertEquals(Interp.num(3), run(vm, "return 1 + 2")[0]);
    }

    @Test
    void javaFunctionsAndYieldingFromJava() {
        Interp vm = new Interp();
        final List<Object> waits = new ArrayList<>();
        vm.getGlobals().rawset("wait", new JavaFunction("wait") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                waits.add(arg(a, 0));
                return i.yield(new Object[] {arg(a, 0)});
            }
        });
        LuaClosure fn = vm.load("local a = wait(1)\nlocal b = wait(2)\nreturn a + b", "w");
        Coroutine co = vm.newThread(fn);
        Object[] r = vm.resume(co, new Object[0]);
        assertEquals(Boolean.TRUE, r[0]);
        assertEquals(Interp.num(1), r[1]);
        r = vm.resume(co, new Object[] {Interp.num(10)});
        assertEquals(Interp.num(2), r[1]);
        r = vm.resume(co, new Object[] {Interp.num(5)});
        assertEquals(Interp.num(15), r[1]);
        assertEquals(Coroutine.DEAD, co.getStatus());
        assertEquals(2, waits.size());
    }

    @Test
    void userdataWithMetatable() {
        Interp vm = new Interp();
        LuaUserdata u = new LuaUserdata() {
            @Override
            public String typeName() {
                return "Thing";
            }
        };
        LuaTable mt = new LuaTable();
        mt.rawset("__index", new JavaFunction("idx") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                return new Object[] {"got " + a[1]};
            }
        });
        u.setMetatable(mt);
        vm.getGlobals().rawset("thing", u);
        Object[] r = run(vm, "return thing.Name, typeof(thing), type(thing)");
        assertEquals("got Name", r[0]);
        assertEquals("Thing", r[1]);
        assertEquals("userdata", r[2]);
    }

    @Test
    void unicodeSourceRoundTrips() {
        Interp vm = new Interp();
        List<String> out = new ArrayList<>();
        vm.setPrinter(out::add);
        Object[] r = run(vm, "local s = '\u00e9t\u00e9' print(s) return #s");
        assertEquals(Interp.num(5), r[0]);
        assertEquals("\u00e9t\u00e9", Utf8.toJava(out.get(0)));
    }

    @Test
    void numberFormatting() {
        assertEquals("3", Interp.numberToString(3));
        assertEquals("-0.5", Interp.numberToString(-0.5));
        assertEquals("1e+100", Interp.numberToString(1e100));
        assertEquals("1.5e-07", Interp.numberToString(1.5e-7));
        assertEquals("2.000", StringLib.formatFloat(2.0004, 'f', 3, ""));
        assertEquals("1.23e+05", StringLib.formatFloat(123456, 'e', 2, ""));
    }
}
