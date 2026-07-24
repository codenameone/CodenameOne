package com.codename1.flutter.material;

import com.codename1.flutter.RenderElement;
import com.codename1.flutter.SingleChildRenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Size;

/**
 * Render element for {@link PopupMenuButton}: lays out the trigger widget
 * ({@code child}, falling back to {@code icon}) as its content. The menu
 * overlay is deferred for this milestone. Owns no CN1 component.
 */
public class PopupMenuButtonRenderElement extends SingleChildRenderElement {

    public PopupMenuButtonRenderElement(PopupMenuButton widget) {
        super(widget);
    }

    private PopupMenuButton button() {
        return (PopupMenuButton) widget();
    }

    @Override
    protected Widget childWidget() {
        return button().getChild() != null ? button().getChild() : button().getIcon();
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        RenderElement child = renderChild();
        if (child == null) {
            return constraints.smallest();
        }
        Size cs = child.layout(constraints.loosen());
        setChildOffset(child, 0, 0);
        return constraints.constrain(cs);
    }
}
