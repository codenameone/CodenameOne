/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.ui;

import com.codename1.testing.AbstractTest;
import com.codename1.ui.plaf.RoundRectBorder;

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
        return true;
        
    }
    
}
