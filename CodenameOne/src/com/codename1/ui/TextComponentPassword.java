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

package com.codename1.ui;

import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

/**
 * TextComponent extended to automatically add mask/unmask password button near
 * the TextField; it acts like a normal TextComponent if the Constraint is not
 * TextArea.PASSWORD
 *
 * @author Francesco Galgani
 */
public class TextComponentPassword extends TextComponent {
    private final TextField field = super.getField();

    public TextComponentPassword() {
        super();
        field.setConstraint(TextArea.PASSWORD);
        action(FontImage.MATERIAL_VISIBILITY_OFF).
            actionClick(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (field.getConstraint() == TextArea.PASSWORD) {
                        field.setConstraint(TextField.NON_PREDICTIVE);
                        action(FontImage.MATERIAL_VISIBILITY_OFF);
                    } else {
                        field.setConstraint(TextField.PASSWORD);
                        action(FontImage.MATERIAL_VISIBILITY);
                    }
                    if (field.isEditing()) {
                        field.stopEditing();
                        field.startEditingAsync();
                    } else {
                        field.getParent().revalidate();
                    }
                }
            });
    }

    
    /**
     * Overridden for covariant return type
     * {@inheritDoc}
     */
    @Override
    public TextComponentPassword label(String text) {
        return (TextComponentPassword)super.label(text); 
    }
    
    /**
     * Overridden for covariant return type
     * {@inheritDoc}
     */
    public TextComponentPassword labelAndHint(String text) {
        return (TextComponentPassword)super.labelAndHint(text);
    }
    
    /**
     * Overridden for covariant return type
     * {@inheritDoc}
     */
    public TextComponentPassword hint(String hint) {
        return (TextComponentPassword)super.hint(hint);
    }

    /**
     * Overridden for covariant return type
     * {@inheritDoc}
     */
    public TextComponentPassword hint(Image hint) {
        return (TextComponentPassword)super.hint(hint);
    }
}
