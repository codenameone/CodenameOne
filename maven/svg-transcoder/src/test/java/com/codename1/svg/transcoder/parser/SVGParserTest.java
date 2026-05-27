package com.codename1.svg.transcoder.parser;

import com.codename1.svg.transcoder.model.*;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

public class SVGParserTest {

    private static SVGDocument parse(String svg) throws IOException {
        return new SVGParser().parse(new ByteArrayInputStream(svg.getBytes("UTF-8")));
    }

    @Test
    public void viewBoxParsed() throws Exception {
        SVGDocument d = parse("<svg xmlns='http://www.w3.org/2000/svg' "
                + "viewBox='0 0 100 200' width='100' height='200'/>");
        assertEquals(100f, d.getWidth(), 0f);
        assertEquals(200f, d.getHeight(), 0f);
        assertEquals(100f, d.getViewBoxWidth(), 0f);
        assertEquals(200f, d.getViewBoxHeight(), 0f);
    }

    @Test
    public void rectShape() throws Exception {
        SVGDocument d = parse("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'>"
                + "<rect x='1' y='2' width='3' height='4' fill='red'/></svg>");
        assertEquals(1, d.getChildren().size());
        SVGRect r = (SVGRect) d.getChildren().get(0);
        assertEquals(1f, r.getX(), 0f);
        assertEquals(3f, r.getWidth(), 0f);
        assertEquals(0xFFFF0000, r.getStyle().getFill().getColor());
    }

    @Test
    public void groupAndCircle() throws Exception {
        SVGDocument d = parse("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'>"
                + "<g transform='translate(5,5)'>"
                + "<circle cx='0' cy='0' r='2' fill='#00FF00'/>"
                + "</g></svg>");
        assertEquals(1, d.getChildren().size());
        SVGGroup g = (SVGGroup) d.getChildren().get(0);
        assertNotNull(g.getTransform());
        assertEquals(5f, g.getTransform().e, 0f);
        SVGCircle c = (SVGCircle) g.getChildren().get(0);
        assertEquals(2f, c.getR(), 0f);
        assertEquals(0xFF00FF00, c.getStyle().getFill().getColor());
    }

    @Test
    public void linearGradientStored() throws Exception {
        SVGDocument d = parse("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'>"
                + "<defs>"
                + "<linearGradient id='g1' x1='0' y1='0' x2='1' y2='0'>"
                + "<stop offset='0' stop-color='#000'/>"
                + "<stop offset='1' stop-color='#FFF'/>"
                + "</linearGradient>"
                + "</defs>"
                + "<rect x='0' y='0' width='10' height='10' fill='url(#g1)'/>"
                + "</svg>");
        SVGLinearGradient lg = (SVGLinearGradient) d.getDefinitions().get("g1");
        assertNotNull(lg);
        assertEquals(2, lg.getStops().size());
        assertEquals(0xFF000000, lg.getStops().get(0).getColor());
        assertEquals(0xFFFFFFFF, lg.getStops().get(1).getColor());

        SVGRect r = (SVGRect) d.getChildren().get(0);
        assertTrue(r.getStyle().getFill().isReference());
        assertEquals("g1", r.getStyle().getFill().getReference());
    }

    @Test
    public void pathParsed() throws Exception {
        SVGDocument d = parse("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'>"
                + "<path d='M0 0 L10 0 L10 10 Z' fill='blue'/></svg>");
        SVGPath p = (SVGPath) d.getChildren().get(0);
        assertEquals(4, p.getCommands().size());
    }

    @Test
    public void smilAnimationParsed() throws Exception {
        SVGDocument d = parse("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'>"
                + "<circle cx='5' cy='5' r='2' fill='red'>"
                + "<animate attributeName='r' from='2' to='5' dur='1s' repeatCount='indefinite'/>"
                + "</circle></svg>");
        SVGCircle c = (SVGCircle) d.getChildren().get(0);
        List<SVGAnimation> anims = c.getAnimations();
        assertEquals(1, anims.size());
        SVGAnimation a = anims.get(0);
        assertEquals("r", a.getAttributeName());
        assertEquals("2", a.getFrom());
        assertEquals("5", a.getTo());
        assertEquals(1000L, a.getDurMs());
        assertEquals(SVGAnimation.REPEAT_INDEFINITE, a.getRepeatCount());
    }

    @Test
    public void textElementParsed() throws Exception {
        SVGDocument d = parse("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 50'>"
                + "<text x='10' y='40' font-size='24' font-weight='bold' "
                + "text-anchor='middle' font-family='Arial' fill='blue'>Hi <tspan>world</tspan></text>"
                + "</svg>");
        assertEquals(1, d.getChildren().size());
        SVGText t = (SVGText) d.getChildren().get(0);
        assertEquals(10f, t.getX(), 0f);
        assertEquals(40f, t.getY(), 0f);
        assertEquals(24f, t.getFontSize(), 0f);
        assertEquals(SVGText.Anchor.MIDDLE, t.getAnchor());
        assertTrue("font-weight=bold should set bold", t.isBold());
        assertEquals("Arial", t.getFontFamily());
        assertEquals("tspan content is flattened into parent text", "Hi world", t.getContent());
        assertEquals(0xFF0000FF, t.getStyle().getFill().getColor());
    }

    @Test
    public void textWithNumericFontWeight() throws Exception {
        SVGDocument d = parse("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'>"
                + "<text x='0' y='10' font-weight='700'>x</text></svg>");
        SVGText t = (SVGText) d.getChildren().get(0);
        assertTrue("font-weight=700 is bold", t.isBold());
    }

    @Test
    public void styleAttributeParsed() throws Exception {
        SVGDocument d = parse("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'>"
                + "<rect x='0' y='0' width='10' height='10' style='fill:#ABCDEF;stroke:none;opacity:0.5'/></svg>");
        SVGRect r = (SVGRect) d.getChildren().get(0);
        assertEquals(0xFFABCDEF, r.getStyle().getFill().getColor());
        assertTrue(r.getStyle().getStroke().isNone());
        assertEquals(0.5f, r.getStyle().getOpacity(), 1e-5f);
    }
}
