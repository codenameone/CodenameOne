/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.compose

import androidx.compose.runtime.Composable
import com.codename1.compose.internal.ComposeHost
import com.codename1.ui.Container
import com.codename1.ui.Form
import com.codename1.ui.layouts.BorderLayout

/// Embeds Compose content in a Codename One user interface. The content's nodes
/// are real Codename One components, so a composition can sit beside any other
/// component, and recomposes on the EDT.
///
/// ```kotlin
/// val view = ComposeView { App() }
/// form.add(BorderLayout.CENTER, view)
/// ```
class ComposeView(content: @Composable () -> Unit) : Container(BorderLayout()) {
    private val host = ComposeHost()

    init {
        allStyles.setPadding(0, 0, 0, 0)
        allStyles.setMargin(0, 0, 0, 0)
        add(BorderLayout.CENTER, host)
        host.setContent(content)
    }

    /// Replaces the content.
    fun setContent(content: @Composable () -> Unit) {
        host.setContent(content)
    }

    /// Ends the composition: its effects are cancelled and its nodes removed.
    fun dispose() {
        host.dispose()
    }

    /// Whether a recomposition or an animation frame is still pending; tests wait
    /// for this to become false.
    fun hasPendingWork(): Boolean = host.cn1HasPendingWork()

    /// Delivers an animation frame now, for tests that do not wait for the EDT's
    /// animation loop.
    fun cn1Frame() {
        host.sendFrame()
    }
}

/// A form whose whole content is a composition: the usual way to start a
/// Compose application on Codename One.
///
/// ```kotlin
/// override fun runApp() {
///     ComposeForm { App() }.show()
/// }
/// ```
class ComposeForm(title: String = "", content: @Composable () -> Unit) : Form(title, BorderLayout()) {
    val view: ComposeView = ComposeView(content)

    init {
        contentPane.allStyles.setPadding(0, 0, 0, 0)
        if (title.isEmpty()) {
            toolbar?.isHidden = true
            titleArea.isVisible = false
        }
        add(BorderLayout.CENTER, view)
    }
}
