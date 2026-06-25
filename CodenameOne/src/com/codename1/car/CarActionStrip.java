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
package com.codename1.car;

import java.util.ArrayList;
import java.util.List;

/// An ordered set of `CarAction`s shown together in a head-unit action bar -- a template header
/// strip or the navigation map controls. Maps to `androidx.car.app.model.ActionStrip` and to a
/// `CPBarButton` array on CarPlay. Head units cap the number of visible actions (typically 2-4); add
/// the most important first.
public class CarActionStrip {
    private final List<CarAction> actions = new ArrayList<CarAction>();

    /// Adds an action to the strip.
    ///
    /// #### Parameters
    ///
    /// - `action`: the action to append
    ///
    /// #### Returns
    ///
    /// this strip, for chaining
    public CarActionStrip addAction(CarAction action) {
        if (action != null) {
            actions.add(action);
        }
        return this;
    }

    /// Convenience that builds a `CarAction` from a title and listener and appends it.
    ///
    /// #### Parameters
    ///
    /// - `title`: the action label
    ///
    /// - `listener`: the activation callback
    ///
    /// #### Returns
    ///
    /// this strip, for chaining
    public CarActionStrip addAction(String title, CarActionListener listener) {
        return addAction(new CarAction(title).setOnAction(listener));
    }

    /// Returns the actions in this strip, in order.
    public List<CarAction> getActions() {
        return actions;
    }
}
