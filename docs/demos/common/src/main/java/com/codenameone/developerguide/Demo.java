package com.codenameone.developerguide;

import com.codename1.ui.Form;

/**
 * Represents a standalone demo that can be launched from the demo browser.
 */
public interface Demo {
    /**
     * @return The title used to identify this demo to the user.
     */
    String getTitle();

    /**
     * @return A short description that is displayed in the demo browser.
     */
    String getDescription();

    /**
     * Launches the demo, optionally using the supplied parent form to return
     * to when the demo is closed.
     *
     * @param parent The form that launched this demo.
     */
    void show(Form parent);
}
