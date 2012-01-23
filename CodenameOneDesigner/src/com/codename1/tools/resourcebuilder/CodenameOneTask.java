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

import com.codename1.ui.Display;
import com.codename1.ui.util.EditableResources;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.MatchingTask;

/**
 * New version of the resource builder ant task is now based on the resource editor
 * to allow a single code base for all persistence related resource file manipulations.
 *
 * @author Shai Almog
 */
public class CodenameOneTask extends MatchingTask {
    /**
     * Magic numbers to prevent data corruption
     */
    static final byte MAGIC_IMAGE = (byte)0xF3;
    static final byte MAGIC_PACKED_IMAGE8 = (byte)0xF4;
    static final byte MAGIC_PACKED_IMAGE16 = (byte)0xF5;
    static final byte MAGIC_FONT = (byte)0xF6;
    static final byte MAGIC_THEME = (byte)0xF2;
    static final byte MAGIC_ANIMATION = (byte)0xF8;
    static final byte MAGIC_L10N = (byte)0xF9;
    static final byte MAGIC_DATA = (byte)0xFA;

    private File dest;
    private List<ResourceTask> resources = new ArrayList<ResourceTask>();

    private boolean changed() {
        long l = dest.lastModified();
        for(ResourceTask task : resources) {
            File[] changes = task.getFiles();
            if(changes != null) {
                for(File current : changes) {
                    if(current.lastModified() > l) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void execute() throws BuildException {
        System.out.println("LWUITBuilder generating resource file");
        DataOutputStream resFile = null;
        try {
            if (dest == null) {
                throw new BuildException("dest must be specified it is the output .res resource");
            }

            // if no source file has changed don't do anything
            if(dest.exists() && (!changed())) {
                System.out.println("Nothing to do for " + dest);
                return;
            }
            System.out.println("Processing " + dest);

            Display.init(null);
            EditableResources output = new EditableResources();
            for(ResourceTask task : resources) {
                task.addToResources(output);
            }

            resFile = new DataOutputStream(new FileOutputStream(dest));
            output.save(resFile);
            resFile.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new BuildException("Error in building file", ex);
        }
    }

    /**
     * Adds font element
     */
    public void addFont(FontTask fontTask) {
        resources.add(fontTask);
    }

    public void addImage(ImageTask task) {
        resources.add(task);
    }

    public void addSvg(SvgTask task) {
        resources.add(task);
    }

    /**
     * Adds font element
     */
    public void addTheme(ThemeTask task) {
        resources.add(task);
    }

    public void addL10n(L10NTask task) {
        resources.add(task);
    }

    public void addData(DataTask task) {
        resources.add(task);
    }

    public File getDest() {
        return dest;
    }

    public void setDest(File dest) {
        this.dest = dest;
    }

}
