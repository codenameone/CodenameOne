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
package androidx.compose.animation.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/// Maps a fraction of an animation's time to a fraction of its distance.
@Immutable
fun interface Easing {
    fun transform(fraction: Float): Float
}

/// A cubic bezier from (0, 0) to (1, 1) through two control points, as CSS
/// defines `cubic-bezier`.
@Immutable
class CubicBezierEasing(private val a: Float, private val b: Float, private val c: Float, private val d: Float) : Easing {
    private fun bezier(t: Float, p1: Float, p2: Float): Float {
        val u = 1 - t
        return 3 * u * u * t * p1 + 3 * u * t * t * p2 + t * t * t
    }

    override fun transform(fraction: Float): Float {
        if (fraction <= 0f) return 0f
        if (fraction >= 1f) return 1f
        var lo = 0f
        var hi = 1f
        var t = fraction
        for (i in 0 until 20) {
            val x = bezier(t, a, c)
            if (Math.abs(x - fraction) < 0.0005f) break
            if (x < fraction) lo = t else hi = t
            t = (lo + hi) / 2
        }
        return bezier(t, b, d)
    }
}

val FastOutSlowInEasing: Easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
val LinearOutSlowInEasing: Easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
val FastOutLinearInEasing: Easing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
val LinearEasing: Easing = Easing { it }
val EaseInOut: Easing = CubicBezierEasing(0.42f, 0.0f, 0.58f, 1.0f)

/// How a value moves to its target.
interface AnimationSpec<T>

/// Answers the value at a time for an animation of a float from `from` to `to`.
internal interface FloatSpec {
    /// The value at `playTimeNanos`, or null once the animation is over.
    fun valueAt(playTimeNanos: Long, from: Float, to: Float, velocity: FloatArray): Float?
}

@Immutable
class TweenSpec<T>(val durationMillis: Int = DefaultDurationMillis, val delay: Int = 0, val easing: Easing = FastOutSlowInEasing) :
    AnimationSpec<T>, FloatSpec {
    override fun valueAt(playTimeNanos: Long, from: Float, to: Float, velocity: FloatArray): Float? {
        val ms = playTimeNanos / 1_000_000L - delay
        if (ms < 0) return from
        if (ms >= durationMillis) return null
        val f = easing.transform(ms.toFloat() / Math.max(1, durationMillis))
        return from + (to - from) * f
    }
}

/// A damped spring. Integrated per frame from the velocity it carries.
@Immutable
class SpringSpec<T>(val dampingRatio: Float = Spring.DampingRatioNoBouncy, val stiffness: Float = Spring.StiffnessMedium) :
    AnimationSpec<T>, FloatSpec {
    override fun valueAt(playTimeNanos: Long, from: Float, to: Float, velocity: FloatArray): Float? {
        // velocity[0] = current value, velocity[1] = velocity, velocity[2] = last time.
        if (velocity[2] < 0f) {
            velocity[0] = from
            velocity[1] = 0f
            velocity[2] = 0f
        }
        val t = playTimeNanos / 1e9f
        var dt = t - velocity[2]
        velocity[2] = t
        val omega = Math.sqrt(stiffness.toDouble()).toFloat()
        var x = velocity[0]
        var v = velocity[1]
        while (dt > 0f) {
            val step = Math.min(dt, 1f / 240f)
            val a = -stiffness * (x - to) - 2f * dampingRatio * omega * v
            v += a * step
            x += v * step
            dt -= step
        }
        velocity[0] = x
        velocity[1] = v
        val range = Math.max(Math.abs(to - from), 1f)
        if (Math.abs(x - to) < range * 0.001f && Math.abs(v) < range * 0.01f) return null
        return x
    }
}

object Spring {
    const val DampingRatioHighBouncy: Float = 0.2f
    const val DampingRatioMediumBouncy: Float = 0.5f
    const val DampingRatioLowBouncy: Float = 0.75f
    const val DampingRatioNoBouncy: Float = 1f
    const val StiffnessHigh: Float = 10_000f
    const val StiffnessMedium: Float = 1500f
    const val StiffnessMediumLow: Float = 400f
    const val StiffnessLow: Float = 200f
    const val StiffnessVeryLow: Float = 50f
}

const val DefaultDurationMillis: Int = 300

fun <T> tween(durationMillis: Int = DefaultDurationMillis, delayMillis: Int = 0, easing: Easing = FastOutSlowInEasing): TweenSpec<T> =
    TweenSpec(durationMillis, delayMillis, easing)

fun <T> spring(dampingRatio: Float = Spring.DampingRatioNoBouncy, stiffness: Float = Spring.StiffnessMedium,
               visibilityThreshold: T? = null): SpringSpec<T> = SpringSpec(dampingRatio, stiffness)

enum class RepeatMode { Restart, Reverse }

@Immutable
class InfiniteRepeatableSpec<T>(val animation: TweenSpec<T>, val repeatMode: RepeatMode) : AnimationSpec<T>

fun <T> infiniteRepeatable(animation: TweenSpec<T>, repeatMode: RepeatMode = RepeatMode.Restart): InfiniteRepeatableSpec<T> =
    InfiniteRepeatableSpec(animation, repeatMode)

/// A float that animates to targets on the frame clock.
class Animatable(initialValue: Float) {
    private var current by mutableFloatStateOf(initialValue)
    private var running by mutableStateOf(false)
    var targetValue: Float = initialValue
        private set

    val value: Float get() = current
    val isRunning: Boolean get() = running

    suspend fun snapTo(targetValue: Float) {
        this.targetValue = targetValue
        current = targetValue
    }

    suspend fun animateTo(targetValue: Float, animationSpec: AnimationSpec<Float> = spring()) {
        this.targetValue = targetValue
        val spec = (animationSpec as? FloatSpec) ?: SpringSpec<Float>()
        val from = current
        if (from == targetValue) return
        val state = floatArrayOf(0f, 0f, -1f)
        running = true
        try {
            var start = -1L
            while (true) {
                val done = withFrameNanos { now ->
                    if (start < 0) start = now
                    val v = spec.valueAt(now - start, from, targetValue, state)
                    if (v == null) {
                        current = targetValue
                        true
                    } else {
                        current = v
                        false
                    }
                }
                if (done) break
            }
        } finally {
            running = false
        }
    }

    fun asState(): State<Float> = object : State<Float> {
        override val value: Float get() = current
    }
}

@Composable
fun animateFloatAsState(targetValue: Float, animationSpec: AnimationSpec<Float> = spring(), label: String = "FloatAnimation",
                        finishedListener: ((Float) -> Unit)? = null): State<Float> {
    val anim = remember { Animatable(targetValue) }
    LaunchedEffect(targetValue) {
        anim.animateTo(targetValue, animationSpec)
        finishedListener?.invoke(targetValue)
    }
    return anim.asState()
}

@Composable
fun animateDpAsState(targetValue: Dp, animationSpec: AnimationSpec<Dp> = spring(), label: String = "DpAnimation"): State<Dp> {
    @Suppress("UNCHECKED_CAST")
    val f = animateFloatAsState(targetValue.value, animationSpec as AnimationSpec<Float>, label)
    return remember(f) { object : State<Dp> { override val value: Dp get() = Dp(f.value) } }
}

/// Runs animations that never end: the values a [rememberInfiniteTransition]
/// produces.
class InfiniteTransition internal constructor() {
    internal var startNanos by mutableStateOf(-1L)
    internal var nowNanos by mutableStateOf(0L)
}

@Composable
fun rememberInfiniteTransition(label: String = "InfiniteTransition"): InfiniteTransition {
    val t = remember { InfiniteTransition() }
    LaunchedEffect(t) {
        while (true) {
            withFrameNanos { now ->
                if (t.startNanos < 0) t.startNanos = now
                t.nowNanos = now - t.startNanos
            }
        }
    }
    return t
}

@Composable
fun InfiniteTransition.animateFloat(initialValue: Float, targetValue: Float, animationSpec: InfiniteRepeatableSpec<Float>,
                                    label: String = "FloatAnimation"): State<Float> {
    val t = this
    return remember(initialValue, targetValue, animationSpec) {
        object : State<Float> {
            override val value: Float
                get() {
                    val spec = animationSpec.animation
                    val period = (spec.durationMillis + spec.delay).toLong() * 1_000_000L
                    if (period <= 0) return targetValue
                    val cycle = t.nowNanos / period
                    val inCycle = t.nowNanos % period
                    val reverse = animationSpec.repeatMode == RepeatMode.Reverse && cycle % 2 == 1L
                    val v = spec.valueAt(inCycle, initialValue, targetValue, floatArrayOf(0f, 0f, -1f)) ?: targetValue
                    return if (reverse) initialValue + targetValue - v else v
                }
        }
    }
}

@Composable
fun animateColorAsState(targetValue: Color, animationSpec: AnimationSpec<Float> = spring(), label: String = "ColorAnimation"): State<Color> {
    val from = remember { mutableStateOf(targetValue) }
    val to = remember { mutableStateOf(targetValue) }
    if (to.value != targetValue) {
        from.value = to.value
        to.value = targetValue
    }
    val anim = remember { Animatable(1f) }
    LaunchedEffect(targetValue) {
        anim.snapTo(0f)
        anim.animateTo(1f, animationSpec)
    }
    return remember {
        object : State<Color> {
            override val value: Color get() = androidx.compose.ui.graphics.lerp(from.value, to.value, anim.value)
        }
    }
}
