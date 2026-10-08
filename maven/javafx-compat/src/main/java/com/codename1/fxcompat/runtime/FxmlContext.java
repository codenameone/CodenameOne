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
package com.codename1.fxcompat.runtime;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.codename1.compat.jdk.ResourceNames;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.Initializable;
import javafx.fxml.LoadException;
import javafx.scene.Node;
import javafx.util.Callback;

/// What a compiled FXML document is given to build itself with: the one
/// load it is part of. The build turns each `.fxml` file into a class with
/// a method `load(FxmlContext)`; everything in a document that depends on
/// who loads it -- the controller, the resource bundle, the location, the
/// names other elements are known by -- that method asks of this object.
///
/// Nothing here is JavaFX API. The methods are public because generated
/// code in the application's own packages calls them.
public final class FxmlContext {

    /// Bind the property to the source as it is.
    public static final int BIND_SAME = 0;
    /// Bind a string property to the text of the source's value.
    public static final int BIND_STRING = 1;
    /// Bind a number property to the source's number.
    public static final int BIND_NUMBER = 2;

    private final FxmlDispatch dispatch;
    private final String path;
    private final URL location;
    private final ResourceBundle resources;
    private final Callback<Class<?>, Object> controllerFactory;
    private final Map<String, Object> namespace;
    private final Object givenRoot;
    private Object controller;

    /// Creates the context of one load.
    ///
    /// #### Parameters
    ///
    /// - `dispatch`: the dispatcher that reaches controllers and documents
    ///
    /// - `path`: the resource path of the document, without a leading slash
    ///
    /// - `location`: where the document was asked for, or `null`
    ///
    /// - `resources`: the bundle of its `%key` strings, or `null`
    ///
    /// - `controllerFactory`: what creates controllers, or `null` for the
    ///   no-argument constructor
    ///
    /// - `namespace`: the map the `fx:id` values are collected in
    ///
    /// - `root`: the object an `fx:root` document builds on, or `null`
    ///
    /// - `controller`: the controller set on the loader, or `null`
    public FxmlContext(FxmlDispatch dispatch, String path, URL location, ResourceBundle resources,
            Callback<Class<?>, Object> controllerFactory, Map<String, Object> namespace, Object root,
            Object controller) {
        this.dispatch = dispatch;
        this.path = path;
        this.location = location;
        this.resources = resources;
        this.controllerFactory = controllerFactory;
        this.namespace = namespace;
        this.givenRoot = root;
        this.controller = controller;
        if (controller != null) {
            namespace.put("controller", controller);
        }
    }

    /// A failure to load, naming the document.
    public LoadException error(String message) {
        return new LoadException(path + ": " + message);
    }

    /// The controller of the document, created now unless the loader was
    /// given one: by the controller factory when there is one, otherwise by
    /// the no-argument constructor of the class the document names.
    public Object controller(Class<?> type, String className) throws LoadException {
        if (controller == null) {
            if (controllerFactory != null) {
                controller = controllerFactory.call(type);
                if (controller == null) {
                    throw error("the controller factory answered null for " + className);
                }
            } else {
                controller = dispatch.create(className);
                if (controller == null) {
                    throw error("cannot create the controller " + className
                            + ": it needs a constructor without arguments, and must not be abstract");
                }
            }
            namespace.put("controller", controller);
        }
        return controller;
    }

    /// The controller, or `null` when the document has none.
    public Object controller() {
        return controller;
    }

    /// The object an `fx:root` document builds on, which the application
    /// set with `FXMLLoader.setRoot`.
    public Object root() throws LoadException {
        if (givenRoot == null) {
            throw error("the document has an <fx:root> element, so setRoot() must be called before load()");
        }
        return givenRoot;
    }

    /// Records an element under its `fx:id`: in the namespace, in the
    /// controller's field of that name, and as the id of a node that has
    /// none.
    public void id(String id, Object value) {
        namespace.put(id, value);
        if (value instanceof Node) {
            Node node = (Node) value;
            if (node.getId() == null) {
                node.setId(id);
            }
        }
        if (controller != null) {
            dispatch.inject(controller, id, value);
        }
    }

    /// The object recorded under an `fx:id`, for `$name` and
    /// `<fx:reference>`.
    public Object get(String id) throws LoadException {
        Object value = namespace.get(id);
        if (value == null && !namespace.containsKey(id)) {
            throw error("nothing with the fx:id \"" + id + "\" was defined before it is used");
        }
        return value;
    }

    /// The handler for `onAction="#name"`: it calls the controller's method
    /// of that name when the event arrives.
    @SuppressWarnings("rawtypes")
    public EventHandler handler(final String name) throws LoadException {
        final Object target = controller;
        if (target == null) {
            throw error("the handler #" + name + " needs a controller, and the document has none");
        }
        return new Handler(dispatch, path, target, name);
    }

    /// The handler of one `#name`: a call of the controller's method.
    private static final class Handler implements EventHandler<Event> {
        private final FxmlDispatch dispatch;
        private final String document;
        private final Object target;
        private final String name;

        Handler(FxmlDispatch dispatch, String document, Object target, String name) {
            this.dispatch = dispatch;
            this.document = document;
            this.target = target;
            this.name = name;
        }

        @Override
        public void handle(Event event) {
            boolean called;
            try {
                called = dispatch.invoke(target, name, event);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                RuntimeException wrapped = new RuntimeException(document + ": the handler #" + name
                        + " failed: " + e);
                wrapped.initCause(e);
                throw wrapped;
            }
            if (!called) {
                throw new IllegalStateException(document + ": the controller " + target.getClass().getName()
                        + " has no handler method " + name
                        + "() that is public or annotated @FXML and takes no argument or the event");
            }
        }
    }

    /// The string of a `%key`.
    public String string(String key) throws LoadException {
        if (resources == null) {
            throw error("the document uses %" + key + " but was loaded without a resource bundle");
        }
        try {
            return resources.getString(key);
        } catch (MissingResourceException e) {
            throw error("the resource bundle has no key \"" + key + "\"");
        }
    }

    /// The address an `@path` stands for: the path taken relative to the
    /// document, as a `cn1res:` address the image and style sheet loaders
    /// read from the application's resources.
    public String url(String relative) {
        String resolved = resolve(relative);
        return resolved == null ? relative : "cn1res:/" + resolved;
    }

    private String resolve(String relative) {
        if (relative.startsWith("/")) {
            return ResourceNames.normalize(relative);
        }
        int slash = path.lastIndexOf('/');
        return ResourceNames.normalize(slash < 0 ? relative : path.substring(0, slash + 1) + relative);
    }

    /// Loads the document an `<fx:include source>` names and answers its
    /// root. With an `fx:id`, the root is recorded under it and the
    /// included document's controller under that id followed by
    /// `Controller`.
    public Object include(String source, String id) throws Exception {
        String child = resolve(source);
        if (child == null || !dispatch.has(child)) {
            throw error("the included document " + source + " (" + child
                    + ") was not compiled into the application");
        }
        URL childLocation = null;
        if (location != null) {
            try {
                childLocation = new URL(location, source);
            } catch (MalformedURLException e) {
                childLocation = null;
            }
        }
        FxmlContext context = new FxmlContext(dispatch, child, childLocation, resources, controllerFactory,
                new java.util.HashMap<String, Object>(), null, null);
        Object root = dispatch.load(child, context);
        if (id != null) {
            id(id, root);
            if (context.controller != null) {
                namespace.put(id + "Controller", context.controller);
                if (controller != null) {
                    dispatch.inject(controller, id + "Controller", context.controller);
                }
            }
        }
        return root;
    }

    /// Called by a compiled document when its tree is complete: hands the
    /// controller its location and resources and calls its `initialize`.
    public void done() throws Exception {
        if (controller == null) {
            return;
        }
        if (location != null) {
            dispatch.inject(controller, "location", location);
        }
        if (resources != null) {
            dispatch.inject(controller, "resources", resources);
        }
        if (controller instanceof Initializable) {
            ((Initializable) controller).initialize(location, resources);
        } else {
            dispatch.init(controller);
        }
    }

    /// Binds a property to a source for `${expression}`. `mode` says how
    /// the value is converted: one of the `BIND_` constants.
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void bind(Property property, final ObservableValue source, int mode) {
        if (mode == BIND_STRING) {
            property.bind(new StringBinding() {
                {
                    bind(source);
                }

                @Override
                protected String computeValue() {
                    Object v = source.getValue();
                    return v == null ? "" : v.toString();
                }
            });
        } else if (mode == BIND_NUMBER) {
            property.bind(new DoubleBinding() {
                {
                    bind(source);
                }

                @Override
                protected double computeValue() {
                    Object v = source.getValue();
                    return v instanceof Number ? ((Number) v).doubleValue() : 0;
                }
            });
        } else {
            property.bind(source);
        }
    }
}
