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

package com.codename1.tools.resourcebuilder;

import com.codename1.ui.util.EditableResources;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import org.apache.tools.ant.BuildException;

/**
 * Adds a localization resource bundle for a set of languages
 *
 * @author Shai Almog
 */
public class L10NTask extends ResourceTask {
    private List<LocaleTask> locales = new ArrayList<LocaleTask>();
        
    public void addLocale(LocaleTask locale) {
        locales.add(locale);
    }
    
    public File[] getFiles() {
        File[] files = new File[locales.size()];
        for(int iter = 0 ; iter < files.length ; iter++) {
            files[iter] = locales.get(iter).getFile();
        }
        return files;
    }

    @Override
    public void addToResources(EditableResources e) throws IOException {
        if(locales.size() == 0) {
            throw new BuildException("L10N task must have at least one locale child element");
        }
        e.setL10N(getName(), new Hashtable());
        for(LocaleTask l : locales) {
            if(l.getName() == null || l.getFile() == null) {
                throw new BuildException("Both name and file attributes of the locale task are required attributes");
            }
            Properties p = new Properties();
            InputStream i = new FileInputStream(l.getFile());
            p.load(i);
            i.close();
            e.addLocale(getName(), l.getName());
            for(Object key : p.keySet()) {
                e.setLocaleProperty(getName(), l.getName(), (String)key, p.getProperty((String)key));
            }
        }
    }
}
