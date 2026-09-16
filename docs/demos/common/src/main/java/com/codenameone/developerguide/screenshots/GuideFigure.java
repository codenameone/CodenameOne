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

package com.codenameone.developerguide.screenshots;

import com.codename1.ui.Display;
import com.codename1.ui.Form;

/// One picture in the developer guide, expressed as the code that produces it.
///
/// A figure builds a [Form] and returns it. It may show the form -- several
/// figures carry the chapter's own sample verbatim, and those samples end in
/// `show()` because that is what a reader would write -- but it must not depend
/// on having shown it. The renderer owns the window size, the theme and the
/// appearance, and prepares the returned form at the target geometry either
/// way, so one figure renders on several devices and in both appearances.
public interface GuideFigure {
    /// Stable identity used to name the output file. Lower case with dashes,
    /// matching the image naming already used under `docs/developer-guide/img`.
    String id();

    /// Builds the form to photograph. Called on the Codename One EDT with the
    /// theme and appearance already installed.
    Form build();

    /// Puts the form into the state worth photographing, after it has been
    /// shown and laid out. Does nothing by default.
    ///
    /// Anything a component only accepts once it is initialised belongs here
    /// rather than in [#build]: a Tree ignores expandPath until it has been
    /// added and shown, so a figure that expanded it while building came out
    /// as a single collapsed root. The same is true of anything that has to
    /// measure itself first -- selecting a row, scrolling to a component,
    /// opening a side menu.
    ///
    /// The renderer lays the form out again afterwards, so a change made here
    /// is reflected in the picture.
    ///
    /// - `form`: the form [#build] returned, now shown and laid out
    default void afterShow(Form form) {
    }

    /// Whether this figure's subject fills the screen it was given.
    ///
    /// The crop normally stops below the content, so a figure is the part of the
    /// screen with something on it rather than a phone-shaped picture that is
    /// mostly empty. That reads a component's preferred height, and a few
    /// components fill whatever they are given while declaring a small one --
    /// an ImageViewer asks for the size of its image, a CodeEditor for the size
    /// of its text -- so the honest answer for those is the whole viewport, and
    /// the measured one was a 70 pixel sliver.
    default boolean fillsViewport() {
        return false;
    }

    /// Runs the event thread for a while so animations finish.
    ///
    /// Several things a figure wants to show are only reachable through an
    /// animation, and some of them offer no way to skip it: ImageViewer.setZoom
    /// takes a flag, SwipeableContainer.openToRight does not -- it starts a
    /// 300ms Motion and returns, so asking and photographing immediately gives
    /// the closed row back.
    ///
    /// invokeAndBlock is what makes this work: it runs the event thread while
    /// the block below sleeps on another one, which is the only way to let time
    /// pass on the event thread from the event thread. A Motion reads the real
    /// clock, so the wait has to be real too.
    static void settleAnimations(int millis) {
        long end = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < end) {
            Display.getInstance().invokeAndBlock(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(16);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
        }
    }
}
