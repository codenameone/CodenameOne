package com.codenameone.developerguide.advancedtopics;

import com.codename1.ui.Display;

// tag::appArg[]
class AppArgSnippet {
    public void readArgument() {
        String arg = Display.getInstance().getProperty("AppArg");
    }
}
// end::appArg[]
