/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.ai.language;

/** Backend selectors for language identification, translation, and smart reply. */
public final class LanguageBackends {
    private static final LanguageBackend AUTO = new Named("auto");
    private static final LanguageBackend ML_KIT = new Named("ml-kit");
    private static final LanguageBackend LITE_RT = new Named("litert");

    private LanguageBackends() {
    }

    public static LanguageBackend auto() {
        return AUTO;
    }

    public static LanguageBackend mlKit() {
        return ML_KIT;
    }

    public static LanguageBackend liteRt() {
        return LITE_RT;
    }

    private static final class Named implements LanguageBackend {
        private final String id;

        private Named(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }
}
