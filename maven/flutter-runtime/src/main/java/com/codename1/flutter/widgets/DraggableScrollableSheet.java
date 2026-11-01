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
package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import dart.runtime.Funcs;

/// Flutter's {@code DraggableScrollableSheet}: a bottom-aligned sheet whose
/// height is a fraction of the space it is given, with its content built for a
/// ScrollController. It is shown at initialChildSize; dragging the sheet between
/// minChildSize and maxChildSize is not implemented, so the content scrolls
/// inside the initial height.
public class DraggableScrollableSheet extends StatelessWidget {

    private double initialChildSize = 0.5;
    private double minChildSize = 0.25;
    private double maxChildSize = 1.0;
    private boolean expand = true;
    private Funcs.Func2<BuildContext, ScrollController, Widget> builder;
    private final ScrollController controller = new ScrollController();

    public void initialChildSize(double v) { this.initialChildSize = v; }
    public void minChildSize(double v) { this.minChildSize = v; }
    public void maxChildSize(double v) { this.maxChildSize = v; }
    public void expand(boolean v) { this.expand = v; }
    public void snap(boolean v) { }
    public void builder(Funcs.Func2<BuildContext, ScrollController, Widget> v) { this.builder = v; }

    public double getInitialChildSize() { return initialChildSize; }
    public double getMinChildSize() { return minChildSize; }
    public double getMaxChildSize() { return maxChildSize; }
    public boolean getExpand() { return expand; }

    @Override
    public Widget build(BuildContext context) {
        Widget content = builder != null ? builder.call(context, controller) : new SizedBox();
        FractionallySizedBox box = new FractionallySizedBox();
        box.heightFactor(initialChildSize);
        box.alignment(com.codename1.flutter.Alignment.bottomCenter);
        box.child(content);
        return box;
    }
}
