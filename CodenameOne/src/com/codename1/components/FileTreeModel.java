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

package com.codename1.components;

import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.ui.tree.TreeModel;
import java.util.Vector;

/**
 * A tree model representing the file system as a whole, notice that this class returns absolute
 * file names which would result in an unreadable tree. To fix this you can create a Tree object
 * and override functionality such as the childToDisplayLabel method like this:
 * <code>
        Tree fileTree = new Tree(new FileTreeModel(true)) {
            protected String childToDisplayLabel(Object child) {
                if (((String) child).endsWith("/")) {
                    return ((String) child).substring(((String) child).lastIndexOf('/', ((String) child).length() - 2));
                }
                return ((String) child).substring(((String) child).lastIndexOf('/'));
            }
        };
   </code>
 *
 * @author Shai Almog
 */
public class FileTreeModel implements TreeModel {
    
    private boolean showFiles;

    private Vector ext;
    
    /**
     * Construct a filesystem tree model
     *
     * @param showFiles indicates whether this is a directory only view or a whole filesystem view
     */
    public FileTreeModel(boolean showFiles) {
        this.showFiles = showFiles;
    }

    /**
     * Shows only files with the given extension
     * 
     * @param extension the file extension to display
     */
    public void addExtensionFilter(String extension){
        if(ext == null){
            ext = new Vector();
        }
        ext.addElement(extension);
    }
    
    
    /**
     * {@inheritDoc}
     */
    public Vector getChildren(Object parent) {
        Vector response = new Vector();
        try {
            if(parent == null) {
                String[] roots = FileSystemStorage.getInstance().getRoots();
                for(int iter = 0 ; iter < roots.length ; iter++) {
                    response.addElement(roots[iter]);
                }
            } else {
                String name = (String)parent;
                if(!name.endsWith("/")) {
                    name += "/";
                }
                String[] res = FileSystemStorage.getInstance().listFiles(name);
                if(res != null){
                    if(showFiles) {
                        for(int iter = 0 ; iter < res.length ; iter++) {
                            String f = res[iter];
                            if(!FileSystemStorage.getInstance().isDirectory(name + f) && ext != null){
                                int i = f.lastIndexOf('.');
                                if(i > 0){
                                    String e = f.substring(i + 1, f.length());
                                    if(ext.contains(e)){
                                        response.addElement(name + f);                                                                
                                    }
                                }
                            }else{
                                response.addElement(name + f);                            
                            }
                        }
                    } else {
                        for(int iter = 0 ; iter < res.length ; iter++) {
                            if(FileSystemStorage.getInstance().isDirectory(name + res[iter])) {
                                response.addElement(name + res[iter]);
                            }
                        }
                    }
                }
            }
        } catch(Throwable err) {
            Log.e(err);
            return new Vector();
        }
        return response;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLeaf(Object node) {
        return !FileSystemStorage.getInstance().isDirectory((String)node);
    }
}
