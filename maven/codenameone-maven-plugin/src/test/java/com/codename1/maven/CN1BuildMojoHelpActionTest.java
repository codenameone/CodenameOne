/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.maven;

import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;

public class CN1BuildMojoHelpActionTest {

    @Test
    public void reproductionUsesSupportedPackageLifecycle() throws Exception {
        CN1BuildMojo mojo = new CN1BuildMojo();
        setField(mojo, "platform", "ios");
        setField(mojo, "buildTarget", "ios-device-release");

        assertEquals("mvn package -Dcodename1.platform=ios"
                        + " -Dcodename1.buildTarget=ios-device-release",
                mojo.helpAction());
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
