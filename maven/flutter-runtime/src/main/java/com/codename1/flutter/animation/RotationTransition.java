/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.flutter.animation;

import com.codename1.flutter.Alignment;
import com.codename1.flutter.AlignmentDirectional;
import com.codename1.flutter.AlignmentGeometry;
import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Transform;

/**
 * Rotates its child about its centre from an {@link Animation} measured in TURNS —
 * Flutter's {@code RotationTransition}, where 1.0 is a full revolution.
 */
public class RotationTransition extends AnimatedChildWidget {

    private Animation<Double> turns;
    private Alignment alignment;

    public void turns(Animation<Double> v) {
        this.turns = v;
        listenable(v);
    }

    public void alignment(AlignmentGeometry v) {
        // Flutter types the slot AlignmentGeometry; a directional value is resolved
        // left-to-right, the way the rest of the runtime lays it out.
        this.alignment = v instanceof AlignmentDirectional ? ((AlignmentDirectional) v).resolve()
                : v instanceof Alignment ? (Alignment) v : null;
    }

    public Animation<Double> getTurns() {
        return turns;
    }

    @Override
    public Widget build(BuildContext context) {
        double radians = valueOf(turns, 0.0) * 2 * Math.PI;
        return Transform.rotate(null, radians, null, alignment, null, null, getChild());
    }
}
