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
package keynote

import androidx.compose.runtime.withFrameNanos
import kotlin.time.TimeSource

/// CPU-bound measurements run inside the app, identical on both sides. Each
/// result is printed as "KMPBENCH <name> <microseconds>" for run_bench.sh.
///
/// Time is taken from a state change to the moment a 1 dp marker after the list
/// is drawn (`AppState.drawnAt`), so it covers recomposition, layout and drawing
/// of that frame. The simulator paces frames to its display, so frame interval
/// alone mostly shows vsync; the change-to-drawn time is the work itself.
private val origin = TimeSource.Monotonic.markNow()
internal fun nowNanos(): Long = origin.elapsedNow().inWholeNanoseconds

private suspend fun awaitDrawn(state: AppState, seq: Int) {
    var guard = 0
    while (state.drawnSeq < seq) {
        if (guard++ == 60) { println("KMPBENCH perf-stall " + seq); throw IllegalStateException("marker not drawn") }
        withFrameNanos { }
    }
}

private fun pct(v: List<Long>, p: Int): Long {
    val s = v.sorted()
    return s[minOf(s.size - 1, (s.size * p) / 100)]
}

private fun report(name: String, v: Long) = println("KMPBENCH $name $v")

private suspend fun recompose(state: AppState, n: Int): List<Long> {
    val out = ArrayList<Long>()
    for (i in 0 until n) {
        withFrameNanos { }
        val t0 = nowNanos()
        state.day = state.day % 3 + 1
        val seq = ++state.tick
        awaitDrawn(state, seq)
        out.add((state.drawnAt - t0) / 1000)
    }
    return out
}

/// Scrolls the list of the current day one card per frame, to the end.
private suspend fun scrollDay(state: AppState, work: MutableList<Long>, frames: MutableList<Long>) {
    val list = state.list ?: return
    list.scrollToItem(0)
    withFrameNanos { }
    val count = 50 // sessions per day, Data.kt
    var last = 0L
    for (i in 1 until count) {
        withFrameNanos { }
        val t0 = nowNanos()
        list.scrollToItem(i)
        val seq = ++state.tick
        awaitDrawn(state, seq)
        work.add((state.drawnAt - t0) / 1000)
        if (last != 0L) frames.add((state.drawnAt - last) / 1000)
        last = state.drawnAt
    }
}

suspend fun runPerf(state: AppState) {
    println("KMPBENCH perf-start 0")
    repeat(10) { withFrameNanos { } }
    recompose(state, 30) // warm-up, discarded
    val r = recompose(state, 300)
    report("recompose-median-us", pct(r, 50)); report("recompose-p95-us", pct(r, 95))
    state.day = 1; withFrameNanos { }; withFrameNanos { }
    scrollDay(state, ArrayList(), ArrayList()) // warm-up, discarded
    val work = ArrayList<Long>(); val frames = ArrayList<Long>()
    for (d in 1..3) {
        state.day = d; withFrameNanos { }; withFrameNanos { }
        scrollDay(state, work, frames)
    }
    val fm = pct(frames, 50)
    report("scroll-frames", frames.size.toLong())
    report("scroll-frame-median-us", fm); report("scroll-frame-p95-us", pct(frames, 95))
    report("scroll-jank-frames", frames.count { it > 2 * fm }.toLong())
    report("scroll-work-median-us", pct(work, 50)); report("scroll-work-p95-us", pct(work, 95))
    report("perf-done", 0)
}
