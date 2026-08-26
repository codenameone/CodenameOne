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

/// A build hint the build server reads as true or false, plus the state of not
/// having said either.
///
/// Not `boolean`, which cannot express the third state. `boolean x() default
/// false` is read by anyone looking at it -- and by IDE completion -- as "off
/// unless you turn it on", and for many hints that is simply untrue:
/// `android.appBundle` defaults to ON at the server. The old declaration was
/// harmless on the wire, because an attribute nobody writes is not written into
/// the request either way, but it stated something about the server that the
/// client does not get to decide and cannot keep true.
///
/// So the third state is named instead of guessed. [#DEFAULT] sends nothing and
/// lets the server decide; [#ON] and [#OFF] are the developer overriding it.
public enum Toggle {
    /// Say nothing, and let the build server apply its own default.
    @HintUnset
    DEFAULT,

    /// Turn the hint on, whatever the server would otherwise have done.
    @HintValue("true")
    ON,

    /// Turn the hint off, whatever the server would otherwise have done.
    @HintValue("false")
    OFF;
}
