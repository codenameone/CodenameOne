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
package javafx.fxml;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;

import com.codename1.fxcompat.runtime.FxmlContext;
import com.codename1.fxcompat.runtime.FxmlDispatch;

import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.util.Callback;

/// Loads the object tree an FXML document describes.
///
/// The document is not read here. The build compiled it into a class, and
/// loading means finding that class for the location and running it; see
/// the package description. A location is matched by its path: a `cn1res:`
/// address by the whole path, any other address -- what `getResource`
/// answers on a desktop -- by the longest tail of its path that is the
/// resource path of a compiled document.
///
/// Not provided: builder factories, script handlers and the character set
/// and class loader settings, none of which mean anything for a document
/// that is compiled ahead of time.
public class FXMLLoader {

    private URL location;
    private ResourceBundle resources;
    private Object root;
    private Object controller;
    private Callback<Class<?>, Object> controllerFactory;
    private final ObservableMap<String, Object> namespace = FXCollections
            .observableMap(new HashMap<String, Object>());

    /// Creates a loader without a location.
    public FXMLLoader() {
        this(null, null);
    }

    /// Creates a loader for the document at a location.
    public FXMLLoader(URL location) {
        this(location, null);
    }

    /// Creates a loader for the document at a location, with the bundle
    /// its `%key` strings come from.
    public FXMLLoader(URL location, ResourceBundle resources) {
        this.location = location;
        this.resources = resources;
    }

    /// Returns the location of the document.
    public URL getLocation() {
        return location;
    }

    /// Sets the location of the document.
    public void setLocation(URL location) {
        this.location = location;
    }

    /// Returns the bundle `%key` strings are read from.
    public ResourceBundle getResources() {
        return resources;
    }

    /// Sets the bundle `%key` strings are read from.
    public void setResources(ResourceBundle resources) {
        this.resources = resources;
    }

    /// Returns the map of the objects the document named with `fx:id`,
    /// with the controller under `controller`.
    public ObservableMap<String, Object> getNamespace() {
        return namespace;
    }

    /// Returns the root of the loaded tree, or the one that was set.
    @SuppressWarnings("unchecked")
    public <T> T getRoot() {
        return (T) root;
    }

    /// Sets the object a document whose root element is `<fx:root>` builds
    /// on.
    public void setRoot(Object root) {
        this.root = root;
    }

    /// Returns the controller of the document.
    @SuppressWarnings("unchecked")
    public <T> T getController() {
        return (T) controller;
    }

    /// Sets the controller, for a document that names none with
    /// `fx:controller`.
    public void setController(Object controller) {
        this.controller = controller;
        if (controller == null) {
            namespace.remove("controller");
        } else {
            namespace.put("controller", controller);
        }
    }

    /// Returns what creates the controllers.
    public Callback<Class<?>, Object> getControllerFactory() {
        return controllerFactory;
    }

    /// Sets what creates the controller of this document and of the ones
    /// it includes; without one the no-argument constructor is used.
    public void setControllerFactory(Callback<Class<?>, Object> controllerFactory) {
        this.controllerFactory = controllerFactory;
    }

    /// Loads the document at the location of this loader.
    @SuppressWarnings("unchecked")
    public <T> T load() throws IOException {
        if (location == null) {
            throw new IllegalStateException("Location is not set.");
        }
        FxmlDispatch dispatch = FxmlDispatch.current();
        String path = cn1Find(dispatch, location.toString());
        if (path == null) {
            throw new LoadException("No compiled FXML document for " + location
                    + ". FXML is compiled by the build from the .fxml files among the application's desktop"
                    + " resources; a document that is not there when the application is built cannot be loaded.");
        }
        FxmlContext context = new FxmlContext(dispatch, path, location, resources, controllerFactory, namespace,
                root, controller);
        Object loaded;
        try {
            loaded = dispatch.load(path, context);
        } catch (IOException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new LoadException(path + ": " + e, e);
        } catch (Exception e) {
            throw new LoadException(path + ": " + e, e);
        }
        controller = context.controller();
        root = loaded;
        return (T) loaded;
    }

    /// Loads the document at a location.
    public static <T> T load(URL location) throws IOException {
        return load(location, null);
    }

    /// Loads the document at a location, with the bundle its `%key`
    /// strings come from.
    public static <T> T load(URL location, ResourceBundle resources) throws IOException {
        if (location == null) {
            throw new NullPointerException("Location is required.");
        }
        return new FXMLLoader(location, resources).<T>load();
    }

    /// The resource path of the compiled document an address names, or
    /// `null`.
    private static String cn1Find(FxmlDispatch dispatch, String address) {
        String path = address;
        int bang = path.lastIndexOf("!/");
        if (bang >= 0) {
            path = path.substring(bang + 2);
        } else {
            int colon = path.indexOf(':');
            int slash = path.indexOf('/');
            if (colon > 0 && (slash < 0 || colon < slash)) {
                path = path.substring(colon + 1);
            }
        }
        while (true) {
            int start = 0;
            while (start < path.length() && path.charAt(start) == '/') {
                start++;
            }
            path = path.substring(start);
            if (path.length() == 0) {
                return null;
            }
            if (dispatch.has(path)) {
                return path;
            }
            int next = path.indexOf('/');
            if (next < 0) {
                return null;
            }
            path = path.substring(next);
        }
    }
}
