/*
    Document   : package
    Created on : May 25, 2026
    Author     : Shai Almog
*/

/// ML Kit Language Identification.
///
/// Identifies the language of a given text string. Bridges to `MLKitLanguageID` on iOS and `com.google.mlkit:language-id` on Android.
///
/// The single public class in this package is [LanguageIdentifier], which exposes
/// the feature via static methods returning
/// [com.codename1.util.AsyncResource]. A package-private
/// `NativeLanguageIdentifier` interface holds the platform contract; iOS Obj-C and
/// Android Java implementations live in `nativeios.zip` / `nativeand.zip`
/// inside the cn1lib bundle. References to `LanguageIdentifier.*` are recognised
/// by the Codename One build server's `AiDependencyTable`, which
/// auto-injects the matching CocoaPod / Swift Package / Android Gradle
/// dep / `Info.plist` usage strings / Android permissions on every
/// build -- no manual build hints required.
package com.codename1.ai.mlkit.langid;
