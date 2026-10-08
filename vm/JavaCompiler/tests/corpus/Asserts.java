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
/** assert compiles to a check the class loader enables, with every detail-message type. */
public class Asserts {
    public static void main(String[] args) {
        // Disabled by default: nothing fires.
        Plain.check(-1);
        System.out.println("disabled ok");
        ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true);
        Checked.run();
    }
}
class Plain {
    static void check(int x) {
        assert x > 0 : "never";
    }
}
class Checked {
    static int calls;
    static boolean touch() { calls++; return true; }
    static void expect(Runnable r) {
        try {
            r.run();
            System.out.println("no error");
        } catch (AssertionError e) {
            System.out.println("AssertionError: " + e.getMessage());
        }
    }
    static void run() {
        expect(() -> { assert touch(); });
        System.out.println("calls " + calls);
        expect(() -> { assert false; });
        expect(() -> { assert false : "text"; });
        expect(() -> { assert false : 42; });
        expect(() -> { assert false : 7L; });
        expect(() -> { assert false : 2.5; });
        expect(() -> { assert false : 1.5f; });
        expect(() -> { assert false : (char) 65; });
        expect(() -> { assert false : true; });
        expect(() -> { assert false : (short) 3; });
        expect(() -> { assert 1 + 1 == 3 : new StringBuilder("sb"); });
        expect(new Runnable() {
            public void run() {
                assert calls < 0 : "anonymous " + calls;
            }
        });
        expect(Inner::fail);
    }
    static class Inner {
        static void fail() {
            assert calls == 0 : "nested";
        }
    }
}
