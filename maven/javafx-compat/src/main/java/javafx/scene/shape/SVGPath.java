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
package javafx.scene.shape;

import com.codename1.fxcompat.runtime.Dirty;
import com.codename1.fxcompat.runtime.FxObject;
import com.codename1.fxcompat.runtime.FxPath;
import com.codename1.fxcompat.runtime.FxString;
import com.codename1.fxcompat.runtime.SvgPath;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;

/// An outline written as SVG path data, the `d` attribute of an SVG
/// `path`.
///
/// Every command of the path grammar is read, with its relative form,
/// repeated coordinates and the compact number spellings; arcs are drawn
/// as cubic curves. See `com.codename1.fxcompat.runtime.SvgPath`. Content
/// that stops making sense draws what came before the error; JavaFX logs
/// such content and, depending on where the error is, draws that much or
/// nothing.
public class SVGPath extends Shape {

    private final StringProperty content = new FxString(this, "content", "", Dirty.GEOMETRY);
    private final ObjectProperty<FillRule> fillRule = new FxObject<FillRule>(this, "fillRule", FillRule.NON_ZERO,
            Dirty.GEOMETRY);

    /// Creates a path without content.
    public SVGPath() {
    }

    /// Returns the path data.
    public final String getContent() {
        String c = content.get();
        return c == null ? "" : c;
    }

    /// Sets the path data.
    public final void setContent(String value) {
        content.set(value);
    }

    /// The path data.
    public final StringProperty contentProperty() {
        return content;
    }

    /// Returns how the inside is decided.
    public final FillRule getFillRule() {
        FillRule r = fillRule.get();
        return r == null ? FillRule.NON_ZERO : r;
    }

    /// Sets how the inside is decided.
    public final void setFillRule(FillRule value) {
        fillRule.set(value);
    }

    /// How the inside is decided.
    public final ObjectProperty<FillRule> fillRuleProperty() {
        return fillRule;
    }

    @Override
    protected FxPath cn1CreatePath() {
        FxPath p = new FxPath();
        p.setEvenOdd(getFillRule() == FillRule.EVEN_ODD);
        SvgPath.append(getContent(), p);
        return p;
    }
}
