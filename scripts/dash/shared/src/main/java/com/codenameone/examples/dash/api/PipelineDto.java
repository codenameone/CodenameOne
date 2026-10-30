/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.dash.api;

import com.codename1.annotations.Mapped;
import java.util.List;

/// An engagement pipeline: when a player matches the conditions, do the
/// action, at most once per `cooldownDays` for each player. The server runs
/// every enabled pipeline once an hour.
///
/// @param action `push` (sends title and body, opening screen) or `gems` (gives amount gems, with a push saying so)
/// @param reached players acted on, ever
/// @param lastRun when the pipeline last ran, in milliseconds, or 0
@Mapped
public record PipelineDto(String id, String name, boolean enabled, List<ConditionDto> conditions, String action,
        String title, String body, String screen, int amount, int cooldownDays, int reached, long lastRun) {
}
