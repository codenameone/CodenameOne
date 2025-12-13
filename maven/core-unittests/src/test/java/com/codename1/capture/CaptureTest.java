package com.codename1.capture;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.events.ActionEvent;

import static org.junit.jupiter.api.Assertions.*;

class CaptureTest extends UITestBase {
    private static final String ORIGINAL_PATH = "/tmp/photo.jpg";

    @FormTest
    void callBackStoresUrlFromActionEvent() {
        Capture.CallBack callBack = new Capture.CallBack();
        ActionEvent event = new ActionEvent(ORIGINAL_PATH);

        callBack.actionPerformed(event);

        assertEquals(ORIGINAL_PATH, callBack.url);
    }

    @FormTest
    void callBackSetsUrlToNullWhenEventIsNull() {
        Capture.CallBack callBack = new Capture.CallBack();

        callBack.actionPerformed(null);

        assertNull(callBack.url);
    }

    @FormTest
    void runSkipsProcessingWhenUrlIsNull() {
        Capture.CallBack callBack = new Capture.CallBack();
        callBack.actionPerformed(null);

        callBack.run();
    }



}
