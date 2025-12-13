package com.codename1.samples;

import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Toolbar;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.spinner.Picker;
import java.util.ArrayList;
import java.util.List;
import static com.codename1.ui.ComponentSelector.$;
import static org.junit.jupiter.api.Assertions.*;

public class VerticalAlignTTFFontTest2798Test extends UITestBase {

    @FormTest
    public void testVerticalAlignTTFFont() {
        Form hi = new Form();
        hi.setToolbar(new Toolbar());
        hi.setTitle("Test Fonts");

        List<FontWrapper> fonts = new ArrayList<>();
        int fontSize = Display.getInstance().convertToPixels(3);

        // We use createSystemFont as fallbacks or assume TTF creation works (mocks might return default font)
        fonts.add(new FontWrapper("System Font Medium", Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM)));
        fonts.add(new FontWrapper("native:MainRegular", Font.createTrueTypeFont(Font.NATIVE_MAIN_REGULAR, Font.NATIVE_MAIN_REGULAR).derive(fontSize, Font.STYLE_PLAIN)));

        for (FontWrapper font : fonts) {
            Button b = new Button(font.name);
            b.addActionListener(e->{
                new ShaiForm(font.name, font.font).show();
            });
            hi.add(b);
        }

        hi.show();
        waitForForm(hi);

        // Click the first button to open the sub-form
        if (hi.getComponentCount() > 0 && hi.getComponentAt(0) instanceof Button) {
            Button b = (Button)hi.getComponentAt(0);
            b.pressed();
            b.released();
            waitFor(200);
            assertTrue(Display.getInstance().getCurrent() instanceof ShaiForm, "Should have navigated to ShaiForm");
        }
    }

    private void waitForForm(Form form) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 3000) {
            if (Display.getInstance().getCurrent() == form) {
                return;
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
        fail("Form did not become current in time");
    }

    private void waitFor(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            // Ignore
        }
    }

    private class FontWrapper {
        String name;
        Font font;

        FontWrapper(String name, Font font) {
            this.name = name;
            this.font = font;
        }
    }

    public class ShaiForm extends Form {
        public ShaiForm(String fontName, Font appFont) {
            setToolbar(new Toolbar());
            setTitle("Test Font "+fontName);
            Form prev = CN.getCurrentForm();
            if (prev != null) {
                setBackCommand("Back", null, e->{
                    prev.showBack();
                });
            }
            setLayout(new BoxLayout(BoxLayout.Y_AXIS));

            FontImage fntImage = FontImage.createFixed("\uE161", FontImage.getMaterialDesignFont(), 0x0, 100, 100);

            Label labelCustomTTF = new Label("custom TTF with g and y");
            Style labelStyle = labelCustomTTF.getAllStyles();
            labelStyle.setFont(appFont);
            labelCustomTTF.setIcon(fntImage);

            Label labelNative = new Label("native Font with g and y");
            labelNative.setIcon(fntImage);

            Label justTextLabel = new Label("Just text with g and y");
            justTextLabel.getStyle().setFont(appFont);

            Picker stringPickerCustom = new Picker();
            Style pickerStyle = stringPickerCustom.getAllStyles();
            pickerStyle.setFont(appFont);
            stringPickerCustom.setType(Display.PICKER_TYPE_STRINGS);
            stringPickerCustom.setStrings("custom TTF Font");
            stringPickerCustom.setText("custom TTF Font");

            Picker stringPickerNative = new Picker();
            stringPickerNative.setType(Display.PICKER_TYPE_STRINGS);
            stringPickerNative.setStrings("native Font");
            stringPickerNative.setText("native Font");

            $(labelCustomTTF, labelNative, justTextLabel, stringPickerCustom, stringPickerNative).selectAllStyles()
                    .setPadding(0);
            add(labelCustomTTF);
            add(labelNative);
            add(justTextLabel);
            add(stringPickerCustom);
            add(stringPickerNative);
        }
    }
}
