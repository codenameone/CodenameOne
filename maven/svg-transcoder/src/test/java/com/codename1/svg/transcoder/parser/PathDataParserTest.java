package com.codename1.svg.transcoder.parser;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class PathDataParserTest {

    @Test
    public void emptyReturnsEmpty() {
        assertTrue(PathDataParser.parse("").isEmpty());
        assertTrue(PathDataParser.parse(null).isEmpty());
    }

    @Test
    public void moveAndLine() {
        List<PathCommand> cmds = PathDataParser.parse("M 10 20 L 30 40");
        assertEquals(2, cmds.size());
        assertEquals(PathCommand.Type.MOVE, cmds.get(0).getType());
        assertArrayEquals(new float[]{10f, 20f}, cmds.get(0).getArgs(), 0f);
        assertEquals(PathCommand.Type.LINE, cmds.get(1).getType());
        assertArrayEquals(new float[]{30f, 40f}, cmds.get(1).getArgs(), 0f);
    }

    @Test
    public void relativeMoveBecomesAbsolute() {
        List<PathCommand> cmds = PathDataParser.parse("M 10 10 m 5 5");
        // M absolute then m relative produces second MOVE at (15, 15)
        assertEquals(2, cmds.size());
        assertArrayEquals(new float[]{15f, 15f}, cmds.get(1).getArgs(), 0f);
    }

    @Test
    public void implicitLineAfterMove() {
        List<PathCommand> cmds = PathDataParser.parse("M 0 0 10 10 20 20");
        // After M, subsequent coordinate pairs are implicit L
        assertEquals(3, cmds.size());
        assertEquals(PathCommand.Type.MOVE, cmds.get(0).getType());
        assertEquals(PathCommand.Type.LINE, cmds.get(1).getType());
        assertEquals(PathCommand.Type.LINE, cmds.get(2).getType());
    }

    @Test
    public void horizontalVerticalLines() {
        List<PathCommand> cmds = PathDataParser.parse("M 0 0 H 50 V 60 h 10 v 10");
        assertEquals(5, cmds.size());
        // H 50 -> LINE (50, 0)
        assertArrayEquals(new float[]{50f, 0f}, cmds.get(1).getArgs(), 0f);
        // V 60 -> LINE (50, 60)
        assertArrayEquals(new float[]{50f, 60f}, cmds.get(2).getArgs(), 0f);
        // h 10 -> LINE (60, 60)
        assertArrayEquals(new float[]{60f, 60f}, cmds.get(3).getArgs(), 0f);
        // v 10 -> LINE (60, 70)
        assertArrayEquals(new float[]{60f, 70f}, cmds.get(4).getArgs(), 0f);
    }

    @Test
    public void cubicBezier() {
        List<PathCommand> cmds = PathDataParser.parse("M 0 0 C 1 2 3 4 5 6");
        assertEquals(2, cmds.size());
        assertEquals(PathCommand.Type.CUBIC, cmds.get(1).getType());
        assertArrayEquals(new float[]{1, 2, 3, 4, 5, 6}, cmds.get(1).getArgs(), 0f);
    }

    @Test
    public void smoothCubicReflectsControlPoint() {
        // After C 1,1 3,3 5,5 the implicit S control = 2*(5,5) - (3,3) = (7, 7)
        List<PathCommand> cmds = PathDataParser.parse("M 0 0 C 1 1 3 3 5 5 S 8 8 10 10");
        assertEquals(3, cmds.size());
        PathCommand smooth = cmds.get(2);
        assertEquals(PathCommand.Type.CUBIC, smooth.getType());
        assertEquals(7f, smooth.getArgs()[0], 1e-6f);
        assertEquals(7f, smooth.getArgs()[1], 1e-6f);
    }

    @Test
    public void closePath() {
        List<PathCommand> cmds = PathDataParser.parse("M 0 0 L 10 0 L 10 10 Z");
        assertEquals(4, cmds.size());
        assertEquals(PathCommand.Type.CLOSE, cmds.get(3).getType());
    }

    @Test
    public void arcCommand() {
        List<PathCommand> cmds = PathDataParser.parse("M 0 0 A 5 5 0 0 1 10 0");
        assertEquals(2, cmds.size());
        assertEquals(PathCommand.Type.ARC, cmds.get(1).getType());
        float[] a = cmds.get(1).getArgs();
        // expected: curX, curY, rx, ry, xRot, largeArc, sweep, x, y
        assertEquals(0f, a[0], 0f);
        assertEquals(0f, a[1], 0f);
        assertEquals(5f, a[2], 0f);
        assertEquals(5f, a[3], 0f);
        assertEquals(0f, a[5], 0f);
        assertEquals(1f, a[6], 0f);
        assertEquals(10f, a[7], 0f);
    }

    @Test
    public void smoothQuadraticReflectsControlPoint() {
        // After Q 1,1 5,5 the implicit T control = 2*(5,5) - (1,1) = (9, 9)
        List<PathCommand> cmds = PathDataParser.parse("M 0 0 Q 1 1 5 5 T 10 10");
        assertEquals(3, cmds.size());
        PathCommand smooth = cmds.get(2);
        assertEquals(PathCommand.Type.QUAD, smooth.getType());
        assertEquals(9f, smooth.getArgs()[0], 1e-6f);
        assertEquals(9f, smooth.getArgs()[1], 1e-6f);
    }

    @Test
    public void smoothCurveFallsBackToCurrentPoint() {
        // S immediately after M (no prior cubic) uses current point as
        // the implicit first control. The control should equal the current
        // point (0, 0) after the M.
        List<PathCommand> cmds = PathDataParser.parse("M 0 0 S 5 5 10 10");
        assertEquals(2, cmds.size());
        PathCommand smooth = cmds.get(1);
        assertEquals(PathCommand.Type.CUBIC, smooth.getType());
        assertEquals(0f, smooth.getArgs()[0], 0f);
        assertEquals(0f, smooth.getArgs()[1], 0f);
    }

    @Test
    public void closeFollowedByImplicitMoveRebasesStart() {
        // After Z the current point returns to the subpath start. A
        // subsequent relative M (m) is relative to that subpath start.
        List<PathCommand> cmds = PathDataParser.parse("M 10 10 L 20 20 Z m 5 5");
        assertEquals(4, cmds.size());
        // The m moves to (10+5, 10+5) = (15, 15)
        assertEquals(PathCommand.Type.MOVE, cmds.get(3).getType());
        assertArrayEquals(new float[]{15f, 15f}, cmds.get(3).getArgs(), 0f);
    }

    @Test
    public void implicitSignSeparator() {
        // "10-20" should parse as two numbers 10 and -20
        List<PathCommand> cmds = PathDataParser.parse("M0 0L10-20");
        assertEquals(2, cmds.size());
        assertArrayEquals(new float[]{10f, -20f}, cmds.get(1).getArgs(), 0f);
    }
}
