package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Icons;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.TargetPlatform;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Icon;

/**
 * The platform-appropriate back-arrow glyph, decoupled from its button —
 * Flutter's {@code BackButtonIcon}: a chevron on iOS/macOS, an arrow elsewhere.
 *
 * <p>It rendered NOTHING until now, which is a costlier omission than it sounds: the
 * gallery builds every demo page's back button as {@code IconButton(icon: BackButtonIcon())},
 * so each of those pages had an invisible — though still tappable — way back.</p>
 */
public class BackButtonIcon extends StatelessWidget {

    @Override
    public Widget build(BuildContext context) {
        return new Icon(isApplePlatform(context) ? Icons.arrow_back_ios : Icons.arrow_back);
    }

    /** Whether the ambient theme targets a platform that uses the chevron. */
    private static boolean isApplePlatform(BuildContext context) {
        try {
            Object p = Theme.of(context).platform();
            return p == TargetPlatform.iOS || p == TargetPlatform.macOS;
        } catch (Throwable t) {
            return false;
        }
    }
}
