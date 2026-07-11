package com.codename1.maven.buildWrappers;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class BuildIosXcodeProjectMojoTest {

    @Test
    public void cleansBeforeGeneratingXcodeProject() {
        BuildIosXcodeProjectMojo mojo = new BuildIosXcodeProjectMojo();

        assertEquals(Arrays.asList("clean", "package"), mojo.getGoals());
    }
}
