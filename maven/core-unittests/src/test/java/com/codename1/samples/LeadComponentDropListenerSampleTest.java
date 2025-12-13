package com.codename1.samples;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.*;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.plaf.Border;
import static org.junit.jupiter.api.Assertions.*;

public class LeadComponentDropListenerSampleTest extends UITestBase {
    boolean dropped = false;

    @FormTest
    public void testLeadComponentDropListener() {
        Form hi = new Form("Hi World", new GridLayout(1, 2));
        hi.setScrollable(false);
        Container draggable = new Container(new FlowLayout());
        Label lead = new Label("Draggable");
        draggable.add(lead);
        draggable.setLeadComponent(lead);

        draggable.getStyle().setBorder(Border.createLineBorder(1, 0xff0000));
        draggable.setDraggable(true);
        lead.addDropListener(evt->{
            dropped = true;
        });

        Container dropTarget = new Container(new FlowLayout())  {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                g.setColor(0x0);
                g.setFont(Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
                g.drawString("Drop Target", 0, 0);
            }
        };
        dropTarget.getStyle().setBorder(Border.createLineBorder(1, 0x00ff00));
        dropTarget.setDropTarget(true);

        dropTarget.add(new Label("DropTarget"));
        hi.addAll(draggable, dropTarget);
        hi.show();

        // Simulate Drag and Drop
        // 1. Pointer press on draggable (lead)
        // 2. Pointer drag to dropTarget
        // 3. Pointer release

        // Coordinates
        int dragX = lead.getAbsoluteX() + lead.getWidth() / 2;
        int dragY = lead.getAbsoluteY() + lead.getHeight() / 2;

        int dropX = dropTarget.getAbsoluteX() + dropTarget.getWidth() / 2;
        int dropY = dropTarget.getAbsoluteY() + dropTarget.getHeight() / 2;

        // Use implementation directly or fire events?
        // UITestBase provides display.
        // We can use Form.pointerPressed/Dragged/Released

        hi.pointerPressed(dragX, dragY);
        // Drag a bit to initiate drag
        hi.pointerDragged(dragX + 10, dragY + 10);
        // Drag to target
        hi.pointerDragged(dropX, dropY);
        hi.pointerReleased(dropX, dropY);

        assertTrue(dropped, "Drop listener should have been triggered");
    }
}
