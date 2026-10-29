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
package com.codename1.compose.internal

import androidx.compose.runtime.snapshots.Snapshot
import com.codename1.ui.CN
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Delay
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.internal.MainDispatcherFactory
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

/// `Dispatchers.Main`: Codename One's event dispatch thread.
@OptIn(InternalCoroutinesApi::class)
internal open class EdtDispatcher(private val eager: Boolean) : MainCoroutineDispatcher(), Delay {
    override val immediate: MainCoroutineDispatcher get() = if (eager) this else Immediate

    override fun isDispatchNeeded(context: CoroutineContext): Boolean = !eager || !isEdt()

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        CN.callSerially(block)
    }

    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        val t = CN.setTimeout(Math.max(0L, Math.min(timeMillis, Int.MAX_VALUE.toLong())).toInt()) {
            continuation.resumeWith(Result.success(Unit))
        }
        continuation.invokeOnCancellation { t.cancel() }
    }

    override fun toString(): String = if (eager) "Dispatchers.Main.immediate" else "Dispatchers.Main"

    companion object {
        val Main: EdtDispatcher = EdtDispatcher(false)
        val Immediate: EdtDispatcher = EdtDispatcher(true)

        fun isEdt(): Boolean = try {
            CN.isEdt()
        } catch (t: Throwable) {
            false
        }
    }
}

/// Found by kotlinx.coroutines through `META-INF/services`, which the translator
/// compiles into a registry; on the JVM the real service loader reads the file.
@OptIn(InternalCoroutinesApi::class)
internal class EdtDispatcherFactory : MainDispatcherFactory {
    override val loadPriority: Int get() = Int.MAX_VALUE

    override fun createDispatcher(allFactories: List<MainDispatcherFactory>): MainCoroutineDispatcher = EdtDispatcher.Main

    override fun hintOnError(): String = "Codename One's main dispatcher"
}

/// Sends snapshot apply notifications on the EDT, so that a state written from
/// any thread schedules a recomposition.
internal object GlobalSnapshotManager {
    private val started = AtomicBoolean(false)
    private val pending = AtomicBoolean(false)

    fun ensureStarted() {
        if (started.compareAndSet(false, true)) {
            Snapshot.registerGlobalWriteObserver {
                if (pending.compareAndSet(false, true)) {
                    CN.callSerially {
                        pending.set(false)
                        Snapshot.sendApplyNotifications()
                    }
                }
            }
        }
    }
}
