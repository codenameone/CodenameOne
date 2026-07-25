/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.impl;

import com.codename1.ai.language.LanguageCandidate;
import com.codename1.ai.language.LanguageOptions;
import com.codename1.ai.language.SmartReplyMessage;
import com.codename1.util.AsyncResource;

/** Port contract behind the built-in on-device language APIs. @hidden */
public abstract class LanguageImpl {
    public abstract boolean isSupported(String feature, String backendId);
    public abstract AsyncResource<LanguageCandidate[]> identify(
            String text, String backendId, LanguageOptions options);
    public abstract AsyncResource<String> translate(
            String text, String sourceLanguage, String targetLanguage,
            String backendId, LanguageOptions options);
    public abstract AsyncResource<String[]> suggestReplies(
            SmartReplyMessage[] conversation, String backendId, LanguageOptions options);
    public abstract void close();
}
