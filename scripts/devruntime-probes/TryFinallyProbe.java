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
public class TryFinallyProbe {
    static StringBuilder r = new StringBuilder();
    static int f() { try { r.append("t"); return 1; } finally { r.append("f"); } }
    static void nested() {
        try { try { throw new IllegalStateException("inner"); } finally { r.append("F1"); } }
        catch (IllegalStateException e) { r.append("C:").append(e.getMessage()); }
    }
    public static void main(String[] a) {
        r.append(" f=").append(f());
        nested();
        try { Object o = null; o.toString(); } catch (NullPointerException e) { r.append(" npe"); }
        try { int[] x = new int[1]; int y = x[3]; r.append(y); }
        catch (ArrayIndexOutOfBoundsException e) { r.append(" aioobe"); }
        try { int z = 1 / Integer.parseInt("0"); r.append(z); }
        catch (ArithmeticException e) { r.append(" arith"); }
        System.out.println("PROBE TryFinallyProbe: " + r);
        new Form("TryFinally").show();
    }
}
