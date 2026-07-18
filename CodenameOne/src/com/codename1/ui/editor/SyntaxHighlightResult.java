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
package com.codename1.ui.editor;

import java.util.List;

/// Result of highlighting one source line, including the opaque state passed to the following line.
public final class SyntaxHighlightResult {
    /// Ordered, non-overlapping tokens for the line.
    public final List<SyntaxToken> tokens;
    /// Opaque state passed to the highlighter for the next line.
    public final int endState;

    /// Creates a line result.
    public SyntaxHighlightResult(List<SyntaxToken> tokens, int endState) {
        if (tokens == null) {
            throw new IllegalArgumentException("tokens must not be null");
        }
        this.tokens = tokens;
        this.endState = endState;
    }
}
