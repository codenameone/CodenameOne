/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui;

import com.codename1.util.SuccessCallback;

import java.util.List;

/// Supplies IDE style code completion proposals to a `CodeEditor`.
///
/// The provider is invoked by the editor as the user types (or when completion is explicitly triggered)
/// and returns its proposals asynchronously, which makes it suitable both for fast in-memory completion
/// and for completion driven by a remote language server.
///
/// #### Example
///
/// ```java
/// editor.setCompletionProvider((ed, code, cursor, results) -> {
///     String prefix = currentWord(code, cursor);
///     List<CodeCompletion> out = new ArrayList<>();
///     for (String kw : KEYWORDS) {
///         if (kw.startsWith(prefix)) {
///             out.add(new CodeCompletion(kw).setType("keyword"));
///         }
///     }
///     results.onSucess(out);
/// });
/// ```
///
/// @author Shai Almog
public interface CodeCompletionProvider {
    /// Requests completion proposals for the given editor state. Implementations must eventually invoke
    /// `results.onSucess(list)` (possibly asynchronously); passing an empty list or null hides the
    /// completion popup.
    ///
    /// #### Parameters
    ///
    /// - `editor`: the editor requesting completions
    ///
    /// - `code`: the full editor text
    ///
    /// - `cursorPosition`: the character offset of the caret within `code`
    ///
    /// - `results`: callback to deliver the proposals
    void getCompletions(CodeEditor editor, String code, int cursorPosition, SuccessCallback<List<CodeCompletion>> results);
}
