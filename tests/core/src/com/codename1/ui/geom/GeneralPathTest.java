/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.ui.geom;

import com.codename1.testing.AbstractTest;
import com.codename1.ui.Transform;

/**
 *
 * @author shannah
 */
public class GeneralPathTest extends AbstractTest {

    @Override
    public boolean shouldExecuteOnEDT() {
        return true;
    }

    @Override
    public boolean runTest() throws Exception {
        GeneralPath gp = new GeneralPath();
        Rectangle bounds = gp.getBounds();
        assertEqual(0, bounds.getWidth());
        assertEqual(0, bounds.getHeight());
        assertEqual(0, bounds.getX());
        assertEqual(0, bounds.getY());
        assertTrue(!gp.isRectangle());
        
        gp.arc(100, 200, 300, 400, 0, Math.PI*2);
        bounds = gp.getBounds();
        assertEqual(300, bounds.getWidth());
        assertEqual(400, bounds.getHeight());
        assertEqual(100, bounds.getX());
        assertEqual(200, bounds.getY());
        assertTrue(!gp.isRectangle());
        
        GeneralPath gp2 = new GeneralPath(gp);
        bounds = gp2.getBounds();
        assertEqual(300, bounds.getWidth());
        assertEqual(400, bounds.getHeight());
        assertEqual(100, bounds.getX());
        assertEqual(200, bounds.getY());
        assertTrue(!gp.isRectangle());
        
        GeneralPath gp3 = new GeneralPath(gp2);
        //assertEqual(300, gp3.getWidth());
        //assertEqual(400, gp3.getHeight());
        //assertEqual(100, gp3.getX());
        //assertEqual(200, gp3.getY());
        assertTrue(!gp.isRectangle());
        
        GeneralPath gp4 = new GeneralPath();
        gp4.append(gp3, true);
        //assertEqual(300, gp4.getWidth());
        //assertEqual(400, gp4.getHeight());
        //assertEqual(100, gp4.getX());
        //assertEqual(200, gp4.getY());
        assertTrue(!gp.isRectangle());
        
        GeneralPath gp5 = new GeneralPath();
        gp5.setRect(new Rectangle(0, 0, 100, 100), null);
        //assertEqual(0, gp5.getX());
        //assertEqual(0, gp5.getY());
        //assertEqual(100, gp5.getWidth());
        //assertEqual(100, gp5.getHeight());
        assertTrue(gp5.isRectangle());
        
        GeneralPath gp6 = new GeneralPath();
        gp6.setPath(gp5, Transform.makeScale(2, 2));
        //assertEqual(0, gp6.getX());
        //assertEqual(0, gp6.getY());
        //assertEqual(200, gp6.getWidth());
        //assertEqual(200, gp6.getHeight());
        assertTrue(gp6.isRectangle());
        
        GeneralPath gp7 = new GeneralPath();
        gp7.setPath(gp5, Transform.makeRotation((float)Math.PI/4f, 0, 0));
        assertTrue(!gp7.isRectangle());
        
        gp7.setRect(new Rectangle(500, 500, 100, 200), Transform.makeTranslation(-500, -500));
        //assertEqual(0, gp7.getX());
        //assertEqual(0, gp7.getY());
        //assertEqual(100, gp7.getWidth());
        //assertEqual(200, gp7.getHeight());
        assertTrue(gp7.isRectangle());
        
        GeneralPath gp8 = new GeneralPath();
        gp8.arc(100, 200, 300, 400, 0, Math.PI*2);
        
        /*
        GeneralPath gp9 = new GeneralPath();
        gp9.setPath(gp8, Transform.makeIdentity());
        assertTrue(gp8.isTranslationOf(gp9, 0.01f));
        
        GeneralPath gp10 = new GeneralPath();
        gp10.setPath(gp8, Transform.makeTranslation(10, 10));
        assertTrue(gp8.isTranslationOf(gp8, 0.001f));
        
        gp10 = new GeneralPath();
        gp10.setPath(gp8, Transform.makeScale(1.5f, 1.5f));
        assertTrue(gp8.isTranslationOf(gp8, 0.001f));
        
        gp10 = new GeneralPath();
        assertTrue(!gp8.isTranslationOf(gp10, 0.001f));
        
        */
        
        
        GeneralPath gp9 = new GeneralPath();
        gp9.setPath(gp7, null);
        //assertEqual(0, gp9.getX());
        //assertEqual(0, gp9.getY());
        //assertEqual(100, gp9.getWidth());
        //assertEqual(200, gp9.getHeight());
        assertTrue(gp9.isRectangle());
        //assertEqual(100, gp7.getWidth());
        gp9.setPath(gp7, Transform.makeScale(2, 2));
        //assertEqual(0, gp9.getX());
        //assertEqual(0, gp9.getY());
        //assertEqual(200, gp9.getWidth());
        //assertEqual(400, gp9.getHeight());
        assertTrue(gp9.isRectangle());
        
        gp7 = new GeneralPath();
        gp7.setPath(gp9, Transform.makeScale(2,2));
        //assertEqual(400, gp7.getWidth());

        //assertEqual(800, gp7.getHeight())  ;
        assertTrue(gp7.isRectangle());
        
        gp9.transform(Transform.makeScale(2, 2));
        //assertEqual(400, gp7.getWidth());
        //assertEqual(400, gp9.getWidth());
        gp9.transform(Transform.makeScale(2,2));
        //assertEqual(400, gp7.getWidth());
        //assertEqual(800, gp9.getWidth());
        
        //GeneralPath.setCongruencyId(gp9, 10);
        //assertEqual(10, GeneralPath.getCongruencyId(gp9));
        //assertEqual(0, GeneralPath.getCongruencyId(gp7));
        
        GeneralPath g10 = new GeneralPath();
        g10.setPath(gp9, null);
        //assertEqual(10, GeneralPath.getCongruencyId(g10));
        g10.transform(Transform.makeTranslation(10, 20));
        //assertEqual(10, GeneralPath.getCongruencyId(g10));
        
        
        
        return true;
    }
    
    
    
}
