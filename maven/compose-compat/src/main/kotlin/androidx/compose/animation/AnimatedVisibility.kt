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
package androidx.compose.animation

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha

/// How content appears. Every transition fades; sizes and slides fade too.
class EnterTransition internal constructor(internal val millis: Int) {
    operator fun plus(other: EnterTransition): EnterTransition = EnterTransition(Math.max(millis, other.millis))
}

class ExitTransition internal constructor(internal val millis: Int) {
    operator fun plus(other: ExitTransition): ExitTransition = ExitTransition(Math.max(millis, other.millis))
}

fun fadeIn(animationSpec: AnimationSpec<Float>? = null): EnterTransition = EnterTransition(220)
fun fadeOut(animationSpec: AnimationSpec<Float>? = null): ExitTransition = ExitTransition(220)
fun expandVertically(): EnterTransition = EnterTransition(220)
fun shrinkVertically(): ExitTransition = ExitTransition(220)
fun slideInVertically(initialOffsetY: (Int) -> Int = { -it / 2 }): EnterTransition = EnterTransition(220)
fun slideOutVertically(targetOffsetY: (Int) -> Int = { -it / 2 }): ExitTransition = ExitTransition(220)
fun scaleIn(): EnterTransition = EnterTransition(220)
fun scaleOut(): ExitTransition = ExitTransition(220)

@Composable
fun AnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandVertically(),
    exit: ExitTransition = fadeOut() + shrinkVertically(),
    label: String = "AnimatedVisibility",
    content: @Composable () -> Unit
) {
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(if (visible) enter.millis else exit.millis))
    if (visible || alpha > 0f) {
        Box(modifier.alpha(alpha)) {
            content()
        }
    }
}
