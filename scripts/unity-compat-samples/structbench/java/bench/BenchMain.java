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
package bench;

/// Times the same integrator four ways: C# structs through the CIL
/// translator, a hand-written model of a cheaper struct lowering, mutable
/// Java vectors, and bare float arrays. Only
/// `System.currentTimeMillis` and `System.out`, so the one class runs on a
/// JVM and through ParparVM.
///
/// The checksums are printed because a variant that computes something else
/// is not a comparison: all of them must agree to the last bit.
public final class BenchMain {
    private BenchMain() {
    }

    /// One form and nothing else, for a machine too busy to trust a wall
    /// clock on: the caller measures the CPU time of the whole process, which
    /// also counts what the collector spent on another thread. `none` is the
    /// cost of starting up, to subtract.
    private static void only(String form, int bodies, int frames) {
        float sum = 0f;
        if (form.equals("struct")) {
            sum = Bench.Integrator.Run(bodies, frames);
        } else if (form.equals("outparam")) {
            sum = OutParam.run(bodies, frames);
        } else if (form.equals("mutable")) {
            sum = MutableVec2.run(bodies, frames);
        } else if (form.equals("scalar")) {
            sum = Scalar.run(bodies, frames);
        }
        System.out.println(form + " checksum " + Float.floatToIntBits(sum));
    }

    public static void main(String[] args) {
        int bodies = 200;
        int frames = 200000;
        if (args.length > 0) {
            only(args[0], bodies, frames * 3);
            return;
        }
        // Warm-up, so a JIT has compiled them all before any is timed.
        for (int i = 0; i < 3; i++) {
            Bench.Integrator.Run(bodies, 20000);
            MutableVec2.run(bodies, 20000);
            Scalar.run(bodies, 20000);
            OutParam.run(bodies, 20000);
        }
        for (int round = 0; round < 3; round++) {
            long t0 = System.currentTimeMillis();
            float a = Bench.Integrator.Run(bodies, frames);
            long t1 = System.currentTimeMillis();
            float b = MutableVec2.run(bodies, frames);
            long t2 = System.currentTimeMillis();
            float c = Scalar.run(bodies, frames);
            long t3 = System.currentTimeMillis();
            float d = OutParam.run(bodies, frames);
            long t4 = System.currentTimeMillis();
            System.out.println("round " + round + " struct=" + (t1 - t0) + "ms outparam=" + (t4 - t3) + "ms mutable="
                    + (t2 - t1) + "ms scalar=" + (t3 - t2) + "ms checksums " + Float.floatToIntBits(a) + " "
                    + Float.floatToIntBits(d) + " " + Float.floatToIntBits(b) + " " + Float.floatToIntBits(c));
        }
    }
}
