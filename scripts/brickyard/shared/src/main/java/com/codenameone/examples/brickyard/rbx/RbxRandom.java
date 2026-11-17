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

/// `Random`: a seeded PCG32 generator, so a seeded experience replays the
/// same on every target.
public final class RbxRandom extends RbxType {
    private long state;
    private static final long MUL = 6364136223846793005L;
    private static final long INC = 1442695040888963407L;

    public RbxRandom(long seed) {
        state = 0;
        next();
        state += seed;
        next();
    }

    private int next() {
        long old = state;
        state = old * MUL + INC;
        int xorshifted = (int) (((old >>> 18) ^ old) >>> 27);
        int rot = (int) (old >>> 59);
        return (xorshifted >>> rot) | (xorshifted << (-rot & 31));
    }

    /// Uniform in [0, 1).
    public double nextDouble() {
        long hi = next() & 0xffffffffL;
        long lo = next() & 0xffffffffL;
        return ((hi << 21) ^ lo) * (1.0 / (1L << 53)) % 1.0;
    }

    @Override
    public String typeName() {
        return "Random";
    }

    private static final Method NUM = new Method("NextNumber", 0);
    private static final Method INT = new Method("NextInteger", 1);
    private static final Method UNIT = new Method("NextUnitVector", 2);
    private static final Method SHUFFLE = new Method("Shuffle", 3);
    private static final Method CLONE = new Method("Clone", 4);

    @Override
    public Object get(Interp in, String k) {
        switch (k) {
            case "NextNumber":
                return NUM;
            case "NextInteger":
                return INT;
            case "NextUnitVector":
                return UNIT;
            case "Shuffle":
                return SHUFFLE;
            case "Clone":
                return CLONE;
            default:
                return super.get(in, k);
        }
    }

    @Override
    protected Object[] invoke(int id, Interp in, Object[] a) {
        switch (id) {
            case 0: {
                double lo = Args.optNum(a, 1, 0);
                double hi = Args.optNum(a, 2, 1);
                return ret(Args.n(lo + (hi - lo) * nextDouble()));
            }
            case 1: {
                long lo = (long) Math.floor(Args.num(a, 1, "NextInteger"));
                long hi = (long) Math.floor(Args.num(a, 2, "NextInteger"));
                if (hi < lo) {
                    throw com.codenameone.examples.brickyard.luau.LuaError.rt("invalid argument #2 to 'NextInteger' (interval is empty)");
                }
                return ret(Args.n(lo + (long) Math.floor(nextDouble() * (hi - lo + 1))));
            }
            case 2: {
                double z = nextDouble() * 2 - 1;
                double t = nextDouble() * Math.PI * 2;
                double r = Math.sqrt(1 - z * z);
                return ret(new Vector3(r * Math.cos(t), r * Math.sin(t), z));
            }
            case 3: {
                com.codenameone.examples.brickyard.luau.LuaTable t = Args.table(a, 1, "Shuffle");
                for (int i = t.length(); i > 1; i--) {
                    int j = 1 + (int) Math.floor(nextDouble() * i);
                    Object x = t.rawgeti(i);
                    t.rawseti(i, t.rawgeti(j));
                    t.rawseti(j, x);
                }
                return null;
            }
            default: {
                RbxRandom c = new RbxRandom(0);
                c.state = state;
                return ret(c);
            }
        }
    }
}
