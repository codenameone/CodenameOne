/*
 * Copyright (c) 2025, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 */
package com.codename1.lottie;

import com.codename1.lottie.transcoder.LottieTranscoder;
import com.codename1.lottie.transcoder.parser.LottieParser;
import com.codename1.svg.transcoder.model.SVGAnimation;
import com.codename1.svg.transcoder.model.SVGDocument;
import com.codename1.svg.transcoder.model.SVGGroup;
import com.codename1.svg.transcoder.model.SVGNode;
import com.codename1.svg.transcoder.model.SVGRect;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LottieParserTest {

    private static final String SIMPLE_RECT = "{\n" +
            "  \"v\":\"5.7.0\",\"fr\":30,\"ip\":0,\"op\":30,\"w\":100,\"h\":100,\n" +
            "  \"layers\":[{\n" +
            "    \"ty\":4,\"nm\":\"sq\",\"ip\":0,\"op\":30,\n" +
            "    \"ks\":{\n" +
            "      \"a\":{\"a\":0,\"k\":[0,0]},\n" +
            "      \"p\":{\"a\":0,\"k\":[50,50]},\n" +
            "      \"s\":{\"a\":0,\"k\":[100,100]},\n" +
            "      \"r\":{\"a\":0,\"k\":0},\n" +
            "      \"o\":{\"a\":0,\"k\":100}\n" +
            "    },\n" +
            "    \"shapes\":[\n" +
            "      {\"ty\":\"rc\",\"p\":{\"a\":0,\"k\":[0,0]},\"s\":{\"a\":0,\"k\":[40,40]},\"r\":{\"a\":0,\"k\":0}},\n" +
            "      {\"ty\":\"fl\",\"c\":{\"a\":0,\"k\":[1,0,0,1]},\"o\":{\"a\":0,\"k\":100}}\n" +
            "    ]\n" +
            "  }]\n" +
            "}\n";

    private static final String SPINNING_RECT = "{\n" +
            "  \"v\":\"5.7.0\",\"fr\":30,\"ip\":0,\"op\":30,\"w\":100,\"h\":100,\n" +
            "  \"layers\":[{\n" +
            "    \"ty\":4,\"nm\":\"sq\",\"ip\":0,\"op\":30,\n" +
            "    \"ks\":{\n" +
            "      \"a\":{\"a\":0,\"k\":[0,0]},\n" +
            "      \"p\":{\"a\":0,\"k\":[50,50]},\n" +
            "      \"s\":{\"a\":0,\"k\":[100,100]},\n" +
            "      \"r\":{\"a\":1,\"k\":[\n" +
            "        {\"t\":0,\"s\":[0]},{\"t\":30,\"s\":[360]}\n" +
            "      ]},\n" +
            "      \"o\":{\"a\":0,\"k\":100}\n" +
            "    },\n" +
            "    \"shapes\":[\n" +
            "      {\"ty\":\"rc\",\"p\":{\"a\":0,\"k\":[0,0]},\"s\":{\"a\":0,\"k\":[30,30]},\"r\":{\"a\":0,\"k\":4}},\n" +
            "      {\"ty\":\"fl\",\"c\":{\"a\":0,\"k\":[0,0.5,1,1]},\"o\":{\"a\":0,\"k\":100}}\n" +
            "    ]\n" +
            "  }]\n" +
            "}\n";

    @Test
    public void parsesStaticRectIntoSvgDocument() throws Exception {
        SVGDocument doc = LottieParser.parse(new ByteArrayInputStream(
                SIMPLE_RECT.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(doc);
        assertEquals(100f, doc.getWidth(), 0.001f);
        assertEquals(100f, doc.getHeight(), 0.001f);
        // One layer -> one group -> one rect
        assertEquals(1, doc.getChildren().size());
        SVGGroup g = (SVGGroup) doc.getChildren().get(0);
        assertEquals(1, g.getChildren().size());
        SVGNode rectNode = g.getChildren().get(0);
        assertTrue(rectNode instanceof SVGRect);
        SVGRect r = (SVGRect) rectNode;
        assertEquals(40f, r.getWidth(), 0.001f);
        assertNotNull(r.getStyle().getFill());
        // Layer has anim list -- empty for a static layer.
        assertTrue(g.getAnimations().isEmpty());
    }

    @Test
    public void emitsRotationAnimationForSpinner() throws Exception {
        SVGDocument doc = LottieParser.parse(new ByteArrayInputStream(
                SPINNING_RECT.getBytes(StandardCharsets.UTF_8)));
        SVGGroup g = (SVGGroup) doc.getChildren().get(0);
        // One animateTransform expected.
        assertEquals(1, g.getAnimations().size());
        SVGAnimation an = g.getAnimations().get(0);
        assertEquals(SVGAnimation.Kind.ANIMATE_TRANSFORM, an.getKind());
        assertEquals(SVGAnimation.TransformType.ROTATE, an.getTransformType());
        assertEquals(1000L, an.getDurMs());
        assertEquals(SVGAnimation.REPEAT_INDEFINITE, an.getRepeatCount());
    }

    /** Real Bodymovin exports use 3D vectors for position / anchor / scale,
     *  wrap shape primitives in a {@code gr} group with a per-group {@code tr}
     *  transform, and decorate every property with an {@code ix} index. The
     *  parser must ignore all of that extra metadata and still produce the
     *  same renderable subtree as the minimal hand-crafted format. */
    private static final String REAL_BODYMOVIN_SPINNER =
            "{\"v\":\"5.7.0\",\"fr\":30,\"ip\":0,\"op\":30,\"w\":120,\"h\":120,\"nm\":\"spin\",\"ddd\":0,\"assets\":[],\n" +
            " \"layers\":[{\"ddd\":0,\"ind\":1,\"ty\":4,\"nm\":\"sq\",\"sr\":1,\n" +
            "   \"ks\":{\n" +
            "     \"o\":{\"a\":0,\"k\":100,\"ix\":11},\n" +
            "     \"r\":{\"a\":1,\"k\":[{\"i\":{\"x\":[0.8],\"y\":[0.8]},\"o\":{\"x\":[0.2],\"y\":[0.2]},\"t\":0,\"s\":[0]},{\"t\":30,\"s\":[360]}],\"ix\":10},\n" +
            "     \"p\":{\"a\":0,\"k\":[60,60,0],\"ix\":2},\n" +
            "     \"a\":{\"a\":0,\"k\":[0,0,0],\"ix\":1},\n" +
            "     \"s\":{\"a\":0,\"k\":[100,100,100],\"ix\":6}},\n" +
            "   \"ao\":0,\n" +
            "   \"shapes\":[{\"ty\":\"gr\",\"it\":[\n" +
            "     {\"ty\":\"rc\",\"d\":1,\"s\":{\"a\":0,\"k\":[16,32],\"ix\":2},\"p\":{\"a\":0,\"k\":[0,-32],\"ix\":3},\"r\":{\"a\":0,\"k\":4,\"ix\":4},\"nm\":\"Rect\",\"hd\":false},\n" +
            "     {\"ty\":\"fl\",\"c\":{\"a\":0,\"k\":[1,0,0,1],\"ix\":4},\"o\":{\"a\":0,\"k\":100,\"ix\":5},\"r\":1,\"bm\":0,\"nm\":\"Fill\",\"hd\":false},\n" +
            "     {\"ty\":\"tr\",\"p\":{\"a\":0,\"k\":[0,0],\"ix\":2},\"a\":{\"a\":0,\"k\":[0,0],\"ix\":1},\"s\":{\"a\":0,\"k\":[100,100],\"ix\":3},\"r\":{\"a\":0,\"k\":0,\"ix\":6},\"o\":{\"a\":0,\"k\":100,\"ix\":7},\"sk\":{\"a\":0,\"k\":0,\"ix\":4},\"sa\":{\"a\":0,\"k\":0,\"ix\":5},\"nm\":\"Transform\"}\n" +
            "   ],\"nm\":\"Group\",\"np\":3,\"cix\":2,\"bm\":0,\"ix\":1,\"hd\":false}],\n" +
            "   \"ip\":0,\"op\":30,\"st\":0,\"bm\":0}],\n" +
            " \"markers\":[]}";

    @Test
    public void parsesRealBodymovinExport() throws Exception {
        SVGDocument doc = LottieParser.parse(new ByteArrayInputStream(
                REAL_BODYMOVIN_SPINNER.getBytes(StandardCharsets.UTF_8)));
        assertEquals(120f, doc.getWidth(), 0.001f);
        // Layer group with rotation animation
        SVGGroup layer = (SVGGroup) doc.getChildren().get(0);
        assertEquals(1, layer.getAnimations().size());
        assertEquals(SVGAnimation.TransformType.ROTATE,
                layer.getAnimations().get(0).getTransformType());
        // The rect inside the gr/tr wrapping has the fill applied even though
        // the fl entry uses normalized 0..1 RGBA quadruplets and an ix index.
        SVGRect rect = findFirstRect(layer);
        assertNotNull("rect should be reachable through the gr/tr wrapping", rect);
        assertEquals(16f, rect.getWidth(), 0.001f);
        assertEquals(32f, rect.getHeight(), 0.001f);
        assertNotNull(rect.getStyle().getFill());
        assertEquals(0xFFFF0000, rect.getStyle().getFill().getColor());
    }

    private static SVGRect findFirstRect(SVGNode n) {
        if (n instanceof SVGRect) return (SVGRect) n;
        if (n instanceof SVGGroup) {
            for (SVGNode c : ((SVGGroup) n).getChildren()) {
                SVGRect r = findFirstRect(c);
                if (r != null) return r;
            }
        }
        return null;
    }

    @Test
    public void transcodesToCompilableJava() throws Exception {
        StringWriter w = new StringWriter();
        LottieTranscoder.transcode(new ByteArrayInputStream(
                        SPINNING_RECT.getBytes(StandardCharsets.UTF_8)),
                "com.example", "Spin", w);
        String src = w.toString();
        assertTrue(src.contains("package com.example;"));
        assertTrue(src.contains("class Spin extends GeneratedSVGImage"));
        assertTrue(src.contains("paintSVG"));
    }

    // ------------------------------------------------------------------
    // Animation extraction
    // ------------------------------------------------------------------

    private static String layer(String ksBody, String shapes, int op) {
        return "{\"v\":\"5.7.0\",\"fr\":30,\"ip\":0,\"op\":" + op
                + ",\"w\":100,\"h\":100,\"layers\":[{"
                + "\"ty\":4,\"nm\":\"l\",\"ip\":0,\"op\":" + op
                + ",\"ks\":{" + ksBody + "},"
                + "\"shapes\":" + shapes + "}]}";
    }

    @Test
    public void emitsTranslateAnimationForAnimatedPosition() throws Exception {
        // Animated position [10,20] -> [60,80] on a layer that rests at [10,20].
        // The static transform bakes in the resting position; the
        // animateTransform from/to are deltas relative to that pose, so the
        // expected delta is (50, 60) on the to side and (0, 0) on the from side.
        String json = layer(
                "\"a\":{\"a\":0,\"k\":[0,0]},"
                + "\"s\":{\"a\":0,\"k\":[100,100]},"
                + "\"r\":{\"a\":0,\"k\":0},"
                + "\"o\":{\"a\":0,\"k\":100},"
                + "\"p\":{\"a\":1,\"k\":["
                +   "{\"t\":0,\"s\":[10,20]},"
                +   "{\"t\":30,\"s\":[60,80]}"
                + "]}",
                "[{\"ty\":\"rc\",\"p\":{\"a\":0,\"k\":[0,0]},\"s\":{\"a\":0,\"k\":[10,10]},\"r\":{\"a\":0,\"k\":0}},"
                + "{\"ty\":\"fl\",\"c\":{\"a\":0,\"k\":[1,0,0,1]},\"o\":{\"a\":0,\"k\":100}}]",
                30);
        SVGDocument doc = LottieParser.parse(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        SVGGroup g = (SVGGroup) doc.getChildren().get(0);
        SVGAnimation an = findAnimation(g, SVGAnimation.TransformType.TRANSLATE);
        assertNotNull("expected an animateTransform translate", an);
        assertEquals(1000L, an.getDurMs());
        assertEquals(SVGAnimation.REPEAT_INDEFINITE, an.getRepeatCount());
        assertEquals("0.0 0.0", an.getFrom());
        assertEquals("50.0 60.0", an.getTo());
    }

    @Test
    public void emitsScaleAnimationForAnimatedScale() throws Exception {
        // Lottie scale is percent. Start at 50% (resting), end at 150% --
        // codegen normalizes to multipliers relative to resting (1.0 -> 3.0).
        String json = layer(
                "\"a\":{\"a\":0,\"k\":[0,0]},"
                + "\"p\":{\"a\":0,\"k\":[0,0]},"
                + "\"r\":{\"a\":0,\"k\":0},"
                + "\"o\":{\"a\":0,\"k\":100},"
                + "\"s\":{\"a\":1,\"k\":["
                +   "{\"t\":0,\"s\":[50,50]},"
                +   "{\"t\":30,\"s\":[150,150]}"
                + "]}",
                "[{\"ty\":\"el\",\"p\":{\"a\":0,\"k\":[0,0]},\"s\":{\"a\":0,\"k\":[20,20]}},"
                + "{\"ty\":\"fl\",\"c\":{\"a\":0,\"k\":[0,1,0,1]},\"o\":{\"a\":0,\"k\":100}}]",
                30);
        SVGDocument doc = LottieParser.parse(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        SVGGroup g = (SVGGroup) doc.getChildren().get(0);
        SVGAnimation an = findAnimation(g, SVGAnimation.TransformType.SCALE);
        assertNotNull("expected an animateTransform scale", an);
        assertEquals("1.0 1.0", an.getFrom());
        assertEquals("3.0 3.0", an.getTo());
    }

    @Test
    public void noAnimationsForFullyStaticLayer() throws Exception {
        // Static rect should generate zero animation entries even though
        // the layer's "op" defines a non-zero duration.
        SVGDocument doc = LottieParser.parse(new ByteArrayInputStream(
                SIMPLE_RECT.getBytes(StandardCharsets.UTF_8)));
        SVGGroup g = (SVGGroup) doc.getChildren().get(0);
        assertTrue("static layer must not emit animations", g.getAnimations().isEmpty());
    }

    // ------------------------------------------------------------------
    // Shape parsing
    // ------------------------------------------------------------------

    @Test
    public void parsesBezierPathShape() throws Exception {
        // 3-vertex closed path with tangent vectors. The parser converts
        // each vertex pair into a cubic curve so we should see one path
        // node with the expected command sequence (move + n cubics +
        // close).
        String shapes = "[{\"ty\":\"sh\",\"ks\":{\"k\":{"
                + "\"v\":[[0,0],[10,0],[10,10]],"
                + "\"i\":[[0,0],[0,0],[0,0]],"
                + "\"o\":[[0,0],[0,0],[0,0]],"
                + "\"c\":true}}},"
                + "{\"ty\":\"fl\",\"c\":{\"a\":0,\"k\":[0,0,1,1]},\"o\":{\"a\":0,\"k\":100}}]";
        String json = layer(
                "\"a\":{\"a\":0,\"k\":[0,0]},\"p\":{\"a\":0,\"k\":[0,0]},"
                + "\"s\":{\"a\":0,\"k\":[100,100]},\"r\":{\"a\":0,\"k\":0},"
                + "\"o\":{\"a\":0,\"k\":100}",
                shapes, 30);
        SVGDocument doc = LottieParser.parse(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        SVGGroup layer = (SVGGroup) doc.getChildren().get(0);
        com.codename1.svg.transcoder.model.SVGPath path = findFirst(layer, com.codename1.svg.transcoder.model.SVGPath.class);
        assertNotNull("expected a path from the sh shape", path);
        assertNotNull(path.getCommands());
        assertTrue("path must have at least move + 1 curve + close",
                path.getCommands().size() >= 3);
        assertEquals(com.codename1.svg.transcoder.parser.PathCommand.Type.MOVE,
                path.getCommands().get(0).getType());
        // closed=true means last command is CLOSE
        assertEquals(com.codename1.svg.transcoder.parser.PathCommand.Type.CLOSE,
                path.getCommands().get(path.getCommands().size() - 1).getType());
    }

    @Test
    public void multipleShapesShareOneFill() throws Exception {
        // Lottie convention: a single "fl" within a shape group applies to
        // every primitive in that group. The parser must propagate the
        // fill to both primitives.
        String shapes = "[{\"ty\":\"gr\",\"it\":["
                + "{\"ty\":\"rc\",\"p\":{\"a\":0,\"k\":[0,0]},\"s\":{\"a\":0,\"k\":[10,10]},\"r\":{\"a\":0,\"k\":0}},"
                + "{\"ty\":\"el\",\"p\":{\"a\":0,\"k\":[5,5]},\"s\":{\"a\":0,\"k\":[8,8]}},"
                + "{\"ty\":\"fl\",\"c\":{\"a\":0,\"k\":[1,0.5,0,1]},\"o\":{\"a\":0,\"k\":100}}"
                + "]}]";
        String json = layer(
                "\"a\":{\"a\":0,\"k\":[0,0]},\"p\":{\"a\":0,\"k\":[0,0]},"
                + "\"s\":{\"a\":0,\"k\":[100,100]},\"r\":{\"a\":0,\"k\":0},"
                + "\"o\":{\"a\":0,\"k\":100}",
                shapes, 30);
        SVGDocument doc = LottieParser.parse(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        SVGGroup layer = (SVGGroup) doc.getChildren().get(0);
        SVGRect rect = findFirst(layer, SVGRect.class);
        com.codename1.svg.transcoder.model.SVGEllipse ellipse =
                findFirst(layer, com.codename1.svg.transcoder.model.SVGEllipse.class);
        assertNotNull(rect);
        assertNotNull(ellipse);
        // Both shapes carry the same fill ARGB derived from (1, 0.5, 0, 1).
        // 0.5 * 255 rounds to 128 (0x80).
        int expected = 0xFFFF8000;
        assertEquals(expected, rect.getStyle().getFill().getColor());
        assertEquals(expected, ellipse.getStyle().getFill().getColor());
    }

    @Test
    public void extractsStrokeWidthAndColor() throws Exception {
        String shapes = "[{\"ty\":\"rc\",\"p\":{\"a\":0,\"k\":[0,0]},\"s\":{\"a\":0,\"k\":[10,10]},\"r\":{\"a\":0,\"k\":0}},"
                + "{\"ty\":\"st\",\"c\":{\"a\":0,\"k\":[0,1,1,1]},\"o\":{\"a\":0,\"k\":100},\"w\":{\"a\":0,\"k\":3}}]";
        String json = layer(
                "\"a\":{\"a\":0,\"k\":[0,0]},\"p\":{\"a\":0,\"k\":[0,0]},"
                + "\"s\":{\"a\":0,\"k\":[100,100]},\"r\":{\"a\":0,\"k\":0},"
                + "\"o\":{\"a\":0,\"k\":100}",
                shapes, 30);
        SVGDocument doc = LottieParser.parse(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        SVGRect rect = findFirst((SVGGroup) doc.getChildren().get(0), SVGRect.class);
        assertNotNull(rect);
        assertNotNull("stroke must be set when an st entry is present",
                rect.getStyle().getStroke());
        assertEquals(0xFF00FFFF, rect.getStyle().getStroke().getColor());
        assertNotNull(rect.getStyle().getStrokeWidth());
        assertEquals(3f, rect.getStyle().getStrokeWidth().floatValue(), 0.001f);
    }

    // ------------------------------------------------------------------
    // Layer handling
    // ------------------------------------------------------------------

    @Test
    public void multipleLayersPaintBackToFront() throws Exception {
        // Lottie array order: top layer first, bottom layer last. SVG/CN1
        // paint in document order, so the parser reverses the list. The
        // bottom layer (last in JSON) must appear FIRST in the document.
        String json = "{\"v\":\"5.7.0\",\"fr\":30,\"ip\":0,\"op\":30,\"w\":100,\"h\":100,\"layers\":["
                + "{\"ty\":4,\"nm\":\"top\",\"ip\":0,\"op\":30,"
                + "  \"ks\":{\"a\":{\"a\":0,\"k\":[0,0]},\"p\":{\"a\":0,\"k\":[0,0]},\"s\":{\"a\":0,\"k\":[100,100]},\"r\":{\"a\":0,\"k\":0},\"o\":{\"a\":0,\"k\":100}},"
                + "  \"shapes\":[{\"ty\":\"rc\",\"p\":{\"a\":0,\"k\":[0,0]},\"s\":{\"a\":0,\"k\":[20,20]},\"r\":{\"a\":0,\"k\":0}},{\"ty\":\"fl\",\"c\":{\"a\":0,\"k\":[1,0,0,1]},\"o\":{\"a\":0,\"k\":100}}]},"
                + "{\"ty\":4,\"nm\":\"bot\",\"ip\":0,\"op\":30,"
                + "  \"ks\":{\"a\":{\"a\":0,\"k\":[0,0]},\"p\":{\"a\":0,\"k\":[0,0]},\"s\":{\"a\":0,\"k\":[100,100]},\"r\":{\"a\":0,\"k\":0},\"o\":{\"a\":0,\"k\":100}},"
                + "  \"shapes\":[{\"ty\":\"el\",\"p\":{\"a\":0,\"k\":[0,0]},\"s\":{\"a\":0,\"k\":[40,40]}},{\"ty\":\"fl\",\"c\":{\"a\":0,\"k\":[0,1,0,1]},\"o\":{\"a\":0,\"k\":100}}]}"
                + "]}";
        SVGDocument doc = LottieParser.parse(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        assertEquals(2, doc.getChildren().size());
        // First child = last layer in JSON = the ellipse (green)
        SVGGroup first = (SVGGroup) doc.getChildren().get(0);
        SVGGroup second = (SVGGroup) doc.getChildren().get(1);
        assertNotNull(findFirst(first, com.codename1.svg.transcoder.model.SVGEllipse.class));
        assertNotNull(findFirst(second, SVGRect.class));
    }

    @Test
    public void solidColorLayerEmitsRect() throws Exception {
        // ty:1 (solid) with explicit sw/sh/sc should produce one filled rect.
        String json = "{\"v\":\"5.7.0\",\"fr\":30,\"ip\":0,\"op\":30,\"w\":100,\"h\":100,\"layers\":[{"
                + "\"ty\":1,\"nm\":\"bg\",\"ip\":0,\"op\":30,\"sw\":80,\"sh\":60,\"sc\":\"#33aaff\","
                + "\"ks\":{\"a\":{\"a\":0,\"k\":[0,0]},\"p\":{\"a\":0,\"k\":[0,0]},\"s\":{\"a\":0,\"k\":[100,100]},\"r\":{\"a\":0,\"k\":0},\"o\":{\"a\":0,\"k\":100}}"
                + "}]}";
        SVGDocument doc = LottieParser.parse(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        SVGGroup layer = (SVGGroup) doc.getChildren().get(0);
        SVGRect bg = findFirst(layer, SVGRect.class);
        assertNotNull("solid layer should produce a rect", bg);
        assertEquals(80f, bg.getWidth(), 0.001f);
        assertEquals(60f, bg.getHeight(), 0.001f);
        assertEquals(0xFF33AAFF, bg.getStyle().getFill().getColor());
    }

    @Test
    public void unsupportedLayerTypeProducesEmptyGroup() throws Exception {
        // Text (ty:5), image (ty:2), null (ty:3), precomp (ty:0) are not
        // rendered but must not throw and must still produce a child node
        // so the layer index/ordering stays stable.
        String json = "{\"v\":\"5.7.0\",\"fr\":30,\"ip\":0,\"op\":30,\"w\":100,\"h\":100,\"layers\":[{"
                + "\"ty\":5,\"nm\":\"txt\",\"ip\":0,\"op\":30,"
                + "\"ks\":{\"a\":{\"a\":0,\"k\":[0,0]},\"p\":{\"a\":0,\"k\":[0,0]},\"s\":{\"a\":0,\"k\":[100,100]},\"r\":{\"a\":0,\"k\":0},\"o\":{\"a\":0,\"k\":100}}"
                + "}]}";
        SVGDocument doc = LottieParser.parse(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        assertEquals(1, doc.getChildren().size());
        SVGGroup g = (SVGGroup) doc.getChildren().get(0);
        assertTrue("unsupported layer must produce an empty group",
                g.getChildren().isEmpty());
    }

    @Test
    public void opacityBelowFullValueIsBakedIntoStyle() throws Exception {
        String json = layer(
                "\"a\":{\"a\":0,\"k\":[0,0]},\"p\":{\"a\":0,\"k\":[0,0]},"
                + "\"s\":{\"a\":0,\"k\":[100,100]},\"r\":{\"a\":0,\"k\":0},"
                + "\"o\":{\"a\":0,\"k\":40}",
                "[{\"ty\":\"rc\",\"p\":{\"a\":0,\"k\":[0,0]},\"s\":{\"a\":0,\"k\":[10,10]},\"r\":{\"a\":0,\"k\":0}},"
                + "{\"ty\":\"fl\",\"c\":{\"a\":0,\"k\":[1,0,0,1]},\"o\":{\"a\":0,\"k\":100}}]",
                30);
        SVGDocument doc = LottieParser.parse(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        SVGGroup layer = (SVGGroup) doc.getChildren().get(0);
        assertNotNull(layer.getStyle().getOpacity());
        assertEquals(0.4f, layer.getStyle().getOpacity().floatValue(), 0.001f);
    }

    // ------------------------------------------------------------------
    // Color normalization
    // ------------------------------------------------------------------

    @Test
    public void normalizesRgbaZeroToOneIntoArgbInt() throws Exception {
        // Edge cases: 0, 0.5, 1, plus alpha quarter -- verify the
        // round(value * 255) conversion.
        String shapes = "[{\"ty\":\"rc\",\"p\":{\"a\":0,\"k\":[0,0]},\"s\":{\"a\":0,\"k\":[10,10]},\"r\":{\"a\":0,\"k\":0}},"
                + "{\"ty\":\"fl\",\"c\":{\"a\":0,\"k\":[0,0.5,1,0.25]},\"o\":{\"a\":0,\"k\":100}}]";
        String json = layer(
                "\"a\":{\"a\":0,\"k\":[0,0]},\"p\":{\"a\":0,\"k\":[0,0]},"
                + "\"s\":{\"a\":0,\"k\":[100,100]},\"r\":{\"a\":0,\"k\":0},"
                + "\"o\":{\"a\":0,\"k\":100}",
                shapes, 30);
        SVGDocument doc = LottieParser.parse(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        SVGRect rect = findFirst((SVGGroup) doc.getChildren().get(0), SVGRect.class);
        // alpha .25 -> 64 (0x40), R=0, G=128, B=255 -> 0x4000 80FF
        assertEquals(0x400080FF, rect.getStyle().getFill().getColor());
    }

    // ------------------------------------------------------------------
    // Codegen
    // ------------------------------------------------------------------

    @Test
    public void codegenForEllipseLayerProducesGeneralPathDraw() throws Exception {
        // Ensure the codegen reaches an ellipse path even when the source
        // is a Lottie "el" shape inside a gr/tr wrapping.
        StringWriter w = new StringWriter();
        String shapes = "[{\"ty\":\"gr\",\"it\":["
                + "{\"ty\":\"el\",\"p\":{\"a\":0,\"k\":[0,0]},\"s\":{\"a\":0,\"k\":[40,40]}},"
                + "{\"ty\":\"fl\",\"c\":{\"a\":0,\"k\":[1,0,0,1]},\"o\":{\"a\":0,\"k\":100}},"
                + "{\"ty\":\"tr\",\"p\":{\"a\":0,\"k\":[0,0]},\"a\":{\"a\":0,\"k\":[0,0]},"
                + "  \"s\":{\"a\":0,\"k\":[100,100]},\"r\":{\"a\":0,\"k\":0},"
                + "  \"o\":{\"a\":0,\"k\":100},\"sk\":{\"a\":0,\"k\":0},\"sa\":{\"a\":0,\"k\":0}}"
                + "]}]";
        String json = layer(
                "\"a\":{\"a\":0,\"k\":[0,0]},\"p\":{\"a\":0,\"k\":[60,60]},"
                + "\"s\":{\"a\":0,\"k\":[100,100]},\"r\":{\"a\":0,\"k\":0},"
                + "\"o\":{\"a\":0,\"k\":100}",
                shapes, 60);
        LottieTranscoder.transcode(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),
                "com.example", "Ell", w);
        String src = w.toString();
        assertTrue(src.contains("class Ell extends GeneratedSVGImage"));
        // Ellipses lower to a drawArc/fillShape on a GeneralPath -- either
        // way the rendered code references one of those APIs.
        assertTrue("expected fillShape or drawArc in generated paint",
                src.contains("fillShape") || src.contains("drawArc"));
    }

    @Test
    public void parseHandlesMissingTopLevelDimensions() throws Exception {
        // Bodymovin always sets w/h; absence falls back to 100x100 so the
        // generated subclass still compiles even on corrupt exports.
        String json = "{\"v\":\"5.7.0\",\"fr\":30,\"ip\":0,\"op\":30,\"layers\":[]}";
        SVGDocument doc = LottieParser.parse(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        assertEquals(100f, doc.getWidth(), 0.001f);
        assertEquals(100f, doc.getHeight(), 0.001f);
        assertEquals(0, doc.getChildren().size());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static SVGAnimation findAnimation(SVGGroup g, SVGAnimation.TransformType ty) {
        for (SVGAnimation a : g.getAnimations()) {
            if (a.getKind() == SVGAnimation.Kind.ANIMATE_TRANSFORM
                    && a.getTransformType() == ty) {
                return a;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T extends SVGNode> T findFirst(SVGNode n, Class<T> cls) {
        if (cls.isInstance(n)) return (T) n;
        if (n instanceof SVGGroup) {
            for (SVGNode c : ((SVGGroup) n).getChildren()) {
                T hit = findFirst(c, cls);
                if (hit != null) return hit;
            }
        }
        return null;
    }
}
