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
package com.codename1.backend.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Runs this method on another thread; the caller returns at once.
///
/// The method returns `void`, or a `java.util.concurrent.Future` -- typically
/// `com.codename1.backend.AsyncResult.of(value)` -- which the caller receives
/// immediately and which completes when the work does. Anything else is a build
/// error, because a caller could not receive it.
///
/// The build rewrites the compiled method so that it packages its arguments into
/// a task and hands the task to the named executor; there is no proxy, so this
/// works however the method is called. An exception from a `void` method is
/// logged and recorded on the current span, since nobody is waiting for it.
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Async {
    /// The executor to run on, configured as `cn1.task.executor.<name>.*`.
    /// Empty means `default`.
    String value() default "";
    /// Which kind of thread.
    ThreadKind thread() default ThreadKind.PLATFORM;
}
