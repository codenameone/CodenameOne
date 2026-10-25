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

/// Everything an FXML loader would find by name with reflection, as methods
/// the build implements with direct code: the class that builds a document,
/// the constructor of a controller, the field an `fx:id` goes into and the
/// method an `onAction="#name"` calls.
///
/// #### Who implements it
///
/// The build does, once per application, in the class [FxmlRegistry]: after
/// the application is compiled it reads the compiled documents and the
/// controllers out of the class files and writes one override per method
/// below. This class is what those overrides fall back to, and each method
/// here answers "not mine" -- which is also what an application without a
/// single FXML document gets.
///
/// #### Why generated code goes through here
///
/// A compiled document does not call the generated dispatcher itself; it
/// calls [FxmlContext], which calls the installed instance of this class.
/// The document is Java source, compiled with the application, and the
/// dispatcher does not exist yet at that point: it is generated from the
/// class files javac leaves, because only they say which members a
/// controller really has. A fixed runtime class between the two lets each
/// be generated without the other, and lets a test install a dispatcher of
/// its own.
///
/// No method takes a `URL` or a `ResourceBundle`: the application sees
/// those types relocated, and keeping them out of the generated signatures
/// keeps the generator independent of where they went.
public abstract class FxmlDispatch {

    private static FxmlDispatch current;

    /// Creates a dispatcher that knows nothing.
    protected FxmlDispatch() {
        // Every answer is the default one until a subclass overrides it.
    }

    /// The installed dispatcher: the application's generated one unless
    /// [#install(FxmlDispatch)] replaced it.
    public static FxmlDispatch current() {
        if (current == null) {
            current = new FxmlRegistry();
        }
        return current;
    }

    /// Installs a dispatcher; `null` returns to the generated one.
    public static void install(FxmlDispatch dispatch) {
        current = dispatch;
    }

    /// Whether the application has a compiled document for `path`, a
    /// resource path without a leading slash (`com/example/main.fxml`).
    public boolean has(String path) {
        return false;
    }

    /// Builds the document at `path` and answers its root. Only called for
    /// a path [#has(String)] answered `true` for.
    public Object load(String path, FxmlContext context) throws Exception {
        return null;
    }

    /// Creates the controller of a class by its no-argument constructor,
    /// or answers `null` for a class the build generated no constructor
    /// call for.
    public Object create(String className) {
        return null;
    }

    /// Sets the field of `controller` that is named `id` to `value`.
    /// Answers `false` when the controller has no such field. Fails with
    /// an `IllegalArgumentException` when it has one of another type.
    public boolean inject(Object controller, String id, Object value) {
        return false;
    }

    /// Calls the handler method `handler` of `controller`, with `event`
    /// when the method takes one. Answers `false` when the controller has
    /// no such method.
    public boolean invoke(Object controller, String handler, Object event) throws Exception {
        return false;
    }

    /// Calls the `initialize()` method of `controller`. Answers `false`
    /// when it has none.
    public boolean init(Object controller) throws Exception {
        return false;
    }

    /// What the generated field code calls for a value of the wrong type.
    public static void mismatch(String id, String fieldType, Object value) {
        throw new IllegalArgumentException("The element with fx:id \"" + id + "\" is a "
                + (value == null ? "null" : value.getClass().getName()) + ", but the controller's field is a "
                + fieldType);
    }
}
