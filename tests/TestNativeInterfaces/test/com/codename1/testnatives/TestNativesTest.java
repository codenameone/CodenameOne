package com.codename1.testnatives;

import com.codename1.system.NativeLookup;
import com.codename1.testing.AbstractTest;
import java.util.Arrays;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author shannah
 */
public class TestNativesTest extends AbstractTest {

    @Override
    public boolean runTest() throws Exception {
        MyNativeInterface ni = (MyNativeInterface)NativeLookup.create(MyNativeInterface.class);
        assertArrayEqual(new byte[]{1, 2, 3, -1}, ni.getBytes());
        assertArrayEqual(new int[]{1, 2, 3, -1}, ni.getInts());
        assertEqual(Arrays.toString(new double[]{1, 2, 3, -1}), Arrays.toString(ni.getDouble()));
        assertArrayEqual(new int[]{3, 2, 1}, ni.setInts(new int[]{3, 2, 1}));
        assertArrayEqual(new byte[]{4, 5, 6}, ni.setBytes(new byte[]{4, 5, 6}));
        assertEqual(Arrays.toString(new double[]{7, 8, 9}), Arrays.toString(ni.setDoubles(new double[]{7, 8, 9})));
        return true;
    }
    
}
