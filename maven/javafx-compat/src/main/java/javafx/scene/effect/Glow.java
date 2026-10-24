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
package javafx.scene.effect;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;

/// A glow over the bright parts of a node. **Recorded and not drawn**:
/// see [Effect].
public class Glow extends Effect {

    private final ObjectProperty<Effect> input = new SimpleObjectProperty<Effect>(this, "input");
    private final DoubleProperty level = new SimpleDoubleProperty(this, "level", 0.3);

    /// Creates a glow of level 0.3.
    public Glow() {
    }

    /// Creates a glow of a level.
    public Glow(double level) {
        setLevel(level);
    }

    /// Returns the effect this one is applied on top of.
    public final Effect getInput() {
        return input.get();
    }

    /// Sets the effect this one is applied on top of.
    public final void setInput(Effect value) {
        input.set(value);
    }

    /// The effect this one is applied on top of.
    public final ObjectProperty<Effect> inputProperty() {
        return input;
    }

    /// Returns the strength of the glow, from 0 to 1.
    public final double getLevel() {
        return level.get();
    }

    /// Sets the strength of the glow.
    public final void setLevel(double value) {
        level.set(DropShadow.clamp(value, 0, 1));
    }

    /// The strength of the glow.
    public final DoubleProperty levelProperty() {
        return level;
    }
}
