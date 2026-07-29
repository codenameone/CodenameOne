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
package com.codename1.ai.language;

/// Selects native backends for language identification, translation, and
/// Smart Reply. Automatic selection uses ML Kit on Android, Apple Natural
/// Language for iOS identification. iOS translation and Smart Reply require
/// their feature-specific ML Kit selector methods.
///
/// The feature-specific methods are also build-time dependency markers. They
/// intentionally remain distinct even though they return the same runtime
/// backend id, because the builder scans each call site to include only the
/// requested ML Kit component.
public final class LanguageBackends {
    private static final LanguageBackend AUTO = new Named("auto");
    private static final LanguageBackend ML_KIT = new Named("ml-kit");
    private static final LanguageBackend APPLE_NATURAL_LANGUAGE =
            new Named("apple-natural-language");

    private LanguageBackends() {
    }

    /// @return the platform-recommended dependency-minimal backend
    public static LanguageBackend auto() {
        return AUTO;
    }

    /// Selects Apple's dependency-free Natural Language framework for
    /// language identification. This backend is available on iOS 12 and
    /// newer and is not available on Android.
    ///
    /// @return the Apple Natural Language backend selector
    public static LanguageBackend appleNaturalLanguage() {
        return APPLE_NATURAL_LANGUAGE;
    }

    /// Selects ML Kit specifically for language identification. Calling this
    /// method lets the builder add the Language ID pod only when the
    /// application opts out of the Apple-native iOS default.
    ///
    /// @return the ML Kit language-identification selector
    public static LanguageBackend mlKitLanguageIdentification() {
        return ML_KIT;
    }

    /// Selects ML Kit translation. Calling this method is the build-time
    /// marker that adds the translation pod on iOS; merely referencing
    /// {@link Translator} does not add it or disable the arm64 simulator.
    ///
    /// @return the ML Kit translation selector
    public static LanguageBackend mlKitTranslation() {
        return ML_KIT;
    }

    /// Selects ML Kit Smart Reply. Calling this method is the build-time
    /// marker that adds the Smart Reply pod on iOS; merely referencing
    /// {@link SmartReply} does not add it or disable the arm64 simulator.
    ///
    /// @return the ML Kit Smart Reply selector
    public static LanguageBackend mlKitSmartReply() {
        return ML_KIT;
    }

    private static final class Named implements LanguageBackend {
        private final String id;

        private Named(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }
    }
}
