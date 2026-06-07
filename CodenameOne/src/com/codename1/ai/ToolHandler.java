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

/// Executor backing a [Tool]. The handler receives the raw JSON
/// argument string the model produced and returns the JSON result
/// to send back to the model in a [ToolResultPart]. Implementations
/// are responsible for parsing `argumentsJson` (typically with
/// [com.codename1.io.JSONParser]) and for serializing the result.
///
/// Pair with [Tool] via the four-argument constructor, then call
/// [ToolCall#execute(java.util.List)] to dispatch.
public interface ToolHandler {
    /// Invokes the tool. Throw any exception to propagate it back
    /// through [ToolCall#execute] -- the calling code can decide
    /// whether to surface the error to the model as a tool result
    /// or to abort the chat.
    String invoke(String argumentsJson) throws Exception;
}
