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

public class StringUtilReplaceTests extends AbstractTest {
    @Override
    public boolean runTest() throws Exception {
        check("aabaa", "aa", "a", "abaa", "aba");
        check("aa", "a", "aaa", "aaaa", "aaaaaa");
        check("", "a", "aaa", "", "");
        check("aaba", "a", "a", "aaba", "aaba");
        check("aabaab", "ab", "ba", "abaaab", "abaaba");
        return true;
    }

    private void check(String source, String target, String replacement, String expectedFirst, String expectedAll) {
        assertEqual(expectedFirst, StringUtil.replaceFirst(source, target, replacement));
        assertEqual(expectedAll, StringUtil.replaceAll(source, target, replacement));
    }
}
