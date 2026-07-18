package com.codename1.flutter.material;

import com.codename1.flutter.Element;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.TextArea;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.geom.Dimension;

import dart.runtime.Funcs;

/**
 * Leaf render box for {@link TextField}: owns a CN1
 * {@link com.codename1.ui.TextField} (UIID "FlutterTextField").
 *
 * <p>Controller sync is two-way: user edits (DataChangedListener) flow into
 * the bound {@link TextEditingController} and fire {@code onChanged};
 * {@code controller.setText/clear} push back into the component via
 * {@link #applyControllerText}. The {@code applying} guard stops the
 * programmatic push from re-entering the data-changed path.</p>
 *
 * <p>Material geometry: fills the available width, minimum height 48lp. The
 * decoration's labelText renders as the CN1 hint in M3 (hintText is the
 * fallback); a floating label is a later milestone.</p>
 */
public class TextFieldRenderElement extends RenderElement {

    /** Material minimum text-field height in logical pixels. */
    public static final double MIN_HEIGHT_LP = 48;
    /** Intrinsic width when the incoming width is unbounded. */
    public static final double DEFAULT_WIDTH_LP = 200;

    private boolean applying;
    private TextEditingController boundController;

    public TextFieldRenderElement(TextField widget) {
        super(widget);
    }

    private TextField textField() {
        return (TextField) widget();
    }

    @Override
    protected Component createComponent() {
        if (!Display.isInitialized()) {
            // headless unit tests: no CN1 components can exist
            return null;
        }
        com.codename1.ui.TextField tf = new com.codename1.ui.TextField();
        tf.setUIID("FlutterTextField");
        tf.addDataChangedListener(new DataChangedListener() {
            @Override
            public void dataChanged(int type, int index) {
                if (applying) {
                    return;
                }
                userEdited(componentText());
            }
        });
        tf.setDoneListener(new ActionListener<ActionEvent>() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                Funcs.VoidFunc1<String> f = textField().getOnSubmitted();
                if (f != null) {
                    f.call(componentText());
                }
            }
        });
        apply(tf);
        return tf;
    }

    @Override
    protected void updateComponent(Component c) {
        apply((com.codename1.ui.TextField) c);
    }

    @Override
    public void unmount() {
        super.unmount();
        if (boundController != null) {
            boundController.unbind(this);
            boundController = null;
        }
    }

    private void apply(com.codename1.ui.TextField tf) {
        applying = true;
        try {
            TextField w = textField();
            tf.setConstraint(w.isObscureText() ? TextArea.PASSWORD : TextArea.ANY);
            tf.setEditable(w.isEnabled());
            tf.setEnabled(w.isEnabled());
            InputDecoration d = w.getDecoration();
            if (d != null) {
                String hint = d.getLabelText() != null ? d.getLabelText() : d.getHintText();
                tf.setHint(hint == null ? "" : hint);
            }
            rebindController();
            if (boundController != null && !eq(tf.getText(), boundController.text())) {
                tf.setText(boundController.text());
            }
        } finally {
            applying = false;
        }
    }

    private void rebindController() {
        TextEditingController c = textField().getController();
        if (c != boundController) {
            if (boundController != null) {
                boundController.unbind(this);
            }
            boundController = c;
            if (c != null) {
                c.bind(this);
            }
        }
    }

    /**
     * A user edit arrived: sync the controller (which notifies its
     * listeners) and fire onChanged with the new string. Public so headless
     * tests can drive the flow without a component.
     */
    public void userEdited(String newText) {
        rebindController();
        if (boundController != null) {
            boundController.valueFromComponent(newText);
        }
        Funcs.VoidFunc1<String> f = textField().getOnChanged();
        if (f != null) {
            f.call(newText);
        }
    }

    /**
     * The component's live text, or null when headless.
     */
    String componentText() {
        Component c = component();
        return c == null ? null : ((TextArea) c).getText();
    }

    /**
     * Push a programmatic controller value into the component (no
     * data-changed feedback loop).
     */
    void applyControllerText(String v) {
        Component c = component();
        if (c == null) {
            return;
        }
        applying = true;
        try {
            ((TextArea) c).setText(v == null ? "" : v);
        } finally {
            applying = false;
        }
    }

    private static boolean eq(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        Component c = component();
        double prefW = Dp.px(DEFAULT_WIDTH_LP);
        double prefH = Dp.px(MIN_HEIGHT_LP);
        if (c != null) {
            Dimension d = c.getPreferredSize();
            prefW = Math.max(prefW, d.getWidth());
            prefH = Math.max(prefH, d.getHeight());
        }
        double w = constraints.hasBoundedWidth() ? constraints.maxWidth() : prefW;
        double h = Math.max(prefH, Dp.px(MIN_HEIGHT_LP));
        return constraints.constrain(new Size(w, h));
    }
}
