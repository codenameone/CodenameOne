/*
 * Copyright (c) 2020, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.designer;

import java.io.File;
import java.io.IOException;

public class MavenHelper {

    public static boolean isMavenProject(File inputFile)  {
        return new File(getProjectDir(inputFile), "pom.xml").exists();
    }

    public static File getProjectDir(File start) {
        File f = new File(start, "codenameone_settings.properties");

        while (!f.exists() && f.getParentFile().getParentFile() != null) {
            f = new File(f.getParentFile().getParentFile(), "codenameone_settings.properties");
            if (f.exists()) {
                return f.getParentFile();
            }
        }
        return f.exists() ? f.getParentFile() : null;

    }
    
    public static File getCSSDir(File start) {
        File projectDir = getProjectDir(start);
        if (isMavenProject(start)) {
            return new File(projectDir, "src" + File.separator + "main" + File.separator + "css");
        } else {
            return new File(projectDir, "css");
        }
        
    }
}
