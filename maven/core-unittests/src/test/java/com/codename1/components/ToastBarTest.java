package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import static org.junit.jupiter.api.Assertions.*;

class ToastBarTest extends UITestBase {

    @FormTest
    void testGetInstanceReturnsSingleton() {
        ToastBar tb1 = ToastBar.getInstance();
        ToastBar tb2 = ToastBar.getInstance();
        assertSame(tb1, tb2);
    }

    @FormTest
    void testDefaultMessageTimeoutGetterAndSetter() {
        int original = ToastBar.getDefaultMessageTimeout();
        try {
            ToastBar.setDefaultMessageTimeout(5000);
            assertEquals(5000, ToastBar.getDefaultMessageTimeout());
        } finally {
            ToastBar.setDefaultMessageTimeout(original);
        }
    }

    @FormTest
    void testDefaultUIIDGetterAndSetter() {
        ToastBar tb = ToastBar.getInstance();
        tb.setDefaultUIID("CustomToast");
        assertEquals("CustomToast", tb.getDefaultUIID());
    }

    @FormTest
    void testDefaultMessageUIIDGetterAndSetter() {
        ToastBar tb = ToastBar.getInstance();
        tb.setDefaultMessageUIID("CustomMessage");
        assertEquals("CustomMessage", tb.getDefaultMessageUIID());
    }

    @FormTest
    void testPositionGetterAndSetter() {
        ToastBar tb = ToastBar.getInstance();
        tb.setPosition(com.codename1.ui.Component.TOP);
        assertEquals(com.codename1.ui.Component.TOP, tb.getPosition());

        tb.setPosition(com.codename1.ui.Component.BOTTOM);
        assertEquals(com.codename1.ui.Component.BOTTOM, tb.getPosition());
    }

    @FormTest
    void testUseFormLayeredPaneGetterAndSetter() {
        ToastBar tb = ToastBar.getInstance();
        tb.setUseFormLayeredPane(true);
        assertTrue(tb.isUseFormLayeredPane());

        tb.setUseFormLayeredPane(false);
        assertFalse(tb.isUseFormLayeredPane());
    }

    @FormTest
    void testCreateStatusReturnsStatus() {
        ToastBar tb = ToastBar.getInstance();
        ToastBar.Status status = tb.createStatus();
        assertNotNull(status);
    }

    @FormTest
    void testShowErrorMessageStatic() {
        ToastBar.showErrorMessage("Test Error");
        // Should not throw exception
        assertTrue(true);
    }

    @FormTest
    void testShowInfoMessageStatic() {
        ToastBar.showInfoMessage("Test Info");
        // Should not throw exception
        assertTrue(true);
    }

    @FormTest
    void testShowMessageWithIcon() {
        ToastBar.Status status = ToastBar.showMessage("Test", '\uE000', 1000);
        assertNotNull(status);
    }
}
