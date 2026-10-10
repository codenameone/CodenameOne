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

/// Every way the type-aware cull can see, or fail to see, that a class is instantiated.
/// Each CASE line must match the JVM's; TypeAwareCullIntegrationTest also reads the
/// generated C to check which bodies were culled. The helper classes are top level so
/// their C names and Class.forName names are plain.
public class TypeAwareCullApp {
    static int helperOnlyUnitCalls() {
        return 100;
    }

    static int callIfUnit(Object o) {
        if (o instanceof UnitShape) {
            return ((UnitShape) o).side();
        }
        return -1;
    }

    static int areaOf(CullShape s) {
        return s.area();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("CASE|square|" + areaOf(new SquareShape()));
        System.out.println("CASE|notUnit|" + callIfUnit("x"));

        // Keep the class itself; a static call does not allocate it.
        ReflectedShape.touch();
        String name = new StringBuilder("epahSdetcelfeR").reverse().toString();
        Object r = Class.forName(name).newInstance();
        System.out.println("CASE|reflected|" + areaOf((CullShape) r));

        System.out.println("CASE|inherited|" + new DerivedShape().inherited());
        System.out.println("DONE");
    }
}

interface CullShape {
    int area();
}

/// Allocated with NEW: its area() is live.
class SquareShape implements CullShape {
    public int area() {
        return 16;
    }
}

/// Never allocated anywhere. areaOf reaches its area() by signature and callIfUnit
/// names side() directly, so both must still compile -- as culled stubs. No no-arg
/// constructor, because once Class.newInstance is reachable every class that has one
/// counts as allocated.
class UnitShape implements CullShape {
    private final int scale;

    UnitShape(int scale) {
        this.scale = scale;
    }

    public int area() {
        return TypeAwareCullApp.helperOnlyUnitCalls() + 1;
    }

    int side() {
        return scale;
    }
}

/// Created only through Class.forName with a name built at run time, so no NEW and
/// no class literal names it. The Class.newInstance seed must keep its area().
class ReflectedShape implements CullShape {
    static int touched;

    static void touch() {
        touched++;
    }

    public int area() {
        return 9;
    }
}

/// Allocated only through a subclass: its inherited method runs on a DerivedShape.
class BaseShape {
    int inherited() {
        return 5;
    }
}

class DerivedShape extends BaseShape {
}
