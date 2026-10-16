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

/// What the natives name, and when that keeps it. NativeScopeNative.c is the C half;
/// NativeScopeIntegrationTest reads the generated C to check what was culled.
public class NativeScopeApp {
    /// Called from Java: its C body, and the static C helper that body uses, run.
    static native int featureStart(int v);

    /// Never called from Java: its C body never runs, so what it names is not a root.
    static native int unusedNative(int v);

    public static void main(String[] args) {
        NativeScopeDead.keep();
        NativeScopeSwitched.keep();
        NativeScopeDerived.keep();
        Object probe = args;
        if (probe instanceof NativeScopeNeverInit || probe instanceof NativeScopeB) {
            System.out.println("never");
        }
        System.out.println("CASE|feature|" + featureStart(5));
        System.out.println("CASE|touched|" + NativeScopeTouched.VALUE);
        System.out.println("CASE|twin|" + NativeScopeA.twin(1));
        System.out.println("DONE");
    }
}

/// Reached through featureStart's C body.
class NativeScopeFeature {
    static int onEvent(int v) {
        return v * 2;
    }
}

/// Reached through a static C helper featureStart's body calls.
class NativeScopeHelperTarget {
    static int viaHelper(int v) {
        return v + 100;
    }
}

/// Named by a C function with external linkage, which generated code could call, so it
/// stays a root.
class NativeScopeRooted {
    static int fromFreeFunction(int v) {
        return v + 7;
    }
}

/// Named only by unusedNative's body.
class NativeScopeDead {
    static void keep() {
    }

    static int onEvent(int v) {
        return v + 1;
    }
}

/// Named only inside a branch a switched-off CN1_INCLUDE_ feature compiles out.
class NativeScopeSwitched {
    static void keep() {
    }

    static int cb(int v) {
        return v + 3;
    }
}

/// Named only under a CN1_ macro that the switched-off feature would have defined.
class NativeScopeDerived {
    static void keep() {
    }

    static int cb(int v) {
        return v + 4;
    }
}

/// Its initializer runs only on the first use of its statics.
class NativeScopeTouched {
    static int VALUE;

    static {
        VALUE = 40 + NativeScopeFeature.onEvent(1);
    }
}

/// A static no one initializes: no static method, field or allocation of it is used.
class NativeScopeNeverInit {
    static int x;

    static {
        x = NativeScopeHelperTarget.viaHelper(1);
    }
}

/// Two statics with one name and descriptor: a call names its class, so only A's runs.
class NativeScopeA {
    static int twin(int v) {
        return v + 10;
    }
}

class NativeScopeB {
    static int twin(int v) {
        return v + 20;
    }

    static void touch() {
        NativeScopeNeverInit.x++;
    }
}
