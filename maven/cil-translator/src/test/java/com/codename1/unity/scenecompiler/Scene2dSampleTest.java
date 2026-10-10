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
package com.codename1.unity.scenecompiler;

import com.codename1.cil.UnityToolchain;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/// The 2D sample scene end to end: its scripts compiled against the reference
/// `UnityEngine`, translated together with the value types, its `.unity` file
/// compiled to Java, and the result stepped for 150 frames with no display by
/// `scene.SceneMain`.
///
/// The trace is compared whole with `expected-trace.txt`, and the lines that
/// each prove one piece of the runtime works are also asserted by name, so a
/// regenerated trace that lost one of them does not pass quietly.
public class Scene2dSampleTest {
    @Test
    public void sceneRunsAndTracesWhatItShould() throws Exception {
        UnityToolchain.dotnet();
        File work = UnityToolchain.workDir("scene2d");
        File sample = new File(UnityToolchain.samples(), "scene2d");
        // Builds the reference and value assemblies too, through its project references.
        UnityToolchain.dotnetBuild(new File(sample, "Scene2d.csproj"), work);
        File csharp = new File(UnityToolchain.runtimeModule(), "src/main/csharp");
        File valuesBin = new File(csharp, "UnityEngine.Values/bin/Release/netstandard2.1");
        String netstandard = new File(valuesBin, "netstandard-ref/netstandard.dll").getAbsolutePath();
        String values = new File(valuesBin, "Codename1.UnityValues.dll").getAbsolutePath();
        String engine = new File(csharp, "UnityEngine/bin/Release/netstandard2.1/UnityEngine.dll").getAbsolutePath();
        String scripts = new File(sample, "bin/Release/netstandard2.1/Assembly-CSharp.dll").getAbsolutePath();
        File javaSources = new File(UnityToolchain.runtimeModule(), "src/main/java/com/codename1/unitycompat");
        File core = UnityToolchain.core();
        String translator = "com.codename1.cil.translate.Translator";

        // The engine classes are written against the value types and the value
        // types are translated against the base library, hence the order.
        File runtime = new File(work, "runtime");
        UnityToolchain.javac(work, "javac-base-library", runtime, null, new File(javaSources, "system"));
        File valueClasses = new File(work, "values");
        UnityToolchain.tool(work, "translate-values", translator, Arrays.asList("--out",
                valueClasses.getAbsolutePath(), "--runtime", runtime.getAbsolutePath(), "--ref", netstandard, values));
        UnityToolchain.javac(work, "javac-engine", runtime, UnityToolchain.path(runtime, valueClasses, core),
                new File(javaSources, "unityengine"),
                new File(javaSources, "tmpro"), new File(javaSources, "cinemachine"));

        File app = new File(work, "app");
        UnityToolchain.tool(work, "translate-scripts", translator, Arrays.asList("--out", app.getAbsolutePath(),
                "--runtime", runtime.getAbsolutePath(), "--ref", engine, "--ref", netstandard, values, scripts));

        File generated = new File(work, "generated");
        File resources = new File(work, "resources");
        UnityToolchain.tool(work, "scene-compiler", "com.codename1.unity.scenecompiler.SceneCompiler", Arrays.asList(
                "--project", sample.getAbsolutePath(), "--out", generated.getAbsolutePath(),
                "--resources", resources.getAbsolutePath(), "--ref", engine, "--ref", netstandard, scripts, values));
        Assert.assertTrue("the scene compiler wrote no factory",
                new File(generated, "com/codename1/generated/unity/UnityAppImpl.java").isFile());
        Assert.assertTrue("the scene compiler copied no ball.png", new File(resources, "ball.png").isFile());

        File main = new File(work, "main");
        UnityToolchain.javac(work, "javac-scene", main, UnityToolchain.path(runtime, app, core), generated,
                new File(sample, "java"));

        UnityToolchain.Result run = UnityToolchain.java(work, "run", UnityToolchain.path(main, app, runtime, core),
                "scene.SceneMain");
        run.assertOk("the scene");
        Assert.assertEquals("the scene wrote to stderr", "", run.err);
        List<String> trace = UnityToolchain.lines(run.out);

        // Awake and Start ran, with the serialized fields the scene file sets.
        assertHas(trace, "ball awake body=True mood=1 verbose=True");
        assertHas(trace, "ball start at 0,400 ground at -50 marker=Marker nudge=25,0");
        // The body fell: it started at y=400 and is lower half a second later.
        int fallen = Integer.parseInt(after(trace, "ball half a second in, frame 30 y="));
        Assert.assertTrue("the ball did not fall: y=" + fallen, fallen < 400);
        // A coroutine resumed after WaitForSeconds, after one frame, and ran to its end.
        assertHas(trace, "ball one frame later, frame 31");
        assertHas(trace, "ball blinking done, frame 46");
        // Physics reported the contact to the script, with the speed the two
        // closed at before the solver took it away: 42 steps of 0.02 s under
        // 9.81 m/s2 is 8.24 m/s. The ball is Discrete, as a Rigidbody2D is
        // unless it says otherwise, so the contact is found on the step after
        // the two overlap -- the 43rd, two hundredths of a unit into the
        // ground -- and not at a time of impact within the 42nd.
        assertHas(trace, "ball hit Ground bounce 1 after 43 steps, y=48 closing=824");
        // The pointer event reached Input.
        assertHas(trace, "ball tapped at 160,240 frame 100");
        // Three draw lists, three sprites each, ordered by sorting order.
        for (int frame : new int[] {1, 60, 150}) {
            int at = trace.indexOf("draw frame=" + frame + " sprites=3 background=ff334d80");
            Assert.assertTrue("no draw list for frame " + frame + " in\n" + run.out, at >= 0);
            for (int i = 1; i <= 3; i++) {
                Assert.assertTrue("frame " + frame + " draw command " + i + ": " + trace.get(at + i),
                        trace.get(at + i).matches("  (ball|crate)\\.png order=-?\\d+ at=-?\\d+,-?\\d+ size=\\d+x\\d+ "
                                + "anchor=\\d+,\\d+ rotation=-?\\d+ color=[0-9a-f]{8} flip=[01]{2}"));
            }
        }
        assertHas(trace, "  crate.png order=-1 at=6400,28800 size=9600x4800 anchor=25,25 rotation=-3150"
                + " color=80ff8040 flip=10");

        UnityToolchain.assertSameLines("scene trace",
                UnityToolchain.read(new File(sample, "expected-trace.txt")), run.out);
    }

    private static void assertHas(List<String> trace, String line) {
        Assert.assertTrue("the trace has no line \"" + line + "\":\n" + trace, trace.contains(line));
    }

    private static String after(List<String> trace, String prefix) {
        for (String line : trace) {
            if (line.startsWith(prefix)) {
                return line.substring(prefix.length());
            }
        }
        Assert.fail("the trace has no line starting \"" + prefix + "\":\n" + trace);
        return null;
    }
}
