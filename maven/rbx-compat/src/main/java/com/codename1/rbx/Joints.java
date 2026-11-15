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
package com.codename1.rbx;

import java.util.ArrayList;
import java.util.List;

/// Joint lookups: which welds touch a part and what a part is rigidly
/// connected to.
final class Joints {
    private Joints() {
    }

    static Instance part0(Instance j) {
        return j.ref("Part0");
    }

    static Instance part1(Instance j) {
        return j.ref("Part1");
    }

    static boolean isJoint(Instance i) {
        return i.isA("JointInstance") || i.isA("WeldConstraint") || i.isA("NoCollisionConstraint");
    }

    /// Every joint that names `p` as one of its parts.
    static List<Instance> of(Instance p) {
        List<Instance> out = new ArrayList<Instance>();
        Instance root = p.getEngine().getWorkspace();
        List<Instance> all = root.descendants();
        for (int i = 0; i < all.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            Instance j = all.get(i);
            if (isJoint(j) && (part0(j) == p || part1(j) == p)) { // NOPMD CompareObjectsWithEquals - reference identity
                out.add(j);
            }
        }
        return out;
    }

    /// The parts reachable from `p` through joints; `recursive` follows the
    /// whole graph, otherwise only direct neighbours.
    static List<Instance> connected(Instance p, boolean recursive) {
        List<Instance> out = new ArrayList<Instance>();
        out.add(p);
        for (int k = 0; k < out.size(); k++) {
            if (!recursive && k > 0) {
                break;
            }
            List<Instance> js = of(out.get(k));
            for (int i = 0; i < js.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
                Instance a = part0(js.get(i));
                Instance b = part1(js.get(i));
                if (a != null && !out.contains(a)) {
                    out.add(a);
                }
                if (b != null && !out.contains(b)) {
                    out.add(b);
                }
            }
        }
        return out;
    }
}
