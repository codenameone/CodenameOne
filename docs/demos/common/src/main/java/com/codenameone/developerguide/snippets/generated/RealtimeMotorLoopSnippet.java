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
package com.codenameone.developerguide.snippets.generated;

import com.codename1.hardware.Hardware;
import com.codename1.hardware.HardwareException;
import com.codename1.hardware.PwmChannel;
import com.codename1.realtime.Realtime;
import com.codename1.realtime.RealtimeParameters;
import com.codename1.realtime.RealtimeTask;
import com.codename1.realtime.RealtimeThread;

class RealtimeMotorLoopSnippet {
    // tag::realtime-motor-loop[]
    class MotorLoop extends RealtimeTask {
        private final double[] samples = new double[64];   // <1>
        private final double[] gains = new double[64];
        private PwmChannel pwm;
        private boolean failed;

        public void setup() {                              // <2>
            try {
                pwm = Hardware.getInstance().openPwm(0, 0);
            } catch (HardwareException e) {
                failed = true;
            }
        }

        public void cycle() {                              // <3>
            if (failed) {
                return;
            }
            double out = 0;
            for (int i = 0; i < samples.length; i++) {
                out += samples[i] * gains[i];
            }
            try {
                pwm.setDutyCycle(out);
            } catch (HardwareException e) {
                failed = true;                             // no allocation on this path
            }
        }

        public void periodsMissed(int count) {             // <4>
            // the loop overran; hold the output, count it, alarm if it persists
        }
    }

    RealtimeThread startLoop() {
        return Realtime.getInstance().start("motor",
                new RealtimeParameters()
                    .setPeriodNanos(1000000)    // 1 kHz
                    .setPriority(80)            // SCHED_FIFO 1..99
                    .setCpu(3),                 // pin to core 3
                new MotorLoop());
    }
    // end::realtime-motor-loop[]
}
