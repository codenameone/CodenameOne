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
package com.codename1.gradle;

import com.codename1.build.Log;
import org.gradle.api.logging.Logger;

/// The build engine's [Log] over a Gradle task logger.
///
/// Info goes to lifecycle, which Gradle shows by default, because the engine's
/// info messages (what is being built, where the output went) are the ones a
/// Maven user sees at the default level too.
public final class GradleLog implements Log {
    private final Logger logger;

    public GradleLog(Logger logger) {
        this.logger = logger;
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void debug(CharSequence content) {
        logger.debug(String.valueOf(content));
    }

    @Override
    public void debug(CharSequence content, Throwable error) {
        logger.debug(String.valueOf(content), error);
    }

    @Override
    public void debug(Throwable error) {
        logger.debug(String.valueOf(error), error);
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isLifecycleEnabled();
    }

    @Override
    public void info(CharSequence content) {
        logger.lifecycle(String.valueOf(content));
    }

    @Override
    public void info(CharSequence content, Throwable error) {
        logger.lifecycle(String.valueOf(content), error);
    }

    @Override
    public void info(Throwable error) {
        logger.lifecycle(String.valueOf(error), error);
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public void warn(CharSequence content) {
        logger.warn(String.valueOf(content));
    }

    @Override
    public void warn(CharSequence content, Throwable error) {
        logger.warn(String.valueOf(content), error);
    }

    @Override
    public void warn(Throwable error) {
        logger.warn(String.valueOf(error), error);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public void error(CharSequence content) {
        logger.error(String.valueOf(content));
    }

    @Override
    public void error(CharSequence content, Throwable error) {
        logger.error(String.valueOf(content), error);
    }

    @Override
    public void error(Throwable error) {
        logger.error(String.valueOf(error), error);
    }
}
