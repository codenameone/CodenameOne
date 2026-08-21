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

/// Receives the tap on a piece of content this application published to device
/// search with [Intents#index].
///
/// Register one handler, ideally from your `init()`, so a tap that cold-started
/// the app is delivered as soon as there is somewhere to deliver it to --
/// registration drains anything that arrived first.
///
/// The entity handed back carries the type and id you indexed under, and nothing
/// else: the platform stores identity, not your object. Load the rest yourself,
/// which is also the moment to notice that the content may have been deleted
/// since it was indexed.
public interface EntitySelectionHandler {
    /// Invoked on the event dispatch thread when the user opened an indexed item.
    ///
    /// #### Parameters
    ///
    /// - `entity`: the type and id that were indexed
    void onEntitySelected(AppEntity entity);
}
