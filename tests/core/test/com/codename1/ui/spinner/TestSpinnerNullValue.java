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

import com.codename1.testing.AbstractTest;
import com.codename1.ui.list.DefaultListModel;

/// Regression test for issue #5069: calling `Picker.setDate(null)` while the
/// lightweight popup is on screen forwards the `null` straight to the live
/// `InternalPickerWidget#setValue`, which used to NPE in every date-type
/// spinner (unchecked cast / unboxing). `TimeSpinner3D` already tolerated
/// `null`, which is why only some picker types crashed. The spinner widgets now
/// uniformly treat a `null` value as a no-op (leave the wheels untouched). This
/// lives in the `com.codename1.ui.spinner` package so it can reach the
/// package-private widget classes directly.
public class TestSpinnerNullValue extends AbstractTest {

    @Override
    public boolean runTest() throws Exception {
        // Each setValue(null) must not throw; previously these NPE'd while
        // casting/unboxing the null value (#5069).
        new DateSpinner3D().setValue(null);
        new DateTimeSpinner3D().setValue(null);
        new CalendarPicker().setValue(null);
        new DurationSpinner3D(DurationSpinner3D.FIELD_HOUR | DurationSpinner3D.FIELD_MINUTE).setValue(null);
        new Spinner3D(new DefaultListModel<String>("a", "b", "c")).setValue(null);
        return true;
    }

    @Override
    public boolean shouldExecuteOnEDT() {
        return true;
    }
}
