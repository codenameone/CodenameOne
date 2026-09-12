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

import com.codename1.ui.ButtonGroup;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.RadioButton;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.spinner.Picker;
import com.codename1.properties.IntProperty;
import com.codename1.properties.Property;
import com.codename1.properties.PropertyBusinessObject;
import com.codename1.properties.PropertyIndex;
import com.codename1.properties.UiBinding;
import java.util.Date;

/// The contact form the properties chapter shows beside its binding sample.
///
/// Nothing here reads the network, the clock or the filesystem: every field is
/// bound to a property of one freshly constructed `Contact`, so the render is
/// the same on any host. The date picker is deliberately left unset -- it shows
/// its empty placeholder, which is both what the sample produces and one less
/// thing that could differ between runs.
class PropertiesBindingFigure implements GuideFigure {
    @Override
    public String id() {
        return "properties-demo-binding";
    }

    @Override
    public Form build() {
        Form hi = new Form("Contact", new BorderLayout());
        Contact c = new Contact();
        // tag::io-java-172[]
        Container resp = new Container(BoxLayout.y());
        UiBinding uib = new UiBinding();

        TextField nameTf = new TextField();
        uib.bind(c.name, nameTf);
        resp.add(c.name.getLabel()). // <1>
                add(nameTf);

        TextField emailTf = new TextField();
        emailTf.setConstraint(TextField.EMAILADDR);
        uib.bind(c.email, emailTf);
        resp.add(c.email.getLabel()).
                add(emailTf);

        TextField phoneTf = new TextField();
        phoneTf.setConstraint(TextField.PHONENUMBER);
        uib.bind(c.phone, phoneTf);
        resp.add(c.phone.getLabel()).
                add(phoneTf);

        Picker dateOfBirth = new Picker();
        dateOfBirth.setType(Display.PICKER_TYPE_DATE); // <2>
        uib.bind(c.dateOfBirth, dateOfBirth);
        resp.add(c.dateOfBirth.getLabel()).
                add(dateOfBirth);

        ButtonGroup genderGroup = new ButtonGroup();
        RadioButton male = RadioButton.createToggle("Male", genderGroup);
        RadioButton female = RadioButton.createToggle("Female", genderGroup);
        RadioButton undefined = RadioButton.createToggle("Undefined", genderGroup);
        uib.bindGroup(c.gender, new String[] {"M", "F", "U"}, male, female, undefined); // <3>
        resp.add(c.gender.getLabel()).
                add(GridLayout.encloseIn(3, male, female, undefined));

        TextField rankTf = new TextField();
        rankTf.setConstraint(TextField.NUMERIC);
        uib.bind(c.rank, rankTf); // <4>
        resp.add(c.rank.getLabel()).
                add(rankTf);
        // end::io-java-172[]
        hi.add(BorderLayout.CENTER, resp);
        hi.show();
        // Focus lands on the first focusable control, and a focused toggle resolves
        // sel# -- the accent ring -- whether or not it is checked. Focusing a text
        // field instead keeps that ring off the gender row, which is what this
        // figure is showing.
        hi.setFocused(nameTf);
        return hi;
    }

    /// The business object the sample binds against.
    public static class Contact implements PropertyBusinessObject {
        public final IntProperty<Contact> id = new IntProperty<>("id");
        public final Property<String, Contact> name = new Property<>("name");
        public final Property<String, Contact> email = new Property<>("email");
        public final Property<String, Contact> phone = new Property<>("phone");
        public final Property<Date, Contact> dateOfBirth = new Property<>("dateOfBirth", Date.class);
        public final Property<String, Contact> gender = new Property<>("gender");
        public final IntProperty<Contact> rank = new IntProperty<>("rank");
        public final PropertyIndex idx =
                new PropertyIndex(this, "Contact", id, name, email, phone, dateOfBirth, gender, rank);

        public PropertyIndex getPropertyIndex() {
            return idx;
        }
    }
}
