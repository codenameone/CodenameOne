package com.codename1.e2e;

import static com.codename1.ui.CN.*;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;

/**
 * Minimal Codename One lifecycle for the protocol end-to-end client. The UI is
 * incidental -- the actual REST / GraphQL / gRPC round-trips against the
 * Spring Boot test server are exercised by the AbstractTest classes under
 * src/test/java and run via cn1:test.
 */
public class ProtocolE2e {
    private Form current;
    private Resources theme;

    public void init(Object context) {
        theme = UIManager.initFirstTheme("/theme");
    }

    public void start() {
        if (current != null) {
            current.show();
            return;
        }
        Form hi = new Form("Protocol E2E", BoxLayout.y());
        hi.add(new Label("REST / GraphQL / gRPC client"));
        hi.show();
    }

    public void stop() {
        current = getCurrentForm();
        if (current instanceof com.codename1.ui.Dialog) {
            ((com.codename1.ui.Dialog) current).dispose();
            current = getCurrentForm();
        }
    }

    public void destroy() {
    }
}
