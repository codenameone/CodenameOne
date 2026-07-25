package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Offset;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * Translates its {@code child} by an {@link Offset} expressed as a fraction of
 * the child's own size before painting — Flutter's {@code FractionalTranslation}.
 * This pass hosts the child unshifted; applying the fractional offset at paint
 * time is deferred, so the parameters are captured only for API shape.
 */
public class FractionalTranslation extends StatelessWidget {

    private Offset translation;
    private boolean transformHitTests = true;
    private Widget child;

    public void translation(Offset v) {
        this.translation = v;
    }

    public void transformHitTests(boolean v) {
        this.transformHitTests = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Offset getTranslation() {
        return translation;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Widget build(BuildContext context) {
        com.codename1.flutter.FlutterErrorReport.unimplemented("FractionalTranslation", "translation is ignored");
        return child;
    }
}
