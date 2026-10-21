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
package scene;

import com.codename1.generated.unity.UnityAppImpl;
import global.BallController;
import global.Spinner;
import com.codename1.unitycompat.unityengine.DrawCommand;
import com.codename1.unitycompat.unityengine.DrawList;
import com.codename1.unitycompat.unityengine.GameObject;
import com.codename1.unitycompat.unityengine.UnityRuntime;

/// Runs the sample scene with no display: builds it, steps two seconds of
/// frames, taps once, and prints what the scripts logged. The same class
/// runs on a JVM and through ParparVM, and the two traces must be equal.
///
/// It draws nothing either, but it does ask what would be drawn: the draw
/// list of three frames -- the first, one in mid fall, and the last, after
/// the crate has swapped its sprite -- is printed for a 320 by 480 view, in
/// hundredths of a pixel and of a degree so that every number is an integer.
public final class SceneMain {
    private SceneMain() {
    }

    public static void main(String[] args) {
        UnityRuntime.reset();
        UnityAppImpl.install();
        UnityRuntime.resize(320, 480);
        UnityRuntime.begin();
        float dt = 1f / 60f;
        for (int frame = 1; frame <= 150; frame++) {
            if (frame == 100) {
                UnityRuntime.pointerPressed(160f, 240f);
            }
            if (frame == 101) {
                UnityRuntime.pointerReleased(160f, 240f);
            }
            UnityRuntime.step(dt);
            if (frame == 1 || frame == 60 || frame == 150) {
                print(frame, UnityRuntime.render());
            }
        }
        BallController ball = (BallController) GameObject.Find("Ball").GetComponent(BallController.class);
        System.out.println(ball.Report());
        Spinner crate = (Spinner) GameObject.Find("Crate").GetComponent(Spinner.class);
        System.out.println(crate.Describe());
    }

    private static void print(int frame, DrawList list) {
        System.out.println("draw frame=" + frame + " sprites=" + list.size() + " background="
                + Integer.toHexString(list.backgroundColor));
        for (int i = 0; i < list.size(); i++) {
            DrawCommand d = list.get(i);
            System.out.println("  " + d.sprite + " order=" + d.sortingOrder + " at=" + c(d.x) + "," + c(d.y)
                    + " size=" + c(d.width) + "x" + c(d.height) + " anchor=" + c(d.anchorX) + "," + c(d.anchorY)
                    + " rotation=" + c(d.rotation) + " color=" + Integer.toHexString(d.color) + " flip="
                    + (d.flipX ? 1 : 0) + (d.flipY ? 1 : 0));
        }
    }

    /// Hundredths, rounded half up by hand: `Math.round` is not what is
    /// being tested here.
    private static int c(float value) {
        float scaled = value * 100f;
        return (int) (scaled < 0f ? scaled - 0.5f : scaled + 0.5f);
    }
}
