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
import com.codename1.ui.*;
public class InnerProbe {
    private int field = 5;
    class Inner { int get() { return field * 2; } }
    static class Nested { int get() { return 3; } }
    interface Cb { int call(); }
    Cb closure(final int base) { return new Cb() { public int call() { return base + field; } }; }
    public static void main(String[] a) {
        InnerProbe p = new InnerProbe();
        InnerProbe.Inner in = p.new Inner();
        System.out.println("PROBE InnerProbe: inner=" + in.get()
            + " nested=" + new Nested().get() + " closure=" + p.closure(10).call());
        new Form("Inner").show();
    }
}
