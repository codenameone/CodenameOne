/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.tools.translator.bytecodes;

/**
 * Interface for an instruction that can be replaced by an expression and assigned
 * to a variable.  
 * @author shannah
 */
public interface AssignableExpression {
    
    /**
     * Outputs C code to assign the expression to given variable.
     * @param varName
     * @param sb
     * @return 
     */
    public boolean assignTo(String varName, String typeVarName, StringBuilder sb);
}
