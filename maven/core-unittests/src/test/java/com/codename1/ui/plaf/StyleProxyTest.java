package com.codename1.ui.plaf;

import com.codename1.ui.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StyleProxyTest {
    @Test
    public void testSetMarginUnitLeft() {
        Style s = new Style();
        Style proxy = Style.createProxyStyle(s);
        proxy.setMarginUnitLeft(Style.UNIT_TYPE_PIXELS);
        assertEquals(Style.UNIT_TYPE_PIXELS, s.getMarginUnit()[Component.LEFT]);
        proxy.setMarginUnitLeft(Style.UNIT_TYPE_DIPS);
        assertEquals(Style.UNIT_TYPE_DIPS, s.getMarginUnit()[Component.LEFT]);
    }

    @Test
    public void testSetMarginUnitRight() {
        Style s = new Style();
        Style proxy = Style.createProxyStyle(s);
        proxy.setMarginUnitRight(Style.UNIT_TYPE_PIXELS);
        assertEquals(Style.UNIT_TYPE_PIXELS, s.getMarginUnit()[Component.RIGHT]);
        proxy.setMarginUnitRight(Style.UNIT_TYPE_DIPS);
        assertEquals(Style.UNIT_TYPE_DIPS, s.getMarginUnit()[Component.RIGHT]);
    }

    @Test
    public void testSetMarginUnitBottom() {
        Style s = new Style();
        Style proxy = Style.createProxyStyle(s);
        proxy.setMarginUnitBottom(Style.UNIT_TYPE_PIXELS);
        assertEquals(Style.UNIT_TYPE_PIXELS, s.getMarginUnit()[Component.BOTTOM]);
        proxy.setMarginUnitBottom(Style.UNIT_TYPE_DIPS);
        assertEquals(Style.UNIT_TYPE_DIPS, s.getMarginUnit()[Component.BOTTOM]);
    }

    @Test
    public void testSetMarginUnitTop() {
        Style s = new Style();
        Style proxy = Style.createProxyStyle(s);
        proxy.setMarginUnitTop(Style.UNIT_TYPE_PIXELS);
        assertEquals(Style.UNIT_TYPE_PIXELS, s.getMarginUnit()[Component.TOP]);
        proxy.setMarginUnitTop(Style.UNIT_TYPE_DIPS);
        assertEquals(Style.UNIT_TYPE_DIPS, s.getMarginUnit()[Component.TOP]);
    }

    @Test
    public void testSetPaddingUnitTop() {
        Style s = new Style();
        Style proxy = Style.createProxyStyle(s);
        proxy.setPaddingUnitTop(Style.UNIT_TYPE_PIXELS);
        assertEquals(Style.UNIT_TYPE_PIXELS, s.getPaddingUnit()[Component.TOP]);
        proxy.setPaddingUnitTop(Style.UNIT_TYPE_DIPS);
        assertEquals(Style.UNIT_TYPE_DIPS, s.getPaddingUnit()[Component.TOP]);
    }

    @Test
    public void testSetPaddingUnitBottom() {
        Style s = new Style();
        Style proxy = Style.createProxyStyle(s);
        proxy.setPaddingUnitBottom(Style.UNIT_TYPE_PIXELS);
        assertEquals(Style.UNIT_TYPE_PIXELS, s.getPaddingUnit()[Component.BOTTOM]);
        proxy.setPaddingUnitBottom(Style.UNIT_TYPE_DIPS);
        assertEquals(Style.UNIT_TYPE_DIPS, s.getPaddingUnit()[Component.BOTTOM]);
    }

    @Test
    public void testSetPaddingUnitLeft() {
        Style s = new Style();
        Style proxy = Style.createProxyStyle(s);
        proxy.setPaddingUnitLeft(Style.UNIT_TYPE_PIXELS);
        assertEquals(Style.UNIT_TYPE_PIXELS, s.getPaddingUnit()[Component.LEFT]);
        proxy.setPaddingUnitLeft(Style.UNIT_TYPE_DIPS);
        assertEquals(Style.UNIT_TYPE_DIPS, s.getPaddingUnit()[Component.LEFT]);
    }

    @Test
    public void testSetPaddingUnitRight() {
        Style s = new Style();
        Style proxy = Style.createProxyStyle(s);
        proxy.setPaddingUnitRight(Style.UNIT_TYPE_PIXELS);
        assertEquals(Style.UNIT_TYPE_PIXELS, s.getPaddingUnit()[Component.RIGHT]);
        proxy.setPaddingUnitRight(Style.UNIT_TYPE_DIPS);
        assertEquals(Style.UNIT_TYPE_DIPS, s.getPaddingUnit()[Component.RIGHT]);
    }
}
