/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

package com.codename1.tools.translator.bytecodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

/**
 *
 * @author Shai Almog
 */
public class LabelInstruction extends Instruction {
    private Label parent;
    static class Pair {
        String cls;
        int counter;
        Pair(String cls, int counter) {
            this.cls = cls; this.counter = counter;
        }
    }
    private static Map<Label, List<Pair>> tryBeginLabels = new HashMap<Label, List<Pair>>();
    private static Map<Label, Integer> tryEndLabels = new HashMap<Label, Integer>();
    private static Map<Label, Integer> labelCatchDepth = new HashMap<Label, Integer>();
    private static Set<String> usedLabels = new TreeSet<String>();
    
    public LabelInstruction(org.objectweb.asm.Label parent) {
        super(-1);
        this.parent = parent;
    }
    
    public static int getLabelCatchDepth(Label l, List<Instruction> inst) {
        Integer i = labelCatchDepth.get(l);
        if(i == null) {
            int counter = 0;
            for(Instruction is : inst) {
                if(is instanceof LabelInstruction) {
                    LabelInstruction ll = (LabelInstruction)is;
                    if(ll.parent == l) {
                        break;
                    }
                    List<Pair> list = tryBeginLabels.get(ll.parent);
                    if(list != null) {
                        counter += list.size();
                    }
                    Integer end = tryEndLabels.get(ll.parent);
                    if(end != null) {
                        counter -= end.intValue();
                    }
                }
            }
            i = Integer.valueOf(counter);
            labelCatchDepth.put(l, i);
        }
        return i.intValue();
    }
    
    public static void addTryBeginLabel(Label l, String exception, int counter) {
        List<Pair> ll = tryBeginLabels.get(l);
        if(ll == null) {
            ll = new ArrayList<Pair>();
            tryBeginLabels.put(l, ll);
            labelIsUsed(l);
        }
        ll.add(new Pair(exception, counter));
    }
    
    public static void addTryEndLabel(Label l) {
        Integer i = tryEndLabels.get(l);
        if(i == null) {
            tryEndLabels.put(l, Integer.valueOf(1));
        } else {
            tryEndLabels.put(l, Integer.valueOf(i.intValue() + 1));
        }
        labelIsUsed(l);
    }
    
    public static void labelIsUsed(Label l) {
        String s = l.toString();
        if(!usedLabels.contains(s)) {
            usedLabels.add(s);
        }
    }
        
    @Override
    public void appendInstruction(StringBuilder b) {
        if(!usedLabels.contains(parent.toString())) {
            return;
        }
        b.append("\nlabel_"); 
        b.append(parent); 
        Integer tryCount = tryEndLabels.get(parent);
        if(tryCount != null) {
            int v = tryCount.intValue();
            v--;
            //b.append(": END_TRY(); NSLog(@\"End try on:  %s %d off: %i\\n\", __FILE__, __LINE__, getThreadLocalData()->tryBlockOffset);");
            b.append(": END_TRY();");
            while(v > 0) {
                //b.append(" END_TRY(); NSLog(@\"End try on:  %s %d off: %i\\n\", __FILE__, __LINE__, getThreadLocalData()->tryBlockOffset);");
                b.append(" END_TRY();");
                v--;
            }
            b.append("\n");
        } else {
            List<Pair> strs = tryBeginLabels.get(parent);
            if(strs != null) {
                b.append(":");
                for(int iter = strs.size() - 1;  iter >= 0 ; iter--) {
                    Pair s = strs.get(iter);
                    b.append(" tryBlockOffset");
                    b.append(parent);
                    b.append(s.cls);
                    b.append(s.counter);
                    b.append(" = threadStateData->tryBlockOffset;\n");
                    b.append("    BEGIN_TRY(");
                    b.append(s.cls);
                    b.append(", catch_");
                    b.append(parent);
                    b.append(s.cls);
                    b.append(s.counter);
                    //b.append("); NSLog(@\"Begin try on:  %s %d off: %i\\n\", __FILE__, __LINE__, getThreadLocalData()->tryBlockOffset);");
                    b.append(");\n    restoreTo");
                    b.append(parent);
                    b.append(s.cls);
                    b.append(s.counter);
                    b.append(" = threadStateData->threadObjectStackOffset;\n");
                }
                b.append("\n");
            } else {
                b.append(":\n"); 
            }
        }
    }

}
