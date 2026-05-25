/*
    Document   : package
    Created on : May 25, 2026
    Author     : Shai Almog
*/

/// TensorFlow Lite on-device inference.
///
/// Loads a `.tflite` model and runs inference against `float[]` inputs. Bridges to `TensorFlowLiteSwift` on iOS and `org.tensorflow:tensorflow-lite` on Android. Optional GPU / NNAPI / Core ML delegates are configured via build hints.
///
/// The single public class in this package is [Interpreter], which exposes
/// the feature via static methods returning
/// [com.codename1.util.AsyncResource]. A package-private
/// `NativeInterpreter` interface holds the platform contract; iOS Obj-C and
/// Android Java implementations live in `nativeios.zip` / `nativeand.zip`
/// inside the cn1lib bundle. References to `Interpreter.*` are recognised
/// by the Codename One build server's `AiDependencyTable`, which
/// auto-injects the matching CocoaPod / Swift Package / Android Gradle
/// dep / `Info.plist` usage strings / Android permissions on every
/// build -- no manual build hints required.
package com.codename1.ai.tflite;
