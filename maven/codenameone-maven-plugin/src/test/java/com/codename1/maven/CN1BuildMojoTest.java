package com.codename1.maven;

import com.codename1.ant.SortedProperties;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CN1BuildMojoTest {

    @Test
    public void mergeRequiredPropertiesAllowsLowerJavaVersionLibrary() throws Exception {
        CN1BuildMojo mojo = new CN1BuildMojo();
        Method method = CN1BuildMojo.class.getDeclaredMethod("mergeRequiredProperties", String.class, Properties.class, Properties.class);
        method.setAccessible(true);

        SortedProperties projectProps = new SortedProperties();
        projectProps.setProperty("codename1.arg.java.version", "17");

        SortedProperties libProps = new SortedProperties();
        libProps.setProperty("codename1.arg.java.version", "8");

        SortedProperties merged = (SortedProperties) method.invoke(mojo, "test-lib", libProps, projectProps);
        assertEquals("17", merged.getProperty("codename1.arg.java.version"));
    }

    @Test
    public void mergeRequiredPropertiesStillFailsOnOtherConflicts() throws Exception {
        CN1BuildMojo mojo = new CN1BuildMojo();
        Method method = CN1BuildMojo.class.getDeclaredMethod("mergeRequiredProperties", String.class, Properties.class, Properties.class);
        method.setAccessible(true);

        SortedProperties projectProps = new SortedProperties();
        projectProps.setProperty("codename1.arg.java.version", "17");
        projectProps.setProperty("codename1.arg.test", "project");

        SortedProperties libProps = new SortedProperties();
        libProps.setProperty("codename1.arg.java.version", "8");
        libProps.setProperty("codename1.arg.test", "lib");

        try {
            method.invoke(mojo, "test-lib", libProps, projectProps);
            fail("Expected a property conflict exception");
        } catch (InvocationTargetException ex) {
            assertTrue(ex.getCause().getMessage().contains("Property codename1.arg.test has a conflict"));
        }
    }
}
