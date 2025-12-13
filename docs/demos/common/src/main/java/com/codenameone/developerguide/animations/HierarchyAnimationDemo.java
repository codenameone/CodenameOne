package com.codenameone.developerguide.animations;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.util.UITimer;
import com.codename1.ui.Display;
import com.codenameone.developerguide.Demo;
import java.util.Vector;

/**
 * Demonstrates animateHierarchyFade.
 */
public class HierarchyAnimationDemo implements Demo {
    @Override
    public String getTitle() {
        return "Hierarchy Animation";
    }

    @Override
    public String getDescription() {
        return "Shows animateHierarchyFade() with components positioned off-screen.";
    }

    @Override
    public void show(Form parent) {
        Form hi = new Form("Hierarchy", BoxLayout.y());
        Container boxContainer = new Container(BoxLayout.y());
        boxContainer.setScrollableY(false);
        Vector<Component> demoComponents = new Vector<>();
        for (int i = 0; i < 6; i++) {
            Label cmp = new Label("Component " + (i + 1));
            demoComponents.add(cmp);
            boxContainer.add(cmp);
        }
        int componentsPerRow = 3;
        hi.add(boxContainer);
        AnimationDemoUtil.show(hi, parent);
        UITimer.timer(50, false, () -> runAnimation(boxContainer, demoComponents, componentsPerRow));
    }

    private void runAnimation(Container boxContainer, Vector<Component> demoComponents, int componentsPerRow) {
        int dw = Display.getInstance().getDisplayWidth();
        // tag::hierarchyAnimation[]
        for(int iter = 0 ; iter < demoComponents.size() ; iter++) {
            Component cmp = (Component)demoComponents.elementAt(iter);
            if(iter < componentsPerRow) {
                cmp.setX(-cmp.getWidth());
            } else {
                if(iter < componentsPerRow * 2) {
                    cmp.setX(dw);
                } else {
                    cmp.setX(-cmp.getWidth());
                }
            }
        }
        boxContainer.setShouldCalcPreferredSize(true);
        boxContainer.animateHierarchyFade(3000, 30);
        // end::hierarchyAnimation[]
    }
}
