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
package com.codename1.builders;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Copies the Swift bridge to Apple's Foundation Models into an Apple app build
 * that uses {@code com.codename1.ai.SystemLlmClient}.
 *
 * <p>The bridge is a builder resource rather than a file in the port's
 * {@code nativeSources}, for the reason {@link SmartHomeInjector} gives: every
 * file there is unpacked into every build, and a {@code .swift} file, unlike an
 * {@code .m} behind an {@code #ifdef}, turns the whole project into a Swift
 * project. Only apps that use the client pay for that.</p>
 *
 * <p>The Swift file imports the framework under
 * {@code #if canImport(FoundationModels)}, so an Xcode whose SDK predates it
 * compiles a stub branch that references nothing. {@code IPhoneBuilder}
 * declares the framework, weakly, only when its Xcode is new enough to have
 * it; naming it on an older one would fail the link on the name alone.</p>
 */
public final class SystemLlmInjector {
    static final String SWIFT_FILE = "CN1SystemLanguageModel.swift";
    private static final String RESOURCE =
            "/com/codename1/builders/ai/CN1SystemLanguageModel.swifts";

    private SystemLlmInjector() {
    }

    /**
     * Copies the bridge into a source directory of the app target.
     *
     * <p>Call it before the builder decides whether the project has Swift
     * sources, so the Swift build settings that decision adds are applied.</p>
     *
     * @param exec      the running builder, for resource access
     * @param appSrcDir a directory whose Swift files are compiled into the app
     *                  target
     */
    public static void injectApple(Executor exec, File appSrcDir) {
        try {
            appSrcDir.mkdirs();
            InputStream is = exec.getResourceAsStream(RESOURCE);
            if (is == null) {
                throw new IllegalStateException("Missing resource " + RESOURCE);
            }
            FileOutputStream os = new FileOutputStream(new File(appSrcDir, SWIFT_FILE));
            try {
                Executor.copy(is, os);
            } finally {
                os.close();
                is.close();
            }
        } catch (Exception ex) {
            // Loud: without the bridge the app builds and then reports the
            // system model unsupported on every device, which reads as a
            // device problem rather than a build problem.
            throw new RuntimeException("Failed to inject the Foundation Models bridge", ex);
        }
    }
}
