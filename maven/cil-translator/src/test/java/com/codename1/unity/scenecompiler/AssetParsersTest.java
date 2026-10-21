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

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/// What the scene compiler reads out of an animation clip, an animator
/// controller, a tilemap and a particle system, each from the YAML Unity
/// writes for it.
public class AssetParsersTest {
    private static final AnimationAssets.Sprites SPRITES = new AnimationAssets.Sprites() {
        @Override
        public String sprite(String user, Object reference) {
            return "sprite_" + SceneCompiler.fileId(reference);
        }
    };

    private static final AnimationAssets.Clips CLIPS = new AnimationAssets.Clips() {
        @Override
        public String clip(String user, Object reference) {
            return "clip_" + SceneCompiler.text(SceneCompiler.map(reference).get("guid"));
        }
    };

    private static final String CLIP = "%YAML 1.1\n"
            + "--- !u!74 &7400000\n"
            + "AnimationClip:\n"
            + "  m_Name: Run\n"
            + "  m_RotationCurves: []\n"
            + "  m_EulerCurves:\n"
            + "  - curve:\n"
            + "      m_Curve:\n"
            + "      - time: 0\n"
            + "        value: {x: 0, y: 0, z: 0}\n"
            + "        inSlope: {x: 0, y: 0, z: 90}\n"
            + "        outSlope: {x: 0, y: 0, z: 90}\n"
            + "      - time: 1\n"
            + "        value: {x: 0, y: 30, z: 90}\n"
            + "        inSlope: {x: 0, y: 0, z: 90}\n"
            + "        outSlope: {x: 0, y: 0, z: 90}\n"
            + "    path: Arm\n"
            + "  m_PositionCurves:\n"
            + "  - curve:\n"
            + "      m_Curve:\n"
            + "      - time: 0\n"
            + "        value: {x: 1, y: 2, z: 0}\n"
            + "        inSlope: {x: 0, y: Infinity, z: 0}\n"
            + "        outSlope: {x: 0, y: Infinity, z: 0}\n"
            + "    path: \n"
            + "  m_ScaleCurves: []\n"
            + "  m_FloatCurves:\n"
            + "  - curve:\n"
            + "      m_Curve:\n"
            + "      - time: 0\n"
            + "        value: 1\n"
            + "        inSlope: 0\n"
            + "        outSlope: -2\n"
            + "      - time: 0.5\n"
            + "        value: 0\n"
            + "        inSlope: -2\n"
            + "        outSlope: 0\n"
            + "    attribute: m_Color.a\n"
            + "    path: \n"
            + "    classID: 212\n"
            + "  - curve:\n"
            + "      m_Curve: []\n"
            + "    attribute: m_Size.x\n"
            + "    path: Box\n"
            + "    classID: 61\n"
            + "  m_PPtrCurves:\n"
            + "  - curve:\n"
            + "    - time: 0\n"
            + "      value: {fileID: 11, guid: aa, type: 3}\n"
            + "    - time: 0.25\n"
            + "      value: {fileID: 12, guid: aa, type: 3}\n"
            + "    attribute: m_Sprite\n"
            + "    path: \n"
            + "    classID: 212\n"
            + "  m_SampleRate: 20\n"
            + "  m_AnimationClipSettings:\n"
            + "    m_StartTime: 0\n"
            + "    m_StopTime: 1\n"
            + "    m_LoopTime: 1\n"
            + "  m_Events:\n"
            + "  - time: 0.5\n"
            + "    functionName: Step\n"
            + "    data: \n"
            + "    objectReferenceParameter: {fileID: 0}\n"
            + "    floatParameter: 0\n"
            + "    intParameter: 3\n";

    /// A clip's sprite keys, its float curve on a renderer, its position and
    /// its turn about z become one call each; what the runtime does not
    /// animate is named in a warning and not silently dropped.
    @Test
    public void aClipBecomesOneCallForEachCurve() {
        List<String> warnings = new ArrayList<String>();
        List<String> notes = new ArrayList<String>();
        String out = AnimationAssets.clip("Run.anim", UnityYaml.parse(CLIP), "c", SPRITES, warnings, notes);
        Assert.assertTrue(out, out.contains("AnimationClip(\"Run\", 1.0f, 20.0f, true);"));
        Assert.assertTrue(out, out.contains("c.$sprites(\"\", new float[] {0.0f, 0.25f}, new "
                + "com.codename1.unitycompat.unityengine.Sprite[] {sprite_11, sprite_12});"));
        Assert.assertTrue(out, out.contains("c.$curve(\"\", 10, new float[] {"
                + "0.0f, 1.0f, 0.0f, -2.0f, 0.5f, 0.0f, -2.0f, 0.0f});"));
        // A stepped key is an infinite slope, which has to stay one.
        Assert.assertTrue(out, out.contains("c.$curve(\"\", 1, new float[] {0.0f, 2.0f, Float.POSITIVE_INFINITY, "
                + "Float.POSITIVE_INFINITY});"));
        Assert.assertTrue(out, out.contains("c.$curve(\"Arm\", 3, new float[] {"
                + "0.0f, 0.0f, 90.0f, 90.0f, 1.0f, 90.0f, 90.0f, 90.0f});"));
        Assert.assertTrue(out, out.contains("c.$event(0.5f, \"Step\");"));
        Assert.assertEquals(warnings.toString(), 1, warnings.size());
        Assert.assertTrue(warnings.get(0), warnings.get(0).contains("m_Size.x of 'Box'"));
        // The turn about y that is not zero, and the event's argument.
        Assert.assertEquals(notes.toString(), 2, notes.size());
        Assert.assertTrue(notes.get(0), notes.get(0).contains("about y"));
        Assert.assertTrue(notes.get(1), notes.get(1).contains("Step carries an argument"));
    }

    /// The properties a curve can name, by the class of the component.
    @Test
    public void aCurveIsKnownByClassAndAttribute() {
        Assert.assertEquals(0, AnimationAssets.kind(4, "m_LocalPosition.x"));
        Assert.assertEquals(3, AnimationAssets.kind(4, "localEulerAnglesRaw.z"));
        Assert.assertEquals(6, AnimationAssets.kind(224, "m_LocalScale.z"));
        Assert.assertEquals(7, AnimationAssets.kind(212, "m_Color.r"));
        Assert.assertEquals(11, AnimationAssets.kind(212, "m_Enabled"));
        Assert.assertEquals(12, AnimationAssets.kind(1, "m_IsActive"));
        Assert.assertEquals(13, AnimationAssets.kind(212, "m_FlipX"));
        Assert.assertEquals(-1, AnimationAssets.kind(4, "m_Color.r"));
        Assert.assertEquals(-1, AnimationAssets.kind(212, "m_LocalPosition.x"));
    }

    private static final String CONTROLLER = "%YAML 1.1\n"
            + "--- !u!91 &9100000\n"
            + "AnimatorController:\n"
            + "  m_Name: Hero\n"
            + "  m_AnimatorParameters:\n"
            + "  - m_Name: speed\n"
            + "    m_Type: 1\n"
            + "    m_DefaultFloat: 0.5\n"
            + "    m_DefaultInt: 0\n"
            + "    m_DefaultBool: 0\n"
            + "  - m_Name: lives\n"
            + "    m_Type: 3\n"
            + "    m_DefaultFloat: 0\n"
            + "    m_DefaultInt: 3\n"
            + "    m_DefaultBool: 0\n"
            + "  - m_Name: grounded\n"
            + "    m_Type: 4\n"
            + "    m_DefaultFloat: 0\n"
            + "    m_DefaultInt: 0\n"
            + "    m_DefaultBool: 1\n"
            + "  - m_Name: hit\n"
            + "    m_Type: 9\n"
            + "    m_DefaultFloat: 0\n"
            + "    m_DefaultInt: 0\n"
            + "    m_DefaultBool: 0\n"
            + "  m_AnimatorLayers:\n"
            + "  - m_Name: Base Layer\n"
            + "    m_StateMachine: {fileID: 50}\n"
            + "--- !u!1107 &50\n"
            + "AnimatorStateMachine:\n"
            + "  m_ChildStates:\n"
            + "  - m_State: {fileID: 60}\n"
            + "  - m_State: {fileID: 61}\n"
            + "  m_ChildStateMachines: []\n"
            + "  m_AnyStateTransitions:\n"
            + "  - {fileID: 72}\n"
            + "  m_EntryTransitions: []\n"
            + "  m_DefaultState: {fileID: 61}\n"
            + "--- !u!1102 &60\n"
            + "AnimatorState:\n"
            + "  m_Name: Idle\n"
            + "  m_Speed: 1\n"
            + "  m_Transitions:\n"
            + "  - {fileID: 70}\n"
            + "  - {fileID: 73}\n"
            + "  m_WriteDefaultValues: 1\n"
            + "  m_Motion: {fileID: 7400000, guid: idle, type: 2}\n"
            + "--- !u!1102 &61\n"
            + "AnimatorState:\n"
            + "  m_Name: Run\n"
            + "  m_Speed: 2\n"
            + "  m_Transitions:\n"
            + "  - {fileID: 71}\n"
            + "  m_WriteDefaultValues: 0\n"
            + "  m_Motion: {fileID: 0}\n"
            + "--- !u!1101 &70\n"
            + "AnimatorStateTransition:\n"
            + "  m_Conditions:\n"
            + "  - m_ConditionMode: 3\n"
            + "    m_ConditionEvent: speed\n"
            + "    m_EventTreshold: 0.1\n"
            + "  - m_ConditionMode: 1\n"
            + "    m_ConditionEvent: grounded\n"
            + "    m_EventTreshold: 0\n"
            + "  m_DstState: {fileID: 61}\n"
            + "  m_Mute: 0\n"
            + "  m_IsExit: 0\n"
            + "  m_TransitionDuration: 0.25\n"
            + "  m_TransitionOffset: 0\n"
            + "  m_ExitTime: 0.75\n"
            + "  m_HasExitTime: 0\n"
            + "  m_HasFixedDuration: 1\n"
            + "  m_CanTransitionToSelf: 1\n"
            + "--- !u!1101 &71\n"
            + "AnimatorStateTransition:\n"
            + "  m_Conditions: []\n"
            + "  m_DstState: {fileID: 60}\n"
            + "  m_Mute: 0\n"
            + "  m_IsExit: 0\n"
            + "  m_TransitionDuration: 0\n"
            + "  m_TransitionOffset: 0\n"
            + "  m_ExitTime: 1\n"
            + "  m_HasExitTime: 1\n"
            + "  m_HasFixedDuration: 0\n"
            + "  m_CanTransitionToSelf: 0\n"
            + "--- !u!1101 &72\n"
            + "AnimatorStateTransition:\n"
            + "  m_Conditions:\n"
            + "  - m_ConditionMode: 1\n"
            + "    m_ConditionEvent: hit\n"
            + "    m_EventTreshold: 0\n"
            + "  m_DstState: {fileID: 60}\n"
            + "  m_Mute: 0\n"
            + "  m_IsExit: 0\n"
            + "  m_TransitionDuration: 0\n"
            + "  m_TransitionOffset: 0\n"
            + "  m_ExitTime: 0\n"
            + "  m_HasExitTime: 0\n"
            + "  m_HasFixedDuration: 1\n"
            + "  m_CanTransitionToSelf: 0\n"
            + "--- !u!1101 &73\n"
            + "AnimatorStateTransition:\n"
            + "  m_Conditions:\n"
            + "  - m_ConditionMode: 1\n"
            + "    m_ConditionEvent: missing\n"
            + "    m_EventTreshold: 0\n"
            + "  m_DstState: {fileID: 61}\n"
            + "  m_Mute: 0\n"
            + "  m_IsExit: 0\n";

    /// Each kind of parameter with its default, each state with its clip,
    /// the default state, and each transition with its conditions as
    /// (mode, parameter, threshold); a condition on a parameter the
    /// controller does not have leaves the transition out and says so.
    @Test
    public void aControllerBecomesStatesAndTransitions() {
        List<String> warnings = new ArrayList<String>();
        String out = AnimationAssets.controller("Hero.controller", UnityYaml.parse(CONTROLLER), "m", CLIPS, warnings);
        Assert.assertTrue(out, out.contains("RuntimeAnimatorController(\"Hero\");"));
        Assert.assertTrue(out, out.contains("m.$parameter(\"speed\", 1, 0.5f);"));
        Assert.assertTrue(out, out.contains("m.$parameter(\"lives\", 3, 3f);"));
        Assert.assertTrue(out, out.contains("m.$parameter(\"grounded\", 4, 1f);"));
        Assert.assertTrue(out, out.contains("m.$parameter(\"hit\", 9, 0f);"));
        Assert.assertTrue(out, out.contains("m.$state(\"Idle\", 1.0f, clip_idle, true);"));
        Assert.assertTrue(out, out.contains("m.$state(\"Run\", 2.0f, null, false);"));
        Assert.assertTrue(out, out.contains("m.$defaultState(1);"));
        Assert.assertTrue(out, out.contains(
                "m.$transition(0, 1, false, 0.75f, 0.25f, true, 0.0f, true, new float[] {"
                + "3f, 0f, 0.1f, 1f, 2f, 0.0f});"));
        Assert.assertTrue(out, out.contains("m.$transition(1, 0, true, 1.0f, 0.0f, false, 0.0f, false, "
                + "new float[] {});"));
        Assert.assertTrue(out, out.contains("m.$transition(-1, 0, false, 0.0f, 0.0f, true, 0.0f, false, "
                + "new float[] {1f, 3f, 0.0f});"));
        // Any State's transitions come before a state's own.
        Assert.assertTrue(out, out.indexOf("$transition(-1,") < out.indexOf("$transition(0,"));
        Assert.assertEquals(warnings.toString(), 1, warnings.size());
        Assert.assertTrue(warnings.get(0), warnings.get(0).contains("tests the parameter 'missing'"));
    }

    /// Cells are written as runs along a row, lowest row first, and each
    /// combination of tile, sprite, matrix and colour once; a cell off the
    /// plane is counted and left out.
    @Test
    public void tilemapCellsBecomeRunsOfAVariant() {
        String text = "%YAML 1.1\n"
                + "--- !u!1839735485 &5\n"
                + "Tilemap:\n"
                + "  m_Tiles:\n"
                + "  - first: {x: 1, y: 2, z: 0}\n"
                + "    second:\n"
                + "      m_TileIndex: 0\n"
                + "      m_TileSpriteIndex: 0\n"
                + "      m_TileMatrixIndex: 0\n"
                + "      m_TileColorIndex: 0\n"
                + "  - first: {x: -3, y: 1, z: 0}\n"
                + "    second:\n"
                + "      m_TileIndex: 1\n"
                + "      m_TileSpriteIndex: 1\n"
                + "      m_TileMatrixIndex: 0\n"
                + "      m_TileColorIndex: 0\n"
                + "  - first: {x: -1, y: 2, z: 0}\n"
                + "    second:\n"
                + "      m_TileIndex: 0\n"
                + "      m_TileSpriteIndex: 0\n"
                + "      m_TileMatrixIndex: 0\n"
                + "      m_TileColorIndex: 0\n"
                + "  - first: {x: 0, y: 2, z: 0}\n"
                + "    second:\n"
                + "      m_TileIndex: 0\n"
                + "      m_TileSpriteIndex: 0\n"
                + "      m_TileMatrixIndex: 0\n"
                + "      m_TileColorIndex: 0\n"
                + "  - first: {x: 2, y: 2, z: 0}\n"
                + "    second:\n"
                + "      m_TileIndex: 0\n"
                + "      m_TileSpriteIndex: 0\n"
                + "      m_TileMatrixIndex: 1\n"
                + "      m_TileColorIndex: 0\n"
                + "  - first: {x: 4, y: 2, z: 1}\n"
                + "    second:\n"
                + "      m_TileIndex: 0\n"
                + "      m_TileSpriteIndex: 0\n"
                + "      m_TileMatrixIndex: 0\n"
                + "      m_TileColorIndex: 0\n";
        TilemapAssets.Cells cells = TilemapAssets.cells(UnityYaml.parse(text).get(0).properties.get("m_Tiles"));
        Assert.assertEquals(5, cells.count);
        Assert.assertEquals(1, cells.offPlane);
        Assert.assertEquals(3, cells.variants.size());
        Assert.assertEquals(1, cells.variants.get(2).matrix);
        Assert.assertEquals(-3, cells.fromX);
        Assert.assertEquals(1, cells.fromY);
        Assert.assertEquals(6, cells.across);
        Assert.assertEquals(2, cells.up);
        Assert.assertEquals(1, cells.runs.size());
        // Variants are numbered as the file meets them: 0 at (1,2) first.
        Assert.assertEquals("-3 1 1 1 -1 2 3 0 2 2 1 2", cells.runs.get(0));
    }

    /// A tile's matrix is a scale, a turn and an offset in the plane; a
    /// mirrored tile is a negative scale up, never a turn.
    @Test
    public void aTileMatrixIsScaleTurnAndOffset() {
        Map<String, Object> turned = SceneCompiler.map(UnityYaml.parse("%YAML 1.1\n--- !u!1 &1\nX:\n"
                + "  m: {e00: 0, e01: -1, e03: 0.5, e10: 1, e11: 0, e13: -0.25}\n").get(0).properties.get("m"));
        Assert.assertArrayEquals(new double[] {1, 1, 90, 0.5, -0.25}, TilemapAssets.place(turned), 0);
        Map<String, Object> mirrored = SceneCompiler.map(UnityYaml.parse("%YAML 1.1\n--- !u!1 &1\nX:\n"
                + "  m: {e00: 1, e01: 0, e03: 0, e10: 0, e11: -1, e13: 0}\n").get(0).properties.get("m"));
        Assert.assertArrayEquals(new double[] {1, -1, 0, 0, 0}, TilemapAssets.place(mirrored), 0);
        Assert.assertArrayEquals(new double[] {1, 1, 0, 0, 0},
                TilemapAssets.place(SceneCompiler.map(null)), 0);
    }

    /// The solid pixels of a tile: null for a full square (the common
    /// case, which then costs nothing), a bit a pixel otherwise, from the
    /// importer's physics shape when it has one.
    @Test
    public void solidPixelsComeFromAlphaOrThePhysicsShape() {
        BufferedImage image = new BufferedImage(8, 4, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                image.setRGB(x, y, 0xff336699);
            }
        }
        // The left tile is full; the right one is empty but for a corner.
        image.setRGB(4, 3, 0x01000000);
        Assert.assertNull(TilemapAssets.solid(image, 0, 0, 4, 4, null));
        Assert.assertArrayEquals(new int[] {0, 0, 0, 1}, TilemapAssets.solid(image, 4, 0, 4, 4, null));
        // A shape of the lower half, in pixels from the middle, y up.
        List<double[]> shape = TilemapAssets.shape(UnityYaml.parse("%YAML 1.1\n--- !u!1 &1\nX:\n"
                + "  physicsShape:\n"
                + "  - - {x: -2, y: -2}\n"
                + "    - {x: 2, y: -2}\n"
                + "    - {x: 2, y: 0}\n"
                + "    - {x: -2, y: 0}\n").get(0).properties.get("physicsShape"));
        Assert.assertEquals(1, shape.size());
        Assert.assertArrayEquals(new int[] {0, 0, 15, 15}, TilemapAssets.solid(image, 0, 0, 4, 4, shape));
    }

    /// A gradient's key times are sixteen-bit fractions, and a gradient of
    /// fixed steps is told apart by a negative count.
    @Test
    public void aGradientIsColourKeysThenAlphaKeys() {
        Object g = UnityYaml.parse("%YAML 1.1\n--- !u!1 &1\nX:\n"
                + "  g:\n"
                + "    key0: {r: 1, g: 0, b: 0, a: 1}\n"
                + "    key1: {r: 0, g: 0, b: 1, a: 0}\n"
                + "    ctime0: 0\n"
                + "    ctime1: 65535\n"
                + "    atime0: 0\n"
                + "    atime1: 65535\n"
                + "    m_Mode: 0\n"
                + "    m_NumColorKeys: 2\n"
                + "    m_NumAlphaKeys: 2\n").get(0).properties.get("g");
        Assert.assertEquals("new float[] {2f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f}",
                ParticleAssets.gradient(g));
        Assert.assertEquals("null", ParticleAssets.gradient(null));
    }

    /// A shape turned to face along x sends its own forward axis to the
    /// right of the screen; the object's turn about z is left out, because
    /// the runtime applies that itself from the transform.
    @Test
    public void anEmitterShapeIsProjectedOntoThePlane() {
        Map<String, Object> none = SceneCompiler.map(null);
        Assert.assertArrayEquals(new double[] {1, 0, 0, 0, 1, 0}, ParticleAssets.matrix(none, none), 0);
        Map<String, Object> about = SceneCompiler.map(UnityYaml.parse("%YAML 1.1\n--- !u!1 &1\nX:\n"
                + "  q: {x: 0, y: 0, z: 0.7071068, w: 0.7071068}\n"
                + "  e: {x: 0, y: 90, z: 0}\n").get(0).properties);
        Assert.assertArrayEquals(new double[] {1, 0, 0, 0, 1, 0},
                ParticleAssets.matrix(SceneCompiler.map(about.get("q")), none), 0);
        Assert.assertArrayEquals(new double[] {0, 0, 1, 0, 1, 0},
                ParticleAssets.matrix(none, SceneCompiler.map(about.get("e"))), 0);
    }
}
