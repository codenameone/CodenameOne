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
package com.codename1.annotations.buildhints;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// What a build hint attribute is, beyond what its Java signature already says.
///
/// The attribute's type, its name and its enum's constants are the compiler's
/// business and are not restated here. This carries only what javac cannot
/// infer: the wire key when it differs from the attribute name, the prose the
/// developer guide and the Settings editor show, and the handful of facts about
/// how the builders read the value.
///
/// The annotation's own `default` clause is deliberately NOT the builder's
/// default. A hint is written only where the developer set it, so the clause has
/// no meaning at runtime, and encoding the real default there cannot survive a
/// round trip -- a list collapses to `{}` and an absent enum default to the
/// first constant. [#def] states it once instead.
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Hint {

    /// The key this attribute writes, when it is not the group's prefix
    /// followed by the attribute name.
    ///
    /// `android.min_sdk_version` is not `minSdkVersion`, and the builders are
    /// the authority on the spelling.
    String name() default "";

    /// What the builder does when nobody sets the hint.
    ///
    /// Documentation, not behaviour: an attribute the developer leaves alone is
    /// not written at all, so the builder's own default applies whatever this
    /// says.
    String def() default "";

    /// What kind of string this is, when the attribute's Java type does not say.
    ///
    /// The editor picks its control from this: a version field, a masked field
    /// for a secret, a multi-line box for an XML fragment.
    HintKind kind() default HintKind.DEFAULT;

    /// Whether a cn1lib may append to this hint rather than replace it.
    ///
    /// Separate from [#separator] because the delimiter can legitimately be
    /// empty: the XML fragment family -- `android.xpermissions` and friends --
    /// is appended to with nothing between the pieces, which is not the same as
    /// a hint no library can contribute to.
    boolean appendable() default false;

    /// What a list's values are joined with on the wire.
    ///
    /// A cn1lib appends onto the same key, so this has to match what the
    /// builder splits on -- `ios.pods` is comma delimited and `ios.add_libs`
    /// semicolon delimited.
    String separator() default "";

    /// The platform this hint applies to, for the guide's table.
    ///
    /// On the annotation TYPE this is the default for every attribute in it,
    /// which is where it belongs: every hint in `@Android` is an Android hint,
    /// and saying so on each of the twenty-four was noise that could also be got
    /// wrong. An attribute states it only to disagree -- `@OnDeviceDebug` spans
    /// two platforms and each of its attributes says which.
    String platform() default "";

    /// The builders that read it, for the guide's table.
    ///
    /// On the annotation TYPE this is the default for every attribute in it. An
    /// attribute states its own only when it differs -- an Android hint that
    /// `MapsProviderInjector` also reads, or an iOS one the watch builder wants
    /// too.
    String[] consumedBy() default {};

    /// The hint this one is a deprecated second spelling of.
    ///
    /// Both names denote ONE effective setting -- the builder reads
    /// `android.captureRecord` and then lets `and.captureRecord` override it --
    /// so conflict detection has to collapse them.
    String aliasOf() default "";

    /// Why this hint is deprecated, and what to use instead.
    String deprecated() default "";

    /// Documented here but read only by the build service, so nothing in this
    /// repository consumes it.
    boolean external() default false;

    /// Available only to enterprise accounts.
    boolean enterpriseOnly() default false;

    /// Further reading, for the guide's table.
    String link() default "";
}
