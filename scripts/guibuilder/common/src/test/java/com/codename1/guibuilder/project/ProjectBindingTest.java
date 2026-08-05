package com.codename1.guibuilder.project;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProjectBindingTest {
    @Test
    void parsesModernProjectBinding() {
        ProjectBinding binding = ProjectBinding.parse("# binding\n"
                + "projectDir=/tmp/app/common\n"
                + "guiDir=/tmp/app/common/src/main/guibuilder\n"
                + "sourceDir=/tmp/app/common/src/main/java\n"
                + "cssFile=/tmp/app/common/src/main/css/theme.css\n"
                + "initialForm=com.example.Login\n");
        assertTrue(binding.isValid());
        assertEquals("com.example.Login", binding.initialForm());
        assertEquals("/tmp/app/common/src/main/css/theme.css", binding.cssFile());
    }
}
