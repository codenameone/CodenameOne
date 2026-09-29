/*
 * Copyright (c) 2018, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package java.util.regex;

public class Matcher implements MatchResult {
    Matcher() {
        
    }
    public Matcher appendReplacement(StringBuffer sb, String replacement) {
        return null;
    }
    
    public StringBuffer appendTail(StringBuffer sb) {
        return null;
    }
    
    public int end() {
        return 0;
    }
    
    public int end(int group) {
        return 0;
    }
    
    public boolean find() {
        return false;
    }
    
    public boolean find(int start) {
        return false;
    }
    
    public String group() {
        return null;
    }
    
    public String group(int group) {
        return null;
    }
    
    public String group(String name) {
        return null;
    }
    
    public int groupCount() {
        return 0;
    }
    
    public boolean hasAnchoringBounds() {
        return false;
    }
    
    public boolean hasTransparentBounds() {
        return false;
    }
    
    public boolean hitEnd() {
        return false;
    }
    
    public boolean lookingAt() {
        return false;
    }
    public boolean matches() {
        return false;
    }
    
    public Pattern pattern() {
        return null;
    }
    
    public String quoteReplacement(String s) {
        return null;
    }
    
    public Matcher region(int start, int end) {
        return null;
    }
    public int regionEnd() {
        return 0;
    }
    
    public int regionStart() {
        return 0;
    }
    
    public String replaceAll(String replacement) {
        return null;
    }
    
    public String replaceFirst(String replacement) {
        return null;
    }
    
    public boolean requireEnd() {
        return false;
    }
    
    public Matcher reset() {
        return null;
    }
    
    public Matcher reset(CharSequence input) {
        return null;
    }
    
    public int start() {
        return 0;
    }
    
    public int start(int group) {
        return 0;
    }
    
    public MatchResult toMatchResult() {
        return null;
    }
    
    public String toString() {
        return null;
    }
    
    public Matcher useAnchoringBounds(boolean b) {
        return null;
    }
    
    public Matcher usePattern(Pattern newPattern) {
        return null;
    }
    
    public Matcher useTransparentBounds(boolean b) {
        return null;
    }
    
    
}
