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

/// Runs this method of a bean on a schedule.
///
/// Exactly one of [#cron], [#fixedRate] or [#fixedDelay] (or their `String`
/// forms) must be given. The method takes no arguments.
///
/// A literal cron expression is parsed by the build, which writes its fields
/// into the entry point as bit masks and refuses a malformed one: an expression
/// nobody noticed was wrong otherwise fires never, or every second. One that
/// reads configuration (`${report.cron}`) is parsed once at start-up instead.
///
/// The format is Spring's six fields -- second, minute, hour, day of month,
/// month, day of week -- plus the `@yearly`, `@monthly`, `@weekly`, `@daily`
/// and `@hourly` macros:
///
/// ```java
/// @Scheduled(cron = "0 */15 * * * MON-FRI")
/// public void refresh() { ... }
/// ```
///
/// Several instances of one server each run every job. [#lock] makes a job run
/// on one of them at a time, through a row in the database.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Scheduled {
    /// A cron expression, or `${key}` to read one from configuration.
    String cron() default "";
    /// The time zone the cron expression is read in: a zone ID such as
    /// `Europe/Berlin`, a fixed offset such as `+02:00`, or `UTC`. Empty means
    /// UTC, which is what a server should keep its clock in anyway.
    String zone() default "";
    /// Milliseconds between the starts of consecutive runs.
    long fixedRate() default -1;
    /// Milliseconds between the end of one run and the start of the next.
    long fixedDelay() default -1;
    /// Milliseconds before the first run of a fixed-rate or fixed-delay job.
    long initialDelay() default -1;
    /// [#fixedRate] as text, which may read configuration.
    String fixedRateString() default "";
    /// [#fixedDelay] as text, which may read configuration.
    String fixedDelayString() default "";
    /// [#initialDelay] as text, which may read configuration.
    String initialDelayString() default "";
    /// Which kind of thread runs it.
    ThreadKind thread() default ThreadKind.PLATFORM;
    /// The executor to run on. Empty means `scheduling`.
    String executor() default "";
    /// A lock name. When set, a run first claims the named row in the
    /// `cn1_scheduler_lock` table and is skipped if another instance holds it.
    /// Needs a database.
    String lock() default "";
    /// How long a claimed lock is held at most, in milliseconds, so an instance
    /// that dies mid-run does not hold it for good. Zero or less means ten
    /// minutes.
    long lockAtMostFor() default -1;
}
