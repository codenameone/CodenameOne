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
package com.codenameone.developerguide.flutter;

import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.platform.EventChannel;
import com.codename1.flutter.platform.MethodCall;
import com.codename1.flutter.platform.MethodChannel;
import com.codename1.flutter.widgets.Text;
import com.codename1.ui.Button;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;

/**
 * The developer guide's Flutter interop examples, compiled rather than taken
 * on trust.
 *
 * <p>Both entry points are shown with a {@code Text} widget standing in for a
 * real one, because the point is where the boundary sits, not what the widget
 * draws.</p>
 */
public final class FlutterInteropSnippets {

    private FlutterInteropSnippets() {
    }

    /** One Dart screen inside an otherwise ordinary Codename One form. */
    public static void embedOneScreen() {
        // tag::flutter-interop-java-001[]
        Form dashboard = new Form("Dashboard", BoxLayout.y());
        dashboard.add(new Label("Written in Codename One"));

        // A Dart widget tree, compiled to Java at build time. wrap() returns
        // an ordinary Container, so it is added like any other component.
        dashboard.add(FlutterUI.wrap(new Text("Rendered from Dart")));

        dashboard.add(new Button("Also Codename One"));
        dashboard.show();
        // end::flutter-interop-java-001[]
    }

    /** The widget tree IS the application. */
    public static void runTheWholeApplication() {
        // tag::flutter-interop-java-002[]
        // runApp mounts the tree in a Form of its own and shows it. Unlike
        // wrap(), this installs the Material base theme, because the widget
        // tree is expected to be the whole UI.
        FlutterUI.runApp(new Text("The entire application"));
        // end::flutter-interop-java-002[]
    }

    /**
     * A Java handler answering a Dart {@code MethodChannel.invokeMethod}
     * call, written exactly like a classic Flutter plugin's Android side --
     * {@code implements MethodChannel.MethodCallHandler}, {@code
     * result.success(...)} -- just importing {@code com.codename1.flutter
     * .platform} instead of {@code io.flutter.plugin.common}.
     */
    public static void answerDartWithAJavaHandler() {
        // tag::flutter-interop-java-003[]
        MethodChannel channel = new MethodChannel("com.example.app/battery");
        channel.setMethodCallHandler(new MethodChannel.MethodCallHandler() {
            @Override
            public void onMethodCall(MethodCall call, MethodChannel.Result result) {
                if ("getBatteryLevel".equals(call.method())) {
                    result.success(87);
                } else {
                    result.notImplemented();
                }
            }
        });
        // end::flutter-interop-java-003[]
    }

    /**
     * Java calling a handler the Dart side registered with {@code
     * MethodChannel.setMethodCallHandler}. {@code result} may complete
     * later -- a loaded resource, a native callback -- the call does not
     * require an answer before it returns.
     */
    public static void invokeADartRegisteredHandler() {
        // tag::flutter-interop-java-004[]
        MethodChannel channel = new MethodChannel("com.example.app/battery");
        channel.invokeMethod("refreshBatteryLevel", null, new MethodChannel.Result() {
            @Override
            public void success(Object result) {
                System.out.println("Dart replied: " + result);
            }

            @Override
            public void error(String errorCode, String errorMessage, Object errorDetails) {
                System.out.println("Dart reported an error: " + errorCode);
            }

            @Override
            public void notImplemented() {
                System.out.println("no Dart handler is registered");
            }
        });
        // end::flutter-interop-java-004[]
    }

    /**
     * A Java {@code EventChannel.StreamHandler} pushing events to whatever
     * Dart listens on {@code receiveBroadcastStream()}. {@code onListen}
     * runs on the first listener, {@code onCancel} on the last cancel; the
     * {@code EventSink} it is handed stays valid for pushing events in
     * between, from wherever the real readings arrive.
     */
    public static void pushEventsFromAJavaStreamHandler() {
        // tag::flutter-interop-java-005[]
        EventChannel channel = new EventChannel("com.example.app/sensor");
        final EventChannel.EventSink[] activeSink = new EventChannel.EventSink[1];
        channel.setStreamHandler(new EventChannel.StreamHandler() {
            @Override
            public void onListen(Object arguments, EventChannel.EventSink events) {
                activeSink[0] = events;
            }

            @Override
            public void onCancel(Object arguments) {
                activeSink[0] = null;
            }
        });

        // Called whenever a new reading arrives, from wherever that is.
        EventChannel.EventSink sink = activeSink[0];
        if (sink != null) {
            sink.success(42.5);
        }
        // end::flutter-interop-java-005[]
    }

    /**
     * Calling a transpiled Dart top-level function directly from Java. A
     * Dart file {@code lib/greeting.dart} declaring
     * {@code String greet(String name) => 'Hello, $name!';} transpiles to a
     * public static method on a generated {@code LibGreetingLib} class --
     * {@link LibGreetingLib} here stands in for that generated class, shaped
     * exactly like the transpiler's own output, so this method is real code
     * that compiles and runs rather than a description of the convention.
     */
    public static void callATranspiledDartFunction() {
        // tag::flutter-interop-java-006[]
        String greeting = LibGreetingLib.greet("World");
        System.out.println(greeting);
        // end::flutter-interop-java-006[]
    }

    /**
     * Stands in for the transpiler's generated output of
     * {@code lib/greeting.dart}'s {@code greet}, so
     * {@link #callATranspiledDartFunction()} above is real, compiling code:
     * a top-level Dart function becomes a public static method on a class
     * named after its file ({@code lib/greeting.dart} -> {@code
     * LibGreetingLib}).
     */
    private static final class LibGreetingLib {
        private LibGreetingLib() {
        }

        static String greet(String name) {
            return "Hello, " + name + "!";
        }
    }
}
