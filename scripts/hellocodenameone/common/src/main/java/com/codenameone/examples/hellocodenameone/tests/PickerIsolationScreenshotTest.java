package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Component;
import com.codename1.ui.ComponentSelector;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.spinner.Picker;
import com.codename1.ui.util.UITimer;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/// Pure-Java isolation repro for the Picker date-wheel-missing bug on the JS port.
/// Shows a Picker in lightweight mode, triggers startEditingAsync, then dumps
/// the resolved Spinner3DRow style + picker popup container dimensions via
/// `CN1SS:DIAG:...` lines. Designed to reveal the root cause hypotheses:
/// (a) rowHeight=0 due to font-metrics bug, (b) spinner container sized 0x0,
/// (c) popup ContentPane missing the spinner subtree entirely.
public class PickerIsolationScreenshotTest extends BaseTest {
    private Picker picker;

    @Override
    public boolean runTest() {
        Form form = createForm("Picker Isolation", BoxLayout.y(), "picker-isolation");
        Date fixed = toDate(LocalDate.of(2026, 4, 11));
        picker = new Picker();
        picker.setType(Display.PICKER_TYPE_DATE);
        picker.setUseLightweightPopup(true);
        picker.setDate(fixed);
        form.add(picker);
        form.show();
        return true;
    }

    @Override
    protected void registerReadyCallback(Form parent, Runnable run) {
        picker.startEditingAsync();
        UITimer.timer(1200, false, parent, () -> {
            Style rowStyle = ComponentSelector.$("Spinner3DRow")
                    .first()
                    != null
                    ? new Label("test", "Spinner3DRow").getUnselectedStyle()
                    : null;
            Label probe = new Label("April", "Spinner3DRow");
            int preferredH = probe.getPreferredH();
            int preferredW = probe.getPreferredW();
            Form current = Display.getInstance().getCurrent();
            System.out.println("CN1SS:DIAG:picker-isolation"
                    + " currentFormClass=" + (current == null ? "null" : current.getClass().getName())
                    + " currentFormTitle=" + (current == null ? "null" : current.getTitle())
                    + " Spinner3DRowPrefH=" + preferredH
                    + " Spinner3DRowPrefW=" + preferredW
                    + " probeFont=" + (rowStyle == null ? "null-style" : String.valueOf(rowStyle.getFont()))
                    + " probeFontHeight=" + (rowStyle == null || rowStyle.getFont() == null
                    ? "null" : rowStyle.getFont().getHeight()));
            dumpHierarchy(current, 0, 15);
            run.run();
        });
    }

    private static void dumpHierarchy(Component c, int depth, int maxDepth) {
        if (c == null || depth > maxDepth) {
            return;
        }
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            indent.append("  ");
        }
        System.out.println("CN1SS:DIAG:picker-tree "
                + indent.toString()
                + c.getClass().getSimpleName()
                + "[" + c.getUIID() + "]"
                + " xywh=" + c.getX() + "," + c.getY() + "," + c.getWidth() + "x" + c.getHeight()
                + " prefWH=" + c.getPreferredW() + "x" + c.getPreferredH()
                + " visible=" + c.isVisible());
        if (c instanceof Container) {
            Container cnt = (Container) c;
            for (int i = 0; i < cnt.getComponentCount(); i++) {
                dumpHierarchy(cnt.getComponentAt(i), depth + 1, maxDepth);
            }
        }
    }

    private static Date toDate(LocalDate date) {
        return new Date(date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }
}
