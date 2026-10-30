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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListNode
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.codename1.compat.testing.HeadlessImplementation
import com.codename1.compat.testing.MainThreadRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

class LazyListTest {
    @get:Rule
    val mainThread = MainThreadRule()

    companion object {
        @BeforeClass
        @JvmStatic
        fun install() {
            HeadlessImplementation.install()
        }

        var itemRuns = 0
    }

    private fun list(form: ComposeForm): LazyListNode = ComposeTesting.find(form, "list") as LazyListNode

    @Test
    fun parentRecompositionDoesNotRecomposeItems() {
        var count by mutableIntStateOf(0)
        itemRuns = 0
        val form = ComposeTesting.show {
            Column {
                BasicText("Count $count", Modifier.testTag("label"))
                LazyColumn(Modifier.height(300.dp).testTag("list")) {
                    items(100, key = { it }) {
                        itemRuns++
                        BasicText("item $it", Modifier.height(40.dp))
                    }
                }
            }
        }
        val composed = list(form).cn1ComposedCount()
        assertTrue("only the items in view are composed", composed in 1 until 100)
        assertEquals(composed, itemRuns)
        count++
        ComposeTesting.settle(form)
        val label = ComposeTesting.find(form, "label") as com.codename1.compose.internal.TextNode
        assertEquals(listOf("Count 1"), label.cn1Lines())
        assertEquals("a recomposition above the list must not recompose its items", composed, itemRuns)
    }

    @Test
    fun duplicateKeysAreRefused() {
        try {
            ComposeTesting.show {
                LazyColumn(Modifier.height(300.dp).testTag("list")) {
                    items(5, key = { "same" }) { BasicText("item $it", Modifier.height(40.dp)) }
                }
            }
            fail("two items with one key must be refused")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("same"))
        }
    }

    @Test
    fun scrollToItemLandsOnTheMeasuredItem() {
        var state: LazyListState? = null
        val form = ComposeTesting.show {
            val s = rememberLazyListState()
            state = s
            // Sizes nothing like the estimate taken from the first items, so the target's
            // position is only right once its neighbours have been measured.
            LazyColumn(Modifier.height(300.dp).testTag("list"), state = s) {
                items(200, key = { it }) {
                    BasicText("item $it", Modifier.fillMaxWidth().height(if (it < 10) 20.dp else (30 + (it % 7) * 15).dp)
                        .testTag("item-$it"))
                }
            }
        }
        val node = list(form)
        for (target in intArrayOf(150, 60, 199, 3)) {
            runBlocking { state!!.scrollToItem(target) }
            ComposeTesting.settle(form)
            val item = ComposeTesting.find(form, "item-$target")
            // A container's absolute Y has its own scroll taken off; the view's top is this.
            val top = node.absoluteY + node.scrollY
            val want = if (target == 199) top + node.height - item.height else top
            assertEquals("item $target starts where scrollToItem put it", want, item.absoluteY)
            if (target != 199) assertEquals(target, state!!.firstVisibleItemIndex)
        }
    }

    @Test
    fun aLongListCostsTheWindow() {
        var state: LazyListState? = null
        val form = ComposeTesting.show {
            val s = rememberLazyListState()
            state = s
            LazyColumn(Modifier.height(300.dp).testTag("list"), state = s) {
                items(100000, key = { it }) {
                    BasicText("item $it", Modifier.height((20 + (it % 5) * 10).dp).testTag("item-$it"))
                }
            }
        }
        val node = list(form)
        var target = 0
        while (target < 100000) {
            runBlocking { state!!.scrollToItem(target) }
            ComposeTesting.settle(form)
            assertTrue("composed ${node.cn1ComposedCount()}", node.cn1ComposedCount() < 40)
            val item = ComposeTesting.find(form, "item-$target")
            if (target < 99990) assertEquals(node.absoluteY + node.scrollY, item.absoluteY)
            target += 9973
        }
    }

    @Test
    fun userScrollCanBeTurnedOff() {
        val form = ComposeTesting.show {
            LazyColumn(Modifier.height(300.dp).testTag("list"), userScrollEnabled = false) {
                items(100, key = { it }) { BasicText("item $it", Modifier.height(40.dp)) }
            }
        }
        assertFalse(list(form).isScrollableY)
    }
}
