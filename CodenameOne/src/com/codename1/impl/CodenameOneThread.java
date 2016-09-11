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
package com.codename1.impl;

import com.codename1.io.Log;
import com.codename1.io.Preferences;
import com.codename1.system.CrashReport;
import com.codename1.ui.Display;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

/**
 * Thread class implementing the crash protection functionality
 *
 * @author Shai Almog
 */
public class CodenameOneThread extends Thread {
    private int[] stack =  new int[500];
    private int stackPointer;
    private Runnable r;
    private static Class CODE = CodenameOneThread.class;
    private Hashtable exceptionStack = new Hashtable();
    
    /**
     * Constructor accepting the runnable
     */
    public CodenameOneThread(Runnable r, String threadName) {
        super(threadName);
        this.r = r;
    }
    
    public boolean hasStackFrame() {
        return stackPointer > 0;
    }
    
    /**
     * Pushes a method id into the stack
     * @param method the method id
     */
    public void pushStack(int method) {
        stack[stackPointer] = method;
        stackPointer++;
    }
    
    /**
     * Pops the method stack pointer
     */
    public void popStack() {
        stackPointer--;
    }
    
    /**
     * Pushes the method to the current thread stack
     * 
     * @param method the method id
     */
    public static void push(int method) {
        Thread t = Thread.currentThread();
        if(t.getClass() == CODE) {
            CodenameOneThread c = (CodenameOneThread)t;
            c.pushStack(method);
        }
    }

    /**
     * Pops the current method from the stack
     */
    public static void pop() {
        Thread t = Thread.currentThread();
        if(t.getClass() == CODE) {
            CodenameOneThread c = (CodenameOneThread)t;
            c.popStack();
        }
    }
    
    
    /**
     * Stores the stack for the given exception
     * @param t the exception mapping to the given stack
     */
    public void storeStackForException(Throwable t, int currentStackFrame) {
        if(!exceptionStack.containsKey(t)) {
            int[] s = new int[stackPointer + 1];
            System.arraycopy(stack, 0, s, 0, stackPointer);
            s[stackPointer] = currentStackFrame;
            exceptionStack.put(t, s);
        }
    }
    
    /**
     * Stores the stack for the given exception
     * @param th the exception mapping to the given stack
     */
    public static void storeStack(Throwable th, int currentStackFrame) {
        Thread t = Thread.currentThread();
        if(t.getClass() == CODE) {
            CodenameOneThread c = (CodenameOneThread)t;
            c.storeStackForException(th, currentStackFrame);
        }
    }
    
    /**
     * Prints the stack trace matching the given stack
     */
    public String getStack(Throwable t) {
        try {
            StringBuilder b = new StringBuilder();
            int size;
            int[] s = (int[])exceptionStack.get(t);
            if(s == null) {
                s = stack;
                size = stackPointer;
            } else {
                size = s.length;
            }
            String[] stk = new String[size];
            
            InputStream inp = Display.getInstance().getResourceAsStream(getClass(), "/methodData.dat");
            if(inp == null) {
                return t.toString();
            }
            DataInputStream di = new DataInputStream(inp);
            int totalAmount = di.readInt();
            String lastClass = "";
            for(int x = 0 ; x < totalAmount ; x++) {
                String current = di.readUTF();
                if(current.indexOf('.') > -1) {
                    lastClass = current;
                } else {
                    for(int iter = 0 ; iter < size ; iter++) {
                        if(s[iter] == x + 1) {
                            stk[iter] = lastClass + "." + current;
                        }
                    }
                }
            }
            for(int iter = size - 1 ; iter >= 0 ; iter--) {
                b.append("at ");
                b.append(stk[iter]);
                b.append("\n");
            }
            return b.toString();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return "Failed in stack generation for " + t;
    }
    
    /**
     * Catches exception
     */
    public void run() {
        try {
            r.run();
        } catch(Throwable err) {
            err.printStackTrace();
            handleException(err);
        }
    }
    
    public static void handleException(Throwable err) {
        Log.e(err);
        Log.sendLog();
    }
}
