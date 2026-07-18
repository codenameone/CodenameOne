package com.codename1.flutter.material;

import com.codename1.flutter.RenderElement;
import com.codename1.flutter.widgets.Icon;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Component;
import com.codename1.ui.FontImage;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;

/**
 * Leaf render box for the material {@link FloatingActionButton}, owning a
 * real CN1 {@code com.codename1.components.FloatingActionButton} created via
 * {@code createFAB(char)} and positioned absolutely by the parent Scaffold
 * (no bindFabToContainer — the flat Flutter layout places it directly).
 *
 * <p>The Icon child is consumed as configuration rather than mounted as a
 * child element; {@code tooltip} is stored but not rendered in M1 (CN1 has
 * no hover tooltips on touch platforms).</p>
 */
public class FabRenderElement extends RenderElement {

    public FabRenderElement(FloatingActionButton widget) {
        super(widget);
    }

    private FloatingActionButton fab() {
        return (FloatingActionButton) widget();
    }

    private char iconChar() {
        if (fab().getChild() instanceof Icon) {
            Icon ic = (Icon) fab().getChild();
            if (ic.getIcon() != null) {
                return ic.getIcon().codePoint();
            }
        }
        return FontImage.MATERIAL_ADD;
    }

    @Override
    protected Component createComponent() {
        com.codename1.components.FloatingActionButton b =
                com.codename1.components.FloatingActionButton.createFAB(iconChar());
        // The listener reads the CURRENT widget config so onPressed updates
        // never require listener rewiring.
        b.addActionListener(new ActionListener<ActionEvent>() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                dart.runtime.Funcs.VoidFunc0 f = fab().getOnPressed();
                if (f != null) {
                    f.call();
                }
            }
        });
        return b;
    }

    @Override
    protected void updateComponent(Component c) {
        FontImage.setMaterialIcon((com.codename1.components.FloatingActionButton) c, iconChar(),
                com.codename1.components.FloatingActionButton.getIconDefaultSize());
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        Component c = component();
        if (c == null) {
            return constraints.smallest();
        }
        Dimension d = c.getPreferredSize();
        return constraints.constrain(new Size(d.getWidth(), d.getHeight()));
    }
}
