/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codenameone.playground;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.util.Resources;

import java.util.ArrayList;
import java.util.List;

/**
 * Host objects and helpers exposed to user scripts.
 */
public class PlaygroundContext {

    public interface Logger {
        void log(String message);
    }

    /** Receives runtime errors that happen after a script has finished its
     * initial evaluation — typically a lambda body that fails on a later
     * event firing. Without this hook the EDT silently swallows the
     * exception and the user sees a UI that no longer reacts to input. */
    public interface RuntimeErrorReporter {
        void reportRuntimeError(String message, Throwable cause);
    }

    private final Form hostForm;
    private final Container previewRoot;
    private final Resources theme;
    private final Logger logger;
    private final RuntimeErrorReporter runtimeErrorReporter;
    private Form shownForm;
    private final List<Component> createdComponents = new ArrayList<Component>();
    private Form firstCreatedForm;
    private Component firstCreatedComponent;

    public PlaygroundContext(Form hostForm, Container previewRoot, Resources theme, Logger logger) {
        this(hostForm, previewRoot, theme, logger, null);
    }

    public PlaygroundContext(Form hostForm, Container previewRoot, Resources theme, Logger logger,
            RuntimeErrorReporter runtimeErrorReporter) {
        this.hostForm = hostForm;
        this.previewRoot = previewRoot;
        this.theme = theme;
        this.logger = logger;
        this.runtimeErrorReporter = runtimeErrorReporter;
    }

    public Form getHostForm() {
        return hostForm;
    }

    public Container getPreviewRoot() {
        return previewRoot;
    }

    public Resources getTheme() {
        return theme;
    }

    /** The context of the current (or most recent) run. */
    public static PlaygroundContext getCurrent() {
        return active;
    }

    public static void debug(String message) {
    }

    /**
     * The context of the most recent run. Compiled code calls {@link #showForm}
     * (the compiler redirects {@code form.show()} there) long after the run that
     * created it -- from a button's listener, say -- and that should still land in
     * the preview rather than replace the Playground.
     */
    private static PlaygroundContext active;

    /** Receives forms shown after the run finished (the Playground replaces its preview). */
    public interface FormShownListener {
        void formShown(Form form);
    }

    private FormShownListener formShownListener;
    private boolean runFinished;

    static void activate(PlaygroundContext context) {
        active = context;
    }

    public static PlaygroundContext getActive() {
        return active;
    }

    public void setFormShownListener(FormShownListener listener) {
        this.formShownListener = listener;
    }

    void markRunFinished() {
        runFinished = true;
    }

    /**
     * What {@code form.show()} and {@code form.showBack()} in Playground code
     * compile to: the form becomes the preview instead of taking over the
     * Playground. Outside a run (no active context) it just shows the form.
     */
    public static void showForm(Form form) {
        PlaygroundContext context = active;
        if (context == null || form == null) {
            if (form != null) {
                form.show();
            }
            return;
        }
        context.captureShownForm(form);
        if (context.runFinished && context.formShownListener != null) {
            context.formShownListener.formShown(form);
        }
    }

    public void log(String message) {
        logger.log(message);
    }

    public void reportRuntimeError(String message, Throwable cause) {
        if (runtimeErrorReporter != null) {
            runtimeErrorReporter.reportRuntimeError(message, cause);
        }
    }

    public void captureShownForm(Form form) {
        shownForm = form;
    }

    public Form getShownForm() {
        return shownForm;
    }

    public void clearShownForm() {
        shownForm = null;
    }

    public void recordCreatedComponent(Component component) {
        if (component == null || component == hostForm || component == previewRoot) {
            return;
        }
        if (firstCreatedComponent == null) {
            firstCreatedComponent = component;
        }
        if (firstCreatedForm == null && component instanceof Form) {
            firstCreatedForm = (Form) component;
        }
        createdComponents.add(component);
    }

    public Component getFirstCreatedComponent() {
        return firstCreatedComponent;
    }

    public Form getFirstCreatedForm() {
        return firstCreatedForm;
    }

    public List<Component> getCreatedComponents() {
        return createdComponents;
    }

    public void clearCreatedComponents() {
        createdComponents.clear();
        firstCreatedForm = null;
        firstCreatedComponent = null;
    }

    public void clearPreview() {
        previewRoot.removeAll();
        previewRoot.revalidate();
    }

    public void refreshPreview() {
        previewRoot.revalidate();
    }

    public void setTitle(String title) {
        hostForm.setTitle(title);
        hostForm.revalidate();
    }
}
