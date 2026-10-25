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
package kmpfixture

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.sync.*

class Node(val name: String) {
    val children = ArrayList<Node>()
    var text: String = ""
    fun dump(sb: StringBuilder, indent: String) {
        sb.append(indent).append(name)
        if (text.isNotEmpty()) sb.append("(").append(text).append(")")
        sb.append('\n')
        for (c in children) c.dump(sb, "$indent  ")
    }
}

class NodeApplier(root: Node) : AbstractApplier<Node>(root) {
    override fun insertTopDown(index: Int, instance: Node) {}
    override fun insertBottomUp(index: Int, instance: Node) { current.children.add(index, instance) }
    override fun remove(index: Int, count: Int) { repeat(count) { current.children.removeAt(index) } }
    override fun move(from: Int, to: Int, count: Int) {
        val dest = if (from > to) to else to - count
        val moved = ArrayList(current.children.subList(from, from + count))
        repeat(count) { current.children.removeAt(from) }
        current.children.addAll(dest, moved)
    }
    override fun onClear() { root.children.clear() }
}

@Composable
fun Label(text: String) {
    ComposeNode<Node, NodeApplier>(factory = { Node("Label") }, update = { set(text) { this.text = it } })
}

@Composable
fun Column(content: @Composable () -> Unit) {
    ComposeNode<Node, NodeApplier>(factory = { Node("Column") }, update = {}, content = content)
}

@Composable
fun CounterApp(count: State<Int>, items: List<String>) {
    Column {
        Label("count=${count.value}")
        if (count.value % 2 == 0) Label("even")
        for (i in items) key(i) { Label(i) }
        val derived by remember { derivedStateOf { count.value * 10 } }
        Label("derived=$derived")
    }
}

fun check(name: String, v: Any?) = println("CHECK $name=$v")

fun runSpike() {
    val t0 = System.currentTimeMillis()
    runBlocking {
        // --- coroutines ---
        check("async", async { delay(5); 40 }.await() + 2)
        val f = flow { for (i in 1..5) { emit(i); delay(1) } }.map { it * it }.filter { it % 2 == 1 }.toList()
        check("flow", f)
        val ch = Channel<Int>(2)
        launch { for (i in 1..4) ch.send(i); ch.close() }
        var sum = 0; for (x in ch) sum += x
        check("channel", sum)
        val sf = MutableStateFlow(0)
        val seen = ArrayList<Int>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) { sf.collect { seen.add(it) } }
        sf.value = 1; yield(); sf.value = 2; yield(); job.cancel()
        check("stateflow", seen)
        check("withContextDefault", withContext(Dispatchers.Default) { (1..100).sum() })
        check("timeout", withTimeoutOrNull(10) { delay(1000); 1 })
        val sup = supervisorScope { val a = async { throw IllegalStateException("x") }; runCatching { a.await() }.exceptionOrNull()?.message }
        check("supervisor", sup)
        check("parallel", (1..200).map { n -> async(Dispatchers.Default) { n * n } }.awaitAll().sum())
        val mutex = Mutex()
        var guarded = 0
        coroutineScope { repeat(50) { launch(Dispatchers.Default) { repeat(100) { mutex.withLock { guarded++ } } } } }
        check("mutex", guarded)
        val shared = MutableSharedFlow<Int>(replay = 2)
        shared.emit(1); shared.emit(2); shared.emit(3)
        check("sharedReplay", shared.take(2).toList())
        check("combine", combine(flowOf(1, 2), flowOf("a")) { a, b -> "$b$a" }.toList())
        val cleanup = ArrayList<String>()
        val cj = launch { try { delay(10_000) } finally { cleanup.add("finally") } }
        yield(); cj.cancelAndJoin()
        check("cancelFinally", cleanup)
        val ex = runCatching { coroutineScope { launch { throw ArithmeticException("boom") }; delay(1000) } }.exceptionOrNull()
        check("childFailure", ex?.javaClass?.simpleName + ":" + ex?.message)

        // --- compose ---
        val clock = BroadcastFrameClock()
        val root = Node("Root")
        val recomposer = Recomposer(coroutineContext + clock)
        val rj = launch(clock) { recomposer.runRecomposeAndApplyChanges() }
        val count = mutableStateOf(0)
        val items = mutableStateListOf("a", "b", "c")
        val composition = Composition(NodeApplier(root), recomposer)
        val itemsSnapshot = mutableStateOf<List<String>>(items.toList())
        composition.setContent { CounterApp(count, itemsSnapshot.value) }
        fun dump(tag: String) { val sb = StringBuilder(); root.dump(sb, ""); print("TREE $tag\n$sb") }
        dump("initial")
        suspend fun frame() {
            Snapshot.sendApplyNotifications()
            yield()
            clock.sendFrame(System.nanoTime())
            yield(); yield()
        }
        count.value = 1; frame(); dump("count1")
        itemsSnapshot.value = listOf("c", "a", "d"); count.value = 2; frame(); dump("reorder")
        val tb = System.nanoTime()
        for (i in 3..1002) { count.value = i; frame() }
        val bench = (System.nanoTime() - tb) / 1000000
        dump("after1000")
        System.err.println("BENCH recompose1000Ms=$bench")
        composition.dispose(); recomposer.cancel(); rj.cancel()
    }
    System.err.println("BENCH totalMs=${System.currentTimeMillis() - t0}")
    println("DONE")
}
