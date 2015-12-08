/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.tools.translator;

import com.codename1.tools.translator.bytecodes.Instruction;
import java.util.LinkedList;
import java.util.List;
import org.objectweb.asm.Opcodes;

/**
 *
 * @author shannah
 */
public class StackOptimizer {
    private class Var {
        String name;
        Type type;
        int index;
        Element value;
        VarCategory category;
        
    }
    
    private enum VarCategory {
        LOCAL,
        TMP
    }
    
    private class VarRef {
        Var var;
        int index;
    }
    
    private enum Type {
        OBJECT,
        FLOAT,
        DOUBLE,
        INT,
        CHAR,
        SHORT,
        BYTE,
        BOOL,
        LONG
    }
    
    private class Element {
        Type type;
        int addr;
        List<VarRef> refs;
        int refCount;
    }
    
    private LinkedList<Element> stack;
    private List<Var> locals;
    private List<Var> tmps;
    
    private List<Instruction> instructions;
    
    public void optimize() {
        int size = instructions.size();
        for (int i=0; i < size; i++) {
            Instruction instr = instructions.get(i);
            Instruction replacement = optimize(instructions.get(i));
            if (replacement != instr) {
                instructions.set(i, replacement);
            }
            
        }
    }
    
    private Var getVarFor(Element el) {
        for (VarRef ref : el.refs) {
            if (ref.index == ref.var.index) {
                return ref.var;
            }
        }
        return null;
    }
    
    
    private Instruction optimize(Instruction instr) {
        switch (instr.getOpcode()) {
            case Opcodes.AALOAD:
                Element index = stack.pop();
                Element array = stack.pop();
                
                
                
                
        }
        return null;
    }
}
