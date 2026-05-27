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
/// `name` is the value the target component returns from
/// `Component#getName()`. `attr` picks the attribute to write: text, UIID,
/// selected state, visible / hidden, the icon name, or the component name
/// itself.
///
/// One-way bindings push from the model to the component on
/// `Binders.bind`. Two-way bindings additionally listen for user input on
/// text fields, text areas, and check boxes so changes flow back into the
/// model.
///
/// #### Accessor resolution
///
/// The annotation processor decides how to read and write the field in
/// this order:
///
/// 1. **Explicit accessor names** -- `@Bind(getter="getX", setter="setX")`.
///    Use this when the JavaBeans naming convention doesn't match (a
///    fluent setter, a renamed boolean, ...).
/// 2. **JavaBeans accessors** when both `getter` and `setter` are blank:
///    `getFoo()` / `isFoo()` for the getter, `setFoo(T)` for the setter.
///    Detected from the project's compiled bytecode -- no runtime
///    reflection.
/// 3. **Direct public-field access** as a last resort. The processor
///    fails the build with a clear error when the field is private and
///    no usable accessor exists.
///
/// For two-way bindings the build-time processor instruments the resolved
/// setter to call `Binders.notifyChanged(this)` at every return point.
/// Application code can mutate the model through the setter from anywhere
/// and the bound component refreshes automatically; see
/// `Annotation-Component-Binding.asciidoc` for the loop-guard details.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface Bind {
    /// `Component#getName()` of the target component.
    String name();

    /// Which property of the component the field mirrors. Default: `TEXT`.
    BindAttr attr() default BindAttr.TEXT;

    /// When false the binding is one-way (model -> component only).
    /// Default `true` for `TEXT` against editable components and for
    /// `SELECTED`; for all other attributes the binding is implicitly
    /// one-way regardless of this flag.
    boolean twoWay() default true;

    /// Explicit getter method name. Default: empty -- the processor uses
    /// JavaBeans `get<Field>` / `is<Field>` discovery, then falls back to
    /// direct public-field access.
    String getter() default "";

    /// Explicit setter method name. Default: empty -- the processor uses
    /// JavaBeans `set<Field>` discovery, then falls back to direct
    /// public-field assignment. The resolved setter (whether explicit or
    /// detected) is instrumented for two-way bindings.
    String setter() default "";
}
