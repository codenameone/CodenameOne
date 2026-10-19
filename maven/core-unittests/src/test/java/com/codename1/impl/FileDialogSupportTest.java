package com.codename1.impl;

import com.codename1.io.filechooser.FileTypeFilter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileDialogSupportTest {

    @Test
    void withExtensionAppendsTheFiltersFirstExtensionOnlyWhenMissing() {
        FileTypeFilter pdf = new FileTypeFilter("PDF").extensions("pdf", "PDFA");
        assertEquals("r.pdf", FileDialogSupport.withExtension("r", pdf));
        assertEquals("r.PDF", FileDialogSupport.withExtension("r.PDF", pdf));
        assertEquals("r.pdfa", FileDialogSupport.withExtension("r.pdfa", pdf));
        assertEquals("r.txt.pdf", FileDialogSupport.withExtension("r.txt", pdf));
        assertEquals("r", FileDialogSupport.withExtension("r", null));
        assertEquals("r", FileDialogSupport.withExtension("r", new FileTypeFilter("Any")));
    }
}
