/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.util;

import com.codename1.testing.AbstractTest;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 *
 * @author shannah
 */
public class StringUtilTests extends AbstractTest {

    @Override
    public boolean runTest() throws Exception {
        String testStr = "1,2,3,,,,,,5,6,3";
        String expected = "[1, 2, 3, 5, 6, 3]";
        //StringTokenizer strtok = new StringTokenizer(testStr, ",");
        //List<String> toks = new ArrayList<>();
        //while (strtok.hasMoreTokens()) {
        //    toks.add(strtok.nextToken());
        //}

        List<String> toks2 = StringUtil.tokenize(testStr, ",");
        assertEqual(expected, toks2.toString());
        return true;
    }
    
}
