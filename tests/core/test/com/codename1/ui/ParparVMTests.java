/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.ui;

import com.codename1.testing.AbstractTest;

/**
 *
 * @author shannah
 */
public class ParparVMTests extends AbstractTest {

    static enum TestEnum2 {
        RED,
        GREEN,
        BLUE
    }
    
    @Override
    public boolean runTest() throws Exception {
        Object anonymousClass = new Object() {
            @Override
            public String toString() {
                return "This is an anonymous class";
            }
        };
            
        assertTrue(TestEnum2.class.isEnum(), "TestEnum2 isEnum() returns wrong value");
        assertTrue(!ParparVMTests.class.isEnum(), "MyTest isEnum() returns wrong value");
        assertTrue(!ParparVMTests.class.isSynthetic(), "MyTest isSynthetic returns wrong value");
        assertTrue(!ParparVMTests.class.isInterface(), "MyTest isInterface returns wrong value");
        assertTrue(com.codename1.ui.events.ActionListener.class.isInterface(), "ActionListener is not recognized as interface");
        assertTrue(!ParparVMTests.class.isAnnotation(), "MyTest is incorrectly recognized as annotation.");
        //assertTrue(TestAnnotation.class.isAnnotation(), "TestAnnotation class is not recognized as annotation");
        assertTrue(!ParparVMTests.class.isAnnotation(), "MyTest is incorrectly recognized as annotation");
        assertTrue(anonymousClass.getClass().isAnonymousClass(), "Anonymous class is not recognized");
        assertTrue(!ParparVMTests.class.isAnonymousClass(), "MyTest is incorrectly recognized as anonymous class");
        //assertTrue(!MyTest.class.isPrimitive(), "MyTest is incorrectly recognized as primitive");
        //assertTrue(int.class.isPrimitive(), "int is incorrectly recognized as primitive");


    return true;
    }
    
}
