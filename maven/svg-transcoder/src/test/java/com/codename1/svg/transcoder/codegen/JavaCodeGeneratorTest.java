package com.codename1.svg.transcoder.codegen;

import com.codename1.svg.transcoder.SVGTranscoder;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;

import static org.junit.Assert.*;

public class JavaCodeGeneratorTest {

    private static String transcode(String svg) throws Exception {
        StringWriter sw = new StringWriter();
        SVGTranscoder.transcode(new ByteArrayInputStream(svg.getBytes("UTF-8")),
                "com.example", "TestIcon", sw);
        return sw.toString();
    }

    @Test
    public void classBoilerplateEmitted() throws Exception {
        String out = transcode("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'/>");
        assertTrue(out.contains("package com.example;"));
        assertTrue(out.contains("public final class TestIcon extends GeneratedSVGImage"));
        assertTrue(out.contains("import com.codename1.ui.GeneratedSVGImage;"));
        assertTrue(out.contains("import com.codename1.ui.geom.GeneralPath;"));
        assertTrue(out.contains("protected void paintSVG(Graphics g, long __t)"));
    }

    @Test
    public void rectGeneratesPath() throws Exception {
        String out = transcode("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'>"
                + "<rect x='1' y='2' width='3' height='4' fill='red'/></svg>");
        assertTrue(out.contains("new GeneralPath()"));
        assertTrue(out.contains("moveTo"));
        assertTrue(out.contains("lineTo"));
        assertTrue(out.contains("closePath"));
        assertTrue(out.contains("g.fillShape(__p)"));
        // fill="red" -> 0xFF0000
        assertTrue(out.contains("g.setColor(0xFF0000)"));
    }

    @Test
    public void circleUsesArc() throws Exception {
        String out = transcode("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'>"
                + "<circle cx='5' cy='5' r='3' fill='blue'/></svg>");
        assertTrue(out.contains("__p.arc("));
        assertTrue(out.contains("g.setColor(0x"));
    }

    @Test
    public void strokeEmitsStroke() throws Exception {
        String out = transcode("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'>"
                + "<line x1='0' y1='0' x2='10' y2='10' stroke='black' stroke-width='2'/></svg>");
        assertTrue(out.contains("g.drawShape(__p, __s)"));
        assertTrue(out.contains("new Stroke("));
    }

    @Test
    public void animationIsReportedAtConstruction() throws Exception {
        String out = transcode("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'>"
                + "<circle cx='5' cy='5' r='3'>"
                + "<animate attributeName='r' from='3' to='5' dur='1s' repeatCount='indefinite'/>"
                + "</circle></svg>");
        assertTrue("animated flag should be true",
                out.contains("super(10, 10, 0.0f, 0.0f, 10.0f, 10.0f, true);"));
        // r animation should reach into runtime helper
        assertTrue(out.contains("GeneratedSVGImage.progress(__t,"));
        assertTrue(out.contains("GeneratedSVGImage.lerp(3.0f, 5.0f"));
    }

    @Test
    public void groupTransformEmitsConcatenate() throws Exception {
        String out = transcode("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'>"
                + "<g transform='translate(2,3)'>"
                + "<rect x='0' y='0' width='1' height='1' fill='red'/></g></svg>");
        // Each transform block declares a unique pair of locals (__tsaveN / __tnewN)
        // so sibling transforms compile without local-variable shadowing.
        assertTrue("expected makeAffine call",
                out.contains(".concatenate(Transform.makeAffine("));
        assertTrue("expected setTransform on the fresh transform",
                out.matches("(?s).*g\\.setTransform\\(__tnew\\d+\\);.*"));
        assertTrue("expected setTransform on the saved transform in finally",
                out.matches("(?s).*g\\.setTransform\\(__tsave\\d+\\);.*"));
    }

    @Test
    public void siblingTransformedRectsUseFreshVariableNames() throws Exception {
        // Regression: a group with multiple transformed siblings used to
        // emit two `Transform __new = ...` declarations in the same scope,
        // which doesn't compile under Java's no-shadowing rule for locals.
        String out = transcode("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'>"
                + "<g transform='translate(5,5)'>"
                + "<rect x='0' y='0' width='1' height='1' transform='rotate(45)' fill='red'/>"
                + "<rect x='0' y='0' width='1' height='1' transform='rotate(90)' fill='blue'/>"
                + "</g></svg>");
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("__tnew(\\d+)").matcher(out);
        java.util.Set<String> ids = new java.util.HashSet<String>();
        while (m.find()) ids.add(m.group(1));
        assertTrue("expected multiple distinct transform-block IDs but found " + ids, ids.size() >= 3);
    }

    @Test
    public void pathArcCallsRuntimeHelper() throws Exception {
        String out = transcode("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'>"
                + "<path d='M 0 0 A 5 5 0 0 1 10 0' fill='red'/></svg>");
        assertTrue(out.contains("GeneratedSVGImage.svgArc(__p"));
    }

    @Test
    public void linearGradientEmitsPaint() throws Exception {
        String out = transcode("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'>"
                + "<defs><linearGradient id='g1' x1='0' y1='0' x2='1' y2='0'>"
                + "<stop offset='0' stop-color='red'/>"
                + "<stop offset='1' stop-color='blue'/>"
                + "</linearGradient></defs>"
                + "<rect x='0' y='0' width='10' height='10' fill='url(#g1)'/></svg>");
        assertTrue(out.contains("new LinearGradientPaint("));
        assertTrue(out.contains("CycleMethod.NO_CYCLE"));
    }

    @Test
    public void classNameFor() {
        assertEquals("HomeIcon", SVGTranscoder.classNameFor("home-icon.svg"));
        assertEquals("Foo", SVGTranscoder.classNameFor("foo"));
        assertEquals("_1Item", SVGTranscoder.classNameFor("1item.svg"));
        assertEquals("MyWeirdName", SVGTranscoder.classNameFor("my.weird name.svg"));
    }
}
