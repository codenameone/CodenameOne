package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.WindowEvent;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WindowApiTest extends UITestBase {

    @FormTest
    void desktopInformationExposedThroughDisplay() {
        Dimension desktopSize = new Dimension(1600, 900);
        implementation.setDesktopSize(desktopSize);

        Dimension fromDisplay = Display.getInstance().getDesktopSize();

        assertEquals(desktopSize.getWidth(), fromDisplay.getWidth());
        assertEquals(desktopSize.getHeight(), fromDisplay.getHeight());
    }

    @FormTest
    void windowListenersReceiveDispatchedEvents() {
        Display display = Display.getInstance();
        List<WindowEvent> events = new ArrayList<WindowEvent>();
        WindowEvent event = new WindowEvent(display, WindowEvent.Type.Resized, new Rectangle(10, 20, 300, 200));

        ActionListener<WindowEvent> listener = evt -> events.add(evt);
        display.addWindowListener(listener);
        display.fireWindowEvent(event);
        flushSerialCalls();

        assertEquals(1, events.size());
        WindowEvent captured = events.get(0);
        assertEquals(WindowEvent.Type.Resized, captured.getType());
        Rectangle bounds = captured.getBounds();
        assertNotNull(bounds);
        assertEquals(10, bounds.getX());
        assertEquals(20, bounds.getY());
        assertEquals(300, bounds.getSize().getWidth());
        assertEquals(200, bounds.getSize().getHeight());
        display.removeWindowListener(listener);
    }

    @FormTest
    void firstFormWindowSizeHintUsesPercentages() {
        implementation.setDesktopSize(new Dimension(2000, 1000));
        Form first = new Form();
        first.putClientProperty(Display.WINDOW_SIZE_HINT_PERCENT, new Dimension(50, 25));

        first.show();
        flushSerialCalls();

        Dimension lastWindowSize = implementation.getLastWindowSize();
        assertNotNull(lastWindowSize);
        assertEquals(1000, lastWindowSize.getWidth());
        assertEquals(250, lastWindowSize.getHeight());

        Form second = new Form();
        second.putClientProperty(Display.WINDOW_SIZE_HINT_PERCENT, new Dimension(10, 10));
        second.show();
        flushSerialCalls();

        assertEquals(1000, implementation.getLastWindowSize().getWidth());
        assertEquals(250, implementation.getLastWindowSize().getHeight());
    }
}
