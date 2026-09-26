/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.ui.spinner;

/// The value contract every lightweight picker spinner implements, so
/// [Picker][com.codename1.ui.spinner.Picker] can drive any of them without
/// knowing which one it holds.
///
/// A `Picker` in lightweight mode owns one of the `*Spinner3D` containers
/// according to its type -- date, time, date and time, duration or a plain
/// string list -- and moves a value in and out of it through this interface
/// alone. Implementing it is what makes a container usable as the body of a
/// picker.
///
/// The value's runtime type is the implementation's own: `Date` for
/// [DateSpinner3D][com.codename1.ui.spinner.DateSpinner3D], an `int[]` of
/// hours and minutes for
/// [DurationSpinner3D][com.codename1.ui.spinner.DurationSpinner3D], and so on.
/// Each implementation documents what it expects; passing something else is a
/// programming error rather than a recoverable condition.
///
/// This is public because the lightweight spinners are, and those are usable
/// on their own -- embedded in a form rather than shown in a picker dialog.
/// The name is retained for source compatibility with the releases in which it
/// was package private.
///
/// @author shannah
public interface InternalPickerWidget {
    /// The currently selected value.
    ///
    /// #### Returns
    ///
    /// the selection, in whatever type this implementation documents; never
    /// null once the widget has been laid out
    Object getValue();

    /// Moves the selection to `value`.
    ///
    /// The widget scrolls to the new selection rather than jumping, when it is
    /// already on screen.
    ///
    /// #### Parameters
    ///
    /// - `value`: the new selection, in the type this implementation
    ///   documents. A value outside the widget's range is clamped into it.
    void setValue(Object value);
}
