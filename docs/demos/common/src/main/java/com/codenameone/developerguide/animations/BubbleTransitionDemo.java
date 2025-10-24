package com.codenameone.developerguide.animations;

import com.codename1.ui.Button;
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.SpanLabel;
import com.codename1.ui.Style;
import com.codename1.ui.animations.BubbleTransition;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.Border;
import com.codenameone.developerguide.Demo;

/**
 * Demonstrates bubble transition dialog.
 */
public class BubbleTransitionDemo implements Demo {
    @Override
    public String getTitle() {
        return "Bubble Transition";
    }

    @Override
    public String getDescription() {
        return "Shows BubbleTransition applied to a dialog.";
    }

    @Override
    public void show(Form parent) {
        Form[] hiRef = new Form[1];
        runDemo(hiRef);
        if (parent != null && hiRef[0] != null) {
            hiRef[0].getToolbar().addCommandToLeftBar("Back", null, e -> parent.showBack());
        }
    }

    private void runDemo(Form[] hiRef) {
        // tag::bubbleTransition[]
        Form hi = new Form("Bubble");
        Button showBubble = new Button("+");
        showBubble.setName("BubbleButton");
        Style buttonStyle = showBubble.getAllStyles();
        buttonStyle.setBorder(Border.createEmpty());
        buttonStyle.setFgColor(0xffffff);
        buttonStyle.setBgPainter((g, rect) -> {
            g.setColor(0xff);
            int actualWidth = rect.getWidth();
            int actualHeight = rect.getHeight();
            int xPos, yPos;
            int size;
            if(actualWidth > actualHeight) {
                yPos = rect.getY();
                xPos = rect.getX() + (actualWidth - actualHeight) / 2;
                size = actualHeight;
            } else {
                yPos = rect.getY() + (actualHeight - actualWidth) / 2;
                xPos = rect.getX();
                size = actualWidth;
            }
            g.setAntiAliased(true);
            g.fillArc(xPos, yPos, size, size, 0, 360);
        });
        hi.add(showBubble);
        hi.setTintColor(0);
        showBubble.addActionListener((e) -> {
            Dialog dlg = new Dialog("Bubbled");
            dlg.setLayout(new BorderLayout());
            SpanLabel sl = new SpanLabel("This dialog should appear with a bubble transition from the button", "DialogBody");
            sl.getTextUnselectedStyle().setFgColor(0xffffff);
            dlg.add(BorderLayout.CENTER, sl);
            dlg.setTransitionInAnimator(new BubbleTransition(500, "BubbleButton"));
            dlg.setTransitionOutAnimator(new BubbleTransition(500, "BubbleButton"));
            dlg.setDisposeWhenPointerOutOfBounds(true);
            dlg.getTitleStyle().setFgColor(0xffffff);
    
            Style dlgStyle = dlg.getDialogStyle();
            dlgStyle.setBorder(Border.createEmpty());
            dlgStyle.setBgColor(0xff);
            dlgStyle.setBgTransparency(0xff);
            dlg.showPacked(BorderLayout.NORTH, true);
        });
    
        hi.show();
        // end::bubbleTransition[]
        hiRef[0] = hi;
    }
}
