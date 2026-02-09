package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Sheet;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.util.UITimer;

public class SheetScreenshotTest extends BaseTest {
    private Form form;
    private Sheet sheet;
    private Sheet childSheet;

    @Override
    public boolean runTest() {
        form = createForm("Sheet Test", new BorderLayout(), "Sheet");
        form.add(BorderLayout.CENTER, new Label("Tap sheet to dismiss"));
        sheet = createSheet(null, "Sheet");
        form.show();
        return true;
    }

    @Override
    protected void registerReadyCallback(Form parent, Runnable run) {
        sheet.show();
        UITimer.timer(500, false, parent, () -> {
            childSheet = createSheet(sheet, "Details");
            childSheet.show();
            UITimer.timer(600, false, parent, run);
        });
    }

    private Sheet createSheet(Sheet parent, String title) {
        Sheet newSheet = new Sheet(parent, title);
        Container content = newSheet.getContentPane();
        content.setLayout(BoxLayout.y());
        content.add(new Label("Sheet content"));
        content.add(new Button("Primary Action"));
        content.add(new Label("Secondary details"));
        newSheet.getCommandsContainer().add(new Button("Edit"));
        return newSheet;
    }
}
