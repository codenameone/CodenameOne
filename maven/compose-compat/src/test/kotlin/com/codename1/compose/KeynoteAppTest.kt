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

import com.codename1.compat.testing.HeadlessImplementation
import com.codename1.compat.testing.MainThreadRule
import androidx.compose.foundation.lazy.LazyListNode
import com.codename1.compose.internal.TextNode
import com.codename1.compose.internal.TextInputNode
import com.codename1.compose.internal.UiNode
import com.codename1.ui.Component
import com.codename1.ui.Container
import com.codename1.ui.Display
import keynote.App
import keynote.AppState
import keynote.Programme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

/// Drives the benchmark's hero application through every screen, headlessly.
class KeynoteAppTest {
    @get:Rule
    val mainThread = MainThreadRule()

    companion object {
        @BeforeClass
        @JvmStatic
        fun install() {
            HeadlessImplementation.install()
        }
    }

    private fun waitUntil(form: ComposeForm, what: String, cond: () -> Boolean) {
        val end = System.currentTimeMillis() + 5000
        while (System.currentTimeMillis() < end) {
            ComposeTesting.settle(form)
            if (cond()) return
            Display.getInstance().invokeAndBlock { Thread.sleep(10) }
        }
        throw AssertionError("timed out waiting for $what")
    }

    private fun cards(root: Container): Int = count(root) { it.name?.startsWith("session-") == true }

    /// How many items the session list holds; only the ones in view are composed.
    private fun items(form: ComposeForm): Int = (ComposeTesting.find(form, "list") as LazyListNode).cn1ItemCount()

    private fun count(c: Container, p: (Component) -> Boolean): Int {
        var n = 0
        for (i in 0 until c.componentCount) {
            val k = c.getComponentAt(i)
            if (p(k)) n++
            if (k is Container) n += count(k, p)
        }
        return n
    }

    private fun texts(c: Container, out: MutableList<String> = ArrayList()): List<String> {
        for (i in 0 until c.componentCount) {
            val k = c.getComponentAt(i)
            if (k is TextNode) out.addAll(k.cn1Lines())
            if (k is Container) texts(k, out)
        }
        return out
    }

    private fun click(form: ComposeForm, tag: String) {
        val node = ComposeTesting.find(form, tag) as UiNode
        assertTrue("$tag is not clickable", node.cn1PerformClick())
        ComposeTesting.settle(form)
    }

    @Test
    fun everyScreen() {
        val state = AppState()
        val form = ComposeTesting.show { App(state) }
        waitUntil(form, "the programme to load") { cards(form) > 0 }
        val day1 = Programme.sessions.filter { it.day == 1 }
        assertEquals(day1.size, items(form))
        // Virtualized: the cards in view are composed, not the whole day.
        val list = ComposeTesting.find(form, "list") as LazyListNode
        assertTrue("composed ${cards(form)} of ${day1.size}", cards(form) < day1.size)
        assertEquals(cards(form), list.cn1ComposedCount())
        // Scrolling to the end composes the last card and disposes of the first.
        list.cn1ScrollY(Int.MAX_VALUE)
        ComposeTesting.settle(form)
        list.cn1ScrollY(Int.MAX_VALUE)
        ComposeTesting.settle(form)
        assertEquals(1, count(form) { it.name == "session-${day1.last().id}" })
        assertEquals(0, count(form) { it.name == "session-${day1.first().id}" })
        list.cn1ScrollY(0)
        ComposeTesting.settle(form)

        // Favorite a session; it shows on the favorites tab.
        click(form, "fav-0")
        assertEquals(listOf(0), state.favorites.toList())
        click(form, "tab-favorites")
        assertEquals(1, items(form))
        assertEquals(1, cards(form))
        click(form, "tab-schedule")

        // Day chips and search filter the list.
        click(form, "day-2")
        assertEquals(Programme.sessions.count { it.day == 2 }, items(form))
        val input = ComposeTesting.find(form, "search")
        val field = findInput(input as Container)!!
        field.field.text = "Compose"
        ComposeTesting.settle(form)
        assertEquals("Compose", state.query)
        val expected = Programme.sessions.count { it.day == 2 && (it.title.contains("Compose", true) ||
            it.speaker.name.contains("Compose", true)) }
        assertEquals(expected, items(form))

        // Open a session and come back.
        val first = Programme.sessions.first { it.day == 2 && it.title.contains("Compose", true) }
        click(form, "session-${first.id}")
        assertTrue(texts(form.contentPane).joinToString(" ").contains(first.speaker.name))
        click(form, "back")
        assertNull(state.selected)

        // Insights: the chart animates in.
        click(form, "tab-insights")
        waitUntil(form, "the chart") { count(form) { it.name == "chart" } == 1 }
        assertTrue(texts(form.contentPane).contains("You have 1 favorite sessions."))

        // Paint every screen state once, to catch a painting failure.
        val img = com.codename1.ui.Image.createImage(form.width, form.height)
        form.paintComponent(img.graphics, true)
    }

    private fun findInput(c: Container): TextInputNode? {
        for (i in 0 until c.componentCount) {
            val k = c.getComponentAt(i)
            if (k is TextInputNode) return k
            if (k is Container) findInput(k)?.let { return it }
        }
        return null
    }
}
