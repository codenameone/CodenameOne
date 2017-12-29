/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.ui;

import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.testing.AbstractTest;
import com.codename1.testing.TestUtils;
import com.codename1.ui.BrowserComponent.JSProxy;
import com.codename1.ui.BrowserComponent.JSRef;
import com.codename1.ui.BrowserComponent.JSType;
import com.codename1.ui.layouts.BorderLayout;

/**
 *
 * @author shannah
 */
public class JavascriptTests extends AbstractTest {

    private static class Res {
        boolean complete;
        Throwable error;
    }
    
    @Override
    public boolean runTest() throws Exception {
        Form f = new Form("Test Browser");
        f.setLayout(new BorderLayout());
        BrowserComponent bc = new BrowserComponent();
        bc.setPage("<!doctype html><html><head><title>Foo</title><body>Body</body></head></html>", "http://www.codenameone.com");
        final Res res = new Res();
        bc.addWebEventListener(BrowserComponent.onLoad, e->{
            try {
                int timeout = 1000;
                bc.execute("window.person={name:'Steve', somefunc: function(a){this.someval = a}}");
                JSRef tmp = bc.executeAndWait(timeout, "callback.onSuccess(person.name)");
                TestUtils.assertEqual(JSType.STRING, tmp.getJSType(), "Wrong JSType for person.name");
                TestUtils.assertEqual("Steve", tmp.toString());
                
                bc.execute("person.age=${0}", new Object[]{24});
                tmp = bc.executeAndWait(timeout, "callback.onSuccess(person.age)");
                TestUtils.assertEqual(JSType.NUMBER, tmp.getJSType(), "Wrong JSType for age");
                TestUtils.assertEqual(24, tmp.getInt(), "Age should be 24");
                
                
                bc.execute("person.enabled=${0}", new Object[]{true});
                tmp = bc.executeAndWait(timeout, "callback.onSuccess(person.enabled)");
                TestUtils.assertEqual(JSType.BOOLEAN, tmp.getJSType(), "Wrong JSType for enabled");
                TestUtils.assertEqual(true, tmp.getBoolean(), "Wrong value for enabled");
                
                tmp = bc.executeAndWait(timeout, "callback.onSuccess(person)");
                TestUtils.assertEqual(JSType.OBJECT, tmp.getJSType(), "Wrong type for person");
                
                tmp = bc.executeAndWait(timeout, "callback.onSuccess(person.somefunc)");
                TestUtils.assertEqual(JSType.FUNCTION, tmp.getJSType(), "Wrong type for somefunc");
                
                tmp = bc.executeAndWait(timeout, "callback.onSuccess(person.firstName)");
                TestUtils.assertEqual(JSType.UNDEFINED, tmp.getJSType(), "firstName should be undefined");
                
                bc.execute("window.person2={name:'Marlene'}");
                tmp = bc.executeAndWait(timeout, "callback.onSuccess(48)");
                bc.execute("window.person2.age=${0}", new Object[]{tmp});
                tmp = bc.executeAndWait(timeout, "callback.onSuccess(window.person2.age)");
                TestUtils.assertEqual(JSType.NUMBER, tmp.getJSType(), "Wrong type for age after setting with JSRef");
                TestUtils.assertEqual(48, tmp.getInt(), "Wrong value for age after setting with JSRef");
                
                tmp = bc.executeAndWait(timeout, "callback.onSuccess(${0} + ${1} + ${2} + ${3})", new Object[]{ 1, 2, 3, 4});
                TestUtils.assertEqual(JSType.NUMBER, tmp.getJSType(), "Wrong type for 1+2+3+4");
                TestUtils.assertEqual(10, tmp.getInt(), "Wrong value for 1+2+3+4");
                
                JSRef name1 = bc.executeAndWait(timeout, "callback.onSuccess(person.name)");
                JSRef name2 = bc.executeAndWait(timeout, "callback.onSuccess(person2.name)");
                JSRef name1name2 = bc.executeAndWait(timeout, "callback.onSuccess(${0} +' '+${1})", new Object[]{name1, name2});
                TestUtils.assertEqual(JSType.STRING, name1name2.getJSType(), "Wrong type for name1name2");
                TestUtils.assertEqual("Steve Marlene", name1name2.toString(), "Wrong value for name1name2");
                
                JSProxy proxy = bc.createJSProxy("person.name"); // Steve
                tmp = proxy.callAndWait(timeout, "indexOf", new Object[]{"e"});
                TestUtils.assertEqual(2, tmp.getInt(), "Wrong position for 'e'");
                tmp = proxy.getAndWait(timeout, "length");
                TestUtils.assertEqual(5, tmp.getInt(), "Wrong string length in proxy");
                
                //JSProxy person = bc.createJSProxy("person2");
                
                
                
                
                synchronized(res) {
                    res.complete = true;
                    res.notifyAll();
                }
                res.complete = true;
                
            } catch (Throwable t) {
                synchronized (res) {
                    res.complete = true;
                    res.error = t;
                    res.notifyAll();
                }
            }
            
        });
        
        f.add(BorderLayout.CENTER, bc);
        f.show();
        
        while (!res.complete) {
            Display.getInstance().invokeAndBlock(()->{
                synchronized(res) {
                    Util.wait(res, 1000);
                }
            });
        }
        if (res.error != null) {
            Log.e(res.error);
            throw new Exception(res.error.getMessage());
        }
        return true;
    }

    @Override
    public boolean shouldExecuteOnEDT() {
        return true;
    }
    
    
    
}
