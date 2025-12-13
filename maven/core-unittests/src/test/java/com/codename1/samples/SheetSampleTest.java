package com.codename1.samples;

import com.codename1.components.RadioButtonList;
import com.codename1.ui.*;
import com.codename1.ui.layouts.*;
import com.codename1.ui.list.DefaultListModel;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class SheetSampleTest extends UITestBase {

    @FormTest
    public void testSheetSample() {
        Form hi = new Form("Hi World", new BorderLayout());

        RadioButtonList sheetPos = new RadioButtonList(new DefaultListModel(
                BorderLayout.NORTH, BorderLayout.EAST, BorderLayout.SOUTH, BorderLayout.WEST, BorderLayout.CENTER
        ));
        Button b = new Button("Open Sheet");

        b.addActionListener(e->{
            MySheet sheet = new MySheet(null);
            int positionIndex = sheetPos.getModel().getSelectedIndex();
            if (positionIndex >= 0) {
                String pos = (String)sheetPos.getModel().getItemAt(positionIndex);
                sheet.setPosition(pos);
            }
            sheet.show();
        });
        hi.add(BorderLayout.NORTH, BoxLayout.encloseY(sheetPos, b));
        hi.show();

        // Simulate user action: click the button
        b.pressed();
        b.released();

        // Since Sheet.show() shows a dialog/interaction dialog, we can check if the sheet is visible
        // But verifying Sheet logic in headless might be tricky if it depends on animations/interactions.
        // However, we can at least ensure no exception is thrown and structure is created.

        // We can simulate selecting different positions
        for(int i = 0; i < sheetPos.getModel().getSize(); i++) {
            sheetPos.getModel().setSelectedIndex(i);
            b.pressed();
            b.released();
            // Just verifying it doesn't crash
        }
    }

    private class MySheet extends Sheet {
        MySheet(Sheet parent) {
            super(parent, "My Sheet");
            Container cnt = getContentPane();
            cnt.setLayout(BoxLayout.y());
            Button gotoSheet2 = new Button("Goto Sheet 2");
            gotoSheet2.addActionListener(e->{
                new MySheet2(this).show();
            });
            cnt.add(gotoSheet2);
            for (String t : new String[]{"Red", "Green", "Blue", "Orange"}) {
                cnt.add(new Label(t));
            }
        }
    }

    private class MySheet2 extends Sheet {
        MySheet2(Sheet parent) {
            super(parent, "Sheet 2");
            Container cnt = getContentPane();
            cnt.setLayout(BoxLayout.y());
            cnt.setScrollableY(true);
            for (int i=0; i<2; i++) {
                for (String t : new String[]{"Red", "Green", "Blue", "Orange"}) {
                    cnt.add(new Label(t));
                }
            }
        }
    }
}
