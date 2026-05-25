/*
    Document   : package
    Created on : May 25, 2026
    Author     : Shai Almog
*/

/// On-device speech-to-text via whisper.cpp.
///
/// Transcribes audio files using the whisper.cpp inference engine -- works offline, no network calls. The cn1lib ships the model loader; callers supply the model file and the audio file path.
///
/// The single public class in this package is [WhisperRecognizer], which exposes
/// the feature via static methods returning
/// [com.codename1.util.AsyncResource]. A package-private
/// `NativeWhisperRecognizer` interface holds the platform contract; iOS Obj-C and
/// Android Java implementations live in `nativeios.zip` / `nativeand.zip`
/// inside the cn1lib bundle. References to `WhisperRecognizer.*` are recognised
/// by the Codename One build server's `AiDependencyTable`, which
/// auto-injects the matching CocoaPod / Swift Package / Android Gradle
/// dep / `Info.plist` usage strings / Android permissions on every
/// build -- no manual build hints required.
package com.codename1.ai.whisper;
