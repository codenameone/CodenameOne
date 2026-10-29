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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.codename1.compat.testing.HeadlessImplementation
import com.codename1.compat.testing.MainThreadRule
import com.codename1.ui.Component
import com.codename1.ui.Container
import com.codename1.ui.Display
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

class LayoutTest {
    @get:Rule
    val mainThread = MainThreadRule()

    companion object {
        @BeforeClass
        @JvmStatic
        fun install() {
            HeadlessImplementation.install()
        }
    }

    @Test
    fun columnRowAndWeights() {
        val form = ComposeTesting.show {
            Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(50.dp).background(Color.Red).testTag("a"))
                Row(Modifier.fillMaxWidth().height(20.dp)) {
                    Box(Modifier.weight(1f).fillMaxSize().testTag("w1"))
                    Box(Modifier.width(30.dp).fillMaxSize().testTag("fixed"))
                    Box(Modifier.weight(2f).fillMaxSize().testTag("w2"))
                }
                Box(Modifier.fillMaxWidth().height(10.dp), contentAlignment = Alignment.Center) {
                    Spacer(Modifier.size(4.dp).testTag("centered"))
                }
            }
        }
        // Two pixels per dp under the headless implementation.
        val a = ComposeTesting.find(form, "a")
        assertEquals(20, a.absoluteX - form.contentPane.absoluteX)
        assertEquals(100, a.width)
        val w1 = ComposeTesting.find(form, "w1")
        val fixed = ComposeTesting.find(form, "fixed")
        val w2 = ComposeTesting.find(form, "w2")
        assertEquals(60, fixed.width)
        val total = form.contentPane.width - 40
        assertEquals(total - 60, w1.width + w2.width)
        org.junit.Assert.assertTrue(Math.abs(w1.width * 2 - w2.width) <= 1)
        assertEquals(40, w1.height)
        assertEquals(a.absoluteY + 100 + 8, w1.absoluteY)
        val c = ComposeTesting.find(form, "centered")
        assertEquals(8, c.width)
        assertEquals(w1.absoluteY + 40 + 8 + 6, c.absoluteY)
    }

    @Test
    fun clickRecomposes() {
        var count by mutableIntStateOf(0)
        val form = ComposeTesting.show {
            Column {
                BasicText("Count $count", Modifier.testTag("label"))
                Box(Modifier.size(40.dp).clickable { count++ }.testTag("button"))
            }
        }
        val label = ComposeTesting.find(form, "label") as com.codename1.compose.internal.TextNode
        assertEquals(listOf("Count 0"), label.cn1Lines())
        val button = ComposeTesting.find(form, "button") as com.codename1.compose.internal.UiNode
        button.cn1PerformClick()
        ComposeTesting.settle(form)
        assertEquals(listOf("Count 1"), label.cn1Lines())
        assertEquals(7 * HeadlessImplementation.CHAR_WIDTH, label.width)
    }

    @Test
    fun textWraps() {
        val form = ComposeTesting.show {
            Box(Modifier.width(50.dp)) {
                BasicText("aaa bbb ccc", Modifier.testTag("t"))
            }
        }
        val t = ComposeTesting.find(form, "t") as com.codename1.compose.internal.TextNode
        assertEquals(listOf("aaa", "bbb", "ccc"), t.cn1Lines())
        assertEquals(3 * HeadlessImplementation.FONT_HEIGHT, t.height)
    }
}

/// Shows compositions and waits for them to settle.
object ComposeTesting {
    fun show(content: @androidx.compose.runtime.Composable () -> Unit): ComposeForm {
        val f = ComposeForm(content = content)
        f.show()
        settle(f)
        return f
    }

    fun settle(f: ComposeForm) {
        for (i in 0 until 50) {
            MainThreadRule.drain()
            f.view.cn1Frame()
            f.revalidate()
            if (!f.view.hasPendingWork()) {
                MainThreadRule.drain()
                f.revalidate()
                return
            }
        }
        throw AssertionError("composition did not settle")
    }

    fun find(root: Container, tag: String): Component {
        val r = search(root, tag)
        assertNotNull("no node tagged $tag", r)
        return r!!
    }

    private fun search(c: Container, tag: String): Component? {
        for (i in 0 until c.componentCount) {
            val k = c.getComponentAt(i)
            if (tag == k.name) return k
            if (k is Container) search(k, tag)?.let { return it }
        }
        return null
    }
}
