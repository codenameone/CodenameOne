package com.codenameone.developerguide.animations;

import com.codename1.ui.Button;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Form;
import com.codename1.ui.Picker;
import com.codename1.ui.Style;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.Toolbar;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.animations.FlipTransition;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.developerguide.Demo;

/**
 * Demonstrates slide transitions.
 */
public class SlideTransitionsDemo implements Demo {
    @Override
    public String getTitle() {
        return "Slide Transitions";
    }

    @Override
    public String getDescription() {
        return "Allows picking different slide transitions.";
    }

    @Override
    public void show(Form parent) {
        Form[] hiRef = new Form[1];
        runTransitionsDemo(hiRef);
        Form hi = hiRef[0];
        if (parent != null && hi != null) {
            hi.getToolbar().addCommandToLeftBar("Back", null, e -> parent.showBack());
        }
    }

    private void runTransitionsDemo(Form[] hiRef) {
        // tag::slideTransitions[]
        Toolbar.setGlobalToolbar(true);
        Form hi = new Form("Transitions", new BoxLayout(BoxLayout.Y_AXIS));
        Style bg = hi.getContentPane().getUnselectedStyle();
        bg.setBgTransparency(255);
        bg.setBgColor(0xff0000);
        Button showTransition = new Button("Show");
        Picker pick = new Picker();
        pick.setStrings("Slide", "SlideFade", "Cover", "Uncover", "Fade", "Flip");
        pick.setSelectedString("Slide");
        TextField duration = new TextField("10000", "Duration", 6, TextArea.NUMERIC);
        CheckBox horizontal = CheckBox.createToggle("Horizontal");
        pick.addActionListener((e) -> {
            String s = pick.getSelectedString().toLowerCase();
            horizontal.setEnabled(s.equals("slide") || s.indexOf("cover") > -1);
        });
        horizontal.setSelected(true);
        hi.add(showTransition).
            add(pick).
            add(duration).
            add(horizontal);
    
        Form dest = new Form("Destination");
        bg = dest.getContentPane().getUnselectedStyle();
        bg.setBgTransparency(255);
        bg.setBgColor(0xff);
        dest.setBackCommand(
                dest.getToolbar().addCommandToLeftBar("Back", null, (e) -> hi.showBack()));
    
        showTransition.addActionListener((e) -> {
            int h = CommonTransitions.SLIDE_HORIZONTAL;
            if(!horizontal.isSelected()) {
                h = CommonTransitions.SLIDE_VERTICAL;
            }
            switch(pick.getSelectedString()) {
                case "Slide":
                    hi.setTransitionOutAnimator(CommonTransitions.createSlide(h, true, duration.getAsInt(3000)));
                    dest.setTransitionOutAnimator(CommonTransitions.createSlide(h, true, duration.getAsInt(3000)));
                    break;
                case "SlideFade":
                    hi.setTransitionOutAnimator(CommonTransitions.createSlideFadeTitle(true, duration.getAsInt(3000)));
                    dest.setTransitionOutAnimator(CommonTransitions.createSlideFadeTitle(true, duration.getAsInt(3000)));
                    break;
                case "Cover":
                    hi.setTransitionOutAnimator(CommonTransitions.createCover(h, true, duration.getAsInt(3000)));
                    dest.setTransitionOutAnimator(CommonTransitions.createCover(h, true, duration.getAsInt(3000)));
                    break;
                case "Uncover":
                    hi.setTransitionOutAnimator(CommonTransitions.createUncover(h, true, duration.getAsInt(3000)));
                    dest.setTransitionOutAnimator(CommonTransitions.createUncover(h, true, duration.getAsInt(3000)));
                    break;
                case "Fade":
                    hi.setTransitionOutAnimator(CommonTransitions.createFade(duration.getAsInt(3000)));
                    dest.setTransitionOutAnimator(CommonTransitions.createFade(duration.getAsInt(3000)));
                    break;
                case "Flip":
                    hi.setTransitionOutAnimator(new FlipTransition(-1, duration.getAsInt(3000)));
                    dest.setTransitionOutAnimator(new FlipTransition(-1, duration.getAsInt(3000)));
                    break;
            }
            dest.show();
        });
        hi.show();
        // end::slideTransitions[]
        hiRef[0] = hi;
    }
}
