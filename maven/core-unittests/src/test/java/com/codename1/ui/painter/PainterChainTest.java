package com.codename1.ui.painter;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Rectangle;

import static org.junit.jupiter.api.Assertions.*;

class PainterChainTest extends UITestBase {

    @FormTest
    void testPainterChainInvokesPaintersInOrder() {
        RecordingPainter first = new RecordingPainter("first");
        RecordingPainter second = new RecordingPainter("second");
        RecordingPainter third = new RecordingPainter("third");

        PainterChain chain = new PainterChain(new com.codename1.ui.Painter[]{first, second});
        chain = chain.addPainter(third);

        chain.paint(new Graphics(), new Rectangle(0, 0, 10, 10));
        assertEquals("first,second,third", first.getOrder());
    }

    private static class RecordingPainter implements com.codename1.ui.Painter {
        private final String label;
        private static StringBuilder order = new StringBuilder();

        RecordingPainter(String label) {
            this.label = label;
        }

        public void paint(Graphics g, Rectangle rect) {
            if (order.length() > 0) {
                order.append(',');
            }
            order.append(label);
        }

        String getOrder() {
            String result = order.toString();
            order.setLength(0);
            return result;
        }
    }
}
