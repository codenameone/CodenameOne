package com.codename1.javascript;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.TestPeerComponent;
import com.codename1.util.SuccessCallback;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

public class JavascriptContextTest extends UITestBase {

    @AfterEach
    public void tearDown() {
        TestCodenameOneImplementation.getInstance().setBrowserScriptResponder(null);
    }

    @FormTest
    public void testCallAsyncWithSuccessCallback() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        impl.setBrowserComponent(new TestPeerComponent(null));
        impl.getBrowserExecuted().clear();

        BrowserComponent browser = new BrowserComponent();
        JavascriptContext context = new JavascriptContext(browser);

        impl.setBrowserScriptResponder(script -> {
            if (script.contains("typeof")) return "object";
            if (script.contains("var id = ")) return "1";
            return null;
        });

        JSObject self = (JSObject) context.get("{}");

        AtomicReference<Object> resultRef = new AtomicReference<Object>();
        SuccessCallback callback = new SuccessCallback() {
            @Override
            public void onSucess(Object value) {
                resultRef.set(value);
            }
        };

        impl.getBrowserExecuted().clear();
        context.callAsync("myFunc", self, new Object[]{"arg1"}, callback);

        List<String> executed = impl.getBrowserExecuted();
        assertFalse(executed.isEmpty(), "Browser should have executed script");

        String lastScript = executed.get(executed.size() - 1);

        int callbackIndex = lastScript.indexOf("callback$$");
        assertTrue(callbackIndex >= 0, "Callback not found");

        int parenIndex = lastScript.indexOf("(", callbackIndex);
        String callbackName = lastScript.substring(callbackIndex, parenIndex).trim();

        JavascriptEvent evt = new JavascriptEvent(context.getWindow(), callbackName, new Object[]{"SuccessResult"});
        browser.fireWebEvent("scriptMessageReceived", evt);

        assertEquals("SuccessResult", resultRef.get());
    }
}
