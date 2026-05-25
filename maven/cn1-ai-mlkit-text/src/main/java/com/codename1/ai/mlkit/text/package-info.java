/*
    Document   : package
    Created on : May 25, 2026
    Author     : Shai Almog
*/

/// ML Kit Text Recognition (OCR).
///
/// Extracts text strings from images via Google's on-device ML Kit. Bridges to `MLKitTextRecognition` on iOS and `com.google.mlkit:text-recognition` on Android.
///
/// The single public class in this package is [TextRecognizer], which exposes
/// the feature via static methods returning
/// [com.codename1.util.AsyncResource]. A package-private
/// `NativeTextRecognizer` interface holds the platform contract; iOS Obj-C and
/// Android Java implementations live in `nativeios.zip` / `nativeand.zip`
/// inside the cn1lib bundle. References to `TextRecognizer.*` are recognised
/// by the Codename One build server's `AiDependencyTable`, which
/// auto-injects the matching CocoaPod / Swift Package / Android Gradle
/// dep / `Info.plist` usage strings / Android permissions on every
/// build -- no manual build hints required.
package com.codename1.ai.mlkit.text;
