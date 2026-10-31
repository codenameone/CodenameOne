/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.realtime;

/// The work a [RealtimeThread] runs every period.
///
/// ```java
/// class MotorLoop extends RealtimeTask {
///     private final double[] samples = new double[64];
///     private final double[] gains = new double[64];
///     private PwmChannel pwm;
///     private boolean failed;
///
///     public void setup() {
///         // runs once on the real-time thread; may allocate
///         try {
///             pwm = Hardware.getInstance().openPwm(0, 0);
///         } catch (HardwareException e) {
///             failed = true;
///         }
///     }
///
///     public void cycle() {
///         // every period; must not allocate
///         if (failed) {
///             return;
///         }
///         double out = 0;
///         for (int i = 0; i < samples.length; i++) {
///             out += samples[i] * gains[i];
///         }
///         try {
///             pwm.setDutyCycle(out);
///         } catch (HardwareException e) {
///             failed = true;
///         }
///     }
/// }
/// ```
///
/// `cycle()` and `periodsMissed(int)` must not allocate: no `new`, no boxing, no
/// string concatenation, no iterator from a for-each over a collection. Keep buffers
/// and objects in fields, made in the constructor or in `setup()`. The build checks
/// every method they reach through calls it can resolve and fails with the call chain;
/// at run time an allocation the build could not see ends the steady state with a
/// [RealtimeAllocationError], unless the thread has an allocation budget.
@com.codename1.impl.SharedWithBackend
public abstract class RealtimeTask {

    /// Runs once on the real-time thread, after its priority is applied and before the
    /// first period. It may allocate: create here what the cycles will use, and touch
    /// every class they use so its static initializer has run.
    public void setup() {
    }

    /// One period's work. Runs on the real-time thread at every release and must not
    /// allocate.
    public abstract void cycle();

    /// Called before `cycle()` when releases were missed because the previous cycle
    /// ran past them. Missed releases are skipped, not run late: the cycle that follows
    /// is the one due now. Must not allocate. The default does nothing.
    ///
    /// #### Parameters
    ///
    /// - `count`: how many releases were skipped
    public void periodsMissed(int count) {
    }

    /// Called on the real-time thread when the task ends abnormally: `setup()` or
    /// `cycle()` threw, or the thread could not be made real-time as its parameters
    /// require. The steady state is over when this runs, so it may allocate. The
    /// thread stops afterwards. The default prints the stack trace.
    ///
    /// #### Parameters
    ///
    /// - `failure`: what ended the task
    public void failed(Throwable failure) {
        failure.printStackTrace();
    }

    /// Called on the real-time thread after the last cycle, when [RealtimeThread#stop()]
    /// was called or after `failed(Throwable)`. May allocate. The default does nothing.
    public void stopped() {
    }
}
