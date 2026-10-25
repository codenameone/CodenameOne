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
package com.codename1.desktopcompat.org.jdesktop.swingx.decorator;

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.javax.swing.AbstractButton;
import com.codename1.desktopcompat.javax.swing.JLabel;
import com.codename1.desktopcompat.javax.swing.JTextField;
import com.codename1.desktopcompat.javax.swing.SwingConstants;

/// Sets the horizontal alignment of a cell's label, button or text field.
public class AlignmentHighlighter extends AbstractHighlighter {

    private int alignment;

    public AlignmentHighlighter() {
        this(SwingConstants.LEADING);
    }

    public AlignmentHighlighter(int alignment) {
        this(null, alignment);
    }

    public AlignmentHighlighter(HighlightPredicate predicate) {
        this(predicate, SwingConstants.LEADING);
    }

    public AlignmentHighlighter(HighlightPredicate predicate, int alignment) {
        super(predicate);
        this.alignment = cn1Check(alignment);
    }

    private static int cn1Check(int alignment) {
        if (alignment == SwingConstants.LEFT || alignment == SwingConstants.CENTER
                || alignment == SwingConstants.RIGHT || alignment == SwingConstants.LEADING
                || alignment == SwingConstants.TRAILING) {
            return alignment;
        }
        throw new IllegalArgumentException("invalid horizontal alignment, expected one of "
                + "LEFT, CENTER, RIGHT, LEADING, TRAILING, but was: " + alignment);
    }

    public int getHorizontalAlignment() {
        return alignment;
    }

    public void setHorizontalAlignment(int alignment) {
        if (this.alignment == alignment) {
            return;
        }
        this.alignment = cn1Check(alignment);
        fireStateChanged();
    }

    @Override
    protected Component doHighlight(Component renderer, ComponentAdapter adapter) {
        if (renderer instanceof JLabel) {
            ((JLabel) renderer).setHorizontalAlignment(alignment);
        } else if (renderer instanceof AbstractButton) {
            ((AbstractButton) renderer).setHorizontalAlignment(alignment);
        } else if (renderer instanceof JTextField) {
            ((JTextField) renderer).setHorizontalAlignment(alignment);
        }
        return renderer;
    }

    @Override
    protected boolean canHighlight(Component component, ComponentAdapter adapter) {
        return component instanceof JLabel || component instanceof AbstractButton
                || component instanceof JTextField;
    }
}
