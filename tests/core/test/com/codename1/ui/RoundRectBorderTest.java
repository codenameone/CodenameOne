/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.ui;

import com.codename1.testing.AbstractTest;
import com.codename1.ui.plaf.RoundRectBorder;
import com.codename1.ui.plaf.Style;

/**
 *
 * @author shannah
 */
public class RoundRectBorderTest extends AbstractTest {

    @Override
    public boolean shouldExecuteOnEDT() {
        return true;
    }

    @Override
    public boolean runTest() throws Exception {
        RoundRectBorder border = RoundRectBorder.create();
        border.cornerRadius(5f);
        assertTrue(Math.abs(border.getCornerRadius() - 5f) < 0.00001, "Setting corner radius has no effect");
        
        border.bezierCorners(false);
        assertTrue(!border.isBezierCorners(), "Setting bezier corners to false failed.");
        border.bezierCorners(true);
        assertTrue(border.isBezierCorners(), "Setting bezier corners to true failed.");
        
        assertNotEqual(RoundRectBorder.create(), RoundRectBorder.create(), "Round rect borders should only be equal with self");
//        assertNotEqual(RoundRectBorder.create().topLeftMode(true), RoundRectBorder.create().topLeftMode(false), "RoundRectBorder should be unequal if topLeftMode not equal");
//        assertNotEqual(RoundRectBorder.create().topRightMode(true), RoundRectBorder.create().topRightMode(false), "RoundRectBorder should be unequal if topRightMode not equal");
//        assertNotEqual(RoundRectBorder.create().bottomRightMode(true), RoundRectBorder.create().bottomRightMode(false), "RoundRectBorder should be unequal if bottomRightMode not equal");
//        assertNotEqual(RoundRectBorder.create().bottomLeftMode(true), RoundRectBorder.create().bottomLeftMode(false), "RoundRectBorder should be unequal if bottomLeftMode not equal");
//        assertNotEqual(RoundRectBorder.create().cornerRadius(2), RoundRectBorder.create().cornerRadius(3), "RoundRectBorder should be unequal if cornerRadius not equal");
//        
        Style s = new Style();
        RoundRectBorder b1 = RoundRectBorder.create();
        s.setBorder(b1);
        assertTrue(b1 == s.getBorder(), "Failed to set RoundRectBorder on style");
        
        RoundRectBorder b2 = RoundRectBorder.create();
        s.setBorder(b2);
            // This shouldn't actually change the border because b1.equals(b2)
        assertTrue(b2 == s.getBorder(), "Failed to change border");
        b2.cornerRadius(b2.getCornerRadius()+1);
        s.setBorder(b2);
        assertTrue(b2 == s.getBorder(), "Border should have been changed to new RoundRectBorder because the border was changed.");
        
        return true;
        
    }
    
}
