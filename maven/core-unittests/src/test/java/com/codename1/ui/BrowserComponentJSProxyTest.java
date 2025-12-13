package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import org.junit.jupiter.api.Assertions;

public class BrowserComponentJSProxyTest extends UITestBase {

    public static class TestPeer extends PeerComponent {
        public TestPeer() {
            super(new Object());
        }
    }

    @FormTest
    public void testJSProxy() {
        implementation.setBrowserComponent(new TestPeer());
        BrowserComponent bc = new BrowserComponent();
        BrowserComponent.JSProxy proxy = bc.createJSProxy("window");

        proxy.call("alert", new Object[]{"Hello"}, null);

        flushSerialCalls();

        java.util.List<String> executed = implementation.getBrowserExecuted();
        Assertions.assertFalse(executed.isEmpty());
        String lastScript = executed.get(executed.size() - 1);
        Assertions.assertTrue(lastScript.contains("window.alert"));
        Assertions.assertTrue(lastScript.contains("Hello"));
    }
}
