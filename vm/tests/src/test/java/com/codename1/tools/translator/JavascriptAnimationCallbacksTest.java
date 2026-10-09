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
package com.codename1.tools.translator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JavascriptAnimationCallbacksTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void overlappingAnimationFramesAreDeliveredInOrder(boolean nativeBinding) throws Exception {
        JavascriptNetworkBindingsTest.runNode(""
                + "const assert = require('assert'); const spawned = [], entered = [], browser = [];\n"
                + "jvm.spawn = (owner, generator) => spawned.push(generator);\n"
                + "jvm.resolveVirtual = () => function*(receiver, timestamp) { entered.push(timestamp); yield 'blocked'; };\n"
                + "jvm.instanceOf = (value, name) => name === 'com_codename1_html5_js_browser_AnimationFrameCallback';\n"
                + "global.requestAnimationFrame = callback => { browser.push(callback); return browser.length; };\n"
                + "const receiver = {__class: 'TestFrame'};\n"
                + "const callback = " + (nativeBinding
                    ? "time => { global.cn1_com_codename1_impl_html5_HTML5Implementation_requestAnimationFrameNative_com_codename1_impl_html5_JavaScriptAnimationFrameCallback_R_int(receiver); browser.shift()(time); }"
                    : "jvm.toNativeJsArg(receiver)") + ";\n"
                + "callback(1); assert.equal(spawned.length, 1); spawned[0].next();\n"
                + "callback(2); callback(3); assert.equal(spawned.length, 1, 'callbacks must stay serialized');\n"
                + "spawned[0].next(); assert.deepEqual(entered, [1, 2], 'the second one-shot frame must not be dropped');\n"
                + "spawned[0].next(); assert.deepEqual(entered, [1, 2, 3]);\n"
                + "assert.equal(spawned[0].next().done, true);\n"
                + "callback(4); assert.equal(spawned.length, 2); spawned[1].next(); spawned[1].next();\n"
                + "assert.deepEqual(entered, [1, 2, 3, 4]); assert.equal(receiver.__cn1RafCallbackPending, false);\n");
    }

    @Test
    void repeatingTimersStillCoalesceWhileTheirCallbackIsRunning() throws Exception {
        JavascriptNetworkBindingsTest.runNode(""
                + "const assert = require('assert'); const spawned = [], entered = [];\n"
                + "jvm.spawn = (owner, generator) => spawned.push(generator);\n"
                + "jvm.resolveVirtual = () => function*(receiver, value) { entered.push(value); yield 'blocked'; };\n"
                + "const receiver = {__class: 'TestTimer'};\n"
                + "spawnVirtualCallback(receiver, 'timer', [1], '__cn1TimerCallbackPending'); spawned[0].next();\n"
                + "spawnVirtualCallback(receiver, 'timer', [2], '__cn1TimerCallbackPending');\n"
                + "assert.equal(spawned.length, 1); assert.equal(spawned[0].next().done, true);\n"
                + "assert.deepEqual(entered, [1]); assert.equal(receiver.__cn1TimerCallbackPending, false);\n");
    }
}
