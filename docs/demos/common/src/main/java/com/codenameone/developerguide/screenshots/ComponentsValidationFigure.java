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

package com.codenameone.developerguide.screenshots;

import com.codename1.ui.Button;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.validation.LengthConstraint;
import com.codename1.ui.validation.RegexConstraint;
import com.codename1.ui.validation.Validator;

/// A form under a Validator, with the constraints the chapter sets up.
class ComponentsValidationFigure implements GuideFigure {

    @Override
    public String id() {
        return "validation-regex-masking-1";
    }

    /// The tagged region is what the chapter includes, so the listing beside the
    /// picture is the code that drew it.
    @Override
    public Form build() {
        // tag::the-components-of-codename-one-java-161[]
        TextField firstName = new TextField("", "First Name");
        TextField surname = new TextField("", "Surname");
        TextField url = new TextField("", "URL", 20, TextField.URL);
        TextField email = new TextField("", "E-Mail", 20, TextField.EMAILADDR);
        TextField phone = new TextField("", "Phone", 20, TextField.PHONENUMBER);
        String phoneRegex = "[0-9\\-\\+ ]+";
        TextField num1 = new TextField("", "", 5, TextField.NUMERIC);
        TextField num2 = new TextField("", "", 5, TextField.NUMERIC);
        TextField num3 = new TextField("", "", 5, TextField.NUMERIC);
        TextField num4 = new TextField("", "", 5, TextField.NUMERIC);
        Button submit = new Button("Submit");
        // the masking from the previous sample still applies to these fields
        automoveToNext(num1, num2);
        automoveToNext(num2, num3);
        automoveToNext(num3, num4);
        num4.setMaxSize(4);

        Validator v = new Validator();
        v.addConstraint(firstName, new LengthConstraint(2)).
                addConstraint(surname, new LengthConstraint(2)).
                addConstraint(url, RegexConstraint.validURL()).
                addConstraint(email, RegexConstraint.validEmail()).
                addConstraint(phone, new RegexConstraint(phoneRegex, "Must be valid phone number")).
                addConstraint(num1, new LengthConstraint(4)).
                addConstraint(num2, new LengthConstraint(4)).
                addConstraint(num3, new LengthConstraint(4)).
                addConstraint(num4, new LengthConstraint(4));

        v.addSubmitButtons(submit);

        Form hi = new Form("Validation", new BoxLayout(BoxLayout.Y_AXIS));
        hi.add(firstName).add(surname).add(url).add(email).add(phone).
                add(num1).add(num2).add(num3).add(num4).add(submit);
        hi.show();
        // end::the-components-of-codename-one-java-161[]
        return hi;
    }

    /// The masking helper the previous section builds, carried here so the
    /// listing above runs rather than compiling against a stub.
    private void automoveToNext(final TextField current, final TextField next) {
        current.addDataChangedListener((type, index) -> {
            String val = current.getText();
            if (val.length() > 4) {
                current.stopEditing();
                current.setText(val.substring(0, 4));
                String rest = val.substring(4);
                next.setText(rest);
                if (rest.length() <= 4) {
                    next.startEditingAsync();
                }
            }
        });
    }
}
