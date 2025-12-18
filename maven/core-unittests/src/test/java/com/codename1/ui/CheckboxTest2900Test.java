package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.plaf.DefaultLookAndFeel;
import com.codename1.ui.plaf.UIManager;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CheckboxTest2900Test extends UITestBase {

    @FormTest
    void checkboxesToggleSelectionWithOppositeSide() {
        UIManager manager = UIManager.getInstance();
        manager.setLookAndFeel(new DefaultLookAndFeel(manager));

        Form form = new Form("Hi >>0 World", new BorderLayout(BorderLayout.CENTER_BEHAVIOR_CENTER_ABSOLUTE));
        Container panel = new Container(new FlowLayout());
        form.add(BorderLayout.CENTER, panel);

        List<CheckBox> checkBoxes = new ArrayList<CheckBox>();
        for (int i = 0; i < 20; i++) {
            CheckBox checkBox = new CheckBox("Test Checkbox #" + i);
            checkBox.setSelected(true);
            checkBox.setOppositeSide((i & 1) == 0);
            panel.add(checkBox);
            checkBoxes.add(checkBox);
        }

        form.show();
        DisplayTest.flushEdt();
        flushSerialCalls();
        DisplayTest.flushEdt();

        CheckBox oppositeSide = checkBoxes.get(0);
        CheckBox regularSide = checkBoxes.get(1);

        assertTrue(oppositeSide.isOppositeSide(), "Even indexed checkboxes should use the opposite side");
        assertFalse(regularSide.isOppositeSide(), "Odd indexed checkboxes should keep the default side");

        assertTrue(oppositeSide.isSelected(), "Opposite side checkbox starts selected");
        assertTrue(regularSide.isSelected(), "Regular checkbox starts selected");

        tapComponent(oppositeSide);

        tapComponent(regularSide);

        assertFalse(oppositeSide.isSelected(), "Pointer events should toggle the opposite side checkbox");
        assertFalse(regularSide.isSelected(), "Pointer events should toggle the regular checkbox");

        tapComponent(oppositeSide);
        tapComponent(regularSide);

        assertTrue(oppositeSide.isSelected(), "Second tap should re-select the opposite side checkbox");
        assertTrue(regularSide.isSelected(), "Second tap should re-select the regular checkbox");
    }

    @FormTest
    void drawImagePreservesClipAndScales() {
        Image source = Image.createImage(4, 4, 0xffff0000);
        Image target = Image.createImage(16, 16, 0xff00ff00);

        Graphics graphics = target.getGraphics();
        graphics.setClip(2, 3, 10, 8);
        int[] originalClip = graphics.getClip();

        drawImage(graphics, source, 2, 3, 14, 11, 0, 0, 4, 4);

        int[] clipAfterDraw = graphics.getClip();
        assertArrayEquals(originalClip, clipAfterDraw, "Custom drawImage should restore the original clip");

        graphics.setColor(0xff112233);
        graphics.fillRect(0, 0, target.getWidth(), target.getHeight());

        int unchangedPixel = target.getRGB()[0];
        int insideClipPixel = target.getRGB()[target.getWidth() * 4 + 4];

        assertEquals(0xff00ff00, unchangedPixel, "Pixels outside the clip should remain unchanged");
        assertEquals(0xff112233, insideClipPixel, "Drawing after the helper should respect the restored clip region");
    }

    private void drawImage(Graphics gc, Image im,
                           int dx, int dy, int dx2, int dy2,
                           int fx, int fy, int fx2, int fy2) {
        if (gc != null) {
            int w = dx2 - dx;
            int h = dy2 - dy;
            int sw = fx2 - fx;
            int sh = fy2 - fy;
            int imw = im.getWidth();
            int imh = im.getHeight();
            double xscale = w / (double) sw;
            double yscale = h / (double) sh;
            int[] clip = gc.getClip();

            if (clip != null && clip.length >= 4) {
                gc.clipRect(dx, dy, w, h);
                int finx = dx - (int) (fx * xscale);
                int finy = dy - (int) (fy * yscale);
                int finw = (int) (imw * xscale);
                int finh = (int) (imh * yscale);
                gc.drawImage(im, finx, finy, finw, finh);
                gc.setClip(clip);
            }
        }
    }
}
