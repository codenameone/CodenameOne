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
package com.codename1.continuity;

import java.util.Map;

/// Supplies and consumes the half of the application state the framework cannot work out for
/// itself.
///
/// The framework already knows the route stack. What it cannot know is the scroll position, the
/// half-typed message, the selected tab, the id of the record being edited -- so this is where
/// those go.
///
/// Both methods run on the event dispatch thread. `saveState` is called whenever the framework
/// takes a checkpoint, which can be often, so it should read fields rather than compute; anything
/// expensive belongs in a field the app updates as the user works.
public interface StateProvider {
    /// The application's share of the state. May return null or an empty map when there is
    /// nothing to add, in which case only the routes are carried.
    ///
    /// The returned map is restricted to `String`, `Integer`, `Long`, `Double`, `Boolean`, and
    /// `List` and `Map` of those -- see `AppState` for why. Returning anything else fails the
    /// checkpoint with a message naming the key.
    ///
    /// #### Returns
    ///
    /// the payload, or null
    Map<String, Object> saveState();

    /// Applies a payload this provider previously produced, on this device or another one.
    ///
    /// Called before the restored screens are shown, so a form built by the route table can read
    /// what was put here during its own construction. When the app has no routes, this is the
    /// whole of restoration and the provider is responsible for showing a form.
    ///
    /// #### Parameters
    ///
    /// - `payload`: the payload, never null and possibly empty
    void restoreState(Map<String, Object> payload);
}
