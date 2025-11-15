package com.codename1.ui.util;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.DisplayTest;
import com.codename1.ui.Form;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

import java.util.HashMap;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.*;

class UIUtilPackageTest extends UITestBase {

    @FormTest
    void eventDispatcherDispatchesAndRemovesListeners() {
        EventDispatcher dispatcher = new EventDispatcher();
        RecordingActionListener first = new RecordingActionListener();
        RecordingActionListener second = new RecordingActionListener();
        dispatcher.addListener(first);
        dispatcher.addListener(second);

        dispatcher.fireActionEvent(new ActionEvent(this));
        flushSerialCalls();
        assertEquals(1, first.invocations);
        assertEquals(1, second.invocations);

        dispatcher.removeListener(first);
        dispatcher.fireActionEvent(new ActionEvent(this));
        flushSerialCalls();
        assertEquals(1, first.invocations);
        assertEquals(2, second.invocations);

        Vector listenerVector = dispatcher.getListenerVector();
        listenerVector.clear();
        assertEquals(1, dispatcher.getListenerCollection().size());
    }

    @FormTest
    void weakHashMapStoresValuesUsingDisplayReferences() {
        WeakHashMap<String, String> map = new WeakHashMap<String, String>();
        assertTrue(map.isEmpty());
        map.put("key", "value");
        assertEquals(1, map.size());
        assertTrue(map.containsKey("key"));
        assertEquals("value", map.get("key"));
        assertEquals("value", map.remove("key"));
        assertTrue(map.isEmpty());
        assertThrows(RuntimeException.class, map::values);

        HashMap<String, String> other = new HashMap<String, String>();
        other.put("a", "b");
        map.putAll(other);
        assertEquals("b", map.get("a"));
        map.clear();
        assertTrue(map.isEmpty());
    }

    @FormTest
    void uiTimerSchedulesAndCancelsTasks() throws Exception {
        Form form = new Form();
        form.show();

        DisplayTest.flushEdt();
        flushSerialCalls();

        CountingRunnable counting = new CountingRunnable();
        UITimer timer = new UITimer(counting);
        timer.schedule(1000, false, form);
        assertEquals(0, counting.count);
        timer.testEllapse();
        assertEquals(1, counting.count);

        timer.cancel();
        int singleCount = counting.count;
        timer.testEllapse();
        assertEquals(singleCount, counting.count);

        counting.count = 0;
        UITimer repeating = new UITimer(counting);
        repeating.schedule(1000, true, form);
        repeating.testEllapse();
        assertEquals(1, counting.count);
        repeating.testEllapse();
        assertEquals(2, counting.count);

        repeating.cancel();
        int before = counting.count;
        repeating.testEllapse();
        assertEquals(before, counting.count);
    }

    private static class RecordingActionListener implements ActionListener {
        int invocations;

        public void actionPerformed(ActionEvent evt) {
            invocations++;
        }
    }

    private static class CountingRunnable implements Runnable {
        int count;

        public void run() {
            count++;
        }
    }

}
