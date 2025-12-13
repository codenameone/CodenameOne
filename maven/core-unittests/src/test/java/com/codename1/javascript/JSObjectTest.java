package com.codename1.javascript;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.CN;
import com.codename1.ui.DisplayTest;
import com.codename1.ui.TestPeerComponent;
import com.codename1.util.Callback;
import com.codename1.util.SuccessCallback;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

public class JSObjectTest extends UITestBase {

    @AfterEach
    public void tearDown() {
        TestCodenameOneImplementation.getInstance().setBrowserScriptResponder(null);
    }

    @FormTest
    public void testJSObjectAsyncWrappers() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        impl.setBrowserComponent(new TestPeerComponent(null));
        impl.getBrowserExecuted().clear();

        BrowserComponent browser = new BrowserComponent();
        CN.getCurrentForm().add(browser);
        JavascriptContext context = new JavascriptContext(browser);

        impl.setBrowserScriptResponder(script -> {
            if(script.contains("ca_weblite_codename1_js_JSObject_R1.ca_weblite_codename1_js_JSObject_ID")) {
                return "1";
            }
            if (script.contains("typeof")) {
                return "object";
            }
            if (script.contains("var id = ")) {
                return "1";
            }
            return null;
        });
        DisplayTest.flushEdt();
        JSObject obj = new JSObject(context, "myObj");

        // --- Test JSObject$2 (callAsync(String, SuccessCallback)) ---
        AtomicReference<Object> resultRef = new AtomicReference<>();
        SuccessCallback successCallback = new SuccessCallback() {
            @Override
            public void onSucess(Object value) {
                resultRef.set(value);
            }
        };

        impl.getBrowserExecuted().clear();
        obj.callAsync("method1", successCallback);
        verifyAndTriggerCallback("Success1", resultRef, browser, context);
        assertEquals("Success1", resultRef.get());

        // --- Test JSObject$3 (callIntAsync(String, Callback<Integer>)) ---
        AtomicReference<Integer> intRef = new AtomicReference<>();
        Callback<Integer> intCallback = new Callback<Integer>() {
            @Override
            public void onSucess(Integer value) {
                intRef.set(value);
            }
            @Override
            public void onError(Object sender, Throwable err, int errorCode, String errorMessage) {}
        };
        impl.getBrowserExecuted().clear();
        obj.callIntAsync("methodInt", intCallback);
        triggerCallback(Double.valueOf(123.0), browser, context);
        assertEquals(123, intRef.get());

        // --- Test JSObject$4 (callIntAsync(String, SuccessCallback<Integer>)) ---
        AtomicReference<Integer> intSuccessRef = new AtomicReference<>();
        SuccessCallback<Integer> intSuccessCallback = new SuccessCallback<Integer>() {
            @Override
            public void onSucess(Integer value) {
                intSuccessRef.set(value);
            }
        };
        impl.getBrowserExecuted().clear();
        obj.callIntAsync("methodIntSuccess", intSuccessCallback);
        triggerCallback(Double.valueOf(456.0), browser, context);
        assertEquals(456, intSuccessRef.get());

        // --- Test JSObject$5 (callDoubleAsync(String, SuccessCallback<Double>)) ---
        AtomicReference<Double> doubleSuccessRef = new AtomicReference<>();
        SuccessCallback<Double> doubleSuccessCallback = new SuccessCallback<Double>() {
            @Override
            public void onSucess(Double value) {
                doubleSuccessRef.set(value);
            }
        };
        impl.getBrowserExecuted().clear();
        obj.callDoubleAsync("methodDoubleSuccess", doubleSuccessCallback);
        triggerCallback(Double.valueOf(78.9), browser, context);
        assertEquals(78.9, doubleSuccessRef.get(), 0.001);

        // --- Test JSObject$8 (callAsync(Object[], SuccessCallback)) ---
        AtomicReference<Object> objArrayRef = new AtomicReference<>();
        SuccessCallback objArrayCallback = new SuccessCallback() {
            @Override
            public void onSucess(Object value) {
                objArrayRef.set(value);
            }
        };
        impl.getBrowserExecuted().clear();
        obj.callAsync(new Object[]{"param"}, objArrayCallback);
        triggerCallback("ResultObject", browser, context);
        assertEquals("ResultObject", objArrayRef.get());
    }

    private void verifyAndTriggerCallback(Object result, AtomicReference<Object> ref, BrowserComponent browser, JavascriptContext context) {
        triggerCallback(result, browser, context);
    }

    private void triggerCallback(Object result, BrowserComponent browser, JavascriptContext context) {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        List<String> executed = impl.getBrowserExecuted();
        assertFalse(executed.isEmpty(), "No JS executed");
        String lastScript = executed.get(executed.size() - 1);

        int callbackIndex = lastScript.indexOf("callback$$");
        assertTrue(callbackIndex >= 0, "Callback not found in script");

        int startIndex = callbackIndex;
        int endIndex = lastScript.indexOf("(", startIndex);
        String callbackName = lastScript.substring(startIndex, endIndex).trim();

        JavascriptEvent evt = new JavascriptEvent(context.getWindow(), callbackName, new Object[]{result});
        browser.fireWebEvent("scriptMessageReceived", evt);
    }
}
