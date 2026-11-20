/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut;

import com.codename1.system.Lifecycle;
import com.codenameone.examples.papercut.play.Sfx;
import com.codenameone.examples.papercut.ui.Nav;

/// A twin-stick arena shooter on a sheet of notebook paper.
///
/// Where things are:
///
/// - `play` -- the game: [com.codenameone.examples.papercut.play.Arena] is the
///   whole simulation, with no UI in it, and
///   [com.codenameone.examples.papercut.play.ArenaView] draws it as cut paper
///   with `com.codename1.gaming` and reads the thumbs, keys and gamepad.
/// - `ui` -- every other screen, ordinary widgets that `src/main/css/theme.css`
///   dresses as paper on a desk.
public class Papercut extends Lifecycle {
    @Override
    public void runApp() {
        Sfx.load();
        Nav.launch();
    }
}
