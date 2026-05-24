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
package com.codename1.ai;

/// Controls how aggressively the model will call tools. Use the
/// constants for the three common modes; use [#named(String)] to force
/// the model to call a specific tool.
public final class ToolChoice {
    /// Model picks freely between calling tools and replying with text.
    public static final ToolChoice AUTO = new ToolChoice("auto", null);

    /// Model must not call any tool — it must reply with text.
    public static final ToolChoice NONE = new ToolChoice("none", null);

    /// Model must call exactly one tool (any tool). Useful for forcing
    /// a structured-output path.
    public static final ToolChoice REQUIRED = new ToolChoice("required", null);

    private final String mode;
    private final String forcedToolName;

    private ToolChoice(String mode, String forcedToolName) {
        this.mode = mode;
        this.forcedToolName = forcedToolName;
    }

    /// Forces the model to call the named tool.
    public static ToolChoice named(String toolName) {
        if (toolName == null || toolName.length() == 0) {
            throw new IllegalArgumentException("toolName is required");
        }
        return new ToolChoice("named", toolName);
    }

    public String getMode() {
        return mode;
    }

    public String getForcedToolName() {
        return forcedToolName;
    }
}
