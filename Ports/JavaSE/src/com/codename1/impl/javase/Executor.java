/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
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
 * Please contact Codename One through http://www.codenameone.com/ if you 
 * need additional information or have any questions.
 */
package com.codename1.impl.javase;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.push.PushCallback;
import com.codename1.ui.Display;
import java.lang.reflect.Method;

/**
 *
 * @author Shai Almog
 */
public class Executor {
    public static void main(final String[] argv) throws Exception {
        FixedJavaSoundRenderer r = new FixedJavaSoundRenderer();
        r.usurpControlFromJavaSoundRenderer();
        final Class c = Class.forName(argv[0]);
        try {
            Method m = c.getDeclaredMethod("main", String[].class);
            m.invoke(null, new Object[]{null});
        } catch (NoSuchMethodException noMain) {
            try {
                Method m = c.getDeclaredMethod("startApp");
                m.invoke(c.newInstance());
            } catch (NoSuchMethodException noStartApp) {
                if (Display.isInitialized()) {
                    Display.deinitialize();
                }
                final Method m = c.getDeclaredMethod("init", Object.class);
                final Object o = c.newInstance();
                if(o instanceof PushCallback) {
                    CodenameOneImplementation.setPushCallback((PushCallback)o);
                }
                Display.init(null);
                Display.getInstance().callSerially(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            m.invoke(o, new Object[]{null});
                            Method start = c.getDeclaredMethod("start", new Class[0]);
                            start.invoke(o, new Object[0]);
                        } catch (NoSuchMethodException err) {
                            System.out.println("Couldn't find a main or a startup in " + argv[0]);
                        } catch (Exception err) {
                            err.printStackTrace();
                            System.exit(1);
                        }
                    }
                });
            }
        }
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
