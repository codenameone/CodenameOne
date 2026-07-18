package com.codename1.flutter.material;

import com.codename1.flutter.RenderElement;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;

import dart.runtime.Funcs;

/**
 * Leaf render box for {@link Checkbox}: a CN1 CheckBox (UIID
 * "FlutterCheckbox") with controlled semantics — the user's toggle fires
 * {@code onChanged} and the component is immediately re-snapped to the
 * widget's configured value; only a rebuild with a new value moves it.
 * Material tap target: 48x48lp minimum.
 */
public class CheckboxRenderElement extends RenderElement {

    /** Material minimum tap target in logical pixels. */
    public static final double TAP_TARGET_LP = 48;

    private boolean applying;

    public CheckboxRenderElement(Checkbox widget) {
        super(widget);
    }

    private Checkbox checkbox() {
        return (Checkbox) widget();
    }

    /**
     * The value the current widget configuration mandates.
     */
    public boolean configuredValue() {
        return checkbox().getValue();
    }

    @Override
    protected Component createComponent() {
        if (!Display.isInitialized()) {
            // headless unit tests: no CN1 components can exist
            return null;
        }
        CheckBox cb = new CheckBox();
        cb.setUIID("FlutterCheckbox");
        cb.addActionListener(new ActionListener<ActionEvent>() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (applying) {
                    return;
                }
                // CN1 already flipped the component; report the flip, then
                // snap back to the controlled value.
                userToggled(((CheckBox) component()).isSelected());
            }
        });
        apply(cb);
        return cb;
    }

    @Override
    protected void updateComponent(Component c) {
        apply((CheckBox) c);
    }

    private void apply(CheckBox cb) {
        applying = true;
        try {
            cb.setSelected(configuredValue());
        } finally {
            applying = false;
        }
    }

    /**
     * Controlled toggle entry point (public so headless tests can drive it):
     * fires onChanged with the attempted value, then re-applies the widget's
     * configured value to the component.
     */
    public void userToggled(boolean attemptedValue) {
        Funcs.VoidFunc1<Boolean> f = checkbox().getOnChanged();
        if (f != null) {
            f.call(attemptedValue);
        }
        Component c = component();
        if (c != null) {
            apply((CheckBox) c);
        }
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        double min = Dp.px(TAP_TARGET_LP);
        double w = min;
        double h = min;
        Component c = component();
        if (c != null) {
            Dimension d = c.getPreferredSize();
            w = Math.max(w, d.getWidth());
            h = Math.max(h, d.getHeight());
        }
        return constraints.constrain(new Size(w, h));
    }
}
