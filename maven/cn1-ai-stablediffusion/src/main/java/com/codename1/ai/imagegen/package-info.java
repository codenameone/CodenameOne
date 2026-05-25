/*
    Document   : package
    Created on : May 25, 2026
    Author     : Shai Almog
*/

/// On-device image generation via Core ML / ONNX Runtime.
///
/// Generates images from text prompts using a bundled Stable Diffusion model. Bridges to Core ML + Vision on iOS and ONNX Runtime on Android. Local-build only -- the model file exceeds the cloud build server's 2 GB upload cap.
///
/// The single public class in this package is [StableDiffusion], which exposes
/// the feature via static methods returning
/// [com.codename1.util.AsyncResource]. A package-private
/// `NativeStableDiffusion` interface holds the platform contract; iOS Obj-C and
/// Android Java implementations live in `nativeios.zip` / `nativeand.zip`
/// inside the cn1lib bundle. References to `StableDiffusion.*` are recognised
/// by the Codename One build server's `AiDependencyTable`, which
/// auto-injects the matching CocoaPod / Swift Package / Android Gradle
/// dep / `Info.plist` usage strings / Android permissions on every
/// build -- no manual build hints required.
package com.codename1.ai.imagegen;
