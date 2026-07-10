/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.tools.translator.bytecodes;

import java.util.List;
import org.objectweb.asm.Opcodes;

/**
 *
 * @author shannah
 */
public class ArrayLoadExpression extends Instruction implements AssignableExpression {
    private Instruction targetArrayInstruction;
    private Instruction indexInstruction;
    private Instruction loadInstruction;

    private ArrayLoadExpression() {
        super(-4);
    }

    public Instruction getTargetArrayInstruction() {
        return targetArrayInstruction;
    }

    public Instruction getIndexInstruction() {
        return indexInstruction;
    }

    public Instruction getLoadInstruction() {
        return loadInstruction;
    }
    
    
    public static int tryReduce(List<Instruction> instructions, int index) {
        Instruction instr = instructions.get(index);
        switch (instr.getOpcode()) {
            case Opcodes.AALOAD:
            case Opcodes.FALOAD:
            case Opcodes.CALOAD:
            case Opcodes.DALOAD:
            case Opcodes.BALOAD:
            case Opcodes.IALOAD:
            case Opcodes.LALOAD:
            case Opcodes.SALOAD:
                break;
            default:
                return -1;
        }
        
        if (index < 2) {
            return -1;
        }
        
        Instruction indexInstr = instructions.get(index-1);
        if (!(indexInstr instanceof AssignableExpression)) {
            return -1;
        }
        
        Instruction arrInstr = instructions.get(index-2);
        if (!(arrInstr instanceof AssignableExpression)) {
            return -1;
        }
        
        ArrayLoadExpression out = new ArrayLoadExpression();
        out.loadInstruction = instr;
        out.indexInstruction = indexInstr;
        out.targetArrayInstruction = arrInstr;
        // Carry the prove-safe flag the BCE pre-pass set on the raw load opcode.
        if (instr.isBoundsSafe()) {
            out.markBoundsSafe();
        }
        
        instructions.remove(index-2);
        instructions.remove(index-2);
        instructions.remove(index-2);
        instructions.add(index-2, out);
        return index-2;
    }
    
    @Override
    public void addDependencies(List<String> dependencyList) {
        if (indexInstruction != null) {
            indexInstruction.addDependencies(dependencyList);
        }
        if (targetArrayInstruction != null) {
            targetArrayInstruction.addDependencies(dependencyList);
        }
        if (loadInstruction != null) {
            loadInstruction.addDependencies(dependencyList);
        }
                
        
    }

    
    
    @Override
    public void appendInstruction(StringBuilder b) {
        if (targetArrayInstruction != null) {
            targetArrayInstruction.appendInstruction(b);
        }
        
        if (indexInstruction != null) {
            indexInstruction.appendInstruction(b);
        }
        
        if (loadInstruction != null) {
            loadInstruction.appendInstruction(b);
        }
    }

    @Override
    public void appendInstruction(StringBuilder b, List<Instruction> l) {
        if (targetArrayInstruction != null) {
            targetArrayInstruction.appendInstruction(b, l);
        }
        
        if (indexInstruction != null) {
            indexInstruction.appendInstruction(b, l);
        }
        
        if (loadInstruction != null) {
            loadInstruction.appendInstruction(b, l);
        }
            
    }

    @Override
    public boolean assignTo(String varName, StringBuilder sb) {
        StringBuilder b = new StringBuilder();

        String arrayExpr;
        String indexExpr;
        if (targetArrayInstruction instanceof AssignableExpression) {
            StringBuilder sb2 = new StringBuilder();
            boolean res = ((AssignableExpression)targetArrayInstruction).assignTo(null, sb2);
            if (!res) {
                return false;
            }
            arrayExpr = sb2.toString().trim();
        } else {
            return false;
        }

        if (indexInstruction instanceof AssignableExpression) {
            StringBuilder sb2 = new StringBuilder();

            boolean res = ((AssignableExpression)indexInstruction).assignTo(null, sb2);
            if (!res) {
                return false;
            }
            indexExpr = sb2.toString().trim();
        } else {
            return false;
        }

        boolean useTemporaries = varName != null && (!isSimpleExpression(arrayExpr) || !isSimpleExpression(indexExpr));
        String arrayType = null;
        String arrayDataType = null;
        switch (loadInstruction.getOpcode()) {
            case Opcodes.FALOAD:
                arrayType = "FLOAT";
                arrayDataType = "JAVA_ARRAY_FLOAT";
                break;
            case Opcodes.DALOAD:
                arrayType = "DOUBLE";
                arrayDataType = "JAVA_ARRAY_DOUBLE";
                break;
            case Opcodes.LALOAD:
                arrayType = "LONG";
                arrayDataType = "JAVA_ARRAY_LONG";
                break;
            case Opcodes.IALOAD:
                arrayType = "INT";
                arrayDataType = "JAVA_ARRAY_INT";
                break;
            case Opcodes.BALOAD:
                arrayType = "BYTE";
                arrayDataType = "JAVA_ARRAY_BYTE";
                break;
            case Opcodes.CALOAD:
                arrayType = "CHAR";
                arrayDataType = "JAVA_ARRAY_CHAR";
                break;
            case Opcodes.AALOAD:
                arrayType = "OBJECT";
                arrayDataType = "JAVA_ARRAY_OBJECT";
                break;
            case Opcodes.SALOAD:
                arrayType = "SHORT";
                arrayDataType = "JAVA_ARRAY_SHORT";
                break;
                
        }
        if (useTemporaries) {
            b.append("{\n");
            b.append("    JAVA_OBJECT __cn1ArrayTmp = ").append(arrayExpr).append(";\n");
            b.append("    JAVA_INT __cn1IndexTmp = ").append(indexExpr).append(";\n");
            if (!isBoundsSafe() && (getMethod() == null || !getMethod().isDisableNullAndArrayBoundsChecks())) {
                b.append("    CHECK_ARRAY_ACCESS_WITH_ARGS(__cn1ArrayTmp, __cn1IndexTmp);\n");
            }
            b.append("    ").append(varName).append(" = ((").append(arrayDataType).append("*) (*(JAVA_ARRAY)__cn1ArrayTmp).data)[__cn1IndexTmp];\n");
            b.append("}\n");
            sb.append(b);
            return true;
        }

        if (varName != null) {
            b.append(varName).append("=");
        }
        b.append("CN1_ARRAY_ELEMENT_");
        b.append(arrayType);
        // Unchecked variant only when our prove-safe BCE pass cleared this access.
        if (isBoundsSafe()) {
            b.append("_NOCHK");
        }
        b.append("(");
        b.append(arrayExpr);
        b.append(", ");
        b.append(indexExpr);
        b.append(")");
        if (varName != null) {
            b.append(";\n");
        }
        
        sb.append(b);
        return true;
    }

    public boolean isObject() {
        return loadInstruction != null && loadInstruction.getOpcode() == Opcodes.AALOAD;
    }

    /**
     * C type of the loaded element (temp variable type) or null when this load
     * shouldn't be used in a fused comparison prelude.
     */
    public String getElementCType() {
        switch (loadInstruction.getOpcode()) {
            case Opcodes.IALOAD: return "JAVA_INT";
            case Opcodes.CALOAD: return "JAVA_CHAR";
            case Opcodes.SALOAD: return "JAVA_SHORT";
            case Opcodes.BALOAD: return "JAVA_BYTE";
            case Opcodes.AALOAD: return "JAVA_OBJECT";
            case Opcodes.FALOAD: return "JAVA_FLOAT";
            case Opcodes.DALOAD: return "JAVA_DOUBLE";
            case Opcodes.LALOAD: return "JAVA_LONG";
        }
        return null;
    }

    private String getArrayDataCType() {
        switch (loadInstruction.getOpcode()) {
            case Opcodes.IALOAD: return "JAVA_ARRAY_INT";
            case Opcodes.CALOAD: return "JAVA_ARRAY_CHAR";
            case Opcodes.SALOAD: return "JAVA_ARRAY_SHORT";
            case Opcodes.BALOAD: return "JAVA_ARRAY_BYTE";
            case Opcodes.AALOAD: return "JAVA_ARRAY_OBJECT";
            case Opcodes.FALOAD: return "JAVA_ARRAY_FLOAT";
            case Opcodes.DALOAD: return "JAVA_ARRAY_DOUBLE";
            case Opcodes.LALOAD: return "JAVA_ARRAY_LONG";
        }
        return null;
    }

    /**
     * Emits this array load as a DIVERGING-check statement prelude for a fused
     * comparison in a FRAMELESS method: the null/bounds failure paths throw and
     * return from the method (CN1_ARRAY_CHECK_DIVERGE) instead of merging back,
     * which keeps calls out of the loop body so clang can hoist the array
     * header loads. Appends declarations + checks + the element read into
     * {@code out}; the loaded value lands in {@code tempName}.
     *
     * @param retvalText the method's default return expression ("0" or "" for void)
     * @return false when the operand expressions can't be rendered (caller
     *         falls back to the ordinary merging accessor form)
     */
    public boolean emitDiverging(String tempName, String retvalText, StringBuilder out) {
        String elemType = getElementCType();
        String dataType = getArrayDataCType();
        if (elemType == null || dataType == null) {
            return false;
        }
        String arrayExpr;
        String indexExpr;
        StringBuilder sb2 = new StringBuilder();
        if (!(targetArrayInstruction instanceof AssignableExpression)
                || !((AssignableExpression) targetArrayInstruction).assignTo(null, sb2)) {
            return false;
        }
        arrayExpr = sb2.toString().trim();
        sb2.setLength(0);
        if (!(indexInstruction instanceof AssignableExpression)
                || !((AssignableExpression) indexInstruction).assignTo(null, sb2)) {
            return false;
        }
        indexExpr = sb2.toString().trim();

        out.append("JAVA_OBJECT ").append(tempName).append("_a = ").append(arrayExpr).append("; ");
        out.append("JAVA_INT ").append(tempName).append("_i = ").append(indexExpr).append(";\n    ");
        if (!isBoundsSafe() && (getMethod() == null || !getMethod().isDisableNullAndArrayBoundsChecks())) {
            out.append("CN1_ARRAY_CHECK_DIVERGE(").append(tempName).append("_a, ")
               .append(tempName).append("_i, ").append(retvalText).append(");\n    ");
        }
        out.append(elemType).append(' ').append(tempName).append(" = ((").append(dataType)
           .append("*) (*(JAVA_ARRAY)").append(tempName).append("_a).data)[").append(tempName).append("_i];\n    ");
        return true;
    }

    private static boolean isSimpleExpression(String expr) {
        if (expr == null || expr.length() == 0) {
            return false;
        }
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if ((c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || c == '_' || c == '.') {
                continue;
            }
            return false;
        }
        return true;
    }
}
