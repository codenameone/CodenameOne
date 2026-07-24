package com.codename1.flutter.widgets;

import com.codename1.flutter.BoxDecoration;
import com.codename1.flutter.BoxShape;
import com.codename1.flutter.Color;
import com.codename1.ui.Component;
import com.codename1.ui.plaf.RoundBorder;

/**
 * Applies a background color and (best-effort) shape from a Flutter
 * {@link BoxDecoration} or plain {@link Color} onto a CN1 component's style.
 * Shared by {@link ColoredBoxRenderElement}, {@link DecoratedBoxRenderElement}
 * and {@link ContainerRenderElement}.
 *
 * <p>Only color and circle shape are honored for this milestone; gradients,
 * borders, border radii, shadows and decoration images are not yet painted.</p>
 */
final class FlutterBoxStyle {

    private FlutterBoxStyle() {
    }

    /**
     * Styles {@code face} from an explicit {@code color} and/or a
     * {@code decoration} (expected to be a {@link BoxDecoration}). The explicit
     * color wins when both are present, matching Flutter (which forbids both).
     */
    static void apply(Component face, Color color, Object decoration) {
        try {
            Color bg = color;
            BoxShape shape = BoxShape.rectangle;
            if (decoration instanceof BoxDecoration) {
                BoxDecoration d = (BoxDecoration) decoration;
                if (bg == null) {
                    bg = d.getColor();
                }
                shape = d.getShape();
            }
            if (shape == BoxShape.circle && bg != null) {
                face.getAllStyles().setBorder(
                        RoundBorder.create().color(bg.rgb()).opacity(bg.alpha()));
                face.getAllStyles().setBgTransparency(0);
                return;
            }
            if (bg != null) {
                face.getAllStyles().setBgColor(bg.rgb());
                face.getAllStyles().setBgTransparency(bg.alpha());
            } else {
                face.getAllStyles().setBgTransparency(0);
            }
        } catch (Exception err) {
            // styling is best-effort; layout must survive regardless
        }
    }

    /**
     * True when the given color/decoration would paint anything — used to
     * decide whether a face component is worth creating.
     */
    static boolean paints(Color color, Object decoration) {
        if (color != null) {
            return true;
        }
        if (decoration instanceof BoxDecoration) {
            BoxDecoration d = (BoxDecoration) decoration;
            return d.getColor() != null || d.getGradient() != null
                    || d.getBorder() != null || d.getBoxShadow() != null;
        }
        return decoration != null;
    }
}
