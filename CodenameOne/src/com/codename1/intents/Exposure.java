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
package com.codename1.intents;

/// Which consumers an intent is offered to.
///
/// The default is [#ASSISTANT] alone, and that default is deliberate. An
/// assistant invokes an intent because a person asked for it by name, with the
/// platform mediating confirmation for anything destructive. A language model
/// invokes one because it inferred that it should, and it can infer wrong.
/// Those are different trust levels, so widening from one to the other is an
/// explicit act rather than something an app gets by accident.
public enum Exposure {
    /// Offered to the platform: Siri and Spotlight on iOS, launcher shortcuts
    /// on Android, the Intents window in the simulator.
    ASSISTANT,

    /// Offered to language models through [Intents#asTools()], which projects
    /// the declaration down to a `com.codename1.ai.Tool`. Nothing is exposed
    /// until the application actually hands those tools to a model or an MCP
    /// host, so this marks an intent as *eligible*, not as published.
    MODEL
}
