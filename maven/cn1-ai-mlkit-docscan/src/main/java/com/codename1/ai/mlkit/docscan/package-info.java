/*
    Document   : package
    Created on : May 25, 2026
    Author     : Shai Almog
*/

/// ML Kit Document Scanner.
///
/// Captures and crops document photos with edge detection. Bridges to `MLKitDocumentScanner` on iOS and `com.google.android.gms:play-services-mlkit-document-scanner` on Android.
///
/// The single public class in this package is [DocumentScanner], which exposes
/// the feature via static methods returning
/// [com.codename1.util.AsyncResource]. A package-private
/// `NativeDocumentScanner` interface holds the platform contract; iOS Obj-C and
/// Android Java implementations live in `nativeios.zip` / `nativeand.zip`
/// inside the cn1lib bundle. References to `DocumentScanner.*` are recognised
/// by the Codename One build server's `AiDependencyTable`, which
/// auto-injects the matching CocoaPod / Swift Package / Android Gradle
/// dep / `Info.plist` usage strings / Android permissions on every
/// build -- no manual build hints required.
package com.codename1.ai.mlkit.docscan;
