/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.ui;

import com.codename1.io.Log;
import com.codename1.l10n.SimpleDateFormat;
import com.codename1.testing.TestUtils;

/**
 *
 * @author shannah
 */
public class TestComponent2 extends TestComponent {

    @Override
    public boolean runTest() throws Exception {
        super.runTest();
        testSimpleDateFormat();
                
                return true;
    }
    
    
    
    
     public void testSimpleDateFormat() {
        
        try {
            SimpleDateFormat format = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss z");
            format.parse("mon, 20-nov-2017 19:49:58 gmt");
            format = new SimpleDateFormat("EEE dd-MMM-yyyy HH:mm:ss z");
            format.parse("mon 20-nov-2017 19:49:58 gmt");
            
            format = new SimpleDateFormat("EEE dd-MMM-yyyy HH:mm:ss z");
            java.util.Date dt = format.parse("tue 21-nov-2017 17:04:27 gmt");
            if (Display.getInstance().isSimulator()) {
                java.text.SimpleDateFormat format0 = new java.text.SimpleDateFormat("EEE dd-MMM-yyyy HH:mm:ss z", java.util.Locale.US);
                java.util.Date dt0 = format0.parse("tue 21-nov-2017 17:04:27 gmt");
                TestUtils.assertEqual(dt0, dt, "SimpleDateFormat gives different result than java.text version on tue 21-nov-2017 17:04:27 gmt with format \"EEE, dd-MMM-yyyy HH:mm:ss z\"");
            }
            TestUtils.assertBool(dt.getTime() / 1000L == 1511283867L, "Incorrect date for simple date format parse");
            
            format = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss z");
            com.codename1.l10n.ParseException pex = null;
            try {
                format.parse("tue 21-nov-2017 17:04:27 gmt");
            } catch (com.codename1.l10n.ParseException pex2)
            {
                pex = pex2;
            }
            TestUtils.assertTrue(pex!=null, "Parsing date with wrong format should give parse exception");
            
            format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
            dt = format.parse("sun, 22 nov 2037 13:20:46 -0000");
            //2142508846
            //Log.p("Difference = "+(dt.getTime() - 2142508846000L));
            TestUtils.assertEqual(dt.getTime()/1000L,  2142508846L, "Failed to parse RFC822 datetime.  "+dt);
            
            String dateStr = "sun 22 nov 2037 13:20:46 -0000";
            format = new SimpleDateFormat("EEE dd MMM yyyy HH:mm:ss Z");
            dt = format.parse("sun 22 nov 2037 13:20:46 -0000");
            
            TestUtils.assertEqual(dt.getTime()/1000L,  2142508846L, "Failed to parse RFC822 datetime no comma.  "+dt);
        } catch (Throwable t) {
            Log.e(t);
            throw new RuntimeException("Failed to parse date mon 20-nov-2017 19:49:58 gmt: "+t.getMessage());
            
        }
        
        
        
    }
}
