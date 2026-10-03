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

import com.codename1.flutter.IconData;

/**
 * Implemented by widgets that ARE a glyph without being an {@link Icon} — the
 * platform-adaptive icons Flutter ships as small StatelessWidgets.
 *
 * <p>The runtime often needs the glyph rather than the widget: a Codename One
 * button carries a font icon, not a child component, so a FAB or an IconButton
 * asks "what glyph is in here?" and walks down the wrapper chain looking for an
 * Icon. A widget that only produces one from its {@code build} is invisible to
 * that walk — and, having found nothing, the button drew a default glyph, which
 * reads as the wrong button rather than as a missing one.</p>
 */
public interface HasIcon {

    /** The glyph this widget stands for, or null when it has none. */
    IconData iconData();
}
