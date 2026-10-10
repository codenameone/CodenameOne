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

/// `SWAP`, alone and after `DUP_X1`. No Java compiler writes either over two
/// computed values, so the methods under test are not in this file: the test
/// assembles `JsStackSwapStraight` and `JsStackSwapBranching` and puts them in
/// place of the two classes of those names below, whose answers are all wrong.
/// The instructions of each method are listed on [JsStackSwapShapes].
public class JsStackSwapApp {
    public static int result;

    /// What the branching flavour tests before anything else, so that it has a
    /// branch and is not emitted as straight line code.
    static int guard;
    /// The calls made, in order, one decimal digit each.
    static int order;

    static int one() {
        order = order * 10 + 1;
        return 1;
    }

    static int two() {
        order = order * 10 + 2;
        return 20;
    }

    static Object target() {
        order = order * 10 + 3;
        return "target";
    }

    static int check(JsStackSwapShapes s) {
        int mask = 0;
        if (s.swapLocals(3, 10) == 7) {
            mask |= 1;
        }
        order = 0;
        if (s.swapCalls() == 19 && order == 12) {
            mask |= 2;
        }
        order = 0;
        if (s.swapCallUnderLocal(5) == 4 && order == 1) {
            mask |= 4;
        }
        order = 0;
        if (s.swapLocalUnderCall(5) == 15 && order == 2) {
            mask |= 8;
        }
        order = 0;
        if (s.swapSumUnderCall(2, 4) == 14 && order == 2) {
            mask |= 16;
        }
        order = 0;
        if (s.swapCallUnderSum(2, 4) == 5 && order == 1) {
            mask |= 32;
        }
        Object marker = new Object();
        JsStackSwapBox boxed = s.box(marker);
        if (boxed != null && boxed.value == marker) {
            mask |= 64;
        }
        order = 0;
        boxed = s.boxCall();
        if (boxed != null && "target".equals(boxed.value) && order == 3) {
            mask |= 128;
        }
        return mask;
    }

    public static void main(String[] args) {
        result = check(new JsStackSwapStraight()) | (check(new JsStackSwapBranching()) << 8);
    }
}

class JsStackSwapBox {
    final Object value;

    JsStackSwapBox(Object value) {
        this.value = value;
    }
}

interface JsStackSwapShapes {
    /// `a, b, SWAP, ISUB`: b - a.
    int swapLocals(int a, int b);

    /// `one(), two(), SWAP, ISUB`: 19, and the calls stay in that order.
    int swapCalls();

    /// `one(), a, SWAP, ISUB`: a - 1.
    int swapCallUnderLocal(int a);

    /// `a, two(), SWAP, ISUB`: 20 - a.
    int swapLocalUnderCall(int a);

    /// `a, b, IADD, two(), SWAP, ISUB`: 20 - (a + b).
    int swapSumUnderCall(int a, int b);

    /// `one(), a, b, IADD, SWAP, ISUB`: (a + b) - 1.
    int swapCallUnderSum(int a, int b);

    /// `target, NEW, DUP_X1, SWAP, INVOKESPECIAL`: a constructor's argument
    /// that was on the stack before the object was allocated.
    JsStackSwapBox box(Object target);

    /// The same, with the argument left there by a call.
    JsStackSwapBox boxCall();
}

/// Stands in for the assembled classes while this file is compiled.
class JsStackSwapStub implements JsStackSwapShapes {
    public int swapLocals(int a, int b) {
        return 0;
    }

    public int swapCalls() {
        return 0;
    }

    public int swapCallUnderLocal(int a) {
        return 0;
    }

    public int swapLocalUnderCall(int a) {
        return 0;
    }

    public int swapSumUnderCall(int a, int b) {
        return 0;
    }

    public int swapCallUnderSum(int a, int b) {
        return 0;
    }

    public JsStackSwapBox box(Object target) {
        return null;
    }

    public JsStackSwapBox boxCall() {
        return null;
    }
}

class JsStackSwapStraight extends JsStackSwapStub {
}

class JsStackSwapBranching extends JsStackSwapStub {
}
