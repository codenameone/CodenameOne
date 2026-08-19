/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.ai.vision;

/// Writing system selectors for {@link TextRecognizer}. The recognizers behind
/// the portable API are script-specific rather than language-specific: one
/// script covers every language written in it, and each script model also
/// recognizes Latin characters embedded in the page.
///
/// Each selector call is also a build-time dependency marker, exactly like
/// {@link VisionBackends}. The methods must remain distinct so a build packages
/// only the script models the application actually asks for; a Japanese OCR
/// application does not carry the Korean or Devanagari model.
///
/// ```java
/// TextRecognizer recognizer = new TextRecognizer(
///         new VisionOptions().textScript(TextScript.japanese()));
/// recognizer.process(VisionImage.encoded(jpeg)).ready(result -> {
///     Log.p(result.getText());
/// });
/// ```
///
/// A selector states the script, not the backend. Android maps each one to the
/// matching ML Kit recognizer. Apple platforms map it to the Vision recognition
/// languages for that script, and report the analysis as unsupported when the
/// OS does not recognize that script -- selecting
/// {@link VisionBackends#mlKitTextRecognition()} then supplies the script model
/// through ML Kit instead.
public final class TextScript {
    private static final TextScript LATIN = new TextScript("latin");
    private static final TextScript CHINESE = new TextScript("chinese");
    private static final TextScript DEVANAGARI = new TextScript("devanagari");
    private static final TextScript JAPANESE = new TextScript("japanese");
    private static final TextScript KOREAN = new TextScript("korean");

    private final String id;

    private TextScript(String id) {
        this.id = id;
    }

    /// Selects the Latin script, which is also the platform default. Referencing
    /// it adds no model beyond the one every text recognizer already carries.
    /// @return the Latin script selector
    public static TextScript latin() {
        return LATIN;
    }

    /// Selects the Chinese script, covering Simplified and Traditional Chinese.
    /// @return the Chinese script selector
    public static TextScript chinese() {
        return CHINESE;
    }

    /// Selects the Devanagari script used by Hindi, Marathi, Nepali and Sanskrit.
    /// @return the Devanagari script selector
    public static TextScript devanagari() {
        return DEVANAGARI;
    }

    /// Selects the Japanese script, covering kanji, hiragana and katakana.
    /// @return the Japanese script selector
    public static TextScript japanese() {
        return JAPANESE;
    }

    /// Selects the Korean script, covering hangul and hanja.
    /// @return the Korean script selector
    public static TextScript korean() {
        return KOREAN;
    }

    /// @return the stable identifier passed to the port, never {@code null}
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return id;
    }
}
