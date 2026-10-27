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
package com.example.kmpbench

import com.codename1.compose.ComposeForm
import com.codename1.ui.CN
import keynote.App

/// Shows the application and prints the marker the harness times start-up
/// against, once the first frame with content has been painted.
fun start() {
    // run_bench.sh launches with SIMCTL_CHILD_KMPBENCH_PERF=1 for the runtime measurements.
    val perf = System.getenv("KMPBENCH_PERF") == "1"
    val form = ComposeForm { App(perf = perf) }
    form.show()
    CN.callSerially {
        println("KMPBENCH first-frame " + System.currentTimeMillis())
    }
}
