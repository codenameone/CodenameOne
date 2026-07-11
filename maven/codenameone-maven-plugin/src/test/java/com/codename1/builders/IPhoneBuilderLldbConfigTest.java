package com.codename1.builders;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class IPhoneBuilderLldbConfigTest {

    @Test
    void generatedSchemesIgnoreParparVmGcStopSignal() {
        String script = IPhoneBuilder.createLldbSchemeSetupScript();

        assertTrue(script.contains("process handle -s false -n false -p true SIGUSR2"));
        assertTrue(script.contains("scheme.launch_action.xml_element.attributes['customLLDBInitFile']"));
        assertTrue(script.contains("scheme.test_action.xml_element.attributes['customLLDBInitFile']"));
        assertTrue(script.contains("$(SRCROOT)/cn1.lldbinit"));
    }
}
