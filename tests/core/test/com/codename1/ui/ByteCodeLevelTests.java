/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.ui;

import com.codename1.testing.AbstractTest;
import com.codename1.testing.TestUtils;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author shannah
 */
public class ByteCodeLevelTests extends AbstractTest {

    
    @Override
    public boolean runTest() throws Exception {
        assertTrue("" instanceof Object, "String should be instanceof Object");
        assertTrue(new ArrayList() instanceof java.util.List, "ArrayList should be instanceof List");
        assertTrue(new ArrayList[0] instanceof java.util.List[], "ArrayList[] should be instanceof List");
        assertTrue(!(new java.util.List[0] instanceof ArrayList[]), "List[] should not be instanceof ArrayList[]");
        assertTrue(new String[0] instanceof Object[], "String[] should be instanceof Object[]");
        assertTrue(new int[0] instanceof Object, "int[] should be instanceof Object");
        assertTrue(new String[0] instanceof Object, "String[] should be instanceof Object");
        assertTrue(new int[0] instanceof int[], "int[] should be instanceof int[]");
        assertTrue(new String[0] instanceof String[], "String[] should be instanceof String[]");
        assertTrue(new int[1][1] instanceof Object[], "int[][] should be instanceof Object[]");
        assertTrue(new int[1][1][1] instanceof Object[][], "int[][][] should be instanceof Object[][]");
        
        int[][] a = {{0,0,0},{0,0,0}};
        a[0][1]++;
        TestUtils.assertEqual("[[0, 1, 0], [0, 0, 0]]", Arrays.deepToString(a), "deepToString(int[][]) incorrect result");
        
        return true;

    }

}
