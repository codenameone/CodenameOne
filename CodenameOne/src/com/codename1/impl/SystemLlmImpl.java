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
package com.codename1.impl;

/// Port contract behind `com.codename1.ai.SystemLlmClient`: the language model
/// the operating system itself provides -- Apple's Foundation Models on iOS and
/// macOS, Gemini Nano through ML Kit GenAI on Android.
///
/// Every blocking method is called from one worker thread per client, never
/// from the EDT. A port returns an instance only when the builder linked the
/// bridge, which it does only for applications that use `SystemLlmClient`.
/// An instance existing says nothing about the device: a model can be absent,
/// disabled by the user or still downloading, and [#getStatus(int)] is what
/// answers that, feature by feature.
///
/// @hidden
public abstract class SystemLlmImpl {
    /// Free-form chat, the general prompt API.
    public static final int FEATURE_CHAT = 0;
    /// Summarizing a text.
    public static final int FEATURE_SUMMARIZE = 1;
    /// Correcting grammar and spelling.
    public static final int FEATURE_PROOFREAD = 2;
    /// Rewriting a text in another style; the option is the style.
    public static final int FEATURE_REWRITE = 3;
    /// Describing an image.
    public static final int FEATURE_DESCRIBE_IMAGE = 4;
    /// Images inside chat messages.
    public static final int FEATURE_IMAGE_INPUT = 5;
    /// Number of features; feature constants are below it.
    public static final int FEATURE_COUNT = 6;

    /// The OS, the device or this build cannot offer the feature at all.
    public static final int STATUS_UNSUPPORTED = 0;
    /// The device can run it, but the user turned the OS feature off.
    public static final int STATUS_DISABLED = 1;
    /// Supported; the model has to be downloaded first ([#download(int)]).
    public static final int STATUS_DOWNLOADABLE = 2;
    /// Supported; the model is being downloaded or prepared by the OS.
    public static final int STATUS_DOWNLOADING = 3;
    /// Ready to use.
    public static final int STATUS_AVAILABLE = 4;

    /// @return the provider name, such as `apple-foundation-models` or
    /// `gemini-nano`
    public abstract String getProvider();

    /// Asks the OS whether a feature can run now. May block briefly.
    ///
    /// @param feature one of the `FEATURE_` constants
    /// @return one of the `STATUS_` constants
    public abstract int getStatus(int feature);

    /// Explains the last [#getStatus(int)] answer for a feature.
    ///
    /// @param feature one of the `FEATURE_` constants
    /// @return a human readable reason, or `null` when there is nothing to add
    public abstract String getStatusReason(int feature);

    /// Downloads the model behind a feature and blocks until it is ready.
    ///
    /// @param feature one of the `FEATURE_` constants
    /// @return `false` when the download failed; [#getLastError(int)] with
    /// `0` describes why
    public abstract boolean download(int feature);

    /// Starts a generation.
    ///
    /// @param feature one of the `FEATURE_` constants
    /// @param roles `system`, `user` or `assistant`, one per message; for the
    /// single-text features a single `user` entry holding the text
    /// @param contents the text of each message
    /// @param image encoded image (PNG or JPEG) for [#FEATURE_DESCRIBE_IMAGE]
    /// or [#FEATURE_IMAGE_INPUT], otherwise `null`
    /// @param option feature specific: the rewrite style for [#FEATURE_REWRITE],
    /// the bullet count for [#FEATURE_SUMMARIZE], otherwise `0`
    /// @param maxTokens upper bound on generated tokens, `0` for the default
    /// @param temperature sampling temperature, negative for the default
    /// @param seed sampling seed, `-1` for none
    /// @return a positive handle, or `0` on failure
    public abstract int start(int feature, String[] roles, String[] contents, byte[] image,
            int option, int maxTokens, float temperature, int seed);

    /// Waits for the next piece of text.
    ///
    /// @param handle generation handle
    /// @return the next piece, possibly empty, or `null` once generation ended
    public abstract String next(int handle);

    /// Describes the last failure.
    ///
    /// @param handle a handle, or `0` for the last failed [#start] or
    /// [#download(int)]
    /// @return the message, or `null` when generation ended normally
    public abstract String getLastError(int handle);

    /// Stops a generation, if it is still running, and frees the handle.
    ///
    /// @param handle generation handle
    public abstract void close(int handle);
}
