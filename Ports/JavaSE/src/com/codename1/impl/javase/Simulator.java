/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.impl.javase;

import com.codename1.ui.Display;
import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.StringTokenizer;

/**
 * A simple class that can invoke a lifecycle object to allow it to run a Codename One application.
 * Classes are loaded with a classloader so the UI skin can be updated and the lifecycle 
 * objects reloaded.
 *
 * @author Shai Almog
 */
public class Simulator {

    /**
     * Accepts the classname to launch
     */
    public static void main(String[] argv) throws Exception {
        FixedJavaSoundRenderer r = new FixedJavaSoundRenderer();
        r.usurpControlFromJavaSoundRenderer();
        
        String skin = System.getProperty("dskin");
        if (skin == null) {
            System.setProperty("dskin", "/iphone3gs.skin");
        }
        StringTokenizer t = new StringTokenizer(System.getProperty("java.class.path"), File.pathSeparator);
        System.setProperty("MainClass", argv[0]);
        URL[] urls = new URL[t.countTokens()];
        for (int iter = 0; iter < urls.length; iter++) {
            File dir = new File(t.nextToken());
            urls[iter] = dir.toURI().toURL();
        }
        URLClassLoader ldr = new URLClassLoader(urls);
        Class c = ldr.loadClass("com.codename1.impl.javase.Executor");
        Method m = c.getDeclaredMethod("main", String[].class);
        m.invoke(null, new Object[]{argv});        
    }

    public static class FixedJavaSoundRenderer extends com.sun.media.renderer.audio.JavaSoundRenderer {

        public void usurpControlFromJavaSoundRenderer() {
            final String OFFENDING_RENDERER_PLUGIN_NAME = com.sun.media.renderer.audio.JavaSoundRenderer.class.getName();
            javax.media.Format[] rendererInputFormats = javax.media.PlugInManager.getSupportedInputFormats(OFFENDING_RENDERER_PLUGIN_NAME, javax.media.PlugInManager.RENDERER);
            javax.media.Format[] rendererOutputFormats = javax.media.PlugInManager.getSupportedOutputFormats(OFFENDING_RENDERER_PLUGIN_NAME, javax.media.PlugInManager.RENDERER);
            //should be only rendererInputFormats
            if (rendererInputFormats != null || rendererOutputFormats != null) {
                final String REPLACEMENT_RENDERER_PLUGIN_NAME = FixedJavaSoundRenderer.class.getName();
                javax.media.PlugInManager.removePlugIn(OFFENDING_RENDERER_PLUGIN_NAME, javax.media.PlugInManager.RENDERER);
                javax.media.PlugInManager.addPlugIn(REPLACEMENT_RENDERER_PLUGIN_NAME, rendererInputFormats, rendererOutputFormats, javax.media.PlugInManager.RENDERER);
            }
        }

        @Override
        protected com.sun.media.renderer.audio.device.AudioOutput createDevice(javax.media.format.AudioFormat format) {
            return new com.sun.media.renderer.audio.device.JavaSoundOutput() {

                @Override
                public void setGain(double g) {
                    g = Math.max(g, this.gc.getMinimum());
                    g = Math.min(g, this.gc.getMaximum());
                    super.setGain(g);
                }
            };
        }
    }
}
