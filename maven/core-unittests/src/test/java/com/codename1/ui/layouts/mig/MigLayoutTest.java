package com.codename1.ui.layouts.mig;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.PainterChain;

import static org.junit.jupiter.api.Assertions.*;

class MigLayoutTest extends UITestBase {

    @FormTest
    void testConstraintsPreservedOnComponents() {
        Form form = new Form(new MigLayout("wrap 2", "[100][grow]", "[]"));

        Label first = new Label("First");
        Label second = new Label("Second");
        form.add(first);
        form.add("grow", second);
        form.revalidate();

        MigLayout layout = (MigLayout) form.getContentPane().getLayout();
        assertEquals("wrap 2", layout.getLayoutConstraints());
        assertEquals("[100][grow]", layout.getColumnConstraints());
        assertEquals("[]", layout.getRowConstraints());
        assertEquals("grow", layout.getComponentConstraint(second));
        assertSame(form, MigLayout.findType(Form.class, second));
    }

    @FormTest
    void testInstallAndRemoveGlassPane() {
        Form form = new Form();
        RecordingPainter first = new RecordingPainter();
        RecordingPainter second = new RecordingPainter();

        PainterChain.installGlassPane(form, first);
        assertSame(first, form.getGlassPane());

        PainterChain.installGlassPane(form, second);
        assertTrue(form.getGlassPane() instanceof PainterChain);

        PainterChain.removeGlassPane(form, second);
        assertSame(first, form.getGlassPane());

        PainterChain.removeGlassPane(form, first);
        assertNull(form.getGlassPane());
    }

    private static class RecordingPainter implements com.codename1.ui.Painter {
        private int paintCalls;

        public void paint(com.codename1.ui.Graphics g, com.codename1.ui.geom.Rectangle rect) {
            paintCalls++;
        }

        int getPaintCalls() {
            return paintCalls;
        }
    }
}
