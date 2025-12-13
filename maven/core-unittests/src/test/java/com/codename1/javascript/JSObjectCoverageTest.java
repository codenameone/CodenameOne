package com.codename1.javascript;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.TestPeerComponent;
import com.codename1.util.SuccessCallback;
import com.codename1.testing.TestCodenameOneImplementation;
import java.util.function.Function;

public class JSObjectCoverageTest extends UITestBase {

    @FormTest
    public void testJSObjectAndContextAnonymousClasses() {
        TestPeerComponent peer = new TestPeerComponent(null);
        TestCodenameOneImplementation.getInstance().setBrowserComponent(peer);

        BrowserComponent browser = new BrowserComponent();
        JavascriptContext ctx = new JavascriptContext(browser);

        TestCodenameOneImplementation.getInstance().setBrowserScriptResponder(new Function<String, String>() {
            @Override
            public String apply(String script) {
                // ID generation script ends with "} id" or checks ID_KEY
                if (script.endsWith("} id")) {
                     return "0";
                }
                if (script.contains("typeof")) {
                    return "object";
                }
                return null;
            }
        });

        JSObject jsObj = new JSObject(ctx, "window");

        // JSObject$1: callAsync(String, Object[], SuccessCallback)
        jsObj.callAsync("method", new Object[]{}, new SuccessCallback() {
            public void onSucess(Object value) {}
        });

        // JSObject$6: callStringAsync
        jsObj.callStringAsync("method", new SuccessCallback<String>() {
             public void onSucess(String value) {}
        });

        // JSObject$7: callObjectAsync
        jsObj.callObjectAsync("method", new SuccessCallback<JSObject>() {
             public void onSucess(JSObject value) {}
        });

        // JavascriptContext$3: getAsync(String, SuccessCallback)
        ctx.getAsync("1+1", new SuccessCallback() {
            public void onSucess(Object value) {}
        });

        // JavascriptContext$4: callAsync(JSObject, JSObject, Object[], SuccessCallback)
        ctx.callAsync(jsObj, jsObj, new Object[]{}, new SuccessCallback() {
            public void onSucess(Object value) {}
        });

        // JavascriptContext$6: call(String, JSObject, Object[], boolean, SuccessCallback)
        ctx.callAsync("func", jsObj, new Object[]{}, new SuccessCallback() {
            public void onSucess(Object value) {}
        });
    }
}
