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

/// Which Xcode the build server compiles an iOS build with.
///
/// The constants are the majors a current build server image carries. That
/// domain belongs to the image rather than to this framework, so it moves on
/// its own schedule and this enum can only follow at the pace of a release --
/// which is why leaving the attribute unset is the normal thing to do, and why
/// a version outside this set is still reachable through a plain
/// `codename1.arg.ios.xcode_version=<version>` line in
/// `codenameone_settings.properties`. That is not a conflict: an attribute left
/// at [#DEFAULT] writes nothing, so there is no second declaration to clash
/// with.
///
/// A minor version is the same story -- the builder accepts `27.1` and this
/// cannot spell it. The constants cover the case that is worth type checking,
/// which is naming a whole major the servers do not have.
///
/// The pre-26 values this hint once took -- `12.4`, `11.3` and the Xcode 7 to
/// 10 chain before them -- are deliberately absent. They name Xcodes that no
/// current build server installs, so offering them as typed constants would
/// promise a selection that cannot happen. A project still carrying one of them
/// in its properties file keeps whatever it has always built with.
public enum IosXcodeVersion {
    /// Say nothing, and let the build server choose.
    ///
    /// It prefers its own default and falls back to the newest Xcode it
    /// actually carries, so a server that has not been re-imaged keeps building
    /// and logs which Xcode it used. This is the right answer for almost every
    /// application: pinning a version opts out of that fallback.
    @HintUnset
    DEFAULT,

    /// Xcode 26.
    @HintValue("26")
    XCODE26,

    /// Xcode 27.
    @HintValue("27")
    XCODE27;
}
