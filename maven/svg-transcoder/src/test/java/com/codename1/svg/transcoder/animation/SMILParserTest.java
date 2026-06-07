package com.codename1.svg.transcoder.animation;

import com.codename1.svg.transcoder.model.SVGAnimation;
import org.junit.Test;

import static org.junit.Assert.*;

public class SMILParserTest {

    @Test
    public void plainSeconds() {
        assertEquals(1000L, SMILParser.parseClock("1s", -1L));
        assertEquals(500L, SMILParser.parseClock("0.5s", -1L));
    }

    @Test
    public void milliseconds() {
        assertEquals(250L, SMILParser.parseClock("250ms", -1L));
    }

    @Test
    public void minutes() {
        assertEquals(60000L, SMILParser.parseClock("1min", -1L));
    }

    @Test
    public void hours() {
        assertEquals(3600000L, SMILParser.parseClock("1h", -1L));
    }

    @Test
    public void rawNumberIsSeconds() {
        assertEquals(2000L, SMILParser.parseClock("2", -1L));
    }

    @Test
    public void clockColonForm() {
        // 1:30 = 1 minute 30 seconds = 90000 ms
        assertEquals(90000L, SMILParser.parseClock("1:30", -1L));
    }

    @Test
    public void indefiniteRepeats() {
        assertEquals(SVGAnimation.REPEAT_INDEFINITE, SMILParser.parseRepeatCount("indefinite"));
    }

    @Test
    public void integerRepeats() {
        assertEquals(3, SMILParser.parseRepeatCount("3"));
        assertEquals(1, SMILParser.parseRepeatCount(null));
        assertEquals(1, SMILParser.parseRepeatCount("notanumber"));
    }

    @Test
    public void transformTypes() {
        assertEquals(SVGAnimation.TransformType.ROTATE, SMILParser.parseTransformType("rotate"));
        assertEquals(SVGAnimation.TransformType.SCALE, SMILParser.parseTransformType("scale"));
        assertEquals(SVGAnimation.TransformType.TRANSLATE, SMILParser.parseTransformType("translate"));
        assertEquals(SVGAnimation.TransformType.TRANSLATE, SMILParser.parseTransformType(null));
    }
}
