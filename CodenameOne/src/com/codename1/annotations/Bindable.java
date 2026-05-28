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
package com.codename1.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Marks a POJO or `PropertyBusinessObject` as a target for the component
/// binding processor. The Codename One Maven plugin generates a `Binder` next
/// to the class that copies the marked fields into the matching components of
/// a `Container` -- and back, for two-way bindings against `TextField`,
/// `TextArea`, `CheckBox`, and friends.
///
/// ```java
/// @Bindable
/// public class LoginModel {
///     @Bind(name="userField", attr=BindAttr.TEXT)
///     public Property<String, LoginModel> user = new Property<>("user");
///
///     @Bind(name="rememberMe", attr=BindAttr.SELECTED)
///     public boolean remember;
/// }
///
/// LoginModel model = new LoginModel();
/// Binders.bind(model, form);
/// // user types into userField -- model.user.get() observes the change
/// // model.user.set("alice") -- userField re-renders with the new text
/// ```
///
/// Lookup happens through `Component#getComponentForm().findByName(name)` so
/// the form's GUI builder names line up with the model field names.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Bindable {
}
