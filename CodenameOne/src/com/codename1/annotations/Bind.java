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

import com.codename1.binding.BindAttr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Pairs a `@Bindable` field with the component it should mirror.
///
/// `name` is the value the target component returns from `Component#getName()`.
/// `attr` picks the attribute to write: text, UIID, selected state, visible /
/// hidden, the icon name, or the component name itself.
///
/// One-way bindings push from the model to the component on `Binders.bind`.
/// Two-way bindings additionally listen for user input on text fields, text
/// areas, and check boxes so changes flow back into the model.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface Bind {
    /// `Component#getName()` of the target component.
    String name();

    /// Which property of the component the field mirrors. Default: `TEXT`.
    BindAttr attr() default BindAttr.TEXT;

    /// When false the binding is one-way (model -> component only). Default
    /// `true` for `TEXT` against editable components and for `SELECTED`; for
    /// all other attributes the binding is implicitly one-way.
    boolean twoWay() default true;
}
