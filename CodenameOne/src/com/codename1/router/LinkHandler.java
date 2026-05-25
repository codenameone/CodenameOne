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
package com.codename1.router;

/// Receives normalized deep-link URLs from the platform.
///
/// Install one with `com.codename1.ui.Display#setDeepLinkHandler(LinkHandler)`. The
/// handler is invoked on the EDT for both cold launches (the URL that started the
/// app) and warm launches (URLs delivered while the app is already running, e.g.
/// `application:openURL:` on iOS or `onNewIntent` on Android).
///
/// Implementations typically delegate to `Router.getInstance().handle(link)` and
/// return its result.
public interface LinkHandler {
    /// Handles a deep link.
    ///
    /// #### Parameters
    /// - `link`: the parsed deep link, never null.
    ///
    /// #### Returns
    /// `true` if the link was consumed; `false` to let the platform fall back to
    /// the legacy `AppArg` property mechanism.
    boolean handle(DeepLink link);
}
