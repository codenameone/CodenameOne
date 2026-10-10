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
package com.codename1.generated.flutter;

/**
 * Base class for the shapes that draw the pieces of a {@code Slider} (thumb,
 * value indicator, overlay, tick marks) — Flutter's {@code SliderComponentShape}.
 * The sliders demo subclasses it with a custom thumb and value-indicator shape.
 *
 * <p>Lives in the transpiler's generated package because the demo references it
 * unqualified (the Flutter SDK type carries no {@code @JavaName} mapping). The
 * concrete {@code getPreferredSize} / {@code paint} overrides are supplied by
 * the generated subclasses; kept as an open base so their exact (Animation /
 * TextPainter / RenderBox / SliderThemeData) signatures compile without the
 * base pinning them.</p>
 */
public abstract class SliderComponentShape {
}
