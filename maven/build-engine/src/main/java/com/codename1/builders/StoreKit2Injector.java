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
 * Copies the Swift bridge to StoreKit 2 into an Apple app build that uses
 * {@code com.codename1.payment}.
 *
 * <p>StoreKit 2 has no Objective-C API. The bridge is a builder resource
 * rather than a file in the port's {@code nativeSources} for the reason
 * {@link SystemLlmInjector} gives: a {@code .swift} file there would make every
 * application a Swift project. {@code CN1StoreKit.m} finds the class by name
 * and uses it on an OS that has StoreKit 2; below that, and in a build without
 * the bridge, the StoreKit 1 natives run unchanged. StoreKit.framework is
 * declared by the builder for any purchase app already.</p>
 */
public final class StoreKit2Injector {
    static final String SWIFT_FILE = "CN1StoreKit2.swift";
    private static final String RESOURCE =
            "/com/codename1/builders/storekit/CN1StoreKit2.swifts";

    private StoreKit2Injector() {
    }

    /**
     * Whether a build wants StoreKit 2: {@code ios.storeKit2=true}, or for a
     * macOS build {@code macos.storeKit2} falling back to it. Off unless
     * {@code true}: with the bridge a receipt's order data is the signed
     * transaction rather than the app receipt, which a developer server that
     * posts it to Apple's {@code verifyReceipt} cannot read, so it is the
     * developer's choice to make. Without the bridge the port's StoreKit 1
     * natives run on every OS version.
     *
     * @param request the build
     * @param mac     true for a macOS build
     * @return true when the build turned StoreKit 2 on
     */
    public static boolean enabled(BuildRequest request, boolean mac) {
        String v = request.getArg("ios.storeKit2", "false");
        if (mac) {
            v = request.getArg("macos.storeKit2", v);
        }
        return "true".equalsIgnoreCase(v.trim());
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
            // Loud: without the bridge the app silently falls back to
            // StoreKit 1, which the build asked not to use.
            throw new RuntimeException("Failed to inject the StoreKit 2 bridge", ex);
        }
    }
}
