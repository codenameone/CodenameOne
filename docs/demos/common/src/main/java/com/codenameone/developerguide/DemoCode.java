package com.codenameone.developerguide;

import com.codename1.system.Lifecycle;
import com.codename1.annotations.buildhints.*;

/**
 * Application entry point that launches the demo browser.
 */
@Ios(newStorageLocation = true)
public class DemoCode extends Lifecycle {
    @Override
    public void runApp() {
        new DemoBrowserForm().show();
    }
}
