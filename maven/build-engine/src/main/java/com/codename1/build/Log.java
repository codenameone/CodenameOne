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
package com.codename1.build;

/// The logger the build engine writes to.
///
/// It has exactly the methods of Maven's `org.apache.maven.plugin.logging.Log`,
/// so the engine code that used to take Maven's type reads the same, and each
/// build tool adapts its own logger to it: the Maven plugin wraps
/// `getLog()`, the Gradle plugin wraps the task logger.
public interface Log {
    /// Whether debug output is wanted; callers skip building expensive debug
    /// messages when it is not.
    boolean isDebugEnabled();

    /// Logs a debug message.
    void debug(CharSequence content);

    /// Logs a debug message with a cause.
    void debug(CharSequence content, Throwable error);

    /// Logs a debug cause.
    void debug(Throwable error);

    /// Whether info output is wanted.
    boolean isInfoEnabled();

    /// Logs an info message.
    void info(CharSequence content);

    /// Logs an info message with a cause.
    void info(CharSequence content, Throwable error);

    /// Logs an info cause.
    void info(Throwable error);

    /// Whether warnings are wanted.
    boolean isWarnEnabled();

    /// Logs a warning.
    void warn(CharSequence content);

    /// Logs a warning with a cause.
    void warn(CharSequence content, Throwable error);

    /// Logs a warning cause.
    void warn(Throwable error);

    /// Whether errors are wanted.
    boolean isErrorEnabled();

    /// Logs an error.
    void error(CharSequence content);

    /// Logs an error with a cause.
    void error(CharSequence content, Throwable error);

    /// Logs an error cause.
    void error(Throwable error);
}
