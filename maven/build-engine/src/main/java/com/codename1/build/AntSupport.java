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
package com.codename1.build;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.input.DefaultInputHandler;
import org.apache.tools.ant.input.InputHandler;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.taskdefs.Redirector;

import java.io.File;

/// The Ant plumbing the engine runs its zip, unzip and forked-java tasks in.
public final class AntSupport {
    /// Forked output goes to debug.
    public static final int LEVEL_DEBUG = 0;
    /// Forked output goes to info.
    public static final int LEVEL_INFO = 1;
    /// Forked output goes to warn.
    public static final int LEVEL_WARN = 2;
    /// Forked output goes to error.
    public static final int LEVEL_ERROR = 3;
    /// Forked output is dropped.
    public static final int LEVEL_DISABLED = 4;

    private AntSupport() {
    }

    /// A fresh, initialized Ant project rooted at `baseDir` (the working
    /// directory when null).
    public static Project newProject(File baseDir) {
        Project p = new Project();
        p.setBaseDir(baseDir == null ? new File(".") : baseDir);
        p.setDefaultInputStream(System.in);
        InputHandler handler = new DefaultInputHandler();
        p.setProjectReference(handler);
        p.setInputHandler(handler);
        p.init();
        return p;
    }

    /// A `java` task whose output is routed to `log` at `level`, and whose
    /// error output always goes to error.
    public static Java createJava(Project project, final Log log, final int level) {
        Java java = new Java() {
            {
                redirector = new Redirector(this) {
                    @Override
                    protected void handleOutput(String output) {
                        write(log, level, output);
                    }

                    @Override
                    protected void handleErrorOutput(String output) {
                        log.error(output);
                    }
                };
            }

            @Override
            protected void handleOutput(String output) {
                write(log, level, output);
            }

            @Override
            protected void handleErrorOutput(String output) {
                log.error(output);
            }

            @Override
            protected void handleFlush(String output) {
                write(log, level, output);
            }

            @Override
            public void log(String msg) {
                log.info(msg);
            }

            @Override
            public void log(String msg, int msgLevel) {
                log.info(msg);
            }
        };
        java.setProject(project);
        return java;
    }

    private static void write(Log log, int level, String output) {
        switch (level) {
            case LEVEL_DEBUG:
                log.debug(output);
                break;
            case LEVEL_DISABLED:
                break;
            case LEVEL_ERROR:
                log.error(output);
                break;
            case LEVEL_WARN:
                log.warn(output);
                break;
            default:
                log.info(output);
                break;
        }
    }
}
