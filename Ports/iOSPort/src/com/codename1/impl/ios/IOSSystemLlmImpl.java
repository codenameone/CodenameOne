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
package com.codename1.impl.ios;

import com.codename1.impl.SystemLlmImpl;

/// Apple's Foundation Models on iOS and macOS, over `CN1SystemLlm.m` and the
/// Swift `CN1SystemLanguageModel` the builder injects for applications that use
/// `SystemLlmClient`. Elsewhere `cn1SystemLlmIsLinked` answers false and no
/// instance is created.
final class IOSSystemLlmImpl extends SystemLlmImpl {
    @Override
    public String getProvider() {
        String p = IOSImplementation.nativeInstance.cn1SystemLlmProvider();
        return p == null ? "apple-foundation-models" : p;
    }

    @Override
    public int getStatus(int feature) {
        return IOSImplementation.nativeInstance.cn1SystemLlmStatus(feature);
    }

    @Override
    public String getStatusReason(int feature) {
        return IOSImplementation.nativeInstance.cn1SystemLlmStatusReason(feature);
    }

    @Override
    public boolean download(int feature) {
        return IOSImplementation.nativeInstance.cn1SystemLlmDownload(feature);
    }

    @Override
    public int start(int feature, String[] roles, String[] contents, byte[] image, int option,
            int maxTokens, float temperature, int seed) {
        return IOSImplementation.nativeInstance.cn1SystemLlmStart(feature, roles, contents,
                image, option, maxTokens, temperature, seed);
    }

    @Override
    public String next(int handle) {
        return IOSImplementation.nativeInstance.cn1SystemLlmNext(handle);
    }

    @Override
    public String getLastError(int handle) {
        return IOSImplementation.nativeInstance.cn1SystemLlmLastError(handle);
    }

    @Override
    public void close(int handle) {
        IOSImplementation.nativeInstance.cn1SystemLlmClose(handle);
    }
}
