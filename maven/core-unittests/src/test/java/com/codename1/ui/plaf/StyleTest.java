package com.codename1.ui.plaf;

import com.codename1.ui.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StyleTest {
    @Test
    public void testSetMarginUnitLeft() {
        Style s = new Style();
        s.setMarginUnitLeft(Style.UNIT_TYPE_PIXELS);
        assertEquals(Style.UNIT_TYPE_PIXELS, s.getMarginUnit()[Component.LEFT]);
        s.setMarginUnitLeft(Style.UNIT_TYPE_DIPS);
        assertEquals(Style.UNIT_TYPE_DIPS, s.getMarginUnit()[Component.LEFT]);
    }

    @Test
    public void testSetMarginUnitRight() {
        Style s = new Style();
        s.setMarginUnitRight(Style.UNIT_TYPE_PIXELS);
        assertEquals(Style.UNIT_TYPE_PIXELS, s.getMarginUnit()[Component.RIGHT]);
        s.setMarginUnitRight(Style.UNIT_TYPE_DIPS);
        assertEquals(Style.UNIT_TYPE_DIPS, s.getMarginUnit()[Component.RIGHT]);
    }

    @Test
    public void testSetMarginUnitBottom() {
        Style s = new Style();
        s.setMarginUnitBottom(Style.UNIT_TYPE_PIXELS);
        assertEquals(Style.UNIT_TYPE_PIXELS, s.getMarginUnit()[Component.BOTTOM]);
        s.setMarginUnitBottom(Style.UNIT_TYPE_DIPS);
        assertEquals(Style.UNIT_TYPE_DIPS, s.getMarginUnit()[Component.BOTTOM]);
    }

    @Test
    public void testSetMarginUnitTop() {
        Style s = new Style();
        s.setMarginUnitTop(Style.UNIT_TYPE_PIXELS);
        assertEquals(Style.UNIT_TYPE_PIXELS, s.getMarginUnit()[Component.TOP]);
        s.setMarginUnitTop(Style.UNIT_TYPE_DIPS);
        assertEquals(Style.UNIT_TYPE_DIPS, s.getMarginUnit()[Component.TOP]);
    }

    @Test
    public void testSetPaddingUnitTop() {
        Style s = new Style();
        s.setPaddingUnitTop(Style.UNIT_TYPE_PIXELS);
        assertEquals(Style.UNIT_TYPE_PIXELS, s.getPaddingUnit()[Component.TOP]);
        s.setPaddingUnitTop(Style.UNIT_TYPE_DIPS);
        assertEquals(Style.UNIT_TYPE_DIPS, s.getPaddingUnit()[Component.TOP]);
    }

    @Test
    public void testSetPaddingUnitBottom() {
        Style s = new Style();
        s.setPaddingUnitBottom(Style.UNIT_TYPE_PIXELS);
        assertEquals(Style.UNIT_TYPE_PIXELS, s.getPaddingUnit()[Component.BOTTOM]);
        s.setPaddingUnitBottom(Style.UNIT_TYPE_DIPS);
        assertEquals(Style.UNIT_TYPE_DIPS, s.getPaddingUnit()[Component.BOTTOM]);
    }

    @Test
    public void testSetPaddingUnitLeft() {
        Style s = new Style();
        s.setPaddingUnitLeft(Style.UNIT_TYPE_PIXELS);
        assertEquals(Style.UNIT_TYPE_PIXELS, s.getPaddingUnit()[Component.LEFT]);
        s.setPaddingUnitLeft(Style.UNIT_TYPE_DIPS);
        assertEquals(Style.UNIT_TYPE_DIPS, s.getPaddingUnit()[Component.LEFT]);
    }

    @Test
    public void testSetPaddingUnitRight() {
        Style s = new Style();
        s.setPaddingUnitRight(Style.UNIT_TYPE_PIXELS);
        assertEquals(Style.UNIT_TYPE_PIXELS, s.getPaddingUnit()[Component.RIGHT]);
        s.setPaddingUnitRight(Style.UNIT_TYPE_DIPS);
        assertEquals(Style.UNIT_TYPE_DIPS, s.getPaddingUnit()[Component.RIGHT]);
    }

}
