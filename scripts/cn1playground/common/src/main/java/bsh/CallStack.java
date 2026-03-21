/*****************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one                *
 * or more contributor license agreements.  See the NOTICE file              *
 * distributed with this work for additional information                     *
 * regarding copyright ownership.  The ASF licenses this file                *
 * to you under the Apache License, Version 2.0 (the                         *
 * "License"); you may not use this file except in compliance                *
 * with the License.  You may obtain a copy of the License at                *
 *                                                                           *
 *     http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                           *
 * Unless required by applicable law or agreed to in writing,                *
 * software distributed under the License is distributed on an               *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY                    *
 * KIND, either express or implied.  See the License for the                 *
 * specific language governing permissions and limitations                   *
 * under the License.                                                        *
 *                                                                           *
 *                                                                           *
 * This file is part of the BeanShell Java Scripting distribution.           *
 * Documentation and updates may be found at http://www.beanshell.org/       *
 * Patrick Niemeyer (pat@pat.net)                                            *
 * Author of Learning Java, O'Reilly & Associates                            *
 *                                                                           *
 *****************************************************************************/


package bsh;

import java.util.Stack;
import java.io.Serializable;
import java.util.EmptyStackException;

/**
    A stack of NameSpaces representing the call path.
    Each method invocation, for example, pushes a new NameSpace onto the stack.
    The top of the stack is always the current namespace of evaluation.
    <p>

    This is used to support the this.caller magic reference and to print
    script "stack traces" when evaluation errors occur.
    <p>

    Note: How can this be thread safe, you might ask?  Wouldn't a thread
    executing various beanshell methods be mutating the callstack?  Don't we
    need one CallStack per Thread in the interpreter?  The answer is that we do.
    Any java.lang.Thread enters our script via an external (hard) Java
    reference via a This type interface, e.g.  the Runnable interface
    implemented by This or an arbitrary interface implemented by XThis.
    In that case the This invokeMethod() method (called by any interface that
    it exposes) creates a new CallStack for each external call.
    <p>
*/
public final class CallStack implements Serializable {
    /** default serial version id */
    private static final long serialVersionUID = 1L;
    private final Stack<NameSpace> stack = new Stack<>();

    public CallStack() { }

    public CallStack( NameSpace namespace ) {
        push( namespace );
    }

    public void clear() {
        stack.clear();
    }

    public void push( NameSpace ns ) {
        stack.push( ns );
    }

    public NameSpace top() {
        return stack.peek();
    }

    /**
        zero based.
    */
    public NameSpace get(int depth) {
        int size = stack.size();
        if ( depth >= size )
            return NameSpace.JAVACODE;
        return stack.toArray(new NameSpace[size])[size-1-depth];
    }

    /**
        This is kind of crazy, but used by the setNameSpace command.
        zero based.
    */
    public synchronized void set(int depth, NameSpace ns) {
        stack.set( stack.size()-1-depth, ns );
    }

    public NameSpace pop() {
        try {
            return stack.pop();
        } catch(EmptyStackException e) {
            throw new InterpreterError("pop on empty CallStack");
        }
    }

    /**
        Swap in the value as the new top of the stack and return the old
        value.
    */
    public NameSpace swap( NameSpace newTop ) {
        NameSpace oldTop = stack.pop();
        stack.push(newTop);
        return oldTop;
    }

    public int depth() {
        return stack.size();
    }
/*
    public NameSpace [] toArray() {
        NameSpace [] nsa = new NameSpace [ depth() ];
        stack.copyInto( nsa );
        return nsa;
    }
*/
    public synchronized String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CallStack:\n");
        for( int i=stack.size()-1; i>=0; i-- )
            sb.append("\t"+stack.get(i)+"\n");

        return sb.toString();
    }

    /**
        Occasionally we need to freeze the callstack for error reporting
        purposes, etc.
    */
    public CallStack copy() {
        CallStack cs = new CallStack();
        cs.stack.addAll(this.stack);
        return cs;
    }
}
