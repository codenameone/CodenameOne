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
import java.util.*;
public abstract class AbstractProbe {
    abstract String kind();
    String describe() { return "a " + kind(); }
    static class Dog extends AbstractProbe { String kind() { return "dog"; } }
    static class Cat extends AbstractProbe {
        String kind() { return "cat"; }
        String describe() { return "definitely " + super.describe(); }
    }
    interface Shape<T extends Number> { T area(); }
    static class Sq implements Shape<Integer> {
        public Integer area() { return 4; }
    }
    public static void main(String[] a) {
        java.util.List<AbstractProbe> l = new ArrayList<AbstractProbe>();
        l.add(new Dog()); l.add(new Cat());
        StringBuilder r = new StringBuilder();
        for (AbstractProbe p : l) r.append(p.describe()).append("; ");
        Shape<Integer> s = new Sq();
        r.append("area=").append(s.area());
        System.out.println("PROBE AbstractProbe: " + r);
        new Form("Abstract").show();
    }
}
