/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.tools.translator.bytecodes;

import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.Opcodes;


/**
 *
 * @author shannah
 */
public class ArithmeticExpression extends Instruction implements AssignableExpression{

    public static final int OPCODE=-2;
    private ArithmeticExpression subExpression;
    private ArithmeticExpression subExpression2;
    private Instruction lastInstruction;
    private ArithmeticExpression() {
        super(OPCODE);
        
    }

    @Override
    public void addDependencies(List<String> dependencyList) {
        if (subExpression != null) {
            subExpression.addDependencies(dependencyList);
        }
        if (subExpression2 != null) {
            subExpression2.addDependencies(dependencyList);
        }
        if (lastInstruction != null) {
            lastInstruction.addDependencies(dependencyList);
        }
    }

    
    
    @Override
    public void appendInstruction(StringBuilder b) {
        if (subExpression != null) {
            subExpression.appendInstruction(b);
        }
        if (subExpression2 != null) {
            subExpression2.appendInstruction(b);
        }
        
        if (lastInstruction != null) {
            lastInstruction.appendInstruction(b);
        }
    }

    @Override
    public void appendInstruction(StringBuilder b, List<Instruction> l) {
        if (subExpression != null) {
            subExpression.appendInstruction(b, l);
        }
        if (subExpression2 != null) {
            subExpression2.appendInstruction(b, l);
        }
        
        if (lastInstruction != null) {
            lastInstruction.appendInstruction(b, l);
        }
    }
    
    
    
   
    
    
    public static int tryReduce(List<Instruction> instructions, int index) {
        Instruction instr = instructions.get(index);
        if (isArithmeticOp(instr)) {
            Instruction op = instr;
            if (isBinaryOp(op)) {
                if (index < 2) {
                    return -1;
                }
                Instruction arg1 = instructions.get(index-2);
                Instruction arg2 = instructions.get(index-1);
                
                if (!isArg(arg1) || !isArg(arg2)) {
                    return -1;
                }
                
                ArithmeticExpression expr = new ArithmeticExpression();
                expr.lastInstruction = instr;
                if (arg1 instanceof ArithmeticExpression) {
                    expr.subExpression = (ArithmeticExpression)arg1;
                } else {
                    expr.subExpression = new ArithmeticExpression();
                    expr.subExpression.lastInstruction = arg1;
                }
                if (arg2 instanceof ArithmeticExpression) {
                    expr.subExpression2 = (ArithmeticExpression)arg2;
                } else {
                    expr.subExpression2 = new ArithmeticExpression();
                    expr.subExpression2.lastInstruction = arg2;
                }
                
                instructions.remove(index-2);
                instructions.remove(index-2);
                instructions.remove(index-2);
                instructions.add(index-2, expr);
                return index-2;
            } else {
                if (index < 1) {
                    return -1;
                }
                
                Instruction arg = instructions.get(index-1);
                if (!isArg(arg)) {
                    return -1;
                }
                
                ArithmeticExpression expr = new ArithmeticExpression();
                expr.lastInstruction = instr;
                if (arg instanceof ArithmeticExpression) {
                    expr.subExpression = (ArithmeticExpression)arg;
                    
                } else {
                    expr.subExpression = new ArithmeticExpression();
                    expr.subExpression.lastInstruction = arg;
                }
                
                instructions.remove(index-1);
                instructions.remove(index-1);
                instructions.add(index-1, expr);
                return index-1;
                
            }
            
        }
        return -1;
    }
    
    public static boolean isArg(Instruction instr) {
        if (instr instanceof ArithmeticExpression) {
            return true;
        }
        if (instr instanceof AssignableExpression) {
            StringBuilder dummy = new StringBuilder();
            
            if (((AssignableExpression)instr).assignTo(null, dummy)) {
                return true;
            }
        }
        int opcode = instr.getOpcode();
        switch (opcode) {
            case Opcodes.FLOAD:
            case Opcodes.DLOAD:
            case Opcodes.ILOAD:
            case Opcodes.LLOAD:
            case org.objectweb.asm.Opcodes.ICONST_0:
            case org.objectweb.asm.Opcodes.ICONST_1: 
            case org.objectweb.asm.Opcodes.ICONST_2:
            case org.objectweb.asm.Opcodes.ICONST_3: 
            case org.objectweb.asm.Opcodes.ICONST_4: 
            case org.objectweb.asm.Opcodes.ICONST_5:
            case org.objectweb.asm.Opcodes.ICONST_M1:
            case org.objectweb.asm.Opcodes.LCONST_0:
            case org.objectweb.asm.Opcodes.LCONST_1: 
            case org.objectweb.asm.Opcodes.BIPUSH:
            case org.objectweb.asm.Opcodes.SIPUSH:
            case Opcodes.LDC:
                return true;
        }
        return false;
    }

    public static boolean isBinaryOp(Instruction instr) {
        switch (instr.getOpcode()) {
            case Opcodes.IOR:
            case Opcodes.LOR:
            case Opcodes.IXOR:
            case Opcodes.LXOR:
            case Opcodes.IAND:
            case Opcodes.LAND:
            case Opcodes.FADD:
            case Opcodes.DADD:
            case Opcodes.IADD:
            case Opcodes.LADD:
            case Opcodes.FSUB:
            case Opcodes.DSUB:
            case Opcodes.ISUB:
            case Opcodes.LSUB:
            case Opcodes.FDIV:
            case Opcodes.DDIV:
            case Opcodes.LDIV:
            case Opcodes.IDIV:
            case Opcodes.IREM:
            case Opcodes.FREM:
            case Opcodes.DREM:
            case Opcodes.LREM:
                return true;
        }
        return false;
    }

    @Override
    public boolean isOptimized() {
        return true;
    }

    
    public static boolean isArithmeticOp(Instruction instr) {
        
        switch (instr.getOpcode()) {
            case Opcodes.FNEG:
            case Opcodes.DNEG:
            case Opcodes.INEG:
            case Opcodes.D2F:
            case Opcodes.D2I:
            case Opcodes.D2L:
            case Opcodes.F2D:
            case Opcodes.F2I:
            case Opcodes.F2L:
            case Opcodes.L2D:
            case Opcodes.L2F:
            case Opcodes.L2I:
            case Opcodes.I2L:
            case Opcodes.I2B:
            case Opcodes.I2C:
            case Opcodes.I2D:
            case Opcodes.I2F:
            case Opcodes.I2S:
            case Opcodes.IOR:
            case Opcodes.LOR:
            case Opcodes.IXOR:
            case Opcodes.LXOR:
            case Opcodes.IAND:
            case Opcodes.LAND:
            case Opcodes.FADD:
            case Opcodes.DADD:
            case Opcodes.IADD:
            case Opcodes.LADD:
            case Opcodes.FSUB:
            case Opcodes.DSUB:
            case Opcodes.ISUB:
            case Opcodes.LSUB:
            case Opcodes.FDIV:
            case Opcodes.DDIV:
            case Opcodes.LDIV:
            case Opcodes.IDIV:
            case Opcodes.IREM:
            case Opcodes.FREM:
            case Opcodes.DREM:
            case Opcodes.LREM:
                return true;
        }
        return false;
    }
    
    
    
    
    public String getExpressionAsString() {
        
        Instruction instr = lastInstruction;
        int opcode = lastInstruction.getOpcode();
        if (subExpression == null) {
            // This is the root of it... probably an FLOAD
            if (lastInstruction instanceof AssignableExpression && !(lastInstruction instanceof ArithmeticExpression)) {
                StringBuilder out = new StringBuilder();
                if (((AssignableExpression)lastInstruction).assignTo(null, out)) {
                    String strOut = out.toString();
                    if (strOut.trim().isEmpty()) {
                        throw new RuntimeException("Instruction produces blank string output: "+lastInstruction);
                    }
                    if (strOut == null || "null".equals(strOut)) {
                        throw new RuntimeException("ArithmeticExpression produced null value.  This shouldn't happen: "+lastInstruction);
                    }
                    return strOut;
                }
            }
            
            if (lastInstruction instanceof VarOp) {
                VarOp var = (VarOp)lastInstruction;
                switch (opcode) {
                    case Opcodes.FLOAD: {
                        return "flocals_"+var.getIndex()+"_";
                        
                    }
                    case Opcodes.DLOAD: {
                        return "dlocals_"+var.getIndex()+"_";
                    }
                    
                    case Opcodes.ILOAD: {
                        return "ilocals_"+var.getIndex()+"_";
                    }
                    
                    case Opcodes.LLOAD: {
                        return "llocals_"+var.getIndex()+"_";
                    }
                 
                    case org.objectweb.asm.Opcodes.ICONST_0: {
                        return "0";
                        
                    }
                    case org.objectweb.asm.Opcodes.ICONST_1: {
                        return "1";
                        
                    }
                    case org.objectweb.asm.Opcodes.ICONST_2: {
                        return "2";
                        
                    }
                    case org.objectweb.asm.Opcodes.ICONST_3: {
                        return "3";
                        
                    }
                    case org.objectweb.asm.Opcodes.ICONST_4: {
                        return "4";
                        
                    }
                    case org.objectweb.asm.Opcodes.ICONST_5: {
                        return "5";
                        
                    }
                    case org.objectweb.asm.Opcodes.ICONST_M1: {
                        return "(-1)";
                        
                    }
                    case org.objectweb.asm.Opcodes.LCONST_0: {
                        return "((JAVA_LONG)0)";
                        
                    }
                    case org.objectweb.asm.Opcodes.LCONST_1: {
                        return "((JAVA_LONG)1)";
                        
                    }
                    case org.objectweb.asm.Opcodes.BIPUSH:
                    case org.objectweb.asm.Opcodes.SIPUSH: {
                        return String.valueOf(var.getIndex());
                    }
                    default: {
                        throw new RuntimeException("Unsupported Opcode in ArithmeticExpression: "+opcode+" "+var);
                    }
                }
            } else {
                switch (instr.getOpcode()) {

                   

                    case org.objectweb.asm.Opcodes.ICONST_0: {
                        return "0";
                        
                    }
                    case org.objectweb.asm.Opcodes.ICONST_1: {
                        return "1";
                        
                    }
                    case org.objectweb.asm.Opcodes.ICONST_2: {
                        return "2";
                        
                    }
                    case org.objectweb.asm.Opcodes.ICONST_3: {
                        return "3";
                        
                    }
                    case org.objectweb.asm.Opcodes.ICONST_4: {
                        return "4";
                        
                    }
                    case org.objectweb.asm.Opcodes.ICONST_5: {
                        return "5";
                        
                    }
                    case org.objectweb.asm.Opcodes.ICONST_M1: {
                        return "(-1)";
                        
                    }
                    case org.objectweb.asm.Opcodes.LCONST_0: {
                        return "((JAVA_LONG)0)";
                        
                    }
                    case org.objectweb.asm.Opcodes.LCONST_1: {
                        return "(JAVA_LONG)1";
                        
                    }
                    case org.objectweb.asm.Opcodes.BIPUSH: {
                        if (instr instanceof BasicInstruction) {
                            return String.valueOf(((BasicInstruction) instr).getValue());
                        }
                        break;
                    }
                    case org.objectweb.asm.Opcodes.LDC: {
                        if (instr instanceof Ldc) {
                            Ldc ldc = (Ldc) instr;
                            return ldc.getValueAsString();

                        }
                        break;
                    }
                    default: {
                        throw new RuntimeException("Unsupported Opcode in ArithmeticExpression: "+opcode+" "+instr);
                    }

                }
            }

        } else {

            switch (opcode) {
                case Opcodes.D2F: {
                    return "((JAVA_FLOAT)" + subExpression.getExpressionAsString().trim() + ")";
                }
                case Opcodes.F2D: {
                    return subExpression.getExpressionAsString().trim();
                }
                case Opcodes.F2I: {
                    return "((JAVA_INT)" + subExpression.getExpressionAsString().trim() + ")";
                }
                case Opcodes.F2L: {
                    return "((JAVA_LONG)" + subExpression.getExpressionAsString().trim() + ")";
                }
                case Opcodes.D2I: {
                    return "((JAVA_INT)" + subExpression.getExpressionAsString().trim() + ")";
                }
                case Opcodes.D2L: {
                    return "((JAVA_LONG)" + subExpression.getExpressionAsString().trim() + ")";
                }
                case Opcodes.I2B: {
                    return "(("+subExpression.getExpressionAsString()+" << 24) >> 24)";
                }
                case Opcodes.I2C: {
                    return "("+subExpression.getExpressionAsString().trim()+" & 0xffff)";
                }
                case Opcodes.I2D: {
                    return "((JAVA_DOUBLE)"+subExpression.getExpressionAsString().trim()+")";
                }
                case Opcodes.I2F: {
                    return "((JAVA_FLOAT)"+subExpression.getExpressionAsString().trim()+")";
                }
                case Opcodes.I2L: {
                    return "((JAVA_LONG)"+subExpression.getExpressionAsString().trim()+")";
                }
                case Opcodes.I2S: {
                    return "(("+subExpression.getExpressionAsString().trim()+" << 16) >> 16)";
                }
                case Opcodes.L2D: {
                    return "((JAVA_DOUBLE)"+subExpression.getExpressionAsString().trim()+")";
                }
                case Opcodes.L2F: {
                    return "((JAVA_FLOAT)"+subExpression.getExpressionAsString().trim()+")";
                }
                case Opcodes.L2I: {
                    return "((JAVA_INT)"+subExpression.getExpressionAsString().trim()+")";
                }
                case Opcodes.IAND:
                case Opcodes.LAND: {
                    return "(" + subExpression.getExpressionAsString().trim() + " & " + subExpression2.getExpressionAsString().trim() + ")";
                    
                }
                case Opcodes.IOR:
                case Opcodes.LOR: {
                    return "(" + subExpression.getExpressionAsString().trim() + " | " + subExpression2.getExpressionAsString().trim() + ")";
                }
                case Opcodes.IXOR:
                case Opcodes.LXOR: {
                    return "(" + subExpression.getExpressionAsString().trim() + " ^ " + subExpression2.getExpressionAsString().trim() + ")";

                }
                case Opcodes.DADD:
                case Opcodes.IADD:
                case Opcodes.LADD:
                case Opcodes.FADD: {
                    return "(" + subExpression.getExpressionAsString().trim() + " + " + subExpression2.getExpressionAsString().trim() + ")";
                }

                case Opcodes.DSUB:
                case Opcodes.ISUB:
                case Opcodes.LSUB:
                case Opcodes.FSUB: {
                    return "(" + subExpression.getExpressionAsString().trim() + " - " + subExpression2.getExpressionAsString().trim() + ")";
                }

                case Opcodes.DMUL:
                case Opcodes.IMUL:
                case Opcodes.LMUL:
                case Opcodes.FMUL: {
                    return "("+subExpression.getExpressionAsString().trim() + " * " + subExpression2.getExpressionAsString().trim()+")";
                }

                case Opcodes.DDIV:
                case Opcodes.IDIV:
                case Opcodes.LDIV:
                case Opcodes.FDIV: {
                    return "("+subExpression.getExpressionAsString().trim() + " / " + subExpression2.getExpressionAsString().trim()+")";
                }

                case Opcodes.FREM:
                case Opcodes.DREM: {
                    return "fmod("+subExpression.getExpressionAsString().trim() + ", "+subExpression2.getExpressionAsString().trim()+")";
                }

                case Opcodes.LREM:
                case Opcodes.IREM: {
                    if (subExpression2.getExpressionAsString() == null || "null".equals(subExpression2.getExpressionAsString())) {
                        throw new RuntimeException("2nd param of REM is null.  Should never happen.  Expression is "+subExpression2+" with last instruction "+subExpression2.lastInstruction);
                    }
                    return "("+subExpression.getExpressionAsString().trim() + " % "+subExpression2.getExpressionAsString().trim()+")";
                }

                case Opcodes.FNEG:
                case Opcodes.INEG:
                case Opcodes.LNEG:
                case Opcodes.DNEG:
                    return "(-("+subExpression.getExpressionAsString().trim()+"))";
                    
                default: {
                    throw new RuntimeException("Unsupported Opcode in ArithmeticExpression: "+opcode+" "+instr);
                }

            }
        }
        throw new RuntimeException("Did not return a value in getExpressionAsString() with lastInstruction "+lastInstruction+" subExpression "+subExpression+" and subExpression2 "+subExpression2);
    }

    @Override
    public boolean assignTo(String varName, StringBuilder sb) {
        if (varName != null) {
            sb.append(varName).append("=");
        }
        sb.append(getExpressionAsString());
        if (varName != null) {
            sb.append(";\n");
        }
        return true;
    }
    
}
