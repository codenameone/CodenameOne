package com.codenameone.developerguide;

import com.codename1.system.Lifecycle;

/**
 * Application entry point that launches the demo browser.
 */
public class DemoCode extends Lifecycle {
    @Override
    public void runApp() {
        new DemoBrowserForm().show();
    }
}
