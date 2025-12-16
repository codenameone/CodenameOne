package com.codename1.ui;

import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.util.EventDispatcher;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.BrowserComponent.JSRef;
import com.codename1.util.SuccessCallback;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import com.codename1.ui.events.ActionEvent;
import java.util.function.Function;

public class BrowserComponentCoverageTest extends UITestBase {
    @FormTest
    public void testExecuteResult() {
        TestCodenameOneImplementation.getInstance().setNativeBrowserTypeSupported(true);
        BrowserComponent bc = new BrowserComponent();
        Form f = new Form("Browser", new BorderLayout());
        f.add(BorderLayout.CENTER, bc);
        f.show();

        TestCodenameOneImplementation.getInstance().setBrowserScriptResponder(new Function<String, String>() {
            public String apply(String script) {
                if(script.contains("eval")) {
                     return "123";
                }
                return null;
            }
        });

        bc.execute("eval('1+1')");
    }

    @FormTest
    public void testReadyWrapper() {
         TestCodenameOneImplementation.getInstance().setNativeBrowserTypeSupported(true);
         final BrowserComponent bc = new BrowserComponent();
         Form f = new Form("Browser", new BorderLayout());
         f.add(BorderLayout.CENTER, bc);
         f.show();

         // Trigger onStart to fire ready
         Display.getInstance().callSerially(new Runnable() {
             public void run() {
                 bc.fireWebEvent(BrowserComponent.onStart, new ActionEvent(bc));
                 bc.fireWebEvent(BrowserComponent.onLoad, new ActionEvent(bc));
             }
         });

         bc.ready(100);
         // waitFor(200);
    }
}
