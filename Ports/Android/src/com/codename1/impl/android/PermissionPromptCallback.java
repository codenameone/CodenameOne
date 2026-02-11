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
package com.codename1.impl.android;

/**
 * Callback used to customize Android permission rationale dialogs.
 */
public interface PermissionPromptCallback {
    /**
     * Shows a permission prompt that has positive and negative actions.
     *
     * @param permission Android permission name
     * @param title prompt title
     * @param body prompt body text
     * @param positiveButtonText positive button text
     * @param negativeButtonText negative button text
     * @return true to perform the positive action, false for the negative action
     */
    public boolean showPermissionPrompt(String permission, String title, String body, String positiveButtonText, String negativeButtonText);

    /**
     * Shows an informational permission message with only one action button.
     *
     * @param permission Android permission name
     * @param title dialog title
     * @param body dialog body
     * @param okButtonText button text
     */
    public void showPermissionMessage(String permission, String title, String body, String okButtonText);
}
