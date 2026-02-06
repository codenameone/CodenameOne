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

package com.codename1.ui;

import com.codename1.ui.spinner.Picker;

import java.util.Date;

/// A picker component similar to `com.codename1.ui.TextComponent` that adapts to native UI
/// conventions and leverages the `com.codename1.ui.spinner.Picker` API. See the docs for
/// `com.codename1.ui.InputComponent` for more options and coverage.
///
/// The following code demonstrates a simple set of inputs and validation as it appears in iOS, Android and with
/// validation errors
///
/// ```java
/// TextModeLayout tl = new TextModeLayout(3, 2);
/// Form f = new Form("Pixel Perfect", tl);
///
/// TextComponent title = new TextComponent().label("Title");
/// TextComponent price = new TextComponent().label("Price");
/// TextComponent location = new TextComponent().label("Location");
/// PickerComponent date = PickerComponent.createDate(new Date()).label("Date");
/// TextComponent description = new TextComponent().label("Description").multiline(true);
///
/// Validator val = new Validator();
/// val.addConstraint(title, new LengthConstraint(2));
/// val.addConstraint(price, new NumericConstraint(true));
///
/// f.add(tl.createConstraint().widthPercentage(60), title);
/// f.add(tl.createConstraint().widthPercentage(40), date);
/// f.add(location);
/// f.add(price);
/// f.add(tl.createConstraint().horizontalSpan(2), description);
/// f.setEditOnShow(title.getField());
///
/// f.show();
/// ```
///
/// @author Shai Almog
public class PickerComponent extends InputComponent {
    private final Picker picker = createPickerInstance();

    /// Allows subclassing the picker component for customization
    protected PickerComponent() {
        initInput();
        picker.setTickerEnabled(false);
    }

    /// Creates a strings picker component
    ///
    /// #### Parameters
    ///
    /// - `values`: the values for the picker
    ///
    /// #### Returns
    ///
    /// a strings version of the picker component
    public static PickerComponent createStrings(String... values) {
        PickerComponent p = new PickerComponent();
        p.picker.setType(Display.PICKER_TYPE_STRINGS);
        p.picker.setStrings(values);
        p.picker.setSelectedString(values[0]);
        return p;
    }

    /// Creates a date picker component
    ///
    /// #### Parameters
    ///
    /// - `date`: the initial date in the picker
    ///
    /// #### Returns
    ///
    /// a date version of the picker component
    public static PickerComponent createDate(Date date) {
        PickerComponent p = new PickerComponent();
        p.picker.setType(Display.PICKER_TYPE_DATE);
        p.picker.setDate(date);
        return p;
    }

    /// Creates a date + time picker component
    ///
    /// #### Parameters
    ///
    /// - `date`: the initial date in the picker
    ///
    /// #### Returns
    ///
    /// a date + time version of the picker component
    public static PickerComponent createDateTime(Date date) {
        PickerComponent p = new PickerComponent();
        p.picker.setType(Display.PICKER_TYPE_DATE_AND_TIME);
        p.picker.setDate(date);
        return p;
    }

    /// Creates a time picker component
    ///
    /// #### Parameters
    ///
    /// - `minutes`: minutes since midnight as a time of day
    ///
    /// #### Returns
    ///
    /// a time version of the picker component
    public static PickerComponent createTime(int minutes) {
        PickerComponent p = new PickerComponent();
        p.picker.setType(Display.PICKER_TYPE_TIME);
        p.picker.setTime(minutes);
        return p;
    }

    /// Creates a duration minutes picker component
    ///
    /// #### Parameters
    ///
    /// - `ms`: the duration value in milliseconds
    ///
    /// #### Returns
    ///
    /// a duration version of the picker component
    public static PickerComponent createDurationMinutes(int ms) {
        PickerComponent p = new PickerComponent();
        p.picker.setType(Display.PICKER_TYPE_DURATION_MINUTES);
        p.picker.setDuration(ms);
        return p;
    }

    /// Creates a duration hours + minutes picker component
    ///
    /// #### Parameters
    ///
    /// - `hours`: the number of hours
    ///
    /// - `minutes`: the number of minutes
    ///
    /// #### Returns
    ///
    /// a duration version of the picker component
    public static PickerComponent createDurationHoursMinutes(int hours, int minutes) {
        PickerComponent p = new PickerComponent();
        p.picker.setType(Display.PICKER_TYPE_DURATION_HOURS);
        p.picker.setDuration(hours, minutes);
        return p;
    }

    /// Allows developers to subclass the Picker component instance to customize
    /// behaviors of the subclass
    ///
    /// #### Returns
    ///
    /// the picker instance
    protected Picker createPickerInstance() {
        return new Picker();
    }

    /// {@inheritDoc}
    @Override
    public final Component getEditor() {
        return picker;
    }

    /// Returns the picker instance
    ///
    /// #### Returns
    ///
    /// the picker
    public Picker getPicker() {
        return picker;
    }

    /// Overridden for covariant return type
    /// {@inheritDoc}
    @Override
    public PickerComponent onTopMode(boolean onTopMode) {
        super.onTopMode(onTopMode);
        return this;
    }

    /// Overridden for covariant return type
    /// {@inheritDoc}
    @Override
    public PickerComponent errorMessage(String errorMessage) {
        super.errorMessage(errorMessage);
        return this;
    }

    /// Overridden for covariant return type
    /// {@inheritDoc}
    /// }
    @Override
    public PickerComponent label(String text) {
        super.label(text);
        return this;
    }
}
