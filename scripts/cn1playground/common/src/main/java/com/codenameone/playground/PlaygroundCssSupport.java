package com.codenameone.playground;

import com.codename1.ui.Component;
import com.codename1.ui.Container;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

final class PlaygroundCssSupport {
    private PlaygroundCssSupport() {
    }

    static String normalizeCustomCss(String css) {
        if (css == null) {
            return "";
        }
        String trimmed = css.trim();
        return trimmed.length() == 0 ? "" : trimmed;
    }

    static List<String> collectVisibleUiids(Component root) {
        if (root == null) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> out = new LinkedHashSet<String>();
        collectVisibleUiids(root, out);
        return new ArrayList<String>(out);
    }

    private static void collectVisibleUiids(Component component, LinkedHashSet<String> uiids) {
        if (component == null || !component.isVisible()) {
            return;
        }
        String uiid = component.getUIID();
        if (uiid != null) {
            String cleaned = uiid.trim();
            if (cleaned.length() > 0) {
                uiids.add(cleaned);
            }
        }
        if (component instanceof Container) {
            Container container = (Container) component;
            for (int i = 0; i < container.getComponentCount(); i++) {
                collectVisibleUiids(container.getComponentAt(i), uiids);
            }
        }
    }
}
