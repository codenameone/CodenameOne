/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.maven.help;

import java.awt.Desktop;
import java.net.URI;

/**
 * Opens a URL in the user's browser. Abstracted behind an interface so the "Get help"
 * flow can be unit-tested without actually launching a browser, and so headless
 * environments degrade gracefully.
 */
public interface BrowserOpener {

    /** @return true if the browser was launched, false if it could not be (headless, etc.). */
    boolean open(String url);

    /** The real implementation, backed by {@link java.awt.Desktop}. Never throws. */
    BrowserOpener DESKTOP = new BrowserOpener() {
        @Override
        public boolean open(String url) {
            if (url == null || url.length() == 0) {
                return false;
            }
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(url));
                    return true;
                }
            } catch (Throwable ignore) {
                // Headless / no browser / sandbox — caller prints the URL as a fallback.
            }
            return false;
        }
    };
}
